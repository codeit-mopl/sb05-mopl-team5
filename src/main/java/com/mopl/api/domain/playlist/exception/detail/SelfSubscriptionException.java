package com.mopl.api.domain.playlist.exception.detail;

import com.mopl.api.domain.playlist.exception.PlaylistErrorCode;
import com.mopl.api.domain.playlist.exception.PlaylistErrorException;
import java.util.UUID;

public class SelfSubscriptionException extends PlaylistErrorException {

    public SelfSubscriptionException() {
        super(PlaylistErrorCode.SELF_SUBSCRIPTION);
    }

    public static SelfSubscriptionException withDetails(UUID playlistId, UUID userId) {
        SelfSubscriptionException exception = new SelfSubscriptionException();
        exception.addDetail("playlistId", playlistId);
        exception.addDetail("userId", userId);
        return exception;
    }
}
