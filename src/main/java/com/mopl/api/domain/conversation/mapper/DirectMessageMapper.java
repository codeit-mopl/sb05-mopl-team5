package com.mopl.api.domain.conversation.mapper;

import com.mopl.api.domain.conversation.dto.response.direct.DirectMessageDto;
import com.mopl.api.domain.conversation.dto.response.direct.DirectMessageReceiver;
import com.mopl.api.domain.conversation.dto.response.direct.DirectMessageSender;
import com.mopl.api.domain.conversation.entity.DirectMessage;
import com.mopl.api.domain.user.dto.response.UserSummary;
import com.mopl.api.domain.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DirectMessageMapper {

    @Mapping(target = "conversationId", source = "conversation.id")
    @Mapping(target = "sender", source = "sender")
    @Mapping(target = "receiver", source = "receiver")
    DirectMessageDto toDto(DirectMessage message);

    default UserSummary map(User user) {
        return UserSummary.builder()
                          .userId(user.getId())
                          .name(user.getName())
                          .profileImageUrl(user.getProfileImageUrl())
                          .build();
    }
}