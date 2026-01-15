package com.mopl.api.domain.sse.controller;


import com.mopl.api.domain.sse.service.SseService;
import com.mopl.api.global.config.security.claim.CustomUserDetails;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
public class SseController {

    private final SseService sseService;

    @GetMapping(value = "/api/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(
        @AuthenticationPrincipal CustomUserDetails user,
        @RequestParam(value = "LastEventId", required = false) String lastEventId
    ) {
        UUID last = null;
        if (lastEventId != null && !lastEventId.isBlank()) {
            last = UUID.fromString(lastEventId);
        }
        UUID userId = user.getUserDto()
                          .id();

        return sseService.connect(userId, last);
    }

}
