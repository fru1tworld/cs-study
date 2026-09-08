# 08. 컬렉션과 함수

- 확인일: 2026-09-08.
- 코드: 개념 설명용 발췌 예제 포함. 생략한 타입, import, 프로젝트 설정이 있는 예제는 독립 실행 프로그램이 아님.
- 원본 문서와 코드의 라이선스: [Apache-2.0](LICENSE.arrow-website).

[전체 목차](01_introduction_and_setup.md)


## 수록한 공식 문서

- [collections-functions](https://arrow-kt.io/learn/collections-functions/)
- [collections-functions/non-empty](https://arrow-kt.io/learn/collections-functions/non-empty/)
- [collections-functions/collectors](https://arrow-kt.io/learn/collections-functions/collectors/)
- [collections-functions/recursive](https://arrow-kt.io/learn/collections-functions/recursive/)
- [collections-functions/memoize](https://arrow-kt.io/learn/collections-functions/memoize/)
- [collections-functions/eval](https://arrow-kt.io/learn/collections-functions/eval/)
- [collections-functions/utils](https://arrow-kt.io/learn/collections-functions/utils/)

## 개요

> 원문: [collections-functions](https://arrow-kt.io/learn/collections-functions/)

Arrow는 Kotlin의 기본 컬렉션과 함수 기능을 보완하는 유틸리티를 제공합니다. 컬렉션이 비어 있지 않음을 타입으로 표현하거나, 집계와 재귀 계산의 실행 방식을 제어할 때 사용할 수 있습니다.

### 주요 카테고리

이 섹션은 6개의 핵심 영역으로 구성되어 있습니다:

1. 비어 있지 않은 컬렉션 - 최소한 하나의 요소가 보장된 컬렉션 작업
2. 수집기 - 시퀀스에 대한 더 나은 집계 연산을 위한 도구
3. 재귀 함수 - 함수를 스택 안전하고 효율적으로 만드는 기술
4. 메모이제이션 - 순수 함수에서 중복 계산을 피하는 전략
5. 평가 제어 - 계산을 지연시키고 결과를 캐싱하는 방법
6. 함수 유틸리티 - 합성, 부분 적용, 커링을 지원하는 도구

---

<a id="비어있지-않은-컬렉션-non-empty-collections"></a>

## 비어 있지 않은 컬렉션 (Non-empty Collections)

> 원문: [collections-functions/non-empty](https://arrow-kt.io/learn/collections-functions/non-empty/)

### 개요

Arrow의 `arrow-core`는 요소가 최소 하나 있어야 하는 상황을 위해 비어 있지 않은 컬렉션 타입을 제공합니다. 대표적인 타입은 다음 두 가지입니다.

- NonEmptyList: 최소한 하나의 요소를 포함하는 것이 보장된 리스트
- NonEmptySet: 최소한 하나의 요소를 포함하는 것이 보장된 집합

### 해결하는 핵심 문제

`Either<List<Problem>, Result>`는 실패를 뜻하는 `Left` 안에 빈 오류 목록을 담을 수도 있습니다. 타입만 보면 실패했지만 문제가 하나도 없는 상태를 허용하는 셈입니다. `mapOrAccumulate`와 `zipOrAccumulate`는 `Either<NonEmptyList<Problem>, Result>`를 반환해 실패한 경우 적어도 하나의 오류가 있도록 합니다.

### API 설계 원칙

비어 있지 않은 컬렉션의 API는 Kotlin의 표준 `kotlin.collections` 규칙을 따르면서, 다음 연산의 반환 타입에도 요소의 존재 여부를 반영합니다.

#### 생성
- `nonEmptyListOf`와 `nonEmptySetOf`는 최소한 하나의 인자를 요구하여 빈 인스턴스 생성을 방지합니다

```kotlin
import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf

// 최소한 하나의 요소가 필요
val nel: NonEmptyList<Int> = nonEmptyListOf(1, 2, 3)

// 컴파일 오류: 인자 없이 호출 불가
// val empty = nonEmptyListOf<Int>()
```

#### 연산
- 크기 보존: `map`, `zip`과 같이 인자 크기를 존중하는 연산은 비어 있지 않은 컬렉션을 반환합니다
- 스마트 연결: 적어도 하나가 비어 있지 않은 컬렉션을 결합할 때, 결과는 비어 있지 않은 상태를 유지합니다

```kotlin
import arrow.core.nonEmptyListOf

val nel = nonEmptyListOf(1, 2, 3)

// map은 NonEmptyList를 반환
val mapped: NonEmptyList<String> = nel.map { it.toString() }

// 비어있지 않은 컬렉션과 연결하면 비어있지 않은 결과 반환
val combined = nel + listOf(4, 5)
```

이 설계 덕분에 컬렉션에 요소가 최소 하나 있어야 하는 조건을 타입으로 유지할 수 있습니다.

---

## 수집기 (Collectors)

> 원문: [collections-functions/collectors](https://arrow-kt.io/learn/collections-functions/collectors/)

### 개요

수집기는 값의 시퀀스를 한 번만 순회하면서 여러 계산을 수행하도록 설계된 도구입니다. 이들은 실험적이지만 안정적인 `arrow-collectors` 라이브러리의 일부입니다.

### 해결하는 핵심 문제

문서는 간단한 연산의 효율성 문제를 설명합니다. `list.sum() / list.size`를 사용하여 평균을 계산하면 컬렉션을 두 번 순회합니다. 더 큰 데이터셋이나 `Sequence` 또는 `Flow`와 같은 지연 구조의 경우, 각 순회마다 요소가 다시 계산되므로 이는 점점 더 문제가 됩니다.

```kotlin
// 비효율적: 리스트를 두 번 순회
val average = list.sum() / list.size
```

### 해결책: 수집기 패턴

수집기는 집계 방법을 데이터 순회와 분리해 정의합니다. 여러 집계를 `zip`으로 결합하면 한 번의 순회에서 함께 계산할 수 있습니다. 아래에서는 합계와 요소 수를 구하는 수집기를 연결해 평균을 계산합니다:

```kotlin
import arrow.collectors.Collectors
import arrow.collectors.collect
import arrow.collectors.zip

val averageCollector = zip(Collectors.sum, Collectors.length, ::divide)
val average = list.collect(averageCollector)
```

### 설계 영향

API는 두 가지 소스에서 영감을 받았습니다:

- Java: 표준 [`Collector`](https://docs.oracle.com/javase/8/docs/api/java/util/stream/Collector.html) 인터페이스
- Haskell: 함수형 연산을 위한 [`foldl` 라이브러리](https://hackage.haskell.org/package/foldl/docs/Control-Foldl.html)

### 연산 카테고리

시퀀스로 작업할 때, 연산은 두 그룹으로 나뉩니다:

- 변환: `map`, `filter`, `distinct` (중간 연산)
- 소비: `sum`, `size` (결과를 생성하는 터미널 연산)

수집기는 데이터를 소비하는 연산을 담당하고, 변환에는 Kotlin의 기존 `Sequence`와 `Flow` 연산을 사용합니다.

### 설치 참고

Android 사용자는 호환성을 위해 라이브러리 디슈가링이 필요합니다.

---

## 재귀 함수 (Recursive Functions)

> 원문: [collections-functions/recursive](https://arrow-kt.io/learn/collections-functions/recursive/)

### 개요

재귀 호출이 깊어지면 JVM의 제한된 스택 공간이 문제가 됩니다. 이 절에서는 호출 스택의 한계를 피하는 방법과, 그 뒤에도 남는 중복 계산을 줄이는 방법을 차례로 살펴봅니다.

### 깊은 재귀의 문제점

함수형 알고리즘은 루프보다 재귀를 선호하지만, JVM은 스택 공간이 제한되어 있습니다. 깊은 재귀 호출은 - 적당한 크기의 입력에도 - `StackOverflowError`를 발생시킬 수 있습니다.

### 스택 안전 솔루션

Arrow는 "호출 스택을 힙에 보관하여 일반적으로 훨씬 더 큰 메모리 공간이 할당되는" Kotlin의 내장 `DeepRecursiveFunction`을 활용합니다. 함수를 직접 호출하는 대신, 재귀 호출에 `callRecursive()`를 사용합니다.

다음 피보나치 수열 예제로 이 방식을 살펴봅니다.

```kotlin
import kotlin.DeepRecursiveFunction

val fibonacciWorker = DeepRecursiveFunction<Int, Int> { n ->
    when (n) {
        0 -> 0
        1 -> 1
        else -> callRecursive(n - 1) + callRecursive(n - 2)
    }
}

// 사용법
val result = fibonacciWorker(10) // 55
```

### 효율성을 위한 메모이제이션

앞의 피보나치 함수는 스택 문제를 피하더라도 같은 하위 문제를 반복해서 계산합니다. `MemoizedDeepRecursiveFunction`은 순수 함수의 결과를 캐시하므로, 이미 구한 하위 문제의 결과를 재사용할 수 있습니다.

```kotlin
import arrow.core.MemoizedDeepRecursiveFunction

val memoizedFibonacci = MemoizedDeepRecursiveFunction<Int, Int> { n ->
    when (n) {
        0 -> 0
        1 -> 1
        else -> callRecursive(n - 1) + callRecursive(n - 2)
    }
}
```

### 설정

라이브러리는 고급 제거 정책을 위한 cache4k 통합을 포함하여 `cache` 매개변수를 통한 캐시 커스터마이제이션을 제공합니다. 이는 장기 실행 애플리케이션에서 무제한 메모리 소비를 방지합니다.

위치: 선택적 `arrow-cache4k` 통합과 함께 `arrow-core`에서 사용 가능합니다.

---

## 메모이제이션 (Memoization)

> 원문: [collections-functions/memoize](https://arrow-kt.io/learn/collections-functions/memoize/)

### 개요

메모이제이션은 결과를 캐싱하여 순수 함수를 최적화하는 기술입니다. Arrow 문서에서 설명하듯이: "동일한 입력이 주어지면 항상 동일한 출력을 생성하고, 다른 효과를 생성하지 않습니다." 메모이제이션된 함수가 이전에 본 입력으로 호출되면, 다시 계산하는 대신 캐시된 결과를 반환합니다.

### 간단한 메모이제이션

Arrow Core는 모든 함수를 캐시된 버전으로 변환하는 `memoize`라는 유틸리티를 제공합니다:

```kotlin
import arrow.core.memoize

fun expensive(x: Int): Int {
    Thread.sleep(x * 100L)
    return x
}

val memoizedExpensive = ::expensive.memoize()

// 첫 번째 호출: 계산 수행
val first = memoizedExpensive(5) // 500ms 대기

// 두 번째 호출: 캐시에서 즉시 반환
val second = memoizedExpensive(5) // 즉시 반환
```

이후 같은 인자로 호출하면 캐시된 결과를 즉시 반환합니다.

### 핵심 고려사항

메모리 트레이드오프: 메모이제이션된 함수를 `val`로 정의할 때, 캐시는 실행 전체에 걸쳐 지속됩니다. 문서는 "이로 인해 전체 실행 중에 회수할 수 없는 메모리가 발생할 수 있다"고 경고하며, 신중한 적용이 필요합니다.

### 재귀 문제

일반 재귀 함수에 `memoize()`를 붙일 때는 내부 호출도 살펴봐야 합니다. 아래 `fibonacci`는 재귀할 때 캐시된 함수가 아닌 원래 함수를 호출하므로, 내부의 중복 계산은 그대로 남습니다.

```kotlin
// 이것은 제대로 작동하지 않습니다!
fun fibonacci(n: Int): Int = when (n) {
    0 -> 0
    1 -> 1
    else -> fibonacci(n - 1) + fibonacci(n - 2) // 메모이제이션되지 않은 버전 호출
}

val memoizedFib = ::fibonacci.memoize()
// 재귀 호출은 여전히 원본 fibonacci를 호출합니다
```

### 권장사항

재귀 호출에도 캐시를 적용하려면 앞서 본 `MemoizedDeepRecursiveFunction`을 사용할 수 있습니다. 재귀 계산의 중복을 줄이면서 스택 오버플로우도 방지하도록 설계된 함수입니다.

---

## 평가 제어 (Eval)

> 원문: [collections-functions/eval](https://arrow-kt.io/learn/collections-functions/eval/)

### 개요

Arrow의 `Eval` 타입은 값을 계산하는 시점과 결과를 재사용할지를 지정합니다. 계산을 즉시 수행할지, 값이 필요해질 때까지 미룰지에 따라 아래 전략을 선택할 수 있습니다.

### 네 가지 평가 전략

`Eval` 타입은 네 가지 구별되는 접근 방식을 지원합니다:

1. Eval.now - 계산을 즉시 실행합니다 (즉시 평가)
2. Eval.later - 첫 번째 접근까지 실행을 지연한 다음 결과를 캐시합니다
3. Eval.atMostOnce - 동시 접근에서도 단일 실행을 보장합니다
4. Eval.always - 각 접근 시 계산을 다시 실행합니다

```kotlin
import arrow.eval.Eval

// 즉시 평가
val now: Eval<Int> = Eval.now(1 + 1) // 바로 계산됨

// 지연 평가 (캐시됨)
val later: Eval<Int> = Eval.later {
    println("Computing...")
    1 + 1
}

// 항상 재계산
val always: Eval<Int> = Eval.always {
    println("Computing again...")
    1 + 1
}

// 값 가져오기
val result = later.value() // "Computing..." 출력, 2 반환
val result2 = later.value() // 출력 없음, 캐시에서 2 반환
```

### 주요 사용 사례: 스택 안전성

주요 이점은 재귀 호출이 깊어질 때 드러납니다. 문서는 "`Eval`의 주요 사용 사례 중 하나는 스택 안전성입니다. 즉, 깊은 재귀 연산에서 스택 오버플로우를 방지합니다."라고 설명합니다.

다음 예제는 `Eval.always`와 `flatMap`을 함께 사용해 짝수와 홀수를 재귀적으로 판별합니다. 일반적인 재귀 구현에서 스택 오버플로우를 일으킬 만큼 큰 숫자(예: 100,000)도 처리할 수 있습니다.

```kotlin
import arrow.eval.Eval

fun even(n: Int): Eval<Boolean> =
    Eval.always { n == 0 }.flatMap {
        if (it) Eval.now(true)
        else odd(n - 1)
    }

fun odd(n: Int): Eval<Boolean> =
    Eval.always { n == 0 }.flatMap {
        if (it) Eval.now(false)
        else even(n - 1)
    }

// 스택 오버플로우 없이 작동!
val isEven = even(100000).value()
```

### 모범 사례

문서는 중요한 지침을 제공합니다:

- Eval 인스턴스에서 패턴 매칭을 피하세요; 대신 `map`과 `flatMap`을 사용하세요
- 중첩된 Eval 인스턴스 내에서 `value()`를 호출하지 마세요, 이는 스택 오버플로우 보호를 약화시킵니다
- 최종 결과를 검색할 때만 `value()`를 사용하세요

### 라이브러리 위치

`Eval` 기능은 `arrow-eval` 라이브러리에 있습니다.

---

## 함수 유틸리티 (Utilities for Functions)

> 원문: [collections-functions/utils](https://arrow-kt.io/learn/collections-functions/utils/)

Arrow는 함수를 값으로 조작하기 위한 함수형 프로그래밍 유틸리티를 제공하지만, 이러한 패턴은 관용적인 Kotlin으로 간주되지 않습니다.

### 합성 (Composition)

함수 합성은 앞 함수의 결과를 다음 함수의 입력으로 연결합니다. `f(a: A): B`와 `g(b: B): C`가 있다면 `g compose f`는 `(a: A) -> C` 함수를 만듭니다. 표기상 오른쪽의 `f`를 먼저 적용하고 왼쪽의 `g`에 그 결과를 전달하는 순서입니다.

```kotlin
import arrow.core.compose

val addOne: (Int) -> Int = { it + 1 }
val double: (Int) -> Int = { it * 2 }

// g compose f: 먼저 f를 적용한 다음 g를 적용
val addOneThenDouble = double compose addOne

val result = addOneThenDouble(3) // (3 + 1) * 2 = 8
```

### 부분 적용 (Partial Application)

부분 적용은 여러 인자를 받는 함수에서 일부 인자를 고정하고, 나머지 인자를 받는 함수를 만드는 기법입니다. Arrow는 "항상 왼쪽부터 인자를 고정하는" `partiallyN` 함수를 제공합니다. 예를 들어, `{ dance(2, it) }` 대신 `::dance.partially1(2)`를 작성할 수 있습니다. 이러한 유틸리티는 `arrow-functions` 라이브러리의 일부입니다.

```kotlin
import arrow.core.partially1

fun dance(times: Int, style: String): String =
    "Dancing $style $times times"

// 부분 적용: 첫 번째 인자 고정
val danceTwice = ::dance.partially1(2)

val result = danceTwice("tango") // "Dancing tango 2 times"
```

### 커링 (Currying)

커링은 여러 인자를 동시에 받는 함수를 순차적으로 인자를 받는 함수로 변환합니다. 이는 `(A, B) -> C`를 `(A) -> (B) -> C`로 변환하며, 첫 번째 인자를 적용하면 두 번째를 기다리는 함수를 반환합니다.

```kotlin
import arrow.core.curried

fun add(a: Int, b: Int): Int = a + b

val curriedAdd = ::add.curried()

// 순차적으로 인자 적용
val addFive = curriedAdd(5)
val result = addFive(3) // 8

// 또는 한 번에
val result2 = curriedAdd(5)(3) // 8
```

### 중요 참고사항

문서는 이러한 패턴이 "관용적인 Kotlin으로 간주되지 않습니다. 대부분의 Kotlin 개발자는 Haskell과 같은 언어에서 일반적인 함수 조작 접근 방식보다 명시적 호출이 있는 블록을 선호합니다."라고 강조합니다.

```kotlin
// 관용적인 Kotlin 스타일 (선호됨)
val result = { style: String -> dance(2, style) }

// 함수형 스타일 (덜 일반적)
val result = ::dance.partially1(2)
```
