package com.mopl.api.domain.notification.service;

import com.mopl.api.domain.notification.dto.event.NotificationCreatedEvent;
import com.mopl.api.domain.notification.dto.request.NotificationCursorPageRequest;
import com.mopl.api.domain.notification.dto.response.CursorResponseNotificationDto;
import com.mopl.api.domain.notification.dto.response.NotificationDto;
import com.mopl.api.domain.notification.entity.Notification;
import com.mopl.api.domain.notification.entity.NotificationLevel;
import com.mopl.api.domain.notification.exception.detail.NotificationNotFoundException;
import com.mopl.api.domain.notification.exception.detail.NotificationUnauthorizedException;
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
@Transactional(readOnly = true)
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final UserRepository userRepository;

    /**
     * 알림 목록 조회 (커서 페이지네이션)
     */
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
            String.valueOf(request.sortBy()),
            String.valueOf(request.sortDirection())
        );
    }

    /**
     * 알림 생성
     */
    @Override
    @Transactional
    public void createNotification(
        UUID receiverId,
        String title,
        String content
//        NotificationLevel level
    ) {
        User receiver = userRepository.getReferenceById(receiverId);
        Notification notification = new Notification(receiver, title, content, NotificationLevel.INFO);

        notification = notificationRepository.save(notification);

        // 이벤트 발행 (NotificationEventListener에서 처리)
        NotificationDto dto = notificationMapper.toDto(notification);
        eventPublisher.publishEvent(new NotificationCreatedEvent(dto));

        log.info("알림 생성: {}", notification.getId());
    }

    /**
     * 알림 삭제
     */
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
            throw NotificationUnauthorizedException.withNotificationIdAndReceiverId(notificationId, receiverId);
        }

        notificationRepository.delete(notification);

        log.info("알림 삭제: {}", notificationId);
    }
}
