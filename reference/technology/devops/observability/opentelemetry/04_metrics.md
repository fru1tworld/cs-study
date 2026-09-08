# Metrics

<a id="metric의-데이터-모델"></a>
## Metric의 데이터 모델

OpenTelemetry Metric의 단위는 `DataPoint`다. 측정 시각, attribute, 측정값을 함께 담는다.

```
(timestamp, attributes, value)
```

같은 attribute 조합을 가진 DataPoint를 시간순으로 모으면 시계열(Time Series)이 된다. 아래 예에서는 HTTP 메서드와 응답 코드가 달라지면 같은 메트릭에서도 별도의 시계열이 만들어진다.

```
http_requests_total
├── {method=GET,  status=200, route=/api/users}  → [(t1, 100), (t2, 105), ...]
├── {method=GET,  status=500, route=/api/users}  → [(t1,   2), (t2,   3), ...]
└── {method=POST, status=200, route=/api/users}  → [(t1,  20), (t2,  21), ...]
```

라벨 조합이 늘어날수록 시계열 수도 함께 늘어난다. 이 때문에 메트릭을 설계할 때는 라벨 값의 종류, 즉 카디널리티를 제한해야 한다.

<a id="instrument-종류"></a>

## Instrument 종류

- OpenTelemetry는 6개의 Instrument를 정의함

### Counter

- 단조 증가하는 누적값, 감소 불가

```python
counter = meter.create_counter(
    name="http.server.requests",
    unit="1",
    description="Number of HTTP requests received",
)
counter.add(1, {"method": "GET", "status": 200})
```

- 대표 사용:
- 요청 수, 에러 수, 처리한 메시지 수
- 누적 바이트, 누적 시간 (시간을 더하기로 추적할 때)

### UpDownCounter

- 증감 가능한 값, 양수/음수 모두 add 가능

```python
in_flight = meter.create_up_down_counter("http.server.active_requests")
in_flight.add(1)   # 요청 시작
# ...
in_flight.add(-1)  # 요청 종료
```

- 대표 사용:
- 동시 처리 중인 요청 수
- 큐의 대기 항목 수
- 메모리 풀의 사용 중인 객체 수

### Gauge

- 현재 시점의 측정값, 누적이 아닌 스냅샷

```python
# Async Gauge: 콜백으로 현재 값을 보고
def cpu_callback(observer):
    observer.observe(get_cpu_percent(), {"core": "0"})

meter.create_observable_gauge("system.cpu.utilization", callbacks=[cpu_callback])
```

- 대표 사용:
- CPU 사용률, 메모리 사용량 (스냅샷)
- 온도, 큐 길이의 현재값

- `Sync Gauge` 도 추가되었지만 대부분의 백엔드는 비동기 Gauge가 더 자연스러움

### Histogram

- 값의 분포를 기록 → P50, P95, P99 같은 백분위 계산 가능

```python
duration = meter.create_histogram(
    name="http.server.request.duration",
    unit="s",
    description="HTTP request duration",
)
duration.record(0.123, {"route": "/api/users", "status": 200})
```

내부적으로 explicit bucket histogram을 사용하며, OTel SDK의 기본 bucket 경계는 다음과 같다.

```
buckets: [0, 5, 10, 25, 50, 75, 100, 250, 500, 750, 1000, 2500, 5000, 7500, 10000]
```

- 예시 (단위 `s` 로 기록하면 대부분의 HTTP 요청은 낮은 bucket에 집중):

```
le=0.005:    10
le=0.01:     50
le=0.025:   200
le=0.05:    400
le=0.1:     500
le=0.25:    520
le=+Inf:    525
sum:        12.34
count:      525
```

### ObservableCounter / ObservableUpDownCounter

- 비동기 버전, 콜백으로 누적값을 보고

```python
def gc_count_callback(observer):
    observer.observe(get_total_gc_count(), {"gc": "G1"})

meter.create_observable_counter("runtime.gc.count", callbacks=[gc_count_callback])
```

- 런타임 메트릭(GC 횟수, 누적 CPU 시간, JVM Metaspace 사용량 등)에 적합

<a id="동기-vs-비동기"></a>

## 동기 vs 비동기

