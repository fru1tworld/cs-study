# TraceQL 쿼리 언어와 Metrics Generator

## TraceQL 쿼리 언어

> 원본: https://grafana.com/docs/tempo/latest/traceql/

<a id="traceql-개요"></a>
### TraceQL 개요

TraceQL은 Tempo에 저장된 트레이스를 조건에 따라 찾는 쿼리 언어다. PromQL이나 LogQL과 비슷한 문법으로 스팬의 속성을 필터링하고, 스팬 사이의 관계나 집계 결과로 검색 범위를 좁힐 수 있다.

#### 기본 문법 구조

```
{ <조건> }
```

- 가장 단순한 쿼리:

```traceql
{}                              # 모든 트레이스
{ .http.method = "GET" }        # http.method가 GET인 스팬
{ duration > 1s }               # 1초 이상 걸린 스팬
```

#### 사용 방법

- Grafana Explore: 쿼리 빌더 또는 직접 입력
- HTTP API: `GET /api/search?q=<traceql>`
- CLI: `tempo-cli` 또는 `traceql-search`

<a id="spanset-선택자"></a>

### Spanset 선택자

`{}` 안에 조건을 적으면 그 조건에 맞는 스팬 집합(spanset)을 선택한다. 조건 없이 모든 스팬을 선택할 수도 있고, 여러 조건을 결합해 범위를 좁힐 수도 있다.

#### 빈 선택자

```traceql
{}    # 모든 스팬
```

#### 단일 조건

```traceql
{ .http.status_code = 500 }
```

#### 다중 조건 (AND)

```traceql
{ .http.method = "POST" && .http.status_code >= 400 }
```

<a id="intrinsic-필드"></a>

### Intrinsic 필드

- 스팬의 내장 속성 → 별도의 접두사 없이 사용

- `name`: 스팬 이름 (operation name)
- `duration`: 스팬 지속 시간
- `status`: 스팬 상태 (`ok`, `error`, `unset`)
- `statusMessage`: 상태 메시지
- `kind`: 스팬 종류 (`server`, `client`, `producer`, `consumer`, `internal`)
- `traceDuration`: 전체 트레이스 지속 시간
- `rootName`: 루트 스팬 이름
- `rootServiceName`: 루트 스팬 서비스 이름
- `parent`: 부모 스팬
- `parent:id`: 부모 스팬 ID
- `event:name`: 이벤트 이름
- `event:timeSinceStart`: 이벤트 발생까지의 시간
- `link:traceID`, `link:spanID`: 링크된 스팬

#### 예시

```traceql
{ name = "HTTP GET /api/users" }
{ duration > 100ms }
{ status = error }
{ kind = server }
{ rootServiceName = "frontend" && status = error }
```

<a id="속성attributes-선택"></a>

### 속성(Attributes) 선택

- 속성 스코프는 다음 네 가지

- `.` 또는 `span.`: 스팬 속성
- `resource.`: 리소스 속성
- `event.`: 이벤트 속성
- `link.`: 링크 속성

#### 예시

```traceql
# 스팬 속성
{ .http.method = "GET" }
{ span.http.status_code = 500 }

# 리소스 속성 (서비스 식별자 등)
{ resource.service.name = "frontend" }
{ resource.cluster = "us-east-1" }

# 이벤트 속성
{ event.exception.type = "NullPointerException" }

# 링크 속성
{ link.trace_id = "abc123" }
```

#### 동적 타입

- 속성 타입은 자동으로 추론됨
- 문자열: `"text"` 큰따옴표
- 숫자: `200`, `1.5`
- 불리언: `true`, `false`
- Duration: `100ms`, `1s`, `5m`
- Status: `ok`, `error`, `unset` (예약어)

<a id="비교-연산자"></a>

### 비교 연산자

- `=`: 같음
- `!=`: 다름
- `>`: 큼
- `>=`: 크거나 같음
- `<`: 작음
- `<=`: 작거나 같음
- `=~`: 정규식 일치
- `!~`: 정규식 불일치

#### 예시

```traceql
{ .http.status_code >= 400 }
{ duration > 500ms }
{ resource.service.name =~ "frontend|backend" }
{ name !~ ".*health.*" }
```

<a id="논리-연산자"></a>

### 논리 연산자

