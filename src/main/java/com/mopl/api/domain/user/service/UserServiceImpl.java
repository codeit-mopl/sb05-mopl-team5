package com.mopl.api.domain.user.service;

import com.mopl.api.domain.user.dto.request.ChangePasswordRequest;
import com.mopl.api.domain.user.dto.request.CursorRequestUserDto;
import com.mopl.api.domain.user.dto.request.UserRoleUpdateRequest;
import com.mopl.api.domain.user.dto.response.CursorResponseUserDto;
import com.mopl.api.domain.user.dto.request.UserCreateRequest;
import com.mopl.api.domain.user.dto.response.UserDto;
import com.mopl.api.domain.user.dto.request.UserLockUpdateRequest;
import com.mopl.api.domain.user.dto.request.UserUpdateRequest;
import com.mopl.api.domain.user.entity.User;
import com.mopl.api.domain.user.entity.User;
import com.mopl.api.domain.user.entity.UserRole;
import com.mopl.api.domain.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import com.mopl.api.domain.user.mapper.UserMapper;
import com.mopl.api.domain.user.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
//    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    private final UserRepository userRepository;

    private final ProfileImageStorageService profileImageStorageService;

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



    //프로필 변경
    public UserDto profileChange(UUID userId, UUID requesterId, UserUpdateRequest request, MultipartFile image) {
        if (requesterId == null || !requesterId.equals(userId)) {
            // 프로젝트에 권한 예외 커스텀이 있으면 그걸로 교체 추천
            throw new IllegalArgumentException("본인의 프로필만 변경할 수 있습니다.");

        }

        User user = userRepository.findById(userId).orElseThrow( ()-> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        if (request != null &&  request.name() != null && !request.name().isBlank()){
            user.changeName(request.name());
        }


        if (image != null && !image.isEmpty()) {
            String imageUrl = profileImageStorageService.store(userId, image);
            user.changeProfileImageUrl(imageUrl);
        }


        return UserDto.from(user);

    }
}
