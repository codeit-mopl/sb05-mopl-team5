package com.mopl.api.domain.review.exception.detail;

import com.mopl.api.domain.review.exception.ReviewErrorCode;
import com.mopl.api.domain.review.exception.ReviewErrorException;
import java.util.UUID;

public class ReviewNotFoundException extends ReviewErrorException {

    public ReviewNotFoundException() {
        super(ReviewErrorCode.REVIEW_NOT_FOUND);
    }

    public static ReviewNotFoundException withReviewId(UUID reviewId) {
        ReviewNotFoundException exception = new ReviewNotFoundException();
        exception.addDetail("reviewId", reviewId);
        return exception;
    }
}
