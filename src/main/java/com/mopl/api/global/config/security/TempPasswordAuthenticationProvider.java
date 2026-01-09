package com.mopl.api.global.config.security;

import com.mopl.api.domain.user.entity.User;
import com.mopl.api.domain.user.exception.detail.UserNotFoundException;
import com.mopl.api.domain.user.mapper.UserMapper;
import com.mopl.api.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
@RequiredArgsConstructor
public class TempPasswordAuthenticationProvider implements AuthenticationProvider {

    private static final String TEMP_PW_KEY_PREFIX = "temp-pw:";

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final RedisTemplate<String, String> redisTemplate;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String email = authentication.getName();
        String beforePassword = (authentication.getCredentials() == null) ? null :authentication.getCredentials().toString();

        if(email == null || beforePassword == null) {
            throw new BadCredentialsException("Invalid credentials");
        }

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> UserNotFoundException.withUserEmail(email));

        // Redis에서 임시 비밀번호 해시값 조회
        String key = TEMP_PW_KEY_PREFIX + email;
        String hashedTempPassword = redisTemplate.opsForValue().get(key);

        if(hashedTempPassword == null) {
            return null; // 다음 Provider로 넘기기(일반 비번 로그인 가능)
        }

        // 임시 비밀번호 검증
        if(!passwordEncoder.matches(beforePassword, hashedTempPassword)) {
            throw new BadCredentialsException("Invalid credentials");
        }

        // 1회성 사용 처리 -> 성공하면 삭제
        redisTemplate.delete(key);

        // 인증 성공 Authentication 생성
        CustomUserDetails userDetails = new CustomUserDetails(userMapper.toDto(user), user.getPassword());
        return new UsernamePasswordAuthenticationToken(
            userDetails,
            null,
            userDetails.getAuthorities()
        );
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
