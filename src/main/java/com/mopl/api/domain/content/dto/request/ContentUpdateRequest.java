package com.mopl.api.domain.content.dto.request;

import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Builder;

@Builder
public record ContentUpdateRequest(
    @Size(max = 255)
    String title,
    String description,
    List<String> tags
) {

}
