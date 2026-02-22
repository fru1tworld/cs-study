# Vector 관리 (Administration)

Vector는 다양한 운영 체제와 플랫폼에서 쉽게 설치하고 운영할 수 있도록 설계되었습니다. 이 문서에서는 Vector 인스턴스 관리부터 설정 검증까지 핵심 관리 작업의 전체 범위를 다룹니다.

## 목차

1. [관리 (Management)](#1-관리-management)
2. [모니터링 (Monitoring)](#2-모니터링-monitoring)
3. [검증 (Validating)](#3-검증-validating)
4. [최적화 (Optimization)](#4-최적화-optimization)
5. [GraphQL API](#5-graphql-api)
6. [단위 테스트 (Unit Testing)](#6-단위-테스트-unit-testing)
7. [업그레이드 (Upgrading)](#7-업그레이드-upgrading)

---

## 1. 관리 (Management)

Vector 인스턴스를 시작, 중지, 재시작, 리로드하는 방법을 다양한 환경에서 설명합니다.

### 1.1 Linux (systemctl)

APT, dpkg, RPM, YUM 또는 pacman을 사용하여 Vector를 설치한 경우 `systemctl`을 사용하여 관리할 수 있습니다.

#### 시작

```bash
sudo systemctl start vector
```

#### 중지

```bash
sudo systemctl stop vector
```

#### 재시작

```bash
sudo systemctl restart vector
```

#### 리로드 (설정 재적용)

```bash
systemctl kill -s HUP --kill-who=main vector.service
```

#### 상태 확인

```bash
sudo systemctl status vector
```

#### 부팅 시 자동 시작 활성화

```bash
sudo systemctl enable vector
```

#### 부팅 시 자동 시작 비활성화

```bash
sudo systemctl disable vector
```

### 1.2 macOS (Homebrew)

Homebrew를 사용하여 Vector를 설치한 경우 Homebrew의 서비스 유틸리티를 사용하여 관리할 수 있습니다.

#### 시작

```bash
brew services start vector
```

#### 중지

```bash
brew services stop vector
```

#### 재시작

```bash
brew services restart vector
```

### 1.3 Windows

MSI를 사용하여 Windows에 Vector를 설치한 경우 다음 명령을 사용할 수 있습니다.

#### 시작

```powershell
C:\Program Files\Vector\bin\vector --config C:\Program Files\Vector\config\vector.yaml
```

TOML, JSON 또는 YAML 형식의 설정 파일을 사용할 수 있습니다.

### 1.4 Docker

Docker를 사용하여 Vector를 실행하는 경우 명령 인터페이스는 모든 플랫폼에서 동일합니다.

#### 시작

```bash
docker run \
  -d \
  -v ~/vector.yaml:/etc/vector/vector.yaml:ro \
  -p 8686:8686 \
  timberio/vector:0.52.0-alpine
```

#### 중지

```bash
docker stop <container-id>
```

#### 다시 시작

```bash
docker restart <container-id>
```

### 1.5 Vector 실행 파일 직접 사용

프로세스 관리자 없이 Vector 실행 파일을 직접 관리하는 방법입니다.

#### 시작

```bash
vector --config /etc/vector/vector.yaml
```

#### 리로드

```bash
killall -s SIGHUP vector
```

### 1.6 설정 자동 리로드

`--watch-config` 또는 `-w` 플래그를 설정하여 설정 파일이 변경될 때 Vector가 자동으로 리로드되도록 할 수 있습니다.

```bash
vector --config /etc/vector/vector.yaml --watch-config
```

#### 설정 감시 방법 지정

`--watch-config-method` 옵션으로 설정 변경 감시 방법을 지정할 수 있습니다:

- `recommended` (기본값): 파일 이벤트 리스너를 사용하여 파일 변경 이벤트 감지
- `poll`: 파일 이벤트 리스너가 작동하지 않는 환경(예: NFS/EFS로 연결된 설정 파일)에서 사용

```bash
vector --config /etc/vector/vector.yaml --watch-config --watch-config-method poll
```

#### 폴링 간격 설정

`poll` 방법을 사용할 때 폴링 간격을 설정할 수 있습니다 (기본값: 30초):

```bash
vector --config /etc/vector/vector.yaml --watch-config --watch-config-method poll --watch-config-poll-interval-seconds 60
```

### 1.7 시그널 처리 및 정상 종료

Vector는 시그널을 받으면 정상적으로 종료됩니다.

#### 정상 종료 타임아웃

SIGINT 또는 SIGTERM을 수신한 후 정상 종료를 기다리는 시간(초)을 설정할 수 있습니다. 이 시간이 지나면 Vector는 강제 종료됩니다.

```bash
vector --config /etc/vector/vector.yaml --graceful-shutdown-limit-secs 120
```

#### 강제 종료 비활성화

강제 종료를 절대 하지 않으려면:

```bash
vector --config /etc/vector/vector.yaml --no-graceful-shutdown-limit
```

기본 타임아웃은 60초입니다.

### 1.8 데이터 디렉토리 (data_dir)

`data_dir`은 Vector 상태 데이터를 저장하는 디렉토리입니다. 디스크 버퍼, 파일 체크포인트 등이 저장됩니다.

#### 설정 예시 (YAML)

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

#### 설정 예시 (TOML)

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
- Vector는 이 디렉토리에 대한 쓰기 권한이 있어야 합니다
- 권한 오류를 방지하려면 Vector를 실행하는 사용자와 그룹으로 디렉토리의 소유자를 변경해야 합니다
- 디스크 버퍼의 최소 크기는 약 256MiB입니다

### 1.9 환경 변수

#### VECTOR_CONFIG

설정 파일 경로를 지정합니다:

```bash
export VECTOR_CONFIG=/etc/vector/vector.yaml
vector
```

#### VECTOR_CONFIG_DIR

설정 디렉토리를 지정합니다:

```bash
export VECTOR_CONFIG_DIR=/etc/vector/config.d/
vector
```

#### VECTOR_LOG

로그 레벨을 설정합니다:

```bash
export VECTOR_LOG=debug
vector --config /etc/vector/vector.yaml
```

#### 설정 파일 내 환경 변수 보간

설정 파일 내에서 환경 변수를 사용할 수 있습니다:

```yaml
sources:
  my_source:
    type: http_server
    address: "${HOST:-0.0.0.0}:${PORT:-8080}"
```

기본값을 지정하려면 `:-` 구문을 사용합니다.

#### 엄격한 환경 변수 모드

정의되지 않은 환경 변수가 있을 때 오류를 발생시키려면:

```bash
vector --config /etc/vector/vector.yaml --strict-env-vars
```

또는:

```bash
export VECTOR_STRICT_ENV_VARS=true
vector --config /etc/vector/vector.yaml
```

---

## 2. 모니터링 (Monitoring)

Vector는 관찰 가능성 데이터를 처리하는 도구이지만 자체적으로도 높은 관찰 가능성을 제공합니다. `internal_logs`와 `internal_metrics` 두 가지 소스를 사용하여 Vector가 생성하는 로그와 메트릭을 다른 소스의 데이터처럼 처리할 수 있습니다.

### 2.1 내부 로그 (Internal Logs)

#### 기본 설정 (YAML)

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

#### 기본 설정 (TOML)

```toml
[sources.vector_logs]
type = "internal_logs"

[sinks.console]
type = "console"
inputs = ["vector_logs"]
encoding.codec = "text"
```

#### 로그 레벨 설정

`internal_logs` 소스를 통해 전달되는 로그는 로그 레벨에 의해 결정됩니다. 기본값은 `info`입니다.

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

#### Splunk로 내부 로그 전송 (TOML)

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

### 2.2 내부 메트릭 (Internal Metrics)

#### 기본 설정 (YAML)

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

#### 기본 설정 (TOML)

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

#### Prometheus Remote Write로 메트릭 전송

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

#### 태그 설정

여러 Vector 인스턴스의 내부 메트릭을 동일한 목적지로 보낼 때 메트릭 시리즈가 충돌하지 않도록 고유한 태그를 추가해야 합니다:

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

### 2.3 통합 모니터링 설정 예시

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

### 2.4 이벤트 기반 관찰 가능성 전략

Vector는 일관되고 상관관계가 있는 텔레메트리 데이터를 보장하는 이벤트 기반 관찰 가능성 전략을 사용합니다.

핫 패스 로그 속도 제한:
Vector는 핫 패스에서 로그 이벤트를 속도 제한하여 IO를 포화시키거나 서비스를 방해하는 위험 없이 세밀한 인사이트를 얻을 수 있습니다.

VRL에서의 로깅:
VRL의 `log` 함수는 기본적으로 속도 제한을 구현하여 VRL 프로그램이 로그 메서드를 호출할 때 실수로 I/O를 포화시키지 않도록 합니다.

---

## 3. 검증 (Validating)

Vector는 설정 유효성을 검사하는 `validate` 서브커맨드를 제공합니다. 검증이 성공하면 Vector는 종료 코드 0으로 종료하고, 실패하면 종료 코드 78로 종료합니다.

### 3.1 기본 검증

```bash
vector validate /etc/vector/vector.yaml
```

### 3.2 검증 수행 내용

`validate` 서브커맨드는 지정된 설정에 대해 여러 검사를 수행합니다:

1. 구문 검사: 설정 파일의 구문이 올바른지 확인
2. 스키마 검사: 설정이 예상된 스키마와 일치하는지 확인
3. 토폴로지 검사: 컴포넌트 간의 연결이 유효한지 확인
4. 환경 검사: Vector가 설정된 토폴로지를 지원할 수 있는 환경에서 실행되고 있는지 확인
   - 모든 컴포넌트가 실행에 필요한 사전 요구 사항을 갖추고 있는지 확인 (예: 데이터 디렉토리 존재 및 쓰기 가능 여부)
   - 모든 싱크가 지정된 대상에 연결할 수 있는지 확인

### 3.3 환경 검사 비활성화

환경 검사를 비활성화하려면 `--no-environment` 플래그를 사용합니다:

```bash
vector validate --no-environment /etc/vector/vector.yaml
```

### 3.4 헬스체크 건너뛰기

로컬 워크스테이션처럼 헬스체크 엔드포인트에 접근할 수 없는 환경에서 설정을 검증하되 다른 환경 검사는 실행하려면 `--skip-healthchecks` 플래그를 사용합니다:

```bash
vector validate --skip-healthchecks /etc/vector/vector.yaml
```

참고: 설정된 `data_dir`은 여전히 쓰기 가능해야 합니다.

### 3.5 여러 설정 파일 검증

```bash
vector validate /etc/vector/*.yaml
```

### 3.6 설정 디렉토리 검증

```bash
vector validate --config-dir /etc/vector/config.d/
```

### 3.7 헬스체크 설정

특정 싱크에 대해 헬스체크를 비활성화하려면 설정에서 `healthcheck` 옵션을 `false`로 설정합니다:

```yaml
sinks:
  my_sink:
    type: http
    uri: "https://example.com"
    healthcheck:
      enabled: false
```

---

## 4. 최적화 (Optimization)

Vector는 Rust로 작성되어 런타임이나 가상 머신을 포함하지 않습니다. 따라서 성능을 향상시키기 위해 특별한 서비스 수준 조치를 취할 필요가 없습니다. Vector는 기본적으로 별도의 조정 없이 모든 시스템 리소스를 최대한 활용합니다.

### 4.1 Profile-Guided Optimization (PGO)

PGO(Profile-Guided Optimization)는 런타임 프로파일을 기반으로 프로그램을 최적화하는 컴파일러 최적화 기법입니다.

#### 성능 향상

테스트에 따르면 PGO는 일부 Vector 워크로드에서 초당 처리되는 로그 이벤트가 최대 15% 향상될 수 있습니다.

#### PGO 빌드 방법

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

#### 전체 빌드 프로세스

```bash
# 일반 릴리스 빌드
cargo build --release

# PGO 계측 빌드
cargo pgo build

# PGO 최적화 빌드
cargo pgo optimize build
```

더 자세한 가이드는 [Rust 문서](https://doc.rust-lang.org/rustc/profile-guided-optimization.html)를 참조하세요.

### 4.2 버퍼링 모델

Vector는 백프레셔와 데이터 손실을 관리하기 위해 버퍼링 모델을 사용합니다.

#### 메모리 버퍼

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

#### 디스크 버퍼

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

#### 버퍼 옵션

- `when_full: block` - 버퍼가 가득 차면 입력을 차단
- `when_full: drop_newest` - 버퍼가 가득 차면 최신 이벤트를 삭제

#### 디스크 버퍼 TOML 예시

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

### 4.3 동시성 모델

Vector는 적응형 동시성(Adaptive Concurrency)을 사용하여 다운스트림 서비스의 상태에 따라 자동으로 동시성을 조정합니다.

---

## 5. GraphQL API

Vector는 GraphQL API를 통해 자체 관찰 가능성을 제공합니다. 이 API는 v0.11.0에서 도입되었습니다.

### 5.1 API 활성화

설정 파일에서 API를 활성화합니다:

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

### 5.2 GraphQL Playground

API가 활성화되면 브라우저에서 GraphQL Playground에 접근할 수 있습니다:

```
http://localhost:8686/playground
```

### 5.3 API 엔드포인트

- GraphQL: `http://localhost:8686/graphql`
- Health: `http://localhost:8686/health`
- Playground: `http://localhost:8686/playground`

### 5.4 API 쿼리 예시

```bash
curl -X POST http://127.0.0.1:8686/graphql \
  -H "Content-Type: application/json" \
  -d '{"query": "{ health }"}'
```

#### 토폴로지 쿼리

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

### 5.5 Vector CLI 도구

Vector의 GraphQL API는 다음 CLI 도구에서 사용됩니다:

#### vector top

컴포넌트 메트릭과 토폴로지 정보를 대시보드 스타일로 표시합니다 (htop과 유사):

```bash
vector top
```

원격 인스턴스에 연결:

```bash
vector top --url http://remote-vector:8686/graphql
```

#### vector tap

파이프라인의 컴포넌트로 흐르는 이벤트를 실시간으로 관찰합니다:

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

#### vector graph

설정에 지정된 토폴로지를 DOT 형식의 그래프로 출력합니다:

```bash
vector graph --config /etc/vector/vector.yaml > topology.dot
```

GraphViz로 시각화:

```bash
vector graph --config /etc/vector/vector.yaml | dot -Tpng > topology.png
```

또는 [webgraphviz.com](http://www.webgraphviz.com/)에서 온라인으로 시각화할 수 있습니다.

### 5.6 GraphQL을 선택한 이유

- 타입 안전성: 클라이언트가 스키마를 내부 검사하여 쿼리 실행 전에 유효성 확인 가능
- 관계형 데이터 모델링: Vector 토폴로지와 같은 관계형 데이터를 자연스럽게 모델링하고 쿼리 가능
- 실시간 스트리밍: WebSocket을 통한 구독으로 고빈도 메트릭에 이상적인 데이터 스트리밍 가능

---

## 6. 단위 테스트 (Unit Testing)

Vector는 처리 파이프라인의 변환(transform)을 단위 테스트할 수 있습니다. 단위 테스트는 대부분의 프로그래밍 언어에서와 같이 작동합니다.

### 6.1 기본 개념

- 변환에 입력 세트를 제공합니다
- VRL 어설션을 사용하여 출력이 예상과 일치하는지 확인합니다
- `insert_at` 지시문은 테스트 입력을 대상 변환에 인위적으로 삽입합니다

### 6.2 테스트 실행

```bash
vector test /etc/vector/vector.yaml
```

### 6.3 YAML 테스트 예시

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

### 6.4 TOML 테스트 예시

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

### 6.5 VRL 어설션 함수

#### assert

Boolean 표현식을 첫 번째 인수로 받습니다. Boolean이 false로 해결되면 테스트가 실패합니다:

```vrl
assert!(1 == 1)
assert!(.field == "expected_value")
```

#### assert_eq

두 값을 인수로 받아 같은지 비교합니다:

```vrl
assert_eq!(.message, "Hello, World!")
assert_eq!(.count, 42)
```

권장사항: 단위 테스트에서 `assert`와 `assert_eq` 호출을 bang(!) 구문을 사용하여 실패 불가능하게 만드세요. 이렇게 하면 조건이 실패할 때 VRL 프로그램이 중단됩니다.

### 6.6 출력이 없는 테스트

필터 변환처럼 특정 조건에서 이벤트가 출력되지 않아야 하는 경우:

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

### 6.7 Raw 입력 테스트

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

### 6.8 모범 사례

1. 개별 파일로 분리: 대규모 컴포넌트 체인이 있는 경우 각각을 개별 파일로 분리하고 자체 단위 테스트를 포함
2. 컴포넌트 재사용: 여러 설정 파일에서 사용되는 컴포넌트는 전용 폴더에 정의
3. 변환 집중 테스트: 단위 테스트는 입력을 받아 출력을 반환하는 격리된 함수인 변환에 특히 적합

---

## 7. 업그레이드 (Upgrading)

### 7.1 일반 업그레이드 권장사항

Vector가 1.0 이전 버전인 동안에는 마이너 버전마다 호환되지 않는 변경 사항이 포함될 수 있으므로 마이너 버전을 순차적으로 업그레이드하는 것이 좋습니다. 이러한 변경 사항은 각 업그레이드 가이드에 명시되어 있습니다.

### 7.2 최신 업그레이드 가이드 하이라이트

#### 버전 0.34 주요 변경사항

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

#### 버전 0.33 주요 변경사항

- OpenSSL 버전이 v3.1.x로 업그레이드됨
- 레거시 OpenSSL 프로바이더가 기본적으로 비활성화됨

#### 버전 0.27 주요 변경사항

1. 디스크 버퍼 백업 권장
   - 이전 Vector 버전으로 롤백할 수 있으려면 디스크 버퍼를 백업해야 함
   - 새 디스크 버퍼 항목은 이전 Vector 버전에서 읽을 수 없을 수 있음

2. Vector 간 통신 업그레이드 순서
   - vector 소스/싱크를 사용한 Vector 간 통신 시
   - 먼저 컨슈머(수신자)를 업그레이드한 후 프로듀서(송신자)를 업그레이드

#### 버전 0.22 주요 변경사항

- 새로운 디스크 버퍼 구현(`disk_v2`)이 기본값(`disk`)으로 승격
- 기존 디스크 버퍼는 자동으로 마이그레이션됨
- 롤백을 위해 디스크 버퍼 백업 권장 (기본 위치: `/var/lib/vector`)

#### 버전 0.21 주요 변경사항

1. 경로 구문 통합
   - VRL 구문으로 마이그레이션 진행
   - 이전 경로 구문에서 VRL 구문으로 변경 필요할 수 있음

2. AWS SDK 마이그레이션
   - Rusoto에서 공식 AWS SDK로 마이그레이션
   - IMDSv2만 지원 (IMDSv1 사용 시 IMDSv2 허용 설정 필요)

### 7.3 업그레이드 절차 예시

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

### 7.4 릴리스 정보

전체 릴리스 노트와 업그레이드 가이드는 [Vector 릴리스 페이지](https://vector.dev/releases/)에서 확인할 수 있습니다.

---

## 참고 자료

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
