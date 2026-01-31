#!/usr/bin/env python3
"""
Deep Pagination Cursor Generator - 균등 분포
목적: 최신 데이터 편향 제거, 전체 기간에 걸쳐 균등하게 cursor 생성
"""

import mysql.connector
import csv
from datetime import datetime, timedelta
import random

# MySQL 연결 설정
config = {
    'host': 'localhost',
    'port': 3306,
    'user': 'root',
    'password': '',
    'database': 'mopl'
}

def generate_cursors_uniform_distribution():
    """
    전체 데이터 범위에서 균등 분포로 cursor 생성
    - Recent Pages (0-30일): 40%
    - Middle Pages (30-180일): 30%
    - Deep Pages (180일+): 30%
    """
    
    conn = mysql.connector.connect(**config)
    cursor = conn.cursor()
    
    # 1. 데이터 범위 확인
    cursor.execute("SELECT MIN(created_at), MAX(created_at), COUNT(*) FROM contents")
    min_date, max_date, total_count = cursor.fetchone()
    
    print(f"데이터 범위: {min_date} ~ {max_date}")
    print(f"총 레코드: {total_count:,}개")
    
    date_range = (max_date - min_date).days
    print(f"기간: {date_range}일\n")
    
    cursors = []
    
    # 2. Recent Pages (0-30일): 40% = 1,786개
    print("Recent Pages (0-30일) 생성 중...")
    recent_date = max_date - timedelta(days=30)
    query = """
        SELECT DATE_FORMAT(created_at, '%Y-%m-%dT%H:%i:%s'), HEX(id)
        FROM contents
        WHERE created_at >= '{}'
        ORDER BY RAND()
        LIMIT 1786
    """.format(recent_date)
    cursor.execute(query)
    
    for created_at, hex_id in cursor.fetchall():
        cursors.append(f"{created_at}_{hex_id}")
    
    print(f"  생성: {len(cursors)}개")
    
    # 3. Middle Pages (30-180일): 30% = 1,340개
    print("Middle Pages (30-180일) 생성 중...")
    middle_start = max_date - timedelta(days=180)
    middle_end = max_date - timedelta(days=30)
    query = """
        SELECT DATE_FORMAT(created_at, '%Y-%m-%dT%H:%i:%s'), HEX(id)
        FROM contents
        WHERE created_at >= '{}' AND created_at < '{}'
        ORDER BY RAND()
        LIMIT 1340
    """.format(middle_start, middle_end)
    cursor.execute(query)
    
    middle_count = 0
    for created_at, hex_id in cursor.fetchall():
        cursors.append(f"{created_at}_{hex_id}")
        middle_count += 1
    
    print(f"  생성: {middle_count}개")
    
    # 4. Deep Pages (180일+): 30% = 1,340개
    print("Deep Pages (180일+) 생성 중...")
    deep_end = max_date - timedelta(days=180)
    query = """
        SELECT DATE_FORMAT(created_at, '%Y-%m-%dT%H:%i:%s'), HEX(id)
        FROM contents
        WHERE created_at < '{}'
        ORDER BY RAND()
        LIMIT 1340
    """.format(deep_end)
    cursor.execute(query)
    
    deep_count = 0
    for created_at, hex_id in cursor.fetchall():
        cursors.append(f"{created_at}_{hex_id}")
        deep_count += 1
    
    print(f"  생성: {deep_count}개")
    
    cursor.close()
    conn.close()
    
    # 5. Shuffle (순서 랜덤화)
    random.shuffle(cursors)
    
    # 6. CSV 저장
    output_file = '../../jmeter/cursor/pagination_cursors.csv'
    with open(output_file, 'w', newline='') as f:
        writer = csv.writer(f)
        writer.writerow(['cursor'])  # 헤더
        for c in cursors:
            writer.writerow([c])
    
    print(f"\n총 생성: {len(cursors):,}개")
    print(f"저장: {output_file}  (from load_test/data/generater/)")
    
    # 7. 분포 통계
    recent = sum(1 for c in cursors if (max_date - datetime.strptime(c.split('_')[0], '%Y-%m-%dT%H:%M:%S')).days <= 30)
    middle = sum(1 for c in cursors if 30 < (max_date - datetime.strptime(c.split('_')[0], '%Y-%m-%dT%H:%M:%S')).days <= 180)
    deep = sum(1 for c in cursors if (max_date - datetime.strptime(c.split('_')[0], '%Y-%m-%dT%H:%M:%S')).days > 180)
    
    print("\n분포:")
    print(f"  Recent (0-30일):   {recent:,}개 ({recent/len(cursors)*100:.1f}%)")
    print(f"  Middle (30-180일): {middle:,}개 ({middle/len(cursors)*100:.1f}%)")
    print(f"  Deep (180일+):     {deep:,}개 ({deep/len(cursors)*100:.1f}%)")

if __name__ == '__main__':
    generate_cursors_uniform_distribution()
