# Vector Transforms와 Sinks

## Vector Transforms (변환) 레퍼런스

> 공식 문서: https://vector.dev/docs/reference/configuration/transforms/

### 개요

Vector의 Transforms는 데이터가 파이프라인을 통과하는 동안 데이터를 변형하고 조작할 수 있게 해주는 컴포넌트입니다. Sources에서 수집한 데이터를 Sinks로 전송하기 전에 파싱, 필터링, 라우팅, 집계 등 다양한 처리를 수행할 수 있습니다.

Vector는 YAML, TOML, JSON 설정 형식을 지원합니다. 대부분의 Linux 시스템에서 설정 파일은 `/etc/vector/vector.yaml`에 위치합니다.

---

### 1. Remap Transform (VRL)

> 공식 문서: https://vector.dev/docs/reference/configuration/transforms/remap/

#### 개요

Remap Transform은 Vector Remap Language(VRL)를 사용하여 이벤트 데이터를 수정하는 핵심 Transform입니다. Vector에서 데이터를 파싱, 변형, 조작하는 데 권장됩니다.

VRL은 관측 데이터(로그 및 메트릭)를 안전하고 효율적으로 처리하기 위해 특별히 설계된 표현식 지향 언어입니다. 기본적인 데이터 변형을 위해 여러 Transform을 체인으로 연결할 필요 없이, 단일 remap Transform에서 복잡한 변환을 수행할 수 있습니다.

#### 기본 설정 예제

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

#### 외부 VRL 파일 사용

```yaml
transforms:
  parse_json_log:
    type: remap
    inputs:
      - stdin
    file: "./config/log_parser.vrl"
```

#### 주요 설정 옵션

| 옵션 | 타입 | 설명 |
|------|------|------|
| `inputs` | array | 필수. 업스트림 source 또는 transform ID 목록. 와일드카드(*) 지원 |
| `source` | string | VRL 프로그램 소스 코드 |
| `file` | string | VRL 프로그램 파일 경로 (source 대신 사용 가능) |
| `drop_on_error` | boolean | 에러 발생 시 이벤트 삭제 여부 (기본값: false) |
| `drop_on_abort` | boolean | abort 발생 시 이벤트 삭제 여부 (기본값: true) |
| `reroute_dropped` | boolean | 삭제된 이벤트를 별도 출력으로 라우팅 (기본값: false) |
| `timezone` | string | 타임스탬프 변환에 사용할 시간대 |

#### 에러 처리 및 재라우팅

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

#### VRL 예제

##### JSON 파싱
```yaml
transforms:
  parse_json:
    type: remap
    inputs:
      - raw_logs
    source: |
      . = parse_json!(.message)
```

##### Syslog 파싱
```yaml
transforms:
  parse_syslog:
    type: remap
    inputs:
      - syslog_source
    source: |
      . |= parse_syslog!(.message)
```

##### Apache 로그 파싱
```yaml
transforms:
  parse_apache:
    type: remap
    inputs:
      - apache_logs
    source: |
      . = parse_apache_log!(.message, format: "combined")
```

##### 필드 추가 및 변환
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

#### VRL 테스트

Vector REPL을 사용하여 VRL 프로그램을 테스트할 수 있습니다:

```bash
vector vrl
```

REPL에서 `help`를 입력하면 도움말을 볼 수 있습니다. REPL은 실제 Vector 설정과 거의 동일하게 동작하므로, 프로덕션에 적용하기 전에 복잡한 프로그램의 개별 스니펫을 검증할 수 있습니다.

#### VRL 주요 함수 카테고리

VRL은 다양한 내장 함수를 제공합니다:

- Array 함수: `append`, `chunks`, `push`, `zip` 등
- String 함수: `upcase`, `downcase`, `replace`, `split` 등
- Parse 함수: `parse_json`, `parse_syslog`, `parse_apache_log`, `parse_regex` 등
- Timestamp 함수: `now`, `format_timestamp`, `parse_timestamp`, `from_unix_timestamp` 등
- Path 함수: `del`, `exists`, `get` 등
- Type 함수: `type_of`, `is_string`, `is_integer` 등

> VRL 함수 전체 레퍼런스: https://vector.dev/docs/reference/vrl/functions/

---

### 2. Filter Transform

> 공식 문서: https://vector.dev/docs/reference/configuration/transforms/filter/

#### 개요

Filter Transform은 조건에 따라 이벤트를 필터링합니다. 조건에 일치하는 이벤트만 다운스트림으로 전달되고, 일치하지 않는 이벤트는 삭제됩니다.

#### 기본 설정 예제

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

#### 주요 설정 옵션

| 옵션 | 타입 | 설명 |
|------|------|------|
| `inputs` | array | 필수. 업스트림 source 또는 transform ID 목록 |
| `condition` | string | 필수. 각 이벤트에 적용되는 VRL 조건 표현식 |

#### 실제 사용 예제

##### 심각도 기반 필터링
```yaml
transforms:
  filter_out_info:
    type: filter
    inputs:
      - logs
    condition: '.severity != "info"'
```

##### 복합 조건 필터링
```yaml
transforms:
  filter_important:
    type: filter
    inputs:
      - logs
    condition: '.severity != "info" && .status_code < 400 && exists(.host)'
```

##### 소스 기반 필터링
```toml
[transforms.filter_process]
inputs = ["source0"]
type = "filter"
condition = '''
.source == "PROCESS_SERVICE_CHECK_RESULT"
'''
```

#### Remap 조건 타입 사용

```toml
[transforms.filter_out_non_critical]
type = "filter"
inputs = ["http-server-logs"]

condition.type = "remap"
condition.source = '.status_code != 200 && !includes(["info", "debug"], .severity)'
```

---

### 3. Route Transform

> 공식 문서: https://vector.dev/docs/reference/configuration/transforms/route/

#### 개요

Route Transform은 사용자 정의 조건에 따라 이벤트를 고유한 서브 스트림으로 분기합니다. 하나의 이벤트가 여러 라우트에 동시에 전달될 수 있습니다.

#### 기본 설정 예제

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

#### 주요 설정 옵션

| 옵션 | 타입 | 설명 |
|------|------|------|
| `inputs` | array | 필수. 업스트림 source 또는 transform ID 목록 |
| `route` | map | 필수. 라우트 식별자에서 논리적 조건으로의 맵 |
| `reroute_unmatched` | boolean | 매치되지 않는 이벤트를 `_unmatched` 출력으로 라우팅 (기본값: true) |

#### 라우트를 입력으로 참조

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

#### VRL 조건 사용 예제

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

#### 매치되지 않는 이벤트 처리

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

#### 지원되는 이벤트 타입

- Log 이벤트: 지원
- Trace 이벤트: 지원

---

### 4. Exclusive Route Transform

> 공식 문서: https://vector.dev/docs/reference/configuration/transforms/exclusive_route/

#### 개요

Exclusive Route Transform은 이벤트를 단일 출력으로만 라우팅합니다. 일반 route transform과 달리 first-match-wins 방식으로 동작하며, 라우트는 선언 순서대로 평가됩니다.

#### 기본 설정 예제

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

#### 간단한 조건 구문

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

#### JSON 예제

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

#### 주요 설정 옵션

| 옵션 | 타입 | 설명 |
|------|------|------|
| `inputs` | array | 필수. 업스트림 source 또는 transform ID 목록 |
| `routes` | array | 필수. 순서대로 평가되는 라우트 배열 |
| `routes[].name` | string | 라우트 이름 (고유해야 함, `_unmatched` 예약어) |
| `routes[].condition` | string/object | VRL 조건 표현식 |

#### 라우트 참조

- 각 라우트는 `<transform_name>.<name>` 형식으로 참조 가능
- 어떤 라우트와도 매치되지 않는 이벤트는 `<transform_name>._unmatched`로 전송

#### 지원되는 이벤트 타입

- Log 이벤트: 지원
- Trace 이벤트: 지원

---

### 5. Aggregate Transform

> 공식 문서: https://vector.dev/docs/reference/configuration/transforms/aggregate/

#### 개요

Aggregate Transform은 설정된 시간 간격 동안 메트릭 이벤트를 집계합니다. 볼륨 감소가 주요 장점으로, 메트릭 볼륨 기준으로 요금이 부과되는 환경에서 직접적인 비용 절감 효과를 얻거나, CPU 처리량과 네트워크 대역폭을 줄여 간접적으로 비용을 절감할 수 있습니다.

