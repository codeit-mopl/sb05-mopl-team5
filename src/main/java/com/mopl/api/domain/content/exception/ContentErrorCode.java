package com.mopl.api.domain.content.exception;

import com.mopl.api.global.config.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ContentErrorCode implements ErrorCode {
    CONTENT_NOT_FOUND("CT001", "존재하지 않는 콘텐츠입니다.", HttpStatus.NOT_FOUND),
    ;

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
