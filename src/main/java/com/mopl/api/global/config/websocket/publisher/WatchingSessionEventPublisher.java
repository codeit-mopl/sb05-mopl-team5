package com.mopl.api.global.config.websocket.publisher;

import com.mopl.api.global.config.websocket.dto.WatchingSessionChange;
import java.util.UUID;

public interface WatchingSessionEventPublisher {
    void publish(UUID contentId, WatchingSessionChange change);
}
