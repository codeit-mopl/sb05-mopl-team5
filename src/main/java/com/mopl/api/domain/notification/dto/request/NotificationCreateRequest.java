package com.mopl.api.domain.notification.dto.request;

import com.mopl.api.domain.notification.entity.NotificationLevel;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;

@Builder
public record NotificationCreateRequest(
    UUID id,
    LocalDateTime createdAt,
    UUID receiverId,
    String title,
    String content,
    NotificationLevel level
) {

}
