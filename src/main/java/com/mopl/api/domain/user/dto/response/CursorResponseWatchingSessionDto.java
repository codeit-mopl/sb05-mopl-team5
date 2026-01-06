package com.mopl.api.domain.user.dto.response;

import com.mopl.api.domain.content.dto.response.ContentDto;
import com.mopl.api.domain.user.entity.WatchingSession;
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