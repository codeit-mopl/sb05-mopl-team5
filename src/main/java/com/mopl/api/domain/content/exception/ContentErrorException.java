package com.mopl.api.domain.content.exception;

import com.mopl.api.global.config.exception.ErrorCode;
import com.mopl.api.global.config.exception.MoplException;

public class ContentErrorException extends MoplException {

    public ContentErrorException(ErrorCode errorCode) {
        super(errorCode);
    }
}
