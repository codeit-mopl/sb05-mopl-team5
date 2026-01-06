package com.mopl.api.domain.playlist.exception.detail;

import com.mopl.api.domain.playlist.exception.PlaylistErrorCode;
import com.mopl.api.domain.playlist.exception.PlaylistErrorException;
import java.util.UUID;

public class UserNotFoundException extends PlaylistErrorException {

    public UserNotFoundException() {
        super(PlaylistErrorCode.USER_NOT_FOUND);
    }

    public static UserNotFoundException withUserId(UUID userId) {
        UserNotFoundException exception = new UserNotFoundException();
        exception.addDetail("userId", userId);
        return exception;
    }
}
