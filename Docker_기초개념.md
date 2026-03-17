# Docker 기초 개념

---

## 1. Docker가 뭔가요?

한 줄 요약: **"내 컴퓨터에서 됐는데 서버에서 안 돼요" 문제를 없애주는 기술**

개발자 A 컴퓨터: Java 17, MySQL 8.0 → 잘 됨
서버: Java 11, MySQL 5.7 → 안 됨

Docker를 쓰면 **실행 환경 자체를 포장해서** 어디서든 똑같이 실행됩니다.

---

## 2. 가상머신(VM) vs Docker 컨테이너

### 가상머신
```
[내 컴퓨터 하드웨어]
└── [하이퍼바이저 (VMware 등)]
    ├── [Guest OS (Ubuntu)] ← OS 전체 포함 → 무거움 (수 GB)
    │   └── [앱]
    └── [Guest OS (CentOS)]
        └── [앱]
```

### Docker 컨테이너
```
[내 컴퓨터 하드웨어]
└── [Host OS (내 운영체제)]
    └── [Docker Engine]
        ├── [컨테이너 A] ← OS 없이 앱만 → 가벼움 (수 MB~수백 MB)
        └── [컨테이너 B]
```

| 항목 | VM | Docker |
|------|----|----|
| 크기 | 수 GB | 수 MB ~ 수백 MB |
| 시작 시간 | 수 분 | 수 초 |
| OS 포함 | O | X (Host OS 공유) |
| 격리 수준 | 강함 | 적당함 |

---

## 3. 핵심 개념 3가지

### Image (이미지)
- 앱 실행에 필요한 **설계도 / 틀**
- 읽기 전용, 변경 불가
- 예: `mysql:8.0` 이미지, `openjdk:17` 이미지

```
이미지 = 붕어빵 틀
```

### Container (컨테이너)
- 이미지를 실행한 **실제 인스턴스**
- 이미지 하나로 컨테이너 여러 개 생성 가능
- 실행/중지/삭제 가능

```
컨테이너 = 붕어빵 틀로 만든 붕어빵
```

### Dockerfile
- 이미지를 **어떻게 만들지** 정의한 설정 파일
- 내 Spring Boot 앱을 이미지로 만들 때 사용

```
Dockerfile = 붕어빵 틀을 만드는 설명서
```

---

## 4. Docker Hub

- 이미지를 저장하고 공유하는 **원격 저장소** (GitHub의 이미지 버전)
- https://hub.docker.com
- `mysql`, `redis`, `nginx` 같은 공식 이미지 무료로 받아서 사용 가능

```
docker pull mysql:8.0   ← Docker Hub에서 이미지 다운로드
```

---

## 5. Docker 핵심 흐름

```
Dockerfile
    ↓ docker build
Image (이미지)
    ↓ docker run
Container (컨테이너) ← 실제로 실행 중인 앱
```

---

## 6. 자주 쓰는 명령어 (기초)

```bash
# 이미지 관련
docker pull nginx            # 이미지 다운로드
docker images                # 이미지 목록 확인
docker rmi nginx             # 이미지 삭제

# 컨테이너 관련
docker run nginx             # 컨테이너 실행
docker run -d nginx          # 백그라운드 실행
docker run -p 8080:80 nginx  # 포트 연결 (내컴:컨테이너)
docker ps                    # 실행 중인 컨테이너 목록
docker ps -a                 # 전체 컨테이너 목록 (중지 포함)
docker stop [컨테이너ID]      # 컨테이너 중지
docker rm [컨테이너ID]        # 컨테이너 삭제
docker logs [컨테이너ID]      # 컨테이너 로그 확인
docker exec -it [ID] bash    # 컨테이너 내부 접속
```

---

## 7. 포트 포워딩 이해

```
내 컴퓨터 8080 포트 → 컨테이너 80 포트

docker run -p 8080:80 nginx
             ↑     ↑
          내컴퓨터  컨테이너
```

브라우저에서 `localhost:8080` 접속 → 컨테이너 안 nginx 80 포트로 연결됨

---

## 8. Volume (볼륨) — 데이터 보존

컨테이너를 삭제하면 내부 데이터도 사라집니다.
DB 데이터를 유지하려면 **볼륨**으로 외부에 저장해야 합니다.

```bash
docker run -v /내컴퓨터/경로:/컨테이너/경로 mysql:8.0
```

---

## 9. 환경변수 설정

```bash
docker run -e MYSQL_ROOT_PASSWORD=1234 mysql:8.0
```

---

## 10. Dockerfile 기본 문법

```dockerfile
FROM openjdk:17                          # 베이스 이미지
WORKDIR /app                             # 작업 디렉토리 설정
COPY build/libs/app.jar app.jar          # 파일 복사
EXPOSE 8080                              # 포트 노출 (문서용)
ENTRYPOINT ["java", "-jar", "app.jar"]  # 실행 명령
```

---

## 11. Docker Compose란?

여러 컨테이너를 **한 번에 관리**하는 도구

예: Spring Boot + MySQL + Redis 를 각각 실행하면 명령어가 3개 필요
→ Docker Compose 쓰면 `docker compose up` 하나로 끝

```yaml
# docker-compose.yml 예시
services:
  app:
    build: .
    ports:
      - "8080:8080"
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: 1234
  redis:
    image: redis:7
```

```bash
docker compose up -d    # 전체 서비스 백그라운드 실행
docker compose down     # 전체 서비스 중지 및 제거
docker compose logs     # 전체 로그 확인
```

---

## 정리

```
Image     → 실행 설계도 (읽기 전용)
Container → 실행 중인 인스턴스
Dockerfile → 이미지 만드는 설정 파일
Docker Hub → 이미지 저장소
Volume    → 데이터 영속성
Docker Compose → 다중 컨테이너 관리
```
