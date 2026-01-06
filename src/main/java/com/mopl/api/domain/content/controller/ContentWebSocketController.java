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

    private final ContentChatService contentChatService;
    private final WatchingSessionService watchingSessionService;

    /**
     * 콘텐츠 채팅 전송 Client: SEND /pub/contents/{contentId}/chat
     */
    @MessageMapping("/contents/{contentId}/chat")
    public void chatSend(
        @DestinationVariable UUID contentId,
        @Payload @Valid ContentChatSendRequest request,
        Principal principal
    ) {
        contentChatService.sendChat();
    }

    /**
     * 시청 세션 입장 (JOIN) Client: SEND /pub/contents/{contentId}/watch/join
     */
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

    /**
     * 시청 세션 퇴장 (LEAVE) Client: SEND /pub/contents/{contentId}/watch/leave
     */
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