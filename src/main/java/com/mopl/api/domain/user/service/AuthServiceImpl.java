package com.mopl.api.domain.user.service;

import com.mopl.api.domain.user.dto.request.JwtInformation;
import com.mopl.api.domain.user.dto.response.JwtDto;
import com.mopl.api.global.config.security.CustomUserDetails;
import com.mopl.api.global.config.security.jwt.JwtRegistry;
import com.mopl.api.global.config.security.jwt.JwtTokenProvider;
import com.nimbusds.jose.JOSEException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserDetailsService userDetailsService;
    private final JwtTokenProvider jwtProvider;
    private final JwtRegistry jwtRegistry;

    @Override
    public JwtInformation refreshToken(String refreshToken) {
        // 토큰 유효성 검증, JWT 세션에서도 유효한지 검증
        if(!jwtProvider.validateRefreshToken(refreshToken)) {
            log.info("Invalid refresh token");
            throw new RuntimeException("Invalid refresh token");
        }

        String username = jwtProvider.getUsernameFromToken(refreshToken);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        if(userDetails == null) {
            throw new UsernameNotFoundException("Invalid username or password.");
        }

        try {
            // 토큰 재발급하는 로직
            CustomUserDetails customDetails =  (CustomUserDetails) userDetails;
            String newAccessToken = jwtProvider.generateAccessToken(customDetails);
            String newRefreshToken = jwtProvider.generateRefreshToken(customDetails);
            log.info("Refresh Token : {}", newAccessToken);

            JwtInformation jwtInformation = new JwtInformation(
                customDetails.getUserDto(), newAccessToken, newRefreshToken
            );

            // JWT 세션의 기존 세션을 rotate 하는 메서드 호출 -> token 갱신용!
            jwtRegistry.rotateJwtInformation(refreshToken, jwtInformation);
            return jwtInformation;
        } catch (JOSEException e) {
            log.error("Failed to generate new token for user : {}", username,e);
            throw new RuntimeException("INTERNAL_SERVER_ERROR");
        }
    }
}
