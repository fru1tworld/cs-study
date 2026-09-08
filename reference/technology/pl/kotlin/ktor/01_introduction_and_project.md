# Ktor 소개와 프로젝트 생성

## Ktor 소개

> 원본 참고: https://ktor.io/docs/

### Ktor란

Ktor는 JetBrains가 HTTP/WebSocket 서버와 클라이언트처럼 서로 통신하는 애플리케이션(connected applications)을 만들기 위해 개발한 비동기 프레임워크다. Kotlin과 코루틴을 기반으로 하며, 가벼운 구성과 기존 코드에 쉽게 적용하는 DSL, 플러그인 기반 아키텍처가 특징이다.

주요 구성 요소는 다음과 같다.

- embeddedServer / EngineMain: JVM Servlet 컨테이너 없이도 단독 실행 가능
- 엔진 분리: Netty, Jetty, Tomcat, CIO 등 교체 가능한 엔진 위에서 동작
- 모듈(Application 확장 함수): 라우트, 설정, 플러그인을 모아서 한 단위로 등록
- 플러그인(파이프라인 인터셉터): 직렬화, 인증, 세션, 로깅 등 횡단 관심사를 install 한 번으로 끼워 넣음
- Routing DSL: `routing { get("/x") { ... } }`로 표현되는 선언적 라우트

### 그 외 자료

- [Ktor 공식 사이트](https://ktor.io/)
- [Server 문서 홈](https://ktor.io/docs/server-create-a-new-project.html)
- [프로젝트 생성기](https://start.ktor.io/)
- [GitHub 리포지토리](https://github.com/ktorio/ktor)
- [샘플 모음](https://github.com/ktorio/ktor-samples)

## 02. 새 Ktor 프로젝트 만들기

> 출처: https://ktor.io/docs/server-create-a-new-project.html

### 새 프로젝트 만드는 세 가지 경로

Ktor 프로젝트는 웹 생성기, IntelliJ IDEA 플러그인, CLI 중 하나로 시작할 수 있다.

#### 1) 웹 프로젝트 생성기 (start.ktor.io)

가장 보편적인 방법은 웹 생성기에서 필요한 설정과 플러그인을 선택하는 것이다.

- 1\. https://start.ktor.io/ 접속
- 2\. `Project artifact`(예: `com.example.ktor-sample`) 입력
- 3\. `Configure` 단계에서 다음을 선택
  - Build System: Gradle Kotlin DSL, Groovy, Maven
  - Engine: Netty, Jetty, CIO, Tomcat
  - 설정 포맷: HOCON, YAML
- 4\. 필요한 플러그인을 검색, 추가(Routing, Content Negotiation, Authentication 등)
- 5\. `Download`로 zip을 받음

#### 2) IntelliJ IDEA Ultimate 플러그인

IntelliJ IDEA의 프로젝트 생성 플러그인은 Ultimate에서 제공한다. `New Project → Ktor`에서 프로젝트 이름, 웹사이트, 엔진과 Advanced Settings(빌드 시스템, Ktor 버전)를 설정한 뒤 플러그인을 선택하고 `Create`를 실행한다.

#### 3) Ktor CLI

```bash
# 설치
brew install ktor          # macOS
winget install JetBrains.KtorCLI   # Windows

# 프로젝트 생성
ktor new
```

`ktor new`를 실행하면 프로젝트 이름과 플러그인을 대화형으로 입력할 수 있다. 입력을 마친 뒤 `Ctrl+G`를 누르면 프로젝트를 생성한다.

### 압축 풀고 실행

```bash
unzip ktor-sample.zip -d ktor-sample
cd ktor-sample
chmod +x ./gradlew
./gradlew build
./gradlew run
```

기본 포트는 `8080`이다. 서버를 실행한 뒤 브라우저에서 http://0.0.0.0:8080 으로 접속하면 `Hello World!`를 확인할 수 있다.

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

생성된 프로젝트는 라우팅과 직렬화 같은 관심사를 `configureRouting()`, `configureSerialization()` 등의 함수로 나눈다. `Application.kt`의 `module()`이 이 함수들을 호출해 애플리케이션을 구성한다.

### 포트 변경

포트는 설정 파일에서 변경할 수 있다.

```yaml
ktor:
  deployment:
    port: 9292
```

`embeddedServer`를 사용한다면 코드에서 포트를 지정할 수도 있다.

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

### 첫 엔드포인트 추가

`Routing.kt`의 `configureRouting()`에 경로와 응답을 추가한다.

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

### 정적 콘텐츠 서빙

```kotlin
fun Application.configureRouting() {
    routing {
        staticResources("/content", "mycontent")
    }
}
```

- `src/main/resources/mycontent/sample.html`에 파일을 두면 http://0.0.0.0:9292/content/sample.html 로 접근 가능

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

`testApplication`은 Ktor의 in-memory 테스트 환경으로, 실제 소켓을 열지 않고 핸들러를 호출한다. 자세한 내용은 [테스트](07_status_pages_testing_deployment.md#14-테스트-testapplication)를 참고한다.

### StatusPages로 에러 처리 등록

```kotlin
install(StatusPages) {
    exception<IllegalStateException> { call, cause ->
        call.respondText("App in illegal state as ${cause.message}")
    }
}
```

- 자세한 사용법은 [StatusPages](07_status_pages_testing_deployment.md#13-statuspages--예외와-상태-코드-처리) 참고
