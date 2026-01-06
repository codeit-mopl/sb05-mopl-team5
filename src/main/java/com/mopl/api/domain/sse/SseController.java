package com.mopl.api.domain.sse;


import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
public class SseController {

    private final SseEmitterRegistry sseEmitterRegistry;

    // 운영에서 보통 30분~1시간 단위로 둠 (클라가 끊기면 재연결)
    private static final long TIMEOUT_MS = 60L * 60 * 1000; // 1 hour

    @GetMapping(value = "/api/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connect() {
        UUID me = currentUserId();

        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);

        // userId 기준 1개만 유지 (재연결 시 교체)
        sseEmitterRegistry.add(me, emitter);

        // (선택) 연결 확인용 ping: 클라이언트가 즉시 연결 성공을 알 수 있음
        sseEmitterRegistry.send(
            me,
            "notifications",                    // name
            "sse-connect-" + System.currentTimeMillis(), // id
            "connected"                         // data
        );

        return emitter;
    }

    private UUID currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new IllegalStateException("인증 정보가 없습니다.");
        }

        Object principal = auth.getPrincipal();
        if (principal instanceof UUID uuid) return uuid;

        // CustomUserDetails를 쓰는 구조면 여기에서 꺼내도록 확장 가능
        // 예: if (principal instanceof CustomUserDetails cud) return cud.getUserDto().id();

        return UUID.fromString(principal.toString());
    }

}
