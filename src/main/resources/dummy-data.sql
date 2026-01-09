-- ==========================================
-- Dummy Data (Validated with Constraints)
-- ==========================================

-- USERS
INSERT INTO users (id, name, email, password, profile_image_url,
                   role, locked, follower_count)
VALUES (UUID_TO_BIN(UUID()), 'Alice', 'alice@test.com', 'password1',
        'https://cdn.test/profiles/alice.jpg', 'USER', FALSE, 25),
       (UUID_TO_BIN(UUID()), 'Bob', 'bob@test.com', 'password2',
        'https://cdn.test/profiles/bob.jpg', 'USER', FALSE, 15),
       (UUID_TO_BIN(UUID()), 'Charlie', 'charlie@test.com', 'password3', NULL, 'USER', FALSE, 8),
       (UUID_TO_BIN(UUID()), 'Diana', 'diana@test.com', 'password4',
        'https://cdn.test/profiles/diana.jpg', 'USER', FALSE, 42),
       (UUID_TO_BIN(UUID()), 'Eve', 'eve@test.com', 'password5', NULL, 'USER', TRUE, 3),
       (UUID_TO_BIN(UUID()), 'Admin', 'admin@test.com', 'adminpass',
        'https://cdn.test/profiles/admin.jpg', 'ADMIN', FALSE, 0);

-- CONTENTS
INSERT INTO contents (id, type, api_id, title, description, thumbnail_url,
                      tags, average_rating, review_count, watcher_count, is_deleted)
VALUES (UUID_TO_BIN(UUID()),
        'MOVIE',
        550,
        'Inception',
        'A thief who steals corporate secrets through dream-sharing technology is given the inverse task of planting an idea into the mind of a C.E.O.',
        'https://cdn.test/inception.jpg',
        JSON_ARRAY('sci-fi', 'thriller', 'action'),
        4.5,
        3,
        150,
        FALSE),
       (UUID_TO_BIN(UUID()),
        'MOVIE',
        13,
        'Forrest Gump',
        'The presidencies of Kennedy and Johnson, the Vietnam War, and other historical events unfold from the perspective of an Alabama man.',
        'https://cdn.test/forrest-gump.jpg',
        JSON_ARRAY('drama', 'romance'),
        4.8,
        2,
        200,
        FALSE),
       (UUID_TO_BIN(UUID()),
        'TV_SERIES',
        1396,
        'Breaking Bad',
        'A high school chemistry teacher turned methamphetamine producer partners with a former student.',
        'https://cdn.test/bb.jpg',
        JSON_ARRAY('crime', 'drama', 'thriller'),
        4.9,
        4,
        320,
        FALSE),
       (UUID_TO_BIN(UUID()),
        'TV_SERIES',
        1399,
        'Game of Thrones',
        'Nine noble families fight for control over the lands of Westeros, while an ancient enemy returns.',
        'https://cdn.test/got.jpg',
        JSON_ARRAY('fantasy', 'drama', 'action'),
        4.3,
        5,
        450,
        FALSE),
       (UUID_TO_BIN(UUID()),
        'SPORT',
        NULL,
        'UEFA Champions League Final 2023',
        'Manchester City vs Inter Milan - The most prestigious club competition in European football.',
        'https://cdn.test/ucl-final.jpg',
        JSON_ARRAY('football', 'soccer', 'live'),
        4.7,
        2,
        180,
        FALSE),
       (UUID_TO_BIN(UUID()),
        'SPORT',
        NULL,
        'NBA Finals 2023',
        'Denver Nuggets vs Miami Heat - The championship series of the NBA.',
        'https://cdn.test/nba-finals.jpg',
        JSON_ARRAY('basketball', 'live', 'sports'),
        4.6,
        1,
        120,
        FALSE),
       (UUID_TO_BIN(UUID()),
        'MOVIE',
        278,
        'The Shawshank Redemption',
        'Two imprisoned men bond over a number of years, finding solace and eventual redemption.',
        'https://cdn.test/shawshank.jpg',
        JSON_ARRAY('drama'),
        4.9,
        6,
        280,
        FALSE),
       (UUID_TO_BIN(UUID()),
        'TV_SERIES',
        82856,
        'The Mandalorian',
        'The travels of a lone bounty hunter in the outer reaches of the galaxy.',
        'https://cdn.test/mandalorian.jpg',
        JSON_ARRAY('sci-fi', 'action', 'adventure'),
        4.4,
        3,
        220,
        FALSE);

