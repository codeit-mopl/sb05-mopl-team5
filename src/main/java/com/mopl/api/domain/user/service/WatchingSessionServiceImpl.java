package com.mopl.api.domain.user.service;

import com.mopl.api.domain.content.entity.Content;
import com.mopl.api.domain.content.repository.ContentRepository;
import com.mopl.api.domain.user.dto.event.WatchingSessionChangeEvent;
import com.mopl.api.domain.user.dto.request.WatchingSessionSearchRequest;
import com.mopl.api.domain.user.dto.response.CursorResponseWatchingSessionDto;
import com.mopl.api.domain.user.dto.response.WatchingSessionDto;
import com.mopl.api.domain.user.entity.User;
import com.mopl.api.domain.user.entity.WatchingSession;
import com.mopl.api.domain.user.exception.detail.WatchingSessionNotFoundException;
import com.mopl.api.domain.user.mapper.WatchingSessionMapper;
import com.mopl.api.domain.user.repository.UserRepository;
import com.mopl.api.domain.user.repository.WatchingSessionRedisRepository;
import com.mopl.api.domain.user.repository.WatchingSessionRepository;
import com.mopl.api.global.config.websocket.dto.WatchingSessionChange;
import com.mopl.api.global.config.websocket.dto.WatchingSessionChange.ChangeType;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WatchingSessionServiceImpl implements WatchingSessionService {

    private final UserRepository userRepository;
    private final ContentRepository contentRepository;
    private final WatchingSessionRepository watchingSessionRepository;
    private final WatchingSessionRedisRepository watchingSessionCacheRepository;
    private final WatchingSessionMapper watchingSessionMapper;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 세션 조회 정책 고민 및 결정 필요 결정 포인트: 1. Redis 단일 신뢰 모델로 갈 것인가? 2. Redis 조회 후 DB 무결성 검증을 할 것인가? - 즉시 검증 - 배치 / 리스너 기반 보정
     */
    @Override
    public WatchingSessionDto getWatchingSession(UUID watcherId) {

        // TODO REDIS에 세션 정보가 있다면 가져오기
        // Redis 신뢰 > REDIS만 조회
        // Redis 신뢰 x > DB 무결성 검증 or DB만 조회

        throw new UnsupportedOperationException("조회 정책 결정 후 구현");
    }

    @Override
    public CursorResponseWatchingSessionDto getWatchingSession(UUID contentId,
        WatchingSessionSearchRequest request) {

        // TODO REDIS에 세션 정보가 있다면 가져오기

        throw new UnsupportedOperationException("조회 정책 결정 후 구현");
    }

    @Override
    @Transactional
    public WatchingSessionChange joinWatchingSession(UUID contentId, UUID watcherId) {

        User watcher = userRepository.findById(watcherId)
                                     .orElseThrow(() -> new RuntimeException("존재하지 않는 유저"));
        Content content = contentRepository.findById(contentId)
                                           .orElseThrow(() -> new RuntimeException("존재하지 않는 콘텐츠"));

        log.debug("세션 생성 시작 contentId: {}, watcherId: {}", contentId, watcherId);
        // TODO 중복 저장 방지 로직

        WatchingSession session = new WatchingSession(watcher, content);
        WatchingSession saved = watchingSessionRepository.save(session);
        log.debug("DB 저장 완료");

        watchingSessionCacheRepository.addWatcher(contentId, watcherId);
        log.debug("Redis 저장 완료");
        long count = watchingSessionCacheRepository.countWatchers(contentId);

        log.debug("세션 생성 완료");

        WatchingSessionChange change = WatchingSessionChange.builder()
                                                            .watchingSession(watchingSessionMapper.toDto(saved))
                                                            .type(ChangeType.JOIN)
                                                            .watcherCount(count)
                                                            .build();

        eventPublisher.publishEvent(WatchingSessionChangeEvent.builder()
                                                              .contentId(contentId)
                                                              .change(change)
                                                              .build());
        log.debug("join 이벤트 발행 완료");

        return change;
    }

    @Override
    @Transactional
    public WatchingSessionChange leaveWatchingSession(UUID contentId, UUID watcherId) {

        WatchingSession session = watchingSessionRepository.findByContentIdAndWatcherId(contentId, watcherId)
                                                           .orElse(null);

        if (session == null) {
            throw WatchingSessionNotFoundException.withContentIdAndWatcherId(contentId, watcherId);
        }

        watchingSessionRepository.delete(session);
        log.debug("DB 삭제 완료");
        watchingSessionCacheRepository.removeWatcher(contentId, watcherId);
        log.debug("Redis 삭제 완료");
        long count = watchingSessionCacheRepository.countWatchers(contentId);

        WatchingSessionChange change = WatchingSessionChange.builder()
                                                            .watchingSession(watchingSessionMapper.toDto(session))
                                                            .watcherCount(count)
                                                            .type(ChangeType.LEAVE)
                                                            .build();

        eventPublisher.publishEvent(WatchingSessionChangeEvent.builder()
                                                              .contentId(contentId)
                                                              .change(change)
                                                              .build());
        log.debug("leave 이벤트 발행 완료");

        return change;
    }
}