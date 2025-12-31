package com.mopl.api.domain.dm.dto.response.conversation;

import java.util.UUID;
import lombok.Builder;

@Builder
public record ConversationDto(
    UUID id,
    ConversationWith with,
    ConversationLastestMessage lastestMessage,
    boolean hasUnread
) {

}
