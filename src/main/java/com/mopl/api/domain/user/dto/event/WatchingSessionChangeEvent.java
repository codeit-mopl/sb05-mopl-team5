package com.mopl.api.domain.user.dto.event;

import com.mopl.api.global.config.websocket.dto.WatchingSessionChange;
import java.util.UUID;
import lombok.Builder;

@Builder
public record WatchingSessionChangeEvent(
    UUID contentId,
    WatchingSessionChange change
) {

}
