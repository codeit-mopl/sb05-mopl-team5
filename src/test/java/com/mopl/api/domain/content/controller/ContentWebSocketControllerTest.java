package com.mopl.api.domain.content.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mopl.api.domain.content.service.ContentChatService;
import com.mopl.api.domain.user.dto.command.WatchingSessionCreateCommand;
import com.mopl.api.domain.user.service.WatchingSessionService;
import com.mopl.api.global.config.websocket.dto.ContentChatSendRequest;
import com.mopl.api.global.config.websocket.publisher.WatchingSessionPublisher;
import java.security.Principal;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContentWebSocketControllerTest {

    @Mock
    ContentChatService contentChatService;

    @Mock
    WatchingSessionService watchingSessionService;

    @Mock
    WatchingSessionPublisher publisher;

    @InjectMocks
    ContentWebSocketController controller;

    @Mock
    Principal principal;

    @Test
    @DisplayName("콘텐츠 채팅 전송 시 ContentChatService.sendChat이 호출")
    void sendChat_shouldDelegateToChatService() {

        UUID contentId = UUID.randomUUID();
        ContentChatSendRequest request = new ContentChatSendRequest("hello");

//        when(principal.getName()).thenReturn(UUID.randomUUID().toString());

        controller.chatSend(contentId, request, principal);

        verify(contentChatService).sendChat();
    }

    @Test
    @DisplayName("시청 세션 JOIN 시 WatchingSessionService.addWatchingSession 호출")
    void joinWatchingSession_shouldCallAddWatchingSession() {

        UUID contentId = UUID.randomUUID();
        UUID watcherId = UUID.randomUUID();

        when(principal.getName()).thenReturn(watcherId.toString());

        controller.watchingSessionJoin(contentId, principal);

        verify(watchingSessionService).addWatchingSession(
            any(WatchingSessionCreateCommand.class)
        );
    }

    @Test
    @DisplayName("시청 세션 LEAVE 시 WatchingSessionService.removeWatchingSession 호출")
    void leaveWatchingSession_shouldCallRemoveWatchingSession() {

        UUID contentId = UUID.randomUUID();
        UUID watcherId = UUID.randomUUID();

//        when(principal.getName()).thenReturn(watcherId.toString());

        controller.watchingSessionLeave(contentId, principal);

        verify(watchingSessionService).removeWatchingSession(contentId);
    }
}