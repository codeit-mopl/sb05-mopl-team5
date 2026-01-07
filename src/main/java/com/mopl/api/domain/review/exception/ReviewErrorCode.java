package com.mopl.api.domain.review.exception;

import com.mopl.api.global.config.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ReviewErrorCode implements ErrorCode {
    REVIEW_NOT_FOUND("RV001", "존재하지 않는 리뷰입니다.", HttpStatus.NOT_FOUND),
    REVIEW_UNAUTHORIZED("RV002", "리뷰에 대한 권한이 없습니다.", HttpStatus.FORBIDDEN),
    USER_NOT_FOUND("RV003", "존재하지 않는 사용자입니다.", HttpStatus.NOT_FOUND),
    CONTENT_NOT_FOUND("RV004", "존재하지 않는 콘텐츠입니다.", HttpStatus.NOT_FOUND),
    REVIEW_ALREADY_EXISTS("RV005", "이미 리뷰를 작성한 콘텐츠입니다.", HttpStatus.CONFLICT),
    ;

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
