package com.mopl.api.domain.user.service;

import com.mopl.api.domain.user.dto.request.WatchingSessionSearchRequest;
import com.mopl.api.domain.user.dto.response.CursorResponseWatchingSessionDto;
import com.mopl.api.domain.user.dto.response.WatchingSessionDto;
import com.mopl.api.domain.user.repository.WatchingSessionCacheRepository;
import com.mopl.api.domain.user.repository.WatchingSessionRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WatchingSessionServiceImpl implements WatchingSessionService {

    private final WatchingSessionRepository watchingSessionRepository;
    private final WatchingSessionCacheRepository watchingSessionCacheRepository;

    @Override
    public WatchingSessionDto getWatchingSession(UUID watcherId) {

        // TODO REDIS에 세션 정보가 있다면 가져오기
        // TODO DB에 세션 정보가 제대로 있는지 검증하기

        return WatchingSessionDto.builder()
                                 .build();
    }

    @Override
    public CursorResponseWatchingSessionDto getWatchingSession(UUID contentId,
        WatchingSessionSearchRequest request) {

        // TODO REDIS에 세션 정보가 있다면 가져오기
        // TODO DB에 세션 정보가 제대로 있는지 검증하기

        return CursorResponseWatchingSessionDto.builder()
                                               .build();
    }
}