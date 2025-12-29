package com.mopl.api.global.config.exception;

import org.springframework.http.HttpStatus;

public interface ErrorCode {

    String name();

    String getMessage();

    HttpStatus getHttpStatus();
}
