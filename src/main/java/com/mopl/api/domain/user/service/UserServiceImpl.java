package com.mopl.api.domain.user.service;

import com.mopl.api.domain.user.dto.request.CursorRequestUserDto;
import com.mopl.api.domain.user.dto.request.UserRoleUpdateRequest;
import com.mopl.api.domain.user.dto.response.CursorResponseUserDto;
import com.mopl.api.domain.user.dto.request.ResetPasswordRequest;
import com.mopl.api.domain.user.dto.request.UserCreateRequest;
import com.mopl.api.domain.user.dto.response.UserDto;
import com.mopl.api.domain.user.dto.request.UserLockUpdateRequest;
import com.mopl.api.domain.user.dto.request.UserUpdateRequest;
import com.mopl.api.domain.user.entity.UserRole;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UserServiceImpl implements UserService {

    @Override
    public UserDto createUser(UserCreateRequest request) {
        return new UserDto(
            UUID.randomUUID(),
            LocalDateTime.now(),
            request.email(),
            request.name(),
            null,
            UserRole.USER,
            true
        );
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
}
