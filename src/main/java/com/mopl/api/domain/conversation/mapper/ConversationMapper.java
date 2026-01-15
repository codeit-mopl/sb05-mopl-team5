package com.mopl.api.domain.conversation.mapper;

import com.mopl.api.domain.conversation.dto.response.conversation.*;
import com.mopl.api.domain.conversation.entity.Conversation;
import com.mopl.api.domain.conversation.entity.DirectMessage;
import com.mopl.api.domain.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;
import java.util.UUID;

@Mapper(
    componentModel = "spring",
    imports = {
        ConversationWith.class,
        ConversationLatestMessage.class,
        ConversationSend.class,
        ConversationReceiver.class,
        UUID.class
    }
)
public interface ConversationMapper {

    // ========================================================================
    // 1. 목록 조회용 (Row -> DTO)
    // ========================================================================
    @Mapping(target = "id", source = "conversationId")
    @Mapping(target = "with", expression = "java(mapWith(row))")
    @Mapping(target = "lastestMessage", expression = "java(mapLatestMessage(row))")
    // Record의 unreadCount가 null일 수 있으므로 null safe 처리
    @Mapping(target = "hasUnread", expression = "java(row.unreadCount() != null && row.unreadCount() > 0)")
    ConversationDto toDto(ConversationListRow row);


    // ========================================================================
    // 2. 단건 조회/생성용 (Entity -> DTO)
    // ========================================================================
    @Mapping(target = "id", source = "conversation.id")
    @Mapping(target = "with", source = "otherUser", qualifiedByName = "mapWithUser")
    @Mapping(target = "lastestMessage", source = "lastMessage", qualifiedByName = "mapLatestMessageFromEntity")
    @Mapping(target = "hasUnread", source = "hasUnread")
    ConversationDto toCheckDto(Conversation conversation, User otherUser, DirectMessage lastMessage, boolean hasUnread);

    @Mapping(target = "id", source = "conversation.id")
    @Mapping(target = "with", source = "otherUser", qualifiedByName = "mapWithUser")
    @Mapping(target = "lastestMessage", expression = "java(null)")
    @Mapping(target = "hasUnread", constant = "false")
    ConversationDto toEmptyDto(Conversation conversation, User otherUser);


    // ========================================================================
    // 3. Response DTO 생성 (수정된 Record 필드 반영)
    // ========================================================================
    default ConversationResponseDto toResponseDto(
        List<ConversationDto> data, // 파라미터 이름
        String nextCursor,
        UUID nextIdAfter,
        boolean hasNext,
        Long totalCount,
        String sortBy,
        String sortDirection
    ) {
        return ConversationResponseDto.builder()
                                      .data(data) // Record 필드명: data
                                      .nextCursor(nextCursor)
                                      .nextIdAfter(nextIdAfter)
                                      .hasNext(hasNext)
                                      .totalCount(totalCount)
                                      .sortBy(sortBy)
                                      .sortDirection(sortDirection)
                                      .build();
    }

    // ========================================================================
    // 💡 Helper Methods (목록 조회용 - Row 처리)
    // ========================================================================

    default ConversationWith mapWith(ConversationListRow row) {
        if (row == null) return null;
        return ConversationWith.builder()
                               .userId(row.otherUserId())
                               .name(row.otherName())
                               .profileImageUrl(row.otherProfileImageUrl())
                               .build();
    }

    default ConversationLatestMessage mapLatestMessage(ConversationListRow row) {
        if (row == null || row.lastMessageId() == null) return null;

        // 목록 조회 성능을 위해 Sender 정보는 최소화
        return ConversationLatestMessage.builder()
                                        .id(row.lastMessageId())
                                        .conversationId(row.conversationId())
                                        .content(row.lastMessageContent())
                                        .createdAt(row.lastMessageCreatedAt())
                                        // .sender(...) // 필요시 추가
                                        .build();
    }

    // ========================================================================
    // 💡 Helper Methods (단건 조회용 - Entity 처리)
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

        return ConversationLatestMessage.builder()
                                        .id(message.getId())
                                        .conversationId(message.getConversation().getId())
                                        .content(message.getContent())
                                        .createdAt(message.getCreatedAt())
                                        .sender(ConversationSend.builder()
                                                                .userId(message.getSender().getId())
                                                                .name(message.getSender().getName())
                                                                .profileImageUrl(message.getSender().getProfileImageUrl())
                                                                .build())
                                        .receiver(ConversationReceiver.builder()
                                                                      .userId(message.getReceiver().getId())
                                                                      .name(message.getReceiver().getName())
                                                                      .profileImageUrl(message.getReceiver().getProfileImageUrl())
                                                                      .build())
                                        .build();
    }
}