package com.mopl.api.domain.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mopl.api.domain.content.entity.Content;
import com.mopl.api.domain.content.entity.ContentType;
import com.mopl.api.domain.content.exception.detail.ContentNotFoundException;
import com.mopl.api.domain.content.repository.ContentRepository;
import com.mopl.api.domain.review.dto.request.ReviewCreateRequest;
import com.mopl.api.domain.review.dto.request.ReviewUpdateRequest;
import com.mopl.api.domain.review.dto.response.AuthorDto;
import com.mopl.api.domain.review.dto.response.CursorResponseReviewDto;
import com.mopl.api.domain.review.dto.response.ReviewDto;
import com.mopl.api.domain.review.entity.Review;
import com.mopl.api.domain.review.exception.detail.ReviewAlreadyExistsException;
import com.mopl.api.domain.review.exception.detail.ReviewNotFoundException;
import com.mopl.api.domain.review.exception.detail.ReviewUnauthorizedException;
import com.mopl.api.domain.review.mapper.ReviewMapper;
import com.mopl.api.domain.review.repository.ReviewRepository;
import com.mopl.api.domain.user.entity.User;
import com.mopl.api.domain.user.entity.UserRole;
import com.mopl.api.domain.user.exception.user.UserErrorCode;
import com.mopl.api.domain.user.exception.user.detail.UserNotFoundException;
import com.mopl.api.domain.user.repository.UserRepository;
import com.navercorp.fixturemonkey.FixtureMonkey;
import com.navercorp.fixturemonkey.api.introspector.FieldReflectionArbitraryIntrospector;
import java.math.BigDecimal;
import java.util.Arrays;
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
import org.springframework.cache.CacheManager;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewService 단위 테스트")
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ContentRepository contentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ReviewMapper reviewMapper;

    @Mock
    private CacheManager cacheManager;

    @InjectMocks
    private ReviewService reviewService;

    private FixtureMonkey fixtureMonkey;
    private UUID userId;
    private UUID contentId;
    private UUID reviewId;

    @BeforeEach
    void setUp() {
        fixtureMonkey = FixtureMonkey.builder()
            .objectIntrospector(FieldReflectionArbitraryIntrospector.INSTANCE)
            .build();
        
        userId = UUID.randomUUID();
        contentId = UUID.randomUUID();
        reviewId = UUID.randomUUID();
    }

    @Test
    @DisplayName("리뷰 생성 성공")
    void addReview_Success() {
        ReviewCreateRequest request = new ReviewCreateRequest(contentId, "Great movie!", 5.0);
        
        User user = fixtureMonkey.giveMeBuilder(User.class)
            .set("id", userId)
            .set("role", UserRole.USER)
            .set("locked", false)
            .sample();
        
        Content content = fixtureMonkey.giveMeBuilder(Content.class)
            .set("id", contentId)
            .set("type", ContentType.MOVIE)
            .set("isDeleted", false)
            .sample();
        
        Review review = fixtureMonkey.giveMeBuilder(Review.class)
            .set("id", reviewId)
            .set("content", content)
            .set("user", user)
            .set("text", "Great movie!")
            .set("rating", BigDecimal.valueOf(5.0))
            .set("isDeleted", false)
            .sample();
        
        AuthorDto authorDto = AuthorDto.builder()
            .userId(userId)
            .name(user.getName())
            .profileImageUrl(user.getProfileImageUrl())
            .build();
        
        ReviewDto expectedDto = ReviewDto.builder()
            .id(reviewId)
            .contentId(contentId)
            .author(authorDto)
            .text("Great movie!")
            .rating(5.0)
            .createdAt(review.getCreatedAt())
            .updatedAt(review.getUpdatedAt())
            .isAuthor(true)
            .build();

        when(contentRepository.findById(contentId)).thenReturn(Optional.of(content));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(reviewRepository.existsByContentIdAndUserIdAndIsDeletedFalse(contentId, userId)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenReturn(review);
        when(reviewMapper.toDto(any(Review.class), eq(true))).thenReturn(expectedDto);

        ReviewDto result = reviewService.addReview(request, userId);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(reviewId);
        verify(contentRepository).findById(contentId);
        verify(userRepository).findById(userId);
        verify(reviewRepository).existsByContentIdAndUserIdAndIsDeletedFalse(contentId, userId);
        verify(reviewRepository).save(any(Review.class));
        verify(contentRepository).incrementRating(eq(contentId), eq(50L));
    }

    @Test
    @DisplayName("리뷰 생성 실패 - 존재하지 않는 콘텐츠")
    void addReview_ContentNotFound() {
        ReviewCreateRequest request = new ReviewCreateRequest(contentId, "Great movie!", 5.0);

        when(contentRepository.findById(contentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.addReview(request, userId))
            .isInstanceOf(ContentNotFoundException.class)
            .hasMessageContaining("존재하지 않는 콘텐츠입니다");

        verify(contentRepository).findById(contentId);
        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("리뷰 생성 실패 - 존재하지 않는 사용자")
    void addReview_UserNotFound() {
        ReviewCreateRequest request = new ReviewCreateRequest(contentId, "Great movie!", 5.0);
        
        Content content = fixtureMonkey.giveMeBuilder(Content.class)
            .set("id", contentId)
            .sample();

        when(contentRepository.findById(contentId)).thenReturn(Optional.of(content));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.addReview(request, userId))
            .isInstanceOf(UserNotFoundException.class)
            .hasMessageContaining(UserErrorCode.USER_NOT_FOUND.getMessage());

        verify(userRepository).findById(userId);
        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("리뷰 생성 실패 - 중복 리뷰")
    void addReview_DuplicateReview() {
        ReviewCreateRequest request = new ReviewCreateRequest(contentId, "Great movie!", 5.0);
        
        Content content = fixtureMonkey.giveMeBuilder(Content.class)
            .set("id", contentId)
            .sample();
        
        User user = fixtureMonkey.giveMeBuilder(User.class)
            .set("id", userId)
            .sample();

        when(contentRepository.findById(contentId)).thenReturn(Optional.of(content));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(reviewRepository.existsByContentIdAndUserIdAndIsDeletedFalse(contentId, userId)).thenReturn(true);

        assertThatThrownBy(() -> reviewService.addReview(request, userId))
            .isInstanceOf(ReviewAlreadyExistsException.class)
            .hasMessageContaining("이미 리뷰를 작성한 콘텐츠입니다");

        verify(reviewRepository).existsByContentIdAndUserIdAndIsDeletedFalse(contentId, userId);
        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("리뷰 수정 성공")
    void modifyReview_Success() {
        ReviewUpdateRequest request = new ReviewUpdateRequest("Updated text", 4.0);
        
        User user = fixtureMonkey.giveMeBuilder(User.class)
            .set("id", userId)
            .sample();
        
        Content content = fixtureMonkey.giveMeBuilder(Content.class)
            .set("id", contentId)
            .sample();
        
        Review review = fixtureMonkey.giveMeBuilder(Review.class)
            .set("id", reviewId)
            .set("user", user)
            .set("content", content)
            .set("rating", BigDecimal.valueOf(4.0))
            .set("isDeleted", false)
            .sample();
        
        AuthorDto authorDto = AuthorDto.builder()
            .userId(userId)
            .name(user.getName())
            .profileImageUrl(user.getProfileImageUrl())
            .build();
        
        ReviewDto expectedDto = ReviewDto.builder()
            .id(reviewId)
            .contentId(contentId)
            .author(authorDto)
            .text("Updated text")
            .rating(4.0)
            .createdAt(review.getCreatedAt())
            .updatedAt(review.getUpdatedAt())
            .isAuthor(true)
            .build();

        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
        when(reviewRepository.save(review)).thenReturn(review);
        when(reviewMapper.toDto(review, true)).thenReturn(expectedDto);

        ReviewDto result = reviewService.modifyReview(reviewId, request, userId);

        assertThat(result).isNotNull();
        assertThat(result.text()).isEqualTo("Updated text");
        verify(reviewRepository).findById(reviewId);
        verify(reviewRepository).save(review);
        verify(contentRepository).updateRating(eq(contentId), eq(40L), eq(40L));
    }

    @Test
    @DisplayName("리뷰 수정 실패 - 존재하지 않는 리뷰")
    void modifyReview_ReviewNotFound() {
        ReviewUpdateRequest request = new ReviewUpdateRequest("Updated text", 4.0);

        when(reviewRepository.findById(reviewId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.modifyReview(reviewId, request, userId))
            .isInstanceOf(ReviewNotFoundException.class)
            .hasMessageContaining("존재하지 않는 리뷰입니다");

        verify(reviewRepository).findById(reviewId);
        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("리뷰 수정 실패 - 권한 없음")
    void modifyReview_Unauthorized() {
        UUID differentUserId = UUID.randomUUID();
        ReviewUpdateRequest request = new ReviewUpdateRequest("Updated text", 4.0);

        User user = fixtureMonkey.giveMeBuilder(User.class)
            .set("id", userId)
            .sample();
        
        Review review = fixtureMonkey.giveMeBuilder(Review.class)
            .set("id", reviewId)
            .set("user", user)
            .sample();

        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.modifyReview(reviewId, request, differentUserId))
            .isInstanceOf(ReviewUnauthorizedException.class)
            .hasMessageContaining("리뷰에 대한 권한이 없습니다");

        verify(reviewRepository).findById(reviewId);
        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("리뷰 삭제 성공")
    void removeReview_Success() {
        User user = fixtureMonkey.giveMeBuilder(User.class)
            .set("id", userId)
            .sample();
        
        Content content = fixtureMonkey.giveMeBuilder(Content.class)
            .set("id", contentId)
            .sample();
        
        Review review = fixtureMonkey.giveMeBuilder(Review.class)
            .set("id", reviewId)
            .set("user", user)
            .set("content", content)
            .set("rating", BigDecimal.valueOf(5.0))
            .set("isDeleted", false)
            .sample();

        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
        when(reviewRepository.save(review)).thenReturn(review);

        reviewService.removeReview(reviewId, userId);

        verify(contentRepository).decrementRating(eq(contentId), eq(50L));
    }

    @Test
    @DisplayName("리뷰 생성 시 콘텐츠 평점 재계산 - 여러 리뷰 평균")
    void recalculateContentRating_MultipleReviews() {
        ReviewCreateRequest request = new ReviewCreateRequest(contentId, "Good movie", 4.0);
        
        User user = fixtureMonkey.giveMeBuilder(User.class)
            .set("id", userId)
            .sample();
        
        Content content = fixtureMonkey.giveMeBuilder(Content.class)
            .set("id", contentId)
            .sample();
        
        Review newReview = fixtureMonkey.giveMeBuilder(Review.class)
            .set("content", content)
            .set("rating", BigDecimal.valueOf(4.0))
            .set("isDeleted", false)
            .sample();
        
        Review existingReview1 = fixtureMonkey.giveMeBuilder(Review.class)
            .set("content", content)
            .set("rating", BigDecimal.valueOf(5.0))
            .set("isDeleted", false)
            .sample();
        
        Review existingReview2 = fixtureMonkey.giveMeBuilder(Review.class)
            .set("content", content)
            .set("rating", BigDecimal.valueOf(3.0))
            .set("isDeleted", false)
            .sample();
        
        ReviewDto expectedDto = mock(ReviewDto.class);

        when(contentRepository.findById(contentId)).thenReturn(Optional.of(content));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(reviewRepository.existsByContentIdAndUserIdAndIsDeletedFalse(contentId, userId)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenReturn(newReview);
        when(reviewMapper.toDto(any(Review.class), eq(true))).thenReturn(expectedDto);

        reviewService.addReview(request, userId);

        verify(contentRepository).incrementRating(eq(contentId), eq(40L));
    }

    @Test
    @DisplayName("리뷰 수정 시 콘텐츠 평점 재계산")
    void recalculateContentRating_OnUpdate() {
        ReviewUpdateRequest request = new ReviewUpdateRequest("Updated text", 3.0);
        
        User user = fixtureMonkey.giveMeBuilder(User.class)
            .set("id", userId)
            .sample();
        
        Content content = fixtureMonkey.giveMeBuilder(Content.class)
            .set("id", contentId)
            .sample();
        
        Review review = fixtureMonkey.giveMeBuilder(Review.class)
            .set("id", reviewId)
            .set("user", user)
            .set("content", content)
            .set("rating", BigDecimal.valueOf(5.0))
            .set("isDeleted", false)
            .sample();
        
        Review existingReview = fixtureMonkey.giveMeBuilder(Review.class)
            .set("content", content)
            .set("rating", BigDecimal.valueOf(4.0))
            .set("isDeleted", false)
            .sample();
        
        ReviewDto expectedDto = mock(ReviewDto.class);

        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
        when(reviewRepository.save(review)).thenReturn(review);
        when(reviewMapper.toDto(review, true)).thenReturn(expectedDto);

        reviewService.modifyReview(reviewId, request, userId);

        verify(contentRepository).updateRating(eq(contentId), eq(50L), eq(30L));
    }

    @Test
    @DisplayName("리뷰 삭제 시 콘텐츠 평점 재계산 - 남은 리뷰 있음")
    void recalculateContentRating_OnDelete_RemainingReviews() {
        User user = fixtureMonkey.giveMeBuilder(User.class)
            .set("id", userId)
            .sample();
        
        Content content = fixtureMonkey.giveMeBuilder(Content.class)
            .set("id", contentId)
            .sample();
        
        Review review = fixtureMonkey.giveMeBuilder(Review.class)
            .set("id", reviewId)
            .set("user", user)
            .set("content", content)
            .set("rating", BigDecimal.valueOf(5.0))
            .set("isDeleted", false)
            .sample();
        
        Review remainingReview = fixtureMonkey.giveMeBuilder(Review.class)
            .set("content", content)
            .set("rating", BigDecimal.valueOf(4.0))
            .set("isDeleted", false)
            .sample();

        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
        when(reviewRepository.save(review)).thenReturn(review);

        reviewService.removeReview(reviewId, userId);

        verify(contentRepository).decrementRating(eq(contentId), eq(50L));
    }

    @Test
    @DisplayName("리뷰 삭제 시 콘텐츠 평점 재계산 - 마지막 리뷰 삭제")
    void recalculateContentRating_OnDelete_LastReview() {
        User user = fixtureMonkey.giveMeBuilder(User.class)
            .set("id", userId)
            .sample();
        
        Content content = fixtureMonkey.giveMeBuilder(Content.class)
            .set("id", contentId)
            .sample();
        
        Review review = fixtureMonkey.giveMeBuilder(Review.class)
            .set("id", reviewId)
            .set("user", user)
            .set("content", content)
            .set("rating", BigDecimal.valueOf(5.0))
            .set("isDeleted", false)
            .sample();

        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
        when(reviewRepository.save(review)).thenReturn(review);

        reviewService.removeReview(reviewId, userId);

        verify(contentRepository).decrementRating(eq(contentId), eq(50L));
    }
}