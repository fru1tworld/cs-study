# Arrow-kt 통합 (Integrations)

Arrow는 Kotlin 에코시스템의 다양한 라이브러리와 통합됩니다. 이 문서에서는 린팅, 테스트, 직렬화, 설정, 유효성 검사, 캐싱, HTTP, Ktor 등의 카테고리별 통합 방법을 설명합니다.

---

## 목차

1. [린팅 (Linting)](#1-린팅-linting)
2. [테스트 (Testing)](#2-테스트-testing)
3. [직렬화 (Serialization)](#3-직렬화-serialization)
4. [설정 (Configuration)](#4-설정-configuration)
5. [유효성 검사 및 에러 (Validation and Errors)](#5-유효성-검사-및-에러-validation-and-errors)
6. [캐싱 (Caching)](#6-캐싱-caching)
7. [HTTP](#7-http)
8. [Ktor](#8-ktor)

---

## 1. 린팅 (Linting)

### Detekt

[Detekt](https://detekt.dev/)는 Arrow를 사용할 때 코드 스타일을 개선하기 위한 커스텀 규칙 세트를 제공합니다.

Detekt를 사용하면 Arrow 코드 패턴에 맞는 정적 분석 규칙을 적용하여 일관된 코딩 스타일을 유지하고 잠재적인 문제를 조기에 발견할 수 있습니다.

---

## 2. 테스트 (Testing)

### Kotest

[Kotest](https://kotest.io/)는 Arrow 타입에 대한 매처(matchers)와 속성 기반 테스트 생성기를 제공합니다.

주요 기능:
- `Either`, `Option` 등 Arrow 타입에 대한 매처
- 속성 기반 테스트를 위한 생성기(generators)

### AssertJ

[AssertJ](https://assertj.github.io/doc/)는 서드파티 라이브러리를 통해 `Either`와 `Option`에 대한 어설션(assertions)을 제공합니다.

---

## 3. 직렬화 (Serialization)

Arrow Core 타입(`Either`, `NonEmptyList` 등)은 최소한의 라이브러리 크기를 유지하기 위해 내장 직렬화 의존성을 포함하지 않으므로 특별한 처리가 필요합니다.

### 3.1 kotlinx.serialization

kotlinx.serialization을 사용하려면 Arrow Core 버전과 일치하는 `arrow-core-serialization` 의존성을 추가합니다.

#### 컴파일 타임 설정

`@UseSerializers` 어노테이션으로 직렬화가 필요한 Arrow 타입을 선언합니다:

```kotlin
@file:UseSerializers(
  EitherSerializer::class,
  IorSerializer::class,
  OptionSerializer::class,
  NonEmptyListSerializer::class,
  NonEmptySetSerializer::class
)

@Serializable
data class Book(val title: String, val authors: NonEmptyList<String>)
```

실제로 사용하는 타입에 대한 직렬화기만 포함하면 됩니다. 누락된 것이 있으면 플러그인이 알려줍니다.

#### 런타임 설정

또는 컨텍스트 직렬화 지원을 등록할 수 있습니다:

```kotlin
val format = Json { serializersModule = ArrowModule }

// 또는 기존 모듈과 병합
val format = Json { serializersModule = myModule + ArrowModule }
```

이를 통해 명시적인 직렬화기 선언 없이 직접 직렬화가 가능합니다:

```kotlin
format.encodeToString(nonEmptyListOf("hello", "world"))
```

> 참고: Arrow Core 타입을 포함하는 필드의 컴파일 타임 직렬화가 런타임 해석을 위해 `@Contextual`로 필드에 어노테이션을 다는 것보다 일반적으로 더 선호됩니다.

### 3.2 Jackson

Jackson 지원은 버전에 따라 다릅니다.

#### Jackson 3.x

`arrow-core-jackson` 의존성을 추가한 후 `addArrowModule()`을 사용합니다:

```kotlin
val mapper = JsonMapper.builder()
    .addModule(kotlinModule())
    .addArrowModule()
    .build()
```

#### Jackson 2.x

`arrow-core-jackson2` 의존성을 추가한 후 `registerArrowModule()`을 호출합니다:

```kotlin
val mapper = ObjectMapper()
    .registerKotlinModule()
    .registerArrowModule()
```

---

## 4. 설정 (Configuration)

### Hoplite

[Hoplite](https://github.com/sksamuel/hoplite)는 다양한 소스, 형식, 그리고 캐스케이딩 설정을 지원하며 Arrow 타입 디코딩을 지원합니다.

Hoplite를 사용하면 YAML, JSON, HOCON, Properties 등 다양한 형식의 설정 파일을 읽고 Arrow의 `Option`, `Either` 등의 타입으로 직접 디코딩할 수 있습니다.

---

## 5. 유효성 검사 및 에러 (Validation and Errors)

### Akkurate

[Akkurate](https://akkurate.dev/)는 Arrow의 타입드 에러 메커니즘과 통합되어 복잡한 유효성 검사 언어를 제공합니다.

Arrow의 `Raise` 프레임워크와 함께 사용하면 유효성 검사 결과를 타입 안전하게 처리할 수 있습니다.

### Result4k

[Result4k](https://github.com/fork-handles/forkhandles/tree/trunk/result4k)는 Arrow의 `Raise` 프레임워크 내에서 Result4k를 지원합니다.

기존에 Result4k를 사용하는 코드베이스에서 Arrow의 이펙트 시스템으로 점진적으로 마이그레이션하거나 상호 운용할 수 있습니다.

---

## 6. 캐싱 (Caching)

### cache4k

[cache4k](https://github.com/ReactiveCircus/cache4k)는 메모이제이션(memoization) 캐싱 메커니즘으로 통합됩니다.

Arrow와 함께 사용하면 비용이 많이 드는 계산 결과를 효율적으로 캐싱하여 성능을 최적화할 수 있습니다.

---

## 7. HTTP

### Retrofit

[Retrofit](https://square.github.io/retrofit/)은 HTTP 서비스 쿼리를 위한 통합 모듈을 제공합니다.

Arrow의 `Either` 타입을 Retrofit 응답과 함께 사용하여 API 호출의 성공과 실패를 타입 안전하게 처리할 수 있습니다.

### kJWT

[kJWT](https://github.com/nicofank/kjwt)는 JSON 웹 서명(JWS)과 JSON 웹 토큰(JWT) 지원을 추가합니다.

인증 및 권한 부여 시나리오에서 Arrow 타입과 함께 JWT를 안전하게 처리할 수 있습니다.

---

## 8. Ktor

Ktor는 Arrow와 여러 방면에서 통합됩니다.

### 8.1 그레이스풀 셧다운 (Graceful Shutdown)

`suspendapp-ktor` 모듈은 Ktor 서버의 `ApplicationEngine`을 Resource로 승격시켜 그레이스풀 셧다운을 가능하게 합니다. 이는 특히 Kubernetes 같은 컨테이너화된 환경에서 가치가 있습니다.

#### 주요 사용 사례

Kubernetes는 Pod에 그레이스풀 셧다운이 필요하다는 신호를 보내기 위해 `SIGTERM`을 보냅니다. 하지만 종료 신호가 도착한 후에도 Pod이 계속 트래픽을 받을 수 있어 백프레셔 메커니즘이 필요합니다.

#### 기본 구현

이 모듈은 자동 리로드를 지원하는 `server` 생성자를 제공합니다:

```kotlin
fun main() = SuspendApp {
  resourceScope {
    server(Netty) {
      routing {
        get("/ping") {
          call.respond("pong")
        }
      }
    }
    awaitCancellation()
  }
}
```

#### 설정 매개변수

세 가지 셧다운 타이밍 매개변수를 사용할 수 있습니다:

| 매개변수 | 기본값 | 설명 |
|---------|--------|------|
| preWait | 30초 | 중지 시작 전 지연 시간. Kubernetes 네트워크 관리를 허용합니다. |
| grace | - | 셧다운 전 진행 중인 요청이 완료될 수 있는 기간 |
| timeout | - | 강제 셧다운 전 최대 기간 |

#### 중요 고려사항

Ktor의 내장 훅: Ktor는 설정된 `preWait` 기간을 우회하는 기본 셧다운 훅 처리를 포함합니다. JVM에서 이를 비활성화하려면 `io.ktor.server.engine.ShutdownHook` 시스템 속성을 `false`로 설정하세요.

개발 모드: Ktor가 개발 모드에서 작동할 때 `preWait` 기간은 무시됩니다.

### 8.2 직렬화 - kotlinx.serialization

Ktor와 kotlinx.serialization을 함께 사용할 때 `ArrowModule`을 사용합니다:

```kotlin
install(ContentNegotiation) {
  json(Json {
    serializersModule = ArrowModule
  })
}
```

이를 통해 `Either`, `Option`, `NonEmptyList` 등의 Arrow 타입을 HTTP 요청/응답에서 자동으로 직렬화/역직렬화할 수 있습니다.

### 8.3 직렬화 - Jackson

Jackson 매퍼 설정에 Arrow 등록을 추가합니다:

```kotlin
install(ContentNegotiation) {
  jackson {
    registerKotlinModule()
    registerArrowModule()
  }
}
```

### 8.4 회복탄력성 플러그인 (Resilience Plugins)

Arrow의 회복탄력성 기능을 Ktor 플러그인으로 사용할 수 있습니다:

- 재시도/반복 (Retry/Repeat): 실패한 요청을 자동으로 재시도
- 서킷 브레이커 (Circuit Breakers): 연쇄 실패를 방지하기 위한 회로 차단기

```kotlin
// 예시: 재시도 플러그인 설정
val schedule = Schedule.recurs<Throwable>(5)
  .and(Schedule.exponential(250.milliseconds))

retry(schedule) {
  // 실패할 수 있는 작업
  httpClient.get("https://api.example.com/data")
}
```

---

## 통합 요약표

| 카테고리 | 라이브러리 | 용도 |
|---------|-----------|------|
| 린팅 | Detekt | Arrow 코드 스타일 규칙 |
| 테스트 | Kotest | 매처 및 생성기 |
| 테스트 | AssertJ | Either/Option 어설션 |
| 직렬화 | kotlinx.serialization | JSON 직렬화 |
| 직렬화 | Jackson | JSON 직렬화 |
| 설정 | Hoplite | 설정 파일 디코딩 |
| 유효성 검사 | Akkurate | 타입드 에러 유효성 검사 |
| 유효성 검사 | Result4k | Raise 프레임워크 호환 |
| 캐싱 | cache4k | 메모이제이션 |
| HTTP | Retrofit | HTTP 클라이언트 통합 |
| HTTP | kJWT | JWT 지원 |
| 웹 프레임워크 | Ktor | 그레이스풀 셧다운, 직렬화, 회복탄력성 |

---

## 참고 자료

- [Arrow 공식 문서](https://arrow-kt.io/)
- [Arrow GitHub 저장소](https://github.com/arrow-kt/arrow)
- [Detekt 공식 사이트](https://detekt.dev/)
- [Kotest 공식 사이트](https://kotest.io/)
- [AssertJ 문서](https://assertj.github.io/doc/)
- [Hoplite GitHub](https://github.com/sksamuel/hoplite)
- [Akkurate 공식 사이트](https://akkurate.dev/)
- [cache4k GitHub](https://github.com/ReactiveCircus/cache4k)
- [Retrofit 공식 사이트](https://square.github.io/retrofit/)
- [Ktor 공식 사이트](https://ktor.io/)
