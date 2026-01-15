package com.mopl.api.domain.content.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mopl.api.domain.content.dto.request.ContentCreateRequest;
import com.mopl.api.domain.content.dto.request.ContentSearchRequest;
import com.mopl.api.domain.content.dto.request.ContentUpdateRequest;
import com.mopl.api.domain.content.dto.response.ContentDto;
import com.mopl.api.domain.content.dto.response.CursorResponseContentDto;
import com.mopl.api.domain.content.entity.Content;
import com.mopl.api.domain.content.entity.ContentType;
import com.mopl.api.domain.content.exception.detail.ContentNotFoundException;
import com.mopl.api.domain.content.exception.detail.InvalidSortByException;
import com.mopl.api.domain.content.mapper.ContentMapper;
import com.mopl.api.domain.content.repository.ContentRepository;
import com.mopl.api.global.config.image.impl.LocalUploader;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

@DisplayName("ContentService 테스트")
@ExtendWith(MockitoExtension.class)
class ContentServiceImplTest {

    @Mock
    private ContentRepository contentRepository;

    @Mock
    private ContentMapper contentMapper;

    @Mock
    private LocalUploader localUploader;

    @InjectMocks
    private ContentServiceImpl contentService;

    private UUID id;

    @BeforeEach
    void setUp() {
        id = UUID.randomUUID();
    }

    private Content sampleContent() {
        return new Content(
            ContentType.MOVIE,
            null,
            "sample title",
            "desc",
            "uploaded.png",
            "tag1|tag2",
            BigDecimal.ZERO,
            0L,
            0L
        );
    }

    private ContentDto sampleDto() {
        return ContentDto.builder()
                         .id(id)
                         .type("MOVIE")
                         .title("sample title")
                         .description("desc")
                         .thumbnailUrl("uploaded.png")
                         .tags(List.of("tag1", "tag2"))
                         .averageRating(0.0)
                         .reviewCount(0L)
                         .watcherCount(0L)
                         .build();
    }

    // getContent() -------------------------------------------------------------------------
    @Test
    @DisplayName("콘텐츠 조회 성공")
    void getContent_success() {

        Content content = sampleContent();
        when(contentRepository.findByIdAndIsDeletedFalse(id)).thenReturn(Optional.of(content));
        when(contentMapper.toDto(content)).thenReturn(sampleDto());

        ContentDto dto = contentService.getContent(id);

        assertThat(dto.title()).isEqualTo("sample title");
        verify(contentRepository).findByIdAndIsDeletedFalse(id);
    }

