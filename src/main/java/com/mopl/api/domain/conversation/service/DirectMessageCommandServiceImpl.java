package com.mopl.api.domain.conversation.service;

import com.mopl.api.domain.conversation.dto.request.DirectMessageSendRequest;
import com.mopl.api.domain.conversation.dto.response.direct.DirectMessageDto;
import com.mopl.api.domain.conversation.entity.Conversation;
import com.mopl.api.domain.conversation.entity.ConversationParticipant;
import com.mopl.api.domain.conversation.entity.DirectMessage;
import com.mopl.api.domain.conversation.mapper.DirectMessageMapper;
import com.mopl.api.domain.conversation.realtime.ActiveConversationRegistry;
import com.mopl.api.domain.conversation.repository.ConversationParticipantRepository;
import com.mopl.api.domain.conversation.repository.ConversationRepository;
import com.mopl.api.domain.conversation.repository.DirectMessageRepository;
import com.mopl.api.domain.sse.SseEmitterRegistry;
import com.mopl.api.domain.user.entity.User;
import com.mopl.api.domain.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class DirectMessageCommandServiceImpl implements DirectMessageCommandService {

    private final ConversationRepository conversationRepository;
    private final DirectMessageRepository directMessageRepository;
    private final UserRepository userRepository;
    private final DirectMessageMapper mapper;
    private final ActiveConversationRegistry activeConversationRegistry;
    private final SseEmitterRegistry sseEmitterRegistry;

    @Override
    public DirectMessageDto send(
        UUID conversationId,
        UUID senderId,
        DirectMessageSendRequest request
    ) {
        Conversation conversation = conversationRepository.findById(conversationId)
                                                          .orElseThrow(() -> new IllegalArgumentException("대화방 없음"));

        if (!directMessageRepository.existsParticipant(conversationId, senderId)) {
            throw new IllegalStateException("참가자 아님");
        }

        ConversationParticipant other =
            directMessageRepository.findOtherParticipant(conversationId, senderId);

        User sender = userRepository.getReferenceById(senderId);
        User receiver = other.getUser();

        DirectMessage message = directMessageRepository.save(
            new DirectMessage(conversation, sender, receiver, request.content())
        );

        conversation.updateLastMessage( message.getId(),
            request.content(),
             sender.getCreatedAt() , sender.getId()
        );
        conversationRepository.save(conversation);

        DirectMessageDto dto = mapper.toDto(message);

        // 🔥 비활성 대화 → SSE
        if (!activeConversationRegistry.isSubscribed(receiver.getId(), conversationId)) {
            sseEmitterRegistry.send(
                receiver.getId(),
                "direct-messages",
                "dm-" + message.getId(),
                dto
            );
        }

        return dto;
    }
}

