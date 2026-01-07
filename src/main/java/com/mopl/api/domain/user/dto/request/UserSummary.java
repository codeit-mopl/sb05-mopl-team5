package com.mopl.api.domain.user.dto.request;

import java.util.UUID;
import lombok.Builder;

@Builder
public record UserSummary(
    UUID userId,
    String name,
    String profileImageUrl) {

}