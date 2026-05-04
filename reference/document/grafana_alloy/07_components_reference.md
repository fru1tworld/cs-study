# Alloy 컴포넌트 레퍼런스

> 이 문서는 Grafana Alloy 공식 문서의 "Reference" 섹션 중 주요 컴포넌트를 한국어로 정리한 것입니다.
> 원본: https://grafana.com/docs/alloy/latest/reference/components/

---

## 목차

1. [컴포넌트 카테고리](#컴포넌트-카테고리)
2. [discovery.*](#discovery)
3. [prometheus.*](#prometheus)
4. [loki.*](#loki)
5. [otelcol.*](#otelcol)
6. [pyroscope.*](#pyroscope)
7. [local.* / remote.*](#local--remote)
8. [기타 유틸리티](#기타-유틸리티)

---

## 컴포넌트 카테고리

| 카테고리 | 개수 (대략) | 용도 |
|---------|-----------|------|
| `discovery.*` | 25+ | 서비스 디스커버리 |
| `prometheus.*` | 60+ | 메트릭 (Exporters, Scrape, Relabel, Remote Write) |
| `loki.*` | 25+ | 로그 (Source, Process, Write) |
| `otelcol.*` | 80+ | OpenTelemetry (Receiver, Processor, Exporter) |
| `pyroscope.*` | 10+ | 프로파일 |
| `local.*` | 5+ | 로컬 리소스 (파일 등) |
| `remote.*` | 5+ | 원격 리소스 (HTTP, S3, Vault, Kubernetes) |
| `mimir.*` | 5+ | Mimir 전용 |
| `faro.*` | 1+ | 프론트엔드 |
| `beyla.*` | 1+ | eBPF 자동 계측 |

---

## discovery.*

### discovery.kubernetes

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

### discovery.docker

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

### discovery.consul

```alloy
discovery.consul "default" {
  server     = "consul:8500"
  datacenter = "dc1"
  services   = ["api", "web"]
  tags       = ["production"]
}
```

### discovery.dns

```alloy
discovery.dns "service" {
  names = ["service.example.com"]
  type  = "A"   // A, AAAA, SRV
  port  = 80
  refresh_interval = "30s"
}
```

### discovery.relabel

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

### discovery.ec2

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

## prometheus.*

### prometheus.scrape

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

### prometheus.remote_write

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

### prometheus.relabel

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

### prometheus.exporter.*

내장 Exporter들:

| Exporter | 컴포넌트 |
|----------|---------|
| Node | `prometheus.exporter.unix` |
| Windows | `prometheus.exporter.windows` |
| Cloudwatch | `prometheus.exporter.cloudwatch` |
| MySQL | `prometheus.exporter.mysql` |
| PostgreSQL | `prometheus.exporter.postgres` |
| Redis | `prometheus.exporter.redis` |
| MongoDB | `prometheus.exporter.mongodb` |
| Kafka | `prometheus.exporter.kafka` |
| Memcached | `prometheus.exporter.memcached` |
| Process | `prometheus.exporter.process` |
| SNMP | `prometheus.exporter.snmp` |
| Blackbox | `prometheus.exporter.blackbox` |
| GitHub | `prometheus.exporter.github` |
| Statsd | `prometheus.exporter.statsd` |

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

## loki.*

### loki.source.file

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

### loki.source.kubernetes

```alloy
loki.source.kubernetes "pods" {
  targets    = discovery.kubernetes.pods.targets
  forward_to = [loki.write.default.receiver]
}
```

### loki.source.journal

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

### loki.source.syslog

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

### loki.process

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

### loki.relabel

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

### loki.write

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
    
    max_backoff_period = "5m"
    max_retries        = 10
  }
  
  external_labels = {
    cluster = "prod",
  }
}
```

---

## otelcol.*

### otelcol.receiver.otlp

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

### otelcol.processor.batch

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

### otelcol.processor.tail_sampling

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

### otelcol.connector.spanmetrics

스팬에서 메트릭 자동 생성.

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

### otelcol.exporter.otlp

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

## pyroscope.*

### pyroscope.scrape

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

### pyroscope.ebpf

```alloy
pyroscope.ebpf "default" {
  forward_to = [pyroscope.write.default.receiver]
  
  targets = discovery.kubernetes.pods.targets
  
  collect_user_profile   = true
  collect_kernel_profile = false
}
```

---

## local.* / remote.*

### local.file

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

### local.file_match

파일 글로브로 타겟 생성.

```alloy
local.file_match "logs" {
  path_targets = [
    {__path__ = "/var/log/*.log"},
  ]
  sync_period = "10s"
}
```

### remote.http

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

### remote.s3

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

### remote.kubernetes.secret

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

### remote.vault

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

## 기타 유틸리티

### faro.receiver

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

### beyla.ebpf

eBPF 기반 자동 계측.

```alloy
beyla.ebpf "default" {
  open_port = "8080"
  
  output {
    traces = [otelcol.exporter.otlp.tempo.input]
  }
}
```

### mimir.rules.kubernetes

Kubernetes CR(PrometheusRule)에서 룰을 자동으로 Mimir에 동기화.

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
