# Ktor 소개와 프로젝트 생성

## Ktor 소개

> 원본 참고: https://ktor.io/docs/

---

### Ktor란

Ktor는 Kotlin과 코루틴을 기반으로 한 비동기 프레임워크로, 연결된 애플리케이션(connected applications) — 즉 HTTP/WebSocket 서버 및 클라이언트 — 을 만들기 위해 JetBrains에서 개발함. 가벼움·비파괴적인 DSL·플러그인 기반 아키텍처가 특징임.

핵심 컨셉:

- embeddedServer / EngineMain: JVM Servlet 컨테이너 없이도 단독 실행 가능
- 엔진 분리: Netty · Jetty · Tomcat · CIO 등 교체 가능한 엔진 위에서 동작
- 모듈(Application 확장 함수): 라우트·설정·플러그인을 모아서 한 단위로 등록
- 플러그인(파이프라인 인터셉터): 직렬화, 인증, 세션, 로깅 등 횡단 관심사를 install 한 번으로 끼워 넣음
- Routing DSL: `routing { get("/x") { ... } }`로 표현되는 선언적 라우트

---

### 그 외 자료

- [Ktor 공식 사이트](https://ktor.io/)
- [Server 문서 홈](https://ktor.io/docs/server-create-a-new-project.html)
- [프로젝트 생성기](https://start.ktor.io/)
- [GitHub 리포지토리](https://github.com/ktorio/ktor)
- [샘플 모음](https://github.com/ktorio/ktor-samples)

---

### 구성

- 01 `01_introduction.md`: 소개 · 인덱스
- 02 `02_create_project.md`: 새 프로젝트 만들기(generator · CLI · IntelliJ)
- 03 `03_engines.md`: 엔진(Netty · Jetty · CIO · Tomcat · Servlet)
- 04 `04_configuration_and_modules.md`: 설정 파일과 Application 모듈
- 05 `05_routing.md`: 라우팅 DSL
- 06 `06_requests_and_responses.md`: 요청 · 응답 처리
- 07 `07_plugins.md`: 플러그인(내장 + 커스텀)
- 08 `08_content_negotiation.md`: Content Negotiation 및 직렬화
- 09 `09_authentication.md`: 인증(Basic · JWT · OAuth · Session)
- 10 `10_sessions.md`: 세션 관리
- 11 `11_websockets.md`: WebSocket
- 12 `12_static_content.md`: 정적 콘텐츠 · 리소스 서빙
- 13 `13_status_pages.md`: StatusPages — 예외/상태 처리
- 14 `14_testing.md`: testApplication 기반 통합 테스트
- 15 `15_deployment.md`: Fat JAR · 컨테이너 · Native 배포

---

## 02. 새 Ktor 프로젝트 만들기

> 출처: https://ktor.io/docs/server-create-a-new-project.html

---

### 새 프로젝트 만드는 세 가지 경로

Ktor 프로젝트는 다음 세 가지 방법 중 하나로 시작 가능.

#### 1) 웹 프로젝트 생성기 (start.ktor.io)

가장 보편적인 방법.

1. https://start.ktor.io/ 접속
2. `Project artifact`(예: `com.example.ktor-sample`) 입력
3. `Configure` 단계에서 다음을 선택
   - Build System: Gradle Kotlin DSL · Groovy · Maven
   - Engine: Netty · Jetty · CIO · Tomcat
   - 설정 포맷: HOCON · YAML
4. 필요한 플러그인을 검색·추가(Routing · Content Negotiation · Authentication 등)
5. `Download`로 zip을 받음

#### 2) IntelliJ IDEA Ultimate 플러그인

Ultimate 전용. `New Project → Ktor`에서 프로젝트 이름, 웹사이트, 엔진, Advanced Settings(빌드 시스템, Ktor 버전)을 설정한 뒤 플러그인을 선택하고 `Create` 실행.

#### 3) Ktor CLI

```bash
# 설치
brew install ktor          # macOS
winget install JetBrains.KtorCLI   # Windows

# 프로젝트 생성
ktor new
```

대화형으로 프로젝트 이름과 플러그인을 입력한 뒤 `Ctrl+G`로 생성함.

---

### 압축 풀고 실행

```bash
unzip ktor-sample.zip -d ktor-sample
cd ktor-sample
chmod +x ./gradlew
./gradlew build
./gradlew run
```

기본 포트는 `8080`. 브라우저에서 http://0.0.0.0:8080 으로 접속하면 `Hello World!`가 나옴.

---

### 기본 프로젝트 구조

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

Generator 기본 산출물은 `configureRouting()`, `configureSerialization()`처럼 관심사별 모듈 함수가 `Application.kt`의 `module()`에서 호출되는 형태임.

---

### 포트 변경

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

### 첫 엔드포인트 추가

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

### 정적 콘텐츠 서빙

```kotlin
fun Application.configureRouting() {
    routing {
        staticResources("/content", "mycontent")
    }
}
```

`src/main/resources/mycontent/sample.html`에 파일을 두면 http://0.0.0.0:9292/content/sample.html 로 접근 가능.

---

### 간단한 통합 테스트

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

`testApplication`은 Ktor가 제공하는 in-memory 테스트 하네스임 → 실제 소켓 없이 핸들러를 직접 호출함. 자세한 내용은 [테스트](7_status_pages_testing_deployment.md#14-테스트-testapplication) 참고.

---

### StatusPages로 에러 처리 등록

```kotlin
install(StatusPages) {
    exception<IllegalStateException> { call, cause ->
        call.respondText("App in illegal state as ${cause.message}")
    }
}
```

자세한 사용법은 [StatusPages](7_status_pages_testing_deployment.md#13-statuspages--예외와-상태-코드-처리) 참고.
