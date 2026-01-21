package com.mopl.api.batch.scheduler;

import com.mopl.api.domain.notification.repository.NotificationRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationCleanupScheduler {

    private final NotificationRepository notificationRepository;

    @Transactional
    @Scheduled(cron = "${batch.schedule.notification}")
    public void cleanupOldNotifications() {
        LocalDateTime targetDate = LocalDateTime.now()
                                                .minusDays(100);

        long deletedCount = notificationRepository.deleteOldNotifications(targetDate);

        log.info("기준 100일 경과 알림 {}건 삭제 완료", deletedCount);
    }
}