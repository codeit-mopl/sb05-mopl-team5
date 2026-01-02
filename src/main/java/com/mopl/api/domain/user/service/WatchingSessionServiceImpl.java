package com.mopl.api.domain.user.service;

import com.mopl.api.domain.user.dto.command.WatchingSessionCreateCommand;
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

        // TODO REDIS O, DB X 인 경우 REDIS 만료시키기
        // TODO REDIS X, DB X 예외

        return WatchingSessionDto.builder()
                                 .build();
    }

    @Override
    public CursorResponseWatchingSessionDto getWatchingSession(UUID contentId,
        WatchingSessionSearchRequest request) {

        // TODO REDIS에 세션 정보가 있다면 가져오기
        // TODO 필요하다면 DB에 세션 정보가 제대로 있는지 검증, 아니라면 생략

        // TODO REDIS O, DB X 인 경우 REDIS 만료시키기

        return CursorResponseWatchingSessionDto.builder()
                                               .build();
    }

    @Override
    public WatchingSessionDto addWatchingSession(WatchingSessionCreateCommand command) {
        // TODO Websocket 연결 시 DB, REDIS에 session 만들기
        return null;
    }

    @Override
    public void removeWatchingSession(UUID sessionId) {
        // TODO Websocket 연결 해제 시 혹은 필요 시 session 제거(물리)
    }
}