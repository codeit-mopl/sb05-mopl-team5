package com.mopl.api.domain.playlist.mapper;

import com.mopl.api.domain.content.dto.response.ContentDto;
import com.mopl.api.domain.content.mapper.ContentMapper;
import com.mopl.api.domain.playlist.dto.response.OwnerDto;
import com.mopl.api.domain.playlist.dto.response.PlaylistDto;
import com.mopl.api.domain.playlist.entity.Playlist;
import com.mopl.api.domain.playlist.entity.PlaylistContent;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = ContentMapper.class)
public interface PlaylistMapper {

    @Mapping(target = "id", source = "playlist.id")
    @Mapping(target = "owner", expression = "java(mapOwner(playlist))")
    @Mapping(target = "title", source = "playlist.title")
    @Mapping(target = "description", source = "playlist.description")
    @Mapping(target = "contents", expression = "java(mapContents(playlistContents))")
    @Mapping(target = "subscriberCount", source = "playlist.subscriberCount")
    @Mapping(target = "subscribedByMe", source = "subscribedByMe")
    @Mapping(target = "isOwner", source = "isOwner")
    PlaylistDto toDto(
        Playlist playlist,
        List<PlaylistContent> playlistContents,
        boolean subscribedByMe,
        boolean isOwner
    );

    default OwnerDto mapOwner(Playlist playlist) {
        if (playlist == null || playlist.getOwner() == null) {
            return null;
        }
        return OwnerDto.builder()
            .userId(playlist.getOwner().getId())
            .name(playlist.getOwner().getName())
            .profileImageUrl(playlist.getOwner().getProfileImageUrl())
            .build();
    }

    default List<ContentDto> mapContents(List<PlaylistContent> playlistContents) {
        if (playlistContents == null) {
            return List.of();
        }
        return playlistContents.stream()
            .map(pc -> toContentDto(pc.getContent()))
            .toList();
    }

    @Mapping(target = "tags", expression = "java(java.util.Arrays.asList(content.getTags().split(\"\\\\|\")))")
    @Mapping(target = "type", expression = "java(content.getType().getValue())")
    ContentDto toContentDto(com.mopl.api.domain.content.entity.Content content);
}
