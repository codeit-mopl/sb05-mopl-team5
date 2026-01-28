-- Reset Subscription Test Data
-- Purpose: Clean up all subscription test data after testing
-- Execute: mysql -uroot -p mopl < load_test/sql/subscription/reset_subscription_data.sql 2>&1

SET AUTOCOMMIT = 0;
SET FOREIGN_KEY_CHECKS = 0;

-- ==========================================
-- 1. Delete Test Subscriptions
-- ==========================================
DELETE s FROM subscriptions s
INNER JOIN playlists p ON s.playlist_id = p.id
WHERE p.title LIKE 'Subscription Test Playlist%';

SELECT CONCAT('✅ Deleted test subscriptions: ', ROW_COUNT(), ' rows') as result;

-- ==========================================
-- 2. Delete Test Playlists
-- ==========================================
DELETE FROM playlists
WHERE title LIKE 'Subscription Test Playlist%';

SELECT CONCAT('✅ Deleted test playlists: ', ROW_COUNT(), ' rows') as result;

-- ==========================================
-- 3. Delete Test Users
-- ==========================================
DELETE FROM users
WHERE email LIKE 'subtest%@mopl.test';

SELECT CONCAT('✅ Deleted test users: ', ROW_COUNT(), ' rows') as result;

COMMIT;
SET FOREIGN_KEY_CHECKS = 1;
SET AUTOCOMMIT = 1;

-- ==========================================
-- Final Verification
-- ==========================================
SELECT '========================================' as separator;
SELECT 'Cleanup Verification' as report_title;
SELECT '========================================' as separator;

SELECT 
    (SELECT COUNT(*) FROM users WHERE email LIKE 'subtest%@mopl.test') as remaining_test_users,
    (SELECT COUNT(*) FROM playlists WHERE title LIKE 'Subscription Test Playlist%') as remaining_test_playlists,
    (SELECT COUNT(*) FROM subscriptions s INNER JOIN playlists p ON s.playlist_id = p.id WHERE p.title LIKE 'Subscription Test Playlist%') as remaining_test_subscriptions;

SELECT '✅ Cleanup Complete!' as result;
