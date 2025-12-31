package com.mopl.api.domain.follow.dto.response;

import java.util.UUID;
import lombok.Builder;


@Builder
public record FollowDto(
    UUID id,
    UUID followeeId,
    UUID followerId
) {

}
