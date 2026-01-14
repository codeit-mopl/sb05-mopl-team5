package com.mopl.api.domain.conversation.repository;

import com.mopl.api.domain.conversation.entity.ConversationParticipant;
import com.mopl.api.domain.conversation.entity.DirectMessage;
import com.mopl.api.domain.conversation.entity.QConversationParticipant;
import com.mopl.api.domain.conversation.entity.QDirectMessage;
import com.mopl.api.domain.user.entity.QUser;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class DirectMessageRepositoryImpl implements DirectMessageRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private static final QDirectMessage dm = QDirectMessage.directMessage;

    private static final QConversationParticipant p = QConversationParticipant.conversationParticipant;
    private static final QConversationParticipant pOther = new QConversationParticipant("pOther");

    private static final QUser sender = new QUser("sender");
    private static final QUser receiver = new QUser("receiver");
    private static final QUser u = new QUser("u");

    @Override
    public List<DirectMessage> findMessageList(
        UUID conversationId,
        LocalDateTime cursorTime,
        UUID idAfter,
        int limit,
        String sortDirection
    ) {
        boolean desc = "DESCENDING".equalsIgnoreCase(sortDirection);

        OrderSpecifier<?> orderCreatedAt = desc ? dm.createdAt.desc() : dm.createdAt.asc();
        OrderSpecifier<?> orderId = desc ? dm.id.desc() : dm.id.asc();

        BooleanBuilder where = new BooleanBuilder();
        where.and(dm.conversation.id.eq(conversationId));

        // seek pagination (createdAt, id)
        if (cursorTime != null && idAfter != null) {
            where.and(applySeek(dm.createdAt, dm.id, cursorTime, idAfter, desc));
        }

        return queryFactory
            .selectFrom(dm)
            // DTO에 sender/receiver 포함되므로 fetchJoin으로 N+1 제거
            .join(dm.sender, sender).fetchJoin()
            .join(dm.receiver, receiver).fetchJoin()
            .where(where)
            .orderBy(orderCreatedAt, orderId)
            .limit(limit + 1L)
            .fetch();
    }
    @Override
    public long countMessageList(UUID conversationId) {
        QDirectMessage m = QDirectMessage.directMessage;

        Long count = queryFactory
            .select(m.count())
            .from(m)
            .where(m.conversation.id.eq(conversationId))
            .fetchOne();

        return count != null ? count : 0L;
    }

    @Override
    public boolean existsParticipant(UUID conversationId, UUID userId) {
        Integer one = queryFactory
            .selectOne()
            .from(p)
            .where(
                p.conversation.id.eq(conversationId),
                p.user.id.eq(userId)
            )
            .fetchFirst();
        return one != null;
    }

    @Override
    public ConversationParticipant findOtherParticipant(UUID conversationId, UUID senderId) {
        return queryFactory
            .selectFrom(pOther)
            .join(pOther.user, u).fetchJoin()
            .where(
                pOther.conversation.id.eq(conversationId),
                pOther.user.id.ne(senderId)
            )
            .fetchFirst();
    }

    private BooleanExpression applySeek(
        com.querydsl.core.types.dsl.DateTimePath<LocalDateTime> createdAtPath,
        com.querydsl.core.types.dsl.ComparablePath<UUID> idPath,
        LocalDateTime cursorTime,
        UUID idAfter,
        boolean desc
    ) {
        // DESC: (createdAt < cursor) OR (createdAt == cursor AND id < idAfter)
        if (desc) {
            return createdAtPath.lt(cursorTime)
                                .or(createdAtPath.eq(cursorTime).and(idPath.lt(idAfter)));
        }
        // ASC : (createdAt > cursor) OR (createdAt == cursor AND id > idAfter)
        return createdAtPath.gt(cursorTime)
                            .or(createdAtPath.eq(cursorTime).and(idPath.gt(idAfter)));
    }
}
