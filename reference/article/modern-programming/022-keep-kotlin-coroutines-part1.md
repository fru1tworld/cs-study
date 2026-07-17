# Kotlin 코루틴 (KEEP 설계 문서)

> 원문: [Kotlin Coroutines](https://github.com/Kotlin/KEEP/blob/master/proposals/coroutines.md)
> 저자: Andrey Breslav, Roman Elizarov | 번역일: 2026-07-10

* **유형**: 설계 제안(Design proposal)
* **저자**: Andrey Breslav, Roman Elizarov
* **기여자**: Vladimir Reshetnikov, Stanislav Erokhin, Ilya Ryzhenkov, Denis Zharkov
* **상태**: Kotlin 1.3부터 안정화(리비전 3.3), Kotlin 1.1-1.2에서는 실험적 기능

## 전체 목차

**Part 1 (이 문서)**

* [개요](#개요)
* [유스 케이스](#유스-케이스)
  * [비동기 연산](#비동기-연산)
  * [퓨처](#퓨처)
  * [제너레이터](#제너레이터)
  * [비동기 UI](#비동기-ui)
  * [그 밖의 유스 케이스](#그-밖의-유스-케이스)
* [코루틴 개요](#코루틴-개요)
  * [용어](#용어)
  * [Continuation 인터페이스](#continuation-인터페이스)
  * [일시 중단 함수](#일시-중단-함수)
  * [코루틴 빌더](#코루틴-빌더)
  * [코루틴 컨텍스트](#코루틴-컨텍스트)
  * [컨티뉴에이션 인터셉터](#컨티뉴에이션-인터셉터)
  * [제한된 일시 중단](#제한된-일시-중단)
* [구현 세부 사항](#구현-세부-사항)
  * [컨티뉴에이션 전달 스타일](#컨티뉴에이션-전달-스타일)
  * [상태 머신](#상태-머신)
  * [일시 중단 함수 컴파일](#일시-중단-함수-컴파일)
  * [코루틴 인트린식](#코루틴-인트린식)

**[Part 2](022-keep-kotlin-coroutines-part2.md)**

* 부록: 자원 관리와 GC, 동시성과 스레드, 비동기 프로그래밍 스타일, 콜백 감싸기, 퓨처 만들기, 논블로킹 sleep, 협력적 단일 스레드 멀티태스킹, 비동기 시퀀스, 채널, 뮤텍스, 실험적 코루틴에서의 마이그레이션, 참고 자료, 피드백
* 리비전 이력

## 개요

이 문서는 Kotlin의 코루틴을 설명한다. 이 개념은 다음의 이름으로도 알려져 있거나 다음을 부분적으로 포괄한다.

- 제너레이터/yield
- async/await
- 조합 가능한(composable)/구획된(delimited) 컨티뉴에이션

목표:

- Futures 같은 특정 리치 라이브러리 구현에 의존하지 않는다.
- "async/await" 유스 케이스와 "제너레이터 블록"을 동등하게 포괄한다.
- Kotlin 코루틴을 다양한 기존 비동기 API(Java NIO, 여러 Futures 구현 등)의 래퍼로 활용할 수 있게 한다.

## 유스 케이스

코루틴은 *일시 중단 가능한 연산*(suspendable computation)의 인스턴스, 즉 특정 지점에서 실행을 멈췄다가 나중에(어쩌면 다른 스레드에서) 다시 이어갈 수 있는 연산이라고 생각할 수 있다. 코루틴들이 서로를 호출하고 데이터를 주고받으면 협력적 멀티태스킹의 기반이 된다.

### 비동기 연산

코루틴의 첫 번째 동기 부여 유스 케이스는 비동기 연산이다(C# 등 여러 언어에서는 async/await로 처리한다). 먼저 이런 연산을 콜백으로 어떻게 처리하는지 보자. 비동기 I/O를 예로 든다(아래 API는 단순화한 것이다).

```kotlin
// `buf`로 비동기 읽기를 수행하고, 완료되면 람다를 실행한다
inChannel.read(buf) {
    // 이 람다는 읽기가 완료되면 실행된다
    bytesRead ->
    ...
    ...
    process(buf, bytesRead)
    
    // `buf`에서 비동기 쓰기를 수행하고, 완료되면 람다를 실행한다
    outChannel.write(buf) {
        // 이 람다는 쓰기가 완료되면 실행된다
        ...
        ...
        outFile.close()          
    }
}
```

여기서 콜백 안에 또 콜백이 들어 있다는 점에 주목하자. 콜백 덕분에 보일러플레이트가 많이 줄기는 하지만(예를 들어 `buf` 파라미터를 콜백에 명시적으로 넘길 필요 없이 클로저의 일부로 볼 수 있다), 들여쓰기 수준이 매번 깊어지고, 중첩이 1단계를 넘어가면 어떤 문제가 생길지 쉽게 예상할 수 있다(JavaScript에서 사람들이 이 문제로 얼마나 고통받는지는 "callback hell"을 검색해 보라).

같은 연산을 코루틴으로는 직관적으로 표현할 수 있다(I/O API를 코루틴 요구 사항에 맞게 어댑팅해 주는 라이브러리가 있다는 전제하에).

```kotlin
launch {
    // 비동기로 읽는 동안 일시 중단한다
    val bytesRead = inChannel.aRead(buf) 
    // 읽기가 완료돼야 이 줄에 도달한다
    ...
    ...
    process(buf, bytesRead)
    // 비동기로 쓰는 동안 일시 중단한다
    outChannel.aWrite(buf)
    // 쓰기가 완료돼야 이 줄에 도달한다
    ...
    ...
    outFile.close()
}
```

여기서 `aRead()`와 `aWrite()`는 특별한 *일시 중단 함수*(suspending function)다. 실행을 *일시 중단*했다가(실행 중이던 스레드를 블로킹한다는 뜻이 아니다) 호출이 완료되면 *재개*할 수 있다. `aRead()` 뒤에 오는 모든 코드가 람다로 감싸여 `aRead()`에 콜백으로 전달됐고 `aWrite()`에도 같은 일이 일어났다고 상상하며 눈을 가늘게 뜨고 보면, 이 코드가 위 코드와 동일하되 더 읽기 좋을 뿐이라는 사실을 알 수 있다.

코루틴을 매우 범용적인 방식으로 지원하는 것이 우리의 명시적 목표다. 그래서 이 예제에서 `launch{}`, `.aRead()`, `.aWrite()`는 코루틴 작업에 맞춰진 **라이브러리 함수**일 뿐이다. `launch`는 *코루틴 빌더*로, 코루틴을 만들고 시작한다. `aRead`/`aWrite`는 *컨티뉴에이션*(continuation)을 암묵적으로 전달받는 특별한 *일시 중단 함수*다(컨티뉴에이션은 그저 범용 콜백이다).

> `launch{}`의 예제 코드는 [코루틴 빌더](#코루틴-빌더) 절에, `.aRead()`의 예제 코드는 [콜백 감싸기](022-keep-kotlin-coroutines-part2.md#콜백-감싸기) 절에 있다.

콜백을 명시적으로 전달하는 방식에서는 루프 한가운데 비동기 호출을 두기가 까다롭지만, 코루틴에서는 지극히 평범한 일이다.

```kotlin
launch {
    while (true) {
        // 비동기로 읽는 동안 일시 중단한다
        val bytesRead = inFile.aRead(buf)
        // 읽기가 끝나면 계속한다
        if (bytesRead == -1) break
        ...
        process(buf, bytesRead)
        // 비동기로 쓰는 동안 일시 중단한다
        outFile.aWrite(buf) 
        // 쓰기가 끝나면 계속한다
        ...
    }
}
```

예외 처리 역시 코루틴에서 좀 더 편하다는 점도 짐작할 수 있다.

### 퓨처

비동기 연산을 표현하는 또 다른 스타일이 있다. 퓨처(future, 프로미스나 디퍼드라고도 부른다)를 이용하는 방식이다. 이미지에 오버레이를 적용하는 가상의 API를 예로 들어 보자.

```kotlin
val future = runAfterBoth(
    loadImageAsync("...original..."), // Future를 생성한다
    loadImageAsync("...overlay...")   // Future를 생성한다
) {
    original, overlay ->
    ...
    applyOverlay(original, overlay)
}
```

코루틴을 쓰면 이렇게 다시 쓸 수 있다.

```kotlin
val future = future {
    val original = loadImageAsync("...original...") // Future를 생성한다
    val overlay = loadImageAsync("...overlay...")   // Future를 생성한다
    ...
    // 이미지 로딩을 기다리는 동안 일시 중단하고,
    // 둘 다 로드되면 `applyOverlay(...)`를 실행한다
    applyOverlay(original.await(), overlay.await())
}
```

> `future{}`의 예제 코드는 [퓨처 만들기](022-keep-kotlin-coroutines-part2.md#퓨처-만들기) 절에, `.await()`의 예제 코드는 [일시 중단 함수](#일시-중단-함수) 절에 있다.

역시 들여쓰기가 줄고 조합 로직이 더 자연스러워지며(여기서는 보여주지 않지만 예외 처리도 그렇다), 퓨처를 지원하기 위한 특수 키워드(C#, JS 등 다른 언어의 `async`와 `await` 같은)가 필요 없다. `future{}`와 `.await()`는 그저 라이브러리의 함수일 뿐이다.

### 제너레이터

코루틴의 또 다른 전형적 유스 케이스는 지연 계산 시퀀스다(C#, Python 등 많은 언어에서 `yield`로 처리한다). 이런 시퀀스는 겉보기에 순차적인 코드로 생성하지만, 런타임에는 요청된 원소만 계산한다.

```kotlin
// 추론되는 타입은 Sequence<Int>
val fibonacci = sequence {
    yield(1) // 첫 번째 피보나치 수
    var cur = 1
    var next = 1
    while (true) {
        yield(next) // 다음 피보나치 수
        val tmp = cur + next
        cur = next
        next = tmp
    }
}
```

이 코드는 [피보나치 수](https://en.wikipedia.org/wiki/Fibonacci_number)의 지연 `Sequence`를 만든다. 이 시퀀스는 잠재적으로 무한하다([Haskell의 무한 리스트](http://www.techrepublic.com/article/infinite-list-tricks-in-haskell/)와 똑같이). 예를 들어 `take()`로 일부만 요청할 수 있다.

```kotlin
println(fibonacci.take(10).joinToString())
```

> `1, 1, 2, 3, 5, 8, 13, 21, 34, 55`가 출력된다.
> 이 코드는 [여기](https://github.com/kotlin/kotlin-coroutines-examples/tree/master/examples/sequence/fibonacci.kt)에서 실행해 볼 수 있다.

제너레이터의 강점은 임의의 제어 흐름을 지원한다는 데 있다. 위 예제의 `while`은 물론 `if`, `try`/`catch`/`finally` 등 무엇이든 쓸 수 있다.

```kotlin
val seq = sequence {
    yield(firstItem) // 일시 중단 지점

    for (item in input) {
        if (!item.isValid()) break // 더 이상 원소를 생성하지 않는다
        val foo = item.toFoo()
        if (!foo.isGood()) continue
        yield(foo) // 일시 중단 지점        
    }
    
    try {
        yield(lastItem()) // 일시 중단 지점
    }
    finally {
        // 마무리 코드
    }
} 
```

> `sequence{}`와 `yield()`의 예제 코드는 [제한된 일시 중단](#제한된-일시-중단) 절에 있다.

이 접근 방식은 `yieldAll(sequence)`도 라이브러리 함수로 표현할 수 있게 해 준다(`sequence{}`와 `yield()`가 그렇듯이). 덕분에 지연 시퀀스를 이어 붙이기가 단순해지고 효율적인 구현이 가능해진다.

### 비동기 UI

전형적인 UI 애플리케이션에는 모든 UI 연산이 일어나는 단일 이벤트 디스패치 스레드가 있다. 다른 스레드에서 UI 상태를 수정하는 것은 보통 허용되지 않는다. 모든 UI 라이브러리는 실행을 UI 스레드로 되돌리는 프리미티브를 제공한다. 예를 들어 Swing에는 [`SwingUtilities.invokeLater`](https://docs.oracle.com/javase/8/docs/api/javax/swing/SwingUtilities.html#invokeLater-java.lang.Runnable-)가, JavaFX에는 [`Platform.runLater`](https://docs.oracle.com/javase/8/javafx/api/javafx/application/Platform.html#runLater-java.lang.Runnable-)가, Android에는 [`Activity.runOnUiThread`](https://developer.android.com/reference/android/app/Activity.html#runOnUiThread(java.lang.Runnable))가 있다. 다음은 비동기 작업을 수행한 뒤 결과를 UI에 표시하는 전형적인 Swing 애플리케이션의 코드 조각이다.

```kotlin
makeAsyncRequest {
    // 이 람다는 비동기 요청이 완료되면 실행된다
    result, exception ->
    
    if (exception == null) {
        // 결과를 UI에 표시한다
        SwingUtilities.invokeLater {
            display(result)   
        }
    } else {
       // 예외를 처리한다
    }
}
```

이는 [비동기 연산](#비동기-연산) 유스 케이스에서 본 콜백 지옥과 비슷하며, 역시 코루틴이 우아하게 해결한다.

```kotlin
launch(Swing) {
    try {
        // 비동기로 요청하는 동안 일시 중단한다
        val result = makeRequest()
        // 결과를 UI에 표시한다. 여기서 Swing 컨텍스트가 항상 이벤트 디스패치 스레드에 머물도록 보장한다
        display(result)
    } catch (exception: Throwable) {
        // 예외를 처리한다
    }
}
```

> `Swing` 컨텍스트의 예제 코드는 [컨티뉴에이션 인터셉터](#컨티뉴에이션-인터셉터) 절에 있다.

모든 예외 처리를 자연스러운 언어 구문으로 수행한다.

### 그 밖의 유스 케이스

코루틴은 다음을 포함해 훨씬 많은 유스 케이스를 포괄할 수 있다.

* 채널 기반 동시성(고루틴과 채널이라고도 한다)
* 액터 기반 동시성
* 가끔 사용자 상호작용이 필요한 백그라운드 프로세스(예: 모달 다이얼로그 표시)
* 통신 프로토콜: 각 액터를 상태 머신이 아니라 시퀀스로 구현
* 웹 애플리케이션 워크플로: 사용자 등록, 이메일 검증, 로그인(일시 중단된 코루틴을 직렬화해 DB에 저장할 수 있다)

## 코루틴 개요

이 절에서는 코루틴을 작성할 수 있게 해 주는 언어 메커니즘과 그 의미론을 관장하는 표준 라이브러리를 개관한다.

### 용어

 *  *코루틴*(coroutine) — *일시 중단 가능한 연산*의 *인스턴스*. 실행할 코드 블록을 받아 비슷한 생명 주기를 가진다는 점에서 개념적으로 스레드와 유사하다. 즉 *생성*되고 *시작*되지만, 특정 스레드에 묶이지 않는다. 한 스레드에서 실행을 *일시 중단*했다가 다른 스레드에서 *재개*할 수 있다. 게다가 퓨처나 프로미스처럼 어떤 결과(값 또는 예외)와 함께 *완료*될 수 있다.

 *  *일시 중단 함수*(suspending function) — `suspend` 변경자가 붙은 함수. 다른 일시 중단 함수를 호출함으로써, 현재 실행 스레드를 블로킹하지 않고 코드 실행을 *일시 중단*할 수 있다. 일시 중단 함수는 일반 코드에서는 호출할 수 없고, 다른 일시 중단 함수와 일시 중단 람다(아래 참조)에서만 호출할 수 있다. 예를 들어 [유스 케이스](#유스-케이스)에서 본 `.await()`와 `yield()`는 라이브러리에 정의될 수 있는 일시 중단 함수다. 표준 라이브러리는 그 밖의 모든 일시 중단 함수를 정의하는 데 쓰이는 원시(primitive) 일시 중단 함수를 제공한다.

 *  *일시 중단 람다*(suspending lambda) — 코루틴 안에서 실행돼야 하는 코드 블록. 일반적인 [람다 식](https://kotlinlang.org/docs/reference/lambdas.html)과 똑같이 생겼지만 함수 타입에 `suspend` 변경자가 붙는다. 일반 람다 식이 익명 로컬 함수의 짧은 문법 형태이듯, 일시 중단 람다는 익명 일시 중단 함수의 짧은 문법 형태다. 일시 중단 함수를 호출함으로써, 현재 실행 스레드를 블로킹하지 않고 코드 실행을 *일시 중단*할 수 있다. 예를 들어 [유스 케이스](#유스-케이스)에서 본 `launch`, `future`, `sequence` 함수 뒤의 중괄호 코드 블록이 일시 중단 람다다.

    > 참고: 일반 람다도 해당 람다에서의 [비지역](https://kotlinlang.org/docs/reference/returns.html) `return` 문이 허용되는 모든 위치에서 일시 중단 함수를 호출할 수 있다. 즉 [`apply{}` 블록](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/apply.html) 같은 인라인 람다 안에서는 일시 중단 함수 호출이 허용되지만, `noinline`이나 `crossinline` 내부 람다 식에서는 허용되지 않는다. *일시 중단*은 특수한 종류의 비지역 제어 이동으로 취급된다.

 *  *일시 중단 함수 타입*(suspending function type) — 일시 중단 함수와 람다를 위한 함수 타입. 일반 [함수 타입](https://kotlinlang.org/docs/reference/lambdas.html#function-types)과 같지만 `suspend` 변경자가 붙는다. 예를 들어 `suspend () -> Int`는 인자 없이 `Int`를 반환하는 일시 중단 함수의 타입이다. `suspend fun foo(): Int`처럼 선언된 일시 중단 함수가 이 함수 타입에 부합한다.

 *  *코루틴 빌더*(coroutine builder) — *일시 중단 람다*를 인자로 받아 코루틴을 생성하고, 선택적으로 어떤 형태로든 그 결과에 접근할 수단을 제공하는 함수. 예를 들어 [유스 케이스](#유스-케이스)에서 본 `launch{}`, `future{}`, `sequence{}`가 코루틴 빌더다. 표준 라이브러리는 그 밖의 모든 코루틴 빌더를 정의하는 데 쓰이는 원시 코루틴 빌더를 제공한다.

    > 참고: 일부 언어는 코루틴을 생성·시작하는 특정 방식을 하드코딩으로 지원하며, 그 실행과 결과가 어떻게 표현되는지도 언어가 정의한다. 예를 들어 `generate` *키워드*는 특정 종류의 이터러블 객체를 반환하는 코루틴을, `async` *키워드*는 특정 종류의 프로미스나 태스크를 반환하는 코루틴을 정의하는 식이다. Kotlin에는 코루틴을 정의하고 시작하는 키워드나 변경자가 없다. 코루틴 빌더는 그저 라이브러리에 정의된 함수다. 다른 언어에서 코루틴 정의가 메서드 본문 형태를 띠는 경우, Kotlin에서는 보통 마지막 인자가 일시 중단 람다인 라이브러리 정의 코루틴 빌더 호출을 식 본문으로 갖는 일반 메서드가 된다.

    ```kotlin
    fun doSomethingAsync() = async { ... }
    ```

 *  *일시 중단 지점*(suspension point) — 코루틴 실행 중 코루틴 실행이 *일시 중단될 수 있는* 지점. 문법적으로 일시 중단 지점은 일시 중단 함수의 호출이지만, *실제* 일시 중단은 그 일시 중단 함수가 실행을 중단하는 표준 라이브러리 프리미티브를 호출할 때 일어난다.

 *  *컨티뉴에이션*(continuation) — 일시 중단 지점에서 멈춘 코루틴의 상태. 개념적으로는 일시 중단 지점 이후의 나머지 실행을 나타낸다. 예를 들면 이렇다.

    ```kotlin
    sequence {
        for (i in 1..10) yield(i * i)
        println("over")
    }  
    ```  

    여기서 코루틴이 일시 중단 함수 `yield()` 호출에서 멈출 때마다 *나머지 실행*이 컨티뉴에이션으로 표현된다. 따라서 컨티뉴에이션은 10개다. 첫 번째는 `i = 2`로 루프를 돌고 멈추고, 두 번째는 `i = 3`으로 루프를 돌고 멈추고, ... 마지막은 "over"를 출력하고 코루틴을 완료한다. *생성*됐지만 아직 *시작*되지 않은 코루틴은 전체 실행으로 이루어진 `Continuation<Unit>` 타입의 *초기 컨티뉴에이션*으로 표현된다.

앞서 말했듯 코루틴의 핵심 요구 사항 하나는 유연성이다. 수많은 기존 비동기 API와 기타 유스 케이스를 지원하면서 컴파일러에 하드코딩되는 부분을 최소화하고자 한다. 그 결과 컴파일러는 일시 중단 함수, 일시 중단 람다, 그리고 그에 대응하는 일시 중단 함수 타입의 지원만 책임진다. 표준 라이브러리에는 소수의 프리미티브만 있고 나머지는 애플리케이션 라이브러리에 맡긴다.

### Continuation 인터페이스

범용 콜백을 나타내는 표준 라이브러리 인터페이스 [`Continuation`](http://kotlinlang.org/api/latest/jvm/stdlib/kotlin.coroutines/-continuation/index.html)(`kotlin.coroutines` 패키지에 정의)의 정의는 다음과 같다.

```kotlin
interface Continuation<in T> {
   val context: CoroutineContext
   fun resumeWith(result: Result<T>)
}
```

context는 [코루틴 컨텍스트](#코루틴-컨텍스트) 절에서 자세히 다루며, 코루틴에 결부되는 임의의 사용자 정의 컨텍스트를 나타낸다. `resumeWith` 함수는 코루틴 완료 시 성공(값과 함께)이나 실패(예외와 함께)를 보고하는 데 쓰는 *완료(completion)* 콜백이다.

편의를 위해 같은 패키지에 두 개의 확장 함수가 정의돼 있다.

```kotlin
fun <T> Continuation<T>.resume(value: T)
fun <T> Continuation<T>.resumeWithException(exception: Throwable)
```

### 일시 중단 함수

`.await()` 같은 전형적인 *일시 중단 함수*의 구현은 다음과 같다.

```kotlin
suspend fun <T> CompletableFuture<T>.await(): T =
    suspendCoroutine<T> { cont: Continuation<T> ->
        whenComplete { result, exception ->
            if (exception == null) // 퓨처가 정상적으로 완료됨
                cont.resume(result)
            else // 퓨처가 예외와 함께 완료됨
                cont.resumeWithException(exception)
        }
    }
```

> 이 코드는 [여기](https://github.com/kotlin/kotlin-coroutines-examples/tree/master/examples/future/await.kt)에서 볼 수 있다.
> 참고: 이 단순 구현은 퓨처가 영영 완료되지 않으면 코루틴을 영원히 중단시킨다. [kotlinx.coroutines](https://github.com/kotlin/kotlinx.coroutines)의 실제 구현은 취소를 지원한다.

`suspend` 변경자는 이 함수가 코루틴 실행을 일시 중단할 수 있음을 나타낸다. 이 함수는 `CompletableFuture<T>` 타입의 [확장 함수](https://kotlinlang.org/docs/reference/extensions.html)로 정의돼 있어서, 사용할 때 실제 실행 순서에 대응하는 왼쪽에서 오른쪽 순서로 자연스럽게 읽힌다.

```kotlin
doSomethingAsync(...).await()
```

`suspend` 변경자는 최상위 함수, 확장 함수, 멤버 함수, 지역 함수, 연산자 함수 등 어떤 함수에도 붙일 수 있다.

> 프로퍼티 게터와 세터, 생성자, 일부 연산자 함수(구체적으로 `getValue`, `setValue`, `provideDelegate`, `get`, `set`, `equals`)에는 `suspend` 변경자를 붙일 수 없다. 이 제한은 앞으로 완화될 수 있다.

일시 중단 함수는 어떤 일반 함수든 호출할 수 있지만, 실제로 실행을 일시 중단하려면 다른 일시 중단 함수를 호출해야 한다. 특히 위의 `await` 구현은 표준 라이브러리(`kotlin.coroutines` 패키지)에 최상위 일시 중단 함수로 정의된 [`suspendCoroutine`](http://kotlinlang.org/api/latest/jvm/stdlib/kotlin.coroutines/suspend-coroutine.html)을 호출한다.

```kotlin
suspend fun <T> suspendCoroutine(block: (Continuation<T>) -> Unit): T
```

`suspendCoroutine`이 코루틴 안에서 호출되면(일시 중단 함수이므로 코루틴 안에서*만* 호출될 수 있다), 코루틴의 실행 상태를 *컨티뉴에이션* 인스턴스에 담아 지정된 `block`의 인자로 넘긴다. 코루틴 실행을 재개하려면 block이 `continuation.resumeWith()`를(직접 또는 `continuation.resume()`, `continuation.resumeWithException()` 확장을 사용해) 이 스레드나 다른 스레드에서 나중에 호출하면 된다. 코루틴의 *실제* 일시 중단은 `suspendCoroutine`의 block이 `resumeWith`를 호출하지 않고 반환할 때 일어난다. block 안에서 반환하기 전에 컨티뉴에이션이 재개됐다면 코루틴은 일시 중단된 것으로 간주되지 않고 실행을 계속한다.

`continuation.resumeWith()`에 전달된 결과가 `suspendCoroutine` 호출의 결과가 되고, 이것이 다시 `.await()`의 결과가 된다.

같은 컨티뉴에이션을 두 번 이상 재개하는 것은 허용되지 않으며 `IllegalStateException`이 발생한다.

> 참고: 이것이 Kotlin의 코루틴과 Scheme 같은 함수형 언어의 일급 구획된 컨티뉴에이션 또는 Haskell의 컨티뉴에이션 모나드의 핵심 차이다. 한 번만 재개 가능한 컨티뉴에이션만 지원하기로 한 선택은 순전히 실용적인 것으로, 의도한 [유스 케이스](#유스-케이스) 중 어느 것도 멀티샷(multi-shot) 컨티뉴에이션을 필요로 하지 않기 때문이다. 다만 멀티샷 컨티뉴에이션은 별도 라이브러리로 구현할 수 있다. 저수준 [코루틴 인트린식](#코루틴-인트린식)으로 코루틴을 일시 중단하고, 컨티뉴에이션에 포착된 코루틴 상태를 복제해 그 복제본을 다시 재개하는 방식이다.

### 코루틴 빌더

일시 중단 함수는 일반 함수에서 호출할 수 없으므로, 표준 라이브러리는 일반적인(일시 중단이 아닌) 스코프에서 코루틴 실행을 시작하는 함수를 제공한다. 다음은 단순한 `launch{}` *코루틴 빌더*의 구현이다.

```kotlin
fun launch(context: CoroutineContext = EmptyCoroutineContext, block: suspend () -> Unit) =
    block.startCoroutine(Continuation(context) { result ->
        result.onFailure { exception ->
            val currentThread = Thread.currentThread()
            currentThread.uncaughtExceptionHandler.uncaughtException(currentThread, exception)
        }
    })
```

> 이 코드는 [여기](https://github.com/kotlin/kotlin-coroutines-examples/tree/master/examples/run/launch.kt)에서 볼 수 있다.

이 구현은 [`Continuation(context) { ... }`](http://kotlinlang.org/api/latest/jvm/stdlib/kotlin.coroutines/-continuation.html) 함수(`kotlin.coroutines` 패키지)를 사용한다. 이 함수는 주어진 `context` 값과 `resumeWith` 함수 본문으로 `Continuation` 인터페이스 구현을 만드는 지름길을 제공한다. 이 컨티뉴에이션은 *완료 컨티뉴에이션*(completion continuation)으로서 [`block.startCoroutine(...)`](http://kotlinlang.org/api/latest/jvm/stdlib/kotlin.coroutines/start-coroutine.html) 확장 함수(`kotlin.coroutines` 패키지)에 전달된다.

코루틴이 완료되면 완료 컨티뉴에이션이 호출된다. 코루틴이 성공 또는 실패로 *완료*될 때 그 `resumeWith` 함수가 호출된다. `launch`는 "발사 후 망각(fire-and-forget)" 코루틴이므로 반환 타입이 `Unit`인 일시 중단 함수에 대해 정의되고, `resume` 함수에서 이 결과를 실제로 무시한다. 코루틴 실행이 예외로 완료되면 현재 스레드의 미처리 예외 핸들러(uncaught exception handler)로 이를 보고한다.

> 참고: 이 단순 구현은 `Unit`을 반환하고 코루틴 상태에 대한 어떤 접근도 제공하지 않는다. [kotlinx.coroutines](https://github.com/kotlin/kotlinx.coroutines)의 실제 구현은 더 복잡하다. 코루틴을 나타내고 취소할 수 있는 `Job` 인터페이스의 인스턴스를 반환하기 때문이다.

context는 [코루틴 컨텍스트](#코루틴-컨텍스트) 절에서 자세히 다룬다. `startCoroutine`은 표준 라이브러리에 파라미터가 없는 일시 중단 함수 타입과 단일 파라미터 일시 중단 함수 타입의 확장으로 정의돼 있다.

```kotlin
fun <T> (suspend  () -> T).startCoroutine(completion: Continuation<T>)
fun <R, T> (suspend  R.() -> T).startCoroutine(receiver: R, completion: Continuation<T>)
```

`startCoroutine`은 코루틴을 생성하고 현재 스레드에서 즉시(단, 아래 비고 참조) 첫 번째 *일시 중단 지점*까지 실행한 뒤 반환한다. 일시 중단 지점은 코루틴 본문 안의 어떤 [일시 중단 함수](#일시-중단-함수) 호출이며, 코루틴 실행이 언제 어떻게 재개될지는 해당 일시 중단 함수의 코드가 정한다.

> 참고: [뒤에서](#컨티뉴에이션-인터셉터) 다루는 (컨텍스트에 들어 있는) 컨티뉴에이션 인터셉터는 코루틴의 실행을 — *초기 컨티뉴에이션을 포함해* — 다른 스레드로 디스패치할 수 있다.

### 코루틴 컨텍스트

코루틴 컨텍스트는 코루틴에 붙일 수 있는 사용자 정의 객체들의 영속(persistent) 집합이다. 코루틴의 스레딩 정책, 로깅, 실행의 보안·트랜잭션 측면, 코루틴의 식별자와 이름 등을 담당하는 객체가 들어갈 수 있다. 코루틴과 컨텍스트에 대한 단순한 심적 모델은 이렇다. 코루틴을 경량 스레드라고 생각하자. 그러면 코루틴 컨텍스트는 스레드 로컬 변수들의 모음과 비슷하다. 차이점은 스레드 로컬 변수는 가변이지만 코루틴 컨텍스트는 불변이라는 점이다. 코루틴은 아주 가벼워서 컨텍스트에서 뭔가 바꿔야 할 때 새 코루틴을 띄우면 그만이므로, 이는 심각한 제약이 아니다.

표준 라이브러리에는 컨텍스트 요소의 구체적 구현이 전혀 없다. 대신 인터페이스와 추상 클래스가 있어서 이 모든 측면을 라이브러리에서 *조합 가능한* 방식으로 정의할 수 있고, 서로 다른 라이브러리의 측면들이 같은 컨텍스트의 요소로서 평화롭게 공존할 수 있다.

개념적으로 코루틴 컨텍스트는 요소들의 인덱스된 집합이며, 각 요소는 고유한 키를 가진다. 집합(set)과 맵(map)의 혼합인 셈이다. 요소들은 맵처럼 키를 갖지만, 키가 요소와 직접 결부돼 있다는 점은 집합에 가깝다. 표준 라이브러리는 [`CoroutineContext`](http://kotlinlang.org/api/latest/jvm/stdlib/kotlin.coroutines/-coroutine-context/index.html)의 최소 인터페이스를 정의한다(`kotlin.coroutines` 패키지).

```kotlin
interface CoroutineContext {
    operator fun <E : Element> get(key: Key<E>): E?
    fun <R> fold(initial: R, operation: (R, Element) -> R): R
    operator fun plus(context: CoroutineContext): CoroutineContext
    fun minusKey(key: Key<*>): CoroutineContext

    interface Element : CoroutineContext {
        val key: Key<*>
    }

    interface Key<E : Element>
}
```

`CoroutineContext` 자체에는 네 가지 핵심 연산이 있다.

* 연산자 [`get`](http://kotlinlang.org/api/latest/jvm/stdlib/kotlin.coroutines/-coroutine-context/get.html)은 주어진 키의 요소에 대한 타입 안전한 접근을 제공한다. [Kotlin 연산자 오버로딩](https://kotlinlang.org/docs/reference/operator-overloading.html)에서 설명하듯 `[..]` 표기로 쓸 수 있다.
* 함수 [`fold`](http://kotlinlang.org/api/latest/jvm/stdlib/kotlin.coroutines/-coroutine-context/fold.html)는 표준 라이브러리의 [`Collection.fold`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/fold.html) 확장처럼 동작하며 컨텍스트의 모든 요소를 순회하는 수단을 제공한다.
* 연산자 [`plus`](http://kotlinlang.org/api/latest/jvm/stdlib/kotlin.coroutines/-coroutine-context/plus.html)는 표준 라이브러리의 [`Set.plus`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/plus.html) 확장처럼 동작하며, 두 컨텍스트의 조합을 반환한다. 이때 plus의 오른쪽 요소가 같은 키를 가진 왼쪽 요소를 대체한다.
* 함수 [`minusKey`](http://kotlinlang.org/api/latest/jvm/stdlib/kotlin.coroutines/-coroutine-context/minus-key.html)는 지정한 키를 포함하지 않는 컨텍스트를 반환한다.

코루틴 컨텍스트의 [`Element`](http://kotlinlang.org/api/latest/jvm/stdlib/kotlin.coroutines/-coroutine-context/-element/index.html)는 그 자체로 하나의 컨텍스트다. 해당 요소 하나만 가진 싱글턴 컨텍스트인 것이다. 이 덕분에 라이브러리에 정의된 코루틴 컨텍스트 요소들을 `+`로 이어 붙여 복합 컨텍스트를 만들 수 있다. 예를 들어 한 라이브러리가 사용자 인가 정보를 담은 `auth` 요소를 정의하고, 다른 라이브러리가 실행 컨텍스트 정보를 담은 `threadPool` 객체를 정의했다면, `launch(auth + threadPool) {...}` 호출로 결합된 컨텍스트와 함께 `launch{}` [코루틴 빌더](#코루틴-빌더)를 쓸 수 있다.

> 참고: [kotlinx.coroutines](https://github.com/kotlin/kotlinx.coroutines)는 여러 컨텍스트 요소를 제공한다. 코루틴 실행을 공유 백그라운드 스레드 풀로 디스패치하는 `Dispatchers.Default` 객체도 그중 하나다.

표준 라이브러리는 [`EmptyCoroutineContext`](http://kotlinlang.org/api/latest/jvm/stdlib/kotlin.coroutines/-empty-coroutine-context/index.html) — 아무 요소도 없는(빈) `CoroutineContext` 인스턴스 — 를 제공한다.

서드파티 컨텍스트 요소는 모두 표준 라이브러리(`kotlin.coroutines` 패키지)가 제공하는 [`AbstractCoroutineContextElement`](http://kotlinlang.org/api/latest/jvm/stdlib/kotlin.coroutines/-abstract-coroutine-context-element/index.html) 클래스를 확장해야 한다. 라이브러리에서 컨텍스트 요소를 정의할 때는 다음 스타일을 권장한다. 아래 예제는 현재 사용자 이름을 저장하는 가상의 인가 컨텍스트 요소다.

```kotlin
class AuthUser(val name: String) : AbstractCoroutineContextElement(AuthUser) {
    companion object Key : CoroutineContext.Key<AuthUser>
}
```

> 이 예제는 [여기](https://github.com/kotlin/kotlin-coroutines-examples/tree/master/examples/context/auth.kt)에서 볼 수 있다.

컨텍스트 `Key`를 해당 요소 클래스의 동반 객체(companion object)로 정의하면 컨텍스트의 해당 요소에 유창하게 접근할 수 있다. 다음은 현재 사용자 이름을 확인해야 하는 일시 중단 함수의 가상 구현이다.

```kotlin
suspend fun doSomething() {
    val currentUser = coroutineContext[AuthUser]?.name ?: throw SecurityException("unauthorized")
    // 사용자별 작업을 수행한다
}
```

여기서는 일시 중단 함수 안에서 현재 코루틴의 컨텍스트를 가져올 수 있는 최상위 [`coroutineContext`](http://kotlinlang.org/api/latest/jvm/stdlib/kotlin.coroutines/coroutine-context.html) 프로퍼티(`kotlin.coroutines` 패키지)를 사용한다.

### 컨티뉴에이션 인터셉터

[비동기 UI](#비동기-ui) 유스 케이스를 되짚어 보자. 비동기 UI 애플리케이션은 여러 일시 중단 함수가 임의의 스레드에서 코루틴 실행을 재개하더라도 코루틴 본문 자체는 항상 UI 스레드에서 실행되도록 보장해야 한다. 이는 *컨티뉴에이션 인터셉터*(continuation interceptor)로 달성한다. 먼저 코루틴의 생명 주기를 온전히 이해해야 한다. [`launch{}`](#코루틴-빌더) 코루틴 빌더를 사용하는 코드 조각을 보자.

```kotlin
launch(Swing) {
    initialCode() // 초기 코드의 실행
    f1.await() // 일시 중단 지점 #1
    block1() // 실행 #1
    f2.await() // 일시 중단 지점 #2
    block2() // 실행 #2
}
```

코루틴은 `initialCode`의 실행으로 시작해 첫 번째 일시 중단 지점까지 진행한다. 일시 중단 지점에서 *일시 중단*하고, 해당 일시 중단 함수가 정하는 대로 얼마 뒤 *재개*해 `block1`을 실행한다. 그리고 다시 일시 중단했다가 재개해 `block2`를 실행하고, 그 뒤 *완료*한다.

컨티뉴에이션 인터셉터는 `initialCode`, `block1`, `block2`의 실행에 해당하는 컨티뉴에이션을 — 재개 시점부터 다음 일시 중단 지점까지 — 가로채 감쌀 수 있다. 코루틴의 초기 코드는 *초기 컨티뉴에이션*의 재개로 취급된다. 표준 라이브러리는 [`ContinuationInterceptor`](http://kotlinlang.org/api/latest/jvm/stdlib/kotlin.coroutines/-continuation-interceptor/index.html) 인터페이스(`kotlin.coroutines` 패키지)를 제공한다.

```kotlin
interface ContinuationInterceptor : CoroutineContext.Element {
    companion object Key : CoroutineContext.Key<ContinuationInterceptor>
    fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T>
    fun releaseInterceptedContinuation(continuation: Continuation<*>)
}
```

`interceptContinuation` 함수는 코루틴의 컨티뉴에이션을 감싼다. 코루틴이 일시 중단될 때마다 코루틴 프레임워크는 다음 코드로 이후 재개에 쓸 실제 `continuation`을 감싼다.

```kotlin
val intercepted = continuation.context[ContinuationInterceptor]?.interceptContinuation(continuation) ?: continuation
```

코루틴 프레임워크는 실제 컨티뉴에이션 인스턴스별로 가로채진(intercepted) 컨티뉴에이션을 캐시하고, 더 이상 필요 없어지면 `releaseInterceptedContinuation(intercepted)`를 호출한다. 자세한 내용은 [구현 세부 사항](#구현-세부-사항) 절을 보라.

> 참고로 `await` 같은 일시 중단 함수는 코루틴 실행을 실제로 중단할 수도 있고 그러지 않을 수도 있다. 예를 들어 [일시 중단 함수](#일시-중단-함수) 절에서 본 `await` 구현은 퓨처가 이미 완료돼 있으면 코루틴을 실제로 중단하지 않는다(이 경우 즉시 `resume`을 호출하고 실행은 실제 중단 없이 계속된다). 컨티뉴에이션은 코루틴 실행 중 실제 일시 중단이 일어날 때만, 즉 `suspendCoroutine`의 block이 `resume` 호출 없이 반환할 때만 가로채진다.

Swing UI 이벤트 디스패치 스레드로 실행을 디스패치하는 `Swing` 인터셉터의 구체적인 예제 코드를 보자. 먼저 `SwingUtilities.invokeLater`로 컨티뉴에이션을 Swing 이벤트 디스패치 스레드로 디스패치하는 `SwingContinuation` 래퍼 클래스를 정의한다.

```kotlin
private class SwingContinuation<T>(val cont: Continuation<T>) : Continuation<T> {
    override val context: CoroutineContext = cont.context
    
    override fun resumeWith(result: Result<T>) {
        SwingUtilities.invokeLater { cont.resumeWith(result) }
    }
}
```

그다음, 해당 컨텍스트 요소 역할을 하며 `ContinuationInterceptor` 인터페이스를 구현하는 `Swing` 객체를 정의한다.

```kotlin
object Swing : AbstractCoroutineContextElement(ContinuationInterceptor), ContinuationInterceptor {
    override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T> =
        SwingContinuation(continuation)
}
```

> 이 코드는 [여기](https://github.com/kotlin/kotlin-coroutines-examples/tree/master/examples/context/swing.kt)에서 볼 수 있다.
> 참고: [kotlinx.coroutines](https://github.com/kotlin/kotlinx.coroutines)의 실제 `Swing` 객체 구현은 코루틴 디버깅 기능도 지원한다. 현재 실행 중인 코루틴의 식별자를 제공하고, 이 코루틴을 실행 중인 스레드의 이름에 표시해 준다.

이제 `Swing` 파라미터와 함께 `launch{}` [코루틴 빌더](#코루틴-빌더)를 사용하면 온전히 Swing 이벤트 디스패치 스레드에서 실행되는 코루틴을 만들 수 있다.

 ```kotlin
launch(Swing) {
   // 이 안의 코드는 일시 중단할 수 있지만, 항상 Swing EDT에서 재개된다
}
```

> [kotlinx.coroutines](https://github.com/kotlin/kotlinx.coroutines)의 실제 `Swing` 컨텍스트 구현은 라이브러리의 시간·디버깅 기능과 통합돼 있어 더 복잡하다.

### 제한된 일시 중단

[제너레이터](#제너레이터) 유스 케이스의 `sequence{}`와 `yield()`를 구현하려면 다른 종류의 코루틴 빌더와 일시 중단 함수가 필요하다. 다음은 `sequence{}` 코루틴 빌더의 예제 코드다.

```kotlin
fun <T> sequence(block: suspend SequenceScope<T>.() -> Unit): Sequence<T> = Sequence {
    SequenceCoroutine<T>().apply {
        nextStep = block.createCoroutine(receiver = this, completion = this)
    }
}
```

이 코드는 표준 라이브러리의 또 다른 프리미티브인 [`createCoroutine`](http://kotlinlang.org/api/latest/jvm/stdlib/kotlin.coroutines/create-coroutine.html)을 사용한다. [코루틴 빌더](#코루틴-빌더) 절에서 설명한 `startCoroutine`과 비슷하지만, 코루틴을 *생성*만 하고 시작하지는 *않는다*. 대신 *초기 컨티뉴에이션*을 `Continuation<Unit>` 참조로 반환한다.

```kotlin
fun <T> (suspend () -> T).createCoroutine(completion: Continuation<T>): Continuation<Unit>
fun <R, T> (suspend R.() -> T).createCoroutine(receiver: R, completion: Continuation<T>): Continuation<Unit>
```

또 다른 차이는 이 빌더의 *일시 중단 람다* `block`이 `SequenceScope<T>`를 리시버로 갖는 [*확장 람다*](https://kotlinlang.org/docs/reference/lambdas.html#function-literals-with-receiver)라는 점이다. `SequenceScope` 인터페이스는 제너레이터 블록의 *스코프*를 제공하며 라이브러리에 이렇게 정의된다.

```kotlin
interface SequenceScope<in T> {
    suspend fun yield(value: T)
}
```

여러 객체의 생성을 피하기 위해 `sequence{}` 구현은 `SequenceScope<T>`를 구현하면서 `Continuation<Unit>`도 구현하는 `SequenceCoroutine<T>` 클래스를 정의한다. 그래서 `createCoroutine`의 `receiver` 파라미터와 `completion` 컨티뉴에이션 파라미터 역할을 모두 할 수 있다. `SequenceCoroutine<T>`의 단순한 구현은 아래와 같다.

```kotlin
private class SequenceCoroutine<T>: AbstractIterator<T>(), SequenceScope<T>, Continuation<Unit> {
    lateinit var nextStep: Continuation<Unit>

    // AbstractIterator 구현
    override fun computeNext() { nextStep.resume(Unit) }

    // 완료 컨티뉴에이션 구현
    override val context: CoroutineContext get() = EmptyCoroutineContext

    override fun resumeWith(result: Result<Unit>) {
        result.getOrThrow() // 오류가 있으면 바로 빠져나간다
        done()
    }

    // 제너레이터 구현
    override suspend fun yield(value: T) {
        setNext(value)
        return suspendCoroutine { cont -> nextStep = cont }
    }
}
```

> 이 코드는 [여기](https://github.com/kotlin/kotlin-coroutines-examples/tree/master/examples/sequence/sequence.kt)에서 볼 수 있다.
> 참고: 표준 라이브러리는 이 [`sequence`](http://kotlinlang.org/api/latest/jvm/stdlib/kotlin.sequences/sequence.html) 함수의 최적화된 구현을 기본 제공하며(`kotlin.sequences` 패키지), [`yieldAll`](http://kotlinlang.org/api/latest/jvm/stdlib/kotlin.sequences/-sequence-scope/yield-all.html) 함수도 추가로 지원한다.

> 실제 `sequence` 코드는 실험적 기능인 `BuilderInference`를 사용한다. 이 기능 덕분에 [제너레이터](#제너레이터) 절에서 본 `fibonacci`를 시퀀스 타입 파라미터 `T`를 명시하지 않고 선언할 수 있다. 대신 `yield` 호출에 전달된 타입에서 추론된다.

`yield`의 구현은 `suspendCoroutine` [일시 중단 함수](#일시-중단-함수)로 코루틴을 일시 중단하고 그 컨티뉴에이션을 포착한다. 컨티뉴에이션은 `nextStep`에 저장되어 `computeNext`가 호출될 때 재개된다.

그러나 위에서 본 `sequence{}`와 `yield()`는 임의의 일시 중단 함수가 그 스코프에서 컨티뉴에이션을 포착하도록 열어 둘 수 없다. 이들은 *동기적으로* 동작한다. 컨티뉴에이션이 어떻게 포착되고, 어디에 저장되며, 언제 재개되는지에 대한 절대적인 통제가 필요하다. 이들은 *제한된 일시 중단 스코프*(restricted suspension scope)를 형성한다. 일시 중단을 제한하는 능력은 스코프 클래스나 인터페이스에 붙이는 `@RestrictsSuspension` 어노테이션이 제공한다. 위 예제에서 스코프 인터페이스는 `SequenceScope`다.

```kotlin
@RestrictsSuspension
interface SequenceScope<in T> {
    suspend fun yield(value: T)
}
```

이 어노테이션은 `sequence{}` 같은 동기 코루틴 빌더의 스코프에서 사용할 수 있는 일시 중단 함수에 특정 제한을 강제한다. *제한된 일시 중단 스코프* 클래스나 인터페이스(`@RestrictsSuspension`가 붙은)를 리시버로 갖는 확장 일시 중단 람다·함수를 *제한된 일시 중단 함수*(restricted suspending function)라고 부른다. 제한된 일시 중단 함수는 자신의 제한된 일시 중단 스코프의 같은 인스턴스에 대한 멤버 또는 확장 일시 중단 함수만 호출할 수 있다.

특히 이는 스코프 안의 어떤 `SequenceScope` 확장 람다도 `suspendContinuation`이나 다른 범용 일시 중단 함수를 호출할 수 없다는 뜻이다. `sequence` 코루틴의 실행을 일시 중단하려면 결국 `SequenceScope.yield`를 호출해야 한다. `yield` 구현 자체는 `SequenceScope` 구현의 멤버 함수이므로 아무 제한을 받지 않는다(*확장* 일시 중단 람다와 함수만 제한된다).

`sequence` 같은 제한된 코루틴 빌더가 임의의 컨텍스트를 지원하는 것은 의미가 없다. 스코프 클래스나 인터페이스(이 예에서는 `SequenceScope`)가 컨텍스트 역할을 하기 때문이다. 그래서 제한된 코루틴은 항상 `EmptyCoroutineContext`를 컨텍스트로 써야 하며, `SequenceCoroutine`의 `context` 프로퍼티 게터 구현이 반환하는 것도 바로 이것이다. `EmptyCoroutineContext`가 아닌 컨텍스트로 제한된 코루틴을 생성하려고 하면 `IllegalArgumentException`이 발생한다.

## 구현 세부 사항

이 절은 코루틴의 구현 세부 사항을 들여다본다. 이것들은 [코루틴 개요](#코루틴-개요) 절에서 설명한 구성 요소 뒤에 숨겨져 있으며, 공개 API와 ABI의 계약을 깨지 않는 한 내부 클래스와 코드 생성 전략은 언제든 바뀔 수 있다.

### 컨티뉴에이션 전달 스타일

일시 중단 함수는 컨티뉴에이션 전달 스타일(Continuation-Passing-Style, CPS)로 구현된다. 모든 일시 중단 함수와 일시 중단 람다는 호출될 때 암묵적으로 전달되는 추가 `Continuation` 파라미터를 가진다. [`await` 일시 중단 함수](#일시-중단-함수)의 선언이 이렇게 생겼다는 것을 떠올려 보자.

```kotlin
suspend fun <T> CompletableFuture<T>.await(): T
```

그러나 실제 *구현*은 *CPS 변환* 후 다음 시그니처를 가진다.

```kotlin
fun <T> CompletableFuture<T>.await(continuation: Continuation<T>): Any?
```

결과 타입 `T`가 추가된 continuation 파라미터의 타입 인자 위치로 옮겨 갔다. 구현의 결과 타입 `Any?`는 일시 중단 함수의 동작을 표현하도록 설계됐다. 일시 중단 함수가 코루틴을 *일시 중단*하면 특별한 마커 값 `COROUTINE_SUSPENDED`를 반환한다([코루틴 인트린식](#코루틴-인트린식) 절에서 더 자세히 다룬다). 일시 중단 함수가 현재 코루틴을 중단하지 않고 실행을 계속하면 결과를 직접 반환하거나 예외를 던진다. 이렇게 `await` 구현의 `Any?` 반환 타입은 사실 Kotlin의 타입 시스템으로는 표현할 수 없는 `COROUTINE_SUSPENDED`와 `T`의 합집합(union)이다.

일시 중단 함수의 실제 구현은 자신의 스택 프레임에서 컨티뉴에이션을 직접 호출해서는 안 된다. 오래 실행되는 코루틴에서 스택 오버플로가 발생할 수 있기 때문이다. 표준 라이브러리의 `suspendCoroutine` 함수는 컨티뉴에이션 호출을 추적함으로써 이 복잡함을 애플리케이션 개발자에게서 감추고, 컨티뉴에이션이 언제 어떻게 호출되든 일시 중단 함수의 실제 구현 계약이 지켜지도록 보장한다.

### 상태 머신

코루틴을 효율적으로 구현하는 것, 즉 클래스와 객체를 최대한 적게 만드는 것이 매우 중요하다. 많은 언어가 코루틴을 *상태 머신*으로 구현하며 Kotlin도 마찬가지다. Kotlin의 경우 이 접근 방식 덕분에 컴파일러는 본문에 일시 중단 지점이 몇 개든 있을 수 있는 일시 중단 람다당 단 하나의 클래스만 만든다.

핵심 아이디어: 일시 중단 함수는 상태 머신으로 컴파일되며, 상태는 일시 중단 지점에 대응한다. 예시로 일시 중단 지점이 두 개인 일시 중단 블록을 보자.

```kotlin
val a = a()
val y = foo(a).await() // 일시 중단 지점 #1
b()
val z = bar(a, y).await() // 일시 중단 지점 #2
c(z)
```

이 코드 블록에는 세 가지 상태가 있다.

 * 초기(어떤 일시 중단 지점도 지나기 전)
 * 첫 번째 일시 중단 지점 이후
 * 두 번째 일시 중단 지점 이후

각 상태는 이 블록의 컨티뉴에이션 중 하나의 진입점이다(초기 컨티뉴에이션은 맨 첫 줄부터 이어진다).

이 코드는 상태 머신을 구현하는 메서드, 상태 머신의 현재 상태를 담는 필드, 상태 간에 공유되는 코루틴의 지역 변수 필드를 가진 익명 클래스로 컴파일된다(코루틴의 클로저를 위한 필드도 있을 수 있지만 이 경우는 비어 있다). 다음은 일시 중단 함수 `await`의 호출에 컨티뉴에이션 전달 스타일을 사용하는, 위 블록에 대한 의사 Java 코드다.

``` java
class <anonymous_for_state_machine> extends SuspendLambda<...> {
    // 상태 머신의 현재 상태
    int label = 0
    
    // 코루틴의 지역 변수
    A a = null
    Y y = null
    
    void resumeWith(Object result) {
        if (label == 0) goto L0
        if (label == 1) goto L1
        if (label == 2) goto L2
        else throw IllegalStateException()
        
      L0:
        // 이 호출에서 result는 `null`이어야 한다
        a = a()
        label = 1
        result = foo(a).await(this) // 'this'가 컨티뉴에이션으로 전달된다
        if (result == COROUTINE_SUSPENDED) return // await가 실행을 일시 중단했으면 반환한다
      L1:
        // 외부 코드가 .await()의 결과를 전달하며 이 코루틴을 재개했다
        y = (Y) result
        b()
        label = 2
        result = bar(a, y).await(this) // 'this'가 컨티뉴에이션으로 전달된다
        if (result == COROUTINE_SUSPENDED) return // await가 실행을 일시 중단했으면 반환한다
      L2:
        // 외부 코드가 .await()의 결과를 전달하며 이 코루틴을 재개했다
        Z z = (Z) result
        c(z)
        label = -1 // 더 이상의 단계는 허용되지 않는다
        return
    }          
}    
```

`goto` 연산자와 레이블이 있는 이유는, 이 예제가 소스 코드가 아니라 바이트 코드에서 일어나는 일을 묘사하기 때문이다.

이제 코루틴이 시작되면 `resumeWith()`가 호출된다. `label`이 `0`이므로 `L0`로 점프하고, 일을 좀 한 뒤 `label`을 다음 상태인 `1`로 설정하고 `.await()`를 호출하며, 코루틴 실행이 일시 중단됐다면 반환한다. 실행을 이어가고 싶을 때 다시 `resumeWith()`를 호출하면 이번에는 곧장 `L1`로 진행해 일을 좀 하고 상태를 `2`로 설정하고 `.await()`를 호출하며 일시 중단 시 또 반환한다. 다음번에는 `L3`부터 이어져(역주: 원문 그대로이며 문맥상 `L2`를 가리킨다) 상태를 `-1`로 설정하는데, 이는 "끝났고, 더 할 일이 없다"는 뜻이다.

루프 안의 일시 중단 지점은 상태를 하나만 만든다. 루프 역시 (조건부) `goto`로 동작하기 때문이다.

```kotlin
var x = 0
while (x < 10) {
    x += nextNumber().await()
}
```

위 코드는 다음과 같이 생성된다.

``` java
class <anonymous_for_state_machine> extends SuspendLambda<...> {
    // 상태 머신의 현재 상태
    int label = 0
    
    // 코루틴의 지역 변수
    int x
    
    void resumeWith(Object result) {
        if (label == 0) goto L0
        if (label == 1) goto L1
        else throw IllegalStateException()
        
      L0:
        x = 0
      LOOP:
        if (x >= 10) goto END
        label = 1
        result = nextNumber().await(this) // 'this'가 컨티뉴에이션으로 전달된다
        if (result == COROUTINE_SUSPENDED) return // await가 실행을 일시 중단했으면 반환한다
      L1:
        // 외부 코드가 .await()의 결과를 전달하며 이 코루틴을 재개했다
        x += ((Integer) result).intValue()
        label = -1
        goto LOOP
      END:
        label = -1 // 더 이상의 단계는 허용되지 않는다
        return 
    }          
}    
```

### 일시 중단 함수 컴파일

일시 중단 함수의 컴파일된 코드는 그 함수가 다른 일시 중단 함수를 언제 어떻게 호출하느냐에 따라 달라진다. 가장 단순한 경우, 일시 중단 함수는 다른 일시 중단 함수를 *꼬리 위치*(tail position)에서만 호출해 *꼬리 호출*(tail call)을 한다. 저수준 동기화 프리미티브를 구현하거나 콜백을 감싸는 일시 중단 함수의 전형적인 경우로, [일시 중단 함수](#일시-중단-함수) 절과 [콜백 감싸기](022-keep-kotlin-coroutines-part2.md#콜백-감싸기) 절에서 본 그대로다. 이런 함수는 `suspendCoroutine` 같은 다른 일시 중단 함수를 꼬리 위치에서 호출한다. 이들은 일반적인 비일시 중단 함수와 똑같이 컴파일되며, 유일한 차이는 [CPS 변환](#컨티뉴에이션-전달-스타일)으로 받은 암묵적 continuation 파라미터를 꼬리 호출에서 다음 일시 중단 함수에 넘긴다는 것뿐이다.

일시 중단 호출이 꼬리가 아닌 위치에 나타나면 컴파일러는 해당 일시 중단 함수에 대한 [상태 머신](#상태-머신)을 만든다. 상태 머신 객체의 인스턴스는 일시 중단 함수가 호출될 때 생성되고 완료되면 버려진다.

> 참고: 향후 버전에서는 이 컴파일 전략이 첫 번째 일시 중단 지점에서만 상태 머신 인스턴스를 만들도록 최적화될 수 있다.

이 상태 머신 객체는 이어서 꼬리가 아닌 위치에서 이루어지는 다른 일시 중단 함수 호출의 *완료 컨티뉴에이션* 역할을 한다. 함수가 다른 일시 중단 함수를 여러 번 호출할 때 이 상태 머신 객체 인스턴스는 갱신되며 재사용된다. 이를 다른 [비동기 프로그래밍 스타일](022-keep-kotlin-coroutines-part2.md#비동기-프로그래밍-스타일)과 비교해 보라. 다른 스타일에서는 비동기 처리의 각 후속 단계가 보통 새로 할당된 별도의 클로저 객체로 구현된다.

### 코루틴 인트린식

Kotlin 표준 라이브러리는 `kotlin.coroutines.intrinsics` 패키지를 제공한다. 이 패키지에는 이 절에서 설명하는, 코루틴 기계 장치의 내부 구현 세부 사항을 드러내는 선언들이 들어 있으며 신중하게 사용해야 한다. 이 선언들은 일반적인 코드에서 쓰면 안 되므로 `kotlin.coroutines.intrinsics` 패키지는 IDE 자동 완성에서 숨겨져 있다. 이 선언들을 쓰려면 소스 파일에 해당 import 문을 직접 추가해야 한다.

```kotlin
import kotlin.coroutines.intrinsics.*
```

표준 라이브러리의 `suspendCoroutine` 일시 중단 함수 실제 구현은 Kotlin 자체로 작성돼 있으며 그 소스 코드는 표준 라이브러리 소스 패키지의 일부로 볼 수 있다. 코루틴을 안전하고 문제없이 쓸 수 있도록, 이 함수는 코루틴이 일시 중단될 때마다 상태 머신의 실제 컨티뉴에이션을 추가 객체로 한 번 감싼다. [비동기 연산](#비동기-연산)이나 [퓨처](#퓨처) 같은 진짜 비동기 유스 케이스에서는 해당 비동기 프리미티브의 런타임 비용이 추가 객체 할당 비용을 훨씬 웃돌기 때문에 이는 전혀 문제가 되지 않는다. 그러나 [제너레이터](#제너레이터) 유스 케이스에서는 이 추가 비용이 지나치게 크므로, intrinsics 패키지가 성능에 민감한 저수준 코드를 위한 프리미티브를 제공한다.

표준 라이브러리의 `kotlin.coroutines.intrinsics` 패키지에는 다음 시그니처의 [`suspendCoroutineUninterceptedOrReturn`](http://kotlinlang.org/api/latest/jvm/stdlib/kotlin.coroutines.intrinsics/suspend-coroutine-unintercepted-or-return.html) 함수가 있다.

```kotlin
suspend fun <T> suspendCoroutineUninterceptedOrReturn(block: (Continuation<T>) -> Any?): T
```

이 함수는 일시 중단 함수의 [컨티뉴에이션 전달 스타일](#컨티뉴에이션-전달-스타일)에 대한 직접 접근을 제공하며 컨티뉴에이션의 *가로채지지 않은*(unintercepted) 참조를 노출한다. 후자는 `Continuation.resumeWith`의 호출이 [ContinuationInterceptor](#컨티뉴에이션-인터셉터)를 거치지 않는다는 뜻이다. 컨티뉴에이션 인터셉터를 설치할 수 없는(컨텍스트가 항상 비어 있으므로) [제한된 일시 중단](#제한된-일시-중단)을 가진 동기 코루틴을 작성할 때, 혹은 현재 실행 중인 스레드가 이미 원하는 컨텍스트에 있다는 것을 알고 있을 때 쓸 수 있다. 그렇지 않다면 [`intercepted`](http://kotlinlang.org/api/latest/jvm/stdlib/kotlin.coroutines.intrinsics/intercepted.html) 확장 함수(`kotlin.coroutines.intrinsics` 패키지)로 가로채진 컨티뉴에이션을 얻어야 한다.

```kotlin
fun <T> Continuation<T>.intercepted(): Continuation<T>
```

그리고 `Continuation.resumeWith`는 그 결과인 *가로채진* 컨티뉴에이션에 대해 호출해야 한다.

`suspendCoroutineUninterceptedOrReturn` 함수에 전달된 `block`은 코루틴이 실제로 일시 중단됐다면 [`COROUTINE_SUSPENDED`](http://kotlinlang.org/api/latest/jvm/stdlib/kotlin.coroutines.intrinsics/-c-o-r-o-u-t-i-n-e_-s-u-s-p-e-n-d-e-d.html) 마커를 반환할 수 있고(이 경우 `Continuation.resumeWith`가 나중에 정확히 한 번 호출돼야 한다), 결과 값 `T`를 반환하거나 예외를 던질 수도 있다(뒤의 두 경우에는 `Continuation.resumeWith`가 절대 호출되면 안 된다).

`suspendCoroutineUninterceptedOrReturn`을 쓰면서 이 관례를 지키지 않으면, 테스트로 찾고 재현하려는 시도를 무력화하는, 추적하기 힘든 버그가 생긴다. 이 관례는 `buildSequence`/`yield` 류의 코루틴에서는 대체로 지키기 쉽지만, `suspendCoroutineUninterceptedOrReturn` 위에 비동기 `await` 류의 일시 중단 함수를 작성하려는 시도는 **권장하지 않는다**. `suspendCoroutine`의 도움 없이 올바르게 구현하기가 **극도로 까다롭기** 때문이다.

다음 시그니처의 [`createCoroutineUnintercepted`](http://kotlinlang.org/api/latest/jvm/stdlib/kotlin.coroutines.intrinsics/create-coroutine-unintercepted.html) 함수들도 있다(`kotlin.coroutines.intrinsics` 패키지).

```kotlin
fun <T> (suspend () -> T).createCoroutineUnintercepted(completion: Continuation<T>): Continuation<Unit>
fun <R, T> (suspend R.() -> T).createCoroutineUnintercepted(receiver: R, completion: Continuation<T>): Continuation<Unit>
```

이들은 `createCoroutine`과 비슷하게 동작하지만 초기 컨티뉴에이션의 가로채지지 않은 참조를 반환한다. `suspendCoroutineUninterceptedOrReturn`과 마찬가지로 동기 코루틴에서 더 나은 성능을 위해 쓸 수 있다. 예를 들어 `createCoroutineUnintercepted`를 이용한 `sequence{}` 빌더의 최적화 버전은 아래와 같다.

```kotlin
fun <T> sequence(block: suspend SequenceScope<T>.() -> Unit): Sequence<T> = Sequence {
    SequenceCoroutine<T>().apply {
        nextStep = block.createCoroutineUnintercepted(receiver = this, completion = this)
    }
}
```

`suspendCoroutineUninterceptedOrReturn`을 이용한 `yield`의 최적화 버전은 아래와 같다. `yield`는 항상 일시 중단하므로 해당 block이 언제나 `COROUTINE_SUSPENDED`를 반환한다는 점에 유의하라.

```kotlin
// 제너레이터 구현
override suspend fun yield(value: T) {
    setNext(value)
    return suspendCoroutineUninterceptedOrReturn { cont ->
        nextStep = cont
        COROUTINE_SUSPENDED
    }
}
```

> 전체 코드는 [여기](https://github.com/kotlin/kotlin-coroutines-examples/tree/master/examples/sequence/optimized/sequenceOptimized.kt)에서 볼 수 있다.

`startCoroutine`([코루틴 빌더](#코루틴-빌더) 절 참조)의 더 저수준 버전을 제공하는 두 개의 인트린식이 추가로 있으며, [`startCoroutineUninterceptedOrReturn`](http://kotlinlang.org/api/latest/jvm/stdlib/kotlin.coroutines.intrinsics/start-coroutine-unintercepted-or-return.html)이라는 이름이다.

```kotlin
fun <T> (suspend () -> T).startCoroutineUninterceptedOrReturn(completion: Continuation<T>): Any?
fun <R, T> (suspend R.() -> T).startCoroutineUninterceptedOrReturn(receiver: R, completion: Continuation<T>): Any?
```

이들은 `startCoroutine`과 두 가지 면에서 다르다. 첫째, 코루틴을 시작할 때 [ContinuationInterceptor](#컨티뉴에이션-인터셉터)가 자동으로 적용되지 않으므로 필요하다면 호출자가 적절한 실행 컨텍스트를 보장해야 한다. 둘째, 코루틴이 일시 중단하지 않고 값을 반환하거나 예외를 던지면 `startCoroutineUninterceptedOrReturn` 호출이 그 값을 반환하거나 그 예외를 던진다. 코루틴이 일시 중단하면 `COROUTINE_SUSPENDED`를 반환한다.

`startCoroutineUninterceptedOrReturn`의 주된 유스 케이스는 `suspendCoroutineUninterceptedOrReturn`과 결합해, 일시 중단된 코루틴을 같은 컨텍스트에서 다른 코드 블록으로 계속 실행하는 것이다.

```kotlin
suspend fun doSomething() = suspendCoroutineUninterceptedOrReturn { cont ->
    // 실행해야 할 코드 블록을 알아내거나 만든다
    startCoroutineUninterceptedOrReturn(completion = block) // 결과를 suspendCoroutineUninterceptedOrReturn에 반환한다
}
```

**계속: [Part 2 — 부록과 리비전 이력](022-keep-kotlin-coroutines-part2.md)**
