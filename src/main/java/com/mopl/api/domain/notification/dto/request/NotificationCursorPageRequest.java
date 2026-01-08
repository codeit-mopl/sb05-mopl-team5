package com.mopl.api.domain.notification.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Builder;

@Builder
public record NotificationCursorPageRequest(
    String cursor,
    UUID idAfter,
    @NotNull Integer limit,
    @NotNull SortDirection sortDirection,
    @NotNull SortBy sortBy
) {

    public enum SortDirection {
        ASCENDING,
        DESCENDING
    }

    public enum SortBy {
        createdAt
    }
}
