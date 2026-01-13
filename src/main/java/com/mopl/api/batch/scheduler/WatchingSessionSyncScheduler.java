package com.mopl.api.batch.scheduler;

import com.mopl.api.domain.content.repository.impl.ContentBatchRepository;
import com.mopl.api.domain.user.repository.WatchingSessionRedisRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class WatchingSessionSyncScheduler {
    private final WatchingSessionRedisRepository redisRepository;
    private final ContentBatchRepository contentBatchRepository;

    @Scheduled(fixedDelay = 3000)
    public void syncToDb() {
        Set<String> changedIds = redisRepository.popAllChangedIds();
        if (changedIds.isEmpty()) return;

        Map<UUID, Long> updateMap = new HashMap<>();
        for (String idStr : changedIds) {
            UUID contentId = UUID.fromString(idStr);
            long count = redisRepository.countWatchers(contentId);
            updateMap.put(contentId, count);
        }

        try {
            contentBatchRepository.batchUpdateWatcherCounts(updateMap);
            log.debug("3초 주기 동기화 완료: {}건", updateMap.size());
        } catch (Exception e) {
            log.error("DB 동기화 실패. 다시 Redis에 보관", e);
            for (UUID id : updateMap.keySet()) {
                redisRepository.addChangedContentId(id);
            }
        }
    }
}