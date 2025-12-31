package com.mopl.api.domain.notification.controller;

import com.mopl.api.domain.notification.dto.NotificationDto;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/*
GET
/api/notifications
알림 목록 조회 (커서 페이지네이션)

DELETE
/api/notifications/{notificationId}
알림 읽음 처리
 */

@Tag(name = "알림", description = "알림 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {

    @GetMapping
    public ResponseEntity<NotificationDto> notificationDetail(UUID userI) {
        // TODO ...
        return ResponseEntity.ok(NotificationDto.builder()
                                                .build());
    }

    @DeleteMapping("/{NotificationId}")
    public ResponseEntity<NotificationDto> notificationRemove(UUID notificationId) {
        // TODO ...
        return ResponseEntity.noContent()
                             .build();
    }
}
