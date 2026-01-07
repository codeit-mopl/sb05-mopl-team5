package com.mopl.api.domain.follow.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record FollowRequest(@NotNull UUID followeeId) {}
