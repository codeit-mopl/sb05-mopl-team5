package com.mopl.api.domain.content.controller;

import com.mopl.api.domain.content.dto.request.ContentCreateRequest;
import com.mopl.api.domain.content.dto.request.ContentSearchRequest;
import com.mopl.api.domain.content.dto.request.ContentUpdateRequest;
import com.mopl.api.domain.content.dto.response.ContentDto;
import com.mopl.api.domain.content.dto.response.CursorResponseContentDto;
import com.mopl.api.domain.content.service.ContentService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/contents")
@RequiredArgsConstructor
public class ContentController {

    private final ContentService contentService;

    @GetMapping("/{contentId}")
    public ResponseEntity<ContentDto> contentList(@PathVariable UUID contentId) {
        ContentDto contentDto = contentService.getContent(contentId);
        return ResponseEntity.status(HttpStatus.OK)
                             .body(contentDto);
    }

    @GetMapping
    public ResponseEntity<CursorResponseContentDto> contentDetails(
        @Valid ContentSearchRequest request) {
        CursorResponseContentDto contentDtos = contentService.getContents(request);
        return ResponseEntity.status(HttpStatus.OK)
                             .body(contentDtos);
    }

    @PostMapping
    public ResponseEntity<ContentDto> contentAdd(@Valid @RequestBody ContentCreateRequest request,
        @RequestParam MultipartFile file) {
        ContentDto contentDto = contentService.addContent(request, file);
        return ResponseEntity.status(HttpStatus.CREATED)
                             .body(contentDto);
    }

    @PatchMapping("/{contentId}")
    public ResponseEntity<ContentDto> contentModify(@PathVariable UUID contentId,
        @Valid @RequestBody ContentUpdateRequest request, @RequestParam(required = false) MultipartFile file) {
        ContentDto contentDto = contentService.modifyContent(contentId, request, file);
        return ResponseEntity.status(HttpStatus.OK)
                             .body(contentDto);
    }

    @DeleteMapping("/{contentId}")
    public ResponseEntity<ContentDto> contentRemove(@PathVariable UUID contentId) {
        contentService.removeContent(contentId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                             .build();
    }
}
