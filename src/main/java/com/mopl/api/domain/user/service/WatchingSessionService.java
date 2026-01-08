package com.mopl.api.domain.user.service;

import com.mopl.api.domain.user.dto.request.WatchingSessionSearchRequest;
import com.mopl.api.domain.user.dto.response.CursorResponseWatchingSessionDto;
import com.mopl.api.domain.user.dto.response.WatchingSessionDto;
import com.mopl.api.global.config.websocket.dto.WatchingSessionChange;
import java.util.UUID;

public interface WatchingSessionService {

    WatchingSessionDto getWatchingSession(UUID watcherId);

    CursorResponseWatchingSessionDto getWatchingSession(UUID contentId, WatchingSessionSearchRequest request);

    WatchingSessionChange joinWatchingSession(UUID contentId, UUID watcherId);

    WatchingSessionChange leaveWatchingSession(UUID contentId, UUID watcherId);
}