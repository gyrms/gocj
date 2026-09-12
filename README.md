# 윤효근 · Backend Engineer

> 서로 다른 시스템 사이에서 데이터가 어긋나지 않게 만드는 일을 8년째 하고 있습니다.
> 호텔 PMS · 부킹엔진 · 채널 매니저(OTA 연동) · B2C 예약 플랫폼을 개발해왔습니다.

`Java` `Spring Boot` `JPA / QueryDSL` `Redis` `Oracle / MySQL` `AWS` `Jenkins`

---

## 📄 이력서

| 문서 | 용도 |
|---|---|
| **[AI이력서2.md](AI이력서2.md)** | 숙박·여행 플랫폼 지원용 (도메인 경험 중심) |
| **[AI이력서.md](AI이력서.md)** | 커머스·일반 지원용 |
| [상세기술경력서.md](상세기술경력서.md) | 프로젝트별 상세 기술 서술 |

> 지난 버전은 [`archive/resume/`](archive/resume) 에 보관돼 있습니다.

---

## 🛠 대표 프로젝트

### [olive-market](https://github.com/gyrms/olive-market) — 이커머스 백엔드 API
`Java 17` `Spring Boot 3.2` `JPA` `QueryDSL` `Spring Security + JWT` `Redis` `MySQL` `Docker`

회원 · 상품 · 장바구니 · 주문 4개 도메인을 도메인형 패키지 구조로 설계한 개인 프로젝트입니다.
실무에서 다루지 못한 최신 스택을 직접 경험하기 위해 진행했습니다.

- JWT + Spring Security 기반 무상태 인증 (실무의 세션 방식과 트레이드오프 비교)
- 주문 확정 전 임시 데이터인 장바구니를 Redis에 저장
- QueryDSL로 상품 검색·필터 동적 쿼리 처리
- GlobalExceptionHandler + ErrorCode로 예외 응답 통일, JUnit5 테스트, Swagger 문서화

> 📌 소스는 **별도 저장소**에 있습니다 → https://github.com/gyrms/olive-market
> [개발 계획 문서](archive/olive-market_개발계획.md)

---

## 📚 학습 기록

실무에서 쓰지 않았거나 더 깊이 보고 싶었던 주제를 정리한 문서입니다.

| 주제 | 문서 |
|---|---|
| JPA | [심화학습](JPA_심화학습.md) · [디테일](JPA_심화학습_1디테일.md) |
| Spring | [개념학습](Spring_개념학습.md) |
| Redis | [심화학습](Redis_심화학습.md) |
| Kafka | [기초학습](Kafka_기초학습.md) |
| MSA | [개념학습](MSA_개념학습.md) |
| Docker | [기초개념](Docker_기초개념.md) · [실습가이드](Docker_실습가이드.md) · [면접대비](Docker_면접대비.md) |
| Java | [면접대비](java_면접대비.md) |

---

## 📫 Contact

- GitHub: [@gyrms](https://github.com/gyrms)
