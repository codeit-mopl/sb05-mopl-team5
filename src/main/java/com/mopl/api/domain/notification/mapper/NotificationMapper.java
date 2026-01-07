package com.mopl.api.domain.notification.mapper;

import com.mopl.api.domain.notification.dto.response.NotificationDto;
import com.mopl.api.domain.notification.entity.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    @Mapping(target = "receiverId", source = "receiver.id")
    NotificationDto toDto(Notification notification);
}