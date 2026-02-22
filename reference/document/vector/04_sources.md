# Vector Sources (소스)

## 개요

Source(소스)는 Vector가 데이터를 가져오는 위치 또는 데이터를 수신하는 방법을 정의합니다. 토폴로지에는 여러 개의 소스가 있을 수 있으며, 데이터를 수집하면서 이벤트로 정규화합니다. 이를 통해 데이터를 쉽고 일관되게 처리할 수 있습니다.

Vector는 로그, 메트릭, 트레이스를 수집하기 위한 다양한 소스를 지원합니다.

## 지원되는 Sources 목록

### 메시징 시스템
- AMQP
- Kafka
- NATS
- MQTT
- Pulsar
- GCP PubSub
- Redis
- AWS SQS
- AWS Kinesis Firehose

### 로그 수집
- File
- Docker logs
- Kubernetes logs
- JournalD
- Syslog
- Logstash
- Fluent
- Heroku Logplex
- Splunk HEC

### HTTP/네트워크
- HTTP Server
- HTTP Client
- Socket
- stdin
- WebSocket
- Vector

### 메트릭 수집
- Host metrics
- Apache metrics
- NGINX metrics
- MongoDB metrics
- PostgreSQL metrics
- EventStoreDB metrics
- Prometheus scrape
- Prometheus remote write
- Prometheus Pushgateway
- StatsD
- Static metrics
- Internal metrics

### 클라우드 서비스
- AWS S3
- AWS ECS metrics
- Datadog agent
- OpenTelemetry

### 기타
- Demo logs
- Internal logs
- Exec
- File Descriptor
- dnstap
- Okta

---

## 주요 Sources 상세 설명

---

## 1. File Source

File 소스는 디스크의 파일에서 데이터를 읽습니다. 파일 로테이션, 체크포인팅, 멀티라인 메시지 집계를 처리합니다.

### 상태 정보
- 상태: Stable
- 역할: Agent
- 전달 보장: Best Effort
- 상태 저장: Stateful

### 기본 구성

TOML 형식:
```toml
[sources.my_file_source]
type = "file"
include = ["/var/log//*.log"]
```

YAML 형식:
```yaml
sources:
  my_file_source:
    type: file
    include:
      - /var/log//*.log
```

### 주요 설정 옵션

| 옵션 | 타입 | 기본값 | 설명 |
|------|------|--------|------|
| `include` | [string] | 필수 | 포함할 파일 패턴 목록. 글로빙 지원 |
| `exclude` | [string] | [] | 제외할 파일 패턴 목록. include보다 우선함 |
| `read_from` | string | "beginning" | 파일 읽기 시작 위치 ("beginning" 또는 "end") |
| `ignore_checkpoints` | bool | false | 기존 체크포인트 무시 여부 |
| `ignore_older_secs` | int | - | 지정된 초보다 오래된 파일 무시 |
| `ignore_not_found` | bool | false | 파일을 찾을 수 없을 때 무시 여부 |
| `oldest_first` | bool | false | 오래된 파일 먼저 처리 |
| `glob_minimum_cooldown` | int | 15 | 글로빙 간격(초) |
| `fingerprint` | object | - | 파일 핑거프린트 설정 |
| `multiline` | object | - | 멀티라인 메시지 집계 설정 |

### 고급 구성 예제

```toml
[sources.application_logs]
type = "file"
include = ["/var/log/app/*.log", "/var/log/service/*.log"]
exclude = ["/var/log/app/debug.log"]
read_from = "beginning"
ignore_older_secs = 86400
oldest_first = true
ignore_not_found = true

# 멀티라인 설정 (스택트레이스 처리)
[sources.application_logs.multiline]
start_pattern = "^[^\\s]"
mode = "continue_through"
condition_pattern = "^[\\s]+"
timeout_ms = 1000
```

### 체크포인팅

Vector는 파일 읽기 위치를 추적하는 체크포인트 파일을 유지합니다. 이를 통해 Vector 재시작 시 마지막으로 읽은 위치부터 계속할 수 있습니다.

---

## 2. Kafka Source

Kafka 소스는 Apache Kafka 토픽에서 옵저버빌리티 데이터를 수집합니다.

### 상태 정보
- 상태: Stable
- 역할: Aggregator
- 전달 보장: At-least-once
- 상태 저장: Stateless

### 기본 구성

TOML 형식:
```toml
[sources.my_kafka_source]
type = "kafka"
bootstrap_servers = "10.14.22.123:9092,10.14.23.332:9092"
group_id = "consumer-group-name"
topics = ["^(prefix1|prefix2)-.+"]
```

YAML 형식:
```yaml
sources:
  my_kafka_source:
    type: kafka
    bootstrap_servers: 10.14.22.123:9092,10.14.23.332:9092
    group_id: consumer-group-name
    topics:
      - ^(prefix1|prefix2)-.+
```

### 주요 설정 옵션

| 옵션 | 타입 | 기본값 | 설명 |
|------|------|--------|------|
| `bootstrap_servers` | string | 필수 | Kafka 부트스트랩 서버 목록 (쉼표로 구분) |
| `group_id` | string | 필수 | 컨슈머 그룹 이름 |
| `topics` | [string] | 필수 | 구독할 Kafka 토픽 목록. 정규식 패턴 지원 |
| `auto_offset_reset` | string | "largest" | 오프셋 초기화 전략 ("smallest" 또는 "largest") |
| `session_timeout_ms` | int | 10000 | 세션 타임아웃(밀리초) |
| `drain_timeout_ms` | int | - | 셧다운/리밸런싱 시 대기 시간 |
| `commit_interval_ms` | int | 5000 | 오프셋 커밋 간격 |
| `fetch_wait_max_ms` | int | 100 | 최대 페치 대기 시간 |

### SASL 인증 구성

```toml
[sources.kafka_sasl]
type = "kafka"
bootstrap_servers = "kafka:9092"
group_id = "my-group"
topics = ["my-topic"]

[sources.kafka_sasl.sasl]
enabled = true
mechanism = "SCRAM-SHA-256"
username = "user"
password = "password"
```

### TLS 구성

```toml
[sources.kafka_tls]
type = "kafka"
bootstrap_servers = "kafka:9093"
group_id = "my-group"
topics = ["my-topic"]

[sources.kafka_tls.tls]
enabled = true
ca_file = "/path/to/ca.crt"
crt_file = "/path/to/client.crt"
key_file = "/path/to/client.key"
```

### 고급 구성

```toml
[sources.kafka_advanced]
type = "kafka"
bootstrap_servers = "kafka1:9092,kafka2:9092,kafka3:9092"
group_id = "vector-consumers"
topics = ["logs", "metrics", "traces"]
auto_offset_reset = "smallest"
session_timeout_ms = 30000
commit_interval_ms = 10000

[sources.kafka_advanced.decoding]
codec = "json"

[sources.kafka_advanced.librdkafka_options]
"partition.assignment.strategy" = "cooperative-sticky"
```

---

## 3. HTTP Server Source

HTTP Server 소스는 HTTP 요청을 통해 데이터를 수신합니다. 웹훅이나 다른 HTTP 클라이언트에서 데이터를 받는 데 유용합니다.

### 상태 정보
- 상태: Stable
- 역할: Aggregator
- 전달 보장: At-least-once
- 상태 저장: Stateless

### 기본 구성

TOML 형식:
```toml
[sources.my_http_server]
type = "http_server"
address = "0.0.0.0:8080"
```

YAML 형식:
```yaml
sources:
  my_http_server:
    type: http_server
    address: 0.0.0.0:8080
```

### 주요 설정 옵션

| 옵션 | 타입 | 기본값 | 설명 |
|------|------|--------|------|
| `address` | string | "0.0.0.0:8080" | 리스닝할 소켓 주소 (포트 포함) |
| `path` | string | "/" | 요청을 받을 HTTP 경로 |
| `path_key` | string | - | 요청 경로를 저장할 이벤트 키 |
| `method` | string | "POST" | 허용할 HTTP 메서드 |
| `strict_path` | bool | true | 경로 엄격 매칭 여부 |
| `headers` | [string] | - | 이벤트에 포함할 HTTP 헤더 목록 |

### 인증 구성

```toml
[sources.http_authenticated]
type = "http_server"
address = "0.0.0.0:8080"

[sources.http_authenticated.auth]
strategy = "basic"
user = "vector"
password = "secret"
```

### 디코딩 설정

