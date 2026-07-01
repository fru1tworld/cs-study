# Signals — Traces, Metrics, Logs

---

## 목차

1. [Signal이란?](#signal이란)
2. [세 가지 핵심 시그널](#세-가지-핵심-시그널)
3. [Resource — 누가 보낸 데이터인가](#resource--누가-보낸-데이터인가)
4. [InstrumentationScope — 어느 라이브러리가 만든 데이터인가](#instrumentationscope--어느-라이브러리가-만든-데이터인가)
5. [Attributes — 속성](#attributes--속성)
6. [Context — 시그널을 묶어주는 끈](#context--시그널을-묶어주는-끈)
7. [시그널 간 상관관계](#시그널-간-상관관계)
8. [Profiles — 네 번째 시그널](#profiles--네-번째-시그널)

---

## Signal이란?

Signal은 **OpenTelemetry가 다루는 한 종류의 텔레메트리** 를 의미합니다. 각 시그널은 다음을 가집니다:

- 고유한 **데이터 모델** (Span / DataPoint / LogRecord)
- 고유한 **API** (Tracer / Meter / Logger)
- 고유한 **SDK 동작** (Sampling, Aggregation, Batching)
- **공유되는 부분**: Resource, Context Propagation, OTLP

같은 Resource·Context 위에서 동작하기 때문에 서로 자연스럽게 상관됩니다.

---

## 세 가지 핵심 시그널

### Traces (분산 트레이싱)

**한 요청이 여러 서비스를 거치는 흐름** 을 기록.

- 단위: **Span** — 하나의 작업 (HTTP 요청 처리, DB 쿼리 등)
- 한 요청 = 하나의 **Trace** = 여러 Span의 트리/DAG
- 핵심 질문: "이 요청은 왜 느린가?", "어디서 에러가 났는가?"

### Metrics (메트릭)

**시간에 따른 수치 측정** 을 집계해서 기록.

- 단위: **DataPoint** — `(시간, 값, attributes)`
- Instrument 종류: Counter, UpDownCounter, Gauge, Histogram
- 핵심 질문: "초당 요청 수는?", "P99 지연은?", "에러율은?"

### Logs (로그)

**이벤트의 텍스트 기반 기록**.

- 단위: **LogRecord** — `(timestamp, severity, body, attributes)`
- 기존 로깅 라이브러리(`logback`, `slf4j`, `python logging`, `winston`)와 통합 가능
- 핵심 질문: "이 시점에 무슨 일이 있었는가?"

세 시그널의 가장 큰 차이는 **카디널리티 비용**:

| 시그널 | 데이터당 비용 | 카디널리티 영향 |
|--------|---------------|-----------------|
| Trace  | 한 요청당 다수 Span (샘플링 필수) | 낮음 (요청 단위) |
| Metric | 시간당 하나의 집계 값 | 매우 높음 (라벨 조합마다 시계열 생성) |
| Log    | 이벤트당 하나 | 중간 |

---

## Resource — 누가 보낸 데이터인가

**Resource** 는 **시그널을 생성한 엔티티** 를 식별합니다. 모든 시그널(Trace/Metric/Log)에 동일한 Resource가 붙습니다.

표준 attribute 예시 (Semantic Conventions):

```yaml
service.name: "checkout"          # 필수
service.namespace: "ecommerce"
service.instance.id: "pod-abc123"
service.version: "1.4.2"
deployment.environment: "production"

host.name: "ip-10-0-1-23"
host.id: "i-0abc..."
os.type: "linux"

k8s.cluster.name: "prod-east"
k8s.namespace.name: "shop"
k8s.pod.name: "checkout-7d4f..."
k8s.container.name: "app"

cloud.provider: "aws"
cloud.region: "us-east-1"

process.pid: 1234
process.runtime.name: "openjdk"
process.runtime.version: "21.0.1"
```

### 왜 중요한가

- 같은 Resource를 공유하는 Trace·Metric·Log는 **자동으로 같은 서비스의 데이터로 묶임**
- 백엔드는 Resource를 키로 인덱싱 → "service.name=checkout"으로 모든 시그널을 한 번에 조회
- **Resource Detector** 가 자동으로 채움: K8s downward API, EC2 metadata, GCE metadata 등

### 주의할 점

- `service.name` 은 **필수**. 없으면 SDK가 `unknown_service` 로 채움
- Resource는 **프로세스 시작 시 한 번 결정** 되고 변경되지 않음
- Resource attribute는 모든 시그널에 복제되므로 **요청별로 변하는 값을 넣으면 안 됨** (그건 Span attribute)

---

## InstrumentationScope — 어느 라이브러리가 만든 데이터인가

Resource보다 한 단계 안쪽 식별자. **어느 계측 라이브러리가 이 시그널을 만들었는가** 를 나타냅니다.

```
Resource (service: checkout, pod: pod-abc)
 └── Scope (name: io.opentelemetry.spring-webmvc, version: 2.10.0)
      └── Spans / Metrics / Logs
 └── Scope (name: io.opentelemetry.jdbc, version: 2.10.0)
      └── Spans / Metrics / Logs
 └── Scope (name: my.app.business, version: 1.0.0)
      └── Spans / Metrics / Logs
```

용도:
- 특정 라이브러리의 계측만 끄고 싶을 때 식별자
- 라이브러리 버전별 동작 차이 추적
- "이 Span은 자동 계측인가, 수동 계측인가" 구분

`Tracer` / `Meter` / `Logger` 를 가져올 때 이름·버전을 지정:

```python
tracer = trace.get_tracer("my.app.business", "1.0.0")
meter = metrics.get_meter("my.app.business", "1.0.0")
```

---

## Attributes — 속성

**Attribute** 는 시그널에 붙는 `key → value` 쌍입니다. 모든 시그널이 attribute를 가집니다.

### 값 타입

표준이 허용하는 값 타입:
- string
- bool
- int (signed 64-bit)
- double
- 위 타입의 **homogeneous array** (string[], int[], ...)

`null` 값은 **속성이 없는 것** 과 동일하게 취급.

### Semantic Conventions

OpenTelemetry는 잘 알려진 attribute에 **표준 이름** 을 정의합니다. 예:

```
http.request.method            # "GET" | "POST" | ...
http.response.status_code      # 200, 404, ...
url.full                       # "https://api.example.com/v1/orders"
url.path                       # "/v1/orders"
server.address                 # "api.example.com"
server.port                    # 443

db.system                      # "postgresql" | "mysql" | ...
db.namespace                   # database name
db.query.text                  # "SELECT ..."

messaging.system               # "kafka" | "rabbitmq"
messaging.destination.name     # topic name
```

표준을 따르면 백엔드 대시보드·알림이 자동으로 동작합니다 (Tempo의 service map, Datadog APM의 endpoint 분석 등).

### 카디널리티 주의

특히 Metric attribute에 **요청 ID, user ID, full URL 같은 고유값** 을 넣으면 시계열이 폭발합니다.

```
# 나쁨: 모든 사용자별로 시계열이 생김
http_requests_total{user_id="u-12345"} 1
http_requests_total{user_id="u-12346"} 1
...

# 좋음: 의미 있는 카테고리로 묶음
http_requests_total{route="/orders/:id", status="200"} 1234
```

Trace/Log는 카디널리티에 덜 민감하지만 (이벤트마다 한 번만 기록), Metric은 **시계열 수 = 라벨 조합 수** 가 됩니다.

---

## Context — 시그널을 묶어주는 끈

**Context** 는 현재 실행 흐름에 묶인 값들의 컨테이너입니다. 핵심은 두 가지:

### 1. SpanContext

현재 활성 Span의 식별자 — TraceID, SpanID, TraceFlags, TraceState.

이 정보가 있어야:
- 자식 Span이 부모를 알 수 있음
- HTTP/gRPC 호출 시 헤더로 전파 가능
- Log·Metric에 trace_id를 자동으로 첨부 가능

### 2. Baggage

요청 전체에 걸쳐 전파되는 **사용자 정의 key/value** (예: `user.tier=premium`).

```
Baggage 헤더로 전파되며, 모든 다운스트림 서비스에서 읽을 수 있음.
주의: Baggage는 외부로 노출되므로 PII나 민감정보 넣지 말 것.
```

Context는 언어별로 구현됩니다:
- Go: `context.Context`
- Python: `contextvars`
- Java: `io.opentelemetry.context.Context` (ThreadLocal 기반)
- Node.js: AsyncLocalStorage

---

## 시그널 간 상관관계

OpenTelemetry의 강점은 **세 시그널이 자연스럽게 연결** 된다는 점입니다.

### Trace ↔ Log

LogRecord에는 `trace_id` 와 `span_id` 필드가 있어, 활성 Span이 있을 때 자동으로 채워집니다.

```json
{
  "timestamp": "2026-05-08T12:34:56Z",
  "severity": "ERROR",
  "body": "payment failed: insufficient funds",
  "trace_id": "5b8aa5a2d2c873e8a6a9f5c7d2e0f1a3",
  "span_id": "00f067aa0ba902b7",
  "attributes": { "user.id": "u-12345" }
}
```

→ Grafana에서 Span을 클릭하면 같은 trace_id를 가진 로그가 자동으로 떠오름.

### Trace ↔ Metric (Exemplars)

Histogram 같은 집계 메트릭에 **대표 샘플의 trace_id** 를 함께 기록 ("Exemplar"). P99에 해당하는 실제 요청의 trace를 클릭으로 찾아갈 수 있음.

```
http_server_duration_seconds_bucket{le="0.5"} 1234
                                  exemplar=trace_id:abc123, span_id:def456, value=0.487
```

### Resource로 묶임

세 시그널 모두 같은 `service.name`, `pod.name`, `host.name` 을 가지므로 백엔드가 같은 엔티티의 데이터로 인식.

---

## Profiles — 네 번째 시그널

2025년 이후 실험적으로 도입된 시그널.

- **CPU/Heap 프로파일링 데이터** 를 OTLP로 전송
- 형식: pprof 호환
- 소스: eBPF (Pyroscope, Parca), 언어 SDK (async-profiler 연동)
- 백엔드: Grafana Pyroscope, Polar Signals

"어디서 시간을 썼는가"를 함수 단위로 깊이 보여주며, continuous profiling이 OTLP 생태계로 통합되는 단계입니다.


---

## 참고 자료

- Signals 개요: https://opentelemetry.io/docs/concepts/signals/
- Resource: https://opentelemetry.io/docs/specs/otel/resource/sdk/
- Semantic Conventions: https://github.com/open-telemetry/semantic-conventions
- Profiles: https://opentelemetry.io/docs/specs/otel/profiles/
