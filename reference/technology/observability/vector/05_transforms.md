# Vector Transforms (변환) 레퍼런스

> 공식 문서: https://vector.dev/docs/reference/configuration/transforms/

## 개요

Vector의 Transforms는 데이터가 파이프라인을 통과하는 동안 데이터를 변형하고 조작할 수 있게 해주는 컴포넌트입니다. Sources에서 수집한 데이터를 Sinks로 전송하기 전에 파싱, 필터링, 라우팅, 집계 등 다양한 처리를 수행할 수 있습니다.

Vector는 YAML, TOML, JSON 설정 형식을 지원합니다. 대부분의 Linux 시스템에서 설정 파일은 `/etc/vector/vector.yaml`에 위치합니다.

---

## 1. Remap Transform (VRL)

> 공식 문서: https://vector.dev/docs/reference/configuration/transforms/remap/

### 개요

Remap Transform은 Vector Remap Language(VRL)를 사용하여 이벤트 데이터를 수정하는 핵심 Transform입니다. Vector에서 데이터를 파싱, 변형, 조작하는 데 권장됩니다.

VRL은 관측 데이터(로그 및 메트릭)를 안전하고 효율적으로 처리하기 위해 특별히 설계된 표현식 지향 언어입니다. 기본적인 데이터 변형을 위해 여러 Transform을 체인으로 연결할 필요 없이, 단일 remap Transform에서 복잡한 변환을 수행할 수 있습니다.

### 기본 설정 예제

YAML:
```yaml
transforms:
  my_transform_id:
    type: remap
    inputs:
      - my-source-or-transform-id
    source: |
      .new_field = "hello"
      .timestamp = now()
```

TOML:
```toml
[transforms.my_transform_id]
type = "remap"
inputs = ["my-source-or-transform-id"]
source = '''
  .new_field = "hello"
  .timestamp = now()
'''
```

JSON:
```json
{
  "transforms": {
    "my_transform_id": {
      "type": "remap",
      "inputs": ["my-source-or-transform-id"],
      "source": ".new_field = \"hello\"\n.timestamp = now()"
    }
  }
}
```

### 외부 VRL 파일 사용

```yaml
transforms:
  parse_json_log:
    type: remap
    inputs:
      - stdin
    file: "./config/log_parser.vrl"
```

### 주요 설정 옵션

| 옵션 | 타입 | 설명 |
|------|------|------|
| `inputs` | array | 필수. 업스트림 source 또는 transform ID 목록. 와일드카드(*) 지원 |
| `source` | string | VRL 프로그램 소스 코드 |
| `file` | string | VRL 프로그램 파일 경로 (source 대신 사용 가능) |
| `drop_on_error` | boolean | 에러 발생 시 이벤트 삭제 여부 (기본값: false) |
| `drop_on_abort` | boolean | abort 발생 시 이벤트 삭제 여부 (기본값: true) |
| `reroute_dropped` | boolean | 삭제된 이벤트를 별도 출력으로 라우팅 (기본값: false) |
| `timezone` | string | 타임스탬프 변환에 사용할 시간대 |

### 에러 처리 및 재라우팅

`drop_on_error` 또는 `drop_on_abort`가 true이고 `reroute_dropped`도 true이면, 런타임 에러나 abort가 발생한 이벤트는 기본 출력 스트림에서 제거되어 dropped 출력으로 전송됩니다.

```yaml
transforms:
  parse_logs:
    type: remap
    inputs:
      - raw_logs
    source: |
      . = parse_json!(.message)
    drop_on_error: true
    reroute_dropped: true

  # 파싱 실패한 이벤트 처리
  handle_failed:
    type: remap
    inputs:
      - parse_logs.dropped
    source: |
      .error = "JSON 파싱 실패"
```

### VRL 예제

#### JSON 파싱
```yaml
transforms:
  parse_json:
    type: remap
    inputs:
      - raw_logs
    source: |
      . = parse_json!(.message)
```

#### Syslog 파싱
```yaml
transforms:
  parse_syslog:
    type: remap
    inputs:
      - syslog_source
    source: |
      . |= parse_syslog!(.message)
```

#### Apache 로그 파싱
```yaml
transforms:
  parse_apache:
    type: remap
    inputs:
      - apache_logs
    source: |
      . = parse_apache_log!(.message, format: "combined")
```

#### 필드 추가 및 변환
```yaml
transforms:
  enrich_logs:
    type: remap
    inputs:
      - parsed_logs
    source: |
      .environment = "production"
      .processed_at = now()
      .level = downcase!(.level)
      del(.unnecessary_field)
```

### VRL 테스트

Vector REPL을 사용하여 VRL 프로그램을 테스트할 수 있습니다:

```bash
vector vrl
```

REPL에서 `help`를 입력하면 도움말을 볼 수 있습니다. REPL은 실제 Vector 설정과 거의 동일하게 동작하므로, 프로덕션에 적용하기 전에 복잡한 프로그램의 개별 스니펫을 검증할 수 있습니다.

### VRL 주요 함수 카테고리

VRL은 다양한 내장 함수를 제공합니다:

