package com.mopl.api.domain.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record UserCreateRequest(
    @NotBlank
    String name,

    @NotBlank
    @Email
    String email,

    @NotBlank
    String password
) {

}
