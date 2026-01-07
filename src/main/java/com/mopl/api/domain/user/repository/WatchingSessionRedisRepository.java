package com.mopl.api.domain.user.repository;

import com.mopl.api.domain.user.entity.WatchingSession;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WatchingSessionRedisRepository {

    Optional<WatchingSession> findSessionByUserId(UUID userId);

    List<WatchingSession> findSessionsByContentId(UUID contentId);

    void addWatcher(UUID contentId, UUID userId);

    void removeWatcher(UUID contentId, UUID userId);

    long countWatchers(UUID contentId);
}
