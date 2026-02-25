# Spring 심화 학습 (3순위)

> CJ올리브영 기술면접 대비

---

## 1. IoC / DI

### IoC (제어의 역전, Inversion of Control)

일반적으로 개발자가 객체를 직접 생성하고 관리합니다.
IoC는 이 **제어권을 개발자가 아닌 스프링 컨테이너에게 넘기는 것**입니다.

**IoC 이전 (직접 제어)**
```java
public class OrderService {
    // 개발자가 직접 객체 생성
    private MemberRepository memberRepository = new MemberRepository();
    private OrderRepository orderRepository = new OrderRepository();
}
```

**IoC 이후 (스프링이 제어)**
```java
@Service
public class OrderService {
    // 스프링이 알아서 넣어줌
    private final MemberRepository memberRepository;
    private final OrderRepository orderRepository;
}
```

- 객체 생성, 관리, 소멸을 **스프링 컨테이너가 담당**
- 개발자는 비즈니스 로직에만 집중 가능

---

### DI (의존성 주입, Dependency Injection)

IoC를 구현하는 방법 중 하나입니다.
스프링이 필요한 객체를 **외부에서 주입**해주는 것입니다.

#### 방법 1 - 생성자 주입 (권장 ✅)
```java
@Service
public class OrderService {
    private final MemberRepository memberRepository;

    // 생성자로 주입
    public OrderService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }
}

// @RequiredArgsConstructor 사용하면 더 간단
@Service
@RequiredArgsConstructor
public class OrderService {
    private final MemberRepository memberRepository;
}
```

#### 방법 2 - 필드 주입 (지양 ❌)
```java
@Service
public class OrderService {
    @Autowired  // 테스트하기 어렵고 final 못씀
    private MemberRepository memberRepository;
}
```

#### 방법 3 - Setter 주입 (특수한 경우에만)
```java
@Service
public class OrderService {
    private MemberRepository memberRepository;

    @Autowired
    public void setMemberRepository(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }
}
```

#### 생성자 주입을 권장하는 이유
```
1. final 키워드 사용 가능 → 불변성 보장
2. 테스트 시 Mock 객체 주입 쉬움
3. 순환 참조 문제를 컴파일 시점에 잡아줌
4. 필수 의존성이 명확하게 드러남
```

---

### 빈(Bean) 생명주기

스프링이 관리하는 객체를 **빈(Bean)**이라고 합니다.

```
스프링 컨테이너 시작
    ↓
빈 생성 (객체 인스턴스화)
    ↓
의존성 주입
    ↓
초기화 콜백 (@PostConstruct)  ← 개발자가 초기화 로직 넣는 곳
    ↓
빈 사용 (서비스 운영)
    ↓
소멸 콜백 (@PreDestroy)       ← 개발자가 정리 로직 넣는 곳
    ↓
스프링 컨테이너 종료
```

```java
@Component
public class DatabaseConnector {

    @PostConstruct
    public void init() {
        // 빈 생성 후 DB 연결 초기화
        System.out.println("DB 연결 초기화");
    }

    @PreDestroy
    public void cleanup() {
        // 컨테이너 종료 전 DB 연결 해제
        System.out.println("DB 연결 해제");
    }
}
```

---

### @Component, @Service, @Repository 차이

세 가지 모두 스프링 빈으로 등록된다는 점은 **동일**합니다.
차이는 **역할을 명확히 표현**하는 것과 **부가 기능**입니다.

| 어노테이션 | 사용 계층 | 부가 기능 |
|-----------|---------|---------|
| @Component | 어디서든 | 없음 (범용) |
| @Service | 비즈니스 로직 | 없음 (의미 부여) |
| @Repository | DB 접근 계층 | DB 예외를 스프링 예외로 변환 |
| @Controller | 웹 요청 처리 | 웹 요청 매핑 |

```java
@Repository  // DB 예외 → DataAccessException 으로 변환해줌
public class MemberRepository {
    public Member findById(Long id) { ... }
}

@Service     // 비즈니스 로직임을 명시
public class MemberService {
    public Member getMember(Long id) { ... }
}
```

