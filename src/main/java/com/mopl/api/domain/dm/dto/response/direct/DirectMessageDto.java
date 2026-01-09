package com.mopl.api.domain.dm.dto.response.direct;


import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;

@Builder
public record DirectMessageDto(

    UUID id,
    UUID conversationId,
    LocalDateTime createdAt,
    DirectMessagesender sender,
    DirectMessageReceiver receiver,
    String content

) {

}