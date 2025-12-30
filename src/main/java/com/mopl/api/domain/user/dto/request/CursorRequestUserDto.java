package com.mopl.api.domain.user.dto.request;

import com.mopl.api.domain.user.entity.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Builder;

@Builder
public record CursorRequestUserDto(
    String emailLike,

    UserRole roleEqual,

    Boolean isLocked,

    String cursor,

    UUID idAfter,

    @NotNull
    Integer limit,

    @NotBlank
    String sortDirection,

    @NotBlank
    String sortBy
) {

}
