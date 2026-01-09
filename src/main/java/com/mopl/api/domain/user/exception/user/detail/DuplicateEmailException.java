package com.mopl.api.domain.user.exception.user.detail;

import com.mopl.api.domain.user.exception.user.UserErrorCode;
import com.mopl.api.domain.user.exception.user.UserException;

public class DuplicateEmailException extends UserException {

    public DuplicateEmailException() {
        super(UserErrorCode.DUPLICATE_EMAIL);
    }

    public static DuplicateEmailException withUserEmail(String email) {
        DuplicateEmailException exception = new DuplicateEmailException();
        exception.addDetail("email", email);
        return exception;
    }
}
