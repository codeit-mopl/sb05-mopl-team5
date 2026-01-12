package com.mopl.api.domain.conversation.service;

import com.mopl.api.domain.conversation.dto.request.ConversationRequestDto;
import com.mopl.api.domain.conversation.dto.response.conversation.ConversationDto;
import com.mopl.api.domain.conversation.dto.response.conversation.ConversationResponseDto;
import com.mopl.api.domain.conversation.dto.response.direct.DirectMessageResponseDto;
import com.mopl.api.domain.conversation.dto.response.direct.DirectMessageWithDto;
import java.util.UUID;

public interface ConversationService {

    // 1:1 대화 생성/찾기
    ConversationDto createConversation(UUID me, ConversationRequestDto request);


    ConversationResponseDto getConversationList(
        UUID me,
        String keywordLike,
        String cursor,
        UUID idAfter,
        int limit,
        String sortDirection,
        String sortBy
    );


    ConversationDto conversationCheck(UUID me, UUID conversationId);

    // 읽음 처리
    void conversationRead(UUID me, UUID conversationId, UUID directMessageId);


    DirectMessageResponseDto getDirectMessageList(
        UUID me,
        UUID conversationId,
        String cursor,
        UUID idAfter,
        int limit,
        String sortDirection,
        String sortBy
    );


    DirectMessageWithDto getDirectMessageWith(UUID me, UUID userId);
}
