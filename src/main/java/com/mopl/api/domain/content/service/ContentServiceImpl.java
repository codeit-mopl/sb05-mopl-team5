package com.mopl.api.domain.content.service;

import com.mopl.api.domain.content.dto.request.ContentCreateRequest;
import com.mopl.api.domain.content.dto.request.ContentSearchRequest;
import com.mopl.api.domain.content.dto.request.ContentUpdateRequest;
import com.mopl.api.domain.content.dto.response.ContentDto;
import com.mopl.api.domain.content.dto.response.CursorResponseContentDto;
import com.mopl.api.domain.content.repository.ContentRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ContentServiceImpl implements ContentService {

    private final ContentRepository contentRepository;

    @Override
    public ContentDto getContent(UUID id) {
        return null;
    }

    @Override
    public CursorResponseContentDto getContents(ContentSearchRequest request) {
        return null;
    }

    @Override
    public ContentDto addContent(ContentCreateRequest request, MultipartFile file) {
        return null;
    }

    @Override
    public ContentDto modifyContent(UUID id, ContentUpdateRequest request, MultipartFile file) {
        return null;
    }

    @Override
    public void removeContent(UUID id) {
    }
}
