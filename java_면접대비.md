# Java 면접 대비 (시니어 백엔드)

> 야놀자 Senior Backend Engineer 포지션 기준 정리

---

## 1. JVM 구조

### JVM 메모리 구조
- **Method Area**: 클래스 메타데이터, static 변수, 상수 풀 저장
- **Heap**: 객체 인스턴스 저장. GC 대상. Young(Eden, Survivor) / Old 영역으로 구분
- **Stack**: 스레드마다 독립적. 메서드 호출 시 스택 프레임 생성 (지역변수, 리턴값)
- **PC Register**: 현재 실행 중인 JVM 명령어 주소
- **Native Method Stack**: JNI를 통한 네이티브 메서드 실행

### 면접 예상 질문
- Q: Heap과 Stack의 차이는?
  - Heap은 동적 할당 객체 저장, GC 관리 대상. Stack은 스레드별 독립적, 메서드 실행 컨텍스트
- Q: static 변수는 어디에 저장되나요?
  - Method Area (Java 8부터는 Metaspace)

---

## 2. GC (Garbage Collection)

### GC 동작 원리
- **Minor GC**: Young 영역(Eden → Survivor) 정리. Stop-the-world 짧음
- **Major GC**: Old 영역 정리. Stop-the-world 길어짐
- **Full GC**: Heap 전체 정리. 성능에 큰 영향

### GC 알고리즘
| 알고리즘 | 특징 | 사용 시점 |
|---------|------|---------|
| Serial GC | 단일 스레드, 소규모 앱 | 개발/테스트 |
| Parallel GC | 멀티 스레드, throughput 중심 | Java 8 기본 |
| G1GC | Region 기반, 예측 가능한 pause time | Java 9+ 기본 |
| ZGC | 대용량 Heap, pause 1ms 이하 | Java 15+ |

### 면접 예상 질문
- Q: GC가 발생하는 이유와 STW(Stop-the-world)란?
  - 불필요한 객체 메모리 회수를 위해. STW는 GC 실행 중 모든 애플리케이션 스레드가 멈추는 현상
- Q: Memory Leak이 발생하는 경우는?
  - static 컬렉션에 계속 추가만 하는 경우, 이벤트 리스너 미제거, ThreadLocal 미제거 등

---

## 3. 멀티스레딩 & 동시성

### synchronized vs volatile vs Atomic

| 키워드 | 용도 | 특징 |
|--------|------|------|
| synchronized | 임계구역 설정 | 상호배제 + 가시성 보장, 성능 오버헤드 |
| volatile | 변수 가시성 보장 | 캐시 무효화, 원자성 보장 안 됨 |
| AtomicInteger 등 | 원자적 연산 | CAS(Compare-And-Swap) 기반, lock-free |

### 주요 개념
- **Race Condition**: 여러 스레드가 공유 자원에 동시 접근 시 발생
- **Deadlock**: 두 스레드가 서로 상대방 락을 기다리는 상황
- **ThreadLocal**: 스레드별 독립적인 변수 저장. 사용 후 반드시 `remove()` 호출
- **ReentrantLock**: synchronized보다 유연한 락. tryLock(), lockInterruptibly() 지원

### 면접 예상 질문
- Q: synchronized와 ReentrantLock 차이는?
  - synchronized는 블록 기반, 자동 해제. ReentrantLock은 명시적 lock/unlock, 타임아웃/인터럽트 지원
- Q: ThreadLocal 사용 시 주의사항은?
  - 스레드 풀 환경에서 스레드 재사용 시 이전 값이 남아있을 수 있어 반드시 remove() 필요
- Q: Deadlock 해결 방법은?
  - 락 획득 순서 일관성 유지, tryLock 타임아웃 사용, 락 최소화

---

## 4. 컬렉션 프레임워크

### HashMap 내부 동작
- 기본 배열(버킷) + LinkedList 구조
- `hashCode()` → 버킷 인덱스 결정 → 충돌 시 LinkedList(Java 8부터 8개 이상이면 TreeNode)
- **Load Factor**: 기본 0.75. 초과 시 rehashing (용량 2배 확장)

### 주요 비교
| 클래스 | 특징 | 사용 시점 |
|--------|------|---------|
| ArrayList | 인덱스 접근 O(1), 삽입/삭제 O(n) | 조회 많을 때 |
| LinkedList | 삽입/삭제 O(1), 인덱스 접근 O(n) | 삽입/삭제 많을 때 |
| HashMap | 비동기, null 허용 | 일반적인 Map |
| ConcurrentHashMap | 동기화, 세그먼트 락 | 멀티스레드 환경 |
| LinkedHashMap | 삽입 순서 유지 | 순서가 필요한 Map |

