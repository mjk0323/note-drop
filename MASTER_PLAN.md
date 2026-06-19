# NoteDrop 마스터 아키텍처 계획서

**버전:** 1.0.0 | **작성일:** 2026-06-19
**기술 스택:** Spring Boot 4.1.0 / Java 21 / MySQL / Redis

---

## 1. 프로젝트 아키텍처

### 패키지 구조

```
com.notedrop.notedrop
├── controller/
│   ├── AuthController.java
│   ├── CoffeeBeanController.java
│   ├── ProductController.java
│   ├── FlashSaleController.java
│   ├── OrderController.java
│   ├── ReviewController.java
│   ├── RecommendationController.java
│   └── admin/
│       ├── AdminProductController.java
│       └── AdminFlashSaleController.java
├── service/
│   ├── AuthService.java
│   ├── CoffeeBeanService.java
│   ├── ProductService.java
│   ├── FlashSaleService.java          # Redis DECR + Redisson Lock 핵심
│   ├── OrderService.java
│   ├── ReviewService.java
│   ├── TasteProfileService.java       # 가중 평균으로 TasteProfile 갱신
│   └── RecommendationService.java     # 인메모리 코사인 유사도 계산
├── repository/
│   ├── UserRepository.java
│   ├── CoffeeBeanRepository.java      # BETWEEN 파생 쿼리 포함
│   ├── ProductRepository.java
│   ├── FlashSaleEventRepository.java
│   ├── OrderRepository.java
│   ├── ReviewRepository.java
│   └── TasteProfileRepository.java
├── entity/
│   ├── BaseEntity.java
│   ├── User.java
│   ├── TasteProfile.java
│   ├── CoffeeBean.java
│   ├── Product.java
│   ├── FlashSaleEvent.java
│   ├── Order.java
│   └── Review.java
├── dto/
│   ├── request/
│   └── response/
├── config/
│   ├── SecurityConfig.java
│   ├── JwtConfig.java
│   ├── RedisConfig.java
│   ├── DataSourceConfig.java          # Master/Replica 라우팅
│   ├── AsyncConfig.java               # 플래시 세일 큐 소비용 ThreadPool
│   └── CacheConfig.java
├── common/
│   ├── enums/
│   │   ├── UserRole.java              # GUEST, MEMBER, ADMIN
│   │   ├── OrderStatus.java           # PENDING, CONFIRMED, CANCELLED, REFUNDED
│   │   ├── ProductStatus.java         # ACTIVE, SOLD_OUT, HIDDEN
│   │   └── FlashSaleStatus.java       # SCHEDULED, ACTIVE, ENDED, CANCELLED
│   ├── response/
│   │   └── ApiResponse.java           # {success, data, message, code}
│   └── util/
│       └── VectorUtils.java           # 코사인 유사도 계산 (5차원)
└── exception/
    ├── GlobalExceptionHandler.java
    ├── BusinessException.java
    └── ErrorCode.java                 # STOCK_EXHAUSTED, LOCK_ACQUISITION_FAILED 등
```

### 요청 흐름

```
클라이언트 HTTP 요청
    │
    ▼
[JwtAuthenticationFilter]          ← JWT 검증 → SecurityContext 저장
    │
    ▼
[Controller]                       ← @Valid DTO + @PreAuthorize 권한 체크
    │
    ├─ [일반 조회] ─────────────────────────────────────────────────────┐
    │   @Transactional(readOnly=true)                                   │
    │   @Cacheable → Redis HIT: 즉시 반환                               │
    │              → Redis MISS: Replica DB → 캐시 저장 후 반환         │
    │                                                                   ▼
    ├─ [추천 쿼리] ─────────────────────────────────── [Replica DB]
    │   TasteProfile 로드 → BETWEEN 범위 쿼리로 후보 원두 조회
    │   → Java 인메모리 코사인 유사도 정렬 → 상위 N개 반환
    │
    └─ [플래시 세일 주문] ──────────────────────────────────────────────┐
        ① Rate Limit 체크 (Redis 슬라이딩 윈도우)                        │
        ② Redis DECR flash_sale:{eventId}:stock                         │
           → 결과 < 0: INCR 롤백 → 409 STOCK_EXHAUSTED                  │
        ③ Redisson tryLock(waitTime=0, leaseTime=5s)                    │
           → 실패 처리: 아래 '락 획득 실패 처리 전략' 참조               │
        ④ idempotency_key 중복 체크 (UNIQUE 인덱스가 최종 방어선)         │
        ⑤ RPUSH flash_sale:{eventId}:queue → 202 Accepted 반환          │
        ⑥ [AsyncOrderConsumer] BLPOP → Master DB INSERT                 │
                                                                        ▼
    ▼                                                           [Master DB]
[Repository] (JpaRepository)
```

