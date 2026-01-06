package com.mopl.api.global.client.tmdb.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.stream.Collectors;

public record MovieResponse(
    @JsonProperty("id")
    Long apiId,

    @JsonProperty("original_name")
    String title,

    @JsonProperty("overview")
    String description,

    @JsonProperty("poster_path")
    String thumbnailUrl,

    @JsonProperty("genres")
    List<TagDto> tags
) {

    public MovieResponse {
        thumbnailUrl = thumbnailUrl == null ? null : "https://image.tmdb.org/t/p/w500" + thumbnailUrl;
    }

    public String getTag() {
        return tags == null ? null : tags.stream()
                                         .map(TagDto::name)
                                         .collect(Collectors.joining("|"));
    }
}
