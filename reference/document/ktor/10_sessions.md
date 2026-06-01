# 10. 세션 (Sessions)

> 출처: https://ktor.io/docs/server-sessions.html
> 한국어 학습 노트입니다.

---

## 개요

`Sessions` 플러그인은 요청 간에 데이터를 **유지**합니다. 결정해야 할 축이 세 가지입니다.

1. **트랜스포트** — 어떻게 클라이언트와 주고받을 것인가? (`cookie` / `header`)
2. **저장 위치** — 페이로드를 어디에 두는가? (클라이언트 측 / 서버 측)
3. **보호 방식** — 변조 방지(서명) / 노출 방지(암호화)

---

## 데이터 클래스 정의

```kotlin
data class UserSession(val userId: String, val count: Int = 0)
```

세션 객체는 직렬화 가능해야 합니다. 기본 직렬화는 자체 포맷을 사용하지만 kotlinx.serialization도 쓸 수 있습니다.

---

## 설치

```kotlin
install(Sessions) {
    cookie<UserSession>("user_session") {
        cookie.path = "/"
        cookie.maxAgeInSeconds = 60 * 60          // 1시간
        cookie.httpOnly = true
        cookie.secure = true                       // HTTPS에서만
        cookie.extensions["SameSite"] = "Lax"
    }
}
```

`cookie<T>(name)` 대신 `header<T>(name)`을 쓰면 API용으로 헤더를 사용하게 됩니다.

```kotlin
install(Sessions) {
    header<CartSession>("X-Cart")
}
```

---

## 저장 위치

### 클라이언트 측 (기본)

쿠키/헤더 값에 페이로드를 직접 인코딩해서 클라이언트가 들고 다닙니다. 서버는 stateless.

### 서버 측

세션 ID만 클라이언트에 두고, 페이로드는 서버 저장소에 보관합니다.

```kotlin
install(Sessions) {
    cookie<UserSession>("user_session", SessionStorageMemory())
    // 또는:
    cookie<UserSession>("user_session", directorySessionStorage(File("build/.sessions")))
}
```

- `SessionStorageMemory()` — 로컬 개발용. 재시작 시 소실.
- `directorySessionStorage(File("..."))` — 파일 기반.
- `SessionStorage` 인터페이스를 직접 구현하면 Redis/DB 등으로 확장 가능.

---

## 변조 방지 / 암호화

### 서명만 (변조 감지)

```kotlin
cookie<UserSession>("user_session") {
    transform(
        SessionTransportTransformerMessageAuthentication(signKey)
    )
}
```

### 서명 + 암호화 (값까지 비공개)

```kotlin
cookie<UserSession>("user_session") {
    transform(
        SessionTransportTransformerEncrypt(encryptKey, signKey)
    )
}
```

`signKey`, `encryptKey`는 충분히 긴 무작위 바이트로, 환경변수에서 주입합니다.

---

## 세션 읽기/쓰기/삭제

```kotlin
get("/login") {
    call.sessions.set(UserSession(userId = "42"))
    call.respondText("OK")
}

get("/me") {
    val s = call.sessions.get<UserSession>()
        ?: return@get call.respond(HttpStatusCode.Unauthorized)
    call.respondText("hi ${s.userId}")
}

get("/logout") {
    call.sessions.clear<UserSession>()
    call.respondText("bye")
}
```

같은 요청 안에서 같은 타입에 대해 `set`을 다시 호출하면 값이 갱신됩니다.

---

## 인증과의 결합

Authentication의 `session` 프로바이더와 묶으면 "쿠키 = 로그인 토큰" 구조를 만들 수 있습니다 — [09_authentication.md](09_authentication.md) 참고.

```kotlin
install(Authentication) {
    session<UserSession>("auth-session") {
        validate { it }
        challenge { call.respondRedirect("/login") }
    }
}

routing {
    authenticate("auth-session") {
        get("/dashboard") {
            val s = call.principal<UserSession>()!!
            call.respondText("user=${s.userId}")
        }
    }
}
```

---

## 운영 팁

- 쿠키에는 항상 `httpOnly = true`, 가능하면 `secure = true`, `SameSite=Lax` 이상.
- 서명/암호화 키는 환경변수 또는 시크릿 매니저로 주입.
- 페이로드를 크게 들고 다니면 모든 요청에 cost가 붙음 → 길어지면 server-side 저장으로 전환.
