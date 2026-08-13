# Akka Typed Actors

## Akka Typed Actors 기초

> 원본: https://doc.akka.io/libraries/akka-core/current/typed/actors.html

---

### 목차

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

### 1. 액터(actor) 소개

#### 1.1 액터 모델(Actor Model)이란

액터 모델(Actor Model)은 동시성(concurrent)·분산(distributed) 시스템을 작성하기 위한 고수준 추상화(abstraction)를 제공함. 개발자가 명시적인 잠금(locking)과 스레드 관리(thread management)를 직접 다루어야 하는 부담에서 벗어나게 해 주며, 그 결과 올바른(correct) 동시·병렬(concurrent and parallel) 시스템을 더 쉽게 작성 가능.

액터 모델은 1973년 칼 휴잇(Carl Hewitt)이 처음 정의했으며, 얼랭(Erlang) 언어가 이를 사용해 고도로 동시적인(highly concurrent) 통신(telecom) 시스템을 성공적으로 구축하면서 널리 알려짐. Akka의 API는 얼랭의 접근 방식에서 일부 구문 요소(syntactic elements)를 차용함.

#### 1.2 모듈 정보와 의존성(dependencies)

Akka Actors를 사용하려면 프로젝트에 특정 의존성(dependency) 추가 필요. 현재 문서가 다루는 버전은 2.10.19. 의존성은 Akka의 보안 라이브러리 저장소(secure library repository)에서 제공되며, 사용하려면 https://account.akka.io/token 에 명시된 대로 보안 토큰이 포함된(secure, tokenized) URL 필요.

sbt 프로젝트에서 필요한 라이브러리는 `akka-actor-typed`와 `akka-actor-testkit-typed`. Maven과 Gradle 설정도 제공되며, 의존성 관리를 위해 BOM(Bill of Materials) 방식 사용.

아티팩트(artifact) 상세 정보는 다음과 같음.

- JDK 버전: 11, 17, 21
- Scala 버전: 2.13.17, 3.3.7
- 라이선스(License): BUSL-1.1
- 상태(Status): Lightbend 지원으로 정식 지원됨(Supported)

#### 1.3 행위(behavior)와 ActorRef

액터(actor)의 행위(behavior)는 메시지를 어떻게 처리할지를 결정함. 행위는 `receive` 행위 팩토리(behavior factory)로 정의됨(예: `Greeter`). 메시지를 처리한 결과로 새로운 행위(new behavior)가 반환되며, 이 새 행위는 이전 행위와 다를 수 있음. 즉, 액터는 메시지를 처리하면서 행위를 바꿈으로써 상태(state)를 표현함.

`ActorRef`는 어떤 액터 인스턴스에 대한 참조(reference)를 나타냄. `ActorRef[T]`는 타입 매개변수 `T`를 가지며, 컴파일러(compiler)는 오직 이 타입의 메시지만 전송하도록 허용함. 다른 사용은 컴파일 오류(compiler error)가 되며, 이를 통해 액터 통신에 대한 타입 안전성(type safety) 보장.

#### 1.4 첫 번째 예제: Hello World

간단한 인사(greeting) 예제는 기본적인 액터 패턴을 보여 줌. 두 가지 메시지 타입이 정의됨.

- `Greet`: 액터에게 누군가에게 인사하라고 명령하는 메시지
- `Greeted`: 인사가 이루어졌음을 확인(confirm)하는 메시지

`Greet` 메시지는 `ActorRef` 필드를 포함하므로, 수신 액터(receiving actor)가 확인 메시지를 다시 돌려보낼 수 있음.

`HelloWorld` 액터 구현은 `receive` 행위 팩토리를 사용하여 들어오는 `Greet` 메시지를 처리함. 메시지를 받으면 정보를 로깅(logging)한 뒤, tell 연산자(`!`)를 사용하여 제공된 참조로 응답(reply)을 보냄.

두 번째 액터인 `HelloWorldBot`은 응답을 받아 자신의 행위를 변경(change behavior)함으로써 카운터(counter)를 관리함. 이는 가변 변수(mutable variable)가 아니라 행위 전이(behavioral transition)를 통해 상태를 관리하는 방식을 보여 줌.

`HelloWorldMain` 액터는 인사하는 액터(greeter)와 봇(bot) 액터를 모두 생성(spawn)하고, `SayHello` 메시지를 보내어 두 액터의 상호작용을 시작함.

`ActorSystem`은 이 액터들을 호스팅(host)하고 메시지를 받음.

```scala
val system: ActorSystem[HelloWorldMain.SayHello] =
  ActorSystem(HelloWorldMain(), "hello")

system ! HelloWorldMain.SayHello("World")
system ! HelloWorldMain.SayHello("Akka")
```