-- PLAYLISTS
INSERT INTO playlists (id, user_id, title, description, subscriber_count, is_deleted)
SELECT UUID_TO_BIN(UUID()),
       u.id,
       'My Sci-Fi Collection',
       'Favorite science fiction contents',
       3,
       FALSE
FROM users u
WHERE u.email = 'alice@test.com'
UNION ALL
SELECT UUID_TO_BIN(UUID()),
       u.id,
       'Drama Masterpieces',
       'Best drama series and movies',
       2,
       FALSE
FROM users u
WHERE u.email = 'diana@test.com'
UNION ALL
SELECT UUID_TO_BIN(UUID()),
       u.id,
       'Sports Highlights',
       'Live sports and memorable matches',
       1,
       FALSE
FROM users u
WHERE u.email = 'bob@test.com'
UNION ALL
SELECT UUID_TO_BIN(UUID()),
       u.id,
       'Top Rated Movies',
       'Highest rated movies of all time',
       5,
       FALSE
FROM users u
WHERE u.email = 'charlie@test.com';

-- PLAYLIST CONTENTS
INSERT INTO playlist_contents (id, playlist_id, content_id, is_deleted)
SELECT UUID_TO_BIN(UUID()),
       p.id,
       c.id,
       FALSE
FROM playlists p
         JOIN users u ON p.user_id = u.id
         JOIN contents c ON c.title IN ('Inception', 'The Mandalorian')
WHERE u.email = 'alice@test.com'
UNION ALL
SELECT UUID_TO_BIN(UUID()),
       p.id,
       c.id,
       FALSE
FROM playlists p
         JOIN users u ON p.user_id = u.id
         JOIN contents c ON c.title IN ('Breaking Bad', 'Game of Thrones', 'Forrest Gump')
WHERE u.email = 'diana@test.com'
UNION ALL
SELECT UUID_TO_BIN(UUID()),
       p.id,
       c.id,
       FALSE
FROM playlists p
         JOIN users u ON p.user_id = u.id
         JOIN contents c ON c.title IN ('UEFA Champions League Final 2023', 'NBA Finals 2023')
WHERE u.email = 'bob@test.com'
UNION ALL
SELECT UUID_TO_BIN(UUID()),
       p.id,
       c.id,
       FALSE
FROM playlists p
         JOIN users u ON p.user_id = u.id
         JOIN contents c ON c.title IN ('The Shawshank Redemption', 'Forrest Gump', 'Inception')
WHERE u.email = 'charlie@test.com';

-- REVIEWS
INSERT INTO reviews (id, content_id, user_id, text, rating, is_deleted)
SELECT UUID_TO_BIN(UUID()),
       c.id,
       u.id,
       'Absolutely mind-bending! Christopher Nolan at his best.',
       5.0,
       FALSE
FROM contents c
         JOIN users u
WHERE c.title = 'Inception'
  AND u.email = 'alice@test.com'
UNION ALL
SELECT UUID_TO_BIN(UUID()),
       c.id,
       u.id,
       'One of the greatest TV series ever made. Brilliant writing and acting.',
       5.0,
       FALSE
FROM contents c
         JOIN users u
WHERE c.title = 'Breaking Bad'
  AND u.email = 'bob@test.com'
UNION ALL
SELECT UUID_TO_BIN(UUID()),
       c.id,
       u.id,
       'A timeless masterpiece. Heartwarming and emotional.',
       5.0,
       FALSE
FROM contents c
         JOIN users u
WHERE c.title = 'Forrest Gump'
  AND u.email = 'diana@test.com'
UNION ALL
SELECT UUID_TO_BIN(UUID()),
       c.id,
       u.id,
       'Epic fantasy at its finest, despite the controversial ending.',
       4.0,
       FALSE
FROM contents c
         JOIN users u
WHERE c.title = 'Game of Thrones'
  AND u.email = 'charlie@test.com'