- `&&`
  - 의미: AND
  - 적용: spanset 내부
- `||`
  - 의미: OR
  - 적용: spanset 내부

#### 예시

```traceql
# 단일 spanset 내 AND
{ .http.method = "POST" && .http.status_code >= 400 }

# 단일 spanset 내 OR
{ resource.service.name = "frontend" || resource.service.name = "api" }
```

<a id="구조적-연산자-structural"></a>

### 구조적 연산자 (Structural)

구조적 연산자는 여러 spanset 사이의 관계를 표현한다. 예를 들어 특정 서비스의 자식 스팬만 찾거나, 같은 트레이스 안에 오류 스팬과 결제 스팬이 함께 있는지 확인할 수 있다.

- `>`: 자식 (Child)
- `>>`: 후손 (Descendant)
- `<`: 부모 (Parent)
- `<<`: 조상 (Ancestor)
- `~`: 형제 (Sibling)
- `&&`: 같은 트레이스에 둘 다 존재
- `||`: 둘 중 하나라도 존재

#### 예시

```traceql
# frontend의 자식 중 backend 호출
{ resource.service.name = "frontend" } > { resource.service.name = "backend" }

# 어딘가에 db 호출이 있는 트레이스
{ resource.service.name = "frontend" } >> { .db.system = "postgresql" }

# 에러 스팬과 같은 트레이스에 결제 처리 스팬이 있는 경우
{ status = error } && { name = "process-payment" }
```

<a id="집계자-aggregators"></a>

### 집계자 (Aggregators)

- spanset에 대한 통계 함수

#### `count()`

- 일치하는 스팬 수 반환

```traceql
{ resource.service.name = "frontend" } | count() > 5
```

#### `avg(<field>)`, `sum(<field>)`, `min(<field>)`, `max(<field>)`

```traceql
{ resource.service.name = "api" } | avg(duration) > 100ms
{ name = "db.query" } | max(duration) > 1s
```

#### `by(<field>)`

- 집계 결과를 그룹화

```traceql
{ resource.service.name = "api" } | by(.http.method) | count() > 10
```

#### `select()`

- 결과에 추가로 표시할 필드 지정

```traceql
{ resource.service.name = "api" && status = error }
| select(.http.url, .http.status_code, span.user_id)
```

<a id="traceql-metrics"></a>

### TraceQL Metrics

TraceQL Metrics는 트레이스 데이터에서 메트릭을 즉시 계산한다. 조건에 맞는 스팬을 선택한 뒤 아래 함수로 호출 비율, 개수, 지연 시간 등을 집계한다.

#### `rate()`

- 초당 일치하는 스팬 수 계산

```traceql
{ resource.service.name = "frontend" } | rate()
```

#### `count_over_time()`

- 시간 간격당 일치하는 스팬 수 계산

```traceql
{ status = error } | count_over_time()
```

#### `quantile_over_time()`

- 지정한 분위수 계산

```traceql
{ resource.service.name = "api" } | quantile_over_time(duration, 0.95, 0.99)
```

#### `histogram_over_time()`

- 시간별 빈도 분포를 히스토그램으로 계산

```traceql
{ resource.service.name = "api" } | histogram_over_time(duration)
```

#### `compare()`

- 두 spanset을 비교해 차이를 강조 → 드릴다운 분석에 유용

```traceql
{ status = error } | compare({ status = ok })
```

#### 그룹화와 결합

```traceql
{ resource.service.name = "api" } | rate() by (.http.route)
```

<a id="실전-쿼리-예시"></a>

### 실전 쿼리 예시

#### 1. 느린 트레이스 찾기

```traceql
{ duration > 1s }
```

#### 2. 에러 트레이스 찾기

```traceql
{ status = error }
```

#### 3. 특정 사용자 트레이스

```traceql
{ .user.id = "u-12345" }
```

#### 4. HTTP 5xx 에러

```traceql
{ .http.status_code >= 500 }
```

#### 5. 데이터베이스 느린 쿼리

```traceql
{ .db.system = "postgresql" && duration > 100ms }
```

#### 6. 특정 서비스의 외부 API 호출

```traceql
{ resource.service.name = "checkout" && .http.url =~ "https://payment-gateway.*" }
```

#### 7. 결제 실패 (구조적)

