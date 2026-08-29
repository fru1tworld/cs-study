# Vector 설정

## Vector 설정 (Configuration)

원본 참고: [Configuring Vector](https://vector.dev/docs/reference/configuration/)

---

### 목차

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

### 개요

Vector 토폴로지는 어떤 컴포넌트를 실행하고 어떻게 상호작용할지 알려주는 설정 파일로 정의됨. Vector는 경량·초고속 관찰 가능성(observability) 파이프라인 구축 도구.

Vector 토폴로지는 세 가지 유형의 컴포넌트로 구성:

- Sources (소스): 관찰 데이터 소스로부터 데이터를 수집·수신
- Transforms (변환): 토폴로지를 통과하는 관찰 데이터를 조작·변경
- Sinks (싱크): Vector에서 외부 서비스·목적지로 데이터 전송

---

### 설정 파일 위치

설정 파일 위치는 설치 방법에 따라 다름. 대부분의 Linux 기반 시스템은 다음 위치에 설정 파일 존재:

```
/etc/vector/vector.yaml
```

Vector는 버전 0.34.0부터 `/etc/vector/vector.yaml`을 기본 경로로 사용.

---

### 지원 형식

Vector는 워크플로우에 맞게 YAML·TOML·JSON 형식 지원. 파일 확장자 기반으로 형식 자동 감지.

#### 형식 감지

- `.yaml` 또는 `.yml` - YAML 형식
- `.toml` - TOML 형식
- `.json` - JSON 형식

지원되는 형식을 감지할 수 없으면 Vector는 YAML로 폴백.

#### 기본 형식 변경

YAML이 기본 설정 형식. Vector 팀은 TOML 설정이 컴포넌트 수가 늘어날수록 가독성이 급격히 떨어짐을 발견 → 문서와 CLI 기본값을 YAML로 전환해 TOML 한계에 도달한 뒤 마이그레이션하는 대신 처음부터 YAML을 사용하도록 유도.

기존 TOML·JSON 설정은 이 결정에 영향받지 않고 평소와 같이 작동.

#### 형식 변환

Vector는 TOML/JSON 형식의 설정을 YAML로 변환할 때 출발점으로 활용 가능한 `vector convert-config` 명령 제공.

```bash
vector convert-config --input vector.toml --output vector.yaml
```

참고: 이 명령은 최선을 다하지만 다음 제약 존재:
- 주석 보존 안 됨
- 기본값과 동일한 경우 해당 값을 명시적으로 출력하지 않을 수 있음
- 변환된 설정은 반드시 검토·필요 시 수정 필요

#### YAML 및 JSON의 추가 이점

YAML·JSON 지원 → ytt, Jsonnet, Cue 같은 데이터 템플릿 언어 사용 가능.

---

### 설정 구조

Vector 설정 파일의 기본 구조:

#### YAML 형식

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

#### TOML 형식

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

#### JSON 형식

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

### 컴포넌트

#### Sources (소스)

소스는 관찰 데이터 소스로부터 Vector로 데이터를 수집·수신. 사용 가능한 소스 예시:

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

#### Transforms (변환)

변환은 Vector를 관찰 데이터 처리에 강력하게 만드는 핵심. 사용 가능한 변환 예시:

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

Remap 변환은 Vector의 관찰 데이터 처리 능력을 강화하는 핵심 요소. Vector Remap Language(VRL)라는 간결한 언어를 제공 → 파이프라인을 통과하는 이벤트 데이터를 파싱·조작·보강 가능.

#### Sinks (싱크)

싱크는 Vector에서 외부 서비스·목적지로 데이터 전송. 사용 가능한 싱크 예시:

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

### 글로벌 옵션

글로벌 옵션은 Vector의 전반적인 동작 제어.

#### data_dir

Vector 상태 데이터를 저장하는 데 사용되는 디렉토리. 이 디렉토리에 디스크 버퍼·파일 체크포인트 등 모든 상태 데이터 저장. Vector는 이 디렉토리에 대한 쓰기 권한 필요.

```yaml
data_dir: "/var/lib/vector"
```

#### log_schema

Vector가 작동하는 기본 필드 이름 변경 가능. 기본적으로 Vector는 주로 세 개의 필드에서 작동: `host`, `message`, `timestamp`.

```yaml
log_schema:
  host_key: "host"           # 기본값
  message_key: "message"     # 기본값
  timestamp_key: "timestamp" # 기본값
```

일부 서비스는 타임스탬프 필드가 `@timestamp` 또는 다른 값에 매핑된 로그를 생성. 이를 Vector 설정에서 전역적으로 사용자 정의 가능.

#### timezone

타임존 옵션은 명시적인 타임존이 포함되지 않은 타임스탬프 변환에 적용할 타임존 지정.

```yaml
timezone: "local"  # 또는 "UTC", "America/New_York" 등
```

타임존 이름은 TZ 데이터베이스의 모든 이름이거나 시스템 로컬 시간을 나타내는 "local" 가능.

#### expire_metrics_secs

설정 시 Vector는 지정된 시간 동안 업데이트되지 않은 모든 메트릭을 자동 제거하도록 내부 메트릭 시스템 구성. 음수 값으로 설정하면 만료 비활성화.

```yaml
expire_metrics_secs: 300
```

이 값은 내부 메트릭 스크레이프 간격(기본값 5분)보다 크게 설정 → 메트릭이 충분히 오래 유지되어 수집·캡처 가능.

#### expire_metrics_per_metric_set

메트릭 세트별로 만료를 더 세밀하게 제어 가능한 옵션. `expire_metrics_secs`는 이 설정에서 일치하지 않는 세트의 전역 기본값으로 사용됨.

#### acknowledgements

종단 간 확인(end-to-end acknowledgements)에 대한 글로벌 설정.

```yaml
acknowledgements:
  enabled: true
```

싱크에서 활성화하면 해당 싱크에 연결된 소스 중 종단 간 확인을 지원하는 소스는, 연결된 모든 싱크에서 이벤트가 확인될 때까지 소스 측 확인을 보류함.

#### require_healthy

시작 시 싱크가 정상 상태로 보고되어야 하는지 여부 설정. 활성화 상태에서 싱크가 비정상으로 보고되면 Vector는 시작 중 종료됨.

```yaml
require_healthy: true
```

---

### 환경 변수

Vector는 설정 파일 내에서 환경 변수를 보간함.

#### 기본 구문

```yaml
# 기본 환경 변수 참조
host: "${HOSTNAME}"
host: "$HOSTNAME"
```

#### 기본값 구문

환경 변수가 설정되지 않았거나 비어 있는 경우 기본값 제공 가능:

```yaml
# 변수가 설정되지 않았거나 비어 있으면 기본값 사용
option: "${ENV_VAR:-default}"

# 변수가 설정되지 않은 경우에만 기본값 사용
option: "${ENV_VAR-default}"
```

#### 필수 환경 변수

환경 변수가 필요하고 설정되지 않은 경우 오류 발생 가능:

```yaml
option: "${TENANT:?tenant must be supplied}"
```

#### 이스케이프

환경 변수를 리터럴 문자열로 처리하려면 `$`를 이중으로 작성해 이스케이프 가능:

```yaml
# 리터럴 ${HOSTNAME}로 처리됨
option: "$${HOSTNAME}"
option: "$$HOSTNAME"
```

#### 보안 고려 사항

Vector는 개행 문자가 포함된 환경 변수를 거부해 보간 관련 보안 취약점 차단 → 다중 행 설정 블록 주입도 방지됨.

#### 엄격 모드

CLI는 환경 변수 보간에 대한 엄격 모드 제공. 활성화 시 설정 파일에 누락된 환경 변수가 있으면 경고 대신 오류를 발생시켜 해당 설정 파일 로드를 실패시킴. 이 옵션은 deprecated 상태이며, 향후 버전에서 누락된 환경 변수를 경고로 처리하는 동작을 제거하면서 함께 삭제될 예정. 기본값은 true.

#### 설정 위치를 지정하는 환경 변수

Vector는 설정 파일 경로를 지정하기 위해 다음 환경 변수 지원:
- `VECTOR_CONFIG`
- `VECTOR_CONFIG_YAML`
- `VECTOR_CONFIG_JSON`
- `VECTOR_CONFIG_TOML`

---

### 다중 설정 파일

Vector 시작 시 여러 설정 파일 전달 가능.

#### 여러 파일 지정

```bash
vector --config vector1.yaml --config vector2.yaml
```

또는 짧은 형식:

```bash
vector -c ./configs/first.toml -c ./configs/second.toml -c ./more/*.toml
```

#### 와일드카드 지원

와일드카드 경로 지원. 파일이 지정되지 않으면 기본 설정 경로 `/etc/vector/vector.yaml`이 대상이 됨.

```bash
vector --config /etc/vector/*.yaml
```

#### 디렉토리 기반 설정

`--config-dir`로 설정 디렉토리를 지정하면 Vector는 컴포넌트 유형(예: transforms, sources)에 해당하는 하위 디렉토리를 자동 탐색 → 해당 디렉토리의 파일이 그 유형의 컴포넌트를 정의한다고 유추. 설정 파일의 파일 이름이 컴포넌트 ID로 사용됨.

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

#### 자동 네임스페이싱

Vector가 디렉토리 구조에서 컴포넌트 유형과 ID를 자동으로 유추 → 설정 내에 별도로 지정할 항목이 줄어듦.

#### 컴포넌트 ID에서 와일드카드

Vector는 토폴로지를 구축할 때 컴포넌트 ID에서 와일드카드(*) 지원.

```yaml
transforms:
  my_transform:
    type: remap
    inputs:
      - "app*"  # app1_logs, app2_logs 등과 일치
    source: |
      . = parse_json!(.message)
```

제한 사항: 와일드카드는 문자열 끝에 있어야 함.

#### 완화된 와일드카드 매칭

유효성 검사 모드를 "relaxed"로 설정하면 일치하는 입력이 없는 와일드카드가 포함된 설정도 오류 없이 허용됨.

---

### 자동 설정 리로드

설정 파일 변경 시 Vector가 자동으로 자신을 다시 로드하도록 설정 가능.

#### --watch-config 플래그

Vector 인스턴스를 처음 시작할 때 `--watch-config` 또는 `-w` 플래그 설정:

```bash
vector --config /etc/vector/vector.yaml --watch-config
```

또는 짧은 형식:

```bash
vector -c /etc/vector/vector.yaml -w
```

#### SIGHUP 신호

Vector가 실행 중인 상태에서 설정을 변경한 경우 SIGHUP 신호를 보내 설정을 다시 로드하도록 할 수 있음:

```bash
kill -SIGHUP $(pidof vector)
```

SIGHUP 신호를 보낼 수 없는 환경에서는 `--watch-config` 플래그 사용 시 파일이 변경될 때마다 자동으로 설정을 다시 로드함.

#### 감시 방법 옵션

설정 파일 감시 방법 옵션:

- `recommend`: 호스트 OS에 권장되는 이벤트 기반 감시자
- `poll`: 이벤트 기반 감시자가 동작하지 않는 환경(예: NFS를 통해 설정을 마운트하는 경우)에서 사용하는 폴링 감시자

```bash
vector --config /etc/vector/vector.yaml --watch-config --watch-config-method poll
```

#### 폴링 간격

설정 변경을 폴링할 간격 지정 가능. `--watch-config-method`가 `poll`로 설정된 경우에만 적용됨. 기본값은 60초.

#### Windows 지원

Windows는 이제 다른 *nix 플랫폼과 마찬가지로 `--watch-config`를 통한 자동 설정 리로드 지원.

#### 확장 파일 감시

`--watch-config` 플래그는 풍부화 테이블 파일의 변경 사항도 함께 감시함. HTTP 싱크의 TLS `crt_file`·`key_file`도 `--watch-config` 활성화 시 감시 대상에 포함 → 해당 파일이 변경되면 Vector 재시작 없이 설정 리로드가 트리거됨.

#### 빈 설정 지원

Vector는 컴포넌트가 없는 빈 설정으로도 실행 가능. 나중에 실제 컴포넌트로 채울 스텁 설정을 미리 로드하는 용도로 활용 가능 → 설정 파일 감시와 함께 사용할 때 유용.

---

### 설정 유효성 검사

`validate` 하위 명령은 지정한 설정에 대해 여러 검사 세트 수행.

#### 기본 유효성 검사

```bash
vector validate /etc/vector/vector.yaml
```

유효성 검사가 성공하면 Vector는 코드 0으로 종료됨. 실패하면 코드 78로 종료됨.

#### 헬스체크 건너뛰기

헬스체크된 엔드포인트에 연결할 수 없는 경우(예: 로컬 워크스테이션) Vector 설정을 유효성 검사하되 다른 모든 환경 검사를 계속 실행하려면 `--skip-healthchecks` 플래그 사용:

```bash
vector validate --skip-healthchecks /etc/vector/vector.yaml
```

참고: 설정된 `data_dir`은 여전히 쓰기 가능해야 함.

#### 여러 설정 파일 유효성 검사

설정은 하나 이상의 파일에서 읽기 가능. 와일드카드 경로 지원:

```bash
vector validate /etc/vector/*.yaml
```

#### 유효성 검사 출력

`vector validate` 명령의 출력은 가독성이 개선되어 사용하기 편리함. `linkerd validate` 명령에서 영감을 받음.

---

### 단위 테스트

Vector는 설정 파일 내에 직접 테스트를 정의 가능한 단위 테스트 설정 지원.

#### 개요

특정 입력 이벤트에 대한 변환 컴포넌트의 출력을 검증 → 설정 동작이 회귀하지 않도록 보장. 협업이 필요한 미션 크리티컬 프로덕션 파이프라인에서 특히 강력한 기능.

#### 기본 단위 테스트 예제

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

#### insert_at 지시문

`insert_at` 지시문은 테스트 입력을 변환에 직접 주입함. Vector 단위 테스트는 관찰 데이터 소스를 모의(mock)하고 해당 모의 소스에 변환이 예상대로 반응하는지 검증하는 방식으로 동작.

#### VRL 어설션

VRL 어설션을 사용해 테스트 중인 변환의 출력이 기대에 부합하는지 확인 가능.

#### 테스트 실행

CLI를 사용해 하나 이상의 파일에서 설정 테스트 가능:

```bash
vector test /etc/vector/vector.yaml
```

#### 테스트 주도 설정

설정에 단위 테스트를 추가하는 것을 권장, 빌드 파이프라인에 통합해 활용 가능. 조건 없이 단위 테스트 출력을 정의하면 변환의 입력·출력 이벤트를 그대로 출력해 동작 확인 가능.

---

### 시크릿 관리

Vector는 설정에 시크릿을 평문으로 저장하지 않도록 여러 메커니즘 제공.

#### 외부 시크릿 백엔드

외부 백엔드에서 시크릿을 가져와 Vector 설정에 평문으로 저장하지 않을 수 있음. 여러 백엔드를 동시에 설정 가능.

```yaml
secret:
  my_backend:
    type: exec
    command:
      - "/path/to/secret-script.sh"
```

`SECRET[<backend_name>.<secret_key>]` 형식으로 시크릿 참조를 작성하면 Vector가 해당 백엔드에서 시크릿을 조회 → 플레이스홀더를 실제 값으로 대체.

```yaml
sinks:
  my_sink:
    type: http
    uri: "https://api.example.com"
    auth:
      strategy: bearer
      token: "SECRET[my_backend.api_token]"
```

#### 지원되는 백엔드 유형

##### 1. Exec 백엔드

외부 프로세스를 호출해 시크릿을 Vector에 안전하게 로드하는 방식. Vault 같은 서비스와 통합해 자격 증명을 제공하는 데 활용 가능.

```yaml
secret:
  vault:
    type: exec
    command:
      - "/usr/local/bin/vault-get-secret.sh"
```

##### 2. AWS Secrets Manager

AWS Secrets Manager를 Vector와 통합해 API 키·데이터베이스 비밀번호 같은 민감한 설정 값을 안전하게 관리.

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

요구 사항: Vector v0.38.0 이상 필요.

##### 3. 파일 백엔드

JSON 파일에서 시크릿 세트를 읽음.

```yaml
secret:
  file_secrets:
    type: file
    path: "/etc/vector/secrets.json"
```

##### 4. 디렉토리 백엔드

파일 목록에서 시크릿을 로드함.

```yaml
secret:
  dir_secrets:
    type: directory
    path: "/etc/vector/secrets.d/"
```

#### VRL 시크릿 함수

VRL에 "secret" 함수 추가됨. 임의의 시크릿을 저장 가능하며 기존 메타데이터 함수 대신 사용하는 것을 권장. 문자열 키-값 쌍의 맵을 안전하게 저장.

#### 보안 고려 사항

과거에는 환경 변수를 통한 주입이 선호되었으나, 호스트 사용자가 Vector 프로세스의 /proc 파일 시스템을 읽을 수 있는 경우 환경 변수 값이 노출될 수 있어 보안상 문제.

#### 디스크 버퍼의 시크릿

Vector 0.34.0부터 이벤트에 포함된 시크릿이 디스크 버퍼에 저장됨. 이 시크릿은 암호화되지 않은 상태로 저장됨.

#### 모범 사례

- 평문 시크릿 사용 금지
- Vector 설정 파일에 평문 시크릿 포함 금지
- Vector의 데이터 디렉토리는 프로세스를 실행하는 사용자/그룹만 읽기/쓰기 가능하도록 권한을 최대한 엄격하게 설정 필요
- Unix 기반 플랫폼에서 Vector는 기본적으로 디스크 버퍼 디렉토리·파일의 권한을 프로세스 사용자만 읽기/쓰기 가능하도록 설정함

---

### 버퍼 설정

Vector는 싱크에 대한 버퍼 토폴로지 구성 가능.

#### 개요

오버플로 동작을 활용하면 버퍼 토폴로지 구성 가능. 순차적으로 배열된 두 개 이상의 버퍼로 구성되며, 하나의 버퍼가 가득 차면 다음 버퍼로 넘어감.

#### 메모리 버퍼

인메모리 버퍼는 내구성이 없음. Vector는 이벤트가 처리될 때까지 소스 측 확인을 보류하는 종단 간 확인 등의 기능을 제공하지만, Vector 프로세스나 호스트가 충돌하면 인메모리 버퍼의 모든 이벤트가 손실됨.

```yaml
sinks:
  my_sink:
    type: http
    buffer:
      type: memory
      max_events: 500  # 기본값
      when_full: block  # 기본값
```

#### 디스크 버퍼

데이터 내구성이 처리 성능보다 우선인 경우 디스크 버퍼를 사용하면 Vector 재시작(충돌 포함) 중에도 버퍼링된 이벤트 보존 가능. Vector가 재시작되면 기본적으로 중단된 지점부터 처리를 재개함.

```yaml
sinks:
  my_sink:
    type: http
    buffer:
      type: disk
      max_size: 1073741824  # 1GiB (최소 ~256MiB)
```

디스크 버퍼는 이벤트를 디스크에 기록할 때 자동으로 체크섬을 계산하며, 읽기 중 손상이 감지되면 디코딩 가능한 이벤트를 자동으로 복구함.

파일 시스템에서 디스크 버퍼는 최대 파일 크기 128MiB로 성장하고 완전히 처리되면 삭제되는 추가 전용 로그 파일처럼 보임.

#### 오버플로 버퍼 토폴로지

인메모리 버퍼만 사용하면 가용 메모리 한도에 제약받고, 디스크 버퍼만 사용하면 싱크에 문제가 없는 상황에서도 처리량이 저하됨. 오버플로 모드를 사용하면 우선적으로 인메모리 버퍼를 활용 → 필요한 경우에만 디스크 버퍼로 자동 폴백.

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

중요: 인메모리 버퍼에 여유 공간이 생기면 디스크 버퍼에 이벤트가 남아 있더라도 새로 버퍼링할 이벤트는 인메모리 버퍼로 먼저 이동함 → 인메모리 버퍼의 새 이벤트가 디스크 버퍼의 이전 이벤트보다 먼저 처리될 수 있음. 오버플로 동작을 사용하는 버퍼 토폴로지에서는 이벤트 순서가 보장되지 않음.

#### 버퍼 설정 매개변수

- `type`: 버퍼 유형 (`memory` 또는 `disk`), 기본값 `memory`
- `max_events`: 버퍼에 허용되는 최대 이벤트 수 (memory 유형에 해당), 기본값 500
- `max_size`: 디스크의 최대 버퍼 크기 (disk 유형에 해당, 최소 ~256MB), 기본값 없음
- `when_full`: 버퍼가 가득 찼을 때 동작 (`block`, `drop_newest`, `overflow`), 기본값 `block`

#### 크기 조정 권장 사항

- 싱크 수가 많을 경우 메모리를 늘리거나 디스크 버퍼로 전환 고려 필요
- 디스크 버퍼는 성능이 낮으므로 가능하면 메모리를 늘리는 편이 나음
- 디스크 처리량은 예상 최대 처리량의 최소 2배로 구성해 충분한 여유 확보 필요
- 일반적인 시작점으로 vCPU당 2GiB의 메모리 권장
- 인메모리 배치·버퍼링으로 인해 싱크 수에 비례해 메모리 사용량 증가

---

### API 설정

Vector에는 GraphQL API가 포함되어 있으며 기본적으로 비활성화됨.

#### API 활성화

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

#### API 옵션

- `enabled`: API 활성화 여부, 기본값 `false`
- `address`: API가 바인딩할 네트워크 주소, 기본값 `"127.0.0.1:8686"`
- `playground`: GraphQL 플레이그라운드 활성화 여부, 기본값 `true`

#### Docker 컨테이너에서 실행

Docker 컨테이너에서 Vector를 실행할 때는 `0.0.0.0`에 바인딩 필요. 그렇지 않으면 API가 컨테이너 외부에서 접근되지 않음.

```yaml
api:
  enabled: true
  address: "0.0.0.0:8686"
```

#### GraphQL 플레이그라운드

Vector에는 사용 가능한 쿼리를 탐색하고 API를 직접 실행해볼 수 있는 GraphQL 플레이그라운드가 내장됨. `/playground` 경로에서 접근 가능.

#### Vector Tap과 함께 사용

`vector tap` 명령은 Vector API를 통해 동작함. Vector 인스턴스에 API가 활성화되어 있으면 `vector tap` 사용 가능. `--url` 옵션으로 기본값이 아닌 API 주소 지정 가능.

```bash
vector tap --url http://localhost:8686/graphql
```

---

### 헬스체크 설정

헬스체크는 다운스트림 서비스에 액세스 가능하고 데이터를 수락할 준비가 되었는지 확인함.

#### 개요

이 검사는 싱크 초기화 시 수행됨. 헬스체크가 실패하면 오류가 기록되지만 Vector는 시작을 계속함.

#### 글로벌 헬스체크 설정

헬스체크는 모든 싱크에 대해 전역적으로 활성화·비활성화 가능하며, 싱크별로 개별 재정의도 가능.

```yaml
healthchecks:
  enabled: true
  require_healthy: false
```

시작 시 싱크가 정상 상태로 보고되어야 하는지 여부 설정. 활성화 상태에서 싱크가 비정상으로 보고되면 Vector는 시작을 중단하고 종료됨.

#### 싱크 수준 헬스체크 설정

```yaml
sinks:
  my_sink:
    type: http
    healthcheck:
      enabled: true
```

헬스체크 실패 시 즉시 종료하려면 `--require-healthy` 플래그 전달 가능:

```bash
vector --config /etc/vector/vector.yaml --require-healthy
```

이 싱크에 대한 헬스체크를 비활성화하려면 `healthcheck.enabled`를 `false`로 설정 가능.

#### 유효성 검사에서 헬스체크 건너뛰기

헬스체크된 엔드포인트에 연결할 수 없는 경우 유효성 검사에서 헬스체크를 건너뛰려면:

```bash
vector validate --skip-healthchecks /etc/vector/vector.yaml
```

#### 특별 고려 사항

Azure Blob Storage에서 비계정 SAS를 사용하는 경우 헬스체크가 실패함. 이 경우 해당 싱크의 `healthcheck.enabled`를 `false`로 설정해 헬스체크 비활성화 필요.

#### Vector API 헬스체크 엔드포인트

Vector는 API를 통해 헬스체크 엔드포인트도 제공함. Vector가 실행 중인지 확인하는 데 유용하며, 성공 응답은 Vector가 초기화되어 정상 실행 중임을 나타냄.

---

### 프록시 설정

Vector는 HTTP 트래픽에 대한 프록시 지원 제공.

#### 글로벌 프록시 설정

트래픽 유형에 따라 각각 다른 프록시 설정 가능. 프록시를 거치지 않아야 할 특정 호스트도 지정 가능.

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

#### 환경 변수 우선순위

설정 파일 또는 컴포넌트 수준에서 프록시를 별도로 설정하면 환경 변수보다 우선 적용됨. 환경 변수는 소문자 형식이 대문자 형식보다 우선순위가 높음.

- `http_proxy` / `HTTP_PROXY`
- `https_proxy` / `HTTPS_PROXY`
- `no_proxy` / `NO_PROXY`

#### 컴포넌트 수준 프록시 설정

개별 컴포넌트에 대해 프록시 설정 가능:

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

#### 아키텍처 권장 사항

HTTP 프록시가 필요한 경우 Vector의 글로벌 프록시 옵션을 사용하면 모든 HTTP 트래픽을 손쉽게 프록시를 통해 라우팅 가능.

---

### 풍부화 테이블 (Enrichment Tables)

Vector는 관찰 데이터를 풍부하게 하기 위한 여러 유형의 풍부화 테이블 지원.

#### 파일 기반 (CSV) 풍부화 테이블

CSV 파일을 풍부화에 사용하려면 다음 설정 필요:

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

#### 메모리 풍부화 테이블

메모리 테이블은 로그 이벤트를 처리하며 VRL 객체를 입력으로 받음. 각 키-값 쌍은 테이블에 별도 항목으로 저장되어 값과 키가 연결됨.

```yaml
enrichment_tables:
  memory_table:
    type: memory
    ttl: 60
    flush_interval: 5
    inputs:
      - cache_generator
```

메모리 풍부화 테이블은 데이터를 주입하기 위해 싱크로 사용해야 하며, 이후 다른 풍부화 테이블과 동일한 방식으로 쿼리 가능.

#### GeoIP 풍부화 테이블

MaxMind GeoIP 데이터베이스에서 풍부화 데이터를 로드함.

```yaml
enrichment_tables:
  geoip:
    type: geoip
    path: "/etc/vector/GeoLite2-City.mmdb"
```

#### MMDB 풍부화 테이블

MaxMind 형식의 모든 데이터베이스에서 풍부화 데이터를 로드함.

```yaml
enrichment_tables:
  custom_mmdb:
    type: mmdb
    path: "/etc/vector/custom.mmdb"
```

#### VRL에서 풍부화 테이블 사용

관찰 데이터를 풍부하게 하려면 `get_enrichment_table_record` 사용 가능:

```yaml
transforms:
  enrich:
    type: remap
    inputs:
      - source
    source: |
      .enriched = get_enrichment_table_record!("iot_remap", {"code": .device_code})
```

`find_enrichment_table_records`는 복잡한 사용 사례에서 여러 행을 배열 형식으로 반환함.

#### 메모리 테이블 설정 옵션

- `ttl`: 캐시에 저장된 데이터의 수명을 제한하는 데 사용되는 TTL(초 단위 유효 시간). TTL이 만료되면 캐시에서 특정 키 뒤의 데이터가 제거됨. 키가 교체되면 TTL이 재설정됨
- `flush_interval`: 만료된 레코드를 찾는 데 사용되는 스캔 간격. TTL이 업데이트되도록 하지만 너무 많은 캐시 스캔을 수행하지 않도록 최적화로 제공됨

#### 중요 참고 사항

시작 시 오류를 방지하려면 풍부화 테이블 이름이 소스·변환·싱크 등 다른 컴포넌트 이름과 충돌하지 않도록 주의 필요. 충돌 발생 시 풍부화 테이블을 고유한 이름으로 변경해 해결 가능.

---

### 내부 메트릭 및 텔레메트리

Vector는 자체 로그·메트릭을 처리 가능한 두 가지 소스 제공.

#### 개요

Vector는 외부 소스의 로그·메트릭을 처리하는 것과 동일한 방식으로 Vector 자체가 생성하는 로그·메트릭을 처리 가능하도록 `internal_logs`와 `internal_metrics` 두 가지 소스 제공.

#### 내부 메트릭 설정

```yaml
sources:
  vector_metrics:
    type: internal_metrics
    namespace: "vector"
    scrape_interval_secs: 15
```

#### 내부 로그 설정

```yaml
sources:
  vector_logs:
    type: internal_logs
```

#### Prometheus로 메트릭 전송 예제

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

#### 콘솔에 로그 출력 예제

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

#### 내부 메트릭 소스 옵션

- `namespace`: 내보낸 메트릭의 기본 네임스페이스를 재정의
- `scrape_interval_secs`: 메트릭 수집 간격(초)을 설정
- `host_key`: 각 메트릭에 피어 호스트를 추가하는 데 사용되는 태그 이름을 재정의
- `pid_key`: 각 메트릭에 현재 프로세스 ID를 추가하는 데 사용되는 태그 이름을 설정. 기본적으로 설정되지 않으며 태그가 자동으로 추가되지 않음

#### 다중 인스턴스 고려 사항

여러 Vector 인스턴스가 동일한 대상으로 internal_metrics를 전송하는 경우, 메트릭 시리즈가 충돌하지 않도록 각 인스턴스를 식별하는 고유한 태그를 메트릭에 추가하는 것이 일반적.

#### 메트릭 만료

설정 시 Vector는 지정된 시간 동안 업데이트되지 않은 모든 메트릭을 자동 제거하도록 내부 메트릭 시스템 구성.

```yaml
expire_metrics_secs: 300
```

내부 메트릭 스크레이프 간격(기본값 5분)보다 크게 설정해 메트릭이 수집·캡처되기에 충분히 오래 유지되도록 함.

#### 관찰 전략

Vector는 일관성 있고 상호 연관된 텔레메트리 데이터를 보장하는 이벤트 기반 관찰 전략을 사용함. 핫 경로에서는 로그 이벤트를 속도 제한해 IO 포화나 서비스 장애 위험 없이 세밀한 가시성 확보.

---

### 설정 스키마 생성

Vector는 파이프라인 실행 외에도 다양한 작업을 위한 CLI 하위 명령 제공.

#### 스키마 생성

Vector 바이너리로 스키마를 생성해 IDE에 제공하면 자동 완성 활성화 가능:

```bash
vector generate-schema -o vector-v0.45.0-schema.json
```

이를 통해 IDE가 실시간 제안을 제공해 Vector 문서를 직접 방문하는 빈도를 줄일 수 있음.

#### IDE 통합

생성된 스키마를 사용하면 IDE에서 Vector 설정 파일을 편집할 때 자동 완성·유효성 검사를 받을 수 있음.

---

### 완전한 설정 예제

#### 기본 예제: stdin에서 console로

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

#### Syslog 파싱 및 JSON 출력

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

#### 파일에서 Elasticsearch와 S3로

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

#### 내부 모니터링 설정

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

#### 다중 소스 및 라우팅

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

#### 버퍼 및 헬스체크가 포함된 설정

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

#### 환경 변수 및 시크릿 사용

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

#### 단위 테스트가 포함된 완전한 설정

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

### 참고 자료

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
