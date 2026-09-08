# 02. 타입으로 표현하는 오류와 Raise

- 확인일: 2026-09-08.
- 코드: 개념 설명용 발췌 예제 포함. 생략한 타입, import, 프로젝트 설정이 있는 예제는 독립 실행 프로그램이 아님.
- 원본 문서와 코드의 라이선스: [Apache-2.0](LICENSE.arrow-website).

[전체 목차](01_introduction_and_setup.md)


## 수록한 공식 문서

- [typed-errors](https://arrow-kt.io/learn/typed-errors/)
- [typed-errors/working-with-typed-errors](https://arrow-kt.io/learn/typed-errors/working-with-typed-errors/)
- [typed-errors/from-either-to-raise](https://arrow-kt.io/learn/typed-errors/from-either-to-raise/)

## 개요

> 원문: [typed-errors](https://arrow-kt.io/learn/typed-errors/)

Arrow는 타입화된 에러를 "함수형 프로그래밍에서 코드 실행 중 발생할 수 있는 잠재적 에러를 시그니처(또는 타입)에서 명시적으로 만드는 기법"으로 정의합니다.

### 두 가지 접근 방식

Arrow는 타입화된 에러를 처리하기 위한 두 가지 상호보완적인 접근 방식을 제공합니다:

#### 1. Raise DSL

Raise DSL은 발생할 수 있는 에러 타입을 확장 수신자에 담습니다. 아래 함수는 성공하면 `User`를 반환하고, 실행 중에는 `UserNotFound` 에러를 발생시킬 수 있음을 `Raise<UserNotFound>`로 표현합니다.

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

타입화된 에러 기능은 `arrow-core`에서 제공합니다. `zipOrAccumulate`에 많은 인자를 전달해야 한다면 `arrow-core-high-arity`의 함수를 사용할 수 있습니다.

---

## 타입화된 에러와 함께 작업하기

> 원문: [typed-errors/working-with-typed-errors](https://arrow-kt.io/learn/typed-errors/working-with-typed-errors/)

### 핵심 개념

타입화된 에러는 코드를 실행할 때 발생할 수 있는 에러를 함수 시그니처나 타입에 명시하는 함수형 프로그래밍 기법입니다.

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

하위 컴포넌트의 검증 에러를 상위 컴포넌트의 에러 타입으로 바꿀 때도 이 방식을 사용합니다. 에러 타입을 맞추면 서로 다른 계산을 같은 Raise 블록에서 연결할 수 있습니다.

#### ignoreErrors

`Either`와 같은 상세 타입에서 에러 정보를 버립니다.

## Either에서 Raise로

> 원문: [typed-errors/from-either-to-raise](https://arrow-kt.io/learn/typed-errors/from-either-to-raise/)

`Either` 계산을 `flatMap`과 `map`으로 연결하던 코드는 `Raise` DSL에서 순차적인 함수 호출 형태로 옮길 수 있습니다. 같은 계산을 두 방식으로 비교해 봅니다.

모든 빌더 함수(`either`, `ior`)는 `Raise`를 기반으로 하므로 함께 조합해 사용할 수 있습니다. 사용 방식에 따라 래퍼 생성과 스택 추적 비용을 줄일 여지도 있지만, 실제 성능 차이는 워크로드로 확인해야 합니다.

### 순차적 합성

#### Either 스타일 (flatMap 접근 방식)

기존 `Either` 방식에서는 실패할 수 있는 작업을 `flatMap`으로 연결하고, 순수 변환에는 `map`을 사용합니다.

```kotlin
fun foo(n: Int): Either<Error, String> =
    f(n).flatMap { s ->
        g(s).map { t ->
            t.summarize()
        }
    }
```

#### Raise 스타일 (bind 접근 방식)

앞의 계산을 `either` 빌더로 옮기면 중첩된 람다 대신 `bind()`로 결과를 꺼내 다음 함수에 전달할 수 있습니다:

```kotlin
fun foo(n: Int): Either<Error, String> = either {
    val s = f(n).bind()
    val t = g(s).bind()
    t.summarize()
}
```

이렇게 쓰면 `flatMap`과 `map`의 중첩 구조보다 계산이 진행되는 순서가 코드에 직접 드러납니다.

### 왜 "Raise DSL"인가?

`either`의 함수 시그니처를 보면 `Raise`가 어떻게 사용되는지 알 수 있습니다.

```kotlin
fun <E, A> either(block: Raise<E>.() -> A): Either<E, A>
```

`Raise<E>`가 확장 수신자이므로 블록 안에서는 `bind`, `raise`, `ensure`를 수신자 없이 호출할 수 있습니다. Kotlin의 타입 안전 빌더나 코루틴 DSL처럼 블록의 수신자가 사용할 수 있는 연산을 제공하는 구조입니다.

### 논리적 에러와 함께 반환하기

`Left`를 명시적으로 구성하는 대신, `raise()`를 사용하여 실패를 알립니다.

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

일반 컬렉션 함수와 `bind()`를 함께 사용하면 `traverse`처럼 이펙트를 다루는 전용 결합자를 대체할 수 있습니다.

### 에러 누적

기본적으로 실행은 첫 번째 에러에서 중단됩니다. 각 요소를 독립적으로 검증하고 모든 에러를 모아야 한다면 `mapOrAccumulate`를 사용합니다.

### 완전한 Raise 스타일 (Either 없이)

함수 자체를 `Raise<Error>`의 확장 함수로 정의하면 반환값을 `Either`로 감싸지 않고도 같은 에러 컨텍스트에서 호출할 수 있습니다:

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

확장 수신자 방식에서는 한 함수에 확장 수신자를 두 개 선언할 수 없습니다. 기존 수신자를 일반 매개변수로 옮기거나, 아래의 context parameter 방식으로 Raise 문맥을 전달할 수 있습니다.

## Raise 스코프와 현재 문법

> 참고: [Arrow 2.2.0](https://arrow-kt.io/community/blog/2025/11/01/arrow-2-2/), [Arrow 2.2.2](https://arrow-kt.io/community/blog/2026/03/04/arrow-2-2-2/)

이 문서의 기본 예제는 확장 수신자 방식인 `arrow.core.raise`를 사용합니다. Arrow 2.2 계열은 context parameter 방식의 `arrow.core.raise.context`도 제공하므로 해당 Kotlin 언어 기능을 활성화한 프로젝트에서는 이 방식을 선택할 수도 있습니다. 두 방식은 import와 호출 문맥이 다르므로 프로젝트 안에서는 일관되게 사용하는 편이 좋습니다.

- `either` 밖으로 반환한 지연 람다, Sequence, Flow 안에서 이전 스코프의 `raise`나 `bind`를 실행하면 스코프가 이미 종료되었을 수 있음. 실제 실행 시점에 Raise 스코프를 생성해야 함.
- `CancellationException`을 일반 오류로 삼켜서는 안 됨. Arrow의 예외 처리 연산과 취소 전파 규칙을 함께 적용함.
- 아래 예제는 확장 수신자 방식에서 입력 검사, 오류 전파, 결과 생성을 연결한 독립 예제임.

```kotlin
import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.raise.ensure

sealed interface NameError {
    data object Empty : NameError
}

fun Raise<NameError>.checkedName(raw: String): String {
    val name = raw.trim()
    ensure(name.isNotEmpty()) { NameError.Empty }
    return name
}

fun parseName(raw: String): Either<NameError, String> = either {
    checkedName(raw)
}
```
