package com.mopl.api.domain.conversation.dto.response.conversation;

import java.time.LocalDateTime;
import java.util.UUID;

public record ConversationListRow(
    UUID conversationId,
    UUID otherUserId,
    String otherName,
    String otherProfileImageUrl,

    UUID lastMessageId,
    String lastMessageContent,
    LocalDateTime lastMessageCreatedAt,

    Long unreadCount
) {
}