package com.mopl.api.domain.user.event;

import com.mopl.api.domain.user.dto.event.WatchingSessionChangeEvent;
import com.mopl.api.global.config.websocket.publisher.WatchingSessionEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class WatchingSessionChangeEventListener {

    private final WatchingSessionEventPublisher publisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(WatchingSessionChangeEvent event) {
        log.debug(
            "AFTER_COMMIT event. contentId={}, type={}",
            event.contentId(),
            event.change()
                 .type()
        );

        publisher.publish(event.contentId(), event.change());
    }
}