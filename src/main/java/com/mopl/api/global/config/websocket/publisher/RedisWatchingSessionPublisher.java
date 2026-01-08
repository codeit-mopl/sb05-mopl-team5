package com.mopl.api.global.config.websocket.publisher;

import com.mopl.api.global.config.redis.constant.RedisChannel;
import com.mopl.api.global.config.websocket.dto.WatchingSessionChange;
import java.util.UUID;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisWatchingSessionPublisher
    extends AbstractRedisPublisher<WatchingSessionChange> implements WatchingSessionEventPublisher {

    public RedisWatchingSessionPublisher(RedisTemplate<String, Object> redisTemplate) {
        super(redisTemplate);
    }

    public void publish(UUID contentId, WatchingSessionChange payload) {
        publish(
            RedisChannel.WATCHING_SESSION.channel(contentId),
            payload
        );
    }
}