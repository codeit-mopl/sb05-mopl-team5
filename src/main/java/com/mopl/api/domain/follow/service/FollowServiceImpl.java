package com.mopl.api.domain.follow.service;

import com.mopl.api.domain.follow.dto.response.FollowDto;
import com.mopl.api.domain.follow.entity.Follow;
import com.mopl.api.domain.follow.repository.FollowRepository;
import com.mopl.api.domain.user.entity.User;
import com.mopl.api.domain.user.repository.UserRepository;
import com.mopl.api.global.config.security.CustomUserDetails;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class FollowServiceImpl implements FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;


    private UUID currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || auth.getPrincipal() == null) {
            throw new IllegalStateException("인증 정보가 없습니다.");
        }

        Object principal = auth.getPrincipal();

        if (principal instanceof CustomUserDetails cud && cud.getUserDto() != null) {
            return cud.getUserDto().id();
        }

        if (principal instanceof UUID uuid) {
            return uuid;
        }

        String name = auth.getName();
        try {
            return UUID.fromString(name);
        } catch (Exception e) {

        }

        throw  new IllegalStateException("현재 사용자 ID를 확인할수 없습ㄴ디ㅏ.");
    }


    @Override
    public FollowDto createFollow(UUID followerId, UUID followeeId) {
        if (followerId == null || followeeId == null) {
            throw new IllegalArgumentException("followerId/followeeId는 필수입니다.");
        }
        if (followerId.equals(followeeId)) {
            throw new IllegalArgumentException("자기 자신은 팔로우할 수 없습니다.");
        }

        if (followRepository.existsByFollower_IdAndFollowee_Id(followerId, followeeId)) {
            throw new IllegalStateException("이미 팔로우한 사용자입니다.");
        }

        User follower = userRepository.findById(followerId)
                                      .orElseThrow(() -> new IllegalArgumentException("follower 사용자가 존재하지 않습니다."));
        User followee = userRepository.findById(followeeId)
                                      .orElseThrow(() -> new IllegalArgumentException("followee 사용자가 존재하지 않습니다."));


        Follow saved = followRepository.save(new Follow(follower, followee));

        return FollowDto.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isFollowedByMe(UUID followerId, UUID followeeId) {
        return followRepository.existsByFollower_IdAndFollowee_Id(followerId, followeeId);
    }

    @Override
    @Transactional(readOnly = true)
    public long getFollowerCount(UUID followeeId) {
        return followRepository.countByFollowee_Id(followeeId);
    }

    @Override
    public void cancelFollow(UUID followerId, UUID followId) {
        Follow follow = followRepository.findById(followId)
                                        .orElseThrow(() -> new IllegalArgumentException("팔로우가 존재하지 않습니다."));

        if (!follow.getFollower().getId().equals(followerId)) {
            throw new IllegalStateException("본인의 팔로우만 취소할 수 있습니다.");
        }

        followRepository.delete(follow);
    }
}
