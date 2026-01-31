-- Redis 캐시 성능 테스트용 데이터 생성
-- 목적: Playlist Detail 조회 시 Redis 캐싱 성능 측정
-- 테스트 시나리오: 동일 Playlist를 여러 사용자가 반복 조회 (캐시 히트율 측정)
-- 실행: mysql -uroot -p mopl < load_test/sql/playlist_review/generate_redis_cache_test_data.sql 2>&1
--
-- 데이터 생성 계획:
-- • Popular Playlists: 100개 (높은 조회수 시뮬레이션)
-- • Test Users: 1,000명 (인증 토큰 생성 필요)
-- • Contents per Playlist: 10~50개
-- • Subscriptions: Playlist당 50~200개 (인기 플레이리스트 시뮬레이션)



-- ==========================================
-- 1. Test Users 생성 (1,000명)
-- ==========================================
DROP PROCEDURE IF EXISTS generate_cache_test_users;

DELIMITER $$
CREATE PROCEDURE generate_cache_test_users()
BEGIN
    DECLARE i INT DEFAULT 0;
    DECLARE user_uuid BINARY(16);
    
    WHILE i < 1000 DO
        SET user_uuid = UNHEX(REPLACE(UUID(), '-', ''));
        
        INSERT INTO users (
            id, name, email, password, profile_image_url, role, locked, follower_count, created_at, updated_at
        ) VALUES (
            user_uuid,
            CONCAT('CacheTestUser', i),
            CONCAT('cachetest', i, '@mopl.test'),
            '$2a$10$N9qo8uLOickgx2ZMRZoMye7GvSakAeXM4Q8bPLBHrSc9SRCxUIXBy',
            CONCAT('https://picsum.photos/seed/cacheuser', i, '/200/200'),
            'USER',
            FALSE,
            FLOOR(RAND() * 50),
            NOW(),
            NOW()
        );
        
        SET i = i + 1;
        
        IF i % 100 = 0 THEN
            COMMIT;
            SELECT CONCAT('Cache Test Users Progress: ', i, ' / 1000') as status;
        END IF;
    END WHILE;
    
    COMMIT;
    SELECT COUNT(*) as total_cache_test_users FROM users WHERE email LIKE 'cachetest%@mopl.test';
END$$
DELIMITER ;

CALL generate_cache_test_users();
DROP PROCEDURE IF EXISTS generate_cache_test_users;

-- ==========================================
-- 2. Popular Playlists 생성 (100개)
-- ==========================================
DROP PROCEDURE IF EXISTS generate_popular_playlists;

DELIMITER $$
CREATE PROCEDURE generate_popular_playlists()
BEGIN
    DECLARE i INT DEFAULT 0;
    DECLARE playlist_uuid BINARY(16);
    DECLARE random_user_id BINARY(16);
    DECLARE created_date DATETIME;
    DECLARE random_subscriber_count BIGINT;
    
    WHILE i < 100 DO
        SET playlist_uuid = UNHEX(REPLACE(UUID(), '-', ''));
        
            SELECT id INTO random_user_id FROM users WHERE email LIKE 'cachetest%@mopl.test' ORDER BY RAND() LIMIT 1;
        
        SET created_date = DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 365) DAY);
        
        SET random_subscriber_count = FLOOR(50 + RAND() * 150);
        
        INSERT INTO playlists (
            id, user_id, title, description, subscriber_count, created_at, updated_at, is_deleted
        ) VALUES (
            playlist_uuid,
            random_user_id,
            CONCAT('Popular Playlist for Cache Test ', i),
            CONCAT('This is a popular playlist designed for Redis caching performance testing. Expected high cache hit rate. Playlist ID: ', i),
            random_subscriber_count,
            created_date,
            NOW(),
            FALSE
        );
        
        SET i = i + 1;
        
        IF i % 10 = 0 THEN
            COMMIT;
            SELECT CONCAT('Popular Playlists Progress: ', i, ' / 100') as status;
        END IF;
    END WHILE;
    
    COMMIT;
    
    SELECT 
        COUNT(*) as total_popular_playlists,
        AVG(subscriber_count) as avg_subscribers,
        MIN(subscriber_count) as min_subscribers,
        MAX(subscriber_count) as max_subscribers
    FROM playlists 
    WHERE title LIKE 'Popular Playlist for Cache Test%';
    
END$$
DELIMITER ;

CALL generate_popular_playlists();
DROP PROCEDURE IF EXISTS generate_popular_playlists;

-- ==========================================
-- 3. PlaylistContents 생성 (Playlist당 10~50개)
-- ==========================================
DROP PROCEDURE IF EXISTS generate_playlist_contents;

