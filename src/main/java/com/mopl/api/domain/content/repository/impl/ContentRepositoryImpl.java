package com.mopl.api.domain.content.repository.impl;

import static com.mopl.api.domain.content.entity.QContent.content;

import com.mopl.api.domain.content.dto.request.ContentSearchRequest;
import com.mopl.api.domain.content.entity.Content;
import com.mopl.api.domain.content.entity.ContentType;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ContentRepositoryImpl implements ContentRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Content> findContentsByCursor(ContentSearchRequest request) {

        return queryFactory
            .selectFrom(content)
            .where(content.isDeleted.eq(false))
            .where(request.typeEqual() != null ? content.type.eq(ContentType.findByValue(request.typeEqual())) : null)
            .where(request.keywordLike() != null ? content.title.containsIgnoreCase(request.keywordLike()) : null)
            .where(buildOrderSpecifiers(request))
            .orderBy(buildOrderSpecifiers(request.sortDirection(), request.sortBy()))
            .limit(request.limit() + 1)
            .fetch();
    }

    public Long countContents(ContentSearchRequest request) {
        return queryFactory
            .select(content.count())
            .from(content)
            .where(content.isDeleted.eq(false))
            .where(request.typeEqual() != null ? content.type.eq(ContentType.findByValue(request.typeEqual())) : null)
            .where(request.keywordLike() != null ? content.title.containsIgnoreCase(request.keywordLike()) : null)
            .fetchOne();
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
                String[] parts = request.cursor()
                                        .split("\\|");
                Long cursorWatcher = Long.parseLong(parts[0]);
                Long cursorReview = Long.parseLong(parts[1]);
                return isDesc
                    ? content.watcherCount.lt(cursorWatcher)
                                          .or(content.watcherCount.eq(cursorWatcher)
                                                                  .and(content.reviewCount.lt(cursorReview)))
                                          .or(content.watcherCount.eq(cursorWatcher)
                                                                  .and(content.reviewCount.eq(cursorReview))
                                                                  .and(content.id.lt(request.idAfter())))
                    : content.watcherCount.gt(cursorWatcher)
                                          .or(content.watcherCount.eq(cursorWatcher)
                                                                  .and(content.reviewCount.gt(cursorReview)))
                                          .or(content.watcherCount.eq(cursorWatcher)
                                                                  .and(content.reviewCount.eq(cursorReview))
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