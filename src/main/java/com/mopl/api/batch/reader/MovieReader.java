package com.mopl.api.batch.reader;

import com.mopl.api.global.client.tmdb.TmdbApiClient;
import com.mopl.api.global.client.tmdb.response.MovieResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;

@Component
@StepScope
@RequiredArgsConstructor
public class MovieReader implements ItemReader<MovieResponse> {

    private final TmdbApiClient tmdbApiClient;

    private Long page = 1L;
    private final Long maxPage = 10L;
    private List<MovieResponse> movies = null;
    private int index = 0;

    @Override
    public MovieResponse read() {
        if (movies == null || index >= movies.size()) {
            if (page > maxPage) {
                return null;
            }
            movies = tmdbApiClient.getMovieDetails(page);
            page++;
            index = 0;
        }
        if (index < movies.size()) {
            return movies.get(index++);
        }
        return null;
    }
}
