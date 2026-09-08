# Mimir 메트릭 전송과 PromQL 쿼리

## Mimir로 메트릭 전송

> 원본: https://grafana.com/docs/mimir/latest/configure/configure-the-write-path/

<a id="개요"></a>
### 개요

Mimir는 여러 프로토콜로 메트릭을 받는다. 사용하는 수집기에 맞춰 전송 엔드포인트와 헤더를 설정한다.

- Prometheus Remote Write
  - 엔드포인트: `/api/v1/push`
  - 인증 헤더: `X-Scope-OrgID`
- OpenTelemetry OTLP/HTTP
  - 엔드포인트: `/otlp/v1/metrics`
  - 인증 헤더: `X-Scope-OrgID`
- Influx Line Protocol
  - 엔드포인트: `/api/v1/push/influx/write`
  - 인증 헤더: `X-Scope-OrgID`
- Datadog Agent
  - 엔드포인트: `/api/v1/push/datadog`
  - 인증 헤더: `X-Scope-OrgID`
- Graphite
  - 엔드포인트: `/api/v1/push/graphite`
  - 인증 헤더: `X-Scope-OrgID`

#### 멀티 테넌시

`multitenancy_enabled: true`로 멀티 테넌시를 활성화했다면 모든 요청에 `X-Scope-OrgID` 헤더를 넣어야 한다.

<a id="prometheus-remote-write"></a>

### Prometheus Remote Write

#### Prometheus 설정

`remote_write`에 Mimir의 수신 주소와 테넌트 헤더를 지정한다. 아래 예제는 인증과 TLS, 전송 큐를 설정하고 `go_.*` 메트릭은 전송 대상에서 제외한다.

```yaml
# prometheus.yml
global:
  scrape_interval: 15s
  external_labels:
    cluster: us-east-1
    replica: 0  # HA 페어 시 0/1로 구분

scrape_configs:
  - job_name: prometheus
    static_configs:
      - targets: ['localhost:9090']

remote_write:
  - url: https://mimir.example.com/api/v1/push
    headers:
      X-Scope-OrgID: tenant-1
    
    basic_auth:
      username: user
      password: pass
    
    tls_config:
      ca_file: /etc/ssl/certs/ca.crt
    
    queue_config:
      capacity: 10000
      max_samples_per_send: 2000
      max_shards: 30
      min_shards: 1
      batch_send_deadline: 5s
    
    metadata_config:
      send: true
      send_interval: 1m
    
    write_relabel_configs:
      - source_labels: [__name__]
        regex: 'go_.*'
        action: drop
```

#### 권장 큐 설정

- 소규모: capacity 2500, max_shards 10, max_samples_per_send 500
- 중규모: capacity 10000, max_shards 30, max_samples_per_send 2000
- 대규모: capacity 100000, max_shards 200, max_samples_per_send 5000

<a id="grafana-alloy"></a>

### Grafana Alloy

#### config.alloy

아래 구성은 Node Exporter와 Kubernetes Pod에서 메트릭을 수집한 뒤 `cluster` 라벨을 추가해 Mimir로 전달한다. 수집 대상은 달라도 라벨 처리와 전송 설정을 공유하는 구조다.

```alloy
// Node Exporter 메트릭 수집
prometheus.exporter.unix "node" { }

// Kubernetes Pod 디스커버리
discovery.kubernetes "pods" {
  role = "pod"
}

prometheus.scrape "node" {
  targets    = prometheus.exporter.unix.node.targets
  forward_to = [prometheus.relabel.add_labels.receiver]
  scrape_interval = "15s"
}

prometheus.scrape "kubernetes" {
  targets    = discovery.kubernetes.pods.targets
  forward_to = [prometheus.relabel.add_labels.receiver]
}

// 라벨 추가
prometheus.relabel "add_labels" {
  forward_to = [prometheus.remote_write.mimir.receiver]
  
  rule {
    target_label = "cluster"
    replacement  = "us-east-1"
  }
}

// Mimir로 전송
prometheus.remote_write "mimir" {
  endpoint {
    url = "https://mimir.example.com/api/v1/push"
    headers = {
      "X-Scope-OrgID" = "tenant-1",
    }
    
    basic_auth {
      username = "user"
      password = sys.env("MIMIR_PASSWORD")
    }
    
    queue_config {
      capacity            = 10000
      max_samples_per_send = 2000
      max_shards          = 30
      batch_send_deadline = "5s"
    }
    
    write_relabel_config {
      source_labels = ["__name__"]
      regex         = "go_.*"
      action        = "drop"
    }
  }
  
  external_labels = {
    cluster = "us-east-1",
    region  = "primary",
  }
}
```