```toml
[sources.http_json]
type = "http_server"
address = "0.0.0.0:8080"

[sources.http_json.decoding]
codec = "json"
```

### 프레이밍 설정

```toml
[sources.http_custom_framing]
type = "http_server"
address = "0.0.0.0:8080"

[sources.http_custom_framing.framing]
method = "character_delimited"

[sources.http_custom_framing.framing.character_delimited]
delimiter = ","
```

### TLS 구성

```toml
[sources.http_tls]
type = "http_server"
address = "0.0.0.0:443"

[sources.http_tls.tls]
enabled = true
crt_file = "/path/to/server.crt"
key_file = "/path/to/server.key"
```

### 고급 구성 예제

```toml
[sources.webhook_receiver]
type = "http_server"
address = "0.0.0.0:8080"
path = "/webhooks/events"
path_key = "http_path"
method = "POST"
headers = ["X-Custom-Header", "X-Request-ID"]
strict_path = false

[sources.webhook_receiver.auth]
strategy = "bearer"
token = "${WEBHOOK_TOKEN}"

[sources.webhook_receiver.decoding]
codec = "json"

[sources.webhook_receiver.decoding.json]
lossy = true
```

---

## 4. Kubernetes Logs Source

Kubernetes 로그 소스는 Kubernetes 클러스터에서 실행되는 Pod의 로그를 수집합니다.

### 상태 정보
- 상태: Stable
- 역할: Agent
- 전달 보장: Best Effort
- 상태 저장: Stateful

### 요구사항

Vector는 Kubernetes API에 대한 액세스가 필요합니다. 특히 다음 엔드포인트를 사용합니다:
- `/api/v1/pods`
- `/api/v1/namespaces`
- `/api/v1/nodes`

### 기본 구성

TOML 형식:
```toml
[sources.kubernetes]
type = "kubernetes_logs"
```

YAML 형식:
```yaml
sources:
  kubernetes:
    type: kubernetes_logs
```

### 주요 설정 옵션

| 옵션 | 타입 | 기본값 | 설명 |
|------|------|--------|------|
| `self_node_name` | string | - | 현재 노드 이름 (일반적으로 환경변수에서 가져옴) |
| `exclude_paths_glob_patterns` | [string] | [] | 제외할 로그 경로 글로브 패턴 |
| `extra_field_selector` | string | - | 추가 필드 셀렉터 |
| `extra_label_selector` | string | - | 추가 레이블 셀렉터 |
| `extra_namespace_label_selector` | string | - | 네임스페이스 레이블 셀렉터 |
| `max_read_bytes` | int | 2048 | 한 번에 읽을 최대 바이트 |
| `max_line_bytes` | int | 32768 | 최대 라인 길이 |
| `glob_minimum_cooldown_ms` | int | 60000 | 글로빙 최소 쿨다운(밀리초) |

### 어노테이션 필드 구성

```yaml
sources:
  kubernetes:
    type: kubernetes_logs
    pod_annotation_fields:
      pod_labels: kubernetes.pod_labels
      pod_annotations: kubernetes.pod_annotations
      pod_name: kubernetes.pod_name
      pod_namespace: kubernetes.pod_namespace
      pod_node_name: kubernetes.pod_node_name
      pod_uid: kubernetes.pod_uid
      container_name: kubernetes.container_name
      container_image: kubernetes.container_image
    namespace_annotation_fields:
      namespace_labels: kubernetes.namespace_labels
    node_annotation_fields:
      node_labels: kubernetes.node_labels
```

### 필드 어노테이션 비활성화

```yaml
sources:
  kubernetes_logs:
    type: kubernetes_logs
    namespace_annotation_fields:
      namespace_labels: ''
    node_annotation_fields:
      node_labels: ''
    pod_annotation_fields:
      pod_annotations: ''
      pod_labels: ''
```

### Pod 제외 설정

기본적으로 `vector.dev/exclude: "true"` 레이블이 있는 Pod의 로그는 건너뜁니다.

특정 컨테이너만 제외:
```yaml
# Pod 어노테이션에 추가
metadata:
  annotations:
    vector.dev/exclude-containers: "container1,container2"
```

### 멀티라인 처리

Vector는 기본적으로 Docker 크기 제한으로 인해 분할된 부분 메시지를 병합합니다. 스택트레이스와 같은 커스텀 병합이 필요한 경우 reduce transform 사용을 권장합니다.

### 고급 Kubernetes 구성

```yaml
sources:
  kubernetes:
    type: kubernetes_logs
    self_node_name: "${VECTOR_SELF_NODE_NAME}"
    extra_label_selector: "app.kubernetes.io/name!=vector"
    extra_namespace_label_selector: "environment=production"
    max_read_bytes: 4096
    max_line_bytes: 65536
    glob_minimum_cooldown_ms: 30000
    pod_annotation_fields:
      pod_labels: kubernetes.labels
      pod_name: kubernetes.pod
      pod_namespace: kubernetes.namespace
    namespace_annotation_fields:
      namespace_labels: kubernetes.namespace_labels
```

---

## 5. Syslog Source

Syslog 소스는 TCP, UDP 또는 Unix 소켓을 통해 syslog 메시지를 수신합니다.

### 상태 정보
- 상태: Stable
- 역할: Aggregator
- 전달 보장: Best Effort
- 상태 저장: Stateless

### 지원되는 Syslog 형식
- RFC 6587
- RFC 5424
- RFC 3164
- 기타 일반적인 변형 (Nginx Syslog 스타일 등)

### 기본 구성

TOML 형식:
```toml
[sources.my_syslog]
type = "syslog"
address = "0.0.0.0:514"
mode = "tcp"
```

YAML 형식:
```yaml
sources:
  my_syslog:
    type: syslog
    address: 0.0.0.0:514
    mode: tcp
```

### 주요 설정 옵션

| 옵션 | 타입 | 기본값 | 설명 |
|------|------|--------|------|
| `address` | string | "0.0.0.0:514" | 리스닝할 소켓 주소 |
| `mode` | string | 필수 | 수신 모드 ("tcp", "udp", "unix") |
| `path` | string | - | Unix 소켓 경로 (mode가 "unix"일 때) |
| `max_length` | int | 102400 | 최대 메시지 길이 |
| `host_key` | string | "host" | 소스 호스트를 저장할 키 |
| `source_ip_key` | string | - | 소스 IP를 저장할 키 |

### TCP 모드 구성

```toml
[sources.syslog_tcp]
type = "syslog"
address = "0.0.0.0:514"
mode = "tcp"
max_length = 42000

[sources.syslog_tcp.tls]
enabled = true
crt_file = "/path/to/server.crt"
key_file = "/path/to/server.key"
```

### UDP 모드 구성

```yaml
sources:
  syslog_udp:
    type: syslog
    address: 0.0.0.0:9514
    mode: udp
    max_length: 65535
```

### Unix 소켓 모드 구성

```toml
[sources.syslog_unix]
type = "syslog"
mode = "unix"
path = "/var/run/syslog.sock"
```

### 고급 구성

```toml
[sources.syslog_advanced]
type = "syslog"
address = "0.0.0.0:514"
mode = "tcp"
max_length = 102400
host_key = "syslog_host"
source_ip_key = "source_ip"
connection_limit = 1000
keepalive.time_secs = 300

[sources.syslog_advanced.tls]
enabled = true
crt_file = "/etc/vector/certs/server.crt"
key_file = "/etc/vector/certs/server.key"
ca_file = "/etc/vector/certs/ca.crt"
verify_certificate = true
```

---

## 6. Docker Logs Source

Docker 로그 소스는 Docker 컨테이너에서 로그를 수집합니다.

### 상태 정보
- 상태: Stable
- 역할: Agent
- 전달 보장: Best Effort
- 상태 저장: Stateful

### 기본 구성

TOML 형식:
```toml
[sources.my_docker_logs]
type = "docker_logs"
```

YAML 형식:
```yaml
sources:
  my_docker_logs:
    type: docker_logs
```

### 주요 설정 옵션

| 옵션 | 타입 | 기본값 | 설명 |
|------|------|--------|------|
| `docker_host` | string | - | Docker 소켓 경로 또는 호스트 |
| `include_containers` | [string] | - | 포함할 컨테이너 이름/ID 목록 |
| `exclude_containers` | [string] | - | 제외할 컨테이너 이름/ID 목록 |
| `include_labels` | [string] | - | 포함할 레이블 목록 |
| `include_images` | [string] | - | 포함할 이미지 이름 목록 |
| `auto_partial_merge` | bool | true | 부분 메시지 자동 병합 |
| `partial_event_marker_field` | string | - | 부분 이벤트 마커 필드 |
| `retry_backoff_secs` | int | 1 | 재시도 백오프 시간 |

