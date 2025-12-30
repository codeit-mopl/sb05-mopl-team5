package com.mopl.api.domain.playlist.dto.response;

import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record CursorResponsePlaylistDto(
    List<PlaylistDto> playlists,
    String nextCursor,
    UUID nextIdAfter,
    boolean hasNext,
    int totalCount,
    String sortBy,
    String sortDirection
) {

}
