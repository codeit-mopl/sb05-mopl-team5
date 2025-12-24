# mopl 백엔드 코딩 컨벤션 (Backend Coding Guidelines)

이 문서는 **mopl 프로젝트**에서 CodeRabbit이 PR 리뷰 시 준수해야 할 **Strict Rules**입니다.
이 규칙을 위반한 코드는 수정 제안(Change Request) 대상으로 간주해주세요.

---

## 1. 🧩 네이밍 규칙 (Naming Conventions)

### 1.1 기본 포맷
| 대상 | 규칙 | 예시 |
| :--- | :--- | :--- |
| **변수 / 필드** | `camelCase` | `userEmail`, `createdAt` |
| **클래스 / Record** | `UpperCamelCase` (명사) | `UserEmail`, `OrderController` |
| **함수 (Method)** | `camelCase` (동사 시작) | `getUserId()`, `isNormal()` |
| **패키지** | **소문자** (단어 구분 없음) | `frontend`, `useremail` (O) / `userEmail` (X) |
| **ENUM / 상수** | `UPPER_SNAKE_CASE` | `NORMAL_STATUS`, `MAX_RETRY_COUNT` |
| **URL / 파일명** | `kebab-case` | `/user-email-page`, `user-profile.html` |

### 1.2 상세 네이밍 가이드
- **중복 지양**: 객체 이름을 메서드 내에 중복하지 않음.
    - ✅ `line.getLength()`
    - ❌ `line.getLineLength()`
- **컬렉션 명시**: 복수형(`s`) 또는 타입 명시(`Map`, `List`) 사용.
    - ✅ `List<Long> ids`, `Map<User, Integer> userToIdMap`
- **금지 단어**: 중의적인 단어 사용 금지.
    - ❌ `event`, `design`
- **명확한 의도**: 줄임말보다는 의도가 드러나는 짧은 이름 사용.
    - ✅ `getUser()`
    - ❌ `retrieveUser()`
- **부수효과 명시**: 단순 조회가 아닌 로직이 포함된 경우 의도를 명시.
    - ✅ `getOrCreateOrder()` (없으면 생성한다는 뜻 내포)
    - ❌ `getOrder()` (내부에서 생성까지 한다면 오해 소지 있음)

### 1.3 날짜/시간 타입 접미사
- `LocalDateTime` 타입: `xxxAt` (예: `createdAt`, `updatedAt`)
- `LocalDate` 타입: `xxxDt` (예: `birthDt`, `targetDt`)

---

## 2. 🧱 JPA & Service 레이어 규칙

### 2.1 Repository (JPA)
- 조회 메서드는 반드시 **`find`**로 시작합니다.
- 예: `findByEmail()`, `findAll()`

### 2.2 Service
- 조회 메서드는 반드시 **`get`**으로 시작합니다.
- 예: `getUser()`, `getOrderList()`

---

## 3. 🧭 계층별 메서드 네이밍 매핑 (중요)

Controller와 Service의 메서드명은 아래 규칙에 따라 서로 다르게 명명합니다.

| 기능 | Controller 메서드명 | Service 메서드명 |
| :--- | :--- | :--- |
| **목록 조회** | `orderList()` | `getOrders()` |
| **단건 상세** | `orderDetails()` | `getOrder()` |
| **등록** | `orderAdd()` | `addOrder()` |
| **수정** | `orderModify()` | `modifyOrder()` |
| **삭제** | `orderRemove()` | `removeOrder()` |
| **등록/수정/삭제 통합** | `orderSave()` | `saveOrder()` |

> **Reviewer Note:** Controller에서 `getOrders`를 쓰거나 Service에서 `orderList`를 쓰는 경우 지적해주세요.

---

## 4. 🧩 DTO 관리 규칙

### 4.1 구조 및 파일 원칙
- **Inner Record 금지**: DTO는 반드시 개별 파일(`.java`)로 생성합니다.
- **파일명 일치**: Swagger 스키마 이름과 파일명이 일치해야 합니다.
- **불변 객체 권장**: 가급적 `record` 타입을 사용합니다.

### 4.2 패키지 위치 (엄수)
DTO는 반드시 기능에 따라 아래 패키지로 분리합니다.
- `.../dto/request/`: 클라이언트 요청 (POST, PUT 바디)
- `.../dto/response/`: 클라이언트 응답 (GET 결과)
- `.../dto/data/`: 내부 공통 데이터 (Response에 직접 반환 가능)

### 4.3 DTO 네이밍 예시
- `ArticleDto`: 단건 상세 정보
- `CursorPageResponseArticleDto`: 목록 조회 결과
- `ArticleRestoreResultDto`: 작업 결과 반환

---

## 5. 📂 디렉토리 구조 (Directory Structure)

프로젝트는 **도메인형 구조**를 따릅니다.

---

## 6. 🎨 코드 포맷팅 (Style Guide)

Google Java Style Guide를 기반으로 아래 항목을 **Override** 하여 적용합니다.

1.  **들여쓰기 (Indent)**:
    - Block Indent: **4 spaces** (Google 기본 2에서 변경)
    - Continuation Indent: **min +8 spaces** (줄바꿈 시)
2.  **Line Length**:
    - **120자** 제한 (Google 기본 100에서 변경)
3.  **줄바꿈 (Wrapping)**:
    - Chained method calls: **Wrap always** (메서드 체이닝 시 무조건 줄바꿈)
    - Align when multiline: 활성화
4.  **빈 줄 (Empty Line)**:
    - 가독성을 위해 논리적 단위 사이에 자유롭게 사용 가능.
    - 클래스 첫 멤버 앞의 빈 줄은 강제하지 않음.

```java
// Good Style Example
public Order getOrCreateOrder() {
    if (order == null) {
        order = new Order();
    }
    return order;
}