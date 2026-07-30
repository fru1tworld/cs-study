# Vector 시작하기와 설치

## Vector 시작하기 (Getting Started)

> 원본 참고: [Vector 공식 문서](https://vector.dev/docs/)

---

### 목차

1. [Vector란 무엇인가?](#vector란-무엇인가)
2. [설치하기](#설치하기)
3. [첫 번째 파이프라인 만들기](#첫-번째-파이프라인-만들기)
4. [Transform 추가하기](#transform-추가하기)
5. [다음 단계](#다음-단계)

---

### Vector란 무엇인가?

Vector는 고성능 엔드투엔드(에이전트 및 애그리게이터) 옵저버빌리티 데이터 파이프라인으로, 옵저버빌리티 데이터를 완전히 통제할 수 있게 해줍니다.

#### 주요 특징

- 통합 데이터 처리: 모든 로그, 메트릭, 트레이스를 수집, 변환, 라우팅할 수 있습니다.
- 벤더 독립성: 현재 사용 중인 벤더뿐만 아니라 향후 사용할 수 있는 모든 벤더로 데이터를 전송할 수 있습니다.
- 비용 절감: 대폭적인 비용 절감이 가능합니다.
- 데이터 보강: 다양한 데이터 보강 기능을 제공합니다.
- 데이터 보안: 벤더에 종속되지 않고 필요한 위치에서 데이터 보안을 보장합니다.
- 오픈 소스: 완전한 오픈 소스이며, 해당 분야의 다른 대안보다 최대 10배 빠릅니다.
- 단일 바이너리: 의존성 없이, 런타임 없이, 메모리 안전한 단일 바이너리로 패키징됩니다.
- Rust로 작성: Vector의 주요 설계 목표는 신뢰성입니다.

Vector는 Datadog의 커뮤니티 오픈소스 엔지니어링 팀에서 유지 관리하며, Datadog Observability Pipelines를 구동하는 오픈 소스 프로젝트입니다.

#### 핵심 개념

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

### 설치하기

Vector 설치는 빠르고 쉽습니다. 다양한 설치 방법 중에서 선택할 수 있습니다.

#### 빠른 설치 (권장)

가장 빠른 설치 방법은 curl 스크립트를 사용하는 것입니다:

```bash
curl --proto '=https' --tlsv1.2 -sSfL https://sh.vector.dev | bash
```

특정 버전을 설치하려면:

```bash
curl --proto '=https' --tlsv1.2 -sSfL https://sh.vector.dev | VECTOR_VERSION=0.34.1 bash
```

`--prefix` 옵션을 사용하여 사용자 정의 설치 디렉토리를 지정할 수 있습니다. 이는 특히 Dockerfile과 같은 자동화된 환경에서 유용합니다.

#### 설치 스크립트 보안 기능

설치 스크립트에는 다음과 같은 보안 기능이 포함되어 있습니다:

- TLS 검증: TLS 1.2 및 강력한 암호화 스위트 적용
- 아키텍처 검증: 시스템 아키텍처 자동 감지
- 체크섬 검증: HTTPS를 통한 체크섬 확인
- 명령 검증: 명령어 유효성 검사
- 오류 처리: 설명적 오류 메시지 제공

#### 패키지 관리자를 통한 설치

Vector는 다양한 패키지 관리자를 지원합니다:

##### APT (Debian/Ubuntu)

APT는 Debian, Ubuntu 및 기타 Linux 배포판에서 사용할 수 있는 무료 패키지 관리자입니다. APT 리포지토리는 Datadog에서 제공합니다.

```bash
# Datadog APT 리포지토리 추가 및 Vector 설치
bash -c "$(curl -L https://s3.amazonaws.com/dd-agent/scripts/install_script_vector_deb.sh)"
```

##### Homebrew (macOS)

Homebrew는 Apple의 macOS용 무료 오픈 소스 패키지 관리 시스템입니다.

```bash
brew tap vectordotdev/brew
brew install vector
```

> 참고: Homebrew를 통한 설치는 macOS만 지원됩니다 (Linux는 지원되지 않음).

##### 기타 패키지 관리자

- dpkg: Debian 패키지 관리자
- Helm: Kubernetes 패키지 관리자
- MSI: Windows 설치 프로그램
- Nix: NixOS 패키지 관리자
- pacman: Arch Linux 패키지 관리자
- RPM/YUM: Red Hat 계열 Linux용

#### Docker를 통한 설치

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

#### 지원되는 운영 체제

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

#### 소스에서 빌드

가능하다면 지원되는 플랫폼, 패키지 관리자 또는 사전 빌드된 아카이브를 통해 Vector를 설치하는 것을 권장합니다. 이러한 방법은 권한, 디렉토리 생성 및 기타 복잡한 사항을 처리해 줍니다.

Docker에서 `cross`를 사용하여 Linux용 정적 링크 바이너리를 빌드할 수 있습니다. 이 경우 Docker가 자동으로 의존성을 가져오므로 별도의 의존성이 필요하지 않습니다.

---

### 첫 번째 파이프라인 만들기

#### Vector 토폴로지 이해하기

Vector 토폴로지는 실행할 컴포넌트와 상호 작용 방식을 정의하는 설정 파일을 사용하여 정의됩니다. Vector 토폴로지는 세 가지 유형의 컴포넌트로 구성됩니다:

1. Sources (소스): 옵저버빌리티 데이터 소스에서 Vector로 데이터를 수집하거나 수신합니다.
2. Transforms (변환): 토폴로지를 통과하는 옵저버빌리티 데이터를 조작하거나 변경합니다.
3. Sinks (싱크): Vector에서 외부 서비스나 목적지로 데이터를 전송합니다.

#### 설정 파일 위치

Vector 설정 파일의 위치는 설치 방법에 따라 다릅니다. 대부분의 Linux 기반 시스템에서는 `/etc/vector/vector.yaml`에 위치합니다.

Vector는 YAML, TOML, JSON 형식의 설정 파일을 지원합니다.

#### 기본 파이프라인 예제: stdin에서 console로

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

##### 설정 설명

첫 번째 컴포넌트 (`sources.in`):
- `stdin` 소스를 사용합니다.
- Vector가 stdin을 통해 데이터를 수신하도록 합니다.
- ID는 `in`으로 지정됩니다.

두 번째 컴포넌트 (`sinks.out`):
- `console` 싱크를 사용합니다.
- Vector가 데이터를 stdout으로 출력합니다.
- `encoding.codec`을 `text`로 설정하여 데이터를 일반 텍스트로 출력합니다.

`inputs` 옵션:
- `sinks.out`의 `inputs` 옵션은 이 싱크가 이벤트를 어디서 받는지 Vector에 알려줍니다.
- 여기서는 ID가 `in`인 소스에서 이벤트를 수신합니다.

#### Vector 실행하기

```bash
vector --config vector.yaml
```

이벤트를 파이프로 전달해 보겠습니다:

```bash
echo "Hello World" | vector --config vector.yaml
```

##### 실행 결과

- `echo` 명령이 stdin을 통해 Vector로 단일 로그를 전달합니다.
- `vector` 명령은 앞서 작성한 설정 파일로 Vector를 시작합니다.
- 전달된 이벤트는 `sources.in`에서 수신된 후 `sinks.out`으로 전송되어 콘솔에 출력됩니다.

#### stdin 소스 출력 이해하기

기본적으로 stdin 소스는 이벤트에 유용한 컨텍스트 키를 추가합니다. 각 줄은 줄바꿈 문자(0xA 바이트)를 만날 때까지 읽힙니다.

stdin에 데이터를 입력하면 다음과 같은 메타데이터를 포함한 JSON 이벤트가 생성됩니다:
- 호스트명
- 타임스탬프
- 소스 정보
- 메시지

이것은 `log` 유형의 이벤트입니다.

#### JSON 인코딩으로 변경하기 (선택 사항)

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

### Transform 추가하기

#### Remap Transform 소개

`remap` 변환은 Vector가 옵저버빌리티 데이터 처리에서 강력한 핵심 기능입니다. Vector Remap Language (VRL)라는 간결한 언어를 제공하여 Vector를 통과하는 이벤트 데이터를 파싱·조작·보강할 수 있게 해줍니다.

VRL을 사용하면 정적인 이벤트를 환경 상태를 파악할 수 있는 풍부한 정보성 데이터로 변환할 수 있습니다.

#### VRL의 특징

- 표현식 지향 언어: 옵저버빌리티 데이터(로그 및 메트릭)를 안전하고 성능 좋게 처리하도록 설계되었습니다.
- 간단한 구문: 배우기 쉬운 간단한 구문을 제공합니다.
- 풍부한 내장 함수: 옵저버빌리티 사용 사례에 맞춤화된 풍부한 내장 함수 세트를 제공합니다.
- 안전성과 성능: 유연성을 희생하지 않으면서 안전성과 성능을 목표로 합니다.
- 강력한 오류 처리: 변환 프로세스의 안정성을 보장하고, 예상치 못한 데이터 이상으로 인한 작업 실패나 데이터 손실을 방지합니다.

#### 완전한 파이프라인 예제: Syslog 파싱

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

##### 설정 설명

1. Source: `demo_logs`

```yaml
sources:
  generate_syslog:
    type: "demo_logs"
    format: "syslog"
    count: 100
```

- `demo_logs` 소스는 다양한 형식의 샘플 로그 이벤트를 생성합니다.
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
- `source` 옵션에는 Vector가 수신하는 각 이벤트에 적용할 리매핑 변환 내용이 담깁니다.

`parse_syslog!` 함수:
- `message` 필드 하나를 인수로 받으며, 이 필드에는 생성된 Syslog 이벤트가 담겨 있습니다.
- Syslog 형식의 메시지를 파싱하여 구조화된 이벤트로 반환합니다.
- 함수 뒤의 `!`는 파싱에 실패했을 때 Vector가 오류를 발생시키도록 지정합니다. 이를 통해 비표준 Syslog가 유입될 경우 이를 인지하고 리매핑을 수정할 수 있습니다.

Remap은 복잡한 Syslog 파싱 정규식 없이도 이벤트의 값 자체에 집중할 수 있게 해줍니다.

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

- `inputs`가 `remap_syslog` 변환의 출력을 받도록 설정되어 있습니다.
- 이벤트는 JSON 형식으로 출력됩니다.

#### 실행하기

이번에는 데이터를 별도로 echo할 필요가 없습니다. 바로 실행하면 됩니다:

```bash
vector --config vector.yaml
```

Vector는:
1. 100줄의 생성된 Syslog 데이터를 처리합니다.
2. 처리된 데이터를 JSON으로 출력합니다.
3. 종료합니다.

Vector는 Syslog 메시지를 파싱하여 모든 Syslog 필드를 포함하는 구조화된 이벤트를 생성합니다. VRL 단 두 줄만으로 가능합니다.

#### 지원되는 로그 형식

Vector는 다양한 로깅 형식의 파싱을 지원합니다:
- Syslog
- Apache (access 및 error 로그)
- 그 외 다양한 형식

지원되지 않는 이벤트 형식의 경우, remap을 사용해 직접 정규 표현식을 작성하여 처리할 수 있습니다.

#### 추가 Transform 예제

##### 기본 Transform 파이프라인

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

VRL 프로그램은 단독으로 실행되지 않으며, 항상 소스에서 이벤트를 받아야 합니다.

##### 조건부 필터링 예제

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

### 프로덕션 설정 예제

#### 파일에서 Elasticsearch로

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

#### Kafka에서 Elasticsearch로

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

### 설정 검증하기

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

### VRL 프로그램 실행하기

`vector vrl` 서브커맨드를 사용하여 VRL 예제를 실행할 수 있습니다:

```bash
vector vrl --input input.json --program program.vrl --print-object
```

- `--input`: 입력은 이벤트 목록을 나타내는 줄바꿈으로 구분된 JSON 파일입니다.
- `--program`: VRL 프로그램 파일
- `--print-object`: 수정된 객체를 표시합니다.

#### REPL (Read-Eval-Print Loop)

인수 없이 `vector vrl`을 실행하면 REPL이 시작됩니다:

```bash
vector vrl
```

Vector가 설치되어 있다면 이 명령으로 REPL을 시작할 수 있습니다.

---

### 배포 역할

Vector는 다양한 역할로 배포하여 사용 사례에 맞게 구성할 수 있습니다. 기존 도구를 수정하지 않고도 A 지점에서 B 지점으로 데이터를 전달할 수 있습니다.

#### 역할 유형

| 역할 | 설명 |
|------|------|
| Agent (에이전트) | 엣지에 Vector를 배포하도록 설계되었으며, 일반적으로 데이터 수집용입니다. 데몬 또는 사이드카로 배포할 수 있습니다. |
| Aggregator (애그리게이터) | 여러 업스트림 소스에서 데이터를 수집하고 처리하도록 설계되었습니다. 이러한 업스트림 소스는 다른 Vector 에이전트이거나 Syslog-ng와 같은 비Vector 에이전트일 수 있습니다. |

---

### 다음 단계

더 깊이 학습하려면 다음 리소스를 참고하세요:

#### 컴포넌트 참조

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

#### Vector Remap Language (VRL)

VRL은 Vector에서 데이터 처리의 핵심입니다:

- [VRL 참조](https://vector.dev/docs/reference/vrl/): VRL 문법 및 개념
- [VRL 함수 참조](https://vector.dev/docs/reference/vrl/functions/): 사용 가능한 모든 함수
- [VRL 예제](https://vector.dev/docs/reference/vrl/examples/): 실용적인 예제
- [VRL 플레이그라운드](https://playground.vrl.dev/): 브라우저에서 VRL 테스트

#### 배포

- [배포 가이드](https://vector.dev/docs/setup/deployment/): 프로덕션 환경에서 Vector 실행
- [아키텍처 설계](https://vector.dev/docs/setup/going-to-prod/architecting/): 배포 아키텍처 설계

#### 추가 리소스

- [설정 참조](https://vector.dev/docs/reference/configuration/): 상세한 설정 옵션
- [CLI 참조](https://vector.dev/docs/reference/cli/): Vector 명령줄 인터페이스
- [GitHub 리포지토리](https://github.com/vectordotdev/vector): 소스 코드 및 이슈

---

### 참고 자료

- [Vector 공식 문서](https://vector.dev/docs/)
- [Vector Quickstart](https://vector.dev/docs/setup/quickstart/)
- [Getting Started with Vector](https://vector.dev/guides/getting-started/getting-started/)
- [Vector 설치 가이드](https://vector.dev/docs/setup/installation/)
- [Vector Remap Language (VRL)](https://vector.dev/docs/reference/vrl/)
- [Vector GitHub](https://github.com/vectordotdev/vector)

---

## Vector 설치 가이드

원본 문서: https://vector.dev/docs/setup/installation/

---

### 목차

1. [개요](#개요)
2. [빠른 설치](#빠른-설치)
3. [패키지 관리자를 통한 설치](#패키지-관리자를-통한-설치)
   - [APT](#apt)
   - [dpkg](#dpkg)
   - [Helm](#helm)
   - [Homebrew](#homebrew)
   - [MSI](#msi)
   - [Nix](#nix)
   - [pacman](#pacman)
   - [RPM](#rpm)
   - [YUM](#yum)
4. [플랫폼별 설치](#플랫폼별-설치)
   - [Docker](#docker)
   - [Kubernetes](#kubernetes)
5. [운영체제별 설치](#운영체제별-설치)
   - [Amazon Linux](#amazon-linux)
   - [Arch Linux](#arch-linux)
   - [CentOS](#centos)
   - [Debian](#debian)
   - [Fedora](#fedora)
   - [macOS](#macos)
   - [NixOS](#nixos)
   - [Raspbian](#raspbian)
   - [RHEL](#rhel)
   - [Ubuntu](#ubuntu)
   - [Windows](#windows)
6. [수동 설치](#수동-설치)
   - [Vector 설치 스크립트](#vector-설치-스크립트)
   - [아카이브에서 설치](#아카이브에서-설치)
   - [소스에서 빌드](#소스에서-빌드)
7. [설치 후 설정](#설치-후-설정)
8. [참고 사항](#참고-사항)

---

### 개요

Vector는 관측성(observability) 파이프라인을 구축하기 위한 경량의 초고속 도구입니다. 다양한 패키지 관리자와 여러 운영체제 및 플랫폼에서 Vector를 설치할 수 있습니다. 사용자 정의 빌드가 필요한 경우 수동으로 설치할 수도 있습니다.

#### 지원되는 설치 방법

- 패키지 관리자: APT, dpkg, Helm, Homebrew, MSI, Nix, pacman, RPM, YUM
- 플랫폼: Docker, Kubernetes
- 운영체제: Amazon Linux, Arch Linux, CentOS, Debian, Fedora, macOS, NixOS, Raspbian, RHEL, Ubuntu, Windows

#### 시스템 요구 사항

Vector는 단일 정적 바이너리로 컴파일되어 설치가 매우 간단합니다. *nix 시스템에서 Vector의 유일한 의존성은 libc이며, 이는 일반적으로 운영체제에서 제공됩니다.

Vector는 musl을 사용해 libc 구현을 정적으로 링크한 아티팩트도 릴리스합니다. 이를 통해 의존성이 없는 정적 바이너리를 생성할 수 있으며, 내장 libc 구현을 제공하지 않는 최소화된 환경에서 유용합니다.

> 참고: musl은 Vector가 여러 스레드에서 실행될 때 glibc보다 성능이 상당히 떨어집니다. 단일 CPU에서 Vector를 실행하지 않는 한, 가능하면 glibc를 사용하는 것이 좋습니다.

---

### 빠른 설치

Vector를 설치하는 가장 간단한 방법은 공식 설치 스크립트를 사용하는 것입니다:

```bash
curl --proto '=https' --tlsv1.2 -sSfL https://sh.vector.dev | bash
```

특정 버전을 설치하려면 `VECTOR_VERSION` 환경 변수를 사용하세요:

```bash
curl --proto '=https' --tlsv1.2 -sSfL https://sh.vector.dev | VECTOR_VERSION=0.52.0 bash
```

---

### 패키지 관리자를 통한 설치

#### APT

APT(Advanced Package Tool)는 Debian, Ubuntu 및 기타 Linux 배포판에서 소프트웨어 설치 및 제거를 처리하는 무료 패키지 관리자입니다.

> 참고: APT 저장소는 Datadog에서 제공합니다.

##### 설치

1단계: Vector 저장소 추가

```bash
bash -c "$(curl -L https://setup.vector.dev)"
```

2단계: Vector 설치

```bash
sudo apt-get install vector
```

##### 기존 저장소에서 마이그레이션

기존 저장소(repositories.timber.io)에서 마이그레이션하는 경우:

```bash
CSM_MIGRATE=true bash -c "$(curl -L https://setup.vector.dev)"
```

또는 기존 저장소 제거를 직접 처리하려면 `CSM_MIGRATE`를 설정하지 않아도 됩니다.

##### 수동 저장소 설정

수동으로 설정하려면 기존 저장소를 제거하고 HTTPS 다운로드를 위한 APT를 설정하세요:

```bash
rm "/etc/apt/sources.list.d/timber-vector.list"
sudo apt-get update
sudo apt-get install apt-transport-https curl gnupg
```

##### 관리 명령어

Vector 업그레이드:
```bash
sudo apt-get upgrade vector
```

Vector 제거:
```bash
sudo apt remove vector
```

Vector 시작:
```bash
sudo systemctl start vector
```

Vector 상태 확인:
```bash
sudo systemctl status vector
```

---

#### dpkg

dpkg는 Debian 운영체제 및 그 파생 제품에서 패키지 관리 시스템을 구동하는 소프트웨어입니다. dpkg는 .deb 패키지를 통해 소프트웨어를 설치하고 관리하는 데 사용됩니다.

##### 설치

1단계: .deb 패키지 다운로드

`{arch}`를 시스템 아키텍처(amd64, arm64, armhf)로 교체하세요:

```bash
curl \
  --proto '=https' \
  --tlsv1.2 -O \
  https://apt.vector.dev/pool/v/ve/vector_0.52.0-1_{arch}.deb
```

2단계: dpkg로 설치

```bash
sudo dpkg -i vector_0.52.0-1_{arch}.deb
```

##### 의존성 처리

설치 중 의존성 누락 오류가 발생하면 다음 명령으로 해결할 수 있습니다:

```bash
sudo apt install -f
```

##### 제거

```bash
dpkg -r vector
```

> 참고: dpkg 명령은 sudo/슈퍼유저 권한이 필요합니다. sudo 없이 실행하면 "dpkg: error: requested operation requires superuser privilege" 오류가 발생합니다.

---

#### Helm

Helm은 Kubernetes 클러스터에서 애플리케이션 및 서비스의 배포와 관리를 용이하게 하는 Kubernetes용 패키지 관리자입니다.

##### 설치

1단계: Vector 저장소 추가

```bash
helm repo add vector https://helm.vector.dev
helm repo update
```

2단계: Vector 설치

릴리스 이름 `<RELEASE_NAME>`으로 차트를 설치합니다:

```bash
helm install <RELEASE_NAME> vector/vector
```

Vector Aggregator를 네임스페이스 생성과 함께 설치:

```bash
helm install vector vector/vector \
  --namespace vector \
  --create-namespace
```

##### 배포 역할

기본적으로 Vector는 "Aggregator" 역할의 StatefulSet으로 실행됩니다. 대안으로:
- "Stateless-Aggregator" 역할의 Deployment로 실행
- "Agent" 역할의 DaemonSet으로 실행

##### 사용자 정의 설정으로 업그레이드

```bash
helm upgrade -f values.yaml <RELEASE_NAME> vector/vector
```

> 권장사항: values.yaml에는 재정의해야 하는 값만 포함하세요. 이렇게 하면 차트 버전 업그레이드 시 더 원활한 경험을 할 수 있습니다.

##### 제거

```bash
helm uninstall <RELEASE_NAME>
```

이 명령은 차트와 연결된 모든 Kubernetes 구성 요소를 제거하고 릴리스를 삭제합니다.

##### 요구 사항

차트는 Kubernetes 버전 >=1.15.0-0이 필요합니다.

> 참고: 별도의 `vector/vector-agent` 및 `vector/vector-aggregator` 차트는 이제 폐기(DEPRECATED) 되었습니다. 대신 통합된 `vector/vector` 차트를 사용하세요.

---

#### Homebrew

Homebrew는 Apple의 macOS 운영체제 및 일부 지원되는 Linux 시스템을 위한 무료 오픈 소스 패키지 관리 시스템입니다.

> 참고: Homebrew를 통한 Vector 설치는 macOS에서만 사용 가능합니다(Linux는 지원되지 않음).

##### 설치

1단계: Vector tap 추가

```bash
brew tap vectordotdev/brew
```

2단계: Vector 설치

```bash
brew install vector
```

##### 설정 파일 위치

brew 설치 시 설정 파일은 다음 위치에 저장됩니다:
- Intel Mac: `/usr/local/etc/vector/vector.yaml`
- Apple Silicon Mac: `/opt/homebrew/etc/vector/vector.yaml`

---

#### MSI

MSI는 Windows Installer의 파일 형식 및 명령줄 유틸리티입니다. Windows Installer(이전의 Microsoft Installer)는 Windows 시스템에서 소프트웨어를 설치하고 관리하는 데 사용되는 Microsoft Windows용 인터페이스입니다.

##### 설치

PowerShell에서 다음 명령을 실행하세요:

```powershell
Invoke-WebRequest https://packages.timber.io/vector/0.52.0/vector-x64.msi -OutFile vector-0.52.0-x64.msi
msiexec /i vector-0.52.0-x64.msi
```

##### 주요 이점

Rust의 Windows tier 1 지원 덕분에 Vector는 별도의 의존성 설치가 필요 없습니다. Vector 바이너리를 머신에 복사하는 것만으로 설치가 완료되며, 추가 DLL 파일 설치나 환경 변경도 필요하지 않습니다.

---

#### Nix

Nix는 선언적 빌드 및 배포를 지원하는 패키지 관리자입니다.

> 참고: Vector의 Nix 릴리스는 수동으로 업데이트해야 하므로, 공식 Vector 릴리스와 Nix 패키지 릴리스 사이에 지연이 있을 수 있습니다. 새로운 Vector 패키지는 일반적으로 며칠 내에 Nix에서 사용 가능합니다.

##### 설치

```bash
nix-env --install \
  --file https://github.com/NixOS/nixpkgs/archive/master.tar.gz \
  --attr vector
```

##### 업그레이드

```bash
nix-env --upgrade vector \
  --file https://github.com/NixOS/nixpkgs/archive/master.tar.gz
```

##### 제거

```bash
nix-env --uninstall vector
```

---

#### pacman

pacman은 주로 Arch Linux 및 그 파생 제품에서 Linux의 소프트웨어 패키지를 관리하는 유틸리티입니다.

##### 설치

Vector는 Arch Linux extra 패키지 저장소를 통해 설치할 수 있습니다:

```bash
sudo pacman -Syu vector
```

---

#### RPM

RPM Package Manager는 Fedora, CentOS, OpenSUSE, OpenMandriva, Red Hat Enterprise Linux 및 관련 Linux 기반 시스템에서 소프트웨어를 설치하고 관리하기 위한 무료 오픈 소스 패키지 관리 시스템입니다.

##### 설치

`{arch}`를 다음 중 하나로 교체하세요: `x86_64`, `aarch64`, 또는 `armv7hl`:

```bash
sudo rpm -i https://yum.vector.dev/stable/vector-0/{arch}/vector-0.52.0-1.{arch}.rpm
```

---

#### YUM

Yellowdog Updater, Modified(YUM)는 RPM Package Manager를 사용하는 Linux 운영체제를 위한 무료 오픈 소스 명령줄 패키지 관리자입니다.

> 참고: YUM 저장소는 Datadog에서 제공합니다.

##### 설치

1단계: 저장소 추가

```bash
bash -c "$(curl -L https://setup.vector.dev)"
```

2단계: Vector 설치

```bash
sudo yum install vector
```

---

### 플랫폼별 설치

#### Docker

Vector는 Docker Hub의 `timberio/vector` 이미지를 통해 컨테이너로 실행할 수 있습니다.

##### Docker 이미지 가져오기

```bash
docker pull timberio/vector:0.52.0-debian
```

사용 가능한 배포판:
- `debian` - Debian slim 기반
- `alpine` - Alpine Linux 기반 (권장, 크기가 작고 안정적)
- `distroless-libc` - 동적 링크 빌드
- `distroless-static` - 정적 링크된 musl x86 빌드

##### Vector 실행

사용자 정의 설정 파일로 Vector 실행:

```bash
docker run \
  -d \
  -v $PWD/vector.yaml:/etc/vector/vector.yaml:ro \
  -p 8686:8686 \
  --name vector \
  timberio/vector:0.52.0-debian
```

다른 배포판을 사용하는 경우 `debian`을 해당 배포판으로 교체하세요.

##### Docker alias 설정

Vector 명령을 더 쉽게 실행하려면 alias를 설정할 수 있습니다:

```bash
alias vector='docker run -i -v $(pwd)/:/etc/vector/ --rm timberio/vector:0.52.0-debian'
```

##### 이미지 변형

| 이미지 | 설명 |
|--------|------|
| Alpine | musl libc와 BusyBox를 기반으로 구축된 Linux 배포판. 다른 Docker 이미지보다 크기가 상당히 작고 라이브러리를 정적으로 링크합니다. 작은 크기와 신뢰성으로 인해 권장됩니다. |
| Debian | debian-slim 이미지 기반으로, debian 이미지의 더 작고 컴팩트한 변형입니다. |
| Distroless | OS를 최소화하거나 핵심 부분을 처음부터 구축하여 만든 기본 Docker 이미지. 정적 또는 동적 링크 바이너리를 실행하는 데 필수적인 것만 포함합니다. |

##### 아키텍처 지원

Vector의 이미지는 멀티 아키텍처를 지원하며 x86_64, ARM64, ARMv7 아키텍처를 지원합니다. Docker가 이를 투명하게 처리합니다.

---

#### Kubernetes

Kubernetes에서 Vector를 Helm, kubectl 또는 Vector Operator를 사용하여 설치할 수 있습니다.

##### Helm을 사용한 설치

위의 [Helm](#helm) 섹션을 참조하세요.

##### kubectl을 사용한 설치

Vector를 Agent 또는 Aggregator 역할로 설치하는 방법입니다. Vector는 전용 Kubernetes 네임스페이스에서 실행하는 것을 권장하며, 일반적으로 `vector`를 네임스페이스로 사용합니다.

Agent로 설치:

Vector Agent는 소스에서 데이터를 수집한 다음 싱크를 사용하여 다양한 대상으로 전달할 수 있습니다.

RBAC 구성:

최신 Kubernetes 클러스터는 역할 기반 액세스 제어(RBAC) 체계로 실행됩니다. RBAC가 활성화된 클러스터는 Vector가 Kubernetes API 엔드포인트에 액세스할 수 있도록 권한을 부여하기 위한 일부 구성이 필요합니다. RBAC는 현재 Kubernetes API에 대한 액세스를 제어하는 표준 방법이므로 필요한 구성이 기본 제공됩니다.

kubectl YAML 설정의 ClusterRole, ClusterRoleBinding 및 ServiceAccount와 Helm 차트의 rbac 구성을 참조하세요.

##### 권장 리소스 제한

Kubernetes에서 Vector를 Agent로 실행할 때 권장되는 리소스 제한:

```yaml
resources:
  requests:
    memory: "64Mi"
    cpu: "500m"
  limits:
    memory: "1024Mi"
    cpu: "6000m"
```

##### Pod 제외 설정

기본적으로 `kubernetes_logs` 소스는 `vector.dev/exclude: "true"` 레이블이 있는 Pod의 로그를 건너뜁니다. 레이블 또는 필드 선택기를 통해 추가 제외 규칙을 구성할 수 있습니다.

##### Vector Operator

Vector Operator(kaasops에서 제공)는 Helm을 통해 설치할 수 있습니다:

```bash
helm install vector-operator vector-operator/vector-operator
```

Operator는 노드 파일 시스템에서 컨테이너 및 애플리케이션 로그를 수집하기 위해 모든 노드에 vector agent daemonset을 배포하고 구성합니다.

---

### 운영체제별 설치

#### Amazon Linux

Amazon Linux에 Vector를 설치하려면 [YUM](#yum) 또는 [RPM](#rpm) 패키지 관리자를 사용하세요.

#### Arch Linux

Arch Linux에 Vector를 설치하려면 [pacman](#pacman)을 사용하세요.

#### CentOS

CentOS에 Vector를 설치하려면 [YUM](#yum) 또는 [RPM](#rpm) 패키지 관리자를 사용하세요.

#### Debian

Debian에 Vector를 설치하려면 [APT](#apt) 또는 [dpkg](#dpkg) 패키지 관리자를 사용하세요.

#### Fedora

Fedora에 Vector를 설치하려면 [YUM](#yum) 또는 [RPM](#rpm) 패키지 관리자를 사용하세요.

#### macOS

macOS에 Vector를 설치하는 방법:

##### Homebrew 사용 (권장)

```bash
brew tap vectordotdev/brew
brew install vector
```

##### Vector 설치 스크립트 사용

```bash
curl --proto '=https' --tlsv1.2 -sSfL https://sh.vector.dev | bash
```

##### 아카이브에서 설치

```bash
mkdir -p vector && \
curl -sSfL --proto '=https' --tlsv1.2 https://packages.timber.io/vector/0.52.0/vector-0.52.0-x86_64-apple-darwin.tar.gz | \
tar xzf - -C vector --strip-components=2
```

#### NixOS

NixOS는 Nix 패키지 관리자 위에 구축된 Linux 배포판입니다.

Nixpkgs에는 Vector에 대한 커뮤니티 유지 관리 모듈이 있으며, 옵션은 NixOS Search에서 볼 수 있습니다. 이를 사용하여 NixOS 시스템에서 Vector를 배포하고 구성할 수 있으며, 모듈은 변경 사항을 활성화하기 전에 Vector 구성이 유효한지 확인합니다.

[Nix](#nix) 섹션을 참조하세요.

#### Raspbian

Raspbian에 Vector를 설치하려면 [APT](#apt) 또는 [dpkg](#dpkg) 패키지 관리자를 사용하세요.

#### RHEL

RHEL에 Vector를 설치하려면 [YUM](#yum) 또는 [RPM](#rpm) 패키지 관리자를 사용하세요.

#### Ubuntu

Ubuntu에 Vector를 설치하려면 [APT](#apt) 또는 [dpkg](#dpkg) 패키지 관리자를 사용하세요.

#### Windows

Windows에 Vector를 설치하는 방법:

##### MSI 설치 프로그램 사용 (권장)

PowerShell에서:

```powershell
Invoke-WebRequest https://packages.timber.io/vector/0.52.0/vector-x64.msi -OutFile vector-0.52.0-x64.msi
msiexec /i vector-0.52.0-x64.msi
```

##### 아카이브에서 설치

PowerShell에서:

```powershell
Invoke-WebRequest https://packages.timber.io/vector/0.52.0/vector-0.52.0-x86_64-pc-windows-msvc.zip -OutFile vector.zip
Expand-Archive vector.zip -DestinationPath .
```

Vector 시작:

```powershell
.\bin\vector --config config\vector.yaml
```

---

### 수동 설치

#### Vector 설치 스크립트

Vector 설치 스크립트를 사용하면 플랫폼에 관계없이 Vector를 설치할 수 있습니다.

##### 기본 설치

```bash
curl --proto '=https' --tlsv1.2 -sSfL https://sh.vector.dev | bash
```

##### 특정 버전 설치

```bash
curl --proto '=https' --tlsv1.2 -sSfL https://sh.vector.dev | VECTOR_VERSION=0.52.0 bash
```

---

#### 아카이브에서 설치

이 방법은 사전 빌드된 아카이브에서 Vector를 설치합니다. 아카이브에는 vector 바이너리와 지원 구성 파일이 포함되어 있습니다.

> 권장사항: 가능하면 지원되는 플랫폼 또는 패키지 관리자를 통해 설치하는 것이 좋습니다. 이들은 권한, 디렉토리 생성 및 기타 복잡한 사항을 처리합니다.

##### Linux (x86_64, GNU libc)

```bash
mkdir -p vector && \
curl -sSfL --proto '=https' --tlsv1.2 https://packages.timber.io/vector/0.52.0/vector-0.52.0-x86_64-unknown-linux-gnu.tar.gz | \
tar xzf - -C vector --strip-components=2
```

##### Linux (x86_64, musl)

```bash
mkdir -p vector && \
curl -sSfL --proto '=https' --tlsv1.2 https://packages.timber.io/vector/0.52.0/vector-0.52.0-x86_64-unknown-linux-musl.tar.gz | \
tar xzf - -C vector --strip-components=2
```

##### Linux (ARM64)

```bash
mkdir -p vector && \
curl -sSfL --proto '=https' --tlsv1.2 https://packages.timber.io/vector/0.52.0/vector-0.52.0-aarch64-unknown-linux-musl.tar.gz | \
tar xzf - -C vector --strip-components=2
```

##### Linux (ARMv7)

```bash
mkdir -p vector && \
curl -sSfL --proto '=https' --tlsv1.2 https://packages.timber.io/vector/0.52.0/vector-0.52.0-armv7-unknown-linux-gnueabihf.tar.gz | \
tar xzf - -C vector --strip-components=2
```

##### macOS (x86_64)

```bash
mkdir -p vector && \
curl -sSfL --proto '=https' --tlsv1.2 https://packages.timber.io/vector/0.52.0/vector-0.52.0-x86_64-apple-darwin.tar.gz | \
tar xzf - -C vector --strip-components=2
```

##### Windows (x86_64)

PowerShell에서:

```powershell
Invoke-WebRequest https://packages.timber.io/vector/0.52.0/vector-0.52.0-x86_64-pc-windows-msvc.zip -OutFile vector.zip
Expand-Archive vector.zip -DestinationPath .
```

##### 설치 후 단계 (Linux/macOS)

1. vector 디렉토리로 이동:
   ```bash
   cd vector
   ```

2. Vector를 $PATH에 추가:
   ```bash
   echo "export PATH=\"$(pwd)/vector/bin:\$PATH\"" >> $HOME/.profile
   source $HOME/.profile
   ```

3. Vector 시작:
   ```bash
   vector --config config/vector.yaml
   ```

##### 다운로드 URL 형식

일반적인 다운로드 URL 형식:
```
https://packages.timber.io/vector/{version}/vector-{version}-{arch}.tar.gz
```

---

#### 소스에서 빌드

Vector는 호스트의 네이티브 툴체인을 사용하여 소스에서 설치할 수 있습니다. 또한 x86_64, ARM64 및 ARMv7 아키텍처용 Linux를 위한 정적 바이너리로 컴파일할 수도 있습니다.

##### 필수 조건

1. Rust 설치

```bash
curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh -s -- -y --default-toolchain stable
```

2. Protoc 설치

```bash
./scripts/environment/install-protoc.sh
```

3. 기타 의존성 설치 (Ubuntu/Debian 예시)

```bash
sudo apt-get update
sudo apt-get install -y build-essential cmake curl git
```

##### 빌드

저장소를 복제하고 빌드:

```bash
git clone https://github.com/vectordotdev/vector
cd vector
make build
```

`FEATURES` 환경 변수를 사용하여 기본 기능을 재정의할 수 있습니다:

```bash
FEATURES="<flag1>,<flag2>,..." make build
```

빌드 후 바이너리가 생성됩니다:
- Windows: `vector.exe`
- Linux/macOS: `target/release/vector`

다음 명령으로 시작할 수 있습니다:

```bash
./target/release/vector --config config/vector.yaml
```

##### 사용자 정의 기능 빌드

Vector는 빌드에 포함할 기능을 사용자 정의할 수 있는 많은 기능 플래그를 지원합니다. 기본적으로 모든 소스, 변환 및 싱크가 활성화됩니다. 전체 기능 목록은 Cargo.toml의 "[features]" 아래에 나열되어 있습니다.

특정 구성 요소만으로 빌드하는 예:

```bash
FEATURES="api,sources-file,transforms-remap,sinks-console" make build
```

##### Windows 빌드

Windows에서는 rustup, VC++ 빌드 도구(프롬프트가 표시되면), CMake, Protoc, Perl을 설치합니다. Perl을 PATH에 추가합니다.

Windows 빌드의 경우:

```bash
git clone https://github.com/vectordotdev/vector
git checkout v0.52.0
cd vector
set RUSTFLAGS=-Ctarget-feature=+crt-static
cargo build --no-default-features --features default-msvc --release
```

##### Docker 빌드

이러한 명령은 대상 아키텍처용 Rust 툴체인이 포함된 Docker 이미지를 빌드합니다. musl 대상은 완전히 정적인 바이너리를 생성하고 gnu 대상은 glibc에 대해 링크합니다. 컴파일된 패키지는 `target/artifacts/`에 위치합니다.

##### 프로파일 가이드 최적화 (PGO)

최적화된 빌드를 위해 Vector 소스 디렉토리로 이동하여 `cargo pgo build`를 실행합니다. 이렇게 하면 계측된 Vector 버전이 빌드됩니다.

```bash
cargo pgo run -- -- -c vector.toml
```

충분한 정보를 수집하기 위해 테스트 로드에서 계측된 Vector를 실행하고 잠시 기다립니다.

그런 다음 PGO 최적화로 Vector를 빌드합니다:

```bash
cargo pgo optimize
```

---

### 설치 후 설정

#### 구성 파일 위치

설치 방법에 따른 Vector 구성 파일 위치:

| 플랫폼 | 위치 |
|--------|------|
| Linux (APT, dpkg, RPM, YUM) | `/etc/vector/vector.yaml` |
| macOS (Homebrew) | `/opt/homebrew/etc/vector/vector.yaml` 또는 `/usr/local/etc/vector/vector.yaml` |
| Windows | `C:\Program Files\Vector\config\vector.yaml` |
| Docker | `/etc/vector/vector.yaml` |

> 참고: 기본 구성 위치 `/etc/vector/vector.toml`은 이제 폐기되었습니다. 이 위치는 v0.33.0에서 존재하는 경우 계속 사용되지만, v0.34.0부터 Vector는 먼저 `/etc/vector/vector.yaml`을 찾습니다. 사용자는 TOML 구성을 이 새 경로의 YAML로 변환하는 것이 좋습니다.

#### 지원되는 구성 형식

Vector는 TOML, YAML, JSON 파일 형식을 지원합니다. 파일 확장자(.yaml, .toml, .json)에서 해석할 형식이 결정됩니다. 지원되는 형식을 감지할 수 없는 경우 Vector는 YAML로 대체합니다.

#### 여러 구성 파일

Vector를 시작할 때 여러 구성 파일을 전달할 수 있습니다:

```bash
vector --config vector1.yaml --config vector2.yaml
```

`VECTOR_CONFIG_DIR` 설정을 사용하여 네임스페이스가 지정된 멀티파일 기능을 배포할 수 있습니다.

#### systemctl을 사용한 Vector 관리

APT, dpkg, RPM, YUM 또는 pacman을 사용하여 Vector를 설치한 경우 systemctl을 사용하여 관리할 수 있습니다.

Vector 시작:
```bash
sudo systemctl start vector
```

Vector 중지:
```bash
sudo systemctl stop vector
```

Vector 재시작 (구성 변경 후):
```bash
sudo systemctl restart vector
```

Vector 상태 확인:
```bash
sudo systemctl status vector
```

부팅 시 자동 시작 활성화:
```bash
sudo systemctl enable vector
```

#### 버전 확인

```bash
vector --version
```

---

### 참고 사항

#### 패키지 저장소 변경

패키지가 `apt.vector.dev` 및 `yum.vector.dev`로 이동되었습니다. `repositories.timber.io`에 있던 저장소는 2024년 2월 28일에 폐기되었습니다.

#### musl vs glibc 성능

musl은 Vector가 여러 스레드에서 실행될 때 glibc보다 성능 프로파일이 상당히 낮습니다. 단일 CPU에서 Vector를 실행하지 않는 한 glibc를 사용하는 것이 좋습니다.

#### 다운로드 페이지

모든 파일은 공식 다운로드 페이지에서 찾을 수 있습니다: https://vector.dev/download/

#### GitHub 저장소

Vector 소스 코드 및 이슈: https://github.com/vectordotdev/vector

---

### 참조 링크

- [Vector 공식 문서](https://vector.dev/docs/)
- [Vector 설치 가이드](https://vector.dev/docs/setup/installation/)
- [Vector 빠른 시작](https://vector.dev/docs/setup/quickstart/)
- [Vector 구성 가이드](https://vector.dev/docs/reference/configuration/)
- [Vector GitHub](https://github.com/vectordotdev/vector)
- [Vector Docker Hub](https://hub.docker.com/r/timberio/vector)
- [Vector Helm Charts](https://github.com/vectordotdev/helm-charts)
