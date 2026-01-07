package com.mopl.api.domain.review.service;

import com.mopl.api.domain.content.entity.Content;
import com.mopl.api.domain.content.exception.detail.ContentNotFoundException;
import com.mopl.api.domain.content.repository.ContentRepository;
import com.mopl.api.domain.review.dto.request.ReviewCreateRequest;
import com.mopl.api.domain.review.dto.request.ReviewUpdateRequest;
import com.mopl.api.domain.review.dto.response.CursorResponseReviewDto;
import com.mopl.api.domain.review.dto.response.ReviewDto;
import com.mopl.api.domain.review.entity.Review;
import com.mopl.api.domain.review.exception.detail.ReviewAlreadyExistsException;
import com.mopl.api.domain.review.exception.detail.ReviewNotFoundException;
import com.mopl.api.domain.review.exception.detail.ReviewUnauthorizedException;
import com.mopl.api.domain.review.mapper.ReviewMapper;
import com.mopl.api.domain.review.repository.ReviewRepository;
import com.mopl.api.domain.user.entity.User;
import com.mopl.api.domain.user.exception.detail.UserNotFoundException;
import com.mopl.api.domain.user.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
        Content content = contentRepository.findById(request.contentId())
                                           .orElseThrow(
                                               () -> ContentNotFoundException.withContentId(request.contentId()));

        User user = userRepository.findById(userId)
                                  .orElseThrow(() -> UserNotFoundException.withUserId(userId));

        if (reviewRepository.existsByContentIdAndUserIdAndIsDeletedFalse(content.getId(), userId)) {
            throw ReviewAlreadyExistsException.withDetails(content.getId(), userId);
        }

        BigDecimal rating = BigDecimal.valueOf(request.rating());

        Review review = Review.create(content, user, request.text(), rating);

        reviewRepository.save(review);

        recalculateContentRating(content.getId());

        return reviewMapper.toDto(review, true);
    }

    @Override
    @Transactional
    public ReviewDto modifyReview(UUID reviewId, ReviewUpdateRequest request, UUID userId) {
        Review review = reviewRepository.findById(reviewId)
                                        .orElseThrow(() -> ReviewNotFoundException.withReviewId(reviewId));

        if (!review.getUser()
                   .getId()
                   .equals(userId)) {
            throw ReviewUnauthorizedException.withDetails(reviewId, userId);
        }

        BigDecimal rating = BigDecimal.valueOf(request.rating());

        review.update(request.text(), rating);

        reviewRepository.save(review);

        recalculateContentRating(review.getContent()
                                       .getId());

        return reviewMapper.toDto(review, true);
    }

    @Override
    @Transactional
    public void removeReview(UUID reviewId, UUID userId) {
        Review review = reviewRepository.findById(reviewId)
                                        .orElseThrow(() -> ReviewNotFoundException.withReviewId(reviewId));

        if (!review.getUser()
                   .getId()
                   .equals(userId)) {
            throw ReviewUnauthorizedException.withDetails(reviewId, userId);
        }

        review.softDelete();

        reviewRepository.save(review);

        recalculateContentRating(review.getContent()
                                       .getId());
    }

    @Override
    public CursorResponseReviewDto getReviews(UUID contentId, String cursor, UUID idAfter, int limit, String sortBy,
        String sortDirection, UUID currentUserId) {
        return new CursorResponseReviewDto(new ArrayList<>(), null, null, false, 0, sortBy, sortDirection);
    }

    private void recalculateContentRating(UUID contentId) {
        Content content = contentRepository.findById(contentId)
                                           .orElseThrow(() -> ContentNotFoundException.withContentId(contentId));

        // TODO: 성능 개선 - Repository에 전용 쿼리 메서드 추가 필요
        List<Review> activeReviews = reviewRepository.findAll()
                                                     .stream()
                                                     .filter(r -> r.getContent()
                                                                   .getId()
                                                                   .equals(contentId) && !r.getIsDeleted())
                                                     .toList();

        if (activeReviews.isEmpty()) {
            content.updateRatingStats(BigDecimal.ZERO, 0L);
        } else {
            BigDecimal sum = activeReviews.stream()
                                          .map(Review::getRating)
                                          .reduce(BigDecimal.ZERO, BigDecimal::add); // 초기값 0.0, 누적 덧셈

            BigDecimal average = sum.divide(BigDecimal.valueOf(activeReviews.size()), 1, RoundingMode.HALF_UP);

            content.updateRatingStats(average, (long) activeReviews.size());
        }

        contentRepository.save(content);
    }
}
