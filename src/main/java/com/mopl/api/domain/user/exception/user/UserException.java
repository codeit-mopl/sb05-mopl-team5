package com.mopl.api.domain.user.exception.user;

import com.mopl.api.global.config.exception.ErrorCode;
import com.mopl.api.global.config.exception.MoplException;

public class UserException extends MoplException {

    public UserException(ErrorCode errorCode) {
        super(errorCode);
    }
}
