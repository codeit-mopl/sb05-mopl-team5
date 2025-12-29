package com.mopl.api.domain.playlist.service;

import com.mopl.api.domain.playlist.dto.request.PlaylistCreateRequest;
import com.mopl.api.domain.playlist.dto.request.PlaylistUpdateRequest;
import com.mopl.api.domain.playlist.dto.response.CursorResponsePlaylistDto;
import com.mopl.api.domain.playlist.dto.response.PlaylistDto;
import java.util.UUID;

public interface PlaylistService {

    PlaylistDto addPlaylist(PlaylistCreateRequest request, UUID userId);

    PlaylistDto modifyPlaylist(UUID playlistId, PlaylistUpdateRequest request, UUID userId);

    void removePlaylist(UUID playlistId, UUID userId);

    PlaylistDto getPlaylist(UUID playlistId, UUID currentUserId);

    CursorResponsePlaylistDto getPlaylists(
        String keywordLike,
        UUID ownerIdEqual,
        UUID subscriberIdEqual,
        String cursor,
        UUID idAfter,
        int limit,
        String sortBy,
        String sortDirection,
        UUID currentUserId
    );

    void addContentToPlaylist(UUID playlistId, UUID contentId, UUID userId);

    void removeContentFromPlaylist(UUID playlistId, UUID contentId, UUID userId);
}
