package com.mopl.api.domain.conversation.repository;

import static com.mopl.api.domain.conversation.entity.QConversation.conversation;
import static com.mopl.api.domain.conversation.entity.QConversationParticipant.conversationParticipant;

import com.mopl.api.domain.conversation.entity.QConversation;
import com.mopl.api.domain.conversation.entity.QConversationParticipant;
import com.mopl.api.domain.dm.dto.response.conversation.ConversationListRow;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ConversationRepositoryImpl implements ConversationRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<ConversationListRow> findConversationList(
        UUID myId,
        String keywordLike,
        LocalDateTime cursorTime,
        UUID idAfter,
        int limit,
        String sortDirection
    ) {
        QConversation c = conversation;
        QConversationParticipant p = conversationParticipant; // 나
        QConversationParticipant p2 = new QConversationParticipant("p2"); // 상대방

        boolean desc = "DESCENDING".equalsIgnoreCase(sortDirection);

        // 1. 정렬 기준 설정
        OrderSpecifier<?> orderTime = desc
            ? c.lastMessageCreatedAt.desc().nullsLast()
            : c.lastMessageCreatedAt.asc().nullsLast();
        OrderSpecifier<?> orderId = desc ? c.id.desc() : c.id.asc();

        // 2. 검색 조건 설정 (BooleanBuilder)
        BooleanBuilder where = new BooleanBuilder();
        where.and(p.user.id.eq(myId));           // 내가 참여한 방
        where.and(p2.user.id.ne(myId));          // 상대방 (1:1 가정)

        if (keywordLike != null && !keywordLike.isBlank()) {
            where.and(p2.user.name.containsIgnoreCase(keywordLike.trim()));
        }

        // 3. 커서 페이징 조건
        if (cursorTime != null && idAfter != null) {
            where.and(applyCursor(c, cursorTime, idAfter, desc));
        }

        // 4. 쿼리 실행 (Projections)
        return queryFactory
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
            .join(p2).on(p2.conversation.eq(c).and(p2.user.id.ne(myId)))
            .where(where)
            .orderBy(orderTime, orderId)
            .limit(limit + 1L) // 다음 페이지 확인을 위해 +1 조회
            .fetch();
    }

    // 커서 조건 생성 메서드 (private으로 숨김)
    private BooleanExpression applyCursor(
        QConversation c,
        LocalDateTime cursorTime,
        UUID idAfter,
        boolean desc
    ) {
        if (desc) {
            return c.lastMessageCreatedAt.lt(cursorTime)
                                         .or(c.lastMessageCreatedAt.eq(cursorTime).and(c.id.lt(idAfter)));
        }
        return c.lastMessageCreatedAt.gt(cursorTime)
                                     .or(c.lastMessageCreatedAt.eq(cursorTime).and(c.id.gt(idAfter)));
    }
}