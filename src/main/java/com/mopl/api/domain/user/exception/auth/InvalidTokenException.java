package com.mopl.api.domain.user.exception.auth;

public class InvalidTokenException extends AuthException {

    public InvalidTokenException() {
        super(AuthErrorCode.TOKEN_INVALID);
    }
}