- Array 함수: `append`, `chunks`, `push`, `zip` 등
- String 함수: `upcase`, `downcase`, `replace`, `split` 등
- Parse 함수: `parse_json`, `parse_syslog`, `parse_apache_log`, `parse_regex` 등
- Timestamp 함수: `now`, `format_timestamp`, `parse_timestamp`, `from_unix_timestamp` 등
- Path 함수: `del`, `exists`, `get` 등
- Type 함수: `type_of`, `is_string`, `is_integer` 등

> VRL 함수 전체 레퍼런스: https://vector.dev/docs/reference/vrl/functions/

---

## 2. Filter Transform

> 공식 문서: https://vector.dev/docs/reference/configuration/transforms/filter/

### 개요

Filter Transform은 조건에 따라 이벤트를 필터링합니다. 조건에 일치하는 이벤트만 다운스트림으로 전달되고, 일치하지 않는 이벤트는 삭제됩니다.

### 기본 설정 예제

YAML:
```yaml
transforms:
  my_transform_id:
    type: filter
    inputs:
      - my-source-or-transform-id
    condition: '.level != "debug"'
```

TOML:
```toml
[transforms.my_transform_id]
type = "filter"
inputs = ["my-source-or-transform-id"]
condition = '.level != "debug"'
```

JSON:
```json
{
  "transforms": {
    "my_transform_id": {
      "type": "filter",
      "inputs": ["my-source-or-transform-id"],
      "condition": ".level != \"debug\""
    }
  }
}
```

### 주요 설정 옵션

| 옵션 | 타입 | 설명 |
|------|------|------|
| `inputs` | array | 필수. 업스트림 source 또는 transform ID 목록 |
| `condition` | string | 필수. 각 이벤트에 적용되는 VRL 조건 표현식 |

### 실제 사용 예제

#### 심각도 기반 필터링
```yaml
transforms:
  filter_out_info:
    type: filter
    inputs:
      - logs
    condition: '.severity != "info"'
```

#### 복합 조건 필터링
```yaml
transforms:
  filter_important:
    type: filter
    inputs:
      - logs
    condition: '.severity != "info" && .status_code < 400 && exists(.host)'
```

#### 소스 기반 필터링
```toml
[transforms.filter_process]
inputs = ["source0"]
type = "filter"
condition = '''
.source == "PROCESS_SERVICE_CHECK_RESULT"
'''
```

### Remap 조건 타입 사용

```toml
[transforms.filter_out_non_critical]
type = "filter"
inputs = ["http-server-logs"]

condition.type = "remap"
condition.source = '.status_code != 200 && !includes(["info", "debug"], .severity)'
```

---

## 3. Route Transform

> 공식 문서: https://vector.dev/docs/reference/configuration/transforms/route/

### 개요

Route Transform은 사용자 정의 조건에 따라 이벤트를 고유한 서브 스트림으로 분기합니다. 하나의 이벤트가 여러 라우트에 동시에 전달될 수 있습니다.

### 기본 설정 예제

YAML:
```yaml
transforms:
  my_transform_id:
    type: route
    inputs:
      - my-source-or-transform-id
    route:
      app: '.namespace == "app"'
      host: '.namespace == "host"'
```

TOML:
```toml
[transforms.my_transform_id]
type = "route"
inputs = ["my-source-or-transform-id"]

[transforms.my_transform_id.route]
app = '.namespace == "app"'
host = '.namespace == "host"'
```

### 주요 설정 옵션

| 옵션 | 타입 | 설명 |
|------|------|------|
| `inputs` | array | 필수. 업스트림 source 또는 transform ID 목록 |
| `route` | map | 필수. 라우트 식별자에서 논리적 조건으로의 맵 |
| `reroute_unmatched` | boolean | 매치되지 않는 이벤트를 `_unmatched` 출력으로 라우팅 (기본값: true) |

### 라우트를 입력으로 참조

각 라우트는 `<transform_name>.<route_id>` 형식으로 다른 컴포넌트의 입력으로 참조할 수 있습니다.

```yaml
transforms:
  route_by_level:
    type: route
    inputs:
      - parsed_logs
    route:
      error: '.level == "error"'
      warning: '.level == "warning"'
      info: '.level == "info"'

  # 에러 로그 처리
  process_errors:
    type: remap
    inputs:
      - route_by_level.error
    source: |
      .alert = true

  # 경고 로그 처리
  process_warnings:
    type: remap
    inputs:
      - route_by_level.warning
    source: |
      .needs_attention = true
```

### VRL 조건 사용 예제

```yaml
transforms:
  my_transform_id:
    type: route
    inputs:
      - my-source-or-transform-id
    reroute_unmatched: true
    route:
      foo-does-not-exist:
        source: "!exists(.foo)"
        type: vrl
      foo-exists:
        source: exists(.foo)
        type: vrl
```

### 매치되지 않는 이벤트 처리

`reroute_unmatched`가 true(기본값)이면, 어떤 라우트와도 일치하지 않는 이벤트는 `<transform_name>._unmatched` 출력으로 전송됩니다. false로 설정하면 일치하지 않는 이벤트는 자동으로 삭제됩니다.

```yaml
transforms:
  route_logs:
    type: route
    inputs:
      - all_logs
    reroute_unmatched: true
    route:
      critical: '.level == "critical"'

  # 매치되지 않는 이벤트 처리
  handle_unmatched:
    type: remap
    inputs:
      - route_logs._unmatched
    source: |
      .route_status = "unmatched"
```

