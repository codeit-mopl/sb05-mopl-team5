package com.mopl.api.domain.user.exception.detail;

import com.mopl.api.domain.user.exception.UserErrorCode;
import com.mopl.api.domain.user.exception.UserErrorException;
import java.util.UUID;

public class UserNotFoundException extends UserErrorException {

    public UserNotFoundException() {
        super(UserErrorCode.USER_NOT_FOUND);
    }

    public static UserNotFoundException withUserId(UUID userId) {
        UserNotFoundException exception = new UserNotFoundException();
        exception.addDetail("userId", userId);
        return exception;
    }
}
