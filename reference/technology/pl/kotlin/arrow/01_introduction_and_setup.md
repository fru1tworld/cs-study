# 01. Arrow 소개와 설치

- 확인일: 2026-09-08.
- 코드: 개념 설명용 발췌 예제 포함. 생략한 타입, import, 프로젝트 설정이 있는 예제는 독립 실행 프로그램이 아님.
- 원본 문서와 코드의 라이선스: [Apache-2.0](LICENSE.arrow-website).



## 수록한 공식 문서

- [quickstart](https://arrow-kt.io/learn/quickstart/)
- [quickstart/setup](https://arrow-kt.io/learn/quickstart/setup/)
- [quickstart/libs](https://arrow-kt.io/learn/quickstart/libs/)
- [quickstart/from-fp](https://arrow-kt.io/learn/quickstart/from-fp/)
- [overview](https://arrow-kt.io/learn/overview/)
- [summary](https://arrow-kt.io/learn/summary/)

## 설치와 버전 관리

> 원문: [Setup](https://arrow-kt.io/learn/quickstart/setup/)

2026-09-08에 확인한 공식 설치 예제는 Arrow `2.2.3`을 사용합니다. [2.2.3 릴리스 공지](https://arrow-kt.io/community/blog/2026/06/04/arrow-2-2-3/)에 명시된 최소 Kotlin 버전은 `2.2.0`이므로 프로젝트의 Kotlin 버전을 먼저 확인합니다.

Maven Central을 등록한 뒤 필요한 모듈을 의존성에 추가합니다. Gradle에서는 개별 버전이나 버전 카탈로그로 관리할 수 있으며, 아래처럼 `arrow-stack` BOM으로 모듈 버전을 맞출 수도 있습니다.

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation(platform("io.arrow-kt:arrow-stack:2.2.3"))
    implementation("io.arrow-kt:arrow-core")
    implementation("io.arrow-kt:arrow-fx-coroutines")
    implementation("io.arrow-kt:arrow-resilience")
}
```

- Maven에서는 `dependencyManagement`에 `arrow-stack`을 `type=pom`, `scope=import`로 등록하고 실제 의존성을 별도로 선언함.
- KMP에서는 필요한 소스 세트에 의존성을 등록함. JVM 전용 연동 라이브러리의 지원 대상은 개별 모듈 확인 필요.
- Android에서는 library desugaring 설정을 확인함. 특히 `arrow-collectors`의 Java API 사용에 유의함.
- Arrow는 릴리스 당시 최신 의존성을 적극적으로 채택하므로, 기존 프로젝트에 도입할 때 Kotlin, 코루틴, 직렬화 라이브러리의 호환성 확인 필요.

### Optics 설정

KSP로 Optics 접근자를 생성하려면 `arrow-optics`와 같은 버전의 `arrow-optics-ksp-plugin`을 추가합니다. 아래의 KSP 버전은 공식 문서의 예시이므로, 프로젝트의 Kotlin과 Gradle 환경에 맞게 선택합니다.

```kotlin
plugins {
    id("com.google.devtools.ksp") version "2.3.4"
}

dependencies {
    implementation("io.arrow-kt:arrow-optics:2.2.3")
    ksp("io.arrow-kt:arrow-optics-ksp-plugin:2.2.3")
}
```

- 기존 KSP 방식에서는 `@optics` 대상 클래스에 `companion object` 선언 필요.
- 별도의 [Arrow Optics Gradle 플러그인](https://arrow-kt.io/community/blog/2025/11/01/arrow-optics-gradle/)은 설정과 companion 생성을 자동화하는 베타 기능임.
- Maven의 KSP 연동은 공식 지원 대상이 아니므로 별도 연동 도구 필요.
- IntelliJ용 Arrow 플러그인은 누락한 `bind`, 부적절한 Raise 스코프 사용 등 탐지에 도움을 줌.
- 개발 빌드는 `-alpha.` 접미사를 사용하며, 안정 버전과 구분해 선택함.

## 라이브러리 구성

> 원문: [Library reference](https://arrow-kt.io/learn/quickstart/libs/)

- `arrow-core`: Raise, Either, Option, 비어 있지 않은 컬렉션, 메모이제이션을 적용한 재귀 함수.
- `arrow-core-high-arity`: 인자가 많은 오류 누적 연산.
- `arrow-fx-coroutines`: 병렬 실행, 경쟁 실행, suspend 리소스 관리.
- `arrow-autoclose`: suspend 없이 사용하는 리소스 관리.
- `suspendapp`, `suspendapp-ktor`: 애플리케이션과 Ktor 서버의 종료 처리.
- `arrow-resilience`: Schedule, CircuitBreaker, Saga.
- `arrow-optics`, `arrow-optics-ksp-plugin`: 불변 데이터 접근과 수정, 접근자 생성.
- `arrow-fx-stm`: 트랜잭션으로 공유 상태 관리.
- `arrow-collectors`: 한 번의 순회로 여러 집계 수행.
- `arrow-eval`: 평가 시점과 재평가 여부 제어.
- `arrow-functions`: 함수 합성, 부분 적용, 커링. 모듈 분리 후에도 일부 패키지 이름은 `arrow.core`를 사용함.
- `arrow-atomic`: 원자적 참조. 표준 라이브러리 공통 atomics와의 관계는 동시성 장 참고.
- `arrow-exception-utils`: 예외 처리 공통 유틸리티.
- 직렬화, Result4k, Retrofit, Compose 등 연동 모듈은 09장 참고.

### Overview 페이지

> 원문: [Overview](https://arrow-kt.io/learn/overview/)

- 현재 원본은 독립 본문 없이 Library reference로 이동하는 페이지임.
- 전체 53개 URL의 누락 여부를 확인하기 위해 목록에 포함함.

## 다른 함수형 언어에서 옮겨 오기

> 원문: [From other FP languages](https://arrow-kt.io/learn/quickstart/from-fp/)

Scala의 for comprehension이나 Haskell의 do 표기법으로 연결하던 오류 계산은 `either { ... }`와 `bind()`로 작성합니다. `bind()`의 결과가 일반 값이므로 이후에는 평범한 함수 호출이나 컬렉션 연산을 사용할 수 있습니다.

컬렉션을 순회하면서 오류를 전파하려면 `either { values.map { operation(it).bind() } }`로 표현합니다. 독립적인 검증에서 발생한 오류를 모두 모아야 한다면 일반 `map` 대신 `mapOrAccumulate`를 사용합니다.

- 부수효과는 별도 `IO` 래퍼 대신 `suspend` 함수와 코루틴으로 다룸. `suspend` 자체가 순수성을 보장하는 것은 아님.
- `map`, `fold`, `contramap` 등의 이름은 기존 함수형 생태계와 연결되지만, Kotlin에 고차 종류 타입이나 범용 타입 클래스가 추가되는 것은 아님.
- 원문에 남아 있는 `Semigroup`, `Monoid`의 폐기 예정 설명은 1.x 시기의 내용임. 2.x 도입 시 결합 함수와 초기값을 받는 API로 이해해야 함.

## 주요 연산 빠른 참조

> 원문: [Summary](https://arrow-kt.io/learn/summary/)

- 오류가 가능한 계산 생성: `either`, `result`, `nullable`, `option`.
- 실패 표현: `raise`, `ensure`, `ensureNotNull`.
- 기존 결과 연결: 래퍼 값의 `bind`, 같은 Raise 컨텍스트 함수의 직접 호출.
- 결과 해석: `fold`, `recover`; 예외 처리: Arrow의 `catch`.
- 오류 누적: `zipOrAccumulate`, `mapOrAccumulate`.
- 병렬 실행: `parZip`, `parMap`; 경쟁 실행: `raceN`.
- 반복과 재시도: `Schedule.repeat`, `Schedule.retry`.
- 획득과 해제 연결: `resourceScope`, `install`.
- Optics 조회: `get`, `getOrNull`, `getAll`, `foldMap`.
- Optics 수정과 구성: `set`, `modify`, `copy`, `reverseGet`.

## 개요

> 원문: [quickstart](https://arrow-kt.io/learn/quickstart/)

Arrow는 데이터 수정과 리소스 관리처럼 Kotlin 애플리케이션에서 반복해서 다루는 작업을 지원합니다. 코루틴과 통합되어 있으므로 기존의 Kotlin 코드 안에서 필요한 기능을 사용할 수 있습니다.

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

Arrow는 Kotlin 코루틴을 바탕으로 병렬 처리와 리소스 관리를 위한 API를 제공합니다.

### 병렬 처리

#### parZip

`parZip`은 독립적인 계산을 병렬로 실행한 뒤 결과를 결합합니다. 각 계산을 인자로 전달하고, 마지막 블록에서 결과를 받아 처리합니다. 아래에서는 사용자 이름과 아바타를 가져와 하나의 `User`로 만듭니다.

```kotlin
suspend fun getUser(id: UserId): User = parZip(
  { getUserName(id) },
  { getAvatar(id) }
) { name, avatar -> User(name, avatar) }
```

#### parMap

`parMap()`은 여러 작업을 동시에 실행하며, 동시 실행 개수를 제한할 수 있습니다.

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

반환값은 `Either<A, B>`입니다. 두 계산의 결과 타입이 같다면 하나의 타입으로 합칠 수 있습니다.

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

여러 리소스가 서로 의존한다면 획득한 리소스를 언제 해제할지도 함께 관리해야 합니다. Arrow의 Resource DSL은 이 획득과 해제를 연결하고, Kotlin 코루틴의 예외와 취소 상황에서도 정리가 이루어지도록 합니다.

#### 핵심 문제

리소스를 직접 관리할 때는 다음과 같은 제약을 고려해야 합니다:

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

리소스를 획득하고 해제하는 절차를 값으로 정의해 재사용합니다.

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
- `either`와 같은 타입화된 에러 빌더와 함께 사용할 수 있습니다
- `closeable` 함수를 통해 Java의 `AutoCloseable`을 지원합니다
- `parZip`과 같은 연산을 사용하여 병렬 획득을 허용합니다

### 트랜잭션: 소프트웨어 트랜잭션 메모리 (STM)

`arrow-fx-stm`은 `TVar` 원자 변수와 롤백 기능을 갖춘 소프트웨어 트랜잭션 메모리(STM)를 구현합니다. 공유 상태의 수정을 트랜잭션으로 묶어 데드락이나 경쟁 조건 없이 동시에 접근할 수 있도록 합니다.

#### 핵심 컴포넌트

TVar (트랜잭션 변수): "타입 A의 값을 보유하지만 동시 수정이 보호되는" 변수를 나타내는 기본 빌딩 블록입니다.

주요 프리미티브: `retry`, `orElse`, `catch`

#### 기본 연산

트랜잭션에는 수행할 연산을 기술하며, `atomically()`로 실행합니다. 읽기와 쓰기는 STM 컨텍스트 안에서 수행합니다.

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

Arrow의 복원력 섹션은 분산 환경에서의 시스템 실패를 다룹니다. 문서에 따르면 "복원력은 이러한 이벤트가 발생할 때 시스템이 조직화된 방식으로 행동하는 능력입니다."

여러 서비스에 의존하는 시스템에서는 각 서비스가 예상치 못하게 실패할 수 있습니다. Arrow는 이런 상황에 대응할 전략을 직접 구성할 수 있도록 조합 가능한 도구를 제공합니다.

### 핵심 복원력 도구

`arrow-resilience` 라이브러리는 세 가지 주요 메커니즘을 제공합니다:

#### 1. 재시도 및 반복 (Retry and Repeat)

`Schedule`로 계산의 재시도 정책을 정의하면 시스템이 일시적인 실패에서 복구하도록 할 수 있습니다.

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

버전 2.0부터는 지정한 예외 타입에 대해서만 재시도하도록 제한할 수 있습니다.

```kotlin
policy.retry<IllegalArgumentException, _> { ... }
```

#### 2. 서킷 브레이커 (Circuit Breaker)

서킷 브레이커는 서비스 상태가 나빠졌을 때 요청을 빠르게 실패시켜 다운스트림 서비스의 과부하와 연쇄 실패를 방지합니다. 재시도 메커니즘과 함께 사용하면 실패한 서비스에 요청을 계속 보내는 상황도 제어할 수 있습니다.

세 가지 상태:

Closed (초기 상태)
요청을 정상적으로 처리하면서 실패를 추적합니다. 실패가 `maxFailures` 임계값을 초과하면 Open으로 전환하고, 요청이 성공하면 카운터를 0으로 재설정합니다.

Open (빠른 실패 상태)
모든 요청에 `ExecutionRejected` 예외를 던져 빠르게 실패시킵니다. `resetTimeout`이 지나면 서비스 상태를 시험하기 위해 Half-Open으로 이동합니다.

Half-Open (테스트 상태)
테스트 요청 하나만 허용하고 나머지 요청은 즉시 실패시킵니다. 성공하면 카운터가 재설정되고 Closed로 돌아갑니다. 실패하면 `resetTimeout`에 지수 백오프가 적용되고 Open으로 돌아갑니다.

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

Arrow의 optics 라이브러리는 객체 안에 중첩된 필드에 접근하는 경로를 값으로 표현합니다. 이 경로를 조합하면 깊게 중첩된 불변 데이터도 간결하게 수정할 수 있습니다.

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

Optics로 접근 경로를 조합하면 중첩된 `copy()` 호출을 줄일 수 있습니다. 먼저 클래스에 `@optics` 애너테이션과 companion object를 추가합니다.

```kotlin
@optics data class Person(val name: String, val age: Int, val address: Address) {
  companion object
}
```

생성한 접근 경로는 `modify`에 전체 값과 변환 함수를 전달하거나, `copy` 빌더 안에서 사용할 수 있습니다. 먼저 `modify`로 국가 이름을 변환하는 예제입니다.

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

초점으로 삼는 값의 개수에 따라 다섯 가지 주요 optic 타입이 계층을 이룹니다.

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

문서에서는 다음 세 단계로 학습을 이어 가도록 권장합니다.

1. 프로젝트 설정
2. 예제 프로젝트 탐색
3. 특정 주제 영역 심층 탐구

자세한 내용은 [Arrow 공식 문서](https://arrow-kt.io/learn/)를 참조하세요.
