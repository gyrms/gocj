# Kafka 기초학습 - 기술면접 완벽 대비

> CJ올리브영 면접 기준 작성 (5순위 - 우대사항)
> 직접 경험 없으므로 개념 + 이커머스 활용 맥락 위주로 학습

---

## 목차
1. [Kafka란 무엇인가](#1-kafka란-무엇인가)
2. [핵심 구성요소](#2-핵심-구성요소)
3. [Offset과 메시지 보관](#3-offset과-메시지-보관)
4. [Consumer Group과 병렬 처리](#4-consumer-group과-병렬-처리)
5. [메시지 전달 보장](#5-메시지-전달-보장)
6. [Kafka 심화 개념](#6-kafka-심화-개념)
7. [이커머스 활용 사례](#7-이커머스-활용-사례)
8. [Spring Kafka 코드 예시](#8-spring-kafka-코드-예시)
9. [Kafka vs RabbitMQ vs REST](#9-kafka-vs-rabbitmq-vs-rest)
10. [전체 면접 빈출 질문 모음](#10-전체-면접-빈출-질문-모음)

---

## 1. Kafka란 무엇인가

### 한 줄 정의

**Kafka**는 대용량 실시간 데이터 스트림을 처리하는 **분산 메시지 스트리밍 플랫폼**이다.

```
[기존 직접 호출 방식]
주문 서비스 → (REST) → 재고 서비스
주문 서비스 → (REST) → 결제 서비스
주문 서비스 → (REST) → 알림 서비스
→ 주문 서비스가 모든 서비스를 직접 알고 있어야 함
→ 하나라도 장애 나면 주문 자체가 실패

[Kafka 방식]
주문 서비스 → Kafka (order-created 토픽)
                  ↓          ↓          ↓
              재고 서비스  결제 서비스  알림 서비스
→ 주문 서비스는 Kafka에 이벤트만 던지면 끝
→ 각 서비스가 알아서 구독해서 처리
```

### Kafka를 쓰는 핵심 이유 3가지

```
1. 비동기 처리 - 이벤트 발행 후 즉시 응답, 처리는 나중에
2. 서비스 간 결합도 제거 - Producer는 Consumer가 누군지 몰라도 됨
3. 대용량 처리 - 초당 수백만 건 메시지 처리 가능 (LinkedIn 개발, 하루 수조 건)
```

### 기존 MQ(RabbitMQ)와 다른 점

| 구분 | Kafka | RabbitMQ |
|------|-------|----------|
| 메시지 보관 | 소비 후에도 **디스크에 보관** (기본 7일) | 소비하면 즉시 삭제 |
| 처리 방식 | Pull 방식 (Consumer가 가져감) | Push 방식 (Broker가 밀어줌) |
| 속도 | 초당 수백만 건 (대용량) | 초당 수만 건 (중소규모) |
| 재처리 | Offset 조정으로 재처리 가능 | 재처리 어려움 |
| 용도 | 이벤트 스트리밍, 로그 수집 | 작업 큐, 알림 |

> 💡 **핵심 차이**: Kafka는 소비해도 메시지가 남는다. 이 덕분에 여러 Consumer Group이 같은 메시지를 각자 처리할 수 있고, 장애 시 재처리도 가능하다.

---

## 2. 핵심 구성요소

### 전체 구조 한눈에 보기

```
                    ┌─────────────────────────────┐
                    │         Kafka Cluster        │
                    │                             │
Producer ──────────▶│  Broker 1  Broker 2  Broker 3  │──────────▶ Consumer
(메시지 발행)        │                             │              (메시지 소비)
                    │    Topic: order-created      │
                    │    ├── Partition 0           │
                    │    ├── Partition 1           │
                    │    └── Partition 2           │
                    └─────────────────────────────┘
                                 ↑
                           ZooKeeper / KRaft
                         (클러스터 메타데이터 관리)
```

---

### Producer (프로듀서)

메시지를 **생성하고 토픽에 발행**하는 주체.

```
Producer의 역할:
- 어떤 Topic에 보낼지 결정
- 어떤 Partition에 넣을지 결정 (기본: 라운드 로빈 or 키 기반 해시)
- 메시지 직렬화 (JSON, Avro 등)
```

```java
// Spring Kafka Producer 예시
@Service
public class OrderEventProducer {

    private final KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;

    public void publishOrderCreated(Order order) {
        OrderCreatedEvent event = OrderCreatedEvent.builder()
            .orderId(order.getId())
            .userId(order.getUserId())
            .productId(order.getProductId())
            .quantity(order.getQuantity())
            .createdAt(LocalDateTime.now())
            .build();

        // Topic에 메시지 발행
        // key = orderId → 같은 주문은 같은 Partition으로 (순서 보장)
        kafkaTemplate.send("order-created", order.getId().toString(), event);
    }
}
```

---

### Consumer (컨슈머)

토픽에서 **메시지를 가져와 처리**하는 주체.

```
Consumer의 역할:
- 특정 Topic 구독
- Partition에서 메시지를 Pull 방식으로 가져옴
- Offset을 커밋해서 "여기까지 읽었다" 표시
```

```java
// Spring Kafka Consumer 예시
@Component
public class StockConsumer {

    @KafkaListener(
        topics = "order-created",
        groupId = "stock-service"
    )
    public void handleOrderCreated(OrderCreatedEvent event) {
        // 주문 생성 이벤트 수신 → 재고 차감 처리
        stockService.decreaseStock(event.getProductId(), event.getQuantity());
        log.info("재고 차감 완료. orderId: {}", event.getOrderId());
    }
}
```

---

### Broker (브로커)

메시지를 **저장하고 전달하는 Kafka 서버**.

```
Broker의 역할:
- 메시지 디스크에 저장 (순차 쓰기 → 매우 빠름)
- Producer/Consumer 연결 관리
- Partition의 Leader/Follower 관리

[클러스터 구성]
Broker 1 (Leader)   ← Producer가 여기에 씀
Broker 2 (Follower) ← Broker1 복제
Broker 3 (Follower) ← Broker1 복제
→ Broker1 장애 시 Broker2가 자동으로 Leader 승격
```

---

### Topic (토픽)

메시지를 **분류하는 논리적 채널**. 이름 붙인 우편함 같은 개념.

```
Topic 예시:
  order-created    → 주문 생성 이벤트
  order-cancelled  → 주문 취소 이벤트
  payment-done     → 결제 완료 이벤트
  stock-decreased  → 재고 차감 이벤트
  user-signup      → 회원 가입 이벤트

각 Topic은 여러 개의 Partition으로 나뉨
```

---

### Partition (파티션)

Topic을 **물리적으로 나눈 단위**. 병렬 처리의 핵심.

```
Topic: order-created
├── Partition 0: [msg1, msg4, msg7, ...]
├── Partition 1: [msg2, msg5, msg8, ...]
└── Partition 2: [msg3, msg6, msg9, ...]

핵심 특징:
- Partition 내부는 순서 보장 (append-only)
- Partition 간 순서는 보장 안 됨
- Partition 수 = 최대 병렬 처리 수
```

**메시지가 어떤 Partition으로 가는가?**
```
1. Key 없음  → 라운드 로빈으로 골고루 분배
2. Key 있음  → hash(key) % partition수 → 같은 key는 항상 같은 Partition
               (같은 주문 ID의 이벤트는 순서 보장)
```

```java
// Key를 설정해서 같은 userId의 이벤트를 같은 Partition으로
kafkaTemplate.send("order-created", userId.toString(), event);
//                  topic명          key(파티션 결정)  메시지
```

---

## 3. Offset과 메시지 보관

### Offset이란?

Partition 내에서 메시지의 **고유한 순서 번호**.

```
Partition 0:
┌────┬────┬────┬────┬────┐
│ 0  │ 1  │ 2  │ 3  │ 4  │  ← Offset
│msg1│msg2│msg3│msg4│msg5│
└────┴────┴────┴────┴────┘
              ↑
         Consumer가 여기(offset 2)까지 읽었음 → commit
         다음엔 offset 3부터 읽기 시작
```

### 왜 Offset이 중요한가?

```
[RabbitMQ - 소비하면 삭제]
Consumer: msg1 처리 완료 → Broker: 삭제
Consumer 장애 → 처리 중이던 메시지 날아감 (재처리 어려움)

[Kafka - Offset으로 재처리 가능]
Consumer: msg1, msg2 처리 → offset 2 commit
Consumer 장애 → 재시작 시 offset 3부터 다시 읽음
또는 offset을 0으로 되돌리면 처음부터 재처리 가능
```

### 메시지 보관 기간

```yaml
# server.properties
log.retention.hours=168    # 기본 7일 보관 (소비해도 삭제 안 됨)
log.retention.bytes=1073741824  # 또는 크기 기준 삭제 (1GB)
```

> 💡 Kafka는 소비해도 메시지가 사라지지 않는다.
> 덕분에 여러 Consumer Group이 같은 토픽을 독립적으로 소비할 수 있다.

---

## 4. Consumer Group과 병렬 처리

### Consumer Group이란?

같은 토픽을 처리하는 **Consumer들의 논리적 묶음**.
같은 Group 내의 Consumer들은 Partition을 나눠서 처리한다.

```
Topic: order-created (Partition 3개)

[stock-service Group]
  Partition 0 → Consumer A (재고 서비스 인스턴스 1)
  Partition 1 → Consumer B (재고 서비스 인스턴스 2)
  Partition 2 → Consumer C (재고 서비스 인스턴스 3)
  → 3개 인스턴스가 병렬로 처리

[notification-service Group]
  Partition 0,1,2 → Consumer D (알림 서비스)
  → 같은 메시지를 알림 서비스도 독립적으로 처리
```

**핵심 규칙:**
```
1. 하나의 Partition은 같은 Group 내에서 하나의 Consumer만 처리
   (중복 처리 방지)

2. Consumer 수 > Partition 수
   → 초과 Consumer는 놀게 됨 (의미 없음)
   → Partition 수가 병렬 처리의 상한선

3. Consumer 수 < Partition 수
   → 하나의 Consumer가 여러 Partition 처리
```

### Consumer Group 예시

```
Partition 3개, Consumer 3개 (최적)
  P0 → C1
  P1 → C2
  P2 → C3

Partition 3개, Consumer 2개
  P0, P1 → C1
  P2     → C2

Partition 3개, Consumer 4개 (비효율)
  P0 → C1
  P1 → C2
  P2 → C3
  C4 → 대기 (아무것도 안 함)
```

### 리밸런싱 (Rebalancing)

Consumer 수가 변경될 때 Partition 재분배가 일어남.

```
[트리거]
- Consumer 추가 (Scale Out)
- Consumer 장애/종료
- Topic Partition 추가

[리밸런싱 중 문제]
- 리밸런싱이 일어나는 동안 해당 Consumer Group 처리 중단
- 짧은 시간이지만 지연 발생

[해결 - Static Membership]
Consumer에 고정 ID를 부여하면 재시작 시 리밸런싱 최소화
```

---

## 5. 메시지 전달 보장

### 3가지 전달 보장 수준

```
At-most-once  (최대 한 번)  : 빠르지만 메시지 유실 가능
At-least-once (최소 한 번)  : 가장 일반적, 중복 가능 (중복 처리 로직 필요)
Exactly-once  (정확히 한 번) : 완벽하지만 성능 비용 큼
```

---

### At-most-once (최대 한 번)

```
메시지 수신 직후 offset commit → 처리
→ 처리 중 실패해도 offset이 이미 넘어가서 재처리 안 함
→ 메시지 유실 가능

사용: 로그 수집처럼 일부 유실이 허용되는 경우
```

---

### At-least-once (최소 한 번) - 가장 일반적

```
메시지 수신 → 처리 완료 → offset commit
→ 처리 성공 후에야 offset 넘김
→ 장애 시 재시작 후 같은 메시지 다시 처리 (중복 가능)

사용: 대부분의 경우 (중복 처리를 멱등성으로 해결)
```

```java
@KafkaListener(topics = "order-created", groupId = "stock-service")
public void handleOrderCreated(OrderCreatedEvent event) {
    // 처리 완료 후 자동으로 offset commit (at-least-once)

    // 멱등성 처리: 같은 orderId로 2번 처리되도 결과 동일하게
    if (stockService.isAlreadyProcessed(event.getOrderId())) {
        log.warn("이미 처리된 이벤트. orderId: {}", event.getOrderId());
        return;
    }

    stockService.decreaseStock(event.getProductId(), event.getQuantity());
    stockService.markProcessed(event.getOrderId()); // 처리 완료 표시
}
```

---

### Exactly-once (정확히 한 번)

```
Kafka Transaction + Idempotent Producer 조합
→ 메시지가 정확히 한 번만 처리됨을 보장
→ 성능 비용 큼 (트랜잭션 오버헤드)

사용: 결제, 이체처럼 중복이 절대 안 되는 경우
```

```java
// Producer - Idempotent (멱등적) 설정
@Bean
public ProducerFactory<String, Object> producerFactory() {
    Map<String, Object> config = new HashMap<>();
    config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true); // 멱등적 프로듀서
    config.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, "payment-producer-1");
    // acks=all, retries=MAX 자동 설정됨
    return new DefaultKafkaProducerFactory<>(config);
}

// Kafka Transaction 사용
@Transactional
public void processPayment(PaymentEvent event) {
    kafkaTemplate.executeInTransaction(ops -> {
        ops.send("payment-done", event);
        ops.send("stock-decrease", event);
        return true;
        // 두 메시지가 모두 성공하거나 모두 실패 (원자성)
    });
}
```

### 실무에서의 선택

```
대부분의 케이스 → At-least-once + 멱등성 처리
이유:
  - Exactly-once는 성능 비용이 큼
  - 멱등성 처리(중복 체크)로 충분히 안전하게 구현 가능

멱등성이란?
  같은 연산을 여러 번 실행해도 결과가 같은 것
  예) 재고 차감 이벤트 중복 수신 시 orderId로 처리 여부 확인
      → 이미 처리됐으면 skip → 중복 차감 방지
```

---

## 6. Kafka 심화 개념

### Replication (복제)

```
Partition의 데이터를 여러 Broker에 복제해 장애 대응.

replication factor = 3 이면:
  Partition 0의 Leader → Broker 1
  Partition 0의 Follower → Broker 2, 3 (복제본)

Broker 1 장애 → Broker 2가 Leader 자동 승격 → 서비스 중단 없음
```

```yaml
# Topic 생성 시 설정
replication-factor: 3    # 복제본 3개 (Broker가 3대 이상 필요)
min.insync.replicas: 2   # 최소 2개 복제본에 쓰여야 성공으로 인정
```

### ISR (In-Sync Replicas)

```
Leader와 동기화가 완료된 Follower 목록.

[정상 상태]
ISR = [Broker1(Leader), Broker2, Broker3]
→ Producer 메시지 → Broker1 + Broker2 + Broker3 모두 복제 완료 → ack

[Broker3 느려지면]
ISR = [Broker1, Broker2]  ← Broker3는 ISR에서 제외
→ Broker3는 복제 중이지만 성공 판단에서 제외됨
```

### acks 설정 (Producer 신뢰도)

```
acks=0   : Broker 응답 기다리지 않음 (가장 빠름, 유실 가능)
acks=1   : Leader에만 저장되면 성공 (기본값)
acks=all : ISR 전체에 복제되면 성공 (가장 안전, 느림)
```

```java
// 중요한 이벤트 (결제, 주문)
config.put(ProducerConfig.ACKS_CONFIG, "all");
config.put(ProducerConfig.RETRIES_CONFIG, 3);

// 덜 중요한 이벤트 (로그, 통계)
config.put(ProducerConfig.ACKS_CONFIG, "1");
```

### Dead Letter Topic (DLT)

처리 실패한 메시지를 별도 Topic에 보내는 패턴.

```
[정상 흐름]
order-created → Consumer → 처리 성공

[실패 흐름]
order-created → Consumer → 처리 실패 (3회 재시도)
                                    ↓
                          order-created.DLT (Dead Letter Topic)
                                    ↓
                          개발자 알림 → 수동 재처리 or 원인 분석
```

```java
@Component
public class StockConsumer {

    @KafkaListener(topics = "order-created", groupId = "stock-service")
    public void handle(OrderCreatedEvent event) {
        stockService.decreaseStock(event.getProductId(), event.getQuantity());
    }

    // 처리 실패 시 Dead Letter Topic으로
    @KafkaListener(topics = "order-created.DLT", groupId = "stock-service-dlt")
    public void handleDlt(OrderCreatedEvent event) {
        log.error("DLT 메시지 수신. 수동 처리 필요. orderId: {}", event.getOrderId());
        alertService.notifyDeveloper(event);
    }
}
```

### Consumer Lag (컨슈머 렉)

```
Producer가 메시지를 쌓는 속도 > Consumer가 처리하는 속도
→ 처리 못 한 메시지가 쌓임 = Consumer Lag 증가

[모니터링]
kafka-consumer-groups.sh --describe --group stock-service
→ LAG 컬럼 확인

[해결]
1. Consumer 인스턴스 수 늘리기 (Scale Out)
2. Partition 수 늘리기
3. Consumer 처리 로직 최적화
```

---

## 7. 이커머스 활용 사례

### 주문 처리 흐름 (올리브영 스타일)

```
[주문 생성]
사용자 → 주문 API → order-created 토픽 발행

[각 서비스가 독립적으로 구독]
order-created 토픽
    ├── stock-service    → 재고 차감
    ├── payment-service  → 결제 처리
    ├── notification-service → 주문 완료 알림 (SMS/앱 푸시)
    └── point-service    → 포인트 적립

[각 처리 완료 후 다시 이벤트 발행]
payment-done 토픽
    └── delivery-service → 배송 시작 처리
```

### REST 직접 호출 vs Kafka 선택 기준

```
[REST 직접 호출이 맞는 경우]
- 즉시 결과가 필요할 때
  예) 로그인, 상품 조회, 재고 확인
- 트랜잭션 결과를 바로 응답해야 할 때
  예) 결제 성공/실패 여부

[Kafka가 맞는 경우]
- 처리 결과를 즉시 알 필요 없을 때
  예) 주문 후 알림 발송, 포인트 적립, 통계 처리
- 여러 서비스에 같은 이벤트를 동시에 전달할 때
  예) 주문 이벤트를 재고/알림/통계 서비스 모두 받아야 할 때
- 트래픽 급증 시 버퍼 역할
  예) 블랙프라이데이 → 주문은 빠르게 받고, 처리는 순서대로
- 서비스 장애 격리
  예) 알림 서비스 죽어도 주문은 성공 (나중에 Kafka에서 재처리)
```

### 블랙프라이데이 대응 (트래픽 버퍼)

```
[Kafka 없이]
갑자기 트래픽 10배 → 재고 서비스 과부하 → 장애

[Kafka 있으면]
갑자기 트래픽 10배 → order-created 토픽에 쌓임 (버퍼)
                    → 재고 서비스는 자기 속도에 맞춰 처리
                    → 장애 없음 (대신 처리 지연)
```

### Saga 패턴 (분산 트랜잭션)

MSA에서 여러 서비스에 걸친 트랜잭션 처리.

```
[주문 → 결제 → 재고 Saga]

1. 주문 서비스: 주문 생성 → payment-requested 이벤트 발행
2. 결제 서비스: 결제 처리 → payment-done 이벤트 발행
3. 재고 서비스: 재고 차감 → stock-decreased 이벤트 발행

[결제 실패 시 보상 트랜잭션]
결제 서비스: payment-failed 이벤트 발행
주문 서비스: 주문 취소 처리 (보상)
→ 각 서비스가 자신의 작업을 롤백
```

---

## 8. Spring Kafka 코드 예시

### 의존성 및 설정

```gradle
// build.gradle
implementation 'org.springframework.kafka:spring-kafka'
```

```yaml
# application.yml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      acks: all
      retries: 3
    consumer:
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      auto-offset-reset: earliest   # 처음부터 읽기 (latest: 최신부터)
      enable-auto-commit: false     # 수동 commit (at-least-once)
      properties:
        spring.json.trusted.packages: "*"
```

### Producer

```java
@Service
@RequiredArgsConstructor
public class OrderEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendOrderCreated(Order order) {
        OrderCreatedEvent event = OrderCreatedEvent.builder()
            .orderId(order.getId())
            .userId(order.getUserId())
            .productId(order.getProductId())
            .quantity(order.getQuantity())
            .totalPrice(order.getTotalPrice())
            .createdAt(LocalDateTime.now())
            .build();

        // 성공/실패 콜백
        kafkaTemplate.send("order-created", order.getUserId().toString(), event)
            .addCallback(
                result -> log.info("이벤트 발행 성공. orderId: {}", order.getId()),
                ex -> log.error("이벤트 발행 실패. orderId: {}", order.getId(), ex)
            );
    }
}
```

### Consumer - 수동 Ack (At-least-once)

```java
@Component
@RequiredArgsConstructor
public class StockEventConsumer {

    private final StockService stockService;

    @KafkaListener(
        topics = "order-created",
        groupId = "stock-service",
        concurrency = "3"          // Consumer 스레드 3개 = Partition 3개에 병렬 처리
    )
    public void handleOrderCreated(
            OrderCreatedEvent event,
            Acknowledgment ack) {  // 수동 커밋을 위해 Acknowledgment 주입

        try {
            // 멱등성 체크 (중복 처리 방지)
            if (stockService.isProcessed(event.getOrderId())) {
                log.warn("중복 이벤트 skip. orderId: {}", event.getOrderId());
                ack.acknowledge(); // 중복이어도 offset은 넘김
                return;
            }

            stockService.decreaseStock(event.getProductId(), event.getQuantity());
            stockService.markProcessed(event.getOrderId());

            ack.acknowledge(); // 처리 완료 후 offset commit

        } catch (OutOfStockException e) {
            log.error("재고 부족. productId: {}", event.getProductId(), e);
            // 재고 부족은 재처리해도 소용없음 → ack 후 보상 이벤트 발행
            ack.acknowledge();
            // TODO: order-cancelled 이벤트 발행 (Saga 보상)
        }
        // RuntimeException 발생 시 ack 안 함 → 재시도 → DLT로
    }
}
```

### Consumer - 재시도 및 DLT 설정

```java
@Configuration
public class KafkaConsumerConfig {

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object>
            kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory) {

        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);

        // 수동 Ack 설정
        factory.getContainerProperties()
            .setAckMode(ContainerProperties.AckMode.MANUAL);

        // 재시도 설정 (3회, 1초 간격)
        factory.setCommonErrorHandler(
            new DefaultErrorHandler(
                new DeadLetterPublishingRecoverer(kafkaTemplate), // DLT로 전송
                new FixedBackOff(1000L, 3L) // 1초 간격, 3회 재시도
            )
        );

        return factory;
    }
}
```

---

## 9. Kafka vs RabbitMQ vs REST

| 구분 | Kafka | RabbitMQ | REST |
|------|-------|----------|------|
| 통신 방식 | 비동기 | 비동기 | 동기 |
| 메시지 보관 | 디스크 보관 (7일) | 소비 후 삭제 | 없음 |
| 처리량 | 매우 높음 (수백만/초) | 높음 (수만/초) | 중간 |
| 순서 보장 | Partition 내 보장 | Queue 내 보장 | 보장 |
| 재처리 | Offset으로 쉬움 | 어려움 | 불가 |
| 결합도 | 낮음 | 낮음 | 높음 |
| 복잡도 | 높음 (운영 복잡) | 중간 | 낮음 |
| 적합한 상황 | 대용량 이벤트 스트리밍 | 작업 큐, 알림 | 즉시 응답 필요 |

### 언제 어떤 것을 쓰나?

```
REST 선택:
  - 로그인, 결제 성공 여부처럼 즉각적인 응답이 필요할 때
  - 간단한 CRUD

RabbitMQ 선택:
  - 작업 큐 (이메일 발송, 이미지 리사이징)
  - 간단한 서비스 간 통신
  - Kafka의 운영 복잡도를 피하고 싶을 때

Kafka 선택:
  - 이벤트 기반 MSA (주문 → 재고/결제/알림 동시 처리)
  - 대용량 로그 수집
  - 트래픽 급증 버퍼가 필요할 때
  - 여러 서비스가 같은 이벤트를 독립적으로 소비해야 할 때
```

---

## 10. 전체 면접 빈출 질문 모음

### 기본 개념

**Q1. Kafka를 쓰는 이유는 무엇인가요?**
> A. 크게 세 가지 이유가 있습니다.
> 첫째, 비동기 처리입니다. 주문 서비스가 재고·결제·알림 서비스를 직접 동기 호출하면 하나가 장애 나면 주문 전체가 실패합니다. Kafka를 쓰면 주문 서비스는 이벤트만 발행하고 즉시 응답하며, 각 서비스가 독립적으로 처리합니다.
> 둘째, 서비스 간 결합도 제거입니다. Producer는 Consumer가 누구인지, 몇 개인지 몰라도 됩니다.
> 셋째, 대용량 처리와 트래픽 버퍼입니다. 블랙프라이데이처럼 트래픽이 급증해도 Kafka가 버퍼 역할을 해서 각 서비스는 자기 속도에 맞춰 처리할 수 있습니다.

**Q2. Kafka의 구성요소(Producer, Consumer, Broker, Topic, Partition)를 설명해주세요.**
> A. Producer는 메시지를 생성해 Topic에 발행하는 주체입니다.
> Consumer는 Topic을 구독해 메시지를 가져와 처리하는 주체입니다.
> Broker는 메시지를 디스크에 저장하고 전달하는 Kafka 서버입니다. 여러 대를 클러스터로 운영합니다.
> Topic은 메시지를 분류하는 논리적 채널로 이름 붙인 우편함 같은 개념입니다.
> Partition은 Topic을 물리적으로 나눈 단위입니다. 병렬 처리의 핵심으로 Partition 수가 최대 병렬 처리 수를 결정합니다.

**Q3. Offset이 무엇인가요?**
> A. Partition 내 메시지의 고유한 순서 번호입니다.
> Consumer는 어디까지 읽었는지 Offset을 커밋해서 기록합니다.
> Kafka는 메시지를 소비해도 삭제하지 않기 때문에 Consumer가 장애로 재시작되면 커밋된 Offset 이후부터 다시 읽을 수 있습니다. 또한 Offset을 되돌리면 과거 메시지를 재처리할 수도 있습니다.

---

### 핵심 개념

**Q4. Consumer Group 동작 원리를 설명해주세요.**
> A. Consumer Group은 같은 Topic을 처리하는 Consumer들의 논리적 묶음입니다.
> 같은 Group 내의 Consumer들은 Partition을 나눠서 처리합니다. 하나의 Partition은 같은 Group 내에서 하나의 Consumer만 담당하기 때문에 중복 처리가 방지됩니다.
> 다른 Group들은 같은 Topic을 독립적으로 소비할 수 있습니다. 예를 들어 order-created 토픽을 재고 서비스 Group과 알림 서비스 Group이 각자 처음부터 끝까지 독립적으로 처리합니다.
> Consumer가 추가되거나 장애가 나면 리밸런싱이 일어나 Partition이 재분배됩니다.

**Q5. Partition과 병렬 처리의 관계를 설명해주세요.**
> A. Partition 수가 최대 병렬 처리 수를 결정합니다.
> Partition이 3개면 같은 Consumer Group에서 최대 3개의 Consumer가 동시에 처리할 수 있습니다.
> Consumer가 Partition보다 많으면 초과 Consumer는 대기 상태가 됩니다.
> 따라서 처리량을 늘리려면 Partition 수를 먼저 늘리고, Consumer 인스턴스(서버)도 함께 늘려야 합니다.
> 단, Partition 내부에서만 순서가 보장되므로 같은 사용자의 이벤트를 순서대로 처리해야 한다면 userId를 Key로 설정해 같은 Partition으로 가도록 해야 합니다.

**Q6. At-least-once와 Exactly-once의 차이와 실무 선택 기준은?**
> A. At-least-once는 처리 완료 후 Offset을 커밋하는 방식으로, 장애 시 같은 메시지를 중복 처리할 수 있습니다. Exactly-once는 Kafka 트랜잭션으로 정확히 한 번만 처리를 보장하지만 성능 비용이 큽니다.
> 실무에서는 대부분 At-least-once + 멱등성 처리를 선택합니다. 처리 여부를 orderId 등으로 확인해 중복이면 skip하는 방식으로 Exactly-once와 동일한 결과를 더 효율적으로 얻을 수 있기 때문입니다.
> 결제나 이체처럼 중복이 절대 안 되는 경우에만 Exactly-once를 사용합니다.

---

### 실무 패턴

**Q7. 이커머스에서 Kafka를 어떻게 활용하나요?**
> A. 주문 처리 흐름에서 가장 많이 활용합니다.
> 주문이 생성되면 order-created 이벤트를 Kafka에 발행하고, 재고 서비스·결제 서비스·알림 서비스·포인트 서비스가 각자 구독해서 독립적으로 처리합니다.
> 이 방식의 장점은 각 서비스가 느슨하게 결합되어 알림 서비스가 장애 나도 주문은 성공하고, 나중에 Kafka에서 재처리할 수 있다는 점입니다.
> 또한 블랙프라이데이 같은 트래픽 급증 시 Kafka가 버퍼 역할을 해서 주문을 빠르게 받고 각 서비스는 자기 속도에 맞춰 순차 처리할 수 있습니다.

**Q8. 동기 REST 호출 대신 Kafka를 쓰는 기준은 무엇인가요?**
> A. 즉각적인 응답이 필요한 경우엔 REST, 아닌 경우엔 Kafka를 선택합니다.
> 로그인, 결제 성공 여부, 재고 확인처럼 사용자가 결과를 바로 봐야 하는 경우엔 REST를 사용합니다.
> 반면 주문 후 알림 발송, 포인트 적립, 통계 집계처럼 사용자가 결과를 즉시 볼 필요가 없거나, 여러 서비스에 같은 이벤트를 전달해야 하거나, 특정 서비스의 장애가 핵심 흐름에 영향을 주면 안 되는 경우엔 Kafka를 사용합니다.

**Q9. Consumer Lag이 무엇이고 어떻게 대응하나요?**
> A. Producer가 메시지를 쌓는 속도보다 Consumer가 처리하는 속도가 느릴 때 발생하는 처리 지연 현상입니다.
> 모니터링 도구로 LAG 수치를 확인하고, 증가 추세면 Consumer 인스턴스를 늘려 병렬 처리를 강화합니다.
> 단, Consumer를 늘리려면 Partition 수도 함께 늘려야 실제 병렬 처리가 가능합니다.
> 처리 로직 자체가 느린 경우엔 DB 쿼리 최적화나 배치 처리로 Consumer 처리 속도를 높입니다.

**Q10. Kafka에서 메시지 처리 실패 시 어떻게 처리하나요?**
> A. 재시도와 Dead Letter Topic(DLT) 패턴을 사용합니다.
> 처리 실패 시 설정한 횟수만큼 재시도하고, 그래도 실패하면 해당 메시지를 별도의 DLT 토픽으로 보냅니다.
> DLT에 쌓인 메시지는 개발자에게 알림을 보내고, 원인을 분석한 후 수동으로 재처리합니다.
> Spring Kafka에서는 DefaultErrorHandler와 DeadLetterPublishingRecoverer를 조합하면 자동으로 구성할 수 있습니다.

---

> 각 섹션 학습 완료 시 기술면접_공부플랜.md 체크리스트에 표시할 것
