package com.mopl.api.domain.user.service;

import com.mopl.api.domain.user.dto.command.WatchingSessionCreateCommand;
import com.mopl.api.domain.user.dto.request.WatchingSessionSearchRequest;
import com.mopl.api.domain.user.dto.response.CursorResponseWatchingSessionDto;
import com.mopl.api.domain.user.dto.response.WatchingSessionDto;
import java.util.UUID;

public interface WatchingSessionService {

    WatchingSessionDto getWatchingSession(UUID watcherId);

    CursorResponseWatchingSessionDto getWatchingSession(UUID contentId, WatchingSessionSearchRequest request);

    WatchingSessionDto addWatchingSession(WatchingSessionCreateCommand command);

    void removeWatchingSession(UUID sessionId);
}