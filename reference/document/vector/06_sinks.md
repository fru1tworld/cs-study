# Vector Sinks (싱크) 가이드

## 개요

Sinks 는 Vector에서 데이터를 외부 서비스나 목적지로 전송하는 컴포넌트입니다. Vector 토폴로지는 세 가지 유형의 컴포넌트로 구성됩니다:
- Sources: 관측 데이터 소스로부터 데이터를 수집하거나 수신
- Transforms: 토폴로지를 통과하는 관측 데이터를 조작하거나 변경
- Sinks: Vector에서 외부 서비스나 목적지로 데이터를 전송

---

## 지원되는 Sinks 전체 목록

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

## 공통 설정 옵션

대부분의 싱크에서 공유하는 공통 설정 패턴입니다.

### 1. Acknowledgements (승인)

승인 기능은 end-to-end 데이터 전송 보장을 위해 사용됩니다.

```toml
[sinks.my_sink]
type = "elasticsearch"
inputs = ["my_source"]

[sinks.my_sink.acknowledgements]
enabled = true
```

설명:
- 싱크에서 승인이 활성화되면, end-to-end 승인을 지원하는 모든 연결된 소스는 해당 싱크가 이벤트를 승인할 때까지 기다립니다
- 싱크 레벨의 승인 설정은 전역 승인 설정보다 우선합니다

### 2. Batching (배칭)

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

### 3. Buffering (버퍼링)

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

### 4. Health Checks (헬스 체크)

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

### 5. Inputs (입력)

업스트림 소스 또는 트랜스폼 ID 목록을 지정합니다.

```toml
[sinks.my_sink]
type = "console"
inputs = ["my_source", "my_transform"]    # 특정 ID 지정
# inputs = ["*"]                          # 와일드카드 지원
```

---

## 주요 Sinks 상세 설정

### 1. Elasticsearch Sink

Elasticsearch 클러스터로 로그 이벤트를 전송합니다.

상태: Stable | 전송 보장: At-least-once | 승인 지원: Yes

#### 기본 설정

```toml
[sinks.elasticsearch]
type = "elasticsearch"
inputs = ["my_source"]
endpoints = ["http://localhost:9200"]

[sinks.elasticsearch.bulk]
index = "vector-%Y-%m-%d"    # 일별 인덱스
```

#### 인증 설정

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

#### AWS OpenSearch 설정

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

### 2. Kafka Sink

Apache Kafka 토픽으로 이벤트를 발행합니다.

상태: Stable | 전송 보장: At-least-once | 승인 지원: Yes

#### 기본 설정

```toml
[sinks.kafka]
type = "kafka"
inputs = ["my_source"]
bootstrap_servers = "localhost:9092"
topic = "my-topic"

[sinks.kafka.encoding]
codec = "json"
```

#### 고급 설정

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

#### SASL 인증 설정

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

### 3. HTTP Sink

HTTP 엔드포인트로 이벤트를 전송합니다.

상태: Stable | 전송 보장: At-least-once | 승인 지원: Yes

#### 기본 설정

```toml
[sinks.http]
type = "http"
inputs = ["my_source"]
uri = "http://localhost:8080/logs"

[sinks.http.encoding]
codec = "json"
```

#### 인증 및 헤더 설정

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

#### Basic 인증 설정

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

### 4. File Sink

로컬 파일 시스템에 이벤트를 기록합니다.

상태: Stable | 전송 보장: At-least-once | 승인 지원: Yes

#### 기본 설정

```toml
[sinks.file]
type = "file"
inputs = ["my_source"]
path = "/var/log/vector/%Y-%m-%d.log"

[sinks.file.encoding]
codec = "json"
```

#### 고급 설정

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

#### 텍스트 인코딩 설정

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

### 5. Console Sink

표준 출력(stdout/stderr)으로 이벤트를 출력합니다. 디버깅에 유용합니다.

상태: Stable | 전송 보장: Best-effort | 승인 지원: No

#### 기본 설정

```toml
[sinks.console]
type = "console"
inputs = ["my_source"]
target = "stdout"

[sinks.console.encoding]
codec = "json"
```

#### 텍스트 출력 설정

```toml
[sinks.console]
type = "console"
inputs = ["my_source"]
target = "stdout"

[sinks.console.encoding]
codec = "text"
```

#### 전체 예제

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

### 6. Loki Sink

Grafana Loki 로그 집계 시스템으로 로그를 전송합니다.

상태: Stable | 전송 보장: At-least-once | 승인 지원: Yes

#### 기본 설정

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

#### 고급 설정

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

#### 인증 설정 (Grafana Cloud)

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
- `labels`: 각 이벤트 배치에 첨부될 레이블 (키-값 쌍)
- `out_of_order_action`: 순서 이탈 로그 처리 방법 (accept, drop, rewrite_timestamp)
- `tenant_id`: 멀티 테넌시 환경에서의 테넌트 ID

주의: 레이블의 카디널리티가 높으면 Loki에 심각한 성능 문제가 발생할 수 있습니다. 고유 레이블 키와 값의 수를 줄이는 것이 좋습니다.

