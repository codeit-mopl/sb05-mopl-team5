# Baseline Test Setup Guide
# Redis 캐시 비활성화를 위한 설정 가이드

이 문서는 Baseline 테스트를 위해 **어떤 코드를 어떻게 수정했는지** 명확하게 정리합니다.

---

## 🎯 수정 목적

**Redis 캐시를 비활성화**하여, 데이터베이스에서 직접 조회하는 성능을 측정하기 위함.

이후 Redis 캐시를 활성화한 테스트 결과와 비교하여, **캐시의 성능 향상 효과**를 정량적으로 측정합니다.

---

## 📝 수정 사항

### 파일 경로
```
src/main/java/com/mopl/api/domain/playlist/service/PlaylistService.java
```

### 수정 대상 메서드
```
getPlaylist(UUID playlistId, UUID currentUserId)
```

### 수정 위치 (라인 번호)
**124-125번째 줄**

---

## 🔄 변경 전/후 비교

### ✅ 원본 코드 (캐시 활성화)
```java
@Cacheable(value = "playlistDetail", key = "#playlistId + '_' + #currentUserId")
public PlaylistDto getPlaylist(UUID playlistId, UUID currentUserId) {
    Playlist playlist = playlistRepository.findById(playlistId)
                                          .orElseThrow(() -> PlaylistNotFoundException.withPlaylistId(playlistId));

    if (playlist.getIsDeleted()) {
        throw PlaylistNotFoundException.withPlaylistId(playlistId);
    }

    // ... 이하 비즈니스 로직 ...
}
```

### 🚫 Baseline 테스트용 코드 (캐시 비활성화)
```java
// @Cacheable(value = "playlistDetail", key = "#playlistId + '_' + #currentUserId")  // Temporarily disabled for baseline test
public PlaylistDto getPlaylist(UUID playlistId, UUID currentUserId) {
    Playlist playlist = playlistRepository.findById(playlistId)
                                          .orElseThrow(() -> PlaylistNotFoundException.withPlaylistId(playlistId));

    if (playlist.getIsDeleted()) {
        throw PlaylistNotFoundException.withPlaylistId(playlistId);
    }

    // ... 이하 비즈니스 로직 ...
}
```

---

## 🔍 주석 처리한 이유

`@Cacheable` 어노테이션을 **주석 처리**하면:
- Spring Boot가 메서드 실행 결과를 Redis에 캐싱하지 않음
- 매 API 호출마다 데이터베이스에서 데이터를 직접 조회
- 캐시 없이 순수 데이터베이스 성능만 측정 가능

---

## 🛠️ 적용 방법

### 1. 코드 수정
위의 코드를 참고하여 `PlaylistService.java` 파일의 124번째 줄을 수정합니다.

### 2. 애플리케이션 재빌드
```bash
cd /Users/PARK/Documents/codeit_mopl/sb05-mopl-team5
./gradlew build -x test
```

### 3. 기존 애플리케이션 종료
```bash
pkill -f "java.*mopl-api"
```

### 4. 새로 빌드된 애플리케이션 시작
```bash
java -jar build/libs/mopl-api-0.0.1-SNAPSHOT.jar > app.log 2>&1 &
```

### 5. 정상 동작 확인
```bash
# 12초 정도 대기 후 API 호출 테스트
sleep 12
curl http://localhost:8080/api/playlists/0a6b4418-fe60-11f0-b73c-689ddc77bd50
```

예상 응답:
```json
{
  "id": "0a6b4418-fe60-11f0-b73c-689ddc77bd50",
  "owner": {
    "userId": "...",
    "name": "...",
    "profileImageUrl": "..."
  },
  "title": "...",
  "subscriberCount": 143,
  "subscribedByMe": false,
  ...
}
```

### 6. Redis에 캐시되지 않는지 확인
```bash
# Redis 모든 키 조회
redis-cli KEYS "*"

# 플레이리스트 관련 캐시 조회
redis-cli KEYS "playlistDetail*"
```

**예상 결과**: 빈 결과 또는 "(empty array)" 반환

---

## ✅ 테스트 실행

Baseline 테스트 실행:
```bash
cd /Users/PARK/Documents/codeit_mopl/sb05-mopl-team5
jmeter -n \
  -t load_test/jmeter/playlist_review/playlist_review_redis_cache_15m.jmx \
  -l results_redis_no_cache_15m.jtl
```

---

## 🔄 원상복구 (캐시 활성화로 되돌리기)

Baseline 테스트가 완료되면, Redis 캐시를 다시 활성화합니다.

### 1. 주석 제거
`PlaylistService.java`의 124번째 줄:
```java
@Cacheable(value = "playlistDetail", key = "#playlistId + '_' + #currentUserId")
public PlaylistDto getPlaylist(UUID playlistId, UUID currentUserId) {
```

### 2. 재빌드 및 재시작
```bash
pkill -f "java.*mopl-api"
./gradlew build -x test
java -jar build/libs/mopl-api-0.0.1-SNAPSHOT.jar > app.log 2>&1 &
```

### 3. Redis 캐시 동작 확인
```bash
# API 호출
curl http://localhost:8080/api/playlists/0a6b4418-fe60-11f0-b73c-689ddc77bd50

# Redis에 캐시되었는지 확인
redis-cli KEYS "playlistDetail*"
```

**예상 결과**: `playlistDetail::0a6b4418-fe60-11f0-b73c-689ddc77bd50_null` 같은 키가 생성됨

---

## 📊 수정 효과

| 항목 | Baseline (캐시 비활성화) | With Cache (캐시 활성화) |
|------|-------------------------|-------------------------|
| `@Cacheable` | 주석 처리됨 ❌ | 활성화됨 ✅ |
| 캐시 사용 | 사용 안 함 | Redis 사용 |
| 데이터 조회 방식 | 매번 DB 조회 | 첫 조회만 DB, 이후 캐시 |
| 평균 응답 시간 | 2.77ms (측정됨) | ? ms (측정 필요) |

---

## ⚠️ 주의사항

1. **테스트 전 반드시 재빌드 필요**: 코드 변경 후 `./gradlew build` 실행 필수
2. **애플리케이션 재시작 필요**: 변경사항이 반영되려면 재시작 필수
3. **Redis 상태 확인**: `redis-cli KEYS "*"`로 캐시가 비어있는지 확인
4. **테스트 순서 중요**: Baseline → Cache 순서로 테스트해야 공정한 비교 가능

---

## 📞 문제 해결

### Q1. 코드를 수정했는데 여전히 Redis에 캐시가 생성됩니다.
**A**: 애플리케이션을 재빌드하지 않았거나, 재시작하지 않았을 가능성이 높습니다.
```bash
pkill -f "java.*mopl-api"
./gradlew build -x test
java -jar build/libs/mopl-api-0.0.1-SNAPSHOT.jar > app.log 2>&1 &
```

### Q2. API 호출이 실패합니다 (HTTP 500).
**A**: `app.log` 파일을 확인하여 에러 로그를 확인하세요.
```bash
tail -100 app.log | grep -i error
```

### Q3. JMeter 테스트가 실패합니다.
**A**: 
- API가 정상 동작하는지 먼저 확인: `curl http://localhost:8080/api/playlists/{playlistId}`
- CSV 파일이 올바른 위치에 있는지 확인
- JMX 파일의 경로 설정 확인

---

**마지막 업데이트**: 2026년 1월 31일  
**테스트 환경**: Spring Boot 3.x, Redis 7.x, MySQL 8.x
