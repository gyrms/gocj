# olive-market 프로젝트 개발 계획

> 목표: CJ올리브영 이력서 제출용 포트폴리오 프로젝트
> 기간: 3주 완성
> 기술스택: Java 17 · Spring Boot 3.x · Spring Security + JWT · JPA · QueryDSL · Redis · MySQL · Docker

---

## 프로젝트 구조

```
src/main/java/com/yhg/olivemarket/
│
├── domain/
│   ├── member/
│   │   ├── entity/Member.java
│   │   ├── repository/MemberRepository.java
│   │   ├── service/MemberService.java
│   │   ├── controller/MemberController.java
│   │   └── dto/
│   │       ├── request/JoinRequest.java
│   │       ├── request/LoginRequest.java
│   │       └── response/MemberResponse.java
│   │
│   ├── product/
│   │   ├── entity/Product.java
│   │   ├── entity/Category.java
│   │   ├── repository/ProductRepository.java
│   │   ├── repository/ProductQueryRepository.java   ← QueryDSL
│   │   ├── service/ProductService.java
│   │   ├── controller/ProductController.java
│   │   └── dto/
│   │       ├── request/CreateProductRequest.java
│   │       ├── request/ProductSearchRequest.java    ← 동적 검색 조건
│   │       └── response/ProductResponse.java
│   │
│   ├── cart/
│   │   ├── service/CartService.java                 ← Redis
│   │   ├── controller/CartController.java
│   │   └── dto/
│   │       ├── request/AddCartRequest.java
│   │       └── response/CartResponse.java
│   │
│   └── order/
│       ├── entity/Order.java
│       ├── entity/OrderItem.java
│       ├── repository/OrderRepository.java
│       ├── service/OrderService.java
│       ├── controller/OrderController.java
│       └── dto/
│           ├── request/CreateOrderRequest.java
│           └── response/OrderResponse.java
│
├── global/
│   ├── auth/
│   │   ├── JwtTokenProvider.java
│   │   ├── JwtAuthenticationFilter.java
│   │   └── SecurityConfig.java
│   ├── config/
│   │   ├── QueryDslConfig.java
│   │   └── RedisConfig.java
│   └── exception/
│       ├── GlobalExceptionHandler.java
│       ├── CustomException.java
│       └── ErrorCode.java
│
└── OliveMarketApplication.java
```

---

## ERD

```
Member (회원)
  id, email, password, name, role, created_at

Category (카테고리)
  id, name

Product (상품)
  id, name, price, stock, description, category_id, created_at

Order (주문)
  id, member_id, total_price, status, created_at

OrderItem (주문 상품)
  id, order_id, product_id, quantity, price
```

---

## API 목록

| 도메인 | Method | URI | 인증 | 설명 |
|--------|--------|-----|------|------|
| 인증 | POST | /api/auth/join | X | 회원가입 |
| 인증 | POST | /api/auth/login | X | 로그인 (JWT 발급) |
| 상품 | GET | /api/products | X | 상품 목록 (QueryDSL 동적 검색) |
| 상품 | GET | /api/products/{id} | X | 상품 상세 |
| 상품 | POST | /api/products | O | 상품 등록 (관리자) |
| 장바구니 | POST | /api/cart | O | 장바구니 담기 (Redis) |
| 장바구니 | GET | /api/cart | O | 장바구니 조회 |
| 장바구니 | DELETE | /api/cart/{productId} | O | 장바구니 삭제 |
| 주문 | POST | /api/orders | O | 주문 생성 |
| 주문 | GET | /api/orders | O | 내 주문 목록 |
| 주문 | GET | /api/orders/{id} | O | 주문 상세 |

---

## 1주차 — 프로젝트 세팅 + 인증

> 목표: 프로젝트 세팅 완료 + 회원가입/로그인 API 동작

### 월요일 (헬스 O / 40분)
- Spring Boot 프로젝트 생성 (start.spring.io)
- build.gradle 의존성 추가
  - spring-boot-starter-web
  - spring-boot-starter-data-jpa
  - spring-boot-starter-security
  - spring-boot-starter-data-redis
  - querydsl-jpa
  - jjwt (JWT 라이브러리)
  - mysql-connector-j
  - lombok