### 지원되는 이벤트 타입

- Log 이벤트: 지원
- Trace 이벤트: 지원

---

## 4. Exclusive Route Transform

> 공식 문서: https://vector.dev/docs/reference/configuration/transforms/exclusive_route/

### 개요

Exclusive Route Transform은 이벤트를 단일 출력으로만 라우팅합니다. 일반 route transform과 달리 first-match-wins 방식으로 동작하며, 라우트는 선언 순서대로 평가됩니다.

### 기본 설정 예제

YAML:
```yaml
transforms:
  transform0:
    inputs:
      - source0
    type: exclusive_route
    routes:
      - name: "a"
        condition:
          type: vrl
          source: .level == 1
      - name: "b"
        condition:
          type: vrl
          source: .level == 2
```

### 간단한 조건 구문

```yaml
transforms:
  transform0:
    inputs:
      - source0
    type: exclusive_route
    routes:
      - name: "foo"
        condition: '.origin == "foo"'
      - name: "bar"
        condition: '.origin == "bar"'
```

### JSON 예제

```json
{
  "transforms": {
    "transform0": {
      "type": "exclusive_route",
      "inputs": ["source0"],
      "routes": [
        {
          "name": "foo-and-bar-exist",
          "condition": {
            "source": "exists(.foo) && exists(.bar)",
            "type": "vrl"
          }
        },
        {
          "name": "only-foo-exists",
          "condition": {
            "source": "exists(.foo)",
            "type": "vrl"
          }
        }
      ]
    }
  }
}
```

### 주요 설정 옵션

| 옵션 | 타입 | 설명 |
|------|------|------|
| `inputs` | array | 필수. 업스트림 source 또는 transform ID 목록 |
| `routes` | array | 필수. 순서대로 평가되는 라우트 배열 |
| `routes[].name` | string | 라우트 이름 (고유해야 함, `_unmatched` 예약어) |
| `routes[].condition` | string/object | VRL 조건 표현식 |

### 라우트 참조

- 각 라우트는 `<transform_name>.<name>` 형식으로 참조 가능
- 어떤 라우트와도 매치되지 않는 이벤트는 `<transform_name>._unmatched`로 전송

### 지원되는 이벤트 타입

- Log 이벤트: 지원
- Trace 이벤트: 지원

---

## 5. Aggregate Transform

> 공식 문서: https://vector.dev/docs/reference/configuration/transforms/aggregate/

### 개요

Aggregate Transform은 설정된 시간 간격 동안 메트릭 이벤트를 집계합니다. 볼륨 감소가 주요 장점으로, 메트릭 볼륨 기준으로 요금이 부과되는 환경에서 직접적인 비용 절감 효과를 얻거나, CPU 처리량과 네트워크 대역폭을 줄여 간접적으로 비용을 절감할 수 있습니다.

### 작동 방식

메트릭은 종류(kind)에 따라 다르게 집계됩니다:
- Incremental 메트릭: 간격 동안 누적됩니다
- Absolute 메트릭: 새 값이 이전 값을 대체합니다

예를 들어, 값이 10과 13인 두 incremental 카운터 메트릭이 한 기간에 처리되면, 값이 23인 단일 incremental 카운터로 집계됩니다.

### 기본 설정 예제

YAML:
```yaml
transforms:
  my_transform_id:
    type: aggregate
    inputs:
      - my-source-or-transform-id
    interval_ms: 10000
    mode: Auto
```

TOML:
```toml
[transforms.my_transform_id]
type = "aggregate"
inputs = ["my-source-or-transform-id"]
interval_ms = 5000
```

### 주요 설정 옵션

| 옵션 | 타입 | 설명 |
|------|------|------|
| `inputs` | array | 필수. 업스트림 source 또는 transform ID 목록 |
| `interval_ms` | integer | 플러시 간격 (밀리초). 이 시간 동안 동일한 시리즈 데이터(이름, 네임스페이스, 태그 등)를 가진 메트릭이 집계됨 |
| `mode` | string | 집계에 사용할 함수. 일부 함수는 incremental에서만, 일부는 absolute에서만 작동 |

### 실제 사용 예제

```yaml
transforms:
  aggregate_metrics:
    type: aggregate
    inputs:
      - host_metrics
    interval_ms: 60000  # 1분마다 집계

sinks:
  prometheus:
    type: prometheus_exporter
    inputs:
      - aggregate_metrics
    address: "0.0.0.0:9598"
```

---

## 6. Dedupe Transform

> 공식 문서: https://vector.dev/docs/reference/configuration/transforms/dedupe/

### 개요

Dedupe Transform은 파이프라인을 통과하는 로그에서 중복 이벤트를 제거합니다. 데이터 무결성을 유지하고 의도치 않은 로그 중복을 방지하는 데 유용합니다.

### 작동 방식

이 Transform은 `cache.num_events` 크기의 LRU 캐시를 사용합니다. 최근 처리한 `cache.num_events`개의 이벤트 정보를 메모리에 유지하며, 항목은 삽입 순서대로 캐시에서 제거됩니다.

캐시에 이미 존재하는 이벤트의 중복이 수신되면, 해당 이벤트는 캐시의 최신 위치로 이동하며 제거 대기열에서의 순서가 초기화됩니다.

