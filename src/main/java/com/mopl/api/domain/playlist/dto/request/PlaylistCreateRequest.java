package com.mopl.api.domain.playlist.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PlaylistCreateRequest(
    @NotBlank(message = "Playlist title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    String title,

    @NotBlank(message = "Playlist description is required")
    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    String description
) {

}
