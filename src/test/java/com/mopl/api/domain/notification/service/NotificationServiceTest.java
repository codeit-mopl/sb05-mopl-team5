package com.mopl.api.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.mopl.api.domain.notification.dto.event.NotificationCreatedEvent;
import com.mopl.api.domain.notification.dto.request.NotificationCreateRequest;
import com.mopl.api.domain.notification.dto.response.NotificationDto;
import com.mopl.api.domain.notification.entity.Notification;
import com.mopl.api.domain.notification.entity.NotificationLevel;
import com.mopl.api.domain.notification.exception.detail.NotificationForbiddenException;
import com.mopl.api.domain.notification.mapper.NotificationMapper;
import com.mopl.api.domain.notification.repository.NotificationRepository;
import com.mopl.api.domain.user.entity.User;
import com.mopl.api.domain.user.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationMapper notificationMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @Test
    @DisplayName("알림 생성 및 이벤트 발행 성공")
    void addNotification_Success() {
        // given
        UUID receiverId = UUID.randomUUID();
        NotificationCreateRequest request = NotificationCreateRequest.builder()
                                                                     .receiverId(receiverId)
                                                                     .title("Test Title")
                                                                     .content("Test Content")
                                                                     .level(NotificationLevel.INFO)
                                                                     .build();

        User mockUser = mock(User.class);
        given(userRepository.getReferenceById(receiverId)).willReturn(mockUser);

        Notification savedNotification = mock(Notification.class);
        given(notificationRepository.save(any(Notification.class))).willReturn(savedNotification);

        NotificationDto mockDto = NotificationDto.builder()
                                                 .id(UUID.randomUUID())
                                                 .build();
        given(notificationMapper.toDto(savedNotification)).willReturn(mockDto);

        // when
        notificationService.addNotification(request);

        // then
        verify(notificationRepository).save(any(Notification.class));
        verify(notificationMapper).toDto(savedNotification);
        verify(eventPublisher).publishEvent(any(NotificationCreatedEvent.class));
    }

    @Test
    @DisplayName("알림 삭제 - 본인 알림일 경우 성공")
    void removeNotification_Success() {
        // given
        UUID notificationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        User mockOwner = mock(User.class);
        given(mockOwner.getId()).willReturn(userId);

        Notification mockNotification = mock(Notification.class);
        given(mockNotification.getReceiver()).willReturn(mockOwner);

        given(notificationRepository.findById(notificationId)).willReturn(Optional.of(mockNotification));

        // when
        notificationService.removeNotification(notificationId, userId);

        // then
        verify(notificationRepository).delete(mockNotification);
    }

    @Test
    @DisplayName("알림 삭제 - 타인의 알림일 경우 예외 발생")
    void removeNotification_Forbidden() {
        // given
        UUID notificationId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();

        User mockOwner = mock(User.class);
        given(mockOwner.getId()).willReturn(ownerId);

        Notification mockNotification = mock(Notification.class);
        given(mockNotification.getReceiver()).willReturn(mockOwner);

        given(notificationRepository.findById(notificationId)).willReturn(Optional.of(mockNotification));

        // when & then
        assertThatThrownBy(() -> notificationService.removeNotification(notificationId, otherUserId))
            .isInstanceOf(NotificationForbiddenException.class);
    }
}