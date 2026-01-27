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
import com.mopl.api.domain.user.exception.user.detail.UserNotFoundException;
import com.mopl.api.domain.user.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ContentRepository contentRepository;
    private final UserRepository userRepository;
    private final ReviewMapper reviewMapper;
    private final CacheManager cacheManager;

    @Transactional
    @CacheEvict(value = "reviewCount", key = "#request.contentId()")
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

    @Transactional
    public void removeReview(UUID reviewId, UUID userId) {
        Review review = reviewRepository.findById(reviewId)
                                        .orElseThrow(() -> ReviewNotFoundException.withReviewId(reviewId));

        if (!review.getUser()
                   .getId()
                   .equals(userId)) {
            throw ReviewUnauthorizedException.withDetails(reviewId, userId);
        }

        UUID contentId = review.getContent().getId();

        review.softDelete();

        reviewRepository.save(review);

        recalculateContentRating(contentId);

        Optional.ofNullable(cacheManager.getCache("reviewCount"))
                .ifPresent(cache -> cache.evict(contentId));
    }

    public CursorResponseReviewDto getReviews(
        UUID contentId,
        String cursor,
        UUID idAfter,
        int limit,
        String sortBy,
        String sortDirection,
        UUID currentUserId
    ) {
        // 1. 커서 값 파싱
        LocalDateTime cursorDateTime = null;
        BigDecimal cursorRating = null;

        if (cursor != null && !cursor.isBlank() && idAfter != null) {
            if ("createdAt".equals(sortBy)) {
                cursorDateTime = LocalDateTime.parse(cursor);
            } else if ("rating".equals(sortBy)) {
                cursorRating = new BigDecimal(cursor);
            }
        }

        // 2. 리뷰 조회 (limit+1개)
        // +1개를 조회하여 다음 페이지 존재 여부 확인
        List<Review> reviews = reviewRepository.findReviewsWithCursor(
            contentId,
            sortBy,
            sortDirection,
            cursorDateTime,
            cursorRating,
            idAfter,
            limit
        );

        // 3. 다음 페이지 존재 여부 확인
        boolean hasNext = reviews.size() > limit;
        if (hasNext) {
            // limit+1개가 조회되었다면 마지막 항목 제거 (실제 응답에는 limit개만 포함)
            reviews = reviews.subList(0, limit);
        }

        List<ReviewDto> reviewDtos = reviews.stream()
                                            .map(review -> {
                                                // isAuthor 계산: 현재 사용자와 리뷰 작성자가 같은지 확인
                                                boolean isAuthor = currentUserId != null && review.getUser()
                                                                                                  .getId()
                                                                                                  .equals(
                                                                                                      currentUserId);
                                                return reviewMapper.toDto(review, isAuthor);
                                            })
                                            .toList(); // Java 16+: 불변 리스트 생성

        // 5. 다음 커서 값 계산
        String nextCursor = null;
        UUID nextIdAfter = null;
        if (hasNext && !reviews.isEmpty()) {
            Review lastReview = reviews.get(reviews.size() - 1);
            if ("createdAt".equals(sortBy)) {
                nextCursor = lastReview.getCreatedAt()
                                       .toString();
            } else if ("rating".equals(sortBy)) {
                nextCursor = lastReview.getRating()
                                       .toString();
            }
            nextIdAfter = lastReview.getId();
        }

        // 6. 전체 개수 조회
        long totalCount = reviewRepository.countReviewsByContentId(contentId);

        // 7. 응답 DTO 생성
        return new CursorResponseReviewDto(
            reviewDtos,
            nextCursor,
            nextIdAfter,
            hasNext,
            (int) totalCount,
            sortBy,
            sortDirection
        );
    }

    private void recalculateContentRating(UUID contentId) {
        Content content = contentRepository.findById(contentId)
                                           .orElseThrow(() -> ContentNotFoundException.withContentId(contentId));

        List<Review> activeReviews = reviewRepository.findActiveReviewsByContentId(contentId);

        if (activeReviews.isEmpty()) {
            content.updateRatingStats(BigDecimal.ZERO, 0L);
        } else {
            BigDecimal sum = activeReviews.stream()
                                          .map(Review::getRating)
                                          .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal average = sum.divide(BigDecimal.valueOf(activeReviews.size()), 1, RoundingMode.HALF_UP);

            content.updateRatingStats(average, (long) activeReviews.size());
        }

        contentRepository.save(content);
    }
}
