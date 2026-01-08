package com.mopl.api.domain.notification.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.mopl.api.domain.notification.dto.request.NotificationCursorPageRequest;
import com.mopl.api.domain.notification.dto.request.NotificationCursorPageRequest.SortBy;
import com.mopl.api.domain.notification.dto.request.NotificationCursorPageRequest.SortDirection;
import com.mopl.api.domain.notification.entity.Notification;
import com.mopl.api.domain.notification.entity.NotificationLevel;
import com.mopl.api.domain.user.entity.User;
import com.mopl.api.global.config.QuerydslConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Slice;

@DataJpaTest
@Import(QuerydslConfig.class)
@DisplayName("NotificationRepository 테스트")
class NotificationRepositoryTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private TestEntityManager em;

    @Test
    @DisplayName("사용자 ID별 알림 목록 조회 (내림차순)")
    void findAllByReceiverId_Success() {
        // given
        User user = new User("test@test.com", "test123", "유저");
        em.persist(user);

        for (int i = 0; i < 3; i++) {
            notificationRepository.save(new Notification(user, "알림" + (i + 1), "내용", NotificationLevel.INFO));
        }

        NotificationCursorPageRequest request = NotificationCursorPageRequest.builder()
                                                                             .limit(2)
                                                                             .sortDirection(SortDirection.DESCENDING)
                                                                             .sortBy(SortBy.createdAt)
                                                                             .build();

        // when
        Slice<Notification> result = notificationRepository.findAllByReceiverId(user.getId(), request);

        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.getContent()
                         .get(0)
                         .getTitle()).isEqualTo("알림3"); // 최신순
    }

    @Test
    @DisplayName("사용자 ID별 알림 개수 조회")
    void countByReceiverId_Success() {
        // given
        User user1 = new User("test1@test.com", "test123", "유저1");
        User user2 = new User("test2@test.com", "test123", "유저2");
        em.persist(user1);
        em.persist(user2);

        for (int i = 0; i < 3; i++) {
            notificationRepository.save(new Notification(user1, "알림" + (i + 1), "내용", NotificationLevel.INFO));
        }

        for (int i = 0; i < 2; i++) {
            notificationRepository.save(new Notification(user2, "알림" + (i + 1), "내용", NotificationLevel.INFO));
        }

        // when
        long result1 = notificationRepository.countByReceiverId(user1.getId());
        long result2 = notificationRepository.countByReceiverId(user2.getId());

        // then
        assertThat(result1).isEqualTo(3L);
        assertThat(result2).isEqualTo(2L);
    }
}