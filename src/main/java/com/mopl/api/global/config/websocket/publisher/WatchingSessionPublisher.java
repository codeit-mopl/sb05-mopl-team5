package com.mopl.api.global.config.websocket.publisher;

import com.mopl.api.global.config.redis.constant.RedisChannel;
import com.mopl.api.global.config.websocket.dto.WatchingSessionChange;
import java.util.UUID;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class WatchingSessionPublisher
    extends AbstractRedisPublisher<WatchingSessionChange> {

    public WatchingSessionPublisher(RedisTemplate<String, Object> redisTemplate) {
        super(redisTemplate);
    }

    public void publish(UUID contentId, WatchingSessionChange payload) {
        publish(
            RedisChannel.WATCHING_SESSION.channel(contentId),
            payload
        );

        // TODO 세션 관련 정보 DB insert는 어디서 할 지 고민 필요
        /*
           → DB 저장
           → Redis 캐시 저장
           → WatchingSessionChange 생성
           → WatchingSessionPublisher.publish()
         */
    }
}