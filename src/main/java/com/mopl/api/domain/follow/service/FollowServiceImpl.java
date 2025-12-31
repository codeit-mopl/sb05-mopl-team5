package com.mopl.api.domain.follow.service;

import com.mopl.api.domain.follow.dto.request.FollowRequest;
import com.mopl.api.domain.follow.dto.response.FollowDto;
import com.mopl.api.domain.follow.dto.response.FollowResponseByMeDto;
import com.mopl.api.domain.follow.dto.response.FollowResponseCountDto;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class FollowServiceImpl implements FollowService {

    @Override
    public FollowDto addFollow(FollowRequest request) {
        return null;
    }

    @Override
    public FollowResponseByMeDto checkFollow(UUID followeeId) {
        return null;
    }

    @Override
    public FollowResponseCountDto countFollow(UUID followeeId) {
        return null;
    }

    @Override
    public void followRemove(UUID followeeId) {

    }
}