---

### 7. Datadog Sinks

Datadog 플랫폼으로 로그, 메트릭, 이벤트, 트레이스를 전송합니다.

#### 7.1 Datadog Logs Sink

상태: Stable | 전송 보장: At-least-once

```toml
[sinks.datadog_logs]
type = "datadog_logs"
inputs = ["my_source"]
default_api_key = "${DATADOG_API_KEY}"
site = "datadoghq.com"
compression = "gzip"
```

#### 7.2 Datadog Metrics Sink

```toml
[sinks.datadog_metrics]
type = "datadog_metrics"
inputs = ["my_metrics"]
default_api_key = "${DATADOG_API_KEY}"
site = "datadoghq.com"
```

#### 7.3 Datadog Events Sink

```toml
[sinks.datadog_events]
type = "datadog_events"
inputs = ["my_events"]
default_api_key = "${DATADOG_API_KEY}"
site = "datadoghq.com"
```

#### 7.4 Datadog Traces Sink

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
- 특수 필드: `ddsource`, `ddtags`, `hostname`, `message`, `service`가 이벤트에 있으면 API 참조에 따라 처리됩니다.

---

### 8. AWS S3 Sink

AWS S3 객체 스토리지에 이벤트를 저장합니다.

상태: Stable | 전송 보장: At-least-once | 승인 지원: Yes

#### 기본 설정

```toml
[sinks.s3]
type = "aws_s3"
inputs = ["my_source"]
bucket = "my-log-bucket"
region = "us-east-1"

[sinks.s3.encoding]
codec = "json"
```

#### 고급 설정

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

#### IAM 역할 기반 인증

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
- `bucket`: S3 버킷 이름 (앞에 `s3://` 또는 뒤에 `/` 제외)
- `key_prefix`: 객체 키 접두사 (디렉토리 구조로 사용 시 `/`로 끝나야 함)
- `grant_full_control`: 교차 계정 액세스 시 버킷 소유자의 정식 사용자 ID

---

### 9. AWS CloudWatch Logs Sink

AWS CloudWatch Logs로 로그를 전송합니다.

상태: Stable | 전송 보장: At-least-once | 승인 지원: Yes

#### 기본 설정

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

#### 고급 설정

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

### 10. Prometheus Sinks

Prometheus와 통합하기 위한 두 가지 싱크를 제공합니다.

#### 10.1 Prometheus Exporter Sink

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

#### 10.2 Prometheus Remote Write Sink

Prometheus Remote Write 프로토콜을 사용하여 메트릭을 전송합니다.

상태: Stable | 전송 보장: At-least-once

```toml
[sinks.prometheus_remote_write]
type = "prometheus_remote_write"
inputs = ["my_metrics"]
endpoint = "https://prometheus.example.com/api/v1/write"
default_namespace = "vector"
```

#### Grafana Cloud 설정

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

압축 옵션: 공식적으로 Snappy만 지원되지만, Vector는 Gzip과 Zstd도 지원합니다.

주의: 높은 카디널리티의 메트릭 이름과 레이블은 Prometheus에서 성능 문제를 일으킬 수 있습니다. `tag_cardinality_limit` 트랜스폼 사용을 고려하세요.

---

### 11. InfluxDB Sinks

InfluxDB로 로그와 메트릭을 전송합니다.

#### 11.1 InfluxDB Metrics Sink

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

#### 11.2 InfluxDB Logs Sink

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

#### InfluxDB v1.x 설정

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

### 12. Splunk HEC Sinks

Splunk HTTP Event Collector로 로그와 메트릭을 전송합니다.

#### Splunk HEC Logs Sink

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

#### 고급 설정

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

#### Splunk HEC Metrics Sink

```toml
[sinks.splunk_metrics]
type = "splunk_hec_metrics"
inputs = ["my_metrics"]
endpoint = "https://splunk.example.com:8088"
default_token = "${SPLUNK_HEC_TOKEN}"
index = "metrics"
```

인덱서 승인:
Splunk HEC 토큰에 인덱서 승인 기능이 활성화되어 있으면, 싱크는 자동으로 통합되어 데이터 전송 성공을 확인합니다.

---

### 13. ClickHouse Sink

ClickHouse 데이터베이스로 로그를 전송합니다.

상태: Stable | 전송 보장: At-least-once | 승인 지원: Yes

#### 기본 설정

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

#### 고급 설정

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

### 14. Redis Sink

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

#### TLS 연결

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

### 15. NATS Sink

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

#### 인증 설정

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

### 16. Pulsar Sink

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

#### 고급 설정

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

### 17. GCP Sinks

Google Cloud Platform 서비스로 데이터를 전송합니다.

#### 17.1 GCP PubSub Sink

```toml
[sinks.gcp_pubsub]
type = "gcp_pubsub"
inputs = ["my_source"]
project = "my-gcp-project"
topic = "my-topic"

[sinks.gcp_pubsub.encoding]
codec = "json"
```

#### 17.2 GCP Cloud Storage Sink

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

#### 고급 GCS 설정

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

