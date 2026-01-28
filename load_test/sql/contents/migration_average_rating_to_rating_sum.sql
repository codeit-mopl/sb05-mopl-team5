-- ==========================================
-- 마이그레이션: average_rating → rating_sum
-- ==========================================
-- 목적: 기존 average_rating 데이터를 새로운 rating_sum 스키마로 변환
-- 변환 로직: rating_sum = ROUND(average_rating * review_count * 10)
-- 
-- 사용법:
-- mysql -uroot -p mopl < load_test/sql/contents/migration_average_rating_to_rating_sum.sql

-- ==========================================
-- 1. 백업 테이블 생성 (안전장치)
-- ==========================================
DROP TABLE IF EXISTS contents_backup_before_migration;

CREATE TABLE contents_backup_before_migration AS
SELECT * FROM contents;

SELECT CONCAT('Backup created: ', COUNT(*), ' rows') as backup_status
FROM contents_backup_before_migration;

-- ==========================================
-- 2. 컬럼 추가 (스키마가 이미 변경되지 않은 경우)
-- ==========================================
-- average_rating 컬럼이 존재하는 경우에만 실행
SET @column_exists = (
    SELECT COUNT(*) 
    FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() 
    AND TABLE_NAME = 'contents' 
    AND COLUMN_NAME = 'average_rating'
);

-- average_rating이 존재하면 새 컬럼 추가 (존재하지 않으면 스킵)
SET @add_rating_sum = IF(@column_exists > 0,
    'ALTER TABLE contents ADD COLUMN rating_sum BIGINT NOT NULL DEFAULT 0 AFTER tags',
    'SELECT "rating_sum column already exists" as status'
);

PREPARE stmt FROM @add_rating_sum;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ==========================================
-- 3. 데이터 마이그레이션
-- ==========================================
-- average_rating 컬럼이 존재하는 경우에만 실행
SET @migrate_data = IF(@column_exists > 0,
    'UPDATE contents 
     SET rating_sum = ROUND(average_rating * review_count * 10)
     WHERE review_count > 0',
    'SELECT "Migration skipped - average_rating column not found" as status'
);

PREPARE stmt FROM @migrate_data;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ==========================================
-- 4. 마이그레이션 결과 확인
-- ==========================================
SELECT 
    COUNT(*) as total_contents,
    COUNT(CASE WHEN rating_sum > 0 THEN 1 END) as contents_with_rating,
    COUNT(CASE WHEN review_count > 0 THEN 1 END) as contents_with_reviews,
    AVG(rating_sum / (review_count * 10)) as avg_calculated_rating
FROM contents
WHERE review_count > 0;

-- ==========================================
-- 5. 인덱스 업데이트
-- ==========================================
-- 기존 average_rating 인덱스 삭제
SET @drop_old_index = IF(@column_exists > 0,
    'DROP INDEX IF EXISTS idx_contents_rating_id ON contents',
    'SELECT "Old index already dropped" as status'
);

PREPARE stmt FROM @drop_old_index;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 새로운 rating_sum 인덱스 생성
DROP INDEX IF EXISTS idx_contents_rating_sum_id ON contents;

CREATE INDEX idx_contents_rating_sum_id 
ON contents(rating_sum DESC, review_count DESC, id DESC);

-- ==========================================
-- 6. 기존 average_rating 컬럼 삭제
-- ==========================================
-- 마이그레이션 완료 후 average_rating 컬럼 제거
SET @drop_column = IF(@column_exists > 0,
    'ALTER TABLE contents DROP COLUMN average_rating',
    'SELECT "average_rating column already removed" as status'
);

PREPARE stmt FROM @drop_column;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ==========================================
-- 7. 최종 검증
-- ==========================================
SELECT '=== Migration Complete ===' as status;

SELECT 
    TABLE_NAME,
    COLUMN_NAME,
    COLUMN_TYPE,
    COLUMN_DEFAULT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'contents'
  AND COLUMN_NAME IN ('rating_sum', 'review_count')
ORDER BY ORDINAL_POSITION;

-- 인덱스 확인
SELECT 
    INDEX_NAME,
    GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) as COLUMNS
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'contents'
  AND INDEX_NAME LIKE '%rating%'
GROUP BY INDEX_NAME;

-- ==========================================
-- 8. 백업 복원 방법 (필요 시)
-- ==========================================
-- 문제 발생 시 아래 명령으로 백업 복원:
-- 
-- DROP TABLE contents;
-- RENAME TABLE contents_backup_before_migration TO contents;
-- 
-- 백업 삭제 (마이그레이션 성공 확인 후):
-- DROP TABLE IF EXISTS contents_backup_before_migration;
