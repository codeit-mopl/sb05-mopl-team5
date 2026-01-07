package com.mopl.api.domain.content.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import com.mopl.api.domain.content.service.ContentChatService;
import com.mopl.api.domain.user.service.WatchingSessionService;
import com.mopl.api.global.config.websocket.dto.ContentChatSendRequest;
import com.mopl.api.global.config.websocket.publisher.RedisWatchingSessionPublisher;
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
    RedisWatchingSessionPublisher publisher;

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
}