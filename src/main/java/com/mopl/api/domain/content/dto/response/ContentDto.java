package com.mopl.api.domain.content.dto.response;

import com.mopl.api.domain.content.entity.Content;
import java.util.Arrays;
import java.util.Collections;
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

    public static ContentDto from(Content content) {
        List<String> tags = Collections.emptyList();
        if (content.getTags() != null && !content.getTags()
                                                 .isBlank()) {
            tags = Arrays.stream(content.getTags()
                                        .split(","))
                         .map(String::trim)
                         .filter(s -> !s.isEmpty())
                         .toList();
        }

        return ContentDto.builder()
                         .id(content.getId())
                         .type(content.getType()
                                      .getValue())
                         .title(content.getTitle())
                         .description(content.getDescription())
                         .thumbnailUrl(content.getThumbnailUrl())
                         .tags(tags)
                         .averageRating(content.getAverageRating() == null ? null : content.getAverageRating()
                                                                                           .doubleValue())
                         .reviewCount(content.getReviewCount())
                         .watcherCount(content.getWatcherCount())
                         .build();
    }

}
