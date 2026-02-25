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

## 4. 싱글톤 패턴 & 스프링 컨테이너

### 싱글톤 패턴이란

애플리케이션 전체에서 **객체를 딱 1개만 생성**하여 공유하는 패턴입니다.

**싱글톤 없을 때**
```java
// 요청마다 새 객체 생성 → 메모리 낭비
MemberService service1 = new MemberService(); // 요청1
MemberService service2 = new MemberService(); // 요청2
MemberService service3 = new MemberService(); // 요청3
```

**스프링 컨테이너 = 싱글톤 컨테이너**
```java
// 스프링이 빈을 딱 1개만 만들어서 공유
MemberService service1 = applicationContext.getBean(MemberService.class);
MemberService service2 = applicationContext.getBean(MemberService.class);
// service1 == service2 (같은 객체)
```

### 싱글톤 주의사항 - 무상태(Stateless) 설계

여러 요청이 같은 객체를 공유하기 때문에 **상태를 가지면 안 됩니다.**

```java
// 위험한 코드 ❌
@Service
public class OrderService {
    private int price; // 공유 필드 → 여러 요청이 같이 씀

    public void order(int price) {
        this.price = price; // A가 10000 저장
        // B가 동시에 20000 저장 → A의 price가 20000으로 바뀜 💥
    }
}

// 올바른 코드 ✅
@Service
public class OrderService {
    public int order(int price) {
        return price; // 지역변수 사용 → 공유 안 됨
    }
}
```

---

### @Configuration과 싱글톤 보장

```java
@Configuration
public class AppConfig {

    @Bean
    public MemberService memberService() {
        return new MemberService(memberRepository()); // memberRepository() 호출
    }

    @Bean
    public OrderService orderService() {
        return new OrderService(memberRepository()); // memberRepository() 또 호출
    }

    @Bean
    public MemberRepository memberRepository() {
        return new MemoryMemberRepository();
    }
}
```

`memberRepository()`가 2번 호출되지만 스프링이 `@Configuration`을 통해 **싱글톤을 보장**합니다.
내부적으로 CGLIB 바이트코드 조작 기술을 사용하여 이미 만들어진 빈이 있으면 그걸 반환합니다.

---

### 빈 스코프

빈이 존재할 수 있는 **범위**를 의미합니다.

| 스코프 | 설명 | 생명주기 |
|--------|------|---------|
| **싱글톤** | 기본값, 컨테이너와 생명주기 같음 | 컨테이너 시작 ~ 종료 |
| **프로토타입** | 요청마다 새 객체 생성 | 생성 후 스프링이 관리 안 함 |
| **request** | HTTP 요청마다 새 객체 | 요청 시작 ~ 응답 끝 |
| **session** | HTTP 세션마다 새 객체 | 세션 시작 ~ 종료 |

```java
@Scope("prototype")
@Component
public class PrototypeBean { ... }
// 요청마다 새로 생성, @PreDestroy 호출 안 됨

@Scope("request")
@Component
public class RequestBean { ... }
// 하나의 HTTP 요청 안에서만 같은 객체 공유
```

---

## 5. 스프링 MVC 구조

### 전체 흐름

```
HTTP 요청
    ↓
DispatcherServlet (프론트 컨트롤러)
    ↓
HandlerMapping → 어떤 컨트롤러가 처리할지 조회
    ↓
HandlerAdapter → 컨트롤러 실행
    ↓
Controller (실제 비즈니스 로직)
    ↓
ModelAndView 반환
    ↓
ViewResolver → 뷰 이름으로 실제 뷰 찾기
    ↓
View → HTML 렌더링
    ↓
HTTP 응답
```

### DispatcherServlet

스프링 MVC의 **핵심 (프론트 컨트롤러)**입니다.
모든 HTTP 요청을 여기서 먼저 받아서 처리를 위임합니다.

```
클라이언트 요청 → DispatcherServlet이 중앙에서 받음
→ 적절한 컨트롤러 찾아서 위임
→ 응답 처리
```

