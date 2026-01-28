-- Subscriber Count Consistency Validation
-- Purpose: Verify that subscriber_count matches actual subscription count
-- Execute: mysql -uroot -p mopl < load_test/sql/subscription/validate_subscriber_count.sql 2>&1

SELECT '========================================' as separator;
SELECT 'Subscriber Count Consistency Check' as report_title;
SELECT '========================================' as separator;

-- Check for inconsistencies in subscription test playlists
SELECT 
    p.id,
    LOWER(CONCAT(
        SUBSTR(HEX(p.id), 1, 8), '-',
        SUBSTR(HEX(p.id), 9, 4), '-',
        SUBSTR(HEX(p.id), 13, 4), '-',
        SUBSTR(HEX(p.id), 17, 4), '-',
        SUBSTR(HEX(p.id), 21, 12)
    )) as playlist_id,
    p.title,
    p.subscriber_count as stored_count,
    COUNT(s.id) as actual_count,
    (p.subscriber_count - COUNT(s.id)) as difference
FROM playlists p
LEFT JOIN subscriptions s ON p.id = s.playlist_id
WHERE p.title LIKE 'Subscription Test Playlist%'
GROUP BY p.id, p.title, p.subscriber_count
HAVING p.subscriber_count != COUNT(s.id)
ORDER BY ABS(p.subscriber_count - COUNT(s.id)) DESC
LIMIT 100;

-- Summary statistics
SELECT '========================================' as separator;
SELECT 'Inconsistency Summary' as report_title;
SELECT '========================================' as separator;

SELECT 
    COUNT(*) as total_playlists,
    SUM(CASE WHEN p.subscriber_count = actual_count THEN 1 ELSE 0 END) as consistent_playlists,
    SUM(CASE WHEN p.subscriber_count != actual_count THEN 1 ELSE 0 END) as inconsistent_playlists,
    ROUND(SUM(CASE WHEN p.subscriber_count = actual_count THEN 1 ELSE 0 END) / COUNT(*) * 100, 2) as consistency_percentage
FROM (
    SELECT 
        p.id,
        p.subscriber_count,
        COUNT(s.id) as actual_count
    FROM playlists p
    LEFT JOIN subscriptions s ON p.id = s.playlist_id
    WHERE p.title LIKE 'Subscription Test Playlist%'
    GROUP BY p.id, p.subscriber_count
) as p;

-- Top 10 worst inconsistencies
SELECT '========================================' as separator;
SELECT 'Top 10 Worst Inconsistencies' as report_title;
SELECT '========================================' as separator;

SELECT 
    LOWER(CONCAT(
        SUBSTR(HEX(p.id), 1, 8), '-',
        SUBSTR(HEX(p.id), 9, 4), '-',
        SUBSTR(HEX(p.id), 13, 4), '-',
        SUBSTR(HEX(p.id), 17, 4), '-',
        SUBSTR(HEX(p.id), 21, 12)
    )) as playlist_id,
    p.subscriber_count as stored_count,
    COUNT(s.id) as actual_count,
    (p.subscriber_count - COUNT(s.id)) as difference,
    CASE 
        WHEN p.subscriber_count > COUNT(s.id) THEN 'OVER-COUNTED'
        ELSE 'UNDER-COUNTED'
    END as issue_type
FROM playlists p
LEFT JOIN subscriptions s ON p.id = s.playlist_id
WHERE p.title LIKE 'Subscription Test Playlist%'
GROUP BY p.id, p.subscriber_count
HAVING p.subscriber_count != COUNT(s.id)
ORDER BY ABS(p.subscriber_count - COUNT(s.id)) DESC
LIMIT 10;

SELECT '========================================' as separator;
SELECT 'Validation Complete!' as result;
SELECT '========================================' as separator;
