package com.mopl.api.domain.notification.dto.event;

import com.mopl.api.domain.conversation.dto.response.direct.DirectMessageDto;
import java.util.UUID;
import lombok.Builder;

@Builder
public record DmReceivedEvent(
    UUID conversationId,
    UUID receiverId,
    UUID senderId,
    String senderName,
    String content,
    DirectMessageDto directMessageDto
) {

}