#### 클러스터링

여러 Alloy 인스턴스로 수집할 때는 클러스터링을 활성화해 스크래핑 부하를 자동으로 분산한다.

```alloy
prometheus.scrape "kubernetes" {
  targets    = discovery.kubernetes.pods.targets
  forward_to = [prometheus.remote_write.mimir.receiver]
  
  clustering {
    enabled = true
  }
}
```

<a id="opentelemetry-collector"></a>

### OpenTelemetry Collector

#### Mimir 활성화

```yaml
# mimir.yaml
distributor:
  otel_metric_suffixes_enabled: true
```

#### OTel Collector 구성

```yaml
receivers:
  otlp:
    protocols:
      grpc:
        endpoint: 0.0.0.0:4317
      http:
        endpoint: 0.0.0.0:4318
  
  prometheus:
    config:
      scrape_configs:
        - job_name: app
          static_configs:
            - targets: ['app:8080']

processors:
  batch:
    send_batch_size: 1000
    timeout: 10s
  
  resource:
    attributes:
      - key: cluster
        value: us-east-1
        action: upsert

exporters:
  otlphttp/mimir:
    endpoint: https://mimir.example.com/otlp
    headers:
      X-Scope-OrgID: tenant-1
    auth:
      authenticator: basicauth/mimir
  
  # 또는 Prometheus Remote Write 사용
  prometheusremotewrite/mimir:
    endpoint: https://mimir.example.com/api/v1/push
    headers:
      X-Scope-OrgID: tenant-1
    external_labels:
      cluster: us-east-1
    resource_to_telemetry_conversion:
      enabled: true

extensions:
  basicauth/mimir:
    client_auth:
      username: user
      password: ${env:MIMIR_PASSWORD}

service:
  extensions: [basicauth/mimir]
  pipelines:
    metrics:
      receivers: [otlp, prometheus]
      processors: [batch, resource]
      exporters: [prometheusremotewrite/mimir]
```

#### OTel 메트릭 → Prometheus 변환

Mimir는 받은 OTel 메트릭을 Prometheus 형식으로 자동 변환한다. 이 과정에서 이름과 속성을 다음과 같이 변환한다.

- 점(`.`)은 언더스코어(`_`)로 변환
- 단위 접미사 자동 추가 (선택적)
- Resource Attributes는 라벨로 변환

<a id="influx-line-protocol"></a>

### Influx Line Protocol

#### Mimir 활성화

```yaml
distributor:
  influx:
    enabled: true
    max_request_size_bytes: 10000000
```

#### Telegraf 구성

```toml
[[outputs.http]]
  url = "https://mimir.example.com/api/v1/push/influx/write"
  method = "POST"
  data_format = "influx"
  headers = {"X-Scope-OrgID" = "tenant-1"}
```

#### 직접 전송

```bash
curl -X POST \
  -H "X-Scope-OrgID: tenant-1" \
  --data-binary "weather,location=us-midwest temperature=82 1700000000000000000" \
  https://mimir.example.com/api/v1/push/influx/write
```

<a id="datadog-agent-호환"></a>

### Datadog Agent 호환

#### Mimir 활성화

```yaml
distributor:
  datadog:
    enabled: true
```

#### Datadog Agent 구성

```yaml
# datadog.yaml
api_key: dummy

dd_url: https://mimir.example.com/api/v1/push/datadog

# 또는 환경변수
# DD_API_KEY=dummy
# DD_DD_URL=https://mimir.example.com/api/v1/push/datadog
# DD_TAGS="X-Scope-OrgID:tenant-1"
```

<a id="graphite"></a>

### Graphite

#### Mimir 활성화

```yaml
distributor:
  graphite:
    enabled: true
```

#### graphite-web/carbon-relay-ng 설정

```ini
[mimir]
type = grpc
addr = mimir-distributor:9095
```

- 또는 직접 HTTP:

```bash
echo "test.metric 42 $(date +%s)" | nc mimir 2003
```

<a id="mimir-push-api-직접-호출"></a>

### Mimir Push API 직접 호출

#### 엔드포인트

```
POST /api/v1/push
Content-Type: application/x-protobuf
Content-Encoding: snappy
X-Scope-OrgID: <tenant-id>
```

#### Body 형식

- Snappy로 압축된 protobuf (`prometheus.WriteRequest`).

#### Go 예시