UNION ALL
SELECT UUID_TO_BIN(UUID()),
       c.id,
       u.id,
       'What an incredible match! City finally won the treble.',
       4.5,
       FALSE
FROM contents c
         JOIN users u
WHERE c.title = 'UEFA Champions League Final 2023'
  AND u.email = 'bob@test.com'
UNION ALL
SELECT UUID_TO_BIN(UUID()),
       c.id,
       u.id,
       'The best prison drama ever. Hope is a powerful thing.',
       5.0,
       FALSE
FROM contents c
         JOIN users u
WHERE c.title = 'The Shawshank Redemption'
  AND u.email = 'alice@test.com'
UNION ALL
SELECT UUID_TO_BIN(UUID()),
       c.id,
       u.id,
       'Great addition to Star Wars universe. Baby Yoda is adorable!',
       4.5,
       FALSE
FROM contents c
         JOIN users u
WHERE c.title = 'The Mandalorian'
  AND u.email = 'diana@test.com'
UNION ALL
SELECT UUID_TO_BIN(UUID()),
       c.id,
       u.id,
       'Inception is visually stunning but a bit confusing on first watch.',
       4.0,
       FALSE
FROM contents c
         JOIN users u
WHERE c.title = 'Inception'
  AND u.email = 'charlie@test.com'
UNION ALL
SELECT UUID_TO_BIN(UUID()),
       c.id,
       u.id,
       'Breaking Bad keeps getting better with each season.',
       5.0,
       FALSE
FROM contents c
         JOIN users u
WHERE c.title = 'Breaking Bad'
  AND u.email = 'diana@test.com'
UNION ALL
SELECT UUID_TO_BIN(UUID()),
       c.id,
       u.id,
       'Game of Thrones seasons 1-6 are phenomenal!',
       4.5,
       FALSE
FROM contents c
         JOIN users u
WHERE c.title = 'Game of Thrones'
  AND u.email = 'alice@test.com'
UNION ALL
SELECT UUID_TO_BIN(UUID()),
       c.id,
       u.id,
       'The Shawshank Redemption deserves all the praise it gets.',
       5.0,
       FALSE
FROM contents c
         JOIN users u
WHERE c.title = 'The Shawshank Redemption'
  AND u.email = 'bob@test.com'
UNION ALL
SELECT UUID_TO_BIN(UUID()),
       c.id,
       u.id,
       'Forrest Gump is a beautiful story about life.',
       4.5,
       FALSE
FROM contents c
         JOIN users u
WHERE c.title = 'Forrest Gump'
  AND u.email = 'charlie@test.com'
UNION ALL
SELECT UUID_TO_BIN(UUID()),
       c.id,
       u.id,
       'NBA Finals was intense! Jokic MVP performance.',
       4.5,
       FALSE
FROM contents c
         JOIN users u
WHERE c.title = 'NBA Finals 2023'
  AND u.email = 'charlie@test.com'
UNION ALL
SELECT UUID_TO_BIN(UUID()),
       c.id,
       u.id,
       'Breaking Bad - the definition of a perfect TV series.',
       5.0,
       FALSE
FROM contents c
         JOIN users u
WHERE c.title = 'Breaking Bad'
  AND u.email = 'alice@test.com'
UNION ALL
SELECT UUID_TO_BIN(UUID()),
       c.id,
       u.id,
       'Game of Thrones could have been perfect with a better ending.',
       4.0,
       FALSE
FROM contents c
         JOIN users u
WHERE c.title = 'Game of Thrones'
  AND u.email = 'bob@test.com'
UNION ALL
SELECT UUID_TO_BIN(UUID()),
       c.id,
       u.id,
       'UCL Final was a tactical masterclass by Guardiola.',
       5.0,
       FALSE
FROM contents c
         JOIN users u
WHERE c.title = 'UEFA Champions League Final 2023'
  AND u.email = 'alice@test.com'
UNION ALL
SELECT UUID_TO_BIN(UUID()),
       c.id,
       u.id,
       'The Mandalorian brings back the magic of original Star Wars.',
       4.0,
       FALSE
FROM contents c
         JOIN users u
WHERE c.title = 'The Mandalorian'
  AND u.email = 'charlie@test.com'
