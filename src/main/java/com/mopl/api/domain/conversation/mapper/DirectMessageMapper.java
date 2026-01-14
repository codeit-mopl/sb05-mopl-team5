package com.mopl.api.domain.conversation.mapper;

import com.mopl.api.domain.conversation.dto.response.direct.DirectMessageDto;
import com.mopl.api.domain.conversation.dto.response.direct.DirectMessageReceiver;
import com.mopl.api.domain.conversation.dto.response.direct.DirectMessageSender;
import com.mopl.api.domain.conversation.entity.DirectMessage;
import com.mopl.api.domain.user.dto.response.UserSummary;
import com.mopl.api.domain.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface DirectMessageMapper {

    @Mapping(target = "conversationId", source = "conversation.id")
    @Mapping(target = "sender", source = "sender", qualifiedByName = "mapSender")
    @Mapping(target = "receiver", source = "receiver", qualifiedByName = "mapReceiver")
    DirectMessageDto toDto(DirectMessage message);


    @Named("mapSender")
    default DirectMessageSender mapSender(User user) {
        if (user == null) return null; // Null 체크 추가 권장
        return DirectMessageSender.builder()
                                  .userId(user.getId())
                                  .name(user.getName())
                                  .profileImageUrl(user.getProfileImageUrl())
                                  .build();
    }


    default DirectMessageReceiver mapReceiver(User user) {
        if (user == null) return null;
        return DirectMessageReceiver.builder()
                                    .userId(user.getId())
                                    .name(user.getName())
                                    .profileImageUrl(user.getProfileImageUrl())
                                    .build();
    }
}