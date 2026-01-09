package com.mopl.api.domain.user.controller;

import com.mopl.api.domain.user.dto.request.JwtInformation;
import com.mopl.api.domain.user.dto.response.JwtDto;
import com.mopl.api.domain.user.dto.request.ResetPasswordRequest;
import com.mopl.api.domain.user.service.AuthService;
import com.mopl.api.domain.user.service.UserService;
import com.mopl.api.global.config.security.jwt.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final AuthService authService;
    private final JwtTokenProvider jwtProvider;

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<JwtDto> refreshToken(
        @CookieValue(name = "REFRESH_TOKEN", required = true) String refreshToken,
        HttpServletResponse response
    ) {
        JwtInformation jwtInformation = authService.refreshToken(refreshToken);
        Cookie cookie = jwtProvider.generateRefreshTokenCookie(jwtInformation.getRefreshToken());
        response.addCookie(cookie);

        JwtDto body = new JwtDto(jwtInformation.getUserDto(), jwtInformation.getAccessToken());
        return ResponseEntity.status(HttpStatus.OK).body(body);
    }

    @GetMapping("/csrf-token")
    public ResponseEntity<Void> csrfTokenDetails(CsrfToken csrfToken) {
        String tokenDetail = csrfToken.getToken();
        log.debug("CSRF TOKEN: {}", tokenDetail);
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