```traceql
{ name = "checkout" } >> { name = "payment" && status = error }
```

#### 8. 특정 엔드포인트의 P95 지연

```traceql
{ resource.service.name = "api" && .http.route = "/api/orders" } 
| quantile_over_time(duration, 0.95)
```

#### 9. 분당 에러율

```traceql
{ status = error } 
| rate() 
by (resource.service.name)
```

#### 10. 외부 서비스 호출이 있는 느린 트레이스

```traceql
{ duration > 500ms } && { kind = client && .http.url =~ "https://.*" }
```

### 쿼리 옵션

#### 시간 범위

- 쿼리는 항상 시간 범위와 함께 실행

```bash
curl -G "http://tempo:3200/api/search" \
  --data-urlencode 'q={ status = error }' \
  --data-urlencode 'start=1700000000' \
  --data-urlencode 'end=1700003600' \
  --data-urlencode 'limit=20'
```

#### 결과 한도

```bash
--data-urlencode 'limit=20'           # 최대 결과 수
--data-urlencode 'spss=10'            # spans per spanset
```

#### TraceQL Metrics

```bash
curl -G "http://tempo:3200/api/metrics/query_range" \
  --data-urlencode 'q={resource.service.name="api"} | rate()' \
  --data-urlencode 'start=1700000000' \
  --data-urlencode 'end=1700003600' \
  --data-urlencode 'step=60s'
```

## Tempo Metrics Generator

> 원본: https://grafana.com/docs/tempo/latest/metrics-generator/

<a id="개요"></a>
### 개요

Metrics Generator는 수집한 트레이스에서 메트릭을 만드는 선택적 컴포넌트다. Distributor가 스팬을 Ingester와 Metrics Generator에 함께 전달하면, Ingester는 트레이스를 저장하고 Metrics Generator는 메트릭을 생성해 Prometheus나 Mimir로 보낸다.

#### 동작 방식

```
[Distributor]
     |
     +--> [Ingester]            (트레이스 저장)
     |
     +--> [Metrics Generator]   (메트릭 생성)
                |
                v
         [Prometheus / Mimir]   (Remote Write)
```

#### 프로세서 종류

- Service Graph
  - 설명: 서비스 간 호출 관계 분석
  - 출력: 서비스 그래프 메트릭
- Span Metrics
  - 설명: 스팬 단위 RED 메트릭
  - 출력: 요청/에러/지연 메트릭
- Local Blocks
  - 설명: 로컬에 메트릭 블록 저장
  - 출력: TraceQL Metrics 백엔드

#### 주의사항

메트릭 생성을 활성화하면 활성 시계열(active series)이 늘어난다. 이로 인해 Mimir나 Prometheus 운영 비용이 커질 수 있고, Grafana Cloud에서는 청구 금액에도 영향을 준다.

<a id="활성화"></a>

### 활성화

#### 글로벌 활성화

```yaml
metrics_generator:
  registry:
    external_labels:
      source: tempo
      cluster: prod-us-east-1
  
  storage:
    path: /tmp/tempo/generator/wal
    remote_write:
      - url: http://mimir:9009/api/v1/push
        send_exemplars: true
        headers:
          X-Scope-OrgID: tempo
        write_relabel_configs: []
  
  traces_storage:
    path: /tmp/tempo/generator/traces

# 테넌트별 활성화
overrides:
  defaults:
    metrics_generator:
      processors:
        - service-graphs
        - span-metrics
        - local-blocks
```

#### 테넌트별 설정 (overrides.yaml)

```yaml
overrides:
  tenant-a:
    metrics_generator:
      processors:
        - service-graphs
        - span-metrics
      collection_interval: 15s
      disable_collection: false
```

<a id="service-graph-프로세서"></a>

### Service Graph 프로세서

#### 기능

Service Graph 프로세서는 스팬을 분석해 서비스 간 호출 관계(엣지)를 찾는다. 이 관계별로 호출 횟수와 소요 시간을 메트릭으로 기록해 서비스 그래프를 구성한다.

#### 생성되는 메트릭

- `traces_service_graph_request_total`
  - 타입: Counter
  - 설명: 서비스 간 요청 수
- `traces_service_graph_request_failed_total`
  - 타입: Counter
  - 설명: 서비스 간 실패 요청 수
