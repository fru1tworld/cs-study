# Ktor 엔진, 설정 파일, Application 모듈

## 03. 엔진 (Engines)

> 출처: https://ktor.io/docs/server-engines.html

---

### 엔진이란

Ktor의 엔진(Engine)은 실제로 TCP 소켓을 열고 HTTP 요청을 받아 Ktor 파이프라인에 전달하는 컴포넌트임. Ktor는 엔진을 추상화함 → 같은 애플리케이션 코드를 Netty에서도 Jetty에서도 실행 가능.

---

### 지원 엔진 비교

- Netty
  - 플랫폼: JVM
  - HTTP/2: 지원
  - 비고: 고성능 비동기 NIO, 기본 선택지
- Jetty
  - 플랫폼: JVM
  - HTTP/2: 지원
  - 비고: Eclipse 기반, 서블릿 컨테이너 친화
- Tomcat
  - 플랫폼: JVM
  - HTTP/2: 지원
  - 비고: Apache 표준 컨테이너
- CIO
  - 플랫폼: JVM / Native / GraalVM / JS / WasmJs
  - HTTP/2: 미지원
  - 비고: 코루틴 기반, 멀티플랫폼 가능
- ServletApplicationEngine
  - 플랫폼: JVM
  - HTTP/2: 지원
  - 비고: WAR로 외부 컨테이너에 배포할 때 사용

HTTP/2가 필요하면 Netty · Jetty · Tomcat 중 선택. Native 빌드가 필요하면 CIO 선택.

---

### 두 가지 서버 시작 방식

#### embeddedServer (코드 중심)

엔진, 포트, 모듈을 코드 인자로 직접 지정함. 빠른 프로토타이핑·테스트에 적합.

```kotlin
fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        routing { get("/") { call.respondText("Hello") } }
    }.start(wait = true)
}
```

#### EngineMain (설정 파일 중심)

엔진 모듈이 제공하는 `EngineMain.main(args)`를 사용함. 포트·모듈은 `application.conf` / `application.yaml`에서 읽음. 운영 배포에 적합.

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

`EngineMain.createServer(args)`를 사용하면 서버 인스턴스만 생성하고 `start()` 호출 시점을 직접 제어 가능.

---

### 공통 엔진 설정 옵션

`embeddedServer(... , configure = { ... })` 블록이나 설정 파일의 `ktor.deployment.*` 키로 지정함.

- `connectors`: 호스트/포트/SSL 등 수신 커넥터 목록
- `connectionGroupSize`: 새 연결을 수락하는 스레드 수
- `workerGroupSize`: 연결을 처리하는 이벤트 루프 그룹 크기
- `callGroupSize`: 요청 핸들러를 실행하는 스레드 풀의 최소 크기
- `shutdownGracePeriod`: 그레이스풀 셧다운 시 새 요청 거부 후 대기 시간(ms)
- `shutdownTimeout`: 셧다운 전체 최대 대기 시간(ms)

---

### 엔진별 특화 옵션 (대표)

#### Netty

- `requestQueueLimit`: 큐잉되는 요청 수 상한
- `shareWorkGroup`: 워커 그룹 공유 여부
- `responseWriteTimeoutSeconds`: 응답 쓰기 타임아웃
- `requestReadTimeoutSeconds`: 요청 본문 읽기 타임아웃
- `tcpKeepAlive`: TCP keep-alive

#### Jetty

- `configureServer`: 내부 `org.eclipse.jetty.server.Server`에 직접 접근
- `idleTimeout`: 유휴 연결 종료 시간

#### CIO

- `connectionIdleTimeoutSeconds`: 연결 유휴 타임아웃

#### Tomcat

- `configureTomcat`: 내부 Tomcat 인스턴스 핸들

---

### 상세 설정 예시

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

### 엔진 선택 가이드

- 순수 HTTP 서버, 최고 성능 필요 → Netty
- 이미 Jetty · Tomcat 인프라 운영 중 → Jetty / Tomcat
- Native 바이너리, 멀티플랫폼 필요 → CIO
- WAR로 외부 컨테이너 배포 필요 → ServletApplicationEngine

---

## 04. 설정 파일과 Application 모듈

> 출처:
> - https://ktor.io/docs/server-configuration-file.html
> - https://ktor.io/docs/server-modules.html
>
---

