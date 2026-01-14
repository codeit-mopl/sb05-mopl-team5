package com.mopl.api.domain.conversation.mapper;

import com.mopl.api.domain.conversation.dto.response.conversation.*;
import org.springframework.stereotype.Component;

@Component
public class ConversationConverter {

    public ConversationDto toDto(ConversationSummary summary) {
        if (summary == null) {
            return null;
        }

        // 1. 상대방 정보 매핑
        ConversationWith with = ConversationWith.builder()
                                                .userId(summary.getOtherUserId())
                                                .name(summary.getOtherName())
                                                .profileImageUrl(summary.getOtherProfileImageUrl())
                                                .build();

        // 2. 마지막 메시지 매핑
        ConversationLatestMessage latestMessage = null;

        if (summary.getLastMessageId() != null) {


            ConversationSend sender = ConversationSend.builder()

                                                      .build();

            ConversationReceiver receiver = ConversationReceiver.builder()

                                                                .build();

            latestMessage = ConversationLatestMessage.builder()
                                                     .id(summary.getLastMessageId())
                                                     .conversationId(summary.getConversationId())
                                                     .content(summary.getLastMessageContent())
                                                     .createdAt(summary.getLastMessageCreatedAt())
                                                     .sender(sender)
                                                     .receiver(receiver)
                                                     .build();
        }

        // 3. 안 읽은 메시지 여부 (Null Safe 처리)
        boolean hasUnread = summary.getUnreadCount() != null && summary.getUnreadCount() > 0;

        return ConversationDto.builder()
                              .id(summary.getConversationId())
                              .with(with)
                              .lastestMessage(latestMessage)
                              .hasUnread(hasUnread)
                              .build();
    }
}