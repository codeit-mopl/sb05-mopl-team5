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

        return new Content(ContentType.SPORT, item.apiId(), item.title(), item.description(), item.thumbnailUrl(),
            item.getTags(), BigDecimal.ZERO, 0L,0L);
    }
}
