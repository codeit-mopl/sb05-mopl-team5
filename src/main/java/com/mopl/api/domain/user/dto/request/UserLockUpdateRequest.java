package com.mopl.api.domain.user.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record UserLockUpdateRequest(
    @NotNull
    Boolean locked
) {

}
