package com.mopl.api.domain.user.service;

import com.mopl.api.domain.content.entity.Content;
import com.mopl.api.domain.content.repository.ContentRepository;
import com.mopl.api.domain.user.dto.command.WatchingSessionCreateCommand;
import com.mopl.api.domain.user.dto.request.WatchingSessionSearchRequest;
import com.mopl.api.domain.user.dto.response.CursorResponseWatchingSessionDto;
import com.mopl.api.domain.user.dto.response.WatchingSessionDto;
import com.mopl.api.domain.user.entity.User;
import com.mopl.api.domain.user.entity.WatchingSession;
import com.mopl.api.domain.user.mapper.WatchingSessionMapper;
import com.mopl.api.domain.user.repository.UserRepository;
import com.mopl.api.domain.user.repository.WatchingSessionCacheRepository;
import com.mopl.api.domain.user.repository.WatchingSessionRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WatchingSessionServiceImpl implements WatchingSessionService {

    private final UserRepository userRepository;
    private final ContentRepository contentRepository;
    private final WatchingSessionRepository watchingSessionRepository;
    private final WatchingSessionCacheRepository watchingSessionCacheRepository;
    private final WatchingSessionMapper watchingSessionMapper;

    /**
     * 세션 조회 정책 고민 및 결정 필요
     *
     * 결정 포인트:
     * 1. Redis 단일 신뢰 모델로 갈 것인가?
     * 2. Redis 조회 후 DB 무결성 검증을 할 것인가?
     *    - 즉시 검증
     *    - 배치 / 리스너 기반 보정
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
    public WatchingSessionDto addWatchingSession(WatchingSessionCreateCommand command) {

        User watcher = userRepository.getReferenceById(command.watcherId());
        Content content = contentRepository.getReferenceById(command.contentId());

        WatchingSession session = new WatchingSession(watcher, content);
        WatchingSession saved = watchingSessionRepository.save(session);

        watchingSessionCacheRepository.save(saved);

        return watchingSessionMapper.toDto(saved);
    }

    @Override
    @Transactional
    public void removeWatchingSession(UUID sessionId) {

        watchingSessionRepository.deleteById(sessionId);
        watchingSessionCacheRepository.deleteBySessionId(sessionId);
    }
}