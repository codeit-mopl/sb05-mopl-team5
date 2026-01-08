package com.mopl.api.global.client.tmdb.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record MovieResponse(
    @JsonProperty("id")
    Long apiId,

    @JsonProperty("original_title")
    String title,

    @JsonProperty("overview")
    String description,

    @JsonProperty("poster_path")
    String thumbnailUrl,

    @JsonProperty("genre_ids")
    List<Integer> tags
) {

    public MovieResponse {
        thumbnailUrl = thumbnailUrl == null ? null : "https://image.tmdb.org/t/p/w500" + thumbnailUrl;
    }
}