#### 작동 방식

메트릭은 종류(kind)에 따라 다르게 집계됩니다:
- Incremental 메트릭: 간격 동안 누적됩니다
- Absolute 메트릭: 새 값이 이전 값을 대체합니다

예를 들어, 값이 10과 13인 두 incremental 카운터 메트릭이 한 기간에 처리되면, 값이 23인 단일 incremental 카운터로 집계됩니다.

#### 기본 설정 예제

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

#### 주요 설정 옵션

| 옵션 | 타입 | 설명 |
|------|------|------|
| `inputs` | array | 필수. 업스트림 source 또는 transform ID 목록 |
| `interval_ms` | integer | 플러시 간격 (밀리초). 이 시간 동안 동일한 시리즈 데이터(이름, 네임스페이스, 태그 등)를 가진 메트릭이 집계됨 |
| `mode` | string | 집계에 사용할 함수. 일부 함수는 incremental에서만, 일부는 absolute에서만 작동 |

#### 실제 사용 예제

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

### 6. Dedupe Transform

> 공식 문서: https://vector.dev/docs/reference/configuration/transforms/dedupe/

#### 개요

Dedupe Transform은 파이프라인을 통과하는 로그에서 중복 이벤트를 제거합니다. 데이터 무결성을 유지하고 의도치 않은 로그 중복을 방지하는 데 유용합니다.

#### 작동 방식

이 Transform은 `cache.num_events` 크기의 LRU 캐시를 사용합니다. 최근 처리한 `cache.num_events`개의 이벤트 정보를 메모리에 유지하며, 항목은 삽입 순서대로 캐시에서 제거됩니다.

캐시에 이미 존재하는 이벤트의 중복이 수신되면, 해당 이벤트는 캐시의 최신 위치로 이동하며 제거 대기열에서의 순서가 초기화됩니다.

#### 기본 설정 예제

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

#### 주요 설정 옵션

| 옵션 | 타입 | 설명 |
|------|------|------|
| `inputs` | array | 필수. 업스트림 source 또는 transform ID 목록 |
| `fields.match` | array | 매칭에 사용할 필드 목록 |
| `fields.ignore` | array | 매칭에서 제외할 필드 목록 |
| `cache.num_events` | integer | 캐시에 저장할 최대 이벤트 수 |

#### 필드 매칭 옵션

- `fields.match`: 지정된 필드만 매칭에 고려됩니다
- `fields.ignore`: 지정된 필드를 제외한 모든 필드가 매칭에 포함됩니다

#### 실제 사용 예제

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

#### 중요 사항

- 명시적으로 null 값을 가진 필드는 해당 필드가 완전히 생략된 것과 항상 다른 것으로 간주됩니다
- 예: `fields.match = ["a"]`로 실행할 때, `{a: null, b:5}`와 `{b:5}`는 서로 다른 이벤트로 간주됩니다
- 이 컴포넌트는 상태를 가지므로, 이전 입력(이벤트)에 따라 동작이 변경됩니다

---

### 7. Reduce Transform

> 공식 문서: https://vector.dev/docs/reference/configuration/transforms/reduce/

#### 개요

Reduce Transform은 조건과 병합 전략에 따라 여러 로그 이벤트를 단일 이벤트로 합칩니다. 다수의 소규모 이벤트 스트림을 더 적은 수의 이벤트 스트림으로 변환할 수 있습니다.

#### 기본 설정 예제

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

#### 주요 설정 옵션

| 옵션 | 타입 | 설명 |
|------|------|------|
| `inputs` | array | 필수. 업스트림 source 또는 transform ID 목록 |
| `group_by` | array | 이벤트 그룹화에 사용할 필드 목록 |
| `merge_strategies` | map | 필드별 사용자 정의 병합 전략 |
| `expire_after_ms` | integer | 그룹 플러시 간격 (밀리초) |
| `ends_when` | string | 트랜잭션의 마지막 이벤트를 구분하는 조건 |
| `starts_when` | string | 트랜잭션의 시작 이벤트를 구분하는 조건 |

#### 병합 전략

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

#### 기본 병합 동작

- 문자열 필드: 첫 번째 값 유지, 후속 값 삭제
- 타임스탬프 필드: 첫 번째 값 유지, `[field-name]_end` 필드에 마지막 타임스탬프 추가
- 숫자 필드: 합계

#### 실제 사용 예제

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

#### VRL 조건 사용

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

### 8. Sample Transform

> 공식 문서: https://vector.dev/docs/reference/configuration/transforms/sample/

#### 개요

Sample Transform은 설정된 비율로 이벤트를 샘플링합니다. 전체 이벤트 중 일부만 선택적으로 처리할 때 유용합니다.

#### 기본 설정 예제

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

#### 주요 설정 옵션

| 옵션 | 타입 | 설명 |
|------|------|------|
| `inputs` | array | 필수. 업스트림 source 또는 transform ID 목록 |
| `rate` | integer | 1/N으로 표현되는 샘플링 비율. 예: rate=10이면 10개 중 1개 전달 |
| `ratio` | float | 전달되는 이벤트 비율 (0.0 ~ 1.0). 예: ratio=0.13이면 13% 전달 |
| `key` | string | 이벤트 샘플링 여부를 결정하기 위해 해시되는 필드 이름 |
| `group_by` | string | 별도로 샘플링할 그룹을 결정하는 값. Vector 템플릿 구문 지원 |
| `condition` | string | VRL 조건 표현식 |

#### rate vs ratio

- rate: 1/N으로 표현됨. rate=1500이면 1500개 중 1개 전달
- ratio: 비율로 표현됨. ratio=0.13이면 13% 전달

두 옵션을 동시에 설정하면 에러가 발생합니다.

#### 실제 사용 예제

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

#### 지원되는 이벤트 타입

- Log 이벤트: 지원
- Trace 이벤트: 지원

---

### 9. Throttle Transform

> 공식 문서: https://vector.dev/docs/reference/configuration/transforms/throttle/

#### 개요

Throttle Transform은 파이프라인을 통과하는 로그의 처리 속도를 제한합니다. Generic Cell Rate Algorithm을 사용하여 이벤트 스트림을 조율합니다.

#### 작동 방식

throttle transform은 `key_field`에 따라 이벤트를 버킷으로 분류합니다(`key_field`를 지정하지 않으면 단일 버킷을 사용). 각 버킷은 독립적으로 속도 제한됩니다.

속도 제한기는 "셀"을 소비하여 이벤트 통과 여부를 결정합니다. 각 이벤트는 사용 가능한 셀을 하나씩 소비하며, 셀이 부족하면 해당 이벤트는 속도 제한에 걸립니다.

예를 들어, `window_secs`가 60이고 `threshold`가 10이면 6초마다 셀이 하나씩 보충되며, 최대 10개 이벤트의 버스트를 허용합니다.

#### 기본 설정 예제

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

#### key_field를 사용한 개별 속도 제한

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

#### 주요 설정 옵션

| 옵션 | 타입 | 설명 |
|------|------|------|
| `inputs` | array | 필수. 업스트림 source 또는 transform ID 목록 |
| `threshold` | integer | 필수. 시간 창 내에서 허용되는 최대 이벤트 수 |
| `window_secs` | integer | 필수. 임계값이 적용되는 시간 창 (초) |
| `key_field` | string | 개별적으로 속도 제한할 그룹을 결정하는 값. 템플릿 구문 지원 |
| `exclude` | string | 속도 제한에서 제외할 VRL 조건 |

#### 내부 메트릭 설정

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

### 10. Log to Metric Transform

> 공식 문서: https://vector.dev/docs/reference/configuration/transforms/log_to_metric/

#### 개요

Log to Metric Transform은 로그 이벤트에서 하나 이상의 메트릭 이벤트를 파생합니다.

중요: 이 Transform은 여러 로그를 하나의 메트릭으로 집계하지 않습니다. 로그 이벤트를 세분화된 개별 메트릭으로 변환하며, 이후 엣지에서 별도로 집계할 수 있습니다.

#### 메트릭 타입

| 타입 | 설명 |
|------|------|
| `counter` | 증가 또는 0으로 리셋될 수 있는 단일 값 (감소 불가) |
| `gauge` | 증가하거나 감소할 수 있는 시점 값 |
| `histogram` | 샘플링된 값의 분포를 나타냄 |
| `set` | 고유 값의 집합 |
| `summary` | 샘플링된 값의 분포 (글로벌 히스토그램 및 요약을 지원하는 서비스에서 사용) |

