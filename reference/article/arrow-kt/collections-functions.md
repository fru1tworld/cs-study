# Arrow-kt 컬렉션과 함수

Arrow는 Kotlin 언어로 작업할 때 개발자의 생산성을 향상시키는 유틸리티를 제공하는 라이브러리입니다. 이 문서는 컬렉션과 함수형 프로그래밍 개념에 대한 Arrow의 기능을 설명합니다.

## 목차

1. [개요](#개요)
2. [비어있지 않은 컬렉션 (Non-empty Collections)](#비어있지-않은-컬렉션-non-empty-collections)
3. [수집기 (Collectors)](#수집기-collectors)
4. [재귀 함수 (Recursive Functions)](#재귀-함수-recursive-functions)
5. [메모이제이션 (Memoization)](#메모이제이션-memoization)
6. [평가 제어 (Eval)](#평가-제어-eval)
7. [함수 유틸리티 (Utilities for Functions)](#함수-유틸리티-utilities-for-functions)

---

## 개요

Arrow는 "Kotlin 여정의 완벽한 동반자"가 되는 것을 목표로 하며, Kotlin 언어로 작업할 때 개발자 생산성을 향상시키는 유틸리티를 제공합니다.

### 주요 카테고리

이 섹션은 6개의 핵심 영역으로 구성되어 있습니다:

1. 비어있지 않은 컬렉션 - 최소한 하나의 요소가 보장된 컬렉션 작업
2. 수집기 - 시퀀스에 대한 더 나은 집계 연산을 위한 도구
3. 재귀 함수 - 함수를 스택 안전하고 효율적으로 만드는 기술
4. 메모이제이션 - 순수 함수에서 중복 계산을 피하는 전략
5. 평가 제어 - 계산을 지연시키고 결과를 캐싱하는 방법
6. 함수 유틸리티 - 합성, 부분 적용, 커링을 지원하는 도구

---

## 비어있지 않은 컬렉션 (Non-empty Collections)

### 개요

비어있지 않은 컬렉션은 최소한 하나의 요소가 필요한 시나리오를 처리하기 위해 설계된 Arrow의 `arrow-core` 라이브러리 기능입니다. Arrow는 두 가지 주요 타입을 제공합니다:

- NonEmptyList: 최소한 하나의 요소를 포함하는 것이 보장된 리스트
- NonEmptySet: 최소한 하나의 요소를 포함하는 것이 보장된 집합

### 해결하는 핵심 문제

전통적인 컬렉션은 비어있을 수 있으며, 이는 오류 처리에서 문제가 되는 상황을 만듭니다. 예를 들어, `Either<List<Problem>, Result>`를 사용할 때, 이론적으로 문제가 0개인 `Left` 상태를 가질 수 있습니다 - 이는 논리적 불일치입니다. 비어있지 않은 컬렉션은 `mapOrAccumulate`와 `zipOrAccumulate`가 대신 `Either<NonEmptyList<Problem>, Result>`를 반환하도록 하여 이를 방지합니다.

### API 설계 원칙

비어있지 않은 컬렉션 API는 특정 연산을 강화하면서 Kotlin의 표준 `kotlin.collections` 규칙을 따릅니다:

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
- 크기 보존: `map`, `zip`과 같이 인자 크기를 존중하는 연산은 비어있지 않은 컬렉션을 반환합니다
- 스마트 연결: 적어도 하나가 비어있지 않은 컬렉션을 결합할 때, 결과는 비어있지 않은 상태를 유지합니다

```kotlin
import arrow.core.nonEmptyListOf

val nel = nonEmptyListOf(1, 2, 3)

// map은 NonEmptyList를 반환
val mapped: NonEmptyList<String> = nel.map { it.toString() }

// 비어있지 않은 컬렉션과 연결하면 비어있지 않은 결과 반환
val combined = nel + listOf(4, 5)
```

이 설계는 컬렉션의 비어있지 않음이 중요한 함수형 프로그래밍 워크플로우 전체에서 타입 안전성을 보장합니다.

---

## 수집기 (Collectors)

### 개요

수집기는 데이터가 한 번만 소비되도록 보장하면서 값의 시퀀스에 대한 복잡한 계산을 구축하기 위해 설계된 도구입니다. 이들은 실험적이지만 안정적인 `arrow-collectors` 라이브러리의 일부입니다.

### 해결하는 핵심 문제

문서는 간단한 연산의 효율성 문제를 설명합니다. `list.sum() / list.size`를 사용하여 평균을 계산하면 컬렉션을 두 번 순회합니다. 더 큰 데이터셋이나 `Sequence` 또는 `Flow`와 같은 지연 구조의 경우, 각 순회마다 요소가 다시 계산되므로 이는 점점 더 문제가 됩니다.

```kotlin
// 비효율적: 리스트를 두 번 순회
val average = list.sum() / list.size
```

### 해결책: 수집기 패턴

데이터를 여러 번 통과하는 대신, 수집기는 집계 로직을 실제 수집에서 분리합니다. 사용자는 `zip`을 사용하여 내장 수집기를 결합합니다:

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

수집기는 소비 카테고리를 다루며, Kotlin의 기존 `Sequence`와 `Flow`는 변환을 효과적으로 처리합니다.

### 설치 참고

Android 사용자는 호환성을 위해 라이브러리 디슈가링이 필요합니다.

---

## 재귀 함수 (Recursive Functions)

### 개요

이 섹션은 특히 JVM에서 스택 오버플로우 오류를 유발할 수 있는 깊은 재귀의 구현 과제를 다룹니다.

### 깊은 재귀의 문제점

함수형 알고리즘은 루프보다 재귀를 선호하지만, JVM은 스택 공간이 제한되어 있습니다. 깊은 재귀 호출은 - 적당한 크기의 입력에도 - `StackOverflowError`를 발생시킬 수 있습니다.

### 스택 안전 솔루션

Arrow는 "호출 스택을 힙에 보관하여 일반적으로 훨씬 더 큰 메모리 공간이 할당되는" Kotlin의 내장 `DeepRecursiveFunction`을 활용합니다. 함수를 직접 호출하는 대신, 재귀 호출에 `callRecursive()`를 사용합니다.

피보나치 수열이 이 접근 방식을 예시합니다:

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

`MemoizedDeepRecursiveFunction`은 순수 함수의 결과를 캐시하여 중복 계산을 제거합니다. 이는 동일한 하위 문제를 반복적으로 계산하는 피보나치와 같은 함수의 성능을 극적으로 향상시킵니다.

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

동일한 인자로 후속 호출하면 캐시에서 즉시 반환합니다.

### 핵심 고려사항

메모리 트레이드오프: 메모이제이션된 함수를 `val`로 정의할 때, 캐시는 실행 전체에 걸쳐 지속됩니다. 문서는 "이로 인해 전체 실행 중에 회수할 수 없는 메모리가 발생할 수 있다"고 경고하며, 신중한 적용이 필요합니다.

### 재귀 문제

재귀 함수에는 중요한 제한이 있습니다. 재귀적 피보나치 함수를 메모이제이션할 때, 재귀는 여전히 메모이제이션되지 않은 버전을 호출하여 캐싱 이점을 무효화합니다.

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

재귀 애플리케이션의 경우, Arrow는 기본 메모이제이션 대신 재귀 시나리오를 적절히 처리하고 스택 오버플로우를 방지하는 전문화된 `MemoizedDeepRecursiveFunction` 접근 방식을 사용할 것을 제안합니다.

---

## 평가 제어 (Eval)

### 개요

Arrow의 `Eval` 타입은 계산이 언제 어떻게 실행되는지를 관리하는 메커니즘을 제공합니다. 문서에 따르면, "`Eval`을 사용하면 값 또는 값을 생성하는 계산의 평가를 제어할 수 있습니다."

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

주요 이점은 깊은 재귀 시나리오를 다룹니다. 문서는 "`Eval`의 주요 사용 사례 중 하나는 스택 안전성입니다. 즉, 깊은 재귀 연산에서 스택 오버플로우를 방지합니다."라고 설명합니다.

예제는 `flatMap`과 짝을 이룬 `Eval.always`를 사용하여 재귀적으로 짝수/홀수를 확인하는 것을 보여주며, 일반적으로 스택 오버플로우를 유발하는 큰 숫자(예: 100,000)에 대한 연산을 가능하게 합니다.

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

Arrow는 함수를 값으로 조작하기 위한 함수형 프로그래밍 유틸리티를 제공하지만, 이러한 패턴은 관용적인 Kotlin으로 간주되지 않습니다.

### 합성 (Composition)

합성은 각 함수가 이전 결과를 소비하는 함수 파이프라인을 만듭니다. `f(a: A): B`와 `g(b: B): C` 함수의 경우, 합성 `g compose f`는 `(a: A) -> C`를 생성합니다. 중요한 점은 여러 합성을 연결할 때 "계산이 오른쪽에서 왼쪽 순서로 적용됩니다."

```kotlin
import arrow.core.compose

val addOne: (Int) -> Int = { it + 1 }
val double: (Int) -> Int = { it * 2 }

// g compose f: 먼저 f를 적용한 다음 g를 적용
val addOneThenDouble = double compose addOne

val result = addOneThenDouble(3) // (3 + 1) * 2 = 8
```

### 부분 적용 (Partial Application)

이 기술은 다중 인자 함수의 일부 인자를 고정하고 나머지는 지정하지 않습니다. Arrow는 "항상 왼쪽부터 인자를 고정하는" `partiallyN` 함수를 제공합니다. 예를 들어, `{ dance(2, it) }` 대신 `::dance.partially1(2)`를 작성할 수 있습니다. 이러한 유틸리티는 `arrow-functions` 라이브러리의 일부입니다.

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

---

## 요약

Arrow의 컬렉션과 함수 섹션은 Kotlin 개발을 위한 강력한 함수형 프로그래밍 도구를 제공합니다:

| 기능 | 라이브러리 | 주요 용도 |
|------|----------|---------|
| NonEmptyList/Set | arrow-core | 타입 안전한 비어있지 않은 컬렉션 |
| Collectors | arrow-collectors | 효율적인 단일 패스 집계 |
| DeepRecursiveFunction | arrow-core | 스택 안전 재귀 |
| Memoization | arrow-core | 순수 함수 결과 캐싱 |
| Eval | arrow-eval | 지연 평가와 스택 안전성 |
| Function Utils | arrow-functions | 합성, 커링, 부분 적용 |

이러한 도구들은 함수형 프로그래밍 패러다임을 Kotlin에 도입하면서 JVM의 제약(스택 크기 등)을 우회하고 코드의 안전성과 효율성을 향상시킵니다.
