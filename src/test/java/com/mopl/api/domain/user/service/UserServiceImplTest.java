package com.mopl.api.domain.user.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.mopl.api.domain.notification.dto.event.RoleChangedEvent;
import com.mopl.api.domain.user.dto.request.ChangePasswordRequest;
import com.mopl.api.domain.user.dto.request.CursorRequestUserDto;
import com.mopl.api.domain.user.dto.request.UserCreateRequest;
import com.mopl.api.domain.user.dto.request.UserLockUpdateRequest;
import com.mopl.api.domain.user.dto.request.UserRoleUpdateRequest;
import com.mopl.api.domain.user.dto.response.CursorResponseUserDto;
import com.mopl.api.domain.user.dto.response.UserDto;
import com.mopl.api.domain.user.entity.User;
import com.mopl.api.domain.user.entity.UserRole;
import com.mopl.api.domain.user.exception.user.detail.DuplicateEmailException;
import com.mopl.api.domain.user.exception.user.detail.UserNotFoundException;
import com.mopl.api.domain.user.mapper.UserMapper;
import com.mopl.api.domain.user.repository.UserRepository;
import com.mopl.api.global.config.security.jwt.JwtRegistry;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
public class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UserMapper userMapper;
    @Mock
    private JwtRegistry jwtRegistry;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    @DisplayName("사용자 회원가입 성공")
    void createUser_success() {
        // given
        String email = "test@test.com";
        String beforepw = "password";
        String encodedPw = "encoded_password";
        String name = "test";

        UserCreateRequest request = new UserCreateRequest(name, email, beforepw);

        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(passwordEncoder.encode(beforepw)).thenReturn(encodedPw);

        UserDto expectedDto = mock(UserDto.class);
        when(userMapper.toDto(any(User.class))).thenReturn(expectedDto);

        // when
        UserDto result = userService.createUser(request);

        // then
        assertThat(result).isSameAs(expectedDto);

        verify(userRepository).existsByEmail(email);
        verify(passwordEncoder).encode(beforepw);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(captor.capture());

        User saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo(email);
        assertThat(saved.getPassword()).isEqualTo(encodedPw);
        assertThat(saved.getName()).isEqualTo(name);

        verify(userMapper).toDto(saved);
    }

    @Test
    @DisplayName("사용자 회원가입 실패 - 이메일 중복")
    void createUser_duplicateEmail_throws() {
        // given
        String email = "dup@mopl.com";
        UserCreateRequest request = new UserCreateRequest("test", email, "password");

        when(userRepository.existsByEmail(email)).thenReturn(true);

        // when + then
        assertThatThrownBy(() -> userService.createUser(request))
            .isInstanceOf(DuplicateEmailException.class);

        verify(userRepository).existsByEmail(email);
        verify(userRepository, never()).saveAndFlush(any());
        verifyNoInteractions(passwordEncoder, userMapper);
    }


    @Test
    @DisplayName("사용자 단건 조회 성공")
    void getUser_success() {
        // given
        UUID userId = UUID.randomUUID();

        User user = mock(User.class);
        when(user.getName()).thenReturn("test");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UserDto expectedDto = mock(UserDto.class);
        when(userMapper.toDto(user)).thenReturn(expectedDto);

        // when
        UserDto result = userService.getUser(userId);

        // then
        assertThat(result).isSameAs(expectedDto);
        verify(userRepository).findById(userId);
        verify(userMapper).toDto(user);
        verifyNoMoreInteractions(userRepository, userMapper, user);
    }

    @Test
    @DisplayName("사용자 단건 조회 실패 - 존재하지 않는 사용자")
    void getUser_notFound_throws() {
        // given
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // when + then
        assertThatThrownBy(() -> userService.getUser(userId))
            .isInstanceOf(UserNotFoundException.class);

        verify(userRepository).findById(userId);
        verifyNoInteractions(userMapper);
    }

    @Test
    @DisplayName("비밀번호 변경 성공")
    void updatePassword_success() {
        // given
        UUID userId = UUID.randomUUID();

        String rawPw = "password";
        String encodedPw = "ENCODED_PASSWORD";

        ChangePasswordRequest request = new ChangePasswordRequest(rawPw);

        User user = mock(User.class);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(rawPw)).thenReturn(encodedPw);

        // when
        userService.updatePassword(userId, request);

        // then
        InOrder inOrder = inOrder(userRepository, passwordEncoder, user, jwtRegistry);
        inOrder.verify(userRepository).findById(userId);
        inOrder.verify(passwordEncoder).encode(rawPw);
        inOrder.verify(user).updatePassword(encodedPw);
        inOrder.verify(jwtRegistry).invalidateJwtInformationByUserId(userId);

        verifyNoMoreInteractions(userRepository, passwordEncoder, user, jwtRegistry);
    }

    @Test
    @DisplayName("비밀번호 변경 실패 - 존재하지 않는 사용자")
    void updatePassword_userNotFound_throws() {
        // given
        UUID userId = UUID.randomUUID();
        ChangePasswordRequest request = new ChangePasswordRequest("newPassword");

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // when + then
        assertThatThrownBy(() -> userService.updatePassword(userId, request))
            .isInstanceOf(UserNotFoundException.class);

        verify(userRepository).findById(userId);
        verifyNoInteractions(passwordEncoder, jwtRegistry);
    }

    @Test
    @DisplayName("사용자 목록 조회 성공")
    void getAllUsers_success() {
        // given
        CursorRequestUserDto request = mock(CursorRequestUserDto.class);

        @SuppressWarnings("unchecked")
        CursorResponseUserDto<UserDto> expectedResponse = mock(CursorResponseUserDto.class);

        when(userRepository.findAllUsers(request))
            .thenReturn(expectedResponse);

        // when
        CursorResponseUserDto<UserDto> result =
            userService.getAllUsers(request);

        // then
        assertThat(result).isSameAs(expectedResponse);

        verify(userRepository).findAllUsers(request);
        verifyNoMoreInteractions(userRepository);
    }


    @Test
    @DisplayName("권한 변경 성공 - USER -> ADMIN")
    void updateUserRole_success() {
        // given
        UUID userId = UUID.randomUUID();

        UserRole beforeRole = UserRole.USER;
        UserRole newRole = UserRole.ADMIN;
        UserRoleUpdateRequest request = new UserRoleUpdateRequest(newRole);

        User user = mock(User.class);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(user.getRole()).thenReturn(beforeRole, newRole);

        // when
        userService.updateUserRole(userId, request);

        // then
        verify(userRepository).findById(userId);
        verify(user).updateUserRole(newRole);

        ArgumentCaptor<RoleChangedEvent> eventCaptor = ArgumentCaptor.forClass(RoleChangedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        RoleChangedEvent publishedEvent = eventCaptor.getValue();
        assertThat(publishedEvent.userId()).isEqualTo(userId);
        assertThat(publishedEvent.beforeRole()).isEqualTo(String.valueOf(beforeRole));
        assertThat(publishedEvent.currentRole()).isEqualTo(String.valueOf(newRole));

        verify(jwtRegistry).invalidateJwtInformationByUserId(userId);
    }

    @Test
    @DisplayName("권한 변경 실패 - 존재하지 않는 사용자, 이벤트 발행/로그아웃/업데이트 모두 수행하지 않음")
    void updateUserRole_userNotFound_throws() {
        // given
        UUID userId = UUID.randomUUID();
        UserRoleUpdateRequest request = new UserRoleUpdateRequest(UserRole.ADMIN);

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // when + then
        assertThatThrownBy(() -> userService.updateUserRole(userId, request))
            .isInstanceOf(UserNotFoundException.class);

        verify(userRepository).findById(userId);
        verifyNoInteractions(eventPublisher, jwtRegistry);
    }

    @Test
    @DisplayName("잠금 상태 변경 성공 - locked 업데이트 + 토큰 무효화")
    void updateUserLock_success() {
        // given
        UUID userId = UUID.randomUUID();
        Boolean beforeLocked = false;
        Boolean newLocked = true;

        UserLockUpdateRequest request = new UserLockUpdateRequest(newLocked);
        User user = mock(User.class);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(user.getLocked()).thenReturn(beforeLocked, newLocked);

        // when
        userService.updateUserLock(userId, request);

        // then
        verify(userRepository).findById(userId);
        verify(user).updateUserLock(newLocked);
        verify(jwtRegistry).invalidateJwtInformationByUserId(userId);

        // 호출 순서 검증
        InOrder inOrder = inOrder(userRepository, user, jwtRegistry);
        inOrder.verify(userRepository).findById(userId);
        inOrder.verify(user).getLocked();            // before
        inOrder.verify(user).updateUserLock(newLocked);
        inOrder.verify(jwtRegistry).invalidateJwtInformationByUserId(userId);
        inOrder.verify(user).getLocked();            // 로그용 after

        verifyNoMoreInteractions(userRepository, user, jwtRegistry);
    }

    @Test
    @DisplayName("잠금 상태 변경 실패 - 존재하지 않는 사용자, 업데이트/로그아웃 수행하지 않음")
    void updateUserLock_userNotFound_throws() {
        // given
        UUID userId = UUID.randomUUID();
        UserLockUpdateRequest request = new UserLockUpdateRequest(true);

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // when + then
        assertThatThrownBy(() -> userService.updateUserLock(userId, request))
            .isInstanceOf(UserNotFoundException.class);

        verify(userRepository).findById(userId);
        verifyNoInteractions(jwtRegistry);
    }

}
