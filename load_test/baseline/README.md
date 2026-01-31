# Baseline Test - Redis 캐시 성능 비교 (캐시 없음)

## 📋 테스트 개요

이 문서는 **Redis 캐시 적용 전후 성능 비교**를 위한 **Baseline 테스트 (캐시 비활성화)** 설정 및 결과를 정리합니다.

### 목적
플레이리스트 조회 API에서 Redis 캐시를 사용하지 않고 데이터베이스에서 직접 조회할 때의 성능을 측정하여, 이후 Redis 캐시 적용 후와 비교하기 위한 기준선(Baseline) 데이터를 수집합니다.

---

## 🔧 캐시 비활성화 설정

### 1. 수정 대상 파일
```
src/main/java/com/mopl/api/domain/playlist/service/PlaylistService.java
```

### 2. 수정 내용

**원본 (캐시 활성화 상태)**:
```java
@Cacheable(value = "playlistDetail", key = "#playlistId + '_' + #currentUserId")
public PlaylistDto getPlaylist(UUID playlistId, UUID currentUserId) {
    // ... 메서드 구현 ...
}
```

**수정 후 (Baseline 테스트용 - 캐시 비활성화)**:
```java
// @Cacheable(value = "playlistDetail", key = "#playlistId + '_' + #currentUserId")  // Temporarily disabled for baseline test
public PlaylistDto getPlaylist(UUID playlistId, UUID currentUserId) {
    // ... 메서드 구현 ...
}
```

### 3. 수정 위치
- **파일**: `PlaylistService.java`
- **라인 번호**: 124-125
- **메서드**: `getPlaylist(UUID playlistId, UUID currentUserId)`

### 4. 변경 사항
- `@Cacheable` 어노테이션을 **주석 처리**
- Spring Boot가 Redis 캐시를 사용하지 않고 매 요청마다 데이터베이스에서 데이터를 직접 조회하도록 설정

---

## 🚀 테스트 실행 방법

### 1. 애플리케이션 재빌드 및 재시작
```bash
cd /Users/PARK/Documents/codeit_mopl/sb05-mopl-team5

# 기존 애플리케이션 종료
pkill -f "java.*mopl-api"

# 재빌드 (테스트 제외)
./gradlew build -x test

# 애플리케이션 시작
java -jar build/libs/mopl-api-0.0.1-SNAPSHOT.jar > app.log 2>&1 &
```

### 2. API 정상 동작 확인
```bash
curl http://localhost:8080/api/playlists/0a6b4418-fe60-11f0-b73c-689ddc77bd50
```

예상 응답:
```json
{
  "id": "0a6b4418-fe60-11f0-b73c-689ddc77bd50",
  "owner": { ... },
  "title": "...",
  "subscriberCount": 143,
  ...
}
```

### 3. JMeter 부하 테스트 실행
```bash
cd /Users/PARK/Documents/codeit_mopl/sb05-mopl-team5

# 기존 결과 파일 삭제 (선택사항)
rm -f results_redis_no_cache_15m.jtl

# JMeter 테스트 실행
jmeter -n \
  -t load_test/jmeter/playlist_review/playlist_review_redis_cache_15m.jmx \
  -l results_redis_no_cache_15m.jtl
```

### 4. 테스트 설정
- **Thread 수**: 200
- **테스트 시간**: 15분 (900초)
- **Think Time**: 500ms (요청 간 대기 시간)
- **대상 API**: `GET /api/playlists/{playlistId}`
- **데이터 소스**: 
  - `load_test/sql/playlist_review/playlist_ids.csv` (1,001 playlists)
  - `load_test/sql/playlist_review/user_tokens.csv` (1,001 tokens)

---

## 📊 테스트 결과

### 실행 정보
- **실행 일시**: 2026년 1월 31일 15:24 KST
- **테스트 지속 시간**: 9.97분 (약 10분)
- **결과 파일**: `results_redis_no_cache_15m.jtl`

### 성능 지표

| 지표 | 값 |
|------|-----|
| **총 샘플 수** | 223,586 |
| **성공 요청** | 223,265 (99.86%) |
| **실패 요청** | 321 (0.14%) |
| **평균 응답 시간** | **2.77 ms** |
| **중간값 (Median)** | 2.00 ms |
| **95th Percentile** | 6.00 ms |
| **99th Percentile** | 11.00 ms |
| **최소 응답 시간** | 0 ms |
| **최대 응답 시간** | 200 ms |
| **처리량 (Throughput)** | **373.66 requests/sec** |

