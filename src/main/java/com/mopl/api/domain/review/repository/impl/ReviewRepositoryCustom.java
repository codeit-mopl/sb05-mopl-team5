package com.mopl.api.domain.review.repository.impl;

import com.mopl.api.domain.review.entity.Review;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.cache.annotation.Cacheable;

public interface ReviewRepositoryCustom {

    List<Review> findReviewsWithCursor(
        UUID contentId,
        String sortBy,
        String sortDirection,
        LocalDateTime cursorDateTime,
        BigDecimal cursorRating,
        UUID idAfter,
        int limit
    );

    @Cacheable(value = "reviewCount", key = "#contentId")
    long countReviewsByContentId(UUID contentId);

    List<Review> findActiveReviewsByContentId(UUID contentId);
}