### 기본 설정 예제

YAML:
```yaml
transforms:
  my_transform_id:
    type: dedupe
    inputs:
      - my-source-or-transform-id
```

TOML (필드 매칭 포함):
```toml
[transforms.my_transform_id]
type = "dedupe"
inputs = ["my-source-id"]
fields.match = ["timestamp", "host", "message"]
```

### 주요 설정 옵션

| 옵션 | 타입 | 설명 |
|------|------|------|
| `inputs` | array | 필수. 업스트림 source 또는 transform ID 목록 |
| `fields.match` | array | 매칭에 사용할 필드 목록 |
| `fields.ignore` | array | 매칭에서 제외할 필드 목록 |
| `cache.num_events` | integer | 캐시에 저장할 최대 이벤트 수 |

### 필드 매칭 옵션

- `fields.match`: 지정된 필드만 매칭에 고려됩니다
- `fields.ignore`: 지정된 필드를 제외한 모든 필드가 매칭에 포함됩니다

### 실제 사용 예제

```yaml
transforms:
  dedupe_logs:
    type: dedupe
    inputs:
      - parsed_logs
    fields:
      match:
        - timestamp
        - host
        - message
    cache:
      num_events: 5000
```

### 중요 사항

- 명시적으로 null 값을 가진 필드는 해당 필드가 완전히 생략된 것과 항상 다른 것으로 간주됩니다
- 예: `fields.match = ["a"]`로 실행할 때, `{a: null, b:5}`와 `{b:5}`는 서로 다른 이벤트로 간주됩니다
- 이 컴포넌트는 상태를 가지므로, 이전 입력(이벤트)에 따라 동작이 변경됩니다

---

## 7. Reduce Transform

> 공식 문서: https://vector.dev/docs/reference/configuration/transforms/reduce/

### 개요

Reduce Transform은 조건과 병합 전략에 따라 여러 로그 이벤트를 단일 이벤트로 합칩니다. 다수의 소규모 이벤트 스트림을 더 적은 수의 이벤트 스트림으로 변환할 수 있습니다.

### 기본 설정 예제

YAML:
```yaml
transforms:
  my_transform_id:
    type: reduce
    inputs:
      - my-source-or-transform-id
    group_by:
      - host
      - pid
      - tid
    merge_strategies:
      message: concat_newline
      pid: discard
      tid: discard
    starts_when: match(string!(.message), r'^...')
```

### 주요 설정 옵션

| 옵션 | 타입 | 설명 |
|------|------|------|
| `inputs` | array | 필수. 업스트림 source 또는 transform ID 목록 |
| `group_by` | array | 이벤트 그룹화에 사용할 필드 목록 |
| `merge_strategies` | map | 필드별 사용자 정의 병합 전략 |
| `expire_after_ms` | integer | 그룹 플러시 간격 (밀리초) |
| `ends_when` | string | 트랜잭션의 마지막 이벤트를 구분하는 조건 |
| `starts_when` | string | 트랜잭션의 시작 이벤트를 구분하는 조건 |

### 병합 전략

| 전략 | 설명 |
|------|------|
| `concat` | 문자열 값 연결 |
| `concat_newline` | 개행 문자로 문자열 값 연결 |
| `concat_raw` | 구분자 없이 문자열 값 연결 |
| `array` | 값을 배열로 수집 |
| `sum` | 숫자 값 합계 |
| `max` | 최대값 |
| `min` | 최소값 |
| `discard` | 첫 번째 값만 유지하고 나머지 삭제 |
| `retain` | 마지막 값 유지 |
| `flat_unique` | 고유 값만 평면 배열로 수집 |

### 기본 병합 동작

- 문자열 필드: 첫 번째 값 유지, 후속 값 삭제
- 타임스탬프 필드: 첫 번째 값 유지, `[field-name]_end` 필드에 마지막 타임스탬프 추가
- 숫자 필드: 합계

### 실제 사용 예제

TOML:
```toml
[transforms.reduce_transactions]
inputs = ["parsed_logs"]
type = "reduce"
group_by = ["request_id"]
ends_when.type = "vrl"
ends_when.source = "exists(.response_status)"
merge_strategies.message = "discard"
merge_strategies.query = "discard"
merge_strategies.query_duration_ms = "sum"
merge_strategies.render_duration_ms = "sum"
merge_strategies.response_duration_ms = "sum"
```

### VRL 조건 사용

VRL 표현식을 사용하면 조건을 더 간결하고 표현력 있게 작성할 수 있습니다:

```yaml
transforms:
  reduce_multiline:
    type: reduce
    inputs:
      - logs
    group_by:
      - host
    starts_when: 'match(string!(.message), r"^\d{4}-\d{2}-\d{2}")'
    merge_strategies:
      message: concat_newline
```

---

## 8. Sample Transform

> 공식 문서: https://vector.dev/docs/reference/configuration/transforms/sample/

### 개요

Sample Transform은 설정된 비율로 이벤트를 샘플링합니다. 전체 이벤트 중 일부만 선택적으로 처리할 때 유용합니다.

### 기본 설정 예제

YAML:
```yaml
transforms:
  my_transform_id:
    type: sample
    inputs:
      - my-source-or-transform-id
    rate: 10
```