### 화요일 (헬스 X / 2시간) — 기술 면접 공부
- JPA 심화 학습 (`JPA_심화학습.md` 참고)
  - 영속성 컨텍스트, N+1 문제, 지연로딩, @Transactional 원리

### 수요일 (헬스 O / 40분)
- Member Entity 작성 (id, email, password, name, role, created_at)
- MemberRepository 작성 (JpaRepository 상속)
- JoinRequest / LoginRequest / MemberResponse DTO 작성

### 목요일 (헬스 X / 2시간) — Docker 공부
- `Docker_기초개념.md` 정독
  - 이미지 / 컨테이너 / Dockerfile / Docker Compose 개념 숙지

### 금요일 (헬스 O / 40분)
- application.yml 설정 (DB, Redis, JWT)
- QueryDslConfig, RedisConfig 작성
- GlobalExceptionHandler, CustomException, ErrorCode 작성

### 토요일 (5시간)
- 오전 (2시간): SecurityConfig 작성, JwtTokenProvider 구현 (토큰 생성/검증)
- 오후 (3시간): JwtAuthenticationFilter 구현, MemberService + MemberController 작성
  - POST /api/auth/join (회원가입)
  - POST /api/auth/login (로그인 → JWT 반환)

### 일요일 (2시간)
- Postman으로 회원가입 / 로그인 API 전체 테스트
- 오류 수정 및 코드 정리
- git commit & push

### 완료 기준
- [ ] POST /api/auth/join 회원가입 동작
- [ ] POST /api/auth/login 로그인 → JWT 토큰 반환
- [ ] JWT 헤더 없이 인증 API 접근 시 401 반환
- [ ] JWT 포함 시 인증 API 정상 접근

---

## 2주차 — 핵심 비즈니스 로직

> 목표: 상품 API (QueryDSL) + 장바구니 (Redis) + 주문 API 완성

### 월요일 (헬스 O / 40분)
- Category Entity 작성 (id, name)
- Product Entity 작성 (id, name, price, stock, description, category_id, created_at)
- ProductRepository 작성

### 화요일 (헬스 X / 2시간) — 기술 면접 공부
- MSA 개념 학습 (`MSA_개념학습.md` 참고)
  - MSA vs 모놀리식, 서비스 간 통신, 분산 트랜잭션, Saga 패턴

### 수요일 (헬스 O / 40분)
- ProductQueryRepository 작성 (QueryDSL)
  - 카테고리 필터 (BooleanExpression)
  - 가격 범위 필터 (minPrice ~ maxPrice)
  - 키워드 검색 (상품명 like)
  - 조건 조합 동적 쿼리

### 목요일 (헬스 X / 2시간) — Docker 공부
- `Docker_실습가이드.md` 정독
  - Dockerfile 멀티 스테이지 빌드 구조 이해
  - docker-compose.yml 작성 방법 미리 파악

### 금요일 (헬스 O / 40분)
- Order Entity 작성 (id, member_id, total_price, status, created_at)
- OrderItem Entity 작성 (id, order_id, product_id, quantity, price)
- OrderRepository 작성

### 토요일 (5시간)
- 오전 (2시간): ProductService + ProductController 구현
  - GET /api/products (동적 검색 — 카테고리, 가격범위, 키워드)
  - GET /api/products/{id} (상품 상세)
  - POST /api/products (상품 등록, 관리자)
- 오후 (3시간): CartService 구현 (Redis Hash 기반)
  - POST /api/cart (장바구니 담기 + TTL 설정)
  - GET /api/cart (장바구니 조회)
  - DELETE /api/cart/{productId} (장바구니 삭제)

### 일요일 (2시간)
- OrderService + OrderController 구현
  - POST /api/orders (주문 생성 + 재고 차감 @Transactional)
  - GET /api/orders (내 주문 목록)
  - GET /api/orders/{id} (주문 상세)
- Postman으로 전체 API 테스트
- git commit & push

### 완료 기준
- [ ] 상품 목록 API 카테고리/가격범위/키워드 필터 동작
- [ ] QueryDSL 동적 쿼리 정상 동작
- [ ] 장바구니 Redis 저장/조회/삭제 동작
- [ ] 주문 생성 시 재고 차감 + 트랜잭션 처리

