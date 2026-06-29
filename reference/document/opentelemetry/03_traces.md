# Traces — 분산 트레이싱

---

## 목차

1. [Trace와 Span](#trace와-span)
2. [Span의 구조](#span의-구조)
3. [Span Kind](#span-kind)
4. [Span Status](#span-status)
5. [Events와 Links](#events와-links)
6. [Context Propagation](#context-propagation)
7. [W3C Trace Context](#w3c-trace-context)
8. [Sampling](#sampling)
9. [실전 패턴](#실전-패턴)

---

## Trace와 Span

### Trace

**한 요청의 전체 흐름**. 여러 서비스를 거치는 동안 발생하는 모든 작업의 집합.

식별자: **TraceID** — 16바이트 (128bit) 16진수 문자열.

```
trace_id = 5b8aa5a2d2c873e8a6a9f5c7d2e0f1a3
```

### Span

**Trace를 구성하는 단위 작업**. 한 함수 호출, 한 HTTP 요청, 한 DB 쿼리 등.

식별자: **SpanID** — 8바이트 (64bit).

```
span_id = 00f067aa0ba902b7
```

Span은 서로 부모-자식 관계를 가져 트리(또는 DAG)를 구성합니다:

```
Trace 5b8aa5a2...
└── Span A (HTTP POST /checkout)               [service: gateway]
    ├── Span B (auth check)                    [service: auth]
    ├── Span C (validate cart)                 [service: cart]
    │   └── Span D (SELECT FROM cart)          [service: cart]
    └── Span E (process payment)               [service: payment]
        ├── Span F (POST stripe API)           [service: payment]
        └── Span G (INSERT INTO orders)        [service: orders]
```

부모 식별: 각 Span은 `parent_span_id` 를 가집니다 (root는 없음).

---

## Span의 구조

OTLP Span의 주요 필드:

```protobuf
message Span {
  bytes trace_id;              // 16 bytes
  bytes span_id;               // 8 bytes
  bytes parent_span_id;        // 8 bytes (optional)
  string trace_state;          // W3C tracestate

  string name;                 // 작업 이름 ("GET /users/:id")
  SpanKind kind;               // SERVER | CLIENT | INTERNAL | PRODUCER | CONSUMER

  fixed64 start_time_unix_nano;
  fixed64 end_time_unix_nano;

  repeated KeyValue attributes;
  repeated Event events;       // 시간이 찍힌 이벤트
  repeated Link links;         // 다른 Trace/Span 참조

  Status status;               // OK | ERROR
}
```

### Span name

**카디널리티가 낮은 패턴** 으로 작성:

```
좋음: "GET /users/:id"
나쁨: "GET /users/12345"           ← user id마다 새 이름

좋음: "SELECT users"
나쁨: "SELECT * FROM users WHERE id=12345"  ← 매번 새 이름
```

라이브러리 자동 계측은 보통 라우트 패턴(`/users/:id`)을 추출해 사용합니다.

### Start/End time

- 정밀도: **나노초**
- 시작은 Span 생성 시, 종료는 `span.end()` 호출 시
- **반드시 end를 호출** 해야 export됨 → 비동기 코드에서 누락 주의

---

## Span Kind

Span이 **요청-응답 관계의 어느 쪽** 인지 표시. 백엔드가 service map을 그릴 때 사용.

| Kind | 의미 | 예 |
|------|------|-----|
| `INTERNAL` | 같은 서비스 내부 작업 (기본값) | 함수 호출, 비즈니스 로직 |
| `SERVER` | 들어오는 요청을 처리 | HTTP 핸들러, gRPC 서버 메서드 |
| `CLIENT` | 다른 서비스로 나가는 요청 | HTTP 클라이언트 호출, DB 쿼리 |
| `PRODUCER` | 메시지를 큐에 넣음 | Kafka producer.send() |
| `CONSUMER` | 큐에서 메시지를 꺼냄 | Kafka consumer.poll() |

서비스 경계는 보통 `CLIENT → SERVER` 짝으로 표현됩니다.

```
[Service A] CLIENT span "POST /api/orders"
                ↓ HTTP + traceparent header
[Service B] SERVER span "POST /api/orders"
            └── INTERNAL span "validate"
            └── CLIENT span "SELECT orders"
                    ↓
[PostgreSQL] (보통 계측되지 않음)
```

---

## Span Status

Span의 결과 상태. 세 가지 값:

- `UNSET` (기본값) — 명시적으로 설정되지 않음
- `OK` — 명시적으로 성공
- `ERROR` — 실패

**HTTP 상태 코드 4xx/5xx 가 자동으로 ERROR가 되지는 않습니다.** Semantic Convention이 정합니다:

- HTTP 5xx (server) → ERROR
- HTTP 4xx (server) → 보통 UNSET (클라이언트 잘못이지 서버 에러는 아님)
- HTTP 4xx/5xx (client) → ERROR (호출자 입장에서는 실패)

수동 계측 시:

```python
try:
    do_work()
except Exception as e:
    span.set_status(Status(StatusCode.ERROR, str(e)))
    span.record_exception(e)   # exception을 event로 기록
    raise
```

---

## Events와 Links

### Event

**Span 내부에서 시간이 찍힌 점(point-in-time) 이벤트**. "이 Span 처리 중에 무슨 일이 있었나"를 기록.

```python
span.add_event("cache.miss", attributes={"key": "user:12345"})
span.add_event("retry", attributes={"attempt": 2})
```

대표적 사용:
- **Exception 기록** — `record_exception()` 이 내부적으로 event를 추가 (`exception.type`, `exception.message`, `exception.stacktrace`)
- **상태 전이** — "queue.acquired", "lock.released"
- **마일스톤** — "headers.received", "body.parsed"

Event는 Span을 쪼개지 않고 한 Span 안에서 작은 사건을 표현하는 가벼운 방법입니다.

### Link

**다른 Trace/Span을 참조**. 부모-자식 관계가 아닌 "관련된" 관계.

대표 사용 사례:

#### 1. 배치 처리

여러 메시지를 모아서 한 번에 처리할 때, 각 메시지의 producer Span에 Link를 거는 형태.

```
[Producer A] message_1, trace_id=A
[Producer B] message_2, trace_id=B

[Consumer] batch_process Span
           ├── link → message_1 (trace A)
           ├── link → message_2 (trace B)
```

부모-자식으로 연결했다면 부모가 둘이 되어 트리가 깨집니다. Link는 트리 구조를 유지하면서 다중 참조를 표현합니다.

#### 2. Async 작업

요청 처리는 끝났지만 백그라운드에서 이어지는 작업이 원래 요청과 관련 있음을 기록.

---

## Context Propagation

서비스 간에 Trace를 연결하는 핵심 메커니즘.

### 흐름

```
[Service A]
  현재 Context = SpanContext(trace_id=T, span_id=S_A)
  HTTP 요청 보내기 직전:
    Propagator.inject(Context, headers)
      → headers["traceparent"] = "00-T-S_A-01"
  HTTP request →

[Service B]
  HTTP 요청 받음:
    Propagator.extract(headers) → SpanContext(trace_id=T, span_id=S_A)
  새 Span 생성:
    parent_span_id = S_A
    span_id = S_B (새로 생성)
    trace_id = T (그대로 사용)
```

### Propagator

OpenTelemetry는 여러 propagator 포맷을 지원:

- **W3C Trace Context** (기본값, 권장) — `traceparent`, `tracestate` 헤더
- **W3C Baggage** — `baggage` 헤더
- **B3** — Zipkin 호환 (`b3`, `x-b3-traceid`, ...)
- **Jaeger** — `uber-trace-id`
- **AWS X-Ray** — `X-Amzn-Trace-Id`

여러 포맷을 동시에 inject/extract 가능하도록 **Composite Propagator** 를 구성.

```python
# 기본 SDK 설정 — 보통 자동
set_global_textmap(
    CompositePropagator([
        TraceContextTextMapPropagator(),
        W3CBaggagePropagator(),
    ])
)
```

### 자동 계측

대부분 라이브러리 자동 계측이 inject/extract를 처리합니다:
- HTTP 클라이언트(requests, axios, OkHttp) → 요청 전 inject
- HTTP 서버(Express, Spring, FastAPI) → 핸들러 진입 시 extract
- gRPC, Kafka, RabbitMQ도 동일 패턴

수동 호출이 필요한 경우는 비표준 전송을 사용하거나 직접 구현한 RPC 정도입니다.

---

## W3C Trace Context

가장 중요한 표준 — 두 헤더로 구성됩니다.

### traceparent

```
traceparent: 00-5b8aa5a2d2c873e8a6a9f5c7d2e0f1a3-00f067aa0ba902b7-01
             │  │                                │                │
             │  └── trace-id (16 bytes hex)      │                └── trace-flags
             │                                   └── parent-id (8 bytes hex)
             └── version (현재 "00")
```

`trace-flags` 의 비트:
- `0x01` (sampled) — 이 trace는 export될 예정
- 나머지는 reserved

### tracestate

벤더별 추가 정보를 전달. 콤마 구분의 `key=value` 리스트, 키는 소문자/숫자/`-_*/` 만, 32엔트리·512바이트 제한.

```
tracestate: vendorA=opaque-string,vendorB=other-data
```

OpenTelemetry SDK 자체는 tracestate를 거의 채우지 않지만, **샘플링 결정** 이나 **벤더 호환** 에 사용됩니다.

---

## Sampling

모든 요청을 저장하면 비용이 급증합니다 → **일부만 선택해서 저장**. 두 가지 큰 전략:

### Head Sampling

**Span을 만들 때 즉시 결정**. SDK 안에서 동작.

장점: 단순, 저비용. 만들지 않은 Span은 비용이 0.

단점: 결정 시점에 정보가 부족. 에러 트레이스가 우연히 누락될 수 있음.

#### 대표 Sampler

```
AlwaysOn / AlwaysOff
TraceIdRatioBased(0.1)            # trace_id 기반 10%
ParentBased(...)                  # 부모 결정 따라감
```

`ParentBased(TraceIdRatioBased(0.1))` 가 흔한 조합:
- 부모 Span이 sampled면 자식도 sampled (trace 일관성)
- root Span(부모 없음)이면 10% 확률로 sampled

### Tail Sampling

**Trace가 끝난 뒤 전체를 보고 결정**. Collector가 수행.

장점: 에러 trace, 느린 trace를 우선 보존 가능.

단점: 모든 Span을 Collector에 우선 수집해야 함 → 네트워크·메모리 비용. 같은 trace의 모든 Span이 동일한 Collector 인스턴스에 모여야 함 (보통 Load Balancing exporter로 trace_id hash 라우팅).

#### Collector 설정 예

```yaml
processors:
  tail_sampling:
    decision_wait: 10s
    num_traces: 50000
    policies:
      - name: errors
        type: status_code
        status_code: { status_codes: [ERROR] }
      - name: slow
        type: latency
        latency: { threshold_ms: 1000 }
      - name: random
        type: probabilistic
        probabilistic: { sampling_percentage: 5 }
```

### Sampling이 trace_flags에 미치는 영향

Head sampling 결정은 `traceparent`의 `trace-flags`에 sampled 비트로 전파되어 → 다운스트림이 일관되게 sampled/dropped됩니다.

Tail sampling은 Collector 내부에서 처리되므로 와이어 포맷에는 영향을 주지 않습니다.

---

## 실전 패턴

### 1. 라이브러리 자동 계측을 먼저 활성화

대부분의 가치는 자동 계측에서 나옵니다. 수동 계측은 **비즈니스 의미가 있는 작업**에만 추가합니다.

```python
# 좋은 수동 Span 후보
with tracer.start_as_current_span("apply_discount") as span:
    span.set_attribute("discount.code", code)
    span.set_attribute("discount.amount", amount)
```

### 2. 의미 있는 attribute를 풍부하게

```python
span.set_attribute("user.tier", user.tier)            # premium/free
span.set_attribute("cart.item_count", len(items))
span.set_attribute("payment.provider", provider)
```

→ 백엔드에서 attribute 기반 필터링·집계를 효과적으로 활용할 수 있습니다.

### 3. 카디널리티 폭발 주의

trace에 user_id 자체는 OK (저장 비용은 trace당). 다만 **trace 검색 인덱스에 user_id 가 인덱싱되는지** 백엔드별 확인 필요.

### 4. span.end() 누락 방지

```python
# 안전: context manager로 자동 end
with tracer.start_as_current_span("work"):
    do_work()

# 위험: end 호출을 잊거나 예외로 누락
span = tracer.start_span("work")
do_work()  # 예외 시 span.end() 안 됨
span.end()
```

---

## 참고 자료

- Trace API/SDK: https://opentelemetry.io/docs/specs/otel/trace/
- W3C Trace Context: https://www.w3.org/TR/trace-context/
- Sampling: https://opentelemetry.io/docs/concepts/sampling/
- Tail Sampling Processor: https://github.com/open-telemetry/opentelemetry-collector-contrib/tree/main/processor/tailsamplingprocessor
