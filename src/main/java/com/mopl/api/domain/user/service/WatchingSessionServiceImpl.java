package com.mopl.api.domain.user.service;

import com.mopl.api.domain.content.entity.Content;
import com.mopl.api.domain.content.exception.detail.ContentNotFoundException;
import com.mopl.api.domain.content.repository.ContentRepository;
import com.mopl.api.domain.notification.dto.event.FolloweeWatchingStartedEvent;
import com.mopl.api.domain.user.dto.event.WatchingSessionChangeEvent;
import com.mopl.api.domain.user.dto.request.WatchingSessionSearchRequest;
import com.mopl.api.domain.user.dto.response.CursorResponseWatchingSessionDto;
import com.mopl.api.domain.user.dto.response.WatchingSessionDto;
import com.mopl.api.domain.user.entity.User;
import com.mopl.api.domain.user.entity.WatchingSession;
import com.mopl.api.domain.user.exception.user.detail.UserNotFoundException;
import com.mopl.api.domain.user.exception.watching.detail.WatchingSessionNotFoundException;
import com.mopl.api.domain.user.mapper.WatchingSessionMapper;
import com.mopl.api.domain.user.repository.UserRepository;
import com.mopl.api.domain.user.repository.WatchingSessionRedisRepository;
import com.mopl.api.domain.user.repository.WatchingSessionRepository;
import com.mopl.api.global.config.websocket.dto.WatchingSessionChange;
import com.mopl.api.global.config.websocket.dto.WatchingSessionChange.ChangeType;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
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

    @Override
    public WatchingSessionDto getWatchingSession(UUID watcherId) {

        List<WatchingSession> sessions = watchingSessionRepository.findAllByWatcher_Id(watcherId);

        WatchingSession latestSession = sessions.stream()
                                                .max(Comparator.comparing(WatchingSession::getCreatedAt))
                                                .orElse(null);

        return watchingSessionMapper.toDto(latestSession);
    }

    @Override
    public CursorResponseWatchingSessionDto getWatchingSession(UUID contentId,
        WatchingSessionSearchRequest request) {
        List<WatchingSession> sessions = watchingSessionRepository.searchSessions(contentId, request);

        boolean hasNext = sessions.size() > request.limit();

        List<WatchingSession> resultData = hasNext
            ? sessions.subList(0, request.limit())
            : sessions;

        String nextCursor = null;
        UUID nextIdAfter = null;

        if (!resultData.isEmpty() && hasNext) {
            WatchingSession lastRecord = resultData.get(resultData.size() - 1);
            nextCursor = lastRecord.getCreatedAt()
                                   .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            nextIdAfter = lastRecord.getId();
        }

        long totalCount = watchingSessionCacheRepository.countWatchers(contentId);

        return CursorResponseWatchingSessionDto.builder()
                                               .data(watchingSessionMapper.toDtoList(resultData))
                                               .nextCursor(nextCursor)
                                               .nextIdAfter(nextIdAfter)
                                               .hasNext(hasNext)
                                               .totalCount(totalCount)
                                               .sortBy(request.sortBy()
                                                              .name())
                                               .sortDirection(request.sortDirection()
                                                                     .name())
                                               .build();
    }

    @Override
    @Transactional
    public WatchingSessionChange joinWatchingSession(UUID contentId, UUID watcherId) {

        User watcher = userRepository.findById(watcherId)
                                     .orElseThrow(() -> UserNotFoundException.withUserId(watcherId));
        Content content = contentRepository.findById(contentId)
                                           .orElseThrow(() -> ContentNotFoundException.withContentId(contentId));

        Optional<WatchingSession> existing = watchingSessionRepository.findByContent_IdAndWatcher_Id(contentId,
            watcherId);
        if (existing.isPresent()) {
            return WatchingSessionChange.builder()
                                        .watchingSession(watchingSessionMapper.toDto(existing.get()))
                                        .type(ChangeType.JOIN)
                                        .watcherCount(watchingSessionCacheRepository.countWatchers(contentId))
                                        .build();
        }

        log.debug("세션 생성 시작 contentId: {}, watcherId: {}", contentId, watcherId);

        WatchingSession session = new WatchingSession(watcher, content);
        WatchingSession saved = watchingSessionRepository.saveAndFlush(session);
        log.debug("DB 저장 완료");

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

        // 알림
        eventPublisher.publishEvent(FolloweeWatchingStartedEvent.builder()
                                                                .watchingSessionId(session.getId())
                                                                .watcherId(session.getWatcher()
                                                                                  .getId())
                                                                .contentId(session.getContent()
                                                                                  .getId())
                                                                .contentTitle(session.getContent()
                                                                                     .getTitle())
                                                                .build());

        log.debug("join 이벤트 발행 완료");

        return change;
    }

    @Override
    @Transactional
    public WatchingSessionChange leaveWatchingSession(UUID contentId, UUID watcherId) {

        WatchingSession session = watchingSessionRepository.findByContent_IdAndWatcher_Id(contentId, watcherId)
                                                           .orElse(null);

        if (session == null) {
            throw WatchingSessionNotFoundException.withContentIdAndWatcherId(contentId, watcherId);
        }

        watchingSessionRepository.delete(session);
        log.debug("DB 삭제 완료");

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