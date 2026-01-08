package com.mopl.api.domain.user.repository.impl;

import com.mopl.api.domain.content.entity.QContent;
import com.mopl.api.domain.user.dto.request.WatchingSessionSearchRequest;
import com.mopl.api.domain.user.entity.QUser;
import com.mopl.api.domain.user.entity.QWatchingSession;
import com.mopl.api.domain.user.entity.WatchingSession;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Slf4j
@Repository
@RequiredArgsConstructor
public class WatchingSessionRepositoryCustomImpl implements WatchingSessionRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private final QContent content = QContent.content;
    private final QUser user = QUser.user;
    private final QWatchingSession watchingSession = QWatchingSession.watchingSession;

    @Override
    public List<WatchingSession> searchSessions(UUID contentId, WatchingSessionSearchRequest request) {
        return queryFactory
            .selectFrom(watchingSession)
            .join(watchingSession.watcher, user)
            .fetchJoin()
            .join(watchingSession.content, content)
            .fetchJoin()
            .where(
                watchingSession.content.id.eq(contentId),
                nameLike(request.watcherNameLike()),
                cursorCondition(request)
            )
            .orderBy(
                request.sortDirection() == WatchingSessionSearchRequest.SortDirection.ASCENDING
                    ? watchingSession.createdAt.asc() : watchingSession.createdAt.desc(),
                request.sortDirection() == WatchingSessionSearchRequest.SortDirection.ASCENDING
                    ? watchingSession.id.asc() : watchingSession.id.desc()
            )
            .limit(request.limit() + 1L)
            .fetch();
    }

    private BooleanExpression nameLike(String nameLike) {
        return StringUtils.hasText(nameLike) ? user.name.contains(nameLike) : null;
    }

    private BooleanExpression cursorCondition(WatchingSessionSearchRequest request) {
        if (!StringUtils.hasText(request.cursor()) || !StringUtils.hasText(request.idAfter())) {
            return null;
        }

        try {
            LocalDateTime cursorDateTime = LocalDateTime.parse(request.cursor(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            UUID lastId = UUID.fromString(request.idAfter());

            if (request.sortDirection() == WatchingSessionSearchRequest.SortDirection.ASCENDING) {
                return watchingSession.createdAt.gt(cursorDateTime)
                                                .or(watchingSession.createdAt.eq(cursorDateTime)
                                                                             .and(watchingSession.id.gt(lastId)));
            } else {
                return watchingSession.createdAt.lt(cursorDateTime)
                                                .or(watchingSession.createdAt.eq(cursorDateTime)
                                                                             .and(watchingSession.id.lt(lastId)));
            }
        } catch (Exception e) {
            log.warn("커서 파싱 실패: cursor={}, idAfter={}", request.cursor(), request.idAfter());
            return null;
        }
    }
}