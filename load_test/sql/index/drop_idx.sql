-- ============================================================
-- Baseline 테스트용 인덱스 제거 (FK 인덱스 대체 전략)
-- ============================================================
-- 전략:
-- 1. FK가 복합 인덱스에 의존하는 경우 → 단일 컬럼 인덱스 생성 후 복합 인덱스 삭제
-- 2. FK가 없는 인덱스 → 바로 삭제
-- ============================================================

USE mopl;
-- Contents 테이블 최적화 인덱스 삭제 (Deep Pagination 테스트용)

ALTER TABLE contents DROP INDEX idx_contents_created_id;
ALTER TABLE contents DROP INDEX idx_contents_rating_id;
ALTER TABLE contents DROP INDEX idx_contents_watcher_id;

-- ============================================================
-- 결과 확인
-- ============================================================
SELECT '=== Remaining Indexes (Baseline) ===' as Status;
SELECT
    table_name,
    index_name,
    GROUP_CONCAT(column_name ORDER BY seq_in_index SEPARATOR ', ') AS columns
FROM information_schema.statistics
WHERE table_schema = 'mopl'
AND table_name IN ('contents', 'reviews', 'playlists', 'playlist_contents', 'follows', 'subscriptions', 'notifications', 'users')
GROUP BY table_name, index_name
ORDER BY table_name, index_name;
