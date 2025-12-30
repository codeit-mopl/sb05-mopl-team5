package com.mopl.api.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record ChangePasswordRequest(
    @NotBlank
    String password
) {

}
