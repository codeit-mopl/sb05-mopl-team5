package com.mopl.api.domain.user.repository.impl;

import static com.mopl.api.domain.user.entity.QUser.user;

import com.mopl.api.domain.user.dto.request.CursorRequestUserDto;
import com.mopl.api.domain.user.dto.response.CursorResponseUserDto;
import com.mopl.api.domain.user.dto.response.UserDto;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserRepositoryCustomImpl implements UserRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public CursorResponseUserDto<UserDto> findAllUsers(CursorRequestUserDto request) {
        // sortBy / sortDirection 기본값(혹시라도 null 방어)
        String sortBy = (request.sortBy() == null
            || request.sortBy().isBlank()) ? "createdAt" : request.sortBy();

        String sortDirection = (request.sortDirection() == null
            || request.sortDirection().isBlank()) ? "ASCENDING" : request.sortDirection();

        // 필터(where) 구성
        BooleanBuilder where = new BooleanBuilder();

        if (request.emailLike() != null && !request.emailLike().isBlank()) {
            where.and(user.email.containsIgnoreCase(request.emailLike()));
        }

        if (request.roleEqual() != null) {
            where.and(user.role.eq(request.roleEqual()));
        }

        if (request.isLocked() != null) {
            where.and(user.locked.eq(request.isLocked()));
        }

        // 커서 조건 추가
        where.and(cursorPredicate(request, sortBy, sortDirection));

        // 정렬 구성 (id ASC 타이브레이커)
        List<OrderSpecifier<?>> orderSpecifiers = buildOrderSpecifiers(sortBy, sortDirection);

        List<UserDto> results = queryFactory
            .select(Projections.constructor(
                UserDto.class,
                user.id,
                user.createdAt,
                user.email,
                user.name,
                user.profileImageUrl,
                user.role,
                user.locked
            ))
            .from(user)
            .where(where)
            .orderBy(orderSpecifiers.toArray(OrderSpecifier[]::new))
            .limit(request.limit().longValue() + 1L) // hasNext 판별용 +1
            .fetch();

        // totalCount (필터만 적용, 커서 조건 제외)
        Long totalCountTemp = queryFactory
            .select(user.count())
            .from(user)
            .where(buildFilterOnlyPredicate(request))
            .fetchOne();

        long totalCount = (totalCountTemp != null) ? totalCountTemp : 0L;
        boolean hasNext = results.size() > request.limit();

        String nextCursor = null;
        UUID nextIdAfter = null;

        if (hasNext) {
            results.remove(results.size() - 1);
            UserDto last = results.get(results.size() - 1);

            nextCursor = extractCursorValue(last, sortBy);
            nextIdAfter = last.id();
        }

        CursorResponseUserDto<UserDto> response =
            CursorResponseUserDto.<UserDto>builder()
                                    .data(results)
                                    .nextCursor(nextCursor)
                                    .nextIdAfter(nextIdAfter)
                                    .hasNext(hasNext)
                                    .totalCount(totalCount)
                                    .sortBy(sortBy)
                                    .sortDirection(sortDirection)
                .build();

        return response;
    }


    // 커서 조건: 정렬이 (sortBy + sortDirection) 일 때, 다음 페이지 후보만 남기는 조건을 만든다.
    private BooleanExpression cursorPredicate(CursorRequestUserDto request, String sortBy, String sortDirection) {

        // 첫 페이지면 커서 조건 없음
        if (request.cursor() == null || request.cursor().isBlank() || request.idAfter() == null) {
            return null;
        }

        boolean asc = "ASCENDING".equalsIgnoreCase(sortDirection);

        return switch (sortBy) {
            // createdAt: cursor는 ISO LocalDateTime 문자열
            case "createdAt" -> {
                LocalDateTime after = LocalDateTime.parse(request.cursor());
                yield asc
                    ? user.createdAt.gt(after)
                                    .or(user.createdAt.eq(after).and(user.id.gt(request.idAfter())))
                    : user.createdAt.lt(after)
                                    .or(user.createdAt.eq(after).and(user.id.gt(request.idAfter())));
            }

            case "email" -> asc
                ? user.email.gt(request.cursor())
                            .or(user.email.eq(request.cursor())
                                          .and(user.id.gt(request.idAfter())))
                : user.email.lt(request.cursor())
                            .or(user.email.eq(request.cursor())
                                          .and(user.id.gt(request.idAfter())));

            case "name" -> asc
                ? user.name.gt(request.cursor())
                           .or(user.name.eq(request.cursor())
                                        .and(user.id.gt(request.idAfter())))
                : user.name.lt(request.cursor())
                           .or(user.name.eq(request.cursor())
                                        .and(user.id.gt(request.idAfter())));

            case "role" -> asc
                ? user.role.stringValue()
                           .gt(request.cursor())
                           .or(user.role.stringValue()
                                        .eq(request.cursor())
                                        .and(user.id.gt(request.idAfter())))
                : user.role.stringValue()
                           .lt(request.cursor())
                           .or(user.role.stringValue()
                                        .eq(request.cursor())
                                        .and(user.id.gt(request.idAfter())));

            case "isLocked" -> asc
                ? user.locked.stringValue()
                             .gt(request.cursor())
                             .or(user.locked.stringValue()
                                            .eq(request.cursor())
                                            .and(user.id.gt(request.idAfter())))
                : user.locked.stringValue()
                             .lt(request.cursor())
                             .or(user.locked.stringValue()
                                            .eq(request.cursor())
                                            .and(user.id.gt(request.idAfter())));

            default -> {
                LocalDateTime after = LocalDateTime.parse(request.cursor());
                yield asc
                    ? user.createdAt.gt(after)
                                    .or(user.createdAt.eq(after)
                                                      .and(user.id.gt(request.idAfter())))
                    : user.createdAt.lt(after)
                                    .or(user.createdAt.eq(after)
                                                      .and(user.id.gt(request.idAfter())));
            }
        };
    }

    //  1차: sortBy + sortDirection   2차: id ASC
    private List<OrderSpecifier<?>> buildOrderSpecifiers(String sortBy, String sortDirection) {

        boolean asc = "ASCENDING".equalsIgnoreCase(sortDirection);

        OrderSpecifier<?> primary = switch (sortBy) {
            case "createdAt" -> asc ? user.createdAt.asc() : user.createdAt.desc();
            case "email" -> asc ? user.email.asc() : user.email.desc();
            case "name" -> asc ? user.name.asc() : user.name.desc();
            case "role" -> asc ? user.role.asc() : user.role.desc();
            case "isLocked" -> asc ? user.locked.asc() : user.locked.desc();
            default -> asc ? user.createdAt.asc() : user.createdAt.desc();
        };

        return List.of(primary, user.id.asc()); // 타이 브레이커
    }

    private BooleanExpression buildFilterOnlyPredicate(CursorRequestUserDto request) {
        BooleanBuilder where = new BooleanBuilder();

        if (request.emailLike() != null && !request.emailLike().isBlank()) {
            where.and(user.email.containsIgnoreCase(request.emailLike()));
        }
        if (request.roleEqual() != null) {
            where.and(user.role.eq(request.roleEqual()));
        }
        if (request.isLocked() != null) {
            where.and(user.locked.eq(request.isLocked()));
        }

        return (BooleanExpression) where.getValue();
    }

    private String extractCursorValue(UserDto last, String sortBy) {
        return switch (sortBy) {
            case "createdAt" -> last.createdAt().toString();
            case "email" -> last.email();
            case "name" -> last.name();
            case "role" -> String.valueOf(last.role());
            case "isLocked" -> String.valueOf(last.locked());
            default -> last.createdAt()
                           .toString();
        };
    }
}