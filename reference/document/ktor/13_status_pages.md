# 13. StatusPages — 예외와 상태 코드 처리

> 출처: https://ktor.io/docs/server-status-pages.html

---

## 개요

`StatusPages` 플러그인은 두 가지를 한 곳에서 처리합니다.

1. 핸들러에서 던져진 **예외**를 상태 코드/본문으로 매핑.
2. 특정 **HTTP 상태 코드**(예: 404)가 만들어졌을 때 응답 본문을 일관되게 교체.

도메인 예외를 그대로 throw하고, 응답으로의 매핑은 한 곳에 모아두는 게 핵심 아이디어입니다.

---

## 의존성과 설치

```kotlin
implementation("io.ktor:ktor-server-status-pages:$ktor_version")
```

```kotlin
fun Application.configureStatusPages() {
    install(StatusPages) {
        // 1) 예외 매핑
        exception<NotFoundException> { call, _ ->
            call.respond(HttpStatusCode.NotFound, ErrorBody("not_found"))
        }
        exception<IllegalArgumentException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, ErrorBody(cause.message ?: "bad request"))
        }
        exception<Throwable> { call, cause ->
            application.log.error("unhandled", cause)
            call.respond(HttpStatusCode.InternalServerError, ErrorBody("internal_error"))
        }

        // 2) 상태 코드 핸들러
        status(HttpStatusCode.NotFound) { call, status ->
            call.respondText("404 — ${call.request.uri}", status = status)
        }
    }
}
```

`exception<T>`는 **가장 구체적인 타입부터** 매칭됩니다. 마지막에 `Throwable` 폴백을 두면 모든 미처리 예외를 잡아낼 수 있습니다.

---

## 정적 HTML로 에러 페이지 제공

`statusFile`은 정해진 패턴의 HTML을 코드별로 서빙합니다. 패턴의 `#`이 상태 코드로 치환됩니다.

```kotlin
install(StatusPages) {
    statusFile(
        HttpStatusCode.NotFound,
        HttpStatusCode.Unauthorized,
        filePattern = "error#.html"
    )
}
```

위 예시는 404 → `error404.html`, 401 → `error401.html`을 응답합니다 (둘 다 정적 리소스에 있어야 함).

---

## 도메인 예외와 묶기

이런 패턴이 흔합니다.

```kotlin
sealed class AppException(msg: String) : RuntimeException(msg) {
    class NotFound(what: String)        : AppException("$what not found")
    class Forbidden(reason: String)     : AppException(reason)
    class Validation(val field: String) : AppException("invalid: $field")
}

install(StatusPages) {
    exception<AppException.NotFound>   { call, e -> call.respond(HttpStatusCode.NotFound, ErrorBody(e.message!!)) }
    exception<AppException.Forbidden>  { call, e -> call.respond(HttpStatusCode.Forbidden, ErrorBody(e.message!!)) }
    exception<AppException.Validation> { call, e -> call.respond(HttpStatusCode.UnprocessableEntity, ValidationError(e.field)) }
}
```

비즈니스 로직에서는 그냥 `throw AppException.NotFound("user")`만 해도 됩니다.

---

## 주의할 점

- `StatusPages`에서 응답을 보낸 후 같은 호출에 다시 응답을 시도하면 예외가 발생합니다.
- `status()` 핸들러는 상태 코드가 명시적으로 설정된 경우에만 동작합니다 — 라우트 매칭이 실패해서 발생한 `404`도 처리하려면 `status(HttpStatusCode.NotFound)`를 등록해 두세요.
- `CallLogging`을 함께 사용하는 경우, 예외가 잡혀도 로그가 유실되지 않도록 `exception<Throwable>`에서 명시적으로 로그를 남기는 것이 안전합니다.
