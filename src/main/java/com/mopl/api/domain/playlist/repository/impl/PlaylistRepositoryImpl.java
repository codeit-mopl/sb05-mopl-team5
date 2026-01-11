package com.mopl.api.domain.playlist.repository.impl;

import com.mopl.api.domain.playlist.entity.Playlist;
import com.mopl.api.domain.playlist.entity.QPlaylist;
import com.mopl.api.domain.playlist.entity.QSubscription;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PlaylistRepositoryImpl implements PlaylistRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Playlist> findPlaylistsWithCursor(
        String keywordLike,
        UUID ownerIdEqual,
        UUID subscriberIdEqual,
        String sortBy,
        String sortDirection,
        LocalDateTime cursorDateTime,
        Long cursorLong,
        UUID idAfter,
        int limit
    ) {
        QPlaylist playlist = QPlaylist.playlist;
        QSubscription subscription = QSubscription.subscription;

        BooleanExpression predicate = playlist.isDeleted.eq(false);

        if (keywordLike != null && !keywordLike.isBlank()) {
            String keyword = "%" + keywordLike.toLowerCase() + "%";
            predicate = predicate.and(
                playlist.title.lower()
                              .like(keyword)
                              .or(playlist.description.lower()
                                                      .like(keyword))
            );
        }

        if (ownerIdEqual != null) {
            predicate = predicate.and(playlist.owner.id.eq(ownerIdEqual));
        }

        if (subscriberIdEqual != null) {
            predicate = predicate.and(
                playlist.id.in(
                    queryFactory
                        .select(subscription.playlist.id)
                        .from(subscription)
                        .where(subscription.user.id.eq(subscriberIdEqual))
                )
            );
        }

        if (idAfter != null) {
            predicate = predicate.and(
                buildCursorPredicate(playlist, sortBy, sortDirection, cursorDateTime, cursorLong, idAfter));
        }

        OrderSpecifier<?> primaryOrder = buildOrderSpecifier(playlist, sortBy, sortDirection);
        OrderSpecifier<UUID> tieBreaker = new OrderSpecifier<>(
            "DESCENDING".equals(sortDirection) ? Order.DESC : Order.ASC,
            playlist.id
        );

        return queryFactory
            .selectFrom(playlist)
            .where(predicate)
            .orderBy(primaryOrder, tieBreaker)
            .limit(limit + 1)
            .fetch();
    }

    @Override
    public long countPlaylists(
        String keywordLike,
        UUID ownerIdEqual,
        UUID subscriberIdEqual
    ) {
        QPlaylist playlist = QPlaylist.playlist;
        QSubscription subscription = QSubscription.subscription;

        BooleanExpression predicate = playlist.isDeleted.eq(false);

        if (keywordLike != null && !keywordLike.isBlank()) {
            String keyword = "%" + keywordLike.toLowerCase() + "%";
            predicate = predicate.and(
                playlist.title.lower()
                              .like(keyword)
                              .or(playlist.description.lower()
                                                      .like(keyword))
            );
        }

        if (ownerIdEqual != null) {
            predicate = predicate.and(playlist.owner.id.eq(ownerIdEqual));
        }

        if (subscriberIdEqual != null) {
            predicate = predicate.and(
                playlist.id.in(
                    queryFactory
                        .select(subscription.playlist.id)
                        .from(subscription)
                        .where(subscription.user.id.eq(subscriberIdEqual))
                )
            );
        }

        return queryFactory
            .selectFrom(playlist)
            .where(predicate)
            .fetchCount();
    }

    private BooleanExpression buildCursorPredicate(
        QPlaylist playlist,
        String sortBy,
        String sortDirection,
        LocalDateTime cursorDateTime,
        Long cursorLong,
        UUID idAfter
    ) {
        boolean isDescending = "DESCENDING".equals(sortDirection);

        if ("updatedAt".equals(sortBy) && cursorDateTime != null) {
            if (isDescending) {
                return playlist.updatedAt.lt(cursorDateTime)
                                         .or(playlist.updatedAt.eq(cursorDateTime)
                                                               .and(playlist.id.lt(idAfter)));
            } else {
                return playlist.updatedAt.gt(cursorDateTime)
                                         .or(playlist.updatedAt.eq(cursorDateTime)
                                                               .and(playlist.id.gt(idAfter)));
            }
        } else if ("subscriberCount".equals(sortBy) && cursorLong != null) {
            if (isDescending) {
                return playlist.subscriberCount.lt(cursorLong)
                                               .or(playlist.subscriberCount.eq(cursorLong)
                                                                           .and(playlist.id.lt(idAfter)));
            } else {
                return playlist.subscriberCount.gt(cursorLong)
                                               .or(playlist.subscriberCount.eq(cursorLong)
                                                                           .and(playlist.id.gt(idAfter)));
            }
        }

        return null;
    }

    private OrderSpecifier<?> buildOrderSpecifier(QPlaylist playlist, String sortBy, String sortDirection) {
        Order order = "DESCENDING".equals(sortDirection) ? Order.DESC : Order.ASC;

        if ("updatedAt".equals(sortBy)) {
            return new OrderSpecifier<>(order, playlist.updatedAt);
        } else if ("subscriberCount".equals(sortBy)) {
            return new OrderSpecifier<>(order, playlist.subscriberCount);
        }

        return new OrderSpecifier<>(order, playlist.updatedAt);
    }
}