### 수평 확장 근거

- **무상태 JWT**: 서버 측 세션 없음 → 인스턴스 간 상태 동기화 불필요
- **공유 재고 카운터**: `flash_sale:{eventId}:stock` Redis 단일 진실의 원천
- **분산 락**: Redisson으로 복수 인스턴스에서의 동시 주문 직렬화 보장
- **읽기/쓰기 분리**: `@Transactional(readOnly=true)` → Replica 자동 라우팅

---

## 2. 도메인 모델 & DB 스키마

### 엔티티 관계

```
User (1) ──── (1) TasteProfile
User (1) ──── (N) Order
User (1) ──── (N) Review

CoffeeBean (1) ── (N) Product
CoffeeBean (1) ── (N) Review

Product (1) ───── (N) Order
Product (1) ───── (1) FlashSaleEvent  [is_flash_sale=true]

FlashSaleEvent (1) ── (N) Order
```

### 주요 엔티티 & 인덱스

#### BaseEntity
```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {
    @CreatedDate  private LocalDateTime createdAt;
    @LastModifiedDate private LocalDateTime updatedAt;
}
```

#### users
| 컬럼 | 타입 | 제약 |
|------|------|------|
| id | BIGINT | PK |
| email | VARCHAR(255) | UNIQUE NOT NULL |
| password | VARCHAR(255) | NOT NULL (BCrypt) |
| nickname | VARCHAR(100) | NOT NULL |
| role | ENUM('GUEST','MEMBER','ADMIN') | DEFAULT 'GUEST' |

인덱스: `UNIQUE idx_users_email (email)`

#### taste_profiles
| 컬럼 | 타입 | 설명 |
|------|------|------|
| user_id | BIGINT | UNIQUE FK → users.id |
| acidity_score | DECIMAL(3,1) | 0.0~5.0 |
| bitterness_score | DECIMAL(3,1) | 0.0~5.0 |
| sweetness_score | DECIMAL(3,1) | 0.0~5.0 |
| body_score | DECIMAL(3,1) | 0.0~5.0 |
| aroma_score | DECIMAL(3,1) | 0.0~5.0 |
| preferred_origins | JSON | 표시용 (참고만) |
| review_count | INT | 가중 평균 계산용 |

> **JSON 대신 개별 컬럼 이유**: BETWEEN 범위 인덱스 적용 가능, 인메모리 코사인 계산 시 타입 안전한 접근

인덱스: `UNIQUE idx_taste_profiles_user_id (user_id)`

#### coffee_beans
| 컬럼 | 타입 |
|------|------|
| origin | VARCHAR(100) |
| processing_method | ENUM('WASHED','NATURAL','HONEY','ANAEROBIC') |
| roast_level | ENUM('LIGHT','MEDIUM_LIGHT','MEDIUM','MEDIUM_DARK','DARK') |
| acidity_score ~ aroma_score | DECIMAL(3,1) × 5개 |

인덱스:
- `idx_coffee_beans_origin (origin)`
- `idx_coffee_beans_roast_level (roast_level)`

#### products
| 컬럼 | 타입 |
|------|------|
| coffee_bean_id | BIGINT FK |
| price | DECIMAL(10,2) |
| stock | INT |
| is_flash_sale | TINYINT(1) |
| status | ENUM('ACTIVE','SOLD_OUT','HIDDEN') |

인덱스: `idx_products_status_flash_sale (status, is_flash_sale)`

#### flash_sale_events
| 컬럼 | 타입 |
|------|------|
| product_id | BIGINT FK |
| total_quantity | INT |
| remaining_quantity | INT (DB 원장, Redis와 동기화) |
| sale_price | DECIMAL(10,2) |
| starts_at / ends_at | DATETIME(6) |
| status | ENUM('SCHEDULED','ACTIVE','ENDED','CANCELLED') |

인덱스: `idx_flash_sale_events_status_starts_at (status, starts_at)`

