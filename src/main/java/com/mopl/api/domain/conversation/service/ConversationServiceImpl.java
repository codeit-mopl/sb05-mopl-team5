package com.mopl.api.domain.conversation.service;


import com.mopl.api.domain.conversation.dto.response.conversation.ConversationReceiver;
import com.mopl.api.domain.conversation.dto.response.conversation.ConversationSend;
import com.mopl.api.domain.conversation.dto.response.direct.DirectMessageSender;
import com.mopl.api.domain.conversation.entity.Conversation;
import com.mopl.api.domain.conversation.entity.ConversationParticipant;
import com.mopl.api.domain.conversation.entity.DirectMessage;
import com.mopl.api.domain.conversation.entity.QConversation;
import com.mopl.api.domain.conversation.entity.QConversationParticipant;
import com.mopl.api.domain.conversation.entity.QDirectMessage;
import com.mopl.api.domain.conversation.repository.ConversationParticipantRepository;
import com.mopl.api.domain.conversation.repository.ConversationRepository;
import com.mopl.api.domain.conversation.repository.DirectMessageRepository;
import com.mopl.api.domain.conversation.dto.request.ConversationRequestDto;
import com.mopl.api.domain.conversation.dto.response.conversation.ConversationDto;
import com.mopl.api.domain.conversation.dto.response.conversation.ConversationLatestMessage;
import com.mopl.api.domain.conversation.dto.response.conversation.ConversationListRow;
import com.mopl.api.domain.conversation.dto.response.conversation.ConversationResponseDto;
import com.mopl.api.domain.conversation.dto.response.conversation.ConversationWith;
import com.mopl.api.domain.conversation.dto.response.direct.DirectMessageDto;
import com.mopl.api.domain.conversation.dto.response.direct.DirectMessageLastestMessage;
import com.mopl.api.domain.conversation.dto.response.direct.DirectMessageReceiver;
import com.mopl.api.domain.conversation.dto.response.direct.DirectMessageResponseDto;

