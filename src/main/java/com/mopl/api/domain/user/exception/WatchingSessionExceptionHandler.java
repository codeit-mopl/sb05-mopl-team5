package com.mopl.api.domain.user.exception;

import com.mopl.api.global.config.exception.ErrorCode;
import java.net.URI;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class WatchingSessionExceptionHandler {

    @ExceptionHandler(WatchingSessionException.class)
    public ProblemDetail handleWatchingSessionException(WatchingSessionException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.error(e.getMessage(), e);
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(errorCode.getHttpStatus(), errorCode.getMessage());

        // TODO error URI 처리
        pd.setType(URI.create("/errors/" + errorCode.getCode()));
        pd.setTitle(errorCode.name());
        pd.setProperty("code", errorCode.getCode());
        pd.setDetail(errorCode.getMessage());
        pd.setStatus(errorCode.getHttpStatus());

        return pd;
    }
}