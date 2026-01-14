package com.mopl.api.domain.conversation.dto.response.conversation;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;

@Builder
public record ConversationLatestMessage(

    UUID id,
    UUID conversationsId,
    LocalDateTime createdAt,
    ConversationSend sender,
    ConversationReceiver receiver,
    String content
) {




}