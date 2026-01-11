package com.mopl.api.domain.review.repository.impl;

import static com.mopl.api.domain.review.entity.QReview.review;

import com.mopl.api.domain.review.entity.Review;
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
public class ReviewRepositoryImpl implements ReviewRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Review> findReviewsWithCursor(
        UUID contentId,
        String sortBy,
        String sortDirection,
        LocalDateTime cursorDateTime,
        BigDecimal cursorRating,
        UUID idAfter,
        int limit
    ) {

        // 기본 조건: 삭제되지 않은 리뷰만 조회
        BooleanExpression predicate = review.isDeleted.eq(false);

        // contentId가 주어진 경우, 해당 콘텐츠의 리뷰만 필터링
        if (contentId != null) {
            predicate = predicate.and(review.content.id.eq(contentId));
        }

        if (idAfter != null) {
            predicate = predicate.and(
                buildCursorPredicate(sortBy, sortDirection, cursorDateTime, cursorRating, idAfter));
        }

        OrderSpecifier<?> primaryOrder = buildOrderSpecifier(sortBy, sortDirection);

        OrderSpecifier<UUID> tieBreaker = new OrderSpecifier<>(
            "DESCENDING".equals(sortDirection) ? Order.DESC : Order.ASC,
            review.id
        );

        return queryFactory
            .selectFrom(review)
            .where(predicate)
            .orderBy(primaryOrder, tieBreaker)
            .limit(limit + 1)
            .fetch();
    }

    @Override
    public long countReviewsByContentId(UUID contentId) {
        BooleanExpression predicate = review.isDeleted.eq(false);

        if (contentId != null) {
            predicate = predicate.and(review.content.id.eq(contentId));
        }

        return queryFactory
            .selectFrom(review)
            .where(predicate)
            .fetchCount();
    }

    private BooleanExpression buildCursorPredicate(
        String sortBy,
        String sortDirection,
        LocalDateTime cursorDateTime,
        BigDecimal cursorRating,
        UUID idAfter
    ) {
        boolean isDescending = "DESCENDING".equals(sortDirection);

        if ("createdAt".equals(sortBy) && cursorDateTime != null) {
            if (isDescending) {
                return review.createdAt.lt(cursorDateTime)
                                       .or(review.createdAt.eq(cursorDateTime)
                                                           .and(review.id.lt(idAfter)));
            } else {
                return review.createdAt.gt(cursorDateTime)
                                       .or(review.createdAt.eq(cursorDateTime)
                                                           .and(review.id.gt(idAfter)));
            }
        } else if ("rating".equals(sortBy) && cursorRating != null) {
            if (isDescending) {
                return review.rating.lt(cursorRating)
                                    .or(review.rating.eq(cursorRating)
                                                     .and(review.id.lt(idAfter)));
            } else {
                return review.rating.gt(cursorRating)
                                    .or(review.rating.eq(cursorRating)
                                                     .and(review.id.gt(idAfter)));
            }
        }

        return null;
    }

    private OrderSpecifier<?> buildOrderSpecifier(String sortBy, String sortDirection) {
        Order order = "DESCENDING".equals(sortDirection) ? Order.DESC : Order.ASC;

        if ("createdAt".equals(sortBy)) {
            return new OrderSpecifier<>(order, review.createdAt);
        } else if ("rating".equals(sortBy)) {
            return new OrderSpecifier<>(order, review.rating);
        }

        return new OrderSpecifier<>(order, review.createdAt);
    }
}
