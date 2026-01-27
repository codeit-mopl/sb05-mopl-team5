-- ==========================================
-- Playlist & Review 부하테스트 최적화 인덱스 생성
-- ==========================================
-- 목적: 커서 기반 Deep Pagination 최적화
-- 사용법: mysql -uroot -p mopl < load_test/sql/playlist_review/index/create_idx.sql

USE mopl;

-- ==========================================
-- 1. Subscriptions 복합 인덱스
-- ==========================================
-- 목적: 사용자의 특정 플레이리스트 구독 여부 확인 최적화
-- 쿼리: SELECT * FROM subscriptions WHERE user_id = ? AND playlist_id = ?
CREATE INDEX idx_subscriptions_user_playlist 
ON subscriptions(user_id, playlist_id);

-- ==========================================
-- 2. Playlists - 최신 업데이트 정렬 인덱스
-- ==========================================
-- 목적: updated_at 기준 커서 페이지네이션
-- 쿼리: WHERE is_deleted = false AND (updated_at < ? OR (updated_at = ? AND id < ?))
--       ORDER BY updated_at DESC, id DESC
CREATE INDEX idx_playlists_updated_id 
ON playlists(is_deleted, updated_at DESC, id DESC);

-- ==========================================
-- 3. Playlists - 인기순 정렬 인덱스
-- ==========================================
-- 목적: subscriber_count 기준 커서 페이지네이션
-- 쿼리: WHERE is_deleted = false AND (subscriber_count < ? OR (subscriber_count = ? AND id < ?))
--       ORDER BY subscriber_count DESC, id DESC
CREATE INDEX idx_playlists_subscriber_id 
ON playlists(is_deleted, subscriber_count DESC, id DESC);

-- ==========================================
-- 4. Playlists - 생성일 정렬 인덱스
-- ==========================================
-- 목적: created_at 기준 커서 페이지네이션
-- 쿼리: WHERE is_deleted = false AND (created_at < ? OR (created_at = ? AND id < ?))
--       ORDER BY created_at DESC, id DESC
CREATE INDEX idx_playlists_deleted_created_id 
ON playlists(is_deleted, created_at DESC, id DESC);

-- ==========================================
-- 5. Reviews - Content별 리뷰 조회 인덱스
-- ==========================================
-- 목적: 특정 콘텐츠의 리뷰 목록 조회 최적화
-- 쿼리: WHERE content_id = ? AND is_deleted = false ORDER BY created_at DESC
CREATE INDEX idx_reviews_content_created 
ON reviews(content_id, created_at DESC, is_deleted);

-- ==========================================
-- 6. Reviews - User별 리뷰 조회 인덱스
-- ==========================================
-- 목적: 특정 사용자의 리뷰 목록 조회 최적화
-- 쿼리: WHERE user_id = ? AND is_deleted = false ORDER BY created_at DESC
CREATE INDEX idx_reviews_user_created 
ON reviews(user_id, created_at DESC, is_deleted);

-- ==========================================
-- 인덱스 생성 확인
-- ==========================================
SELECT '========================================' as separator;
SELECT 'Created Indexes for Playlist & Review' as report_title;
SELECT '========================================' as separator;

SELECT 
    TABLE_NAME,
    INDEX_NAME,
    GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ', ') as COLUMNS,
    INDEX_TYPE,
    CASE WHEN NON_UNIQUE = 0 THEN 'UNIQUE' ELSE 'NON-UNIQUE' END as UNIQUENESS
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME IN ('playlists', 'reviews', 'subscriptions')
  AND INDEX_NAME LIKE 'idx_%'
GROUP BY TABLE_NAME, INDEX_NAME, INDEX_TYPE, NON_UNIQUE
ORDER BY TABLE_NAME, INDEX_NAME;

SELECT '========================================' as separator;
SELECT '✅ Indexes created successfully!' as status;
SELECT '========================================' as separator;

-- ==========================================
-- 인덱스 크기 확인
-- ==========================================
SELECT 
    TABLE_NAME,
    INDEX_NAME,
    ROUND(STAT_VALUE * @@innodb_page_size / 1024 / 1024, 2) as SIZE_MB
FROM mysql.innodb_index_stats
WHERE DATABASE_NAME = DATABASE()
  AND TABLE_NAME IN ('playlists', 'reviews', 'subscriptions')
  AND INDEX_NAME LIKE 'idx_%'
  AND STAT_NAME = 'size'
ORDER BY TABLE_NAME, INDEX_NAME;
