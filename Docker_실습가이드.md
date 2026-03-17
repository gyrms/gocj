# Docker 실습 가이드 — olive-market 프로젝트 기준

---

## 1. 설치 확인

```bash
docker --version
docker compose version
```

---

## 2. Spring Boot 앱 Dockerfile 작성

프로젝트 루트에 `Dockerfile` 생성

```dockerfile
# 1단계: 빌드
FROM gradle:8.5-jdk17 AS builder
WORKDIR /app
COPY . .
RUN gradle bootJar --no-daemon

# 2단계: 실행
FROM openjdk:17-slim
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 왜 2단계로 나누나?
- 1단계(builder): Gradle로 빌드 → jar 파일 생성
- 2단계: jar 파일만 복사해서 실행 → 이미지 크기 대폭 감소
- 빌드 도구(Gradle)가 최종 이미지에 포함되지 않음

---

## 3. docker-compose.yml 작성

프로젝트 루트에 `docker-compose.yml` 생성

```yaml
services:

  # Spring Boot 앱
  app:
    build: .
    container_name: olive-market-app
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/olivemarket?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8
      SPRING_DATASOURCE_USERNAME: root
      SPRING_DATASOURCE_PASSWORD: olive1234
      SPRING_REDIS_HOST: redis
      SPRING_REDIS_PORT: 6379
    depends_on:
      mysql:
        condition: service_healthy
      redis:
        condition: service_started
    restart: always

  # MySQL
  mysql:
    image: mysql:8.0
    container_name: olive-market-mysql
    ports:
      - "3306:3306"
    environment:
      MYSQL_ROOT_PASSWORD: olive1234
      MYSQL_DATABASE: olivemarket
      MYSQL_CHARSET: utf8mb4
    volumes:
      - mysql-data:/var/lib/mysql
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5
    restart: always

  # Redis
  redis:
    image: redis:7-alpine
    container_name: olive-market-redis
    ports:
      - "6379:6379"
    volumes:
      - redis-data:/data
    restart: always

volumes:
  mysql-data:
  redis-data:
```

---

## 4. .dockerignore 작성

불필요한 파일이 이미지에 포함되지 않도록 설정

```
.git
.gradle
build
*.md
*.log
```

---

## 5. application.yml 환경변수 연동

`src/main/resources/application.yml`

```yaml
spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL:jdbc:mysql://localhost:3306/olivemarket}
    username: ${SPRING_DATASOURCE_USERNAME:root}
    password: ${SPRING_DATASOURCE_PASSWORD:olive1234}
    driver-class-name: com.mysql.cj.jdbc.Driver

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true

  data:
    redis:
      host: ${SPRING_REDIS_HOST:localhost}
      port: ${SPRING_REDIS_PORT:6379}

jwt:
  secret: ${JWT_SECRET:olive-market-secret-key-must-be-long-enough}
  expiration: 86400000  # 24시간
```

> `${변수:기본값}` — Docker 환경변수가 있으면 사용, 없으면 기본값(로컬 개발용)

---

## 6. 실행 명령어

### 최초 실행
```bash
docker compose up --build -d
```

### 로그 확인
```bash
docker compose logs -f app      # 앱 로그만
docker compose logs -f          # 전체 로그
```

### 재시작 (코드 변경 후)
```bash
docker compose down
docker compose up --build -d
```

### 컨테이너 상태 확인
```bash
docker compose ps
```

### MySQL 직접 접속
```bash
docker exec -it olive-market-mysql mysql -u root -p
```

### Redis 직접 접속
```bash
docker exec -it olive-market-redis redis-cli
```

---

## 7. 라즈베리파이 배포 흐름

```
[개발 PC]                          [라즈베리파이]
코드 작성
    ↓
git push origin main
                                       ↓
                               git pull origin main
                                       ↓
                            docker compose up --build -d
                                       ↓
                               localhost:8080 실행 중
```

### 라즈베리파이에서 실행 순서

```bash
# 1. 코드 받기
git pull origin main

# 2. 빌드 및 실행
docker compose up --build -d

# 3. 확인
docker compose ps
docker compose logs -f app
```

---

## 8. 자주 발생하는 문제

### MySQL 연결 안 될 때
```bash
# MySQL 컨테이너 헬스체크 확인
docker compose ps
# healthy 상태가 될 때까지 app이 기다림 (depends_on healthcheck 설정 덕분)
```

### 포트 충돌 날 때
```bash
# 이미 사용 중인 포트 확인
sudo lsof -i :8080
sudo lsof -i :3306
```

### 이미지 캐시 무시하고 새로 빌드
```bash
docker compose build --no-cache
docker compose up -d
```

### 전체 초기화 (데이터 포함)
```bash
docker compose down -v   # 볼륨까지 삭제
docker compose up --build -d
```

---

## 9. 프로젝트 디렉토리 구조

```
olive-market/
├── src/
├── build.gradle
├── Dockerfile              ← 추가
├── docker-compose.yml      ← 추가
├── .dockerignore           ← 추가
└── README.md
```
