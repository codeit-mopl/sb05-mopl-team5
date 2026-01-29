package com.mopl.api.global.config.security;

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
import com.mopl.api.global.config.security.provider.TempPasswordAuthenticationProvider;
import jakarta.servlet.DispatcherType;
import java.util.List;
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
import org.springframework.web.cors.CorsConfiguration;

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
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            )
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .httpBasic(basic -> basic.disable())
            .formLogin(form -> form.disable())
            .oauth2Login(oauth2 -> oauth2.disable());

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

//    @Bean
//    public JwtRegistry jwtRegistry(JwtTokenProvider jwtTokenProvider) {
//        return new InMemoryJwtRegistry(1, jwtTokenProvider);
//    }

    @Bean
    public RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.withDefaultRolePrefix()
                                .role(UserRole.ADMIN.name())
                                .implies(UserRole.USER.name())
                                .build();
    }
}