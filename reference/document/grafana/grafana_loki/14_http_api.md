# Loki HTTP API 레퍼런스

> 이 문서는 Grafana Loki 공식 문서의 HTTP API 섹션을 한국어로 정리한 것입니다.
> 원본: https://grafana.com/docs/loki/latest/reference/loki-http-api/

---

## 목차

1. [개요](#개요)
2. [공통 사항](#공통-사항)
3. [수집(Ingestion) API](#수집ingestion-api)
4. [쿼리 API](#쿼리-api)
5. [라벨 API](#라벨-api)
6. [시리즈 API](#시리즈-api)
7. [Tail API](#tail-api)
8. [통계 API](#통계-api)
9. [Volume API](#volume-api)
10. [Patterns API](#patterns-api)
11. [Rules API](#rules-api)
12. [Delete API](#delete-api)
13. [관리/헬스 API](#관리헬스-api)

---

## 개요

Loki의 HTTP API는 두 카테고리로 나뉩니다.

- **Prometheus 호환 API**: PromQL 호환 형식 (시리즈, 라벨, 쿼리)
- **Loki 전용 API**: 푸시, tail, patterns 등

기본 포트: **3100**

---

## 공통 사항

### 인증 헤더

멀티 테넌시 활성화 시:
```
X-Scope-OrgID: <tenant-id>
```

여러 테넌트:
```
X-Scope-OrgID: tenant-a|tenant-b
```

### 응답 형식

| Content-Type | 설명 |
|-------------|------|
| `application/json` | 기본 |
| `application/x-protobuf` | Protobuf (일부 엔드포인트) |

### 시간 포맷

| 형식 | 예시 |
|------|------|
| Unix nanoseconds | `1700000000000000000` |
| Unix seconds | `1700000000` |
| RFC3339Nano | `2023-11-15T10:00:00.000000000Z` |
| Go duration | `1h`, `5m`, `30s` |

### 응답 코드

| 코드 | 의미 |
|------|------|
| 200 | 성공 |
| 204 | 성공, 본문 없음 |
| 400 | 잘못된 요청 |
| 401 | 인증 실패 |
| 404 | 리소스 없음 |
| 422 | 처리 불가 (쿼리 한도 초과 등) |
| 429 | Rate Limit |
| 500 | 서버 에러 |
| 502/503/504 | 일시적 에러 |

---

## 수집(Ingestion) API

### `POST /loki/api/v1/push`

로그 라인을 푸시합니다.

#### JSON 형식

```bash
curl -H "Content-Type: application/json" \
     -H "X-Scope-OrgID: tenant-1" \
     -XPOST "http://loki:3100/loki/api/v1/push" \
     --data-raw '{
       "streams": [
         {
           "stream": {"app": "myapp", "level": "info"},
           "values": [
             ["1700000000000000000", "log line 1"],
             ["1700000001000000000", "log line 2"]
           ]
         }
       ]
     }'
```

#### Protobuf + Snappy (대용량)

```
Content-Type: application/x-protobuf
Content-Encoding: snappy
```

페이로드는 `logproto.PushRequest` 메시지를 snappy 압축한 바이트.

#### Structured Metadata 포함

```json
{
  "streams": [
    {
      "stream": {"app": "myapp"},
      "values": [
        [
          "1700000000000000000",
          "log line",
          {"trace_id": "abc123", "user_id": "u-001"}
        ]
      ]
    }
  ]
}
```

#### 응답

```
204 No Content    (성공)
400 Bad Request   (라벨 형식 오류 등)
429 Too Many Requests (Rate Limit)
```

### `POST /otlp/v1/logs`

OTLP HTTP 형식 로그 푸시.

```bash
curl -H "Content-Type: application/x-protobuf" \
     -H "X-Scope-OrgID: tenant-1" \
     -XPOST "http://loki:3100/otlp/v1/logs" \
     --data-binary @logs.pb
```

---

## 쿼리 API

### `GET /loki/api/v1/query` (Instant Query)

특정 시점 쿼리.

```bash
curl -G "http://loki:3100/loki/api/v1/query" \
  -H "X-Scope-OrgID: tenant-1" \
  --data-urlencode 'query={app="myapp"} |= "error"' \
  --data-urlencode "time=1700000000" \
  --data-urlencode "limit=100" \
  --data-urlencode "direction=BACKWARD"
```

| 파라미터 | 설명 |
|----------|------|
| `query` | LogQL 표현식 |
| `time` | 시점 (기본: 현재) |
| `limit` | 결과 수 한도 |
| `direction` | `FORWARD` 또는 `BACKWARD` |

#### 응답

```json
{
  "status": "success",
  "data": {
    "resultType": "streams",  // 또는 vector, matrix
    "result": [
      {
        "stream": {"app": "myapp", "level": "error"},
        "values": [
          ["1700000000000000000", "error log line"]
        ]
      }
    ],
    "stats": { ... }
  }
}
```

### `GET /loki/api/v1/query_range` (Range Query)

시간 범위 쿼리.

```bash
curl -G "http://loki:3100/loki/api/v1/query_range" \
  -H "X-Scope-OrgID: tenant-1" \
  --data-urlencode 'query={app="myapp"}' \
  --data-urlencode "start=1700000000" \
  --data-urlencode "end=1700003600" \
  --data-urlencode "step=15s" \
  --data-urlencode "limit=1000" \
  --data-urlencode "direction=BACKWARD" \
  --data-urlencode "interval=10s"
```

| 파라미터 | 설명 |
|----------|------|
| `query` | LogQL |
| `start` | 시작 시간 |
| `end` | 종료 시간 |
| `step` | 메트릭 쿼리의 평가 간격 |
| `interval` | 로그 쿼리의 샘플링 간격 |
| `limit` | 결과 한도 |
| `direction` | `FORWARD`/`BACKWARD` |

---

## 라벨 API

### `GET /loki/api/v1/labels`

모든 라벨 이름 목록.

```bash
curl -G "http://loki:3100/loki/api/v1/labels" \
  -H "X-Scope-OrgID: tenant-1" \
  --data-urlencode "start=1700000000" \
  --data-urlencode "end=1700003600"
```

응답:
```json
{
  "status": "success",
  "data": ["app", "namespace", "level", "host"]
}
```

### `GET /loki/api/v1/label/<name>/values`

특정 라벨의 값 목록.

```bash
curl -G "http://loki:3100/loki/api/v1/label/app/values" \
  -H "X-Scope-OrgID: tenant-1" \
  --data-urlencode 'query={namespace="prod"}'
```

응답:
```json
{
  "status": "success",
  "data": ["api", "frontend", "worker"]
}
```

---

## 시리즈 API

### `GET /loki/api/v1/series`

매칭되는 스트림 목록.

```bash
curl -G "http://loki:3100/loki/api/v1/series" \
  -H "X-Scope-OrgID: tenant-1" \
  --data-urlencode 'match[]={app="api"}' \
  --data-urlencode 'match[]={namespace="prod"}' \
  --data-urlencode "start=1700000000" \
  --data-urlencode "end=1700003600"
```

응답:
```json
{
  "status": "success",
  "data": [
    {"app": "api", "instance": "host1", "level": "info"},
    {"app": "api", "instance": "host1", "level": "error"},
    {"app": "api", "instance": "host2", "level": "info"}
  ]
}
```

---

## Tail API

### `GET /loki/api/v1/tail` (WebSocket)

실시간 로그 스트리밍.

```bash
websocat "ws://loki:3100/loki/api/v1/tail?query={app=\"api\"}&start=1700000000&limit=100"
```

| 파라미터 | 설명 |
|----------|------|
| `query` | LogQL |
| `delay_for` | 지연 (초, 기본 0) |
| `limit` | 라인 한도 |
| `start` | 시작 시간 |

각 메시지:
```json
{
  "streams": [
    {
      "stream": {"app": "api"},
      "values": [
        ["1700000000000000000", "log line"]
      ]
    }
  ],
  "dropped_entries": null
}
```

---

## 통계 API

### `GET /loki/api/v1/index/stats`

인덱스 통계 (스트림 수, 청크 수, 바이트 수).

```bash
curl -G "http://loki:3100/loki/api/v1/index/stats" \
  -H "X-Scope-OrgID: tenant-1" \
  --data-urlencode 'query={namespace="prod"}' \
  --data-urlencode "start=1700000000" \
  --data-urlencode "end=1700003600"
```

응답:
```json
{
  "streams": 1234,
  "chunks": 5678,
  "bytes": 12345678,
  "entries": 9876543
}
```

---

## Volume API

### `GET /loki/api/v1/index/volume`

라벨별 데이터 볼륨.

```bash
curl -G "http://loki:3100/loki/api/v1/index/volume" \
  -H "X-Scope-OrgID: tenant-1" \
  --data-urlencode 'query={namespace="prod"}' \
  --data-urlencode "start=1700000000" \
  --data-urlencode "end=1700003600" \
  --data-urlencode "limit=10" \
  --data-urlencode "step=1h" \
  --data-urlencode "targetLabels=app,level" \
  --data-urlencode "aggregateBy=labels"
```

### `GET /loki/api/v1/index/volume_range`

시간별 볼륨 변화.

| 파라미터 | 설명 |
|----------|------|
| `query` | LogQL 매처 |
| `targetLabels` | 그룹화할 라벨 |
| `aggregateBy` | `series` 또는 `labels` |
| `step` | 시간 단위 |

---

## Patterns API

### `GET /loki/api/v1/patterns`

자주 발생하는 로그 패턴 (Pattern Ingester 활성화 필요).

```bash
curl -G "http://loki:3100/loki/api/v1/patterns" \
  -H "X-Scope-OrgID: tenant-1" \
  --data-urlencode 'query={app="api"}' \
  --data-urlencode "start=1700000000" \
  --data-urlencode "end=1700003600"
```

응답:
```json
{
  "status": "success",
  "data": [
    {
      "pattern": "User <_> logged in",
      "samples": [
        [1700000000, 100],
        [1700000060, 95]
      ]
    }
  ]
}
```

---

## Rules API

Prometheus Rules API와 호환.

### `GET /loki/api/v1/rules`

룰 그룹 목록.

```bash
curl -H "X-Scope-OrgID: tenant-1" \
  http://loki:3100/loki/api/v1/rules
```

### `GET /loki/api/v1/rules/<namespace>`

특정 네임스페이스의 룰 그룹들.

### `GET /loki/api/v1/rules/<namespace>/<group>`

특정 룰 그룹.

### `POST /loki/api/v1/rules/<namespace>`

룰 그룹 생성/업데이트.

```bash
curl -H "X-Scope-OrgID: tenant-1" \
  -H "Content-Type: application/yaml" \
  -XPOST http://loki:3100/loki/api/v1/rules/infra \
  --data-binary @rule.yaml
```

### `DELETE /loki/api/v1/rules/<namespace>/<group>`

룰 그룹 삭제.

### `DELETE /loki/api/v1/rules/<namespace>`

네임스페이스 전체 룰 삭제.

### `GET /prometheus/api/v1/rules`

룰 평가 상태 (Prometheus 호환).

### `GET /prometheus/api/v1/alerts`

활성 알림 (Prometheus 호환).

---

## Delete API

### `POST /loki/api/v1/delete`

삭제 요청 생성.

```bash
curl -G -XPOST "http://loki:3100/loki/api/v1/delete" \
  -H "X-Scope-OrgID: tenant-1" \
  --data-urlencode 'query={app="myapp"} |= "user_id=12345"' \
  --data-urlencode "start=1700000000" \
  --data-urlencode "end=1700003600"
```

### `GET /loki/api/v1/delete`

삭제 요청 목록.

```bash
curl -H "X-Scope-OrgID: tenant-1" \
  "http://loki:3100/loki/api/v1/delete"
```

응답:
```json
[
  {
    "request_id": "abc123",
    "start_time": 1700000000,
    "end_time": 1700003600,
    "query": "{app=\"myapp\"} |= \"user_id=12345\"",
    "status": "received",
    "created_at": 1700004000
  }
]
```

상태:
- `received`
- `processed`
- `failed`

### `DELETE /loki/api/v1/delete?request_id=<id>`

삭제 요청 취소 (`processed` 전에만 가능).

---

## 관리/헬스 API

### `GET /ready`

준비 상태 (모든 종속성 정상).

```bash
curl http://loki:3100/ready
# Ready
```

### `GET /metrics`

Prometheus 형식 자체 메트릭.

### `GET /config`

현재 활성 구성. 일부 필드는 마스킹.

```bash
curl http://loki:3100/config
```

### `GET /services`

실행 중인 서비스 목록과 상태.

```bash
curl http://loki:3100/services
```

응답:
```
distributor => Running
ingester => Running
querier => Running
```

### `GET /memberlist`

Memberlist 클러스터 상태.

```bash
curl http://loki:3100/memberlist
```

### `GET /ring`

해시 링 상태 (Ingester, Distributor 등).

```bash
curl http://loki:3100/ring
curl http://loki:3100/distributor/ring
curl http://loki:3100/scheduler/ring
curl http://loki:3100/compactor/ring
```

### `GET /loki/api/v1/format_query`

LogQL 쿼리 포맷팅.

```bash
curl -G "http://loki:3100/loki/api/v1/format_query" \
  --data-urlencode 'query={ app = "api"}|="error"'
```

응답:
```json
{
  "status": "success",
  "data": "{app=\"api\"} |= `error`"
}
```

### `GET /flush`

Ingester 메모리 청크를 즉시 플러시 (테스트/디버깅).

### `POST /ingester/shutdown`

Ingester 정상 종료 (청크 플러시 후 종료).

### `GET /debug/pprof/*`

Go pprof 프로파일 (디버깅).

```bash
curl http://loki:3100/debug/pprof/heap > heap.pprof
curl http://loki:3100/debug/pprof/profile?seconds=30 > cpu.pprof
go tool pprof heap.pprof
```
