package com.mopl.api.domain.user.controller;

import com.mopl.api.domain.user.dto.request.WatchingSessionSearchRequest;
import com.mopl.api.domain.user.dto.response.CursorResponseWatchingSessionDto;
import com.mopl.api.domain.user.dto.response.WatchingSessionDto;
import com.mopl.api.domain.user.service.WatchingSessionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "시청 세션 관리", description = "시청 세션 관리 API")
@Slf4j
@RestController
@RequiredArgsConstructor
public class WatchingSessionController {

    private final WatchingSessionService watchingSessionService;

    @GetMapping("/api/users/{watcherId}/watching-sessions")
    public ResponseEntity<WatchingSessionDto> watchingSessionDetail(@PathVariable UUID watcherId) {

        log.info("watcherId : {}", watcherId);

        return ResponseEntity.ok(watchingSessionService.getWatchingSession(watcherId));
    }


    @GetMapping("/api/contents/{contentId}/watching-sessions")
    public ResponseEntity<CursorResponseWatchingSessionDto> watchingSessionList(@PathVariable UUID contentId,
        @Valid WatchingSessionSearchRequest request) {

        log.info("contentId : {}", contentId);
        log.info("request : {}", request);

        return ResponseEntity.ok(watchingSessionService.getWatchingSession(contentId, request));
    }
}