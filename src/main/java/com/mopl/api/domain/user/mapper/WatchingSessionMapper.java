package com.mopl.api.domain.user.mapper;

import com.mopl.api.domain.content.dto.response.ContentDto;
import com.mopl.api.domain.user.dto.response.UserDto;
import com.mopl.api.domain.user.dto.response.WatchingSessionDto;
import com.mopl.api.domain.user.entity.WatchingSession;
import java.util.Arrays;
import java.util.List;
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
                                 .user(UserDto.builder()
                                              .id(entity.getWatcher()
                                                        .getId())
                                              .email(entity.getWatcher()
                                                           .getEmail())
                                              .name(entity.getWatcher()
                                                          .getName())
                                              .role(entity.getWatcher()
                                                          .getRole())
                                              .locked(entity.getWatcher()
                                                            .getLocked())
                                              .profileImageUrl(entity.getWatcher()
                                                                     .getProfileImageUrl())
                                              .createdAt(entity.getWatcher()
                                                               .getCreatedAt())
                                              .build())      // TODO UserMapper 연동
                                 .content(ContentDto.builder()
                                                    .type(entity.getContent()
                                                                .getType())
                                                    .tags(Arrays.stream(entity.getContent()
                                                                              .getTags()
                                                                              .split("\\|"))
                                                                .toList())
                                                    .title(entity.getContent()
                                                                 .getTitle())
                                                    .id(entity.getContent()
                                                              .getId())
                                                    .thumbnailUrl(entity.getContent()
                                                                        .getThumbnailUrl())
                                                    .build())   // TODO ContentMapper 연동
                                 .build();
    }

    public List<WatchingSessionDto> toDtoList(List<WatchingSession> entities) {
        return entities.stream()
                       .map(this::toDto)
                       .toList();
    }
}