---

## 2. AOP (관점 지향 프로그래밍)

### AOP가 왜 필요한가

여러 곳에서 반복되는 **공통 기능**(로깅, 트랜잭션, 보안)을 분리하기 위해서입니다.

**AOP 없을 때**
```java
public void createOrder() {
    log.info("createOrder 시작");        // 반복되는 로깅
    트랜잭션 시작;                        // 반복되는 트랜잭션
    // 실제 비즈니스 로직
    트랜잭션 종료;
    log.info("createOrder 종료");        // 반복되는 로깅
}

public void cancelOrder() {
    log.info("cancelOrder 시작");        // 또 반복
    트랜잭션 시작;
    // 실제 비즈니스 로직
    트랜잭션 종료;
    log.info("cancelOrder 종료");        // 또 반복
}
```

**AOP 있을 때**
```java
// 공통 기능은 AOP가 알아서 처리
public void createOrder() {
    // 실제 비즈니스 로직만
}

public void cancelOrder() {
    // 실제 비즈니스 로직만
}
```

---

### 프록시 기반 동작 원리

AOP는 **프록시 패턴**으로 동작합니다.

```
실제 객체 앞에 프록시(대리인)를 세워서
공통 기능을 프록시가 처리하고
실제 로직은 원본 객체가 처리
```

```
클라이언트 → 프록시(로깅, 트랜잭션 처리) → 실제 객체(비즈니스 로직)
```

---

### @Transactional이 AOP로 동작하는 원리

```java
@Service
public class OrderService {

    @Transactional  // AOP가 이걸 감지
    public void createOrder() {
        // 비즈니스 로직
    }
}
```

스프링이 내부적으로 하는 일:
```java
// 스프링이 자동으로 만드는 프록시
class OrderServiceProxy extends OrderService {

    public void createOrder() {
        트랜잭션 시작;          // AOP가 추가한 공통 기능
        super.createOrder();   // 실제 비즈니스 로직
        트랜잭션 커밋/롤백;     // AOP가 추가한 공통 기능
    }
}
```

개발자는 `@Transactional`만 붙이면 트랜잭션 코드를 직접 작성하지 않아도 됩니다.

---

## 3. Spring Security

### 인증(Authentication) vs 인가(Authorization)

```
인증 (Authentication) = 너 누구야? (신원 확인)
인가 (Authorization)  = 너 이거 할 수 있어? (권한 확인)
```

```
예시:
인증 → 로그인 (아이디/비밀번호로 본인 확인)
인가 → 관리자 페이지 접근 (관리자 권한 있는지 확인)
```

---

### JWT 토큰 기반 인증 흐름

```
1. 클라이언트 → 로그인 요청 (아이디/비밀번호)
        ↓
2. 서버 → DB에서 회원 확인
        ↓
3. 서버 → JWT 토큰 생성해서 클라이언트에 반환
        ↓
4. 클라이언트 → 이후 요청마다 헤더에 JWT 토큰 포함
        ↓
5. 서버 → JWT 토큰 검증 (유효한지, 만료됐는지)
        ↓
6. 유효하면 → 요청 처리
   무효하면 → 401 Unauthorized 반환
```

**JWT 구조**
```
Header.Payload.Signature

Header    = 토큰 타입, 알고리즘
Payload   = 사용자 정보 (id, role, 만료시간)
Signature = 위변조 방지 서명
```

**JWT 장점**
```
서버가 토큰 정보를 저장하지 않아도 됨 (Stateless)
→ 여러 서버로 스케일 아웃해도 세션 공유 불필요
```

---

### OAuth2 소셜 로그인 흐름

```
1. 클라이언트 → "카카오로 로그인" 클릭
        ↓
2. 카카오 로그인 페이지로 리다이렉트
        ↓
3. 사용자 → 카카오에서 로그인
        ↓
4. 카카오 → 우리 서버에 Authorization Code 전달
        ↓
5. 우리 서버 → 카카오에 Code로 Access Token 요청
        ↓
6. 카카오 → Access Token 반환
        ↓
7. 우리 서버 → Access Token으로 사용자 정보 요청
        ↓
8. 카카오 → 사용자 정보 반환 (이메일, 닉네임 등)
        ↓
9. 우리 서버 → 회원가입 or 로그인 처리 → JWT 발급
```

