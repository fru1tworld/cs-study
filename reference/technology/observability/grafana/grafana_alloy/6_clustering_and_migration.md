# Alloy 클러스터링·모듈과 마이그레이션

## Alloy 클러스터링과 모듈

> 원본: https://grafana.com/docs/alloy/latest/concepts/clustering/

---

### 목차

1. [클러스터링 개요](#클러스터링-개요)
2. [클러스터링 활성화](#클러스터링-활성화)
3. [피어 디스커버리](#피어-디스커버리)
4. [클러스터링 사용 컴포넌트](#클러스터링-사용-컴포넌트)
5. [실전 예시: K8s에서 Alloy 클러스터](#실전-예시-k8s에서-alloy-클러스터)
6. [모듈 시스템](#모듈-시스템)
7. [모듈 작성 가이드](#모듈-작성-가이드)
8. [공식 모듈 활용](#공식-모듈-활용)

---

### 클러스터링 개요

#### 목적

여러 Alloy 인스턴스를 클러스터로 묶어:

- **자동 워크로드 분산**: 스크래핑 타겟 자동 분배
- **고가용성**: 인스턴스 장애 시 다른 인스턴스가 처리
- **수평 확장**: 인스턴스 추가로 처리량 증가

#### 작동 원리

1. 모든 노드가 가십(gossip)으로 클러스터 구성
2. 일관된 해싱(consistent hashing) 링 생성
3. 각 작업(타겟 스크래핑 등)이 해시에 따라 특정 노드에 할당
4. 노드는 자기 몫만 처리

#### 적합한 컴포넌트

- 스크래핑 컴포넌트 (`prometheus.scrape`, `pyroscope.scrape` 등)
- 디스커버리 결과 분배가 의미 있는 워크로드

#### 부적합한 컴포넌트

- Receiver 컴포넌트 (모든 노드가 받아야 함)
- File 기반 (각 노드의 파일은 다름)

---

### 클러스터링 활성화

#### 명령줄 플래그

```bash
alloy run \
  --cluster.enabled=true \
  --cluster.join-addresses=alloy-0:12345,alloy-1:12345 \
  --cluster.advertise-address=$(hostname -i):12345 \
  --cluster.name=prod-cluster \
  config.alloy
```

#### 환경변수 (systemd)

`/etc/default/alloy`:

```bash
CUSTOM_ARGS="--cluster.enabled=true \
             --cluster.join-addresses=alloy-0:12345,alloy-1:12345"
```

#### 주요 플래그

| 플래그 | 설명 |
|--------|------|
| `--cluster.enabled` | 클러스터링 활성화 |
| `--cluster.name` | 클러스터 이름 (다른 클러스터와 격리) |
| `--cluster.join-addresses` | 가입할 노드 주소들 |
| `--cluster.advertise-address` | 다른 노드에 알릴 주소 |
| `--cluster.advertise-interfaces` | 자동으로 광고 주소 결정할 NIC |
| `--cluster.discover-peers` | 자동 피어 디스커버리 |
| `--cluster.rejoin-interval` | 재가입 시도 주기 |
| `--cluster.max-join-peers` | 한 번에 가입 시도할 피어 수 |
| `--cluster.tls-*` | gossip TLS |

---

### 피어 디스커버리

#### 정적 목록

```bash
--cluster.join-addresses=alloy-0:12345,alloy-1:12345,alloy-2:12345
```

#### Kubernetes Headless Service

```bash
--cluster.discover-peers="provider=k8s,namespace=alloy,label_selector=app=alloy,port=12345"
```

또는 Helm values:

```yaml
alloy:
  clustering:
    enabled: true

controller:
  type: deployment
  replicas: 3
```

Helm 차트가 Headless Service 생성과 피어 발견 설정을 자동으로 처리한다.

#### DNS

```bash
--cluster.discover-peers="provider=dns,name=alloy.example.com,port=12345"
```

DNS A 레코드의 모든 IP를 피어로 등록한다.

#### 다중 디스커버리

```bash
--cluster.discover-peers="provider=k8s,namespace=alloy,label_selector=app=alloy,port=12345 provider=dns,name=external.example.com,port=12345"
```

---

### 클러스터링 사용 컴포넌트

#### prometheus.scrape

```alloy
prometheus.scrape "kubernetes" {
  targets    = discovery.kubernetes.pods.targets
  forward_to = [prometheus.remote_write.mimir.receiver]
  
  clustering {
    enabled = true
  }
}
```

각 타겟이 일관된 해싱으로 단일 노드에만 할당된다. N개 노드 기준으로 각 노드가 전체 타겟의 ~1/N을 처리한다.

#### pyroscope.scrape

```alloy
pyroscope.scrape "default" {
  targets    = discovery.kubernetes.pods.targets
  forward_to = [pyroscope.write.default.receiver]
  
  clustering {
    enabled = true
  }
}
```

#### loki.source.kubernetes

```alloy
loki.source.kubernetes "pods" {
  targets    = discovery.kubernetes.pods.targets
  forward_to = [loki.write.default.receiver]
  
  clustering {
    enabled = true
  }
}
```

#### loki.source.kubernetes_events

```alloy
loki.source.kubernetes_events "events" {
  forward_to = [loki.write.default.receiver]
  
  // 이벤트는 단일 노드만 수집해야 (중복 방지)
  clustering {
    enabled = true
  }
}
```

---

### 실전 예시: K8s에서 Alloy 클러스터

#### Helm Values

```yaml
alloy:
  configMap:
    create: true
    content: |
      logging {
        level = "info"
      }
      
      // Kubernetes 디스커버리
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
          source_labels = ["__meta_kubernetes_pod_annotation_prometheus_io_port"]
          target_label  = "__address__"
          regex         = "(.+)"
          replacement   = "$1"
        }
      }
      
      // 클러스터링된 스크래핑
      prometheus.scrape "kubernetes_pods" {
        targets    = discovery.relabel.pods.output
        forward_to = [prometheus.remote_write.mimir.receiver]
        
        clustering {
          enabled = true
        }
      }
      
      prometheus.remote_write "mimir" {
        endpoint {
          url = "http://mimir:9009/api/v1/push"
        }
      }
  
  clustering:
    enabled: true
  
  resources:
    requests:
      cpu: 200m
      memory: 256Mi
    limits:
      memory: 1Gi

controller:
  type: deployment
  replicas: 3

rbac:
  create: true

serviceAccount:
  create: true
```

#### 동작

- 3개의 Deployment Pod 실행
- Headless Service를 통해 상호 피어 발견
- Kubernetes API에서 전체 Pod 목록 수신
- 각 Pod 타겟이 일관된 해싱으로 3개 Alloy 인스턴스에 분배
- 각 Alloy 인스턴스는 전체 Pod의 ~1/3만 스크래핑

#### 모니터링

```promql
# 클러스터 노드 수
alloy_cluster_node_peers

# 노드별 스크래핑 타겟 수
sum by (instance) (prometheus_sd_discovered_targets)
```

---

### 모듈 시스템

#### 모듈이란?

재사용 가능한 Alloy 구성 단위로, 매개변수와 출력을 정의해 라이브러리처럼 활용한다.

#### 모듈 구조

```alloy
// 모듈은 다음 블록을 가질 수 있음:

argument "param_name" {
  optional = false      // 또는 true
  default  = "default"  // optional=true일 때
  comment  = "설명"
}

declare "name" {
  // 내부 컴포넌트 정의
}

export "exported_value" {
  value = ...
}
```

#### 모듈 사용 흐름

1. 모듈 파일 작성
2. `import.*`로 모듈 임포트
3. 인자(arguments) 전달하여 인스턴스 생성
4. 출력(exports) 사용

---

### 모듈 작성 가이드

#### 단순 예시: 로그 파이프라인 모듈

`modules/loki_pipeline.alloy`:

```alloy
argument "endpoint" {
  comment = "Loki push endpoint"
}

argument "tenant_id" {
  optional = true
  default  = "default"
}

argument "external_labels" {
  optional = true
  default  = {}
}

loki.write "default" {
  endpoint {
    url = argument.endpoint.value
    headers = {
      "X-Scope-OrgID" = argument.tenant_id.value,
    }
  }
  
  external_labels = argument.external_labels.value
}

loki.process "main" {
  forward_to = [loki.write.default.receiver]
  
  stage.json {
    expressions = {
      level = "level",
      msg   = "message",
    }
  }
  
  stage.labels {
    values = {
      level = "",
    }
  }
}

export "receiver" {
  value = loki.process.main.receiver
}
```

#### 메인 구성에서 사용

```alloy
import.file "loki_pipeline" {
  filename = "modules/loki_pipeline.alloy"
}

loki_pipeline "default" {
  endpoint  = "http://loki:3100/loki/api/v1/push"
  tenant_id = "tenant-1"
  
  external_labels = {
    cluster = sys.env("CLUSTER"),
    region  = sys.env("REGION"),
  }
}

loki.source.file "app" {
  targets    = [{__path__ = "/var/log/app.log"}]
  forward_to = [loki_pipeline.default.receiver]
}
```

#### `declare` 블록 사용

같은 파일 안에서 모듈을 정의할 수 있다:

```alloy
declare "log_to_loki" {
  argument "url" { }
  
  loki.write "internal" {
    endpoint {
      url = argument.url.value
    }
  }
  
  export "receiver" {
    value = loki.write.internal.receiver
  }
}

// 사용
log_to_loki "default" {
  url = "http://loki:3100/loki/api/v1/push"
}

loki.source.file "app" {
  targets    = [{...}]
  forward_to = [log_to_loki.default.receiver]
}
```

---

### 공식 모듈 활용

#### Modules 저장소

[grafana/alloy-modules](https://github.com/grafana/alloy-modules) 에서 다양한 공식 모듈 제공.

| 모듈 | 용도 |
|------|------|
| `kubernetes/logs` | K8s Pod 로그 수집 표준 파이프라인 |
| `kubernetes/metrics` | K8s 메트릭 수집 |
| `kubernetes/events` | K8s 이벤트 수집 |
| `node-exporter` | Node Exporter 통합 |
| `cadvisor` | cAdvisor 통합 |

#### Git 임포트로 사용

```alloy
import.git "modules" {
  repository     = "https://github.com/grafana/alloy-modules.git"
  revision       = "main"
  pull_frequency = "5m"
}

// 사용
modules.kubernetes.logs.pods "default" {
  forward_to = [loki.write.default.receiver]
}
```

#### 디렉토리 임포트

여러 모듈을 한 번에 임포트한다:

```alloy
import.git "k8s_modules" {
  repository     = "https://github.com/grafana/alloy-modules.git"
  revision       = "main"
  path           = "modules/kubernetes"
}
```

---

### 클러스터링 + 모듈 조합 예시

```alloy
// 외부 모듈
import.git "k8s" {
  repository = "https://github.com/grafana/alloy-modules.git"
  revision   = "main"
  path       = "modules/kubernetes"
}

// 로그 파이프라인 모듈 사용
k8s.logs.pods "default" {
  forward_to = [loki.write.default.receiver]
  
  // 클러스터링 활성화
  clustering = true
}

k8s.metrics.kubelet "default" {
  forward_to = [prometheus.remote_write.mimir.receiver]
  
  clustering = true
}

// 백엔드 전송
loki.write "default" {
  endpoint {
    url = "http://loki:3100/loki/api/v1/push"
  }
  external_labels = {
    cluster = sys.env("CLUSTER"),
  }
}

prometheus.remote_write "mimir" {
  endpoint {
    url = "http://mimir:9009/api/v1/push"
  }
  external_labels = {
    cluster = sys.env("CLUSTER"),
  }
}
```

이 조합으로:
- **모듈로 코드 재사용**
- **클러스터링으로 워크로드 분산**
- **간결한 메인 구성 파일**

---

## Alloy 마이그레이션 가이드

> 원본: https://grafana.com/docs/alloy/latest/set-up/migrate/

---

### 목차

1. [개요](#개요)
2. [Promtail에서 마이그레이션](#promtail에서-마이그레이션)
3. [Prometheus에서 마이그레이션](#prometheus에서-마이그레이션)
4. [Grafana Agent Static에서 마이그레이션](#grafana-agent-static에서-마이그레이션)
5. [Grafana Agent Flow에서 마이그레이션](#grafana-agent-flow에서-마이그레이션)
6. [Grafana Agent Operator에서 마이그레이션](#grafana-agent-operator에서-마이그레이션)
7. [OpenTelemetry Collector에서 마이그레이션](#opentelemetry-collector에서-마이그레이션)
8. [수동 마이그레이션 팁](#수동-마이그레이션-팁)

---

### 개요

Grafana는 다음 도구들에서 Alloy로의 마이그레이션을 권장합니다.

| 원본 | 마이그레이션 대상 | 자동 변환 도구 |
|------|----------------|--------------|
| Promtail | Loki 로그 수집 | `alloy convert --source-format=promtail` |
| Prometheus | 메트릭 수집 | `alloy convert --source-format=prometheus` |
| Grafana Agent Static | 통합 수집 (Deprecated) | `alloy convert --source-format=static` |
| Grafana Agent Flow | 통합 수집 (Deprecated) | 수동 마이그레이션 (구문 호환) |
| Grafana Agent Operator | K8s CR 기반 | 수동 마이그레이션 |
| OpenTelemetry Collector | OTel 표준 | `alloy convert --source-format=otelcol` |

#### 자동 변환 명령어 기본 형식

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

### Promtail에서 마이그레이션

#### Promtail 구성 예시

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

#### 자동 변환

```bash
alloy convert \
  --source-format=promtail \
  -o alloy-config.alloy \
  promtail.yaml
```

#### 변환 결과

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

#### 주요 차이점

- `scrape_configs.static_configs` → `local.file_match`
- `pipeline_stages` → `loki.process` 내부 `stage.*`
- `clients` → `loki.write`
- `positions` → 자동 (storage path)

---

### Prometheus에서 마이그레이션

#### Prometheus 구성 예시

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

#### 자동 변환

```bash
alloy convert \
  --source-format=prometheus \
  -o alloy-config.alloy \
  prometheus.yml
```

#### 변환 결과

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

#### 주요 차이점

- `scrape_configs` → 각각 `prometheus.scrape` 컴포넌트
- `relabel_configs` → `discovery.relabel`
- `remote_write` → `prometheus.remote_write`
- `global.external_labels` → `prometheus.remote_write.external_labels`

#### 주의사항

- Service Discovery(`kubernetes_sd_configs`, `consul_sd_configs` 등)는 `discovery.kubernetes`, `discovery.consul` 등으로 자동 변환됩니다.
- Recording Rules / Alerting Rules는 별도로 처리해야 합니다(Mimir Ruler에 등록).

---

### Grafana Agent Static에서 마이그레이션

#### Static Agent 구성 예시

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

#### 자동 변환

```bash
alloy convert \
  --source-format=static \
  -o alloy-config.alloy \
  agent-static.yaml
```

#### 결과

`metrics`, `logs`, `integrations` 섹션이 각각 Alloy 컴포넌트로 변환됩니다.

---

### Grafana Agent Flow에서 마이그레이션

#### Flow는 Alloy의 전신

Grafana Agent Flow의 River 구문은 Alloy 구문과 거의 동일합니다.

#### Flow 구성 예시 (River)

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

#### 마이그레이션 방법

Flow → Alloy는 자동 변환 도구(`alloy convert`)를 지원하지 않습니다. 공식 가이드는 수동 마이그레이션을 권장하며, 구체적인 절차는 다음과 같습니다.

1. 지원이 중단된 컴포넌트를 대체 컴포넌트로 교체
2. 기본 구성으로 Alloy 배포
3. 데이터 디렉터리 복사
4. 파이프라인 재구성

#### 차이점

대부분 동일하지만:

- 일부 컴포넌트 이름 변경 (예: `prometheus.exporter.unix` → 거의 동일)
- 일부 deprecated 컴포넌트 제거
- 클래식 모듈(`module.file`, `module.git`, `module.http`, `module.string`) → `import.*` 블록으로 대체

#### 수동 변경 사항

```diff
- prometheus.scrape "default" {
+ prometheus.scrape "default" {
    targets    = [...]
    forward_to = [...]
  }
```

코드 자체는 대부분 그대로 동작합니다.

---

### Grafana Agent Operator에서 마이그레이션

#### Agent Operator 패턴

Kubernetes Custom Resources를 사용:

- `GrafanaAgent`
- `MetricsInstance`
- `LogsInstance`
- `Integration`

#### 마이그레이션 방법

공식 권장 방식은 `grafana/alloy` Helm Chart를 사용한 직접 배포입니다. Alloy Operator는 별도로 제공되지 않습니다.

```bash
helm install alloy grafana/alloy \
  --namespace monitoring \
  --values values.yaml
```

`values.yaml` 또는 ConfigMap에 Alloy 구성을 작성합니다.

#### CR → Alloy 매핑

| Operator CR | Alloy 컴포넌트 |
|------------|---------------|
| `GrafanaAgent` (메트릭) | `prometheus.scrape` + `prometheus.remote_write` |
| `MetricsInstance` | `prometheus.scrape` 인스턴스 |
| `LogsInstance` | `loki.source.*` + `loki.write` |
| `PodMonitor` / `ServiceMonitor` | `discovery.kubernetes` + `discovery.relabel` |
| `Probe` (Blackbox) | `prometheus.exporter.blackbox` |

---

### OpenTelemetry Collector에서 마이그레이션

#### OTel Collector 구성 예시

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

#### 자동 변환

```bash
alloy convert \
  --source-format=otelcol \
  -o alloy-config.alloy \
  otel-collector.yaml
```

#### 변환 결과

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

#### 매핑 규칙

| OTel YAML | Alloy |
|-----------|-------|
| `receivers.<type>` | `otelcol.receiver.<type>` |
| `processors.<type>` | `otelcol.processor.<type>` |
| `exporters.<type>` | `otelcol.exporter.<type>` |
| `extensions.<type>` | `otelcol.extension.<type>` |
| `connectors.<type>` | `otelcol.connector.<type>` |
| `service.pipelines` | 컴포넌트 간 `output` / `input` 연결 |

#### 주의사항

- 일부 OTel 컴포넌트는 Alloy에서 다른 이름을 사용합니다.
- 변환 후 반드시 검증해야 합니다.
- 커스텀 프로세서는 수동으로 변환해야 합니다.

---

### 수동 마이그레이션 팁

#### 1. 점진적 마이그레이션

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

#### 2. 변환 보고서 활용

```bash
alloy convert \
  --source-format=prometheus \
  -o alloy-config.alloy \
  -r conversion-report.txt \
  prometheus.yml
```

`conversion-report.txt`에서 변환되지 않은 항목이나 주의사항을 확인합니다.

#### 3. 검증

```bash
# 구문 검증
alloy validate alloy-config.alloy

# 포맷 정리
alloy fmt -w alloy-config.alloy

# 드라이런
alloy run --server.http.listen-addr=:0 alloy-config.alloy
```

#### 4. 메트릭/로그 비교

마이그레이션 전후에 동일한 메트릭/로그가 수집되는지 확인합니다.

```promql
# 메트릭 누락 확인
count(up{job=~".*"}) - count(up{job=~".*", instance=~"alloy.*"})

# 라벨 일치 확인
group(metric_name) by (instance, job) == group(metric_name) by (instance, job)
```

#### 5. Helm Chart 사용

새로 시작하는 경우, 공식 Helm Chart의 `values.yaml` 예시를 참고하여 처음부터 Alloy 모범 사례를 적용할 수 있습니다.

#### 6. 모듈 활용

[grafana/alloy-modules](https://github.com/grafana/alloy-modules)의 표준 모듈을 활용하면 구성을 단순화할 수 있습니다.

```alloy
import.git "modules" {
  repository = "https://github.com/grafana/alloy-modules.git"
  revision   = "main"
}

modules.kubernetes.logs.pods "default" {
  forward_to = [loki.write.default.receiver]
}
```
