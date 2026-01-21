package com.mopl.api.domain.sse.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mopl.api.domain.sse.service.SseService;
import com.mopl.api.domain.user.dto.response.UserDto;
import com.mopl.api.global.config.security.claim.CustomUserDetails;
import com.mopl.api.global.config.security.filter.JwtAuthenticationFilter;
import com.mopl.api.global.config.security.jwt.JwtTokenProvider;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@WebMvcTest(SseController.class)
class SseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SseService sseService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    @DisplayName("SSE 연결 성공 - LastEventId 없음")
    void subscribe_Success_NoLastEventId() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        CustomUserDetails mockPrincipal = mockUserDetails(userId);
        SseEmitter mockEmitter = new SseEmitter();

        given(sseService.connect(eq(userId), isNull())).willReturn(mockEmitter);

        // when & then
        mockMvc.perform(get("/api/sse")
                   .with(user(mockPrincipal))
                   .accept("text/event-stream"))
               .andExpect(status().isOk());
    }

    @Test
    @DisplayName("SSE 연결 성공 - LastEventId 포함")
    void subscribe_Success_WithLastEventId() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        UUID lastEventId = UUID.randomUUID();
        CustomUserDetails mockPrincipal = mockUserDetails(userId);
        SseEmitter mockEmitter = new SseEmitter();

        given(sseService.connect(eq(userId), eq(lastEventId))).willReturn(mockEmitter);

        // when & then
        mockMvc.perform(get("/api/sse")
                   .with(user(mockPrincipal))
                   .param("LastEventId", lastEventId.toString())
                   .accept("text/event-stream"))
               .andExpect(status().isOk());
    }

    private CustomUserDetails mockUserDetails(UUID userId) {
        UserDto mockUserDto = org.mockito.Mockito.mock(UserDto.class);
        given(mockUserDto.id()).willReturn(userId);
        CustomUserDetails mockPrincipal = org.mockito.Mockito.mock(CustomUserDetails.class);
        given(mockPrincipal.getUserDto()).willReturn(mockUserDto);
        return mockPrincipal;
    }
}