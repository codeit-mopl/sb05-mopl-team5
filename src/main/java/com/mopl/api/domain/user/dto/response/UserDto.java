package com.mopl.api.domain.user.dto.response;

import com.mopl.api.domain.user.entity.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;

@Builder
public record UserDto(
    @NotNull
    UUID id,

    @NotNull
    LocalDateTime createdAt,

    @NotBlank
    String email,

    @NotBlank
    String name,

    String profileImageUrl,

    @NotNull
    UserRole role,

    Boolean locked

) {

}
