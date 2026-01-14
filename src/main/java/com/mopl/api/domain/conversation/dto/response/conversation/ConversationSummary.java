package com.mopl.api.domain.conversation.dto.response.conversation;

import java.time.LocalDateTime;
import java.util.UUID;

public interface ConversationSummary {
    UUID getConversationId();
    UUID getOtherUserId();
    String getOtherName();
    String getOtherProfileImageUrl();

    UUID getLastMessageId();
    String getLastMessageContent();
    LocalDateTime getLastMessageCreatedAt();

    Long getUnreadCount();
}