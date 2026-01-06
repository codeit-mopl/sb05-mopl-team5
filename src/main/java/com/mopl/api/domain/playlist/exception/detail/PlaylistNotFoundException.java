package com.mopl.api.domain.playlist.exception.detail;

import com.mopl.api.domain.playlist.exception.PlaylistErrorCode;
import com.mopl.api.domain.playlist.exception.PlaylistErrorException;
import java.util.UUID;

public class PlaylistNotFoundException extends PlaylistErrorException {

    public PlaylistNotFoundException() {
        super(PlaylistErrorCode.PLAYLIST_NOT_FOUND);
    }

    public static PlaylistNotFoundException withPlaylistId(UUID playlistId) {
        PlaylistNotFoundException exception = new PlaylistNotFoundException();
        exception.addDetail("playlistId", playlistId);
        return exception;
    }
}
