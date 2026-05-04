# Alloy 클러스터링과 모듈

> 이 문서는 Grafana Alloy 공식 문서의 "Clustering" 및 "Modules" 섹션을 한국어로 정리한 것입니다.
> 원본: https://grafana.com/docs/alloy/latest/concepts/clustering/

---

## 목차

1. [클러스터링 개요](#클러스터링-개요)
2. [클러스터링 활성화](#클러스터링-활성화)
3. [피어 디스커버리](#피어-디스커버리)
4. [클러스터링 사용 컴포넌트](#클러스터링-사용-컴포넌트)
5. [실전 예시: K8s에서 Alloy 클러스터](#실전-예시-k8s에서-alloy-클러스터)
6. [모듈 시스템](#모듈-시스템)
7. [모듈 작성 가이드](#모듈-작성-가이드)
8. [공식 모듈 활용](#공식-모듈-활용)

---

## 클러스터링 개요

### 목적

여러 Alloy 인스턴스를 클러스터로 묶어:

- **자동 워크로드 분산**: 스크래핑 타겟 자동 분배
- **고가용성**: 인스턴스 장애 시 다른 인스턴스가 처리
- **수평 확장**: 인스턴스 추가로 처리량 증가

### 작동 원리

1. 모든 노드가 가십(gossip)으로 클러스터 구성
2. 일관된 해싱(consistent hashing) 링 생성
3. 각 작업(타겟 스크래핑 등)이 해시에 따라 특정 노드에 할당
4. 노드는 자기 몫만 처리

### 적합한 컴포넌트

- 스크래핑 컴포넌트 (`prometheus.scrape`, `pyroscope.scrape` 등)
- 디스커버리 결과 분배가 의미 있는 워크로드

### 부적합한 컴포넌트

- Receiver 컴포넌트 (모든 노드가 받아야 함)
- File 기반 (각 노드의 파일은 다름)

---

## 클러스터링 활성화

### 명령줄 플래그

```bash
alloy run \
  --cluster.enabled=true \
  --cluster.join-addresses=alloy-0:12345,alloy-1:12345 \
  --cluster.advertise-address=$(hostname -i):12345 \
  --cluster.name=prod-cluster \
  config.alloy
```

### 환경변수 (systemd)

`/etc/default/alloy`:

```bash
CUSTOM_ARGS="--cluster.enabled=true \
             --cluster.join-addresses=alloy-0:12345,alloy-1:12345"
```

### 주요 플래그

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

## 피어 디스커버리

### 정적 목록

```bash
--cluster.join-addresses=alloy-0:12345,alloy-1:12345,alloy-2:12345
```

### Kubernetes Headless Service

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

Helm 차트가 자동으로 Headless Service와 발견 설정 처리.

### DNS

```bash
--cluster.discover-peers="provider=dns,name=alloy.example.com,port=12345"
```

DNS A 레코드의 모든 IP를 피어로 등록.

### 다중 디스커버리

```bash
--cluster.discover-peers="provider=k8s,namespace=alloy,label_selector=app=alloy,port=12345 provider=dns,name=external.example.com,port=12345"
```

---

## 클러스터링 사용 컴포넌트

### prometheus.scrape

```alloy
prometheus.scrape "kubernetes" {
  targets    = discovery.kubernetes.pods.targets
  forward_to = [prometheus.remote_write.mimir.receiver]
  
  clustering {
    enabled = true
  }
}
```

각 타겟이 일관된 해싱으로 한 노드에만 할당. N개 노드면 각 노드가 ~1/N의 타겟 처리.

### pyroscope.scrape

```alloy
pyroscope.scrape "default" {
  targets    = discovery.kubernetes.pods.targets
  forward_to = [pyroscope.write.default.receiver]
  
  clustering {
    enabled = true
  }
}
```

### loki.source.kubernetes

```alloy
loki.source.kubernetes "pods" {
  targets    = discovery.kubernetes.pods.targets
  forward_to = [loki.write.default.receiver]
  
  clustering {
    enabled = true
  }
}
```

### loki.source.kubernetes_events

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

## 실전 예시: K8s에서 Alloy 클러스터

### Helm Values

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

### 동작

- 3개 Deployment Pod
- Headless Service로 서로 발견
- Kubernetes API에서 모든 Pod 목록 받음
- 각 Pod 타겟이 일관된 해싱으로 3개 Alloy에 분배
- 각 Alloy는 ~1/3의 Pod만 스크래핑

### 모니터링

```promql
# 클러스터 노드 수
alloy_cluster_node_peers

# 노드별 스크래핑 타겟 수
sum by (instance) (prometheus_sd_discovered_targets)
```

---

## 모듈 시스템

### 모듈이란?

재사용 가능한 Alloy 구성 단위. 매개변수와 출력을 정의하여 라이브러리로 사용.

### 모듈 구조

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

### 모듈 사용 흐름

1. 모듈 파일 작성
2. `import.*`로 모듈 임포트
3. 인자(arguments) 전달하여 인스턴스 생성
4. 출력(exports) 사용

---

## 모듈 작성 가이드

### 단순 예시: 로그 파이프라인 모듈

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

### 메인 구성에서 사용

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

### `declare` 블록 사용

같은 파일 내에서 모듈 정의:

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

## 공식 모듈 활용

### Modules 저장소

[grafana/alloy-modules](https://github.com/grafana/alloy-modules) 에서 다양한 공식 모듈 제공.

| 모듈 | 용도 |
|------|------|
| `kubernetes/logs` | K8s Pod 로그 수집 표준 파이프라인 |
| `kubernetes/metrics` | K8s 메트릭 수집 |
| `kubernetes/events` | K8s 이벤트 수집 |
| `node-exporter` | Node Exporter 통합 |
| `cadvisor` | cAdvisor 통합 |

### Git 임포트로 사용

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

### 디렉토리 임포트

여러 모듈을 한 번에 임포트:

```alloy
import.git "k8s_modules" {
  repository     = "https://github.com/grafana/alloy-modules.git"
  revision       = "main"
  path           = "modules/kubernetes"
}
```

---

## 클러스터링 + 모듈 조합 예시

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
