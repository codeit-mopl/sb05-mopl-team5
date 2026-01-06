package com.mopl.api.domain.follow.service;

import com.mopl.api.domain.follow.dto.response.FollowDto;
import java.util.UUID;

public interface FollowService {
    FollowDto createFollow(UUID followerId, UUID followeeId);
    boolean isFollowedByMe(UUID followerId, UUID followeeId);
    long getFollowerCount(UUID followeeId);
    void cancelFollow(UUID followerId, UUID followId);
}
