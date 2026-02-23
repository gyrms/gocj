# JPA 심화학습 - 기술면접 완벽 대비

> CJ올리브영 면접 기준 작성
> 각 섹션 학습 후 체크박스 표시

---

## 목차
1. [영속성 컨텍스트](#1-영속성-컨텍스트)
2. [N+1 문제](#2-n1-문제)
3. [지연 로딩 vs 즉시 로딩](#3-지연-로딩-vs-즉시-로딩)
4. [연관관계 매핑](#4-연관관계-매핑)
5. [트랜잭션](#5-트랜잭션)
6. [QueryDSL](#6-querydsl)

---

## 1. 영속성 컨텍스트

### 개념

영속성 컨텍스트(Persistence Context)는 **엔티티를 영구 저장하는 환경**이다.
쉽게 말하면 JPA가 관리하는 **엔티티 보관 창고**다.

`EntityManager`가 영속성 컨텍스트에 접근할 수 있는 창구 역할을 하며,
스프링에서는 트랜잭션 범위 안에서 영속성 컨텍스트가 생성되고 종료된다.

### 엔티티의 4가지 상태

```
비영속 (new)     → persist() →  영속 (managed)
영속 (managed)   → detach() →  준영속 (detached)
영속 (managed)   → remove() →  삭제 (removed)
```

```java
// 비영속 상태 - JPA가 모르는 상태
Member member = new Member();
member.setName("윤효근");

// 영속 상태 - 영속성 컨텍스트가 관리 시작
em.persist(member);

// 준영속 상태 - 영속성 컨텍스트에서 분리
em.detach(member);

// 삭제 상태
em.remove(member);
```

### 핵심 기능 3가지

#### 1) 1차 캐시

```java
// 첫 번째 조회 - DB에서 가져와서 1차 캐시에 저장
Member member1 = em.find(Member.class, 1L);  // SQL 실행 O

// 두 번째 조회 - 1차 캐시에서 바로 반환 (SQL 실행 안 함)
Member member2 = em.find(Member.class, 1L);  // SQL 실행 X

System.out.println(member1 == member2); // true (동일 인스턴스)
```

> 💡 1차 캐시는 트랜잭션 범위 안에서만 유효하다. 트랜잭션이 끝나면 사라진다.

#### 2) 쓰기 지연 (Write-Behind)

```java
transaction.begin();

em.persist(new Member("회원1")); // INSERT SQL을 DB에 보내지 않고 쓰기 지연 SQL 저장소에 모아둠
em.persist(new Member("회원2")); // 마찬가지

// 커밋하는 순간 한 번에 DB에 INSERT SQL 전송
transaction.commit();
```

> 💡 쓰기 지연 덕분에 네트워크 통신 횟수를 줄여 성능 최적화가 가능하다.

#### 3) 변경 감지 (Dirty Checking)

```java
transaction.begin();

Member member = em.find(Member.class, 1L); // 조회 시점 스냅샷 저장
member.setName("변경된 이름"); // 엔티티 수정

// em.update() 같은 메서드 호출 필요 없음!
// 커밋 시점에 스냅샷과 비교해서 변경된 필드 자동으로 UPDATE SQL 생성
transaction.commit();
```

> 💡 JPA는 엔티티를 조회할 때 원본 스냅샷을 저장해두고, 커밋 시점에 비교한다.
> 변경된 필드가 있으면 자동으로 UPDATE SQL을 만들어 실행한다.

### 면접 예상 질문

**Q. 영속성 컨텍스트의 장점이 무엇인가요?**
> A. 1차 캐시로 동일 트랜잭션 내 반복 조회 시 SQL 없이 반환되어 성능이 향상됩니다.
> 쓰기 지연으로 여러 SQL을 모아서 한 번에 전송해 네트워크 비용을 줄입니다.
> 변경 감지로 개발자가 별도의 update 메서드 없이도 엔티티 수정이 가능합니다.

---

## 2. N+1 문제

### 개념

1번의 쿼리를 실행했더니 **연관된 데이터 때문에 N번의 추가 쿼리가 실행**되는 문제다.

### 문제 재현

```java
// 엔티티
@Entity
public class Order {
    @Id @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER) // 즉시 로딩
    private Member member;
}

// 서비스
List<Order> orders = orderRepository.findAll(); // ORDER 전체 조회 쿼리 1번
// EAGER 설정이면 각 Order마다 Member 조회 쿼리가 추가로 실행됨
// Order가 100개면 쿼리 101번 실행 → N+1 문제
```

실행되는 SQL:
```sql
SELECT * FROM orders;                         -- 1번
SELECT * FROM member WHERE id = 1;            -- Order 1번의 Member
SELECT * FROM member WHERE id = 2;            -- Order 2번의 Member
SELECT * FROM member WHERE id = 3;            -- Order 3번의 Member
-- ... N번 반복
```

### 해결 방법 3가지

#### 방법 1: fetch join (가장 많이 씀)

```java
// Repository
@Query("SELECT o FROM Order o JOIN FETCH o.member")
List<Order> findAllWithMember();
```

실행되는 SQL:
```sql
-- 쿼리 1번으로 끝
SELECT o.*, m.* FROM orders o
INNER JOIN member m ON o.member_id = m.id;
```

**주의사항**: 컬렉션(OneToMany) fetch join 시 데이터 뻥튀기 발생 → `DISTINCT` 사용

```java
@Query("SELECT DISTINCT o FROM Order o JOIN FETCH o.orderItems")
List<Order> findAllWithItems();
```

#### 방법 2: @EntityGraph

```java
// fetch join을 어노테이션으로 표현
@EntityGraph(attributePaths = {"member"})
@Query("SELECT o FROM Order o")
List<Order> findAllWithMember();
```

> 💡 fetch join과 결과는 같지만 JPQL을 직접 작성하지 않아도 돼서 간단하다.

#### 방법 3: @BatchSize

```java
// 엔티티에 설정
@Entity
public class Order {
    @BatchSize(size = 100) // 연관 엔티티를 100개씩 IN절로 조회
    @OneToMany(mappedBy = "order")
    private List<OrderItem> orderItems;
}

// 또는 application.yml에 글로벌 설정
spring:
  jpa:
    properties:
      hibernate:
        default_batch_fetch_size: 100
```

실행되는 SQL:
```sql
SELECT * FROM orders;                           -- 1번
SELECT * FROM order_item WHERE order_id IN (1, 2, 3, ... 100); -- IN절로 한 번에
```

### 세 가지 방법 비교

| 방법 | 장점 | 단점 | 사용 시기 |
|------|------|------|---------|
| fetch join | 쿼리 1번으로 해결 | JPQL 직접 작성 필요, 컬렉션 중복 주의 | 단순 연관관계 조회 |
| @EntityGraph | JPQL 없이 간편 | 복잡한 조건에서 한계 | 간단한 fetch join |
| @BatchSize | 컬렉션에 효과적 | 쿼리가 2번 실행됨 | OneToMany 컬렉션 |

### 면접 예상 질문

**Q. JPA N+1 문제가 무엇이고 어떻게 해결하나요?**
> A. 1건의 쿼리를 실행했을 때 연관된 엔티티 로딩으로 N번의 추가 쿼리가 실행되는 문제입니다.
> 해결 방법으로는 fetch join, @EntityGraph, @BatchSize가 있습니다.
> 단건 조회나 ManyToOne 관계에서는 fetch join을 주로 사용하고,
> OneToMany 컬렉션 조회에서는 @BatchSize를 통해 IN절로 한 번에 처리합니다.
> YPT 프로젝트에서 예약 목록 조회 시 N+1이 발생하여 fetch join으로 해결한 경험이 있습니다.

---

## 3. 지연 로딩 vs 즉시 로딩

### 개념

```java
@Entity
public class Order {

    // 즉시 로딩 (EAGER) - Order 조회 시 Member도 즉시 함께 조회
    @ManyToOne(fetch = FetchType.EAGER)
    private Member member;

    // 지연 로딩 (LAZY) - Order 조회 시 Member는 프록시 객체로 대체
    @OneToMany(fetch = FetchType.LAZY)
    private List<OrderItem> orderItems;
}
```

### JPA 기본값

| 연관관계 | 기본 로딩 전략 |
|---------|-------------|
| @ManyToOne | EAGER (즉시 로딩) |
| @OneToOne | EAGER (즉시 로딩) |
| @OneToMany | LAZY (지연 로딩) |
| @ManyToMany | LAZY (지연 로딩) |

> ⚠️ 실무에서는 **모든 연관관계를 LAZY로 설정**하는 것이 기본 원칙이다.

### 지연 로딩 동작 원리

```java
// LAZY 설정 시
Order order = em.find(Order.class, 1L);
// 이 시점에 member는 프록시 객체 (빈 껍데기)

String memberName = order.getMember().getName();
// getName() 호출 시 그때서야 DB에서 Member 조회 (프록시 초기화)
```

### 즉시 로딩의 문제점

```java
// EAGER 설정 시 예측 불가능한 쿼리 발생
List<Order> orders = orderRepository.findAll();
// 단순한 주문 목록 조회인데 Member까지 모두 JOIN해서 가져옴
// 연관관계가 많을수록 예상치 못한 쿼리가 대량 발생
```

### 면접 예상 질문

**Q. 지연 로딩과 즉시 로딩의 차이와 실무 권장 사항은?**
> A. 즉시 로딩(EAGER)은 엔티티 조회 시 연관 엔티티를 항상 함께 조회하고,
> 지연 로딩(LAZY)은 실제로 연관 엔티티를 사용하는 시점에 조회합니다.
> 실무에서는 모든 연관관계를 LAZY로 설정하는 것을 권장합니다.
> EAGER 사용 시 예측 불가능한 쿼리가 발생하고 N+1 문제가 생길 수 있기 때문입니다.
> 필요한 경우 fetch join으로 필요한 시점에만 함께 조회합니다.

---

## 4. 연관관계 매핑

### 기본 매핑

```java
// 회원 : 주문 = 1 : N
@Entity
public class Member {
    @Id @GeneratedValue
    private Long id;
    private String name;

    @OneToMany(mappedBy = "member") // 주인이 아닌 쪽 (읽기 전용)
    private List<Order> orders = new ArrayList<>();
}

@Entity
public class Order {
    @Id @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id") // FK를 가진 쪽이 연관관계 주인
    private Member member;
}
```

### 연관관계 주인

- FK(외래키)를 가진 쪽이 **연관관계 주인**
- 주인만 DB에 등록/수정/삭제 가능
- `mappedBy`가 붙은 쪽은 읽기 전용

```java
// 올바른 연관관계 설정
Order order = new Order();
Member member = em.find(Member.class, 1L);

order.setMember(member); // 주인(Order)에 설정 → DB에 반영됨
member.getOrders().add(order); // 객체 그래프 일관성을 위해 양쪽 모두 설정 권장

em.persist(order);
```

### 편의 메서드 패턴

```java
@Entity
public class Order {
    @ManyToOne(fetch = FetchType.LAZY)
    private Member member;

    // 연관관계 편의 메서드 - 양방향 동시에 설정
    public void assignMember(Member member) {
        this.member = member;
        member.getOrders().add(this);
    }
}
```

### cascade 옵션

```java
@Entity
public class Order {
    // cascade = PERSIST: Order 저장 시 OrderItem도 자동 저장
    // cascade = REMOVE: Order 삭제 시 OrderItem도 자동 삭제
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();
}
```

> ⚠️ cascade는 완전히 종속적인 관계에서만 사용 (OrderItem은 Order 없이 존재 불가 → OK)
> 다른 엔티티에서도 참조하는 경우엔 cascade 사용 금지

### 면접 예상 질문

**Q. 연관관계 주인이란 무엇인가요?**
> A. JPA에서 양방향 연관관계 설정 시 외래키(FK)를 관리하는 쪽을 연관관계 주인이라고 합니다.
> 외래키를 가진 테이블의 엔티티가 주인이 되며, 주인만 DB에 데이터를 등록·수정할 수 있습니다.
> mappedBy가 붙은 반대쪽은 읽기 전용입니다.
> 실무에서는 양방향 일관성을 위해 편의 메서드를 만들어 양쪽 모두 세팅하는 것을 권장합니다.

---

## 5. 트랜잭션

### @Transactional 동작 원리

```java
@Service
public class OrderService {

    @Transactional // 스프링 AOP 프록시가 트랜잭션 시작/종료를 감싼다
    public void order(Long memberId, Long itemId) {
        // 1. 트랜잭션 시작 (프록시가 자동 처리)
        Member member = memberRepository.findById(memberId).orElseThrow();
        Item item = itemRepository.findById(itemId).orElseThrow();
        Order order = Order.createOrder(member, item);
        orderRepository.save(order);
        // 2. 정상 종료 시 commit (프록시가 자동 처리)
        // 3. 예외 발생 시 rollback (프록시가 자동 처리)
    }
}
```

### 전파 옵션 (Propagation)

| 옵션 | 동작 |
|------|------|
| REQUIRED (기본값) | 트랜잭션 있으면 참여, 없으면 새로 생성 |
| REQUIRES_NEW | 항상 새 트랜잭션 생성 (기존 트랜잭션 일시 정지) |
| NESTED | 중첩 트랜잭션 생성 |

```java
@Service
public class PaymentService {

    @Transactional
    public void pay(Long orderId) {
        orderService.updateStatus(orderId);   // REQUIRED: 같은 트랜잭션 참여
        logService.saveLog(orderId);          // REQUIRES_NEW: 로그는 별도 트랜잭션
        // 결제 실패로 롤백돼도 로그는 남아있음
    }
}

@Service
public class LogService {
    @Transactional(propagation = Propagation.REQUIRES_NEW) // 새 트랜잭션
    public void saveLog(Long orderId) {
        // 별도 트랜잭션으로 실행 → 결제 롤백과 무관하게 로그 저장됨
    }
}
```

### readOnly = true

```java
@Transactional(readOnly = true) // 조회 전용 트랜잭션
public List<Order> findOrders(Long memberId) {
    return orderRepository.findByMemberId(memberId);
}
```

**readOnly 이점:**
- 변경 감지(Dirty Checking) 수행 안 함 → 성능 향상
- DB 복제 환경에서 읽기 전용 DB로 라우팅 가능
- 의도를 코드로 명확히 표현

### 자기 호출 (Self-Invocation) 문제

```java
@Service
public class OrderService {

    @Transactional
    public void methodA() {
        this.methodB(); // ⚠️ 프록시를 거치지 않아 @Transactional 무시됨
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void methodB() {
        // methodA에서 this로 호출 시 트랜잭션 적용 안 됨
    }
}
```

**해결 방법**: 별도 클래스로 분리하거나 ApplicationContext에서 빈을 다시 가져옴

```java
// 해결 - 클래스 분리
@Service
public class OrderService {
    private final OrderInternalService orderInternalService;

    @Transactional
    public void methodA() {
        orderInternalService.methodB(); // 프록시를 통해 호출 → 정상 동작
    }
}
```

### 면접 예상 질문

**Q. @Transactional의 동작 원리와 사용 시 주의사항은?**
> A. 스프링 AOP 프록시를 통해 동작합니다. 메서드 호출 전 트랜잭션을 시작하고,
> 정상 종료 시 commit, 런타임 예외 발생 시 rollback을 자동으로 처리합니다.
> 주의사항으로는 같은 클래스 내에서 self-invocation 시 프록시를 거치지 않아
> @Transactional이 무시되는 문제가 있습니다. 이 경우 클래스를 분리해야 합니다.
> 또한 조회 전용 메서드에는 readOnly = true를 설정하면 변경 감지를 skip해 성능이 향상됩니다.

---

## 6. QueryDSL

### 왜 쓰는가

```java
// JPQL - 문자열이라 컴파일 타임에 오류를 잡을 수 없음
@Query("SELECT m FROM Member m WHERE m.name = :name AND m.age > :age")
List<Member> findByNameAndAge(String name, int age);

// QueryDSL - 자바 코드라 컴파일 타임에 오류를 잡을 수 있고 동적 쿼리 작성이 쉬움
```

### 기본 사용법

```java
@Repository
public class MemberQueryRepository {

    private final JPAQueryFactory queryFactory;

    // 기본 조회
    public List<Member> findByName(String name) {
        return queryFactory
            .selectFrom(member)
            .where(member.name.eq(name))
            .fetch();
    }

    // 페이징
    public Page<Member> findMembers(Pageable pageable) {
        List<Member> content = queryFactory
            .selectFrom(member)
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

        long total = queryFactory
            .select(member.count())
            .from(member)
            .fetchOne();

        return new PageImpl<>(content, pageable, total);
    }

    // JOIN
    public List<Order> findOrdersWithMember() {
        return queryFactory
            .selectFrom(order)
            .join(order.member, member).fetchJoin()
            .fetch();
    }
}
```

### 동적 쿼리 (핵심)

```java
// 검색 조건 객체
public class OrderSearchCondition {
    private String memberName;
    private OrderStatus status;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}

// BooleanExpression으로 동적 쿼리 작성
@Repository
public class OrderQueryRepository {

    public List<Order> searchOrders(OrderSearchCondition condition) {
        return queryFactory
            .selectFrom(order)
            .join(order.member, member).fetchJoin()
            .where(
                memberNameEq(condition.getMemberName()),  // null이면 조건에서 제외
                statusEq(condition.getStatus()),
                dateGoe(condition.getStartDate()),
                dateLoe(condition.getEndDate())
            )
            .fetch();
    }

    // null 반환 시 해당 조건 무시됨
    private BooleanExpression memberNameEq(String memberName) {
        return StringUtils.hasText(memberName) ? member.name.eq(memberName) : null;
    }

    private BooleanExpression statusEq(OrderStatus status) {
        return status != null ? order.status.eq(status) : null;
    }

    private BooleanExpression dateGoe(LocalDateTime startDate) {
        return startDate != null ? order.createdAt.goe(startDate) : null;
    }

    private BooleanExpression dateLoe(LocalDateTime endDate) {
        return endDate != null ? order.createdAt.loe(endDate) : null;
    }
}
```

### 이커머스 실전 예시 (올리브영 스타일)

```java
// 상품 목록 검색 (카테고리, 가격 범위, 키워드 동적 검색)
public Page<ProductDto> searchProducts(ProductSearchCondition condition, Pageable pageable) {
    List<ProductDto> content = queryFactory
        .select(new QProductDto(
            product.id,
            product.name,
            product.price,
            product.category,
            product.stock
        ))
        .from(product)
        .where(
            categoryEq(condition.getCategoryId()),
            priceBetween(condition.getMinPrice(), condition.getMaxPrice()),
            keywordContains(condition.getKeyword()),
            product.stock.gt(0) // 재고 있는 상품만
        )
        .orderBy(priceSort(condition.getSortType()))
        .offset(pageable.getOffset())
        .limit(pageable.getPageSize())
        .fetch();

    long total = queryFactory
        .select(product.count())
        .from(product)
        .where(
            categoryEq(condition.getCategoryId()),
            priceBetween(condition.getMinPrice(), condition.getMaxPrice()),
            keywordContains(condition.getKeyword())
        )
        .fetchOne();

    return new PageImpl<>(content, pageable, total);
}

private BooleanExpression categoryEq(Long categoryId) {
    return categoryId != null ? product.category.id.eq(categoryId) : null;
}

private BooleanExpression priceBetween(Integer minPrice, Integer maxPrice) {
    if (minPrice != null && maxPrice != null) {
        return product.price.between(minPrice, maxPrice);
    }
    if (minPrice != null) return product.price.goe(minPrice);
    if (maxPrice != null) return product.price.loe(maxPrice);
    return null;
}

private BooleanExpression keywordContains(String keyword) {
    return StringUtils.hasText(keyword) ? product.name.contains(keyword) : null;
}
```

### 면접 예상 질문

**Q. QueryDSL을 사용하는 이유는 무엇인가요?**
> A. JPQL은 문자열로 작성하기 때문에 컴파일 타임에 오류를 잡을 수 없고,
> 동적 쿼리 작성이 복잡하다는 단점이 있습니다.
> QueryDSL은 자바 코드로 쿼리를 작성하기 때문에 IDE 자동완성과 컴파일 타임 오류 감지가 가능하고,
> BooleanExpression을 활용해 동적 쿼리를 깔끔하게 작성할 수 있습니다.
> 특히 이커머스에서 다양한 필터 조건의 상품 검색 같은 복잡한 동적 쿼리에 매우 유용합니다.

---

## 전체 면접 빈출 질문 모음

```
JPA
1. 영속성 컨텍스트란 무엇이고 어떤 이점이 있나요?
2. N+1 문제가 무엇이고 어떻게 해결하나요?
3. 지연 로딩과 즉시 로딩의 차이와 실무 권장 방식은?
4. @Transactional의 동작 원리와 주의사항은?
5. QueryDSL을 사용하는 이유는?
6. JPA 연관관계 주인이란 무엇인가요?
7. cascade 옵션은 언제 사용하나요?
8. readOnly = true의 이점은 무엇인가요?
```

---

> 각 섹션 학습 완료 시 기술면접_공부플랜.md 체크리스트에 표시할 것
