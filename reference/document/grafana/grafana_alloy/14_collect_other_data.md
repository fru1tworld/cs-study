# Prometheus / Loki / Pyroscope 데이터 수집

> 이 문서는 Grafana Alloy 공식 문서의 Collect 섹션 (OTel 외 신호) 을 한국어로 정리한 것입니다.
> 원본: https://grafana.com/docs/alloy/latest/collect/

---

## 목차

1. [개요](#개요)
2. [Prometheus 메트릭 수집](#prometheus-메트릭-수집)
3. [Loki 로그 수집](#loki-로그-수집)
4. [Pyroscope 프로파일 수집](#pyroscope-프로파일-수집)
5. [eBPF 자동 계측 (Beyla)](#ebpf-자동-계측-beyla)
6. [Grafana Faro (Frontend)](#grafana-faro-frontend)
7. [통합 파이프라인](#통합-파이프라인)

---

## 개요

Alloy는 OpenTelemetry 외에도 Grafana 에코시스템 전용 컴포넌트로 다양한 신호를 수집합니다.

| 신호 | 컴포넌트 네임스페이스 | 백엔드 |
|------|--------------------|------|
| Metrics | `prometheus.*` | Mimir, Prometheus, Cortex, Thanos |
| Logs | `loki.*` | Loki |
| Profiles | `pyroscope.*` | Pyroscope |
| Frontend | `faro.*` | Faro/Loki/Tempo |
| eBPF | `beyla.*` | Tempo (트레이스), Mimir (메트릭) |

---

## Prometheus 메트릭 수집

### Scrape 컴포넌트

#### Static Targets

```alloy
prometheus.scrape "static" {
  targets = [
    {__address__ = "host1:9100", job = "node"},
    {__address__ = "host2:9100", job = "node"},
  ]
  forward_to = [prometheus.remote_write.mimir.receiver]
  
  scrape_interval = "15s"
  scrape_timeout  = "10s"
  metrics_path    = "/metrics"
  scheme          = "http"
}
```

#### Kubernetes Discovery

```alloy
discovery.kubernetes "pods" {
  role = "pod"
}

discovery.relabel "pods" {
  targets = discovery.kubernetes.pods.targets
  
  rule {
    source_labels = ["__meta_kubernetes_pod_annotation_prometheus_io_scrape"]
    regex         = "true"
    action        = "keep"
  }
  
  rule {
    source_labels = ["__meta_kubernetes_pod_annotation_prometheus_io_path"]
    target_label  = "__metrics_path__"
    regex         = "(.+)"
  }
  
  rule {
    source_labels = ["__address__", "__meta_kubernetes_pod_annotation_prometheus_io_port"]
    action        = "replace"
    target_label  = "__address__"
    regex         = "([^:]+)(?::\\d+)?;(\\d+)"
    replacement   = "$1:$2"
  }
}

prometheus.scrape "kubernetes_pods" {
  targets    = discovery.relabel.pods.output
  forward_to = [prometheus.remote_write.mimir.receiver]
  
  clustering {
    enabled = true
  }
}
```

#### ServiceMonitor / PodMonitor 자동 변환

```alloy
prometheus.operator.servicemonitors "all" {
  forward_to = [prometheus.remote_write.mimir.receiver]
  
  selector {
    match_labels = {
      "monitoring" = "enabled",
    }
  }
}

prometheus.operator.podmonitors "all" {
  forward_to = [prometheus.remote_write.mimir.receiver]
}
```

### 내장 Exporter

#### Node Exporter (Linux)

```alloy
prometheus.exporter.unix "node" {
  set_collectors = [
    "cpu", "diskstats", "filesystem", "loadavg",
    "meminfo", "netdev", "netstat", "stat",
  ]
  
  filesystem {
    fs_types_exclude     = "^(autofs|binfmt_misc|cgroup.*|debugfs|devpts|devtmpfs|fusectl|hugetlbfs|mqueue|nsfs|overlay|proc|procfs|pstore|rpc_pipefs|securityfs|selinuxfs|squashfs|sysfs|tracefs)$"
    mount_points_exclude = "^/(dev|proc|sys|var/lib/docker/.+)($|/)"
  }
}

prometheus.scrape "node" {
  targets    = prometheus.exporter.unix.node.targets
  forward_to = [prometheus.remote_write.mimir.receiver]
}
```

#### MySQL

```alloy
prometheus.exporter.mysql "production" {
  data_source_name = "user:password@tcp(mysql:3306)/"
}

prometheus.scrape "mysql" {
  targets    = prometheus.exporter.mysql.production.targets
  forward_to = [prometheus.remote_write.mimir.receiver]
}
```

#### PostgreSQL

```alloy
prometheus.exporter.postgres "default" {
  data_source_names = ["postgresql://user:password@postgres:5432/?sslmode=disable"]
  
  enabled_collectors = ["database", "stat_database", "stat_user_tables"]
}
```

#### Redis

```alloy
prometheus.exporter.redis "cache" {
  redis_addr = "redis:6379"
  redis_password = sys.env("REDIS_PASSWORD")
}
```

#### Cloudwatch

```alloy
prometheus.exporter.cloudwatch "aws" {
  sts_region = "us-east-1"
  
  discovery {
    type = "AWS/EC2"
    
    metric {
      name       = "CPUUtilization"
      statistics = ["Average", "Maximum"]
      period     = "5m"
    }
  }
}
```

### Remote Write

```alloy
prometheus.remote_write "mimir" {
  endpoint {
    url = "http://mimir:9009/api/v1/push"
    
    headers = {
      "X-Scope-OrgID" = "tenant-1",
    }
    
    basic_auth {
      username = "alloy"
      password = sys.env("MIMIR_PASSWORD")
    }
    
    queue_config {
      capacity            = 10000
      max_samples_per_send = 2000
      max_shards          = 30
      batch_send_deadline = "5s"
    }
    
    write_relabel_config {
      source_labels = ["__name__"]
      regex         = "go_(memstats|gc).*"
      action        = "drop"
    }
  }
  
  external_labels = {
    cluster = sys.env("CLUSTER"),
  }
  
  wal {
    truncate_frequency = "2h"
    min_keepalive_time = "5m"
    max_keepalive_time = "8h"
  }
}
```

### Relabel

```alloy
prometheus.relabel "drop_internal" {
  forward_to = [prometheus.remote_write.mimir.receiver]
  
  rule {
    source_labels = ["__name__"]
    regex         = "(go_.*|process_.*)"
    action        = "drop"
  }
  
  rule {
    target_label = "cluster"
    replacement  = sys.env("CLUSTER")
  }
}
```

---

## Loki 로그 수집

### File Logs

```alloy
local.file_match "logs" {
  path_targets = [
    {__path__ = "/var/log/*.log", job = "system"},
    {__path__ = "/var/log/nginx/*.log", job = "nginx"},
  ]
  sync_period = "10s"
}

loki.source.file "logs" {
  targets    = local.file_match.logs.targets
  forward_to = [loki.process.parse.receiver]
  
  legacy_positions_file = "/tmp/positions.yaml"
}
```

### Kubernetes Pod Logs

```alloy
discovery.kubernetes "pods" {
  role = "pod"
}

discovery.relabel "pods" {
  targets = discovery.kubernetes.pods.targets
  
  rule {
    source_labels = ["__meta_kubernetes_namespace"]
    target_label  = "namespace"
  }
  
  rule {
    source_labels = ["__meta_kubernetes_pod_name"]
    target_label  = "pod"
  }
  
  rule {
    source_labels = ["__meta_kubernetes_pod_container_name"]
    target_label  = "container"
  }
}

loki.source.kubernetes "pods" {
  targets    = discovery.relabel.pods.output
  forward_to = [loki.write.default.receiver]
}
```

### Kubernetes Events

```alloy
loki.source.kubernetes_events "events" {
  forward_to = [loki.write.default.receiver]
  
  log_format = "logfmt"   // 또는 json
  
  job_name = "k8s-events"
  
  // 클러스터링 (이벤트 중복 방지)
  clustering {
    enabled = true
  }
}
```

### Journal (systemd)

```alloy
loki.source.journal "journal" {
  forward_to = [loki.write.default.receiver]
  
  max_age        = "12h"
  path           = "/var/log/journal"
  format_as_json = false
  
  matches = "_SYSTEMD_UNIT=ssh.service"
  
  labels = {
    job = "systemd-journal",
  }
  
  relabel_rules = loki.relabel.journal.rules
}

loki.relabel "journal" {
  forward_to = []
  
  rule {
    source_labels = ["__journal__systemd_unit"]
    target_label  = "unit"
  }
  
  rule {
    source_labels = ["__journal__hostname"]
    target_label  = "hostname"
  }
}
```

### Syslog

```alloy
loki.source.syslog "syslog" {
  listener {
    address  = "0.0.0.0:1514"
    protocol = "tcp"
    
    tls_config {
      cert_file = "/etc/certs/server.crt"
      key_file  = "/etc/certs/server.key"
    }
    
    use_incoming_timestamp = true
  }
  
  forward_to = [loki.write.default.receiver]
  
  labels = {
    job = "syslog",
  }
}
```

### Windows Event Log

```alloy
loki.source.windowsevent "application" {
  eventlog_name          = "Application"
  use_incoming_timestamp = true
  
  forward_to = [loki.write.default.receiver]
  
  labels = {
    job = "windows-events",
  }
}
```

### Process / Pipeline Stages

```alloy
loki.process "parse" {
  forward_to = [loki.write.default.receiver]
  
  // JSON 파싱
  stage.json {
    expressions = {
      level = "level",
      msg   = "message",
      ts    = "timestamp",
    }
  }
  
  // 라벨 추출
  stage.labels {
    values = {
      level = "",
    }
  }
  
  // 타임스탬프
  stage.timestamp {
    source = "ts"
    format = "RFC3339"
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
  
  // 출력 변경
  stage.output {
    source = "msg"
  }
  
  // Multi-line
  stage.multiline {
    firstline     = "^\\d{4}-\\d{2}-\\d{2}"
    max_wait_time = "3s"
  }
  
  // Drop
  stage.drop {
    expression = ".*health.*"
  }
  
  // Sample
  stage.sampling {
    rate = 0.1
  }
}
```

### Loki Write

```alloy
loki.write "default" {
  endpoint {
    url = "http://loki:3100/loki/api/v1/push"
    
    headers = {
      "X-Scope-OrgID" = "tenant-1",
    }
    
    basic_auth {
      username = "alloy"
      password = sys.env("LOKI_PASSWORD")
    }
    
    batch_size = "1MiB"
    batch_wait = "1s"
    
    max_backoff_period = "5m"
    max_retries        = 10
  }
  
  external_labels = {
    cluster  = sys.env("CLUSTER"),
    hostname = constants.hostname,
  }
}
```

---

## Pyroscope 프로파일 수집

### Pull 방식 (HTTP)

```alloy
pyroscope.scrape "default" {
  targets = [
    {__address__ = "app:8080", "service_name" = "myapp"},
  ]
  forward_to = [pyroscope.write.default.receiver]
  
  profiling_config {
    profile.cpu {
      enabled = true
    }
    profile.memory {
      enabled = true
    }
    profile.goroutine {
      enabled = true
    }
    profile.process_cpu {
      enabled = false
    }
  }
}
```

### eBPF (자동 계측)

```alloy
pyroscope.ebpf "default" {
  forward_to = [pyroscope.write.default.receiver]
  
  targets = discovery.kubernetes.pods.targets
  
  collect_user_profile   = true
  collect_kernel_profile = false
  
  sample_rate = 97   // Hz
}
```

### Java

```alloy
pyroscope.java "java_app" {
  targets = [
    {__address__ = "app:8080", "service_name" = "java-app"},
  ]
  forward_to = [pyroscope.write.default.receiver]
  
  profiling_config {
    interval   = "60s"
    cpu        = true
    sample_rate = 100
    alloc      = "1MB"
    lock       = "1ms"
  }
}
```

### Pyroscope Write

```alloy
pyroscope.write "default" {
  endpoint {
    url = "http://pyroscope:4040"
    
    basic_auth {
      username = "alloy"
      password = sys.env("PYROSCOPE_PASSWORD")
    }
  }
  
  external_labels = {
    cluster = sys.env("CLUSTER"),
  }
}
```

---

## eBPF 자동 계측 (Beyla)

Grafana Beyla로 코드 수정 없이 자동 트레이싱.

```alloy
beyla.ebpf "default" {
  open_port = "8080,9090"
  
  // 또는 실행 가능한 파일 모니터링
  executable_name = "myapp"
  
  // Kubernetes
  attributes {
    kubernetes {
      enable = true
    }
  }
  
  output {
    traces  = [otelcol.exporter.otlp.tempo.input]
    metrics = [otelcol.exporter.prometheus.mimir.input]
  }
}

otelcol.exporter.otlp "tempo" {
  client {
    endpoint = "tempo:4317"
    tls { insecure = true }
  }
}

otelcol.exporter.prometheus "mimir" {
  forward_to = [prometheus.remote_write.mimir.receiver]
}
```

---

## Grafana Faro (Frontend)

웹 프론트엔드에서 보내는 RUM 데이터.

```alloy
faro.receiver "default" {
  server {
    listen_address       = "0.0.0.0"
    listen_port          = 12347
    cors_allowed_origins = ["https://app.example.com"]
    api_key              = sys.env("FARO_API_KEY")
  }
  
  // 일부 필드 마스킹
  sourcemaps {
    download = true
    location {
      minified_path_prefix = "https://app.example.com/static/"
      path                 = "/etc/sourcemaps"
    }
  }
  
  output {
    logs   = [loki.write.faro.receiver]
    traces = [otelcol.exporter.otlp.tempo.input]
  }
}

loki.write "faro" {
  endpoint {
    url = "http://loki:3100/loki/api/v1/push"
    headers = {
      "X-Scope-OrgID" = "faro",
    }
  }
}
```

### Faro Web SDK

```javascript
import { initializeFaro } from '@grafana/faro-web-sdk';

const faro = initializeFaro({
  url: 'https://alloy.example.com:12347/collect',
  apiKey: 'your-api-key',
  app: {
    name: 'my-app',
    version: '1.0.0',
    environment: 'production',
  },
});
```

---

## 통합 파이프라인

여러 신호를 한 Alloy에서 모두 수집:

```alloy
// === Kubernetes Discovery ===
discovery.kubernetes "pods" {
  role = "pod"
}

discovery.relabel "pods" {
  targets = discovery.kubernetes.pods.targets
  
  rule {
    source_labels = ["__meta_kubernetes_namespace"]
    target_label  = "namespace"
  }
  rule {
    source_labels = ["__meta_kubernetes_pod_name"]
    target_label  = "pod"
  }
  rule {
    source_labels = ["__meta_kubernetes_pod_container_name"]
    target_label  = "container"
  }
}

// === Metrics ===
prometheus.scrape "kubernetes" {
  targets    = discovery.relabel.pods.output
  forward_to = [prometheus.remote_write.mimir.receiver]
  clustering { enabled = true }
}

prometheus.exporter.unix "node" { }

prometheus.scrape "node" {
  targets    = prometheus.exporter.unix.node.targets
  forward_to = [prometheus.remote_write.mimir.receiver]
}

prometheus.remote_write "mimir" {
  endpoint {
    url = sys.env("MIMIR_URL")
    headers = { "X-Scope-OrgID" = sys.env("TENANT") }
  }
}

// === Logs ===
loki.source.kubernetes "pods" {
  targets    = discovery.relabel.pods.output
  forward_to = [loki.write.default.receiver]
  clustering { enabled = true }
}

loki.source.kubernetes_events "events" {
  forward_to = [loki.write.default.receiver]
  clustering { enabled = true }
}

loki.write "default" {
  endpoint {
    url = sys.env("LOKI_URL")
    headers = { "X-Scope-OrgID" = sys.env("TENANT") }
  }
}

// === Traces (OTLP) ===
otelcol.receiver.otlp "default" {
  grpc { endpoint = "0.0.0.0:4317" }
  http { endpoint = "0.0.0.0:4318" }
  
  output {
    traces = [otelcol.processor.batch.default.input]
  }
}

otelcol.processor.batch "default" {
  output {
    traces = [otelcol.exporter.otlp.tempo.input]
  }
}

otelcol.exporter.otlp "tempo" {
  client {
    endpoint = sys.env("TEMPO_URL")
    tls { insecure = true }
    headers = { "X-Scope-OrgID" = sys.env("TENANT") }
  }
}

// === Profiles (eBPF) ===
pyroscope.ebpf "default" {
  forward_to = [pyroscope.write.default.receiver]
  targets    = discovery.relabel.pods.output
}

pyroscope.write "default" {
  endpoint {
    url = sys.env("PYROSCOPE_URL")
  }
}

// === 자체 모니터링 ===
logging {
  level    = "info"
  format   = "json"
  write_to = [loki.write.default.receiver]
}
```
