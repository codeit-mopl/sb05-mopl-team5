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
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

//    private final AuthenticationManager authenticationManager;

    @Bean
    public SecurityFilterChain filterChain(
        HttpSecurity http,
        LoginSuccessHandler loginSuccessHandler,
        LoginFailureHandler loginFailureHandler,
        JwtLogoutHandler logoutHandler,
        PasswordEncoder passwordEncoder,
        JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
        JwtAuthenticationFilter jwtAuthenticationFilter,
        AccessDeniedHandlerImpl accessDeniedHandlerImpl) throws Exception {
        http
            // CSRF 비활성화 (API 서버 기준)
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/csrf-token").permitAll()
                .requestMatchers("/api/auth/sign-in").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/users").permitAll()
                .requestMatchers("/api/auth/refresh").permitAll()
                .requestMatchers("/api/auth/reset-password").permitAll()
                .requestMatchers( "*", "/swagger-resource/**"
                    , "/swagger-ui.html", "/swagger-ui/**", "/v3/**",
                    "/assets/**","/h2/**").permitAll()

                // 어드민 권한
                .requestMatchers(HttpMethod.GET, "/api/users").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/users/{userId}/role",
                    "/api/users/{userId}/locked").hasRole("ADMIN")
                .anyRequest().authenticated()
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
            .oauth2Login(oauth -> oauth.disable())
            .httpBasic(basic -> basic.disable());

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
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
//                .role(Role.CHANNEL_MANAGER.name())
//                .implies(Role.USER.name())
                                .build();
    }
}