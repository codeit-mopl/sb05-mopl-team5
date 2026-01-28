-- Playlist 목록 조회 부하테스트용 대용량 데이터 생성
-- 목적: 구독순 정렬 + 커서 페이지네이션 + 캐싱 효과 검증
-- 실행: mysql -uroot -p mopl < load_test/sql/playlist_review/generate_playlists_reviews.sql 2>&1
--
-- 데이터 생성 계획:
-- • Playlists: 50,000개
-- • Reviews: 200,000개
-- • Contents: 10,000개 (자체 생성 - 기존 데이터 미활용)
-- • Users: 10,000명 (자체 생성)
-- • Subscriptions: Zipf 분포 (상위 5% 플레이리스트가 전체 구독의 80%)
-- • 기간: 2023-01-26 ~ 2026-01-26 (약 3년)
-- • 특징: 구독순 정렬 시 Hot Key 발생, 캐싱 필요성 입증

SET AUTOCOMMIT = 0;
SET UNIQUE_CHECKS = 0;
SET FOREIGN_KEY_CHECKS = 0;

-- ==========================================
-- 1. Contents 생성 (10,000개)
-- ==========================================
DROP PROCEDURE IF EXISTS generate_contents;

DELIMITER $$
CREATE PROCEDURE generate_contents()
BEGIN
    DECLARE i INT DEFAULT 0;
    DECLARE content_uuid BINARY(16);
    DECLARE content_type VARCHAR(20);
    DECLARE random_days INT;
    DECLARE created_date DATETIME;

    WHILE i < 10000 DO
            SET content_uuid = UNHEX(REPLACE(UUID(), '-', ''));

            SET content_type = CASE
                                   WHEN i % 3 = 0 THEN 'movie'
                                   WHEN i % 3 = 1 THEN 'tvSeries'
                                   ELSE 'sport'
                END;

            SET random_days = FLOOR(RAND() * 1095);
            SET created_date = DATE_SUB(NOW(), INTERVAL random_days DAY);

            INSERT INTO contents (
                id, type, api_id, title, description, thumbnail_url, tags,
                rating_sum, review_count, watcher_count, created_at, updated_at, is_deleted
            ) VALUES (
                         content_uuid,
                         content_type,
                         100000 + i,
                         CONCAT('Load Test Content ', i, ' - ', content_type),
                         CONCAT('This is a test content for playlist review load testing. Content type: ', content_type, ', ID: ', i),
                         CONCAT('https://picsum.photos/seed/content', i, '/400/600'),
                         CASE content_type
                             WHEN 'movie' THEN 'action,drama,thriller'
                             WHEN 'tvSeries' THEN 'comedy,romance,fantasy'
                             ELSE 'sports,live,documentary'
                             END,
                         FLOOR(RAND() * 5000),
                         FLOOR(RAND() * 100),
                         FLOOR(RAND() * 1000),
                         created_date,
                         created_date,
                         FALSE
                     );

            SET i = i + 1;

            IF i % 1000 = 0 THEN
                COMMIT;
                SELECT CONCAT('Contents Progress: ', i, ' / 10000') as status;
            END IF;
        END WHILE;

    COMMIT;
    SELECT COUNT(*) as total_contents FROM contents;
END$$
DELIMITER ;

CALL generate_contents();
DROP PROCEDURE IF EXISTS generate_contents;

-- ==========================================
-- 2. Users 생성 (10,000명)
-- ==========================================
DROP PROCEDURE IF EXISTS generate_users;

