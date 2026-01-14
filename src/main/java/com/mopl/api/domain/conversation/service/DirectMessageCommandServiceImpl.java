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
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
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
    ) throws AccessDeniedException { // AccessDeniedException throws 명시 필요할 수 있음
        // 1. 대화방 조회
        Conversation conversation = conversationRepository.findById(conversationId)
                                                          .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 대화방입니다."));

        // 2. 참여자 검증
        if (!directMessageRepository.existsParticipant(conversationId, senderId)) {
            throw new AccessDeniedException("대화방 참여자가 아닙니다.");
        }

        // 3. 상대방 찾기
        ConversationParticipant other = directMessageRepository.findOtherParticipant(conversationId, senderId);
        if (other == null) {
            throw new IllegalStateException("대화 상대방을 찾을 수 없습니다.");
        }

        User sender = userRepository.getReferenceById(senderId);
        User receiver = other.getUser();


        DirectMessage message = directMessageRepository.saveAndFlush(
            DirectMessage.create(conversation, sender, receiver, request.content())
        );

        // 7. 대화방 메타데이터 갱신
        // 위에서 flush를 했기 때문에 message.getCreatedAt()이 더 이상 null이 아닙니다.
        conversation.updateLastMessage(
            message.getId(),
            request.content(),
            message.getCreatedAt(), // ✅ 이제 정확한 시간이 들어갑니다.
            sender.getId()
        );


        DirectMessageDto dto = mapper.toDto(message);



        // 9. 실시간 알림 (SSE)
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
