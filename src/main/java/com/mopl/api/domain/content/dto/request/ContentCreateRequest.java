package com.mopl.api.domain.content.dto.request;

import com.mopl.api.domain.content.entity.ContentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ContentCreateRequest(
    @NotNull
    ContentType type,

    @NotBlank
    @Size(max = 255)
    String title,

    @NotBlank
    String description,

    @NotEmpty
    List<String> tags
) {

}