- 호출 방식
  - 동기 (Sync): 코드 흐름에서 `add()`/`record()` 직접 호출
  - 비동기 (Async / Observable): SDK가 주기적으로 콜백 호출
- 적합한 데이터
  - 동기 (Sync): 이벤트(요청, 에러) 발생 시점에 기록
  - 비동기 (Async / Observable): 외부 자원의 현재 상태(CPU, 메모리)
- Counter
  - 동기 (Sync): `Counter` (delta로 add)
  - 비동기 (Async / Observable): `ObservableCounter` (cumulative 보고)
- UpDownCounter
  - 동기 (Sync): `UpDownCounter` (delta)
  - 비동기 (Async / Observable): `ObservableUpDownCounter` (절대값)
- Gauge
  - 동기 (Sync): `Gauge` (값 직접 기록)
  - 비동기 (Async / Observable): `ObservableGauge` (콜백으로 현재 상태)

- 핵심 차이: 동기 instrument는 델타를 SDK에 누적하고, 비동기 instrument는 콜백이 현재 절대값을 보고함 (ObservableCounter는 누적값).

<a id="aggregation-temporality"></a>

## Aggregation Temporality

Aggregation Temporality는 Counter/Histogram을 export할 때 어느 기간의 측정값을 보낼지 결정한다. 프로세스 시작 이후의 누적값을 보낼 수도 있고, 마지막 보고 이후의 변화량만 보낼 수도 있다.

### Cumulative (누적)

- 프로세스 시작 이후의 총합을 매번 보냄

```
t1: count = 100
t2: count = 105
t3: count = 110
```

- 장점:
- 보고를 놓쳐도 다음 보고에서 복구 가능
- Prometheus 모델과 일치

- 단점:
- 프로세스 재시작 시 카운터가 0으로 리셋 → 백엔드가 reset 감지 필요
- 짧게 사는 워크로드(서버리스)에 부적합

### Delta (증분)

- 마지막 보고 이후의 변화량만 보냄

```
t1: count = 100  (지난 60초간 100)
t2: count =   5  (다음 60초간 5)
t3: count =   5  (다음 60초간 5)
```

- 장점:
- Stateless export: Lambda/FaaS에 적합
- 합산할 때 단순

- 단점:
- 보고를 한 번 놓치면 데이터 영구 손실
- Prometheus와 직접 호환 안 됨 (변환 필요)

### 선택 기준

- Prometheus 백엔드: Cumulative 강제
- Datadog, Dynatrace 등 OTLP 수신 SaaS: 보통 Delta 선호
- Lambda/Cloud Run: Delta (재시작이 흔하므로)

- SDK에서 설정:

```python
exporter = OTLPMetricExporter(
    preferred_temporality={
        Counter: AggregationTemporality.DELTA,
        Histogram: AggregationTemporality.DELTA,
    }
)
```

<a id="views--instrument을-재구성하기"></a>

## Views: Instrument을 재구성하기

`View`는 SDK 단계에서 Instrument의 동작을 바꾸는 메커니즘이다. 계측 코드를 수정하지 않고 다음 사항을 조정할 수 있다.

- 이름, 설명 변경
- 특정 attribute만 남기기 (카디널리티 제어)
- 집계 방식 변경 (예: Histogram bucket 커스터마이즈)
- 특정 Instrument를 완전히 drop

### 예: 카디널리티 제어

- `http.request.duration` 에 user_id가 자동으로 들어가서 시계열이 폭발할 때:

```python
view = View(
    instrument_name="http.request.duration",
    attribute_keys={"route", "method", "status"},   # user_id를 빼버림
)
provider = MeterProvider(views=[view])
```

### 예: Histogram bucket 변경

- 기본 bucket이 ms 단위 작업에 부적합할 때:

```python
view = View(
    instrument_name="db.query.duration",
    aggregation=ExplicitBucketHistogramAggregation(
        boundaries=[0.0001, 0.0005, 0.001, 0.005, 0.01, 0.05, 0.1]
    ),
)
```

### 예: Instrument drop

```python
view = View(
    instrument_name="noisy.metric.*",
    aggregation=DropAggregation(),
)
```

- Views를 사용하면 계측 코드를 수정하지 않고 운영 환경별로 메트릭을 재구성할 수 있음

<a id="exemplars"></a>

## Exemplars

