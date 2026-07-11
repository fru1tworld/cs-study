# 스레드 블로킹과 코루틴 일시 중단 (정리 노트)

> 원문: [Blocking threads, suspending coroutines](https://elizarov.medium.com/blocking-threads-suspending-coroutines-d33e11bf4761)
> 저자: Roman Elizarov | 정리일: 2026-07-10
>
> 저작권이 있는 글이므로 전문 번역 대신 섹션 구조를 따라 내용을 재서술했다. 원문 표현 그대로가 필요하면 원문을 참고할 것.

## 스레드 블로킹 (Blocking threads)

스레드는 동시 실행을 위한 추상화이고 멀티스레딩이 있는데도 왜 블로킹이 문제인가에서 글이 시작한다. 블로킹에는 두 부류가 있다.

**CPU 바운드.** 예를 들어 4096비트 소수를 생성하는 함수는 최신 하드웨어에서도 10초 안팎이 걸린다.

```kotlin
fun findBigPrime(): BigInteger =
    BigInteger.probablePrime(4096, Random())
```

이 함수를 호출한 스레드는 계산이 끝날 때까지 다른 일을 전혀 못 한다. 계산이 실제로 CPU를 쓰고 있으니 어쩔 수 없는 면이 있지만, 어느 스레드가 묶이느냐는 선택의 문제다.

**I/O 바운드.** 원격에서 메시지를 읽어 파싱하는 함수를 보자.

```kotlin
fun BufferedReader.readMessage(): Message? =
    readLine()?.parseMessage()
```

`readLine()`은 데이터가 도착할 때까지 스레드를 붙잡아 둔다. 이때 스레드는 CPU를 쓰지도 않으면서 그냥 놀며 기다린다. 그런데도 이 스레드로는 다른 어떤 작업도 처리할 수 없다.

두 경우 모두 실제 피해는 같다. UI 앱에서 메인 스레드가 블로킹되면 화면 전체가 멈추고, 고정 크기 스레드 풀로 요청을 처리하는 백엔드에서는 요청들이 느린 외부 서비스를 기다리며 풀을 다 점유해 버리면 새 요청을 받을 스레드가 없다(원문은 이 포화 상태를 교통 정체 사진으로 비유한다). 결론: 스레드 블로킹은 피해야 할 대상이며, 피할 수 없다면 최소한 어느 스레드를 블로킹할지는 통제해야 한다.

## 코루틴 일시 중단 (Suspending coroutines)

코루틴은 블로킹의 대안으로 **일시 중단**(suspension)을 제공한다. 그런데 호출하는 쪽 코드만 보면 둘은 구별되지 않는다.

```kotlin
val data = awaitData() // 블로킹인가, 일시 중단인가?
processData(data)
```

어느 쪽이든 `awaitData()`가 끝난 뒤에 `processData(data)`가 실행된다. 실행 순서 관점에서는 완전히 같다. 그렇다면 구별이 무의미한가? 전혀 아니다. 시스템 전체의 거동이 근본적으로 다르다.

- `awaitData()`가 스레드를 **블로킹**하면: 그 스레드는 죽은 자원이 된다. 메인 스레드였다면 UI가 얼어붙는다.
- `awaitData()`가 코루틴을 **일시 중단**하면: 코루틴만 멈추고 스레드는 즉시 풀려나 다른 코루틴(다른 UI 이벤트, 다른 요청)을 실행한다. 데이터가 준비되면 코루틴이 재개된다.

즉 코드 모양은 순차적 그대로 두면서, 스레드라는 희소 자원을 묶지 않는 것이 일시 중단의 가치다.

## 블로킹 코드 알아보기 (Recognizing blocking code)

문제는 어떤 호출이 블로킹인지 알아보기 어렵다는 것이다. Java API 문서는 블로킹 여부를 제각각으로 표기한다.

- `InputStream.read`: "input data가 있을 때까지 block한다"고 명시적으로 적혀 있다. 친절한 경우.
- `ReentrantLock.lock`: "락을 얻을 때까지 현재 스레드는 스레드 스케줄링 목적상 비활성화된다" 같은 간접적 표현을 쓴다. 블로킹이라는 단어가 없다.
- `BigInteger.probablePrime`: 몇 초씩 CPU를 점유하는데도 문서에 아무 힌트가 없다.

게다가 JVM 문서에서 "blocking"이라는 용어는 주로 I/O 대기라는 좁은 의미로 쓰이지만, 실용적으로는 "스레드가 다른 일을 못 하게 만드는 모든 것"(장시간 CPU 계산 포함)을 블로킹으로 봐야 한다. 결국 큰 애플리케이션에서 부적절한 블로킹 호출을 찾는 일은 린트 검사와 런타임 예외(안드로이드의 `NetworkOnMainThreadException` 같은 장치)를 조합한 탐정 작업이 된다.

## suspend 함수 (Suspending functions)

흔한 오해를 짚는다. `suspend` 키워드를 붙이면 함수가 논블로킹·비동기가 된다는 오해다.

```kotlin
suspend fun findBigPrime(): BigInteger =
    BigInteger.probablePrime(4096, Random())
```

이 함수는 `suspend`가 붙었지만 여전히 호출자 스레드를 몇 초씩 점유한다. `suspend`는 함수에게 "일시 중단할 수 있는 능력"을 줄 뿐, 내부의 블로킹 코드를 없애 주지 않는다. 실제로 IntelliJ IDEA는 이 코드에 "redundant 'suspend' modifier"(불필요한 suspend 한정자) 경고를 띄운다. 일시 중단하는 데가 없기 때문이다.

그렇다면 "코루틴은 메인 스레드에서 띄워도 안전하다"는 통념은 어디서 오는가? `suspend` 키워드의 마법이 아니라, 생태계가 지키는 **관례** 덕분이라는 것이 다음 섹션의 요지다.

## 일시 중단 관례 (Suspending convention)

kotlinx.coroutines의 모든 API와 잘 설계된 라이브러리들이 따르는 관례:

> **suspend 함수는 호출자 스레드를 블로킹하지 않는다.**

시간이 오래 걸리는 일은 일시 중단으로 처리하고, 스레드는 항상 빨리 돌려준다는 약속이다. 블로킹 코드를 이 관례에 맞게 감싸는 도구가 `withContext`다. CPU 바운드 작업이라면 `Dispatchers.Default`로 보낸다.

```kotlin
suspend fun findBigPrime(): BigInteger =
    withContext(Dispatchers.Default) {
        BigInteger.probablePrime(4096, Random())
    }
```

이제 이 함수는 호출자 스레드를 즉시 돌려주고, 계산은 Default 디스패처의 스레드 풀에서 진행되며, 끝나면 코루틴이 재개된다. `Dispatchers.Default`의 풀 크기는 CPU 코어 수에 맞춰져 있다. CPU 바운드 작업은 코어 수보다 많은 스레드를 만들어 봐야 빨라지지 않으므로, 물리 자원을 딱 포화시키는 크기가 최적이다.

이 관례가 지켜지면 메인 스레드에서 코루틴을 띄우고 그 안에서 어떤 suspend 함수를 호출해도 UI가 멈추지 않는다.

## 블로킹 I/O를 일시 중단으로 (Blocking IO to suspending)

I/O 바운드 블로킹은 CPU 바운드와 다르게 다뤄야 한다. I/O를 기다리는 스레드는 CPU를 안 쓰고 놀고 있을 뿐이므로, 코어 수만큼의 스레드로는 오히려 부족하다. 8코어 머신에서 Default 풀의 스레드 8개가 전부 네트워크 응답을 기다리며 블로킹되면, CPU는 완전히 놀면서도 아무 일도 진행되지 않는 상황이 된다.

그래서 블로킹 I/O 전용으로 `Dispatchers.IO`가 있다. 이 디스패처는 스레드가 I/O 대기로 묶이는 것을 전제로, 필요하면 코어 수 이상으로 스레드를 추가 생성한다.

```kotlin
suspend fun BufferedReader.readMessage(): Message? =
    withContext(Dispatchers.IO) {
        readLine()?.parseMessage()
    }
```

이렇게 감싸면 블로킹 I/O API를 그대로 쓰면서도 호출자 관점에서는 관례를 지키는 suspend 함수가 된다.

## 결론 (Conclusion)

`withContext`로 감싸는 것이 유일한 방법은 아니다. 처음부터 비동기로 설계된 라이브러리를 쓰면 블로킹 자체가 없다. 예컨대 `Thread.sleep` 대신 `delay`를 쓰면 스레드를 묶지 않고 기다린다.

핵심은 관례의 힘이다. 모든 suspend 함수가 "호출자 스레드를 블로킹하지 않는다"를 지키면, 개발자는 어떤 suspend 함수든 문서를 뒤져 보지 않고도 아무 데서나(메인 스레드 포함) 안심하고 호출할 수 있다. 인지 부담이 줄고, UI 앱에서는 화면 멈춤이 사라지며, 백엔드에서는 적은 스레드로 높은 처리량을 낸다. 블로킹 코드는 suspend 함수 안에 격리하고, 나머지 세상은 논블로킹으로 유지하라는 것이 이 글의 처방이다.

## 같이 보면 좋은 글

- 전편: [Structured Concurrency](https://elizarov.medium.com/structured-concurrency-722d765aa952)
- 후속: [Explicit concurrency](https://elizarov.medium.com/explicit-concurrency-67a8e8fd9b25)
