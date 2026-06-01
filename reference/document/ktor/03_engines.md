# 03. 엔진 (Engines)

> 출처: https://ktor.io/docs/server-engines.html
> 내용을 정리한 한국어 학습 노트입니다.

---

## 엔진이란

Ktor의 **엔진(Engine)** 은 실제로 TCP 소켓을 열고 HTTP 요청을 받아 Ktor 파이프라인에 넘기는 컴포넌트입니다. Ktor는 엔진을 추상화해 두었기 때문에, 같은 애플리케이션 코드를 Netty 위에서도 Jetty 위에서도 돌릴 수 있습니다.

---

## 지원 엔진 비교

| 엔진 | 플랫폼 | HTTP/2 | 비고 |
| --- | --- | --- | --- |
| **Netty** | JVM | ✓ | 고성능 비동기 NIO. 기본 선택지. |
| **Jetty** | JVM | ✓ | Eclipse 기반. 서블릿 컨테이너 친화. |
| **Tomcat** | JVM | ✓ | Apache 표준 컨테이너. |
| **CIO** | JVM / Native / GraalVM / JS / WasmJs | ✗ | 코루틴 기반, 멀티플랫폼 가능. |
| **ServletApplicationEngine** | JVM | ✓ | WAR로 외부 컨테이너에 배포할 때 사용. |

> HTTP/2가 필요한 경우 Netty / Jetty / Tomcat 중 선택. Native 빌드가 필요하면 CIO.

---

## 두 가지 서버 시작 방식

### embeddedServer (코드 중심)

엔진과 포트, 모듈을 코드 인자로 직접 지정합니다. 빠른 프로토타이핑·테스트에 좋습니다.

```kotlin
fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        routing { get("/") { call.respondText("Hello") } }
    }.start(wait = true)
}
```

### EngineMain (설정 파일 중심)

엔진 모듈이 제공하는 `EngineMain.main(args)`를 사용. 포트·모듈은 `application.conf` / `application.yaml`에서 읽습니다. 운영 배포에 적합.

```kotlin
fun main(args: Array<String>) =
    io.ktor.server.netty.EngineMain.main(args)

fun Application.module() {
    routing { get("/") { call.respondText("Hi") } }
}
```

```yaml
ktor:
  deployment:
    port: 8080
  application:
    modules:
      - com.example.ApplicationKt.module
```

`EngineMain.createServer(args)`를 쓰면 서버 인스턴스만 만들고 `start()` 시점을 직접 제어할 수도 있습니다.

---

## 공통 엔진 설정 옵션

`embeddedServer(... , configure = { ... })` 블록에서 또는 설정 파일 `ktor.deployment.*` 키로 지정합니다.

| 옵션 | 의미 |
| --- | --- |
| `connectors` | 호스트/포트/SSL 등 수신 커넥터 목록 |
| `connectionGroupSize` | 새 연결을 수락하는 스레드 수 |
| `workerGroupSize` | 연결을 처리하는 이벤트 루프 그룹 크기 |
| `callGroupSize` | 요청 핸들러를 실행하는 스레드 풀의 최소 크기 |
| `shutdownGracePeriod` | 그레이스풀 셧다운 시 새 요청 거부 후 대기 시간(ms) |
| `shutdownTimeout` | 셧다운 전체 최대 대기 시간(ms) |

---

## 엔진별 특화 옵션 (대표)

### Netty

| 옵션 | 의미 |
| --- | --- |
| `requestQueueLimit` | 큐잉되는 요청 수 상한 |
| `shareWorkGroup` | 워커 그룹 공유 여부 |
| `responseWriteTimeoutSeconds` | 응답 쓰기 타임아웃 |
| `requestReadTimeoutSeconds` | 요청 본문 읽기 타임아웃 |
| `tcpKeepAlive` | TCP keep-alive |

### Jetty

| 옵션 | 의미 |
| --- | --- |
| `configureServer` | 내부 `org.eclipse.jetty.server.Server`에 직접 접근 |
| `idleTimeout` | 유휴 연결 종료 시간 |

### CIO

| 옵션 | 의미 |
| --- | --- |
| `connectionIdleTimeoutSeconds` | 연결 유휴 타임아웃 |

### Tomcat

| 옵션 | 의미 |
| --- | --- |
| `configureTomcat` | 내부 Tomcat 인스턴스 핸들 |

---

## 상세 설정 예시

```kotlin
embeddedServer(Netty, configure = {
    connectors.add(EngineConnectorBuilder().apply {
        host = "127.0.0.1"
        port = 8080
    })
    workerGroupSize = 5
    callGroupSize = 10
    shutdownGracePeriod = 2_000
    shutdownTimeout = 10_000
    requestReadTimeoutSeconds = 30
    responseWriteTimeoutSeconds = 30
}) {
    routing { get("/") { call.respondText("OK") } }
}.start(wait = true)
```

---

## 엔진 선택 가이드

- **순수 HTTP 서버, 최고 성능** → Netty
- **이미 Jetty / Tomcat 인프라 운영 중** → Jetty / Tomcat
- **Native 바이너리, 멀티플랫폼** → CIO
- **WAR로 외부 컨테이너 배포** → ServletApplicationEngine
