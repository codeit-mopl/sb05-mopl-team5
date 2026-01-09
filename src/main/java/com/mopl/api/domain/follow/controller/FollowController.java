package com.mopl.api.domain.follow.controller;

import com.mopl.api.domain.follow.dto.request.FollowRequest;
import com.mopl.api.domain.follow.dto.response.FollowDto;
import com.mopl.api.domain.follow.service.FollowService;
import com.mopl.api.global.config.security.CustomUserDetails;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/follows")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;

    private UUID currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || auth.getPrincipal() == null) {
            throw new IllegalStateException("인증 정보가 없습니다.");
        }

        Object principal = auth.getPrincipal();

        if (principal instanceof CustomUserDetails cud && cud.getUserDto() != null) {
            return cud.getUserDto().id();
        }

        if (principal instanceof UUID uuid) {
            return uuid;
        }

        String name = auth.getName();
        try {
            return UUID.fromString(name);
        } catch (Exception ignored) {

        }

        throw  new IllegalStateException("현재 사용자 ID를 확인할 수 없습니다.");
    }


    @PostMapping
    public ResponseEntity<FollowDto> createFollow(
        @RequestBody @Valid FollowRequest request
    ) {
        UUID followerId = currentUserId();

        FollowDto dto = followService.createFollow(followerId, request.followeeId());
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping("/followed-by-me")
    public ResponseEntity<Boolean> isFollowedByMe(

        @RequestParam UUID followeeId
    ) {
        UUID followerId = currentUserId();
        return ResponseEntity.ok(followService.isFollowedByMe(followerId, followeeId));
    }

    @GetMapping("/count")
    public ResponseEntity<Long> getFollowerCount(@RequestParam UUID followeeId) {
        return ResponseEntity.ok(followService.getFollowerCount(followeeId));
    }

    @DeleteMapping("/{followId}")
    public ResponseEntity<Void> cancelFollow(

        @PathVariable UUID followId
    ) {
        UUID followerId = currentUserId();
        followService.cancelFollow(followerId, followId);
        return ResponseEntity.noContent().build();
    }
}
