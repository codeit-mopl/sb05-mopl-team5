package com.mopl.api.global.config.websocket.dto;

import com.mopl.api.domain.user.dto.response.WatchingSessionDto;

public record WatchingSessionChange(
    ChangeType type,
    WatchingSessionDto watchingSession,
    long watcherCount
) {

    public enum ChangeType {
        JOIN,
        LEAVE
    }
}