package com.mopl.api.domain.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mopl.api.domain.content.dto.response.ContentDto;
import com.mopl.api.domain.user.dto.request.WatchingSessionSearchRequest;
import com.mopl.api.domain.user.dto.response.CursorResponseWatchingSessionDto;
import com.mopl.api.domain.user.dto.response.UserDto;
import com.mopl.api.domain.user.dto.response.WatchingSessionDto;
import com.mopl.api.domain.user.service.WatchingSessionService;
import com.mopl.api.global.config.security.filter.JwtAuthenticationFilter;
import com.mopl.api.global.config.security.jwt.JwtTokenProvider;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(WatchingSessionController.class)
@AutoConfigureMockMvc(addFilters = false)
class WatchingSessionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WatchingSessionService watchingSessionService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    @DisplayName("사용자 ID로 시청 세션 조회 - 중첩 객체 포함 검증")
    void getWatchingSessionByUser() throws Exception {

        UUID watcherId = UUID.randomUUID();
        WatchingSessionDto responseDto = WatchingSessionDto.builder()
                                                           .id(UUID.randomUUID())
                                                           .createdAt(LocalDateTime.now())
                                                           .user(UserDto.builder()
                                                                        .id(watcherId)
                                                                        .name("테스트유저")
                                                                        .build())
                                                           .content(ContentDto.builder()
                                                                              .id(UUID.randomUUID())
                                                                              .title("영화 제목")
                                                                              .build())
                                                           .build();

        given(watchingSessionService.getWatchingSession(watcherId)).willReturn(responseDto);

        mockMvc.perform(get("/api/users/{watcherId}/watching-sessions", watcherId))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.id").value(responseDto.id()
                                                            .toString()))
               .andExpect(jsonPath("$.user.id").value(watcherId.toString()))
               .andExpect(jsonPath("$.user.name").value("테스트유저"))
               .andExpect(jsonPath("$.content.title").value("영화 제목"));
    }

    @Test
    @DisplayName("콘텐츠 ID로 시청 세션 조회 - 커서 기반 리스트 응답 검증")
    void getWatchingSessionByContent() throws Exception {

        UUID contentId = UUID.randomUUID();
        WatchingSessionDto sessionDto = WatchingSessionDto.builder()
                                                          .id(UUID.randomUUID())
                                                          .createdAt(LocalDateTime.now())
                                                          .build();

        CursorResponseWatchingSessionDto responseDto = CursorResponseWatchingSessionDto.builder()
                                                                                       .data(List.of(sessionDto))
                                                                                       .nextCursor(
                                                                                           "2026-01-07T16:00:00")
                                                                                       .nextIdAfter(UUID.randomUUID())
                                                                                       .hasNext(true)
                                                                                       .totalCount(100L)
                                                                                       .sortBy("createdAt")
                                                                                       .sortDirection("DESCENDING")
                                                                                       .build();

        given(watchingSessionService.getWatchingSession(eq(contentId), any(WatchingSessionSearchRequest.class)))
            .willReturn(responseDto);

        mockMvc.perform(get("/api/contents/{contentId}/watching-sessions", contentId)
                   .param("limit", "1")
                   .param("sortBy", "createdAt")
                   .param("sortDirection", "DESCENDING"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.data").isArray())
               .andExpect(jsonPath("$.data[0].id").value(sessionDto.id()
                                                                   .toString()))
               .andExpect(jsonPath("$.nextCursor").value(responseDto.nextCursor()))
               .andExpect(jsonPath("$.hasNext").value(true))
               .andExpect(jsonPath("$.totalCount").value(100));
    }

    @Test
    @DisplayName("콘텐츠 ID로 시청 세션 조회 - 필수 파라미터(limit 등) 누락 시 400 에러")
    void getWatchingSessionByContentBadRequest() throws Exception {

        UUID contentId = UUID.randomUUID();

        mockMvc.perform(get("/api/contents/{contentId}/watching-sessions", contentId)
                   .param("sortBy", "createdAt")
               )
               .andExpect(status().isBadRequest());
    }
}