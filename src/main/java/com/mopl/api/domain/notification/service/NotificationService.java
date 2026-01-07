package com.mopl.api.domain.notification.service;

import com.mopl.api.domain.notification.dto.request.NotificationCursorPageRequest;
import com.mopl.api.domain.notification.dto.response.CursorResponseNotificationDto;
import com.mopl.api.domain.notification.entity.NotificationLevel;
import java.util.UUID;

public interface NotificationService {

    CursorResponseNotificationDto getNotifications(UUID receiverId, NotificationCursorPageRequest request);

    public void createNotification(UUID receiverId, String title, String content, NotificationLevel level);

    void removeNotification(UUID notificationId, UUID receiverId);
}
