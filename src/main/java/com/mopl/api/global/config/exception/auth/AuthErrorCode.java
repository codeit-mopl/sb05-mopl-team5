package com.mopl.api.global.config.exception.auth;

import com.mopl.api.global.config.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {

    TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "Invalid Token"),
    JWT_INFORMATION_NOT_FOUND(HttpStatus.UNAUTHORIZED, "JWTInformation not found");

    private final HttpStatus status;
    private final String message;

    @Override
    public String getName() {
        return this.name();
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public HttpStatus getHttpStatus() {
        return status;
    }
}
