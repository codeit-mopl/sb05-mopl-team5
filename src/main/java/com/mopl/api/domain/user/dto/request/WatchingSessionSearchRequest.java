package com.mopl.api.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import org.hibernate.query.SortDirection;
import org.springdoc.core.annotations.ParameterObject;

@Builder
@ParameterObject
@Schema(description = "시청 세션 검색 조건")
public record WatchingSessionSearchRequest(
    String watcherNameLike,
    String cursor,
    String idAfter,
    @NotNull Integer limit,
    @NotNull SortDirection sortDirection,
    @NotNull SortBy sortBy
) {

    public WatchingSessionSearchRequest {
        if (limit == null) {
            limit = 20;
        }

        if (sortDirection == null) {
            sortDirection = SortDirection.DESCENDING;
        }

        if (sortBy == null) {
            sortBy = SortBy.createAt;
        }
    }

    public enum SortBy {
        createAt
    }
}