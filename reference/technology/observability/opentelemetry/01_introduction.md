# OpenTelemetry 소개

> 원본: https://opentelemetry.io/docs/ , https://github.com/open-telemetry/opentelemetry-specification

---

## 목차

1. [OpenTelemetry란?](#opentelemetry란)
2. [왜 필요한가](#왜-필요한가)
3. [역사](#역사)
4. [구성 요소](#구성-요소)
5. [데이터 흐름](#데이터-흐름)
6. [언어/생태계 지원](#언어생태계-지원)
7. [학습 출발점](#학습-출발점)

---

## OpenTelemetry란?

**OpenTelemetry (OTel)** 는 **분산 시스템의 텔레메트리(Telemetry) 데이터를 생성·수집·내보내기 위한 벤더 중립적 표준** 입니다. CNCF 인큐베이팅을 거쳐 졸업(graduated)한 프로젝트로, Kubernetes 다음으로 활발한 CNCF 프로젝트입니다.

핵심 정의:
- **API + SDK + Protocol + Collector** 의 통합 패키지
- 백엔드(Jaeger, Tempo, Prometheus, Datadog 등)에 **종속되지 않는** 계측 코드
- **Traces, Metrics, Logs** 를 일관된 모델로 다룸 (최근 Profiles 추가)

OpenTelemetry는 "**관측 가능성(Observability)을 위한 USB-C**"라고 비유됩니다 — 어떤 백엔드를 꽂아도 동작하는 표준 인터페이스.

---

## 왜 필요한가

### 1. 벤더 록인 문제

기존에는 백엔드마다 SDK가 달랐습니다:
- Jaeger Client → Jaeger 백엔드만
- Zipkin Brave → Zipkin 백엔드만
- Datadog Agent → Datadog만
- Prometheus Client → Prometheus만

백엔드를 바꾸면 **모든 애플리케이션 코드의 계측을 다시 작성** 해야 했습니다.

### 2. 신호 분리 문제

Trace, Metric, Log가 서로 다른 라이브러리·포맷·전송 채널을 사용해 **상관관계를 추적하기 어려웠습니다**. 같은 요청의 trace와 log를 연결하려면 trace_id를 수동으로 주입해야 했습니다.

### 3. 표준의 부재

용어가 통일되지 않았습니다:
- "request"인가 "span"인가
- "service.name"인가 "app"인가 "process"인가
- HTTP 상태 코드를 attribute로 어떻게 기록할 것인가

OpenTelemetry는 **Semantic Conventions** 로 이를 표준화합니다.

---

## 역사

### 2016 — OpenTracing

CNCF 아래 분산 트레이싱 API 표준. Jaeger, Zipkin이 호환. 단점: API만 있고 SDK는 벤더가 따로 구현, Metric/Log는 다루지 않음.

### 2018 — OpenCensus

Google이 주도. API + SDK + Collector(Agent)를 모두 제공. Trace + Metric 통합. 단점: OpenTracing과 분열.

### 2019 — OpenTelemetry로 합병

OpenTracing + OpenCensus 두 프로젝트가 통합 발표. CNCF 샌드박스 → 인큐베이팅 진입.

### 2021 — Tracing GA

Trace 시그널이 Stable로 선언. 대부분의 언어 SDK에서 프로덕션 사용 가능.

### 2023 — Metrics GA, Logs GA

Metric 시그널과 Log 시그널이 차례로 Stable에 도달. 세 시그널이 모두 안정화됨.

### 2024 — CNCF Graduated

CNCF에서 졸업 단계로 격상. Kubernetes 다음으로 두 번째로 활발한 프로젝트로 인정.

### 2025+ — Profiles

네 번째 시그널 **Profiles** 가 실험적으로 도입. CPU/메모리 프로파일링 데이터를 표준 형식으로 다룸 (eBPF·pprof와 연계).

---

## 구성 요소

OpenTelemetry는 다음 레이어로 구성됩니다:

### 1. Specification

언어 중립적 사양으로, 모든 구현이 따라야 하는 기준입니다.
- **API Specification** — 사용자 코드가 호출하는 인터페이스
- **SDK Specification** — API의 표준 구현 동작
- **Data Specification** — 데이터 모델, OTLP 포맷
- **Semantic Conventions** — attribute 이름/값 표준 (예: `http.request.method`, `service.name`)

### 2. API

애플리케이션 코드가 호출하는 **얇은 추상 계층**. SDK가 없으면 no-op로 동작 (실패하지 않음).
- `Tracer.startSpan()`
- `Meter.createCounter()`
- `LoggerProvider.get()`

라이브러리 작성자는 API에만 의존 → 사용자가 SDK를 선택.

### 3. SDK

API의 구체적 구현. Sampling, Batching, Export 등 실제 동작을 담당.
- TracerProvider, MeterProvider, LoggerProvider
- SpanProcessor (BatchSpanProcessor / SimpleSpanProcessor)
- MetricReader (PeriodicExportingMetricReader)
- Exporter (OTLP, Console, Prometheus, ...)

### 4. Instrumentation

애플리케이션과 라이브러리에 계측을 삽입하는 코드.
- **Manual** — 사용자가 직접 `tracer.startSpan()` 호출
- **Library Instrumentation** — HTTP 클라이언트, DB 드라이버 등 라이브러리 자동 계측 패키지
- **Auto-instrumentation / Zero-code** — 바이트코드 조작(Java agent), eBPF, monkey-patching으로 코드 수정 없이 계측

### 5. Collector

벤더 중립적 텔레메트리 파이프라인으로, 별도의 프로세스 또는 사이드카로 동작합니다.
- 다양한 포맷 수신 (OTLP, Jaeger, Zipkin, Prometheus, Fluentd, ...)
- 필터링, 가공, 샘플링
- 다양한 백엔드로 전송

### 6. OTLP

**OpenTelemetry Protocol**. 텔레메트리 시그널을 전송하는 표준 와이어 프로토콜입니다.
- gRPC (포트 4317) 또는 HTTP/protobuf (포트 4318)
- Protobuf 기반 데이터 모델

---

## 데이터 흐름

전형적인 OpenTelemetry 데이터 흐름:

```
┌─────────────────────┐
│   Application       │
│  ┌───────────────┐  │
│  │  OTel API     │  │  ← 계측 코드가 호출
│  └───────┬───────┘  │
│  ┌───────▼───────┐  │
│  │  OTel SDK     │  │  ← Sample, Batch
│  │  + Exporter   │  │
│  └───────┬───────┘  │
└──────────┼──────────┘
           │ OTLP (gRPC/HTTP)
           ▼
┌─────────────────────┐
│  OTel Collector     │
│  Receiver → Processor → Exporter
└──────────┬──────────┘
           │
   ┌───────┴───────┬──────────┐
   ▼               ▼          ▼
┌────────┐   ┌─────────┐  ┌──────────┐
│ Tempo  │   │  Mimir  │  │   Loki   │
│(Trace) │   │ (Metric)│  │  (Log)   │
└────────┘   └─────────┘  └──────────┘
```

각 단계는 선택적입니다. 가장 단순한 구성은 SDK가 직접 백엔드로 OTLP를 보내는 것이고, 가장 복잡한 구성은 Agent Collector → Gateway Collector → 여러 백엔드로 분기하는 것입니다.

---

## 언어/생태계 지원

### Stable 언어 SDK (2026 기준)

Trace/Metric/Log 모두 GA에 도달한 언어:

- **Java** — `opentelemetry-java`, Java agent (auto-instrumentation 가장 성숙)
- **Go** — `opentelemetry-go`
- **Python** — `opentelemetry-python`, auto-instrumentation 지원
- **Node.js / JavaScript** — `opentelemetry-js`
- **.NET** — `OpenTelemetry .NET`, `System.Diagnostics.Activity`와 통합
- **Rust** — `opentelemetry-rust`
- **Ruby**, **PHP**, **Erlang/Elixir**, **Swift** 등

### 프로토콜·포맷 호환

OTLP 외에도 Collector가 다음을 수신할 수 있어 **점진적 마이그레이션** 가능:
- Jaeger (Thrift, gRPC)
- Zipkin
- Prometheus (scrape, Remote Write)
- StatsD
- Fluentd / Fluent Bit
- syslog

### 대표 백엔드

OTLP를 직접 수신하는 백엔드:
- **Grafana 스택**: Tempo (trace), Mimir/Prometheus (metric), Loki (log)
- **Jaeger v2** — 자체가 Collector 기반
- **Elastic APM**, **Datadog**, **New Relic**, **Honeycomb**, **Splunk**

---

## 학습 출발점

OpenTelemetry는 다루는 범위가 넓기 때문에 단계적으로 접근하는 것이 효율적입니다.

### 1단계: 개념 이해

- 본 문서의 02_signals 까지 읽고 Trace/Metric/Log 데이터 모델 파악
- 핵심: "Span은 무엇인가", "Resource와 Scope의 차이", "OTLP가 무엇을 보내는가"

### 2단계: 한 언어로 hello world

- 익숙한 언어의 SDK quickstart를 따라 콘솔 Exporter로 trace를 출력
- `tracer.startSpan()` → console에 trace_id가 찍히는 것을 확인

### 3단계: Collector 띄우기

- 로컬에 `otelcol` 바이너리 또는 Docker로 Collector 실행
- Receiver: OTLP, Exporter: logging
- 애플리케이션 → Collector → 콘솔 출력 흐름 확인

### 4단계: 백엔드 연결

- Tempo + Grafana, 또는 Jaeger를 띄워 실제로 Span을 시각화
- Trace에 attribute, event, link를 추가하며 데이터 모델 체득

### 5단계: 운영 고려사항

- Sampling 전략 (Head vs Tail)
- Collector 배포 패턴 (Agent / Gateway)
- 카디널리티 폭발 방지
- Semantic Conventions 준수

---

## 참고 자료

- 공식 문서: https://opentelemetry.io/docs/
- Specification: https://github.com/open-telemetry/opentelemetry-specification
- Semantic Conventions: https://github.com/open-telemetry/semantic-conventions
- Collector: https://github.com/open-telemetry/opentelemetry-collector
- Contrib (수신기·내보내기 모음): https://github.com/open-telemetry/opentelemetry-collector-contrib