인증: API 키 또는 서비스 계정 JSON 파일 경로를 지정하거나, `GOOGLE_APPLICATION_CREDENTIALS` 환경 변수를 사용합니다.

---

### 18. Azure Sinks

Azure 서비스로 데이터를 전송합니다.

#### 18.1 Azure Blob Storage Sink

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

#### 18.2 Azure Monitor Logs Sink

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

### 19. Socket Sink

TCP, UDP 또는 Unix 소켓으로 이벤트를 전송합니다.

상태: Stable | 전송 보장: Best-effort

#### TCP 소켓

```toml
[sinks.socket]
type = "socket"
inputs = ["my_source"]
address = "192.168.1.100:5000"
mode = "tcp"

[sinks.socket.encoding]
codec = "json"
```

#### Unix 소켓

```toml
[sinks.socket]
type = "socket"
inputs = ["my_source"]
path = "/var/run/vector.sock"
mode = "unix"

[sinks.socket.encoding]
codec = "json"
```

#### UDP 소켓

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

### 20. WebSocket Sinks

WebSocket을 통해 이벤트를 전송합니다.

#### 20.1 WebSocket Sink (클라이언트)

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

#### TLS 연결

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

#### 20.2 WebSocket Server Sink

```toml
[sinks.websocket_server]
type = "websocket_server"
inputs = ["my_source"]
address = "0.0.0.0:8080"

[sinks.websocket_server.encoding]
codec = "json"
```

---

### 21. OpenTelemetry Sink

OTLP 프로토콜을 통해 로그, 메트릭, 트레이스를 전송합니다.

상태: Beta | 전송 보장: At-least-once | 승인 지원: Yes

```toml
[sinks.otel]
type = "opentelemetry"
inputs = ["my_source"]

[sinks.otel.protocol]
type = "http"
uri = "http://localhost:4318/v1/logs"

[sinks.otel.encoding]
codec = "otlp"
```

#### gRPC 프로토콜

```toml
[sinks.otel]
type = "opentelemetry"
inputs = ["my_source"]

[sinks.otel.protocol]
type = "grpc"
uri = "http://localhost:4317"

[sinks.otel.encoding]
codec = "otlp"
```

입력 형식: `<component_id>.logs`, `<component_id>.metrics`, `<component_id>.traces` 형식으로 입력을 지정합니다.

---

### 22. New Relic Sink

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

#### 메트릭 전송

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

### 23. Honeycomb Sink

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

### 24. Blackhole Sink

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

### 25. Vector Sink

다른 Vector 인스턴스로 이벤트를 전송합니다. 분산 아키텍처에 유용합니다.

상태: Stable | 전송 보장: At-least-once | 승인 지원: Yes

```toml
[sinks.vector]
type = "vector"
inputs = ["my_source"]
address = "aggregator.example.com:9000"
```

#### TLS 설정

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

### 26. AMQP Sink

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

### 27. MQTT Sink

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

#### TLS 및 인증

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

### 28. StatsD Sink

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

#### TCP 모드

```toml
[sinks.statsd]
type = "statsd"
inputs = ["my_metrics"]
address = "statsd.example.com:8125"
mode = "tcp"
```

---

### 29. Papertrail Sink

Papertrail로 로그를 전송합니다.

상태: Stable | 전송 보장: Best-effort

```toml
[sinks.papertrail]
type = "papertrail"
inputs = ["my_source"]
endpoint = "logs.papertrailapp.com:12345"
process = "{{ application }}"
```

설정: Papertrail에서 Log Destination을 생성하고 TCP를 활성화한 후 엔드포인트를 설정합니다.

---

### 30. Sematext Sinks

Sematext로 로그와 메트릭을 전송합니다.

#### Sematext Logs Sink

```toml
[sinks.sematext_logs]
type = "sematext_logs"
inputs = ["my_source"]
token = "${SEMATEXT_TOKEN}"
region = "us"
```

#### Sematext Metrics Sink

```toml
[sinks.sematext_metrics]
type = "sematext_metrics"
inputs = ["my_metrics"]
token = "${SEMATEXT_TOKEN}"
region = "us"
default_namespace = "vector"
```

참고: Sematext 모니터링은 단일 값을 포함하는 메트릭만 허용하므로, counter와 gauge 메트릭만 지원됩니다.

---

## 설정 형식

Vector는 세 가지 설정 형식을 지원합니다:

### TOML (권장)
```toml
[sinks.my_sink]
type = "console"
inputs = ["my_source"]

[sinks.my_sink.encoding]
codec = "json"
```

### YAML
```yaml
sinks:
  my_sink:
    type: console
    inputs:
      - my_source
    encoding:
      codec: json
```

### JSON
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

## 실전 예제

### 멀티 싱크 파이프라인

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

### 조건부 라우팅

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

## 참고 자료

- [Vector 공식 문서 - Sinks](https://vector.dev/docs/reference/configuration/sinks/)
- [Vector 컴포넌트 목록](https://vector.dev/components/)
- [Vector 설정 가이드](https://vector.dev/docs/reference/configuration/)
- [Vector GitHub 저장소](https://github.com/vectordotdev/vector)
