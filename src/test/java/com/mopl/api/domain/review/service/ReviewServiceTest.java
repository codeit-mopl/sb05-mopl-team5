package com.mopl.api.domain.review.service;

import com.mopl.api.domain.content.entity.Content;
import com.mopl.api.domain.content.repository.ContentRepository;
import com.mopl.api.domain.review.dto.request.ReviewCreateRequest;
import com.mopl.api.domain.review.dto.request.ReviewUpdateRequest;
import com.mopl.api.domain.review.dto.response.ReviewDto;
import com.mopl.api.domain.review.entity.Review;
import com.mopl.api.domain.content.exception.detail.ContentNotFoundException;
import com.mopl.api.domain.review.exception.detail.ReviewAlreadyExistsException;
import com.mopl.api.domain.review.exception.detail.ReviewNotFoundException;
import com.mopl.api.domain.review.exception.detail.ReviewUnauthorizedException;
import com.mopl.api.domain.user.exception.detail.UserNotFoundException;
import com.mopl.api.domain.review.mapper.ReviewMapper;
import com.mopl.api.domain.review.repository.ReviewRepository;
import com.mopl.api.domain.user.dto.response.UserDto;
import com.mopl.api.domain.user.entity.User;
import com.mopl.api.domain.user.entity.UserRole;
import com.mopl.api.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

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
    private UserDto mockUserDto;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        contentId = UUID.randomUUID();
        reviewId = UUID.randomUUID();
        mockUserDto = UserDto.builder()
                             .id(userId)
                             .createdAt(LocalDateTime.now())
                             .email("test@example.com")
                             .name("Test User")
                             .profileImageUrl(null)
                             .role(UserRole.USER)
                             .locked(false)
                             .build();
    }

    @Test
    @DisplayName("리뷰 생성 성공")
    void addReview_Success() {
        ReviewCreateRequest request = new ReviewCreateRequest(contentId, "Great movie!", 5.0);
        ReviewDto expectedDto = new ReviewDto(reviewId, contentId, mockUserDto, "Great movie!", 5.0, null, null, true);

        User mockUser = mock(User.class);
        Content mockContent = mock(Content.class);
        Review mockReview = mock(Review.class);

        when(mockContent.getId()).thenReturn(contentId);

        when(contentRepository.findById(contentId)).thenReturn(Optional.of(mockContent));
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(reviewRepository.existsByContentIdAndUserIdAndIsDeletedFalse(contentId, userId)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenReturn(mockReview);
        when(reviewMapper.toDto(any(Review.class), eq(true))).thenReturn(expectedDto);

        ReviewDto result = reviewService.addReview(request, userId);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(reviewId);
        verify(contentRepository).findById(contentId);
        verify(userRepository).findById(userId);
        verify(reviewRepository).existsByContentIdAndUserIdAndIsDeletedFalse(contentId, userId);
        verify(reviewRepository).save(any(Review.class));
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
            .hasMessageContaining("존재하지 않는 사용자입니다");

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
        ReviewDto expectedDto = new ReviewDto(reviewId, contentId, mockUserDto, "Updated text", 4.0, null, null, true);

        User mockUser = mock(User.class);
        Review mockReview = mock(Review.class);

        when(mockUser.getId()).thenReturn(userId);
        when(mockReview.getUser()).thenReturn(mockUser);

        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(mockReview));
        when(reviewRepository.save(mockReview)).thenReturn(mockReview);
        when(reviewMapper.toDto(mockReview, true)).thenReturn(expectedDto);

        ReviewDto result = reviewService.modifyReview(reviewId, request, userId);

        assertThat(result).isNotNull();
        assertThat(result.text()).isEqualTo("Updated text");
        verify(reviewRepository).findById(reviewId);
        verify(mockReview).update("Updated text", BigDecimal.valueOf(4.0));
        verify(reviewRepository).save(mockReview);
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
        Review mockReview = mock(Review.class);

        when(mockUser.getId()).thenReturn(userId);
        when(mockReview.getUser()).thenReturn(mockUser);

        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(mockReview));
        when(reviewRepository.save(mockReview)).thenReturn(mockReview);

        reviewService.removeReview(reviewId, userId);

        verify(reviewRepository).findById(reviewId);
        verify(mockReview).softDelete();
        verify(reviewRepository).save(mockReview);
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
}
