# Playlist Review Redis Cache Load Test - Quick Start

## 빠른 실행 가이드 (5분 완성)

### 1️⃣ 전제 조건 확인

```bash
# MySQL, Redis, Spring Boot 실행 여부 확인
mysql -uroot -p -e "SELECT 1;" 2>/dev/null && echo "✓ MySQL Running"
redis-cli PING 2>/dev/null && echo "✓ Redis Running"
curl -s http://localhost:8080/actuator/health 2>/dev/null && echo "✓ Spring Boot Running"
```

모두 ✓이어야 진행 가능합니다.

---

### 2️⃣ 테스트 데이터 생성 (2분)

```bash
cd <repo-root>

# MySQL에 테스트 데이터 생성
mysql -uroot -p mopl < load_test/sql/playlist_review/generate_redis_cache_test_data.sql

# 생성된 CSV 파일 확인
ls -lh /tmp/playlist_ids.csv /tmp/user_tokens.csv

# CSV 파일 이동
mv /tmp/playlist_ids.csv load_test/sql/playlist_review/
mv /tmp/user_tokens.csv load_test/sql/playlist_review/
```

**생성 내용**:
- 1,000명의 테스트 사용자 (cachetest0 ~ cachetest999)
- 100개의 인기 플레이리스트
- 플레이리스트당 10~50개 콘텐츠
- 플레이리스트당 50~200명 구독자

---

### 3️⃣ JWT 토큰 생성 (1분)

```bash
# Python requests 설치 (필요시)
pip3 install requests

# JWT 토큰 생성 (200개)
python3 load_test/sql/playlist_review/generate_jwt_tokens.py \
  --count 200 \
  --output load_test/sql/playlist_review/user_tokens.csv

# 토큰 생성 확인
wc -l load_test/sql/playlist_review/user_tokens.csv
# 출력: 201 (헤더 1개 + 토큰 200개)
```

**문제 발생 시**:
```bash
# 테스트 모드로 1명만 로그인 시도
python3 load_test/sql/playlist_review/generate_jwt_tokens.py --test
```

---

### 4️⃣ JMeter 테스트 실행 (3분)

#### 3분 빠른 테스트
```bash
cd <repo-root>

# 결과 디렉토리 생성
mkdir -p load_test/results

# Redis 캐시 초기화
redis-cli FLUSHALL

# JMeter 테스트 실행 (CLI 모드)
jmeter -n -t load_test/jmeter/playlist_review/playlist_review_redis_cache_3m.jmx \
  -l load_test/results/cache_test_result.jtl \
  -e -o load_test/results/cache_test_report

# 리포트 확인
open load_test/results/cache_test_report/index.html
```

#### 15분 안정성 테스트
```bash
jmeter -n -t load_test/jmeter/playlist_review/playlist_review_redis_cache_15m.jmx \
  -l load_test/results/cache_test_15m_result.jtl \
  -e -o load_test/results/cache_test_15m_report

open load_test/results/cache_test_15m_report/index.html
```

---

### 5️⃣ 결과 확인

#### JMeter HTML 리포트에서 확인
- **Response Time**: 평균 응답 시간, 90%, 95% 백분위수
- **Throughput**: 초당 처리 요청 수
- **Error Rate**: 오류 발생률 (0% 목표)

#### Redis 캐시 상태 확인
```bash
# 캐시된 키 개수
redis-cli KEYS "playlistDetail*" | wc -l

# Redis 메모리 사용량
redis-cli INFO memory | grep used_memory_human

# 캐시 히트/미스 통계 (Spring Actuator 사용 시)
curl http://localhost:8080/actuator/metrics/cache.gets | jq
```

---

## 📊 성능 비교 테스트

### Before: Redis 캐시 비활성화
```bash
# 1. RedisConfig.java에서 playlistDetail 캐시 주석 처리
# 2. Spring Boot 재시작
# 3. 테스트 실행
jmeter -n -t load_test/jmeter/playlist_review/playlist_review_redis_cache_3m.jmx \
  -l load_test/results/no_cache_result.jtl \
  -e -o load_test/results/no_cache_report
```

### After: Redis 캐시 활성화
```bash
# 1. RedisConfig.java에서 playlistDetail 캐시 활성화
# 2. Spring Boot 재시작 + Redis Flush
redis-cli FLUSHALL

# 3. 테스트 실행
jmeter -n -t load_test/jmeter/playlist_review/playlist_review_redis_cache_3m.jmx \
  -l load_test/results/with_cache_result.jtl \
  -e -o load_test/results/with_cache_report
```

### 결과 비교
| 지표 | 캐시 없음 | 캐시 있음 | 개선율 |
|------|----------|----------|--------|
| 평균 응답 시간 | ? ms | ? ms | ?% |
| 90% 응답 시간 | ? ms | ? ms | ?% |
| 처리량 (req/s) | ? | ? | ?% |

---

## 🧹 정리

```bash
# Redis 캐시만 삭제
redis-cli FLUSHALL

# MySQL 테스트 데이터 삭제
mysql -uroot -p mopl << EOF
DELETE FROM subscriptions WHERE playlist_id IN (SELECT id FROM playlists WHERE title LIKE 'Popular Playlist for Cache Test%');
DELETE FROM playlist_contents WHERE playlist_id IN (SELECT id FROM playlists WHERE title LIKE 'Popular Playlist for Cache Test%');
DELETE FROM playlists WHERE title LIKE 'Popular Playlist for Cache Test%';
DELETE FROM users WHERE email LIKE 'cachetest%@mopl.test';
EOF

# CSV 파일 삭제
rm -f load_test/sql/playlist_review/playlist_ids.csv
rm -f load_test/sql/playlist_review/user_tokens.csv

# JMeter 결과 삭제
rm -rf load_test/results/*
```

---

## ⚠️ 문제 해결

### JWT 토큰 생성 실패
```bash
# 원인 1: Spring Boot 미실행
curl http://localhost:8080/actuator/health

# 원인 2: 테스트 사용자 미생성
mysql -uroot -p mopl -e "SELECT COUNT(*) FROM users WHERE email LIKE 'cachetest%';"
# 결과가 1000이 아니면 Step 2 재실행

# 원인 3: 비밀번호 불일치
# generate_redis_cache_test_data.sql에서 설정한 비밀번호 확인
```

### JMeter 실행 오류
```bash
# CSV 파일 경로 확인
ls -l load_test/sql/playlist_review/playlist_ids.csv
ls -l load_test/sql/playlist_review/user_tokens.csv

# JMeter 버전 확인 (5.6.3 이상)
jmeter --version
```

### 응답 시간 목표 미달
```bash
# 1. 캐시 히트율 확인
redis-cli KEYS "playlistDetail*" | wc -l
# 200명 × 100개 = 최대 20,000개

# 2. DB 쿼리 로그 확인 (캐시 미스 시 쿼리 발생)
# application.yaml: logging.level.org.hibernate.SQL=DEBUG

# 3. Redis 메모리 확인
redis-cli INFO memory
```

---

**실행 시간**: 총 5~10분  
**작성일**: 2026-01-28  
**참고**: 전체 가이드는 `README_REDIS_CACHE_TEST.md` 참조
