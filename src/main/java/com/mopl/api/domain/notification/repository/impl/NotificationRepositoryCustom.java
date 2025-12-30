package com.mopl.api.domain.notification.repository.impl;

import com.mopl.api.domain.notification.entity.Notification;
import java.util.List;

public interface NotificationRepositoryCustom {

    List<Notification> tempNotificationPage(String var);
}
