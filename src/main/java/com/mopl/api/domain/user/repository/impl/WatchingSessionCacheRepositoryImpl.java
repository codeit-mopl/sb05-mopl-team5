package com.mopl.api.domain.user.repository.impl;

import com.mopl.api.domain.user.entity.WatchingSession;
import com.mopl.api.domain.user.repository.WatchingSessionCacheRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class WatchingSessionCacheRepositoryImpl implements WatchingSessionCacheRepository {

    @Override
    public Optional<WatchingSession> findSessionByUserId(UUID userId) {
        return Optional.empty();
    }

    @Override
    public void deleteBySessionId(UUID sessionId) {

    }

    @Override
    public void save(WatchingSession watchingSession) {
    }

    @Override
    public List<WatchingSession> findSessionsByContentId(UUID contentId) {
        return List.of();
    }
}
