# Akka Streams

## Akka Streams 기초

> 원본: https://doc.akka.io/libraries/akka-core/current/stream/index.html

---

### 목차

1. [소개(Introduction)](#1-소개introduction)
2. [퀵스타트 가이드(Quickstart Guide)](#2-퀵스타트-가이드quickstart-guide)
3. [Akka Streams의 설계 원칙(Design Principles)](#3-akka-streams의-설계-원칙design-principles)
4. [기초와 플로우 다루기(Basics and Working with Flows)](#4-기초와-플로우-다루기basics-and-working-with-flows)
5. [그래프 다루기(Working with Graphs)](#5-그래프-다루기working-with-graphs)
6. [모듈성, 합성, 계층 구조(Modularity, Composition and Hierarchy)](#6-모듈성-합성-계층-구조modularity-composition-and-hierarchy)
7. [버퍼와 처리율 다루기(Buffers and Working with Rate)](#7-버퍼와-처리율-다루기buffers-and-working-with-rate)
8. [참고 자료](#참고-자료)

---

### 1. 소개(Introduction)

#### 1.1 동기(Motivation)

오늘날의 인터넷에서 우리는 데이터를 스트림(stream) 형태로 소비하는 경우가 점점 더 많아지고 있습니다. 파일을 다운로드하거나 업로드할 때, 또는 피어 투 피어(peer-to-peer) 방식으로 데이터를 주고받을 때가 그 예입니다. 데이터를 한 덩어리 전체(in its entirety)가 아니라 요소(element)들의 스트림으로 바라보는 관점은 매우 유용한데, 이는 컴퓨터가 데이터를 송수신하는 방식(예: TCP를 통한 전송)과 정확히 일치하기 때문입니다.

또한, 처리해야 할 데이터셋이 한 번에 메모리에 올리기에는 너무 큰 경우가 흔하며, 이런 경우 데이터를 클러스터(cluster) 전반에 걸쳐 분산 처리해야 할 수도 있습니다. 그럼에도 데이터를 스트림처럼 다루는 것은 합리적인 접근 방식입니다.

액터(actor)는 메시지 스트림을 처리해 지식(knowledge)이나 정보를 전달하는 데 사용될 수 있지만, 안정적이고 견고한 액터 간 스트리밍을 구현하는 일은 번거롭고 오류가 발생하기 쉽습니다(error-prone). 개발자는 버퍼(buffer)나 메일박스(mailbox)가 넘치지 않도록 막아야 하고, 메시지 유실 가능성을 재전송(retransmission)으로 관리해야 합니다. 데이터가 누락되면 수신된 데이터에 빈틈(gap)이 생기기 때문입니다.

이러한 과제를 해결하기 위해 Akka는 Streams API를 함께 제공합니다. 이 API는 스트림 처리 구성을 직관적이고 안전하게 표현할 수 있게 해주며, 그렇게 표현한 구성을 효율적으로 그리고 제한된(bounded) 자원 사용 범위 내에서 실행할 수 있게 해줍니다. 더 이상 `OutOfMemoryError`를 걱정할 필요가 없습니다. 이를 달성하기 위해 Akka Streams는 백프레셔(back-pressure)를 구현하는데, 이는 Reactive Streams의 핵심 원칙으로서 소비자(consumer)가 처리 속도를 따라가지 못할 때 생산자(producer)가 속도를 늦추도록 보장합니다.

#### 1.2 Reactive Streams와의 관계(Relationship with Reactive Streams)

Akka Streams API는 Reactive Streams 인터페이스로부터 완전히 분리(decoupled)되어 있습니다. Akka Streams는 스트림 변환(stream transformation)을 표현(formulate)하는 데 초점을 맞추지만, Reactive Streams는 손실(loss) 없이, 버퍼링이나 자원 고갈(resource exhaustion) 없이 비동기적으로 데이터를 이동시키기 위한 공통 메커니즘(common mechanism)을 정의합니다.

Akka Streams API는 최종 사용자(end-user)를 지향하는 반면, Akka Streams 구현 내부에서는 서로 다른 연산자(operator) 사이로 데이터를 전달하기 위해 Reactive Streams 인터페이스를 사용합니다. Reactive Streams의 주된 목적은 서로 다른 구현체(implementation) 사이의 상호 운용성(interoperability)을 위한 인터페이스를 정의하는 것이지, 최종 사용자용 API를 규정하는 것이 아닙니다.

#### 1.3 이 문서를 읽는 방법(How to Read These Docs)

권장되는 학습 순서는 다음과 같습니다.

- 먼저 **퀵스타트 가이드**(Quick Start Guide)부터 시작하십시오.
- 하향식(top-down)으로 학습하고 싶다면 **설계 원칙**(Design Principles)을 검토하십시오.
- 상향식(bottom-up)으로 학습하고 싶다면 **Streams 쿡북**(Cookbook)을 살펴보십시오.
- 내장된 처리 연산자(operator)에 대해서는 연산자 인덱스(operator index)를 참조하십시오.
- 그 외 섹션들은 순서대로 읽거나 필요에 따라 참조하십시오.

#### 1.4 모듈 정보(Module Info)

Akka Streams 의존성을 사용하려면 Akka 저장소(repository)에서 제공하는 보안이 적용된 토큰화된(tokenized) URL이 필요합니다. 현재 버전은 2.10.19이며, JDK 11, 17, 21을 지원하고 Scala 2.13.17 및 3.3.7과 호환됩니다.

`akka-stream` 아티팩트를 프로젝트에 추가하면 사용할 수 있으며, sbt, Maven, Gradle용 설정 예시가 공식 문서에 제공됩니다.

---

### 2. 퀵스타트 가이드(Quickstart Guide)

#### 2.1 의존성(Dependencies)과 임포트(Imports)

Akka Streams를 사용하려면 해당 모듈을 프로젝트에 추가해야 합니다. 핵심 임포트는 다음과 같습니다.

- Scala: `akka.stream._`, `akka.stream.scaladsl._`
- Java: `akka.stream.*`, `akka.stream.javadsl.*`

추가로 `ActorSystem`, `Done`, `NotUsed`, `ByteString` 등의 유틸리티 임포트가 필요합니다.

#### 2.2 소스(Source)로 시작하기

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

#### 2.3 변환(Transformation)과 재사용 가능한 구성 요소

##### scan을 사용한 누적 계산과 파일 쓰기

`scan` 연산자를 사용하면 스트림 전체에 걸쳐 계산을 수행할 수 있습니다. 1에서 시작하여 들어오는 각 숫자를 차례로 곱해 나가면, 팩토리얼(factorial) 수열이 만들어집니다. 이렇게 생성된 결과는 `FileIO.toPath()`를 통해 파일로 기록할 수 있습니다.

```scala
val factorials = source.scan(BigInt(1))((acc, next) => acc * next)

val result: Future[IOResult] =
  factorials.map(num => ByteString(s"$num\n")).runWith(FileIO.toPath(Paths.get("factorials.txt")))
```

여기서 `runWith(...)`는 주어진 싱크(Sink)로 스트림을 실행하며, 싱크의 구체화된 값(여기서는 `Future[IOResult]`)을 반환합니다.

##### 재사용 가능한 플로우(Flow) 만들기

Akka Streams의 두드러진 특징 중 하나는, 소스(Source)뿐만 아니라 다른 모든 요소도 청사진(blueprint)처럼 재사용할 수 있다는 점입니다. 예를 들어, 문자열을 받아 줄바꿈을 붙여 파일에 쓰는 싱크를 다음과 같이 만들 수 있습니다.

```scala
def lineSink(filename: String): Sink[String, Future[IOResult]] =
  Flow[String].map(s => ByteString(s + "\n")).toMat(FileIO.toPath(Paths.get(filename)))(Keep.right)
```

`toMat(...)`은 플로우와 싱크를 연결하고, 두 번째 인자로 받은 결합 함수(combiner)인 `Keep.right`를 통해 어느 쪽의 구체화된 값을 유지할지 결정합니다. 여기서는 `FileIO.toPath`가 만드는 `Future[IOResult]`(오른쪽)를 유지하므로, 결과 청사진의 타입은 `Sink[String, Future[IOResult]]`가 됩니다.

#### 2.4 시간 기반 처리(Time-Based Processing)

두 개의 소스를 결합하는 `zipWith()`와 흐름의 속도를 제어하는 `throttle()`을 사용하면 스트림의 동작을 실제로 체감할 수 있습니다.

```scala
factorials
  .zipWith(Source(0 to 100))((num, idx) => s"$idx! = $num")
  .throttle(1, 1.second)
  .runForeach(println)
```

Akka Streams는 모든 곳에 걸쳐 흐름 제어(flow control)를 암묵적으로 구현하며, 모든 연산자(operator)는 백프레셔(back-pressure)를 존중합니다. 들어오는 처리율(incoming rate)이 초당 한 개보다 높으면, `throttle` 연산자는 상류(upstream)를 향해 백프레셔를 가합니다(assert back-pressure upstream). 이 덕분에 매우 큰 데이터양을 다루더라도 `OutOfMemoryError`를 막을 수 있습니다.

#### 2.5 Reactive Tweets 예제

##### 데이터 모델

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

##### 필터링(Filtering)과 매핑(Mapping)

기본적인 스트림 연산은 컬렉션과 유사한 문법을 따릅니다. 예를 들어 `#akka` 해시태그를 포함하는 트윗을 필터링하고, 거기서 `Author` 객체를 추출하도록 매핑할 수 있습니다. 이러한 연산은 Scala 컬렉션 라이브러리를 사용해 본 사람에게는 익숙하게 보이지만, 컬렉션이 아니라 스트림 위에서 동작한다는 점이 다릅니다.

##### mapConcat을 사용한 평탄화(Flattening)

1-대-다(one-to-many) 관계를 다룰 때는 `mapConcat`을 사용합니다. `flatMap`이라는 이름은 for-컴프리헨션(for-comprehension)이나 모나딕 합성(monadic composition)과 너무 가까워 혼동을 줄 수 있기에 의도적으로 피했습니다. 대신 `mapConcat`은 각 트윗을 해당 트윗의 해시태그 목록으로 변환하여, 평탄화된(flattened) 스트림을 생성합니다.

#### 2.6 그래프 기반 패턴(Graph-Based Patterns)

##### 브로드캐스트(Broadcast)

팬 아웃(fan-out) 시나리오에서는, 이러한 팬 아웃 구조를 형성하는 데 사용되는 요소들을 Akka Streams에서 정션(junction)이라고 부릅니다. `Broadcast` 요소는 자신의 입력 포트로 들어온 요소들을 모든 출력 포트로 방출합니다.

비선형적이고 복잡한 스트림 구조에는 `GraphDSL`이 가장 편리한 API를 제공합니다. 소스를 `Broadcast`를 통해 여러 싱크에 연결할 때는 엣지(edge) 연산자 `~>`를 사용합니다.

#### 2.7 백프레셔 처리(Back-Pressure Handling)

구독자(subscriber)가 발행자(publisher)의 속도를 따라가지 못하는 상황이 발생할 수 있습니다. Akka Streams는 이런 시나리오에서 어떤 일이 일어나야 할지를 제어할 수 있도록 내부 백프레셔 신호(internal backpressure signal)에 의존합니다.

`buffer` 연산자는 `dropHead` 같은 명시적인 오버플로 전략(overflow strategy)을 제공합니다. 버퍼링은 명시적으로(explicitly) 처리할 수 있고, 또 반드시 명시적으로 처리해야 한다는 점이 강조됩니다. 메모리 문제를 일으킬 수 있는 암묵적인 큐잉(implicit queuing)에 의존해서는 안 됩니다.

#### 2.8 구체화된 값(Materialized Values)

##### 구체화(Materialization) 이해하기

`Source`, `Flow`, `Sink`에 붙은 타입 파라미터(이른바 Mat 타입)는 이들 처리 구성 요소가 구체화될 때 반환하는 값의 타입을 나타냅니다. 소스를 만든다는 것은 "처음 100개의 자연수를 어떻게 방출할지에 대한 기술(description)"을 갖게 됨을 의미할 뿐, 이 소스 자체는 아직 활성화(active)된 상태가 아닙니다.

##### 구체화된 값 결합하기

`toMat()`과 `Keep` 결합 함수를 사용해 파이프라인 내 여러 구성 요소가 만들어 내는 결과를 결합할 수 있습니다. `Keep.right()`를 사용하면 가장 오른쪽 연산자의 구체화 타입을 유지하므로, 결과 청사진은 `Sink[String, Future[IOResult]]`가 됩니다.

##### RunnableGraph

`RunnableGraph`는 실행할 준비가 된, 완전히 연결된(fully connected) 그래프를 나타냅니다. `RunnableGraph[T]`에 대해 `run()`을 호출한 결과값의 타입은 `T`입니다. 이를 통해 유한(finite) 스트림에서 요소 개수 같은 결과를 얻을 수 있습니다.

##### 재사용 가능한 청사진(Reusable Blueprints)

`RunnableGraph`는 단지 스트림의 청사진이기 때문에 여러 번 재사용하고 구체화할 수 있습니다. 즉, 동일한 그래프를 서로 다른 데이터 배치(batch)에 적용해 각기 다른 결과를 얻을 수 있습니다.

#### 2.9 핵심 개념 요약

퀵스타트는 다음과 같은 기초 패턴을 확립합니다. 소스(Source)는 요소를 방출하고, 플로우(Flow)는 이를 변환하며, 싱크(Sink)는 이를 소비합니다. 그리고 모든 구성 요소는 백프레셔를 존중합니다. 청사진(blueprint) 기술과 실행(execution)을 구체화(materialization)를 통해 분리함으로써, 단순한 선형 파이프라인부터 복잡한 그래프 토폴로지까지 모두에 적합한 재사용 가능하고 합성 가능한(composable) 스트림 정의가 가능해집니다.

---

### 3. Akka Streams의 설계 원칙(Design Principles)

#### 3.1 핵심 철학(Core Philosophy)

Akka Streams는 단순히 사용하기 쉬운 것보다는, 최소하고 일관된 API(minimal and consistent APIs)를 강조합니다. 이 프레임워크는 "마법(magic)"보다 "명시성(explicitness)"을 우선시하여, 예외 없이 신뢰성 있게 동작하는 기능을 보장합니다.

세 가지 근본 원칙은 다음과 같습니다.

1. **모든 기능은 API에 명시적으로 드러난다. 마법은 없다(no magic).**
2. **최상의 합성성(Supreme compositionality): 결합된 조각들은 각 부분의 기능을 그대로 유지한다.**
3. **분산된 제한된 스트림 처리(distributed bounded stream processing)라는 도메인의 완전한 모델(exhaustive model).**

#### 3.2 사용자가 기대해야 하는 것(What Users Should Expect)

사용자는 어떠한 스트림 처리 토폴로지(topology)든 표현할 수 있는 도구를 제공받으며, 동시에 백프레셔(back-pressure), 버퍼링(buffering), 변환(transformation), 실패 복구(failure recovery) 같은 본질적인 측면들을 모델링할 수 있습니다. 그리고 구축된 모든 구성물은 더 큰 맥락(larger context) 안에서 재사용 가능한 상태로 남아 있습니다.

##### 요소 처리 보장(Element Processing Guarantees)

Akka Streams는 토폴로지를 통해 전송된 모든 객체가 처리될 것이라고 보장할 수 없습니다. 요소(element)는 여러 이유로 드롭(drop)될 수 있습니다.

- `map(…)` 같은 연산자 안의 사용자 코드가 전혀 다른 출력을 만들어 낼 수 있습니다.
- 일반적인 연산자들이 의도적으로 요소를 드롭합니다: `take` / `drop` / `filter` / `conflate` / `buffer`.
- 스트림 실패(failure)는 처리 중인(in-flight) 요소를 기다리지 않고 폐기합니다.
- 스트림 취소(cancellation)는 상류로 전파되어 처리 단계들을 종료시킵니다.

따라서 정리(cleanup)가 필요한 객체의 마무리(finalization)는 Akka Streams 기능 바깥에서 사용자가 직접 처리해야 합니다.

##### 구현 아키텍처(Implementation Architecture)

합성성을 위해서는 재사용 가능한 부분 토폴로지(partial topology)가 필요합니다. Akka는 데이터 플로우를 불변(immutable)의 그래프 청사진(graph blueprint)으로 표현하는 "리프티드 접근법(lifted approach)"을 사용하며, 이를 명시적으로 구체화(materialize)할 때 비로소 처리가 시작됩니다. 구체화는 엔진(engine)과 상호작용하기 위한 특정 객체를 생성하는데, 이 객체를 "그래프의 구체화된 값(materialized value of a graph)"이라고 부릅니다.

#### 3.3 Reactive Streams와의 상호 운용성(Interoperation with Reactive Streams)

Akka는 Reactive Streams 명세(specification)를 완전히 구현합니다. 다만, Reactive Streams 인터페이스를 사용자 수준의 API와 의도적으로 분리하여, 이를 최종 사용자용 도구가 아니라 구현 세부 사항(implementation detail)으로 취급합니다.

`Publisher`나 `Subscriber` 인스턴스를 얻으려면 다음을 사용합니다.

- `Sink.asPublisher` — Sink로부터 Publisher를 얻습니다.
- `Source.asSubscriber` — Source로부터 Subscriber를 얻습니다.

##### 단일 구독자 제약(Single Subscriber Restriction)

기본 Akka 구체화는 단일 구독자(single-subscriber) `Processor`를 생성합니다. 추가 구독자는 거부되는데, 이는 DSL 토폴로지가 모든 팬 아웃(fan-out)을 `Broadcast<T>` 같은 명시적 요소를 통해 처리하기 때문입니다.

다른 Reactive Streams 구현체와 브로드캐스트 상호 운용성이 필요한 경우에는 `Sink.asPublisher(true)`를 사용하십시오.

##### 왜 Reactive Streams 인터페이스와 분리하는가?

Reactive Streams는 서비스 제공자 인터페이스(Service Provider Interface, SPI)로 기능합니다. 즉, 상호 운용 가능한 라이브러리들을 위한 내부 인프라(internal infrastructure)입니다. 이 타입들을 최종 사용자에게 노출하면 내부 구현 세부 사항을 의도치 않게 유출(leak)하게 됩니다.

`Source`, `Sink`, `Flow`는 유창한(fluent) DSL과 구체화 팩토리(materialization factory)를 제공하는 반면, 그에 대응하는 Reactive Streams 타입(`Publisher`, `Subscriber`, `Processor`)은 더 낮은 수준(lower level)에서 동작합니다. 이렇게 분리하면 구체화 시점에 퓨징(fusing)이나 디스패처(dispatcher) 설정과 같은 최적화를 적용할 수 있습니다.

또한 Java 9부터는 `java.util.concurrent.Flow.Subscriber`가 포함되었기 때문에, Reactive Streams 타입을 직접 상속(extend)하는 라이브러리는 마이그레이션 문제에 직면합니다. Akka는 두 가지 모두를 투명하게(transparently) 지원합니다.

Akka는 구현하기 어려운 Reactive Streams 조각들 대신, 더 단순한 추상화인 `GraphStage`와 연산자(operator)를 사용하도록 권장합니다. 상호 운용은 `Sink.asPublisher`나 `Source.asSubscriber` 같은 메서드를 통해 여전히 사용할 수 있습니다.

#### 3.4 스트리밍 라이브러리가 제공해야 하는 것(What Streaming Libraries Should Provide)

Akka Streams 위에 구축되는 라이브러리는 다음 원칙을 따라야 합니다.

1. **재사용 가능한 연산자(reusable operator)를 노출하라.** 합성 가능한 조각을 반환하는 팩토리(factory) 형태로 제공하여 완전한 합성성을 가능하게 해야 합니다.
2. **선택적으로 구체화 편의 기능(materialization facilities)을 제공하라.** 편리한 소비를 위해 제공할 수 있습니다.

첫 번째 규칙은 합성성이 파괴되는 것을 방지합니다. 만약 라이브러리가 이미 구체화된(pre-materialized) 연산자만 받아들인다면, 여러 라이브러리를 결합하는 것이 불가능해집니다. 라이브러리 기능은 자원 바인딩(resource binding)을 구체화 시점까지 미뤄야 하며, 그 시점은 사용자가 제어합니다.

두 번째 규칙은 편의용(convenience) API를 허용합니다. 예를 들어 Akka HTTP의 `handleWith` 메서드처럼, 흔한 시나리오를 위한 편의 메서드를 제공할 수 있습니다.

##### 살아 있는 자원의 처리(Live Resource Handling)

재사용 가능한 플로우 기술(flow description)은 "살아 있는(live)" 자원(이미 존재하는 TCP 연결이나 활성화된 멀티캐스트 Publisher 등)에 바인딩되어서는 안 됩니다. 자원 할당(resource allocation)은 반드시 구체화 시점까지 미뤄져야 합니다. `TickSource`는 타이머가 구체화 시점에만 초기화된다면 "살아 있지 않은(non-live)" 자원으로 간주됩니다.

이에 대한 예외는 신중한 정당화(justification)와 문서화(documentation)를 필요로 합니다.

##### 라이브러리를 위한 구성 요소(Building Blocks for Libraries)

기초가 되는 요소들은 다음과 같습니다.

- **Source**: 정확히 하나의 출력 스트림(output stream).
- **Sink**: 정확히 하나의 입력 스트림(input stream).
- **Flow**: 정확히 하나의 입력과 하나의 출력 스트림.
- **BidiFlow**: 두 개의 입력과 두 개의 출력 스트림을 가지며, 서로 반대 방향으로 동작하는 두 개의 Flow처럼 동작합니다.
- **Graph**: 입력/출력 포트를 노출하는, 패키징된 토폴로지(packaged topology)로서 `Shape` 객체로 특징지어집니다.

스트림을 방출하는 스트림(streams emitting streams)도 여전히 평범한 `Source`로 남아 있습니다. 요소의 타입이 무엇이든 정적인(static) 토폴로지 표현에는 영향을 주지 않습니다.

#### 3.5 오류(Error) 대 실패(Failure)

Reactive Manifesto를 따르면, 오류(error)는 스트림 데이터 요소(data element)이고, 실패(failure)는 스트림 자체가 붕괴(collapse)되는 것을 의미합니다.

Reactive Streams 인프라에서: `onNext`는 데이터(오류 포함)를 신호하고, `onError`는 실패를 신호합니다.

> **용어에 대한 주석**: 메서드 이름 `onError`는 역사적인 이유(historical)로 붙여진 것입니다. 이는 구독자(Subscriber)에게 "실패(failure)"를 신호하는데, 이는 고수준의 붕괴(high-level collapse)를 스트림 인프라가 낮은 수준에서 표현한 것입니다.

##### 제한적인 오류 지원(Limited Error Support)

Akka Streams는 데이터 변환 연산자에 비해 `onError` 처리를 제한적으로 제공합니다. 이는 의도적인 것인데, `onError`가 스트림의 붕괴를 신호하기 때문입니다. 변환 연산자(transformation operator)는 스트림과 함께 붕괴되며, 버퍼링된 요소가 유실될 수 있습니다.

실패(failure)는 데이터 요소(data element)보다 더 빠르게 전파됩니다. 이는 교착(deadlock)되거나 넘쳐버릴(overflow) 수 있는 백프레셔 상태의 스트림을 해체(tear down)하는 데 필수적입니다.

##### 스트림 복구 시맨틱(Stream Recovery Semantics)

복구(recovery) 요소는 `onError` 신호를 흡수하여, 이를 데이터 요소(data element)로 변환한 뒤 정상적인 완료(normal completion)로 이어지게 합니다. 복구는 일종의 격벽(bulkhead) 역할을 하여, 붕괴를 토폴로지의 특정 영역(specific region)에 가둡니다.

붕괴된 영역 안에서는 버퍼링된 요소가 유실될 수 있지만, 외부 영역(external region)은 영향을 받지 않은 채로 남아 있습니다. 이는 `try`-`catch`의 동작과 유사한데, 명령문(statement)의 위치가 예외 영역(exception region) 안에서 어디까지 건너뛸지(skip)를 결정하는 것과 같은 원리입니다.

---

### 4. 기초와 플로우 다루기(Basics and Working with Flows)

#### 4.1 핵심 개념(Core Concepts)

Akka Streams는 제한된 버퍼 공간(bounded buffer space)을 사용하여 요소(element)의 시퀀스(sequence)를 처리하는 라이브러리입니다. 핵심 용어는 다음과 같습니다.

- **스트림(Stream)**: 데이터를 이동하고 변환하는 일을 포함하는 활성 프로세스(active process).
- **요소(Element)**: 스트림의 처리 단위(processing unit). 모든 연산은 요소를 상류(upstream)에서 하류(downstream)로 변환하고 전달합니다.
- **백프레셔(Back-pressure)**: 흐름 제어(flow-control)의 수단으로, 데이터의 소비자가 생산자에게 자신의 현재 가용성(availability)을 알리는 방법입니다. 이를 통해 상류 생산자의 속도를 효과적으로 늦추어 소비 속도에 맞춥니다.
- **논블로킹(Non-Blocking)**: 어떤 연산이, 요청된 작업이 완료되기까지 오랜 시간이 걸리더라도, 호출한 스레드(calling thread)의 진행을 방해하지 않는 것.
- **그래프(Graph)**: 스트림 처리 토폴로지(topology)의 기술(description)로서, 스트림이 실행될 때 요소가 흘러갈 경로(pathway)를 정의합니다.
- **연산자(Operator)**: 그래프를 구성하는 모든 빌딩 블록(building block)에 대한 공통 명칭. 예로는 `map()`, `filter()`, `GraphStage`를 확장한 사용자 정의 연산자, 그리고 `Merge`나 `Broadcast` 같은 그래프 정션(junction)이 있습니다.

#### 4.2 스트림 정의와 실행(Defining and Running Streams)

선형 파이프라인(linear pipeline)을 위한 핵심 추상화는 다음과 같습니다.

- **Source**: 정확히 하나의 출력을 가진 연산자로, 하류 연산자가 받을 준비가 되었을 때마다 데이터 요소를 방출합니다.
- **Sink**: 정확히 하나의 입력을 가진 연산자로, 데이터 요소를 요청하고 받아들이며, 필요하다면 상류 생산자의 속도를 늦출 수 있습니다.
- **Flow**: 정확히 하나의 입력과 하나의 출력을 가진 연산자로, 흐르는 데이터 요소를 변환함으로써 상류와 하류를 연결합니다.
- **RunnableGraph**: 양쪽 끝이 각각 Source와 Sink에 "부착(attached)"되어 `run()`을 호출할 준비가 된 Flow.

소스, 싱크, 그리고 여러 연산자를 모두 연결해 `RunnableGraph`를 구성한 이후라도, 구체화(materialize)되기 전까지는 어떤 데이터도 흐르지 않습니다.

#### 4.3 Source, Sink, Flow 정의하기

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

#### 4.4 허용되지 않는 스트림 요소(Illegal Stream Elements)

Akka Streams는 `null`을 스트림의 요소로 전달하는 것을 허용하지 않습니다. 권장되는 대안으로는 `scala.Option`, `scala.util.Either`, 또는 `java.util.Optional`이 있습니다.

#### 4.5 백프레셔 설명(Back-Pressure Explained)

Akka는 Reactive Streams 명세에 의해 표준화된 비동기 논블로킹 백프레셔 프로토콜(asynchronous non-blocking back-pressure protocol)을 구현합니다. 이 프로토콜은 수요(demand) 신호를 통해 동작합니다.

소스(source)는 주어진 어떤 구독자(Subscriber)에 대해서도, 수신한 총 수요(total demand)보다 더 많은 요소를 결코 방출하지 않을 것을 보장합니다.

##### 느린 발행자, 빠른 구독자(Slow Publisher, Fast Subscriber)

이 시나리오에서 구독자는 `Request(n)` 메시지를 비동기적으로 신호합니다. 발행자(Publisher)는 들어오는 요소를 발행하면서 결코 기다릴(백프레셔를 받을) 필요가 없으며, 푸시 모드(push-mode)로 동작합니다.

##### 빠른 발행자, 느린 구독자(Fast Publisher, Slow Subscriber)

이 경우 발행자는 다음과 같은 전략을 적용해야 합니다.

- 생산 속도를 제어할 수 있다면 요소를 생성하지 않는다.
- 더 많은 수요가 신호될 때까지 요소를 제한된(bounded) 방식으로 버퍼링하려고 시도한다.
- 더 많은 수요가 신호될 때까지 요소를 드롭(drop)한다.
- 위 어느 전략도 적용할 수 없다면 스트림을 해체(tear down)한다.

#### 4.6 스트림 구체화(Stream Materialization)

구체화(materialization)는 스트림 기술(RunnableGraph)을 받아 실행에 필요한 모든 자원을 할당(allocate)하는 과정입니다. 일반적으로 처리를 구동하는 액터(Actor)를 시작하는 것을 포함하지만, 파일이나 소켓 연결(socket connection)을 여는 것을 의미할 수도 있습니다.

구체화를 유발하는 종단 연산(terminal operation)에는 `run()`, `runWith()`, 그리고 `runForeach(...)` 같은 편의 메서드가 있습니다.

##### 연산자 퓨징(Operator Fusion)

기본적으로 Akka Streams는 스트림 연산자들을 퓨징(fuse)합니다. 이는 플로우나 스트림의 처리 단계들이 동일한 액터(Actor) 내에서 실행될 수 있음을 의미합니다. 그 결과 요소 전달이 더 빨라지지만, 퓨징된 영역(fused region)당 하나의 CPU 코어만 사용하게 됩니다.

병렬 처리(parallel processing)를 위해서는 비동기 경계(asynchronous boundary)를 삽입해야 하며, `.async` 메서드를 통해 추가할 수 있습니다.

##### 구체화된 값 결합하기(Combining Materialized Values)

`toMat()`이나 `viaMat()` 같은 메서드를 사용하면 구체화된 값을 합성(compose)할 수 있습니다. `Keep` 객체는 다음과 같은 편의 함수를 제공합니다.

- `Keep.left` — 왼쪽(left-side)의 구체화된 값을 유지
- `Keep.right` — 오른쪽(right-side)의 구체화된 값을 유지
- `Keep.both` — 두 값을 튜플(tuple)로 결합

더 복잡한 조합은 `mapMaterializedValue()`를 사용해 중첩된 결과를 변환할 수 있습니다.

#### 4.7 소스 사전 구체화(Source Pre-materialization)

`preMaterialize()` 연산자는 소스를 스트림 그래프에 부착하기 전에 그 소스의 구체화된 값을 미리 얻을 수 있게 해줍니다. 이는 `Source.queue`, `Source.actorRef`, `Source.maybe`처럼 구체화된 값으로 구동되는(materialized value powered) 소스에서 유용합니다.

#### 4.8 스트림 순서(Stream Ordering)

거의 모든 계산 연산자(computation operator)는 요소의 입력 순서(input order)를 보존합니다. 입력 {IA1, IA2, ...}이 출력 {OA1, OA2, ...}을 유발하고, 이것이 출력 {OB1, OB2, ...}을 유발하는 입력 {IB1, IB2, ...}보다 먼저 발생한다면, 출력 OAi는 OBi보다 앞서게 됩니다.

`mapAsync` 같은 비동기 연산은 순서를 유지하는 반면, `mapAsyncUnordered`는 그렇지 않습니다. `Merge` 같은 팬 인(fan-in) 연산은 서로 다른 입력 포트 간의 출력 순서가 정의되어 있지 않지만(no defined order), `Zip`은 순서를 보장합니다.

#### 4.9 ActorMaterializer 생명주기(Lifecycle)

`Materializer`는 스트림 청사진의 실행을 관장합니다. `ActorSystem` 전체 범위(system-wide)의 구체화기는 `SystemMaterializer` 익스텐션(extension)을 통해 제공됩니다.

액터 내부에서 구체화기를 생성하면, 스트림의 생명주기가 액터의 생명주기에 묶입니다. 즉, 액터가 멈추면 스트림도 종료됩니다.

시스템 범위(system-scoped)의 구체화기를 명시적으로 전달하면, 스트림이 그것을 생성한 액터보다 더 오래 살아남게(outlive) 할 수 있습니다. 액터 내부에서 새로운 구체화기를 직접 생성하면 자원 누수(resource leak) 위험이 있습니다. 대신 구체화기를 주입받거나 액터 컨텍스트(actor context)를 통해 생성하십시오.

---

### 5. 그래프 다루기(Working with Graphs)

#### 5.1 개요(Overview)

Akka Streams에서 "그래프 다루기"는 `GraphDSL`을 사용해 비선형(non-linear) 스트림 토폴로지를 구성하는 방법을 다룹니다. 팬 인(fan-in)과 팬 아웃(fan-out) 연산이 필요한 계산 그래프(computation graph)를 만드는 데 초점이 맞춰져 있습니다.

#### 5.2 그래프 구성의 기초(Graph Construction Fundamentals)

그래프는 선형(linear) Flow와 달리, 그래프 설계를 화이트보드(whiteboard)에 그린 그림처럼 보이도록 설계된 전용 DSL을 사용합니다. 그래프는 그래프 내의 선형 연결(linear connection) 역할을 하는 단순한 Flow와, 팬 인 및 팬 아웃 지점 역할을 하는 정션(junction)으로 구성됩니다.

`GraphDSL`은 요소를 연결하기 위해 `~>` 연산자(그리고 그 반대 방향인 `<~`)를 사용합니다. 중요한 설계상의 특징은, `GraphDSL.Builder` 객체는 가변(mutable)이지만, `GraphDSL` 인스턴스 자체는 불변(immutable)이고 스레드 안전(thread-safe)하며 자유롭게 공유(shareable) 가능하다는 점입니다.

#### 5.3 사용 가능한 정션(Available Junctions)

##### 팬 아웃 연산자(Fan-out: 1 입력, N 출력)

- **Broadcast[T]**: 입력 요소를 각 출력으로 방출합니다.
- **Balance[T]**: 요소를 (사용 가능한) 하나의 출력 포트로 분배합니다.
- **Partition[T]**: 분할 함수(partition function)에 따라 요소를 라우팅합니다.
- **UnzipWith**: 요소를 N개의 출력으로 분할합니다.
- **UnZip[A, B]**: 튜플(tuple) 스트림을 두 개의 별도 스트림으로 분할합니다.

##### 팬 인 연산자(Fan-in: N 입력, 1 출력)

- **Merge[In]**: 여러 입력 중에서 무작위로(randomly) 선택합니다.
- **MergePreferred[In]**: 선호 포트(preferred port)를 우선시합니다.
- **MergePrioritized[In]**: 우선순위(priority) 기반으로 선택합니다.
- **MergeLatest[In]**: 갱신된 값들로 구성된 `List`를 방출합니다.
- **ZipWith / Zip**: 여러 입력을 하나의 출력으로 결합합니다.
- **Concat[A]**: 두 스트림을 순차적으로(sequentially) 이어 붙입니다.

#### 5.4 부분 그래프(Partial Graphs)

부분 그래프는 `ClosedShape` 이외의 셰이프(shape)를 반환하여 모듈식(modular) 구성을 가능하게 합니다. 핵심 사항은 다음과 같습니다.

- `SourceShape`는 정확히 하나의 출력을 가진 부분 그래프를 나타냅니다.
- `SinkShape`는 정확히 하나의 입력을 가진 부분 그래프를 나타냅니다.
- `FlowShape`는 하나의 입력과 하나의 출력을 가집니다.
- 부분 그래프는 모든 포트가 연결되어 있거나, 아니면 반환되는 셰이프의 일부인지(즉 외부로 노출되는 포트인지)를 검증합니다.

#### 5.5 그래프로부터 Source, Sink, Flow 생성하기

복잡한 그래프 구조는 `Source.fromGraph`, `Sink.fromGraph`, `Flow.fromGraph`를 사용해 단순한 연산자로 캡슐화(encapsulate)할 수 있습니다. 이 메서드들은 적절한 셰이프를 가진 그래프를 받아, 이를 표준 스트림 요소로 노출합니다.

#### 5.6 단순화된 결합 API(Simplified Combination API)

흔한 경우를 위해, `Source.combine()`이나 `Sink.combine()` 같은 메서드는 명시적인 `GraphDSL` 구성을 불필요하게 만듭니다. 이 메서드들은 내부의 그래프 배선(wiring)을 자동으로 처리합니다.

#### 5.7 재사용 가능한 컴포넌트(Reusable Components)

셰이프 클래스를 확장하거나 구현하여 사용자 정의 셰이프(custom shape)를 만들 수 있습니다. 공식 문서의 예제는 두 개의 입력과 하나의 출력을 가진 `PriorityWorkerPoolShape`를 보여주며, 이는 복잡한 내부 배선을 캡슐화하면서도 깔끔한 인터페이스를 제공하는 도메인 특화(domain-specific) 정션을 만드는 방법을 설명합니다.

#### 5.8 양방향 플로우(Bidirectional Flows)

`BidiFlow` 타입은 서로 반대 방향으로 배치된 두 개의 입력과 두 개의 출력을 가진 그래프를 나타냅니다. 이는 나가는(outgoing) 메시지를 직렬화(serialize)하고 들어오는(incoming) 옥텟 스트림(octet stream)을 역직렬화(deserialize)하는 코덱(codec) 연산자에 유용합니다. `BidiFlow`는 단순한 변환의 경우 `BidiFlow.fromFunctions()`를 사용해 생성하거나, 복잡한 로직의 경우 `GraphDSL`을 통해 구성할 수 있습니다.

#### 5.9 그래프 내부에서 구체화된 값 접근하기

`builder.materializedValue` 아웃렛(outlet)은 그래프 구성 내부에서 구체화된 값(materialized value)을 피드백(feedback)할 수 있게 해줍니다.

> **중요한 경고**: 구체화된 값이 실제로 자기 자신의 구체화된 값에 기여하는(contribute) 사이클(cycle)을 만들지 않도록 주의하십시오.

#### 5.10 그래프 사이클과 교착(Graph Cycles and Deadlocks)

공식 문서는 라이브니스(liveness) 문제를 일으키는 세 가지 주요 패턴을 식별합니다.

1. **불균형한 주입/추출(Unbalanced injection/extraction)**: 사이클이 잃는 요소보다 더 많은 요소를 얻게 되면, 결국 버퍼가 완전히 가득 차서 교착(deadlock)이 발생합니다.

2. **요소 기아(Element starvation)**: `MergePreferred`를 사용하면 교착은 막을 수 있지만, 소스 소비(source consumption)가 기아 상태(starve)에 빠질 수 있습니다.

3. **초기화 교착(Initialization deadlocks)**: `ZipWith` 같은 연산을 사용하는 균형 잡힌(balanced) 사이클은 "닭과 달걀(chicken-and-egg)" 문제를 일으킬 수 있습니다. 초기 요소를 처리하기 위해 초기 요소가 필요한 상황입니다.

제시된 해결책은 다음과 같습니다.

- 피드백 아크(feedback arc)에 `OverflowStrategy.dropHead` 같은 드롭(dropping) 전략을 사용하기.
- 요소 평형(element equilibrium)을 유지하는 균형 잡힌 연산을 사용하기.
- `Source.single()`과 `Concat`을 통해 초기 "킥오프(kick-off)" 요소를 주입하기.

#### 5.11 구현 세부 사항(Implementation Details)

`GraphDSL`은 모든 요소가 제대로 연결되었는지에 대한 컴파일 타임(compile time) 타입 안전성을 제공할 수 없습니다. 이 검증(validation)은 그래프 인스턴스화(instantiation) 시점에 런타임 검사(runtime check)로 수행됩니다.

정션의 참조 동등성(reference equality)이 그래프 노드의 동등성을 결정합니다. 즉, 동일한 정션 인스턴스는 결과 그래프에서 동일한 위치(location)에 대응합니다.

---

### 6. 모듈성, 합성, 계층 구조(Modularity, Composition and Hierarchy)

#### 6.1 핵심 개념(Core Concepts)

Akka 문서는 스트림 처리를 "포트가 있는 상자(box with ports)"라는 멘탈 모델(mental model)로 제시합니다. Akka Streams에서 사용되는 모든 연산자는, 처리할 요소가 도착하고 떠나는 입력 포트(input port)와 출력 포트(output port)를 가진 하나의 "상자(box)"로 상상할 수 있습니다.

##### 연산자 타입과 셰이프(Operator Types and Shapes)

프레임워크는 연산자를 서로 다른 셰이프(shape)로 분류합니다.

- **선형 연산자(Linear operator)** (Source, Sink, Flow)는 엄격한 체인(strict chain)을 형성합니다.
- **팬 인 / 팬 아웃 연산자**는 복잡하고 비선형적인 레이아웃(layout)을 가능하게 합니다.
- **BidiFlow** 연산자는 양방향 입출력을 처리하며, 프로토콜 계층화(protocol layering)에 유용합니다.

Source는 단 하나의 출력 포트를 가진 "상자"에 지나지 않으며, BidiFlow는 정확히 두 개의 입력 포트와 두 개의 출력 포트를 가진 "상자"입니다.

#### 6.2 합성과 모듈성의 기초(Basics of Composition and Modularity)

모듈성(modularity)은 복잡한 그래프가 재사용 가능한 컴포넌트로 패키징될 때 나타납니다. 기본 연산자만으로도 복잡한 처리 네트워크(processing network)를 만드는 것은 가능하지만, 그것만으로는 모듈성을 구현하지는 못합니다.

##### 복합 연산자 만들기(Creating Composite Operators)

유창한 DSL(`named()`이나 `withAttributes()` 사용)은 캡슐화(encapsulation)를 가능하게 합니다. 선형 연산자를 코드에서 연결할 때, 개발자는 경계(boundary)를 명시적으로 선언해야 합니다. 예제는 연산을 체이닝(chaining)할 때 현재의 Source를 감싸서(wrap up) 이름을 부여하기 위해 `named()` 메서드가 필요함을 보여줍니다.

핵심 원칙: 이러한 재사용 가능한 컴포넌트들은 이미 복잡한 처리 네트워크의 생성을 가능하게 합니다. 복합체(composite)는 내부를 숨기면서(hide internals) 필요한 포트만 외부에 노출합니다.

#### 6.3 복잡한 시스템 합성하기(Composing Complex Systems)

##### 비선형 레이아웃을 위한 GraphDSL

`GraphDSL`은 복잡한 그래프를 위한 고급 합성(advanced composition)을 제공합니다. 두 DSL(선형 DSL과 `GraphDSL`)의 차이는 표면적인 것일 뿐이며, 다루는 개념(concept)은 모든 DSL에 걸쳐 동일합니다(uniform).

복잡한 레이아웃에는 `RunnableGraph.fromGraph()`와 `GraphDSL.create()` 메서드를 사용하여 팬 인, 팬 아웃, 사이클(cycle)을 갖춘 정교한 처리 네트워크를 구축할 수 있습니다.

##### 셰이프 정의(Shape Definition)

재사용 가능한 그래프 컴포넌트를 만들 때, 개발자는 노출되는 포트를 `Shape`를 통해 명시합니다. 모든 연산자(Source, BidiFlow 등을 포함하여)는 셰이프(shape)를 가지며, 이 셰이프는 모듈의 타입이 지정된 포트(typed port)들을 인코딩(encode)합니다.

내장 셰이프(built-in shape)에는 `SourceShape`, `FlowShape`, `SinkShape`, `BidiShape`가 있습니다.

#### 6.4 구체화된 값(Materialized Values)

##### 합성을 통한 전파(Propagation Through Composition)

구체화된 값은 실행 중인 스트림 네트워크와 상호작용할 수 있는 능력(interaction capability)을 나타냅니다. 각 구체화(materialization)는 제공된 `RunnableGraph`에 인코딩된 청사진에 대응하는, 새로운 실행 중인 네트워크(running network)를 생성합니다.

액터의 `Props`와 달리, 스트림에서는 각 연산자가 구체화된 값을 제공할 수 있습니다. 따라서 여러 연산자나 모듈을 합성할 때는 그 구체화된 값들도 함께 결합(combine)해야 합니다.

##### 결합 함수(Combiner Functions)

`viaMat()`, `toMat()` 같은 메서드는 결합 함수(`Keep.left`, `Keep.right`, `Keep.both`)를 받아서, 어떤 구체화된 값이 상위로(upward) 전파될지를 결정합니다. 사용자 정의 결합 함수(custom combiner)는 여러 값을 단일 결과(single result)로 변환할 수도 있습니다.

#### 6.5 어트리뷰트(Attributes)

##### 정의와 상속(Definition and Inheritance)

어트리뷰트(Attribute)는 구체화되는 엔티티(materialized entity)의 여러 측면을 세밀하게 조정(fine-tune)합니다. 어트리뷰트는 중첩된 모듈(nested module)에 상속(inherit)되며, 해당 모듈이 사용자 정의 값으로 재정의(override)하지 않는 한 그대로 적용됩니다.

##### 일반적인 어트리뷰트(Common Attributes)

- **name**: `named()` 단축 메서드를 통해 생성됩니다.
- **inputBuffer**: 비동기 경계(asynchronous boundary)의 버퍼 크기를 제어합니다.

그래프에서 연산자에 가장 가깝게(closest) 지정된 어트리뷰트가 그 연산자에 대해 실제로 적용되는 값입니다. 중첩 모듈은 부모(parent)의 어트리뷰트를 상속하지만, `withAttributes()`를 사용해 명시적으로 재정의할 수 있습니다.

##### 버퍼 관리(Buffer Management)

`inputBuffer` 어트리뷰트는 비동기 연산자(asynchronous operator)의 버퍼 크기를 제어할 수 있게 해줍니다. 상속은 계층(hierarchy)을 따라 연쇄적으로(cascade) 이루어집니다. 즉, 내부(inner) 연산자는 명시적으로 재정의되지 않는 한 외부(outer) 어트리뷰트를 물려받습니다.

---

### 7. 버퍼와 처리율 다루기(Buffers and Working with Rate)

#### 7.1 개요(Overview)

이 섹션은 Akka Streams에서 버퍼(buffer)가 어떻게 동작하는지 다룹니다. 특히 상류(upstream)와 하류(downstream)의 처리율(rate)이 서로 다르거나, 처리량(throughput)에 급격한 스파이크(spike)가 발생하는 경우에 초점을 맞춥니다.

#### 7.2 비동기 연산자를 위한 버퍼(Buffers for Asynchronous Operators)

##### 내부 버퍼링 전략(Internal Buffering Strategy)

연산자가 `.async()`를 사용해 비동기적으로 실행되면, 요소 하나를 하류로 전달한 직후 곧바로 다음 메시지를 처리할 수 있습니다. 이는 파이프라이닝(pipelining)을 가능하게 하지만, 스레드 교차 경계(thread-crossing boundary)에서 비용을 발생시킵니다.

Akka Streams는 이를 최적화하기 위해 "윈도우 기반의 배칭 백프레셔 전략(windowed, batching backpressure strategy)"을 사용합니다. 공식 문서의 설명은 다음과 같습니다. 이 전략은 윈도우 기반(windowed)인데, 이는 Stop-And-Wait 프로토콜과 달리 여러 요소가 요소 요청(request)과 함께 동시에 "비행 중(in-flight)"일 수 있기 때문입니다. 또한 배칭(batching) 방식인데, 이는 윈도우 버퍼(window-buffer)에서 요소 하나가 빠져나갈 때마다 즉시 새 요소를 요청하는 것이 아니라, 여러 요소가 빠져나간 뒤에 여러 요소를 한꺼번에 요청하기 때문입니다.

##### 기본 버퍼 설정(Default Buffer Configuration)

기본 내부 버퍼 크기는 다음과 같이 설정됩니다.

```
akka.stream.materializer.max-input-buffer-size = 16
```

##### 사용자 정의 버퍼 크기 설정(Custom Buffer Size Configuration)

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

##### 내부 버퍼 문제 해결(Troubleshooting Internal Buffers)

시간(time) 또는 처리율(rate) 기반의 연산자가 예상치 못한 동작을 보일 때, 입력 버퍼를 1로 줄이는 것은 효과적인 첫 번째 문제 해결(troubleshooting) 단계인 경우가 많습니다.

#### 7.3 Akka Streams의 명시적 버퍼(Explicit Buffers)

`buffer()` 연산자는 설정 가능한 오버플로 전략(overflow strategy)과 함께, 명시적이고 애플리케이션 수준의(application-level) 버퍼링을 제공합니다.

##### 백프레셔 전략(Backpressure)

버퍼가 가득 차면 표준 백프레셔를 적용합니다.

```scala
jobs.buffer(1000, OverflowStrategy.backpressure)
```

##### 드롭 전략(Drop Strategies)

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

##### fail 전략(Fail Strategy)

버퍼가 용량(capacity)에 도달하면 스트림을 종료시킵니다(실패시킵니다).

```scala
jobs.buffer(1000, OverflowStrategy.fail)
```

#### 7.4 처리율 변환(Rate Transformation)

##### 빠른 생산자를 위한 conflate

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

##### 느린 생산자를 위한 expand와 extrapolate

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

##### 핵심 차이: extrapolate 대 expand

- **extrapolate**는 충족되지 못한 하류 수요(unmet downstream demand)가 있을 때만 `Iterator`를 생성하며, 이를 사용해 대기 중인 요청을 충족시킵니다.
- **expand**는 항상(always) `Iterator`를 생성하고 거기서 요소를 방출합니다. 이를 통해 (빈 `Iterator`를 사용하여) 원본 요소를 변환하거나 필터링할 수 있습니다.

이 플로우의 출력은 생산자가 충분히 빠르면 드리프트(drift)를 0으로 보고하고, 그렇지 않으면 더 큰 드리프트를 보고합니다.

---

### 참고 자료

- [Akka 공식 문서](https://doc.akka.io/libraries/akka-core/current/)
- [Stream Introduction](https://doc.akka.io/libraries/akka-core/current/stream/stream-introduction.html)
- [Streams Quickstart Guide](https://doc.akka.io/libraries/akka-core/current/stream/stream-quickstart.html)
- [Design Principles behind Akka Streams](https://doc.akka.io/libraries/akka-core/current/general/stream/stream-design.html)
- [Basics and working with Flows](https://doc.akka.io/libraries/akka-core/current/stream/stream-flows-and-basics.html)
- [Working with Graphs](https://doc.akka.io/libraries/akka-core/current/stream/stream-graphs.html)
- [Modularity, Composition and Hierarchy](https://doc.akka.io/libraries/akka-core/current/stream/stream-composition.html)
- [Buffers and working with rate](https://doc.akka.io/libraries/akka-core/current/stream/stream-rate.html)

---

## Akka Streams 고급과 연동

> 원본: https://doc.akka.io/libraries/akka-core/current/stream/index.html

---

### 목차

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

### 1. 동적 스트림 처리(Dynamic Stream Handling)

동적 스트림 처리(dynamic stream handling)는 그래프 연결을 사전에 정해 두지 않고도, 실행 시점(runtime)에 스트림의 완료(completion)와 라우팅(routing)을 제어할 수 있게 해 줍니다. 이 섹션에서는 스트림을 외부에서 종료시키는 킬 스위치(KillSwitch) 메커니즘과, 실행 중에 동적으로 팬-인(fan-in)/팬-아웃(fan-out)을 구성할 수 있는 허브(Hub) 구현을 다룹니다.

#### 1.1 KillSwitch: 스트림 완료 제어

킬 스위치(KillSwitch)는 연산자(operator)의 완료를 **외부에서** 제어할 수 있게 해 주는 도구입니다. 공식 문서에 따르면 KillSwitch는 "외부에서 `FlowShape` 연산자의 완료를 가능하게(allows the completion of operators of FlowShape from the outside)" 합니다. 이 인터페이스는 두 가지 주요 동작을 제공합니다.

- **`shutdown()`**: 스트림을 정상적으로 완료시킵니다. 업스트림(upstream)을 취소(cancel)하고 다운스트림(downstream)을 완료(complete)합니다.
- **`abort(Throwable)`**: 스트림을 실패(fail)시킵니다. 업스트림을 취소하고 다운스트림을 지정된 예외로 실패시킵니다.

두 메서드 중 하나가 처음 호출된 이후의 후속 호출은 모두 무시됩니다.

##### UniqueKillSwitch

`UniqueKillSwitch`는 정확히 하나의 머티리얼라이즈된 그래프 인스턴스(materialized graph instance)를 제어합니다. 이는 머티리얼라이제이션(materialization)의 결과로 획득되며, 개별 스트림을 독립적으로 관리할 수 있게 해 줍니다.

- **셧다운(shutdown) 예시**: 지연(delay)을 두고 카운트하는 소스에 적용했을 때 `shutdown()`을 호출하면, 스트림이 우아하게(graceful) 종료되며 마지막으로 방출(emit)한 요소가 반환됩니다.
- **어보트(abort) 예시**: `abort(error)`를 사용하면 지정된 예외로 스트림이 실패하게 되어, 다운스트림에서 에러 처리(error handling)를 수행할 수 있습니다.

##### SharedKillSwitch

`SharedKillSwitch`는 여러 스트림 인스턴스를 동시에 관리합니다. `UniqueKillSwitch`와 달리, 머티리얼라이제이션 이전에 `KillSwitches.shared(name)`를 통해 미리 생성되어야 합니다.

- **여러 스트림 제어**: 단일 `SharedKillSwitch`는 여러 개의 독립적인 머티리얼라이제이션을 함께 통제할 수 있어서, 서로 관련된 스트림들을 한꺼번에 조정(coordinated shutdown)하여 종료할 때 유용합니다.

#### 1.2 허브 구현: 동적 팬-인과 팬-아웃

##### MergeHub (동적 팬-인)

`MergeHub`는 동적 팬-인(dynamic fan-in)을 구현합니다. 여러 생산자(producer) 소스를 하나의 소비자(consumer)로 결합합니다. 주요 특성은 다음과 같습니다.

- 여러 생산자가 선착순(first-come-first-served) 방식으로 하나의 소비자에게 데이터를 공급합니다.
- 소비자가 처리 속도를 따라가지 못하면 모든 생산자가 백프레셔(backpressure)를 받습니다.
- 소비자에 연결된 후, 허브는 `Sink`로 머티리얼라이즈됩니다.

작업 흐름은 다음과 같습니다. 먼저 소스-소비자 그래프를 머티리얼라이즈하여 `Sink`를 얻고, 그 다음 그 `Sink`를 서로 다른 생산자들과 반복적으로 연결하여 사용합니다.

##### BroadcastHub (동적 팬-아웃)

`BroadcastHub`는 단일 생산자에서 여러 소비자로의 동적 팬-아웃(dynamic fan-out)을 가능하게 합니다.

- 하나의 생산자가 여러 구독자(subscriber)에게 요소를 공급합니다.
- 생산자의 속도는 가장 느린 소비자(slowest consumer)에 맞추어 조정됩니다.
- 허브는 생산자가 연결되는 `Sink`로 동작합니다.
- 각각의 `Source` 머티리얼라이제이션이 새로운 소비자를 추가합니다.

> **동작 주의**: 활성 구독자가 하나도 없으면, 허브는 (버퍼 전략으로 수정하지 않는 한) 요소를 버리지 않고 업스트림에 백프레셔를 겁니다.

##### 허브를 결합한 발행-구독(Publish-Subscribe)

`MergeHub`와 `BroadcastHub`를 연결하면 발행-구독(publish-subscribe) 채널을 만드는 실용적인 패턴을 구성할 수 있습니다.

1. `MergeHub`의 소스를 `BroadcastHub`의 싱크에 연결하고 함께 머티리얼라이즈합니다.
2. 구독자가 없을 때 백프레셔를 방지하기 위해 `Sink.ignore()`를 브로드캐스트 출력에 연결합니다.
3. 양 끝점을 `Flow.fromSinkAndSource()`를 사용해 하나의 `Flow`로 감쌉니다.
4. 외부에서 취소할 수 있도록 `KillSwitch`를 추가합니다.
5. `backpressureTimeout()`을 사용해 응답하지 않는 느린 구독자를 제거합니다.

결과로 만들어진 `Flow`는 여러 생산자와 소비자를 연결받을 수 있으면서도 단일 제어 지점(single control point)을 유지합니다.

##### PartitionHub

`PartitionHub`는 분할 함수(partitioning function)를 기반으로 단일 생산자에서 여러 소비자로 요소를 라우팅합니다.

- 각 요소는 (`BroadcastHub`와 달리) 정확히 하나의 소비자에게만 라우팅됩니다.
- 생산자의 속도는 가장 느린 소비자에 맞추어 조정됩니다.
- 인덱스를 기준으로 소비자를 선택하는 함수가 필요합니다.

분할 방식에는 두 가지가 있습니다.

- **무상태(stateless) 분할**: 소비자 수(consumer count)와 요소(element)를 받아 선택할 소비자의 인덱스를 반환하는 단순한 함수를 사용합니다.
- **유상태(stateful) 분할**: 팩토리 함수(factory function)가 각 머티리얼라이제이션마다 상태 보관자(state holder)를 생성하여, 라운드 로빈(round-robin)이나 큐 크기를 고려한 라우팅(queue-size-aware routing)을 가능하게 합니다.

> **고급 라우팅 예시**: `ConsumerInfo`의 `queueSize()` 접근자는 대략적인 버퍼 수준(approximate buffer level)을 알려 주어, 버퍼에 쌓인 요소가 더 적은(더 빠른) 소비자 쪽으로 라우팅할 수 있게 합니다.

#### 1.3 구현 패턴

- **여러 스트림 제어**: `SharedKillSwitch`는 단일 스위치로 여러 독립 스트림 인스턴스를 함께 제어하는 방식을 보여 주며, 관련 작업들의 생명주기(lifecycle) 관리에 유용합니다.
- **부하 분산(Load Balancing)**: 유상태 라우팅을 사용하는 `PartitionHub`는, 무작위 선택 대신 큐 깊이(queue depth)나 라운드 로빈 순서를 기준으로 지능적인 분배를 수행할 수 있게 합니다.
- **버퍼 관리(Buffer Management)**: 허브는 백프레셔를 전략적으로 처리합니다. `MergeHub`는 모든 생산자에게 집합적으로 백프레셔를 걸고, `BroadcastHub`는 가장 느린 구독자에 맞추어 조정합니다. 이를 통해 요소 손실을 방지하면서도 응답성을 유지합니다.

> 이 기능을 사용하려면 `"com.typesafe.akka" %% "akka-stream"` 의존성이 필요합니다.

---

### 2. 커스텀 스트림 처리(Custom Stream Processing, GraphStage)

`GraphStage` 추상화는 임의의 입력/출력 포트(port)를 갖는 커스텀 연산자(custom operator)를 만들 수 있게 해 줍니다. 다만 공식 문서가 강조하듯이, "커스텀 연산자가 가장 먼저 손을 뻗어야 할 도구는 아니며(A custom operator should not be the first tool you reach for), 플로우(flow)와 그래프 DSL을 사용해 연산자를 정의하는 것이 일반적으로 더 쉽습니다."

#### 2.1 핵심 개념

##### GraphStage 구조

`GraphStage`는 그 형태(shape)를 통해 연산자의 인터페이스를 정의하며, `Inlet`과 `Outlet` 객체로 포트를 지정합니다. 실제 실행 로직은 `GraphStageLogic` 안에 들어가며, 이는 머티리얼라이제이션마다 `createLogic()` 메서드를 통해 생성됩니다.

핵심 원칙:

> 모든 상태는 반드시 `GraphStageLogic` 안에 있어야 하며, 절대 둘러싼 `GraphStage` 안에 두면 안 됩니다(All state MUST be inside the GraphStageLogic, never inside the enclosing GraphStage). 이 상태는 모든 콜백(callback)에서 안전하게 접근하고 수정할 수 있습니다.

##### 포트 메커니즘

**출력 포트**(Output Port)는 세 가지 연산을 지원합니다.

- `push(out, elem)`: 다운스트림이 풀(pull)했을 때 요소를 전송합니다.
- `complete(out)`: 포트를 정상적으로 닫습니다.
- `fail(out, exception)`: 실패와 함께 포트를 닫습니다.

**입력 포트**(Input Port)는 다음을 가능하게 합니다.

- `pull(in)`: 업스트림에 요소를 요청합니다.
- `grab(in)`: 푸시(push)된 요소를 획득합니다.
- `cancel(in)`: 포트를 닫습니다.

`isAvailable()`, `hasBeenPulled()`, `isClosed()` 같은 질의(query) 메서드로 포트의 상태를 확인할 수 있습니다.

##### 핸들러 프레임워크(Handler Framework)

**OutHandler** (출력 포트용)는 다음을 제공합니다.

- `onPull()`: 다운스트림이 요소를 요청할 때 호출됩니다.
- `onDownstreamFinish()`: 다운스트림이 취소할 때 호출됩니다.

**InHandler** (입력 포트용)는 다음을 제공합니다.

- `onPush()`: 업스트림이 요소를 전송할 때 트리거됩니다.
- `onUpstreamFinish()`: 업스트림이 완료될 때 호출됩니다.
- `onUpstreamFailure()`: 업스트림이 실패할 때 호출됩니다.

#### 2.2 커스텀 선형 연산자(Custom Linear Operators)

선형 연산자는 하나의 입력과 하나의 출력을 갖는 `GraphStage[FlowShape[A, B]]`를 상속합니다.

- **Map 연산자 예시**: 단순한 변환으로, 수요(demand)를 업스트림으로 전달하고 요소를 다운스트림으로 전달합니다. `Map` 구현은 양방향 신호 흐름을 보여 줍니다. `onPull()`은 `pull(in)`을 호출하고, `onPush()`는 변환을 적용한 뒤 `push(out)`을 호출합니다.
- **Filter 연산자 예시**: 다대일(many-to-one) 변환으로, 조건에 따라 요소를 선택적으로 전달합니다. `Filter`는 조건에 부합하는 요소를 푸시하거나, 부합하지 않는 경우 다시 풀하여 선택적인 요소 전파를 달성합니다.
- **Duplicator 연산자 예시**: 일대다(one-to-many) 변환으로 유상태(stateful) 처리가 필요합니다. `Duplicator`는 요소 상태를 유지하며 복제본을 방출합니다. 중요한 점은, 완료 전에 버퍼에 남아 있는 요소를 방출하기 위해 `onUpstreamFinish()`를 오버라이드한다는 것입니다. 대안 구현으로는 `emitMultiple(out, elements)`를 사용할 수 있는데, 이는 여러 요소를 안전하게 방출하기 위해 핸들러를 일시적으로 교체합니다.

#### 2.3 포트 상태 기계(Port State Machine)

공식 문서는 유효한 전이(transition)를 보여 주는 상태 다이어그램을 제공합니다.

- **출력 포트**: `idle` → `waiting for pull` → `available` → `finished`
- **입력 포트**: `idle` → `waiting for push` → `available` → `finished`

#### 2.4 고급 기능

##### 타이머 지원(Timer Support)

`TimerGraphStageLogic`은 다음을 통해 스케줄링을 가능하게 합니다.

- `scheduleOnce(key, delay)`
- `scheduleAtFixedRate(key, initialDelay, interval)`
- `scheduleWithFixedDelay(key, initialDelay, interval)`

타이머 만료를 처리하려면 `onTimer(timerKey)`를 오버라이드합니다. 타이머는 생성자(constructor)에서는 스케줄할 수 없지만, `preStart()`에서는 동작합니다.

##### 비동기 사이드 채널(Asynchronous Side-Channels)

`getAsyncCallback()`은 외부 이벤트를 안전하게 주입할 수 있게 해 줍니다.

> 실행 엔진이 제공된 콜백을 스레드 안전하게(thread-safe) 호출하는 일을 처리합니다(The execution engine will take care of calling the provided callback in a thread-safe way).

외부 API는 콜백을 직접 호출하지 않고, 반환된 `AsyncCallback`에 대해 `invoke(event)`를 호출합니다.

##### 커스텀 머티리얼라이즈 값(Custom Materialized Values)

`NotUsed`를 넘어서는 값을 반환하려면 `GraphStage` 대신 `GraphStageWithMaterializedValue`를 상속합니다. `createLogicAndMaterializedValue()`를 오버라이드하여 로직과 값을 모두 반환합니다.

> **경고**: 로직이 실행되는 스레드와 머티리얼라이즈된 값을 보유한 스레드 양쪽에서 이 값에 접근하는 것에 대한 기본 동기화(built-in synchronization)는 제공되지 않습니다.

##### 속도 분리(Rate Decoupling)

버퍼와 같은 연산자는 업스트림과 다운스트림의 속도를 분리(decouple)합니다. `TwoBuffer` 예시는 독립적인 수요 관리를 보여 줍니다. `preStart()`가 다운스트림 수요 없이 업스트림 풀을 시작하여, 블록되기 전에 두 개의 요소를 버퍼링할 수 있게 합니다.

#### 2.5 생명주기와 자원(Lifecycle and Resources)

**정리(cleanup) 책임**은 핸들러 콜백이 아니라 `GraphStageLogic.postStop()`에 두어야 합니다. 이는 연산자의 완료, 실패, 또는 머티리얼라이저(materializer) 셧다운 시에 정리가 이루어지도록 보장합니다.

**완료 메서드**:

- `completeStage()`: 모든 출력을 닫고 모든 입력을 취소합니다.
- `failStage(exception)`: 모든 출력을 실패시키고 모든 입력을 취소합니다.

`setKeepGoing(true)`는 포트가 닫혀도 자동 셧다운이 일어나지 않게 하여 명시적인 완료 호출을 요구하지만, 누수(leak) 위험이 따릅니다.

#### 2.6 스레드 안전성 보장(Thread Safety Guarantees)

커스텀 연산자는 액터와 유사한(actor-like) 시맨틱을 제공합니다.

> 이 클래스들이 노출하는 콜백은 절대 동시에(concurrently) 호출되지 않습니다. 이 클래스들이 캡슐화하는 상태는 추가적인 동기화 없이도 제공된 콜백 안에서 안전하게 수정할 수 있습니다.

> **중요한 제약**: 제공된 콜백 외부에서는 절대 연산자 상태에 접근하지 마십시오. 미래(future) 콜백은 동시성 위험 때문에 내부 상태를 클로저(closure)로 포착할 수 없습니다.

#### 2.7 통합 패턴과 로깅

- `.via()` 연산자는 커스텀 연산자를 스트림에 통합합니다. 암시적(implicit) 스칼라 확장 메서드를 통해 `Source`와 `Flow`에 대해 DSL과 유사한 문법을 사용할 수 있습니다. 다만 `SubFlow` 확장은 Scala 2에서 실용적이지 않은 랭크-2 타입 추상화(rank-2 type abstraction)를 요구합니다.
- **로깅**: `GraphStageLogicWithLogging`과 `TimerGraphStageLogicWithLogging`은 액터 로깅과 유사하게 `log` 필드에 대한 접근을 제공합니다. 비동기 어펜더(asynchronous appender)가 블로킹을 막아 준다면 SLF4J를 직접 사용하는 것도 허용됩니다.

전형적인 연산자 작성 순서는 다음과 같습니다. (1) 포트를 갖는 형태(shape) 정의, (2) `createLogic()`에서 핸들러 등록, (3) `GraphStageLogic`에서 상태 관리, (4) `postStop()`에서 자원 정리, (5) `.via()`를 통한 외부 통합.

---

### 3. 스트림 에러 처리(Error Handling)

Akka Streams에서 연산자가 실패하면 보통 스트림 전체가 종료됩니다. 프레임워크는 전체 실패를 막고 스트림의 연속성을 유지하기 위한 여러 전략을 제공합니다.

#### 3.1 에러 로깅(Logging Errors)

`log()` 연산자는 스트림 모니터링을 가능하게 합니다. 실패가 발생하면 에러 메시지가 자동으로 로그에 남습니다.

```
[error logging] Upstream failed.
java.lang.ArithmeticException: / by zero
```

이 방식은 예외를 포착하지만, 스트림 종료 자체를 막지는 않습니다.

#### 3.2 recover

`recover` 연산자는 업스트림 실패 시 마지막 요소를 하나 방출한 다음 스트림을 정상적으로 완료합니다. 어떤 예외를 처리할지는 `PartialFunction`으로 결정합니다. 부합하지 않는(unmatched) 예외는 여전히 스트림을 실패시킵니다.

> **핵심 특성**: recover는 업스트림 실패 시 마지막 요소 하나를 방출한 뒤 스트림을 완료할 수 있게 해 줍니다(recover allows you to emit a final element and then complete the stream on an upstream failure).

예시 동작: `0 to 6`을 처리하다가 문제가 되는 값(예: 4, 5)을 만나면 복구(recovery)가 트리거되어, 스트림 완료 전에 에러 메시지를 출력합니다.

#### 3.3 recoverWithRetries

이 연산자는 실패한 업스트림을 새로운 업스트림으로 교체하여, 최대 횟수까지 재시도(retry) 로직을 가능하게 합니다. 역시 `PartialFunction` 매칭을 사용합니다.

> **결과**: 실패 후 대체 소스(alternative source)에서 스트림이 이어지며, 백업 데이터 스트림으로 우아하게 폴백(fallback)할 수 있습니다.

#### 3.4 지수 백오프를 사용한 재시작 연산자(Restart Operators with Exponential Backoff)

##### RestartSource, RestartSink, RestartFlow

이들은 지수 백오프(exponential backoff) 슈퍼비전을 구현하여, 시도 사이의 지연을 점점 늘려 가며 연산자를 재시작합니다. 이 패턴은 외부 자원이 일시적으로 사용 불가능할 때 빡빡한 재연결 루프(tight reconnection loop)를 방지합니다.

**설정 파라미터**:

- `minBackoff`: 최초 재시작 지연
- `maxBackoff`: 최대 지연 상한
- `randomFactor`: 변동량 추가(동기화된 재시작을 막기 위해 0.2 권장)
- `maxRestarts`: 전체 재시작 횟수 제한
- `maxRestartsWithin`: 재시작 횟수를 집계하는 시간 윈도우

> **중요 고려사항**: 재시작 간격에 추가적인 무작위성(randomness)을 더하면, 스트림들이 시간상 약간씩 다른 지점에서 시작하게 되어 큰 트래픽 스파이크를 피할 수 있습니다.

일반적인 사용처: 서버 비가용성 이후의 WebSocket 재연결.

#### 3.5 슈퍼비전 전략(Supervision Strategies)

슈퍼비전(supervision)은 세 가지 지시(directive)를 통해 연산자별 예외 처리를 가능하게 합니다.

- **Stop (기본값)**: 예외가 발생하면 스트림이 실패와 함께 완료됩니다. 모든 연산자의 기본 동작입니다.
- **Resume**: "요소가 버려지고 스트림이 계속됩니다(The element is dropped and the stream continues)." 누적된 상태(accumulated state)는 그대로 유지됩니다. 일시적인 처리 오류를 다룰 때 유용합니다.
- **Restart**: Resume와 유사하지만, 연산자 인스턴스를 재생성하여 "누적된 상태가 모두 지워집니다(any accumulated state is cleared)." `scan`처럼 값을 누적하는 유상태 연산자에 유익합니다.

##### 슈퍼비전 전략 적용하기

전략은 `ActorAttributes.supervisionStrategy(decider)`를 통해 `RunnableGraph`나 개별 플로우에 부착됩니다.

```scala
val decider: Supervision.Decider = {
  case _: ArithmeticException => Supervision.Resume
  case _ => Supervision.Stop
}
```

디사이더(decider) 함수는 예외를 검사하여 적절한 지시를 반환합니다.

#### 3.6 mapAsync 에러 처리

`mapAsync`와 `mapAsyncUnordered` 연산자는 `Future`/`CompletionStage` 실패에 대한 슈퍼비전을 지원합니다. `Supervision.resumingDecider()`를 사용하면 예외적으로 실패한 future의 요소를 버리고 다운스트림을 계속 진행할 수 있습니다.

> **예시 맥락**: 일부 레코드에 대해 실패하는 조회(lookup) 서비스를, 찾지 못한 항목은 버리고 유효한 항목만 처리하는 방식으로 우아하게 다룰 수 있습니다.

#### 3.7 중요한 주의사항(Caveats)

- **데드락 위험**: "요소를 버리는 것은 사이클(cycle)이 있는 그래프에서 데드락(deadlock)을 초래할 수 있습니다(Dropping elements may result in deadlocks in graphs with cycles)." 피드백 루프를 포함하는 그래프에서는 Resume/Restart 전략을 신중하게 설계해야 합니다.
- **GraphStage 제약**: 재시작 래퍼(restart wrapper) 내부에서 조건부 종료 전파(conditional termination propagation)를 수행하는 일부 커스텀 연산자는 예상과 다르게 동작할 수 있습니다. 특히 `eagerCancel = false`인 `Broadcast`는 까다로운 문제를 일으킵니다.

#### 3.8 설정 가이드

여러 접근법을 조합하면 견고한 에러 처리를 얻을 수 있습니다.

- 관측 가능성(observability)을 위해 `log()`를 사용
- 폴백 값과 함께 우아한 완료를 위해 `recover`를 적용
- 외부 서비스 복원력을 위해 `RestartSource`를 배치
- 연산자별 정책을 위해 슈퍼비전 전략을 부착
- 대량 작업에 대한 관용성을 위해 `mapAsync`와 `resumingDecider`를 사용

공식 문서는 "슈퍼비전이 스트림 연산자에 자동으로 적용되는 것이 아니라, 각 연산자가 명시적으로 구현해야 하는 것(supervision is not automatically applied to stream operators but instead something that each operator has to implement explicitly)"이라고 강조합니다. 즉, 모든 연산자가 이 전략을 지원하는 것은 아닙니다.

---

### 4. 스트리밍 IO 다루기(Working with Streaming IO)

Akka Streams는 백프레셔(back-pressure)를 투명하게 관리하면서 파일 IO와 TCP 연결을 다루는 도구를 제공합니다. 수동으로 다루는 Akka IO와 달리, Streams는 백프레셔를 자동으로 처리하여 네트워크와 파일 작업을 단순화합니다.

> 이 기능을 사용하려면 `akka-stream` 모듈을 프로젝트에 추가해야 합니다.

#### 4.1 스트리밍 TCP

##### 서버 측: 연결 수락(Accepting Connections)

`Tcp(system).bind()` 메서드는 수락된 클라이언트마다 `IncomingConnection` 요소를 방출하는 `Source`를 반환하는 서버를 생성합니다. 이 바인딩은 활성 소켓을 담은 `Future[ServerBinding]`을 만들어 냅니다.

- **기본 서버 설정**: `bind` 연산은 호스트와 포트를 받아, 리스닝을 시작하기 위해 머티리얼라이즈해야 하는 `Source`를 반환합니다. 들어오는 각 연결은 해당 연결 객체를 처리함으로써 개별적으로 다룰 수 있습니다.
- **에코 서버(Echo Server) 예시**: 각 연결마다, 개발자는 들어오는 `ByteString`을 처리하는 `Flow`를 정의합니다. `Framing.delimiter` 헬퍼는 구분자(예: 개행 문자)를 기준으로 바이트 스트림을 논리적 메시지로 청크(chunk)화합니다. 연결의 `handleWith` 메서드가 그 `Flow`를 소켓에 적용하여 양방향 데이터 교환을 관리합니다.
- **연결 처리**: 서버는 연산을 연쇄하여 데이터를 처리할 수 있습니다. 즉, 프레이밍된 메시지를 수신하고, 내용을 변환한 뒤, 응답을 다시 전송합니다. 예시는 받은 텍스트에 느낌표를 덧붙여 다시 에코하는 모습을 보여 줍니다.
- **연결 닫기**: 들어오는 `Flow`를 취소하거나 어느 한쪽을 끊어서 연결을 종료할 수 있습니다. 서버 소켓 자체는 연결 `Source`를 취소하여 셧다운할 수 있습니다.

##### 클라이언트 측: 아웃바운드 연결(Outgoing Connections)

`Tcp(system).outgoingConnection()` 팩토리는 원격 서버에 연결하기 위한 클라이언트 `Flow`를 생성합니다. 바인딩과 달리, 아웃바운드 연결은 커스텀 로직과 조인(join)할 수 있는 `Flow`를 반환합니다.

- **REPL 클라이언트 예시**: Read-Evaluate-Print-Loop 클라이언트는 대화형 TCP 통신을 보여 줍니다. 구현은 `Flow`를 사용하여 서버 응답을 처리하고, 그것을 출력하며, 사용자 입력을 읽어 명령을 전송합니다. `join` 메서드는 연결 `Flow`와 애플리케이션 로직을 하나의 통합된 스트림으로 결합합니다.

##### 백프레셔 사이클에서의 데드락 회피

백프레셔 데드락은 양쪽이 모두 전송하기 전에 입력을 기다릴 때 발생합니다. 클라이언트-서버 시나리오에서, 이를 해결하려면 한쪽이 먼저 통신을 시작해야 합니다.

- **해결 전략**: 서버(또는 적절한 쪽)가 초기 메시지를 주입하여 사이클을 끊습니다. 이는 단 하나의 "대화 시작자(conversation starter)" 요소를 갖는 `Source`를 처리 `Flow`에 병합(merge)함으로써 달성됩니다. 상호 백프레셔에도 불구하고 통신이 시작되도록 보장합니다.
- **프로토콜별 처리**: 많은 프로토콜은 어느 쪽이 먼저 말해야 하는지를 자연스럽게 규정합니다. 적절한 명령 파서(command parser)와 스트림 완료 로직(예: "BYE"나 "q" 명령 인식)을 구현하면 조정된 셧다운이 가능합니다.

##### 프레이밍 프로토콜(Framing Protocols)

TCP는 메시지 경계가 없는 바이트 스트림만 제공하므로 프레이밍(framing) 방식이 필요합니다.

- **구분자 기반(Delimiter-Based)**: `Framing.delimiter`는 프레임 종료 마커(예: `\n`)를 사용해 논리적 메시지를 분리하며, 최대 프레임 길이와 절단(truncation) 처리를 설정할 수 있습니다.
- **길이 접두(Length-Prefixed)**: `Framing.simpleFramingProtocol`은 길이 필드를 사용해 메시지를 구분하여, 고정 길이 프로토콜을 가능하게 합니다.
- **JSON 프레이밍**: `JsonFraming.objectScanner`는 `ByteString` 스트림에서 유효한 JSON 객체를 식별하고 분리하며, TCP 상의 JSON 프로토콜에 유용합니다.

##### TLS 지원

Akka는 TLS 변형을 제공합니다. `outgoingConnectionWithTls`, `bindWithTls`, `bindAndHandleWithTls`. 이들은 키스토어(keystore)/트러스트스토어(truststore) 정보를 갖춘 `SSLEngine` 설정을 요구합니다.

- **설정 과정**: 개발자는 키스토어를 로드하고, `TrustManagerFactory`와 `KeyManagerFactory` 인스턴스를 초기화하며, `SSLContext`를 구성하고, 클라이언트/서버 역할·암호 스위트(cipher suite)·프로토콜을 지정한 `SSLEngine` 인스턴스를 생성해야 합니다.

#### 4.2 스트리밍 파일 IO

- **파일 읽기**: `FileIO.fromPath()`는 파일로부터 `ByteString` 청크를 방출하는 `Source`를 생성합니다. 선택적 `chunkSize` 파라미터는 스트림 요소당 버퍼 크기를 제어합니다. 읽기는 간단합니다. 소스를 어떤 싱크에든 연결하여 처리하면 됩니다.
- **파일 쓰기**: `FileIO.toPath()`는 파일에 쓰기 위한 `ByteString` 청크를 받는 `Sink`를 생성하여, 파이프라인식 파일 처리를 위해 읽기 작업을 보완합니다.
- **디스패처 설정**: 파일 IO 연산은 파일 작업을 액터 시스템 연산으로부터 격리하는 전용 블로킹 디스패처(blocking dispatcher)를 사용합니다. 기본 디스패처(`akka.stream.materializer.blocking-io-dispatcher`)는 전역적으로 또는 `withAttributes()`와 커스텀 디스패처 지정을 사용해 연산자별로 커스터마이즈할 수 있어, 최적의 자원 활용을 보장합니다.

---

### 5. 액터와의 연동(Integration with Actors)

이 섹션에서는 스트림과 액터 기반 시스템을 결합하는 여러 접근법을 설명합니다. 이러한 기법들은 기존 API, 공유된 가변 상태(shared mutable state), 또는 스트림 실행 중 외부 영향을 받는 로직이 관여하는 시나리오를 다룹니다.

#### 5.1 Ask 패턴(ask 연산자)

`ask` 연산자는 백프레셔를 유지하면서 스트림 요소의 처리를 액터에 위임합니다. 공식 문서에 따르면, "스트림의 백프레셔는 ask의 `Future`/`CompletionStage`에 의해 유지되며, 주어진 병렬성(parallelism)보다 더 많은 메시지로 액터의 메일박스(mailbox)가 채워지지 않습니다."

주요 특성:

- 병렬성 설정과 무관하게 요소의 순서(ordering)가 유지됩니다.
- 액터는 각 메시지에 대해 반드시 `sender()`에게 응답해야 합니다.
- 응답(reply)이 다운스트림 요소가 됩니다.
- 대상 액터가 종료되면 `AskStageTargetActorTerminatedException`이 트리거됩니다.
- 타임아웃 실패는 `TimeoutException`을 발생시킵니다.

> 공식 문서는 "요소의 발신자(스트림)에게 응답하는 것이 필수이며, 이 ack 신호가 없으면 백프레셔로 해석된다(replying to the sender of the elements (the stream) is required as lack of those ack signals would be interpreted as back-pressure)"고 명시합니다.

#### 5.2 Sink.actorRefWithBackpressure

이 싱크는 스트림 요소를 액터에 전송하며, 백프레셔 제어를 위해 확인(acknowledgment) 메시지를 요구합니다. 패턴은 다음을 포함합니다.

- 스트림 시작을 알리는 초기 메시지
- 각 요소 이후의 확인(ack)
- 성공 시 완료 메시지
- 스트림 에러 시 실패 메시지

> 공식 문서는 액터 메일박스에 버퍼가 누적되는 것을 막기 위해 "요소의 발신자(스트림)에게 응답하는 것이 필수(replying to the sender of the elements (the stream) is required)"라고 강조합니다.

#### 5.3 Source.queue

이 소스는 외부 코드(액터 포함)에서 요소를 스트림으로 푸시할 수 있게 합니다. `offer` 메서드는 요소가 큐에 들어갔는지(enqueued), 버려졌는지(dropped), 실패했는지(failed)를 나타내는 `Future`/`CompletionStage`를 반환합니다. 공식 문서는 요소가 "스트림이 처리할 수 있을 때까지 버퍼링된다(will be buffered until the stream can process them)"고 명시합니다.

`OverflowStrategy.backpressure`를 사용하면, 버퍼 공간이 생길 때까지 offer future의 완료를 지연시켜 요소 손실을 방지합니다.

#### 5.4 Source.actorRef

머티리얼라이즈된 `ActorRef`로 전송된 메시지가 다운스트림으로 방출됩니다. `Source.queue`와 달리, 이 방식은 백프레셔 전략을 지원하지 않습니다. 공식 문서는 "스트림이 소비할 수 있는 것보다 빠른 속도로 전송하여 버퍼가 가득 차면 요소가 버려진다(elements will be dropped if the buffer is filled by sending at a rate that is faster than the stream can consume)"고 언급합니다.

스트림 완료는 완료(completion) 또는 실패(failure) 매칭 함수를 통해 트리거할 수 있습니다.

#### 5.5 타입드 액터(Typed Actor) 소스와 싱크

공식 문서는 네 가지 타입드(typed) 대안을 설명합니다.

- **`ActorSource.actorRef`**: 매칭되는 메시지를 방출하는 `ActorRef[T]`를 머티리얼라이즈합니다.
- **`ActorSource.actorRefWithBackpressure`**: 소스로부터 확인 기반(acknowledgment-based) 백프레셔를 제공합니다.
- **`ActorSink.actorRef`**: 백프레셔를 고려하지 않고 스트림 요소를 전송합니다.
- **`ActorSink.actorRefWithBackpressure`**: 백프레셔 신호와 함께 요소를 전송합니다.

#### 5.6 Pub/Sub 연동

공식 문서는 Akka 타입드 액터 토픽(topic) 시스템에 메시지를 구독하고 발행하기 위한 `Topic.source`와 `Topic.sink`를 언급합니다.

#### 5.7 중요한 구현 고려사항

공식 문서는 map 연산 안에서 `Sink.actorRef`나 tell 기반 패턴을 사용하는 것을 경고합니다. 왜냐하면 "대상 액터로부터의 백프레셔 신호가 없어(there is no back-pressure signal from the destination actor)" 메일박스 오버플로를 초래할 수 있기 때문입니다. 대신 `Sink.actorRefWithBackpressure`나 `mapAsync` 안의 `ask`를 선호할 것을 권장합니다.

라우터(router) 기반 시나리오의 경우, "응답의 순서가 중요하지 않으므로(the ordering of the replies is not important)" 수동으로 ask를 구현하는 `mapAsyncUnordered`를 사용할 것을 제안합니다.

---

### 6. Reactive Streams 상호운용성(Reactive Streams Interoperability)

Akka Streams는 Reactive Streams 표준을 구현합니다. 이 표준은 "논블로킹 백프레셔를 갖춘 비동기 스트림 처리(asynchronous stream processing with non-blocking back pressure)"를 제공합니다. 프레임워크는 여러 API 버전과 다른 리액티브 라이브러리 간의 상호운용성을 지원합니다.

#### 6.1 API 버전

Akka는 두 가지 별개의 Reactive Streams API 구현에 대한 상호운용성을 제공합니다.

1. **독립 Reactive Streams(Standalone Reactive Streams)**: `org.reactivestreams` 패키지를 사용합니다(Java 8 호환).
2. **Java 9+ 표준 라이브러리**: `java.util.concurrent.Flow` 네임스페이스를 사용하며, 전용 `JavaFlowSupport` 팩토리를 통해 접근합니다.

> 공식 문서는 "Java 8에서는 필요한 인터페이스가 단순히 존재하지 않기 때문에 `JavaFlowSupport`를 사용할 수 없다(it is not possible to use JavaFlowSupport on Java 8 since the needed interfaces simply is not available)"고 언급합니다.

#### 6.2 핵심 인터페이스

두 가지 기본 인터페이스는 `Publisher`와 `Subscriber`입니다. `Processor`는 둘을 결합하여, 스트림의 소비자(consumer)이자 생산자(producer)로 동작합니다.

#### 6.3 실용적인 상호운용성 패턴

##### Publisher로부터 Source 생성(Source from Publisher)

외부 publisher를 Akka 소스로 변환합니다.

```
Source.fromPublisher(tweets).via(authors).to(Sink.fromSubscriber(storage)).run()
```

이를 통해 Akka가 아닌 리액티브 라이브러리로부터 데이터를 수집할 수 있습니다.

##### Subscriber로부터 Sink 생성(Sink from Subscriber)

Akka 싱크를 외부 subscriber에 노출합니다.

```
Source.fromPublisher(tweets).via(authors).to(Sink.fromSubscriber(storage))
```

##### Source를 Publisher로 노출하기(Exposing Sources as Publishers)

Akka 소스를 발행 가능한 스트림으로 변환합니다.

```
Source.fromPublisher(tweets).via(authors).runWith(Sink.asPublisher(fanout = false))
```

**단일 구독 vs. 다중 구독**:

- `fanout = false`는 단 하나의 구독자만 지원합니다. 추가 시도는 `IllegalStateException`을 발생시킵니다.
- `fanout = true`는 여러 구독자로의 팬-아웃/브로드캐스팅을 가능하게 하며, 입력 버퍼가 구독자 간 속도 차이를 제어합니다.

##### Sink를 Subscriber로 노출하기(Exposing Sinks as Subscribers)

Akka 싱크로부터 subscriber 엔드포인트를 생성합니다.

```
authors.to(Sink.fromSubscriber(storage)).runWith(Source.asSubscriber[Tweet])
```

##### Processor 래핑(Processor Wrapping)

외부 `Processor` 인스턴스를 Akka 플로우로 통합합니다.

```
val flow: Flow[Int, Int, NotUsed] = Flow.fromProcessor(() => createProcessor)
```

재사용성을 위해 팩토리 함수(factory function)가 필요하다는 점에 유의하십시오.

#### 6.4 호환 라이브러리

공식 문서는 다음과 같은 다른 Reactive Streams 구현을 식별합니다.

- Reactor (1.1+)
- RxJava
- Ratpack
- Slick

이 표준화된 인터페이스는 서로 다른 스트리밍 프레임워크 간의 매끄러운 조합(composition)을 가능하게 합니다.

---

### 7. 스트림 테스트하기(Testing Streams)

Akka Streams는 여러 접근법을 통해 포괄적인 테스트 기능을 제공합니다. 공식 문서에 따르면, "Akka Stream 소스, 플로우, 싱크의 동작 검증은 다양한 코드 패턴과 라이브러리를 사용하여 수행할 수 있습니다(Verifying behavior of Akka Stream sources, flows and sinks can be done using various code patterns and libraries)."

#### 7.1 내장 소스, 싱크, 연산자

가장 간단한 테스트 접근법은 표준 Akka 컴포넌트를 활용하는 것입니다. 커스텀 싱크를 테스트할 때는 미리 정의된 데이터를 갖는 소스를 연결하고, 플로우를 실행한 뒤, 결과를 단언(assert)합니다. 무한 스트림을 생성하는 소스의 경우, `take` 연산자와 `Sink.seq`를 결합하면 초기 요소들이 기대 조건을 만족하는지 효과적으로 검증할 수 있습니다.

플로우를 테스트할 때는 스트림의 양 끝점이 모두 테스트 제어하에 있으므로, 엣지 케이스를 위한 유연한 소스 선택과 손쉬운 단언을 가능하게 하는 싱크를 사용할 수 있습니다.

#### 7.2 ActorRef를 사용한 TestKit 통합

Akka Stream은 액터와 기본적으로 통합됩니다. `Sink.actorRef` 컴포넌트는 들어오는 요소를 지정된 `ActorRef`로 보내, `TestProbe` 단언을 가능하게 합니다. 스트림 완료는 `onCompleteMessage` 파라미터를 통해 신호됩니다.

`Source.actorRef`는 역방향 제어를 제공합니다. 액터 참조를 통해 요소를 전송할 완전한 권한을 가지며, 커스텀 완료·실패 매처(matcher)가 스트림 생명주기를 통제합니다.

#### 7.3 Streams TestKit 모듈

전용 `akka-stream-testkit` 모듈은 두 가지 주요 컴포넌트, `TestSource`와 `TestSink`를 제공합니다.

`TestSink.probe`는 구독 프로브(subscription probe)로 머티리얼라이즈되어, "수요에 대한 수동 제어와 다운스트림으로 내려오는 요소에 대한 단언(manual control over demand and assertions over elements coming downstream)"을 가능하게 합니다. 주요 메서드:

- `request(n)`: 수요(demand)를 신호합니다.
- `expectNext()`: 특정 요소를 검증합니다.
- `expectComplete()`: 스트림 종료를 단언합니다.

`TestSource.probe`는 다운스트림 컴포넌트를 테스트하기 위한 역방향 제어를 제공합니다.

- `sendNext()`: 요소를 주입합니다.
- `sendError()`: 실패 조건을 트리거합니다.
- `expectCancellation()`: 취소 처리를 검증합니다.

이 컴포넌트들을 결합하면 순차적·비순차적 요소 검증을 모두 지원하는 포괄적인 플로우 테스트가 가능합니다. (이 밖에 자주 쓰이는 메서드로는 수요 신호와 요소 검증을 한 번에 수행하는 `requestNext`, 발신용 `sendComplete`, 구독 보장을 위한 `ensureSubscription` 등이 있습니다.)

#### 7.4 퍼징 모드(Fuzzing Mode)

공격적인 동시성 테스트를 위해, 설정에서 `akka.stream.materializer.debug.fuzzing-mode = on`을 활성화할 수 있습니다. 이는 경쟁 상태(race condition)를 드러내기 위해 "동시 실행 경로를 더 공격적으로 실행(exercises concurrent execution paths more aggressively)"합니다. 다만 성능에 상당한 영향을 주므로 엄격하게 테스트 환경에서만 사용해야 합니다.

---

### 8. 연산자 색인(Operators Index)

다음은 카테고리별로 정리한 Akka Streams 연산자 목록입니다.

#### 8.1 소스 연산자(Source Operators)

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

#### 8.2 싱크 연산자(Sink Operators)

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

#### 8.3 추가 싱크/소스 변환기(Additional Sink and Source Converters)

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

#### 8.4 파일 IO 싱크/소스(File IO Sinks and Sources)

| 연산자 | 설명 |
| --- | --- |
| `fromFile` | 파일 내용을 방출 |
| `fromPath` | 주어진 경로의 파일 내용을 방출 |
| `toFile` | 들어오는 `ByteString`을 파일에 씀 |
| `toPath` | 들어오는 `ByteString`을 파일 경로에 씀 |

#### 8.5 단순 연산자(Simple Operators)

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

#### 8.6 싱크/소스로 구성된 플로우 연산자(Flow Operators Composed of Sinks and Sources)

| 연산자 | 설명 |
| --- | --- |
| `fromSinkAndSource` | `Sink`와 `Source`로부터 `Flow`를 생성 |
| `fromSinkAndSourceCoupled` | `Sink`와 `Source`의 종료를 결합한 `Flow`를 생성 |

#### 8.7 비동기 연산자(Asynchronous Operators)

| 연산자 | 설명 |
| --- | --- |
| `mapAsync` | `Future`/`CompletionStage`를 반환하는 함수에 요소를 전달 |
| `mapAsyncPartitioned` | 파티션별 `Future` 제한을 갖는 비동기 매핑 |
| `mapAsyncUnordered` | 요소를 비동기로 전달하고 순서와 무관하게 결과를 방출 |

#### 8.8 타이머 구동 연산자(Timer Driven Operators)

| 연산자 | 설명 |
| --- | --- |
| `delay` | 각 요소를 지정된 시간만큼 지연 |
| `delayWith` | 동적으로 제어되는 시간만큼 요소를 지연 |
| `dropWithin` | 타임아웃이 발생할 때까지 요소를 버림 |
| `groupedWeightedWithin` | 시간 윈도우 또는 가중치 임계값으로 청크화 |
| `groupedWithin` | 시간 윈도우 또는 요소 개수 임계값으로 청크화 |
| `initialDelay` | 첫 요소를 지정된 시간만큼 지연 |
| `takeWithin` | 타임아웃 이내의 요소를 전달한 뒤 완료 |

#### 8.9 백프레셔 인지 연산자(Backpressure Aware Operators)

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

#### 8.10 중첩과 평탄화 연산자(Nesting and Flattening Operators)

| 연산자 | 설명 |
| --- | --- |
| `flatMapConcat` | 요소를 `Source`로 변환하고 연결(concatenation)로 평탄화 |
| `flatMapMerge` | 요소를 `Source`로 변환하고 병합(merging)으로 평탄화 |
| `flatMapPrefix` | 처음 n개 요소로 나머지 처리를 결정 |
| `groupBy` | 스트림을 별도의 출력 스트림으로 역다중화(demultiplex) |
| `prefixAndTail` | n개 요소를 취하고 나머지 스트림과의 쌍(pair)을 반환 |
| `splitAfter` | 술어가 true일 때 서브스트림(substream)을 종료 |
| `splitWhen` | 술어가 true일 때 새 서브스트림으로 분할 |

#### 8.11 시간 인지 연산자(Time Aware Operators)

| 연산자 | 설명 |
| --- | --- |
| `backpressureTimeout` | 요소 방출이 수요까지 타임아웃을 초과하면 실패 |
| `completionTimeout` | 스트림 완료가 타임아웃을 초과하면 실패 |
| `idleTimeout` | 요소 간 시간 간격이 타임아웃을 초과하면 실패 |
| `initialTimeout` | 첫 요소가 타임아웃 내에 도착하지 않으면 실패 |
| `keepAlive` | 업스트림이 설정 시간 동안 침묵하면 요소를 주입 |

#### 8.12 팬-인 연산자(Fan-in Operators)

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

#### 8.13 팬-아웃 연산자(Fan-out Operators)

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

#### 8.14 상태 감시 연산자(Watching Status Operators)

| 연산자 | 설명 |
| --- | --- |
| `monitor` | 메시지 모니터링을 위한 `FlowMonitor`로 머티리얼라이즈 |
| `watchTermination` | `Done` 또는 실패로 완료되는 `Future`로 머티리얼라이즈 |

#### 8.15 액터 연동 연산자(Actor Interop Operators)

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

#### 8.16 압축 연산자(Compression Operators)

| 연산자 | 설명 |
| --- | --- |
| `deflate` | `ByteString` 스트림을 Deflate 압축 |
| `gunzip` | `ByteString` 스트림을 Gzip 압축 해제 |
| `gzip` | `ByteString` 스트림을 Gzip 압축 |
| `inflate` | `ByteString` 스트림을 Deflate 압축 해제 |

#### 8.17 에러 처리 연산자(Error Handling)

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

### 참고 자료

- [Akka 공식 문서](https://doc.akka.io/libraries/akka-core/current/)
