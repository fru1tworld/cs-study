# Loki 시각화 (Grafana 통합)

> 이 문서는 Grafana Loki 공식 문서의 Visualization 섹션을 한국어로 정리한 것입니다.
> 원본: https://grafana.com/docs/loki/latest/visualize/grafana/

---

## 목차

1. [개요](#개요)
2. [Loki 데이터소스 추가](#loki-데이터소스-추가)
3. [Explore 사용법](#explore-사용법)
4. [대시보드 생성](#대시보드-생성)
5. [패널 종류별 LogQL 활용](#패널-종류별-logql-활용)
6. [데이터소스 간 연결](#데이터소스-간-연결)
7. [Annotation](#annotation)
8. [Grafana Alerting 연동](#grafana-alerting-연동)
9. [공식 대시보드](#공식-대시보드)

---

## 개요

Grafana는 Loki의 기본 시각화 도구입니다. 다음을 지원합니다.

- **Explore**: 임시 쿼리 및 탐색
- **Dashboards**: 패널과 변수로 구성된 대시보드
- **Alerting**: LogQL 기반 알림
- **Drilldown**: 메트릭 → 트레이스 → 로그 연결

---

## Loki 데이터소스 추가

### UI에서 추가

1. Grafana → **Connections** → **Data sources** → **Add new data source**
2. **Loki** 선택
3. URL 입력: `http://loki:3100`
4. 인증 설정 (필요시)
5. **Save & test**

### Provisioning (자동 설정)

```yaml
# /etc/grafana/provisioning/datasources/loki.yaml
apiVersion: 1

datasources:
  - name: Loki
    type: loki
    access: proxy
    url: http://loki:3100
    jsonData:
      maxLines: 1000
      timeout: 60
      derivedFields:
        - name: TraceID
          matcherRegex: 'trace_id=(\w+)'
          url: '$${__value.raw}'
          datasourceUid: tempo
    secureJsonData:
      httpHeaderValue1: 'tenant-1'
    httpHeaderName1: 'X-Scope-OrgID'
    isDefault: false
    editable: true
    version: 1
```

### 멀티 테넌시 헤더

```yaml
jsonData:
  httpHeaderName1: X-Scope-OrgID
secureJsonData:
  httpHeaderValue1: tenant-1
```

### 인증

#### Basic Auth

```yaml
basicAuth: true
basicAuthUser: user
secureJsonData:
  basicAuthPassword: pass
```

#### TLS

```yaml
jsonData:
  tlsAuth: true
  tlsAuthWithCACert: true
secureJsonData:
  tlsCACert: |
    -----BEGIN CERTIFICATE-----
    ...
  tlsClientCert: |
    -----BEGIN CERTIFICATE-----
    ...
  tlsClientKey: |
    -----BEGIN PRIVATE KEY-----
    ...
```

---

## Explore 사용법

### 기본 사용

1. 좌측 사이드바 → **Explore**
2. 데이터소스: **Loki** 선택
3. 쿼리 입력 또는 빌더 사용
4. **Run query**

### 쿼리 빌더

UI로 라벨/필터/파서/연산자를 클릭하여 쿼리 작성. LogQL을 모르는 사용자도 사용 가능.

### Logs 패널 기능

- **Live tailing**: 실시간 로그 스트리밍 (WebSocket)
- **Wrapping**: 긴 라인 줄바꿈
- **Time order**: 오름차순/내림차순
- **Show context**: 특정 라인 주변 로그
- **Pretty Print JSON**: JSON 포맷팅
- **Histogram**: 시간별 분포

### Logs to Metrics 변환

```logql
# 로그 → 메트릭
sum(rate({app="api"} [5m]))

# 결과를 그래프로 자동 시각화
```

### Split View

화면 분할로 두 데이터소스 동시 비교 (예: 메트릭 + 로그).

---

## 대시보드 생성

### 패널 추가

1. 대시보드 → **Add visualization**
2. 데이터소스 선택
3. LogQL 쿼리 입력
4. 시각화 타입 선택

### 변수 (Variables)

대시보드 변수로 동적 필터링:

```
변수 이름: namespace
타입: Query
데이터소스: Loki
쿼리: label_values(namespace)
```

쿼리에서 사용:
```logql
{namespace="$namespace", app=~".+"}
```

### 시간 범위 변수

`$__from`, `$__to`, `$__interval`, `$__range` 등 자동 변수 활용.

---

## 패널 종류별 LogQL 활용

### Logs 패널

원본 로그 표시:

```logql
{namespace="$namespace"} |= "$search"
```

### Time Series

메트릭 형식 결과:

```logql
sum by (level) (rate({namespace="$namespace"}[5m]))
```

### Stat / Single Stat

단일 값:

```logql
sum(rate({namespace="$namespace"} |= "error" [5m]))
```

### Bar Chart / Pie Chart

```logql
sum by (level) (count_over_time({namespace="$namespace"}[$__range]))
```

### Table

원본 로그 테이블:

```logql
{namespace="$namespace"} | json | line_format "{{.method}} {{.path}}"
```

### Heatmap

응답 시간 분포:

```logql
sum by (le) (
  rate(
    {app="api"}
    | json
    | unwrap response_time_ms
    | __error__=""
    [5m]
  )
)
```

### Logs Volume

상단에 자동 표시되는 로그 양 차트. Volume API 사용.

---

## 데이터소스 간 연결

### Logs ↔ Traces (Loki ↔ Tempo)

#### Loki → Tempo (Derived Fields)

```yaml
# Loki 데이터소스 설정
jsonData:
  derivedFields:
    - name: TraceID
      matcherRegex: '"trace_id":"(\w+)"'
      url: '$${__value.raw}'
      datasourceUid: tempo-uid
    
    - name: TraceID
      matcherRegex: 'trace_id=(\w+)'
      url: '$${__value.raw}'
      datasourceUid: tempo-uid
```

로그에서 `trace_id`를 클릭하면 자동으로 Tempo에서 트레이스 조회.

#### Tempo → Loki (Trace to Logs)

```yaml
# Tempo 데이터소스 설정
jsonData:
  tracesToLogsV2:
    datasourceUid: loki-uid
    spanStartTimeShift: '-1m'
    spanEndTimeShift: '1m'
    tags:
      - key: 'service.name'
        value: 'service'
    customQuery: true
    query: '{$${__tags}} |~ "$${__span.traceId}"'
```

### Logs ↔ Metrics (Loki ↔ Mimir/Prometheus)

#### Logs to Metrics

LogQL 메트릭 쿼리로 직접 변환:

```logql
sum(rate({app="api"} |= "error" [5m]))
```

#### Metrics to Logs (Exemplars)

Prometheus 메트릭에 trace ID exemplar이 있으면 → 트레이스 → 관련 로그.

---

## Annotation

대시보드에 이벤트 표시.

### 설정

대시보드 설정 → **Annotations** → **Add annotation query**

데이터소스: Loki

쿼리:
```logql
{app="deploy"} |= "deployment completed"
```

이름 템플릿:
```
{{labels.app}} 배포 완료
```

이러면 모든 패널에 배포 시점이 세로선으로 표시됨.

### 활용 예

- 배포 이벤트
- 인시던트 시작/종료
- 점검 시간

---

## Grafana Alerting 연동

### 새로운 Unified Alerting (Grafana 8.0+)

LogQL로 알림 정의 가능.

#### 알림 룰 작성

1. **Alerting** → **Alert rules** → **New rule**
2. 데이터소스: Loki 선택
3. 쿼리:
```logql
sum(rate({namespace="prod"} |= "error" [5m]))
```
4. 조건:
```
WHEN last() OF query(A, 5m, now) IS ABOVE 10
```
5. 평가 주기, for, 라벨, 주석 설정
6. Contact Point 연결 (Slack, Email 등)

#### Loki Ruler vs Grafana Alerting

| 측면 | Loki Ruler | Grafana Alerting |
|------|-----------|-----------------|
| 평가 위치 | Loki 자체 | Grafana 외부 |
| 룰 관리 | YAML 파일 / API | UI / Provisioning |
| 백엔드 통합 | Alertmanager | Grafana Contact Points |
| Recording | Mimir/Prometheus로 보냄 | 옵션 |
| 권장 | 대규모/표준 | 소규모/시각적 관리 |

---

## 공식 대시보드

### Loki Mixin

[Loki Mixin](https://github.com/grafana/loki/tree/main/production/loki-mixin) 에서 제공:

```bash
jb install github.com/grafana/loki/production/loki-mixin@main
jsonnet -J vendor mixin.libsonnet > dashboards.json
```

포함:
- **Loki / Operational**: 컴포넌트별 운영 메트릭
- **Loki / Reads**: 쿼리 경로
- **Loki / Writes**: 수집 경로
- **Loki / Resources**: CPU/메모리/네트워크
- **Loki / Chunks**: 청크 관련
- **Loki / Object Store**: 스토리지 사용량
- **Loki / Logs**: Loki 자체 로그

### Grafana.com 대시보드

[Grafana Dashboards](https://grafana.com/grafana/dashboards/?search=loki) 에서 검색.

추천:
- ID 13186: Loki Logs/Metrics Dashboard
- ID 12611: Loki Stack Monitoring (Promtail, Loki, Cortex/Mimir)
- ID 14055: Loki Operational

### Import

```
Grafana → Dashboards → Import → Import via grafana.com → ID 입력
```

### 커스텀 대시보드 예시 패널

#### 1. 로그 수집 속도

```logql
sum by (job) (rate({}[5m]))
```

#### 2. 에러 발생 시각화

```logql
sum by (namespace) (
  rate({namespace=~".+"} |~ "(?i)error" [5m])
)
```

#### 3. 응답 시간 P95

```logql
quantile_over_time(0.95,
  {app="api"}
  | json
  | unwrap duration_ms
  [5m]
)
```

#### 4. 상위 N개 로그 양

```logql
topk(10,
  sum by (app) (
    sum_over_time(({namespace=~".+"} | label_format size=`{{.size}}`)[5m])
  )
)
```

#### 5. 로그 레벨 분포

```logql
sum by (level) (
  count_over_time({namespace="$namespace"} | json [$__range])
)
```
