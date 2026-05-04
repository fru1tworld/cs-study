# Tempo 시각화 (Grafana 통합)

> 이 문서는 Grafana Tempo 공식 문서의 Visualization 섹션을 한국어로 정리한 것입니다.
> 원본: https://grafana.com/docs/tempo/latest/getting-started/tempo-in-grafana/

---

## 목차

1. [개요](#개요)
2. [Tempo 데이터소스 추가](#tempo-데이터소스-추가)
3. [Explore에서 트레이스 보기](#explore에서-트레이스-보기)
4. [Service Graph](#service-graph)
5. [Node Graph](#node-graph)
6. [Span 패널](#span-패널)
7. [데이터소스 간 연결](#데이터소스-간-연결)
8. [Traces Drilldown (Plugin)](#traces-drilldown-plugin)
9. [APM 대시보드](#apm-대시보드)
10. [공식 대시보드](#공식-대시보드)

---

## 개요

Tempo와 Grafana 통합으로:

- **트레이스 검색** (TraceID, TraceQL)
- **트레이스 시각화** (Span 시계열, Timeline)
- **Service Graph** (자동 의존성 그래프)
- **로그/메트릭 연결** (Trace ↔ Logs ↔ Metrics)
- **APM 대시보드** (Span Metrics)

---

## Tempo 데이터소스 추가

### UI에서

1. **Connections** → **Data sources** → **Add new**
2. **Tempo** 선택
3. URL: `http://tempo:3200`
4. **Save & test**

### Provisioning

```yaml
# /etc/grafana/provisioning/datasources/tempo.yaml
apiVersion: 1

datasources:
  - name: Tempo
    type: tempo
    access: proxy
    url: http://tempo:3200
    uid: tempo
    
    jsonData:
      # 멀티 테넌시
      httpHeaderName1: X-Scope-OrgID
      
      # Trace to logs (Tempo → Loki)
      tracesToLogsV2:
        datasourceUid: loki
        spanStartTimeShift: '-1m'
        spanEndTimeShift: '1m'
        tags:
          - key: 'service.name'
            value: 'service'
          - key: 'k8s.namespace.name'
            value: 'namespace'
        filterByTraceID: true
        filterBySpanID: false
        customQuery: true
        query: '{$${__tags}} |= "$${__span.traceId}"'
      
      # Trace to metrics (Tempo → Mimir)
      tracesToMetrics:
        datasourceUid: mimir
        spanStartTimeShift: '-2m'
        spanEndTimeShift: '2m'
        tags:
          - key: 'service.name'
            value: 'service'
        queries:
          - name: 'Sample query'
            query: 'sum(rate(traces_spanmetrics_calls_total{$${__tags}}[5m]))'
      
      # Service Map
      serviceMap:
        datasourceUid: mimir
      
      # Node Graph
      nodeGraph:
        enabled: true
      
      # Span bar (스팬 표시 옵션)
      spanBar:
        type: 'Tag'
        tag: 'http.method'
      
      # Streaming search (실험적)
      streamingEnabled:
        search: true
        metrics: true
      
      # Trace Drilldown
      traceQuery:
        timeShiftEnabled: true
        spanStartTimeShift: '-1h'
        spanEndTimeShift: '1h'
    
    secureJsonData:
      httpHeaderValue1: 'tenant-1'
```

---

## Explore에서 트레이스 보기

### Query Type

Tempo Explore는 4가지 쿼리 타입 제공:

| 타입 | 용도 |
|------|------|
| **Search** | TraceQL로 검색 |
| **TraceID** | 특정 트레이스 직접 조회 |
| **Service Graph** | 서비스 그래프 |
| **Search (legacy)** | key=value 형식 (구식) |

### TraceQL Search

```traceql
{ resource.service.name = "frontend" && status = error && duration > 100ms }
```

#### Query Builder

UI로 조건 클릭하며 작성:
- 서비스
- 스팬 이름
- 속성
- 시간/지속
- 상태

### TraceID 조회

직접 입력하거나 다른 데이터에서 클릭으로 이동.

### 결과 표시

검색 결과:
- 트레이스 ID
- Root Service Name
- Root Span Name
- Start Time
- Duration

행 클릭 → 트레이스 상세 (Timeline view).

---

## Service Graph

### 자동 생성

Tempo Metrics Generator가 활성화되어 있으면 자동으로 서비스 그래프 메트릭 생성.

### Grafana에서 보기

Explore → Tempo → **Service Graph** 탭.

### 그래프 요소

```
┌──────────┐  요청 수    ┌──────────┐
│ frontend │ ─────────> │   api    │
└──────────┘  에러율    └──────────┘
                │ P95 지연    │
                └──────────────┘
                   │
                   v
           ┌──────────────┐
           │  postgresql  │
           └──────────────┘
```

각 노드/엣지에 마우스 오버하면 메트릭 표시:
- 분당 요청 수
- 에러율
- P50/P95/P99 지연

### 노드 클릭 → 액션

- 해당 서비스의 트레이스 검색
- Span Metrics 패널로 이동
- 관련 로그 조회 (Loki 연결 시)

---

## Node Graph

대시보드 패널로도 사용 가능.

### 패널 추가

1. Add visualization → **Node Graph**
2. 데이터소스: Tempo (또는 Mimir)
3. 쿼리:
```promql
# Mimir 데이터소스 사용 시
sum by (client, server) (
  rate(traces_service_graph_request_total[5m])
)
```

### 활용

- 대시보드에 영구 표시
- 시간 범위에 따른 변화 관찰
- 변수와 결합 (네임스페이스/클러스터별)

---

## Span 패널

### Trace View 패널

대시보드에 트레이스를 직접 표시.

```yaml
# 패널 쿼리
queryType: traceID
query: ${__data.fields.traceID}
```

### Span 시계열 (Span Bar Display)

각 스팬을 가로 막대로 표시. 시간 축으로 정렬.

### Span 속성 표시

`spanBar` 설정으로 막대에 추가 정보 표시:

```yaml
spanBar:
  type: Tag           # None, Duration, Tag
  tag: http.method
```

각 스팬 옆에 HTTP method 표시.

---

## 데이터소스 간 연결

### Logs ↔ Traces

#### Tempo → Loki (Trace to Logs)

트레이스 보다가 관련 로그로 이동:

```yaml
tracesToLogsV2:
  datasourceUid: loki
  spanStartTimeShift: '-1m'
  spanEndTimeShift: '1m'
  tags:
    - key: service.name
      value: service
  customQuery: true
  query: '{$${__tags}} |= "$${__span.traceId}"'
```

스팬 우측 메뉴 → **Logs for this span** 클릭 → Loki 쿼리로 이동.

#### Loki → Tempo (Derived Fields)

로그에서 trace_id 클릭 → 트레이스로 이동:

```yaml
# Loki 데이터소스
derivedFields:
  - name: TraceID
    matcherRegex: '"trace_id":"(\w+)"'
    url: '$${__value.raw}'
    datasourceUid: tempo
```

### Metrics ↔ Traces

#### Tempo → Mimir (Trace to Metrics)

```yaml
tracesToMetrics:
  datasourceUid: mimir
  spanStartTimeShift: '-2m'
  spanEndTimeShift: '2m'
  tags:
    - key: service.name
      value: service
  queries:
    - name: Request Rate
      query: 'sum(rate(traces_spanmetrics_calls_total{$${__tags}}[5m]))'
    - name: Error Rate
      query: 'sum(rate(traces_spanmetrics_calls_total{$${__tags}, status_code="STATUS_CODE_ERROR"}[5m]))'
```

#### Mimir → Tempo (Exemplars)

메트릭 차트의 점에 trace ID 포함:

```yaml
# Mimir 데이터소스
exemplarTraceIdDestinations:
  - name: trace_id
    datasourceUid: tempo
```

차트 점을 클릭하면 해당 트레이스로 이동.

---

## Traces Drilldown (Plugin)

### 개요

쿼리 작성 없이 클릭으로 트레이스 탐색하는 Grafana 플러그인.

### 설치

```bash
grafana-cli plugins install grafana-exploretraces-app
```

또는 Grafana Cloud에서 자동 활성화.

### 사용

1. Grafana → **Explore** → **Traces Drilldown**
2. 시간 범위 선택
3. 자동 표시:
   - 서비스 목록과 RED 메트릭
   - 가장 느린/에러 많은 서비스 강조
4. 서비스 클릭 → 해당 서비스의 스팬 분석
5. 스팬 클릭 → 트레이스 상세

### 활용

- 사전 지식 없이 시스템 파악
- 이상 자동 강조
- 문제 디버깅 시작점

---

## APM 대시보드

### Tempo + Span Metrics 활용

Metrics Generator로 생성된 메트릭으로 APM 대시보드 구성.

### 핵심 패널

#### 1. RED 메트릭 (서비스별)

**Rate**:
```promql
sum by (service) (
  rate(traces_spanmetrics_calls_total[5m])
)
```

**Errors**:
```promql
sum by (service) (
  rate(traces_spanmetrics_calls_total{status_code="STATUS_CODE_ERROR"}[5m])
)
/
sum by (service) (
  rate(traces_spanmetrics_calls_total[5m])
)
```

**Duration P95**:
```promql
histogram_quantile(0.95,
  sum by (service, le) (
    rate(traces_spanmetrics_latency_bucket[5m])
  )
)
```

#### 2. Service Graph 패널

Node Graph 패널 사용.

#### 3. Top N 느린 엔드포인트

```promql
topk(10,
  histogram_quantile(0.95,
    sum by (service, span_name, le) (
      rate(traces_spanmetrics_latency_bucket[5m])
    )
  )
)
```

#### 4. 의존성 변화 (변경 감지)

```promql
# 새 의존성 출현
sum by (client, server) (
  rate(traces_service_graph_request_total[5m])
)
unless
sum by (client, server) (
  rate(traces_service_graph_request_total[1h] offset 1h)
)
```

### 변수

```
$service = label_values(traces_spanmetrics_calls_total, service)
$cluster = label_values(traces_spanmetrics_calls_total, cluster)
```

---

## 공식 대시보드

### Tempo Mixin

```bash
jb install github.com/grafana/tempo/operations/tempo-mixin@main
jsonnet -J vendor mixin.libsonnet > dashboards.json
```

포함:
- **Tempo / Operational**: 컴포넌트 운영
- **Tempo / Reads**: 검색/조회 경로
- **Tempo / Writes**: 수집 경로
- **Tempo / Resources**: CPU/메모리/네트워크
- **Tempo / Tenants**: 테넌트별 사용량

### Grafana.com 대시보드

검색: https://grafana.com/grafana/dashboards/?search=tempo

추천:
- Tempo Operational
- APM Dashboard (Tempo + Mimir)
- Service Graph Dashboard

### Import

```
Dashboards → Import → grafana.com → ID 입력
```

### 커스텀 대시보드 모범 사례

1. **메트릭 + 트레이스 + 로그 한 화면**
   - 좌측: 시계열 메트릭
   - 중간: 트레이스 검색
   - 우측: 관련 로그
2. **시간 범위 동기화**: 모든 패널이 같은 시간 범위
3. **변수 활용**: `$service`, `$env` 등 드릴다운 가능
4. **Exemplar 활성화**: 메트릭에서 트레이스로 빠른 점프
