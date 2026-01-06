package com.mopl.api.domain.user.exception;

import com.mopl.api.global.config.exception.ErrorCode;
import com.mopl.api.global.config.exception.MoplException;

public class WatchingSessionException extends MoplException {

    public WatchingSessionException(ErrorCode errorCode) {
        super(errorCode);
    }

    public WatchingSessionException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}