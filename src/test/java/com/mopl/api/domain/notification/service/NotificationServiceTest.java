package com.mopl.api.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.mopl.api.domain.notification.entity.Notification;
import com.mopl.api.domain.notification.entity.NotificationLevel;
import com.mopl.api.domain.notification.exception.detail.NotificationForBiddenException;
import com.mopl.api.domain.notification.exception.detail.NotificationNotFoundException;
import com.mopl.api.domain.notification.repository.NotificationRepository;
import com.mopl.api.domain.user.entity.User;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService 테스트")
class NotificationServiceTest {

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @Mock
    private NotificationRepository notificationRepository;

    @Test
    @DisplayName("자신의 알림이 아닌 경우 삭제 시 예외 발생")
    void removeNotification_Unauthorized() {
        // given
        UUID notificationId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();

        User owner = new User("test@test.com", "test123", "유저");
        ReflectionTestUtils.setField(owner, "id", ownerId);
        Notification notification = new Notification(owner, "알림", "내용", NotificationLevel.INFO);

        given(notificationRepository.findById(notificationId)).willReturn(Optional.of(notification));

        // when & then
        assertThatThrownBy(() -> notificationService.removeNotification(notificationId, requesterId))
            .isInstanceOf(NotificationForBiddenException.class);
    }

    @Test
    @DisplayName("존재하지 않는 알림 삭제 시 Not Found 예외 발생")
    void removeNotification_NotFound() {
        // given
        UUID id = UUID.randomUUID();
        given(notificationRepository.findById(id)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> notificationService.removeNotification(id, UUID.randomUUID()))
            .isInstanceOf(NotificationNotFoundException.class);
    }
}