### 컨테이너 필터링

```toml
[sources.docker_filtered]
type = "docker_logs"
include_containers = ["app_", "web_"]
exclude_containers = ["test_", "debug_"]
include_images = ["nginx", "httpd"]
include_labels = ["com.docker.compose.project=myproject"]
```

### 필터링 동작

- exclude가 우선: 컨테이너가 include와 exclude 모두와 일치하면 제외 됩니다.
- 접두사 매칭: 옵션들은 접두사 매칭을 사용합니다. `foo`는 `foo`, `foobar`, `foo-123` 등과 일치합니다.
- 기본 동작: 구성 없이 Vector는 모든 컨테이너에서 로그를 수집합니다.

### YAML 구성 예제

```yaml
sources:
  docker_logs:
    type: docker_logs
    docker_host: unix:///var/run/docker.sock
    include_containers:
      - nginx
      - app_
    exclude_containers:
      - debug_
      - test_
    include_images:
      - httpd
    auto_partial_merge: true
    retry_backoff_secs: 2
```

### 고급 구성

```toml
[sources.docker_advanced]
type = "docker_logs"
docker_host = "unix:///var/run/docker.sock"
include_containers = ["production_"]
exclude_containers = ["monitoring_vector"]
auto_partial_merge = true
retry_backoff_secs = 5

# 멀티라인 설정
[sources.docker_advanced.multiline]
start_pattern = "^\\d{4}-\\d{2}-\\d{2}"
mode = "halt_before"
condition_pattern = "^\\d{4}-\\d{2}-\\d{2}"
timeout_ms = 1000
```

---

## 7. JournalD Source

JournalD 소스는 systemd 저널에서 로그를 수집합니다.

### 상태 정보
- 상태: Stable
- 역할: Agent
- 전달 보장: Best Effort
- 상태 저장: Stateful

### 기본 구성

TOML 형식:
```toml
[sources.my_journald]
type = "journald"
```

YAML 형식:
```yaml
sources:
  my_journald:
    type: journald
```

### 주요 설정 옵션

| 옵션 | 타입 | 기본값 | 설명 |
|------|------|--------|------|
| `current_boot_only` | bool | true | 현재 부팅의 로그만 수집 |
| `include_units` | [string] | - | 포함할 systemd 유닛 목록 |
| `exclude_units` | [string] | - | 제외할 systemd 유닛 목록 |
| `include_matches` | object | - | 포함할 저널 필드 매칭 |
| `exclude_matches` | object | - | 제외할 저널 필드 매칭 |
| `journalctl_path` | string | - | journalctl 바이너리 경로 |
| `batch_size` | int | 16 | 한 번에 처리할 이벤트 수 |
| `data_dir` | string | - | 체크포인트 저장 디렉토리 |

### 유닛 필터링

```toml
[sources.journald_filtered]
type = "journald"
current_boot_only = true
include_units = ["nginx", "mysql", "application"]
exclude_units = ["systemd-resolved", "NetworkManager"]
```

### 필드 매칭 필터

```yaml
sources:
  journald_matches:
    type: journald
    include_matches:
      _SYSTEMD_UNIT:
        - nginx.service
        - mysql.service
      _TRANSPORT:
        - stdout
        - stderr
    exclude_matches:
      SYSLOG_IDENTIFIER:
        - audit
```

### 고급 구성

```toml
[sources.journald_advanced]
type = "journald"
current_boot_only = false
batch_size = 32
data_dir = "/var/lib/vector"
journalctl_path = "/usr/bin/journalctl"
include_units = ["nginx.service", "myapp.service"]
exclude_units = ["systemd-networkd.service"]

[sources.journald_advanced.include_matches]
_TRANSPORT = ["stdout", "stderr", "syslog"]
PRIORITY = ["0", "1", "2", "3", "4"]  # emerg to warning
```

---

## 8. Stdin Source

Stdin 소스는 표준 입력(stdin)을 통해 데이터를 수신합니다.

### 상태 정보
- 상태: Stable
- 역할: Agent
- 전달 보장: At-least-once
- 상태 저장: Stateless

### 기본 구성

TOML 형식:
```toml
[sources.my_stdin]
type = "stdin"
```

YAML 형식:
```yaml
sources:
  my_stdin:
    type: stdin
```

### 주요 설정 옵션

| 옵션 | 타입 | 기본값 | 설명 |
|------|------|--------|------|
| `max_length` | int | 102400 | 최대 라인 길이 |
| `host_key` | string | - | 호스트 키 |
| `decoding` | object | - | 디코딩 설정 |
| `framing` | object | - | 프레이밍 설정 |

### 디코딩 구성

```toml
[sources.stdin_json]
type = "stdin"

[sources.stdin_json.decoding]
codec = "json"
```

### 프레이밍 구성

기본적으로 각 라인은 새 줄 구분자(0xA 바이트)로 구분됩니다.

```toml
[sources.stdin_custom]
type = "stdin"

[sources.stdin_custom.framing]
method = "character_delimited"

[sources.stdin_custom.framing.character_delimited]
delimiter = "|"
```

---

## 9. Socket Source

Socket 소스는 TCP, UDP 또는 Unix 소켓을 통해 원시 데이터를 수신합니다.

### 상태 정보
- 상태: Stable
- 역할: Aggregator
- 전달 보장: Best Effort
- 상태 저장: Stateless

### 기본 구성

TOML 형식:
```toml
[sources.my_socket]
type = "socket"
address = "0.0.0.0:9000"
mode = "tcp"
```

YAML 형식:
```yaml
sources:
  my_socket:
    type: socket
    address: 0.0.0.0:9000
    mode: tcp
```

### 주요 설정 옵션

| 옵션 | 타입 | 기본값 | 설명 |
|------|------|--------|------|
| `address` | string | 필수 | 리스닝할 주소 |
| `mode` | string | 필수 | 모드 ("tcp", "udp", "unix", "unix_datagram") |
| `path` | string | - | Unix 소켓 경로 |
| `max_length` | int | 102400 | 최대 메시지 길이 |
| `host_key` | string | - | 호스트를 저장할 키 |
| `port_key` | string | - | 포트를 저장할 키 |

### TCP 모드

```toml
[sources.socket_tcp]
type = "socket"
address = "0.0.0.0:9000"
mode = "tcp"
max_length = 102400

[sources.socket_tcp.tls]
enabled = true
crt_file = "/path/to/cert.crt"
key_file = "/path/to/key.key"
```

### UDP 모드

```yaml
sources:
  socket_udp:
    type: socket
    address: 0.0.0.0:9001
    mode: udp
    max_length: 65535
```

### Unix 소켓 모드

```toml
[sources.socket_unix]
type = "socket"
mode = "unix"
path = "/var/run/vector.sock"
```

---

## 10. Exec Source

Exec 소스는 명령을 실행하고 그 출력을 수집합니다.

### 상태 정보
- 상태: Beta
- 역할: Agent
- 전달 보장: Best Effort
- 상태 저장: Stateless

### 기본 구성

TOML 형식:
```toml
[sources.my_exec]
type = "exec"
command = ["/bin/date"]
mode = "scheduled"
scheduled.exec_interval_secs = 60
```

YAML 형식:
```yaml
sources:
  my_exec:
    type: exec
    command:
      - /bin/date
    mode: scheduled
    scheduled:
      exec_interval_secs: 60
```

### 주요 설정 옵션

| 옵션 | 타입 | 기본값 | 설명 |
|------|------|--------|------|
| `command` | [string] | 필수 | 실행할 명령어와 인자 |
| `mode` | string | 필수 | 실행 모드 ("scheduled" 또는 "streaming") |
| `working_directory` | string | - | 작업 디렉토리 |
| `include_stderr` | bool | true | stderr 포함 여부 |
| `maximum_buffer_size` | int | - | 최대 버퍼 크기 |

### 스케줄 모드

```toml
[sources.exec_scheduled]
type = "exec"
command = ["/usr/bin/uptime"]
mode = "scheduled"
working_directory = "/tmp"
include_stderr = true

[sources.exec_scheduled.scheduled]
exec_interval_secs = 30
```

### 스트리밍 모드

