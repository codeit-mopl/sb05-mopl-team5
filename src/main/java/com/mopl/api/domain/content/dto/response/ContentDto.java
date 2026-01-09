package com.mopl.api.domain.content.dto.response;

import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record ContentDto(
    UUID id,
    String type,
    String title,
    String description,
    String thumbnailUrl,
    List<String> tags,
    Double averageRating,
    Long reviewCount,
    Long watcherCount
) {

}