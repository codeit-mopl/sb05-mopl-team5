#!/usr/bin/env python3
"""
Subscription Test - User Access Tokens Generator
목적: Subscription race condition 테스트를 위한 user ID와 access token CSV 생성
"""

import mysql.connector
import csv
import sys

config = {
    'host': 'localhost',
    'port': 3306,
    'user': 'root',
    'password': '',
    'database': 'mopl'
}

def generate_subscription_user_tokens(output_file: str = '../../jmeter/subscription/subscription_user_tokens.csv', count: int = 1000):
    """
    Subscription 테스트용 사용자 ID와 access token을 추출하여 CSV로 저장
    
    Args:
        output_file: 출력 CSV 파일명
        count: 추출할 사용자 수 (기본: 1000)
    """
    try:
        conn = mysql.connector.connect(**config)
        cursor = conn.cursor()
        
        print(f"Subscription 테스트용 사용자 토큰 {count}개 추출 중...")
        
        query = """
            SELECT 
                LOWER(CONCAT(
                    SUBSTR(HEX(id), 1, 8), '-',
                    SUBSTR(HEX(id), 9, 4), '-',
                    SUBSTR(HEX(id), 13, 4), '-',
                    SUBSTR(HEX(id), 17, 4), '-',
                    SUBSTR(HEX(id), 21, 12)
                )) as user_id,
                CONCAT('dummy_jwt_token_', HEX(id)) as access_token
            FROM users
            WHERE email LIKE 'subtest%@mopl.test'
            ORDER BY created_at DESC
            LIMIT %s
        """
        cursor.execute(query, (count,))
        
        user_tokens = cursor.fetchall()
        
        cursor.close()
        conn.close()
        
        if not user_tokens:
            print("❌ 에러: subtest 사용자를 찾을 수 없습니다.")
            print("⚠️  먼저 다음 SQL을 실행하세요:")
            print("    mysql -uroot -p mopl < load_test/sql/subscription/generate_subscription_test_data.sql")
            sys.exit(1)
        
        with open(output_file, 'w', newline='') as f:
            writer = csv.writer(f)
            writer.writerow(['user_id', 'access_token'])
            for user_id, token in user_tokens:
                writer.writerow([user_id, token])
        
        print(f"✅ {len(user_tokens)}개 사용자 토큰 저장 완료!")
        print(f"📁 파일: {output_file}")
        
        print("\n📊 통계:")
        conn = mysql.connector.connect(**config)
        cursor = conn.cursor()
        cursor.execute("""
            SELECT 
                COUNT(*) as total,
                COUNT(DISTINCT email) as unique_emails
            FROM users
            WHERE email LIKE 'subtest%@mopl.test'
        """)
        row = cursor.fetchone()
        if row:
            total, unique_emails = row
            print(f"  전체 사용자: {total}개")
            print(f"  고유 이메일: {unique_emails}개")
        
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
    
    parser = argparse.ArgumentParser(description='Generate subscription test user tokens CSV')
    parser.add_argument('--output', default='../../jmeter/subscription/subscription_user_tokens.csv', help='Output CSV file path')
    parser.add_argument('--count', type=int, default=1000, help='Number of user tokens to generate')
    
    args = parser.parse_args()
    
    generate_subscription_user_tokens(args.output, args.count)
