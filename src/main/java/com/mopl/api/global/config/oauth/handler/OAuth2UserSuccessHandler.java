package com.mopl.api.global.config.oauth.handler;

import com.mopl.api.domain.user.dto.request.JwtInformation;
import com.mopl.api.domain.user.dto.response.UserDto;
import com.mopl.api.domain.user.repository.UserRepository;
import com.mopl.api.global.config.oauth.claim.CustomOAuth2UserDetails;
import com.mopl.api.global.config.security.claim.CustomUserDetails;
import com.mopl.api.global.config.security.jwt.JwtRegistry;
import com.mopl.api.global.config.security.jwt.JwtTokenProvider;
import com.nimbusds.jose.JOSEException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Slf4j
@Component
public class OAuth2UserSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtRegistry jwtRegistry;

    @Value("${custom.jwt.refresh-token.expiration-ms}")
    private long refreshTokenExpirationMs;

    @Value("${auth.oauth2.redirect-uri:/}")
    private String redirectUri;

    @Override
    public void onAuthenticationSuccess(
        HttpServletRequest request,
        HttpServletResponse response,
        Authentication authentication
    ) throws IOException, ServletException {

        log.info("[OAuth2] SuccessHandler 실행 !!");
        try {
            CustomOAuth2UserDetails principal = (CustomOAuth2UserDetails) authentication.getPrincipal();

            UserDto userDto = principal.getUserDto();

            // 계정 잠금 상황이면 -> 일단 차단!
            if (Boolean.TRUE.equals(userDto.locked())) {
                log.warn("[OAuth2] locked user login blocked userId={}, email={}",
                    userDto.id(), userDto.email());
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "현재 계정은 잠금 상태입니다!!");
                return;
            }

            CustomUserDetails userDetails = new CustomUserDetails(userDto, principal.getPassword());

            String accessToken  = jwtTokenProvider.generateAccessToken(userDetails);
            String refreshToken = jwtTokenProvider.generateRefreshToken(userDetails);

           // 중복 로그인 처리
            if (jwtRegistry.hasActiveJwtInformationByUserId(userDto.id())) {
                log.info("[OAuth2] 중복 로그인!!!! userId={}, email={}", userDto.id(), userDto.email());
                jwtRegistry.invalidateJwtInformationByUserId(userDto.id());
            }

            jwtRegistry.registerJwtInformation(new JwtInformation(userDto, accessToken, refreshToken));

            ResponseCookie refreshCookie = ResponseCookie
                .from(JwtTokenProvider.REFRESH_TOKEN, refreshToken)
                .path("/")
                .httpOnly(true)
                .secure(false)         // 개발환경에서는 일단 false로 설정!
                .sameSite("Lax")       // 개발환경에서는 일단 Lax로 설정!
                .maxAge(Duration.ofMillis(refreshTokenExpirationMs))
                .build();

            response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

            log.info("[OAuth2] 로그인 성공 !!! ->  redirectUri={}, email={}", redirectUri, userDto.email());
            response.sendRedirect(redirectUri);

        } catch (JOSEException e) {
            log.error("[OAuth2] 토큰 생성 실패!!!!!", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } catch (ClassCastException e) {
            log.error("[OAuth2] principal 타입 매칭 실패!!!! :{}", authentication.getPrincipal().getClass(), e);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        } catch (Exception e) {
            log.error("[OAuth2] Success Handler 실행 실패!!!!!", e);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "OAuth2 Success Handler를 실패했습니다");
        }
    }
}

