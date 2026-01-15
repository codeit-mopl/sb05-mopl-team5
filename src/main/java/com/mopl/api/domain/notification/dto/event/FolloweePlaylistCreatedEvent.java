package com.mopl.api.domain.notification.dto.event;

import java.util.UUID;
import lombok.Builder;

@Builder
public record FolloweePlaylistCreatedEvent(
    UUID playlistId,
    String playlistTitle,
    String playlistDescription,
    UUID ownerId,
    String ownerName
) {

}