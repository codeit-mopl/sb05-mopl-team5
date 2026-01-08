package com.mopl.api.domain.notification.service;

import com.mopl.api.domain.notification.dto.request.NotificationCursorPageRequest;
import com.mopl.api.domain.notification.dto.response.CursorResponseNotificationDto;
import com.mopl.api.domain.notification.dto.response.NotificationDto;
import com.mopl.api.domain.notification.entity.Notification;
import com.mopl.api.domain.notification.entity.NotificationLevel;
import com.mopl.api.domain.notification.exception.NotificationErrorCode;
import com.mopl.api.domain.notification.exception.NotificationException;
import com.mopl.api.domain.notification.exception.detail.NotificationNotFoundException;
import com.mopl.api.domain.notification.mapper.NotificationMapper;
import com.mopl.api.domain.notification.repository.NotificationRepository;
import com.mopl.api.domain.user.entity.User;
import com.mopl.api.domain.user.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final UserRepository userRepository;

    @Override
    public CursorResponseNotificationDto getNotifications(UUID receiverId, NotificationCursorPageRequest request) {
        Slice<Notification> notifications = notificationRepository.findAllByReceiverId(receiverId, request);
        int totalCount = Long.valueOf(notificationRepository.countByReceiverId(receiverId))
                             .intValue();

        return new CursorResponseNotificationDto(
            notifications.getContent()
                         .stream()
                         .map(notificationMapper::toDto)
                         .toList(),
            notifications.hasNext() && !notifications.getContent()
                                                     .isEmpty()
                ? notifications.getContent()
                               .get(notifications.getContent()
                                                 .size() - 1)
                               .getCreatedAt()
                               .toString()
                : null,
            notifications.hasNext() && !notifications.getContent()
                                                     .isEmpty()
                ? notifications.getContent()
                               .get(notifications.getContent()
                                                 .size() - 1)
                               .getId()
                : null,
            notifications.hasNext(),
            totalCount,
            request.sortBy()
                   .toString(),
            request.sortDirection()
                   .toString()
        );
    }

    @Transactional
    public void createNotification(
        UUID receiverId,
        String title,
        String content,
        NotificationLevel level
    ) {
        User receiver = userRepository.getReferenceById(receiverId);

        Notification notification = Notification.builder()
                                                .receiver(receiver)
                                                .title(title)
                                                .content(content)
                                                .level(level)
                                                .build();

        notification = notificationRepository.save(notification);

        // 이벤트 발행 (SSE 전송은 SseService에서 처리)
        NotificationDto dto = notificationMapper.toDto(notification);
//        eventPublisher.publishEvent(new NotificationCreatedEvent(dto));
    }

    @Override
    @Transactional
    public void removeNotification(UUID notificationId, UUID receiverId) {
        Notification notification = notificationRepository.findById(notificationId)
                                                          .orElseThrow(
                                                              () -> NotificationNotFoundException.withNotificationId(
                                                                  notificationId));

        if (!notification.getReceiver()
                         .getId()
                         .equals(receiverId)) {
            throw new NotificationException(NotificationErrorCode.NOTIFICATION_UNAUTHORIZED);
        }

        notificationRepository.delete(notification);
    }
}
