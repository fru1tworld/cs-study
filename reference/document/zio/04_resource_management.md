# ZIO 리소스 관리

> 원본: https://zio.dev/reference/resource/

---

## 목차

1. [리소스 관리 개요(Resource Management Overview)](#1-리소스-관리-개요resource-management-overview)
2. [ensuring를 통한 종료 처리(Finalizing with ensuring)](#2-ensuring를-통한-종료-처리finalizing-with-ensuring)
3. [획득-해제 패턴(Acquire-Release Pattern)](#3-획득-해제-패턴acquire-release-pattern)
4. [AutoCloseable 리소스(fromAutoCloseable)](#4-autocloseable-리소스fromautocloseable)
5. [Scope: 안전하고 합성 가능한 리소스 관리(Scope)](#5-scope-안전하고-합성-가능한-리소스-관리scope)
6. [ZPool: 리소스 풀링(ZPool)](#6-zpool-리소스-풀링zpool)
7. [ZKeyedPool: 키 기반 리소스 풀링(ZKeyedPool)](#7-zkeyedpool-키-기반-리소스-풀링zkeyedpool)
8. [ScopedRef: 리소스를 위한 가변 참조(ScopedRef)](#8-scopedref-리소스를-위한-가변-참조scopedref)
9. [참고 자료](#9-참고-자료)

---

## 1. 리소스 관리 개요(Resource Management Overview)

대규모 애플리케이션에서 적절한 리소스 관리(resource management)는 매우 중요합니다. 커넥션(connection)이나 파일 디스크립터(file descriptor)를 누수(leak)시키지 않는 것이 리소스 관리의 핵심 목표입니다.

### 전통적인 try/finally 패턴의 한계

Scala의 전통적인 `try`/`finally` 구문은 리소스 정리를 보장하는 기본적인 방법입니다.

```scala
def lines(file: String): Task[Long] = ZIO.attempt {
  def countLines(br: BufferedReader): Long = br.lines().count()
  val bufferedReader = new BufferedReader(
    new InputStreamReader(new FileInputStream("file.txt")),
    2048
  )
  try countLines(bufferedReader)
  finally bufferedReader.close()
}
```

하지만 `try`/`finally`에는 다음과 같은 단점(drawbacks)이 있습니다.

- 여러 리소스를 다룰 때 합성(compose)하기 어렵습니다.
- 코드가 지저분해지고 추론(reason)하기 어려워집니다.
- 정리(cleanup) 순서를 제어할 수 없습니다.
- 동시적인(concurrent) 리소스 합성을 처리할 수 없습니다.
- 비동기(asynchronous) 워크플로를 지원하지 않습니다.
- 수작업이 필요하며 오류가 발생하기 쉽습니다(manual and error-prone).

ZIO는 이러한 문제를 해결하기 위해 여러 가지 메커니즘을 제공하며, 비동기 연산과 동시적 이펙트(effect) 전반에 걸쳐 리소스를 안전하게 관리합니다.

---

## 2. ensuring를 통한 종료 처리(Finalizing with ensuring)

`ensuring` 메서드는 `try`/`finally`와 동일한 의미(semantics)를 가집니다. 즉, 어떤 이펙트가 실행을 시작한 뒤 정상적으로든 비정상적으로든(either normally or abnormally) 종료되면 종료자(finalizer)가 반드시 실행됨을 보장합니다.

```scala
val finalizer: UIO[Unit] = ZIO.succeed(println("Finalizing!"))
val finalized: IO[String, Unit] = ZIO.fail("Failed!").ensuring(finalizer)
```

### 핵심 사항

- 종료자(finalizer)는 복구 가능한(recoverable) 방식으로 실패할 수 없으므로, 오류는 반드시 처리(handle)되어야 합니다.
- 중첩된(nested) 종료자들은 추가된 순서의 역순(reverse order)으로 순차적으로 실행됩니다.
- 안쪽 종료자(inner finalizer)의 실패가 바깥쪽 종료자(outer finalizer)의 실행을 막지 않습니다.

---

## 3. 획득-해제 패턴(Acquire-Release Pattern)

`ZIO.acquireReleaseWith`는 리소스의 생명 주기(lifecycle)를 안전하게 관리하며, 세 가지 구성 요소로 이루어집니다.

1. **획득 이펙트(Acquire effect)** - 리소스를 획득합니다(예: 파일 열기).
2. **해제 함수(Release function)** - 리소스를 해제하는 이펙트를 반환합니다(예: 파일 닫기).
3. **사용 함수(Use function)** - 리소스를 소비하는 이펙트입니다(예: 처리하기).

일반적인 구조는 다음과 같습니다.

```scala
val result: Task[Any] = ZIO.acquireReleaseWith(acquire)(release)(use)
```

### 파일 읽기 예제

```scala
def lines(file: String): Task[Long] = {
  def countLines(reader: BufferedReader): Task[Long] =
    ZIO.attempt(reader.lines().count())
  def releaseReader(reader: BufferedReader): UIO[Unit] =
    ZIO.succeed(reader.close())
  def acquireReader(file: String): Task[BufferedReader] =
    ZIO.attempt(new BufferedReader(new FileReader(file), 2048))
  ZIO.acquireReleaseWith(acquireReader(file))(releaseReader)(countLines)
}
```

데이터를 그룹화하는 예제도 같은 방식으로 작성할 수 있습니다.

```scala
val groupedFileData: IO[IOException, Unit] =
  ZIO.acquireReleaseWith(openFile("data.json"))(closeFile(_)) { file =>
    for {
      data    <- decodeData(file)
      grouped <- groupData(data)
    } yield grouped
  }
```

### 중첩 패턴을 사용한 파일 전송

두 개의 리소스를 병렬로 획득하고 해제하는 파일 전송 예제입니다.

```scala
def transfer(src: String, dst: String): ZIO[Any, Throwable, Unit] =
  ZIO.acquireReleaseWith {
    is(src).zipPar(os(dst))
  } { case (in, out) =>
    ZIO.succeed(in.close()).zipPar(ZIO.succeed(out.close()))
  } { case (in, out) =>
    copy(in, out)
  }
```

### 한계점(Limitations)

`acquireReleaseWith` 패턴은 다음과 같은 한계가 있습니다.

- 쉽게 합성(compose)되지 않습니다.
- 여러 리소스를 다룰 때 중첩 패턴(nested pattern)이 지저분해집니다.

이 한계를 해결하기 위해 ZIO는 합성 가능한 데이터 타입인 `Scope`를 제공합니다(5장 참고).

---

## 4. AutoCloseable 리소스(fromAutoCloseable)

`AutoCloseable`을 구현한 리소스의 경우, `ZIO.fromAutoCloseable`을 사용하면 Java의 try-with-resource와 동등한 편리한 처리가 가능합니다.

```scala
val bytesInFile: IO[Throwable, Int] =
  ZIO.scoped {
    for {
      stream <- ZIO.fromAutoCloseable(openFileInputStream("data.json"))
      data   <- ZIO.attemptBlockingIO(stream.readAllBytes())
    } yield data.length
  }
```

`acquireReleaseWith` 방식과 `fromAutoCloseable` 방식 모두 오류(error)나 인터럽트(interruption)가 발생하더라도 해제(release)가 반드시 실행됨을 보장합니다.

---

## 5. Scope: 안전하고 합성 가능한 리소스 관리(Scope)

`Scope` 데이터 타입은 ZIO에서 안전하고 합성 가능한 리소스 처리(safe and composable resources handling)의 기반(foundation)입니다. 스코프(scope)는 하나 이상의 리소스의 수명(lifetime)을 나타내며, 스코프가 닫힐(close) 때 해당 리소스들이 반드시 해제됨을 보장합니다.

### 5.1 Scope 인터페이스

```scala
trait Scope {
  def addFinalizerExit(finalizer: Exit[Any, Any] => UIO[Any]): UIO[Unit]
  def close(exit: => Exit[Any, Any]): UIO[Unit]
}
object Scope {
  def make: UIO[Scope] = ???
}
```

### 5.2 리소스 정의 연산자(Resource Definition Operators)

스코프 리소스를 생성하는 기본 생성자(constructor)는 `acquireRelease`입니다.

```scala
def acquireRelease[R, E, A](acquire: => ZIO[R, E, A])
  (release: A => ZIO[R, Nothing, Any]): ZIO[R with Scope, E, A]
```

`acquireRelease` 연산자는 `acquire` 워크플로를 인터럽트 불가능하게(uninterruptibly) 수행합니다. 이는 리소스 획득이 부분적으로(partial) 인터럽트되는 것을 방지합니다.

종료 결과(exit)에 따라 정리 로직을 달리하고 싶을 때는 `acquireReleaseExit` 변형을 사용합니다.

```scala
def acquireReleaseExit[R, E, A](acquire: => ZIO[R, E, A])
  (release: (A, Exit[Any, Any]) => ZIO[R, Nothing, Any]): ZIO[R with Scope, E, A]
```

해제(release)가 리소스에 의존하지 않는 경우를 위한 인터럽트 가능한(interruptible) 변형도 존재합니다.

### 5.3 스코프 닫기와 사용(ZIO.scoped)

```scala
object ZIO {
  def scoped[R, E, A](zio: ZIO[Scope with R, E, A]): ZIO[R, E, A] = ???
}
```

`ZIO.scoped`는 환경(environment)에서 `Scope`를 제거하여, 이 워크플로가 더 이상 스코프를 필요로 하는 리소스를 사용하지 않음을 나타냅니다. 즉, 스코프가 이 지점에서 닫히고 모든 종료자가 실행됩니다.

### 5.4 Scope를 사용한 파일 전송 리팩터링

3장의 중첩 패턴을 `Scope`로 리팩터링하면, 여러 리소스를 평평하게(flat) 합성할 수 있습니다.

```scala
def transfer(from: String, to: String): ZIO[Any, Throwable, Unit] = {
  val resource = for {
    from <- ZIO.acquireRelease(is(from))(close)
    to   <- ZIO.acquireRelease(os(to))(close)
  } yield (from, to)
  ZIO.scoped {
    resource.flatMap { case (in, out) =>
      copy(in, out)
    }
  }
}
```

기본 사용 형태는 다음과 같습니다.

```scala
val scoped = ZIO.acquireRelease(acquire)(release)
```

### 5.5 종료자 순서(Finalizer Ordering)

기본적으로, `Scope`가 닫힐 때 그 스코프에 추가된 모든 종료자(finalizer)는 추가된 순서의 역순(reverse of the order)으로 닫힙니다. 이는 리소스 의존적인 해제(resource-dependent release)가 실패하는 것을 방지합니다. 예를 들어 리소스 A를 먼저 획득하고 그에 의존하는 리소스 B를 나중에 획득했다면, 해제 시에는 B를 먼저 닫고 A를 나중에 닫게 됩니다.

### 5.6 고급 연산(Advanced Operations)

**스코프 확장(Extending scopes):** `Scope#extend` 연산자는 `Scope`를 필요로 하는 `ZIO` 이펙트를 받아 `Scope`를 제공하되, 이후에 그것을 닫지 않습니다(without closing it afterwards). 이를 통해 스코프의 수명을 더 바깥쪽으로 연장할 수 있습니다.

**병렬 종료(Parallel finalization):** 종료자들을 병렬로 실행하려면 `ZIO.parallelFinalizers`를 사용합니다.

```scala
def zipScoped[R <: Scope, E, A, B](
  left: ZIO[R, E, A],
  right: ZIO[R, E, B]): ZIO[R, E, (A, B)] =
  ZIO.parallelFinalizers(left.zipPar(right))
```

**서비스 접근(Service access):** `ZIO.service[Scope]`를 호출하면 `Scope` 서비스를 직접 얻을 수 있습니다.

### 5.7 데이터 타입 변환(Data Type Conversion)

스코프 리소스(scoped resource)는 `scoped` 연산자를 통해 다른 ZIO 구성체로 변환될 수 있습니다. `ZStream`, `ZLayer` 등의 타입은 `scoped` 연산자를 제공하며, 이를 통해 수명 관리(lifetime governance)의 책임을 해당 구성체로 옮길 수 있습니다.

---

## 6. ZPool: 리소스 풀링(ZPool)

`ZPool[E, A]`는 재사용 가능한 리소스 풀을 관리합니다. `E`는 발생 가능한 오류 타입을, `A`는 항목의 타입을 나타냅니다. ZPool은 비동기(asynchronous)·동시성(concurrent) 환경을 지원하는 풀 구현체입니다.

### 6.1 동기(Motivation)

데이터베이스, 커넥션, 스레드(thread)와 같이 비용이 큰 리소스를 반복적으로 획득하면 성능 오버헤드(performance overhead)가 발생합니다. ZPool은 다음과 같은 방식으로 이 문제를 해결합니다.

- 리소스 풀을 미리 초기화(pre-initialize)하여 "빠르고 예측 가능한(fast and predictable)" 접근을 제공합니다.
- 클라이언트가 새 리소스를 생성하는 대신 기존 리소스에서 획득(acquire)하도록 합니다.
- 해제 메커니즘(release mechanism)을 통해 리소스 재활용(recycling)을 가능하게 합니다.

### 6.2 ZPool 인터페이스

```scala
trait ZPool[+Error, Item] {
  def get: ZIO[Scope, Error, Item]
  def invalidate(item: Item): UIO[Unit]
}
```

- **`get`**: 스코프 이펙트(scoped effect) 내에서 풀로부터 항목을 가져옵니다. 반환 타입이 `ZIO[Scope, Error, Item]`이므로, 사용이 끝나면 스코프가 닫힐 때 해당 항목이 풀로 반환됩니다.
- **`invalidate`**: 결함이 있는(faulty) 리소스를 표시하여 추후 재할당(reallocation)되도록 합니다.

### 6.3 풀 생성(Pool Construction)

#### 고정 크기 풀(Fixed-Size Pools)

`make` 생성자는 즉시 할당(eagerly-allocated)되는 풀을 생성합니다.

```scala
def make[E, A](get: ZIO[Scope, E, A], size: Int):
  ZIO[Scope, Nothing, ZPool[E, A]]
```

모든 항목(entry)이 리소스를 즉시 미리 할당(pre-allocate)합니다. 반환 타입 `ZIO[Scope, ...]`은 풀의 생명 주기 정리(lifecycle cleanup)를 자동으로 관리합니다.

정리(cleanup)가 필요 없는 리소스의 경우 `ZPool.fromIterable`을 사용하는 것이 적합합니다.

#### 동적 크기 풀(Dynamic Sizing)

두 번째 변형은 축출(eviction)을 동반한 탄력적 확장(elastic scaling)을 지원합니다.

```scala
def make[E, A](get: ZIO[Scope, E, A],
  range: Range, timeToLive: Duration):
  ZIO[Scope, Nothing, ZPool[E, A]]
```

이 풀은 최소(minimum) 개수의 항목을 미리 할당한 상태를 유지하면서, 필요에 따라(on-demand) 최대(maximum) 용량까지 확장할 수 있습니다. "축출 정책(eviction policy)"은 `timeToLive` 기간을 초과하여 사용되지 않은 항목을 제거하여, 풀을 다시 최소 크기로 축소(contract)합니다.

### 6.4 리소스 획득 동작(Resource Acquisition Behavior)

`pool.get`을 호출할 때의 동작은 다음과 같습니다.

- 미리 할당된 리소스가 있으면 즉시(immediately) 반환됩니다.
- 수요(demand)가 용량을 초과하면 필요에 따라(on-demand) 할당이 트리거됩니다.
- 요청이 최대 크기를 초과하면 가용성(availability)을 기다리며 블록(block)됩니다.
- 획득 실패(failed acquisition)는 해당 오류를 그대로 전파(propagate)합니다.

### 6.5 코드 예제

#### 기본 풀 생성

```scala
import zio._
object PoolExample extends ZIOAppDefault {
  def resource: ZIO[Scope, Nothing, UUID] = ZIO.acquireRelease(
    ZIO.random.flatMap(_.nextUUID).flatMap(uuid =>
      ZIO.debug(s"Acquiring the resource: $uuid!").as(uuid))
  )(uuid => ZIO.debug(s"Releasing the resource $uuid!"))

  def run =
    for {
      pool <- ZPool.make(resource, 3)
      item <- pool.get
      _ <- ZIO.debug(s"Item: $item")
    } yield ()
}
```

#### 데이터베이스 커넥션을 사용한 동적 풀

```scala
ZIO.scoped {
  ZPool.make(acquireDbConnection, 10 to 20, 60.seconds).flatMap { pool =>
    ZIO.scoped {
      pool.get.flatMap { conn => useConnection(conn) }
    }
  }
}
```

#### 재시도 패턴(Retry pattern)

획득에 실패할 수 있는 리소스는 `.eventually` 연산을 통해 성공할 때까지 재시도할 수 있습니다.

```scala
ZPool.make(acquireDbConnection, 10).flatMap { pool =>
  pool.get.flatMap(conn => useConnection(conn)).eventually
}
```

### 6.6 리소스 무효화(Resource Invalidation)

리소스가 결함이 생기거나(faulty) 연결이 끊어지면(disconnected), `invalidate`를 호출하여 풀의 오염(pool contamination)을 방지합니다. 풀은 즉시 교체(immediate replacement)하는 대신 지연 재할당(lazy reallocation)을 수행하여 시스템 효율성을 유지합니다.

---

## 7. ZKeyedPool: 키 기반 리소스 풀링(ZKeyedPool)

`ZKeyedPool[+Err, -Key, Item]`은 `Key` 타입의 키와 연관된 `Item` 타입 항목들의 풀입니다. 예를 들어 호스트별, 또는 샤드(shard)별로 별도의 커넥션 풀을 유지할 때 유용합니다.

### 7.1 주요 연산

인터페이스는 두 가지 핵심 메서드를 제공합니다.

1. **`get(key: Key)`**: 주어진 키와 연관된 항목을 스코프 이펙트(scoped effect) 내에서 풀로부터 가져옵니다.
2. **`invalidate(item: Item)`**: 항목을 재할당(reallocation) 대상으로 표시합니다.

### 7.2 생성 메서드(Creation Methods)

#### 고정 크기 풀(Fixed-Size Pools)

**균일한 풀 크기(Uniform pool sizes):** 모든 키에 대해 동일한 풀 크기를 사용하려면 다음을 사용합니다.

```scala
ZKeyedPool.make(get: Key => ZIO[Env, Err, Item], size: => Int)
```

예제에서는 각 키("foo"와 "bar")마다 3개의 리소스를 획득하는 모습을 보여 줍니다.

**가변 풀 크기(Variable pool sizes):** 키마다 다른 용량을 지정하려면 다음을 사용합니다.

```scala
ZKeyedPool.make(get: Key => ZIO[Env, Err, Item], size: Key => Int)
```

문서에서는 "foo"로 시작하는 키는 풀 크기 2, "bar"는 크기 3, 그 외에는 크기 1로 설정하는 예를 보여 줍니다.

#### 동적 크기 풀(Dynamic-Size Pools)

최소/최대 용량과 time-to-live 파라미터를 갖는 풀입니다.

```scala
make(get, range: Key => Range, timeToLive: Duration)
make(get, range: Key => Range, timeToLive: Key => Duration)
```

### 7.3 동작 특성

ZIO 공식 문서에는 리소스 획득/해제 주기(acquisition/release cycle)를 보여 주는 상세한 추적 로그(trace log)가 포함되어 있습니다. 이를 통해 풀이 키별로 별도의 컬렉션을 유지하고, 스코프 컨텍스트 안에서 생명 주기 연산을 올바르게 관리함을 확인할 수 있습니다.

---

## 8. ScopedRef: 리소스를 위한 가변 참조(ScopedRef)

`ScopedRef`는 스코프 리소스(scoped resource)를 관리하기 위해 특별히 설계된 `Ref`의 리소스 버전입니다. 가변 참조(mutable reference) 기능을 제공하면서 리소스 생명 주기를 스코프와 함께 관리합니다.

### 8.1 핵심 연산(Core Operations)

API는 두 가지 기본 연산을 포함합니다.

1. **Get**: `ScopedRef#get`은 스코프 참조(scoped ref)의 현재 값을 반환합니다.
2. **Set**: `ScopedRef#set`은 새 리소스를 획득하여 스코프 참조의 새 값을 생성함으로써 값을 설정합니다. 새 값을 설정하면 이전 리소스(old resource)가 자동으로 해제(release)됩니다.

### 8.2 생성 메서드(Construction Methods)

두 가지 생성자를 사용할 수 있습니다.

```scala
object ScopedRef {
  def make[A](a: => A): ZIO[Scope, Nothing, ScopedRef[A]] = ???
  def fromAcquire[R, E, A](acquire: ZIO[R, E, A]): ZIO[R with Scope, E, ScopedRef[A]] = ???
}
```

- **`make`**: 일반 값(ordinary value)으로부터 스코프 참조를 생성합니다. 리소스 획득이 필요 없는 상수 값(constant value)에 유용합니다.
- **`fromAcquire`**: 리소스 관리를 통해 값을 생성하는 이펙트로부터 스코프 참조를 생성합니다.

### 8.3 주의 사항

`ScopedRef`는 리소스를 다루는 타입이므로 수명(lifetime)이 스코프에 의해 관리됩니다. 더 이상 필요하지 않을 때는 `ZIO.scoped` 조합자(combinator)로 해제할 수 있습니다.

### 8.4 인터럽트 동작(Interruption Behavior)

`set` 연산은 인터럽트 불가능한 블록(uninterruptible block) 안에서 실행됩니다. 이는 `forkScoped`를 사용하여 `ScopedRef`로 파이버(Fiber)를 관리하려는 경우, 해당 파이버를 명시적으로 인터럽트 가능하게(interruptible) 만들어야 함을 의미합니다.

---

## 9. 참고 자료

- [Handling Resources (Overview)](https://zio.dev/overview/handling-resources)
- [Resource Management (Reference Index)](https://zio.dev/reference/resource/)
- [Scope](https://zio.dev/reference/resource/scope)
- [ZPool](https://zio.dev/reference/resource/zpool)
- [ZKeyedPool](https://zio.dev/reference/resource/zkeyedpool)
- [ScopedRef](https://zio.dev/reference/resource/scopedref)
