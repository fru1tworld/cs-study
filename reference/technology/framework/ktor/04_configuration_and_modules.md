# 04. 설정 파일과 Application 모듈

> 출처:
> - https://ktor.io/docs/server-configuration-file.html
> - https://ktor.io/docs/server-modules.html
>
---

## 설정 포맷 — HOCON vs YAML

Ktor는 두 가지 설정 포맷을 지원합니다. 둘 다 `src/main/resources/` 디렉터리에 위치합니다.

| 항목 | `application.conf` (HOCON) | `application.yaml` (YAML) |
| --- | --- | --- |
| 의존성 | 기본 제공 | `io.ktor:ktor-server-config-yaml` 필요 |
| 환경변수 문법 | `${ENV}` | `${ENV}`, `$ENV` 모두 |
| Maven 빌드 지원 | ✓ | (현 시점 미지원) |
| 스타일 | 중괄호 블록 | 들여쓰기 |

---

## 사전 정의된 키들

### `ktor.deployment.*`

| 키 | 의미 |
| --- | --- |
| `port` | 평문 HTTP 포트 (`0`이면 랜덤) |
| `sslPort` | SSL 포트 |
| `host` | 바인딩 주소 (예: `0.0.0.0`) |
| `watch` | 자동 재로드 대상 경로 목록 |
| `rootPath` | 서블릿 컨텍스트 경로 |
| `shutdownGracePeriod` | 신규 요청 거부 후 대기(ms) |
| `shutdownTimeout` | 셧다운 최대 시간(ms) |
| `connectionGroupSize` | accept 스레드 수 |
| `workerGroupSize` | 이벤트 루프 크기 |
| `callGroupSize` | 핸들러 풀 최소 크기 |

### `ktor.application.*`

| 키 | 의미 |
| --- | --- |
| `modules` | 로드할 모듈 함수의 FQN 목록 (필수) |

### `ktor.security.ssl.*` (SSL 사용 시)

`keyStore`, `keyAlias`, `keyStorePassword`, `privateKeyPassword`, `trustStore`, `trustStorePassword`, `enabledProtocols`.

---

## 예시 — application.yaml

```yaml
ktor:
  deployment:
    port: 8080
    host: 0.0.0.0
  application:
    modules:
      - com.example.ApplicationKt.module
```

## 예시 — application.conf (HOCON)

```hocon
ktor {
    deployment {
        port = 8080
        port = ${?PORT}              # 환경변수 있으면 덮어씀
    }
    application {
        modules = [ com.example.ApplicationKt.module ]
    }
}
```

---

## 환경변수 오버라이드

```hocon
# HOCON
port = ${PORT}      # 필수 — 없으면 오류
port = ${?PORT}     # 선택 — 없으면 다음 값으로 폴백
port = 8080
```

```yaml
# YAML
port: ${PORT}
port: ${PORT:8080}  # 콜론 뒤가 기본값
```

---

## 커맨드라인 오버라이드

`EngineMain` 사용 시 빌드된 jar에 인자를 전달하면 설정값을 덮어쓸 수 있습니다.

```bash
java -jar app.jar -port=9090
java -jar app.jar -config=custom.conf
java -jar app.jar -P:ktor.deployment.callGroupSize=7
```

---

## 코드에서 설정값 읽기

### 단순 접근

```kotlin
fun Application.module() {
    val port = environment.config
        .propertyOrNull("ktor.deployment.port")
        ?.getString() ?: "8080"
}
```

### 타입 안전한 디코딩

```kotlin
@Serializable
data class AppConfig(val security: Security) {
    @Serializable data class Security(val clientId: String)
}

fun Application.module() {
    val config = environment.config.getAs<AppConfig>()
    val clientId = config.security.clientId
}
```

---

## Application 모듈이란

**모듈(Module)** 은 `Application` 클래스의 **확장 함수**로 정의되는 단위입니다. 라우팅, 플러그인 설치, 직렬화 등 한 묶음의 설정을 캡슐화합니다.

```kotlin
fun Application.module() {
    install(ContentNegotiation) { json() }
    configureRouting()
}

fun Application.configureRouting() {
    routing {
        get("/") { call.respondText("OK") }
    }
}
```

모듈로 쪼개는 이유:

- 관심사별 분리 (`configureRouting`, `configureSecurity`, `configureSerialization`)
- 도메인 단위 격리 → 테스트 용이
- 여러 환경에서 다른 모듈 조합으로 부팅 가능

---

## 모듈 등록 — embeddedServer

```kotlin
fun main() {
    embeddedServer(Netty, port = 8080, module = Application::module)
        .start(wait = true)
}
```

여러 모듈을 람다 안에서 호출하는 형태도 흔합니다.

```kotlin
embeddedServer(CIO, port = 8080) {
    val service = MyService()
    routingModule(service)
    schedulingModule(service)
}.start(wait = true)
```

## 모듈 등록 — 설정 파일

```yaml
ktor:
  application:
    modules:
      - com.example.ApplicationKt.module
      - com.example.AdminKt.adminModule
```

> 설정 파일에서 등록할 때는 모듈 함수의 **완전한 정규화 이름**(예: `com.example.ApplicationKt.module`)을 정확히 명시해야 합니다.
