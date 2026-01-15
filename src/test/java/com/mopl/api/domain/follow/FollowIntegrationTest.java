package com.mopl.api.domain.follow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mopl.api.domain.follow.dto.response.FollowDto;
import com.mopl.api.domain.follow.service.FollowService;
import com.mopl.api.domain.notification.service.NotificationService;
import com.mopl.api.domain.user.entity.User;
import com.mopl.api.domain.user.entity.UserRole;
import com.mopl.api.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.lang.reflect.Field;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FollowIntegrationTest {

    @Autowired
    FollowService followService;
    @Autowired
    UserRepository userRepository;
    @MockitoBean
    NotificationService notificationService;
    @Autowired
    EntityManager em;

    /**
     * ✅ 스키마 제약조건(특히 users.password NOT NULL)을 100% 만족하는 테스트 유저 생성 - 프로젝트마다 User 생성/세터가 다를 수 있어서, "세터가 있으면 세터", 없으면
     * "reflection"으로 필수 필드를 채움. - 저장 직후 flush로 제약 위반을 즉시 터뜨려 원인 추적이 쉬움.
     */
    private User saveUser(String prefix) {
        User user;

        // 1) 가장 흔한 기본 생성자 시도
        try {
            user = User.class.getDeclaredConstructor()
                             .newInstance();
        } catch (Exception e) {
            // 2) 기본 생성자가 없으면 네가 쓰던 생성자 사용 (email, name, role)
            user = new User(
                UUID.randomUUID() + "@test.com",
                "123456789",
                prefix + "-" + UUID.randomUUID()

            );
        }

        // 필수 값들
        String email = UUID.randomUUID() + "@test.com";
        String name = prefix + "-" + UUID.randomUUID();
        String password = "test-password!123"; // ✅ NOT NULL 충족용

        // 가능하면 세터로, 없으면 reflection으로 세팅
        setFieldIfPossible(user, "email", email);
        setFieldIfPossible(user, "name", name);
        setFieldIfPossible(user, "password", password);
        setFieldIfPossible(user, "role", UserRole.USER);
        setFieldIfPossible(user, "locked", Boolean.FALSE);

        User saved = userRepository.save(user);

        // ✅ 여기서 users insert를 강제로 DB에 반영 → NOT NULL/UNIQUE 위반 즉시 발견
        em.flush();

        // 안전장치: ID는 반드시 있어야 함
        assertThat(saved.getId()).as("User ID must be generated/present")
                                 .isNotNull();

        return saved;
    }

    private void setFieldIfPossible(Object target, String fieldName, Object value) {
        // 1) setter가 존재하면 우선 사용
        String setter = "set" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        try {
            // value가 enum/Boolean/String 등일 수 있으니 모든 메서드를 훑어서 이름 매칭
            for (var m : target.getClass()
                               .getMethods()) {
                if (m.getName()
                     .equals(setter) && m.getParameterCount() == 1) {
                    m.invoke(target, value);
                    return;
                }
            }
        } catch (Exception ignored) {
        }

        // 2) setter가 없으면 reflection으로 필드 직접 주입
        try {
            Field f = target.getClass()
                            .getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception ignored) {
            // 필드명이 다를 수 있음 (e.g. profileImageUrl 등)
            // 여기서는 "없으면 넘어가되", password 세팅이 안 되면 flush에서 바로 터질 것.
        }
    }

    @Test
    @DisplayName("팔로우 생성 성공")
    void createFollow_success() {
        User follower = saveUser("follower");
        User followee = saveUser("followee");

        FollowDto dto = followService.createFollow(follower.getId(), followee.getId());

        assertThat(dto).isNotNull();
        assertThat(dto.id()).isNotNull();
        assertThat(dto.followerId()).isEqualTo(follower.getId());
        assertThat(dto.followeeId()).isEqualTo(followee.getId());
    }

    @Test
    @DisplayName("중복 팔로우 시 IllegalStateException 발생")
    void createFollow_duplicate_throwsException() {
        User follower = saveUser("follower");
        User followee = saveUser("followee");

        followService.createFollow(follower.getId(), followee.getId());

        assertThatThrownBy(() ->
            followService.createFollow(follower.getId(), followee.getId())
        ).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("내가 특정 유저를 팔로우 중인지 여부 조회")
    void isFollowedByMe() {
        User follower = saveUser("follower");
        User followee = saveUser("followee");

        assertThat(followService.isFollowedByMe(follower.getId(), followee.getId())).isFalse();

        followService.createFollow(follower.getId(), followee.getId());

        assertThat(followService.isFollowedByMe(follower.getId(), followee.getId())).isTrue();
    }

    @Test
    @DisplayName("팔로워 수 조회")
    void getFollowerCount() {
        User followee = saveUser("followee");
        User follower1 = saveUser("f1");
        User follower2 = saveUser("f2");

        followService.createFollow(follower1.getId(), followee.getId());
        followService.createFollow(follower2.getId(), followee.getId());

        assertThat(followService.getFollowerCount(followee.getId())).isEqualTo(2);
    }

    @Test
    @DisplayName("팔로우 취소 성공")
    void cancelFollow_success() {
        User follower = saveUser("follower");
        User followee = saveUser("followee");

        FollowDto dto = followService.createFollow(follower.getId(), followee.getId());

        followService.cancelFollow(follower.getId(), dto.id());

        assertThat(followService.isFollowedByMe(follower.getId(), followee.getId())).isFalse();
    }

    @Test
    @DisplayName("본인이 아닌 경우 팔로우 취소 실패")
    void cancelFollow_notOwner_throwsException() {
        User follower = saveUser("follower");
        User followee = saveUser("followee");
        User other = saveUser("other");

        FollowDto dto = followService.createFollow(follower.getId(), followee.getId());

        assertThatThrownBy(() ->
            followService.cancelFollow(other.getId(), dto.id())
        ).isInstanceOf(IllegalStateException.class);
    }
}
