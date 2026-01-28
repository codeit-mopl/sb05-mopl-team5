-- ==========================================
-- Playlist & Review 테스트 데이터 초기화
-- ==========================================
-- 목적: 부하 테스트 데이터만 선택적으로 삭제
-- 사용법: mysql -uroot -p mopl < load_test/sql/playlist_review/reset_playlist_review_data.sql
-- 주의: Load Test로 생성된 데이터만 삭제됩니다.

SET FOREIGN_KEY_CHECKS = 0;
SET AUTOCOMMIT = 0;

-- ==========================================
-- 1. Subscriptions 삭제
-- ==========================================
DELETE FROM subscriptions 
WHERE playlist_id IN (SELECT id FROM playlists WHERE title LIKE 'Load Test Playlist%');

COMMIT;

SELECT CONCAT('✅ Deleted ', ROW_COUNT(), ' subscriptions') as status;

-- ==========================================
-- 2. Playlist Contents 삭제
-- ==========================================
DELETE FROM playlist_contents 
WHERE playlist_id IN (SELECT id FROM playlists WHERE title LIKE 'Load Test Playlist%');

COMMIT;

SELECT CONCAT('✅ Deleted ', ROW_COUNT(), ' playlist_contents') as status;

-- ==========================================
-- 3. Playlists 삭제
-- ==========================================
DELETE FROM playlists 
WHERE title LIKE 'Load Test Playlist%';

COMMIT;

SELECT CONCAT('✅ Deleted ', ROW_COUNT(), ' playlists') as status;

-- ==========================================
-- 4. Reviews 삭제
-- ==========================================
DELETE FROM reviews 
WHERE text LIKE 'This is a load test review%';

COMMIT;

SELECT CONCAT('✅ Deleted ', ROW_COUNT(), ' reviews') as status;

-- ==========================================
-- 5. Load Test Users 삭제 (옵션)
-- ==========================================
-- 주의: Load Test 사용자를 삭제하면 다른 데이터에 영향을 줄 수 있습니다.
-- 필요시 주석을 해제하여 사용하세요.

-- DELETE FROM social_accounts 
-- WHERE user_id IN (SELECT id FROM users WHERE email LIKE 'loadtest%@mopl.test');
-- COMMIT;

-- DELETE FROM users 
-- WHERE email LIKE 'loadtest%@mopl.test';
-- COMMIT;

-- SELECT CONCAT('✅ Deleted ', ROW_COUNT(), ' load test users') as status;

SET FOREIGN_KEY_CHECKS = 1;
SET AUTOCOMMIT = 1;

-- ==========================================
-- 초기화 완료 확인
-- ==========================================
SELECT '========================================' as divider;
SELECT 'Remaining Data Count' as report_title;
SELECT '========================================' as divider;

SELECT 
    (SELECT COUNT(*) FROM users) as total_users,
    (SELECT COUNT(*) FROM playlists) as total_playlists,
    (SELECT COUNT(*) FROM reviews) as total_reviews,
    (SELECT COUNT(*) FROM subscriptions) as total_subscriptions,
    (SELECT COUNT(*) FROM playlist_contents) as total_playlist_contents;

SELECT '========================================' as divider;
SELECT '✅ Playlist & Review test data reset successfully!' as status;
SELECT '========================================' as divider;
