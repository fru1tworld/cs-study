# Ktor StatusPages, 테스트, 배포

## 13. StatusPages — 예외와 상태 코드 처리

> 출처: https://ktor.io/docs/server-status-pages.html

---

### 개요

`StatusPages` 플러그인은 두 가지를 한 곳에서 처리함.

1. 핸들러에서 던져진 **예외**를 상태 코드/본문으로 매핑.
2. 특정 **HTTP 상태 코드**(예: 404)가 만들어졌을 때 응답 본문을 일관되게 교체.

도메인 예외를 그대로 throw하고, 응답으로의 매핑은 한 곳에 모아두는 것이 핵심 아이디어임.

---

### 의존성과 설치

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

`exception<T>`는 **가장 구체적인 타입부터** 매칭됨. 마지막에 `Throwable` 폴백을 두면 모든 미처리 예외를 잡아낼 수 있음.

---

### 정적 HTML로 에러 페이지 제공

`statusFile`은 정해진 패턴의 HTML을 코드별로 서빙함. 패턴의 `#`이 상태 코드로 치환됨.

```kotlin
install(StatusPages) {
    statusFile(
        HttpStatusCode.NotFound,
        HttpStatusCode.Unauthorized,
        filePattern = "error#.html"
    )
}
```

위 예시는 404 → `error404.html`, 401 → `error401.html`을 응답함 (둘 다 정적 리소스에 있어야 함).

---

### 도메인 예외와 묶기

이런 패턴이 흔함.

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

비즈니스 로직에서는 그냥 `throw AppException.NotFound("user")`만 해도 됨.

---

### 주의할 점

- `StatusPages`에서 응답을 보낸 후 같은 호출에 다시 응답을 시도하면 예외가 발생함.
- `status()` 핸들러는 상태 코드가 명시적으로 설정된 경우에만 동작함 → 라우트 매칭이 실패해서 발생한 `404`도 처리하려면 `status(HttpStatusCode.NotFound)`를 등록해둘 것.
- `CallLogging`을 함께 사용하는 경우, 예외가 잡혀도 로그가 유실되지 않도록 `exception<Throwable>`에서 명시적으로 로그를 남기는 것이 안전함.

---

## 14. 테스트 (testApplication)

> 출처: https://ktor.io/docs/server-testing.html

---

### 핵심 아이디어

Ktor 서버 테스트는 **실제 TCP 소켓을 띄우지 않음.** `testApplication { ... }` 블록 안에서 가짜 엔진 위에 모듈을 올리고, in-memory `HttpClient`로 요청을 보냄 → 빠르고 · 포트 충돌 없고 · 병렬 실행 안전함.

---

### 의존성

```kotlin
testImplementation("io.ktor:ktor-server-test-host:$ktor_version")
testImplementation(kotlin("test"))
```

---

### 기본 구조

```kotlin
class RootTest {
    @Test
    fun `GET root returns OK`() = testApplication {
        application { module() }                 // 실제 모듈을 그대로 부팅
        val res = client.get("/")
        assertEquals(HttpStatusCode.OK, res.status)
        assertEquals("Hello, World!", res.bodyAsText())
    }
}
```

- `application { ... }`: 모듈을 정의하는 자리, 보통 운영 코드의 `Application.module()`을 그대로 호출함.
- `client`: in-memory 클라이언트, `get`, `post`, `setBody`, `headers { ... }` 등을 제공함.

---

### 설정 오버라이드

테스트 전용 설정으로 띄우고 싶을 때:

```kotlin
@Test
fun `custom config`() = testApplication {
    environment {
        config = MapApplicationConfig(
            "ktor.deployment.port" to "0",
            "jwt.secret" to "test-secret",
        )
    }
    application { module() }
    // ...
}
```

`application.conf` / `application.yaml` 자체를 교체하려면 `ApplicationConfig`를 만들어서 넣어주면 됨.

---

### 클라이언트 커스터마이즈

`HttpClient` 기능이 필요하면 `createClient`로 별도 클라이언트를 만들 수 있음.

```kotlin
@Test
fun `posts JSON`() = testApplication {
    application { module() }

    val json = createClient {
        install(ContentNegotiation) { json() }
    }

    val res = json.post("/users") {
        contentType(ContentType.Application.Json)
        setBody(CreateUser(name = "ada"))
    }
    assertEquals(HttpStatusCode.Created, res.status)
}
```

쿠키 기반 세션을 다룰 때:

```kotlin
val cookied = createClient { install(HttpCookies) }
```

---

### 외부 서비스 모킹

`externalServices { hosts(...) { ... } }`로 외부 호스트로의 요청을 가짜 라우팅으로 가로챌 수 있음.

```kotlin
testApplication {
    externalServices {
        hosts("https://issuer.example") {
            routing {
                get("/.well-known/jwks.json") {
                    call.respondText("""{"keys":[]}""")
                }
            }
        }
    }
    application { module() }
}
```

---

### WebSocket 테스트

```kotlin
@Test
fun `ws echo`() = testApplication {
    application { module() }
    val ws = createClient { install(WebSockets) }

    ws.webSocket("/echo") {
        send("hello")
        val reply = (incoming.receive() as Frame.Text).readText()
        assertEquals("ECHO: hello", reply)
    }
}
```

