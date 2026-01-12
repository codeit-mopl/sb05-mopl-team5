package com.mopl.api.global.client.tmdb.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TvSeriesResponse(
    @JsonProperty("id")
    Long apiId,

    @JsonProperty("name")
    String title,

    @JsonProperty("overview")
    String description,

    @JsonProperty("poster_path")
    String thumbnailUrl,

    @JsonProperty("genre_ids")
    List<Integer> tags
) {

    public TvSeriesResponse {
        thumbnailUrl = thumbnailUrl == null ? null : "https://image.tmdb.org/t/p/w500" + thumbnailUrl;
    }
}
