package com.mopl.api.domain.user.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;

@Builder
public record WatchingSessionDto(UUID id, LocalDateTime createdAt
// TODO 그 외... UserDto user, ContentDto content
) {

}
