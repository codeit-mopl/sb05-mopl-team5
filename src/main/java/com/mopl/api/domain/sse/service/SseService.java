package com.mopl.api.domain.sse.service;

import com.mopl.api.domain.sse.entity.SseMessage;
import com.mopl.api.domain.sse.repository.SseEmitterRepository;
import com.mopl.api.domain.sse.repository.SseMessageRepository;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Service
@RequiredArgsConstructor
public class SseService {

    @Value("${sse.timeout:1800000}") // 30분
    private long timeout;

    // Emitter 저장소
    private final SseEmitterRepository sseEmitterRepository;

    // Message 저장소
    private final SseMessageRepository sseMessageRepository;

    // 연결 시도
    public SseEmitter connect(UUID receiverId, UUID lastEventId) {
        SseEmitter emitter = new SseEmitter(timeout);

        // 완료 되었을때
        emitter.onCompletion(() -> {
            sseEmitterRepository.delete(receiverId, emitter);
        });
        emitter.onTimeout(() -> {
            sseEmitterRepository.delete(receiverId, emitter);
        });
        emitter.onError(ex -> {
            sseEmitterRepository.delete(receiverId, emitter);
        });

        sseEmitterRepository.save(receiverId, emitter);

        sendToClient(emitter, "connect", "connected");

        // 마지막 아이디 기준 복구
        if (lastEventId != null) {
            sseMessageRepository.findAllByEventIdAfterAndReceiverId(lastEventId, receiverId)
                                .forEach(msg -> sendToClient(emitter, msg.getEventName(), msg.getData()));
        }

        return emitter;
    }

    public void send(Collection<UUID> receiverIds, String eventName, Object data) {
        SseMessage message = sseMessageRepository.save(
            SseMessage.create(receiverIds, eventName, data));

        List<SseEmitter> emitters = sseEmitterRepository.findAllByReceiverIdsIn(receiverIds);

        log.debug("SSE send. eventName={} receivers={} emitterCount={}",
            eventName, receiverIds, emitters.size());

        emitters.forEach(emitter -> {
            sendToClient(emitter, eventName, data);
        });
    }

    private void sendToClient(SseEmitter emitter, String name, Object data) {
        try {
            emitter.send(SseEmitter.event()
                                   .name(name)
                                   .data(data));
        } catch (IOException e) {
            log.error("Failed to send message", e);
            emitter.completeWithError(e);
        }
    }

    // 오래된 emitter 정리
//    @Scheduled(fixedDelay = 1000L * 60 * 30) // 30분
    @Scheduled(fixedDelay = 25000) // 25초
    public void cleanUp() {
        sseEmitterRepository.findAll()
                            .stream()
                            .filter(emitter -> !ping(emitter))
                            .forEach(emitter -> emitter.complete());
    }

    private boolean ping(SseEmitter emitter) {
        try {
            emitter.send(SseEmitter.event()
                                   .name("ping")
                                   .build());
            return true;
        } catch (IOException | IllegalStateException e) {
            log.debug("Failed to send ping event: {}", e.getMessage());
            return false;
        }
    }

    private String makeEmitterId(UUID userId) {
        return userId + "_" + System.currentTimeMillis() + "_" + UUID.randomUUID();
    }
}