```toml
[sources.exec_streaming]
type = "exec"
command = ["/usr/bin/tail", "-f", "/var/log/messages"]
mode = "streaming"
include_stderr = false

[sources.exec_streaming.streaming]
respawn_on_exit = true
respawn_interval_secs = 5
```

---

## 11. Demo Logs Source

Demo 로그 소스는 테스트 및 데모 목적으로 샘플 로그 데이터를 생성합니다.

### 상태 정보
- 상태: Stable
- 역할: Agent
- 전달 보장: Best Effort
- 상태 저장: Stateless

### 기본 구성

TOML 형식:
```toml
[sources.demo]
type = "demo_logs"
format = "syslog"
count = 100
```

YAML 형식:
```yaml
sources:
  demo:
    type: demo_logs
    format: syslog
    count: 100
```

### 주요 설정 옵션

| 옵션 | 타입 | 기본값 | 설명 |
|------|------|--------|------|
| `format` | string | "syslog" | 로그 형식 ("syslog", "common", "json", "apache_common", "apache_error", "bsd_syslog", "shuffle") |
| `count` | int | - | 생성할 로그 수 (무제한이면 생략) |
| `interval` | float | 1.0 | 로그 생성 간격(초) |
| `lines` | [string] | - | shuffle 형식에서 사용할 커스텀 라인 |

### 다양한 형식 예제

```toml
# Syslog 형식
[sources.demo_syslog]
type = "demo_logs"
format = "syslog"
interval = 0.1

# JSON 형식
[sources.demo_json]
type = "demo_logs"
format = "json"
count = 1000

# Apache Common Log 형식
[sources.demo_apache]
type = "demo_logs"
format = "apache_common"
interval = 0.5

# 커스텀 라인 셔플
[sources.demo_custom]
type = "demo_logs"
format = "shuffle"
lines = [
  "User logged in",
  "Request processed",
  "Error occurred",
  "Connection established"
]
interval = 1.0
```

---

## 12. Internal Logs Source

Internal 로그 소스는 Vector 자체에서 생성되는 로그를 수집합니다.

### 상태 정보
- 상태: Stable
- 역할: Agent
- 전달 보장: Best Effort
- 상태 저장: Stateless

### 기본 구성

TOML 형식:
```toml
[sources.vector_logs]
type = "internal_logs"
```

YAML 형식:
```yaml
sources:
  vector_logs:
    type: internal_logs
```

### 주요 설정 옵션

| 옵션 | 타입 | 기본값 | 설명 |
|------|------|--------|------|
| `host_key` | string | - | 호스트를 저장할 키 |
| `pid_key` | string | - | PID를 저장할 키 |

### 로그 레벨

Vector가 `internal_logs` 소스를 통해 전달하는 로그는 로그 레벨에 의해 결정되며, 기본값은 `info`입니다.

`internal_logs` 소스 외에도 Vector는 stderr에 로그를 기록하며, 이는 Kubernetes, SystemD 또는 Vector를 실행하는 방식에 따라 캡처될 수 있습니다.

### 사용 예제

```toml
# Vector 내부 로그 수집
[sources.vector_logs]
type = "internal_logs"

# 로그를 Splunk로 전송
[sinks.splunk]
type = "splunk_hec_logs"
inputs = ["vector_logs"]
endpoint = "https://splunk.example.com:8088"
token = "${SPLUNK_TOKEN}"
```

---

## 13. Internal Metrics Source

Internal 메트릭 소스는 Vector 자체의 내부 메트릭을 노출합니다.

### 상태 정보
- 상태: Stable
- 역할: Agent
- 전달 보장: Best Effort
- 상태 저장: Stateless

### 기본 구성

TOML 형식:
```toml
[sources.vector_metrics]
type = "internal_metrics"
```

YAML 형식:
```yaml
sources:
  vector_metrics:
    type: internal_metrics
```

### 주요 설정 옵션

| 옵션 | 타입 | 기본값 | 설명 |
|------|------|--------|------|
| `namespace` | string | "vector" | 메트릭 네임스페이스 |
| `scrape_interval_secs` | float | 1.0 | 메트릭 수집 간격(초) |
| `tags` | object | - | 모든 메트릭에 추가할 태그 |

### 구성 예제

```toml
[sources.vector_metrics]
type = "internal_metrics"
namespace = "vector"
scrape_interval_secs = 15

[sources.vector_metrics.tags]
environment = "production"
host = "${HOSTNAME}"
```

### Prometheus로 내보내기

```toml
[sources.vector_metrics]
type = "internal_metrics"
scrape_interval_secs = 10

[sinks.prometheus]
type = "prometheus_exporter"
inputs = ["vector_metrics"]
address = "0.0.0.0:9598"
```

---

## 14. Host Metrics Source

Host 메트릭 소스는 호스트 시스템의 메트릭(CPU, 메모리, 디스크 등)을 수집합니다.

### 상태 정보
- 상태: Beta
- 역할: Agent
- 전달 보장: Best Effort
- 상태 저장: Stateless

### 기본 구성

TOML 형식:
```toml
[sources.host]
type = "host_metrics"
```

YAML 형식:
```yaml
sources:
  host:
    type: host_metrics
```

### 주요 설정 옵션

| 옵션 | 타입 | 기본값 | 설명 |
|------|------|--------|------|
| `collectors` | [string] | 모두 | 활성화할 수집기 목록 |
| `namespace` | string | "host" | 메트릭 네임스페이스 |
| `scrape_interval_secs` | float | 15.0 | 수집 간격(초) |

### 사용 가능한 수집기

- `cpu` - CPU 사용량
- `disk` - 디스크 사용량
- `filesystem` - 파일시스템 사용량
- `load` - 시스템 로드
- `host` - 호스트 정보
- `memory` - 메모리 사용량
- `network` - 네트워크 통계

### 수집기 필터링

```toml
[sources.host_metrics]
type = "host_metrics"
collectors = ["cpu", "memory", "disk", "network"]
namespace = "host"
scrape_interval_secs = 30
```

### 디스크/네트워크 디바이스 필터링

```yaml
sources:
  host_metrics:
    type: host_metrics
    collectors:
      - cpu
      - memory
      - disk
      - network
    disk:
      devices:
        includes:
          - sda
          - sdb
        excludes:
          - loop*
    network:
      devices:
        includes:
          - eth*
          - ens*
        excludes:
          - lo
```

### 컨테이너 내에서 호스트 메트릭 수집

```toml
[sources.host_metrics]
type = "host_metrics"
# 호스트의 /proc를 컨테이너에 마운트한 경우
procfs_root = "/host/proc"
```

---

## 15. NGINX Metrics Source

NGINX 메트릭 소스는 NGINX 서버에서 메트릭을 수집합니다.

### 상태 정보
- 상태: Beta
- 역할: Agent
- 전달 보장: Best Effort
- 상태 저장: Stateless

### 요구사항

NGINX `ngx_http_stub_status_module` 모듈이 활성화되어 있어야 합니다.

### 기본 구성

TOML 형식:
```toml
[sources.nginx_metrics]
type = "nginx_metrics"
endpoints = ["http://localhost/basic_status"]
```

YAML 형식:
```yaml
sources:
  nginx_metrics:
    type: nginx_metrics
    endpoints:
      - http://localhost/basic_status
```

### 주요 설정 옵션

| 옵션 | 타입 | 기본값 | 설명 |
|------|------|--------|------|
| `endpoints` | [string] | 필수 | NGINX 상태 엔드포인트 목록 |
| `namespace` | string | "nginx" | 메트릭 네임스페이스 |
| `scrape_interval_secs` | float | 15.0 | 수집 간격(초) |

### 고급 구성

```toml
[sources.nginx_metrics]
type = "nginx_metrics"
endpoints = ["http://nginx1/basic_status", "http://nginx2/basic_status"]
namespace = "nginx"
scrape_interval_secs = 30

[sources.nginx_metrics.tls]
verify_certificate = true
```

---

## 16. Apache Metrics Source

Apache 메트릭 소스는 Apache HTTP 서버에서 메트릭을 수집합니다.

### 상태 정보
- 상태: Beta
- 역할: Agent
- 전달 보장: Best Effort
- 상태 저장: Stateless

### 요구사항

Apache `mod_status` 모듈이 활성화되어 있어야 합니다.

### 기본 구성

TOML 형식:
```toml
[sources.apache_metrics]
type = "apache_metrics"
endpoints = ["http://localhost:8080/server-status/?auto"]
```

YAML 형식:
```yaml
sources:
  apache_metrics:
    type: apache_metrics
    endpoints:
      - http://localhost:8080/server-status/?auto
```

