# Arrow-kt 타입화된 에러 (Typed Errors) 완벽 가이드

> 이 문서는 Arrow-kt의 Typed Errors 섹션 전체를 한국어로 번역한 것입니다.

## 목차

1. [개요](#개요)
2. [타입화된 에러와 함께 작업하기](#타입화된-에러와-함께-작업하기)
3. [유효성 검사](#유효성-검사)
4. [래퍼 타입](#래퍼-타입)
   - [Either와 Ior](#either와-ior)
   - [Nullable과 Option](#nullable과-option)
   - [Outcome과 Progress](#outcome과-progress)
   - [사용자 정의 에러 타입](#사용자-정의-에러-타입)
5. [Either에서 Raise로](#either에서-raise로)

---

## 개요

Arrow는 타입화된 에러를 "함수형 프로그래밍에서 코드 실행 중 발생할 수 있는 잠재적 에러를 시그니처(또는 타입)에서 명시적으로 만드는 기법"으로 정의합니다.

### 두 가지 접근 방식

Arrow는 타입화된 에러를 처리하기 위한 두 가지 상호보완적인 접근 방식을 제공합니다:

#### 1. Raise DSL

Raise DSL은 지정된 타입의 에러가 발생할 수 있는 컨텍스트를 나타내는 확장 수신자를 사용합니다. 이 방식은 일반적으로 더 관용적인 Kotlin 코드를 생성합니다.

```kotlin
fun Raise<UserNotFound>.findUser(id: UserId): User
```

#### 2. 래퍼 타입

`Either`, `Option`, `Result`와 같은 래퍼 타입을 사용하여 계산이 반환 타입에 명시된 논리적 에러로 종료될 수 있음을 나타냅니다.

```kotlin
fun findUser(id: UserId): Either<UserNotFound, User>
```

### 주요 특징

- 일관된 API: Arrow는 두 스타일 모두에서 균일한 API를 제공합니다
- 간편한 변환: 두 접근 방식 간의 간단한 변환 경로를 제공합니다
- 타입화된 방식의 에러 작업, 복구, 누적 지원
- 유효성 검사 모델링 기능

### 의존성

타입화된 에러 기능은 `arrow-core` 라이브러리에 있으며, `zipOrAccumulate` 함수의 고차 버전은 `arrow-core-high-arity`에서 사용할 수 있습니다.

---

## 타입화된 에러와 함께 작업하기

### 핵심 개념

타입화된 에러는 코드 조각 실행 중 발생할 수 있는 잠재적 에러를 시그니처(또는 타입)에서 명시적으로 만드는 함수형 프로그래밍의 기법입니다.

### 두 종류의 문제

Arrow는 두 가지 유형의 문제를 구분합니다:

1. 논리적 실패(Logical Failures): 도메인별 문제(예: 사용자를 찾을 수 없음)로, 비즈니스 로직 내에서 예상되고 복구 가능한 것들
2. 실제 예외(Real Exceptions): 기술적 문제(데이터베이스 연결 끊김, 네트워크 타임아웃)로, 진정으로 예외적이며 복원력 메커니즘이 필요한 것들

### 구현 접근 방식

#### 래퍼 타입 접근 방식 - 값으로서의 에러

```kotlin
fun findUser(id: UserId): Either<UserNotFound, User>
```

#### 계산 컨텍스트 접근 방식 - 컨텍스트로서의 에러 (Raise 사용)

```kotlin
fun Raise<UserNotFound>.findUser(id: UserId): User
```

### 성공과 실패 정의하기

#### 성공 값

래퍼 타입에서는 `.right()`를, Raise에서는 직접 값을 반환합니다:

```kotlin
val user: Either<UserNotFound, User> = User(1).right()

fun Raise<UserNotFound>.user(): User = User(1)
```

#### 실패 값

`.left()` 또는 `raise()`를 사용합니다:

```kotlin
val error: Either<UserNotFound, User> = UserNotFound.left()

fun Raise<UserNotFound>.error(): User = raise(UserNotFound)
```

### 유효성 검사 함수

#### ensure

조건을 검사하고, 거짓이면 에러를 발생시킵니다:

```kotlin
ensure(id > 0) { UserNotFound("Invalid id: $id") }
```

#### ensureNotNull

널러블 값을 언래핑하고 스마트 캐스팅을 수행합니다:

```kotlin
ensureNotNull(user) { UserNotFound("Cannot process null user") }
return user.id // null이 아닌 것으로 스마트 캐스팅됨
```

### 검사와 실행

Kotlin의 `when` 표현식이나 `fold()`를 사용하여 결과를 검사합니다:

```kotlin
val result: Either<UserNotFound, User> = findUser(userId)

when (result) {
    is Either.Left -> println("Error: ${result.value}")
    is Either.Right -> println("User: ${result.value}")
}

// 또는 fold 사용
result.fold(
    ifLeft = { error -> println("Error: $error") },
    ifRight = { user -> println("User: $user") }
)
```

### 에러 복구

#### 논리적 실패로부터의 복구

getOrElse - 래퍼 타입에서 대체 값을 제공합니다:

```kotlin
fetchUser(-1).getOrElse { e: UserNotFound -> null }
```

recover - 언래핑 없이 한 에러 타입을 다른 것으로 변환합니다:

```kotlin
fetchUser(-1).recover { _: UserNotFound -> raise(OtherError) }
```

#### 예외 처리

catch DSL은 외부 코드를 래핑하고 예외를 타입화된 에러로 변환합니다:

```kotlin
catch({ UsersQueries.insert(username, email) }) { e: SQLException ->
    if (e.isUniqueViolation()) raise(UserAlreadyExists(username, email))
    else throw e
}
```

이 접근 방식은 `OutOfMemoryError`와 같은 치명적인 예외를 캡처하지 않습니다.

### 에러 누적

첫 번째 에러에서 단락(short-circuit)하는 대신, 모든 에러를 수집할 수 있습니다.

#### mapOrAccumulate

모든 에러를 `NonEmptyList`에 수집합니다:

```kotlin
(1..10).mapOrAccumulate { isEven(it) }
// 모든 에러 또는 모든 성공 중 하나를 반환
```

사용자 정의 누적기는 이항 연산자를 사용하여 에러를 결합합니다:

```kotlin
(1..10).mapOrAccumulate(MyError::plus) { isEven(it) }
```

#### forEachAccumulating

결과를 저장하지 않고 계산을 실행합니다:

```kotlin
forEachAccumulating(1..10) { i ->
    ensure(i % 2 == 0) { "$i is not even" }
}
```

#### zipOrAccumulate

독립적인 유효성 검사를 실행하고 결과를 결합합니다:

```kotlin
zipOrAccumulate(
    { ensure(name.isNotEmpty()) { UserProblem.EmptyName } },
    { ensure(age >= 0) { UserProblem.NegativeAge(age) } }
) { _, _ -> User(name, age) }
```

#### accumulate 블록

`ensureOrAccumulate`와 `bindOrAccumulate`를 사용한 범위 지정 누적:

```kotlin
accumulate {
    ensureOrAccumulate(name.isNotEmpty()) { UserProblem.EmptyName }
    ensureOrAccumulate(age >= 0) { UserProblem.NegativeAge(age) }
    User(name, age)
}
```

### 에러 변환

#### withError

호환되지 않는 계산을 바인딩할 때 에러 타입을 변환합니다:

```kotlin
val intError: Either<Int, Boolean> = either {
    withError({ it.length }) { stringError.bind() }
}
```

일반적인 사용 사례: 하위 컴포넌트 유효성 검사 에러를 상위 유효성 검사 에러로 연결합니다.

#### ignoreErrors

`Either`와 같은 상세 타입에서 에러 정보를 버립니다.

### 타입화된 에러의 장점 요약

- 타입 안전성: 컴파일러가 에러 조건을 조기에 감지
- 예측 가능성: 시그니처에 에러 조건을 명시적으로 표시
- 조합 가능성: 함수 체인을 통해 에러가 원활하게 전파
- 성능: 예외 처리보다 더 효율적

모든 빌더 함수(`either`, `ior`)는 `Raise` 위에 구축되어 복잡한 애플리케이션 전반에서 원활한 상호운용성을 가능하게 합니다.

---

## 유효성 검사

이 튜토리얼은 Arrow에서 타입화된 에러를 사용하여 도메인 유효성 검사를 구현하는 방법을 보여주며, "유효성 검사하지 말고 파싱하라(parse, don't validate)" 원칙을 따릅니다.

> 잠재적으로 잘못된 클래스 인스턴스를 먼저 생성하는 대신, 컴포넌트가 모든 제약 조건을 충족할 때만 인스턴스를 구축해야 합니다.

### 도메인 모델

```kotlin
data class Author(val name: String)
data class Book(val title: String, val authors: NonEmptyList<Author>)
```

세 가지 유효성 검사 규칙:
1. 제목은 비어있을 수 없음
2. 저자 목록은 비어있을 수 없음
3. 저자 이름은 비어있을 수 없음

### 스마트 생성자 패턴

공개 생성자를 노출하는 대신, Arrow는 컴패니언 객체 `invoke` 연산자를 통한 "스마트 생성자"를 사용합니다:

```kotlin
data class Author private constructor(val name: String) {
    companion object {
        operator fun invoke(name: String): Either<EmptyAuthorName, Author> = either {
            ensure(name.isNotEmpty()) { EmptyAuthorName }
            Author(name)
        }
    }
}
```

이렇게 하면 성공 또는 특정 에러를 전달하는 타입화된 `Either` 결과를 반환하면서 구현 세부사항을 숨깁니다.

### 에러 타입

유효성 검사 에러는 계층적으로 구성됩니다:

```kotlin
sealed interface BookValidationError
object EmptyTitle: BookValidationError
object NoAuthors: BookValidationError
data class EmptyAuthor(val index: Int): BookValidationError
```

### zipOrAccumulate를 사용한 에러 누적

첫 번째 에러에서 실패하는 대신, `zipOrAccumulate`는 여러 유효성 검사 실패를 수집합니다:

```kotlin
Either<NonEmptyList<BookValidationError>, Book> = either {
    zipOrAccumulate(
        { ensure(title.isNotEmpty()) { EmptyTitle } },
        { ensureNotNull(authors.toNonEmptyListOrNull()) { NoAuthors } }
    ) { _, _ -> Unit }
}
```

### 컬렉션 유효성 검사

`mapOrAccumulate` 함수는 에러를 누적하면서 컬렉션을 처리합니다:

```kotlin
val validatedAuthors = mapOrAccumulate(authors.withIndex()) { nameAndIx ->
    Author(nameAndIx.value)
        .recover { _ -> raise(EmptyAuthor(nameAndIx.index)) }
        .bind()
}
```

주요 요소:
- `withIndex()`: 에러 보고를 위한 위치 정보 보존
- `recover`: 계산 내에서 에러 타입 변환
- `.bind()`: `Either` 값을 `Raise` 블록에 임베드

### 대안적 접근 방식

`map` 다음에 `.bindAll()`을 사용:

```kotlin
val validatedAuthors = authors.withIndex().map { nameAndIx ->
    Author(nameAndIx.value)
        .mapLeft { EmptyAuthor(nameAndIx.index) }
}.bindAll()
```

이 변형은 에러가 부수 효과를 필요로 하지 않을 때 코드를 단순화합니다.

### 주요 구분점

- `recover`: 블록 내에서 타입화된 에러 계산 수행
- `mapLeft`: 에러 값 자체만 변환

두 접근 방식 모두 유효성 검사가 부수 효과가 없을 때 동등한 결과를 달성합니다.

---

## 래퍼 타입

Arrow는 함수형 프로그래밍에서 실패를 표현하기 위한 여러 래퍼 타입을 제공합니다.

### 래퍼 타입 비교

| 타입 | 실패 | 추가 상태 | 소스 |
|------|------|----------|------|
| `A?` | `null` | 없음 | Kotlin stdlib |
| `Option<A>` | `None` | 없음 | Arrow core |
| `Result<A>` | `Throwable` | 없음 | Kotlin stdlib |
| `Either<E, A>` | `Left` (타입 E) | 없음 | Arrow core |
| `Ior<E, A>` | `Left` (타입 E) | `Both` (성공 + 실패) | Arrow core |
| `Result<A, E>` | `Failure` (타입 E) | 없음 | Result4k 라이브러리 |
| `Outcome<E, A>` | `Failure` (타입 E) | `Absent` 값 | Quiver 라이브러리 |
| `ProgressiveOutcome<E, A>` | `Failure` (타입 E) | `Incomplete` 값 | Pedestal State |

---

### Either와 Ior

#### 개요

`Either<E, A>`와 `Ior<E, A>`는 에러 처리를 위한 래퍼 타입입니다. 관례상 `E`는 에러를, `A`는 성공 값을 나타냅니다.

Either: 두 가지 가능성을 허용합니다 - `Left`(에러) 또는 `Right`(성공).

Ior (Inclusive Or): 세 번째 옵션인 `Both`를 추가합니다. 이는 잠재적인 경고나 비치명적 에러와 함께 성공적인 실행을 나타냅니다. 예: 성공적으로 완료되지만 경고를 생성하는 컴파일러.

Result: `Either`와 유사하지만 코루틴 기계에 밀접하게 결합되어 있고 `Throwable` 에러만 수락합니다.

#### 빌더 사용 (권장 접근 방식)

권장 패턴은 `Raise<E>` 수신자와 함께 `either`, `ior`, 또는 `result` 빌더 블록을 사용합니다:

```kotlin
import arrow.core.raise.either
import arrow.core.raise.ensure

data class MyError(val message: String)

fun isPositive(i: Int): Either<MyError, Int> = either {
    ensure(i > 0) { MyError("$i is not positive") }
    i
}

suspend fun example() {
    isPositive(-1) shouldBe MyError("-1 is not positive").left()
    isPositive(1) shouldBe 1.right()
}
```

블록 내 주요 함수:
- `raise`: 에러 신호
- `ensure`: 조건 유효성 검사
- `bind()`: 잠재적으로 에러가 있는 값 언래핑; 에러가 위로 버블링됨

Ior 블록의 경우: 여러 `Both` 에러를 결합하는 방법을 지정하는 추가 매개변수가 있습니다.

#### 빌더 없이 사용

더 간단한 경우에는 직접 함수를 사용합니다:

```kotlin
@JvmInline value class Age(val age: Int)

sealed interface AgeProblem {
    object InvalidAge: AgeProblem
    object NotLegalAdult: AgeProblem
}

fun validAdult(age: Int): Either<AgeProblem, Age> = when {
    age < 0  -> AgeProblem.InvalidAge.left()
    age < 18 -> AgeProblem.NotLegalAdult.left()
    else     -> Age(age).right()
}
```

확장 함수:
- `.left()`와 `.right()`: Left 또는 Right 값 생성
- `Either.catch`: 예외를 던지는 코드 래핑
- `mapLeft`: 에러 값 변환
- `recover`: 에러 처리

#### 유효성 검사를 위한 Either

두 가지 구별되는 패턴이 있습니다:

빠른 실패(Fail-fast): 첫 번째 에러에서 중지 (기본 `either` 블록 동작). 단계가 서로 의존할 때 사용합니다.

```kotlin
either {
    val step1 = someComputation().bind()
    val step2 = dependentComputation(step1).bind()
}
```

누적(Accumulation): 모든 에러를 수집. 실패가 독립적일 때 사용합니다.

```kotlin
zipOrAccumulate(
    { validation1().bind() },
    { validation2().bind() }
) { result1, result2 -> combine(result1, result2) }

mapOrAccumulate(itemList) { item -> validateItem(item) }
```

유효성 검사를 위한 일반적인 패턴:

```kotlin
public typealias EitherNel<E, A> = Either<NonEmptyList<E>, A>
```

이렇게 하면 에러 목록이 절대 비어있지 않도록 보장하여, 빈 에러 컬렉션과 함께하는 어색한 `Left` 값을 방지합니다.

#### 각 타입을 언제 사용할 것인가

| 타입 | 사용 사례 |
|------|----------|
| Either | 단계 의존적 작업; 빠른 실패 에러 처리; 성공/실패 상태 모델링 |
| Ior | 성공과 함께 경고를 허용하는 작업; 부분적 성공이 필요한 드문 사용 사례 |
| Result | 예외 기반 레거시 코드; `Throwable` 에러와의 코루틴 통합 |
| EitherNel | 종합적인 에러 누적이 필요한 입력 유효성 검사 |

---

### Nullable과 Option

#### 핵심 문제

Kotlin의 null 안전성 기능은 대부분의 사용 사례에 일반적으로 충분합니다. 그러나 Arrow의 `Option` 타입은 특정 문제인 중첩된 널러빌리티 문제를 해결합니다.

널러블 타입을 수락하는 제네릭 함수로 작업할 때, "목록이 비어있음"과 "첫 번째 요소가 null임" 사이에 모호성이 발생합니다. 예를 들어:

> "`null` 값은 종종 부재하는 선택적 값을 표현하기 위해 남용됩니다."

이 결함 있는 구현을 고려하세요:

```kotlin
fun <A> List<A>.firstOrElse(default: () -> A): A =
    firstOrNull() ?: default()
```

`listOf(null, 2, 3)`에서 이것은 `null` 대신 기본값을 반환하여 컨테이너가 비어있는지에 대한 혼란을 야기합니다.

#### 해결책

`firstOrNone()`과 함께 `Option`을 사용하면 이러한 경우를 명확하게 구분합니다:

```kotlin
fun <A> List<A>.firstOrElse(default: () -> A): A =
    when(val option = firstOrNone()) {
        is Some -> option.value
        None -> default()
    }
```

이제 null 값을 포함하는 목록을 포함하여 모든 테스트 케이스가 올바르게 동작합니다.

#### Option을 사용해야 할 때

문서에서 `Option`을 주로 다음과 같은 경우에 권장합니다:
- 중첩된 널러빌리티가 발생할 수 있는 제네릭 코드 작성 시
- null 사용을 제한하는 라이브러리(RxJava, Project Reactor)와 통합 시
- 부재 대 null 값의 명시적인 타입 안전 처리가 필요할 때

#### Option 작업하기

생성:

```kotlin
val some = Some("value").some()
val none = none<String>()
val fromNullable = "text".toOption()
```

추출:

```kotlin
Some("x").getOrNull()           // "x" 반환
None.getOrElse { "default" }    // "default" 반환
```

DSL 사용:

```kotlin
option {
    val userId = params.userId().bind()
    val user = findUserById(userId).bind()
    sendEmail(user.email).bind()
}
```

검사:

```kotlin
Some(1).isSome()                    // true
Some(2).isSome { it % 2 == 0 }     // true
Some(1).onSome { println(it) }     // 부수 효과 실행
```

#### 권장 사항

> "일반적으로 Kotlin에서 작업할 때, 더 관용적이므로 `Option`보다 널러블 타입으로 작업하는 것을 선호해야 합니다."

제네릭 코드와 라이브러리 통합 시나리오에서 선택적으로 `Option`을 사용하세요.

---

### Outcome과 Progress

#### Outcome: 단순한 성공/실패를 넘어서

Arrow는 [Quiver](https://block.github.io/quiver/)와 통합되며, 세 가지 별개의 상태를 특징으로 하는 `Outcome` 타입을 제공합니다:

- Present: 성공적인 값을 나타냄
- Failure: 에러가 발생했음을 나타냄
- Absent: 부재를 실패로 취급하지 않고 누락된 값을 나타냄

이 구분은 성공 또는 실패 상태만 처리하는 `Either`와 `Outcome`을 차별화합니다. 사용 예:

```kotlin
val good = 3.present()
val bad = "problem".failure()
val whoKnows = Absent
```

#### 진행 중 추적을 위한 Progressive Outcome

[Pedestal State](https://opensavvy.gitlab.io/groundwork/pedestal/)는 계산 상태와 진행 정보를 결합하는 `ProgressiveOutcome`을 도입합니다. 이 타입은 두 가지 구성 요소를 유지합니다:

상태 구성 요소 (`Outcome`과 유사):
- `Success`
- `Failure`
- `Incomplete`

진행 구성 요소:
- `Done`
- `Loading` (백분율 포함)

중요한 점은 성공이 완료를 요구하지 않는다는 것입니다. `Success(5, loading(0.4))`와 같은 값은 마지막 성공 결과가 5이고 새로고침이 40% 완료되었음을 나타냅니다.

코드 예제:

구조 분해 패턴:

```kotlin
fun <E, A> printProgress(po: ProgressiveOutcome<E, A>) {
    val (current, progress) = po
    when {
        current is Outcome.Success -> println("현재 값은 ${current.value}!")
        progress is Progress.Loading -> println("로딩 중...")
    }
}
```

헬퍼 함수 접근 방식:

```kotlin
fun <E, A> printProgress(po: ProgressiveOutcome<E, A>) {
    po.onSuccess { println("현재 값은 $it") }
    po.onLoading { println("로딩 중...") }
}
```

#### 반응형 스트림과의 통합

이러한 타입은 Kotlin의 `Flow` 또는 Compose의 `MutableState`와 효과적으로 결합하여 시간에 따른 데이터 변화를 모델링합니다. Pedestal은 이 패턴을 지원하는 동반 코루틴 라이브러리를 포함합니다.

---

### 사용자 정의 에러 타입

#### 개요

Arrow의 `Raise` 메커니즘을 통해 개발자는 특정 도메인에 맞춤화된 사용자 정의 에러 처리 DSL을 구축할 수 있습니다. 문서에서는 이를 `Lce`(Loading-Content-Failure) 예제를 통해 보여줍니다.

#### LCE 타입

프레임워크는 세 가지 상태를 나타내는 sealed interface를 사용합니다:

```kotlin
sealed interface Lce<out E, out A> {
    object Loading : Lce<Nothing, Nothing>
    data class Content<A>(val value: A) : Lce<Nothing, A>
    data class Failure<E>(val error: E) : Lce<E, Nothing>
}
```

#### 구현 패턴

LceRaise 래퍼 클래스:

```kotlin
@JvmInline
value class LceRaise<E>(val raise: Raise<Lce<E, Nothing>>) : Raise<Lce<E, Nothing>> by raise {
    fun <A> Lce<E, A>.bind(): A =
        when (this) {
            is Lce.Content -> value
            is Lce.Failure -> raise.raise(this)
            Lce.Loading -> raise.raise(Lce.Loading)
        }
}
```

래퍼는 `Raise`에 위임하면서 실패 또는 로딩 상태에서 단락하는 `bind()` 함수를 제공합니다.

DSL 함수:

```kotlin
inline fun <E, A> lce(@BuilderInference block: LceRaise<E>.() -> A): Lce<E, A> =
    recover({ Lce.Content(block(LceRaise(this))) }) { e: Lce<E, Nothing> -> e }
```

#### 사용 예제

```kotlin
lce {
    val a = Lce.Content(1).bind()
    val b = Lce.Content(1).bind()
    a + b
} // Lce.Content(2) 반환
```

#### 핵심 설계 통찰

에러 상태에 `Lce<E, Nothing>`을 사용하면 여러 에러 시나리오와 `Either`와 같은 다른 Arrow 타입과의 상호운용성을 가능하게 하여 에러 누적 패턴을 지원합니다.

---

## Either에서 Raise로

이 Arrow 문서 페이지는 Kotlin에서 타입화된 에러를 처리하기 위해 함수형 프로그래밍의 `Either` 타입에서 Arrow의 `Raise` DSL로 전환하는 방법을 설명합니다.

### 순차적 합성

#### Either 스타일 (flatMap 접근 방식)

전통적인 방법은 실패할 가능성이 있는 작업에 `flatMap`을, 순수 변환에 `map`을 사용하여 계산을 체이닝합니다:

```kotlin
fun foo(n: Int): Either<Error, String> =
    f(n).flatMap { s ->
        g(s).map { t ->
            t.summarize()
        }
    }
```

#### Raise 스타일 (bind 접근 방식)

DSL 기반 접근 방식은 더 순차적인 모습을 위해 `bind()` 호출과 함께 `either` 빌더를 사용합니다:

```kotlin
fun foo(n: Int): Either<Error, String> = either {
    val s = f(n).bind()
    val t = g(s).bind()
    t.summarize()
}
```

주요 구조적 차이점: `Raise`를 사용하면 "함수 합성 구조에 의존하지 않고 선호하는 논리적 분해에 의존"합니다.

### 왜 "Raise DSL"인가?

`either` 함수 시그니처가 패턴을 드러냅니다:

```kotlin
fun <E, A> either(block: Raise<E>.() -> A): Either<E, A>
```

`Raise<E>`는 확장 수신자 역할을 하여 `bind`, `raise`, `ensure`와 같은 함수를 블록 범위 내에서 암시적으로 사용할 수 있게 합니다 - Kotlin의 타입 안전 빌더나 코루틴 DSL과 유사합니다.

### 논리적 에러와 함께 반환하기

`Left`를 명시적으로 구성하는 대신, `raise()`를 사용하여 실패를 신호합니다:

```kotlin
fun fooThatRaises(n: Int): Either<Error, String> = either {
    ensure(n >= 0) { Error.NegativeInput }
    val s = f(n).bind()
    val t = g(s).bind()
    t.summarize()
}
```

강조되는 구분: "도메인 모델의 논리적 문제에 타입화된 에러를 사용하고, 예외적인 상황에는 사용하지 마세요."

### 에러 값 변환하기

서로 다른 계산이 서로 다른 에러 타입을 생성할 때, `withError`를 사용하여 연결합니다:

```kotlin
fun bar(n: Int): Either<Error, String> = either {
    val s = f(n).bind()
    val t = withError({ boo -> boo.toError() }) { h(s).bind() }
    t.summarize()
}
```

이것은 `Either` 스타일의 `mapLeft` 함수를 대체합니다.

### 컬렉션 처리하기

Raise 없이:

```kotlin
fun foos(xs: List<Int>) = xs.traverse { foo(it) }
```

Raise와 함께:

```kotlin
fun foos(xs: List<Int>) = either {
    xs.map { foo(it).bind() }
    // 또는
    xs.map { foo(it) }.bindAll()
}
```

일반 컬렉션 함수가 `traverse`와 같은 특수화된 "효과적" 결합자를 대체합니다.

### 에러 누적

기본적으로 실행은 첫 번째 에러에서 중단됩니다. 모든 에러를 수집하려면 "실패하고 계속하기 의미론"을 위해 `mapOrAccumulate`로 전환하세요.

### 완전한 Raise 스타일 (Either 없이)

궁극적인 패턴은 함수를 `Raise<Error>`의 확장 함수로 만들어 `Either` 래핑을 완전히 제거합니다:

```kotlin
fun Raise<Error>.f(n: Int): String
fun Raise<Error>.g(s: String): Thing

fun Raise<Error>.bar(n: Int): String {
    val s = f(n)  // bind() 필요 없음
    val t = withError({ boo -> boo.toError() }) { h(s) }
    return t.summarize()
}
```

이것은 "Right와 Left 값을 래핑하고 언래핑하는 것"을 완전히 피합니다.

### 주요 제한사항

Kotlin은 현재 다중 수신자를 허용하지 않습니다. 확장 수신자가 있는 함수(예: `Thing.problematic()`)는 원래 수신자를 매개변수로 수락하도록 재구성하지 않고는 직접 `Raise<Error>` 수신자가 될 수 없습니다.

---

## 참고 자료

- [Arrow-kt 공식 문서](https://arrow-kt.io/)
- [Arrow-kt Typed Errors](https://arrow-kt.io/learn/typed-errors/)
- [Arrow-kt GitHub](https://github.com/arrow-kt/arrow)

---

*이 문서는 Arrow-kt 공식 문서를 기반으로 한국어로 번역되었습니다.*
