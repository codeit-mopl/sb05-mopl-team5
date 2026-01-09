package com.mopl.api.domain.conversation.service;


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
import com.mopl.api.domain.conversation.dto.response.direct.DirectMessageSend;
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

    // TODO 권장사항, DB 조작 관련한 로직/기능이 Service 레이어에 작성된게 Repository로직으로 정리 되었으면 좋겠다.

    private final JPAQueryFactory queryFactory;
    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository conversationParticipantRepository;
    private final DirectMessageRepository directMessageRepository;
    private final UserRepository userRepository;

    // Q-Type 정의 (QueryDSL 사용)
    private static final QConversation c = QConversation.conversation;
    private static final QConversationParticipant p = QConversationParticipant.conversationParticipant; // 나
    private static final QConversationParticipant p2 = new QConversationParticipant("p2"); // 상대방
    private static final QDirectMessage m = QDirectMessage.directMessage;

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

        UUID conversationId = findOneToOneConversationId(me, other);

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

        boolean desc = "DESCENDING".equalsIgnoreCase(sortDirection);

        // ✅ 최신 메시지 시간 기준 정렬 + 안정적인 tie-break
        OrderSpecifier<?> orderTime =
            desc ? c.lastMessageCreatedAt.desc().nullsLast()
                : c.lastMessageCreatedAt.asc().nullsLast();
        OrderSpecifier<?> orderId =
            desc ? c.id.desc() : c.id.asc();

        BooleanBuilder where = new BooleanBuilder();
        // 내가 참여한 방
        where.and(p.user.id.eq(me));
        // 상대(1:1)
        where.and(p2.user.id.ne(me));

        // 상대 이름 검색
        if (keywordLike != null && !keywordLike.isBlank()) {
            where.and(p2.user.name.containsIgnoreCase(keywordLike.trim()));
        }

        // ✅ 커서: lastMessageCreatedAt + id tie-break
        // cursorTime이 null이면 커서 조건 미적용
        if (cursorTime != null && idAfter != null) {
            where.and(applyCursor(c.lastMessageCreatedAt, c.id, cursorTime, idAfter, desc));
        }

        // ✅ 1쿼리 Projection Row
        List<ConversationListRow> rows = queryFactory
            .select(Projections.constructor(
                ConversationListRow.class,
                c.id,
                p2.user.id,
                p2.user.name,
                p2.user.profileImageUrl,
                c.lastMessageContent,
                c.lastMessageCreatedAt,
                p.lastReadAt
            ))
            .from(c)
            .join(p).on(p.conversation.eq(c))
            .join(p2).on(p2.conversation.eq(c).and(p2.user.id.ne(me)))
            .where(where)
            .orderBy(orderTime, orderId)
            .limit(limit + 1L)
            .fetch();

        boolean hasNext = rows.size() > limit;
        if (hasNext) {
            rows = rows.subList(0, limit);
        }

        // ✅ record DTO 조립 (너희 실제 record 시그니처 1:1)
        List<ConversationDto> data = rows.stream().map(r -> {

            // hasUnread: null-safe
            // lastReadAt이 null이면 안 읽은 것으로 처리 (lastMessageCreatedAt이 존재할 때)
            boolean hasUnread =
                r.lastMessageCreatedAt() != null
                    && (r.myLastReadAt() == null || r.myLastReadAt().isBefore(r.lastMessageCreatedAt()));

            ConversationWith with = ConversationWith.builder()
                                                    .userId(r.otherUserId())
                                                    .name(r.otherName())
                                                    .profileImageUrl(r.otherProfileImageUrl())
                                                    .build();

            // ConversationLatestMessage는 필드가 6개라서
            // ✅ 우리가 가진 역정규화 정보(content/createdAt)만 채우고 나머지는 null
            ConversationLatestMessage latest = (r.lastMessageCreatedAt() == null && r.lastMessageContent() == null)
                ? null
                : ConversationLatestMessage.builder()
                                           .id(null)
                                           .conversationsId(r.conversationId())
                                           .createdAt(r.lastMessageCreatedAt())
                                           .sender(null)
                                           .receiver(null)
                                           .content(r.lastMessageContent())
                                           .build();

            return ConversationDto.builder()
                                  .id(r.conversationId())
                                  .with(with)
                                  .latestMessage(latest)
                                  .hasUnread(hasUnread)
                                  .build();
        }).toList();

        // next cursor
        String nextCursor = null;
        UUID nextIdAfter = null;
        if (hasNext && !rows.isEmpty()) {
            ConversationListRow last = rows.get(rows.size() - 1);
            nextCursor = last.lastMessageCreatedAt() != null ? last.lastMessageCreatedAt().toString() : null;
            nextIdAfter = last.conversationId();
        }

        return ConversationResponseDto.builder()
                                      .data(data)
                                      .nextCursor(nextCursor)
                                      .nextIdAfter(nextIdAfter)
                                      .hasNext(hasNext)
                                      .totalCount(0) // ✅ 무한스크롤 최적화: Count 쿼리 제거
                                      .sortBy(sortBy)
                                      .sortDirection(sortDirection)
                                      .build();
    }



    @Override
    @Transactional
    public void conversationRead(UUID conversationId, UUID directMessageId) {

        UUID me = currentUserId();

        // 메시지 시간 확인 (단순 조회)
        LocalDateTime messageCreatedAt = queryFactory
            .select(m.createdAt)
            .from(m)
            .where(m.id.eq(directMessageId).and(m.conversation.id.eq(conversationId)))
            .fetchOne();

        if (messageCreatedAt == null) {
            throw new IllegalArgumentException("메시지를 찾을 수 없습니다.");
        }

        // [최적화] Bulk Update: 영속성 컨텍스트를 거치지 않고 바로 DB 업데이트
        queryFactory
            .update(p)
            .set(p.lastReadAt, messageCreatedAt)
            .where(p.conversation.id.eq(conversationId)
                                    .and(p.user.id.eq(me))
                                    // 이미 읽은 시간이 더 최신이면 업데이트 안 함 (방어 로직)
                                    .and(p.lastReadAt.isNull().or(p.lastReadAt.lt(messageCreatedAt))))
            .execute();

    }

    @Override
    public ConversationDto conversationCheck(UUID conversationId) {
        UUID me = currentUserId();

        // [최적화] 단건 조회도 Projections 활용하여 쿼리 1방으로 해결
        ConversationDto result = queryFactory
            .select(Projections.constructor(ConversationDto.class,
                c.id,
                Projections.constructor(ConversationWith.class,
                    p2.user.id,
                    p2.user.name,
                    p2.user.profileImageUrl
                ),
                Projections.constructor(ConversationLatestMessage.class,
                    c.lastMessageContent,
                    c.lastMessageCreatedAt
                ),
                p.lastReadAt.before(c.lastMessageCreatedAt).and(c.lastMessageCreatedAt.isNotNull())
            ))
            .from(c)
            .join(p).on(p.conversation.eq(c).and(p.user.id.eq(me))) // 나
            .join(p2).on(p2.conversation.eq(c).and(p2.user.id.ne(me))) // 상대방
            .where(c.id.eq(conversationId))
            .fetchOne();

        if (result == null) {
            throw new IllegalStateException("대화방에 참여하지 않았거나 존재하지 않습니다.");
        }
        return result;
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

        // 1) 참여자 검증 (1쿼리)
        boolean isMember = queryFactory.selectOne()
                                       .from(p)
                                       .where(p.conversation.id.eq(conversationId).and(p.user.id.eq(me)))
                                       .fetchFirst() != null;

        if (!isMember) {
            throw new IllegalStateException("대화방 참여자가 아닙니다.");
        }

        boolean desc = "DESCENDING".equalsIgnoreCase(sortDirection);

        OrderSpecifier<?> orderCreatedAt = desc ? m.createdAt.desc() : m.createdAt.asc();
        OrderSpecifier<?> orderId = desc ? m.id.desc() : m.id.asc();

        BooleanBuilder where = new BooleanBuilder()
            .and(m.conversation.id.eq(conversationId));

        if (cursorTime != null && idAfter != null) {
            where.and(applyCursor(m.createdAt, m.id, cursorTime, idAfter, desc));
        }

        // 2) 메시지 조회 (fetchJoin으로 N+1 방지)
        List<DirectMessage> messages = queryFactory
            .selectFrom(m)
            .join(m.sender).fetchJoin()
            .join(m.receiver).fetchJoin()
            .where(where)
            .orderBy(orderCreatedAt, orderId)
            .limit(limit + 1L)
            .fetch();

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
                                       .totalCount(0) // ✅ 무한스크롤 최적화: count 쿼리 제거(필요하면 별도 API 권장)
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

        UUID conversationId = findOneToOneConversationId(me, other);

        // 대화방이 없으면 빈 응답
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

        // 내 lastReadAt (1쿼리)
        LocalDateTime myLastReadAt = queryFactory
            .select(p.lastReadAt)
            .from(p)
            .where(p.conversation.id.eq(conversationId).and(p.user.id.eq(me)))
            .fetchOne();

        // 최신 메시지 1건 (fetchJoin)
        DirectMessage latest = queryFactory
            .selectFrom(m)
            .join(m.sender).fetchJoin()
            .join(m.receiver).fetchJoin()
            .where(m.conversation.id.eq(conversationId))
            .orderBy(m.createdAt.desc(), m.id.desc())
            .fetchFirst();

        DirectMessageLastestMessage latestDto = (latest == null) ? null : toDirectMessageLastestMessage(latest);

        // ✅ 현재 컬럼 구조에서 정확한 unread 계산(최신 메시지가 "내가 받은 것"일 때만)
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
                                          .sender(DirectMessageSend.builder()
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


    private UUID findOneToOneConversationId(UUID me, UUID other) {

        return queryFactory.select(p.conversation.id).from(p).where(p.user.id.in(me, other)).groupBy(p.conversation.id).
                           having(p.user.id.countDistinct().eq(2L))
                           .fetchFirst();

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
