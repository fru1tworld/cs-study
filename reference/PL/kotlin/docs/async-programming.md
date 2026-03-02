# 비동기 프로그래밍 기법

## 개요

이 문서는 데스크톱, 모바일 및 서버 측 애플리케이션을 포함하여 애플리케이션의 차단을 방지하기 위한 다양한 접근 방식을 다룹니다.

## 비동기 프로그래밍의 주요 접근 방식

### 1. 스레딩
**정의:** 메인 스레드 차단을 피하기 위해 별도의 스레드를 사용합니다.

**예제:**
```kotlin
fun postItem(item: Item) {
    val token = preparePost()
    val post = submitPost(token, item)
    processPost(post)
}

fun preparePost(): Token {
    // 요청을 만들고 결과적으로 메인 스레드를 차단
    return token
}
```

**단점:**
- 스레드는 비용이 큼 (컨텍스트 전환 비용이 높음)
- 스레드 수가 제한됨 (OS 의존적 병목)
- 모든 플랫폼에서 사용 불가 (예: JavaScript)
- 디버깅이 어렵고 경쟁 조건에 취약함

### 2. 콜백
**정의:** 완료 후 호출될 함수를 매개변수로 전달합니다.

**예제:**
```kotlin
fun postItem(item: Item) {
    preparePostAsync { token ->
        submitPostAsync(token, item) { post ->
            processPost(post)
        }
    }
}

fun preparePostAsync(callback: (Token) -> Unit) {
    // 요청을 만들고 즉시 반환
    // 나중에 콜백이 호출되도록 준비
}
```

**단점:**
- 콜백 지옥 / 피라미드 오브 둠 (깊게 중첩된 콜백)
- 복잡한 오류 처리

### 3. Futures, Promises 등
**정의:** 호출은 어느 시점에 해결될 `Promise` 객체를 반환합니다.

**예제:**
```kotlin
fun postItem(item: Item) {
    preparePostAsync()
        .thenCompose { token ->
            submitPostAsync(token, item)
        }
        .thenAccept { post ->
            processPost(post)
        }
}

fun preparePostAsync(): Promise<Token> {
    // 요청을 만들고 나중에 완료될 promise를 반환
    return promise
}
```

**단점:**
- 다른 프로그래밍 모델 (합성적 vs 명령형)
- 새로운 API를 배워야 함 (`thenCompose`, `thenAccept`)
- 반환 타입이 실제 데이터에서 `Promise`로 변경됨
- 오류 처리가 복잡할 수 있음

### 4. Reactive Extensions (Rx)
**정의:** Erik Meijer가 C#용으로 도입하고 Netflix의 RxJava로 인기를 얻음. 관찰 가능한 스트림을 사용하여 데이터를 처리합니다.

**핵심 개념:** "모든 것이 스트림이고, 관찰 가능하다"

**특징:**
- 개별 요소가 아닌 스트림을 반환
- 플랫폼 전반에 걸쳐 일관된 API (C#, Java, JavaScript 등)
- Futures보다 나은 오류 처리
- 프로그래밍 모델의 상당한 변화 필요

### 5. 코루틴 (Kotlin의 접근 방식)
**정의:** 함수가 실행을 일시 중단하고 나중에 재개할 수 있는 일시 중단 가능한 계산.

**예제:**
```kotlin
fun postItem(item: Item) {
    launch {
        val token = preparePost()
        val post = submitPost(token, item)
        processPost(post)
    }
}

suspend fun preparePost(): Token {
    // 요청을 만들고 코루틴을 일시 중단
    return suspendCoroutine { /* ... */ }
}
```

**장점:**
- 논블로킹임에도 불구하고 코드가 동기적으로 보임 (위에서 아래로)
- 함수 시그니처가 동일하게 유지됨 (`suspend` 키워드만 추가)
- 표준 프로그래밍 구문 사용 (루프, 예외 처리)
- 플랫폼 독립적 (JVM, JavaScript 등)
- 대부분의 기능이 라이브러리에 위임됨 (언어에는 `suspend` 키워드만)
- 구현이 플랫폼 전반에 걸쳐 컴파일러에 의해 처리됨

**핵심 이점:** 논블로킹 코드를 작성하는 것이 본질적으로 블로킹 코드를 작성하는 것과 같습니다.

## 역사적 맥락

코루틴은 새로운 것이 아니며 수십 년 동안 존재해왔고 Go와 같은 언어에서 인기가 있습니다. Kotlin의 구현은 C#과 달리 `async`와 `await` 같은 언어 키워드를 피하고 라이브러리 함수를 선호합니다.

## 관련 문서
- [코루틴 참조](coroutines-overview.md)
- [제네릭: in, out, where](generics.md)
