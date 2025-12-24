package com.mopl.api.domain.user.repository;

import com.mopl.api.domain.user.entity.WatchingSession;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WatchingSessionRepository extends JpaRepository<WatchingSession, UUID> {

}