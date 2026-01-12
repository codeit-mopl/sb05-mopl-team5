package com.mopl.api.domain.review.controller;

import com.mopl.api.domain.review.dto.request.ReviewCreateRequest;
import com.mopl.api.domain.review.dto.request.ReviewUpdateRequest;
import com.mopl.api.domain.review.dto.response.CursorResponseReviewDto;
import com.mopl.api.domain.review.dto.response.ReviewDto;
import com.mopl.api.domain.review.service.ReviewService;
import com.mopl.api.global.config.security.claim.CustomUserDetails;
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

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<ReviewDto> reviewAdd(
        @Valid @RequestBody ReviewCreateRequest request,
        @AuthenticationPrincipal CustomUserDetails user
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                             .body(reviewService.addReview(request, user.getUserDto().id()));
    }

    @PatchMapping("/{reviewId}")
    public ResponseEntity<ReviewDto> reviewModify(
        @PathVariable UUID reviewId,
        @Valid @RequestBody ReviewUpdateRequest request,
        @AuthenticationPrincipal CustomUserDetails user
    ) {
        return ResponseEntity.ok(reviewService.modifyReview(reviewId, request, user.getUserDto().id()));
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> reviewRemove(
        @PathVariable UUID reviewId,
        @AuthenticationPrincipal CustomUserDetails user
    ) {
        reviewService.removeReview(reviewId, user.getUserDto().id());
        return ResponseEntity.noContent()
                             .build();
    }

    @GetMapping
    public ResponseEntity<CursorResponseReviewDto> reviewList(
        @RequestParam UUID contentId,
        @RequestParam(required = false) String cursor,
        @RequestParam(required = false) UUID idAfter,
        @RequestParam(defaultValue = "20") int limit,
        @RequestParam(defaultValue = "createdAt") String sortBy,
        @RequestParam(defaultValue = "DESCENDING") String sortDirection,
        @AuthenticationPrincipal CustomUserDetails user
    ) {
        UUID currentUserId = user != null ? user.getUserDto().id() : null;
        return ResponseEntity.ok(
            reviewService.getReviews(contentId, cursor, idAfter, limit, sortBy, sortDirection, currentUserId)
        );
    }
}
