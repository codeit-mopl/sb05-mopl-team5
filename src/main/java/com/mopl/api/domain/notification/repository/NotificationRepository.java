package com.mopl.api.domain.notification.repository;

import com.mopl.api.domain.notification.entity.Notification;
import com.mopl.api.domain.notification.repository.impl.NotificationRepositoryCustom;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, UUID>, NotificationRepositoryCustom {

}
