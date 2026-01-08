package com.mopl.api.domain.user.exception.auth;

import com.mopl.api.global.config.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AuthErrorCode implements ErrorCode {

    TOKEN_INVALID("A001", "Invalid Token", HttpStatus.UNAUTHORIZED),
    JWT_INFORMATION_NOT_FOUND("A002", "JWTInformation not found", HttpStatus.UNAUTHORIZED);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
