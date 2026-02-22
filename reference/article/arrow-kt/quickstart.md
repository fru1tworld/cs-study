# Arrow-kt 빠른 시작 가이드 및 개요

> Arrow는 함수형, 데이터 지향, 동시성 프로그래밍 원칙에서 영감을 받은 Kotlin 여정의 완벽한 동반자입니다.

Arrow는 백엔드, 프론트엔드, JVM, Android, 멀티플랫폼 환경 전반에서 개발을 지원합니다. 이 라이브러리는 `Either`와 `Validated` 타입을 사용한 에러 처리와 같은 실용적인 솔루션을 강조하여 개발자가 예상치 못한 예외를 제거하고 시스템 신뢰성을 향상시킬 수 있도록 합니다.

---

## 목차

1. [개요](#개요)
2. [타입화된 에러 (Typed Errors)](#타입화된-에러-typed-errors)
3. [동시성과 리소스](#동시성과-리소스)
4. [복원력 (Resilience)](#복원력-resilience)
5. [불변 데이터](#불변-데이터)

---

## 개요

Arrow는 "Kotlin 여정의 완벽한 동반자"로 자리매김하며, 데이터 수정 및 리소스 관리와 같은 실용적인 개발자 작업에 초점을 맞추면서 Kotlin의 코루틴과 통합됩니다.

### 핵심 기능

Arrow는 Kotlin 개발자들에게 여러 핵심 기능을 제공합니다:

- 타입화된 에러: 예외에 의존하는 대신 "구조화되고, 예측 가능하며, 효율적인 도메인 에러 처리"를 가능하게 합니다.
- 동시성과 리소스: 코루틴으로 작업하고 리소스와 공유 데이터를 올바르게 관리하기 위한 "고수준 유틸리티"를 제공합니다.
- 복원력: 개발자가 "코드가 최소한의 수고로 조직화된 방식으로 실패에 대응"하고 코루틴과 통합되도록 도와줍니다.
- Optics를 사용한 불변 데이터: "불변 데이터와 봉인된 계층 구조를 다루기 위한 훌륭한 도구"를 제공합니다.
- 컬렉션과 함수: 코드를 더 표현력 있게 만들기 위한 "기본 기능에 대한 강력한 추가 기능"을 제공합니다.
- 디자인 패턴: 도메인 모델과 아키텍처에 "함수형 및 데이터 지향 프로그래밍 개념"을 통합합니다.

---

## 타입화된 에러 (Typed Errors)

Arrow는 개발자가 `Either<Error, Success>`와 같은 구조를 사용하여 함수 시그니처에서 잠재적인 도메인 에러를 선언할 수 있게 합니다.

### 핵심 개념

#### 논리적 실패 vs 실제 예외

문서에서는 두 가지 범주를 구분합니다:

- 논리적 실패: 예상되는 도메인 특정 문제 (예: 저장소에서 사용자를 찾을 수 없음)
- 실제 예외: 도메인 외부의 기술적 문제 (예: 데이터베이스 연결 끊김)

#### 두 가지 주요 접근 방식

래퍼 타입 - 값으로 표현되는 에러:
```kotlin
fun findUser(id: UserId): Either<UserNotFound, User>
```

계산 컨텍스트 - 실행 컨텍스트의 일부인 에러:
```kotlin
fun Raise<UserNotFound>.findUser(id: UserId): User
```

### Fail-First 동작

`Raise` DSL에서 `bind()` 연산을 사용하면 실패 시 계산을 중단합니다:

```kotlin
val user: Either<UserNotFound, User> = User(1).right()
fun Raise<UserNotFound>.user(): User = User(1)
```

### 에러 발생시키기

```kotlin
val error: Either<UserNotFound, User> = UserNotFound.left()
fun Raise<UserNotFound>.error(): User = raise(UserNotFound)
```

### 검증 함수

- ensure: 조건을 검증하고, 만족하지 않으면 에러를 발생시킵니다
- ensureNotNull: null 여부를 확인하고 non-null 값을 스마트 캐스트합니다

### 검사 및 결과

```kotlin
when (result) {
  is Left -> // 에러 처리
  is Right -> // 성공 처리
}

fold(block, recover, transform)
```

### 에러 복구

#### 논리적 실패로부터 복구

```kotlin
fetchUser(-1).getOrElse { e: UserNotFound -> null }
recover({ fetchUser(1) }) { e: UserNotFound -> null }
```

#### 예외로부터 복구

`catch` DSL은 외부 코드를 래핑하고 예외를 타입화된 에러로 변환합니다:

```kotlin
catch({
  UsersQueries.insert(username, email)
}) { e: SQLException ->
  if (e.isUniqueViolation()) raise(UserAlreadyExists(username, email))
  else throw e
}
```

### 에러 축적 (Error Accumulation)

`either { accumulate { } }` 패턴은 첫 번째 실패에서 멈추지 않고 여러 검증 에러를 동시에 수집합니다. 이는 "사용자 입력을 검증할 때 한 번에 최대한 많은 문제를 보고하고 싶은 경우"에 유용합니다.

#### 여러 값에 대한 축적

```kotlin
(1..10).mapOrAccumulate { isEven(it) }
```

#### 다른 계산들

zipOrAccumulate 사용:
```kotlin
zipOrAccumulate(
  { ensure(name.isNotEmpty()) { UserProblem.EmptyName } },
  { ensure(age >= 0) { UserProblem.NegativeAge(age) } }
) { _, _ -> User(name, age) }
```

accumulate 블록 사용:
```kotlin
accumulate {
  ensureOrAccumulate(name.isNotEmpty()) { UserProblem.EmptyName }
  ensureOrAccumulate(age >= 0) { UserProblem.NegativeAge(age) }
  User(name, age)
}
```

### 에러 변환

`withError`를 사용하여 에러 타입을 변환합니다:

```kotlin
val intError: Either<Int, Boolean> = either {
  withError({ it.length }) {
    stringError.bind()
  }
}
```

### 타입화된 에러의 장점

1. 타입 안전성 - 컴파일러가 불일치를 조기에 감지합니다
2. 예측 가능성 - 에러 조건이 시그니처에 명시적으로 표시됩니다
3. 합성 가능성 - 함수 호출을 통한 쉬운 전파
4. 성능 - 예외 처리보다 더 효율적입니다

---

## 동시성과 리소스

프레임워크는 Kotlin 코루틴 위에 고수준 추상화를 구축합니다.

### 병렬 처리

#### parZip

독립적인 계산을 병렬로 결합합니다. "각 계산을 나타내는 인자들의 시퀀스가 있고, 마지막에 결과를 처리하는 하나의 최종 블록"이 있습니다.

```kotlin
suspend fun getUser(id: UserId): User = parZip(
  { getUserName(id) },
  { getAvatar(id) }
) { name, avatar -> User(name, avatar) }
```

#### parMap

`parMap()`은 설정 가능한 제한으로 여러 작업을 동시에 실행합니다:

```kotlin
suspend fun getFriendNames(id: UserId): List<User> =
  getFriendIds(id).parMap { getUserName(it) }
```

`parMap` 함수는 Flow에도 제공됩니다. 동시성이 1을 초과하면 내부 flow가 동시에 실행됩니다. parMapUnordered는 출력 순서가 소스 순서와 일치할 필요가 없을 때 성능 향상을 제공합니다.

#### 실험적 awaitAll

구조화된 동시성 내에서 async/await 패턴을 사용하는 대안 문법을 제공합니다:

```kotlin
suspend fun getUser(id: UserId): User = awaitAll {
  val name = async { getUserName(id) }
  val avatar = async { getAvatar(id) }
  User(name.await(), avatar.await())
}
```

### 레이싱 (Racing)

여러 계산이 동시에 실행되고, 첫 번째로 성공한 결과가 승리합니다.

프레임워크는 세 가지 핵심 동작을 구현합니다:

1. 첫 번째 성공이 승리 - 초기 성공 값이 레이스를 종료합니다
2. 예외 처리 - 실패는 기록되지만 레이스를 종료하지 않습니다; 다른 참가자들은 계속됩니다
3. 리소스 정리 - 승자가 나타나면 모든 활성 참가자들이 즉시 취소되어 획득한 리소스가 제대로 닫히도록 합니다

#### raceN을 사용한 간단한 레이싱

2-3개의 계산이 있는 기본 시나리오의 경우:

```kotlin
suspend fun file(server1: String, server2: String) =
  raceN(
    { downloadFrom(server1) },
    { downloadFrom(server2) }
  ).merge()
```

이는 `Either<A, B>`를 반환하여 두 브랜치가 같은 타입을 공유할 때 병합할 수 있습니다.

#### 고급 레이싱 DSL

Arrow는 저수준 `select` 표현식의 복잡성을 단순화하는 실험적 고수준 DSL을 제공합니다:

```kotlin
suspend fun getUserRacing(id: UserId): User = racing {
  race { RemoteCache.getUser(id) }
  race { LocalCache.getUser(id) }
}
```

타임아웃 처리: 무한 대기를 방지하기 위해 지연을 추가합니다:
```kotlin
race {
  delay(10.milliseconds)
  throw TimeoutException()
}
```

조건부 레이싱: 결과가 기준을 충족해야 하는 조건을 추가합니다:
```kotlin
race(condition = { it.size == ids.size }) {
  LocalCache.getCachedUsers(ids)
}
```

### 리소스 관리

`resourceScope` DSL은 예외 상황에서도 `AutoCloseable` 리소스의 적절한 정리를 보장합니다.

Arrow의 Resource DSL은 특히 여러 상호 의존적인 리소스에서 할당 및 해제 문제를 해결합니다. "예외와 취소 상황에서도 적절한 종료를 보장"하면서 Kotlin 코루틴 및 구조화된 동시성과 통합됩니다.

#### 핵심 문제

전통적인 리소스 관리에는 상당한 격차가 있습니다:

- 예외나 취소 중 수동 정리는 리소스 누수 위험이 있습니다
- `AutoCloseable`/`Closeable`은 JVM과 인터페이스 구현이 필요합니다
- 콜백 중첩은 합성 가능성을 줄입니다
- 메서드 이름이 강제됩니다 (예: `close()`)
- 동기 종료자는 suspend 함수를 실행할 수 없습니다
- 성공적인 완료, 에러 또는 취소를 나타내는 신호가 없습니다

#### 해결책: 3단계 접근 방식

리소스는 세 단계를 따릅니다: 획득, 사용, 해제. Arrow는 획득과 해제를 묶어 예외나 취소에 관계없이 정확성을 보장합니다.

방법 1: `resourceScope` DSL

`install` 함수는 획득과 해제를 모두 관리합니다. 실행이 어떻게 종료되었는지를 나타내는 `ExitCase` 매개변수를 받습니다:

```kotlin
suspend fun ResourceScope.userProcessor(): UserProcessor =
  install({ UserProcessor().also { it.start() } }) { p, _ -> p.shutdown() }

suspend fun example(): Unit = resourceScope {
  val service = userProcessor()
  service.processData()
}
```

방법 2: `Resource<T>` 값

재사용 가능한 리소스 레시피를 값으로 정의합니다:

```kotlin
val dataSource: Resource<DataSource> = resource({
  DataSource().also { it.connect() }
}) { ds, exitCase ->
  println("Releasing $ds with exit: $exitCase")
  ds.close()
}
```

#### 주요 특성

- 리소스는 `NonCancellable`로 획득됩니다 - 획득이 실패하면 해제가 트리거되지 않습니다
- 성공적으로 획득된 리소스는 항상 해제됩니다
- `either`와 같은 타입화된 에러 빌더와 원활하게 통합됩니다
- `closeable` 함수를 통해 Java의 `AutoCloseable`을 지원합니다
- `parZip`과 같은 연산을 사용하여 병렬 획득을 허용합니다

### 트랜잭션: 소프트웨어 트랜잭션 메모리 (STM)

`TVar` 원자 변수와 롤백 기능을 가진 소프트웨어 트랜잭션 메모리 (STM)입니다.

STM은 데드락이나 경쟁 조건 없이 안전한 동시 접근을 가능하게 하는 "동시 상태 수정을 위한 추상화"입니다. `arrow-fx-stm` 라이브러리가 이 개념을 구현합니다.

#### 핵심 컴포넌트

TVar (트랜잭션 변수): "타입 A의 값을 보유하지만 동시 수정이 보호되는" 변수를 나타내는 기본 빌딩 블록입니다.

주요 프리미티브: `retry`, `orElse`, `catch`

#### 기본 연산

트랜잭션은 `atomically()`를 통해 실행되는 설명입니다. 읽기와 쓰기는 STM 컨텍스트 내에서 발생합니다:

```kotlin
fun STM.transfer(from: TVar<Int>, to: TVar<Int>, amount: Int) {
  withdraw(from, amount)
  deposit(to, amount)
}
```

속성 위임이 접근을 단순화합니다:
```kotlin
var acc by accVar  // 암시적 읽기/쓰기
```

#### 추가 데이터 구조

Arrow는 `TQueue`, `TMVar`, `TSet`, `TMap`, `TArray`, `TSemaphore`를 제공합니다. 각각은 표준 타입을 래핑하는 대신 특정 사용 사례에 최적화되어 있습니다.

#### 고급 기능

재시도: `retry()`를 사용하면 잘못된 상태를 만난 트랜잭션을 중단하고, 접근된 변수가 변경되면 자동으로 재시작합니다.

분기: `orElse`는 재시도가 트리거될 때 대체 경로를 제공합니다.

예외: 에러 시 상태 변경을 적절히 롤백하려면 표준 try-catch 대신 STM의 `catch` 함수를 사용합니다.

#### 중요한 주의사항

트랜잭션은 여러 번 재시작될 수 있으므로, 예상치 못한 재실행을 피하려면 작게 유지하고 부작용이 없도록 합니다.

### 타입화된 에러 통합

- 람다 내에서: 에러가 지역화된 상태로 유지됩니다; 다른 작업들은 계속됩니다
- Raise DSL 내에서: 에러가 모든 병렬 작업의 취소를 트리거합니다
- parMapOrAccumulate: 단락 평가 대신 모든 에러를 축적합니다

프레임워크는 "작업 중 하나가 실패할 때마다 예외를 전파하고 실행 중인 계산을 취소"하도록 보장합니다.

---

## 복원력 (Resilience)

실패를 처리하기 위한 두 가지 핵심 패턴입니다.

Arrow의 복원력 섹션은 분산 환경에서의 시스템 실패를 다룹니다. 문서에 따르면 "복원력은 이러한 이벤트가 발생할 때 시스템이 조직화된 방식으로 행동하는 능력입니다."

라이브러리는 현대 시스템이 예측 불가능하게 실패할 수 있는 여러 서비스에 의존한다는 것을 인식합니다. 규범적인 솔루션 대신 Arrow는 복원력 전략을 설계하기 위한 합성 가능한 도구를 제공합니다.

### 핵심 복원력 도구

`arrow-resilience` 라이브러리는 세 가지 주요 메커니즘을 제공합니다:

#### 1. 재시도 및 반복 (Retry and Repeat)

`Schedule` 패턴을 사용하여 계산 재시도를 활성화하여 시스템이 일시적인 실패에서 복구할 수 있도록 합니다.

두 가지 운영 모드가 있습니다:

1. Retry: "액션을 한 번 실행하고, 실패하면 스케줄링 정책에 따라 성공하거나 정책이 종료될 때까지 재시도합니다"
2. Repeat: "액션을 실행하고, 성공하면 스케줄링 정책에 따라 실패하거나 정책이 종료될 때까지 계속 실행합니다"

정책 구성 방법:

기본 정책:
- `Schedule.recurs<A>(n)` - n회 실행
- `Schedule.exponential<Unit>(duration)` - 시도 간 증가하는 지연
- `Schedule.spaced<A>(duration)` - 일정한 지연 간격

고급 합성은 `andThen`, `doWhile`, `jittered`와 같은 연산자를 통해 정책을 결합하여 "60초까지 지수 백오프, 그 후 무작위화와 함께 100회 시도에 대해 일정한 60초 지연"과 같은 정교한 패턴을 만듭니다.

예외 특정 재시도:

버전 2.0부터 특정 예외 타입에 대한 재시도 시도를 제한할 수 있습니다:

```kotlin
policy.retry<IllegalArgumentException, _> { ... }
```

#### 2. 서킷 브레이커 (Circuit Breaker)

시스템이 비정상적일 때 연쇄 실패를 방지하여 다운스트림 서비스를 과부하로부터 보호합니다.

서킷 브레이커는 서비스가 악화될 때 빠른 실패를 통해 시스템 과부하를 방지합니다. 재시도 메커니즘과 결합할 때 특히 효과적이며 분산 시스템에서 연쇄 실패를 방지하는 데 도움이 됩니다.

세 가지 상태:

Closed (초기 상태)
"이 상태에서는 요청이 정상적으로 이루어집니다" 실패 추적과 함께. 실패가 `maxFailures` 임계값을 초과하면 브레이커가 Open으로 전환됩니다. 성공적인 요청은 카운터를 0으로 재설정합니다.

Open (빠른 실패 상태)
"서킷 브레이커가 모든 요청을 단락/빠른 실패시킵니다" `ExecutionRejected` 예외를 던지면서. `resetTimeout`이 경과한 후 테스트를 위해 Half-Open으로 이동합니다.

Half-Open (테스트 상태)
다른 요청들은 빠른 실패하는 동안 단일 테스트 요청이 허용됩니다. 성공하면 카운터가 재설정되고 Closed로 돌아갑니다. 실패하면 `resetTimeout`에 지수 백오프가 적용되고 Open으로 돌아갑니다.

개방 전략:

Arrow는 두 가지 접근 방식을 제공합니다:

1. 카운트 전략: 연속 실패가 임계값을 초과하면 Open을 트리거합니다. 각 성공은 카운터를 재설정합니다.

2. 슬라이딩 윈도우 전략: 시간 창 내의 실패를 계산합니다. 해당 기간 내의 실패가 임계값을 초과하면 창 외부의 성공에 관계없이 Open됩니다.

구현:

`openingStrategy`, `resetTimeout`, `exponentialBackoffFactor`, `maxResetTimeout`을 포함한 매개변수로 `CircuitBreaker` 생성자를 사용하여 인스턴스를 생성합니다.

`protectOrThrow()` 또는 `protectEither()`를 사용하여 서비스 호출을 보호합니다. 후자는 에러 처리를 위해 `Either<ExecutionRejected, A>`를 반환합니다.

중요: 동일한 서비스에 접근하는 여러 동시 스레드는 일치하는 매개변수를 가진 인스턴스가 아닌 동일한 서킷 브레이커 인스턴스가 필요합니다.

#### 3. 사가 (Saga)

서비스 경계를 넘어 보상 트랜잭션을 관리하며 분산 시스템에서 트랜잭션 의미론을 구현합니다.

### 설계 철학

Arrow는 복원력 접근 방식이 컨텍스트에 따라 달라진다고 강조합니다:
- 요청을 재시도할 수 있는가?
- 치명적인 에러가 관리자 알림을 트리거해야 하는가?
- 어떤 보상 전략이 필요한가?

프레임워크는 복원력을 모놀리식 솔루션이 아닌 합성 가능한 도구 키트로 취급합니다.

---

## 불변 데이터

Arrow의 optics 라이브러리는 중첩된 불변 구조를 업데이트하는 것을 단순화합니다.

Arrow는 더 큰 객체 내의 중첩된 필드에 대한 접근 지점을 나타내는 값인 optics를 통해 불변 중첩 데이터 구조를 변환하기 위한 라이브러리 솔루션을 제공합니다.

### 문제 설명

표준 Kotlin 데이터 클래스는 깊게 중첩된 필드를 수정하기 위해 장황한 중첩 `copy()` 호출이 필요합니다:

```kotlin
fun Person.capitalizeCountry(): Person =
  this.copy(
    address = address.copy(
      city = address.city.copy(
        country = address.city.country.capitalize()
      )
    )
  )
```

### 해결책: Optics

Optics는 합성을 통해 이를 단순화합니다. 클래스에 `@optics`로 주석을 달고 companion object를 포함한 후:

```kotlin
@optics data class Person(val name: String, val age: Int, val address: Address) {
  companion object
}
```

두 가지 사용 패턴이 나타납니다:

Modify 연산: "optic의 `modify` 연산은 전체 값과 적용할 변환"을 받습니다.

```kotlin
fun Person.capitalizeCountryModify(): Person =
  Person.address.city.country.modify(this) { it.capitalize() }
```

Copy 빌더: copy 블록 내에서 `transform` 문법을 사용합니다.

```kotlin
fun Person.capitalizeCountryCopy(): Person =
  this.copy {
    Person.address.city.country transform { it.capitalize() }
  }
```

### Optics 계층 구조

포커스 카디널리티에 따라 계층을 형성하는 다섯 가지 주요 optic 타입이 있습니다:

- Traversal: 여러 요소
- Optional: 0개 또는 1개의 요소
- Lens: 정확히 1개의 요소
- Prism: 값 생성/매칭
- Iso: 양방향 변환

### 컬렉션 순회

직접 수정은 번거로운 중첩 `copy()` 호출을 피합니다. 컬렉션 순회는 불변성을 유지하면서 리스트 전체에 변환을 적용합니다.

### 기술 요구 사항

구현에는 다음이 필요합니다:
- `arrow-optics` 라이브러리
- `arrow-optics-ksp-plugin` 컴파일러 플러그인

---

## 시작하기

문서에서는 세 가지 다음 단계를 권장합니다:
1. 프로젝트 설정
2. 예제 프로젝트 탐색
3. 특정 주제 영역 심층 탐구

자세한 내용은 [Arrow 공식 문서](https://arrow-kt.io/learn/)를 참조하세요.

---

## 추가 리소스

- [API 문서](https://arrow-kt.io/docs/)
- [블로그](https://arrow-kt.io/blog/)
- [예제 프로젝트](https://arrow-kt.io/learn/design/)
- [커뮤니티](https://arrow-kt.io/community/)
  - Twitter
  - Slack
  - GitHub
  - Stack Overflow

---

*이 문서는 Arrow가 커뮤니티에 의해 개발되고 유지된다는 것을 나타내며, 이는 Kotlin 개발자를 위한 협업 오픈 소스 프로젝트임을 시사합니다.*