#### 기본 설정 예제

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

#### increment_by_value 사용

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

#### Summary 메트릭 예제

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

#### Nginx 메트릭 예제

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

#### 주요 동작

- 단일 로그 이벤트에서 여러 메트릭 이벤트가 생성되는 경우, 메트릭은 배열이 아닌 개별 이벤트로 출력됩니다
- 다운스트림 컴포넌트는 해당 메트릭이 단일 로그에서 파생되었다는 사실을 알 수 없습니다
- 대상 로그 필드의 값이 null이면 해당 항목은 무시되며 메트릭이 생성되지 않습니다

#### VRL을 사용한 고급 사용법 (v0.35.0+)

`all_metrics` 옵션을 사용하면 VRL로 구조화된 로그 이벤트를 메트릭으로 변환하는 커스텀 코드를 작성할 수 있습니다.

---

### 11. Metric to Log Transform

> 공식 문서: https://vector.dev/docs/reference/configuration/transforms/metric_to_log/

#### 개요

Metric to Log Transform은 메트릭 이벤트를 로그 이벤트로 변환합니다. 로그만 지원하는 다운스트림 컴포넌트로 메트릭을 전달할 때 유용합니다.

#### 기본 설정 예제

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

#### host_tag 사용

```toml
[transforms.my_transform_id]
type = "metric_to_log"
inputs = ["my-source-or-transform-id"]
host_tag = "host"
```

#### 주요 설정 옵션

| 옵션 | 타입 | 설명 |
|------|------|------|
| `inputs` | array | 필수. 업스트림 source 또는 transform ID 목록 |
| `host_tag` | string | 소스 호스트에 사용할 메트릭 태그 이름. 해당 태그 값이 생성된 로그 이벤트의 `host` 필드에 설정됨 |
| `metric_tag_values` | string | 메트릭 태그 값 인코딩 방식. `single`: 마지막 값만 표시, `full`: 모든 태그를 별도 할당으로 표시 |
| `timezone` | string | 타임스탬프 변환에 사용할 시간대 |

#### 변환 예제

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

### 12. Lua Transform

> 공식 문서: https://vector.dev/docs/reference/configuration/transforms/lua/

#### 개요

Lua Transform은 Lua 프로그래밍 언어로 이벤트 데이터를 수정합니다. 내장 Lua 5.4 엔진을 통해 이벤트를 변환합니다.

주의: lua transform은 remap transform보다 약 60% 느리므로, 가능하면 remap transform을 사용하는 것이 좋습니다. lua transform은 remap transform으로 처리하기 어려운 엣지 케이스를 위해 설계되었습니다.

#### Hooks

| Hook | 설명 |
|------|------|
| `init` | Transform 초기화 시 호출 |
| `process` | 각 수신 이벤트에 대해 호출 |
| `shutdown` | Transform 종료 시 호출 |

`process`가 핵심 hook으로, 단일 이벤트를 입력으로 받아 두 번째 인자로 전달되는 `emit` 함수를 통해 하나 이상의 이벤트를 출력할 수 있습니다.

#### Timers

타이머 핸들러는 hook과 마찬가지로 이벤트를 생성할 수 있는 Lua 함수입니다. 다만 미리 정해진 간격으로 주기적으로 호출된다는 점이 다릅니다.

#### 기본 설정 예제

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

#### 주요 설정 옵션

| 옵션 | 타입 | 설명 |
|------|------|------|
| `inputs` | array | 필수. 업스트림 source 또는 transform ID 목록 |
| `version` | string | 필수. Lua transform API 버전 ("2" 권장) |
| `hooks.init` | string | 초기화 hook Lua 코드 |
| `hooks.process` | string | 필수. 이벤트 처리 hook Lua 코드 |
| `hooks.shutdown` | string | 종료 hook Lua 코드 |
| `timers` | array | 주기적으로 호출되는 타이머 핸들러 목록 |
| `search_dirs` | array | Lua require 함수에서 검색할 디렉토리 경로 |

#### 외부 Lua 모듈 사용

```toml
[transforms.my_lua]
type = "lua"
version = "2"
inputs = ["source0"]
source = "require('my_aggregator')"
```

#### Version 2 개선사항

Version 2는 개선된 API, 더 나은 데이터 처리 인터페이스, 향상된 성능을 제공합니다:

- 이벤트를 타입 변환이 적용된 Lua 테이블로 표현
- 전역 상태 유지를 위한 hooks 도입
- 시간 기반 플러시를 위한 타이머 도입 (집계에 유용)
- 로그 이벤트 외에 메트릭 이벤트 처리 지원

---

### 13. Tag Cardinality Limit Transform

> 공식 문서: https://vector.dev/docs/reference/configuration/transforms/tag_cardinality_limit/

#### 개요

Tag Cardinality Limit Transform은 메트릭 이벤트의 태그 카디널리티를 제한하여 카디널리티 폭발을 방지합니다.

Prometheus에서는 카디널리티가 높은 메트릭 이름과 레이블이 성능 및 안정성 문제를 일으킬 수 있어 주의가 필요합니다.

#### 기본 설정 예제

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

#### 주요 설정 옵션

| 옵션 | 타입 | 설명 |
|------|------|------|
| `inputs` | array | 필수. 업스트림 source 또는 transform ID 목록 |
| `mode` | string | 알고리즘 모드. `exact`: 정확한 중복 감지, `probabilistic`: 확률적 감지 (메모리 효율적) |
| `value_limit` | integer | 허용되는 고유 태그 값의 수 |
| `limit_exceeded_action` | string | 제한 초과 시 동작. `drop_tag`: 태그 삭제, `drop_event`: 이벤트 삭제 |
| `cache_size_per_key` | integer | 중복 태그 감지를 위한 캐시 크기 (바이트) |

#### 모드 설명

exact 모드:
- 처리한 모든 메트릭 이벤트의 태그 키 복사본을 메모리에 저장
- 각 키에 대해 `value_limit`개의 고유 값을 확인할 때까지 해당 값들도 메모리에 저장
- 한도를 초과하면 해당 키의 새 값은 거부됨

probabilistic 모드:
- 모든 값을 저장하는 대신 블룸 필터를 사용하여 각 고유 키의 값이 이전에 나타났는지 확률적으로 판단
- 메모리 효율적이지만 오탐(false positive) 가능성이 있음

#### 실제 사용 예제

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

### 14. AWS EC2 Metadata Transform

> 공식 문서: https://vector.dev/docs/reference/configuration/transforms/aws_ec2_metadata/

#### 개요

AWS EC2 Metadata Transform은 AWS EC2 인스턴스 메타데이터를 이벤트에 추가합니다.

#### 기본 설정 예제

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

#### 주요 설정 옵션

