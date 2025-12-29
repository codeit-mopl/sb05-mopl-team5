package com.mopl.api.domain.review.dto.response;

import java.util.List;
import java.util.UUID;

public record CursorResponseReviewDto(
    List<ReviewDto> reviews,
    String nextCursor,
    UUID nextIdAfter,
    boolean hasNext,
    int totalCount,
    String sortBy,
    String sortDirection
) {
}
