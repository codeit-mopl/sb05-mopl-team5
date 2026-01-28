-- ==========================================
-- 부하테스트 최적화를 위한 인덱스 추가 스크립트
-- ==========================================

-- 1) Contents Deep Pagination 최적화
-- 커서 기반 페이지네이션 + created_at 정렬
CREATE INDEX idx_contents_created_id 
ON contents(created_at DESC, id DESC);

-- 2) Contents 평점 기준 정렬 최적화
-- rating_sum, review_count, id 순서로 인덱스 생성 (계산 컬럼 정렬 지원)
CREATE INDEX idx_contents_rating_id
ON contents(rating_sum DESC, review_count DESC, id DESC);

-- 3) Contents 시청자 수 기준 정렬 최적화
CREATE INDEX idx_contents_watcher_id 
ON contents(watcher_count DESC, id DESC);


-- ==========================================
-- 인덱스 생성 확인
-- ==========================================
SELECT 
    TABLE_NAME,
    INDEX_NAME,
    GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) as COLUMNS,
    INDEX_TYPE,
    NON_UNIQUE
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND INDEX_NAME LIKE 'idx_%'
GROUP BY TABLE_NAME, INDEX_NAME, INDEX_TYPE, NON_UNIQUE
ORDER BY TABLE_NAME, INDEX_NAME;

-- ==========================================
-- 인덱스 사용률 확인 (테스트 후 실행)
-- ==========================================
-- SHOW INDEX FROM contents;

-- ==========================================
-- Slow Query 확인
-- ==========================================
-- SET GLOBAL slow_query_log = 'ON';
-- SET GLOBAL long_query_time = 0.5;
-- SET GLOBAL log_queries_not_using_indexes = 'ON';
