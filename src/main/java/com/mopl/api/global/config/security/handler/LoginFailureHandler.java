package com.mopl.api.global.config.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoginFailureHandler implements AuthenticationFailureHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
        HttpServletResponse response,
        AuthenticationException exception)
        throws IOException, ServletException {
        log.error("Authentication Failure : {}", exception.getMessage(), exception);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        // 아이디 또는 비밀번호 달랐을 경우의 메세지
        String code = "UNAUTHORIZED";
        String message = "아이디 또는 비밀번호가 올바르지 않습니다.";

        // 계정 잠겼을 경우의 메세지
        if(exception instanceof LockedException){
            code = "ACCOUNT_LOCKED";
            message = "계정이 잠금 상태입니다.";
        }
        objectMapper.writeValue(response.getWriter(), new FailureBody(code, message));
    }

    private record FailureBody(String code, String message) {

    }
}