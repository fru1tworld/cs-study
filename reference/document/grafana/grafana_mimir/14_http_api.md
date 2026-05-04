# Mimir HTTP API 레퍼런스

> 이 문서는 Grafana Mimir 공식 문서의 HTTP API 섹션을 한국어로 정리한 것입니다.
> 원본: https://grafana.com/docs/mimir/latest/references/http-api/

---

## 목차

1. [개요](#개요)
2. [공통 사항](#공통-사항)
3. [수집(Push) API](#수집push-api)
4. [PromQL 쿼리 API](#promql-쿼리-api)
5. [라벨/시리즈 API](#라벨시리즈-api)
6. [Cardinality API](#cardinality-api)
7. [Exemplars API](#exemplars-api)
8. [Ruler API](#ruler-api)
9. [Alertmanager API](#alertmanager-api)
10. [관리/디버그 API](#관리디버그-api)

---

## 개요

Mimir의 HTTP API:

- **Prometheus 호환 API** (`/prometheus/api/v1/*`): PromQL 쿼리
- **Mimir 전용 API** (`/api/v1/*`): 푸시, Ruler, Alertmanager
- **OTLP** (`/otlp/v1/*`): OpenTelemetry
- **관리 API** (`/-/*`, `/debug/*`): 헬스체크, pprof

기본 포트: **9009** (HTTP), **9095** (gRPC)

---

## 공통 사항

### 멀티 테넌시 헤더

```
X-Scope-OrgID: <tenant-id>
```

### 시간 형식

| 형식 | 예시 |
|------|------|
| Unix seconds | `1700000000` |
| Unix seconds + ms | `1700000000.123` |
| RFC3339 | `2023-11-15T10:00:00Z` |
| RFC3339Nano | `2023-11-15T10:00:00.123456Z` |

### 응답 형식

```json
{
  "status": "success",
  "data": { ... },
  "errorType": null,
  "error": null,
  "warnings": []
}
```

---

## 수집(Push) API

### `POST /api/v1/push`

Prometheus Remote Write.

```bash
curl -X POST \
  -H "Content-Type: application/x-protobuf" \
  -H "Content-Encoding: snappy" \
  -H "X-Scope-OrgID: tenant-1" \
  --data-binary @write-request.snappy \
  http://mimir:9009/api/v1/push
```

#### Body

Snappy 압축된 protobuf (`prometheus.WriteRequest`).

#### 응답

| 코드 | 의미 |
|------|------|
| 200 | 성공 |
| 400 | 잘못된 요청 (라벨 오류 등) |
| 401 | 인증 실패 |
| 429 | Rate Limit |
| 500 | 서버 에러 |

### `POST /otlp/v1/metrics`

OpenTelemetry OTLP HTTP.

```bash
curl -X POST \
  -H "Content-Type: application/x-protobuf" \
  -H "X-Scope-OrgID: tenant-1" \
  --data-binary @metrics.pb \
  http://mimir:9009/otlp/v1/metrics
```

### `POST /api/v1/push/influx/write`

InfluxDB Line Protocol (활성화 시).

```bash
curl -X POST \
  -H "X-Scope-OrgID: tenant-1" \
  --data-binary "weather,location=us temperature=82 1700000000000000000" \
  http://mimir:9009/api/v1/push/influx/write
```

---

## PromQL 쿼리 API

### `GET /prometheus/api/v1/query`

Instant query.

```bash
curl -G "http://mimir:9009/prometheus/api/v1/query" \
  -H "X-Scope-OrgID: tenant-1" \
  --data-urlencode 'query=up' \
  --data-urlencode "time=1700000000"
```

| 파라미터 | 설명 |
|----------|------|
| `query` | PromQL |
| `time` | 평가 시점 (선택) |
| `timeout` | 쿼리 타임아웃 |

#### 응답

```json
{
  "status": "success",
  "data": {
    "resultType": "vector",
    "result": [
      {
        "metric": {"__name__": "up", "job": "prometheus"},
        "value": [1700000000, "1"]
      }
    ]
  }
}
```

### `GET /prometheus/api/v1/query_range`

Range query.

```bash
curl -G "http://mimir:9009/prometheus/api/v1/query_range" \
  -H "X-Scope-OrgID: tenant-1" \
  --data-urlencode 'query=rate(http_requests_total[5m])' \
  --data-urlencode "start=1700000000" \
  --data-urlencode "end=1700003600" \
  --data-urlencode "step=15s"
```

| 파라미터 | 설명 |
|----------|------|
| `query` | PromQL |
| `start` | 시작 시간 |
| `end` | 종료 시간 |
| `step` | 평가 간격 |
| `timeout` | 타임아웃 |

### `POST /prometheus/api/v1/read`

Remote Read (긴 시간 범위에 권장).

```bash
curl -X POST \
  -H "Content-Type: application/x-protobuf" \
  -H "Content-Encoding: snappy" \
  -H "X-Scope-OrgID: tenant-1" \
  --data-binary @read-request.snappy \
  http://mimir:9009/prometheus/api/v1/read
```

### `GET /prometheus/api/v1/format_query`

PromQL 포맷팅.

```bash
curl -G "http://mimir:9009/prometheus/api/v1/format_query" \
  --data-urlencode 'query=  up   {job="prom"} '
```

응답:
```json
{"status":"success","data":"up{job=\"prom\"}"}
```

---

## 라벨/시리즈 API

### `GET /prometheus/api/v1/labels`

모든 라벨 이름.

```bash
curl -G "http://mimir:9009/prometheus/api/v1/labels" \
  -H "X-Scope-OrgID: tenant-1" \
  --data-urlencode "start=1700000000" \
  --data-urlencode "end=1700003600" \
  --data-urlencode 'match[]={job="prometheus"}'
```

### `GET /prometheus/api/v1/label/<name>/values`

특정 라벨의 값.

```bash
curl -G "http://mimir:9009/prometheus/api/v1/label/job/values" \
  -H "X-Scope-OrgID: tenant-1"
```

### `GET /prometheus/api/v1/series`

매칭 시리즈.

```bash
curl -G "http://mimir:9009/prometheus/api/v1/series" \
  -H "X-Scope-OrgID: tenant-1" \
  --data-urlencode 'match[]={job="prometheus"}' \
  --data-urlencode "start=1700000000" \
  --data-urlencode "end=1700003600"
```

### `GET /prometheus/api/v1/metadata`

메트릭 메타데이터.

```bash
curl "http://mimir:9009/prometheus/api/v1/metadata" \
  -H "X-Scope-OrgID: tenant-1"
```

응답:
```json
{
  "status": "success",
  "data": {
    "http_requests_total": [
      {
        "type": "counter",
        "help": "Total HTTP requests",
        "unit": ""
      }
    ]
  }
}
```

---

## Cardinality API

### `GET /prometheus/api/v1/cardinality/label_names`

라벨 이름별 시리즈 수.

```bash
curl -G "http://mimir:9009/prometheus/api/v1/cardinality/label_names" \
  -H "X-Scope-OrgID: tenant-1" \
  --data-urlencode "limit=10" \
  --data-urlencode 'selector={job="prometheus"}'
```

응답:
```json
{
  "label_values_count_total": 12345,
  "label_names_count": 50,
  "items": [
    {"label_name": "instance", "label_values_count": 1500},
    {"label_name": "pod", "label_values_count": 1200}
  ]
}
```

### `GET /prometheus/api/v1/cardinality/label_values`

라벨 값별 시리즈 수.

```bash
curl -G "http://mimir:9009/prometheus/api/v1/cardinality/label_values" \
  -H "X-Scope-OrgID: tenant-1" \
  --data-urlencode "label_names[]=instance" \
  --data-urlencode 'selector={job="prometheus"}'
```

### `GET /prometheus/api/v1/cardinality/active_series`

활성 시리즈 분석.

```bash
curl -G "http://mimir:9009/prometheus/api/v1/cardinality/active_series" \
  -H "X-Scope-OrgID: tenant-1" \
  --data-urlencode 'selector={job="prometheus"}'
```

---

## Exemplars API

### `GET /prometheus/api/v1/query_exemplars`

Exemplars 조회.

```bash
curl -G "http://mimir:9009/prometheus/api/v1/query_exemplars" \
  -H "X-Scope-OrgID: tenant-1" \
  --data-urlencode 'query=http_request_duration_seconds_bucket' \
  --data-urlencode "start=1700000000" \
  --data-urlencode "end=1700003600"
```

응답:
```json
{
  "status": "success",
  "data": [
    {
      "seriesLabels": {"__name__": "http_request_duration_seconds_bucket", "le": "0.1"},
      "exemplars": [
        {
          "labels": {"trace_id": "abc123"},
          "value": "0.05",
          "timestamp": 1700000050.123
        }
      ]
    }
  ]
}
```

---

## Ruler API

Prometheus Rules API와 호환.

### `GET /prometheus/api/v1/rules`

평가 중인 룰 (Prometheus 호환).

```bash
curl -H "X-Scope-OrgID: tenant-1" \
  http://mimir:9009/prometheus/api/v1/rules
```

### `GET /prometheus/api/v1/alerts`

활성 알림.

```bash
curl -H "X-Scope-OrgID: tenant-1" \
  http://mimir:9009/prometheus/api/v1/alerts
```

### `GET /api/v1/rules`

룰 그룹 목록 (Mimir 관리 API).

### `GET /api/v1/rules/<namespace>`

특정 네임스페이스의 그룹.

### `GET /api/v1/rules/<namespace>/<group>`

특정 그룹.

### `POST /api/v1/rules/<namespace>`

룰 그룹 생성/업데이트.

```bash
curl -H "X-Scope-OrgID: tenant-1" \
  -H "Content-Type: application/yaml" \
  -X POST \
  --data-binary @rules.yaml \
  http://mimir:9009/api/v1/rules/infra
```

### `DELETE /api/v1/rules/<namespace>/<group>`

룰 그룹 삭제.

### `DELETE /api/v1/rules/<namespace>`

네임스페이스 전체 삭제.

---

## Alertmanager API

### `GET /alertmanager`

Alertmanager UI (브라우저).

### `GET /api/v1/alerts`

테넌트의 Alertmanager 구성 조회.

```bash
curl -H "X-Scope-OrgID: tenant-1" \
  http://mimir:9009/api/v1/alerts
```

### `POST /api/v1/alerts`

Alertmanager 구성 업로드.

```bash
curl -H "X-Scope-OrgID: tenant-1" \
  -H "Content-Type: application/yaml" \
  -X POST \
  --data-binary @alertmanager.yaml \
  http://mimir:9009/api/v1/alerts
```

### `DELETE /api/v1/alerts`

Alertmanager 구성 삭제.

### Alertmanager Native API

```bash
# 알림 푸시
curl -X POST -H "X-Scope-OrgID: tenant-1" \
  -H "Content-Type: application/json" \
  -d '[{"labels":{"alertname":"Test","severity":"critical"}}]' \
  http://mimir:9009/alertmanager/api/v2/alerts

# 활성 알림 조회
curl -H "X-Scope-OrgID: tenant-1" \
  http://mimir:9009/alertmanager/api/v2/alerts

# Silence 생성
curl -X POST -H "X-Scope-OrgID: tenant-1" \
  -H "Content-Type: application/json" \
  -d '{
    "matchers": [{"name": "alertname", "value": "Test", "isRegex": false}],
    "startsAt": "2024-01-01T00:00:00Z",
    "endsAt": "2024-01-01T01:00:00Z",
    "createdBy": "ops",
    "comment": "test silence"
  }' \
  http://mimir:9009/alertmanager/api/v2/silences

# Silence 목록
curl -H "X-Scope-OrgID: tenant-1" \
  http://mimir:9009/alertmanager/api/v2/silences
```

---

## 관리/디버그 API

### `GET /ready`

준비 상태.

```bash
curl http://mimir:9009/ready
# Ready
```

### `GET /metrics`

자체 메트릭.

### `GET /config`

활성 구성 (마스킹된 형태).

```bash
curl http://mimir:9009/config
```

### `GET /runtime_config`

현재 runtime config.

```bash
curl http://mimir:9009/runtime_config
```

### `GET /services`

실행 중인 서비스.

```bash
curl http://mimir:9009/services
```

### `GET /memberlist`

Memberlist 상태.

### Ring API

각 컴포넌트 ring:

```bash
curl http://mimir:9009/ingester/ring
curl http://mimir:9009/distributor/ring
curl http://mimir:9009/store-gateway/ring
curl http://mimir:9009/compactor/ring
curl http://mimir:9009/ruler/ring
curl http://mimir:9009/alertmanager/ring
```

### Ring 조작

```bash
# 죽은 인스턴스 제거 (강제)
curl -X POST "http://mimir:9009/ingester/ring?forget=<instance-id>"
```

### `POST /ingester/shutdown`

Ingester 정상 종료.

### `POST /ingester/flush`

Ingester 메모리 플러시.

```bash
curl -X POST http://mimir:9009/ingester/flush
```

### `POST /ingester/prepare-shutdown`

종료 준비 (블록 업로드 가속).

### `GET /distributor/all_user_stats`

테넌트별 통계 (HA Tracker, Rate 등).

### `GET /distributor/ha_tracker`

HA Tracker 상태.

### `GET /multitenant_alertmanager/status`

Alertmanager 상태.

### Cardinality 분석 도구

```bash
# 활성 시리즈 분포
curl -G "http://mimir:9009/store-gateway/cardinality/label_names" \
  -H "X-Scope-OrgID: tenant-1"
```

### `GET /debug/pprof/*`

Go pprof 프로파일.

```bash
# Heap
curl http://mimir:9009/debug/pprof/heap > heap.pprof
go tool pprof heap.pprof

# CPU (30초)
curl http://mimir:9009/debug/pprof/profile?seconds=30 > cpu.pprof
go tool pprof cpu.pprof

# Goroutines
curl http://mimir:9009/debug/pprof/goroutine?debug=2

# Block (lock contention)
curl http://mimir:9009/debug/pprof/block > block.pprof
```

### `GET /debug/fgprof`

Off-CPU 프로파일링.

```bash
curl "http://mimir:9009/debug/fgprof?seconds=30" > fgprof.pprof
go tool pprof fgprof.pprof
```
