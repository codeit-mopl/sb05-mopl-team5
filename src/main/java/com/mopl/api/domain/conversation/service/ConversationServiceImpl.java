package com.mopl.api.domain.conversation.service;

import com.mopl.api.domain.conversation.dto.request.ConversationRequestDto;
import com.mopl.api.domain.conversation.dto.response.conversation.ConversationDto;
import com.mopl.api.domain.conversation.dto.response.conversation.ConversationLatestMessage;
import com.mopl.api.domain.conversation.dto.response.conversation.ConversationListRow;
import com.mopl.api.domain.conversation.dto.response.conversation.ConversationReceiver;
import com.mopl.api.domain.conversation.dto.response.conversation.ConversationResponseDto;
import com.mopl.api.domain.conversation.dto.response.conversation.ConversationSend;
import com.mopl.api.domain.conversation.dto.response.conversation.ConversationWith;
import com.mopl.api.domain.conversation.dto.response.direct.DirectMessageDto;
import com.mopl.api.domain.conversation.dto.response.direct.DirectMessageLastestMessage;
import com.mopl.api.domain.conversation.dto.response.direct.DirectMessageReceiver;
import com.mopl.api.domain.conversation.dto.response.direct.DirectMessageResponseDto;
import com.mopl.api.domain.conversation.dto.response.direct.DirectMessageSender;
import com.mopl.api.domain.conversation.dto.response.direct.DirectMessageWith;
import com.mopl.api.domain.conversation.dto.response.direct.DirectMessageWithDto;
import com.mopl.api.domain.conversation.entity.Conversation;
import com.mopl.api.domain.conversation.entity.ConversationParticipant;
import com.mopl.api.domain.conversation.entity.DirectMessage;
import com.mopl.api.domain.conversation.mapper.ConversationMapper;
import com.mopl.api.domain.conversation.mapper.DirectMessageMapper;
import com.mopl.api.domain.conversation.repository.ConversationParticipantRepository;
import com.mopl.api.domain.conversation.repository.ConversationRepository;
import com.mopl.api.domain.conversation.repository.DirectMessageRepository;
import com.mopl.api.domain.user.entity.User;
import com.mopl.api.domain.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ConversationServiceImpl implements ConversationService {

    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository conversationParticipantRepository;
    private final DirectMessageRepository directMessageRepository;
    private final UserRepository userRepository;

    private final ConversationMapper conversationMapper;
    private final DirectMessageMapper directMessageMapper;

    // -------------------------
    // 1) 1:1 대화방 생성 (없으면 생성, 있으면 existing 반환)
    // -------------------------
    @Override
    @Transactional
    public ConversationDto createConversation(UUID me, ConversationRequestDto withUserId) {
        UUID other = withUserId.withUserId();

        if (other == null) {
            throw new IllegalArgumentException("withUserId는 필수입니다.");
        }
        if (me.equals(other)) {
            throw new IllegalArgumentException("자기 자신과 대화할 수 없습니다.");
        }

        UUID conversationId = conversationRepository.findOneToOneConversationId(Set.of(me, other))
                                                    .orElse(null);



        if (conversationId == null) {
            // 성능: me는 proxy로 충분, other는 존재 검증이 필요하므로 findById
            User meUser = userRepository.getReferenceById(me);
            User otherUser = userRepository.findById(other)
                                           .orElseThrow(() -> new IllegalArgumentException("상대 유저가 존재하지 않습니다."));

            Conversation newConversation = conversationRepository.save(Conversation.create());

            // lastReadAt 기본값은 엔티티 생성자/빌더에서 now로 세팅되도록 되어있다면 OK
            conversationParticipantRepository.save(new ConversationParticipant(newConversation, meUser));
            conversationParticipantRepository.save(new ConversationParticipant(newConversation, otherUser));

            return buildEmptyConversationDto(newConversation, otherUser);
        }

        // 방이 있으면 해당 방 정보 반환
        return conversationCheck(me, conversationId);
    }

    // -------------------------
    // 2) 대화 목록 조회 (repo custom 최적쿼리 사용)
    // -------------------------
    @Override
    public ConversationResponseDto getConversationList(
        UUID me,
        String keywordLike,
        String cursor,
        UUID idAfter,
        int limit,
        String sortDirection,
        String sortBy
    ) {
        LocalDateTime cursorTime = parseCursor(cursor);

        List<ConversationListRow> rows = conversationRepository.findConversationList(
            me,
            keywordLike,
            cursorTime,
            idAfter,
            limit,
            sortDirection
        );

        long totalCount = conversationRepository.countConversationList(me, keywordLike);

        boolean hasNext = rows.size() > limit;
        if (hasNext) {
            rows = rows.subList(0, limit);
        }

        List<ConversationDto> data = rows.stream()
                                         .map(conversationMapper::toDto)
                                         .toList();

        String nextCursor = null;
        UUID nextIdAfter = null;

        if (hasNext && !rows.isEmpty()) {
            ConversationListRow lastRow = rows.get(rows.size() - 1);
            if (lastRow.lastMessageCreatedAt() != null) {
                nextCursor = lastRow.lastMessageCreatedAt().toString();
            }
            nextIdAfter = lastRow.conversationId();
        }

        return ConversationResponseDto.builder()
                                      .data(data)
                                      .nextCursor(nextCursor)
                                      .nextIdAfter(nextIdAfter)
                                      .hasNext(hasNext)
                                      .totalCount(totalCount)
                                      .sortBy(sortBy)
                                      .sortDirection(sortDirection)
                                      .build();
    }

    // -------------------------
    // 3) 읽음 처리 (DB에서 메시지 createdAt만 가져와 조건 업데이트)
    // -------------------------
    @Override
    @Transactional
    public void conversationRead(UUID userId, UUID conversationId, UUID directMessageId) {

        ensureParticipant(conversationId, userId);

        LocalDateTime messageCreatedAt = directMessageRepository
            .findCreatedAtByIdAndConversationId(directMessageId, conversationId)
            .orElseThrow(() -> new IllegalArgumentException("메시지를 찾을 수 없습니다."));

        // 성능: "현재 lastReadAt보다 더 최신일 때만 갱신"을 DB에서 처리
        conversationParticipantRepository.updateLastReadAtIfNewer( conversationId, userId,messageCreatedAt);
    }

    // -------------------------
    // 4) 대화방 단건 조회 (권한 + 최신 메시지 + hasUnread)
    // -------------------------
    @Override
    public ConversationDto conversationCheck(UUID me, UUID conversationId) {

        // 존재 + 내가 참가자인지까지 여기서 처리하는 게 안전
        Conversation conversation = conversationRepository.findById(conversationId)
                                                          .orElseThrow(() -> new IllegalStateException("대화방에 참여하지 않았거나 존재하지 않습니다."));

        List<ConversationParticipant> participants =
            conversationParticipantRepository.findAllByConversationId(conversationId);

        ConversationParticipant myParticipant = participants.stream()
                                                            .filter(p -> p.getUser().getId().equals(me))
                                                            .findFirst()
                                                            .orElseThrow(() -> new AccessDeniedException("대화방 참여자가 아닙니다."));

        User otherUser = participants.stream()
                                     .map(ConversationParticipant::getUser)
                                     .filter(u -> !u.getId().equals(me))
                                     .findFirst()
                                     .orElseThrow(() -> new IllegalStateException("대화 상대방을 찾을 수 없습니다."));

        DirectMessage lastMessage = directMessageRepository.findLatestByConversationId(conversationId)
                                                           .orElse(null);

        boolean hasUnread = false;
        if (lastMessage != null) {
            LocalDateTime lastReadAt = myParticipant.getLastReadAt();
            hasUnread = lastReadAt == null || lastReadAt.isBefore(lastMessage.getCreatedAt());
        }

        return ConversationDto.builder()
                              .id(conversation.getId())
                              .with(ConversationWith.builder()
                                                    .userId(otherUser.getId())
                                                    .name(otherUser.getName())
                                                    .profileImageUrl(otherUser.getProfileImageUrl())
                                                    .build())
                              .latestMessage(toLatestMessageDto(lastMessage))
                              .hasUnread(hasUnread)
                              .build();
    }

    private ConversationLatestMessage toLatestMessageDto(DirectMessage message) {
        if (message == null) return null;

        return ConversationLatestMessage.builder()
                                        .id(message.getId())
                                        .conversationsId(message.getConversation().getId())
                                        .createdAt(message.getCreatedAt())
                                        .content(message.getContent())
                                        .sender(ConversationSend.builder()
                                                                .userId(message.getSender().getId())
                                                                .name(message.getSender().getName())
                                                                .profileImageUrl(message.getSender().getProfileImageUrl())
                                                                .build())
                                        .receiver(ConversationReceiver.builder()
                                                                      .userId(message.getReceiver().getId())
                                                                      .name(message.getReceiver().getName())
                                                                      .profileImageUrl(message.getReceiver().getProfileImageUrl())
                                                                      .build())
                                        .build();
    }

    // -------------------------
    // 5) DM 목록 조회 (✅ 참가자 검증 + repo seek pagination)
    // -------------------------
    @Override
    @Transactional
    public DirectMessageResponseDto getDirectMessageList(
        UUID me,
        UUID conversationId,
        String cursor,
        UUID idAfter,
        int limit,
        String sortDirection,
        String sortBy
    ) {
        // ✅ 권한 체크 추가 (이게 빠져있으면 보안 이슈)
        ensureParticipant(conversationId, me);

        LocalDateTime cursorTime = parseCursor(cursor);

        List<DirectMessage> list = directMessageRepository.findMessageList(
            conversationId,
            cursorTime,
            idAfter,
            limit,
            sortDirection
        );
        long totalCount = directMessageRepository.countMessageList(conversationId);
        if (!list.isEmpty()) {

            LocalDateTime latestTime = list.stream()
                                           .map(DirectMessage::getCreatedAt)
                                           .max(LocalDateTime::compareTo) // 가장 미래의 시간 찾기
                                           .orElse(null);

            // "이 시간까지 다 읽었다"고 DB에 도장 쾅!
            conversationParticipantRepository.updateLastReadAtIfNewer(
                conversationId,
                me,
                latestTime
            );
        }

        boolean hasNext = list.size() > limit;
        if (hasNext) {
            list = list.subList(0, limit);
        }

        List<DirectMessageDto> data = list.stream()
                                          .map(directMessageMapper::toDto)
                                          .toList();

        String nextCursor = null;
        UUID nextIdAfter = null;

        if (hasNext && !list.isEmpty()) {
            DirectMessage last = list.get(list.size() - 1);
            nextCursor = last.getCreatedAt().toString();
            nextIdAfter = last.getId();
        }

        return DirectMessageResponseDto.builder()
                                       .data(data)
                                       .nextCursor(nextCursor)
                                       .nextIdAfter(nextIdAfter)
                                       .hasNext(hasNext)
                                       .totalCount(totalCount)
                                       .sortDirection(sortDirection)
                                       .sortBy(sortBy)
                                       .build();
    }


    @Override
    public DirectMessageWithDto getDirectMessageWith(UUID me, UUID other) {
        if (other == null) {
            throw new IllegalArgumentException("withUserId는 필수입니다.");
        }
        if (me.equals(other)) {
            throw new IllegalArgumentException("자기 자신과 대화할 수 없습니다.");
        }

        User otherUser = userRepository.findById(other)
                                       .orElseThrow(() -> new IllegalArgumentException("상대 유저가 존재하지 않습니다."));

        UUID conversationId = conversationRepository.findOneToOneConversationId(Set.of(me, other))
                                                    .orElse(null);

        if (conversationId == null) {
            return DirectMessageWithDto.builder()
                                       .id(null)
                                       .with(DirectMessageWith.builder()
                                                              .userId(otherUser.getId())
                                                              .name(otherUser.getName())
                                                              .profileImageUrl(otherUser.getProfileImageUrl())
                                                              .build())
                                       .lastestMessage(null)
                                       .hasUnread(false)
                                       .build();
        }

        // 참가자 검증(여긴 findLastReadAt를 활용)
        LocalDateTime myLastReadAt = conversationParticipantRepository
            .findLastReadAtByConversationIdAndUserId(conversationId, me)
            .orElseThrow(() -> new AccessDeniedException("대화방 참여자가 아닙니다."));

        DirectMessage latest = directMessageRepository.findLatestByConversationId(conversationId)
                                                      .orElse(null);

        DirectMessageLastestMessage latestDto = (latest == null) ? null : toDirectMessageLastestMessage(latest);

        boolean hasUnread = false;
        if (latest != null) {
            boolean receiverIsMe = latest.getReceiver().getId().equals(me);
            boolean newerThanRead = (myLastReadAt == null) || latest.getCreatedAt().isAfter(myLastReadAt);
            hasUnread = receiverIsMe && newerThanRead;
        }

        return DirectMessageWithDto.builder()
                                   .id(conversationId)
                                   .with(DirectMessageWith.builder()
                                                          .userId(otherUser.getId())
                                                          .name(otherUser.getName())
                                                          .profileImageUrl(otherUser.getProfileImageUrl())
                                                          .build())
                                   .lastestMessage(latestDto)
                                   .hasUnread(hasUnread)
                                   .build();
    }

    private DirectMessageLastestMessage toDirectMessageLastestMessage(DirectMessage msg) {
        return DirectMessageLastestMessage.builder()
                                          .id(msg.getId())
                                          .conversationId(msg.getConversation().getId())
                                          .createdAt(msg.getCreatedAt())
                                          .sender(DirectMessageSender.builder()
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

    private ConversationDto buildEmptyConversationDto(Conversation conv, User other) {
        return ConversationDto.builder()
                              .id(conv.getId())
                              .with(ConversationWith.builder()
                                                    .userId(other.getId())
                                                    .name(other.getName())
                                                    .profileImageUrl(other.getProfileImageUrl())
                                                    .build())
                              .latestMessage(null)
                              .hasUnread(false)
                              .build();
    }

    /**
     * 참가자 검증 - repo에 exists 메서드가 없더라도
     * findLastReadAtByConversationIdAndUserId로 검증 가능.
     */
    private void ensureParticipant(UUID conversationId, UUID me) {
        boolean ok = conversationParticipantRepository
            .findLastReadAtByConversationIdAndUserId(conversationId, me)
            .isPresent();
        if (!ok) {
            throw new AccessDeniedException("대화방 참여자가 아닙니다.");
        }
    }

    private LocalDateTime parseCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(cursor.trim());
        } catch (Exception e) {
            // ✅ cursor 잘못 들어오면 400이 나게 만드는 게 명세/테스트에 유리
            throw new IllegalArgumentException("cursor 형식이 올바르지 않습니다. (예: 2026-01-01T12:30:00)");
        }
    }
}
