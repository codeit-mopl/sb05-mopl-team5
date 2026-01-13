package com.mopl.api.global.config.security;

import static org.springframework.security.config.Customizer.withDefaults;

import com.mopl.api.domain.user.entity.UserRole;
import com.mopl.api.global.config.oauth.handler.OAuth2UserSuccessHandler;
import com.mopl.api.global.config.oauth.service.CustomOAuth2UserService;
import com.mopl.api.global.config.security.filter.JwtAuthenticationFilter;
import com.mopl.api.global.config.security.handler.AccessDeniedHandlerImpl;
import com.mopl.api.global.config.security.handler.JwtAuthenticationEntryPoint;
import com.mopl.api.global.config.security.handler.JwtLogoutHandler;
import com.mopl.api.global.config.security.handler.LoginFailureHandler;
import com.mopl.api.global.config.security.handler.LoginSuccessHandler;
import com.mopl.api.global.config.security.handler.SpaCsrfTokenRequestHandler;
import com.mopl.api.global.config.security.jwt.InMemoryJwtRegistry;
import com.mopl.api.global.config.security.jwt.JwtRegistry;
import com.mopl.api.global.config.security.jwt.JwtTokenProvider;
import com.mopl.api.global.config.security.provider.TempPasswordAuthenticationProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(
        HttpSecurity http,
        LoginSuccessHandler loginSuccessHandler,
        LoginFailureHandler loginFailureHandler,
        JwtLogoutHandler logoutHandler,
        TempPasswordAuthenticationProvider tempPasswordAuthenticationProvider,
        PasswordEncoder passwordEncoder,
        UserDetailsService customUserDetailsService,
        JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
        JwtAuthenticationFilter jwtAuthenticationFilter,
        AccessDeniedHandlerImpl accessDeniedHandlerImpl,
        CustomOAuth2UserService oAuth2UserService,
        OAuth2UserSuccessHandler oAuth2UserSuccessHandler
    ) throws Exception {
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
                .requestMatchers("/contents/**")
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
                // 웹 소켓 handshake는 허용, stomp 시 사용자 검사
                .requestMatchers("/ws/**")
                .permitAll()
                .requestMatchers("/ws")
                .permitAll()

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
            .oauth2Login(oauth -> oauth
                .loginPage("/api/auth/sign-in")
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(oAuth2UserService))
                .successHandler(oAuth2UserSuccessHandler))
            .httpBasic(basic -> basic.disable());

        // 임시 비밀번호 검증
        AuthenticationManagerBuilder authBuilder =
            http.getSharedObject(AuthenticationManagerBuilder.class);
        authBuilder.authenticationProvider(tempPasswordAuthenticationProvider)
                   .userDetailsService(customUserDetailsService)
                   .passwordEncoder(passwordEncoder);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
//        return NoOpPasswordEncoder.getInstance();
        return new BCryptPasswordEncoder();
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
}