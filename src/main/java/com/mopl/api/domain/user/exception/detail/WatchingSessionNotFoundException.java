package com.mopl.api.domain.user.exception.detail;

import com.mopl.api.domain.user.exception.WatchingSessionErrorCode;
import com.mopl.api.domain.user.exception.WatchingSessionException;
import java.util.UUID;

public class WatchingSessionNotFoundException extends WatchingSessionException {

    public WatchingSessionNotFoundException() {
        super(WatchingSessionErrorCode.SESSION_NOT_FOUND);
    }

    public static WatchingSessionNotFoundException withWatcherId(UUID watcherId) {
        WatchingSessionNotFoundException exception = new WatchingSessionNotFoundException();
        exception.addDetail("watcherId", watcherId);
        return exception;
    }

    public static WatchingSessionNotFoundException withContentIdAndWatcherId(UUID contentId, UUID watcherId) {
        WatchingSessionNotFoundException exception = new WatchingSessionNotFoundException();
        exception.addDetail("contentId", contentId);
        exception.addDetail("watcherId", watcherId);
        return exception;
    }
}