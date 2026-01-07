package com.mopl.api.domain.review.exception;

import com.mopl.api.global.config.exception.ErrorCode;
import com.mopl.api.global.config.exception.MoplException;

public class ReviewErrorException extends MoplException {

    public ReviewErrorException(ErrorCode errorCode) {
        super(errorCode);
    }

    public ReviewErrorException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