### 주요 설정 옵션

| 옵션 | 타입 | 기본값 | 설명 |
|------|------|--------|------|
| `endpoints` | [string] | 필수 | mod_status 엔드포인트 목록 |
| `namespace` | string | "apache" | 메트릭 네임스페이스 |
| `scrape_interval_secs` | float | 15.0 | 수집 간격(초) |

### 고급 구성

```yaml
sources:
  apache_metrics:
    type: apache_metrics
    endpoints:
      - http://apache1:8080/server-status/?auto
      - http://apache2:8080/server-status/?auto
    namespace: apache
    scrape_interval_secs: 30
```

---

## 17. MongoDB Metrics Source

MongoDB 메트릭 소스는 MongoDB 데이터베이스에서 메트릭을 수집합니다.

### 상태 정보
- 상태: Beta
- 역할: Agent
- 전달 보장: Best Effort
- 상태 저장: Stateless

### 기본 구성

TOML 형식:
```toml
[sources.mongodb_metrics]
type = "mongodb_metrics"
endpoints = ["mongodb://localhost:27017"]
```

YAML 형식:
```yaml
sources:
  mongodb_metrics:
    type: mongodb_metrics
    endpoints:
      - mongodb://localhost:27017
```

### 수집되는 메트릭

- 어설션 수
- 연결 상태별 수
- 힙 공간 사용량
- 페이지 폴트 수

---

## 18. PostgreSQL Metrics Source

PostgreSQL 메트릭 소스는 PostgreSQL 데이터베이스에서 메트릭을 수집합니다.

### 상태 정보
- 상태: Beta
- 역할: Agent
- 전달 보장: Best Effort
- 상태 저장: Stateless

### 기본 구성

TOML 형식:
```toml
[sources.postgresql_metrics]
type = "postgresql_metrics"
endpoints = ["postgresql://user:password@localhost:5432/database"]
```

YAML 형식:
```yaml
sources:
  postgresql_metrics:
    type: postgresql_metrics
    endpoints:
      - postgresql://user:password@localhost:5432/database
```

### 주요 설정 옵션

| 옵션 | 타입 | 기본값 | 설명 |
|------|------|--------|------|
| `endpoints` | [string] | 필수 | PostgreSQL 인스턴스 목록 |
| `namespace` | string | "postgresql" | 메트릭 네임스페이스 |
| `scrape_interval_secs` | float | 15.0 | 수집 간격(초) |
| `include_databases` | [string] | - | 포함할 데이터베이스 목록 |
| `exclude_databases` | [string] | - | 제외할 데이터베이스 목록 |

---

## 19. Prometheus Scrape Source

Prometheus 스크레이프 소스는 Prometheus 클라이언트를 통해 메트릭을 수집합니다.

### 상태 정보
- 상태: Stable
- 역할: Agent
- 전달 보장: Best Effort
- 상태 저장: Stateless

### 기본 구성

TOML 형식:
```toml
[sources.prometheus]
type = "prometheus_scrape"
endpoints = ["http://localhost:9090/metrics"]
```

YAML 형식:
```yaml
sources:
  prometheus:
    type: prometheus_scrape
    endpoints:
      - http://localhost:9090/metrics
```

### 주요 설정 옵션

| 옵션 | 타입 | 기본값 | 설명 |
|------|------|--------|------|
| `endpoints` | [string] | 필수 | Prometheus 엔드포인트 목록 |
| `scrape_interval_secs` | float | 15.0 | 수집 간격(초) |
| `scrape_timeout_secs` | float | - | 스크레이프 타임아웃 |
| `honor_labels` | bool | false | 원본 레이블 유지 |

### 고급 구성

```toml
[sources.prometheus_scrape]
type = "prometheus_scrape"
endpoints = ["http://app1:9090/metrics", "http://app2:9090/metrics"]
scrape_interval_secs = 30
scrape_timeout_secs = 10
honor_labels = true

[sources.prometheus_scrape.tls]
verify_certificate = true
```

---

## 20. Prometheus Remote Write Source

Prometheus 리모트 라이트 소스는 Prometheus 리모트 라이트 프로토콜을 통해 메트릭을 수신합니다.

### 상태 정보
- 상태: Beta
- 역할: Aggregator
- 전달 보장: At-least-once
- 상태 저장: Stateless

### 메트릭 타입 처리

리모트 라이트 프로토콜은 메트릭 태그, 타임스탬프, 숫자 값만 전송합니다. 원래 메트릭 타입(카운터, 히스토그램 등)에 대한 명시적인 정보는 포함되지 않습니다. 따라서 이 소스는 원래 메트릭 타입을 추측합니다:
- `_total` 접미사가 있는 메트릭: Counter로 처리
- 기타 모든 메트릭: Gauge로 처리

### 기본 구성

TOML 형식:
```toml
[sources.prometheus_remote_write]
type = "prometheus_remote_write"
address = "0.0.0.0:9090"
```

YAML 형식:
```yaml
sources:
  prometheus_remote_write:
    type: prometheus_remote_write
    address: 0.0.0.0:9090
```

---

## 21. Prometheus Pushgateway Source

Prometheus Pushgateway 소스는 Prometheus Pushgateway 프로토콜을 통해 메트릭을 수신합니다.

### 상태 정보
- 상태: Beta
- 역할: Aggregator
- 전달 보장: At-least-once
- 상태 저장: Stateless

### 제한사항

- POST 요청만 지원됩니다 (PUT 요청의 시맨틱스는 Vector의 아키텍처에서 구현하기 어려워 지원되지 않습니다).
- Prometheus Protobuf 형식은 현재 지원되지 않습니다. 텍스트 노출 형식으로만 메트릭을 푸시할 수 있습니다.

### 기본 구성

TOML 형식:
```toml
[sources.pushgateway]
type = "prometheus_pushgateway"
address = "0.0.0.0:9091"
```

YAML 형식:
```yaml
sources:
  pushgateway:
    type: prometheus_pushgateway
    address: 0.0.0.0:9091
```

---

## 22. StatsD Source

StatsD 소스는 StatsD 프로토콜을 통해 메트릭을 수집합니다.

### 상태 정보
- 상태: Stable
- 역할: Aggregator
- 전달 보장: Best Effort
- 상태 저장: Stateless

### 기본 구성

TOML 형식:
```toml
[sources.statsd]
type = "statsd"
address = "0.0.0.0:8125"
mode = "udp"
```

YAML 형식:
```yaml
sources:
  statsd:
    type: statsd
    address: 0.0.0.0:8125
    mode: udp
```

### 주요 설정 옵션

| 옵션 | 타입 | 기본값 | 설명 |
|------|------|--------|------|
| `address` | string | "0.0.0.0:8125" | 리스닝 주소 |
| `mode` | string | "udp" | 수신 모드 ("tcp" 또는 "udp") |

---

## 23. Splunk HEC Source

Splunk HEC 소스는 Splunk HTTP Event Collector 프로토콜을 통해 데이터를 수신합니다.

### 상태 정보
- 상태: Stable
- 역할: Aggregator
- 전달 보장: At-least-once
- 상태 저장: Stateless

### 기본 구성

TOML 형식:
```toml
[sources.splunk_hec]
type = "splunk_hec"
address = "0.0.0.0:8088"
```

YAML 형식:
```yaml
sources:
  splunk_hec:
    type: splunk_hec
    address: 0.0.0.0:8088
```

### 주요 설정 옵션

| 옵션 | 타입 | 기본값 | 설명 |
|------|------|--------|------|
| `address` | string | 필수 | 리스닝 소켓 주소 (포트 포함) |
| `token` | string | - | 인증 토큰 (설정 시 요청의 Authorization 헤더에 필요) |
| `valid_tokens` | [string] | - | 유효한 토큰 목록 |

### 인덱서 승인

acknowledgements를 활성화하면 소스는 Splunk HEC 인덱서 승인 프로토콜을 사용하여 클라이언트가 데이터가 대상 싱크에 전달되었는지 확인할 수 있습니다.

### 구성 예제

```toml
[sources.splunk_hec]
type = "splunk_hec"
address = "0.0.0.0:8088"
token = "${SPLUNK_HEC_TOKEN}"

[sources.splunk_hec.tls]
enabled = true
crt_file = "/path/to/cert.crt"
key_file = "/path/to/key.key"
```

---

## 24. AWS Kinesis Firehose Source

AWS Kinesis Firehose 소스는 AWS Kinesis Firehose에서 전달된 데이터를 수신합니다.

