package com.mopl.api.domain.playlist.service;

import com.mopl.api.domain.content.entity.Content;
import com.mopl.api.domain.content.repository.ContentRepository;
import com.mopl.api.domain.playlist.dto.request.PlaylistCreateRequest;
import com.mopl.api.domain.playlist.dto.request.PlaylistUpdateRequest;
import com.mopl.api.domain.playlist.dto.response.CursorResponsePlaylistDto;
import com.mopl.api.domain.playlist.dto.response.PlaylistDto;
import com.mopl.api.domain.playlist.entity.Playlist;
import com.mopl.api.domain.playlist.entity.PlaylistContent;
import com.mopl.api.domain.playlist.exception.detail.ContentAlreadyExistsException;
import com.mopl.api.domain.playlist.exception.detail.ContentNotFoundException;
import com.mopl.api.domain.playlist.exception.detail.ContentNotInPlaylistException;
import com.mopl.api.domain.playlist.exception.detail.PlaylistNotFoundException;
import com.mopl.api.domain.playlist.exception.detail.PlaylistUnauthorizedException;
import com.mopl.api.domain.playlist.exception.detail.UserNotFoundException;
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
                                  .orElseThrow(() -> UserNotFoundException.withUserId(userId));

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
                                              .orElseThrow(() -> PlaylistNotFoundException.withPlaylistId(playlistId));

        if (!playlist.getOwner()
                     .getId()
                     .equals(userId)) {
            throw PlaylistUnauthorizedException.withDetails(playlistId, userId);
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
                                              .orElseThrow(() -> PlaylistNotFoundException.withPlaylistId(playlistId));

        if (!playlist.getOwner()
                     .getId()
                     .equals(userId)) {
            throw PlaylistUnauthorizedException.withDetails(playlistId, userId);
        }

        playlist.softDelete();
        playlistRepository.save(playlist);
    }

    @Override
    public PlaylistDto getPlaylist(UUID playlistId, UUID currentUserId) {
        Playlist playlist = playlistRepository.findById(playlistId)
                                              .orElseThrow(() -> PlaylistNotFoundException.withPlaylistId(playlistId));

        if (playlist.getIsDeleted()) {
            throw PlaylistNotFoundException.withPlaylistId(playlistId);
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
        Playlist playlist = playlistRepository.findById(playlistId)
                                              .orElseThrow(() -> PlaylistNotFoundException.withPlaylistId(playlistId));

        if (!playlist.getOwner()
                     .getId()
                     .equals(userId)) {
            throw PlaylistUnauthorizedException.withDetails(playlistId, userId);
        }

        Content content = contentRepository.findById(contentId)
                                           .orElseThrow(() -> ContentNotFoundException.withContentId(contentId));

        if (playlistContentRepository.existsByPlaylistIdAndContentIdAndIsDeletedFalse(playlistId, contentId)) {
            throw ContentAlreadyExistsException.withDetails(playlistId, contentId);
        }

        PlaylistContent playlistContent = PlaylistContent.create(playlist, content);
        playlistContentRepository.save(playlistContent);
    }

    @Override
    @Transactional
    public void removeContentFromPlaylist(UUID playlistId, UUID contentId, UUID userId) {
        Playlist playlist = playlistRepository.findById(playlistId)
                                              .orElseThrow(() -> PlaylistNotFoundException.withPlaylistId(playlistId));

        if (!playlist.getOwner()
                     .getId()
                     .equals(userId)) {
            throw PlaylistUnauthorizedException.withDetails(playlistId, userId);
        }

        PlaylistContent playlistContent = playlistContentRepository.findByPlaylistIdAndContentId(playlistId, contentId)
                                                                   .orElseThrow(() -> ContentNotInPlaylistException.withDetails(playlistId, contentId));

        playlistContent.softDelete();
        playlistContentRepository.save(playlistContent);
    }
}
