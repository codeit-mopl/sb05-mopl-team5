package com.mopl.api.domain.user.mapper;

import com.mopl.api.domain.user.dto.response.UserDto;
import com.mopl.api.domain.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public abstract class UserMapper {

    public abstract UserDto toDto(User user);

}
