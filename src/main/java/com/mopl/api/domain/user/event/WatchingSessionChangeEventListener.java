package com.mopl.api.domain.user.event;

import com.mopl.api.domain.user.dto.event.WatchingSessionChangeEvent;
import com.mopl.api.domain.user.repository.WatchingSessionRedisRepository;
import com.mopl.api.global.config.websocket.dto.WatchingSessionChange;
import com.mopl.api.global.config.websocket.dto.WatchingSessionChange.ChangeType;
import com.mopl.api.global.config.websocket.publisher.WatchingSessionEventPublisher;
import java.util.UUID;
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
    private final WatchingSessionRedisRepository watchingSessionRedisRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(WatchingSessionChangeEvent event) {
        log.debug(
            "AFTER_COMMIT event. contentId={}, type={}",
            event.contentId(),
            event.change()
                 .type()
        );

        UUID contentId = event.contentId();
        UUID watcherId = event.change()
                              .watchingSession()
                              .watcher()
                              .userId();

        if (event.change()
                 .type()
                 .equals(ChangeType.JOIN)) {

            watchingSessionRedisRepository.addWatcher(contentId, watcherId);
        } else if (event.change()
                        .type()
                        .equals(ChangeType.LEAVE)) {
            watchingSessionRedisRepository.removeWatcher(contentId, watcherId);
        }

        publisher.publish(event.contentId(), WatchingSessionChange.builder()
                                                                  .type(event.change()
                                                                             .type())
                                                                  .watchingSession(event.change()
                                                                                        .watchingSession())
                                                                  .watcherCount(
                                                                      watchingSessionRedisRepository.countWatchers(
                                                                          contentId))
                                                                  .build());
    }
}