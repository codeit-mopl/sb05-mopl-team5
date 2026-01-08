package com.mopl.api.domain.notification.exception.detail;

import com.mopl.api.domain.notification.exception.NotificationErrorCode;
import com.mopl.api.domain.notification.exception.NotificationException;
import java.util.UUID;

public class NotificationUnauthorizedException extends NotificationException {

    public NotificationUnauthorizedException() {
        super(NotificationErrorCode.NOTIFICATION_UNAUTHORIZED);
    }

    public static NotificationUnauthorizedException withNotificationIdAndReceiverId(UUID notificationId,
        UUID receiverId) {
        NotificationUnauthorizedException exception = new NotificationUnauthorizedException();
        exception.addDetail("notificationId", notificationId);
        exception.addDetail("receiverId", receiverId);
        return exception;
    }
}