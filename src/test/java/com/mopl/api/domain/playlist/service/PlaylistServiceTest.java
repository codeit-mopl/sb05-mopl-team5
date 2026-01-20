package com.mopl.api.domain.playlist.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mopl.api.domain.content.entity.Content;
import com.mopl.api.domain.content.entity.ContentType;
import com.mopl.api.domain.content.exception.detail.ContentNotFoundException;
import com.mopl.api.domain.content.repository.ContentRepository;
import com.mopl.api.domain.playlist.dto.request.PlaylistCreateRequest;
import com.mopl.api.domain.playlist.dto.request.PlaylistUpdateRequest;
import com.mopl.api.domain.playlist.dto.response.CursorResponsePlaylistDto;
import com.mopl.api.domain.playlist.dto.response.PlaylistDto;
import com.mopl.api.domain.playlist.entity.Playlist;
import com.mopl.api.domain.playlist.entity.PlaylistContent;
import com.mopl.api.domain.playlist.exception.detail.ContentAlreadyExistsException;
import com.mopl.api.domain.playlist.exception.detail.ContentNotInPlaylistException;
import com.mopl.api.domain.playlist.exception.detail.PlaylistNotFoundException;
import com.mopl.api.domain.playlist.exception.detail.PlaylistUnauthorizedException;
import com.mopl.api.domain.playlist.mapper.PlaylistMapper;
import com.mopl.api.domain.playlist.repository.PlaylistContentRepository;
import com.mopl.api.domain.playlist.repository.PlaylistRepository;
import com.mopl.api.domain.playlist.repository.SubscriptionRepository;
import com.mopl.api.domain.user.entity.User;
import com.mopl.api.domain.user.entity.UserRole;
import com.mopl.api.domain.user.exception.user.detail.UserNotFoundException;
import com.mopl.api.domain.user.repository.UserRepository;
import com.navercorp.fixturemonkey.FixtureMonkey;
import com.navercorp.fixturemonkey.api.introspector.FieldReflectionArbitraryIntrospector;
import java.util.ArrayList;
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
import org.springframework.context.ApplicationEventPublisher;

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

    @Mock
    ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private PlaylistService playlistService;

    private FixtureMonkey fixtureMonkey;
    private UUID userId;
    private UUID playlistId;
    private UUID contentId;

    @BeforeEach
    void setUp() {
        fixtureMonkey = FixtureMonkey.builder()
            .objectIntrospector(FieldReflectionArbitraryIntrospector.INSTANCE)
            .defaultNotNull(true)
            .build();
        
        userId = UUID.randomUUID();
        playlistId = UUID.randomUUID();
        contentId = UUID.randomUUID();
    }

    @Test
    @DisplayName("플레이리스트 생성 성공")
    void addPlaylist_Success() {
        PlaylistCreateRequest request = new PlaylistCreateRequest("My Playlist", "Great movies");
        
        User user = fixtureMonkey.giveMeBuilder(User.class)
            .set("id", userId)
            .set("role", UserRole.USER)
            .set("locked", false)
            .sample();
        
        Playlist playlist = fixtureMonkey.giveMeBuilder(Playlist.class)
            .set("owner", user)
            .set("title", "My Playlist")
            .set("description", "Great movies")
            .set("isDeleted", false)
            .sample();
        
        PlaylistDto expectedDto = mock(PlaylistDto.class);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(playlistRepository.save(any(Playlist.class))).thenReturn(playlist);
        when(playlistContentRepository.findByPlaylistIdAndIsDeletedFalse(any())).thenReturn(new ArrayList<>());
        when(playlistMapper.toDto(any(Playlist.class), anyList(), anyBoolean(), anyBoolean())).thenReturn(expectedDto);

        PlaylistDto result = playlistService.addPlaylist(request, userId);

        assertThat(result).isNotNull();
        verify(userRepository).findById(userId);
        verify(playlistRepository).save(any(Playlist.class));
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
        
        User user = fixtureMonkey.giveMeBuilder(User.class)
            .set("id", userId)
            .set("role", UserRole.USER)
            .sample();
        
        Playlist playlist = fixtureMonkey.giveMeBuilder(Playlist.class)
            .set("id", playlistId)
            .set("owner", user)
            .set("isDeleted", false)
            .sample();
        
        PlaylistDto expectedDto = mock(PlaylistDto.class);

        when(playlistRepository.findById(playlistId)).thenReturn(Optional.of(playlist));
        when(playlistRepository.save(playlist)).thenReturn(playlist);
        when(playlistContentRepository.findByPlaylistIdAndIsDeletedFalse(any())).thenReturn(new ArrayList<>());
        when(playlistMapper.toDto(any(Playlist.class), anyList(), anyBoolean(), anyBoolean())).thenReturn(expectedDto);

        PlaylistDto result = playlistService.modifyPlaylist(playlistId, request, userId);

        assertThat(result).isNotNull();
        verify(playlistRepository).findById(playlistId);
        verify(playlistRepository).save(playlist);
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
        
        User user = fixtureMonkey.giveMeBuilder(User.class)
            .set("id", userId)
            .set("role", UserRole.USER)
            .sample();
        
        Playlist playlist = fixtureMonkey.giveMeBuilder(Playlist.class)
            .set("id", playlistId)
            .set("owner", user)
            .sample();

        when(playlistRepository.findById(playlistId)).thenReturn(Optional.of(playlist));

        assertThatThrownBy(() -> playlistService.modifyPlaylist(playlistId, request, differentUserId))
            .isInstanceOf(PlaylistUnauthorizedException.class);

        verify(playlistRepository).findById(playlistId);
        verify(playlistRepository, never()).save(any());
    }

    @Test
    @DisplayName("플레이리스트 삭제 성공")
    void removePlaylist_Success() {
        User user = fixtureMonkey.giveMeBuilder(User.class)
            .set("id", userId)
            .set("role", UserRole.USER)
            .sample();
        
        Playlist playlist = fixtureMonkey.giveMeBuilder(Playlist.class)
            .set("id", playlistId)
            .set("owner", user)
            .set("isDeleted", false)
            .sample();

        when(playlistRepository.findById(playlistId)).thenReturn(Optional.of(playlist));
        when(playlistRepository.save(playlist)).thenReturn(playlist);

        playlistService.removePlaylist(playlistId, userId);

        verify(playlistRepository).findById(playlistId);
        verify(playlistRepository).save(playlist);
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
        
        User user = fixtureMonkey.giveMeBuilder(User.class)
            .set("id", userId)
            .set("role", UserRole.USER)
            .sample();
        
        Playlist playlist = fixtureMonkey.giveMeBuilder(Playlist.class)
            .set("id", playlistId)
            .set("owner", user)
            .sample();

        when(playlistRepository.findById(playlistId)).thenReturn(Optional.of(playlist));

        assertThatThrownBy(() -> playlistService.removePlaylist(playlistId, differentUserId))
            .isInstanceOf(PlaylistUnauthorizedException.class);

        verify(playlistRepository).findById(playlistId);
        verify(playlistRepository, never()).save(any());
    }

    @Test
    @DisplayName("플레이리스트 조회 성공")
    void getPlaylist_Success() {
        User user = fixtureMonkey.giveMeBuilder(User.class)
            .set("id", userId)
            .set("role", UserRole.USER)
            .sample();
        
        Playlist playlist = fixtureMonkey.giveMeBuilder(Playlist.class)
            .set("id", playlistId)
            .set("owner", user)
            .set("isDeleted", false)
            .sample();
        
        PlaylistDto expectedDto = mock(PlaylistDto.class);

        when(playlistRepository.findById(playlistId)).thenReturn(Optional.of(playlist));
        when(playlistContentRepository.findByPlaylistIdAndIsDeletedFalse(any())).thenReturn(new ArrayList<>());
        when(subscriptionRepository.existsByUserIdAndPlaylistId(userId, playlistId)).thenReturn(false);
        when(playlistMapper.toDto(any(Playlist.class), anyList(), anyBoolean(), anyBoolean())).thenReturn(expectedDto);

        PlaylistDto result = playlistService.getPlaylist(playlistId, userId);

        assertThat(result).isNotNull();
        verify(playlistRepository).findById(playlistId);
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
        Playlist playlist = fixtureMonkey.giveMeBuilder(Playlist.class)
            .set("id", playlistId)
            .set("isDeleted", true)
            .sample();

        when(playlistRepository.findById(playlistId)).thenReturn(Optional.of(playlist));

        assertThatThrownBy(() -> playlistService.getPlaylist(playlistId, userId))
            .isInstanceOf(PlaylistNotFoundException.class);

        verify(playlistRepository).findById(playlistId);
    }

    @Test
    @DisplayName("플레이리스트에 콘텐츠 추가 성공")
    void addContentToPlaylist_Success() {
        User user = fixtureMonkey.giveMeBuilder(User.class)
            .set("id", userId)
            .set("role", UserRole.USER)
            .sample();
        
        Playlist playlist = fixtureMonkey.giveMeBuilder(Playlist.class)
            .set("id", playlistId)
            .set("owner", user)
            .sample();
        
        Content content = fixtureMonkey.giveMeBuilder(Content.class)
            .set("id", contentId)
            .set("type", ContentType.MOVIE)
            .set("isDeleted", false)
            .sample();

        when(playlistRepository.findById(playlistId)).thenReturn(Optional.of(playlist));
        when(contentRepository.findById(contentId)).thenReturn(Optional.of(content));
        when(playlistContentRepository.existsByPlaylistIdAndContentIdAndIsDeletedFalse(playlistId,
            contentId)).thenReturn(false);

        playlistService.addContentToPlaylist(playlistId, contentId, userId);

        verify(playlistRepository).findById(playlistId);
        verify(contentRepository).findById(contentId);
        verify(playlistContentRepository).existsByPlaylistIdAndContentIdAndIsDeletedFalse(playlistId, contentId);
        verify(playlistContentRepository).save(any(PlaylistContent.class));
    }

    @Test
    @DisplayName("플레이리스트에 콘텐츠 추가 실패 - 플레이리스트 없음")
    void addContentToPlaylist_PlaylistNotFound() {
        when(playlistRepository.findById(playlistId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> playlistService.addContentToPlaylist(playlistId, contentId, userId))
            .isInstanceOf(PlaylistNotFoundException.class);

        verify(playlistRepository).findById(playlistId);
        verify(contentRepository, never()).findById(any());
        verify(playlistContentRepository, never()).save(any());
    }

    @Test
    @DisplayName("플레이리스트에 콘텐츠 추가 실패 - 권한 없음")
    void addContentToPlaylist_Unauthorized() {
        UUID differentUserId = UUID.randomUUID();
        
        User user = fixtureMonkey.giveMeBuilder(User.class)
            .set("id", userId)
            .set("role", UserRole.USER)
            .sample();
        
        Playlist playlist = fixtureMonkey.giveMeBuilder(Playlist.class)
            .set("id", playlistId)
            .set("owner", user)
            .sample();

        when(playlistRepository.findById(playlistId)).thenReturn(Optional.of(playlist));

        assertThatThrownBy(() -> playlistService.addContentToPlaylist(playlistId, contentId, differentUserId))
            .isInstanceOf(PlaylistUnauthorizedException.class);

        verify(playlistRepository).findById(playlistId);
        verify(contentRepository, never()).findById(any());
        verify(playlistContentRepository, never()).save(any());
    }

    @Test
    @DisplayName("플레이리스트에 콘텐츠 추가 실패 - 콘텐츠 없음")
    void addContentToPlaylist_ContentNotFound() {
        User user = fixtureMonkey.giveMeBuilder(User.class)
            .set("id", userId)
            .set("role", UserRole.USER)
            .sample();
        
        Playlist playlist = fixtureMonkey.giveMeBuilder(Playlist.class)
            .set("id", playlistId)
            .set("owner", user)
            .sample();

        when(playlistRepository.findById(playlistId)).thenReturn(Optional.of(playlist));
        when(contentRepository.findById(contentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> playlistService.addContentToPlaylist(playlistId, contentId, userId))
            .isInstanceOf(ContentNotFoundException.class);

        verify(playlistRepository).findById(playlistId);
        verify(contentRepository).findById(contentId);
        verify(playlistContentRepository, never()).save(any());
    }

    @Test
    @DisplayName("플레이리스트에 콘텐츠 추가 실패 - 이미 존재하는 콘텐츠")
    void addContentToPlaylist_ContentAlreadyExists() {
        User user = fixtureMonkey.giveMeBuilder(User.class)
            .set("id", userId)
            .set("role", UserRole.USER)
            .sample();
        
        Playlist playlist = fixtureMonkey.giveMeBuilder(Playlist.class)
            .set("id", playlistId)
            .set("owner", user)
            .sample();
        
        Content content = fixtureMonkey.giveMeBuilder(Content.class)
            .set("id", contentId)
            .set("type", ContentType.MOVIE)
            .sample();

        when(playlistRepository.findById(playlistId)).thenReturn(Optional.of(playlist));
        when(contentRepository.findById(contentId)).thenReturn(Optional.of(content));
        when(playlistContentRepository.existsByPlaylistIdAndContentIdAndIsDeletedFalse(playlistId,
            contentId)).thenReturn(true);

        assertThatThrownBy(() -> playlistService.addContentToPlaylist(playlistId, contentId, userId))
            .isInstanceOf(ContentAlreadyExistsException.class);

        verify(playlistRepository).findById(playlistId);
        verify(contentRepository).findById(contentId);
        verify(playlistContentRepository).existsByPlaylistIdAndContentIdAndIsDeletedFalse(playlistId, contentId);
        verify(playlistContentRepository, never()).save(any());
    }

    @Test
    @DisplayName("플레이리스트에서 콘텐츠 삭제 성공")
    void removeContentFromPlaylist_Success() {
        User user = fixtureMonkey.giveMeBuilder(User.class)
            .set("id", userId)
            .set("role", UserRole.USER)
            .sample();
        
        Playlist playlist = fixtureMonkey.giveMeBuilder(Playlist.class)
            .set("id", playlistId)
            .set("owner", user)
            .sample();
        
        PlaylistContent playlistContent = fixtureMonkey.giveMeBuilder(PlaylistContent.class)
            .set("playlist", playlist)
            .set("isDeleted", false)
            .sample();

        when(playlistRepository.findById(playlistId)).thenReturn(Optional.of(playlist));
        when(playlistContentRepository.findByPlaylistIdAndContentIdAndIsDeletedFalse(playlistId, contentId)).thenReturn(
            Optional.of(playlistContent));

        playlistService.removeContentFromPlaylist(playlistId, contentId, userId);

        verify(playlistRepository).findById(playlistId);
        verify(playlistContentRepository).findByPlaylistIdAndContentIdAndIsDeletedFalse(playlistId, contentId);
        verify(playlistContentRepository).save(playlistContent);
    }

    @Test
    @DisplayName("플레이리스트에서 콘텐츠 삭제 실패 - 플레이리스트 없음")
    void removeContentFromPlaylist_PlaylistNotFound() {
        when(playlistRepository.findById(playlistId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> playlistService.removeContentFromPlaylist(playlistId, contentId, userId))
            .isInstanceOf(PlaylistNotFoundException.class);

        verify(playlistRepository).findById(playlistId);
        verify(playlistContentRepository, never()).findByPlaylistIdAndContentIdAndIsDeletedFalse(any(), any());
        verify(playlistContentRepository, never()).save(any());
    }

    @Test
    @DisplayName("플레이리스트에서 콘텐츠 삭제 실패 - 권한 없음")
    void removeContentFromPlaylist_Unauthorized() {
        UUID differentUserId = UUID.randomUUID();
        
        User user = fixtureMonkey.giveMeBuilder(User.class)
            .set("id", userId)
            .set("role", UserRole.USER)
            .sample();
        
        Playlist playlist = fixtureMonkey.giveMeBuilder(Playlist.class)
            .set("id", playlistId)
            .set("owner", user)
            .sample();

        when(playlistRepository.findById(playlistId)).thenReturn(Optional.of(playlist));

        assertThatThrownBy(() -> playlistService.removeContentFromPlaylist(playlistId, contentId, differentUserId))
            .isInstanceOf(PlaylistUnauthorizedException.class);

        verify(playlistRepository).findById(playlistId);
        verify(playlistContentRepository, never()).findByPlaylistIdAndContentIdAndIsDeletedFalse(any(), any());
        verify(playlistContentRepository, never()).save(any());
    }

    @Test
    @DisplayName("플레이리스트에서 콘텐츠 삭제 실패 - 플레이리스트에 없는 콘텐츠")
    void removeContentFromPlaylist_ContentNotInPlaylist() {
        User user = fixtureMonkey.giveMeBuilder(User.class)
            .set("id", userId)
            .set("role", UserRole.USER)
            .sample();
        
        Playlist playlist = fixtureMonkey.giveMeBuilder(Playlist.class)
            .set("id", playlistId)
            .set("owner", user)
            .sample();

        when(playlistRepository.findById(playlistId)).thenReturn(Optional.of(playlist));
        when(playlistContentRepository.findByPlaylistIdAndContentIdAndIsDeletedFalse(playlistId, contentId)).thenReturn(
            Optional.empty());

        assertThatThrownBy(() -> playlistService.removeContentFromPlaylist(playlistId, contentId, userId))
            .isInstanceOf(ContentNotInPlaylistException.class);

        verify(playlistRepository).findById(playlistId);
        verify(playlistContentRepository).findByPlaylistIdAndContentIdAndIsDeletedFalse(playlistId, contentId);
        verify(playlistContentRepository, never()).save(any());
    }

    @Test
    @DisplayName("플레이리스트 목록 조회 성공 - updatedAt 정렬")
    void getPlaylists_Success_SortByUpdatedAt() {
        String keywordLike = null;
        UUID ownerIdEqual = null;
        UUID subscriberIdEqual = null;
        String cursor = null;
        UUID idAfter = null;
        int limit = 2;
        String sortBy = "updatedAt";
        String sortDirection = "desc";
        UUID currentUserId = UUID.randomUUID();

        User owner = fixtureMonkey.giveMeBuilder(User.class)
            .set("id", UUID.randomUUID())
            .set("role", UserRole.USER)
            .sample();
        
        Playlist playlist1 = fixtureMonkey.giveMeBuilder(Playlist.class)
            .set("owner", owner)
            .set("isDeleted", false)
            .sample();
        
        Playlist playlist2 = fixtureMonkey.giveMeBuilder(Playlist.class)
            .set("owner", owner)
            .set("isDeleted", false)
            .sample();

        List<Playlist> mockPlaylists = Arrays.asList(playlist1, playlist2);
        PlaylistDto mockDto1 = mock(PlaylistDto.class);
        PlaylistDto mockDto2 = mock(PlaylistDto.class);

        when(playlistRepository.findPlaylistsWithCursor(
            eq(keywordLike),
            eq(ownerIdEqual),
            eq(subscriberIdEqual),
            eq(sortBy),
            eq(sortDirection),
            any(),
            any(),
            eq(idAfter),
            eq(limit)
        )).thenReturn(mockPlaylists);
        when(playlistRepository.countPlaylists(keywordLike, ownerIdEqual, subscriberIdEqual)).thenReturn(10L);
        when(playlistContentRepository.findByPlaylistIdAndIsDeletedFalse(any())).thenReturn(new ArrayList<>());
        when(subscriptionRepository.existsByUserIdAndPlaylistId(eq(currentUserId), any())).thenReturn(false);
        when(playlistMapper.toDto(eq(playlist1), anyList(), eq(false), eq(false))).thenReturn(mockDto1);
        when(playlistMapper.toDto(eq(playlist2), anyList(), eq(false), eq(false))).thenReturn(mockDto2);

        CursorResponsePlaylistDto result = playlistService.getPlaylists(
            keywordLike,
            ownerIdEqual,
            subscriberIdEqual,
            cursor,
            idAfter,
            limit,
            sortBy,
            sortDirection,
            currentUserId
        );

        assertThat(result).isNotNull();
        assertThat(result.data()).hasSize(2);
        assertThat(result.hasNext()).isFalse();
        assertThat(result.totalCount()).isEqualTo(10);
        verify(playlistRepository).findPlaylistsWithCursor(
            eq(keywordLike),
            eq(ownerIdEqual),
            eq(subscriberIdEqual),
            eq(sortBy),
            eq(sortDirection),
            any(),
            any(),
            eq(idAfter),
            eq(limit)
        );
        verify(playlistRepository).countPlaylists(keywordLike, ownerIdEqual, subscriberIdEqual);
    }

    @Test
    @DisplayName("플레이리스트 목록 조회 성공 - subscriberCount 정렬 및 커서 페이징")
    void getPlaylists_Success_SortBySubscriberCountWithCursor() {
        String keywordLike = null;
        UUID ownerIdEqual = null;
        UUID subscriberIdEqual = null;
        String cursor = "100";
        UUID idAfter = UUID.randomUUID();
        int limit = 2;
        String sortBy = "subscriberCount";
        String sortDirection = "desc";
        UUID currentUserId = UUID.randomUUID();

        User owner = fixtureMonkey.giveMeBuilder(User.class)
            .set("id", UUID.randomUUID())
            .set("role", UserRole.USER)
            .sample();
        
        UUID mockPlaylist2Id = UUID.randomUUID();
        
        Playlist playlist1 = fixtureMonkey.giveMeBuilder(Playlist.class)
            .set("owner", owner)
            .set("isDeleted", false)
            .sample();
        
        Playlist playlist2 = fixtureMonkey.giveMeBuilder(Playlist.class)
            .set("id", mockPlaylist2Id)
            .set("owner", owner)
            .set("subscriberCount", 50L)
            .set("isDeleted", false)
            .sample();
        
        Playlist playlist3 = fixtureMonkey.giveMeBuilder(Playlist.class)
            .set("owner", owner)
            .set("isDeleted", false)
            .sample();

        List<Playlist> mockPlaylists = Arrays.asList(playlist1, playlist2, playlist3);
        PlaylistDto mockDto1 = mock(PlaylistDto.class);
        PlaylistDto mockDto2 = mock(PlaylistDto.class);

        when(playlistRepository.findPlaylistsWithCursor(
            eq(keywordLike),
            eq(ownerIdEqual),
            eq(subscriberIdEqual),
            eq(sortBy),
            eq(sortDirection),
            any(),
            eq(100L),
            eq(idAfter),
            eq(limit)
        )).thenReturn(mockPlaylists);
        when(playlistRepository.countPlaylists(keywordLike, ownerIdEqual, subscriberIdEqual)).thenReturn(5L);
        when(playlistContentRepository.findByPlaylistIdAndIsDeletedFalse(any())).thenReturn(new ArrayList<>());
        when(subscriptionRepository.existsByUserIdAndPlaylistId(eq(currentUserId), any())).thenReturn(false);
        when(playlistMapper.toDto(eq(playlist1), anyList(), eq(false), eq(false))).thenReturn(mockDto1);
        when(playlistMapper.toDto(eq(playlist2), anyList(), eq(false), eq(false))).thenReturn(mockDto2);

        CursorResponsePlaylistDto result = playlistService.getPlaylists(
            keywordLike,
            ownerIdEqual,
            subscriberIdEqual,
            cursor,
            idAfter,
            limit,
            sortBy,
            sortDirection,
            currentUserId
        );

        assertThat(result).isNotNull();
        assertThat(result.data()).hasSize(2);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.nextCursor()).isEqualTo("50");
        assertThat(result.nextIdAfter()).isEqualTo(mockPlaylist2Id);
        verify(playlistRepository).findPlaylistsWithCursor(
            eq(keywordLike),
            eq(ownerIdEqual),
            eq(subscriberIdEqual),
            eq(sortBy),
            eq(sortDirection),
            any(),
            eq(100L),
            eq(idAfter),
            eq(limit)
        );
    }

    @Test
    @DisplayName("플레이리스트 목록 조회 성공 - 인증되지 않은 사용자")
    void getPlaylists_Success_NullCurrentUser() {
        String keywordLike = null;
        UUID ownerIdEqual = null;
        UUID subscriberIdEqual = null;
        String cursor = null;
        UUID idAfter = null;
        int limit = 2;
        String sortBy = "updatedAt";
        String sortDirection = "desc";
        UUID currentUserId = null;

        Playlist playlist1 = fixtureMonkey.giveMeBuilder(Playlist.class)
            .set("isDeleted", false)
            .sample();

        List<Playlist> mockPlaylists = Arrays.asList(playlist1);
        PlaylistDto mockDto1 = mock(PlaylistDto.class);

        when(playlistRepository.findPlaylistsWithCursor(
            eq(keywordLike),
            eq(ownerIdEqual),
            eq(subscriberIdEqual),
            eq(sortBy),
            eq(sortDirection),
            any(),
            any(),
            eq(idAfter),
            eq(limit)
        )).thenReturn(mockPlaylists);
        when(playlistRepository.countPlaylists(keywordLike, ownerIdEqual, subscriberIdEqual)).thenReturn(1L);
        when(playlistContentRepository.findByPlaylistIdAndIsDeletedFalse(any())).thenReturn(new ArrayList<>());
        when(playlistMapper.toDto(eq(playlist1), anyList(), eq(false), eq(false))).thenReturn(mockDto1);

        CursorResponsePlaylistDto result = playlistService.getPlaylists(
            keywordLike,
            ownerIdEqual,
            subscriberIdEqual,
            cursor,
            idAfter,
            limit,
            sortBy,
            sortDirection,
            currentUserId
        );

        assertThat(result).isNotNull();
        assertThat(result.data()).hasSize(1);
        verify(subscriptionRepository, never()).existsByUserIdAndPlaylistId(any(), any());
    }
}
