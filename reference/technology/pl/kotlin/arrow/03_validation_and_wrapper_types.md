# 03. 검증과 오류 래퍼 타입

- 확인일: 2026-09-08.
- 코드: 개념 설명용 발췌 예제 포함. 생략한 타입, import, 프로젝트 설정이 있는 예제는 독립 실행 프로그램이 아님.
- 원본 문서와 코드의 라이선스: [Apache-2.0](LICENSE.arrow-website).

[전체 목차](01_introduction_and_setup.md)


## 수록한 공식 문서

- [typed-errors/validation](https://arrow-kt.io/learn/typed-errors/validation/)
- [typed-errors/wrappers](https://arrow-kt.io/learn/typed-errors/wrappers/)
- [typed-errors/wrappers/either-and-ior](https://arrow-kt.io/learn/typed-errors/wrappers/either-and-ior/)
- [typed-errors/wrappers/nullable-and-option](https://arrow-kt.io/learn/typed-errors/wrappers/nullable-and-option/)
- [typed-errors/wrappers/outcome-progress](https://arrow-kt.io/learn/typed-errors/wrappers/outcome-progress/)
- [typed-errors/wrappers/own-error-types](https://arrow-kt.io/learn/typed-errors/wrappers/own-error-types/)

## 유효성 검사

> 원문: [typed-errors/validation](https://arrow-kt.io/learn/typed-errors/validation/)

Arrow의 타입화된 에러로 도메인 값을 검증할 때는 "유효성 검사하지 말고 파싱하라(parse, don't validate)" 원칙을 적용할 수 있습니다. 검증 결과를 단순한 성공 여부로 끝내지 않고, 조건을 통과한 도메인 값으로 반환하는 방식입니다.

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

앞의 규칙을 객체 생성 시점에 적용하려면 생성자를 비공개로 두고, 컴패니언 객체의 `invoke` 연산자에서 검증합니다. 다음은 이런 "스마트 생성자"로 저자 이름을 검사하는 예시입니다:

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

제목과 저자 목록의 존재 여부를 검사했다면, 목록 안의 저자 이름도 검사해야 합니다. `mapOrAccumulate`는 각 요소를 검증하면서 발생한 에러를 누적합니다. 다음 예제는 잘못된 저자의 위치도 함께 보존합니다.

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

같은 검증을 `map`과 `.bindAll()`로 나눌 수도 있습니다. 먼저 각 저자의 에러를 `mapLeft`로 변환한 뒤 결과들을 바인딩합니다.

```kotlin
val validatedAuthors = authors.withIndex().map { nameAndIx ->
    Author(nameAndIx.value)
        .mapLeft { EmptyAuthor(nameAndIx.index) }
}.bindAll()
```

에러를 변환할 때 부수 효과가 필요하지 않다면 이 방식으로 코드를 단순화할 수 있습니다.

### 주요 구분점

- `recover`: 블록 내에서 타입화된 에러 계산 수행
- `mapLeft`: 에러 값 자체만 변환

유효성 검사에 부수 효과가 없다면 두 방식은 같은 결과를 냅니다.

---

## 래퍼 타입

> 원문: [typed-errors/wrappers](https://arrow-kt.io/learn/typed-errors/wrappers/)

Arrow는 함수형 프로그래밍에서 실패를 표현하기 위한 여러 래퍼 타입을 제공합니다.

### 래퍼 타입 비교

- `A?`
  - 실패: `null`
  - 추가 상태: 없음
  - 소스: Kotlin stdlib
- `Option<A>`
  - 실패: `None`
  - 추가 상태: 없음
  - 소스: Arrow core
- `Result<A>`
  - 실패: `Throwable`
  - 추가 상태: 없음
  - 소스: Kotlin stdlib
- `Either<E, A>`
  - 실패: `Left` (타입 E)
  - 추가 상태: 없음
  - 소스: Arrow core
- `Ior<E, A>`
  - 실패: `Left` (타입 E)
  - 추가 상태: `Both` (성공 + 실패)
  - 소스: Arrow core
- `Result<A, E>`
  - 실패: `Failure` (타입 E)
  - 추가 상태: 없음
  - 소스: Result4k 라이브러리
- `Outcome<E, A>`
  - 실패: `Failure` (타입 E)
  - 추가 상태: `Absent` 값
  - 소스: Quiver 라이브러리
- `ProgressiveOutcome<E, A>`
  - 실패: `Failure` (타입 E)
  - 추가 상태: `Incomplete` 값
  - 소스: Pedestal State

---

### Either와 Ior

> 원문: [typed-errors/wrappers/either-and-ior](https://arrow-kt.io/learn/typed-errors/wrappers/either-and-ior/)

#### 개요

`Either<E, A>`와 `Ior<E, A>`는 에러 처리를 위한 래퍼 타입입니다. 관례상 `E`는 에러를, `A`는 성공 값을 나타냅니다.

`Either`는 `Left`(에러)와 `Right`(성공) 두 상태를 표현합니다. `Ior`(Inclusive Or)는 여기에 성공 값과 에러를 함께 담는 `Both`를 추가합니다. 컴파일은 성공했지만 경고가 남는 경우처럼, 결과와 함께 비치명적인 문제를 전달할 때 사용할 수 있습니다.

Result: `Either`와 유사하지만 코루틴의 동작 방식과 밀접하게 연결되어 있으며, 에러로는 `Throwable`만 받습니다.

#### 빌더 사용 (권장 접근 방식)

`Raise<E>` 수신자와 함께 `either`, `ior`, `result` 빌더 블록을 사용하는 방식을 권장합니다.

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

이렇게 하면 에러 목록에 적어도 하나의 에러가 있음을 보장하므로, 에러가 없는 빈 컬렉션을 `Left`에 담는 상황을 방지합니다.

#### 각 타입을 언제 사용할 것인가

- Either: 단계 의존적 작업; 빠른 실패 에러 처리; 성공/실패 상태 모델링
- Ior: 성공과 함께 경고를 허용하는 작업; 부분적 성공이 필요한 드문 사용 사례
- Result: 예외 기반 레거시 코드; `Throwable` 에러와의 코루틴 통합
- EitherNel: 종합적인 에러 누적이 필요한 입력 유효성 검사

---

### Nullable과 Option

> 원문: [typed-errors/wrappers/nullable-and-option](https://arrow-kt.io/learn/typed-errors/wrappers/nullable-and-option/)

#### 핵심 문제

대부분의 경우에는 Kotlin의 null 안전성 기능만으로 충분합니다. 다만 널러블 타입도 허용하는 제네릭 함수에서는 "목록이 비어 있음"과 "첫 번째 요소가 null임"을 구분해야 할 수 있습니다. Arrow의 `Option`은 이처럼 값의 부재와 null 값이 겹치는 문제를 해결합니다.

> "`null` 값은 종종 부재하는 선택적 값을 표현하기 위해 남용됩니다."

다음 구현에서는 이 두 경우가 같은 결과로 처리됩니다:

```kotlin
fun <A> List<A>.firstOrElse(default: () -> A): A =
    firstOrNull() ?: default()
```

`listOf(null, 2, 3)`에 이 함수를 호출하면 첫 요소인 `null` 대신 기본값을 반환합니다. `firstOrNull()`만으로는 목록이 비었는지, 실제 첫 요소가 null인지 구분할 수 없기 때문입니다.

#### 해결책

`firstOrNone()`과 함께 `Option`을 사용하면 이러한 경우를 명확하게 구분합니다:

```kotlin
fun <A> List<A>.firstOrElse(default: () -> A): A =
    when(val option = firstOrNone()) {
        is Some -> option.value
        None -> default()
    }
```

이 구현에서는 목록이 비었을 때만 `None` 분기에서 기본값을 반환합니다. 첫 요소가 null이면 `Some` 안의 null을 그대로 반환하므로 두 경우를 구분할 수 있습니다.

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

> 원문: [typed-errors/wrappers/outcome-progress](https://arrow-kt.io/learn/typed-errors/wrappers/outcome-progress/)

#### Outcome: 단순한 성공/실패를 넘어서

Arrow는 [Quiver](https://block.github.io/quiver/)와 통합되며, 세 가지 상태를 구분하는 `Outcome` 타입을 제공합니다.

- Present: 성공적인 값을 나타냄
- Failure: 에러가 발생했음을 나타냄
- Absent: 부재를 실패로 취급하지 않고 누락된 값을 나타냄

성공과 실패만 표현하는 `Either`에 비해, `Outcome`은 실패가 아닌 값의 부재를 `Absent`로 따로 표현할 수 있습니다:

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

성공 상태라고 해서 진행 중인 작업까지 완료된 것은 아닙니다. 예를 들어 `Success(5, loading(0.4))`는 마지막 성공 결과가 5이고 새로고침은 40% 진행되었음을 나타냅니다.

상태와 진행 정보를 읽을 때는 먼저 두 값을 구조 분해한 뒤 필요한 조건을 확인할 수 있습니다.

```kotlin
fun <E, A> printProgress(po: ProgressiveOutcome<E, A>) {
    val (current, progress) = po
    when {
        current is Outcome.Success -> println("현재 값은 ${current.value}!")
        progress is Progress.Loading -> println("로딩 중...")
    }
}
```

같은 타입에 제공되는 헬퍼 함수를 사용하면 처리할 상태별로 동작을 지정할 수 있습니다.

```kotlin
fun <E, A> printProgress(po: ProgressiveOutcome<E, A>) {
    po.onSuccess { println("현재 값은 $it") }
    po.onLoading { println("로딩 중...") }
}
```

#### 반응형 스트림과의 통합

이 타입들을 Kotlin의 `Flow`나 Compose의 `MutableState`와 함께 사용하면 시간에 따른 데이터 변화를 표현할 수 있습니다. Pedestal은 이 용도로 사용할 코루틴 라이브러리도 제공합니다.

---

### 사용자 정의 에러 타입

> 원문: [typed-errors/wrappers/own-error-types](https://arrow-kt.io/learn/typed-errors/wrappers/own-error-types/)

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

## 오류 누적과 UI 상태를 선택하는 기준

- 필수 입력이 하나라도 잘못되면 결과를 만들 수 없는 경우 `Either<NonEmptyList<E>, A>`와 오류 누적 연산을 사용함.
- 경고와 결과가 함께 유효한 경우 `Ior<E, A>` 고려 가능. `Both`가 오류 누적용 Either를 자동 대체하는 것은 아님.
- 단순 부재만 필요한 경우 nullable 또는 `Option`, 부재 이유가 필요한 경우 `Either` 등 구체적인 오류 타입 사용.
- `Outcome`, `Progress`는 [원문](https://arrow-kt.io/learn/typed-errors/wrappers/outcome-progress/)에서 소개하는 별도 Quiver 라이브러리 타입임. `arrow-core` 자체의 타입으로 간주하지 않음.
- 사용자 정의 오류 타입과 DSL을 연결할 때는 반환 타입만 만들지 말고 `Raise`와의 변환, `bind` 역할도 함께 정의함.
- 1.x의 `Validated` 예제를 옮길 때에는 `Either`로 이름만 치환하면 오류 누적이 중단될 수 있음. 09장의 마이그레이션 절 참고.