이 예제는 비동기 메시징(asynchronous messaging)을 보여 줌. tell 연산은 호출자의 스레드(caller's thread)를 막지 않는(non-blocking) 비동기 연산(asynchronous operation).

#### 1.5 복잡한 예제: 채팅방(chat room) 프로토콜

좀 더 정교한 예제로 채팅방(chat room) 프로토콜은 다음과 같은 여러 중요한 패턴을 보여 줌.

- 여러 메시지 타입을 표현하기 위해 sealed trait와 case class(또는 Java에서는 인터페이스와 그 구현 클래스)를 사용하는 방법
- 자식 액터(child actor)를 통해 세션(session)을 관리하는 방법
- 행위를 변경함으로써 상태를 다루는 방법
- 프로토콜의 서로 다른 단계를 안전하게 표현하기 위해 여러 액터를 사용하는 방법

채팅방은 서로 다른 메시지 타입으로 구성된 프로토콜(protocol)을 정의함. 초기에 클라이언트는 `ActorRef[GetSession]`에 대한 접근 권한을 받아 세션을 요청 가능. 세션이 수립되면 클라이언트는 메시지를 게시(post)할 수 있는 핸들(handle)을 담은 `SessionGranted` 메시지를 받음. 이후의 `MessagePosted` 이벤트는 클라이언트가 공개한(revealed) 주소로 전송됨.

이 구현은 각 클라이언트 세션마다 자식 액터(child actor)를 생성하여, 프로토콜을 액터 계층 구조(actor hierarchy) 안에 캡슐화(encapsulate)함. `PublishSessionMessage` 타입은 비공개(private) 가시성을 유지하여 권한 없는 프로토콜 위반(protocol violation) 방지. 즉, 클라이언트가 다른 화면 이름(screen name)으로 위장 불가.

이는 액터가 단순히 Java 객체의 메서드 호출(method call)에 상응하는 것 이상을 표현할 수 있음을 보여 줌. 선언된 메시지 타입과 그 내용은 여러 액터에 걸쳐 진행되고 여러 단계에 걸쳐 발전할 수 있는 완전한 프로토콜(full protocol)을 기술함.

#### 1.6 함수형 스타일(functional style)과 객체지향 스타일(object-oriented style)

두 가지 구현 방식이 제시됨.

**함수형 스타일(Functional Style)** 은 `Behaviors.receive`를 사용하며, 갱신된 상태를 가진 새로운 행위 인스턴스(new behavior instance)를 반환함으로써 상태를 관리함. 상태 변경은 가변 필드(mutable field)가 아니라 행위 전이(behavior transition)를 통해 표현됨.

**객체지향 스타일(Object-Oriented Style)** 은 `AbstractBehavior`를 확장(extend)하고 인스턴스 필드(instance field)에 상태를 유지함. `onMessage` 메서드는 메시지를 처리하고, 새로운 행위 인스턴스를 생성하는 대신 `this` 또는 `Behaviors.same()`을 반환함.

두 스타일은 각 액터의 구체적인 필요에 따라 혼합(mix)하여 사용 가능. 두 접근 방식 중 어느 것을 선택할지에 대한 자세한 고려 사항은 스타일 가이드(Style Guide) 참고 권장.

#### 1.7 AbstractBehavior API

객체지향 방식에서는 기본 행위 클래스(base behavior class)를 확장하고 메시지 핸들러(message handler) 구현 필요. 이 패턴은 전통적인 클래스 기반 설계(class-based design)와 유사하지만, 단일 스레드 메시지 처리(single-threaded message processing)에 대한 액터 모델의 보장은 그대로 유지됨.

대안으로 `AbstractOnMessageBehavior` API가 있으며, 이는 `AbstractBehavior`가 사용하는 빌더 패턴(builder pattern)을 피하고 Java 17 이상의 패턴 매칭(pattern matching) 기능을 코드에서 직접 지원함.

#### 1.8 액터 시스템(actor system)과 실행(execution)

애플리케이션은 일반적으로 JVM당 하나의 `ActorSystem`으로 구성되며, 그 안에서 많은 액터가 실행됨. `ActorSystem`은 액터가 실행되는 런타임 환경(runtime environment)을 제공함.

프레임워크의 핵심 개념은 다음과 같음.

- **ActorContext**: 자식 액터 생성(spawning child actors), 로깅(logging), 자기 자신에 대한 참조(self-reference) 등 제공
- **시그널(Signals)**: 사용자 메시지(user message)와 구별되는 시스템 이벤트(system event)이며, 별도로 처리됨
- **감시(Watching)**: `context.watch()`를 통해 액터의 종료(termination)를 감시(monitor)하고 `Terminated` 시그널을 받음
- **셋업(Setup)**: `Behaviors.setup` 팩토리는 액터 시작 시점까지 행위 생성을 지연(defer)시키고, `ActorContext`를 매개변수로 제공

#### 1.9 타입 안전성(type safety)과 프로토콜 정의

액터 프레임워크는 제네릭 타입(generic type)을 통해 컴파일 타임 타입 안전성(compile-time type safety)을 강조함. 메시지 프로토콜은 명시적으로 타입이 지정된 메시지 클래스(message class)와 `ActorRef` 선언을 통해 정의됨. `ActorRef[-T]`의 반공변성(contravariance)에 의해 `ActorRef[PublishSessionMessage]`가 기대되는 곳에서 `ActorRef[RoomCommand]` 사용 가능. 더 넓은 타입(broader type)이 더 좁은 프로토콜(narrower protocol)을 수용하기 때문.

#### 1.10 콘솔 출력 예시

Hello World 예제를 실행하면, 서로 다른 디스패처 스레드(dispatcher thread)들이 인사와 응답을 처리하는 로그 메시지가 출력됨. 이는 액터 시스템 전반에 걸친 동시 실행(concurrent execution)을 보여 줌.

---

### 2. 액터 생명주기(actor lifecycle)

#### 2.1 개요

액터(actor)는 명시적으로 시작(start)되고 중지(stop)되어야 하는 상태를 가진 자원(stateful resource). 액터는 더 이상 참조되지 않더라도 자동으로 종료되지 않음. 각 액터는 명시적으로 소멸(destroy)되어야 함. 부모 액터(parent actor)가 중지되면 그 모든 자식 액터(child actor)도 재귀적으로(recursively) 함께 중지됨. 또한 `ActorSystem`이 종료(shut down)되면 모든 액터도 자동으로 중지됨.

`ActorSystem`은 스레드를 할당하는 무거운(heavyweight) 구조이므로, 논리적 애플리케이션(logical application)당 하나만 생성해야 함. 일반적으로 JVM 프로세스당 하나의 `ActorSystem`을 둠.

#### 2.2 ActorContext와 그 기능

`ActorContext`는 다음과 같은 여러 기능에 대한 접근을 제공함.

- 자식 액터 생성(spawning child actors)과 슈퍼비전(supervision)
- 다른 액터를 감시(watch)하여 종료 이벤트(termination event) 수신
- 로깅(logging) 기능
- 메시지 어댑터(message adapter) 생성
- ask 패턴(ask pattern)을 통한 요청-응답(request-response) 상호작용
- 액터의 자기 참조(self reference)에 대한 접근

##### 스레드 안전성(thread safety) 관련 주의 사항

`ActorContext`의 많은 메서드는 스레드 안전(thread safe)하지 않음. 이들은 Future/CompletionStage 콜백(callback) 내의 스레드에서 접근해서는 안 되며, 여러 액터 인스턴스 사이에서 공유(share)되어서도 안 됨. 이러한 연산은 오직 일반적인 액터 메시지 처리 스레드(ordinary actor message processing thread)에서만 사용해야 함.

#### 2.3 액터 생성과 생성(spawning)

`ActorContext`에 접근하려면 행위를 `Behaviors.setup`으로 감싸야(wrap) 함. 이를 통해 자식 액터를 생성하는 데 필요한 컨텍스트(context) 획득 가능.

##### 가디언 액터(guardian actor)

가디언(guardian, 또는 루트(root)) 액터는 `ActorSystem`과 함께 생성되며, 계층 구조(hierarchy)에서 최상위(top-level) 액터로서 동작함. 간단한 애플리케이션에서는 가디언이 핵심 로직(core logic)을 담을 수도 있음. 그러나 더 큰 시스템에서는 가디언을 주로 작업의 초기화(initialization of tasks)와 애플리케이션의 초기 액터(initial actors) 생성에 사용해야 함.

가디언 액터가 중지되면 `ActorSystem`이 중지됨. 시스템 종료(system termination)는 조정된 종료(Coordinated Shutdown) 프로세스를 트리거하며, 이는 액터와 서비스를 특정 순서(specific order)대로 중지함.

##### 자식 액터 생성(spawning child actors)

자식 액터는 `ActorContext`의 `spawn` 메서드를 사용해 생성됨. 이 관계는 계층적(hierarchical). 즉, 자식 액터의 생명주기는 부모와 묶여 있음. 자식은 스스로를 중지하거나 언제든지 중지될 수 있지만, 결코 부모보다 오래 살아남을(outlive) 수는 없음.

생성 시 디스패처(dispatcher)를 지정하려면 `DispatcherSelector` 사용. 지정하지 않으면 액터는 기본 디스패처(default dispatcher)를 사용함.

#### 2.4 SpawnProtocol 패턴

(예: HTTP 요청마다) 외부 소스로부터 동적으로 액터를 생성해야 하는 애플리케이션의 경우, `SpawnProtocol`이 미리 정의된 메시지 프로토콜과 행위 구현을 제공함. 이는 가디언 액터의 행위로 사용 가능하며, 초기화 작업을 위해 `Behaviors.setup`과 결합도 가능.

이렇게 하면 시스템의 액터 참조에 `SpawnProtocol.Spawn`을 tell하거나 ask함으로써 외부에서 자식 액터 시작 가능. ask를 사용하면 즉시 생성하는 대신 `ActorRef`의 Future/CompletionStage를 반환받게 됨.

#### 2.5 액터 중지(stopping actors)

액터는 다음 행위로 `Behaviors.stopped`를 반환함으로써 스스로를 중지함. 부모는 `ActorContext`의 `stop` 메서드를 사용하여, 자식이 현재 메시지 처리를 마친 뒤에 강제로 중지 가능.

액터가 중지되면 `PostStop` 시그널을 받으며, 이는 자원 정리(cleaning up resources)에 사용 가능. 자원 정리를 위해 `PostStop`과 `PreRestart` 시그널을 모두 처리하는 것 권장. 재시작(restart) 시에는 `PostStop`이 방출되지 않으므로 주의 필요.

#### 2.6 액터 감시(watching actors)

액터는 `watch` 메서드를 사용해 다른 액터의 종료를 감시 가능. 감시 대상 액터가 종료되면 감시하는 액터는 `Terminated` 시그널을 받음. 감시 대상은 임의의 `ActorRef`가 될 수 있으며, 반드시 자식 액터일 필요는 없음.

대안으로 `watchWith`가 있는데, 이는 `Terminated` 시그널 대신 사용자 정의 메시지(custom message)를 지정할 수 있게 해 줌. 이 방식은 추가 정보(additional information)를 메시지에 포함할 수 있기 때문에, `watch`와 `Terminated` 시그널을 사용하는 것보다 종종 더 선호됨.

주요 동작은 다음과 같음.

- 종료 메시지(terminated message)는 등록(registration)과 종료(termination)의 순서에 관계없이 생성됨.
- 여러 번 등록하면 여러 개의 메시지가 생성될 수 있음. 즉, 감시 대상 액터의 종료로 메시지가 생성되어 큐(queue)에 들어간 뒤, 그 메시지가 처리되기 전에 또 다른 등록이 이루어진 경우.
- `context.unwatch(target)`을 통해 등록 해제(deregistration) 가능.
- 감시 대상 액터가 클러스터(Cluster)에서 제거된 노드(node)에 있을 때에도 종료 메시지가 전송됨.

---

### 3. 상호작용 패턴(interaction patterns)

#### 3.1 보내고 잊기(fire and forget, tell)

**설명**: tell 연산자(`!`)를 사용하는 가장 기본적인 상호작용 방법으로, 응답을 기대하지 않고 메시지를 비동기적으로(asynchronously) 보냄. 이 메서드는 즉시 반환되며, 메시지가 실제로 처리되었다는 보장은 제공하지 않음.

**프로토콜 예시**: 문자열을 담은 단순한 `PrintMe` 메시지로, 액터는 메시지를 로깅한 뒤 이전 행위로 돌아감.

**유용한 경우(useful when)**:
- 메시지 수신 확인(receipt confirmation)이 불필요할 때
- 전달 실패(delivery failure)에 대해 조치할 방법이 없을 때
- 메시지 수를 최소화하여 처리량(throughput)을 극대화하는 것이 중요할 때

**문제점(problems)**:
- 유입량이 처리 능력을 초과하면 메일박스 오버플로(mailbox overflow) 발생 가능
- 분실된 메시지를 송신자가 알아채지 못함
- 성공적인 처리에 대한 확인(acknowledgment)이 없음

---

#### 3.2 요청-응답(request-response, 직접 프로토콜)

**설명**: 수신자의 참조가 메시지 안에 인코딩(encode)되어 있어, 직접 응답 가능. 송신자는 `ActorRef[Response]` 필드를 포함시키고, 수신자는 이를 사용하여 tell로 응답을 돌려보냄.

**프로토콜 예시**: 질의(query)와 응답용 `ActorRef[Response]`를 담은 `Request` 메시지. 응답자(responder)는 질의를 처리하고 제공된 참조로 직접 `Response` 메시지를 보냄.

**유용한 경우**:
- 한 액터가 여러 개의 응답 메시지를 돌려보낼 때
- 다른 액터로부터 지속적인 업데이트(ongoing updates)를 구독(subscribe)할 때
- 서로 알고 있는 당사자 간의 직접적인 양방향 통신(bidirectional communication)

**문제점**:
- 대부분의 액터는 자신의 프로토콜에서 임의의 응답 타입(arbitrary response type)을 받도록 지원하지 않음
- 전달되지 않거나 처리되지 않은 메시지를 감지하기 어려움
- 요청 컨텍스트(예: 요청 ID)를 인코딩하지 않으면 응답을 요청에 연관(correlate)시키기 위해 별도의 액터가 필요함

---

#### 3.3 적응된 응답(adapted response, 메시지 어댑터)

**설명**: `context.messageAdapter()`를 사용하여 한 액터의 프로토콜에서 온 응답을 수신 액터가 이해할 수 있는 메시지로 변환(translate)하는 패턴. 이는 수신 액터가 외부 메시지 타입을 받아들이지 않으면서도 호환되지 않는 프로토콜 사이에 타입 안전한 다리(type-safe bridge)를 놓음.

**프로토콜 예시**: `Frontend`가 `Backend`의 응답을 내부용 `WrappedBackendResponse` 메시지로 감싸서(wrap), 공개 프로토콜(public protocol)을 깨끗하게 유지하면서 백엔드 통신을 내부적으로 처리함.

**핵심 동작 원리**:
- 메시지 클래스당 하나의 메시지 어댑터만 존재 가능. 새 어댑터를 등록하면 기존 어댑터를 대체함.
- 어댑터는 수신 액터의 컨텍스트 내에서 실행되므로 상태에 안전하게 접근 가능.
- 적절한 생명주기 관리를 위해 어댑터는 `Behaviors.setup`이나 생성자(constructor)에서 등록.

**유용한 경우**:
- 서로 다른 액터 메시지 프로토콜 간의 변환
- 다른 액터로부터 많은 응답 메시지를 구독할 때
- 내부 통신을 처리하면서도 공개 프로토콜을 깨끗하게 유지할 때

**문제점**:
- 응답 타입당 하나의 어댑터만 가능하므로, 서로 다른 대상에 대한 유연성이 제한됨
- 전달/처리 실패를 감지하기 어려움
- 별도의 액터 없이 컨텍스트를 연관시키려면 (요청 ID 등의) 인코딩이 필요함

---

#### 3.4 액터 간 ask를 통한 요청-응답(request-response with ask between actors)

**설명**: `context.ask()`를 사용하여 자동 타임아웃 처리(automatic timeout handling)가 포함된 일대일(one-to-one) 요청-응답 상호작용을 구성함. 임시(temporary) `ActorRef[Response]`를 필요로 하는 메시지를 구성한 뒤, 그 응답이나 실패(failure)를 송신자가 처리할 수 있는 메시지로 변환함.

**프로토콜 예시**: Dave가 Hal에게 질문을 ask하면, Hal은 임시 참조로 응답을 보내고, Dave의 응답 핸들러는 그 결과를 추가 처리를 위한 `AdaptedResponse` 메시지로 변환함.

**핵심 동작 원리**:
- 암묵적(implicit) `Timeout` 매개변수 필요.
- 변환 함수(transformation function)는 성공 응답과 실패를 모두 받음.
- 타임아웃은 응답이 아니라 `TimeoutException` 실패를 야기함.
- 송신 액터는 적응된 메시지를 받은 후에도 정상적으로 계속 메시지를 처리함.

**유용한 경우**:
- 단일 응답(single-response) 질의
- 계속 진행하기 전에 메시지 처리를 검증할 때
- 응답이 누락된 경우의 재시도(retry) 처리
- 수신자를 과부하시키지 않도록 미해결(outstanding) 요청을 추적할 때
- 프로토콜을 수정하지 않고 컨텍스트를 첨부할 때

**문제점**:
- ask당 단일 응답만 지원함
- 수신자는 타임아웃을 알지 못하므로 타임아웃 이후에도 메시지를 처리할 수 있음
- ask를 연쇄(chain)하면 적절한 타임아웃 값을 찾기 어려워짐

---

#### 3.5 액터 외부에서의 ask를 통한 요청-응답(request-response with ask from outside an actor)

**설명**: 비액터 컨텍스트(non-actor context)인 외부 코드에서 `ask` 또는 `?`를 사용해 액터에게 메시지를 보냄. 이는 응답으로 완료되거나 `TimeoutException`으로 완료되는 `Future[Response]` 또는 `CompletionStage<Response>`를 반환함.

**의존성**: Scala에서는 `akka.actor.typed.scaladsl.AskPattern._`을 임포트(import)해야 하고, Java에서는 `AskPattern.ask()` 사용.

**프로토콜 예시**: 외부 서비스가 `CookieFabric` 액터에게 쿠키(cookie)를 ask하면, 그 ask는 `Cookies` 또는 `InvalidRequest` 응답으로 완료되는 Future를 반환함.

**유용한 경우**:
- 액터 시스템 외부에서 액터에 질의할 때
- 외부 API를 액터 시스템과 통합할 때

**문제점**:
- 반환된 Future의 콜백(callback)은 다른 스레드에서 실행되므로, 가변 상태(mutable state)에 대한 안전하지 않은 접근의 위험이 있음
- ask당 단일 응답만 가능함
- 타임아웃을 알지 못하는 수신자는 이미 완료된 요청을 계속 처리할 수 있음

---

#### 3.6 범용 응답 래퍼(generic response wrapper, StatusReply)

**설명**: `StatusReply[T]`를 사용하여 모든 상호작용 패턴에 걸쳐 성공/오류(success/error) 응답을 표준화함. 이 범용 래퍼(generic wrapper)는 성공 결과와 검증 오류(validation error)를 모두 지원하며, 클러스터 배포(clustered deployment)를 위한 직렬화기(serializer)가 내장됨.

**프로토콜 예시**: 액터는 성공에 대해 `StatusReply.Success(value)`로, 실패에 대해 `StatusReply.Error(text)`로 응답함. `askWithStatus` 같은 ask 메서드는 성공을 자동으로 풀어 주고(unwrap) 오류를 Failure로 변환함.

**주요 타입**:
- `StatusReply.Ack()`는 `StatusReply[Done]` 타입의 미리 만들어진 확인 응답(acknowledgment)을 제공함.
- 오류는 텍스트 설명으로 표현하는 것이 선호되지만, 예외(exception)에는 타입 정보를 첨부 가능.
- 내장 직렬화기는 클러스터링 시 보일러플레이트(boilerplate)를 줄여 줌.

**유용한 경우**:
- 응답이 성공/오류 쌍을 나타낼 때
- 여러 요청-응답 상호작용이 유사한 응답 패턴을 공유할 때
- 반복적인 상위 타입(supertype) 정의와 직렬화 설정을 피하고 싶을 때
- 검증 오류와 시스템 실패를 구별하고 싶을 때

---

#### 3.7 응답 무시하기(ignoring replies)

**설명**: 응답을 정의하고 있지만 송신자가 그 응답에 관심이 없는 메시지를 처리하기 위해 `system.ignoreRef()` 사용. 이는 요청-응답을 사실상 보내고 잊기(fire-and-forget)로 전환함.

**사용법**: 요청을 보낼 때 `replyTo` 필드로 `context.system.ignoreRef`(Scala) 또는 `context.getSystem().ignoreRef()`(Java) 전달.

**문제점**:
- ignore 참조는 모든 메시지를 버리므로, 부주의하게 전달될 경우 위험이 따름
- 외부에서 `ask`와 함께 사용하면 Future가 영구적으로 타임아웃됨
- ignore 참조를 감시(watch)해도 `Terminated` 시그널이 절대 발생하지 않음

---

#### 3.8 Future 결과를 자기 자신에게 보내기(send future result to self, pipeToSelf)

**설명**: `context.pipeToSelf()` 메서드는 비동기 `Future`/`CompletionStage`의 결과를 내부 메시지(internal message)로 감싸서, 블로킹(blocking) 없이 상태에 안전하게 접근할 수 있게 해 줌. 결과는 액터가 일반 receive 핸들러를 통해 처리하는 메시지로 매핑(map)됨.

**프로토콜 예시**: `CustomerRepository`가 Future를 반환하는 외부 API를 호출하고, 성공/실패를 내부용 `WrappedUpdateResult` 메시지로 매핑한 뒤, 스레드 안전성 문제 없이 이를 정상적으로 처리함.

**핵심 동작 원리**:
- 매핑 함수는 `Try[Value]` 또는 성공/실패를 분리한 매개변수를 받음.
- 매핑된 메시지는 액터의 일반 메일박스(mailbox)에 들어가 순서대로(ordered) 처리됨.
- 액터의 상태와 생명주기 안전성을 존중함.

**유용한 경우**:
- Future를 반환하는 데이터베이스나 외부 서비스 API를 통합할 때
- Future 완료 후 액터 처리를 계속할 때
- 원래 요청의 컨텍스트(요청 ID, 응답 참조)를 유지할 때
- 동시 연산(concurrent operation)의 개수를 안전하게 추적할 때

**문제점**:
- 래퍼 메시지(wrapper message)가 필요하여 보일러플레이트가 늘어남
- 단순한 통합에서는 복잡성이 증가함

---

#### 3.9 세션별 자식 액터(per session child actor)

**설명**: 특정 상호작용을 위해, 특히 여러 응답을 집계(aggregate)할 때 임시 자식 액터(temporary child actor)를 생성함. 각 세션 액터(session actor)는 요청 컨텍스트와 응답 핸들러를 받고, 작업이 끝나면 완료한 뒤 스스로를 중지함.

**프로토콜 유연성**: 세션 액터는 프로토콜에 구애받지 않는(protocol-agnostic) 상호작용을 위해 `Any`/`Object` 타입을 받을 수 있으며, 의존 대상에게 타입 안전한 메시지를 보내기 위해 참조를 좁혀(narrow) 사용함.

**프로토콜 예시**: `Home` 액터가 임시 `PrepareToLeaveHome` 자식들을 생성하고, 각 자식은 서로 다른 액터로부터 열쇠(keys)와 지갑(wallet)을 요청하여 응답을 집계한 뒤, 둘 다 도착하면 중지됨.

**핵심 패턴**: 세션 액터는 제네릭 타입을 받지만, 알려진 의존 대상에게 보낼 때는 `.narrow[SpecificType]()`을 사용하여 타입을 좁힘.

**유용한 경우**:
- 단일 요청이 응답하기 전에 여러 상호작용을 필요로 할 때
- 재시도, 타임아웃, 진행 추적(progress tracking) 같은 복잡한 로직을 구현할 때
- 서로 관련 없는 응답들을 하나의 결과로 집계할 때
- 확인/재시도(acknowledgment/retry) 처리가 있는 적어도-한-번(at-least-once) 전달

**문제점**:
- 생명주기 관리의 복잡성이 자원 누수(resource leak)의 위험을 낳음
- 세션 액터들이 동시에(concurrently) 실행되어 시스템 복잡성이 증가함
- 세션 종료에 실패하는 시나리오를 놓치기 쉬움

---

#### 3.10 범용 응답 집계기(general purpose response aggregator)

**설명**: 타임아웃을 지원하면서 여러 응답을 집계하는, 재사용 가능하고 설정 가능한(configurable) 액터 패턴. 내장 기능으로 제공되기보다는, 반복적인 집계 로직을 추출(extract)하는 방법을 보여 주는 문서의 형태로 제공됨.

**핵심 구성 요소**:
- `sendRequests` 함수: 모든 대상 액터에 요청을 보냄.
- `expectedReplies`: 기다릴 응답의 수.
- `aggregateReplies` 함수: 수집된 응답들을 결합함.
- `timeout`: 부분 결과(partial result)를 집계하기 전까지의 기간(duration).

**구현 특징**:
- 메시지 어댑터가 모든 응답을 공통 타입(common type)으로 감쌈.
- 타임아웃이 발생하면 그때까지 도착한 응답들을 집계함.
- 기대 응답 수에 도달하면 조기에 완료함.

**유용한 경우**:
- 여러 곳에서 동일한 집계 패턴을 수행할 때
- 단일 요청이 서로 관련 없는 여러 액터로부터 응답을 받아야 할 때
- 타임아웃 시 부분 결과를 집계해야 할 때
- 확인 응답을 통한 회복력(resilience)을 구현할 때

**문제점**:
- 제네릭 타입 소거(type erasure)가 타입 매개변수를 가진 프로토콜을 복잡하게 만듦
- 세션 생명주기 관리가 누수의 위험을 낳음
- 자식의 동시 실행이 복잡성을 증가시킴

---

#### 3.11 지연 꼬리 자르기(latency tail chopping)

**설명**: 초기 지연 후 여러 액터에게 "백업 요청(backup request)"을 보냄으로써 꼬리 지연(tail latency)을 줄이는 기법. 첫 번째 응답이 채택되고(wins), 이후의 응답은 무시됨. 이는 느린 응답자(slow responder)를 기다릴 확률을 낮춤.

**핵심 메커니즘**: 첫 번째 요청이 `nextRequestAfter` 기간 내에 응답하지 않으면, 다른 액터에게 요청을 보냄. `finalTimeout`이 작업을 완료할 때까지 이를 계속하며, 응답이 하나도 없으면 기본값(default value)을 반환함.

**프로토콜 패턴**:
- 여러 번의 백업 시도를 가능하게 하기 위해 요청 수를 추적함.
- 첫 번째 응답이 오면 조기에 반환함.
- 모든 요청이 실패하면 타임아웃 값으로 폴백(fallback)함.

**이론적 배경**: 대규모 서비스에서 중복 백엔드(redundant backend)에 작업을 병렬화하여 응답 시간을 줄이는 것에 관한 제프 딘(Jeff Dean)의 연구에 기반함.

**유용한 경우**:
- 여러 액터가 동일한 작업을 수행할 수 있을 때
- 가끔 발생하는 느린 응답자가 용납할 수 없는 꼬리 지연을 야기할 때
- 모든 액터가 느릴 경우 기본/캐시 응답이 허용될 때
- 요청 중복(request redundancy)이 용인될 때

**문제점**:
- 백업 요청을 통해 추가 부하(extra load)가 발생함
- 여러 개의 동일한 서비스 백엔드가 필요함
- 주 액터가 결국 응답할 경우 자원을 낭비할 수 있음

---

#### 3.12 자기 자신에게 메시지 예약하기(scheduling messages to self)

**설명**: `ActorContext`의 타이머(timer) 메서드를 사용하여 액터 자기 자신에게 전달되는 메시지를 예약함. 이를 통해 스레드를 막지 않고 타임아웃, 주기적 동작, 지연 연산 등 구현 가능.

**주요 기능**: 타이머는 단일(single) 또는 반복(recurring) 메시지를 시작할 수 있으며, 액터 행위 내에서 세션 타임아웃, 하트비트(heartbeat), 주기적 청소(periodic housekeeping) 같은 사용 사례를 지원함.

---

#### 3.13 샤딩된 액터에 응답하기(responding to a sharded actor)

**설명**: 샤딩(sharding)이 도입한 라우팅 계층(routing layer)을 고려하여, 적절한 응답 참조를 가진 메시지를 구성함으로써 샤딩된 액터에게 메시지를 보냄.

---

#### 3.14 Scala 3 유니온 타입(union type) 향상

최신 Scala에서는 유니온 타입(`Command | Response`)을 사용하여 일부 패턴을 단순화 가능. 이를 통해 액터가 래퍼 메시지 없이 여러 프로토콜 타입을 받을 수 있음. `.narrow` 메서드는 내부 유니온 타입을 받으면서도 공개 프로토콜의 타입 안전성을 유지함.

---

### 4. 내결함성과 슈퍼비전(fault tolerance and supervision)

#### 4.1 핵심 개념

Akka의 내결함성(fault tolerance) 프레임워크는 두 가지 중요한 실패 모드(failure mode)를 구별함.

- **검증 오류(validation error)**: 들어온 명령(command) 데이터가 유효하지 않을 때 발생함. 이는 예외(exception)를 발생시키기보다는 액터 프로토콜의 일부로 모델링되어야 함.
- **실패(failure)**: 끊어진 데이터베이스 연결처럼 예기치 못했거나 액터의 통제를 벗어난 무언가를 나타냄.

이 프레임워크는 "그냥 죽게 두라(let it crash)" 철학을 권장하며, 복구 로직(recovery logic)을 비즈니스 로직(business logic)과 뒤섞는 대신 그 책임을 시스템의 다른 곳으로 위임함.

슈퍼비전(supervision) 메커니즘은 특정 예외 타입이 액터 내에서 발생했을 때 무슨 일이 일어나야 하는지를 선언함으로써 실패를 처리함. 기본적으로 타입드 액터(typed actor)는 슈퍼비전 전략(supervision strategy)이 정의되어 있지 않으면 예외가 던져질 때 중지(stop)됨. 이는 기본적으로 재시작(restart)하는 클래식 Akka와의 중요한 차이점.

#### 4.2 슈퍼비전 전략(supervision strategy) 구현

슈퍼비전을 구현하려면 실제 액터 행위를 `Behaviors.supervise`로 감쌈(wrap). 다음은 `IllegalStateException` 발생 시 액터를 재시작하는 기본 예제.

```java
Behaviors.supervise(behavior)
    .onFailure(IllegalStateException.class, SupervisorStrategy.restart());
```

또는 실패를 무시하고 다음 메시지를 계속 처리하도록 재개(resume)하려면 다음과 같이 함.

```java
Behaviors.supervise(behavior)
    .onFailure(IllegalStateException.class, SupervisorStrategy.resume());
```

#### 4.3 고급 재시작 전략(advanced restart strategies)

더 정교한 접근 방식은 재시작 빈도(restart frequency)를 제한함. 예를 들어 10초 윈도우(window) 내에서 10번을 초과하지 않도록 재시작하려면 다음과 같이 함.

```java
Behaviors.supervise(behavior)
    .onFailure(
        IllegalStateException.class,
        SupervisorStrategy.restart().withLimit(10, Duration.ofSeconds(10)));
```

여러 개의 슈퍼비전 계층(supervision layer)을 중첩(nest)된 `supervise` 호출을 통해 서로 다른 예외를 각기 다른 전략으로 처리 가능.

```java
Behaviors.supervise(
    Behaviors.supervise(behavior)
        .onFailure(IllegalStateException.class, SupervisorStrategy.restart()))
    .onFailure(IllegalArgumentException.class, SupervisorStrategy.stop());
```

#### 4.4 함수형 스타일에서의 행위 감싸기(behavior wrapping in functional style)

행위를 변경함으로써 상태를 저장하는 함수형 스타일 액터의 경우, 슈퍼비전은 최상위 레벨(top level)에서만 감싸야 함.

```scala
def apply(): Behavior[Command] =
  Behaviors.supervise(counter(1)).onFailure(SupervisorStrategy.restart)
```

반환되는 각 행위는 슈퍼바이저(supervisor)로 자동으로 다시 감싸지므로, 슈퍼비전 선언을 중복할 필요 없음.

#### 4.5 재시작 중의 자식 액터 관리(child actor management during restart)

부모 액터가 재시작하면, 자원 누수를 방지하기 위해 자식 액터들은 기본적으로 중지됨. 자식 액터는 일반적으로 `setup` 블록 안에서 생성되며, 이 블록은 부모 재시작 시 다시 실행됨.

그러나 슈퍼비전을 `setup` 블록 안에 두고 `withStopChildren(false)`를 사용하면 부모가 재시작하더라도 자식 액터 보존 가능.

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

이 구성은 `setup` 블록이 최초 시작(initial startup) 시에만 실행되도록 보장하여, 부모 재시작에 걸쳐 자식 액터 참조를 보존함.

#### 4.6 PreRestart 시그널

슈퍼비전 대상 액터가 재시작하기 전에, 그 액터는 `PreRestart` 시그널을 받음. 이는 `PostStop` 시그널과 유사하게 자원 정리를 가능하게 함. `PreRestart`에서 반환된 행위는 무시됨.

```java
Behaviors.receiveMessage(...)
    .receiveSignal((ctx, signal) -> {
      if (signal instanceof PreRestart || signal instanceof PostStop) {
        resource.close();
      }
      return Behaviors.same();
    })
```

재시작 중에는 `PostStop`이 방출되지 않으므로, `PreRestart`와 `PostStop`을 모두 처리하면 포괄적인(comprehensive) 자원 정리가 보장됨.

#### 4.7 실패를 계층 위로 올리기(bubbling failures up the hierarchy)

부모는 자식을 감시(watch)함으로써 실패 처리 결정을 상위로 위임 가능. 자식이 실패로 인해 종료되면, 부모는 그 원인(cause)을 담은 `ChildFailed` 시그널을 받음. `ChildFailed`는 `Terminated`를 확장(extend)하므로, 중지와 실패를 구별할 필요가 없을 때는 통합하여 처리 가능.

부모가 `Terminated`를 처리하지 않으면 `DeathPactException`으로 실패함. 이를 통해 각 액터가 구현 세부 사항(implementation detail)을 노출하지 않으면서 부모에게 알리는 계층적 실패 전파(hierarchical failure propagation)가 가능해짐. 직속 부모(immediate parent)는 원래 예외에 접근 가능한 반면, 더 상위의 부모는 `DeathPactException`만 보게 됨.

예외를 여러 계층에 걸쳐 위로 올려야 하는 시나리오에서는 명시적인 처리 필요. 각 액터가 `Terminated`를 잡아서(catch) 예외를 다시 던짐(rethrow).

```java
Behaviors.watch(child)
    .receiveSignal((ctx, signal) -> {
      if (signal instanceof ChildFailed) {
        throw ((ChildFailed) signal).getCause();
      }
      return Behaviors.same();
    })
```

이 계층적 접근 방식은 최상위 슈퍼바이저가 포괄적인 복구 결정을 내리도록 하는 한편, 하위 레벨은 당장의 관심사(immediate concern)에 집중하게 함.

---

### 5. 액터 디스커버리(actor discovery)

#### 5.1 개요

액터 디스커버리(actor discovery)는 생성자 매개변수나 메시지를 통한 직접적인 참조 전달이 비현실적일 때, 액터가 다른 액터를 찾아 통신할 수 있게 하는 과제를 다룸. 이는 액터가 여러 노드에 걸쳐 동작하는 분산 클러스터(distributed cluster) 시나리오에서 특히 중요함.

#### 5.2 액터 참조 얻기(obtaining actor references)

프레임워크는 액터 참조를 획득하는 두 가지 주요 메커니즘을 제공함. 첫 번째는 액터를 직접 생성하고 그 참조를 생성자 매개변수나 메시지에 담아 액터 사이에 전달하는 것. 그러나 이 접근 방식이 부적절할 때 — 예를 들어 서로 다른 클러스터 노드에 있는 액터 간 상호작용을 부트스트랩(bootstrap)할 때, 또는 전통적인 의존성 주입(dependency injection)을 적용할 수 없을 때 — 리셉셔니스트(Receptionist) 서비스가 대안적인 디스커버리 패턴을 제공함.

#### 5.3 리셉셔니스트(the Receptionist)

리셉셔니스트(Receptionist)는 로컬과 클러스터 배포를 모두 지원하는 동적 레지스트리(dynamic registry) 서비스로 동작함. 메시지 기반 API(message-based API)를 통해 작동하며, 액터가 디스커버리를 위해 스스로를 등록(register)하고 다른 액터가 등록된 서비스를 찾을 수 있게 함.

##### 등록 과정(registration process)

액터는 `ServiceKey`를 사용해 리셉셔니스트에 등록함. `ServiceKey`는 관련된 서비스 인스턴스(service instance)들을 그룹화하는 타입이 지정된 식별자(typed identifier). 액터가 초기화될 때, 자신의 서비스 키와 자기 참조(self-reference)를 담은 `Receptionist.Register` 메시지를 시스템의 리셉셔니스트 인스턴스로 보냄.

예를 들어, `PingService`는 다음과 같이 자신을 등록함.

```scala
context.system.receptionist ! Receptionist.Register(PingServiceKey, context.self)
```

여러 액터가 동일한 `ServiceKey`에 대해 등록 가능하며, 이는 디스커버리에 사용할 수 있는 서비스 인스턴스의 모음(collection)을 만듦.

##### 액터 찾기: Listing 응답(finding actors: the Listing response)

클라이언트 액터가 서비스를 발견해야 할 때, 단일 스냅샷(single snapshot)이 필요하면 `Receptionist.Find` 메시지를 보내고, 지속적인 업데이트(continuous update)가 필요하면 `Receptionist.Subscribe` 메시지를 보냄. 두 질의 모두 요청한 `ServiceKey`에 일치하는 액터 참조들의 `Set`을 담은 `Listing` 응답을 반환함.

`Listing`은 해당 키에 대해 등록된 액터 참조들의 `Set`을 제공함.

##### 구독 모델(subscription model)

`Receptionist.Subscribe` 방식은 등록 변경(registration change)에 대한 동적 인식(dynamic awareness)을 제공함. 구독 시, 리셉셔니스트는 현재 등록된 액터들을 담은 `Listing`을 즉시 보내고, 이후 등록이 변경될 때마다 갱신된 `Listing` 메시지를 계속 보냄. 이로 인해 구독은 사용 가능한 서비스에 대한 실시간 인식(real-time awareness)이 필요한 시나리오에 이상적임.

가디언 액터가 구독하는 방법은 다음과 같음.

```scala
context.system.receptionist ! Receptionist.Subscribe(PingService.PingServiceKey, context.self)
```

구독한 액터는 그 후 `Listing` 메시지를 받음.

```scala
case PingService.PingServiceKey.Listing(listings) =>
  listings.foreach(ps => context.spawnAnonymous(Pinger(ps)))
```

##### Find 질의(find queries)

일회성 조회(one-time lookup)의 경우 `Receptionist.Find` 패턴이 더 적합함. 이 접근 방식은 지속적인 구독을 수립하지 않고 현재 상태(current state)를 질의함. 메시지 어댑터를 사용해 `Receptionist.Listing` 응답을 요청 액터의 메시지 타입으로 변환함.

```scala
val listingResponseAdapter = context.messageAdapter[Receptionist.Listing](ListingResponse.apply)
context.system.receptionist ! Receptionist.Find(PingService.PingServiceKey, listingResponseAdapter)
```

#### 5.4 등록 해제(deregistration)

더 이상 서비스 등록이 필요 없는 액터는 `Receptionist.Deregister`를 사용해 등록 해제 가능. 이 명령은 로컬 리셉셔니스트에서 해당 항목을 제거하고 모든 구독자(subscriber)에게 통지함. 등록 해제 명령은 선택적으로 로컬 제거를 확인하는 응답(acknowledgement)을 반환할 수 있으나, 이 확인 응답이 모든 구독자가 인스턴스 제거를 확인했음을 보장하지는 않음. 즉, 이후로도 한동안 구독자로부터 메시지를 받을 수 있음.

```scala
context.system.receptionist ! Receptionist.Deregister(PingService.PingServiceKey, context.self)
```

#### 5.5 클러스터 리셉셔니스트(cluster receptionist)

클러스터 배포에서, 리셉셔니스트는 분산 데이터(distributed data) 메커니즘을 사용하여 자신의 레지스트리를 노드 전반에 자동으로 분산시킴. 이로써 각 노드가 결국 일관된(eventually consistent) 서비스 정보를 유지하게 됨. 등록된 액터는 로컬에 머무르지 않고 클러스터 전역(cluster-wide)에서 발견 가능해짐.

클러스터 리셉셔니스트는 표준 `Listing`을 클러스터 인식 필터링(cluster-aware filtering)으로 향상시킴. 표준 질의는 클러스터 멤버십(membership)과 연결성을 존중하여 도달 가능한(reachable) 액터만 반환함. 그러나 도달 불가능한(unreachable) 인스턴스를 포함한 전체 집합은 `Listing.allServiceInstances`를 통해 여전히 접근 가능.

클러스터 시나리오에서는 직렬화(serialization)가 중요해짐. 원격 액터(remote actor)와 교환되는 모든 메시지가 직렬화를 지원해야 하기 때문.

#### 5.6 생명주기 관리(lifecycle management)

레지스트리는 동적인 특성을 보임. 새로운 등록은 시스템 생애 동안 계속 일어남. 등록된 액터가 중지될 때, 명시적인 등록 해제가 일어날 때, 또는 호스팅 노드가 클러스터를 떠날 때 항목(entry)은 자동으로 제거됨.

#### 5.7 확장성 고려 사항(scalability considerations)

리셉셔니스트는 적당한 규모의 서비스 모집단(service population)에 대해 실용적인 디스커버리를 제공하며, 수천 개에서 수만 개(up to thousands or tens of thousands)의 서비스를 처리 가능. 그러나 서비스 수가 이보다 현저히 많은 시스템에서는, 리셉셔니스트가 액터 간의 초기 접촉(initial contact)만을 중개하고 이후의 상호작용은 애플리케이션 고유 로직(application-specific logic)이 관리하는 대안적인 패턴을 사용해야 함.

---

### 6. 라우터(routers)

#### 6.1 개요

Akka Typed는 메시지를 여러 액터에 분배(distribute)하여 병렬 처리(parallel processing)를 가능하게 하는 라우터(router)를 제공함. 어떤 경우에는 동일한 타입의 메시지를 액터들의 집합에 분배하여 메시지가 병렬로 처리되도록 하는 것이 유용함.

라우터 자체는 들어오는 메시지를 라우티(routee)들의 집합 중 하나의 수신자에게 전달하는 액터. Akka Typed에는 두 가지 주요 라우터 타입, 즉 풀 라우터(pool router)와 그룹 라우터(group router)가 있음.

#### 6.2 풀 라우터(pool router)

##### 개념과 동작

풀 라우터는 제공된 행위 정의를 사용하여 자식 액터들을 생성한 뒤, 그 자식들에게 메시지를 전달함. 풀 라우터는 라우티 `Behavior`로 생성되며, 그 행위를 가진 다수의 자식들을 생성하고 그들에게 메시지를 전달함.

중요한 특성: 자식이 중지되면 풀 라우터는 그것을 라우티 집합에서 제거함. 마지막 자식이 중지되면 라우터 자체도 중지됨.

풀 라우터는 본질적으로 로컬(local)임. 라우티는 클러스터 전반에 분산되지 않음.

##### 풀 라우터 생성

먼저 라우티 행위를 정의함. 다음은 간단한 워커(worker) 예제.

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

이제 풀 라우터를 생성하고 spawn함.

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

슈퍼비전 래퍼(supervision wrapper)에 주목. 이것이 없으면 워커의 실패가 라우터를 함께 죽게(crash) 할 수 있음.

##### 디스패처 설정(dispatcher configuration)

라우티가 사용할 디스패처를 `withRouteeProps()`를 통해 설정함.

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

라우터 액터 자체는 `spawn()` 호출에 지정된 디스패처를 사용함.

##### 모든 라우티에 브로드캐스트(broadcasting to all routees)

풀 라우터는 `withBroadcastPredicate()`를 통해 선택적 브로드캐스팅(selective broadcasting)을 지원함. 술어(predicate)에 일치하는 메시지는 모든 라우티에게 전달됨.

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

#### 6.3 그룹 라우터(group router)

##### 개념과 클러스터 인식

그룹 라우터는 `ServiceKey`를 사용해 리셉셔니스트를 통해 라우티를 발견하며, 따라서 자동으로 클러스터를 인식(cluster-aware)함. 그룹 라우터는 `ServiceKey`로 생성되고, 리셉셔니스트를 사용하여 그 키에 대해 사용 가능한 액터들을 발견한 뒤, 그 키에 대해 현재 알려진 등록된 액터 중 하나로 메시지를 라우팅함.

리셉셔니스트를 사용한다는 것은 결과적 일관성(eventual consistency)을 함의함. 라우터는 클러스터 내에서 도달 가능한 임의의 노드에 등록된 액터들에게 메시지를 보냄.

##### 스태싱(stashing) 동작

중요한 특성: 그룹 라우터는 리셉셔니스트로부터 등록된 서비스의 첫 번째 목록(listing)을 볼 때까지 메시지를 스태시(stash)함. 이는 그룹 라우터를 생성한 직후에 메시지를 보내는 것이 안전함을 의미함. 메시지는 리셉셔니스트가 응답할 때까지 큐에 대기함.

라우터가 등록된 집합이 비어 있음을 알게 되면 메시지를 버리며, 이를 이벤트 스트림(event stream)에 `akka.actor.Dropped`로 발행(publish)함.

##### 구현

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

워커들은 `Receptionist.Register()`를 통해 스스로를 등록하고, 그룹 라우터는 자동으로 그들을 발견함.

#### 6.4 라우팅 전략(routing strategies)

Akka Typed는 spawn 전에 설정할 수 있는 세 가지 라우팅 전략(routing strategy)을 제공함.

##### 라운드 로빈(round robin)

라운드 로빈은 라우티들을 순차적으로(sequentially) 순환함. `n`개의 라우티와 `n`개의 메시지가 있으면, 각 액터는 정확히 하나의 메시지를 처리함. 이 전략은 풀 라우터의 기본값(default).

라운드 로빈은 라우티 집합이 비교적 안정적으로 유지되는 한, 사용 가능한 모든 라우티가 동일한 양의 메시지를 받는 공정한 라우팅(fair routing)을 제공함.

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

선택적인 `preferLocalRoutees` 매개변수 존재. 이 값이 true이면, 라우터는 가능한 경우 로컬 라우티를 우선함.

##### 랜덤(random)

랜덤 선택은 각 메시지마다 임의로 라우티를 고름. 이는 그룹 라우터의 기본값. 클러스터에서 라우티 멤버십이 동적으로 변하기 때문.

라운드 로빈과 마찬가지로, 이 전략도 `preferLocalRoutees` 설정을 받음.

##### 일관된 해싱(consistent hashing)

이 전략은 메시지 내용에 기반하여 라우티에 메시지를 할당하기 위해 일관된 해싱(consistent hashing)을 사용함. 즉, 전송된 메시지에 기반하여 라우티를 선택하기 위해 일관된 해싱을 사용함.

필수적인 해시 매핑 함수(hash mapping function)가 라우팅 결정을 투명하게(transparent) 만듦. 라우티 집합이 동일하게 유지되는 한, 같은 해시를 가진 메시지는 항상 같은 라우티에게 전달됨.

라우티 집합이 변경될 때, 일관된 해싱은 같은 해시를 가진 메시지가 같은 라우티로 라우팅되도록 최대한 보장하지만 이를 완전히 보장하지는 않음.

일관된 해싱을 보여 주는 프록시(proxy) 예제는 다음과 같음.

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

같은 해시 키("123" 또는 "zh3")를 가진 메시지는 같은 액터로 라우팅됨. `withConsistentHashingRouting()` 메서드는 가상 노드(virtual nodes) 수(10)와 매핑 함수를 인자로 받음.

안정적인 재균형(rebalancing)이 필요한 시나리오에는 Akka 클러스터 샤딩(Akka Cluster Sharding)을 대안으로 권장함.

#### 6.5 성능 고려 사항(performance considerations)

라우터 자체는 액터이며 메일박스를 가짐. 이는 메시지가 라우티들로 순차적으로(sequentially) 라우팅된다는 것을 의미하며, 라우티에서는 병렬로(in parallel) 처리될 수 있음.

단일 액터를 통한 이 순차적 라우팅은 고처리량(high-throughput) 시스템에서 병목(bottleneck)이 될 수 있음. 또한 라우티들이 자원을 공유하는 경우, 그 자원이 액터 수를 늘리는 것이 실제로 더 높은 처리량이나 더 빠른 응답을 줄지를 결정하게 됨.

CPU 바운드(CPU-bound) 라우티는 사용 가능한 스레드 수를 초과해도 이점을 얻지 못함. Akka Typed는 직접적인 병렬 메시지 분배가 필요한 극단적 처리량(extreme-throughput) 시나리오를 위한 특수 최적화를 제공하지 않음.

---

### 7. 스태시(stash)

#### 7.1 스태싱(stashing) 개요

스태싱(stashing)은 현재 행위에서 처리할 수 없거나 처리해서는 안 되는 메시지를 액터가 일시적으로 보류(hold)하는 메커니즘. 메시지를 즉시 처리하는 대신, 조건이 바뀌거나 자원이 사용 가능해질 때 나중에 처리하도록 큐에 넣을 수 있음.

프레임워크는 이 기능을 `StashBuffer` API를 통해 제공하며, 액터는 이를 사용해 메시지 처리를 전략적으로 지연(defer) 가능. 이는 특히 두 가지 흔한 시나리오에서 가치가 있음. 하나는 액터가 일반 메시지를 받아들이기 전에 초기 상태(initial state)를 로드해야 할 때이고, 다른 하나는 데이터베이스 쓰기 같은 특정 연산을 직렬화(serialize)해야 할 때.

#### 7.2 DataAccess 액터 예제

이 문서는 단일 값을 관리하는 데이터베이스 접근 액터(database access actor)를 통해 실용적인 예시를 제시함. 이 액터가 시작될 때, 클라이언트 요청에 응답하기 전에 데이터베이스로부터 현재 상태를 가져와야 함. 이 로딩 기간 동안 들어오는 모든 메시지는 스태시됨. 마찬가지로, 업데이트를 영속화(persist)할 때 액터는 동시 쓰기(concurrent write)가 아닌 순차 처리(sequential processing)를 보장하기 위해 새 요청을 스태시함.

Scala 구현은 액터 행위를 `Behaviors.withStash(100)`로 감쌈으로써 이 패턴을 보여 줌. 이는 100개의 메시지를 담을 수 있는 용량(capacity)의 버퍼를 할당함. 액터는 로딩 단계(loading phase)에서 시작하여, 초기 상태가 데이터베이스로부터 도착했음을 확인하는 응답을 제외한 모든 메시지를 스태시함.

#### 7.3 StashBuffer 생성과 설정

스태싱을 활성화하려면 액터 행위를 `Behaviors.withStash(capacity)`로 감싸고, 버퍼가 보유할 수 있는 메시지의 수를 지정함. `capacity` 매개변수는 필수. 버퍼가 보유할 수 있는 메시지의 수는 생성 시점에 반드시 지정되어야 하기 때문.

Java 버전의 설정은 다음과 같음.

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

이는 `StashBuffer<Command>`를 생성하여 액터 생성자에 전달하므로, 액터의 생애 전반에 걸쳐 사용 가능해짐.

#### 7.4 메시지 스태싱(stashing messages)

액터가 즉시 처리할 수 없는 메시지를 받으면, `buffer.stash(message)`를 호출하여 그것을 지연시킴. DataAccess 예제에서는 데이터베이스가 초기 상태를 반환하기를 기다리는 동안, 액터가 들어오는 모든 `Get`과 `Save` 명령을 스태시함.

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

Java에서도 메시지 핸들러 내에서 유사한 패턴을 사용함.

#### 7.5 스태시 용량 관리(managing stash capacity)

스태시된 메시지는 언스태시(unstash)되거나 액터가 중지되어 가비지 컬렉션(garbage collection)될 때까지 메모리에 유지됨. 스태시된 메시지가 많으면 메모리 부담이 커지므로 버퍼는 경계(bounded)를 가짐. 용량을 초과하여 스태시를 시도하면 `StashOverflowException`이 던져짐.

스태시하기 전에 `StashBuffer.isFull`을 확인하여 오버플로 회피 가능. 버퍼가 용량에 도달하면, 메시지를 버리거나, 송신자에게 거부 응답(rejection response)을 보내거나, 사용 사례에 적합한 다른 방어적 조치를 취할 수 있음.

#### 7.6 언스태싱과 처리 재개(unstashing and resuming processing)

액터가 큐에 대기 중인 메시지를 처리할 준비가 되면, `buffer.unstashAll(behavior)`을 호출하여 새 행위로 전이하면서 스태시된 모든 메시지를 순서대로(in order) 처리함.

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

새 행위는 새로 도착하는 메시지를 처리하기 전에 스태시된 모든 메시지를 순차적으로 받음. 이는 메시지 순서(message ordering)를 유지하고, 오래된 요청과 새 요청이 뒤섞이는 것(interleaving)을 방지함.

#### 7.7 스태시된 메시지의 순차 처리(processing stashed messages sequentially)

언스태싱이 일어나면, 메시지는 추가된 순서대로 순차적으로 처리되며, 예외가 던져지지 않는 한 모두 처리됨. 액터는 스태시된 모든 메시지가 처리될 때까지 새 메시지에 응답하지 않게 됨. 이 직렬화는 일관성(consistency)을 보장하지만, 스태시에 많은 메시지가 있을 경우 액터의 스레드를 막아 다른 액터를 굶주리게(starve) 할 수 있음.

메시지 처리 스레드를 너무 오래 독점(hog)하는 액터는 다른 액터의 기아(starvation)를 초래할 수 있음. 스태시된 메시지 수를 낮게 유지하면 이 위험이 완화됨.

#### 7.8 부분 처리를 통한 고급 언스태싱(advanced unstashing with partial processing)

매우 큰 스태시가 있는 시나리오를 위해, 프레임워크는 `numberOfMessages` 매개변수를 가진 `StashBuffer.unstash`를 제공하여 메시지를 배치(batch)로 처리 가능. 제한된 수의 메시지를 처리한 후, 언스태싱을 계속하기 전에 `context.self`로 메시지를 보낼 수 있음. 그러나 이 접근 방식은 처리를 복잡하게 만듦. 배칭 과정 중에 도착하는 새 메시지도 순서를 보존하기 위해 스태시되어야 하기 때문. 권장 사항은 여전히, 스태시된 메시지 수를 낮게 유지하는 것이 낫다는 것.

#### 7.9 DataAccess의 실제 워크플로(practical workflow in DataAccess)

DataAccess 액터는 완전한 워크플로를 보여 줌.

1. **초기화 단계(Initialization Phase)**: 생성 시 액터는 데이터베이스 로드를 시작하고 들어오는 모든 요청을 스태시함.

2. **활성 단계(Active Phase)**: 초기 상태가 도착하면, 대기 중인 모든 메시지를 언스태시하고 `active` 행위로 진입하여, 정상적으로 `Get`과 `Save` 명령을 처리함.

3. **저장 단계(Saving Phase)**: `Save` 명령이 도착하면, 액터는 `saving` 행위로 전이하여 데이터베이스 쓰기를 시작하면서 새 요청을 스태시함.

4. **완료(Completion)**: 저장이 완료되면, 액터는 대기 중인 메시지를 언스태시하고 `active` 행위로 돌아감.

이 패턴은 데이터베이스 연산이 순차적으로 유지되도록 하고, 초기화나 상태 갱신 중의 타이밍 문제로 인해 어떤 클라이언트 요청도 분실되지 않도록 보장함.

---

### 8. 유한 상태 기계(finite state machines, FSM)

#### 8.1 개요

Akka의 타입드 액터 프레임워크는 행위(behavior)를 통해 유한 상태 기계(Finite State Machine, FSM)를 모델링할 수 있게 함. 클래식 FSM 접근 방식과 달리, 타입드 시스템은 각 상태(state)를 별개의 행위로 표현하여 타입 안전성과 더 명확한 코드 구성을 제공함.

#### 8.2 핵심 개념

##### 메시지로서의 이벤트(events as messages)

타입드 Akka에서 FSM의 기반은 액터가 받을 수 있는 메시지 타입이 되는 이벤트(event)를 정의하는 것. 예를 들어, Buncher 액터는 다음과 같은 여러 이벤트 타입으로 이 패턴을 보여 줌.

- `SetTarget`: 액터를 목적지 참조(destination reference)로 초기화함.
- `Queue`: 버스트(burst) 기간 동안 내부 큐(internal queue)에 객체를 추가함.
- `Flush`: 버스트의 끝을 알림.
- `Timeout`: 상태 타임아웃(state timeout)에 의해 트리거됨.

각 이벤트는 공통의 sealed trait나 인터페이스를 구현하여, 모든 상태 핸들러(state handler)에 걸쳐 타입 안전성을 보장함.

##### 상태 데이터 표현(state data representation)

상태별 데이터(state-specific data)는 전용 데이터 타입을 통해 유지됨. Buncher 예제는 다음을 사용함.

- `Uninitialized`: 설정 전의 시작 상태(startup state)를 나타냄.
- `Todo`: 동작 중에 대상 액터 참조와 큐에 쌓인 메시지들을 보관함.

이 분리(separation)는 각 행위 메서드가 자신의 상태에 관련된 데이터만 접근하도록 함.

##### 행위로서의 상태(states as behaviors)

각 FSM 상태는 `Behavior` 객체를 반환하는 메서드가 됨. 핵심 패턴은 다음을 포함함.

1. 정의된 이벤트 타입의 메시지를 받음.
2. 이벤트와 현재 상태에 대해 패턴 매칭(pattern matching)을 수행함.
3. 다음 행위(새 상태)를 반환함.

**Idle 상태**는 비활성 상태일 때 초기 설정과 큐잉(queuing)을 처리함. `Uninitialized` 상태에서 `SetTarget` 메시지가 도착하면, 초기화된 데이터(initialized data)를 가진 idle 상태로 전이함. idle 상태에서 받은 `Queue` 메시지는 **Active 상태**로의 전이를 트리거함.

**Active 상태**는 타임아웃 관리를 동반한 버스트 처리(burst processing)를 다룸. 메시지는 계속 큐에 쌓이며, `Flush` 또는 타임아웃 이벤트는 배치(batch)된 메시지를 대상 액터에게 보내고 idle 상태로 돌아가도록 트리거함.

#### 8.3 타이머 구현(implementing timers)

상태 타임아웃은 `TimerScheduler`를 제공하는 `Behaviors.withTimers` 래퍼를 사용함. active 상태 내에서는 다음과 같이 사용함.

```scala
timers.startSingleTimer(Timeout, 1.second)
```

이는 타임아웃 메시지를 예약하며, 이 메시지는 명시적인 플러시(flush) 요청과 마찬가지로 액터가 누적된 메시지를 디스패치(dispatch)하고 idle 상태로 되돌아가게 함.

#### 8.4 처리되지 않은 메시지 다루기(handling unhandled messages)

예기치 못한 메시지를 다루기 위한 세 가지 행위가 있음.

- `Behaviors.unhandled`: 처리되지 않은 메시지를 로깅하면서 현재 행위를 유지함.
- `Behaviors.empty`: 액터가 더 이상 메시지를 기대하지 않음을 나타냄.
- `Behaviors.ignore`: 로깅 없이 처리되지 않은 모든 메시지를 조용히 버림.

#### 8.5 상태 간 전이(transitioning between states)

상태 전이는 메시지 핸들러에서 새 행위를 반환함으로써 일어남. 이 명시적 패턴은 전통적인 FSM 상태 선언을 대체하며, FSM 생애 전반에 걸쳐 더 명확한 제어 흐름(control flow)과 타입 안전성을 제공함.

---

### 참고 자료

- [Akka 공식 문서](https://doc.akka.io/libraries/akka-core/current/)
- [Actors 소개](https://doc.akka.io/libraries/akka-core/current/typed/actors.html)
- [Actor lifecycle](https://doc.akka.io/libraries/akka-core/current/typed/actor-lifecycle.html)
- [Interaction Patterns](https://doc.akka.io/libraries/akka-core/current/typed/interaction-patterns.html)
- [Fault Tolerance](https://doc.akka.io/libraries/akka-core/current/typed/fault-tolerance.html)
- [Actor discovery](https://doc.akka.io/libraries/akka-core/current/typed/actor-discovery.html)
- [Routers](https://doc.akka.io/libraries/akka-core/current/typed/routers.html)
- [Stash](https://doc.akka.io/libraries/akka-core/current/typed/stash.html)
- [Finite State Machines](https://doc.akka.io/libraries/akka-core/current/typed/fsm.html)

---

## Akka 디스패처, 메일박스, 테스트, 스타일 가이드

> 원본: https://doc.akka.io/libraries/akka-core/current/

---

### 목차

1. [디스패처(Dispatchers)](#1-디스패처dispatchers)
   - [1.1 디스패처란 무엇인가](#11-디스패처란-무엇인가)
   - [1.2 기본 디스패처와 내부 디스패처](#12-기본-디스패처와-내부-디스패처)
   - [1.3 디스패처 선택(DispatcherSelector)](#13-디스패처-선택dispatcherselector)
   - [1.4 디스패처 종류](#14-디스패처-종류)
   - [1.5 Fork-Join 실행기 설정](#15-fork-join-실행기-설정)
   - [1.6 Thread-Pool 실행기 설정](#16-thread-pool-실행기-설정)
   - [1.7 블로킹(blocking) 작업 처리](#17-블로킹blocking-작업-처리)
   - [1.8 디스패처 별칭(aliases)과 조회(lookup)](#18-디스패처-별칭aliases과-조회lookup)
2. [메일박스(Mailboxes)](#2-메일박스mailboxes)
   - [2.1 메일박스란 무엇인가](#21-메일박스란-무엇인가)
   - [2.2 메일박스 선택(MailboxSelector)](#22-메일박스-선택mailboxselector)
   - [2.3 기본 제공 메일박스 구현](#23-기본-제공-메일박스-구현)
   - [2.4 커스텀 메일박스 구현](#24-커스텀-메일박스-구현)
3. [테스트(Testing)](#3-테스트testing)
   - [3.1 개요와 모듈 설정](#31-개요와-모듈-설정)
   - [3.2 비동기 테스트(Asynchronous testing)](#32-비동기-테스트asynchronous-testing)
   - [3.3 동기 테스트(Synchronous behavior testing)](#33-동기-테스트synchronous-behavior-testing)
4. [스타일 가이드(Style Guide)](#4-스타일-가이드style-guide)
   - [4.1 함수형 스타일 vs 객체지향 스타일](#41-함수형-스타일-vs-객체지향-스타일)
   - [4.2 너무 많은 파라미터 전달 문제](#42-너무-많은-파라미터-전달-문제)
   - [4.3 메시지를 어디에 정의할 것인가](#43-메시지를-어디에-정의할-것인가)
   - [4.4 공개 vs 비공개 메시지](#44-공개-vs-비공개-메시지)
   - [4.5 람다 vs 메서드 참조, ask 패턴](#45-람다-vs-메서드-참조-ask-패턴)
   - [4.6 명명 규칙(Naming conventions)](#46-명명-규칙naming-conventions)
5. [코디네이티드 셧다운(Coordinated Shutdown)](#5-코디네이티드-셧다운coordinated-shutdown)
   - [5.1 개요](#51-개요)
   - [5.2 셧다운 단계(phases)](#52-셧다운-단계phases)
   - [5.3 태스크 등록](#53-태스크-등록)
   - [5.4 셧다운 실행](#54-셧다운-실행)
   - [5.5 단계 설정과 JVM 종료](#55-단계-설정과-jvm-종료)
6. [Typed와 Classic의 공존(Coexisting)](#6-typed와-classic의-공존coexisting)
   - [6.1 개요](#61-개요)
   - [6.2 ActorSystem 변환](#62-actorsystem-변환)
   - [6.3 액터의 생성과 관리](#63-액터의-생성과-관리)
   - [6.4 슈퍼비전(supervision) 차이](#64-슈퍼비전supervision-차이)

---

### 1. 디스패처(Dispatchers)

#### 1.1 디스패처란 무엇인가

Akka의 `MessageDispatcher`는 Akka 액터(actor)를 "동작하게 만드는" 핵심 요소. 비유하자면 기계를 움직이게 하는 엔진(engine)과 같은 존재. 모든 `MessageDispatcher` 구현체는 `ExecutionContext`(스칼라의 실행 컨텍스트)이자 `Executor`(자바의 실행기) 역할을 동시에 수행함. 따라서 임의의 코드를 실행하는 데에도 사용 가능하며, 예를 들어 `Future`(스칼라) 또는 `CompletableFuture`(자바)를 실행하는 용도로 활용 가능.

디스패처(dispatcher)는 액터로 들어온 메시지를 처리하기 위해 스레드(thread)를 할당하는 메커니즘을 담당함. 즉, 어떤 액터가 어떤 스레드 풀(thread pool) 위에서 메시지를 처리할지를 결정함.

---

#### 1.2 기본 디스패처와 내부 디스패처

모든 `ActorSystem`은 기본 디스패처(default dispatcher)를 가지며, 별도로 디스패처를 설정하지 않은 경우 자동으로 이 기본 디스패처가 사용됨. 기본 디스패처는 일반적으로 "fork-join-executor" 설정을 사용하며, 이 설정은 대부분의 경우 매우 우수한 성능(excellent performance)을 제공함.

이와 별개로 Akka는 전용 내부 디스패처(internal dispatcher)를 유지함. 이 내부 디스패처는 프레임워크가 생성하는 액터(framework-spawned actors)를 사용자 애플리케이션의 부하(load)로부터 격리하기 위한 것. 즉, 사용자 코드가 기본 디스패처를 과도하게 점유하더라도 Akka 내부의 핵심 액터들은 영향을 받지 않고 정상적으로 동작 가능.

---

#### 1.3 디스패처 선택(DispatcherSelector)

액터를 스폰(spawn)할 때 `DispatcherSelector`를 사용하여 디스패처 지정 가능. 사용 가능한 선택자는 다음과 같음.

- `DispatcherSelector.default()` — 시스템의 기본 디스패처를 사용함.
- `DispatcherSelector.blocking()` — 블로킹(blocking) 작업을 위한 디스패처를 사용함.
- `DispatcherSelector.sameAsParent()` — 부모 액터의 디스패처를 그대로 상속받아 사용함.
- `DispatcherSelector.fromConfig("name")` — 설정 파일(configuration)에서 지정한 이름의 디스패처를 로드하여 사용함.

스폰 시 다음과 같은 형태로 적용함(개념 예시).

```
context.spawn(behavior, "actor-name", DispatcherSelector.fromConfig("my-dispatcher"))
```

---

#### 1.4 디스패처 종류

Akka에는 두 가지 주요 디스패처 유형이 있음.

**Dispatcher (이벤트 기반, Event-based)**

- 액터들의 집합을 스레드 풀(thread pool)에 바인딩(binding)함.
- 여러 액터가 공유(shareability) 가능하며, 공유 가능한 액터 수에 제한 없음.
- 액터마다 하나의 메일박스(mailbox)를 생성함.
- `ExecutorService` 구현체에 의해 구동됨.

**PinnedDispatcher**

- 각 액터에 고유한(unique) 스레드 하나를 전담시킴.
- 공유 불가(no shareability).
- 액터마다 하나의 메일박스를 생성함.
- 각 액터는 자신만의 단일 스레드 풀(single-thread pool)을 가짐.

`PinnedDispatcher`는 특정 액터가 항상 같은 스레드 위에서 동작해야 하거나, 다른 액터의 영향을 전혀 받지 않아야 하는 특수한 상황에 적합함.

---

#### 1.5 Fork-Join 실행기 설정

`fork-join-executor`의 주요 설정 항목은 다음과 같음.

- `parallelism-min` / `parallelism-max` — 스레드 개수의 하한과 상한을 지정함.
- `parallelism-factor` — 가용 프로세서 수(available processors)에 곱해지는 배수(multiplier). 예를 들어 factor가 2.0이고 코어가 8개라면 16개의 스레드를 사용하려는 의도(`parallelism-min`/`parallelism-max` 범위 내에서).
- `maximum-spare-threads` — `ManagedBlocker`를 위해 추가로 사용할 수 있는 여유 스레드(spare threads) 수.
- `throughput` — 디스패처가 다른 액터로 전환하기 전에 한 액터의 메시지를 몇 개나 연속으로 처리할지를 지정함.

**중요한 주의사항**: `fork-join-executor`의 `parallelism-max`는 `ForkJoinPool`이 할당하는 전체 스레드 수의 상한(upper bound)을 설정하는 것이 아님. 이는 풀이 동시에 실행하려고 시도하는 활성 스레드 수에 관한 설정으로, 블로킹 등으로 인해 실제 생성되는 스레드 수는 이보다 많아질 수 있음.

설정 예시:

```
my-fork-join-dispatcher {
  type = Dispatcher
  executor = "fork-join-executor"
  fork-join-executor {
    parallelism-min = 2
    parallelism-factor = 2.0
    parallelism-max = 10
  }
  throughput = 100
}
```

---

#### 1.6 Thread-Pool 실행기 설정

`thread-pool-executor`는 스레드 개수를 명시적으로 제어할 수 있게 해줌. 주요 설정 항목은 다음과 같음.

- `fixed-pool-size` — 정확히 고정된 스레드 개수를 지정함.
- `core-pool-size-min` / `core-pool-size-max` / `core-pool-size-factor` — 동적으로 코어 풀 크기를 산정하기 위한 설정. `core-pool-size-factor`는 프로세서 수에 곱해지는 배수이며, 결과값은 min과 max 사이로 제한됨.
- `keep-alive-time` — 유휴(idle) 스레드가 종료되기까지 대기하는 시간.
- `allow-core-timeout` — 코어 스레드도 타임아웃(timeout)으로 종료될 수 있는지 여부를 지정함.

설정 예시:

```
my-thread-pool-dispatcher {
  type = Dispatcher
  executor = "thread-pool-executor"
  thread-pool-executor {
    fixed-pool-size = 32
  }
  throughput = 1
}
```

---

#### 1.7 블로킹(blocking) 작업 처리

##### 문제점

기본 디스패처에서 블로킹 호출(blocking call)을 수행하면 스레드 고갈(thread starvation)이 발생할 수 있음. 모든 스레드가 블로킹되어 버리면 블로킹되지 않는 액터들조차 실행될 기회를 얻지 못하게 되고, 결과적으로 시스템 전체의 성능이 저하됨.

##### 잘못된 해결책(Non-Solutions)

- 블로킹 코드를 `context.executionContext`를 사용해 `Future`로 감싸는 것은 문제를 해결하지 못함. 이는 단지 문제를 기본 디스패처로 옮기는 것에 불과함.
- `Await`(스칼라) 또는 `CompletableFuture::get()`(자바)을 사용하면 스레드 풀이 여유 스레드를 추가로 생성하게 됨. 그러나 이렇게 추가된 스레드는 블로킹이 끝난 뒤에도 한동안 그대로 할당된 상태로 남아, 불필요한 컨텍스트 스위칭(context switching)과 자원 소비를 유발함.

##### 권장 해결책: 전용 디스패처(Dedicated Dispatcher)

블로킹 작업 전용으로 별도의 디스패처를 구성하는 것 권장.

```
my-blocking-dispatcher {
  type = Dispatcher
  executor = "thread-pool-executor"
  thread-pool-executor {
    fixed-pool-size = 16
  }
  throughput = 1
}
```

이렇게 하면 블로킹 동작을 격리(isolate)하여 다른 액터에 영향을 주지 않음. 고정 풀 크기(fixed pool size)는 동시에 수행되는 블로킹 작업 수를 제한하여 자원 사용을 통제함. 이것이 리액티브 애플리케이션(reactive application)에서 모든 종류의 블로킹을 다루는 권장 방식이며, Akka HTTP, Akka Streams 등 Akka 기반의 다양한 리액티브 프레임워크 전반에 동일하게 적용됨.

##### 가상 스레드(Virtual Threads) 대안 (Java 21+)

Java 21 이상에서는 `executor = "virtual-thread-executor"`로 설정하여 가상 스레드(virtual threads) 활용 가능. 가상 스레드는 블로킹이 발생하는 동안 OS 수준 스레드(OS-level thread)로부터 분리(detach) 가능. 다만 스레드 풀과 달리 가상 스레드에는 동시 실행 개수의 본질적인 상한이 없다는 점 유의 필요. 그럼에도 블로킹 작업에 대해서는 일반 스레드 풀보다 더 효율적임.

---

#### 1.8 디스패처 별칭(aliases)과 조회(lookup)

##### 별칭(Aliases)

디스패처 설정에서 문자열(string) 값을 지정하면 이는 별칭(alias)으로 동작하여, 다른 설정 위치로 리다이렉트(redirect)됨. 이를 통해 하나의 디스패처 인스턴스를 여러 식별자(identifier)에서 공유 가능.

```
akka.actor.internal-dispatcher = akka.actor.default-dispatcher
```

위 예시는 내부 디스패처를 기본 디스패처와 동일한 것으로 매핑함.

##### 디스패처 조회(Looking up Dispatchers)

`Future`나 스케줄러(scheduler) 작업에 사용하기 위해 디스패처를 프로그래밍 방식으로 조회 가능.

```
implicit val executionContext = context.system.dispatchers
  .lookup(DispatcherSelector.fromConfig("my-dispatcher"))
```

##### 설정 베스트 프랙티스

- **CPU 바운드 작업**: `core-pool-size-factor`를 사용해 프로세서 수 기반으로 설정함.
- **블로킹 I/O**: 동시에 블로킹되는 스레드 수를 제한하기 위해 고정 크기 스레드 풀(fixed-size thread pool)을 사용함.
- **종료 동작**: `shutdown-timeout`을 적절히 설정함. 기본값 1초(default 1 second)는 풀이 너무 일찍 종료되는 원인이 될 수 있음.
- **throughput 설정**: 값이 1이면 공정성(fairness)을 제공하고, 값이 클수록 처리량(throughput)을 우선시함.

---

### 2. 메일박스(Mailboxes)

#### 2.1 메일박스란 무엇인가

Akka에서 각 액터는 메일박스(mailbox)를 가지며, 들어오는 메시지(incoming messages)는 처리되기 전에 이 메일박스에 큐(queue) 형태로 쌓임.

기본적으로는 무제한(unbounded) 메일박스가 사용되어 메시지를 무한히 담을 수 있음. 그러나 생산자(producer)가 소비자(consumer)보다 빠르게 메시지를 생성하는 경우 메모리 문제(memory issue)가 발생할 수 있음. 이때 제한(bounded) 메일박스를 사용하면 용량을 초과하는 메시지를 데드레터(deadletters)로 보내거나 블로킹할 수 있으며, 설정을 통해 메일박스 선택을 외부 설정 파일로 위임 가능.

##### 의존성 설정

메일박스는 Akka 코어의 `akka-actor` 의존성에 포함되어 함께 제공됨. `akka-actor-typed`를 사용하려면 다음을 추가함.

**sbt:**
```
"com.typesafe.akka" %% "akka-actor-typed" % "2.10.19"
```

**Maven/Gradle:** 모듈 간 버전 일관성을 위해 Akka BOM(bill of materials)을 활용하는 것 권장.

---

#### 2.2 메일박스 선택(MailboxSelector)

##### 액터별 선택(Per-Actor Selection)

액터를 스폰할 때 `MailboxSelector`를 사용함.

- `MailboxSelector.bounded(100)` — 용량이 100인 제한 메일박스를 생성함.
- `MailboxSelector.fromConfig("path.to.config")` — 설정 파일에서 지정한 이름의 메일박스 설정을 읽어옴.

##### 기본 메일박스(Default Mailbox)

`SingleConsumerOnlyUnboundedMailbox`가 기본 메일박스로 사용됨. 이는 다중 생산자/단일 소비자(MPSC, multiple-producer single-consumer) 큐를 사용하며, 일반적인 액터 워크로드(workload)에 최적화됨.

설정 예시:

```
my-app {
  my-special-mailbox {
    mailbox-type = "akka.dispatch.SingleConsumerOnlyUnboundedMailbox"
  }
}
```

해당 메일박스 타입은 명명된 설정 섹션(named config section)을 전달받으며, 기본 메일박스 설정을 폴백(fallback)으로 사용함.

---

#### 2.3 기본 제공 메일박스 구현

**무제한(Unbounded) 옵션:**

- **SingleConsumerOnlyUnboundedMailbox** (기본값): MPSC 큐 기반, 논블로킹(non-blocking). `BalancingDispatcher`와는 호환되지 않음.
- **UnboundedMailbox**: `ConcurrentLinkedQueue` 기반, 논블로킹.
- **UnboundedControlAwareMailbox**: 이중 큐(dual queue) 구조를 사용하여 `ControlMessage` 인스턴스를 우선 처리함.
- **UnboundedPriorityMailbox**: `PriorityBlockingQueue` 기반. 우선순위가 동일한 메시지들 사이의 FIFO 순서는 정의되지 않음(undefined).
- **UnboundedStablePriorityMailbox**: `PriorityBlockingQueue`를 안정화기(stabilizer)로 감싸, 우선순위가 동일한 메시지에 대해 FIFO 순서를 보존함.

**제한 + 논블로킹(Bounded Non-Blocking):**

- **NonBlockingBoundedMailbox**: 효율적인 MPSC 큐 기반. 용량을 초과한 메시지는 데드레터(deadletters)로 폐기(discard)됨.

**블로킹(Blocking) — `mailbox-push-timeout-time`을 0보다 크게 설정하여 사용:**

- **BoundedMailbox**: `LinkedBlockingQueue` 기반.
- **BoundedPriorityMailbox**: 우선순위 큐 기반. 동일 우선순위 간 순서는 정의되지 않음.
- **BoundedStablePriorityMailbox**: 동일 우선순위에 대해 안정적인 FIFO 순서를 보장함.
- **BoundedControlAwareMailbox**: 제어 메시지(control message)를 우선 처리하며, 용량 도달 시 블로킹함.

##### 메일박스 선택 시 고려사항

- 제한 메일박스(bounded mailbox)는 무제한 메모리 증가를 방지하지만 메시지를 잃을 수 있음.
- 우선순위 메일박스(priority mailbox)는 추가 오버헤드(overhead)가 있으므로 메시지 순서가 중요한 경우에만 사용.
- 제어 인지 메일박스(control-aware mailbox)는 관리용(administrative) 메시지에 빠르게 반응해야 하는 시스템에 적합함.

---

#### 2.4 커스텀 메일박스 구현

커스텀 메일박스는 두 개(혹은 세 개)의 구성 요소를 구현하여 만듦.

**1. MessageQueue 구현:**

`MessageQueue` 인터페이스를 구현함. 다음과 같은 메서드를 포함함.

- `enqueue()` — 메시지를 큐에 넣음
- `dequeue()` — 큐에서 메시지를 꺼냄
- `numberOfMessages()` — 큐에 들어 있는 메시지 개수 반환
- `hasMessages()` — 큐에 메시지가 있는지 여부 반환
- `cleanUp()` — 정리 작업 수행

내부적으로는 `ConcurrentLinkedQueue` 같은 동시성(concurrent) 자료구조를 사용함.

**2. MailboxType 클래스:**

`MailboxType`을 확장(extend)하고 `ProducesMessageQueue[YourMessageQueueType]`을 구현함. `ActorSystem.Settings`와 `Config` 파라미터를 받는 생성자(constructor)를 반드시 포함해야 함 — Akka가 리플렉션(reflection)을 통해 이 생성자를 호출하기 때문. 또한 새로운 `MessageQueue` 인스턴스를 반환하는 `create()` 메서드를 구현함.

**3. 마커 트레이트/인터페이스(Marker Trait/Interface, 선택 사항):**

메일박스 요구사항 매핑(requirement mapping)을 위한 마커 시맨틱 트레이트(marker semantic trait)를 정의 가능. 이를 통해 디스패처가 메일박스 호환성을 검증 가능.

**설정:**

커스텀 메일박스의 완전한 클래스 이름(fully-qualified class name)을 디스패처 또는 메일박스 설정의 `mailbox-type` 필드에 지정함.

##### 커스텀 구현 시 주의사항

커스텀 구현은 정리(cleanup) 작업을 적절히 처리하여, 남아 있는 메시지를 데드레터(deadletters)로 옮겨야 함.

---

### 3. 테스트(Testing)

#### 3.1 개요와 모듈 설정

Akka Typed 액터의 테스트는 두 가지 방식으로 수행 가능.

> "테스트는 실제 `ActorSystem`을 사용하는 비동기(asynchronously) 방식으로 하거나, `BehaviorTestKit`을 사용하여 테스트 스레드(testing thread) 위에서 동기적으로(synchronously) 수행 가능."

**동기 테스트(Synchronous Testing)**: `Behavior` 로직을 격리하여 테스트하는 데 적합함. 다만 사용 가능한 기능은 제한적임.

**비동기 테스트(Asynchronous Testing)**: 여러 액터 간의 상호작용을 보다 현실적인 환경에서 테스트하는 데 권장됨.

##### 모듈 설정

- **sbt**: `akka-actor-testkit-typed` (버전 2.10.19) 사용

```
"com.typesafe.akka" %% "akka-actor-testkit-typed" % "2.10.19" % Test
```

- **Maven / Gradle**: 스칼라 바이너리 버전을 명시한 Akka BOM 기반 설정 사용
- **테스트 프레임워크**: ScalaTest 3.2.17 사용 권장

---

#### 3.2 비동기 테스트(Asynchronous testing)

비동기 테스트는 실제 `ActorSystem`을 사용하며, `ActorTestKit`을 중심으로 진행됨. 다루는 주제는 다음과 같음.

- **기본 예제(Basic examples)**: 실제 액터 시스템 위에서 액터를 스폰하고 메시지를 보내며 응답을 검증함. 응답 검증에는 주로 `TestProbe`를 사용함.
- **목 처리된 동작 관찰(Mocked behavior observation)**: 테스트 대상 액터가 다른 액터와 어떻게 상호작용하는지를 관찰함.
- **테스트 프레임워크 통합(Framework integration)**: ScalaTest 등과의 통합 방법을 다룸.
- **설정(Configuration)**: 테스트용 `ActorSystem`에 커스텀 설정을 적용하는 방법.
- **스케줄러 제어(Scheduler control)**: 시간 기반 동작을 테스트하기 위해 스케줄러를 제어함.
- **로깅 테스트(Logging tests)**: 액터가 기대한 로그 메시지를 출력하는지 검증함.

핵심 도구:

- `ActorTestKit` — 테스트용 실제 액터 시스템과 헬퍼(helper)를 제공함.
- `TestProbe` — 메시지를 수신하고, 기대한 메시지가 도착하는지(`expectMessage` 등) 검증하는 데 사용하는 프로브(probe). 또한 다른 액터에게 전달할 `ActorRef`를 제공하여, 응답이 프로브로 들어오게 만들 수 있음.

---

#### 3.3 동기 테스트(Synchronous behavior testing)

동기 테스트는 `BehaviorTestKit`을 사용하여 테스트 스레드 위에서 직접 동작을 실행함. 실제 액터 시스템 없이 `Behavior` 로직을 단위(unit) 수준으로 검증함. 다루는 주제는 다음과 같음.

- **자식 스폰(Child spawning)**: 테스트 대상 동작이 자식 액터를 스폰하는지 검증함.
- **메시지 전송(Message sending)**: 동작이 다른 액터에게 메시지를 보내는지 검증함.
- **효과 테스트(Effect testing)**: 스폰, 메시지 전송, 워치(watch) 등 액터가 일으키는 부수 효과(effect)를 검증함. `BehaviorTestKit`은 이러한 효과를 기록하여 확인할 수 있게 해줌.
- **로그 메시지 검증(Log message verification)**: 동작이 출력한 로그 메시지를 검증함.

동기 테스트는 빠르고 결정적(deterministic)이지만, 실제 동시성(concurrency)이나 타이밍 관련 동작은 재현할 수 없으므로 단일 `Behavior`의 로직 검증에 한정하여 사용하는 것이 좋음.

##### 프로젝트 정보(참고)

- **라이선스**: BUSL-1.1
- **상태**: Lightbend가 지원(Supported)
- **JDK 지원**: 11, 17, 21
- **Scala 버전**: 2.13.17, 3.3.7

---

### 4. 스타일 가이드(Style Guide)

#### 4.1 함수형 스타일 vs 객체지향 스타일

Akka Typed에서 액터 동작(behavior)을 구현하는 데에는 두 가지 주요 스타일이 있음.

**함수형 스타일(Functional Style)**

- 불변(immutable) 상태를 동작들 사이에서 파라미터로 전달함.
- 상태 변경은 새로운 동작 인스턴스를 반환(return)하는 방식으로 표현함.
- 예: 메시지를 처리한 뒤 갱신된 상태를 인자로 하여 다음 동작을 반환함.

**객체지향 스타일(Object-Oriented Style)**

- `AbstractBehavior`를 확장하는 클래스 내부의 가변(mutable) 필드를 사용함.
- 상태를 인스턴스 변수(instance variable)로 유지함.

두 방식 모두 프로젝트의 필요와 개발자의 친숙도에 따라 각자의 장점 존재. 한 가지를 선택해 일관성 있게 사용하는 것이 좋음.

---

#### 4.2 너무 많은 파라미터 전달 문제

함수형 스타일의 액터가 많은 파라미터를 필요로 하게 되면 코드가 번잡해질 수 있음. 이때 권장되는 방법은 다음과 같음.

> "생성자(constructor) 성격의 파라미터들을 불변 필드를 가진 별도의 클래스로 캡슐화(encapsulate)하고, 실제로 변하는 상태(changing state)만 별도의 파라미터로 유지."

즉, 변하지 않는 의존성과 설정 값들은 하나의 클래스로 묶어두고, 메시지 처리마다 실제로 변경되는 값만 동작 간에 전달함으로써, 파라미터 전달의 부담을 줄이면서도 중요한 부분에서는 함수형의 순수성(functional purity)을 유지 가능.

---

#### 4.3 메시지를 어디에 정의할 것인가

메시지는 해당 메시지를 사용하는 동작(behavior)과 함께 정의하는 것이 좋음. 일반적으로 다음 위치에 둠.

- **Scala**: 컴패니언 객체(companion object) 내부
- **Java**: 정적 내부 클래스(static inner class)로

이렇게 함께 두면(co-location) 발견 가능성(discoverability)이 좋아지고, 액터 이름을 접두사로 붙이는 명명을 자연스럽게 유도함. 예를 들어 그냥 `Increment`가 아니라 `Counter.Increment`처럼 사용하게 됨.

여러 액터가 공유하는 프로토콜(protocol)의 경우, 전용 프로토콜 클래스나 인터페이스에 메시지를 정의하는 것을 고려 가능.

---

#### 4.4 공개 vs 비공개 메시지

내부 전용(internal-only) 메시지는 비공개(private)로 표시하여, 외부 코드가 직접 그 메시지를 보내지 못하도록 막는 것이 좋음. 예를 들어 타이머나 어댑터를 통해서만 들어와야 하는 내부 메시지를 외부에서 직접 보내는 것을 방지함.

또한 커맨드(command)에 대해서는 봉인된 트레이트 계층(sealed trait hierarchy, Scala)을 사용하는 것 권장. 이렇게 하면 패턴 매칭(pattern matching) 시 컴파일러가 모든 경우를 다루었는지(exhaustiveness) 검사해 주므로, 메시지를 추가했을 때 처리 누락을 컴파일 시점에 발견 가능.

---

#### 4.5 람다 vs 메서드 참조, ask 패턴

코드 품질을 위한 관례는 다음과 같음.

- 짧은 람다(lambda) 표현식에서는 로직을 직접 작성하기보다 이름 있는 메서드(named method)로 위임(delegate)하여 가독성을 높임.
- 람다보다 메서드 참조(method reference, 예: `this::handler`)를 우선적으로 사용함.
- 일관성을 위해 `?` 연산자(operator)보다 `ask` 메서드를 사용함.
- `setup`, `withTimers`, `withStash` 블록은 필요에 따라 중첩(nesting)하여 사용하며, 일반적으로 `setup`을 가장 바깥쪽(outermost)에 둠.

---

#### 4.6 명명 규칙(Naming conventions)

- 응답(response)을 받을 `ActorRef` 파라미터에는 `replyTo`라는 이름을 사용함.
- 액터로 들어오는 메시지(incoming message)는 "커맨드(command)"라고 부르며, 상위 타입(super-type) 이름으로 `Command`를 사용함.
- 영속화(persist)되는 이벤트(event)에는 과거 시제(past tense)를 사용함. 예: `Incremented`(증가됨), `Decremented`(감소됨).

이러한 명명 규칙은 커맨드(명령, 현재형)와 이벤트(이미 일어난 사실, 과거형)를 명확히 구분하여, 특히 이벤트 소싱(event sourcing) 기반 액터에서 코드의 의도를 분명히 드러냄.

---

### 5. 코디네이티드 셧다운(Coordinated Shutdown)

#### 5.1 개요

`CoordinatedShutdown` 확장(extension)은 `ActorSystem`의 정돈된 종료(orderly termination)를 관리함. 이는 설정에 정의된 단계(phase)들로 그룹화된 태스크(task)들을 등록하고, 종료 시 이 태스크들을 정해진 순서대로 실행함.

주요 특징:

- 단계들은 방향성 비순환 그래프(DAG, directed acyclic graph)를 형성하며, 위상 정렬(topological sorting)을 통해 실행 순서가 결정됨.
- 같은 단계 내의 태스크들은 병렬(parallel)로 실행되며, 다음 단계는 이전 단계가 완료될 때까지 대기함.
- 루트 액터(root actor)가 종료되거나 클러스터 노드(cluster node)가 떠날 때 자동으로 트리거(trigger)됨.

---

#### 5.2 셧다운 단계(phases)

문서가 정의하는 주요 단계는 순서대로 다음과 같음.

1. **before-service-unbind** — 애플리케이션 태스크를 위한 최초 단계.
2. **service-unbind** — 새로운 연결(connection) 수락을 중단함.
3. **service-requests-done** — 진행 중인 요청(in-progress requests)이 완료되기를 기다림.
4. **service-stop** — 남아 있는 연결을 강제로 종료함.
5. **before-cluster-shutdown** — 클러스터 종료 전에 수행할 애플리케이션 커스텀 태스크.
6. **cluster-sharding-shutdown-region** — Cluster Sharding을 우아하게(graceful) 종료함.
7. **cluster-leave** — leave 커맨드를 발행(emit)함.
8. **cluster-exiting** — 클러스터 싱글톤(cluster singleton)을 종료함.
9. **cluster-exiting-done** — exiting 완료를 기다림.
10. **cluster-shutdown** — 클러스터 확장(cluster extension)을 종료함.
11. **before-actor-system-terminate** — `ActorSystem` 종료 전에 수행할 커스텀 태스크.
12. **actor-system-terminate** — 마지막 단계로, 실제로 `ActorSystem`을 종료함.

클러스터 관련 단계(5~10)는 Akka Cluster를 사용할 때만 의미가 있으며, 단일 노드 애플리케이션에서는 사실상 비어 있는 채로 빠르게 지나감.

---

#### 5.3 태스크 등록

태스크는 `CoordinatedShutdown`의 다음 메서드를 통해 추가함.

- **addTask()** — `Future[Done]`(스칼라) 또는 `CompletionStage<Done>`(자바)을 반환하는 표준 셧다운 태스크를 등록함. 특정 단계(phase)에 소속시켜 등록함.
- **addCancellableTask()** — 취소 가능한(cancellable) 태스크를 등록함. 반환된 핸들을 통해 나중에 등록을 취소 가능.
- **addJvmShutdownHook()** — JVM 셧다운 훅을 `CoordinatedShutdown`을 통해 등록함.

태스크는 자신이 속한 단계 안에서 다른 태스크들과 병렬로 실행되며, 후속 단계는 이전 단계의 모든 태스크가 완료되기를 기다림.

개념 예시(태스크 등록):

```
CoordinatedShutdown.get(system).addTask(
  CoordinatedShutdown.PhaseBeforeServiceUnbind(),
  "my-task-name") { () =>
    // 종료 시 수행할 작업
    Future.successful(Done)
}
```

---

#### 5.4 셧다운 실행

셧다운은 다음 방법으로 시작 가능.

- `system.terminate()` — `ActorSystemTerminateReason`을 종료 사유(reason)로 사용하여 종료함.
- `CoordinatedShutdown(system).run(reason)` — 커스텀 종료 사유(custom shutdown reason)를 지정하여 실행함.

`run` 메서드는 멱등성(idempotent)을 가지므로 여러 번 호출해도 안전함. 즉, 이미 셧다운이 진행 중이라면 추가 호출은 진행 중인 셧다운의 완료를 가리키는 동일한 결과를 반환함.

자동 트리거:

- 루트 액터가 종료될 때 자동으로 실행됨.
- 클러스터 노드가 클러스터를 떠날 때(departure) 자동으로 실행됨.

---

#### 5.5 단계 설정과 JVM 종료

각 단계는 설정으로 다음 항목을 조정 가능.

- **timeout** — 해당 단계의 타임아웃(기본값은 단계마다 다름). 타임아웃이 지나도 완료되지 않은 태스크가 있어도, 기본적으로는 다음 단계 진행을 막지 않음.
- **recover = off** — 해당 단계 실패 시 셧다운을 중단(abort)하도록 설정함(기본은 복구하여 다음 단계로 진행).
- **enabled = off** — 해당 단계의 태스크들을 건너뛰도록(skip) 설정함.
- **depends-on** — 단계 의존성을 DAG 형태로 정의함.

전역 설정:

- **akka.coordinated-shutdown.exit-jvm** — `on`으로 설정하면 셧다운 완료 후 `System.exit()`를 호출하여 JVM을 강제 종료함.
- **akka.coordinated-shutdown.run-by-jvm-shutdown-hook** — `off`로 설정하면 SIGTERM 등 JVM 셧다운 시 코디네이티드 셧다운이 자동으로 실행되지 않음.

중요한 동작:

- 단계들은 DAG를 이루며 위상 정렬을 통해 순서가 정해짐.
- 타임아웃 후 완료되지 않은 태스크는 (`recover = off`가 아닌 한) 다음 단계를 막지 않음.
- **Kubernetes 고려사항**: 기본적으로 SIGKILL 전 30초의 그레이스 기간(grace period)이 주어짐. 셧다운 단계들의 전체 소요 시간이 이 기간 내에 끝나도록 설정하는 것이 중요함.

---

### 6. Typed와 Classic의 공존(Coexisting)

#### 6.1 개요

기존 시스템에서 Akka Typed로의 전환은 점진적으로 이루어지기 때문에, Typed 액터와 Classic 액터는 같은 `ActorSystem` 안에서 공존(coexist)할 수 있도록 설계됨.

두 개의 별도 `ActorSystem` 구현이 존재함.

- `akka.actor.ActorSystem` — Classic(클래식)
- `akka.actor.typed.ActorSystem` — Typed(타입)

Typed와 Classic 액터는 다음과 같은 방식으로 상호작용 가능.

- Classic 액터 시스템이 Typed 액터를 생성할 수 있음.
- 메시지가 두 종류 사이에서 양방향(bidirectional)으로 흐를 수 있음.
- Classic 부모에서 Typed 자식을 스폰하고 슈퍼바이즈(supervise)할 수 있으며, 그 반대도 가능함.
- 서로를 워치(watch)할 수 있음.
- Classic 시스템을 Typed로 변환할 수 있음.

---

#### 6.2 ActorSystem 변환

**Classic에서 Typed로:**

Classic `ActorSystem`을 Typed로 변환하려면 어댑터(adapter) 패턴을 사용함.

- **Scala**: `import akka.actor.typed.scaladsl.adapter._`를 임포트하면 `toTyped` 같은 확장 메서드(extension method) 사용 가능.
- **Java**: `akka.actor.typed.javadsl.Adapter` 클래스의 정적 메서드(static method)를 사용함.

**Typed에서 Classic으로:**

마찬가지로 Typed 시스템에서도 동일한 어댑터 임포트를 통해 Classic 기능에 접근 가능. `toClassic` 변환을 사용하고, `Adapter.spawn()` 또는 `Adapter.actorOf()`를 통해 Classic 액터를 스폰 가능.

---

#### 6.3 액터의 생성과 관리

**Classic이 Typed를 생성:**

Classic 액터는 (어댑터를 임포트한 후) 컨텍스트 확장 메서드(context extension method)를 사용해 Typed 자식 액터를 스폰 가능. 이 Typed 자식을 워치하고 메시지를 보낼 수 있으며, reply-to 패턴(응답 받을 `ActorRef`를 메시지에 담아 보내는 방식)을 통해 응답을 받음.

**Typed가 Classic을 생성:**

Typed 액터는 `Adapter.actorOf()`를 사용해 Classic 자식 액터를 생성함. 생명주기 모니터링을 위해 `Adapter.watch()`를 사용하며, 메시지 파라미터를 통해 발신자(sender)를 명시적으로 전달해야 함(Classic의 암묵적 `sender()`가 Typed에는 없기 때문).

---

#### 6.4 슈퍼비전(supervision) 차이

문서에서 강조하는 중요한 차이는 다음과 같음.

> "Classic 액터의 기본 슈퍼비전(default supervision)은 재시작(restart), Typed의 기본 슈퍼비전은 정지(stop)."

즉, 두 모델의 기본 장애 처리 전략이 다름. Classic은 예외가 발생하면 액터를 재시작하는 것이 기본이고, Typed는 예외가 발생하면 액터를 정지시키는 것이 기본. 액터를 혼합하여 사용할 때, 자식 슈퍼비전(child supervision)의 기본값은 부모의 타입을 따름. 따라서 Classic 부모 아래의 자식은 Classic의 재시작 정책을, Typed 부모 아래의 자식은 Typed의 정지 정책을 기본으로 적용받게 됨. 이 차이를 인지하지 못하면 마이그레이션 과정에서 장애 처리 동작이 예기치 않게 바뀔 수 있으므로 주의 필요.

---

### 참고 자료

- [Akka 공식 문서](https://doc.akka.io/libraries/akka-core/current/)
- [Dispatchers](https://doc.akka.io/libraries/akka-core/current/typed/dispatchers.html)
- [Mailboxes](https://doc.akka.io/libraries/akka-core/current/typed/mailboxes.html)
- [Testing](https://doc.akka.io/libraries/akka-core/current/typed/testing.html)
- [Style Guide](https://doc.akka.io/libraries/akka-core/current/typed/style-guide.html)
- [Coordinated Shutdown](https://doc.akka.io/libraries/akka-core/current/coordinated-shutdown.html)
- [Coexisting](https://doc.akka.io/libraries/akka-core/current/typed/coexisting.html)
