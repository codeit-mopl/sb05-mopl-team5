package com.mopl.api.domain.user.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.mopl.api.domain.user.entity.WatchingSession;
import com.mopl.api.domain.user.repository.UserRepository;
import com.mopl.api.domain.user.repository.WatchingSessionCacheRepository;
import com.mopl.api.domain.user.repository.WatchingSessionRepository;
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


    @BeforeEach
    void setUp() {
        // TODO 테스트에 필요한 세팅
    }

    @Test
    @DisplayName("사용자 세션이 REDIS에는 있지만 DB에는 없는 경우")
    void getWatchingSession_redisHit_but_dbMiss_shouldThrowException() {
        UUID watcherId = UUID.randomUUID();
        WatchingSession session = null;

        when(watchingSessionCacheRepository.findSessionByUserId(watcherId))
            .thenReturn(Optional.of(session));

        when(watchingSessionRepository.existsById(session.getId()))
            .thenReturn(false);

        // TODO 나중에 발생할 예외 정리
        assertThatThrownBy(() ->
            watchingSessionService.getWatchingSession(watcherId)
        ).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("사용자 세션이 REDIS, DB에 둘 다 없는 경우")
    void getWatchingSession_redisMiss_dbMiss_shouldThrowException() {

        UUID watcherId = UUID.randomUUID();

        when(watchingSessionCacheRepository.findSessionByUserId(watcherId))
            .thenReturn(Optional.empty());

        when(watchingSessionRepository.findByWatcherId(watcherId))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
            watchingSessionService.getWatchingSession(watcherId)
        ).isInstanceOf(RuntimeException.class);
    }
}