package com.mopl.api.domain.follow.controller;

import com.mopl.api.domain.follow.dto.request.FollowRequest;
import com.mopl.api.domain.follow.dto.response.FollowDto;
import com.mopl.api.domain.follow.service.FollowService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/follows")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;

    @PostMapping
    public ResponseEntity<FollowDto> createFollow(
        @RequestHeader("X-User-Id") UUID followerId,
        @RequestBody @Valid FollowRequest request
    ) {
        FollowDto dto = followService.createFollow(followerId, request.followeeId());
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping("/followed-by-me")
    public ResponseEntity<Boolean> isFollowedByMe(
        @RequestHeader("X-User-Id") UUID followerId,
        @RequestParam UUID followeeId
    ) {
        return ResponseEntity.ok(followService.isFollowedByMe(followerId, followeeId));
    }

    @GetMapping("/count")
    public ResponseEntity<Long> getFollowerCount(@RequestParam UUID followeeId) {
        return ResponseEntity.ok(followService.getFollowerCount(followeeId));
    }

    @DeleteMapping("/{followId}")
    public ResponseEntity<Void> cancelFollow(
        @RequestHeader("X-User-Id") UUID followerId,
        @PathVariable UUID followId
    ) {
        followService.cancelFollow(followerId, followId);
        return ResponseEntity.noContent().build();
    }
}
