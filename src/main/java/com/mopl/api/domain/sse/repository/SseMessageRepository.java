package com.mopl.api.domain.sse.repository;

import com.mopl.api.domain.sse.entity.SseMessage;
import java.util.List;
import java.util.UUID;

public interface SseMessageRepository {

    SseMessage save(SseMessage message);

    List<SseMessage> findAllByEventIdAfterAndReceiverId(UUID eventId, UUID receiverId);
}
