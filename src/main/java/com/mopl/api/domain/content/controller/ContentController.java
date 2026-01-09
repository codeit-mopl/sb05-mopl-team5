package com.mopl.api.domain.content.controller;

import com.mopl.api.domain.content.dto.request.ContentCreateRequest;
import com.mopl.api.domain.content.dto.request.ContentSearchRequest;
import com.mopl.api.domain.content.dto.request.ContentUpdateRequest;
import com.mopl.api.domain.content.dto.response.ContentDto;
import com.mopl.api.domain.content.dto.response.CursorResponseContentDto;
import com.mopl.api.domain.content.mapper.ContentMapper;
import com.mopl.api.domain.content.repository.ContentRepository;
import com.mopl.api.domain.content.service.ContentService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/contents")
@RequiredArgsConstructor
public class ContentController {

    private final ContentService contentService;
    private final ContentRepository contentRepository;  // 목록 조회 테스트를 위해 임시 사용
    private final ContentMapper contentMapper;


    @GetMapping("/{contentId}")
    public ResponseEntity<ContentDto> contentList(@PathVariable UUID contentId) {
        ContentDto contentDto = contentService.getContent(contentId);
        return ResponseEntity.status(HttpStatus.OK)
                             .body(contentDto);
    }

    @GetMapping
    public ResponseEntity<CursorResponseContentDto> contentDetails(
        @Valid ContentSearchRequest request) {
//        CursorResponseContentDto contentDtos = contentService.getContents(request);
//        return ResponseEntity.status(HttpStatus.OK)
//                             .body(contentDtos);

        List<ContentDto> contents = contentRepository.findAll()
                                                     .stream()
                                                     .map(contentMapper::toDto)
                                                     .toList();
        return ResponseEntity.status(HttpStatus.OK).body(CursorResponseContentDto.builder()
                                .data(contents)
                                 .sortBy("createdAt")
                                 .sortDirection("DESCENDING")
                                .build());

    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ContentDto> contentAdd(@Valid @RequestPart ContentCreateRequest request,
        @RequestPart MultipartFile thumbnail) {
        ContentDto contentDto = contentService.addContent(request, thumbnail);
        return ResponseEntity.status(HttpStatus.CREATED)
                             .body(contentDto);
    }

    @PatchMapping("/{contentId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ContentDto> contentModify(@PathVariable UUID contentId,
        @Valid @RequestPart ContentUpdateRequest request, @RequestPart(required = false) MultipartFile file) {
        ContentDto contentDto = contentService.modifyContent(contentId, request, file);
        return ResponseEntity.status(HttpStatus.OK)
                             .body(contentDto);
    }

    @DeleteMapping("/{contentId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ContentDto> contentRemove(@PathVariable UUID contentId) {
        contentService.removeContent(contentId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                             .build();
    }
}
