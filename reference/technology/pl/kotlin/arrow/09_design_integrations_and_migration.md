# 09. 설계, 통합과 마이그레이션

- 확인일: 2026-09-08.
- 코드: 개념 설명용 발췌 예제 포함. 생략한 타입, import, 프로젝트 설정이 있는 예제는 독립 실행 프로그램이 아님.
- 원본 문서와 코드의 라이선스: [Apache-2.0](LICENSE.arrow-website).

[전체 목차](01_introduction_and_setup.md)


## 수록한 공식 문서

- [design](https://arrow-kt.io/learn/design/)
- [design/domain-modeling](https://arrow-kt.io/learn/design/domain-modeling/)
- [design/effects-contexts](https://arrow-kt.io/learn/design/effects-contexts/)
- [design/receivers-flatmap](https://arrow-kt.io/learn/design/receivers-flatmap/)
- [design/suspend-io](https://arrow-kt.io/learn/design/suspend-io/)
- [integrations](https://arrow-kt.io/learn/integrations/)
- [projects](https://arrow-kt.io/learn/projects/)
- [quickstart/compose](https://arrow-kt.io/learn/quickstart/compose/)
- [quickstart/migration](https://arrow-kt.io/learn/quickstart/migration/)
- [quickstart/setup/serialization](https://arrow-kt.io/learn/quickstart/setup/serialization/)

## 1. 도메인 모델링

> 원문: [design/domain-modeling](https://arrow-kt.io/learn/design/domain-modeling/)

함수형 도메인 모델링은 타입 안전성을 높이고 컴파일러를 활용하여 버그를 방지하는 데 중점을 둡니다. Kotlin의 `data class`, `sealed class`, `enum class`, `value class`와 Arrow의 `Either`, `Ior` 같은 타입을 활용합니다.

### 1.1 곱 타입 (Product Types) - 데이터 구성

#### 문제점

원시 타입을 직접 사용하면 필드를 혼동하기 쉽습니다:

```kotlin
data class Event(
  val id: Long,
  val title: String,
  val organizer: String,
  val description: String,
  val date: LocalDate
)
```

위 코드에서 `title`, `organizer`, `description`은 모두 `String` 타입이므로 실수로 값을 바꿔서 넣어도 컴파일러가 감지하지 못합니다.

#### 해결책: Value Class 사용

`value class`를 사용하여 전용 타입을 만들면 런타임 오버헤드 없이 컴파일러 검증을 받을 수 있습니다:

```kotlin
@JvmInline value class EventId(val value: Long)
@JvmInline value class Title(val value: String)
@JvmInline value class Organizer(val value: String)
@JvmInline value class Description(val value: String)

data class Event(
  val id: EventId,
  val title: Title,
  val organizer: Organizer,
  val description: Description,
  val date: LocalDate
)
```

이제 컴파일러가 타입을 검증하므로 `Title`과 `Organizer`를 실수로 바꿔서 사용할 수 없습니다.

### 1.2 합 타입 (Sum Types) - 열거형

#### enum class 사용

가능한 값이 제한된 경우 `enum class`를 사용합니다:

```kotlin
enum class AgeRestriction(val description: String) {
  General("모든 연령 시청가"),
  PG("부모 지도 필요"),
  PG13("13세 이상"),
  Restricted("17세 이상"),
  NC17("성인 전용")
}
```

열거형을 사용하면 임의의 문자열 대신 미리 정한 케이스만 허용할 수 있습니다.

### 1.3 고급 합 타입 - Sealed Class

온라인 이벤트에는 URL이, 오프라인 이벤트에는 주소가 필요합니다. 이렇게 구조가 다른 경우를 `sealed class`의 하위 타입으로 나누면 각 이벤트에 맞는 데이터를 담고 패턴 매칭으로 처리할 수 있습니다:

```kotlin
sealed class Event {
  abstract val id: EventId
  abstract val title: Title
  abstract val organizer: Organizer
  abstract val description: Description
  abstract val date: LocalDate
  abstract val ageRestriction: AgeRestriction

  data class Online(
    override val id: EventId,
    override val title: Title,
    override val organizer: Organizer,
    override val description: Description,
    override val date: LocalDate,
    override val ageRestriction: AgeRestriction,
    val url: Url
  ) : Event()

  data class AtAddress(
    override val id: EventId,
    override val title: Title,
    override val organizer: Organizer,
    override val description: Description,
    override val date: LocalDate,
    override val ageRestriction: AgeRestriction,
    val address: Address
  ) : Event()
}
```

이렇게 나누면 온라인 이벤트에 주소만 있거나 오프라인 이벤트에 URL만 있는 잘못된 조합을 막을 수 있습니다. 위치를 표시할 때도 `when`으로 두 경우를 빠짐없이 처리합니다.

```kotlin
fun Event.locationInfo(): String = when (this) {
  is Event.Online -> "온라인 참여: ${url.value}"
  is Event.AtAddress -> "장소: ${address.value}"
}
```

### 1.4 Arrow의 Either를 활용한 에러 처리

에러 도메인과 성공 도메인을 조합하여 표현합니다:

```kotlin
sealed class Error {
  data class EventNotFound(val id: EventId) : Error()
  data class EventPassed(val event: Event) : Error()
}

interface EventService {
  suspend fun fetchUpcomingEvent(id: EventId): Either<Error, Event>
}
```

`Either`를 사용하면 에러 상태와 성공 상태 간의 "이것 또는 저것" 관계를 명확하게 모델링할 수 있습니다.

### 1.5 도메인 모델링의 장점

- 타입 혼동으로 인한 버그를 제거합니다
- 유효한 상태에 대한 추론을 개선합니다
- 정확한 도메인 표현을 통해 코드 명확성을 높입니다

---

## 2. 이펙트와 컨텍스트

> 원문: [design/effects-contexts](https://arrow-kt.io/learn/design/effects-contexts/)

함수가 필요로 하는 동작을 인터페이스로 정의하고, Kotlin의 수신자를 통해 제공할 수 있습니다. 이 절에서는 이펙트(효과)와 컨텍스트로 이런 의존성을 표현하는 방법을 살펴봅니다.

### 2.1 핵심 문제

함수는 주요 데이터 외에도 부수적인 컨텍스트(데이터베이스 연결, 로거 등)가 필요합니다.

#### 명시적 매개변수의 문제점

전통적인 접근 방식:

```kotlin
suspend fun User.saveInDb(conn: DatabaseConnection, logger: Logger) {
  val result = conn.execute(update)
  if (result.isFailure) logger.log("큰 문제 발생!")
}
```

이 방식에서는 함수를 호출할 때마다 연결과 로거를 매개변수로 전달해야 합니다. 같은 컨텍스트를 여러 단계에 걸쳐 전달하면 반복 코드와 유지보수 부담이 늘어납니다.

### 2.2 해결책: 리시버로서의 의존성

의존성을 인터페이스(이펙트라고 부름)로 정의하고 리시버 타입으로 사용합니다.

#### 인터페이스 정의

```kotlin
interface Database {
  suspend fun <A> execute(q: Query<A>): Result<A>
}
```

#### 리시버로 사용

```kotlin
suspend fun Database.saveUserInDb(user: User) {
  val result = execute<User>(update)
}
```

장점: "Database가 타입의 일부로 나타나지만... Kotlin의 리시버 기능을 사용하여 많은 보일러플레이트를 피합니다."

### 2.3 의존성 주입

구현을 생성하고 `with` 스코프 함수를 통해 제공합니다:

```kotlin
class DatabaseFromConnection(conn: DatabaseConnection) : Database {
  override suspend fun <A> execute(q: Query<A>): Result<A> {
    // 구현
  }
}

suspend fun example() {
  val conn = openDatabaseConnection(connParams)
  with(DatabaseFromConnection(conn)) {
    saveUserInDb(User("Alex"))
  }
}
```

또는 전용 러너 함수를 사용합니다:

```kotlin
suspend fun <A> db(
  params: ConnectionParams,
  f: suspend Database.() -> A
): A {
  val conn = openDatabaseConnection(params)
  return with(DatabaseFromConnection(conn), f)
}
```

### 2.4 suspend 수정자의 중요성

모든 이펙트 함수에 `suspend`를 표시해야 하는 이유는 설명과 실행을 분리하기 때문입니다:

- "이러한 함수는 즉시 실행되지 않습니다"
- 유연성을 유지하면서 조합을 가능하게 합니다
- 구현자가 스레딩을 도입하거나 실행 매개변수를 수정할 수 있습니다
- 사용자가 `runBlocking` 등을 통해 최종 실행을 결정합니다

이 패턴은 이펙트 핸들러 역할을 하는 `runBlocking`의 시그니처와 일치합니다.

### 2.5 다중 의존성

`where` 절을 사용하여 여러 상위 바운드를 정의합니다:

```kotlin
interface Log {
  suspend fun log(message: String): Unit
}

suspend fun <Ctx> Ctx.saveUserInDb(user: User)
  where Ctx : Database, Ctx : Log {
  val result = execute(update)
  if (result.isFailure) log("큰 문제 발생!")
}
```

두 상위 바운드를 만족시키려면 `Database`와 `Log`를 함께 구현하는 객체가 필요합니다. 다음처럼 각각의 구현에 위임하는 복합 객체를 만들어 전달할 수 있습니다:

```kotlin
with(object : Database by this@db, Log by this@stdoutLogger) {
  saveUserInDb(User("Alex"))
}
```

### 2.6 미래 방향: 컨텍스트 매개변수

Kotlin의 컨텍스트 매개변수를 사용하면 필요한 의존성을 더 간결하게 선언할 수 있습니다.

```kotlin
context(db: Database, logger: Logger)
fun User.saveInDb() {
  val result = db.execute(update)
  if (result.isFailure) logger.log("큰 문제 발생!")
}
```

이를 통해 수동 객체 구성 없이 더 간단한 중첩 호출이 가능합니다.

### 2.7 용어 정리

- 컨텍스트(Contexts): 스코프 함수 내에서 사용 가능한 리시버
- 이펙트(Effects): 데이터 조작을 넘어선 계산을 표현하는 함수형 프로그래밍 용어
- 대수(Algebra): 이펙트 연산을 정의하는 인터페이스 (tagless final 패턴과 관련)

### 2.8 주요 장점

- 의존성이 타입에 명시적으로 유지됩니다
- 전통적인 DI의 "숨겨진 계약"을 피합니다
- 어노테이션 마법 대신 언어 기능을 사용합니다
- 컴파일러가 정상성 검사를 수행할 수 있습니다
- 코드 명확성과 유지보수성을 유지합니다

---

## 3. 리시버 vs flatMap

> 원문: [design/receivers-flatmap](https://arrow-kt.io/learn/design/receivers-flatmap/)

Arrow는 Haskell이나 Scala의 전통적인 모나딕 패턴 대신 코루틴과 컨텍스트 리시버를 통한 이펙트 조합성을 강조하는 Kotlin 프로그래밍 스타일을 권장합니다.

### 3.1 핵심 개념

이펙트의 정의: 이펙트는 함수의 "가시적 동작"을 시그니처에 명시적으로 표현합니다. 예외나 I/O 같은 동작을 숨기는 대신 타입이 잠재적 부수 효과를 명확하게 나타내야 합니다.

명시적 시그니처의 예:

```kotlin
context(Raise<WhatsHappeningError>, UserRepository)
suspend fun getUserById(id: UserId): User?
```

### 3.2 두 가지 핵심 Kotlin 기능

#### 코루틴

`suspend`로 표시되며, 세밀한 계산 제어를 가능하게 합니다. 컴파일러가 이를 연속 전달 스타일(CPS)로 변환하여 개발자 개입 없이 정교한 이펙트 관리를 가능하게 합니다.

#### 컨텍스트 리시버

컨텍스트 리시버는 암시적 매개변수로 의존성을 전달합니다. 함수에서 필요한 컨텍스트를 다음처럼 선언할 수 있습니다.

```kotlin
context(UserRepository)
suspend fun getUserName(id: UserId): String?
```

여러 컨텍스트를 고정된 순서 없이 조합할 수 있습니다.

### 3.3 전통적인 모나드 대비 장점

이 접근 방식은 모나딕 "flatMap 피라미드"를 피합니다. 다음과 같은 코드 대신:

```kotlin
doOneThing().flatMap { x ->
  doAnotherThing(x).flatMap { y ->
    doYetAnotherThing(y).flatMap { z ->
      // ...
    }
  }
}
```

suspend 블록에서는 명령형 스타일로 코드를 작성할 수 있습니다. 이펙트를 도입할 때 함수를 다시 작성하지 않아도 되므로 구문이 간결해집니다.

특히, Kotlin은 주류 언어에서 고급 개념으로 남아 있는 고차 종류 타입(Higher-Kinded Types)이 필요하지 않습니다.

### 3.4 에러 처리

`Raise<E>`로 함수에 필요한 오류 처리 문맥을 표현할 수 있습니다. 다음은 원문의 context receiver 방식 예시이며, 현재 context parameter 방식의 API와 설정은 이 문서의 마이그레이션 절을 참고합니다:

```kotlin
context(Raise<DbConnectionError>, Raise<MalformedQuery>)
suspend fun queryUsers(q: UserQuery): List<User>
```

### 3.5 제한 사항

#### 단발성 연속(One-shot continuations)

Kotlin의 연속은 한 번만 실행할 수 있으므로 비결정적 이펙트를 직접 구현하는 데 제약이 있습니다. 이는 성능 최적화를 우선한 설계 선택입니다.

#### 고차 종류 타입의 부재

operational 또는 free 모나드 같은 일부 패턴은 코드 생성 없이 추상화할 수 없습니다.

### 3.6 결론

이 스타일은 "Kotlin 개발자에게 관용적"이며, JVM 에코시스템에 적합한 실용적인 성능 특성을 유지하면서 조합성 이점을 제공합니다.

---

## 4. 왜 IO 대신 suspend를 사용하는가

> 원문: [design/suspend-io](https://arrow-kt.io/learn/design/suspend-io/)

Arrow는 Scala와 Haskell 같은 다른 에코시스템의 `IO` 모나드 패턴 대신 부수 효과 처리를 위해 `suspend` 함수를 사용하는 것을 권장합니다.

### 4.1 인체공학성(Ergonomics)

`suspend`가 더 나은 개발자 경험을 제공합니다.

#### IO 접근 방식 (체이닝 필요)

```kotlin
fun ioProgram(): IO<Triple<Int, Int, Int>> =
  number().flatMap { a ->
    number().flatMap { b ->
      number().map { c ->
        Triple(a, b, c)
      }
    }
  }
```

#### Suspend 접근 방식 (더 직관적)

```kotlin
suspend fun triple(): Triple<Int, Int, Int> =
  Triple(number(), number(), number())
```

suspend 모델은 부수효과가 있는 계산을 일반 함수 호출에 가까운 문법으로 조합합니다. 실제 안전성은 취소, 예외 처리와 리소스 관리 규칙을 어떻게 적용하는지에 달려 있습니다.

IO 방식은 모나드 개념과 해당 구현의 평가·실행 규칙을 이해해야 합니다. suspend 방식은 Kotlin의 기본 기능을 사용하므로 Kotlin 코드에서 익숙한 호출 형태를 유지할 수 있습니다.

### 4.2 안전성과 참조 투명성

원문은 IO와 suspend를 효과를 조합하는 두 방식으로 비교합니다. 다만 `suspend` 표시는 그 자체로 순수성이나 참조 투명성을 보장하지 않으며, 예외를 자동으로 `Result`로 바꾸지도 않습니다. 예외를 값으로 처리하려면 명시적인 오류 처리 연산이 필요하고, 코루틴 취소는 그대로 전파해야 합니다.

### 4.3 이펙트 혼합

Suspend는 계산 블록을 통해 `Either` 같은 다른 이펙트와 조합할 수 있습니다.

```kotlin
suspend fun suspendProgram(): Either<PersistenceError, ProcessedUser> =
  either {
    val user = fetchUser().bind()
    val processed = user.process().bind()
    processed
  }
```

의존성 주입은 확장 함수와 인터페이스 위임을 사용하여 변환기 복잡성을 피합니다.

### 4.4 성능

원문은 컴파일러가 suspend 코드를 상태 머신으로 변환하므로 별도 IO 실행 구조의 일부 비용을 줄일 수 있다는 점을 강조합니다. 모든 IO 구현보다 항상 빠르거나 할당이 모두 사라진다는 보장은 아니며, 실제 성능은 사용한 런타임과 작업에 따라 확인해야 합니다.

<a id="통합-요약표"></a>

## 라이브러리 통합

> 원문: [integrations](https://arrow-kt.io/learn/integrations/)

## 1. 린팅 (Linting)

### Detekt

[Detekt](https://detekt.dev/)는 Arrow 코드 패턴에 맞는 커스텀 규칙 세트를 제공합니다. 이를 정적 분석에 적용하면 코드 스타일을 일관되게 유지하고 잠재적인 문제를 일찍 발견할 수 있습니다.

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

[Hoplite](https://github.com/sksamuel/hoplite)는 여러 소스와 형식, 캐스케이딩 설정을 지원합니다. YAML, JSON, HOCON, Properties 등의 설정 파일을 읽어 Arrow의 `Option`, `Either` 같은 타입으로 직접 디코딩할 수 있습니다.

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

`suspendapp-ktor` 모듈은 Ktor 서버의 `ApplicationEngine`을 Resource로 관리해 그레이스풀 셧다운을 지원합니다. 특히 Kubernetes 같은 컨테이너 환경에서 서버의 종료 절차를 관리할 때 유용합니다.

#### 주요 사용 사례

Kubernetes는 Pod에 그레이스풀 셧다운이 필요하다는 신호를 보내기 위해 `SIGTERM`을 보냅니다. 하지만 종료 신호가 도착한 후에도 Pod가 계속 트래픽을 받을 수 있어 백프레셔 메커니즘이 필요합니다.

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

- preWait
  - 기본값: 30초
  - 설명: 중지를 시작하기 전 대기 시간. Kubernetes가 네트워크를 정리할 시간을 확보합니다.
- grace
  - 기본값: -
  - 설명: 셧다운 전 진행 중인 요청이 완료될 수 있는 기간
- timeout
  - 기본값: -
  - 설명: 강제 셧다운 전 최대 기간

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

## Compose와 UI

> 원문: [Compose and UIs](https://arrow-kt.io/learn/quickstart/compose/)

Compose는 UI를 현재 상태의 함수로 보고 상태 변경을 명시적으로 관리하므로 불변 데이터와 결합하기 좋습니다. 독립적인 데이터 요청은 `parZip`으로 합치고, 실패 정책은 typed errors와 resilience로 표현할 수 있습니다. 상태 모델은 성공과 실패만 필요하면 `Either`, 성공과 경고가 함께 존재하면 `Ior`, 부재까지 표현해야 하면 Quiver의 `Outcome`을 고려합니다.

상태 갱신에서 중첩된 `copy` 호출이 반복된다면 Optics로 수정할 필드에 접근할 수 있습니다. `arrow-optics-compose`의 `updateCopy`와 `inside`를 쓰면 여러 변경을 모아 적용할 수 있습니다. 다만 Optics는 갱신을 표현하는 도구이므로 상태 소유권이나 ViewModel의 수명은 별도로 설계해야 합니다.

## 예제 프로젝트

> 원문: [Example projects](https://arrow-kt.io/learn/projects/)

- [Real World](https://github.com/nomisRev/ktor-arrow-example): Ktor, SQLDelight, Kotest를 사용하는 블로그 서비스 구현.
- [Functional Quiz](https://github.com/TevJ/kfp-quiz): http4k와 Exposed를 사용하는 퀴즈 서비스.
- [MasterMind](https://gist.github.com/jakzal/3f0ee968fe8073ee81e328ecbd59fe8b): 함수형 이벤트 소싱으로 게임 상태를 구성하는 예제.
- [Weather App](https://github.com/serras/WeatherApp): Compose Multiplatform Desktop, Ktor Client, Kotest, Turbine을 사용하는 날씨 앱.
- GitHub Alerts: GitHub 이벤트 구독을 Slack 메시지로 전달하는 마이크로서비스 예제. Kafka, kotlin-kafka, Avro4k 사용.
  - [Ktor 구현](https://github.com/47deg/gh-alerts-subscriptions-kotlin): Cohort, Tegral, SQLDelight 연동.
  - [Spring 구현](https://github.com/xebia-functional/gh-alerts-subscriptions-kotlin-spring): 같은 문제를 Spring 기반으로 구성.

- 위 목록은 공식 문서에 등록된 예제이며, 각 저장소를 이 컬렉션에 복제하거나 실행한 것은 아님.

## 직렬화 설정 보완

> 원문: [Serialization](https://arrow-kt.io/learn/quickstart/setup/serialization/)

- `arrow-core`는 직렬화 프레임워크에 직접 의존하지 않으므로 별도 통합 모듈 필요.
- kotlinx.serialization에서는 `arrow-core-serialization` 버전을 `arrow-core`와 맞춤.
- 필드에 사용하는 타입에 맞춰 파일 최상단의 `@file:UseSerializers`에 serializer를 등록함.
- 런타임 contextual 해석이 필요한 경우 `ArrowModule`을 `serializersModule`에 등록하거나 다른 모듈과 합침.
- Jackson 3.x는 `arrow-core-jackson`과 `addArrowModule`, Jackson 2.x는 `arrow-core-jackson2`와 `registerArrowModule` 사용.
- Jackson의 제네릭 value class 처리 때문에 비어 있지 않은 컬렉션의 요소 타입을 잃는 경우 `@field:JsonDeserialize(contentAs = Inner::class)`로 요소 타입을 명시함.

## Arrow 1.x에서 2.x로 마이그레이션

> 원문: [Migration to Arrow 2.0 / 1.2](https://arrow-kt.io/learn/quickstart/migration/)

### 진행 순서

- 기존 1.x 프로젝트는 먼저 1.2 계열의 deprecation 안내를 적용해 제거 예정 API를 줄임.
- 공식 안내가 보장하려는 것은 소스 호환성임. 바이너리 호환성을 가정해 이전 버전으로 컴파일한 모듈을 그대로 섞지 않음.
- `arrow.core.continuations.either`, `arrow.core.computations.either`를 `arrow.core.raise.either`로 변경함.
- `either.eager { ... }`를 `either { ... }`로 변경함.
- `EffectScope`, `EagerEffectScope`는 `Raise` 기반 API로 이전함.
- `Effect`, `EagerEffect`는 `arrow.core.raise`의 해당 타입을 사용하고 `fold`, `ensure`, 오류 처리 확장 함수의 import를 확인함.
- IDE의 ReplaceWith와 원문에 연결된 마이그레이션 스크립트는 수정 보조 수단임. 적용 후 컴파일과 동작 검증 필요.

### traverse와 zip

오류가 발생할 수 있는 원소를 순차 처리할 때는 `traverse` 대신 `either` 안에서 `map`과 `bind`를 사용할 수 있습니다. 순차적인 결과 결합에 쓰던 `zip`도 각 결과를 `bind`한 뒤 일반 함수 호출로 결합합니다.

이때 기존 연산의 실행 방식을 유지해야 합니다. 병렬 실행이 목적이라면 `parZip`이나 `parMap`을 사용하고, 오류를 누적하던 코드라면 별도 누적 연산을 선택합니다. 누적 연산을 첫 오류에서 중단하는 `bind`로 바꾸면 동작이 달라집니다.

```kotlin
import arrow.core.Either
import arrow.core.raise.either

fun parseNumber(raw: String): Either<String, Int> =
    raw.toIntOrNull()?.let { Either.Right(it) }
        ?: Either.Left("정수가 아님: $raw")

fun parseNumbers(raw: List<String>): Either<String, List<Int>> = either {
    raw.map { parseNumber(it).bind() }
}
```

### Validated와 오류 누적

- `Validated`는 `Either`, `ValidatedNel`은 `EitherNel`로 이전함.
- 누적하던 `zip`은 `zipOrAccumulate`, `traverse`는 `mapOrAccumulate`로 먼저 바꿔 동작을 유지함.
- `Validated.Valid`와 `valid()`는 `Either.Right`와 `right()`에 대응함.
- `Validated.Invalid`와 `invalid()`는 `Either.Left`와 `left()`에 대응함.
- 여러 실패가 있는 입력을 검증해 첫 번째 실패만 남는 회귀가 없는지 확인함.

### 모듈, Optics와 context parameter

- `Eval`과 함수 유틸리티는 각각 `arrow-eval`, `arrow-functions` 모듈 의존성 확인 필요.
- Optics의 타입 계층 단순화와 nullable 필드 접근 방식 변경은 07장과 [2.0 공지](https://arrow-kt.io/community/blog/2024/12/05/arrow-2-0/) 참고.
- 2.2 계열의 `arrow.core.raise.context`는 확장 수신자 방식에서 선택적으로 옮겨 갈 수 있는 별도 API임.
- 2.2.2에는 context 기반 리소스 API와 `arrow-raise-ktor-server-resources` 통합도 추가됨. [릴리스 설명](https://arrow-kt.io/community/blog/2026/03/04/arrow-2-2-2/) 참고.
- 2.2.3은 Jackson 직렬화 수정과 공통 atomics 기반 구현 등을 포함함. 설치 버전을 정할 때 [릴리스 설명](https://arrow-kt.io/community/blog/2026/06/04/arrow-2-2-3/)과 모듈별 의존성을 함께 확인함.
