package com.mopl.api.global.config.websocket.listener;

import com.mopl.api.domain.user.service.WatchingSessionService;
import java.security.Principal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class WatchingSessionStompListener {

    private static final String DEST_PREFIX = "/sub/contents/";
    private static final String DEST_SUFFIX = "/watch";
    private static final String WATCH_KEY = "watchingContentIds";
    private static final String SUB_KEY = "subMapping";

    private final WatchingSessionService watchingSessionService;

    @EventListener
    public void handleSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = accessor.getDestination();
        String subscriptionId = accessor.getSubscriptionId();

        UUID contentId = parseContentId(destination);
        UUID userId = extractUserId(accessor.getUser());

        if (contentId == null || userId == null || subscriptionId == null) {
            return;
        }

        Map<String, Object> attrs = accessor.getSessionAttributes();
        if (attrs == null) {
            return;
        }

        Set<UUID> watchingContents = (Set<UUID>) attrs.computeIfAbsent(WATCH_KEY, k -> new HashSet<UUID>());
        watchingContents.add(contentId);

        Map<String, UUID> subMap = (Map<String, UUID>) attrs.computeIfAbsent(SUB_KEY,
            k -> new HashMap<String, UUID>());
        subMap.put(subscriptionId, contentId);

        watchingSessionService.joinWatchingSession(contentId, userId);
        log.debug("[JOIN] userId: {}, contentId: {}, subId: {}", userId, contentId, subscriptionId);
    }

    @EventListener
    public void handleUnsubscribe(SessionUnsubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String subscriptionId = accessor.getSubscriptionId();
        UUID userId = extractUserId(accessor.getUser());

        Map<String, Object> attrs = accessor.getSessionAttributes();
        if (attrs == null || userId == null || subscriptionId == null) {
            return;
        }

        Map<String, UUID> subMap = (Map<String, UUID>) attrs.get(SUB_KEY);
        if (subMap != null && subMap.containsKey(subscriptionId)) {
            UUID contentId = subMap.remove(subscriptionId);

            if (!subMap.containsValue(contentId)) {
                Set<UUID> watchingContents = (Set<UUID>) attrs.get(WATCH_KEY);
                if (watchingContents != null) {
                    watchingContents.remove(contentId);
                }

                watchingSessionService.leaveWatchingSession(contentId, userId);
                log.debug("[LEAVE by Unsubscribe] userId: {}, contentId: {}", userId, contentId);
            }
        }
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        UUID userId = extractUserId(accessor.getUser());
        Map<String, Object> attrs = accessor.getSessionAttributes();

        if (attrs == null || userId == null) {
            return;
        }

        Set<UUID> watchingContents = (Set<UUID>) attrs.get(WATCH_KEY);
        if (watchingContents == null) {
            return;
        }

        for (UUID contentId : watchingContents) {
            try {
                watchingSessionService.leaveWatchingSession(contentId, userId);
                log.debug("[LEAVE by Disconnect] userId: {}, contentId: {}", userId, contentId);
            } catch (Exception e) {
                log.error("Disconnect LEAVE 처리 실패", e);
            }
        }
        attrs.remove(WATCH_KEY);
        attrs.remove(SUB_KEY);
    }

    private UUID parseContentId(String destination) {
        if (destination == null) {
            return null;
        }

        if (!destination.startsWith(DEST_PREFIX) ||
            !destination.endsWith(DEST_SUFFIX)) {
            return null;
        }

        String raw = destination.substring(
            DEST_PREFIX.length(),
            destination.length() - DEST_SUFFIX.length()
        );

        try {
            return UUID.fromString(raw);
        } catch (Exception e) {
            return null;
        }
    }

    private UUID extractUserId(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken token) {
            Object p = token.getPrincipal();
            if (p instanceof UUID uuid) {
                return uuid;
            }
            if (p instanceof String s) {
                try {
                    return UUID.fromString(s);
                } catch (Exception ignored) {
                }
            }
        }
        return null;
    }
}