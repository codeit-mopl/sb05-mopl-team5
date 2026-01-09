package com.mopl.api.domain.user.exception.auth;

import com.mopl.api.global.config.exception.ErrorCode;
import com.mopl.api.global.config.exception.MoplException;

public class AuthException extends MoplException {

    public AuthException(ErrorCode errorCode) {
        super(errorCode);
    }
}
