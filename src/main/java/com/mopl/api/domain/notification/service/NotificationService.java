package com.mopl.api.domain.notification.service;

import com.mopl.api.domain.notification.dto.request.NotificationCreateRequest;
import com.mopl.api.domain.notification.dto.request.NotificationCursorPageRequest;
import com.mopl.api.domain.notification.dto.response.CursorResponseNotificationDto;
import com.mopl.api.domain.notification.dto.response.NotificationDto;
import java.util.UUID;

public interface NotificationService {

    CursorResponseNotificationDto getNotifications(UUID receiverId, NotificationCursorPageRequest request);

    NotificationDto addNotification(NotificationCreateRequest request);

    void removeNotification(UUID notificationId, UUID receiverId);
}