- `traces_service_graph_request_server_seconds`
  - 타입: Histogram
  - 설명: 서버 측 응답 시간
- `traces_service_graph_request_client_seconds`
  - 타입: Histogram
  - 설명: 클라이언트 측 응답 시간
- `traces_service_graph_unpaired_spans_total`
  - 타입: Counter
  - 설명: 짝이 없는 스팬 수
- `traces_service_graph_dropped_spans_total`
  - 타입: Counter
  - 설명: 드롭된 스팬 수

#### 라벨

- `client`: 호출자 서비스
- `server`: 호출 대상 서비스
- `connection_type`: messaging_system, database, virtual_node 등

#### 설정

```yaml
metrics_generator:
  processor:
    service_graphs:
      max_items: 10000              # 메모리 내 보관할 엣지 수
      workers: 10                   # 처리 워커 수
      histogram_buckets: [0.1, 0.2, 0.4, 0.8, 1.6, 3.2, 6.4, 12.8]
      dimensions: []                # 추가 라벨 차원
      peer_attributes:              # 외부 서비스 식별 속성
        - peer.service
        - db.name
        - db.system
      enable_client_server_prefix: false
```

#### 동작 원리

프로세서는 `kind=client` 스팬을 메모리에 보관하고 같은 트레이스의 `kind=server` 스팬과 짝을 찾는다. 짝을 찾으면 client에서 server로 향하는 엣지를 만들고, 정해진 시간 안에 찾지 못하면 unpaired로 처리한다. 따라서 `traces_service_graph_unpaired_spans_total`은 호출 관계를 완성하지 못한 스팬을 확인할 때 사용한다.

<a id="span-metrics-프로세서"></a>

### Span Metrics 프로세서

#### 기능

- 각 스팬에서 RED 메트릭(Rate, Error, Duration) 생성

#### 생성되는 메트릭

- `traces_spanmetrics_calls_total`
  - 타입: Counter
  - 설명: 스팬 호출 수
- `traces_spanmetrics_latency`
  - 타입: Histogram
  - 설명: 스팬 지연 시간
- `traces_spanmetrics_size_total`
  - 타입: Counter
  - 설명: 스팬 크기 (선택적)

#### 기본 라벨

- `service` (resource.service.name)
- `span_name` (스팬 이름)
- `span_kind` (스팬 종류)
- `status_code` (스팬 상태)

#### 설정

```yaml
metrics_generator:
  processor:
    span_metrics:
      histogram_buckets: [0.002, 0.004, 0.008, 0.016, 0.032, 0.064, 0.128, 0.256, 0.512, 1.02, 2.05, 4.10]
      
      # 추가 차원으로 사용할 속성
      dimensions:
        - http.method
        - http.status_code
        - http.target
        - db.system
      
      # 추가 측정 메트릭
      additional_dimensions:
        - http.route
      
      enable_target_info: false
```

#### RED 메트릭 PromQL 예시

```promql
# 요청 비율 (Rate)
sum by (service) (rate(traces_spanmetrics_calls_total[5m]))

# 에러 비율 (Error)
sum by (service) (rate(traces_spanmetrics_calls_total{status_code="STATUS_CODE_ERROR"}[5m]))
/
sum by (service) (rate(traces_spanmetrics_calls_total[5m]))

# P95 지연 시간 (Duration)
histogram_quantile(0.95,
  sum by (service, le) (rate(traces_spanmetrics_latency_bucket[5m]))
)
```

<a id="local-blocks-프로세서"></a>

### Local Blocks 프로세서

#### 기능

Local Blocks 프로세서는 TraceQL Metrics에서 사용할 데이터를 로컬 디스크에 블록으로 저장한다. 쿼리가 들어오면 이 블록을 읽어 메트릭을 계산하며, 일반 트레이스 저장소와는 별도로 관리한다.

#### 설정

```yaml
metrics_generator:
  processor:
    local_blocks:
      max_live_traces: 10000
      max_block_duration: 5m
      max_block_bytes: 500_000_000
      flush_check_period: 10s
      trace_idle_period: 10s
      complete_block_timeout: 1h
      filter_server_spans: false
  
  traces_storage:
    path: /tmp/tempo/generator/traces
```

### TraceQL Metrics

#### 개요

