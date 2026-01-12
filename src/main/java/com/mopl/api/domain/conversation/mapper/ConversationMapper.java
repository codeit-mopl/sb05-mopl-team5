package com.mopl.api.domain.conversation.mapper;

import com.mopl.api.domain.conversation.dto.response.conversation.ConversationDto;
import com.mopl.api.domain.conversation.dto.response.conversation.ConversationLatestMessage;
import com.mopl.api.domain.conversation.dto.response.conversation.ConversationListRow;
import com.mopl.api.domain.conversation.dto.response.conversation.ConversationReceiver;
import com.mopl.api.domain.conversation.dto.response.conversation.ConversationSend;
import com.mopl.api.domain.conversation.dto.response.conversation.ConversationWith;
import java.time.LocalDateTime;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ConversationMapper {

    @Mapping(target = "id", source = "conversationId")
    @Mapping(target = "with", expression = "java(mapWith(row))")
    @Mapping(target = "latestMessage", expression = "java(mapLatestMessage(row))")
    @Mapping(target = "hasUnread", expression = "java(hasUnread(row))")
    ConversationDto toDto(ConversationListRow row);

    default ConversationWith mapWith(ConversationListRow row) {
        if (row == null) return null;
        return ConversationWith.builder()
                               .userId(row.otherUserId())
                               .name(row.otherName())
                               .profileImageUrl(row.otherProfileImageUrl())
                               .build();
    }

    /**
     * 목록에서는 sender/receiver를 성능상 null/빈값으로 내려주고 있음(기존 Service 구현 그대로 유지)
     */
    default ConversationLatestMessage mapLatestMessage(ConversationListRow row) {
        if (row == null) return null;
        if (row.lastMessageCreatedAt() == null && row.lastMessageContent() == null) return null;

        // 기존 서비스가 하던 방식(목록에서는 sender/receiver "빈 객체" 유지)을 그대로 반영
        ConversationSend sender = ConversationSend.builder()
                                                  .userId(null)
                                                  .name("")
                                                  .profileImageUrl(null)
                                                  .build();

        ConversationReceiver receiver = ConversationReceiver.builder()
                                                            .userId(null)
                                                            .name("")
                                                            .profileImageUrl(null)
                                                            .build();

        return ConversationLatestMessage.builder()
                                        .id(null)
                                        .conversationsId(row.conversationId())
                                        .createdAt(row.lastMessageCreatedAt())
                                        .sender(sender)
                                        .receiver(receiver)
                                        .content(row.lastMessageContent())
                                        .build();
    }

    default boolean hasUnread(ConversationListRow row) {
        if (row == null) return false;

        LocalDateTime lastMsgAt = row.lastMessageCreatedAt();
        LocalDateTime myLastReadAt = row.myLastReadAt();

        if (lastMsgAt == null) return false;
        if (myLastReadAt == null) return true;
        return myLastReadAt.isBefore(lastMsgAt);
    }
}
