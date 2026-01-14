package com.mopl.api.domain.conversation.mapper;

import com.mopl.api.domain.conversation.dto.response.direct.DirectMessageDto;
import com.mopl.api.domain.conversation.dto.response.direct.DirectMessageLastestMessage;
import com.mopl.api.domain.conversation.dto.response.direct.DirectMessageReceiver;
import com.mopl.api.domain.conversation.dto.response.direct.DirectMessageResponseDto;
import com.mopl.api.domain.conversation.dto.response.direct.DirectMessageSender;
import com.mopl.api.domain.conversation.dto.response.direct.DirectMessageWith;
import com.mopl.api.domain.conversation.entity.DirectMessage;
import com.mopl.api.domain.user.entity.User;
import java.util.List;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface DirectMessageMapper {

    @Mapping(target = "conversationId", source = "conversation.id")
    @Mapping(target = "sender", source = "sender", qualifiedByName = "mapSender")
    @Mapping(target = "receiver", source = "receiver", qualifiedByName = "mapReceiver")
    DirectMessageDto toDto(DirectMessage message);

    @Mapping(target = "conversationId", source = "conversation.id")
    @Mapping(target = "sender", source = "sender", qualifiedByName = "mapSender")
    @Mapping(target = "receiver", source = "receiver", qualifiedByName = "mapReceiver")
    DirectMessageLastestMessage toLatestMessageDto(DirectMessage message);


    @Mapping(target = "userId", source = "id")
    DirectMessageWith toWithDto(User user);

    @Named("mapSender")
    default DirectMessageSender mapSender(User user) {
        if (user == null) return null; // Null 체크 추가 권장
        return DirectMessageSender.builder()
                                  .userId(user.getId())
                                  .name(user.getName())
                                  .profileImageUrl(user.getProfileImageUrl())
                                  .build();
    }

    @Named("mapReceiver")
    default DirectMessageReceiver mapReceiver(User user) {
        if (user == null) return null;
        return DirectMessageReceiver.builder()
                                    .userId(user.getId())
                                    .name(user.getName())
                                    .profileImageUrl(user.getProfileImageUrl())
                                    .build();
    }


    DirectMessageResponseDto toResponseDto(
        List<DirectMessageDto> data,
        String nextCursor,
        UUID nextIdAfter,
        boolean hasNext,
        long totalCount,
        String sortDirection,
        String sortBy
    );
}