package com.mopl.api.domain.user.dto.command;

import java.util.UUID;
import lombok.Builder;

@Builder
public record WatchingSessionCreateCommand(
    UUID userId,
    UUID contentId
) {

}