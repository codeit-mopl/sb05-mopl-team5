package com.mopl.api.domain.user.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mopl.api.domain.user.dto.response.CursorResponseWatchingSessionDto;
import com.mopl.api.domain.user.dto.response.WatchingSessionDto;
import com.mopl.api.domain.user.service.WatchingSessionService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
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
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @BeforeEach
    void setUp() {
        // TODO 테스트에 필요한 세팅
    }

    @Test
    @DisplayName("사용자 ID로 시청 세션 조회 - 정상 요청")
    void getWatchingSessionByUser() throws Exception {
        UUID watcherId = UUID.randomUUID();

        WatchingSessionDto responseDto = WatchingSessionDto.builder()
                                                           .id(UUID.randomUUID())
                                                           .createdAt(LocalDateTime.now())
                                                           .build();
        mockMvc.perform(
                   get("/api/users/{watcherId}/watching-sessions", watcherId)
               )
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.id").value(responseDto.id()
                                                            .toString()))
               .andExpect(jsonPath("$.createdAt").value(responseDto.createdAt()
                                                                   .toString()));
    }

    @Test
    @DisplayName("콘텐츠 ID로 시청 세션 조회 - 커서 기반 조회")
    void getWatchingSessionByContent() throws Exception {
        UUID contentId = UUID.randomUUID();

        CursorResponseWatchingSessionDto responseDto = CursorResponseWatchingSessionDto.builder()
                                                                                       .data(List.of(
                                                                                           WatchingSessionDto.builder()
                                                                                                             .build()))
                                                                                       .hasNext(true)
                                                                                       .build();

        mockMvc.perform(
                   get("/api/contents/{contentId}/watching-sessions", contentId)
                       .param("cursor", "2025-12-30T00:00:00Z")
                       .param("limit", "10")
                       .param("sortDirection", "ASCENDING")
                       .param("sortBy", "createdAt")
               )
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.hasNext").value(responseDto.hasNext()));
    }


    @Test
    @DisplayName("콘텐츠 ID로 시청 세션 조회 - 커서 기반 조회 파라미터 예외 발생")
    void getWatchingSessionByContentBadRequest() throws Exception {
        UUID contentId = UUID.randomUUID();

        mockMvc.perform(
                   get("/api/contents/{contentId}/watching-sessions", contentId)
                       .param("cursor", "2025-12-30T00:00:00Z")
                       .param("limit", "10")
               )
               .andExpect(status().isBadRequest());
    }
}
