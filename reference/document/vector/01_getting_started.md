# Vector 시작하기 (Getting Started)

> 이 문서는 [Vector 공식 문서](https://vector.dev/docs/)의 Getting Started / Quickstart 섹션을 한국어로 번역한 것입니다.

---

## 목차

1. [Vector란 무엇인가?](#vector란-무엇인가)
2. [설치하기](#설치하기)
3. [첫 번째 파이프라인 만들기](#첫-번째-파이프라인-만들기)
4. [Transform 추가하기](#transform-추가하기)
5. [다음 단계](#다음-단계)

---

## Vector란 무엇인가?

Vector에 오신 것을 환영합니다! Vector는 고성능 엔드투엔드(에이전트 및 애그리게이터) 옵저버빌리티 데이터 파이프라인으로, 옵저버빌리티 데이터를 완벽하게 제어할 수 있게 해줍니다.

### 주요 특징

- 통합 데이터 처리: 모든 로그, 메트릭, 트레이스를 수집, 변환, 라우팅할 수 있습니다.
- 벤더 독립성: 현재 사용 중인 벤더뿐만 아니라 향후 사용할 수 있는 모든 벤더로 데이터를 전송할 수 있습니다.
- 비용 절감: 극적인 비용 절감을 가능하게 합니다.
- 데이터 보강: 새로운 데이터 보강 기능을 제공합니다.
- 데이터 보안: 벤더에 의존하지 않고 필요한 곳에서 데이터 보안을 보장합니다.
- 오픈 소스: 완전한 오픈 소스이며, 해당 분야의 다른 대안보다 최대 10배 빠릅니다.
- 단일 바이너리: 의존성 없이, 런타임 없이, 메모리 안전한 단일 바이너리로 패키징됩니다.
- Rust로 작성: Vector의 주요 설계 목표는 신뢰성입니다.

Vector는 Datadog의 커뮤니티 오픈소스 엔지니어링 팀에서 유지 관리하며, Datadog Observability Pipelines를 구동하는 오픈 소스 프로젝트입니다.

### 핵심 개념

Vector는 워크플로를 세 가지 주요 모듈로 추상화합니다:

| 컴포넌트 | 설명 |
|---------|------|
| Sources (소스) | 옵저버빌리티 데이터 소스에서 데이터를 수집하거나 수신합니다. |
| Transforms (변환) | 토폴로지를 통과하는 옵저버빌리티 데이터를 조작하거나 변경합니다. |
| Sinks (싱크) | Vector에서 외부 서비스나 목적지로 데이터를 전송합니다. |

Vector의 모든 옵저버빌리티 데이터는 Event(이벤트) 라는 단일 추상화로 통합되며, 이는 두 가지 주요 유형을 포함합니다:
- Metrics (메트릭)
- Logs (로그)

---

## 설치하기

Vector 설치는 빠르고 쉽습니다. 다양한 설치 방법 중에서 선택할 수 있습니다.

### 빠른 설치 (권장)

가장 빠른 설치 방법은 curl 스크립트를 사용하는 것입니다:

```bash
curl --proto '=https' --tlsv1.2 -sSfL https://sh.vector.dev | bash
```

특정 버전을 설치하려면:

```bash
curl --proto '=https' --tlsv1.2 -sSfL https://sh.vector.dev | VECTOR_VERSION=0.34.1 bash
```

`--prefix` 옵션을 사용하여 사용자 정의 설치 디렉토리를 지정할 수 있습니다. 이는 특히 Dockerfile과 같은 자동화된 환경에서 유용합니다.

### 설치 스크립트 보안 기능

설치 스크립트에는 다음과 같은 보안 기능이 포함되어 있습니다:

- TLS 검증: TLS 1.2 및 강력한 암호화 스위트 적용
- 아키텍처 검증: 시스템 아키텍처 자동 감지
- 체크섬 검증: HTTPS를 통한 체크섬 확인
- 명령 검증: 명령어 유효성 검사
- 오류 처리: 설명적 오류 메시지 제공

### 패키지 관리자를 통한 설치

Vector는 다양한 패키지 관리자를 지원합니다:

#### APT (Debian/Ubuntu)

APT는 Debian, Ubuntu 및 기타 Linux 배포판에서 사용할 수 있는 무료 패키지 관리자입니다. APT 리포지토리는 Datadog에서 제공합니다.

```bash
# Datadog APT 리포지토리 추가 및 Vector 설치
bash -c "$(curl -L https://s3.amazonaws.com/dd-agent/scripts/install_script_vector_deb.sh)"
```

#### Homebrew (macOS)

Homebrew는 Apple의 macOS용 무료 오픈 소스 패키지 관리 시스템입니다.

```bash
brew tap vectordotdev/brew
brew install vector
```

> 참고: Homebrew를 통한 설치는 macOS만 지원됩니다 (Linux는 지원되지 않음).

#### 기타 패키지 관리자

- dpkg: Debian 패키지 관리자
- Helm: Kubernetes 패키지 관리자
- MSI: Windows 설치 프로그램
- Nix: NixOS 패키지 관리자
- pacman: Arch Linux 패키지 관리자
- RPM/YUM: Red Hat 계열 Linux용

### Docker를 통한 설치

Docker는 애플리케이션 개발, 배포 및 실행을 위한 오픈 플랫폼입니다.

```bash
# Docker 이미지 풀
docker pull timberio/vector:latest-debian

# Vector 실행
docker run -i -v $(pwd)/:/etc/vector/ --rm timberio/vector:0.52.0-debian
```

Docker를 사용하여 Vector를 설치하는 경우, 튜토리얼 전체에서 명령을 실행할 때 별칭(alias)을 사용하는 것이 좋습니다:

```bash
alias vector='docker run -i -v $(pwd)/:/etc/vector/ --rm timberio/vector:latest-debian'
```

사용 가능한 Docker 이미지 배포판:
- `debian` (기본)
- `distroless-libc`
- `distroless-static`
- `alpine`

### 지원되는 운영 체제

Vector는 다음 운영 체제를 지원합니다:

- Amazon Linux
- Arch Linux
- CentOS
- Debian
- macOS
- NixOS
- Raspbian
- RHEL (Red Hat Enterprise Linux)
- Ubuntu
- Windows

### 소스에서 빌드

가능하다면 지원되는 플랫폼, 패키지 관리자 또는 사전 빌드된 아카이브를 통해 Vector를 설치하는 것을 권장합니다. 이러한 방법은 권한, 디렉토리 생성 및 기타 복잡한 사항을 처리해 줍니다.

Docker에서 `cross`를 사용하여 Linux용 정적 링크 바이너리를 빌드할 수 있습니다. 이 경우 Docker가 자동으로 의존성을 가져오므로 별도의 의존성이 필요하지 않습니다.

---

## 첫 번째 파이프라인 만들기

### Vector 토폴로지 이해하기

Vector 토폴로지는 실행할 컴포넌트와 상호 작용 방식을 정의하는 설정 파일을 사용하여 정의됩니다. Vector 토폴로지는 세 가지 유형의 컴포넌트로 구성됩니다:

1. Sources (소스): 옵저버빌리티 데이터 소스에서 Vector로 데이터를 수집하거나 수신합니다.
2. Transforms (변환): 토폴로지를 통과하는 옵저버빌리티 데이터를 조작하거나 변경합니다.
3. Sinks (싱크): Vector에서 외부 서비스나 목적지로 데이터를 전송합니다.

### 설정 파일 위치

Vector 설정 파일의 위치는 설치 방법에 따라 다릅니다. 대부분의 Linux 기반 시스템에서는 `/etc/vector/vector.yaml`에 위치합니다.

Vector는 설정 파일 형식으로 YAML, TOML, JSON을 지원합니다.

### 기본 파이프라인 예제: stdin에서 console로

가장 간단한 파이프라인을 만들어 보겠습니다. `vector.yaml`이라는 설정 파일을 생성합니다:

```yaml
sources:
  in:
    type: "stdin"

sinks:
  out:
    inputs:
      - "in"
    type: "console"
    encoding:
      codec: "text"
```

#### 설정 설명

첫 번째 컴포넌트 (`sources.in`):
- `stdin` 소스를 사용합니다.
- Vector가 stdin을 통해 데이터를 수신하도록 합니다.
- ID는 `in`으로 지정됩니다.

두 번째 컴포넌트 (`sinks.out`):
- `console` 싱크를 사용합니다.
- Vector가 데이터를 stdout으로 출력하도록 합니다.
- `encoding.codec` 옵션을 `text`로 설정하여 데이터를 일반 텍스트로 출력합니다 (인코딩되지 않음).

`inputs` 옵션:
- `sinks.out` 컴포넌트의 `inputs` 옵션은 이 싱크의 이벤트가 어디에서 오는지를 Vector에 알려줍니다.
- 이 경우 ID가 `in`인 소스에서 이벤트를 수신합니다.

### Vector 실행하기

```bash
vector --config vector.yaml
```

이벤트를 파이프로 전달해 보겠습니다:

```bash
echo "Hello World" | vector --config vector.yaml
```

#### 실행 결과

- `echo` 문은 stdin을 통해 Vector로 단일 로그를 보냅니다.
- `vector` 명령은 이전에 생성한 설정 파일로 Vector를 시작합니다.
- 전송된 이벤트는 `sources.in` 컴포넌트에서 수신된 후 `sinks.out` 컴포넌트로 전송되어 콘솔에 다시 출력됩니다.

### stdin 소스 출력 이해하기

기본적으로 stdin 소스는 이벤트에 유용한 컨텍스트 키를 추가합니다. 각 줄은 새 줄 구분자(0xA 바이트)가 발견될 때까지 읽힙니다.

stdin에 입력을 입력하면 다음과 같은 메타데이터를 포함하는 JSON 이벤트를 얻게 됩니다:
- 호스트명
- 타임스탬프
- 소스 정보
- 메시지

이것은 `log` 유형의 이벤트입니다.

### JSON 인코딩으로 변경하기 (선택 사항)

싱크 설정에서 `encoding.codec = "json"`으로 설정해 보세요:

```yaml
sources:
  in:
    type: "stdin"

sinks:
  out:
    inputs:
      - "in"
    type: "console"
    encoding:
      codec: "json"
```

---

## Transform 추가하기

### Remap Transform 소개

`remap` 변환은 Vector가 옵저버빌리티 데이터 처리에서 강력한 이유의 핵심입니다. 이 변환은 Vector Remap Language (VRL)라는 간단한 언어를 제공하여 Vector를 통과하는 이벤트 데이터를 파싱, 조작 및 장식할 수 있게 해줍니다.

VRL을 사용하면 정적 이벤트를 환경 상태에 대한 질문을 할 수 있는 정보성 데이터로 변환할 수 있습니다.

### VRL의 특징

- 표현식 지향 언어: 옵저버빌리티 데이터(로그 및 메트릭)를 안전하고 성능 좋게 처리하도록 설계되었습니다.
- 간단한 구문: 배우기 쉬운 간단한 구문을 제공합니다.
- 풍부한 내장 함수: 옵저버빌리티 사용 사례에 맞춤화된 풍부한 내장 함수 세트를 제공합니다.
- 안전성과 성능: 유연성을 희생하지 않으면서 안전성과 성능을 목표로 합니다.
- 강력한 오류 처리: 변환 프로세스의 안정성을 보장하고, 예상치 못한 데이터 이상으로 인한 작업 실패나 데이터 손실을 방지합니다.

### 완전한 파이프라인 예제: Syslog 파싱

다음은 세 가지 컴포넌트 유형(소스, 변환, 싱크)이 모두 포함된 업데이트된 `vector.yaml` 설정 파일입니다:

```yaml
sources:
  generate_syslog:
    type: "demo_logs"
    format: "syslog"
    count: 100

transforms:
  remap_syslog:
    inputs:
      - "generate_syslog"
    type: "remap"
    source: |
      structured = parse_syslog!(.message)
      . = merge(., structured)

sinks:
  emit_syslog:
    inputs:
      - "remap_syslog"
    type: "console"
    encoding:
      codec: "json"
```

#### 설정 설명

1. Source: `demo_logs`

```yaml
sources:
  generate_syslog:
    type: "demo_logs"
    format: "syslog"
    count: 100
```

- `demo_logs` 소스는 다양한 형식의 다양한 유형의 이벤트를 시뮬레이션할 수 있는 샘플 로그 데이터를 생성합니다.
- `format` 옵션은 demo_logs 소스가 어떤 유형의 로그를 생성할지 지정합니다 (여기서는 syslog).
- `count` 옵션은 demo_logs 소스가 몇 줄을 생성할지 지정합니다 (여기서는 100줄).

2. Transform: `remap`

```yaml
transforms:
  remap_syslog:
    inputs:
      - "generate_syslog"
    type: "remap"
    source: |
      structured = parse_syslog!(.message)
      . = merge(., structured)
```

- `inputs` 옵션이 `generate_syslog`로 설정되어 `generate_syslog` 소스에서 이벤트를 수신합니다.
- 변환 유형은 `remap`입니다.
- `source` 옵션 안에는 Vector가 수신하는 각 이벤트에 적용할 리매핑 변환 목록이 있습니다.

`parse_syslog!` 함수:
- `message`라는 단일 필드를 전달받습니다. 이 필드에는 생성 중인 Syslog 이벤트가 포함되어 있습니다.
- 이 올인원 함수는 Syslog 형식의 메시지를 받아 내용을 파싱하고 구조화된 이벤트로 내보냅니다.
- `parse_syslog` 함수 뒤의 `!`는 메시지 파싱에 실패하면 Vector가 오류를 발생시키도록 합니다. 이를 통해 비표준 Syslog가 수신되면 알 수 있고, 리매핑을 조정하여 처리할 수 있습니다!

Remap은 복잡한 Syslog 파싱 정규 표현식의 필요성을 제거하고 이벤트의 값에 집중할 수 있게 해줍니다. 값을 추출하는 방법이 아니라 값 자체에 집중할 수 있습니다.

3. Sink: `console` (JSON)

```yaml
sinks:
  emit_syslog:
    inputs:
      - "remap_syslog"
    type: "console"
    encoding:
      codec: "json"
```

- `inputs` 옵션이 `remap_syslog` 변환에서 생성된 이벤트를 처리하도록 업데이트되었습니다.
- 이벤트는 JSON 형식으로 출력됩니다.

### 실행하기

이번에는 데이터를 echo할 필요가 없습니다. 명령줄에서 그냥 실행하면 됩니다:

```bash
vector --config vector.yaml
```

Vector는:
1. 100줄의 생성된 Syslog 데이터를 처리합니다.
2. 처리된 데이터를 JSON으로 출력합니다.
3. 종료합니다.

Vector는 Syslog 메시지를 파싱하고 모든 Syslog 필드를 포함하는 구조화된 이벤트를 생성합니다. 이 모든 것이 Vector의 remap 언어 한 줄로 가능합니다.

### 지원되는 로그 형식

Vector는 다양한 로깅 형식의 파싱을 지원합니다:
- Syslog
- Apache (access 및 error 로그)
- 그 외 다양한 형식

지원되지 않는 이벤트 형식이 있다면, remap을 사용하여 자체 사용자 정의 정규 표현식을 지정할 수도 있습니다.

### 추가 Transform 예제

#### 기본 Transform 파이프라인

```yaml
sources:
  stdin:
    type: "stdin"

transforms:
  our_example:
    inputs: ["stdin"]
    type: remap
    source: ""

sinks:
  stdout:
    type: "console"
    inputs: ["our_example"]
    encoding:
      codec: "json"
```

이것은 매우 간단한 DAG(Directed Acyclic Graph)를 생성합니다: stdin -> our_example -> stdout

VRL 프로그램은 절대 단독으로 실행되지 않습니다. 항상 소스에서 오는 이벤트가 필요합니다.

#### 조건부 필터링 예제

VRL을 사용하여 조건을 지정할 수 있으며, 이벤트를 단일 Boolean 표현식으로 변환합니다:

```yaml
transforms:
  filter_logs:
    type: "filter"
    inputs:
      - "source_id"
    condition: '.severity != "info" && .status_code < 400 && exists(.host)'
```

이 조건은 다음을 필터링합니다:
- `severity` 필드가 "info"인 이벤트
- `status_code` 필드가 400 이상인 이벤트
- `host` 필드가 설정되지 않은 이벤트

---

## 프로덕션 설정 예제

### 파일에서 Elasticsearch로

다음은 프로덕션 환경에서 사용할 수 있는 더 완전한 예제입니다:

```yaml
# 전역 옵션 설정
data_dir: "/var/lib/vector"

# Vector API (기본적으로 비활성화)
api:
  enabled: false

# 하나 이상의 파일을 테일링하여 데이터 수집
sources:
  apache_logs:
    type: "file"
    include:
      - "/var/log/apache2/*.log"
    ignore_older_secs: 86400

# Vector의 Remap Language를 통한 구조화 및 파싱
transforms:
  apache_parser:
    inputs:
      - "apache_logs"
    type: "remap"
    source: ". = parse_apache_log(.message)"

  # 비용 절감을 위한 데이터 샘플링
  apache_sampler:
    inputs:
      - "apache_parser"
    type: "sample"
    rate: 2

# 구조화된 데이터를 단기 스토리지로 전송
sinks:
  es_cluster:
    inputs:
      - "apache_sampler"
    type: "elasticsearch"
    endpoints:
      - "http://79.12.221.222:9200"
    bulk:
      index: "vector-%Y-%m-%d"
```

### Kafka에서 Elasticsearch로

```yaml
sources:
  kafka_in:
    type: "kafka"
    bootstrap_servers: "10.14.22.123:9092,10.14.23.332:9092"
    group_id: "vector-logs"
    key_field: "message"
    topics: ["logs-*"]

transforms:
  json_parse:
    type: "remap"
    inputs: ["kafka_in"]
    source: |
      parsed, err = parse_json(.message)
      if err != null {
        log(err, level: "error")
      }
      . |= object(parsed) ?? {}

sinks:
  elasticsearch_out:
    type: "elasticsearch"
    inputs: ["json_parse"]
    endpoint: "http://10.24.32.122:9000"
    index: "logs-via-kafka"
```

---

## 설정 검증하기

설정을 검증하려면 다음 명령을 사용합니다:

```bash
# 환경 검사와 함께 검증
vector validate /etc/vector/vector.yaml

# 환경 검사 없이 검증
vector validate --no-environment /etc/vector/vector.yaml

# 헬스 체크 건너뛰기
vector validate --skip-healthchecks /etc/vector/vector.yaml
```

---

## VRL 프로그램 실행하기

`vector vrl` 서브커맨드를 사용하여 VRL 예제를 실행할 수 있습니다:

```bash
vector vrl --input input.json --program program.vrl --print-object
```

- `--input`: 입력은 이벤트 목록을 나타내는 줄바꿈으로 구분된 JSON 파일입니다.
- `--program`: VRL 프로그램 파일
- `--print-object`: 수정된 객체를 표시합니다.

### REPL (Read-Eval-Print Loop)

인수 없이 `vector vrl`을 실행하면 REPL이 시작됩니다:

```bash
vector vrl
```

Vector가 설치되어 있다면 이 명령으로 REPL을 시작할 수 있습니다.

---

## 배포 역할

Vector를 다양한 역할로 배포하여 사용 사례에 맞출 수 있습니다. 도구를 패치하지 않고도 A 지점에서 B 지점으로 데이터를 전달할 수 있습니다.

### 역할 유형

| 역할 | 설명 |
|------|------|
| Agent (에이전트) | 엣지에 Vector를 배포하도록 설계되었으며, 일반적으로 데이터 수집용입니다. 데몬 또는 사이드카로 배포할 수 있습니다. |
| Aggregator (애그리게이터) | 여러 업스트림 소스에서 데이터를 수집하고 처리하도록 설계되었습니다. 이러한 업스트림 소스는 다른 Vector 에이전트이거나 Syslog-ng와 같은 비Vector 에이전트일 수 있습니다. |

---

## 다음 단계

학습을 계속하려면 다음 리소스를 탐색하세요:

### 컴포넌트 참조

- [Sources (소스)](https://vector.dev/docs/reference/configuration/sources/): 사용 가능한 모든 소스 탐색
  - AMQP, Apache Metrics, AWS ECS metrics, AWS Kinesis Firehose, AWS S3, AWS SQS
  - Datadog agent, Demo Logs, dnstap, Docker logs
  - EventStoreDB metrics, Exec, File, Fluent
  - GCP PubSub, Heroku Logplex, Host metrics
  - HTTP Client, HTTP Server, Internal logs, Internal metrics
  - JournalD, Kafka, Kubernetes logs, Logstash
  - MongoDB metrics, MQTT, NATS, NGINX metrics
  - Okta, OpenTelemetry, PostgreSQL metrics, Prometheus
  - Redis, Socket, Splunk HEC, Static metrics
  - StatsD, stdin, Syslog, Vector, WebSocket

- [Transforms (변환)](https://vector.dev/docs/reference/configuration/transforms/): 사용 가능한 모든 변환 탐색
  - Remap (VRL 사용)
  - Filter
  - Sample
  - Aggregate
  - 등 다양한 변환

- [Sinks (싱크)](https://vector.dev/docs/reference/configuration/sinks/): 사용 가능한 모든 싱크 탐색

### Vector Remap Language (VRL)

VRL은 Vector에서 데이터 처리의 핵심입니다:

- [VRL 참조](https://vector.dev/docs/reference/vrl/): VRL 문법 및 개념
- [VRL 함수 참조](https://vector.dev/docs/reference/vrl/functions/): 사용 가능한 모든 함수
- [VRL 예제](https://vector.dev/docs/reference/vrl/examples/): 실용적인 예제
- [VRL 플레이그라운드](https://playground.vrl.dev/): 브라우저에서 VRL 테스트

### 배포

- [배포 가이드](https://vector.dev/docs/setup/deployment/): 프로덕션 환경에서 Vector 실행
- [아키텍처 설계](https://vector.dev/docs/setup/going-to-prod/architecting/): 배포 아키텍처 설계

### 추가 리소스

- [설정 참조](https://vector.dev/docs/reference/configuration/): 상세한 설정 옵션
- [CLI 참조](https://vector.dev/docs/reference/cli/): Vector 명령줄 인터페이스
- [GitHub 리포지토리](https://github.com/vectordotdev/vector): 소스 코드 및 이슈

---

## 참고 자료

- [Vector 공식 문서](https://vector.dev/docs/)
- [Vector Quickstart](https://vector.dev/docs/setup/quickstart/)
- [Getting Started with Vector](https://vector.dev/guides/getting-started/getting-started/)
- [Vector 설치 가이드](https://vector.dev/docs/setup/installation/)
- [Vector Remap Language (VRL)](https://vector.dev/docs/reference/vrl/)
- [Vector GitHub](https://github.com/vectordotdev/vector)
