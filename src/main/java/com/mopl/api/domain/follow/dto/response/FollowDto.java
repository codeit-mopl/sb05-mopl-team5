package com.mopl.api.domain.follow.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;

@Builder
public record FollowDto(
    UUID id,
    UUID followerId,
    UUID followeeId,
    LocalDateTime createdAt
) {

}
