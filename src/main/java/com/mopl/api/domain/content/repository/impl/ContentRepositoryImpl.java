package com.mopl.api.domain.content.repository.impl;

import static com.mopl.api.domain.content.entity.QContent.content;

import com.mopl.api.domain.content.dto.request.ContentSearchRequest;
import com.mopl.api.domain.content.dto.response.CursorResponseContentDto;
import com.mopl.api.domain.content.entity.Content;
import com.mopl.api.domain.content.entity.ContentType;
import com.mopl.api.domain.content.mapper.ContentMapper;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ContentRepositoryImpl implements ContentRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private final ContentMapper contentMapper;

    @Override
    public CursorResponseContentDto findContentsByCursor(ContentSearchRequest request) {

        List<Content> contents = queryFactory
            .selectFrom(content)
            .where(content.isDeleted.eq(false))
            .where(request.typeEqual() != null ? content.type.eq(ContentType.findByValue(request.typeEqual())) : null)
            .where(request.keywordLike() != null ? content.title.containsIgnoreCase(request.keywordLike()) : null)
            .where(buildOrderSpecifiers(request))
            .orderBy(buildOrderSpecifiers(request.sortDirection(), request.sortBy()))
            .limit(request.limit() + 1)
            .fetch();

        Long totalCount = queryFactory
            .select(content.count())
            .from(content)
            .where(content.isDeleted.eq(false))
            .where(request.typeEqual() != null ? content.type.eq(ContentType.findByValue(request.typeEqual())) : null)
            .where(request.keywordLike() != null ? content.title.containsIgnoreCase(request.keywordLike()) : null)
            .fetchOne();

        String nextCursor = null;
        UUID nextIdAfter = null;

        boolean hasNext = contents.size() > request.limit();
        if (hasNext) {
            contents = contents.subList(0, request.limit());

            Content last = contents.get(contents.size() - 1);

            nextCursor = switch (request.sortBy()) {
                case "createdAt" -> last.getCreatedAt()
                                        .toString();
                case "watcherCount" -> last.getWatcherCount()
                                           .toString();
                case "rate" -> last.getAverageRating()
                                   .toString();
                default -> throw new IllegalStateException("Unexpected value: " + request.sortBy());
            };
            nextIdAfter = last.getId();
        }

        return CursorResponseContentDto.builder()
                                       .data(contents.stream()
                                                     .map(contentMapper::toDto)
                                                     .toList())
                                       .nextCursor(nextCursor)
                                       .nextIdAfter(nextIdAfter)
                                       .hasNext(hasNext)
                                       .totalCount(totalCount)
                                       .sortDirection(request.sortDirection())
                                       .sortBy(request.sortBy())
                                       .build();
    }

    public BooleanExpression buildOrderSpecifiers(ContentSearchRequest request) {
        if (request.cursor() == null || request.idAfter() == null) {
            return null;
        }
        boolean isDesc = "DESCENDING".equalsIgnoreCase(request.sortDirection());

        switch (request.sortBy()) {
            case "createdAt" -> {
                LocalDateTime cursorTime = LocalDateTime.parse(request.cursor());
                return isDesc ? content.createdAt.lt(cursorTime)
                                                 .or(content.createdAt.eq(cursorTime)
                                                                      .and(content.id.lt(request.idAfter())))
                    : content.createdAt.gt(cursorTime)
                                       .or(content.createdAt.eq(cursorTime)
                                                            .and(content.id.gt(request.idAfter())));
            }
            case "watcherCount" -> {
                Long cursorWatcher = Long.parseLong(request.cursor());
                return isDesc ? content.watcherCount.lt(cursorWatcher)
                                                    .or(content.watcherCount.eq(cursorWatcher)
                                                                            .and(content.id.lt(request.idAfter())))
                    : content.watcherCount.gt(cursorWatcher)
                                          .or(content.watcherCount.eq(cursorWatcher)
                                                                  .and(content.id.gt(request.idAfter())));
            }
            case "rate" -> {
                BigDecimal cursorRate = new BigDecimal(request.cursor());
                return isDesc ? content.averageRating.lt(cursorRate)
                                                     .or(content.averageRating.eq(cursorRate)
                                                                              .and(content.id.lt(request.idAfter())))
                    : content.averageRating.gt(cursorRate)
                                           .or(content.averageRating.eq(cursorRate)
                                                                    .and(content.id.gt(request.idAfter())));
            }
        }
        throw new IllegalStateException("Unexpected value: " + request.sortBy());
    }

    public OrderSpecifier<?>[] buildOrderSpecifiers(String sortDirection, String sortBy) {
        Order order = sortDirection.equals("DESCENDING") ? Order.DESC : Order.ASC;

        switch (sortBy) {
            case "createdAt" -> {
                return new OrderSpecifier[]{new OrderSpecifier<>(order, content.createdAt)};
            }
            case "watcherCount" -> {
                return new OrderSpecifier[]{new OrderSpecifier<>(order, content.watcherCount),
                    new OrderSpecifier<>(order, content.reviewCount),
                    new OrderSpecifier<>(order, content.id)};
            }
            case "rate" -> {
                return new OrderSpecifier[]{new OrderSpecifier<>(order, content.averageRating),
                    new OrderSpecifier<>(order, content.id)};
            }
        }
        throw new IllegalStateException("Unexpected value: " + sortBy);
    }
}