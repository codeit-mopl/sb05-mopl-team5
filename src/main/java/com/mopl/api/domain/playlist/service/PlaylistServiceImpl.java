package com.mopl.api.domain.playlist.service;

import com.mopl.api.domain.content.repository.ContentRepository;
import com.mopl.api.domain.playlist.dto.request.PlaylistCreateRequest;
import com.mopl.api.domain.playlist.dto.request.PlaylistUpdateRequest;
import com.mopl.api.domain.playlist.dto.response.CursorResponsePlaylistDto;
import com.mopl.api.domain.playlist.dto.response.PlaylistDto;
import com.mopl.api.domain.playlist.entity.Playlist;
import com.mopl.api.domain.playlist.entity.PlaylistContent;
import com.mopl.api.domain.playlist.mapper.PlaylistMapper;
import com.mopl.api.domain.playlist.repository.PlaylistContentRepository;
import com.mopl.api.domain.playlist.repository.PlaylistRepository;
import com.mopl.api.domain.playlist.repository.SubscriptionRepository;
import com.mopl.api.domain.user.entity.User;
import com.mopl.api.domain.user.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaylistServiceImpl implements PlaylistService {

    private final PlaylistRepository playlistRepository;
    private final PlaylistContentRepository playlistContentRepository;
    private final ContentRepository contentRepository;
    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PlaylistMapper playlistMapper;

    @Override
    @Transactional
    public PlaylistDto addPlaylist(PlaylistCreateRequest request, UUID userId) {

        User user = userRepository.findById(userId)
                                  .orElseThrow(
                                      () -> new RuntimeException("User not found with id: " + userId));

        Playlist playlist = Playlist.create(user, request.title(), request.description());
        playlistRepository.save(playlist);

        List<PlaylistContent> playlistContents = playlistContentRepository.findByPlaylistIdAndIsDeletedFalse(
            playlist.getId());
        boolean isOwner = true;
        boolean subscribedByMe = false;
        return playlistMapper.toDto(playlist, playlistContents, subscribedByMe, isOwner);
    }

    @Override
    @Transactional
    public PlaylistDto modifyPlaylist(UUID playlistId, PlaylistUpdateRequest request, UUID userId) {
        Playlist playlist = playlistRepository.findById(playlistId)
                                              .orElseThrow(() -> new RuntimeException(
                                                  "Playlist not found with id: " + playlistId));

        if (!playlist.getOwner()
                     .getId()
                     .equals(userId)) {
            throw new RuntimeException("User is not authorized to modify this playlist");
        }

        playlist.update(request.title(), request.description());
        playlistRepository.save(playlist);

        List<PlaylistContent> playlistContents = playlistContentRepository.findByPlaylistIdAndIsDeletedFalse(
            playlistId);
        boolean isOwner = true;
        boolean subscribedByMe = false;
        return playlistMapper.toDto(playlist, playlistContents, subscribedByMe, isOwner);
    }

    @Override
    @Transactional
    public void removePlaylist(UUID playlistId, UUID userId) {
        Playlist playlist = playlistRepository.findById(playlistId)
                                              .orElseThrow(() -> new RuntimeException(
                                                  "Playlist not found with id: " + playlistId));

        if (!playlist.getOwner()
                     .getId()
                     .equals(userId)) {
            throw new RuntimeException("User is not authorized to delete this playlist");
        }

        playlist.softDelete();
        playlistRepository.save(playlist);
    }

    @Override
    public PlaylistDto getPlaylist(UUID playlistId, UUID currentUserId) {
        Playlist playlist = playlistRepository.findById(playlistId)
                                              .orElseThrow(() -> new RuntimeException(
                                                  "Playlist not found with id: " + playlistId));

        if (playlist.getIsDeleted()) {
            throw new RuntimeException("Playlist not found with id: " + playlistId);
        }

        List<PlaylistContent> playlistContents = playlistContentRepository.findByPlaylistIdAndIsDeletedFalse(
            playlistId);
        boolean isOwner = playlist.getOwner()
                                  .getId()
                                  .equals(currentUserId);
        boolean subscribedByMe = subscriptionRepository.existsByUserIdAndPlaylistId(currentUserId, playlistId);

        return playlistMapper.toDto(playlist, playlistContents, subscribedByMe, isOwner);
    }

    @Override
    public CursorResponsePlaylistDto getPlaylists(
        String keywordLike,
        UUID ownerIdEqual,
        UUID subscriberIdEqual,
        String cursor,
        UUID idAfter,
        int limit,
        String sortBy,
        String sortDirection,
        UUID currentUserId
    ) {
        return new CursorResponsePlaylistDto(
            new ArrayList<>(),
            null,
            null,
            false,
            0,
            sortBy,
            sortDirection
        );
    }

    @Override
    @Transactional
    public void addContentToPlaylist(UUID playlistId, UUID contentId, UUID userId) {
    }

    @Override
    @Transactional
    public void removeContentFromPlaylist(UUID playlistId, UUID contentId, UUID userId) {
    }
}
