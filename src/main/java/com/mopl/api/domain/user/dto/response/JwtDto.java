package com.mopl.api.domain.user.dto.response;

import lombok.Builder;

@Builder
public record JwtDto(
    UserDto userDto,
    String accessToken
) {

}
