# Ktor 인증과 세션

## 09. 인증 (Authentication)

> 출처: https://ktor.io/docs/server-auth.html

---

### 개념

Ktor 인증은 `Authentication` 플러그인 + 명명된 프로바이더 + `authenticate("name") { }` 블록 구조로 동작함 → 인증에 성공하면 Principal 객체가 생성되며 `call.principal<T>()`로 꺼낼 수 있음.

핵심 용어:

- Credential: 자격 정보 (예: 사용자명+비밀번호, API 키, JWT 토큰)
- Principal: 인증된 주체 (User, Service 등)
- Provider: 어떤 방식으로 자격을 검증할지 정의하는 단위

---

### 지원 방식

- `basic`: HTTP Basic (HTTPS와 함께 사용)
- `digest`: HTTP Digest
- `bearer`: Bearer 토큰(커스텀/일반)
- `form`: HTML 폼 로그인
- `jwt`: JWT 토큰 검증
- `oauth`: OAuth1/2 - Google·GitHub 등 외부 IdP
- `session`: 세션 기반(Sessions 플러그인과 함께 사용)
- `ldap`: LDAP 디렉터리

---

### 설치와 기본 패턴

```kotlin
install(Authentication) {
    basic("auth-basic") {
        realm = "Ktor Server"
        validate { creds ->
            if (creds.name == "admin" && creds.password == "secret") {
                UserIdPrincipal(creds.name)
            } else null
        }
    }
}
```

#### 라우트 보호 + Principal 사용

```kotlin
routing {
    authenticate("auth-basic") {
        get("/me") {
            val user = call.principal<UserIdPrincipal>()!!
            call.respondText("Hello ${user.name}")
        }
    }
}
```

여러 프로바이더를 동시에 허용하려면 이름을 여러 개 전달함.

```kotlin
authenticate("auth-basic", "auth-jwt") { ... }
```

---

### 방식별 예제

#### JWT

```kotlin
val jwtRealm    = "ktor sample"
val jwtSecret   = environment.config.property("jwt.secret").getString()
val jwtIssuer   = "https://issuer.example"
val jwtAudience = "ktor-audience"

install(Authentication) {
    jwt("auth-jwt") {
        realm = jwtRealm
        verifier(
            JWT.require(Algorithm.HMAC256(jwtSecret))
                .withIssuer(jwtIssuer)
                .withAudience(jwtAudience)
                .build()
        )
        validate { credential ->
            if (credential.payload.getClaim("uid").asString() != null)
                JWTPrincipal(credential.payload)
            else null
        }
        challenge { _, _ ->
            call.respond(HttpStatusCode.Unauthorized, "Token invalid")
        }
    }
}
```

#### Form

```kotlin
install(Authentication) {
    form("auth-form") {
        userParamName = "username"
        passwordParamName = "password"
        validate { creds ->
            if (loginService.verify(creds.name, creds.password))
                UserIdPrincipal(creds.name)
            else null
        }
        challenge {
            call.respondRedirect("/login")
        }
    }
}
```

#### Session

세션에 저장된 값으로 인증하려면 `Sessions`를 먼저 설치함.

```kotlin
data class UserSession(val name: String) : Principal

install(Authentication) {
    session<UserSession>("auth-session") {
        validate { session -> session }
        challenge {
            call.respondRedirect("/login")
        }
    }
}
```

자세한 세션 사용법은 [10_sessions.md](10_sessions.md).

#### OAuth (Google 예)

```kotlin
install(Authentication) {
    oauth("auth-google") {
        urlProvider = { "https://yourapp.com/callback" }
        providerLookup = {
            OAuthServerSettings.OAuth2ServerSettings(
                name = "google",
                authorizeUrl = "https://accounts.google.com/o/oauth2/auth",
                accessTokenUrl = "https://oauth2.googleapis.com/token",
                requestMethod = HttpMethod.Post,
                clientId = System.getenv("GOOGLE_CLIENT_ID"),
                clientSecret = System.getenv("GOOGLE_CLIENT_SECRET"),
                defaultScopes = listOf("openid", "email"),
            )
        }
        client = HttpClient(CIO)
    }
}
```

