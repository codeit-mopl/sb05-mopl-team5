package com.mopl.api.domain.content.service;

import com.mopl.api.global.config.websocket.dto.ContentChatSendRequest;
import java.util.UUID;

public interface ContentChatService {

    void sendChat(UUID contentId, UUID senderId, ContentChatSendRequest request);
}