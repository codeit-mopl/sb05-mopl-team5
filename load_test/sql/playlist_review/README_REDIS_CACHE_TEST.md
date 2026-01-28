# Playlist Review Redis Cache Load Test

## 📖 개요

Playlist 상세 조회 시 Redis 캐싱의 성능 개선 효과를 측정하기 위한 부하 테스트 환경입니다.

**테스트 목표**:
- Redis 캐시 적용 후 응답 시간 개선율 측정
- 캐시 히트율(Cache Hit Rate) 분석
- 동시 사용자 200명 환경에서의 성능 검증

## 📋 테스트 시나리오

### 1. 캐시 적용 API
- **엔드포인트**: `GET /api/playlists/{playlistId}`
- **캐싱 전략**: 
  - 캐시명: `playlistDetail`
  - TTL: 30분
  - 키: `{playlistId}_{currentUserId}` (사용자별 캐시)
- **캐싱 데이터**:
  - Playlist 기본 정보
  - PlaylistContents (3개 쿼리 통합)
  - Subscription 정보

### 2. 테스트 시나리오
- **100개의 인기 플레이리스트**를 **1,000명의 사용자**가 반복 조회
- 동일 플레이리스트에 대한 요청 집중 → 높은 캐시 히트율 시뮬레이션
- Think Time: 500ms (사용자 행동 시뮬레이션)

## 📊 테스트 파일

### JMeter 테스트 파일

| 파일명 | 동시 사용자 | 테스트 시간 | Ramp-up | 용도 |
|--------|------------|------------|---------|------|
| `playlist_review_redis_cache_3m.jmx` | 200명 | 3분 | 30초 | 빠른 검증 |
| `playlist_review_redis_cache_15m.jmx` | 200명 | 15분 | 60초 | 안정성 테스트 |

### SQL 스크립트

| 파일명 | 용도 |
|--------|------|
| `generate_redis_cache_test_data.sql` | 테스트 데이터 생성 |
| `reset_playlist_review_data.sql` | 데이터 정리 (기존 파일 활용) |

## 🚀 실행 방법

### 1. 전제 조건

#### 필수 사항
- MySQL 실행 중
- Spring Boot 애플리케이션 실행 중 (`http://localhost:8080`)
- Redis 실행 중 (Redis 캐싱 활성화)
- JMeter 5.6.3 이상 설치
- Contents 데이터 최소 100개 이상 존재

#### Contents 데이터 확인
```bash
mysql -uroot -p mopl -e "SELECT COUNT(*) FROM contents WHERE is_deleted = FALSE;"
```

최소 100개 이상 필요. 부족하면:
```bash
mysql -uroot -p mopl < load_test/sql/contents/generate_contents.sql
```

---

### 2. 테스트 데이터 생성

```bash
# MySQL 접속 및 데이터 생성
mysql -uroot -p mopl < load_test/sql/playlist_review/generate_redis_cache_test_data.sql 2>&1
```

**생성 데이터**:
- 인기 플레이리스트: 100개
- 테스트 사용자: 1,000명
- 플레이리스트당 콘텐츠: 10~50개
- 플레이리스트당 구독자: 50~200명

**소요 시간**: 약 2~3분

---

### 3. CSV 파일 이동

```bash
# SQL 실행 후 생성된 CSV 파일을 JMeter가 읽을 수 있는 위치로 이동
mv /tmp/playlist_ids.csv load_test/sql/playlist_review/
mv /tmp/user_tokens.csv load_test/sql/playlist_review/
```

**생성된 CSV 파일**:
- `playlist_ids.csv`: 100개의 플레이리스트 UUID
- `user_tokens.csv`: 200명의 사용자 토큰 (더미 토큰)

---

### 4. 실제 JWT 토큰 생성 (중요!)

⚠️ **주의**: SQL에서 생성한 `user_tokens.csv`는 더미 토큰입니다.  
실제 테스트를 위해서는 **Spring Boot 애플리케이션에서 발급한 JWT 토큰**이 필요합니다.

#### 옵션 1: Python 스크립트 사용 (권장)

**전제 조건**:
- Spring Boot 애플리케이션이 실행 중이어야 함 (`http://localhost:8080`)
- 테스트 사용자가 데이터베이스에 존재해야 함 (Step 2 완료)

