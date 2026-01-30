package com.mopl.api.domain.content.dto.request;

import java.util.UUID;
import lombok.Builder;

@Builder
public record ContentSearchRequest(
    String typeEqual,
    String keywordLike,
    String cursor,
    UUID idAfter,
    Integer limit,
    String sortDirection,
    String sortBy
) {
    public ContentSearchRequest {
        if (limit == null || limit <= 0) {
            limit = 20;
        }
        if (sortDirection == null || sortDirection.isBlank()) {
            sortDirection = "DESCENDING";
        }
        if (sortBy == null || sortBy.isBlank()) {
            sortBy = "watcherCount";
        }
    }
}
