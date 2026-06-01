# 14. 테스트 (testApplication)

> 출처: https://ktor.io/docs/server-testing.html
> 한국어 학습 노트입니다.

---

## 핵심 아이디어

Ktor 서버 테스트는 **실제 TCP 소켓을 띄우지 않습니다.** `testApplication { ... }` 블록 안에서 가짜 엔진 위에 모듈을 올리고, in-memory `HttpClient`로 요청을 보냅니다. 그래서 빠르고, 포트 충돌이 없고, 병렬 실행이 안전합니다.

---

## 의존성

```kotlin
testImplementation("io.ktor:ktor-server-test-host:$ktor_version")
testImplementation(kotlin("test"))
```

---

## 기본 구조

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

- `application { ... }`: 모듈을 정의하는 자리. 보통 운영 코드의 `Application.module()`을 그대로 호출.
- `client`: in-memory 클라이언트. `get`, `post`, `setBody`, `headers { ... }` 등을 제공.

---

## 설정 오버라이드

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

`application.conf` / `application.yaml` 자체를 갈아끼우려면 `ApplicationConfig`를 만들어서 넣어주면 됩니다.

---

## 클라이언트 커스터마이즈

`HttpClient` 기능이 필요하면 `createClient`로 별도 클라이언트를 만들 수 있습니다.

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

## 외부 서비스 모킹

`externalServices { hosts(...) { ... } }`로 외부 호스트로의 호출을 가짜 라우팅으로 받아낼 수 있습니다.

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

## WebSocket 테스트

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

## 패턴 메모

- 단위 테스트는 핸들러를 직접 호출하기 어려우므로, 보통 `testApplication`을 사용한 **얇은 통합 테스트**로 라우트 단위 검증을 합니다.
- 도메인 로직은 별도 모듈로 빼서 일반 Kotlin 단위 테스트로 검증하는 편이 빠릅니다.
- 테스트마다 모듈 부팅 비용이 작지만 0은 아니므로, 한 클래스 안에서 같은 `testApplication { }` 블록을 공유하는 헬퍼를 만들어 쓰는 패턴도 흔합니다.
