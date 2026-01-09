package com.mopl.api.domain.review.repository.impl;

import com.mopl.api.domain.review.entity.QReview;
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
        // Q클래스 인스턴스 생성 (엔티티 필드에 타입 안전하게 접근하기 위한 객체)
        QReview review = QReview.review;

        // 기본 조건: 삭제되지 않은 리뷰만 조회
        BooleanExpression predicate = review.isDeleted.eq(false);

        // contentId가 주어진 경우, 해당 콘텐츠의 리뷰만 필터링
        if (contentId != null) {
            predicate = predicate.and(review.content.id.eq(contentId));
        }

        // 커서 조건 추가 (다음 페이지 조회 시)
        // idAfter: 마지막으로 조회한 리뷰의 ID
        if (idAfter != null) {
            predicate = predicate.and(
                buildCursorPredicate(review, sortBy, sortDirection, cursorDateTime, cursorRating, idAfter));
        }

        // 정렬 조건 생성
        OrderSpecifier<?> primaryOrder = buildOrderSpecifier(review, sortBy, sortDirection);

        // Tie-breaker: 정렬 기준이 같은 경우를 위한 추가 정렬 (ID로 정렬)
        // 예: 같은 시간에 생성된 리뷰들을 ID로 구분하여 일관된 순서 보장
        OrderSpecifier<UUID> tieBreaker = new OrderSpecifier<>(
            "DESCENDING".equals(sortDirection) ? Order.DESC : Order.ASC,
            review.id
        );

        // Querydsl 쿼리 실행
        return queryFactory
            .selectFrom(review)           // SELECT * FROM reviews
            .where(predicate)              // WHERE 조건들
            .orderBy(primaryOrder, tieBreaker)  // ORDER BY 절
            .limit(limit + 1)              // LIMIT (다음 페이지 존재 여부 확인을 위해 +1)
            .fetch();                      // 쿼리 실행 및 결과 리스트 반환
    }

    @Override
    public long countReviewsByContentId(UUID contentId) {
        QReview review = QReview.review;

        // 기본 조건: 삭제되지 않은 리뷰만
        BooleanExpression predicate = review.isDeleted.eq(false);

        // contentId 필터 적용
        if (contentId != null) {
            predicate = predicate.and(review.content.id.eq(contentId));
        }

        // COUNT 쿼리 실행
        return queryFactory
            .selectFrom(review)
            .where(predicate)
            .fetchCount();  // SELECT COUNT(*) 실행
    }

    private BooleanExpression buildCursorPredicate(
        QReview review,
        String sortBy,
        String sortDirection,
        LocalDateTime cursorDateTime,
        BigDecimal cursorRating,
        UUID idAfter
    ) {
        // 내림차순 여부 확인
        boolean isDescending = "DESCENDING".equals(sortDirection);

        // 생성일 기준 정렬인 경우
        if ("createdAt".equals(sortBy) && cursorDateTime != null) {
            if (isDescending) {
                // 내림차순: 커서보다 이전 데이터 조회
                // (작은 값) OR (같은 값이면서 ID가 작은 것)
                return review.createdAt.lt(cursorDateTime)
                                       .or(review.createdAt.eq(cursorDateTime)
                                                           .and(review.id.lt(idAfter)));
            } else {
                // 오름차순: 커서보다 이후 데이터 조회
                return review.createdAt.gt(cursorDateTime)
                                       .or(review.createdAt.eq(cursorDateTime)
                                                           .and(review.id.gt(idAfter)));
            }
        }
        // 평점 기준 정렬인 경우
        else if ("rating".equals(sortBy) && cursorRating != null) {
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

        // 커서 조건을 생성할 수 없는 경우 null 반환 (조건 없음)
        return null;
    }

    private OrderSpecifier<?> buildOrderSpecifier(QReview review, String sortBy, String sortDirection) {
        // 정렬 방향 결정 (ASC or DESC)
        Order order = "DESCENDING".equals(sortDirection) ? Order.DESC : Order.ASC;

        // 정렬 기준 필드에 따라 OrderSpecifier 생성
        if ("createdAt".equals(sortBy)) {
            return new OrderSpecifier<>(order, review.createdAt);
        } else if ("rating".equals(sortBy)) {
            return new OrderSpecifier<>(order, review.rating);
        }

        // 기본값: createdAt으로 정렬
        return new OrderSpecifier<>(order, review.createdAt);
    }
}