### HandlerMapping

요청 URL에 맞는 **컨트롤러를 찾아주는 역할**입니다.

```
GET /members → MemberController.getMembers() 로 매핑
POST /orders → OrderController.createOrder() 로 매핑
```

### HTTP 메시지 컨버터

`@RequestBody`, `@ResponseBody` 사용 시 동작합니다.
HTTP 요청/응답의 Body를 **자바 객체로 변환**하거나 반대로 변환합니다.

```java
@PostMapping("/members")
public ResponseEntity<Member> createMember(@RequestBody MemberDto dto) {
    // @RequestBody → JSON → MemberDto 변환 (HTTP 메시지 컨버터 동작)
    Member member = memberService.join(dto);
    return ResponseEntity.ok(member);
    // Member → JSON 변환 (HTTP 메시지 컨버터 동작)
}
```

```
요청: JSON {"name": "홍길동"}
    ↓ HTTP 메시지 컨버터 (Jackson)
MemberDto { name = "홍길동" }

MemberDto { name = "홍길동" }
    ↓ HTTP 메시지 컨버터 (Jackson)
응답: JSON {"name": "홍길동"}
```

### @RequestParam vs @ModelAttribute vs @RequestBody

```java
// @RequestParam - 쿼리 파라미터, 단순 타입
// GET /members?name=홍길동&age=20
public String search(@RequestParam String name, @RequestParam int age) { }

// @ModelAttribute - 폼 데이터, 객체로 바인딩
// POST /members (form data)
public String create(@ModelAttribute MemberDto dto) { }

// @RequestBody - HTTP Body의 JSON
// POST /members (JSON body)
public String create(@RequestBody MemberDto dto) { }
```

---

## 6. 필터 vs 인터셉터

김영한 강의에서 중요하게 다루는 내용입니다.
둘 다 요청 전/후에 공통 처리를 하는 역할이지만 **동작 위치가 다릅니다.**

### 동작 위치

```
HTTP 요청
    ↓
서블릿 필터 (Filter)          ← 서블릿 기술 (스프링 밖)
    ↓
DispatcherServlet
    ↓
스프링 인터셉터 (Interceptor)  ← 스프링 기술 (스프링 안)
    ↓
Controller
```

### 필터 (Filter)

```java
@Component
public class LogFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        // 요청 전 처리
        log.info("요청 들어옴");

        chain.doFilter(request, response); // 다음 필터 or 서블릿으로

        // 응답 후 처리
        log.info("응답 나감");
    }
}
```

### 인터셉터 (Interceptor)

```java
@Component
public class LoginCheckInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 컨트롤러 실행 전
        // false 반환 시 컨트롤러 실행 안 됨
        String token = request.getHeader("Authorization");
        if (token == null) {
            response.setStatus(401);
            return false; // 여기서 차단
        }
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) {
        // 컨트롤러 실행 후, 뷰 렌더링 전
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 뷰 렌더링 후 (예외 발생해도 실행)
    }
}
```

### 필터 vs 인터셉터 비교

| | 필터 | 인터셉터 |
|--|------|---------|
| 기술 | 서블릿 표준 | 스프링 기술 |
| 위치 | DispatcherServlet 앞 | DispatcherServlet 뒤 |
| 스프링 빈 접근 | 어려움 | 쉬움 |
| 세밀한 제어 | preHandle만 | pre/post/after 3단계 |
| 사용 예 | 인코딩, XSS 방어 | 로그인 체크, 로깅 |

> 실무에서는 **스프링 빈을 쓸 수 있는 인터셉터**를 더 많이 사용합니다.

---

## 7. 예외 처리

### @ExceptionHandler

특정 예외 발생 시 처리할 메서드를 지정합니다.

