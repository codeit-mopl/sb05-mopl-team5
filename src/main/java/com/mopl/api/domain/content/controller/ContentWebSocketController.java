package com.mopl.api.domain.content.controller;

import com.mopl.api.domain.content.service.ContentChatService;
import com.mopl.api.domain.user.dto.command.WatchingSessionCreateCommand;
import com.mopl.api.domain.user.service.WatchingSessionService;
import com.mopl.api.global.config.websocket.dto.ContentChatSendRequest;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ContentWebSocketController {
    // TODO WatchingSessionWebsocketController 등으로 네이밍, 패키지 변경 고려해보자

    private final ContentChatService contentChatService;
    private final WatchingSessionService watchingSessionService;

    @MessageMapping("/contents/{contentId}/chat")
    public void chatSend(
        @DestinationVariable UUID contentId,
        @Payload @Valid ContentChatSendRequest request,
        Principal principal
    ) {
        contentChatService.sendChat();
    }

    @MessageMapping("/contents/{contentId}/watch/join")
    public void watchingSessionJoin(
        @DestinationVariable UUID contentId,
        Principal principal
    ) {
        watchingSessionService.addWatchingSession(
            WatchingSessionCreateCommand.builder()
                                        .contentId(contentId)
                                        .watcherId(UUID.fromString(principal.getName()))
                                        .build()
        );
    }

    @MessageMapping("/contents/{contentId}/watch/leave")
    public void watchingSessionLeave(
        @DestinationVariable UUID contentId,
        Principal principal
    ) {
        watchingSessionService.removeWatchingSession(
            contentId
        );
    }
}