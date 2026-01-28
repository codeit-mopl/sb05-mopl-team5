-- Subscription Race Condition Test Data Generation
-- Purpose: Generate test data for subscription race condition load testing
-- Execute: mysql -uroot -p mopl < load_test/sql/subscription/generate_subscription_test_data.sql 2>&1
--
-- Data Generation Plan:
-- • Playlists: 1,000개
-- • Users: 1,000명 (테스트용 전용)
-- • Initial Subscriptions: 각 Playlist당 0~50개 (다양한 분포)
-- • JWT Tokens: 1,000개 (각 사용자별)

SET AUTOCOMMIT = 0;
SET UNIQUE_CHECKS = 0;
SET FOREIGN_KEY_CHECKS = 0;

-- ==========================================
-- 1. Test Users 생성 (1,000명)
-- ==========================================
DROP PROCEDURE IF EXISTS generate_subscription_test_users;

DELIMITER $$
CREATE PROCEDURE generate_subscription_test_users()
BEGIN
    DECLARE i INT DEFAULT 0;
    DECLARE user_uuid BINARY(16);
    
    WHILE i < 1000 DO
        SET user_uuid = UNHEX(REPLACE(UUID(), '-', ''));
        
        INSERT INTO users (
            id, name, email, password, profile_image_url, role, locked, follower_count, created_at, updated_at
        ) VALUES (
            user_uuid,
            CONCAT('SubTestUser', i),
            CONCAT('subtest', i, '@mopl.test'),
            '$2a$10$dummyPasswordHashForSubscriptionTest',
            CONCAT('https://picsum.photos/seed/subuser', i, '/200/200'),
            'USER',
            FALSE,
            0,
            NOW(),
            NOW()
        );
        
        SET i = i + 1;
        
        IF i % 100 = 0 THEN
            COMMIT;
            SELECT CONCAT('Users Progress: ', i, ' / 1000') as status;
        END IF;
    END WHILE;
    
    COMMIT;
    
    SELECT COUNT(*) as total_test_users FROM users WHERE email LIKE 'subtest%@mopl.test';
END$$
DELIMITER ;

CALL generate_subscription_test_users();
DROP PROCEDURE IF EXISTS generate_subscription_test_users;

-- ==========================================
-- 2. Test Playlists 생성 (1,000개)
-- ==========================================
DROP PROCEDURE IF EXISTS generate_subscription_test_playlists;

DELIMITER $$
CREATE PROCEDURE generate_subscription_test_playlists()
BEGIN
    DECLARE i INT DEFAULT 0;
    DECLARE playlist_uuid BINARY(16);
    DECLARE random_user_id BINARY(16);
    DECLARE initial_subscriber_count BIGINT;
    DECLARE rand_val DOUBLE;
    
    WHILE i < 1000 DO
        SET playlist_uuid = UNHEX(REPLACE(UUID(), '-', ''));
        
        SELECT id INTO random_user_id FROM users WHERE email LIKE 'subtest%@mopl.test' ORDER BY RAND() LIMIT 1;
        
        SET rand_val = RAND();
        SET initial_subscriber_count = CASE
            WHEN rand_val < 0.3 THEN 0
            WHEN rand_val < 0.6 THEN FLOOR(RAND() * 10)
            WHEN rand_val < 0.9 THEN FLOOR(RAND() * 30)
            ELSE FLOOR(RAND() * 50)
        END;
        
        INSERT INTO playlists (
            id, user_id, title, description, subscriber_count, created_at, updated_at, is_deleted
        ) VALUES (
            playlist_uuid,
            random_user_id,
            CONCAT('Subscription Test Playlist ', i),
            CONCAT('This is a subscription race condition test playlist. ID: ', i),
            initial_subscriber_count,
            NOW(),
            NOW(),
            FALSE
        );
        
        SET i = i + 1;
        
        IF i % 100 = 0 THEN
            COMMIT;
            SELECT CONCAT('Playlists Progress: ', i, ' / 1000') as status;
        END IF;
    END WHILE;
    
    COMMIT;
    
    SELECT 
        COUNT(*) as total_test_playlists,
        AVG(subscriber_count) as avg_subscribers,
        MAX(subscriber_count) as max_subscribers
    FROM playlists 
    WHERE title LIKE 'Subscription Test Playlist%';
    
END$$
DELIMITER ;

CALL generate_subscription_test_playlists();
DROP PROCEDURE IF EXISTS generate_subscription_test_playlists;

-- ==========================================
-- 3. Initial Subscriptions 생성
-- ==========================================
DROP PROCEDURE IF EXISTS generate_initial_subscriptions;

