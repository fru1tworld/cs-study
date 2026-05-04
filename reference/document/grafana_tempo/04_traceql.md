# TraceQL 쿼리 언어

> 이 문서는 Grafana Tempo 공식 문서의 "Query with TraceQL" 섹션을 한국어로 정리한 것입니다.
> 원본: https://grafana.com/docs/tempo/latest/traceql/

---

## 목차

1. [TraceQL 개요](#traceql-개요)
2. [Spanset 선택자](#spanset-선택자)
3. [Intrinsic 필드](#intrinsic-필드)
4. [속성(Attributes) 선택](#속성attributes-선택)
5. [비교 연산자](#비교-연산자)
6. [논리 연산자](#논리-연산자)
7. [구조적 연산자 (Structural)](#구조적-연산자-structural)
8. [집계자 (Aggregators)](#집계자-aggregators)
9. [TraceQL Metrics](#traceql-metrics)
10. [실전 쿼리 예시](#실전-쿼리-예시)

---

## TraceQL 개요

**TraceQL**은 Tempo에서 트레이스를 선택하기 위한 쿼리 언어로, **PromQL/LogQL과 유사한 문법** 을 사용합니다.

### 기본 문법 구조

```
{ <조건> }
```

가장 단순한 쿼리:
```traceql
{}                              # 모든 트레이스
{ .http.method = "GET" }        # http.method가 GET인 스팬
{ duration > 1s }               # 1초 이상 걸린 스팬
```

### 사용 방법

- **Grafana Explore**: 쿼리 빌더 또는 직접 입력
- **HTTP API**: `GET /api/search?q=<traceql>`
- **CLI**: `tempo-cli` 또는 `traceql-search`

---

## Spanset 선택자

`{}` 안의 조건으로 매칭되는 스팬 집합(spanset)을 선택합니다.

### 빈 선택자

```traceql
{}    # 모든 스팬
```

### 단일 조건

```traceql
{ .http.status_code = 500 }
```

### 다중 조건 (AND)

```traceql
{ .http.method = "POST" && .http.status_code >= 400 }
```

---

## Intrinsic 필드

스팬의 내장 속성 (속성 접두사 없이 사용).

| 필드 | 설명 |
|------|------|
| `name` | 스팬 이름 (operation name) |
| `duration` | 스팬 지속 시간 |
| `status` | 스팬 상태 (`ok`, `error`, `unset`) |
| `statusMessage` | 상태 메시지 |
| `kind` | 스팬 종류 (`server`, `client`, `producer`, `consumer`, `internal`) |
| `traceDuration` | 전체 트레이스 지속 시간 |
| `rootName` | 루트 스팬 이름 |
| `rootServiceName` | 루트 스팬 서비스 이름 |
| `parent` | 부모 스팬 |
| `parent:id` | 부모 스팬 ID |
| `event:name` | 이벤트 이름 |
| `event:timeSinceStart` | 이벤트 발생까지의 시간 |
| `link:traceID`, `link:spanID` | 링크된 스팬 |

### 예시

```traceql
{ name = "HTTP GET /api/users" }
{ duration > 100ms }
{ status = error }
{ kind = server }
{ rootServiceName = "frontend" && status = error }
```

---

## 속성(Attributes) 선택

세 가지 스코프의 속성:

| 접두사 | 의미 |
|--------|------|
| `.` 또는 `span.` | 스팬 속성 |
| `resource.` | 리소스 속성 |
| `event.` | 이벤트 속성 |
| `link.` | 링크 속성 |

### 예시

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

### 동적 타입

속성 타입은 자동 추론:
- 문자열: `"text"` 큰따옴표
- 숫자: `200`, `1.5`
- 불리언: `true`, `false`
- Duration: `100ms`, `1s`, `5m`
- Status: `ok`, `error`, `unset` (예약어)

---

## 비교 연산자

| 연산자 | 의미 |
|--------|------|
| `=` | 같음 |
| `!=` | 다름 |
| `>` | 큼 |
| `>=` | 크거나 같음 |
| `<` | 작음 |
| `<=` | 작거나 같음 |
| `=~` | 정규식 일치 |
| `!~` | 정규식 불일치 |

### 예시

```traceql
{ .http.status_code >= 400 }
{ duration > 500ms }
{ resource.service.name =~ "frontend|backend" }
{ name !~ ".*health.*" }
```

---

## 논리 연산자

| 연산자 | 의미 | 적용 |
|--------|------|------|
| `&&` | AND | spanset 내부 |
| `\|\|` | OR | spanset 내부 |

### 예시

```traceql
# 단일 spanset 내 AND
{ .http.method = "POST" && .http.status_code >= 400 }

# 단일 spanset 내 OR
{ resource.service.name = "frontend" || resource.service.name = "api" }
```

---

## 구조적 연산자 (Structural)

여러 spanset 간 관계를 표현.

| 연산자 | 의미 |
|--------|------|
| `>` | 자식 (Child) |
| `>>` | 후손 (Descendant) |
| `<` | 부모 (Parent) |
| `<<` | 조상 (Ancestor) |
| `~` | 형제 (Sibling) |
| `&&` | 같은 트레이스에 둘 다 존재 |
| `\|\|` | 둘 중 하나라도 존재 |

### 예시

```traceql
# frontend의 자식 중 backend 호출
{ resource.service.name = "frontend" } > { resource.service.name = "backend" }

# 어딘가에 db 호출이 있는 트레이스
{ resource.service.name = "frontend" } >> { .db.system = "postgresql" }

# 에러 스팬과 같은 트레이스에 결제 처리 스팬이 있는 경우
{ status = error } && { name = "process-payment" }
```

---

## 집계자 (Aggregators)

spanset에 대한 통계 함수.

### `count()`

매칭된 스팬 수.

```traceql
{ resource.service.name = "frontend" } | count() > 5
```

### `avg(<field>)`, `sum(<field>)`, `min(<field>)`, `max(<field>)`

```traceql
{ resource.service.name = "api" } | avg(duration) > 100ms
{ name = "db.query" } | max(duration) > 1s
```

### `by(<field>)`

그룹화 (집계와 함께 사용).

```traceql
{ resource.service.name = "api" } | by(.http.method) | count() > 10
```

### `select()`

추가로 표시할 필드 지정.

```traceql
{ resource.service.name = "api" && status = error }
| select(.http.url, .http.status_code, span.user_id)
```

---

## TraceQL Metrics

TraceQL을 사용해 트레이스 데이터에서 메트릭을 즉시 생성.

### `rate()`

스팬 비율.

```traceql
{ resource.service.name = "frontend" } | rate()
```

### `count_over_time()`

시간 윈도우 내 스팬 수.

```traceql
{ status = error } | count_over_time()
```

### `quantile_over_time()`

분위수 통계.

```traceql
{ resource.service.name = "api" } | quantile_over_time(duration, 0.95, 0.99)
```

### `histogram_over_time()`

히스토그램.

```traceql
{ resource.service.name = "api" } | histogram_over_time(duration)
```

### `compare()`

두 spanset 비교 (드릴다운에 유용).

```traceql
{ status = error } | compare({ status = ok })
```

### 그룹화와 결합

```traceql
{ resource.service.name = "api" } | rate() by (.http.route)
```

---

## 실전 쿼리 예시

### 1. 느린 트레이스 찾기

```traceql
{ duration > 1s }
```

### 2. 에러 트레이스 찾기

```traceql
{ status = error }
```

### 3. 특정 사용자 트레이스

```traceql
{ .user.id = "u-12345" }
```

### 4. HTTP 5xx 에러

```traceql
{ .http.status_code >= 500 }
```

### 5. 데이터베이스 느린 쿼리

```traceql
{ .db.system = "postgresql" && duration > 100ms }
```

### 6. 특정 서비스의 외부 API 호출

```traceql
{ resource.service.name = "checkout" && .http.url =~ "https://payment-gateway.*" }
```

### 7. 결제 실패 (구조적)

```traceql
{ name = "checkout" } >> { name = "payment" && status = error }
```

### 8. 특정 엔드포인트의 P95 지연

```traceql
{ resource.service.name = "api" && .http.route = "/api/orders" } 
| quantile_over_time(duration, 0.95)
```

### 9. 분당 에러율

```traceql
{ status = error } 
| rate() 
by (resource.service.name)
```

### 10. 외부 서비스 호출이 있는 느린 트레이스

```traceql
{ duration > 500ms } && { kind = client && .http.url =~ "https://.*" }
```

---

## 쿼리 옵션

### 시간 범위

쿼리는 항상 시간 범위와 함께 실행됩니다.

```bash
curl -G "http://tempo:3200/api/search" \
  --data-urlencode 'q={ status = error }' \
  --data-urlencode 'start=1700000000' \
  --data-urlencode 'end=1700003600' \
  --data-urlencode 'limit=20'
```

### 결과 한도

```bash
--data-urlencode 'limit=20'           # 최대 결과 수
--data-urlencode 'spss=10'            # spans per spanset
```

### TraceQL Metrics

```bash
curl -G "http://tempo:3200/api/metrics/query_range" \
  --data-urlencode 'q={resource.service.name="api"} | rate()' \
  --data-urlencode 'start=1700000000' \
  --data-urlencode 'end=1700003600' \
  --data-urlencode 'step=60s'
```
