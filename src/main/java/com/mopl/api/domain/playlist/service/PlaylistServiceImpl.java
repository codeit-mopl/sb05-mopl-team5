package com.mopl.api.domain.playlist.service;

import com.mopl.api.domain.content.repository.ContentRepository;
import com.mopl.api.domain.playlist.dto.request.PlaylistCreateRequest;
import com.mopl.api.domain.playlist.dto.request.PlaylistUpdateRequest;
import com.mopl.api.domain.playlist.dto.response.CursorResponsePlaylistDto;
import com.mopl.api.domain.playlist.dto.response.PlaylistDto;
import com.mopl.api.domain.playlist.mapper.PlaylistMapper;
import com.mopl.api.domain.playlist.repository.PlaylistContentRepository;
import com.mopl.api.domain.playlist.repository.PlaylistRepository;
import com.mopl.api.domain.playlist.repository.SubscriptionRepository;
import com.mopl.api.domain.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
        return new PlaylistDto(
            UUID.randomUUID(),
            request.title(),
            request.description(),
            0,
            false,
            true,
            LocalDateTime.now(),
            LocalDateTime.now()
        );
    }

    @Override
    @Transactional
    public PlaylistDto modifyPlaylist(UUID playlistId, PlaylistUpdateRequest request, UUID userId) {
        return new PlaylistDto(
            playlistId,
            request.title(),
            request.description(),
            0,
            false,
            true,
            LocalDateTime.now()
                         .minusDays(1),
            LocalDateTime.now()
        );
    }

    @Override
    @Transactional
    public void removePlaylist(UUID playlistId, UUID userId) {
    }

    @Override
    public PlaylistDto getPlaylist(UUID playlistId, UUID currentUserId) {
        return new PlaylistDto(
            playlistId,
            "Stub Playlist Title",
            "Stub playlist description",
            0,
            false,
            false,
            LocalDateTime.now()
                         .minusDays(1),
            LocalDateTime.now()
        );
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
