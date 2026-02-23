# JPA 심화학습 - 완전 정복 디테일

> JPA_심화학습.md의 모든 내용을 "왜?"부터 상세하게 설명
> 처음 보는 코드도 이해할 수 있도록 작성

---

## 목차
1. [em이 뭔가요? (EntityManager)](#1-em이-뭔가요)
2. [1L이 뭔가요? (Java 타입)](#2-1l이-뭔가요)
3. [어노테이션 완전 정복](#3-어노테이션-완전-정복)
4. [영속성 컨텍스트 완전 이해](#4-영속성-컨텍스트-완전-이해)
5. [N+1 문제 완전 이해](#5-n1-문제-완전-이해)
6. [프록시와 지연 로딩 완전 이해](#6-프록시와-지연-로딩-완전-이해)
7. [연관관계 매핑 완전 이해](#7-연관관계-매핑-완전-이해)
8. [트랜잭션 완전 이해](#8-트랜잭션-완전-이해)
9. [Spring Data JPA 완전 이해](#9-spring-data-jpa-완전-이해)
10. [QueryDSL 완전 이해](#10-querydsl-완전-이해)
11. [추가로 알아야 할 것들](#11-추가로-알아야-할-것들)
12. [면접 빈출 질문 모음](#12-면접-빈출-질문-모음)

---

## 1. em이 뭔가요?

### em = EntityManager (JPA의 핵심 객체)

JPA에서 DB와 직접 대화하는 객체다.
`em`은 `EntityManager`를 줄여 쓴 **변수명**일 뿐이다.

```java
// em은 그냥 변수명. 아래 둘은 완전히 같은 코드
EntityManager em = ...;
em.find(Member.class, 1L);

EntityManager entityManager = ...;
entityManager.find(Member.class, 1L);
```

---

### EntityManager가 하는 일

| 메서드 | 역할 | 실행 SQL |
|--------|------|---------|
| `em.persist(entity)` | 엔티티 저장 (영속 상태로 만들기) | INSERT |
| `em.find(Class, id)` | PK로 엔티티 단건 조회 | SELECT |
| `em.remove(entity)` | 엔티티 삭제 | DELETE |
| `em.merge(entity)` | 준영속 엔티티를 다시 영속 상태로 | UPDATE |
| `em.flush()` | 영속성 컨텍스트 변경사항을 DB에 즉시 반영 | - |
| `em.clear()` | 영속성 컨텍스트 초기화 (1차 캐시 비우기) | - |
| `em.detach(entity)` | 특정 엔티티만 준영속 상태로 전환 | - |

---

### 실무에서 em을 직접 쓰나요?

**거의 안 씁니다.** 실무에서는 Spring Data JPA가 em을 대신 처리한다.

```
순수 JPA:     EntityManager 직접 사용
Spring Data JPA: JpaRepository 인터페이스 상속 → 자동으로 em 처리
```

```java
// ❌ 순수 JPA 방식 (em 직접)
@Repository
public class MemberRepository {
    @PersistenceContext
    private EntityManager em;

    public Member findById(Long id) {
        return em.find(Member.class, id); // 직접 호출
    }
    public void save(Member member) {
        em.persist(member); // 직접 호출
    }
}

// ✅ Spring Data JPA 방식 (실무)
public interface MemberRepository extends JpaRepository<Member, Long> {
    // 아무것도 안 써도 아래가 자동 제공됨
    // findById(Long id)
    // save(Member member)
    // delete(Member member)
    // findAll()
}
```

> 💡 면접 설명에서 em이 나오는 이유:
> JPA 내부 동작 원리를 설명할 때 em으로 설명하는 게 더 직관적이기 때문.
> 실제로는 Spring Data JPA가 뒤에서 em을 쓰고 있다.

---

### em은 어디서 오는가?

```java
// 방법 1: @PersistenceContext (전통 방식)
@Repository
public class MemberRepository {
    @PersistenceContext             // 스프링이 자동으로 주입
    private EntityManager em;
}

// 방법 2: 생성자 주입 (최신 방식, QueryDSL에서 많이 씀)
@Repository
@RequiredArgsConstructor           // Lombok - 생성자 자동 생성
public class MemberQueryRepository {
    private final EntityManager em; // final + @RequiredArgsConstructor = 자동 주입
}
```

> 💡 `new EntityManager()`로 직접 만드는 게 아니라
> 스프링 컨테이너가 알아서 만들어서 주입해준다.

---

## 2. 1L이 뭔가요?

### Java 기본 숫자 타입

```java
byte   a = 1;    // 8비트  (-128 ~ 127)
short  b = 1;    // 16비트 (-32,768 ~ 32,767)
int    c = 1;    // 32비트 (-21억 ~ 21억)     ← 정수의 기본 타입
long   d = 1L;   // 64비트 (-922경 ~ 922경)
float  e = 1.0f; // 32비트 실수
double f = 1.0;  // 64비트 실수               ← 실수의 기본 타입
```

### L의 의미

```java
1     // 이건 int 타입 (Java에서 정수 기본값은 int)
1L    // 이건 long 타입 (L을 붙여서 long임을 명시)
1l    // 소문자 l도 되지만 숫자 1과 헷갈려서 대문자 L 권장

// 에러 예시
long x = 9999999999;   // 컴파일 에러! int 범위 초과
long y = 9999999999L;  // OK - long임을 명시
```

### JPA에서 ID를 Long으로 쓰는 이유

```java
@Entity
public class Order {
    @Id
    @GeneratedValue
    private Long id;   // int 아닌 Long ← 왜?
}
```

| 타입 | 최대값 | 문제 상황 |
|------|--------|---------|
| `int` | 약 21억 (2,147,483,647) | 주문이 21억 건 넘으면 오버플로우 |
| `Long` | 약 922경 | 사실상 무제한 |

> 💡 올리브영처럼 하루 수백만 건 주문이 쌓이는 대형 이커머스는
> int ID가 몇 년 내로 오버플로우될 수 있다. 그래서 Long을 쓴다.

### em.find()에서 1L 이해

```java
em.find(Member.class, 1L);
//       ↑              ↑
//   조회할 엔티티 타입   PK 값 (Member의 id가 Long 타입이라 1L)

// 실제로 실행되는 SQL
// SELECT * FROM member WHERE id = 1;
```

---

## 3. 어노테이션 완전 정복

### 엔티티 기본 어노테이션

#### @Entity

```java
@Entity
// ↑ "이 클래스는 DB 테이블과 연결됩니다"
// 없으면 JPA가 무시함
public class Member {
    ...
}
```

기본적으로 **클래스명 소문자 = 테이블명**이다.

```java
@Entity               // → member 테이블
@Entity               // → order 테이블
@Table(name = "tb_member") // 테이블명 직접 지정 가능
public class Member { }
```

---

#### @Id, @GeneratedValue

```java
@Entity
public class Member {

    @Id                                                    // PK 지정
    @GeneratedValue(strategy = GenerationType.IDENTITY)   // 자동 증가
    private Long id;
}
```

**@GeneratedValue 전략:**

```java
// 1. IDENTITY - MySQL AUTO_INCREMENT (가장 많이 씀)
@GeneratedValue(strategy = GenerationType.IDENTITY)
// INSERT 후 DB가 생성한 ID를 받아옴
// INSERT INTO member ... → id = 1 자동 생성

// 2. SEQUENCE - Oracle 시퀀스
@GeneratedValue(strategy = GenerationType.SEQUENCE)

// 3. AUTO - DB에 맞게 자동 선택 (기본값)
@GeneratedValue  // strategy 생략 시 AUTO
```

---

#### @Column

```java
@Entity
public class Member {

    @Id @GeneratedValue
    private Long id;

    @Column(
        name = "member_name",   // DB 컬럼명 (생략 시 필드명 그대로)
        nullable = false,        // NOT NULL 제약조건
        length = 50,             // VARCHAR(50)
        unique = true,           // UNIQUE 제약조건
        updatable = false        // UPDATE 시 이 컬럼 제외 (수정 불가)
    )
    private String name;

    // @Column 생략 시 → 필드명이 그대로 컬럼명이 됨
    private String email;        // → email 컬럼
}
```

---

#### @Enumerated

```java
// Enum 타입을 DB에 저장할 때 사용
public enum OrderStatus {
    ORDER, CANCEL
}

@Entity
public class Order {

    // ❌ 절대 쓰면 안 됨
    @Enumerated(EnumType.ORDINAL)  // 숫자로 저장 (0, 1)
    // Enum 순서가 바뀌면 기존 데이터 망가짐

    // ✅ 항상 이걸 써야 함
    @Enumerated(EnumType.STRING)   // 문자열로 저장 ("ORDER", "CANCEL")
    private OrderStatus status;
}
```

---

#### @CreatedDate, @LastModifiedDate (등록일/수정일 자동화)

```java
// 설정 파일에 추가
@EnableJpaAuditing  // 메인 클래스 또는 Config에 추가

@Entity
@EntityListeners(AuditingEntityListener.class)  // Auditing 활성화
public class Member {

    @Id @GeneratedValue
    private Long id;

    @CreatedDate
    @Column(updatable = false)   // 등록 후 수정 불가
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
// save() 호출 시 자동으로 createdAt, updatedAt 채워짐
```

---

### 연관관계 어노테이션

#### @ManyToOne - N:1 관계

```java
// 주문(N) - 회원(1): 여러 주문이 한 회원에 속함
@Entity
public class Order {

    @ManyToOne(fetch = FetchType.LAZY)
    //  ↑Many = Order (여러 주문)
    //       ↑One  = Member (한 회원)
    @JoinColumn(name = "member_id")  // orders 테이블의 FK 컬럼명
    private Member member;
}
```

실제 DB:
```sql
CREATE TABLE orders (
    id        BIGINT PRIMARY KEY AUTO_INCREMENT,
    member_id BIGINT,                             -- @JoinColumn(name = "member_id")
    FOREIGN KEY (member_id) REFERENCES member(id)
);
```

---

#### @OneToMany - 1:N 관계

```java
// 회원(1) - 주문(N): 한 회원이 여러 주문을 가짐
@Entity
public class Member {

    @OneToMany(mappedBy = "member")
    //  ↑One = Member (한 회원)
    //      ↑Many = Order (여러 주문)
    //              ↑ "Order 클래스의 member 필드가 주인이야"
    private List<Order> orders = new ArrayList<>();
    //                           ↑ null 방지를 위해 빈 리스트로 초기화
}
```

---

#### @JoinColumn vs mappedBy 차이

```
@JoinColumn  = "나(이 엔티티)가 FK를 가진 주인이야"
mappedBy     = "저쪽(다른 엔티티)이 주인이야, 나는 읽기만"

DB:
  orders 테이블에 member_id(FK) 컬럼이 있음
  → FK를 가진 Order.member가 주인
  → Order에 @JoinColumn, Member에 mappedBy
```

```java
// 연관관계 주인 (Order.member)
@ManyToOne
@JoinColumn(name = "member_id")  // ← 주인: DB에 쓰기 가능
private Member member;

// 반대편 (Member.orders)
@OneToMany(mappedBy = "member")  // ← 거울: 읽기 전용
private List<Order> orders;
```

---

#### @OneToOne - 1:1 관계

```java
// 회원(1) - 배송지(1): 한 회원에 하나의 대표 배송지
@Entity
public class Member {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_id")
    private Delivery delivery;
}

@Entity
public class Delivery {

    @OneToOne(mappedBy = "delivery")  // 읽기 전용
    private Member member;
}
```

---

#### cascade - 연쇄 저장/삭제

```java
@Entity
public class Order {

    @OneToMany(
        mappedBy = "order",
        cascade = CascadeType.ALL,  // Order 저장/삭제 시 OrderItem도 함께
        orphanRemoval = true         // Order에서 제거된 OrderItem은 DB에서도 삭제
    )
    private List<OrderItem> orderItems = new ArrayList<>();
}
```

**cascade 종류:**

| 옵션 | 동작 |
|------|------|
| `PERSIST` | 부모 저장 시 자식도 자동 저장 |
| `REMOVE` | 부모 삭제 시 자식도 자동 삭제 |
| `ALL` | 위 전부 적용 |
| `MERGE` | 부모 병합 시 자식도 병합 |

```java
// cascade 예시
Order order = new Order();
OrderItem item1 = new OrderItem();
OrderItem item2 = new OrderItem();

order.getOrderItems().add(item1);
order.getOrderItems().add(item2);

orderRepository.save(order);
// cascade = PERSIST 덕분에
// item1, item2 도 자동으로 INSERT됨
// itemRepository.save(item1) 별도 호출 불필요
```

> ⚠️ cascade 사용 조건:
> - 완전히 종속적인 관계에서만 사용 (OrderItem은 Order 없이 존재 불가 → OK)
> - 다른 엔티티에서도 참조하면 절대 사용 금지

---

#### @Embedded, @Embeddable - 값 타입

```java
// 주소처럼 여러 엔티티에서 반복 사용되는 값 타입
@Embeddable  // "나는 다른 엔티티에 포함될 수 있어"
public class Address {
    private String city;
    private String street;
    private String zipcode;
}

@Entity
public class Member {
    @Embedded  // "Address를 여기에 포함"
    private Address homeAddress;

    @Embedded
    @AttributeOverrides({  // 같은 타입을 2번 쓸 때 컬럼명 구분
        @AttributeOverride(name = "city",    column = @Column(name = "work_city")),
        @AttributeOverride(name = "street",  column = @Column(name = "work_street")),
        @AttributeOverride(name = "zipcode", column = @Column(name = "work_zipcode"))
    })
    private Address workAddress;
}
```

DB 테이블 (member):
```sql
id, city, street, zipcode,          -- homeAddress
    work_city, work_street, work_zipcode  -- workAddress
```

---

### 로딩/캐시 어노테이션

#### @BatchSize

```java
@Entity
public class Order {
    @BatchSize(size = 100)
    // ↑ 이 컬렉션을 N+1 없이 100개씩 IN절로 조회
    @OneToMany(mappedBy = "order")
    private List<OrderItem> orderItems;
}
```

```sql
-- 없을 때 (N+1)
SELECT * FROM order_item WHERE order_id = 1;
SELECT * FROM order_item WHERE order_id = 2;
-- N번 반복

-- @BatchSize(100) 적용 후
SELECT * FROM order_item WHERE order_id IN (1,2,3,...,100);
-- 1번으로 끝
```

application.yml에 전체 적용:
```yaml
spring:
  jpa:
    properties:
      hibernate:
        default_batch_fetch_size: 100
```

---

### 스프링 계층 어노테이션

```java
@Entity         // JPA 엔티티 (DB 테이블 매핑)
@Repository     // 데이터 접근 계층 (DAO) - DB 예외를 스프링 예외로 변환
@Service        // 비즈니스 로직 계층
@RestController // HTTP API 컨트롤러 (@Controller + @ResponseBody)
@Component      // 위 어노테이션들의 부모 - 스프링 빈으로 등록
```

**전체 요청 흐름:**
```
클라이언트 HTTP 요청
    ↓
@RestController (OrderController)  ← HTTP 요청/응답 처리
    ↓
@Service        (OrderService)     ← 비즈니스 로직, @Transactional
    ↓
@Repository     (OrderRepository)  ← DB 접근
    ↓
JPA / DB
```

---

## 4. 영속성 컨텍스트 완전 이해

### 한 문장 정의

> **영속성 컨텍스트 = JPA가 엔티티를 관리하는 임시 저장소**
> 트랜잭션 시작 시 생성되고, 트랜잭션 종료 시 사라진다.

```
[내 Java 코드]  ←→  [영속성 컨텍스트]  ←→  [DB]
                      (1차 캐시)
                      (쓰기 지연 저장소)
                      (변경 감지용 스냅샷)
```

---

### 엔티티 4가지 상태

```java
// 1. 비영속 (new/transient)
// JPA가 전혀 모르는 상태. 그냥 자바 객체
Member member = new Member();
member.setName("윤효근");
// → DB와 무관. 아무 SQL 없음

// 2. 영속 (managed)
// 영속성 컨텍스트가 관리 시작. 변경 감지 시작
memberRepository.save(member);       // Spring Data JPA
// 또는 em.persist(member);          // 순수 JPA
// → 1차 캐시에 저장 + 스냅샷 저장

// 3. 준영속 (detached)
// 영속 → 관리 해제. 변경해도 DB 반영 안 됨
// 트랜잭션 종료 시 자동으로 준영속 상태
// 또는 em.detach(member) 로 명시적 해제

// 4. 삭제 (removed)
memberRepository.delete(member);
// em.remove(member)
// → DELETE SQL 예약 → 트랜잭션 종료 시 실행
```

---

### 핵심 기능 3가지 상세 설명

#### 1차 캐시

```java
@Transactional
public void cacheExample() {

    // 첫 조회 → DB 접근 → 1차 캐시 저장
    Member m1 = memberRepository.findById(1L).orElseThrow();
    // 실행: SELECT * FROM member WHERE id = 1;

    // 같은 트랜잭션에서 같은 ID 재조회 → 1차 캐시 반환 (SQL 없음!)
    Member m2 = memberRepository.findById(1L).orElseThrow();
    // SQL 실행 안 함

    System.out.println(m1 == m2);  // true (같은 객체 인스턴스)
}
```

> 💡 1차 캐시는 트랜잭션 단위로 동작한다.
> 트랜잭션이 끝나면 캐시도 사라진다 (서버 전체에서 공유하는 캐시가 아님).

---

#### 쓰기 지연 (Write-Behind)

```java
@Transactional
public void batchInsert() {
    // 이 시점에 INSERT SQL이 DB로 가는 게 아님
    memberRepository.save(new Member("회원1"));  // SQL 저장소에 보관
    memberRepository.save(new Member("회원2"));  // SQL 저장소에 보관
    memberRepository.save(new Member("회원3"));  // SQL 저장소에 보관

    // 트랜잭션 종료(커밋) 시점에 한꺼번에 전송
    // INSERT INTO member VALUES ('회원1');
    // INSERT INTO member VALUES ('회원2');
    // INSERT INTO member VALUES ('회원3');
}
// DB 네트워크 통신을 3번 → 1번으로 줄임
```

---

#### 변경 감지 (Dirty Checking)

```java
@Transactional
public void updateExample(Long memberId) {

    Member member = memberRepository.findById(memberId).orElseThrow();
    // 이 시점에 스냅샷 저장: { id: 1, name: "원래이름" }

    member.setName("새이름");  // 엔티티 값 변경
    // memberRepository.save(member) 호출 불필요!

    // 트랜잭션 종료 시:
    // 스냅샷 { name: "원래이름" } vs 현재 { name: "새이름" } 비교
    // 다르면 자동으로 UPDATE SQL 생성
    // UPDATE member SET name = '새이름' WHERE id = 1;
}
```

> ⚠️ 변경 감지는 **트랜잭션 안에서만** 동작한다.
> @Transactional 없으면 save() 직접 호출해야 한다.

---

### flush() vs commit()

```java
// flush() - 영속성 컨텍스트 변경사항을 DB에 반영
//           하지만 트랜잭션은 유지됨 (롤백 가능)
em.flush();

// commit() - flush() + 트랜잭션 확정
//            롤백 불가능
transaction.commit();
```

```
영속성 컨텍스트 변경 → flush() → DB에 SQL 전송 (아직 롤백 가능)
                    → commit() → DB 확정 (롤백 불가)
```

---

## 5. N+1 문제 완전 이해

### N+1이란?

```
1번의 쿼리를 날렸더니
연관된 엔티티를 로딩하느라
N번의 추가 쿼리가 더 나가는 문제
```

### 실제 발생 시나리오

```java
@Entity
public class Order {
    @Id @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)  // 즉시 로딩 → N+1 원인
    @JoinColumn(name = "member_id")
    private Member member;
}

// 서비스
List<Order> orders = orderRepository.findAll();
// orders 크기가 100이면?

// 실행 SQL:
// SELECT * FROM orders;                        ← 1번 (전체 조회)
// SELECT * FROM member WHERE id = 1;           ← N번 시작
// SELECT * FROM member WHERE id = 2;
// SELECT * FROM member WHERE id = 3;
// ...
// SELECT * FROM member WHERE id = 100;         ← 총 101번!
```

---

### LAZY여도 N+1이 발생하는 경우

```java
@ManyToOne(fetch = FetchType.LAZY)  // LAZY로 설정해도
private Member member;

// 서비스
List<Order> orders = orderRepository.findAll(); // SQL 1번
for (Order order : orders) {
    System.out.println(order.getMember().getName());
    // getMember().getName() 호출할 때마다 SQL 1번씩
    // → 결국 N+1 발생
}
```

> 💡 LAZY라고 N+1이 안 생기는 게 아니라
> 발생 시점이 늦춰질 뿐이다.

---

### 해결 방법 1: fetch join (가장 많이 씀)

```java
// Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("SELECT o FROM Order o JOIN FETCH o.member")
    //                              ↑ FETCH 키워드가 핵심
    //                                한 번의 JOIN으로 함께 조회
    List<Order> findAllWithMember();
}
```

실행 SQL:
```sql
SELECT o.*, m.*
FROM orders o
INNER JOIN member m ON o.member_id = m.id;
-- 쿼리 딱 1번
```

**컬렉션 fetch join 주의사항:**
```java
// OneToMany fetch join 시 데이터 중복 발생!
@Query("SELECT o FROM Order o JOIN FETCH o.orderItems")
List<Order> findAllWithItems();

// Order 1개에 OrderItem 3개 → 결과 3행 → Order가 3개로 뻥튀기됨

// 해결: DISTINCT 추가
@Query("SELECT DISTINCT o FROM Order o JOIN FETCH o.orderItems")
List<Order> findAllWithItems();
```

---

### 해결 방법 2: @EntityGraph

```java
// JPQL 없이 fetch join 효과
@EntityGraph(attributePaths = {"member"})
@Query("SELECT o FROM Order o")
List<Order> findAllWithMember();

// 메서드 이름 쿼리에도 적용 가능
@EntityGraph(attributePaths = {"member"})
List<Order> findByStatus(OrderStatus status);
```

---

### 해결 방법 3: @BatchSize (컬렉션에 효과적)

```java
@Entity
public class Order {
    @BatchSize(size = 100)
    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY)
    private List<OrderItem> orderItems;
}
```

```sql
-- N+1 없이 IN절로 한 번에
SELECT * FROM orders;                                           -- 1번
SELECT * FROM order_item WHERE order_id IN (1,2,3,...,100);    -- 1번
```

---

### 세 방법 언제 쓰나?

| 상황 | 권장 방법 |
|------|---------|
| ManyToOne, OneToOne 조회 | fetch join |
| 간단한 조회 (JPQL 없애고 싶을 때) | @EntityGraph |
| OneToMany 컬렉션 조회 | @BatchSize |
| 페이징 + 컬렉션 | @BatchSize (fetch join 페이징 불가) |

> ⚠️ **컬렉션 fetch join은 페이징 불가**
> `SELECT DISTINCT o FROM Order o JOIN FETCH o.orderItems` 에 페이징 적용 시
> Hibernate가 경고를 내고 전체를 메모리에 올려 페이징 → 메모리 폭발 위험

---

## 6. 프록시와 지연 로딩 완전 이해

### 프록시가 뭔가요?

```
프록시 = 진짜 엔티티 대신 들어가는 가짜 객체 (껍데기)
LAZY 설정 시 → 진짜 객체 대신 프록시가 들어옴
실제 값이 필요할 때 → 프록시가 진짜 객체를 DB에서 조회
```

```java
@ManyToOne(fetch = FetchType.LAZY)
private Member member;

Order order = orderRepository.findById(1L).orElseThrow();
// order.member → 프록시 객체 (껍데기, id 값만 있음)

String name = order.getMember().getName();
//                              ↑ 여기서 처음으로 DB 조회
//                              프록시 → 진짜 Member 객체로 초기화
// SQL: SELECT * FROM member WHERE id = ?;
```

---

### JPA 연관관계 기본값

| 어노테이션 | 기본 로딩 전략 | 권장 |
|-----------|--------------|------|
| `@ManyToOne` | EAGER (즉시 로딩) | LAZY로 변경 |
| `@OneToOne` | EAGER (즉시 로딩) | LAZY로 변경 |
| `@OneToMany` | LAZY (지연 로딩) | 유지 |
| `@ManyToMany` | LAZY (지연 로딩) | 유지 |

> ⚠️ 실무 원칙: **모든 연관관계는 LAZY로 설정**
> EAGER는 예측 불가능한 SQL이 나가고 N+1 문제의 원인이 됨

---

### 즉시 로딩의 문제점

```java
// EAGER 설정 시
@ManyToOne(fetch = FetchType.EAGER)
private Member member;

@ManyToOne(fetch = FetchType.EAGER)
private Coupon coupon;

@ManyToOne(fetch = FetchType.EAGER)
private Delivery delivery;

// 단순 주문 목록 조회
List<Order> orders = orderRepository.findAll();
// 내 의도: ORDER만 조회
// 실제 SQL: ORDER + MEMBER + COUPON + DELIVERY 전부 JOIN
// 연관관계가 많을수록 JOIN이 폭발적으로 증가
```

---

### LazyInitializationException (흔한 에러)

```java
// 트랜잭션 밖에서 LAZY 조회 시 에러 발생
public Order findOrder(Long id) {
    return orderRepository.findById(id).orElseThrow();
}
// 트랜잭션 종료 → member는 프록시 상태

Order order = findOrder(1L);
order.getMember().getName();  // 💥 LazyInitializationException!
// 트랜잭션이 이미 끝났는데 프록시 초기화 시도
```

**해결 방법:**
```java
// 1. 트랜잭션 안에서 처리
@Transactional
public OrderDto findOrder(Long id) {
    Order order = orderRepository.findById(id).orElseThrow();
    order.getMember().getName();  // 트랜잭션 안 → 정상 동작
    return new OrderDto(order);
}

// 2. fetch join으로 미리 로딩
@Query("SELECT o FROM Order o JOIN FETCH o.member WHERE o.id = :id")
Optional<Order> findWithMember(@Param("id") Long id);
```

---

## 7. 연관관계 매핑 완전 이해

### 연관관계 주인 개념

```
DB는 FK로 관계를 표현 (단방향)
Java는 참조로 관계를 표현 (양방향 가능)

→ 양방향이면 어느 쪽에서 FK를 관리할지 정해야 함
→ 그게 "연관관계 주인"
```

```
orders 테이블:
  id | member_id(FK) | ...

member 테이블:
  id | name | ...

FK는 orders 테이블에 있음
→ Order 엔티티가 FK를 관리하는 주인
→ Order.member에 @JoinColumn
→ Member.orders에 mappedBy
```

---

### 올바른 연관관계 설정 패턴

```java
@Entity
public class Order {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    // 연관관계 편의 메서드 - 양쪽을 동시에 설정
    public void assignMember(Member member) {
        // 이전 member가 있으면 관계 제거
        if (this.member != null) {
            this.member.getOrders().remove(this);
        }
        this.member = member;
        member.getOrders().add(this);
    }
}

// 사용
Order order = new Order();
order.assignMember(member);  // Order.member + Member.orders 동시 설정
orderRepository.save(order);
```

> 💡 편의 메서드를 만드는 이유:
> 주인(Order.member)에만 세팅하면 DB는 정상이지만
> 같은 트랜잭션 안에서 member.getOrders()가 빈 리스트라 버그 발생 가능

---

### @OneToOne 주의사항

```java
// 회원과 배송지가 1:1 관계일 때
// FK를 어느 쪽에 둘지 선택해야 함
// 보통 더 자주 조회하는 쪽에 FK를 둠

@Entity
public class Member {
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "locker_id")  // member 테이블에 FK
    private Locker locker;
}

@Entity
public class Locker {
    @OneToOne(mappedBy = "locker")   // 읽기 전용
    private Member member;
}
```

---

## 8. 트랜잭션 완전 이해

### @Transactional 동작 원리 (AOP 프록시)

```
실제 동작:

OrderService.createOrder() 호출
    ↓
스프링 AOP 프록시가 가로챔
    ↓
1. 트랜잭션 시작
2. 실제 createOrder() 실행
3. 예외 없음 → commit
   예외 발생 → rollback
```

```java
@Service
public class OrderService {

    @Transactional
    public void createOrder(Long memberId, Long itemId) {
        Member member = memberRepository.findById(memberId).orElseThrow();
        Item item = itemRepository.findById(itemId).orElseThrow();

        if (item.getStock() <= 0) {
            throw new RuntimeException("재고 없음");
            // RuntimeException → 자동 rollback
        }

        Order order = Order.createOrder(member, item);
        orderRepository.save(order);
        // 정상 종료 → commit
    }
}
```

---

### 전파 옵션 (Propagation) 상세

```java
// 케이스 1: REQUIRED (기본값)
// 트랜잭션 있으면 참여, 없으면 새로 생성
@Transactional  // = @Transactional(propagation = Propagation.REQUIRED)
public void methodA() {
    methodB();  // A의 트랜잭션에 참여
}

@Transactional
public void methodB() { ... }
// A가 rollback → B도 같이 rollback (같은 트랜잭션이므로)


// 케이스 2: REQUIRES_NEW
// 항상 새 트랜잭션 생성 (기존 트랜잭션 일시 정지)
@Transactional
public void pay(Long orderId) {
    paymentProcess();      // 결제 처리 (같은 트랜잭션)
    logService.log();      // 로그는 별도 트랜잭션
    throw new Exception(); // 결제 rollback → 로그는 유지
}

@Service
class LogService {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log() { ... }
    // 결제 실패해도 로그는 살아남음
}
```

---

### @Transactional 예외 rollback 규칙

```java
@Transactional
public void example() {
    // RuntimeException (unchecked) → 자동 rollback
    throw new RuntimeException();      // ✅ rollback
    throw new IllegalArgumentException(); // ✅ rollback (RuntimeException 자식)

    // Exception (checked) → 기본적으로 rollback 안 함
    throw new Exception();             // ❌ rollback 안 됨
    throw new IOException();           // ❌ rollback 안 됨
}

// checked 예외도 rollback 하려면
@Transactional(rollbackFor = Exception.class)
public void example() throws Exception {
    throw new Exception();  // ✅ rollback
}
```

---

### self-invocation 문제 (자주 나오는 면접 질문)

```java
@Service
public class OrderService {

    @Transactional
    public void outer() {
        inner();  // this.inner() 와 동일
        // ⚠️ 프록시를 거치지 않아서 @Transactional 무시됨
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void inner() {
        // outer()에서 this로 직접 호출 시
        // REQUIRES_NEW 무시되고 outer() 트랜잭션에 그냥 참여
    }
}
```

왜 발생하는가?
```
스프링 @Transactional은 프록시로 동작
외부에서 호출: 클라이언트 → 프록시 → 실제 메서드 (트랜잭션 적용됨)
내부에서 호출: 실제 메서드 → this.메서드 (프록시 안 거침 → 트랜잭션 무시)
```

해결:
```java
// 클래스 분리
@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderInternalService internalService;

    @Transactional
    public void outer() {
        internalService.inner();  // 프록시를 통한 호출 → 정상
    }
}

@Service
public class OrderInternalService {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void inner() { ... }
}
```

---

## 9. Spring Data JPA 완전 이해

### JpaRepository가 제공하는 기본 메서드

```java
public interface MemberRepository extends JpaRepository<Member, Long> {
    // 기본 제공 메서드들
    // save(S entity)                  - 저장/수정
    // findById(ID id)                 - PK로 단건 조회
    // findAll()                       - 전체 조회
    // findAll(Pageable pageable)       - 페이징 조회
    // delete(T entity)                - 삭제
    // deleteById(ID id)               - PK로 삭제
    // count()                         - 전체 카운트
    // existsById(ID id)               - 존재 여부
}
```

---

### 메서드 이름으로 쿼리 자동 생성

```java
public interface MemberRepository extends JpaRepository<Member, Long> {

    // findBy필드명 → WHERE 조건
    List<Member> findByName(String name);
    // SELECT * FROM member WHERE name = ?

    List<Member> findByNameAndEmail(String name, String email);
    // SELECT * FROM member WHERE name = ? AND email = ?

    List<Member> findByAgeGreaterThan(int age);
    // SELECT * FROM member WHERE age > ?

    List<Member> findByNameContaining(String keyword);
    // SELECT * FROM member WHERE name LIKE '%keyword%'

    List<Member> findByNameStartingWith(String prefix);
    // SELECT * FROM member WHERE name LIKE 'prefix%'

    // 정렬
    List<Member> findByAgeOrderByNameAsc(int age);
    // SELECT * FROM member WHERE age = ? ORDER BY name ASC

    // 상위 N개
    List<Member> findTop3ByOrderByCreatedAtDesc();
    // SELECT * FROM member ORDER BY created_at DESC LIMIT 3
}
```

---

### 페이징 처리

```java
// Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Page<Order> findByMemberId(Long memberId, Pageable pageable);
}

// Service
@Transactional(readOnly = true)
public Page<OrderDto> getOrders(Long memberId, int page, int size) {
    Pageable pageable = PageRequest.of(
        page,            // 페이지 번호 (0부터 시작)
        size,            // 페이지 크기
        Sort.by(Sort.Direction.DESC, "createdAt")  // 정렬
    );
    Page<Order> orders = orderRepository.findByMemberId(memberId, pageable);
    return orders.map(OrderDto::from);  // DTO 변환
}

// Page 객체가 제공하는 정보
page.getContent()        // 현재 페이지 데이터
page.getTotalElements()  // 전체 데이터 수
page.getTotalPages()     // 전체 페이지 수
page.getNumber()         // 현재 페이지 번호
page.isFirst()           // 첫 번째 페이지?
page.isLast()            // 마지막 페이지?
```

---

### DTO 직접 조회 (성능 최적화)

```java
// DTO 클래스
@Getter
public class OrderSummaryDto {
    private Long orderId;
    private String memberName;
    private int totalPrice;

    // JPQL new 문법용 생성자
    public OrderSummaryDto(Long orderId, String memberName, int totalPrice) {
        this.orderId = orderId;
        this.memberName = memberName;
        this.totalPrice = totalPrice;
    }
}

// Repository
@Query("SELECT new com.example.dto.OrderSummaryDto(o.id, m.name, o.totalPrice) " +
       "FROM Order o JOIN o.member m " +
       "WHERE o.status = :status")
List<OrderSummaryDto> findOrderSummaries(@Param("status") OrderStatus status);
// 엔티티 전체 대신 필요한 필드만 조회 → 성능 향상
```

---

## 10. QueryDSL 완전 이해

### QueryDSL이 뭔가요?

```
JPQL: "SELECT o FROM Order o WHERE o.status = 'ORDER'"
      → 문자열이라 오타 있어도 컴파일 시 모름. 런타임에 에러

QueryDSL: queryFactory.selectFrom(order).where(order.status.eq(OrderStatus.ORDER))
         → 자바 코드라 오타 있으면 컴파일 에러로 바로 확인
         → IDE 자동완성 지원
         → 동적 쿼리 작성이 훨씬 쉬움
```

### Q타입이 뭔가요?

```java
// QueryDSL은 엔티티를 기반으로 Q클래스를 자동 생성함
// Member 엔티티 → QMember 자동 생성
// Order 엔티티 → QOrder 자동 생성

// 사용할 때
QMember member = QMember.member;   // 기본 인스턴스 사용
QOrder order = QOrder.order;

// 또는 static import로 더 간단하게
import static com.example.entity.QMember.*;
import static com.example.entity.QOrder.*;
// member, order 바로 사용 가능
```

### 기본 사용법 상세

```java
@Repository
@RequiredArgsConstructor
public class MemberQueryRepository {

    private final JPAQueryFactory queryFactory;
    // JPAQueryFactory = QueryDSL 쿼리를 만드는 공장
    // EntityManager를 받아서 동작

    // 단건 조회
    public Member findById(Long id) {
        return queryFactory
            .selectFrom(member)           // SELECT * FROM member
            .where(member.id.eq(id))      // WHERE id = ?
            .fetchOne();                  // 단건 반환 (없으면 null, 2개 이상이면 예외)
    }

    // 목록 조회
    public List<Member> findByName(String name) {
        return queryFactory
            .selectFrom(member)
            .where(member.name.eq(name))
            .orderBy(member.createdAt.desc())  // ORDER BY created_at DESC
            .fetch();                           // 리스트 반환
    }

    // 카운트
    public long countByAge(int age) {
        return queryFactory
            .select(member.count())
            .from(member)
            .where(member.age.gt(age))    // age > ?
            .fetchOne();
    }
}
```

### 조건 연산자 정리

```java
// 동등
member.name.eq("윤효근")       // name = '윤효근'
member.name.ne("윤효근")       // name != '윤효근'

// 비교
member.age.gt(20)              // age > 20
member.age.goe(20)             // age >= 20
member.age.lt(20)              // age < 20
member.age.loe(20)             // age <= 20
member.age.between(10, 30)     // age BETWEEN 10 AND 30

// 문자열
member.name.contains("효근")   // name LIKE '%효근%'
member.name.startsWith("윤")   // name LIKE '윤%'
member.name.endsWith("근")     // name LIKE '%근'

// NULL
member.email.isNull()          // email IS NULL
member.email.isNotNull()       // email IS NOT NULL

// IN
member.id.in(1L, 2L, 3L)      // id IN (1, 2, 3)
member.id.notIn(1L, 2L)        // id NOT IN (1, 2)
```

### 동적 쿼리 (핵심)

```java
// 검색 조건 DTO
@Getter
public class MemberSearchCondition {
    private String name;
    private String email;
    private Integer minAge;
    private Integer maxAge;
}

// 동적 쿼리
public List<Member> search(MemberSearchCondition condition) {
    return queryFactory
        .selectFrom(member)
        .where(
            nameEq(condition.getName()),        // null이면 이 조건 무시
            emailEq(condition.getEmail()),
            ageGoe(condition.getMinAge()),
            ageLoe(condition.getMaxAge())
        )
        .fetch();
}

// null 반환 = 조건에서 제외
private BooleanExpression nameEq(String name) {
    return StringUtils.hasText(name) ? member.name.eq(name) : null;
}

private BooleanExpression emailEq(String email) {
    return StringUtils.hasText(email) ? member.email.eq(email) : null;
}

private BooleanExpression ageGoe(Integer minAge) {
    return minAge != null ? member.age.goe(minAge) : null;
}

private BooleanExpression ageLoe(Integer maxAge) {
    return maxAge != null ? member.age.loe(maxAge) : null;
}

// 조건 조합도 가능
private BooleanExpression ageBetween(Integer minAge, Integer maxAge) {
    if (minAge != null && maxAge != null) return member.age.between(minAge, maxAge);
    if (minAge != null) return ageGoe(minAge);
    if (maxAge != null) return ageLoe(maxAge);
    return null;
}
```

---

### 이커머스 실전 예시 (올리브영 스타일)

```java
// 상품 검색 - 카테고리, 가격범위, 키워드, 정렬, 페이징
@Getter
public class ProductSearchCondition {
    private Long categoryId;
    private Integer minPrice;
    private Integer maxPrice;
    private String keyword;
    private String sortType;  // "price_asc", "price_desc", "newest"
}

public Page<ProductDto> searchProducts(ProductSearchCondition cond, Pageable pageable) {

    List<ProductDto> content = queryFactory
        .select(new QProductDto(
            product.id,
            product.name,
            product.price,
            product.thumbnailUrl,
            product.stock
        ))
        .from(product)
        .leftJoin(product.category, category)
        .where(
            categoryEq(cond.getCategoryId()),
            priceBetween(cond.getMinPrice(), cond.getMaxPrice()),
            keywordContains(cond.getKeyword()),
            product.stock.gt(0),          // 재고 있는 상품만
            product.isActive.isTrue()      // 판매 중인 상품만
        )
        .orderBy(getOrderSpecifier(cond.getSortType()))
        .offset(pageable.getOffset())
        .limit(pageable.getPageSize())
        .fetch();

    // count 쿼리는 조건만 (정렬/조인 제거하여 성능 최적화)
    JPAQuery<Long> countQuery = queryFactory
        .select(product.count())
        .from(product)
        .where(
            categoryEq(cond.getCategoryId()),
            priceBetween(cond.getMinPrice(), cond.getMaxPrice()),
            keywordContains(cond.getKeyword()),
            product.stock.gt(0),
            product.isActive.isTrue()
        );

    // 마지막 페이지면 count 쿼리 생략 (최적화)
    return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
}

private OrderSpecifier<?> getOrderSpecifier(String sortType) {
    if ("price_asc".equals(sortType))  return product.price.asc();
    if ("price_desc".equals(sortType)) return product.price.desc();
    return product.createdAt.desc();   // 기본: 최신순
}

private BooleanExpression categoryEq(Long categoryId) {
    return categoryId != null ? product.category.id.eq(categoryId) : null;
}

private BooleanExpression priceBetween(Integer min, Integer max) {
    if (min != null && max != null) return product.price.between(min, max);
    if (min != null) return product.price.goe(min);
    if (max != null) return product.price.loe(max);
    return null;
}

private BooleanExpression keywordContains(String keyword) {
    return StringUtils.hasText(keyword)
        ? product.name.contains(keyword).or(product.description.contains(keyword))
        : null;
}
```

---

## 11. 추가로 알아야 할 것들

### JPQL vs SQL 차이

```sql
-- SQL: 테이블/컬럼 기준
SELECT m.member_name, o.order_date
FROM member m
JOIN orders o ON m.id = o.member_id
WHERE m.member_name = '윤효근'

-- JPQL: 엔티티/필드 기준
SELECT m.name, o.orderDate
FROM Member m
JOIN m.orders o
WHERE m.name = '윤효근'
```

> 💡 JPQL은 테이블 이름이 아니라 **엔티티 클래스명**, 컬럼명이 아니라 **필드명** 사용

---

### 낙관적 락 vs 비관적 락

```java
// 낙관적 락 (Optimistic Lock)
// 충돌이 적을 것이라 가정. 수정 시 버전 확인
@Entity
public class Product {
    @Version          // 버전 필드 추가
    private Integer version;
    // 동시에 2명이 같은 상품 수정 시도 시
    // 먼저 수정한 사람의 version이 올라감
    // 나중 사람은 version 불일치로 OptimisticLockException 발생
}

// 비관적 락 (Pessimistic Lock)
// 충돌이 많을 것이라 가정. 조회 시 바로 DB 락 걸기
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT p FROM Product p WHERE p.id = :id")
Optional<Product> findByIdWithLock(@Param("id") Long id);
// SELECT * FROM product WHERE id = ? FOR UPDATE;
// 다른 트랜잭션은 이 row를 수정 못함
```

---

### 엔티티 설계 팁

```java
// 좋은 엔티티 설계
@Entity
@Getter                    // Lombok - getter 자동 생성
@NoArgsConstructor(access = AccessLevel.PROTECTED)
// ↑ JPA 스펙상 기본 생성자 필요하지만 외부에서 직접 new 못하게 막음
public class Order {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    // 정적 팩토리 메서드 패턴
    public static Order createOrder(Member member, List<OrderItem> items) {
        Order order = new Order();
        order.member = member;
        order.status = OrderStatus.ORDER;
        items.forEach(order::addOrderItem);
        return order;
    }
    // Order order = new Order() 대신
    // Order order = Order.createOrder(member, items) 사용
    // → 생성 시 필요한 값이 명확해짐
}
```

---

## 12. 면접 빈출 질문 모음

### JPA 핵심 질문

**Q1. 영속성 컨텍스트란 무엇이고 어떤 이점이 있나요?**
> JPA가 엔티티를 관리하는 임시 저장소입니다. 트랜잭션 범위 안에서 동작하며
> 1차 캐시로 반복 조회 시 SQL을 줄이고, 쓰기 지연으로 여러 SQL을 한 번에 전송해 네트워크 비용을 줄입니다.
> 변경 감지로 개발자가 update 메서드 없이도 엔티티 수정이 DB에 자동 반영됩니다.

**Q2. N+1 문제가 무엇이고 어떻게 해결하나요?**
> 1번의 쿼리를 실행했을 때 연관 엔티티 로딩으로 N번의 추가 쿼리가 발생하는 문제입니다.
> ManyToOne 관계에서는 fetch join을 주로 사용하고, OneToMany 컬렉션에서는 페이징이 필요하면 @BatchSize,
> 페이징이 없으면 fetch join + DISTINCT를 사용합니다.

**Q3. 지연 로딩과 즉시 로딩의 차이와 실무 권장은?**
> 즉시 로딩은 조회 시 연관 엔티티를 항상 함께 조회하고, 지연 로딩은 실제 사용 시점에 조회합니다.
> 실무에서는 모든 연관관계를 LAZY로 설정합니다. EAGER는 예측 불가능한 쿼리와 N+1 문제의 원인이 되기 때문입니다.

**Q4. @Transactional의 동작 원리와 주의사항은?**
> 스프링 AOP 프록시로 동작합니다. 메서드 호출 전 트랜잭션 시작, 정상 종료 시 commit,
> RuntimeException 발생 시 rollback합니다. 같은 클래스 내 self-invocation 시 프록시를 거치지 않아
> 트랜잭션이 무시되는 문제가 있어 이 경우 클래스를 분리해야 합니다.

**Q5. 연관관계 주인이란?**
> FK를 가진 엔티티가 연관관계 주인입니다. 주인만 DB에 쓰기가 가능하며,
> mappedBy가 붙은 반대쪽은 읽기 전용입니다. 실무에서는 양방향 일관성을 위해
> 편의 메서드를 만들어 양쪽 모두 설정합니다.

**Q6. QueryDSL을 사용하는 이유는?**
> JPQL은 문자열이라 컴파일 타임에 오류를 잡을 수 없고 동적 쿼리 작성이 복잡합니다.
> QueryDSL은 자바 코드로 작성해 컴파일 에러 감지, IDE 자동완성이 가능하고
> BooleanExpression으로 동적 쿼리를 깔끔하게 구성할 수 있습니다.

**Q7. cascade와 orphanRemoval의 차이는?**
> cascade는 부모 엔티티 저장/삭제 시 자식도 연쇄 적용되고,
> orphanRemoval은 부모와의 관계가 끊긴(컬렉션에서 제거된) 자식을 자동으로 DB에서 삭제합니다.
> 둘 다 완전히 종속적인 관계에서만 사용해야 합니다.

**Q8. JPQL과 SQL의 차이는?**
> SQL은 테이블과 컬럼 기준으로 작성하지만, JPQL은 엔티티 클래스와 필드 기준으로 작성합니다.
> JPQL은 DB 종류에 독립적이며 JPA가 실행 시 각 DB에 맞는 SQL로 변환합니다.

---

> 각 섹션 이해 후 기술면접_공부플랜.md 체크리스트 표시할 것
> 모르는 내용 나오면 바로 질문!
