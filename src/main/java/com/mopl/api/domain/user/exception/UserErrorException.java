package com.mopl.api.domain.user.exception;

import com.mopl.api.global.config.exception.ErrorCode;
import com.mopl.api.global.config.exception.MoplException;

public class UserErrorException extends MoplException {

    public UserErrorException(ErrorCode errorCode) {
        super(errorCode);
    }
}
