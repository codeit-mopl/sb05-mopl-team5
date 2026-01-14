package com.mopl.api.batch.processor;

import com.mopl.api.domain.content.entity.Content;
import com.mopl.api.domain.content.entity.ContentType;
import com.mopl.api.global.client.tmdb.TmdbGenre;
import com.mopl.api.global.client.tmdb.response.MovieResponse;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MovieProcessor implements ItemProcessor<MovieResponse, Content> {

    @Override
    public Content process(MovieResponse item) {
        List<String> genres = new ArrayList<>();

        for (int id : item.tags()) {
            genres.add(TmdbGenre.getNameById(id));
        }

        String thumbnail = (item.thumbnailUrl() != null && !item.thumbnailUrl()
                                                                .isBlank())
            ? item.thumbnailUrl() : "/static/thumbnail.png";

        return new Content(ContentType.MOVIE, item.apiId(), item.title(), item.description(), thumbnail,
            String.join("|", genres),
            BigDecimal.ZERO, 0L, 0L);
    }
}
