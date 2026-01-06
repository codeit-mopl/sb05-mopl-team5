package com.mopl.api.domain.user.repository.impl;

import com.mopl.api.domain.user.dto.request.CursorRequestUserDto;
import com.mopl.api.domain.user.dto.response.CursorResponseUserDto;
import com.mopl.api.domain.user.dto.response.UserDto;

public interface UserRepositoryCustom {

    CursorResponseUserDto<UserDto> findAllUsers(CursorRequestUserDto request);
}