DELIMITER $$
CREATE PROCEDURE generate_playlist_contents()
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE playlist_id_var BINARY(16);
    DECLARE random_content_count INT;
    DECLARE i INT;
    DECLARE playlist_content_uuid BINARY(16);
    DECLARE random_content_id BINARY(16);
    DECLARE total_processed INT DEFAULT 0;
    
    DECLARE playlist_cursor CURSOR FOR 
        SELECT id FROM playlists WHERE title LIKE 'Popular Playlist for Cache Test%';
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    
    SELECT COUNT(*) INTO @content_count FROM contents WHERE is_deleted = FALSE;
    IF @content_count < 100 THEN
        SELECT 'ERROR: Contents 테이블에 최소 100개 이상의 데이터가 필요합니다.' as error_message;
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Insufficient contents data';
    END IF;
    
    OPEN playlist_cursor;
    
    read_loop: LOOP
        FETCH playlist_cursor INTO playlist_id_var;
        IF done THEN
            LEAVE read_loop;
        END IF;
        
        SET random_content_count = FLOOR(10 + RAND() * 40);
        SET i = 0;
        
        WHILE i < random_content_count DO
            SET playlist_content_uuid = UNHEX(REPLACE(UUID(), '-', ''));
            
            SELECT id INTO random_content_id FROM contents WHERE is_deleted = FALSE ORDER BY RAND() LIMIT 1;
            
            INSERT IGNORE INTO playlist_contents (id, playlist_id, content_id, position, created_at)
            VALUES (playlist_content_uuid, playlist_id_var, random_content_id, i, NOW());
            
            SET i = i + 1;
        END WHILE;
        
        SET total_processed = total_processed + 1;
        
        IF total_processed % 10 = 0 THEN
            COMMIT;
            SELECT CONCAT('Playlist Contents Progress: ', total_processed, ' playlists processed') as status;
        END IF;
    END LOOP;
    
    CLOSE playlist_cursor;
    COMMIT;
    
    SELECT COUNT(*) as total_playlist_contents FROM playlist_contents 
    WHERE playlist_id IN (SELECT id FROM playlists WHERE title LIKE 'Popular Playlist for Cache Test%');
    
END$$
DELIMITER ;

CALL generate_playlist_contents();
DROP PROCEDURE IF EXISTS generate_playlist_contents;

-- ==========================================
-- 4. Subscriptions 생성 (Playlist당 실제 구독)
-- ==========================================
DROP PROCEDURE IF EXISTS generate_cache_test_subscriptions;

DELIMITER $$
CREATE PROCEDURE generate_cache_test_subscriptions()
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE playlist_id_var BINARY(16);
    DECLARE target_count BIGINT;
    DECLARE i INT;
    DECLARE subscription_uuid BINARY(16);
    DECLARE random_user_id BINARY(16);
    DECLARE total_processed INT DEFAULT 0;
    
    DECLARE playlist_cursor CURSOR FOR 
        SELECT id, subscriber_count FROM playlists WHERE title LIKE 'Popular Playlist for Cache Test%';
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
            
        SELECT id INTO random_user_id FROM users WHERE email LIKE 'cachetest%@mopl.test' ORDER BY RAND() LIMIT 1;
            
            INSERT IGNORE INTO subscriptions (id, user_id, playlist_id, created_at)
            VALUES (subscription_uuid, random_user_id, playlist_id_var, NOW());
            
            SET i = i + 1;
        END WHILE;
        
        SET total_processed = total_processed + 1;
        
        IF total_processed % 10 = 0 THEN
            COMMIT;
            SELECT CONCAT('Cache Test Subscriptions Progress: ', total_processed, ' playlists processed') as status;
        END IF;
    END LOOP;
    
    CLOSE playlist_cursor;
    COMMIT;
    
    SELECT COUNT(*) as total_cache_test_subscriptions FROM subscriptions 
    WHERE playlist_id IN (SELECT id FROM playlists WHERE title LIKE 'Popular Playlist for Cache Test%');
    
END$$
DELIMITER ;

-- ==========================================
-- Main Execution Wrapper with Error Handling
-- ==========================================
DROP PROCEDURE IF EXISTS execute_all_with_cleanup;

DELIMITER $$
CREATE PROCEDURE execute_all_with_cleanup()
BEGIN
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        -- Restore settings on error
        SET FOREIGN_KEY_CHECKS = 1;
        SET UNIQUE_CHECKS = 1;
        SET AUTOCOMMIT = 1;
        
        SELECT 'ERROR: Script failed. Settings have been restored.' as error_status;
        
        -- Re-signal the error
        RESIGNAL;
    END;
    
    -- Set performance optimizations
    SET AUTOCOMMIT = 0;
    SET UNIQUE_CHECKS = 0;
    SET FOREIGN_KEY_CHECKS = 0;
    
    -- Execute all generation procedures
    CALL generate_cache_test_users();
    CALL generate_popular_playlists();
    CALL generate_playlist_contents();
    CALL generate_cache_test_subscriptions();
    
    -- Restore settings after successful completion
    SET FOREIGN_KEY_CHECKS = 1;
    SET UNIQUE_CHECKS = 1;
    SET AUTOCOMMIT = 1;
    
    SELECT 'SUCCESS: All procedures completed. Settings restored.' as success_status;
