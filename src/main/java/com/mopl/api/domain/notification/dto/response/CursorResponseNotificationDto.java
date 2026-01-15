package com.mopl.api.domain.notification.dto.response;

import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record CursorResponseNotificationDto(
    List<NotificationDto> data,
    String nextCursor,
    UUID nextIdAfter,
    boolean hasNext,
    int totalCount,
    String sortBy,
    String sortDirection
) {

}
