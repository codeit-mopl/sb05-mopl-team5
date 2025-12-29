package com.mopl.api.domain.content.dto.request;

import com.mopl.api.domain.content.entity.ContentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ContentCreateRequest(
    @NotNull ContentType type,
    @NotBlank String title,
    @NotBlank String description,
    @NotEmpty List<String> tags
) {

}
