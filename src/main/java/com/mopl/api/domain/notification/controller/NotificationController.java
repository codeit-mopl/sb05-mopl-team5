package com.mopl.api.domain.notification.controller;

import com.mopl.api.domain.notification.dto.request.NotificationCursorPageRequest;
import com.mopl.api.domain.notification.dto.request.NotificationCursorPageRequest.SortBy;
import com.mopl.api.domain.notification.dto.request.NotificationCursorPageRequest.SortDirection;
import com.mopl.api.domain.notification.dto.response.CursorResponseNotificationDto;
import com.mopl.api.domain.notification.service.NotificationService;
import com.mopl.api.global.config.security.claim.CustomUserDetails;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "알림", description = "알림 API")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<CursorResponseNotificationDto> notificationList(
        @AuthenticationPrincipal CustomUserDetails user,
        @RequestParam(required = false) String cursor,
        @RequestParam(required = false) UUID idAfter,
        @Parameter(required = true) @RequestParam int limit,
        @Parameter(required = true) @RequestParam String sortDirection,
        @Parameter(required = true) @RequestParam String sortBy
    ) {
        UUID userId = user.getUserDto()
                          .id();

        NotificationCursorPageRequest request = NotificationCursorPageRequest.builder()
                                                                             .cursor(cursor)
                                                                             .idAfter(idAfter)
                                                                             .limit(limit)
                                                                             .sortDirection(
                                                                                 SortDirection.valueOf(sortDirection))
                                                                             .sortBy(SortBy.valueOf(sortBy))
                                                                             .build();

        return ResponseEntity.ok(notificationService.getNotifications(userId, request));
    }

    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Void> notificationRemove(
        @PathVariable UUID notificationId,
        @AuthenticationPrincipal CustomUserDetails user
    ) {
        UUID userId = user.getUserDto()
                          .id();

        notificationService.removeNotification(notificationId, userId);

        return ResponseEntity.noContent()
                             .build();
    }
}
