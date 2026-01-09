package com.mopl.api.domain.conversation.dto.response.conversation;

import java.util.UUID;
import lombok.Builder;

@Builder
public record ConversationSend(
    UUID userId,
    String name,
    String profileImageUrl
){

}