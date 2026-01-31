#!/usr/bin/env python3
"""
Subscription Test - Playlist IDs Generator
목적: Subscription race condition 테스트를 위한 playlist ID CSV 생성
"""

import mysql.connector
import csv
import sys

config = {
    'host': 'localhost',
    'port': 3306,
    'user': 'root',
    'password': '12345678',
    'database': 'mopl'
}

def generate_subscription_playlist_ids(output_file: str = '../../jmeter/subscription/subscription_playlist_ids.csv', count: int = 1000):
    """
    Subscription 테스트용 플레이리스트 ID를 추출하여 CSV로 저장
    
    Args:
        output_file: 출력 CSV 파일명
        count: 추출할 플레이리스트 수 (기본: 1000)
    """
    try:
        conn = mysql.connector.connect(**config)
        cursor = conn.cursor()
        
        print(f"Subscription 테스트용 플레이리스트 ID {count}개 추출 중...")
        
        query = """
            SELECT LOWER(CONCAT(
                SUBSTR(HEX(id), 1, 8), '-',
                SUBSTR(HEX(id), 9, 4), '-',
                SUBSTR(HEX(id), 13, 4), '-',
                SUBSTR(HEX(id), 17, 4), '-',
                SUBSTR(HEX(id), 21, 12)
            )) as playlist_id
            FROM playlists
            WHERE title LIKE 'Subscription Test Playlist%'
            AND is_deleted = FALSE
            ORDER BY subscriber_count DESC, created_at DESC
            LIMIT %s
        """
        cursor.execute(query, (count,))
        
        playlist_ids = [row[0] for row in cursor.fetchall()]
        
        cursor.close()
        conn.close()
        
        if not playlist_ids:
            print("❌ 에러: Subscription Test Playlist를 찾을 수 없습니다.")
            print("⚠️  먼저 다음 SQL을 실행하세요:")
            print("    mysql -uroot -p mopl < load_test/sql/subscription/generate_subscription_test_data.sql")
            sys.exit(1)
        
        with open(output_file, 'w', newline='') as f:
            writer = csv.writer(f)
            writer.writerow(['playlist_id'])
            for pid in playlist_ids:
                writer.writerow([pid])
        
        print(f"✅ {len(playlist_ids)}개 플레이리스트 ID 저장 완료!")
        print(f"📁 파일: {output_file}")
        
        print("\n📊 통계:")
        conn = mysql.connector.connect(**config)
        cursor = conn.cursor()
        cursor.execute("""
            SELECT 
                COUNT(*) as total,
                AVG(subscriber_count) as avg_subs,
                MIN(subscriber_count) as min_subs,
                MAX(subscriber_count) as max_subs
            FROM playlists
            WHERE title LIKE 'Subscription Test Playlist%'
            AND is_deleted = FALSE
        """)
        total, avg_subs, min_subs, max_subs = cursor.fetchone()
        print(f"  전체 플레이리스트: {total}개")
        print(f"  평균 구독자 수: {avg_subs:.1f}")
        print(f"  구독자 범위: {min_subs} ~ {max_subs}")
        
        cursor.close()
        conn.close()
        
    except mysql.connector.Error as err:
        print(f"\n❌ MySQL 에러: {err}")
        print("\n설정을 확인하세요:")
        print("  - host, port, user, password, database")
        print("  - MySQL 서버 실행 여부")
        print("  - 데이터베이스 및 테이블 존재 여부")
        sys.exit(1)

if __name__ == '__main__':
    import argparse
    
    parser = argparse.ArgumentParser(description='Generate subscription test playlist IDs CSV')
    parser.add_argument('--output', default='../../jmeter/subscription/subscription_playlist_ids.csv', help='Output CSV file path')
    parser.add_argument('--count', type=int, default=1000, help='Number of playlist IDs to generate')
    
    args = parser.parse_args()
    
    generate_subscription_playlist_ids(args.output, args.count)
