# 06. 요청과 응답

> 출처:
> - https://ktor.io/docs/server-requests.html
> - https://ktor.io/docs/server-responses.html
>

---

## ApplicationCall

핸들러 안에서 사용하는 `call`은 `ApplicationCall` 타입으로, 두 핵심 객체를 포함합니다.

- `call.request` — 들어온 요청
- `call.response` — 나갈 응답

---

## 요청 읽기

### 경로/쿼리/헤더/쿠키

```kotlin
get("/search") {
    val id = call.parameters["id"]                     // 경로 변수
    val q  = call.request.queryParameters["q"]         // ?q=...
    val ua = call.request.headers[HttpHeaders.UserAgent]
    val sid = call.request.cookies["sid"]
}
```

### 연결 정보

```kotlin
call.request.local.scheme    // http / https
call.request.local.host
call.request.local.port
call.request.uri             // 풀 경로(쿼리 포함)
```

### 필수값 강제

값이 없으면 `MissingRequestParameterException`을 던지는 검증 헬퍼입니다.

```kotlin
val id    = call.requirePathParameter("id")
val token = call.requireHeader("X-Token")
val sid   = call.requireCookie("sid")
val limit = call.requireQueryParameter("limit")
```

---

## 본문(Body) 읽기

| 호출 | 용도 |
| --- | --- |
| `call.receiveText()` | 본문을 `String`으로 |
| `call.receive<ByteArray>()` | 바이트 배열 |
| `call.receive<ByteReadChannel>()` | 스트리밍 |
| `call.receive<T>()` | JSON 등으로 역직렬화 (ContentNegotiation 필요) |
| `call.receiveParameters()` | `application/x-www-form-urlencoded` 또는 `multipart/form-data` |
| `call.receiveMultipart()` | `multipart/form-data` |

### 데이터 클래스 역직렬화

```kotlin
@Serializable
data class CreateUser(val name: String, val email: String)

post("/users") {
    val body = call.receive<CreateUser>()
    // ...
    call.respond(HttpStatusCode.Created, body)
}
```

`ContentNegotiation` 플러그인이 필요합니다. [08_content_negotiation.md](08_content_negotiation.md) 참고.

### Multipart 파일 업로드

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

## 응답 쓰기

### 응답 헬퍼

| 호출 | 용도 |
| --- | --- |
| `respondText(text, contentType?, status?)` | 평문/HTML 등 텍스트 |
| `respond(obj)` | 객체를 직렬화 (ContentNegotiation) |
| `respond(status, obj)` | 상태 코드 + 본문 |
| `respondHtml { ... }` | kotlinx.html DSL로 HTML 생성 |
| `respondFile(file)` | 파일 다운로드 |
| `respondBytes(bytes)` | 원시 바이트 |
| `respondOutputStream { ... }` | 스트리밍 |
| `respondRedirect(url, permanent)` | 301/302 리다이렉트 |
| `respondNullable(obj)` | nullable 값 그대로 |

### 상태 코드, 콘텐츠 타입, 헤더, 쿠키

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

### 리다이렉트

```kotlin
get("/old") {
    call.respondRedirect("/new", permanent = true)
}
```

### HTML DSL

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

## 자주 쓰는 조합 패턴

```kotlin
post("/login") {
    val req = call.receive<LoginRequest>()
    val user = service.login(req) ?: return@post call.respond(HttpStatusCode.Unauthorized)
    call.sessions.set(UserSession(user.id))
    call.respond(LoginResponse(token = issue(user)))
}
```
