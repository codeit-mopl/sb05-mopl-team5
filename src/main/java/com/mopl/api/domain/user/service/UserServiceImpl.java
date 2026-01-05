package com.mopl.api.domain.user.service;

import com.mopl.api.domain.user.dto.request.ChangePasswordRequest;
import com.mopl.api.domain.user.dto.request.CursorRequestUserDto;
import com.mopl.api.domain.user.dto.request.UserRoleUpdateRequest;
import com.mopl.api.domain.user.dto.response.CursorResponseUserDto;
import com.mopl.api.domain.user.dto.request.UserCreateRequest;
import com.mopl.api.domain.user.dto.response.UserDto;
import com.mopl.api.domain.user.dto.request.UserLockUpdateRequest;
import com.mopl.api.domain.user.entity.User;
import com.mopl.api.domain.user.entity.UserRole;
import com.mopl.api.domain.user.mapper.UserMapper;
import com.mopl.api.domain.user.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
//    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Transactional
    @Override
    public UserDto createUser(UserCreateRequest request) {
        if(userRepository.existsByEmail(request.email())){
            throw new RuntimeException("Email already exists");
        }
//        String encodedPassword = passwordEncoder.encode(request.password());
        User user = new User(request.email(), request.password(), request.name());
        userRepository.save(user);
        log.info("User created! email: {}", user.getEmail());
        return userMapper.toDto(user);
    }

    @Transactional(readOnly = true)
    @Override
    public UserDto getUser(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UsernameNotFoundException(userId.toString()));
       // TODO user 예외 상황 처리 필요!
        log.info("User Detail username: {}", user.getName());
        return userMapper.toDto(user);
    }

    @Override
    public void updatePassword(UUID userId, ChangePasswordRequest request) {

    }

    @Transactional(readOnly = true)
    @Override
    public CursorResponseUserDto<UserDto> getAllUsers(CursorRequestUserDto request) {
        return userRepository.findAllUsers(request);
    }

    @Transactional
    @Override
    public void updateUserRole(UUID userId, UserRoleUpdateRequest request) {
        User user = userRepository.findById(userId).orElseThrow(()-> new RuntimeException("User not found!") );
        UserRole oldRole = user.getRole();
        user.updateUserRole(request.role());
    }

    @Override
    public void updateUserLock(UUID userId, UserLockUpdateRequest request) {

    }


    @Override
    public UserDto profileChange(UUID userId, String image) {
        return null;
    }
}
