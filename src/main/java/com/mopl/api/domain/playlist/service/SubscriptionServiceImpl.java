package com.mopl.api.domain.playlist.service;

import com.mopl.api.domain.playlist.entity.Playlist;
import com.mopl.api.domain.playlist.entity.Subscription;
import com.mopl.api.domain.playlist.exception.detail.DuplicateSubscriptionException;
import com.mopl.api.domain.playlist.exception.detail.PlaylistNotFoundException;
import com.mopl.api.domain.playlist.exception.detail.SelfSubscriptionException;
import com.mopl.api.domain.playlist.exception.detail.SubscriptionNotFoundException;
import com.mopl.api.domain.playlist.repository.PlaylistRepository;
import com.mopl.api.domain.playlist.repository.SubscriptionRepository;
import com.mopl.api.domain.user.entity.User;
import com.mopl.api.domain.user.exception.user.detail.UserNotFoundException;
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
        Playlist playlist = playlistRepository.findById(playlistId)
                                              .orElseThrow(() -> PlaylistNotFoundException.withPlaylistId(playlistId));

        if (playlist.getOwner()
                    .getId()
                    .equals(userId)) {
            throw SelfSubscriptionException.withDetails(playlistId, userId);
        }

        if (subscriptionRepository.existsByUserIdAndPlaylistId(userId, playlistId)) {
            throw DuplicateSubscriptionException.withDetails(playlistId, userId);
        }

        User user = userRepository.findById(userId)
                                  .orElseThrow(() -> UserNotFoundException.withUserId(userId));

        Subscription subscription = Subscription.create(user, playlist);
        subscriptionRepository.save(subscription);

        playlist.incrementSubscriberCount();
        playlistRepository.save(playlist);
    }

    @Override
    @Transactional
    public void unsubscribeFromPlaylist(UUID playlistId, UUID userId) {
        Playlist playlist = playlistRepository.findById(playlistId)
                                              .orElseThrow(() -> PlaylistNotFoundException.withPlaylistId(playlistId));

        Subscription subscription = subscriptionRepository.findByUserIdAndPlaylistId(userId, playlistId)
                                                          .orElseThrow(() -> SubscriptionNotFoundException.withDetails(playlistId, userId));

        subscriptionRepository.delete(subscription);

        playlist.decrementSubscriberCount();
        playlistRepository.save(playlist);
    }

    @Override
    public boolean isUserSubscribed(UUID userId, UUID playlistId) {
        return subscriptionRepository.existsByUserIdAndPlaylistId(userId, playlistId);
    }
}
