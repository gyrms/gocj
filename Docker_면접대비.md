# Docker 면접 대비

---

## Q1. Docker가 무엇인지 설명해주세요.

**답변:**
Docker는 애플리케이션을 컨테이너 단위로 패키징하여 어떤 환경에서도 동일하게 실행할 수 있게 해주는 컨테이너화 플랫폼입니다.

개발 환경과 운영 환경의 차이로 발생하는 "내 컴퓨터에서는 됩니다" 문제를 해결하며, 애플리케이션과 그 실행에 필요한 모든 의존성을 하나의 이미지로 묶어 배포할 수 있습니다.

---

## Q2. Docker 컨테이너와 가상머신(VM)의 차이는 무엇인가요?

**답변:**

| 항목 | VM | Docker 컨테이너 |
|------|----|----|
| 가상화 대상 | 하드웨어 전체 | OS 프로세스 수준 |
| Guest OS | 포함 (수 GB) | 없음 (Host OS 공유) |
| 크기 | 수 GB | 수 MB ~ 수백 MB |
| 시작 시간 | 수 분 | 수 초 |
| 격리 수준 | 강함 | 적당함 |

VM은 하이퍼바이저 위에 Guest OS 전체를 올리기 때문에 무겁습니다.
컨테이너는 Host OS의 커널을 공유하고 프로세스 수준에서 격리하기 때문에 훨씬 가볍고 빠릅니다.

---

## Q3. Docker 이미지와 컨테이너의 차이는 무엇인가요?

**답변:**
이미지는 컨테이너를 만들기 위한 **읽기 전용 템플릿**이고, 컨테이너는 이미지를 **실행한 인스턴스**입니다.

붕어빵에 비유하면 이미지는 붕어빵 틀이고, 컨테이너는 틀로 찍어낸 붕어빵입니다.
하나의 이미지로 여러 개의 컨테이너를 실행할 수 있습니다.

---

## Q4. Dockerfile의 레이어 구조란 무엇인가요?

**답변:**
Dockerfile의 각 명령어(FROM, COPY, RUN 등)는 실행될 때마다 새로운 레이어를 생성합니다.
Docker는 이 레이어를 캐싱하여, 변경되지 않은 레이어는 재사용합니다.

```dockerfile
FROM openjdk:17        # 레이어 1
WORKDIR /app           # 레이어 2
COPY . .               # 레이어 3 ← 소스 변경 시 여기부터 다시 빌드
RUN gradle bootJar     # 레이어 4
```

때문에 자주 변경되는 명령어는 Dockerfile 아래쪽에 배치하는 것이 빌드 속도에 유리합니다.

---

## Q5. 멀티 스테이지 빌드란 무엇이고 왜 사용하나요?

**답변:**
하나의 Dockerfile 안에서 여러 FROM 단계를 사용하는 빌드 방식입니다.

주로 빌드 환경과 실행 환경을 분리하여 **최종 이미지 크기를 줄이기 위해** 사용합니다.

```dockerfile
# 1단계: 빌드 (Gradle 포함 → 크기 큼)
FROM gradle:8.5-jdk17 AS builder
WORKDIR /app
COPY . .
RUN gradle bootJar

# 2단계: 실행 (jar 파일만 복사 → 크기 작음)
FROM openjdk:17-slim
COPY --from=builder /app/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

빌드 도구(Gradle)와 소스코드가 최종 이미지에 포함되지 않아 이미지 크기가 크게 줄어듭니다.

---

## Q6. Docker Compose를 사용하는 이유는 무엇인가요?

**답변:**
여러 컨테이너를 단일 YAML 파일로 정의하고 한 번에 관리하기 위해 사용합니다.

Spring Boot + MySQL + Redis처럼 여러 서비스가 필요한 경우, 각각 docker run 명령어로 실행하면 관리가 복잡해집니다.
Docker Compose를 사용하면 서비스 간 네트워크 설정, 의존성 순서(depends_on), 환경변수 관리를 한 파일에서 처리할 수 있습니다.

```bash
docker compose up -d    # 전체 서비스 실행
docker compose down     # 전체 서비스 중지
```

---

## Q7. Docker Volume이 필요한 이유는 무엇인가요?

**답변:**
컨테이너는 기본적으로 Stateless(무상태)입니다. 컨테이너를 삭제하면 내부 데이터도 함께 사라집니다.

MySQL처럼 데이터 영속성이 필요한 경우, Volume을 사용하여 컨테이너 외부(Host OS)에 데이터를 저장해야 합니다.

```yaml
volumes:
  - mysql-data:/var/lib/mysql  # mysql-data 볼륨에 DB 데이터 저장