#### orders
| 컬럼 | 타입 |
|------|------|
| user_id / product_id / flash_sale_event_id | BIGINT FK |
| quantity | INT |
| total_price | DECIMAL(10,2) |
| status | ENUM('PENDING','CONFIRMED','CANCELLED','REFUNDED') |
| idempotency_key | VARCHAR(36) UNIQUE |

인덱스:
- `UNIQUE idx_orders_idempotency_key (idempotency_key)` — 중복 주문 DB 레벨 최종 차단
- `idx_orders_user_id_created_at (user_id, created_at DESC)`

#### reviews
| 컬럼 | 타입 |
|------|------|
| user_id / product_id / coffee_bean_id | BIGINT FK |
| acidity~aroma_rating | DECIMAL(3,1) × 5개 |
| overall_rating | DECIMAL(3,1) |
| content | TEXT |

인덱스: `UNIQUE idx_reviews_user_product (user_id, product_id)`

---

## 3. 기술 스택 통합 가이드

### MySQL

#### Master–Replica 라우팅
```java
// AbstractRoutingDataSource → readOnly 트랜잭션이면 Replica 선택
protected Object determineCurrentLookupKey() {
    return TransactionSynchronizationManager.isCurrentTransactionReadOnly()
        ? "replica" : "master";
}
```
`@Transactional(readOnly=true)` 사용 근거: dirty checking 비활성화 + MVCC 오버헤드 감소 + Replica 자동 라우팅

#### HikariCP 튜닝
```yaml
master.hikari:
  maximum-pool-size: 20      # CPU 코어 * 2 + 1 (8코어 기준)
  connection-timeout: 3000
  max-lifetime: 1800000      # MySQL wait_timeout 이하 설정
  leak-detection-threshold: 5000

replica.hikari:
  maximum-pool-size: 30      # 읽기 트래픽 > 쓰기 트래픽
```

#### Flyway 마이그레이션
```
src/main/resources/db/migration/
├── V1__create_users_taste_profiles.sql
├── V2__create_coffee_beans.sql
├── V3__create_products.sql
├── V4__create_flash_sale_events.sql
├── V5__create_orders.sql
└── V6__create_reviews.sql
```

---

### Redis

#### 키 패턴

| 키 패턴 | 타입 | TTL | 용도 |
|--------|------|-----|------|
| `flash_sale:{eventId}:stock` | String | 이벤트 종료 시 | 재고 원자적 카운터 |
| `flash_sale:{eventId}:queue` | List | 이벤트 + 24h | 주문 큐 (RPUSH/BLPOP) |
| `lock:order:{userId}` | String (Redisson) | 5초 | 분산 락 |
| `rate_limit:{userId}:{window}` | ZSet | 60초 | 슬라이딩 윈도우 |
| `cache:product:{id}` | String (JSON) | 30분 | 상품 캐시 |
| `cache:recommendation:{userId}` | String (JSON) | 10분 | 추천 결과 캐시 |

#### 재고 카운터: Redis DECR
```java
Long remaining = redisTemplate.opsForValue()
    .decrement("flash_sale:" + eventId + ":stock");
if (remaining < 0) {
    redisTemplate.opsForValue().increment("flash_sale:" + eventId + ":stock"); // 롤백
    throw new FlashSaleException(ErrorCode.STOCK_EXHAUSTED);
}
```
> Redis 단일 스레드 이벤트 루프 → DECR은 원자적. `SELECT ... FOR UPDATE` 대비 인메모리 처리로 처리량 수십 배 우위.

#### 분산 락: Redisson + 락 획득 실패 처리 전략

```java
RLock lock = redissonClient.getLock("lock:order:" + userId);
boolean acquired = lock.tryLock(0, 5, TimeUnit.SECONDS); // waitTime=0: 즉시 실패 반환

if (!acquired) {
    // 락 획득 실패 처리 — 두 가지 케이스 구분
    String existingStatus = redisTemplate.opsForValue()
        .get("order_status:" + userId + ":" + idempotencyKey);

    if (existingStatus != null) {
        // [케이스 1] 동일 idempotencyKey가 이미 처리 중
        // → 현재 상태를 그대로 반환 (멱등성 보장), 202 Accepted
        return OrderStatusResponse.of(existingStatus);
    }
    // [케이스 2] 다른 요청이 락 점유 중 (신규 요청)
    // → 503 Service Unavailable + Retry-After: 3 헤더
    // → 클라이언트는 동일 idempotencyKey로 3초 후 재시도
    throw new FlashSaleException(ErrorCode.LOCK_ACQUISITION_FAILED);
}
```