---

### challenge

인증 실패 시 클라이언트에 반환할 응답을 정의함 → 미정의 시 `401 Unauthorized`(basic은 `WWW-Authenticate` 헤더 포함)가 기본 동작임.

---

### 자주 쓰는 패턴

- 로그인 라우트는 인증 블록 바깥에 두고, 검증 성공 시 토큰을 발급하거나 세션을 설정함
- 재발급/로그아웃 라우트는 `authenticate {}` 안에 둠
- JWT 키/시크릿은 `application.conf`의 환경변수 보간으로 주입함 (`${JWT_SECRET}`)

---

## 10. 세션 (Sessions)

> 출처: https://ktor.io/docs/server-sessions.html

---

### 개요

`Sessions` 플러그인은 요청 간에 데이터를 유지함 → 결정해야 할 축은 세 가지임.

1. 트랜스포트 — 어떻게 클라이언트와 주고받을 것인가? (`cookie` / `header`)
2. 저장 위치 — 페이로드를 어디에 두는가? (클라이언트 측 / 서버 측)
3. 보호 방식 — 변조 방지(서명) / 노출 방지(암호화)

---

### 데이터 클래스 정의

```kotlin
data class UserSession(val userId: String, val count: Int = 0)
```

세션 객체는 직렬화 가능해야 함 → 기본 직렬화는 자체 포맷을 사용하지만 kotlinx.serialization도 사용 가능함.

---

### 설치

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

`cookie<T>(name)` 대신 `header<T>(name)`을 쓰면 API용으로 헤더를 사용함.

```kotlin
install(Sessions) {
    header<CartSession>("X-Cart")
}
```

---

### 저장 위치

#### 클라이언트 측 (기본)

쿠키/헤더 값에 페이로드를 직접 인코딩하여 클라이언트가 보관함 → 서버는 stateless임.

#### 서버 측

세션 ID만 클라이언트에 두고, 페이로드는 서버 저장소에 보관함.

```kotlin
install(Sessions) {
    cookie<UserSession>("user_session", SessionStorageMemory())
    // 또는:
    cookie<UserSession>("user_session", directorySessionStorage(File("build/.sessions")))
}
```

- `SessionStorageMemory()`: 로컬 개발용, 재시작 시 소실됨
- `directorySessionStorage(File("..."))`: 파일 기반
- `SessionStorage` 인터페이스를 직접 구현하면 Redis·DB 등으로 확장 가능

---

### 변조 방지 / 암호화

#### 서명만 (변조 감지)

```kotlin
cookie<UserSession>("user_session") {
    transform(
        SessionTransportTransformerMessageAuthentication(signKey)
    )
}
```

#### 서명 + 암호화 (값까지 비공개)

```kotlin
cookie<UserSession>("user_session") {
    transform(
        SessionTransportTransformerEncrypt(encryptKey, signKey)
    )
}
```

`signKey`, `encryptKey`는 충분히 긴 무작위 바이트로, 환경변수에서 주입함.

---

### 세션 읽기/쓰기/삭제

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

같은 요청 안에서 같은 타입에 대해 `set`을 다시 호출하면 값이 갱신됨.

---

### 인증과의 결합

Authentication의 `session` 프로바이더와 묶으면 "쿠키 = 로그인 토큰" 구조를 만들 수 있음 → [09_authentication.md](09_authentication.md) 참고.

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

### 운영 팁

- 쿠키에는 항상 `httpOnly = true`, 가능하면 `secure = true`, `SameSite=Lax` 이상 적용
- 서명/암호화 키는 환경변수 또는 시크릿 매니저로 주입
- 페이로드가 커지면 모든 요청에 비용이 발생함 → server-side 저장으로 전환
