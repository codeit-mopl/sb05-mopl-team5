package com.mopl.api.domain.playlist.exception.detail;

import com.mopl.api.domain.playlist.exception.PlaylistErrorCode;
import com.mopl.api.domain.playlist.exception.PlaylistErrorException;
import java.util.UUID;

public class ContentNotFoundException extends PlaylistErrorException {

    public ContentNotFoundException() {
        super(PlaylistErrorCode.CONTENT_NOT_FOUND);
    }

    public static ContentNotFoundException withContentId(UUID contentId) {
        ContentNotFoundException exception = new ContentNotFoundException();
        exception.addDetail("contentId", contentId);
        return exception;
    }
}
