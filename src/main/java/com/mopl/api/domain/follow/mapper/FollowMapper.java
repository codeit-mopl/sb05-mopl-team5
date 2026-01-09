package com.mopl.api.domain.follow.mapper;

import com.mopl.api.domain.follow.dto.response.FollowDto;
import com.mopl.api.domain.follow.entity.Follow;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FollowMapper {

    @Mapping(target = "followerId", source = "follower.id")
    @Mapping(target = "followeeId", source = "followee.id")
    FollowDto toDto(Follow follow);
}