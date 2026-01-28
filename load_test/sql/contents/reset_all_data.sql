-- ==========================================
-- MOPL 데이터베이스 전체 초기화
-- ==========================================
-- 주의: 모든 데이터가 삭제됩니다!
-- 사용법: mysql -uroot -p mopl < reset_all_data.sql

SET FOREIGN_KEY_CHECKS = 0;
SET AUTOCOMMIT = 0;

-- 모든 테이블 데이터 삭제 (외래 키 순서 고려)
TRUNCATE TABLE `direct_messages`;
TRUNCATE TABLE `conversation_participants`;
TRUNCATE TABLE `conversations`;
TRUNCATE TABLE `follows`;
TRUNCATE TABLE `subscriptions`;
TRUNCATE TABLE `watching_sessions`;
TRUNCATE TABLE `playlist_contents`;
TRUNCATE TABLE `playlists`;
TRUNCATE TABLE `social_accounts`;
TRUNCATE TABLE `notifications`;
TRUNCATE TABLE `reviews`;
TRUNCATE TABLE `contents`;
TRUNCATE TABLE `users`;

COMMIT;
SET FOREIGN_KEY_CHECKS = 1;
SET AUTOCOMMIT = 1;

-- 초기화 완료 확인
SELECT 
    'users' as table_name, COUNT(*) as count FROM users
UNION ALL
SELECT 'contents', COUNT(*) FROM contents
UNION ALL
SELECT 'reviews', COUNT(*) FROM reviews
UNION ALL
SELECT 'playlists', COUNT(*) FROM playlists
UNION ALL
SELECT 'notifications', COUNT(*) FROM notifications
UNION ALL
SELECT 'follows', COUNT(*) FROM follows;

SELECT '✅ All data has been reset successfully!' as status;
