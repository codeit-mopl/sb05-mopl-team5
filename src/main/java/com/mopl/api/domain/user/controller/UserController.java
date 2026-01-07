package com.mopl.api.domain.user.controller;

import com.mopl.api.domain.user.dto.request.ChangePasswordRequest;
import com.mopl.api.domain.user.dto.request.CursorRequestUserDto;
import com.mopl.api.domain.user.dto.request.UserUpdateRequest;
import com.mopl.api.domain.user.dto.response.CursorResponseUserDto;
import com.mopl.api.domain.user.dto.request.UserCreateRequest;
import com.mopl.api.domain.user.dto.response.UserDto;
import com.mopl.api.domain.user.dto.request.UserLockUpdateRequest;
import com.mopl.api.domain.user.dto.request.UserRoleUpdateRequest;
import com.mopl.api.domain.user.service.UserService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<UserDto> userAdd(@RequestBody UserCreateRequest request) {
        UserDto response = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserDto> userDetails(@PathVariable UUID userId) {
        UserDto response = userService.getUser(userId);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PatchMapping("/{userId}/password")
    public ResponseEntity<Void> passwordModify(
        @PathVariable UUID userId,
        @RequestBody ChangePasswordRequest request
    ) {
        userService.updatePassword(userId, request);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CursorResponseUserDto<UserDto>> userList(CursorRequestUserDto request) {
        CursorResponseUserDto<UserDto> response = userService.getAllUsers(request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PatchMapping("/{userId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> userRoleModify(@PathVariable UUID userId, @RequestBody UserRoleUpdateRequest request) {
        userService.updateUserRole(userId, request);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @PatchMapping("/{userId}/locked")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> userLockModify(@PathVariable UUID userId, @RequestBody UserLockUpdateRequest request) {
        userService.updateUserLock(userId, request);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    //프로필 변경
    @PatchMapping("/{userId}")
    public ResponseEntity<UserDto> profileChange(
        @PathVariable UUID userId,
        @RequestHeader("X-User-Id") UUID requesterId,
        @RequestPart("request") @Valid UserUpdateRequest request,
        @RequestPart(value = "image", required = false) MultipartFile image) {
        UserDto userDto = userService.profileChange(userId, requesterId, request, image);
        return ResponseEntity.status(HttpStatus.OK).body(userDto);

    }


}