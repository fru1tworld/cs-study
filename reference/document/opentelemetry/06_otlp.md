# OTLP — OpenTelemetry Protocol

> SDK ↔ Collector ↔ 백엔드 사이의 표준 와이어 프로토콜을 정리합니다.

---

## 목차

1. [OTLP란?](#otlp란)
2. [전송 변종](#전송-변종)
3. [데이터 모델](#데이터-모델)
4. [요청·응답 흐름](#요청응답-흐름)
5. [부분 성공 (Partial Success)](#부분-성공-partial-success)
6. [재시도와 배압](#재시도와-배압)
7. [보안](#보안)
8. [성능 고려사항](#성능-고려사항)
9. [디버깅](#디버깅)

---

## OTLP란?

**OpenTelemetry Protocol (OTLP)** 는 OpenTelemetry가 정의한 **벤더 중립 와이어 프로토콜** 입니다.

특징:
- **Protobuf** 기반 데이터 모델 (`opentelemetry-proto` 리포에서 관리)
- 두 가지 전송 변종: **gRPC** 와 **HTTP/protobuf**
- 모든 시그널(Trace/Metric/Log/Profile)을 하나의 프로토콜로 처리
- 단방향 push 모델 (SDK·Collector가 푸시, 수신자는 ack만)

용도:
- **SDK → Collector** (가장 흔함)
- **Collector → Collector** (Agent → Gateway)
- **Collector → 백엔드** (Tempo, Mimir, Loki, Jaeger v2 등)

---

## 전송 변종

### OTLP/gRPC

- 포트: **4317** (관습)
- HTTP/2 + Protobuf
- 양방향 스트리밍은 사용하지 않음 — 단순 unary RPC
- 메서드: `Export(ExportRequest) → ExportResponse`

각 시그널마다 별도 service:
- `opentelemetry.proto.collector.trace.v1.TraceService`
- `opentelemetry.proto.collector.metrics.v1.MetricsService`
- `opentelemetry.proto.collector.logs.v1.LogsService`

장점: 헤더 압축(HPACK), 멀티플렉싱, 효율적인 binary 프레이밍
단점: HTTP/2 필요, 일부 환경에서 프록시 호환성 이슈

### OTLP/HTTP

- 포트: **4318** (관습)
- HTTP/1.1 또는 HTTP/2 모두 가능
- Body: **Protobuf** (권장) 또는 **JSON** (옵션)
- POST 엔드포인트:
  - `/v1/traces`
  - `/v1/metrics`
  - `/v1/logs`

```
POST /v1/traces HTTP/1.1
Content-Type: application/x-protobuf
<protobuf bytes>
```

장점: 어느 HTTP 프록시도 통과, 디버깅 쉬움 (curl 가능), 브라우저에서 사용 가능
단점: gRPC보다 약간 더 무거움 (헤더 등)

### 둘 중 무엇을 쓸까?

- **Server-side 애플리케이션**: gRPC 권장 — 성능·기능
- **브라우저/모바일**: HTTP/JSON — gRPC-Web이 아니면 직접 사용 불가
- **방화벽/프록시 제약**: HTTP

대부분의 SDK는 둘 다 지원하며 environment variable로 선택:
```
OTEL_EXPORTER_OTLP_PROTOCOL=grpc | http/protobuf | http/json
```

---

## 데이터 모델

OTLP의 메시지는 **Resource → InstrumentationScope → 시그널 데이터** 의 3단계 그룹화 구조입니다.

### Trace 예시 (의사 표현)

```
ExportTraceServiceRequest {
  resource_spans: [
    {
      resource: {
        attributes: [{ "service.name": "checkout" }, ...]
      },
      scope_spans: [
        {
          scope: { name: "io.opentelemetry.spring-webmvc", version: "2.10.0" },
          spans: [
            {
              trace_id: <16 bytes>,
              span_id: <8 bytes>,
              name: "GET /checkout",
              kind: SERVER,
              start_time_unix_nano: ...,
              end_time_unix_nano: ...,
              attributes: [...],
              events: [...],
              links: [...],
              status: { code: OK }
            },
            ...
          ]
        },
        {
          scope: { name: "io.opentelemetry.jdbc", version: "2.10.0" },
          spans: [...]
        }
      ]
    },
    ...
  ]
}
```

### Metric 예시

```
ExportMetricsServiceRequest {
  resource_metrics: [
    {
      resource: { ... },
      scope_metrics: [
        {
          scope: { ... },
          metrics: [
            {
              name: "http.server.request.duration",
              unit: "s",
              histogram: {
                aggregation_temporality: DELTA,
                data_points: [
                  {
                    attributes: [...],
                    start_time_unix_nano: ...,
                    time_unix_nano: ...,
                    count: 525,
                    sum: 12.34,
                    bucket_counts: [10, 50, 200, ...],
                    explicit_bounds: [0.005, 0.01, 0.025, ...],
                    exemplars: [...]
                  }
                ]
              }
            }
          ]
        }
      ]
    }
  ]
}
```

### Log 예시

```
ExportLogsServiceRequest {
  resource_logs: [
    {
      resource: { ... },
      scope_logs: [
        {
          scope: { ... },
          log_records: [
            {
              time_unix_nano: ...,
              severity_number: 17,    // ERROR
              severity_text: "ERROR",
              body: { string_value: "payment failed" },
              attributes: [...],
              trace_id: <16 bytes>,   // 자동 상관
              span_id: <8 bytes>
            }
          ]
        }
      ]
    }
  ]
}
```

### 그룹화의 의미

같은 Resource·Scope의 시그널이 묶여 전송되므로:
- 와이어 효율 (Resource attribute가 한 번만 직렬화됨)
- 수신자가 같은 그룹의 데이터를 함께 처리 가능

---

## 요청·응답 흐름

### 정상 흐름

```
SDK              Collector
  │                  │
  │── Export(req) ──▶│
  │                  │  receiver → processor → exporter
  │◀─── response ────│   (response는 즉시; export는 비동기 큐로)
  │                  │
```

응답은 보통 **거의 즉시** 옵니다. Collector가 데이터를 큐에 넣고 ack하기 때문 → "response = 백엔드 도달"이 아니라 "response = 수신자가 받음"입니다.

### 응답 객체

```protobuf
message ExportTraceServiceResponse {
  ExportTracePartialSuccess partial_success = 1;
}

message ExportTracePartialSuccess {
  int64 rejected_spans = 1;
  string error_message = 2;
}
```

`partial_success` 가 비어있으면 **전부 수락**, 채워져 있으면 일부 거부.

---

## 부분 성공 (Partial Success)

OTLP는 **부분 성공** 을 명시적으로 지원합니다. 한 요청 안의 일부 데이터만 수락될 수 있음.

```
ExportResponse {
  partial_success {
    rejected_spans: 5
    error_message: "5 spans rejected: invalid trace_id"
  }
}
```

이 경우 SDK는:
- HTTP 상태 200 / gRPC OK 로 받음
- 거부된 데이터를 **재시도하지 않음** (재시도해도 같은 결과일 가능성)
- 에러 메시지를 로그로 남김

비교 — 완전 실패는 별도 에러 코드로:
- gRPC: `UNAVAILABLE`, `RESOURCE_EXHAUSTED`, ...
- HTTP: 4xx, 5xx

---

## 재시도와 배압

### 재시도 가능한 실패

다음 응답은 **재시도 권장**:
- gRPC: `UNAVAILABLE`, `CANCELLED`, `DEADLINE_EXCEEDED`, `RESOURCE_EXHAUSTED`, `OUT_OF_RANGE`, `UNAUTHENTICATED`, `ABORTED`, `DATA_LOSS`
- HTTP: `408`, `429`, `502`, `503`, `504`

### 재시도하지 말아야 하는 실패

- gRPC: `INVALID_ARGUMENT`, `NOT_FOUND`, `ALREADY_EXISTS`, `FAILED_PRECONDITION`
- HTTP: 4xx (429 제외)

→ 데이터가 잘못된 경우. 재시도해도 실패함.

### 재시도 전략

지수 백오프 + jitter:

```
initial_interval = 5s
multiplier       = 1.5
max_interval     = 30s
max_elapsed_time = 5min      # 이 시간 넘으면 포기 (drop)
```

### Throttling 힌트

서버가 `Retry-After` 헤더(HTTP) 또는 `RetryInfo` (gRPC)로 재시도 대기 시간을 지정 가능. 클라이언트는 이를 따라야 함.

```
HTTP/1.1 429 Too Many Requests
Retry-After: 30
```

### 배압 (Backpressure)

큐가 가득 차면 SDK/Collector는 **새 데이터를 drop** 하거나 **계측 코드를 차단** 할 수 있음.

- SDK 기본: 큐 가득 차면 drop, 메트릭으로 카운트
- Collector `memory_limiter` processor: 메모리 임계 초과 시 receiver가 거부 → upstream에 압력 전달

OTLP는 **느린 백엔드가 빠른 client를 늦추지 않게** 비동기 큐를 두는 게 표준입니다.

---

## 보안

### TLS

프로덕션은 **TLS 필수**. SDK·Collector·백엔드 모두 mTLS 또는 TLS+Bearer 토큰을 지원.

```yaml
# Collector exporter
exporters:
  otlp:
    endpoint: tempo:4317
    tls:
      ca_file: /etc/certs/ca.crt
      cert_file: /etc/certs/client.crt
      key_file: /etc/certs/client.key
```

### 인증 헤더

Bearer 토큰, API key:

```yaml
exporters:
  otlphttp:
    endpoint: https://otlp.example.com:4318
    headers:
      Authorization: "Bearer ${env:OTLP_TOKEN}"
      X-API-Key: "${env:API_KEY}"
```

SaaS 백엔드(Datadog, New Relic, Honeycomb)는 보통 헤더 기반 인증을 사용.

### 데이터 정화

OTLP 자체는 정화를 하지 않음 — Collector processor 단계에서 처리:
- `attributes` processor로 PII 제거/마스킹
- `redaction` processor로 정규식 기반 마스킹
- `transform` processor의 OTTL로 복잡한 규칙

### 환경 변수 표준

대부분의 SDK가 다음 envvar를 지원합니다:

```
OTEL_EXPORTER_OTLP_ENDPOINT       # 기본 엔드포인트 (모든 시그널 공유)
OTEL_EXPORTER_OTLP_TRACES_ENDPOINT # trace만 다른 엔드포인트
OTEL_EXPORTER_OTLP_PROTOCOL       # grpc | http/protobuf | http/json
OTEL_EXPORTER_OTLP_HEADERS        # "k1=v1,k2=v2"
OTEL_EXPORTER_OTLP_TIMEOUT        # ms
OTEL_EXPORTER_OTLP_COMPRESSION    # gzip | none
OTEL_EXPORTER_OTLP_CERTIFICATE    # CA cert 경로
```

→ 코드 변경 없이 배포별 설정 변경.

---

## 성능 고려사항

### 압축

OTLP는 gzip(권장)·zstd·deflate 등을 지원. 텔레메트리 데이터는 attribute 이름·값에 반복이 많아 압축률이 매우 높음 (보통 5~10배).

```
OTEL_EXPORTER_OTLP_COMPRESSION=gzip
```

### Batching

SDK·Collector 모두 batch processor/processor가 핵심:
- Batch size: 보통 512~2048 record
- Timeout: 1~5초

작은 batch가 너무 많으면 RPC overhead, 큰 batch는 메모리·지연 증가.

### Keep-alive

gRPC는 HTTP/2 connection을 유지. Idle 시간이 길면 일부 LB가 끊을 수 있어 keep-alive 설정 필요:

```yaml
exporters:
  otlp:
    endpoint: backend:4317
    keepalive:
      time: 30s
      timeout: 10s
      permit_without_stream: true
```

### Cardinality

OTLP 자체는 카디널리티 제한이 없음 — **수신 측 백엔드가 비용** 부담. SDK·Collector에서 미리 제어해야 함:
- Metric View로 attribute 줄이기
- Resource attribute 일관성

---

## 디버깅

### Debug Exporter

Collector에서 받은 데이터를 stdout에 덤프:

```yaml
exporters:
  debug:
    verbosity: detailed     # basic | normal | detailed

service:
  pipelines:
    traces:
      receivers: [otlp]
      exporters: [debug]
```

### File Exporter

JSON 라인으로 파일에 기록 → 나중에 분석:

```yaml
exporters:
  file:
    path: /tmp/traces.jsonl
```

### curl로 OTLP/HTTP 보내기

JSON 모드는 사람이 읽고 쓸 수 있어 디버깅에 유용:

```
curl -i http://localhost:4318/v1/traces \
  -H 'Content-Type: application/json' \
  -d @- <<'EOF'
{
  "resourceSpans": [{
    "resource": { "attributes": [{ "key": "service.name", "value": { "stringValue": "test" } }] },
    "scopeSpans": [{
      "scope": { "name": "manual" },
      "spans": [{
        "traceId": "5b8aa5a2d2c873e8a6a9f5c7d2e0f1a3",
        "spanId": "00f067aa0ba902b7",
        "name": "test-span",
        "startTimeUnixNano": "1714000000000000000",
        "endTimeUnixNano": "1714000000100000000",
        "kind": 2
      }]
    }]
  }]
}
EOF
```

### zPages Extension

Collector에 `zpages` extension을 켜면 `http://localhost:55679/debug/tracez` 등에서 내부 상태 조회 가능:

```yaml
extensions:
  zpages:
    endpoint: 0.0.0.0:55679

service:
  extensions: [zpages]
```

### Self-telemetry

Collector 자체의 metric:

```yaml
service:
  telemetry:
    metrics:
      address: 0.0.0.0:8888

# 다른 Prometheus가 8888/metrics 를 scrape
```

주요 메트릭:
- `otelcol_receiver_accepted_spans` / `otelcol_receiver_refused_spans`
- `otelcol_processor_dropped_spans`
- `otelcol_exporter_sent_spans` / `otelcol_exporter_send_failed_spans`
- `otelcol_processor_queued_retry_send_size`

---

## 참고 자료

- OTLP Specification: https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/protocol/otlp.md
- Protobuf 정의: https://github.com/open-telemetry/opentelemetry-proto
- 환경 변수 사양: https://opentelemetry.io/docs/specs/otel/configuration/sdk-environment-variables/
- Collector OTLP Receiver: https://github.com/open-telemetry/opentelemetry-collector/tree/main/receiver/otlpreceiver
