#!/bin/bash
# Redis Cache Load Test Validation Script
# Checks if all prerequisites are met before running the load test

set +e

echo "=========================================="
echo "Redis Cache Load Test - Validation"
echo "=========================================="
echo ""

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

PASSED=0
FAILED=0
ANY_FAILURE=0

# Function to check and report status
check() {
    local exit_code=$1
    local message=$2
    if [ $exit_code -eq 0 ]; then
        echo -e "${GREEN}✓${NC} $message"
        ((PASSED++))
        return 0
    else
        echo -e "${RED}✗${NC} $message"
        ((FAILED++))
        ANY_FAILURE=1
        return 1
    fi
}

echo "1. Service Availability Checks"
echo "-------------------------------"

# MySQL
mysql -uroot -p"$MYSQL_PASSWORD" -e "SELECT 1;" &>/dev/null
check $? "MySQL is running and accessible"

# Redis
redis-cli PING &>/dev/null
check $? "Redis is running and accessible"

# Spring Boot
curl -s http://localhost:8080/actuator/health | grep -q "UP" &>/dev/null
check $? "Spring Boot application is running (http://localhost:8080)"

echo ""
echo "2. Database Checks"
echo "------------------"

# Test users count
USER_COUNT=$(mysql -uroot -p"$MYSQL_PASSWORD" mopl -sNe "SELECT COUNT(*) FROM users WHERE email LIKE 'cachetest%@mopl.test';" 2>/dev/null || echo "0")
if [ "$USER_COUNT" -ge 200 ]; then
    echo -e "${GREEN}✓${NC} Test users exist in database (${USER_COUNT} users)"
    ((PASSED++))
else
    echo -e "${YELLOW}!${NC} Test users not found (${USER_COUNT}/200 expected)"
    echo "  Run: mysql -uroot -p mopl < load_test/sql/playlist_review/generate_redis_cache_test_data.sql"
    ((FAILED++))
    ANY_FAILURE=1
fi

# Test playlists count
PLAYLIST_COUNT=$(mysql -uroot -p"$MYSQL_PASSWORD" mopl -sNe "SELECT COUNT(*) FROM playlists WHERE title LIKE 'Popular Playlist for Cache Test%';" 2>/dev/null || echo "0")
if [ "$PLAYLIST_COUNT" -ge 100 ]; then
    echo -e "${GREEN}✓${NC} Test playlists exist in database (${PLAYLIST_COUNT} playlists)"
    ((PASSED++))
else
    echo -e "${YELLOW}!${NC} Test playlists not found (${PLAYLIST_COUNT}/100 expected)"
    echo "  Run: mysql -uroot -p mopl < load_test/sql/playlist_review/generate_redis_cache_test_data.sql"
    ((FAILED++))
    ANY_FAILURE=1
fi

# Contents count
CONTENT_COUNT=$(mysql -uroot -p"$MYSQL_PASSWORD" mopl -sNe "SELECT COUNT(*) FROM contents WHERE is_deleted = FALSE;" 2>/dev/null || echo "0")
if [ "$CONTENT_COUNT" -ge 100 ]; then
    echo -e "${GREEN}✓${NC} Sufficient contents in database (${CONTENT_COUNT} contents)"
    ((PASSED++))
else
    echo -e "${RED}✗${NC} Insufficient contents (${CONTENT_COUNT}/100 minimum)"
    echo "  Run: mysql -uroot -p mopl < load_test/sql/contents/generate_contents.sql"
    ((FAILED++))
    ANY_FAILURE=1
fi

echo ""
echo "3. File Checks"
echo "--------------"

# CSV files
if [ -f "load_test/sql/playlist_review/playlist_ids.csv" ]; then
    PLAYLIST_CSV_COUNT=$(wc -l < load_test/sql/playlist_review/playlist_ids.csv)
    echo -e "${GREEN}✓${NC} playlist_ids.csv exists (${PLAYLIST_CSV_COUNT} lines)"
    ((PASSED++))
else
    echo -e "${YELLOW}!${NC} playlist_ids.csv not found"
    echo "  Run: mv /tmp/playlist_ids.csv load_test/sql/playlist_review/"
    ((FAILED++))
    ANY_FAILURE=1
fi

if [ -f "load_test/sql/playlist_review/user_tokens.csv" ]; then
    TOKEN_CSV_COUNT=$(wc -l < load_test/sql/playlist_review/user_tokens.csv)
    
    # Check if tokens are real (not dummy)
    FIRST_TOKEN=$(sed -n '2p' load_test/sql/playlist_review/user_tokens.csv)
    if [[ "$FIRST_TOKEN" == "dummy"* ]]; then
        echo -e "${YELLOW}!${NC} user_tokens.csv contains dummy tokens"
        echo "  Run: python3 load_test/sql/playlist_review/generate_jwt_tokens.py --count 200 --output load_test/sql/playlist_review/user_tokens.csv"
        ((FAILED++))
        ANY_FAILURE=1
    else
        echo -e "${GREEN}✓${NC} user_tokens.csv exists with real tokens (${TOKEN_CSV_COUNT} lines)"
        ((PASSED++))
    fi
else
    echo -e "${RED}✗${NC} user_tokens.csv not found"
    echo "  Run: python3 load_test/sql/playlist_review/generate_jwt_tokens.py --count 200 --output load_test/sql/playlist_review/user_tokens.csv"
    ((FAILED++))
    ANY_FAILURE=1
