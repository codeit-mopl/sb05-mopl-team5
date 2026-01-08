package com.mopl.api.domain.user.dto.response;

import com.mopl.api.domain.content.dto.response.ContentDto;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;

@Builder
public record WatchingSessionDto(
    UUID id,
    LocalDateTime createdAt,
    UserSummary watcher,
    ContentDto content
) {

}