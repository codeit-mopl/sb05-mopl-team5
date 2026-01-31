# ⚠️ Temporary Security Bypass for Load Testing

## 목적
JMeter 부하 테스트를 위해 **일시적으로** Spring Security 인증을 우회하는 설정입니다.

**🚨 경고: 이 변경사항은 절대 프로덕션에 배포하면 안 됩니다!**

---

## 📁 포함된 파일

### `security_bypass_changes.patch`
- **PlaylistController.java**: `@AuthenticationPrincipal(errorOnInvalidType = false)` 추가
- **SecurityConfig.java**: 플레이리스트 API 인증 우회 설정

---

## 🔧 사용 방법

### 1. 패치 적용 (로컬 테스트용)

```bash
# 저장소 루트에서 실행
cd /path/to/sb05-mopl-team5

# 패치 적용
git apply load_test/baseline/temp_security_bypass/security_bypass_changes.patch

# 또는 stash에서 직접 적용
git stash apply stash@{0}  # stash 번호는 'git stash list'로 확인
```

### 2. 부하 테스트 실행

```bash
# Redis 캐시 테스트
jmeter -n -t load_test/jmeter/playlist_review/playlist_review_redis_cache_15m.jmx \
  -l results.jtl -e -o report/

# 구독 경쟁 조건 테스트  
jmeter -n -t load_test/jmeter/subscription/subscription_race_condition_15m.jmx \
  -l results.jtl
```

### 3. 테스트 완료 후 변경사항 제거

```bash
# 변경사항 되돌리기
git restore src/main/java/com/mopl/api/domain/playlist/controller/PlaylistController.java
git restore src/main/java/com/mopl/api/global/config/security/SecurityConfig.java

# 또는 stash 삭제
git stash drop stash@{0}
```

---

## ⚡ 변경 내용 상세

### SecurityConfig.java
```java
// 인증 없이 접근 허용
.requestMatchers(HttpMethod.GET, "/api/playlists", "/api/playlists/*")
    .permitAll()
.requestMatchers(HttpMethod.POST, "/api/playlists/*/subscription")
    .permitAll()
.requestMatchers(HttpMethod.DELETE, "/api/playlists/*/subscription")
    .permitAll()
```

**위험성:**
- 모든 사용자가 인증 없이 플레이리스트 조회/구독 가능
- 악의적인 사용자가 데이터 조작 가능

### PlaylistController.java
```java
// user가 null일 경우 랜덤 UUID 생성
@AuthenticationPrincipal(errorOnInvalidType = false) CustomUserDetails user
UUID userId = (user != null) ? user.getUserDto().id() : UUID.randomUUID();
```

**위험성:**
- 익명 사용자도 구독/구독 취소 가능
- 랜덤 UUID로 인한 데이터 무결성 파괴
- 실제 사용자 추적 불가능

---

## 🔐 보안 체크리스트

- [ ] 로컬 개발 환경에서만 사용
- [ ] 테스트 완료 후 즉시 변경사항 제거
- [ ] Git 커밋에 포함하지 않음
- [ ] 프로덕션/스테이징 서버에 배포하지 않음
- [ ] PR에 이 변경사항 포함하지 않음

---

## 📊 대안: 실제 JWT 토큰 사용

보안 우회 대신 실제 JWT 토큰을 생성하여 테스트하는 방법:

```bash
# JWT 토큰 생성 (서버 실행 필요)
cd load_test/sql/playlist_review
python3 generate_jwt_tokens.py --count 1000 --output user_tokens.csv

# JMeter에서 user_tokens.csv의 실제 토큰 사용
# HTTP Header Manager에 Authorization: Bearer ${token} 추가
```

**장점:**
- 실제 프로덕션 환경과 동일한 조건으로 테스트
- 보안 설정 변경 불필요
- 인증 오버헤드 포함한 정확한 성능 측정

---

## 🗑️ 파일 정리

테스트 완료 후 이 디렉토리는 삭제해도 무방합니다:

```bash
rm -rf load_test/baseline/temp_security_bypass/
git stash drop stash@{0}  # stash도 함께 삭제
```

---

**마지막 업데이트**: 2026-01-31  
**Git Stash**: `stash@{0}: On perf/#158: Temporary load test changes - security bypass`
