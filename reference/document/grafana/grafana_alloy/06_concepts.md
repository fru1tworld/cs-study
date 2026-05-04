# Alloy 핵심 개념

> 이 문서는 Grafana Alloy 공식 문서의 "Concepts" 섹션을 한국어로 정리한 것입니다.
> 원본: https://grafana.com/docs/alloy/latest/concepts/

---

## 목차

1. [컴포넌트 모델](#컴포넌트-모델)
2. [컴포넌트 컨트롤러](#컴포넌트-컨트롤러)
3. [Stability Levels](#stability-levels)
4. [클러스터링 (Clustering)](#클러스터링-clustering)
5. [모듈 (Modules)](#모듈-modules)
6. [구성 평가 모델](#구성-평가-모델)
7. [디스커버리와 라벨링](#디스커버리와-라벨링)
8. [신호 처리 모델](#신호-처리-모델)

---

## 컴포넌트 모델

### 컴포넌트란?

Alloy의 가장 기본적인 빌딩 블록. 각 컴포넌트는 특정 작업을 수행:

- 메트릭 스크래핑
- 로그 파일 읽기
- 데이터 변환
- 백엔드로 전송
- ...

### 명명 규칙

```
<namespace>.<type> "<user_label>"
```

예시:
- `prometheus.scrape "kubernetes"`
- `loki.write "default"`
- `discovery.kubernetes "pods"`
- `otelcol.receiver.otlp "main"`

### 네임스페이스

| 네임스페이스 | 용도 |
|------------|------|
| `prometheus.*` | Prometheus 호환 (메트릭) |
| `loki.*` | Loki 호환 (로그) |
| `pyroscope.*` | Pyroscope 호환 (프로파일) |
| `otelcol.*` | OpenTelemetry Collector |
| `discovery.*` | 서비스 디스커버리 |
| `local.*` | 로컬 리소스 (파일 등) |
| `remote.*` | 원격 리소스 (HTTP, S3) |
| `mimir.*` | Mimir 전용 |
| `faro.*` | Frontend 관측성 |
| `beyla.*` | eBPF 자동 계측 |

### 컴포넌트의 구성 요소

각 컴포넌트는 다음을 가집니다:

#### 1. Arguments (입력)

```alloy
prometheus.scrape "default" {
  targets         = [...]      // argument
  forward_to      = [...]      // argument
  scrape_interval = "15s"      // argument
}
```

#### 2. Exports (출력)

다른 컴포넌트에서 참조 가능한 값.

```alloy
prometheus.exporter.unix "default" { }

// Exports: prometheus.exporter.unix.default.targets

prometheus.scrape "node" {
  targets = prometheus.exporter.unix.default.targets   // 참조
}
```

#### 3. State (상태)

내부 상태와 디버그 정보. UI에서 확인 가능.

#### 4. Health (건강 상태)

컴포넌트의 현재 상태:
- `healthy`
- `unhealthy`
- `exited`

---

## 컴포넌트 컨트롤러

### 역할

Alloy의 핵심 엔진. 다음을 수행:

- 구성 파일 파싱
- 컴포넌트 그래프 생성 (DAG)
- 컴포넌트 평가 순서 결정
- 변경 감지 및 재평가
- 컴포넌트 생명주기 관리

### 평가 사이클

1. 구성 파일 로드 또는 변경 감지
2. 그래프 빌드 (의존성 분석)
3. 노드 단위 평가 (의존성 순서)
4. 변경된 출력은 다운스트림으로 전파
5. 영향받는 컴포넌트만 재평가

### 핫 리로드

구성 파일 변경 시 다음 방법으로 리로드:

```bash
# SIGHUP
kill -HUP $(pgrep alloy)

# HTTP
curl -X POST http://localhost:12345/-/reload
```

평가 컨트롤러가:
1. 새 구성 파싱
2. 새 그래프 빌드
3. 변경된 컴포넌트만 재시작
4. 이전 컴포넌트 정리

---

## Stability Levels

각 컴포넌트는 안정성 레벨이 표시되어 있습니다.

| 레벨 | 의미 | 호환성 |
|------|------|--------|
| `experimental` | 실험적, 변경 가능 | 보장 없음 |
| `public-preview` | 공개 미리보기, 안정화 중 | 일부 변경 가능 |
| `generally-available` | 정식 안정 (기본) | 메이저 버전 내 호환 |

### 활성화

기본은 `generally-available`만. 더 낮은 레벨 활성화:

```bash
alloy run --stability.level=experimental config.alloy
```

### 컴포넌트 문서 확인

각 컴포넌트 페이지에 안정성 레벨 표시.

---

## 클러스터링 (Clustering)

### 개념

여러 Alloy 인스턴스를 클러스터로 묶어:

- **워크로드 분산**: 스크래핑 타겟을 인스턴스 간 자동 분배
- **고가용성**: 인스턴스 다운 시 다른 인스턴스가 작업 인계
- **확장**: 부하 증가 시 인스턴스 추가

### 클러스터 활성화

```bash
alloy run \
  --cluster.enabled=true \
  --cluster.join-addresses=alloy-0:12345,alloy-1:12345 \
  --cluster.advertise-address=$(hostname -i):12345 \
  config.alloy
```

### 컴포넌트에서 클러스터링 사용

```alloy
prometheus.scrape "kubernetes" {
  targets    = discovery.kubernetes.pods.targets
  forward_to = [prometheus.remote_write.mimir.receiver]
  
  clustering {
    enabled = true
  }
}
```

### 작동 방식

1. 클러스터 멤버들이 가십(gossip)으로 서로 발견
2. 일관된 해싱(consistent hashing) 링 구성
3. 각 타겟이 어떤 노드에 할당될지 결정
4. 노드는 자기 몫의 타겟만 처리

### 클러스터 디스커버리

#### Kubernetes Headless Service

```bash
--cluster.discover-peers="provider=k8s,namespace=alloy,label_selector=app=alloy,port=12345"
```

#### DNS

```bash
--cluster.discover-peers="provider=dns,name=alloy.example.com,port=12345"
```

### 모니터링

UI의 **Cluster** 페이지에서 노드 목록 확인.

메트릭:
- `cluster_node_peers`: 알고 있는 피어 수
- `cluster_transport_*`: 전송 통계

---

## 모듈 (Modules)

### 개념

재사용 가능한 구성 단위. 라이브러리 기능 제공.

### 정의

```alloy
// modules/log_pipeline.alloy

argument "endpoint" {
  optional = false
  comment  = "Loki endpoint URL"
}

argument "tenant_id" {
  optional = true
  default  = "default"
}

loki.process "main" {
  // ... 로그 처리 단계
  forward_to = [loki.write.default.receiver]
}

loki.write "default" {
  endpoint {
    url = argument.endpoint.value
    headers = {
      "X-Scope-OrgID" = argument.tenant_id.value,
    }
  }
}

export "receiver" {
  value = loki.process.main.receiver
}
```

### 임포트

#### 파일 시스템

```alloy
import.file "log_pipeline" {
  filename = "modules/log_pipeline.alloy"
}
```

#### HTTP

```alloy
import.http "shared_modules" {
  url = "https://example.com/modules/log.alloy"
  poll_frequency = "1m"
  poll_timeout   = "10s"
}
```

#### Git

```alloy
import.git "modules" {
  repository = "https://github.com/myorg/alloy-modules.git"
  revision   = "main"
  path       = "log_pipeline.alloy"
  pull_frequency = "5m"
}
```

#### 인라인 문자열

```alloy
import.string "inline_module" {
  content = `
    argument "name" {}
    export "greeting" {
      value = "Hello, " + argument.name.value
    }
  `
}
```

### 사용

```alloy
log_pipeline.main "default" {
  endpoint  = "http://loki:3100/loki/api/v1/push"
  tenant_id = "tenant-1"
}

loki.source.file "app" {
  targets    = [{__path__ = "/var/log/app.log"}]
  forward_to = [log_pipeline.main.default.receiver]
}
```

---

## 구성 평가 모델

### 정적 평가

Alloy는 평가 시점에 모든 표현식을 즉시 계산. 동적 평가 없음.

### 의존성 그래프 (DAG)

```
discovery.kubernetes.pods → discovery.relabel → prometheus.scrape → prometheus.remote_write
```

순환 참조는 에러.

### 변경 전파

A → B → C 그래프에서 A의 출력이 변하면:
1. A 평가
2. B 재평가
3. C 재평가

영향받지 않는 컴포넌트는 그대로.

### 평가 트리거

- 구성 파일 변경
- 컴포넌트 출력 변경 (`file.contents()`로 파일 읽는 경우)
- 외부 디스커버리 결과 변경 (Kubernetes API 등)

---

## 디스커버리와 라벨링

### Discovery 컴포넌트의 출력

`discovery.*` 컴포넌트들은 **타겟 리스트**를 출력합니다.

각 타겟은:
```
{
  __address__ = "host:port",
  __meta_<source>_<key> = "value",
  ...
}
```

`__meta_*` 라벨은 출처별 메타데이터. relabel을 통해 원하는 라벨로 변환.

### 디스커버리 종류

| 컴포넌트 | 대상 |
|---------|------|
| `discovery.kubernetes` | Kubernetes (pods, services, nodes 등) |
| `discovery.consul` | Consul 서비스 |
| `discovery.docker` | Docker 컨테이너 |
| `discovery.dns` | DNS |
| `discovery.ec2` | AWS EC2 |
| `discovery.gce` | GCP Compute Engine |
| `discovery.azure` | Azure VM |
| `discovery.file` | 파일 기반 |
| `discovery.http` | HTTP 엔드포인트 |
| `discovery.kuma` | Kuma 메시 |
| `discovery.lightsail` | AWS Lightsail |
| `discovery.linode` | Linode |
| `discovery.marathon` | Marathon |
| `discovery.nomad` | HashiCorp Nomad |
| `discovery.openstack` | OpenStack |
| `discovery.serverset` | Zookeeper Serverset |
| `discovery.triton` | Triton |
| `discovery.uyuni` | Uyuni/SUSE Manager |

### Relabel

```alloy
discovery.relabel "filter" {
  targets = discovery.kubernetes.pods.targets
  
  rule {
    source_labels = ["__meta_kubernetes_namespace"]
    regex         = "kube-system"
    action        = "drop"
  }
  
  rule {
    source_labels = ["__meta_kubernetes_pod_label_app"]
    target_label  = "app"
  }
  
  rule {
    source_labels = ["__meta_kubernetes_pod_node_name"]
    target_label  = "node"
  }
}
```

---

## 신호 처리 모델

### Receiver/Forward_to 패턴 (Loki/Prometheus)

```alloy
loki.source.file "app" {
  targets    = [{...}]
  forward_to = [loki.process.parse.receiver]   // ← receiver
}

loki.process "parse" {
  // ...
  forward_to = [loki.write.default.receiver]
}

loki.write "default" {
  endpoint { ... }
}
```

### Output Block 패턴 (OpenTelemetry)

```alloy
otelcol.receiver.otlp "default" {
  grpc { endpoint = "0.0.0.0:4317" }
  
  output {
    traces = [otelcol.processor.batch.default.input]   // ← input
  }
}

otelcol.processor.batch "default" {
  output {
    traces = [otelcol.exporter.otlp.tempo.input]
  }
}

otelcol.exporter.otlp "tempo" {
  client { ... }
}
```

### 차이점

- **Loki/Prometheus**: 컴포넌트가 `receiver`를 export, 다른 컴포넌트가 `forward_to`로 참조
- **OpenTelemetry**: 컴포넌트가 `input`을 export, 다른 컴포넌트가 `output` 블록 안에서 참조

### 다중 수신자

```alloy
prometheus.scrape "default" {
  targets    = [...]
  forward_to = [
    prometheus.remote_write.mimir.receiver,
    prometheus.remote_write.backup.receiver,
  ]
}
```

여러 백엔드로 동시 전송 가능 (팬아웃).

### 다중 송신자

```alloy
loki.write "default" {
  endpoint { url = "..." }
}

loki.source.file "app1" {
  targets    = [...]
  forward_to = [loki.write.default.receiver]
}

loki.source.file "app2" {
  targets    = [...]
  forward_to = [loki.write.default.receiver]
}

loki.source.kubernetes "pods" {
  targets    = [...]
  forward_to = [loki.write.default.receiver]
}
```

여러 소스를 단일 백엔드로 통합 (팬인).
