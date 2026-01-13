package com.mopl.api.domain.user.service;

import com.mopl.api.domain.user.dto.request.JwtInformation;
import com.mopl.api.domain.user.dto.request.ResetPasswordRequest;
import com.mopl.api.domain.user.exception.auth.detail.InvalidTokenException;
import com.mopl.api.domain.user.exception.user.detail.UserNotFoundException;
import com.mopl.api.domain.user.repository.UserRepository;
import com.mopl.api.global.config.security.claim.CustomUserDetails;
import com.mopl.api.global.config.security.jwt.JwtRegistry;
import com.mopl.api.global.config.security.jwt.JwtTokenProvider;
import com.nimbusds.jose.JOSEException;
import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserDetailsService userDetailsService;
    private final JwtTokenProvider jwtProvider;
    private final JwtRegistry jwtRegistry;
    private final UserRepository userRepository;
    private final MailService mailService;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${auth.temp-password-expiration}")
    private long expirationSeconds;

    private static final String TEMP_PW_KEY_PREFIX = "temp-pw:";

    @Transactional
    @Override
    public JwtInformation refreshToken(String refreshToken) {

        log.info("[REFRESH] validateRefresh={}", jwtProvider.validateRefreshToken(refreshToken));
        log.info("[REFRESH] registryHasRefresh={}", jwtRegistry.hasActiveJwtInformationByRefreshToken(refreshToken));

        // 토큰 유효성 검증, JWT 세션에서도 유효한지 검증
        if(!jwtProvider.validateRefreshToken(refreshToken)
            || !jwtRegistry.hasActiveJwtInformationByRefreshToken(refreshToken)) {
            log.info("Invalid refresh token");
            throw new InvalidTokenException();
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
            log.info("New Access Token : {}", newAccessToken.substring(0,10));
            log.info("New Refresh Token : {}", newRefreshToken.substring(0,10));

            JwtInformation jwtInformation = new JwtInformation(
                customDetails.getUserDto(), newAccessToken, newRefreshToken
            );

            // JWT 세션의 기존 세션을 rotate 하는 메서드 호출 -> token 갱신용!
            jwtRegistry.rotateJwtInformation(refreshToken, jwtInformation);
            return jwtInformation;
        } catch (JOSEException e) {
            log.error("Failed to generate new token for user : {}", username,e);
            throw new RuntimeException("INTERNAL_SERVER_ERROR",e);
        }
    }

    @Transactional
    @Override
    public void resetPassword(ResetPasswordRequest request) {
        String email = request.email();

        if(!userRepository.existsByEmail(request.email()) ) {
            throw UserNotFoundException.withUserEmail(request.email());
        }
        String tempPassword = generateTempPassword(); // 임시비밀번호 생성

        // Redis- 해시저장 + TTL
        String hashed = passwordEncoder.encode(tempPassword);
        String key = TEMP_PW_KEY_PREFIX + email; // temp_pw:{email}

        // 재발급이면 덮어쓰기
        redisTemplate.opsForValue().set(key, hashed, Duration.ofSeconds(expirationSeconds));

        // 메일 전송
        mailService.sendMail(email, tempPassword);
    }

    private String generateTempPassword() {
        String temp= UUID.randomUUID().toString().replace("-", "");
        return "mopl1!" + temp.substring(0,6);
    }
}
