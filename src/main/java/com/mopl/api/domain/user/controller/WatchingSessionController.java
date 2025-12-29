package com.mopl.api.domain.user.controller;

import com.mopl.api.domain.user.dto.WatchingSessionDto;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/*
GET
/api/users/{watcherId}/watching-sessions
특정 사용자의 시청 세션 조회 (nullable)


GET
/api/contents/{contentId}/watching-sessions
특정 콘텐츠의 시청 세션 목록 조회 (커서 페이지네이션)
 */

@Tag(name = "시청 세션 관리", description = "시청 세션 관리 API")
@RestController
public class WatchingSessionController {

    @GetMapping("/api/users/{watcherId}/watching-sessions")
    public ResponseEntity<WatchingSessionDto> getWatchingSession(Long watcherId, Long contentId) {
        // TODO 추후 세션 관련 작업

        // Wathcing 관련 DB 인서트
        // Redis 관련 처리...

        return ResponseEntity.ok(WatchingSessionDto.builder()
                                                   .build());
    }


    @GetMapping("/api/contents/{contentId}/watching-sessions")
    public ResponseEntity<WatchingSessionDto> getContent(Long watcherId, Long contentId) {
        return ResponseEntity.ok(WatchingSessionDto.builder()
                                                   .build());
    }
}
