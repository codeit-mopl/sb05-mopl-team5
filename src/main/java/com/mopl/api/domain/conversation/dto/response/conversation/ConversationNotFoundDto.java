package com.mopl.api.domain.conversation.dto.response.conversation;

import lombok.Builder;

@Builder
public record ConversationNotFoundDto(
    String exceptionName,
    String message,
    withUserIdDetail details

) {

}


