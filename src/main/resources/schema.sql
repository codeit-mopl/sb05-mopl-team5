-- ==========================================
-- 0. Drop Tables (Clean Up)
-- ==========================================
DROP TABLE IF EXISTS `jwt_sessions`;
DROP TABLE IF EXISTS `direct_messages`;
DROP TABLE IF EXISTS `conversation_participants`;
DROP TABLE IF EXISTS `conversations`;
DROP TABLE IF EXISTS `follows`;
DROP TABLE IF EXISTS `subscriptions`;
DROP TABLE IF EXISTS `watching_sessions`;
DROP TABLE IF EXISTS `playlist_contents`;
DROP TABLE IF EXISTS `playlists`;
DROP TABLE IF EXISTS `social_accounts`;
DROP TABLE IF EXISTS `notifications`;
DROP TABLE IF EXISTS `reviews`;
DROP TABLE IF EXISTS `contents`;
DROP TABLE IF EXISTS `users`;

-- ==========================================
-- 1. Users Table
-- ==========================================
CREATE TABLE `users`
(
    `id`                BINARY(16)      NOT NULL COMMENT 'PK',
    `name`              VARCHAR(100) NOT NULL,
    `email`             VARCHAR(255) NOT NULL COMMENT 'UNIQUE',
    `password`          VARCHAR(100) NOT NULL,
    `profile_image_url` VARCHAR(500) NULL,
    `role`              VARCHAR(20)  NOT NULL DEFAULT 'USER' COMMENT 'USER, ADMIN',
    `locked`            BOOLEAN      NOT NULL DEFAULT FALSE,
    `follower_count`    BIGINT       NOT NULL DEFAULT 0,
    `created_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT `PK_USERS` PRIMARY KEY (`id`)
);

-- ==========================================
-- 2. Contents Table
-- ==========================================
CREATE TABLE `contents`
(
    `id`             BINARY(16)     NOT NULL COMMENT 'PK',
    `type`           VARCHAR(20)   NOT NULL COMMENT 'movie, tvSeries, sport',
    `api_id`         BIGINT NULL     COMMENT 'UNIQUE(api_id, type)',
    `title`          VARCHAR(255)  NOT NULL,
    `description`    VARCHAR(1000) NOT NULL,
    `thumbnail_url`  VARCHAR(255)  NOT NULL,
    `tags`           VARCHAR(500)  NOT NULL,
    `average_rating` DECIMAL(2, 1) NOT NULL DEFAULT 0.0,
    `review_count`   BIGINT        NOT NULL DEFAULT 0,
    `watcher_count`  BIGINT        NOT NULL DEFAULT 0,
    `created_at`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted`     BOOLEAN       NOT NULL DEFAULT FALSE,

    CONSTRAINT `PK_CONTENTS` PRIMARY KEY (`id`),
    CONSTRAINT `UK_CONTENTS_TYPE_API_ID` UNIQUE (`type`, `api_id`)
);

-- ==========================================
-- 3. Reviews Table
-- ==========================================
CREATE TABLE `reviews`
(
    `id`         BINARY(16)     NOT NULL COMMENT 'PK',
    `content_id` BINARY(16)     NOT NULL COMMENT 'FK',
    `user_id`    BINARY(16)     NOT NULL COMMENT 'FK',
    `text`       VARCHAR(1000) NOT NULL,
    `rating`     DECIMAL(2, 1) NOT NULL COMMENT '0.0 ~ 5.0',
    `created_at` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted` BOOLEAN       NOT NULL DEFAULT FALSE,

    CONSTRAINT `PK_REVIEWS` PRIMARY KEY (`id`)
);

-- ==========================================
-- 4. Notifications Table
-- ==========================================
CREATE TABLE `notifications`
(
    `id`          BINARY(16)    NOT NULL COMMENT 'PK',
    `receiver_id` BINARY(16)    NOT NULL COMMENT 'FK',
    `title`       VARCHAR(255) NOT NULL,
    `content`     VARCHAR(255) NOT NULL,
    `level`       VARCHAR(10)  NOT NULL COMMENT 'INFO, WARNING, ERROR',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT `PK_NOTIFICATIONS` PRIMARY KEY (`id`)
);

-- ==========================================
-- 5. Social Accounts Table
-- ==========================================
CREATE TABLE `social_accounts`
(
    `id`          BINARY(16)    NOT NULL COMMENT 'PK',
    `user_id`     BINARY(16)    NOT NULL COMMENT 'FK',
    `provider`    VARCHAR(20)  NOT NULL COMMENT 'LOCAL, GOOGLE, KAKAO',
    `provider_id` VARCHAR(500) NOT NULL,
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT `PK_SOCIAL_ACCOUNTS` PRIMARY KEY (`id`)
);

-- ==========================================
-- 6. Playlists Table
-- ==========================================
CREATE TABLE `playlists`
(
    `id`               BINARY(16)   NOT NULL COMMENT 'PK',
    `user_id`          BINARY(16)   NOT NULL COMMENT 'FK',
    `title`            VARCHAR(255)  NOT NULL,
    `description`      VARCHAR(1000) NOT NULL,
    `subscriber_count` BIGINT        NOT NULL DEFAULT 0,
    `created_at`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted`       BOOLEAN       NOT NULL DEFAULT FALSE,

    CONSTRAINT `PK_PLAYLISTS` PRIMARY KEY (`id`)
);

