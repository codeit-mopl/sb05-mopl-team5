package com.mopl.api.domain.review.service;

import com.mopl.api.domain.content.repository.ContentRepository;
import com.mopl.api.domain.review.dto.request.ReviewCreateRequest;
import com.mopl.api.domain.review.dto.request.ReviewUpdateRequest;
import com.mopl.api.domain.review.dto.response.CursorResponseReviewDto;
import com.mopl.api.domain.review.dto.response.ReviewDto;
import com.mopl.api.domain.review.mapper.ReviewMapper;
import com.mopl.api.domain.review.repository.ReviewRepository;
import com.mopl.api.domain.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ContentRepository contentRepository;
    private final UserRepository userRepository;
    private final ReviewMapper reviewMapper;

    @Override
    @Transactional
    public ReviewDto addReview(ReviewCreateRequest request, UUID userId) {
        return new ReviewDto(
            UUID.randomUUID(),
            request.contentId(),
            request.text(),
            request.rating(),
            LocalDateTime.now(),
            LocalDateTime.now(),
            true
        );
    }

    @Override
    @Transactional
    public ReviewDto modifyReview(UUID reviewId, ReviewUpdateRequest request, UUID userId) {
        return new ReviewDto(
            reviewId,
            UUID.randomUUID(),
            request.text(),
            request.rating(),
            LocalDateTime.now().minusDays(1),
            LocalDateTime.now(),
            true
        );
    }

    @Override
    @Transactional
    public void removeReview(UUID reviewId, UUID userId) {
    }

    @Override
    public CursorResponseReviewDto getReviews(
        UUID contentId,
        String cursor,
        UUID idAfter,
        int limit,
        String sortBy,
        String sortDirection,
        UUID currentUserId
    ) {
        return new CursorResponseReviewDto(
            new ArrayList<>(),
            null,
            null,
            false,
            0,
            sortBy,
            sortDirection
        );
    }
}
