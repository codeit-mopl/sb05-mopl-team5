-- ==========================================
-- Dummy Data (Validated with Constraints)
-- ==========================================

-- USERS
INSERT INTO users (id, name, email, password, profile_image_url,
                   role, locked, follower_count)
VALUES (UUID_TO_BIN(UUID()), 'Alice', 'alice@test.com', 'password1', NULL, 'USER', FALSE, 10),
       (UUID_TO_BIN(UUID()), 'Bob', 'bob@test.com', 'password2', NULL, 'USER', FALSE, 5),
       (UUID_TO_BIN(UUID()), 'Admin', 'admin@test.com', 'adminpass', NULL, 'ADMIN', FALSE, 0);

-- CONTENTS
INSERT INTO contents (id, type, title, description, thumbnail_url,
                      tags, average_rating, review_count, watcher_count, is_deleted)
VALUES (UUID_TO_BIN(UUID()),
        'movie',
        'Inception',
        'Sci-fi movie',
        'https://cdn.test/inception.jpg',
        JSON_ARRAY('sci-fi', 'thriller'),
        4.5,
        1,
        100,
        FALSE),
       (UUID_TO_BIN(UUID()),
        'tvSeries',
        'Breaking Bad',
        'Crime drama',
        'https://cdn.test/bb.jpg',
        JSON_ARRAY('crime', 'drama'),
        4.8,
        0,
        200,
        FALSE);

-- PLAYLISTS
INSERT INTO playlists (id, user_id, title, description, subscriber_count, is_deleted)
SELECT UUID_TO_BIN(UUID()),
       u.id,
       'My Playlist',
       'Favorite contents',
       1,
       FALSE
FROM users u
WHERE u.email = 'alice@test.com';

-- PLAYLIST CONTENTS
INSERT INTO playlist_contents (id, playlist_id, content_id, is_deleted)
SELECT UUID_TO_BIN(UUID()),
       p.id,
       c.id,
       FALSE
FROM playlists p
         JOIN contents c ON c.title = 'Inception';

-- REVIEWS
INSERT INTO reviews (id, content_id, user_id, text, rating, is_deleted)
SELECT UUID_TO_BIN(UUID()),
       c.id,
       u.id,
       'Great movie',
       5.0,
       FALSE
FROM contents c
         JOIN users u
WHERE c.title = 'Inception'
  AND u.email = 'alice@test.com';

-- WATCHING SESSIONS
INSERT INTO watching_sessions (id, watcher_id, content_id)
SELECT UUID_TO_BIN(UUID()),
       u.id,
       c.id
FROM users u
         JOIN contents c
WHERE u.email = 'bob@test.com'
  AND c.title = 'Breaking Bad';

-- SUBSCRIPTIONS
INSERT INTO subscriptions (id, user_id, playlist_id)
SELECT UUID_TO_BIN(UUID()),
       u.id,
       p.id
FROM users u
         JOIN playlists p
WHERE u.email = 'bob@test.com';

-- FOLLOWS
INSERT INTO follows (id, followee_id, follower_id)
SELECT UUID_TO_BIN(UUID()),
       followee.id,
       follower.id
FROM users followee
         JOIN users follower
WHERE followee.email = 'alice@test.com'
  AND follower.email = 'bob@test.com';

-- NOTIFICATIONS
INSERT INTO notifications (id, receiver_id, title, content, level)
SELECT UUID_TO_BIN(UUID()),
       u.id,
       'Welcome',
       'Welcome to MOPL',
       'INFO'
FROM users u;

-- SOCIAL ACCOUNTS
INSERT INTO social_accounts (id, provider, provider_id, user_id)
SELECT UUID_TO_BIN(UUID()),
       'LOCAL',
       'alice_local_id',
       u.id
FROM users u
WHERE u.email = 'alice@test.com';

-- CONVERSATIONS
INSERT INTO conversations (id)
VALUES (UUID_TO_BIN(UUID()));

-- CONVERSATION PARTICIPANTS
INSERT INTO conversation_participants (id, conversation_id, user_id)
SELECT UUID_TO_BIN(UUID()),
       c.id,
       u.id
FROM conversations c
         JOIN users u ON u.email IN ('alice@test.com', 'bob@test.com');

-- DIRECT MESSAGES
INSERT INTO direct_messages (id, conversation_id, sender_id, receiver_id, content)
SELECT UUID_TO_BIN(UUID()),
       c.id,
       s.id,
       r.id,
       'Hello!'
FROM conversations c
         JOIN users s ON s.email = 'alice@test.com'
         JOIN users r ON r.email = 'bob@test.com';

-- JWT SESSIONS
INSERT INTO jwt_sessions (id, refresh_token, expired_at, user_id)
SELECT UUID_TO_BIN(UUID()),
       'dummy-refresh-token',
       DATE_ADD(NOW(), INTERVAL 30 DAY),
       u.id
FROM users u
WHERE u.email = 'alice@test.com';
