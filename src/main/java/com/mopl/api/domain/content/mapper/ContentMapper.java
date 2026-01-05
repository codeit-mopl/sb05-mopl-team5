package com.mopl.api.domain.content.mapper;

import com.mopl.api.domain.content.dto.response.ContentDto;
import com.mopl.api.domain.content.entity.Content;
import java.util.Arrays;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", imports = {Arrays.class})
public interface ContentMapper {

    @Mapping(target = "tags", expression = "java(Arrays.asList(content.getTags().split(\"\\\\|\")))")
    ContentDto toDto(Content content);
}
