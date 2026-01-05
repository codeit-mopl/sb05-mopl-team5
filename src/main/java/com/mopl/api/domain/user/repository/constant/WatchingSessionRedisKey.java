package com.mopl.api.domain.user.repository.constant;

import java.util.UUID;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class WatchingSessionRedisKey {

    public static String contentKey(UUID contentId) {
        return "watching-session:content:" + contentId;
    }
}
