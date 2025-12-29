package com.mopl.api.domain.user.controller;

import com.mopl.api.domain.user.dto.WatchingSessionDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    @GetMapping("/{watcherId}/watching-sessions")
    public ResponseEntity<WatchingSessionDto> getWatchingSession(Long watcherId, Long contentId) {
        return ResponseEntity.ok(WatchingSessionDto.builder()
                                                   .build());
    }
}