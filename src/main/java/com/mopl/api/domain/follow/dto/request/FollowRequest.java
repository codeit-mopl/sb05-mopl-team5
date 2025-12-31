package com.mopl.api.domain.follow.dto.request;

import java.util.UUID;
import lombok.Builder;

@Builder
public record FollowRequest(

    UUID followeeId) {

}
