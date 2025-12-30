package com.mopl.api.domain.user.dto.response;

import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record CursorResponseWatchingSessionDto(
    List<WatchingSessionDto> data,
    String nextCursor,
    UUID nextIdAfter,
    boolean hasNext,
    Long totalCount,
    String sortBy,
    String sortDirection
) {

}