---

### 패턴 메모

- 단위 테스트는 핸들러를 직접 호출하기 어려움 → 보통 `testApplication`을 사용한 **경량 통합 테스트**로 라우트 단위 검증함.
- 도메인 로직은 별도 모듈로 빼서 일반 Kotlin 단위 테스트로 검증하는 편이 빠름.
- 테스트마다 모듈 부팅 비용이 작지만 0은 아님 → 한 클래스 안에서 같은 `testApplication { }` 블록을 공유하는 헬퍼를 만들어 쓰는 패턴도 흔함.

---

## 15. 배포

> 출처:
> - https://ktor.io/docs/server-fatjar.html
> - 일반적인 컨테이너/네이티브 배포 패턴

---

### 배포 옵션 한눈에

- Fat JAR
  - 사용 시점: 가장 단순한 JVM 실행
  - 산출물: `app-all.jar`
- Docker 이미지
  - 사용 시점: 컨테이너 오케스트레이션
  - 산출물: OCI 이미지
- Application 플러그인 tar/zip
  - 사용 시점: 시스템 서비스로 설치
  - 산출물: `bin/` + `lib/` 트리
- WAR (Servlet)
  - 사용 시점: 외부 서블릿 컨테이너 사용
  - 산출물: `app.war`
- GraalVM Native Image
  - 사용 시점: 빠른 부팅, 낮은 메모리
  - 산출물: 네이티브 바이너리

---

### Fat JAR — Ktor Gradle 플러그인

가장 빠른 길은 **Ktor Gradle 플러그인**이 제공하는 `buildFatJar` 태스크임.

```kotlin
// build.gradle.kts
plugins {
    kotlin("jvm") version "2.0.0"
    id("io.ktor.plugin") version "3.5.0"
    application
}

application {
    mainClass.set("com.example.ApplicationKt")
}

ktor {
    fatJar {
        archiveFileName.set("app.jar")
    }
}
```

빌드 & 실행:

```bash
./gradlew buildFatJar
java -jar build/libs/app.jar
```

`runFatJar` 태스크는 빌드 직후 바로 실행해줌.

> Kotlin Multiplatform 플러그인과 함께 사용하면 fatJar가 비활성화됨. JVM 전용 모듈을 별도로 두고 MPP 모듈을 의존성으로 추가하는 것이 정석임.

---

### Shadow 플러그인 (수동 설정)

Ktor 플러그인 없이 직접 fat JAR을 만들고 싶다면 Shadow 플러그인을 사용함.

```kotlin
plugins {
    kotlin("jvm") version "2.0.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    application
}

application { mainClass.set("com.example.ApplicationKt") }

tasks.shadowJar {
    archiveBaseName.set("app")
    archiveClassifier.set("")
    archiveVersion.set("")
}
```

```bash
./gradlew shadowJar
java -jar build/libs/app.jar
```

---

### Docker

흔한 멀티 스테이지 패턴:

```dockerfile
# ---- build ----
FROM gradle:8.10-jdk21 AS build
WORKDIR /src
COPY . .
RUN ./gradlew buildFatJar --no-daemon

# ---- runtime ----
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /src/build/libs/app.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

- 환경변수로 설정을 주입하려면 `application.conf`에 `${?PORT}`처럼 선택적 보간 키를 미리 선언해두는 것이 깔끔함 (`-Dconfig.override.X` 또는 `-P:ktor.deployment.X=...`도 가능).
- 헬스체크 엔드포인트(`get("/healthz") { call.respond(HttpStatusCode.OK) }`)는 거의 항상 둠.

---

### Application 플러그인 (tar/zip)

Gradle 표준 `application` 플러그인이 만드는 `distZip` / `distTar`는 systemd / nssm 같은 시스템 서비스 매니저로 띄울 때 깔끔함.

```bash
./gradlew installDist
./build/install/<project>/bin/<project>
```

---

### 서블릿 컨테이너 (WAR)

`ServletApplicationEngine`을 사용하고 `war` 플러그인을 적용하면 Tomcat/Jetty 같은 외부 컨테이너에 배포할 수 있는 WAR가 생성됨. 사내 표준이 서블릿 컨테이너라면 이 방법을 사용함.

---

### GraalVM Native Image

엔진을 **CIO**로 두고 `org.graalvm.buildtools.native` 플러그인을 적용하면 단일 바이너리로 빌드할 수 있음 → 시작 시간과 메모리가 크게 줄어들지만, 리플렉션을 쓰는 라이브러리(Jackson 등)는 별도 설정 필요.

---

### 운영 체크리스트

- `application.conf`의 secret/host 같은 값은 환경변수로 분리.
- `CallLogging` 또는 별도 액세스 로그를 켜둘 것.
- `StatusPages`에서 `Throwable` 폴백을 등록해 5xx 본문을 일관되게 유지.
- 그레이스풀 셧다운(`shutdownGracePeriod`, `shutdownTimeout`)을 컨테이너 종료 신호와 맞춰서 설정.
- 프록시 뒤라면 `ForwardedHeaders` 또는 `XForwardedHeaders` 설치.
- 메트릭은 `MicrometerMetrics`로 Prometheus 등에 노출.
