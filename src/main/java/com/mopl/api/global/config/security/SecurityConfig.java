package com.mopl.api.global.config.security;

import static org.springframework.security.config.Customizer.withDefaults;

import com.mopl.api.domain.user.entity.UserRole;
import com.mopl.api.global.config.security.filter.JwtAuthenticationFilter;
import com.mopl.api.global.config.security.handler.AccessDeniedHandlerImpl;
import com.mopl.api.global.config.security.handler.JwtAuthenticationEntryPoint;
import com.mopl.api.global.config.security.handler.JwtLogoutHandler;
import com.mopl.api.global.config.security.handler.LoginFailureHandler;
import com.mopl.api.global.config.security.handler.LoginSuccessHandler;
import com.mopl.api.global.config.security.jwt.InMemoryJwtRegistry;
import com.mopl.api.global.config.security.jwt.JwtRegistry;
import com.mopl.api.global.config.security.jwt.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.util.StringUtils;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(
        HttpSecurity http,
        LoginSuccessHandler loginSuccessHandler,
        LoginFailureHandler loginFailureHandler,
        JwtLogoutHandler logoutHandler,
        JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
        JwtAuthenticationFilter jwtAuthenticationFilter,
        AccessDeniedHandlerImpl accessDeniedHandlerImpl) throws Exception {
        http
            .csrf(csrf -> csrf
                // 쿠키에 CSRF 토큰 저장 : 쿠키명:XSRF-TOKEN 헤더명:X-XSRF-TOKEN
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/csrf-token")
                .permitAll()
                .requestMatchers("/api/auth/sign-in")
                .permitAll()
                .requestMatchers(HttpMethod.POST, "/api/users")
                .permitAll()
                .requestMatchers("/api/auth/refresh")
                .permitAll()
                .requestMatchers("/api/auth/reset-password")
                .permitAll()
                .requestMatchers("*", "/swagger-resource/**"
                    , "/swagger-ui.html", "/swagger-ui/**", "/v3/**",
                    "/assets/**", "/h2/**")
                .permitAll()

                // 어드민 권한
                .requestMatchers(HttpMethod.GET, "/api/users")
                .hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/users/{userId}/role",
                    "/api/users/{userId}/locked")
                .hasRole("ADMIN")
                .anyRequest()
                .authenticated()
            )
            .formLogin(form -> form
                .loginProcessingUrl("/api/auth/sign-in")
                .successHandler(loginSuccessHandler)
                .failureHandler(loginFailureHandler)
            )
            .logout(logout -> logout
                .logoutUrl("/api/auth/sign-out")
                .addLogoutHandler(logoutHandler)
                .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT))
            )
            .cors(withDefaults())
            // 예외처리
//            .exceptionHandling(ex -> ex
//                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
//                .accessDeniedHandler(accessDeniedHandlerImpl)
//            )

            // 매 요청마다 Authorization 헤더의 JWT 검증하는 필터
            .addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class
            )
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS) // 세션 안씀
            )
            .oauth2Login(oauth -> oauth.disable())
            .httpBasic(basic -> basic.disable());

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return NoOpPasswordEncoder.getInstance(); // 테스트용
//        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtRegistry jwtRegistry(JwtTokenProvider jwtTokenProvider) {
        return new InMemoryJwtRegistry(1, jwtTokenProvider);
    }

    @Bean
    public RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.withDefaultRolePrefix()
                                .role(UserRole.ADMIN.name())
                                .implies(UserRole.USER.name())
                                .build();
    }

    static class SpaCsrfTokenRequestHandler implements CsrfTokenRequestHandler {

        private final CsrfTokenRequestHandler plain = new CsrfTokenRequestAttributeHandler();
        private final CsrfTokenRequestHandler xor = new XorCsrfTokenRequestAttributeHandler();

        @Override
        public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            Supplier<CsrfToken> csrfToken
        ) {
            // 1) 응답에 토큰을 렌더링할 때 XOR 방식(BREACH 방어) 적용
            this.xor.handle(request, response, csrfToken);

            // 2) 토큰을 강제로 로드해서 "쿠키로 토큰이 저장/발급되도록" 트리거 확실히
            csrfToken.get();
        }

        @Override
        public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
            String headerValue = request.getHeader(csrfToken.getHeaderName());

            // 헤더가 있으면 SPA 방식(plain)으로 검증
            // 없으면 폼 파라미터(_csrf) 등 XOR 방식으로 검증
            return (StringUtils.hasText(headerValue) ? this.plain : this.xor)
                .resolveCsrfTokenValue(request, csrfToken);
        }
    }
}