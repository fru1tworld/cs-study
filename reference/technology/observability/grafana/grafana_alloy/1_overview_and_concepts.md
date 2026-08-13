# Grafana Alloy 개요와 핵심 개념

## Grafana Alloy 개요

> 원본: https://grafana.com/docs/alloy/latest/

---

### 목차

1. [Alloy란 무엇인가](#alloy란-무엇인가)
2. [Alloy의 주요 기능](#alloy의-주요-기능)
3. [지원 신호 유형](#지원-신호-유형)
4. [Alloy 아키텍처](#alloy-아키텍처)
5. [핵심 개념](#핵심-개념)
6. [Alloy와 다른 수집기 비교](#alloy와-다른-수집기-비교)
7. [마이그레이션 경로](#마이그레이션-경로)

---

### Alloy란 무엇인가

Grafana Alloy는 OpenTelemetry Collector를 기반으로 하는 오픈소스 텔레메트리 수집기(open-source telemetry collector).

#### 핵심 특징

- OpenTelemetry Collector 배포판: OTel Collector를 기반으로 Grafana 생태계 통합
- 다중 신호 지원: 메트릭·로그·트레이스·프로파일을 단일 도구로 수집
- 벤더 중립(Vendor-neutral): Grafana Cloud뿐만 아니라 다양한 백엔드로 전송 가능
- 풍부한 통합: Prometheus, Loki, Tempo, Pyroscope, OpenTelemetry 등 지원
- 컴포넌트 기반: 모듈식 컴포넌트로 유연한 파이프라인 구성

#### Alloy가 등장한 배경

기존에는 Grafana Agent (Static, Flow), Promtail, OpenTelemetry Collector 등 여러 수집기를 별도로 운영해야 했음. Alloy는 이를 단일 도구로 통합해 운영 복잡성을 줄이는 것이 목표.

---

### Alloy의 주요 기능

#### 1. 다중 신호 (Multi-signal) 지원

각 신호 유형별로 별도 수집기를 실행할 필요 없이, 하나의 도구로 모든 텔레메트리 데이터를 수집.

- Metrics: Prometheus·OpenTelemetry·StatsD 등 지원
- Logs: Loki·Syslog·Journal·File·Kubernetes 등 지원
- Traces: OTLP·Jaeger·Zipkin 등 지원
- Profiles: Pyroscope (Continuous Profiling) 지원

#### 2. 유연한 백엔드 연결

- Grafana Cloud (네이티브 통합)
- 자체 관리형 Grafana Stack (Loki, Mimir, Tempo, Pyroscope)
- OpenTelemetry 호환 백엔드
- Prometheus 호환 백엔드

#### 3. 컴포넌트 기반 구성

Alloy 구성은 컴포넌트(component) 단위로 작성. 각 컴포넌트는 독립적으로 동작 → 컴포넌트 간 출력/입력으로 파이프라인 구성.

```alloy
// 예시: 시스템 메트릭을 Prometheus 형식으로 수집하여 Mimir에 전송
prometheus.exporter.unix "default" { }

prometheus.scrape "default" {
  targets    = prometheus.exporter.unix.default.targets
  forward_to = [prometheus.remote_write.mimir.receiver]
}

prometheus.remote_write "mimir" {
  endpoint {
    url = "http://mimir:9009/api/v1/push"
  }
}
```

#### 4. OpenTelemetry 네이티브

OpenTelemetry Collector의 모든 기능 사용 가능.

- OTLP Receiver (gRPC, HTTP)
- 다양한 Processor (Batch, Memory Limiter, Resource 등)
- 다양한 Exporter

#### 5. 클러스터링

여러 Alloy 인스턴스를 클러스터로 구성해 워크로드를 자동으로 분배 가능.

- 자동 타겟 분배
- 고가용성
- 무중단 스케일링

---

### 지원 신호 유형

#### 메트릭 (Metrics)

##### 수집 (Collection)
- `prometheus.scrape`: Prometheus 스타일 스크래핑
- `prometheus.exporter.*`: 각종 Exporter 내장 (Unix, MySQL, Redis, MongoDB 등)
- `otelcol.receiver.prometheus`: OpenTelemetry 형식
- `otelcol.receiver.otlp`: OTLP

##### 처리 (Processing)
- `prometheus.relabel`: 라벨 변환
- `otelcol.processor.batch`: 배치 처리
- `otelcol.processor.transform`: 데이터 변환

##### 전송 (Export)
- `prometheus.remote_write`: Prometheus Remote Write (Mimir, Cortex, Thanos)
- `otelcol.exporter.otlp`: OTLP Exporter

#### 로그 (Logs)

##### 수집
- `loki.source.file`: 파일 로그
- `loki.source.kubernetes`: 쿠버네티스 Pod 로그
- `loki.source.journal`: systemd journal
- `loki.source.syslog`: Syslog
- `otelcol.receiver.otlp`: OTLP 로그

##### 처리
- `loki.process`: Promtail 스타일 파이프라인
- `loki.relabel`: 라벨 변환
- `otelcol.processor.*`: OTel 프로세서

##### 전송
- `loki.write`: Loki로 전송
- `otelcol.exporter.otlp`: OTLP

#### 트레이스 (Traces)

##### 수집
- `otelcol.receiver.otlp`: OTLP (gRPC, HTTP)
- `otelcol.receiver.jaeger`: Jaeger 호환
- `otelcol.receiver.zipkin`: Zipkin 호환

##### 처리
- `otelcol.processor.batch`: 배치
- `otelcol.processor.tail_sampling`: 테일 샘플링
- `otelcol.processor.span_metrics`: 스팬 메트릭 생성

##### 전송
- `otelcol.exporter.otlp`: Tempo 등으로 전송

#### 프로파일 (Profiles)

##### 수집
- `pyroscope.scrape`: Pyroscope 스크래핑
- `pyroscope.ebpf`: eBPF 기반 프로파일링

##### 전송
- `pyroscope.write`: Pyroscope 백엔드로 전송

---

### Alloy 아키텍처

#### 컴포넌트 그래프

Alloy는 컴포넌트들이 서로 연결된 방향성 그래프(directed graph)로 동작.

```
   ┌──────────────────┐
   │  prometheus      │
   │  .exporter.unix  │
   └────────┬─────────┘
            │ targets
            v
   ┌──────────────────┐
   │  prometheus      │
   │  .scrape         │
   └────────┬─────────┘
            │ samples
            v
   ┌──────────────────┐
   │  prometheus      │
   │  .relabel        │
   └────────┬─────────┘
            │
            v
   ┌──────────────────┐
   │  prometheus      │
   │  .remote_write   │
   └──────────────────┘
```

각 컴포넌트 구성 요소:
- Arguments (입력): 설정 파라미터
- Exports (출력): 다른 컴포넌트에서 참조 가능한 값
- State (상태): 내부 상태 (메트릭, 헬스 등)

#### 단일 바이너리

Alloy는 단일 정적 Go 바이너리로 배포 → 모든 컴포넌트가 동일한 바이너리에 포함.

---

### 핵심 개념

#### 컴포넌트 (Component)

가장 기본적인 빌딩 블록. 각 컴포넌트는 특정 작업(스크래핑, 처리, 전송 등)을 수행.

명명 규칙: `<namespace>.<type> "<label>"`
- 예: `prometheus.scrape "kubernetes_pods"`

#### 표현식 (Expressions)

컴포넌트의 값을 참조하거나 변환할 때 사용.

```alloy
// 컴포넌트 출력 참조
forward_to = [prometheus.remote_write.mimir.receiver]

// 환경 변수 사용
url = sys.env("MIMIR_URL")

// 문자열 연산
endpoint = "http://" + sys.env("HOST") + ":9009"
```

#### 모듈 (Module)

재사용 가능한 구성 단위 → 다른 Alloy 구성에서 임포트하여 사용 가능.

#### 클러스터링 (Clustering)

여러 Alloy 인스턴스를 클러스터로 묶어 작업을 분산.

```alloy
prometheus.scrape "kubernetes" {
  targets    = discovery.kubernetes.pods.targets
  forward_to = [prometheus.remote_write.mimir.receiver]
  
  clustering {
    enabled = true
  }
}
```

이 설정으로 클러스터 내 Alloy 인스턴스들이 자동으로 타겟을 나눠 가져감.

#### 구성 파일 형식

Alloy는 자체 구성 언어(Alloy syntax, 이전엔 River라고 불림)를 사용 → HCL과 유사한 문법.

---

### Alloy와 다른 수집기 비교

- Alloy: 메트릭 지원 · 로그 지원 · 트레이스 지원 · 프로파일 지원 → 통합 솔루션
- Prometheus: 메트릭 지원 · 로그 미지원 · 트레이스 미지원 · 프로파일 미지원 → 메트릭 전용
- Promtail: 메트릭 미지원 · 로그 지원 · 트레이스 미지원 · 프로파일 미지원 → 로그 전용 (Loki)
- OpenTelemetry Collector: 메트릭 지원 · 로그 지원 · 트레이스 지원 · 프로파일 미지원 → OTel 전용
- Grafana Agent (Static): 메트릭 지원 · 로그 지원 · 트레이스 지원 · 프로파일 지원 → Deprecated
- Grafana Agent (Flow): 메트릭 지원 · 로그 지원 · 트레이스 지원 · 프로파일 지원 → Deprecated, Alloy로 통합
- Vector: 메트릭 지원 · 로그 지원 · 트레이스 미지원 · 프로파일 미지원 → Datadog/Datadog Logs
- Fluent Bit: 메트릭 미지원 · 로그 지원 · 트레이스 미지원 · 프로파일 미지원 → 로그 전용
- Telegraf: 메트릭 지원 · 로그 지원 · 트레이스 부분 지원(제한적) · 프로파일 미지원 → InfluxDB 생태계

---

### 마이그레이션 경로

Grafana는 다음 도구들에서 Alloy로 마이그레이션을 권장.

#### 1. Grafana Agent Operator → Alloy
- Operator를 통한 자동 배포에서 Alloy로 전환
- 마이그레이션 도구 제공

#### 2. Prometheus → Alloy
- `prometheus.scrape`로 동일한 스크래핑 가능
- `prometheus.remote_write`로 Mimir/Cortex/Prometheus 등에 전송

#### 3. Promtail → Alloy
- `loki.source.file`, `loki.process`로 동일 기능 구현
- Promtail 구성 자동 변환 도구 제공

#### 4. Grafana Agent Static → Alloy
- 자동 변환 도구 제공
- `alloy convert` 커맨드 사용

#### 5. Grafana Agent Flow → Alloy
- 거의 동일한 구성 사용 가능 (River → Alloy syntax)
- 직접적인 마이그레이션 가이드 제공

#### 6. OpenTelemetry Collector → Alloy
- 모든 OTel 컴포넌트가 `otelcol.*`로 사용 가능
- YAML → Alloy syntax 변환 도구

---

### 다음 단계

- [02_install.md](./02_install.md) - 설치 (Docker, Kubernetes, Linux, macOS, Windows)
- [03_configure.md](./03_configure.md) - 구성 (Configuration)
- [04_components.md](./04_components.md) - 컴포넌트 레퍼런스
- [05_collect_otel.md](./05_collect_otel.md) - OpenTelemetry 데이터 수집
- [06_clustering.md](./06_clustering.md) - 클러스터링
- [07_migrate.md](./07_migrate.md) - 마이그레이션 가이드

---

## Alloy 핵심 개념

> 원본: https://grafana.com/docs/alloy/latest/concepts/

---

### 목차

1. [컴포넌트 모델](#컴포넌트-모델)
2. [컴포넌트 컨트롤러](#컴포넌트-컨트롤러)
3. [Stability Levels](#stability-levels)
4. [클러스터링 (Clustering)](#클러스터링-clustering)
5. [모듈 (Modules)](#모듈-modules)
6. [구성 평가 모델](#구성-평가-모델)
7. [디스커버리와 라벨링](#디스커버리와-라벨링)
8. [신호 처리 모델](#신호-처리-모델)

---

### 컴포넌트 모델

#### 컴포넌트란?

Alloy의 가장 기본적인 빌딩 블록으로, 각 컴포넌트는 다음과 같은 특정 작업을 담당:

- 메트릭 스크래핑
- 로그 파일 읽기
- 데이터 변환
- 백엔드로 전송
- 그 외 다양한 작업

#### 명명 규칙

```
<namespace>.<type> "<user_label>"
```

예시:
- `prometheus.scrape "kubernetes"`
- `loki.write "default"`
- `discovery.kubernetes "pods"`
- `otelcol.receiver.otlp "main"`

#### 네임스페이스

- `prometheus.*`: Prometheus 호환 (메트릭) 용도
- `loki.*`: Loki 호환 (로그) 용도
- `pyroscope.*`: Pyroscope 호환 (프로파일) 용도
- `otelcol.*`: OpenTelemetry Collector 용도
- `discovery.*`: 서비스 디스커버리 용도
- `local.*`: 로컬 리소스 (파일 등) 용도
- `remote.*`: 원격 리소스 (HTTP, S3) 용도
- `mimir.*`: Mimir 전용
- `faro.*`: Frontend 관측성 용도
- `beyla.*`: eBPF 자동 계측 용도

#### 컴포넌트의 구성 요소

각 컴포넌트는 다음을 포함.

##### 1. Arguments (입력)

```alloy
prometheus.scrape "default" {
  targets         = [...]      // argument
  forward_to      = [...]      // argument
  scrape_interval = "15s"      // argument
}
```

##### 2. Exports (출력)

다른 컴포넌트에서 참조 가능한 값.

```alloy
prometheus.exporter.unix "default" { }

// Exports: prometheus.exporter.unix.default.targets

prometheus.scrape "node" {
  targets = prometheus.exporter.unix.default.targets   // 참조
}
```

##### 3. State (상태)

내부 상태와 디버그 정보 → UI에서 확인 가능.

##### 4. Health (건강 상태)

컴포넌트의 현재 상태:
- `healthy`
- `unhealthy`
- `exited`

---

### 컴포넌트 컨트롤러

#### 역할

Alloy의 핵심 엔진으로 다음을 수행:

- 구성 파일 파싱
- 컴포넌트 그래프 생성 (DAG)
- 컴포넌트 평가 순서 결정
- 변경 감지 및 재평가
- 컴포넌트 생명주기 관리

#### 평가 사이클

1. 구성 파일 로드 또는 변경 감지
2. 그래프 빌드 (의존성 분석)
3. 노드 단위 평가 (의존성 순서)
4. 변경된 출력은 다운스트림으로 전파
5. 영향받는 컴포넌트만 재평가

#### 핫 리로드

구성 파일 변경 시 다음 방법으로 리로드.

```bash
# SIGHUP
kill -HUP $(pgrep alloy)

# HTTP
curl -X POST http://localhost:12345/-/reload
```

평가 컨트롤러는 다음 순서로 처리:
1. 새 구성 파싱
2. 새 그래프 빌드
3. 변경된 컴포넌트만 재시작
4. 이전 컴포넌트 정리

---

### Stability Levels

각 컴포넌트는 안정성 레벨이 표시.

- `experimental`: 실험적, 변경 가능 → 호환성 보장 없음
- `public-preview`: 공개 미리보기, 안정화 중 → 일부 변경 가능
- `generally-available`: 정식 안정 (기본) → 메이저 버전 내 호환

#### 활성화

기본값은 `generally-available`만 활성화. 더 낮은 레벨을 활성화하려면 다음 명령 사용.

```bash
alloy run --stability.level=experimental config.alloy
```

#### 컴포넌트 문서 확인

각 컴포넌트 페이지에 안정성 레벨 표시.

---

### 클러스터링 (Clustering)

#### 개념

여러 Alloy 인스턴스를 클러스터로 묶어 다음을 제공.

- 워크로드 분산: 스크래핑 타겟을 인스턴스 간 자동 분배
- 고가용성: 인스턴스 다운 시 다른 인스턴스가 작업 인계
- 확장: 부하 증가 시 인스턴스 추가

#### 클러스터 활성화

```bash
alloy run \
  --cluster.enabled=true \
  --cluster.join-addresses=alloy-0:12345,alloy-1:12345 \
  --cluster.advertise-address=$(hostname -i):12345 \
  config.alloy
```

#### 컴포넌트에서 클러스터링 사용

```alloy
prometheus.scrape "kubernetes" {
  targets    = discovery.kubernetes.pods.targets
  forward_to = [prometheus.remote_write.mimir.receiver]
  
  clustering {
    enabled = true
  }
}
```

#### 작동 방식

1. 클러스터 멤버들이 가십(gossip)으로 서로 발견
2. 일관된 해싱(consistent hashing) 링 구성
3. 각 타겟이 어떤 노드에 할당될지 결정
4. 노드는 자기 몫의 타겟만 처리

#### 클러스터 디스커버리

##### Kubernetes Headless Service

```bash
--cluster.discover-peers="provider=k8s,namespace=alloy,label_selector=app=alloy,port=12345"
```

##### DNS

```bash
--cluster.discover-peers="provider=dns,name=alloy.example.com,port=12345"
```

#### 모니터링

UI의 Cluster 페이지에서 노드 목록 확인.

메트릭:
- `cluster_node_peers`: 알고 있는 피어 수
- `cluster_transport_*`: 전송 통계

---

### 모듈 (Modules)

#### 개념

재사용 가능한 구성 단위로, 라이브러리처럼 활용 가능.

#### 정의

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

#### 임포트

##### 파일 시스템

```alloy
import.file "log_pipeline" {
  filename = "modules/log_pipeline.alloy"
}
```

##### HTTP

```alloy
import.http "shared_modules" {
  url = "https://example.com/modules/log.alloy"
  poll_frequency = "1m"
  poll_timeout   = "10s"
}
```

##### Git

```alloy
import.git "modules" {
  repository = "https://github.com/myorg/alloy-modules.git"
  revision   = "main"
  path       = "log_pipeline.alloy"
  pull_frequency = "5m"
}
```

##### 인라인 문자열

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

#### 사용

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

### 구성 평가 모델

#### 정적 평가

Alloy는 평가 시점에 모든 표현식을 즉시 계산 → 지연 동적 평가는 미지원.

#### 의존성 그래프 (DAG)

```
discovery.kubernetes.pods → discovery.relabel → prometheus.scrape → prometheus.remote_write
```

순환 참조 발생 시 에러.

#### 변경 전파

A → B → C 그래프에서 A의 출력이 변하면:
1. A 평가
2. B 재평가
3. C 재평가

영향받지 않는 컴포넌트는 그대로 유지.

#### 평가 트리거

- 구성 파일 변경
- 컴포넌트 출력 변경 (`file.contents()`로 파일 읽는 경우)
- 외부 디스커버리 결과 변경 (Kubernetes API 등)

---

### 디스커버리와 라벨링

#### Discovery 컴포넌트의 출력

`discovery.*` 컴포넌트는 타겟 리스트를 출력.

각 타겟 구조:
```
{
  __address__ = "host:port",
  __meta_<source>_<key> = "value",
  ...
}
```

`__meta_*` 라벨은 출처별 메타데이터 → relabel을 통해 원하는 라벨로 변환.

#### 디스커버리 종류

- `discovery.kubernetes`: Kubernetes (pods, services, nodes 등) 대상
- `discovery.consul`: Consul 서비스 대상
- `discovery.docker`: Docker 컨테이너 대상
- `discovery.dns`: DNS 대상
- `discovery.ec2`: AWS EC2 대상
- `discovery.gce`: GCP Compute Engine 대상
- `discovery.azure`: Azure VM 대상
- `discovery.file`: 파일 기반 대상
- `discovery.http`: HTTP 엔드포인트 대상
- `discovery.kuma`: Kuma 메시 대상
- `discovery.lightsail`: AWS Lightsail 대상
- `discovery.linode`: Linode 대상
- `discovery.marathon`: Marathon 대상
- `discovery.nomad`: HashiCorp Nomad 대상
- `discovery.openstack`: OpenStack 대상
- `discovery.serverset`: Zookeeper Serverset 대상
- `discovery.triton`: Triton 대상
- `discovery.uyuni`: Uyuni/SUSE Manager 대상

#### Relabel

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

### 신호 처리 모델

#### Receiver/Forward_to 패턴 (Loki/Prometheus)

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

#### Output Block 패턴 (OpenTelemetry)

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

#### 차이점

- Loki/Prometheus: 컴포넌트가 `receiver`를 export → 다른 컴포넌트가 `forward_to`로 참조
- OpenTelemetry: 컴포넌트가 `input`을 export → 다른 컴포넌트가 `output` 블록 안에서 참조

#### 다중 수신자

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

#### 다중 송신자

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