fi

# JMeter files
if [ -f "load_test/jmeter/playlist_review/playlist_review_redis_cache_3m.jmx" ]; then
    echo -e "${GREEN}✓${NC} JMeter test file (3m) exists"
    ((PASSED++))
else
    echo -e "${RED}✗${NC} JMeter test file (3m) not found"
    ((FAILED++))
    ANY_FAILURE=1
fi

if [ -f "load_test/jmeter/playlist_review/playlist_review_redis_cache_15m.jmx" ]; then
    echo -e "${GREEN}✓${NC} JMeter test file (15m) exists"
    ((PASSED++))
else
    echo -e "${RED}✗${NC} JMeter test file (15m) not found"
    ((FAILED++))
    ANY_FAILURE=1
fi

echo ""
echo "4. Tool Checks"
echo "--------------"

# JMeter
if command -v jmeter &>/dev/null; then
    JMETER_VERSION=$(jmeter --version 2>&1 | grep -oP 'Version \K[0-9.]+' || echo "unknown")
    echo -e "${GREEN}✓${NC} JMeter is installed (version ${JMETER_VERSION})"
    ((PASSED++))
else
    echo -e "${RED}✗${NC} JMeter not found"
    echo "  Install: brew install jmeter (macOS) or download from https://jmeter.apache.org/"
    ((FAILED++))
    ANY_FAILURE=1
fi

# Python3
if command -v python3 &>/dev/null; then
    PYTHON_VERSION=$(python3 --version | grep -oP 'Python \K[0-9.]+')
    echo -e "${GREEN}✓${NC} Python3 is installed (version ${PYTHON_VERSION})"
    ((PASSED++))
else
    echo -e "${RED}✗${NC} Python3 not found"
    ((FAILED++))
    ANY_FAILURE=1
fi

# Python requests library
python3 -c "import requests" &>/dev/null
check $? "Python requests library is installed"

echo ""
echo "5. API Endpoint Check"
echo "---------------------"

# Test login endpoint
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST http://localhost:8080/api/auth/sign-in \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -d "username=invalid&password=invalid" 2>/dev/null || echo "000")

if [ "$HTTP_CODE" == "401" ] || [ "$HTTP_CODE" == "200" ]; then
    echo -e "${GREEN}✓${NC} Login endpoint is accessible (HTTP ${HTTP_CODE})"
    ((PASSED++))
else
    echo -e "${RED}✗${NC} Login endpoint issue (HTTP ${HTTP_CODE})"
    ((FAILED++))
    ANY_FAILURE=1
fi

# Test playlist endpoint (sample)
if [ "$PLAYLIST_COUNT" -gt 0 ]; then
    SAMPLE_PLAYLIST_ID=$(mysql -uroot -p"$MYSQL_PASSWORD" mopl -sNe "SELECT CONCAT(SUBSTR(LOWER(HEX(id)), 1, 8), '-', SUBSTR(LOWER(HEX(id)), 9, 4), '-', SUBSTR(LOWER(HEX(id)), 13, 4), '-', SUBSTR(LOWER(HEX(id)), 17, 4), '-', SUBSTR(LOWER(HEX(id)), 21, 12)) FROM playlists WHERE title LIKE 'Popular Playlist for Cache Test%' LIMIT 1;" 2>/dev/null)
    if [ -n "$SAMPLE_PLAYLIST_ID" ]; then
        HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/playlists/${SAMPLE_PLAYLIST_ID} 2>/dev/null || echo "000")
        if [ "$HTTP_CODE" == "200" ] || [ "$HTTP_CODE" == "401" ]; then
            echo -e "${GREEN}✓${NC} Playlist endpoint is accessible (HTTP ${HTTP_CODE})"
            ((PASSED++))
        else
            echo -e "${RED}✗${NC} Playlist endpoint issue (HTTP ${HTTP_CODE})"
            ((FAILED++))
            ANY_FAILURE=1
        fi
    fi
fi

echo ""
echo "=========================================="
echo "Validation Summary"
echo "=========================================="
echo -e "Passed: ${GREEN}${PASSED}${NC}"
echo -e "Failed: ${RED}${FAILED}${NC}"
echo ""

if [ $ANY_FAILURE -eq 0 ]; then
    echo -e "${GREEN}✓ All checks passed! Ready to run load test.${NC}"
    echo ""
    echo "Next steps:"
    echo "  1. Flush Redis: redis-cli FLUSHALL"
    echo "  2. Run test: jmeter -n -t load_test/jmeter/playlist_review/playlist_review_redis_cache_3m.jmx -l results.jtl -e -o report"
    echo "  3. View report: open report/index.html"
    exit 0
else
    echo -e "${RED}✗ Some checks failed. Please fix the issues above.${NC}"
    echo ""
    echo "Quick fixes:"
    echo "  - Generate test data: mysql -uroot -p mopl < load_test/sql/playlist_review/generate_redis_cache_test_data.sql"
    echo "  - Move CSV files: mv /tmp/*.csv load_test/sql/playlist_review/"
    echo "  - Generate tokens: python3 load_test/sql/playlist_review/generate_jwt_tokens.py --count 200"
    exit 1
fi
