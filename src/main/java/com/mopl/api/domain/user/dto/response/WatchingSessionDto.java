package com.mopl.api.domain.user.dto.response;

import com.mopl.api.domain.content.dto.response.ContentDto;
import com.mopl.api.domain.user.entity.WatchingSession;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;

@Builder
public record WatchingSessionDto(
    UUID id,
    LocalDateTime createdAt,
    UserDto user,
    ContentDto content
) {
    public static WatchingSessionDto from(WatchingSession ws) {
        return WatchingSessionDto.builder()
                                 .id(ws.getId())
                                 .createdAt(ws.getCreatedAt())
                                 .user(UserDto.from(ws.getWatcher()))
                                 .content(ContentDto.from(ws.getContent()))
                                 .build();
    }
}