```java
@RestController
public class MemberController {

    @GetMapping("/members/{id}")
    public Member getMember(@PathVariable Long id) {
        return memberService.findById(id); // MemberNotFoundException 발생 가능
    }

    // 이 컨트롤러에서 발생한 예외만 처리
    @ExceptionHandler(MemberNotFoundException.class)
    public ResponseEntity<String> handleMemberNotFound(MemberNotFoundException e) {
        return ResponseEntity.status(404).body(e.getMessage());
    }
}
```

### @ControllerAdvice / @RestControllerAdvice

**전체 컨트롤러**의 예외를 한 곳에서 처리합니다.

```java
@RestControllerAdvice  // 모든 컨트롤러에 적용
public class GlobalExceptionHandler {

    // 회원을 못 찾은 경우
    @ExceptionHandler(MemberNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleMemberNotFound(MemberNotFoundException e) {
        return ResponseEntity.status(404)
                .body(new ErrorResponse("MEMBER_NOT_FOUND", e.getMessage()));
    }

    // 잘못된 요청값
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        return ResponseEntity.status(400)
                .body(new ErrorResponse("INVALID_INPUT", "입력값이 올바르지 않습니다."));
    }

    // 그 외 모든 예외
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        return ResponseEntity.status(500)
                .body(new ErrorResponse("INTERNAL_ERROR", "서버 오류가 발생했습니다."));
    }
}
```

**장점**: 예외 처리 코드가 한 곳에 모여서 관리가 쉬움

---

## 8. 검증 (Validation)

### @Valid / @Validated

요청 데이터의 유효성을 검증합니다.

```java
// DTO에 검증 조건 선언
public class MemberDto {

    @NotBlank(message = "이름은 필수입니다.")
    private String name;

    @Min(value = 1, message = "나이는 1 이상이어야 합니다.")
    @Max(value = 150, message = "나이는 150 이하이어야 합니다.")
    private int age;

    @Email(message = "이메일 형식이 아닙니다.")
    private String email;
}

// 컨트롤러에서 @Valid 붙이면 자동 검증
@PostMapping("/members")
public ResponseEntity<Member> create(@Valid @RequestBody MemberDto dto) {
    // dto가 검증 실패하면 MethodArgumentNotValidException 발생
    return ResponseEntity.ok(memberService.join(dto));
}
```

---

## 9. 기타

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

### Q8. 스프링 빈 스코프에 대해 설명해주세요.

스프링 빈의 스코프는 빈이 존재할 수 있는 범위를 의미합니다.

기본값은 싱글톤 스코프로, 스프링 컨테이너와 생명주기가 같고 하나의 객체를 모든 요청이 공유합니다.

프로토타입 스코프는 요청마다 새로운 객체를 생성합니다. 스프링이 생성까지만 관여하고 이후 관리는 하지 않아서 `@PreDestroy`가 호출되지 않습니다.

웹 스코프로는 request(HTTP 요청마다), session(세션마다) 스코프가 있습니다.

실무에서는 싱글톤을 주로 사용하며, 싱글톤 빈은 상태를 가지면 안 된다는 무상태(Stateless) 설계를 지켜야 합니다.

---

### Q9. 필터(Filter)와 인터셉터(Interceptor)의 차이는 무엇인가요?

둘 다 요청 전/후에 공통 처리를 하는 역할이지만 동작 위치가 다릅니다.

필터는 서블릿 기술로, DispatcherServlet 앞에서 동작합니다. 스프링과 무관하게 동작하며 인코딩 처리, XSS 방어 같은 용도로 사용합니다.

인터셉터는 스프링 기술로, DispatcherServlet 이후 컨트롤러 앞에서 동작합니다. 스프링 빈에 접근할 수 있고 preHandle, postHandle, afterCompletion 3단계로 세밀하게 제어할 수 있습니다. 로그인 체크, 로깅 같은 용도로 사용합니다.

실무에서는 스프링 빈을 활용할 수 있는 인터셉터를 더 많이 사용합니다.

---

### Q10. @ControllerAdvice가 무엇인가요?

