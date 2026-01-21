package com.mopl.api.domain.notification.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.mopl.api.domain.notification.dto.request.NotificationCursorPageRequest;
import com.mopl.api.domain.notification.dto.request.NotificationCursorPageRequest.SortBy;
import com.mopl.api.domain.notification.dto.request.NotificationCursorPageRequest.SortDirection;
import com.mopl.api.domain.notification.entity.Notification;
import com.mopl.api.domain.notification.entity.NotificationLevel;
import com.mopl.api.domain.notification.repository.impl.NotificationRepositoryCustomImpl;
import com.mopl.api.domain.user.entity.User;
import com.mopl.api.global.config.QuerydslConfig;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Slice;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest
@Import({QuerydslConfig.class, NotificationRepositoryCustomImpl.class})
class NotificationRepositoryTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @PersistenceContext
    private EntityManager em;

    @Test
    @DisplayName("커서 기반 페이지네이션 조회 확인")
    void findAllByReceiverId_CheckCursor() {
        // given
        User user = new User("test@email.com", "password", "tester");
        ReflectionTestUtils.setField(user, "createdAt", LocalDateTime.now());
        em.persist(user);

        for (int i = 1; i <= 5; i++) {
            Notification notification = new Notification(user, "Title " + i, "Content", NotificationLevel.INFO);
            ReflectionTestUtils.setField(notification, "createdAt", LocalDateTime.now());
            em.persist(notification);
        }
        em.flush();

        NotificationCursorPageRequest request = NotificationCursorPageRequest.builder()
                                                                             .limit(3)
                                                                             .sortDirection(SortDirection.DESCENDING)
                                                                             .sortBy(SortBy.createdAt)
                                                                             .build();

        // when
        Slice<Notification> result = notificationRepository.findAllByReceiverId(user.getId(), request);

        // then
        assertThat(result.getContent()).hasSize(3);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.getContent()
                         .get(0)
                         .getTitle()).isEqualTo("Title 5");
    }
}