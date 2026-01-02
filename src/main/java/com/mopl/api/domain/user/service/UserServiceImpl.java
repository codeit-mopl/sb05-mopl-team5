package com.mopl.api.domain.user.service;

import com.mopl.api.domain.user.dto.request.CursorRequestUserDto;
import com.mopl.api.domain.user.dto.request.UserRoleUpdateRequest;
import com.mopl.api.domain.user.dto.response.CursorResponseUserDto;
import com.mopl.api.domain.user.dto.request.ResetPasswordRequest;
import com.mopl.api.domain.user.dto.request.UserCreateRequest;
import com.mopl.api.domain.user.dto.response.UserDto;
import com.mopl.api.domain.user.dto.request.UserLockUpdateRequest;
import com.mopl.api.domain.user.dto.request.UserUpdateRequest;
import com.mopl.api.domain.user.entity.User;
import com.mopl.api.domain.user.entity.UserRole;
import com.mopl.api.domain.user.mapper.UserMapper;
import com.mopl.api.domain.user.repository.UserRepository;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Transactional
    @Override
    public UserDto createUser(UserCreateRequest request) {
       // TODO 이메일 중복 검사
        String encodedPassword = passwordEncoder.encode(request.password());
        User user = new User(request.email(), encodedPassword, request.name());
        userRepository.save(user);
        log.info("User created! email: {}", user.getEmail());
        return userMapper.toDto(user);
    }

    @Override
    public UserDto getUser(UUID userId) {
        return new UserDto(
            userId,
            LocalDateTime.now(),
            "alice@test.com",
            "alice",
            null,
            UserRole.USER,
            true
        );
    }

    @Override
    public void updatePassword(UUID userId, String newPassword) {

    }

    @Override
    public CursorResponseUserDto<UserDto> getAllUsers(CursorRequestUserDto request) {
        return new CursorResponseUserDto<UserDto>(
            new ArrayList<>(),
            null,
            null,
            false,
            0L,
            "sortBy",
            "sortDirection"
        );
    }

    @Override
    public void updateUserRole(UUID userId, UserRoleUpdateRequest request) {

    }

    @Override
    public void updateUserLock(UUID userId, UserLockUpdateRequest request) {

    }

    @Override
    public void resetPassword(ResetPasswordRequest request) {

    }

    @Override
    public UserDto profileChange(UUID userId, String image) {
        return null;
    }
}
