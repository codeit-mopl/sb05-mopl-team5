package com.mopl.api.domain.user.repository;

import java.util.Set;
import java.util.UUID;

public interface WatchingSessionRedisRepository {

    void addWatcher(UUID contentId, UUID userId);

    void removeWatcher(UUID contentId, UUID userId);

    long countWatchers(UUID contentId);

    boolean isWatching(UUID contentId, UUID userId);

    void addChangedContentId(UUID contentId);

    Set<String> popAllChangedIds();
}