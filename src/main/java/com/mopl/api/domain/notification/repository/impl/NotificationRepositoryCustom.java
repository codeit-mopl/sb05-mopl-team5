package com.mopl.api.domain.notification.repository.impl;

import com.mopl.api.domain.notification.dto.request.NotificationCursorPageRequest;
import com.mopl.api.domain.notification.entity.Notification;
import java.util.UUID;
import org.springframework.data.domain.Slice;

public interface NotificationRepositoryCustom {

    Slice<Notification> findAllByReceiverId(UUID receiverId, NotificationCursorPageRequest request);

    long countByReceiverId(UUID receiverId);
}
