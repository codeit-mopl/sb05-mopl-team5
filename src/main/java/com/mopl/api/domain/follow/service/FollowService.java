package com.mopl.api.domain.follow.service;

import com.mopl.api.domain.follow.dto.request.FollowRequest;
import com.mopl.api.domain.follow.dto.response.FollowDto;
import com.mopl.api.domain.follow.dto.response.FollowResponseByMeDto;
import com.mopl.api.domain.follow.dto.response.FollowResponseCountDto;
import java.util.UUID;

public interface FollowService {

  FollowDto setFollow(FollowRequest request);

  FollowResponseByMeDto checkFollow(UUID followeeId);

  FollowResponseCountDto countFollow(UUID followeeId);

  void cancelFollow(UUID followeeId);

}
