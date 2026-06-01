# 02. 새 Ktor 프로젝트 만들기

> 출처: https://ktor.io/docs/server-create-a-new-project.html
> 내용을 정리·요약한 한국어 학습 노트입니다.

---

## 새 프로젝트 만드는 세 가지 경로

Ktor 프로젝트는 다음 세 가지 방법 중 하나로 시작할 수 있습니다.

### 1) 웹 프로젝트 생성기 (start.ktor.io)

가장 보편적인 방법.

1. https://start.ktor.io/ 접속
2. `Project artifact` (예: `com.example.ktor-sample`) 입력
3. `Configure`로 Build System (Gradle Kotlin DSL / Groovy / Maven), Engine (Netty / Jetty / CIO / Tomcat), 설정 포맷 (HOCON / YAML) 선택
4. 필요한 플러그인을 검색·추가 (Routing / Content Negotiation / Authentication 등)
5. `Download`로 zip을 받음

### 2) IntelliJ IDEA Ultimate 플러그인

Ultimate 한정. `New Project → Ktor`에서 프로젝트 이름, 웹사이트, 엔진, Advanced Settings(빌드 시스템, Ktor 버전)을 설정 후 플러그인을 골라 `Create`.

### 3) Ktor CLI

```bash
# 설치
brew install ktor          # macOS
winget install JetBrains.KtorCLI   # Windows

# 프로젝트 생성
ktor new
```

대화형으로 프로젝트 이름·플러그인을 입력하고 `Ctrl+G`로 생성.

---

## 압축 풀고 실행

```bash
unzip ktor-sample.zip -d ktor-sample
cd ktor-sample
chmod +x ./gradlew
./gradlew build
./gradlew run
```

기본 포트는 `8080`. 브라우저에서 http://0.0.0.0:8080 으로 접속하면 `Hello World!`가 나옵니다.

---

## 기본 프로젝트 구조

```
ktor-sample/
├── src/main/kotlin/
│   ├── Application.kt         # 엔트리 포인트와 모듈 정의
│   └── Routing.kt             # configureRouting() 등 모듈별 분리 파일
├── src/main/resources/
│   ├── application.yaml       # (또는 application.conf) 서버 설정
│   └── logback.xml            # 로깅 설정
├── src/test/kotlin/
├── build.gradle.kts
└── settings.gradle.kts
```

Generator 기본 산출물은 `configureRouting()`, `configureSerialization()`처럼 **관심사별 모듈 함수**가 `Application.kt`의 `module()`에서 호출되는 형태입니다.

---

## 포트 변경

설정 파일에서:

```yaml
ktor:
  deployment:
    port: 9292
```

또는 코드에서 (embeddedServer 사용 시):

```kotlin
fun main() {
    embeddedServer(
        factory = io.ktor.server.netty.Netty,
        port = 9292,
        host = "0.0.0.0",
        module = Application::module,
    ).start(wait = true)
}
```

---

## 첫 엔드포인트 추가

`Routing.kt`:

```kotlin
fun Application.configureRouting() {
    routing {
        get("/test1") {
            call.respondText(
                "<h1>Hello From Ktor</h1>",
                ContentType.parse("text/html"),
            )
        }
    }
}
```

---

## 정적 콘텐츠 서빙

```kotlin
fun Application.configureRouting() {
    routing {
        staticResources("/content", "mycontent")
    }
}
```

`src/main/resources/mycontent/sample.html`에 파일을 두면 http://0.0.0.0:9292/content/sample.html 로 접근됩니다.

---

## 간단한 통합 테스트

```kotlin
class ServerTest {
    @Test
    fun `root endpoint`() = testApplication {
        application { module() }
        val response = client.get("/")
        assertEquals(HttpStatusCode.OK, response.status)
    }
}
```

`testApplication`은 Ktor가 제공하는 in-memory 테스트 하네스로, 실제 소켓을 띄우지 않고 핸들러를 호출합니다. 자세한 내용은 [14_testing.md](14_testing.md).

---

## StatusPages로 에러 처리 등록

```kotlin
install(StatusPages) {
    exception<IllegalStateException> { call, cause ->
        call.respondText("App in illegal state as ${cause.message}")
    }
}
```

자세한 사용법은 [13_status_pages.md](13_status_pages.md).
