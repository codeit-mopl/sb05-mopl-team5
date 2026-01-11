package com.mopl.api.domain.review.mapper;

import com.mopl.api.domain.review.dto.response.AuthorDto;
import com.mopl.api.domain.review.dto.response.ReviewDto;
import com.mopl.api.domain.review.entity.Review;
import com.mopl.api.domain.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReviewMapper {

    @Mapping(target = "id", source = "review.id")
    @Mapping(target = "contentId", source = "review.content.id")
    @Mapping(target = "author", source = "review.user")
    @Mapping(target = "text", source = "review.text")
    @Mapping(target = "rating", source = "review.rating")
    @Mapping(target = "isAuthor", source = "isAuthor")
    ReviewDto toDto(Review review, boolean isAuthor);

    default AuthorDto map(User user) {
        if (user == null) {
            return null;
        }
        return AuthorDto.builder()
            .userId(user.getId())
            .name(user.getName())
            .profileImageUrl(user.getProfileImageUrl())
            .build();
    }
}
