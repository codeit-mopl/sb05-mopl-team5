package com.mopl.api.domain.user.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.mopl.api.domain.content.entity.Content;
import com.mopl.api.domain.content.entity.ContentType;
import com.mopl.api.domain.content.repository.ContentRepository;
import com.mopl.api.domain.user.dto.request.WatchingSessionSearchRequest;
import com.mopl.api.domain.user.dto.request.WatchingSessionSearchRequest.SortBy;
import com.mopl.api.domain.user.dto.request.WatchingSessionSearchRequest.SortDirection;
import com.mopl.api.domain.user.entity.User;
import com.mopl.api.domain.user.entity.WatchingSession;
import com.mopl.api.domain.user.repository.impl.WatchingSessionRepositoryCustomImpl;
import com.mopl.api.global.config.QuerydslConfig;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest
@Import({QuerydslConfig.class, WatchingSessionRepositoryCustomImpl.class})
class WatchingSessionRepositoryTest {

    @Autowired
    private WatchingSessionRepository watchingSessionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ContentRepository contentRepository;

    @PersistenceContext
    private EntityManager em;

    private Content content;

    @BeforeEach
    void setUp() {
        content = new Content(ContentType.MOVIE, 1L, "test content", "td",
            "", "", new BigDecimal("0.0"), 0L, 0L);
        contentRepository.save(content);
        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("생성일 내림차순(DESC) 커서 페이징이 정상 동작한다")
    void searchSessions_Desc_Cursor_Success() {

        LocalDateTime now = LocalDateTime.now();
        User user1 = createUser("유저1", "test1@test.com");
        User user2 = createUser("유저2", "test2@test.com");
        User user3 = createUser("유저3", "test3@test.com");

        saveSession(user1, now.minusMinutes(10));
        saveSession(user2, now.minusMinutes(5));
        WatchingSession session3 = saveSession(user3, now);

        WatchingSessionSearchRequest firstRequest = WatchingSessionSearchRequest.builder()
                                                                                .limit(1)
                                                                                .sortDirection(SortDirection.DESCENDING)
                                                                                .sortBy(SortBy.createdAt)
                                                                                .build();

        List<WatchingSession> firstPage = watchingSessionRepository.searchSessions(content.getId(), firstRequest);

        assertThat(firstPage).hasSize(2);
        assertThat(firstPage.get(0)
                            .getWatcher()
                            .getName()).isEqualTo("유저3");

        WatchingSessionSearchRequest secondRequest = WatchingSessionSearchRequest.builder()
                                                                                 .cursor(session3.getCreatedAt()
                                                                                                 .format(
                                                                                                     DateTimeFormatter.ISO_DATE_TIME))
                                                                                 .idAfter(session3.getId()
                                                                                                  .toString())
                                                                                 .limit(1)
                                                                                 .sortDirection(
                                                                                     SortDirection.DESCENDING)
                                                                                 .sortBy(SortBy.createdAt)
                                                                                 .build();

        List<WatchingSession> secondPage = watchingSessionRepository.searchSessions(content.getId(), secondRequest);

        assertThat(secondPage.get(0)
                             .getWatcher()
                             .getName()).isEqualTo("유저2");
    }

    @Test
    @DisplayName("생성일이 같을 경우 ID를 비교하여 다음 데이터를 가져온다")
    void searchSessions_TieBreaker_Success() {

        LocalDateTime sameTime = LocalDateTime.now().withNano(0);
        User user1 = createUser("A 유저", "auser@test.com");
        User user2 = createUser("B 유저", "buser@test.com");

        WatchingSession s1 = saveSession(user1, sameTime);
        WatchingSession s2 = saveSession(user2, sameTime);

        WatchingSession firstInList = s1.getId()
                                        .compareTo(s2.getId()) < 0 ? s1 : s2;
        WatchingSession secondInList = s1.getId()
                                         .compareTo(s2.getId()) < 0 ? s2 : s1;

        WatchingSessionSearchRequest request = WatchingSessionSearchRequest.builder()
                                                                           .cursor(firstInList.getCreatedAt()
                                                                                              .format(
                                                                                                  DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                                                                           .idAfter(firstInList.getId()
                                                                                               .toString())
                                                                           .limit(1)
                                                                           .sortDirection(SortDirection.ASCENDING)
                                                                           .sortBy(SortBy.createdAt)
                                                                           .build();

        List<WatchingSession> result = watchingSessionRepository.searchSessions(content.getId(), request);

        assertThat(result.get(0)
                         .getId()).isEqualTo(secondInList.getId());
    }

    private User createUser(String name, String email) {
        User user = new User(email, "password123!", name);
        return userRepository.save(user);
    }

    private WatchingSession saveSession(User user, LocalDateTime createdAt) {
        WatchingSession session = new WatchingSession(user, content);
        watchingSessionRepository.save(session);

        ReflectionTestUtils.setField(session, "createdAt", createdAt);

        em.flush();
        em.clear();

        return watchingSessionRepository.findById(session.getId()).get();
    }
}