**클라이언트 재시도 규칙 (503 수신 시)**
- 동일 `idempotencyKey` 유지 필수 (멱등성)
- 최대 3회 / 지수 백오프: 1s → 2s → 4s
- 3회 초과 실패 시 클라이언트 측에서 주문 실패 처리

**큐 vs 즉시 거절 구분**

| 상황 | 처리 | HTTP 상태 |
|------|------|-----------|
| 재고 소진 | 즉시 거절 (재시도 무의미) | 409 CONFLICT |
| 동일 요청 처리 중 (케이스 1) | 기존 상태 반환 | 202 ACCEPTED |
| 다른 요청 락 점유 (케이스 2) | 거절 + Retry-After | 503 SERVICE_UNAVAILABLE |
| 분당 요청 초과 | 즉시 거절 | 429 TOO_MANY_REQUESTS |

#### 주문 큐: RPUSH + BLPOP
```java
// 삽입 (FlashSaleService)
redisTemplate.opsForList()
    .rightPush("flash_sale:" + eventId + ":queue", orderEventJson);

// 소비 (AsyncOrderConsumer — @Scheduled fixedDelay=100ms)
String event = redisTemplate.opsForList()
    .leftPop("flash_sale:" + eventId + ":queue", 5, TimeUnit.SECONDS);
if (event != null) orderService.persistOrder(parse(event));
```
> 10,000 동시 요청을 큐에서 평탄화 → DB 커넥션 풀 포화 방지. 클라이언트는 202 후 폴링으로 결과 확인.

#### 레이트 리밋: Redis 슬라이딩 윈도우 (Lua 스크립트)
```lua
-- 원자적 실행
ZADD rate_limit:{userId} currentTime currentTime
ZREMRANGEBYSCORE rate_limit:{userId} 0 (currentTime - 60000)
local count = ZCARD rate_limit:{userId}
EXPIRE rate_limit:{userId} 60
if count > 5 then return 1 else return 0 end
```

---

### 추천 시스템: MySQL + 인메모리 코사인 유사도

벡터 DB 없이 현재 MySQL 스키마만으로 구현한다.

#### 데이터 접근 전략 (2단계)

**1단계 — MySQL BETWEEN 범위 사전 필터 (Spring Data JPA 파생 쿼리)**
```java
// CoffeeBeanRepository.java
List<CoffeeBean> findByStatusAndAcidityScoreBetweenAndSweetnessBetween(
    BeanStatus status,
    BigDecimal acidityMin, BigDecimal acidityMax,
    BigDecimal sweetnessMin, BigDecimal sweetnessMax
);
// 범위: 사용자 선호 점수 ±1.5 → 후보군 축소 (전체 스캔 방지)
// 인덱스: idx_coffee_beans_taste_scores 활용
```

**2단계 — Java 인메모리 코사인 유사도 정렬**
```java
// VectorUtils.java
public static double cosineSimilarity(double[] user, double[] bean) {
    double dot = 0, normU = 0, normB = 0;
    for (int i = 0; i < user.length; i++) {
        dot += user[i] * bean[i];
        normU += user[i] * user[i];
        normB += bean[i] * bean[i];
    }
    return (normU == 0 || normB == 0) ? 0 : dot / (Math.sqrt(normU) * Math.sqrt(normB));
}

// RecommendationService.java
double[] userVec = {profile.getAcidityScore(), profile.getBitternessScore(),
                    profile.getSweetnessScore(), profile.getBodyScore(), profile.getAromaScore()};

return candidates.stream()
    .map(bean -> new ScoredBean(bean, VectorUtils.cosineSimilarity(userVec, bean.toVector())))
    .sorted(Comparator.comparingDouble(ScoredBean::score).reversed())
    .limit(limit)
    .map(RecommendationResponse::from)
    .toList();
```

> 원두 수가 수천 건 이내인 초기 단계에서는 BETWEEN 필터 후 인메모리 정렬로 충분.
> 데이터 규모 증가 시 pgvector/Milvus 등 벡터 DB로 마이그레이션 경로 열어둠.

---

### 추가 의존성 (build.gradle)

