package com.mopl.api.domain.notification.service;

import com.mopl.api.domain.notification.dto.event.NotificationCreatedEvent;
import com.mopl.api.domain.notification.dto.request.NotificationCreateRequest;
import com.mopl.api.domain.notification.dto.request.NotificationCursorPageRequest;
import com.mopl.api.domain.notification.dto.response.CursorResponseNotificationDto;
import com.mopl.api.domain.notification.dto.response.NotificationDto;
import com.mopl.api.domain.notification.entity.Notification;
import com.mopl.api.domain.notification.entity.NotificationLevel;
import com.mopl.api.domain.notification.exception.detail.NotificationForbiddenException;
import com.mopl.api.domain.notification.exception.detail.NotificationNotFoundException;
import com.mopl.api.domain.notification.mapper.NotificationMapper;
import com.mopl.api.domain.notification.repository.NotificationRepository;
import com.mopl.api.domain.user.entity.User;
import com.mopl.api.domain.user.repository.UserRepository;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

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
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public NotificationDto addNotification(NotificationCreateRequest request) {
        User receiver = userRepository.getReferenceById(request.receiverId());

        Notification notification = new Notification(
            receiver,
            request.title(),
            Objects.requireNonNullElse(request.content(), ""),
            NotificationLevel.INFO);

        Notification saved = notificationRepository.save(notification);
        NotificationDto dto = notificationMapper.toDto(saved);
        eventPublisher.publishEvent(NotificationCreatedEvent.builder()
                                                            .notification(dto)
                                                            .build());

        log.info("알림 생성 notificationId={}", notification.getId());

        return dto;
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
            throw NotificationForbiddenException.withNotificationIdAndReceiverId(notificationId, receiverId);
        }

        notificationRepository.delete(notification);

        log.info("알림 삭제: {}", notificationId);
    }
}
