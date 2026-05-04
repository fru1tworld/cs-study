# Tempo HTTP API 레퍼런스

> 이 문서는 Grafana Tempo 공식 문서의 HTTP API 섹션을 한국어로 정리한 것입니다.
> 원본: https://grafana.com/docs/tempo/latest/api_docs/

---

## 목차

1. [개요](#개요)
2. [공통 사항](#공통-사항)
3. [트레이스 조회](#트레이스-조회)
4. [TraceQL 검색](#traceql-검색)
5. [태그/속성 조회](#태그속성-조회)
6. [TraceQL Metrics](#traceql-metrics)
7. [수집 API](#수집-api)
8. [관리/헬스 API](#관리헬스-api)
9. [Echo / Buildinfo](#echo--buildinfo)

---

## 개요

Tempo의 주요 HTTP API:

- 트레이스 조회 (TraceID 기반)
- TraceQL 검색
- 메트릭 쿼리 (TraceQL Metrics)
- 태그/속성 디스커버리

기본 포트: **3200**

---

## 공통 사항

### 인증 헤더

멀티 테넌시 활성화 시:
```
X-Scope-OrgID: <tenant-id>
```

### 시간 형식

| 파라미터 | 형식 |
|----------|------|
| `start`, `end` | Unix seconds 또는 RFC3339 |

### 응답 코드

| 코드 | 의미 |
|------|------|
| 200 | 성공 |
| 400 | 잘못된 쿼리 |
| 404 | 트레이스 없음 |
| 422 | 처리 불가 (한도 초과) |
| 429 | Rate Limit |
| 5xx | 서버 에러 |

---

## 트레이스 조회

### `GET /api/traces/<traceID>`

특정 TraceID 조회.

```bash
curl -H "X-Scope-OrgID: tenant-1" \
  http://tempo:3200/api/traces/2c1b3d8f9e7a4c6b
```

#### 응답

OTLP/JSON 형식:

```json
{
  "batches": [
    {
      "resource": {
        "attributes": [
          {"key": "service.name", "value": {"stringValue": "frontend"}}
        ]
      },
      "scopeSpans": [
        {
          "spans": [
            {
              "traceId": "...",
              "spanId": "...",
              "name": "HTTP GET /api/users",
              "startTimeUnixNano": "1700000000000000000",
              "endTimeUnixNano": "1700000000100000000",
              "attributes": [...]
            }
          ]
        }
      ]
    }
  ]
}
```

### `GET /api/v2/traces/<traceID>`

확장된 V2 응답 형식.

옵션:
```bash
?start=1700000000        # 검색 범위 시작
?end=1700003600          # 검색 범위 종료
```

### Trace 청크 응답

큰 트레이스는 여러 청크로 나뉘어 응답:

```json
{
  "trace": { "batches": [...] },
  "metrics": {
    "inspectedBytes": "1234567",
    "inspectedTraces": 5
  }
}
```

---

## TraceQL 검색

### `GET /api/search`

TraceQL로 트레이스 검색.

```bash
curl -G "http://tempo:3200/api/search" \
  -H "X-Scope-OrgID: tenant-1" \
  --data-urlencode 'q={ resource.service.name = "frontend" && status = error }' \
  --data-urlencode 'start=1700000000' \
  --data-urlencode 'end=1700003600' \
  --data-urlencode 'limit=20' \
  --data-urlencode 'spss=10'
```

| 파라미터 | 설명 |
|----------|------|
| `q` | TraceQL 쿼리 |
| `start` | 시작 시간 |
| `end` | 종료 시간 |
| `limit` | 반환할 트레이스 수 |
| `spss` | 트레이스당 최대 스팬 수 |

#### 응답

```json
{
  "traces": [
    {
      "traceID": "abc123...",
      "rootServiceName": "frontend",
      "rootTraceName": "HTTP GET /api/users",
      "startTimeUnixNano": "1700000000000000000",
      "durationMs": 150,
      "spanSet": {
        "spans": [
          {
            "spanID": "...",
            "startTimeUnixNano": "...",
            "durationNanos": "1000000",
            "attributes": [...]
          }
        ],
        "matched": 1
      }
    }
  ],
  "metrics": {
    "inspectedTraces": 1234,
    "inspectedBytes": "5678901",
    "completedJobs": 10,
    "totalJobs": 10
  }
}
```

### 레거시: `GET /api/search` (key=value)

오래된 형식, TraceQL 권장:

```bash
curl -G "http://tempo:3200/api/search" \
  --data-urlencode 'service.name=frontend' \
  --data-urlencode 'minDuration=100ms' \
  --data-urlencode 'start=1700000000' \
  --data-urlencode 'end=1700003600'
```

---

## 태그/속성 조회

### `GET /api/search/tags`

사용 가능한 태그 키 목록.

```bash
curl -G "http://tempo:3200/api/search/tags" \
  -H "X-Scope-OrgID: tenant-1" \
  --data-urlencode 'scope=resource'   # span, resource, intrinsic, all
```

응답:
```json
{
  "tagNames": ["service.name", "service.namespace", "cluster", "region"]
}
```

### `GET /api/v2/search/tags`

V2: scope별로 분리된 응답.

```bash
curl -G "http://tempo:3200/api/v2/search/tags" \
  -H "X-Scope-OrgID: tenant-1"
```

```json
{
  "scopes": [
    {
      "name": "span",
      "tags": ["http.method", "http.status_code", ...]
    },
    {
      "name": "resource",
      "tags": ["service.name", "k8s.namespace.name", ...]
    },
    {
      "name": "intrinsic",
      "tags": ["name", "duration", "status", ...]
    }
  ]
}
```

### `GET /api/search/tag/<tag>/values`

특정 태그의 값 목록.

```bash
curl -G "http://tempo:3200/api/search/tag/service.name/values" \
  -H "X-Scope-OrgID: tenant-1" \
  --data-urlencode 'q={resource.cluster="us-east-1"}'   # 옵션: 필터
```

응답:
```json
{
  "tagValues": ["frontend", "backend", "checkout", "payment"]
}
```

### `GET /api/v2/search/tag/<tag>/values`

타입 정보 포함:

```json
{
  "tagValues": [
    {"type": "string", "value": "frontend"},
    {"type": "string", "value": "backend"}
  ]
}
```

---

## TraceQL Metrics

### `GET /api/metrics/query`

Instant 메트릭 쿼리.

```bash
curl -G "http://tempo:3200/api/metrics/query" \
  -H "X-Scope-OrgID: tenant-1" \
  --data-urlencode 'q={resource.service.name="api"} | rate()' \
  --data-urlencode 'time=1700000000'
```

### `GET /api/metrics/query_range`

Range 메트릭 쿼리.

```bash
curl -G "http://tempo:3200/api/metrics/query_range" \
  -H "X-Scope-OrgID: tenant-1" \
  --data-urlencode 'q={resource.service.name="api"} | rate()' \
  --data-urlencode 'start=1700000000' \
  --data-urlencode 'end=1700003600' \
  --data-urlencode 'step=60s'
```

#### 응답 (Prometheus 호환)

```json
{
  "status": "success",
  "data": {
    "resultType": "matrix",
    "result": [
      {
        "metric": {"__name__": "rate"},
        "values": [
          [1700000000, "12.5"],
          [1700000060, "13.1"]
        ]
      }
    ]
  }
}
```

### Metric 함수 예시

```bash
# 비율
?q={status=error} | rate()

# 시간 윈도우 카운트
?q={status=error} | count_over_time()

# 분위수
?q={resource.service.name="api"} | quantile_over_time(duration, 0.95)

# 히스토그램
?q={resource.service.name="api"} | histogram_over_time(duration)

# 그룹화
?q={status=error} | rate() by (resource.service.name)

# 비교
?q={status=error} | compare({status=ok})
```

---

## 수집 API

### OTLP

#### gRPC: `4317`

OpenTelemetry SDK가 gRPC로 직접 전송.

#### HTTP: `4318`

```bash
curl -X POST -H "Content-Type: application/x-protobuf" \
  -H "X-Scope-OrgID: tenant-1" \
  --data-binary @traces.pb \
  http://tempo:4318/v1/traces
```

### Jaeger

#### Thrift HTTP: `14268`

```bash
curl -X POST -H "Content-Type: application/x-thrift" \
  --data-binary @jaeger.thrift \
  http://tempo:14268/api/traces
```

#### gRPC: `14250`

#### Thrift Compact UDP: `6831`
#### Thrift Binary UDP: `6832`

### Zipkin: `9411`

```bash
curl -X POST -H "Content-Type: application/json" \
  -d '[{"id":"abc","traceId":"def","name":"op","timestamp":1700000000000000,"duration":100000}]' \
  http://tempo:9411/api/v2/spans
```

### OpenCensus: `55678`

레거시 프로토콜.

---

## 관리/헬스 API

### `GET /ready`

준비 상태.

```bash
curl http://tempo:3200/ready
# Ready
```

### `GET /metrics`

Prometheus 형식 자체 메트릭.

### `GET /config`

현재 활성 구성. 일부 필드는 마스킹.

```bash
curl http://tempo:3200/config
```

### `GET /services`

실행 중인 서비스 목록.

```bash
curl http://tempo:3200/services
```

응답:
```
distributor => Running
ingester => Running
querier => Running
query-frontend => Running
compactor => Running
```

### `GET /memberlist`

Memberlist 가십 클러스터 상태.

```bash
curl http://tempo:3200/memberlist
```

### `GET /ingester/ring`

Ingester 해시 링.

```bash
curl http://tempo:3200/ingester/ring
```

### `GET /compactor/ring`

Compactor 해시 링.

### `GET /distributor/ring`

Distributor 해시 링 (HA Tracker).

### `GET /metrics-generator/ring`

Metrics Generator 해시 링.

### `POST /shutdown`

정상 종료 (블록 플러시 후).

```bash
curl -X POST http://tempo:3200/shutdown
```

### `GET /flush`

Ingester 메모리 트레이스를 즉시 플러시.

```bash
curl http://tempo:3200/flush
```

### `GET /debug/pprof/*`

Go pprof 프로파일 (디버깅).

```bash
curl http://tempo:3200/debug/pprof/heap > heap.pprof
curl http://tempo:3200/debug/pprof/profile?seconds=30 > cpu.pprof

go tool pprof heap.pprof
```

---

## Echo / Buildinfo

### `GET /api/echo`

수신 헤더와 본문을 그대로 응답 (디버깅용).

```bash
curl -H "X-Scope-OrgID: tenant-1" \
  -H "X-Custom: value" \
  http://tempo:3200/api/echo
```

### `GET /api/status/buildinfo`

Tempo 버전 정보.

```bash
curl http://tempo:3200/api/status/buildinfo
```

응답:
```json
{
  "version": "2.4.0",
  "branch": "HEAD",
  "buildDate": "2024-04-01",
  "revision": "abc1234",
  "buildUser": "ci"
}
```

### `GET /api/overrides`

현재 적용된 테넌트별 overrides 조회 (관리자).

---

## API 사용 예시 모음

### 1. 에러 트레이스 모니터링

```bash
# 최근 1시간 에러 트레이스 수
curl -G "http://tempo:3200/api/metrics/query" \
  -H "X-Scope-OrgID: tenant-1" \
  --data-urlencode 'q={status=error} | rate()' \
  --data-urlencode 'time='$(date -d '1 hour ago' +%s)
```

### 2. 서비스별 P95 지연

```bash
curl -G "http://tempo:3200/api/metrics/query_range" \
  -H "X-Scope-OrgID: tenant-1" \
  --data-urlencode 'q={resource.service.name=~".+"} | quantile_over_time(duration, 0.95) by (resource.service.name)' \
  --data-urlencode 'start='$(date -d '1 hour ago' +%s) \
  --data-urlencode 'end='$(date +%s) \
  --data-urlencode 'step=60s'
```

### 3. 자동화 스크립트: 느린 트레이스 알림

```bash
#!/bin/bash
SLOW_TRACES=$(curl -s -G "http://tempo:3200/api/search" \
  -H "X-Scope-OrgID: tenant-1" \
  --data-urlencode 'q={duration > 5s}' \
  --data-urlencode "start=$(date -d '5 min ago' +%s)" \
  --data-urlencode "end=$(date +%s)" \
  --data-urlencode 'limit=100' | jq '.traces | length')

if [ "$SLOW_TRACES" -gt 10 ]; then
  echo "ALERT: $SLOW_TRACES slow traces detected!"
fi
```

### 4. CI/CD에 통합 (Smoke Test)

```bash
# 배포 후 새 버전의 에러 트레이스 확인
ERROR_COUNT=$(curl -s -G "http://tempo:3200/api/search" \
  -H "X-Scope-OrgID: tenant-1" \
  --data-urlencode "q={resource.service.version=\"$VERSION\" && status=error}" \
  --data-urlencode "start=$(date -d '5 min ago' +%s)" \
  --data-urlencode "end=$(date +%s)" | jq '.traces | length')

if [ "$ERROR_COUNT" -gt 0 ]; then
  echo "Deployment validation failed: $ERROR_COUNT error traces"
  exit 1
fi
```
