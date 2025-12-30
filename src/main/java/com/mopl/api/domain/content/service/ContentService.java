package com.mopl.api.domain.content.service;

import com.mopl.api.domain.content.dto.request.ContentCreateRequest;
import com.mopl.api.domain.content.dto.request.ContentSearchRequest;
import com.mopl.api.domain.content.dto.request.ContentUpdateRequest;
import com.mopl.api.domain.content.dto.response.ContentDto;
import com.mopl.api.domain.content.dto.response.CursorResponseContentDto;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public interface ContentService {

    ContentDto getContent(UUID id);

    CursorResponseContentDto getContents(ContentSearchRequest request);

    ContentDto addContent(ContentCreateRequest request, MultipartFile file);

    ContentDto modifyContent(UUID id, ContentUpdateRequest request, MultipartFile file);

    void removeContent(UUID id);

}
