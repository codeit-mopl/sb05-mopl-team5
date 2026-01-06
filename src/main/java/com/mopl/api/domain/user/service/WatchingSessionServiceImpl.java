package com.mopl.api.domain.user.service;

import com.mopl.api.domain.content.dto.response.ContentDto;
import com.mopl.api.domain.content.entity.Content;
import com.mopl.api.domain.content.entity.QContent;
import com.mopl.api.domain.user.dto.response.CursorResponseWatchingSessionDto;
import com.mopl.api.domain.user.dto.response.UserDto;
import com.mopl.api.domain.user.dto.response.WatchingSessionDto;
import com.mopl.api.domain.user.dto.request.WatchingSessionSearchRequest;
import com.mopl.api.domain.user.entity.QUser;
import com.mopl.api.domain.user.entity.QWatchingSession;
import com.mopl.api.domain.user.entity.User;
import com.mopl.api.domain.user.entity.WatchingSession;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WatchingSessionServiceImpl implements WatchingSessionService {

    private final JPAQueryFactory queryFactory;

    @Override
    public WatchingSessionDto getWatchingSessionByUser(UUID watcherId) {
        if (watcherId == null) return null;

        QWatchingSession ws = QWatchingSession.watchingSession;
        QUser u = QUser.user;
        QContent c = QContent.content;

        WatchingSession entity = queryFactory
            .selectFrom(ws)
            .join(ws.watcher, u).fetchJoin()
            .join(ws.content, c).fetchJoin()
            .where(u.id.eq(watcherId))
            .orderBy(ws.createdAt.desc(), ws.id.desc())
            .fetchFirst();

        // 스웨거: nullable -> 없으면 null 반환(200 OK)
        return entity == null ? null : WatchingSessionDto.from(entity);
    }

    @Override
    public CursorResponseWatchingSessionDto getWatchingSessionByContent(UUID contentId,
        WatchingSessionSearchRequest request) {
        if (contentId == null) throw new IllegalArgumentException("contentId는 필수입니다.");
        if (request == null) throw new IllegalArgumentException("request는 필수입니다.");


        int limit = request.limit();
        if (limit <= 0) throw new IllegalArgumentException("limit은 1이상이어야 합니다.");

        boolean desc = request.sortDirection() == WatchingSessionSearchRequest.SortDirection.DESCENDING;

        QWatchingSession ws = QWatchingSession.watchingSession;
        QUser u = QUser.user;
        QContent c = QContent.content;

        BooleanBuilder where = new BooleanBuilder();
        where.and(ws.content.id.eq(contentId));

        if (StringUtils.hasText(request.watcherNameLike())) {
            where.and(u.name.containsIgnoreCase(request.watcherNameLike().trim()));
        }

        LocalDateTime cursor = parseCursor(request.cursor());
        UUID idAfter = parseUuid(request.idAfter());

        if (cursor != null) {
            if (desc) {
                // createdAt < cursor OR (createdAt = cursor AND id < idAfter)
                BooleanBuilder cursorCond = new BooleanBuilder();
                cursorCond.or(ws.createdAt.lt(cursor));
                if (idAfter != null) cursorCond.or(ws.createdAt.eq(cursor).and(ws.id.lt(idAfter)));
                where.and(cursorCond);
            } else {
                // createdAt > cursor OR (createdAt = cursor AND id > idAfter)
                BooleanBuilder cursorCond = new BooleanBuilder();
                cursorCond.or(ws.createdAt.gt(cursor));
                if (idAfter != null) cursorCond.or(ws.createdAt.eq(cursor).and(ws.id.gt(idAfter)));
                where.and(cursorCond);
            }
        }

        Order order = desc ? Order.DESC : Order.ASC;
        OrderSpecifier<?>[] orderBy = new OrderSpecifier<?>[] {
            new OrderSpecifier<>(order, ws.createdAt),
            new OrderSpecifier<>(order, ws.id)
        };

        Long totalCount = queryFactory
            .select(ws.count())
            .from(ws)
            .join(ws.watcher, u)
            .where(where)
            .fetchOne();

        List<WatchingSession> rows = queryFactory
            .selectFrom(ws)
            .join(ws.watcher, u).fetchJoin()
            .join(ws.content, c).fetchJoin()
            .where(where)
            .orderBy(orderBy)
            .limit((long) limit + 1)
            .fetch();

        boolean hasNext = rows.size() > limit;
        if (hasNext) rows = rows.subList(0, limit);

        List<WatchingSessionDto> data = rows.stream()
                                            .map(WatchingSessionDto::from)
                                            .collect(Collectors.toList()); // Java 11 호환

        String nextCursor = null;
        UUID nextIdAfter = null;
        if (hasNext && !rows.isEmpty()) {
            WatchingSession last = rows.get(rows.size() - 1);
            nextCursor = last.getCreatedAt().toString();
            nextIdAfter = last.getId();
        }

        return CursorResponseWatchingSessionDto.builder()
                                               .data(data)
                                               .nextCursor(nextCursor)
                                               .nextIdAfter(nextIdAfter)
                                               .hasNext(hasNext)
                                               .totalCount(totalCount)
                                               .sortBy(request.sortBy().name())
                                               .sortDirection(request.sortDirection().name())
                                               .build();

    }


    private LocalDateTime parseCursor(String cursor) {
        if (!StringUtils.hasText(cursor)) {
            return null;
        }
        try {
            return LocalDateTime.parse(cursor.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("cursor 형식이 올바르지 않습니다. (ISO-8601 LocalDateTime 필요)");
        }
    }

    private UUID parseUuid(String rawUuid) {
        if (!StringUtils.hasText(rawUuid)) {
            return null;
        }
        try {
            return UUID.fromString(rawUuid.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("idAfter 형식이 올바르지 않습니다. (UUID 필요)");


        }
    }




}