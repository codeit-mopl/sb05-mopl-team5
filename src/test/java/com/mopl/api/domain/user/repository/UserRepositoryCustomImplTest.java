package com.mopl.api.domain.user.repository;

import com.mopl.api.domain.user.dto.request.CursorRequestUserDto;
import com.mopl.api.domain.user.dto.response.CursorResponseUserDto;
import com.mopl.api.domain.user.dto.response.UserDto;
import com.mopl.api.domain.user.entity.User;
import com.mopl.api.domain.user.entity.UserRole;
import com.mopl.api.domain.user.repository.impl.UserRepositoryCustomImpl;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest
@ActiveProfiles("test")
@Import(UserRepositoryCustomImplTest.TestQuerydslConfig.class)
class UserRepositoryCustomImplTest {

    @PersistenceContext
    private EntityManager em;

    private UserRepositoryCustomImpl userRepositoryCustom;

    @BeforeEach
    void setUp() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        userRepositoryCustom = new UserRepositoryCustomImpl(queryFactory);
    }

    @TestConfiguration
    @EnableJpaAuditing
    static class TestQuerydslConfig {
        @Bean
        JPAQueryFactory jpaQueryFactory(EntityManager em) {
            return new JPAQueryFactory(em);
        }
    }

    @Test
    @DisplayName("기본값: sortBy/sortDirection이 null이면 createdAt + ASCENDING, hasNext/nextCursor 계산")
    void default_sortBy_sortDirection_null() {
        User u1 = persistUser("a@mopl.com", "A", UserRole.USER, false, LocalDateTime.now().minusMinutes(3));
        User u2 = persistUser("b@mopl.com", "B", UserRole.USER, false, LocalDateTime.now().minusMinutes(2));
        User u3 = persistUser("c@mopl.com", "C", UserRole.USER, false, LocalDateTime.now().minusMinutes(1));

        em.flush();
        em.clear();

        CursorRequestUserDto req = CursorRequestUserDto.builder()
                                                       .limit(2)
                                                       .sortBy(null)
                                                       .sortDirection(null)
                                                       .build();

        // when
        CursorResponseUserDto<UserDto> res = userRepositoryCustom.findAllUsers(req);

        // then
        assertThat(res.data()).hasSize(2);
        assertThat(res.hasNext()).isTrue();
        assertThat(res.totalCount()).isEqualTo(3L);
        assertThat(res.sortBy()).isEqualTo("createdAt");
        assertThat(res.sortDirection()).isEqualTo("ASCENDING");

        assertThat(res.nextCursor()).isNotBlank();
        assertThat(res.nextIdAfter()).isNotNull();
    }

    @Test
    @DisplayName("커서 페이징: 1페이지 nextCursor/nextIdAfter로 2페이지 조회가 이어진다")
    void cursor_paging_nextCursor_nextIdAfter() {
        // given
        persistUser("a@mopl.com", "A", UserRole.USER, false, LocalDateTime.now().minusMinutes(3));
        persistUser("b@mopl.com", "B", UserRole.USER, false, LocalDateTime.now().minusMinutes(2));
        persistUser("c@mopl.com", "C", UserRole.USER, false, LocalDateTime.now().minusMinutes(1));

        em.flush();
        em.clear();

        CursorRequestUserDto firstReq = CursorRequestUserDto.builder()
                                                            .limit(1)
                                                            .sortBy("email")
                                                            .sortDirection("ASCENDING")
                                                            .build();

        CursorResponseUserDto<UserDto> firstPage = userRepositoryCustom.findAllUsers(firstReq);

        // when (2페이지)
        CursorRequestUserDto secondReq = CursorRequestUserDto.builder()
                                                             .limit(1)
                                                             .sortBy("email")
                                                             .sortDirection("ASCENDING")
                                                             .cursor(firstPage.nextCursor())
                                                             .idAfter(firstPage.nextIdAfter())
                                                             .build();

        CursorResponseUserDto<UserDto> secondPage = userRepositoryCustom.findAllUsers(secondReq);

        // then
        assertThat(firstPage.data()).hasSize(1);
        assertThat(secondPage.data()).hasSize(1);

        String firstEmail = firstPage.data().get(0).email();
        String secondEmail = secondPage.data().get(0).email();

        assertThat(secondEmail).isGreaterThan(firstEmail);
    }

    private User persistUser(String email, String name, UserRole role, boolean locked, LocalDateTime createdAt) {
        User user = new User(email, "pw123!", name);
        user.updateUserRole(role);
        user.updateUserLock(locked);

        em.persist(user);
        em.flush();

        try {
            ReflectionTestUtils.setField(user, "createdAt", createdAt);
        } catch (Exception ignored) {
        }

        return user;
    }
}
