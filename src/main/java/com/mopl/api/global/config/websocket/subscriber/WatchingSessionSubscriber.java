package com.mopl.api.global.config.websocket.subscriber;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mopl.api.global.config.redis.constant.RedisChannel;
import com.mopl.api.global.config.websocket.dto.WatchingSessionChange;
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
public class WatchingSessionSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
            UUID contentId = extractContentId(channel);

            WatchingSessionChange payload =
                objectMapper.readValue(message.getBody(), WatchingSessionChange.class);

            log.debug("channel={}, payload={}", channel, payload);

            messagingTemplate.convertAndSend(
                RedisChannel.WATCHING_SESSION.websocketDestination(contentId),
                payload
            );
        } catch (Exception e) {
            log.error("WatchingSessionSubscriber 처리 실패", e);
        }
    }

    private UUID extractContentId(String channel) {
        // watching-session:{contentId}
        return UUID.fromString(channel.split(":")[1]);
    }
}