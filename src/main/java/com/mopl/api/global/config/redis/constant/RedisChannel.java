package com.mopl.api.global.config.redis.constant;

import java.util.UUID;

public enum RedisChannel {

    WATCHING_SESSION("watching-session"),
    CONTENT_CHAT("content-chat");

    private final String prefix;

    RedisChannel(String prefix) {
        this.prefix = prefix;
    }

    public String channel(UUID contentId) {
        return prefix + ":" + contentId;
    }

    public String websocketDestination(UUID contentId) {
        return "/sub/contents/" + contentId + "/" + destinationSuffix();
    }

    private String destinationSuffix() {
        return switch (this) {
            case WATCHING_SESSION -> "watch";
            case CONTENT_CHAT -> "chat";
        };
    }
}