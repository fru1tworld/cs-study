# Akka Typed Actors 기초

> 이 문서는 Akka 공식 문서의 "Actors" 섹션을 한국어로 번역한 것입니다.
> 원본: https://doc.akka.io/libraries/akka-core/current/typed/actors.html

---

## 목차

1. [액터(actor) 소개](#1-액터actor-소개)
2. [액터 생명주기(actor lifecycle)](#2-액터-생명주기actor-lifecycle)
3. [상호작용 패턴(interaction patterns)](#3-상호작용-패턴interaction-patterns)
4. [내결함성과 슈퍼비전(fault tolerance and supervision)](#4-내결함성과-슈퍼비전fault-tolerance-and-supervision)
5. [액터 디스커버리(actor discovery)](#5-액터-디스커버리actor-discovery)
6. [라우터(routers)](#6-라우터routers)
7. [스태시(stash)](#7-스태시stash)
8. [유한 상태 기계(finite state machines, FSM)](#8-유한-상태-기계finite-state-machines-fsm)
9. [참고 자료](#참고-자료)

---

## 1. 액터(actor) 소개

### 1.1 액터 모델(Actor Model)이란

액터 모델(Actor Model)은 동시성(concurrent)·분산(distributed) 시스템을 작성하기 위한 한 단계 높은 수준의 추상화(abstraction)를 제공합니다. 액터 모델은 개발자가 명시적인 잠금(locking)과 스레드 관리(thread management)를 직접 다루어야 하는 부담에서 벗어나게 해 주며, 그 결과 올바른(correct) 동시·병렬(concurrent and parallel) 시스템을 더 쉽게 작성할 수 있게 해 줍니다.

액터 모델은 1973년 칼 휴잇(Carl Hewitt)에 의해 처음 정의되었으며, 얼랭(Erlang) 언어가 이를 사용해 고도로 동시적인(highly concurrent) 통신(telecom) 시스템을 성공적으로 구축하면서 널리 알려지게 되었습니다. Akka의 API는 얼랭의 접근 방식으로부터 일부 구문 요소(syntactic elements)를 차용하고 있습니다.

### 1.2 모듈 정보와 의존성(dependencies)

Akka Actors를 사용하려면 프로젝트에 특정 의존성(dependency)을 추가해야 합니다. 현재 문서가 다루는 버전은 2.10.19입니다. 의존성은 Akka의 보안 라이브러리 저장소(secure library repository)에서 제공되며, 이를 사용하려면 https://account.akka.io/token 에 명시된 대로 보안 토큰이 포함된(secure, tokenized) URL이 필요합니다.

sbt 프로젝트에서 필요한 라이브러리는 `akka-actor-typed`와 `akka-actor-testkit-typed`입니다. Maven과 Gradle 설정도 제공되며, 의존성 관리를 위해 BOM(Bill of Materials) 방식을 사용합니다.

아티팩트(artifact) 상세 정보는 다음과 같습니다.

- JDK 버전: 11, 17, 21
- Scala 버전: 2.13.17, 3.3.7
- 라이선스(License): BUSL-1.1
- 상태(Status): Lightbend 지원으로 정식 지원됨(Supported)

### 1.3 행위(behavior)와 ActorRef

액터(actor)의 행위(behavior)는 그 액터가 메시지를 어떻게 처리하는지를 결정합니다. 액터의 행위는 `receive` 행위 팩토리(behavior factory)의 도움을 받아 정의됩니다(예: `Greeter`). 다음 메시지를 처리한 결과는 새로운 행위(new behavior)가 되며, 이 새 행위는 잠재적으로 이전 행위와 다를 수 있습니다. 즉, 액터는 메시지를 처리하면서 자신의 행위를 바꿈으로써 상태(state)를 표현할 수 있습니다.

`ActorRef`는 어떤 액터 인스턴스에 대한 참조(reference)를 나타냅니다. `ActorRef[T]`는 타입 매개변수 `T`를 가지며, 컴파일러(compiler)는 오직 이 타입의 메시지만 전송하도록 허용합니다. 다른 사용은 컴파일 오류(compiler error)가 되며, 이를 통해 액터 통신에 대한 타입 안전성(type safety)이 보장됩니다.

### 1.4 첫 번째 예제: Hello World

간단한 인사(greeting) 예제는 기본적인 액터 패턴을 보여 줍니다. 두 가지 메시지 타입이 정의됩니다.

- `Greet`: 액터에게 누군가에게 인사하라고 명령하는 메시지
- `Greeted`: 인사가 이루어졌음을 확인(confirm)하는 메시지

`Greet` 메시지는 `ActorRef` 필드를 포함하므로, 수신 액터(receiving actor)가 확인 메시지를 다시 돌려보낼 수 있습니다.

`HelloWorld` 액터 구현은 `receive` 행위 팩토리를 사용하여 들어오는 `Greet` 메시지를 처리합니다. 메시지를 받으면 정보를 로깅(logging)한 뒤, tell 연산자(`!`)를 사용하여 제공된 참조로 응답(reply)을 보냅니다.

두 번째 액터인 `HelloWorldBot`은 응답을 받아 자신의 행위를 변경(change behavior)함으로써 카운터(counter)를 관리합니다. 이는 가변 변수(mutable variable)가 아니라 행위 전이(behavioral transition)를 통해 상태를 관리하는 방식을 보여 줍니다.

`HelloWorldMain` 액터는 인사하는 액터(greeter)와 봇(bot) 액터를 모두 생성(spawn)하고, `SayHello` 메시지를 보내어 두 액터의 상호작용을 시작합니다.

`ActorSystem`은 이 액터들을 호스팅(host)하고 메시지를 받습니다.

```scala
val system: ActorSystem[HelloWorldMain.SayHello] =
  ActorSystem(HelloWorldMain(), "hello")

system ! HelloWorldMain.SayHello("World")
system ! HelloWorldMain.SayHello("Akka")
```

이 예제는 비동기 메시징(asynchronous messaging)을 보여 줍니다. tell 연산은 호출자의 스레드(caller's thread)를 막지 않는(non-blocking) 비동기 연산(asynchronous operation)입니다.

### 1.5 복잡한 예제: 채팅방(chat room) 프로토콜

좀 더 정교한 예제로 채팅방(chat room) 프로토콜은 다음과 같은 여러 중요한 패턴을 보여 줍니다.

- 여러 메시지 타입을 표현하기 위해 sealed trait와 case class(또는 Java에서는 인터페이스와 그 구현 클래스)를 사용하는 방법
- 자식 액터(child actor)를 통해 세션(session)을 관리하는 방법
- 행위를 변경함으로써 상태를 다루는 방법
- 프로토콜의 서로 다른 단계를 안전하게 표현하기 위해 여러 액터를 사용하는 방법

채팅방은 서로 다른 메시지 타입으로 구성된 프로토콜(protocol)을 정의합니다. 초기에 클라이언트는 `ActorRef[GetSession]`에 대한 접근 권한을 받아 세션을 요청할 수 있습니다. 세션이 수립되면 클라이언트는 메시지를 게시(post)할 수 있는 핸들(handle)을 담은 `SessionGranted` 메시지를 받습니다. 이후의 `MessagePosted` 이벤트는 클라이언트가 공개한(revealed) 주소로 전송됩니다.

이 구현은 각 클라이언트 세션마다 자식 액터(child actor)를 생성하여, 프로토콜을 액터 계층 구조(actor hierarchy) 안에 캡슐화(encapsulate)합니다. `PublishSessionMessage` 타입은 비공개(private) 가시성을 유지하여 권한 없는 프로토콜 위반(protocol violation)을 방지합니다. 즉, 클라이언트가 다른 화면 이름(screen name)으로 위장할 수 없습니다.

이는 액터가 단순히 Java 객체의 메서드 호출(method call)에 상응하는 것 이상을 표현할 수 있음을 보여 줍니다. 선언된 메시지 타입과 그 내용은 여러 액터에 걸쳐 진행되고 여러 단계에 걸쳐 발전할 수 있는 완전한 프로토콜(full protocol)을 기술합니다.

### 1.6 함수형 스타일(functional style)과 객체지향 스타일(object-oriented style)

두 가지 구현 방식이 제시됩니다.

**함수형 스타일(Functional Style)** 은 `Behaviors.receive`를 사용하며, 갱신된 상태를 가진 새로운 행위 인스턴스(new behavior instance)를 반환함으로써 상태를 관리합니다. 상태 변경은 가변 필드(mutable field)가 아니라 행위 전이(behavior transition)를 통해 표현됩니다.

**객체지향 스타일(Object-Oriented Style)** 은 `AbstractBehavior`를 확장(extend)하고 인스턴스 필드(instance field)에 상태를 유지합니다. `onMessage` 메서드는 메시지를 처리하고, 새로운 행위 인스턴스를 생성하는 대신 `this` 또는 `Behaviors.same()`을 반환합니다.

두 스타일은 각 액터의 구체적인 필요에 따라 혼합(mix)하여 사용할 수 있습니다. 두 접근 방식 중 어느 것을 선택할지에 대한 자세한 고려 사항은 스타일 가이드(Style Guide)를 참고하는 것이 권장됩니다.

### 1.7 AbstractBehavior API

객체지향 방식에서는 기본 행위 클래스(base behavior class)를 확장하고 메시지 핸들러(message handler)를 구현해야 합니다. 이 패턴은 전통적인 클래스 기반 설계(class-based design)와 유사하지만, 단일 스레드 메시지 처리(single-threaded message processing)에 대한 액터 모델의 보장은 그대로 유지합니다.

대안으로 `AbstractOnMessageBehavior` API가 있으며, 이는 `AbstractBehavior`가 사용하는 빌더 패턴(builder pattern)을 피하고 Java 17 이상의 패턴 매칭(pattern matching) 기능을 코드에서 직접 지원합니다.

### 1.8 액터 시스템(actor system)과 실행(execution)

애플리케이션은 일반적으로 JVM당 하나의 `ActorSystem`으로 구성되며, 그 안에서 많은 액터가 실행됩니다. `ActorSystem`은 액터가 실행되는 런타임 환경(runtime environment)을 제공합니다.

프레임워크의 핵심 개념은 다음과 같습니다.

- **ActorContext**: 자식 액터 생성(spawning child actors), 로깅(logging), 자기 자신에 대한 참조(self-reference) 등을 제공합니다.
- **시그널(Signals)**: 사용자 메시지(user message)와 구별되는 시스템 이벤트(system event)이며, 별도로 처리됩니다.
- **감시(Watching)**: `context.watch()`를 통해 액터의 종료(termination)를 감시(monitor)하고 `Terminated` 시그널을 받습니다.
- **셋업(Setup)**: `Behaviors.setup` 팩토리는 액터 시작 시점까지 행위 생성을 지연(defer)시키고, `ActorContext`를 매개변수로 제공합니다.

### 1.9 타입 안전성(type safety)과 프로토콜 정의

액터 프레임워크는 제네릭 타입(generic type)을 통해 컴파일 타임 타입 안전성(compile-time type safety)을 강조합니다. 메시지 프로토콜은 명시적으로 타입이 지정된 메시지 클래스(message class)와 `ActorRef` 선언을 통해 정의됩니다. `ActorRef[-T]`의 반공변성(contravariance)에 의해 `ActorRef[PublishSessionMessage]`가 기대되는 곳에서 `ActorRef[RoomCommand]`를 사용할 수 있습니다. 더 넓은 타입(broader type)이 더 좁은 프로토콜(narrower protocol)을 수용하기 때문입니다.

### 1.10 콘솔 출력 예시

Hello World 예제를 실행하면, 서로 다른 디스패처 스레드(dispatcher thread)들이 인사와 응답을 처리하는 로그 메시지가 출력됩니다. 이는 액터 시스템 전반에 걸친 동시 실행(concurrent execution)을 보여 줍니다.

---

## 2. 액터 생명주기(actor lifecycle)

### 2.1 개요

액터(actor)는 명시적으로 시작(start)되고 중지(stop)되어야 하는 상태를 가진 자원(stateful resource)입니다. 중요한 점은, 액터가 더 이상 참조되지 않더라도 자동으로 종료되지 않는다는 것입니다. 각 액터는 명시적으로 소멸(destroy)되어야 합니다. 부모 액터(parent actor)가 중지되면 그 모든 자식 액터(child actor)도 재귀적으로(recursively) 함께 중지됩니다. 또한 `ActorSystem`이 종료(shut down)되면 모든 액터도 자동으로 중지됩니다.

`ActorSystem`은 스레드를 할당하는 무거운(heavyweight) 구조이므로, 논리적 애플리케이션(logical application)당 하나만 생성해야 합니다. 일반적으로 JVM 프로세스당 하나의 `ActorSystem`을 둡니다.

### 2.2 ActorContext와 그 기능

`ActorContext`는 다음과 같은 여러 기능에 대한 접근을 제공합니다.

- 자식 액터 생성(spawning child actors)과 슈퍼비전(supervision)
- 다른 액터를 감시(watch)하여 종료 이벤트(termination event) 수신
- 로깅(logging) 기능
- 메시지 어댑터(message adapter) 생성
- ask 패턴(ask pattern)을 통한 요청-응답(request-response) 상호작용
- 액터의 자기 참조(self reference)에 대한 접근

#### 스레드 안전성(thread safety) 관련 주의 사항

`ActorContext`의 많은 메서드는 스레드 안전(thread safe)하지 않습니다. 이들은 Future/CompletionStage 콜백(callback) 내의 스레드에서 접근해서는 안 되며, 여러 액터 인스턴스 사이에서 공유(share)되어서도 안 됩니다. 이러한 연산은 오직 일반적인 액터 메시지 처리 스레드(ordinary actor message processing thread)에서만 사용해야 합니다.

### 2.3 액터 생성과 생성(spawning)

`ActorContext`에 접근하려면 행위를 `Behaviors.setup`으로 감싸야(wrap) 합니다. 이를 통해 자식 액터를 생성하는 데 필요한 컨텍스트(context)를 얻을 수 있습니다.

#### 가디언 액터(guardian actor)

가디언(guardian, 또는 루트(root)) 액터는 `ActorSystem`과 함께 생성되며, 계층 구조(hierarchy)에서 최상위(top-level) 액터로서 동작합니다. 간단한 애플리케이션에서는 가디언이 핵심 로직(core logic)을 담을 수도 있습니다. 그러나 더 큰 시스템에서는 가디언을 주로 작업의 초기화(initialization of tasks)와 애플리케이션의 초기 액터(initial actors) 생성에 사용해야 합니다.

가디언 액터가 중지되면 `ActorSystem`이 중지됩니다. 시스템 종료(system termination)는 조정된 종료(Coordinated Shutdown) 프로세스를 트리거하며, 이는 액터와 서비스를 특정 순서(specific order)대로 중지합니다.

#### 자식 액터 생성(spawning child actors)

자식 액터는 `ActorContext`의 `spawn` 메서드를 사용해 생성됩니다. 이 관계는 계층적(hierarchical)입니다. 즉, 자식 액터의 생명주기는 부모와 묶여 있습니다. 자식은 스스로를 중지하거나 언제든지 중지될 수 있지만, 결코 부모보다 오래 살아남을(outlive) 수는 없습니다.

생성 시 디스패처(dispatcher)를 지정하려면 `DispatcherSelector`를 사용합니다. 지정하지 않으면 액터는 기본 디스패처(default dispatcher)를 사용합니다.

### 2.4 SpawnProtocol 패턴

(예: HTTP 요청마다) 외부 소스로부터 동적으로 액터를 생성해야 하는 애플리케이션의 경우, `SpawnProtocol`이 미리 정의된 메시지 프로토콜과 행위 구현을 제공합니다. 이는 가디언 액터의 행위로 사용할 수 있으며, 초기화 작업을 위해 `Behaviors.setup`과 결합할 수도 있습니다.

이렇게 하면 시스템의 액터 참조에 `SpawnProtocol.Spawn`을 tell하거나 ask함으로써 외부에서 자식 액터를 시작할 수 있습니다. ask를 사용하면 즉시 생성하는 대신 `ActorRef`의 Future/CompletionStage를 반환받게 됩니다.

### 2.5 액터 중지(stopping actors)

액터는 다음 행위로 `Behaviors.stopped`를 반환함으로써 스스로를 중지합니다. 부모는 `ActorContext`의 `stop` 메서드를 사용하여, 자식이 현재 메시지 처리를 마친 뒤에 강제로 중지시킬 수 있습니다.

액터가 중지되면 `PostStop` 시그널을 받으며, 이는 자원 정리(cleaning up resources)에 사용할 수 있습니다. 개발자는 자원 정리를 위해 `PostStop`과 `PreRestart` 시그널을 모두 처리하는 것을 고려해야 합니다. 재시작(restart) 시에는 `PostStop`이 방출되지 않는다(not emitted)는 점에 주의해야 합니다.

### 2.6 액터 감시(watching actors)

액터는 `watch` 메서드를 사용해 다른 액터의 종료를 감시할 수 있습니다. 감시 대상 액터가 종료되면 감시하는 액터는 `Terminated` 시그널을 받습니다. 감시 대상은 임의의 `ActorRef`가 될 수 있으며, 반드시 자식 액터일 필요는 없습니다.

대안으로 `watchWith`가 있는데, 이는 `Terminated` 시그널 대신 사용자 정의 메시지(custom message)를 지정할 수 있게 해 줍니다. 이 방식은 추가 정보(additional information)를 메시지에 포함할 수 있기 때문에, `watch`와 `Terminated` 시그널을 사용하는 것보다 종종 더 선호됩니다.

주요 동작은 다음과 같습니다.

- 종료 메시지(terminated message)는 등록(registration)과 종료(termination)가 일어나는 순서와 무관하게(independent of the order) 생성됩니다.
- 여러 번 등록하면 여러 개의 메시지가 생성될 수 있습니다. 즉, 감시 대상 액터의 종료로 메시지가 생성되어 큐(queue)에 들어간 뒤, 그 메시지가 처리되기 전에 또 다른 등록이 이루어진 경우입니다.
- `context.unwatch(target)`을 통해 등록을 해제(deregistration)할 수 있습니다.
- 감시 대상 액터가 클러스터(Cluster)에서 제거된 노드(node)에 있을 때에도 종료 메시지가 전송됩니다.

---

## 3. 상호작용 패턴(interaction patterns)

### 3.1 보내고 잊기(fire and forget, tell)

**설명**: tell 연산자(`!`)를 사용하는 가장 기본적인 상호작용 방법으로, 응답을 기대하지 않고 메시지를 비동기적으로(asynchronously) 보냅니다. 이 메서드는 즉시 반환되며, 메시지가 실제로 처리되었다는 보장은 제공하지 않습니다.

**프로토콜 예시**: 문자열을 담은 단순한 `PrintMe` 메시지로, 액터는 메시지를 로깅한 뒤 이전 행위로 돌아갑니다.

**유용한 경우(useful when)**:
- 메시지 수신 확인(receipt confirmation)이 불필요할 때
- 전달 실패(delivery failure)에 대해 조치할 방법이 없을 때
- 메시지 수를 최소화하여 처리량(throughput)을 극대화하는 것이 중요할 때

**문제점(problems)**:
- 유입량이 처리 능력을 초과하면 메일박스 오버플로(mailbox overflow)가 발생할 수 있습니다.
- 분실된 메시지를 송신자가 알아채지 못합니다.
- 성공적인 처리에 대한 확인(acknowledgment)이 없습니다.

---

### 3.2 요청-응답(request-response, 직접 프로토콜)

**설명**: 수신자의 참조가 메시지 안에 인코딩(encode)되어 있어, 직접 응답을 보낼 수 있습니다. 송신자는 `ActorRef[Response]` 필드를 포함시키고, 수신자는 이를 사용하여 tell로 응답을 돌려보냅니다.

**프로토콜 예시**: 질의(query)와 응답용 `ActorRef[Response]`를 담은 `Request` 메시지. 응답자(responder)는 질의를 처리하고 제공된 참조로 직접 `Response` 메시지를 보냅니다.

**유용한 경우**:
- 한 액터가 여러 개의 응답 메시지를 돌려보낼 때
- 다른 액터로부터 지속적인 업데이트(ongoing updates)를 구독(subscribe)할 때
- 서로 알고 있는 당사자 간의 직접적인 양방향 통신(bidirectional communication)

**문제점**:
- 대부분의 액터는 자신의 프로토콜에서 임의의 응답 타입(arbitrary response type)을 받도록 지원하지 않습니다.
- 전달되지 않거나 처리되지 않은 메시지를 감지하기 어렵습니다.
- 요청 컨텍스트(예: 요청 ID)를 인코딩하지 않으면 응답을 요청에 연관(correlate)시키기 위해 별도의 액터가 필요합니다.

---

### 3.3 적응된 응답(adapted response, 메시지 어댑터)

**설명**: `context.messageAdapter()`를 사용하여 한 액터의 프로토콜에서 온 응답을 수신 액터가 이해할 수 있는 메시지로 변환(translate)하는 패턴입니다. 이는 수신 액터가 외부 메시지 타입을 받아들이지 않으면서도 호환되지 않는 프로토콜 사이에 타입 안전한 다리(type-safe bridge)를 놓습니다.

**프로토콜 예시**: `Frontend`가 `Backend`의 응답을 내부용 `WrappedBackendResponse` 메시지로 감싸서(wrap), 공개 프로토콜(public protocol)을 깨끗하게 유지하면서 백엔드 통신을 내부적으로 처리합니다.

**핵심 동작 원리**:
- 메시지 클래스당 하나의 메시지 어댑터만 존재할 수 있습니다. 새 어댑터를 등록하면 기존 어댑터를 대체합니다.
- 어댑터는 수신 액터의 컨텍스트 내에서 실행되므로 상태에 안전하게 접근할 수 있습니다.
- 적절한 생명주기 관리를 위해 어댑터는 `Behaviors.setup`이나 생성자(constructor)에서 등록합니다.

**유용한 경우**:
- 서로 다른 액터 메시지 프로토콜 간의 변환
- 다른 액터로부터 많은 응답 메시지를 구독할 때
- 내부 통신을 처리하면서도 공개 프로토콜을 깨끗하게 유지할 때

**문제점**:
- 응답 타입당 하나의 어댑터만 가능하므로, 서로 다른 대상에 대한 유연성이 제한됩니다.
- 전달/처리 실패를 감지하기 어렵습니다.
- 별도의 액터 없이 컨텍스트를 연관시키려면 (요청 ID 등의) 인코딩이 필요합니다.

---

### 3.4 액터 간 ask를 통한 요청-응답(request-response with ask between actors)

**설명**: `context.ask()`를 사용하여 자동 타임아웃 처리(automatic timeout handling)가 포함된 일대일(one-to-one) 요청-응답 상호작용을 생성합니다. 이 패턴은 임시(temporary) `ActorRef[Response]`를 필요로 하는 메시지를 구성한 뒤, 그 응답이나 실패(failure)를 송신자가 처리할 수 있는 메시지로 변환합니다.

**프로토콜 예시**: Dave가 Hal에게 질문을 ask하면, Hal은 임시 참조로 응답을 보내고, Dave의 응답 핸들러는 그 결과를 추가 처리를 위한 `AdaptedResponse` 메시지로 변환합니다.

**핵심 동작 원리**:
- 암묵적(implicit) `Timeout` 매개변수가 필요합니다.
- 변환 함수(transformation function)는 성공 응답과 실패를 모두 받습니다.
- 타임아웃은 응답이 아니라 `TimeoutException` 실패를 야기합니다.
- 송신 액터는 적응된 메시지를 받은 후에도 정상적으로 계속 메시지를 처리합니다.

**유용한 경우**:
- 단일 응답(single-response) 질의
- 계속 진행하기 전에 메시지 처리를 검증할 때
- 응답이 누락된 경우의 재시도(retry) 처리
- 수신자를 과부하시키지 않도록 미해결(outstanding) 요청을 추적할 때
- 프로토콜을 수정하지 않고 컨텍스트를 첨부할 때

**문제점**:
- ask당 단일 응답만 지원합니다.
- 수신자는 타임아웃을 알지 못하므로 타임아웃 이후에도 메시지를 처리할 수 있습니다.
- ask를 연쇄(chain)하면 적절한 타임아웃 값을 찾기 어려워집니다.

---

### 3.5 액터 외부에서의 ask를 통한 요청-응답(request-response with ask from outside an actor)

**설명**: 비액터 컨텍스트(non-actor context)인 외부 코드에서 `ask` 또는 `?`를 사용해 액터에게 메시지를 보냅니다. 이는 응답으로 완료되거나 `TimeoutException`으로 완료되는 `Future[Response]` 또는 `CompletionStage<Response>`를 반환합니다.

**의존성**: Scala에서는 `akka.actor.typed.scaladsl.AskPattern._`을 임포트(import)해야 하고, Java에서는 `AskPattern.ask()`를 사용합니다.

**프로토콜 예시**: 외부 서비스가 `CookieFabric` 액터에게 쿠키(cookie)를 ask하면, 그 ask는 `Cookies` 또는 `InvalidRequest` 응답으로 완료되는 Future를 반환합니다.

**유용한 경우**:
- 액터 시스템 외부에서 액터에 질의할 때
- 외부 API를 액터 시스템과 통합할 때

**문제점**:
- 반환된 Future의 콜백(callback)은 다른 스레드에서 실행되므로, 가변 상태(mutable state)에 대한 안전하지 않은 접근의 위험이 있습니다.
- ask당 단일 응답만 가능합니다.
- 타임아웃을 알지 못하는 수신자는 이미 완료된 요청을 계속 처리할 수 있습니다.

---

### 3.6 범용 응답 래퍼(generic response wrapper, StatusReply)

**설명**: `StatusReply[T]`를 사용하여 모든 상호작용 패턴에 걸쳐 성공/오류(success/error) 응답을 표준화합니다. 이 범용 래퍼(generic wrapper)는 성공 결과와 검증 오류(validation error)를 모두 지원하며, 클러스터 배포(clustered deployment)를 위한 직렬화기(serializer)가 내장되어 있습니다.

**프로토콜 예시**: 액터는 성공에 대해 `StatusReply.Success(value)`로, 실패에 대해 `StatusReply.Error(text)`로 응답합니다. `askWithStatus` 같은 ask 메서드는 성공을 자동으로 풀어 주고(unwrap) 오류를 Failure로 변환합니다.

**주요 타입**:
- `StatusReply.Ack()`는 `StatusReply[Done]` 타입의 미리 만들어진 확인 응답(acknowledgment)을 제공합니다.
- 오류는 텍스트 설명으로 표현하는 것이 선호되지만, 예외(exception)에는 타입 정보를 첨부할 수 있습니다.
- 내장 직렬화기는 클러스터링 시 보일러플레이트(boilerplate)를 줄여 줍니다.

**유용한 경우**:
- 응답이 성공/오류 쌍을 나타낼 때
- 여러 요청-응답 상호작용이 유사한 응답 패턴을 공유할 때
- 반복적인 상위 타입(supertype) 정의와 직렬화 설정을 피하고 싶을 때
- 검증 오류와 시스템 실패를 구별하고 싶을 때

---

### 3.7 응답 무시하기(ignoring replies)

**설명**: 응답을 정의하고 있지만 송신자가 그 응답에 관심이 없는 메시지를 처리하기 위해 `system.ignoreRef()`를 사용합니다. 이는 요청-응답을 사실상 보내고 잊기(fire-and-forget)로 전환합니다.

**사용법**: 요청을 보낼 때 `replyTo` 필드로 `context.system.ignoreRef`(Scala) 또는 `context.getSystem().ignoreRef()`(Java)를 전달합니다.

**문제점**:
- ignore 참조는 모든 메시지를 버리므로, 부주의하게 전달될 경우 위험이 따릅니다.
- 외부에서 `ask`와 함께 사용하면 Future가 영구적으로 타임아웃됩니다.
- ignore 참조를 감시(watch)해도 `Terminated` 시그널이 절대 발생하지 않습니다.

---

### 3.8 Future 결과를 자기 자신에게 보내기(send future result to self, pipeToSelf)

**설명**: `context.pipeToSelf()` 메서드는 비동기 `Future`/`CompletionStage`의 결과를 내부 메시지(internal message)로 감싸서, 블로킹(blocking) 없이 상태에 안전하게 접근할 수 있게 해 줍니다. 결과는 액터가 일반 receive 핸들러를 통해 처리하는 메시지로 매핑(map)됩니다.

**프로토콜 예시**: `CustomerRepository`가 Future를 반환하는 외부 API를 호출하고, 성공/실패를 내부용 `WrappedUpdateResult` 메시지로 매핑한 뒤, 스레드 안전성 문제 없이 이를 정상적으로 처리합니다.

**핵심 동작 원리**:
- 매핑 함수는 `Try[Value]` 또는 성공/실패를 분리한 매개변수를 받습니다.
- 매핑된 메시지는 액터의 일반 메일박스(mailbox)에 들어가 순서대로(ordered) 처리됩니다.
- 액터의 상태와 생명주기 안전성을 존중합니다.

**유용한 경우**:
- Future를 반환하는 데이터베이스나 외부 서비스 API를 통합할 때
- Future 완료 후 액터 처리를 계속할 때
- 원래 요청의 컨텍스트(요청 ID, 응답 참조)를 유지할 때
- 동시 연산(concurrent operation)의 개수를 안전하게 추적할 때

**문제점**:
- 래퍼 메시지(wrapper message)가 필요하여 보일러플레이트가 늘어납니다.
- 단순한 통합에서는 복잡성이 증가합니다.

---

### 3.9 세션별 자식 액터(per session child actor)

**설명**: 특정 상호작용을 위해, 특히 여러 응답을 집계(aggregate)할 때 임시 자식 액터(temporary child actor)를 생성합니다. 각 세션 액터(session actor)는 요청 컨텍스트와 응답 핸들러를 받고, 작업이 끝나면 완료한 뒤 스스로를 중지합니다.

**프로토콜 유연성**: 세션 액터는 프로토콜에 구애받지 않는(protocol-agnostic) 상호작용을 위해 `Any`/`Object` 타입을 받을 수 있으며, 의존 대상에게 타입 안전한 메시지를 보내기 위해 참조를 좁혀(narrow) 사용합니다.

**프로토콜 예시**: `Home` 액터가 임시 `PrepareToLeaveHome` 자식들을 생성하고, 각 자식은 서로 다른 액터로부터 열쇠(keys)와 지갑(wallet)을 요청하여 응답을 집계한 뒤, 둘 다 도착하면 중지됩니다.

**핵심 패턴**: 세션 액터는 제네릭 타입을 받지만, 알려진 의존 대상에게 보낼 때는 `.narrow[SpecificType]()`을 사용하여 타입을 좁힙니다.

**유용한 경우**:
- 단일 요청이 응답하기 전에 여러 상호작용을 필요로 할 때
- 재시도, 타임아웃, 진행 추적(progress tracking) 같은 복잡한 로직을 구현할 때
- 서로 관련 없는 응답들을 하나의 결과로 집계할 때
- 확인/재시도(acknowledgment/retry) 처리가 있는 적어도-한-번(at-least-once) 전달

**문제점**:
- 생명주기 관리의 복잡성이 자원 누수(resource leak)의 위험을 낳습니다.
- 세션 액터들이 동시에(concurrently) 실행되어 시스템 복잡성이 증가합니다.
- 세션 종료에 실패하는 시나리오를 놓치기 쉽습니다.

---

### 3.10 범용 응답 집계기(general purpose response aggregator)

**설명**: 타임아웃을 지원하면서 여러 응답을 집계하는, 재사용 가능하고 설정 가능한(configurable) 액터 패턴입니다. 내장 기능으로 제공되기보다는, 반복적인 집계 로직을 추출(extract)하는 방법을 보여 주는 문서의 형태로 제공됩니다.

**핵심 구성 요소**:
- `sendRequests` 함수: 모든 대상 액터에 요청을 보냅니다.
- `expectedReplies`: 기다릴 응답의 수
- `aggregateReplies` 함수: 수집된 응답들을 결합합니다.
- `timeout`: 부분 결과(partial result)를 집계하기 전까지의 기간(duration)

**구현 특징**:
- 메시지 어댑터가 모든 응답을 공통 타입(common type)으로 감쌉니다.
- 타임아웃이 발생하면 그때까지 도착한 응답들을 집계합니다.
- 기대 응답 수에 도달하면 조기에 완료합니다.

**유용한 경우**:
- 여러 곳에서 동일한 집계 패턴을 수행할 때
- 단일 요청이 서로 관련 없는 여러 액터로부터 응답을 받아야 할 때
- 타임아웃 시 부분 결과를 집계해야 할 때
- 확인 응답을 통한 회복력(resilience)을 구현할 때

**문제점**:
- 제네릭 타입 소거(type erasure)가 타입 매개변수를 가진 프로토콜을 복잡하게 만듭니다.
- 세션 생명주기 관리가 누수의 위험을 낳습니다.
- 자식의 동시 실행이 복잡성을 증가시킵니다.

---

### 3.11 지연 꼬리 자르기(latency tail chopping)

**설명**: 초기 지연 후 여러 액터에게 "백업 요청(backup request)"을 보냄으로써 꼬리 지연(tail latency)을 줄이는 기법입니다. 첫 번째 응답이 채택되고(wins), 이후의 응답은 무시됩니다. 이는 느린 응답자(slow responder)를 기다릴 확률을 낮춥니다.

**핵심 메커니즘**: 첫 번째 요청이 `nextRequestAfter` 기간 내에 응답하지 않으면, 다른 액터에게 요청을 보냅니다. `finalTimeout`이 작업을 완료할 때까지 이를 계속하며, 응답이 하나도 없으면 기본값(default value)을 반환합니다.

**프로토콜 패턴**:
- 여러 번의 백업 시도를 가능하게 하기 위해 요청 수를 추적합니다.
- 첫 번째 응답이 오면 조기에 반환합니다.
- 모든 요청이 실패하면 타임아웃 값으로 폴백(fallback)합니다.

**이론적 배경**: 대규모 서비스에서 중복 백엔드(redundant backend)에 작업을 병렬화하여 응답 시간을 줄이는 것에 관한 제프 딘(Jeff Dean)의 연구에 기반합니다.

**유용한 경우**:
- 여러 액터가 동일한 작업을 수행할 수 있을 때
- 가끔 발생하는 느린 응답자가 용납할 수 없는 꼬리 지연을 야기할 때
- 모든 액터가 느릴 경우 기본/캐시 응답이 허용될 때
- 요청 중복(request redundancy)이 용인될 때

**문제점**:
- 백업 요청을 통해 추가 부하(extra load)가 발생합니다.
- 여러 개의 동일한 서비스 백엔드가 필요합니다.
- 주 액터가 결국 응답할 경우 자원을 낭비할 수 있습니다.

---

### 3.12 자기 자신에게 메시지 예약하기(scheduling messages to self)

**설명**: `ActorContext`의 타이머(timer) 메서드를 사용하여 액터 자기 자신에게 전달되는 메시지를 예약합니다. 이를 통해 스레드를 막지 않고 타임아웃, 주기적 동작, 지연 연산 등을 구현할 수 있습니다.

**주요 기능**: 타이머는 단일(single) 또는 반복(recurring) 메시지를 시작할 수 있으며, 액터 행위 내에서 세션 타임아웃, 하트비트(heartbeat), 주기적 청소(periodic housekeeping) 같은 사용 사례를 지원합니다.

---

### 3.13 샤딩된 액터에 응답하기(responding to a sharded actor)

**설명**: 샤딩(sharding)이 도입한 라우팅 계층(routing layer)을 고려하여, 적절한 응답 참조를 가진 메시지를 구성함으로써 샤딩된 액터에게 메시지를 보냅니다.

---

### 3.14 Scala 3 유니온 타입(union type) 향상

최신 Scala에서는 유니온 타입(`Command | Response`)을 사용하여 일부 패턴을 단순화할 수 있습니다. 이를 통해 액터가 래퍼 메시지 없이 여러 프로토콜 타입을 받을 수 있습니다. `.narrow` 메서드는 내부 유니온 타입을 받으면서도 공개 프로토콜의 타입 안전성을 유지합니다.

---

## 4. 내결함성과 슈퍼비전(fault tolerance and supervision)

### 4.1 핵심 개념

Akka의 내결함성(fault tolerance) 프레임워크는 두 가지 중요한 실패 모드(failure mode)를 구별합니다.

- **검증 오류(validation error)**: 들어온 명령(command) 데이터가 유효하지 않을 때 발생합니다. 이는 예외(exception)를 발생시키기보다는 액터 프로토콜의 일부로 모델링되어야 합니다.
- **실패(failure)**: 끊어진 데이터베이스 연결처럼 예기치 못했거나 액터의 통제를 벗어난 무언가를 나타냅니다.

이 프레임워크는 "그냥 죽게 두라(let it crash)" 철학을 적용할 것을 권장합니다. 복구 로직(recovery logic)을 비즈니스 로직(business logic)과 뒤섞기보다는, 그 책임을 시스템의 다른 곳으로 옮깁니다.

슈퍼비전(supervision) 메커니즘은 특정 예외 타입이 액터 내에서 발생했을 때 무슨 일이 일어나야 하는지를 선언함으로써 실패를 처리합니다. 기본적으로 타입드 액터(typed actor)는 슈퍼비전 전략(supervision strategy)이 정의되어 있지 않으면 예외가 던져질 때 중지(stop)됩니다. 이는 기본적으로 재시작(restart)하는 클래식 Akka와의 중요한 차이점입니다.

### 4.2 슈퍼비전 전략(supervision strategy) 구현

슈퍼비전을 구현하려면 실제 액터 행위를 `Behaviors.supervise`로 감쌉니다(wrap). 다음은 `IllegalStateException` 발생 시 액터를 재시작하는 기본 예제입니다.

```java
Behaviors.supervise(behavior)
    .onFailure(IllegalStateException.class, SupervisorStrategy.restart());
```

또는 실패를 무시하고 다음 메시지를 계속 처리하도록 재개(resume)하려면 다음과 같이 합니다.

```java
Behaviors.supervise(behavior)
    .onFailure(IllegalStateException.class, SupervisorStrategy.resume());
```

### 4.3 고급 재시작 전략(advanced restart strategies)

더 정교한 접근 방식은 재시작 빈도(restart frequency)를 제한합니다. 예를 들어 10초 윈도우(window) 내에서 10번을 초과하지 않도록 재시작하려면 다음과 같이 합니다.

```java
Behaviors.supervise(behavior)
    .onFailure(
        IllegalStateException.class,
        SupervisorStrategy.restart().withLimit(10, Duration.ofSeconds(10)));
```

여러 개의 슈퍼비전 계층(supervision layer)을 중첩(nest)된 `supervise` 호출을 통해 서로 다른 예외를 각기 다른 전략으로 처리할 수 있습니다.

```java
Behaviors.supervise(
    Behaviors.supervise(behavior)
        .onFailure(IllegalStateException.class, SupervisorStrategy.restart()))
    .onFailure(IllegalArgumentException.class, SupervisorStrategy.stop());
```

### 4.4 함수형 스타일에서의 행위 감싸기(behavior wrapping in functional style)

행위를 변경함으로써 상태를 저장하는 함수형 스타일 액터의 경우, 슈퍼비전은 최상위 레벨(top level)에서만 감싸야 합니다.

```scala
def apply(): Behavior[Command] =
  Behaviors.supervise(counter(1)).onFailure(SupervisorStrategy.restart)
```

반환되는 각 행위는 슈퍼바이저(supervisor)로 자동으로 다시 감싸지므로(re-wrapped), 슈퍼비전 선언을 중복할 필요가 없습니다.

### 4.5 재시작 중의 자식 액터 관리(child actor management during restart)

부모 액터가 재시작하면, 자원 누수를 방지하기 위해 자식 액터들은 기본적으로 중지됩니다. 자식 액터는 일반적으로 `setup` 블록 안에서 생성되며, 이 블록은 부모 재시작 시 다시 실행됩니다.

그러나 슈퍼비전을 `setup` 블록 안에 두고 `withStopChildren(false)`를 사용하면 부모 재시작 시에도 자식 액터를 보존할 수 있습니다.

```java
Behaviors.setup(
    ctx -> {
      final ActorRef<String> child1 = ctx.spawn(child(0), "child1");
      final ActorRef<String> child2 = ctx.spawn(child(0), "child2");

      return Behaviors.<String>supervise(
          Behaviors.receiveMessage(msg -> { ... }))
          .onFailure(SupervisorStrategy.restart().withStopChildren(false));
    });
```

이 구성은 `setup` 블록이 최초 시작(initial startup) 시에만 실행되도록 보장하여, 부모 재시작에 걸쳐 자식 액터 참조를 보존합니다.

### 4.6 PreRestart 시그널

슈퍼비전 대상 액터가 재시작하기 전에, 그 액터는 `PreRestart` 시그널을 받습니다. 이는 `PostStop` 시그널과 유사하게 자원 정리를 가능하게 합니다. `PreRestart`에서 반환된 행위는 무시됩니다.

```java
Behaviors.receiveMessage(...)
    .receiveSignal((ctx, signal) -> {
      if (signal instanceof PreRestart || signal instanceof PostStop) {
        resource.close();
      }
      return Behaviors.same();
    })
```

재시작 중에는 `PostStop`이 방출되지 않으므로, `PreRestart`와 `PostStop`을 모두 처리하면 포괄적인(comprehensive) 자원 정리가 보장됩니다.

### 4.7 실패를 계층 위로 올리기(bubbling failures up the hierarchy)

부모는 자식을 감시(watch)함으로써 실패 처리 결정을 위로 떠넘길 수 있습니다. 자식이 실패로 인해 종료되면, 부모는 그 원인(cause)을 담은 `ChildFailed` 시그널을 받습니다. `ChildFailed`는 `Terminated`를 확장(extend)하므로, 중지와 실패를 구별할 필요가 없을 때는 통합하여 처리할 수 있습니다.

부모가 `Terminated`를 처리하지 않으면, `DeathPactException`으로 실패합니다. 이를 통해 각 액터가 구현 세부 사항(implementation detail)을 노출하지 않으면서 부모에게 알리는 계층적 실패 전파(hierarchical failure propagation)가 가능해집니다. 직속 부모(immediate parent)는 원래 예외에 접근할 수 있는 반면, 더 상위의 부모는 `DeathPactException`만 보게 됩니다.

예외를 여러 계층에 걸쳐 위로 올려야 하는 시나리오에서는 명시적인 처리가 필요합니다. 각 액터가 `Terminated`를 잡아서(catch) 예외를 다시 던집니다(rethrow).

```java
Behaviors.watch(child)
    .receiveSignal((ctx, signal) -> {
      if (signal instanceof ChildFailed) {
        throw ((ChildFailed) signal).getCause();
      }
      return Behaviors.same();
    })
```

이 계층적 접근 방식은 최상위 슈퍼바이저가 포괄적인 복구 결정을 내리도록 하는 한편, 하위 레벨은 당장의 관심사(immediate concern)에 집중하게 합니다.

---

## 5. 액터 디스커버리(actor discovery)

### 5.1 개요

액터 디스커버리(actor discovery)는 생성자 매개변수나 메시지를 통한 직접적인 참조 전달이 비현실적일 때, 액터가 다른 액터를 찾아 통신할 수 있게 하는 과제를 다룹니다. 이는 액터가 여러 노드에 걸쳐 동작하는 분산 클러스터(distributed cluster) 시나리오에서 특히 중요합니다.

### 5.2 액터 참조 얻기(obtaining actor references)

프레임워크는 액터 참조를 획득하는 두 가지 주요 메커니즘을 제공합니다. 첫 번째는 액터를 직접 생성하고 그 참조를 생성자 매개변수나 메시지에 담아 액터 사이에 전달하는 것입니다. 그러나 이 접근 방식이 부적절할 때 — 예를 들어 서로 다른 클러스터 노드에 있는 액터 간 상호작용을 부트스트랩(bootstrap)할 때, 또는 전통적인 의존성 주입(dependency injection)을 적용할 수 없을 때 — 리셉셔니스트(Receptionist) 서비스가 대안적인 디스커버리 패턴을 제공합니다.

### 5.3 리셉셔니스트(the Receptionist)

리셉셔니스트(Receptionist)는 로컬과 클러스터 배포를 모두 지원하는 동적 레지스트리(dynamic registry) 서비스로 동작합니다. 메시지 기반 API(message-based API)를 통해 작동하며, 액터가 디스커버리를 위해 스스로를 등록(register)하고 다른 액터가 등록된 서비스를 찾을 수 있게 합니다.

#### 등록 과정(registration process)

액터는 `ServiceKey`를 사용해 리셉셔니스트에 등록합니다. `ServiceKey`는 관련된 서비스 인스턴스(service instance)들을 그룹화하는 타입이 지정된 식별자(typed identifier)입니다. 액터가 초기화될 때, 자신의 서비스 키와 자기 참조(self-reference)를 담은 `Receptionist.Register` 메시지를 시스템의 리셉셔니스트 인스턴스로 보냅니다.

예를 들어, `PingService`는 다음과 같이 자신을 등록합니다.

```scala
context.system.receptionist ! Receptionist.Register(PingServiceKey, context.self)
```

여러 액터가 동일한 `ServiceKey`에 대해 등록할 수 있으며, 이는 디스커버리에 사용할 수 있는 서비스 인스턴스의 모음(collection)을 만듭니다.

#### 액터 찾기: Listing 응답(finding actors: the Listing response)

클라이언트 액터가 서비스를 발견해야 할 때, 단일 스냅샷(single snapshot)을 위해 `Receptionist.Find` 메시지를 보내거나, 지속적인 업데이트(continuous update)를 위해 `Receptionist.Subscribe` 메시지를 보냅니다. 두 질의 모두 요청된 `ServiceKey`에 일치하는 액터 참조들의 `Set`을 담은 `Listing` 응답을 반환합니다.

`Listing`은 해당 키에 대해 등록된 액터 참조들의 `Set`을 제공합니다.

#### 구독 모델(subscription model)

`Receptionist.Subscribe` 방식은 등록 변경(registration change)에 대한 동적 인식(dynamic awareness)을 제공합니다. 구독 시, 리셉셔니스트는 현재 등록된 액터들을 담은 `Listing`을 즉시 보내고, 이후 등록이 변경될 때마다 갱신된 `Listing` 메시지를 계속 보냅니다. 이로 인해 구독은 사용 가능한 서비스에 대한 실시간 인식(real-time awareness)이 필요한 시나리오에 이상적입니다.

가디언 액터가 구독하는 방법은 다음과 같습니다.

```scala
context.system.receptionist ! Receptionist.Subscribe(PingService.PingServiceKey, context.self)
```

구독한 액터는 그 후 `Listing` 메시지를 받습니다.

```scala
case PingService.PingServiceKey.Listing(listings) =>
  listings.foreach(ps => context.spawnAnonymous(Pinger(ps)))
```

#### Find 질의(find queries)

일회성 조회(one-time lookup)의 경우 `Receptionist.Find` 패턴이 더 적합합니다. 이 접근 방식은 지속적인 구독을 수립하지 않고 현재 상태(current state)를 질의합니다. 메시지 어댑터는 `Receptionist.Listing` 응답을 요청 액터의 메시지 타입으로 변환하는 데 도움을 줍니다.

```scala
val listingResponseAdapter = context.messageAdapter[Receptionist.Listing](ListingResponse.apply)
context.system.receptionist ! Receptionist.Find(PingService.PingServiceKey, listingResponseAdapter)
```

### 5.4 등록 해제(deregistration)

더 이상 서비스 등록이 필요 없는 액터는 `Receptionist.Deregister`를 사용해 등록을 해제할 수 있습니다. 이 명령은 로컬 리셉셔니스트에서 연관을 제거하고 모든 구독자(subscriber)에게 통지합니다. 등록 해제 명령은 선택적으로 로컬 제거를 확인하는 확인 응답(acknowledgement)을 반환할 수 있으나, 이 확인 응답이 모든 구독자가 인스턴스 제거를 확인했음을 보장하지는 않습니다. 즉, 이후로도 한동안 구독자로부터 메시지를 받을 수 있습니다.

```scala
context.system.receptionist ! Receptionist.Deregister(PingService.PingServiceKey, context.self)
```

### 5.5 클러스터 리셉셔니스트(cluster receptionist)

클러스터 배포에서, 리셉셔니스트는 분산 데이터(distributed data) 메커니즘을 사용하여 자신의 레지스트리를 노드 전반에 자동으로 분산시킵니다. 이로써 각 노드가 결국 일관된(eventually consistent) 서비스 정보를 유지하게 됩니다. 등록된 액터는 로컬에 머무르지 않고 클러스터 전역(cluster-wide)에서 발견 가능해집니다.

클러스터 리셉셔니스트는 표준 `Listing`을 클러스터 인식 필터링(cluster-aware filtering)으로 향상시킵니다. 표준 질의는 클러스터 멤버십(membership)과 연결성을 존중하여 도달 가능한(reachable) 액터만 반환합니다. 그러나 도달 불가능한(unreachable) 인스턴스를 포함한 전체 집합은 `Listing.allServiceInstances`를 통해 여전히 접근할 수 있습니다.

클러스터 시나리오에서는 직렬화(serialization)가 중요해집니다. 원격 액터(remote actor)와 교환되는 모든 메시지가 직렬화를 지원해야 하기 때문입니다.

### 5.6 생명주기 관리(lifecycle management)

레지스트리는 동적인 특성을 보입니다. 새로운 등록은 시스템 생애 동안 계속 일어납니다. 등록된 액터가 중지될 때, 명시적인 등록 해제가 일어날 때, 또는 호스팅 노드가 클러스터를 떠날 때 항목(entry)은 자동으로 제거됩니다.

### 5.7 확장성 고려 사항(scalability considerations)

리셉셔니스트는 적당한 규모의 서비스 모집단(service population)에 대해 실용적인 디스커버리를 제공하며, 수천 개에서 수만 개(up to thousands or tens of thousands)의 서비스를 처리할 수 있습니다. 그러나 서비스 수가 이보다 현저히 많은 시스템에서는, 리셉셔니스트가 액터 간의 초기 접촉(initial contact)만을 중개하고 이후의 상호작용은 애플리케이션 고유 로직(application-specific logic)이 관리하는 대안적인 패턴을 사용해야 합니다.

---

## 6. 라우터(routers)

### 6.1 개요

Akka Typed는 메시지를 여러 액터에 분배(distribute)하여 병렬 처리(parallel processing)를 가능하게 하는 라우터(router)를 제공합니다. 어떤 경우에는 동일한 타입의 메시지를 액터들의 집합에 분배하여 메시지가 병렬로 처리되도록 하는 것이 유용합니다.

라우터 자체는 들어오는 메시지를 라우티(routee)들의 집합 중 하나의 수신자에게 전달하는 액터입니다. Akka Typed에는 두 가지 주요 라우터 타입, 즉 풀 라우터(pool router)와 그룹 라우터(group router)가 있습니다.

### 6.2 풀 라우터(pool router)

#### 개념과 동작

풀 라우터는 제공된 행위 정의를 사용하여 자식 액터들을 생성한 뒤, 그 자식들에게 메시지를 전달합니다. 풀 라우터는 라우티 `Behavior`로 생성되며, 그 행위를 가진 다수의 자식들을 생성하고 그들에게 메시지를 전달합니다.

중요한 특성: 자식이 중지되면 풀 라우터는 그것을 라우티 집합에서 제거합니다. 마지막 자식이 중지되면 라우터 자체도 중지됩니다.

풀 라우터는 본질적으로 로컬(local)입니다. 라우티는 클러스터 전반에 분산되지 않습니다.

#### 풀 라우터 생성

먼저 라우티 행위를 정의합니다. 다음은 간단한 워커(worker) 예제입니다.

**Scala 구현:**
```scala
object Worker {
  sealed trait Command
  case class DoLog(text: String) extends Command

  def apply(): Behavior[Command] = Behaviors.setup { context =>
    context.log.info("Starting worker")
    Behaviors.receiveMessage {
      case DoLog(text) =>
        context.log.info("Got message {}", text)
        Behaviors.same
    }
  }
}
```

**Java 구현:**
```java
class Worker {
  interface Command {}

  static class DoLog implements Command {
    public final String text;
    public DoLog(String text) {
      this.text = text;
    }
  }

  static final Behavior<Command> create() {
    return Behaviors.setup(
      context -> {
        context.getLog().info("Starting worker");
        return Behaviors.receive(Command.class)
          .onMessage(DoLog.class, doLog -> onDoLog(context, doLog))
          .build();
      });
  }

  private static Behavior<Command> onDoLog(ActorContext<Command> context, DoLog doLog) {
    context.getLog().info("Got message {}", doLog.text);
    return Behaviors.same();
  }
}
```

이제 풀 라우터를 생성하고 spawn합니다.

**Scala:**
```scala
Behaviors.setup[Unit] { ctx =>
  val pool = Routers.pool(poolSize = 4) {
    Behaviors.supervise(Worker()).onFailure[Exception](SupervisorStrategy.restart)
  }
  val router = ctx.spawn(pool, "worker-pool")

  (0 to 10).foreach { n =>
    router ! Worker.DoLog(s"msg $n")
  }

  Behaviors.empty
}
```

**Java:**
```java
Behaviors.setup(
  context -> {
    int poolSize = 4;
    PoolRouter<Worker.Command> pool =
      Routers.pool(
        poolSize,
        Behaviors.supervise(Worker.create()).onFailure(SupervisorStrategy.restart()));
    ActorRef<Worker.Command> router = context.spawn(pool, "worker-pool");

    for (int i = 0; i < 10; i++) {
      router.tell(new Worker.DoLog("msg " + i));
    }

    return Behaviors.empty();
  });
```

슈퍼비전 래퍼(supervision wrapper)에 주목하세요. 이것이 없으면 워커의 실패가 라우터를 함께 죽게(crash) 할 수 있습니다.

#### 디스패처 설정(dispatcher configuration)

라우티가 사용할 디스패처를 `withRouteeProps()`를 통해 설정합니다.

**Scala:**
```scala
val blockingPool = pool.withRouteeProps(routeeProps = DispatcherSelector.blocking())
val blockingRouter = ctx.spawn(blockingPool, "blocking-pool",
  DispatcherSelector.sameAsParent())
```

**Java:**
```java
PoolRouter<Worker.Command> blockingPool =
  pool.withRouteeProps(DispatcherSelector.blocking());
ActorRef<Worker.Command> blockingRouter =
  context.spawn(blockingPool, "blocking-pool", DispatcherSelector.sameAsParent());
```

라우터 액터 자체는 `spawn()` 호출에 지정된 디스패처를 사용합니다.

#### 모든 라우티에 브로드캐스트(broadcasting to all routees)

풀 라우터는 `withBroadcastPredicate()`를 통해 선택적 브로드캐스팅(selective broadcasting)을 지원합니다. 술어(predicate)에 일치하는 메시지는 모든 라우티에게 전달됩니다.

**Scala:**
```scala
val poolWithBroadcast = pool.withBroadcastPredicate(_.isInstanceOf[DoBroadcastLog])
val routerWithBroadcast = ctx.spawn(poolWithBroadcast, "pool-with-broadcast")
routerWithBroadcast ! DoBroadcastLog("msg")
```

**Java:**
```java
PoolRouter<Worker.Command> broadcastingPool =
  pool.withBroadcastPredicate(msg -> msg instanceof DoBroadcastLog);
```

### 6.3 그룹 라우터(group router)

#### 개념과 클러스터 인식

그룹 라우터는 `ServiceKey`를 사용해 리셉셔니스트를 통해 라우티를 발견하며, 따라서 자동으로 클러스터를 인식(cluster-aware)합니다. 그룹 라우터는 `ServiceKey`로 생성되고, 리셉셔니스트를 사용하여 그 키에 대해 사용 가능한 액터들을 발견한 뒤, 그 키에 대해 현재 알려진 등록된 액터 중 하나로 메시지를 라우팅합니다.

리셉셔니스트를 사용한다는 것은 결과적 일관성(eventual consistency)을 함의합니다. 라우터는 클러스터 내에서 도달 가능한 임의의 노드에 등록된 액터들에게 메시지를 보냅니다.

#### 스태싱(stashing) 동작

중요한 특성: 그룹 라우터는 리셉셔니스트로부터 등록된 서비스의 첫 번째 목록(listing)을 볼 때까지 메시지를 스태시(stash)합니다. 이는 그룹 라우터를 생성한 직후에 메시지를 보내는 것이 안전함을 의미합니다. 메시지는 리셉셔니스트가 응답할 때까지 큐에 대기합니다.

라우터가 등록된 집합이 비어 있음을 알게 되면, 라우터는 메시지를 버립니다(이를 이벤트 스트림(event stream)에 `akka.actor.Dropped`로 발행(publish)합니다).

#### 구현

**Scala:**
```scala
val serviceKey = ServiceKey[Worker.Command]("log-worker")

Behaviors.setup[Unit] { ctx =>
  val worker = ctx.spawn(Worker(), "worker")
  ctx.system.receptionist ! Receptionist.Register(serviceKey, worker)

  val group = Routers.group(serviceKey)
  val router = ctx.spawn(group, "worker-group")

  (0 to 10).foreach { n =>
    router ! Worker.DoLog(s"msg $n")
  }

  Behaviors.empty
}
```

**Java:**
```java
ServiceKey<Worker.Command> serviceKey =
  ServiceKey.create(Worker.Command.class, "log-worker");

Behaviors.setup(
  context -> {
    ActorRef<Worker.Command> worker = context.spawn(Worker.create(), "worker");
    context.getSystem().receptionist().tell(Receptionist.register(serviceKey, worker));

    GroupRouter<Worker.Command> group = Routers.group(serviceKey);
    ActorRef<Worker.Command> router = context.spawn(group, "worker-group");

    for (int i = 0; i < 10; i++) {
      router.tell(new Worker.DoLog("msg " + i));
    }

    return Behaviors.empty();
  });
```

워커들은 `Receptionist.Register()`를 통해 스스로를 등록하고, 그룹 라우터는 자동으로 그들을 발견합니다.

### 6.4 라우팅 전략(routing strategies)

Akka Typed는 spawn 전에 설정할 수 있는 세 가지 라우팅 전략(routing strategy)을 제공합니다.

#### 라운드 로빈(round robin)

라운드 로빈은 라우티들을 순차적으로(sequentially) 순환합니다. `n`개의 라우티와 `n`개의 메시지가 있으면, 각 액터는 정확히 하나의 메시지를 처리합니다. 이 전략은 풀 라우터의 기본값(default)입니다.

라운드 로빈은 라우티 집합이 비교적 안정적으로 유지되는 한, 사용 가능한 모든 라우티가 동일한 양의 메시지를 받는 공정한 라우팅(fair routing)을 제공합니다.

설정:

**Scala:**
```scala
val alternativePool = pool.withPoolSize(2).withRoundRobinRouting()
```

**Java:**
```java
PoolRouter<Worker.Command> alternativePool =
  pool.withPoolSize(2).withRoundRobinRouting();
```

선택적인 `preferLocalRoutees` 매개변수가 존재합니다. 이 값이 true이면, 라우터는 가능한 경우 로컬 라우티를 우선합니다.

#### 랜덤(random)

랜덤 선택은 각 메시지마다 임의로 라우티를 고릅니다. 이는 그룹 라우터의 기본값입니다. 클러스터에서 라우티 멤버십이 동적으로 변하기 때문입니다.

라운드 로빈과 마찬가지로, 이 전략도 `preferLocalRoutees` 설정을 받습니다.

#### 일관된 해싱(consistent hashing)

이 전략은 메시지 내용에 기반하여 라우티에 메시지를 할당하기 위해 일관된 해싱(consistent hashing)을 사용합니다. 즉, 전송된 메시지에 기반하여 라우티를 선택하기 위해 일관된 해싱을 사용합니다.

필수적인 해시 매핑 함수(hash mapping function)가 라우팅 결정을 투명하게(transparent) 만듭니다. 일관된 해싱은 라우티 집합이 동일하게 유지되는 한, 같은 해시를 가진 메시지를 같은 라우티에게 전달합니다.

라우티 집합이 변경될 때, 일관된 해싱은 같은 해시를 가진 메시지가 같은 라우티로 라우팅되도록 보장하려 노력하지만, 이를 보장하지는 않습니다.

일관된 해싱을 보여 주는 프록시(proxy) 예제는 다음과 같습니다.

**Scala:**
```scala
val router = spawn(Routers.group(Proxy.RegisteringKey)
  .withConsistentHashingRouting(10, Proxy.mapping))

router ! Proxy.Message("123", "Text1")
router ! Proxy.Message("123", "Text2")
router ! Proxy.Message("zh3", "Text3")
router ! Proxy.Message("zh3", "Text4")
```

**Java:**
```java
ActorRef<Proxy.Message> router =
  testKit.spawn(
    Routers.group(proxy.registeringKey)
      .withConsistentHashingRouting(10, command -> proxy.mapping(command)));

router.tell(new Proxy.Message("123", "Text1"));
router.tell(new Proxy.Message("123", "Text2"));
router.tell(new Proxy.Message("zh3", "Text3"));
router.tell(new Proxy.Message("zh3", "Text4"));
```

같은 해시 키("123" 또는 "zh3")를 가진 메시지는 같은 액터로 라우팅됩니다. `withConsistentHashingRouting()` 메서드는 가상 노드(virtual nodes) 수(10)와 매핑 함수를 인자로 받습니다.

안정적인 재균형(rebalancing) 시나리오에는 Akka 클러스터 샤딩(Akka Cluster Sharding)을 대안으로 권장합니다.

### 6.5 성능 고려 사항(performance considerations)

라우터 자체는 액터이며 메일박스를 가집니다. 이는 메시지가 라우티들로 순차적으로(sequentially) 라우팅된다는 것을 의미하며, 라우티에서는 병렬로(in parallel) 처리될 수 있습니다.

단일 액터를 통한 이 순차적 라우팅은 고처리량(high-throughput) 시스템에서 병목(bottleneck)이 될 수 있습니다. 또한 라우티들이 자원을 공유하는 경우, 그 자원이 액터 수를 늘리는 것이 실제로 더 높은 처리량이나 더 빠른 응답을 줄지를 결정하게 됩니다.

CPU 바운드(CPU-bound) 라우티는 사용 가능한 스레드 수를 초과해도 이점을 얻지 못합니다. Akka Typed는 직접적인 병렬 메시지 분배를 요구하는 극단적 처리량(extreme-throughput) 시나리오에 대한 특수 최적화를 제공하지 않습니다.

---

## 7. 스태시(stash)

### 7.1 스태싱(stashing) 개요

스태싱(stashing)은 현재 행위에서 처리할 수 없거나 처리해서는 안 되는 들어오는 메시지를 액터가 일시적으로 보류(hold)할 수 있게 하는 메커니즘을 제공합니다. 메시지를 즉시 처리하는 대신, 조건이 바뀌거나 자원이 사용 가능해질 때 나중에 처리하도록 큐에 넣을 수 있습니다.

프레임워크는 이 기능을 `StashBuffer` API를 통해 제공하며, 액터는 이를 사용해 메시지 처리를 전략적으로 지연(defer)시킬 수 있습니다. 이는 특히 두 가지 흔한 시나리오에서 가치가 있습니다. 하나는 액터가 일반 메시지를 받아들이기 전에 초기 상태(initial state)를 로드해야 할 때이고, 다른 하나는 데이터베이스 쓰기 같은 특정 연산을 직렬화(serialize)해야 할 때입니다.

### 7.2 DataAccess 액터 예제

이 문서는 단일 값을 관리하는 데이터베이스 접근 액터(database access actor)를 통해 실용적인 예시를 제시합니다. 이 액터가 시작될 때, 클라이언트 요청에 응답하기 전에 데이터베이스로부터 현재 상태를 가져와야 합니다. 이 로딩 기간 동안 들어오는 모든 메시지는 스태시됩니다. 마찬가지로, 업데이트를 영속화(persist)할 때 액터는 동시 쓰기(concurrent write)가 아닌 순차 처리(sequential processing)를 보장하기 위해 새 요청을 스태시합니다.

Scala 구현은 액터 행위를 `Behaviors.withStash(100)`로 감쌈으로써 이 패턴을 보여 줍니다. 이는 100개의 메시지를 담을 수 있는 용량(capacity)의 버퍼를 할당합니다. 액터는 로딩 단계(loading phase)에서 시작하여, 초기 상태가 데이터베이스로부터 도착했음을 확인하는 응답을 제외한 모든 메시지를 스태시합니다.

### 7.3 StashBuffer 생성과 설정

스태싱을 활성화하려면 액터 행위를 `Behaviors.withStash(capacity)`로 감싸고, 버퍼가 보유할 수 있는 메시지의 수를 지정합니다. `capacity` 매개변수는 필수입니다. 버퍼가 보유할 수 있는 메시지의 수는 생성 시점에 반드시 지정되어야 하기 때문입니다.

Java 버전의 설정은 다음과 같습니다.

```java
Behaviors.withStash(
    100,
    stash ->
        Behaviors.setup(
            ctx -> {
              ctx.pipeToSelf(
                  db.load(id),
                  (value, cause) -> {
                    if (cause == null) return new InitialState(value);
                    else return new DBError(asRuntimeException(cause));
                  });
              return new DataAccess(ctx, stash, id, db).start();
            }));
```

이는 `StashBuffer<Command>`를 생성하여 액터 생성자에 전달하므로, 액터의 생애 전반에 걸쳐 사용할 수 있게 됩니다.

### 7.4 메시지 스태싱(stashing messages)

액터가 즉시 처리할 수 없는 메시지를 받으면, `buffer.stash(message)`를 호출하여 그것을 지연시킵니다. DataAccess 예제에서는 데이터베이스가 초기 상태를 반환하기를 기다리는 동안, 액터가 들어오는 모든 `Get`과 `Save` 명령을 스태시합니다.

```scala
Behaviors.receiveMessage {
  case InitialState(value) =>
    buffer.unstashAll(active(value))
  case DBError(cause) =>
    throw cause
  case other =>
    buffer.stash(other)
    Behaviors.same
}
```

Java에서도 메시지 핸들러 내에서 유사한 패턴을 사용합니다.

### 7.5 스태시 용량 관리(managing stash capacity)

스태시된 메시지는 언스태시(unstash)될 때까지(또는 액터가 중지되어 가비지 컬렉션(garbage collection)될 때까지) 메모리에 유지되므로, 많은 수의 스태시된 메시지는 메모리 위험을 초래합니다. 이것이 버퍼가 경계(bounded)를 갖는 이유입니다. 용량을 초과하여 스태시를 시도하면 `StashOverflowException`이 던져집니다.

스태시하기 전에 `StashBuffer.isFull`을 확인하여 오버플로를 피할 수 있습니다. 버퍼가 용량에 도달하면, 메시지를 버리거나, 송신자에게 거부 응답(rejection response)을 보내거나, 사용 사례에 적합한 다른 방어적 조치를 취할 수 있습니다.

### 7.6 언스태싱과 처리 재개(unstashing and resuming processing)

액터가 큐에 대기 중인 메시지를 처리할 준비가 되면, `buffer.unstashAll(behavior)`을 호출하여 새 행위로 전이하면서 스태시된 모든 메시지를 순서대로(in order) 처리합니다.

```scala
private def start(): Behavior[Command] = {
  // ... 데이터베이스 로드 시작 ...
  Behaviors.receiveMessage {
    case InitialState(value) =>
      buffer.unstashAll(active(value))
    // ...
  }
}
```

새 행위는 새로 도착하는 메시지를 처리하기 전에 스태시된 모든 메시지를 순차적으로 받습니다. 이는 메시지 순서(message ordering)를 유지하고, 오래된 요청과 새 요청이 뒤섞이는 것(interleaving)을 방지합니다.

### 7.7 스태시된 메시지의 순차 처리(processing stashed messages sequentially)

언스태싱이 일어나면, 메시지는 추가된 순서대로 순차적으로 처리되며, 예외가 던져지지 않는 한 모두 처리됩니다. 액터는 스태시된 모든 메시지가 처리될 때까지 새 메시지에 응답하지 않게 됩니다. 이 직렬화는 일관성(consistency)을 보장하지만, 스태시에 많은 메시지가 있을 경우 액터의 스레드를 막아 다른 액터를 굶주리게(starve) 할 수 있습니다.

이 문서는 이 우려를 다음과 같이 언급합니다. 메시지 처리 스레드를 너무 오래 독점(hog)하는 액터는 다른 액터의 기아(starvation)를 초래할 수 있습니다. 스태시된 메시지 수를 낮게 유지하면 이 위험이 완화됩니다.

### 7.8 부분 처리를 통한 고급 언스태싱(advanced unstashing with partial processing)

매우 큰 스태시가 있는 시나리오를 위해, 프레임워크는 `numberOfMessages` 매개변수를 가진 `StashBuffer.unstash`를 제공하여 메시지를 배치(batch)로 처리할 수 있게 합니다. 제한된 수의 메시지를 처리한 후, 언스태싱을 계속하기 전에 `context.self`로 메시지를 보낼 수 있습니다. 그러나 이 접근 방식은 처리를 복잡하게 만듭니다. 배칭 과정 중에 도착하는 새 메시지도 순서를 보존하기 위해 스태시되어야 하기 때문입니다. 권장 사항은 여전히, 스태시된 메시지 수를 낮게 유지하는 것이 낫다는 것입니다.

### 7.9 DataAccess의 실제 워크플로(practical workflow in DataAccess)

DataAccess 액터는 완전한 워크플로를 보여 줍니다.

1. **초기화 단계(Initialization Phase)**: 생성 시 액터는 데이터베이스 로드를 시작하고 들어오는 모든 요청을 스태시합니다.

2. **활성 단계(Active Phase)**: 초기 상태가 도착하면, 대기 중인 모든 메시지를 언스태시하고 `active` 행위로 진입하여, 정상적으로 `Get`과 `Save` 명령을 처리합니다.

3. **저장 단계(Saving Phase)**: `Save` 명령이 도착하면, 액터는 `saving` 행위로 전이하여 데이터베이스 쓰기를 시작하면서 새 요청을 스태시합니다.

4. **완료(Completion)**: 저장이 완료되면, 액터는 대기 중인 메시지를 언스태시하고 `active` 행위로 돌아갑니다.

이 패턴은 데이터베이스 연산이 순차적으로 유지되도록 하고, 초기화나 상태 갱신 중의 타이밍 문제로 인해 어떤 클라이언트 요청도 분실되지 않도록 보장합니다.

---

## 8. 유한 상태 기계(finite state machines, FSM)

### 8.1 개요

Akka의 타입드 액터 프레임워크는 행위(behavior)를 통해 유한 상태 기계(Finite State Machine, FSM)를 모델링할 수 있게 합니다. 클래식 FSM 접근 방식과 달리, 타입드 시스템은 각 상태(state)를 별개의 행위로 표현하여 타입 안전성과 더 명확한 코드 구성을 제공합니다.

### 8.2 핵심 개념

#### 메시지로서의 이벤트(events as messages)

타입드 Akka에서 FSM의 기반은 액터가 받을 수 있는 메시지 타입이 되는 이벤트(event)를 정의하는 것입니다. 예를 들어, Buncher 액터는 다음과 같은 여러 이벤트 타입으로 이 패턴을 보여 줍니다.

- `SetTarget`: 액터를 목적지 참조(destination reference)로 초기화합니다.
- `Queue`: 버스트(burst) 기간 동안 내부 큐(internal queue)에 객체를 추가합니다.
- `Flush`: 버스트의 끝을 알립니다.
- `Timeout`: 상태 타임아웃(state timeout)에 의해 트리거됩니다.

각 이벤트는 공통의 sealed trait나 인터페이스를 구현하여, 모든 상태 핸들러(state handler)에 걸쳐 타입 안전성을 보장합니다.

#### 상태 데이터 표현(state data representation)

상태별 데이터(state-specific data)는 전용 데이터 타입을 통해 유지됩니다. Buncher 예제는 다음을 사용합니다.

- `Uninitialized`: 설정 전의 시작 상태(startup state)를 나타냅니다.
- `Todo`: 동작 중에 대상 액터 참조와 큐에 쌓인 메시지들을 보관합니다.

이 분리(separation)는 각 행위 메서드가 자신의 상태에 관련된 데이터만 접근하도록 합니다.

#### 행위로서의 상태(states as behaviors)

각 FSM 상태는 `Behavior` 객체를 반환하는 메서드가 됩니다. 핵심 패턴은 다음을 포함합니다.

1. 정의된 이벤트 타입의 메시지를 받습니다.
2. 이벤트와 현재 상태에 대해 패턴 매칭(pattern matching)을 수행합니다.
3. 다음 행위(새 상태)를 반환합니다.

**Idle 상태**는 비활성 상태일 때 초기 설정과 큐잉(queuing)을 처리합니다. `Uninitialized` 상태에서 `SetTarget` 메시지가 도착하면, 초기화된 데이터(initialized data)를 가진 idle 상태로 전이합니다. idle 상태에서 받은 `Queue` 메시지는 **Active 상태**로의 전이를 트리거합니다.

**Active 상태**는 타임아웃 관리를 동반한 버스트 처리(burst processing)를 다룹니다. 메시지는 계속 큐에 쌓이며, `Flush` 또는 타임아웃 이벤트는 배치(batch)된 메시지를 대상 액터에게 보내고 idle 상태로 돌아가도록 트리거합니다.

### 8.3 타이머 구현(implementing timers)

상태 타임아웃은 `TimerScheduler`를 제공하는 `Behaviors.withTimers` 래퍼를 사용합니다. active 상태 내에서는 다음과 같이 사용합니다.

```scala
timers.startSingleTimer(Timeout, 1.second)
```

이는 타임아웃 메시지를 예약하며, 이 메시지는 명시적인 플러시(flush) 요청과 마찬가지로 액터가 누적된 메시지를 디스패치(dispatch)하고 idle 상태로 되돌아가게 합니다.

### 8.4 처리되지 않은 메시지 다루기(handling unhandled messages)

예기치 못한 메시지를 다루기 위한 세 가지 행위가 있습니다.

- `Behaviors.unhandled`: 처리되지 않은 메시지를 로깅하면서 현재 행위를 유지합니다.
- `Behaviors.empty`: 액터가 더 이상 메시지를 기대하지 않음을 나타냅니다.
- `Behaviors.ignore`: 로깅 없이 처리되지 않은 모든 메시지를 조용히 버립니다.

### 8.5 상태 간 전이(transitioning between states)

상태 전이는 메시지 핸들러에서 새 행위를 반환함으로써 일어납니다. 이 명시적 패턴은 전통적인 FSM 상태 선언을 대체하며, FSM 생애 전반에 걸쳐 더 명확한 제어 흐름(control flow)과 타입 안전성을 제공합니다.

---

## 참고 자료

- [Akka 공식 문서](https://doc.akka.io/libraries/akka-core/current/)
- [Actors 소개](https://doc.akka.io/libraries/akka-core/current/typed/actors.html)
- [Actor lifecycle](https://doc.akka.io/libraries/akka-core/current/typed/actor-lifecycle.html)
- [Interaction Patterns](https://doc.akka.io/libraries/akka-core/current/typed/interaction-patterns.html)
- [Fault Tolerance](https://doc.akka.io/libraries/akka-core/current/typed/fault-tolerance.html)
- [Actor discovery](https://doc.akka.io/libraries/akka-core/current/typed/actor-discovery.html)
- [Routers](https://doc.akka.io/libraries/akka-core/current/typed/routers.html)
- [Stash](https://doc.akka.io/libraries/akka-core/current/typed/stash.html)
- [Finite State Machines](https://doc.akka.io/libraries/akka-core/current/typed/fsm.html)
