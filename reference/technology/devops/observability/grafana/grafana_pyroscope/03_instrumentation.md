# 애플리케이션 계측

## 애플리케이션 계측 (Instrumentation)

> 원본: https://grafana.com/docs/pyroscope/latest/configure-client/

<a id="계측-방식-선택"></a>
### 계측 방식 선택

계측 방식은 애플리케이션을 얼마나 수정할 수 있는지, 라벨을 어디서 제어할지에 따라 선택한다. 핵심 서비스에서 작업별 라벨이 필요하다면 SDK를, 이미 pprof를 노출하는 Go 앱이라면 Pull 방식을 고려할 수 있다.

| 방식 | 장점 | 제약 | 적합한 대상 |
| --- | --- | --- | --- |
| 언어 SDK(Push) | 정밀 제어, 풍부한 라벨 | 코드 수정 필요 | 핵심 서비스 |
| pprof endpoint(Pull) | 적은 코드 변경 | 라벨 제어 제한 | 이미 pprof를 노출하는 Go 앱 |
| Alloy 자동 수집 | 중앙 집중 관리 | 파이프라인 구축 필요 | 다수 서비스 운영 |
| eBPF | 코드 계측 없이 모든 프로세스 수집 | 컨테이너와 커널 호환성 확인 필요 | 시스템 전체, 레거시 |

<a id="push-vs-pull"></a>

### Push vs Pull

#### Push 방식

Push 방식에서는 애플리케이션이 `/ingest` 또는 `/push.v1.PusherService/Push` 엔드포인트로 HTTP POST 요청을 보낸다. SDK 코드에서 라벨을 자유롭게 추가할 수 있으며, 서버리스나 ECS 같은 동적 환경에 유리하다.

#### Pull 방식

Pull 방식에서는 애플리케이션이 `/debug/pprof/profile` 같은 pprof 엔드포인트를 노출하고, Alloy/Agent가 주기적으로 스크레이핑해 Pyroscope로 전달한다. 서비스 디스커버리와 relabel을 사용하는 Prometheus의 운영 모델과 같아, 표준 pprof를 사용하는 Go 앱에 자연스럽게 적용할 수 있다.

<a id="go-계측"></a>

### Go 계측

#### 옵션 1: 표준 pprof endpoint (권장)

- Go 표준 라이브러리는 pprof를 기본 지원함

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

- 이후 Alloy가 해당 엔드포인트를 스크레이핑함

#### 옵션 2: Push SDK

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

#### 동적 라벨 (Tag wrapper)

- 특정 작업 단위에만 라벨을 붙임

```go
pyroscope.TagWrapper(ctx, pyroscope.Labels("endpoint", "/api/checkout"), func(ctx context.Context) {
    handleCheckout(ctx)
})
```

#### Mutex/Block 활성화

```go
runtime.SetMutexProfileFraction(5)
runtime.SetBlockProfileRate(5)
```

<a id="java-계측"></a>

### Java 계측

