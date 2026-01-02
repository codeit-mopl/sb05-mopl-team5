package com.mopl.api.domain.user.exception.detail;

import com.mopl.api.domain.user.exception.WatchingSessionErrorCode;
import com.mopl.api.domain.user.exception.WatchingSessionException;

public class WatchingSessionNotFoundException extends WatchingSessionException {

    public WatchingSessionNotFoundException() {
        super(WatchingSessionErrorCode.SESSION_NOT_FOUND);
    }
}