package com.mopl.api.domain.user.exception.auth;

public class JwtInformationNotFoundException extends AuthException {

    public JwtInformationNotFoundException() {
        super(AuthErrorCode.JWT_INFORMATION_NOT_FOUND);
    }
}
