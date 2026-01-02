package com.mopl.api.domain.user.mapper;

import com.mopl.api.domain.user.dto.response.WatchingSessionDto;
import com.mopl.api.domain.user.entity.WatchingSession;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public abstract class WatchingSessionMapper {

    // TODO UserMapper, ContentMapper가 정의 되면 추후 정리
    public WatchingSessionDto toDto(WatchingSession entity) {
        if (entity == null) {
            return null;
        }

        return WatchingSessionDto.builder()
                                 .id(entity.getId())
                                 .createdAt(entity.getCreatedAt())
                                 .user(null)      // TODO UserMapper 연동
                                 .content(null)   // TODO ContentMapper 연동
                                 .build();
    }
}