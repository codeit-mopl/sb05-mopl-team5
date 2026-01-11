package com.mopl.api.domain.review.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mopl.api.config.TestSecurityConfig;
import com.mopl.api.config.WithMockCustomUser;
import com.mopl.api.domain.review.dto.request.ReviewCreateRequest;
import com.mopl.api.domain.review.dto.request.ReviewUpdateRequest;
import com.mopl.api.domain.review.dto.response.CursorResponseReviewDto;
import com.mopl.api.domain.review.dto.response.ReviewDto;
import com.mopl.api.domain.review.service.ReviewService;
import com.mopl.api.domain.user.dto.response.UserDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.mopl.api.domain.user.entity.UserRole.USER;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
@WithMockCustomUser
@DisplayName("ReviewController 통합 테스트")
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ReviewService reviewService;

    private UUID userId;
    private UUID reviewId;
    private UUID contentId;
    private ReviewCreateRequest createRequest;
    private ReviewUpdateRequest updateRequest;
    private ReviewDto reviewDto;
    private UserDto userDto;

    @BeforeEach
    void setUp() {
        userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        reviewId = UUID.randomUUID();
        contentId = UUID.randomUUID();

        userDto = UserDto.builder()
                         .id(userId)
                         .createdAt(LocalDateTime.now())
                         .email("test@example.com")
                         .name("testuser")
                         .profileImageUrl("http://example.com/profile.jpg")
                         .role(USER)
                         .locked(false)
                         .build();

        createRequest = new ReviewCreateRequest(contentId, "Great content!", 4.5);

        updateRequest = new ReviewUpdateRequest("Updated review", 5.0);

        reviewDto = new ReviewDto(
            reviewId,
            contentId,
            userDto,
            "Great content!",
            4.5,
            LocalDateTime.now(),
            LocalDateTime.now(),
            true
        );
    }

    @Test
    @DisplayName("POST /api/reviews - 리뷰 생성 성공")
    void reviewAdd_Success() throws Exception {
        when(reviewService.addReview(any(ReviewCreateRequest.class), eq(userId)))
            .thenReturn(reviewDto);

        mockMvc.perform(post("/api/reviews")
                   .with(csrf())
                   .contentType(MediaType.APPLICATION_JSON)
                   .content(objectMapper.writeValueAsString(createRequest)))
               .andExpect(status().isCreated())
               .andExpect(jsonPath("$.id").value(reviewId.toString()))
               .andExpect(jsonPath("$.text").value("Great content!"))
               .andExpect(jsonPath("$.rating").value(4.5));
    }

    @Test
    @DisplayName("POST /api/reviews - 중복 리뷰 생성 실패 (409 Conflict)")
    void reviewAdd_DuplicateReview() throws Exception {
        when(reviewService.addReview(any(ReviewCreateRequest.class), eq(userId)))
            .thenThrow(new RuntimeException("이미 해당 콘텐츠에 리뷰를 작성하셨습니다."));

        mockMvc.perform(post("/api/reviews")
                   .with(csrf())
                   .contentType(MediaType.APPLICATION_JSON)
                   .content(objectMapper.writeValueAsString(createRequest)))
               .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/reviews - 유효하지 않은 요청 (400 Bad Request)")
    void reviewAdd_InvalidRequest() throws Exception {
        ReviewCreateRequest invalidRequest = new ReviewCreateRequest(null, "", 6.0);

        mockMvc.perform(post("/api/reviews")
                   .with(csrf())
                   .contentType(MediaType.APPLICATION_JSON)
                   .content(objectMapper.writeValueAsString(invalidRequest)))
               .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH /api/reviews/{reviewId} - 리뷰 수정 성공")
    void reviewModify_Success() throws Exception {
        ReviewDto updatedDto = new ReviewDto(
            reviewId,
            contentId,
            userDto,
            "Updated review",
            5.0,
            LocalDateTime.now(),
            LocalDateTime.now(),
            true
        );

        when(reviewService.modifyReview(eq(reviewId), any(ReviewUpdateRequest.class), eq(userId)))
            .thenReturn(updatedDto);

        mockMvc.perform(patch("/api/reviews/{reviewId}", reviewId)
                   .with(csrf())
                   .contentType(MediaType.APPLICATION_JSON)
                   .content(objectMapper.writeValueAsString(updateRequest)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.text").value("Updated review"))
               .andExpect(jsonPath("$.rating").value(5.0));
    }

    @Test
    @DisplayName("PATCH /api/reviews/{reviewId} - 존재하지 않는 리뷰 (404 Not Found)")
    void reviewModify_NotFound() throws Exception {
        when(reviewService.modifyReview(eq(reviewId), any(ReviewUpdateRequest.class), eq(userId)))
            .thenThrow(new RuntimeException("리뷰를 찾을 수 없습니다."));

        mockMvc.perform(patch("/api/reviews/{reviewId}", reviewId)
                   .with(csrf())
                   .contentType(MediaType.APPLICATION_JSON)
                   .content(objectMapper.writeValueAsString(updateRequest)))
               .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH /api/reviews/{reviewId} - 권한 없음 (403 Forbidden)")
    void reviewModify_Unauthorized() throws Exception {
        when(reviewService.modifyReview(eq(reviewId), any(ReviewUpdateRequest.class), eq(userId)))
            .thenThrow(new RuntimeException("리뷰를 수정할 권한이 없습니다."));

        mockMvc.perform(patch("/api/reviews/{reviewId}", reviewId)
                   .with(csrf())
                   .contentType(MediaType.APPLICATION_JSON)
                   .content(objectMapper.writeValueAsString(updateRequest)))
               .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /api/reviews/{reviewId} - 리뷰 삭제 성공")
    void reviewRemove_Success() throws Exception {
        doNothing().when(reviewService)
                   .removeReview(eq(reviewId), eq(userId));

        mockMvc.perform(delete("/api/reviews/{reviewId}", reviewId)
                   .with(csrf()))
               .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/reviews/{reviewId} - 존재하지 않는 리뷰 (404 Not Found)")
    void reviewRemove_NotFound() throws Exception {
        doThrow(new RuntimeException("리뷰를 찾을 수 없습니다."))
            .when(reviewService)
            .removeReview(eq(reviewId), eq(userId));

        mockMvc.perform(delete("/api/reviews/{reviewId}", reviewId)
                   .with(csrf()))
               .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /api/reviews/{reviewId} - 권한 없음 (403 Forbidden)")
    void reviewRemove_Unauthorized() throws Exception {
        doThrow(new RuntimeException("리뷰를 삭제할 권한이 없습니다."))
            .when(reviewService)
            .removeReview(eq(reviewId), eq(userId));

        mockMvc.perform(delete("/api/reviews/{reviewId}", reviewId)
                   .with(csrf()))
               .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/reviews - 리뷰 목록 조회 성공")
    void reviewList_Success() throws Exception {
        CursorResponseReviewDto response = new CursorResponseReviewDto(
            List.of(reviewDto),
            null,
            null,
            false,
            1,
            "createdAt",
            "DESC"
        );

        when(reviewService.getReviews(
            eq(contentId),
            any(),
            any(),
            eq(20),
            eq("createdAt"),
            eq("DESC"),
            eq(userId)
        )).thenReturn(response);

        mockMvc.perform(get("/api/reviews")
                   .param("contentId", contentId.toString())
                   .param("limit", "20")
                   .param("sortBy", "createdAt")
                   .param("sortDirection", "DESC"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.data").isArray())
               .andExpect(jsonPath("$.data[0].id").value(reviewId.toString()))
               .andExpect(jsonPath("$.hasNext").value(false))
               .andExpect(jsonPath("$.totalCount").value(1));
    }

    @Test
    @DisplayName("GET /api/reviews - 커서 페이지네이션 조회 성공")
    void reviewList_WithCursor() throws Exception {
        String cursor = "2024-01-01T10:00:00";
        UUID idAfter = UUID.randomUUID();

        CursorResponseReviewDto response = new CursorResponseReviewDto(
            List.of(reviewDto),
            "2024-01-01T09:00:00",
            reviewId,
            true,
            10,
            "createdAt",
            "DESC"
        );

        when(reviewService.getReviews(
            eq(contentId),
            eq(cursor),
            eq(idAfter),
            eq(10),
            eq("createdAt"),
            eq("DESC"),
            eq(userId)
        )).thenReturn(response);

        mockMvc.perform(get("/api/reviews")
                   .param("contentId", contentId.toString())
                   .param("cursor", cursor)
                   .param("idAfter", idAfter.toString())
                   .param("limit", "10")
                   .param("sortBy", "createdAt")
                   .param("sortDirection", "DESC"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.hasNext").value(true))
               .andExpect(jsonPath("$.nextCursor").value("2024-01-01T09:00:00"));
    }
}
