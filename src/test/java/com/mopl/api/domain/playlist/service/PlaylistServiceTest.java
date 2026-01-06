package com.mopl.api.domain.playlist.service;

import com.mopl.api.domain.content.repository.ContentRepository;
import com.mopl.api.domain.playlist.dto.request.PlaylistCreateRequest;
import com.mopl.api.domain.playlist.dto.request.PlaylistUpdateRequest;
import com.mopl.api.domain.playlist.dto.response.PlaylistDto;
import com.mopl.api.domain.playlist.entity.Playlist;
import com.mopl.api.domain.playlist.exception.detail.PlaylistNotFoundException;
import com.mopl.api.domain.playlist.exception.detail.PlaylistUnauthorizedException;
import com.mopl.api.domain.playlist.exception.detail.UserNotFoundException;
import com.mopl.api.domain.playlist.mapper.PlaylistMapper;
import com.mopl.api.domain.playlist.repository.PlaylistContentRepository;
import com.mopl.api.domain.playlist.repository.PlaylistRepository;
import com.mopl.api.domain.playlist.repository.SubscriptionRepository;
import com.mopl.api.domain.user.entity.User;
import com.mopl.api.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlaylistService 단위 테스트")
class PlaylistServiceTest {

    @Mock
    private PlaylistRepository playlistRepository;

    @Mock
    private PlaylistContentRepository playlistContentRepository;

    @Mock
    private ContentRepository contentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private PlaylistMapper playlistMapper;

    @InjectMocks
    private PlaylistServiceImpl playlistService;

