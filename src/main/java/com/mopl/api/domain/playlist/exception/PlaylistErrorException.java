package com.mopl.api.domain.playlist.exception;

import com.mopl.api.global.config.exception.ErrorCode;
import com.mopl.api.global.config.exception.MoplException;

public class PlaylistErrorException extends MoplException {

    public PlaylistErrorException(ErrorCode errorCode) {
        super(errorCode);
    }

    public PlaylistErrorException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
