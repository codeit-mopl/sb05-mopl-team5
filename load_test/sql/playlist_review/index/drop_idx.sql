-- ==========================================
-- Playlist & Review 부하테스트 인덱스 제거
-- ==========================================
-- 목적: Baseline 성능 측정을 위한 인덱스 삭제
-- 사용법: mysql -uroot -p mopl < load_test/sql/playlist_review/index/drop_idx.sql

USE mopl;

-- ==========================================
-- 인덱스 삭제 전 존재 확인
-- ==========================================
SELECT '========================================' as divider;
SELECT 'Current Indexes (Before Drop)' as report_title;
SELECT '========================================' as divider;

SELECT 
    TABLE_NAME,
    INDEX_NAME,
    GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ', ') as COLUMNS
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME IN ('playlists', 'reviews', 'subscriptions')
  AND INDEX_NAME LIKE 'idx_%'
GROUP BY TABLE_NAME, INDEX_NAME
ORDER BY TABLE_NAME, INDEX_NAME;

-- ==========================================
-- 인덱스 삭제
-- ==========================================

-- 1. Subscriptions 인덱스 삭제
-- 주의: idx_subscriptions_user_playlist는 FK 제약조건 때문에 삭제 불가 (스킵)

-- 2. Playlists 인덱스 삭제 (3개) - 삭제 가능
DROP INDEX idx_playlists_updated_id ON playlists;
DROP INDEX idx_playlists_subscriber_id ON playlists;
DROP INDEX idx_playlists_deleted_created_id ON playlists;

-- 3. Reviews 인덱스 삭제 (2개)
-- 주의: FK 제약조건 때문에 삭제 불가 (스킵)
-- DROP INDEX idx_reviews_content_created ON reviews;
-- DROP INDEX idx_reviews_user_created ON reviews;

-- ==========================================
-- 인덱스 삭제 후 확인
-- ==========================================
SELECT '========================================' as divider;
SELECT 'Remaining Indexes (After Drop)' as report_title;
SELECT '========================================' as divider;

SELECT 
    TABLE_NAME,
    INDEX_NAME,
    GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ', ') as COLUMNS
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME IN ('playlists', 'reviews', 'subscriptions')
GROUP BY TABLE_NAME, INDEX_NAME
ORDER BY TABLE_NAME, INDEX_NAME;

SELECT '========================================' as divider;
SELECT '✅ Indexes dropped successfully! (Baseline mode)' as status;
SELECT '========================================' as divider;
