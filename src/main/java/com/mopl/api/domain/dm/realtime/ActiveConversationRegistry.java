package com.mopl.api.domain.dm.realtime;


import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * 현재 WebSocket으로 활성 구독 중인 대화(conversation) 추적
 * <p>
 * key   : userId value : 사용자가 현재 구독 중인 conversationId 집합
 */
@Component
public class ActiveConversationRegistry {

    private final ConcurrentHashMap<UUID, Set<UUID>> registry = new ConcurrentHashMap<>();

    public void subscribe(UUID userId, UUID conversationId) {
        registry.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(conversationId);
    }


    public void unsubscribe(UUID userId, UUID conversationId) {
        Set<UUID> set = registry.get(userId);
        if (set == null) {
            return;
        }
        set.remove(conversationId);

        if (set.isEmpty()) {
            registry.remove(userId);
        }
    }


    public boolean isSubscribed(UUID userId, UUID conversationId) {
        Set<UUID> set = registry.get(userId);
        return set != null && set.contains(conversationId);
    }


    

}