-- ==========================================
-- 7. Playlist Contents Table
-- ==========================================
CREATE TABLE `playlist_contents`
(
    `id`          BINARY(16)    NOT NULL COMMENT 'PK',
    `playlist_id` BINARY(16)    NOT NULL COMMENT 'FK',
    `content_id`  BINARY(16)    NOT NULL COMMENT 'FK',
    `created_at`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted`  BOOLEAN  NOT NULL DEFAULT FALSE,

    CONSTRAINT `PK_PLAYLIST_CONTENTS` PRIMARY KEY (`id`)
);

-- ==========================================
-- 8. Watching Sessions Table
-- ==========================================
CREATE TABLE `watching_sessions`
(
    `id`         BINARY(16)     NOT NULL COMMENT 'PK',
    `watcher_id` BINARY(16)     NOT NULL COMMENT 'FK',
    `content_id` BINARY(16)     NOT NULL COMMENT 'FK',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT `PK_WATCHING_SESSIONS` PRIMARY KEY (`id`)
);

-- ==========================================
-- 9. Subscriptions Table
-- ==========================================
CREATE TABLE `subscriptions`
(
    `id`          BINARY(16)    NOT NULL COMMENT 'PK',
    `user_id`     BINARY(16)    NOT NULL COMMENT 'FK',
    `playlist_id` BINARY(16)    NOT NULL COMMENT 'FK',
    `created_at`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT `PK_SUBSCRIPTIONS` PRIMARY KEY (`id`)
);

-- ==========================================
-- 10. Follows Table
-- ==========================================
CREATE TABLE `follows`
(
    `id`          BINARY(16)    NOT NULL COMMENT 'PK',
    `followee_id` BINARY(16)    NOT NULL COMMENT 'FK',
    `follower_id` BINARY(16)    NOT NULL COMMENT 'FK',
    `created_at`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT `PK_FOLLOWS` PRIMARY KEY (`id`)
);

-- ==========================================
-- 11. Conversations Table
-- ==========================================
CREATE TABLE `conversations`
(
    `id`         BINARY(16)     NOT NULL COMMENT 'PK',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- [추가] 목록 조회를 위한 역정규화 컬럼 (최신 메시지 캐싱)
    `last_message_content`    VARCHAR(2000) NULL,
    `last_message_created_at` DATETIME      NULL,

    CONSTRAINT `PK_CONVERSATIONS` PRIMARY KEY (`id`)
);

-- ==========================================
-- 12. Conversation Participants Table
-- ==========================================
CREATE TABLE `conversation_participants`
(
    `id`              BINARY(16) NOT NULL COMMENT 'PK',
    `conversation_id` BINARY(16) NOT NULL COMMENT 'FK',
    `user_id`         BINARY(16) NOT NULL COMMENT 'FK',
    `last_read_at`    DATETIME NULL,
    `created_at`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT `PK_CONVERSATION_PARTICIPANTS` PRIMARY KEY (`id`)
);

-- ==========================================
-- 13. Direct Messages Table
-- ==========================================
CREATE TABLE `direct_messages`
(
    `id`              BINARY(16) NOT NULL COMMENT 'PK',
    `conversation_id` BINARY(16) NOT NULL COMMENT 'FK',
    `sender_id`       BINARY(16) NOT NULL COMMENT 'FK',
    `receiver_id`     BINARY(16) NOT NULL COMMENT 'FK',
    `content`         VARCHAR(1000) NULL,
    `created_at`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT `PK_DIRECT_MESSAGES` PRIMARY KEY (`id`)
);

-- ==========================================
-- 14. JWT Sessions Table
-- ==========================================
CREATE TABLE `jwt_sessions`
(
    `id`            BINARY(16)   NOT NULL COMMENT 'PK',
    `user_id`       BINARY(16)   NOT NULL COMMENT 'FK',
    `refresh_token` VARCHAR(500) NOT NULL,
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `expired_at`    DATETIME     NOT NULL,

    CONSTRAINT `PK_JWT_SESSIONS` PRIMARY KEY (`id`)
);

-- ==========================================
-- Foreign Key Constraints
-- ==========================================

ALTER TABLE `reviews`
    ADD CONSTRAINT `FK_REVIEWS_CONTENT` FOREIGN KEY (`content_id`) REFERENCES `contents` (`id`);
ALTER TABLE `reviews`
    ADD CONSTRAINT `FK_REVIEWS_USER` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`);