### 설정 포맷 — HOCON vs YAML

Ktor는 두 가지 설정 포맷을 지원함. 둘 다 `src/main/resources/` 디렉터리에 위치.

- `application.conf` (HOCON)
  - 의존성: 기본 제공
  - 환경변수 문법: `${ENV}`
  - Maven 빌드 지원: 허용
  - 스타일: 중괄호 블록
- `application.yaml` (YAML)
  - 의존성: `io.ktor:ktor-server-config-yaml` 필요
  - 환경변수 문법: `${ENV}`, `$ENV` 모두 지원
  - Maven 빌드 지원: 현 시점 미지원
  - 스타일: 들여쓰기

---

### 사전 정의된 키들

#### `ktor.deployment.*`

- `port`: 평문 HTTP 포트 (`0`이면 랜덤)
- `sslPort`: SSL 포트
- `host`: 바인딩 주소 (예: `0.0.0.0`)
- `watch`: 자동 재로드 대상 경로 목록
- `rootPath`: 서블릿 컨텍스트 경로
- `shutdownGracePeriod`: 신규 요청 거부 후 대기(ms)
- `shutdownTimeout`: 셧다운 최대 시간(ms)
- `connectionGroupSize`: accept 스레드 수
- `workerGroupSize`: 이벤트 루프 크기
- `callGroupSize`: 핸들러 풀 최소 크기

#### `ktor.application.*`

- `modules`: 로드할 모듈 함수의 FQN 목록 (필수)

#### `ktor.security.ssl.*` (SSL 사용 시)

`keyStore`, `keyAlias`, `keyStorePassword`, `privateKeyPassword`, `trustStore`, `trustStorePassword`, `enabledProtocols`.

---

### 예시 — application.yaml

```yaml
ktor:
  deployment:
    port: 8080
    host: 0.0.0.0
  application:
    modules:
      - com.example.ApplicationKt.module
```

### 예시 — application.conf (HOCON)

```hocon
ktor {
    deployment {
        port = 8080
        port = ${?PORT}              # 환경변수 있으면 덮어씀
    }
    application {
        modules = [ com.example.ApplicationKt.module ]
    }
}
```

---

### 환경변수 오버라이드

```hocon
# HOCON
port = ${PORT}      # 필수 — 없으면 오류
port = ${?PORT}     # 선택 — 없으면 다음 값으로 폴백
port = 8080
```

```yaml
# YAML
port: ${PORT}
port: ${PORT:8080}  # 콜론 뒤가 기본값
```

---

### 커맨드라인 오버라이드

`EngineMain` 사용 시 빌드된 jar에 인자를 전달하면 설정값을 덮어쓰기 가능.

```bash
java -jar app.jar -port=9090
java -jar app.jar -config=custom.conf
java -jar app.jar -P:ktor.deployment.callGroupSize=7
```

---

### 코드에서 설정값 읽기

#### 단순 접근

```kotlin
fun Application.module() {
    val port = environment.config
        .propertyOrNull("ktor.deployment.port")
        ?.getString() ?: "8080"
}
```

#### 타입 안전한 디코딩

```kotlin
@Serializable
data class AppConfig(val security: Security) {
    @Serializable data class Security(val clientId: String)
}

fun Application.module() {
    val config = environment.config.getAs<AppConfig>()
    val clientId = config.security.clientId
}
```

---

### 환경별 분기 처리

`dev`/`prod`처럼 배포 환경에 따라 동작을 바꾸고 싶을 때는 사전 정의된 키 대신 커스텀 프로퍼티를 하나 두고 환경변수로 채워 넣는 방식이 흔함.

```hocon
ktor {
    environment = ${?KTOR_ENV}
}
```

`KTOR_ENV` 환경변수 값이 `ktor.environment`로 들어가며, 코드에서는 이 값을 읽어 분기함.

```kotlin
fun Application.module() {
    val env = environment.config.propertyOrNull("ktor.environment")?.getString()

    routing {
        get {
            call.respondText(
                when (env) {
                    "dev" -> "Development"
                    "prod" -> "Production"
                    else -> "..."
                }
            )
        }
    }
}
```

로컬에서는 `KTOR_ENV=dev`, 운영 환경에서는 `KTOR_ENV=prod`로 실행 시점에 값을 넣어주면 됨.

