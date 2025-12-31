package com.mopl.api.domain.dm.service;


import com.mopl.api.domain.dm.dto.request.ConversationRequestDto;
import com.mopl.api.domain.dm.dto.response.conversation.ConversationDto;
import com.mopl.api.domain.dm.dto.response.conversation.ConversationResponseDto;
import com.mopl.api.domain.dm.dto.response.direct.DirectMessageResponseDto;
import com.mopl.api.domain.dm.dto.response.direct.DirectMessageWithDto;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ConversationServiceImpl implements ConversationService {

    @Override
    public ConversationDto createConversation(ConversationRequestDto withUserId) {
        return null;
    }

    @Override
    public ConversationResponseDto getConversationList(String keywordLike, String cursor, UUID String, int limit,
        String sortDirection, String sortBy) {
        return null;
    }

    @Override
    public void conversationRead(UUID conversationId, UUID directMessageId) {

    }

    @Override
    public ConversationDto conversationCheck(UUID conversationId) {
        return null;
    }

    @Override
    public DirectMessageResponseDto getDirectMessageList(UUID conversationId, String cursor, UUID idAfter, int limit,
        String sortDirection, String sortBy) {
        return null;
    }

    @Override
    public DirectMessageWithDto getDirectMessageWith(UUID userId) {
        return null;
    }
}
