# Tempo Metrics Generator

> 이 문서는 Grafana Tempo 공식 문서의 "Metrics and tracing" 섹션을 한국어로 정리한 것입니다.
> 원본: https://grafana.com/docs/tempo/latest/metrics-generator/

---

## 목차

1. [개요](#개요)
2. [활성화](#활성화)
3. [Service Graph 프로세서](#service-graph-프로세서)
4. [Span Metrics 프로세서](#span-metrics-프로세서)
5. [Local Blocks 프로세서](#local-blocks-프로세서)
6. [TraceQL Metrics](#traceql-metrics)
7. [Remote Write 설정](#remote-write-설정)
8. [Grafana 대시보드 통합](#grafana-대시보드-통합)
9. [용량 계획](#용량-계획)

---

## 개요

**Metrics-generator** 는 Tempo의 선택적 컴포넌트로, **수집된 트레이스에서 메트릭을 자동 생성** 합니다.

### 동작 방식

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

Distributor가 수신한 스팬을 Ingester와 Metrics Generator **양쪽으로 동시에 전달**합니다.

### 프로세서 종류

| 프로세서 | 설명 | 출력 |
|---------|------|------|
| **Service Graph** | 서비스 간 호출 관계 분석 | 서비스 그래프 메트릭 |
| **Span Metrics** | 스팬 단위 RED 메트릭 | 요청/에러/지연 메트릭 |
| **Local Blocks** | 로컬에 메트릭 블록 저장 | TraceQL Metrics 백엔드 |

### 주의사항

- 메트릭 생성을 활성화하면 **활성 시계열(active series)이 증가** 하므로 메트릭 백엔드(Mimir/Prometheus) 비용에 영향
- Grafana Cloud의 경우 청구에 영향

---

## 활성화

### 글로벌 활성화

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

### 테넌트별 설정 (overrides.yaml)

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

---

## Service Graph 프로세서

### 기능

스팬을 분석하여 **서비스 간 호출 관계(엣지)** 를 발견하고, 호출 횟수와 지속 시간을 메트릭으로 기록합니다.

### 생성되는 메트릭

| 메트릭 | 타입 | 설명 |
|--------|------|------|
| `traces_service_graph_request_total` | Counter | 서비스 간 요청 수 |
| `traces_service_graph_request_failed_total` | Counter | 서비스 간 실패 요청 수 |
| `traces_service_graph_request_server_seconds` | Histogram | 서버 측 응답 시간 |
| `traces_service_graph_request_client_seconds` | Histogram | 클라이언트 측 응답 시간 |
| `traces_service_graph_unpaired_spans_total` | Counter | 짝이 없는 스팬 수 |
| `traces_service_graph_dropped_spans_total` | Counter | 드롭된 스팬 수 |

### 라벨

- `client`: 호출자 서비스
- `server`: 호출 대상 서비스
- `connection_type`: messaging_system, database, virtual_node 등

### 설정

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

### 동작 원리

1. 스팬을 받으면 `kind=client`인 스팬을 메모리에 저장
2. 같은 트레이스의 `kind=server` 스팬과 매칭
3. 매칭되면 client → server 엣지 생성
4. 시간 윈도우 내에 매칭 안 되면 unpaired로 처리

---

## Span Metrics 프로세서

### 기능

각 스팬에서 **RED 메트릭(Rate, Error, Duration)** 을 생성합니다.

### 생성되는 메트릭

| 메트릭 | 타입 | 설명 |
|--------|------|------|
| `traces_spanmetrics_calls_total` | Counter | 스팬 호출 수 |
| `traces_spanmetrics_latency` | Histogram | 스팬 지연 시간 |
| `traces_spanmetrics_size_total` | Counter | 스팬 크기 (선택적) |

### 기본 라벨

- `service` (resource.service.name)
- `span_name` (스팬 이름)
- `span_kind` (스팬 종류)
- `status_code` (스팬 상태)

### 설정

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
      
      enable_target_info: true
```

### RED 메트릭 PromQL 예시

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

---

## Local Blocks 프로세서

### 기능

**TraceQL Metrics** 를 위한 메트릭 데이터를 로컬에 블록 형태로 저장합니다.

### 동작

- 스팬 데이터를 메트릭 생성 가능한 형태로 로컬 디스크에 블록 저장
- TraceQL Metrics 쿼리 시 이 블록들을 사용
- 일반 트레이스 저장과는 별개

### 설정

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

---

## TraceQL Metrics

### 개요

TraceQL Metrics를 사용하면 **트레이스 데이터에서 즉석으로 메트릭을 계산** 할 수 있습니다. (Local Blocks 프로세서 활성화 필요)

### 메트릭 함수

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

### 그룹화

```traceql
{ resource.service.name = "api" } | rate() by (.http.route)
```

### 비교

```traceql
{ status = error } | compare({ status = ok })
```

### 활용 사례

- 일반 메트릭으로는 추적 불가능한 임시 분석
- 특정 트레이스 속성 조합의 메트릭 즉석 생성
- 새 메트릭 정의 전 탐색

---

## Remote Write 설정

### Mimir로 전송

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

### Prometheus로 전송

```yaml
metrics_generator:
  storage:
    remote_write:
      - url: http://prometheus:9090/api/v1/write
```

### 다중 백엔드

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

---

## Grafana 대시보드 통합

### 데이터 소스 연결

Grafana에서 Tempo 데이터 소스 설정:

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

### Service Graph 보기

1. Grafana Explore에서 Tempo 선택
2. **Service Graph** 탭 선택
3. 시간 범위 지정
4. 자동 생성된 서비스 그래프 확인

### APM 대시보드

Tempo와 함께 제공되는 [APM 대시보드](https://github.com/grafana/tempo/blob/main/example/docker-compose/grafana/grafana-tempo-mixin/dashboards/tempo-operational.json) 사용 가능.

---

## 용량 계획

### 활성 시계열 추정

Span Metrics에서 생성되는 시계열 수:

```
시계열 수 ≈ (서비스 수) × (스팬 이름 수) × (스팬 종류 수) × (status 수) × (추가 차원 카디널리티)
```

예: 10개 서비스, 평균 50개 엔드포인트, 4개 종류, 3개 status, http_method 4종 = **24,000 시계열**

### 권장 사항

- 추가 차원(dimensions)에 카디널리티 높은 값 추가 금지 (user_id, request_id 등)
- 처음에는 기본 차원만 사용, 점진적 추가
- Grafana Cloud 사용 시 비용 모니터링

### 메모리/디스크

- Local Blocks: `max_block_bytes` × `max_live_traces`만큼 메모리 + 블록 디스크
- Service Graph: `max_items`개 만큼 메모리 (각 약 100바이트)

### 샤딩

```yaml
metrics_generator:
  ring:
    kvstore:
      store: memberlist
```

여러 인스턴스가 트레이스를 분산 처리.