- [async-profiler](https://github.com/async-profiler/async-profiler) 기반

#### Maven

```xml
<dependency>
    <groupId>io.pyroscope</groupId>
    <artifactId>agent</artifactId>
    <version>...</version>
</dependency>
```

#### Java agent 방식 (코드 변경 없음)

```bash
java -javaagent:pyroscope.jar \
     -DPYROSCOPE_APPLICATION_NAME=checkout \
     -DPYROSCOPE_SERVER_ADDRESS=http://pyroscope:4040 \
     -DPYROSCOPE_PROFILER_EVENT=cpu \
     -jar app.jar
```

#### 환경 변수

- `PYROSCOPE_APPLICATION_NAME`: 서비스 이름
- `PYROSCOPE_SERVER_ADDRESS`: 서버 URL
- `PYROSCOPE_AUTH_TOKEN`: 인증 토큰(Grafana Cloud 등)
- `PYROSCOPE_PROFILER_EVENT`: `cpu`, `alloc`, `lock`, `wall`
- `PYROSCOPE_PROFILER_LOCK`: 락 컨텐션 임계값(예: `10ms`)
- `PYROSCOPE_LABELS`: `env=prod,region=us-east-1`

<a id="python-계측"></a>

### Python 계측

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

- py-spy 기반 외부 프로파일러도 사용 가능

```bash
py-spy record -o profile.pprof --pyroscope-server http://pyroscope:4040 -- python app.py
```

<a id="nodejs-계측"></a>

### Node.js 계측

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

- 지원 프로파일: CPU, Heap, Wall.

<a id="ruby-계측"></a>

### Ruby 계측

```ruby
require 'pyroscope'

Pyroscope.configure do |config|
  config.application_name = "checkout"
  config.server_address   = "http://pyroscope:4040"
  config.tags             = { env: "prod" }
end
```

- 내부적으로 [rbspy](https://rbspy.github.io/)와 유사한 외부 프로세스 방식이 사용됨

<a id="net-계측"></a>

### .NET 계측

- `dotnet diagnostics`와 통합한 패키지를 사용함

```csharp
using Pyroscope;

Profiler.Configure(new ProfilerOptions {
    ServerAddress = "http://pyroscope:4040",
    ApplicationName = "checkout",
    Tags = new Dictionary<string, string> { { "env", "prod" } },
});
```

- 지원 프로파일: CPU, Allocation, Lock, Exception.

<a id="rust-계측"></a>

### Rust 계측

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

<a id="grafana-alloy-기반-자동-계측"></a>

### Grafana Alloy 기반 자동 계측

- [Grafana Alloy](https://grafana.com/docs/alloy/)는 OpenTelemetry Collector, Prometheus Agent, Pyroscope agent를 통합한 에이전트임

#### 풀 모드 (Go pprof 스크레이핑)

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

#### 푸시 수신

- SDK가 전송하는 프로파일을 Alloy에서 수신해 변환, 포워딩 가능(`pyroscope.receive_http`).

#### 어노테이션 기반 자동 발견

```yaml
# pod annotations
metadata:
  annotations:
    profiles.grafana.com/cpu.scrape: "true"
    profiles.grafana.com/cpu.port: "6060"
    profiles.grafana.com/memory.scrape: "true"
```

- Alloy가 어노테이션을 읽어 자동으로 타겟을 등록함

<a id="ebpf-기반-무계측-프로파일링"></a>

### eBPF 기반 무계측 프로파일링

- eBPF를 활용해 코드 변경 없이 호스트의 모든 프로세스를 프로파일링함

#### 장점

- 라이브러리/언어를 가리지 않음 (시스템 콜 레벨)
- 레거시 애플리케이션, 바이너리 전용 환경에 적합
- 매우 낮은 오버헤드 (보통 1% 미만)

#### 한계

- 측정 가능한 것은 주로 process_cpu (CPU on-CPU 시간)
- 인터프리터 언어(Python/Ruby/Node)는 심볼화에 추가 작업 필요
- 커널 ≥ 4.9, 권한 (CAP_BPF, CAP_PERFMON) 필요

#### Alloy의 eBPF 컴포넌트

```alloy
pyroscope.ebpf "system" {
  forward_to = [pyroscope.write.default.receiver]
  targets    = discovery.process.all.targets
}
```

- Alloy가 호스트의 프로세스 목록을 탐색하고 eBPF로 스택을 캡처함

### 계측 모범 사례

- 1\. `service_name` 라벨 통일: Mimir/Loki/Tempo와 동일한 값 사용 → 신호 간 점프 가능
- 2\. 고카디널리티 라벨 주의: `request_id` 같은 고유 값은 절대 라벨로 쓰지 말 것
- 3\. 환경 라벨 추가: `env`, `cluster`, `region`, `version` 정도가 적당
- 4\. 샘플링 레이트: CPU는 100Hz가 표준
  - 낮추면 작은 함수 누락
- 5\. 프로파일 주기: 10~60초 단위 권장
  - 너무 짧으면 노이즈, 너무 길면 회귀 검출 지연
- 6\. Mutex/Block은 선택적으로: 활성화하면 약간의 추가 비용
