package com.mopl.api.domain.content.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Builder;

@Builder
public record ContentCreateRequest(
    @NotNull
    String type,

    @NotBlank
    @Size(max = 255)
    String title,

    @NotBlank
    String description,

    @NotEmpty
    List<String> tags
) {

}
