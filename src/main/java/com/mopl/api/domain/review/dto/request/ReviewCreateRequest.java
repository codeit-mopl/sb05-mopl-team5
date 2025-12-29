package com.mopl.api.domain.review.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record ReviewCreateRequest(
    @NotNull(message = "Content ID is required")
    UUID contentId,

    @NotBlank(message = "Review text is required")
    @Size(max = 2000, message = "Review text must not exceed 2000 characters")
    String text,

    @NotNull(message = "Rating is required")
    @DecimalMin(value = "0.0", message = "Rating must be at least 0.0")
    @DecimalMax(value = "5.0", message = "Rating must not exceed 5.0")
    @Digits(integer = 1, fraction = 1, message = "Rating must have 1 decimal place")
    Double rating
) {
}
