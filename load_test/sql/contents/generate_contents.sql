-- 균등 분포 과거 데이터 생성 (Deep Pagination 테스트용)
-- 목적: 최신 데이터 편향을 제거하고 전체 기간에 걸쳐 균등 분포 생성
# mysql -uroot -p mopl < load_test/sql/generate/generate_contents.sql 2>&1
#
# 결과:
#
# • 총 340,000건
# • 기간: 2023-01-25 ~ 2026-01-23 (약 3년)
# • 월평균 약 9,000건 (균등 분포)
# • 일평균 약 311건

SET AUTOCOMMIT = 0;
SET UNIQUE_CHECKS = 0;
SET FOREIGN_KEY_CHECKS = 0;

DROP PROCEDURE IF EXISTS generate_contents;

DELIMITER $$
CREATE PROCEDURE generate_contents()
BEGIN
    DECLARE i INT DEFAULT 0;
    DECLARE content_uuid BINARY(16);
    DECLARE content_type_val VARCHAR(20);
    DECLARE api_id_val BIGINT;
    DECLARE random_days INT;
    DECLARE created_date DATETIME;
    
    -- 데이터 생성 설정
    -- 총 생성: 340,000 건
    -- 기간: 3년 (1095일)
    -- 분포: 균등 (최신/과거 데이터 편향 제거)
    
    WHILE i < 340000 DO
        SET content_uuid = UNHEX(REPLACE(UUID(), '-', ''));
        SET content_type_val = CASE MOD(i, 3)
            WHEN 0 THEN 'MOVIE'
            WHEN 1 THEN 'TV_SERIES'
            ELSE 'SPORT'
        END;
        SET api_id_val = CASE MOD(i, 3)
            WHEN 0 THEN 3000000 + i
            WHEN 1 THEN 4000000 + i
            ELSE 5000000 + i
        END;
        
        -- 균등 분포 날짜 생성
        -- RAND() * 1095 = 0일 ~ 1095일 (3년) 균등 분포
        SET random_days = FLOOR(RAND() * 1095);
        SET created_date = DATE_SUB(NOW(), INTERVAL random_days DAY);
        
        INSERT INTO contents (
            id, type, api_id, title, description, thumbnail_url, 
            tags, average_rating, watcher_count, review_count, 
            created_at, updated_at, is_deleted
        ) VALUES (
            content_uuid,
            content_type_val,
            api_id_val,
            CONCAT('Content Title ', api_id_val),
            CONCAT('Description for content ', api_id_val, '. This is a sample description for load testing.'),
            CONCAT('https://picsum.photos/seed/', api_id_val, '/300/450'),
            CONCAT('tag', MOD(i, 10), ',tag', MOD(i, 20), ',tag', MOD(i, 30)),
            ROUND(RAND() * 5, 1),
            FLOOR(RAND() * 1000),
            FLOOR(RAND() * 10000),
            created_date,  -- V3: 균등 분포 날짜
            NOW(),
            FALSE
        );
        
        SET i = i + 1;
        
        IF i % 5000 = 0 THEN
            COMMIT;
            -- 진행 상황 표시 (옵션)
            SELECT CONCAT('Progress: ', i, ' / 340000 (', ROUND(i/340000*100, 1), '%)') as status;
        END IF;
    END WHILE;
    
    COMMIT;
    
    -- 최종 통계 출력
    SELECT 
        COUNT(*) as total_contents,
        MIN(created_at) as oldest_date,
        MAX(created_at) as newest_date,
        DATEDIFF(MAX(created_at), MIN(created_at)) as days_span
    FROM contents;
    
END$$
DELIMITER ;

CALL generate_contents();
DROP PROCEDURE IF EXISTS generate_contents;

SET FOREIGN_KEY_CHECKS = 1;
SET UNIQUE_CHECKS = 1;
SET AUTOCOMMIT = 1;

-- 날짜별 데이터 분포 확인
SELECT 
    DATE_FORMAT(created_at, '%Y-%m') as month,
    COUNT(*) as count
FROM contents
GROUP BY DATE_FORMAT(created_at, '%Y-%m')
ORDER BY month;

-- 최종 요약
SELECT 
    COUNT(*) as total_contents,
    MIN(created_at) as oldest_date,
    MAX(created_at) as newest_date,
    DATEDIFF(MAX(created_at), MIN(created_at)) as days_span,
    COUNT(*) / DATEDIFF(MAX(created_at), MIN(created_at)) as avg_per_day
FROM contents;
