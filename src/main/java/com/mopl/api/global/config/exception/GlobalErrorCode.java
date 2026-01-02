package com.mopl.api.global.config.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum GlobalErrorCode implements ErrorCode {

    ERROR_CODE("INTERNAL_SERVER_ERROR","message", HttpStatus.INTERNAL_SERVER_ERROR),
    ;

    private final String name;
    private final String message;
    private final HttpStatus httpStatus;

    GlobalErrorCode(String name, String message, HttpStatus httpStatus) {
        this.name = name;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
