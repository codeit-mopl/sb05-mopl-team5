package com.mopl.api.domain.notification.dto.event;

import java.util.UUID;
import lombok.Builder;

@Builder
public record PlaylistSubscribedEvent(
    UUID playlistId,
    String playlistTitle,
    String playlistDescription,
    UUID ownerId,
    UUID subscriberId,
    String subscriberName
) {

}