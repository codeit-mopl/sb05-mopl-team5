package com.mopl.api.domain.user.service;

import com.mopl.api.domain.user.dto.request.CursorRequestUserDto;
import com.mopl.api.domain.user.dto.request.UserRoleUpdateRequest;
import com.mopl.api.domain.user.dto.response.CursorResponseUserDto;
import com.mopl.api.domain.user.dto.request.ResetPasswordRequest;
import com.mopl.api.domain.user.dto.request.UserCreateRequest;
import com.mopl.api.domain.user.dto.response.UserDto;
import com.mopl.api.domain.user.dto.request.UserLockUpdateRequest;
import com.mopl.api.domain.user.dto.request.UserUpdateRequest;
import java.util.UUID;

public interface UserService {

    UserDto createUser(UserCreateRequest request);
    UserDto getUser(UUID userId);
    void updatePassword(UUID userId,String newPassword);

    CursorResponseUserDto<UserDto> getAllUsers(CursorRequestUserDto request);
    void updateUserRole(UUID userId, UserRoleUpdateRequest request);
    void updateUserLock(UUID userId, UserLockUpdateRequest request);
    void resetPassword(ResetPasswordRequest request);




}