TOML:
```toml
[transforms.my_transform_id]
type = "sample"
inputs = ["my-source-or-transform-id"]
rate = 10
```

JSON:
```json
{
  "transforms": {
    "my_transform_id": {
      "type": "sample",
      "inputs": ["my-source-or-transform-id"],
      "rate": 10
    }
  }
}
```

### 주요 설정 옵션

| 옵션 | 타입 | 설명 |
|------|------|------|
| `inputs` | array | 필수. 업스트림 source 또는 transform ID 목록 |
| `rate` | integer | 1/N으로 표현되는 샘플링 비율. 예: rate=10이면 10개 중 1개 전달 |
| `ratio` | float | 전달되는 이벤트 비율 (0.0 ~ 1.0). 예: ratio=0.13이면 13% 전달 |
| `key` | string | 이벤트 샘플링 여부를 결정하기 위해 해시되는 필드 이름 |
| `group_by` | string | 별도로 샘플링할 그룹을 결정하는 값. Vector 템플릿 구문 지원 |
| `condition` | string | VRL 조건 표현식 |

### rate vs ratio

- rate: 1/N으로 표현됨. rate=1500이면 1500개 중 1개 전달
- ratio: 비율로 표현됨. ratio=0.13이면 13% 전달

두 옵션을 동시에 설정하면 에러가 발생합니다.

### 실제 사용 예제

```yaml
transforms:
  apache_parser:
    type: remap
    inputs:
      - apache_logs
    source: '. = parse_apache_log!(.message)'

  apache_sampler:
    type: sample
    inputs:
      - apache_parser
    rate: 50  # 50개 중 1개만 샘플링
```

### 지원되는 이벤트 타입

- Log 이벤트: 지원
- Trace 이벤트: 지원

---

## 9. Throttle Transform

> 공식 문서: https://vector.dev/docs/reference/configuration/transforms/throttle/

### 개요

Throttle Transform은 파이프라인을 통과하는 로그의 처리 속도를 제한합니다. Generic Cell Rate Algorithm을 사용하여 이벤트 스트림을 조율합니다.

### 작동 방식

throttle transform은 `key_field`에 따라 이벤트를 버킷으로 분류합니다(`key_field`를 지정하지 않으면 단일 버킷을 사용). 각 버킷은 독립적으로 속도 제한됩니다.

속도 제한기는 "셀"을 소비하여 이벤트 통과 여부를 결정합니다. 각 이벤트는 사용 가능한 셀을 하나씩 소비하며, 셀이 부족하면 해당 이벤트는 속도 제한에 걸립니다.

예를 들어, `window_secs`가 60이고 `threshold`가 10이면 6초마다 셀이 하나씩 보충되며, 최대 10개 이벤트의 버스트를 허용합니다.

### 기본 설정 예제

YAML:
```yaml
transforms:
  my_transform_id:
    type: throttle
    inputs:
      - my-source-or-transform-id
    threshold: 100
    window_secs: 60
```

TOML:
```toml
[transforms.my_transform_id]
type = "throttle"
inputs = ["my-source-or-transform-id"]
threshold = 100
window_secs = 60
```

### key_field를 사용한 개별 속도 제한

```yaml
transforms:
  throttle_per_host:
    type: throttle
    inputs:
      - logs
    key_field: "{{ host }}"
    threshold: 100
    window_secs: 60
```

### 주요 설정 옵션

| 옵션 | 타입 | 설명 |
|------|------|------|
| `inputs` | array | 필수. 업스트림 source 또는 transform ID 목록 |
| `threshold` | integer | 필수. 시간 창 내에서 허용되는 최대 이벤트 수 |
| `window_secs` | integer | 필수. 임계값이 적용되는 시간 창 (초) |
| `key_field` | string | 개별적으로 속도 제한할 그룹을 결정하는 값. 템플릿 구문 지원 |
| `exclude` | string | 속도 제한에서 제외할 VRL 조건 |

### 내부 메트릭 설정

throttle transform의 `events_discarded_total` 내부 메트릭(key 태그 포함)은 opt-in 방식으로만 출력됩니다. 이 메트릭을 활성화하려면 `internal_metrics.emit_events_discarded_per_key`를 true로 설정하세요.

```yaml
transforms:
  my_throttle:
    type: throttle
    inputs:
      - logs
    threshold: 100
    window_secs: 60
    internal_metrics:
      emit_events_discarded_per_key: true
```

주의: key 태그는 카디널리티가 무한히 증가할 수 있으므로 기본값은 false입니다. 고유 키의 수가 제한적인 경우에만 true로 설정하세요.

---

## 10. Log to Metric Transform

> 공식 문서: https://vector.dev/docs/reference/configuration/transforms/log_to_metric/

### 개요

Log to Metric Transform은 로그 이벤트에서 하나 이상의 메트릭 이벤트를 파생합니다.

중요: 이 Transform은 여러 로그를 하나의 메트릭으로 집계하지 않습니다. 로그 이벤트를 세분화된 개별 메트릭으로 변환하며, 이후 엣지에서 별도로 집계할 수 있습니다.

### 메트릭 타입