```bash
# Python requests 라이브러리 설치
pip3 install requests

# JWT 토큰 생성 (200개)
python3 load_test/sql/playlist_review/generate_jwt_tokens.py \
  --count 200 \
  --output load_test/sql/playlist_review/user_tokens.csv

# 테스트 모드 (1명만 테스트)
python3 load_test/sql/playlist_review/generate_jwt_tokens.py --test
```

**스크립트 옵션**:
- `--base-url`: Spring Boot URL (기본값: `http://localhost:8080`)
- `--start-idx`: 시작 사용자 인덱스 (기본값: 0)
- `--count`: 생성할 토큰 개수 (기본값: 200)
- `--password`: 테스트 사용자 비밀번호 (기본값: `testpassword`)
- `--output`: 출력 CSV 파일 경로 (기본값: `user_tokens.csv`)
- `--test`: 첫 번째 사용자로만 테스트

**출력 예시**:
```text
Generating 200 JWT tokens...
Base URL: http://localhost:8080
User range: cachetest0@mopl.test ~ cachetest199@mopl.test
------------------------------------------------------------
[ 10/200] cachetest9@mopl.test ✓
[ 20/200] cachetest19@mopl.test ✓
...
[ 200/200] cachetest199@mopl.test ✓
------------------------------------------------------------
Success: 200/200 (100.0%)

✓ Tokens saved to: user_tokens.csv
```

#### 옵션 2: curl로 수동 생성 (수동)
```bash
# CSRF 토큰 획득 및 로그인
curl -c cookies.txt -X GET http://localhost:8080/api/auth/csrf-token

# 각 사용자에 대해 로그인 (form data 형식)
for i in {0..199}; do
  csrf_token=$(grep XSRF-TOKEN cookies.txt | awk '{print $7}')
  curl -b cookies.txt -X POST http://localhost:8080/api/auth/sign-in \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -H "X-XSRF-TOKEN: $csrf_token" \
    -d "username=cachetest${i}@mopl.test&password=testpassword" \
    | jq -r '.token' >> user_tokens.csv
done
```

#### 옵션 3: 인증 없이 테스트 (제한적)
- JMeter 파일에서 `Authorization` 헤더를 제거
- `CustomUserDetails` 없이 조회 (일부 필드 누락 가능)

---

### 5. JMeter 테스트 실행

#### GUI 모드 (개발/디버깅용)
```bash
# 3분 테스트
jmeter -t load_test/jmeter/playlist_review/playlist_review_redis_cache_3m.jmx

# 15분 테스트
jmeter -t load_test/jmeter/playlist_review/playlist_review_redis_cache_15m.jmx
```

#### CLI 모드 (성능 측정용, 권장)
```bash
# 3분 테스트
jmeter -n -t load_test/jmeter/playlist_review/playlist_review_redis_cache_3m.jmx \
  -l results/cache_test_3m_result.jtl \
  -e -o results/cache_test_3m_report

# 15분 테스트
jmeter -n -t load_test/jmeter/playlist_review/playlist_review_redis_cache_15m.jmx \
  -l results/cache_test_15m_result.jtl \
  -e -o results/cache_test_15m_report
```

**리포트 확인**:
```bash
# HTML 리포트 열기
open results/cache_test_3m_report/index.html
```

---

### 6. Redis 캐시 모니터링

#### 캐시 키 확인
```bash
redis-cli KEYS "playlistDetail*" | wc -l
```

#### 캐시 히트율 확인
```bash
redis-cli INFO stats | grep keyspace
```

#### 특정 캐시 조회
```bash
# 캐시 키 패턴: playlistDetail::{playlistId}_{userId}
redis-cli GET "playlistDetail::<playlist-uuid>_<user-uuid>"
```

---

## 📈 성능 측정 지표

### 주요 메트릭

#### 1. 응답 시간 (Response Time)
- **캐시 미스 (Cold Start)**: 첫 요청, DB 조회 필요
- **캐시 히트 (Warm)**: 캐시에서 조회, Redis 속도

**목표**:
- 캐시 히트 시: **< 100ms**
- 캐시 미스 시: **< 500ms**

#### 2. 캐시 히트율 (Cache Hit Rate)
```text
캐시 히트율 = (캐시 히트 수 / 총 요청 수) × 100%
```

**기대값**:
- 인기 플레이리스트 (100개)를 200명이 반복 조회
- 예상 히트율: **80~95%**

#### 3. 처리량 (Throughput)
- **요청/초 (Requests per Second)**
- 캐시 적용 후 증가 예상

