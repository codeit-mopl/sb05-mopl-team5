package com.mopl.api.domain.user.repository;

import com.mopl.api.domain.user.entity.WatchingSession;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WatchingSessionCacheRepository {

    Optional<WatchingSession> findSessionByUserId(UUID userId);

    void deleteBySessionId(UUID sessionId);

    WatchingSession save(WatchingSession watchingSession);

    List<WatchingSession> findSessionsByContentId(UUID contentId);
}
