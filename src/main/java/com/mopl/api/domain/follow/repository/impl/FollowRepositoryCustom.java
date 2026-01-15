package com.mopl.api.domain.follow.repository.impl;

import com.mopl.api.domain.follow.entity.Follow;
import io.lettuce.core.dynamic.annotation.Param;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;

public interface FollowRepositoryCustom {

    @Query("select f from Follow f join fetch f.follower join fetch f.followee where f.followee.id = :followeeId")
    List<Follow> findFollowsByFolloweeId(@Param("followeeId") UUID followeeId);
}
