package com.mopl.api.domain.conversation.service;

import com.mopl.api.domain.conversation.entity.Conversation;
import com.mopl.api.domain.conversation.entity.ConversationParticipant;
import com.mopl.api.domain.conversation.entity.DirectMessage;
import com.mopl.api.domain.conversation.entity.QConversationParticipant;
import com.mopl.api.domain.conversation.repository.ConversationRepository;
import com.mopl.api.domain.conversation.repository.DirectMessageRepository;
import com.mopl.api.domain.dm.dto.response.direct.DirectMessageDto;
import com.mopl.api.domain.dm.dto.response.direct.DirectMessageReceiver;
import com.mopl.api.domain.dm.dto.response.direct.DirectMessageSend;
import com.mopl.api.domain.dm.realtime.ActiveConversationRegistry;
import com.mopl.api.domain.sse.SseEmitterRegistry;
import com.mopl.api.domain.user.entity.User;
import com.mopl.api.domain.user.repository.UserRepository;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class DirectMessageCommandServiceImpl implements DirectMessageCommandService {

    private final JPAQueryFactory queryFactory;
    private final ConversationRepository conversationRepository;
    private final DirectMessageRepository directMessageRepository;
    private final UserRepository userRepository;

    private final ActiveConversationRegistry activeConversationRegistry;
    private final SseEmitterRegistry sseEmitterRegistry;

    private static final QConversationParticipant p = QConversationParticipant.conversationParticipant;
    private static final QConversationParticipant pOther = new QConversationParticipant("p2");


    @Override
    @Transactional
    public DirectMessageDto send(UUID conversationId, UUID senderId, String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("content는 비어있을 수 없습니다.");
        }
        if (content.length() > 1000) {
            throw new IllegalArgumentException("content는 1000자를 초과할 수 없습니다.");
        }

        // 1) 대화방 존재 확인
        Conversation conversation = conversationRepository.findById(conversationId)
                                                          .orElseThrow(() -> new IllegalArgumentException("대화방이 존재하지 않습니다."));

        // 2) sender가 참여자인지 검증
        boolean isMember = queryFactory.selectOne()
                                       .from(p)
                                       .where(p.conversation.id.eq(conversationId).and(p.user.id.eq(senderId)))
                                       .fetchFirst() != null;

        if (!isMember) {
            throw new IllegalStateException("대화방 참여자가 아닙니다.");
        }

        // 3) 상대(1:1) 찾기
        ConversationParticipant otherParticipant = queryFactory
            .selectFrom(pOther)
            .join(pOther.user).fetchJoin()
            .where(pOther.conversation.id.eq(conversationId).and(pOther.user.id.ne(senderId)))
            .fetchFirst();

        if (otherParticipant == null) {
            throw new IllegalStateException("1:1 대화 상대를 찾을 수 없습니다.");
        }

        UUID receiverId = otherParticipant.getUser().getId();

        // 4) 유저 로드 (sender는 fetchJoin으로 최적화하려면 QueryDSL로도 가능하지만,
        //    여기서는 레포지토리 findById 2회가 충분히 빠르고 단순/안전)
        User sender = userRepository.findById(senderId)
                                    .orElseThrow(() -> new IllegalArgumentException("sender 유저가 존재하지 않습니다."));
        User receiver = userRepository.findById(receiverId)
                                      .orElseThrow(() -> new IllegalArgumentException("receiver 유저가 존재하지 않습니다."));

        // 5) DM 저장
        DirectMessage saved = directMessageRepository.save(
            new DirectMessage(conversation, sender, receiver, content)
        );

        // 6) 역정규화 갱신 (같은 트랜잭션에서)
        conversation.updateLastMessage(saved.getContent(), saved.getCreatedAt());

        DirectMessageDto dto = toDirectMessageDto(saved);

        // 7) 비활성 대화면 SSE push
        boolean receiverSubscribed = activeConversationRegistry.isSubscribed(receiverId, conversationId);
        if (!receiverSubscribed) {
            sseEmitterRegistry.send(
                receiverId,
                "direct-messages",
                "dm-" + saved.getId(),
                dto
            );
        }

        return dto;
    }




    private DirectMessageDto toDirectMessageDto(DirectMessage msg) {
        return DirectMessageDto.builder()
                               .id(msg.getId())
                               .conversationId(msg.getConversation().getId())
                               .createdAt(msg.getCreatedAt())
                               .send(DirectMessageSend.builder()
                                                      .userId(msg.getSender().getId())
                                                      .name(msg.getSender().getName())
                                                      .profileImageUrl(msg.getSender().getProfileImageUrl())
                                                      .build())
                               .receiver(DirectMessageReceiver.builder()
                                                              .userId(msg.getReceiver().getId())
                                                              .name(msg.getReceiver().getName())
                                                              .profileImageUrl(msg.getReceiver().getProfileImageUrl())
                                                              .build())
                               .content(msg.getContent())
                               .build();
    }
}
