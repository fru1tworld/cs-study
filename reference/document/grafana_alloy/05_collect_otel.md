# OpenTelemetry 데이터 수집

> 이 문서는 Grafana Alloy 공식 문서의 "Collect OpenTelemetry data" 섹션을 한국어로 정리한 것입니다.
> 원본: https://grafana.com/docs/alloy/latest/collect/opentelemetry-data/

---

## 목차

1. [개요](#개요)
2. [OTLP Receiver 구성](#otlp-receiver-구성)
3. [프로세서(Processors)](#프로세서processors)
4. [익스포터(Exporters)](#익스포터exporters)
5. [신호별 파이프라인](#신호별-파이프라인)
6. [샘플링](#샘플링)
7. [배칭과 메모리 제한](#배칭과-메모리-제한)
8. [라벨/속성 변환](#라벨속성-변환)
9. [전체 파이프라인 예시](#전체-파이프라인-예시)

---

## 개요

Alloy는 OpenTelemetry Collector의 컴포넌트를 모두 사용할 수 있습니다. 모든 OTel 컴포넌트는 `otelcol.*` 네임스페이스에 있습니다.

### 컴포넌트 카테고리

| 종류 | 네임스페이스 | 예 |
|------|------------|------|
| Receivers | `otelcol.receiver.*` | `otlp`, `prometheus`, `jaeger`, `zipkin` |
| Processors | `otelcol.processor.*` | `batch`, `memory_limiter`, `transform`, `tail_sampling` |
| Exporters | `otelcol.exporter.*` | `otlp`, `otlphttp`, `prometheus`, `loki` |
| Connectors | `otelcol.connector.*` | `spanmetrics`, `servicegraph` |
| Extensions | `otelcol.extension.*` | `basicauth`, `bearertokenauth` |

### 기본 데이터 흐름

```
[Receiver] → [Processor 1] → [Processor 2] → ... → [Exporter]
```

각 컴포넌트는 입력(input)과 출력(output)을 갖습니다.

---

## OTLP Receiver 구성

### gRPC와 HTTP 둘 다

```alloy
otelcol.receiver.otlp "default" {
  grpc {
    endpoint = "0.0.0.0:4317"
    
    max_recv_msg_size = "16MiB"
  }
  
  http {
    endpoint = "0.0.0.0:4318"
    cors {
      allowed_origins = ["*"]
    }
  }
  
  output {
    metrics = [otelcol.processor.batch.default.input]
    logs    = [otelcol.processor.batch.default.input]
    traces  = [otelcol.processor.batch.default.input]
  }
}
```

### TLS 설정

```alloy
otelcol.receiver.otlp "secure" {
  grpc {
    endpoint = "0.0.0.0:4317"
    tls {
      cert_file = "/etc/certs/server.crt"
      key_file  = "/etc/certs/server.key"
      ca_file   = "/etc/certs/ca.crt"
    }
  }
  
  output {
    traces = [otelcol.processor.batch.default.input]
  }
}
```

### 인증

```alloy
otelcol.auth.basic "default" {
  username = "user"
  password = sys.env("AUTH_PASSWORD")
}

otelcol.receiver.otlp "default" {
  grpc {
    endpoint = "0.0.0.0:4317"
    auth     = otelcol.auth.basic.default.handler
  }
  
  output {
    traces = [otelcol.processor.batch.default.input]
  }
}
```

---

## 프로세서(Processors)

### batch

여러 작은 신호를 묶어서 처리. **거의 항상 권장**.

```alloy
otelcol.processor.batch "default" {
  send_batch_size = 1000
  timeout         = "5s"
  send_batch_max_size = 2000
  
  output {
    metrics = [otelcol.exporter.prometheus.mimir.input]
    logs    = [otelcol.exporter.loki.default.input]
    traces  = [otelcol.exporter.otlp.tempo.input]
  }
}
```

### memory_limiter

메모리 폭증 방지. **모든 파이프라인 시작에 권장**.

```alloy
otelcol.processor.memory_limiter "default" {
  check_interval         = "1s"
  limit_percentage       = 80
  spike_limit_percentage = 25
  
  output {
    metrics = [otelcol.processor.batch.default.input]
    logs    = [otelcol.processor.batch.default.input]
    traces  = [otelcol.processor.batch.default.input]
  }
}
```

### resource

리소스 속성 조작.

```alloy
otelcol.processor.resource "default" {
  attributes {
    key    = "cluster"
    value  = "us-east-1"
    action = "upsert"
  }
  attributes {
    key    = "deployment.environment"
    value  = sys.env("ENV")
    action = "upsert"
  }
  
  output {
    traces = [otelcol.exporter.otlp.tempo.input]
  }
}
```

### attributes

스팬/메트릭/로그 속성 조작.

```alloy
otelcol.processor.attributes "default" {
  action {
    key            = "http.url"
    action         = "hash"   // PII 보호
  }
  action {
    key            = "credit_card"
    action         = "delete"
  }
  action {
    key            = "service.name"
    from_attribute = "service_name"
    action         = "insert"
  }
  
  output {
    traces = [otelcol.exporter.otlp.tempo.input]
  }
}
```

### transform

OTTL(OpenTelemetry Transformation Language)로 자유로운 변환.

```alloy
otelcol.processor.transform "default" {
  trace_statements {
    context = "span"
    statements = [
      "set(attributes[\"environment\"], \"production\")",
      "delete_key(attributes, \"sensitive_data\")",
    ]
  }
  
  output {
    traces = [otelcol.exporter.otlp.tempo.input]
  }
}
```

### filter

조건에 따라 데이터 제거.

```alloy
otelcol.processor.filter "drop_health" {
  traces {
    span {
      span_record = ["attributes[\"http.route\"] == \"/health\""]
    }
  }
  
  output {
    traces = [otelcol.exporter.otlp.tempo.input]
  }
}
```

### tail_sampling

트레이스 종료 후 샘플링.

```alloy
otelcol.processor.tail_sampling "default" {
  decision_wait               = "10s"
  num_traces                  = 50000
  expected_new_traces_per_sec = 10
  
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
  
  policy {
    name = "probabilistic"
    type = "probabilistic"
    probabilistic {
      sampling_percentage = 10
    }
  }
  
  output {
    traces = [otelcol.exporter.otlp.tempo.input]
  }
}
```

### probabilistic_sampler

Head 샘플링 (트레이스 시작 시 결정).

```alloy
otelcol.processor.probabilistic_sampler "default" {
  sampling_percentage = 10
  
  output {
    traces = [otelcol.processor.batch.default.input]
  }
}
```

---

## 익스포터(Exporters)

### otlp (gRPC)

```alloy
otelcol.exporter.otlp "tempo" {
  client {
    endpoint = "tempo:4317"
    tls {
      insecure = true
    }
    headers = {
      "X-Scope-OrgID" = "tenant-1",
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

### otlphttp

```alloy
otelcol.exporter.otlphttp "tempo" {
  client {
    endpoint = "https://tempo:4318"
    headers = {
      "X-Scope-OrgID" = "tenant-1",
    }
    auth = otelcol.auth.basic.default.handler
  }
}
```

### prometheus / prometheusremotewrite

OTel 메트릭 → Prometheus 형식.

```alloy
otelcol.exporter.prometheus "mimir" {
  forward_to = [prometheus.remote_write.mimir.receiver]
  
  add_metric_suffixes = true
  resource_to_telemetry_conversion = true
}

prometheus.remote_write "mimir" {
  endpoint {
    url = "http://mimir:9009/api/v1/push"
    headers = {
      "X-Scope-OrgID" = "tenant-1",
    }
  }
}
```

### loki

OTel 로그 → Loki 형식.

```alloy
otelcol.exporter.loki "default" {
  forward_to = [loki.write.default.receiver]
}

loki.write "default" {
  endpoint {
    url = "http://loki:3100/loki/api/v1/push"
  }
}
```

### debug

디버그용 stdout 출력.

```alloy
otelcol.exporter.debug "default" {
  verbosity = "detailed"   // basic, normal, detailed
}
```

---

## 신호별 파이프라인

### 트레이스 전용

```alloy
otelcol.receiver.otlp "default" {
  grpc { endpoint = "0.0.0.0:4317" }
  
  output {
    traces = [otelcol.processor.batch.traces.input]
  }
}

otelcol.processor.batch "traces" {
  output {
    traces = [otelcol.exporter.otlp.tempo.input]
  }
}

otelcol.exporter.otlp "tempo" {
  client {
    endpoint = "tempo:4317"
    tls { insecure = true }
  }
}
```

### 메트릭 전용

```alloy
otelcol.receiver.otlp "default" {
  grpc { endpoint = "0.0.0.0:4317" }
  
  output {
    metrics = [otelcol.processor.batch.metrics.input]
  }
}

otelcol.processor.batch "metrics" {
  output {
    metrics = [otelcol.exporter.prometheus.mimir.input]
  }
}

otelcol.exporter.prometheus "mimir" {
  forward_to = [prometheus.remote_write.mimir.receiver]
}

prometheus.remote_write "mimir" {
  endpoint {
    url = "http://mimir:9009/api/v1/push"
  }
}
```

### 로그 전용

```alloy
otelcol.receiver.otlp "default" {
  grpc { endpoint = "0.0.0.0:4317" }
  
  output {
    logs = [otelcol.processor.batch.logs.input]
  }
}

otelcol.processor.batch "logs" {
  output {
    logs = [otelcol.exporter.loki.default.input]
  }
}

otelcol.exporter.loki "default" {
  forward_to = [loki.write.default.receiver]
}

loki.write "default" {
  endpoint {
    url = "http://loki:3100/loki/api/v1/push"
  }
}
```

---

## 샘플링

### Head Sampling (시작 시)

```alloy
otelcol.processor.probabilistic_sampler "head" {
  sampling_percentage = 10
  hash_seed           = 22
  
  output {
    traces = [otelcol.exporter.otlp.tempo.input]
  }
}
```

### Tail Sampling (종료 후)

[프로세서 섹션](#tail_sampling) 참조.

---

## 배칭과 메모리 제한

### 권장 파이프라인 순서

```
[Receiver] → [memory_limiter] → [batch] → [기타 processors] → [Exporter]
```

### memory_limiter 설정

```alloy
otelcol.processor.memory_limiter "default" {
  check_interval         = "1s"
  limit_mib              = 4000        // 또는 limit_percentage
  spike_limit_mib        = 800
  
  output {
    metrics = [otelcol.processor.batch.default.input]
    logs    = [otelcol.processor.batch.default.input]
    traces  = [otelcol.processor.batch.default.input]
  }
}
```

### batch 권장 값

| 신호 | send_batch_size | timeout |
|------|----------------|---------|
| Traces | 8192 | 200ms |
| Metrics | 1024 | 5s |
| Logs | 1024 | 5s |

---

## 라벨/속성 변환

### Resource Attributes 추가

```alloy
otelcol.processor.resource "default" {
  attributes {
    key    = "cluster"
    value  = "prod-us-east-1"
    action = "upsert"
  }
  attributes {
    key    = "deployment.environment"
    value  = "production"
    action = "upsert"
  }
  attributes {
    key    = "host.name"
    from_attribute = "k8s.node.name"
    action = "insert"
  }
  
  output {
    traces = [otelcol.exporter.otlp.tempo.input]
  }
}
```

### Span Attributes 마스킹

```alloy
otelcol.processor.attributes "mask_pii" {
  action {
    key    = "user.email"
    action = "hash"
  }
  action {
    key    = "credit_card"
    action = "delete"
  }
  action {
    key       = "phone"
    pattern   = "(\\d{3})-(\\d{3,4})-(\\d{4})"
    action    = "hash"
  }
  
  output {
    traces = [otelcol.exporter.otlp.tempo.input]
  }
}
```

---

## 전체 파이프라인 예시

### 모든 신호 수집

```alloy
// === Receivers ===
otelcol.receiver.otlp "default" {
  grpc { endpoint = "0.0.0.0:4317" }
  http { endpoint = "0.0.0.0:4318" }
  
  output {
    metrics = [otelcol.processor.memory_limiter.default.input]
    logs    = [otelcol.processor.memory_limiter.default.input]
    traces  = [otelcol.processor.memory_limiter.default.input]
  }
}

// === Memory Limiter ===
otelcol.processor.memory_limiter "default" {
  check_interval         = "1s"
  limit_percentage       = 80
  spike_limit_percentage = 25
  
  output {
    metrics = [otelcol.processor.resource.default.input]
    logs    = [otelcol.processor.resource.default.input]
    traces  = [otelcol.processor.resource.default.input]
  }
}

// === Resource Enrichment ===
otelcol.processor.resource "default" {
  attributes {
    key    = "cluster"
    value  = sys.env("CLUSTER")
    action = "upsert"
  }
  attributes {
    key    = "deployment.environment"
    value  = sys.env("ENV")
    action = "upsert"
  }
  
  output {
    metrics = [otelcol.processor.batch.default.input]
    logs    = [otelcol.processor.batch.default.input]
    traces  = [otelcol.processor.batch.default.input]
  }
}

// === Batch ===
otelcol.processor.batch "default" {
  send_batch_size = 1024
  timeout         = "5s"
  
  output {
    metrics = [otelcol.exporter.prometheus.mimir.input]
    logs    = [otelcol.exporter.loki.default.input]
    traces  = [otelcol.exporter.otlp.tempo.input]
  }
}

// === Exporters ===

// Metrics → Mimir (via Prometheus Remote Write)
otelcol.exporter.prometheus "mimir" {
  forward_to = [prometheus.remote_write.mimir.receiver]
}

prometheus.remote_write "mimir" {
  endpoint {
    url = sys.env("MIMIR_URL")
    headers = {
      "X-Scope-OrgID" = sys.env("TENANT_ID"),
    }
  }
}

// Logs → Loki
otelcol.exporter.loki "default" {
  forward_to = [loki.write.default.receiver]
}

loki.write "default" {
  endpoint {
    url = sys.env("LOKI_URL")
    headers = {
      "X-Scope-OrgID" = sys.env("TENANT_ID"),
    }
  }
}

// Traces → Tempo
otelcol.exporter.otlp "tempo" {
  client {
    endpoint = sys.env("TEMPO_GRPC_URL")
    tls { insecure = true }
    headers = {
      "X-Scope-OrgID" = sys.env("TENANT_ID"),
    }
  }
}
```