`@ControllerAdvice`는 모든 컨트롤러에서 발생하는 예외를 한 곳에서 처리할 수 있게 해주는 어노테이션입니다.

`@ExceptionHandler`를 각 컨트롤러마다 작성하면 중복 코드가 발생하는데, `@ControllerAdvice`를 사용하면 전역 예외 처리 클래스를 만들어 한 곳에서 관리할 수 있습니다.

REST API에서는 `@RestControllerAdvice`를 사용하면 응답을 JSON으로 반환합니다. 예외 종류별로 `@ExceptionHandler`를 선언하여 404, 400, 500 등 상황에 맞는 응답을 내려줄 수 있습니다.

---

### Q11. 스프링 MVC 요청 처리 흐름을 설명해주세요.

HTTP 요청이 들어오면 DispatcherServlet이 가장 먼저 받습니다.

DispatcherServlet은 HandlerMapping을 통해 요청 URL에 맞는 컨트롤러를 조회합니다. 그 다음 HandlerAdapter를 통해 컨트롤러를 실행합니다.

컨트롤러가 비즈니스 로직을 처리하고 결과를 반환하면, ViewResolver가 뷰 이름으로 실제 뷰를 찾아 렌더링하거나, `@ResponseBody`의 경우 HTTP 메시지 컨버터가 객체를 JSON으로 변환하여 응답합니다.

---

---

## 중~고급 스프링 개념

---

## 10. 트랜잭션 심화

### 격리 수준 (Isolation Level)

여러 트랜잭션이 동시에 실행될 때 **서로 얼마나 영향을 주는가**에 대한 설정입니다.

격리 수준이 낮을수록 성능이 좋지만 데이터 정합성 문제가 생깁니다.

#### 발생할 수 있는 문제

```
Dirty Read     - 다른 트랜잭션의 커밋 안 된 데이터를 읽음
Non-Repeatable Read - 같은 쿼리를 두 번 실행했는데 결과가 다름
Phantom Read   - 같은 쿼리를 두 번 실행했는데 행 개수가 다름
```

#### 격리 수준 4단계

| 격리 수준 | Dirty Read | Non-Repeatable Read | Phantom Read |
|----------|-----------|-------------------|-------------|
| READ_UNCOMMITTED | 발생 | 발생 | 발생 |
| READ_COMMITTED | 없음 | 발생 | 발생 |
| REPEATABLE_READ | 없음 | 없음 | 발생 |
| SERIALIZABLE | 없음 | 없음 | 없음 |

```java
@Transactional(isolation = Isolation.READ_COMMITTED)  // 기본값 (대부분 DB)
public void order() { ... }

@Transactional(isolation = Isolation.REPEATABLE_READ)  // MySQL InnoDB 기본값
public void order() { ... }
```

> 실무에서는 대부분 **DB 기본값(READ_COMMITTED)** 사용

---

### 전파 옵션 (Propagation) 심화

| 옵션 | 설명 |
|------|------|
| REQUIRED | 기존 트랜잭션 있으면 참여, 없으면 새로 생성 (기본값) |
| REQUIRES_NEW | 항상 새 트랜잭션 생성, 기존 트랜잭션 일시 정지 |
| SUPPORTS | 기존 트랜잭션 있으면 참여, 없으면 트랜잭션 없이 실행 |
| NOT_SUPPORTED | 트랜잭션 없이 실행, 기존 트랜잭션 일시 정지 |
| MANDATORY | 기존 트랜잭션 없으면 예외 발생 |
| NEVER | 트랜잭션 있으면 예외 발생 |
| NESTED | 중첩 트랜잭션 생성 (부모 rollback → 자식도 rollback, 자식 rollback → 부모 유지) |

```java
// 실무에서 자주 쓰는 패턴
@Transactional
public void pay(Long orderId) {
    paymentProcess();      // 같은 트랜잭션 (REQUIRED)
    logService.log();      // 별도 트랜잭션 (REQUIRES_NEW) → 결제 실패해도 로그는 남김
}

@Transactional(propagation = Propagation.REQUIRES_NEW)
public void log() {
    // 독립적인 트랜잭션으로 실행
}
```

