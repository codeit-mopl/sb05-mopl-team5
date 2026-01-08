package com.mopl.api.domain.playlist.repository;

import com.mopl.api.domain.playlist.entity.Playlist;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface PlaylistRepositoryCustom {

    List<Playlist> findPlaylistsWithCursor(
        String keywordLike,
        UUID ownerIdEqual,
        UUID subscriberIdEqual,
        String sortBy,
        String sortDirection,
        Instant cursorInstant,
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