    private UUID userId;
    private UUID playlistId;
    private UUID contentId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        playlistId = UUID.randomUUID();
        contentId = UUID.randomUUID();
    }

    @Test
    @DisplayName("플레이리스트 생성 성공")
    void addPlaylist_Success() throws Exception {
        PlaylistCreateRequest request = new PlaylistCreateRequest("My Playlist", "Great movies");
        User mockUser = mock(User.class);
        PlaylistDto expectedDto = mock(PlaylistDto.class);

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(playlistRepository.save(any(Playlist.class))).thenAnswer(invocation -> {
            Playlist savedPlaylist = invocation.getArgument(0);
            Field idField = savedPlaylist.getClass()
                                         .getSuperclass()
                                         .getSuperclass()
                                         .getSuperclass()
                                         .getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(savedPlaylist, playlistId);
            return savedPlaylist;
        });
        when(playlistContentRepository.findByPlaylistIdAndIsDeletedFalse(playlistId)).thenReturn(new ArrayList<>());
        when(playlistMapper.toDto(any(Playlist.class), anyList(), anyBoolean(), anyBoolean())).thenReturn(expectedDto);

        PlaylistDto result = playlistService.addPlaylist(request, userId);

        assertThat(result).isNotNull();
        verify(userRepository).findById(userId);
        verify(playlistRepository).save(any(Playlist.class));
        verify(playlistContentRepository).findByPlaylistIdAndIsDeletedFalse(playlistId);
        verify(playlistMapper).toDto(any(Playlist.class), anyList(), eq(false), eq(true));
    }

    @Test
    @DisplayName("플레이리스트 생성 실패 - 사용자 없음")
    void addPlaylist_UserNotFound() {
        PlaylistCreateRequest request = new PlaylistCreateRequest("My Playlist", "Great movies");

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> playlistService.addPlaylist(request, userId))
            .isInstanceOf(UserNotFoundException.class);

        verify(userRepository).findById(userId);
        verify(playlistRepository, never()).save(any());
    }

    @Test
    @DisplayName("플레이리스트 수정 성공")
    void modifyPlaylist_Success() {
        PlaylistUpdateRequest request = new PlaylistUpdateRequest("Updated Title", "Updated Description");
        User mockUser = mock(User.class);
        Playlist mockPlaylist = mock(Playlist.class);
        PlaylistDto expectedDto = mock(PlaylistDto.class);

        when(mockUser.getId()).thenReturn(userId);
        when(mockPlaylist.getOwner()).thenReturn(mockUser);
        when(playlistRepository.findById(playlistId)).thenReturn(Optional.of(mockPlaylist));
        when(playlistRepository.save(mockPlaylist)).thenReturn(mockPlaylist);
        when(playlistContentRepository.findByPlaylistIdAndIsDeletedFalse(playlistId)).thenReturn(new ArrayList<>());
        when(playlistMapper.toDto(any(Playlist.class), anyList(), anyBoolean(), anyBoolean())).thenReturn(expectedDto);

        PlaylistDto result = playlistService.modifyPlaylist(playlistId, request, userId);

        assertThat(result).isNotNull();
        verify(playlistRepository).findById(playlistId);
        verify(mockPlaylist).update("Updated Title", "Updated Description");
        verify(playlistRepository).save(mockPlaylist);
    }

    @Test
    @DisplayName("플레이리스트 수정 실패 - 플레이리스트 없음")
    void modifyPlaylist_PlaylistNotFound() {
        PlaylistUpdateRequest request = new PlaylistUpdateRequest("Updated Title", "Updated Description");

        when(playlistRepository.findById(playlistId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> playlistService.modifyPlaylist(playlistId, request, userId))
            .isInstanceOf(PlaylistNotFoundException.class);

        verify(playlistRepository).findById(playlistId);
        verify(playlistRepository, never()).save(any());
    }

    @Test
    @DisplayName("플레이리스트 수정 실패 - 권한 없음")
    void modifyPlaylist_Unauthorized() {
        UUID differentUserId = UUID.randomUUID();
        PlaylistUpdateRequest request = new PlaylistUpdateRequest("Updated Title", "Updated Description");
        User mockUser = mock(User.class);
        Playlist mockPlaylist = mock(Playlist.class);

        when(mockUser.getId()).thenReturn(userId);
        when(mockPlaylist.getOwner()).thenReturn(mockUser);
        when(playlistRepository.findById(playlistId)).thenReturn(Optional.of(mockPlaylist));

        assertThatThrownBy(() -> playlistService.modifyPlaylist(playlistId, request, differentUserId))
            .isInstanceOf(PlaylistUnauthorizedException.class);

        verify(playlistRepository).findById(playlistId);
        verify(playlistRepository, never()).save(any());
    }

    @Test
    @DisplayName("플레이리스트 삭제 성공")
    void removePlaylist_Success() {
        User mockUser = mock(User.class);
        Playlist mockPlaylist = mock(Playlist.class);

        when(mockUser.getId()).thenReturn(userId);
        when(mockPlaylist.getOwner()).thenReturn(mockUser);
        when(playlistRepository.findById(playlistId)).thenReturn(Optional.of(mockPlaylist));
        when(playlistRepository.save(mockPlaylist)).thenReturn(mockPlaylist);

        playlistService.removePlaylist(playlistId, userId);

        verify(playlistRepository).findById(playlistId);
        verify(mockPlaylist).softDelete();
        verify(playlistRepository).save(mockPlaylist);
    }

    @Test
    @DisplayName("플레이리스트 삭제 실패 - 플레이리스트 없음")
    void removePlaylist_PlaylistNotFound() {
        when(playlistRepository.findById(playlistId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> playlistService.removePlaylist(playlistId, userId))
            .isInstanceOf(PlaylistNotFoundException.class);

        verify(playlistRepository).findById(playlistId);
        verify(playlistRepository, never()).save(any());
    }

    @Test
    @DisplayName("플레이리스트 삭제 실패 - 권한 없음")
    void removePlaylist_Unauthorized() {
        UUID differentUserId = UUID.randomUUID();
        User mockUser = mock(User.class);
        Playlist mockPlaylist = mock(Playlist.class);

        when(mockUser.getId()).thenReturn(userId);
        when(mockPlaylist.getOwner()).thenReturn(mockUser);
        when(playlistRepository.findById(playlistId)).thenReturn(Optional.of(mockPlaylist));

        assertThatThrownBy(() -> playlistService.removePlaylist(playlistId, differentUserId))
            .isInstanceOf(PlaylistUnauthorizedException.class);

        verify(playlistRepository).findById(playlistId);
        verify(mockPlaylist, never()).softDelete();
        verify(playlistRepository, never()).save(any());
    }

    @Test
    @DisplayName("플레이리스트 조회 성공")
    void getPlaylist_Success() {
        User mockUser = mock(User.class);
        Playlist mockPlaylist = mock(Playlist.class);
        PlaylistDto expectedDto = mock(PlaylistDto.class);

        when(mockUser.getId()).thenReturn(userId);
        when(mockPlaylist.getOwner()).thenReturn(mockUser);
        when(mockPlaylist.getIsDeleted()).thenReturn(false);
        when(playlistRepository.findById(playlistId)).thenReturn(Optional.of(mockPlaylist));
        when(playlistContentRepository.findByPlaylistIdAndIsDeletedFalse(playlistId)).thenReturn(new ArrayList<>());
        when(subscriptionRepository.existsByUserIdAndPlaylistId(userId, playlistId)).thenReturn(false);
        when(playlistMapper.toDto(any(Playlist.class), anyList(), anyBoolean(), anyBoolean())).thenReturn(expectedDto);

        PlaylistDto result = playlistService.getPlaylist(playlistId, userId);

        assertThat(result).isNotNull();
        verify(playlistRepository).findById(playlistId);
        verify(playlistContentRepository).findByPlaylistIdAndIsDeletedFalse(playlistId);
        verify(subscriptionRepository).existsByUserIdAndPlaylistId(userId, playlistId);
    }

    @Test
    @DisplayName("플레이리스트 조회 실패 - 플레이리스트 없음")
    void getPlaylist_PlaylistNotFound() {
        when(playlistRepository.findById(playlistId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> playlistService.getPlaylist(playlistId, userId))
            .isInstanceOf(PlaylistNotFoundException.class);

        verify(playlistRepository).findById(playlistId);
    }

    @Test
    @DisplayName("플레이리스트 조회 실패 - 삭제된 플레이리스트")
    void getPlaylist_DeletedPlaylist() {
        Playlist mockPlaylist = mock(Playlist.class);

        when(mockPlaylist.getIsDeleted()).thenReturn(true);
        when(playlistRepository.findById(playlistId)).thenReturn(Optional.of(mockPlaylist));

        assertThatThrownBy(() -> playlistService.getPlaylist(playlistId, userId))
            .isInstanceOf(PlaylistNotFoundException.class);

        verify(playlistRepository).findById(playlistId);
    }






}
