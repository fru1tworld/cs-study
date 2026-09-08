# 05. 리소스 관리와 SuspendApp

- 확인일: 2026-09-08.
- 코드: 개념 설명용 발췌 예제 포함. 생략한 타입, import, 프로젝트 설정이 있는 예제는 독립 실행 프로그램이 아님.
- 원본 문서와 코드의 라이선스: [Apache-2.0](LICENSE.arrow-website).

[전체 목차](01_introduction_and_setup.md)


## 수록한 공식 문서

- [coroutines/resource-safety](https://arrow-kt.io/learn/coroutines/resource-safety/)
- [coroutines/suspendapp](https://arrow-kt.io/learn/coroutines/suspendapp/)
- [coroutines/suspendapp/ktor](https://arrow-kt.io/learn/coroutines/suspendapp/ktor/)
- [coroutines/suspendapp/kafka](https://arrow-kt.io/learn/coroutines/suspendapp/kafka/)

## 리소스 안전성 (Resource Safety)

> 원문: [coroutines/resource-safety](https://arrow-kt.io/learn/coroutines/resource-safety/)

Arrow의 Resource 시스템은 예외나 취소 시에도 리소스의 할당과 해제를 안전하게 관리합니다. Kotlin의 코루틴 및 구조적 동시성과 통합됩니다.

### 문제 이해하기

리소스 사용 뒤에 해제 코드를 순서대로 적는 것만으로는 예외에 대응하기 어렵습니다. 다음 예시에서 `processData()`가 예외를 던지면 뒤의 `close()`와 `shutdown()`에 도달하지 못해 리소스가 남습니다:

```kotlin
class UserProcessor {
    fun start(): Unit = println("Creating UserProcessor")
    fun shutdown(): Unit = println("Shutting down UserProcessor")
}

class DataSource {
    fun connect(): Unit = println("Connecting dataSource")
    fun close(): Unit = println("Closing dataSource")
}

class Service(val db: DataSource, val userProcessor: UserProcessor) {
    suspend fun processData(): List<String> = TODO()
}

suspend fun example() {
    val userProcessor = UserProcessor().also { it.start() }
    val dataSource = DataSource().also { it.connect() }
    val service = Service(dataSource, userProcessor)
    service.processData() // 여기서 예외 발생 = 리소스 누수
    dataSource.close()
    userProcessor.shutdown()
}
```

`use` 블록이 도움이 되지만 제한사항이 있습니다:

1. 해당 타입에 Closeable 또는 AutoCloseable 인터페이스 구현이 필요
2. 외부 타입은 래핑이 필요
3. 깊은 중첩으로 조합성이 감소
4. 메서드 이름이 `close`로 강제됨
5. 일반 close() 메서드 자체에는 suspend 함수를 선언할 수 없음
6. 종료 신호에 대한 가시성 없음

### 해결책: 3단계 리소스 모델

리소스는 세 단계를 따릅니다:

1. 획득 (Acquiring) - 리소스 획득
2. 사용 (Using) - 리소스 사용
3. 해제 (Releasing) - 리소스 해제

Arrow는 리소스 획득과 해제를 묶어 예외나 취소가 발생해도 리소스를 정리하도록 보장합니다.

### 두 가지 구현 접근법

#### 1. ResourceScope DSL

`install` 함수는 획득과 해제를 모두 처리합니다. "실행이 어떻게 종료되었는지에 따라 다른 작업을 수행할 수 있는 유연성을 제공합니다: 성공적 완료, 예외, 또는 취소."

```kotlin
suspend fun ResourceScope.userProcessor(): UserProcessor =
    install({ UserProcessor().also { it.start() } }) { p, _ ->
        p.shutdown()
    }

suspend fun ResourceScope.dataSource(): DataSource =
    install({ DataSource().also { it.connect() } }) { ds, _ ->
        ds.close()
    }

suspend fun example(): Unit = resourceScope {
    val service = parZip(
        { userProcessor() },
        { dataSource() }
    ) { userProcessor, ds -> Service(ds, userProcessor) }
    val data = service.processData()
    println(data)
}
```

핵심 참고: "Install은 부분 획득을 방지하기 위해 acquire와 release를 NonCancellable로 호출합니다."

#### 2. Resource 타입 별칭

같은 리소스 구성을 재사용하고 싶다면 획득과 해제 방법을 `Resource<T>` 값으로 정의할 수 있습니다. 이렇게 정의한 값은 `resourceScope` 안에서 `.bind()`를 호출해 사용합니다:

```kotlin
val userProcessor: Resource<UserProcessor> = resource({
    UserProcessor().also { it.start() }
}) { p, _ -> p.shutdown() }

val dataSource: Resource<DataSource> = resource({
    DataSource().also { it.connect() }
}) { ds, exitCase ->
    println("Releasing $ds with exit: $exitCase")
    withContext(Dispatchers.IO) { ds.close() }
}

val service: Resource<Service> = resource {
    Service(dataSource.bind(), userProcessor.bind())
}

suspend fun example(): Unit = resourceScope {
    val data = service.bind().processData()
    println(data)
}
```

기술적으로, "Resource는 ResourceScope를 사용하는 매개변수 없는 함수에 대한 타입 별칭에 불과합니다."

```kotlin
typealias Resource<A> = suspend ResourceScope.() -> A
```

### Java 통합

Arrow는 JVM에서 `AutoCloseable` 리소스를 관리하는 `closeable()` 함수를 제공합니다.

```kotlin
suspend fun example(): Unit = resourceScope {
    val connection = closeable { getConnection() }
    // connection 사용
}
```

### 타입화된 에러 통합

`either`와 `resourceScope`를 함께 쓸 때는 어느 스코프가 바깥에 있는지에 따라 리소스에 전달되는 종료 상태가 달라집니다:

패턴 1 (either가 외부, resourceScope가 내부):
```kotlin
suspend fun pattern1(): Either<String, Unit> = either {
    resourceScope {
        val resource = install({ acquireResource() }) { r, _ -> r.release() }
        // bind 실패 시 리소스는 Cancelled 상태로 해제됨
    }
}
```

패턴 2 (resourceScope가 외부, either가 내부):
```kotlin
suspend fun pattern2(): Unit = resourceScope {
    val result = either {
        // 에러 발생 시 리소스는 정상 완료 상태로 해제됨
    }
}
```

두 패턴 모두 적절한 정리를 보장하지만, 종료 상태 처리 방식에 따라 동작이 다릅니다.

### ExitCase 매개변수

파이널라이저는 실행이 성공했는지, 에러가 발생했는지, 또는 취소되었는지를 보여주는 종료 정보를 받습니다:

```kotlin
resource({
    acquireResource()
}) { resource, exitCase ->
    when (exitCase) {
        is ExitCase.Completed -> println("정상 완료")
        is ExitCase.Cancelled -> println("취소됨: ${exitCase.exception}")
        is ExitCase.Failure -> println("실패: ${exitCase.failure}")
    }
    resource.release()
}
```

---

## 우아한 종료 (Graceful Shutdown)

> 원문: [coroutines/suspendapp](https://arrow-kt.io/learn/coroutines/suspendapp/)

"우아한 종료가 필요한 애플리케이션을 구축할 때 일반적으로 많은 플랫폼별 코드를 작성해야 합니다. 이 라이브러리는 KotlinX Coroutines와 구조적 동시성을 사용하여 Kotlin MPP를 활용함으로써 그 문제를 해결하는 것을 목표로 합니다."

SuspendApp은 플랫폼별 종료 처리를 코루틴의 취소와 연결합니다. 아래 예시에서는 종료 요청을 받으면 취소를 감지하고, 정리 작업을 마친 뒤 애플리케이션을 종료합니다.

### 간단한 예제

문서는 인터럽트 처리를 보여주는 기본 구현을 제공합니다:

```kotlin
import arrow.continuations.SuspendApp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

fun main() = SuspendApp {
    try {
        println("앱이 시작되었습니다! 종료 요청을 기다리는 중.")
        while (true) {
            delay(2_500)
            println("Ping")
        }
    } catch (e: CancellationException) {
        println("앱 정리 중... 10초가 걸립니다...")
        withContext(NonCancellable) { delay(10_000) }
        println("정리 완료. 앱 종료를 해제합니다")
    }
}
```

Ctrl+C (SIGINT)나 `kill PID` (SIGTERM)로 종료를 요청할 수 있습니다.

### Arrow의 Resource와 함께 SuspendApp 사용

앞에서 사용한 Resource를 SuspendApp에 연결하면 종료 신호를 받은 뒤 리소스 해제를 기다릴 수 있습니다. 다음 예제는 `awaitCancellation()`에서 기다리다가 취소되면 등록해 둔 해제 함수를 실행합니다:

```kotlin
fun main() = SuspendApp {
    resourceScope {
        install({ println("리소스 생성 중") }) { _, exitCase ->
            println("ExitCase: $exitCase")
            println("종료하는 데 10초가 걸립니다")
            delay(10_000)
            println("종료 완료")
        }
        println("획득한 리소스로 애플리케이션 실행 중.")
        awaitCancellation()
    }
}
```

"CoroutineScope가 취소되면, 모든 suspend 파이널라이저는 Job#join에 백프레셔를 가합니다."

Ctrl+C 후 예상 출력:
```
리소스 생성 중
획득한 리소스로 애플리케이션 실행 중.
ExitCase: Cancelled(...)
종료하는 데 10초가 걸립니다
종료 완료
```

### 지원되는 플랫폼

현재 지원되는 타겟:

- JVM
- MacOS (X64 & Arm64)
- NodeJS
- Windows (MingwX64)
- Linux

모바일과 브라우저 타겟은 지원되지 않습니다.

#### Node.js 구성

```kotlin
js(IR) {
    nodejs {
        binaries.executable()
    }
}
```

#### Native 구성

```kotlin
linuxX64 { binaries.executable() }
mingwX64 { binaries.executable() }
macosArm64 { binaries.executable() }
macosX64 { binaries.executable() }
```

## suspend 없는 리소스 관리

> 원문: [Resource safety](https://arrow-kt.io/learn/coroutines/resource-safety/)

코루틴과 취소에 맞춰 리소스를 관리할 때는 `arrow-fx-coroutines`를 사용합니다. suspend가 필요 없는 리소스의 수명은 별도 `arrow-autoclose` 모듈로 관리할 수 있습니다. 코루틴 안에서 호출하더라도 일반 `AutoCloseable.close()`가 suspend 함수가 되는 것은 아니므로, 중단 가능한 해제 로직이 필요하다면 Resource의 해제 함수를 사용합니다.

획득 함수 자체가 실패하는 경우도 구분해야 합니다. 획득 도중 이미 부수효과가 발생했다면, 아직 완전히 획득되지 않은 자원은 해당 획득 코드에서 정리해야 합니다.

## SuspendApp과 Ktor

> 원문: [SuspendApp with Ktor](https://arrow-kt.io/learn/coroutines/suspendapp/ktor/)

`suspendapp-ktor`의 `server`는 Ktor 엔진의 실행 수명을 Resource에 연결합니다. `SuspendApp` 안에서 `resourceScope`를 열어 서버를 등록한 뒤, `awaitCancellation()`으로 종료 신호를 기다리는 형태로 구성합니다.

- 종료 단계의 `preWait`는 서버 정지 전에 기다리는 시간이며, 공식 문서의 기본값은 30초임.
- `grace`는 처리 중인 요청에 주는 유예 시간, `timeout`은 강제 종료까지의 제한 시간임.
- Kubernetes의 트래픽 경로 갱신과 프로세스 종료가 즉시 동시에 완료되는 것은 아니므로 종료 예산에 각 대기 시간을 반영함.
- JVM에서 Ktor 자체 종료 훅이 먼저 서버를 멈추는 일을 피하려면 시스템 속성 `io.ktor.server.engine.ShutdownHook=false`를 설정함.
- 개발 모드에서는 `preWait`를 무시함. 다른 플랫폼에서 종료 훅을 비활성화할 수 있는지는 원문의 지원 범위 확인 필요.

```kotlin
// Netty, routing, respond 등은 Ktor 프로젝트의 import와 의존성 필요.
fun main() = SuspendApp {
    resourceScope {
        server(Netty) {
            routing {
                get("/ping") { call.respond("pong") }
            }
        }
        awaitCancellation()
    }
}
```

## SuspendApp과 Kafka

> 원문: [SuspendApp with Kafka](https://arrow-kt.io/learn/coroutines/suspendapp/kafka/)

offset을 배치로 commit하는 소비자는 종료 시점에 처리는 끝났지만 아직 commit하지 않은 레코드가 남을 수 있습니다. `acknowledge()` 호출과 브로커에 반영된 commit이 서로 다른 단계이기 때문입니다.

공식 예제는 `kotlin-kafka` 또는 `reactor-kafka`의 스트림 종료 정리와 SuspendApp을 함께 사용합니다. 레코드를 처리한 뒤 `acknowledge()`하고, 종료 신호로 스트림이 취소되면 라이브러리가 offset commit을 마칠 때까지 기다립니다. 다만 이 구성 자체가 처리 결과와 외부 시스템 갱신의 원자성이나 exactly-once를 보장하지는 않습니다.

```kotlin
// receiver는 애플리케이션의 ReceiverSettings로 생성한 KafkaReceiver임.
fun main() = SuspendApp {
    receiver.receive(topicName)
        .map { record ->
            process(record.value())
            record.offset.acknowledge()
        }
        .collect()
}
```
