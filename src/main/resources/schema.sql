-- 1. Users Table
CREATE TABLE `users` (
                         `id`                BINARY(16)      NOT NULL COMMENT 'PK',
                         `name`              VARCHAR(100)    NOT NULL,
                         `email`             VARCHAR(255)    NOT NULL COMMENT 'UNIQUE',
                         `password`          VARCHAR(100)    NOT NULL,
                         `profile_image_url` VARCHAR(500)    NULL,
                         `role`              VARCHAR(10)     NOT NULL DEFAULT 'USER' COMMENT 'USER, ADMIN',
                         `locked`            BOOLEAN         NOT NULL DEFAULT FALSE,
                         `follower_count`    BIGINT          NOT NULL DEFAULT 0,
                         `created_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         `updated_at`        DATETIME        NULL     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                         CONSTRAINT `PK_USERS` PRIMARY KEY (`id`),
                         CONSTRAINT `UK_USERS_EMAIL` UNIQUE (`email`),
                         CONSTRAINT `CHK_USERS_ROLE` CHECK (`role` IN ('USER', 'ADMIN'))
);

-- 2. Contents Table
CREATE TABLE `contents` (
                            `id`             BINARY(16)     NOT NULL COMMENT 'PK',
                            `type`           VARCHAR(20)    NOT NULL COMMENT 'movie, tvSeries, sport',
                            `title`          VARCHAR(255)   NOT NULL,
                            `description`    TEXT           NOT NULL,
                            `thumbnail_url`  VARCHAR(255)   NOT NULL,
                            `tags`           JSON           NOT NULL,
                            `average_rating` DECIMAL(2,1)   NOT NULL DEFAULT 0.0,
                            `review_count`   BIGINT         NOT NULL DEFAULT 0,
                            `watcher_count`  BIGINT         NOT NULL DEFAULT 0,
                            `created_at`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            `updated_at`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                            `is_deleted`     BOOLEAN        NOT NULL DEFAULT FALSE,

                            CONSTRAINT `PK_CONTENTS` PRIMARY KEY (`id`),
                            CONSTRAINT `CHK_CONTENTS_TYPE` CHECK (`type` IN ('movie', 'tvSeries', 'sport'))
);

-- 3. Reviews Table
CREATE TABLE `reviews` (
                           `id`         BINARY(16)   NOT NULL COMMENT 'PK',
                           `content_id` BINARY(16)   NOT NULL COMMENT 'FK',
                           `user_id`    BINARY(16)   NOT NULL COMMENT 'FK',
                           `text`       TEXT         NOT NULL,
                           `rating`     DECIMAL(2,1) NOT NULL COMMENT '(0.0 ~ 5.0)',
                           `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           `updated_at`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                           `is_deleted` BOOLEAN      NOT NULL DEFAULT FALSE,

                           CONSTRAINT `PK_REVIEWS` PRIMARY KEY (`id`),
                           CONSTRAINT `CHK_REVIEWS_RATING` CHECK (`rating` >= 0.0 AND `rating` <= 5.0)
);

-- 4. Notifications Table
CREATE TABLE `notifications` (
                                 `id`          BINARY(16)   NOT NULL COMMENT 'PK',
                                 `receiver_id` BINARY(16)   NOT NULL COMMENT 'FK',
                                 `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 `title`       VARCHAR(255) NOT NULL,
                                 `content`     VARCHAR(255) NOT NULL,
                                 `level`       VARCHAR(10)  NOT NULL COMMENT 'INFO, WARNING, ERROR',

                                 CONSTRAINT `PK_NOTIFICATIONS` PRIMARY KEY (`id`),
                                 CONSTRAINT `CHK_NOTIFICATIONS_LEVEL` CHECK (`level` IN ('INFO', 'WARNING', 'ERROR'))
);

-- 5. Social Accounts Table
CREATE TABLE `social_accounts` (
                                   `id`          BINARY(16)   NOT NULL COMMENT 'PK',
                                   `provider`    VARCHAR(20)  NOT NULL COMMENT 'LOCAL, GOOGLE, KAKAO',
                                   `provider_id` VARCHAR(500) NOT NULL,
                                   `user_id`     BINARY(16)   NOT NULL COMMENT 'FK',
                                   `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                   CONSTRAINT `PK_SOCIAL_ACCOUNTS` PRIMARY KEY (`id`),
                                   CONSTRAINT `CHK_SOCIAL_PROVIDER` CHECK (`provider` IN ('LOCAL', 'GOOGLE', 'KAKAO'))
);

