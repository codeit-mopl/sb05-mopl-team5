package com.mopl.api.domain.playlist.dto.response;

import java.util.UUID;

import lombok.Builder;

@Builder
public record OwnerDto(

    UUID userId,
    String name,
    String profileImageUrl
) {

}