| 타입 | 설명 |
|------|------|
| `counter` | 증가 또는 0으로 리셋될 수 있는 단일 값 (감소 불가) |
| `gauge` | 증가하거나 감소할 수 있는 시점 값 |
| `histogram` | 샘플링된 값의 분포를 나타냄 |
| `set` | 고유 값의 집합 |
| `summary` | 샘플링된 값의 분포 (글로벌 히스토그램 및 요약을 지원하는 서비스에서 사용) |

### 기본 설정 예제

YAML (Counter):
```yaml
transforms:
  log_to_metric:
    type: log_to_metric
    inputs:
      - parsed_logs
    metrics:
      - type: counter
        field: status
        name: response_total
        namespace: service
        tags:
          status: "{{status}}"
          host: "{{host}}"
```

### increment_by_value 사용

필드 값만큼 카운터를 증가시키려면 `increment_by_value`를 true로 설정합니다:

```yaml
transforms:
  log_to_metric:
    type: log_to_metric
    inputs:
      - logs
    metrics:
      - type: counter
        field: bytes_sent
        name: bytes_total
        increment_by_value: true
```

### Summary 메트릭 예제

```yaml
transforms:
  timing_metrics:
    type: log_to_metric
    inputs:
      - logs
    metrics:
      - type: summary
        field: response_time
        name: response_time_ms
```

### Nginx 메트릭 예제

```yaml
transforms:
  metric_nginx:
    type: log_to_metric
    inputs:
      - json_transform
    metrics:
      - type: counter
        field: app_name
        kind: incremental
        name: http_requests_count
        tags:
          status: "{{status}}"
          host: "{{host}}"
          org_id: "{{org_id}}"
          path: "{{path}}"
```

### 주요 동작

- 단일 로그 이벤트에서 여러 메트릭 이벤트가 생성되는 경우, 메트릭은 배열이 아닌 개별 이벤트로 출력됩니다
- 다운스트림 컴포넌트는 해당 메트릭이 단일 로그에서 파생되었다는 사실을 알 수 없습니다
- 대상 로그 필드의 값이 null이면 해당 항목은 무시되며 메트릭이 생성되지 않습니다

### VRL을 사용한 고급 사용법 (v0.35.0+)

`all_metrics` 옵션을 사용하면 VRL로 구조화된 로그 이벤트를 메트릭으로 변환하는 커스텀 코드를 작성할 수 있습니다.

---

## 11. Metric to Log Transform

> 공식 문서: https://vector.dev/docs/reference/configuration/transforms/metric_to_log/

### 개요

Metric to Log Transform은 메트릭 이벤트를 로그 이벤트로 변환합니다. 로그만 지원하는 다운스트림 컴포넌트로 메트릭을 전달할 때 유용합니다.

### 기본 설정 예제

YAML:
```yaml
transforms:
  my_transform_id:
    type: metric_to_log
    inputs:
      - my-source-or-transform-id
```

TOML:
```toml
[transforms.my_transform_id]
type = "metric_to_log"
inputs = ["my-source-or-transform-id"]
```

JSON:
```json
{
  "transforms": {
    "my_transform_id": {
      "type": "metric_to_log",
      "inputs": ["my-source-or-transform-id"]
    }
  }
}
```

### host_tag 사용

```toml
[transforms.my_transform_id]
type = "metric_to_log"
inputs = ["my-source-or-transform-id"]
host_tag = "host"
```

### 주요 설정 옵션

| 옵션 | 타입 | 설명 |
|------|------|------|
| `inputs` | array | 필수. 업스트림 source 또는 transform ID 목록 |
| `host_tag` | string | 소스 호스트에 사용할 메트릭 태그 이름. 해당 태그 값이 생성된 로그 이벤트의 `host` 필드에 설정됨 |
| `metric_tag_values` | string | 메트릭 태그 값 인코딩 방식. `single`: 마지막 값만 표시, `full`: 모든 태그를 별도 할당으로 표시 |
| `timezone` | string | 타임스탬프 변환에 사용할 시간대 |

### 변환 예제

입력 (Histogram 메트릭):
```json
{
  "metric": {
    "histogram": {
      "buckets": [
        { "count": 10, "upper_limit": 1 },
        { "count": 20, "upper_limit": 2 }
      ],
      "count": 30,
      "sum": 50
    },
    "kind": "absolute",
    "name": "histogram",
    "tags": { "code": "200", "host": "my.host.com" },
    "timestamp": "2020-08-01T21:15:47+00:00"
  }
}
```

출력 (로그 이벤트):
히스토그램 데이터가 로그 필드로 변환되며, `host_tag = "host"` 설정 시 태그의 host 값이 로그 이벤트의 `host` 필드로 추출됩니다.

---

## 12. Lua Transform

> 공식 문서: https://vector.dev/docs/reference/configuration/transforms/lua/

### 개요

Lua Transform은 Lua 프로그래밍 언어로 이벤트 데이터를 수정합니다. 내장 Lua 5.4 엔진을 통해 이벤트를 변환합니다.

주의: lua transform은 remap transform보다 약 60% 느리므로, 가능하면 remap transform을 사용하는 것이 좋습니다. lua transform은 remap transform으로 처리하기 어려운 엣지 케이스를 위해 설계되었습니다.

### Hooks

| Hook | 설명 |
|------|------|
| `init` | Transform 초기화 시 호출 |
| `process` | 각 수신 이벤트에 대해 호출 |
| `shutdown` | Transform 종료 시 호출 |