    @Test
    @DisplayName("콘텐츠 조회 실패 - ID 없거나 삭제됨")
    void getContent_fail_notFound() {

        when(contentRepository.findByIdAndIsDeletedFalse(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> contentService.getContent(id))
            .isInstanceOf(ContentNotFoundException.class);
    }

    // getContents() -------------------------------------------------------------------------
    @Test
    @DisplayName("콘텐츠 조회 성공 - createdAt 정렬")
    void getContents_success_createdAtSort() {

        Content content = sampleContent();
        ContentDto dto = sampleDto();

        ContentSearchRequest req = ContentSearchRequest.builder()
                                                       .typeEqual("MOVIE")
                                                       .keywordLike("sample")
                                                       .sortBy("createdAt")
                                                       .sortDirection("ASC")
                                                       .limit(10)
                                                       .build();

        when(contentRepository.findContentsByCursor(req)).thenReturn(List.of(content));
        when(contentRepository.countContents(req)).thenReturn(1L);
        when(contentMapper.toDto(content)).thenReturn(dto);

        CursorResponseContentDto result = contentService.getContents(req);

        assertThat(result.data()).hasSize(1);
        assertThat(result.hasNext()).isFalse();
        assertThat(result.sortBy()).isEqualTo("createdAt");
        assertThat(result.sortDirection()).isEqualTo("ASC");
    }

    @Test
    @DisplayName("콘텐츠 조회 성공 - watcherCount 정렬")
    void getContents_success_watcherCountSort() {

        Content content = sampleContent();
        ContentDto dto = sampleDto();

        ContentSearchRequest req = ContentSearchRequest.builder()
                                                       .limit(10)
                                                       .sortBy("watcherCount")
                                                       .sortDirection("DESCENDING")
                                                       .build();

        when(contentRepository.findContentsByCursor(req)).thenReturn(List.of(content));
        when(contentRepository.countContents(req)).thenReturn(1L);
        when(contentMapper.toDto(content)).thenReturn(dto);

        CursorResponseContentDto result = contentService.getContents(req);

        assertThat(result.data()).hasSize(1);
    }

    @Test
    @DisplayName("콘텐츠 조회 성공 - rate 정렬")
    void getContents_success_rateSort() {

        Content content = sampleContent();
        ContentDto dto = sampleDto();

        ContentSearchRequest req = ContentSearchRequest.builder()
                                                       .limit(10)
                                                       .sortBy("rate")
                                                       .sortDirection("ASC")
                                                       .build();

        when(contentRepository.findContentsByCursor(req)).thenReturn(List.of(content));
        when(contentRepository.countContents(req)).thenReturn(1L);
        when(contentMapper.toDto(content)).thenReturn(dto);

        CursorResponseContentDto result = contentService.getContents(req);

        assertThat(result.data()).hasSize(1);
    }

    @Test
    @DisplayName("콘텐츠 조회 실패 - 잘못된 정렬 기준")
    void getContents_fail_invalidSortBy() {

        Content c1 = sampleContent();
        Content c2 = sampleContent();

        ContentSearchRequest req = ContentSearchRequest.builder()
                                                       .limit(1)
                                                       .sortBy("??wrong??")
                                                       .sortDirection("ASC")
                                                       .build();

        when(contentRepository.findContentsByCursor(req)).thenReturn(List.of(c1, c2));
        when(contentRepository.countContents(req)).thenReturn(2L);

        assertThatThrownBy(() -> contentService.getContents(req))
            .isInstanceOf(InvalidSortByException.class);
    }

    // addContent() -------------------------------------------------------------------------
    @Test
    @DisplayName("콘텐츠 생성 성공")
    void addContent_success() {

        MultipartFile file = mock(MultipartFile.class);

        ContentCreateRequest req = ContentCreateRequest.builder()
                                                       .type("MOVIE")
                                                       .title("sample title")
                                                       .description("desc")
                                                       .tags(List.of("tag1", "tag2"))
                                                       .build();

        when(localUploader.upload(file)).thenReturn("uploaded.png");

        Content saved = sampleContent();
        when(contentRepository.save(any(Content.class))).thenReturn(saved);
        when(contentMapper.toDto(saved)).thenReturn(sampleDto());

        ContentDto dto = contentService.addContent(req, file);

        assertThat(dto.title()).isEqualTo("sample title");
        assertThat(dto.description()).isEqualTo("desc");
        assertThat(dto.thumbnailUrl()).isEqualTo("uploaded.png");
        verify(localUploader).upload(file);
    }

    @Test
    @DisplayName("콘텐츠 생성 실패 - 이미지 업로드 실패")
    void addContent_fail_uploadError() {

        MultipartFile file = mock(MultipartFile.class);

        ContentCreateRequest req = ContentCreateRequest.builder()
                                                       .type("MOVIE")
                                                       .title("title")
                                                       .description("d")
                                                       .tags(List.of("t"))
                                                       .build();

        when(localUploader.upload(file))
            .thenThrow(new RuntimeException("upload fail"));

        assertThatThrownBy(() -> contentService.addContent(req, file))
            .isInstanceOf(RuntimeException.class);

        verify(contentRepository, never()).save(any());
    }

    @Test
    @DisplayName("콘텐츠 생성 성공 - tags null 허용")
    void addContent_tagsNull() {

        MultipartFile file = mock(MultipartFile.class);

        ContentCreateRequest req = ContentCreateRequest.builder()
                                                       .type("MOVIE")
                                                       .title("title")
                                                       .description("desc")
                                                       .tags(null)
                                                       .build();

        when(localUploader.upload(file)).thenReturn("img.png");

        Content saved = sampleContent();
        when(contentRepository.save(any(Content.class))).thenReturn(saved);
        when(contentMapper.toDto(saved)).thenReturn(sampleDto());

        ContentDto dto = contentService.addContent(req, file);

        assertThat(dto).isNotNull();
        verify(contentRepository).save(any());
    }

     // modifyContent -------------------------------------------------------------------------
    @Test
    @DisplayName("콘텐츠 수정 성공 - 사진 수정 포함")
    void modifyContent_withThumbnail() {

        Content content = spy(sampleContent());
        when(contentRepository.findByIdAndIsDeletedFalse(id))
            .thenReturn(Optional.of(content));

        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(localUploader.upload(file)).thenReturn("uploaded_new.png");

        ContentUpdateRequest req = ContentUpdateRequest.builder()
                                                       .title("updated")
                                                       .description("desc")
                                                       .tags(List.of("a", "b"))
                                                       .build();

        when(contentMapper.toDto(any())).thenReturn(sampleDto());

        contentService.modifyContent(id, req, file);

        verify(content).update("updated", "desc", "a|b", "uploaded_new.png");
    }

    @Test
    @DisplayName("콘텐츠 수정 성공 - 사진 수정 없음")
    void modifyContent_withoutThumbnail() {

        Content content = spy(sampleContent());
        when(contentRepository.findByIdAndIsDeletedFalse(id))
            .thenReturn(Optional.of(content));

        MultipartFile empty = mock(MultipartFile.class);
        when(empty.isEmpty()).thenReturn(true);

        ContentUpdateRequest req = ContentUpdateRequest.builder()
                                                       .title("updated")
                                                       .description("desc")
                                                       .tags(List.of("a"))
                                                       .build();

        when(contentMapper.toDto(any())).thenReturn(sampleDto());

        contentService.modifyContent(id, req, empty);

        verify(content).update("updated", "desc", "a", null);
    }

    @Test
    @DisplayName("콘텐츠 수정 실패 - 콘텐츠 없음")
    void modifyContent_fail_notFound() {

        when(contentRepository.findByIdAndIsDeletedFalse(id))
            .thenReturn(Optional.empty());

        ContentUpdateRequest req = ContentUpdateRequest.builder()
                                                       .title("t")
                                                       .build();

        assertThatThrownBy(() -> contentService.modifyContent(id, req, null))
            .isInstanceOf(ContentNotFoundException.class);
    }

    // removeContent() -------------------------------------------------------------------------
    @Test
    @DisplayName("콘텐츠 삭제 성공")
    void removeContent_success() {

        Content content = spy(sampleContent());
        when(contentRepository.findByIdAndIsDeletedFalse(id)).thenReturn(Optional.of(content));

        contentService.removeContent(id);

        verify(content).softDelete();
    }

    @Test
    @DisplayName("콘텐츠 삭제 실패 - 콘텐츠 없음")
    void removeContent_fail() {

        when(contentRepository.findByIdAndIsDeletedFalse(id))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> contentService.removeContent(id))
            .isInstanceOf(ContentNotFoundException.class);
    }
}