```groovy
// 인증/보안
implementation 'org.springframework.boot:spring-boot-starter-security'
implementation 'org.springframework.boot:spring-boot-starter-validation'
implementation 'io.jsonwebtoken:jjwt-api:0.12.6'
runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.12.6'
runtimeOnly 'io.jsonwebtoken:jjwt-jackson:0.12.6'

// Redis
implementation 'org.springframework.boot:spring-boot-starter-data-redis'
implementation 'org.springframework.session:spring-session-data-redis'
implementation 'org.redisson:redisson-spring-boot-starter:3.34.1'

// DB 마이그레이션
implementation 'org.flywaydb:flyway-mysql'

// 모니터링
implementation 'org.springframework.boot:spring-boot-starter-actuator'
implementation 'io.micrometer:micrometer-registry-prometheus'
```

---

## 4. 구현 로드맵

### Phase 1 — 기반 구축 & 추천 시스템 (4~6주)

#### Week 1-2: 인프라 기반
- `application.yml` — Master/Replica DataSource, Redis 연결, JPA(ddl-auto: validate)
- Flyway V1~V6 마이그레이션 파일 작성
- `BaseEntity` + 모든 엔티티 클래스 + `@EnableJpaAuditing`
- `RoutingDataSource` (Master/Replica 자동 라우팅)
- Spring Security + JWT: `JwtTokenProvider`, `JwtAuthenticationFilter`, `SecurityConfig`
- `POST /api/auth/signup`, `POST /api/auth/login`

#### Week 3-4: 핵심 도메인 CRUD
- CoffeeBean/Product CRUD API
- `POST /api/reviews` → `TasteProfileService.updateProfile()`:
  ```
  newScore = (existingScore * reviewCount + newRating) / (reviewCount + 1)
  ```
- Redis 상품 목록/상세 캐시 적용 (`@Cacheable`)

#### Week 5-6: 추천 시스템
- `CoffeeBeanRepository` BETWEEN 파생 쿼리 구현
- `VectorUtils.cosineSimilarity()` 구현
- `GET /api/recommendations` → `@Cacheable("recommendation")` TTL 10분
- 추천 흐름: TasteProfile 로드 → BETWEEN 필터 → 인메모리 코사인 정렬 → 상위 10개

---

### Phase 2 — 플래시 세일 트래픽 제어 (3~4주)

#### Week 1: 이벤트 관리 & 재고 사전 적재
- `POST /api/admin/flash-sales` — FlashSaleEvent 생성 (status: SCHEDULED)
- `@Scheduled(fixedRate=60000)` 시작 스케줄러:
  - `status=SCHEDULED AND starts_at <= NOW()` 조회
  - Redis `SET flash_sale:{id}:stock {totalQuantity}` 사전 적재
  - status → ACTIVE
- 종료 스케줄러: Redis 재고 ↔ DB `remaining_quantity` 동기화 후 status → ENDED

#### Week 2: 주문 API 핵심 구현
```
POST /api/flash-sales/orders 처리 순서:
① 이벤트 유효성 검사 (ACTIVE + 시간 범위)
② Redis 슬라이딩 윈도우 Rate Limit (분당 5회)
③ Redis DECR → 재고 소진 시 409 즉시 반환
④ Redisson tryLock(waitTime=0) → 실패 시 케이스 분기 (위 전략 참조)
⑤ idempotency_key 중복 체크
⑥ RPUSH queue → 202 Accepted 반환
⑦ [비동기] AsyncOrderConsumer BLPOP → DB INSERT
```
- `AsyncConfig`: `ThreadPoolTaskExecutor` — corePoolSize=10, maxPoolSize=50
- 주문 상태 임시 저장: `order_status:{userId}:{idempotencyKey}` → Redis String TTL 10분

#### Week 3: 부하 테스트
```bash
# k6 시나리오: 10,000 VU, 이벤트 재고 100개
# 검증 목표
- orders_confirmed == 100  (정확히 재고만큼)
- orders_oversell == 0
- p95 응답 시간 < 500ms
- http_req_failed < 0.1%
```

#### Week 4: 통합 테스트
- Testcontainers (MySQL + Redis) 기반 `FlashSaleIntegrationTest`
- 100 스레드 동시 주문 → 재고 10개 → 확정 주문 정확히 10개 검증

---

### Phase 3 — 고도화 (지속)