```go
import (
    "github.com/prometheus/prometheus/prompb"
    "github.com/golang/snappy"
)

req := &prompb.WriteRequest{
    Timeseries: []prompb.TimeSeries{
        {
            Labels: []prompb.Label{
                {Name: "__name__", Value: "test_metric"},
                {Name: "job", Value: "demo"},
            },
            Samples: []prompb.Sample{
                {Value: 42.0, Timestamp: time.Now().UnixMilli()},
            },
        },
    },
}

data, _ := proto.Marshal(req)
compressed := snappy.Encode(nil, data)

httpReq, _ := http.NewRequest("POST", "https://mimir/api/v1/push", bytes.NewReader(compressed))
httpReq.Header.Set("Content-Type", "application/x-protobuf")
httpReq.Header.Set("Content-Encoding", "snappy")
httpReq.Header.Set("X-Scope-OrgID", "tenant-1")
```

#### 응답 코드

- 200: 성공
- 400: 잘못된 요청(라벨 포맷 등)
- 401: 인증 실패
- 429: Rate Limit 초과
- 500: 서버 에러

<a id="ha-pair-처리"></a>

### HA Pair 처리

#### 문제

Prometheus를 HA 페어로 운영하면 두 인스턴스가 같은 메트릭을 전송하므로 데이터가 중복된다.

#### 해결: HA Tracker

- Mimir의 HA Tracker가 한 시점에 한 페어만 활성으로 인식

#### Prometheus 설정

- 각 페어는 동일한 `cluster` 라벨을 공유하되, `__replica__` 라벨은 서로 다르게 설정

```yaml
# prometheus-1.yml
global:
  external_labels:
    cluster: prod
    __replica__: replica-0

remote_write:
  - url: https://mimir/api/v1/push
    headers:
      X-Scope-OrgID: tenant-1
```

```yaml
# prometheus-2.yml
global:
  external_labels:
    cluster: prod
    __replica__: replica-1

remote_write:
  - url: https://mimir/api/v1/push
    headers:
      X-Scope-OrgID: tenant-1
```

#### Mimir HA Tracker 활성화

```yaml
distributor:
  ha_tracker:
    enable_ha_tracker: true
    
    kvstore:
      store: consul
      consul:
        host: consul:8500
    
    ha_tracker_update_timeout: 15s
    ha_tracker_update_timeout_jitter_max: 5s
    ha_tracker_failover_timeout: 30s

limits:
  ha_cluster_label: cluster
  ha_replica_label: __replica__
```

#### 동작

두 인스턴스가 모두 메트릭을 보내더라도 HA Tracker는 KV 저장소에 기록한 활성 replica의 데이터만 받아들인다. 활성 replica가 응답하지 않으면 다른 replica로 페일오버한다.

## Mimir PromQL 쿼리

> 원본: https://grafana.com/docs/mimir/latest/references/http-api/

<a id="promql-개요"></a>
### PromQL 개요

Mimir에 저장한 메트릭은 Prometheus와 같은 쿼리 언어인 PromQL(Prometheus Query Language)로 조회한다. 먼저 쿼리가 다루는 데이터 타입을 구분한 뒤, 라벨로 시계열을 선택하고 연산이나 집계 함수를 적용한다.

#### 4가지 쿼리 타입

- Instant Vector: `up`
- Range Vector: `up[5m]`
- Scalar: `42`
- String: `"hello"`(드물게 사용)

<a id="데이터-타입"></a>

### 데이터 타입

#### Instant Vector

- 특정 시점에서 시계열의 현재 값

```promql
http_requests_total
```

#### Range Vector

- 특정 시점에서 과거 일정 기간의 값들

```promql
http_requests_total[5m]
```

#### Scalar

- 단일 숫자 값

```promql
3.14
```

<a id="selectors-셀렉터"></a>

### Selectors (셀렉터)

#### Label 매칭 연산자

- `=`: 정확 일치
- `!=`: 일치하지 않음
- `=~`: 정규식 일치
- `!~`: 정규식 불일치

```promql
http_requests_total{method="GET"}
http_requests_total{status=~"5.."}
http_requests_total{job!="prometheus"}
```

#### 메트릭 이름 매처

- `__name__` 라벨로 매칭 가능:

```promql
{__name__=~"http_requests.*"}
```

#### Range 셀렉터

```promql
http_requests_total[5m]      # 5분
http_requests_total[1h]      # 1시간
http_requests_total[1d]      # 1일
http_requests_total[1w]      # 1주
```

#### Offset

```promql
http_requests_total offset 5m
http_requests_total[5m] offset 1h
```

#### `@` Modifier

- 특정 시점 평가:

```promql
http_requests_total @ 1700000000
http_requests_total[5m] @ 1700000000
http_requests_total @ start()
http_requests_total @ end()
```

<a id="연산자"></a>

### 연산자

