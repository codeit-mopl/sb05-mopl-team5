package com.mopl.api.domain.conversation.dto.response.direct;


import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;

@Builder
public record DirectMessageDto(

    UUID id,
    UUID conversationId,
    LocalDateTime createdAt,
    DirectMessageSend send,
    DirectMessageReceiver receiver,
    String content

) {

}