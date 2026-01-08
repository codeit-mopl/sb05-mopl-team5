package com.mopl.api.domain.user.repository.impl;

import com.mopl.api.domain.user.dto.request.WatchingSessionSearchRequest;
import com.mopl.api.domain.user.entity.WatchingSession;
import java.util.List;
import java.util.UUID;

public interface WatchingSessionRepositoryCustom {

    List<WatchingSession> searchSessions(UUID contentId, WatchingSessionSearchRequest request);
}