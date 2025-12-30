package com.mopl.api.domain.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record ResetPasswordRequest(
    @NotBlank
    @Email(message = "올바른 이메일 형식이어야 합니다.")
    String email
) {

}
