package com.mopl.api.domain.notification.repository.impl;

import com.mopl.api.domain.notification.dto.request.NotificationCursorPageRequest;
import com.mopl.api.domain.notification.dto.request.NotificationCursorPageRequest.SortDirection;
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
    private static final QNotification notification = QNotification.notification;

    @Override
    public Slice<Notification> findAllByReceiverId(UUID receiverId, NotificationCursorPageRequest request) {
        List<Notification> notifications = queryFactory
            .selectFrom(notification)
            .where(notification.receiver.id.eq(receiverId),
                getCursorCondition(request))
            .orderBy(getOrderSpecifier(request))
            .limit(request.limit() + 1)
            .fetch();

        boolean hasNext = notifications.size() > request.limit();
        if (hasNext) {
            notifications.remove(notifications.size() - 1);
        }

        return new SliceImpl<>(notifications, Pageable.ofSize(request.limit()), hasNext);
    }

    @Override
    public long countByReceiverId(UUID receiverId) {
        Long count = queryFactory
            .select(notification.count())
            .from(notification)
            .where(notification.receiver.id.eq(receiverId))
            .fetchOne();

        return count != null ? count : 0L;
    }

    private BooleanExpression getCursorCondition(NotificationCursorPageRequest request) {
        if (request.cursor() == null) {
            return null;
        }

        LocalDateTime cursorTime = LocalDateTime.parse(request.cursor());
        UUID idAfter = request.idAfter();

        if (request.sortDirection() == SortDirection.DESCENDING) {
            // 내림차순: 커서 시간보다 작거나, 시간이 같으면 ID가 작은 것
            if (idAfter != null) {
                return notification.createdAt.lt(cursorTime)
                                             .or(notification.createdAt.eq(cursorTime)
                                                                       .and(notification.id.lt(idAfter)));
            }
            return notification.createdAt.lt(cursorTime);
        } else {
            // 오름차순: 커서 시간보다 크거나, 시간이 같으면 ID가 큰 것
            if (idAfter != null) {
                return notification.createdAt.gt(cursorTime)
                                             .or(notification.createdAt.eq(cursorTime)
                                                                       .and(notification.id.gt(idAfter)));
            }
            return notification.createdAt.gt(cursorTime);
        }
    }

    private OrderSpecifier<?>[] getOrderSpecifier(NotificationCursorPageRequest request) {
        boolean isAscending = request.sortDirection() == SortDirection.ASCENDING;

        return new OrderSpecifier<?>[]{
            isAscending ? notification.createdAt.asc() : notification.createdAt.desc(),
            isAscending ? notification.id.asc() : notification.id.desc()
        };
    }
}
