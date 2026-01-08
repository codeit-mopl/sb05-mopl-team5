package com.mopl.api.domain.notification.exception;

import com.mopl.api.global.config.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum NotificationErrorCode implements ErrorCode {
    NOTIFICATION_NOT_FOUND("N001", "존재하지 않는 알림입니다.", HttpStatus.NOT_FOUND),
    NOTIFICATION_UNAUTHORIZED("N002", "알림에 접근할 권한이 없습니다.", HttpStatus.UNAUTHORIZED),
    ;

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}