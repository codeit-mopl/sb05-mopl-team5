package com.mopl.api.domain.content.dto.response;

import java.util.List;
import java.util.UUID;

public record CursorResponseContentDto(
    List<ContentDto> data,
    String nextCursor,
    UUID nextIdAfter,
    Boolean hasNext,
    Long totalCount,
    String sortBy,
    String sortDirection
) {

}
