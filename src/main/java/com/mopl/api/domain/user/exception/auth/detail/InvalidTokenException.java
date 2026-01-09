package com.mopl.api.domain.user.exception.auth.detail;

import com.mopl.api.domain.user.exception.auth.AuthErrorCode;
import com.mopl.api.domain.user.exception.auth.AuthException;

public class InvalidTokenException extends AuthException {

    public InvalidTokenException() {
        super(AuthErrorCode.TOKEN_INVALID);
    }
}