UNION ALL
SELECT UUID_TO_BIN(UUID()),
       c.id,
       u.id,
       'The Shawshank Redemption - a movie everyone should watch.',
       5.0,
       FALSE
FROM contents c
         JOIN users u
WHERE c.title = 'The Shawshank Redemption'
  AND u.email = 'diana@test.com';

-- WATCHING SESSIONS
INSERT INTO watching_sessions (id, watcher_id, content_id)
SELECT UUID_TO_BIN(UUID()),
       u.id,
       c.id
FROM users u
         JOIN contents c
WHERE u.email = 'bob@test.com'
  AND c.title = 'Breaking Bad'
UNION ALL
SELECT UUID_TO_BIN(UUID()),
       u.id,
       c.id
FROM users u
         JOIN contents c
WHERE u.email = 'alice@test.com'
  AND c.title = 'Inception'
UNION ALL
SELECT UUID_TO_BIN(UUID()),
       u.id,
       c.id
FROM users u
         JOIN contents c
WHERE u.email = 'diana@test.com'
  AND c.title = 'Game of Thrones'
UNION ALL
SELECT UUID_TO_BIN(UUID()),
       u.id,
       c.id
FROM users u
         JOIN contents c
WHERE u.email = 'charlie@test.com'
  AND c.title = 'The Shawshank Redemption'
UNION ALL
SELECT UUID_TO_BIN(UUID()),
       u.id,
       c.id
FROM users u
         JOIN contents c
WHERE u.email = 'bob@test.com'
  AND c.title = 'NBA Finals 2023'
UNION ALL
SELECT UUID_TO_BIN(UUID()),
       u.id,
       c.id
FROM users u
         JOIN contents c
WHERE u.email = 'alice@test.com'
  AND c.title = 'UEFA Champions League Final 2023'
UNION ALL
SELECT UUID_TO_BIN(UUID()),
       u.id,
       c.id
FROM users u
         JOIN contents c
WHERE u.email = 'diana@test.com'
  AND c.title = 'The Mandalorian'
UNION ALL
SELECT UUID_TO_BIN(UUID()),
       u.id,
       c.id
FROM users u
         JOIN contents c
WHERE u.email = 'charlie@test.com'
  AND c.title = 'Forrest Gump';

-- SUBSCRIPTIONS
INSERT INTO subscriptions (id, user_id, playlist_id)
SELECT UUID_TO_BIN(UUID()),
       u.id,
       p.id
FROM users u
         JOIN playlists p ON p.title = 'My Sci-Fi Collection'
WHERE u.email = 'bob@test.com'
UNION ALL
SELECT UUID_TO_BIN(UUID()),
       u.id,
       p.id
FROM users u
         JOIN playlists p ON p.title = 'Drama Masterpieces'
WHERE u.email = 'alice@test.com'
UNION ALL
SELECT UUID_TO_BIN(UUID()),
       u.id,
       p.id
FROM users u
         JOIN playlists p ON p.title = 'Top Rated Movies'
WHERE u.email = 'bob@test.com'
UNION ALL
SELECT UUID_TO_BIN(UUID()),
       u.id,
       p.id
FROM users u
         JOIN playlists p ON p.title = 'Sports Highlights'
WHERE u.email = 'charlie@test.com'
UNION ALL
SELECT UUID_TO_BIN(UUID()),
       u.id,
       p.id
FROM users u
         JOIN playlists p ON p.title = 'My Sci-Fi Collection'
WHERE u.email = 'diana@test.com'
UNION ALL
SELECT UUID_TO_BIN(UUID()),
       u.id,
       p.id
FROM users u
         JOIN playlists p ON p.title = 'Top Rated Movies'
WHERE u.email = 'diana@test.com'
UNION ALL
SELECT UUID_TO_BIN(UUID()),
       u.id,
       p.id
FROM users u
         JOIN playlists p ON p.title = 'Drama Masterpieces'
WHERE u.email = 'charlie@test.com';

-- FOLLOWS
INSERT INTO follows (id, followee_id, follower_id)
SELECT UUID_TO_BIN(UUID()),
       followee.id,
       follower.id
FROM users followee
         JOIN users follower
WHERE followee.email = 'alice@test.com'
  AND follower.email = 'bob@test.com'