| 옵션 | 타입 | 설명 |
|------|------|------|
| `inputs` | array | 필수. 업스트림 source 또는 transform ID 목록 |
| `endpoint` | string | EC2 메타데이터 엔드포인트 (기본값: http://169.254.169.254) |
| `fields` | array | 이벤트에 포함할 메타데이터 필드 목록 |
| `namespace` | string | Transform이 추가하는 모든 이벤트 필드의 접두사 |
| `refresh_interval_secs` | integer | 업데이트된 메타데이터 쿼리 간격 (초) |
| `tags` | array | 이벤트에 포함할 인스턴스 태그 목록 |

#### 기본 필드 목록

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

#### 중요 사항

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

#### 지원되는 이벤트 타입

- Log 이벤트: 지원

---

### 15. Window Transform

> 공식 문서: https://vector.dev/docs/reference/configuration/transforms/window/

#### 개요

Window Transform은 링 버퍼 기반의 슬라이딩 윈도우로 구현된 백트레이스 로깅 컴포넌트입니다. `flush_when` 조건이 일치할 때까지 이벤트를 버퍼에 보관하며, 버퍼가 가득 차면 가장 오래된 이벤트부터 삭제됩니다.

#### 작동 방식

이벤트 스트림이 Transform을 통과할 때, `flush_when` 조건과 일치하는 이벤트를 기준으로 `num_events_before`와 `num_events_after` 범위의 "윈도우"가 구성됩니다. 조건이 일치하면 해당 이벤트와 `num_events_after`가 0보다 큰 경우 이후 이벤트를 포함한 전체 윈도우가 출력으로 플러시됩니다.

백트레이스 로깅 또는 링 버퍼 로깅이라고도 합니다.

#### 기본 설정 예제

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

#### 주요 설정 옵션

| 옵션 | 타입 | 설명 |
|------|------|------|
| `inputs` | array | 필수. 업스트림 source 또는 transform ID 목록 |
| `flush_when` | string | 필수. 윈도우 플러시를 트리거하는 VRL 조건 |
| `num_events_before` | integer | `flush_when` 조건과 일치하는 이벤트 이전에 보관할 최대 이벤트 수 |
| `num_events_after` | integer | `flush_when` 조건과 일치하는 이벤트 이후에 보관할 최대 이벤트 수 |
| `pass_through` | string | 버퍼링 없이 이벤트를 통과시키는 조건 (주의해서 사용) |

#### 버퍼 동작

- 과거 이벤트는 `num_events_before` 크기의 메모리 버퍼에 저장됨
- 버퍼가 가득 차면 새 이벤트를 위해 가장 오래된 이벤트가 제거됨
- 버퍼는 영속적이지 않으므로 시스템 크래시 시 버퍼링된 이벤트가 모두 손실됨
- `flush_when` 조건이 버퍼가 채워지기 전에 일치하면 즉시 플러시됨

#### 실제 사용 예제

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

#### 지원되는 이벤트 타입

- Log 이벤트: 지원

---

### Transform 구성 생성

Vector의 `generate` 명령으로 Transform이 포함된 보일러플레이트 설정을 생성할 수 있습니다:

```bash
vector generate /remap,filter,reduce > vector.toml
```

---

### 참고 자료

- [Vector 공식 문서 - Transforms](https://vector.dev/docs/reference/configuration/transforms/)
- [VRL 레퍼런스](https://vector.dev/docs/reference/vrl/)
- [VRL 함수 레퍼런스](https://vector.dev/docs/reference/vrl/functions/)
- [VRL 예제 레퍼런스](https://vector.dev/docs/reference/vrl/examples/)
- [Vector 설정 가이드](https://vector.dev/docs/reference/configuration/)
- [VRL Playground](https://playground.vrl.dev/)

---

## Vector Sinks (싱크) 가이드

### 개요

Sinks 는 Vector에서 데이터를 외부 서비스나 목적지로 전송하는 컴포넌트입니다. Vector 토폴로지는 세 가지 유형의 컴포넌트로 구성됩니다:
- Sources: 관측 데이터 소스로부터 데이터를 수집하거나 수신
- Transforms: 토폴로지를 통과하는 관측 데이터를 조작하거나 변경
- Sinks: Vector에서 외부 서비스나 목적지로 데이터를 전송

---

### 지원되는 Sinks 전체 목록

Vector는 다양한 싱크를 지원합니다:

| 카테고리 | Sinks |
|---------|-------|
| 메시징/스트리밍 | AMQP, Kafka, MQTT, NATS, Pulsar, Redis |
| 클라우드 스토리지 | AWS S3, Azure Blob Storage, GCP Cloud Storage |
| AWS 서비스 | AWS CloudWatch Logs, AWS CloudWatch Metrics, AWS Kinesis Data Firehose, AWS Kinesis Streams, AWS SNS, AWS SQS |
| GCP 서비스 | GCP Chronicle, GCP Cloud Monitoring, GCP PubSub, GCP Stackdriver |
| Azure 서비스 | Azure Blob Storage, Azure Monitor Logs |
| 관측성 플랫폼 | Datadog (Events, Logs, Metrics, Traces), Elasticsearch, Loki, New Relic, OpenTelemetry, Prometheus, Splunk HEC |
| 로깅 서비스 | Axiom, Honeycomb, Humio, Mezmo (LogDNA), Papertrail, Sematext |
| 데이터베이스 | ClickHouse, Databend, GreptimeDB, InfluxDB, Postgres |
| 네트워크 | HTTP, Socket, WebSocket, WebSocket Server |
| 기타 | Blackhole, Console, File, StatsD, Vector |

---

### 공통 설정 옵션

대부분의 싱크에서 공유하는 공통 설정 패턴입니다.

#### 1. Acknowledgements (승인)

승인 기능은 end-to-end 데이터 전송을 보장하기 위해 사용됩니다.

```toml
[sinks.my_sink]
type = "elasticsearch"
inputs = ["my_source"]

[sinks.my_sink.acknowledgements]
enabled = true
```

설명:
- 싱크에서 승인이 활성화되면, end-to-end 승인을 지원하는 연결된 소스는 해당 싱크가 이벤트를 승인할 때까지 대기합니다
- 싱크 레벨의 승인 설정은 전역 승인 설정보다 우선적으로 적용됩니다

#### 2. Batching (배칭)

이벤트 배칭 동작을 구성합니다.

```toml
[sinks.my_sink]
type = "http"
inputs = ["my_source"]

[sinks.my_sink.batch]
max_bytes = 10485760    # 최대 배치 크기 (바이트)
max_events = 1000       # 최대 이벤트 수
timeout_secs = 1        # 최대 배치 대기 시간 (초)
```

배치 플러시 조건:
- 배치 수명이 `timeout_secs`에 도달하거나 초과
- 배치 크기가 `max_bytes` 또는 `max_events`에 도달하거나 초과

#### 3. Buffering (버퍼링)

싱크의 버퍼링 동작을 구성합니다.

```toml
[sinks.my_sink]
type = "elasticsearch"
inputs = ["my_source"]

[sinks.my_sink.buffer]
type = "memory"           # memory 또는 disk
max_events = 500          # 최대 이벤트 수
when_full = "block"       # block 또는 drop_newest
```

디스크 버퍼 설정:
```toml
[sinks.my_sink.buffer]
type = "disk"
max_size = 268435488      # 최소 ~256MB 필요
when_full = "block"
```

#### 4. Health Checks (헬스 체크)

다운스트림 서비스의 접근성과 데이터 수신 준비 상태를 확인합니다.

```toml
[sinks.my_sink]
type = "http"
inputs = ["my_source"]
healthcheck.enabled = true
```

즉시 종료 옵션:
```bash
vector --config /etc/vector/vector.toml --require-healthy
```

#### 5. Inputs (입력)

업스트림 소스 또는 트랜스폼 ID 목록을 지정합니다.

```toml
[sinks.my_sink]
type = "console"
inputs = ["my_source", "my_transform"]    # 특정 ID 지정
# inputs = ["*"]                          # 와일드카드 지원
```

---

### 주요 Sinks 상세 설정

#### 1. Elasticsearch Sink

Elasticsearch 클러스터로 로그 이벤트를 전송합니다.

상태: Stable | 전송 보장: At-least-once | 승인 지원: Yes

##### 기본 설정

```toml
[sinks.elasticsearch]
type = "elasticsearch"
inputs = ["my_source"]
endpoints = ["http://localhost:9200"]

[sinks.elasticsearch.bulk]
index = "vector-%Y-%m-%d"    # 일별 인덱스
```

##### 인증 설정

```toml
[sinks.elasticsearch]
type = "elasticsearch"
inputs = ["my_source"]
api_version = "v7"
endpoints = ["https://your-endpoint:9200"]

[sinks.elasticsearch.auth]
strategy = "basic"
user = "your_username"
password = "your_password"

[sinks.elasticsearch.tls]
verify_certificate = true

[sinks.elasticsearch.bulk]
index = "vector"

[sinks.elasticsearch.batch]
max_bytes = 10485760
max_events = 5000
timeout_secs = 1
```

##### AWS OpenSearch 설정

```toml
[sinks.elasticsearch]
type = "elasticsearch"
inputs = ["my_source"]
endpoints = ["https://your-opensearch-domain.region.es.amazonaws.com"]

[sinks.elasticsearch.auth]
strategy = "aws"
region = "us-east-1"

[sinks.elasticsearch.bulk]
index = "logs"
```

주요 옵션:
- `api_version`: Elasticsearch API 버전 (v6, v7, v8)
- `compression`: 압축 알고리즘 (none, gzip)
- `doc_type`: 문서 타입 (v7+에서는 `_doc` 권장)

---

#### 2. Kafka Sink

Apache Kafka 토픽으로 이벤트를 발행합니다.

상태: Stable | 전송 보장: At-least-once | 승인 지원: Yes

##### 기본 설정

```toml
[sinks.kafka]
type = "kafka"
inputs = ["my_source"]
bootstrap_servers = "localhost:9092"
topic = "my-topic"

[sinks.kafka.encoding]
codec = "json"
```

##### 고급 설정

```toml
[sinks.kafka]
type = "kafka"
inputs = ["my_source"]
bootstrap_servers = "10.14.22.123:9092,10.14.23.332:9092"
topic = "logs-{{ environment }}"    # 템플릿 지원
key_field = "user_id"               # 파티션 키
compression = "gzip"

[sinks.kafka.encoding]
codec = "json"

# librdkafka 고급 옵션
[sinks.kafka.librdkafka_options]
"message.max.bytes" = "1000000"
"queue.buffering.max.ms" = "5"
```

##### SASL 인증 설정

```toml
[sinks.kafka]
type = "kafka"
inputs = ["my_source"]
bootstrap_servers = "kafka.example.com:9093"
topic = "secure-topic"

[sinks.kafka.sasl]
enabled = true
mechanism = "SCRAM-SHA-256"
username = "user"
password = "password"

[sinks.kafka.tls]
enabled = true
```

주요 옵션:
- `bootstrap_servers`: Kafka 브로커 주소 (쉼표로 구분)
- `topic`: 대상 토픽 (템플릿 구문 지원)
- `key_field`: 파티션 키로 사용할 필드
- `librdkafka_options`: librdkafka 클라이언트 고급 옵션

---

#### 3. HTTP Sink

HTTP 엔드포인트로 이벤트를 전송합니다.

상태: Stable | 전송 보장: At-least-once | 승인 지원: Yes

##### 기본 설정

```toml
[sinks.http]
type = "http"
inputs = ["my_source"]
uri = "http://localhost:8080/logs"

[sinks.http.encoding]
codec = "json"
```

##### 인증 및 헤더 설정

```toml
[sinks.http]
type = "http"
inputs = ["my_source"]
uri = "https://api.example.com/v1/logs"
method = "POST"
compression = "gzip"

[sinks.http.encoding]
codec = "json"

[sinks.http.auth]
strategy = "bearer"
token = "${API_TOKEN}"

[sinks.http.request]
headers.Content-Type = "application/json"
headers.X-Custom-Header = "custom-value"

[sinks.http.batch]
max_bytes = 1048576
timeout_secs = 1

[sinks.http.tls]
verify_certificate = true
```

##### Basic 인증 설정

```toml
[sinks.http]
type = "http"
inputs = ["my_source"]
uri = "https://api.example.com/logs"

[sinks.http.auth]
strategy = "basic"
user = "username"
password = "password"

[sinks.http.encoding]
codec = "json"
```

주요 옵션:
- `uri`: 대상 HTTP 엔드포인트 URI
- `method`: HTTP 메서드 (POST, PUT 등)
- `compression`: 압축 알고리즘 (none, gzip, zstd)
- `auth.strategy`: 인증 전략 (basic, bearer)

Adaptive Request Concurrency:
Vector는 TCP 혼잡 제어 알고리즘에서 영감을 받은 피드백 루프를 사용하여 HTTP 동시성을 자동으로 최적화합니다.

---

#### 4. File Sink

로컬 파일 시스템에 이벤트를 기록합니다.

상태: Stable | 전송 보장: At-least-once | 승인 지원: Yes

##### 기본 설정

```toml
[sinks.file]
type = "file"
inputs = ["my_source"]
path = "/var/log/vector/%Y-%m-%d.log"

[sinks.file.encoding]
codec = "json"
```

##### 고급 설정

```toml
[sinks.file]
type = "file"
inputs = ["my_source"]
path = "/var/log/vector/{{ host }}/%Y-%m-%d.log"
compression = "gzip"
idle_timeout_secs = 30

[sinks.file.encoding]
codec = "json"
timestamp_format = "rfc3339"
```

##### 텍스트 인코딩 설정

```toml
[sinks.file]
type = "file"
inputs = ["my_source"]
path = "/tmp/vector-%Y-%m-%d.log"
timezone = "local"

[sinks.file.encoding]
codec = "text"
```

주요 옵션:
- `path`: 파일 경로 (시간 기반 템플릿 지원: `%Y`, `%m`, `%d` 등)
- `compression`: 압축 (none, gzip, zstd)
- `idle_timeout_secs`: 유휴 파일 닫기 시간
- `timezone`: 경로 템플릿용 시간대

---

#### 5. Console Sink

표준 출력(stdout/stderr)으로 이벤트를 출력합니다. 디버깅에 유용합니다.

상태: Stable | 전송 보장: Best-effort | 승인 지원: No

##### 기본 설정

```toml
[sinks.console]
type = "console"
inputs = ["my_source"]
target = "stdout"

[sinks.console.encoding]
codec = "json"
```

##### 텍스트 출력 설정

```toml
[sinks.console]
type = "console"
inputs = ["my_source"]
target = "stdout"

[sinks.console.encoding]
codec = "text"
```

##### 전체 예제

```toml
# 소스 설정
[sources.stdin]
type = "stdin"

# 콘솔 출력
[sinks.stdout]
type = "console"
inputs = ["stdin"]
target = "stdout"

[sinks.stdout.encoding]
codec = "json"
```

주요 옵션:
- `target`: 출력 대상 (stdout, stderr)
- `encoding.codec`: 인코딩 형식 (json, text)

---

#### 6. Loki Sink

Grafana Loki 로그 집계 시스템으로 로그를 전송합니다.

상태: Stable | 전송 보장: At-least-once | 승인 지원: Yes

##### 기본 설정

```toml
[sinks.loki]
type = "loki"
inputs = ["my_source"]
endpoint = "http://localhost:3100"

[sinks.loki.labels]
source = "vector"

[sinks.loki.encoding]
codec = "json"
```

##### 고급 설정

```toml
[sinks.loki]
type = "loki"
inputs = ["my_source"]
endpoint = "http://loki:3100"
compression = "snappy"
out_of_order_action = "accept"

[sinks.loki.labels]
forwarder = "vector"
host = "{{ host }}"
environment = "production"

[sinks.loki.encoding]
codec = "json"

[sinks.loki.batch]
max_bytes = 1048576
timeout_secs = 1
```

##### 인증 설정 (Grafana Cloud)

```toml
[sinks.loki]
type = "loki"
inputs = ["my_source"]
endpoint = "https://logs-prod-us-central1.grafana.net"
tenant_id = "your-tenant-id"

[sinks.loki.auth]
strategy = "basic"
user = "your-user-id"
password = "${GRAFANA_API_KEY}"

[sinks.loki.labels]
job = "vector"
```

주요 옵션:
- `endpoint`: Loki 서버 엔드포인트
- `labels`: 각 이벤트 배치에 첨부할 레이블 (키-값 쌍)
- `out_of_order_action`: 순서가 어긋난 로그 처리 방식 (accept, drop, rewrite_timestamp)
- `tenant_id`: 멀티 테넌시 환경에서의 테넌트 ID

주의: 레이블 카디널리티가 높으면 Loki에 심각한 성능 문제가 발생할 수 있습니다. 고유 레이블 키와 값의 수는 최소화하는 것이 좋습니다.

---

#### 7. Datadog Sinks

Datadog 플랫폼으로 로그, 메트릭, 이벤트, 트레이스를 전송합니다.

##### 7.1 Datadog Logs Sink

상태: Stable | 전송 보장: At-least-once

```toml
[sinks.datadog_logs]
type = "datadog_logs"
inputs = ["my_source"]
default_api_key = "${DATADOG_API_KEY}"
site = "datadoghq.com"
compression = "gzip"
```

##### 7.2 Datadog Metrics Sink

```toml
[sinks.datadog_metrics]
type = "datadog_metrics"
inputs = ["my_metrics"]
default_api_key = "${DATADOG_API_KEY}"
site = "datadoghq.com"
```

##### 7.3 Datadog Events Sink

```toml
[sinks.datadog_events]
type = "datadog_events"
inputs = ["my_events"]
default_api_key = "${DATADOG_API_KEY}"
site = "datadoghq.com"
```

##### 7.4 Datadog Traces Sink

```toml
[sinks.datadog_traces]
type = "datadog_traces"
inputs = ["my_traces"]
default_api_key = "${DATADOG_API_KEY}"
site = "datadoghq.com"
```

공통 옵션:
- `default_api_key`: Datadog API 키 (환경 변수 `DD_API_KEY`로도 설정 가능)
- `site`: Datadog 사이트 (datadoghq.com, datadoghq.eu 등)
- 특수 필드: 이벤트에 `ddsource`, `ddtags`, `hostname`, `message`, `service`가 있으면 API 규격에 따라 처리됩니다.

---

#### 8. AWS S3 Sink

AWS S3 객체 스토리지에 이벤트를 저장합니다.

상태: Stable | 전송 보장: At-least-once | 승인 지원: Yes

##### 기본 설정

```toml
[sinks.s3]
type = "aws_s3"
inputs = ["my_source"]
bucket = "my-log-bucket"
region = "us-east-1"

[sinks.s3.encoding]
codec = "json"
```

##### 고급 설정

```toml
[sinks.s3]
type = "aws_s3"
inputs = ["my_source"]
bucket = "my-log-archives"
region = "us-east-1"
key_prefix = "logs/date=%Y-%m-%d/"    # Hive 친화적 파티셔닝
compression = "gzip"
content_type = "application/x-ndjson"

[sinks.s3.encoding]
codec = "json"

[sinks.s3.framing]
method = "newline_delimited"

[sinks.s3.batch]
max_bytes = 10485760    # 10MB
timeout_secs = 300

# 서버 측 암호화
server_side_encryption = "aws:kms"
ssekms_key_id = "your-kms-key-id"
```

##### IAM 역할 기반 인증

```toml
[sinks.s3]
type = "aws_s3"
inputs = ["my_source"]
bucket = "my-bucket"
region = "us-east-1"

[sinks.s3.auth]
assume_role = "arn:aws:iam::123456789012:role/VectorS3Role"
```

주요 옵션:
- `bucket`: S3 버킷 이름 (`s3://` 접두사나 끝의 `/`는 포함하지 않음)
- `key_prefix`: 객체 키 접두사 (디렉토리 구조로 사용 시 `/`로 끝나야 함)
- `grant_full_control`: 교차 계정 액세스 시 버킷 소유자의 정규 사용자 ID

---

#### 9. AWS CloudWatch Logs Sink

AWS CloudWatch Logs로 로그를 전송합니다.

상태: Stable | 전송 보장: At-least-once | 승인 지원: Yes

##### 기본 설정

```toml
[sinks.cloudwatch]
type = "aws_cloudwatch_logs"
inputs = ["my_source"]
region = "us-east-1"
group_name = "my-log-group"
stream_name = "{{ host }}"

[sinks.cloudwatch.encoding]
codec = "json"
```

##### 고급 설정

```toml
[sinks.cloudwatch]
type = "aws_cloudwatch_logs"
inputs = ["my_source"]
region = "us-east-1"
group_name = "my-application-logs"
stream_name = "{{ host }}/{{ application }}"
compression = "gzip"
create_missing_group = true
create_missing_stream = true

[sinks.cloudwatch.encoding]
codec = "json"

[sinks.cloudwatch.healthcheck]
enabled = true
```

주요 옵션:
- `group_name`: CloudWatch Logs 그룹 이름 (템플릿 지원)
- `stream_name`: 로그 스트림 이름 (템플릿 지원)
- `create_missing_group`: 그룹이 없으면 생성
- `create_missing_stream`: 스트림이 없으면 생성

---

#### 10. Prometheus Sinks

Prometheus와 통합하기 위한 두 가지 싱크를 제공합니다.

##### 10.1 Prometheus Exporter Sink

Prometheus 스크래핑을 위한 HTTP 엔드포인트를 노출합니다.

상태: Stable | 전송 보장: Best-effort

```toml
[sinks.prometheus_exporter]
type = "prometheus_exporter"
inputs = ["my_metrics"]
address = "0.0.0.0:9598"
flush_period_secs = 60
default_namespace = "vector"
```

메트릭 경로: `/metrics`에서 노출됩니다.

##### 10.2 Prometheus Remote Write Sink

Prometheus Remote Write 프로토콜을 사용하여 메트릭을 전송합니다.

상태: Stable | 전송 보장: At-least-once

```toml
[sinks.prometheus_remote_write]
type = "prometheus_remote_write"
inputs = ["my_metrics"]
endpoint = "https://prometheus.example.com/api/v1/write"
default_namespace = "vector"
```

##### Grafana Cloud 설정

```toml
[sinks.prometheus_remote_write]
type = "prometheus_remote_write"
inputs = ["my_metrics"]
endpoint = "https://prometheus-prod-us-central1.grafana.net/api/prom/push"

[sinks.prometheus_remote_write.auth]
strategy = "basic"
user = "your-user-id"
password = "${GRAFANA_API_KEY}"
```

압축 옵션: Prometheus Remote Write 프로토콜은 공식적으로 Snappy만 지원하지만, Vector는 Gzip과 Zstd도 지원합니다.

주의: 카디널리티가 높은 메트릭 이름과 레이블은 Prometheus 성능에 문제를 일으킬 수 있습니다. `tag_cardinality_limit` 트랜스폼 사용을 고려하세요.

---

#### 11. InfluxDB Sinks

InfluxDB로 로그와 메트릭을 전송합니다.

##### 11.1 InfluxDB Metrics Sink

```toml
[sinks.influxdb_metrics]
type = "influxdb_metrics"
inputs = ["my_metrics"]
endpoint = "https://influxdb.example.com"
namespace = "vector"

# InfluxDB v2.x
org = "my-org"
bucket = "my-bucket"
token = "${INFLUXDB_TOKEN}"
```

##### 11.2 InfluxDB Logs Sink

```toml
[sinks.influxdb_logs]
type = "influxdb_logs"
inputs = ["my_logs"]
endpoint = "https://influxdb.example.com"
namespace = "vector"
tags = ["host", "application"]

# InfluxDB v2.x
org = "my-org"
bucket = "my-bucket"
token = "${INFLUXDB_TOKEN}"
```

##### InfluxDB v1.x 설정

```toml
[sinks.influxdb_metrics]
type = "influxdb_metrics"
inputs = ["my_metrics"]
endpoint = "http://influxdb:8086"
database = "mydb"
consistency = "one"
username = "user"
password = "password"
```

주요 옵션:
- InfluxDB v2.x: `org`, `bucket`, `token` 사용
- InfluxDB v1.x: `database`, `username`, `password`, `consistency` 사용

---

#### 12. Splunk HEC Sinks

Splunk HTTP Event Collector로 로그와 메트릭을 전송합니다.

##### Splunk HEC Logs Sink

상태: Stable | 전송 보장: At-least-once

```toml
[sinks.splunk]
type = "splunk_hec_logs"
inputs = ["my_source"]
endpoint = "https://splunk.example.com:8088"
default_token = "${SPLUNK_HEC_TOKEN}"
index = "main"
sourcetype = "vector"

[sinks.splunk.encoding]
codec = "json"
```

##### 고급 설정

```toml
[sinks.splunk]
type = "splunk_hec_logs"
inputs = ["my_source"]
endpoint = "https://splunk.example.com:8088"
default_token = "${SPLUNK_HEC_TOKEN}"
index = "main"
sourcetype = "application_logs"
host_key = "host"

[sinks.splunk.encoding]
codec = "json"

[sinks.splunk.tls]
verify_certificate = true

[sinks.splunk.acknowledgements]
enabled = false
indexer_acknowledgements_enabled = false

[sinks.splunk.batch]
max_bytes = 1048576
timeout_secs = 1
```

##### Splunk HEC Metrics Sink

```toml
[sinks.splunk_metrics]
type = "splunk_hec_metrics"
inputs = ["my_metrics"]
endpoint = "https://splunk.example.com:8088"
default_token = "${SPLUNK_HEC_TOKEN}"
index = "metrics"
```

인덱서 승인:
Splunk HEC 토큰에 인덱서 승인 기능이 활성화되어 있으면, 싱크가 자동으로 연동되어 데이터 전송 성공 여부를 확인합니다.

---

#### 13. ClickHouse Sink

ClickHouse 데이터베이스로 로그를 전송합니다.

상태: Stable | 전송 보장: At-least-once | 승인 지원: Yes

##### 기본 설정

```toml
[sinks.clickhouse]
type = "clickhouse"
inputs = ["my_source"]
endpoint = "http://clickhouse:8123"
database = "default"
table = "logs"

[sinks.clickhouse.auth]
strategy = "basic"
user = "default"
password = ""
```

##### 고급 설정

```toml
[sinks.clickhouse]
type = "clickhouse"
inputs = ["my_source"]
endpoint = "https://clickhouse.example.com:8443"
database = "logs_db"
table = "application_logs"
skip_unknown_fields = true

[sinks.clickhouse.auth]
strategy = "basic"
user = "vector"
password = "${CLICKHOUSE_PASSWORD}"

[sinks.clickhouse.encoding]
timestamp_format = "unix"

[sinks.clickhouse.buffer]
type = "disk"
max_size = 104900000
when_full = "block"

[sinks.clickhouse.request]
in_flight_limit = 20
```

주요 옵션:
- `database`: 대상 데이터베이스 (템플릿 지원)
- `table`: 대상 테이블
- `skip_unknown_fields`: 알 수 없는 필드 무시

---

#### 14. Redis Sink

Redis로 이벤트를 발행합니다.

상태: Stable | 전송 보장: At-least-once | 승인 지원: Yes

```toml
[sinks.redis]
type = "redis"
inputs = ["my_source"]
endpoint = "redis://localhost:6379/0"
data_type = "channel"
key = "vector-logs"

[sinks.redis.encoding]
codec = "json"
```

##### TLS 연결

```toml
[sinks.redis]
type = "redis"
inputs = ["my_source"]
endpoint = "rediss://redis.example.com:6379/0"    # rediss:// for TLS
key = "logs"

[sinks.redis.encoding]
codec = "json"
```

주요 옵션:
- `endpoint`: URL 형식 `protocol://server:port/db` (redis:// 또는 rediss://)
- `data_type`: Redis 데이터 타입 (list, channel)
- `key`: 발행할 Redis 키 (템플릿 지원)

---

#### 15. NATS Sink

NATS 메시징 시스템으로 이벤트를 발행합니다.

상태: Stable | 전송 보장: Best-effort | 승인 지원: Yes

```toml
[sinks.nats]
type = "nats"
inputs = ["my_source"]
url = "nats://localhost:4222"
subject = "logs.{{ host }}"

[sinks.nats.encoding]
codec = "json"
```

##### 인증 설정

```toml
[sinks.nats]
type = "nats"
inputs = ["my_source"]
url = "nats://nats.example.com:4222"
subject = "logs"

[sinks.nats.auth]
strategy = "credentials"
credentials_file = "/etc/vector/nats.creds"

[sinks.nats.encoding]
codec = "json"
```

주요 옵션:
- `url`: NATS 서버 URL (`nats://server:port`, 기본 포트 4222)
- `subject`: 발행할 NATS 주제 (템플릿 지원)

---

#### 16. Pulsar Sink

Apache Pulsar 토픽으로 이벤트를 발행합니다.

상태: Stable | 전송 보장: At-least-once | 승인 지원: Yes

```toml
[sinks.pulsar]
type = "pulsar"
inputs = ["my_source"]
endpoint = "pulsar://localhost:6650"
topic = "persistent://public/default/logs"

[sinks.pulsar.encoding]
codec = "json"
```

##### 고급 설정

```toml
[sinks.pulsar]
type = "pulsar"
inputs = ["my_source"]
endpoint = "pulsar://pulsar.example.com:6650"
topic = "logs-{{ application }}"
partition_key_field = "user_id"
producer_name = "vector-producer"
compression = "lz4"

[sinks.pulsar.encoding]
codec = "json"

[sinks.pulsar.auth]
name = "token"
token = "${PULSAR_TOKEN}"
```

주요 옵션:
- `endpoint`: Pulsar 브로커 엔드포인트
- `topic`: 대상 토픽 (템플릿 지원)
- `partition_key_field`: 파티션 키로 사용할 필드
- `producer_name`: Pulsar 프로듀서 이름

---

#### 17. GCP Sinks

Google Cloud Platform 서비스로 데이터를 전송합니다.

##### 17.1 GCP PubSub Sink

```toml
[sinks.gcp_pubsub]
type = "gcp_pubsub"
inputs = ["my_source"]
project = "my-gcp-project"
topic = "my-topic"

[sinks.gcp_pubsub.encoding]
codec = "json"
```

##### 17.2 GCP Cloud Storage Sink

```toml
[sinks.gcs]
type = "gcp_cloud_storage"
inputs = ["my_source"]
bucket = "my-log-bucket"
key_prefix = "logs/date=%Y-%m-%d/"
compression = "gzip"

[sinks.gcs.encoding]
codec = "json"
```

##### 고급 GCS 설정

```toml
[sinks.gcs]
type = "gcp_cloud_storage"
inputs = ["my_source"]
bucket = "my-bucket"
acl = "authenticated-read"
storage_class = "STANDARD"
filename_append_uuid = true
filename_time_format = "%s"

[sinks.gcs.encoding]
codec = "json"
```

인증: API 키나 서비스 계정 JSON 파일 경로를 지정하거나, `GOOGLE_APPLICATION_CREDENTIALS` 환경 변수를 사용합니다.

---

#### 18. Azure Sinks

Azure 서비스로 데이터를 전송합니다.

##### 18.1 Azure Blob Storage Sink

```toml
[sinks.azure_blob]
type = "azure_blob"
inputs = ["my_source"]
connection_string = "${AZURE_STORAGE_CONNECTION_STRING}"
container_name = "logs"
blob_prefix = "logs/%Y/%m/%d/"
blob_append_uuid = true

[sinks.azure_blob.encoding]
codec = "json"
```

##### 18.2 Azure Monitor Logs Sink

```toml
[sinks.azure_monitor]
type = "azure_monitor_logs"
inputs = ["my_source"]
customer_id = "${AZURE_CUSTOMER_ID}"
shared_key = "${AZURE_SHARED_KEY}"
log_type = "VectorLogs"
azure_resource_id = "/subscriptions/.../resourceGroups/.../providers/..."
```

---

#### 19. Socket Sink

TCP, UDP 또는 Unix 소켓으로 이벤트를 전송합니다.

상태: Stable | 전송 보장: Best-effort

##### TCP 소켓

```toml
[sinks.socket]
type = "socket"
inputs = ["my_source"]
address = "192.168.1.100:5000"
mode = "tcp"

[sinks.socket.encoding]
codec = "json"
```

##### Unix 소켓

```toml
[sinks.socket]
type = "socket"
inputs = ["my_source"]
path = "/var/run/vector.sock"
mode = "unix"

[sinks.socket.encoding]
codec = "json"
```

##### UDP 소켓

```toml
[sinks.socket]
type = "socket"
inputs = ["my_source"]
address = "192.168.1.100:5000"
mode = "udp"

[sinks.socket.encoding]
codec = "text"
```

주요 옵션:
- `address`: 대상 주소 (IP:포트)
- `mode`: 소켓 모드 (tcp, udp, unix)
- `path`: Unix 소켓 경로 (절대 경로)
- `send_buffer_bytes`: 소켓 송신 버퍼 크기

---

#### 20. WebSocket Sinks

WebSocket을 통해 이벤트를 전송합니다.

##### 20.1 WebSocket Sink (클라이언트)

```toml
[sinks.websocket]
type = "websocket"
inputs = ["my_source"]
uri = "ws://localhost:8080/logs"
ping_interval = 30
ping_timeout = 5

[sinks.websocket.encoding]
codec = "json"
```

##### TLS 연결

```toml
[sinks.websocket]
type = "websocket"
inputs = ["my_source"]
uri = "wss://secure.example.com/logs"

[sinks.websocket.tls]
verify_certificate = true

[sinks.websocket.encoding]
codec = "json"
```

##### 20.2 WebSocket Server Sink

```toml
[sinks.websocket_server]
type = "websocket_server"
inputs = ["my_source"]
address = "0.0.0.0:8080"

[sinks.websocket_server.encoding]
codec = "json"
```

---

#### 21. OpenTelemetry Sink

OTLP 프로토콜을 통해 로그, 메트릭, 트레이스를 전송합니다.

상태: Beta | 전송 보장: At-least-once | 승인 지원: Yes

```toml
[sinks.otel]
type = "opentelemetry"
inputs = ["my_source"]

[sinks.otel.protocol]
type = "http"
uri = "http://localhost:4318/v1/logs"

[sinks.otel.protocol.encoding]
codec = "otlp"
```

입력 형식: `<component_id>.logs`, `<component_id>.metrics`, `<component_id>.traces` 형식으로 입력을 지정합니다.

---

#### 22. New Relic Sink

New Relic으로 로그, 메트릭, 트레이스를 전송합니다.

상태: Stable | 전송 보장: At-least-once

```toml
[sinks.new_relic]
type = "new_relic"
inputs = ["my_source"]
license_key = "${NEW_RELIC_LICENSE_KEY}"
account_id = "your-account-id"
api = "logs"
region = "us"
```

##### 메트릭 전송

```toml
[sinks.new_relic_metrics]
type = "new_relic"
inputs = ["my_metrics"]
license_key = "${NEW_RELIC_LICENSE_KEY}"
account_id = "your-account-id"
api = "metrics"
```

주요 옵션:
- `api`: 전송 타입 (logs, metrics, traces)
- `region`: New Relic 지역 (us, eu)
- `account_id`: New Relic 계정 ID

---

#### 23. Honeycomb Sink

Honeycomb으로 로그를 전송합니다.

상태: Stable | 전송 보장: At-least-once

```toml
[sinks.honeycomb]
type = "honeycomb"
inputs = ["my_source"]
api_key = "${HONEYCOMB_API_KEY}"
dataset = "my-dataset"
```

---

#### 24. Blackhole Sink

이벤트를 삭제합니다. 테스트와 벤치마킹에 유용합니다.

상태: Stable | 전송 보장: At-least-once | 승인 지원: Yes

```toml
[sinks.blackhole]
type = "blackhole"
inputs = ["my_source"]
print_interval_secs = 10    # 활동 요약 출력 간격 (0=비활성화)
rate = 1000                 # 초당 소비 가능한 이벤트 수 (선택)
```

주요 옵션:
- `print_interval_secs`: 활동 요약 보고 간격 (기본값 0, 비활성화)
- `rate`: 초당 소비 가능한 최대 이벤트 수 (기본값 무제한)

---

#### 25. Vector Sink

다른 Vector 인스턴스로 이벤트를 전송합니다. 분산 아키텍처에 유용합니다.

상태: Stable | 전송 보장: At-least-once | 승인 지원: Yes

```toml
[sinks.vector]
type = "vector"
inputs = ["my_source"]
address = "aggregator.example.com:9000"
```

##### TLS 설정

```toml
[sinks.vector]
type = "vector"
inputs = ["my_source"]
address = "aggregator.example.com:9000"

[sinks.vector.tls]
enabled = true
verify_certificate = true
ca_file = "/etc/vector/ca.crt"
```

---

#### 26. AMQP Sink

RabbitMQ 등 AMQP 0.9.1 호환 브로커로 이벤트를 전송합니다.

상태: Beta | 전송 보장: At-least-once | 승인 지원: Yes

```toml
[sinks.amqp]
type = "amqp"
inputs = ["my_source"]
connection_string = "amqp://user:password@rabbitmq:5672/%2f"
exchange = "logs"
routing_key = "{{ application }}"

[sinks.amqp.encoding]
codec = "json"
```

---

#### 27. MQTT Sink

MQTT 브로커로 이벤트를 전송합니다.

상태: Beta | 전송 보장: Best-effort | 승인 지원: Yes

```toml
[sinks.mqtt]
type = "mqtt"
inputs = ["my_source"]
host = "mqtt.example.com"
port = 1883
topic = "logs/{{ host }}"

[sinks.mqtt.encoding]
codec = "json"
```

##### TLS 및 인증

```toml
[sinks.mqtt]
type = "mqtt"
inputs = ["my_source"]
host = "mqtt.example.com"
port = 8883
topic = "logs"
user = "vector"
password = "${MQTT_PASSWORD}"

[sinks.mqtt.tls]
enabled = true

[sinks.mqtt.encoding]
codec = "json"
```

---

#### 28. StatsD Sink

StatsD 서버로 메트릭을 전송합니다.

상태: Stable | 전송 보장: Best-effort

```toml
[sinks.statsd]
type = "statsd"
inputs = ["my_metrics"]
address = "statsd.example.com:8125"
mode = "udp"
default_namespace = "vector"
```

##### TCP 모드

```toml
[sinks.statsd]
type = "statsd"
inputs = ["my_metrics"]
address = "statsd.example.com:8125"
mode = "tcp"
```

---

#### 29. Papertrail Sink

Papertrail로 로그를 전송합니다.

상태: Stable | 전송 보장: Best-effort

```toml
[sinks.papertrail]
type = "papertrail"
inputs = ["my_source"]
endpoint = "logs.papertrailapp.com:12345"
process = "{{ application }}"
```

설정: Papertrail에서 Log Destination을 생성하고 TCP를 활성화한 뒤 엔드포인트를 지정합니다.

---

#### 30. Sematext Sinks

Sematext로 로그와 메트릭을 전송합니다.

##### Sematext Logs Sink

```toml
[sinks.sematext_logs]
type = "sematext_logs"
inputs = ["my_source"]
token = "${SEMATEXT_TOKEN}"
region = "us"
```

##### Sematext Metrics Sink

```toml
[sinks.sematext_metrics]
type = "sematext_metrics"
inputs = ["my_metrics"]
token = "${SEMATEXT_TOKEN}"
region = "us"
default_namespace = "vector"
```

참고: Sematext 모니터링은 단일 값을 가진 메트릭만 허용하므로 counter와 gauge 메트릭만 지원됩니다.

---

### 설정 형식

Vector는 세 가지 설정 형식을 지원합니다:

#### TOML (권장)
```toml
[sinks.my_sink]
type = "console"
inputs = ["my_source"]

[sinks.my_sink.encoding]
codec = "json"
```

#### YAML
```yaml
sinks:
  my_sink:
    type: console
    inputs:
      - my_source
    encoding:
      codec: json
```

#### JSON
```json
{
  "sinks": {
    "my_sink": {
      "type": "console",
      "inputs": ["my_source"],
      "encoding": {
        "codec": "json"
      }
    }
  }
}
```

---

### 실전 예제

#### 멀티 싱크 파이프라인

여러 싱크로 동시에 데이터를 전송하는 예제:

```toml
# 소스
[sources.app_logs]
type = "file"
include = ["/var/log/app/*.log"]

# 트랜스폼
[transforms.parse_json]
type = "remap"
inputs = ["app_logs"]
source = '''
. = parse_json!(.message)
'''

# 로컬 파일 백업
[sinks.local_backup]
type = "file"
inputs = ["parse_json"]
path = "/var/log/vector/backup/%Y-%m-%d.log"
compression = "gzip"

[sinks.local_backup.encoding]
codec = "json"

# Elasticsearch로 전송
[sinks.elasticsearch]
type = "elasticsearch"
inputs = ["parse_json"]
endpoints = ["http://elasticsearch:9200"]

[sinks.elasticsearch.bulk]
index = "app-logs-%Y-%m-%d"

# Loki로 전송
[sinks.loki]
type = "loki"
inputs = ["parse_json"]
endpoint = "http://loki:3100"

[sinks.loki.labels]
application = "{{ application }}"

[sinks.loki.encoding]
codec = "json"
```

#### 조건부 라우팅

```toml
# 소스
[sources.all_logs]
type = "file"
include = ["/var/log/*.log"]

# 에러 로그 필터링
[transforms.error_filter]
type = "filter"
inputs = ["all_logs"]
condition = 'contains(string!(.message), "ERROR")'

# 에러만 PagerDuty로 전송
[sinks.pagerduty]
type = "http"
inputs = ["error_filter"]
uri = "https://events.pagerduty.com/v2/enqueue"

[sinks.pagerduty.encoding]
codec = "json"

# 모든 로그는 S3로 아카이브
[sinks.s3_archive]
type = "aws_s3"
inputs = ["all_logs"]
bucket = "log-archives"
key_prefix = "logs/%Y/%m/%d/"

[sinks.s3_archive.encoding]
codec = "json"
```

---

### 참고 자료

- [Vector 공식 문서 - Sinks](https://vector.dev/docs/reference/configuration/sinks/)
- [Vector 컴포넌트 목록](https://vector.dev/components/)
- [Vector 설정 가이드](https://vector.dev/docs/reference/configuration/)
- [Vector GitHub 저장소](https://github.com/vectordotdev/vector)
