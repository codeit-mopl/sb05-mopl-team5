package com.mopl.api.domain.follow.controller;

import com.mopl.api.domain.follow.dto.request.FollowRequest;
import com.mopl.api.domain.follow.dto.response.FollowDto;
import com.mopl.api.domain.follow.service.FollowService;
import com.mopl.api.global.config.security.claim.CustomUserDetails;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/follows")
@RequiredArgsConstructor

public class FollowController {

    private final FollowService followService;

    @PostMapping
    public ResponseEntity<FollowDto> createFollow(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @RequestBody @Valid FollowRequest request
    ) {
        UUID me = userDetails.getUserDto().id();
        return ResponseEntity.status(HttpStatus.CREATED)
                                      .body(followService.createFollow(me, request.followeeId()));
    }

    @GetMapping("/followed-by-me")
    public ResponseEntity<Boolean> isFollowedByMe(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @RequestParam UUID followeeId
    ) {
        UUID me = userDetails.getUserDto().id();
        return ResponseEntity.ok(followService.isFollowedByMe(me, followeeId));
    }

    @GetMapping("/count")
    public ResponseEntity<Long> getFollowerCount(
        @RequestParam("followeeId") UUID followeeId // @RequestParam 필수
    ) {
        Long count = followService.getFollowerCount(followeeId);
        return ResponseEntity.ok(count);
    }

    @DeleteMapping("/{followId}")
    public ResponseEntity<Void> cancelFollow(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @PathVariable UUID followId
    ) {
        UUID me = userDetails.getUserDto().id();
        followService.cancelFollow(me, followId);
        return ResponseEntity.noContent().build();
    }

}
