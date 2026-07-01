# 07. 플러그인 (Plugins)

> 출처: https://ktor.io/docs/server-plugins.html

---

## 플러그인이란

Ktor의 **플러그인(Plugin)** 은 요청/응답 파이프라인을 가로채는 컴포넌트입니다. 직렬화, 인증, 로깅, 압축, CORS 같은 횡단 관심사를 한 줄의 `install`로 끼울 수 있게 해줍니다.

> Ktor의 라우팅조차 내부적으로 플러그인입니다 — `"Routing is a Plugin"`.

---

## 설치 패턴

플러그인은 **Application 레벨** 또는 **Route 레벨**에서 설치할 수 있습니다.

```kotlin
fun Application.module() {
    install(ContentNegotiation) { json() }   // Application 레벨
    install(CORS) { anyHost() }
    install(CallLogging)

    routing {
        route("/admin") {
            install(SomePlugin) { /* 이 서브트리만 */ }
        }
    }
}
```

같은 플러그인을 외부 범위와 내부 범위 모두에 설치한 경우, **더 안쪽(라우트) 설정이 전역 설정을 덮어씁니다.**

---

## 주요 내장 플러그인

| 플러그인 | 역할 |
| --- | --- |
| `ContentNegotiation` | JSON/XML/CBOR 등 자동 직렬화·역직렬화 |
| `StatusPages` | 예외/상태 코드별 응답 핸들러 |
| `Authentication` | Basic / JWT / OAuth / Session 등 인증 |
| `Sessions` | 세션 트랜스포트 + 저장 |
| `CORS` | Cross-Origin Resource Sharing 허용 |
| `Compression` | gzip / deflate 응답 압축 |
| `CallLogging` | 요청별 로그 |
| `CallId` | 요청에 추적 ID 부여 |
| `DefaultHeaders` | `Server`, `Date` 등 기본 응답 헤더 |
| `AutoHeadResponse` | `GET` 라우트에 대한 자동 `HEAD` 응답 |
| `ForwardedHeaders`, `XForwardedHeaders` | 프록시 뒤에서 원본 IP 인식 |
| `HSTS` | HTTPS 강제 헤더 |
| `WebSockets` | WebSocket 지원 |
| `IgnoreTrailingSlash` | `/foo` ↔ `/foo/` 동일 취급 |
| `RateLimit` | 요청 빈도 제한 |
| `RequestValidation` | 요청 바디 검증 |
| `MicrometerMetrics`, `DropwizardMetrics` | 메트릭 수출 |

설치 예시:

```kotlin
install(Compression) {
    gzip()
    deflate()
}

install(CallLogging) {
    level = Level.INFO
    filter { call -> call.request.path().startsWith("/api") }
}

install(CORS) {
    allowHost("example.com", schemes = listOf("https"))
    allowMethod(HttpMethod.Put)
    allowHeader(HttpHeaders.ContentType)
    allowCredentials = true
}

install(DefaultHeaders) {
    header(HttpHeaders.Server, "MyServer")
}
```

---

## 커스텀 플러그인 만들기

Ktor는 두 가지 빌더를 제공합니다.

| 빌더 | 스코프 |
| --- | --- |
| `createApplicationPlugin` | Application 전역 |
| `createRouteScopedPlugin` | Route 단위로 다른 설정 가능 |

### 단순한 예: 모든 응답에 헤더 추가

```kotlin
val RequestTimer = createApplicationPlugin("RequestTimer") {
    onCall { call ->
        val t0 = System.nanoTime()
        call.attributes.put(AttributeKey("t0"), t0)
    }
    onCallRespond { call, _ ->
        val t0 = call.attributes[AttributeKey<Long>("t0")]
        val dt = (System.nanoTime() - t0) / 1_000_000
        call.response.header("X-Response-Time-Ms", dt.toString())
    }
}

fun Application.module() {
    install(RequestTimer)
}
```

### 설정값을 받는 플러그인

```kotlin
class MyPluginConfig {
    var greeting: String = "Hello"
}

val MyPlugin = createApplicationPlugin(
    name = "MyPlugin",
    createConfiguration = ::MyPluginConfig,
) {
    val greeting = pluginConfig.greeting
    onCall { call ->
        call.response.header("X-Greet", greeting)
    }
}

install(MyPlugin) {
    greeting = "Hi"
}
```

### 라우트 스코프

```kotlin
val RouteOnly = createRouteScopedPlugin("RouteOnly") {
    onCall { call -> call.response.header("X-Where", "scoped") }
}

routing {
    route("/admin") {
        install(RouteOnly)
        get { call.respondText("admin") }
    }
}
```

---

## 훅(Hooks)

플러그인 내부에서 사용할 수 있는 주요 훅입니다.

| 훅 | 시점 |
| --- | --- |
| `onCall` | 라우트 핸들러보다 먼저 |
| `onCallReceive` | 본문 수신 전후 변환 |
| `onCallRespond` | 응답을 만들 때 |
| `on(CallFailed)` | 예외 발생 시 |
| `on(ResponseSent)` | 응답 전송 완료 후 |

이 훅들을 통해 `StatusPages` 같은 플러그인을 구현할 수 있습니다.
