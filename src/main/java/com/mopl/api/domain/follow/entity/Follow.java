package com.mopl.api.domain.follow.entity;

import com.mopl.api.domain.user.entity.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "follows",
    uniqueConstraints = {
        @UniqueConstraint(name = "UK_FOLLOWS_FOLLOWER_FOLLOWEE", columnNames = {"follower_id", "followee_id"})
    },
    indexes = {
        @Index(name = "IDX_FOLLOWS_FOLLOWER_FOLLOWEE", columnList = "follower_id, followee_id"),
        @Index(name = "IDX_FOLLOWS_FOLLOWEE", columnList = "followee_id")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Follow {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "follower_id", nullable = false, columnDefinition = "BINARY(16)")
    private User follower;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "followee_id", nullable = false, columnDefinition = "BINARY(16)")
    private User followee;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Follow(User follower, User followee) {
        if (follower == null || followee == null) {
            throw new IllegalArgumentException("follower/followee는 필수입니다.");
        }
        if (follower.getId().equals(followee.getId())) {
            throw new IllegalArgumentException("자기 자신은 팔로우할 수 없습니다.");
        }
        this.follower = follower;
        this.followee = followee;
    }
}
