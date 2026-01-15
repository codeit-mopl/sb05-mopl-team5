package com.mopl.api.domain.playlist.repository;

import com.mopl.api.domain.playlist.entity.Subscription;
import com.mopl.api.domain.playlist.repository.impl.SubscriptionRepositoryCustom;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID>, SubscriptionRepositoryCustom {

    Optional<Subscription> findByUserIdAndPlaylistId(UUID userId, UUID playlistId);

    boolean existsByUserIdAndPlaylistId(UUID userId, UUID playlistId);
}