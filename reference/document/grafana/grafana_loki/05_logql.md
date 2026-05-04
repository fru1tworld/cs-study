# LogQL 쿼리 언어

> 이 문서는 Grafana Loki 공식 문서의 LogQL 섹션을 한국어로 정리한 것입니다.
> 원본: https://grafana.com/docs/loki/latest/query/

---

## 목차

1. [LogQL 개요](#logql-개요)
2. [Log 쿼리](#log-쿼리)
3. [Log Stream Selector](#log-stream-selector)
4. [Log Pipeline](#log-pipeline)
5. [파서(Parser)](#파서parser)
6. [라벨 필터](#라벨-필터)
7. [라인 필터](#라인-필터)
8. [Format Expression](#format-expression)
9. [Metric 쿼리](#metric-쿼리)
10. [집계 함수](#집계-함수)
11. [Built-in 함수](#built-in-함수)

---

## LogQL 개요

**LogQL**은 Grafana Loki의 쿼리 언어로, **PromQL에서 영감** 을 받았습니다. 라벨과 연산자를 사용하여 로그를 필터링하고 분석할 수 있습니다.

### 두 가지 쿼리 유형

1. **Log Queries (로그 쿼리)**: 로그 라인의 내용을 반환
2. **Metric Queries (메트릭 쿼리)**: 로그 결과를 바탕으로 메트릭 값을 계산

### 기본 구조

```
{ log stream selector } | log pipeline
```

- **log stream selector**: 필수. 처리할 로그 스트림 선택
- **log pipeline**: 선택. 필터, 파서, 포맷터로 구성

---

## Log 쿼리

가장 단순한 형태:

```logql
{job="varlogs"}
```

위 쿼리는 `job` 라벨이 `varlogs`인 모든 스트림의 로그를 반환합니다.

파이프라인을 추가하면:

```logql
{job="varlogs"} |= "error" | json | status_code >= 500
```

위는 다음을 의미합니다:
1. `job=varlogs` 스트림 선택
2. "error" 문자열 포함 라인만
3. JSON 파싱
4. `status_code` >= 500 라벨 필터

---

## Log Stream Selector

라벨 키-값 쌍으로 처리할 데이터셋을 좁힙니다.

### 사용 가능한 연산자

| 연산자 | 의미 |
|--------|------|
| `=` | 정확히 일치 |
| `!=` | 일치하지 않음 |
| `=~` | 정규표현식 일치 |
| `!~` | 정규표현식 불일치 |

### 예시

```logql
{app="frontend"}                           # 정확 일치
{app=~"frontend|backend"}                  # 정규식 일치
{environment="production", level!="debug"} # AND 조건 (콤마)
{cluster="us-east-1", namespace=~"prod-.*"}
```

### 주의사항

- **하나 이상의 라벨 매처가 필요**합니다.
- 단순히 `{}`만 사용할 수 없습니다.
- 빈 값을 매칭하려면 `{label=""}` 사용

---

## Log Pipeline

Stream Selector 뒤에 `|`로 시작하는 표현식들을 연결하여 구성합니다.

```
{stream selector} | filter1 | filter2 | parser | filter3 | format
```

### 구성 요소

1. **Line Filter Expression**: 라인 단위 텍스트 필터
2. **Parser Expression**: 로그 라인 파싱 (JSON, logfmt 등)
3. **Label Filter Expression**: 라벨 값 비교
4. **Format Expression**: 라인/라벨 형식 변환
5. **Drop Labels Expression**: 라벨 제거
6. **Keep Labels Expression**: 라벨 유지
7. **Unwrap Expression**: 메트릭 쿼리용 값 추출

### 평가 순서

파이프라인의 각 단계는 **왼쪽에서 오른쪽** 순서로 적용됩니다. 가능하면 선택성이 높은 필터를 앞에 두어 처리량을 줄이는 것이 좋습니다.

---

## 파서(Parser)

로그 라인을 파싱하여 라벨로 추출합니다.

### JSON 파서

```logql
{job="api"} | json
```

JSON 로그를 파싱하여 키를 라벨로 변환합니다.

```json
{"level":"error","msg":"timeout","duration":"5s"}
```
→ 라벨: `level=error`, `msg=timeout`, `duration=5s`

특정 필드만 추출:

```logql
{job="api"} | json status="response.status_code", method="request.method"
```

### logfmt 파서

```logql
{job="api"} | logfmt
```

logfmt 형식을 파싱합니다:

```
level=error msg="timeout occurred" duration=5s
```

### pattern 파서

```logql
{job="nginx"} | pattern `<ip> - - [<_>] "<method> <path> <_>" <status>`
```

위치 기반 추출. `<_>`는 무시할 부분.

### regexp 파서

```logql
{job="app"} | regexp `(?P<method>\w+) (?P<path>[^\s]+)`
```

정규식 그룹으로 라벨 추출. 명명된 캡처 그룹(`?P<name>`) 사용.

### unpack 파서

```logql
{job="app"} | unpack
```

Promtail의 `pack` stage로 패킹된 로그를 풉니다.

---

## 라벨 필터

### 비교 연산자

| 연산자 | 의미 |
|--------|------|
| `==` 또는 `=` | 같음 |
| `!=` | 다름 |
| `>` , `>=` | 크다, 크거나 같다 |
| `<` , `<=` | 작다, 작거나 같다 |

### 논리 연산자

`and`, `or` 사용 가능.

### 데이터 타입

라벨 필터는 다음 타입을 자동 감지합니다.

- **String**: 따옴표 사용 `level="error"`
- **Duration**: `1ms`, `1s`, `1m`, `1h`
- **Number**: 숫자 비교 `status >= 400`
- **Bytes**: `1KB`, `1MB`, `1GB`

### 예시

```logql
{job="api"} | json | status >= 400
{job="api"} | logfmt | duration > 100ms
{job="api"} | json | level="error" and host="server1"
```

---

## 라인 필터

원본 로그 라인 자체에 대한 텍스트 필터입니다.

| 연산자 | 의미 |
|--------|------|
| `\|=` | 문자열 포함 |
| `!=` | 문자열 미포함 |
| `\|~` | 정규표현식 일치 |
| `!~` | 정규표현식 불일치 |

### 예시

```logql
{app="api"} |= "error"
{app="api"} != "DEBUG"
{app="api"} |~ "(?i)error|warning"
{app="api"} !~ "healthcheck"
```

### 체이닝

```logql
{app="api"} |= "error" != "test" |~ "user_id=\\d+"
```

### 성능 팁

라인 필터는 파서보다 빠르므로 **가능한 앞쪽에 두는 것이 좋습니다**.

---

## Format Expression

### `line_format`

로그 라인 자체를 변환합니다.

```logql
{job="api"} | json | line_format "{{.method}} {{.path}} took {{.duration}}"
```

Go 템플릿 문법을 사용합니다.

### `label_format`

라벨을 변경하거나 추가합니다.

```logql
{job="api"} | json | label_format service=app, normalized_status=`{{ if eq .status "200" }}OK{{ else }}ERROR{{ end }}`
```

---

## Metric 쿼리

로그를 메트릭으로 변환합니다. 두 가지 카테고리가 있습니다.

### Range 집계

특정 기간(range) 내의 로그를 집계합니다.

```logql
rate({app="api"}[5m])              # 초당 로그 수
count_over_time({app="api"}[5m])   # 5분간 총 로그 수
bytes_rate({app="api"}[5m])        # 초당 바이트
bytes_over_time({app="api"}[5m])   # 5분간 총 바이트
```

### Unwrap 집계

`unwrap`으로 추출한 숫자 값에 대한 집계입니다.

```logql
sum_over_time({job="api"} | json | unwrap duration [5m])
avg_over_time({job="api"} | json | unwrap response_size [5m])
quantile_over_time(0.95, {job="api"} | json | unwrap duration [5m])
max_over_time({job="api"} | json | unwrap duration [5m])
min_over_time({job="api"} | json | unwrap duration [5m])
stddev_over_time({job="api"} | json | unwrap duration [5m])
```

---

## 집계 함수

벡터 집계로 시계열을 그룹화합니다.

| 함수 | 설명 |
|------|------|
| `sum` | 합계 |
| `avg` | 평균 |
| `min` | 최소 |
| `max` | 최대 |
| `count` | 개수 |
| `stddev` | 표준편차 |
| `stdvar` | 분산 |
| `topk` | 상위 K |
| `bottomk` | 하위 K |
| `sort` | 오름차순 정렬 |
| `sort_desc` | 내림차순 정렬 |

### 그룹화

```logql
sum by (level) (rate({app="api"}[5m]))
sum without (instance) (rate({app="api"}[5m]))
```

### 예시

```logql
# 레벨별 에러 발생 비율
sum by (level) (rate({app="api"} |= "error" [5m]))

# 상위 5개 인스턴스의 로그 속도
topk(5, sum by (instance) (rate({app="api"}[5m])))

# 95퍼센타일 응답 시간
quantile_over_time(0.95, {app="api"} | json | unwrap duration [5m])
```

---

## Built-in 함수

### 산술 연산

```logql
sum(rate({app="api"}[5m])) * 60   # 분당 비율
```

### 비교 연산

```logql
sum(rate({app="api"}[5m])) > 100
```

### 논리 연산

`and`, `or`, `unless`

### 라벨 조작 함수

- `label_replace(v, dst, repl, src, regex)`: 라벨 추가/변경
- `label_join(v, dst, sep, src...)`: 라벨 결합

### 시간 함수

- `time()`: 현재 시간
- `timestamp()`: 샘플 타임스탬프

---

## 쿼리 예시 모음

### 에러 로그 모니터링

```logql
# 5분간 에러 발생 횟수
sum(count_over_time({namespace="prod"} |= "error" [5m]))

# 서비스별 에러 비율
sum by (service) (rate({namespace="prod"} |~ "(?i)error" [5m]))
```

### 응답 시간 분석

```logql
# JSON 로그에서 95퍼센타일 응답 시간
quantile_over_time(0.95,
  {app="api"}
  | json
  | unwrap duration_ms
  [5m]
)
```

### HTTP 상태 코드 분포

```logql
sum by (status) (
  rate(
    {app="api"}
    | logfmt
    | __error__=""
    [5m]
  )
)
```

### 로그 폭증 감지

```logql
# 평소 대비 로그 양이 갑자기 증가한 경우 감지
sum by (app) (rate({namespace="prod"}[5m]))
> 
2 * sum by (app) (rate({namespace="prod"}[1h] offset 1h))
```
