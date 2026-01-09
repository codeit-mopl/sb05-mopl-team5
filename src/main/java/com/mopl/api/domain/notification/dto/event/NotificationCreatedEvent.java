package com.mopl.api.domain.notification.dto.event;

import com.mopl.api.domain.notification.dto.response.NotificationDto;
import lombok.Builder;

@Builder
public record NotificationCreatedEvent(
    NotificationDto notification
) {

}