`Exemplar`는 Histogram(또는 Counter) DataPoint에 첨부되는 대표 샘플이다. 아래 예에서는 측정값과 함께 trace_id, span_id가 들어 있어 해당 요청의 trace로 이동할 수 있다.

```
http_request_duration_seconds_bucket{le="0.5"} 1234
exemplar { trace_id: "abc...", span_id: "def...", value: 0.487, time: t }
```

지연 메트릭을 보다가 실제 요청을 살펴보고 싶다면 exemplar를 클릭해 trace 화면으로 이동한다. 메트릭에서 발견한 현상을 개별 요청의 실행 과정과 연결하는 용도다.

### 동작

SDK는 Histogram bucket마다 최근 측정값을 하나씩 샘플로 유지한다. 측정 시점에 활성 SpanContext가 있으면 trace_id도 포함하며, export할 때 DataPoint에 첨부해 Prometheus/OTLP로 전송한다.

### 백엔드 지원

- Prometheus + OpenMetrics: native exemplar 지원
- Grafana: exemplar 위에서 클릭하면 Tempo로 점프
- Datadog/New Relic: 자체 trace 연결 메커니즘 사용

- 기본적으로 켜져 있으며 SDK 설정으로 켜기/끄기 가능

<a id="prometheus와의-관계"></a>

## Prometheus와의 관계

- OpenTelemetry Metric 모델은 Prometheus와 의도적으로 호환되도록 설계됨

### 매핑

- Counter → counter
- UpDownCounter → gauge (Prometheus엔 진짜 updown counter가 없음)
- Gauge → gauge
- Histogram (explicit bucket) → histogram
- Resource attributes → `target_info` 메트릭의 라벨 또는 모든 시계열의 라벨
- `service.name` → `job` 라벨로 매핑

### 양방향 변환

- OTel SDK → Prometheus: SDK에 Prometheus exporter 붙이거나, Collector의 `prometheusexporter` 사용
- Prometheus → OTel: Collector의 `prometheusreceiver`가 `/metrics`를 scrape하거나 Remote Write 수신

### 단위

- OTel은 UCUM(Unified Code for Units of Measure) 표기를 권장:
- `s` (seconds, 밀리초가 아님)
- `By` (bytes)
- `1` (dimensionless)

- Prometheus 변환 시 단위를 메트릭 이름에 붙이는 관습:

```
http.server.duration (unit=s) → http_server_duration_seconds
```

<a id="실전-패턴"></a>

## 실전 패턴

### 1. RED 메트릭

- 서비스 골든 시그널:
- Rate: 초당 요청 수 (Counter)
- Errors: 에러율 (Counter, status 라벨)
- Duration: 지연 분포 (Histogram)

- 자동 계측 라이브러리는 보통 아래 이름으로 생성함 (Semantic Conventions):

```
http.server.request.duration       (Histogram, unit=s)
http.server.active_requests        (UpDownCounter)
```

### 2. USE 메트릭

- 리소스 골든 시그널:
- Utilization: 사용률 (ObservableGauge)
- Saturation: 큐 길이/대기 (UpDownCounter, Gauge)
- Errors: 자원 에러 (Counter)

- 런타임 메트릭(GC, 스레드, 힙)은 보통 자동 계측 패키지가 제공

### 3. 카디널리티 예산

- 서비스마다 시계열 수의 상한을 두고 운영:
- 서비스 1개당 시계열 < 1만 권장
- attribute는 enum-like 카테고리만 (status, method, route)
- 고유값(user_id, trace_id, full URL)은 Span attribute로 처리 → Trace에 기록하고 Metric에는 기록 금지

### 4. 단위 통일

- 같은 의미의 측정은 같은 단위로:
- 시간은 항상 초(`s`)
- 크기는 항상 바이트(`By`)

- 서로 다른 단위가 섞이면 대시보드, 알람이 깨짐

## 참고 자료

- Metrics API/SDK: https://opentelemetry.io/docs/specs/otel/metrics/
- Metrics Data Model: https://opentelemetry.io/docs/specs/otel/metrics/data-model/
- Prometheus 호환: https://opentelemetry.io/docs/specs/otel/compatibility/prometheus_and_openmetrics/
- Semantic Conventions for HTTP metrics: https://github.com/open-telemetry/semantic-conventions/tree/main/docs/http
