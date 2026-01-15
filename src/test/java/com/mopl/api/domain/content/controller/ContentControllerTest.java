package com.mopl.api.domain.content.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mopl.api.config.TestSecurityConfig;
import com.mopl.api.domain.content.dto.response.ContentDto;
import com.mopl.api.domain.content.entity.Content;
import com.mopl.api.domain.content.entity.ContentType;
import com.mopl.api.domain.content.service.ContentService;
import com.mopl.api.global.config.security.jwt.JwtRegistry;
import com.mopl.api.global.config.security.jwt.JwtTokenProvider;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@Import(TestSecurityConfig.class)
@WebMvcTest(controllers = ContentController.class)
@DisplayName("ContentController 테스트")
public class ContentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ContentService contentService;

    @MockitoBean
    private JwtRegistry jwtRegistry;

    @MockitoBean
    private JwtTokenProvider jwtProvider;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private UUID contentId;

    @BeforeEach
    void setUp() {
        contentId = UUID.randomUUID();
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
                         .id(contentId)
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

    private ContentDto updateDto() {
        return ContentDto.builder()
                         .id(contentId)
                         .type("MOVIE")
                         .title("updated title")
                         .description("updated desc")
                         .thumbnailUrl("uploaded.png")
                         .tags(List.of("tag1", "tag2"))
                         .averageRating(0.0)
                         .reviewCount(0L)
                         .watcherCount(0L)
                         .build();
    }


    // ---------------- 조회 ----------------
    @Test
    @DisplayName("콘텐츠 조회 성공")
    void getContent_success() throws Exception {
        when(contentService.getContent(contentId)).thenReturn(sampleDto());
        mockMvc.perform(get("/api/contents/{id}", contentId))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.title").value("sample title"));
    }

    @Test
    @DisplayName("콘텐츠 조회 실패 - 존재하지 않는 ID")
    void getContent_fail_notFound() throws Exception {
        when(contentService.getContent(contentId)).thenThrow(new RuntimeException("not found"));
        mockMvc.perform(get("/api/contents/{id}", contentId))
               .andExpect(status().isBadRequest());
    }

    // ---------------- 생성 ----------------
    @Test
    @WithMockUser(roles = {"ADMIN"})
    @DisplayName("콘텐츠 생성 성공 - ADMIN 접근")
    void addContent_success() throws
        Exception {
        when(contentService.addContent(Mockito.any(), Mockito.any())).thenReturn(sampleDto());
        MockMultipartFile json = new MockMultipartFile("request", "request.json", "application/json", """
            { "type":"MOVIE", "title":"sample title", "description":"desc", "tags":["tag1","tag2"] }""".getBytes());
        MockMultipartFile file = new MockMultipartFile("thumbnail", "thumb.png", "image/png", "fakeimage".getBytes());
        mockMvc.perform(multipart("/api/contents").file(json)
                                                  .file(file))
               .andExpect(status().isCreated())
               .andExpect(jsonPath("$.title").value("sample title"));
    }

    @Test
    @WithMockUser(roles = {"USER"})
    @DisplayName("콘텐츠 생성 실패 - USER 접근 시 Forbidden")
    void addContent_fail_forbidden() throws Exception {
        MockMultipartFile json = new MockMultipartFile("request", "request.json", "application/json", """ 
            { "type":"MOVIE", "title":"sample title", "description":"desc", "tags":["tag1","tag2"] }""".getBytes());
        MockMultipartFile file = new MockMultipartFile("thumbnail", "thumb.png", "image/png",
            "fakeimage".getBytes());
        mockMvc.perform(multipart("/api/contents").file(json)
                                                  .file(file))
               .andExpect(status().isForbidden());
    }

    // ---------------- 수정 ----------------
    @Test
    @WithMockUser(roles = {"ADMIN"})
    @DisplayName("콘텐츠 수정 성공")
    void modifyContent_success() throws Exception {
        when(contentService.modifyContent(Mockito.eq(contentId), Mockito.any(), Mockito.any()))
            .thenReturn(updateDto());

        MockMultipartFile json = new MockMultipartFile("request", "request.json", "application/json", """ 
            { "title":"updated title", "description":"updated desc", "tags":["a","b"] }""".getBytes());
        mockMvc.perform(multipart("/api/contents/{id}", contentId).file(json)
                                                                  .with(req -> {
                                                                      req.setMethod("PATCH");
                                                                      return req;
                                                                  }))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.title").value("updated title"));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    @DisplayName("콘텐츠 수정 실패 - 존재하지 않는 ID")
    void modifyContent_fail_notFound() throws Exception {
        when(contentService.modifyContent(Mockito.eq(contentId), Mockito.any(), Mockito.any())).thenThrow(
            new RuntimeException("not found"));
        MockMultipartFile json = new MockMultipartFile("request", "request.json", "application/json", """ 
            { "title":"updated title", "description":"updated desc", "tags":["a","b"] }""".getBytes());
        mockMvc.perform(multipart("/api/contents/{id}", contentId).file(json)
                                                                  .with(req -> {
                                                                      req.setMethod("PATCH");
                                                                      return req;
                                                                  }))
               .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = {"USER"})
    @DisplayName("콘텐츠 수정 실패 - USER 접근 시 Forbidden")
    void modifyContent_fail_forbidden() throws Exception {
        when(contentService.modifyContent(Mockito.eq(contentId), Mockito.any(), Mockito.any())).thenReturn(sampleDto());
        MockMultipartFile json = new MockMultipartFile("request", "request.json", "application/json", """ 
            { "title":"updated title", "description":"updated desc", "tags":["a","b"] }""".getBytes());
        mockMvc.perform(multipart("/api/contents/{id}", contentId).file(json)
                                                                  .with(req -> {
                                                                      req.setMethod("PATCH");
                                                                      return req;
                                                                  }))
               .andExpect(status().isForbidden());
    }

    // ---------------- 삭제 ----------------
    @Test
    @WithMockUser(roles = {"ADMIN"})
    @DisplayName("콘텐츠 삭제 성공")
    void removeContent_success() throws Exception {
        mockMvc.perform(delete("/api/contents/{id}", contentId))
               .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    @DisplayName("콘텐츠 삭제 실패 - 존재하지 않는 ID")
    void removeContent_fail_notFound() throws Exception {
        Mockito.doThrow(new RuntimeException("not found"))
               .when(contentService)
               .removeContent(contentId);
        mockMvc.perform(delete("/api/contents/{id}", contentId))
               .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = {"USER"})
    @DisplayName("콘텐츠 삭제 실패 - USER 접근 시 Forbidden")
    void removeContent_fail_forbidden() throws Exception {
        mockMvc.perform(delete("/api/contents/{id}", contentId))
               .andExpect(status().isForbidden());
    }
}