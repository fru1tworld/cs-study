# Alloy 마이그레이션 가이드

> 이 문서는 Grafana Alloy 공식 문서의 "Migrate to Alloy" 섹션을 한국어로 정리한 것입니다.
> 원본: https://grafana.com/docs/alloy/latest/set-up/migrate/

---

## 목차

1. [개요](#개요)
2. [Promtail에서 마이그레이션](#promtail에서-마이그레이션)
3. [Prometheus에서 마이그레이션](#prometheus에서-마이그레이션)
4. [Grafana Agent Static에서 마이그레이션](#grafana-agent-static에서-마이그레이션)
5. [Grafana Agent Flow에서 마이그레이션](#grafana-agent-flow에서-마이그레이션)
6. [Grafana Agent Operator에서 마이그레이션](#grafana-agent-operator에서-마이그레이션)
7. [OpenTelemetry Collector에서 마이그레이션](#opentelemetry-collector에서-마이그레이션)
8. [수동 마이그레이션 팁](#수동-마이그레이션-팁)

---

## 개요

Grafana는 다음 도구들에서 Alloy로의 마이그레이션을 권장합니다.

| 원본 | 마이그레이션 대상 | 자동 변환 도구 |
|------|----------------|--------------|
| Promtail | Loki 로그 수집 | `alloy convert --source-format=promtail` |
| Prometheus | 메트릭 수집 | `alloy convert --source-format=prometheus` |
| Grafana Agent Static | 통합 수집 (Deprecated) | `alloy convert --source-format=static` |
| Grafana Agent Flow | 통합 수집 (Deprecated) | `alloy convert --source-format=flow` |
| Grafana Agent Operator | K8s CR 기반 | 수동 마이그레이션 |
| OpenTelemetry Collector | OTel 표준 | `alloy convert --source-format=otelcol` |

### 자동 변환 명령어 기본 형식

```bash
alloy convert \
  --source-format=<format> \
  -o alloy-config.alloy \
  source-config.yaml
```

옵션:
- `-o, --output`: 출력 파일
- `-r, --report`: 변환 보고서 파일
- `-b, --bypass-errors`: 에러 무시
- `--extra-args`: 추가 매개변수

---

## Promtail에서 마이그레이션

### Promtail 구성 예시

```yaml
# promtail.yaml
server:
  http_listen_port: 9080
  grpc_listen_port: 0

positions:
  filename: /tmp/positions.yaml

clients:
  - url: http://loki:3100/loki/api/v1/push

scrape_configs:
  - job_name: system
    static_configs:
      - targets:
          - localhost
        labels:
          job: varlogs
          __path__: /var/log/*.log
    
    pipeline_stages:
      - regex:
          expression: '^(?P<level>\S+) (?P<msg>.*)$'
      - labels:
          level:
```

### 자동 변환

```bash
alloy convert \
  --source-format=promtail \
  -o alloy-config.alloy \
  promtail.yaml
```

### 변환 결과

```alloy
local.file_match "system" {
  path_targets = [{
    __path__ = "/var/log/*.log",
    job      = "varlogs",
  }]
}

loki.source.file "system" {
  targets    = local.file_match.system.targets
  forward_to = [loki.process.system.receiver]
}

loki.process "system" {
  forward_to = [loki.write.default.receiver]
  
  stage.regex {
    expression = "^(?P<level>\\S+) (?P<msg>.*)$"
  }
  
  stage.labels {
    values = {
      level = null,
    }
  }
}

loki.write "default" {
  endpoint {
    url = "http://loki:3100/loki/api/v1/push"
  }
}
```

### 주요 차이점

- `scrape_configs.static_configs` → `local.file_match`
- `pipeline_stages` → `loki.process` 내부 `stage.*`
- `clients` → `loki.write`
- `positions` → 자동 (storage path)

---

## Prometheus에서 마이그레이션

### Prometheus 구성 예시

```yaml
# prometheus.yml
global:
  scrape_interval: 15s
  external_labels:
    cluster: prod

scrape_configs:
  - job_name: prometheus
    static_configs:
      - targets: ['localhost:9090']
  
  - job_name: node
    static_configs:
      - targets: ['node1:9100', 'node2:9100']
    
    relabel_configs:
      - source_labels: [__address__]
        regex: '(.+):.*'
        target_label: instance

remote_write:
  - url: http://mimir:9009/api/v1/push
    headers:
      X-Scope-OrgID: tenant-1
```

### 자동 변환

```bash
alloy convert \
  --source-format=prometheus \
  -o alloy-config.alloy \
  prometheus.yml
```

### 변환 결과

```alloy
discovery.relabel "node" {
  targets = [
    {__address__ = "node1:9100"},
    {__address__ = "node2:9100"},
  ]
  
  rule {
    source_labels = ["__address__"]
    regex         = "(.+):.*"
    target_label  = "instance"
  }
}

prometheus.scrape "prometheus" {
  targets = [{__address__ = "localhost:9090"}]
  forward_to = [prometheus.remote_write.default.receiver]
  job_name = "prometheus"
  scrape_interval = "15s"
}

prometheus.scrape "node" {
  targets = discovery.relabel.node.output
  forward_to = [prometheus.remote_write.default.receiver]
  job_name = "node"
  scrape_interval = "15s"
}

prometheus.remote_write "default" {
  endpoint {
    url = "http://mimir:9009/api/v1/push"
    headers = {
      "X-Scope-OrgID" = "tenant-1",
    }
  }
  
  external_labels = {
    cluster = "prod",
  }
}
```

### 주요 차이점

- `scrape_configs` → 각각 `prometheus.scrape` 컴포넌트
- `relabel_configs` → `discovery.relabel`
- `remote_write` → `prometheus.remote_write`
- `global.external_labels` → `prometheus.remote_write.external_labels`

### 주의사항

- Service Discovery (`kubernetes_sd_configs`, `consul_sd_configs` 등)는 `discovery.kubernetes`, `discovery.consul` 등으로 자동 변환
- Recording Rules / Alerting Rules는 별도 처리 필요 (Mimir Ruler에 등록)

---

## Grafana Agent Static에서 마이그레이션

### Static Agent 구성 예시

```yaml
server:
  http_listen_port: 12345
  log_level: info

metrics:
  global:
    scrape_interval: 15s
    remote_write:
      - url: http://mimir:9009/api/v1/push
  configs:
    - name: integrations
      scrape_configs:
        - job_name: node
          static_configs:
            - targets: ['localhost:9100']

logs:
  configs:
    - name: default
      clients:
        - url: http://loki:3100/loki/api/v1/push
      scrape_configs:
        - job_name: system
          static_configs:
            - targets:
                - localhost
              labels:
                job: varlogs
                __path__: /var/log/*.log

integrations:
  node_exporter:
    enabled: true
  prometheus_remote_write:
    - url: http://mimir:9009/api/v1/push
```

### 자동 변환

```bash
alloy convert \
  --source-format=static \
  -o alloy-config.alloy \
  agent-static.yaml
```

### 결과

`metrics`, `logs`, `integrations` 섹션이 모두 Alloy 컴포넌트로 변환됩니다.

---

## Grafana Agent Flow에서 마이그레이션

### Flow는 Alloy의 전신

Grafana Agent Flow의 River 구문은 Alloy 구문과 거의 동일합니다.

### Flow 구성 예시 (River)

```river
prometheus.scrape "default" {
  targets    = [{__address__ = "localhost:9090"}]
  forward_to = [prometheus.remote_write.mimir.receiver]
}

prometheus.remote_write "mimir" {
  endpoint {
    url = "http://mimir:9009/api/v1/push"
  }
}
```

### 자동 변환

```bash
alloy convert \
  --source-format=flow \
  -o alloy-config.alloy \
  flow-config.river
```

### 차이점

대부분 동일하지만:

- 일부 컴포넌트 이름 변경 (예: `prometheus.exporter.unix` → 거의 동일)
- 일부 deprecated 컴포넌트 제거
- `--config.format=flow` 플래그 → `alloy` 기본값

### 수동 변경 사항

```diff
- prometheus.scrape "default" {
+ prometheus.scrape "default" {
    targets    = [...]
    forward_to = [...]
  }
```

코드 자체는 거의 그대로 동작.

---

## Grafana Agent Operator에서 마이그레이션

### Agent Operator 패턴

Kubernetes Custom Resources를 사용:

- `GrafanaAgent`
- `MetricsInstance`
- `LogsInstance`
- `Integration`

### 마이그레이션 옵션

#### 옵션 1: Alloy Operator

```bash
helm install alloy-operator grafana/alloy-operator \
  --namespace alloy-system \
  --create-namespace
```

CR로 관리:

```yaml
apiVersion: collector.grafana.com/v1alpha1
kind: Alloy
metadata:
  name: prod
  namespace: monitoring
spec:
  controllerKind: deployment
  replicas: 3
  config: |
    // Alloy 구성
```

#### 옵션 2: Helm Chart

```bash
helm install alloy grafana/alloy \
  --namespace monitoring \
  --values values.yaml
```

ConfigMap 또는 values.yaml에 구성 작성.

### CR → Alloy 매핑

| Operator CR | Alloy 컴포넌트 |
|------------|---------------|
| `GrafanaAgent` (메트릭) | `prometheus.scrape` + `prometheus.remote_write` |
| `MetricsInstance` | `prometheus.scrape` 인스턴스 |
| `LogsInstance` | `loki.source.*` + `loki.write` |
| `PodMonitor` / `ServiceMonitor` | `discovery.kubernetes` + `discovery.relabel` |
| `Probe` (Blackbox) | `prometheus.exporter.blackbox` |

---

## OpenTelemetry Collector에서 마이그레이션

### OTel Collector 구성 예시

```yaml
receivers:
  otlp:
    protocols:
      grpc:
        endpoint: 0.0.0.0:4317

processors:
  batch:
    timeout: 5s
    send_batch_size: 1000

exporters:
  otlp:
    endpoint: tempo:4317
    tls:
      insecure: true

service:
  pipelines:
    traces:
      receivers: [otlp]
      processors: [batch]
      exporters: [otlp]
```

### 자동 변환

```bash
alloy convert \
  --source-format=otelcol \
  -o alloy-config.alloy \
  otel-collector.yaml
```

### 변환 결과

```alloy
otelcol.receiver.otlp "default" {
  grpc {
    endpoint = "0.0.0.0:4317"
  }
  
  output {
    traces = [otelcol.processor.batch.default.input]
  }
}

otelcol.processor.batch "default" {
  timeout         = "5s"
  send_batch_size = 1000
  
  output {
    traces = [otelcol.exporter.otlp.default.input]
  }
}

otelcol.exporter.otlp "default" {
  client {
    endpoint = "tempo:4317"
    tls {
      insecure = true
    }
  }
}
```

### 매핑 규칙

| OTel YAML | Alloy |
|-----------|-------|
| `receivers.<type>` | `otelcol.receiver.<type>` |
| `processors.<type>` | `otelcol.processor.<type>` |
| `exporters.<type>` | `otelcol.exporter.<type>` |
| `extensions.<type>` | `otelcol.extension.<type>` |
| `connectors.<type>` | `otelcol.connector.<type>` |
| `service.pipelines` | 컴포넌트 간 `output` / `input` 연결 |

### 주의사항

- 일부 OTel 컴포넌트는 Alloy에서 다른 이름 사용
- 변환 후 검증 필수
- Custom processor는 수동 변환

---

## 수동 마이그레이션 팁

### 1. 점진적 마이그레이션

```
[기존 Promtail/Agent] (운영 중)
        +
[새 Alloy] (병렬 실행, 동일 데이터 전송)
        |
        v (비교 검증)
[Alloy로 완전 전환]
        |
        v
[기존 도구 제거]
```

### 2. 변환 보고서 활용

```bash
alloy convert \
  --source-format=prometheus \
  -o alloy-config.alloy \
  -r conversion-report.txt \
  prometheus.yml
```

`conversion-report.txt`에서 변환 안 된 부분이나 주의사항 확인.

### 3. 검증

```bash
# 구문 검증
alloy validate alloy-config.alloy

# 포맷 정리
alloy fmt -w alloy-config.alloy

# 드라이런
alloy run --server.http.listen-addr=:0 alloy-config.alloy
```

### 4. 메트릭/로그 비교

마이그레이션 전후로 같은 메트릭/로그가 들어가는지 확인:

```promql
# 메트릭 누락 확인
count(up{job=~".*"}) - count(up{job=~".*", instance=~"alloy.*"})

# 라벨 일치 확인
group(metric_name) by (instance, job) == group(metric_name) by (instance, job)
```

### 5. Helm Chart 사용

새로 시작한다면 공식 Helm Chart의 `values.yaml` 예시를 참고하여 처음부터 Alloy 모범 사례로 시작.

### 6. 모듈 활용

[grafana/alloy-modules](https://github.com/grafana/alloy-modules) 의 표준 모듈 사용으로 구성을 단순화.

```alloy
import.git "modules" {
  repository = "https://github.com/grafana/alloy-modules.git"
  revision   = "main"
}

modules.kubernetes.logs.pods "default" {
  forward_to = [loki.write.default.receiver]
}
```
