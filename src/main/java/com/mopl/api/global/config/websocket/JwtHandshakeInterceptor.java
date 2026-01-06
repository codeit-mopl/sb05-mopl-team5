package com.mopl.api.global.config.websocket;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(
        @NotNull ServerHttpRequest request,
        @NotNull ServerHttpResponse response,
        @NotNull WebSocketHandler wsHandler,
        @NotNull Map<String, Object> attributes
    ) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            return false;
        }

        HttpServletRequest httpRequest = servletRequest.getServletRequest();
        String authHeader = httpRequest.getHeader("Authorization");

        return authHeader != null && authHeader.startsWith("Bearer ");
    }

    @Override
    public void afterHandshake(
        @NotNull ServerHttpRequest request,
        @NotNull ServerHttpResponse response,
        @NotNull WebSocketHandler wsHandler,
        Exception exception
    ) {
        // TODO 필요하면 로깅 / 메트릭 전송 추가
    }
}