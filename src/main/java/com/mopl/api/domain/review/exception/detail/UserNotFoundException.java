package com.mopl.api.domain.review.exception.detail;

import com.mopl.api.domain.review.exception.ReviewErrorCode;
import com.mopl.api.domain.review.exception.ReviewErrorException;
import java.util.UUID;

public class UserNotFoundException extends ReviewErrorException {

    public UserNotFoundException() {
        super(ReviewErrorCode.USER_NOT_FOUND);
    }

    public static UserNotFoundException withUserId(UUID userId) {
        UserNotFoundException exception = new UserNotFoundException();
        exception.addDetail("userId", userId);
        return exception;
    }
}