### 결과 분석 스크립트
```bash
python3 << 'EOF'
import csv
from datetime import datetime

jtl_file = 'results_redis_no_cache_15m.jtl'
response_times = []
errors = 0
total = 0
success_times = []

with open(jtl_file, 'r') as f:
    reader = csv.DictReader(f)
    for row in reader:
        total += 1
        elapsed = int(row['elapsed'])
        response_times.append(elapsed)
        
        if row['success'] == 'true':
            success_times.append(elapsed)
        else:
            errors += 1

response_times.sort()
success_times.sort()

def percentile(data, p):
    if not data:
        return 0
    k = (len(data) - 1) * p / 100
    f = int(k)
    c = f + 1
    if c >= len(data):
        return data[-1]
    return data[f] + (k - f) * (data[c] - data[f])

print('=== BASELINE TEST RESULTS (Without Redis Cache) ===')
print(f'Total Samples: {total:,}')
print(f'Successful: {len(success_times):,}')
print(f'Errors: {errors} ({errors/total*100:.2f}%)')
print(f'')
print(f'Response Time (Success only):')
print(f'  Average: {sum(success_times)/len(success_times):.2f} ms')
print(f'  Median: {percentile(success_times, 50):.2f} ms')
print(f'  95th percentile: {percentile(success_times, 95):.2f} ms')
print(f'  99th percentile: {percentile(success_times, 99):.2f} ms')
EOF
```

---

## 🔄 캐시 테스트로 전환하기

Baseline 테스트가 완료된 후, Redis 캐시를 활성화하여 성능 비교 테스트를 진행합니다.

### 1. 캐시 활성화
`PlaylistService.java` 파일에서 주석 제거:

```java
@Cacheable(value = "playlistDetail", key = "#playlistId + '_' + #currentUserId")
public PlaylistDto getPlaylist(UUID playlistId, UUID currentUserId) {
    // ... 메서드 구현 ...
}
```

### 2. 애플리케이션 재빌드 및 재시작
```bash
pkill -f "java.*mopl-api"
./gradlew build -x test
java -jar build/libs/mopl-api-0.0.1-SNAPSHOT.jar > app.log 2>&1 &
```

### 3. Redis 캐시 초기화 (선택사항)
```bash
redis-cli FLUSHALL
```

### 4. 캐시 적용 테스트 실행
```bash
jmeter -n \
  -t load_test/jmeter/playlist_review/playlist_review_redis_cache_15m.jmx \
  -l results_redis_with_cache_15m.jtl
```

### 5. 결과 비교
두 테스트의 결과 파일을 비교하여 Redis 캐시의 성능 향상 효과를 분석합니다:
- `results_redis_no_cache_15m.jtl` (Baseline)
- `results_redis_with_cache_15m.jtl` (With Cache)

---

## 📝 주요 파일 위치

### 소스 코드
- **캐시 설정 파일**: `src/main/java/com/mopl/api/domain/playlist/service/PlaylistService.java`
- **라인**: 124-125

### 테스트 파일
- **JMeter 테스트**: `load_test/jmeter/playlist_review/playlist_review_redis_cache_15m.jmx`
- **테스트 데이터**: 
  - `load_test/sql/playlist_review/playlist_ids.csv`
  - `load_test/sql/playlist_review/user_tokens.csv`

### 결과 파일
- **Baseline 결과**: `results_redis_no_cache_15m.jtl` (프로젝트 루트)
- **애플리케이션 로그**: `app.log` (프로젝트 루트)

---

## ⚠️ 주의사항

1. **테스트 순서**: 반드시 Baseline 테스트(캐시 비활성화)를 먼저 실행한 후, 캐시 활성화 테스트를 진행해야 공정한 비교가 가능합니다.

2. **동일한 환경**: 두 테스트는 동일한 데이터베이스 상태, 동일한 네트워크 환경에서 실행해야 합니다.

3. **충분한 테스트 시간**: 15분 이상의 테스트를 권장하며, 초기 워밍업 시간을 고려해야 합니다.

4. **데이터베이스 부하**: Baseline 테스트는 캐시 없이 데이터베이스를 직접 조회하므로, 데이터베이스 서버의 CPU/메모리 사용량이 높을 수 있습니다.

5. **결과 파일 백업**: 테스트 결과 파일(`.jtl`)은 재실행 시 덮어쓰여지므로, 중요한 결과는 별도로 백업하세요.

---

## 📚 참고 자료

### 관련 문서
- [Redis Cache Test README](../sql/playlist_review/README_REDIS_CACHE_TEST.md)
- [JMeter Test Guide](../sql/playlist_review/QUICKSTART.md)

### 기타 완료된 테스트
- **Deep Pagination 인덱스 최적화**:
  - `results_baseline_15m.jtl` - 인덱스 없음 (71ms avg)
  - `results_optimized_15m.jtl` - 인덱스 적용 (3ms avg)

---

## 📞 문의 및 이슈

테스트 실행 중 문제가 발생하거나 결과에 대한 질문이 있을 경우, 프로젝트 이슈 트래커에 등록해주세요.

---

**마지막 업데이트**: 2026년 1월 31일  
**작성자**: 부하 테스트 팀