DELIMITER $$
CREATE PROCEDURE generate_users()
BEGIN
    DECLARE i INT DEFAULT 0;
    DECLARE user_uuid BINARY(16);
    DECLARE existing_users INT;

    SELECT COUNT(*) INTO existing_users FROM users;

    IF existing_users < 10000 THEN
        WHILE i < 10000 - existing_users DO
                SET user_uuid = UNHEX(REPLACE(UUID(), '-', ''));

                INSERT INTO users (
                    id, name, email, password, profile_image_url, role, locked, follower_count, created_at, updated_at
                ) VALUES (
                             user_uuid,
                             CONCAT('LoadTestUser', i),
                             CONCAT('loadtest', i, '@mopl.test'),
                             '$2a$10$dummyPasswordHashForLoadTest',
                             CONCAT('https://picsum.photos/seed/user', i, '/200/200'),
                             'USER',
                             FALSE,
                             FLOOR(RAND() * 100),
                             DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 1095) DAY),
                             NOW()
                         );

                SET i = i + 1;

                IF i % 1000 = 0 THEN
                    COMMIT;
                    SELECT CONCAT('Users Progress: ', i + existing_users, ' / 10000') as status;
                END IF;
            END WHILE;

        COMMIT;
    END IF;

    SELECT COUNT(*) as total_users FROM users;
END$$
DELIMITER ;

CALL generate_users();
DROP PROCEDURE IF EXISTS generate_users;

-- ==========================================
-- 3. Playlists 생성 (50,000개)
-- ==========================================
DROP PROCEDURE IF EXISTS generate_playlists;

DELIMITER $$
CREATE PROCEDURE generate_playlists()
BEGIN
    DECLARE i INT DEFAULT 0;
    DECLARE playlist_uuid BINARY(16);
    DECLARE random_user_id BINARY(16);
    DECLARE random_days INT;
    DECLARE created_date DATETIME;
    DECLARE updated_date DATETIME;
    DECLARE random_subscriber_count BIGINT;

    WHILE i < 50000 DO
            SET playlist_uuid = UNHEX(REPLACE(UUID(), '-', ''));

            -- 랜덤 사용자 선택
            SELECT id INTO random_user_id FROM users ORDER BY RAND() LIMIT 1;

            -- 균등 분포 날짜 생성 (3년)
            SET random_days = FLOOR(RAND() * 1095);
            SET created_date = DATE_SUB(NOW(), INTERVAL random_days DAY);

            -- updated_at: created_at 이후 ~ 현재 사이 랜덤
            IF DATEDIFF(NOW(), created_date) > 0 THEN
                SET updated_date = DATE_ADD(created_date, INTERVAL FLOOR(RAND() * DATEDIFF(NOW(), created_date)) DAY);
            ELSE
                SET updated_date = created_date;
            END IF;

            -- 초기값 0으로 설정 (나중에 Zipf 분포로 재계산)
            SET random_subscriber_count = 0;

            INSERT INTO playlists (
                id, user_id, title, description, subscriber_count, created_at, updated_at, is_deleted
            ) VALUES (
                         playlist_uuid,
                         random_user_id,
                         CONCAT('Load Test Playlist ', i),
                         CONCAT('This is a load test playlist for deep pagination testing. Playlist ID: ', i, '. Created to simulate real-world data distribution.'),
                         random_subscriber_count,
                         created_date,
                         updated_date,
                         FALSE
                     );

            SET i = i + 1;

            IF i % 5000 = 0 THEN
                COMMIT;
                SELECT CONCAT('Playlists Progress: ', i, ' / 50000 (', ROUND(i/50000*100, 1), '%)') as status;
            END IF;
        END WHILE;

    COMMIT;

    -- 통계 출력
    SELECT
        COUNT(*) as total_playlists,
        MIN(created_at) as oldest_date,
        MAX(created_at) as newest_date,
        DATEDIFF(MAX(created_at), MIN(created_at)) as days_span,
        AVG(subscriber_count) as avg_subscribers
    FROM playlists;

END$$
DELIMITER ;

CALL generate_playlists();
DROP PROCEDURE IF EXISTS generate_playlists;

-- ==========================================
-- 4. Subscriptions 생성 (Zipf 분포)
-- ==========================================
-- 상위 5% 플레이리스트가 전체 구독의 80%를 차지하도록 설계
DROP PROCEDURE IF EXISTS generate_subscriptions_zipf;