import com.mopl.api.domain.conversation.dto.response.direct.DirectMessageWith;
import com.mopl.api.domain.conversation.dto.response.direct.DirectMessageWithDto;
import com.mopl.api.domain.user.entity.User;
import com.mopl.api.domain.user.repository.UserRepository;
import com.mopl.api.global.config.security.claim.CustomUserDetails;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ConversationServiceImpl implements ConversationService {


    private final JPAQueryFactory queryFactory;
    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository conversationParticipantRepository;
    private final DirectMessageRepository directMessageRepository;
    private final UserRepository userRepository;


    // 현재 로그인한 사용자 ID 추출
    private UUID currentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (principal instanceof CustomUserDetails userDetails) {
            return userDetails.getUserDto().id();
        }
        throw new IllegalStateException("인증 정보가 올바르지 않습니다.");
    }


    // -------------------------
    // 1) 1:1 대화방 생성
    // -------------------------
    @Override
    @Transactional // 쓰기 트랜잭션 필요
    public ConversationDto createConversation(ConversationRequestDto withUserId) {
        UUID me = currentUserId();
        UUID other = withUserId.withUserId();

        if (other == null) {
            throw new IllegalArgumentException("withUserId는 필수입니다.");
        }
        if (me.equals(other)) {
            throw new IllegalArgumentException("자기 자신과 대화할 수 없습니다.");
        }

        UUID conversationId = conversationRepository.findOneToOneConversationId(Set.of(me, other))
                                                    .orElse(null);

        // 방이 없으면 새로 생성
        if (conversationId == null) {
            User meUser = userRepository.getReferenceById(me); // Proxy 조회
            User otherUser = userRepository.findById(other)
                                           .orElseThrow(() -> new IllegalArgumentException("상대 유저가 존재하지 않습니다."));

            // [수정] Conversation 생성자 문제 해결됨 (Entity 수정 덕분)
            Conversation newConversation = conversationRepository.save(Conversation.create());

            conversationParticipantRepository.save(new ConversationParticipant(newConversation, meUser));
            conversationParticipantRepository.save(new ConversationParticipant(newConversation, otherUser));

            // 새 방은 메시지가 없으므로 빈 DTO 반환
            return buildEmptyConversationDto(newConversation, otherUser);
        }

        // 방이 있으면 해당 방 정보 반환
        return conversationCheck(conversationId);
    }

    @Override
    @Transactional
    public ConversationResponseDto getConversationList(
        String keywordLike,
        String cursor,
        UUID idAfter,
        int limit,
        String sortDirection,
        String sortBy
    ) {
        UUID me = currentUserId();
        LocalDateTime cursorTime = parseCursor(cursor);

        // ✅ [변경] Repository 호출: 복잡한 쿼리 로직은 전부 RepositoryImpl로 위임
        List<ConversationListRow> rows = conversationRepository.findConversationList(
            me,
            keywordLike,
            cursorTime,
            idAfter,
            limit,
            sortDirection
        );

        // --- 아래부터는 DTO 변환 및 커서 계산 로직 (기존 유지) ---

        boolean hasNext = rows.size() > limit;
        if (hasNext) {
            rows = rows.subList(0, limit);
        }

        List<ConversationDto> data = rows.stream().map(r -> {
            boolean hasUnread = r.lastMessageCreatedAt() != null
                && (r.myLastReadAt() == null || r.myLastReadAt().isBefore(r.lastMessageCreatedAt()));

            // 목록 조회용 LatestMessage 조립 (Sender/Receiver는 목록 성능상 null 처리 유지)
            ConversationLatestMessage latest = (r.lastMessageCreatedAt() == null && r.lastMessageContent() == null)
                ? null
                : ConversationLatestMessage.builder()
                                           .id(null)
                                           .conversationsId(r.conversationId())
                                           .createdAt(r.lastMessageCreatedAt())
                                           .content(r.lastMessageContent())
                                           .sender(ConversationSend.builder()
                                                                   .userId(null)   // ID는 알 수 없으므로 null
                                                                   .name("")       // 이름은 빈 문자열로 처리
                                                                   .profileImageUrl(null)
                                                                   .build())
                                           .receiver(ConversationReceiver.builder()
                                                                         .userId(null)
                                                                         .name("")
                                                                         .profileImageUrl(null)
                                                                         .build())
                                           .build();

            return ConversationDto.builder()
                                  .id(r.conversationId())
                                  .with(ConversationWith.builder()
                                                        .userId(r.otherUserId())
                                                        .name(r.otherName())
                                                        .profileImageUrl(r.otherProfileImageUrl())
                                                        .build())
                                  .latestMessage(latest)
                                  .hasUnread(hasUnread)
                                  .build();
        }).toList();

        // 다음 커서 생성
        String nextCursor = null;
        UUID nextIdAfter = null;
        if (hasNext && !data.isEmpty()) {
            // 원본 rows의 마지막 데이터를 기준으로 커서 생성
            // (참고: data 리스트는 map을 거쳤으므로 rows를 쓰는 게 안전할 수 있음)
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
                                      .totalCount(0)
                                      .sortBy(sortBy)
                                      .sortDirection(sortDirection)
                                      .build();
    }


    @Override
    @Transactional
    public void conversationRead(UUID conversationId, UUID directMessageId) {
        UUID me = currentUserId();

        LocalDateTime messageCreatedAt = directMessageRepository
            .findCreatedAtByIdAndConversationId(directMessageId, conversationId)
            .orElseThrow(() -> new IllegalArgumentException("메시지를 찾을 수 없습니다."));

        conversationParticipantRepository.updateLastReadAtIfNewer(conversationId, me, messageCreatedAt);
    }

    @Override
    public ConversationDto conversationCheck(UUID conversationId) {
        UUID me = currentUserId();

        // 1. 대화방 존재 확인 (기본 JPA 메서드 사용)
        Conversation conversation = conversationRepository.findById(conversationId)
                                                          .orElseThrow(() -> new IllegalStateException(
                                                              "대화방에 참여하지 않았거나 존재하지 않습니다."));

        // 2. 참여자 목록 조회 (Repository로 쿼리 위임)
        List<ConversationParticipant> participants = conversationParticipantRepository.findAllByConversationId(
            conversationId);

        // 3. 내 참여 정보와 상대방(Other) 찾기 (Java Stream 로직)
        ConversationParticipant myParticipant = participants.stream()
                                                            .filter(p -> p.getUser().getId().equals(me))
                                                            .findFirst()
                                                            .orElseThrow(
                                                                () -> new IllegalStateException("대화방 참여자가 아닙니다."));

        User otherUser = participants.stream()
                                     .map(ConversationParticipant::getUser)
                                     .filter(user -> !user.getId().equals(me))
                                     .findFirst()
                                     .orElseThrow(() -> new IllegalStateException("대화 상대방을 찾을 수 없습니다."));

        // 4. 최신 메시지 조회 (Repository로 쿼리 위임 - 핵심!)
        // 여기서 Sender/Receiver가 채워진 엔티티를 받아옵니다.
        DirectMessage lastMessage = directMessageRepository.findLatestByConversationId(conversationId)
                                                           .orElse(null);

        // 5. 안 읽은 메시지 여부 계산
        boolean hasUnread = false;
        if (lastMessage != null) {
            LocalDateTime lastReadAt = myParticipant.getLastReadAt();
            // 마지막 읽은 시간이 없거나, 메시지 시간이 더 뒤라면 Unread
            hasUnread = lastReadAt == null || lastReadAt.isBefore(lastMessage.getCreatedAt());
        }

        // 6. DTO 변환 및 반환
        return ConversationDto.builder()
                              .id(conversation.getId())
                              .with(ConversationWith.builder()
                                                    .userId(otherUser.getId())
                                                    .name(otherUser.getName())
                                                    .profileImageUrl(otherUser.getProfileImageUrl())
                                                    .build())
                              .latestMessage(toLatestMessageDto(lastMessage)) // 아래 헬퍼 메서드 사용
                              .hasUnread(hasUnread)
                              .build();
    }

    private ConversationLatestMessage toLatestMessageDto(DirectMessage message) {
        if (message == null) {
            return null;
        }
        return ConversationLatestMessage.builder()
                                        .id(message.getId())
                                        .conversationsId(message.getConversation().getId())
                                        .createdAt(message.getCreatedAt())
                                        .content(message.getContent())
                                        .sender(ConversationSend.builder()
                                                                .userId(message.getSender().getId())
                                                                .name(message.getSender().getName())
                                                                .profileImageUrl(
                                                                    message.getSender().getProfileImageUrl())
                                                                .build())
                                        .receiver(ConversationReceiver.builder()
                                                                      .userId(message.getReceiver().getId())
                                                                      .name(message.getReceiver().getName())
                                                                      .profileImageUrl(
                                                                          message.getReceiver().getProfileImageUrl())
                                                                      .build())
                                        .build();
    }

    @Override
    @Transactional(readOnly = true)
    public DirectMessageResponseDto getDirectMessageList(
        UUID conversationId,
        String cursor,
        UUID idAfter,
        int limit,
        String sortDirection,
        String sortBy
    ) {
        UUID me = currentUserId();
        LocalDateTime cursorTime = parseCursor(cursor);

        // 1) 참여자 검증: JPA exists 메서드로 간결하게 처리
        boolean isMember = conversationParticipantRepository.existsByConversationIdAndUserId(conversationId, me);

        if (!isMember) {
            throw new IllegalStateException("대화방 참여자가 아닙니다.");
        }

        // 2) 메시지 조회: RepositoryImpl로 로직 위임
        List<DirectMessage> messages = directMessageRepository.findMessageList(
            conversationId,
            cursorTime,
            idAfter,
            limit,
            sortDirection
        );

        // --- 이하 데이터 가공 로직 (기존과 동일) ---

        boolean hasNext = messages.size() > limit;
        if (hasNext) {
            messages = messages.subList(0, limit);
        }

        List<DirectMessageDto> data = messages.stream()
                                              .map(this::toDirectMessageDto)
                                              .toList();

        String nextCursor = null;
        UUID nextIdAfter = null;
        if (hasNext && !messages.isEmpty()) {
            DirectMessage last = messages.get(messages.size() - 1);
            nextCursor = last.getCreatedAt().toString();
            nextIdAfter = last.getId();
        }

        return DirectMessageResponseDto.builder()
                                       .data(data)
                                       .nextCursor(nextCursor)
                                       .nextIdAfter(nextIdAfter)
                                       .hasNext(hasNext)
                                       .totalCount(0)
                                       .sortBy(sortBy)
                                       .sortDirection(sortDirection)
                                       .build();
    }


    @Override
    @Transactional(readOnly = true)
    public DirectMessageWithDto getDirectMessageWith(UUID userId) {
        UUID me = currentUserId();
        UUID other = userId;

        if (other == null) {
            throw new IllegalArgumentException("userId는 필수입니다.");
        }
        if (me.equals(other)) {
            throw new IllegalArgumentException("자기 자신과의 대화는 조회할 수 없습니다.");
        }

        User otherUser = userRepository.findById(other)
                                       .orElseThrow(() -> new IllegalArgumentException("상대 유저가 존재하지 않습니다."));

        // 1. 1:1 대화방 ID 찾기 (Repository로 위임)
        UUID conversationId = conversationRepository.findOneToOneConversationId(Set.of(me, other))
                                                    .orElse(null);

        // 대화방이 없으면 빈 응답 반환
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

        // 2. 내 lastReadAt 조회 (Repository로 위임)
        LocalDateTime myLastReadAt = conversationParticipantRepository
            .findLastReadAtByConversationIdAndUserId(conversationId, me)
            .orElse(null);

        // 3. 최신 메시지 1건 조회 (기존에 만든 메서드 재활용!)
        // Fetch Join이 적용되어 있어 Sender/Receiver 정보가 안전하게 들어있음
        DirectMessage latest = directMessageRepository.findLatestByConversationId(conversationId)
                                                      .orElse(null);

        DirectMessageLastestMessage latestDto = (latest == null) ? null : toDirectMessageLastestMessage(latest);

        // 4. 안 읽은 메시지 여부 계산
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
                                                                     .profileImageUrl(
                                                                       msg.getSender().getProfileImageUrl())
                                                                     .build())
                                          .receiver(DirectMessageReceiver.builder()
                                                                         .userId(msg.getReceiver().getId())
                                                                         .name(msg.getReceiver().getName())
                                                                         .profileImageUrl(
                                                                             msg.getReceiver().getProfileImageUrl())
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

    private DirectMessageDto toDirectMessageDto(DirectMessage msg) {
        return DirectMessageDto.builder()
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

    private LocalDateTime parseCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(cursor);
        } catch (Exception e) {
            return null; // 포맷 에러 시 커서 무시
        }
    }

    private BooleanExpression applyCursor(
        com.querydsl.core.types.dsl.DateTimePath<LocalDateTime> createdAtPath,
        com.querydsl.core.types.dsl.ComparablePath<UUID> idPath,
        LocalDateTime cursorTime,
        UUID idAfter,
        boolean desc
    ) {
        if (desc) {
            return createdAtPath.lt(cursorTime)
                                .or(createdAtPath.eq(cursorTime).and(idPath.lt(idAfter)));
        }
        return createdAtPath.gt(cursorTime)
                            .or(createdAtPath.eq(cursorTime).and(idPath.gt(idAfter)));
    }

}
