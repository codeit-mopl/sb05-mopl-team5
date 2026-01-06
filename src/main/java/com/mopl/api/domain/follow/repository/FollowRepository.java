package com.mopl.api.domain.follow.repository;

import com.mopl.api.domain.follow.entity.Follow;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FollowRepository extends JpaRepository<Follow, UUID> {
    boolean existsByFollower_IdAndFollowee_Id(UUID followerId, UUID followeeId);
    long countByFollowee_Id(UUID followeeId);
}