### 면접 예상 질문
- Q: HashMap과 HashTable 차이는?
  - HashTable은 synchronized로 스레드 안전하지만 성능 낮음. 멀티스레드 환경엔 ConcurrentHashMap 권장
- Q: HashMap의 충돌 해결 방법은?
  - Separate Chaining(LinkedList/TreeNode). Java 8에서 버킷 8개 이상 시 TreeNode로 변환 O(n) → O(log n)

---

## 5. Java 8+ 핵심 기능

### Stream API
```java
// 자주 쓰는 패턴
List<String> result = list.stream()
    .filter(s -> s.startsWith("A"))
    .map(String::toUpperCase)
    .sorted()
    .collect(Collectors.toList());

// 그룹핑
Map<String, List<User>> groupBy = users.stream()
    .collect(Collectors.groupingBy(User::getDepartment));
```

### Optional
```java
// 올바른 사용
Optional<String> name = Optional.ofNullable(getName());
String result = name.orElse("default");
String result2 = name.orElseGet(() -> computeDefault());
name.ifPresent(n -> System.out.println(n));

// 잘못된 사용 - get() 직접 호출 지양
```

### CompletableFuture
```java
// 비동기 실행
CompletableFuture<String> future = CompletableFuture
    .supplyAsync(() -> fetchData())
    .thenApply(data -> process(data))
    .exceptionally(ex -> "error");
```

### 면접 예상 질문
- Q: Stream과 for-loop 성능 차이는?
  - 소량 데이터는 for-loop가 빠름. 병렬 스트림은 대용량에서 유리하나 오버헤드 고려 필요
- Q: Optional을 필드 타입으로 쓰면 안 되는 이유는?
  - Serializable 미구현, null 체크용 반환값으로만 설계됨

---

## 6. 객체지향 & SOLID

### SOLID 원칙
| 원칙 | 설명 | 예시 |
|------|------|------|
| SRP (단일책임) | 클래스는 하나의 책임만 | UserService는 인증만, UserRepository는 DB만 |
| OCP (개방폐쇄) | 확장에 열려있고 수정에 닫혀있음 | 인터페이스로 구현 |
| LSP (리스코프치환) | 자식 클래스는 부모를 대체 가능 | 오버라이드 시 행동 규약 유지 |
| ISP (인터페이스분리) | 클라이언트별 인터페이스 분리 | 불필요한 메서드 구현 강제 X |
| DIP (의존역전) | 고수준이 저수준에 의존 X, 추상화에 의존 | Spring DI |

### 면접 예상 질문
- Q: 상속 vs 컴포지션, 언제 사용?
  - IS-A 관계면 상속, HAS-A 관계면 컴포지션. 일반적으로 컴포지션이 더 유연함
- Q: 인터페이스 vs 추상클래스 차이는?
  - 인터페이스: 다중 구현 가능, 행동 계약 정의. 추상클래스: 단일 상속, 공통 구현 코드 포함 가능

---

## 7. 예외 처리

### Checked vs Unchecked
- **Checked Exception**: 컴파일 시점 강제 처리 (IOException, SQLException)
- **Unchecked Exception**: RuntimeException 하위, 명시적 처리 불필요

### 면접 예상 질문
- Q: `finally` vs `try-with-resources` 차이는?
  - try-with-resources는 AutoCloseable 구현체를 자동으로 close(). 더 안전하고 간결
- Q: Exception을 잡아서 무시하면 안 되는 이유는?
  - 장애 추적 불가, 근본 원인 숨김. 최소 로깅 필요

---

## 8. 제네릭

```java
// 공변 (? extends T): 읽기만 가능
List<? extends Number> list = new ArrayList<Integer>();

// 반공변 (? super T): 쓰기만 가능
List<? super Integer> list2 = new ArrayList<Number>();
```

### 면접 예상 질문
- Q: 제네릭 타입 소거(Type Erasure)란?
  - 컴파일 시 제네릭 타입 정보가 지워지고 Object 또는 바운드 타입으로 대체됨. 런타임에 타입 정보 없음

---

## 9. 자주 나오는 추가 질문

- Q: `equals()`와 `hashCode()` 재정의 시 왜 같이 해야 하나요?
  - HashMap/HashSet은 hashCode로 버킷 찾고 equals로 동등성 비교. 둘 중 하나만 재정의하면 컬렉션이 의도대로 동작 안 함

- Q: String이 불변(immutable)인 이유는?
  - 보안(비밀번호 등), String Pool을 통한 메모리 절약, 스레드 안전성, HashCode 캐싱 가능

- Q: `==` vs `equals()` 차이는?
  - `==`: 참조 주소 비교. `equals()`: 논리적 동등성 비교 (재정의 필요)

- Q: Java는 Call by Value인가 Call by Reference인가?
  - Call by Value. 기본 타입은 값 복사, 참조 타입은 참조값(주소)을 복사해서 전달
