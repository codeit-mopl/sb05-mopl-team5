
package com.mopl.api.domain.conversation.repository;

import static com.mopl.api.domain.conversation.entity.QDirectMessage.directMessage;

import com.mopl.api.domain.conversation.entity.DirectMessage;
import com.mopl.api.domain.conversation.entity.QDirectMessage;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DirectMessageRepositoryImpl implements DirectMessageRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<DirectMessage> findMessageList(
        UUID conversationId,
        LocalDateTime cursorTime,
        UUID idAfter,
        int limit,
        String sortDirection
    ) {
        QDirectMessage m = directMessage;
        boolean desc = "DESCENDING".equalsIgnoreCase(sortDirection);

        // 1. 정렬 기준
        OrderSpecifier<?> orderCreatedAt = desc ? m.createdAt.desc() : m.createdAt.asc();
        OrderSpecifier<?> orderId = desc ? m.id.desc() : m.id.asc();

        // 2. 검색 조건 (Where)
        BooleanBuilder where = new BooleanBuilder();
        where.and(m.conversation.id.eq(conversationId));

        // 3. 커서 페이징 적용
        if (cursorTime != null && idAfter != null) {
            where.and(applyCursor(m, cursorTime, idAfter, desc));
        }

        // 4. 쿼리 실행 (Fetch Join 포함)
        return queryFactory
            .selectFrom(m)
            .join(m.sender).fetchJoin()   // N+1 방지
            .join(m.receiver).fetchJoin() // N+1 방지
            .where(where)
            .orderBy(orderCreatedAt, orderId)
            .limit(limit + 1L) // hasNext 확인용 +1
            .fetch();
    }

    // 커서 조건 생성 (Impl 내부로 이동)
    private BooleanExpression applyCursor(
        QDirectMessage m,
        LocalDateTime cursorTime,
        UUID idAfter,
        boolean desc
    ) {
        if (desc) {
            return m.createdAt.lt(cursorTime)
                              .or(m.createdAt.eq(cursorTime).and(m.id.lt(idAfter)));
        }
        return m.createdAt.gt(cursorTime)
                          .or(m.createdAt.eq(cursorTime).and(m.id.gt(idAfter)));
    }
}