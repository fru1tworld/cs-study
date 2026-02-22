# Arrow-kt 디자인 레시피

> 좋은 Kotlin 코드를 설계하기 위한 레시피

Arrow는 함수형 프로그래밍 스타일로 깔끔하고 타입 안전한 Kotlin 코드를 작성하기 위한 다양한 디자인 패턴을 제공합니다. 이 섹션에서는 도메인 모델링, 이펙트와 컨텍스트, 리시버와 flatMap 비교, 그리고 suspend를 IO 대신 사용하는 이유에 대해 다룹니다.

---

## 목차

1. [도메인 모델링](#1-도메인-모델링)
2. [이펙트와 컨텍스트](#2-이펙트와-컨텍스트)
3. [리시버 vs flatMap](#3-리시버-vs-flatmap)
4. [왜 IO 대신 suspend를 사용하는가](#4-왜-io-대신-suspend를-사용하는가)

---

## 1. 도메인 모델링

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

열거형을 사용하면 무한한 문자열 값 대신 특정 케이스로 복잡성을 줄일 수 있습니다.

### 1.3 고급 합 타입 - Sealed Class

구조적으로 다른 이벤트(온라인 vs 오프라인)를 표현할 때 `sealed class`가 강력한 패턴 매칭을 제공합니다:

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

이 접근 방식의 장점:
- 잘못된 상태 조합을 방지합니다
- 안전한 완전 분기 `when` 표현식을 사용할 수 있습니다

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

이 섹션에서는 이펙트(효과)를 통해 동작을 모델링하는 방법을 탐구하며, 무거운 의존성 주입 프레임워크의 대안을 제시합니다.

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

단점: 코드베이스 전체에 컨텍스트를 수동으로 전달해야 하므로 보일러플레이트와 유지보수 부담이 증가합니다.

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

주입 해결 방법은 복합 객체를 생성해야 합니다:

```kotlin
with(object : Database by this@db, Log by this@stdoutLogger) {
  saveUserInDb(User("Alex"))
}
```

### 2.6 미래 방향: 컨텍스트 매개변수

Kotlin의 새로운 컨텍스트 매개변수 기능이 더 깔끔한 구문을 가능하게 합니다:

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

암시적 매개변수로 작동하여 의존성 주입을 가능하게 합니다. 함수는 필요한 컨텍스트를 선언할 수 있습니다:

```kotlin
context(UserRepository)
suspend fun getUserName(id: UserId): String?
```

여러 컨텍스트가 고정된 순서 없이 매끄럽게 조합됩니다.

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

개발자는 suspend 블록 내에서 명령형 스타일 코드를 작성할 수 있어, 이펙트를 도입할 때 함수를 다시 작성하지 않아도 더 깔끔한 구문을 제공합니다.

특히, Kotlin은 주류 언어에서 고급 개념으로 남아 있는 고차 종류 타입(Higher-Kinded Types)이 필요하지 않습니다.

### 3.4 에러 처리

Arrow 2.0의 `Raise<E>` 스코프는 인체공학적 에러 처리를 가능하게 합니다. 여러 에러 타입이 명시적으로 공존할 수 있습니다:

```kotlin
context(Raise<DbConnectionError>, Raise<MalformedQuery>)
suspend fun queryUsers(q: UserQuery): List<User>
```

### 3.5 제한 사항

#### 단발성 연속(One-shot continuations)

Kotlin의 연속은 한 번만 실행되어 비결정적 이펙트의 직접적인 구현을 방지합니다. 이 트레이드오프는 성능 최적화를 우선시합니다.

#### 고차 종류 타입의 부재

operational 또는 free 모나드 같은 일부 패턴은 코드 생성 없이 추상화할 수 없습니다.

### 3.6 결론

이 스타일은 "Kotlin 개발자에게 관용적"이며, JVM 에코시스템에 적합한 실용적인 성능 특성을 유지하면서 조합성 이점을 제공합니다.

---

## 4. 왜 IO 대신 suspend를 사용하는가

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

suspend 모델은 동일한 안전 속성을 유지하면서 형식적인 부담을 제거합니다.

### 4.2 안전성과 참조 투명성

두 모델 모두 동등한 보장을 제공합니다. 문서에서 언급하듯이, "suspend는 정확히 동일한 속성을 제공하지만 컴파일러 지원으로 네이티브하게" 제공합니다. 둘 다 안전하게 예외를 캡처하고 `Result<A>`(`Either<Throwable, A>`와 동등)로 반환합니다.

### 4.3 이펙트 혼합

Suspend는 계산 블록을 통해 `Either` 같은 다른 이펙트와 매끄럽게 조합할 수 있습니다:

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

결정적으로, "suspend는 IO<A>에 비해 매우 빠릅니다. IO<A>는 런타임에 구축되는 반면 suspend는 컴파일러에 의해 구축되기 때문입니다." Kotlin 컴파일러는 최적화된 상태 머신을 생성하여 래퍼 타입이 필요로 하는 할당을 제거합니다.

### 4.5 비교 요약

| 측면 | IO 모나드 | suspend |
|------|-----------|---------|
| 구문 | flatMap 체이닝 필요 | 직접적인 명령형 스타일 |
| 안전성 | 참조 투명성 보장 | 동등한 보장 |
| 성능 | 런타임 구조 생성 | 컴파일러 최적화 |
| 학습 곡선 | 모나드 개념 이해 필요 | Kotlin 네이티브 기능 |
| 에러 처리 | Either와 조합 | either 블록으로 자연스럽게 조합 |

---

## 참고 자료

- [Arrow 공식 문서](https://arrow-kt.io/)
- [Arrow GitHub 저장소](https://github.com/arrow-kt/arrow)
- [Kotlin 코루틴 문서](https://kotlinlang.org/docs/coroutines-overview.html)
