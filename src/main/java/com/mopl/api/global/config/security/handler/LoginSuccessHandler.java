package com.mopl.api.global.config.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mopl.api.domain.user.dto.request.JwtInformation;
import com.mopl.api.domain.user.dto.response.JwtDto;
import com.mopl.api.domain.user.dto.response.UserDto;
import com.mopl.api.global.config.security.CustomUserDetails;
import com.mopl.api.global.config.security.jwt.JwtRegistry;
import com.mopl.api.global.config.security.jwt.JwtTokenProvider;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoginSuccessHandler implements AuthenticationSuccessHandler {
    private final JwtTokenProvider jwtProvider;
    private final JwtRegistry jwtRegistry;
    private final ObjectMapper objectMapper;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
        HttpServletResponse response,
        Authentication authentication)
        throws IOException, ServletException {
        log.info("Login success! username = {}", authentication.getName());

        // principal 방어적으로 꺼내기 (ClassCastException 방지)
        if (!(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            log.warn("로그인 성공 처리 중 principal 타입이 CustomUserDetails가 아님. principal={}",
                authentication.getPrincipal());

            throw new IllegalStateException("Authentication principal is not CustomUserDetails");

//            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
//            response.setCharacterEncoding("UTF-8");
//            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
//
//            objectMapper.writeValue(response.getWriter(),);
//            return;
        }

        UserDto userDto = userDetails.getUserDto();

        try {
            // JWT 발급
            String accessToken  = jwtProvider.generateAccessToken(userDetails);
            String refreshToken = jwtProvider.generateRefreshToken(userDetails);

            // 중복 로그인 처리
            if (jwtRegistry.hasActiveJwtInformationByUserId(userDto.id())) {
                log.info("[사용자 관리] 중복 로그인 감지 username = {}", userDto.name());
                jwtRegistry.invalidateJwtInformationByUserId(userDto.id());
            }

            JwtInformation jwtInformation = new JwtInformation(userDto, accessToken, refreshToken);
            jwtRegistry.registerJwtInformation(jwtInformation);

            // refreshToken 쿠키 생성
            Cookie refreshCookie = jwtProvider.genereateRefreshTokenCookie(refreshToken);
            response.addCookie(refreshCookie);

            // 응답 바디: accessToken + userDto
            JwtDto jwtDto = new JwtDto(userDto, accessToken);

            response.setStatus(HttpServletResponse.SC_OK);
            response.setCharacterEncoding("UTF-8");
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getWriter(), jwtDto);

        } catch (Exception e) {
            log.error("JWT 생성/응답 처리 중 오류", e);
            throw new RuntimeException("Failed to issue JWT", e);

//            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
//            response.setCharacterEncoding("UTF-8");
//            response.setContentType(MediaType.APPLICATION_JSON_VALUE);

//            ErrorResponse errorResponse = new ErrorResponse(
//                new RuntimeException("Failed to generate JWT"),
//                HttpServletResponse.SC_INTERNAL_SERVER_ERROR
//            );
//            objectMapper.writeValue(response.getWriter(), errorResponse);
        }
    }
}

