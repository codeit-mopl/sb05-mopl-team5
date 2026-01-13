package com.mopl.api.domain.user.repository;

import com.mopl.api.domain.user.entity.WatchingSession;
import com.mopl.api.domain.user.repository.impl.WatchingSessionRepositoryCustom;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WatchingSessionRepository extends JpaRepository<WatchingSession, UUID>,
    WatchingSessionRepositoryCustom {

    @EntityGraph(attributePaths = {"content", "watcher"})
    List<WatchingSession> findAllByWatcher_Id(UUID watcherId);

    @EntityGraph(attributePaths = {"content", "watcher"})
    Optional<WatchingSession> findByContent_IdAndWatcher_Id(UUID contentId, UUID watcherId);
}