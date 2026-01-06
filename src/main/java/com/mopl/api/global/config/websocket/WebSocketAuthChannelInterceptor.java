package com.mopl.api.global.config.websocket;


import com.mopl.api.global.config.security.jwt.JwtTokenProvider;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public @Nullable Message<?> preSend(Message<?> message, MessageChannel channel) {

        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {

            String auth = getAuthorization(accessor);

            if (auth == null || !auth.startsWith("Bearer ")) {
                throw new IllegalStateException("WebSocket Authorization 헤더가 필요합니다.");
            }

            String token = auth.substring(7).trim();
            if (!jwtTokenProvider.validateAccessToken(token)) {
                throw new IllegalStateException("WebSocket AccessToken이 유효하지 않습니다.");
            }

            UUID userId = jwtTokenProvider.getUserIdFromToken(token);

            // Principal = UUID (REST와 동일하게 UUID를 principal로 유지)
            var authentication = new UsernamePasswordAuthenticationToken(userId, null, List.of());
            accessor.setUser(authentication);

        }
        return message;
    }

    private String getAuthorization(StompHeaderAccessor accessor) {
        List<String> authHeaders = accessor.getNativeHeader("Authorization");
        if (authHeaders == null || authHeaders.isEmpty()) {
            return null;
        }
        return authHeaders.get(0);
    }
}
