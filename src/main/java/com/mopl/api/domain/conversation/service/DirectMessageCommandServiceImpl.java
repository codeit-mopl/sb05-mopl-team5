package com.mopl.api.domain.conversation.service;

import com.mopl.api.domain.conversation.dto.request.DirectMessageSendRequest;
import com.mopl.api.domain.conversation.dto.response.direct.DirectMessageDto;
import com.mopl.api.domain.conversation.entity.Conversation;
import com.mopl.api.domain.conversation.entity.ConversationParticipant;
import com.mopl.api.domain.conversation.entity.DirectMessage;
import com.mopl.api.domain.conversation.mapper.DirectMessageMapper;
import com.mopl.api.domain.conversation.realtime.ActiveConversationRegistry;
import com.mopl.api.domain.conversation.repository.ConversationParticipantRepository; // 👈 추가
import com.mopl.api.domain.conversation.repository.ConversationRepository;
import com.mopl.api.domain.conversation.repository.DirectMessageRepository;
import com.mopl.api.domain.sse.SseEmitterRegistry;
import com.mopl.api.domain.user.entity.User;
import com.mopl.api.domain.user.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DirectMessageCommandServiceImpl implements DirectMessageCommandService {

    private final ConversationRepository conversationRepository;
    private final DirectMessageRepository directMessageRepository;
    private final ConversationParticipantRepository conversationParticipantRepository;

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

        // 2. 참여자 검증 (레포지토리 변경: DirectMessageRepo -> ConversationParticipantRepo)
        // existsParticipant -> existsByConversationIdAndUserId 로 변경됨
        if (!conversationParticipantRepository.existsByConversationIdAndUserId(conversationId, senderId)) {
            throw new AccessDeniedException("대화방 참여자가 아닙니다.");
        }

        // 3. 상대방 찾기 (레포지토리 변경 & Optional 처리)
        // findOtherParticipant가 이제 Optional을 반환하므로 .orElseThrow 사용
        ConversationParticipant other = conversationParticipantRepository
            .findOtherParticipant(conversationId, senderId)
            .orElseThrow(() -> new IllegalStateException("대화 상대방을 찾을 수 없습니다."));

        User sender = userRepository.getReferenceById(senderId);
        User receiver = other.getUser();

        // 4. 메시지 저장 (Flush로 시간 확정)
        DirectMessage message = directMessageRepository.saveAndFlush(
            DirectMessage.create(conversation, sender, receiver, request.content())
        );

        // 5. 대화방 메타데이터 갱신
        conversation.updateLastMessage(
            message.getId(),
            request.content(),
            message.getCreatedAt(),
            sender.getId()
        );

        // 6. DTO 변환
        DirectMessageDto dto = mapper.toDto(message);

        // 7. 실시간 알림 (SSE)
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