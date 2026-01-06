package com.mopl.api.global.config.websocket.subscriber;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mopl.api.global.config.redis.constant.RedisChannel;
import com.mopl.api.global.config.websocket.dto.ContentChatDto;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContentChatSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
            UUID contentId = extractContentId(channel);

            ContentChatDto payload =
                objectMapper.readValue(message.getBody(), ContentChatDto.class);

            messagingTemplate.convertAndSend(
                RedisChannel.CONTENT_CHAT.websocketDestination(contentId),
                payload
            );
        } catch (Exception e) {
            log.error("ContentChatSubscriber 처리 실패", e);
        }
    }

    private UUID extractContentId(String channel) {
        return UUID.fromString(channel.split(":")[1]);
    }
}