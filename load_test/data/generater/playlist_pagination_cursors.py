#!/usr/bin/env python3
"""
Playlist Deep Pagination Cursor Generator - 균등 분포
목적: Playlist 페이지네이션 성능 테스트를 위한 cursor 생성
정렬 기준: updatedAt, subscriberCount, createdAt
"""

import mysql.connector
import csv
from datetime import datetime, timedelta
import random
from typing import Optional, List, Tuple

config = {
    'host': 'localhost',
    'port': 3306,
    'user': 'root',
    'password': 'your_password',
    'database': 'mopl'
}

def generate_playlist_cursors(sort_by: str = 'updated_at') -> None:
    """
    Playlist cursor 생성 (3가지 정렬 기준)
    - Recent Pages (0-30일): 40%
    - Middle Pages (30-180일): 30%
    - Deep Pages (180일+): 30%
    
    Args:
        sort_by: 'updated_at', 'created_at', 'subscriber_count'
    """
    
    conn = mysql.connector.connect(**config)
    cur = conn.cursor()
    
    sort_column_map = {
        'updated_at': 'updated_at',
        'created_at': 'created_at',
        'subscriber_count': 'subscriber_count'
    }
    
    sort_column = sort_column_map.get(sort_by, 'updated_at')
    
    print(f"\n=== Playlist Cursor Generation (Sort by: {sort_by}) ===")
    
    if sort_by in ['updated_at', 'created_at']:
        cur.execute(f"SELECT MIN({sort_column}), MAX({sort_column}), COUNT(*) FROM playlists WHERE is_deleted = FALSE")
        result = cur.fetchone()
        
        if not result or result[0] is None or result[1] is None:
            print(f"❌ 에러: playlists 테이블에 데이터가 없습니다.")
            cur.close()
            conn.close()
            return
        
        min_value_dt: datetime = result[0]  
        max_value_dt: datetime = result[1]
        total_count: int = result[2]
        
        print(f"데이터 범위: {min_value_dt} ~ {max_value_dt}")
        print(f"총 레코드: {total_count:,}개")
        
        date_range = (max_value_dt - min_value_dt).days
        print(f"기간: {date_range}일\n")
        
        cursors: List[str] = []
        
        print("Recent Pages (0-30일) 생성 중...")
        recent_date = max_value_dt - timedelta(days=30)
        query = f"""
            SELECT DATE_FORMAT({sort_column}, '%Y-%m-%dT%H:%i:%s'), HEX(id)
            FROM playlists
            WHERE {sort_column} >= %s AND is_deleted = FALSE
            ORDER BY RAND()
            LIMIT 1200
        """
        cur.execute(query, (recent_date,))
        
        for value, hex_id in cur.fetchall():
            cursors.append(f"{value}_{hex_id}")
        
        print(f"  생성: {len(cursors)}개")
        
        print("Middle Pages (30-180일) 생성 중...")
        middle_start = max_value_dt - timedelta(days=180)
        middle_end = max_value_dt - timedelta(days=30)
        query = f"""
            SELECT DATE_FORMAT({sort_column}, '%Y-%m-%dT%H:%i:%s'), HEX(id)
            FROM playlists
            WHERE {sort_column} >= %s AND {sort_column} < %s AND is_deleted = FALSE
            ORDER BY RAND()
            LIMIT 900
        """
        cur.execute(query, (middle_start, middle_end))
        
        middle_count = 0
        for value, hex_id in cur.fetchall():
            cursors.append(f"{value}_{hex_id}")
            middle_count += 1
        
        print(f"  생성: {middle_count}개")
        
        print("Deep Pages (180일+) 생성 중...")
        deep_end = max_value_dt - timedelta(days=180)
        query = f"""
            SELECT DATE_FORMAT({sort_column}, '%Y-%m-%dT%H:%i:%s'), HEX(id)
            FROM playlists
            WHERE {sort_column} < %s AND is_deleted = FALSE
            ORDER BY RAND()
            LIMIT 900
        """
        cur.execute(query, (deep_end,))
        
        deep_count = 0
        for value, hex_id in cur.fetchall():
            cursors.append(f"{value}_{hex_id}")
            deep_count += 1
        
        print(f"  생성: {deep_count}개")
        
    else:
        cur.execute(f"SELECT MIN({sort_column}), MAX({sort_column}), COUNT(*) FROM playlists WHERE is_deleted = FALSE")
        result = cur.fetchone()
        
        if not result or result[2] == 0:
            print(f"❌ 에러: playlists 테이블에 데이터가 없습니다.")
            cur.close()
            conn.close()
            return
            
        min_value: int = result[0] or 0
        max_value: int = result[1] or 0
        total_count: int = result[2]
        
        print(f"구독자 수 범위: {min_value} ~ {max_value}")
        print(f"총 레코드: {total_count:,}개\n")
        
        cursors: List[str] = []
        
        print("High Subscriber Playlists (Top 40%) 생성 중...")
        limit_high = int(total_count * 0.4)
        query = f"""
            SELECT {sort_column}, HEX(id)
            FROM playlists
            WHERE is_deleted = FALSE
            ORDER BY {sort_column} DESC, id DESC
            LIMIT %s
        """
        cur.execute(query, (limit_high,))
        high_sub = [(count, hex_id) for count, hex_id in cur.fetchall()]
        random.shuffle(high_sub)
        high_sub = high_sub[:1200]
        
        for count, hex_id in high_sub:
            cursors.append(f"{count}_{hex_id}")
        
        print(f"  생성: {len(cursors)}개")
        
        print("Medium Subscriber Playlists (Middle 30%) 생성 중...")
        limit_medium = int(total_count * 0.3)
        offset_medium = int(total_count * 0.4)
        query = f"""
            SELECT {sort_column}, HEX(id)
            FROM playlists
            WHERE is_deleted = FALSE
            ORDER BY {sort_column} DESC, id DESC
            LIMIT %s OFFSET %s
        """
        cur.execute(query, (limit_medium, offset_medium))
        medium_sub = [(count, hex_id) for count, hex_id in cur.fetchall()]
        random.shuffle(medium_sub)
        medium_sub = medium_sub[:900]
        
        medium_count = 0
        for count, hex_id in medium_sub:
            cursors.append(f"{count}_{hex_id}")
            medium_count += 1
        
        print(f"  생성: {medium_count}개")
        
        print("Low Subscriber Playlists (Bottom 30%) 생성 중...")
        limit_low = int(total_count * 0.3)
        query = f"""
            SELECT {sort_column}, HEX(id)
            FROM playlists
            WHERE is_deleted = FALSE
            ORDER BY {sort_column} ASC, id ASC
            LIMIT %s
        """
        cur.execute(query, (limit_low,))
        low_sub = [(count, hex_id) for count, hex_id in cur.fetchall()]
        random.shuffle(low_sub)
        low_sub = low_sub[:900]
        
        deep_count = 0
        for count, hex_id in low_sub:
            cursors.append(f"{count}_{hex_id}")
            deep_count += 1
        
        print(f"  생성: {deep_count}개")
    
    cur.close()
    conn.close()
    
    random.shuffle(cursors)
    
    output_file = f'playlist_pagination_cursors_{sort_by}.csv'
    with open(output_file, 'w', newline='') as f:
        writer = csv.writer(f)
        writer.writerow(['cursor'])
        for c in cursors:
            writer.writerow([c])
    
    print(f"\n총 생성: {len(cursors):,}개")
    print(f"저장: {output_file}\n")

def main() -> None:
    """3가지 정렬 기준으로 cursor 생성"""
    
    print("=" * 60)
    print("Playlist Pagination Cursor Generator")
    print("=" * 60)
    
    try:
        generate_playlist_cursors('updated_at')
        generate_playlist_cursors('created_at')
        generate_playlist_cursors('subscriber_count')
        
        print("=" * 60)
        print("✅ 모든 커서 파일 생성 완료!")
        print("=" * 60)
        print("\n생성된 파일:")
        print("  - playlist_pagination_cursors_updated_at.csv")
        print("  - playlist_pagination_cursors_created_at.csv")
        print("  - playlist_pagination_cursors_subscriber_count.csv")
        
    except mysql.connector.Error as err:
        print(f"\n❌ MySQL 에러: {err}")
        print("\n설정을 확인하세요:")
        print("  - host, port, user, password, database")
        print("  - MySQL 서버 실행 여부")
        print("  - 데이터베이스 및 테이블 존재 여부")

if __name__ == '__main__':
    main()
