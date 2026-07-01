# Akka Streams 기초

> 원본: https://doc.akka.io/libraries/akka-core/current/stream/index.html

---

## 목차

1. [소개(Introduction)](#1-소개introduction)
2. [퀵스타트 가이드(Quickstart Guide)](#2-퀵스타트-가이드quickstart-guide)
3. [Akka Streams의 설계 원칙(Design Principles)](#3-akka-streams의-설계-원칙design-principles)
4. [기초와 플로우 다루기(Basics and Working with Flows)](#4-기초와-플로우-다루기basics-and-working-with-flows)
5. [그래프 다루기(Working with Graphs)](#5-그래프-다루기working-with-graphs)
6. [모듈성, 합성, 계층 구조(Modularity, Composition and Hierarchy)](#6-모듈성-합성-계층-구조modularity-composition-and-hierarchy)
7. [버퍼와 처리율 다루기(Buffers and Working with Rate)](#7-버퍼와-처리율-다루기buffers-and-working-with-rate)
8. [참고 자료](#참고-자료)

---

## 1. 소개(Introduction)

### 1.1 동기(Motivation)

오늘날의 인터넷에서 우리는 데이터를 스트림(stream) 형태로 소비하는 경우가 점점 더 많아지고 있습니다. 파일을 다운로드하거나 업로드할 때, 또는 피어 투 피어(peer-to-peer) 방식으로 데이터를 주고받을 때가 그 예입니다. 데이터를 한 덩어리 전체(in its entirety)가 아니라 요소(element)들의 스트림으로 바라보는 관점은 매우 유용한데, 이는 컴퓨터가 데이터를 송수신하는 방식(예: TCP를 통한 전송)과 정확히 일치하기 때문입니다.

또한, 처리해야 할 데이터셋이 한 번에 메모리에 올리기에는 너무 큰 경우가 흔하며, 이런 경우 데이터를 클러스터(cluster) 전반에 걸쳐 분산 처리해야 할 수도 있습니다. 그럼에도 데이터를 스트림처럼 다루는 것은 합리적인 접근 방식입니다.

액터(actor)는 메시지 스트림을 처리해 지식(knowledge)이나 정보를 전달하는 데 사용될 수 있지만, 안정적이고 견고한 액터 간 스트리밍을 구현하는 일은 번거롭고 오류가 발생하기 쉽습니다(error-prone). 개발자는 버퍼(buffer)나 메일박스(mailbox)가 넘치지 않도록 막아야 하고, 메시지 유실 가능성을 재전송(retransmission)으로 관리해야 합니다. 데이터가 누락되면 수신된 데이터에 빈틈(gap)이 생기기 때문입니다.

이러한 과제를 해결하기 위해 Akka는 Streams API를 함께 제공합니다. 이 API는 스트림 처리 구성을 직관적이고 안전하게 표현할 수 있게 해주며, 그렇게 표현한 구성을 효율적으로 그리고 제한된(bounded) 자원 사용 범위 내에서 실행할 수 있게 해줍니다. 더 이상 `OutOfMemoryError`를 걱정할 필요가 없습니다. 이를 달성하기 위해 Akka Streams는 백프레셔(back-pressure)를 구현하는데, 이는 Reactive Streams의 핵심 원칙으로서 소비자(consumer)가 처리 속도를 따라가지 못할 때 생산자(producer)가 속도를 늦추도록 보장합니다.

### 1.2 Reactive Streams와의 관계(Relationship with Reactive Streams)

Akka Streams API는 Reactive Streams 인터페이스로부터 완전히 분리(decoupled)되어 있습니다. Akka Streams는 스트림 변환(stream transformation)을 표현(formulate)하는 데 초점을 맞추지만, Reactive Streams는 손실(loss) 없이, 버퍼링이나 자원 고갈(resource exhaustion) 없이 비동기적으로 데이터를 이동시키기 위한 공통 메커니즘(common mechanism)을 정의합니다.

Akka Streams API는 최종 사용자(end-user)를 지향하는 반면, Akka Streams 구현 내부에서는 서로 다른 연산자(operator) 사이로 데이터를 전달하기 위해 Reactive Streams 인터페이스를 사용합니다. Reactive Streams의 주된 목적은 서로 다른 구현체(implementation) 사이의 상호 운용성(interoperability)을 위한 인터페이스를 정의하는 것이지, 최종 사용자용 API를 규정하는 것이 아닙니다.

### 1.3 이 문서를 읽는 방법(How to Read These Docs)

권장되는 학습 순서는 다음과 같습니다.

- 먼저 **퀵스타트 가이드**(Quick Start Guide)부터 시작하십시오.
- 하향식(top-down)으로 학습하고 싶다면 **설계 원칙**(Design Principles)을 검토하십시오.
- 상향식(bottom-up)으로 학습하고 싶다면 **Streams 쿡북**(Cookbook)을 살펴보십시오.
- 내장된 처리 연산자(operator)에 대해서는 연산자 인덱스(operator index)를 참조하십시오.
- 그 외 섹션들은 순서대로 읽거나 필요에 따라 참조하십시오.

### 1.4 모듈 정보(Module Info)

Akka Streams 의존성을 사용하려면 Akka 저장소(repository)에서 제공하는 보안이 적용된 토큰화된(tokenized) URL이 필요합니다. 현재 버전은 2.10.19이며, JDK 11, 17, 21을 지원하고 Scala 2.13.17 및 3.3.7과 호환됩니다.

`akka-stream` 아티팩트를 프로젝트에 추가하면 사용할 수 있으며, sbt, Maven, Gradle용 설정 예시가 공식 문서에 제공됩니다.

---

## 2. 퀵스타트 가이드(Quickstart Guide)

### 2.1 의존성(Dependencies)과 임포트(Imports)

Akka Streams를 사용하려면 해당 모듈을 프로젝트에 추가해야 합니다. 핵심 임포트는 다음과 같습니다.

- Scala: `akka.stream._`, `akka.stream.scaladsl._`
- Java: `akka.stream.*`, `akka.stream.javadsl.*`

추가로 `ActorSystem`, `Done`, `NotUsed`, `ByteString` 등의 유틸리티 임포트가 필요합니다.

### 2.2 소스(Source)로 시작하기

스트림(stream)은 보통 소스(Source)에서 시작합니다. 그래서 Akka Stream을 만드는 첫걸음도 소스를 만드는 것입니다. 가장 단순한 예로 정수 1부터 100까지의 범위를 들 수 있습니다. 이 스트림을 실행하면 결과로 `NotUsed`가 만들어집니다.

```scala
val source: Source[Int, NotUsed] = Source(1 to 100)
```

여기서 타입은 `Source[Int, NotUsed]`입니다. 첫 번째 타입 파라미터 `Int`는 이 소스가 방출(emit)하는 요소(element)의 타입을 나타내고, 두 번째 타입 파라미터 `NotUsed`는 구체화(materialization) 시점에 반환되는 값의 타입(materialized value)을 나타냅니다. 단순한 정수 범위에는 구체화된 값으로 의미 있게 활용할 만한 것이 없으므로 `NotUsed`가 사용됩니다.

소스를 실행하려면 소비 함수(consumer function)를 받아 처리하는 `runForeach()`를 호출합니다. 이 메서드는 `Future[Done]`을 반환합니다.

```scala
source.runForeach(i => println(i))
```

별도의 종료 처리를 하지 않으면 `ActorSystem`은 결코 종료되지 않는다는 점에 유의하십시오.

### 2.3 변환(Transformation)과 재사용 가능한 구성 요소

#### scan을 사용한 누적 계산과 파일 쓰기

`scan` 연산자를 사용하면 스트림 전체에 걸쳐 계산을 수행할 수 있습니다. 1에서 시작하여 들어오는 각 숫자를 차례로 곱해 나가면, 팩토리얼(factorial) 수열이 만들어집니다. 이렇게 생성된 결과는 `FileIO.toPath()`를 통해 파일로 기록할 수 있습니다.

```scala
val factorials = source.scan(BigInt(1))((acc, next) => acc * next)

val result: Future[IOResult] =
  factorials.map(num => ByteString(s"$num\n")).runWith(FileIO.toPath(Paths.get("factorials.txt")))
```

여기서 `runWith(...)`는 주어진 싱크(Sink)로 스트림을 실행하며, 싱크의 구체화된 값(여기서는 `Future[IOResult]`)을 반환합니다.

#### 재사용 가능한 플로우(Flow) 만들기

Akka Streams의 두드러진 특징 중 하나는, 소스(Source)뿐만 아니라 다른 모든 요소도 청사진(blueprint)처럼 재사용할 수 있다는 점입니다. 예를 들어, 문자열을 받아 줄바꿈을 붙여 파일에 쓰는 싱크를 다음과 같이 만들 수 있습니다.

```scala
def lineSink(filename: String): Sink[String, Future[IOResult]] =
  Flow[String].map(s => ByteString(s + "\n")).toMat(FileIO.toPath(Paths.get(filename)))(Keep.right)
```

`toMat(...)`은 플로우와 싱크를 연결하고, 두 번째 인자로 받은 결합 함수(combiner)인 `Keep.right`를 통해 어느 쪽의 구체화된 값을 유지할지 결정합니다. 여기서는 `FileIO.toPath`가 만드는 `Future[IOResult]`(오른쪽)를 유지하므로, 결과 청사진의 타입은 `Sink[String, Future[IOResult]]`가 됩니다.

### 2.4 시간 기반 처리(Time-Based Processing)

두 개의 소스를 결합하는 `zipWith()`와 흐름의 속도를 제어하는 `throttle()`을 사용하면 스트림의 동작을 실제로 체감할 수 있습니다.

```scala
factorials
  .zipWith(Source(0 to 100))((num, idx) => s"$idx! = $num")
  .throttle(1, 1.second)
  .runForeach(println)
```

Akka Streams는 모든 곳에 걸쳐 흐름 제어(flow control)를 암묵적으로 구현하며, 모든 연산자(operator)는 백프레셔(back-pressure)를 존중합니다. 들어오는 처리율(incoming rate)이 초당 한 개보다 높으면, `throttle` 연산자는 상류(upstream)를 향해 백프레셔를 가합니다(assert back-pressure upstream). 이 덕분에 매우 큰 데이터양을 다루더라도 `OutOfMemoryError`를 막을 수 있습니다.

### 2.5 Reactive Tweets 예제

#### 데이터 모델

이 예제는 세 개의 클래스를 사용합니다.

```scala
final case class Author(handle: String)

final case class Hashtag(name: String)

final case class Tweet(author: Author, timestamp: Long, body: String) {
  def hashtags: Set[Hashtag] =
    body
      .split(" ")
      .collect {
        case t if t.startsWith("#") => Hashtag(t.replaceAll("[^#\\w]", ""))
      }
      .toSet
}

val akkaTag = Hashtag("#akka")
```

- `Author`는 핸들(handle) 문자열 필드를 가집니다.
- `Hashtag`는 이름(name) 문자열 필드를 가집니다.
- `Tweet`은 작성자(author), 타임스탬프(timestamp), 본문(body)을 가지며, 본문 텍스트에서 해시태그(hashtag)를 추출하는 `hashtags` 메서드를 포함합니다.

#### 필터링(Filtering)과 매핑(Mapping)

기본적인 스트림 연산은 컬렉션과 유사한 문법을 따릅니다. 예를 들어 `#akka` 해시태그를 포함하는 트윗을 필터링하고, 거기서 `Author` 객체를 추출하도록 매핑할 수 있습니다. 이러한 연산은 Scala 컬렉션 라이브러리를 사용해 본 사람에게는 익숙하게 보이지만, 컬렉션이 아니라 스트림 위에서 동작한다는 점이 다릅니다.

#### mapConcat을 사용한 평탄화(Flattening)

1-대-다(one-to-many) 관계를 다룰 때는 `mapConcat`을 사용합니다. `flatMap`이라는 이름은 for-컴프리헨션(for-comprehension)이나 모나딕 합성(monadic composition)과 너무 가까워 혼동을 줄 수 있기에 의도적으로 피했습니다. 대신 `mapConcat`은 각 트윗을 해당 트윗의 해시태그 목록으로 변환하여, 평탄화된(flattened) 스트림을 생성합니다.

### 2.6 그래프 기반 패턴(Graph-Based Patterns)

#### 브로드캐스트(Broadcast)

팬 아웃(fan-out) 시나리오에서는, 이러한 팬 아웃 구조를 형성하는 데 사용되는 요소들을 Akka Streams에서 정션(junction)이라고 부릅니다. `Broadcast` 요소는 자신의 입력 포트로 들어온 요소들을 모든 출력 포트로 방출합니다.

비선형적이고 복잡한 스트림 구조에는 `GraphDSL`이 가장 편리한 API를 제공합니다. 소스를 `Broadcast`를 통해 여러 싱크에 연결할 때는 엣지(edge) 연산자 `~>`를 사용합니다.

### 2.7 백프레셔 처리(Back-Pressure Handling)

구독자(subscriber)가 발행자(publisher)의 속도를 따라가지 못하는 상황이 발생할 수 있습니다. Akka Streams는 이런 시나리오에서 어떤 일이 일어나야 할지를 제어할 수 있도록 내부 백프레셔 신호(internal backpressure signal)에 의존합니다.

`buffer` 연산자는 `dropHead` 같은 명시적인 오버플로 전략(overflow strategy)을 제공합니다. 버퍼링은 명시적으로(explicitly) 처리할 수 있고, 또 반드시 명시적으로 처리해야 한다는 점이 강조됩니다. 메모리 문제를 일으킬 수 있는 암묵적인 큐잉(implicit queuing)에 의존해서는 안 됩니다.

### 2.8 구체화된 값(Materialized Values)

#### 구체화(Materialization) 이해하기

`Source`, `Flow`, `Sink`에 붙은 타입 파라미터(이른바 Mat 타입)는 이들 처리 구성 요소가 구체화될 때 반환하는 값의 타입을 나타냅니다. 소스를 만든다는 것은 "처음 100개의 자연수를 어떻게 방출할지에 대한 기술(description)"을 갖게 됨을 의미할 뿐, 이 소스 자체는 아직 활성화(active)된 상태가 아닙니다.

#### 구체화된 값 결합하기

`toMat()`과 `Keep` 결합 함수를 사용해 파이프라인 내 여러 구성 요소가 만들어 내는 결과를 결합할 수 있습니다. `Keep.right()`를 사용하면 가장 오른쪽 연산자의 구체화 타입을 유지하므로, 결과 청사진은 `Sink[String, Future[IOResult]]`가 됩니다.

#### RunnableGraph

`RunnableGraph`는 실행할 준비가 된, 완전히 연결된(fully connected) 그래프를 나타냅니다. `RunnableGraph[T]`에 대해 `run()`을 호출한 결과값의 타입은 `T`입니다. 이를 통해 유한(finite) 스트림에서 요소 개수 같은 결과를 얻을 수 있습니다.

#### 재사용 가능한 청사진(Reusable Blueprints)

`RunnableGraph`는 단지 스트림의 청사진이기 때문에 여러 번 재사용하고 구체화할 수 있습니다. 즉, 동일한 그래프를 서로 다른 데이터 배치(batch)에 적용해 각기 다른 결과를 얻을 수 있습니다.

### 2.9 핵심 개념 요약

퀵스타트는 다음과 같은 기초 패턴을 확립합니다. 소스(Source)는 요소를 방출하고, 플로우(Flow)는 이를 변환하며, 싱크(Sink)는 이를 소비합니다. 그리고 모든 구성 요소는 백프레셔를 존중합니다. 청사진(blueprint) 기술과 실행(execution)을 구체화(materialization)를 통해 분리함으로써, 단순한 선형 파이프라인부터 복잡한 그래프 토폴로지까지 모두에 적합한 재사용 가능하고 합성 가능한(composable) 스트림 정의가 가능해집니다.

---

## 3. Akka Streams의 설계 원칙(Design Principles)

### 3.1 핵심 철학(Core Philosophy)

Akka Streams는 단순히 사용하기 쉬운 것보다는, 최소하고 일관된 API(minimal and consistent APIs)를 강조합니다. 이 프레임워크는 "마법(magic)"보다 "명시성(explicitness)"을 우선시하여, 예외 없이 신뢰성 있게 동작하는 기능을 보장합니다.

세 가지 근본 원칙은 다음과 같습니다.

1. **모든 기능은 API에 명시적으로 드러난다. 마법은 없다(no magic).**
2. **최상의 합성성(Supreme compositionality): 결합된 조각들은 각 부분의 기능을 그대로 유지한다.**
3. **분산된 제한된 스트림 처리(distributed bounded stream processing)라는 도메인의 완전한 모델(exhaustive model).**

### 3.2 사용자가 기대해야 하는 것(What Users Should Expect)

사용자는 어떠한 스트림 처리 토폴로지(topology)든 표현할 수 있는 도구를 제공받으며, 동시에 백프레셔(back-pressure), 버퍼링(buffering), 변환(transformation), 실패 복구(failure recovery) 같은 본질적인 측면들을 모델링할 수 있습니다. 그리고 구축된 모든 구성물은 더 큰 맥락(larger context) 안에서 재사용 가능한 상태로 남아 있습니다.

#### 요소 처리 보장(Element Processing Guarantees)

Akka Streams는 토폴로지를 통해 전송된 모든 객체가 처리될 것이라고 보장할 수 없습니다. 요소(element)는 여러 이유로 드롭(drop)될 수 있습니다.

- `map(…)` 같은 연산자 안의 사용자 코드가 전혀 다른 출력을 만들어 낼 수 있습니다.
- 일반적인 연산자들이 의도적으로 요소를 드롭합니다: `take` / `drop` / `filter` / `conflate` / `buffer`.
- 스트림 실패(failure)는 처리 중인(in-flight) 요소를 기다리지 않고 폐기합니다.
- 스트림 취소(cancellation)는 상류로 전파되어 처리 단계들을 종료시킵니다.

따라서 정리(cleanup)가 필요한 객체의 마무리(finalization)는 Akka Streams 기능 바깥에서 사용자가 직접 처리해야 합니다.

#### 구현 아키텍처(Implementation Architecture)

합성성을 위해서는 재사용 가능한 부분 토폴로지(partial topology)가 필요합니다. Akka는 데이터 플로우를 불변(immutable)의 그래프 청사진(graph blueprint)으로 표현하는 "리프티드 접근법(lifted approach)"을 사용하며, 이를 명시적으로 구체화(materialize)할 때 비로소 처리가 시작됩니다. 구체화는 엔진(engine)과 상호작용하기 위한 특정 객체를 생성하는데, 이 객체를 "그래프의 구체화된 값(materialized value of a graph)"이라고 부릅니다.

### 3.3 Reactive Streams와의 상호 운용성(Interoperation with Reactive Streams)

Akka는 Reactive Streams 명세(specification)를 완전히 구현합니다. 다만, Reactive Streams 인터페이스를 사용자 수준의 API와 의도적으로 분리하여, 이를 최종 사용자용 도구가 아니라 구현 세부 사항(implementation detail)으로 취급합니다.

`Publisher`나 `Subscriber` 인스턴스를 얻으려면 다음을 사용합니다.

- `Sink.asPublisher` — Sink로부터 Publisher를 얻습니다.
- `Source.asSubscriber` — Source로부터 Subscriber를 얻습니다.

#### 단일 구독자 제약(Single Subscriber Restriction)

기본 Akka 구체화는 단일 구독자(single-subscriber) `Processor`를 생성합니다. 추가 구독자는 거부되는데, 이는 DSL 토폴로지가 모든 팬 아웃(fan-out)을 `Broadcast<T>` 같은 명시적 요소를 통해 처리하기 때문입니다.

다른 Reactive Streams 구현체와 브로드캐스트 상호 운용성이 필요한 경우에는 `Sink.asPublisher(true)`를 사용하십시오.

#### 왜 Reactive Streams 인터페이스와 분리하는가?

Reactive Streams는 서비스 제공자 인터페이스(Service Provider Interface, SPI)로 기능합니다. 즉, 상호 운용 가능한 라이브러리들을 위한 내부 인프라(internal infrastructure)입니다. 이 타입들을 최종 사용자에게 노출하면 내부 구현 세부 사항을 의도치 않게 유출(leak)하게 됩니다.

`Source`, `Sink`, `Flow`는 유창한(fluent) DSL과 구체화 팩토리(materialization factory)를 제공하는 반면, 그에 대응하는 Reactive Streams 타입(`Publisher`, `Subscriber`, `Processor`)은 더 낮은 수준(lower level)에서 동작합니다. 이렇게 분리하면 구체화 시점에 퓨징(fusing)이나 디스패처(dispatcher) 설정과 같은 최적화를 적용할 수 있습니다.

또한 Java 9부터는 `java.util.concurrent.Flow.Subscriber`가 포함되었기 때문에, Reactive Streams 타입을 직접 상속(extend)하는 라이브러리는 마이그레이션 문제에 직면합니다. Akka는 두 가지 모두를 투명하게(transparently) 지원합니다.

Akka는 구현하기 어려운 Reactive Streams 조각들 대신, 더 단순한 추상화인 `GraphStage`와 연산자(operator)를 사용하도록 권장합니다. 상호 운용은 `Sink.asPublisher`나 `Source.asSubscriber` 같은 메서드를 통해 여전히 사용할 수 있습니다.

### 3.4 스트리밍 라이브러리가 제공해야 하는 것(What Streaming Libraries Should Provide)

Akka Streams 위에 구축되는 라이브러리는 다음 원칙을 따라야 합니다.

1. **재사용 가능한 연산자(reusable operator)를 노출하라.** 합성 가능한 조각을 반환하는 팩토리(factory) 형태로 제공하여 완전한 합성성을 가능하게 해야 합니다.
2. **선택적으로 구체화 편의 기능(materialization facilities)을 제공하라.** 편리한 소비를 위해 제공할 수 있습니다.

첫 번째 규칙은 합성성이 파괴되는 것을 방지합니다. 만약 라이브러리가 이미 구체화된(pre-materialized) 연산자만 받아들인다면, 여러 라이브러리를 결합하는 것이 불가능해집니다. 라이브러리 기능은 자원 바인딩(resource binding)을 구체화 시점까지 미뤄야 하며, 그 시점은 사용자가 제어합니다.

두 번째 규칙은 편의용(convenience) API를 허용합니다. 예를 들어 Akka HTTP의 `handleWith` 메서드처럼, 흔한 시나리오를 위한 편의 메서드를 제공할 수 있습니다.

#### 살아 있는 자원의 처리(Live Resource Handling)

재사용 가능한 플로우 기술(flow description)은 "살아 있는(live)" 자원(이미 존재하는 TCP 연결이나 활성화된 멀티캐스트 Publisher 등)에 바인딩되어서는 안 됩니다. 자원 할당(resource allocation)은 반드시 구체화 시점까지 미뤄져야 합니다. `TickSource`는 타이머가 구체화 시점에만 초기화된다면 "살아 있지 않은(non-live)" 자원으로 간주됩니다.

이에 대한 예외는 신중한 정당화(justification)와 문서화(documentation)를 필요로 합니다.

#### 라이브러리를 위한 구성 요소(Building Blocks for Libraries)

기초가 되는 요소들은 다음과 같습니다.

- **Source**: 정확히 하나의 출력 스트림(output stream).
- **Sink**: 정확히 하나의 입력 스트림(input stream).
- **Flow**: 정확히 하나의 입력과 하나의 출력 스트림.
- **BidiFlow**: 두 개의 입력과 두 개의 출력 스트림을 가지며, 서로 반대 방향으로 동작하는 두 개의 Flow처럼 동작합니다.
- **Graph**: 입력/출력 포트를 노출하는, 패키징된 토폴로지(packaged topology)로서 `Shape` 객체로 특징지어집니다.

스트림을 방출하는 스트림(streams emitting streams)도 여전히 평범한 `Source`로 남아 있습니다. 요소의 타입이 무엇이든 정적인(static) 토폴로지 표현에는 영향을 주지 않습니다.

### 3.5 오류(Error) 대 실패(Failure)

Reactive Manifesto를 따르면, 오류(error)는 스트림 데이터 요소(data element)이고, 실패(failure)는 스트림 자체가 붕괴(collapse)되는 것을 의미합니다.

Reactive Streams 인프라에서: `onNext`는 데이터(오류 포함)를 신호하고, `onError`는 실패를 신호합니다.

> **용어에 대한 주석**: 메서드 이름 `onError`는 역사적인 이유(historical)로 붙여진 것입니다. 이는 구독자(Subscriber)에게 "실패(failure)"를 신호하는데, 이는 고수준의 붕괴(high-level collapse)를 스트림 인프라가 낮은 수준에서 표현한 것입니다.

#### 제한적인 오류 지원(Limited Error Support)

Akka Streams는 데이터 변환 연산자에 비해 `onError` 처리를 제한적으로 제공합니다. 이는 의도적인 것인데, `onError`가 스트림의 붕괴를 신호하기 때문입니다. 변환 연산자(transformation operator)는 스트림과 함께 붕괴되며, 버퍼링된 요소가 유실될 수 있습니다.

실패(failure)는 데이터 요소(data element)보다 더 빠르게 전파됩니다. 이는 교착(deadlock)되거나 넘쳐버릴(overflow) 수 있는 백프레셔 상태의 스트림을 해체(tear down)하는 데 필수적입니다.

#### 스트림 복구 시맨틱(Stream Recovery Semantics)

복구(recovery) 요소는 `onError` 신호를 흡수하여, 이를 데이터 요소(data element)로 변환한 뒤 정상적인 완료(normal completion)로 이어지게 합니다. 복구는 일종의 격벽(bulkhead) 역할을 하여, 붕괴를 토폴로지의 특정 영역(specific region)에 가둡니다.

붕괴된 영역 안에서는 버퍼링된 요소가 유실될 수 있지만, 외부 영역(external region)은 영향을 받지 않은 채로 남아 있습니다. 이는 `try`-`catch`의 동작과 유사한데, 명령문(statement)의 위치가 예외 영역(exception region) 안에서 어디까지 건너뛸지(skip)를 결정하는 것과 같은 원리입니다.

---

## 4. 기초와 플로우 다루기(Basics and Working with Flows)

### 4.1 핵심 개념(Core Concepts)

Akka Streams는 제한된 버퍼 공간(bounded buffer space)을 사용하여 요소(element)의 시퀀스(sequence)를 처리하는 라이브러리입니다. 핵심 용어는 다음과 같습니다.

- **스트림(Stream)**: 데이터를 이동하고 변환하는 일을 포함하는 활성 프로세스(active process).
- **요소(Element)**: 스트림의 처리 단위(processing unit). 모든 연산은 요소를 상류(upstream)에서 하류(downstream)로 변환하고 전달합니다.
- **백프레셔(Back-pressure)**: 흐름 제어(flow-control)의 수단으로, 데이터의 소비자가 생산자에게 자신의 현재 가용성(availability)을 알리는 방법입니다. 이를 통해 상류 생산자의 속도를 효과적으로 늦추어 소비 속도에 맞춥니다.
- **논블로킹(Non-Blocking)**: 어떤 연산이, 요청된 작업이 완료되기까지 오랜 시간이 걸리더라도, 호출한 스레드(calling thread)의 진행을 방해하지 않는 것.
- **그래프(Graph)**: 스트림 처리 토폴로지(topology)의 기술(description)로서, 스트림이 실행될 때 요소가 흘러갈 경로(pathway)를 정의합니다.
- **연산자(Operator)**: 그래프를 구성하는 모든 빌딩 블록(building block)에 대한 공통 명칭. 예로는 `map()`, `filter()`, `GraphStage`를 확장한 사용자 정의 연산자, 그리고 `Merge`나 `Broadcast` 같은 그래프 정션(junction)이 있습니다.

### 4.2 스트림 정의와 실행(Defining and Running Streams)

선형 파이프라인(linear pipeline)을 위한 핵심 추상화는 다음과 같습니다.

- **Source**: 정확히 하나의 출력을 가진 연산자로, 하류 연산자가 받을 준비가 되었을 때마다 데이터 요소를 방출합니다.
- **Sink**: 정확히 하나의 입력을 가진 연산자로, 데이터 요소를 요청하고 받아들이며, 필요하다면 상류 생산자의 속도를 늦출 수 있습니다.
- **Flow**: 정확히 하나의 입력과 하나의 출력을 가진 연산자로, 흐르는 데이터 요소를 변환함으로써 상류와 하류를 연결합니다.
- **RunnableGraph**: 양쪽 끝이 각각 Source와 Sink에 "부착(attached)"되어 `run()`을 호출할 준비가 된 Flow.

소스, 싱크, 그리고 여러 연산자를 모두 연결해 `RunnableGraph`를 구성한 이후라도, 구체화(materialize)되기 전까지는 어떤 데이터도 흐르지 않습니다.

### 4.3 Source, Sink, Flow 정의하기

자주 사용되는 구성물의 예는 다음과 같습니다.

- `Source(List(...))` — 이터러블(iterable)로부터 소스를 생성
- `Source.future(...)` — 퓨처(Future)로부터 소스를 생성
- `Source.single(...)` — 단일 요소 소스
- `Source.empty` — 빈 소스
- `Sink.fold[Int, Int](0)(_ + _)` — 누적(accumulation) 싱크
- `Sink.head` — 첫 번째 요소 추출
- `Sink.ignore` — 모든 요소를 폐기
- `Sink.foreach[String](println(_))` — 부수 효과(side-effect) 실행

배선(wiring) 예시는 `.via()`, `.to()`, `.alsoTo()` 메서드를 사용해 소스를 플로우를 거쳐 싱크에 연결하는 것을 보여줍니다.

### 4.4 허용되지 않는 스트림 요소(Illegal Stream Elements)

Akka Streams는 `null`을 스트림의 요소로 전달하는 것을 허용하지 않습니다. 권장되는 대안으로는 `scala.Option`, `scala.util.Either`, 또는 `java.util.Optional`이 있습니다.

### 4.5 백프레셔 설명(Back-Pressure Explained)

Akka는 Reactive Streams 명세에 의해 표준화된 비동기 논블로킹 백프레셔 프로토콜(asynchronous non-blocking back-pressure protocol)을 구현합니다. 이 프로토콜은 수요(demand) 신호를 통해 동작합니다.

소스(source)는 주어진 어떤 구독자(Subscriber)에 대해서도, 수신한 총 수요(total demand)보다 더 많은 요소를 결코 방출하지 않을 것을 보장합니다.

#### 느린 발행자, 빠른 구독자(Slow Publisher, Fast Subscriber)

이 시나리오에서 구독자는 `Request(n)` 메시지를 비동기적으로 신호합니다. 발행자(Publisher)는 들어오는 요소를 발행하면서 결코 기다릴(백프레셔를 받을) 필요가 없으며, 푸시 모드(push-mode)로 동작합니다.

#### 빠른 발행자, 느린 구독자(Fast Publisher, Slow Subscriber)

이 경우 발행자는 다음과 같은 전략을 적용해야 합니다.

- 생산 속도를 제어할 수 있다면 요소를 생성하지 않는다.
- 더 많은 수요가 신호될 때까지 요소를 제한된(bounded) 방식으로 버퍼링하려고 시도한다.
- 더 많은 수요가 신호될 때까지 요소를 드롭(drop)한다.
- 위 어느 전략도 적용할 수 없다면 스트림을 해체(tear down)한다.

### 4.6 스트림 구체화(Stream Materialization)

구체화(materialization)는 스트림 기술(RunnableGraph)을 받아 실행에 필요한 모든 자원을 할당(allocate)하는 과정입니다. 일반적으로 처리를 구동하는 액터(Actor)를 시작하는 것을 포함하지만, 파일이나 소켓 연결(socket connection)을 여는 것을 의미할 수도 있습니다.

구체화를 유발하는 종단 연산(terminal operation)에는 `run()`, `runWith()`, 그리고 `runForeach(...)` 같은 편의 메서드가 있습니다.

#### 연산자 퓨징(Operator Fusion)

기본적으로 Akka Streams는 스트림 연산자들을 퓨징(fuse)합니다. 이는 플로우나 스트림의 처리 단계들이 동일한 액터(Actor) 내에서 실행될 수 있음을 의미합니다. 그 결과 요소 전달이 더 빨라지지만, 퓨징된 영역(fused region)당 하나의 CPU 코어만 사용하게 됩니다.

병렬 처리(parallel processing)를 위해서는 비동기 경계(asynchronous boundary)를 삽입해야 하며, `.async` 메서드를 통해 추가할 수 있습니다.

#### 구체화된 값 결합하기(Combining Materialized Values)

`toMat()`이나 `viaMat()` 같은 메서드를 사용하면 구체화된 값을 합성(compose)할 수 있습니다. `Keep` 객체는 다음과 같은 편의 함수를 제공합니다.

- `Keep.left` — 왼쪽(left-side)의 구체화된 값을 유지
- `Keep.right` — 오른쪽(right-side)의 구체화된 값을 유지
- `Keep.both` — 두 값을 튜플(tuple)로 결합

더 복잡한 조합은 `mapMaterializedValue()`를 사용해 중첩된 결과를 변환할 수 있습니다.

### 4.7 소스 사전 구체화(Source Pre-materialization)

`preMaterialize()` 연산자는 소스를 스트림 그래프에 부착하기 전에 그 소스의 구체화된 값을 미리 얻을 수 있게 해줍니다. 이는 `Source.queue`, `Source.actorRef`, `Source.maybe`처럼 구체화된 값으로 구동되는(materialized value powered) 소스에서 유용합니다.

### 4.8 스트림 순서(Stream Ordering)

거의 모든 계산 연산자(computation operator)는 요소의 입력 순서(input order)를 보존합니다. 입력 {IA1, IA2, ...}이 출력 {OA1, OA2, ...}을 유발하고, 이것이 출력 {OB1, OB2, ...}을 유발하는 입력 {IB1, IB2, ...}보다 먼저 발생한다면, 출력 OAi는 OBi보다 앞서게 됩니다.

`mapAsync` 같은 비동기 연산은 순서를 유지하는 반면, `mapAsyncUnordered`는 그렇지 않습니다. `Merge` 같은 팬 인(fan-in) 연산은 서로 다른 입력 포트 간의 출력 순서가 정의되어 있지 않지만(no defined order), `Zip`은 순서를 보장합니다.

### 4.9 ActorMaterializer 생명주기(Lifecycle)

`Materializer`는 스트림 청사진의 실행을 관장합니다. `ActorSystem` 전체 범위(system-wide)의 구체화기는 `SystemMaterializer` 익스텐션(extension)을 통해 제공됩니다.

액터 내부에서 구체화기를 생성하면, 스트림의 생명주기가 액터의 생명주기에 묶입니다. 즉, 액터가 멈추면 스트림도 종료됩니다.

시스템 범위(system-scoped)의 구체화기를 명시적으로 전달하면, 스트림이 그것을 생성한 액터보다 더 오래 살아남게(outlive) 할 수 있습니다. 액터 내부에서 새로운 구체화기를 직접 생성하면 자원 누수(resource leak) 위험이 있습니다. 대신 구체화기를 주입받거나 액터 컨텍스트(actor context)를 통해 생성하십시오.

---

## 5. 그래프 다루기(Working with Graphs)

### 5.1 개요(Overview)

Akka Streams에서 "그래프 다루기"는 `GraphDSL`을 사용해 비선형(non-linear) 스트림 토폴로지를 구성하는 방법을 다룹니다. 팬 인(fan-in)과 팬 아웃(fan-out) 연산이 필요한 계산 그래프(computation graph)를 만드는 데 초점이 맞춰져 있습니다.

### 5.2 그래프 구성의 기초(Graph Construction Fundamentals)

그래프는 선형(linear) Flow와 달리, 그래프 설계를 화이트보드(whiteboard)에 그린 그림처럼 보이도록 설계된 전용 DSL을 사용합니다. 그래프는 그래프 내의 선형 연결(linear connection) 역할을 하는 단순한 Flow와, 팬 인 및 팬 아웃 지점 역할을 하는 정션(junction)으로 구성됩니다.

`GraphDSL`은 요소를 연결하기 위해 `~>` 연산자(그리고 그 반대 방향인 `<~`)를 사용합니다. 중요한 설계상의 특징은, `GraphDSL.Builder` 객체는 가변(mutable)이지만, `GraphDSL` 인스턴스 자체는 불변(immutable)이고 스레드 안전(thread-safe)하며 자유롭게 공유(shareable) 가능하다는 점입니다.

### 5.3 사용 가능한 정션(Available Junctions)

#### 팬 아웃 연산자(Fan-out: 1 입력, N 출력)

- **Broadcast[T]**: 입력 요소를 각 출력으로 방출합니다.
- **Balance[T]**: 요소를 (사용 가능한) 하나의 출력 포트로 분배합니다.
- **Partition[T]**: 분할 함수(partition function)에 따라 요소를 라우팅합니다.
- **UnzipWith**: 요소를 N개의 출력으로 분할합니다.
- **UnZip[A, B]**: 튜플(tuple) 스트림을 두 개의 별도 스트림으로 분할합니다.

#### 팬 인 연산자(Fan-in: N 입력, 1 출력)

- **Merge[In]**: 여러 입력 중에서 무작위로(randomly) 선택합니다.
- **MergePreferred[In]**: 선호 포트(preferred port)를 우선시합니다.
- **MergePrioritized[In]**: 우선순위(priority) 기반으로 선택합니다.
- **MergeLatest[In]**: 갱신된 값들로 구성된 `List`를 방출합니다.
- **ZipWith / Zip**: 여러 입력을 하나의 출력으로 결합합니다.
- **Concat[A]**: 두 스트림을 순차적으로(sequentially) 이어 붙입니다.

### 5.4 부분 그래프(Partial Graphs)

부분 그래프는 `ClosedShape` 이외의 셰이프(shape)를 반환하여 모듈식(modular) 구성을 가능하게 합니다. 핵심 사항은 다음과 같습니다.

- `SourceShape`는 정확히 하나의 출력을 가진 부분 그래프를 나타냅니다.
- `SinkShape`는 정확히 하나의 입력을 가진 부분 그래프를 나타냅니다.
- `FlowShape`는 하나의 입력과 하나의 출력을 가집니다.
- 부분 그래프는 모든 포트가 연결되어 있거나, 아니면 반환되는 셰이프의 일부인지(즉 외부로 노출되는 포트인지)를 검증합니다.

### 5.5 그래프로부터 Source, Sink, Flow 생성하기

복잡한 그래프 구조는 `Source.fromGraph`, `Sink.fromGraph`, `Flow.fromGraph`를 사용해 단순한 연산자로 캡슐화(encapsulate)할 수 있습니다. 이 메서드들은 적절한 셰이프를 가진 그래프를 받아, 이를 표준 스트림 요소로 노출합니다.

### 5.6 단순화된 결합 API(Simplified Combination API)

흔한 경우를 위해, `Source.combine()`이나 `Sink.combine()` 같은 메서드는 명시적인 `GraphDSL` 구성을 불필요하게 만듭니다. 이 메서드들은 내부의 그래프 배선(wiring)을 자동으로 처리합니다.

### 5.7 재사용 가능한 컴포넌트(Reusable Components)

셰이프 클래스를 확장하거나 구현하여 사용자 정의 셰이프(custom shape)를 만들 수 있습니다. 공식 문서의 예제는 두 개의 입력과 하나의 출력을 가진 `PriorityWorkerPoolShape`를 보여주며, 이는 복잡한 내부 배선을 캡슐화하면서도 깔끔한 인터페이스를 제공하는 도메인 특화(domain-specific) 정션을 만드는 방법을 설명합니다.

### 5.8 양방향 플로우(Bidirectional Flows)

`BidiFlow` 타입은 서로 반대 방향으로 배치된 두 개의 입력과 두 개의 출력을 가진 그래프를 나타냅니다. 이는 나가는(outgoing) 메시지를 직렬화(serialize)하고 들어오는(incoming) 옥텟 스트림(octet stream)을 역직렬화(deserialize)하는 코덱(codec) 연산자에 유용합니다. `BidiFlow`는 단순한 변환의 경우 `BidiFlow.fromFunctions()`를 사용해 생성하거나, 복잡한 로직의 경우 `GraphDSL`을 통해 구성할 수 있습니다.

### 5.9 그래프 내부에서 구체화된 값 접근하기

`builder.materializedValue` 아웃렛(outlet)은 그래프 구성 내부에서 구체화된 값(materialized value)을 피드백(feedback)할 수 있게 해줍니다.

> **중요한 경고**: 구체화된 값이 실제로 자기 자신의 구체화된 값에 기여하는(contribute) 사이클(cycle)을 만들지 않도록 주의하십시오.

### 5.10 그래프 사이클과 교착(Graph Cycles and Deadlocks)

공식 문서는 라이브니스(liveness) 문제를 일으키는 세 가지 주요 패턴을 식별합니다.

1. **불균형한 주입/추출(Unbalanced injection/extraction)**: 사이클이 잃는 요소보다 더 많은 요소를 얻게 되면, 결국 버퍼가 완전히 가득 차서 교착(deadlock)이 발생합니다.

2. **요소 기아(Element starvation)**: `MergePreferred`를 사용하면 교착은 막을 수 있지만, 소스 소비(source consumption)가 기아 상태(starve)에 빠질 수 있습니다.

3. **초기화 교착(Initialization deadlocks)**: `ZipWith` 같은 연산을 사용하는 균형 잡힌(balanced) 사이클은 "닭과 달걀(chicken-and-egg)" 문제를 일으킬 수 있습니다. 초기 요소를 처리하기 위해 초기 요소가 필요한 상황입니다.

제시된 해결책은 다음과 같습니다.

- 피드백 아크(feedback arc)에 `OverflowStrategy.dropHead` 같은 드롭(dropping) 전략을 사용하기.
- 요소 평형(element equilibrium)을 유지하는 균형 잡힌 연산을 사용하기.
- `Source.single()`과 `Concat`을 통해 초기 "킥오프(kick-off)" 요소를 주입하기.

### 5.11 구현 세부 사항(Implementation Details)

`GraphDSL`은 모든 요소가 제대로 연결되었는지에 대한 컴파일 타임(compile time) 타입 안전성을 제공할 수 없습니다. 이 검증(validation)은 그래프 인스턴스화(instantiation) 시점에 런타임 검사(runtime check)로 수행됩니다.

정션의 참조 동등성(reference equality)이 그래프 노드의 동등성을 결정합니다. 즉, 동일한 정션 인스턴스는 결과 그래프에서 동일한 위치(location)에 대응합니다.

---

## 6. 모듈성, 합성, 계층 구조(Modularity, Composition and Hierarchy)

### 6.1 핵심 개념(Core Concepts)

Akka 문서는 스트림 처리를 "포트가 있는 상자(box with ports)"라는 멘탈 모델(mental model)로 제시합니다. Akka Streams에서 사용되는 모든 연산자는, 처리할 요소가 도착하고 떠나는 입력 포트(input port)와 출력 포트(output port)를 가진 하나의 "상자(box)"로 상상할 수 있습니다.

#### 연산자 타입과 셰이프(Operator Types and Shapes)

프레임워크는 연산자를 서로 다른 셰이프(shape)로 분류합니다.

- **선형 연산자(Linear operator)** (Source, Sink, Flow)는 엄격한 체인(strict chain)을 형성합니다.
- **팬 인 / 팬 아웃 연산자**는 복잡하고 비선형적인 레이아웃(layout)을 가능하게 합니다.
- **BidiFlow** 연산자는 양방향 입출력을 처리하며, 프로토콜 계층화(protocol layering)에 유용합니다.

Source는 단 하나의 출력 포트를 가진 "상자"에 지나지 않으며, BidiFlow는 정확히 두 개의 입력 포트와 두 개의 출력 포트를 가진 "상자"입니다.

### 6.2 합성과 모듈성의 기초(Basics of Composition and Modularity)

모듈성(modularity)은 복잡한 그래프가 재사용 가능한 컴포넌트로 패키징될 때 나타납니다. 기본 연산자만으로도 복잡한 처리 네트워크(processing network)를 만드는 것은 가능하지만, 그것만으로는 모듈성을 구현하지는 못합니다.

#### 복합 연산자 만들기(Creating Composite Operators)

유창한 DSL(`named()`이나 `withAttributes()` 사용)은 캡슐화(encapsulation)를 가능하게 합니다. 선형 연산자를 코드에서 연결할 때, 개발자는 경계(boundary)를 명시적으로 선언해야 합니다. 예제는 연산을 체이닝(chaining)할 때 현재의 Source를 감싸서(wrap up) 이름을 부여하기 위해 `named()` 메서드가 필요함을 보여줍니다.

핵심 원칙: 이러한 재사용 가능한 컴포넌트들은 이미 복잡한 처리 네트워크의 생성을 가능하게 합니다. 복합체(composite)는 내부를 숨기면서(hide internals) 필요한 포트만 외부에 노출합니다.

### 6.3 복잡한 시스템 합성하기(Composing Complex Systems)

#### 비선형 레이아웃을 위한 GraphDSL

`GraphDSL`은 복잡한 그래프를 위한 고급 합성(advanced composition)을 제공합니다. 두 DSL(선형 DSL과 `GraphDSL`)의 차이는 표면적인 것일 뿐이며, 다루는 개념(concept)은 모든 DSL에 걸쳐 동일합니다(uniform).

복잡한 레이아웃에는 `RunnableGraph.fromGraph()`와 `GraphDSL.create()` 메서드를 사용하여 팬 인, 팬 아웃, 사이클(cycle)을 갖춘 정교한 처리 네트워크를 구축할 수 있습니다.

#### 셰이프 정의(Shape Definition)

재사용 가능한 그래프 컴포넌트를 만들 때, 개발자는 노출되는 포트를 `Shape`를 통해 명시합니다. 모든 연산자(Source, BidiFlow 등을 포함하여)는 셰이프(shape)를 가지며, 이 셰이프는 모듈의 타입이 지정된 포트(typed port)들을 인코딩(encode)합니다.

내장 셰이프(built-in shape)에는 `SourceShape`, `FlowShape`, `SinkShape`, `BidiShape`가 있습니다.

### 6.4 구체화된 값(Materialized Values)

#### 합성을 통한 전파(Propagation Through Composition)

구체화된 값은 실행 중인 스트림 네트워크와 상호작용할 수 있는 능력(interaction capability)을 나타냅니다. 각 구체화(materialization)는 제공된 `RunnableGraph`에 인코딩된 청사진에 대응하는, 새로운 실행 중인 네트워크(running network)를 생성합니다.

액터의 `Props`와 달리, 스트림에서는 각 연산자가 구체화된 값을 제공할 수 있습니다. 따라서 여러 연산자나 모듈을 합성할 때는 그 구체화된 값들도 함께 결합(combine)해야 합니다.

#### 결합 함수(Combiner Functions)

`viaMat()`, `toMat()` 같은 메서드는 결합 함수(`Keep.left`, `Keep.right`, `Keep.both`)를 받아서, 어떤 구체화된 값이 상위로(upward) 전파될지를 결정합니다. 사용자 정의 결합 함수(custom combiner)는 여러 값을 단일 결과(single result)로 변환할 수도 있습니다.

### 6.5 어트리뷰트(Attributes)

#### 정의와 상속(Definition and Inheritance)

어트리뷰트(Attribute)는 구체화되는 엔티티(materialized entity)의 여러 측면을 세밀하게 조정(fine-tune)합니다. 어트리뷰트는 중첩된 모듈(nested module)에 상속(inherit)되며, 해당 모듈이 사용자 정의 값으로 재정의(override)하지 않는 한 그대로 적용됩니다.

#### 일반적인 어트리뷰트(Common Attributes)

- **name**: `named()` 단축 메서드를 통해 생성됩니다.
- **inputBuffer**: 비동기 경계(asynchronous boundary)의 버퍼 크기를 제어합니다.

그래프에서 연산자에 가장 가깝게(closest) 지정된 어트리뷰트가 그 연산자에 대해 실제로 적용되는 값입니다. 중첩 모듈은 부모(parent)의 어트리뷰트를 상속하지만, `withAttributes()`를 사용해 명시적으로 재정의할 수 있습니다.

#### 버퍼 관리(Buffer Management)

`inputBuffer` 어트리뷰트는 비동기 연산자(asynchronous operator)의 버퍼 크기를 제어할 수 있게 해줍니다. 상속은 계층(hierarchy)을 따라 연쇄적으로(cascade) 이루어집니다. 즉, 내부(inner) 연산자는 명시적으로 재정의되지 않는 한 외부(outer) 어트리뷰트를 물려받습니다.

---

## 7. 버퍼와 처리율 다루기(Buffers and Working with Rate)

### 7.1 개요(Overview)

이 섹션은 Akka Streams에서 버퍼(buffer)가 어떻게 동작하는지 다룹니다. 특히 상류(upstream)와 하류(downstream)의 처리율(rate)이 서로 다르거나, 처리량(throughput)에 급격한 스파이크(spike)가 발생하는 경우에 초점을 맞춥니다.

### 7.2 비동기 연산자를 위한 버퍼(Buffers for Asynchronous Operators)

#### 내부 버퍼링 전략(Internal Buffering Strategy)

연산자가 `.async()`를 사용해 비동기적으로 실행되면, 요소 하나를 하류로 전달한 직후 곧바로 다음 메시지를 처리할 수 있습니다. 이는 파이프라이닝(pipelining)을 가능하게 하지만, 스레드 교차 경계(thread-crossing boundary)에서 비용을 발생시킵니다.

Akka Streams는 이를 최적화하기 위해 "윈도우 기반의 배칭 백프레셔 전략(windowed, batching backpressure strategy)"을 사용합니다. 공식 문서의 설명은 다음과 같습니다. 이 전략은 윈도우 기반(windowed)인데, 이는 Stop-And-Wait 프로토콜과 달리 여러 요소가 요소 요청(request)과 함께 동시에 "비행 중(in-flight)"일 수 있기 때문입니다. 또한 배칭(batching) 방식인데, 이는 윈도우 버퍼(window-buffer)에서 요소 하나가 빠져나갈 때마다 즉시 새 요소를 요청하는 것이 아니라, 여러 요소가 빠져나간 뒤에 여러 요소를 한꺼번에 요청하기 때문입니다.

#### 기본 버퍼 설정(Default Buffer Configuration)

기본 내부 버퍼 크기는 다음과 같이 설정됩니다.

```
akka.stream.materializer.max-input-buffer-size = 16
```

#### 사용자 정의 버퍼 크기 설정(Custom Buffer Size Configuration)

버퍼 크기는 `Attributes.inputBuffer()`를 사용해 스트림별로 커스터마이즈할 수 있습니다.

**Scala:**

```scala
val section = Flow[Int].map(_ * 2).async.addAttributes(
  Attributes.inputBuffer(initial = 1, max = 1)
)
```

**Java:**

```java
final Flow<Integer, Integer, NotUsed> flow1 =
    Flow.of(Integer.class)
        .map(elem -> elem * 2)
        .async()
        .addAttributes(Attributes.inputBuffer(1, 1));
```

#### 내부 버퍼 문제 해결(Troubleshooting Internal Buffers)

시간(time) 또는 처리율(rate) 기반의 연산자가 예상치 못한 동작을 보일 때, 입력 버퍼를 1로 줄이는 것은 효과적인 첫 번째 문제 해결(troubleshooting) 단계인 경우가 많습니다.

### 7.3 Akka Streams의 명시적 버퍼(Explicit Buffers)

`buffer()` 연산자는 설정 가능한 오버플로 전략(overflow strategy)과 함께, 명시적이고 애플리케이션 수준의(application-level) 버퍼링을 제공합니다.

#### 백프레셔 전략(Backpressure)

버퍼가 가득 차면 표준 백프레셔를 적용합니다.

```scala
jobs.buffer(1000, OverflowStrategy.backpressure)
```

#### 드롭 전략(Drop Strategies)

**dropTail** — 가장 최근에 추가된(youngest) 요소를 제거합니다.

```scala
jobs.buffer(1000, OverflowStrategy.dropTail)
```

**dropNew** — 들어오는 요소를 버퍼링하지 않고 거부(reject)합니다.

```scala
jobs.buffer(1000, OverflowStrategy.dropNew)
```

**dropHead** — 가장 오래된(oldest) 요소를 제거합니다. 요소가 재전송(retransmit)되는 경우에 선호됩니다.

```scala
jobs.buffer(1000, OverflowStrategy.dropHead)
```

**dropBuffer** — 버퍼가 가득 차면 버퍼링된 모든 요소를 비웁니다(clear).

```scala
jobs.buffer(1000, OverflowStrategy.dropBuffer)
```

#### fail 전략(Fail Strategy)

버퍼가 용량(capacity)에 도달하면 스트림을 종료시킵니다(실패시킵니다).

```scala
jobs.buffer(1000, OverflowStrategy.fail)
```

### 7.4 처리율 변환(Rate Transformation)

#### 빠른 생산자를 위한 conflate

`conflate` 연산자는 빠른 생산자(fast producer)를 백프레셔로 늦출 수 없는 시나리오를 처리합니다. 하류의 수요(demand)가 도착할 때까지 요소들을 결합(combine)해 둡니다.

**통계 요약 예제(Statistical Summary, Scala):**

```scala
val statsFlow = Flow[Double].conflateWithSeed(immutable.Seq(_))(_ :+ _).map { s =>
  val μ = s.sum / s.size
  val se = s.map(x => pow(x - μ, 2))
  val σ = sqrt(se.sum / se.size)
  (σ, μ, s.size)
}
```

**확률 기반 샘플링 예제(Sampling with Probability, Scala):**

```scala
val p = 0.01
val sampleFlow = Flow[Double]
  .conflateWithSeed(immutable.Seq(_)) {
    case (acc, elem) if Random.nextDouble() < p => acc :+ elem
    case (acc, _) => acc
  }
  .mapConcat(identity)
```

#### 느린 생산자를 위한 expand와 extrapolate

이 연산자들은 하류의 수요를 충족시키지 못하는 느린 생산자(slow producer)를 처리하기 위해, 추가적인 요소를 생성합니다.

**extrapolate — 마지막 요소 반복:**

```scala
val lastFlow = Flow[Double].extrapolate(Iterator.continually(_))
```

**extrapolate — 초기 시드(seed) 지정:**

```scala
val initial = 2.0
val seedFlow = Flow[Double].extrapolate(Iterator.continually(_), Some(initial))
```

**expand — 드리프트(drift) 추적:**

```scala
val driftFlow = Flow[Double].expand(i => Iterator.from(0).map(i -> _))
```

#### 핵심 차이: extrapolate 대 expand

- **extrapolate**는 충족되지 못한 하류 수요(unmet downstream demand)가 있을 때만 `Iterator`를 생성하며, 이를 사용해 대기 중인 요청을 충족시킵니다.
- **expand**는 항상(always) `Iterator`를 생성하고 거기서 요소를 방출합니다. 이를 통해 (빈 `Iterator`를 사용하여) 원본 요소를 변환하거나 필터링할 수 있습니다.

이 플로우의 출력은 생산자가 충분히 빠르면 드리프트(drift)를 0으로 보고하고, 그렇지 않으면 더 큰 드리프트를 보고합니다.

---

## 참고 자료

- [Akka 공식 문서](https://doc.akka.io/libraries/akka-core/current/)
- [Stream Introduction](https://doc.akka.io/libraries/akka-core/current/stream/stream-introduction.html)
- [Streams Quickstart Guide](https://doc.akka.io/libraries/akka-core/current/stream/stream-quickstart.html)
- [Design Principles behind Akka Streams](https://doc.akka.io/libraries/akka-core/current/general/stream/stream-design.html)
- [Basics and working with Flows](https://doc.akka.io/libraries/akka-core/current/stream/stream-flows-and-basics.html)
- [Working with Graphs](https://doc.akka.io/libraries/akka-core/current/stream/stream-graphs.html)
- [Modularity, Composition and Hierarchy](https://doc.akka.io/libraries/akka-core/current/stream/stream-composition.html)
- [Buffers and working with rate](https://doc.akka.io/libraries/akka-core/current/stream/stream-rate.html)
