package com.mopl.api.domain.playlist.repository.impl;

import com.mopl.api.domain.playlist.entity.Subscription;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;

public interface SubscriptionRepositoryCustom {

    @Query("select s from Subscription s join fetch s.user where s.playlist.id = :playlistId")
    List<Subscription> findSubscriptionsByPlaylistId(UUID playlistId);

    @Query("select s.playlist.id from Subscription s where s.user.id = :userId and s.playlist.id in :playlistIds")
    List<UUID> findPlaylistIdsByUserIdAndPlaylistIdIn(UUID userId, List<UUID> playlistIds);
}
