package com.mopl.api.global.client.tmdb;

import com.mopl.api.global.client.tmdb.response.MovieResponse;
import com.mopl.api.global.client.tmdb.response.PopularMovieResponse;
import com.mopl.api.global.client.tmdb.response.PopularTvSeriesResponse;
import com.mopl.api.global.client.tmdb.response.TvSeriesResponse;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class TmdbApiClient {

    @Value("${tmdb.api-key}")
    private String apiKey;

    private final RestClient restClient;

    public TmdbApiClient(RestClient.Builder builder) {
        this.restClient = builder
            .baseUrl("https://api.themoviedb.org/3")
            .build();
    }

    public List<MovieResponse> getMovieDetails(Long page) {
        try {
            PopularMovieResponse popularMovies = restClient.get()
                                                           .uri(uriBuilder ->
                                                               uriBuilder.path("/movie/popular")
                                                                         .queryParam("page", page)
                                                                         .queryParam("api_key", apiKey)
                                                                         .queryParam("language", "ko-KR")
                                                                         .build()
                                                           )
                                                           .retrieve()
                                                           .body(PopularMovieResponse.class);
            return popularMovies != null ? popularMovies.movies() : List.of();
        } catch (Exception e) {
            log.warn("Failed to fetch movie from TMDB for page=" + page, e);
            return List.of();
        }
    }

    public List<TvSeriesResponse> getTvSeriesDetails(Long page) {
        try {
            PopularTvSeriesResponse popularTvSeries = restClient.get()
                                                                .uri(uriBuilder ->
                                                                    uriBuilder.path("/tv/popular")
                                                                              .queryParam("page", page)
                                                                              .queryParam("api_key", apiKey)
                                                                              .queryParam("language", "ko-KR")
                                                                              .build()
                                                                )
                                                                .retrieve()
                                                                .body(PopularTvSeriesResponse.class);
            return popularTvSeries != null ? popularTvSeries.tvSeries() : List.of();
        } catch (Exception e) {
            log.warn("Failed to fetch tvSeries from TMDB for page=" + page, e);
            return List.of();
        }
    }
}
