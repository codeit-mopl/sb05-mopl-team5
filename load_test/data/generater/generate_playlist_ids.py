#!/usr/bin/env python3

import mysql.connector
import csv

config = {
    'host': 'localhost',
    'port': 3306,
    'user': 'root',
    'password': '',
    'database': 'mopl'
}

def generate_playlist_ids(output_file: str = 'playlist_ids.csv', count: int = 500):
    conn = mysql.connector.connect(**config)
    cursor = conn.cursor()
    
    print(f"상위 {count}개 플레이리스트 ID 추출 중...")
    
    query = """
        SELECT HEX(id)
        FROM playlists
        WHERE is_deleted = FALSE
        ORDER BY subscriber_count DESC, id DESC
        LIMIT %s
    """
    cursor.execute(query, (count,))
    
    playlist_ids = [row[0] for row in cursor.fetchall()]
    
    cursor.close()
    conn.close()
    
    with open(output_file, 'w', newline='') as f:
        writer = csv.writer(f)
        writer.writerow(['playlist_id'])
        for pid in playlist_ids:
            writer.writerow([pid])
    
    print(f"✅ {len(playlist_ids)}개 플레이리스트 ID 저장: {output_file}")

if __name__ == '__main__':
    generate_playlist_ids()
