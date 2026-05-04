# 애플리케이션 계측 (Instrumentation)

> 이 문서는 Pyroscope에 프로파일을 보내는 다양한 방법을 다룹니다: 언어 SDK, Grafana Alloy, eBPF.
> 원본: https://grafana.com/docs/pyroscope/latest/configure-client/

---

## 목차

1. [계측 방식 선택](#계측-방식-선택)
2. [Push vs Pull](#push-vs-pull)
3. [Go 계측](#go-계측)
4. [Java 계측](#java-계측)
5. [Python 계측](#python-계측)
6. [Node.js 계측](#nodejs-계측)
7. [Ruby 계측](#ruby-계측)
8. [.NET 계측](#net-계측)
9. [Rust 계측](#rust-계측)
10. [Grafana Alloy 기반 자동 계측](#grafana-alloy-기반-자동-계측)
11. [eBPF 기반 무계측 프로파일링](#ebpf-기반-무계측-프로파일링)

---

## 계측 방식 선택

| 방식 | 장점 | 단점 | 권장 시나리오 |
|------|------|------|----------------|
| 언어 SDK (Push) | 정밀 제어, 라벨 풍부 | 코드 수정 필요 | 핵심 서비스 |
| pprof endpoint (Pull) | 코드 변경 적음 | 라벨 제어 제한 | 이미 pprof 노출하는 Go 앱 |
| Alloy 자동 수집 | 중앙 집중 관리 | 파이프라인 구축 필요 | 다수 서비스 운영 |
| eBPF | 무계측, 모든 프로세스 | 컨테이너/커널 호환성 주의 | 시스템 전체, 레거시 |

---

## Push vs Pull

### Push 방식

- 애플리케이션이 직접 Pyroscope 서버로 HTTP POST
- 엔드포인트: `/ingest` 또는 `/push.v1.PusherService/Push`
- 라벨을 SDK 코드에서 자유롭게 추가
- 동적 환경(서버리스, ECS)에 유리

### Pull 방식

- 애플리케이션은 pprof 엔드포인트 노출(예: `/debug/pprof/profile`)
- Alloy/Agent가 주기적으로 스크레이핑 → Pyroscope로 전달
- Prometheus와 같은 운영 모델 (서비스 디스커버리, relabel)
- 표준 pprof 사용하는 Go 앱에 자연스러움

---

## Go 계측

### 옵션 1: 표준 pprof endpoint (권장)

이미 Go가 표준 라이브러리로 pprof를 지원합니다.

```go
import (
    "net/http"
    _ "net/http/pprof"
)

func main() {
    go func() {
        http.ListenAndServe(":6060", nil)
    }()
    // ...
}
```

- `http://host:6060/debug/pprof/profile` (CPU)
- `http://host:6060/debug/pprof/heap`
- `http://host:6060/debug/pprof/goroutine`
- `http://host:6060/debug/pprof/mutex`
- `http://host:6060/debug/pprof/block`

이후 Alloy가 스크레이핑하면 됩니다.

### 옵션 2: Push SDK

```go
import "github.com/grafana/pyroscope-go"

func main() {
    profiler, _ := pyroscope.Start(pyroscope.Config{
        ApplicationName: "checkout-service",
        ServerAddress:   "http://pyroscope:4040",
        Tags:            map[string]string{"env": "prod"},
        ProfileTypes: []pyroscope.ProfileType{
            pyroscope.ProfileCPU,
            pyroscope.ProfileAllocObjects,
            pyroscope.ProfileAllocSpace,
            pyroscope.ProfileInuseObjects,
            pyroscope.ProfileInuseSpace,
        },
    })
    defer profiler.Stop()
    // ...
}
```

### 동적 라벨 (Tag wrapper)

특정 작업 단위에만 라벨을 붙일 수 있습니다.

```go
pyroscope.TagWrapper(ctx, pyroscope.Labels("endpoint", "/api/checkout"), func(ctx context.Context) {
    handleCheckout(ctx)
})
```

### Mutex/Block 활성화

```go
runtime.SetMutexProfileFraction(5)
runtime.SetBlockProfileRate(5)
```

---

## Java 계측

[async-profiler](https://github.com/async-profiler/async-profiler) 기반.

### Maven

```xml
<dependency>
    <groupId>io.pyroscope</groupId>
    <artifactId>agent</artifactId>
    <version>...</version>
</dependency>
```

### Java agent 방식 (코드 변경 없음)

```bash
java -javaagent:pyroscope.jar \
     -DPYROSCOPE_APPLICATION_NAME=checkout \
     -DPYROSCOPE_SERVER_ADDRESS=http://pyroscope:4040 \
     -DPYROSCOPE_PROFILER_EVENT=cpu \
     -jar app.jar
```

### 환경 변수

| 변수 | 설명 |
|------|------|
| `PYROSCOPE_APPLICATION_NAME` | 서비스 이름 |
| `PYROSCOPE_SERVER_ADDRESS` | 서버 URL |
| `PYROSCOPE_AUTH_TOKEN` | 인증 토큰 (Grafana Cloud 등) |
| `PYROSCOPE_PROFILER_EVENT` | `cpu`, `alloc`, `lock`, `wall` |
| `PYROSCOPE_PROFILER_LOCK` | 락 컨텐션 임계값(예: `10ms`) |
| `PYROSCOPE_LABELS` | `env=prod,region=us-east-1` |

---

## Python 계측

```bash
pip install pyroscope-io
```

```python
import pyroscope

pyroscope.configure(
    application_name="checkout",
    server_address="http://pyroscope:4040",
    tags={"env": "prod"},
)

with pyroscope.tag_wrapper({"endpoint": "/api/checkout"}):
    handle_checkout()
```

py-spy 기반 외부 프로파일러로도 가능합니다.

```bash
py-spy record -o profile.pprof --pyroscope-server http://pyroscope:4040 -- python app.py
```

---

## Node.js 계측

```bash
npm install @pyroscope/nodejs
```

```js
import Pyroscope from '@pyroscope/nodejs';

Pyroscope.init({
  serverAddress: 'http://pyroscope:4040',
  appName: 'checkout',
  tags: { env: 'prod' },
});
Pyroscope.start();
```

지원 프로파일: CPU, Heap, Wall.

---

## Ruby 계측

```ruby
require 'pyroscope'

Pyroscope.configure do |config|
  config.application_name = "checkout"
  config.server_address   = "http://pyroscope:4040"
  config.tags             = { env: "prod" }
end
```

내부적으로 [rbspy](https://rbspy.github.io/) 와 유사한 외부 프로세스 방식이 사용될 수 있습니다.

---

## .NET 계측

`dotnet diagnostics` 와 통합한 패키지를 사용합니다.

```csharp
using Pyroscope;

Profiler.Configure(new ProfilerOptions {
    ServerAddress = "http://pyroscope:4040",
    ApplicationName = "checkout",
    Tags = new Dictionary<string, string> { { "env", "prod" } },
});
```

지원 프로파일: CPU, Allocation, Lock, Exception.

---

## Rust 계측

```toml
[dependencies]
pyroscope = "..."
pyroscope_pprofrs = "..."
```

```rust
use pyroscope::PyroscopeAgent;
use pyroscope_pprofrs::{pprof_backend, PprofConfig};

let agent = PyroscopeAgent::builder("http://pyroscope:4040", "checkout")
    .backend(pprof_backend(PprofConfig::new().sample_rate(100)))
    .build()?;
let agent_running = agent.start()?;
```

---

## Grafana Alloy 기반 자동 계측

[Grafana Alloy](https://grafana.com/docs/alloy/) 는 OpenTelemetry Collector + Prometheus Agent + Pyroscope agent의 통합 에이전트입니다.

### 풀 모드 (Go pprof 스크레이핑)

```alloy
discovery.kubernetes "pods" {
  role = "pod"
}

pyroscope.scrape "default" {
  targets = discovery.kubernetes.pods.targets
  forward_to = [pyroscope.write.default.receiver]

  profiling_config {
    profile.process_cpu { enabled = true }
    profile.memory     { enabled = true }
    profile.goroutine  { enabled = true }
    profile.mutex      { enabled = true }
    profile.block      { enabled = true }
  }
}

pyroscope.write "default" {
  endpoint {
    url = "http://pyroscope:4040"
  }
}
```

### 푸시 수신

SDK가 보내는 것을 Alloy로 수신하여 변환/포워딩할 수도 있습니다(`pyroscope.receive_http`).

### 어노테이션 기반 자동 발견

```yaml
# pod annotations
metadata:
  annotations:
    profiles.grafana.com/cpu.scrape: "true"
    profiles.grafana.com/cpu.port: "6060"
    profiles.grafana.com/memory.scrape: "true"
```

Alloy가 어노테이션을 보고 자동으로 타겟을 등록합니다.

---

## eBPF 기반 무계측 프로파일링

eBPF를 활용하여 **코드 변경 없이** 호스트의 모든 프로세스를 프로파일링합니다.

### 장점

- 라이브러리/언어를 가리지 않음 (시스템 콜 레벨)
- 레거시 애플리케이션, 바이너리 전용 환경에 유효
- 매우 낮은 오버헤드 (보통 1% 미만)

### 한계

- 측정 가능한 것은 주로 **process_cpu** (CPU on-CPU 시간)
- 인터프리터 언어(Python/Ruby/Node)는 심볼화에 추가 작업 필요
- 커널 ≥ 4.9, 권한 (CAP_BPF, CAP_PERFMON) 필요

### Alloy의 eBPF 컴포넌트

```alloy
pyroscope.ebpf "system" {
  forward_to = [pyroscope.write.default.receiver]
  targets    = discovery.process.all.targets
}
```

Alloy가 호스트의 프로세스 목록을 발견하고 eBPF로 스택을 캡처합니다.

---

## 계측 모범 사례

1. **`service_name` 라벨 통일**: Mimir/Loki/Tempo와 동일한 값 사용 → 신호 간 점프 가능
2. **고카디널리티 라벨 주의**: `request_id` 같은 고유 값은 절대 라벨로 쓰지 말 것
3. **환경 라벨 추가**: `env`, `cluster`, `region`, `version` 정도가 적당
4. **샘플링 레이트**: CPU는 100Hz가 표준. 낮추면 작은 함수 누락
5. **프로파일 주기**: 10~60초 단위 권장. 너무 짧으면 노이즈, 너무 길면 회귀 검출 지연
6. **Mutex/Block은 선택적으로**: 활성화하면 약간의 추가 비용

---

## 다음 단계

- [09_configuration.md](./09_configuration.md) - 서버 설정
- [12_visualization.md](./12_visualization.md) - Grafana 통합
- [08_use_cases.md](./08_use_cases.md) - 실전 활용
