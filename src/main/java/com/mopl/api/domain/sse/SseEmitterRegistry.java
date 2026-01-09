package com.mopl.api.domain.sse;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class SseEmitterRegistry {
    // TODO Redis 
    private final Map<UUID, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * SSE 연결 등록
     */
    public SseEmitter add(UUID userId, SseEmitter emitter) {
        SseEmitter old = emitters.put(userId, emitter);
        if (old != null) {
            try { old.complete(); } catch (Exception ignored) {}
        }

        emitter.onCompletion(() -> emitters.remove(userId));
        emitter.onTimeout(() -> emitters.remove(userId));
        emitter.onError(e -> emitters.remove(userId));

        return emitter;
    }

    /**
     * 특정 사용자에게 SSE 이벤트 전송
     */
    public void send(UUID userId, String eventName, String eventId, Object data) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter == null) return;

        try {
            emitter.send(
                SseEmitter.event()
                          .name(eventName)
                          .id(eventId)
                          .data(data)
            );
        } catch (IOException e) {
            emitters.remove(userId);
        }
    }

    /**
     * 명시적 제거 (필요 시)
     */
    public void remove(UUID userId) {
        emitters.remove(userId);
    }
}