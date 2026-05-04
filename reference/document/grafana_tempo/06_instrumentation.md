# Tempo 애플리케이션 계측

> 이 문서는 Grafana Tempo 공식 문서의 "Set up tracing" 섹션 중 계측 부분을 한국어로 정리한 것입니다.
> 원본: https://grafana.com/docs/tempo/latest/setup/instrumentation/

---

## 목차

1. [개요](#개요)
2. [OpenTelemetry SDK (권장)](#opentelemetry-sdk-권장)
3. [언어별 계측](#언어별-계측)
4. [수동 계측 (Manual)](#수동-계측-manual)
5. [자동 계측 (Auto)](#자동-계측-auto)
6. [컨텍스트 전파](#컨텍스트-전파)
7. [샘플링](#샘플링)
8. [수집기 (Collector) 구성](#수집기-collector-구성)

---

## 개요

애플리케이션이 Tempo로 트레이스를 보내려면 **계측(instrumentation)** 이 필요합니다.

### 권장 라이브러리

- **OpenTelemetry SDK** (강력 권장): 모든 언어/프레임워크 지원, 벤더 중립
- Jaeger Client (Deprecated)
- Zipkin Client (Deprecated)

### 계측 방식

| 방식 | 설명 | 장단점 |
|------|------|--------|
| **자동(Auto)** | 에이전트가 자동으로 라이브러리 후킹 | 코드 변경 없음, 깊이 제한 |
| **수동(Manual)** | 코드에서 직접 스팬 생성 | 정밀 제어, 코드 수정 필요 |
| **하이브리드** | 자동 + 필요한 부분 수동 | 권장 |

---

## OpenTelemetry SDK (권장)

### OTel 핵심 요소

- **TracerProvider**: 트레이서 생성 팩토리
- **Tracer**: 스팬을 만드는 객체
- **SpanProcessor**: 생성된 스팬을 처리/내보내기 (Batch, Simple)
- **Exporter**: 백엔드(Tempo)로 전송 (OTLP, Jaeger, Zipkin)
- **Sampler**: 어떤 트레이스를 샘플링할지 결정

### 데이터 흐름

```
[Application Code]
     |
     v (Tracer.startSpan)
[TracerProvider]
     |
     v
[SpanProcessor (Batch)]
     |
     v
[Exporter (OTLP)]
     |
     v
[Alloy / OTel Collector]
     |
     v
[Tempo]
```

---

## 언어별 계측

### Java

```xml
<!-- pom.xml -->
<dependency>
  <groupId>io.opentelemetry</groupId>
  <artifactId>opentelemetry-api</artifactId>
  <version>1.30.0</version>
</dependency>
<dependency>
  <groupId>io.opentelemetry</groupId>
  <artifactId>opentelemetry-sdk</artifactId>
  <version>1.30.0</version>
</dependency>
<dependency>
  <groupId>io.opentelemetry</groupId>
  <artifactId>opentelemetry-exporter-otlp</artifactId>
  <version>1.30.0</version>
</dependency>
```

```java
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;

OpenTelemetry openTelemetry = OpenTelemetrySdk.builder()
    .setTracerProvider(SdkTracerProvider.builder()
        .addSpanProcessor(BatchSpanProcessor.builder(
            OtlpGrpcSpanExporter.builder()
                .setEndpoint("http://alloy:4317")
                .build()
        ).build())
        .setResource(Resource.getDefault().merge(
            Resource.create(Attributes.of(
                AttributeKey.stringKey("service.name"), "my-service"
            ))
        ))
        .build())
    .build();

Tracer tracer = openTelemetry.getTracer("my-service");

Span span = tracer.spanBuilder("processOrder").startSpan();
try (Scope scope = span.makeCurrent()) {
    span.setAttribute("order.id", "12345");
    // 비즈니스 로직
} finally {
    span.end();
}
```

### Java 자동 계측 (Java Agent)

```bash
# Agent 다운로드
curl -L -O https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar

# 실행
java -javaagent:./opentelemetry-javaagent.jar \
     -Dotel.service.name=my-service \
     -Dotel.exporter.otlp.endpoint=http://alloy:4317 \
     -Dotel.traces.exporter=otlp \
     -jar my-app.jar
```

### Go

```go
package main

import (
    "context"
    "go.opentelemetry.io/otel"
    "go.opentelemetry.io/otel/exporters/otlp/otlptrace/otlptracegrpc"
    "go.opentelemetry.io/otel/sdk/resource"
    sdktrace "go.opentelemetry.io/otel/sdk/trace"
    semconv "go.opentelemetry.io/otel/semconv/v1.21.0"
)

func initTracer() (*sdktrace.TracerProvider, error) {
    ctx := context.Background()

    exporter, err := otlptracegrpc.New(ctx,
        otlptracegrpc.WithEndpoint("alloy:4317"),
        otlptracegrpc.WithInsecure(),
    )
    if err != nil {
        return nil, err
    }

    res, _ := resource.New(ctx,
        resource.WithAttributes(
            semconv.ServiceName("my-service"),
        ),
    )

    tp := sdktrace.NewTracerProvider(
        sdktrace.WithBatcher(exporter),
        sdktrace.WithResource(res),
    )
    otel.SetTracerProvider(tp)
    return tp, nil
}

func processOrder(ctx context.Context, orderID string) {
    tracer := otel.Tracer("my-service")
    ctx, span := tracer.Start(ctx, "processOrder")
    defer span.End()

    span.SetAttributes(attribute.String("order.id", orderID))
    // 비즈니스 로직
}
```

### Python

```bash
pip install opentelemetry-api opentelemetry-sdk \
            opentelemetry-exporter-otlp \
            opentelemetry-instrumentation-flask
```

```python
from opentelemetry import trace
from opentelemetry.sdk.trace import TracerProvider
from opentelemetry.sdk.trace.export import BatchSpanProcessor
from opentelemetry.exporter.otlp.proto.grpc.trace_exporter import OTLPSpanExporter
from opentelemetry.sdk.resources import Resource
from opentelemetry.semconv.resource import ResourceAttributes

resource = Resource(attributes={
    ResourceAttributes.SERVICE_NAME: "my-service"
})

provider = TracerProvider(resource=resource)
processor = BatchSpanProcessor(
    OTLPSpanExporter(endpoint="http://alloy:4317", insecure=True)
)
provider.add_span_processor(processor)
trace.set_tracer_provider(provider)

tracer = trace.get_tracer("my-service")

with tracer.start_as_current_span("processOrder") as span:
    span.set_attribute("order.id", "12345")
    # 비즈니스 로직
```

#### Python 자동 계측

```bash
opentelemetry-bootstrap --action=install
opentelemetry-instrument \
  --service_name my-service \
  --exporter_otlp_endpoint http://alloy:4317 \
  --traces_exporter otlp \
  python my_app.py
```

### Node.js

```bash
npm install @opentelemetry/api @opentelemetry/sdk-node \
            @opentelemetry/auto-instrumentations-node \
            @opentelemetry/exporter-trace-otlp-grpc
```

```javascript
const { NodeSDK } = require('@opentelemetry/sdk-node');
const { OTLPTraceExporter } = require('@opentelemetry/exporter-trace-otlp-grpc');
const { getNodeAutoInstrumentations } = require('@opentelemetry/auto-instrumentations-node');
const { Resource } = require('@opentelemetry/resources');
const { SemanticResourceAttributes } = require('@opentelemetry/semantic-conventions');

const sdk = new NodeSDK({
  resource: new Resource({
    [SemanticResourceAttributes.SERVICE_NAME]: 'my-service',
  }),
  traceExporter: new OTLPTraceExporter({
    url: 'http://alloy:4317',
  }),
  instrumentations: [getNodeAutoInstrumentations()],
});

sdk.start();
```

### .NET

```bash
dotnet add package OpenTelemetry
dotnet add package OpenTelemetry.Extensions.Hosting
dotnet add package OpenTelemetry.Exporter.OpenTelemetryProtocol
dotnet add package OpenTelemetry.Instrumentation.AspNetCore
```

```csharp
using OpenTelemetry.Trace;
using OpenTelemetry.Resources;

builder.Services.AddOpenTelemetry()
    .ConfigureResource(r => r.AddService("my-service"))
    .WithTracing(t => t
        .AddAspNetCoreInstrumentation()
        .AddHttpClientInstrumentation()
        .AddOtlpExporter(o => {
            o.Endpoint = new Uri("http://alloy:4317");
        }));
```

### Ruby

```ruby
# Gemfile
gem 'opentelemetry-sdk'
gem 'opentelemetry-exporter-otlp'
gem 'opentelemetry-instrumentation-all'

# config
require 'opentelemetry/sdk'
require 'opentelemetry/instrumentation/all'

OpenTelemetry::SDK.configure do |c|
  c.service_name = 'my-service'
  c.use_all
end
```

---

## 수동 계측 (Manual)

자동 계측이 다루지 않는 비즈니스 로직을 직접 추적.

### 스팬 속성 추가

```python
with tracer.start_as_current_span("calculatePrice") as span:
    span.set_attribute("user.id", user_id)
    span.set_attribute("cart.size", len(cart))
    span.set_attribute("currency", "USD")
    
    # 계산
    price = calculate(cart)
    span.set_attribute("price.total", price)
```

### 이벤트 추가

```python
with tracer.start_as_current_span("processPayment") as span:
    span.add_event("payment_initiated")
    
    result = payment_gateway.process(...)
    
    span.add_event("payment_completed", attributes={
        "payment.id": result.id,
        "payment.method": result.method,
    })
```

### 에러 기록

```python
with tracer.start_as_current_span("riskOperation") as span:
    try:
        risky()
    except Exception as e:
        span.record_exception(e)
        span.set_status(Status(StatusCode.ERROR, str(e)))
        raise
```

---

## 자동 계측 (Auto)

각 언어별 SDK는 인기 있는 라이브러리/프레임워크를 자동 계측합니다.

### 지원 라이브러리 예 (언어별)

| 언어 | 자동 계측 라이브러리 |
|------|---------------------|
| Java | Spring, Servlet, JDBC, Kafka, gRPC, AWS SDK 등 |
| Go | net/http, gRPC, Gin, AWS SDK 등 (수동 추가 형태) |
| Python | Flask, Django, FastAPI, requests, SQLAlchemy 등 |
| Node.js | Express, Fastify, Koa, http, MongoDB, MySQL 등 |
| .NET | ASP.NET Core, HttpClient, SqlClient 등 |
| Ruby | Rails, Sinatra, Net::HTTP, ActiveRecord 등 |

---

## 컨텍스트 전파

### W3C Trace Context (기본 권장)

HTTP 헤더로 트레이스 정보 전파:

```
traceparent: 00-<trace-id>-<span-id>-<trace-flags>
tracestate: <vendor-specific>
```

### Baggage

요청 전반에 걸쳐 키-값 메타데이터 전파:

```python
from opentelemetry import baggage

with tracer.start_as_current_span("op"):
    ctx = baggage.set_baggage("user.id", "u-123")
    # 다운스트림 호출 시 baggage가 함께 전파됨
```

### B3 Propagation (Zipkin 호환)

```python
from opentelemetry.propagators.b3 import B3MultiFormat
from opentelemetry import propagate

propagate.set_global_textmap(B3MultiFormat())
```

---

## 샘플링

모든 트레이스를 보내면 비용/성능 부담. 샘플링으로 일부만 수집.

### Head-based Sampling (시작 시 결정)

```python
from opentelemetry.sdk.trace.sampling import TraceIdRatioBased

provider = TracerProvider(
    sampler=TraceIdRatioBased(0.1),  # 10% 샘플링
    resource=resource,
)
```

### ParentBased Sampling

부모 결정 따름.

```python
from opentelemetry.sdk.trace.sampling import ParentBased, TraceIdRatioBased

provider = TracerProvider(
    sampler=ParentBased(root=TraceIdRatioBased(0.1)),
)
```

### Tail-based Sampling (Collector에서)

OTel Collector나 Alloy에서 트레이스 종료 후 결정 (느린 트레이스, 에러 트레이스만 보존 등).

```alloy
otelcol.processor.tail_sampling "default" {
  policy {
    name = "errors"
    type = "status_code"
    status_code {
      status_codes = ["ERROR"]
    }
  }
  
  policy {
    name = "slow"
    type = "latency"
    latency {
      threshold_ms = 1000
    }
  }
  
  policy {
    name = "probabilistic"
    type = "probabilistic"
    probabilistic {
      sampling_percentage = 10
    }
  }
  
  output {
    traces = [otelcol.exporter.otlp.tempo.input]
  }
}
```

---

## 수집기 (Collector) 구성

### Grafana Alloy

```alloy
otelcol.receiver.otlp "default" {
  grpc {
    endpoint = "0.0.0.0:4317"
  }
  http {
    endpoint = "0.0.0.0:4318"
  }
  
  output {
    traces = [otelcol.processor.batch.default.input]
  }
}

otelcol.processor.batch "default" {
  output {
    traces = [otelcol.exporter.otlp.tempo.input]
  }
}

otelcol.exporter.otlp "tempo" {
  client {
    endpoint = "tempo:4317"
    tls {
      insecure = true
    }
  }
}
```

### OpenTelemetry Collector

```yaml
receivers:
  otlp:
    protocols:
      grpc:
        endpoint: 0.0.0.0:4317
      http:
        endpoint: 0.0.0.0:4318

processors:
  batch:
    timeout: 5s
    send_batch_size: 1000

exporters:
  otlp/tempo:
    endpoint: tempo:4317
    tls:
      insecure: true
    headers:
      X-Scope-OrgID: tenant-1

service:
  pipelines:
    traces:
      receivers: [otlp]
      processors: [batch]
      exporters: [otlp/tempo]
```

### 트레이스 보강 (Enrichment)

```yaml
processors:
  resource:
    attributes:
      - key: cluster
        value: us-east-1
        action: upsert
      - key: env
        value: production
        action: upsert
  
  attributes:
    actions:
      - key: http.url
        action: hash    # PII 보호
```
