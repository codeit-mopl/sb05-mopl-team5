package com.mopl.api.domain.review.exception.detail;

import com.mopl.api.domain.review.exception.ReviewErrorCode;
import com.mopl.api.domain.review.exception.ReviewErrorException;
import java.util.UUID;

public class ContentNotFoundException extends ReviewErrorException {

    public ContentNotFoundException() {
        super(ReviewErrorCode.CONTENT_NOT_FOUND);
    }

    public static ContentNotFoundException withContentId(UUID contentId) {
        ContentNotFoundException exception = new ContentNotFoundException();
        exception.addDetail("contentId", contentId);
        return exception;
    }
}
