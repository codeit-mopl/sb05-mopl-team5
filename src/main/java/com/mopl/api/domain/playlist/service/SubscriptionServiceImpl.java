package com.mopl.api.domain.playlist.service;

import com.mopl.api.domain.playlist.repository.PlaylistRepository;
import com.mopl.api.domain.playlist.repository.SubscriptionRepository;
import com.mopl.api.domain.user.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final PlaylistRepository playlistRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void subscribeToPlaylist(UUID playlistId, UUID userId) {
    }

    @Override
    @Transactional
    public void unsubscribeFromPlaylist(UUID playlistId, UUID userId) {
    }

    @Override
    public boolean isUserSubscribed(UUID userId, UUID playlistId) {
        return false;
    }
}
