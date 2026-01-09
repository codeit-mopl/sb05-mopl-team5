package com.mopl.api.domain.playlist.mapper;

import com.mopl.api.domain.content.dto.response.ContentDto;
import com.mopl.api.domain.content.mapper.ContentMapper;
import com.mopl.api.domain.playlist.dto.response.PlaylistDto;
import com.mopl.api.domain.playlist.entity.Playlist;
import com.mopl.api.domain.playlist.entity.PlaylistContent;
import com.mopl.api.domain.user.mapper.UserMapper;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring")
public abstract class PlaylistMapper {

    @Autowired
    protected UserMapper userMapper;

    @Autowired
    protected ContentMapper contentMapper;

    @Mapping(target = "id", source = "playlist.id")
    @Mapping(target = "owner", expression = "java(userMapper.toDto(playlist.getOwner()))")
    @Mapping(target = "title", source = "playlist.title")
    @Mapping(target = "description", source = "playlist.description")
    @Mapping(target = "contents", expression = "java(mapContents(playlistContents))")
    @Mapping(target = "subscriberCount", source = "playlist.subscriberCount")
    @Mapping(target = "subscribedByMe", source = "subscribedByMe")
    @Mapping(target = "isOwner", source = "isOwner")
    public abstract PlaylistDto toDto(
        Playlist playlist,
        List<PlaylistContent> playlistContents,
        boolean subscribedByMe,
        boolean isOwner
    );

    protected List<ContentDto> mapContents(List<PlaylistContent> playlistContents) {
        if (playlistContents == null) {
            return List.of();
        }
        return playlistContents.stream()
            .map(pc -> contentMapper.toDto(pc.getContent()))
            .collect(Collectors.toList());
    }
}
