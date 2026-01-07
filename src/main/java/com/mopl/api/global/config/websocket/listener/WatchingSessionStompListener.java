package com.mopl.api.global.config.websocket.listener;

import com.mopl.api.domain.user.service.WatchingSessionService;
import java.security.Principal;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class WatchingSessionStompListener {

    private static final String DEST_PREFIX = "/sub/contents/";
    private static final String DEST_SUFFIX = "/watch";

    private final WatchingSessionService watchingSessionService;

    @EventListener
    public void handleSubscribe(SessionSubscribeEvent event) {
        // JOIN
        StompHeaderAccessor accessor =
            MessageHeaderAccessor.getAccessor(event.getMessage(), StompHeaderAccessor.class);
        if (accessor == null) {
            return;
        }

        UUID contentId = parseContentId(accessor.getDestination());
        if (contentId == null) {
            return;
        }

        UUID userId = extractUserId(accessor.getUser());
        if (userId == null) {
            return;
        }

        Map<String, Object> attrs = accessor.getSessionAttributes();
        if (attrs == null) {
            return;
        }

        @SuppressWarnings("unchecked")
        Set<UUID> watchingContents =
            (Set<UUID>) attrs.computeIfAbsent(
                "watchingContentIds",
                k -> new HashSet<UUID>()
            );

        // 이미 보고 있으면 무시 (중복 방지, 나중에 정교화 가능)
        if (!watchingContents.add(contentId)) {
            return;
        }

        watchingSessionService.joinWatchingSession(contentId, userId);
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        //LEAVE
        StompHeaderAccessor accessor =
            MessageHeaderAccessor.getAccessor(event.getMessage(), StompHeaderAccessor.class);
        if (accessor == null) {
            return;
        }

        UUID userId = extractUserId(accessor.getUser());
        if (userId == null) {
            return;
        }

        Map<String, Object> attrs = accessor.getSessionAttributes();
        if (attrs == null) {
            return;
        }

        @SuppressWarnings("unchecked")
        Set<UUID> watchingContents =
            (Set<UUID>) attrs.get("watchingContentIds");

        if (watchingContents == null || watchingContents.isEmpty()) {
            return;
        }

        for (UUID contentId : watchingContents) {
            try {
                watchingSessionService.leaveWatchingSession(contentId, userId);
            } catch (Exception e) {
                log.warn("LEAVE 처리 실패 contentId={}, userId={}", contentId, userId, e);
            }
        }
    }

    private UUID parseContentId(String destination) {
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

    // TODO 나중에 dm stomp 쪽 같이 추출
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