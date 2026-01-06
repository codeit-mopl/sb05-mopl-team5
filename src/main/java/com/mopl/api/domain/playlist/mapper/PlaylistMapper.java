package com.mopl.api.domain.playlist.mapper;

import com.mopl.api.domain.playlist.dto.response.PlaylistDto;
import com.mopl.api.domain.playlist.entity.Playlist;
import com.mopl.api.domain.playlist.entity.PlaylistContent;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring")
    //, unmappedTargetPolicy = ReportingPolicy.IGNORE) // 테스트 코드 사용할때 에러 무시
public abstract class PlaylistMapper {

    @Mapping(target = "id", source = "playlist.id")
    @Mapping(target = "title", source = "playlist.title")
    @Mapping(target = "description", source = "playlist.description")
    @Mapping(target = "subscriberCount", source = "playlist.subscriberCount")
    @Mapping(target = "createdAt", expression = "java(toLocalDateTime(playlist.getCreatedAt()))")
    @Mapping(target = "updatedAt", expression = "java(toLocalDateTime(playlist.getUpdatedAt()))")
    @Mapping(target = "subscribedByMe", source = "subscribedByMe")
    @Mapping(target = "isOwner", source = "isOwner")
    public abstract PlaylistDto toDto(
        Playlist playlist,
        List<PlaylistContent> playlistContents,
        boolean subscribedByMe,
        boolean isOwner
    );

    protected LocalDateTime toLocalDateTime(LocalDateTime dateTime) {
        return dateTime;
    }

    protected LocalDateTime toLocalDateTime(Instant instant) {
        if (instant == null) {
            return null;
        }
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }
}
