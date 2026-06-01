# 05. 라우팅 (Routing)

> 출처: https://ktor.io/docs/server-routing.html
> 한국어 학습 노트입니다.

---

## Routing 플러그인

라우팅은 사실상 모든 Ktor 서버의 핵심이며, 그 자체가 **플러그인**입니다. (`"Routing is a Plugin"`) `routing { ... }` DSL을 호출하면 자동으로 설치됩니다.

```kotlin
fun Application.module() {
    routing {
        get("/hello") { call.respondText("Hello") }
    }
}
```

라우트 한 개를 정의하려면 **세 가지**가 필요합니다.

1. HTTP 동사 — `get`, `post`, `put`, `patch`, `delete`, `head`, `options`
2. 경로 패턴 — `"/users/{id}"`
3. 핸들러 람다 — `ApplicationCall` 컨텍스트에서 응답 작성

---

## 경로 패턴

| 패턴 | 매칭 | 추출 방법 |
| --- | --- | --- |
| `/hello` | 정확히 `/hello` | — |
| `/user/{login}` | 한 세그먼트(필수) | `call.parameters["login"]` |
| `/user/{login?}` | 선택적, **경로 끝에서만** | 같음 |
| `/user/*` | 임의의 한 세그먼트(와일드카드) | — |
| `/user/{...}` | 남은 경로 전체(tailcard) | `call.parameters.getAll("...")` |
| `Regex(".+/hello")` | 정규식 매칭 | 명명 그룹 사용 가능 |

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

## 트레일링 슬래시

Ktor는 기본적으로 `/foo`와 `/foo/`를 **다른 라우트로 취급**합니다. 합치고 싶다면 `IgnoreTrailingSlash` 플러그인을 설치합니다.

```kotlin
install(IgnoreTrailingSlash)
```

---

## 라우트 그룹화

### 경로 기반 그룹

```kotlin
route("/api/v1") {
    route("/users") {
        get { /* GET /api/v1/users */ }
        get("/{id}") { /* GET /api/v1/users/{id} */ }
        post { /* POST /api/v1/users */ }
    }
}
```

### 동사 기반 그룹

```kotlin
route("/orders/{id}") {
    get { /* read */ }
    put { /* update */ }
    delete { /* delete */ }
}
```

---

## Route 확장으로 모듈화하기

라우트가 늘어나면 **`Route` 확장 함수**로 파일을 쪼개 둡니다. 이게 사실상 Ktor에서의 컨트롤러 단위입니다.

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

## Route 스코프 플러그인

플러그인을 특정 라우트 서브트리에만 적용할 수도 있습니다.

```kotlin
routing {
    route("/admin") {
        install(SomePlugin) { /* admin 전용 설정 */ }
        get("/dashboard") { /* ... */ }
    }
}
```

같은 플러그인이 글로벌·라우트 양쪽에 설치돼 있다면 **더 안쪽(라우트 쪽)** 설정이 우선합니다.

---

## 디버깅 — 라우트 트레이싱

매칭이 왜 안 되는지 확인하려면 라우팅 로거 레벨을 `TRACE`로 올립니다.

```xml
<logger name="io.ktor.server.routing" level="TRACE"/>
```

요청별로 어떤 라우트 후보가 어떻게 점수 매겨졌는지 로그가 찍힙니다.
