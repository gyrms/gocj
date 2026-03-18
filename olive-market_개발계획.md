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

## 1주차 — DB 설계 + 인증

> 목표: 프로젝트 세팅 완료 + 회원가입/로그인 API 동작

### 평일 (헬스 O, 40분)

| 요일 | 할 일 |
|------|------|
| 월 | Spring Boot 프로젝트 생성, 의존성 설정 (build.gradle) |
| 수 | Member Entity + MemberRepository 작성 |
| 금 | JoinRequest / LoginRequest / MemberResponse DTO 작성 |

### 주말

| 일정 | 할 일 |
|------|------|
| 토 오전 | Spring Security + JWT 설정 (SecurityConfig, JwtTokenProvider) |
| 토 오후 | JwtAuthenticationFilter 구현, 토큰 발급 로직 |
| 일 | MemberService + MemberController 구현, Postman으로 회원가입/로그인 테스트 |

### 완료 기준
- [ ] 회원가입 API 동작 (POST /api/auth/join)
- [ ] 로그인 API 동작 → JWT 토큰 반환 (POST /api/auth/login)
- [ ] JWT 토큰으로 인증이 필요한 API 접근 가능

---

## 2주차 — 핵심 비즈니스 로직

> 목표: 상품 API (QueryDSL) + 장바구니 (Redis) + 주문 API 완성

### 평일 (헬스 O, 40분)

| 요일 | 할 일 |
|------|------|
| 월 | Category + Product Entity, ProductRepository 작성 |
| 수 | ProductQueryRepository 작성 (QueryDSL 동적 검색 — 카테고리, 가격범위, 키워드) |
| 금 | Order + OrderItem Entity, OrderRepository 작성 |

### 주말

| 일정 | 할 일 |
|------|------|
| 토 오전 | ProductService + ProductController 구현, 상품 목록/상세 API 테스트 |
| 토 오후 | RedisConfig 설정, CartService 구현 (Redis Hash로 장바구니 관리) |
| 일 오전 | CartController 구현, 장바구니 담기/조회/삭제 테스트 |
| 일 오후 | OrderService + OrderController 구현, 주문 생성/조회 테스트 |

### 완료 기준
- [ ] 상품 목록 API (카테고리/가격범위/키워드 필터 동작)
- [ ] QueryDSL 동적 쿼리 동작 확인
- [ ] 장바구니 Redis 저장/조회/삭제 동작
- [ ] 주문 생성 시 재고 차감 트랜잭션 처리

---

## 3주차 — 테스트 코드 + 배포

> 목표: 테스트 코드 작성 + Docker 라즈베리파이 배포 + README 완성

### 평일 (헬스 O, 40분)

| 요일 | 할 일 |
|------|------|
| 월 | MemberService 단위 테스트 (JUnit5 + Mockito) |
| 수 | ProductService 단위 테스트 (동적 검색 로직 검증) |
| 금 | OrderService 단위 테스트 (재고 차감 트랜잭션 검증) |

### 주말

| 일정 | 할 일 |
|------|------|
| 토 오전 | Dockerfile 작성 (멀티 스테이지 빌드) |
| 토 오후 | docker-compose.yml 작성 (app + mysql + redis) |
| 일 오전 | 라즈베리파이 Docker 설치 + docker compose up 배포 |
| 일 오후 | README.md 작성 (프로젝트 소개, 기술스택, API 목록, 실행 방법) |

### 완료 기준
- [ ] 핵심 Service 단위 테스트 3개 이상
- [ ] 라즈베리파이에서 docker compose up 으로 정상 실행
- [ ] 외부에서 API 호출 가능 (공인 IP 또는 도메인)
- [ ] README.md 정리 완료

---

## 주간 공부 시간 배분

```
월 (헬스 O, 40분)  : 프로젝트 작업
화 (헬스 X, 2시간) : 기술 면접 공부 (JPA / MSA / Spring 등)
수 (헬스 O, 40분)  : 프로젝트 작업
목 (헬스 X, 2시간) : 기술 면접 공부
금 (헬스 O, 40분)  : 프로젝트 작업
토 (5시간)         : 프로젝트 집중 (어려운 것 몰아서)
일 (2시간)         : 프로젝트 마무리 + 복습
```

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