#### 산술 연산

```promql
+, -, *, /, %, ^
```

```promql
node_memory_MemFree_bytes / node_memory_MemTotal_bytes
```

#### 비교 연산

```promql
==, !=, >, <, >=, <=
```

비교 연산은 기본적으로 필터로 동작해 조건을 만족하는 시계열만 반환한다.

```promql
node_memory_MemFree_bytes < 1000000000
```

조건의 충족 여부를 값으로 확인하려면 `bool` 수식어를 붙인다. 이 경우 결과를 0 또는 1로 반환한다.

```promql
node_memory_MemFree_bytes < bool 1000000000
```

#### 논리/집합 연산

- `and`: 두 셋의 교집합
- `or`: 합집합
- `unless`: 차집합

```promql
metric_a and metric_b
metric_a or metric_b
metric_a unless metric_b
```

#### 벡터 매칭

```promql
# one-to-one (기본)
metric_a + metric_b

# 라벨 무시
metric_a + ignoring(method) metric_b

# 특정 라벨로만 매칭
metric_a + on(instance) metric_b

# many-to-one
sum_metric * on(instance) group_left small_metric

# one-to-many
small_metric * on(instance) group_right sum_metric
```

<a id="집계-함수"></a>

### 집계 함수

- `sum`: 합계
- `avg`: 평균
- `min`: 최소
- `max`: 최대
- `count`: 개수
- `count_values`: 고유 값별 개수
- `stddev`: 표준편차
- `stdvar`: 분산
- `topk`: 상위 K
- `bottomk`: 하위 K
- `quantile`: 분위수
- `group`: 그룹화만

#### 그룹화

```promql
# 라벨별 그룹화
sum by (instance) (rate(http_requests_total[5m]))

# 특정 라벨만 제외
sum without (method) (rate(http_requests_total[5m]))

# 상위 5개
topk(5, sum by (instance) (rate(http_requests_total[5m])))

# 분위수
quantile by (job) (0.95, http_request_duration_seconds_bucket)
```

<a id="built-in-함수"></a>

### Built-in 함수

#### 시계열 함수

- `rate(v[d])`: 평균 초당 증가율(Counter)
- `irate(v[d])`: 마지막 두 데이터로 즉시 비율
- `increase(v[d])`: 시간 윈도우 내 증가량
- `delta(v[d])`: Gauge의 변화량
- `idelta(v[d])`: Gauge의 마지막 변화량
- `deriv(v[d])`: Gauge의 도함수
- `predict_linear(v[d], t)`: t초 후 예측값
- `resets(v[d])`: Counter 재시작 횟수
- `changes(v[d])`: Gauge 변경 횟수

#### 시간 윈도우 집계

- `avg_over_time(v[d])`: 평균
- `min_over_time(v[d])`: 최소
- `max_over_time(v[d])`: 최대
- `sum_over_time(v[d])`: 합계
- `count_over_time(v[d])`: 개수
- `quantile_over_time(q, v[d])`: 분위수
- `stddev_over_time(v[d])`: 표준편차
- `last_over_time(v[d])`: 마지막 값
- `present_over_time(v[d])`: 존재 여부(1)
- `absent_over_time(v[d])`: 부재 여부

#### 히스토그램 함수

```promql
# P95 응답시간
histogram_quantile(0.95,
  sum by (le) (rate(http_request_duration_seconds_bucket[5m]))
)

# Native Histogram
histogram_quantile(0.99, sum by (le) (rate(my_native_histogram[5m])))
```

#### 라벨 조작

```promql
# 라벨 추가/변경
label_replace(v, "dst", "$1", "src", "(.*)")

# 라벨 결합
label_join(v, "dst", "-", "src1", "src2")
```

#### 시간 함수

```promql
time()                    # 현재 Unix 시간
timestamp(v)              # 시계열 샘플 시간
year(v)                   # 연도
month(v)                  # 월
day_of_month(v)
day_of_week(v)
hour(v)
minute(v)
days_in_month(v)
```

#### 수학 함수

```promql
abs, ceil, floor, round, exp, ln, log2, log10, sqrt, sgn
sin, cos, tan, asin, acos, atan, sinh, cosh, tanh
deg, rad, pi
```

#### 정렬

```promql
sort(v)        # 오름차순
sort_desc(v)   # 내림차순
```

#### 클램프

```promql
clamp(v, min, max)
clamp_min(v, min)
clamp_max(v, max)
```

<a id="recording-rules"></a>

### Recording Rules

Recording Rule은 자주 사용하는 쿼리를 미리 계산해 새 메트릭으로 저장한다. 아래 예제는 요청 증가율과 P95 응답 시간을 계산하는 룰이다.

