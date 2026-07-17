# Kotlin 코루틴 (KEEP 설계 문서) — Part 2

> 원문: [Kotlin Coroutines](https://github.com/Kotlin/KEEP/blob/master/proposals/coroutines.md)
> 저자: Andrey Breslav, Roman Elizarov | 번역일: 2026-07-10

[Part 1 — 유스 케이스, 코루틴 개요, 구현 세부 사항](022-keep-kotlin-coroutines-part1.md)에서 이어진다.

* [부록](#부록)
  * [자원 관리와 GC](#자원-관리와-gc)
  * [동시성과 스레드](#동시성과-스레드)
  * [비동기 프로그래밍 스타일](#비동기-프로그래밍-스타일)
  * [콜백 감싸기](#콜백-감싸기)
  * [퓨처 만들기](#퓨처-만들기)
  * [논블로킹 sleep](#논블로킹-sleep)
  * [협력적 단일 스레드 멀티태스킹](#협력적-단일-스레드-멀티태스킹)
  * [비동기 시퀀스](#비동기-시퀀스)
  * [채널](#채널)
  * [뮤텍스](#뮤텍스)
  * [실험적 코루틴에서의 마이그레이션](#실험적-코루틴에서의-마이그레이션)
  * [참고 자료](#참고-자료)
  * [피드백](#피드백)
* [리비전 이력](#리비전-이력)

## 부록

이 절은 비규범적(non-normative) 절로, 새로운 언어 구성 요소나 라이브러리 함수를 도입하지는 않는다. 대신 자원 관리, 동시성, 프로그래밍 스타일에 관한 추가 주제를 다루고, 다양한 유스 케이스의 예제를 더 제공한다.

### 자원 관리와 GC

코루틴은 힙 밖(off-heap) 저장소를 전혀 쓰지 않으며, 코루틴 안에서 실행되는 코드가 파일이나 다른 자원을 열지 않는 한 스스로 네이티브 자원을 소비하지 않는다. 코루틴 안에서 연 파일은 어떻게든 닫아야 하지만, 코루틴 자체는 닫을 필요가 없다. 코루틴이 일시 중단되면 그 전체 상태는 컨티뉴에이션 참조를 통해 접근할 수 있다. 일시 중단된 코루틴의 컨티뉴에이션 참조를 잃어버리면 결국 가비지 컬렉터가 수거한다.

닫을 수 있는 자원을 여는 코루틴은 특별한 주의가 필요하다. [제한된 일시 중단](022-keep-kotlin-coroutines-part1.md#제한된-일시-중단) 절의 `sequence{}` 빌더로 파일에서 줄 시퀀스를 생성하는 다음 코루틴을 보자.

```kotlin
fun sequenceOfLines(fileName: String) = sequence<String> {
    BufferedReader(FileReader(fileName)).use {
        while (true) {
            yield(it.readLine() ?: break)
        }
    }
}
```

이 함수는 `Sequence<String>`을 반환하며, 파일의 모든 줄을 자연스러운 방식으로 출력하는 데 쓸 수 있다.

```kotlin
sequenceOfLines("https://github.com/kotlin/kotlin-coroutines-examples/tree/master/examples/sequence/sequenceOfLines.kt")
    .forEach(::println)
```

> 전체 코드는 [여기](https://github.com/kotlin/kotlin-coroutines-examples/tree/master/examples/sequence/sequenceOfLines.kt)에서 볼 수 있다.

`sequenceOfLines` 함수가 반환한 시퀀스를 끝까지 순회하는 한 기대대로 동작한다. 그러나 다음처럼 파일의 첫 몇 줄만 출력하면,

```kotlin
sequenceOfLines("https://github.com/kotlin/kotlin-coroutines-examples/tree/master/examples/sequence/sequenceOfLines.kt")
        .take(3)
        .forEach(::println)
```

코루틴은 첫 세 줄을 내놓기 위해 몇 번 재개된 뒤 *버려진다*(abandoned). 코루틴 자체가 버려지는 것은 괜찮지만 열린 파일은 그렇지 않다. [`use` 함수](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.io/use.html)는 실행을 끝마치고 파일을 닫을 기회를 얻지 못한다. Java 파일에는 파일을 닫는 `finalizer`가 있으므로 파일은 GC가 수거할 때까지 열린 채로 남는다. 작은 발표용 예제나 짧게 도는 유틸리티에서는 큰 문제가 아니지만, 수 기가바이트 힙을 가진 대형 백엔드 시스템에서는 재앙이 될 수 있다. GC를 유발할 만큼 메모리가 바닥나기 전에 열린 파일 핸들이 먼저 바닥날 수 있기 때문이다.

이는 지연 줄 스트림을 만드는 Java의 [`Files.lines`](https://docs.oracle.com/javase/8/docs/api/java/nio/file/Files.html#lines-java.nio.file.Path-) 메서드와 비슷한 함정이다. 이 메서드는 닫을 수 있는 Java 스트림을 반환하지만, 대부분의 스트림 연산은 해당 `Stream.close` 메서드를 자동으로 호출하지 않으므로 스트림을 닫아야 한다는 사실을 사용자가 기억해야 한다. Kotlin에서도 닫을 수 있는 시퀀스 제너레이터를 정의할 수는 있지만, 사용 후 닫힘을 언어의 자동 메커니즘으로 보장할 수 없다는 비슷한 문제를 겪게 된다. 자동화된 자원 관리를 위한 언어 메커니즘 도입은 Kotlin 코루틴의 범위에서 명시적으로 제외돼 있다.

다만 이 문제는 보통 코루틴의 비동기 유스 케이스에는 영향을 주지 않는다. 비동기 코루틴은 결코 버려지지 않고 궁극적으로 완료까지 실행되므로, 코루틴 안의 코드가 자원을 제대로 닫는다면 자원은 결국 닫힌다.

### 동시성과 스레드

개별 코루틴은 스레드와 마찬가지로 순차적으로 실행된다. 따라서 코루틴 안에서 다음과 같은 코드는 완벽하게 안전하다.

```kotlin
launch { // 코루틴을 시작한다
    val m = mutableMapOf<String, String>()
    val v1 = someAsyncTask1() // 비동기 작업을 시작한다
    val v2 = someAsyncTask2() // 비동기 작업을 시작한다
    m["k1"] = v1.await() // await로 기다리며 맵을 수정한다
    m["k2"] = v2.await() // await로 기다리며 맵을 수정한다
}
```

특정 코루틴의 스코프 안에서는 일반적인 단일 스레드용 가변 구조를 모두 쓸 수 있다. 그러나 코루틴 *사이에서* 가변 상태를 공유하는 것은 잠재적으로 위험하다. [컨티뉴에이션 인터셉터](022-keep-kotlin-coroutines-part1.md#컨티뉴에이션-인터셉터) 절에서 본 `Swing` 인터셉터처럼 모든 코루틴을 JS 스타일로 단일 이벤트 디스패치 스레드에서 재개하는 디스패처를 설치하는 코루틴 빌더를 쓴다면, 이 이벤트 디스패치 스레드에서만 수정되는 공유 객체들은 안전하게 다룰 수 있다. 그러나 멀티스레드 환경에서 작업하거나 서로 다른 스레드에서 도는 코루틴 간에 가변 상태를 공유한다면 스레드 안전한(동시성) 자료구조를 써야 한다.

이런 의미에서 코루틴은 스레드와 비슷하되 더 가볍다. 몇 개의 스레드 위에서 수백만 개의 코루틴을 돌릴 수 있다. 실행 중인 코루틴은 항상 어떤 스레드에서 실행된다. 그러나 *일시 중단된* 코루틴은 스레드를 소비하지 않으며 어떤 식으로도 스레드에 묶이지 않는다. 이 코루틴을 재개하는 일시 중단 함수가 어느 스레드에서 `Continuation.resumeWith`를 호출하느냐로 코루틴이 재개될 스레드를 정하며, 코루틴의 인터셉터가 이 결정을 재정의해 코루틴 실행을 다른 스레드로 디스패치할 수 있다.

### 비동기 프로그래밍 스타일

비동기 프로그래밍에는 여러 스타일이 있다.

콜백은 [비동기 연산](022-keep-kotlin-coroutines-part1.md#비동기-연산) 절에서 다뤘으며, 코루틴이 대체하려고 설계된 대체로 가장 불편한 스타일이다. 어떤 콜백 스타일 API든 [콜백 감싸기](#콜백-감싸기)에서 보듯 해당하는 일시 중단 함수로 감쌀 수 있다.

되짚어 보자. 예를 들어 다음 시그니처의 가상 *블로킹* `sendEmail` 함수에서 시작한다고 하자.

```kotlin
fun sendEmail(emailArgs: EmailArgs): EmailResult
```

이 함수는 동작하는 동안 실행 스레드를 잠재적으로 오랫동안 블로킹한다.

논블로킹으로 만들려면, 예를 들어 error-first [node.js 콜백 관례](https://www.tutorialspoint.com/nodejs/nodejs_callbacks_concept.htm)를 사용해 논블로킹 버전을 다음 시그니처의 콜백 스타일로 표현할 수 있다.

```kotlin
fun sendEmail(emailArgs: EmailArgs, callback: (Throwable?, EmailResult?) -> Unit)
```

그러나 코루틴은 그 밖의 비동기 논블로킹 프로그래밍 스타일도 가능하게 한다. 그중 하나가 여러 인기 언어에 내장된 async/await 스타일이다. Kotlin에서는 [퓨처](022-keep-kotlin-coroutines-part1.md#퓨처) 유스 케이스 절에서 본 `future{}`와 `.await()` 라이브러리 함수를 도입해 이 스타일을 재현할 수 있다.

이 스타일의 특징은 함수가 콜백을 파라미터로 받는 대신 어떤 종류의 퓨처 객체를 반환한다는 관례다. 이 async 스타일에서 `sendEmail`의 시그니처는 다음과 같아진다.

```kotlin
fun sendEmailAsync(emailArgs: EmailArgs): Future<EmailResult>
```

스타일 측면에서 이런 메서드 이름에는 `Async` 접미사를 붙이는 것이 좋은 관행이다. 파라미터가 블로킹 버전과 다르지 않아서, 그 동작의 비동기적 성격을 잊는 실수를 저지르기 아주 쉽기 때문이다. `sendEmailAsync` 함수는 *동시적인* 비동기 연산을 시작하며, 잠재적으로 동시성의 모든 함정을 함께 끌고 온다. 다만 이 프로그래밍 스타일을 권장하는 언어들은 보통 필요할 때 실행을 다시 순차로 되돌리는 `await` 류의 프리미티브도 갖고 있다.

Kotlin *고유의* 프로그래밍 스타일은 일시 중단 함수에 기반한다. 이 스타일에서 `sendEmail`의 시그니처는 파라미터나 반환 타입을 훼손하지 않고 `suspend` 변경자만 더한 자연스러운 형태가 된다.

```kotlin
suspend fun sendEmail(emailArgs: EmailArgs): EmailResult
```

async 스타일과 일시 중단 스타일은 이미 본 프리미티브들로 서로 쉽게 변환할 수 있다. 예를 들어 `sendEmailAsync`는 [`future` 코루틴 빌더](#퓨처-만들기)를 써서 일시 중단 함수 `sendEmail`로 구현할 수 있다.

```kotlin
fun sendEmailAsync(emailArgs: EmailArgs): Future<EmailResult> = future {
    sendEmail(emailArgs)
}
```

반대로 일시 중단 함수 `sendEmail`은 [`.await()` 일시 중단 함수](022-keep-kotlin-coroutines-part1.md#일시-중단-함수)를 써서 `sendEmailAsync`로 구현할 수 있다.

```kotlin
suspend fun sendEmail(emailArgs: EmailArgs): EmailResult = 
    sendEmailAsync(emailArgs).await()
```

그래서 어떤 의미에서 두 스타일은 동등하며, 편의성 면에서 둘 다 콜백 스타일보다 확실히 낫다. 그러나 `sendEmailAsync`와 일시 중단 함수 `sendEmail`의 차이를 더 깊이 들여다보자.

먼저 어떻게 **조합**되는지 비교해 보자. 일시 중단 함수는 일반 함수와 똑같이 조합할 수 있다.

```kotlin
suspend fun largerBusinessProcess() {
    // 여기에 많은 코드가 있고, 그 어딘가에서
    sendEmail(emailArgs)
    // 그 뒤에 다른 일이 이어진다
}
```

같은 일을 async 스타일 함수로 조합하면 이렇게 된다.

```kotlin
fun largerBusinessProcessAsync() = future {
   // 여기에 많은 코드가 있고, 그 어딘가에서
   sendEmailAsync(emailArgs).await()
   // 그 뒤에 다른 일이 이어진다
}
```

async 스타일의 함수 조합이 더 장황하고 *오류를 유발하기 쉽다*는 점을 눈여겨보라. async 스타일 예제에서 `.await()` 호출을 빠뜨려도 코드는 여전히 컴파일되고 동작한다. 하지만 이제 이메일 발송이 더 큰 비즈니스 프로세스의 나머지와 비동기적으로, 심지어 *동시에* 수행되면서 공유 상태를 수정해 재현하기 극히 어려운 오류를 끌어들일 수 있다. 반면 일시 중단 함수는 *기본이 순차적*이다. 일시 중단 함수에서는 동시성이 필요할 때마다 `future{}` 같은 코루틴 빌더 호출로 소스 코드에 명시적으로 표현한다.

여러 라이브러리를 쓰는 큰 프로젝트에서 이 스타일들이 어떻게 **확장**되는지 비교해 보자. 일시 중단 함수는 Kotlin의 경량 언어 개념이다. 모든 일시 중단 함수는 제한 없는 어떤 Kotlin 코루틴에서도 온전히 쓸 수 있다. async 스타일 함수는 프레임워크에 종속된다. 모든 프로미스/퓨처 프레임워크는 자기 종류의 프로미스/퓨처 클래스를 반환하는 자기만의 `async` 류 함수를 정의해야 하고, 자기만의 `await` 류 함수도 정의해야 한다.

**성능**을 비교해 보자. 일시 중단 함수는 호출당 최소한의 오버헤드만 발생시킨다. [구현 세부 사항](022-keep-kotlin-coroutines-part1.md#구현-세부-사항) 절을 참고하라. async 스타일 함수는 그 모든 일시 중단 기계 장치에 더해 꽤 무거운 프로미스/퓨처 추상화를 유지해야 한다. async 스타일 함수 호출은 항상 어떤 퓨처류 객체 인스턴스를 반환해야 하며, 함수가 아주 짧고 단순하더라도 이를 최적화로 제거할 수 없다. async 스타일은 아주 잘게 쪼갠 분해에는 적합하지 않다.

JVM/JS 코드와의 **상호 운용성**을 비교해 보자. async 스타일 함수는 같은 종류의 퓨처류 추상화를 쓰는 JVM/JS 코드와 더 잘 상호 운용된다. Java나 JS에서 보면 그저 해당 퓨처류 객체를 반환하는 함수일 뿐이다. [컨티뉴에이션 전달 스타일](022-keep-kotlin-coroutines-part1.md#컨티뉴에이션-전달-스타일)을 네이티브로 지원하지 않는 언어에서 보면 일시 중단 함수는 낯설어 보인다. 그러나 위 예제에서 보듯 어떤 일시 중단 함수든 주어진 프로미스/퓨처 프레임워크에 맞는 async 스타일 함수로 아주 쉽게 변환할 수 있다. 따라서 Kotlin에서 일시 중단 함수를 한 번만 작성하고, 적절한 `future{}` 코루틴 빌더 함수를 써서 한 줄의 코드로 어떤 스타일의 프로미스/퓨처와도 상호 운용되도록 어댑팅할 수 있다.

### 콜백 감싸기

많은 비동기 API가 콜백 스타일 인터페이스를 가진다. 표준 라이브러리의 `suspendCoroutine` 일시 중단 함수([일시 중단 함수](022-keep-kotlin-coroutines-part1.md#일시-중단-함수) 절 참조)는 어떤 콜백이든 Kotlin 일시 중단 함수로 쉽게 감싸는 방법을 제공한다.

간단한 패턴이 있다. 연산 결과인 `Value`를 전달받는 콜백을 가진 `someLongComputation` 함수가 있다고 하자.

```kotlin
fun someLongComputation(params: Params, callback: (Value) -> Unit)
```

다음의 직관적인 코드로 이를 일시 중단 함수로 변환할 수 있다.

```kotlin
suspend fun someLongComputation(params: Params): Value = suspendCoroutine { cont ->
    someLongComputation(params) { cont.resume(it) }
} 
```

이제 이 연산의 반환 타입이 명시적이 됐지만, 여전히 비동기적이고 스레드를 블로킹하지 않는다.

> 참고로 [kotlinx.coroutines](https://github.com/kotlin/kotlinx.coroutines)에는 코루틴의 협력적 취소를 위한 프레임워크가 있다. `suspendCoroutine`과 비슷하지만 취소를 지원하는 `suspendCancellableCoroutine` 함수를 제공한다. 자세한 내용은 가이드의 [취소에 관한 절](http://kotlinlang.org/docs/reference/coroutines/cancellation-and-timeouts.html)을 보라.

더 복잡한 예로 [비동기 연산](022-keep-kotlin-coroutines-part1.md#비동기-연산) 유스 케이스의 `aRead()` 함수를 보자. Java NIO의 [`AsynchronousFileChannel`](https://docs.oracle.com/javase/8/docs/api/java/nio/channels/AsynchronousFileChannel.html)과 그 [`CompletionHandler`](https://docs.oracle.com/javase/8/docs/api/java/nio/channels/CompletionHandler.html) 콜백 인터페이스에 대한 일시 중단 확장 함수로 다음처럼 구현할 수 있다.

```kotlin
suspend fun AsynchronousFileChannel.aRead(buf: ByteBuffer): Int =
    suspendCoroutine { cont ->
        read(buf, 0L, Unit, object : CompletionHandler<Int, Unit> {
            override fun completed(bytesRead: Int, attachment: Unit) {
                cont.resume(bytesRead)
            }

            override fun failed(exception: Throwable, attachment: Unit) {
                cont.resumeWithException(exception)
            }
        })
    }
```

> 이 코드는 [여기](https://github.com/kotlin/kotlin-coroutines-examples/tree/master/examples/io/io.kt)에서 볼 수 있다.
> 참고: [kotlinx.coroutines](https://github.com/kotlin/kotlinx.coroutines)의 실제 구현은 오래 걸리는 IO 작업을 중단하기 위한 취소를 지원한다.

같은 타입의 콜백을 공유하는 함수가 많다면, 공통 래퍼 함수를 정의해 그것들을 모두 일시 중단 함수로 쉽게 변환할 수 있다. 예를 들어 [vert.x](http://vertx.io/)는 모든 비동기 함수가 콜백으로 `Handler<AsyncResult<T>>`를 받는다는 고유한 관례를 쓴다. 코루틴에서 임의의 vert.x 함수를 간편하게 쓰려면 다음 헬퍼 함수를 정의하면 된다.

```kotlin
inline suspend fun <T> vx(crossinline callback: (Handler<AsyncResult<T>>) -> Unit) = 
    suspendCoroutine<T> { cont ->
        callback(Handler { result: AsyncResult<T> ->
            if (result.succeeded()) {
                cont.resume(result.result())
            } else {
                cont.resumeWithException(result.cause())
            }
        })
    }
```

이 헬퍼 함수를 쓰면 임의의 비동기 vert.x 함수 `async.foo(params, handler)`를 코루틴에서 `vx { async.foo(params, it) }`로 호출할 수 있다.

### 퓨처 만들기

[퓨처](022-keep-kotlin-coroutines-part1.md#퓨처) 유스 케이스의 `future{}` 빌더는 [코루틴 빌더](022-keep-kotlin-coroutines-part1.md#코루틴-빌더) 절에서 설명한 `launch{}` 빌더와 비슷하게 어떤 퓨처나 프로미스 프리미티브에 대해서도 정의할 수 있다.

```kotlin
fun <T> future(context: CoroutineContext = CommonPool, block: suspend () -> T): CompletableFuture<T> =
        CompletableFutureCoroutine<T>(context).also { block.startCoroutine(completion = it) }
```

`launch{}`와의 첫 번째 차이는 [`CompletableFuture`](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletableFuture.html)의 구현을 반환한다는 것이고, 또 다른 차이는 기본 컨텍스트가 `CommonPool`로 정의돼 있어 기본 실행 동작이 [`CompletableFuture.supplyAsync`](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletableFuture.html#supplyAsync-java.util.function.Supplier-) 메서드와 비슷하다는 것이다(이 메서드는 기본적으로 [`ForkJoinPool.commonPool`](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ForkJoinPool.html#commonPool--)에서 코드를 실행한다). `CompletableFutureCoroutine`의 기본 구현은 단순하다.

```kotlin
class CompletableFutureCoroutine<T>(override val context: CoroutineContext) : CompletableFuture<T>(), Continuation<T> {
    override fun resumeWith(result: Result<T>) {
        result
            .onSuccess { complete(it) }
            .onFailure { completeExceptionally(it) }
    }
}
```

> 이 코드는 [여기](https://github.com/kotlin/kotlin-coroutines-examples/tree/master/examples/future/future.kt)에서 볼 수 있다.
> [kotlinx.coroutines](https://github.com/kotlin/kotlinx.coroutines)의 실제 구현은 더 발전된 형태다. 결과 퓨처의 취소를 전파해 코루틴을 취소하기 때문이다.

이 코루틴이 완료되면 퓨처의 해당 `complete` 메서드를 호출해 코루틴의 결과를 기록한다.

### 논블로킹 sleep

코루틴은 [`Thread.sleep`](https://docs.oracle.com/javase/8/docs/api/java/lang/Thread.html#sleep-long-)을 쓰면 안 된다. 스레드를 블로킹하기 때문이다. 하지만 Java의 [`ScheduledThreadPoolExecutor`](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ScheduledThreadPoolExecutor.html)를 사용하면 일시 중단하는 논블로킹 `delay` 함수를 아주 쉽게 구현할 수 있다.

```kotlin
private val executor = Executors.newSingleThreadScheduledExecutor {
    Thread(it, "scheduler").apply { isDaemon = true }
}

suspend fun delay(time: Long, unit: TimeUnit = TimeUnit.MILLISECONDS): Unit = suspendCoroutine { cont ->
    executor.schedule({ cont.resume(Unit) }, time, unit)
}
```

> 이 코드는 [여기](https://github.com/kotlin/kotlin-coroutines-examples/tree/master/examples/delay/delay.kt)에서 볼 수 있다.
> 참고: [kotlinx.coroutines](https://github.com/kotlin/kotlinx.coroutines)도 `delay` 함수를 제공한다.

이런 종류의 `delay` 함수는 자신을 사용하는 코루틴을 자신의 단일 "scheduler" 스레드에서 재개한다는 점에 유의하라. `Swing` 같은 [인터셉터](022-keep-kotlin-coroutines-part1.md#컨티뉴에이션-인터셉터)를 쓰는 코루틴은 인터셉터가 적절한 스레드로 디스패치하므로 이 스레드에 머물러 실행되지 않는다. 인터셉터가 없는 코루틴은 이 scheduler 스레드에 머물러 실행된다. 그래서 이 방법은 데모 목적으로는 편리하지만 가장 효율적이지는 않다. sleep은 해당 인터셉터에서 네이티브로 구현하는 것이 바람직하다.

`Swing` 인터셉터라면, 논블로킹 sleep의 네이티브 구현은 바로 이 목적을 위해 설계된 [Swing Timer](https://docs.oracle.com/javase/8/docs/api/javax/swing/Timer.html)를 써야 한다.

```kotlin
suspend fun Swing.delay(millis: Int): Unit = suspendCoroutine { cont ->
    Timer(millis) { cont.resume(Unit) }.apply {
        isRepeats = false
        start()
    }
}
```

> 이 코드는 [여기](https://github.com/kotlin/kotlin-coroutines-examples/tree/master/examples/context/swing-delay.kt)에서 볼 수 있다.
> 참고: [kotlinx.coroutines](https://github.com/kotlin/kotlinx.coroutines)의 `delay` 구현은 인터셉터별 sleep 기능을 인지하고 있으며 적절한 곳에서 위 방식을 자동으로 사용한다.

### 협력적 단일 스레드 멀티태스킹

협력적 단일 스레드 애플리케이션은 동시성과 공유 가변 상태를 다룰 필요가 없어서 작성하기가 매우 편하다. JS, Python을 비롯한 많은 언어에는 스레드가 없지만 협력적 멀티태스킹 프리미티브는 있다.

[코루틴 인터셉터](022-keep-kotlin-coroutines-part1.md#컨티뉴에이션-인터셉터)는 모든 코루틴을 단일 스레드에 가두는 직관적인 도구를 제공한다. [여기](https://github.com/kotlin/kotlin-coroutines-examples/tree/master/examples/context/threadContext.kt)의 예제 코드는 단일 스레드 실행 서비스를 만들고 코루틴 인터셉터 요구 사항에 맞게 어댑팅하는 `newSingleThreadContext()` 함수를 정의한다.

다음 예제에서는 이를 [퓨처 만들기](#퓨처-만들기) 절에서 정의한 `future{}` 코루틴 빌더와 함께 사용한다. 내부에 동시에 활성 상태인 비동기 작업이 두 개나 있는데도 단일 스레드에서 동작한다.

```kotlin
fun main(args: Array<String>) {
    log("Starting MyEventThread")
    val context = newSingleThreadContext("MyEventThread")
    val f = future(context) {
        log("Hello, world!")
        val f1 = future(context) {
            log("f1 is sleeping")
            delay(1000) // 1초 잔다
            log("f1 returns 1")
            1
        }
        val f2 = future(context) {
            log("f2 is sleeping")
            delay(1000) // 1초 잔다
            log("f2 returns 2")
            2
        }
        log("I'll wait for both f1 and f2. It should take just a second!")
        val sum = f1.await() + f2.await()
        log("And the sum is $sum")
    }
    f.get()
    log("Terminated")
}
```

> 완전히 동작하는 예제는 [여기](https://github.com/kotlin/kotlin-coroutines-examples/tree/master/examples/context/threadContext-example.kt)에서 볼 수 있다.
> 참고: [kotlinx.coroutines](https://github.com/kotlin/kotlinx.coroutines)에는 바로 쓸 수 있는 `newSingleThreadContext` 구현이 있다.

애플리케이션 전체가 단일 스레드 실행에 기반한다면, 단일 스레드 실행 설비에 대한 컨텍스트를 하드코딩한 자신만의 헬퍼 코루틴 빌더를 정의할 수 있다.

### 비동기 시퀀스

[제한된 일시 중단](022-keep-kotlin-coroutines-part1.md#제한된-일시-중단) 절에서 본 `sequence{}` 코루틴 빌더는 *동기* 코루틴의 예다. 소비자가 `Iterator.next()`를 호출하는 즉시 코루틴 안의 생산자 코드가 같은 스레드에서 동기적으로 실행된다. `sequence{}` 코루틴 블록은 제한돼 있어서, [콜백 감싸기](#콜백-감싸기) 절에서 본 비동기 파일 IO 같은 서드파티 일시 중단 함수로 실행을 일시 중단할 수 없다.

*비동기* 시퀀스 빌더는 실행을 임의로 일시 중단하고 재개할 수 있다. 이는 데이터가 아직 생산되지 않은 경우를 소비자가 처리할 준비가 돼 있어야 한다는 뜻이다. 이것이 일시 중단 함수의 자연스러운 유스 케이스다. 일반 [`Iterator`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-iterator/) 인터페이스와 비슷하지만 `next()`와 `hasNext()` 함수가 일시 중단 함수인 `SuspendingIterator` 인터페이스를 정의하자.

```kotlin
interface SuspendingIterator<out T> {
    suspend operator fun hasNext(): Boolean
    suspend operator fun next(): T
}
```

`SuspendingSequence`의 정의는 표준 [`Sequence`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.sequences/-sequence/index.html)와 비슷하지만 `SuspendingIterator`를 반환한다.

```kotlin
interface SuspendingSequence<out T> {
    operator fun iterator(): SuspendingIterator<T>
}
```

동기 시퀀스의 스코프와 비슷하지만 일시 중단이 제한되지 않는 스코프 인터페이스도 정의한다.

```kotlin
interface SuspendingSequenceScope<in T> {
    suspend fun yield(value: T)
}
```

빌더 함수 `suspendingSequence{}`는 동기 `sequence{}`와 비슷하다. 차이는 `SuspendingIteratorCoroutine`의 구현 세부 사항에 있고, 이 경우에는 선택적 컨텍스트를 받는 것이 의미가 있다는 점에 있다.

```kotlin
fun <T> suspendingSequence(
    context: CoroutineContext = EmptyCoroutineContext,
    block: suspend SuspendingSequenceScope<T>.() -> Unit
): SuspendingSequence<T> = object : SuspendingSequence<T> {
    override fun iterator(): SuspendingIterator<T> = suspendingIterator(context, block)
}
```

> 전체 코드는 [여기](https://github.com/kotlin/kotlin-coroutines-examples/tree/master/examples/suspendingSequence/suspendingSequence.kt)에서 볼 수 있다.
> 참고: [kotlinx.coroutines](https://github.com/kotlin/kotlinx.coroutines)에는 같은 개념을 더 유연하게 구현한 `Channel` 프리미티브와 그에 대응하는 `produce{}` 코루틴 빌더가 있다.

[협력적 단일 스레드 멀티태스킹](#협력적-단일-스레드-멀티태스킹) 절의 `newSingleThreadContext{}` 컨텍스트와 [논블로킹 sleep](#논블로킹-sleep) 절의 논블로킹 `delay` 함수를 가져오자. 이렇게 하면 1부터 10까지의 정수를 500ms 간격으로 내놓는 논블로킹 시퀀스의 구현을 작성할 수 있다.

```kotlin
val seq = suspendingSequence(context) {
    for (i in 1..10) {
        yield(i)
        delay(500L)
    }
}
```

이제 소비자 코루틴은 자기 페이스대로 이 시퀀스를 소비하면서 다른 임의의 일시 중단 함수로 일시 중단할 수도 있다. Kotlin의 [for 루프](https://kotlinlang.org/docs/reference/control-flow.html#for-loops)는 관례(convention)로 동작하므로 언어에 특별한 `await for` 루프 구문이 필요 없다는 점에 유의하라. 위에서 정의한 비동기 시퀀스를 순회하는 데 일반 `for` 루프를 쓸 수 있다. 생산자에게 값이 없을 때마다 일시 중단된다.

```kotlin
for (value in seq) { // 생산자를 기다리는 동안 일시 중단한다
    // 여기서 value로 뭔가 한다. 여기서도 일시 중단할 수 있다
}
```

> 실행 과정을 보여주는 로깅이 포함된 완성 예제는 [여기](https://github.com/kotlin/kotlin-coroutines-examples/tree/master/examples/suspendingSequence/suspendingSequence-example.kt)에서 볼 수 있다.

### 채널

Go 스타일의 타입 안전한 채널을 Kotlin에서 라이브러리로 구현할 수 있다. 일시 중단 함수 `send`를 가진 송신 채널 인터페이스를 정의하고,

```kotlin
interface SendChannel<T> {
    suspend fun send(value: T)
    fun close()
}
```

[비동기 시퀀스](#비동기-시퀀스)와 비슷한 스타일로 일시 중단 함수 `receive`와 `operator iterator`를 가진 수신 채널을 정의한다.

```kotlin
interface ReceiveChannel<T> {
    suspend fun receive(): T
    suspend operator fun iterator(): ReceiveIterator<T>
}
```

`Channel<T>` 클래스는 두 인터페이스를 모두 구현한다. `send`는 채널 버퍼가 가득 차면 일시 중단하고, `receive`는 버퍼가 비면 일시 중단한다. 이렇게 하면 Go 스타일 코드를 거의 그대로 Kotlin에 옮길 수 있다. [Go 투어의 4번째 동시성 예제](https://tour.golang.org/concurrency/4)에서 `n`개의 피보나치 수를 채널로 보내는 `fibonacci` 함수는 Kotlin에서 이렇게 된다.

```kotlin
suspend fun fibonacci(n: Int, c: SendChannel<Int>) {
    var x = 0
    var y = 1
    for (i in 0..n - 1) {
        c.send(x)
        val next = x + y
        x = y
        y = next
    }
    c.close()
}

```

임의 개수의 경량 코루틴을 고정된 수의 실제 중량 스레드에 디스패치하는 일종의 멀티스레드 풀에서 새 코루틴을 시작하는 Go 스타일 `go {...}` 블록도 정의할 수 있다. [여기](https://github.com/kotlin/kotlin-coroutines-examples/tree/master/examples/channel/go.kt)의 예제 구현은 Java의 공용 [`ForkJoinPool`](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ForkJoinPool.html) 위에 간단하게 작성돼 있다.

이 `go` 코루틴 빌더를 쓰면 해당 Go 코드의 main 함수는 다음과 같아진다. 여기서 `mainBlocking`은 `go{}`와 같은 풀을 쓰는 `runBlocking`의 축약 헬퍼 함수다.

```kotlin
fun main(args: Array<String>) = mainBlocking {
    val c = Channel<Int>(2)
    go { fibonacci(10, c) }
    for (i in c) {
        println(i)
    }
}
```

> 동작하는 코드는 [여기](https://github.com/kotlin/kotlin-coroutines-examples/tree/master/examples/channel/channel-example-4.kt)에서 확인할 수 있다.

채널의 버퍼 크기는 자유롭게 바꿔 볼 수 있다. 단순함을 위해 예제에는 버퍼 있는 채널만(최소 버퍼 크기 1로) 구현돼 있다. 버퍼 없는 채널은 앞서 다룬 [비동기 시퀀스](#비동기-시퀀스)와 개념적으로 비슷하기 때문이다.

여러 채널 중 하나에서 어떤 동작이 가능해질 때까지 일시 중단하는 Go 스타일 `select` 제어 블록은 Kotlin DSL로 구현할 수 있다. [Go 투어의 5번째 동시성 예제](https://tour.golang.org/concurrency/5)는 Kotlin에서 이렇게 된다.

```kotlin
suspend fun fibonacci(c: SendChannel<Int>, quit: ReceiveChannel<Int>) {
    var x = 0
    var y = 1
    whileSelect {
        c.onSend(x) {
            val next = x + y
            x = y
            y = next
            true // while 루프를 계속한다
        }
        quit.onReceive {
            println("quit")
            false // while 루프를 끝낸다
        }
    }
}
```

> 동작하는 코드는 [여기](https://github.com/kotlin/kotlin-coroutines-examples/tree/master/examples/channel/channel-example-5.kt)에서 확인할 수 있다.

이 예제에는 Kotlin [`when` 식](https://kotlinlang.org/docs/reference/control-flow.html#when-expression)처럼 케이스 중 하나의 결과를 반환하는 `select {...}`와, `while(select<Boolean> { ... })`을 중괄호를 덜 쓰고 표현한 편의 함수 `whileSelect { ... }` 둘 다의 구현이 들어 있다.

[Go 투어의 6번째 동시성 예제](https://tour.golang.org/concurrency/6)의 기본(default) 선택 케이스는 `select {...}` DSL에 케이스를 하나 더 추가하기만 하면 된다.

```kotlin
fun main(args: Array<String>) = mainBlocking {
    val tick = Time.tick(100)
    val boom = Time.after(500)
    whileSelect {
        tick.onReceive {
            println("tick.")
            true // 루프를 계속한다
        }
        boom.onReceive {
            println("BOOM!")
            false // 루프를 끝낸다
        }
        onDefault {
            println("    .")
            delay(50)
            true // 루프를 계속한다
        }
    }
}
```

> 동작하는 코드는 [여기](https://github.com/kotlin/kotlin-coroutines-examples/tree/master/examples/channel/channel-example-6.kt)에서 확인할 수 있다.

`Time.tick`과 `Time.after`는 논블로킹 `delay` 함수로 [여기](https://github.com/kotlin/kotlin-coroutines-examples/tree/master/examples/channel/time.kt)에 간단하게 구현돼 있다.

다른 예제들은 해당 Go 코드로의 링크가 주석으로 달린 채 [여기](https://github.com/kotlin/kotlin-coroutines-examples/tree/master/examples/channel/)에서 찾을 수 있다.

이 채널 샘플 구현은 내부 대기 목록을 관리하기 위해 단일 락 하나에 기반한다는 점에 유의하라. 덕분에 이해하고 추론하기 쉽다. 다만 이 락 아래에서 사용자 코드를 실행하는 일은 결코 없으므로 완전히 동시적이다. 이 락은 아주 많은 수의 동시 스레드에 대한 확장성만 다소 제한한다.

> [kotlinx.coroutines](https://github.com/kotlin/kotlinx.coroutines)의 실제 채널과 `select` 구현은 락프리(lock-free) disjoint-access-parallel 자료구조에 기반한다.

이 채널 구현은 코루틴 컨텍스트의 인터셉터와 독립적이다. 해당 [컨티뉴에이션 인터셉터](022-keep-kotlin-coroutines-part1.md#컨티뉴에이션-인터셉터) 절에서 보듯 이벤트 스레드 인터셉터 아래의 UI 애플리케이션에서 쓸 수도 있고, 다른 어떤 인터셉터와 함께 쓸 수도 있고, 인터셉터 없이 쓸 수도 있다(마지막 경우 실행 스레드는 코루틴에서 사용하는 다른 일시 중단 함수들의 코드만으로 결정된다). 채널 구현은 그저 스레드 안전한 논블로킹 일시 중단 함수를 제공할 뿐이다.

### 뮤텍스

확장 가능한 비동기 애플리케이션 작성은 하나의 규율이다. 코드가 결코 블로킹하지 않고, 실제로 스레드를 블로킹하는 일 없이 (일시 중단 함수를 사용해) 일시 중단하도록 지키는 것이다. [`ReentrantLock`](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/locks/ReentrantLock.html) 같은 Java 동시성 프리미티브는 스레드를 블로킹하므로 진정한 논블로킹 코드에서는 쓰면 안 된다. 공유 자원 접근을 제어하기 위해, 코루틴을 블로킹하는 대신 실행을 일시 중단하는 `Mutex` 클래스를 정의할 수 있다. 해당 클래스의 헤더는 이렇게 생겼다.

```kotlin
class Mutex {
    suspend fun lock()
    fun unlock()
}
```

> 전체 구현은 [여기](https://github.com/kotlin/kotlin-coroutines-examples/tree/master/examples/mutex/mutex.kt)에서 볼 수 있다.
> [kotlinx.coroutines](https://github.com/kotlin/kotlinx.coroutines)의 실제 구현에는 함수가 몇 개 더 있다.

이 논블로킹 뮤텍스 구현을 사용하면 [Go 투어의 9번째 동시성 예제](https://tour.golang.org/concurrency/9)를 Kotlin으로 옮길 수 있다. Go의 `defer`와 같은 역할을 하는 Kotlin의 [`try-finally`](https://kotlinlang.org/docs/reference/exceptions.html)를 사용한다.

```kotlin
class SafeCounter {
    private val v = mutableMapOf<String, Int>()
    private val mux = Mutex()

    suspend fun inc(key: String) {
        mux.lock()
        try { v[key] = v.getOrDefault(key, 0) + 1 }
        finally { mux.unlock() }
    }

    suspend fun get(key: String): Int? {
        mux.lock()
        return try { v[key] }
        finally { mux.unlock() }
    }
}
```

> 동작하는 코드는 [여기](https://github.com/kotlin/kotlin-coroutines-examples/tree/master/examples/channel/channel-example-9.kt)에서 확인할 수 있다.

### 실험적 코루틴에서의 마이그레이션

코루틴은 Kotlin 1.1-1.2에서 실험적 기능이었다. 해당 API는 `kotlin.coroutines.experimental` 패키지로 노출됐다. Kotlin 1.3부터 제공되는 안정 버전 코루틴은 `kotlin.coroutines` 패키지를 사용한다. 실험적 패키지는 여전히 표준 라이브러리에 남아 있으며, 실험적 코루틴으로 컴파일된 코드는 예전처럼 계속 동작한다.

Kotlin 1.3 컴파일러는 실험적 일시 중단 함수를 호출하고, 실험적 코루틴으로 컴파일된 라이브러리에 일시 중단 람다를 전달하는 것을 지원한다. 내부적으로는 대응하는 안정·실험적 코루틴 인터페이스 사이의 어댑터가 만들어진다.

### 참고 자료

* 더 읽을거리:
   * [코루틴 레퍼런스 가이드](http://kotlinlang.org/docs/reference/coroutines/coroutines-guide.html) **이것부터 읽어라!**
* 발표:
   * [Introduction to Coroutines](https://www.youtube.com/watch?v=_hfBv0a09Jc) (Roman Elizarov, KotlinConf 2017, [슬라이드](https://www.slideshare.net/elizarov/introduction-to-coroutines-kotlinconf-2017))
   * [Deep dive into Coroutines](https://www.youtube.com/watch?v=YrrUCSi72E8) (Roman Elizarov, KotlinConf 2017, [슬라이드](https://www.slideshare.net/elizarov/deep-dive-into-coroutines-on-jvm-kotlinconf-2017))
   * [Kotlin Coroutines in Practice](https://www.youtube.com/watch?v=a3agLJQ6vt8) (Roman Elizarov, KotlinConf 2018, [슬라이드](https://www.slideshare.net/elizarov/kotlin-coroutines-in-practice-kotlinconf-2018))
* 언어 설계 개요:
  * Part 1 (프로토타입 설계): [Coroutines in Kotlin](https://www.youtube.com/watch?v=4W3ruTWUhpw) (Andrey Breslav, JVMLS 2016)
  * Part 2 (현재 설계): [Kotlin Coroutines Reloaded](https://www.youtube.com/watch?v=3xalVUY69Ok&feature=youtu.be) (Roman Elizarov, JVMLS 2017, [슬라이드](https://www.slideshare.net/elizarov/kotlin-coroutines-reloaded))

### 피드백

피드백은 다음으로 제출해 달라.

* Kotlin 컴파일러의 코루틴 구현 문제와 기능 요청은 [Kotlin YouTrack](http://kotl.in/issue)으로.
* 지원 라이브러리의 문제는 [`kotlinx.coroutines`](https://github.com/Kotlin/kotlinx.coroutines/issues)로.

## 리비전 이력

이 절은 코루틴 설계의 리비전 간 변경 사항을 개관한다.

### 리비전 3.3의 변경 사항

* 코루틴이 더 이상 실험적 기능이 아니며 `kotlin.coroutines` 패키지로 이동했다.
* 실험적 상태에 관한 절 전체를 삭제하고 마이그레이션 절을 추가했다.
* 명명 스타일의 진화를 반영하는 비규범적·문체상의 변경 일부.
* Kotlin 1.3에 구현된 새 기능에 맞춰 명세를 갱신했다:
  * 더 많은 연산자와 다양한 종류의 함수를 지원한다.
  * 인트린식 함수 목록의 변경:
  * `suspendCoroutineOrReturn`을 삭제하고 대신 `suspendCoroutineUninterceptedOrReturn`을 제공한다.
  * `createCoroutineUnchecked`를 삭제하고 대신 `createCoroutineUnintercepted`를 제공한다.
  * `startCoroutineUninterceptedOrReturn`을 제공한다.
  * `intercepted` 확장 함수를 추가했다.
* 읽기 쉽도록, 고급 주제와 추가 예제를 다루는 비규범적 절들을 문서 끝의 부록으로 옮겼다.

### 리비전 3.2의 변경 사항

* `createCoroutineUnchecked` 인트린식의 설명을 추가했다.

### 리비전 3.1의 변경 사항

이 리비전은 Kotlin 1.1.0 릴리스에 구현됐다.

* `kotlin.coroutines` 패키지를 `kotlin.coroutines.experimental`로 교체했다.
* `SUSPENDED_MARKER`를 `COROUTINE_SUSPENDED`로 이름을 바꿨다.
* 코루틴의 실험적 상태에 관한 설명을 추가했다.

### 리비전 3의 변경 사항

이 리비전은 Kotlin 1.1-Beta에 구현됐다.

* 일시 중단 함수가 다른 일시 중단 함수를 임의의 지점에서 호출할 수 있다.
* 코루틴 디스패처를 코루틴 컨텍스트로 일반화했다:
  * `CoroutineContext` 인터페이스를 도입했다.
  * `ContinuationDispatcher` 인터페이스를 `ContinuationInterceptor`로 교체했다.
  * `createCoroutine`/`startCoroutine`의 `dispatcher` 파라미터를 삭제했다.
  * `Continuation` 인터페이스가 `val context: CoroutineContext`를 포함한다.
* `CoroutineIntrinsics` 객체를 `kotlin.coroutines.intrinsics` 패키지로 교체했다.

### 리비전 2의 변경 사항

이 리비전은 Kotlin 1.1-M04에 구현됐다.

* `coroutine` 키워드를 일시 중단 함수 타입으로 교체했다.
* 일시 중단 함수의 `Continuation`이 호출 지점과 선언 지점 모두에서 암묵적이 됐다.
* 필요할 때 일시 중단 함수에서 컨티뉴에이션을 포착할 수 있도록 `suspendContinuation`을 제공한다.
* 컨티뉴에이션 전달 스타일 변환에 일시 중단하지 않는 호출에서의 스택 증가를 막는 장치가 마련됐다.
* `createCoroutine`/`startCoroutine` 코루틴 빌더를 도입했다.
* 코루틴 컨트롤러 개념을 폐기했다:
  * 코루틴 완료 결과는 `Continuation` 인터페이스로 전달된다.
  * 코루틴 스코프는 코루틴 `receiver`를 통해 선택적으로 사용할 수 있다.
  * 일시 중단 함수를 리시버 없이 최상위에 정의할 수 있다.
* `CoroutineIntrinsics` 객체에 안전성보다 성능이 중요한 경우를 위한 저수준 프리미티브가 들어 있다.
