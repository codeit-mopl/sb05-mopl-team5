package com.mopl.api.domain.user.dto.request;

import lombok.Builder;

@Builder
public record UserUpdateRequest(
    String name
) {

}
