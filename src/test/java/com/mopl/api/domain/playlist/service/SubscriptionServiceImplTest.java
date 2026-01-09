package com.mopl.api.domain.playlist.service;

import com.mopl.api.domain.playlist.entity.Playlist;
import com.mopl.api.domain.playlist.entity.Subscription;
import com.mopl.api.domain.playlist.exception.detail.DuplicateSubscriptionException;
import com.mopl.api.domain.playlist.exception.detail.PlaylistNotFoundException;
import com.mopl.api.domain.playlist.exception.detail.SelfSubscriptionException;
import com.mopl.api.domain.playlist.exception.detail.SubscriptionNotFoundException;
import com.mopl.api.domain.playlist.repository.PlaylistRepository;
import com.mopl.api.domain.playlist.repository.SubscriptionRepository;
import com.mopl.api.domain.user.entity.User;
import com.mopl.api.domain.user.exception.user.detail.UserNotFoundException;
import com.mopl.api.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubscriptionService 단위 테스트")
class SubscriptionServiceImplTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private PlaylistRepository playlistRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SubscriptionServiceImpl subscriptionService;

    private UUID userId;
    private UUID ownerId;
    private UUID playlistId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        ownerId = UUID.randomUUID();
        playlistId = UUID.randomUUID();
    }

    @Test
    @DisplayName("플레이리스트 구독 성공")
    void subscribeToPlaylist_Success() {
        User mockUser = mock(User.class);
        User mockOwner = mock(User.class);
        Playlist mockPlaylist = mock(Playlist.class);

        when(mockOwner.getId()).thenReturn(ownerId);
        when(mockPlaylist.getOwner()).thenReturn(mockOwner);
        when(playlistRepository.findById(playlistId)).thenReturn(Optional.of(mockPlaylist));
        when(subscriptionRepository.existsByUserIdAndPlaylistId(userId, playlistId)).thenReturn(false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(mock(Subscription.class));
        when(playlistRepository.save(mockPlaylist)).thenReturn(mockPlaylist);

        subscriptionService.subscribeToPlaylist(playlistId, userId);

        verify(playlistRepository).findById(playlistId);
        verify(subscriptionRepository).existsByUserIdAndPlaylistId(userId, playlistId);
        verify(userRepository).findById(userId);
        verify(subscriptionRepository).save(any(Subscription.class));
        verify(mockPlaylist).incrementSubscriberCount();
        verify(playlistRepository).save(mockPlaylist);
    }

    @Test
    @DisplayName("플레이리스트 구독 실패 - 플레이리스트 없음")
    void subscribeToPlaylist_PlaylistNotFound() {
        when(playlistRepository.findById(playlistId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subscriptionService.subscribeToPlaylist(playlistId, userId))
            .isInstanceOf(PlaylistNotFoundException.class)
            .hasMessageContaining("존재하지 않는 플레이리스트입니다");

        verify(playlistRepository).findById(playlistId);
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    @DisplayName("플레이리스트 구독 실패 - 자기 자신의 플레이리스트")
    void subscribeToPlaylist_SelfSubscription() {
        User mockOwner = mock(User.class);
        Playlist mockPlaylist = mock(Playlist.class);

        when(mockOwner.getId()).thenReturn(userId);
        when(mockPlaylist.getOwner()).thenReturn(mockOwner);
        when(playlistRepository.findById(playlistId)).thenReturn(Optional.of(mockPlaylist));

        assertThatThrownBy(() -> subscriptionService.subscribeToPlaylist(playlistId, userId))
            .isInstanceOf(SelfSubscriptionException.class)
            .hasMessageContaining("자신의 플레이리스트는 구독할 수 없습니다");

        verify(playlistRepository).findById(playlistId);
        verify(subscriptionRepository, never()).existsByUserIdAndPlaylistId(any(), any());
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    @DisplayName("플레이리스트 구독 실패 - 이미 구독한 플레이리스트")
    void subscribeToPlaylist_DuplicateSubscription() {
        User mockOwner = mock(User.class);
        Playlist mockPlaylist = mock(Playlist.class);

        when(mockOwner.getId()).thenReturn(ownerId);
        when(mockPlaylist.getOwner()).thenReturn(mockOwner);
        when(playlistRepository.findById(playlistId)).thenReturn(Optional.of(mockPlaylist));
        when(subscriptionRepository.existsByUserIdAndPlaylistId(userId, playlistId)).thenReturn(true);

        assertThatThrownBy(() -> subscriptionService.subscribeToPlaylist(playlistId, userId))
            .isInstanceOf(DuplicateSubscriptionException.class)
            .hasMessageContaining("이미 구독한 플레이리스트입니다");

        verify(playlistRepository).findById(playlistId);
        verify(subscriptionRepository).existsByUserIdAndPlaylistId(userId, playlistId);
        verify(userRepository, never()).findById(any());
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    @DisplayName("플레이리스트 구독 실패 - 사용자 없음")
    void subscribeToPlaylist_UserNotFound() {
        User mockOwner = mock(User.class);
        Playlist mockPlaylist = mock(Playlist.class);

        when(mockOwner.getId()).thenReturn(ownerId);
        when(mockPlaylist.getOwner()).thenReturn(mockOwner);
        when(playlistRepository.findById(playlistId)).thenReturn(Optional.of(mockPlaylist));
        when(subscriptionRepository.existsByUserIdAndPlaylistId(userId, playlistId)).thenReturn(false);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subscriptionService.subscribeToPlaylist(playlistId, userId))
            .isInstanceOf(UserNotFoundException.class)
            .hasMessageContaining("존재하지 않는 사용자입니다");

        verify(playlistRepository).findById(playlistId);
        verify(subscriptionRepository).existsByUserIdAndPlaylistId(userId, playlistId);
        verify(userRepository).findById(userId);
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    @DisplayName("플레이리스트 구독 취소 성공")
    void unsubscribeFromPlaylist_Success() {
        Playlist mockPlaylist = mock(Playlist.class);
        Subscription mockSubscription = mock(Subscription.class);

        when(playlistRepository.findById(playlistId)).thenReturn(Optional.of(mockPlaylist));
        when(subscriptionRepository.findByUserIdAndPlaylistId(userId, playlistId)).thenReturn(Optional.of(mockSubscription));
        when(playlistRepository.save(mockPlaylist)).thenReturn(mockPlaylist);

        subscriptionService.unsubscribeFromPlaylist(playlistId, userId);

        verify(playlistRepository).findById(playlistId);
        verify(subscriptionRepository).findByUserIdAndPlaylistId(userId, playlistId);
        verify(subscriptionRepository).delete(mockSubscription);
        verify(mockPlaylist).decrementSubscriberCount();
        verify(playlistRepository).save(mockPlaylist);
    }

    @Test
    @DisplayName("플레이리스트 구독 취소 실패 - 플레이리스트 없음")
    void unsubscribeFromPlaylist_PlaylistNotFound() {
        when(playlistRepository.findById(playlistId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subscriptionService.unsubscribeFromPlaylist(playlistId, userId))
            .isInstanceOf(PlaylistNotFoundException.class)
            .hasMessageContaining("존재하지 않는 플레이리스트입니다");

        verify(playlistRepository).findById(playlistId);
        verify(subscriptionRepository, never()).delete(any());
    }

    @Test
    @DisplayName("플레이리스트 구독 취소 실패 - 구독하지 않은 플레이리스트")
    void unsubscribeFromPlaylist_SubscriptionNotFound() {
        Playlist mockPlaylist = mock(Playlist.class);

        when(playlistRepository.findById(playlistId)).thenReturn(Optional.of(mockPlaylist));
        when(subscriptionRepository.findByUserIdAndPlaylistId(userId, playlistId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subscriptionService.unsubscribeFromPlaylist(playlistId, userId))
            .isInstanceOf(SubscriptionNotFoundException.class)
            .hasMessageContaining("구독 정보를 찾을 수 없습니다");

        verify(playlistRepository).findById(playlistId);
        verify(subscriptionRepository).findByUserIdAndPlaylistId(userId, playlistId);
        verify(subscriptionRepository, never()).delete(any());
    }

    @Test
    @DisplayName("구독 여부 확인 - 구독함")
    void isUserSubscribed_True() {
        when(subscriptionRepository.existsByUserIdAndPlaylistId(userId, playlistId)).thenReturn(true);

        boolean result = subscriptionService.isUserSubscribed(userId, playlistId);

        assertThat(result).isTrue();
        verify(subscriptionRepository).existsByUserIdAndPlaylistId(userId, playlistId);
    }

    @Test
    @DisplayName("구독 여부 확인 - 구독 안 함")
    void isUserSubscribed_False() {
        when(subscriptionRepository.existsByUserIdAndPlaylistId(userId, playlistId)).thenReturn(false);

        boolean result = subscriptionService.isUserSubscribed(userId, playlistId);

        assertThat(result).isFalse();
        verify(subscriptionRepository).existsByUserIdAndPlaylistId(userId, playlistId);
    }
}