ALTER TABLE `notifications`
    ADD CONSTRAINT `FK_NOTIFICATIONS_USER` FOREIGN KEY (`receiver_id`) REFERENCES `users` (`id`);
ALTER TABLE `social_accounts`
    ADD CONSTRAINT `FK_SOCIAL_ACCOUNTS_USER` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`);
ALTER TABLE `playlists`
    ADD CONSTRAINT `FK_PLAYLISTS_USER` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`);
ALTER TABLE `playlist_contents`
    ADD CONSTRAINT `FK_PC_PLAYLIST` FOREIGN KEY (`playlist_id`) REFERENCES `playlists` (`id`);
ALTER TABLE `playlist_contents`
    ADD CONSTRAINT `FK_PC_CONTENT` FOREIGN KEY (`content_id`) REFERENCES `contents` (`id`);
ALTER TABLE `watching_sessions`
    ADD CONSTRAINT `FK_WS_USER` FOREIGN KEY (`watcher_id`) REFERENCES `users` (`id`);
ALTER TABLE `watching_sessions`
    ADD CONSTRAINT `FK_WS_CONTENT` FOREIGN KEY (`content_id`) REFERENCES `contents` (`id`);
ALTER TABLE `subscriptions`
    ADD CONSTRAINT `FK_SUB_USER` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`);
ALTER TABLE `subscriptions`
    ADD CONSTRAINT `FK_SUB_PLAYLIST` FOREIGN KEY (`playlist_id`) REFERENCES `playlists` (`id`);
ALTER TABLE `follows`
    ADD CONSTRAINT `FK_FOLLOWS_FOLLOWEE` FOREIGN KEY (`followee_id`) REFERENCES `users` (`id`);
ALTER TABLE `follows`
    ADD CONSTRAINT `FK_FOLLOWS_FOLLOWER` FOREIGN KEY (`follower_id`) REFERENCES `users` (`id`);
ALTER TABLE `conversation_participants`
    ADD CONSTRAINT `FK_PARTICIPANTS_CONV` FOREIGN KEY (`conversation_id`) REFERENCES `conversations` (`id`);
ALTER TABLE `conversation_participants`
    ADD CONSTRAINT `FK_PARTICIPANTS_USER` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`);
ALTER TABLE `direct_messages`
    ADD CONSTRAINT `FK_DM_CONV` FOREIGN KEY (`conversation_id`) REFERENCES `conversations` (`id`);
ALTER TABLE `direct_messages`
    ADD CONSTRAINT `FK_DM_SENDER` FOREIGN KEY (`sender_id`) REFERENCES `users` (`id`);
ALTER TABLE `direct_messages`
    ADD CONSTRAINT `FK_DM_RECEIVER` FOREIGN KEY (`receiver_id`) REFERENCES `users` (`id`);
ALTER TABLE `jwt_sessions`
    ADD CONSTRAINT `FK_JWT_USER` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`);