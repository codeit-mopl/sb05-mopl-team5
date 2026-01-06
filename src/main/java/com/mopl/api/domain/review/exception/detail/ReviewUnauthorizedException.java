package com.mopl.api.domain.review.exception.detail;

import com.mopl.api.domain.review.exception.ReviewErrorCode;
import com.mopl.api.domain.review.exception.ReviewErrorException;
import java.util.UUID;

public class ReviewUnauthorizedException extends ReviewErrorException {

    public ReviewUnauthorizedException() {
        super(ReviewErrorCode.REVIEW_UNAUTHORIZED);
    }

    public static ReviewUnauthorizedException withDetails(UUID reviewId, UUID userId) {
        ReviewUnauthorizedException exception = new ReviewUnauthorizedException();
        exception.addDetail("reviewId", reviewId);
        exception.addDetail("userId", userId);
        return exception;
    }
}
