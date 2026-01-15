package com.mopl.api.domain.sse.entity;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;

@Getter
public class SseMessage {

    private final UUID eventId;
    private final Set<UUID> receiverIds;
    private final String eventName;
    private final Object data;

    private SseMessage(Set<UUID> receiverIds, String eventName, Object data) {
        this.eventId = UUID.randomUUID();
        this.receiverIds = receiverIds;
        this.eventName = eventName;
        this.data = data;
    }

    public static SseMessage create(Collection<UUID> receiverIds, String eventName, Object data) {
        return new SseMessage(Set.copyOf(receiverIds), eventName, data);
    }

    public boolean isReceivable(UUID receiverId) {
        return receiverIds.contains(receiverId);
    }
}
