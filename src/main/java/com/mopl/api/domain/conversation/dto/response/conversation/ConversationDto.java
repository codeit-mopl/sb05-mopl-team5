package com.mopl.api.domain.conversation.dto.response.conversation;

import java.util.UUID;
import lombok.Builder;

@Builder
public record ConversationDto(
    UUID id,
    ConversationWith with,
    ConversationLatestMessage latestMessage,
    boolean hasUnread
) {

}