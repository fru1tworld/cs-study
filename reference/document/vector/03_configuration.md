# Vector 설정 (Configuration)

이 문서는 Vector의 공식 문서 [Configuring Vector](https://vector.dev/docs/reference/configuration/)를 기반으로 한국어로 번역 및 정리한 내용입니다.

---

## 목차

1. [개요](#개요)
2. [설정 파일 위치](#설정-파일-위치)
3. [지원 형식](#지원-형식)
4. [설정 구조](#설정-구조)
5. [컴포넌트](#컴포넌트)
6. [글로벌 옵션](#글로벌-옵션)
7. [환경 변수](#환경-변수)
8. [다중 설정 파일](#다중-설정-파일)
9. [자동 설정 리로드](#자동-설정-리로드)
10. [설정 유효성 검사](#설정-유효성-검사)
11. [단위 테스트](#단위-테스트)
12. [시크릿 관리](#시크릿-관리)
13. [버퍼 설정](#버퍼-설정)
14. [API 설정](#api-설정)
15. [헬스체크 설정](#헬스체크-설정)
16. [프록시 설정](#프록시-설정)
17. [풍부화 테이블 (Enrichment Tables)](#풍부화-테이블-enrichment-tables)
18. [내부 메트릭 및 텔레메트리](#내부-메트릭-및-텔레메트리)
19. [설정 스키마 생성](#설정-스키마-생성)
20. [완전한 설정 예제](#완전한-설정-예제)

---

## 개요

Vector 토폴로지는 어떤 컴포넌트를 실행하고 어떻게 상호작용해야 하는지를 알려주는 설정 파일을 사용하여 정의됩니다. Vector는 경량이면서도 초고속인 관찰 가능성(observability) 파이프라인 구축 도구입니다.

Vector 토폴로지는 세 가지 유형의 컴포넌트로 구성됩니다:

- Sources (소스): 관찰 데이터 소스로부터 데이터를 수집하거나 수신합니다.
- Transforms (변환): 토폴로지를 통과하는 관찰 데이터를 조작하거나 변경합니다.
- Sinks (싱크): Vector에서 외부 서비스나 목적지로 데이터를 전송합니다.

---

## 설정 파일 위치

설정 파일의 위치는 설치 방법에 따라 다릅니다. 대부분의 Linux 기반 시스템에서는 다음 위치에서 설정 파일을 찾을 수 있습니다:

```
/etc/vector/vector.yaml
```

Vector는 버전 0.34.0부터 `/etc/vector/vector.yaml`을 기본 경로로 사용합니다.

---

## 지원 형식

Vector는 워크플로우에 맞게 YAML, TOML, JSON 형식을 지원합니다. 설정 파일을 전달할 때 파일 확장자에서 형식을 유추합니다.

### 형식 감지

- `.yaml` 또는 `.yml` - YAML 형식
- `.toml` - TOML 형식
- `.json` - JSON 형식

지원되는 형식을 감지할 수 없는 경우 Vector는 YAML로 폴백합니다.

### 기본 형식 변경

YAML이 이제 기본 설정 형식입니다. Vector 팀은 TOML 설정이 소수의 컴포넌트 이상을 포함할 때 빠르게 읽기 어려워진다는 것을 발견했습니다. 문서와 CLI 기본값에서 기본값을 업데이트하면 사용자가 TOML을 넘어선 후에만 전환하는 것이 아니라 처음부터 YAML로 시작하도록 권장합니다.

기존 TOML 및 JSON 설정은 이 결정에 영향을 받지 않으며 평소와 같이 작동합니다.

### 형식 변환

Vector는 TOML/JSON에서 YAML로 하나 이상의 설정을 변환하기 위한 시작점으로 사용할 수 있는 `vector convert-config` 명령을 제공합니다.

```bash
vector convert-config --input vector.toml --output vector.yaml
```

참고: 이 명령은 최선의 노력을 다하며 다음과 같은 주의 사항이 있습니다:
- 주석을 보존하지 않습니다.
- 기본 설정 값과 동일한 경우 명시적으로 값을 작성하지 않을 수 있습니다.
- 변환된 설정을 검토하고 그에 따라 편집하십시오.

### YAML 및 JSON의 추가 이점

YAML과 JSON을 지원하면 ytt, Jsonnet, Cue 와 같은 데이터 템플릿 언어를 사용할 수 있습니다.

---

## 설정 구조

Vector 설정 파일의 기본 구조는 다음과 같습니다:

### YAML 형식

```yaml
# 글로벌 옵션
data_dir: "/var/lib/vector"

# API 설정
api:
  enabled: true
  address: "127.0.0.1:8686"

# 소스 정의
sources:
  source_name:
    type: "source_type"
    # 소스별 옵션...

# 변환 정의
transforms:
  transform_name:
    type: "transform_type"
    inputs:
      - "source_name"
    # 변환별 옵션...

# 싱크 정의
sinks:
  sink_name:
    type: "sink_type"
    inputs:
      - "transform_name"
    # 싱크별 옵션...
```

### TOML 형식

```toml
# 글로벌 옵션
data_dir = "/var/lib/vector"

# API 설정
[api]
enabled = true
address = "127.0.0.1:8686"

# 소스 정의
[sources.source_name]
type = "source_type"
# 소스별 옵션...

# 변환 정의
[transforms.transform_name]
type = "transform_type"
inputs = ["source_name"]
# 변환별 옵션...

# 싱크 정의
[sinks.sink_name]
type = "sink_type"
inputs = ["transform_name"]
# 싱크별 옵션...
```

### JSON 형식

```json
{
  "data_dir": "/var/lib/vector",
  "api": {
    "enabled": true,
    "address": "127.0.0.1:8686"
  },
  "sources": {
    "source_name": {
      "type": "source_type"
    }
  },
  "transforms": {
    "transform_name": {
      "type": "transform_type",
      "inputs": ["source_name"]
    }
  },
  "sinks": {
    "sink_name": {
      "type": "sink_type",
      "inputs": ["transform_name"]
    }
  }
}
```

---

## 컴포넌트

### Sources (소스)

소스는 관찰 데이터 소스로부터 Vector로 데이터를 수집하거나 수신합니다. 사용 가능한 소스의 예:

- AMQP: AMQP 메시지 브로커에서 데이터 수집
- Apache Metrics: Apache HTTP 서버 메트릭 수집
- AWS ECS Metrics: AWS ECS 메트릭 수집
- AWS Kinesis Firehose: AWS Kinesis Firehose에서 데이터 수집
- AWS S3: AWS S3에서 데이터 수집
- AWS SQS: AWS SQS 큐에서 데이터 수집
- Datadog Agent: Datadog Agent에서 데이터 수집
- Demo Logs: 데모 로그 생성
- Docker Logs: Docker 컨테이너 로그 수집
- File: 파일에서 로그 수집
- HTTP Server: HTTP 서버로 데이터 수신
- Internal Logs: Vector의 내부 로그 수집
- Internal Metrics: Vector의 내부 메트릭 수집
- JournalD: Journald에서 로그 수집
- Kafka: Kafka에서 데이터 수집
- Kubernetes Logs: Kubernetes Pod 로그 수집
- stdin: 표준 입력에서 데이터 수집

### Transforms (변환)

변환은 Vector를 관찰 데이터 처리에 강력하게 만드는 핵심입니다. 사용 가능한 변환의 예:

- Remap (VRL): Vector Remap Language를 사용한 데이터 변환
- Aggregate: 메트릭 집계
- AWS EC2 Metadata: AWS EC2 메타데이터 추가
- Dedupe: 중복 이벤트 제거
- Exclusive Route: 배타적 라우팅
- Filter: 이벤트 필터링
- Incremental to Absolute: 증분 메트릭을 절대 메트릭으로 변환
- Log to Metric: 로그를 메트릭으로 변환
- Lua: Lua 스크립트를 사용한 데이터 변환
- Metric to Log: 메트릭을 로그로 변환
- Reduce: 여러 이벤트를 하나로 축소
- Route: 조건에 따른 이벤트 라우팅
- Sample: 이벤트 샘플링
- Tag Cardinality Limit: 태그 카디널리티 제한
- Throttle: 이벤트 속도 제한
- Trace to Log: 추적을 로그로 변환
- Window: 시간 윈도우 기반 처리

Remap 변환은 Vector가 관찰 데이터를 처리하는 데 매우 강력한 이유의 핵심입니다. 이 변환은 Vector Remap Language(VRL)라는 간단한 언어를 노출하여 Vector를 통과하는 이벤트 데이터를 구문 분석, 조작 및 장식할 수 있습니다.

### Sinks (싱크)

싱크는 Vector에서 외부 서비스나 목적지로 데이터를 전송합니다. 사용 가능한 싱크의 예:

- AMQP: AMQP 메시지 브로커로 전송
- AppSignal: AppSignal로 전송
- AWS CloudWatch Logs: AWS CloudWatch Logs로 전송
- AWS CloudWatch Metrics: AWS CloudWatch Metrics로 전송
- AWS Kinesis Data Firehose: AWS Kinesis Data Firehose로 전송
- AWS S3: AWS S3로 전송
- Console: 콘솔에 출력
- Elasticsearch: Elasticsearch로 전송
- File: 파일로 전송
- GCP Cloud Storage: GCP Cloud Storage로 전송
- HTTP: HTTP 엔드포인트로 전송
- Kafka: Kafka로 전송
- Loki: Grafana Loki로 전송
- Prometheus Remote Write: Prometheus Remote Write로 전송
- Vector: 다른 Vector 인스턴스로 전송

---

## 글로벌 옵션

글로벌 옵션은 Vector의 전반적인 동작을 제어합니다.

### data_dir

Vector 상태 데이터를 저장하는 데 사용되는 디렉토리입니다. 이 디렉토리에 Vector는 디스크 버퍼, 파일 체크포인트 등과 같은 모든 상태 데이터를 저장합니다. Vector는 이 디렉토리에 대한 쓰기 권한이 있어야 합니다.

```yaml
data_dir: "/var/lib/vector"
```

### log_schema

Vector가 작동하는 기본 필드 이름을 변경할 수 있습니다. 기본적으로 Vector는 주로 세 개의 필드에서 작동합니다: `host`, `message`, `timestamp`.

```yaml
log_schema:
  host_key: "host"           # 기본값
  message_key: "message"     # 기본값
  timestamp_key: "timestamp" # 기본값
```

일부 서비스는 타임스탬프 필드가 `@timestamp` 또는 다른 값에 매핑된 로그를 생성합니다. 이를 Vector 설정에서 전역적으로 사용자 정의할 수 있습니다.

### timezone

타임존 옵션은 명시적인 타임존이 포함되지 않은 타임스탬프 변환에 적용할 타임존을 지정합니다.

```yaml
timezone: "local"  # 또는 "UTC", "America/New_York" 등
```

타임존 이름은 TZ 데이터베이스의 모든 이름이거나 시스템 로컬 시간을 나타내는 "local"일 수 있습니다.

### expire_metrics_secs

설정된 경우, Vector는 지정된 시간 동안 업데이트되지 않은 모든 메트릭을 자동으로 제거하도록 내부 메트릭 시스템을 구성합니다. 음수 값으로 설정하면 만료가 비활성화됩니다.

```yaml
expire_metrics_secs: 300  # 기본값 (5분)
```

이 값은 내부 메트릭 스크레이프 간격(기본값 5분)보다 크게 설정하여 메트릭이 내보내지고 캡처될 만큼 충분히 오래 유지되도록 합니다.

### expire_metrics_per_metric_set

메트릭 세트별로 만료를 더 세밀하게 제어할 수 있는 옵션입니다. `expire_metrics_secs`는 이 설정에서 일치하지 않는 세트의 전역 기본값으로 사용됩니다.

### acknowledgements

종단 간 확인(end-to-end acknowledgements)에 대한 글로벌 설정입니다.

```yaml
acknowledgements:
  enabled: true
```

싱크에 대해 활성화되면, 해당 싱크에 연결된 종단 간 확인을 지원하는 모든 소스는 모든 연결된 싱크에서 이벤트가 확인될 때까지 소스에서 확인하기를 기다립니다.

### require_healthy

시작 중에 싱크가 정상으로 보고되어야 하는지 여부를 구성합니다. 활성화되고 싱크가 정상이 아닌 것으로 보고되면 Vector는 시작 중에 종료됩니다.

```yaml
require_healthy: true
```

---

## 환경 변수

Vector는 설정 파일 내에서 환경 변수를 보간합니다.

### 기본 구문

```yaml
# 기본 환경 변수 참조
host: "${HOSTNAME}"
host: "$HOSTNAME"
```

### 기본값 구문

환경 변수가 설정되지 않았거나 비어 있는 경우 기본값을 제공할 수 있습니다:

```yaml
# 변수가 설정되지 않았거나 비어 있으면 기본값 사용
option: "${ENV_VAR:-default}"

# 변수가 설정되지 않은 경우에만 기본값 사용
option: "${ENV_VAR-default}"
```

### 필수 환경 변수

환경 변수가 필요하고 설정되지 않은 경우 오류를 발생시킬 수 있습니다:

```yaml
option: "${TENANT:?tenant must be supplied}"
```

### 이스케이프

환경 변수를 리터럴로 처리하려면 `$` 문자를 앞에 붙여 이스케이프할 수 있습니다:

```yaml
# 리터럴 ${HOSTNAME}로 처리됨
option: "$${HOSTNAME}"
option: "$$HOSTNAME"
```

### 보안 고려 사항

Vector는 개행 문자가 포함된 환경 변수를 거부하여 환경 변수 보간과 관련된 보안 문제를 방지합니다. 이는 또한 다중 행 설정 블록의 주입을 방지합니다.

### 엄격 모드

CLI는 환경 변수 보간에 대한 엄격 모드를 제공합니다. 설정하면 설정 파일에서 누락된 환경 변수의 보간은 경고 대신 오류를 발생시켜 해당 설정 파일 로드에 실패합니다. 이 옵션은 더 이상 사용되지 않으며 향후 버전에서 누락된 환경 변수를 경고로 다운그레이드하는 기능을 제거하기 위해 제거될 예정입니다. 기본값은 true입니다.

### 설정 위치를 나타내는 환경 변수

Vector는 설정 위치를 나타내는 다음 환경 변수를 소싱합니다:
- `VECTOR_CONFIG`
- `VECTOR_CONFIG_YAML`
- `VECTOR_CONFIG_JSON`
- `VECTOR_CONFIG_TOML`

---

## 다중 설정 파일

Vector를 시작할 때 여러 설정 파일을 전달할 수 있습니다.

### 여러 파일 지정

```bash
vector --config vector1.yaml --config vector2.yaml
```

또는 짧은 형식:

```bash
vector -c ./configs/first.toml -c ./configs/second.toml -c ./more/*.toml
```

### 와일드카드 지원

와일드카드 경로가 지원됩니다. 파일이 지정되지 않으면 기본 설정 경로 `/etc/vector/vector.yaml`이 대상이 됩니다.

```bash
vector --config /etc/vector/*.yaml
```

### 디렉토리 기반 설정

`--config-dir`을 사용하여 Vector의 설정을 제공할 때, 컴포넌트 유형(예: transforms 또는 sources)이 있는 하위 디렉토리를 찾고 이러한 디렉토리의 파일이 해당 유형의 컴포넌트를 참조한다고 자동으로 유추합니다. 또한 Vector는 설정 파일의 파일 이름을 컴포넌트 ID로 사용합니다.

디렉토리 구조 예:

```
configs/
├── sources/
│   ├── apache_logs.yaml
│   └── nginx_logs.yaml
├── transforms/
│   ├── apache_parser.yaml
│   └── nginx_parser.yaml
└── sinks/
    ├── elasticsearch.yaml
    └── s3_archives.yaml
```

Vector를 시작할 때:

```bash
vector --config-dir ./configs
```

### 자동 네임스페이싱

Vector는 디렉토리 구조에서 컴포넌트 유형과 ID를 유추할 수 있으므로 더 적은 설정을 지정해야 합니다.

### 컴포넌트 ID에서 와일드카드

Vector는 토폴로지를 구축할 때 컴포넌트 ID에서 와일드카드(*)를 지원합니다.

```yaml
transforms:
  my_transform:
    type: remap
    inputs:
      - "app*"  # app1_logs, app2_logs 등과 일치
    source: |
      . = parse_json!(.message)
```

제한 사항: 와일드카드는 문자열 끝에 있어야 합니다.

### 완화된 와일드카드 매칭

유효성 검사 모드를 "relaxed"로 설정하면 입력과 일치하지 않는 와일드카드가 있는 설정이 오류 없이 허용됩니다.

---

## 자동 설정 리로드

설정 파일이 변경될 때 Vector가 자동으로 자신을 다시 로드하도록 할 수 있습니다.

### --watch-config 플래그

Vector 인스턴스를 처음 시작할 때 `--watch-config` 또는 `-w` 플래그를 설정합니다:

```bash
vector --config /etc/vector/vector.yaml --watch-config
```

또는 짧은 형식:

```bash
vector -c /etc/vector/vector.yaml -w
```

### SIGHUP 신호

Vector가 이미 실행 중인 설정을 수정하는 경우 SIGHUP 신호를 보내 변경 사항을 다시 로드하도록 프롬프트할 수 있습니다:

```bash
kill -SIGHUP $(pidof vector)
```

SIGHUP 신호를 발행할 수 없는 환경에서 Vector를 실행하는 경우 `--watch-config` 플래그로 실행하면 파일이 작성될 때마다 변경 사항을 자동으로 가져옵니다.

### 감시 방법 옵션

설정 파일을 감시하는 방법에 대한 옵션:

- recommend: 호스트 OS에 권장되는 이벤트 기반 감시자
- poll: 이벤트 기반 감시자가 작동하지 않는 경우(예: NFS를 통해 설정을 연결하는 경우) 폴링 감시자를 사용할 수 있습니다.

```bash
vector --config /etc/vector/vector.yaml --watch-config --watch-config-method poll
```

### 폴링 간격

설정 변경을 폴링할 간격을 지정할 수 있습니다. `--watch-config-method`에서 poll이 설정된 경우에만 적용됩니다. 기본값은 30초입니다.

### Windows 지원

Windows는 이제 다른 *nix 플랫폼과 마찬가지로 `--watch-config`를 통한 자동 설정 리로드를 지원합니다.

### 확장 파일 감시

`--watch-config` 플래그는 이제 풍부화 테이블 파일의 변경 사항도 감시합니다. HTTP 싱크의 TLS `crt_file` 및 `key_file`은 이제 `--watch-config`가 활성화되면 감시되므로 해당 파일의 변경 사항은 Vector를 다시 시작할 필요 없이 설정 리로드를 트리거합니다.

### 빈 설정 지원

Vector는 컴포넌트 없이 설정을 실행할 수 있습니다. 이는 나중에 실제 컴포넌트로 대체될 빈 스텁 설정을 로드하는 데 유용합니다. 이 기능은 설정 파일 변경 감시 없이는 유용하지 않을 수 있습니다.

---

## 설정 유효성 검사

`validate` 하위 명령은 지정한 설정에 대해 여러 검사 세트를 수행합니다.

### 기본 유효성 검사

```bash
vector validate /etc/vector/vector.yaml
```

유효성 검사가 성공하면 Vector는 코드 0으로 종료됩니다. 실패하면 코드 78로 종료됩니다.

### 헬스체크 건너뛰기

헬스체크된 엔드포인트에 연결할 수 없는 경우(예: 로컬 워크스테이션에서) Vector 설정을 유효성 검사하되 다른 모든 환경 검사를 계속 실행하려면 `--skip-healthchecks` 플래그를 사용합니다:

```bash
vector validate --skip-healthchecks /etc/vector/vector.yaml
```

참고: 설정된 `data_dir`은 여전히 쓰기 가능해야 합니다.

### 여러 설정 파일 유효성 검사

설정은 하나 이상의 파일에서 읽을 수 있습니다. 와일드카드 경로가 지원됩니다:

```bash
vector validate /etc/vector/*.yaml
```

### 유효성 검사 출력

`vector validate` 명령은 더 좋아 보이고 사용하기 편하게 미화되었습니다. 이는 환상적인 `linkerd validate` 명령에서 크게 영감을 받았습니다.

---

## 단위 테스트

Vector는 설정 파일 내에 직접 테스트를 정의할 수 있는 단위 테스트 설정을 지원합니다.

### 개요

이러한 테스트는 특정 입력 이벤트가 주어졌을 때 변환 컴포넌트의 토폴로지에서 출력을 확인하는 데 사용되어 설정 동작이 퇴행하지 않도록 보장합니다. 이는 협업하는 미션 크리티컬 프로덕션 파이프라인에 매우 강력한 기능입니다.

### 기본 단위 테스트 예제

```yaml
sources:
  my_source:
    type: demo_logs
    format: syslog
    count: 1

transforms:
  my_transform:
    type: remap
    inputs:
      - my_source
    source: |
      .message = upcase!(.message)

sinks:
  my_sink:
    type: console
    inputs:
      - my_transform
    encoding:
      codec: json

# 단위 테스트 정의
tests:
  - name: "test_upcase"
    inputs:
      - insert_at: my_transform
        type: log
        log_fields:
          message: "hello world"
    outputs:
      - extract_from: my_transform
        conditions:
          - type: vrl
            source: |
              assert_eq!(.message, "HELLO WORLD")
```

### insert_at 지시문

`insert_at` 지시문은 테스트 입력을 변환에 인위적으로 삽입합니다. Vector 단위 테스트를 관찰 데이터 소스를 모의하고 해당 모의 소스에 예상대로 변환이 응답하는지 확인하는 방법으로 생각해야 합니다.

### VRL 어설션

VRL 어설션을 사용하여 테스트 중인 변환의 출력이 기대에 부합하는지 확인할 수 있습니다.

### 테스트 실행

CLI를 사용하여 하나 이상의 파일에서 설정을 테스트할 수 있습니다:

```bash
vector test /etc/vector/vector.yaml
```

### 테스트 주도 설정

설정을 단위 테스트로 보완하는 것을 지원하며, 빌드 단계에서도 유용합니다. 조건 없이 단위 테스트 출력을 추가하면 변환의 입력 및 출력 이벤트를 단순히 인쇄하여 동작을 검사할 수 있습니다.

---

## 시크릿 관리

Vector는 설정에 시크릿을 평문으로 저장하지 않도록 여러 메커니즘을 제공합니다.

### 외부 시크릿 백엔드

설정 옵션을 사용하여 Vector 설정에 시크릿을 평문으로 저장하지 않도록 외부 백엔드에서 시크릿을 검색할 수 있습니다. 여러 백엔드를 설정할 수 있습니다.

```yaml
secret:
  my_backend:
    type: exec
    command:
      - "/path/to/secret-script.sh"
```

`SECRET[<backend_name>.<secret_key>]`를 사용하여 Vector에 시크릿을 검색하도록 알립니다. 이 플레이스홀더는 관련 백엔드에서 검색한 시크릿으로 대체됩니다.

```yaml
sinks:
  my_sink:
    type: http
    uri: "https://api.example.com"
    auth:
      strategy: bearer
      token: "SECRET[my_backend.api_token]"
```

### 지원되는 백엔드 유형

#### 1. Exec 백엔드

외부 프로세스를 호출하여 시크릿을 Vector에 안전하게 로드하는 메커니즘입니다. 예를 들어 Vault와 같은 서비스와 통합하여 자격 증명을 제공하는 데 사용할 수 있습니다.

```yaml
secret:
  vault:
    type: exec
    command:
      - "/usr/local/bin/vault-get-secret.sh"
```

#### 2. AWS Secrets Manager

AWS Secrets Manager를 Vector와 통합하여 API 키 및 데이터베이스 비밀번호와 같은 민감한 설정 값을 안전하게 관리합니다.

```yaml
secret:
  aws_secrets:
    type: aws_secrets_manager
    region: "us-east-1"
```

사용 예:

```yaml
sinks:
  elasticsearch:
    type: elasticsearch
    endpoints:
      - "https://elasticsearch.example.com"
    auth:
      strategy: basic
      user: "elastic"
      password: "SECRET[aws_secrets.elasticsearch_password]"
```

요구 사항: Vector v0.38.0 이상이 필요합니다.

#### 3. 파일 백엔드

JSON 파일에서 시크릿 세트를 읽습니다.

```yaml
secret:
  file_secrets:
    type: file
    path: "/etc/vector/secrets.json"
```

#### 4. 디렉토리 백엔드

파일 목록에서 시크릿을 로드합니다.

```yaml
secret:
  dir_secrets:
    type: directory
    path: "/etc/vector/secrets.d/"
```

### VRL 시크릿 함수

새로운 "secret" 함수가 추가되었습니다. 이러한 함수는 임의의 시크릿을 저장할 수 있으며 이제 메타데이터 함수 대신 사용해야 합니다. 문자열 키에서 문자열 값으로의 맵을 안전하게 저장합니다.

### 보안 고려 사항

이전에 선호되는 메커니즘은 환경 변수를 통한 주입이었지만, 호스트의 사용자가 Vector 프로세스의 /proc 파일 시스템에서 읽을 수 있는 경우 이러한 값을 읽을 수 있으므로 보안 문제가 있습니다.

### 디스크 버퍼의 시크릿

Vector 0.34.0 릴리스부터 이벤트의 시크릿이 이제 디스크 버퍼에 저장됩니다. 이러한 시크릿은 암호화되지 않은 상태로 저장됩니다.

### 모범 사례

- 절대 평문 시크릿을 사용하지 마십시오.
- 절대 Vector의 설정 파일에 평문 시크릿을 추가하지 마십시오.
- Vector가 사용하도록 설정된 데이터 디렉토리는 Vector 프로세스를 실행하는 사용자/그룹만 읽기/쓰기 액세스할 수 있도록 가능한 한 엄격하게 잠가야 합니다.
- 기본적으로 Unix 기반 플랫폼에서 Vector는 디스크 버퍼 디렉토리/파일에 대한 파일 권한을 프로세스 사용자만 읽기/쓰기 가능하도록 설정하려고 시도합니다.

---

## 버퍼 설정

Vector는 싱크에 대한 버퍼 토폴로지를 구성할 수 있습니다.

### 개요

오버플로 동작을 사용하여 운영자는 버퍼 토폴로지를 구성할 수 있습니다. 이는 순차적으로 배열된 두 개 이상의 버퍼로 구성되며, 하나의 버퍼가 토폴로지의 다음 버퍼로 오버플로될 수 있습니다.

### 메모리 버퍼

인메모리 버퍼는 내구성이 없습니다. Vector는 소스가 처리될 때까지 이벤트를 확인하지 않도록 하는 종단 간 확인과 같은 기능을 제공하지만, Vector 또는 Vector를 실행하는 호스트가 충돌하면 인메모리 버퍼에 있는 모든 이벤트가 손실됩니다.

```yaml
sinks:
  my_sink:
    type: http
    buffer:
      type: memory
      max_events: 500  # 기본값
      when_full: block  # 기본값
```

### 디스크 버퍼

데이터의 내구성이 Vector의 전반적인 성능보다 더 중요한 경우, 디스크 버퍼를 사용하여 Vector를 중지하고 시작하는 동안(Vector가 충돌하는 경우 포함) 버퍼링된 이벤트를 유지할 수 있습니다. 디스크 버퍼를 사용하면 Vector가 다시 시작될 때 기본적으로 중단된 곳에서 다시 시작할 수 있습니다.

```yaml
sinks:
  my_sink:
    type: http
    buffer:
      type: disk
      max_size: 1073741824  # 1GiB (최소 ~256MiB)
```

디스크 버퍼는 모든 이벤트가 디스크에 기록될 때 자동으로 체크섬을 계산하고, 읽기 중 손상이 감지되면 올바르게 디코딩할 수 있는 만큼의 이벤트를 자동으로 복구합니다.

파일 시스템에서 디스크 버퍼는 최대 파일 크기 128MiB로 성장하고 완전히 처리되면 삭제되는 추가 전용 로그 파일처럼 보입니다.

### 오버플로 버퍼 토폴로지

사용 가능한 메모리에 의해 제한되는 인메모리 버퍼만 사용하거나 싱크에 문제가 없더라도 처리량을 감소시키는 디스크 버퍼만 사용하는 대신, 오버플로 모드를 사용하여 먼저 인메모리 버퍼를 사용하고 필요한 경우에만 디스크 버퍼로 폴백하여 이벤트를 우선적으로 버퍼링할 수 있습니다.

```yaml
sinks:
  overflow_test:
    type: blackhole
    buffer:
      - type: memory
        max_events: 1000
        when_full: overflow
      - type: disk
        max_size: 1073741824  # 1GiB
```

중요 참고 사항: 인메모리 버퍼에 공간이 생기면 Vector가 버퍼링하려는 새 이벤트는 디스크 버퍼에 여전히 이벤트가 있더라도 인메모리 버퍼로 이동합니다. 또한 인메모리 버퍼의 새 이벤트가 디스크 버퍼에 저장된 이전 이벤트보다 먼저 반환될 수 있습니다. 버퍼 토폴로지에 오버플로 동작을 사용할 때 이벤트 순서 보장이 없습니다.

### 버퍼 설정 매개변수

| 매개변수 | 설명 | 기본값 |
|---------|------|--------|
| `type` | 버퍼 유형 (`memory` 또는 `disk`) | `memory` |
| `max_events` | 버퍼에 허용되는 최대 이벤트 수 (memory 유형에 해당) | 500 |
| `max_size` | 디스크의 최대 버퍼 크기 (disk 유형에 해당, 최소 ~256MB) | - |
| `when_full` | 버퍼가 가득 찼을 때 동작 (`block`, `drop_newest`, `overflow`) | `block` |

### 크기 조정 권장 사항

- 싱크가 많은 경우 메모리를 늘리거나 디스크 버퍼로 전환하는 것을 고려하십시오.
- 디스크 버퍼는 느리므로 가능하면 메모리를 늘리는 것이 좋습니다.
- 적용 가능한 경우 디스크 처리량을 예상 최대 처리량의 최소 2배로 구성하여 애플리케이션에 적절한 여유 공간을 제공하는 것이 좋습니다.
- 일반적인 시작점으로 vCPU당 2GiB의 메모리가 권장됩니다.
- 인메모리 배치 및 버퍼링으로 인해 싱크 수에 따라 메모리 사용량이 증가합니다.

---

## API 설정

Vector에는 GraphQL API가 포함되어 있으며 기본적으로 비활성화되어 있습니다.

### API 활성화

```yaml
api:
  enabled: true
  address: "127.0.0.1:8686"  # 선택 사항
```

TOML 형식:

```toml
[api]
enabled = true
address = "127.0.0.1:8686"
```

### API 옵션

| 옵션 | 설명 | 기본값 |
|------|------|--------|
| `enabled` | API 활성화 여부 | `false` |
| `address` | API가 바인딩할 네트워크 주소 | `"127.0.0.1:8686"` |
| `playground` | GraphQL 플레이그라운드 활성화 여부 | `true` |

### Docker 컨테이너에서 실행

Docker 컨테이너에서 Vector를 실행하는 경우 `0.0.0.0`에 바인딩해야 합니다. 그렇지 않으면 API가 컨테이너 외부에 노출되지 않습니다.

```yaml
api:
  enabled: true
  address: "0.0.0.0:8686"
```

### GraphQL 플레이그라운드

Vector에는 사용 가능한 쿼리를 탐색하고 API에 대해 수동으로 쿼리를 실행할 수 있는 번들 GraphQL 플레이그라운드가 포함되어 있습니다. `/playground` 경로에서 액세스할 수 있습니다.

### Vector Tap과 함께 사용

`vector tap` 명령은 Vector API에 의해 구동됩니다. Vector 인스턴스에 API가 활성화되고 노출되어 있는 한 `vector tap`이 작동합니다. `--url` 옵션을 사용하여 기본이 아닌 API 주소를 지정할 수 있습니다.

```bash
vector tap --url http://localhost:8686/graphql
```

---

## 헬스체크 설정

헬스체크는 다운스트림 서비스에 액세스할 수 있고 데이터를 수락할 준비가 되었는지 확인합니다.

### 개요

이 검사는 싱크 초기화 시 수행됩니다. 헬스체크가 실패하면 오류가 기록되고 Vector는 시작을 계속합니다.

### 글로벌 헬스체크 설정

헬스체크는 모든 싱크에 대해 전역적으로 활성화하거나 비활성화할 수 있으며, 싱크별로 재정의할 수 있습니다.

```yaml
healthchecks:
  enabled: true
  require_healthy: false
```

시작 중에 싱크가 정상으로 보고되어야 하는지 여부를 구성합니다. 활성화되고 싱크가 정상이 아닌 것으로 보고되면 Vector는 시작 중에 종료됩니다.

### 싱크 수준 헬스체크 설정

```yaml
sinks:
  my_sink:
    type: http
    healthcheck:
      enabled: true
```

헬스체크 실패 시 즉시 종료하려면 `--require-healthy` 플래그를 전달할 수 있습니다:

```bash
vector --config /etc/vector/vector.yaml --require-healthy
```

이 싱크에 대한 헬스체크를 비활성화하려면 `healthcheck.enabled`를 `false`로 설정할 수 있습니다.

### 유효성 검사에서 헬스체크 건너뛰기

헬스체크된 엔드포인트에 연결할 수 없는 경우 유효성 검사에서 헬스체크를 건너뛰려면:

```bash
vector validate --skip-healthchecks /etc/vector/vector.yaml
```

### 특별 고려 사항

Azure Blob Storage의 경우 비계정 SAS를 사용하는 경우 헬스체크가 실패하며 이 싱크에 대해 `healthcheck.enabled`를 `false`로 설정하여 비활성화해야 합니다.

### Vector API 헬스체크 엔드포인트

Vector는 또한 Vector가 실행 중인지 확인하는 데 유용한 API를 통해 헬스체크 엔드포인트를 제공합니다. 성공적인 응답은 Vector가 초기화되고 실행 중임을 나타냅니다.

---

## 프록시 설정

Vector는 HTTP 트래픽에 대한 프록시 지원을 제공합니다.

### 글로벌 프록시 설정

트래픽 유형에 따라 사용할 다른 프록시를 설정할 수 있습니다. 프록시되지 않아야 하는 특정 호스트도 설정할 수 있습니다.

```yaml
proxy:
  enabled: true
  http: "http://proxy.example.com:8080"
  https: "http://proxy.example.com:8080"
  no_proxy:
    - "localhost"
    - "127.0.0.1"
    - ".internal.example.com"
```

### 환경 변수 우선순위

설정 파일 또는 컴포넌트 수준에서 다른 HTTPS 프록시가 설정된 경우 환경 변수가 재정의됩니다. 소문자 변형이 대문자 변형보다 우선합니다.

- `http_proxy` / `HTTP_PROXY`
- `https_proxy` / `HTTPS_PROXY`
- `no_proxy` / `NO_PROXY`

### 컴포넌트 수준 프록시 설정

개별 컴포넌트에 대해 프록시를 설정할 수 있습니다:

```yaml
sinks:
  my_sink:
    type: http
    proxy:
      enabled: true
      http: "http://proxy.example.com:8080"
      https: "http://proxy.example.com:8080"
      no_proxy:
        - "localhost"
```

### 아키텍처 권장 사항

HTTP 프록시를 사용하려는 경우 Vector는 모든 Vector HTTP 트래픽을 프록시를 통해 쉽게 라우팅할 수 있는 글로벌 프록시 옵션을 제공합니다.

---

## 풍부화 테이블 (Enrichment Tables)

Vector는 관찰 데이터를 풍부하게 하기 위한 여러 유형의 풍부화 테이블을 지원합니다.

### 파일 기반 (CSV) 풍부화 테이블

CSV 파일을 풍부화에 사용하려면 다음 설정이 필요합니다:

```yaml
enrichment_tables:
  iot_remap:
    type: file
    file:
      path: "/etc/vector/iot_remap.csv"
      encoding:
        type: csv
    schema:
      code: integer
```

TOML 형식:

```toml
[enrichment_tables.iot_remap]
type = "file"

[enrichment_tables.iot_remap.file]
path = "/etc/vector/iot_remap.csv"
encoding = { type = "csv" }

[enrichment_tables.iot_remap.schema]
code = "integer"
```

### 메모리 풍부화 테이블

메모리 테이블은 로그에서 작동하고 VRL 객체를 수락합니다. 각 키-값 쌍은 테이블에 별도의 항목으로 저장되어 값을 해당 키와 연결합니다.

```yaml
enrichment_tables:
  memory_table:
    type: memory
    ttl: 60
    flush_interval: 5
    inputs:
      - cache_generator
```

메모리 풍부화 테이블은 데이터를 공급하기 위해 싱크로 사용해야 하며, 그런 다음 다른 풍부화 테이블처럼 쿼리할 수 있습니다.

### GeoIP 풍부화 테이블

MaxMind GeoIP 데이터베이스에서 풍부화 데이터를 로드합니다.

```yaml
enrichment_tables:
  geoip:
    type: geoip
    path: "/etc/vector/GeoLite2-City.mmdb"
```

### MMDB 풍부화 테이블

MaxMind 형식의 모든 데이터베이스에서 풍부화 데이터를 로드합니다.

```yaml
enrichment_tables:
  custom_mmdb:
    type: mmdb
    path: "/etc/vector/custom.mmdb"
```

### VRL에서 풍부화 테이블 사용

관찰 데이터를 풍부하게 하려면 `get_enrichment_table_record`를 사용할 수 있습니다:

```yaml
transforms:
  enrich:
    type: remap
    inputs:
      - source
    source: |
      .enriched = get_enrichment_table_record!("iot_remap", {"code": .device_code})
```

`find_enrichment_table_records`는 더 복잡한 사용 사례를 위해 배열 형식으로 여러 행을 반환할 수 있습니다.

### 메모리 테이블 설정 옵션

| 옵션 | 설명 |
|------|------|
| `ttl` | 캐시에 저장된 데이터의 수명을 제한하는 데 사용되는 TTL(초 단위 유효 시간). TTL이 만료되면 캐시에서 특정 키 뒤의 데이터가 제거됩니다. 키가 교체되면 TTL이 재설정됩니다. |
| `flush_interval` | 만료된 레코드를 찾는 데 사용되는 스캔 간격. TTL이 업데이트되도록 하지만 너무 많은 캐시 스캔을 수행하지 않도록 최적화로 제공됩니다. |

### 중요 참고 사항

시작 시 충돌을 방지하려면 풍부화 테이블 이름과 설정의 소스, 변환 및 싱크와 같은 다른 컴포넌트 간의 이름 충돌을 피하십시오. 이를 해결하려면 풍부화 테이블을 다른 컴포넌트에서 아직 사용하지 않는 고유한 이름으로 이름을 바꿀 수 있습니다.

---

## 내부 메트릭 및 텔레메트리

Vector는 자체 로그와 메트릭을 처리할 수 있는 두 가지 소스를 제공합니다.

### 개요

Vector는 다른 소스의 로그와 메트릭을 처리하는 것처럼 Vector가 생성한 로그와 메트릭을 처리하는 데 사용할 수 있는 `internal_logs` 및 `internal_metrics` 두 가지 소스를 제공합니다.

### 내부 메트릭 설정

```yaml
sources:
  vector_metrics:
    type: internal_metrics
    namespace: "vector"
    scrape_interval_secs: 15
```

### 내부 로그 설정

```yaml
sources:
  vector_logs:
    type: internal_logs
```

### Prometheus로 메트릭 전송 예제

```yaml
sources:
  vector_metrics:
    type: internal_metrics

sinks:
  prometheus:
    type: prometheus_remote_write
    endpoint: "https://localhost:8087/"
    inputs:
      - vector_metrics
```

### 콘솔에 로그 출력 예제

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

### 내부 메트릭 소스 옵션

| 옵션 | 설명 |
|------|------|
| `namespace` | 내보낸 메트릭의 기본 네임스페이스를 재정의합니다. |
| `scrape_interval_secs` | 메트릭 수집 간격(초)을 설정합니다. |
| `host_key` | 각 메트릭에 피어 호스트를 추가하는 데 사용되는 태그 이름을 재정의합니다. |
| `pid_key` | 각 메트릭에 현재 프로세스 ID를 추가하는 데 사용되는 태그 이름을 설정합니다. 기본적으로 설정되지 않으며 태그가 자동으로 추가되지 않습니다. |

### 다중 인스턴스 고려 사항

여러 Vector 인스턴스에서 동일한 대상으로 internal_metrics를 보낼 때, 일반적으로 메트릭 시리즈가 충돌하지 않도록 메트릭을 보내는 Vector 인스턴스에 고유한 태그로 메트릭에 태그를 지정하려고 합니다.

### 메트릭 만료

설정된 경우, Vector는 지정된 시간 동안 업데이트되지 않은 모든 메트릭을 자동으로 제거하도록 내부 메트릭 시스템을 구성합니다.

```yaml
expire_metrics_secs: 300
```

내부 메트릭 스크레이프 간격(기본값 5분)보다 크게 설정하여 메트릭이 내보내지고 캡처될 만큼 충분히 오래 유지되도록 합니다.

### 관찰 전략

Vector는 일관되고 상관된 텔레메트리 데이터를 보장하는 이벤트 기반 관찰 전략을 사용합니다. Vector는 핫 경로에서 로그 이벤트를 속도 제한합니다. 이를 통해 IO를 포화시키고 서비스를 중단시킬 위험 없이 세분화된 통찰력을 얻을 수 있습니다.

---

## 설정 스키마 생성

Vector는 파이프라인 실행 외에 다양한 작업을 수행하기 위한 CLI 하위 명령을 제공합니다.

### 스키마 생성

Vector 바이너리로 Vector 스키마를 생성하고 IDE에 출력을 제공하여 자동 완성을 활성화하는 방법:

```bash
vector generate-schema -o vector-v0.45.0-schema.json
```

이 설정으로 IDE는 실시간 제안을 제공하고 Vector 문서 방문을 줄입니다.

### IDE 통합

생성된 스키마를 사용하면 IDE에서 Vector 설정 파일을 편집할 때 자동 완성 및 유효성 검사를 받을 수 있습니다.

---

## 완전한 설정 예제

### 기본 예제: stdin에서 console로

```yaml
sources:
  in:
    type: stdin

sinks:
  out:
    inputs:
      - in
    type: console
    encoding:
      codec: text
```

### Syslog 파싱 및 JSON 출력

```yaml
sources:
  generate_syslog:
    type: demo_logs
    format: syslog
    count: 100

transforms:
  remap_syslog:
    type: remap
    inputs:
      - generate_syslog
    source: |
      . = parse_syslog!(string!(.message))

sinks:
  console:
    type: console
    inputs:
      - remap_syslog
    encoding:
      codec: json
```

### 파일에서 Elasticsearch와 S3로

```yaml
# 글로벌 옵션
data_dir: "/var/lib/vector"

# API 설정
api:
  enabled: false

# 소스: 파일에서 Apache 로그 수집
sources:
  apache_logs:
    type: file
    include:
      - "/var/log/apache2/*.log"
    ignore_older_secs: 86400

# 변환: Apache 로그 파싱
transforms:
  apache_parser:
    type: remap
    inputs:
      - apache_logs
    source: |
      . = parse_apache_log!(.message)

  # 샘플링
  apache_sampler:
    type: sample
    inputs:
      - apache_parser
    rate: 10

# 싱크: Elasticsearch
sinks:
  es_cluster:
    type: elasticsearch
    inputs:
      - apache_sampler
    endpoints:
      - "https://elasticsearch.example.com:9200"
    bulk:
      index: "apache-logs-%Y-%m-%d"

  # S3 아카이브
  s3_archives:
    type: aws_s3
    inputs:
      - apache_parser
    bucket: "my-log-archives"
    region: "us-east-1"
    key_prefix: "apache-logs/"
    encoding:
      codec: json
```

### 내부 모니터링 설정

```yaml
# Vector의 내부 로그 수집
sources:
  vector_logs:
    type: internal_logs

  vector_metrics:
    type: internal_metrics

# 콘솔에 로그 출력
sinks:
  log_console:
    type: console
    inputs:
      - vector_logs
    encoding:
      codec: text

  # Prometheus Remote Write로 메트릭 전송
  prometheus:
    type: prometheus_remote_write
    endpoint: "https://prometheus.example.com/api/v1/write"
    inputs:
      - vector_metrics
```

### 다중 소스 및 라우팅

```yaml
sources:
  app1_logs:
    type: file
    include:
      - "/var/log/app1/*.log"

  app2_logs:
    type: file
    include:
      - "/var/log/app2/*.log"

  nginx_logs:
    type: file
    include:
      - "/var/log/nginx/*.log"

transforms:
  # 와일드카드를 사용하여 모든 app 로그 처리
  parse_app_logs:
    type: remap
    inputs:
      - "app*"
    source: |
      . = parse_json!(.message)

  # Nginx 로그 파싱
  parse_nginx:
    type: remap
    inputs:
      - nginx_logs
    source: |
      . = parse_nginx_log!(.message)

  # 조건부 라우팅
  route_logs:
    type: route
    inputs:
      - parse_app_logs
      - parse_nginx
    route:
      errors: '.level == "error"'
      warnings: '.level == "warn"'

sinks:
  # 오류 로그는 PagerDuty로
  pagerduty:
    type: http
    inputs:
      - "route_logs.errors"
    uri: "https://events.pagerduty.com/v2/enqueue"
    encoding:
      codec: json

  # 모든 로그는 S3로
  s3:
    type: aws_s3
    inputs:
      - parse_app_logs
      - parse_nginx
    bucket: "all-logs"
    region: "us-east-1"
    encoding:
      codec: json
```

### 버퍼 및 헬스체크가 포함된 설정

```yaml
data_dir: "/var/lib/vector"

healthchecks:
  enabled: true
  require_healthy: false

sources:
  kafka:
    type: kafka
    bootstrap_servers: "kafka1:9092,kafka2:9092"
    group_id: "vector-consumer"
    topics:
      - "logs"

transforms:
  parse:
    type: remap
    inputs:
      - kafka
    source: |
      . = parse_json!(.message)

sinks:
  elasticsearch:
    type: elasticsearch
    inputs:
      - parse
    endpoints:
      - "https://elasticsearch:9200"
    buffer:
      - type: memory
        max_events: 1000
        when_full: overflow
      - type: disk
        max_size: 5368709120  # 5GiB
    healthcheck:
      enabled: true
```

### 환경 변수 및 시크릿 사용

```yaml
data_dir: "${VECTOR_DATA_DIR:-/var/lib/vector}"

secret:
  aws_secrets:
    type: aws_secrets_manager
    region: "${AWS_REGION:-us-east-1}"

sources:
  http:
    type: http_server
    address: "0.0.0.0:${VECTOR_HTTP_PORT:-8080}"

sinks:
  elasticsearch:
    type: elasticsearch
    inputs:
      - http
    endpoints:
      - "${ELASTICSEARCH_URL:?Elasticsearch URL must be set}"
    auth:
      strategy: basic
      user: "elastic"
      password: "SECRET[aws_secrets.elasticsearch_password]"
```

### 단위 테스트가 포함된 완전한 설정

```yaml
sources:
  logs:
    type: demo_logs
    format: json
    count: 10

transforms:
  parse_json:
    type: remap
    inputs:
      - logs
    source: |
      . = parse_json!(.message)
      .processed_at = now()
      .environment = "${ENVIRONMENT:-development}"

  filter_errors:
    type: filter
    inputs:
      - parse_json
    condition:
      type: vrl
      source: '.level == "error"'

sinks:
  console:
    type: console
    inputs:
      - filter_errors
    encoding:
      codec: json

# 단위 테스트
tests:
  - name: "test_json_parsing"
    inputs:
      - insert_at: parse_json
        type: log
        log_fields:
          message: '{"level": "info", "message": "test"}'
    outputs:
      - extract_from: parse_json
        conditions:
          - type: vrl
            source: |
              assert_eq!(.level, "info")
              assert_eq!(.message, "test")
              assert!(exists(.processed_at))

  - name: "test_error_filter"
    inputs:
      - insert_at: filter_errors
        type: log
        log_fields:
          level: "error"
          message: "Something went wrong"
    outputs:
      - extract_from: filter_errors
        conditions:
          - type: vrl
            source: |
              assert_eq!(.level, "error")

  - name: "test_non_error_filtered"
    inputs:
      - insert_at: filter_errors
        type: log
        log_fields:
          level: "info"
          message: "All good"
    no_outputs_from:
      - filter_errors
```

---

## 참고 자료

- [Vector 공식 문서 - Configuring Vector](https://vector.dev/docs/reference/configuration/)
- [Vector 글로벌 옵션 참조](https://vector.dev/docs/reference/configuration/global-options/)
- [Vector CLI 참조](https://vector.dev/docs/reference/cli/)
- [Vector 환경 변수](https://vector.dev/docs/reference/environment_variables/)
- [Vector 단위 테스트](https://vector.dev/docs/reference/configuration/unit-tests/)
- [Vector 퀵스타트 가이드](https://vector.dev/docs/setup/quickstart/)
- [Vector 버퍼링 모델](https://vector.dev/docs/architecture/buffering-model/)
- [Vector API 참조](https://vector.dev/docs/reference/api/)
- [Vector 시크릿 관리](https://vector.dev/highlights/2022-07-07-secrets-management/)
- [Vector 모니터링 및 관찰](https://vector.dev/docs/administration/monitoring/)
- [복잡한 설정 관리 가이드](https://vector.dev/guides/level-up/managing-complex-configs/)
