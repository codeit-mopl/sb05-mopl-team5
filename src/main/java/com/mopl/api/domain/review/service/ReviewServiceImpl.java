package com.mopl.api.domain.review.service;

import com.mopl.api.domain.content.entity.Content;
import com.mopl.api.domain.content.repository.ContentRepository;
import com.mopl.api.domain.review.dto.request.ReviewCreateRequest;
import com.mopl.api.domain.review.dto.request.ReviewUpdateRequest;
import com.mopl.api.domain.review.dto.response.CursorResponseReviewDto;
import com.mopl.api.domain.review.dto.response.ReviewDto;
import com.mopl.api.domain.review.entity.Review;
import com.mopl.api.domain.review.mapper.ReviewMapper;
import com.mopl.api.domain.review.repository.ReviewRepository;
import com.mopl.api.domain.user.entity.User;
import com.mopl.api.domain.user.repository.UserRepository;
import java.math.BigDecimal;
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
        Content content = contentRepository.findById(request.contentId())
                                           .orElseThrow(() -> new RuntimeException(
                                               "Content not found with id: " + request.contentId()));

        User user = userRepository.findById(userId)
                                  .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        if (reviewRepository.existsByContentIdAndUserIdAndIsDeletedFalse(content.getId(), userId)) {
            throw new RuntimeException("User already reviewed this content");
        }

        BigDecimal rating = BigDecimal.valueOf(request.rating());

        Review review = Review.create(content, user, request.text(), rating);

        reviewRepository.save(review);

        //TODO: 콘텐츠 평점 재계산(콘텐츠 생성 기능 완성 된 이후 진행)

        return reviewMapper.toDto(review, true);
    }

    @Override
    @Transactional
    public ReviewDto modifyReview(UUID reviewId, ReviewUpdateRequest request, UUID userId) {
        Review review = reviewRepository.findById(reviewId)
                                        .orElseThrow(
                                            () -> new RuntimeException("Review not found with id: " + reviewId));

        if (!review.getUser()
                   .getId()
                   .equals(userId)) {
            throw new RuntimeException("User is not authorized to modify this review");
        }

        BigDecimal rating = BigDecimal.valueOf(request.rating());

        review.update(request.text(), rating);

        reviewRepository.save(review);

        //TODO: 콘텐츠 평점 재계산(콘텐츠 생성 기능 완성 된 이후 진행)

        return reviewMapper.toDto(review, true);
    }

    @Override
    @Transactional
    public void removeReview(UUID reviewId, UUID userId) {
        Review review = reviewRepository.findById(reviewId)
                                        .orElseThrow(
                                            () -> new RuntimeException("Review not found with id: " + reviewId));

        if (!review.getUser()
                   .getId()
                   .equals(userId)) {
            throw new RuntimeException("User is not authorized to delete this review");
        }

        review.softDelete();

        reviewRepository.save(review);

        //TODO: 콘텐츠 평점 재계산(콘텐츠 생성 기능 완성 된 이후 진행)
    }

    @Override
    public CursorResponseReviewDto getReviews(UUID contentId, String cursor, UUID idAfter, int limit, String sortBy,
        String sortDirection, UUID currentUserId) {
        return new CursorResponseReviewDto(new ArrayList<>(), null, null, false, 0, sortBy, sortDirection);
    }
}
