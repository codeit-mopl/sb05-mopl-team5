package com.mopl.api.domain.user.repository;

import com.mopl.api.domain.user.entity.WatchingSession;
import com.mopl.api.domain.user.repository.impl.WatchingSessionRepositoryCustom;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WatchingSessionRepository extends JpaRepository<WatchingSession, UUID>,
    WatchingSessionRepositoryCustom {

    Optional<WatchingSession> findByWatcherId(UUID watcherId);

    Optional<WatchingSession> findByContentIdAndWatcherId(UUID contentId, UUID watcherId);
}