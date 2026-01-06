package com.mopl.api.domain.user.exception.detail;

import com.mopl.api.domain.user.exception.WatchingSessionErrorCode;
import com.mopl.api.domain.user.exception.WatchingSessionException;
import java.util.UUID;

public class WatchingSessionNotFoundException extends WatchingSessionException {

    public WatchingSessionNotFoundException() {
        super(WatchingSessionErrorCode.SESSION_NOT_FOUND);
    }

    public static WatchingSessionNotFoundException withSessionId(UUID sessionId) {
        WatchingSessionNotFoundException exception = new WatchingSessionNotFoundException();
        exception.addDetail("watchingSessionId", sessionId);
        return exception;
    }
}