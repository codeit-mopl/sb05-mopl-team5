package com.mopl.api.domain.content.service;

import com.mopl.api.domain.content.dto.request.ContentCreateRequest;
import com.mopl.api.domain.content.dto.request.ContentSearchRequest;
import com.mopl.api.domain.content.dto.request.ContentUpdateRequest;
import com.mopl.api.domain.content.dto.response.ContentDto;
import com.mopl.api.domain.content.dto.response.CursorResponseContentDto;
import com.mopl.api.domain.content.entity.Content;
import com.mopl.api.domain.content.entity.ContentType;
import com.mopl.api.domain.content.mapper.ContentMapper;
import com.mopl.api.domain.content.repository.ContentRepository;
import com.mopl.api.global.config.image.LocalUploader;
import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ContentServiceImpl implements ContentService {

    private final ContentRepository contentRepository;
    private final ContentMapper contentMapper;
    private final LocalUploader localUploader;

    @Override
    @Transactional(readOnly = true)
    public ContentDto getContent(UUID id) {
        Content content = contentRepository.findByIdAndIsDeletedFalse(id)
                                           .orElseThrow(
                                               () -> new NoSuchElementException("Content not found with id: " + id));
        return contentMapper.toDto(content);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorResponseContentDto getContents(ContentSearchRequest request) {
        List<Content> contents = contentRepository.findContentsByCursor(request);
        Long totalCount = contentRepository.countContents(request);

        String nextCursor = null;
        UUID nextIdAfter = null;

        boolean hasNext = contents.size() > request.limit();
        if (hasNext) {
            contents = contents.subList(0, request.limit());

            Content last = contents.get(contents.size() - 1);

            nextCursor = switch (request.sortBy()) {
                case "createdAt" -> last.getCreatedAt()
                                        .toString();
                case "watcherCount" -> last.getWatcherCount() + "|" + last.getReviewCount();
                case "rate" -> last.getAverageRating()
                                   .toString();
                default -> throw new IllegalStateException("Unexpected value: " + request.sortBy());
            };
            nextIdAfter = last.getId();
        }

        return CursorResponseContentDto.builder()
                                       .data(contents.stream()
                                                     .map(contentMapper::toDto)
                                                     .toList())
                                       .nextCursor(nextCursor)
                                       .nextIdAfter(nextIdAfter)
                                       .hasNext(hasNext)
                                       .totalCount(totalCount)
                                       .sortDirection(request.sortDirection())
                                       .sortBy(request.sortBy())
                                       .build();
    }

    @Override
    @Transactional
    public ContentDto addContent(ContentCreateRequest request, MultipartFile file) {
        String tags = tagToString(request.tags());

        String thumbnail = localUploader.upload(file);

        Content content = new Content(ContentType.findByValue(request.type()), null, request.title(),
            request.description(),
            thumbnail, tags, BigDecimal.ZERO, 0L, 0L);

        return contentMapper.toDto(contentRepository.save(content));
    }

    @Override
    @Transactional
    public ContentDto modifyContent(UUID id, ContentUpdateRequest request, MultipartFile file) {
        Content content = contentRepository.findByIdAndIsDeletedFalse(id)
                                           .orElseThrow(
                                               () -> new NoSuchElementException("Content not found with id: " + id));

        String thumbnail = null;
        String tags = tagToString(request.tags());

        if (file != null && !file.isEmpty()) {
            thumbnail = localUploader.upload(file);
        }

        content.update(request.title(), request.description(), tags, thumbnail);

        return contentMapper.toDto(content);
    }

    @Override
    @Transactional
    public void removeContent(UUID id) {
        Content content = contentRepository.findByIdAndIsDeletedFalse(id)
                                           .orElseThrow(
                                               () -> new NoSuchElementException("Content not found with id: " + id));
        content.softDelete();
    }

    private String tagToString(List<String> tagList) {
        if (tagList == null || tagList.isEmpty()) {
            return null;
        }
        return String.join("|", tagList);
    }
}
