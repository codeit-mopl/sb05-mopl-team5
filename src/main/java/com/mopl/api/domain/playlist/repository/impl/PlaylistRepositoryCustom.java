package com.mopl.api.domain.playlist.repository.impl;

import com.mopl.api.domain.playlist.entity.Playlist;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface PlaylistRepositoryCustom {

    List<Playlist> findPlaylistsWithCursor(
        String keywordLike,
        UUID ownerIdEqual,
        UUID subscriberIdEqual,
        String sortBy,
        String sortDirection,
        LocalDateTime cursorDateTime,
        Long cursorLong,
        UUID idAfter,
        int limit
    );

    long countPlaylists(
        String keywordLike,
        UUID ownerIdEqual,
        UUID subscriberIdEqual
    );
}