---

### Application 모듈이란

모듈(Module)은 `Application` 클래스의 확장 함수로 정의되는 단위임. 라우팅, 플러그인 설치, 직렬화 등 한 묶음의 설정을 캡슐화.

```kotlin
fun Application.module() {
    install(ContentNegotiation) { json() }
    configureRouting()
}

fun Application.configureRouting() {
    routing {
        get("/") { call.respondText("OK") }
    }
}
```

모듈로 쪼개는 이유:

- 관심사별 분리 (`configureRouting`, `configureSecurity`, `configureSerialization`)
- 도메인 단위 격리 → 테스트 용이
- 여러 환경에서 다른 모듈 조합으로 부팅 가능

---

### 모듈 등록 — embeddedServer

```kotlin
fun main() {
    embeddedServer(Netty, port = 8080, module = Application::module)
        .start(wait = true)
}
```

여러 모듈을 람다 안에서 호출하는 형태도 흔함.

```kotlin
embeddedServer(CIO, port = 8080) {
    val service = MyService()
    routingModule(service)
    schedulingModule(service)
}.start(wait = true)
```

### 모듈 등록 — 설정 파일

```yaml
ktor:
  application:
    modules:
      - com.example.ApplicationKt.module
      - com.example.AdminKt.adminModule
```

설정 파일에서 등록할 때는 모듈 함수의 완전한 정규화 이름(예: `com.example.ApplicationKt.module`)을 정확히 명시 필요.

---

### 모듈 간 의존성 공유

모듈이 여러 개로 쪼개지면 서비스·리포지토리 같은 공용 의존성을 어떻게 넘길지 정해야 함. 세 가지 접근이 흔함.

#### 파라미터로 전달

```kotlin
fun main() {
    embeddedServer(CIO, port = 8080) {
        val myService = MyService(property<MyServiceConfig>())
        routingModule(myService)
        schedulingModule(myService)
    }.start(wait = true)
}
```

의존성이 함수 시그니처에 그대로 드러나 추적하기 쉬움 → 소·중형 애플리케이션에 적합하지만, 모듈 간 컴파일 타임 결합이 생김.

#### Application.attributes

```kotlin
val customerServiceKey = AttributeKey<CustomerService>("CustomerService")

fun Application.servicesModule() {
    attributes[customerServiceKey] = CustomerService()
}

fun Application.customerModule() {
    val service = attributes[customerServiceKey]
    routing { /* service 사용 */ }
}
```

모듈끼리 서로 직접 참조하지 않고 `Application` 인스턴스를 통해 값을 주고받음 → 느슨한 결합을 얻는 대신, 타입이 attributes 맵 뒤에 숨어 컴파일 타임 검증을 잃음.

#### DI 플러그인

Ktor가 제공하는 의존성 주입 플러그인을 설치하면 경량 컨테이너에 의존성을 등록하고 필요한 곳에서 바로 꺼내 쓸 수 있음 → 대규모 애플리케이션에서 모듈 수가 많아질 때 위 두 방식보다 관리가 수월함.

---

### 모듈 병렬 로딩

모듈이 많고 각각 외부 리소스(DB, 메시지 브로커 등)에 연결하는 부팅 로직을 갖고 있으면, 기본 순차 로딩 방식은 시작 시간이 모듈 수에 비례해 늘어남. `ktor.application.startup`을 `concurrent`로 바꾸면 모듈들을 동시에 로드함.

- `ktor.application.startup`: `sequential`(기본값) / `concurrent`
- `ktor.application.startupTimeoutMillis`: 로딩 전체에 허용할 최대 시간(ms), 기본값 `10000`

```hocon
ktor {
    application {
        startup = concurrent
    }
}
```

모듈 함수를 `suspend fun Application.xxx()`로 선언해두면 동시 로딩 시 각 모듈의 초기화(예: 외부 커넥션 수립)가 블로킹 없이 병렬로 진행됨.

```kotlin
suspend fun Application.installEvents() {
    val kubernetesConnection = connect(property<KubernetesConfig>())
}
```

동시 모듈 로딩 자체는 단일 스레드에서 코루틴으로 진행되므로, 애플리케이션 내부 공유 상태에 대한 스레드 안전성 문제는 새로 생기지 않음.
