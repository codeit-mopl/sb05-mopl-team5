package com.mopl.api.domain.notification.dto.event;

import java.util.UUID;
import lombok.Builder;

@Builder
public record FolloweeWatchingStartedEvent(
    UUID watchingSessionId,
    UUID watcherId,
    UUID contentId,
    String contentTitle
) {

}