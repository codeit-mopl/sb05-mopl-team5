package com.mopl.api.domain.playlist.dto.response;

import com.mopl.api.domain.content.dto.response.ContentDto;
import com.mopl.api.domain.user.dto.response.UserDto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record PlaylistDto(
    UUID id,
    UserDto owner,
    String title,
    String description,
    List<ContentDto> contents,
    int subscriberCount,
    boolean subscribedByMe,
    boolean isOwner,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {

}
