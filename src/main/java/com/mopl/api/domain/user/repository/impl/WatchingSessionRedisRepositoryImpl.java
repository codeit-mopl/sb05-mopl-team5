package com.mopl.api.domain.user.repository.impl;

import com.mopl.api.domain.user.repository.WatchingSessionRedisRepository;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class WatchingSessionRedisRepositoryImpl implements WatchingSessionRedisRepository {

    private static final String KEY_PREFIX = "watching:content:";
    private static final String SYNC_SET_KEY = "watching:sync:set";

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void addWatcher(UUID contentId, UUID userId) {
        redisTemplate.opsForSet()
                     .add(key(contentId), userId.toString());

        addChangedContentId(contentId);
    }

    @Override
    public void removeWatcher(UUID contentId, UUID userId) {
        redisTemplate.opsForSet()
                     .remove(key(contentId), userId.toString());

        addChangedContentId(contentId);
    }

    @Override
    public long countWatchers(UUID contentId) {
        Long size = redisTemplate.opsForSet()
                                 .size(key(contentId));
        return size != null ? size : 0L;
    }

    @Override
    public boolean isWatching(UUID contentId, UUID userId) {
        Boolean isMember =
            redisTemplate.opsForSet()
                         .isMember(key(contentId), userId.toString());

        return Boolean.TRUE.equals(isMember);
    }

    @Override
    public void addChangedContentId(UUID contentId) {
        redisTemplate.opsForSet()
                     .add(SYNC_SET_KEY, contentId.toString());
    }

    @Override
    public Set<String> popAllChangedIds() {
        String tempKey = SYNC_SET_KEY + ":" + UUID.randomUUID();

        Boolean hasKey = redisTemplate.hasKey(SYNC_SET_KEY);
        if (!hasKey) {
            return Collections.emptySet();
        }

        redisTemplate.rename(SYNC_SET_KEY, tempKey);

        Set<Object> members = redisTemplate.opsForSet()
                                           .members(tempKey);
        redisTemplate.delete(tempKey);

        if (members == null || members.isEmpty()) {
            return Collections.emptySet();
        }

        return members.stream()
                      .map(String::valueOf)
                      .collect(Collectors.toSet());
    }

    private String key(UUID contentId) {
        return KEY_PREFIX + contentId;
    }
}