END$$
DELIMITER ;

-- Execute the main wrapper
CALL execute_all_with_cleanup();

-- Clean up all procedures
DROP PROCEDURE IF EXISTS generate_cache_test_users;
DROP PROCEDURE IF EXISTS generate_popular_playlists;
DROP PROCEDURE IF EXISTS generate_playlist_contents;
DROP PROCEDURE IF EXISTS generate_cache_test_subscriptions;
DROP PROCEDURE IF EXISTS execute_all_with_cleanup;

-- ==========================================
-- 5. CSV 파일 생성 (JMeter용)
-- ==========================================

-- 5.1 Playlist IDs 추출
SELECT 'playlist_id' as playlist_id
UNION ALL
SELECT CONCAT(
    SUBSTR(LOWER(HEX(id)), 1, 8), '-',
    SUBSTR(LOWER(HEX(id)), 9, 4), '-',
    SUBSTR(LOWER(HEX(id)), 13, 4), '-',
    SUBSTR(LOWER(HEX(id)), 17, 4), '-',
    SUBSTR(LOWER(HEX(id)), 21, 12)
) as playlist_id
FROM playlists 
WHERE title LIKE 'Popular Playlist for Cache Test%'
INTO OUTFILE '/tmp/playlist_ids.csv'
FIELDS TERMINATED BY ','
ENCLOSED BY ''
LINES TERMINATED BY '\n';

-- 5.2 User Tokens 생성 (실제 JWT 토큰 대신 임시 ID 사용)
-- 주의: 실제 테스트 시에는 Spring Boot 애플리케이션에서 JWT 토큰을 생성해야 함
SELECT 'user_token' as user_token
UNION ALL
SELECT CONCAT('dummy_token_', LOWER(HEX(id))) as user_token
FROM users 
WHERE email LIKE 'cachetest%@mopl.test'
LIMIT 200
INTO OUTFILE '/tmp/user_tokens.csv'
FIELDS TERMINATED BY ','
ENCLOSED BY ''
LINES TERMINATED BY '\n';

SET FOREIGN_KEY_CHECKS = 1;
SET UNIQUE_CHECKS = 1;
SET AUTOCOMMIT = 1;

-- ==========================================
-- 최종 통계 리포트
-- ==========================================

SELECT '========================================' as separator;
SELECT 'Redis Cache Test Data Summary' as report_title;
SELECT '========================================' as separator;

SELECT 
    (SELECT COUNT(*) FROM users WHERE email LIKE 'cachetest%@mopl.test') as cache_test_users,
    (SELECT COUNT(*) FROM playlists WHERE title LIKE 'Popular Playlist for Cache Test%') as popular_playlists,
    (SELECT COUNT(*) FROM playlist_contents WHERE playlist_id IN 
        (SELECT id FROM playlists WHERE title LIKE 'Popular Playlist for Cache Test%')) as total_playlist_contents,
    (SELECT COUNT(*) FROM subscriptions WHERE playlist_id IN 
        (SELECT id FROM playlists WHERE title LIKE 'Popular Playlist for Cache Test%')) as total_subscriptions;

SELECT '========================================' as separator;
SELECT 'Popular Playlists Details' as report_title;
SELECT '========================================' as separator;

SELECT 
    COUNT(*) as total_playlists,
    ROUND(AVG(subscriber_count), 1) as avg_subscribers,
    MIN(subscriber_count) as min_subscribers,
    MAX(subscriber_count) as max_subscribers
FROM playlists
WHERE title LIKE 'Popular Playlist for Cache Test%';

SELECT '========================================' as separator;
SELECT 'CSV Files Generated' as report_title;
SELECT '========================================' as separator;

SELECT 'Generated Files:' as info
UNION ALL SELECT '  - /tmp/playlist_ids.csv (100 playlists)'
UNION ALL SELECT '  - /tmp/user_tokens.csv (200 user tokens)'
UNION ALL SELECT ''
UNION ALL SELECT '다음 명령으로 파일을 이동하세요:'
UNION ALL SELECT '  mv /tmp/playlist_ids.csv load_test/sql/playlist_review/'
UNION ALL SELECT '  mv /tmp/user_tokens.csv load_test/sql/playlist_review/';

SELECT '========================================' as separator;
SELECT 'Next Steps' as report_title;
SELECT '========================================' as separator;

SELECT 'Instructions:' as info
UNION ALL SELECT '1. CSV 파일을 load_test/sql/playlist_review/ 디렉토리로 이동'
UNION ALL SELECT '2. user_tokens.csv에 실제 JWT 토큰 생성 (Spring Security 로그인 필요)'
UNION ALL SELECT '3. JMeter 테스트 실행: playlist_review_redis_cache_3m.jmx'
UNION ALL SELECT '4. Redis 캐시 히트율 모니터링'
UNION ALL SELECT '5. 응답 시간 측정 및 분석';