DELIMITER $$
CREATE PROCEDURE generate_initial_subscriptions()
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE playlist_id_var BINARY(16);
    DECLARE target_count BIGINT;
    DECLARE i INT;
    DECLARE subscription_uuid BINARY(16);
    DECLARE random_user_id BINARY(16);
    DECLARE total_processed INT DEFAULT 0;
    
    DECLARE playlist_cursor CURSOR FOR 
        SELECT id, subscriber_count FROM playlists 
        WHERE title LIKE 'Subscription Test Playlist%' AND subscriber_count > 0;
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
            
            SELECT id INTO random_user_id FROM users 
            WHERE email LIKE 'subtest%@mopl.test' 
            ORDER BY RAND() LIMIT 1;
            
            INSERT IGNORE INTO subscriptions (id, user_id, playlist_id, created_at)
            VALUES (subscription_uuid, random_user_id, playlist_id_var, NOW());
            
            SET i = i + 1;
        END WHILE;
        
        SET total_processed = total_processed + 1;
        
        IF total_processed % 100 = 0 THEN
            COMMIT;
            SELECT CONCAT('Subscriptions Progress: ', total_processed, ' playlists processed') as status;
        END IF;
    END LOOP;
    
    CLOSE playlist_cursor;
    COMMIT;
    
    SELECT COUNT(*) as total_initial_subscriptions 
    FROM subscriptions s
    INNER JOIN playlists p ON s.playlist_id = p.id
    WHERE p.title LIKE 'Subscription Test Playlist%';
    
END$$
DELIMITER ;

CALL generate_initial_subscriptions();
DROP PROCEDURE IF EXISTS generate_initial_subscriptions;

-- ==========================================
-- 4. Export Playlist IDs to CSV (for JMeter)
-- ==========================================
SELECT 'playlist_id' as header
UNION ALL
SELECT LOWER(CONCAT(
    SUBSTR(HEX(id), 1, 8), '-',
    SUBSTR(HEX(id), 9, 4), '-',
    SUBSTR(HEX(id), 13, 4), '-',
    SUBSTR(HEX(id), 17, 4), '-',
    SUBSTR(HEX(id), 21, 12)
)) as playlist_id
FROM playlists
WHERE title LIKE 'Subscription Test Playlist%'
INTO OUTFILE '/tmp/subscription_playlist_ids.csv'
FIELDS TERMINATED BY ','
LINES TERMINATED BY '\n';

SELECT '✅ CSV file generated: /tmp/subscription_playlist_ids.csv' as result;
SELECT '⚠️  Copy this file to: load_test/data/generater/subscription_playlist_ids.csv' as instruction;

-- ==========================================
-- 5. Export User IDs and Tokens to CSV
-- ==========================================
SELECT 'user_id,access_token' as header
UNION ALL
SELECT CONCAT(
    LOWER(CONCAT(
        SUBSTR(HEX(id), 1, 8), '-',
        SUBSTR(HEX(id), 9, 4), '-',
        SUBSTR(HEX(id), 13, 4), '-',
        SUBSTR(HEX(id), 17, 4), '-',
        SUBSTR(HEX(id), 21, 12)
    )),
    ',',
    'dummy_jwt_token_for_user_', SUBSTR(email, 8, LOCATE('@', email) - 8)
) as user_data
FROM users
WHERE email LIKE 'subtest%@mopl.test'
INTO OUTFILE '/tmp/subscription_user_tokens.csv'
FIELDS TERMINATED BY ','
LINES TERMINATED BY '\n';

SELECT '✅ CSV file generated: /tmp/subscription_user_tokens.csv' as result;
SELECT '⚠️  Copy this file to: load_test/data/generater/subscription_user_tokens.csv' as instruction;
SELECT '⚠️  NOTE: JWT tokens are dummy values. For real testing, generate actual JWT tokens.' as warning;

SET FOREIGN_KEY_CHECKS = 1;
SET UNIQUE_CHECKS = 1;
SET AUTOCOMMIT = 1;

-- ==========================================
-- Final Statistics Report
-- ==========================================

SELECT '========================================' as separator;
SELECT 'Subscription Test Data Summary' as report_title;
SELECT '========================================' as separator;

SELECT 
    (SELECT COUNT(*) FROM users WHERE email LIKE 'subtest%@mopl.test') as test_users,
    (SELECT COUNT(*) FROM playlists WHERE title LIKE 'Subscription Test Playlist%') as test_playlists,
    (SELECT COUNT(*) FROM subscriptions s INNER JOIN playlists p ON s.playlist_id = p.id WHERE p.title LIKE 'Subscription Test Playlist%') as initial_subscriptions;

SELECT '========================================' as separator;
SELECT 'Subscriber Count Distribution' as report_title;
SELECT '========================================' as separator;

SELECT 
    CASE 
        WHEN subscriber_count = 0 THEN '0 subscribers'
        WHEN subscriber_count <= 10 THEN '1-10 subscribers'
        WHEN subscriber_count <= 30 THEN '11-30 subscribers'
        ELSE '31-50 subscribers'
    END as range,
    COUNT(*) as playlist_count
FROM playlists
WHERE title LIKE 'Subscription Test Playlist%'
GROUP BY 
    CASE 
        WHEN subscriber_count = 0 THEN '0 subscribers'
        WHEN subscriber_count <= 10 THEN '1-10 subscribers'
        WHEN subscriber_count <= 30 THEN '11-30 subscribers'
        ELSE '31-50 subscribers'
    END;

SELECT '========================================' as separator;
SELECT 'Data Generation Complete!' as result;
SELECT '========================================' as separator;