-- 6. Playlists Table
CREATE TABLE `playlists` (
                             `id`               BINARY(16)   NOT NULL COMMENT 'PK',
                             `user_id`          BINARY(16)   NOT NULL COMMENT 'FK',
                             `title`            VARCHAR(255) NOT NULL,
                             `description`      TEXT         NOT NULL,
                             `subscriber_count` BIGINT       NOT NULL DEFAULT 0,
                             `created_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             `updated_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                             `is_deleted`       BOOLEAN      NOT NULL DEFAULT FALSE,

                             CONSTRAINT `PK_PLAYLISTS` PRIMARY KEY (`id`)
);

-- 7. Playlist Contents Table (Many-to-Many)
CREATE TABLE `playlist_contents` (
                                     `id`               BINARY(16)   NOT NULL COMMENT 'PK',
                                     `playlist_id` BINARY(16) NOT NULL COMMENT 'PK, FK',
                                     `content_id`  BINARY(16) NOT NULL COMMENT 'PK, FK',
                                     `created_at`  DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                     `updated_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                     `is_deleted`  BOOLEAN    NOT NULL DEFAULT FALSE,

                                     CONSTRAINT `PK_PLAYLIST_CONTENTS` PRIMARY KEY (`id`)
);

-- 8. Watching Sessions Table
CREATE TABLE `watching_sessions` (
                                     `id`               BINARY(16)   NOT NULL COMMENT 'PK',
                                     `watcher_id` BINARY(16) NOT NULL COMMENT 'PK, FK',
                                     `content_id` BINARY(16) NOT NULL COMMENT 'PK, FK',
                                     `created_at` DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                     CONSTRAINT `PK_WATCHING_SESSIONS` PRIMARY KEY (`id`)
);

-- 9. Subscriptions Table
CREATE TABLE `subscriptions` (
                                 `id`               BINARY(16)   NOT NULL COMMENT 'PK',
                                 `user_id`     BINARY(16) NOT NULL COMMENT 'PK, FK',
                                 `playlist_id` BINARY(16) NOT NULL COMMENT 'PK, FK',
                                 `created_at`  DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                 CONSTRAINT `PK_SUBSCRIPTIONS` PRIMARY KEY (`id`)
);

-- 10. Follows Table
CREATE TABLE `follows` (
                           `id`               BINARY(16)   NOT NULL COMMENT 'PK',
                           `followee_id` BINARY(16) NOT NULL COMMENT 'PK',
                           `follower_id` BINARY(16) NOT NULL COMMENT 'PK',
                           `created_at`  DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,

                           CONSTRAINT `PK_FOLLOWS` PRIMARY KEY (`id`)
);

-- 11. Conversations Table
CREATE TABLE `conversations` (
                                 `id`         BINARY(16) NOT NULL COMMENT 'PK',
                                 `created_at` DATETIME   NULL DEFAULT CURRENT_TIMESTAMP,

                                 CONSTRAINT `PK_CONVERSATIONS` PRIMARY KEY (`id`)
);

-- 12. Conversation Participants Table
CREATE TABLE `conversation_participants` (
                                             `id`              BINARY(16) NOT NULL COMMENT 'PK',
                                             `conversation_id` BINARY(16) NOT NULL COMMENT 'FK',
                                             `user_id`         BINARY(16) NOT NULL COMMENT 'FK',
                                             `last_read_at`    DATETIME   NULL,
                                             `created_at`      DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                             `updated_at`      DATETIME   NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                                             CONSTRAINT `PK_CONVERSATION_PARTICIPANTS` PRIMARY KEY (`id`)
);

-- 13. Direct Messages Table
CREATE TABLE `direct_messages` (
                                   `id`              BINARY(16) NOT NULL COMMENT 'PK',
                                   `conversation_id` BINARY(16) NOT NULL COMMENT 'FK',
                                   `sender_id`       BINARY(16) NOT NULL COMMENT 'FK',
                                   `receiver_id`     BINARY(16) NOT NULL COMMENT 'FK',
                                   `content`         TEXT       NULL,
                                   `created_at`      DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                   CONSTRAINT `PK_DIRECT_MESSAGES` PRIMARY KEY (`id`)
);

-- 14. JWT Sessions Table
CREATE TABLE `jwt_sessions` (
                                `id`            BINARY(16)   NOT NULL COMMENT 'PK',
                                `refresh_token` VARCHAR(500) NOT NULL,
                                `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                `expired_at`    DATETIME     NOT NULL,
                                `user_id`       BINARY(16)   NOT NULL COMMENT 'FK',

                                CONSTRAINT `PK_JWT_SESSIONS` PRIMARY KEY (`id`)
);


