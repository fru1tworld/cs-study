# Ktor 라우팅, 요청과 응답

## 05. 라우팅 (Routing)

> 출처: https://ktor.io/docs/server-routing.html

---

### Routing 플러그인

라우팅은 사실상 모든 Ktor 서버의 핵심이며, 그 자체가 플러그인임 (`"Routing is a Plugin"`) → `routing { ... }` DSL을 호출하면 자동으로 설치됨.

```kotlin
fun Application.module() {
    routing {
        get("/hello") { call.respondText("Hello") }
    }
}
```

라우트 한 개를 정의하려면 세 가지가 필요함.

1. HTTP 동사 — `get`, `post`, `put`, `patch`, `delete`, `head`, `options`
2. 경로 패턴 — `"/users/{id}"`
3. 핸들러 람다 — `ApplicationCall` 컨텍스트에서 응답 작성

---

### 경로 패턴

- `/hello`: 정확히 `/hello`에 매칭, 추출 방법 없음
- `/user/{login}`: 한 세그먼트(필수)에 매칭 → `call.parameters["login"]`으로 추출
- `/user/{login?}`: 선택적 매칭, 경로 끝에서만 사용 가능 → `call.parameters["login"]`으로 추출
- `/user/*`: 임의의 한 세그먼트(와일드카드)에 매칭, 추출 방법 없음
- `/user/{...}`: 남은 경로 전체(tailcard)에 매칭 → `call.parameters.getAll("...")`으로 추출
- `Regex(".+/hello")`: 정규식 매칭, 명명 그룹 사용 가능

```kotlin
get("/users/{id}") {
    val id = call.parameters["id"]   // 필수 파라미터
    call.respondText("user $id")
}

get("/files/{path...}") {
    val segments = call.parameters.getAll("path") ?: emptyList()
    call.respondText(segments.joinToString("/"))
}
```

---

### 트레일링 슬래시

Ktor는 기본적으로 `/foo`와 `/foo/`를 다른 라우트로 취급함 → 합치려면 `IgnoreTrailingSlash` 플러그인을 설치함.

```kotlin
install(IgnoreTrailingSlash)
```

---

### 라우트 그룹화

#### 경로 기반 그룹

```kotlin
route("/api/v1") {
    route("/users") {
        get { /* GET /api/v1/users */ }
        get("/{id}") { /* GET /api/v1/users/{id} */ }
        post { /* POST /api/v1/users */ }
    }
}
```

#### 동사 기반 그룹

```kotlin
route("/orders/{id}") {
    get { /* read */ }
    put { /* update */ }
    delete { /* delete */ }
}
```

---

### Route 확장으로 모듈화하기

라우트가 늘어나면 `Route` 확장 함수로 파일을 쪼개 둠 → 사실상 Ktor에서의 컨트롤러 단위임.

```kotlin
// UserRoutes.kt
fun Route.userRoutes() {
    route("/users") {
        get { /* ... */ }
        get("/{id}") { /* ... */ }
        post { /* ... */ }
    }
}

// Routing.kt
fun Application.configureRouting() {
    routing {
        userRoutes()
        orderRoutes()
        adminRoutes()
    }
}
```

---

### Route 스코프 플러그인

플러그인을 특정 라우트 서브트리에만 적용 가능함.

```kotlin
routing {
    route("/admin") {
        install(SomePlugin) { /* admin 전용 설정 */ }
        get("/dashboard") { /* ... */ }
    }
}
```

같은 플러그인이 글로벌·라우트 양쪽에 설치되어 있다면 더 안쪽(라우트 쪽) 설정이 우선함.

---

### 디버깅 — 라우트 트레이싱

매칭이 왜 안 되는지 확인하려면 라우팅 로거 레벨을 `TRACE`로 올림.

```xml
<logger name="io.ktor.server.routing" level="TRACE"/>
```

요청별로 어떤 라우트 후보가 어떻게 평가됐는지 로그가 출력됨.

---

## 06. 요청과 응답

> 출처:
> - https://ktor.io/docs/server-requests.html
> - https://ktor.io/docs/server-responses.html
>

---

### ApplicationCall

핸들러 안에서 사용하는 `call`은 `ApplicationCall` 타입으로, 두 핵심 객체를 포함함.

