package com.mopl.api.domain.notification.dto.event;

import java.util.UUID;
import lombok.Builder;

@Builder
public record SubscribingPlaylistContentAddedEvent(
    UUID playlistId,
    String playlistTitle,
    String playlistDescription,
    UUID contentId,
    String contentTitle
) {

}