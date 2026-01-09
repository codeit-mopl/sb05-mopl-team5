package com.mopl.api.domain.review.mapper;

import com.mopl.api.domain.review.dto.response.ReviewDto;
import com.mopl.api.domain.review.entity.Review;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReviewMapper {

    @Mapping(target = "id", source = "review.id")
    @Mapping(target = "contentId", source = "review.content.id")
    @Mapping(target = "author", source = "review.user")
    @Mapping(target = "text", source = "review.text")
    @Mapping(target = "rating", source = "review.rating")
    @Mapping(target = "createdAt", expression = "java(toLocalDateTime(review.getCreatedAt()))")
    @Mapping(target = "updatedAt", expression = "java(toLocalDateTime(review.getUpdatedAt()))")
    @Mapping(target = "isAuthor", source = "isAuthor")
    ReviewDto toDto(Review review, boolean isAuthor);

    default LocalDateTime toLocalDateTime(LocalDateTime dateTime) {
        return dateTime;
    }

    default LocalDateTime toLocalDateTime(Instant instant) {
        if (instant == null) {
            return null;
        }
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }
}
