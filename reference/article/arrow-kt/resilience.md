# Arrow-kt Resilience (복원력)

> 이 문서는 [Arrow-kt 공식 문서](https://arrow-kt.io/learn/resilience/)의 Resilience 섹션을 한국어로 번역한 것입니다.

## 목차

1. [개요](#개요)
2. [재시도와 반복 (Retry and Repeat)](#재시도와-반복-retry-and-repeat)
3. [서킷 브레이커 (Circuit Breaker)](#서킷-브레이커-circuit-breaker)
4. [사가 (Saga)](#사가-saga)

---

## 개요

마이크로서비스를 다룰 때, 서비스 간 통신으로 인해 시스템 실패가 발생할 수 있습니다. 복원력(Resilience)이란 이러한 이벤트가 발생했을 때 시스템이 체계적으로 대응할 수 있는 능력을 의미합니다.

Arrow는 미리 정의된 솔루션 대신 툴킷으로서 복원력을 제공합니다. 이를 통해 여러분의 솔루션을 간결하고 조합 가능한 방식으로 설계할 수 있습니다.

### 주요 복원력 메커니즘

Arrow는 세 가지 핵심 복원력 메커니즘을 제공합니다:

| 메커니즘 | 설명 |
|---------|------|
| 재시도와 반복 (Retry and Repeat) | `Schedule`을 활용하여 연산을 재시도하거나 반복 |
| 서킷 브레이커 (Circuit Breaker) | 다른 서비스가 과부하되는 것을 방지 |
| 사가 (Saga) | 분산 시스템에서 트랜잭션 동작 구현 |

### 라이브러리

모든 복원력 도구는 `arrow-resilience` 라이브러리에서 찾을 수 있습니다.

```kotlin
// build.gradle.kts
dependencies {
    implementation("io.arrow-kt:arrow-resilience:1.2.0")
}
```

### 추가 참고 자료

- [Microservices.io 패턴 문서](https://microservices.io/patterns/)
- [Microsoft Azure 클라우드 디자인 패턴 가이드](https://docs.microsoft.com/azure/architecture/patterns/)

---

## 재시도와 반복 (Retry and Repeat)

`Schedule`은 재시도(retry)와 반복(repeat) 작업을 처리하기 위한 Arrow의 핵심 API입니다. 이 프레임워크를 통해 실패한 액션을 자동으로 재시도하거나 성공한 액션을 지정된 조건에 따라 반복하는 정책을 정의할 수 있습니다.

### 핵심 개념

두 가지 주요 작업이 있습니다:

- `retry`: 액션을 한 번 실행한 후, 실패 시 스케줄링 정책에 따라 재시도합니다.
- `repeat`: 액션을 성공적으로 반복 실행하며, 정책이 중단을 결정할 때까지 계속됩니다.

### 정책 구성

`Schedule` 컴패니언 객체는 재시도/반복 정책을 구성하는 메서드를 제공합니다.

#### 기본 정책 예제

10번 반복:
```kotlin
fun <A> recurTenTimes() = Schedule.recurs<A>(10)
```

지수 백오프 (Exponential Backoff):
```kotlin
@ExperimentalTime
val exponential = Schedule.exponential<Unit>(250.milliseconds)
```

#### 복합 정책 - 여러 전략 조합

다음은 지수 백오프(최대 60초 제한), 간격 지연, 무작위 지터를 결합한 정교한 재시도 로직 예제입니다:

```kotlin
@ExperimentalTime
fun <A> complexPolicy(): Schedule<A, List<A>> =
  Schedule.exponential<A>(10.milliseconds)
    .doWhile { _, duration -> duration < 60.seconds }
    .andThen(Schedule.spaced<A>(60.seconds) and Schedule.recurs(100))
    .jittered()
    .zipRight(Schedule.identity<A>().collect())
```

이 정책은:
1. 10밀리초부터 시작하여 지수적으로 증가하되 60초를 넘지 않음
2. 이후 60초 간격으로 최대 100번 추가 시도
3. 무작위 지터를 적용하여 동시 재시도 충돌 방지
4. 모든 중간 결과를 수집

### 액션 반복하기

기본 반복 (총 4회 실행):
```kotlin
suspend fun example(): Unit {
  var counter = 0
  val res = Schedule.recurs<Unit>(3).repeat {
    counter++
  }
  counter shouldBe 4  // 초기 1회 + 3회 반복 = 4회
}
```

### 예외 타입 필터링

버전 2.0부터 특정 예외 하위 타입만 재시도하도록 지정할 수 있습니다:

```kotlin
policy.retry<IllegalArgumentException, _> {
    // IllegalArgumentException만 재시도
    // 다른 예외는 즉시 전파
}
```

### 결과 수집하기

반복 중 생성된 값을 처리하는 여러 방법이 있습니다:

#### 모든 결과 버리기
`Schedule.unit`을 사용하여 모든 결과를 버립니다.

#### 최종 결과만 유지
`Schedule.identity`를 사용하여 마지막 결과만 유지합니다.

#### 모든 중간 값 보존
`Schedule.collect`를 사용하여 모든 중간 값을 수집합니다.

왼쪽 또는 오른쪽 정책 출력 유지:
```kotlin
suspend fun example(): Unit {
  var counter = 0

  // 왼쪽 값 유지 (Unit)
  val keepLeft = (Schedule.identity<Unit>() zipLeft Schedule.recurs(3)).repeat {
    counter++
  }

  // 오른쪽 값 유지 (Unit)
  val keepRight = (Schedule.recurs<Unit>(3) zipRight Schedule.identity<Unit>()).repeat {
    counter++
  }

  counter shouldBe 8
  keepLeft shouldBe Unit
  keepRight shouldBe Unit
}
```

최종 결과 유지:
```kotlin
suspend fun example(): Unit {
  var counter = 0
  val keepLast = (Schedule.identity<String>() zipLeft Schedule.recurs(3)).repeat {
    counter++
    "$counter"
  }
  keepLast shouldBe "4"  // 마지막 실행의 결과
}
```

모든 중간 결과 수집:
```kotlin
suspend fun example(): Unit {
  var counter = 0
  val keepAll = (Schedule.collect<Int>() zipLeft Schedule.recurs(3)).repeat {
    counter++
    counter
  }
  keepAll shouldBe listOf(1, 2, 3, 4)  // 모든 실행의 결과
}
```

### 조건부 실행

`doWhile`과 `doUntil` 정책을 사용하여 액션의 출력에 따라 조건부로 반복할 수 있습니다.

조건이 충족되는 동안 반복:
```kotlin
suspend fun example(): Unit {
  var result = ""
  Schedule.doWhile<String> { input, _ ->
    input.length <= 5  // 길이가 5 이하인 동안 계속
  }.repeat {
    result += "a"
    result
  }
  result shouldBe "aaaaaa"  // 6번째에서 조건 불충족으로 종료
}
```

### 스케줄 정책 요약

| 메서드 | 설명 |
|--------|------|
| `recurs(n)` | 최대 n번 반복 |
| `exponential(base)` | 지수적으로 증가하는 지연 |
| `spaced(duration)` | 고정 간격 지연 |
| `linear(base)` | 선형적으로 증가하는 지연 |
| `fibonacci(base)` | 피보나치 수열 기반 지연 |
| `doWhile { }` | 조건이 참인 동안 계속 |
| `doUntil { }` | 조건이 참이 될 때까지 계속 |
| `jittered()` | 무작위 지터 추가 |
| `collect()` | 모든 결과 수집 |
| `identity()` | 마지막 결과 유지 |
| `and` / `or` | 정책 조합 |
| `andThen` | 순차적 정책 적용 |
| `zipLeft` / `zipRight` | 왼쪽/오른쪽 결과 유지 |

---

## 서킷 브레이커 (Circuit Breaker)

서킷 브레이커는 과부하된 서비스를 보호하기 위한 복원력 메커니즘입니다. 서비스가 과부하 상태일 때 추가적인 상호작용은 과부하 상태를 더욱 악화시킬 수 있습니다.

### 세 가지 상태

서킷 브레이커는 세 가지 상태로 작동합니다:

```
    ┌─────────┐     실패 임계값 초과     ┌─────────┐
    │  Closed │ ────────────────────> │  Open   │
    │ (정상)   │                       │(차단/실패)│
    └─────────┘                       └─────────┘
         ^                                  │
         │                                  │ 리셋 타임아웃 경과
         │     성공                          v
         │         ┌───────────┐           │
         └─────────│ Half-Open │<──────────┘
                   │ (테스트)    │
                   └───────────┘
                         │
                         │ 실패
                         v
                   (Open으로 복귀,
                    타임아웃 증가)
```

#### 1. Closed (정상 작동)

초기 상태로, 요청이 정상적으로 처리됩니다.

- 예외가 발생하면 실패 카운터가 증가합니다.
- 성공하면 카운터가 0으로 리셋됩니다.
- 실패 임계값을 초과하면 Open 상태로 전환됩니다.

#### 2. Open (즉시 실패)

실패가 임계값을 초과하면, 서킷 브레이커는 `ExecutionRejected` 예외를 던져 모든 요청을 거부합니다.

- 리셋 타임아웃이 경과하면 Half-Open 상태로 전환됩니다.
- 이 상태에서는 서비스 호출을 시도하지 않습니다.

#### 3. Half-Open (테스트)

단일 테스트 요청만 허용됩니다.

- 성공: Closed 상태로 복귀합니다.
- 실패: Open 상태로 복귀하며, 타임아웃이 지수 백오프 팩터만큼 증가합니다.

### 열림 전략 (Opening Strategies)

서킷을 열 시기를 결정하는 두 가지 전략이 있습니다:

#### Count (카운트 기반)

연속 실패 횟수를 추적합니다. 최대값을 초과하면 서킷이 열립니다.

```kotlin
OpeningStrategy.Count(maxFailures = 5)
```

#### Sliding Window (슬라이딩 윈도우)

특정 시간 창 내의 실패 횟수를 계산합니다. 해당 기간 동안 임계값을 초과하면 서킷이 열립니다.

```kotlin
OpeningStrategy.SlidingWindow(
    timeWindow = 30.seconds,
    maxFailures = 10
)
```

### 기본 사용 예제

```kotlin
@ExperimentalTime
suspend fun main(): Unit {
  val maxFailuresUntilOpen = 2

  val circuitBreaker = CircuitBreaker(
    openingStrategy = OpeningStrategy.Count(maxFailuresUntilOpen),
    resetTimeout = 2.seconds,          // Open에서 Half-Open으로 전환 대기 시간
    exponentialBackoffFactor = 1.2,    // Half-Open 실패 시 타임아웃 증가 배수
    maxResetTimeout = 60.seconds,      // 최대 리셋 타임아웃
  )

  // 정상 작동 - Closed 상태
  circuitBreaker.protectOrThrow {
    "I am in Closed: ${circuitBreaker.state()}"
  }.also(::println)

  // 과부하 시뮬레이션 - 실패 발생
  for (i in 1 .. maxFailuresUntilOpen + 1) {
    Either.catch {
      circuitBreaker.protectOrThrow {
        throw RuntimeException("Service overloaded")
      }
    }.also(::println)
  }

  // Open 상태 - 즉시 실패
  circuitBreaker.protectEither { }
    .also { println("I am Open and short-circuit with ${it}") }

  // 리셋 타임아웃 대기
  delay(2000)

  // Half-Open 상태 - 테스트 요청
  circuitBreaker.protectOrThrow {
    "I am running test-request in HalfOpen: ${circuitBreaker.state()}"
  }.also(::println)
}
```

### Schedule과 결합하기

서킷 브레이커와 재시도 스케줄을 결합하여 더욱 견고한 복원력 로직을 구현할 수 있습니다:

```kotlin
@ExperimentalTime
suspend fun main(): Unit {
  suspend fun apiCall(): Unit {
    throw RuntimeException("Overloaded service")
  }

  val circuitBreaker = CircuitBreaker(
    openingStrategy = OpeningStrategy.Count(2),
    resetTimeout = 2.seconds,
    exponentialBackoffFactor = 1.2,
    maxResetTimeout = 60.seconds,
  )

  // 복원력 있는 호출 함수
  suspend fun <A> resilient(
    schedule: Schedule<Throwable, *>,
    f: suspend () -> A
  ): A = schedule.retry {
    circuitBreaker.protectOrThrow(f)
  }

  // 최대 5번 재시도와 함께 서킷 브레이커 사용
  Either.catch {
    resilient(Schedule.recurs(5), ::apiCall)
  }
}
```

### 보호 메서드

| 메서드 | 설명 |
|--------|------|
| `protectOrThrow` | Open 상태일 때 `ExecutionRejected` 예외를 던짐 |
| `protectEither` | `Either<ExecutionRejected, A>`를 반환하여 안전한 처리 가능 |

### 중요 사항

> 주의: 여러 (동시) 스레드가 동일한 서비스에 접근하는 경우, 동일한 서킷 브레이커 인스턴스로 보호해야 합니다. 동일한 매개변수를 가진 다른 인스턴스가 아니라, 문자 그대로 같은 인스턴스여야 합니다.

```kotlin
// 올바른 사용: 단일 인스턴스 공유
val sharedCircuitBreaker = CircuitBreaker(...)

suspend fun service1() = sharedCircuitBreaker.protectOrThrow { /* ... */ }
suspend fun service2() = sharedCircuitBreaker.protectOrThrow { /* ... */ }

// 잘못된 사용: 각각 다른 인스턴스 (동일한 매개변수라도)
suspend fun service1() = CircuitBreaker(...).protectOrThrow { /* ... */ }
suspend fun service2() = CircuitBreaker(...).protectOrThrow { /* ... */ }
```

---

## 사가 (Saga)

사가 패턴은 분산 시스템에서 여러 작업이 성공하거나 함께 실패해야 하는 트랜잭션 동작을 구현합니다. 각 작업마다 보상 작업(compensating action)을 정의하여 실패 시 이전 상태로 롤백합니다.

### 핵심 개념

- Saga 함수: 보상 작업을 함께 선언할 수 있는 스코프를 생성합니다.
- transact 호출: 실제 작업을 실행합니다.
- 보상 작업: 작업 실패 시 변경사항을 취소하는 역 작업입니다.

### 기본 구조

```kotlin
val mySaga: Saga<Result> = saga {
  saga(
    action = { /* 정방향 작업 */ },
    compensate = { /* 보상/롤백 작업 */ }
  )
  // 더 많은 saga 작업들...

  // 최종 결과 반환
  result
}

// 트랜잭션 실행
mySaga.transact()
```

### 예제: 카운터 트랜잭션

Counter 클래스 구현:
```kotlin
import arrow.atomic.AtomicInt
import arrow.resilience.*

val INITIAL_VALUE = 1

object Counter {
  val value = AtomicInt(INITIAL_VALUE)

  fun increment() {
    value.incrementAndGet()
  }

  fun decrement() {
    value.decrementAndGet()
  }
}
```

보상 작업이 포함된 기본 Saga:
```kotlin
val PROBLEM = Throwable("problem detected!")

val transaction: Saga<Int> = saga {
  // 첫 번째 작업: increment, 보상: decrement
  saga({
    Counter.increment()
  }) {
    Counter.decrement()
  }

  // 두 번째 작업: 실패 발생
  saga({
    throw PROBLEM
  }) {
    // 이 작업은 실패하므로 보상 작업 없음
  }

  // 반환값
  Counter.value.get()
}
```

### 예외 처리와 함께 실행

```kotlin
suspend fun example() {
  val result = Either.catch { transaction.transact() }

  result shouldBe PROBLEM.left()

  // 보상 작업이 실행되어 원래 값으로 복원됨
  Counter.value.get() shouldBe INITIAL_VALUE
}
```

작동 순서:
1. `Counter.increment()` 실행 - 값이 2가 됨
2. `throw PROBLEM` 실행 - 예외 발생
3. 첫 번째 작업의 보상 `Counter.decrement()` 실행 - 값이 1로 복원
4. 예외가 호출자에게 전파됨

### Raise 패턴과 통합

Arrow의 Raise 패턴과 함께 사용하여 타입 안전한 에러 처리를 할 수 있습니다:

```kotlin
val PROBLEM = "문제 발생!"

fun Raise<String>.transaction(): Saga<Int> = saga {
  saga(
    { Counter.increment() },
    { Counter.decrement() }
  )
  saga(
    { raise(PROBLEM) },  // 타입 안전한 에러 발생
    {}
  )
  Counter.value.get()
}

suspend fun example() {
  val result = either { transaction().transact() }

  result shouldBe PROBLEM.left()
  Counter.value.get() shouldBe INITIAL_VALUE
}
```

### 다른 패턴과의 비교

#### Saga vs ResourceScope

| 특성 | Saga | ResourceScope |
|------|------|---------------|
| 정리 작업 실행 시점 | 실패 시에만 보상 작업 실행 | 항상 release 작업 실행 |
| 사용 사례 | 분산 트랜잭션 롤백 | 리소스 정리 (파일, 연결 등) |

```kotlin
// ResourceScope: 성공하든 실패하든 항상 release 실행
resourceScope {
  val file = install({ openFile() }) { file -> file.close() }
  // ...
}

// Saga: 실패 시에만 보상 작업 실행
saga {
  saga(
    { createOrder() },
    { cancelOrder() }  // 이후 작업 실패 시에만 실행
  )
  // ...
}
```

#### Saga vs STM (Software Transactional Memory)

| 특성 | Saga | STM |
|------|------|-----|
| 적합한 사용 사례 | 분산 시스템, 외부 서비스 호출 | 로컬 데이터 구조 조정 |
| 원자성 보장 방식 | 보상 작업을 통한 롤백 | 트랜잭션 재시도 |
| 성능 | 네트워크 오버헤드 있음 | 메모리 내에서 빠름 |

로컬 데이터 작업을 여러 액션에 걸쳐 조정해야 할 경우, Software Transactional Memory(STM)가 Saga와 원자적 참조를 조합하는 것보다 더 적합한 의미론을 제공합니다.

### 실제 사용 사례

주문 처리 시스템:
```kotlin
suspend fun processOrder(orderId: String): Saga<OrderResult> = saga {
  // 1. 재고 예약
  val inventory = saga(
    { inventoryService.reserve(orderId) },
    { inventoryService.release(orderId) }
  )

  // 2. 결제 처리
  val payment = saga(
    { paymentService.charge(orderId) },
    { paymentService.refund(orderId) }
  )

  // 3. 배송 예약
  val shipping = saga(
    { shippingService.schedule(orderId) },
    { shippingService.cancel(orderId) }
  )

  OrderResult(inventory, payment, shipping)
}

// 실행
suspend fun main() {
  val result = Either.catch {
    processOrder("order-123").transact()
  }

  when (result) {
    is Either.Left -> println("주문 실패, 모든 작업 롤백됨: ${result.value}")
    is Either.Right -> println("주문 성공: ${result.value}")
  }
}
```

---

## 정리

Arrow의 Resilience 라이브러리는 분산 시스템에서 발생할 수 있는 다양한 실패 상황에 대응하기 위한 세 가지 핵심 도구를 제공합니다:

| 도구 | 사용 시점 | 핵심 기능 |
|------|----------|-----------|
| Schedule (Retry/Repeat) | 일시적 실패가 예상될 때 | 지수 백오프, 지터, 조건부 재시도 |
| Circuit Breaker | 서비스 과부하 방지가 필요할 때 | 상태 기반 요청 차단, 자동 복구 |
| Saga | 분산 트랜잭션이 필요할 때 | 보상 작업을 통한 일관성 보장 |

이 도구들은 독립적으로 사용할 수도 있고, 함께 조합하여 더욱 견고한 복원력 로직을 구성할 수도 있습니다.

```kotlin
// 세 가지 도구의 조합 예시
suspend fun <A> resilientCall(
  circuitBreaker: CircuitBreaker,
  schedule: Schedule<Throwable, *>,
  action: suspend () -> A
): A = schedule.retry {
  circuitBreaker.protectOrThrow(action)
}

val orderSaga = saga {
  saga(
    { resilientCall(cb, schedule) { createOrder() } },
    { resilientCall(cb, schedule) { cancelOrder() } }
  )
}
```

### 의존성 추가

```kotlin
// build.gradle.kts
dependencies {
    implementation("io.arrow-kt:arrow-resilience:1.2.0")
}
```

```xml
<!-- Maven -->
<dependency>
    <groupId>io.arrow-kt</groupId>
    <artifactId>arrow-resilience</artifactId>
    <version>1.2.0</version>
</dependency>
```
