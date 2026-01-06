package com.mopl.api.domain.follow.dto.response;

import com.mopl.api.domain.follow.entity.Follow;
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
    public static FollowDto from(Follow follow) {
        return FollowDto.builder()
                        .id(follow.getId())
                        .followerId(follow.getFollower().getId())
                        .followeeId(follow.getFollowee().getId())
                        .createdAt(follow.getCreatedAt())
                        .build();
    }
}
