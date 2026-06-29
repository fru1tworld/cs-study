# OpenTelemetry Collector

---

## 목차

1. [Collector란?](#collector란)
2. [왜 Collector를 쓰는가](#왜-collector를-쓰는가)
3. [아키텍처](#아키텍처)
4. [Receivers](#receivers)
5. [Processors](#processors)
6. [Exporters](#exporters)
7. [Connectors](#connectors)
8. [Pipelines 설정](#pipelines-설정)
9. [배포 패턴](#배포-패턴)
10. [배포판 (Distribution)](#배포판-distribution)

---

## Collector란?

**OpenTelemetry Collector**는 텔레메트리 데이터를 **수신·가공·전송**하는 벤더 중립적 에이전트/프록시입니다.

특징:
- Go로 작성된 단일 바이너리 (`otelcol`)
- 설정은 YAML
- 모든 시그널(Trace/Metric/Log/Profile) 처리 가능
- 다양한 포맷 입출력 지원 (OTLP, Jaeger, Zipkin, Prometheus, Fluentd, ...)

별도 프로세스로 동작하며, 보통 사이드카·DaemonSet·중앙 게이트웨이 형태로 배포됩니다.

---

## 왜 Collector를 쓰는가

### 1. 애플리케이션과 백엔드의 분리

SDK가 직접 백엔드로 OTLP를 보내면:
- 백엔드 변경 시 모든 앱 재배포 필요
- 백엔드 인증 정보가 모든 앱에 분산
- 배압·재시도 로직이 SDK에 의존

Collector를 거치면 앱은 **localhost로만 보내고**, Collector가 백엔드 라우팅을 담당.

### 2. 데이터 가공

- 민감정보 필터링/마스킹
- attribute 정규화 (`http.status` → `http.status_code`)
- 호스트/K8s 메타데이터 자동 추가
- 샘플링 (특히 tail sampling)

### 3. 포맷 변환

기존 시스템과의 호환:
- Jaeger Thrift를 받아서 OTLP로 변환
- Prometheus `/metrics` 를 scrape해서 OTLP로 export
- syslog를 받아서 Loki로 보냄

### 4. 배압·버퍼링·재시도

네트워크 장애 시 SDK는 보통 작은 버퍼만 가짐. Collector는 큰 큐와 디스크 백업을 제공해 데이터 손실을 줄임.

### 5. Multi-backend 라우팅

같은 데이터를 두 백엔드로 동시에 보내거나 (예: 자체 호스팅 + SaaS), 시그널별로 다른 백엔드로 분기.

---

## 아키텍처

Collector는 **컴포넌트 기반** 구조입니다. 데이터는 다음 순서로 처리됩니다:

```
┌─────────────┐    ┌──────────────┐    ┌─────────────┐
│  Receiver   │───▶│  Processor   │───▶│  Exporter   │
└─────────────┘    └──────────────┘    └─────────────┘
   (입력 끝단)         (가공)             (출력 끝단)
```

각 컴포넌트는 **시그널 타입을 명시**:
- `traces` 만 처리하는 컴포넌트
- `metrics` 만 처리하는 컴포넌트
- 모든 시그널을 처리하는 컴포넌트

이 컴포넌트들을 묶어 **Pipeline**을 구성합니다 (시그널별로 별도 파이프라인).

### 추가 컴포넌트

- **Extension** — 파이프라인 외부에서 동작하는 부가 기능 (헬스체크, pprof, zPages, OAuth)
- **Connector** — 한 파이프라인의 출력을 다른 파이프라인의 입력으로 (예: Span을 Metric으로 변환)

---

## Receivers

데이터를 받아들이는 입구입니다. 대표적인 것들:

### otlp

OTLP 프로토콜 수신. Collector를 띄우는 가장 표준적인 receiver.

```yaml
receivers:
  otlp:
    protocols:
      grpc:
        endpoint: 0.0.0.0:4317
      http:
        endpoint: 0.0.0.0:4318
```

### prometheus

Prometheus `/metrics` 엔드포인트를 scrape.

```yaml
receivers:
  prometheus:
    config:
      scrape_configs:
        - job_name: my-app
          static_configs:
            - targets: ['app:9090']
```

### jaeger / zipkin

레거시 trace 포맷 호환.

```yaml
receivers:
  jaeger:
    protocols:
      thrift_http:
      grpc:
  zipkin:
    endpoint: 0.0.0.0:9411
```

### filelog / journald / syslog

로그 수집:

```yaml
receivers:
  filelog:
    include: [/var/log/myapp/*.log]
    operators:
      - type: regex_parser
        regex: '^(?P<time>\S+) (?P<level>\S+) (?P<msg>.*)$'

  journald:
    units: [nginx.service]
    priority: info
```

### hostmetrics

호스트 메트릭(CPU, 메모리, 디스크, 네트워크) 자체 수집:

```yaml
receivers:
  hostmetrics:
    collection_interval: 30s
    scrapers:
      cpu: {}
      memory: {}
      disk: {}
      filesystem: {}
      load: {}
      network: {}
```

### kubeletstats / k8s_cluster

K8s 환경 메트릭:

```yaml
receivers:
  kubeletstats:
    collection_interval: 30s
    auth_type: serviceAccount
    endpoint: ${env:K8S_NODE_NAME}:10250

  k8s_cluster:
    auth_type: serviceAccount
```

---

## Processors

데이터를 변형·필터링·샘플링하는 단계. 순서가 중요합니다.

### batch

**거의 모든 파이프라인의 필수 processor**. 데이터를 묶어 export 효율을 높임.

```yaml
processors:
  batch:
    timeout: 1s
    send_batch_size: 1024
    send_batch_max_size: 2048
```

### memory_limiter

메모리 사용량을 모니터링하고 임계치 초과 시 receive를 거부 (배압).

```yaml
processors:
  memory_limiter:
    check_interval: 1s
    limit_mib: 512
    spike_limit_mib: 128
```

**모든 파이프라인의 첫 processor로 권장**.

### resource

Resource attribute 추가/수정/삭제:

```yaml
processors:
  resource:
    attributes:
      - key: deployment.environment
        value: production
        action: upsert
      - key: cloud.region
        from_attribute: aws_region
        action: insert
```

### attributes

Span/Metric/Log의 attribute 조작:

```yaml
processors:
  attributes:
    actions:
      - key: http.user_agent
        action: delete                    # PII 제거
      - key: db.statement
        action: hash                       # 마스킹
      - key: env
        from_attribute: deployment.environment
        action: insert
```

### filter

특정 데이터를 drop:

```yaml
processors:
  filter/health:
    error_mode: ignore
    traces:
      span:
        - 'attributes["http.target"] == "/healthz"'   # health check 무시
```

### tail_sampling

Trace가 끝난 뒤 전체를 보고 샘플링 결정:

```yaml
processors:
  tail_sampling:
    decision_wait: 10s
    num_traces: 50000
    policies:
      - name: errors
        type: status_code
        status_code: { status_codes: [ERROR] }
      - name: slow
        type: latency
        latency: { threshold_ms: 500 }
      - name: random-1pct
        type: probabilistic
        probabilistic: { sampling_percentage: 1 }
```

### k8sattributes

Pod의 K8s 메타데이터 자동 부착:

```yaml
processors:
  k8sattributes:
    auth_type: serviceAccount
    extract:
      metadata: [namespace, pod, node, deployment]
      labels:
        - tag_name: app
          key: app.kubernetes.io/name
```

### transform (OTTL)

OpenTelemetry Transformation Language로 복잡한 변환:

```yaml
processors:
  transform:
    metric_statements:
      - context: datapoint
        statements:
          - set(attributes["status_class"], Concat([Substring(attributes["http.status_code"], 0, 1), "xx"], ""))
```

OTTL은 SQL/grep 같은 표현식으로 데이터를 가공할 수 있는 mini-language.

---

## Exporters

데이터를 외부로 내보내는 출구입니다.

### otlp / otlphttp

다른 OTLP 수신자(다른 Collector, Tempo, Jaeger v2, SaaS)로 전송:

```yaml
exporters:
  otlp:
    endpoint: tempo:4317
    tls:
      insecure: true

  otlphttp/saas:
    endpoint: https://otlp.example.com:4318
    headers:
      Authorization: "Bearer ${env:API_KEY}"
```

### prometheus / prometheusremotewrite

Metric을 Prometheus 형식으로 노출:

```yaml
exporters:
  prometheus:
    endpoint: 0.0.0.0:8889       # /metrics 엔드포인트
    resource_to_telemetry_conversion:
      enabled: true

  prometheusremotewrite:
    endpoint: https://mimir:9009/api/v1/push
```

### loki

Log를 Loki로:

```yaml
exporters:
  loki:
    endpoint: http://loki:3100/loki/api/v1/push
```

### file

디버깅용. JSON 라인으로 파일에 기록:

```yaml
exporters:
  file:
    path: /var/log/otel/traces.json
```

### debug (구 logging)

표준 출력으로 덤프. 개발/디버깅용:

```yaml
exporters:
  debug:
    verbosity: detailed
```

### kafka

Kafka로 전송 (대용량 fan-out):

```yaml
exporters:
  kafka:
    brokers: [kafka:9092]
    topic: otlp_spans
    encoding: otlp_proto
```

### Sending Queue & Retry

대부분의 OTLP exporter는 큐와 재시도를 내장:

```yaml
exporters:
  otlp:
    endpoint: backend:4317
    sending_queue:
      enabled: true
      num_consumers: 10
      queue_size: 5000
      storage: file_storage     # 디스크 백업 (extension 필요)
    retry_on_failure:
      enabled: true
      initial_interval: 5s
      max_interval: 30s
      max_elapsed_time: 300s
```

---

## Connectors

**Connector**는 한 파이프라인의 exporter처럼 동작하면서 동시에 다른 파이프라인의 receiver처럼 동작합니다 → 시그널 변환·라우팅에 활용됩니다.

### spanmetrics

Span에서 RED 메트릭 자동 생성:

```yaml
connectors:
  spanmetrics:
    histogram:
      explicit:
        buckets: [10ms, 50ms, 100ms, 500ms, 1s, 5s]
    dimensions:
      - name: http.method
      - name: http.status_code
```

→ Span을 받아 `calls_total`, `duration_milliseconds_bucket` 등의 메트릭을 생성하고 metric 파이프라인으로 전달합니다.

### routing

조건에 따라 다른 파이프라인으로 분기:

```yaml
connectors:
  routing:
    table:
      - statement: route() where attributes["env"] == "prod"
        pipelines: [traces/prod]
      - statement: route() where attributes["env"] == "dev"
        pipelines: [traces/dev]
```

### exceptions

Span의 exception event에서 별도 trace를 만들어내는 등 특수 변환.

---

## Pipelines 설정

전체 설정의 핵심은 `service.pipelines` 섹션입니다.

```yaml
receivers:
  otlp: ...
  prometheus: ...

processors:
  memory_limiter: ...
  batch: ...
  k8sattributes: ...
  tail_sampling: ...

exporters:
  otlp/tempo: ...
  prometheusremotewrite: ...
  otlphttp/loki: ...

connectors:
  spanmetrics: ...

service:
  pipelines:
    traces:
      receivers: [otlp]
      processors: [memory_limiter, k8sattributes, tail_sampling, batch]
      exporters: [otlp/tempo, spanmetrics]   # spanmetrics는 connector

    metrics:
      receivers: [otlp, prometheus, spanmetrics]   # connector가 receiver 자리
      processors: [memory_limiter, batch]
      exporters: [prometheusremotewrite]

    logs:
      receivers: [otlp]
      processors: [memory_limiter, k8sattributes, batch]
      exporters: [otlphttp/loki]

  telemetry:
    metrics:
      address: 0.0.0.0:8888    # Collector 자체의 metric 노출
    logs:
      level: info
```

### 멀티 파이프라인

같은 시그널에 대해 여러 파이프라인을 정의할 수 있습니다 — 이름 뒤에 `/`로 구분:

```yaml
service:
  pipelines:
    traces/sampled:
      receivers: [otlp]
      processors: [tail_sampling, batch]
      exporters: [otlp/tempo]
    traces/raw:
      receivers: [otlp]
      processors: [batch]
      exporters: [file/raw]    # 디버깅용 전체 저장
```

### Processor 순서

파이프라인 내 processor는 **선언 순서대로** 실행됩니다. 권장 순서:

```
memory_limiter → k8sattributes/resource → filter → tail_sampling → batch
```

`batch` 는 보통 **마지막**, `memory_limiter` 는 **처음** 에 둡니다.

---

## 배포 패턴

### 1. No Collector

SDK가 직접 백엔드로 OTLP를 보냄.

```
[App + SDK] ───OTLP───▶ [Backend]
```

장점: 단순, 1홉. 작은 규모/PoC에 적합.
단점: 가공 불가, 백엔드 변경 시 앱 재배포.

### 2. Agent (사이드카 / DaemonSet / 호스트당 1개)

각 노드/Pod에 Collector 인스턴스. 앱은 localhost로 전송.

```
[App + SDK] ──localhost──▶ [Agent Collector] ──▶ [Backend]
```

장점:
- 앱과 같은 노드라 네트워크 비용 거의 0
- 노드 메타데이터(K8s, host) 부착에 유리
- 앱 입장에서는 항상 살아있는 localhost

단점: 노드마다 Collector 리소스 필요.

### 3. Gateway (중앙 집중)

여러 앱이 같은 Collector 클러스터로 보냄.

```
[App + SDK] ─┐
[App + SDK] ─┼──▶ [Gateway Collector cluster] ──▶ [Backend]
[App + SDK] ─┘
```

장점:
- Tail sampling이 가능 (한 trace의 모든 Span이 모임)
- 백엔드 인증·라우팅을 한 곳에서 관리
- 큰 디스크 큐로 장애 복구

단점: 한 홉 추가, Gateway 자체가 고가용성 필요.

### 4. Agent + Gateway (대규모 표준)

```
[App + SDK] ──▶ [Agent (DaemonSet)] ──▶ [Gateway cluster] ──▶ [Backend]
```

가장 흔한 운영 패턴:
- Agent: K8s 메타 부착, 노드 메트릭 수집, 1차 batch
- Gateway: tail sampling, 백엔드 라우팅, 큰 버퍼

#### Tail sampling을 위한 LB

Gateway가 여러 인스턴스일 때 같은 trace의 모든 Span이 같은 인스턴스로 가야 함 → `loadbalancing` exporter가 trace_id로 hash 기반 라우팅:

```yaml
exporters:
  loadbalancing:
    routing_key: traceID
    protocol:
      otlp:
        tls: { insecure: true }
    resolver:
      dns:
        hostname: gateway.otel.svc.cluster.local
```

---

## 배포판 (Distribution)

Collector는 컴포넌트가 매우 많아 **모든 컴포넌트를 포함하는 단일 빌드는 비대**합니다. 따라서 빌드를 분리합니다.

### Core Distribution

`opentelemetry-collector` 리포의 핵심 컴포넌트만:
- otlp, debug, otlphttp 등 표준 컴포넌트
- 안정성·릴리스 주기 보장

### Contrib Distribution

`opentelemetry-collector-contrib` 의 모든 컴포넌트 포함:
- jaeger, zipkin, prometheus 등 호환 컴포넌트
- k8sattributes, tail_sampling 등 운영 필수 processor
- 클라우드별 receiver/exporter
- **운영에서 거의 항상 contrib를 사용**

### Custom Distribution (`ocb`)

OpenTelemetry Collector Builder(`ocb`)로 필요한 컴포넌트만 선택해 자체 바이너리를 생성합니다. 보안·성능을 위해 큰 조직을 중심으로 직접 빌드하는 추세입니다.

### 벤더 배포판

- **Grafana Alloy** — Grafana 스택용 OTel Collector 배포판 (구 `grafana-agent` flow mode를 OTel 기반으로 통합)
- **AWS Distro for OpenTelemetry (ADOT)** — AWS 통합 컴포넌트 추가
- **Elastic Distro**, **Datadog Distribution** 등

---

## 운영 체크리스트

- [ ] `memory_limiter` 와 `batch` 가 모든 파이프라인에 존재
- [ ] `service.telemetry.metrics` 로 Collector 자체 메트릭 노출
- [ ] `health_check` extension 활성화 (K8s readiness)
- [ ] Sending queue를 디스크 백업(`file_storage`)과 함께 사용
- [ ] Tail sampling 사용 시 loadbalancing exporter로 trace 일관성 보장
- [ ] K8s 환경이면 `k8sattributes` processor로 메타 부착
- [ ] Contrib distribution 또는 ocb 빌드 사용

---

## 참고 자료

- Collector 공식: https://opentelemetry.io/docs/collector/
- Core 리포: https://github.com/open-telemetry/opentelemetry-collector
- Contrib 리포: https://github.com/open-telemetry/opentelemetry-collector-contrib
- ocb (Builder): https://github.com/open-telemetry/opentelemetry-collector/tree/main/cmd/builder
- OTTL: https://github.com/open-telemetry/opentelemetry-collector-contrib/tree/main/pkg/ottl
