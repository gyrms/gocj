# Redis 심화학습 - 기술면접 완벽 대비

> CJ올리브영 면접 기준 작성
> 각 섹션 학습 후 체크박스 표시

---

## 목차
1. [Redis 기본 개념](#1-redis-기본-개념)
2. [자료구조 (Data Structure)](#2-자료구조-data-structure)
3. [캐싱 전략](#3-캐싱-전략)
4. [분산 락 (Redisson)](#4-분산-락-redisson)
5. [세션 저장소 활용](#5-세션-저장소-활용)
6. [Redis 운영 및 장애 대응](#6-redis-운영-및-장애-대응)
7. [Redis vs Memcached](#7-redis-vs-memcached)

---

## 1. Redis 기본 개념

### Redis란?

**Redis(Remote Dictionary Server)**는 인메모리(In-Memory) 기반의 키-값(Key-Value) 저장소다.
모든 데이터를 메모리에 저장하기 때문에 디스크 기반 DB(MySQL 등)보다 **수십~수백 배 빠른 응답속도**를 제공한다.

```
[클라이언트] → Redis (메모리, ~1ms)
[클라이언트] → MySQL (디스크, ~100ms)
```

### 주요 특징

| 특징 | 설명 |
|------|------|
| 인메모리 저장 | RAM에 저장, 초고속 응답 (~1ms) |
| 다양한 자료구조 | String, Hash, List, Set, ZSet 등 |
| 영속성 지원 | RDB, AOF로 데이터 유실 방지 |
| 싱글 스레드 | 원자성 보장, Race Condition 방지 |
| TTL 지원 | 만료 시간 설정으로 자동 삭제 |
| Pub/Sub | 메시지 브로커 역할 가능 |

### 싱글 스레드이지만 빠른 이유

```
1. 모든 데이터가 메모리에 있어 디스크 I/O 없음
2. 단순한 키-값 구조로 연산 자체가 가벼움
3. I/O 멀티플렉싱으로 여러 클라이언트 동시 처리
4. 싱글 스레드 덕분에 Context Switching 비용 없음
```

> 💡 싱글 스레드 = Race Condition 없음 = INCR 같은 연산이 항상 원자적으로 실행됨

### 영속성 옵션

```
RDB (Redis Database)  - 특정 시점(스냅샷) 저장. 빠른 복구. 데이터 유실 가능
AOF (Append Only File) - 모든 쓰기 명령을 로그로 기록. 데이터 유실 최소화. 느린 복구
```

### 면접 예상 질문

**Q. Redis가 빠른 이유는 무엇인가요?**
> A. Redis는 모든 데이터를 RAM에 저장하는 인메모리 방식이기 때문에 디스크 I/O가 없습니다.
> 또한 단순한 키-값 구조로 연산 자체가 가볍고, 싱글 스레드로 동작해 Context Switching 비용이 없습니다.
> 덕분에 MySQL 같은 디스크 DB 대비 수십~수백 배 빠른 1ms 수준의 응답 속도를 제공합니다.

---

## 2. 자료구조 (Data Structure)

### 2-1. String (문자열)

가장 기본적인 자료구조. 문자열, 숫자, JSON 모두 저장 가능.

```bash
# 기본 저장/조회
SET user:1:name "윤효근"
GET user:1:name  # "윤효근"

# TTL(만료시간) 설정 - 초 단위
SET session:token "abc123" EX 3600     # 1시간 후 자동 삭제
SETEX session:token 3600 "abc123"      # 동일한 효과

# 숫자 카운터 (원자적 연산 - 동시성 안전)
SET page:views 0
INCR page:views    # 1 (1 증가)
INCRBY page:views 5  # 6 (5 증가)
DECR page:views    # 5 (1 감소)
```

**이커머스 활용 사례:**
```bash
# 상품 조회수 카운터
INCR product:1001:views

# 재고 원자적 차감 (동시성 이슈 없음)
DECRBY product:1001:stock 1

# 쿠폰 발급 수 제한
INCR coupon:SAVE10:issued
# 발급 수가 1000 넘으면 발급 중단 처리
```

```java
// Spring Data Redis 사용
@Service
public class ProductCacheService {

    private final StringRedisTemplate redisTemplate;

    // 상품 조회수 증가
    public Long incrementViewCount(Long productId) {
        return redisTemplate.opsForValue()
            .increment("product:" + productId + ":views");
    }

    // 재고 캐시 저장 (30분)
    public void cacheStock(Long productId, int stock) {
        redisTemplate.opsForValue()
            .set("product:" + productId + ":stock",
                 String.valueOf(stock),
                 30, TimeUnit.MINUTES);
    }
}
```

---

### 2-2. Hash (해시)

**객체**를 필드:값 형태로 저장. 하나의 키에 여러 필드를 담을 수 있음.

```bash
# 회원 정보 저장
HSET user:1 name "윤효근" email "yhg@example.com" grade "VIP"

# 특정 필드 조회
HGET user:1 name         # "윤효근"
HGET user:1 grade        # "VIP"

# 전체 필드 조회
HGETALL user:1
# 1) "name"
# 2) "윤효근"
# 3) "email"
# 4) "yhg@example.com"
# 5) "grade"
# 6) "VIP"

# 특정 필드만 업데이트
HSET user:1 grade "GOLD"
```

**String vs Hash 비교:**
```
String으로 저장:
  SET user:1:name "윤효근"
  SET user:1:email "yhg@example.com"
  → 키가 많아지고 관리 복잡

Hash로 저장:
  HSET user:1 name "윤효근" email "yhg@example.com"
  → 하나의 키에 객체처럼 관리 (관련 데이터 묶음)
```

```java
// 장바구니를 Hash로 관리
@Service
public class CartService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final HashOperations<String, String, Integer> hashOps;

    // 장바구니에 상품 추가
    public void addToCart(Long userId, Long productId, int quantity) {
        String key = "cart:" + userId;
        hashOps.put(key, productId.toString(), quantity);
        redisTemplate.expire(key, 7, TimeUnit.DAYS); // 7일 만료
    }

    // 장바구니 전체 조회
    public Map<String, Integer> getCart(Long userId) {
        return hashOps.entries("cart:" + userId);
    }

    // 장바구니에서 상품 삭제
    public void removeFromCart(Long userId, Long productId) {
        hashOps.delete("cart:" + userId, productId.toString());
    }
}
```

---

### 2-3. List (리스트)

양쪽 끝에서 push/pop이 가능한 **양방향 연결 리스트**.
Queue(큐), Stack(스택) 구현에 사용.

```bash
# 오른쪽에 추가 (큐로 사용 시)
RPUSH notification:user:1 "주문 완료" "배송 시작" "배송 완료"

# 왼쪽에서 꺼내기 (FIFO - 큐)
LPOP notification:user:1  # "주문 완료"

# 왼쪽에 추가 (스택으로 사용 시)
LPUSH recent:user:1 "상품A" "상품B" "상품C"
LPOP recent:user:1  # "상품C" (가장 최근 조회)

# 범위 조회 (0부터 9까지 = 10개)
LRANGE recent:user:1 0 9

# 리스트 길이 제한 (최근 50개만 유지)
LTRIM recent:user:1 0 49
```

```java
// 최근 본 상품 목록 (최대 10개)
@Service
public class RecentViewService {

    private final RedisTemplate<String, String> redisTemplate;

    public void addRecentView(Long userId, Long productId) {
        String key = "recent:view:" + userId;
        ListOperations<String, String> listOps = redisTemplate.opsForList();

        listOps.leftPush(key, productId.toString()); // 최신 상품을 앞에 추가
        listOps.trim(key, 0, 9);                     // 최근 10개만 유지
        redisTemplate.expire(key, 30, TimeUnit.DAYS); // 30일 만료
    }

    public List<String> getRecentViews(Long userId) {
        String key = "recent:view:" + userId;
        return redisTemplate.opsForList().range(key, 0, 9);
    }
}
```

---

### 2-4. Set (집합)

**중복을 허용하지 않는** 순서 없는 집합.
교집합, 합집합, 차집합 연산 지원.

```bash
# 좋아요 누른 유저 저장
SADD product:1001:likes 101 102 103 104
SADD product:1001:likes 102  # 중복 → 무시됨

# 좋아요 수 확인
SCARD product:1001:likes  # 4

# 특정 유저가 좋아요 눌렀는지 확인 (O(1))
SISMEMBER product:1001:likes 102  # 1 (있음)
SISMEMBER product:1001:likes 999  # 0 (없음)

# 좋아요 취소
SREM product:1001:likes 102

# 교집합: 두 상품 모두 좋아요 누른 유저
SINTER product:1001:likes product:1002:likes

# 합집합: 두 상품 중 하나라도 좋아요 누른 유저
SUNION product:1001:likes product:1002:likes
```

```java
// 쿠폰 중복 발급 방지
@Service
public class CouponService {

    private final SetOperations<String, String> setOps;

    public boolean issueCoupon(Long userId, String couponCode) {
        String key = "coupon:issued:" + couponCode;

        // SADD는 추가 성공 시 1, 이미 있으면 0 반환
        Long added = setOps.add(key, userId.toString());

        if (added == 0L) {
            throw new IllegalStateException("이미 발급된 쿠폰입니다.");
        }
        return true;
    }

    public boolean hasIssuedCoupon(Long userId, String couponCode) {
        String key = "coupon:issued:" + couponCode;
        return Boolean.TRUE.equals(setOps.isMember(key, userId.toString()));
    }
}
```

---

### 2-5. ZSet / Sorted Set (정렬된 집합)

**score(점수)를 기준으로 자동 정렬**되는 집합.
랭킹, 인기 상품, 대기열 구현에 매우 유용.

```bash
# 상품 판매량 랭킹
ZADD ranking:weekly 150 "product:1001"
ZADD ranking:weekly 320 "product:1002"
ZADD ranking:weekly 85  "product:1003"

# score 증가 (판매될 때마다)
ZINCRBY ranking:weekly 1 "product:1001"

# 상위 10개 조회 (내림차순)
ZREVRANGE ranking:weekly 0 9          # 상품 ID만
ZREVRANGE ranking:weekly 0 9 WITHSCORES  # 점수 포함

# 내 순위 확인 (0부터 시작)
ZREVRANK ranking:weekly "product:1001"  # 순위 (0 = 1위)

# score 조회
ZSCORE ranking:weekly "product:1001"    # 151
```

```java
// 실시간 인기 상품 랭킹
@Service
public class RankingService {

    private final ZSetOperations<String, String> zSetOps;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String WEEKLY_RANKING = "ranking:weekly";

    // 상품 판매 시 랭킹 점수 증가
    public void increaseScore(Long productId, double score) {
        zSetOps.incrementScore(WEEKLY_RANKING, productId.toString(), score);
    }

    // TOP 10 상품 조회
    public List<ProductRankDto> getTop10() {
        Set<ZSetOperations.TypedTuple<String>> tuples =
            zSetOps.reverseRangeWithScores(WEEKLY_RANKING, 0, 9);

        return tuples.stream()
            .map(t -> new ProductRankDto(
                Long.parseLong(t.getValue()),
                t.getScore()))
            .collect(Collectors.toList());
    }

    // 특정 상품 순위 조회
    public Long getRank(Long productId) {
        Long rank = zSetOps.reverseRank(WEEKLY_RANKING, productId.toString());
        return rank != null ? rank + 1 : null; // 0-based → 1-based
    }
}
```

**대기열 구현 (선착순 이벤트):**
```java
// 선착순 쿠폰 이벤트 대기열
@Service
public class WaitingQueueService {

    private final ZSetOperations<String, String> zSetOps;

    public void addToQueue(String eventId, Long userId) {
        String key = "queue:" + eventId;
        double score = System.currentTimeMillis(); // 입장 시각 = score
        zSetOps.add(key, userId.toString(), score);
    }

    // 대기 순번 조회 (앞에 몇 명 있는지)
    public Long getWaitingNumber(String eventId, Long userId) {
        String key = "queue:" + eventId;
        return zSetOps.rank(key, userId.toString()); // 0부터 시작
    }

    // 앞에서 N명 처리
    public Set<String> processQueue(String eventId, int count) {
        String key = "queue:" + eventId;
        Set<String> users = zSetOps.range(key, 0, count - 1);
        zSetOps.removeRange(key, 0, count - 1);
        return users;
    }
}
```

### 자료구조 선택 가이드

| 자료구조 | 사용 케이스 |
|---------|-----------|
| String | 캐시, 카운터, 세션 토큰, 플래그 |
| Hash | 객체 저장(회원 정보), 장바구니 |
| List | 최근 본 상품, 알림 큐, 피드 |
| Set | 좋아요, 팔로워, 중복 방지 |
| ZSet | 랭킹, 인기 상품, 선착순 대기열 |

### 면접 예상 질문

**Q. Redis 자료구조를 어떻게 활용해봤나요? ZSet은 어디에 쓰나요?**
> A. 세션 저장소에서는 String, 장바구니는 Hash, 최근 본 상품은 List를 활용했습니다.
> ZSet(Sorted Set)은 score를 기준으로 자동 정렬되는 자료구조로,
> 이커머스에서 주간 인기 상품 랭킹이나 선착순 이벤트 대기열 구현에 유용합니다.
> 상품이 판매될 때마다 ZINCRBY로 score를 증가시키고, ZREVRANGE로 상위 N개를 조회하는 방식으로 구현합니다.

---

## 3. 캐싱 전략

### 3-1. Look-Aside (Cache-Aside) 패턴 - 가장 일반적

애플리케이션이 직접 캐시를 확인하고 없으면 DB에서 읽어 캐시에 저장하는 방식.

```
[조회 흐름]
1. 캐시에서 데이터 조회
2. (캐시 HIT) → 바로 반환
3. (캐시 MISS) → DB에서 조회 → 캐시에 저장 → 반환

[쓰기 흐름]
1. DB에 저장
2. 캐시 삭제 or 업데이트
```

```java
@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final RedisTemplate<String, ProductDto> redisTemplate;

    private static final long CACHE_TTL = 30; // 30분

    public ProductDto getProduct(Long productId) {
        String key = "product:" + productId;
        ValueOperations<String, ProductDto> ops = redisTemplate.opsForValue();

        // 1. 캐시에서 먼저 조회 (Cache HIT)
        ProductDto cached = ops.get(key);
        if (cached != null) {
            return cached; // 캐시 히트 → 즉시 반환
        }

        // 2. 캐시 MISS → DB 조회
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ProductNotFoundException(productId));

        ProductDto dto = ProductDto.from(product);

        // 3. 캐시에 저장 (TTL 30분)
        ops.set(key, dto, CACHE_TTL, TimeUnit.MINUTES);

        return dto;
    }

    // 상품 수정 시 캐시 삭제
    @Transactional
    public void updateProduct(Long productId, UpdateProductRequest request) {
        Product product = productRepository.findById(productId).orElseThrow();
        product.update(request.getName(), request.getPrice());

        // 캐시 무효화 (Cache Invalidation)
        redisTemplate.delete("product:" + productId);
    }
}
```

**Look-Aside 특징:**
```
장점: 캐시가 장애나도 DB에서 직접 조회 가능 (장애 격리)
     필요한 데이터만 캐시에 올라감
단점: 캐시 MISS 시 응답 느림 (캐시 워밍업 필요)
     데이터 불일치 가능 (DB 업데이트 후 캐시 업데이트 전 조회 시)
```

---

### 3-2. Write-Through 패턴

DB에 쓸 때 캐시에도 **동시에** 저장하는 방식.

```
[쓰기 흐름]
1. DB에 저장
2. 캐시에 저장 (동시에)

[조회 흐름]
1. 캐시에서 조회
2. (캐시 HIT) → 반환 (항상 최신 데이터)
```

```java
@Service
public class MemberService {

    @Transactional
    public void updateMemberGrade(Long memberId, Grade grade) {
        // DB 업데이트
        Member member = memberRepository.findById(memberId).orElseThrow();
        member.updateGrade(grade);

        // 캐시도 동시에 업데이트 (Write-Through)
        MemberDto dto = MemberDto.from(member);
        redisTemplate.opsForValue()
            .set("member:" + memberId, dto, 1, TimeUnit.HOURS);
    }
}
```

**Write-Through 특징:**
```
장점: 캐시와 DB 항상 동기화 → 데이터 일관성 보장
단점: 쓰기 비용 증가 (DB + 캐시 두 곳에 항상 저장)
     읽히지 않는 데이터도 캐시에 올라감 (낭비)
사용: 쓰기 후 바로 읽는 경우가 많을 때, 데이터 일관성이 중요할 때
```

---

### 3-3. Write-Behind (Write-Back) 패턴

캐시에 먼저 쓰고, **나중에 비동기로** DB에 반영하는 방식.

```
[쓰기 흐름]
1. 캐시에 저장 (즉시 응답)
2. 일정 시간 후 배치로 DB에 반영

[조회 흐름]
1. 캐시에서 조회 (최신 데이터)
```

```
장점: 쓰기 성능 매우 빠름 (캐시에만 저장하고 즉시 응답)
     DB 부하 감소 (배치로 한 번에 처리)
단점: 캐시 장애 시 데이터 유실 위험
사용: 실시간 좋아요 수, 조회수처럼 빈번한 쓰기가 있을 때
```

```java
// 조회수 카운터 - Write-Behind 방식
// 1. Redis에 먼저 올림 (즉시 반응)
// 2. 배치 스케줄러로 주기적으로 DB 반영

@Service
public class ViewCountService {

    public void increaseView(Long productId) {
        // Redis에 즉시 증가 (O(1))
        redisTemplate.opsForValue()
            .increment("product:" + productId + ":views");
    }
}

@Component
@Scheduled(cron = "0 */5 * * * *") // 5분마다 DB에 반영
public class ViewCountSyncJob {

    public void sync() {
        Set<String> keys = redisTemplate.keys("product:*:views");
        for (String key : keys) {
            Long productId = extractProductId(key);
            String count = redisTemplate.opsForValue().get(key);
            productRepository.updateViewCount(productId, Long.parseLong(count));
        }
    }
}
```

---

### 3-4. Cache Stampede (캐시 스탬피드) 문제

TTL 만료 직후 수많은 요청이 동시에 DB로 몰리는 현상.

```
[문제 상황]
1. 인기 상품 캐시 TTL 만료
2. 동시에 10,000명이 해당 상품 조회 요청
3. 모두 캐시 MISS → 10,000개의 DB 쿼리 동시 발생
4. DB 과부하 → 서비스 장애
```

**해결 방법 1: Mutex Lock (분산 락)**
```java
public ProductDto getProduct(Long productId) {
    String cacheKey = "product:" + productId;
    String lockKey = "lock:product:" + productId;

    // 1. 캐시 조회
    ProductDto cached = (ProductDto) redisTemplate.opsForValue().get(cacheKey);
    if (cached != null) return cached;

    // 2. 캐시 MISS → 락 획득 시도
    Boolean locked = redisTemplate.opsForValue()
        .setIfAbsent(lockKey, "1", 3, TimeUnit.SECONDS);

    if (Boolean.TRUE.equals(locked)) {
        try {
            // 락 획득한 스레드만 DB 조회
            ProductDto dto = ProductDto.from(
                productRepository.findById(productId).orElseThrow());
            redisTemplate.opsForValue().set(cacheKey, dto, 30, TimeUnit.MINUTES);
            return dto;
        } finally {
            redisTemplate.delete(lockKey);
        }
    } else {
        // 락 못 얻은 스레드는 잠시 대기 후 캐시 재조회
        Thread.sleep(100);
        return getProduct(productId); // 재귀 호출 (캐시에 이미 올라옴)
    }
}
```

**해결 방법 2: TTL에 랜덤값 추가 (만료 분산)**
```java
// 동일한 TTL 대신 랜덤 범위 TTL 적용
int ttl = 30 + new Random().nextInt(10); // 30~39분 랜덤
redisTemplate.opsForValue().set(cacheKey, dto, ttl, TimeUnit.MINUTES);
// → 동일 상품이라도 각 서버마다 만료 시간이 다름
```

**해결 방법 3: Early Recomputation (만료 전 갱신)**
```java
// 실제 만료 전에 미리 갱신 (백그라운드에서)
public ProductDto getProductWithEarlyRefresh(Long productId) {
    String key = "product:" + productId;
    Long remainTtl = redisTemplate.getExpire(key, TimeUnit.SECONDS);

    // TTL이 5분 미만이면 미리 비동기 갱신
    if (remainTtl != null && remainTtl < 300) {
        CompletableFuture.runAsync(() -> refreshCache(productId));
    }

    ProductDto cached = (ProductDto) redisTemplate.opsForValue().get(key);
    return cached != null ? cached : loadFromDbAndCache(productId);
}
```

### 캐싱 전략 비교

| 전략 | 읽기 | 쓰기 | 데이터 일관성 | 사용 케이스 |
|------|------|------|------------|-----------|
| Look-Aside | 빠름 (HIT 시) | DB만 저장 | 보통 | 상품 조회, 일반 캐싱 |
| Write-Through | 빠름 | 느림 (DB+캐시) | 높음 | 자주 읽고 쓰는 데이터 |
| Write-Behind | 빠름 | 매우 빠름 | 낮음 | 조회수, 좋아요 카운터 |

### 면접 예상 질문

**Q. Redis 캐싱 전략에 대해 설명해주세요.**
> A. 대표적인 전략으로 Look-Aside, Write-Through, Write-Behind가 있습니다.
> Look-Aside는 조회 시 캐시를 먼저 확인하고 없으면 DB에서 읽어 캐시에 저장하는 방식으로,
> 가장 일반적으로 사용됩니다. 캐시 장애 시 DB 직접 조회로 폴백이 가능한 장점이 있습니다.
> Write-Through는 쓰기 시 DB와 캐시를 동시에 업데이트해 일관성이 높지만 쓰기 비용이 늘어납니다.
> Write-Behind는 캐시에 먼저 쓰고 배치로 DB에 반영해 쓰기 성능이 좋지만 장애 시 데이터 유실 위험이 있습니다.
> 실무에서는 상품 조회처럼 읽기가 많은 곳엔 Look-Aside를, 조회수 카운터처럼 빈번한 쓰기엔 Write-Behind를 활용했습니다.

**Q. Cache Stampede가 무엇이고 어떻게 해결하나요?**
> A. 캐시 TTL 만료 직후 수많은 요청이 동시에 DB로 몰려 과부하가 발생하는 현상입니다.
> 해결 방법으로는 분산 락으로 하나의 스레드만 DB를 조회하고 나머지는 대기시키거나,
> TTL에 랜덤값을 더해 만료 시간을 분산시키는 방법, 만료 전에 미리 캐시를 갱신하는 방법이 있습니다.

---

## 4. 분산 락 (Redisson)

### 분산 락이 필요한 이유

```
[문제 상황 - 재고 차감]
재고: 1개
서버 A: 재고 조회(1) → 재고 차감 → DB 저장(0)
서버 B: 재고 조회(1) → 재고 차감 → DB 저장(0)
→ 동시에 2명이 재고 1개를 구매해버림 (중복 구매 발생)

[단일 서버라면?]
synchronized 키워드로 해결 가능
But MSA / 멀티 인스턴스 환경에서는 synchronized가 다른 서버에 적용 안 됨
→ 분산 락이 필요
```

### Redisson을 사용하는 이유

Redis에서 분산 락을 구현할 때 `SETNX` 명령어로 직접 구현할 수도 있지만,
**Redisson 라이브러리**를 사용하면 더 안전하고 간편하게 구현할 수 있다.

```
SETNX 직접 구현의 문제점:
1. 락 타임아웃 설정이 복잡함
2. 워치독(Watchdog) 없어서 작업 중 락 만료 위험
3. 락 해제 시 원자성 보장 어려움

Redisson 장점:
1. Lock 인터페이스 제공 (자바 표준 방식)
2. 워치독(Watchdog): 작업 중에 자동으로 락 연장
3. Pub/Sub 기반 대기 → 스핀 락보다 효율적
4. tryLock, fairLock, readWriteLock 등 다양한 락 지원
```

### Redisson 설정

```gradle
// build.gradle
implementation 'org.redisson:redisson-spring-boot-starter:3.23.5'
```

```java
// RedissonConfig.java
@Configuration
public class RedissonConfig {

    @Value("${spring.redis.host}")
    private String host;

    @Value("${spring.redis.port}")
    private int port;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer()
              .setAddress("redis://" + host + ":" + port);
        return Redisson.create(config);
    }
}
```

### 기본 분산 락 구현

```java
@Service
public class StockService {

    private final RedissonClient redissonClient;
    private final ProductRepository productRepository;

    @Transactional
    public void decreaseStock(Long productId, int quantity) {
        String lockKey = "lock:stock:" + productId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 락 획득 시도: 최대 5초 대기, 락 유지 시간 3초
            boolean acquired = lock.tryLock(5, 3, TimeUnit.SECONDS);

            if (!acquired) {
                throw new IllegalStateException("재고 처리 중입니다. 잠시 후 다시 시도해주세요.");
            }

            // --- 임계 구역(Critical Section) ---
            Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

            if (product.getStock() < quantity) {
                throw new OutOfStockException("재고가 부족합니다.");
            }

            product.decreaseStock(quantity);
            productRepository.save(product);
            // --- 임계 구역 끝 ---

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("락 획득 중 인터럽트 발생", e);
        } finally {
            // 락을 가진 스레드만 해제 (안전한 해제)
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
```

### AOP 기반 분산 락 (어노테이션 방식)

```java
// 어노테이션 정의
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {
    String key();
    long waitTime() default 5;
    long leaseTime() default 3;
    TimeUnit timeUnit() default TimeUnit.SECONDS;
}

// AOP 구현
@Aspect
@Component
public class DistributedLockAspect {

    private final RedissonClient redissonClient;

    @Around("@annotation(distributedLock)")
    public Object lock(ProceedingJoinPoint joinPoint, DistributedLock distributedLock)
            throws Throwable {

        String lockKey = distributedLock.key();
        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean acquired = lock.tryLock(
                distributedLock.waitTime(),
                distributedLock.leaseTime(),
                distributedLock.timeUnit()
            );

            if (!acquired) {
                throw new LockAcquisitionException("락 획득 실패: " + lockKey);
            }

            return joinPoint.proceed();

        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}

// 사용 예시
@Service
public class CouponService {

    @DistributedLock(key = "'coupon:issue:' + #couponId")
    public void issueCoupon(Long couponId, Long userId) {
        // 쿠폰 발급 처리 (동시에 한 스레드만 실행)
    }
}
```

### 워치독(Watchdog) 동작 원리

```
[Watchdog]
- leaseTime을 지정하지 않으면 Redisson이 자동으로 30초 TTL 설정
- 작업이 진행 중인 경우 10초마다 자동으로 TTL을 30초로 갱신 (자동 연장)
- 작업이 완료되어 lock.unlock() 호출 시 Watchdog도 종료

[leaseTime 지정 시]
- Watchdog 동작 안 함
- 지정한 시간 초과 시 자동 해제
- 데드락 방지에 유리하지만 작업 시간 예측 필요
```

### 면접 예상 질문

**Q. Redis 분산 락을 사용한 경험이 있나요? Redisson을 왜 사용했나요?**
> A. 멀티 인스턴스 환경에서 재고 차감 시 중복 구매가 발생하는 문제를 해결하기 위해
> Redisson 기반 분산 락을 사용했습니다.
> SETNX로 직접 구현하는 방법도 있지만, Redisson은 Watchdog 기능으로 작업 중 락 만료를 방지하고
> Pub/Sub 기반으로 대기해서 스핀 락보다 효율적입니다.
> tryLock으로 최대 대기 시간을 설정하고, finally 블록에서 isHeldByCurrentThread()로
> 락 보유 여부 확인 후 안전하게 해제하는 패턴을 사용했습니다.

**Q. synchronized와 Redisson 분산 락의 차이는 무엇인가요?**
> A. synchronized는 단일 JVM 내에서만 동기화가 가능합니다.
> 하지만 MSA나 서버를 여러 대 띄우는 환경에서는 각 서버마다 독립적인 JVM이 동작하기 때문에
> synchronized로는 서버 간 동기화가 불가능합니다.
> Redisson 분산 락은 Redis 서버를 중심으로 락을 관리하므로 여러 서버가 동일한 락을 공유할 수 있어
> MSA 환경에서도 동시성 문제를 해결할 수 있습니다.

---

## 5. 세션 저장소 활용

### 기존 세션 방식의 문제

```
[단일 서버 - 문제 없음]
클라이언트 → 서버 A (세션 저장) → OK

[멀티 서버 - 문제 발생]
클라이언트 → 로드밸런서
                → 서버 A (세션 있음) → OK
                → 서버 B (세션 없음) → 로그인 필요 ← 문제!
```

### Redis 세션 저장소로 해결

```
[Redis 세션]
클라이언트 → 로드밸런서
                → 서버 A → Redis (세션 조회) → OK
                → 서버 B → Redis (세션 조회) → OK (서버에 관계없이 동일 세션)
```

### Spring Session + Redis 설정

```gradle
// build.gradle
implementation 'org.springframework.boot:spring-boot-starter-data-redis'
implementation 'org.springframework.session:spring-session-data-redis'
```

```java
// application.yml
spring:
  session:
    store-type: redis
    timeout: 30m           # 세션 만료 30분
  redis:
    host: localhost
    port: 6379
```

```java
// 설정 클래스
@Configuration
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 1800) // 30분
public class RedisSessionConfig {
    // Spring Session이 자동으로 HttpSession → Redis 연동
}
```

```java
// 실제 사용 - 기존 HttpSession과 동일하게 사용
@RestController
public class AuthController {

    @PostMapping("/login")
    public ResponseEntity<String> login(
            @RequestBody LoginRequest request,
            HttpSession session) {

        Member member = memberService.login(request.getEmail(), request.getPassword());

        // 세션에 저장 (내부적으로 Redis에 저장됨)
        session.setAttribute("loginMember", new LoginMemberDto(member.getId(), member.getEmail()));
        session.setMaxInactiveInterval(1800); // 30분

        return ResponseEntity.ok("로그인 성공");
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpSession session) {
        session.invalidate(); // Redis에서 세션 삭제
        return ResponseEntity.ok("로그아웃 성공");
    }

    @GetMapping("/my")
    public ResponseEntity<MyPageDto> myPage(HttpSession session) {
        LoginMemberDto loginMember =
            (LoginMemberDto) session.getAttribute("loginMember");

        if (loginMember == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(memberService.getMyPage(loginMember.getId()));
    }
}
```

### Redis에 실제로 저장되는 형태

```
# Redis에 저장된 세션 키 구조
spring:session:sessions:{sessionId}            # 세션 데이터 Hash
spring:session:sessions:expires:{sessionId}   # 세션 만료 시각
spring:session:expirations:{만료시각}           # 만료 인덱스

# HGETALL 로 조회 예시
HGETALL "spring:session:sessions:abc123"
1) "maxInactiveInterval"      → 1800
2) "lastAccessedTime"         → 1709123456789
3) "sessionAttr:loginMember"  → {직렬화된 LoginMemberDto}
```

### 세션 vs JWT 비교

| 구분 | 세션 (Redis) | JWT |
|------|------------|-----|
| 저장 위치 | 서버(Redis) | 클라이언트(쿠키/로컬스토리지) |
| 강제 로그아웃 | 즉시 가능 (세션 삭제) | 어려움 (블랙리스트 필요) |
| 서버 부하 | Redis 조회 필요 | 토큰 검증만 (DB 조회 없음) |
| 확장성 | Redis 중앙화 필요 | 무상태 → 확장 쉬움 |
| 보안 | 세션 하이재킹 주의 | 토큰 탈취 주의 |
| 적합한 경우 | 관리자 강제 로그아웃 필요 | 마이크로서비스, 모바일 API |

### 면접 예상 질문

**Q. Redis를 세션 저장소로 사용한 이유와 방법을 설명해주세요.**
> A. 서버를 여러 대 운영하는 환경에서 기존 서버 메모리 세션은 로드밸런서에 의해 다른 서버로
> 요청이 분산될 때 세션을 찾지 못하는 문제가 있습니다.
> Redis를 공유 세션 저장소로 사용하면 어떤 서버로 요청이 가더라도 동일한 세션을 조회할 수 있습니다.
> Spring Session Data Redis를 사용하면 기존 HttpSession API 그대로 사용하면서
> 내부적으로 Redis에 세션이 저장됩니다.
> 직접 경험으로는 YPT 프로젝트에서 멀티 인스턴스 환경을 고려해 Redis 세션을 적용한 바 있습니다.

**Q. Redis 세션과 JWT 중 어떤 것을 선택하시겠어요?**
> A. 상황에 따라 다르게 선택합니다.
> 관리자가 특정 사용자를 즉시 강제 로그아웃시켜야 하는 기능이 필요하거나,
> 서버 사이드 렌더링 기반 웹 서비스라면 Redis 세션이 적합합니다.
> 반면 MSA 환경에서 여러 서비스가 인증을 공유해야 하거나, 모바일 앱처럼 상태를 서버에 저장하지 않아야 한다면
> JWT가 더 적합합니다. 저는 현재 프로젝트에서 JWT를 기본으로 사용하되,
> Refresh Token을 Redis에 저장해 강제 로그아웃과 토큰 갱신을 처리하는 하이브리드 방식을 사용했습니다.

---

## 6. Redis 운영 및 장애 대응

### TTL (Time To Live) 설정 전략

```java
// TTL 기준 설계
public class CacheTtlConfig {

    // 변경이 거의 없는 데이터 → 긴 TTL
    public static final long CATEGORY_TTL_HOURS = 24;       // 카테고리 24시간

    // 자주 조회되지만 가끔 변경되는 데이터 → 중간 TTL
    public static final long PRODUCT_TTL_MINUTES = 30;      // 상품 30분

    // 자주 변경되는 데이터 → 짧은 TTL
    public static final long STOCK_TTL_MINUTES = 5;         // 재고 5분

    // 세션 → 사용자 활동 기준
    public static final long SESSION_TTL_MINUTES = 30;      // 세션 30분 (비활성 기준)

    // 인증 토큰 → 보안 정책 기준
    public static final long ACCESS_TOKEN_TTL_MINUTES = 30;
    public static final long REFRESH_TOKEN_TTL_DAYS = 14;
}
```

### 메모리 관리 및 Eviction Policy

```
# redis.conf - 메모리 초과 시 삭제 정책
maxmemory 512mb
maxmemory-policy allkeys-lru   # 전체 키 중 LRU(가장 오래된) 삭제 → 캐시 용도
maxmemory-policy volatile-lru  # TTL 있는 키 중 LRU 삭제 → 세션 + 캐시 혼합 시
maxmemory-policy noeviction    # 삭제 안 함, 메모리 부족 시 에러 → 주요 데이터 저장 시
```

```java
// 키 네이밍 컨벤션 (운영 중요)
// 형식: {서비스}:{도메인}:{식별자}:{속성}
"product:detail:1001"           // 상품 상세 캐시
"product:ranking:weekly"        // 주간 랭킹
"user:session:abc123"           // 사용자 세션
"lock:stock:1001"               // 재고 분산 락
"coupon:issued:SAVE10"          // 쿠폰 발급 목록
```

### Redis 장애 상황 대응

**패턴 1: 캐시 장애 시 DB 폴백 (Look-Aside의 장점)**
```java
public ProductDto getProduct(Long productId) {
    try {
        String key = "product:" + productId;
        ProductDto cached = (ProductDto) redisTemplate.opsForValue().get(key);
        if (cached != null) return cached;

        // 캐시 MISS → DB 조회 및 캐시 저장
        ProductDto dto = loadFromDb(productId);
        redisTemplate.opsForValue().set(key, dto, 30, TimeUnit.MINUTES);
        return dto;

    } catch (RedisConnectionException e) {
        // Redis 장애 시 DB에서 직접 조회 (서비스 중단 방지)
        log.error("Redis 연결 실패, DB에서 직접 조회합니다. productId: {}", productId, e);
        return loadFromDb(productId);
    }
}
```

**패턴 2: Circuit Breaker (Spring Cloud Resilience4j)**
```java
@Service
public class ProductService {

    @CircuitBreaker(name = "redis", fallbackMethod = "getProductFallback")
    public ProductDto getProduct(Long productId) {
        // Redis 조회 시도
    }

    // Redis 장애 시 DB 직접 조회
    public ProductDto getProductFallback(Long productId, Exception e) {
        return ProductDto.from(productRepository.findById(productId).orElseThrow());
    }
}
```

### Redis Replication & Sentinel (운영 환경)

```
[단일 Redis - 개발/소규모]
Client → Redis (Master)

[Redis Sentinel - 장애 자동 복구]
                    ┌─ Redis Slave (복제본)
Client → Sentinel ─┤
                    └─ Redis Master → 장애 시 자동 Failover

[Redis Cluster - 대용량/분산]
Shard 1: Master1 + Slave1
Shard 2: Master2 + Slave2
Shard 3: Master3 + Slave3
→ 데이터를 여러 샤드에 분산 저장
```

### 면접 예상 질문

**Q. Redis를 운영할 때 주의해야 할 점은 무엇인가요?**
> A. 몇 가지 중요한 포인트가 있습니다.
> 첫째, TTL 설정입니다. 데이터 특성에 맞게 적절한 만료 시간을 설정해야 합니다.
> 변경이 잦은 재고 데이터는 짧게, 카테고리 같은 정적 데이터는 길게 설정합니다.
> 둘째, 메모리 관리입니다. maxmemory와 eviction policy를 명시적으로 설정해야 합니다.
> 캐시 목적이라면 allkeys-lru, 세션처럼 중요 데이터가 있다면 volatile-lru를 사용합니다.
> 셋째, 장애 대응입니다. Look-Aside 패턴은 Redis 장애 시 DB로 폴백이 가능해 서비스 연속성을 보장합니다.
> 운영 환경에서는 Redis Sentinel로 장애 자동 복구를 설정하는 것이 중요합니다.

---

## 7. Redis vs Memcached

### 비교

| 구분 | Redis | Memcached |
|------|-------|-----------|
| 자료구조 | String, Hash, List, Set, ZSet 등 다양 | String만 지원 |
| 영속성 | RDB, AOF로 데이터 유지 가능 | 없음 (재시작 시 모두 삭제) |
| 복제 | Replication 지원 | 없음 |
| 클러스터 | Redis Cluster 지원 | 클라이언트 사이드 샤딩 |
| Pub/Sub | 지원 | 미지원 |
| 멀티스레드 | 싱글 스레드 (Redis 6.0부터 I/O 멀티스레드) | 멀티스레드 |
| 단순 캐싱 속도 | 빠름 | 약간 더 빠름 (단순 캐싱만 비교 시) |
| 사용 케이스 | 캐시 + 세션 + 랭킹 + 분산락 등 다목적 | 단순 캐시 전용 |

### 실무에서 Redis를 선택하는 이유

```
1. 다양한 자료구조: 캐싱뿐만 아니라 랭킹(ZSet), 세션(Hash), 대기열(List) 등 활용 가능
2. 영속성: 재시작해도 데이터 유지 (RDB/AOF)
3. Replication: 마스터-슬레이브 구성으로 고가용성 확보
4. Pub/Sub: 간단한 메시지 브로커 역할 가능
5. 분산 락: Redisson 등 생태계가 풍부
```

### 면접 예상 질문

**Q. Redis와 Memcached의 차이를 설명해주세요.**
> A. 둘 다 인메모리 캐시 저장소지만 Redis가 훨씬 다양한 기능을 제공합니다.
> Memcached는 String 자료구조와 단순 캐싱만 지원하고 영속성이 없어 재시작 시 데이터가 모두 사라집니다.
> Redis는 Hash, List, Set, ZSet 등 다양한 자료구조를 지원하고, RDB/AOF로 영속성을 보장하며,
> Replication, Pub/Sub, 분산 락까지 지원합니다.
> 단순 캐시 성능만 보면 Memcached가 약간 우세하다는 의견도 있지만,
> 실무에서는 다양한 요구사항을 한 곳에서 해결할 수 있는 Redis를 대부분 선택합니다.

---

## 전체 면접 빈출 질문 모음

### Redis 기본

**Q1. Redis가 빠른 이유는 무엇인가요?**
> A. 크게 4가지 이유가 있습니다.
> 첫째, 모든 데이터를 RAM에 저장하기 때문에 디스크 I/O가 없습니다. RAM은 SSD보다 약 1,000배 빠릅니다.
> 둘째, 단순한 키-값 구조로 해시맵에서 O(1)으로 바로 조회하기 때문에 연산 자체가 가볍습니다.
> 셋째, 싱글 스레드로 동작해 Context Switching과 Lock 경합 비용이 없습니다.
> 넷째, I/O 멀티플렉싱으로 싱글 스레드지만 수천 개의 클라이언트 연결을 이벤트 루프 방식으로 처리합니다.

**Q2. Redis의 영속성 옵션(RDB, AOF)에 대해 설명해주세요.**
> A. RDB는 특정 시점의 스냅샷을 파일로 저장하는 방식입니다.
> 복구 속도가 빠르고 파일 크기가 작지만, 스냅샷 사이에 데이터가 변경되면 유실될 수 있습니다.
> AOF는 모든 쓰기 명령을 로그 파일에 순서대로 기록하는 방식입니다.
> 데이터 유실이 거의 없지만 파일이 커지고 복구 속도가 느립니다.
> 실무에서는 RDB + AOF를 함께 사용해 빠른 복구와 데이터 안전성을 동시에 확보합니다.

**Q3. Redis가 싱글 스레드인데 왜 빠른가요?**
> A. 싱글 스레드이기 때문에 오히려 빠른 측면이 있습니다.
> 멀티스레드에서 발생하는 Context Switching 비용, Lock 경합, 동기화 오버헤드가 전혀 없습니다.
> 또한 모든 명령이 순서대로 원자적으로 실행되어 INCR 같은 연산도 Race Condition 없이 안전합니다.
> I/O 멀티플렉싱으로 단일 스레드로도 수천 개의 클라이언트 요청을 동시에 처리할 수 있습니다.

---

### 자료구조

**Q4. Redis 자료구조 종류와 각각의 사용 케이스를 설명해주세요.**
> A. 대표적인 5가지가 있습니다.
> String은 가장 기본적인 타입으로 캐시, 카운터, 세션 토큰에 사용합니다. INCR 명령으로 원자적 카운터를 구현할 수 있어 재고 차감이나 조회수에 활용합니다.
> Hash는 필드:값 구조로 객체를 저장합니다. 회원 정보나 장바구니 관리에 적합합니다.
> List는 양방향 연결 리스트로 최근 본 상품 목록, 알림 큐에 사용합니다. LTRIM으로 크기를 제한할 수 있습니다.
> Set은 중복 없는 집합으로 좋아요, 쿠폰 중복 발급 방지에 사용합니다. 교집합·합집합 연산도 지원합니다.
> ZSet(Sorted Set)은 score 기준으로 자동 정렬되는 집합으로 랭킹, 선착순 대기열 구현에 사용합니다.

**Q5. ZSet(Sorted Set)을 어떻게 활용할 수 있나요?**
> A. score를 기준으로 자동 정렬되는 자료구조입니다.
> 이커머스에서 주간 인기 상품 랭킹 구현에 활용할 수 있습니다. 상품이 판매될 때마다 ZINCRBY로 score를 증가시키고, ZREVRANGE로 상위 N개를 조회합니다.
> 선착순 이벤트 대기열에도 활용합니다. 입장 시각을 score로 사용해 먼저 들어온 순서대로 처리할 수 있습니다.
> ZREVRANK로 특정 상품의 실시간 순위도 O(log N)으로 빠르게 조회할 수 있습니다.

---

### 캐싱

**Q6. Redis 캐싱 전략(Look-Aside, Write-Through, Write-Behind)을 설명해주세요.**
> A. Look-Aside는 조회 시 캐시를 먼저 확인하고 없으면 DB에서 읽어 캐시에 저장하는 방식입니다. 가장 일반적으로 사용되며, Redis 장애 시 DB로 폴백이 가능한 장점이 있습니다.
> Write-Through는 쓰기 시 DB와 캐시를 동시에 업데이트합니다. 데이터 일관성이 높지만 쓰기 비용이 증가합니다.
> Write-Behind는 캐시에 먼저 쓰고 배치로 DB에 반영합니다. 쓰기 성능이 가장 좋지만 장애 시 데이터 유실 위험이 있습니다.
> 실무에서는 상품 조회처럼 읽기가 많은 곳엔 Look-Aside를, 조회수·좋아요처럼 빈번한 쓰기엔 Write-Behind를 사용합니다.

**Q7. Cache Stampede 문제와 해결 방법을 설명해주세요.**
> A. 캐시 TTL 만료 직후 수많은 요청이 동시에 DB로 몰려 과부하가 발생하는 현상입니다.
> 인기 상품 캐시가 만료되는 순간 수천 개의 요청이 모두 DB에 쿼리를 날리는 상황이 대표적입니다.
> 해결 방법은 세 가지입니다. 첫째, 분산 락으로 하나의 스레드만 DB를 조회하고 나머지는 캐시가 올라올 때까지 대기시킵니다. 둘째, TTL에 랜덤값을 더해 여러 서버의 캐시 만료 시간을 분산시킵니다. 셋째, TTL이 얼마 남지 않았을 때 비동기로 미리 캐시를 갱신하는 Early Recomputation 방식을 사용합니다.

**Q8. 캐시 만료(TTL)를 어떻게 설계하시나요?**
> A. 데이터 변경 빈도와 비즈니스 중요도에 따라 다르게 설정합니다.
> 카테고리처럼 거의 변경되지 않는 데이터는 24시간, 상품 상세처럼 가끔 변경되는 데이터는 30분, 재고처럼 자주 변경되는 데이터는 5분으로 짧게 설정합니다.
> 세션은 사용자 비활성 기준 30분, Refresh Token은 보안 정책에 따라 2주로 설정합니다.
> Cache Stampede 방지를 위해 동일한 데이터를 여러 서버에 캐시할 때는 TTL에 랜덤값(±10%)을 더해 만료 시간을 분산시킵니다.

---

### 분산 락

**Q9. Redis 분산 락을 왜 사용하나요? synchronized와 차이는?**
> A. synchronized는 단일 JVM 내에서만 동기화가 가능합니다.
> MSA나 서버를 여러 대 운영하는 환경에서는 각 서버가 독립적인 JVM을 가지기 때문에 synchronized로는 서버 간 동기화가 불가능합니다.
> Redis 분산 락은 Redis 서버를 중심으로 락을 관리하므로 여러 서버가 동일한 락을 공유할 수 있습니다.
> 예를 들어 재고가 1개 남은 상품을 두 서버에서 동시에 차감할 때, 분산 락 없이는 중복 구매가 발생할 수 있습니다. Redisson으로 분산 락을 잡으면 하나의 스레드만 임계 구역에 진입하도록 보장할 수 있습니다.

**Q10. Redisson Watchdog이 무엇인가요?**
> A. Redisson에서 제공하는 락 자동 연장 기능입니다.
> leaseTime을 지정하지 않으면 Redisson이 기본 30초 TTL을 설정하고, 작업이 진행 중인 동안 10초마다 TTL을 30초로 자동 갱신합니다.
> 이 덕분에 작업 시간이 예상보다 길어져도 락이 중간에 만료되어 다른 스레드가 진입하는 문제를 방지합니다.
> lock.unlock()을 호출해 락을 해제하면 Watchdog도 자동으로 종료됩니다.
> leaseTime을 명시적으로 지정하면 Watchdog은 동작하지 않고 지정한 시간이 지나면 자동 해제됩니다.

---

### 세션

**Q11. Redis를 세션 저장소로 사용하는 이유는?**
> A. 서버를 여러 대 운영하는 환경에서 세션 불일치 문제를 해결하기 위해서입니다.
> 기존 서버 메모리 세션은 로드밸런서가 요청을 다른 서버로 분산할 때 해당 서버에 세션이 없으면 로그인이 풀리는 문제가 발생합니다.
> Redis를 공유 세션 저장소로 사용하면 어떤 서버로 요청이 가더라도 동일한 세션을 조회할 수 있습니다.
> Spring Session Data Redis를 사용하면 기존 HttpSession API 그대로 사용하면서 내부적으로는 Redis에 저장됩니다.

**Q12. Redis 세션과 JWT 중 어떤 것을 선택하시겠어요?**
> A. 상황에 따라 다르게 선택합니다.
> 관리자가 특정 사용자를 즉시 강제 로그아웃시켜야 하는 기능이 필요하거나, 서버 사이드 렌더링 기반 웹 서비스라면 Redis 세션이 적합합니다.
> 반면 MSA 환경에서 여러 서비스가 인증을 공유해야 하거나, 모바일 앱처럼 무상태(Stateless)가 중요한 경우 JWT가 더 적합합니다.
> 실무에서는 Access Token은 JWT로 발급하고, Refresh Token은 Redis에 저장해 강제 로그아웃과 토큰 갱신을 처리하는 하이브리드 방식을 선호합니다.

---

### 운영

**Q13. Redis 메모리가 부족해지면 어떻게 되나요?**
> A. maxmemory-policy 설정에 따라 다르게 동작합니다.
> noeviction(기본값)이면 새로운 쓰기 요청에 에러를 반환해 서비스 장애로 이어질 수 있습니다.
> allkeys-lru이면 전체 키 중 가장 오래 사용되지 않은 키를 자동으로 삭제합니다. 캐시 용도로 사용할 때 권장됩니다.
> volatile-lru이면 TTL이 설정된 키 중에서만 LRU로 삭제합니다. 세션과 캐시를 함께 저장할 때 세션은 보호하고 캐시만 지우고 싶을 때 사용합니다.
> 운영에서는 maxmemory를 명시적으로 설정하고, 메모리 사용량 모니터링 알림을 걸어두는 것이 중요합니다.

**Q14. Redis 장애 시 서비스가 중단되지 않으려면 어떻게 해야 하나요?**
> A. 두 가지 측면에서 대비해야 합니다.
> 아키텍처 측면에서는 Redis Sentinel을 사용해 마스터 장애 시 슬레이브가 자동으로 마스터로 승격되는 Failover를 설정합니다. 대용량 환경에서는 Redis Cluster로 데이터를 분산 저장합니다.
> 코드 측면에서는 Look-Aside 패턴을 사용하면 Redis가 다운되어도 DB에서 직접 조회해 서비스를 유지할 수 있습니다. try-catch로 RedisConnectionException을 잡아 DB 폴백 로직을 구현하거나, Resilience4j Circuit Breaker를 사용해 Redis 장애를 자동으로 감지하고 폴백 메서드로 전환합니다.

**Q15. Redis와 Memcached의 차이는 무엇인가요?**
> A. 둘 다 인메모리 캐시 저장소지만 Redis가 훨씬 다양한 기능을 제공합니다.
> Memcached는 String 자료구조만 지원하고 영속성이 없어 재시작 시 데이터가 모두 사라집니다.
> Redis는 Hash, List, Set, ZSet 등 다양한 자료구조를 지원하고, RDB/AOF로 영속성을 보장하며, Replication, Pub/Sub, 분산 락까지 지원합니다.
> 단순 캐시 성능만 보면 Memcached가 약간 우세하다는 의견도 있지만, 실무에서는 다양한 요구사항을 한 곳에서 해결할 수 있는 Redis를 대부분 선택합니다.

---

> 각 섹션 학습 완료 시 기술면접_공부플랜.md 체크리스트에 표시할 것
