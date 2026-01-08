package com.mopl.api.global.client.tmdb.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PopularTvSeriesResponse(
    @JsonProperty("results")
    List<TvSeriesResponse> tvSeries
) {

}