`process`가 핵심 hook으로, 단일 이벤트를 입력으로 받아 두 번째 인자로 전달되는 `emit` 함수를 통해 하나 이상의 이벤트를 출력할 수 있습니다.

### Timers

타이머 핸들러는 hook과 마찬가지로 이벤트를 생성할 수 있는 Lua 함수입니다. 다만 미리 정해진 간격으로 주기적으로 호출된다는 점이 다릅니다.

### 기본 설정 예제

TOML:
```toml
[transforms.aggregator]
type = "lua"
version = "2"
inputs = ["source0"]
hooks.init = """
  function (emit)
    count = 0  -- 전역 변수를 설정하여 상태 초기화
  end
"""
hooks.process = """
  function (event, emit)
    count = count + 1  -- 카운터 증가 후 종료
  end
"""
timers = [{interval_seconds = 5, handler = """
  function (emit)
    emit {
      metric = {
        name = "event_counter",
        kind = "incremental",
        timestamp = os.date("!*t"),
        counter = { value = count }
      }
    }
    count = 0
  end
"""}]
```

### 주요 설정 옵션

| 옵션 | 타입 | 설명 |
|------|------|------|
| `inputs` | array | 필수. 업스트림 source 또는 transform ID 목록 |
| `version` | string | 필수. Lua transform API 버전 ("2" 권장) |
| `hooks.init` | string | 초기화 hook Lua 코드 |
| `hooks.process` | string | 필수. 이벤트 처리 hook Lua 코드 |
| `hooks.shutdown` | string | 종료 hook Lua 코드 |
| `timers` | array | 주기적으로 호출되는 타이머 핸들러 목록 |
| `search_dirs` | array | Lua require 함수에서 검색할 디렉토리 경로 |

### 외부 Lua 모듈 사용

```toml
[transforms.my_lua]
type = "lua"
version = "2"
inputs = ["source0"]
source = "require('my_aggregator')"
```

### Version 2 개선사항

Version 2는 개선된 API, 더 나은 데이터 처리 인터페이스, 향상된 성능을 제공합니다:

- 이벤트를 타입 변환이 적용된 Lua 테이블로 표현
- 전역 상태 유지를 위한 hooks 도입
- 시간 기반 플러시를 위한 타이머 도입 (집계에 유용)
- 로그 이벤트 외에 메트릭 이벤트 처리 지원

---

## 13. Tag Cardinality Limit Transform

> 공식 문서: https://vector.dev/docs/reference/configuration/transforms/tag_cardinality_limit/

### 개요

Tag Cardinality Limit Transform은 메트릭 이벤트의 태그 카디널리티를 제한하여 카디널리티 폭발을 방지합니다.

Prometheus에서는 카디널리티가 높은 메트릭 이름과 레이블이 성능 및 안정성 문제를 일으킬 수 있어 주의가 필요합니다.

### 기본 설정 예제

YAML:
```yaml
transforms:
  my_transform_id:
    type: tag_cardinality_limit
    inputs:
      - my-source-or-transform-id
    cache_size_per_key: 5120
    limit_exceeded_action: drop_tag
    mode: exact
    value_limit: 500
```

### 주요 설정 옵션

| 옵션 | 타입 | 설명 |
|------|------|------|
| `inputs` | array | 필수. 업스트림 source 또는 transform ID 목록 |
| `mode` | string | 알고리즘 모드. `exact`: 정확한 중복 감지, `probabilistic`: 확률적 감지 (메모리 효율적) |
| `value_limit` | integer | 허용되는 고유 태그 값의 수 |
| `limit_exceeded_action` | string | 제한 초과 시 동작. `drop_tag`: 태그 삭제, `drop_event`: 이벤트 삭제 |
| `cache_size_per_key` | integer | 중복 태그 감지를 위한 캐시 크기 (바이트) |

### 모드 설명

exact 모드:
- 처리한 모든 메트릭 이벤트의 태그 키 복사본을 메모리에 저장
- 각 키에 대해 `value_limit`개의 고유 값을 확인할 때까지 해당 값들도 메모리에 저장
- 한도를 초과하면 해당 키의 새 값은 거부됨

probabilistic 모드:
- 모든 값을 저장하는 대신 블룸 필터를 사용하여 각 고유 키의 값이 이전에 나타났는지 확률적으로 판단
- 메모리 효율적이지만 오탐(false positive) 가능성이 있음

### 실제 사용 예제

```yaml
transforms:
  limit_cardinality:
    type: tag_cardinality_limit
    inputs:
      - host_metrics
    mode: probabilistic
    value_limit: 1000
    limit_exceeded_action: drop_tag
```

---

## 14. AWS EC2 Metadata Transform

> 공식 문서: https://vector.dev/docs/reference/configuration/transforms/aws_ec2_metadata/

### 개요

AWS EC2 Metadata Transform은 AWS EC2 인스턴스 메타데이터를 이벤트에 추가합니다.

### 기본 설정 예제

YAML:
```yaml
transforms:
  my_transform_id:
    type: aws_ec2_metadata
    inputs:
      - my-source-or-transform-id
    endpoint: http://169.254.169.254
    fields:
      - instance-id
      - region
      - availability-zone
    refresh_interval_secs: 10
    refresh_timeout_secs: 1
    required: true
    tags:
      - Name
```

