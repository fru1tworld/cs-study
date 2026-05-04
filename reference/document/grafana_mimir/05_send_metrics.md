# Mimir로 메트릭 전송

> 이 문서는 Grafana Mimir 공식 문서의 "Send metric data" 섹션을 한국어로 정리한 것입니다.
> 원본: https://grafana.com/docs/mimir/latest/configure/configure-the-write-path/

---

## 목차

1. [개요](#개요)
2. [Prometheus Remote Write](#prometheus-remote-write)
3. [Grafana Alloy](#grafana-alloy)
4. [OpenTelemetry Collector](#opentelemetry-collector)
5. [Influx Line Protocol](#influx-line-protocol)
6. [Datadog Agent 호환](#datadog-agent-호환)
7. [Graphite](#graphite)
8. [Mimir Push API 직접 호출](#mimir-push-api-직접-호출)
9. [HA Pair 처리](#ha-pair-처리)

---

## 개요

Mimir는 다양한 프로토콜로 메트릭을 받습니다.

| 프로토콜 | 엔드포인트 | 인증 헤더 |
|---------|----------|----------|
| Prometheus Remote Write | `/api/v1/push` | `X-Scope-OrgID` |
| OpenTelemetry OTLP/HTTP | `/otlp/v1/metrics` | `X-Scope-OrgID` |
| Influx Line Protocol | `/api/v1/push/influx` | `X-Scope-OrgID` |
| Datadog Agent | `/api/v1/push/datadog` | `X-Scope-OrgID` |
| Graphite | `/api/v1/push/graphite` | `X-Scope-OrgID` |

### 멀티 테넌시

`multitenancy_enabled: true` 시 모든 요청에 `X-Scope-OrgID` 헤더 필수.

---

## Prometheus Remote Write

### Prometheus 설정

```yaml
# prometheus.yml
global:
  scrape_interval: 15s
  external_labels:
    cluster: us-east-1
    replica: 0  # HA 페어 시 0/1로 구분

scrape_configs:
  - job_name: prometheus
    static_configs:
      - targets: ['localhost:9090']

remote_write:
  - url: https://mimir.example.com/api/v1/push
    headers:
      X-Scope-OrgID: tenant-1
    
    basic_auth:
      username: user
      password: pass
    
    tls_config:
      ca_file: /etc/ssl/certs/ca.crt
    
    queue_config:
      capacity: 10000
      max_samples_per_send: 2000
      max_shards: 30
      min_shards: 1
      batch_send_deadline: 5s
    
    metadata_config:
      send: true
      send_interval: 1m
    
    write_relabel_configs:
      - source_labels: [__name__]
        regex: 'go_.*'
        action: drop
```

### 권장 큐 설정

| 환경 | capacity | max_shards | max_samples_per_send |
|------|---------|-----------|---------------------|
| 소규모 | 2500 | 10 | 500 |
| 중규모 | 10000 | 30 | 2000 |
| 대규모 | 100000 | 200 | 5000 |

---

## Grafana Alloy

### config.alloy

```alloy
// Node Exporter 메트릭 수집
prometheus.exporter.unix "node" { }

// Kubernetes Pod 디스커버리
discovery.kubernetes "pods" {
  role = "pod"
}

prometheus.scrape "node" {
  targets    = prometheus.exporter.unix.node.targets
  forward_to = [prometheus.relabel.add_labels.receiver]
  scrape_interval = "15s"
}

prometheus.scrape "kubernetes" {
  targets    = discovery.kubernetes.pods.targets
  forward_to = [prometheus.relabel.add_labels.receiver]
}

// 라벨 추가
prometheus.relabel "add_labels" {
  forward_to = [prometheus.remote_write.mimir.receiver]
  
  rule {
    target_label = "cluster"
    replacement  = "us-east-1"
  }
}

// Mimir로 전송
prometheus.remote_write "mimir" {
  endpoint {
    url = "https://mimir.example.com/api/v1/push"
    headers = {
      "X-Scope-OrgID" = "tenant-1",
    }
    
    basic_auth {
      username = "user"
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
      regex         = "go_.*"
      action        = "drop"
    }
  }
  
  external_labels = {
    cluster = "us-east-1",
    region  = "primary",
  }
}
```

### 클러스터링

여러 Alloy 인스턴스가 스크래핑 부하를 자동 분산:

```alloy
prometheus.scrape "kubernetes" {
  targets    = discovery.kubernetes.pods.targets
  forward_to = [prometheus.remote_write.mimir.receiver]
  
  clustering {
    enabled = true
  }
}
```

---

## OpenTelemetry Collector

### Mimir 활성화

```yaml
# mimir.yaml
distributor:
  otel_metric_suffixes_enabled: true
```

### OTel Collector 구성

```yaml
receivers:
  otlp:
    protocols:
      grpc:
        endpoint: 0.0.0.0:4317
      http:
        endpoint: 0.0.0.0:4318
  
  prometheus:
    config:
      scrape_configs:
        - job_name: app
          static_configs:
            - targets: ['app:8080']

processors:
  batch:
    send_batch_size: 1000
    timeout: 10s
  
  resource:
    attributes:
      - key: cluster
        value: us-east-1
        action: upsert

exporters:
  otlphttp/mimir:
    endpoint: https://mimir.example.com/otlp
    headers:
      X-Scope-OrgID: tenant-1
    auth:
      authenticator: basicauth/mimir
  
  # 또는 Prometheus Remote Write 사용
  prometheusremotewrite/mimir:
    endpoint: https://mimir.example.com/api/v1/push
    headers:
      X-Scope-OrgID: tenant-1
    external_labels:
      cluster: us-east-1
    resource_to_telemetry_conversion:
      enabled: true

extensions:
  basicauth/mimir:
    client_auth:
      username: user
      password: ${env:MIMIR_PASSWORD}

service:
  extensions: [basicauth/mimir]
  pipelines:
    metrics:
      receivers: [otlp, prometheus]
      processors: [batch, resource]
      exporters: [prometheusremotewrite/mimir]
```

### OTel 메트릭 → Prometheus 변환

OTel은 Mimir에 들어오면서 자동으로 Prometheus 형식으로 변환됩니다.

- 점(`.`)은 언더스코어(`_`)로 변환
- 단위 접미사 자동 추가 (선택적)
- Resource Attributes는 라벨로 변환

---

## Influx Line Protocol

### Mimir 활성화

```yaml
distributor:
  influx:
    enabled: true
    max_request_size_bytes: 10000000
```

### Telegraf 구성

```toml
[[outputs.http]]
  url = "https://mimir.example.com/api/v1/push/influx/write"
  method = "POST"
  data_format = "influx"
  headers = {"X-Scope-OrgID" = "tenant-1"}
```

### 직접 전송

```bash
curl -X POST \
  -H "X-Scope-OrgID: tenant-1" \
  --data-binary "weather,location=us-midwest temperature=82 1700000000000000000" \
  https://mimir.example.com/api/v1/push/influx/write
```

---

## Datadog Agent 호환

### Mimir 활성화

```yaml
distributor:
  datadog:
    enabled: true
```

### Datadog Agent 구성

```yaml
# datadog.yaml
api_key: dummy

dd_url: https://mimir.example.com/api/v1/push/datadog

# 또는 환경변수
# DD_API_KEY=dummy
# DD_DD_URL=https://mimir.example.com/api/v1/push/datadog
# DD_TAGS="X-Scope-OrgID:tenant-1"
```

---

## Graphite

### Mimir 활성화

```yaml
distributor:
  graphite:
    enabled: true
```

### graphite-web/carbon-relay-ng 설정

```ini
[mimir]
type = grpc
addr = mimir-distributor:9095
```

또는 직접 HTTP:

```bash
echo "test.metric 42 $(date +%s)" | nc mimir 2003
```

---

## Mimir Push API 직접 호출

### 엔드포인트

```
POST /api/v1/push
Content-Type: application/x-protobuf
Content-Encoding: snappy
X-Scope-OrgID: <tenant-id>
```

### Body 형식

Snappy로 압축된 protobuf (`prometheus.WriteRequest`).

### Go 예시

```go
import (
    "github.com/prometheus/prometheus/prompb"
    "github.com/golang/snappy"
)

req := &prompb.WriteRequest{
    Timeseries: []prompb.TimeSeries{
        {
            Labels: []prompb.Label{
                {Name: "__name__", Value: "test_metric"},
                {Name: "job", Value: "demo"},
            },
            Samples: []prompb.Sample{
                {Value: 42.0, Timestamp: time.Now().UnixMilli()},
            },
        },
    },
}

data, _ := proto.Marshal(req)
compressed := snappy.Encode(nil, data)

httpReq, _ := http.NewRequest("POST", "https://mimir/api/v1/push", bytes.NewReader(compressed))
httpReq.Header.Set("Content-Type", "application/x-protobuf")
httpReq.Header.Set("Content-Encoding", "snappy")
httpReq.Header.Set("X-Scope-OrgID", "tenant-1")
```

### 응답 코드

| 코드 | 의미 |
|------|------|
| 200 | 성공 |
| 400 | 잘못된 요청 (라벨 포맷 등) |
| 401 | 인증 실패 |
| 429 | Rate Limit 초과 |
| 500 | 서버 에러 |

---

## HA Pair 처리

### 문제

Prometheus를 HA 페어로 운영하면 동일 메트릭이 두 번 전송되어 중복 발생.

### 해결: HA Tracker

Mimir의 HA Tracker가 한 시점에 한 페어만 활성으로 인식.

### Prometheus 설정

각 페어는 동일 `cluster` 라벨을 가지지만 다른 `__replica__` 라벨을 가집니다.

```yaml
# prometheus-1.yml
global:
  external_labels:
    cluster: prod
    __replica__: replica-0

remote_write:
  - url: https://mimir/api/v1/push
    headers:
      X-Scope-OrgID: tenant-1
```

```yaml
# prometheus-2.yml
global:
  external_labels:
    cluster: prod
    __replica__: replica-1

remote_write:
  - url: https://mimir/api/v1/push
    headers:
      X-Scope-OrgID: tenant-1
```

### Mimir HA Tracker 활성화

```yaml
distributor:
  ha_tracker:
    enable_ha_tracker: true
    
    kvstore:
      store: consul
      consul:
        host: consul:8500
    
    ha_tracker_update_timeout: 15s
    ha_tracker_update_timeout_jitter_max: 5s
    ha_tracker_failover_timeout: 30s

limits:
  ha_cluster_label: cluster
  ha_replica_label: __replica__
```

### 동작

1. 두 페어가 모두 메트릭 푸시
2. HA Tracker가 KV 저장소에 활성 replica 기록
3. 활성 replica가 보낸 데이터만 수용
4. 활성 replica가 응답 안 하면 다른 replica로 페일오버
