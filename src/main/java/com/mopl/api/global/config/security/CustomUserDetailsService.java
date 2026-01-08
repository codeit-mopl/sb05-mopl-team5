package com.mopl.api.global.config.security;

import com.mopl.api.domain.user.entity.User;
import com.mopl.api.domain.user.exception.detail.UserNotFoundException;
import com.mopl.api.domain.user.exception.user.UserLockedException;
import com.mopl.api.domain.user.mapper.UserMapper;
import com.mopl.api.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    @Override
    public UserDetails loadUserByUsername(String email) {
        User user = userRepository.findByEmail(email)
                                  .orElseThrow(() -> UserNotFoundException.withUserEmail(email));
        // 잠긴 계정에 대해 예외 처리
        if (Boolean.TRUE.equals(user.getLocked()))
            throw UserLockedException.withUserEmail(email);

        return new CustomUserDetails(userMapper.toDto(user), user.getPassword());
    }
}
