package com.mopl.api.domain.content.dto.request;

import com.mopl.api.domain.content.entity.ContentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ContentSearchRequest(
    ContentType type,
    String keywordLike,
    String cursor,
    UUID idAfter,
    @NotNull Long limit,
    @NotBlank String sortDirection,
    @NotBlank String sortBy
) {

}