---

### Security Filter Chain 동작 원리

Spring Security는 **Filter Chain** 구조로 동작합니다.
HTTP 요청이 들어오면 여러 필터를 순서대로 통과합니다.

```
HTTP 요청
    ↓
SecurityContextPersistenceFilter  (인증 정보 로드)
    ↓
UsernamePasswordAuthenticationFilter  (로그인 처리)
    ↓
JwtAuthenticationFilter  (JWT 토큰 검증 - 커스텀)
    ↓
ExceptionTranslationFilter  (인증/인가 예외 처리)
    ↓
FilterSecurityInterceptor  (권한 확인)
    ↓
실제 Controller 실행
```

각 필터는 **체인 형태**로 연결되어 있어서, 앞 필터를 통과해야 다음 필터로 넘어갑니다.
인증 실패 시 그 자리에서 요청이 차단됩니다.

---

## 4. 기타

### @SpringBootApplication 내부 동작

```java
@SpringBootApplication  // 이 하나에 3가지가 합쳐져 있음
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

```
@SpringBootApplication
    ├── @SpringBootConfiguration  → 설정 클래스임을 선언 (@Configuration과 동일)
    ├── @EnableAutoConfiguration  → 자동 설정 활성화
    └── @ComponentScan            → 현재 패키지 하위의 빈 자동 등록
```

---

### 자동 설정 (AutoConfiguration) 원리

스프링 부트가 **클래스패스에 있는 라이브러리를 감지**해서 자동으로 설정해주는 기능입니다.

```
예시) spring-data-jpa 라이브러리가 있으면
→ DataSource 자동 생성
→ EntityManager 자동 생성
→ 트랜잭션 매니저 자동 생성
개발자가 직접 설정 안 해도 됨
```

```
예시) spring-boot-starter-web 있으면
→ Tomcat 자동 설정
→ DispatcherServlet 자동 등록
→ Jackson(JSON 변환기) 자동 등록
```

동작 원리:
```
1. @EnableAutoConfiguration 선언
2. spring.factories 파일에서 자동 설정 목록 로드
3. @Conditional 조건에 따라 적용 여부 결정
   (라이브러리가 있으면 적용, 없으면 스킵)
