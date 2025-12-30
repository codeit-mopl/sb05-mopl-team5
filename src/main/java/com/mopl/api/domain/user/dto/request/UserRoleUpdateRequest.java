package com.mopl.api.domain.user.dto.request;

import com.mopl.api.domain.user.entity.UserRole;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record UserRoleUpdateRequest(
    @NotNull
    UserRole role
) {

}