- **추천 고도화**: 리뷰 데이터 충분 시 User-User 협업 필터링 Hybrid (콘텐츠 기반 70% + 협업 30%)
- **실시간 재고 SSE**: `GET /api/flash-sales/{eventId}/inventory/stream` (text/event-stream)
- **관리자 분석 API**: 판매 요약, 상위 원두, 취향 분포
- **Docker Compose**: app / mysql-master / mysql-replica / redis / prometheus / grafana
- **AWS 배포**: ECS Fargate + RDS MySQL(Multi-AZ) + ElastiCache Redis + ALB + Secrets Manager
- **Observability**: Actuator + Micrometer 커스텀 메트릭 → Grafana 대시보드

---

## 5. 핵심 기술 결정 요약

| 영역 | 결정 | 이유 |
|------|------|------|
| 재고 제어 | Redis DECR (원자적) | DB `SELECT FOR UPDATE` 대비 인메모리 처리로 경합 없이 수만 건/초 처리 |
| 락 실패 처리 | tryLock(waitTime=0) + 케이스 분기 | 동일 요청 재시도(멱등성)와 신규 충돌을 구분하여 클라이언트에 정확한 피드백 제공 |
| 중복 주문 방지 | Redisson 락(1차) + idempotency_key UNIQUE 인덱스(2차) | 이중 방어: 락은 직렬화, 인덱스는 예외 상황 최종 차단 |
| 트래픽 평탄화 | Redis List 큐 + 비동기 소비자 | 스파이크 흡수 + DB 커넥션 풀 보호, 202 패턴으로 UX 유지 |
| 추천 구현 | MySQL BETWEEN 파생 쿼리 + Java 인메모리 코사인 유사도 | 별도 벡터 DB 없이 현재 스키마로 구현 가능. 원두 수 수천 건 이내에서 충분한 성능 |
| 추천 스키마 | Taste 점수 개별 DECIMAL 컬럼 | JSON 컬럼은 BETWEEN 인덱스 불가. 개별 컬럼으로 사전 필터링 인덱스 활용 가능 |
| 인증 | JWT Stateless + Spring Security | 수평 확장 시 세션 동기화 불필요. 액세스(1h) + 리프레시(7d) 이중 구조 |
| DB 읽기 분리 | AbstractRoutingDataSource + Replica | 추천 조회를 Replica로 분리 → 플래시 세일 쓰기 Master 자원 보호 |
| 레이트 리밋 | Redis 슬라이딩 윈도우 (Lua) | 고정 윈도우의 경계 2배 문제 해결. Lua로 원자적 처리 |
| DB 마이그레이션 | Flyway | SQL 기반 직관적 버전 관리. 환경 간 스키마 일관성 보장 |

---

## 부록: API 엔드포인트

```
# 인증 (공개)
POST /api/auth/signup
POST /api/auth/login          → {accessToken, refreshToken}
POST /api/auth/refresh

# 원두 (공개)
GET  /api/beans               ?origin=&roastLevel=&page=&size=
GET  /api/beans/{id}

# 상품 (공개, 캐시)
GET  /api/products
GET  /api/products/{id}

# 추천 (인증 필요)
GET  /api/recommendations

# 플래시 세일 (인증 필요)
GET  /api/flash-sales/active
POST /api/flash-sales/orders   → 202 Accepted + {orderId, status: PENDING}
GET  /api/flash-sales/{eventId}/inventory/stream  [Phase 3 SSE]

# 주문 (인증 필요)
GET  /api/orders
GET  /api/orders/{id}

# 리뷰 (인증 필요)
POST /api/reviews              → TasteProfile 자동 갱신
GET  /api/reviews?productId=

# 관리자 (ADMIN)
POST /api/admin/beans
POST /api/admin/products
POST /api/admin/flash-sales
PUT  /api/admin/flash-sales/{id}
GET  /api/admin/analytics/**   [Phase 3]
```

## 부록: 환경 변수

```bash
DB_MASTER_HOST= / DB_MASTER_USER= / DB_MASTER_PASSWORD=
DB_REPLICA_HOST= / DB_REPLICA_USER= / DB_REPLICA_PASSWORD=
REDIS_HOST= / REDIS_PORT=6379 / REDIS_PASSWORD=
JWT_SECRET=          # 최소 32바이트 Base64
JWT_ACCESS_EXPIRY=3600000    # 1시간 (ms)
JWT_REFRESH_EXPIRY=604800000 # 7일 (ms)
SPRING_PROFILES_ACTIVE=local|dev|prod
```