```

---

## 면접 자주 나오는 질문

---

### Q1. IoC와 DI에 대해 설명해주세요.

IoC(제어의 역전)는 객체의 생성과 관리를 개발자가 아닌 스프링 컨테이너가 담당하는 개념입니다. 기존에는 개발자가 `new` 키워드로 직접 객체를 생성했지만, IoC에서는 스프링이 객체를 생성하고 생명주기를 관리합니다.

DI(의존성 주입)는 IoC를 구현하는 방법으로, 객체가 필요로 하는 의존 객체를 스프링이 외부에서 주입해주는 방식입니다.

주입 방법은 생성자 주입, 필드 주입, Setter 주입 3가지가 있으며, 생성자 주입을 권장합니다. 그 이유는 `final` 키워드로 불변성을 보장할 수 있고, 테스트 시 Mock 객체 주입이 쉬우며, 순환 참조 문제를 컴파일 시점에 잡아낼 수 있기 때문입니다.

---

### Q2. AOP가 무엇이고 왜 필요한가요?

AOP(관점 지향 프로그래밍)는 로깅, 트랜잭션, 보안처럼 여러 곳에서 반복되는 공통 기능을 비즈니스 로직에서 분리하여 관리하는 기법입니다.

예를 들어 모든 서비스 메서드에 트랜잭션 처리 코드를 직접 작성하면 중복 코드가 많아지고 유지보수가 어렵습니다. AOP를 사용하면 `@Transactional` 하나만 붙여도 스프링이 프록시를 통해 자동으로 트랜잭션을 처리해줍니다.

내부적으로는 프록시 패턴으로 동작합니다. 스프링이 실제 객체 앞에 프록시를 생성하고, 프록시가 공통 기능을 처리한 뒤 실제 메서드를 호출합니다.

---

### Q3. @Transactional 동작 원리와 주의사항을 설명해주세요.

`@Transactional`은 AOP 프록시 기반으로 동작합니다. 스프링이 `@Transactional`이 붙은 클래스의 프록시를 생성하고, 외부에서 메서드를 호출할 때 프록시가 트랜잭션 시작, 커밋, 롤백을 처리합니다.

주의사항은 두 가지입니다.

첫째, **Self-invocation(자기 호출) 문제**입니다. 같은 클래스 내에서 `this.메서드()`로 호출하면 프록시를 거치지 않아 `@Transactional`이 무시됩니다.

둘째, **접근 제어자** 문제입니다. `private` 메서드에 `@Transactional`을 붙여도 프록시가 오버라이딩할 수 없어서 동작하지 않습니다. `public` 메서드에만 사용해야 합니다.

---

### Q4. 인증과 인가의 차이를 설명해주세요.

인증(Authentication)은 사용자가 누구인지 확인하는 과정입니다. 로그인 시 아이디와 비밀번호로 신원을 확인하는 것이 인증입니다.

인가(Authorization)는 인증된 사용자가 특정 리소스에 접근할 권한이 있는지 확인하는 과정입니다. 관리자 페이지에 일반 회원이 접근하지 못하도록 막는 것이 인가입니다.

순서는 반드시 인증 → 인가입니다. 누구인지 모르는 상태에서 권한을 확인할 수 없기 때문입니다.

---

### Q5. JWT 기반 인증과 세션 기반 인증의 차이는 무엇인가요?

세션 기반 인증은 로그인 시 서버가 세션을 생성하고 저장합니다. 클라이언트는 세션 ID만 쿠키에 저장하고, 요청마다 서버가 세션 저장소에서 확인합니다. 서버에 상태가 저장되므로 Stateful합니다. 여러 서버로 스케일 아웃할 경우 세션을 공유(Redis 등)해야 합니다.

JWT 기반 인증은 로그인 시 서버가 JWT 토큰을 발급하고 클라이언트가 저장합니다. 이후 요청마다 토큰을 헤더에 담아 보내면 서버가 토큰 자체를 검증합니다. 서버에 상태를 저장하지 않으므로 Stateless합니다. 스케일 아웃 시 세션 공유가 필요 없어 MSA 환경에 적합합니다.

단, JWT는 토큰이 탈취되면 만료 전까지 막을 방법이 없다는 단점이 있습니다. 이를 위해 Access Token(짧은 만료)과 Refresh Token(긴 만료)을 함께 사용합니다.

---

### Q6. Spring Security Filter Chain에 대해 설명해주세요.

Spring Security는 HTTP 요청이 Controller에 도달하기 전에 여러 보안 필터를 순서대로 통과시키는 Filter Chain 구조로 동작합니다.

주요 필터로는 JWT 토큰을 검증하는 커스텀 필터, 인증/인가 예외를 처리하는 ExceptionTranslationFilter, 권한을 확인하는 FilterSecurityInterceptor 등이 있습니다.

각 필터는 체인으로 연결되어 있어서 앞 필터를 통과해야 다음 필터로 넘어갑니다. 인증에 실패하면 그 자리에서 요청이 차단되고 401이나 403 응답이 반환됩니다.

---

### Q7. @Component, @Service, @Repository의 차이는 무엇인가요?

세 가지 모두 스프링 빈으로 등록된다는 점은 동일합니다.

차이는 역할의 명시성과 부가 기능입니다. `@Component`는 범용적으로 사용하는 어노테이션이고, `@Service`는 비즈니스 로직 계층임을 명시합니다. `@Repository`는 DB 접근 계층임을 명시하며 추가로 DB 관련 예외를 스프링의 `DataAccessException`으로 변환해주는 기능이 있어서 특정 DB 기술에 종속되지 않는 예외 처리가 가능합니다.

---

> 공부 후 체크 및 면접 답변 직접 말로 연습할 것
