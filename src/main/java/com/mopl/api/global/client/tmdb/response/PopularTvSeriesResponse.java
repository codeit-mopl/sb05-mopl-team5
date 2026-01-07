package com.mopl.api.global.client.tmdb.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record PopularTvSeriesResponse(
    @JsonProperty("results")
    List<TvSeriesResponse> tvSeries
) {

}
