package com.mopl.api.domain.user.service;

import com.mopl.api.domain.notification.dto.request.NotificationCreateRequest;
import com.mopl.api.domain.notification.service.NotificationService;
import com.mopl.api.domain.user.dto.request.ChangePasswordRequest;
import com.mopl.api.domain.user.dto.request.CursorRequestUserDto;
import com.mopl.api.domain.user.dto.request.UserCreateRequest;
import com.mopl.api.domain.user.dto.request.UserLockUpdateRequest;
import com.mopl.api.domain.user.dto.request.UserRoleUpdateRequest;
import com.mopl.api.domain.user.dto.request.UserUpdateRequest;
import com.mopl.api.domain.user.dto.response.CursorResponseUserDto;
import com.mopl.api.domain.user.dto.response.UserDto;
import com.mopl.api.domain.user.entity.User;
import com.mopl.api.domain.user.entity.UserRole;
import com.mopl.api.domain.user.exception.user.detail.DuplicateEmailException;
import com.mopl.api.domain.user.exception.user.detail.UserNotFoundException;
import com.mopl.api.domain.user.mapper.UserMapper;
import com.mopl.api.domain.user.repository.UserRepository;
import com.mopl.api.global.config.security.jwt.JwtRegistry;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final JwtRegistry jwtRegistry;
    private final ProfileImageStorageService profileImageStorageService;
    private final NotificationService notificationService;

    @Transactional
    @Override
    public UserDto createUser(UserCreateRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw DuplicateEmailException.withUserEmail(request.email());
        }
        String encodedPassword = passwordEncoder.encode(request.password());
        User user = new User(request.email(), encodedPassword, request.name());
        userRepository.saveAndFlush(user);
        log.info("User created! email: {}", user.getEmail());
        return userMapper.toDto(user);
    }

    @Transactional(readOnly = true)
    @Override
    public UserDto getUser(UUID userId) {
        User user = userRepository.findById(userId)
                                  .orElseThrow(() -> UserNotFoundException.withUserId(userId));
        log.info("User Detail username: {}", user.getName());
        return userMapper.toDto(user);
    }

    @Transactional
    @Override
    public void updatePassword(UUID userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                                  .orElseThrow(() -> UserNotFoundException.withUserId(userId));
        String raw = request.password();
        String encoded = passwordEncoder.encode(raw);

        user.updatePassword(encoded);

        jwtRegistry.invalidateJwtInformationByUserId(userId);
    }

    @Transactional(readOnly = true)
    @Override
    public CursorResponseUserDto<UserDto> getAllUsers(CursorRequestUserDto request) {
        return userRepository.findAllUsers(request);
    }

    @Transactional
    @Override
    public void updateUserRole(UUID userId, UserRoleUpdateRequest request) {
        User user = userRepository.findById(userId)
                                  .orElseThrow(() -> UserNotFoundException.withUserId(userId));
        UserRole before = user.getRole();
        user.updateUserRole(request.role());

        // 알림
        notificationService.addNotification(NotificationCreateRequest.builder()
                                                                     .receiverId(userId)
                                                                     .title("내 권한이 변경되었어요.")
                                                                     .content(
                                                                         "내 권한이 [" + before + "]에서 "
                                                                             + "[" + request.role() + "](으)로 변경되었어요.")
                                                                     .build());

        // 자동 로그아웃
        jwtRegistry.invalidateJwtInformationByUserId(userId);
        log.info("User role updated! role: {} -> {}", before, user.getRole());
    }

    @Transactional
    @Override
    public void updateUserLock(UUID userId, UserLockUpdateRequest request) {
        User user = userRepository.findById(userId)
                                  .orElseThrow(() -> UserNotFoundException.withUserId(userId));
        Boolean before = user.getLocked();
        user.updateUserLock(request.locked());
        // 자동 로그아웃
        jwtRegistry.invalidateJwtInformationByUserId(userId);
        log.info("User locked updated! locked: {} -> {}", before, user.getLocked());
    }

    //프로필 변경
    @Transactional
    @Override
    public UserDto profileChange(UUID userId, UUID requesterId, UserUpdateRequest request, MultipartFile image) {
        if (requesterId == null || !requesterId.equals(userId)) {
            // 프로젝트에 권한 예외 커스텀이 있으면 그걸로 교체 추천
            throw new IllegalArgumentException("본인의 프로필만 변경할 수 있습니다.");

        }

        User user = userRepository.findById(userId)
                                  .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        if (request != null && request.name() != null && !request.name()
                                                                 .isBlank()) {
            user.updateName(request.name());
        }

        if (image != null && !image.isEmpty()) {
            String imageUrl = profileImageStorageService.store(userId, image);
            user.updateProfileImageUrl(imageUrl);
        }

        return userMapper.toDto(user);
    }
}