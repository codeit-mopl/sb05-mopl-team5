package com.mopl.api.domain.content.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Builder;

@Builder
public record ContentSearchRequest(
    String typeEqual,
    String keywordLike,
    String cursor,
    UUID idAfter,
    @NotNull int limit,
    @NotBlank String sortDirection,
    @NotBlank String sortBy
) {

}
