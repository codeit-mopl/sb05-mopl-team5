package com.mopl.api.domain.playlist.exception.detail;

import com.mopl.api.domain.playlist.exception.PlaylistErrorCode;
import com.mopl.api.domain.playlist.exception.PlaylistErrorException;
import java.util.UUID;

public class ContentAlreadyExistsException extends PlaylistErrorException {

    public ContentAlreadyExistsException() {
        super(PlaylistErrorCode.CONTENT_ALREADY_EXISTS);
    }

    public static ContentAlreadyExistsException withDetails(UUID playlistId, UUID contentId) {
        ContentAlreadyExistsException exception = new ContentAlreadyExistsException();
        exception.addDetail("playlistId", playlistId);
        exception.addDetail("contentId", contentId);
        return exception;
    }
}
