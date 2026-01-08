package com.mopl.api.batch.reader;

import com.mopl.api.global.client.tmdb.TmdbApiClient;
import com.mopl.api.global.client.tmdb.response.TvSeriesResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;

@Component
@StepScope
@RequiredArgsConstructor
public class TvSeriesReader implements ItemReader<TvSeriesResponse> {

    private final TmdbApiClient tmdbApiClient;

    private Long page = 1L;
    private final Long maxPage = 10L;
    private List<TvSeriesResponse> tvSeries = null;
    private int index = 0;

    @Override
    public TvSeriesResponse read() {
        if (tvSeries == null || index >= tvSeries.size()) {
            if (page > maxPage) {
                return null;
            }
            tvSeries = tmdbApiClient.getTvSeriesDetails(page);
            page++;
            index = 0;
        }
        if (index < tvSeries.size()) {
            return tvSeries.get(index++);
        }
        return null;
    }
}
