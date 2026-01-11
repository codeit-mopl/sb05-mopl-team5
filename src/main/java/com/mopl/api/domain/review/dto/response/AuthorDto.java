package com.mopl.api.domain.review.dto.response;

import java.util.UUID;

import lombok.Builder;

@Builder
public record AuthorDto(

    UUID userId,
    String name,
    String profileImageUrl
) {

}