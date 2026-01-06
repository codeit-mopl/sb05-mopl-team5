package com.mopl.api.global.config.websocket.publisher;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;

@RequiredArgsConstructor
public abstract class AbstractRedisPublisher<T> {

    protected final RedisTemplate<String, Object> redisTemplate;

    protected void publish(String channel, T payload) {
        redisTemplate.convertAndSend(channel, payload);
    }
}