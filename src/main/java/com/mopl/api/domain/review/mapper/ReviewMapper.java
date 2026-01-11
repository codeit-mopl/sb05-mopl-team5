package com.mopl.api.domain.review.mapper;

import com.mopl.api.domain.review.dto.response.AuthorDto;
import com.mopl.api.domain.review.dto.response.ReviewDto;
import com.mopl.api.domain.review.entity.Review;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReviewMapper {

    @Mapping(target = "id", source = "review.id")
    @Mapping(target = "contentId", source = "review.content.id")
    @Mapping(target = "author", expression = "java(mapAuthor(review))")
    @Mapping(target = "text", source = "review.text")
    @Mapping(target = "rating", source = "review.rating")
    @Mapping(target = "isAuthor", source = "isAuthor")
    ReviewDto toDto(Review review, boolean isAuthor);

    default AuthorDto mapAuthor(Review review) {
        return AuthorDto.builder()
            .userId(review.getUser().getId())
            .name(review.getUser().getName())
            .profileImageUrl(review.getUser().getProfileImageUrl())
            .build();
    }
}
