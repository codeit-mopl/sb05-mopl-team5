package com.mopl.api.domain.playlist.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mopl.api.domain.playlist.dto.request.PlaylistCreateRequest;
import com.mopl.api.domain.playlist.dto.request.PlaylistUpdateRequest;
import com.mopl.api.domain.playlist.dto.response.PlaylistDto;
import com.mopl.api.domain.playlist.service.PlaylistService;
import com.mopl.api.domain.playlist.service.SubscriptionService;
import com.mopl.api.domain.user.dto.response.UserDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import com.mopl.api.config.WithMockCustomUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;

import static com.mopl.api.domain.user.entity.UserRole.USER;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockCustomUser
@DisplayName("PlaylistController 통합 테스트")
class PlaylistControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PlaylistService playlistService;

    @MockitoBean
    private SubscriptionService subscriptionService;

    private UUID userId;
    private UUID playlistId;
    private UUID contentId;
    private PlaylistCreateRequest createRequest;
    private PlaylistUpdateRequest updateRequest;
    private PlaylistDto playlistDto;
    private UserDto userDto;

    @BeforeEach
    void setUp() {
        userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        playlistId = UUID.randomUUID();
        contentId = UUID.randomUUID();

        userDto = UserDto.builder()
                         .id(userId)
                         .createdAt(LocalDateTime.now())
                         .email("test@example.com")
                         .name("testuser")
                         .profileImageUrl("http://example.com/profile.jpg")
                         .role(USER)
                         .locked(false)
                         .build();

        createRequest = new PlaylistCreateRequest("My Playlist", "Great movies");

        updateRequest = new PlaylistUpdateRequest("Updated Playlist", "Updated Description");

        playlistDto = PlaylistDto.builder()
                                 .id(playlistId)
                                 .owner(userDto)
                                 .title("My Playlist")
                                 .description("Great movies")
                                 .contents(new ArrayList<>())
                                 .subscriberCount(0)
                                 .subscribedByMe(false)
                                 .isOwner(true)
                                 .createdAt(LocalDateTime.now())
                                 .updatedAt(LocalDateTime.now())
                                 .build();
    }

    @Test
    @DisplayName("POST /api/playlists - 플레이리스트 생성 성공")
    void playlistAdd_Success() throws Exception {
        when(playlistService.addPlaylist(any(PlaylistCreateRequest.class), eq(userId)))
            .thenReturn(playlistDto);

        mockMvc.perform(post("/api/playlists")
                   .with(csrf())
                   .contentType(MediaType.APPLICATION_JSON)
                   .content(objectMapper.writeValueAsString(createRequest)))
               .andExpect(status().isCreated())
               .andExpect(jsonPath("$.id").value(playlistId.toString()))
               .andExpect(jsonPath("$.title").value("My Playlist"))
               .andExpect(jsonPath("$.description").value("Great movies"));
    }

    @Test
    @DisplayName("POST /api/playlists - 필수 필드 누락 시 400 에러")
    void playlistAdd_InvalidRequest() throws Exception {
        PlaylistCreateRequest invalidRequest = new PlaylistCreateRequest("", "Description");

        mockMvc.perform(post("/api/playlists")
                   .with(csrf())
                   .contentType(MediaType.APPLICATION_JSON)
                   .content(objectMapper.writeValueAsString(invalidRequest)))
               .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH /api/playlists/{playlistId} - 플레이리스트 수정 성공")
    void playlistModify_Success() throws Exception {
        PlaylistDto updatedDto = PlaylistDto.builder()
                                            .id(playlistId)
                                            .owner(userDto)
                                            .title("Updated Playlist")
                                            .description("Updated Description")
                                            .contents(new ArrayList<>())
                                            .subscriberCount(0)
                                            .subscribedByMe(false)
                                            .isOwner(true)
                                            .createdAt(LocalDateTime.now())
                                            .updatedAt(LocalDateTime.now())
                                            .build();

        when(playlistService.modifyPlaylist(eq(playlistId), any(PlaylistUpdateRequest.class), eq(userId)))
            .thenReturn(updatedDto);

        mockMvc.perform(patch("/api/playlists/{playlistId}", playlistId)
                   .with(csrf())
                   .contentType(MediaType.APPLICATION_JSON)
                   .content(objectMapper.writeValueAsString(updateRequest)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.title").value("Updated Playlist"))
               .andExpect(jsonPath("$.description").value("Updated Description"));
    }

    @Test
    @DisplayName("DELETE /api/playlists/{playlistId} - 플레이리스트 삭제 성공")
    void playlistRemove_Success() throws Exception {
        doNothing().when(playlistService)
                   .removePlaylist(playlistId, userId);

        mockMvc.perform(delete("/api/playlists/{playlistId}", playlistId)
                   .with(csrf()))
               .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /api/playlists/{playlistId} - 플레이리스트 조회 성공")
    void playlistDetails_Success() throws Exception {
        when(playlistService.getPlaylist(playlistId, userId))
            .thenReturn(playlistDto);

        mockMvc.perform(get("/api/playlists/{playlistId}", playlistId))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.id").value(playlistId.toString()))
               .andExpect(jsonPath("$.title").value("My Playlist"));
    }

}