---

### readOnly = true

```java
@Transactional(readOnly = true)
public List<Member> findAll() { ... }
```

**성능 이점:**
```
1. 변경 감지(Dirty Checking) 비활성화 → 스냅샷 저장 안 함 → 메모리 절약
2. DB에 따라 읽기 전용 커넥션 사용 가능 (Master-Slave 구조에서 Slave로 라우팅)
3. JPA 플러시 모드 NEVER → 불필요한 flush 방지
```

> 조회 메서드에는 항상 `readOnly = true` 붙이는 것이 좋습니다.

---

## 11. 비동기 처리

### @Async

메서드를 **별도 스레드에서 비동기로 실행**합니다.

```java
@SpringBootApplication
@EnableAsync  // 비동기 활성화 필수
public class Application { ... }

@Service
public class EmailService {

    @Async  // 이 메서드는 별도 스레드에서 실행
    public void sendEmail(String email) {
        // 시간이 걸리는 이메일 발송 로직
        // 호출한 쪽은 기다리지 않고 바로 다음 코드 실행
    }
}

// 사용하는 쪽
@Service
public class OrderService {
    public void createOrder() {
        // 주문 처리
        emailService.sendEmail(email); // 비동기 → 기다리지 않음
        // 바로 여기로 옴
    }
}
```

**반환값이 필요한 경우**
```java
@Async
public CompletableFuture<String> sendEmailWithResult() {
    // 처리
    return CompletableFuture.completedFuture("발송 완료");
}

// 결과 기다리기
CompletableFuture<String> future = emailService.sendEmailWithResult();
String result = future.get(); // 여기서 결과 기다림
```

**주의:** `@Async`도 `@Transactional`과 마찬가지로 **같은 클래스 내부 호출 시 동작 안 함** (프록시 문제)

---

## 12. 스케줄링

### @Scheduled

**정해진 시간에 자동으로 실행**되는 메서드를 만듭니다.

```java
@SpringBootApplication
@EnableScheduling  // 스케줄링 활성화 필수
public class Application { ... }

@Component
public class BatchJob {

    // 매일 새벽 2시에 실행
    @Scheduled(cron = "0 0 2 * * *")
    public void dailyBatch() {
        // 정산, 통계 처리 등
    }

    // 5초마다 실행
    @Scheduled(fixedRate = 5000)
    public void healthCheck() { ... }

    // 이전 작업 끝나고 3초 후 실행
    @Scheduled(fixedDelay = 3000)
    public void syncData() { ... }
}
```

**cron 표현식**
```
"0 0 2 * * *"
 초 분 시 일 월 요일
→ 매일 02:00:00 실행

"0 0/30 * * * *"  → 30분마다
"0 0 9 * * MON"   → 매주 월요일 09:00
```

---

## 13. 캐싱

### @Cacheable / @CacheEvict

```java
@Service
public class ProductService {

    // 결과를 캐시에 저장 (같은 id로 다시 호출 시 DB 조회 안 하고 캐시 반환)
    @Cacheable(value = "product", key = "#id")
    public Product findById(Long id) {
        return productRepository.findById(id).orElseThrow();
    }

    // 상품 수정 시 캐시 삭제
    @CacheEvict(value = "product", key = "#product.id")
    public void update(Product product) {
        productRepository.save(product);
    }

    // 모든 product 캐시 삭제
    @CacheEvict(value = "product", allEntries = true)
    public void clearAll() { ... }
}
```

**Redis와 함께 사용**
```yaml
# application.yml
spring:
  cache:
    type: redis  # 캐시 저장소로 Redis 사용
```

---

## 14. 외부 API 호출

### RestTemplate vs WebClient