### 상태 정보
- 상태: Stable
- 역할: Aggregator
- 전달 보장: At-least-once
- 상태 저장: Stateless

### 기본 구성

TOML 형식:
```toml
[sources.aws_kinesis_firehose]
type = "aws_kinesis_firehose"
address = "0.0.0.0:443"
access_keys = ["A94A8FE5CCB19BA61C4C08"]
```

YAML 형식:
```yaml
sources:
  aws_kinesis_firehose:
    type: aws_kinesis_firehose
    address: 0.0.0.0:443
    access_keys:
      - A94A8FE5CCB19BA61C4C08
```

### 주요 설정 옵션

| 옵션 | 타입 | 기본값 | 설명 |
|------|------|--------|------|
| `address` | string | "0.0.0.0:443" | 리스닝 소켓 주소 |
| `access_keys` | [string] | - | 인증용 액세스 키 목록 |
| `record_compression` | string | "auto" | 레코드 압축 해제 방식 |

### 레코드 압축

AWS CloudWatch Logs와 같은 일부 서비스는 AWS Kinesis Firehose로 보내기 전에 gzip으로 이벤트를 압축합니다. `record_compression` 옵션을 사용하여 다음 컴포넌트로 전달하기 전에 자동으로 압축을 해제할 수 있습니다.

### TLS 고려사항

AWS Kinesis Firehose는 HTTP를 통해서만 데이터를 전달할 수 있습니다. 로드 밸런서를 사용하거나 TLS 옵션을 구성하여 TLS 종료를 해결해야 합니다.

---

## 25. AWS S3 Source

AWS S3 소스는 AWS S3 버킷에서 객체를 수집합니다.

### 상태 정보
- 상태: Beta
- 역할: Aggregator
- 전달 보장: At-least-once
- 상태 저장: Stateful

### 기본 구성

TOML 형식:
```toml
[sources.aws_s3]
type = "aws_s3"
region = "us-east-1"
sqs.queue_url = "https://sqs.us-east-1.amazonaws.com/123456789012/my-queue"
```

YAML 형식:
```yaml
sources:
  aws_s3:
    type: aws_s3
    region: us-east-1
    sqs:
      queue_url: https://sqs.us-east-1.amazonaws.com/123456789012/my-queue
```

---

## 26. AWS SQS Source

AWS SQS 소스는 AWS SQS 큐에서 메시지를 수집합니다.

### 상태 정보
- 상태: Beta
- 역할: Aggregator
- 전달 보장: At-least-once
- 상태 저장: Stateless

### 기본 구성

TOML 형식:
```toml
[sources.aws_sqs]
type = "aws_sqs"
region = "us-east-1"
queue_url = "https://sqs.us-east-1.amazonaws.com/123456789012/my-queue"
```

YAML 형식:
```yaml
sources:
  aws_sqs:
    type: aws_sqs
    region: us-east-1
    queue_url: https://sqs.us-east-1.amazonaws.com/123456789012/my-queue
```

---

## 27. GCP PubSub Source

GCP PubSub 소스는 GCP의 PubSub 메시징 시스템에서 옵저버빌리티 이벤트를 가져옵니다.

### 상태 정보
- 상태: Beta
- 역할: Aggregator
- 전달 보장: At-least-once
- 상태 저장: Stateless

### 요구사항

GCP Pub/Sub 소스에는 Pub/Sub 구독이 필요합니다.

### 기본 구성

TOML 형식:
```toml
[sources.gcp_pubsub]
type = "gcp_pubsub"
project = "my-log-source-project"
subscription = "my-vector-source-subscription"
```

YAML 형식:
```yaml
sources:
  gcp_pubsub:
    type: gcp_pubsub
    project: my-log-source-project
    subscription: my-vector-source-subscription
```

### 주요 설정 옵션

| 옵션 | 타입 | 기본값 | 설명 |
|------|------|--------|------|
| `project` | string | 필수 | GCP 프로젝트 ID |
| `subscription` | string | 필수 | Pub/Sub 구독 이름 |
| `max_concurrency` | int | - | 최대 동시 스트림 수 |
| `full_response_size` | int | - | 전체 응답 크기 임계값 |
| `idle_timeout_seconds` | int | - | 유휴 타임아웃(초) |

### 동시성 관리

`gcp_pubsub` 소스는 스트림을 통과하는 트래픽을 모니터링하여 동시 활성 스트림 수를 자동으로 관리합니다. 스트림이 전체 응답(`full_response_size` 설정에 의해 결정)을 받으면 "busy"로 표시됩니다. 주기적으로 소스는 모든 활성 연결을 폴링하고 모든 활성 스트림이 busy로 표시되고 `max_concurrency`보다 적은 스트림이 활성 상태인 경우 새 스트림을 시작합니다.

---

## 28. OpenTelemetry Source

OpenTelemetry 소스는 gRPC 또는 HTTP를 통해 OTLP 데이터를 수신합니다.

### 상태 정보
- 상태: Beta
- 역할: Aggregator
- 전달 보장: At-least-once
- 상태 저장: Stateless

### 지원되는 데이터 타입
- 로그
- 메트릭 (실험적)
- 트레이스

### 기본 구성

TOML 형식:
```toml
[sources.otel]
type = "opentelemetry"

[sources.otel.grpc]
address = "0.0.0.0:4317"

[sources.otel.http]
address = "0.0.0.0:4318"
```

YAML 형식:
```yaml
sources:
  otel:
    type: opentelemetry
    grpc:
      address: 0.0.0.0:4317
    http:
      address: 0.0.0.0:4318
```

### 주요 설정 옵션

| 옵션 | 타입 | 기본값 | 설명 |
|------|------|--------|------|
| `grpc.address` | string | "0.0.0.0:4317" | gRPC 서버 주소 |
| `http.address` | string | "0.0.0.0:4318" | HTTP 서버 주소 |
| `http.headers` | [string] | - | 로그 이벤트에 포함할 HTTP 헤더 |

### 메트릭 지원 참고사항

메트릭 지원은 실험적이며 내부 Vector 메트릭 데이터 모델과 OpenTelemetry 간의 구조적 차이로 인해 변경될 수 있습니다. 집계 시간성(aggregation temporality)이 OpenTelemetry 메트릭 타입에 대해 지원되는 경우:
- Delta 시간성: Incremental 메트릭 종류
- 기타: Absolute 메트릭 종류

---

## 29. Datadog Agent Source

Datadog Agent 소스는 Datadog Agent에서 수집한 로그, 메트릭, 트레이스를 수신합니다.

### 상태 정보
- 상태: Stable
- 역할: Aggregator, Sidecar
- 전달 보장: At-least-once
- 상태 저장: Stateless

### 기본 구성

TOML 형식:
```toml
[sources.datadog_agent]
type = "datadog_agent"
address = "0.0.0.0:8282"
```

YAML 형식:
```yaml
sources:
  datadog_agent:
    type: datadog_agent
    address: 0.0.0.0:8282
```

### 주요 설정 옵션

| 옵션 | 타입 | 기본값 | 설명 |
|------|------|--------|------|
| `address` | string | "0.0.0.0:8282" | 리스닝 소켓 주소 |
| `multiple_outputs` | bool | false | 데이터 타입별 출력 분리 |
| `store_api_key` | bool | false | 이벤트 메타데이터에 API 키 저장 |
| `split_metric_namespace` | bool | false | 메트릭 이름을 네임스페이스와 이름으로 분리 |

### 메트릭 네임스페이스 분리

`split_metric_namespace`가 true로 설정되면 메트릭 이름이 첫 번째 `.`에서 네임스페이스와 이름으로 분리됩니다. 예: `system.cpu.usage`는 네임스페이스 `system`과 이름 `cpu.usage`로 분리됩니다.

### API 키 저장

`store_api_key`가 true로 설정되면 수신 이벤트에 Datadog API 키가 포함된 경우 이벤트 메타데이터에 저장되며, 이후 Datadog 싱크로 전송할 때 사용됩니다.

---

## 30. Fluent Source

Fluent 소스는 FluentD, Fluent Bit 또는 fluent 프로토콜을 사용하여 전달할 수 있는 다른 서비스(예: Docker)에서 전달된 로그를 수집합니다.

### 상태 정보
- 상태: Beta
- 역할: Aggregator
- 전달 보장: Best Effort
- 상태 저장: Stateless

### 기본 구성

