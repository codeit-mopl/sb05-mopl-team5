package com.mopl.api.batch.processor;

import com.mopl.api.domain.content.entity.Content;
import com.mopl.api.domain.content.entity.ContentType;
import com.mopl.api.global.client.sportdb.response.SportResponse;
import java.math.BigDecimal;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
public class SportProcessor implements ItemProcessor<SportResponse, Content> {

    @Override
    public Content process(SportResponse item) {
        String thumbnail = (item.thumbnailUrl() != null && !item.thumbnailUrl()
                                                                .isBlank())
            ? item.thumbnailUrl() : "/static/thumbnail.png";

        return new Content(ContentType.SPORT, item.apiId(), item.title(), item.description(), thumbnail,
            item.getTags(), BigDecimal.ZERO, 0L, 0L);
    }
}
