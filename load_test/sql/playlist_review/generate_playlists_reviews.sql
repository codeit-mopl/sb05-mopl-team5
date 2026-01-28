-- 균등 분포 Playlist & Review 대용량 데이터 생성 (Deep Pagination 테스트용)
-- 목적: 페이지네이션 성능 부하 테스트를 위한 대규모 데이터 생성
-- 실행: mysql -uroot -p mopl < load_test/sql/playlist_review/generate_playlists_reviews.sql 2>&1
--
-- 데이터 생성 계획:
-- • Playlists: 50,000개
-- • Reviews: 200,000개
-- • Users: 10,000명 (이미 존재한다고 가정, 없으면 생성)
-- • Contents: 기존 데이터 활용 (generate_contents.sql 먼저 실행 필요)
-- • 기간: 2023-01-26 ~ 2026-01-26 (약 3년)
-- • 분포: 균등 (최신/과거 데이터 편향 제거)
-- • Subscription 데이터: Playlist당 평균 5개 구독 생성

SET AUTOCOMMIT = 0;
SET UNIQUE_CHECKS = 0;
SET FOREIGN_KEY_CHECKS = 0;

-- ==========================================
-- 1. Users 생성 (10,000명)
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
-- 2. Playlists 생성 (50,000개)
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
        SET updated_date = DATE_ADD(created_date, INTERVAL FLOOR(RAND() * DATEDIFF(NOW(), created_date)) DAY);
        
        -- 구독자 수: 파레토 분포 시뮬레이션 (대부분 적고, 소수가 많음)
        SET random_subscriber_count = CASE
            WHEN RAND() < 0.7 THEN FLOOR(RAND() * 10)          -- 70%: 0~10명
            WHEN RAND() < 0.9 THEN FLOOR(RAND() * 100)         -- 20%: 10~100명
            WHEN RAND() < 0.98 THEN FLOOR(RAND() * 1000)       -- 8%: 100~1000명
            ELSE FLOOR(RAND() * 10000)                         -- 2%: 1000~10000명
        END;
        
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
-- 3. Subscriptions 생성 (Playlist당 실제 구독 데이터)
-- ==========================================
DROP PROCEDURE IF EXISTS generate_subscriptions;

DELIMITER $$
CREATE PROCEDURE generate_subscriptions()
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE playlist_id_var BINARY(16);
    DECLARE target_count BIGINT;
    DECLARE current_count INT;
    DECLARE i INT;
    DECLARE subscription_uuid BINARY(16);
    DECLARE random_user_id BINARY(16);
    DECLARE total_processed INT DEFAULT 0;
    
    DECLARE playlist_cursor CURSOR FOR 
        SELECT id, subscriber_count FROM playlists WHERE subscriber_count > 0;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    
    OPEN playlist_cursor;
    
    read_loop: LOOP
        FETCH playlist_cursor INTO playlist_id_var, target_count;
        IF done THEN
            LEAVE read_loop;
        END IF;
        
        SET i = 0;
        WHILE i < target_count DO
            SET subscription_uuid = UNHEX(REPLACE(UUID(), '-', ''));
            
            -- 랜덤 사용자 선택 (중복 방지 로직 생략, 성능 우선)
            SELECT id INTO random_user_id FROM users ORDER BY RAND() LIMIT 1;
            
            INSERT IGNORE INTO subscriptions (id, user_id, playlist_id, created_at)
            VALUES (subscription_uuid, random_user_id, playlist_id_var, NOW());
            
            SET i = i + 1;
        END WHILE;
        
        SET total_processed = total_processed + 1;
        
        IF total_processed % 1000 = 0 THEN
            COMMIT;
            SELECT CONCAT('Subscriptions Progress: ', total_processed, ' playlists processed') as status;
        END IF;
    END LOOP;
    
    CLOSE playlist_cursor;
    COMMIT;
    
    SELECT COUNT(*) as total_subscriptions FROM subscriptions;
    
END$$
DELIMITER ;

CALL generate_subscriptions();
DROP PROCEDURE IF EXISTS generate_subscriptions;

-- ==========================================
-- 4. Reviews 생성 (200,000개)
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
    DECLARE content_exists INT;
    
    -- Contents 데이터 존재 확인
    SELECT COUNT(*) INTO content_exists FROM contents;
    IF content_exists < 1000 THEN
        SELECT 'ERROR: Contents 테이블에 최소 1000개 이상의 데이터가 필요합니다. generate_contents.sql을 먼저 실행하세요.' as error_message;
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Insufficient contents data';
    END IF;
    
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
        SET random_rating = CASE
            WHEN RAND() < 0.05 THEN ROUND(RAND() * 2, 1)           -- 5%: 0~2점
            WHEN RAND() < 0.15 THEN ROUND(2 + RAND() * 1, 1)       -- 10%: 2~3점
            WHEN RAND() < 0.50 THEN ROUND(3 + RAND() * 1, 1)       -- 35%: 3~4점
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
    (SELECT COUNT(*) FROM playlists) as total_playlists,
    (SELECT COUNT(*) FROM reviews) as total_reviews,
    (SELECT COUNT(*) FROM subscriptions) as total_subscriptions,
    (SELECT COUNT(*) FROM contents) as total_contents;