#### 4. 오류율 (Error Rate)
- **목표**: 0%
- 500ms 초과 시 `DurationAssertion` 실패

---

## 🔄 비교 테스트 시나리오

### 캐시 적용 전/후 성능 비교

#### 1단계: 캐시 비활성화 테스트
```java
// RedisConfig.java에서 playlistDetail 캐시 제거 또는 주석 처리
// configurations.put("playlistDetail", config.entryTtl(Duration.ofMinutes(30)));
```

```bash
# 애플리케이션 재시작 후 테스트
jmeter -n -t load_test/jmeter/playlist_review/playlist_review_redis_cache_3m.jmx \
  -l results/no_cache_result.jtl \
  -e -o results/no_cache_report
```

#### 2단계: 캐시 활성화 테스트
```java
// RedisConfig.java에서 playlistDetail 캐시 활성화
configurations.put("playlistDetail", config.entryTtl(Duration.ofMinutes(30)));
```

```bash
# 애플리케이션 재시작 및 Redis Flush
redis-cli FLUSHALL

# 테스트 실행
jmeter -n -t load_test/jmeter/playlist_review/playlist_review_redis_cache_3m.jmx \
  -l results/with_cache_result.jtl \
  -e -o results/with_cache_report
```

#### 3단계: 결과 비교

| 지표 | 캐시 없음 | 캐시 있음 | 개선율 |
|------|----------|----------|--------|
| 평균 응답 시간 | ? ms | ? ms | ?% |
| 90% 응답 시간 | ? ms | ? ms | ?% |
| 95% 응답 시간 | ? ms | ? ms | ?% |
| 처리량 (req/s) | ? | ? | ?% |
| 오류율 | ?% | ?% | - |

---

## 🧹 테스트 데이터 정리

### 캐시만 삭제 (데이터 유지)
```bash
redis-cli FLUSHALL
```

### MySQL 테스트 데이터 삭제
```bash
mysql -uroot -p mopl << EOF
DELETE FROM subscriptions WHERE playlist_id IN (SELECT id FROM playlists WHERE title LIKE 'Popular Playlist for Cache Test%');
DELETE FROM playlist_contents WHERE playlist_id IN (SELECT id FROM playlists WHERE title LIKE 'Popular Playlist for Cache Test%');
DELETE FROM playlists WHERE title LIKE 'Popular Playlist for Cache Test%';
DELETE FROM users WHERE email LIKE 'cachetest%@mopl.test';
EOF
```

또는 기존 정리 스크립트 활용:
```bash
mysql -uroot -p mopl < load_test/sql/playlist_review/reset_playlist_review_data.sql
```

---

## 📚 참고 문서

- **Redis 캐싱 구현 보고서**: `playlist_review_redis_cache_v2/report_v2.md`
- **캐싱 전략**: 
  - TTL: 30분 (변경 빈도 낮음)
  - 키: `{playlistId}_{userId}` (사용자별 개인화)
  - 무효화: Playlist 수정/삭제 시 자동 무효화

---

## ⚠️ 주의사항

1. **JWT 토큰**: 실제 테스트 시 반드시 유효한 JWT 토큰 사용
2. **Redis 메모리**: 200명 × 100개 = 20,000개 캐시 생성 가능 (메모리 확인)
3. **DB 부하**: 캐시 미스 시 DB 쿼리 발생 (첫 요청 시)
4. **TTL 만료**: 30분 후 캐시 자동 만료 → 캐시 미스 발생
5. **테스트 격리**: 다른 부하 테스트와 동시 실행 시 결과 왜곡 가능

---

## 🎯 예상 결과 (Redis 캐싱 효과)

### 보고서 기반 예상치

| 시나리오 | Before (캐시 없음) | After (캐시 있음) | 개선율 |
|---------|-------------------|------------------|--------|
| Playlist 상세 조회 (캐시 히트) | ~30ms | ~3ms | **90%** |
| Playlist 상세 조회 (캐시 미스) | ~30ms | ~30ms | 0% |
| DB 쿼리 수 (시간당 10K 요청) | 30,000 | 180 | **99.4%↓** |

**캐시 히트율 80% 가정 시**:
- 평균 응답 시간: `0.8 × 3ms + 0.2 × 30ms = 8.4ms` (**72% 개선**)

---

**작성일**: 2026-01-28  
**테스트 환경**: Spring Boot + Redis + MySQL + JMeter  
**문의**: 팀 내부 Slack #performance-testing
