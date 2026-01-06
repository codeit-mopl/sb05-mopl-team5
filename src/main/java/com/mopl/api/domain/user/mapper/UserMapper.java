package com.mopl.api.domain.user.mapper;

import com.mopl.api.domain.user.dto.response.UserDto;
import com.mopl.api.domain.user.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserDto toDto(User user);

}
