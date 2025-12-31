package com.mopl.api.domain.user.service;

import com.mopl.api.domain.user.dto.response.CursorResponseWatchingSessionDto;
import com.mopl.api.domain.user.dto.response.WatchingSessionDto;
import com.mopl.api.domain.user.dto.request.WatchingSessionSearchRequest;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class WatchingSessionServiceImpl implements WatchingSessionService {

    @Override
    public WatchingSessionDto getWatchingSession(UUID watcherId) {
        return null;
    }

    @Override
    public CursorResponseWatchingSessionDto getWatchingSession(UUID contentId,
        WatchingSessionSearchRequest request) {
        return null;
    }
}