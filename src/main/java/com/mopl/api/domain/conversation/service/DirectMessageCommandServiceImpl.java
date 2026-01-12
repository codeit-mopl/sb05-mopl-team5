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

    private final DirectMessageMapper directMessageMapper;

    private final SimpMessagingTemplate messagingTemplate;
    private final ActiveConversationRegistry activeConversationRegistry;
    private final SseEmitterRegistry sseEmitterRegistry;

    @Override
    public DirectMessageDto send(UUID conversationId, UUID senderId, DirectMessageSendRequest request) {

        String content = request.content();
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("content는 필수입니다.");
        }

        Conversation conversation = conversationRepository.findById(conversationId)
                                                          .orElseThrow(() -> new IllegalArgumentException("대화방이 존재하지 않습니다."));

        // sender가 이 대화방 참가자인지
        if (!directMessageRepository.existsParticipant(conversationId, senderId)) {
            throw new IllegalStateException("대화방 참가자가 아닙니다.");
        }

        // 1:1 상대방 찾기
        ConversationParticipant other = directMessageRepository.findOtherParticipant(conversationId, senderId);
        if (other == null) {
            throw new IllegalStateException("1:1 대화 상대를 찾을 수 없습니다.");
        }

        User sender = userRepository.findById(senderId)
                                    .orElseThrow(() -> new IllegalArgumentException("발신자 유저가 존재하지 않습니다."));

        User receiver = other.getUser();

        DirectMessage saved = directMessageRepository.save(
            new DirectMessage(conversation, sender, receiver, content)
        );

        // 역정규화 갱신 (너가 Conversation에 추가한 메서드)
        conversation.updateLastMessage(content, saved.getCreatedAt());

        DirectMessageDto dto = directMessageMapper.toDto(saved);

        // WS publish: 구독자들은 여기서 받음
        messagingTemplate.convertAndSend(
            "/sub/conversations/" + conversationId + "/direct-messages",
            dto
        );

        // 비활성(상대가 현재 그 대화를 보고 있지 않으면) SSE로도 알림
        UUID receiverId = receiver.getId();
        if (!activeConversationRegistry.isSubscribed(receiverId, conversationId)) {
            sseEmitterRegistry.send(receiverId, "direct-messages", "dm-" + saved.getId(), dto);
        }

        return dto;
    }
}
