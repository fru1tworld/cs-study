# 구조적 동시성 (정리 노트)

> 원문: [Structured Concurrency](https://elizarov.medium.com/structured-concurrency-722d765aa952)
> 저자: Roman Elizarov | 정리일: 2026-07-10
>
> 저작권이 있는 글이므로 전문 번역 대신 섹션 구조를 따라 내용을 재서술했다. 원문 표현 그대로가 필요하면 원문을 참고할 것.

## 배경: kotlinx.coroutines 0.26.0이라는 분기점

이 글은 kotlinx.coroutines 0.26.0 릴리스에 맞춰 쓰였다. Elizarov는 이 릴리스가 단순한 기능 추가가 아니라 라이브러리의 이념 자체를 바꾼 전환점이라고 선언한다.

그전까지 코루틴을 소개하는 표준 화법은 "가벼운 스레드"였다. 스레드와 개념적으로 같지만 생성 비용이 훨씬 싸서 필요할 때마다 마음껏 만들어도 된다는 것이다. 이 비유의 자연스러운 귀결로, 코루틴도 스레드처럼 아무 데서나 전역으로 띄우는 것이 기본 사용법이었다(`launch { ... }`가 곧 전역 실행이었다).

문제는 이 자유가 전역 변수의 자유와 같은 종류라는 것이다. 전역으로 띄운 코루틴은:

- 누가 소유하는지 코드에 드러나지 않고,
- 소유자가 사라져도(화면이 닫혀도) 계속 실행되며,
- 실패했을 때 예외를 보고할 대상이 불분명하고,
- 완료를 기다려 줄 주체가 없다.

0.26.0부터는 이것을 뒤집어, **모든 코루틴은 어떤 `CoroutineScope` 안에서 실행되어야 한다**를 기본 규칙으로 삼는다. 이것이 구조적 동시성(structured concurrency)이다. 글은 이 전환을 두 가지 대표 사용 사례로 정당화한다.

## 사례 1: 비동기 작업 (Asynchronous Operations)

첫 번째 사례는 UI 애플리케이션에서 백엔드에 데이터를 요청하고 결과로 화면을 갱신하는, 가장 흔한 패턴이다. 코루틴이 싸다는 점 때문에 이런 작업마다 코루틴을 하나씩 띄우는 것이 자연스러운데, 백엔드 요청은 얼마나 걸릴지 예측할 수 없고 사용자가 그 사이 화면을 닫아버릴 수 있다. 그러면 진행 중인 요청은 취소되어야 한다.

과거의 권장 코드는 이랬다.

```kotlin
fun requestSomeData() {
    launch(UI) {
        updateUI(performRequest())
    }
}
```

이 코드의 문제:

- **취소가 자동으로 안 된다.** 화면이 닫혀도 이 코루틴은 계속 돈다. 제대로 하려면 `Job`을 만들어 필드에 보관하고, `launch(UI + job)`처럼 컨텍스트에 합성하고, 화면이 닫힐 때 `job.cancel()`을 호출하는 보일러플레이트를 매번 반복해야 했다. 전부 개발자의 기억력에 의존하며, 하나라도 빼먹으면 조용히 누수가 생긴다.
- **컨텍스트 지정도 실수하기 쉽다.** `UI`를 빼먹으면 엉뚱한 스레드에서 UI를 만진다.

요컨대 "가장 쓰기 쉬운 코드"(그냥 `launch`)와 "올바른 코드"(Job 관리 포함)가 서로 달랐다. API 설계에서 이것은 치명적이다. 사람들은 결국 쉬운 쪽을 쓴다.

새 모델의 해법은 UI 클래스(액티비티, 프래그먼트, 뷰모델처럼 수명이 유한한 엔티티)가 `CoroutineScope` 인터페이스를 구현하는 것이다. 스코프 구현부에서 UI 디스패처와 `Job`을 한 번만 지정해 두고, 엔티티의 수명이 끝날 때 그 Job을 취소한다. 그러면 클래스 안 어디서든 다음처럼 쓸 수 있다.

```kotlin
fun requestSomeData() {
    launch {
        updateUI(performRequest())
    }
}
```

이 `launch`는 이제 전역 함수가 아니라 `CoroutineScope`의 확장 함수다. 스코프의 컨텍스트(UI 디스패처)와 Job(취소 연결)을 자동으로 물려받는다. 즉 **가장 짧고 쉬운 코드가 곧 올바른 코드**가 된다. 반대로 스코프 밖에서는 `launch`를 호출할 수 없으므로, 잘못된 코드는 아예 작성하기 어려워진다.

## 사례 2: 병렬 분해 (Parallel Decomposition)

두 번째 사례는 하나의 큰 작업을 여러 하위 작업으로 쪼개 동시에 수행하는 것이다. 예: 이미지 두 장을 동시에 로드해서 합치기.

과거에 문서가 권장하던 패턴은 이랬다.

```kotlin
suspend fun loadAndCombine(name1: String, name2: String): Image {
    val deferred1 = async { loadImage(name1) }
    val deferred2 = async { loadImage(name2) }
    return combineImages(deferred1.await(), deferred2.await())
}
```

겉보기에 멀쩡하지만 미묘한 결함이 두 가지 있다.

1. **부모 취소가 자식에게 전파되지 않는다.** 이 함수를 호출한 상위 작업이 취소되어도, 전역으로 띄운 두 `async`는 계속 이미지를 내려받는다. 결과를 받을 사람이 없는데도.
2. **형제 실패가 정리되지 않는다.** 첫 번째 이미지 로드가 실패해 `await`에서 예외가 던져져도, 두 번째 `async`는 멈추지 않고 끝까지 실행된다. 자원 낭비이고, 심하면 누수다.

구조적 동시성의 해법은 `coroutineScope { ... }` 빌더로 경계를 치는 것이다.

```kotlin
suspend fun loadAndCombine(name1: String, name2: String): Image =
    coroutineScope {
        val deferred1 = async { loadImage(name1) }
        val deferred2 = async { loadImage(name2) }
        combineImages(deferred1.await(), deferred2.await())
    }
```

블록 안의 `async`들은 이 스코프의 자식이 된다. 그 결과:

- 이 함수를 감싼 상위 작업이 취소되면 두 로드도 함께 취소된다.
- 한쪽 로드가 실패하면 다른 쪽도 자동으로 취소되고, 예외는 호출자에게 전파된다.
- `coroutineScope` 블록은 모든 자식이 끝나기 전에는 반환하지 않는다. 즉 "이 함수가 반환했다면 이 함수가 벌인 일은 전부 끝났다"가 보장된다.

동시 실행의 수명이 코드의 블록 구조와 일치하게 되는 것, 이것이 "구조적"이라는 말의 의미다.

## 탈출구: GlobalScope

정말로 애플리케이션 프로세스 전체와 수명을 같이해야 하는 코루틴(예: 앱 전역 이벤트 처리)을 위해 `GlobalScope` 객체가 남아 있다. `GlobalScope.launch { ... }`라고 써야 하므로, 전역 변수를 쓸 때처럼 "지금 전역 수명의 무언가를 만들고 있다"는 사실이 코드에 명시적으로 드러난다. 실수로 쓰는 것이 아니라 의식적으로 선택하게 만드는 장치다.

## 더 읽을거리 (Further Reading)

Elizarov는 이 아이디어의 철학적 근거로 Nathaniel J. Smith의 "[Notes on structured concurrency, or: Go statement considered harmful](https://vorpus.org/blog/notes-on-structured-concurrency-or-go-statement-considered-harmful/)"을 소개한다. 그 글은 `go`문(그리고 `launch`, `spawn`, 스레드 생성 일반)을 goto에 비유한다. goto가 제어 흐름을 코드의 블록 구조 밖으로 탈출시켜 구조적 프로그래밍이 그것을 추방했듯, 무제한 동시 실행 시작은 실행 수명을 코드 구조 밖으로 탈출시키므로 같은 방식으로 길들여야 한다는 논지다. 구조적 동시성은 goto 없는 프로그래밍에 대응하는, "전역 launch 없는 동시성"이다.

## 같이 보면 좋은 글

- 후속: [Blocking threads, suspending coroutines](https://elizarov.medium.com/blocking-threads-suspending-coroutines-d33e11bf4761), [Explicit concurrency](https://elizarov.medium.com/explicit-concurrency-67a8e8fd9b25)
