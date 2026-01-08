package com.mopl.api.domain.user.mapper;

import com.mopl.api.domain.content.mapper.ContentMapper;
import com.mopl.api.domain.user.dto.response.WatchingSessionDto;
import com.mopl.api.domain.user.entity.WatchingSession;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(
    componentModel = "spring",
    uses = {UserMapper.class, ContentMapper.class}
)
public interface WatchingSessionMapper {

    @Mapping(source = "watcher", target = "watcher")
    @Mapping(source = "content", target = "content")
    WatchingSessionDto toDto(WatchingSession entity);

    List<WatchingSessionDto> toDtoList(List<WatchingSession> entities);
}