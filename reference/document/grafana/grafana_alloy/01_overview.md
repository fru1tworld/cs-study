# Grafana Alloy 개요

> 이 문서는 Grafana Alloy 공식 문서의 "Introduction to Alloy" 섹션을 한국어로 정리한 것입니다.
> 원본: https://grafana.com/docs/alloy/latest/

---

## 목차

1. [Alloy란 무엇인가](#alloy란-무엇인가)
2. [Alloy의 주요 기능](#alloy의-주요-기능)
3. [지원 신호 유형](#지원-신호-유형)
4. [Alloy 아키텍처](#alloy-아키텍처)
5. [핵심 개념](#핵심-개념)
6. [Alloy와 다른 수집기 비교](#alloy와-다른-수집기-비교)
7. [마이그레이션 경로](#마이그레이션-경로)

---

## Alloy란 무엇인가

**Grafana Alloy** 는 OpenTelemetry Collector를 기반으로 하는 **오픈소스 텔레메트리 수집기(open-source telemetry collector)** 입니다.

### 핵심 특징

- **OpenTelemetry Collector 배포판**: OTel Collector를 기반으로 Grafana 생태계 통합
- **다중 신호 지원**: 메트릭, 로그, 트레이스, 프로파일을 단일 도구로 수집
- **벤더 중립(Vendor-neutral)**: Grafana Cloud뿐만 아니라 다양한 백엔드로 전송 가능
- **풍부한 통합**: Prometheus, Loki, Tempo, Pyroscope, OpenTelemetry 등 지원
- **컴포넌트 기반**: 모듈식 컴포넌트로 유연한 파이프라인 구성

### Alloy가 등장한 배경

기존에는 Grafana Agent (Static, Flow), Promtail, OpenTelemetry Collector 등 **여러 수집기를 별도로 운영** 해야 했습니다. Alloy는 이를 **단일 도구로 통합** 하여 운영 복잡성을 줄이는 것이 목표입니다.

---

## Alloy의 주요 기능

### 1. 다중 신호 (Multi-signal) 지원

각 신호 유형별로 별도 수집기를 실행할 필요 없이, 하나의 도구로 모든 텔레메트리 데이터를 수집합니다.

| 신호 | 지원 |
|------|------|
| Metrics | Prometheus, OpenTelemetry, StatsD 등 |
| Logs | Loki, Syslog, Journal, File, Kubernetes 등 |
| Traces | OTLP, Jaeger, Zipkin 등 |
| Profiles | Pyroscope (Continuous Profiling) |

### 2. 유연한 백엔드 연결

- **Grafana Cloud** (네이티브 통합)
- **자체 관리형 Grafana Stack** (Loki, Mimir, Tempo, Pyroscope)
- **OpenTelemetry 호환 백엔드**
- **Prometheus 호환 백엔드**

### 3. 컴포넌트 기반 구성

Alloy 구성은 **컴포넌트(component)** 단위로 작성됩니다. 각 컴포넌트는 독립적으로 동작하며, 컴포넌트 간 출력/입력으로 파이프라인을 구성합니다.

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

### 4. OpenTelemetry 네이티브

OpenTelemetry Collector의 모든 기능을 사용할 수 있습니다.

- OTLP Receiver (gRPC, HTTP)
- 다양한 Processor (Batch, Memory Limiter, Resource 등)
- 다양한 Exporter

### 5. 클러스터링

여러 Alloy 인스턴스를 클러스터로 구성하여 워크로드를 자동으로 분배할 수 있습니다.

- 자동 타겟 분배
- 고가용성
- 무중단 스케일링

---

## 지원 신호 유형

### 메트릭 (Metrics)

#### 수집 (Collection)
- `prometheus.scrape`: Prometheus 스타일 스크래핑
- `prometheus.exporter.*`: 각종 Exporter 내장 (Unix, MySQL, Redis, MongoDB 등)
- `otelcol.receiver.prometheus`: OpenTelemetry 형식
- `otelcol.receiver.otlp`: OTLP

#### 처리 (Processing)
- `prometheus.relabel`: 라벨 변환
- `otelcol.processor.batch`: 배치 처리
- `otelcol.processor.transform`: 데이터 변환

#### 전송 (Export)
- `prometheus.remote_write`: Prometheus Remote Write (Mimir, Cortex, Thanos)
- `otelcol.exporter.otlp`: OTLP Exporter

### 로그 (Logs)

#### 수집
- `loki.source.file`: 파일 로그
- `loki.source.kubernetes`: 쿠버네티스 Pod 로그
- `loki.source.journal`: systemd journal
- `loki.source.syslog`: Syslog
- `otelcol.receiver.otlp`: OTLP 로그

#### 처리
- `loki.process`: Promtail 스타일 파이프라인
- `loki.relabel`: 라벨 변환
- `otelcol.processor.*`: OTel 프로세서

#### 전송
- `loki.write`: Loki로 전송
- `otelcol.exporter.otlp`: OTLP

### 트레이스 (Traces)

#### 수집
- `otelcol.receiver.otlp`: OTLP (gRPC, HTTP)
- `otelcol.receiver.jaeger`: Jaeger 호환
- `otelcol.receiver.zipkin`: Zipkin 호환

#### 처리
- `otelcol.processor.batch`: 배치
- `otelcol.processor.tail_sampling`: 테일 샘플링
- `otelcol.processor.span_metrics`: 스팬 메트릭 생성

#### 전송
- `otelcol.exporter.otlp`: Tempo 등으로 전송

### 프로파일 (Profiles)

#### 수집
- `pyroscope.scrape`: Pyroscope 스크래핑
- `pyroscope.ebpf`: eBPF 기반 프로파일링

#### 전송
- `pyroscope.write`: Pyroscope 백엔드로 전송

---

## Alloy 아키텍처

### 컴포넌트 그래프

Alloy는 컴포넌트들이 서로 연결된 **방향성 그래프(directed graph)** 로 동작합니다.

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

각 컴포넌트는:
- **Arguments (입력)**: 설정 파라미터
- **Exports (출력)**: 다른 컴포넌트에서 참조 가능한 값
- **State (상태)**: 내부 상태 (메트릭, 헬스 등)

### 단일 바이너리

Alloy는 단일 정적 Go 바이너리로 배포됩니다. 모든 컴포넌트가 동일한 바이너리에 포함되어 있습니다.

---

## 핵심 개념

### 컴포넌트 (Component)

가장 기본적인 빌딩 블록입니다. 각 컴포넌트는 특정 작업(스크래핑, 처리, 전송 등)을 수행합니다.

명명 규칙: `<namespace>.<type> "<label>"`
- 예: `prometheus.scrape "kubernetes_pods"`

### 표현식 (Expressions)

컴포넌트의 값을 참조하거나 변환할 때 사용합니다.

```alloy
// 컴포넌트 출력 참조
forward_to = [prometheus.remote_write.mimir.receiver]

// 환경 변수 사용
url = sys.env("MIMIR_URL")

// 문자열 연산
endpoint = "http://" + sys.env("HOST") + ":9009"
```

### 모듈 (Module)

재사용 가능한 구성 단위입니다. 다른 Alloy 구성에서 임포트하여 사용할 수 있습니다.

### 클러스터링 (Clustering)

여러 Alloy 인스턴스를 클러스터로 묶어 작업을 분산합니다.

```alloy
prometheus.scrape "kubernetes" {
  targets    = discovery.kubernetes.pods.targets
  forward_to = [prometheus.remote_write.mimir.receiver]
  
  clustering {
    enabled = true
  }
}
```

이 설정은 클러스터 내 Alloy 인스턴스들이 자동으로 타겟을 나눠 가져갑니다.

### 구성 파일 형식

Alloy는 자체 구성 언어(Alloy syntax, 이전엔 River라고 불림)를 사용합니다. HCL과 유사한 문법입니다.

---

## Alloy와 다른 수집기 비교

| 도구 | 메트릭 | 로그 | 트레이스 | 프로파일 | 비고 |
|------|--------|------|---------|---------|------|
| **Alloy** | O | O | O | O | 통합 솔루션 |
| Prometheus | O | X | X | X | 메트릭 전용 |
| Promtail | X | O | X | X | 로그 전용 (Loki) |
| OpenTelemetry Collector | O | O | O | X | OTel 전용 |
| Grafana Agent (Static) | O | O | O | O | Deprecated |
| Grafana Agent (Flow) | O | O | O | O | Deprecated, Alloy로 통합 |
| Vector | O | O | X | X | Datadog/Datadog Logs |
| Fluent Bit | X | O | X | X | 로그 전용 |
| Telegraf | O | O | △ | X | InfluxDB 생태계 |

---

## 마이그레이션 경로

Grafana는 다음 도구들에서 Alloy로 마이그레이션을 권장합니다.

### 1. Grafana Agent Operator → Alloy
- Operator를 통한 자동 배포에서 Alloy로 전환
- 마이그레이션 도구 제공

### 2. Prometheus → Alloy
- `prometheus.scrape`로 동일한 스크래핑 가능
- `prometheus.remote_write`로 Mimir/Cortex/Prometheus 등에 전송

### 3. Promtail → Alloy
- `loki.source.file`, `loki.process`로 동일 기능 구현
- Promtail 구성 자동 변환 도구 제공

### 4. Grafana Agent Static → Alloy
- 자동 변환 도구 제공
- `alloy convert` 커맨드 사용

### 5. Grafana Agent Flow → Alloy
- 거의 동일한 구성 사용 가능 (River → Alloy syntax)
- 직접적인 마이그레이션 가이드 제공

### 6. OpenTelemetry Collector → Alloy
- 모든 OTel 컴포넌트가 `otelcol.*`로 사용 가능
- YAML → Alloy syntax 변환 도구

---

## 다음 단계

- [02_install.md](./02_install.md) - 설치 (Docker, Kubernetes, Linux, macOS, Windows)
- [03_configure.md](./03_configure.md) - 구성 (Configuration)
- [04_components.md](./04_components.md) - 컴포넌트 레퍼런스
- [05_collect_otel.md](./05_collect_otel.md) - OpenTelemetry 데이터 수집
- [06_clustering.md](./06_clustering.md) - 클러스터링
- [07_migrate.md](./07_migrate.md) - 마이그레이션 가이드
