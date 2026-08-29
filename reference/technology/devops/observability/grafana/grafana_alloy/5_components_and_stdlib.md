# Alloy 컴포넌트 레퍼런스와 표준 라이브러리

## Alloy 컴포넌트 레퍼런스

> 원본: https://grafana.com/docs/alloy/latest/reference/components/

---

### 목차

1. [컴포넌트 카테고리](#컴포넌트-카테고리)
2. [discovery.*](#discovery)
3. [prometheus.*](#prometheus)
4. [loki.*](#loki)
5. [otelcol.*](#otelcol)
6. [pyroscope.*](#pyroscope)
7. [local.* / remote.*](#local--remote)
8. [기타 유틸리티](#기타-유틸리티)

---

### 컴포넌트 카테고리

- `discovery.*`: 25개 이상, 서비스 디스커버리 용도
- `prometheus.*`: 60개 이상, 메트릭 용도(Exporters·Scrape·Relabel·Remote Write)
- `loki.*`: 25개 이상, 로그 용도(Source·Process·Write)
- `otelcol.*`: 80개 이상, OpenTelemetry 용도(Receiver·Processor·Exporter)
- `pyroscope.*`: 10개 이상, 프로파일 용도
- `local.*`: 5개 이상, 로컬 리소스 용도(파일 등)
- `remote.*`: 5개 이상, 원격 리소스 용도(HTTP·S3·Vault·Kubernetes)
- `mimir.*`: 5개 이상, Mimir 전용
- `faro.*`: 1개 이상, 프론트엔드 용도
- `beyla.*`: 1개 이상, eBPF 자동 계측 용도

---

### discovery.*

#### discovery.kubernetes

```alloy
discovery.kubernetes "pods" {
  role = "pod"   // pod, service, endpoints, endpointslice, node, ingress
  
  // 필터
  namespaces {
    own_namespace = false
    names         = ["default", "production"]
  }
  
  selectors {
    role  = "pod"
    label = "app=myapp"
    field = "status.phase=Running"
  }
}
```

#### discovery.docker

```alloy
discovery.docker "containers" {
  host             = "unix:///var/run/docker.sock"
  refresh_interval = "5s"
  filter {
    name   = "status"
    values = ["running"]
  }
}
```

#### discovery.consul

```alloy
discovery.consul "default" {
  server     = "consul:8500"
  datacenter = "dc1"
  services   = ["api", "web"]
  tags       = ["production"]
}
```

#### discovery.dns

```alloy
discovery.dns "service" {
  names = ["service.example.com"]
  type  = "A"   // A, AAAA, SRV
  port  = 80
  refresh_interval = "30s"
}
```

#### discovery.relabel

```alloy
discovery.relabel "default" {
  targets = discovery.kubernetes.pods.targets
  
  rule {
    source_labels = ["__meta_kubernetes_pod_label_app"]
    regex         = "myapp"
    action        = "keep"
  }
  
  rule {
    source_labels = ["__meta_kubernetes_namespace"]
    target_label  = "namespace"
  }
}
```

#### discovery.ec2

```alloy
discovery.ec2 "ec2" {
  region = "us-east-1"
  port   = 9100
  
  filter {
    name   = "tag:Environment"
    values = ["production"]
  }
}
```

---

### prometheus.*

#### prometheus.scrape

```alloy
prometheus.scrape "default" {
  targets    = discovery.kubernetes.pods.targets
  forward_to = [prometheus.remote_write.mimir.receiver]
  
  job_name        = "kubernetes"
  scrape_interval = "15s"
  scrape_timeout  = "10s"
  metrics_path    = "/metrics"
  scheme          = "http"
  
  basic_auth {
    username = "user"
    password = sys.env("PASSWORD")
  }
  
  tls_config {
    ca_file   = "/etc/ssl/ca.crt"
    cert_file = "/etc/ssl/client.crt"
    key_file  = "/etc/ssl/client.key"
  }
  
  clustering {
    enabled = true
  }
}
```

#### prometheus.remote_write

```alloy
prometheus.remote_write "mimir" {
  endpoint {
    url = "http://mimir:9009/api/v1/push"
    
    headers = {
      "X-Scope-OrgID" = "tenant-1",
    }
    
    basic_auth {
      username = "user"
      password = sys.env("PASSWORD")
    }
    
    queue_config {
      capacity            = 10000
      max_samples_per_send = 2000
      max_shards          = 30
      min_shards          = 1
      batch_send_deadline = "5s"
    }
    
    metadata_config {
      send          = true
      send_interval = "1m"
    }
    
    write_relabel_config {
      source_labels = ["__name__"]
      regex         = "go_.*"
      action        = "drop"
    }
  }
  
  external_labels = {
    cluster = "prod",
  }
  
  wal {
    truncate_frequency = "2h"
    min_keepalive_time = "5m"
    max_keepalive_time = "8h"
  }
}
```

#### prometheus.relabel

```alloy
prometheus.relabel "drop_internal" {
  forward_to = [prometheus.remote_write.mimir.receiver]
  
  rule {
    source_labels = ["__name__"]
    regex         = "go_(memstats|gc).*"
    action        = "drop"
  }
}
```

#### prometheus.exporter.*

내장 Exporter 목록:

- Node: `prometheus.exporter.unix`
- Windows: `prometheus.exporter.windows`
- Cloudwatch: `prometheus.exporter.cloudwatch`
- MySQL: `prometheus.exporter.mysql`
- PostgreSQL: `prometheus.exporter.postgres`
- Redis: `prometheus.exporter.redis`
- MongoDB: `prometheus.exporter.mongodb`
- Kafka: `prometheus.exporter.kafka`
- Memcached: `prometheus.exporter.memcached`
- Process: `prometheus.exporter.process`
- SNMP: `prometheus.exporter.snmp`
- Blackbox: `prometheus.exporter.blackbox`
- GitHub: `prometheus.exporter.github`
- Statsd: `prometheus.exporter.statsd`

예: Unix Exporter

```alloy
prometheus.exporter.unix "default" {
  set_collectors = ["cpu", "memory", "diskstats", "filesystem", "loadavg", "meminfo", "netstat"]
  
  filesystem {
    fs_types_exclude = "^(autofs|binfmt_misc|cgroup|cgroup2|debugfs|devpts|devtmpfs|fusectl|hugetlbfs|mqueue|nsfs|overlay|proc|procfs|pstore|rpc_pipefs|securityfs|selinuxfs|squashfs|sysfs|tracefs)$"
  }
}
```

---

### loki.*

#### loki.source.file

```alloy
loki.source.file "files" {
  targets    = [
    {__path__ = "/var/log/app.log", job = "app"},
    {__path__ = "/var/log/nginx/*.log", job = "nginx"},
  ]
  forward_to = [loki.write.default.receiver]
  
  decompression {
    enabled       = true
    initial_delay = "10s"
    format        = "gz"
  }
}
```

#### loki.source.kubernetes

```alloy
loki.source.kubernetes "pods" {
  targets    = discovery.kubernetes.pods.targets
  forward_to = [loki.write.default.receiver]
}
```

#### loki.source.journal

```alloy
loki.source.journal "journal" {
  forward_to    = [loki.write.default.receiver]
  max_age       = "12h"
  path          = "/var/log/journal"
  matches       = "_SYSTEMD_UNIT=ssh.service"
  format_as_json = false
  labels = {
    job = "systemd",
  }
}
```

#### loki.source.syslog

```alloy
loki.source.syslog "syslog" {
  listener {
    address  = "0.0.0.0:1514"
    protocol = "tcp"
    
    tls_config {
      cert_file = "/etc/certs/server.crt"
      key_file  = "/etc/certs/server.key"
    }
  }
  
  forward_to = [loki.write.default.receiver]
  labels = {
    job = "syslog",
  }
}
```

#### loki.process

```alloy
loki.process "default" {
  forward_to = [loki.write.default.receiver]
  
  // JSON 파싱
  stage.json {
    expressions = {
      level    = "level",
      msg      = "message",
      duration = "duration",
    }
  }
  
  // 라벨 추출
  stage.labels {
    values = {
      level = "",
    }
  }
  
  // logfmt 파싱
  stage.logfmt {
    mapping = {
      method = "",
      path   = "",
      status = "",
    }
  }
  
  // 정규식
  stage.regex {
    expression = "(?P<ip>\\S+) - - \\[(?P<ts>[^\\]]+)\\] \"(?P<method>\\S+)"
  }
  
  // 타임스탬프 파싱
  stage.timestamp {
    source = "ts"
    format = "RFC3339"
  }
  
  // 라인 변환
  stage.output {
    source = "msg"
  }
  
  // 메트릭 생성
  stage.metrics {
    error_count {
      type        = "Counter"
      description = "Total errors"
      source      = "level"
      config {
        match_all = false
        value     = "error"
        action    = "inc"
      }
    }
  }
}
```

#### loki.relabel

```alloy
loki.relabel "default" {
  forward_to = [loki.write.default.receiver]
  
  rule {
    source_labels = ["__path__"]
    regex         = ".*\\/(?P<filename>[^\\/]+)\\.log"
    target_label  = "filename"
    replacement   = "$1"
  }
}
```

#### loki.write

```alloy
loki.write "default" {
  endpoint {
    url = "http://loki:3100/loki/api/v1/push"
    
    headers = {
      "X-Scope-OrgID" = "tenant-1",
    }
    
    basic_auth {
      username = "user"
      password = sys.env("PASSWORD")
    }
    
    batch_size = "1MiB"
    batch_wait = "1s"
    
    max_backoff_period   = "5m"
    max_backoff_retries  = 10
  }
  
  external_labels = {
    cluster = "prod",
  }
}
```

---

### otelcol.*

#### otelcol.receiver.otlp

```alloy
otelcol.receiver.otlp "default" {
  grpc {
    endpoint = "0.0.0.0:4317"
    max_recv_msg_size = "16MiB"
  }
  http {
    endpoint = "0.0.0.0:4318"
  }
  
  output {
    metrics = [otelcol.processor.batch.default.input]
    logs    = [otelcol.processor.batch.default.input]
    traces  = [otelcol.processor.batch.default.input]
  }
}
```

#### otelcol.processor.batch

```alloy
otelcol.processor.batch "default" {
  send_batch_size      = 1024
  send_batch_max_size  = 2048
  timeout              = "5s"
  
  output {
    metrics = [otelcol.exporter.prometheus.mimir.input]
    logs    = [otelcol.exporter.loki.default.input]
    traces  = [otelcol.exporter.otlp.tempo.input]
  }
}
```

#### otelcol.processor.tail_sampling

```alloy
otelcol.processor.tail_sampling "default" {
  decision_wait = "10s"
  num_traces    = 50000
  
  policy {
    name = "errors"
    type = "status_code"
    status_code {
      status_codes = ["ERROR"]
    }
  }
  
  policy {
    name = "slow"
    type = "latency"
    latency {
      threshold_ms = 1000
    }
  }
  
  output {
    traces = [otelcol.exporter.otlp.tempo.input]
  }
}
```

#### otelcol.connector.spanmetrics

스팬으로부터 메트릭을 자동으로 생성함.

```alloy
otelcol.connector.spanmetrics "default" {
  histogram {
    explicit {
      buckets = ["100us", "1ms", "5ms", "10ms", "100ms", "500ms", "1s", "5s"]
    }
  }
  
  dimension {
    name = "http.method"
  }
  dimension {
    name = "http.status_code"
  }
  
  output {
    metrics = [otelcol.exporter.prometheus.mimir.input]
  }
}
```

#### otelcol.exporter.otlp

```alloy
otelcol.exporter.otlp "tempo" {
  client {
    endpoint = "tempo:4317"
    tls {
      insecure = true
    }
    
    sending_queue {
      enabled       = true
      num_consumers = 10
      queue_size    = 5000
    }
    
    retry_on_failure {
      enabled          = true
      initial_interval = "5s"
      max_interval     = "30s"
      max_elapsed_time = "5m"
    }
  }
}
```

---

### pyroscope.*

#### pyroscope.scrape

```alloy
pyroscope.scrape "default" {
  targets    = [
    {__address__ = "localhost:8080", service_name = "myapp"},
  ]
  forward_to = [pyroscope.write.default.receiver]
  
  profiling_config {
    profile.cpu {
      enabled = true
    }
    profile.memory {
      enabled = true
    }
  }
}

pyroscope.write "default" {
  endpoint {
    url = "http://pyroscope:4040"
  }
}
```

#### pyroscope.ebpf

```alloy
pyroscope.ebpf "default" {
  forward_to = [pyroscope.write.default.receiver]
  
  targets = discovery.kubernetes.pods.targets
  
  collect_user_profile   = true
  collect_kernel_profile = false
}
```

---

### local.* / remote.*

#### local.file

파일 내용 읽기 (변경 시 자동 리로드).

```alloy
local.file "secret" {
  filename      = "/etc/alloy/secret"
  is_secret     = true
  detector      = "fsnotify"   // fsnotify 또는 poll
  poll_frequency = "1m"
}

// 사용
prometheus.remote_write "mimir" {
  endpoint {
    basic_auth {
      password = local.file.secret.content
    }
  }
}
```

#### local.file_match

파일 글로브 패턴으로 타겟을 생성함.

```alloy
local.file_match "logs" {
  path_targets = [
    {__path__ = "/var/log/*.log"},
  ]
  sync_period = "10s"
}
```

#### remote.http

HTTP 엔드포인트에서 데이터 가져오기.

```alloy
remote.http "config" {
  url = "https://config.example.com/alloy.json"
  
  poll_frequency = "1m"
  poll_timeout   = "10s"
  
  headers = {
    "Authorization" = "Bearer " + sys.env("TOKEN"),
  }
}
```

#### remote.s3

S3에서 파일 가져오기.

```alloy
remote.s3 "config" {
  path = "s3://my-bucket/alloy/config"
  
  poll_frequency = "5m"
  
  client {
    region = "us-east-1"
  }
}
```

#### remote.kubernetes.secret

Kubernetes Secret 읽기.

```alloy
remote.kubernetes.secret "credentials" {
  namespace = "alloy"
  name      = "mimir-credentials"
}

// 사용
prometheus.remote_write "mimir" {
  endpoint {
    basic_auth {
      username = remote.kubernetes.secret.credentials.data["username"]
      password = remote.kubernetes.secret.credentials.data["password"]
    }
  }
}
```

#### remote.vault

HashiCorp Vault에서 비밀 읽기.

```alloy
remote.vault "secret" {
  server = "https://vault.example.com"
  path   = "secret/data/mimir"
  
  reread_frequency = "1h"
  
  auth.token {
    token = sys.env("VAULT_TOKEN")
  }
}
```

---

### 기타 유틸리티

#### faro.receiver

프론트엔드 관측성 (Grafana Faro).

```alloy
faro.receiver "default" {
  server {
    listen_address = "0.0.0.0"
    listen_port    = 12347
    cors_allowed_origins = ["https://app.example.com"]
  }
  
  output {
    logs   = [loki.write.default.receiver]
    traces = [otelcol.exporter.otlp.tempo.input]
  }
}
```

#### beyla.ebpf

eBPF 기반 자동 계측.

```alloy
beyla.ebpf "default" {
  open_port = "8080"
  
  output {
    traces = [otelcol.exporter.otlp.tempo.input]
  }
}
```

#### mimir.rules.kubernetes

Kubernetes CR(PrometheusRule)에 정의된 룰을 Mimir에 자동으로 동기화함.

```alloy
mimir.rules.kubernetes "default" {
  address          = "http://mimir:9009"
  tenant_id        = "tenant-1"
  
  rule_namespace_selector {
    match_labels = {
      "monitoring" = "enabled",
    }
  }
}
```

---

## Alloy 표준 라이브러리

> 원본: https://grafana.com/docs/alloy/latest/reference/stdlib/

---

### 목차

1. [개요](#개요)
2. [`sys`](#sys)
3. [`file`](#file)
4. [`string`](#string)
5. [`array`](#array)
6. [`encoding`](#encoding)
7. [`convert`](#convert)
8. [`coalesce`](#coalesce)
9. [`concat`](#concat)
10. [`json_path`](#json_path)
11. [`format`](#format)
12. [`constants`](#constants)
13. [기타 함수](#기타-함수)

---

### 개요

Alloy 표준 라이브러리는 구성 파일의 표현식에서 사용할 수 있는 내장 함수와 상수를 제공함.

#### 사용 위치

```alloy
prometheus.remote_write "mimir" {
  endpoint {
    url = sys.env("MIMIR_URL")          // 표준 라이브러리 사용
    
    basic_auth {
      password = file.contents("/etc/secret")
    }
  }
  
  external_labels = {
    cluster  = sys.env("CLUSTER"),
    hostname = constants.hostname,
  }
}
```

---

### `sys`

시스템 관련 함수.

#### `sys.env(name)`

환경 변수 읽기.

```alloy
url      = sys.env("MIMIR_URL")
password = sys.env("API_KEY")
```

환경 변수가 존재하지 않으면 빈 문자열을 반환함.

기본값:
```alloy
url = coalesce(sys.env("MIMIR_URL"), "http://localhost:9009")
```

---

### `file`

파일 관련 함수.

#### `file.contents(path)`

파일 내용을 문자열로 읽기.

```alloy
ca_cert  = file.contents("/etc/ssl/ca.crt")
password = file.contents("/run/secrets/mimir-pass")
```

##### 자동 리로드

`file.contents()` 결과는 파일이 변경되면 자동으로 갱신됨 → 이 값에 의존하는 컴포넌트가 재평가됨. 시크릿 로테이션에 유용.

##### 사용 예 (TLS)

```alloy
prometheus.remote_write "mimir" {
  endpoint {
    url = "https://mimir.example.com/api/v1/push"
    
    tls_config {
      ca_pem   = file.contents("/etc/certs/ca.crt")
      cert_pem = file.contents("/etc/certs/client.crt")
      key_pem  = file.contents("/etc/certs/client.key")
    }
  }
}
```

---

### `string`

문자열 처리.

#### `string.format(fmt, args...)`

C 스타일 포맷팅.

```alloy
url = string.format("http://%s:%d/loki/api/v1/push", host, port)
```

#### `string.join(list, separator)`

리스트를 문자열로 결합.

```alloy
labels = string.join(["env", "region", "cluster"], ",")
// → "env,region,cluster"
```

#### `string.split(s, separator)`

문자열을 리스트로 분할.

```alloy
parts = string.split("a,b,c", ",")
// → ["a", "b", "c"]
```

#### `string.to_lower(s)` / `string.to_upper(s)`

대소문자 변환.

```alloy
env = string.to_lower(sys.env("ENVIRONMENT"))   // "PROD" → "prod"
```

#### `string.trim(s, cutset)`

양쪽 끝 문자 제거.

```alloy
clean = string.trim("  hello  ", " ")   // → "hello"
```

#### `string.trim_prefix(s, prefix)` / `string.trim_suffix(s, suffix)`

접두/접미사 제거.

```alloy
host = string.trim_prefix("http://example.com", "http://")
```

#### `string.replace(s, old, new)`

치환.

```alloy
fixed = string.replace("hello world", "world", "alloy")
```

---

### `array`

배열 관련.

#### `array.concat(lists...)`

여러 리스트 결합.

```alloy
all_targets = array.concat(
  discovery.kubernetes.pods.targets,
  discovery.docker.containers.targets,
  static_targets,
)
```

#### `array.combine_maps(maps1, maps2, keys)`

두 `list(map(string))`을 지정한 키 기준으로 결합함. 실험적(Experimental) 함수.

```alloy
// map 리스트 두 개를 "instance" 키를 기준으로 결합
combined = array.combine_maps(list1, list2, ["instance"])
```

---

### `encoding`

인코딩/디코딩.

#### `encoding.from_json(s)`

JSON 문자열을 객체로.

```alloy
config = encoding.from_json(file.contents("/etc/config.json"))
url    = config.endpoint
```

#### `encoding.from_yaml(s)`

YAML 문자열을 객체로.

```alloy
config = encoding.from_yaml(file.contents("/etc/config.yaml"))
```

#### `encoding.from_base64(s)`

Base64 디코드.

```alloy
secret = encoding.from_base64(sys.env("ENCODED_SECRET"))
```

#### `encoding.to_json(v)`

객체를 JSON 문자열로.

```alloy
json = encoding.to_json({a = 1, b = "hello"})
// → "{\"a\":1,\"b\":\"hello\"}"
```

---

### `convert`

타입 변환.

#### `convert.nonsensitive(secret)`

`secret` 타입을 일반 문자열로 변환함. UI나 로그에 노출될 수 있음 → 신중하게 사용 필요.

```alloy
// 일반적으로 권장하지 않음
visible_pass = convert.nonsensitive(sys.env("PASSWORD"))
```

활용:
- 디버깅용
- 일부 컴포넌트가 secret 타입을 받지 않을 때

---

### `coalesce`

여러 값 중 비어 있지 않은 첫 번째 값을 반환함.

```alloy
url = coalesce(
  sys.env("MIMIR_URL"),
  sys.env("DEFAULT_URL"),
  "http://localhost:9009",
)
```

null, 빈 문자열, 빈 리스트, 빈 맵을 빈 값으로 간주함.

---

### `concat`

리스트를 결합함(`array.concat`의 전역 alias).

```alloy
all = concat(list1, list2, list3)
```

---

### `json_path`

JSONPath로 값 추출.

```alloy
data = encoding.from_json(file.contents("/etc/config.json"))

// 특정 경로 추출
endpoint = json_path(data, "$.servers[0].endpoint")
all_urls = json_path(data, "$.servers[*].url")
```

JSONPath 문법:
- `$`: 루트
- `.field`: 필드
- `[*]`: 모든 요소
- `[0]`: 인덱스
- `..field`: 재귀 검색

---

### `format`

문자열 포맷팅 함수로, `string.format`의 전역 alias임.

```alloy
msg = format("server %s on port %d", host, port)
```

---

### `constants`

내장 상수.

- `constants.hostname`: 호스트 이름
- `constants.os`: OS 이름(`linux`, `darwin`, `windows`)
- `constants.arch`: 아키텍처(`amd64`, `arm64`)

#### 사용

```alloy
external_labels = {
  hostname = constants.hostname,
  os       = constants.os,
  arch     = constants.arch,
}
```

---

### 기타 함수

#### `to_lower(s)` / `to_upper(s)`

`string.to_lower` / `string.to_upper`의 전역 alias.

#### 산술 연산자

```alloy
total  = a + b
diff   = a - b
prod   = a * b
quot   = a / b
mod    = a % b
power  = a ^ b
```

#### 비교 연산자

```alloy
condition = (count > 100) && (status == "ok")
```

#### 논리 연산자

```alloy
all = a && b
any = a || b
not = !a
```

#### 조건부 (Ternary)

```alloy
url = sys.env("ENV") == "prod" ? "https://prod.api" : "https://dev.api"
```

#### 인덱싱

```alloy
list  = ["a", "b", "c"]
first = list[0]   // "a"

map = {key = "value"}
val = map.key     // "value"
val = map["key"]  // 동등
```

---

### 실전 예시

#### 1. 환경별 설정

```alloy
env = sys.env("ENVIRONMENT")

prometheus.remote_write "mimir" {
  endpoint {
    url = env == "prod" 
      ? "https://prod-mimir/api/v1/push" 
      : "https://staging-mimir/api/v1/push"
    
    headers = {
      "X-Scope-OrgID" = string.format("tenant-%s", env),
    }
  }
}
```

#### 2. JSON 구성에서 동적 타겟

```alloy
targets_data = encoding.from_json(file.contents("/etc/targets.json"))

prometheus.scrape "dynamic" {
  targets = targets_data.targets
  forward_to = [prometheus.remote_write.mimir.receiver]
}
```

`/etc/targets.json`:
```json
{
  "targets": [
    {"__address__": "host1:9090", "job": "node"},
    {"__address__": "host2:9090", "job": "node"}
  ]
}
```

#### 3. Secret 파일 사용

```alloy
prometheus.remote_write "mimir" {
  endpoint {
    url = "https://mimir.example.com/api/v1/push"
    
    basic_auth {
      username = "alloy"
      password = file.contents("/run/secrets/mimir-pass")
    }
    
    tls_config {
      ca_pem   = file.contents("/etc/certs/ca.crt")
      cert_pem = file.contents("/etc/certs/client.crt")
      key_pem  = file.contents("/etc/certs/client.key")
    }
  }
}
```

시크릿 파일이 변경되면 자동으로 리로드됨.

#### 4. 라벨 동적 생성

```alloy
// array.combine_maps는 두 list(map(string))을 지정 키로 결합하는 함수다.
// 단순 맵 병합에는 alloy의 맵 리터럴 병합 표현식을 사용한다.
external_labels = {
  cluster  = sys.env("CLUSTER"),
  region   = sys.env("REGION"),
  hostname = constants.hostname,
}
```

#### 5. 조건부 컴포넌트

```alloy
// env가 prod면 PII 마스킹 활성화
declare "log_pipeline" {
  argument "is_prod" { default = false }
  
  loki.process "pipeline" {
    forward_to = [loki.write.default.receiver]
    
    // is_prod일 때만 마스킹 단계 추가는 현재 직접 지원되지 않으므로
    // 다른 방식 사용
  }
}
```

또는 `import.file`을 환경별로:

```alloy
env = sys.env("ENVIRONMENT")

import.file "pipeline" {
  filename = string.format("modules/%s_pipeline.alloy", env)
}
```

#### 6. URL 빌더

```alloy
host  = sys.env("BACKEND_HOST")
port  = sys.env("BACKEND_PORT")
path  = "/api/v1/push"
proto = sys.env("USE_TLS") == "true" ? "https" : "http"

backend_url = string.format("%s://%s:%s%s", proto, host, port, path)
```

#### 7. JSONPath로 복잡한 추출

```alloy
config = encoding.from_yaml(file.contents("/etc/services.yaml"))

api_endpoints = json_path(config, "$.services[?(@.type=='api')].endpoint")

prometheus.scrape "api_services" {
  targets    = [for ep in api_endpoints : {__address__ = ep}]   // 가상 예시
  forward_to = [prometheus.remote_write.mimir.receiver]
}
```