### 주요 설정 옵션

| 옵션 | 타입 | 설명 |
|------|------|------|
| `inputs` | array | 필수. 업스트림 source 또는 transform ID 목록 |
| `endpoint` | string | EC2 메타데이터 엔드포인트 (기본값: http://169.254.169.254) |
| `fields` | array | 이벤트에 포함할 메타데이터 필드 목록 |
| `namespace` | string | Transform이 추가하는 모든 이벤트 필드의 접두사 |
| `refresh_interval_secs` | integer | 업데이트된 메타데이터 쿼리 간격 (초) |
| `tags` | array | 이벤트에 포함할 인스턴스 태그 목록 |

### 기본 필드 목록

- `ami-id`
- `availability-zone`
- `instance-id`
- `instance-type`
- `local-hostname`
- `local-ipv4`
- `public-hostname`
- `public-ipv4`
- `region`
- `subnet-id`
- `vpc-id`
- `role-name`
- `account-id`

### 중요 사항

Docker에서 EC2 사용 시:
```bash
aws ec2 modify-instance-metadata-options \
  --instance-id <ID> \
  --http-endpoint enabled \
  --http-put-response-hop-limit 2
```

인스턴스 태그 액세스 활성화:
```bash
aws ec2 modify-instance-metadata-options \
  --instance-id <ID> \
  --instance-metadata-tags enabled
```

주의: Vector를 Aggregator로 실행하는 경우 이 Transform을 활성화하지 마세요. 메타데이터가 클라이언트가 아닌 Aggregator 노드의 메타데이터 서버에서 조회됩니다.

### 지원되는 이벤트 타입

- Log 이벤트: 지원

---

## 15. Window Transform

> 공식 문서: https://vector.dev/docs/reference/configuration/transforms/window/

### 개요

Window Transform은 링 버퍼 기반의 슬라이딩 윈도우로 구현된 백트레이스 로깅 컴포넌트입니다. `flush_when` 조건이 일치할 때까지 이벤트를 버퍼에 보관하며, 버퍼가 가득 차면 가장 오래된 이벤트부터 삭제됩니다.

### 작동 방식

이벤트 스트림이 Transform을 통과할 때, `flush_when` 조건과 일치하는 이벤트를 기준으로 `num_events_before`와 `num_events_after` 범위의 "윈도우"가 구성됩니다. 조건이 일치하면 해당 이벤트와 `num_events_after`가 0보다 큰 경우 이후 이벤트를 포함한 전체 윈도우가 출력으로 플러시됩니다.

백트레이스 로깅 또는 링 버퍼 로깅이라고도 합니다.

### 기본 설정 예제

YAML:
```yaml
transforms:
  my_window:
    type: window
    inputs:
      - logs
    flush_when: '.level == "error"'
    num_events_before: 10
    num_events_after: 5
```

### 주요 설정 옵션

| 옵션 | 타입 | 설명 |
|------|------|------|
| `inputs` | array | 필수. 업스트림 source 또는 transform ID 목록 |
| `flush_when` | string | 필수. 윈도우 플러시를 트리거하는 VRL 조건 |
| `num_events_before` | integer | `flush_when` 조건과 일치하는 이벤트 이전에 보관할 최대 이벤트 수 |
| `num_events_after` | integer | `flush_when` 조건과 일치하는 이벤트 이후에 보관할 최대 이벤트 수 |
| `pass_through` | string | 버퍼링 없이 이벤트를 통과시키는 조건 (주의해서 사용) |

### 버퍼 동작

- 과거 이벤트는 `num_events_before` 크기의 메모리 버퍼에 저장됨
- 버퍼가 가득 차면 새 이벤트를 위해 가장 오래된 이벤트가 제거됨
- 버퍼는 영속적이지 않으므로 시스템 크래시 시 버퍼링된 이벤트가 모두 손실됨
- `flush_when` 조건이 버퍼가 채워지기 전에 일치하면 즉시 플러시됨

### 실제 사용 예제

에러 발생 시 컨텍스트 로그 캡처:

```yaml
transforms:
  error_context:
    type: window
    inputs:
      - application_logs
    flush_when: '.level == "error" || .level == "fatal"'
    num_events_before: 20  # 에러 전 20개 이벤트
    num_events_after: 10   # 에러 후 10개 이벤트
```

### 지원되는 이벤트 타입

- Log 이벤트: 지원

---

## Transform 구성 생성

Vector의 `generate` 명령으로 Transform이 포함된 보일러플레이트 설정을 생성할 수 있습니다:

```bash
vector generate /remap,filter,reduce > vector.toml
```

---

## 참고 자료

- [Vector 공식 문서 - Transforms](https://vector.dev/docs/reference/configuration/transforms/)
- [VRL 레퍼런스](https://vector.dev/docs/reference/vrl/)
- [VRL 함수 레퍼런스](https://vector.dev/docs/reference/vrl/functions/)
- [VRL 예제 레퍼런스](https://vector.dev/docs/reference/vrl/examples/)
- [Vector 설정 가이드](https://vector.dev/docs/reference/configuration/)
- [VRL Playground](https://playground.vrl.dev/)
