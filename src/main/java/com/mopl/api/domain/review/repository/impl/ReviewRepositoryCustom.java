package com.mopl.api.domain.review.repository.impl;

import com.mopl.api.domain.review.entity.Review;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

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

    long countReviewsByContentId(UUID contentId);
}