- `call.request` — 들어온 요청
- `call.response` — 나갈 응답

---

### 요청 읽기

#### 경로/쿼리/헤더/쿠키

```kotlin
get("/search") {
    val id = call.parameters["id"]                     // 경로 변수
    val q  = call.request.queryParameters["q"]         // ?q=...
    val ua = call.request.headers[HttpHeaders.UserAgent]
    val sid = call.request.cookies["sid"]
}
```

#### 연결 정보

```kotlin
call.request.local.scheme    // http / https
call.request.local.host
call.request.local.port
call.request.uri             // 풀 경로(쿼리 포함)
```

#### 필수값 강제

값이 없으면 `MissingRequestParameterException`을 던지는 검증 헬퍼임.

```kotlin
val id    = call.requirePathParameter("id")
val token = call.requireHeader("X-Token")
val sid   = call.requireCookie("sid")
val limit = call.requireQueryParameter("limit")
```

---

### 본문(Body) 읽기

- `call.receiveText()`: 본문을 `String`으로 읽음
- `call.receive<ByteArray>()`: 바이트 배열로 읽음
- `call.receive<ByteReadChannel>()`: 스트리밍 용도
- `call.receive<T>()`: JSON 등으로 역직렬화(ContentNegotiation 필요)
- `call.receiveParameters()`: `application/x-www-form-urlencoded` 또는 `multipart/form-data` 용도
- `call.receiveMultipart()`: `multipart/form-data` 용도

#### 데이터 클래스 역직렬화

```kotlin
@Serializable
data class CreateUser(val name: String, val email: String)

post("/users") {
    val body = call.receive<CreateUser>()
    // ...
    call.respond(HttpStatusCode.Created, body)
}
```

`ContentNegotiation` 플러그인 필요 → [08_content_negotiation.md](08_content_negotiation.md) 참고.

#### Multipart 파일 업로드

```kotlin
post("/upload") {
    val multipart = call.receiveMultipart()
    multipart.forEachPart { part ->
        when (part) {
            is PartData.FormItem -> {
                println("${part.name} = ${part.value}")
            }
            is PartData.FileItem -> {
                val name = part.originalFileName ?: "file"
                part.provider().copyAndClose(File("uploads/$name").writeChannel())
            }
            else -> {}
        }
        part.dispose()
    }
    call.respond(HttpStatusCode.OK)
}
```

---

### 응답 쓰기

#### 응답 헬퍼

- `respondText(text, contentType?, status?)`: 평문/HTML 등 텍스트 응답 용도
- `respond(obj)`: 객체를 직렬화해 응답(ContentNegotiation)
- `respond(status, obj)`: 상태 코드 + 본문 응답
- `respondHtml { ... }`: kotlinx.html DSL로 HTML 생성
- `respondFile(file)`: 파일 다운로드 용도
- `respondBytes(bytes)`: 원시 바이트 응답 용도
- `respondOutputStream { ... }`: 스트리밍 용도
- `respondRedirect(url, permanent)`: 301/302 리다이렉트 용도
- `respondNullable(obj)`: nullable 값을 그대로 응답

#### 상태 코드, 콘텐츠 타입, 헤더, 쿠키

```kotlin
get("/x") {
    call.response.status(HttpStatusCode.Created)
    call.response.header("X-Trace-Id", "abc-123")
    call.response.cookies.append(
        Cookie(name = "sid", value = "...", path = "/", httpOnly = true)
    )
    call.respondText("ok", ContentType.Text.Plain)
}
```

#### 리다이렉트

```kotlin
get("/old") {
    call.respondRedirect("/new", permanent = true)
}
```

#### HTML DSL

```kotlin
get("/") {
    call.respondHtml {
        head { title { +"Ktor" } }
        body {
            h1 { +"Hello" }
            p { +"From kotlinx.html" }
        }
    }
}
```

---

### 자주 쓰는 조합 패턴

```kotlin
post("/login") {
    val req = call.receive<LoginRequest>()
    val user = service.login(req) ?: return@post call.respond(HttpStatusCode.Unauthorized)
    call.sessions.set(UserSession(user.id))
    call.respond(LoginResponse(token = issue(user)))
}
```
