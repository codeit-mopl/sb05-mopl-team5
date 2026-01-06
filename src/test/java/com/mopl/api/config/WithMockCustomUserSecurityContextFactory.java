package com.mopl.api.config;

import com.mopl.api.domain.user.dto.response.UserDto;
import com.mopl.api.domain.user.entity.UserRole;
import com.mopl.api.global.config.security.CustomUserDetails;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.time.LocalDateTime;
import java.util.UUID;

public class WithMockCustomUserSecurityContextFactory implements WithSecurityContextFactory<WithMockCustomUser> {

    @Override
    public SecurityContext createSecurityContext(WithMockCustomUser annotation) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();

        UUID userId = UUID.fromString(annotation.userId());
        
        UserDto userDto = UserDto.builder()
            .id(userId)
            .email("test@example.com")
            .name("Test User")
            .role(UserRole.USER)
            .profileImageUrl(null)
            .locked(false)
            .createdAt(LocalDateTime.now())
            .build();

        CustomUserDetails principal = new CustomUserDetails(userDto, "password");

        Authentication auth = new UsernamePasswordAuthenticationToken(
            principal,
            null,
            principal.getAuthorities()
        );

        context.setAuthentication(auth);
        return context;
    }
}
