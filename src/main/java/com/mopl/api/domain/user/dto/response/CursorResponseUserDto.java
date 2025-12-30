package com.mopl.api.domain.user.dto.response;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record CursorResponseUserDto<T>(
    @NotNull
    List<T> data,

    String nextCursor,

    UUID nextIdAfter,

    @NotNull
    Boolean hasNext,

    @NotNull
    Long totalCount,

    @NotBlank
    String sortBy,

    @NotBlank
    String sortDirection

) {

}
