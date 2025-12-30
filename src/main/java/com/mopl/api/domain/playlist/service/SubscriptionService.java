package com.mopl.api.domain.playlist.service;

import java.util.UUID;

public interface SubscriptionService {

    void subscribeToPlaylist(UUID playlistId, UUID userId);

    void unsubscribeFromPlaylist(UUID playlistId, UUID userId);

    boolean isUserSubscribed(UUID userId, UUID playlistId);
}
