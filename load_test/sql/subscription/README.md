# 구독 경쟁 상태 부하 테스트

## 개요
이 디렉토리는 JMeter와 MySQL을 사용하여 플레이리스트 구독자 수 증가/감소에 대한 경쟁 상태(Race Condition) 수정을 검증하는 부하 테스트 스크립트를 포함합니다.

## 문제 정의
여러 사용자가 동시에 플레이리스트를 구독하거나 구독 취소할 때, 경쟁 상태로 인해 `subscriber_count` 필드가 데이터베이스의 실제 구독 수와 일치하지 않을 수 있습니다.

## 해결 방법
경쟁 상태는 **원자적 데이터베이스 연산**을 사용하여 해결됩니다:
- `PlaylistRepository.incrementSubscriberCount(UUID playlistId)`
- `PlaylistRepository.decrementSubscriberCount(UUID playlistId)`

이 메서드들은 읽기-수정-쓰기 패턴 대신 직접적인 SQL `UPDATE` 문을 사용하여 증가/감소 연산을 수행합니다.

## 테스트 구조

### JMeter 테스트 파일
- `subscription_race_condition_3m.jmx` - 3분 부하 테스트 (동시 사용자 200명)
- `subscription_race_condition_15m.jmx` - 15분 부하 테스트 (동시 사용자 200명)

### SQL 스크립트
- `generate_subscription_test_data.sql` - 테스트 데이터 생성 (플레이리스트 1,000개, 사용자 1,000명)
- `reset_subscription_data.sql` - 테스트 후 테스트 데이터 정리
- `validate_subscriber_count.sql` - 구독자 수 일관성 검증

## 테스트 실행 방법

### 전제 조건
1. MySQL 데이터베이스 실행 중
2. JMeter 5.6.3+ 설치
3. 애플리케이션 서버가 `http://localhost:8080`에서 실행 중

### 1단계: 테스트 데이터 생성
```bash
mysql -uroot -p mopl < load_test/sql/subscription/generate_subscription_test_data.sql 2>&1
```

**출력 결과:**
- "Subscription Test Playlist" 접두사를 가진 1,000개의 테스트 플레이리스트 생성
- `subtest*@mopl.test` 이메일 패턴을 가진 1,000명의 테스트 사용자 생성
- 초기 구독 생성 (플레이리스트당 0-50개)
- CSV 파일 내보내기:
  - `/tmp/subscription_playlist_ids.csv`
  - `/tmp/subscription_user_tokens.csv`

### 2단계: CSV 파일 복사
```bash
cp /tmp/subscription_playlist_ids.csv load_test/data/generater/
cp /tmp/subscription_user_tokens.csv load_test/data/generater/
```

**참고:** CSV의 JWT 토큰은 더미 값입니다. 인증이 활성화된 실제 테스트의 경우, 테스트 사용자를 위한 실제 JWT 토큰을 생성해야 합니다.

### 3단계: JMeter 테스트 실행

#### 3분 테스트 (빠른 검증)
```bash
cd load_test/jmeter/subscription
jmeter -n -t subscription_race_condition_3m.jmx -l results_3m.jtl
```

#### 15분 테스트 (종합 테스트)
```bash
cd load_test/jmeter/subscription
jmeter -n -t subscription_race_condition_15m.jmx -l results_15m.jtl
```

**테스트 구성:**
- 스레드: 동시 사용자 200명
- Ramp-up: 10초
- 지속 시간: 180초 (3분) / 900초 (15분)
- Think time: 요청 간 100ms
- 동작: 랜덤 구독/구독 취소 작업

### 4단계: 결과 검증
```bash
mysql -uroot -p mopl < load_test/sql/subscription/validate_subscriber_count.sql 2>&1
```

**예상 출력:**
- 경쟁 상태가 적절히 수정되었다면 불일치 없음
- 100% 일관성 비율
- 불일치 플레이리스트 목록 비어있음

### 5단계: 정리
```bash
mysql -uroot -p mopl < load_test/sql/subscription/reset_subscription_data.sql 2>&1
```

## 테스트 엔드포인트

### 구독
- **엔드포인트:** `POST /api/playlists/{playlistId}/subscription`
- **인증:** Bearer 토큰 필요
- **예상 응답:** `200`, `201`, `409` (이미 구독함)

### 구독 취소
- **엔드포인트:** `DELETE /api/playlists/{playlistId}/subscription`
- **인증:** Bearer 토큰 필요
- **예상 응답:** `200`, `204`, `404` (구독하지 않음)

## 성공 기준

### ✅ 테스트 통과 조건:
1. 모든 JMeter 요청이 예상 응답 코드를 반환
2. 검증 쿼리에서 100% 일관성 표시
3. 모든 플레이리스트에 대해 `subscriber_count`가 실제 구독 수와 일치
4. 애플리케이션 로그에 데이터베이스 교착 상태나 오류 없음

### ❌ 테스트 실패 조건:
1. 검증 쿼리에서 불일치 표시
2. `subscriber_count`가 실제 구독 수와 일치하지 않음
3. JMeter 결과에서 높은 오류율
4. 데이터베이스 교착 상태 또는 트랜잭션 오류

## 문제 해결

### 문제: CSV 파일을 찾을 수 없음
**해결책:** 1단계와 2단계를 실행했는지 확인하세요. CSV 파일이 `load_test/data/generater/`에 존재하는지 확인하세요.

### 문제: 인증 오류 (401)
**해결책:** CSV의 JWT 토큰은 더미입니다. 다음 중 하나를 선택하세요:
- 부하 테스트를 위해 인증 비활성화
- 테스트 사용자를 위한 실제 JWT 토큰 생성
- 유효한 토큰으로 CSV 업데이트

### 문제: 높은 오류율 (409 Conflict)
**예상:** 일부 409 오류는 정상입니다 (사용자가 이미 구독함). 오류율이 합리적인지 확인하세요 (<30%)

### 문제: 데이터베이스 연결 오류
**해결책:**
- 데이터베이스 연결 풀 크기 증가
- `application.yaml` 데이터소스 설정 확인
- 테스트 중 데이터베이스 연결 수 모니터링

## 성능 지표

### 모니터링할 주요 지표:
1. **처리량:** 초당 요청 수
2. **응답 시간:** 평균, 95 백분위수, 최대
3. **오류율:** <5% 이어야 함 (예상되는 409/404 제외)
4. **데이터베이스 일관성:** 100%이어야 함
5. **CPU/메모리:** 애플리케이션 서버 리소스 사용량

## 관련 문서
- 추가 문서는 준비 중입니다

## 참고 사항
- 테스트 데이터는 쉬운 정리를 위해 특정 명명 패턴으로 격리됩니다
- 테스트는 충돌 없이 여러 번 실행될 수 있습니다
- 데이터베이스 무결성을 유지하기 위해 항상 테스트 후 검증하고 정리하세요
