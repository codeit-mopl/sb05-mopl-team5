package com.mopl.api.domain.user.dto.response;

import java.util.List;
import lombok.Builder;

@Builder
public record CursorResponseWatchingSessionDto(
    List<WatchingSessionDto> data,
    String nextCursor,
    String nextIdAfter,
    boolean hasNext,
    Integer totalCount,
    String sortBy,
    String sortDirection
) {

}