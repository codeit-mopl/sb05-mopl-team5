package com.mopl.api.domain.notification.event;

import com.mopl.api.domain.notification.dto.event.NotificationCreatedEvent;
import com.mopl.api.domain.sse.SseEmitterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationCreatedEventListener {

    private final SseEmitterRegistry sseEmitterRegistry;

    @EventListener
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleNotificationCreatedEvent(NotificationCreatedEvent event) {
        try {
            sseEmitterRegistry.send(
                event.notification()
                     .receiverId(),
                "notifications",
                event.notification()
                     .id()
                     .toString(),
                event.notification()
            );
            log.info("SSE를 통해 사용자에게 알림 전송 성공: {}", event.notification()
                                                        .receiverId());
        } catch (Exception e) {
            log.error("SSE를 통해 사용자에게 알림 전송 실패: {}",
                event.notification()
                     .receiverId(), e);
        }
    }
}
