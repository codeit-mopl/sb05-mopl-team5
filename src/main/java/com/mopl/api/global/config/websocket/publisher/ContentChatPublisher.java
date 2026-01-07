package com.mopl.api.global.config.websocket.publisher;

import com.mopl.api.global.config.redis.constant.RedisChannel;
import com.mopl.api.global.config.websocket.dto.ContentChatDto;
import java.util.UUID;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class ContentChatPublisher
    extends AbstractRedisPublisher<ContentChatDto> {

    public ContentChatPublisher(RedisTemplate<String, Object> redisTemplate) {
        super(redisTemplate);
    }

    public void publish(UUID contentId, ContentChatDto payload) {
        publish(
            RedisChannel.CONTENT_CHAT.channel(contentId),
            payload
        );
        // TODO 채팅 웹소켓에서 이벤트 발행 예정
    }
}