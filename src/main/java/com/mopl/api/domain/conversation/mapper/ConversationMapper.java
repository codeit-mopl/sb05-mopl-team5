package com.mopl.api.domain.conversation.mapper;

import com.mopl.api.domain.conversation.dto.response.conversation.ConversationDto;
import com.mopl.api.domain.conversation.dto.response.conversation.ConversationLatestMessage;
import com.mopl.api.domain.conversation.dto.response.conversation.ConversationListRow;
import com.mopl.api.domain.conversation.dto.response.conversation.ConversationReceiver;
import com.mopl.api.domain.conversation.dto.response.conversation.ConversationResponseDto;
import com.mopl.api.domain.conversation.dto.response.conversation.ConversationSend;
import com.mopl.api.domain.conversation.dto.response.conversation.ConversationWith;
import com.mopl.api.domain.conversation.entity.Conversation;
import com.mopl.api.domain.conversation.entity.DirectMessage;
import com.mopl.api.domain.user.entity.User;

import java.util.List;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface ConversationMapper {

    // 1. 목록 조회용 (Row -> DTO)
    @Mapping(target = "id", source = "conversationId")
    @Mapping(target = "with", expression = "java(mapWith(row))")
    @Mapping(target = "lastestMessage", expression = "java(mapLatestMessage(row))")
    @Mapping(target = "hasUnread", expression = "java(hasUnread(row))")
    ConversationDto toDto(ConversationListRow row);

    // 2. 단건 조회/생성용 (Entity -> DTO)
    @Mapping(target = "id", source = "conversation.id")
    @Mapping(target = "with", source = "otherUser", qualifiedByName = "mapWithUser")
    @Mapping(target = "lastestMessage", source = "lastMessage", qualifiedByName = "mapLatestMessageFromEntity")
    @Mapping(target = "hasUnread", source = "hasUnread")
    ConversationDto toCheckDto(Conversation conversation, User otherUser, DirectMessage lastMessage, boolean hasUnread);


    @Mapping(target = "id", source = "conversation.id")
    @Mapping(target = "with", source = "otherUser", qualifiedByName = "mapWithUser")
    @Mapping(target = "lastestMessage", expression = "java(null)") // 메시지 없음
    @Mapping(target = "hasUnread", constant = "false") // 안 읽음 없음
    ConversationDto toEmptyDto(Conversation conversation, User otherUser);


    ConversationResponseDto toResponseDto(
        List<ConversationDto> data,
        String nextCursor,
        UUID nextIdAfter,
        boolean hasNext,
        long totalCount,
        String sortBy,
        String sortDirection
    );
    // ========================================================================
    // 💡 Helper Methods (목록 조회용)
    // ========================================================================

    default ConversationWith mapWith(ConversationListRow row) {
        if (row == null) return null;
        return ConversationWith.builder()
                               .userId(row.otherUserId())
                               .name(row.otherName())
                               .profileImageUrl(row.otherProfileImageUrl())
                               .build();
    }

    default boolean hasUnread(ConversationListRow row) {
        if (row == null) return false;
        return row.myLastReadAt() == null ||
            (row.lastMessageCreatedAt() != null && row.lastMessageCreatedAt().isAfter(row.myLastReadAt()));
    }

    default ConversationLatestMessage mapLatestMessage(ConversationListRow row) {
        if (row == null) return null;
        if (row.lastMessageCreatedAt() == null && row.lastMessageContent() == null) return null;

        ConversationSend sender = ConversationSend.builder()
                                                  .userId(row.lastMessageSenderId())
                                                  .name(row.lastMessageSenderName())
                                                  .profileImageUrl(row.lastMessageSenderProfileImageUrl())
                                                  .build();

        ConversationReceiver receiver = ConversationReceiver.builder()
                                                            .userId(row.otherUserId())
                                                            .name(row.otherName())
                                                            .profileImageUrl(row.otherProfileImageUrl())
                                                            .build();

        return ConversationLatestMessage.builder()
                                        .id(row.lastMessageId())
                                        .conversationId(row.conversationId())
                                        .createdAt(row.lastMessageCreatedAt())
                                        .sender(sender)
                                        .receiver(receiver)
                                        .content(row.lastMessageContent())
                                        .build();
    }

    // ========================================================================
    // 💡 Helper Methods (단건 조회용 - @Named 필수)
    // ========================================================================

    @Named("mapWithUser")
    default ConversationWith mapWithUser(User user) {
        if (user == null) return null;
        return ConversationWith.builder()
                               .userId(user.getId())
                               .name(user.getName())
                               .profileImageUrl(user.getProfileImageUrl())
                               .build();
    }

    @Named("mapLatestMessageFromEntity")
    default ConversationLatestMessage mapLatestMessageFromEntity(DirectMessage message) {
        if (message == null) return null;

        ConversationSend sender = ConversationSend.builder()
                                                  .userId(message.getSender().getId())
                                                  .name(message.getSender().getName())
                                                  .profileImageUrl(message.getSender().getProfileImageUrl())
                                                  .build();

        ConversationReceiver receiver = ConversationReceiver.builder()
                                                            .userId(message.getReceiver().getId())
                                                            .name(message.getReceiver().getName())
                                                            .profileImageUrl(message.getReceiver().getProfileImageUrl())
                                                            .build();

        return ConversationLatestMessage.builder()
                                        .id(message.getId())
                                        .conversationId(message.getConversation().getId())
                                        .content(message.getContent())
                                        .createdAt(message.getCreatedAt())
                                        .sender(sender)
                                        .receiver(receiver)
                                        .build();
    }
}