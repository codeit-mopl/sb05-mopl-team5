package com.mopl.api.domain.sse.repository;

import com.mopl.api.domain.sse.entity.SseMessage;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

@Repository
public class SseMessageRepositoryImpl implements SseMessageRepository {

    private final int eventQueueCapacity;
    private final Object lock = new Object();

    private final ConcurrentLinkedDeque<UUID> eventIdQueue = new ConcurrentLinkedDeque<>();
    private final Map<UUID, SseMessage> messages = new ConcurrentHashMap<>();

    public SseMessageRepositoryImpl(
        @Value("${sse.event-queue-capacity:100}") int eventQueueCapacity) {
        this.eventQueueCapacity = eventQueueCapacity;
    }

    public SseMessage save(SseMessage message) {
        synchronized (lock) {
            makeAvailableCapacity();

            UUID eventId = message.getEventId();
            eventIdQueue.addLast(eventId);
            messages.put(eventId, message);
            return message;
        }
    }

    public List<SseMessage> findAllByEventIdAfterAndReceiverId(UUID eventId, UUID receiverId) {
        return eventIdQueue.stream()
                           .dropWhile(id -> !id.equals(eventId))
                           .skip(1)
                           .map(messages::get)
                           .filter(msg -> msg != null && msg.isReceivable(receiverId))
                           .toList();
    }

    private void makeAvailableCapacity() {
        int availableCapacity = eventQueueCapacity - eventIdQueue.size();
        while (availableCapacity < 1) {
            UUID removed = eventIdQueue.removeFirst();
            messages.remove(removed);
            availableCapacity++;
        }
    }
}
