package com.mopl.api.domain.review.dto.response;

//import com.mopl.api.domain.user.dto.response.UserDto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ReviewDto(
    UUID id,
    UUID contentId,
//    UserDto author,
    String text,
    double rating,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    boolean isAuthor
) {

}
