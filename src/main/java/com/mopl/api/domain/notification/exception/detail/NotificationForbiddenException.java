package com.mopl.api.domain.notification.exception.detail;

import com.mopl.api.domain.notification.exception.NotificationErrorCode;
import com.mopl.api.domain.notification.exception.NotificationException;
import java.util.UUID;

public class NotificationForbiddenException extends NotificationException {

    public NotificationForbiddenException() {
        super(NotificationErrorCode.NOTIFICATION_FORBIDDEN);
    }

    public static NotificationForbiddenException withNotificationIdAndReceiverId(UUID notificationId,
        UUID receiverId) {
        NotificationForbiddenException exception = new NotificationForbiddenException();
        exception.addDetail("notificationId", notificationId);
        exception.addDetail("receiverId", receiverId);
        return exception;
    }
}