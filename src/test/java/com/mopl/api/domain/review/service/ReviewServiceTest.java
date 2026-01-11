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
import java.math.BigDecimal;
import java.time.LocalDateTime;
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

    @InjectMocks
    private ReviewServiceImpl reviewService;

    private UUID userId;
    private UUID contentId;
    private UUID reviewId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        contentId = UUID.randomUUID();
        reviewId = UUID.randomUUID();
    }

    @Test
    @DisplayName("리뷰 생성 성공")
    void addReview_Success() {
        ReviewCreateRequest request = new ReviewCreateRequest(contentId, "Great movie!", 5.0);
        
        AuthorDto authorDto = AuthorDto.builder()
                                       .userId(userId)
                                       .name("Test User")
                                       .profileImageUrl(null)
                                       .build();
        
        ReviewDto expectedDto = ReviewDto.builder()
                                         .id(reviewId)
                                         .contentId(contentId)
                                         .author(authorDto)
                                         .text("Great movie!")
                                         .rating(5.0)
                                         .createdAt(null)
                                         .updatedAt(null)
                                         .isAuthor(true)
                                         .build();

        User mockUser = mock(User.class);
        Content mockContent = mock(Content.class);
        Review mockReview = mock(Review.class);

        when(mockContent.getId()).thenReturn(contentId);
        when(mockReview.getRating()).thenReturn(BigDecimal.valueOf(5.0));
        when(mockReview.getIsDeleted()).thenReturn(false);
        when(mockReview.getContent()).thenReturn(mockContent);

        when(contentRepository.findById(contentId)).thenReturn(Optional.of(mockContent));
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(reviewRepository.existsByContentIdAndUserIdAndIsDeletedFalse(contentId, userId)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenReturn(mockReview);
        when(reviewRepository.findAll()).thenReturn(List.of(mockReview));
        when(reviewMapper.toDto(any(Review.class), eq(true))).thenReturn(expectedDto);

        ReviewDto result = reviewService.addReview(request, userId);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(reviewId);
        verify(contentRepository, times(2)).findById(contentId);
        verify(userRepository).findById(userId);
        verify(reviewRepository).existsByContentIdAndUserIdAndIsDeletedFalse(contentId, userId);
        verify(reviewRepository).save(any(Review.class));
        verify(contentRepository).save(mockContent);
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
        Content mockContent = mock(Content.class);

        when(contentRepository.findById(contentId)).thenReturn(Optional.of(mockContent));
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
        Content mockContent = mock(Content.class);
        User mockUser = mock(User.class);

        when(mockContent.getId()).thenReturn(contentId);
        when(contentRepository.findById(contentId)).thenReturn(Optional.of(mockContent));
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
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
        
        AuthorDto authorDto = AuthorDto.builder()
                                       .userId(userId)
                                       .name("Test User")
                                       .profileImageUrl(null)
                                       .build();
        
        ReviewDto expectedDto = ReviewDto.builder()
                                         .id(reviewId)
                                         .contentId(contentId)
                                         .author(authorDto)
                                         .text("Updated text")
                                         .rating(4.0)
                                         .createdAt(null)
                                         .updatedAt(null)
                                         .isAuthor(true)
                                         .build();

        User mockUser = mock(User.class);
        Content mockContent = mock(Content.class);
        Review mockReview = mock(Review.class);

        when(mockUser.getId()).thenReturn(userId);
        when(mockReview.getUser()).thenReturn(mockUser);
        when(mockReview.getContent()).thenReturn(mockContent);
        when(mockReview.getRating()).thenReturn(BigDecimal.valueOf(4.0));
        when(mockReview.getIsDeleted()).thenReturn(false);
        when(mockContent.getId()).thenReturn(contentId);

        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(mockReview));
        when(reviewRepository.save(mockReview)).thenReturn(mockReview);
        when(reviewRepository.findAll()).thenReturn(List.of(mockReview));
        when(contentRepository.findById(contentId)).thenReturn(Optional.of(mockContent));
        when(reviewMapper.toDto(mockReview, true)).thenReturn(expectedDto);

        ReviewDto result = reviewService.modifyReview(reviewId, request, userId);

        assertThat(result).isNotNull();
        assertThat(result.text()).isEqualTo("Updated text");
        verify(reviewRepository).findById(reviewId);
        verify(mockReview).update("Updated text", BigDecimal.valueOf(4.0));
        verify(reviewRepository).save(mockReview);
        verify(contentRepository).save(mockContent);
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

        User mockUser = mock(User.class);
        Review mockReview = mock(Review.class);

        when(mockUser.getId()).thenReturn(userId);
        when(mockReview.getUser()).thenReturn(mockUser);
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(mockReview));

        assertThatThrownBy(() -> reviewService.modifyReview(reviewId, request, differentUserId))
            .isInstanceOf(ReviewUnauthorizedException.class)
            .hasMessageContaining("리뷰에 대한 권한이 없습니다");

        verify(reviewRepository).findById(reviewId);
        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("리뷰 삭제 성공")
    void removeReview_Success() {
        User mockUser = mock(User.class);
        Content mockContent = mock(Content.class);
        Review mockReview = mock(Review.class);

        when(mockUser.getId()).thenReturn(userId);
        when(mockReview.getUser()).thenReturn(mockUser);
        when(mockReview.getContent()).thenReturn(mockContent);
        when(mockReview.getIsDeleted()).thenReturn(true);
        when(mockContent.getId()).thenReturn(contentId);

        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(mockReview));
        when(reviewRepository.save(mockReview)).thenReturn(mockReview);
        when(reviewRepository.findAll()).thenReturn(List.of(mockReview));
        when(contentRepository.findById(contentId)).thenReturn(Optional.of(mockContent));

        reviewService.removeReview(reviewId, userId);

        verify(reviewRepository).findById(reviewId);
        verify(mockReview).softDelete();
        verify(reviewRepository).save(mockReview);
        verify(contentRepository).save(mockContent);
    }

    @Test
    @DisplayName("리뷰 삭제 실패 - 존재하지 않는 리뷰")
    void removeReview_ReviewNotFound() {
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.removeReview(reviewId, userId))
            .isInstanceOf(ReviewNotFoundException.class)
            .hasMessageContaining("존재하지 않는 리뷰입니다");

        verify(reviewRepository).findById(reviewId);
        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("리뷰 삭제 실패 - 권한 없음")
    void removeReview_Unauthorized() {
        UUID differentUserId = UUID.randomUUID();

        User mockUser = mock(User.class);
        Review mockReview = mock(Review.class);

        when(mockUser.getId()).thenReturn(userId);
        when(mockReview.getUser()).thenReturn(mockUser);
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(mockReview));

        assertThatThrownBy(() -> reviewService.removeReview(reviewId, differentUserId))
            .isInstanceOf(ReviewUnauthorizedException.class)
            .hasMessageContaining("리뷰에 대한 권한이 없습니다");

        verify(reviewRepository).findById(reviewId);
        verify(mockReview, never()).softDelete();
        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("리뷰 목록 조회 성공 - createdAt 정렬")
    void getReviews_Success_SortByCreatedAt() {
        String sortBy = "createdAt";
        String sortDirection = "desc";
        int limit = 10;
        String cursor = null;
        UUID idAfter = null;

        User mockUser = mock(User.class);
        when(mockUser.getId()).thenReturn(userId);

        Review mockReview1 = mock(Review.class);
        Review mockReview2 = mock(Review.class);
        when(mockReview1.getUser()).thenReturn(mockUser);
        when(mockReview2.getUser()).thenReturn(mockUser);

        List<Review> mockReviews = Arrays.asList(mockReview1, mockReview2);
        
        AuthorDto authorDto = AuthorDto.builder()
                                       .userId(userId)
                                       .name("Test User")
                                       .profileImageUrl(null)
                                       .build();
        
        ReviewDto mockDto1 = ReviewDto.builder()
                                      .id(UUID.randomUUID())
                                      .contentId(contentId)
                                      .author(authorDto)
                                      .text("Great!")
                                      .rating(5.0)
                                      .createdAt(null)
                                      .updatedAt(null)
                                      .isAuthor(true)
                                      .build();
        
        ReviewDto mockDto2 = ReviewDto.builder()
                                      .id(UUID.randomUUID())
                                      .contentId(contentId)
                                      .author(authorDto)
                                      .text("Good!")
                                      .rating(4.0)
                                      .createdAt(null)
                                      .updatedAt(null)
                                      .isAuthor(true)
                                      .build();

        when(reviewRepository.findReviewsWithCursor(
            eq(contentId),
            eq(sortBy),
            eq(sortDirection),
            any(),
            any(),
            any(),
            eq(limit)
        )).thenReturn(mockReviews);
        when(reviewRepository.countReviewsByContentId(contentId)).thenReturn(2L);
        when(reviewMapper.toDto(eq(mockReview1), eq(true))).thenReturn(mockDto1);
        when(reviewMapper.toDto(eq(mockReview2), eq(true))).thenReturn(mockDto2);

        CursorResponseReviewDto result = reviewService.getReviews(
            contentId,
            cursor,
            idAfter,
            limit,
            sortBy,
            sortDirection,
            userId
        );

        assertThat(result).isNotNull();
        assertThat(result.data()).hasSize(2);
        assertThat(result.hasNext()).isFalse();
        assertThat(result.totalCount()).isEqualTo(2);
        assertThat(result.sortBy()).isEqualTo(sortBy);
        assertThat(result.sortDirection()).isEqualTo(sortDirection);
        verify(reviewRepository).findReviewsWithCursor(eq(contentId), eq(sortBy), eq(sortDirection), any(), any(),
            any(), eq(limit));
        verify(reviewRepository).countReviewsByContentId(contentId);
    }

    @Test
    @DisplayName("리뷰 목록 조회 성공 - rating 정렬 및 커서 페이징")
    void getReviews_Success_SortByRatingWithCursor() {
        String sortBy = "rating";
        String sortDirection = "desc";
        int limit = 2;
        String cursor = "4.5";
        UUID idAfter = UUID.randomUUID();

        User mockUser = mock(User.class);
        when(mockUser.getId()).thenReturn(userId);

        UUID mockReview1Id = UUID.randomUUID();
        UUID mockReview2Id = UUID.randomUUID();
        Review mockReview1 = mock(Review.class);
        Review mockReview2 = mock(Review.class);
        Review mockReview3 = mock(Review.class);
        when(mockReview1.getUser()).thenReturn(mockUser);
        when(mockReview2.getUser()).thenReturn(mockUser);
        when(mockReview2.getId()).thenReturn(mockReview2Id);
        when(mockReview2.getRating()).thenReturn(BigDecimal.valueOf(3.5));

        List<Review> mockReviews = Arrays.asList(mockReview1, mockReview2, mockReview3);
        
        AuthorDto authorDto = AuthorDto.builder()
                                       .userId(userId)
                                       .name("Test User")
                                       .profileImageUrl(null)
                                       .build();
        
        ReviewDto mockDto1 = ReviewDto.builder()
                                      .id(mockReview1Id)
                                      .contentId(contentId)
                                      .author(authorDto)
                                      .text("Good!")
                                      .rating(4.0)
                                      .createdAt(null)
                                      .updatedAt(null)
                                      .isAuthor(true)
                                      .build();
        
        ReviewDto mockDto2 = ReviewDto.builder()
                                      .id(mockReview2Id)
                                      .contentId(contentId)
                                      .author(authorDto)
                                      .text("OK!")
                                      .rating(3.5)
                                      .createdAt(null)
                                      .updatedAt(null)
                                      .isAuthor(true)
                                      .build();

        when(reviewRepository.findReviewsWithCursor(
            eq(contentId),
            eq(sortBy),
            eq(sortDirection),
            any(),
            any(BigDecimal.class),
            eq(idAfter),
            eq(limit)
        )).thenReturn(mockReviews);
        when(reviewRepository.countReviewsByContentId(contentId)).thenReturn(5L);
        when(reviewMapper.toDto(eq(mockReview1), eq(true))).thenReturn(mockDto1);
        when(reviewMapper.toDto(eq(mockReview2), eq(true))).thenReturn(mockDto2);

        CursorResponseReviewDto result = reviewService.getReviews(
            contentId,
            cursor,
            idAfter,
            limit,
            sortBy,
            sortDirection,
            userId
        );

        assertThat(result).isNotNull();
        assertThat(result.data()).hasSize(2);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.nextCursor()).isEqualTo("3.5");
        assertThat(result.nextIdAfter()).isEqualTo(mockReview2Id);
        assertThat(result.totalCount()).isEqualTo(5);
        verify(reviewRepository).findReviewsWithCursor(eq(contentId), eq(sortBy), eq(sortDirection), any(),
            any(BigDecimal.class), eq(idAfter), eq(limit));
    }

    @Test
    @DisplayName("리뷰 목록 조회 성공 - currentUserId가 null인 경우")
    void getReviews_Success_NullCurrentUser() {
        String sortBy = "createdAt";
        String sortDirection = "desc";
        int limit = 10;

        Review mockReview = mock(Review.class);

        List<Review> mockReviews = Arrays.asList(mockReview);
        
        AuthorDto authorDto = AuthorDto.builder()
                                       .userId(UUID.randomUUID())
                                       .name("Test User")
                                       .profileImageUrl(null)
                                       .build();
        
        ReviewDto mockDto = ReviewDto.builder()
                                     .id(UUID.randomUUID())
                                     .contentId(contentId)
                                     .author(authorDto)
                                     .text("Great!")
                                     .rating(5.0)
                                     .createdAt(null)
                                     .updatedAt(null)
                                     .isAuthor(false)
                                     .build();

        when(reviewRepository.findReviewsWithCursor(
            eq(contentId),
            eq(sortBy),
            eq(sortDirection),
            any(),
            any(),
            any(),
            eq(limit)
        )).thenReturn(mockReviews);
        when(reviewRepository.countReviewsByContentId(contentId)).thenReturn(1L);
        when(reviewMapper.toDto(eq(mockReview), eq(false))).thenReturn(mockDto);

        CursorResponseReviewDto result = reviewService.getReviews(
            contentId,
            null,
            null,
            limit,
            sortBy,
            sortDirection,
            null
        );

        assertThat(result).isNotNull();
        assertThat(result.data()).hasSize(1);
        assertThat(result.data()
                         .get(0)
                         .isAuthor()).isFalse();
        verify(reviewRepository).findReviewsWithCursor(eq(contentId), eq(sortBy), eq(sortDirection), any(), any(),
            any(), eq(limit));
    }

    @Test
    @DisplayName("리뷰 생성 시 콘텐츠 평점 재계산 - 첫 리뷰")
    void recalculateContentRating_FirstReview() {
        ReviewCreateRequest request = new ReviewCreateRequest(contentId, "Great movie!", 5.0);
        
        AuthorDto authorDto = AuthorDto.builder()
                                       .userId(userId)
                                       .name("Test User")
                                       .profileImageUrl(null)
                                       .build();
        
        ReviewDto expectedDto = ReviewDto.builder()
                                         .id(reviewId)
                                         .contentId(contentId)
                                         .author(authorDto)
                                         .text("Great movie!")
                                         .rating(5.0)
                                         .createdAt(null)
                                         .updatedAt(null)
                                         .isAuthor(true)
                                         .build();

        User mockUser = mock(User.class);
        Content mockContent = mock(Content.class);
        Review mockReview = mock(Review.class);

        when(mockContent.getId()).thenReturn(contentId);
        when(mockReview.getRating()).thenReturn(BigDecimal.valueOf(5.0));
        when(mockReview.getIsDeleted()).thenReturn(false);
        when(mockReview.getContent()).thenReturn(mockContent);

        when(contentRepository.findById(contentId)).thenReturn(Optional.of(mockContent));
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(reviewRepository.existsByContentIdAndUserIdAndIsDeletedFalse(contentId, userId)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenReturn(mockReview);
        when(reviewRepository.findAll()).thenReturn(List.of(mockReview));
        when(reviewMapper.toDto(any(Review.class), eq(true))).thenReturn(expectedDto);

        reviewService.addReview(request, userId);

        verify(mockContent).updateRatingStats(BigDecimal.valueOf(5.0), 1L);
        verify(contentRepository).save(mockContent);
    }

    @Test
    @DisplayName("리뷰 생성 시 콘텐츠 평점 재계산 - 여러 리뷰 평균")
    void recalculateContentRating_MultipleReviews() {
        ReviewCreateRequest request = new ReviewCreateRequest(contentId, "Good movie", 4.0);
        
        AuthorDto authorDto = AuthorDto.builder()
                                       .userId(userId)
                                       .name("Test User")
                                       .profileImageUrl(null)
                                       .build();
        
        ReviewDto expectedDto = ReviewDto.builder()
                                         .id(reviewId)
                                         .contentId(contentId)
                                         .author(authorDto)
                                         .text("Good movie")
                                         .rating(4.0)
                                         .createdAt(null)
                                         .updatedAt(null)
                                         .isAuthor(true)
                                         .build();

        User mockUser = mock(User.class);
        Content mockContent = mock(Content.class);
        Review newReview = mock(Review.class);
        Review existingReview1 = mock(Review.class);
        Review existingReview2 = mock(Review.class);

        when(mockContent.getId()).thenReturn(contentId);
        when(newReview.getRating()).thenReturn(BigDecimal.valueOf(4.0));
        when(newReview.getIsDeleted()).thenReturn(false);
        when(newReview.getContent()).thenReturn(mockContent);

        when(existingReview1.getRating()).thenReturn(BigDecimal.valueOf(5.0));
        when(existingReview1.getIsDeleted()).thenReturn(false);
        when(existingReview1.getContent()).thenReturn(mockContent);

        when(existingReview2.getRating()).thenReturn(BigDecimal.valueOf(3.0));
        when(existingReview2.getIsDeleted()).thenReturn(false);
        when(existingReview2.getContent()).thenReturn(mockContent);

        when(contentRepository.findById(contentId)).thenReturn(Optional.of(mockContent));
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(reviewRepository.existsByContentIdAndUserIdAndIsDeletedFalse(contentId, userId)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenReturn(newReview);
        when(reviewRepository.findAll()).thenReturn(List.of(existingReview1, existingReview2, newReview));
        when(reviewMapper.toDto(any(Review.class), eq(true))).thenReturn(expectedDto);

        reviewService.addReview(request, userId);

        verify(mockContent).updateRatingStats(BigDecimal.valueOf(4.0), 3L);
        verify(contentRepository).save(mockContent);
    }

    @Test
    @DisplayName("리뷰 수정 시 콘텐츠 평점 재계산")
    void recalculateContentRating_OnUpdate() {
        ReviewUpdateRequest request = new ReviewUpdateRequest("Updated text", 3.0);
        
        AuthorDto authorDto = AuthorDto.builder()
                                       .userId(userId)
                                       .name("Test User")
                                       .profileImageUrl(null)
                                       .build();
        
        ReviewDto expectedDto = ReviewDto.builder()
                                         .id(reviewId)
                                         .contentId(contentId)
                                         .author(authorDto)
                                         .text("Updated text")
                                         .rating(3.0)
                                         .createdAt(null)
                                         .updatedAt(null)
                                         .isAuthor(true)
                                         .build();

        User mockUser = mock(User.class);
        Content mockContent = mock(Content.class);
        Review mockReview = mock(Review.class);
        Review existingReview = mock(Review.class);

        when(mockUser.getId()).thenReturn(userId);
        when(mockReview.getUser()).thenReturn(mockUser);
        when(mockReview.getContent()).thenReturn(mockContent);
        when(mockReview.getRating()).thenReturn(BigDecimal.valueOf(3.0));
        when(mockReview.getIsDeleted()).thenReturn(false);

        when(mockContent.getId()).thenReturn(contentId);
        when(existingReview.getRating()).thenReturn(BigDecimal.valueOf(4.0));
        when(existingReview.getIsDeleted()).thenReturn(false);
        when(existingReview.getContent()).thenReturn(mockContent);

        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(mockReview));
        when(reviewRepository.save(mockReview)).thenReturn(mockReview);
        when(reviewRepository.findAll()).thenReturn(List.of(mockReview, existingReview));
        when(contentRepository.findById(contentId)).thenReturn(Optional.of(mockContent));
        when(reviewMapper.toDto(mockReview, true)).thenReturn(expectedDto);

        reviewService.modifyReview(reviewId, request, userId);

        verify(mockReview).update("Updated text", BigDecimal.valueOf(3.0));
        verify(mockContent).updateRatingStats(BigDecimal.valueOf(3.5), 2L);
        verify(contentRepository).save(mockContent);
    }

    @Test
    @DisplayName("리뷰 삭제 시 콘텐츠 평점 재계산 - 남은 리뷰 있음")
    void recalculateContentRating_OnDelete_RemainingReviews() {
        User mockUser = mock(User.class);
        Content mockContent = mock(Content.class);
        Review mockReview = mock(Review.class);
        Review remainingReview = mock(Review.class);

        when(mockUser.getId()).thenReturn(userId);
        when(mockReview.getUser()).thenReturn(mockUser);
        when(mockReview.getContent()).thenReturn(mockContent);
        when(mockReview.getIsDeleted()).thenReturn(true);

        when(mockContent.getId()).thenReturn(contentId);
        when(remainingReview.getRating()).thenReturn(BigDecimal.valueOf(4.0));
        when(remainingReview.getIsDeleted()).thenReturn(false);
        when(remainingReview.getContent()).thenReturn(mockContent);

        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(mockReview));
        when(reviewRepository.save(mockReview)).thenReturn(mockReview);
        when(reviewRepository.findAll()).thenReturn(List.of(mockReview, remainingReview));
        when(contentRepository.findById(contentId)).thenReturn(Optional.of(mockContent));

        reviewService.removeReview(reviewId, userId);

        verify(mockReview).softDelete();
        verify(mockContent).updateRatingStats(BigDecimal.valueOf(4.0), 1L);
        verify(contentRepository).save(mockContent);
    }

    @Test
    @DisplayName("리뷰 삭제 시 콘텐츠 평점 재계산 - 마지막 리뷰 삭제")
    void recalculateContentRating_OnDelete_LastReview() {
        User mockUser = mock(User.class);
        Content mockContent = mock(Content.class);
        Review mockReview = mock(Review.class);

        when(mockUser.getId()).thenReturn(userId);
        when(mockReview.getUser()).thenReturn(mockUser);
        when(mockReview.getContent()).thenReturn(mockContent);
        when(mockReview.getIsDeleted()).thenReturn(true);

        when(mockContent.getId()).thenReturn(contentId);

        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(mockReview));
        when(reviewRepository.save(mockReview)).thenReturn(mockReview);
        when(reviewRepository.findAll()).thenReturn(List.of(mockReview));
        when(contentRepository.findById(contentId)).thenReturn(Optional.of(mockContent));

        reviewService.removeReview(reviewId, userId);

        verify(mockReview).softDelete();
        verify(mockContent).updateRatingStats(BigDecimal.ZERO, 0L);
        verify(contentRepository).save(mockContent);
    }
}