TOML 형식:
```toml
[sources.fluent]
type = "fluent"
address = "0.0.0.0:24224"
```

YAML 형식:
```yaml
sources:
  fluent:
    type: fluent
    address: 0.0.0.0:24224
```

### 주요 설정 옵션

| 옵션 | 타입 | 기본값 | 설명 |
|------|------|--------|------|
| `address` | string | "0.0.0.0:24224" | 리스닝 소켓 주소 |
| `mode` | string | "tcp" | 수신 모드 ("tcp" 또는 "unix") |
| `path` | string | - | Unix 소켓 경로 |

### 사용 사례

인프라에서 이미 fluent 에이전트(Fluentd 또는 Fluent Bit)를 실행 중인 경우 이 소스를 사용하여 해당 데이터를 쉽게 Vector로 가져올 수 있습니다.

---

## 31. Logstash Source

Logstash 소스는 Logstash, Elastic Beats 또는 lumberjack 프로토콜을 사용하여 전달할 수 있는 다른 서비스에서 전달된 로그를 수집합니다.

### 상태 정보
- 상태: Beta
- 역할: Aggregator
- 전달 보장: At-least-once
- 상태 저장: Stateless

### 기본 구성

TOML 형식:
```toml
[sources.logstash]
type = "logstash"
address = "0.0.0.0:5044"
```

YAML 형식:
```yaml
sources:
  logstash:
    type: logstash
    address: 0.0.0.0:5044
```

### 주요 설정 옵션

| 옵션 | 타입 | 기본값 | 설명 |
|------|------|--------|------|
| `address` | string | "0.0.0.0:5044" | 리스닝 소켓 주소 |
| `permit_origin` | [string] | - | 허용된 원본 IP/CIDR 목록 |
| `receive_buffer_bytes` | int | 65536 | 수신 버퍼 크기 |

### Logstash 구성

Logstash가 Vector 인스턴스로 전달하도록 구성하려면 lumberjack 출력 구성을 사용할 수 있습니다. 참고: Logstash는 SSL 구성이 필요합니다.

### 고급 구성

```toml
[sources.logstash]
type = "logstash"
address = "0.0.0.0:5044"
permit_origin = ["192.168.0.0/16"]
receive_buffer_bytes = 65536

[sources.logstash.tls]
enabled = true
crt_file = "/path/to/cert.crt"
key_file = "/path/to/key.key"
```

---

## 32. Redis Source

Redis 소스는 Redis에서 옵저버빌리티 데이터를 수집합니다.

### 상태 정보
- 상태: Beta
- 역할: Aggregator
- 전달 보장: Best Effort
- 상태 저장: Stateless

### 기본 구성

TOML 형식:
```toml
[sources.redis]
type = "redis"
url = "redis://127.0.0.1:6379/0"
key = "vector"
data_type = "list"
```

YAML 형식:
```yaml
sources:
  redis:
    type: redis
    url: redis://127.0.0.1:6379/0
    key: vector
    data_type: list
```

### 주요 설정 옵션

| 옵션 | 타입 | 기본값 | 설명 |
|------|------|--------|------|
| `url` | string | 필수 | Redis 서버 URL |
| `key` | string | 필수 | Redis 키 |
| `data_type` | string | "list" | 데이터 타입 ("list" 또는 "channel") |

---

## 33. AMQP Source

AMQP 소스는 RabbitMQ와 같은 AMQP 0.9.1 호환 브로커에서 이벤트를 수집합니다.

### 상태 정보
- 상태: Beta
- 역할: Aggregator
- 전달 보장: At-least-once
- 상태 저장: Stateless

### 기본 구성

TOML 형식:
```toml
[sources.amqp]
type = "amqp"
connection = "amqp://user:password@127.0.0.1:5672/%2f?timeout=10"
group_id = "consumer-group"
offset_key = "_offset"
```

YAML 형식:
```yaml
sources:
  amqp:
    type: amqp
    connection: amqp://user:password@127.0.0.1:5672/%2f?timeout=10
    group_id: consumer-group
    offset_key: _offset
```

### 연결 문자열 형식

```
amqp://<user>:<password>@<host>:<port>/<vhost>?timeout=<seconds>
```

- 기본 vhost는 `/`로 지정 (URL 인코딩: `%2f`)
- TLS 연결을 위해 `amqps` 스킴 사용 가능

---

## 34. NATS Source

NATS 소스는 NATS 메시징 시스템의 주제에서 옵저버빌리티 데이터를 읽습니다.

### 상태 정보
- 상태: Stable
- 역할: Aggregator
- 전달 보장: Best Effort
- 상태 저장: Stateless

### 기본 구성

TOML 형식:
```toml
[sources.nats]
type = "nats"
url = "nats://demo.nats.io"
subject = "events"
connection_name = "vector"
```

YAML 형식:
```yaml
sources:
  nats:
    type: nats
    url: nats://demo.nats.io
    subject: events
    connection_name: vector
```

### 주요 설정 옵션

| 옵션 | 타입 | 기본값 | 설명 |
|------|------|--------|------|
| `url` | string | 필수 | NATS 서버 URL |
| `subject` | string | 필수 | 구독할 NATS 주제 |
| `connection_name` | string | "vector" | 연결 이름 |
| `queue` | string | - | 큐 그룹 이름 |

---

## 35. MQTT Source

MQTT 소스는 MQTT 브로커에서 로그를 수신합니다.

### 상태 정보
- 상태: Beta
- 역할: Aggregator
- 전달 보장: Best Effort
- 상태 저장: Stateless

### 기본 구성

TOML 형식:
```toml
[sources.mqtt]
type = "mqtt"
host = "mqtt.example.com"
port = 1883
topic = "events/#"
```

YAML 형식:
```yaml
sources:
  mqtt:
    type: mqtt
    host: mqtt.example.com
    port: 1883
    topic: events/#
```

---

## 36. Pulsar Source

Pulsar 소스는 Apache Pulsar 토픽에서 옵저버빌리티 이벤트를 수집합니다.

### 상태 정보
- 상태: Beta
- 역할: Aggregator
- 전달 보장: At-least-once
- 상태 저장: Stateless

### 기본 구성

TOML 형식:
```toml
[sources.pulsar]
type = "pulsar"
endpoint = "pulsar://127.0.0.1:6650"
topics = ["persistent://public/default/events"]
```

YAML 형식:
```yaml
sources:
  pulsar:
    type: pulsar
    endpoint: pulsar://127.0.0.1:6650
    topics:
      - persistent://public/default/events
```

### 연결 재시도 옵션

Pulsar 클라이언트는 커스텀 연결 재시도 옵션을 지원합니다:
- 연결 설정 시간 제한
- 재연결 재시도 간 최대 지연
- 최대 연결 재시도 횟수
- 연결 재시도 간 최소 지연

---

## 37. Heroku Logplex Source

Heroku Logplex 소스는 Heroku의 Logplex(Heroku 앱에서 로그를 수신하는 라우터)에서 로그를 수집합니다.

### 상태 정보
- 상태: Stable
- 역할: Aggregator
- 전달 보장: At-least-once
- 상태 저장: Stateless

### 기본 구성

TOML 형식:
```toml
[sources.heroku]
type = "heroku_logs"
address = "0.0.0.0:8080"
```

YAML 형식:
```yaml
sources:
  heroku:
    type: heroku_logs
    address: 0.0.0.0:8080
```

### 주요 설정 옵션

| 옵션 | 타입 | 기본값 | 설명 |
|------|------|--------|------|
| `address` | string | "0.0.0.0:80" | 리스닝 소켓 주소 |
| `query_parameters` | [string] | - | 이벤트에 포함할 쿼리 파라미터 |

---

## 38. Vector Source

Vector 소스는 다른 Vector 인스턴스에서 데이터를 수신합니다.

### 상태 정보
- 상태: Stable
- 역할: Aggregator
- 전달 보장: Best Effort
- 상태 저장: Stateless

### 기본 구성

TOML 형식:
```toml
[sources.vector]
type = "vector"
address = "0.0.0.0:9000"
```

YAML 형식:
```yaml
sources:
  vector:
    type: vector
    address: 0.0.0.0:9000
```

### 주요 설정 옵션

| 옵션 | 타입 | 기본값 | 설명 |
|------|------|--------|------|
| `address` | string | "0.0.0.0:9000" | 리스닝 소켓 주소 |
| `version` | string | "2" | Vector 프로토콜 버전 |

---

## 39. WebSocket Source

WebSocket 소스는 WebSocket 서버에서 이벤트를 수신합니다.

