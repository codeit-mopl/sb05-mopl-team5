package com.mopl.api.domain.user.exception.user.detail;

import com.mopl.api.domain.user.exception.user.UserErrorCode;
import com.mopl.api.domain.user.exception.user.UserException;
import java.util.UUID;

public class UserNotFoundException extends UserException {

    public UserNotFoundException() {
        super(UserErrorCode.USER_NOT_FOUND);
    }

    public static UserNotFoundException withUserId(UUID userId) {
        UserNotFoundException exception = new UserNotFoundException();
        exception.addDetail("userId", userId);
        return exception;
    }

    public static UserNotFoundException withUserEmail(String email) {
        UserNotFoundException exception = new UserNotFoundException();
        exception.addDetail("email", email);
        return exception;
    }
}
