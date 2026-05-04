# Grafana Tempo 개요

> 이 문서는 Grafana Tempo 공식 문서의 "Learn about tracing" 섹션을 한국어로 정리한 것입니다.
> 원본: https://grafana.com/docs/tempo/latest/

---

## 목차

1. [Tempo란 무엇인가](#tempo란-무엇인가)
2. [분산 트레이싱 개념](#분산-트레이싱-개념)
3. [Tempo의 주요 기능](#tempo의-주요-기능)
4. [Tempo 트레이싱 스택](#tempo-트레이싱-스택)
5. [지원 프로토콜](#지원-프로토콜)
6. [Tempo와 다른 트레이싱 백엔드 비교](#tempo와-다른-트레이싱-백엔드-비교)

---

## Tempo란 무엇인가

**Grafana Tempo**는 오픈소스이며 대규모(massively scalable)로 동작하는 **분산 트레이싱 백엔드(distributed tracing backend)** 입니다.

### 핵심 특징

- **오픈소스**: AGPL-3.0 라이선스
- **대규모 확장성**: 수평 확장 가능한 마이크로서비스 아키텍처
- **저비용**: 인덱싱 없이 오브젝트 스토리지(S3, GCS, Azure Blob)에 트레이스 저장
- **TraceID 기반 검색**: TraceID로 직접 트레이스 조회 가능
- **TraceQL**: 강력한 쿼리 언어로 정밀한 스팬 선택 및 필터링
- **다른 신호와의 통합**: 로그(Loki), 메트릭(Mimir/Prometheus)과 연결

---

## 분산 트레이싱 개념

### 분산 트레이싱이란?

분산 트레이싱은 **요청이 여러 애플리케이션과 서비스를 통과하면서의 생명주기를 시각화** 하는 기법입니다. 마이크로서비스 환경에서 단일 사용자 요청이 수십 개의 서비스를 거치는 경우가 많아, 각 서비스에서 어떤 일이 발생했는지를 추적하는 것이 매우 중요합니다.

### 기본 용어

#### Trace (트레이스)

하나의 요청이 시스템 전체를 통과하는 전체 흐름을 나타냅니다. 트레이스는 여러 스팬(Span)으로 구성됩니다.

#### Span (스팬)

트레이스 내에서 단일 작업(operation)을 나타내는 단위입니다. 스팬에는 다음 정보가 포함됩니다.

- **Operation Name**: 작업 이름 (예: `HTTP GET /api/users`)
- **Start Time / Duration**: 시작 시간과 소요 시간
- **Parent Span ID**: 부모 스팬 참조
- **Attributes/Tags**: 키-값 메타데이터
- **Events / Logs**: 스팬 내부에서 발생한 이벤트
- **Status**: 성공/실패 상태

#### TraceID

전체 트레이스를 식별하는 고유 ID입니다. 일반적으로 16바이트(128비트) 값으로 표현됩니다.

#### SpanID

각 스팬을 식별하는 고유 ID입니다. 일반적으로 8바이트(64비트)입니다.

#### Span Context

TraceID, SpanID, Trace Flags 등 트레이스 정보를 서비스 간에 전파하는 데 사용되는 컨텍스트입니다.

### 트레이스 시각화 예시

```
Trace ID: abc123...
│
├── Span: HTTP GET /api/checkout (200ms)
│   ├── Span: validate user (10ms)
│   ├── Span: query inventory (50ms)
│   │   └── Span: SELECT * FROM products (30ms)
│   ├── Span: calculate price (5ms)
│   └── Span: process payment (120ms)
│       ├── Span: HTTP POST payment-gateway (100ms)
│       └── Span: write order to DB (15ms)
```

---

## Tempo의 주요 기능

### 트레이스 저장 및 검색

- 오브젝트 스토리지(S3, GCS, Azure Blob)에 트레이스 영속화
- TraceID로 직접 트레이스 조회
- TraceQL로 속성 기반 검색

### 메트릭 생성

**Span Metrics Generator**가 스팬 데이터에서 자동으로 RED 메트릭(Rate, Error, Duration)을 생성합니다. 이 메트릭은 Prometheus나 Mimir에 저장될 수 있습니다.

### Service Graph

서비스 간 호출 관계를 자동으로 발견하고 그래프로 시각화합니다. 어떤 서비스가 어떤 서비스를 호출하는지, 호출 빈도와 에러율을 한눈에 파악할 수 있습니다.

### 로그 및 메트릭과의 통합

- **Logs to Traces**: Loki에서 로그를 보다가 관련 트레이스로 이동 (Trace ID를 로그에 포함)
- **Traces to Logs**: 트레이스 보던 중 관련 로그로 이동
- **Metrics to Traces**: 메트릭 이상치 발견 시 관련 트레이스로 이동 (Exemplars)

### TraceQL

Tempo의 **트레이스 선택 쿼리 언어**입니다. 시간 범위, 스팬 속성, 리소스 속성, 지속 시간 등 다양한 조건으로 트레이스를 정밀하게 검색할 수 있습니다.

```traceql
{ resource.service.name = "frontend" && duration > 100ms && status = error }
```

---

## Tempo 트레이싱 스택

전형적인 Tempo 기반 트레이싱 스택은 **4가지 컴포넌트** 로 구성됩니다.

### 1. 클라이언트 계측 (Client Instrumentation)

애플리케이션에서 스팬을 생성합니다. 다음 라이브러리 사용 가능합니다.

- **OpenTelemetry SDK**: 권장. 다양한 언어 지원 (Java, Go, Python, Node.js, .NET, Ruby 등)
- **Jaeger Client**: 레거시. OpenTelemetry로 마이그레이션 권장
- **Zipkin Client**: 레거시. OpenTelemetry로 마이그레이션 권장

### 2. 파이프라인 (Pipeline)

수집된 트레이스를 버퍼링하고 Tempo로 전송합니다.

- **Grafana Alloy** (권장)
- **OpenTelemetry Collector**
- **Jaeger Agent / Collector** (레거시)

### 3. 백엔드 (Backend)

**Tempo** 가 트레이스를 저장하고 조회합니다.

### 4. 시각화 (Visualization)

**Grafana** 의 내장 Tempo 데이터 소스를 통해 트레이스를 시각화합니다.

```
[Application]
  + OTel SDK
      |
      v (OTLP/Jaeger/Zipkin)
[Grafana Alloy / OTel Collector]
      |
      v (OTLP)
[Tempo Distributor] -> [Ingester] -> [Object Storage]
                           |              ^
                           v              |
                     [Querier] <----------+
                           ^
                           |
                       [Grafana]
```

---

## 지원 프로토콜

Tempo는 다양한 트레이스 수집 프로토콜을 지원합니다.

| 프로토콜 | 포트 (기본) | 비고 |
|---------|------------|------|
| OTLP/gRPC | 4317 | OpenTelemetry 표준 (권장) |
| OTLP/HTTP | 4318 | OpenTelemetry 표준 |
| Jaeger gRPC | 14250 | Jaeger 호환 |
| Jaeger Thrift HTTP | 14268 | Jaeger 호환 |
| Jaeger Thrift Compact | 6831 | UDP, 레거시 |
| Jaeger Thrift Binary | 6832 | UDP, 레거시 |
| Zipkin | 9411 | Zipkin 호환 |
| OpenCensus | 55678 | 레거시 |

---

## Tempo와 다른 트레이싱 백엔드 비교

| 항목 | Tempo | Jaeger | Zipkin |
|------|-------|--------|--------|
| 라이선스 | AGPL-3.0 | Apache 2.0 | Apache 2.0 |
| 인덱스 | 없음 (TraceID 기반) | 모든 속성 인덱싱 | 모든 속성 인덱싱 |
| 스토리지 | 오브젝트 스토리지 | Cassandra, Elasticsearch | MySQL, Cassandra, ES |
| 비용 | 매우 저렴 | 중-고 | 중-고 |
| 쿼리 언어 | TraceQL | 제한적 | 제한적 |
| 확장성 | 페타바이트 규모 | 큰 규모 | 중간 규모 |
| Grafana 통합 | 네이티브 | 데이터소스 | 데이터소스 |
| 메트릭 생성 | 내장 | 별도 | 별도 |
| 서비스 그래프 | 내장 | 별도 | 별도 |

### Tempo의 핵심 차별점

**"인덱스 없는 디자인"**: Tempo는 의도적으로 인덱스를 만들지 않습니다. TraceID로만 직접 조회 가능하며, 검색은 TraceQL을 통해 수행됩니다. 이로 인해:

- 스토리지 비용이 매우 낮음 (오브젝트 스토리지만 사용)
- 인덱스 유지보수 부담 없음
- 100% 샘플링도 비용 효율적으로 가능

---

## 다음 단계

- [02_architecture.md](./02_architecture.md) - Tempo 아키텍처 상세
- [03_deployment.md](./03_deployment.md) - 배포 모드 (Monolithic, Microservices)
- [04_traceql.md](./04_traceql.md) - TraceQL 쿼리 언어
- [05_metrics_generator.md](./05_metrics_generator.md) - Span Metrics 및 Service Graph
- [06_instrumentation.md](./06_instrumentation.md) - 애플리케이션 계측
