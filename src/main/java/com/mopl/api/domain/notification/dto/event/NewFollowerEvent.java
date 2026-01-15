package com.mopl.api.domain.notification.dto.event;

import java.util.UUID;
import lombok.Builder;

@Builder
public record NewFollowerEvent(
    UUID followeeId,
    UUID followerId,
    String followerName
) {

}