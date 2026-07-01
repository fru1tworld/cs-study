# 스케줄링과 재시도: Schedule

> 원본: https://zio.dev/reference/schedule/

---

## 목차

1. [Schedule 소개(Introduction)](#1-schedule-소개introduction)
2. [반복(Repetition)](#2-반복repetition)
3. [재시도(Retrying)](#3-재시도retrying)
4. [내장 스케줄(Built-in Schedules)](#4-내장-스케줄built-in-schedules)
5. [스케줄 조합자(Schedule Combinators)](#5-스케줄-조합자schedule-combinators)
6. [예제(Examples)](#6-예제examples)
7. [참고 자료](#7-참고-자료)

---

## 1. Schedule 소개(Introduction)

`Schedule[Env, In, Out]`는 **반복적인(recurring) 효과적(effectful) 스케줄을 기술(describe)하는 불변 값**(immutable value)입니다. 이 스케줄은 어떤 환경(environment) `Env`에서 실행되며, 타입 `In`의 값(재시도(`retry`)의 경우에는 오류(error), 반복(`repeat`)의 경우에는 값)을 소비(consume)한 후, 타입 `Out`의 값을 생성(produce)합니다. 그리고 매 단계(step)마다 입력 값(input value)과 내부 상태(internal state)에 기반하여, 중단(halt)할지 아니면 어떤 지연(delay) **d** 이후에 계속(continue)할지를 결정합니다.

스케줄(Schedule)은 시간(time)에 걸쳐 펼쳐진, 잠재적으로 무한한(possibly infinite) 구간(interval)들의 집합으로 정의됩니다. 각 구간은 반복(recurrence)이 가능한 윈도우(window)를 정의합니다.

[반복(Repetition)](#2-반복repetition)과 [재시도(Retrying)](#3-재시도retrying)는 스케줄링(scheduling) 영역에서 유사한 두 가지 개념입니다. 이 둘은 동일한 개념이자 아이디어이며, 다만 하나는 성공(success)을 찾고 다른 하나는 실패(failure)를 찾는다는 점만 다릅니다.

스케줄이 효과(effect)를 반복하거나 재시도하는 데 사용될 때, 스케줄이 생성하는 각 구간의 시작 경계(starting boundary)가 그 효과가 다시 실행될 시점(moment)으로 사용됩니다.

스케줄은 유연한 반복 스케줄(flexible recurrence schedule)을 정의하고 조합(compose)할 수 있게 해 주며, 이를 이용해 액션(action)을 **반복**(repeat)하거나, 오류가 발생한 경우에 액션을 **재시도**(retry)할 수 있습니다.

스케줄을 변환(transform)하고 결합(combine)하기 위한 다양한 [조합자(combinator)](#5-스케줄-조합자schedule-combinators)가 존재하며, `Schedule`의 컴패니언 객체(companion object)에는 재시도와 반복을 수행하기 위한 [모든 일반적인 유형의 스케줄](#4-내장-스케줄built-in-schedules)이 들어 있습니다.

### 함께 보기(See Also)

- **ZStream 스케줄링(ZStream Scheduling)** — 설정 가능한 스케줄 정책(schedule policy)을 사용하여 스트림 출력의 방출 타이밍(emission timing)과 간격(spacing)을 제어하는 ZStream 스케줄링 조합자입니다.
- **TestAspect: 반복과 재시도(Repetition and Retrying)** — 지정된 스케줄에 따라 테스트를 반복하거나 재시도하기 위한 테스트 애스펙트(test aspect)입니다.

> 이후 모든 예제는 다음 임포트를 전제로 합니다.
>
> ```scala
> import zio._
> ```

---

## 2. 반복(Repetition)

반복(repetition)의 경우, ZIO에는 `ZIO#repeat` 함수가 있습니다. 이 함수는 스케줄을 반복 정책(repetition policy)으로 받아, 그 정책에 따른 반복 전략(repetition strategy)을 가진 효과를 기술하는 또 다른 효과를 반환합니다.

반복 정책은 다음 함수들에서 사용됩니다.

- `ZIO#repeat` — 스케줄이 완료될 때까지 효과를 반복합니다.
- `ZIO#repeatOrElse` — 스케줄이 완료될 때까지 효과를 반복하며, 오류에 대한 폴백(fallback)을 제공합니다.

> _**주의:**_
>
> 스케줄에 의한 반복 실행(scheduled recurrence)은 **첫 번째 실행에 추가로(in addition to)** 일어납니다. 따라서 `io.repeat(Schedule.once)`는 `io`를 실행하고, 그것이 성공하면 `io`를 **한 번 더** 실행하는 효과를 만들어 냅니다.

`ZIO#repeat` 함수로 반복 효과(repeated effect)를 만드는 방법을 살펴보겠습니다.

```scala
val action:      ZIO[R, E, A] = ???
val policy: Schedule[R1, A, B] = ???

val repeated = action repeat policy
```

오류 발생 시 폴백 전략(fallback strategy)을 제공하는 또 다른 버전의 `repeat`도 있습니다. `ZIO#repeatOrElse` 함수를 사용하면 반복 실패 시 실행될 `orElse` 콜백(callback)을 지정할 수 있습니다.

```scala
val action:       ZIO[R, E, A] = ???
val policy: Schedule[R1, A, B] = ???

val orElse: (E, Option[B]) => ZIO[R1, E2, B] = ???

val repeated = action repeatOrElse (policy, orElse)
```

---

## 3. 재시도(Retrying)

재시도(retrying)의 경우, ZIO에는 `ZIO#retry` 함수가 있습니다. 이 함수는 스케줄을 반복 정책으로 받아, 원래 효과의 실패(failure)에 뒤이어 재시도를 수행하는 반복 전략을 가진 효과를 기술하는 또 다른 효과를 반환합니다.

반복 정책은 다음 함수들에서 사용됩니다.

- `ZIO#retry` — 효과가 성공할 때까지 재시도합니다.
- `ZIO#retryOrElse` — 효과가 성공할 때까지 재시도하며, 오류에 대한 폴백을 제공합니다.

`ZIO#retry` 함수로 재시도 효과를 만드는 방법을 살펴보겠습니다.

```scala
val action:       ZIO[R, E, A] = ???
val policy: Schedule[R1, E, S] = ???

val repeated = action retry policy

```

오류 발생 시 폴백 전략을 제공하는 또 다른 버전의 `retry`도 있습니다. `ZIO#retryOrElse` 함수를 사용하면 재시도 실패 시 실행될 `orElse` 콜백을 지정할 수 있습니다.

```scala
val action:       ZIO[R, E, A] = ???
val policy: Schedule[R1, A, B] = ???

val orElse: (E, S) => ZIO[R1, E1, A1] = ???

val repeated = action retryOrElse (policy, orElse)
```

> **반복(repeat)과 재시도(retry)의 차이**: 두 함수 모두 동일한 `Schedule` 메커니즘을 사용하지만, `repeat`는 효과가 **성공할 때마다** 스케줄을 적용해 반복하고, `retry`는 효과가 **실패할 때마다** 스케줄을 적용해 다시 시도합니다. 즉, `repeat`에서 스케줄의 입력 타입 `In`은 효과의 성공 값(`A`)이고, `retry`에서 스케줄의 입력 타입 `In`은 효과의 오류(`E`)입니다.

---

## 4. 내장 스케줄(Built-in Schedules)

`Schedule`의 컴패니언 객체는 효과의 반복을 제어하기 위한 여러 내장 스케줄을 제공합니다. 고정 간격(fixed interval), 지수 백오프(exponential backoff), 피보나치 기반(fibonacci-based) 지연 등 다양한 지연 전략(delay strategy)이 포함됩니다.

### 4.1 succeed

지정된 상수 값(constant value)을 생성하면서 **한 번 반복**하는 스케줄을 반환합니다.

```scala
val constant = Schedule.succeed(5)
```

### 4.2 fromFunction

**항상 반복**하며, 입력 값을 지정된 함수를 통해 매핑(mapping)하는 스케줄입니다.

```scala
val inc = Schedule.fromFunction[Int, Int](_ + 1)
```

### 4.3 stop

반복하지 않고, 그냥 멈춘 뒤 하나의 `Unit` 요소를 반환하는 스케줄입니다.

```scala
val stop = Schedule.stop
```

### 4.4 once

**한 번 반복**하고 하나의 `Unit` 요소를 반환하는 스케줄입니다.

```scala
val once = Schedule.once
```

### 4.5 forever

**항상 반복**하며, 매 실행마다 반복 횟수(number of recurrence)를 생성하는 스케줄입니다.

```scala
val forever = Schedule.forever
```

### 4.6 recurs

지정된 횟수(specified number of times)만큼만 반복하는 스케줄입니다.

```scala
val recurs = Schedule.recurs(5)
```

### 4.7 spaced

연속적으로 반복하되, 각 반복(repetition)이 직전 실행으로부터 지정된 지속 시간(duration)만큼 간격을 두고(spaced) 일어나는 스케줄입니다.

```scala
val spaced = Schedule.spaced(10.milliseconds)
```

### 4.8 fixed

**고정 간격**(fixed interval)으로 반복하는 스케줄입니다. 지금까지의 스케줄 반복 횟수(number of repetitions)를 반환합니다.

```scala
val fixed = Schedule.fixed(10.seconds)
```

> `spaced`와 `fixed`의 차이: `spaced`는 직전 실행이 **끝난 시점**으로부터 지정된 시간만큼 간격을 두지만, `fixed`는 실행의 소요 시간과 무관하게 **고정된 주기**(period)에 맞춰 반복합니다.

### 4.9 exponential

**지수 백오프**(exponential backoff)를 사용하여 반복하는 스케줄입니다.

```scala
val exponential = Schedule.exponential(10.milliseconds)
```

### 4.10 fibonacci

**항상 반복**하며, 직전 두 지연(preceding two delays)을 합산하여 지연을 증가시키는(피보나치 수열과 유사) 스케줄입니다. 반복 간의 현재 지속 시간(current duration)을 반환합니다.

```scala
val fibonacci = Schedule.fibonacci(10.milliseconds)
```

### 4.11 identity

**항상 계속(continue)하기로 결정**하는 스케줄입니다. 어떠한 지연도 없이 영원히 반복합니다. `identity` 스케줄은 입력을 소비하고, 그것과 동일한 것을 출력으로 방출합니다(`Schedule[Any, A, A]`).

```scala
val identity = Schedule.identity[Int]
```

### 4.12 unfold

지정된 상태(state)와 반복자(iterator)로부터 **한 번 반복**하는 스케줄입니다.

```scala
val unfold = Schedule.unfold(0)(_ + 1)
```

---

## 5. 스케줄 조합자(Schedule Combinators)

스케줄은 상태를 가지며(stateful), 잠재적으로 효과적인(possibly effectful) 반복 이벤트 스케줄을 정의하고, 다양한 방식으로 조합(compose)할 수 있습니다. 조합자(combinator)는 여러 스케줄을 결합하여 새로운 스케줄을 만들어 줍니다. 적절한 조합자를 갖추면, 몇 가지 기본 스케줄(base schedule)과 조합자만으로도 매우 다양한 상황에 대응할 수 있습니다.

### 5.1 합성(Composition)

스케줄은 다음과 같은 주요 방식으로 합성됩니다.

- **합집합(Union)**: 두 스케줄의 구간들의 합집합(union)을 수행합니다.
- **교집합(Intersection)**: 두 스케줄의 구간들의 교집합(intersection)을 수행합니다.
- **순차 연결(Sequencing)**: 한 스케줄의 구간 위에 다른 스케줄의 구간을 이어 붙입니다(concatenate).

#### 합집합(Union)

두 스케줄을 합집합으로 결합합니다. 두 스케줄 중 **어느 하나라도** 반복하기를 원하면 반복하며, 두 지연 중 **최솟값**(minimum)을 반복 간 지연으로 사용합니다.

|                     | `s1`                | `s2`                | `s1` &#124;&#124; `s2`    |
|---------------------|---------------------|---------------------|---------------------------|
| 타입(Type)          | `Schedule[R, A, B]` | `Schedule[R, A, C]` | `Schedule[R, A, (B, C)]`  |
| 계속(Continue): `Boolean` | `b1`          | `b2`                | `b1` &#124;&#124; `b2`    |
| 지연(Delay): `Duration`   | `d1`          | `d2`                | `d1.min(d2)`              |
| 방출(Emit): `(A, B)`      | `a`           | `b`                 | `(a, b)`                  |

`||` 연산자로 두 스케줄을 합집합으로 결합할 수 있습니다.

```scala
val expCapped = Schedule.exponential(100.milliseconds) || Schedule.spaced(1.second)
```

#### 교집합(Intersection)

두 스케줄을 교집합으로 결합합니다. 두 스케줄이 **모두** 반복하기를 원하는 경우에만 반복하며, 두 지연 중 **최댓값**(maximum)을 반복 간 지연으로 사용합니다.

|                     | `s1`                | `s2`                | `s1 && s2`               |
|---------------------|---------------------|---------------------|--------------------------|
| 타입(Type)          | `Schedule[R, A, B]` | `Schedule[R, A, C]` | `Schedule[R, A, (B, C)]` |
| 계속(Continue): `Boolean` | `b1`          | `b2`                | `b1 && b2`               |
| 지연(Delay): `Duration`   | `d1`          | `d2`                | `d1.max(d2)`             |
| 방출(Emit): `(A, B)`      | `a`           | `b`                 | `(a, b)`                 |

`&&` 연산자로 두 스케줄을 교집합으로 결합할 수 있습니다.

```scala
val expUpTo10 = Schedule.exponential(1.second) && Schedule.recurs(10)
```

#### 순차 연결(Sequencing)

두 스케줄을 순차적으로 결합합니다. 첫 번째 정책(first policy)이 끝날 때까지 그것을 따른 다음, 두 번째 정책(second policy)을 따릅니다.

|                   | `s1`                | `s2`                | `s1 andThen s2`     |
|-------------------|---------------------|---------------------|---------------------|
| 타입(Type)        | `Schedule[R, A, B]` | `Schedule[R, A, C]` | `Schedule[R, A, C]` |
| 지연(Delay): `Duration` | `d1`          | `d2`                | `d1 + d2`           |
| 방출(Emit): `B`   | `a`                 | `b`                 | `b`                 |

`andThen`을 사용하여 두 스케줄을 순차적으로 연결할 수 있습니다.

```scala
val sequential = Schedule.recurs(10) andThen Schedule.spaced(1.second)
```

### 5.2 파이핑(Piping)

첫 번째 스케줄의 출력을 두 번째 스케줄의 입력으로 파이핑(pipe)하여 두 스케줄을 결합합니다. 첫 번째 스케줄이 기술하는 효과는 항상 두 번째 스케줄이 기술하는 효과보다 먼저 실행됩니다.

|                   | `s1`                | `s2`                | `s1 >>> s2`         |
|-------------------|---------------------|---------------------|---------------------|
| 타입(Type)        | `Schedule[R, A, B]` | `Schedule[R, B, C]` | `Schedule[R, A, C]` |
| 지연(Delay): `Duration` | `d1`          | `d2`                | `d1 + d2`           |
| 방출(Emit): `B`   | `a`                 | `b`                 | `b`                 |

`>>>` 연산자를 사용하여 두 스케줄을 파이핑할 수 있습니다.

```scala
val totalElapsed = Schedule.spaced(1.second) <* Schedule.recurs(5) >>> Schedule.elapsed
```

### 5.3 지터링(Jittering)

`jittered`는 하나의 스케줄을 받아, 지연(delay)이 무작위로(randomly) 적용된다는 점을 제외하고 동일한 타입의 또 다른 스케줄을 반환하는 조합자입니다.

| 함수(Function) | 입력 타입(Input Type)       | 출력 타입(Output Type)               |
|----------------|----------------------------|--------------------------------------|
| `jittered`     |                            | `Schedule[Env with Random, In, Out]` |
| `jittered`     | `min: Double, max: Double` | `Schedule[Env with Random, In, Out]` |

어떤 스케줄이든 `jittered`를 호출하여 지터(jitter)를 적용할 수 있습니다.

```scala
val jitteredExp = Schedule.exponential(10.milliseconds).jittered
```

리소스가 과부하(overload)나 경합(contention)으로 인해 사용 불가능 상태가 되었을 때, 재시도와 백오프(backoff)만으로는 충분하지 않습니다. 실패한 API 호출이 모두 동일한 시점에 백오프되면, 그 자체가 또 다른 과부하나 경합을 유발하기 때문입니다. 지터(Jitter)는 스케줄의 지연에 약간의 무작위성(randomness)을 추가하여, 재시도가 우연히 동기화되어 서비스를 다시 다운시키는 상황을 방지합니다.

`min`과 `max` 매개변수를 가진 형태는, 새 구간 크기(interval size)가 `min * 기존 구간`과 `max * 기존 구간` 사이에서 무작위로 분포되는 새로운 스케줄을 만듭니다.

[연구](https://aws.amazon.com/blogs/architecture/exponential-backoff-and-jitter/)에 따르면 `Schedule.jittered(0.0, 1.0)`이 재시도에 매우 적합합니다.

### 5.4 수집(Collecting)

`collectAll`은 어떤 스케줄에 대해 호출하면, 첫 번째 스케줄의 출력들을 청크(chunk)로 수집하는 새 스케줄을 만들어 내는 조합자입니다.

| 함수(Function) | 입력 타입(Input Type)    | 출력 타입(Output Type)          |
|----------------|--------------------------|---------------------------------|
| `collectAll`   | `Schedule[Env, In, Out]` | `Schedule[Env, In, Chunk[Out]]` |

다음 예제에서는 스케줄의 모든 반복(recurrence) 출력을 `Chunk`로 수집하므로, 최종적으로 `Chunk(0, 1, 2, 3, 4)`가 됩니다.

```scala
val collect = Schedule.recurs(5).collectAll
```

### 5.5 필터링(Filtering)

`whileInput`과 `whileOutput`을 사용하여 스케줄의 입력(input)이나 출력(output)을 필터링할 수 있습니다. 또한 ZIO 스케줄에는 이 두 함수의 효과적(effectful) 버전인 `whileInputZIO`와 `whileOutputZIO`도 있습니다.

| 함수(Function)   | 입력 타입(Input Type)        | 출력 타입(Output Type)     |
|------------------|------------------------------|----------------------------|
| `whileInput`     | `In1 => Boolean`             | `Schedule[Env, In1, Out]`  |
| `whileOutput`    | `Out => Boolean`             | `Schedule[Env, In, Out]`   |
| `whileInputZIO`  | `In1 => URIO[Env1, Boolean]` | `Schedule[Env1, In1, Out]` |
| `whileOutputZIO` | `Out => URIO[Env1, Boolean]` | `Schedule[Env1, In, Out]`  |

다음 예제에서는 출력이 5가 되기 전까지 방출되는 모든 출력을 수집하므로, `Chunk(0, 1, 2, 3, 4)`가 됩니다.

```scala
val res = Schedule.unfold(0)(_ + 1).whileOutput(_ < 5).collectAll
```

### 5.6 매핑(Mapping)

스케줄을 매핑하는 두 가지 버전, `map`과 그 효과적 버전인 `mapZIO`가 있습니다.

| 함수(Function) | 입력 타입(Input Type)        | 출력 타입(Output Type)     |
|----------------|------------------------------|----------------------------|
| `map`          | `f: Out => Out2`             | `Schedule[Env, In, Out2]`  |
| `mapZIO`       | `f: Out => URIO[Env1, Out2]` | `Schedule[Env1, In, Out2]` |

### 5.7 좌/우 적용(Left/Right Ap)

`&&` 연산자로 두 스케줄을 교집합할 때, 왼쪽 또는 오른쪽 출력을 무시(ignore)하고 싶은 경우가 있습니다.

- `*>` — 왼쪽 출력(left output)을 무시합니다.
- `<*` — 오른쪽 출력(right output)을 무시합니다.

### 5.8 수정(Modifying)

스케줄의 지연(delay)을 수정합니다.

```scala
val boosted = Schedule.spaced(1.second).delayed(_ => 100.milliseconds)
```

### 5.9 태핑(Tapping)

스케줄의 입력/출력을 효과적으로(effectfully) 처리해야 할 때 `tapInput`과 `tapOutput`을 사용할 수 있습니다. 로깅(logging) 용도로 주로 활용됩니다.

```scala
val tappedSchedule = Schedule.count.whileOutput(_ < 5).tapOutput(o => Console.printLine(s"retrying $o").orDie)
```

---

## 6. 예제(Examples)

스케줄을 생성하고 조합하는 예제를 살펴보겠습니다.

1. **지정된 시간이 경과한 후 재시도 중단하기**:

```scala
val expMaxElapsed = (Schedule.exponential(10.milliseconds) >>> Schedule.elapsed).whileOutput(_ < 30.seconds)
```

이 스케줄은 지수 백오프(`exponential`)의 출력을 경과 시간(`elapsed`)으로 파이핑한 뒤, 경과 시간이 30초 미만인 동안에만(`whileOutput(_ < 30.seconds)`) 계속 반복합니다. 즉, 총 경과 시간이 30초를 넘으면 재시도를 중단합니다.

2. **특정 예외(exception)가 발생했을 때에만 재시도하기**:

```scala
import scala.concurrent.TimeoutException

val whileTimeout = Schedule.exponential(10.milliseconds) && Schedule.recurWhile[Throwable] {
  case _: TimeoutException => true
  case _ => false
}
```

이 스케줄은 지수 백오프와, 입력 오류가 `TimeoutException`인 동안에만 반복하는 스케줄(`recurWhile`)을 교집합(`&&`)으로 결합합니다. 따라서 `TimeoutException`이 발생하는 경우에만 지수 백오프 지연으로 재시도하고, 다른 예외에서는 재시도하지 않습니다.

---

## 7. 참고 자료

- [Introduction to Scheduling ZIO Effects](https://zio.dev/reference/schedule/)
- [Repetition](https://zio.dev/reference/schedule/repetition)
- [Retrying](https://zio.dev/reference/schedule/retrying)
- [Built-in Schedules](https://zio.dev/reference/schedule/built-in-schedules)
- [Schedule Combinators](https://zio.dev/reference/schedule/combinators)
- [Examples](https://zio.dev/reference/schedule/examples)
- [Exponential Backoff and Jitter (AWS Architecture Blog)](https://aws.amazon.com/blogs/architecture/exponential-backoff-and-jitter/)