| | RestTemplate | WebClient |
|--|-------------|-----------|
| 방식 | 동기 (결과 기다림) | 동기/비동기 모두 가능 |
| 스레드 | 요청마다 스레드 점유 | 적은 스레드로 처리 |
| 스프링 버전 | 구버전 | Spring 5+ 권장 |
| 상태 | 유지보수 모드 | 현재 권장 |

**RestTemplate (구버전)**
```java
RestTemplate restTemplate = new RestTemplate();

// GET 요청
Member member = restTemplate.getForObject(
    "https://api.example.com/members/1",
    Member.class
);

// POST 요청
ResponseEntity<Member> response = restTemplate.postForEntity(
    "https://api.example.com/members",
    new MemberDto("홍길동"),
    Member.class
);
```

**WebClient (현재 권장)**
```java
WebClient webClient = WebClient.create("https://api.example.com");

// GET 요청 (동기)
Member member = webClient.get()
    .uri("/members/1")
    .retrieve()
    .bodyToMono(Member.class)
    .block(); // 동기로 결과 기다림

// GET 요청 (비동기)
Mono<Member> memberMono = webClient.get()
    .uri("/members/1")
    .retrieve()
    .bodyToMono(Member.class); // 비동기, 나중에 구독
```

---

## 15. 설정값 관리

### @Value vs @ConfigurationProperties

**@Value - 단순한 값 하나씩**
```java
@Service
public class KakaoPayService {

    @Value("${kakao.pay.secret-key}")
    private String secretKey;

    @Value("${kakao.pay.base-url}")
    private String baseUrl;
}
```

**@ConfigurationProperties - 관련 설정을 묶어서**
```java
// application.yml
// kakao:
//   pay:
//     secret-key: abc123
//     base-url: https://kapi.kakao.com
//     timeout: 3000

@ConfigurationProperties(prefix = "kakao.pay")
@Component
public class KakaoPayProperties {
    private String secretKey;
    private String baseUrl;
    private int timeout;
    // getter/setter
}

// 사용
@Service
@RequiredArgsConstructor
public class KakaoPayService {
    private final KakaoPayProperties properties;

    public void pay() {
        String key = properties.getSecretKey();
    }
}
```

> 관련 설정이 많다면 `@ConfigurationProperties`가 타입 안전하고 관리하기 좋습니다.

---

## 16. 프로파일 (@Profile)

환경(개발/운영)에 따라 다른 빈을 등록합니다.

```java
// 개발 환경에서만 사용
@Profile("dev")
@Component
public class DevDataInitializer {
    @PostConstruct
    public void init() {
        // 테스트 데이터 자동 삽입
    }
}

// 운영 환경에서만 사용
@Profile("prod")
@Component
public class ProdDataInitializer { ... }
```

```yaml
# application-dev.yml
spring:
  datasource:
    url: jdbc:h2:mem:testdb  # 인메모리 DB

# application-prod.yml
spring:
  datasource:
    url: jdbc:mysql://prod-db:3306/mydb  # 실제 DB
```

```
# 실행 시 프로파일 지정
java -jar app.jar --spring.profiles.active=prod
```

---

## 17. 이벤트 처리

### ApplicationEvent / @EventListener

서비스 간 결합도를 낮추는 **스프링 내부 이벤트 시스템**입니다.

```java
// 이벤트 클래스 정의
public class OrderCreatedEvent {
    private final Long orderId;
    private final String memberEmail;

    public OrderCreatedEvent(Long orderId, String memberEmail) {
        this.orderId = orderId;
        this.memberEmail = memberEmail;
    }
}

// 이벤트 발행
@Service
@RequiredArgsConstructor
public class OrderService {
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void createOrder(OrderDto dto) {
        // 주문 처리
        Order order = orderRepository.save(new Order(dto));

        // 이벤트 발행 (알림, 포인트 적립 서비스를 직접 호출하지 않음)
        eventPublisher.publishEvent(new OrderCreatedEvent(order.getId(), dto.getEmail()));
    }
}

// 이벤트 수신
@Component
public class OrderEventListener {

    @EventListener
    public void handleOrderCreated(OrderCreatedEvent event) {
        // 이메일 발송
        emailService.send(event.getMemberEmail());
    }

    @Async
    @EventListener
    public void handleOrderCreatedAsync(OrderCreatedEvent event) {
        // 비동기로 포인트 적립
        pointService.save(event.getOrderId());
    }
}
```

