package com.mopl.api.domain.notification.repository.impl;

import com.mopl.api.domain.notification.dto.request.NotificationCursorPageRequest;
import com.mopl.api.domain.notification.entity.Notification;
import com.mopl.api.domain.notification.entity.QNotification;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class NotificationRepositoryCustomImpl implements NotificationRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private static final QNotification n = QNotification.notification;

    @Override
    public Slice<Notification> findAllByReceiverId(UUID receiverId, NotificationCursorPageRequest request) {
        List<Notification> notifications = queryFactory
            .selectFrom(n)
            .where(n.receiver.id.eq(receiverId),
                getCursorCondition(request))
            .orderBy(getOrderSpecifier(request))
            .limit(request.limit() + 1)
            .fetch();

        boolean hasNext = notifications.size() > request.limit();

        if (hasNext) {
            notifications.remove(notifications.size() - 1);
        }

        // TODO : Pageable.unpaged() 자세히
        return new SliceImpl<>(notifications, Pageable.unpaged(), hasNext);
    }

    @Override
    public long countByReceiverId(UUID receiverId) {
        // TODO : 기능 작성해야 함
        return 0;
    }

    private BooleanExpression getCursorCondition(NotificationCursorPageRequest request) {
        if (request.cursor() == null) {
            return null;
        }

        LocalDateTime cursorTime = LocalDateTime.parse(request.cursor());
        UUID idAfter = request.idAfter();

        if (request.sortDirection()
                   .toString()
                   .equals("DESCENDING")) {
            if (idAfter != null) {
                return n.createdAt.lt(cursorTime)
                                  .or(n.createdAt.eq(cursorTime)
                                                 .and(n.id.lt(idAfter)));
            }
            return n.createdAt.lt(cursorTime);
        } else {
            if (idAfter != null) {
                return n.createdAt.gt(cursorTime)
                                  .or(n.createdAt.eq(cursorTime)
                                                 .and(n.id.gt(idAfter)));
            }
            return n.createdAt.gt(cursorTime);
        }
    }

    private OrderSpecifier<?>[] getOrderSpecifier(NotificationCursorPageRequest request) {
        boolean isAscending = request.sortDirection()
                                     .toString()
                                     .equals("ASCENDING");

        return new OrderSpecifier<?>[]{
            isAscending ? n.createdAt.asc() : n.createdAt.desc(),
            isAscending ? n.id.asc() : n.id.desc()
        };
    }
}
