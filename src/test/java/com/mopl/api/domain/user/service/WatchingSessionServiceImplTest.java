package com.mopl.api.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mopl.api.domain.content.entity.Content;
import com.mopl.api.domain.content.repository.ContentRepository;
import com.mopl.api.domain.user.dto.command.WatchingSessionCreateCommand;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WatchingSessionServiceImplTest {

    @Mock
    ContentRepository contentRepository;
    @Mock
    UserRepository userRepository;
    @Mock
    WatchingSessionRepository watchingSessionRepository;
    @Mock
    WatchingSessionRedisRepository watchingSessionCacheRepository;
    @Mock
    WatchingSessionMapper watchingSessionMapper;

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
    @DisplayName("REDIS O → 시청 세션 정상 반환")
    @Disabled("조회 정책 결정 후 구현 예정")
    void getWatchingSession_redisHit_success() {

        when(watchingSessionCacheRepository.findSessionByUserId(watcherId))
            .thenReturn(Optional.of(session));

        WatchingSessionDto result =
            watchingSessionService.getWatchingSession(watcherId);

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("REDIS X → 예외 발생")
    @Disabled("조회 정책 결정 후 구현 예정")
    void getWatchingSession_redisMiss_dbMiss_shouldThrow() {

        when(watchingSessionCacheRepository.findSessionByUserId(watcherId))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
            watchingSessionService.getWatchingSession(watcherId)
        ).isInstanceOf(WatchingSessionNotFoundException.class);
    }

    @Test
    @DisplayName("contentId 기준 REDIS O → 커서 응답 반환")
    @Disabled("조회 정책 결정 후 구현 예정")
    void getWatchingSessionByContent_redisHit_success() {

        when(watchingSessionCacheRepository.findSessionsByContentId(contentId))
            .thenReturn(List.of(session));

        CursorResponseWatchingSessionDto result =
            watchingSessionService.getWatchingSession(contentId, request);

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("contentId 기준 REDIS X → 빈 커서 응답")
    @Disabled("조회 정책 결정 후 구현 예정")
    void getWatchingSessionByContent_redisMiss_shouldReturnEmptyCursor() {

        when(watchingSessionCacheRepository.findSessionsByContentId(contentId))
            .thenReturn(List.of());

        CursorResponseWatchingSessionDto result =
            watchingSessionService.getWatchingSession(contentId, request);

        assertThat(result).isNotNull();
        assertThat(result.data()).isEmpty();
    }

    @Test
    @DisplayName("웹소켓 연결 시 DB + Redis 세션 생성")
    void addWatchingSession_shouldSaveDbAndRedis() {

        WatchingSessionCreateCommand command = mock(WatchingSessionCreateCommand.class);

        when(command.watcherId()).thenReturn(watcherId);
        when(command.contentId()).thenReturn(contentId);

        when(userRepository.findById(watcherId))
            .thenReturn(Optional.of(watcher));
        when(contentRepository.findById(contentId))
            .thenReturn(Optional.of(content));

        when(watchingSessionRepository.save(any(WatchingSession.class)))
            .thenReturn(session);

        WatchingSessionDto mappedDto = WatchingSessionDto.builder()
                                                         .build();
        when(watchingSessionMapper.toDto(session))
            .thenReturn(mappedDto);

        WatchingSessionChange result =
            watchingSessionService.joinWatchingSession(contentId, watcherId);

        verify(watchingSessionRepository).save(any(WatchingSession.class));
        verify(watchingSessionCacheRepository).addWatcher(contentId, watcherId);
        verify(watchingSessionMapper).toDto(session);

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("유저가 존재하지 않으면 예외 발생")
    void addWatchingSession_userNotFound_shouldThrow() {

        when(userRepository.findById(watcherId))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
            watchingSessionService.joinWatchingSession(contentId, watcherId)
        ).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("웹소켓 해제 / 필요 시 DB + Redis 세션 제거")
    void leaveWatchingSession_shouldDeleteBoth() {

        UUID sessionId = UUID.randomUUID();

        when(watchingSessionRepository.findByContentIdAndWatcherId(contentId, watcherId))
            .thenReturn(Optional.of(session));

        WatchingSessionChange result = watchingSessionService.leaveWatchingSession(contentId, watcherId);

        verify(watchingSessionRepository).deleteById(sessionId);
        verify(watchingSessionCacheRepository).removeWatcher(contentId, watcherId);

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("존재하지 않는 세션 제거 시 예외 발생")
    void leaveWatchingSession_notFound_shouldThrow() {

        UUID sessionId = UUID.randomUUID();

        when(watchingSessionRepository.existsById(sessionId))
            .thenReturn(false);

        assertThatThrownBy(() ->
            watchingSessionService.leaveWatchingSession(contentId, watcherId)
        ).isInstanceOf(WatchingSessionNotFoundException.class);
    }
}