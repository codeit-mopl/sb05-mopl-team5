package com.mopl.api.domain.review.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;

@Builder
public record ReviewDto(
    UUID id,
    UUID contentId,
    AuthorDto author,
    String text,
    double rating,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    boolean isAuthor
) {

}
