# JPA 심화학습 1 - 디테일 설명

> JPA_심화학습.md에서 나온 개념들을 더 자세히 설명
> "왜 이게 나왔는지"부터 설명

---

## 목차
1. [em이 뭔가요? (EntityManager)](#1-em이-뭔가요-entitymanager)
2. [1L이 뭔가요? (Java 타입 시스템)](#2-1l이-뭔가요-java-타입-시스템)
3. [어노테이션 완전 정복](#3-어노테이션-완전-정복)
4. [영속성 컨텍스트 재정리 (더 쉽게)](#4-영속성-컨텍스트-재정리-더-쉽게)

---

## 1. em이 뭔가요? (EntityManager)

### em = EntityManager

JPA에서 DB와 실제로 대화하는 객체다.
`em`은 `EntityManager`의 변수명을 줄여 쓴 것이다.

```java
// EntityManager를 em이라는 변수에 담은 것
EntityManager em = ...; // em은 그냥 변수명

// 이렇게 써도 똑같음
EntityManager entityManager = ...;
entityManager.find(Member.class, 1L);
```

---

### EntityManager의 역할

| 메서드 | 하는 일 | SQL |
|--------|---------|-----|
| `em.persist(entity)` | 엔티티 저장 | INSERT |
| `em.find(Class, id)` | 엔티티 조회 | SELECT |
| `em.remove(entity)` | 엔티티 삭제 | DELETE |
| `em.merge(entity)` | 엔티티 수정 | UPDATE |
| `em.flush()` | 변경사항 DB에 반영 | - |

---

### 실무에서는 em을 직접 쓰나요?

**아니요.** 실무 스프링 프로젝트에서는 `em`을 직접 거의 안 쓴다.

```
JPA 원래 방식: EntityManager → em.find(), em.persist() 직접 호출
스프링 방식:   Spring Data JPA → JpaRepository 인터페이스만 상속하면 자동 제공
```

```java
// JPA 원래 방식 (em 직접 사용)
@Repository
public class MemberRepository {
    @PersistenceContext
    private EntityManager em; // 스프링이 주입해줌

    public Member findById(Long id) {
        return em.find(Member.class, id); // 직접 호출
    }

    public void save(Member member) {
        em.persist(member); // 직접 호출
    }
}

// 스프링 실무 방식 (Spring Data JPA)
public interface MemberRepository extends JpaRepository<Member, Long> {
    // 아무것도 안 써도 findById(), save(), delete() 자동 제공!
}
```

> 💡 면접 설명에서 em이 나오는 이유:
> JPA의 내부 동작 원리를 설명할 때는 em을 직접 쓰는 방식으로 설명하는 게 더 명확하기 때문
> 실무에서는 Spring Data JPA가 em을 대신 처리해준다

---

### em이 어디서 오는가?

```java
// 방법 1: @PersistenceContext (가장 일반적)
@Repository
public class MemberRepository {

    @PersistenceContext
    private EntityManager em; // 스프링이 자동으로 주입해줌

    public Member find(Long id) {
        return em.find(Member.class, id);
    }
}

// 방법 2: 생성자 주입 (스프링 최신 방식)
@Repository
@RequiredArgsConstructor
public class MemberRepository {

    private final EntityManager em; // 생성자로 주입

    public Member find(Long id) {
        return em.find(Member.class, id);
    }
}
```

> 💡 `em`은 스프링이 자동으로 만들어서 주입해준다.
> 직접 `new EntityManager()`로 만드는 게 아니다.

---

## 2. 1L이 뭔가요? (Java 타입 시스템)

### Java 숫자 타입

```java
int  a = 1;    // 32비트 정수 (-21억 ~ 21억)
long b = 1L;   // 64비트 정수 (-922경 ~ 922경)
```

### L의 의미

```java
1    // int 타입 리터럴 (정수의 기본 타입)
1L   // long 타입 리터럴 (L을 붙여서 long임을 명시)

// 아래 둘은 값은 같지만 타입이 다름
int  x = 1;   // int
long y = 1L;  // long
```

---

### JPA에서 왜 Long을 쓰나?

JPA 엔티티의 ID는 **Long을 권장**한다.

```java
@Entity
public class Member {

    @Id
    @GeneratedValue
    private Long id; // int 아닌 Long 사용

    private String name;
}
```

**이유:**

| 타입 | 최대값 | 문제 |
|------|--------|------|
| int | 약 21억 | 대형 서비스에서 21억 초과 시 오버플로우 |
| Long | 약 922경 | 사실상 무제한 |

> 💡 올리브영 같은 대형 이커머스에서 주문 건수가 수십억 건이 넘으면
> int ID가 오버플로우된다. 그래서 Long을 쓴다.

---

### em.find()에서 1L이 나오는 이유

```java
// em.find(클래스, PK값)
em.find(Member.class, 1L);
//                    ↑
//            ID가 Long 타입이라서 1L로 작성
//            만약 int 타입 ID라면 em.find(Member.class, 1)

// 이건 "ID가 1번인 Member를 조회해줘" 라는 뜻
```

```java
// 실제로 어떻게 동작하는지
Member member = em.find(Member.class, 1L);
// 내부적으로 이런 SQL 실행
// SELECT * FROM member WHERE id = 1;
```

---

### 정리

```java
// 예제 코드에서 자주 보이는 패턴
Member member = em.find(Member.class, 1L);
//               ↑         ↑           ↑
//         EntityManager  조회할 클래스  조회할 ID (Long 타입)
```

---

## 3. 어노테이션 완전 정복

### 엔티티 기본 어노테이션

#### @Entity

```java
@Entity // ← 이 클래스가 JPA가 관리하는 테이블과 매핑된다는 표시
public class Member {
    ...
}
```

- 이 어노테이션이 없으면 JPA가 이 클래스를 DB 테이블과 연결하지 않음
- 기본적으로 클래스명 = 테이블명 (`Member` 클래스 → `member` 테이블)

```java
@Entity
@Table(name = "tb_member") // 테이블명이 다를 때 직접 지정
public class Member {
    ...
}
```

---

#### @Id

```java
@Entity
public class Member {

    @Id // ← 이 필드가 DB의 PK(기본키)임을 표시
    private Long id;
}
```

---

#### @GeneratedValue

```java
@Entity
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // ↑ ID 값을 DB가 자동으로 생성해준다는 표시
    // INSERT 시 DB의 AUTO_INCREMENT 사용
    private Long id;
}
```

**전략 종류:**

| 전략 | 설명 | 주로 사용 DB |
|------|------|------------|
| `IDENTITY` | DB의 AUTO_INCREMENT 사용 | MySQL, PostgreSQL |
| `SEQUENCE` | DB 시퀀스 사용 | Oracle |
| `TABLE` | 키 전용 테이블 사용 | 모든 DB |
| `AUTO` | DB에 맞게 자동 선택 (기본값) | - |

> 💡 MySQL(또는 MariaDB) 사용 시 `IDENTITY`, 실무에서 가장 많이 씀

---

#### @Column

```java
@Entity
public class Member {

    @Id
    @GeneratedValue
    private Long id;

    @Column(name = "member_name", nullable = false, length = 50)
    // ↑ DB 컬럼과 매핑 설정
    // name: DB 컬럼명 (생략 시 필드명 그대로 사용)
    // nullable: NOT NULL 여부
    // length: 문자열 최대 길이
    private String name;

    @Column(updatable = false) // 수정 불가 컬럼 (등록일 등)
    private LocalDateTime createdAt;
}
```

---

### 연관관계 어노테이션

#### @ManyToOne

```java
@Entity
public class Order {

    @ManyToOne(fetch = FetchType.LAZY)
    // ↑ 여러 주문(Many) → 한 명의 회원(One)
    // fetch = LAZY: 지연 로딩 (Order 조회 시 Member는 나중에 필요할 때 조회)
    @JoinColumn(name = "member_id")
    // ↑ FK 컬럼명 지정 (orders 테이블의 member_id 컬럼)
    private Member member;
}
```

**fetch 옵션:**

```java
fetch = FetchType.LAZY   // 지연 로딩 - 필요할 때 조회 (권장)
fetch = FetchType.EAGER  // 즉시 로딩 - 항상 함께 조회 (비권장)
```

---

#### @OneToMany

```java
@Entity
public class Member {

    @OneToMany(mappedBy = "member")
    // ↑ 한 명의 회원(One) → 여러 주문(Many)
    // mappedBy = "member": Order 클래스의 member 필드가 연관관계 주인임을 표시
    //                      이쪽은 읽기 전용 (DB에 영향 없음)
    private List<Order> orders = new ArrayList<>();
}
```

> ⚠️ `mappedBy`가 붙은 쪽은 **가짜 주인** = DB에 쓰기 불가 (읽기만 가능)
> FK를 가진 쪽(`@JoinColumn`이 있는 쪽)이 **진짜 주인**

---

#### @JoinColumn

```java
@ManyToOne
@JoinColumn(name = "member_id")
// ↑ orders 테이블에 member_id 컬럼이 FK로 존재한다는 표시
// name: FK 컬럼명
private Member member;
```

**DB 실제 테이블:**
```sql
CREATE TABLE orders (
    id         BIGINT PRIMARY KEY,
    member_id  BIGINT,  -- ← @JoinColumn(name = "member_id")가 만드는 컬럼
    FOREIGN KEY (member_id) REFERENCES member(id)
);
```

---

#### mappedBy 쉽게 이해하기

```
회원(Member) - 주문(Order) 관계에서

DB 테이블:
  orders 테이블에 member_id 컬럼(FK)이 있음
  → FK를 가진 Order가 연관관계 주인

Java 코드:
  Order.member    → @JoinColumn → "나(Order)가 주인이야, FK 내가 관리해"
  Member.orders   → mappedBy   → "주인은 Order야, 나는 그냥 읽기만 해"
```

---

### 로딩 관련 어노테이션

#### @BatchSize

```java
@Entity
public class Order {

    @BatchSize(size = 100)
    // ↑ 이 컬렉션을 조회할 때 한 번에 100개씩 IN절로 가져온다
    // N+1 문제 해결 방법 중 하나
    @OneToMany(mappedBy = "order")
    private List<OrderItem> orderItems;
}
```

실제 실행 SQL:
```sql
-- BatchSize 없을 때 (N+1)
SELECT * FROM order_item WHERE order_id = 1;
SELECT * FROM order_item WHERE order_id = 2;
SELECT * FROM order_item WHERE order_id = 3;
-- ...N번 반복

-- BatchSize(100) 적용 후
SELECT * FROM order_item WHERE order_id IN (1, 2, 3, ..., 100);
-- 한 방에 해결!
```

---

### 트랜잭션 어노테이션

#### @Transactional

```java
@Service
public class OrderService {

    @Transactional
    // ↑ 이 메서드는 트랜잭션 안에서 실행된다
    // 메서드 시작 → 트랜잭션 시작
    // 메서드 정상 종료 → commit (DB에 확정)
    // 예외 발생 → rollback (DB 변경 취소)
    public void createOrder(Long memberId) {
        // 이 안에서 DB 변경이 일어나도
        // 예외가 터지면 전부 없던 일이 됨
    }

    @Transactional(readOnly = true)
    // ↑ 조회 전용 트랜잭션
    // 변경 감지(Dirty Checking) 안 함 → 성능 약간 향상
    public List<Order> findOrders() {
        return orderRepository.findAll();
    }
}
```

**전파 옵션 (propagation):**

```java
// 기본값 - 트랜잭션 있으면 참여, 없으면 새로 생성
@Transactional(propagation = Propagation.REQUIRED)

// 항상 새 트랜잭션 생성 (기존 트랜잭션과 분리)
@Transactional(propagation = Propagation.REQUIRES_NEW)
```

---

### 쿼리 관련 어노테이션

#### @Query

```java
public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("SELECT o FROM Order o WHERE o.member.id = :memberId")
    // ↑ JPQL(Java Persistence Query Language)로 직접 쿼리 작성
    // SQL이 아닌 엔티티/필드명으로 작성
    // :memberId → 파라미터 바인딩
    List<Order> findByMemberId(@Param("memberId") Long memberId);

    @Query(value = "SELECT * FROM orders WHERE member_id = :memberId", nativeQuery = true)
    // ↑ nativeQuery = true: 실제 SQL로 작성 (테이블명/컬럼명 사용)
    List<Order> findByMemberIdNative(@Param("memberId") Long memberId);
}
```

---

### 계층 구조 어노테이션

```java
@Entity         // DB 테이블과 매핑되는 클래스
@Repository     // DAO 계층 (데이터 접근)
@Service        // 비즈니스 로직 계층
@RestController // API 컨트롤러 계층
```

**실제 구조:**
```
[클라이언트]
     ↓ HTTP 요청
@RestController (OrderController)
     ↓ 메서드 호출
@Service        (OrderService)   ← @Transactional 주로 여기
     ↓ 데이터 접근
@Repository     (OrderRepository)
     ↓ JPA
[DB]
```

---

## 4. 영속성 컨텍스트 재정리 (더 쉽게)

### 쉬운 비유

```
영속성 컨텍스트 = JPA가 관리하는 "임시 저장소"

[Java 코드] ←→ [영속성 컨텍스트] ←→ [DB]
                (1차 캐시, 변경감지)
```

### 흐름으로 이해하기

```java
@Transactional
public void example() {

    // 1. DB에서 조회 → 영속성 컨텍스트에 저장
    Member member = memberRepository.findById(1L).orElseThrow();
    // SQL: SELECT * FROM member WHERE id = 1;
    // 이 시점에 영속성 컨텍스트에 member 저장 + 스냅샷 저장

    // 2. 같은 ID로 다시 조회
    Member sameMember = memberRepository.findById(1L).orElseThrow();
    // SQL 실행 안 함! 영속성 컨텍스트에서 바로 반환
    // member == sameMember → true

    // 3. 값 변경
    member.setName("새이름");
    // em.update() 같은 거 안 불러도 됨
    // 트랜잭션 종료 시점에 스냅샷과 비교해서 자동 UPDATE

} // 트랜잭션 종료 → 변경 감지 → UPDATE SQL 실행 → commit
```

### 스냅샷이 뭔가요?

```
조회 시점:
  스냅샷 저장: { id: 1, name: "원래이름" }
  실제 엔티티: { id: 1, name: "원래이름" }

name 변경 후:
  스냅샷 저장: { id: 1, name: "원래이름" }  ← 변하지 않음
  실제 엔티티: { id: 1, name: "새이름" }    ← 변경됨

트랜잭션 종료 시:
  스냅샷 vs 엔티티 비교 → name이 다르다!
  → UPDATE member SET name = '새이름' WHERE id = 1; 자동 실행
```

---

### 엔티티 4가지 상태 (em 없이 Spring 방식으로 재설명)

```java
// 비영속 (new) - JPA가 모르는 상태
Member member = new Member();
member.setName("윤효근");
// 이 시점엔 그냥 자바 객체. DB랑 관계없음

// 영속 (managed) - JPA가 관리 시작
memberRepository.save(member);
// save() 내부에서 em.persist() 호출
// 이제 JPA가 이 객체를 감시함 (변경 감지 시작)

// 준영속 (detached) - JPA 관리에서 벗어남
// 트랜잭션이 끝나거나 영속성 컨텍스트가 종료되면 자동으로 준영속 상태가 됨
// 이 상태에서 값 바꿔도 DB에 반영 안 됨

// 삭제 (removed)
memberRepository.delete(member);
// DELETE SQL 예약 → 트랜잭션 종료 시 실행
```

---

### 전체 흐름 그림

```
새 객체 생성
    new Member()
        │
        ▼ save() / em.persist()
   [영속 상태]
   영속성 컨텍스트가 관리
        │
        ├── 조회 → 1차 캐시 활용
        ├── 수정 → 변경 감지 (트랜잭션 종료 시 자동 UPDATE)
        ├── 삭제 → delete() → 트랜잭션 종료 시 DELETE
        │
        ▼ 트랜잭션 종료
   [준영속 상태]
   JPA 관리 밖 (일반 자바 객체처럼 동작)
```

---

## 요약 - 헷갈리는 것들

| 궁금증 | 답 |
|--------|-----|
| em이 뭔가요? | EntityManager - JPA가 DB와 대화하는 객체 |
| 실무에서 em 직접 쓰나요? | 거의 안 씀. Spring Data JPA가 대신 처리 |
| 1L이 뭔가요? | Long 타입 숫자 리터럴. 1과 값은 같지만 타입이 Long |
| ID를 왜 Long으로? | int는 21억 제한, 대형 서비스에서 오버플로우 위험 |
| @Entity 없으면? | JPA가 그 클래스를 DB 테이블로 인식 안 함 |
| mappedBy가 있으면? | 읽기 전용. DB에 쓰기 불가 |
| @Transactional 없으면? | 변경 감지 안 됨, 트랜잭션 없어서 오류 가능 |

---

> 궁금한 거 생기면 바로 물어볼 것!
