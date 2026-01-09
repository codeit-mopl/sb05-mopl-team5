package com.mopl.api.batch.writer;

import com.mopl.api.domain.content.entity.Content;
import com.mopl.api.domain.content.repository.ContentRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SportWriter implements ItemWriter<Content> {

    private final ContentRepository contentRepository;

    @Override
    public void write(Chunk<? extends Content> chunk) {
        List<Content> contents = chunk.getItems()
                                      .stream()
                                      .map(Content.class::cast)
                                      .filter(content -> !contentRepository.existsByApiIdAndType(content.getApiId(),
                                          content.getType()))
                                      .toList();
        contentRepository.saveAll(contents);
    }
}
