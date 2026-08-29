# Ktor 플러그인과 Content Negotiation

## 07. 플러그인 (Plugins)

> 출처: https://ktor.io/docs/server-plugins.html

---

### 플러그인이란

Ktor의 플러그인(Plugin)은 요청/응답 파이프라인을 가로채는 컴포넌트임 → 직렬화, 인증, 로깅, 압축, CORS 같은 횡단 관심사를 한 줄의 `install`로 끼울 수 있음.

> Ktor의 라우팅조차 내부적으로 플러그인임 — `"Routing is a Plugin"`.

---

### 설치 패턴

플러그인은 Application 레벨 또는 Route 레벨에서 설치 가능.

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

같은 플러그인을 외부 범위와 내부 범위 모두에 설치한 경우, 더 안쪽(라우트) 설정이 전역 설정을 덮어씀.

---

### 주요 내장 플러그인

- `ContentNegotiation`: JSON/XML/CBOR 등 자동 직렬화·역직렬화 담당
- `StatusPages`: 예외/상태 코드별 응답 핸들러
- `Authentication`: Basic / JWT / OAuth / Session 등 인증 처리
- `Sessions`: 세션 트랜스포트 + 저장
- `CORS`: Cross-Origin Resource Sharing 허용
- `Compression`: gzip / deflate 응답 압축
- `CallLogging`: 요청별 로그 기록
- `CallId`: 요청에 추적 ID 부여
- `DefaultHeaders`: `Server`, `Date` 등 기본 응답 헤더 설정
- `AutoHeadResponse`: `GET` 라우트에 대한 자동 `HEAD` 응답 생성
- `ForwardedHeaders`, `XForwardedHeaders`: 프록시 뒤에서 원본 IP 인식
- `HSTS`: HTTPS 강제 헤더 부여
- `WebSockets`: WebSocket 지원
- `IgnoreTrailingSlash`: `/foo` 와 `/foo/` 를 동일 취급
- `RateLimit`: 요청 빈도 제한
- `RequestValidation`: 요청 바디 검증
- `MicrometerMetrics`, `DropwizardMetrics`: 메트릭 수출

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

### 커스텀 플러그인 만들기

Ktor는 두 가지 빌더를 제공함.

- `createApplicationPlugin`: Application 전역 스코프에 적용
- `createRouteScopedPlugin`: Route 단위로 다른 설정 적용 가능

#### 단순한 예: 모든 응답에 헤더 추가

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

#### 설정값을 받는 플러그인

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

#### 라우트 스코프

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

### 훅(Hooks)

플러그인 내부에서 사용 가능한 주요 훅.

- `onCall`: 라우트 핸들러보다 먼저 실행
- `onCallReceive`: 본문 수신 전후 변환 시점
- `onCallRespond`: 응답을 만들 때 실행
- `on(CallFailed)`: 예외 발생 시 실행
- `on(ResponseSent)`: 응답 전송 완료 후 실행

이 훅들을 통해 `StatusPages` 같은 플러그인 구현 가능.

---

## 08. Content Negotiation 과 직렬화

> 출처: https://ktor.io/docs/server-serialization.html

---

### 무엇을 하는 플러그인인가

`ContentNegotiation`은 두 가지를 동시에 처리함.

1. 콘텐츠 협상: 클라이언트의 `Accept` 헤더와 서버가 지원하는 포맷을 매칭
2. 직렬화/역직렬화: JSON / XML / CBOR / ProtoBuf 등을 객체 ↔ 본문으로 자동 변환

이게 없으면 `call.receive<MyDto>()`나 `call.respond(myDto)`가 동작하지 않음.

---

### 의존성

- 코어: `io.ktor:ktor-server-content-negotiation`
- 포맷별 컨버터: `ktor-serialization-kotlinx-json`, `-xml`, `-cbor`, `-protobuf`, `ktor-serialization-jackson`, `ktor-serialization-gson` 등

`build.gradle.kts` 예:

```kotlin
dependencies {
    implementation("io.ktor:ktor-server-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")
}
```

---

### 설치

```kotlin
install(ContentNegotiation) {
    json()       // kotlinx.serialization
    // xml()
    // cbor()
}
```

설정을 전달하는 경우:

```kotlin
install(ContentNegotiation) {
    json(Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = false
    })
}
```

여러 포맷을 동시에 등록해 두면 `Accept` 헤더에 따라 자동 선택됨.

---

### 사용 흐름

#### 응답 직렬화

```kotlin
@Serializable
data class User(val id: Long, val name: String)

get("/users/{id}") {
    val user = service.find(call.parameters["id"]!!)
    call.respond(user)        // Accept: application/json → JSON
}
```

#### 요청 역직렬화

```kotlin
@Serializable
data class CreateUser(val name: String, val email: String)

post("/users") {
    val req = call.receive<CreateUser>()
    val created = service.create(req)
    call.respond(HttpStatusCode.Created, created)
}
```

---

### 라이브러리별 비교

- kotlinx.serialization
  - 강점: Kotlin-native, 멀티플랫폼, `@Serializable` 지원
  - 비고: Ktor 공식 권장
- Jackson
  - 강점: 풍부한 모듈 / 어노테이션 생태계
  - 비고: 리플렉션 기반
- Gson
  - 강점: 단순함
  - 비고: 코틀린 null/default 처리에서 가끔 함정 발생

---

### 커스텀 컨버터

`ContentConverter` 인터페이스를 직접 구현해 임의의 미디어 타입 처리 가능.

```kotlin
class CsvConverter : ContentConverter {
    override suspend fun serialize(
        contentType: ContentType, charset: Charset, typeInfo: TypeInfo, value: Any
    ): OutgoingContent? = TODO()

    override suspend fun deserialize(
        charset: Charset, typeInfo: TypeInfo, content: ByteReadChannel
    ): Any? = TODO()
}

install(ContentNegotiation) {
    register(ContentType.Text.CSV, CsvConverter())
}
```

---

### 주의할 점

- `@Serializable`이 없는 일반 클래스는 kotlinx.serialization에서 처리 불가
- 클라이언트가 `Accept: */*`인 경우 등록 순서대로 첫 컨버터가 사용됨
- `respondNullable`이 아닌 `respond`에 `null`을 넘기면 예외 발생