---

## 3주차 — 테스트 코드 + Docker 배포

> 목표: 테스트 코드 작성 + Docker 라즈베리파이 배포 + README 완성

### 월요일 (헬스 O / 40분)
- MemberService 단위 테스트 (JUnit5 + Mockito)
  - 회원가입 성공 / 이메일 중복 예외 테스트

### 화요일 (헬스 X / 2시간) — 기술 면접 공부
- Spring 심화 학습 (`Spring_개념학습.md` 참고)
  - IoC/DI, AOP, @Transactional 원리, Spring Security Filter Chain

### 수요일 (헬스 O / 40분)
- ProductService 단위 테스트 (JUnit5 + Mockito)
  - 상품 조회 성공 / 존재하지 않는 상품 예외 테스트
  - QueryDSL 동적 검색 조건 테스트

### 목요일 (헬스 X / 2시간) — Docker 공부
- `Docker_면접대비.md` 숙지
  - 면접 예상 질문 12개 답변 직접 소리 내어 연습

### 금요일 (헬스 O / 40분)
- OrderService 단위 테스트 (JUnit5 + Mockito)
  - 주문 생성 성공 / 재고 부족 예외 테스트

### 토요일 (5시간)
- 오전 (2시간): Dockerfile 작성 (멀티 스테이지 빌드), .dockerignore 작성
- 오후 (3시간): docker-compose.yml 작성 (app + mysql + redis)
  - healthcheck 설정 (MySQL 기동 후 앱 시작)
  - Volume 설정 (MySQL, Redis 데이터 영속성)
  - 환경변수 설정

### 일요일 (2시간)
- 라즈베리파이에서 Docker 설치 확인
- git pull → docker compose up --build -d 배포
- 외부에서 API 호출 테스트
- README.md 작성
  - 프로젝트 소개, 기술스택, ERD, API 목록, 실행 방법, 어필 포인트
- git commit & push

### 완료 기준
- [ ] 핵심 Service 단위 테스트 3개 이상 작성
- [ ] 라즈베리파이 docker compose up 정상 실행
- [ ] 외부에서 API 호출 가능
- [ ] README.md 완성

---

## 주간 공부 시간 배분 요약

| 요일 | 헬스 | 시간 | 내용 |
|------|------|------|------|
| 월 | O | 40분 | 프로젝트 작업 |
| 화 | X | 2시간 | 기술 면접 공부 (JPA → MSA → Spring) |
| 수 | O | 40분 | 프로젝트 작업 |
| 목 | X | 2시간 | Docker 공부 (기초 → 실습 → 면접대비) |
| 금 | O | 40분 | 프로젝트 작업 |
| 토 | 선택 | 5시간 | 프로젝트 집중 (어려운 것 몰아서) |
| 일 | X | 2시간 | 마무리 + 테스트 + git push |

---

## 이력서 어필 포인트 (완성 후)

| 기술 | 어필 내용 |
|------|----------|
| QueryDSL | 카테고리·가격범위·키워드 동적 검색 구현 |
| Spring Security + JWT | 토큰 기반 인증/인가 처리 |
| Redis | 장바구니 TTL 관리, 세션 캐싱 |
| JUnit5 + Mockito | Service 계층 단위 테스트 작성 |
| Docker | 멀티 스테이지 빌드, docker-compose 운영 |
| 라즈베리파이 | 실서버 배포 및 운영 경험 |

---

## 체크리스트

### 1주차
- [ ] 프로젝트 생성 및 의존성 설정
- [ ] Member Entity + Repository
- [ ] Spring Security + JWT 설정
- [ ] 회원가입 / 로그인 API 동작

### 2주차
- [ ] Product + Category Entity + QueryDSL 동적 검색
- [ ] Redis 장바구니
- [ ] Order + OrderItem 주문 API

### 3주차
- [ ] JUnit5 + Mockito 테스트 코드
- [ ] Dockerfile + docker-compose.yml
- [ ] 라즈베리파이 배포
- [ ] README.md 완성
