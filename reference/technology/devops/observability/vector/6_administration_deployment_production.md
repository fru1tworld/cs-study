# Vector 관리, 배포, 프로덕션 전환

## Vector 관리 (Administration)

Vector는 다양한 운영 체제와 플랫폼에서 쉽게 설치하고 운영할 수 있도록 설계됨.

### 목차

1. [관리 (Management)](#1-관리-management)
2. [모니터링 (Monitoring)](#2-모니터링-monitoring)
3. [검증 (Validating)](#3-검증-validating)
4. [최적화 (Optimization)](#4-최적화-optimization)
5. [GraphQL API](#5-graphql-api)
6. [단위 테스트 (Unit Testing)](#6-단위-테스트-unit-testing)
7. [업그레이드 (Upgrading)](#7-업그레이드-upgrading)

---

### 1. 관리 (Management)

다양한 환경에서 Vector 인스턴스를 시작·중지·재시작·리로드하는 방법을 설명함.

#### 1.1 Linux (systemctl)

APT·dpkg·RPM·YUM·pacman으로 Vector를 설치한 경우 `systemctl`로 관리 가능.

##### 시작

```bash
sudo systemctl start vector
```

##### 중지

```bash
sudo systemctl stop vector
```

##### 재시작

```bash
sudo systemctl restart vector
```

##### 리로드 (설정 재적용)

```bash
systemctl kill -s HUP --kill-who=main vector.service
```

##### 상태 확인

```bash
sudo systemctl status vector
```

##### 부팅 시 자동 시작 활성화

```bash
sudo systemctl enable vector
```

##### 부팅 시 자동 시작 비활성화

```bash
sudo systemctl disable vector
```

#### 1.2 macOS (Homebrew)

Homebrew로 Vector를 설치한 경우 Homebrew의 서비스 유틸리티로 관리 가능.

##### 시작

```bash
brew services start vector
```

##### 중지

```bash
brew services stop vector
```

##### 재시작

```bash
brew services restart vector
```

#### 1.3 Windows

MSI로 Windows에 Vector를 설치한 경우 다음 명령 사용 가능.

##### 시작

```powershell
C:\Program Files\Vector\bin\vector --config C:\Program Files\Vector\config\vector.yaml
```

TOML·JSON·YAML 형식의 설정 파일 사용 가능.

#### 1.4 Docker

Docker로 Vector를 실행하는 경우 명령 인터페이스는 모든 플랫폼에서 동일.

##### 시작

```bash
docker run \
  -d \
  -v ~/vector.yaml:/etc/vector/vector.yaml:ro \
  -p 8686:8686 \
  timberio/vector:0.52.0-alpine
```

##### 중지

```bash
docker stop <container-id>
```

##### 다시 시작

```bash
docker restart <container-id>
```

#### 1.5 Vector 실행 파일 직접 사용

프로세스 관리자 없이 Vector 실행 파일을 직접 관리.

##### 시작

```bash
vector --config /etc/vector/vector.yaml
```

##### 리로드

```bash
killall -s SIGHUP vector
```

#### 1.6 설정 자동 리로드

`--watch-config`(또는 `-w`) 플래그 지정 → 설정 파일 변경 시 Vector가 자동으로 리로드.

```bash
vector --config /etc/vector/vector.yaml --watch-config
```

##### 설정 감시 방법 지정

`--watch-config-method` 옵션으로 설정 변경 감시 방법 지정 가능:

- `recommended` (기본값): 파일 이벤트 리스너를 사용하여 파일 변경 이벤트 감지
- `poll`: 파일 이벤트 리스너가 작동하지 않는 환경(예: NFS/EFS로 연결된 설정 파일)에서 사용

```bash
vector --config /etc/vector/vector.yaml --watch-config --watch-config-method poll
```

##### 폴링 간격 설정

`poll` 방법 사용 시 폴링 간격 설정 가능(기본값: 30초):

```bash
vector --config /etc/vector/vector.yaml --watch-config --watch-config-method poll --watch-config-poll-interval-seconds 60
```

#### 1.7 시그널 처리 및 정상 종료

Vector는 시그널 수신 시 정상적으로 종료됨.

##### 정상 종료 타임아웃

SIGINT·SIGTERM 수신 후 정상 종료를 기다리는 최대 시간(초) → 시간 초과 시 Vector 강제 종료.

```bash
vector --config /etc/vector/vector.yaml --graceful-shutdown-limit-secs 120
```

##### 강제 종료 비활성화

강제 종료를 비활성화하려면:

```bash
vector --config /etc/vector/vector.yaml --no-graceful-shutdown-limit
```

기본 타임아웃은 60초.

#### 1.8 데이터 디렉토리 (data_dir)

`data_dir`은 Vector 상태 데이터(디스크 버퍼·파일 체크포인트 등)를 저장하는 디렉토리.

##### 설정 예시 (YAML)

```yaml
data_dir: "/var/lib/vector"

sources:
  my_source:
    type: file
    include:
      - /var/log/*.log

sinks:
  my_sink:
    type: console
    inputs:
      - my_source
    encoding:
      codec: text
```

##### 설정 예시 (TOML)

```toml
data_dir = "/var/lib/vector"

[sources.my_source]
type = "file"
include = ["/var/log/*.log"]

[sinks.my_sink]
type = "console"
inputs = ["my_source"]
encoding.codec = "text"
```

주의사항:
- Vector는 이 디렉토리에 대한 쓰기 권한 필요
- 권한 오류 방지 → Vector를 실행하는 사용자·그룹으로 디렉토리 소유자 변경 필요
- 디스크 버퍼의 최소 크기는 약 256MiB

#### 1.9 환경 변수

##### VECTOR_CONFIG

설정 파일 경로 지정:

```bash
export VECTOR_CONFIG=/etc/vector/vector.yaml
vector
```

##### VECTOR_CONFIG_DIR

설정 디렉토리 지정:

```bash
export VECTOR_CONFIG_DIR=/etc/vector/config.d/
vector
```

##### VECTOR_LOG

로그 레벨 설정:

```bash
export VECTOR_LOG=debug
vector --config /etc/vector/vector.yaml
```

##### 설정 파일 내 환경 변수 보간

설정 파일 내에서 환경 변수 사용 가능:

```yaml
sources:
  my_source:
    type: http_server
    address: "${HOST:-0.0.0.0}:${PORT:-8080}"
```

기본값 지정 시 `:-` 구문 사용.

##### 엄격한 환경 변수 모드

정의되지 않은 환경 변수가 있을 때 오류로 처리하려면:

```bash
vector --config /etc/vector/vector.yaml --strict-env-vars
```

또는:

```bash
export VECTOR_STRICT_ENV_VARS=true
vector --config /etc/vector/vector.yaml
```

---

### 2. 모니터링 (Monitoring)

Vector는 관찰 가능성 데이터를 처리하는 도구이면서 자체적으로도 높은 관찰 가능성 제공 → `internal_logs`와 `internal_metrics` 두 소스로 Vector가 생성하는 로그·메트릭을 일반 소스 데이터처럼 처리 가능.

#### 2.1 내부 로그 (Internal Logs)

##### 기본 설정 (YAML)

```yaml
sources:
  vector_logs:
    type: internal_logs

sinks:
  console:
    type: console
    inputs:
      - vector_logs
    encoding:
      codec: text
```

##### 기본 설정 (TOML)

```toml
[sources.vector_logs]
type = "internal_logs"

[sinks.console]
type = "console"
inputs = ["vector_logs"]
encoding.codec = "text"
```

##### 로그 레벨 설정

`internal_logs` 소스로 전달되는 로그는 로그 레벨에 따라 결정됨. 기본값은 `info`.

명령줄 플래그로 설정:

```bash
# 디버그 레벨
vector --config /etc/vector/vector.yaml -v

# 트레이스 레벨 (가장 상세)
vector --config /etc/vector/vector.yaml -vv
```

환경 변수로 설정:

```bash
export VECTOR_LOG=debug
vector --config /etc/vector/vector.yaml
```

사용 가능한 로그 레벨:
- `error`: 오류만 표시
- `warn`: 경고 이상 표시
- `info`: 정보 이상 표시 (기본값)
- `debug`: 디버그 이상 표시
- `trace`: 모든 로그 표시

##### Splunk로 내부 로그 전송 (TOML)

```toml
[sources.vector_logs]
type = "internal_logs"

[sinks.splunk]
type = "splunk_hec"
inputs = ["vector_logs"]
endpoint = "https://my-account.splunkcloud.com"
token = "${SPLUNK_HEC_TOKEN}"
encoding.codec = "json"
```

#### 2.2 내부 메트릭 (Internal Metrics)

##### 기본 설정 (YAML)

```yaml
sources:
  vector_metrics:
    type: internal_metrics
    namespace: vector
    scrape_interval_secs: 15

sinks:
  prometheus:
    type: prometheus_exporter
    inputs:
      - vector_metrics
    address: "0.0.0.0:9090"
```

##### 기본 설정 (TOML)

```toml
[sources.vector_metrics]
type = "internal_metrics"
namespace = "vector"
scrape_interval_secs = 15

[sinks.prometheus]
type = "prometheus_exporter"
inputs = ["vector_metrics"]
address = "0.0.0.0:9090"
```

##### Prometheus Remote Write로 메트릭 전송

```yaml
sources:
  vector_metrics:
    type: internal_metrics

sinks:
  prometheus:
    type: prometheus_remote_write
    endpoint: "https://prometheus.example.com:9090/api/v1/write"
    inputs:
      - vector_metrics
```

##### 태그 설정

여러 Vector 인스턴스의 내부 메트릭을 같은 목적지로 보낼 때는 메트릭 시리즈 충돌 방지를 위해 고유한 태그 추가 필요:

```yaml
sources:
  vector_metrics:
    type: internal_metrics
    tags:
      host_key: hostname
      pid_key: pid

transforms:
  add_instance_tag:
    type: remap
    inputs:
      - vector_metrics
    source: |
      .tags.instance = "${HOSTNAME}"

sinks:
  prometheus:
    type: prometheus_exporter
    inputs:
      - add_instance_tag
    address: "0.0.0.0:9090"
```

#### 2.3 통합 모니터링 설정 예시

```toml
# 내부 로그 수집
[sources.vector_logs]
type = "internal_logs"

# 내부 메트릭 수집
[sources.vector_metrics]
type = "internal_metrics"

# Splunk로 로그 전송
[sinks.splunk]
type = "splunk_hec"
inputs = ["vector_logs"]
endpoint = "https://my-account.splunkcloud.com"
token = "${SPLUNK_HEC_TOKEN}"
encoding.codec = "json"

# Prometheus로 메트릭 노출
[sinks.prometheus]
type = "prometheus_exporter"
inputs = ["vector_metrics"]
address = "0.0.0.0:9090"
```

#### 2.4 이벤트 기반 관찰 가능성 전략

Vector는 일관되고 상관관계가 있는 텔레메트리 데이터를 보장하는 이벤트 기반 관찰 가능성 전략을 사용함.

핫 패스 로그 속도 제한:
Vector는 핫 패스의 로그 이벤트에 속도 제한을 적용 → I/O 포화나 서비스 장애 위험 없이 세밀한 인사이트 확보 가능.

VRL에서의 로깅:
VRL의 `log` 함수는 기본적으로 속도 제한을 구현 → VRL 프로그램이 `log`를 호출할 때 실수로 I/O를 포화시키는 상황 방지.

---

### 3. 검증 (Validating)

Vector는 설정 유효성을 검사하는 `validate` 서브커맨드를 제공함. 검증 성공 시 종료 코드 0, 실패 시 종료 코드 78로 종료.

#### 3.1 기본 검증

```bash
vector validate /etc/vector/vector.yaml
```

#### 3.2 검증 수행 내용

`validate` 서브커맨드는 지정된 설정에 대해 다음 검사를 수행:

1. 구문 검사: 설정 파일의 구문이 올바른지 확인
2. 스키마 검사: 설정이 예상된 스키마와 일치하는지 확인
3. 토폴로지 검사: 컴포넌트 간의 연결이 유효한지 확인
4. 환경 검사: Vector가 설정된 토폴로지를 지원할 수 있는 환경에서 실행 중인지 확인
   - 모든 컴포넌트가 실행에 필요한 사전 조건을 갖추었는지 확인(예: 데이터 디렉토리 존재 및 쓰기 가능 여부)
   - 모든 싱크가 지정된 대상에 연결 가능한지 확인

#### 3.3 환경 검사 비활성화

환경 검사를 비활성화하려면 `--no-environment` 플래그 사용:

```bash
vector validate --no-environment /etc/vector/vector.yaml
```

#### 3.4 헬스체크 건너뛰기

헬스체크 엔드포인트에 접근할 수 없는 환경(예: 로컬 워크스테이션)에서 다른 환경 검사는 유지하면서 헬스체크만 건너뛰려면 `--skip-healthchecks` 플래그 사용:

```bash
vector validate --skip-healthchecks /etc/vector/vector.yaml
```

참고: 설정된 `data_dir`은 여전히 쓰기 가능해야 함.

#### 3.5 여러 설정 파일 검증

```bash
vector validate /etc/vector/*.yaml
```

#### 3.6 설정 디렉토리 검증

```bash
vector validate --config-dir /etc/vector/config.d/
```

#### 3.7 헬스체크 설정

특정 싱크에 대해 헬스체크를 비활성화하려면 설정에서 `healthcheck` 옵션을 `false`로 설정:

```yaml
sinks:
  my_sink:
    type: http
    uri: "https://example.com"
    healthcheck:
      enabled: false
```

---

### 4. 최적화 (Optimization)

Vector는 Rust로 작성되어 런타임이나 가상 머신을 포함하지 않음. 별도의 조정 없이도 기본적으로 모든 시스템 리소스를 최대한 활용.

#### 4.1 Profile-Guided Optimization (PGO)

PGO(Profile-Guided Optimization)는 런타임 프로파일을 기반으로 프로그램을 최적화하는 컴파일러 기법.

##### 성능 향상

테스트 결과, 일부 Vector 워크로드에서 초당 처리 로그 이벤트가 최대 15% 향상 가능.

##### PGO 빌드 방법

1. `cargo-pgo` 도구 설치:

```bash
cargo install cargo-pgo
```

2. PGO 계측 빌드:

```bash
cargo pgo build
```

3. 프로파일 수집을 위해 계측된 Vector 실행:

```bash
./target/x86_64-unknown-linux-gnu/release/vector --config your_config.yaml
# 대표적인 워크로드 실행
```

4. PGO 최적화 빌드:

```bash
cargo pgo optimize
```

##### 전체 빌드 프로세스

```bash
# 일반 릴리스 빌드
cargo build --release

# PGO 계측 빌드
cargo pgo build

# PGO 최적화 빌드
cargo pgo optimize build
```

더 자세한 가이드는 [Rust 문서](https://doc.rust-lang.org/rustc/profile-guided-optimization.html) 참고.

#### 4.2 버퍼링 모델

Vector는 백프레셔와 데이터 손실을 관리하는 버퍼링 모델을 사용함.

##### 메모리 버퍼

```yaml
sinks:
  my_sink:
    type: console
    inputs:
      - my_source
    buffer:
      type: memory
      max_events: 500
      when_full: block
```

##### 디스크 버퍼

```yaml
sinks:
  my_sink:
    type: console
    inputs:
      - my_source
    buffer:
      type: disk
      max_size: 268435488  # 약 256MB
      when_full: block
```

##### 버퍼 옵션

- `when_full: block` - 버퍼가 가득 차면 입력을 차단
- `when_full: drop_newest` - 버퍼가 가득 차면 최신 이벤트를 삭제

##### 디스크 버퍼 TOML 예시

```toml
data_dir = "/var/lib/vector"

[sources.in]
type = "stdin"

[sinks.out]
type = "console"
inputs = ["in"]
encoding.codec = "text"

[sinks.out.buffer]
type = "disk"
max_size = 1400000
when_full = "drop_newest"
```

#### 4.3 동시성 모델

Vector는 적응형 동시성(Adaptive Concurrency)을 사용 → 다운스트림 서비스 상태에 따라 동시성을 자동으로 조정.

---

### 5. GraphQL API

Vector는 GraphQL API로 자체 관찰 가능성을 제공함. 이 API는 v0.11.0에 도입됨.

#### 5.1 API 활성화

설정 파일에서 API 활성화:

```yaml
api:
  enabled: true
  address: "0.0.0.0:8686"
  playground: true
```

```toml
[api]
enabled = true
address = "0.0.0.0:8686"
playground = true
```

#### 5.2 GraphQL Playground

API 활성화 시 브라우저에서 GraphQL Playground 접속 가능:

```
http://localhost:8686/playground
```

#### 5.3 API 엔드포인트

- GraphQL: `http://localhost:8686/graphql`
- Health: `http://localhost:8686/health`
- Playground: `http://localhost:8686/playground`

#### 5.4 API 쿼리 예시

```bash
curl -X POST http://127.0.0.1:8686/graphql \
  -H "Content-Type: application/json" \
  -d '{"query": "{ health }"}'
```

##### 토폴로지 쿼리

```graphql
{
  sources {
    edges {
      node {
        componentId
        componentType
      }
    }
  }
  transforms {
    edges {
      node {
        componentId
        componentType
      }
    }
  }
  sinks {
    edges {
      node {
        componentId
        componentType
      }
    }
  }
}
```

#### 5.5 Vector CLI 도구

Vector의 GraphQL API는 다음 CLI 도구에서 사용됨:

##### vector top

컴포넌트 메트릭과 토폴로지 정보를 htop과 유사한 대시보드 스타일로 표시:

```bash
vector top
```

원격 인스턴스에 연결:

```bash
vector top --url http://remote-vector:8686/graphql
```

##### vector tap

파이프라인 컴포넌트를 흐르는 이벤트를 실시간으로 관찰:

```bash
# 모든 컴포넌트의 이벤트 관찰
vector tap

# 특정 컴포넌트의 출력 관찰
vector tap --outputs-of my_transform

# YAML 형식으로 출력
vector tap --format yaml

# 원격 인스턴스의 이벤트 관찰
vector tap --url http://remote-vector:8686/graphql
```

##### vector graph

설정에 정의된 토폴로지를 DOT 형식의 그래프로 출력:

```bash
vector graph --config /etc/vector/vector.yaml > topology.dot
```

GraphViz로 시각화:

```bash
vector graph --config /etc/vector/vector.yaml | dot -Tpng > topology.png
```

또는 [webgraphviz.com](http://www.webgraphviz.com/)에서 온라인으로 시각화 가능.

#### 5.6 GraphQL을 선택한 이유

- 타입 안전성: 클라이언트가 스키마를 내부 검사하여 쿼리 실행 전에 유효성을 확인할 수 있음
- 관계형 데이터 모델링: Vector 토폴로지와 같은 관계형 데이터를 자연스럽게 모델링하고 쿼리할 수 있음
- 실시간 스트리밍: WebSocket 구독을 통해 고빈도 메트릭에 이상적인 데이터 스트리밍 가능

---

### 6. 단위 테스트 (Unit Testing)

Vector는 처리 파이프라인의 변환(transform)을 단위 테스트 가능.

#### 6.1 기본 개념

- 변환에 입력 세트 제공
- VRL 어설션으로 출력이 예상과 일치하는지 확인
- `insert_at` 지시문은 테스트 입력을 대상 변환에 인위적으로 삽입

#### 6.2 테스트 실행

```bash
vector test /etc/vector/vector.yaml
```

#### 6.3 YAML 테스트 예시

```yaml
# vector.yaml
transforms:
  parse_log:
    type: remap
    inputs:
      - my_source
    source: |
      . = parse_json!(.message)
      .timestamp = now()
      .environment = "production"

tests:
  - name: "Test parse_log transform"
    inputs:
      - insert_at: parse_log
        type: log
        log_fields:
          message: '{"user": "john", "action": "login"}'
    outputs:
      - extract_from: parse_log
        conditions:
          - type: vrl
            source: |
              assert_eq!(.user, "john")
              assert_eq!(.action, "login")
              assert_eq!(.environment, "production")
              assert!(exists(.timestamp))
```

#### 6.4 TOML 테스트 예시

```toml
[transforms.parse_log]
type = "remap"
inputs = ["my_source"]
source = '''
  . = parse_json!(.message)
  .timestamp = now()
  .environment = "production"
'''

[[tests]]
name = "Test parse_log transform"

[[tests.inputs]]
insert_at = "parse_log"
type = "log"

[tests.inputs.log_fields]
message = '{"user": "john", "action": "login"}'

[[tests.outputs]]
extract_from = "parse_log"

[[tests.outputs.conditions]]
type = "vrl"
source = '''
  assert_eq!(.user, "john")
  assert_eq!(.action, "login")
  assert_eq!(.environment, "production")
  assert!(exists(.timestamp))
'''
```

#### 6.5 VRL 어설션 함수

##### assert

Boolean 표현식을 첫 번째 인수로 받음. 표현식이 false이면 테스트 실패:

```vrl
assert!(1 == 1)
assert!(.field == "expected_value")
```

##### assert_eq

두 값을 인수로 받아 동일한지 비교:

```vrl
assert_eq!(.message, "Hello, World!")
assert_eq!(.count, 42)
```

권장사항: 단위 테스트에서 `assert`와 `assert_eq`는 bang(`!`) 구문으로 인폴리블(infallible)하게 작성 권장. 조건 실패 시 VRL 프로그램이 즉시 중단됨.

#### 6.6 출력이 없는 테스트

필터 변환처럼 특정 조건에서 이벤트가 출력되지 않아야 할 경우:

```yaml
tests:
  - name: "Test filter drops empty messages"
    inputs:
      - insert_at: filter_empty
        type: log
        log_fields:
          message: ""
    no_outputs_from:
      - filter_empty
```

#### 6.7 Raw 입력 테스트

```toml
[[tests]]
name = "check_simple_log"

[[tests.inputs]]
insert_at = "parse_log"
type = "raw"
value = "2019-11-28T12:00:00+00:00 info Sorry, I'm busy this week Cecil"

[[tests.outputs]]
extract_from = "parse_log"

[[tests.outputs.conditions]]
type = "vrl"
source = '''
  assert_eq!(.message, "Sorry, I'm busy this week Cecil")
'''
```

#### 6.8 모범 사례

1. 개별 파일로 분리: 대규모 컴포넌트 체인이 있는 경우 각각을 개별 파일로 분리하고 자체 단위 테스트를 포함
2. 컴포넌트 재사용: 여러 설정 파일에서 사용되는 컴포넌트는 전용 폴더에 정의
3. 변환 집중 테스트: 단위 테스트는 입력을 받아 출력을 반환하는 격리된 함수인 변환에 특히 적합

---

### 7. 업그레이드 (Upgrading)

#### 7.1 일반 업그레이드 권장사항

Vector가 1.0 미만인 동안에는 마이너 버전마다 호환되지 않는 변경 사항이 포함될 수 있음 → 마이너 버전 순차 업그레이드 권장. 구체적인 변경 사항은 각 업그레이드 가이드에 명시됨.

#### 7.2 최신 업그레이드 가이드 하이라이트

##### 버전 0.34 주요 변경사항

1. 기본 설정 파일 위치 변경
   - 기존: `/etc/vector/vector.toml`
   - 변경: `/etc/vector/vector.yaml`
   - YAML이 Vector의 선호 설정 언어로 변경됨 (TOML과 JSON도 계속 지원)

2. Datadog 싱크 설정 변경
   - `region` 옵션이 제거됨
   - 대신 `site` 옵션을 사용해야 함

3. 패키지 저장소 변경
   - 기존: `repositories.timber.io`
   - 변경: `apt.vector.dev` 및 `yum.vector.dev`

##### 버전 0.33 주요 변경사항

- OpenSSL 버전이 v3.1.x로 업그레이드됨
- 레거시 OpenSSL 프로바이더가 기본적으로 비활성화됨

##### 버전 0.27 주요 변경사항

1. 디스크 버퍼 백업 권장
   - 이전 버전으로 롤백하려면 디스크 버퍼를 미리 백업해야 함
   - 새 디스크 버퍼 항목은 이전 Vector 버전에서 읽히지 않을 수 있음

2. Vector 간 통신 업그레이드 순서
   - `vector` 소스/싱크를 사용한 Vector 간 통신 시
   - 컨슈머(수신자)를 먼저 업그레이드한 후 프로듀서(송신자)를 업그레이드

##### 버전 0.22 주요 변경사항

- 새 디스크 버퍼 구현(`disk_v2`)이 기본값(`disk`)으로 승격됨
- 기존 디스크 버퍼는 자동으로 마이그레이션됨
- 롤백에 대비해 디스크 버퍼 백업 권장 (기본 위치: `/var/lib/vector`)

##### 버전 0.21 주요 변경사항

1. 경로 구문 통합
   - VRL 구문으로 마이그레이션이 진행됨
   - 이전 경로 구문에서 VRL 구문으로 변경이 필요할 수 있음

2. AWS SDK 마이그레이션
   - Rusoto에서 공식 AWS SDK로 마이그레이션됨
   - IMDSv2만 지원하므로 IMDSv1 사용 환경에서는 IMDSv2 허용 설정이 필요함

#### 7.3 업그레이드 절차 예시

```bash
# 1. 현재 버전 확인
vector --version

# 2. 설정 백업
cp -r /etc/vector /etc/vector.backup
cp -r /var/lib/vector /var/lib/vector.backup

# 3. 새 버전 설치 (예: APT)
sudo apt update
sudo apt install vector

# 4. 설정 검증
vector validate /etc/vector/vector.yaml

# 5. 서비스 재시작
sudo systemctl restart vector

# 6. 상태 확인
sudo systemctl status vector
vector --version
```

#### 7.4 릴리스 정보

전체 릴리스 노트와 업그레이드 가이드는 [Vector 릴리스 페이지](https://vector.dev/releases/)에서 확인 가능.

---

### 참고 자료

- [Vector 관리 문서](https://vector.dev/docs/administration/)
- [Vector 관리 - 모니터링](https://vector.dev/docs/administration/monitoring/)
- [Vector 관리 - 검증](https://vector.dev/docs/administration/validating/)
- [Vector 관리 - 최적화](https://vector.dev/docs/administration/optimization/)
- [Vector PGO 문서](https://vector.dev/docs/administration/optimization/pgo/)
- [Vector API 문서](https://vector.dev/docs/reference/api/)
- [Vector CLI 문서](https://vector.dev/docs/reference/cli/)
- [Vector 단위 테스트 문서](https://vector.dev/docs/reference/configuration/unit-tests/)
- [Vector 릴리스](https://vector.dev/releases/)
- [Internal Logs 소스](https://vector.dev/docs/reference/configuration/sources/internal_logs/)
- [Internal Metrics 소스](https://vector.dev/docs/reference/configuration/sources/internal_metrics/)
- [Prometheus Exporter 싱크](https://vector.dev/docs/reference/configuration/sinks/prometheus_exporter/)
- [환경 변수 참조](https://vector.dev/docs/reference/environment_variables/)
- [전역 옵션 참조](https://vector.dev/docs/reference/configuration/global-options/)
- [버퍼링 모델](https://vector.dev/docs/architecture/buffering-model/)

---

## 배포 (Deployment)

> 원문: https://vector.dev/docs/setup/deployment/

Vector는 에이전트로 배포할 수 있을 만큼 효율적이면서 동시에 애그리게이터로 배포할 수 있을 만큼 강력함.

### 목차

1. [배포 역할 (Roles)](#1-배포-역할-roles)
   - [에이전트 (Agent)](#11-에이전트-agent)
   - [애그리게이터 (Aggregator)](#12-애그리게이터-aggregator)
2. [토폴로지 (Topologies)](#2-토폴로지-topologies)
   - [분산형 (Distributed)](#21-분산형-distributed)
   - [중앙 집중형 (Centralized)](#22-중앙-집중형-centralized)
   - [스트림 기반 (Stream-based)](#23-스트림-기반-stream-based)
3. [배포 전략 선택](#3-배포-전략-선택)

---

### 1. 배포 역할 (Roles)

> 원문: https://vector.dev/docs/setup/deployment/roles/

Vector는 데이터 파이프라인 아키텍처를 위한 세 가지 주요 배포 역할을 지원함.

#### 1.1 에이전트 (Agent)

에이전트 역할은 단일 호스트의 모든 데이터를 수집하도록 설계됨. 에이전트는 데몬(Daemon)과 사이드카(Sidecar) 두 가지 방식으로 배포 가능.

##### 1.1.1 데몬 (Daemon)

데몬 역할은 단일 호스트의 모든 서비스에서 데이터를 수집함. 효율적인 방향성 비순환 그래프(DAG) 토폴로지 모델을 사용 → 여러 서비스에서 동시에 데이터 수집·처리 가능.

```
┌─────────────────────────────────────────────────────────────────┐
│                            호스트                                │
│                                                                  │
│  ┌─────────┐  ┌─────────┐  ┌─────────┐                          │
│  │ 서비스 A │  │ 서비스 B │  │ 서비스 C │                          │
│  └────┬────┘  └────┬────┘  └────┬────┘                          │
│       │            │            │                                │
│       │   STDOUT   │   STDOUT   │   STDOUT                       │
│       ▼            ▼            ▼                                │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                      플랫폼 캡처                          │   │
│  └──────────────────────────┬───────────────────────────────┘   │
│                             │                                    │
│                             ▼                                    │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                    Vector (데몬)                          │   │
│  └─────────┬─────────────────┬─────────────────┬────────────┘   │
│            │                 │                 │                 │
└────────────┼─────────────────┼─────────────────┼─────────────────┘
             │                 │                 │
             ▼                 ▼                 ▼
        ┌─────────┐       ┌─────────┐       ┌─────────┐
        │  목적지 A │       │  목적지 B │       │  목적지 C │
        └─────────┘       └─────────┘       └─────────┘
```

데이터 흐름:

1. 서비스가 STDOUT으로 출력 (12-factor 앱 원칙 준수)
2. 플랫폼이 STDOUT을 캡처
3. Vector가 데이터를 수집하여 여러 목적지로 분배

장점:

- 호스트 리소스를 가장 효율적으로 사용
- 여러 서비스에서 동시에 데이터 수집 가능
- 중앙 집중식 로그 관리

권장 사항:

데몬 역할은 호스트 리소스를 가장 효율적으로 사용 → 대부분의 배포에서 권장.

##### 1.1.2 사이드카 (Sidecar)

사이드카 역할은 Vector를 개별 서비스에 결합 → 단일 서비스의 데이터 수집. 데몬 방식보다 효율성은 낮지만 관리가 더 단순할 수 있고, 수집 책임을 서비스 소유자에게 위임 가능.

```
┌─────────────────────────────────────────────────────────────────┐
│                            호스트                                │
│                                                                  │
│  ┌─────────────────────────┐  ┌─────────────────────────┐       │
│  │        Pod A            │  │        Pod B            │       │
│  │  ┌─────────┐            │  │  ┌─────────┐            │       │
│  │  │ 서비스 A │            │  │  │ 서비스 B │            │       │
│  │  └────┬────┘            │  │  └────┬────┘            │       │
│  │       │ 공유 볼륨/파일    │  │       │ 공유 볼륨/파일    │       │
│  │       ▼                 │  │       ▼                 │       │
│  │  ┌─────────────┐        │  │  ┌─────────────┐        │       │
│  │  │   Vector    │        │  │  │   Vector    │        │       │
│  │  │ (사이드카)   │        │  │  │ (사이드카)   │        │       │
│  │  └──────┬──────┘        │  │  └──────┬──────┘        │       │
│  └─────────┼───────────────┘  └─────────┼───────────────┘       │
│            │                            │                        │
└────────────┼────────────────────────────┼────────────────────────┘
             │                            │
             ▼                            ▼
        ┌─────────────────────────────────────┐
        │           다운스트림 서비스            │
        └─────────────────────────────────────┘
```

데이터 흐름:

1. 서비스가 공유 리소스(볼륨의 파일, 접근 가능한 위치)로 출력
2. Vector가 데이터를 수집
3. Vector가 다운스트림으로 데이터 전달

장점:

- 서비스별 독립적인 수집 관리
- 수집 책임을 서비스 소유자에게 위임 가능
- 서비스별 맞춤 설정 용이

사용 사례:

- 조직 구조상 서비스 소유자가 로그 수집을 담당해야 하는 경우
- 서비스별로 다른 수집 설정이 필요한 경우
- 컨테이너화된 환경에서 Pod 단위 관리가 필요한 경우

#### 1.2 애그리게이터 (Aggregator)

애그리게이터 역할은 중앙 처리에 적합하도록 설계됨 → 여러 업스트림 소스로부터 데이터를 수집하고 호스트 간 집계·분석 수행.

```
┌──────────┐   ┌──────────┐   ┌──────────┐
│  호스트 A  │   │  호스트 B  │   │  호스트 C  │
│ (에이전트) │   │ (에이전트) │   │ (에이전트) │
└─────┬────┘   └─────┬────┘   └─────┬────┘
      │              │              │
      └──────────────┼──────────────┘
                     │
                     ▼
      ┌─────────────────────────────┐
      │    Vector (애그리게이터)      │
      │                             │
      │  - 파싱                      │
      │  - 변환                      │
      │  - 보강                      │
      │  - 호스트 간 집계             │
      └──────────────┬──────────────┘
                     │
         ┌───────────┼───────────┐
         │           │           │
         ▼           ▼           ▼
    ┌─────────┐ ┌─────────┐ ┌─────────┐
    │  목적지 A │ │  목적지 B │ │  목적지 C │
    └─────────┘ └─────────┘ └─────────┘
```

핵심 기능:

1. 데이터 수신: 업스트림 Vector 인스턴스로부터 데이터 수신
2. 처리: 데이터 파싱, 변환, 보강
3. 분배: 하나 이상의 다운스트림 서비스로 데이터 팬아웃

장점:

- 호스트 간 데이터 집계 및 분석 가능
- 글로벌 메트릭 계산 (예: 전체 인프라의 에러율)
- 중앙에서 복잡한 처리 로직 관리
- 다운스트림 서비스 보호 (버퍼링, 재시도)

권장 사항:

> 중요: 가능하면 처리를 엣지로 푸시 권장. 엣지 처리가 더 효율적이고 관리하기 쉬움.

애그리게이터 역할은 호스트 간 집계와 분석이 실제로 필요한 경우에만 사용 권장. Vector의 고유한 기능 중 하나는 에이전트와 애그리게이터로 동시에 작동 가능하다는 점.

Kubernetes에서의 배포:

Kubernetes 환경에서 애그리게이터를 배포하려면 Helm 패키지 매니저 사용 가능.

```bash
# Helm 저장소 추가
helm repo add vector https://helm.vector.dev
helm repo update

# 애그리게이터로 Vector 설치
helm install vector vector/vector \
  --set role=Aggregator
```

---

### 2. 토폴로지 (Topologies)

> 원문: https://vector.dev/docs/setup/deployment/topologies/

토폴로지는 Vector 인스턴스들이 어떻게 연결되는지, 그리고 데이터가 어떻게 흐르는지를 정의함. Vector는 세 가지 주요 배포 토폴로지를 지원함.

#### 2.1 분산형 (Distributed)

분산형 토폴로지에서는 Vector가 클라이언트 노드에서 직접 다운스트림 서비스와 통신함. 중간 구성 요소 없이 가장 단순한 배포 방식.

```
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│   호스트 A    │  │   호스트 B    │  │   호스트 C    │
│              │  │              │  │              │
│ ┌──────────┐ │  │ ┌──────────┐ │  │ ┌──────────┐ │
│ │  서비스   │ │  │ │  서비스   │ │  │ │  서비스   │ │
│ └────┬─────┘ │  │ └────┬─────┘ │  │ └────┬─────┘ │
│      ▼       │  │      ▼       │  │      ▼       │
│ ┌──────────┐ │  │ ┌──────────┐ │  │ ┌──────────┐ │
│ │  Vector  │ │  │ │  Vector  │ │  │ │  Vector  │ │
│ └────┬─────┘ │  │ └────┬─────┘ │  │ └────┬─────┘ │
└──────┼───────┘  └──────┼───────┘  └──────┼───────┘
       │                 │                 │
       └─────────────────┼─────────────────┘
                         ▼
              ┌─────────────────────┐
              │  다운스트림 서비스     │
              │  (Elasticsearch 등)  │
              └─────────────────────┘
```

##### 장점

- 단순함: 구성 요소가 적어 이해하고 구현하기 쉬움
- 애플리케이션과 함께 확장: 리소스가 앱 확장에 따라 자연스럽게 증가

##### 단점

- 리소스 비효율성: 복잡한 파이프라인은 상당한 로컬 리소스를 소비 → 동일 호스트에서 실행되는 애플리케이션 성능 저하 가능
- 데이터 손실 위험: 데이터가 호스트에서 버퍼링됨 → 복구 불가능한 크래시 발생 시 버퍼링된 데이터를 잃을 가능성 높음
- 다운스트림 부담: 통합된 페이로드 대신 다운스트림 서비스에 작은 요청이 여러 번 전송됨 → 안정성에 영향 가능
- 용량 제한: 빠른 확장이나 예기치 않은 트래픽 급증 시 중앙 버퍼링이 없으면 다운스트림 서비스 과부하 가능
- 호스트 간 작업 불가: 개별 노드 격리로 인해 인프라 전체의 글로벌 메트릭 계산 같은 다중 호스트 집계 작업 불가능

##### 사용 사례

- 소규모 배포
- 단순한 파이프라인
- 빠른 프로토타이핑

#### 2.2 중앙 집중형 (Centralized)

중앙 집중형 토폴로지는 분산형과 스트림 기반 사이의 균형점 → 많은 사용 사례에서 단순성·안정성·제어 측면의 적절한 절충안.

이 방식에서는 Vector가 에이전트와 애그리게이터 역할을 모두 수행함. 클라이언트 노드의 에이전트가 중앙 Vector 서비스로 데이터를 전달하고, 중앙 서비스가 다운스트림 통신을 담당함.

```
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│   호스트 A    │  │   호스트 B    │  │   호스트 C    │
│              │  │              │  │              │
│ ┌──────────┐ │  │ ┌──────────┐ │  │ ┌──────────┐ │
│ │  서비스   │ │  │ │  서비스   │ │  │ │  서비스   │ │
│ └────┬─────┘ │  │ └────┬─────┘ │  │ └────┬─────┘ │
│      ▼       │  │      ▼       │  │      ▼       │
│ ┌──────────┐ │  │ ┌──────────┐ │  │ ┌──────────┐ │
│ │  Vector  │ │  │ │  Vector  │ │  │ │  Vector  │ │
│ │ (에이전트)│ │  │ │ (에이전트)│ │  │ │ (에이전트)│ │
│ └────┬─────┘ │  │ └────┬─────┘ │  │ └────┬─────┘ │
└──────┼───────┘  └──────┼───────┘  └──────┼───────┘
       │                 │                 │
       └─────────────────┼─────────────────┘
                         ▼
           ┌─────────────────────────┐
           │         Vector          │
           │      (애그리게이터)       │
           │                         │
           │  - 버퍼링               │
           │  - 압축                 │
           │  - 배치 최적화          │
           │  - 호스트 간 집계        │
           └────────────┬────────────┘
                        │
            ┌───────────┼───────────┐
            ▼           ▼           ▼
       ┌─────────┐ ┌─────────┐ ┌─────────┐
       │  목적지 A │ │  목적지 B │ │  목적지 C │
       └─────────┘ └─────────┘ └─────────┘
```

##### 장점

- 효율성 향상: 중앙 집중형 토폴로지는 일반적으로 클라이언트 노드와 다운스트림 서비스 모두에 더 효율적 → Vector 에이전트는 더 적은 작업을 수행해 리소스를 적게 사용. 중앙 서비스가 데이터를 버퍼링·압축하고, 다운스트림 시스템에 대한 요청 배치를 최적화
- 안정성 보호: Vector가 데이터를 버퍼링하고 완만한 간격으로 플러시 → 다운스트림 서비스를 볼륨 급증으로부터 보호
- 호스트 간 작업 가능: 데이터가 중앙 집중화되어 있어 로그를 글로벌 메트릭으로 축소하는 등 호스트 간 작업 수행 가능. 집계된 인사이트가 필요한 대규모 배포에 유용

##### 단점

- 아키텍처 복잡성: 여러 역할에서 Vector를 실행하면 단순한 분산형 설정에 비해 운영상 구성 요소 증가
- 내구성 제한: 에이전트 노드는 가능한 한 빨리 머신에서 데이터를 전송하도록 설계됨 → 중앙 Vector 서비스가 다운되면 버퍼링된 데이터 손실 가능성 있음. 데이터 손실이 허용되지 않는 사용 사례에는 스트림 기반 토폴로지 권장

##### 설정 예시

에이전트 설정 (호스트):

```yaml
# /etc/vector/vector.yaml (에이전트)
sources:
  host_logs:
    type: file
    include:
      - /var/log/*.log

sinks:
  to_aggregator:
    type: vector
    inputs:
      - host_logs
    address: "aggregator.example.com:6000"
```

애그리게이터 설정:

```yaml
# /etc/vector/vector.yaml (애그리게이터)
sources:
  from_agents:
    type: vector
    address: "0.0.0.0:6000"

transforms:
  parse_logs:
    type: remap
    inputs:
      - from_agents
    source: |
      . = parse_json!(.message)

sinks:
  elasticsearch:
    type: elasticsearch
    inputs:
      - parse_logs
    endpoints:
      - "https://elasticsearch.example.com:9200"
```

##### 사용 사례

- 중간 규모 배포
- 호스트 간 집계가 필요한 경우
- 다운스트림 서비스 보호가 필요한 경우
- 약간의 데이터 손실이 허용되는 경우

#### 2.3 스트림 기반 (Stream-based)

스트림 기반 토폴로지는 내구성과 복원력이 가장 뛰어난 토폴로지 → Kafka 같은 스트림 서비스 운영 경험이 있는 팀이 대규모 데이터 스트림에 활용.

```
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│   호스트 A    │  │   호스트 B    │  │   호스트 C    │
│              │  │              │  │              │
│ ┌──────────┐ │  │ ┌──────────┐ │  │ ┌──────────┐ │
│ │  서비스   │ │  │ │  서비스   │ │  │ │  서비스   │ │
│ └────┬─────┘ │  │ └────┬─────┘ │  │ └────┬─────┘ │
│      ▼       │  │      ▼       │  │      ▼       │
│ ┌──────────┐ │  │ ┌──────────┐ │  │ ┌──────────┐ │
│ │  Vector  │ │  │ │  Vector  │ │  │ │  Vector  │ │
│ │ (에이전트)│ │  │ │ (에이전트)│ │  │ │ (에이전트)│ │
│ └────┬─────┘ │  │ └────┬─────┘ │  │ └────┬─────┘ │
└──────┼───────┘  └──────┼───────┘  └──────┼───────┘
       │                 │                 │
       └─────────────────┼─────────────────┘
                         ▼
           ┌─────────────────────────┐
           │    스트림 서비스          │
           │       (Kafka)           │
           │                         │
           │  - 여러 노드에 복제       │
           │  - 높은 내구성           │
           │  - 데이터 보존           │
           └────────────┬────────────┘
                        │
                        ▼
           ┌─────────────────────────┐
           │         Vector          │
           │      (애그리게이터)       │
           └────────────┬────────────┘
                        │
            ┌───────────┼───────────┐
            ▼           ▼           ▼
       ┌─────────┐ ┌─────────┐ ┌─────────┐
       │  목적지 A │ │  목적지 B │ │  목적지 C │
       └─────────┘ └─────────┘ └─────────┘
```

##### 장점

- 가장 높은 내구성과 신뢰성: Kafka와 같은 스트림 서비스는 높은 내구성과 신뢰성을 위해 설계되어 여러 노드에 데이터 복제
- 가장 높은 효율성: Vector 에이전트는 최소한의 작업만 수행하고, Vector 서비스는 내구성 문제보다 성능에 집중 가능
- 데이터 재스트리밍 가능: 스트림의 보존 기간에 따라 데이터 재스트리밍 가능
- 책임의 명확한 분리: Vector는 순수하게 라우팅 레이어로 사용되며 내구성에 대한 책임 없음. 내구성은 시간이 지남에 따라 전환·발전시킬 수 있는 전용 서비스에 위임

##### 단점

- 관리 오버헤드 증가: Kafka와 같은 스트림 서비스 관리는 복잡한 작업 → 일반적으로 경험 있는 팀 필요
- 더 복잡함: 이 접근 방식은 프로덕션 수준의 스트림 관리·인프라에 대한 깊은 이해 필요
- 더 비쌈: 운영 복잡성 외에도 전용 스트림 클러스터를 유지하려면 추가 리소스가 필요해 전체 비용 증가

##### 설정 예시

에이전트 설정 (호스트):

```yaml
# /etc/vector/vector.yaml (에이전트)
sources:
  host_logs:
    type: file
    include:
      - /var/log/*.log

sinks:
  to_kafka:
    type: kafka
    inputs:
      - host_logs
    bootstrap_servers: "kafka1:9092,kafka2:9092,kafka3:9092"
    topic: "logs"
    encoding:
      codec: json
```

애그리게이터 설정:

```yaml
# /etc/vector/vector.yaml (애그리게이터)
sources:
  from_kafka:
    type: kafka
    bootstrap_servers: "kafka1:9092,kafka2:9092,kafka3:9092"
    topics:
      - "logs"
    group_id: "vector-aggregator"

transforms:
  parse_logs:
    type: remap
    inputs:
      - from_kafka
    source: |
      . = parse_json!(.message)

sinks:
  elasticsearch:
    type: elasticsearch
    inputs:
      - parse_logs
    endpoints:
      - "https://elasticsearch.example.com:9200"
```

##### 사용 사례

- 대규모 배포
- 데이터 손실이 허용되지 않는 경우
- 데이터 재처리가 필요한 경우
- 여러 컨슈머가 동일한 데이터를 처리해야 하는 경우
- Kafka 운영 경험이 있는 팀

---

### 3. 배포 전략 선택

적합한 배포 전략은 조직의 요구 사항·규모·팀 역량에 따라 달라짐.

#### 3.1 의사 결정 가이드

- 복잡성: 분산형 낮음 · 중앙 집중형 중간 · 스트림 기반 높음
- 내구성: 분산형 낮음 · 중앙 집중형 중간 · 스트림 기반 높음
- 확장성: 분산형 제한적 · 중앙 집중형 좋음 · 스트림 기반 매우 좋음
- 호스트 간 집계: 분산형 불가능 · 중앙 집중형 가능 · 스트림 기반 가능
- 운영 오버헤드: 분산형 낮음 · 중앙 집중형 중간 · 스트림 기반 높음
- 비용: 분산형 낮음 · 중앙 집중형 중간 · 스트림 기반 높음
- 데이터 손실 위험: 분산형 높음 · 중앙 집중형 중간 · 스트림 기반 낮음

#### 3.2 권장 시나리오

##### 분산형 권장

- 개발/테스트 환경
- 소규모 인프라 (호스트 10개 미만)
- 단순한 로그 수집 요구사항
- 빠른 구현이 필요한 경우

##### 중앙 집중형 권장

- 중간 규모 인프라 (호스트 10~100개)
- 호스트 간 집계가 필요한 경우
- 다운스트림 서비스 보호가 중요한 경우
- 약간의 데이터 손실이 허용되는 프로덕션 환경

##### 스트림 기반 권장

- 대규모 인프라 (호스트 100개 이상)
- 데이터 손실이 절대 허용되지 않는 경우
- 데이터 재처리가 필요한 경우
- Kafka 운영 경험이 있는 팀
- 여러 시스템이 동일한 데이터를 소비하는 경우

#### 3.3 하이브리드 접근 방식

실제 환경에서는 여러 토폴로지를 조합해 사용 가능.

```
┌─────────────────────────────────────────────────────────────────┐
│                        프로덕션 환경                              │
│                                                                  │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐              │
│  │ 중요 서비스들  │  │ 일반 서비스들  │  │  개발 환경    │              │
│  │             │  │             │  │             │              │
│  │ 스트림 기반   │  │ 중앙 집중형   │  │   분산형     │              │
│  │  (Kafka)    │  │             │  │             │              │
│  └─────────────┘  └─────────────┘  └─────────────┘              │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

예시:

- 결제, 인증 등 중요 서비스: 스트림 기반 토폴로지
- 일반 애플리케이션 서비스: 중앙 집중형 토폴로지
- 개발/테스트 환경: 분산형 토폴로지

---

### 참고 자료

- [Vector 배포 문서](https://vector.dev/docs/setup/deployment/)
- [Vector 배포 역할](https://vector.dev/docs/setup/deployment/roles/)
- [Vector 배포 토폴로지](https://vector.dev/docs/setup/deployment/topologies/)
- [Helm 설치 가이드](https://vector.dev/docs/setup/installation/package-managers/helm/)
- [Vector 소스/싱크](https://vector.dev/docs/reference/configuration/sources/vector/)
- [Kafka 소스](https://vector.dev/docs/reference/configuration/sources/kafka/)
- [Kafka 싱크](https://vector.dev/docs/reference/configuration/sinks/kafka/)

---

## 프로덕션 환경으로 전환 (Going to Production)

> 원본 참고: https://vector.dev/docs/setup/going-to-prod/

---

### 목차

1. [개요](#개요)
2. [레퍼런스 아키텍처](#레퍼런스-아키텍처)
   - [애그리게이터 아키텍처](#애그리게이터-아키텍처)
   - [에이전트 아키텍처](#에이전트-아키텍처)
   - [통합 아키텍처](#통합-아키텍처)
3. [아키텍처 설계](#아키텍처-설계)
   - [설계 지침](#설계-지침)
   - [배포 역할](#배포-역할)
   - [네트워킹](#네트워킹)
   - [데이터 수집](#데이터-수집)
   - [데이터 처리](#데이터-처리)
   - [버퍼링 전략](#버퍼링-전략)
   - [라우팅 및 분리](#라우팅-및-분리)
4. [보안 강화 (Hardening)](#보안-강화-hardening)
   - [위협 모델](#위협-모델)
   - [심층 방어 전략](#심층-방어-전략)
5. [고가용성 (High Availability)](#고가용성-high-availability)
   - [장애 모드](#장애-모드)
   - [고가용성 전략](#고가용성-전략)
   - [재해 복구](#재해-복구)
6. [롤아웃 (Rollout)](#롤아웃-rollout)
   - [롤아웃 전략](#롤아웃-전략)
   - [롤아웃 계획](#롤아웃-계획)
7. [사이징 및 용량 계획 (Sizing)](#사이징-및-용량-계획-sizing)
   - [인스턴스 권장 사항](#인스턴스-권장-사항)
   - [메모리 및 CPU 전략](#메모리-및-cpu-전략)
   - [디스크 구성](#디스크-구성)
   - [스케일링 접근 방식](#스케일링-접근-방식)
   - [오토스케일링](#오토스케일링)
   - [실제 사례](#실제-사례)

---

### 개요

이 섹션은 Vector를 프로덕션 환경에 배포하는 인프라 아키텍트를 위한 가이드라인과 모범 사례를 제공함. 프로덕션 배포를 위한 다섯 가지 주요 영역을 다룸:

1. 아키텍처 설계 (Architecting) - Vector 인프라 설계 원칙
2. 보안 강화 (Hardening) - 보안 및 견고성 조치
3. 고가용성 (High Availability) - 서비스 연속성 보장
4. 롤아웃 (Rollout) - 배포 전략 및 단계적 접근 방식
5. 사이징 및 용량 계획 (Sizing) - 리소스 할당 가이드

---

### 레퍼런스 아키텍처

레퍼런스 아키텍처는 파이프라인 인프라의 출발점을 제공함. Vector는 세 가지 주요 배포 패턴을 지원함.

#### 애그리게이터 아키텍처

"원격 처리를 위해 전용 노드에 Vector를 애그리게이터로 배포."

애그리게이터 아키텍처는 중앙 집중식 로그·메트릭 수집을 위해 설계됨. 데이터는 하나 이상의 업스트림 에이전트 또는 업스트림 시스템에서 수집됨.

##### 사용 시기

이 아키텍처는 다음과 같은 환경에 적합:

- 높은 내구성 또는 고가용성이 필요한 환경(대부분의 환경)
- 기존 에이전트를 변경하지 않고도 복잡한 환경에 쉽게 통합 가능
- 기업 및 대규모 조직에 특히 적합

##### 프로덕션 권장 사항

아키텍처 설계:
- 네트워크 경계(클러스터/VPC)당 여러 애그리게이터 배포
- 에이전트 라우팅을 위해 DNS 또는 서비스 디스커버리 사용
- HTTP 기반 프로토콜 선호
- Vector 간 통신을 위해 Vector 네이티브 source/sink 사용
- 처리 책임을 애그리게이터로 이동
- 에이전트는 단순한 포워더로 구성

고가용성:
- 여러 노드 및 가용 영역에 애그리게이터 배포
- 모든 소스에 대해 엔드투엔드 승인(acknowledgements) 활성화
- 주요 싱크 시스템에 디스크 버퍼 구현
- 분석 시스템에 워터폴 버퍼 사용
- 실패한 데이터를 주요 시스템으로 라우팅

사이징 및 용량:
- 애그리게이터 트래픽 로드 밸런싱
- 인스턴스당 최소 4 vCPU 및 8 GiB 메모리 프로비저닝
- 85% CPU 사용률을 목표로 오토스케일링 활성화

롤아웃:
"한 번에 하나의 네트워크 파티션과 하나의 시스템"으로 점진적 도입 구현

##### 고급 패턴

Pub-Sub 시스템:
Vector는 새로운 pub-sub 서비스를 프로비저닝할 필요 없음. Kafka와 같은 기존 시스템에서 데이터 소비 가능, "서비스 또는 호스트와 같은 데이터 출처 라인" 기반 파티셔닝 권장.

글로벌 애그리게이션:
단일 장애 지점을 생성하지 않으면서 데이터 축소(히스토그램)를 위한 계층화된 애그리게이션 지원.

---

#### 에이전트 아키텍처

"엣지에서 Vector를 실행해 처리를 민주화."

에이전트 아키텍처는 로컬 데이터 수집·처리를 위해 각 노드에 Vector를 에이전트로 배포함. 데이터는 Vector에서 직접 수집하거나, 다른 에이전트를 통해 간접적으로 수집하거나, 두 가지를 동시에 수행 가능.

##### 사용 시기

이 아키텍처는 다음과 같은 경우에 권장:

- 높은 내구성이나 고가용성이 필요하지 않은 단순한 환경
- 빠른 상태 비저장 처리 및 스트리밍 전송처럼 확장된 데이터 보존이 필요하지 않은 사용 사례
- 최소한의 저항으로 노드 수준 수정을 구현 가능한 운영자

##### 프로덕션 권장 사항

아키텍처 설계:
- 일반 데이터 포워딩을 수행하는 에이전트만 교체; 특수 에이전트와는 통합
- 처리를 "빠른 상태 비저장 처리"로 제한
- 전송을 "빠른 스트리밍 전송"으로 제한
- 인메모리 버퍼링만 사용; 디스크 버퍼링 피하기

고가용성:
- 단일 Vector 에이전트 장애가 문제가 되는 경우, 애그리게이터 아키텍처 고려
- 데이터 수신 실패 완화를 위해 엔드투엔드 승인 활성화
- 처리 실패 해결을 위해 드롭된 이벤트 라우팅

사이징 요구 사항:
Vector 에이전트를 2 vCPU 및 4 GiB 메모리로 제한

롤아웃 전략:
"한 번에 하나의 네트워크 파티션과 하나의 시스템"으로 점진적 배포

##### 고급 섹션

엣지에서의 처리:
에이전트는 데이터를 보존하지 않아야 함. 처리와 전송은 "빠르고 스트리밍" 방식이어야 함.

---

#### 통합 아키텍처

"엣지에서 수집하고 애그리게이션해 최대 유연성 제공."

통합 아키텍처는 Vector를 에이전트와 애그리게이터 모두로 배포 → 에이전트 아키텍처와 애그리게이터 아키텍처를 결합한 통합 옵저버빌리티 파이프라인 생성.

##### 사용 시기

이 아키텍처는 이미 애그리게이터 접근 방식을 구현하고 개별 노드로 Vector를 확장하려는 사용자에게 권장. 이 진화는 다음을 통해 옵저버빌리티 인프라를 강화:

- Vector가 데이터 전송 책임을 관리 → 에이전트 관련 위험 감소
- vector source·sink 컴포넌트로 Vector 네이티브 프로토콜을 사용 → 성능 향상
- 상태 비저장 처리 작업을 엣지 위치로 이동 → 확장성 개선
- 고급 장애 조치 및 재해 복구 기능 지원

---

### 아키텍처 설계

#### 설계 지침

프로덕션 배포를 위한 핵심 설계 지침:

1. 최적의 도구 사용 - Vector는 모든 도구를 대체하기보다는 기존 도구를 보완
2. 에이전트 책임 최소화 - 에이전트는 단순한 데이터 포워더로 유지, 복잡한 작업은 Vector에 위임
3. 데이터 가까이에 Vector 배포 - 단일 장애 지점을 피하기 위해 인프라 전체에 Vector를 분산 배포

#### 배포 역할

Vector는 두 가지 주요 역할로 배포 가능:

- 에이전트 역할: 개별 노드에 직접 Vector를 배포해 로컬 데이터 수집·처리. 데이터는 로컬에서 처리하거나 원격 애그리게이터로 전달 가능
- 애그리게이터 역할: 업스트림 에이전트 또는 pub-sub 서비스에서 데이터를 수신하는 전용 노드에 Vector 배포. 성능 최적화된 하드웨어에서 중앙 집중식 처리

#### 네트워킹

네트워크 경계 내 배포:
- 계정 및 리전 전반에 걸쳐 네트워크 경계(클러스터/VPC)당 애그리게이터 배포
- 이 전략은 단일 장애 지점 방지, 쉽고 안전한 내부 통신 허용, 네트워크 관리자가 유지하는 경계 존중

방화벽 액세스 제한:
- 에이전트 -> 애그리게이터
- 애그리게이터 -> 구성된 소스/싱크

DNS/서비스 디스커버리:
Vector 애그리게이터 등록을 위해 DNS 또는 서비스 디스커버리 사용

프로토콜 선호:
로드 밸런싱 및 전송 승인을 위해 HTTP 및 gRPC 프로토콜 선호

#### 데이터 수집

에이전트 선택 가이드:

교체할 에이전트:
- 일반적인 포워딩을 수행하는 에이전트 (로그 테일링, 기본 메트릭 수집)

통합할 에이전트:
- 강화된 데이터를 생성하는 벤더 특정 도구 (Datadog Agent, Metricbeat, Telegraf 등)

#### 데이터 처리

로컬 처리:
- 높은 내구성/가용성 요구 사항이 없는 단순한 환경에 적합

원격 처리:
- HA/내구성이 필요한 대부분의 환경에 권장

통합 처리:
- 두 전략을 균형 있게 결합하는 접근 방식

#### 버퍼링 전략

- 싱크별로 격리된 버퍼로 목적지 가까이에서 데이터 버퍼링
- 고내구성 기록 시스템에는 디스크 버퍼 사용
- 저지연 분석 시스템에는 메모리 버퍼 사용
- 아카이브용: 배치 크기 5MB 이상, 타임아웃 5분 이상
- 분석용: 타임아웃 5초 이하

#### 라우팅 및 분리

기록 시스템 (Systems of Record):
- 원시, 처리되지 않은 데이터 아카이브
- 디스크 버퍼링 사용

분석 시스템 (Systems of Analysis):
- 데이터 샘플링/정리
- 메모리 버퍼링 사용
- 불필요한 속성 제거

---

### 보안 강화 (Hardening)

이 섹션은 데이터·프로세스·호스트·네트워크 계층 전반에 걸쳐 심층 방어 전략을 적용한 Vector 배포를 위한 보안 가이드를 제공함.

#### 위협 모델

다섯 가지 주요 위협에 대한 방어:

- 도청 공격: 디스크 암호화·스왑 비활성화·디렉토리 제한·스토리지 암호화·속성 수정·코어 덤프 방지·엔드투엔드 TLS·현대적 암호화·방화벽
- 공급망 공격: 암호화된 채널을 통한 다운로드·Vector 다운로드 검증
- 자격 증명 도난: 평문 비밀 저장 회피·구성 디렉토리 제한
- 권한 상승: 비특권 사용자로 Vector 실행·서비스 계정 제한·단일 테넌시·원격 액세스 비활성화
- 업스트림 공격: 보안 정책 검토·빈번한 업그레이드

#### 심층 방어 전략

심층 방어는 네 가지 계층으로 구성된 "양파 모델"을 따름:

##### 데이터 보안

```yaml
# 데이터 보안 체크리스트
- 운영 체제 수준에서 전체 디스크 암호화 활성화
- 메모리가 디스크로 오버플로되는 것을 방지하기 위해 스왑 비활성화
- Vector 데이터 디렉토리 권한 제한
- 플랫폼별 방법을 사용하여 외부 스토리지 암호화
- VRL 함수를 사용하여 민감한 PII 수정
- 시스템 제한을 통해 코어 덤프 비활성화
```

VRL을 사용한 민감 데이터 수정 예시:

```yaml
transforms:
  redact_pii:
    type: remap
    inputs:
      - source_id
    source: |
      # 이메일 주소 수정
      .message = redact(.message, filters: ["email"])

      # 신용카드 번호 수정
      .message = redact(.message, filters: ["credit_card"])

      # 사용자 정의 패턴 수정
      .message = redact(.message, patterns: [r'\d{3}-\d{2}-\d{4}'])
```

##### 프로세스 보안

- Vector의 오픈 소스 코드를 활용해 투명성 확보
- TLS를 통해서만 아티팩트 다운로드
- 구성에 평문 비밀 저장 금지
- 구성 디렉토리 액세스 제한
- Vector를 root로 실행 금지
- 서비스 계정 권한 제한
- 빈번한 업그레이드 유지

환경 변수를 사용한 비밀 관리:

```yaml
sinks:
  elasticsearch:
    type: elasticsearch
    endpoints:
      - "https://elasticsearch.example.com:9200"
    auth:
      user: "${ELASTICSEARCH_USER}"
      password: "${ELASTICSEARCH_PASSWORD}"
```

##### 호스트 보안

- 애그리게이터 머신에 단일 테넌트로 Vector 배포
- SSH/직접 액세스 비활성화; 대신 중앙 제어 플레인 사용

##### 네트워크 보안

- 모든 소스 및 싱크에 엔드투엔드 TLS 활성화
- 현대적 암호화 구현 (TLS 1.3 권장)
- 트래픽 제한을 위해 방화벽 배포

TLS 구성 예시:

```yaml
sources:
  http_server:
    type: http_server
    address: "0.0.0.0:443"
    tls:
      enabled: true
      crt_file: "/etc/vector/certs/server.crt"
      key_file: "/etc/vector/certs/server.key"
      ca_file: "/etc/vector/certs/ca.crt"
      verify_certificate: true

sinks:
  elasticsearch:
    type: elasticsearch
    endpoints:
      - "https://elasticsearch.example.com:9200"
    tls:
      verify_certificate: true
      verify_hostname: true
```

> 참고: Vector는 Rust의 어파인 타입 시스템으로 메모리 안전성을 달성하고 자동 메모리 정리로 데이터 노출을 최소화함. 더 이상 필요하지 않을 때 메모리에서 데이터를 자동으로 제거함.

---

### 고가용성 (High Availability)

"인프라 수준 소프트웨어의 엄격한 가동 시간 요구 사항 충족."

#### 장애 모드

##### 하드웨어 장애

디스크 장애:
디스크 장애는 프로세스 장애로 처리됨. 런타임 중에 디스크를 사용할 수 없게 되면 Vector 종료됨. 디스크는 디스크 버퍼·파일 소스/싱크와 같은 컴포넌트를 사용하는 경우에만 필요.

노드 장애:
완화 방법:
- 여러 노드에 Vector 분산
- 장애 조치를 위한 로드 밸런서 사용
- 단일 노드가 데이터의 33% 이상을 처리하지 않도록 보장
- 자동화된 자가 치유 구현

데이터 센터 장애:
보호 전략:
- 여러 가용 영역에 Vector 배포
- 단일 AZ 손실을 처리할 수 있는 용량 유지
- 오버 프로비저닝을 위해 Vector의 고성능 설계 활용

##### 소프트웨어 장애

Vector 프로세스 장애:
노드 장애와 유사한 완화: 플랫폼 수준 감독자(예: Kubernetes 컨트롤러)를 사용한 로드 밸런서 장애 조치

데이터 수신 장애:
해결책: "Vector의 엔드투엔드 승인 기능 활성화." 승인 전송 전까지 데이터를 지속함.

```yaml
sources:
  http_server:
    type: http_server
    address: "0.0.0.0:8080"
    acknowledgements:
      enabled: true
```

데이터 처리 장애:
접근 방식: remap transform으로 드롭된 이벤트를 백업 목적지로 라우팅하여 나중에 검사·재생할 수 있도록 처리

```yaml
transforms:
  parse_logs:
    type: remap
    inputs:
      - source_id
    source: |
      structured, err = parse_json(.message)
      if err != null {
        log("Failed to parse: " + err, level: "error")
      }
      . = structured ?? .
    drop_on_error: true
    reroute_dropped: true

sinks:
  # 성공적으로 처리된 이벤트
  main_destination:
    type: elasticsearch
    inputs:
      - parse_logs
    # ...

  # 실패한 이벤트 백업
  failed_events:
    type: file
    inputs:
      - parse_logs.dropped
    path: "/var/log/vector/failed/%Y-%m-%d.log"
    encoding:
      codec: json
```

데이터 전송 장애:
완화 방법:
- 자동 연결 스케일링을 위한 적응형 요청 동시성 (ARC)
- 백프레셔를 흡수하기 위한 버퍼
- 재생 기능을 갖춘 내구성 있는 데이터 지속성

```yaml
sinks:
  elasticsearch:
    type: elasticsearch
    inputs:
      - transform_id
    endpoints:
      - "https://elasticsearch.example.com:9200"
    buffer:
      type: disk
      max_size: 10737418240  # 10 GiB
      when_full: block
    request:
      adaptive_concurrency:
        decrease_ratio: 0.9
        ewma_alpha: 0.4
        rtt_deviation_scale: 2.5
```

##### 전체 시스템 장애

서비스 디스커버리 또는 DNS를 사용하여 네트워크 내 스탠바이로 장애 조치:
- 로드 밸런서 시스템 장애
- 애그리게이터 시스템 장애

#### 고가용성 전략

##### 폭발 반경 억제

단일 애그리게이터 장애의 연쇄 발생 방지 → 네트워크 경계 내에 Vector 배포.

##### 하드웨어 장애 완화

1. 여러 가용 영역에 노드 배포
2. 단일 AZ 장애를 처리할 수 있는 용량 확보

##### 소프트웨어 장애 완화

구성 요구 사항:
- 가능한 소스에서 엔드투엔드 승인 활성화
- 디스크 오버플로가 있는 디스크/메모리 버퍼 구현
- 드롭된 이벤트를 기록 시스템으로 라우팅

워터폴 버퍼 구성 예시:

```yaml
sinks:
  primary_destination:
    type: elasticsearch
    inputs:
      - transform_id
    endpoints:
      - "https://elasticsearch.example.com:9200"
    buffer:
      type: disk
      max_size: 10737418240  # 10 GiB
      when_full: overflow
```

##### 전체 시스템 장애 완화

엄격한 요구 사항을 위한 고급 전술:
- 에이전트가 스탠바이 로드 밸런서로 장애 조치
- 로드 밸런서가 스탠바이 애그리게이터로 장애 조치
- 비용 절감을 위해 오토스케일링 사용

#### 재해 복구

내부 DR:
Vector는 복제된 상태를 공유하지 않음. 더 넓은 조직 DR 계획을 따름.

외부 DR:
Vector는 서킷 브레이커를 통해 관리형 서비스 DR 사이트로의 라우팅을 용이하게 함.

---

### 롤아웃 (Rollout)

이 섹션은 Vector를 프로덕션 환경에 롤아웃하기 위한 전략을 제공함.

#### 롤아웃 전략

네 가지 핵심 원칙:

##### 1. 점진적 도입

한 번에 하나의 네트워크 파티션(클러스터 또는 VPC)에 Vector를 배포 → 지속 가능한 진행 가능.

##### 2. 범위 최소화

롤아웃 계획에 따라 각 단위에 대해 "한 번에 하나의 네트워크 파티션과 하나의 시스템"에 배포 집중.

##### 3. 안전한 실패

Vector는 현재 프로덕션 워크플로를 방해하지 않고 "중복 데이터 스트림에서 작동"해야 하며, 비즈니스 크리티컬한 사용 전에 신뢰 구축 필요.

##### 4. "빅 스위치" 피하기

전환 시점까지 Vector는 이미 "지속적인 기간 동안 프로덕션 용량으로 운영"되어 신뢰성을 보장해야 함.

#### 롤아웃 계획

5단계 롤아웃 계획:

##### Phase 1: 블랙홀 배포

- 단일 네트워크 파티션 선택
- 해당 네트워크 파티션 내에 단일 blackhole 싱크(기본값)로 Vector 배포
- 10 MiB/s/vCPU로 인스턴스를 보수적으로 사이징

```yaml
# Phase 1: 블랙홀 구성
sources:
  http_server:
    type: http_server
    address: "0.0.0.0:8080"

sinks:
  blackhole:
    type: blackhole
    inputs:
      - http_server
    print_interval_secs: 60
```

##### Phase 2: 데이터 복사 스트리밍

- 에이전트에서 Vector로 데이터 복사 스트리밍
- `vector top`·`vector tap` 명령으로 수신 확인

```bash
# Vector 상태 모니터링
vector top

# 실시간 이벤트 검사
vector tap transform_id
```

##### Phase 3: Vector 구성

- 사용 사례에 따라 데이터 처리
- 다운스트림 시스템과 통합
- 목적지에서 데이터 확인

```yaml
# Phase 3: 전체 구성 예시
sources:
  http_server:
    type: http_server
    address: "0.0.0.0:8080"
    acknowledgements:
      enabled: true

transforms:
  parse_logs:
    type: remap
    inputs:
      - http_server
    source: |
      . = parse_json!(.message)
      .processed_at = now()

sinks:
  elasticsearch:
    type: elasticsearch
    inputs:
      - parse_logs
    endpoints:
      - "https://elasticsearch.example.com:9200"
    bulk:
      index: "logs-%Y-%m-%d"
    buffer:
      type: disk
      max_size: 10737418240
```

##### Phase 4: 사이징, 스케일링 및 소크

- 오토스케일링 활성화
- 프로덕션 준비 상태를 위해 24시간 이상 모니터링

```yaml
# 오토스케일링 예시 (Kubernetes HPA)
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: vector-aggregator
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: vector-aggregator
  minReplicas: 3
  maxReplicas: 10
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 85
  behavior:
    scaleDown:
      stabilizationWindowSeconds: 300
    scaleUp:
      stabilizationWindowSeconds: 300
```

##### Phase 5: 전환

1. 에이전트를 안전하게 종료 → 손실 없이 데이터 드레인
2. Vector로만 데이터를 전송하도록 에이전트 재구성
3. 에이전트 재시작

```yaml
# 에이전트 구성 예시 - Vector로만 전송
# Fluent Bit 구성 예시
[OUTPUT]
    Name        http
    Match       *
    Host        vector-aggregator.example.com
    Port        8080
    URI         /
    Format      json
    tls         On
    tls.verify  On
```

---

### 사이징 및 용량 계획 (Sizing)

Vector 워크로드는 일반적으로 CPU 집약적.

#### 기준 메트릭

- 비구조화 로그: ~10 MiB/s/vCPU
- 구조화 로그 및 메트릭: ~25 MiB/s/vCPU
- 트레이스 스팬: ~25 MiB/s/vCPU

#### 인스턴스 권장 사항

최소 요구 사항:
- 인스턴스당 최소 8 vCPU 및 16 GiB 메모리

클라우드 제공자별 권장 인스턴스:

- AWS: c6i.2xlarge 또는 c6g.2xlarge
- Azure: f8 또는 D8plds_v6
- GCP: c2(8 vCPU, 16 GiB)

역할별 최소 요구 사항:

- 에이전트 역할: 최소 2 vCPU
- 애그리게이터 역할: 최소 4 vCPU

#### 메모리 및 CPU 전략

- ARM64 아키텍처가 일반적으로 더 나은 성능 투자를 제공
- 일반적인 시작점으로 vCPU당 2 GiB 메모리 권장
- 인메모리 배칭으로 인해 싱크 수에 따라 메모리 사용량이 스케일됨

#### 디스크 구성

디스크 버퍼의 경우:
- 10분 분량의 데이터 용량 프로비저닝
- 적절한 여유를 위해 처리량을 예상 최대 처리량의 최소 2배로 구성

비용 예시:
48 GiB 디스크 공간은 AWS EBS io2에서 약 $0.20/일 비용 발생

```yaml
# 디스크 버퍼 구성 예시
sinks:
  elasticsearch:
    type: elasticsearch
    inputs:
      - transform_id
    endpoints:
      - "https://elasticsearch.example.com:9200"
    buffer:
      type: disk
      max_size: 53687091200  # 50 GiB (10분 @ 100 MiB/s)
      when_full: block
```

#### 스케일링 접근 방식

##### 수직 스케일링

- Vector는 사용 가능한 vCPU를 자동으로 활용
- 고가용성 복원력을 위해 인스턴스를 총 볼륨의 33% 이하로 제한

##### 수평 스케일링

- 로드 밸런서 배포
- Keep-alive 활성화
- aggregate/dedupe transform에 스테이트풀 라우팅 사용
- 다른 시나리오에는 라운드 로빈 사용

```yaml
# Kubernetes 서비스 예시 - 로드 밸런싱
apiVersion: v1
kind: Service
metadata:
  name: vector-aggregator
spec:
  type: LoadBalancer
  selector:
    app: vector-aggregator
  ports:
    - port: 8080
      targetPort: 8080
  sessionAffinity: None
```

#### 오토스케일링

권장 설정:
- 5분 평균 85% CPU 사용률을 목표로 설정
- 일치하는 안정화 기간 설정
- CPU 사용률이 가장 강력한 스케일링 신호

```yaml
# Kubernetes HPA 예시
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: vector-aggregator
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: vector-aggregator
  minReplicas: 3
  maxReplicas: 20
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 85
  behavior:
    scaleDown:
      stabilizationWindowSeconds: 300
      policies:
        - type: Percent
          value: 10
          periodSeconds: 60
    scaleUp:
      stabilizationWindowSeconds: 300
      policies:
        - type: Percent
          value: 100
          periodSeconds: 60
```

#### 실제 사례

##### 시나리오 1: 비구조화 로그 10 TiB/일

요구 사항:
- 10 TiB/일 = ~119 MiB/s
- 비구조화 로그 처리량: ~10 MiB/s/vCPU
- 필요 vCPU: 119 / 10 = 약 12 vCPU

권장 구성:
- 인스턴스: 3 x c6g.xlarge (각 4 vCPU)
- 예상 비용: 약 $6.20/일

##### 시나리오 2: 트레이싱 데이터 25 TiB/일

요구 사항:
- 25 TiB/일 = ~298 MiB/s
- 트레이스 처리량: ~25 MiB/s/vCPU
- 필요 vCPU: 298 / 25 = 약 12 vCPU

권장 구성:
- 인스턴스: 4 x c2 (GCP)
- 예상 비용: 약 $10.49/일

---

### 참고 자료

- [Vector 공식 문서](https://vector.dev/docs/)
- [Going to Production](https://vector.dev/docs/setup/going-to-prod/)
- [Architecting](https://vector.dev/docs/setup/going-to-prod/architecting/)
- [Hardening](https://vector.dev/docs/setup/going-to-prod/hardening/)
- [High Availability](https://vector.dev/docs/setup/going-to-prod/high-availability/)
- [Rollout](https://vector.dev/docs/setup/going-to-prod/rollout/)
- [Sizing](https://vector.dev/docs/setup/going-to-prod/sizing/)
- [Reference Architectures](https://vector.dev/docs/setup/going-to-prod/arch/)
- [Aggregator Architecture](https://vector.dev/docs/setup/going-to-prod/arch/aggregator/)
- [Agent Architecture](https://vector.dev/docs/setup/going-to-prod/arch/agent/)
- [Unified Architecture](https://vector.dev/docs/setup/going-to-prod/arch/unified/)
