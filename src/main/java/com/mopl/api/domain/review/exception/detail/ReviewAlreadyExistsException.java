package com.mopl.api.domain.review.exception.detail;

import com.mopl.api.domain.review.exception.ReviewErrorCode;
import com.mopl.api.domain.review.exception.ReviewErrorException;
import java.util.UUID;

public class ReviewAlreadyExistsException extends ReviewErrorException {

    public ReviewAlreadyExistsException() {
        super(ReviewErrorCode.REVIEW_ALREADY_EXISTS);
    }

    public static ReviewAlreadyExistsException withDetails(UUID contentId, UUID userId) {
        ReviewAlreadyExistsException exception = new ReviewAlreadyExistsException();
        exception.addDetail("contentId", contentId);
        exception.addDetail("userId", userId);
        return exception;
    }
}
