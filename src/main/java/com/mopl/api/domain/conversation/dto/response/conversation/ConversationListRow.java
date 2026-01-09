package com.mopl.api.domain.conversation.dto.response.conversation;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * ✅ Projection 전용 Row record (DB에서 필요한 값만 가져오게 해서 성능/메모리 최적)
 */
public record ConversationListRow(
    UUID conversationId,
    UUID otherUserId,
    String otherName,
    String otherProfileImageUrl,
    String lastMessageContent,
    LocalDateTime lastMessageCreatedAt,
    LocalDateTime myLastReadAt
) {

}