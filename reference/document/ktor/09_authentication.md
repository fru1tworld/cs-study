# 09. 인증 (Authentication)

> 출처: https://ktor.io/docs/server-auth.html
> 한국어 학습 노트입니다.

---

## 개념

Ktor의 인증은 **`Authentication` 플러그인 + 명명된 프로바이더 + `authenticate("name") { }` 블록** 구조로 동작합니다. 인증에 성공하면 **Principal** 객체가 만들어져 `call.principal<T>()`로 꺼낼 수 있습니다.

핵심 용어:

- **Credential**: 자격 정보 (예: 사용자명+비밀번호, API 키, JWT 토큰)
- **Principal**: 인증된 주체 (User, Service 등)
- **Provider**: 어떤 방식으로 자격을 검증할지 정의한 단위

---

## 지원 방식

| 방식 | 용도 |
| --- | --- |
| `basic` | HTTP Basic (HTTPS와 함께 사용) |
| `digest` | HTTP Digest |
| `bearer` | Bearer 토큰 (커스텀/일반) |
| `form` | HTML 폼 로그인 |
| `jwt` | JWT 토큰 검증 |
| `oauth` | OAuth1/2 — Google/GitHub 등 외부 IdP |
| `session` | 세션 기반 (Sessions 플러그인과 함께) |
| `ldap` | LDAP 디렉터리 |

---

## 설치와 기본 패턴

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

### 라우트 보호 + Principal 사용

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

여러 프로바이더를 동시에 허용하려면 이름을 여러 개 넘기면 됩니다.

```kotlin
authenticate("auth-basic", "auth-jwt") { ... }
```

---

## 방식별 예제

### JWT

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

### Form

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

### Session

세션에 저장된 값으로 인증하려면 `Sessions`를 먼저 설치한 뒤:

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

### OAuth (Google 예)

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

## challenge

인증 실패 시 클라이언트에게 줄 응답을 정의합니다. 미정의 시 `401 Unauthorized`(basic은 `WWW-Authenticate` 헤더 포함)가 기본 동작입니다.

---

## 자주 쓰는 패턴

- **로그인** 라우트는 인증 블록 바깥에 두고, 검증 성공 시 토큰을 발급 또는 세션을 set.
- **재발급/로그아웃** 라우트는 `authenticate {}` 안에 둔다.
- JWT 키/시크릿은 `application.conf`의 환경변수 보간으로 주입한다 (`${JWT_SECRET}`).