UNION ALL
SELECT UUID_TO_BIN(UUID()),
       followee.id,
       follower.id
FROM users followee
         JOIN users follower
WHERE followee.email = 'alice@test.com'
  AND follower.email = 'charlie@test.com'
UNION ALL
SELECT UUID_TO_BIN(UUID()),
       followee.id,
       follower.id
FROM users followee
         JOIN users follower
WHERE followee.email = 'diana@test.com'
  AND follower.email = 'alice@test.com'
UNION ALL
SELECT UUID_TO_BIN(UUID()),
       followee.id,
       follower.id
FROM users followee
         JOIN users follower
WHERE followee.email = 'diana@test.com'
  AND follower.email = 'bob@test.com'
UNION ALL
SELECT UUID_TO_BIN(UUID()),
       followee.id,
       follower.id
FROM users followee
         JOIN users follower
WHERE followee.email = 'bob@test.com'
  AND follower.email = 'charlie@test.com'
UNION ALL
SELECT UUID_TO_BIN(UUID()),
       followee.id,
       follower.id
FROM users followee
         JOIN users follower
WHERE followee.email = 'charlie@test.com'
  AND follower.email = 'diana@test.com'
UNION ALL
SELECT UUID_TO_BIN(UUID()),
       followee.id,
       follower.id
FROM users followee
         JOIN users follower
WHERE followee.email = 'alice@test.com'
  AND follower.email = 'diana@test.com';

-- NOTIFICATIONS
INSERT INTO notifications (id, receiver_id, title, content, level)
SELECT UUID_TO_BIN(UUID()),
       u.id,
       'Welcome to MOPL',
       'Welcome to MOPL! Start creating your playlists and discover amazing contents.',
       'INFO'
FROM users u
UNION ALL
SELECT UUID_TO_BIN(UUID()),
       u.id,
       'New Follower',
       CONCAT(follower.name, ' started following you!'),
       'INFO'
FROM follows f
         JOIN users u ON f.followee_id = u.id
         JOIN users follower ON f.follower_id = follower.id
WHERE u.email = 'alice@test.com'
UNION ALL
SELECT UUID_TO_BIN(UUID()),
       u.id,
       'Playlist Subscriber',
       'Someone subscribed to your playlist!',
       'INFO'
FROM users u
         JOIN playlists p ON p.user_id = u.id
WHERE u.email = 'alice@test.com'
UNION ALL
SELECT UUID_TO_BIN(UUID()),
       u.id,
       'System Maintenance',
       'Scheduled maintenance on Dec 30, 2025 at 2:00 AM UTC.',
       'WARNING'
FROM users u
WHERE u.email = 'admin@test.com';

-- SOCIAL ACCOUNTS
INSERT INTO social_accounts (id, user_id, provider, provider_id)
SELECT UUID_TO_BIN(UUID()),
       u.id,
       'LOCAL',
       CONCAT('local_', u.email)
FROM users u
WHERE u.email IN ('alice@test.com', 'bob@test.com', 'admin@test.com')
UNION ALL
SELECT UUID_TO_BIN(UUID()),
       u.id,
       'GOOGLE',
       'google_charlie_12345'
FROM users u
WHERE u.email = 'charlie@test.com'
UNION ALL
SELECT UUID_TO_BIN(UUID()),
       u.id,
       'KAKAO',
       'kakao_diana_67890'
FROM users u
WHERE u.email = 'diana@test.com'
UNION ALL
SELECT UUID_TO_BIN(UUID()),
       u.id,
       'GOOGLE',
       'google_eve_54321'
FROM users u
WHERE u.email = 'eve@test.com';

-- CONVERSATIONS
INSERT INTO conversations (id)
VALUES (UUID_TO_BIN(UUID())),
       (UUID_TO_BIN(UUID())),
       (UUID_TO_BIN(UUID()));

-- CONVERSATION PARTICIPANTS
INSERT INTO conversation_participants (id, conversation_id, user_id, last_read_at)
SELECT UUID_TO_BIN(UUID()),
       c.id,
       u.id,
       NOW()
FROM (SELECT id, ROW_NUMBER() OVER () as rn FROM conversations) c
         JOIN users u ON u.email IN ('alice@test.com', 'bob@test.com')