DELIMITER $$
CREATE PROCEDURE generate_subscriptions_zipf()
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE playlist_id_var BINARY(16);
    DECLARE playlist_rank INT DEFAULT 0;
    DECLARE target_count INT;
    DECLARE i INT;
    DECLARE subscription_uuid BINARY(16);
    DECLARE random_user_id BINARY(16);
    DECLARE total_subscriptions INT DEFAULT 0;
    DECLARE total_playlists INT;

    SELECT COUNT(*) INTO total_playlists FROM playlists;

    DECLARE playlist_cursor CURSOR FOR
    SELECT id FROM playlists ORDER BY RAND();
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    OPEN playlist_cursor;

    read_loop: LOOP
        FETCH playlist_cursor INTO playlist_id_var;
        IF done THEN
            LEAVE read_loop;
        END IF;

        SET playlist_rank = playlist_rank + 1;

        -- Zipf 분포: rank가 낮을수록 많은 구독
        -- 상위 1% (500개): 500~2000 구독
        -- 상위 5% (2500개): 100~500 구독
        -- 상위 20% (10000개): 20~100 구독
        -- 나머지 80% (40000개): 0~20 구독
        SET target_count = CASE
                               WHEN playlist_rank <= total_playlists * 0.01 THEN 500 + FLOOR(RAND() * 1500)
                               WHEN playlist_rank <= total_playlists * 0.05 THEN 100 + FLOOR(RAND() * 400)
                               WHEN playlist_rank <= total_playlists * 0.20 THEN 20 + FLOOR(RAND() * 80)
                               ELSE FLOOR(RAND() * 20)
            END;

        -- subscriber_count 업데이트
        UPDATE playlists SET subscriber_count = target_count WHERE id = playlist_id_var;

        -- 실제 구독 데이터 생성
        SET i = 0;
        WHILE i < target_count DO
                SET subscription_uuid = UNHEX(REPLACE(UUID(), '-', ''));
                SELECT id INTO random_user_id FROM users ORDER BY RAND() LIMIT 1;

                INSERT IGNORE INTO subscriptions (id, user_id, playlist_id, created_at)
                VALUES (subscription_uuid, random_user_id, playlist_id_var, NOW());

                SET i = i + 1;
            END WHILE;

        SET total_subscriptions = total_subscriptions + target_count;

        IF playlist_rank % 1000 = 0 THEN
            COMMIT;
            SELECT CONCAT('Subscriptions Progress: ', playlist_rank, ' / ', total_playlists,
                          ' (Total subs: ', total_subscriptions, ')') as status;
        END IF;
    END LOOP;

    CLOSE playlist_cursor;
    COMMIT;

    SELECT
        COUNT(*) as total_subscriptions,
        COUNT(DISTINCT playlist_id) as playlists_with_subs,
        ROUND(AVG(subscriber_count), 1) as avg_subs_per_playlist,
        MAX(subscriber_count) as max_subs
    FROM playlists;

END$$
DELIMITER ;

CALL generate_subscriptions_zipf();
DROP PROCEDURE IF EXISTS generate_subscriptions_zipf;

-- ==========================================
-- 5. 구독 분포 검증 쿼리
-- ==========================================
SELECT '========================================' as separator;
SELECT 'Subscription Distribution Verification' as report_title;
SELECT '========================================' as separator;

SELECT
    'Top 1%' as tier,
    COUNT(*) as playlists,
    SUM(subscriber_count) as total_subs,
    ROUND(AVG(subscriber_count), 1) as avg_subs,
    MIN(subscriber_count) as min_subs,
    MAX(subscriber_count) as max_subs
FROM (
         SELECT subscriber_count
         FROM playlists
         ORDER BY subscriber_count DESC
         LIMIT 500
     ) t