- TraceQL Metrics 사용 시 트레이스 데이터에서 즉석으로 메트릭 계산 가능 (Local Blocks 프로세서 활성화 필요)

#### 메트릭 함수

```traceql
# 요청 비율
{ resource.service.name = "frontend" } | rate()

# 시간 윈도우 카운트
{ status = error } | count_over_time()

# 분위수 통계
{ resource.service.name = "api" } 
| quantile_over_time(duration, 0.5, 0.95, 0.99)

# 히스토그램
{ resource.service.name = "api" } | histogram_over_time(duration)
```

#### 그룹화

```traceql
{ resource.service.name = "api" } | rate() by (.http.route)
```

#### 비교

```traceql
{ status = error } | compare({ status = ok })
```

#### 활용 사례

- 일반 메트릭으로는 파악하기 어려운 임시 분석
- 특정 트레이스 속성 조합의 메트릭을 즉석에서 생성
- 새 메트릭 정의 전 사전 탐색

<a id="remote-write-설정"></a>

### Remote Write 설정

#### Mimir로 전송

```yaml
metrics_generator:
  storage:
    path: /tmp/tempo/generator/wal
    remote_write:
      - url: http://mimir:9009/api/v1/push
        send_exemplars: true
        headers:
          X-Scope-OrgID: tempo
        timeout: 30s
        queue_config:
          capacity: 10000
          max_shards: 200
          min_shards: 1
          max_samples_per_send: 1000
        metadata_config:
          send: true
          send_interval: 1m
```

#### Prometheus로 전송

```yaml
metrics_generator:
  storage:
    remote_write:
      - url: http://prometheus:9090/api/v1/write
```

#### 다중 백엔드

```yaml
metrics_generator:
  storage:
    remote_write:
      - url: http://mimir-prod:9009/api/v1/push
        headers:
          X-Scope-OrgID: tempo
      - url: http://mimir-backup:9009/api/v1/push
        headers:
          X-Scope-OrgID: tempo-backup
```

<a id="grafana-대시보드-통합"></a>

### Grafana 대시보드 통합

#### 데이터 소스 연결

- Grafana에서 Tempo 데이터 소스 설정

```yaml
# Service Graph 활성화
serviceMap:
  datasourceUid: <prometheus_or_mimir_uid>

# Span Metrics 활성화
spanBar:
  type: Tag
  tag: http.method

nodeGraph:
  enabled: true

tracesToMetrics:
  datasourceUid: <prometheus_or_mimir_uid>
  spanStartTimeShift: '-2m'
  spanEndTimeShift: '2m'
```

#### Service Graph 보기

- 1\. Grafana Explore에서 Tempo 선택
- 2\. Service Graph 탭 선택
- 3\. 시간 범위 지정
- 4\. 자동 생성된 서비스 그래프 확인

#### APM 대시보드

- Tempo와 함께 제공되는 [APM 대시보드](https://github.com/grafana/tempo/blob/main/example/docker-compose/grafana/grafana-tempo-mixin/dashboards/tempo-operational.json) 사용 가능

<a id="용량-계획"></a>

### 용량 계획

#### 활성 시계열 추정

Span Metrics의 시계열 수는 라벨 값의 조합에 따라 늘어난다. 서비스와 스팬 이름 외에 추가 차원을 지정하면, 그 차원의 카디널리티도 함께 고려해야 한다.

```
시계열 수 ≈ (서비스 수) × (스팬 이름 수) × (스팬 종류 수) × (status 수) × (추가 차원 카디널리티)
```

- 예: 10개 서비스, 평균 50개 엔드포인트, 4개 종류, 3개 status, http_method 4종 = 24,000 시계열

#### 권장 사항

- 카디널리티가 높은 값(user_id, request_id 등)은 추가 차원(dimensions)에 넣지 말 것
- 초기에는 기본 차원만 사용하고 점진적으로 추가
- Grafana Cloud 사용 시 비용을 지속적으로 모니터링

#### 메모리/디스크

- Local Blocks: `max_block_bytes` × `max_live_traces`만큼 메모리 + 블록 디스크
- Service Graph: `max_items`개 만큼 메모리 (각 약 100바이트)

#### 샤딩

```yaml
metrics_generator:
  ring:
    kvstore:
      store: memberlist
```

- 여러 인스턴스가 트레이스를 분산 처리
