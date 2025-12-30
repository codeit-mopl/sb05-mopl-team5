package com.mopl.api.domain.user.service;

import com.mopl.api.domain.user.dto.response.JwtDto;
import com.mopl.api.domain.user.dto.response.UserDto;
import com.mopl.api.domain.user.entity.UserRole;
import com.mopl.api.global.config.security.jwt.JwtTokenProvider;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AuthServiceImpl implements AuthService {

//    private final UserDetailsService userDetailsService;
    private final JwtTokenProvider jwtProvider;

    @Override
    public JwtDto refreshToken(String refreshToken) {

        JwtDto dummy = JwtDto.builder()
            .userDto(
                UserDto.builder()
                    .id(UUID.randomUUID())
                    .createdAt(LocalDate.now()
                                        .atStartOfDay())
                    .email("alice@test.com")
                    .name("alice")
                    .profileImageUrl(null)
                    .role(UserRole.USER)
                    .locked(true)
                    .build()
            )
        .accessToken("dkaldkfjaldkfas").build();

        return dummy;

    }

    @Override
    public JwtDto signIn(String username, String password) {
        return null;
    }
}
