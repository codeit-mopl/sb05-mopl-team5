package com.mopl.api.global.config.redis;

import com.mopl.api.global.config.websocket.subscriber.ContentChatSubscriber;
import com.mopl.api.global.config.websocket.subscriber.WatchingSessionSubscriber;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
@RequiredArgsConstructor
public class RedisSubscriberConfig {

    private final RedisConnectionFactory connectionFactory;
    private final WatchingSessionSubscriber watchingSessionSubscriber;
    private final ContentChatSubscriber contentChatSubscriber;

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer() {

        RedisMessageListenerContainer container =
            new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        container.addMessageListener(
            watchingSessionSubscriber,
            new PatternTopic("watching-session:*")
        );

        container.addMessageListener(
            contentChatSubscriber,
            new PatternTopic("content-chat:*")
        );

        return container;
    }
}