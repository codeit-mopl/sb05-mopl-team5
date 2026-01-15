package com.mopl.api.domain.sse.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface SseEmitterRepository {

    SseEmitter save(UUID receiverId, SseEmitter emitter);

    Optional<List<SseEmitter>> findByReceiverId(UUID receiverId);

    List<SseEmitter> findAllByReceiverIdsIn(Collection<UUID> receiverIds);

    List<SseEmitter> findAll();

    void delete(UUID receiverId, SseEmitter emitter);
}
