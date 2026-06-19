# Akka Streams 고급과 연동

> 이 문서는 Akka 공식 문서의 "Streams" 고급/연동 섹션을 한국어로 번역한 것입니다.
> 원본: https://doc.akka.io/libraries/akka-core/current/stream/index.html

---

## 목차

1. [동적 스트림 처리(Dynamic Stream Handling)](#1-동적-스트림-처리dynamic-stream-handling)
2. [커스텀 스트림 처리(Custom Stream Processing, GraphStage)](#2-커스텀-스트림-처리custom-stream-processing-graphstage)
3. [스트림 에러 처리(Error Handling)](#3-스트림-에러-처리error-handling)
4. [스트리밍 IO 다루기(Working with Streaming IO)](#4-스트리밍-io-다루기working-with-streaming-io)
5. [액터와의 연동(Integration with Actors)](#5-액터와의-연동integration-with-actors)
6. [Reactive Streams 상호운용성(Reactive Streams Interoperability)](#6-reactive-streams-상호운용성reactive-streams-interoperability)
7. [스트림 테스트하기(Testing Streams)](#7-스트림-테스트하기testing-streams)
8. [연산자 색인(Operators Index)](#8-연산자-색인operators-index)
9. [참고 자료](#참고-자료)

---

## 1. 동적 스트림 처리(Dynamic Stream Handling)

동적 스트림 처리(dynamic stream handling)는 그래프 연결을 사전에 정해 두지 않고도, 실행 시점(runtime)에 스트림의 완료(completion)와 라우팅(routing)을 제어할 수 있게 해 줍니다. 이 섹션은 스트림을 외부에서 종료시키는 킬 스위치(KillSwitch) 메커니즘과, 실행 중에 동적으로 팬-인(fan-in)/팬-아웃(fan-out)을 구성할 수 있는 허브(Hub) 구현을 다룹니다.

### 1.1 KillSwitch: 스트림 완료 제어

킬 스위치(KillSwitch)는 연산자(operator)의 완료를 **외부에서** 제어할 수 있게 해 주는 도구입니다. 공식 문서의 표현을 빌리면, KillSwitch는 "외부에서 `FlowShape` 연산자의 완료를 가능하게(allows the completion of operators of FlowShape from the outside)" 합니다. 이 인터페이스는 두 가지 주요 동작을 제공합니다.

- **`shutdown()`**: 스트림을 정상적으로 완료시킵니다. 업스트림(upstream)을 취소(cancel)하고 다운스트림(downstream)을 완료(complete)합니다.
- **`abort(Throwable)`**: 스트림을 실패(fail)시킵니다. 업스트림을 취소하고 다운스트림을 지정된 예외로 실패시킵니다.

두 메서드 중 하나가 처음 호출된 이후의 후속 호출은 모두 무시됩니다.

#### UniqueKillSwitch

`UniqueKillSwitch`는 정확히 하나의 머티리얼라이즈된 그래프 인스턴스(materialized graph instance)를 제어합니다. 이는 머티리얼라이제이션(materialization)의 결과로 획득되며, 개별 스트림을 독립적으로 관리할 수 있게 해 줍니다.

- **셧다운(shutdown) 예시**: 지연(delay)을 두고 카운트하는 소스에 적용했을 때 `shutdown()`을 호출하면, 스트림이 우아하게(graceful) 종료되며 마지막으로 방출(emit)한 요소가 반환됩니다.
- **어보트(abort) 예시**: `abort(error)`를 사용하면 지정된 예외로 스트림이 실패하게 되어, 다운스트림에서 에러 처리(error handling)를 수행할 수 있습니다.

#### SharedKillSwitch

`SharedKillSwitch`는 여러 스트림 인스턴스를 동시에 관리합니다. `UniqueKillSwitch`와 달리, 머티리얼라이제이션 이전에 `KillSwitches.shared(name)`를 통해 미리 생성되어야 합니다.

- **여러 스트림 제어**: 단일 `SharedKillSwitch`는 여러 개의 독립적인 머티리얼라이제이션을 함께 통제할 수 있어서, 서로 관련된 스트림들을 한꺼번에 조정(coordinated shutdown)하여 종료할 때 유용합니다.

### 1.2 허브 구현: 동적 팬-인과 팬-아웃

#### MergeHub (동적 팬-인)

`MergeHub`는 동적 팬-인(dynamic fan-in)을 구현합니다. 여러 생산자(producer) 소스를 하나의 소비자(consumer)로 결합합니다. 주요 특성은 다음과 같습니다.

- 여러 생산자가 선착순(first-come-first-served) 방식으로 하나의 소비자에게 데이터를 공급합니다.
- 소비자가 처리 속도를 따라가지 못하면 모든 생산자가 백프레셔(backpressure)를 받습니다.
- 소비자에 연결된 후, 허브는 `Sink`로 머티리얼라이즈됩니다.

작업 흐름은 다음과 같습니다. 먼저 소스-소비자 그래프를 머티리얼라이즈하여 `Sink`를 얻고, 그 다음 그 `Sink`를 서로 다른 생산자들과 반복적으로 연결하여 사용합니다.

#### BroadcastHub (동적 팬-아웃)

`BroadcastHub`는 단일 생산자에서 여러 소비자로의 동적 팬-아웃(dynamic fan-out)을 가능하게 합니다.

- 하나의 생산자가 여러 구독자(subscriber)에게 요소를 공급합니다.
- 생산자의 속도는 가장 느린 소비자(slowest consumer)에 맞추어 조정됩니다.
- 허브는 생산자가 연결되는 `Sink`로 동작합니다.
- 각각의 `Source` 머티리얼라이제이션이 새로운 소비자를 추가합니다.

> **동작 주의**: 활성 구독자가 하나도 없으면, 허브는 (버퍼 전략으로 수정하지 않는 한) 요소를 버리지 않고 업스트림에 백프레셔를 겁니다.

#### 허브를 결합한 발행-구독(Publish-Subscribe)

`MergeHub`와 `BroadcastHub`를 연결하면 발행-구독(publish-subscribe) 채널을 만드는 실용적인 패턴을 구성할 수 있습니다.

1. `MergeHub`의 소스를 `BroadcastHub`의 싱크에 연결하고 함께 머티리얼라이즈합니다.
2. 구독자가 없을 때 백프레셔를 방지하기 위해 `Sink.ignore()`를 브로드캐스트 출력에 연결합니다.
3. 양 끝점을 `Flow.fromSinkAndSource()`를 사용해 하나의 `Flow`로 감쌉니다.
4. 외부에서 취소할 수 있도록 `KillSwitch`를 추가합니다.
5. `backpressureTimeout()`을 사용해 응답하지 않는 느린 구독자를 제거합니다.

결과로 만들어진 `Flow`는 여러 생산자와 소비자를 연결받을 수 있으면서도 단일 제어 지점(single control point)을 유지합니다.

#### PartitionHub

`PartitionHub`는 분할 함수(partitioning function)를 기반으로 단일 생산자에서 여러 소비자로 요소를 라우팅합니다.

- 각 요소는 (`BroadcastHub`와 달리) 정확히 하나의 소비자에게만 라우팅됩니다.
- 생산자의 속도는 가장 느린 소비자에 맞추어 조정됩니다.
- 인덱스를 기준으로 소비자를 선택하는 함수가 필요합니다.

분할 방식에는 두 가지가 있습니다.

- **무상태(stateless) 분할**: 단순한 함수가 소비자 수(consumer count)와 요소(element)를 받아, 선택할 소비자의 인덱스를 반환합니다.
- **유상태(stateful) 분할**: 팩토리 함수(factory function)가 각 머티리얼라이제이션마다 상태 보관자(state holder)를 생성하여, 라운드 로빈(round-robin)이나 큐 크기를 고려한 라우팅(queue-size-aware routing)을 가능하게 합니다.

> **고급 라우팅 예시**: `ConsumerInfo`의 `queueSize()` 접근자는 대략적인 버퍼 수준(approximate buffer level)을 알려 주어, 버퍼에 쌓인 요소가 더 적은(더 빠른) 소비자 쪽으로 라우팅할 수 있게 합니다.

### 1.3 구현 패턴

- **여러 스트림 제어**: `SharedKillSwitch`는 단일 스위치로 여러 독립 스트림 인스턴스를 함께 제어하는 방식을 보여 주며, 관련 작업들의 생명주기(lifecycle) 관리에 유용합니다.
- **부하 분산(Load Balancing)**: 유상태 라우팅을 사용하는 `PartitionHub`는, 무작위 선택 대신 큐 깊이(queue depth)나 라운드 로빈 순서를 기준으로 지능적인 분배를 수행할 수 있게 합니다.
- **버퍼 관리(Buffer Management)**: 허브는 백프레셔를 전략적으로 처리합니다. `MergeHub`는 모든 생산자에게 집합적으로 백프레셔를 걸고, `BroadcastHub`는 가장 느린 구독자에 맞추어 조정합니다. 이를 통해 요소 손실을 방지하면서도 응답성을 유지합니다.

> 이 기능을 사용하려면 `"com.typesafe.akka" %% "akka-stream"` 의존성이 필요합니다.

---

## 2. 커스텀 스트림 처리(Custom Stream Processing, GraphStage)

`GraphStage` 추상화는 임의의 입력/출력 포트(port)를 갖는 커스텀 연산자(custom operator)를 만들 수 있게 해 줍니다. 다만 공식 문서가 강조하듯이, "커스텀 연산자가 가장 먼저 손을 뻗어야 할 도구는 아니며(A custom operator should not be the first tool you reach for), 플로우(flow)와 그래프 DSL을 사용해 연산자를 정의하는 것이 일반적으로 더 쉽습니다."

### 2.1 핵심 개념

#### GraphStage 구조

`GraphStage`는 그 형태(shape)를 통해 연산자의 인터페이스를 정의하며, `Inlet`과 `Outlet` 객체로 포트를 지정합니다. 실제 실행 로직은 `GraphStageLogic` 안에 들어가며, 이는 머티리얼라이제이션마다 `createLogic()` 메서드를 통해 생성됩니다.

핵심 원칙:

> 모든 상태는 반드시 `GraphStageLogic` 안에 있어야 하며, 절대 둘러싼 `GraphStage` 안에 두면 안 됩니다(All state MUST be inside the GraphStageLogic, never inside the enclosing GraphStage). 이 상태는 모든 콜백(callback)에서 안전하게 접근하고 수정할 수 있습니다.

#### 포트 메커니즘

**출력 포트(Output Port)**는 세 가지 연산을 지원합니다.

- `push(out, elem)`: 다운스트림이 풀(pull)했을 때 요소를 전송합니다.
- `complete(out)`: 포트를 정상적으로 닫습니다.
- `fail(out, exception)`: 실패와 함께 포트를 닫습니다.

**입력 포트(Input Port)**는 다음을 가능하게 합니다.

- `pull(in)`: 업스트림에 요소를 요청합니다.
- `grab(in)`: 푸시(push)된 요소를 획득합니다.
- `cancel(in)`: 포트를 닫습니다.

`isAvailable()`, `hasBeenPulled()`, `isClosed()` 같은 질의(query) 메서드로 포트의 상태를 확인할 수 있습니다.

#### 핸들러 프레임워크(Handler Framework)

**OutHandler** (출력 포트용)는 다음을 제공합니다.

- `onPull()`: 다운스트림이 요소를 요청할 때 호출됩니다.
- `onDownstreamFinish()`: 다운스트림이 취소할 때 호출됩니다.

**InHandler** (입력 포트용)는 다음을 제공합니다.

- `onPush()`: 업스트림이 요소를 전송할 때 트리거됩니다.
- `onUpstreamFinish()`: 업스트림이 완료될 때 호출됩니다.
- `onUpstreamFailure()`: 업스트림이 실패할 때 호출됩니다.

### 2.2 커스텀 선형 연산자(Custom Linear Operators)

선형 연산자는 하나의 입력과 하나의 출력을 갖는 `GraphStage[FlowShape[A, B]]`를 상속합니다.

- **Map 연산자 예시**: 단순한 변환으로, 수요(demand)를 업스트림으로 전달하고 요소를 다운스트림으로 전달합니다. `Map` 구현은 양방향 신호 흐름을 보여 줍니다. `onPull()`은 `pull(in)`을 호출하고, `onPush()`는 변환을 적용한 뒤 `push(out)`을 호출합니다.
- **Filter 연산자 예시**: 다대일(many-to-one) 변환으로, 조건에 따라 요소를 선택적으로 전달합니다. `Filter`는 조건에 부합하는 요소를 푸시하거나, 부합하지 않는 경우 다시 풀하여 선택적인 요소 전파를 달성합니다.
- **Duplicator 연산자 예시**: 일대다(one-to-many) 변환으로 유상태(stateful) 처리가 필요합니다. `Duplicator`는 요소 상태를 유지하며 복제본을 방출합니다. 중요한 점은, 완료 전에 버퍼에 남아 있는 요소를 방출하기 위해 `onUpstreamFinish()`를 오버라이드한다는 것입니다. 대안 구현으로는 `emitMultiple(out, elements)`를 사용할 수 있는데, 이는 여러 요소를 안전하게 방출하기 위해 핸들러를 일시적으로 교체합니다.

### 2.3 포트 상태 기계(Port State Machine)

공식 문서는 유효한 전이(transition)를 보여 주는 상태 다이어그램을 제공합니다.

- **출력 포트**: `idle` → `waiting for pull` → `available` → `finished`
- **입력 포트**: `idle` → `waiting for push` → `available` → `finished`

### 2.4 고급 기능

#### 타이머 지원(Timer Support)

`TimerGraphStageLogic`은 다음을 통해 스케줄링을 가능하게 합니다.

- `scheduleOnce(key, delay)`
- `scheduleAtFixedRate(key, initialDelay, interval)`
- `scheduleWithFixedDelay(key, initialDelay, interval)`

타이머 만료를 처리하려면 `onTimer(timerKey)`를 오버라이드합니다. 타이머는 생성자(constructor)에서는 스케줄할 수 없지만, `preStart()`에서는 동작합니다.

#### 비동기 사이드 채널(Asynchronous Side-Channels)

`getAsyncCallback()`은 외부 이벤트를 안전하게 주입할 수 있게 해 줍니다.

> 실행 엔진이 제공된 콜백을 스레드 안전하게(thread-safe) 호출하는 일을 처리합니다(The execution engine will take care of calling the provided callback in a thread-safe way).

외부 API는 콜백을 직접 호출하지 않고, 반환된 `AsyncCallback`에 대해 `invoke(event)`를 호출합니다.

#### 커스텀 머티리얼라이즈 값(Custom Materialized Values)

`NotUsed`를 넘어서는 값을 반환하려면 `GraphStage` 대신 `GraphStageWithMaterializedValue`를 상속합니다. `createLogicAndMaterializedValue()`를 오버라이드하여 로직과 값을 모두 반환합니다.

> **경고**: 로직이 실행되는 스레드와 머티리얼라이즈된 값을 보유한 스레드 양쪽에서 이 값에 접근하는 것에 대한 기본 동기화(built-in synchronization)는 제공되지 않습니다.

#### 속도 분리(Rate Decoupling)

버퍼와 같은 연산자는 업스트림과 다운스트림의 속도를 분리(decouple)합니다. `TwoBuffer` 예시는 독립적인 수요 관리를 보여 줍니다. `preStart()`가 다운스트림 수요 없이 업스트림 풀을 시작하여, 블록되기 전에 두 개의 요소를 버퍼링할 수 있게 합니다.

### 2.5 생명주기와 자원(Lifecycle and Resources)

**정리(cleanup) 책임**은 핸들러 콜백이 아니라 `GraphStageLogic.postStop()`에 두어야 합니다. 이는 연산자의 완료, 실패, 또는 머티리얼라이저(materializer) 셧다운 시에 정리가 이루어지도록 보장합니다.

**완료 메서드**:

- `completeStage()`: 모든 출력을 닫고 모든 입력을 취소합니다.
- `failStage(exception)`: 모든 출력을 실패시키고 모든 입력을 취소합니다.

`setKeepGoing(true)`는 포트가 닫혀도 자동 셧다운이 일어나지 않게 하여 명시적인 완료 호출을 요구하지만, 누수(leak) 위험이 따릅니다.

### 2.6 스레드 안전성 보장(Thread Safety Guarantees)

커스텀 연산자는 액터와 유사한(actor-like) 시맨틱을 제공합니다.

> 이 클래스들이 노출하는 콜백은 절대 동시에(concurrently) 호출되지 않습니다. 이 클래스들이 캡슐화하는 상태는 추가적인 동기화 없이도 제공된 콜백 안에서 안전하게 수정할 수 있습니다.

> **중요한 제약**: 제공된 콜백 외부에서는 절대 연산자 상태에 접근하지 마십시오. 미래(future) 콜백은 동시성 위험 때문에 내부 상태를 클로저(closure)로 포착할 수 없습니다.

### 2.7 통합 패턴과 로깅

- `.via()` 연산자는 커스텀 연산자를 스트림에 통합합니다. 암시적(implicit) 스칼라 확장 메서드를 통해 `Source`와 `Flow`에 대해 DSL과 유사한 문법을 사용할 수 있습니다. 다만 `SubFlow` 확장은 Scala 2에서 실용적이지 않은 랭크-2 타입 추상화(rank-2 type abstraction)를 요구합니다.
- **로깅**: `GraphStageLogicWithLogging`과 `TimerGraphStageLogicWithLogging`은 액터 로깅과 유사하게 `log` 필드에 대한 접근을 제공합니다. 비동기 어펜더(asynchronous appender)가 블로킹을 막아 준다면 SLF4J를 직접 사용하는 것도 허용됩니다.

전형적인 연산자 작성 순서는 다음과 같습니다. (1) 포트를 갖는 형태(shape) 정의, (2) `createLogic()`에서 핸들러 등록, (3) `GraphStageLogic`에서 상태 관리, (4) `postStop()`에서 자원 정리, (5) `.via()`를 통한 외부 통합.

---

## 3. 스트림 에러 처리(Error Handling)

Akka Streams에서 연산자가 실패하면 보통 스트림 전체가 종료됩니다. 프레임워크는 전체 실패를 막고 스트림의 연속성을 유지하기 위한 여러 전략을 제공합니다.

### 3.1 에러 로깅(Logging Errors)

`log()` 연산자는 스트림 모니터링을 가능하게 합니다. 실패가 발생하면 에러 메시지가 자동으로 로그에 남습니다.

```
[error logging] Upstream failed.
java.lang.ArithmeticException: / by zero
```

이 방식은 예외를 포착하지만, 스트림 종료 자체를 막지는 않습니다.

### 3.2 recover

`recover` 연산자는 업스트림 실패 시 마지막 요소를 하나 방출한 다음 스트림을 정상적으로 완료합니다. 어떤 예외를 처리할지는 `PartialFunction`으로 결정합니다. 부합하지 않는(unmatched) 예외는 여전히 스트림을 실패시킵니다.

> **핵심 특성**: recover는 업스트림 실패 시 마지막 요소 하나를 방출한 뒤 스트림을 완료할 수 있게 해 줍니다(recover allows you to emit a final element and then complete the stream on an upstream failure).

예시 동작: `0 to 6`을 처리하다가 문제가 되는 값(예: 4, 5)을 만나면 복구(recovery)가 트리거되어, 스트림 완료 전에 에러 메시지를 출력합니다.

### 3.3 recoverWithRetries

이 연산자는 실패한 업스트림을 새로운 업스트림으로 교체하여, 최대 횟수까지 재시도(retry) 로직을 가능하게 합니다. 역시 `PartialFunction` 매칭을 사용합니다.

> **결과**: 실패 후 대체 소스(alternative source)에서 스트림이 이어지며, 백업 데이터 스트림으로 우아하게 폴백(fallback)할 수 있습니다.

### 3.4 지수 백오프를 사용한 재시작 연산자(Restart Operators with Exponential Backoff)

#### RestartSource, RestartSink, RestartFlow

이들은 지수 백오프(exponential backoff) 슈퍼비전을 구현하여, 시도 사이의 지연을 점점 늘려 가며 연산자를 재시작합니다. 이 패턴은 외부 자원이 일시적으로 사용 불가능할 때 빡빡한 재연결 루프(tight reconnection loop)를 방지합니다.

**설정 파라미터**:

- `minBackoff`: 최초 재시작 지연
- `maxBackoff`: 최대 지연 상한
- `randomFactor`: 변동량 추가(동기화된 재시작을 막기 위해 0.2 권장)
- `maxRestarts`: 전체 재시작 횟수 제한
- `maxRestartsWithin`: 재시작 횟수를 집계하는 시간 윈도우

> **중요 고려사항**: 재시작 간격에 추가적인 무작위성(randomness)을 더하면, 스트림들이 시간상 약간씩 다른 지점에서 시작하게 되어 큰 트래픽 스파이크를 피할 수 있습니다.

일반적인 사용처: 서버 비가용성 이후의 WebSocket 재연결.

### 3.5 슈퍼비전 전략(Supervision Strategies)

슈퍼비전(supervision)은 세 가지 지시(directive)를 통해 연산자별 예외 처리를 가능하게 합니다.

- **Stop (기본값)**: 예외가 발생하면 스트림이 실패와 함께 완료됩니다. 모든 연산자의 기본 동작입니다.
- **Resume**: "요소가 버려지고 스트림이 계속됩니다(The element is dropped and the stream continues)." 누적된 상태(accumulated state)는 그대로 유지됩니다. 일시적인 처리 오류를 다룰 때 유용합니다.
- **Restart**: Resume와 유사하지만, 연산자 인스턴스를 재생성하여 "누적된 상태가 모두 지워집니다(any accumulated state is cleared)." `scan`처럼 값을 누적하는 유상태 연산자에 유익합니다.

#### 슈퍼비전 전략 적용하기

전략은 `ActorAttributes.supervisionStrategy(decider)`를 통해 `RunnableGraph`나 개별 플로우에 부착됩니다.

```scala
val decider: Supervision.Decider = {
  case _: ArithmeticException => Supervision.Resume
  case _ => Supervision.Stop
}
```

데사이더(decider) 함수는 예외를 검사하여 적절한 지시를 반환합니다.

### 3.6 mapAsync 에러 처리

`mapAsync`와 `mapAsyncUnordered` 연산자는 `Future`/`CompletionStage` 실패에 대한 슈퍼비전을 지원합니다. `Supervision.resumingDecider()`를 사용하면 예외적으로 실패한 future의 요소를 버리고 다운스트림을 계속 진행할 수 있습니다.

> **예시 맥락**: 일부 레코드에 대해 실패하는 조회(lookup) 서비스를, 찾지 못한 항목은 버리고 유효한 항목만 처리하는 방식으로 우아하게 다룰 수 있습니다.

### 3.7 중요한 주의사항(Caveats)

- **데드락 위험**: "요소를 버리는 것은 사이클(cycle)이 있는 그래프에서 데드락(deadlock)을 초래할 수 있습니다(Dropping elements may result in deadlocks in graphs with cycles)." 피드백 루프를 포함하는 그래프에서는 Resume/Restart 전략을 신중하게 설계해야 합니다.
- **GraphStage 제약**: 재시작 래퍼(restart wrapper) 내부에서 조건부 종료 전파(conditional termination propagation)를 수행하는 일부 커스텀 연산자는 예상과 다르게 동작할 수 있습니다. 특히 `eagerCancel = false`인 `Broadcast`는 까다로운 문제를 일으킵니다.

### 3.8 설정 가이드

여러 접근법을 조합하면 견고한 에러 처리를 얻을 수 있습니다.

- 관측 가능성(observability)을 위해 `log()`를 사용
- 폴백 값과 함께 우아한 완료를 위해 `recover`를 적용
- 외부 서비스 복원력을 위해 `RestartSource`를 배치
- 연산자별 정책을 위해 슈퍼비전 전략을 부착
- 대량 작업에 대한 관용성을 위해 `mapAsync`와 `resumingDecider`를 사용

공식 문서는 "슈퍼비전이 스트림 연산자에 자동으로 적용되는 것이 아니라, 각 연산자가 명시적으로 구현해야 하는 것(supervision is not automatically applied to stream operators but instead something that each operator has to implement explicitly)"이라고 강조합니다. 즉, 모든 연산자가 이 전략을 지원하는 것은 아닙니다.

---

## 4. 스트리밍 IO 다루기(Working with Streaming IO)

Akka Streams는 백프레셔(back-pressure)를 투명하게 관리하면서 파일 IO와 TCP 연결을 다루는 도구를 제공합니다. 수동으로 다루는 Akka IO 방식과 달리, Streams는 백프레셔를 자동으로 처리하여 네트워크와 파일 작업을 단순화합니다.

> 이 기능을 사용하려면 `akka-stream` 모듈을 프로젝트에 추가해야 합니다.

### 4.1 스트리밍 TCP

#### 서버 측: 연결 수락(Accepting Connections)

`Tcp(system).bind()` 메서드는 수락된 클라이언트마다 `IncomingConnection` 요소를 방출하는 `Source`를 반환하는 서버를 생성합니다. 이 바인딩은 활성 소켓을 담은 `Future[ServerBinding]`을 만들어 냅니다.

- **기본 서버 설정**: `bind` 연산은 호스트와 포트를 받아, 리스닝을 시작하기 위해 머티리얼라이즈해야 하는 `Source`를 반환합니다. 들어오는 각 연결은 해당 연결 객체를 처리함으로써 개별적으로 다룰 수 있습니다.
- **에코 서버(Echo Server) 예시**: 각 연결마다, 개발자는 들어오는 `ByteString`을 처리하는 `Flow`를 정의합니다. `Framing.delimiter` 헬퍼는 구분자(예: 개행 문자)를 기준으로 바이트 스트림을 논리적 메시지로 청크(chunk)화합니다. 연결의 `handleWith` 메서드가 그 `Flow`를 소켓에 적용하여 양방향 데이터 교환을 관리합니다.
- **연결 처리**: 서버는 연산을 연쇄하여 데이터를 처리할 수 있습니다. 즉, 프레이밍된 메시지를 수신하고, 내용을 변환한 뒤, 응답을 다시 전송합니다. 예시는 받은 텍스트에 느낌표를 덧붙여 다시 에코하는 모습을 보여 줍니다.
- **연결 닫기**: 들어오는 `Flow`를 취소하거나 어느 한쪽을 끊어서 연결을 종료할 수 있습니다. 서버 소켓 자체는 연결 `Source`를 취소하여 셧다운할 수 있습니다.

#### 클라이언트 측: 아웃바운드 연결(Outgoing Connections)

`Tcp(system).outgoingConnection()` 팩토리는 원격 서버에 연결하기 위한 클라이언트 `Flow`를 생성합니다. 바인딩과 달리, 아웃바운드 연결은 커스텀 로직과 조인(join)할 수 있는 `Flow`를 반환합니다.

- **REPL 클라이언트 예시**: Read-Evaluate-Print-Loop 클라이언트는 대화형 TCP 통신을 보여 줍니다. 구현은 `Flow`를 사용하여 서버 응답을 처리하고, 그것을 출력하며, 사용자 입력을 읽어 명령을 전송합니다. `join` 메서드는 연결 `Flow`와 애플리케이션 로직을 하나의 통합된 스트림으로 결합합니다.

#### 백프레셔 사이클에서의 데드락 회피

백프레셔 데드락은 양쪽이 모두 전송하기 전에 입력을 기다릴 때 발생합니다. 클라이언트-서버 시나리오에서, 이를 해결하려면 한쪽이 먼저 통신을 시작해야 합니다.

- **해결 전략**: 서버(또는 적절한 쪽)가 초기 메시지를 주입하여 사이클을 끊습니다. 이는 단 하나의 "대화 시작자(conversation starter)" 요소를 갖는 `Source`를 처리 `Flow`에 병합(merge)함으로써 달성됩니다. 상호 백프레셔에도 불구하고 통신이 시작되도록 보장합니다.
- **프로토콜별 처리**: 많은 프로토콜은 어느 쪽이 먼저 말해야 하는지를 자연스럽게 규정합니다. 적절한 명령 파서(command parser)와 스트림 완료 로직(예: "BYE"나 "q" 명령 인식)을 구현하면 조정된 셧다운이 가능합니다.

#### 프레이밍 프로토콜(Framing Protocols)

TCP는 메시지 경계가 없는 바이트 스트림만 제공하므로 프레이밍(framing) 방식이 필요합니다.

- **구분자 기반(Delimiter-Based)**: `Framing.delimiter`는 프레임 종료 마커(예: `\n`)를 사용해 논리적 메시지를 분리하며, 최대 프레임 길이와 절단(truncation) 처리를 설정할 수 있습니다.
- **길이 접두(Length-Prefixed)**: `Framing.simpleFramingProtocol`은 길이 필드를 사용해 메시지를 구분하여, 고정 길이 프로토콜을 가능하게 합니다.
- **JSON 프레이밍**: `JsonFraming.objectScanner`는 `ByteString` 스트림에서 유효한 JSON 객체를 식별하고 분리하며, TCP 상의 JSON 프로토콜에 유용합니다.

#### TLS 지원

Akka는 TLS 변형을 제공합니다. `outgoingConnectionWithTls`, `bindWithTls`, `bindAndHandleWithTls`. 이들은 키스토어(keystore)/트러스트스토어(truststore) 정보를 갖춘 `SSLEngine` 설정을 요구합니다.

- **설정 과정**: 개발자는 키스토어를 로드하고, `TrustManagerFactory`와 `KeyManagerFactory` 인스턴스를 초기화하며, `SSLContext`를 구성하고, 클라이언트/서버 역할·암호 스위트(cipher suite)·프로토콜을 지정한 `SSLEngine` 인스턴스를 생성해야 합니다.

### 4.2 스트리밍 파일 IO

- **파일 읽기**: `FileIO.fromPath()`는 파일로부터 `ByteString` 청크를 방출하는 `Source`를 생성합니다. 선택적 `chunkSize` 파라미터는 스트림 요소당 버퍼 크기를 제어합니다. 읽기는 간단합니다. 소스를 어떤 싱크에든 연결하여 처리하면 됩니다.
- **파일 쓰기**: `FileIO.toPath()`는 파일에 쓰기 위한 `ByteString` 청크를 받는 `Sink`를 생성하여, 파이프라인식 파일 처리를 위해 읽기 작업을 보완합니다.
- **디스패처 설정**: 파일 IO 연산은 파일 작업을 액터 시스템 연산으로부터 격리하는 전용 블로킹 디스패처(blocking dispatcher)를 사용합니다. 기본 디스패처(`akka.stream.materializer.blocking-io-dispatcher`)는 전역적으로 또는 `withAttributes()`와 커스텀 디스패처 지정을 사용해 연산자별로 커스터마이즈할 수 있어, 최적의 자원 활용을 보장합니다.

---

## 5. 액터와의 연동(Integration with Actors)

이 섹션은 스트림과 액터 기반 시스템을 결합하는 여러 접근법을 설명합니다. 이러한 기법들은 기존 API, 공유된 가변 상태(shared mutable state), 또는 스트림 실행 중 외부 영향을 받는 로직이 관여하는 시나리오를 다룹니다.

> 이 기능을 사용하려면 `akka-stream` 아티팩트를 프로젝트에 추가해야 합니다.

### 5.1 Ask 패턴(ask 연산자)

`ask` 연산자는 백프레셔를 유지하면서 스트림 요소의 처리를 액터에 위임합니다. 공식 문서에 따르면, "스트림의 백프레셔는 ask의 `Future`/`CompletionStage`에 의해 유지되며, 주어진 병렬성(parallelism)보다 더 많은 메시지로 액터의 메일박스(mailbox)가 채워지지 않습니다."

주요 특성:

- 병렬성 설정과 무관하게 요소의 순서(ordering)가 유지됩니다.
- 액터는 각 메시지에 대해 반드시 `sender()`에게 응답해야 합니다.
- 응답(reply)이 다운스트림 요소가 됩니다.
- 대상 액터가 종료되면 `AskStageTargetActorTerminatedException`이 트리거됩니다.
- 타임아웃 실패는 `TimeoutException`을 발생시킵니다.

> 공식 문서는 "요소의 발신자(스트림)에게 응답하는 것이 필수이며, 이 ack 신호가 없으면 백프레셔로 해석된다(replying to the sender of the elements (the stream) is required as lack of those ack signals would be interpreted as back-pressure)"고 명시합니다.

### 5.2 Sink.actorRefWithBackpressure

이 싱크는 스트림 요소를 액터에 전송하며, 백프레셔 제어를 위해 확인(acknowledgment) 메시지를 요구합니다. 패턴은 다음을 포함합니다.

- 스트림 시작을 알리는 초기 메시지
- 각 요소 이후의 확인(ack)
- 성공 시 완료 메시지
- 스트림 에러 시 실패 메시지

> 공식 문서는 액터 메일박스에 버퍼가 누적되는 것을 막기 위해 "요소의 발신자(스트림)에게 응답하는 것이 필수(replying to the sender of the elements (the stream) is required)"라고 강조합니다.

### 5.3 Source.queue

이 소스는 외부 코드(액터 포함)에서 요소를 스트림으로 푸시할 수 있게 합니다. `offer` 메서드는 요소가 큐에 들어갔는지(enqueued), 버려졌는지(dropped), 실패했는지(failed)를 나타내는 `Future`/`CompletionStage`를 반환합니다. 공식 문서는 요소가 "스트림이 처리할 수 있을 때까지 버퍼링된다(will be buffered until the stream can process them)"고 명시합니다.

`OverflowStrategy.backpressure`를 사용하면, 버퍼 공간이 생길 때까지 offer future의 완료를 지연시켜 요소 손실을 방지합니다.

### 5.4 Source.actorRef

머티리얼라이즈된 `ActorRef`로 전송된 메시지가 다운스트림으로 방출됩니다. `Source.queue`와 달리, 이 방식은 백프레셔 전략을 지원하지 않습니다. 공식 문서는 "스트림이 소비할 수 있는 것보다 빠른 속도로 전송하여 버퍼가 가득 차면 요소가 버려진다(elements will be dropped if the buffer is filled by sending at a rate that is faster than the stream can consume)"고 언급합니다.

스트림 완료는 완료(completion) 또는 실패(failure) 매칭 함수를 통해 트리거할 수 있습니다.

### 5.5 타입드 액터(Typed Actor) 소스와 싱크

공식 문서는 네 가지 타입드(typed) 대안을 설명합니다.

- **`ActorSource.actorRef`**: 매칭되는 메시지를 방출하는 `ActorRef[T]`를 머티리얼라이즈합니다.
- **`ActorSource.actorRefWithBackpressure`**: 소스로부터 확인 기반(acknowledgment-based) 백프레셔를 제공합니다.
- **`ActorSink.actorRef`**: 백프레셔를 고려하지 않고 스트림 요소를 전송합니다.
- **`ActorSink.actorRefWithBackpressure`**: 백프레셔 신호와 함께 요소를 전송합니다.

### 5.6 Pub/Sub 연동

공식 문서는 Akka 타입드 액터 토픽(topic) 시스템에 메시지를 구독하고 발행하기 위한 **`Topic.source`**와 **`Topic.sink`**를 언급합니다.

### 5.7 중요한 구현 고려사항

공식 문서는 map 연산 안에서 `Sink.actorRef`나 tell 기반 패턴을 사용하는 것을 경고합니다. 왜냐하면 "대상 액터로부터의 백프레셔 신호가 없어(there is no back-pressure signal from the destination actor)" 메일박스 오버플로를 초래할 수 있기 때문입니다. 대신 `Sink.actorRefWithBackpressure`나 `mapAsync` 안의 `ask`를 선호할 것을 권장합니다.

라우터(router) 기반 시나리오의 경우, "응답의 순서가 중요하지 않으므로(the ordering of the replies is not important)" 수동으로 ask를 구현하는 `mapAsyncUnordered`를 사용할 것을 제안합니다.

---

## 6. Reactive Streams 상호운용성(Reactive Streams Interoperability)

Akka Streams는 Reactive Streams 표준을 구현합니다. 이 표준은 "논블로킹 백프레셔를 갖춘 비동기 스트림 처리(asynchronous stream processing with non-blocking back pressure)"를 제공합니다. 프레임워크는 여러 API 버전과 다른 리액티브 라이브러리 간의 상호운용성을 지원합니다.

### 6.1 API 버전

Akka는 두 가지 별개의 Reactive Streams API 구현에 대한 상호운용성을 제공합니다.

1. **독립 Reactive Streams(Standalone Reactive Streams)**: `org.reactivestreams` 패키지를 사용합니다(Java 8 호환).
2. **Java 9+ 표준 라이브러리**: `java.util.concurrent.Flow` 네임스페이스를 사용하며, 전용 `JavaFlowSupport` 팩토리를 통해 접근합니다.

> 공식 문서는 "Java 8에서는 필요한 인터페이스가 단순히 존재하지 않기 때문에 `JavaFlowSupport`를 사용할 수 없다(it is not possible to use JavaFlowSupport on Java 8 since the needed interfaces simply is not available)"고 언급합니다.

### 6.2 핵심 인터페이스

두 가지 기본 인터페이스는 `Publisher`와 `Subscriber`입니다. `Processor`는 둘을 결합하여, 스트림의 소비자(consumer)이자 생산자(producer)로 동작합니다.

### 6.3 실용적인 상호운용성 패턴

#### Publisher로부터 Source 생성(Source from Publisher)

외부 publisher를 Akka 소스로 변환합니다.

```
Source.fromPublisher(tweets).via(authors).to(Sink.fromSubscriber(storage)).run()
```

이를 통해 Akka가 아닌 리액티브 라이브러리로부터 데이터를 수집할 수 있습니다.

#### Subscriber로부터 Sink 생성(Sink from Subscriber)

Akka 싱크를 외부 subscriber에 노출합니다.

```
Source.fromPublisher(tweets).via(authors).to(Sink.fromSubscriber(storage))
```

#### Source를 Publisher로 노출하기(Exposing Sources as Publishers)

Akka 소스를 발행 가능한 스트림으로 변환합니다.

```
Source.fromPublisher(tweets).via(authors).runWith(Sink.asPublisher(fanout = false))
```

**단일 구독 vs. 다중 구독**:

- `fanout = false`는 단 하나의 구독자만 지원합니다. 추가 시도는 `IllegalStateException`을 발생시킵니다.
- `fanout = true`는 여러 구독자로의 팬-아웃/브로드캐스팅을 가능하게 하며, 입력 버퍼가 구독자 간 속도 차이를 제어합니다.

#### Sink를 Subscriber로 노출하기(Exposing Sinks as Subscribers)

Akka 싱크로부터 subscriber 엔드포인트를 생성합니다.

```
authors.to(Sink.fromSubscriber(storage)).runWith(Source.asSubscriber[Tweet])
```

#### Processor 래핑(Processor Wrapping)

외부 `Processor` 인스턴스를 Akka 플로우로 통합합니다.

```
val flow: Flow[Int, Int, NotUsed] = Flow.fromProcessor(() => createProcessor)
```

재사용성을 위해 팩토리 함수(factory function)가 필요하다는 점에 유의하십시오.

### 6.4 호환 라이브러리

공식 문서는 다음과 같은 다른 Reactive Streams 구현을 식별합니다.

- Reactor (1.1+)
- RxJava
- Ratpack
- Slick

이 표준화된 인터페이스는 서로 다른 스트리밍 프레임워크 간의 매끄러운 조합(composition)을 가능하게 합니다.

---

## 7. 스트림 테스트하기(Testing Streams)

Akka Streams는 여러 접근법을 통해 포괄적인 테스트 기능을 제공합니다. 공식 문서에 따르면, "Akka Stream 소스, 플로우, 싱크의 동작 검증은 다양한 코드 패턴과 라이브러리를 사용하여 수행할 수 있습니다(Verifying behavior of Akka Stream sources, flows and sinks can be done using various code patterns and libraries)."

### 7.1 내장 소스, 싱크, 연산자

가장 간단한 테스트 접근법은 표준 Akka 컴포넌트를 활용하는 것입니다. 커스텀 싱크를 테스트할 때는 미리 정의된 데이터를 갖는 소스를 연결하고, 플로우를 실행한 뒤, 결과를 단언(assert)합니다. 무한 스트림을 생성하는 소스의 경우, `take` 연산자와 `Sink.seq`를 결합하면 초기 요소들이 기대 조건을 만족하는지 효과적으로 검증할 수 있습니다.

플로우를 테스트할 때는 스트림의 양 끝점이 모두 테스트 제어하에 있으므로, 엣지 케이스를 위한 유연한 소스 선택과 손쉬운 단언을 가능하게 하는 싱크를 사용할 수 있습니다.

### 7.2 ActorRef를 사용한 TestKit 통합

Akka Stream은 액터와 기본적으로 통합됩니다. `Sink.actorRef` 컴포넌트는 들어오는 요소를 지정된 `ActorRef`로 보내, `TestProbe` 단언을 가능하게 합니다. 스트림 완료는 `onCompleteMessage` 파라미터를 통해 신호됩니다.

`Source.actorRef`는 역방향 제어를 제공합니다. 액터 참조를 통해 요소를 전송할 완전한 권한을 가지며, 커스텀 완료·실패 매처(matcher)가 스트림 생명주기를 통제합니다.

### 7.3 Streams TestKit 모듈

전용 `akka-stream-testkit` 모듈은 두 가지 주요 컴포넌트, `TestSource`와 `TestSink`를 제공합니다.

**`TestSink.probe`**는 구독 프로브(subscription probe)로 머티리얼라이즈되어, "수요에 대한 수동 제어와 다운스트림으로 내려오는 요소에 대한 단언(manual control over demand and assertions over elements coming downstream)"을 가능하게 합니다. 주요 메서드:

- `request(n)`: 수요(demand)를 신호합니다.
- `expectNext()`: 특정 요소를 검증합니다.
- `expectComplete()`: 스트림 종료를 단언합니다.

**`TestSource.probe`**는 다운스트림 컴포넌트를 테스트하기 위한 역방향 제어를 제공합니다.

- `sendNext()`: 요소를 주입합니다.
- `sendError()`: 실패 조건을 트리거합니다.
- `expectCancellation()`: 취소 처리를 검증합니다.

이 컴포넌트들을 결합하면 순차적·비순차적 요소 검증을 모두 지원하는 포괄적인 플로우 테스트가 가능합니다. (이 밖에 자주 쓰이는 메서드로는 수요 신호와 요소 검증을 한 번에 수행하는 `requestNext`, 발신용 `sendComplete`, 구독 보장을 위한 `ensureSubscription` 등이 있습니다.)

### 7.4 퍼징 모드(Fuzzing Mode)

공격적인 동시성 테스트를 위해, 설정에서 `akka.stream.materializer.debug.fuzzing-mode = on`을 활성화할 수 있습니다. 이는 경쟁 상태(race condition)를 드러내기 위해 "동시 실행 경로를 더 공격적으로 실행(exercises concurrent execution paths more aggressively)"합니다. 다만 성능에 상당한 영향을 주므로 엄격하게 테스트 환경에서만 사용해야 합니다.

---

## 8. 연산자 색인(Operators Index)

다음은 카테고리별로 정리한 Akka Streams 연산자 목록입니다.

### 8.1 소스 연산자(Source Operators)

| 연산자 | 설명 |
| --- | --- |
| `asSourceWithContext` | 소스 요소에서 컨텍스트 데이터를 추출하여 컨텍스트 전파(context propagation)를 가능하게 함 |
| `asSubscriber` | Reactive Streams와 연동, `Subscriber`로 머티리얼라이즈 |
| `combine` | 지정된 전략으로 여러 소스를 병합 |
| `completionStage` | 완료 시 단일 `CompletionStage` 값을 방출 |
| `completionStageSource` | 완료 후 비동기 소스로부터 요소를 스트리밍 |
| `cycle` | 이터레이터(iterator) 값을 반복적으로 스트리밍 |
| `empty` | 요소 방출 없이 즉시 완료 |
| `failed` | 사용자가 지정한 예외로 즉시 실패 |
| `from` | `immutable.Seq`나 `Iterable`로부터 값을 스트리밍 |
| `fromCompletionStage` | `Source.completionStage`로 대체됨(Deprecated) |
| `fromFuture` | `Source.future`로 대체됨(Deprecated) |
| `fromFutureSource` | `Source.futureSource`로 대체됨(Deprecated) |
| `fromIterator` | 수요에 따라 `Iterator` 값을 스트리밍 |
| `fromJavaStream` | 수요에 따라 Java 8 `Stream` 값을 스트리밍 |
| `fromPublisher` | Reactive Streams와 연동, `Publisher`를 구독 |
| `fromSourceCompletionStage` | `Source.completionStageSource`로 대체됨(Deprecated) |
| `future` | 수요가 있을 때 완료된 단일 `Future` 값을 방출 |
| `futureSource` | 완료된 future 소스로부터 요소를 스트리밍 |
| `lazily` | `Source.lazySource`로 대체됨(Deprecated) |
| `lazilyAsync` | `Source.lazyFutureSource`로 대체됨(Deprecated) |
| `lazyCompletionStage` | 수요가 생길 때까지 future 단일 요소 생성을 지연 |
| `lazyCompletionStageSource` | 수요가 생길 때까지 future 소스 생성을 지연 |
| `lazyFuture` | 수요가 생길 때까지 future 단일 요소 생성을 지연 |
| `lazyFutureSource` | 수요가 생길 때까지 소스 생성과 머티리얼라이제이션을 지연 |
| `lazySingle` | 수요가 생길 때까지 단일 요소 생성을 지연 |
| `lazySource` | 수요가 생길 때까지 소스 생성과 머티리얼라이제이션을 지연 |
| `maybe` | 머티리얼라이즈된 `Promise`/`CompletableFuture`가 완료되면 방출 |
| `never` | 어떤 요소도 방출하지 않고 완료·실패도 하지 않음 |
| `queue` | 요소를 푸시하기 위한 `BoundedSourceQueue`로 머티리얼라이즈 |
| `range` | 설정 가능한 스텝으로 범위 내 정수를 방출 |
| `repeat` | 단일 객체를 반복적으로 스트리밍 |
| `single` | 단일 객체를 한 번 스트리밍 |
| `tick` | 주기적으로 임의의 객체를 반복 |
| `unfold` | 함수가 `Some`/비어 있지 않은 `Optional`을 반환하는 동안 결과를 스트리밍 |
| `unfoldAsync` | `Future`/`CompletionStage`를 반환하는 함수로 unfold |
| `unfoldResource` | open·query·close 함수로 자원을 래핑 |
| `unfoldResourceAsync` | 비동기 open·query·close로 자원을 래핑 |
| `zipN` | 여러 소스의 요소를 시퀀스로 결합 |
| `zipWithN` | 결합 함수로 여러 스트림 요소를 결합 |

### 8.2 싱크 연산자(Sink Operators)

| 연산자 | 설명 |
| --- | --- |
| `asPublisher` | Reactive Streams와 연동, `Publisher`로 머티리얼라이즈 |
| `cancelled` | 스트림을 즉시 취소 |
| `collect` | Java `Collector`로 요소를 수집 |
| `collection` | 방출된 값을 컬렉션으로 수집(Scala 전용) |
| `combine` | 사용자가 지정한 전략으로 여러 싱크를 결합 |
| `completionStageSink` | 완료 후 future 싱크로 요소를 스트리밍 |
| `fold` | 함수로 요소를 폴드(fold)하여 최종 결과를 방출 |
| `foreach` | 받은 각 요소에 대해 프로시저를 실행 |
| `foreachAsync` | 각 요소에 대해 비동기로 프로시저를 실행 |
| `fromMaterializer` | 머티리얼라이제이션 시점까지 `Sink` 생성을 지연 |
| `fromSubscriber` | Reactive Streams와 연동, `Subscriber`를 래핑 |
| `futureSink` | 완료 후 future 싱크로 요소를 스트리밍 |
| `head` | 첫 값으로 완료되는 `Future`로 머티리얼라이즈한 뒤 취소 |
| `headOption` | 첫 값을 `Optional`로 감싸 완료되는 `Future`로 머티리얼라이즈 |
| `ignore` | 모든 요소를 소비하고 버림 |
| `last` | 마지막으로 방출된 값으로 완료되는 `Future`로 머티리얼라이즈 |
| `lastOption` | 마지막 값을 `Optional`로 감싸 완료되는 `Future`로 머티리얼라이즈 |
| `lazyCompletionStageSink` | 첫 요소가 올 때까지 싱크 생성을 지연 |
| `lazyFutureSink` | 첫 요소가 올 때까지 싱크 생성을 지연 |
| `lazyInitAsync` | `Sink.lazyFutureSink`로 대체됨(Deprecated) |
| `lazySink` | 첫 요소가 올 때까지 싱크 생성을 지연 |
| `never` | 항상 백프레셔를 걸고, 취소하거나 요소를 소비하지 않음 |
| `onComplete` | 스트림 완료 또는 실패 시 콜백을 실행 |
| `preMaterialize` | 즉시 머티리얼라이즈하여 머티리얼라이즈 값과 새 `Sink`를 반환 |
| `queue` | 풀(pull)로 수요를 트리거하는 `SinkQueue`로 머티리얼라이즈 |
| `reduce` | 들어오는 요소에 리덕션(reduction) 함수를 적용 |
| `seq` | 스트림에서 방출된 값을 컬렉션으로 수집 |
| `setup` | 머티리얼라이제이션 시점까지 `Sink` 생성을 지연 |
| `takeLast` | 마지막 n개의 값을 컬렉션으로 수집 |

### 8.3 추가 싱크/소스 변환기(Additional Sink and Source Converters)

| 연산자 | 설명 |
| --- | --- |
| `asInputStream` | 블로킹 `InputStream`으로 머티리얼라이즈되는 싱크 |
| `asJavaStream` | Java 8 `Stream`으로 머티리얼라이즈되는 싱크 |
| `asOutputStream` | `OutputStream`으로 머티리얼라이즈되는 소스 |
| `fromInputStream` | `InputStream`을 래핑하는 소스 |
| `fromJavaStream` | Java 8 `Stream`을 래핑하는 소스 |
| `fromOutputStream` | `OutputStream`을 래핑하는 싱크 |
| `javaCollector` | `Collector` 결과를 담은 `Future`로 머티리얼라이즈되는 싱크 |
| `javaCollectorParallelUnordered` | 병렬·비순차 `Collector` 결과를 갖는 싱크 |

### 8.4 파일 IO 싱크/소스(File IO Sinks and Sources)

| 연산자 | 설명 |
| --- | --- |
| `fromFile` | 파일 내용을 방출 |
| `fromPath` | 주어진 경로의 파일 내용을 방출 |
| `toFile` | 들어오는 `ByteString`을 파일에 씀 |
| `toPath` | 들어오는 `ByteString`을 파일 경로에 씀 |

### 8.5 단순 연산자(Simple Operators)

| 연산자 | 설명 |
| --- | --- |
| `asFlowWithContext` | `Flow` 요소에서 컨텍스트를 추출 |
| `collect` | 부분 함수(partial function)를 적용하여 정의된 결과를 다운스트림으로 전달 |
| `collectType` | 요소 타입을 검사하여 해당 인스턴스를 다운스트림으로 전달 |
| `completionStageFlow` | 완료 후 future 플로우를 통해 요소를 스트리밍 |
| `contramap` | 들어오는 업스트림 요소에 함수를 적용 |
| `detach` | 업스트림을 다운스트림 수요로부터 분리 |
| `drop` | n개 요소를 버리고 이후 요소를 다운스트림으로 전달 |
| `dropWhile` | 술어(predicate)가 true인 동안 요소를 버림 |
| `filter` | 술어로 들어오는 요소를 필터링 |
| `filterNot` | 부정된 술어로 들어오는 요소를 필터링 |
| `flattenOptional` | `Optional` 값을 수집하고 빈 값을 필터링 |
| `fold` | 제로(zero)에서 시작하여 현재 값과 다음 값에 함수를 적용 |
| `foldAsync` | `Future`/`CompletionStage`를 반환하는 함수로 폴드 |
| `fromMaterializer` | 머티리얼라이제이션 시점까지 `Source`/`Flow` 생성을 지연 |
| `futureFlow` | 완료 후 future 플로우를 통해 요소를 스트리밍 |
| `grouped` | 개수에 도달할 때까지 요소를 누적하여 다운스트림으로 전달 |
| `groupedWeighted` | 결합 가중치(weight) 임계값에 도달할 때까지 누적 |
| `intersperse` | 스트림 요소 사이에 제공된 요소를 삽입 |
| `lazyCompletionStageFlow` | 첫 요소가 올 때까지 플로우 생성을 지연 |
| `lazyFlow` | 첫 요소가 올 때까지 플로우 생성을 지연 |
| `lazyFutureFlow` | 첫 요소가 올 때까지 플로우 생성을 지연 |
| `lazyInitAsync` | `prefixAndTail`과 함께 `Flow.lazyFutureFlow`로 대체됨(Deprecated) |
| `limit` | 업스트림 요소를 최대 개수로 제한 |
| `limitWeighted` | 들어오는 요소의 총 가중치를 제한 |
| `log` | 흐르는 요소, 완료, 에러를 로깅 |
| `logWithMarker` | 커스텀 마커(marker)와 함께 로깅 |
| `map` | 매핑 함수로 각 요소를 변환 |
| `mapConcat` | 요소를 0개 이상의 다운스트림 요소로 변환 |
| `mapWithResource` | 자원 지원과 함께 요소를 매핑 |
| `preMaterialize` | 즉시 머티리얼라이즈하여 값과 사전 머티리얼라이즈된 그래프를 반환 |
| `reduce` | 첫 요소에서 시작하여 함수를 적용 |
| `scan` | 제로에서 시작하여 현재 값을 방출하고 함수를 적용 |
| `scanAsync` | `Future`/`CompletionStage`를 반환하는 함수로 스캔 |
| `setup` | 머티리얼라이제이션 시점까지 `Source`/`Flow` 생성을 지연 |
| `sliding` | 스트림 위에 슬라이딩 윈도우(sliding window)를 제공 |
| `statefulMap` | 상태 지원과 함께 요소를 변환 |
| `statefulMapConcat` | 상태와 함께 0개 이상의 요소로 변환 |
| `take` | n개의 들어오는 요소를 다운스트림으로 전달한 뒤 완료 |
| `takeWhile` | 술어가 true인 동안 요소를 전달한 뒤 완료 |
| `throttle` | 시간 단위당 요소 또는 비용으로 처리량을 제한 |

### 8.6 싱크/소스로 구성된 플로우 연산자(Flow Operators Composed of Sinks and Sources)

| 연산자 | 설명 |
| --- | --- |
| `fromSinkAndSource` | `Sink`와 `Source`로부터 `Flow`를 생성 |
| `fromSinkAndSourceCoupled` | `Sink`와 `Source`의 종료를 결합한 `Flow`를 생성 |

### 8.7 비동기 연산자(Asynchronous Operators)

| 연산자 | 설명 |
| --- | --- |
| `mapAsync` | `Future`/`CompletionStage`를 반환하는 함수에 요소를 전달 |
| `mapAsyncPartitioned` | 파티션별 `Future` 제한을 갖는 비동기 매핑 |
| `mapAsyncUnordered` | 요소를 비동기로 전달하고 순서와 무관하게 결과를 방출 |

### 8.8 타이머 구동 연산자(Timer Driven Operators)

| 연산자 | 설명 |
| --- | --- |
| `delay` | 각 요소를 지정된 시간만큼 지연 |
| `delayWith` | 동적으로 제어되는 시간만큼 요소를 지연 |
| `dropWithin` | 타임아웃이 발생할 때까지 요소를 버림 |
| `groupedWeightedWithin` | 시간 윈도우 또는 가중치 임계값으로 청크화 |
| `groupedWithin` | 시간 윈도우 또는 요소 개수 임계값으로 청크화 |
| `initialDelay` | 첫 요소를 지정된 시간만큼 지연 |
| `takeWithin` | 타임아웃 이내의 요소를 전달한 뒤 완료 |

### 8.9 백프레셔 인지 연산자(Backpressure Aware Operators)

| 연산자 | 설명 |
| --- | --- |
| `aggregateWithBoundary` | 경계 조건까지 집계하여 방출 |
| `batch` | 백프레셔가 걸린 동안 요소를 배치(batch) |
| `batchWeighted` | 백프레셔가 걸린 동안 가중치로 배치 |
| `buffer` | 일시적으로 더 빠른 업스트림 이벤트를 버퍼링 |
| `conflate` | 백프레셔 동안 요소를 집계(conflate) |
| `conflateWithSeed` | 시드(seed) 집계 함수로 conflate |
| `expand` | 마지막 요소를 `Iterator`로 확장하여 다운스트림을 빠르게 함 |
| `extrapolate` | 초기 값과 함께 마지막 요소를 `Iterator`로 확장 |

### 8.10 중첩과 평탄화 연산자(Nesting and Flattening Operators)

| 연산자 | 설명 |
| --- | --- |
| `flatMapConcat` | 요소를 `Source`로 변환하고 연결(concatenation)로 평탄화 |
| `flatMapMerge` | 요소를 `Source`로 변환하고 병합(merging)으로 평탄화 |
| `flatMapPrefix` | 처음 n개 요소로 나머지 처리를 결정 |
| `groupBy` | 스트림을 별도의 출력 스트림으로 역다중화(demultiplex) |
| `prefixAndTail` | n개 요소를 취하고 나머지 스트림과의 쌍(pair)을 반환 |
| `splitAfter` | 술어가 true일 때 서브스트림(substream)을 종료 |
| `splitWhen` | 술어가 true일 때 새 서브스트림으로 분할 |

### 8.11 시간 인지 연산자(Time Aware Operators)

| 연산자 | 설명 |
| --- | --- |
| `backpressureTimeout` | 요소 방출이 수요까지 타임아웃을 초과하면 실패 |
| `completionTimeout` | 스트림 완료가 타임아웃을 초과하면 실패 |
| `idleTimeout` | 요소 간 시간 간격이 타임아웃을 초과하면 실패 |
| `initialTimeout` | 첫 요소가 타임아웃 내에 도착하지 않으면 실패 |
| `keepAlive` | 업스트림이 설정 시간 동안 침묵하면 요소를 주입 |

### 8.12 팬-인 연산자(Fan-in Operators)

| 연산자 | 설명 |
| --- | --- |
| `concat` | 업스트림 완료 후 주어진 소스의 요소를 방출 |
| `concatAllLazy` | 완료 후 여러 소스의 요소를 순차적으로 방출 |
| `concatLazy` | 업스트림 완료 후 소스의 요소를 방출 |
| `interleave` | 원본 소스와 제공된 소스에서 번갈아 방출 |
| `interleaveAll` | 원본 소스와 제공된 소스들에서 번갈아 방출 |
| `merge` | 여러 소스를 병합 |
| `mergeAll` | 여러 소스를 병합 |
| `mergeLatest` | 각 소스에서 최신 값을 골라 병합 |
| `mergePreferred` | 선호(preferred) 소스와 함께 병합 |
| `mergePrioritized` | 우선순위(prioritization)와 함께 병합 |
| `mergePrioritizedN` | 우선순위를 갖는 여러 소스를 병합 |
| `MergeSequence` | 여러 소스에 걸친 선형 시퀀스를 병합 |
| `mergeSorted` | 정렬 순서를 유지하며 여러 소스를 병합 |
| `orElse` | 기본 소스가 비어 완료되면 보조 소스를 방출 |
| `prepend` | 원본 소스 소비 전에 소스를 앞에 붙임 |
| `prependLazy` | 소비 전에 소스를 지연 방식으로 앞에 붙임 |
| `zip` | 소스들의 요소를 튜플(tuple)로 결합 |
| `zipAll` | 조기 완료를 처리하며 소스들을 튜플로 결합 |
| `zipLatest` | 각 소스에서 최신 값을 골라 결합 |
| `zipLatestWith` | 각 소스에서 최신 값을 골라 함수로 결합 |
| `zipWith` | 여러 소스와 함수로 결합 |
| `zipWithIndex` | 스트림 요소를 인덱스(index)와 지퍼링 |

### 8.13 팬-아웃 연산자(Fan-out Operators)

| 연산자 | 설명 |
| --- | --- |
| `alsoTo` | `Flow`에 `Sink`를 부착하여 요소를 양쪽으로 전송 |
| `alsoToAll` | `Flow`에 여러 `Source`(싱크)를 부착하여 요소를 모두에게 전송 |
| `Balance` | 스트림을 여러 스트림으로 팬-아웃 |
| `Broadcast` | 각 요소를 n개 출력 모두에 방출 |
| `divertTo` | 술어에 따라 요소를 싱크 또는 다운스트림으로 라우팅 |
| `Partition` | 스트림을 여러 스트림으로 팬-아웃 |
| `Unzip` | 두 요소 튜플을 별도의 다운스트림으로 언지핑 |
| `UnzipWith` | 함수로 요소를 여러 다운스트림으로 분할 |
| `wireTap` | 메인라인에 영향을 주지 않고 `Sink`를 와이어 탭(wire tap)으로 부착 |

### 8.14 상태 감시 연산자(Watching Status Operators)

| 연산자 | 설명 |
| --- | --- |
| `monitor` | 메시지 모니터링을 위한 `FlowMonitor`로 머티리얼라이즈 |
| `watchTermination` | `Done` 또는 실패로 완료되는 `Future`로 머티리얼라이즈 |

### 8.15 액터 연동 연산자(Actor Interop Operators)

| 연산자 | 설명 |
| --- | --- |
| `ActorFlow.ask` | 요소를 신규 액터 API에 ask로 보내고 응답을 기대 |
| `ActorFlow.askWithContext` | 컨텍스트 없이 ask하고 응답을 기대 |
| `ActorFlow.askWithStatus` | `StatusReply`를 기대하며 ask하고 T를 언래핑 |
| `ActorFlow.askWithStatusAndContext` | 컨텍스트 없이 `StatusReply`를 기대하며 ask |
| `ActorSink.actorRef` | 스트림 요소를 신규 액터 API `ActorRef`로 전송 |
| `ActorSink.actorRefWithBackpressure` | 백프레셔와 함께 신규 API `ActorRef`로 전송 |
| `ActorSource.actorRef` | 타입드 메시지를 방출하는 신규 API `ActorRef`로 머티리얼라이즈 |
| `ActorSource.actorRefWithBackpressure` | 백프레셔를 갖는 신규 API `ActorRef`로 머티리얼라이즈 |
| `ask` | 클래식 액터 API 요청-응답을 위한 Ask 패턴 사용 |
| `PubSub.sink` | 방출된 메시지를 `Topic`에 발행 |
| `PubSub.source` | `Topic`을 구독하여 발행된 메시지를 스트리밍 |
| `Sink.actorRef` | 스트림 요소를 클래식 액터 API `ActorRef`로 전송 |
| `Sink.actorRefWithBackpressure` | 백프레셔와 함께 클래식 API `ActorRef`로 전송 |
| `Source.actorRef` | 메시지를 방출하는 클래식 API `ActorRef`로 머티리얼라이즈 |
| `Source.actorRefWithBackpressure` | 백프레셔를 갖는 클래식 API `ActorRef`로 머티리얼라이즈 |
| `watch` | `ActorRef`를 감시하여 종료 시 다운스트림에 실패를 신호 |

### 8.16 압축 연산자(Compression Operators)

| 연산자 | 설명 |
| --- | --- |
| `deflate` | `ByteString` 스트림을 Deflate 압축 |
| `gunzip` | `ByteString` 스트림을 Gzip 압축 해제 |
| `gzip` | `ByteString` 스트림을 Gzip 압축 |
| `inflate` | `ByteString` 스트림을 Deflate 압축 해제 |

### 8.17 에러 처리 연산자(Error Handling)

| 연산자 | 설명 |
| --- | --- |
| `mapError` | 에러로 로깅하지 않고 에러 신호를 변환 |
| `onErrorComplete` | 업스트림 에러 발생 시 스트림을 완료 |
| `recover` | 실패 시 마지막 요소 하나를 다운스트림으로 전송 |
| `recoverWith` | 실패 시 대체 `Source`로 전환 |
| `recoverWithRetries` | 재시도와 함께 실패 시 대체 `Source`로 전환 |
| `RestartFlow.onFailuresWithBackoff` | 실패 시 백오프와 함께 플로우를 재시작 |
| `RestartFlow.withBackoff` | 실패 또는 완료 시 백오프와 함께 플로우를 재시작 |
| `RestartSink.withBackoff` | 실패 또는 완료 시 백오프와 함께 싱크를 재시작 |
| `RestartSource.onFailuresWithBackoff` | 실패 시 백오프와 함께 소스를 재시작 |
| `RestartSource.withBackoff` | 실패 또는 완료 시 백오프와 함께 소스를 재시작 |
| `RetryFlow.withBackoff` | 지수 백오프와 함께 개별 요소를 재시도 |
| `RetryFlow.withBackoffAndContext` | 백오프와 함께 `FlowWithContext` 요소를 재시도 |

---

## 참고 자료

- [Akka 공식 문서](https://doc.akka.io/libraries/akka-core/current/)
