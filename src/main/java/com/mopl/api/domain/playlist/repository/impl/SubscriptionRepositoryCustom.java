package com.mopl.api.domain.playlist.repository.impl;

import com.mopl.api.domain.playlist.entity.Subscription;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;

public interface SubscriptionRepositoryCustom {

    @Query("select s from Subscription s join fetch s.user where s.playlist.id = :playlistId")
    List<Subscription> findSubscriptionsByPlaylistId(UUID playlistId);
}
