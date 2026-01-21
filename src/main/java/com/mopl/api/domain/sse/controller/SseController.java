package com.mopl.api.domain.sse.controller;


import com.mopl.api.domain.sse.service.SseService;
import com.mopl.api.global.config.security.claim.CustomUserDetails;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
@Slf4j
public class SseController {

    private final SseService sseService;

    @GetMapping(value = "/api/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(
        @AuthenticationPrincipal CustomUserDetails user,
        @RequestParam(value = "LastEventId", required = false) String lastEventId
    ) {
        UUID last = null;
        if (lastEventId != null && !lastEventId.isBlank()) {
            try {
                last = UUID.fromString(lastEventId);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid LastEventId format: " + lastEventId);
            }
        }
        UUID userId = user.getUserDto()
                          .id();

        log.info("SSE subscribe LastEventId={}", last);

        return sseService.connect(userId, last);
    }

}