UNION ALL
SELECT
    'Top 5%' as tier,
    COUNT(*) as playlists,
    SUM(subscriber_count) as total_subs,
    ROUND(AVG(subscriber_count), 1) as avg_subs,
    MIN(subscriber_count) as min_subs,
    MAX(subscriber_count) as max_subs
FROM (
         SELECT subscriber_count
         FROM playlists
         ORDER BY subscriber_count DESC
         LIMIT 2500
     ) t
UNION ALL
SELECT
    'Bottom 80%' as tier,
    COUNT(*) as playlists,
    SUM(subscriber_count) as total_subs,
    ROUND(AVG(subscriber_count), 1) as avg_subs,
    MIN(subscriber_count) as min_subs,
    MAX(subscriber_count) as max_subs
FROM (
         SELECT subscriber_count
         FROM playlists
         ORDER BY subscriber_count ASC
         LIMIT 40000
     ) t;

-- ==========================================
-- 6. Subscriptions 생성 (구 버전 - 사용 안 함)
-- ==========================================
-- (이 프로시저는 더 이상 사용하지 않음 - generate_subscriptions_zipf로 대체됨)

-- ==========================================
-- 7. Reviews 생성 (200,000개)
-- ==========================================
DROP PROCEDURE IF EXISTS generate_reviews;

DELIMITER $$
CREATE PROCEDURE generate_reviews()
BEGIN
    DECLARE i INT DEFAULT 0;
    DECLARE review_uuid BINARY(16);
    DECLARE random_user_id BINARY(16);
    DECLARE random_content_id BINARY(16);
    DECLARE random_days INT;
    DECLARE created_date DATETIME;
    DECLARE random_rating DECIMAL(2,1);
    DECLARE rand_val DOUBLE;

    WHILE i < 200000 DO
            SET review_uuid = UNHEX(REPLACE(UUID(), '-', ''));

            -- 랜덤 사용자 선택
            SELECT id INTO random_user_id FROM users ORDER BY RAND() LIMIT 1;

            -- 랜덤 콘텐츠 선택
            SELECT id INTO random_content_id FROM contents WHERE is_deleted = FALSE ORDER BY RAND() LIMIT 1;

            -- 균등 분포 날짜 생성 (3년)
            SET random_days = FLOOR(RAND() * 1095);
            SET created_date = DATE_SUB(NOW(), INTERVAL random_days DAY);

            -- 평점: 정규분포 시뮬레이션 (평균 3.5, 대부분 3~5점)
            SET rand_val = RAND();
            SET random_rating = CASE
                                    WHEN rand_val < 0.05 THEN ROUND(RAND() * 2, 1)         -- 5%: 0~2점
                                    WHEN rand_val < 0.15 THEN ROUND(2 + RAND() * 1, 1)     -- 10%: 2~3점
                                    WHEN rand_val < 0.50 THEN ROUND(3 + RAND() * 1, 1)     -- 35%: 3~4점
                                    ELSE ROUND(4 + RAND() * 1, 1)                          -- 50%: 4~5점
                END;

            INSERT INTO reviews (
                id, content_id, user_id, text, rating, created_at, updated_at, is_deleted
            ) VALUES (
                         review_uuid,
                         random_content_id,
                         random_user_id,
                         CONCAT('This is a load test review #', i, '. ',
                                CASE FLOOR(random_rating)
                                    WHEN 5 THEN 'Absolutely amazing content! Highly recommended.'
                                    WHEN 4 THEN 'Great content, really enjoyed it.'
                                    WHEN 3 THEN 'Decent content, worth watching.'
                                    WHEN 2 THEN 'Not bad, but could be better.'
                                    ELSE 'Disappointing experience.'
                                    END),
                         random_rating,
                         created_date,
                         created_date,
                         FALSE
                     );

            SET i = i + 1;

            IF i % 5000 = 0 THEN
                COMMIT;
                SELECT CONCAT('Reviews Progress: ', i, ' / 200000 (', ROUND(i/200000*100, 1), '%)') as status;
            END IF;
        END WHILE;

    COMMIT;

    -- 통계 출력
    SELECT
        COUNT(*) as total_reviews,
        MIN(created_at) as oldest_date,
        MAX(created_at) as newest_date,
        DATEDIFF(MAX(created_at), MIN(created_at)) as days_span,
        AVG(rating) as avg_rating
    FROM reviews;

