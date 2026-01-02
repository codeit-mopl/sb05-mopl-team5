package com.mopl.api.domain.user.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mopl.api.domain.content.entity.Content;
import com.mopl.api.domain.user.dto.command.WatchingSessionCreateCommand;
import com.mopl.api.domain.user.dto.request.WatchingSessionSearchRequest;
import com.mopl.api.domain.user.dto.response.CursorResponseWatchingSessionDto;
import com.mopl.api.domain.user.dto.response.WatchingSessionDto;
import com.mopl.api.domain.user.entity.User;
import com.mopl.api.domain.user.entity.WatchingSession;
import com.mopl.api.domain.user.exception.detail.WatchingSessionNotFoundException;
import com.mopl.api.domain.user.repository.WatchingSessionCacheRepository;
import com.mopl.api.domain.user.repository.WatchingSessionRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WatchingSessionServiceImplTest {

    @Mock
    WatchingSessionRepository watchingSessionRepository;

    @Mock
    WatchingSessionCacheRepository watchingSessionCacheRepository;

    @InjectMocks
    WatchingSessionServiceImpl watchingSessionService;

    UUID watcherId;
    User watcher;
    UUID contentId;
    Content content;
    WatchingSession session;
    WatchingSessionSearchRequest request;

    @BeforeEach
    void setUp() {
        // TODO 테스트에 필요한 세팅
        watcherId = UUID.randomUUID();
        contentId = UUID.randomUUID();

        watcher = mock(User.class);
        content = mock(Content.class);

        session = new WatchingSession(watcher, content);

        request = WatchingSessionSearchRequest.builder()
                                              .limit(10)
                                              .sortBy(WatchingSessionSearchRequest.SortBy.createdAt)
                                              .sortDirection(WatchingSessionSearchRequest.SortDirection.DESCENDING)
                                              .cursor(null)
                                              .idAfter(null)
                                              .watcherNameLike(null)
                                              .build();
    }

    // TODO DB 조회에 대한 고민을 하고 정리해야할 듯
    @Test
    @DisplayName("REDIS O, DB O → 정상 반환")
    void getWatchingSession_redisHit_dbHit_success() {

        WatchingSession spySession = spy(session);
        UUID sessionId = UUID.randomUUID();
        doReturn(sessionId).when(spySession)
                           .getId();

        when(watchingSessionCacheRepository.findSessionByUserId(watcherId))
            .thenReturn(Optional.of(spySession));

        when(watchingSessionRepository.existsById(sessionId))
            .thenReturn(true);

        WatchingSessionDto result =
            watchingSessionService.getWatchingSession(watcherId);

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("REDIS O, DB X → Redis 세션 제거 후 예외")
    void getWatchingSession_redisHit_dbMiss_shouldExpireAndThrow() {

        WatchingSession spySession = spy(session);
        UUID sessionId = UUID.randomUUID();
        doReturn(sessionId).when(spySession)
                           .getId();

        when(watchingSessionCacheRepository.findSessionByUserId(watcherId))
            .thenReturn(Optional.of(spySession));

        when(watchingSessionRepository.existsById(sessionId))
            .thenReturn(false);

        assertThatThrownBy(() ->
            watchingSessionService.getWatchingSession(watcherId)
        ).isInstanceOf(WatchingSessionNotFoundException.class);

        verify(watchingSessionCacheRepository)
            .deleteBySessionId(sessionId);
    }

    @Test
    @DisplayName("REDIS X, DB O → DB 조회 성공")
    void getWatchingSession_redisMiss_dbHit_success() {

        when(watchingSessionCacheRepository.findSessionByUserId(watcherId))
            .thenReturn(Optional.empty());

        when(watchingSessionRepository.findByWatcherId(watcherId))
            .thenReturn(Optional.of(session));

        WatchingSessionDto result =
            watchingSessionService.getWatchingSession(watcherId);

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("REDIS X, DB X → 예외 발생")
    void getWatchingSession_redisMiss_dbMiss_shouldThrow() {

        when(watchingSessionCacheRepository.findSessionByUserId(watcherId))
            .thenReturn(Optional.empty());

        when(watchingSessionRepository.findByWatcherId(watcherId))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
            watchingSessionService.getWatchingSession(watcherId)
        ).isInstanceOf(WatchingSessionNotFoundException.class);
    }

    @Test
    @DisplayName("웹소켓 연결 시 DB + Redis 세션 생성")
    void addWatchingSession_shouldSaveDbAndRedis() {

        WatchingSessionCreateCommand command = mock(WatchingSessionCreateCommand.class);

        when(watchingSessionRepository.save(any(WatchingSession.class)))
            .thenReturn(session);

        WatchingSessionDto result =
            watchingSessionService.addWatchingSession(command);

        verify(watchingSessionRepository).save(any(WatchingSession.class));
        verify(watchingSessionCacheRepository).save(any(WatchingSession.class));

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("웹소켓 해제 시 / 필요 시 DB + Redis 세션 제거")
    void removeWatchingSession_shouldDeleteBoth() {

        UUID sessionId = UUID.randomUUID();

        watchingSessionService.removeWatchingSession(sessionId);

        verify(watchingSessionRepository).deleteById(sessionId);
        verify(watchingSessionCacheRepository).deleteBySessionId(sessionId);
    }

    @Test
    @DisplayName("contentId 기준 REDIS O → 커서 응답 반환")
    void getWatchingSessionByContent_redisHit_success() {

        when(watchingSessionCacheRepository.findSessionsByContentId(
            contentId)
        ).thenReturn(List.of(session));

        CursorResponseWatchingSessionDto result =
            watchingSessionService.getWatchingSession(contentId, request);

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("contentId 기준 REDIS X, DB X → 빈 커서 응답")
    void getWatchingSessionByContent_redisMiss_dbMiss_shouldReturnEmptyCursor() {

        when(watchingSessionCacheRepository.findSessionsByContentId(
            contentId)
        ).thenReturn(List.of());

        CursorResponseWatchingSessionDto result =
            watchingSessionService.getWatchingSession(contentId, request);

        assertThat(result).isNotNull();
        assertThat(result.data()
                         .size()).isEqualTo(0);
    }
}