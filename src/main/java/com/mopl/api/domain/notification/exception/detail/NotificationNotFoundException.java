package com.mopl.api.domain.notification.exception.detail;

import com.mopl.api.domain.notification.exception.NotificationErrorCode;
import com.mopl.api.domain.notification.exception.NotificationException;
import java.util.UUID;

public class NotificationNotFoundException extends NotificationException {

    public NotificationNotFoundException() {
        super(NotificationErrorCode.NOTIFICATION_NOT_FOUND);
    }

    public static NotificationNotFoundException withNotificationId(UUID notificationId) {
        NotificationNotFoundException exception = new NotificationNotFoundException();
        exception.addDetail("notificationId", notificationId);
        return exception;
    }
}