**장점:**
```
OrderService가 EmailService, PointService를 직접 알 필요 없음
새 기능 추가 시 OrderService 코드 수정 없이 리스너만 추가하면 됨
```

---

## 18. 테스트

### 테스트 어노테이션 종류

```java
// 전체 스프링 컨텍스트 로드 - 통합 테스트 (느림)
@SpringBootTest
class OrderServiceIntegrationTest { ... }

// Controller 레이어만 테스트 - Security, Filter 포함
@WebMvcTest(OrderController.class)
class OrderControllerTest {
    @Autowired MockMvc mockMvc;
    @MockBean OrderService orderService; // 서비스는 Mock으로 대체
}

// JPA 레이어만 테스트 - Repository 테스트
@DataJpaTest
class MemberRepositoryTest {
    @Autowired MemberRepository memberRepository;
    // 인메모리 DB 사용, 트랜잭션 자동 롤백
}

// 단위 테스트 - 스프링 컨텍스트 없음 (가장 빠름)
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    @Mock MemberRepository memberRepository;
    @InjectMocks OrderService orderService;
}
```

### Mockito 기본 사용법

```java
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    MemberRepository memberRepository;

    @InjectMocks
    OrderService orderService;

    @Test
    void 주문_생성_성공() {
        // given - Mock 동작 정의
        Member member = new Member(1L, "홍길동");
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));

        // when - 실행
        Order order = orderService.createOrder(1L, "치킨");

        // then - 검증
        assertThat(order.getMemberName()).isEqualTo("홍길동");
        verify(memberRepository, times(1)).findById(1L); // 1번 호출됐는지 확인
    }
}
```

### MockMvc로 Controller 테스트

```java
@WebMvcTest(MemberController.class)
class MemberControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    MemberService memberService;

    @Test
    void 회원_조회() throws Exception {
        // given
        given(memberService.findById(1L)).willReturn(new MemberDto("홍길동"));

        // when & then
        mockMvc.perform(get("/members/1")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("홍길동"));
    }
}
```

---

## 19. CORS

### CORS란

다른 도메인에서 API를 호출할 때 브라우저가 막는 보안 정책입니다.

```
프론트엔드: https://www.oliveyoung.co.kr
백엔드 API: https://api.oliveyoung.co.kr

→ 다른 도메인 → 브라우저가 기본으로 차단
→ 서버에서 허용해줘야 함
```

### 설정 방법

```java
// 방법 1 - @CrossOrigin (컨트롤러에 직접)
@CrossOrigin(origins = "https://www.oliveyoung.co.kr")
@RestController
public class ProductController { ... }

// 방법 2 - 전역 설정 (권장)
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")               // 허용할 경로
            .allowedOrigins("https://www.oliveyoung.co.kr")  // 허용할 도메인
            .allowedMethods("GET", "POST", "PUT", "DELETE")  // 허용할 HTTP 메서드
            .allowedHeaders("*")                     // 허용할 헤더
            .allowCredentials(true)                  // 쿠키 허용
            .maxAge(3600);                           // preflight 캐시 시간
    }
}
```

---

## 면접 추가 질문 (중~고급)

---

### Q12. 트랜잭션 격리 수준에 대해 설명해주세요.

트랜잭션 격리 수준은 여러 트랜잭션이 동시에 실행될 때 서로 얼마나 영향을 주는지에 대한 설정입니다.

