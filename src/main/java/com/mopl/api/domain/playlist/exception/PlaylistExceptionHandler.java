package com.mopl.api.domain.playlist.exception;

import com.mopl.api.domain.playlist.exception.PlaylistErrorException;
import com.mopl.api.global.config.exception.ErrorCode;
import java.net.URI;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class PlaylistExceptionHandler {

    @ExceptionHandler(PlaylistErrorException.class)
    public ProblemDetail handleWatchingSessionException(PlaylistErrorException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.error(e.getMessage(), e);
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(errorCode.getHttpStatus(), errorCode.getMessage());

        // TODO error URI 처리
        pd.setType(URI.create("/errors/" + errorCode.getCode()));
        pd.setTitle(errorCode.getCode());
        pd.setProperty("code", errorCode.getCode());

        return pd;
    }
}
