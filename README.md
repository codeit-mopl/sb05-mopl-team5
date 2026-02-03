# 🎬 모두의 플리 (Mopl)
> **대규모 트래픽을 고려한 글로벌 콘텐츠 평점 및 큐레이션 소셜 플랫폼**

프로젝트 기간: 2025.12.18 ~ 2026.01.29 (6주)  

| 항목 | 내용 |
|------|------|
| **📣 발표 자료** | [발표 자료 PDF](https://drive.google.com/file/d/14XjBs0ICAJCx39b1stmPevsVI-7Lqmu5/view?usp=sharing) |
| **📄 협업 문서** | [Notion 페이지](https://pricey-periodical-112.notion.site/SB5-PART4-TEAM5-2ccadbd27791807b803edafbaa5bb67c) |
| **🔗 배포 링크** | [배포 링크](http://mopl-alb-1504082754.ap-northeast-2.elb.amazonaws.com/#/sign-in) |
| **🎬 시연 영상** | [시연 영상](https://drive.google.com/file/d/15ogVEJtYbS8HRtJ9Mc24WJ-DHge4cH-5/view)|

"모두의 플리"는 흩어져 있는 영화, 드라마, 스포츠 콘텐츠 정보를 통합하여 나만의 플레이리스트를 큐레이팅하고, 실시간으로 타 유저와 소통하며 경험을 확장하는 소셜 서비스입니다.

---

## 🎯 기획 의도

### 1. 파편화된 정보 통합: 여러 플랫폼에 흩어진 콘텐츠 정보를 한곳에서 관리

### 2. 소셜 경험 확장: 단순 시청을 넘어 실시간 채팅과 '같이 보기' 기능을 통한 소통 창구 마련

### 3. 개인화 큐레이션: 나만의 취향이 담긴 플레이리스트 생성 및 공유

---

## 🎯 주요 기능 (Key Features)

| 카테고리 | 기능 |
|------|------|
| **🔐 사용자** | • JWT 기반 인증 및 OAuth 2.0 소셜 로그인 (Google, Kakao)<br>• 이메일 인증 기반 비밀번호 재설정 |
| **📺 콘텐츠** | • TMDB, TheSportsDB Open API 연동 (Spring Batch 수집)<br>• 영화 / TV / 스포츠 콘텐츠 검색 및 필터링 (인기순, 최신순 등)<br>• 별점 및 리뷰 작성 |
| **💬 소셜** | • 실시간 같이 보기 및 채팅 (WebSocket)<br>• 1:1 DM (Direct Message)<br>• 팔로우 / 팔로잉 및 실시간 알림 (SSE) |
| **🎵 플레이리스트** | • 공개 / 비공개 플레이리스트 생성<br>• 타 유저 플레이리스트 구독 및 좋아요 |

## 🛠 Tech Stack

### Frontend

- **Framework**: React 19, TypeScript
- **Build Tool**: Vite
- **State Management**: Zustand
- **Routing**: React Router DOM
- **Form & Validation**: React Hook Form, Zod
- **Styling / UI**: Tailwind CSS, Radix UI
- **Communication**: Axios, WebSocket (STOMP), SSE

### Backend
- **Framework**: Java 17, Spring Boot 3.5.9
- **Data**: Spring Data JPA, Querydsl
- **Batch**: Spring Batch (Content Data 수집 및 처리)
- **Messaging/Real-time**: WebSocket, STOMP, SSE, Redis Pub/Sub
- **Security**: Spring Security, JWT, OAuth2 (Google, Kakao), CSRF Token

### Infrastructure & DevOps
- **Cloud**: AWS ECS
- **CI/CD**: GitHub Actions
- **Container**: Docker, Docker Compose
- **Monitoring**: Grafana, Prometheus

---

## 🏗 System Architecture
<img width="1209" height="811" alt="모플_아키텍처_V1 drawio" src="https://github.com/user-attachments/assets/45bdaff6-5ef9-4e3c-8b42-30f615edb6a4" />  

---
- AWS ECS 기반의 컨테이너 환경 구축  
- GitHub Actions를 이용한 CI/CD 자동화  
- Redis를 활용한 세션 관리 및 캐싱 전략  
- Prometheus & Grafana & Sentry를 통한 전방위 모니터링  
---