```

컨테이너를 삭제하고 재생성해도 볼륨에 저장된 데이터는 유지됩니다.

---

## Q8. 컨테이너 간 통신은 어떻게 이루어지나요?

**답변:**
Docker Compose로 실행한 컨테이너들은 기본적으로 동일한 네트워크에 속하며, **서비스 이름**을 호스트명으로 사용하여 서로 통신할 수 있습니다.

예를 들어 `app` 컨테이너에서 `mysql` 컨테이너에 접근할 때:
```yaml
SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/olivemarket
#                                   ↑
#                          서비스 이름이 호스트명
```

IP 주소 대신 서비스 이름을 사용하므로 컨테이너가 재시작되어 IP가 바뀌어도 영향을 받지 않습니다.

---

## Q9. docker run -p 8080:8080 에서 포트 두 개의 의미는?

**답변:**
```
-p [호스트포트]:[컨테이너포트]
-p 8080:8080
   ↑           ↑
내 컴퓨터     컨테이너 내부
```

왼쪽은 외부에서 접근할 호스트 포트, 오른쪽은 컨테이너 내부 포트입니다.
`localhost:8080`으로 접근하면 컨테이너의 8080 포트로 트래픽이 전달됩니다.

---

## Q10. 컨테이너가 stateless하다는 의미는?

**답변:**
컨테이너 내부에 저장된 데이터는 컨테이너 생명주기에 종속된다는 의미입니다.
컨테이너를 중지하거나 삭제하면 내부에서 생성된 파일이나 데이터는 사라집니다.

이 특성 때문에:
- DB 데이터는 Volume으로 외부에 저장
- 설정값은 환경변수나 외부 설정 파일로 관리
- 앱 자체는 상태를 갖지 않도록(stateless) 설계

이 원칙이 지켜져야 컨테이너를 여러 개 띄우거나(스케일 아웃) 재시작해도 정상 동작합니다.

---

## Q11. 이미지를 최적화하는 방법은?

**답변:**

1. **멀티 스테이지 빌드** — 빌드 도구를 최종 이미지에서 제거
2. **경량 베이스 이미지 사용** — `openjdk:17` 대신 `openjdk:17-slim` 또는 `eclipse-temurin:17-alpine`
3. **.dockerignore 설정** — 불필요한 파일(소스, .git 등)이 이미지에 포함되지 않도록
4. **레이어 수 최소화** — RUN 명령어 여러 개를 `&&`로 하나로 합치기

```dockerfile
# 나쁜 예
RUN apt-get update
RUN apt-get install -y curl
RUN rm -rf /var/lib/apt/lists/*

# 좋은 예
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*
```

---

## Q12. Docker를 실무에서 어떻게 활용했나요? (예상 답변)

**답변:**
개인 프로젝트(olive-market)에서 Spring Boot + MySQL + Redis 환경을 Docker Compose로 구성했습니다.

멀티 스테이지 빌드로 Dockerfile을 작성하여 최종 이미지 크기를 줄였고, docker-compose.yml에서 depends_on과 healthcheck를 설정하여 MySQL이 완전히 기동된 후 애플리케이션이 시작되도록 의존성 순서를 제어했습니다.

또한 MySQL 데이터는 named volume으로 관리하여 컨테이너 재시작 시에도 데이터가 유지되도록 구성했으며, 라즈베리파이에 배포하여 실서버 환경에서 운영했습니다.

---

## 핵심 키워드 요약

| 키워드 | 한 줄 설명 |
|--------|-----------|
| Image | 실행 설계도, 읽기 전용 |
| Container | 이미지의 실행 인스턴스 |
| Dockerfile | 이미지 빌드 설정 파일 |
| Docker Compose | 다중 컨테이너 관리 도구 |
| Volume | 컨테이너 외부 데이터 저장소 |
| Layer | Dockerfile 명령어 단위 캐시 |
| 멀티 스테이지 빌드 | 이미지 크기 최적화 기법 |
| stateless | 컨테이너는 상태를 갖지 않음 |
| 서비스 이름 | Compose 내 컨테이너 간 호스트명 |
