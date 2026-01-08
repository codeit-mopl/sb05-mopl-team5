package com.mopl.api.global.client.tmdb.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PopularMovieResponse(
    @JsonProperty("results")
    List<MovieResponse> movies
) {

}
