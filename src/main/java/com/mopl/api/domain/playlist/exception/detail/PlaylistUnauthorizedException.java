package com.mopl.api.domain.playlist.exception.detail;

import com.mopl.api.domain.playlist.exception.PlaylistErrorCode;
import com.mopl.api.domain.playlist.exception.PlaylistErrorException;
import java.util.UUID;

public class PlaylistUnauthorizedException extends PlaylistErrorException {

    public PlaylistUnauthorizedException() {
        super(PlaylistErrorCode.PLAYLIST_UNAUTHORIZED);
    }

    public static PlaylistUnauthorizedException withDetails(UUID playlistId, UUID userId) {
        PlaylistUnauthorizedException exception = new PlaylistUnauthorizedException();
        exception.addDetail("playlistId", playlistId);
        exception.addDetail("userId", userId);
        return exception;
    }
}
