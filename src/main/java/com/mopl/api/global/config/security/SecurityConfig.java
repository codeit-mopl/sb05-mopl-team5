package com.mopl.api.global.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Profile("!test")
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // TODO 추후 필요한 설정
        http
            // CSRF 비활성화 (API 서버 기준)
            .csrf(csrf -> csrf.disable())
            // 모든 요청 허용
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            )
            // 로그인 관련 기능 전부 비활성화
            .formLogin(form -> form.disable())
            .oauth2Login(oauth -> oauth.disable())
            .httpBasic(basic -> basic.disable());

        return http.build();
    }
}