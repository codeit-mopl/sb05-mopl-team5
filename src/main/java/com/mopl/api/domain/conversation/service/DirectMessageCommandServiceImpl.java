package com.mopl.api.domain.conversation.service;

import com.mopl.api.domain.conversation.dto.request.DirectMessageSendRequest;
import com.mopl.api.domain.conversation.dto.response.direct.DirectMessageDto;
import com.mopl.api.domain.conversation.entity.Conversation;
import com.mopl.api.domain.conversation.entity.ConversationParticipant;
import com.mopl.api.domain.conversation.entity.DirectMessage;
import com.mopl.api.domain.conversation.mapper.DirectMessageMapper;
import com.mopl.api.domain.conversation.realtime.ActiveConversationRegistry;
import com.mopl.api.domain.conversation.repository.ConversationRepository;
import com.mopl.api.domain.conversation.repository.DirectMessageRepository;
import com.mopl.api.domain.sse.SseEmitterRegistry;
import com.mopl.api.domain.user.entity.User;
import com.mopl.api.domain.user.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
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
    ) throws AccessDeniedException {
        // 1. 대화방 조회
        Conversation conversation = conversationRepository.findById(conversationId)
                                                          .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 대화방입니다."));

        // 2. 참여자 검증 (Repository 최적화 가정)
        // (참여자가 아니면 예외 발생)
        if (!directMessageRepository.existsParticipant(conversationId, senderId)) {
            throw new AccessDeniedException("대화방 참여자가 아닙니다.");
        }

        // 3. 상대방 찾기 (1:1 대화 가정)
        ConversationParticipant other = directMessageRepository.findOtherParticipant(conversationId, senderId);
        if (other == null) {
            throw new IllegalStateException("대화 상대방을 찾을 수 없습니다.");
        }


        User sender = userRepository.getReferenceById(senderId);
        User receiver = other.getUser();



        // 6. 메시지 생성 및 저장
        // (Entity에 create 메서드를 만들거나, 생성자에 now를 넘기는 것을 추천)
        DirectMessage message = directMessageRepository.save(
            DirectMessage.create(conversation, sender, receiver, request.content())
        );


        conversation.updateLastMessage(
            message.getId(),
            request.content(),
            message.getCreatedAt(),
            sender.getId()
        );


        // 8. DTO 변환
        DirectMessageDto dto = mapper.toDto(message);

        // 9. 실시간 알림 (SSE) - 보고 있지 않은 유저에게만 전송
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
