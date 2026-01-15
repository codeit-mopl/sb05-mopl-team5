package com.mopl.api.config;

import com.mopl.api.domain.user.entity.UserRole;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@TestConfiguration
@EnableWebSecurity
public class TestSecurityConfig {

    @Bean
    public RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.withDefaultRolePrefix()
                                .role(UserRole.ADMIN.name())
                                .implies(UserRole.USER.name())
                                .build();
    }

    @Bean
    @Primary
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // controller 테스트 시 crsf, jwt 인증 없이 가능하도록
        http.csrf(AbstractHttpConfigurer::disable);
        http.authorizeHttpRequests(auth -> auth.requestMatchers(HttpMethod.POST, "/api/contents")
                                               .hasRole("ADMIN")
                                               .requestMatchers(HttpMethod.PATCH, "/api/contents/{contentId}")
                                               .hasRole("ADMIN")
                                               .requestMatchers(HttpMethod.DELETE, "/api/contents/{contentId}")
                                               .hasRole("ADMIN")
                                               .anyRequest()
                                               .permitAll());
        return http.build();
    }
}