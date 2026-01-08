package com.mopl.api.domain.notification.service;

import com.mopl.api.domain.notification.dto.request.NotificationCursorPageRequest;
import com.mopl.api.domain.notification.dto.response.CursorResponseNotificationDto;
import java.util.UUID;

public interface NotificationService {

    CursorResponseNotificationDto getNotifications(UUID receiverId, NotificationCursorPageRequest request);

    void createNotification(UUID receiverId, String title, String content);

    void removeNotification(UUID notificationId, UUID receiverId);
}
