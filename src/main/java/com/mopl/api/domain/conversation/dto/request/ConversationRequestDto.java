package com.mopl.api.domain.conversation.dto.request;


import java.util.UUID;
import lombok.Builder;

@Builder
public record ConversationRequestDto(UUID withUserId) {

}