END$$
DELIMITER ;

CALL generate_reviews();
DROP PROCEDURE IF EXISTS generate_reviews;

SET FOREIGN_KEY_CHECKS = 1;
SET UNIQUE_CHECKS = 1;
SET AUTOCOMMIT = 1;

-- ==========================================
-- 최종 통계 리포트
-- ==========================================

SELECT '========================================' as separator;
SELECT 'Playlists Distribution by Month' as report_title;
SELECT '========================================' as separator;

SELECT
    DATE_FORMAT(created_at, '%Y-%m') as month,
    COUNT(*) as count
FROM playlists
GROUP BY DATE_FORMAT(created_at, '%Y-%m')
ORDER BY month;

SELECT '========================================' as separator;
SELECT 'Playlists Summary' as report_title;
SELECT '========================================' as separator;

SELECT
    COUNT(*) as total_playlists,
    MIN(created_at) as oldest_date,
    MAX(created_at) as newest_date,
    DATEDIFF(MAX(created_at), MIN(created_at)) as days_span,
    ROUND(COUNT(*) / DATEDIFF(MAX(created_at), MIN(created_at)), 1) as avg_per_day,
    ROUND(AVG(subscriber_count), 1) as avg_subscribers,
    MAX(subscriber_count) as max_subscribers
FROM playlists;

SELECT '========================================' as separator;
SELECT 'Reviews Distribution by Month' as report_title;
SELECT '========================================' as separator;

SELECT
    DATE_FORMAT(created_at, '%Y-%m') as month,
    COUNT(*) as count,
    ROUND(AVG(rating), 2) as avg_rating
FROM reviews
GROUP BY DATE_FORMAT(created_at, '%Y-%m')
ORDER BY month;

SELECT '========================================' as separator;
SELECT 'Reviews Summary' as report_title;
SELECT '========================================' as separator;

SELECT
    COUNT(*) as total_reviews,
    MIN(created_at) as oldest_date,
    MAX(created_at) as newest_date,
    DATEDIFF(MAX(created_at), MIN(created_at)) as days_span,
    ROUND(COUNT(*) / DATEDIFF(MAX(created_at), MIN(created_at)), 1) as avg_per_day,
    ROUND(AVG(rating), 2) as avg_rating,
    MIN(rating) as min_rating,
    MAX(rating) as max_rating
FROM reviews;

SELECT '========================================' as separator;
SELECT 'Subscriptions Summary' as report_title;
SELECT '========================================' as separator;

SELECT
    COUNT(*) as total_subscriptions,
    COUNT(DISTINCT user_id) as unique_users,
    COUNT(DISTINCT playlist_id) as playlists_with_subscriptions,
    ROUND(COUNT(*) / COUNT(DISTINCT playlist_id), 1) as avg_subs_per_playlist
FROM subscriptions;

SELECT '========================================' as separator;
SELECT 'Overall Summary' as report_title;
SELECT '========================================' as separator;

SELECT
    (SELECT COUNT(*) FROM users) as total_users,
    (SELECT COUNT(*) FROM contents) as total_contents,
    (SELECT COUNT(*) FROM playlists) as total_playlists,
    (SELECT COUNT(*) FROM reviews) as total_reviews,
    (SELECT COUNT(*) FROM subscriptions) as total_subscriptions;

SELECT '========================================' as separator;
SELECT '✅ Data Generation Complete!' as status;
SELECT '========================================' as separator;