WHERE c.rn = 1
UNION ALL
SELECT UUID_TO_BIN(UUID()),
       c.id,
       u.id,
       DATE_SUB(NOW(), INTERVAL 2 HOUR)
FROM (SELECT id, ROW_NUMBER() OVER () as rn FROM conversations) c
         JOIN users u ON u.email IN ('alice@test.com', 'diana@test.com')
WHERE c.rn = 2
UNION ALL
SELECT UUID_TO_BIN(UUID()),
       c.id,
       u.id,
       NULL
FROM (SELECT id, ROW_NUMBER() OVER () as rn FROM conversations) c
         JOIN users u ON u.email IN ('bob@test.com', 'charlie@test.com')
WHERE c.rn = 3;

-- DIRECT MESSAGES
INSERT INTO direct_messages (id, conversation_id, sender_id, receiver_id, content)
SELECT UUID_TO_BIN(UUID()),
       c.id,
       s.id,
       r.id,
       'Hey Bob! Have you watched Inception yet? You must see it!'
FROM (SELECT id, ROW_NUMBER() OVER () as rn FROM conversations) c
         JOIN users s ON s.email = 'alice@test.com'
         JOIN users r ON r.email = 'bob@test.com'
WHERE c.rn = 1
UNION ALL
SELECT UUID_TO_BIN(UUID()),
       c.id,
       s.id,
       r.id,
       'Yes Alice! Just finished it yesterday. Mind-blowing!'
FROM (SELECT id, ROW_NUMBER() OVER () as rn FROM conversations) c
         JOIN users s ON s.email = 'bob@test.com'
         JOIN users r ON r.email = 'alice@test.com'
WHERE c.rn = 1
UNION ALL
SELECT UUID_TO_BIN(UUID()),
       c.id,
       s.id,
       r.id,
       'I added it to my Top Rated Movies playlist 🎬'
FROM (SELECT id, ROW_NUMBER() OVER () as rn FROM conversations) c
         JOIN users s ON s.email = 'bob@test.com'
         JOIN users r ON r.email = 'alice@test.com'
WHERE c.rn = 1
UNION ALL
SELECT UUID_TO_BIN(UUID()),
       c.id,
       s.id,
       r.id,
       'Diana, your Drama Masterpieces playlist is amazing!'
FROM (SELECT id, ROW_NUMBER() OVER () as rn FROM conversations) c
         JOIN users s ON s.email = 'alice@test.com'
         JOIN users r ON r.email = 'diana@test.com'
WHERE c.rn = 2
UNION ALL
SELECT UUID_TO_BIN(UUID()),
       c.id,
       s.id,
       r.id,
       'Thank you! I spend a lot of time curating it 😊'
FROM (SELECT id, ROW_NUMBER() OVER () as rn FROM conversations) c
         JOIN users s ON s.email = 'diana@test.com'
         JOIN users r ON r.email = 'alice@test.com'
WHERE c.rn = 2
UNION ALL
SELECT UUID_TO_BIN(UUID()),
       c.id,
       s.id,
       r.id,
       'Charlie, did you catch the NBA Finals?'
FROM (SELECT id, ROW_NUMBER() OVER () as rn FROM conversations) c
         JOIN users s ON s.email = 'bob@test.com'
         JOIN users r ON r.email = 'charlie@test.com'
WHERE c.rn = 3
UNION ALL
SELECT UUID_TO_BIN(UUID()),
       c.id,
       s.id,
       r.id,
       'Of course! Jokic was unstoppable 🏀'
FROM (SELECT id, ROW_NUMBER() OVER () as rn FROM conversations) c
         JOIN users s ON s.email = 'charlie@test.com'
         JOIN users r ON r.email = 'bob@test.com'
WHERE c.rn = 3;

-- JWT SESSIONS
INSERT INTO jwt_sessions (id, user_id, refresh_token, expired_at)
SELECT UUID_TO_BIN(UUID()),
       u.id,
       CONCAT('refresh_token_', u.email, '_', UUID()),
       DATE_ADD(NOW(), INTERVAL 30 DAY)
FROM users u
WHERE u.email IN
      ('alice@test.com', 'bob@test.com', 'charlie@test.com', 'diana@test.com', 'admin@test.com');
