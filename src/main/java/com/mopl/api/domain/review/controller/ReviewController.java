package com.mopl.api.domain.review.controller;

import com.mopl.api.domain.review.dto.request.ReviewCreateRequest;
import com.mopl.api.domain.review.dto.request.ReviewUpdateRequest;
import com.mopl.api.domain.review.dto.response.CursorResponseReviewDto;
import com.mopl.api.domain.review.dto.response.ReviewDto;
import com.mopl.api.domain.review.service.ReviewService;
import com.mopl.api.global.config.security.claim.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "리뷰 관리", description = "리뷰 관리 API")
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "리뷰 생성", description = "콘텐츠에 대한 리뷰를 생성합니다.")
    @PostMapping
    public ResponseEntity<ReviewDto> reviewAdd(
        @Valid @RequestBody ReviewCreateRequest request,
        @AuthenticationPrincipal CustomUserDetails user
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                             .body(reviewService.addReview(request, user.getUserDto().id()));
    }

    @Operation(summary = "리뷰 수정", description = "작성한 리뷰를 수정합니다.")
    @PatchMapping("/{reviewId}")
    public ResponseEntity<ReviewDto> reviewModify(
        @PathVariable UUID reviewId,
        @Valid @RequestBody ReviewUpdateRequest request,
        @AuthenticationPrincipal CustomUserDetails user
    ) {
        return ResponseEntity.ok(reviewService.modifyReview(reviewId, request, user.getUserDto().id()));
    }

    @Operation(summary = "리뷰 삭제", description = "작성한 리뷰를 삭제합니다.")
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> reviewRemove(
        @PathVariable UUID reviewId,
        @AuthenticationPrincipal CustomUserDetails user
    ) {
        reviewService.removeReview(reviewId, user.getUserDto().id());
        return ResponseEntity.noContent()
                             .build();
    }

    @Operation(summary = "리뷰 목록 조회 (커서 페이지네이션)", description = "콘텐츠의 리뷰 목록을 커서 기반 페이징으로 조회합니다.")
    @GetMapping
    public ResponseEntity<CursorResponseReviewDto> reviewList(
        @RequestParam UUID contentId,
        @RequestParam(required = false) String cursor,
        @RequestParam(required = false) UUID idAfter,
        @RequestParam int limit,
        @RequestParam String sortBy,
        @RequestParam String sortDirection,
        @AuthenticationPrincipal CustomUserDetails user
    ) {
        UUID currentUserId = user != null ? user.getUserDto().id() : null;
        return ResponseEntity.ok(
            reviewService.getReviews(contentId, cursor, idAfter, limit, sortBy, sortDirection, currentUserId)
        );
    }
}
