#!/usr/bin/env python3
"""
JWT Token Generator for Load Testing
Generates real JWT tokens by calling the Spring Boot login API.

Usage:
    python3 generate_jwt_tokens.py --output user_tokens.csv --count 200

Requirements:
    pip install requests
"""

import requests
import sys
import argparse
import time
from typing import List, Tuple


def get_csrf_token(base_url: str, session: requests.Session) -> str:
    """Get CSRF token from the server."""
    try:
        response = session.get(f"{base_url}/api/auth/csrf-token", timeout=10)
        if response.status_code != 200:
            print(f"Warning: CSRF token request returned {response.status_code}")
        
        csrf_cookie = session.cookies.get('XSRF-TOKEN')
        if csrf_cookie:
            return csrf_cookie
        
        return ""
    except Exception as e:
        print(f"Warning: Failed to get CSRF token: {e}")
        return ""


def login_user(base_url: str, email: str, password: str, session: requests.Session) -> Tuple[bool, str]:
    """
    Login a user and return JWT access token.
    
    Returns:
        Tuple[bool, str]: (success, token or error_message)
    """
    csrf_token = get_csrf_token(base_url, session)
    
    headers = {
        'Content-Type': 'application/x-www-form-urlencoded'
    }
    
    if csrf_token:
        headers['X-XSRF-TOKEN'] = csrf_token
    
    data = {
        'username': email,
        'password': password
    }
    
    try:
        response = session.post(
            f"{base_url}/api/auth/sign-in",
            headers=headers,
            data=data,
            timeout=10,
            allow_redirects=False
        )
        
        if response.status_code == 200:
            try:
                json_data = response.json()
                token = json_data.get('token')
                if token:
                    return True, token
                else:
                    return False, f"No token in response: {json_data}"
            except Exception as e:
                return False, f"Failed to parse JSON: {e}"
        else:
            return False, f"HTTP {response.status_code}: {response.text[:200]}"
            
    except Exception as e:
        return False, f"Request failed: {e}"


def generate_tokens(base_url: str, start_idx: int, count: int, password: str) -> List[str]:
    """
    Generate JWT tokens for multiple test users.
    
    Args:
        base_url: Spring Boot application URL
        start_idx: Starting user index (e.g., 0 for cachetest0@mopl.test)
        count: Number of tokens to generate
        password: Password for all test users
        
    Returns:
        List of JWT access tokens
    """
    tokens = []
    
    print(f"Generating {count} JWT tokens...")
    print(f"Base URL: {base_url}")
    print(f"User range: cachetest{start_idx}@mopl.test ~ cachetest{start_idx + count - 1}@mopl.test")
    print("-" * 60)
    
    for i in range(start_idx, start_idx + count):
        email = f"cachetest{i}@mopl.test"
        
        session = requests.Session()
        
        success, result = login_user(base_url, email, password, session)
        
        if success:
            tokens.append(result)
            if (i - start_idx + 1) % 10 == 0:
                print(f"[{i - start_idx + 1:3d}/{count}] {email} ✓")
        else:
            print(f"[{i - start_idx + 1:3d}/{count}] {email} ✗ - {result}")
            tokens.append("")
        
        if (i - start_idx + 1) % 50 == 0:
            time.sleep(1)
    
    print("-" * 60)
    success_count = sum(1 for t in tokens if t)
    print(f"Success: {success_count}/{count} ({success_count/count*100:.1f}%)")
    
    return tokens


def save_tokens_to_csv(tokens: List[str], output_file: str):
    """Save tokens to CSV file."""
    try:
        with open(output_file, 'w', encoding='utf-8') as f:
            f.write("token\n")
            for token in tokens:
                f.write(f"{token}\n")
        print(f"\n✓ Tokens saved to: {output_file}")
    except Exception as e:
        print(f"\n✗ Failed to save tokens: {e}")
        sys.exit(1)


def main():
    parser = argparse.ArgumentParser(
        description='Generate JWT tokens for load testing by calling login API'
    )
    parser.add_argument(
        '--base-url',
        default='http://localhost:8080',
        help='Spring Boot application URL (default: http://localhost:8080)'
    )
    parser.add_argument(
        '--start-idx',
        type=int,
        default=0,
        help='Starting user index (default: 0)'
    )
    parser.add_argument(
        '--count',
        type=int,
        default=200,
        help='Number of tokens to generate (default: 200)'
    )
    parser.add_argument(
        '--password',
        default='testpassword',
        help='Password for test users (default: testpassword)'
    )
    parser.add_argument(
        '--output',
        default='user_tokens.csv',
        help='Output CSV file path (default: user_tokens.csv)'
    )
    parser.add_argument(
        '--test',
        action='store_true',
        help='Test with first user only'
    )
    
    args = parser.parse_args()
    
    if args.test:
        print("Test mode: Logging in with cachetest0@mopl.test...")
        session = requests.Session()
        success, result = login_user(args.base_url, 'cachetest0@mopl.test', args.password, session)
        
        if success:
            print(f"\n✓ Login successful!")
            print(f"Token (first 50 chars): {result[:50]}...")
        else:
            print(f"\n✗ Login failed: {result}")
        
        return
    
    tokens = generate_tokens(args.base_url, args.start_idx, args.count, args.password)
    
    if not any(tokens):
        print("\n✗ No tokens generated. Please check:")
        print("  1. Spring Boot application is running at", args.base_url)
        print("  2. Test users exist in database (run generate_redis_cache_test_data.sql)")
        print("  3. Password is correct")
        sys.exit(1)
    
    save_tokens_to_csv(tokens, args.output)


if __name__ == '__main__':
    main()
