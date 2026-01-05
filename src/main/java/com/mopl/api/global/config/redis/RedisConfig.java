package com.mopl.api.global.config.redis;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@EnableCaching
public class RedisConfig {

    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        return template;
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                                                                .entryTtl(
                                                                    Duration.ofMinutes(60)) // 기본 캐시 유지 시간 임시 60분
                                                                .disableCachingNullValues() // null 값은 캐싱하지 않음
                                                                .serializeKeysWith(
                                                                    RedisSerializationContext.SerializationPair.fromSerializer(
                                                                        new StringRedisSerializer())
                                                                )
                                                                .serializeValuesWith(
                                                                    RedisSerializationContext.SerializationPair.fromSerializer(
                                                                        new GenericJackson2JsonRedisSerializer())
                                                                );

        // 필요시 특정 캐시 이름에 대해서만 다른 설정 적용
        Map<String, RedisCacheConfiguration> configurations = new HashMap<>();
        // temp가 value인 경우 2시간
        // @Cacheable(value = "temp", key = "#tempId", cacheManager = "cacheManager")
        configurations.put("temp", config.entryTtl(Duration.ofHours(2)));

        return RedisCacheManager.builder(connectionFactory)
                                .cacheDefaults(config)
                                .withInitialCacheConfigurations(configurations)
                                .build();
    }
}