-- ==========================================
-- Foreign Key Constraints (Relationships)
-- ==========================================

-- Reviews FKs
ALTER TABLE `reviews` ADD CONSTRAINT `FK_reviews_content` FOREIGN KEY (`content_id`) REFERENCES `contents` (`id`);
ALTER TABLE `reviews` ADD CONSTRAINT `FK_reviews_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`);

-- Notifications FK
ALTER TABLE `notifications` ADD CONSTRAINT `FK_notifications_receiver` FOREIGN KEY (`receiver_id`) REFERENCES `users` (`id`);

-- Social Accounts FK
ALTER TABLE `social_accounts` ADD CONSTRAINT `FK_social_accounts_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`);

-- Playlists FK
ALTER TABLE `playlists` ADD CONSTRAINT `FK_playlists_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`);

-- Playlist Contents FKs
ALTER TABLE `playlist_contents` ADD CONSTRAINT `FK_playlist_contents_playlist` FOREIGN KEY (`playlist_id`) REFERENCES `playlists` (`id`);
ALTER TABLE `playlist_contents` ADD CONSTRAINT `FK_playlist_contents_content` FOREIGN KEY (`content_id`) REFERENCES `contents` (`id`);

-- Watching Sessions FKs
ALTER TABLE `watching_sessions` ADD CONSTRAINT `FK_watching_sessions_user` FOREIGN KEY (`watcher_id`) REFERENCES `users` (`id`);
ALTER TABLE `watching_sessions` ADD CONSTRAINT `FK_watching_sessions_content` FOREIGN KEY (`content_id`) REFERENCES `contents` (`id`);

-- Subscriptions FKs
ALTER TABLE `subscriptions` ADD CONSTRAINT `FK_subscriptions_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`);
ALTER TABLE `subscriptions` ADD CONSTRAINT `FK_subscriptions_playlist` FOREIGN KEY (`playlist_id`) REFERENCES `playlists` (`id`);

-- Follows FKs
ALTER TABLE `follows` ADD CONSTRAINT `FK_follows_followee` FOREIGN KEY (`followee_id`) REFERENCES `users` (`id`);
ALTER TABLE `follows` ADD CONSTRAINT `FK_follows_follower` FOREIGN KEY (`follower_id`) REFERENCES `users` (`id`);

-- Conversation Participants FKs
ALTER TABLE `conversation_participants` ADD CONSTRAINT `FK_participants_conversation` FOREIGN KEY (`conversation_id`) REFERENCES `conversations` (`id`);
ALTER TABLE `conversation_participants` ADD CONSTRAINT `FK_participants_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`);

-- Direct Messages FKs
ALTER TABLE `direct_messages` ADD CONSTRAINT `FK_dm_conversation` FOREIGN KEY (`conversation_id`) REFERENCES `conversations` (`id`);
ALTER TABLE `direct_messages` ADD CONSTRAINT `FK_dm_sender` FOREIGN KEY (`sender_id`) REFERENCES `users` (`id`);
ALTER TABLE `direct_messages` ADD CONSTRAINT `FK_dm_receiver` FOREIGN KEY (`receiver_id`) REFERENCES `users` (`id`);

-- JWT Sessions FK
ALTER TABLE `jwt_sessions` ADD CONSTRAINT `FK_jwt_sessions_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`);