#### 룰 파일

```yaml
groups:
  - name: api_recording
    interval: 30s
    rules:
      - record: api:requests:rate5m
        expr: sum by (job, instance) (rate(http_requests_total[5m]))
      
      - record: api:request_duration:p95
        expr: |
          histogram_quantile(0.95,
            sum by (job, le) (rate(http_request_duration_seconds_bucket[5m]))
          )
```

#### Mimir Ruler에 등록

```bash
mimirtool rules load --address=http://mimir:9009 \
  --id=tenant-1 \
  recording-rules.yaml
```

<a id="mimir-http-api"></a>

### Mimir HTTP API

- Prometheus HTTP API와 호환

#### Instant Query

```bash
curl -G "http://mimir:9009/prometheus/api/v1/query" \
  -H "X-Scope-OrgID: tenant-1" \
  --data-urlencode 'query=up' \
  --data-urlencode 'time=1700000000'
```

#### Range Query

```bash
curl -G "http://mimir:9009/prometheus/api/v1/query_range" \
  -H "X-Scope-OrgID: tenant-1" \
  --data-urlencode 'query=rate(http_requests_total[5m])' \
  --data-urlencode 'start=1700000000' \
  --data-urlencode 'end=1700003600' \
  --data-urlencode 'step=15s'
```

#### Series

```bash
curl -G "http://mimir:9009/prometheus/api/v1/series" \
  -H "X-Scope-OrgID: tenant-1" \
  --data-urlencode 'match[]={__name__=~"http_.*"}' \
  --data-urlencode 'start=1700000000' \
  --data-urlencode 'end=1700003600'
```

#### Labels

```bash
# 모든 라벨 이름
curl "http://mimir:9009/prometheus/api/v1/labels" \
  -H "X-Scope-OrgID: tenant-1"

# 특정 라벨 값
curl "http://mimir:9009/prometheus/api/v1/label/job/values" \
  -H "X-Scope-OrgID: tenant-1"
```

#### 메타데이터

```bash
curl "http://mimir:9009/prometheus/api/v1/metadata" \
  -H "X-Scope-OrgID: tenant-1"
```

#### Cardinality 분석

```bash
# 라벨 카디널리티
curl -G "http://mimir:9009/prometheus/api/v1/cardinality/label_names" \
  -H "X-Scope-OrgID: tenant-1"

# 활성 시계열 통계
curl -G "http://mimir:9009/prometheus/api/v1/cardinality/active_series" \
  -H "X-Scope-OrgID: tenant-1" \
  --data-urlencode 'selector={job="prometheus"}'
```

<a id="native-histograms"></a>

### Native Histograms

- Prometheus 2.40+에서 도입 → Mimir에서 완전히 지원

#### 장점

- 고해상도 분포 (자동 버킷)
- 적은 시계열 (단일 시계열로 모든 분위수)
- 빠른 쿼리

#### 활성화

```yaml
limits:
  native_histograms_ingestion_enabled: true
```

#### 쿼리

```promql
# 클래식 히스토그램과 동일
histogram_quantile(0.95, sum by (le) (rate(my_metric[5m])))

# Native Histogram 전용
histogram_count(rate(my_metric[5m]))
histogram_sum(rate(my_metric[5m]))
histogram_avg(rate(my_metric[5m]))
histogram_fraction(0, 100, rate(my_metric[5m]))
```

<a id="exemplars"></a>

### Exemplars

Exemplar는 메트릭 데이터 포인트와 트레이스를 연결한다. 이를 활용하면 메트릭에서 이상을 발견했을 때 관련 트레이스로 이동해 요청의 실행 과정을 살펴볼 수 있다.

#### 활성화

```yaml
limits:
  max_global_exemplars_per_user: 100000

ingester:
  max_exemplars: 100000
```

#### Prometheus에서 생성

- OpenMetrics 형식 메트릭에 `# {trace_id="abc"} value`를 추가

#### 쿼리

```bash
curl -G "http://mimir:9009/prometheus/api/v1/query_exemplars" \
  -H "X-Scope-OrgID: tenant-1" \
  --data-urlencode 'query=http_request_duration_seconds_bucket' \
  --data-urlencode 'start=1700000000' \
  --data-urlencode 'end=1700003600'
```

- 응답에 `exemplars` 필드 포함

#### Grafana 통합

Prometheus/Mimir 데이터 소스에서 Exemplars를 활성화하고 Tempo 데이터 소스를 연결한다. 이후 차트의 Exemplar를 클릭하면 해당 트레이스로 이동할 수 있다.
