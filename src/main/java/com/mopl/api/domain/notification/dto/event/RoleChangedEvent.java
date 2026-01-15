package com.mopl.api.domain.notification.dto.event;

import java.util.UUID;
import lombok.Builder;

@Builder
public record RoleChangedEvent(
    UUID userId,
    String beforeRole,
    String currentRole
) {

}