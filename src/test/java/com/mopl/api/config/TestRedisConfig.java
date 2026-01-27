package com.mopl.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@TestConfiguration
@EnableCaching
public class TestRedisConfig {

    @Bean
    @Primary
    public RedisConnectionFactory testRedisConnectionFactory() {
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration("localhost", 6379);
        return new LettuceConnectionFactory(redisConfig);
    }

    @Bean
    @Primary
    public RedisTemplate<String, String> testRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        return template;
    }

    @Bean
    @Primary
    public RedisTemplate<String, Object> testRedisObjectTemplate(
        RedisConnectionFactory connectionFactory,
        ObjectMapper objectMapper
    ) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);

        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);

        return template;
    }

    @Bean
    @Primary
    public RedisCacheManager testCacheManager(
        RedisConnectionFactory connectionFactory,
        ObjectMapper objectMapper
    ) {
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                                                                .entryTtl(Duration.ofMinutes(60))
                                                                .disableCachingNullValues()
                                                                .serializeKeysWith(
                                                                    RedisSerializationContext.SerializationPair.fromSerializer(
                                                                        new StringRedisSerializer())
                                                                )
                                                                .serializeValuesWith(
                                                                    RedisSerializationContext.SerializationPair.fromSerializer(
                                                                        serializer)
                                                                );

        Map<String, RedisCacheConfiguration> configurations = new HashMap<>();
        configurations.put("playlistCount", config.entryTtl(Duration.ofMinutes(10)));
        configurations.put("reviewCount", config.entryTtl(Duration.ofMinutes(10)));
        configurations.put("playlistDetail", config.entryTtl(Duration.ofMinutes(30)));

        return RedisCacheManager.builder(connectionFactory)
                                .cacheDefaults(config)
                                .withInitialCacheConfigurations(configurations)
                                .build();
    }
}
