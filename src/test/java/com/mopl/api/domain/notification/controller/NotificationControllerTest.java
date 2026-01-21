package com.mopl.api.domain.notification.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mopl.api.config.WithMockCustomUser;
import com.mopl.api.domain.notification.dto.request.NotificationCursorPageRequest;
import com.mopl.api.domain.notification.dto.response.CursorResponseNotificationDto;
import com.mopl.api.domain.notification.service.NotificationService;
import com.mopl.api.global.config.security.filter.JwtAuthenticationFilter;
import com.mopl.api.global.config.security.jwt.JwtTokenProvider;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(NotificationController.class)
@AutoConfigureMockMvc(addFilters = false)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    private final String TEST_USER_ID = "550e8400-e29b-41d4-a716-446655440000";

    @Test
    @WithMockCustomUser
    @DisplayName("알림 목록 조회 성공")
    void getNotificationList_Success() throws Exception {
        // given
        UUID userId = UUID.fromString(TEST_USER_ID);
        CursorResponseNotificationDto response = CursorResponseNotificationDto.builder()
                                                                              .data(Collections.emptyList())
                                                                              .hasNext(false)
                                                                              .build();

        given(notificationService.getNotifications(eq(userId), any(NotificationCursorPageRequest.class)))
            .willReturn(response);

        // when & then
        mockMvc.perform(get("/api/notifications")
                   .param("limit", "10")
                   .param("sortDirection", "DESCENDING")
                   .param("sortBy", "createdAt"))
               .andExpect(status().isOk())
               .andDo(print());
    }

    @Test
    @WithMockCustomUser
    @DisplayName("알림 삭제 성공")
    void deleteNotification_Success() throws Exception {
        // given
        UUID notificationId = UUID.randomUUID();

        // when & then
        mockMvc.perform(delete("/api/notifications/{notificationId}", notificationId))
               .andDo(print())
               .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("인증되지 않은 사용자 접근 시 테스트")
    void getNotificationList_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/notifications")
                   .param("limit", "10")
                   .param("sortDirection", "DESCENDING")
                   .param("sortBy", "createdAt"))
               .andDo(print());
    }
}