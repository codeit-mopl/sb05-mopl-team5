package com.mopl.api.domain.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mopl.api.domain.user.dto.request.ChangePasswordRequest;
import com.mopl.api.domain.user.dto.request.UserCreateRequest;
import com.mopl.api.domain.user.dto.request.UserLockUpdateRequest;
import com.mopl.api.domain.user.dto.request.UserRoleUpdateRequest;
import com.mopl.api.domain.user.dto.response.CursorResponseUserDto;
import com.mopl.api.domain.user.dto.response.UserDto;
import com.mopl.api.domain.user.entity.UserRole;
import com.mopl.api.domain.user.service.UserService;
import com.mopl.api.global.config.security.filter.JwtAuthenticationFilter;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = UserController.class,
    excludeFilters = {
        @ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = JwtAuthenticationFilter.class
        )
    }
)

@AutoConfigureMockMvc(addFilters = false) // 보안 필터 비활성화
class UserControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    UserService userService;

    @Test
    @DisplayName("POST /api/users - 회원 생성 성공이면 201과 UserDto 반환")
    void userAdd_success() throws Exception {
        // given
        UserCreateRequest request = new UserCreateRequest("test", "test@mopl.com", "password");
        UserDto response = mockUserDto("test@mopl.com", "test");

        given(userService.createUser(any(UserCreateRequest.class))).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/users")
                   .contentType(MediaType.APPLICATION_JSON)
                   .content(objectMapper.writeValueAsString(request)))
               .andExpect(status().isCreated())
               .andExpect(jsonPath("$.email").value("test@mopl.com"))
               .andExpect(jsonPath("$.name").value("test"));

        then(userService).should().createUser(any(UserCreateRequest.class));
    }

    @Test
    @DisplayName("GET /api/users/{userId} - 유저 조회 성공이면 200과 UserDto 반환")
    void userDetails_success() throws Exception {
        UUID userId = UUID.randomUUID();
        UserDto response = mockUserDto("test@mopl.com", "test");

        given(userService.getUser(userId)).willReturn(response);

        mockMvc.perform(get("/api/users/{userId}", userId))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.email").value("test@mopl.com"))
               .andExpect(jsonPath("$.name").value("test"));

        then(userService).should().getUser(userId);
    }

    @Test
    @DisplayName("PATCH /api/users/{userId}/password - 비번 변경 성공이면 200")
    void passwordModify_success() throws Exception {
        UUID userId = UUID.randomUUID();
        ChangePasswordRequest request = new ChangePasswordRequest("newPassword!");

        mockMvc.perform(patch("/api/users/{userId}/password", userId)
                   .contentType(MediaType.APPLICATION_JSON)
                   .content(objectMapper.writeValueAsString(request)))
               .andExpect(status().isOk());

        then(userService).should().updatePassword(eq(userId), any(ChangePasswordRequest.class));
    }

    @Test
    @DisplayName("GET /api/users - (ADMIN) 유저 리스트 조회 성공이면 200")
    void userList_success() throws Exception {
        CursorResponseUserDto<UserDto> response = CursorResponseUserDto.<UserDto>builder()
                                                                       .data(List.of(mockUserDto("test@mopl.com", "test")))
                                                                       .hasNext(false)
                                                                       .totalCount(1L)
                                                                       .sortBy("createdAt")
                                                                       .sortDirection("ASCENDING")
                                                                       .nextCursor(null)
                                                                       .nextIdAfter(null)
                                                                       .build();

        given(userService.getAllUsers(any())).willReturn(response);

        mockMvc.perform(get("/api/users")
                   .param("limit", "10")
                   .param("sortBy", "createdAt")
                   .param("sortDirection", "ASCENDING"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.totalCount").value(1))
               .andExpect(jsonPath("$.data[0].email").value("test@mopl.com"));

        then(userService).should().getAllUsers(any());
    }

    @Test
    @DisplayName("PATCH /api/users/{userId}/role 'ADMIN' - 성공이면 200")
    void userRoleModify_success() throws Exception {
        UUID userId = UUID.randomUUID();
        UserRoleUpdateRequest request = new UserRoleUpdateRequest(UserRole.ADMIN);

        mockMvc.perform(patch("/api/users/{userId}/role", userId)
                   .contentType(MediaType.APPLICATION_JSON)
                   .content(objectMapper.writeValueAsString(request)))
               .andExpect(status().isOk());

        then(userService).should().updateUserRole(eq(userId), any(UserRoleUpdateRequest.class));
    }

    @Test
    @DisplayName("PATCH /api/users/{userId}/locked 'true' - 성공이면 200")
    void userLockModify_success() throws Exception {
        UUID userId = UUID.randomUUID();
        UserLockUpdateRequest request = new UserLockUpdateRequest(true);

        mockMvc.perform(patch("/api/users/{userId}/locked", userId)
                   .contentType(MediaType.APPLICATION_JSON)
                   .content(objectMapper.writeValueAsString(request)))
               .andExpect(status().isOk());

        then(userService).should().updateUserLock(eq(userId), any(UserLockUpdateRequest.class));
    }

    private UserDto mockUserDto(String email, String name) {
        return new UserDto(
            UUID.randomUUID(),
            null,
            email,
            name,
            null,
            UserRole.USER,
            false
        );
    }
}
