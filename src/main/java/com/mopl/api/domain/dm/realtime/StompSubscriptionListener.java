package com.mopl.api.domain.dm.realtime;


import java.security.Principal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

@Component
@RequiredArgsConstructor
public class StompSubscriptionListener {

    private final ActiveConversationRegistry registry;


    private static final String DEST_PREFIX = "/sub/conversations/";
    private static final String DEST_SUFFIX = "/direct-messages";

    @EventListener
    public void handleSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor =
            MessageHeaderAccessor.getAccessor(event.getMessage(), StompHeaderAccessor.class);

        if (accessor == null) return;

        String destination = accessor.getDestination();
        if (destination == null) return;

        UUID conversationId = parseConversationId(destination);
        if (conversationId == null) return;

        UUID userId = extractUserId(accessor.getUser());
        if (userId == null) return;

        registry.subscribe(userId, conversationId);
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        UUID userId = extractUserId(event.getUser());
        if (userId != null) {
            registry.clear(userId);
        }
    }

    private UUID parseConversationId(String destination) {
        if (!destination.startsWith(DEST_PREFIX) || !destination.endsWith(DEST_SUFFIX)) {
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
            if (p instanceof UUID uuid) return uuid;
            if (p instanceof String s) return UUID.fromString(s);
        }
        return null;
    }

}
