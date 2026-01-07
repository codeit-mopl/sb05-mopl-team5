package com.mopl.api.global.config.websocket;

import com.mopl.api.global.config.security.jwt.JwtTokenProvider;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

@Slf4j
@RequiredArgsConstructor
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public Message<?> preSend(@NotNull Message<?> message, @NotNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.error("웹소켓 연결 실패: Authorization 헤더가 없거나 잘못되었습니다.");
                return message;
            }

            String token = authHeader.substring(7);

            try {
                UUID userId = jwtTokenProvider.getUserIdFromToken(token);

                Authentication authentication = new UsernamePasswordAuthenticationToken(
                    userId.toString(),
                    null,
                    List.of()
                );

                accessor.setUser(authentication);
                log.info("웹소켓 인증 성공: 유저 ID = {}", userId);

            } catch (Exception e) {
                log.error("웹소켓 인증 실패: {}", e.getMessage());
            }
        }

        return message;
    }
}