### 상태 정보
- 상태: Beta
- 역할: Aggregator
- 전달 보장: Best Effort
- 상태 저장: Stateless

### 기본 구성

TOML 형식:
```toml
[sources.websocket]
type = "websocket"
url = "ws://example.com/events"
```

YAML 형식:
```yaml
sources:
  websocket:
    type: websocket
    url: ws://example.com/events
```

### 주요 설정 옵션

| 옵션 | 타입 | 기본값 | 설명 |
|------|------|--------|------|
| `url` | string | 필수 | WebSocket 서버 URI |
| `ping_interval` | int | - | 핑 간격(초) |
| `ping_timeout` | int | - | Pong 응답 대기 시간(초) |

---

## 40. HTTP Client Source

HTTP Client 소스는 HTTP 엔드포인트에서 이벤트를 가져옵니다.

### 상태 정보
- 상태: Beta
- 역할: Aggregator
- 전달 보장: Best Effort
- 상태 저장: Stateless

### 기본 구성

TOML 형식:
```toml
[sources.http_client]
type = "http_client"
endpoint = "http://example.com/api/events"
scrape_interval_secs = 30
```

YAML 형식:
```yaml
sources:
  http_client:
    type: http_client
    endpoint: http://example.com/api/events
    scrape_interval_secs: 30
```

### 주요 설정 옵션

| 옵션 | 타입 | 기본값 | 설명 |
|------|------|--------|------|
| `endpoint` | string | 필수 | 수집할 HTTP 엔드포인트 |
| `scrape_interval_secs` | float | 15.0 | 수집 간격(초) |
| `method` | string | "GET" | HTTP 메서드 |
| `headers` | object | - | 요청 헤더 |

---

## 41. dnstap Source

dnstap 소스는 dnstap 소켓에서 DNS 트래픽 데이터를 수집합니다.

### 상태 정보
- 상태: Beta
- 역할: Agent
- 전달 보장: Best Effort
- 상태 저장: Stateless

### 기본 구성

TOML 형식:
```toml
[sources.dnstap]
type = "dnstap"
mode = "unix"
socket_path = "/var/run/dnstap.sock"
```

YAML 형식:
```yaml
sources:
  dnstap:
    type: dnstap
    mode: unix
    socket_path: /var/run/dnstap.sock
```

### 주요 설정 옵션

| 옵션 | 타입 | 기본값 | 설명 |
|------|------|--------|------|
| `mode` | string | 필수 | 소켓 타입 ("tcp" 또는 "unix") |
| `socket_path` | string | - | Unix 소켓 경로 |
| `address` | string | - | TCP 리스닝 주소 |
| `max_connections` | int | - | 최대 TCP 연결 수 |
| `max_frame_length` | int | - | 최대 DNSTAP 프레임 길이 |

---

## 42. EventStoreDB Metrics Source

EventStoreDB 메트릭 소스는 EventStoreDB에서 메트릭을 수집합니다.

### 상태 정보
- 상태: Beta
- 역할: Agent
- 전달 보장: Best Effort
- 상태 저장: Stateless

### 기본 구성

TOML 형식:
```toml
[sources.eventstoredb_metrics]
type = "eventstoredb_metrics"
endpoint = "https://localhost:2113/stats"
```

YAML 형식:
```yaml
sources:
  eventstoredb_metrics:
    type: eventstoredb_metrics
    endpoint: https://localhost:2113/stats
```

### 주요 설정 옵션

| 옵션 | 타입 | 기본값 | 설명 |
|------|------|--------|------|
| `endpoint` | string | "https://localhost:2113/stats" | 통계 스크레이프 엔드포인트 |
| `namespace` | string | "eventstoredb" | 메트릭 네임스페이스 |
| `scrape_interval_secs` | float | 15.0 | 수집 간격(초) |

---

## 43. Okta Source

Okta 소스는 Okta에서 시스템 로그를 수집합니다.

### 상태 정보
- 상태: Beta
- 역할: Aggregator
- 전달 보장: At-least-once
- 상태 저장: Stateful

### 기본 구성

TOML 형식:
```toml
[sources.okta]
type = "okta"
url = "https://your-org.okta.com"
token = "${OKTA_API_TOKEN}"
```

YAML 형식:
```yaml
sources:
  okta:
    type: okta
    url: https://your-org.okta.com
    token: ${OKTA_API_TOKEN}
```

---

## 44. Static Metrics Source

Static 메트릭 소스는 정적 메트릭 값을 생성합니다.

### 상태 정보
- 상태: Stable
- 역할: Agent
- 전달 보장: Best Effort
- 상태 저장: Stateless

### 기본 구성

TOML 형식:
```toml
[sources.static_metrics]
type = "static_metrics"
namespace = "myapp"
interval = 60

[[sources.static_metrics.metrics]]
name = "version"
kind = "gauge"
value = 1.0

[sources.static_metrics.metrics.tags]
version = "1.2.3"
```

YAML 형식:
```yaml
sources:
  static_metrics:
    type: static_metrics
    namespace: myapp
    interval: 60
    metrics:
      - name: version
        kind: gauge
        value: 1.0
        tags:
          version: "1.2.3"
```

---

## 공통 설정 옵션

### 디코딩 (Decoding)

많은 소스에서 디코딩 옵션을 지원합니다:

```toml
[sources.my_source.decoding]
codec = "json"  # "bytes", "json", "syslog", "native", "native_json", "gelf", "protobuf", "avro", "vrl"
```

#### JSON 디코딩
```toml
[sources.my_source.decoding]
codec = "json"

[sources.my_source.decoding.json]
lossy = true  # 유효하지 않은 UTF-8 시퀀스를 대체 문자로 교체
```

#### Protobuf 디코딩
```toml
[sources.my_source.decoding]
codec = "protobuf"

[sources.my_source.decoding.protobuf]
desc_file = "/path/to/descriptor.pb"
message_type = "my.package.MyMessage"
```

#### VRL 디코딩
```toml
[sources.my_source.decoding]
codec = "vrl"

[sources.my_source.decoding.vrl]
source = """
. = parse_json!(.message)
.timestamp = now()
"""
```

### 프레이밍 (Framing)

이벤트가 원시 바이트 형식으로 인코딩될 때 이벤트가 구분되는 방식을 구성합니다:

```toml
[sources.my_source.framing]
method = "newline_delimited"  # "bytes", "character_delimited", "length_delimited", "newline_delimited", "octet_counting"
```

#### Character Delimited 프레이밍
```toml
[sources.my_source.framing]
method = "character_delimited"

[sources.my_source.framing.character_delimited]
delimiter = "|"
max_length = 102400
```

### TLS 설정

많은 소스에서 TLS 옵션을 지원합니다:

```toml
[sources.my_source.tls]
enabled = true
crt_file = "/path/to/cert.crt"
key_file = "/path/to/key.key"
ca_file = "/path/to/ca.crt"
verify_certificate = true
verify_hostname = true
```

### 인증 (Authentication)

#### Basic 인증
```toml
[sources.my_source.auth]
strategy = "basic"
user = "username"
password = "password"
```

#### Bearer 토큰 인증
```toml
[sources.my_source.auth]
strategy = "bearer"
token = "${API_TOKEN}"
```

---

## 참고 자료

- [Vector 공식 문서 - Sources](https://vector.dev/docs/reference/configuration/sources/)
- [Vector 공식 문서 - File Source](https://vector.dev/docs/reference/configuration/sources/file/)
- [Vector 공식 문서 - Kafka Source](https://vector.dev/docs/reference/configuration/sources/kafka/)
- [Vector 공식 문서 - HTTP Server Source](https://vector.dev/docs/reference/configuration/sources/http_server/)
- [Vector 공식 문서 - Kubernetes Logs Source](https://vector.dev/docs/reference/configuration/sources/kubernetes_logs/)
- [Vector 공식 문서 - Syslog Source](https://vector.dev/docs/reference/configuration/sources/syslog/)
- [Vector 공식 문서 - Docker Logs Source](https://vector.dev/docs/reference/configuration/sources/docker_logs/)
- [Vector 공식 문서 - JournalD Source](https://vector.dev/docs/reference/configuration/sources/journald/)
- [Vector 공식 문서 - OpenTelemetry Source](https://vector.dev/docs/reference/configuration/sources/opentelemetry/)
- [Vector 공식 문서 - Prometheus Scrape Source](https://vector.dev/docs/reference/configuration/sources/prometheus_scrape/)
