package com.mopl.api.domain.playlist.exception.detail;

import com.mopl.api.domain.playlist.exception.PlaylistErrorCode;
import com.mopl.api.domain.playlist.exception.PlaylistErrorException;
import java.util.UUID;

public class ContentNotInPlaylistException extends PlaylistErrorException {

    public ContentNotInPlaylistException() {
        super(PlaylistErrorCode.CONTENT_NOT_IN_PLAYLIST);
    }

    public static ContentNotInPlaylistException withDetails(UUID playlistId, UUID contentId) {
        ContentNotInPlaylistException exception = new ContentNotInPlaylistException();
        exception.addDetail("playlistId", playlistId);
        exception.addDetail("contentId", contentId);
        return exception;
    }
}
