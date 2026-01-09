package com.mopl.api.domain.user.exception.auth.detail;

import com.mopl.api.domain.user.exception.auth.AuthErrorCode;
import com.mopl.api.domain.user.exception.auth.AuthException;

public class JwtInformationNotFoundException extends AuthException {

    public JwtInformationNotFoundException() {
        super(AuthErrorCode.JWT_INFORMATION_NOT_FOUND);
    }
}
