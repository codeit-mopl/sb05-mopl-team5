package com.mopl.api.domain.dm.service;

import com.mopl.api.domain.dm.dto.request.ConversationRequestDto;
import com.mopl.api.domain.dm.dto.response.conversation.ConversationDto;
import com.mopl.api.domain.dm.dto.response.conversation.ConversationResponseDto;
import com.mopl.api.domain.dm.dto.response.direct.DirectMessageResponseDto;
import com.mopl.api.domain.dm.dto.response.direct.DirectMessageWithDto;
import java.util.UUID;

public interface ConversationService {

    ConversationDto createConversation(ConversationRequestDto withUserId);

    ConversationResponseDto getConversationList(
        String keywordLike,
        String cursor,
        UUID idAfter,
        int limit,
        String sortDirection,
        String sortBy
    );

    void conversationRead(UUID conversationId, UUID directMessageId);

    ConversationDto conversationCheck(UUID conversationId);

    DirectMessageResponseDto getDirectMessageList(
        UUID conversationId,
        String cursor,
        UUID idAfter,
        int limit,
        String sortDirection,
        String sortBy
    );

    DirectMessageWithDto getDirectMessageWith(UUID userId);
}