package com.mopl.api.domain.notification.exception.detail;

import com.mopl.api.domain.notification.exception.NotificationErrorCode;
import com.mopl.api.domain.notification.exception.NotificationException;
import java.util.UUID;

public class NotificationForBiddenException extends NotificationException {

    public NotificationForBiddenException() {
        super(NotificationErrorCode.NOTIFICATION_FORBIDDEN);
    }

    public static NotificationForBiddenException withNotificationIdAndReceiverId(UUID notificationId,
        UUID receiverId) {
        NotificationForBiddenException exception = new NotificationForBiddenException();
        exception.addDetail("notificationId", notificationId);
        exception.addDetail("receiverId", receiverId);
        return exception;
    }
}