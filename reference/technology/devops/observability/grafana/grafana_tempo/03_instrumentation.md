# Tempo 애플리케이션 계측

## Tempo 애플리케이션 계측

> 원본: https://grafana.com/docs/tempo/latest/setup/instrumentation/

<a id="개요"></a>
### 개요

애플리케이션에서 트레이스를 생성해 Tempo로 보내려면 계측(instrumentation)이 필요하다. 라이브러리 호출을 자동으로 추적하거나, 코드에서 직접 스팬을 만들어 필요한 작업을 기록할 수 있다.

#### 권장 라이브러리

- OpenTelemetry SDK (강력 권장): 모든 언어/프레임워크 지원, 벤더 중립
- Jaeger Client (Deprecated)
- Zipkin Client (Deprecated)

#### 계측 방식

자동 계측(Auto)은 에이전트가 라이브러리를 후킹하므로 코드 변경 없이 적용할 수 있지만, 추적할 수 있는 깊이에 제한이 있다. 수동 계측(Manual)은 코드에서 직접 스팬을 만들어 더 정밀하게 제어하는 대신 코드를 수정해야 한다. 이 문서에서는 자동 계측을 기본으로 적용하고 필요한 부분만 수동으로 보완하는 하이브리드 방식을 권장한다.

<a id="opentelemetry-sdk-권장"></a>

### OpenTelemetry SDK (권장)

#### OTel 핵심 요소

- TracerProvider: 트레이서 생성 팩토리
- Tracer: 스팬을 만드는 객체
- SpanProcessor: 생성된 스팬을 처리/내보내기 (Batch, Simple)
- Exporter: 백엔드(Tempo)로 전송 (OTLP, Jaeger, Zipkin)
- Sampler: 어떤 트레이스를 샘플링할지 결정

#### 데이터 흐름

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

<a id="언어별-계측"></a>

### 언어별 계측

#### Java

다음 예제는 `my-service`의 `processOrder` 작업을 스팬으로 기록하고, OTLP exporter로 Alloy에 전송한다. 스팬에 주문 ID를 속성으로 추가하고, 작업이 끝나면 `finally`에서 스팬도 종료한다.

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

#### Java 자동 계측 (Java Agent)

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

#### Go

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

#### Python

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

- Python 자동 계측

  ```bash
  opentelemetry-bootstrap --action=install
  opentelemetry-instrument \
    --service_name my-service \
    --exporter_otlp_endpoint http://alloy:4317 \
    --traces_exporter otlp \
    python my_app.py
  ```

#### Node.js

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

#### .NET

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

#### Ruby

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

<a id="수동-계측-manual"></a>

### 수동 계측 (Manual)

자동 계측만으로는 애플리케이션 고유의 비즈니스 로직을 충분히 구분하기 어려울 수 있다. 이런 작업에는 직접 스팬을 만들고 속성과 이벤트를 추가해, 실행 중 어떤 일이 있었는지 기록한다.

#### 스팬 속성 추가

```python
with tracer.start_as_current_span("calculatePrice") as span:
    span.set_attribute("user.id", user_id)
    span.set_attribute("cart.size", len(cart))
    span.set_attribute("currency", "USD")
    
    # 계산
    price = calculate(cart)
    span.set_attribute("price.total", price)
```

#### 이벤트 추가

속성이 작업의 정보를 담는다면 이벤트는 작업 중 발생한 일을 기록한다. 다음 예제에서는 결제 시작과 완료 시점을 이벤트로 남긴다.

```python
with tracer.start_as_current_span("processPayment") as span:
    span.add_event("payment_initiated")
    
    result = payment_gateway.process(...)
    
    span.add_event("payment_completed", attributes={
        "payment.id": result.id,
        "payment.method": result.method,
    })
```

#### 에러 기록

```python
with tracer.start_as_current_span("riskOperation") as span:
    try:
        risky()
    except Exception as e:
        span.record_exception(e)
        span.set_status(Status(StatusCode.ERROR, str(e)))
        raise
```

<a id="자동-계측-auto"></a>

### 자동 계측 (Auto)

- 각 언어별 SDK는 주요 라이브러리와 프레임워크를 자동으로 계측함

#### 지원 라이브러리 예 (언어별)

- Java: Spring, Servlet, JDBC, Kafka, gRPC, AWS SDK 등
- Go: net/http, gRPC, Gin, AWS SDK 등 (수동 추가 형태)
- Python: Flask, Django, FastAPI, requests, SQLAlchemy 등
- Node.js: Express, Fastify, Koa, http, MongoDB, MySQL 등
- .NET: ASP.NET Core, HttpClient, SqlClient 등
- Ruby: Rails, Sinatra, Net::HTTP, ActiveRecord 등

<a id="컨텍스트-전파"></a>

### 컨텍스트 전파

#### W3C Trace Context (기본 권장)

서비스 사이에서 같은 요청을 추적하려면 다음 호출에도 트레이스 정보를 전달해야 한다. W3C Trace Context는 다음 HTTP 헤더로 이 정보를 전파한다.

```
traceparent: 00-<trace-id>-<span-id>-<trace-flags>
tracestate: <vendor-specific>
```

#### Baggage

- 요청 전체에 걸쳐 키-값 메타데이터를 전파함

```python
from opentelemetry import baggage

with tracer.start_as_current_span("op"):
    ctx = baggage.set_baggage("user.id", "u-123")
    # 다운스트림 호출 시 baggage가 함께 전파됨
```

#### B3 Propagation (Zipkin 호환)

```python
from opentelemetry.propagators.b3 import B3MultiFormat
from opentelemetry import propagate

propagate.set_global_textmap(B3MultiFormat())
```

<a id="샘플링"></a>

### 샘플링

모든 트레이스를 수집해 전송하면 비용과 성능에 부담이 생긴다. 샘플링은 그중 일부만 선택하는 방식이며, 트레이스가 시작될 때 결정할지 수집한 뒤 결정할지에 따라 적용 방법이 달라진다.

#### Head-based Sampling (시작 시 결정)

```python
from opentelemetry.sdk.trace.sampling import TraceIdRatioBased

provider = TracerProvider(
    sampler=TraceIdRatioBased(0.1),  # 10% 샘플링
    resource=resource,
)
```

#### ParentBased Sampling

- 부모 스팬의 샘플링 결정을 따른다

```python
from opentelemetry.sdk.trace.sampling import ParentBased, TraceIdRatioBased

provider = TracerProvider(
    sampler=ParentBased(root=TraceIdRatioBased(0.1)),
)
```

#### Tail-based Sampling (Collector에서)

- OTel Collector나 Alloy에서 트레이스가 완전히 수집된 후 샘플링 여부를 결정함 (느린 트레이스, 에러 트레이스만 보존하는 방식 등).

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

<a id="수집기-collector-구성"></a>

### 수집기 (Collector) 구성

아래 구성은 OTLP로 받은 스팬을 배치로 묶어 Tempo에 전달한다. Grafana Alloy와 OpenTelemetry Collector 예제 모두 수신기, 배치 처리기, exporter를 연결해 전송 경로를 만든다.

#### Grafana Alloy

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

#### OpenTelemetry Collector

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

#### 트레이스 보강 (Enrichment)

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
