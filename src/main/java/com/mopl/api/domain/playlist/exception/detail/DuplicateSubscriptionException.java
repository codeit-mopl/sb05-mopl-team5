package com.mopl.api.domain.playlist.exception.detail;

import com.mopl.api.domain.playlist.exception.PlaylistErrorCode;
import com.mopl.api.domain.playlist.exception.PlaylistErrorException;
import java.util.UUID;

public class DuplicateSubscriptionException extends PlaylistErrorException {

    public DuplicateSubscriptionException() {
        super(PlaylistErrorCode.DUPLICATE_SUBSCRIPTION);
    }

    public static DuplicateSubscriptionException withDetails(UUID playlistId, UUID userId) {
        DuplicateSubscriptionException exception = new DuplicateSubscriptionException();
        exception.addDetail("playlistId", playlistId);
        exception.addDetail("userId", userId);
        return exception;
    }
}
