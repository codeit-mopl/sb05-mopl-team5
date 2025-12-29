package com.mopl.api.domain.review.service;

import com.mopl.api.domain.review.dto.request.ReviewCreateRequest;
import com.mopl.api.domain.review.dto.request.ReviewUpdateRequest;
import com.mopl.api.domain.review.dto.response.CursorResponseReviewDto;
import com.mopl.api.domain.review.dto.response.ReviewDto;
import java.util.UUID;

public interface ReviewService {

    ReviewDto addReview(ReviewCreateRequest request, UUID userId);

    ReviewDto modifyReview(UUID reviewId, ReviewUpdateRequest request, UUID userId);

    void removeReview(UUID reviewId, UUID userId);

    CursorResponseReviewDto getReviews(
        UUID contentId,
        String cursor,
        UUID idAfter,
        int limit,
        String sortBy,
        String sortDirection,
        UUID currentUserId
    );
}