READ_UNCOMMITTED는 커밋 안 된 데이터를 읽을 수 있어 Dirty Read가 발생합니다. READ_COMMITTED는 커밋된 데이터만 읽지만 같은 쿼리를 두 번 실행 시 결과가 달라지는 Non-Repeatable Read가 발생합니다. REPEATABLE_READ는 같은 쿼리는 항상 같은 결과를 보장하지만 새로운 행이 추가되는 Phantom Read가 발생합니다. SERIALIZABLE은 완전한 격리를 보장하지만 성능이 가장 낮습니다.

실무에서는 대부분 READ_COMMITTED를 사용하고, MySQL InnoDB는 기본값이 REPEATABLE_READ입니다.

---

### Q13. @Async 사용 시 주의사항은 무엇인가요?

첫째, `@EnableAsync`를 반드시 선언해야 합니다.

둘째, `@Transactional`과 마찬가지로 같은 클래스 내부에서 `this.메서드()`로 호출하면 프록시를 거치지 않아 동작하지 않습니다.

셋째, 별도 스레드에서 실행되므로 `@Transactional`과 함께 사용 시 트랜잭션이 공유되지 않습니다. 비동기 메서드는 별도 트랜잭션을 갖습니다.

넷째, 스레드 풀 설정을 하지 않으면 기본 스레드 풀을 사용하는데, 적절한 스레드 풀 크기를 설정하지 않으면 성능 문제가 생길 수 있습니다.

---

### Q14. RestTemplate과 WebClient의 차이는 무엇인가요?

RestTemplate은 동기 방식으로 요청을 보내고 응답을 받을 때까지 스레드가 블로킹됩니다. 오래된 API이며 현재는 유지보수 모드입니다.

WebClient는 Spring 5에서 도입된 새로운 HTTP 클라이언트입니다. 동기, 비동기 모두 지원하며 Non-Blocking 방식으로 동작할 수 있어 적은 스레드로 많은 요청을 처리할 수 있습니다. 현재 스프링에서 권장하는 방식입니다.

MSA 환경에서 서비스 간 HTTP 통신이 많을 경우 WebClient의 비동기 방식이 성능상 유리합니다.

---

### Q15. 스프링에서 이벤트를 사용하는 이유는 무엇인가요?

서비스 간 결합도를 낮추기 위해 사용합니다.

예를 들어 주문 생성 후 이메일 발송, 포인트 적립, 재고 차감이 필요할 때, OrderService가 EmailService, PointService, StockService를 직접 호출하면 강하게 결합됩니다. 새 기능이 추가될 때마다 OrderService를 수정해야 합니다.

ApplicationEvent를 사용하면 OrderService는 이벤트만 발행하고, 각 서비스가 이벤트를 구독하여 처리합니다. OrderService는 다른 서비스를 알 필요가 없고, 새 기능 추가 시 리스너만 추가하면 됩니다.

Kafka와 비슷한 개념이지만 스프링 애플리케이션 내부에서 동작하는 점이 다릅니다.

---

### Q16. 스프링 테스트에서 @SpringBootTest와 @WebMvcTest의 차이는 무엇인가요?

`@SpringBootTest`는 전체 스프링 컨텍스트를 로드하는 통합 테스트입니다. 모든 빈이 등록되어 실제 환경과 가장 유사하지만, 로딩 시간이 길고 느립니다.

`@WebMvcTest`는 Controller 레이어만 로드하는 테스트입니다. DispatcherServlet, Filter, Security 설정은 포함하지만 Service, Repository는 `@MockBean`으로 대체합니다. 빠르고 Controller 로직에 집중할 수 있습니다.

`@DataJpaTest`는 JPA 관련 빈만 로드하여 Repository를 테스트합니다. 기본적으로 인메모리 DB를 사용하고 각 테스트 후 트랜잭션을 롤백합니다.

단위 테스트는 스프링 컨텍스트 없이 Mockito만으로 가장 빠르게 실행할 수 있습니다.

---

> 공부 후 체크 및 면접 답변 직접 말로 연습할 것
