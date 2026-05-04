# Mimir 시각화 (Grafana 통합)

> 이 문서는 Grafana Mimir 공식 문서의 "Visualize data" 섹션을 한국어로 정리한 것입니다.
> 원본: https://grafana.com/docs/mimir/latest/visualize/

---

## 목차

1. [개요](#개요)
2. [Mimir 데이터소스 추가](#mimir-데이터소스-추가)
3. [Explore 사용](#explore-사용)
4. [Native Histograms 시각화](#native-histograms-시각화)
5. [Exemplars (트레이스 연결)](#exemplars-트레이스-연결)
6. [대시보드 구성](#대시보드-구성)
7. [데이터소스 간 연결](#데이터소스-간-연결)
8. [공식 대시보드](#공식-대시보드)
9. [Recording Rules와 대시보드](#recording-rules와-대시보드)

---

## 개요

Mimir는 Prometheus HTTP API와 호환되므로, Prometheus 데이터소스로 등록 가능합니다.

추가 기능:
- Native Histograms 시각화
- Exemplars로 트레이스 연결
- Cardinality 분석 UI
- 멀티 테넌트 헤더 지원

---

## Mimir 데이터소스 추가

### UI에서

1. **Connections** → **Data sources** → **Add new**
2. **Prometheus** 선택 (Mimir는 Prometheus 호환)
3. URL: `http://mimir:9009/prometheus`
4. Custom HTTP Headers: `X-Scope-OrgID: tenant-1`
5. **Save & test**

### Provisioning

```yaml
# /etc/grafana/provisioning/datasources/mimir.yaml
apiVersion: 1

datasources:
  - name: Mimir
    type: prometheus
    access: proxy
    url: http://mimir:9009/prometheus
    uid: mimir
    isDefault: true
    
    jsonData:
      # 멀티 테넌시
      httpHeaderName1: X-Scope-OrgID
      
      # 쿼리 옵션
      httpMethod: POST
      manageAlerts: false           # Mimir Ruler 사용
      prometheusType: Mimir
      prometheusVersion: 2.50.0
      
      # 시간 범위
      timeInterval: 15s
      queryTimeout: 60s
      
      # Exemplars
      exemplarTraceIdDestinations:
        - name: trace_id
          datasourceUid: tempo
          urlDisplayLabel: 'View Trace'
      
      # Mimir 추가 기능
      cacheLevel: High
      incrementalQuerying: true
      incrementalQueryOverlapWindow: 10m
      
      # Cardinality
      disableRecordingRules: false
    
    secureJsonData:
      httpHeaderValue1: tenant-1
      basicAuthPassword: ${MIMIR_PASSWORD}
```

### Grafana Cloud의 경우

```yaml
url: https://prometheus-blocks-prod-us-central1.grafana.net/api/prom
basicAuth: true
basicAuthUser: '12345'
secureJsonData:
  basicAuthPassword: ${GRAFANA_CLOUD_API_KEY}
```

---

## Explore 사용

### PromQL 입력

1. **Explore** → 데이터소스 **Mimir**
2. PromQL 입력:
```promql
rate(http_requests_total[5m])
```

### Builder 모드

UI로 메트릭/라벨/연산 클릭하여 작성. PromQL 모르는 사용자도 사용.

### Code 모드

직접 PromQL 작성. 자동완성 지원.

### 결과 보기

- **Graph**: 시계열 그래프
- **Table**: 테이블 형식
- **Time series**: 패널
- **Heatmap**: Native Histogram에 적합

### Range vs Instant

- **Range**: 시간 범위 (시계열)
- **Instant**: 단일 시점 값

---

## Native Histograms 시각화

### Heatmap 패널

가장 적합한 시각화:

1. Add visualization → **Heatmap**
2. 데이터소스: Mimir
3. 쿼리:
```promql
sum by (le) (
  rate(http_request_duration_seconds_bucket[5m])
)
```
4. 옵션:
   - **Calculate from data**: Y-Buckets
   - **Cell type**: Heatmap

### Native Histogram 자동 감지

Native Histogram이면 Grafana가 자동으로 적절한 시각화 제공.

```promql
# Native Histogram
my_native_histogram

# Bucket 분포 시각화
histogram_quantile(0.95, sum by (le) (rate(my_native_histogram[5m])))
histogram_quantile(0.99, sum by (le) (rate(my_native_histogram[5m])))
```

### 분위수 시각화

```promql
# 동시에 여러 분위수
histogram_quantile(0.50, sum by (le) (rate(my_metric[5m])))
histogram_quantile(0.95, sum by (le) (rate(my_metric[5m])))
histogram_quantile(0.99, sum by (le) (rate(my_metric[5m])))
```

---

## Exemplars (트레이스 연결)

### Exemplars 활성화

#### Mimir 측

```yaml
limits:
  max_global_exemplars_per_user: 100000

ingester:
  max_exemplars: 100000
```

#### Prometheus 메트릭 코드

```go
// Go 예시
histogram.(prometheus.ExemplarObserver).ObserveWithExemplar(
    duration.Seconds(),
    prometheus.Labels{"trace_id": traceID},
)
```

### Grafana 데이터소스 설정

```yaml
exemplarTraceIdDestinations:
  - name: trace_id
    datasourceUid: tempo
    urlDisplayLabel: 'View Trace'
```

### 시각화

차트의 점이 Exemplar 표시:

```
시계열 그래프
   ●     ●  ←  Exemplar (trace_id 포함)
  ╱│ ╲ ╱│
 ╱ │  ╳ │
   │ ╱╲ │
```

점을 클릭하면 → Tempo에서 트레이스 조회.

### PromQL Range Query

```bash
curl -G "http://mimir:9009/prometheus/api/v1/query_range" \
  -H "X-Scope-OrgID: tenant-1" \
  --data-urlencode 'query=histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[5m]))' \
  --data-urlencode 'start=1700000000' \
  --data-urlencode 'end=1700003600' \
  --data-urlencode 'step=15s'
```

응답에 `exemplars` 필드 포함.

---

## 대시보드 구성

### 변수 (Variables)

```
$cluster = label_values(up, cluster)
$namespace = label_values(up{cluster="$cluster"}, namespace)
$service = label_values(up{cluster="$cluster", namespace="$namespace"}, app)
$instance = label_values(up{cluster="$cluster", namespace="$namespace", app="$service"}, instance)
```

### 자주 쓰는 패널

#### 1. RED 메트릭 (Rate, Errors, Duration)

**Rate**
```promql
sum by (service) (
  rate(http_requests_total{cluster="$cluster"}[5m])
)
```

**Errors**
```promql
sum by (service) (
  rate(http_requests_total{cluster="$cluster", status=~"5.."}[5m])
)
/
sum by (service) (
  rate(http_requests_total{cluster="$cluster"}[5m])
)
```

**Duration P95**
```promql
histogram_quantile(0.95,
  sum by (service, le) (
    rate(http_request_duration_seconds_bucket{cluster="$cluster"}[5m])
  )
)
```

#### 2. USE 메트릭 (Utilization, Saturation, Errors)

**CPU Utilization**
```promql
1 - avg by (instance) (
  rate(node_cpu_seconds_total{mode="idle"}[5m])
)
```

**Memory Utilization**
```promql
1 - (
  node_memory_MemAvailable_bytes
  /
  node_memory_MemTotal_bytes
)
```

**Disk Saturation**
```promql
node_disk_io_time_weighted_seconds_total
```

#### 3. SLO 패널

**SLO 준수율**
```promql
(
  sum(rate(http_requests_total{status!~"5.."}[30d]))
  /
  sum(rate(http_requests_total[30d]))
)
> 0.999
```

**Error Budget 소진율**
```promql
1 - (
  (1 - 0.999)        # 허용 에러
  -
  (
    sum(increase(http_requests_total{status=~"5.."}[30d]))
    /
    sum(increase(http_requests_total[30d]))
  )
) / (1 - 0.999)
```

### 패널 옵션

#### Thresholds

```yaml
thresholds:
  steps:
    - value: null
      color: green
    - value: 0.95
      color: yellow
    - value: 0.99
      color: red
```

#### Color Scheme

- **Green-Yellow-Red**: 좋음/주의/나쁨
- **By Value**: 값에 따라 자동
- **Classic Palette**: 시리즈별 다른 색

---

## 데이터소스 간 연결

### Metrics → Logs

#### Exemplar 활용

차트 점 클릭 → Tempo에서 trace_id 조회 → Loki에서 같은 trace_id 로그.

#### 데이터 링크 (Data Links)

```yaml
# 패널 설정
links:
  - title: View Logs
    url: '/explore?orgId=1&left=["now-1h","now","Loki",{"expr":"{service=\"$service\"}"}]'
```

### Metrics → Traces

```yaml
exemplarTraceIdDestinations:
  - name: trace_id
    datasourceUid: tempo
    urlDisplayLabel: 'View Trace'
  - name: traceID
    datasourceUid: tempo
```

### 변수로 자동 연결

대시보드 변수 `$service`를 모든 패널/링크에 일관되게 사용:

```
Metrics 패널: rate(http_requests_total{service="$service"}[5m])
Logs 링크: /explore?...{service="$service"}
Traces 링크: /explore?...resource.service.name="$service"
```

---

## 공식 대시보드

### Mimir Mixin

```bash
jb install github.com/grafana/mimir/operations/mimir-mixin@main
jsonnet -J vendor mixin.libsonnet > dashboards.json
```

포함:
- **Mimir / Writes**: 수집 경로
- **Mimir / Reads**: 쿼리 경로
- **Mimir / Queries**: 쿼리 분석
- **Mimir / Object Store**: 스토리지
- **Mimir / Compactor**: 압축
- **Mimir / Ruler**: 룰 평가
- **Mimir / Alertmanager**: 알림
- **Mimir / Tenants**: 테넌트별
- **Mimir / Top Tenants**: 상위 사용자
- **Mimir / Slow Queries**: 느린 쿼리
- **Mimir / Resources**: 리소스 사용
- **Mimir / Networking**: 네트워크
- **Mimir / Overview**: 종합

### Grafana Cloud

Grafana Cloud Mimir는 자동으로 모든 Mixin 대시보드를 사전 설치.

### Import

```
Dashboards → Import → grafana.com → ID 입력
```

추천 Mimir 대시보드 ID:
- 17775: Mimir / Writes
- 17776: Mimir / Reads
- 17779: Mimir / Object Store

---

## Recording Rules와 대시보드

### 미리 계산된 메트릭

대시보드에서 자주 쓰는 무거운 쿼리는 Recording Rules로:

```yaml
groups:
  - name: api_dashboard_recording
    interval: 30s
    rules:
      - record: cluster:api_request_rate:5m
        expr: |
          sum by (cluster, service) (
            rate(http_requests_total[5m])
          )
      
      - record: cluster:api_error_rate:5m
        expr: |
          sum by (cluster, service) (
            rate(http_requests_total{status=~"5.."}[5m])
          )
          /
          sum by (cluster, service) (
            rate(http_requests_total[5m])
          )
      
      - record: cluster:api_latency:p95:5m
        expr: |
          histogram_quantile(0.95,
            sum by (cluster, service, le) (
              rate(http_request_duration_seconds_bucket[5m])
            )
          )
```

### 대시보드에서 사용

```promql
# 무거운 원본 쿼리 대신:
cluster:api_latency:p95:5m{cluster="$cluster"}
```

빠른 대시보드 로딩.

### 명명 규칙

```
<aggregation_level>:<metric_name>:<duration>

예시:
- cluster:api_requests:5m
- namespace:cpu_usage:rate1m
- pod:memory_usage:max
```
