package com.mopl.api.domain.playlist.repository;

import com.mopl.api.domain.playlist.entity.Subscription;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    boolean existsByUserIdAndPlaylistId(UUID userId, UUID playlistId);
}