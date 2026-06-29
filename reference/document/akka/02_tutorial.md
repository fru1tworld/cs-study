# Akka 튜토리얼: IoT 시스템 만들기

> 원본: https://doc.akka.io/libraries/akka-core/current/typed/guide/tutorial.html

---

## 목차

1. [예제 소개 (Introduction to the Example)](#1-예제-소개-introduction-to-the-example)
2. [Part 1: 액터 아키텍처 (Actor Architecture)](#2-part-1-액터-아키텍처-actor-architecture)
   - [Akka 액터 계층 구조 (The Akka Actor Hierarchy)](#akka-액터-계층-구조-the-akka-actor-hierarchy)
   - [첫 번째 액터 (The First Actor)](#첫-번째-액터-the-first-actor)
   - [액터 생명주기 (The Actor Lifecycle)](#액터-생명주기-the-actor-lifecycle)
   - [장애 처리 (Failure Handling)](#장애-처리-failure-handling)
3. [Part 2: 첫 번째 액터 만들기 (Creating the First Actor)](#3-part-2-첫-번째-액터-만들기-creating-the-first-actor)
4. [Part 3: 디바이스 액터 다루기 (Working with Device Actors)](#4-part-3-디바이스-액터-다루기-working-with-device-actors)
   - [디바이스에 대한 메시지 식별 (Identifying Messages for Devices)](#디바이스에-대한-메시지-식별-identifying-messages-for-devices)
   - [메시지 전달 (Message Delivery)](#메시지-전달-message-delivery)
   - [메시지 순서 (Message Ordering)](#메시지-순서-message-ordering)
   - [디바이스 메시지에 유연성 추가하기](#디바이스-메시지에-유연성-추가하기)
   - [디바이스 액터와 읽기 프로토콜 구현](#디바이스-액터와-읽기-프로토콜-구현)
   - [액터 테스트](#액터-테스트)
   - [쓰기 프로토콜 추가](#쓰기-프로토콜-추가)
   - [읽기/쓰기 메시지를 가진 액터](#읽기쓰기-메시지를-가진-액터)
5. [Part 4: 디바이스 그룹 다루기 (Working with Device Groups)](#5-part-4-디바이스-그룹-다루기-working-with-device-groups)
   - [디바이스 매니저 계층 구조](#디바이스-매니저-계층-구조)
   - [등록 프로토콜](#등록-프로토콜)
   - [디바이스 그룹 액터에 등록 지원 추가하기](#디바이스-그룹-액터에-등록-지원-추가하기)
   - [그룹 내 디바이스 액터 추적하기](#그룹-내-디바이스-액터-추적하기)
   - [디바이스 매니저 액터 만들기](#디바이스-매니저-액터-만들기)
   - [테스트 케이스](#테스트-케이스)
6. [Part 5: 디바이스 그룹 조회하기 (Querying Device Groups)](#6-part-5-디바이스-그룹-조회하기-querying-device-groups)
   - [동적 액터 멤버십 처리](#동적-액터-멤버십-처리)
   - [메시지 프로토콜](#메시지-프로토콜)
   - [DeviceGroupQuery 액터 구현](#devicegroupquery-액터-구현)
   - [쿼리를 DeviceGroup에 통합하기](#쿼리를-devicegroup에-통합하기)
   - [테스트](#테스트)
7. [참고 자료](#참고-자료)

---

## 1. 예제 소개 (Introduction to the Example)

이 튜토리얼은 액터 기반 시스템 설계를 이해하는 데 도움을 주기 위해 만들어진 Akka IoT 튜토리얼을 소개합니다. 이 가이드는 온도 센서 관리 시스템을 실용적인 예제로 사용합니다.

### 주요 학습 목표

독자는 다음을 학습하게 됩니다.

- 액터 계층 구조(actor hierarchy)와 그것이 액터 동작(behavior)에 어떻게 영향을 미치는지
- 적절한 액터 단위(actor granularity, 액터의 입도/세분화 정도) 선택하기
- 메시징을 통한 프로토콜 정의
- 전형적인 대화형 상호작용 패턴(conversational interaction patterns)

### IoT 사용 사례 (Use Case)

이 튜토리얼은 두 가지 주요 구성 요소로 이루어진 사물인터넷(Internet of Things, IoT) 애플리케이션을 구축하는 데 초점을 맞춥니다.

1. **디바이스 데이터 수집(Device data collection)** — 원격 디바이스의 표현(representation)을 관리하며, 여러 센서를 디바이스 그룹(device group)으로 조직화합니다.
2. **사용자 대시보드(User dashboard)** — 디바이스로부터 주기적으로 데이터를 수집하고 리포트를 생성합니다.

이 예제는 고객이 여러 구역의 측정값을 확인할 수 있도록, 가정용 센서의 온도 측정값(temperature readings)에 초점을 맞춥니다.

### 사전 준비와 구조

독자는 먼저 Hello World 예제를 완료해야 합니다. 이 튜토리얼은 액터 아키텍처의 기초로 시작하여, 디바이스 액터(device actor) 생성, 그룹 관리(group management), 조회(querying) 시스템으로 발전해 나가는 다섯 개의 파트(part)로 진행됩니다.

### 개발 지원

Java DSL과 Scala DSL이 함께 번들로 제공됩니다. 임포트(import) 제안 충돌을 방지하기 위해 Eclipse 및 IntelliJ에 대한 IDE 설정을 권장합니다. 본문 예제는 **Scala 코드**로 제시합니다.

---

## 2. Part 1: 액터 아키텍처 (Actor Architecture)

Akka를 사용하면 액터 시스템 인프라를 구축하고 기본적인 동작 관리를 위한 저수준(low-level) 코드를 작성하는 부담에서 벗어날 수 있습니다. 이 장점을 충분히 이해하려면, 개발자가 작성하는 액터들과 Akka가 내부적으로 생성·관리하는 액터들 사이의 관계, 그리고 액터 생명주기(actor lifecycle)와 장애(failure)가 어떻게 처리되는지를 이해해야 합니다.

### Akka 액터 계층 구조 (The Akka Actor Hierarchy)

Akka에서 모든 액터는 반드시 부모(parent)를 가져야 합니다. 여러분은 `ActorContext.spawn()`을 호출하여 액터를 생성합니다. 이 동작을 통해 생성자(creator) 액터는 새로 생성된 자식(child) 액터의 부모가 됩니다. 그러면 자연스럽게 다음 질문이 떠오릅니다. "그렇다면 여러분이 생성하는 최초의 액터의 부모는 누구인가?"

계층 구조 다이어그램에 묘사되어 있듯이, 여러분의 모든 액터는 공통의 부모인 **사용자 가디언(user guardian)**을 공유합니다. 이 사용자 가디언은 `ActorSystem`을 시작할 때 생성되고 초기화됩니다. 첫 번째 Hello World 예제에서 다루었듯이, 액터를 생성하면 유효한 URL로서 기능하는 참조(reference)가 생성됩니다. 따라서 사용자 가디언으로부터 `context.spawn(someBehavior, "someActor")`를 사용하여 `someActor`라는 이름의 액터를 생성하면, 그 참조는 `/user/someActor` 경로를 따르게 됩니다.

여러분의 첫 번째 액터인 사용자 가디언이 동작을 시작하기 전에, Akka는 이미 시스템에 두 개의 추가적인 가디언 액터, 즉 `/`와 `/system`을 설정해 둔 상태입니다. 따라서 계층 구조의 정점(apex)에는 세 개의 가디언 액터가 존재합니다.

- **루트 가디언(root guardian)** — `/`에 위치하며, 시스템 내 모든 액터의 부모로서 기능하고, 시스템 자체가 종료될 때 가장 마지막에 종료되는 액터를 나타냅니다.
- **시스템 가디언(system guardian)** — `/system`에 위치하며, Akka 또는 Akka 위에 구축된 라이브러리가 시스템 네임스페이스 내에 자체 액터를 생성할 수 있는 곳입니다.
- **사용자 가디언(user guardian)** — `/user`에 위치하며, 애플리케이션의 다른 모든 액터를 시작하기 위해 여러분이 제공하는 최상위(top-level) 액터를 나타냅니다.

동작 중인 액터 계층 구조를 이해하는 가장 간단한 방법은 `ActorRef` 인스턴스를 살펴보는 것입니다. 간단한 실험으로, 액터를 하나 생성하여 그 참조를 출력하고, 그 액터의 자식을 생성하여 자식의 참조를 출력해 봅니다.

```scala
package com.example

import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.AbstractBehavior
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.Behaviors

object PrintMyActorRefActor {
  def apply(): Behavior[String] =
    Behaviors.setup(context => new PrintMyActorRefActor(context))
}

class PrintMyActorRefActor(context: ActorContext[String]) extends AbstractBehavior[String](context) {

  override def onMessage(msg: String): Behavior[String] =
    msg match {
      case "printit" =>
        val secondRef = context.spawn(Behaviors.empty[String], "second-actor")
        println(s"Second: $secondRef")
        this
    }
}

object Main {
  def apply(): Behavior[String] =
    Behaviors.setup(context => new Main(context))

}

class Main(context: ActorContext[String]) extends AbstractBehavior[String](context) {
  override def onMessage(msg: String): Behavior[String] =
    msg match {
      case "start" =>
        val firstRef = context.spawn(PrintMyActorRefActor(), "first-actor")
        println(s"First: $firstRef")
        firstRef ! "printit"
        this
    }
}

object ActorHierarchyExperiments extends App {
  val testSystem = ActorSystem(Main(), "testSystem")
  testSystem ! "start"
}
```

콘솔 출력은 다음과 같습니다.

```
First: Actor[akka://testSystem/user/first-actor#1053618476]
Second: Actor[akka://testSystem/user/first-actor/second-actor#-1544706041]
```

출력을 살펴보면 참조의 구조를 알 수 있습니다. 두 경로 모두 `akka://testSystem/`로 시작합니다. 액터 참조는 유효한 URL로서 기능하기 때문에 `akka://`가 프로토콜(protocol) 필드 값으로 사용됩니다. 이후, 웹 주소와 마찬가지로 URL은 시스템을 식별합니다. 이 예에서 시스템의 이름은 `testSystem`이지만 다른 어떤 것이든 될 수 있습니다. 여러 시스템 간의 원격 통신(remote communication)이 활성화되면, URL의 이 부분에 호스트명(hostname)이 포함되어 다른 시스템이 네트워크에서 그것을 찾을 수 있게 합니다.

두 번째 액터의 참조가 `/first-actor/` 경로를 포함하고 있으므로, 이는 두 번째 액터가 첫 번째 액터의 자식임을 나타냅니다. 액터 참조의 마지막 구성 요소인 `#1053618476` 또는 `#-1544706041`과 같은 것은 고유 식별자(unique identifier)를 나타내며, 일반적으로 무시해도 됩니다.

이제 액터 계층 구조의 형태를 이해했으니, 자연스럽게 "왜 이런 계층 구조가 필요한가? 어떤 목적을 위한 것인가?"라는 질문이 떠오릅니다.

계층 구조는 액터 생명주기를 안전하게 감독(oversee)하는 중요한 역할을 합니다. 다음에는 이 측면을 살펴보고, 이 이해가 코드 품질을 어떻게 향상시키는지 알아보겠습니다.

### 액터 생명주기 (The Actor Lifecycle)

액터는 생성될 때 존재하게 되며, 이후 사용자의 요청에 따라 정지(halt)될 수 있습니다. 액터가 정지되면, 그 모든 자식들도 재귀적으로(recursively) 함께 정지됩니다. 이 특성은 리소스 정리(resource cleanup)를 크게 간소화하고, 닫히지 않은 소켓(socket)이나 파일로 인한 리소스 누수(resource leak)를 방지합니다. 저수준 멀티스레드 프로그래밍에서는 다양한 동시성 리소스의 생명주기 관리가 종종 간과되는 어려운 과제입니다.

액터를 정지시키기 위해 권장되는 방식은 액터 내부에서 `Behaviors.stopped`를 반환하는 것이며, 이는 일반적으로 사용자 정의 정지 메시지에 대한 응답으로 또는 액터의 할당된 작업이 완료되었을 때 이루어집니다. 부모로부터 `context.stop(childRef)`를 호출하여 자식 액터를 정지시키는 것도 기술적으로는 가능하지만, 이 메커니즘으로 임의의(자식이 아닌) 액터를 종료시키는 것은 불가능합니다.

Akka 액터 API는 `PostStop`을 포함한 특정 생명주기 시그널(lifecycle signals)을 제공하는데, `PostStop`은 액터가 종료된 직후에 디스패치(dispatch)됩니다. 이 시그널 이후로는 더 이상 메시지가 처리되지 않습니다.

다음은 `PostStop` 시그널을 통한 생명주기를 보여주는 예제입니다.

```scala
object StartStopActor1 {
  def apply(): Behavior[String] =
    Behaviors.setup(context => new StartStopActor1(context))
}

class StartStopActor1(context: ActorContext[String]) extends AbstractBehavior[String](context) {
  println("first started")
  context.spawn(StartStopActor2(), "second")

  override def onMessage(msg: String): Behavior[String] =
    msg match {
      case "stop" => Behaviors.stopped
    }

  override def onSignal: PartialFunction[Signal, Behavior[String]] = {
    case PostStop =>
      println("first stopped")
      this
  }

}

object StartStopActor2 {
  def apply(): Behavior[String] =
    Behaviors.setup(new StartStopActor2(_))
}

class StartStopActor2(context: ActorContext[String]) extends AbstractBehavior[String](context) {
  println("second started")

  override def onMessage(msg: String): Behavior[String] = {
    // no messages handled by this actor
    Behaviors.unhandled
  }

  override def onSignal: PartialFunction[Signal, Behavior[String]] = {
    case PostStop =>
      println("second stopped")
      this
  }

}
```

사용 예시는 다음과 같습니다.

```scala
val first = context.spawn(StartStopActor1(), "first")
first ! "stop"
```

콘솔 출력은 다음과 같습니다.

```
first started
second started
second stopped
first stopped
```

첫 번째 액터를 정지시켰을 때, 그것은 자기 자신을 종료하기 전에 자식 액터를 정지시켰습니다. 이 순서는 엄격합니다. 즉, 자식들로부터의 모든 `PostStop` 시그널은 부모의 `PostStop` 시그널이 처리되기 전에 처리됩니다.

### 장애 처리 (Failure Handling)

부모와 자식은 그들의 생애 동안 연결을 유지합니다. 액터가 장애를 겪을 때마다(예외를 던지거나 메시지 핸들러에서 처리되지 않은 예외가 발생하는 경우), 장애 정보는 **감독 전략(supervision strategy)**으로 전달되며, 감독 전략이 그 결과로 발생한 예외를 어떻게 관리할지 결정합니다. 감독 전략은 보통 부모 액터가 자식을 생성할 때 설정됩니다. 이러한 방식으로 부모는 자식의 감독자(supervisor)로서 기능합니다. **기본 감독 전략(default supervisor strategy)은 자식을 종료시킵니다.** 전략이 정의되지 않으면 모든 장애는 종료로 귀결됩니다.

장애 발생 후, 감독되는 액터는 재시작 작업이 시작되기 전에 `PreRestart` 시그널을 받습니다. 이후 액터가 재시작됩니다.

다음은 재시작(restart) 감독 전략을 사용하는 예제입니다.

```scala
object SupervisingActor {
  def apply(): Behavior[String] =
    Behaviors.setup(context => new SupervisingActor(context))
}

class SupervisingActor(context: ActorContext[String]) extends AbstractBehavior[String](context) {
  private val child = context.spawn(
    Behaviors.supervise(SupervisedActor()).onFailure(SupervisorStrategy.restart),
    name = "supervised-actor")

  override def onMessage(msg: String): Behavior[String] =
    msg match {
      case "failChild" =>
        child ! "fail"
        this
    }
}

object SupervisedActor {
  def apply(): Behavior[String] =
    Behaviors.setup(context => new SupervisedActor(context))
}

class SupervisedActor(context: ActorContext[String]) extends AbstractBehavior[String](context) {
  println("supervised actor started")

  override def onMessage(msg: String): Behavior[String] =
    msg match {
      case "fail" =>
        println("supervised actor fails now")
        throw new Exception("I failed!")
    }

  override def onSignal: PartialFunction[Signal, Behavior[String]] = {
    case PreRestart =>
      println("supervised actor will be restarted")
      this
    case PostStop =>
      println("supervised actor stopped")
      this
  }

}
```

사용 예시는 다음과 같습니다.

```scala
val supervisingActor = context.spawn(SupervisingActor(), "supervising-actor")
supervisingActor ! "failChild"
```

콘솔 출력은 다음과 같습니다.

```
supervised actor started
supervised actor fails now
supervised actor will be restarted
supervised actor started
[ERROR] [11/12/2018 12:03:27.171] [ActorHierarchyExperiments-akka.actor.default-dispatcher-2] [akka://ActorHierarchyExperiments/user/supervising-actor/supervised-actor] Supervisor akka.actor.typed.internal.RestartSupervisor@1c452254 saw failure: I failed!
java.lang.Exception: I failed!
	at typed.tutorial_1.SupervisedActor.onMessage(ActorHierarchyExperiments.scala:113)
	at typed.tutorial_1.SupervisedActor.onMessage(ActorHierarchyExperiments.scala:106)
	at akka.actor.typed.scaladsl.AbstractBehavior.receive(AbstractBehavior.scala:59)
	at akka.actor.typed.Behavior$.interpret(Behavior.scala:395)
	at akka.actor.typed.Behavior$.interpretMessage(Behavior.scala:369)
	at akka.actor.typed.internal.InterceptorImpl$$anon$2.apply(InterceptorImpl.scala:49)
	at akka.actor.typed.internal.SimpleSupervisor.aroundReceive(Supervision.scala:85)
	at akka.actor.typed.internal.InterceptorImpl.receive(InterceptorImpl.scala:70)
	at akka.actor.typed.Behavior$.interpret(Behavior.scala:395)
	at akka.actor.typed.Behavior$.interpretMessage(Behavior.scala:369)
```

폴트 톨러런스(fault tolerance, 장애 내성)와 감독 전략에 대한 포괄적인 세부 사항은 폴트 톨러런스 레퍼런스 문서를 참고하시기 바랍니다. 해당 문서는 이러한 메커니즘과 구현 세부 사항에 대해 더 깊이 있게 다룹니다.

### 요약

지금까지 Akka가 액터를 계층적으로 관리하는 방식을 다루었으며, 부모가 자식을 감독하고 예외를 관리하는 방법을 보여주었습니다. 기본적인 액터와 그 자식을 구성하는 메커니즘을 살펴보았습니다. 다음으로, 이 지식을 예제 시나리오에 적용하여 디바이스 액터로부터 정보를 가져오는 데 필요한 통신을 모델링할 것입니다. 그 후에는 그룹으로 조직화된 액터들의 관리를 다룰 것입니다.

---

## 3. Part 2: 첫 번째 액터 만들기 (Creating the First Actor)

액터 계층 구조와 동작을 이해하고 나면, 최상위 IoT 시스템 구성 요소를 어떻게 액터로 매핑할지가 다음 질문이 됩니다. 사용자 가디언(user guardian)은 애플리케이션 전체를 나타내는 최상위 액터입니다. 디바이스와 대시보드를 관리하는 구성 요소들은 이 액터의 자식이 되어, 예제 아키텍처를 트리 구조(tree structure)로 구성할 수 있습니다.

최초의 액터인 `IotSupervisor`는 단 몇 줄의 코드만 필요합니다. 튜토리얼 애플리케이션을 시작하려면 다음과 같이 합니다.

1. `com.example` 패키지에 새로운 `IotSupervisor` 소스 파일을 생성합니다.
2. 다음 코드를 새 파일에 붙여넣어 `IotSupervisor`를 정의합니다.

**Scala:**

```scala
package com.example

import akka.actor.typed.Behavior
import akka.actor.typed.PostStop
import akka.actor.typed.Signal
import akka.actor.typed.scaladsl.AbstractBehavior
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.Behaviors

object IotSupervisor {
  def apply(): Behavior[Nothing] =
    Behaviors.setup[Nothing](context => new IotSupervisor(context))
}

class IotSupervisor(context: ActorContext[Nothing]) extends AbstractBehavior[Nothing](context) {
  context.log.info("IoT Application started")

  override def onMessage(msg: Nothing): Behavior[Nothing] = {
    // No need to handle any messages
    Behaviors.unhandled
  }

  override def onSignal: PartialFunction[Signal, Behavior[Nothing]] = {
    case PostStop =>
      context.log.info("IoT Application stopped")
      this
  }
}
```

이 코드는 앞서의 액터 예제들과 유사하지만, `println()` 대신 Akka의 통합 로깅 기능(logging facility)을 사용합니다.

액터 시스템을 생성하는 `main` 진입점(entry point)을 제공하기 위해, 새로운 `IotApp` 오브젝트에 다음 코드를 추가합니다.

**Scala:**

```scala
package com.example

import akka.actor.typed.ActorSystem

object IotApp {

  def main(args: Array[String]): Unit = {
    // Create ActorSystem and top level supervisor
    ActorSystem[Nothing](IotSupervisor(), "iot-system")
  }

}
```

이 애플리케이션은 시작 상태를 로깅하는 것 외에 최소한의 작업만 수행합니다. 하지만 기초가 되는 액터가 확립되었으며, 추가적인 액터를 받아들일 준비가 되었습니다.

### 다음 단계

이어지는 장(chapter)들은 다음을 통해 애플리케이션을 점진적으로 확장합니다.

1. 디바이스 표현(device representation) 생성하기
2. 디바이스 관리 구성 요소(device management component) 생성하기
3. 디바이스 그룹에 조회(query) 기능 추가하기

---

## 4. Part 3: 디바이스 액터 다루기 (Working with Device Actors)

이전 파트들에서는 액터 시스템을 **거시적으로(in the large)** 바라보는 방법, 즉 구성 요소를 어떻게 표현하고 계층 구조 안에 어떻게 배치할지를 설명했습니다. 이번 파트에서는 디바이스 액터를 구현함으로써 액터를 **미시적으로(in the small)** 살펴봅니다.

객체(object)를 다룰 때는 일반적으로 API를 **인터페이스(interface)**, 즉 실제 구현으로 채워질 추상 메서드들의 모음으로 설계합니다. 액터의 세계에서는 **프로토콜(protocol)**이 인터페이스의 역할을 대신합니다. 프로그래밍 언어로 일반적인 프로토콜을 형식화(formalize)하는 것은 불가능하지만, 그 가장 기본적인 요소인 **메시지(message)**를 구성할 수는 있습니다. 따라서 디바이스 액터에 보내고자 하는 메시지를 식별하는 것부터 시작하겠습니다.

일반적으로 메시지는 범주(category) 또는 패턴(pattern)으로 분류됩니다. 이러한 패턴을 식별하면 그것들 중에서 선택하고 구현하기가 더 쉬워집니다. 첫 번째 예제는 **요청-응답(request-respond)** 메시지 패턴을 보여줍니다.

### 디바이스에 대한 메시지 식별 (Identifying Messages for Devices)

디바이스 액터의 작업은 간단합니다.

- 온도 측정값을 수집한다.
- 요청을 받으면, 마지막으로 측정한 온도를 보고한다.

하지만 디바이스는 즉시 온도 측정값을 갖지 못한 채 시작될 수 있습니다. 따라서 온도가 존재하지 않는 경우를 고려해야 합니다. 이것은 또한 쓰기(write) 부분이 없는 상태에서 액터의 조회(query) 부분을 테스트할 수 있게 해 주는데, 디바이스 액터가 빈 결과(empty result)를 보고할 수 있기 때문입니다.

디바이스 액터로부터 현재 온도를 얻기 위한 프로토콜은 간단합니다. 액터는 다음을 수행합니다.

1. 현재 온도에 대한 요청을 기다린다.
2. 다음 중 하나로 요청에 응답한다.
   - 현재 온도를 포함하거나,
   - 아직 온도를 사용할 수 없음을 나타낸다.

두 개의 메시지, 즉 요청용 하나와 응답용 하나가 필요합니다. 첫 시도는 다음과 같을 수 있습니다.

**Scala:**

```scala
package com.example

import akka.actor.typed.ActorRef

object Device {
  sealed trait Command
  final case class ReadTemperature(replyTo: ActorRef[RespondTemperature]) extends Command
  final case class RespondTemperature(value: Option[Double])
}
```

`ReadTemperature` 메시지는 디바이스 액터가 요청에 응답할 때 사용할 `ActorRef[RespondTemperature]`를 포함하고 있다는 점에 유의하세요.

이 두 메시지는 필요한 기능을 다루는 것처럼 보입니다. 하지만 우리가 선택하는 접근 방식은 애플리케이션의 분산적(distributed) 특성을 고려해야 합니다. 로컬 JVM 상의 액터와 통신하는 기본 메커니즘은 원격 액터와 통신하는 것과 동일하지만, 다음 사항을 염두에 두어야 합니다.

- 로컬 메시지와 원격 메시지 사이에는 전달 지연(latency)에서 관찰 가능한 차이가 있을 것입니다. 네트워크 링크 대역폭(bandwidth)과 메시지 크기 같은 요인도 작용하기 때문입니다.
- 원격 메시지 전송은 더 많은 단계를 수반하므로, 더 많은 것이 잘못될 수 있다는 점에서 신뢰성(reliability)이 우려됩니다.
- 로컬 전송은 동일한 JVM 내에서 메시지에 대한 참조를 전달하며, 전송되는 기반 객체에 대한 제약이 없습니다. 반면 원격 전송(remote transport)은 메시지 크기에 제한을 둡니다.

또한, 동일한 JVM 내에서의 전송은 훨씬 더 신뢰성이 높지만, 액터가 메시지를 처리하는 도중 프로그래머의 오류로 인해 실패하면, 그 효과는 원격 호스트가 메시지를 처리하는 중에 크래시되어 원격 네트워크 요청이 실패한 것과 동일합니다. 두 경우 모두 서비스는 얼마 후 복구되지만(액터는 그 감독자에 의해 재시작되고, 호스트는 운영자나 모니터링 시스템에 의해 재시작됨), 크래시 동안 개별 요청들은 손실됩니다. **따라서 모든 메시지가 손실될 가능성이 있다고 가정하고 액터를 작성하는 것이 안전하고 비관적인(pessimistic) 선택입니다.**

프로토콜에서 유연성이 필요한 이유를 더 잘 이해하려면 Akka의 메시지 순서(message ordering)와 메시지 전달 보장(message delivery guarantees)을 고려하는 것이 도움이 됩니다. Akka는 메시지 전송에 대해 다음과 같은 동작을 제공합니다.

- **최대 한 번 전달(at-most-once delivery)**, 즉 전달이 보장되지 않음.
- 메시지 순서는 **송신자-수신자 쌍(sender, receiver pair)별로** 유지됨.

다음 절들에서 이 동작을 더 자세히 다룹니다.

#### 메시지 전달 (Message Delivery)

메시징 서브시스템이 제공하는 전달 의미론(delivery semantics)은 일반적으로 다음 범주에 속합니다.

- **최대 한 번 전달(At-most-once delivery)** — 각 메시지는 0번 또는 1번 전달됩니다. 더 일상적인 표현으로는 메시지가 손실될 수는 있지만 절대 중복되지 않는다는 뜻입니다.
- **최소 한 번 전달(At-least-once delivery)** — 각 메시지를 적어도 한 번 성공할 때까지 여러 번 전달 시도가 이루어질 수 있습니다. 다시 말해, 메시지가 중복될 수는 있지만 절대 손실되지 않는다는 뜻입니다.
- **정확히 한 번 전달(Exactly-once delivery)** — 각 메시지가 수신자에게 정확히 한 번 전달됩니다. 메시지는 손실될 수도, 중복될 수도 없습니다.

첫 번째 동작인 Akka가 사용하는 방식은 가장 저렴하며 가장 높은 성능을 냅니다. 송신 측이나 전송 메커니즘에 상태를 유지하지 않고 발사 후 망각(fire-and-forget) 방식으로 처리할 수 있기 때문에 구현 오버헤드가 가장 적습니다. 두 번째인 최소 한 번 전달은 전송 손실을 상쇄하기 위해 재시도(retry)가 필요합니다. 이는 송신 측에 상태를 유지하고 수신 측에 확인 응답(acknowledgment) 메커니즘을 두는 오버헤드를 추가합니다. 정확히 한 번 전달은 가장 비싸며 최악의 성능을 냅니다. 최소 한 번 전달이 추가하는 오버헤드에 더해, 중복 전달을 걸러내기 위해 수신 측에 상태를 유지해야 합니다.

액터 시스템에서는 보장(guarantee)의 정확한 의미, 즉 시스템이 어느 시점에 전달이 완료되었다고 간주하는지를 정해야 합니다.

1. 메시지가 네트워크로 전송될 때?
2. 메시지가 대상 액터의 호스트에 수신될 때?
3. 메시지가 대상 액터의 메일박스(mailbox)에 들어갈 때?
4. 메시지 대상 액터가 메시지 처리를 시작할 때?
5. 대상 액터가 메시지를 성공적으로 처리했을 때?

보장된 전달을 주장하는 대부분의 프레임워크와 프로토콜은 실제로는 4번이나 5번과 유사한 것을 제공합니다. 합리적으로 들리지만, **실제로 유용할까요?** 그 함의를 이해하기 위해 간단하고 실용적인 예를 생각해 봅시다. 어떤 사용자가 주문을 넣으려고 하는데, 우리는 그것이 실제로 주문 데이터베이스의 디스크에 기록되었을 때에만 성공적으로 처리되었다고 주장하고 싶습니다.

메시지의 성공적인 처리에 의존한다면, 액터는 주문이 그것을 검증·처리하고 데이터베이스에 넣을 책임을 지는 내부 API에 제출되자마자 성공을 보고할 것입니다. 안타깝게도 API가 호출된 직후 다음 중 어느 것이든 발생할 수 있습니다.

- 호스트가 크래시될 수 있습니다.
- 역직렬화(deserialization)가 실패할 수 있습니다.
- 검증(validation)이 실패할 수 있습니다.
- 데이터베이스를 사용할 수 없을 수 있습니다.
- 프로그래밍 오류가 발생할 수 있습니다.

이는 **전달의 보장(guarantee of delivery)**이 **도메인 수준의 보장(domain level guarantee)**으로 변환되지 않음을 보여줍니다. 우리는 주문이 실제로 완전히 처리되고 영속화(persist)되었을 때에만 성공을 보고하고 싶습니다. **성공을 보고할 수 있는 유일한 주체는 애플리케이션 자신입니다. 오직 애플리케이션만이 요구되는 도메인 보장에 대한 이해를 갖고 있기 때문입니다. 어떤 일반화된 프레임워크도 특정 도메인의 세부 사항과 그 도메인에서 무엇이 성공으로 간주되는지를 파악할 수 없습니다.**

이 특정한 예에서는, 데이터베이스가 주문이 이제 안전하게 저장되었음을 확인한, 성공적인 데이터베이스 쓰기 이후에만 성공을 알리고 싶습니다. **이러한 이유로 Akka는 보장의 책임을 애플리케이션 자체에 위임합니다. 즉, 여러분이 Akka가 제공하는 도구로 직접 그것을 구현해야 합니다. 이것은 여러분이 제공하고자 하는 보장에 대한 완전한 제어권을 줍니다.** 이제 애플리케이션 로직에 대해 추론하기 쉽게 만들어 주는, Akka가 제공하는 메시지 순서를 살펴봅시다.

#### 메시지 순서 (Message Ordering)

Akka에서는 주어진 한 쌍의 액터에 대해, 첫 번째 액터에서 두 번째 액터로 **직접(directly)** 보낸 메시지는 순서가 뒤바뀐 채로 수신되지 않습니다. "직접"이라는 단어는 이 보장이 tell 연산자(operator)를 사용하여 최종 목적지로 직접 보낼 때에만 적용되며, 중개자(mediator)를 사용할 때는 적용되지 않음을 강조합니다.

만약 다음과 같다면:

- 액터 `A1`이 메시지 `M1`, `M2`, `M3`을 `A2`로 보냅니다.
- 액터 `A3`이 메시지 `M4`, `M5`, `M6`을 `A2`로 보냅니다.

이는 Akka 메시지에 대해 다음을 의미합니다.

- `M1`이 전달되면, 반드시 `M2`와 `M3`보다 먼저 전달되어야 합니다.
- `M2`가 전달되면, 반드시 `M3`보다 먼저 전달되어야 합니다.
- `M4`가 전달되면, 반드시 `M5`와 `M6`보다 먼저 전달되어야 합니다.
- `M5`가 전달되면, 반드시 `M6`보다 먼저 전달되어야 합니다.
- `A2`는 `A1`로부터의 메시지와 `A3`으로부터의 메시지가 뒤섞인(interleaved) 형태로 볼 수 있습니다.
- 전달이 보장되지 않으므로, 어떤 메시지든 누락될 수 있습니다. 즉, `A2`에 도착하지 않을 수 있습니다.

이 보장은 좋은 균형을 이룹니다. 한 액터로부터의 메시지가 순서대로 도착하는 것은 쉽게 추론할 수 있는 시스템을 구축하는 데 편리합니다. 반면, 서로 다른 액터들로부터의 메시지가 뒤섞여 도착하는 것을 허용함으로써 액터 시스템의 효율적인 구현을 위한 충분한 자유를 제공합니다.

전달 보장에 대한 전체 세부 사항은 레퍼런스 페이지를 참고하세요.

### 디바이스 메시지에 유연성 추가하기

우리의 첫 번째 조회 프로토콜은 정확했지만, 분산 애플리케이션 실행을 고려하지 않았습니다. (타임아웃된 요청 때문에) 디바이스 액터를 조회하는 액터에서 재전송(resend)을 구현하거나, 여러 액터를 조회하고자 한다면, 요청과 응답을 상관(correlate)시킬 수 있어야 합니다. 따라서 요청자가 ID를 제공할 수 있도록 메시지에 필드를 하나 더 추가합니다(이 코드는 이후 단계에서 앱에 추가할 것입니다).

**Scala:**

```scala
sealed trait Command
final case class ReadTemperature(requestId: Long, replyTo: ActorRef[RespondTemperature]) extends Command
final case class RespondTemperature(requestId: Long, value: Option[Double])
```

### 디바이스 액터와 읽기 프로토콜 구현

Hello World 예제에서 배웠듯이, 각 액터는 자신이 받아들일 메시지의 타입을 정의합니다. 우리의 디바이스 액터는 주어진 조회에 대한 응답에 동일한 ID 파라미터를 사용할 책임이 있는데, 이는 다음과 같은 모습이 됩니다.

**Scala:**

```scala
package com.example

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.PostStop
import akka.actor.typed.Signal
import akka.actor.typed.scaladsl.AbstractBehavior
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.Behaviors

object Device {
  def apply(groupId: String, deviceId: String): Behavior[Command] =
    Behaviors.setup(context => new Device(context, groupId, deviceId))

  sealed trait Command
  final case class ReadTemperature(requestId: Long, replyTo: ActorRef[RespondTemperature]) extends Command
  final case class RespondTemperature(requestId: Long, value: Option[Double])
}

class Device(context: ActorContext[Device.Command], groupId: String, deviceId: String)
    extends AbstractBehavior[Device.Command](context) {
  import Device._

  var lastTemperatureReading: Option[Double] = None

  context.log.info("Device actor {}-{} started", groupId, deviceId)

  override def onMessage(msg: Command): Behavior[Command] = {
    msg match {
      case ReadTemperature(id, replyTo) =>
        replyTo ! RespondTemperature(id, lastTemperatureReading)
        this
    }
  }

  override def onSignal: PartialFunction[Signal, Behavior[Command]] = {
    case PostStop =>
      context.log.info("Device actor {}-{} stopped", groupId, deviceId)
      this
  }

}
```

코드에서 다음을 주목하세요.

- 컴패니언 오브젝트(companion object)의 `apply` 메서드(Java에서는 정적 `create` 메서드)는 `Device` 액터의 `Behavior`를 어떻게 생성할지 정의합니다. 파라미터로는 디바이스의 ID와 그것이 속한 그룹이 포함되며, 이후에 사용할 것입니다.
- 앞서 추론했던 메시지들은 컴패니언 오브젝트(또는 Device 클래스)에 정의됩니다.
- `Device` 클래스에서 `lastTemperatureReading`의 값은 처음에 `None`(Java에서는 `Optional.empty()`)으로 설정되며, 조회를 받으면 액터가 이를 다시 보고합니다.

### 액터 테스트

위 액터를 기반으로 테스트를 작성할 수 있습니다. 프로젝트의 테스트 트리(test tree)에 있는 `com.example` 패키지에, 다음 코드를 `DeviceSpec.scala`(또는 Java의 경우 `DeviceTest.java`) 파일에 추가합니다. (여기서는 ScalaTest를 사용하지만 Akka Testkit과 함께 다른 테스트 프레임워크를 사용할 수도 있습니다.)

이 테스트는 sbt 프롬프트에서 `test`를 실행하거나 `mvn test`를 실행하여 수행할 수 있습니다.

**Scala:**

```scala
package com.example

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.wordspec.AnyWordSpecLike

class DeviceSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {
  import Device._

  "Device actor" must {

    "reply with empty reading if no temperature is known" in {
      val probe = createTestProbe[RespondTemperature]()
      val deviceActor = spawn(Device("group", "device"))

      deviceActor ! Device.ReadTemperature(requestId = 42, probe.ref)
      val response = probe.receiveMessage()
      response.requestId should ===(42)
      response.value should ===(None)
    }
  }
}
```

이제 액터는 센서로부터 메시지를 받았을 때 온도의 상태를 변경할 방법이 필요합니다.

### 쓰기 프로토콜 추가

쓰기 프로토콜의 목적은 액터가 온도를 포함하는 메시지를 받았을 때 `currentTemperature` 필드를 갱신하는 것입니다. 다시 한번, 쓰기 프로토콜을 다음과 같은 매우 단순한 메시지로 정의하고 싶은 유혹이 듭니다.

**Scala:**

```scala
sealed trait Command
final case class RecordTemperature(value: Double) extends Command
```

하지만 이 접근 방식은 온도 기록 메시지의 송신자가 메시지가 처리되었는지 아닌지를 결코 확신할 수 없다는 점을 고려하지 않습니다. 우리는 Akka가 이러한 메시지의 전달을 보장하지 않으며 성공 알림 제공을 애플리케이션에 맡긴다는 것을 보았습니다. 우리의 경우, 마지막 온도 기록을 갱신한 후 송신자에게 확인 응답(acknowledgment)을 보내고 싶습니다. 예를 들어 `TemperatureRecorded` 메시지로 응답하는 것입니다. 온도 조회와 응답의 경우와 마찬가지로, 최대한의 유연성을 제공하기 위해 ID 필드를 포함하는 것도 좋은 생각입니다.

**Scala:**

```scala
final case class RecordTemperature(requestId: Long, value: Double, replyTo: ActorRef[TemperatureRecorded])
    extends Command
final case class TemperatureRecorded(requestId: Long)
```

### 읽기/쓰기 메시지를 가진 액터

읽기와 쓰기 프로토콜을 함께 결합하면, 디바이스 액터는 다음 예제와 같은 모습이 됩니다.

**Scala:**

```scala
package com.example

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.PostStop
import akka.actor.typed.Signal
import akka.actor.typed.scaladsl.AbstractBehavior
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.Behaviors

object Device {
  def apply(groupId: String, deviceId: String): Behavior[Command] =
    Behaviors.setup(context => new Device(context, groupId, deviceId))

  sealed trait Command

  final case class ReadTemperature(requestId: Long, replyTo: ActorRef[RespondTemperature]) extends Command
  final case class RespondTemperature(requestId: Long, value: Option[Double])

  final case class RecordTemperature(requestId: Long, value: Double, replyTo: ActorRef[TemperatureRecorded])
      extends Command
  final case class TemperatureRecorded(requestId: Long)
}

class Device(context: ActorContext[Device.Command], groupId: String, deviceId: String)
    extends AbstractBehavior[Device.Command](context) {
  import Device._

  var lastTemperatureReading: Option[Double] = None

  context.log.info("Device actor {}-{} started", groupId, deviceId)

  override def onMessage(msg: Command): Behavior[Command] = {
    msg match {
      case RecordTemperature(id, value, replyTo) =>
        context.log.info("Recorded temperature reading {} with {}", value, id)
        lastTemperatureReading = Some(value)
        replyTo ! TemperatureRecorded(id)
        this

      case ReadTemperature(id, replyTo) =>
        replyTo ! RespondTemperature(id, lastTemperatureReading)
        this
    }
  }

  override def onSignal: PartialFunction[Signal, Behavior[Command]] = {
    case PostStop =>
      context.log.info("Device actor {}-{} stopped", groupId, deviceId)
      this
  }

}
```

이제 읽기/조회 기능과 쓰기/기록 기능을 함께 동작시키는 새로운 테스트 케이스도 작성해야 합니다.

**Scala:**

```scala
"reply with latest temperature reading" in {
  val recordProbe = createTestProbe[TemperatureRecorded]()
  val readProbe = createTestProbe[RespondTemperature]()
  val deviceActor = spawn(Device("group", "device"))

  deviceActor ! Device.RecordTemperature(requestId = 1, 24.0, recordProbe.ref)
  recordProbe.expectMessage(Device.TemperatureRecorded(requestId = 1))

  deviceActor ! Device.ReadTemperature(requestId = 2, readProbe.ref)
  val response1 = readProbe.receiveMessage()
  response1.requestId should ===(2)
  response1.value should ===(Some(24.0))

  deviceActor ! Device.RecordTemperature(requestId = 3, 55.0, recordProbe.ref)
  recordProbe.expectMessage(Device.TemperatureRecorded(requestId = 3))

  deviceActor ! Device.ReadTemperature(requestId = 4, readProbe.ref)
  val response2 = readProbe.receiveMessage()
  response2.requestId should ===(4)
  response2.value should ===(Some(55.0))
}
```

### 다음 단계

지금까지 우리는 전체 아키텍처를 설계하기 시작했고, 도메인에 직접 대응하는 첫 번째 액터를 작성했습니다. 이제 디바이스 그룹과 디바이스 액터 자체를 유지 관리하는 책임을 지는 구성 요소를 만들어야 합니다.

---

## 5. Part 4: 디바이스 그룹 다루기 (Working with Device Groups)

가정의 온도를 모니터링하는 완전한 IoT 시스템에서, 디바이스 센서들은 프로토콜을 통해 연결되어 디바이스 매니저(device manager) 구성 요소에 등록됩니다. 시스템은 센서 상태를 유지하는 책임을 지는 액터를 조회(look up)하거나 생성하여 등록을 처리합니다. 액터는 확인 응답으로 응답하며 자신의 `ActorRef`를 노출합니다. 그러면 네트워킹 구성 요소는 이 참조를 사용하여 센서와 디바이스 액터 간에 디바이스 매니저를 거치지 않고 직접 통신합니다.

주요 아키텍처상의 과제는 적절한 액터 단위(actor granularity)를 선택하는 것입니다. 지침은 더 큰 단위(larger granularity)를 선호하되, 시스템이 다음을 필요로 할 때 더 세밀한 단위를 추가하라고 제안합니다. 더 높은 동시성(concurrency)이 필요할 때, 많은 상태를 가진 복잡한 대화가 필요할 때, 액터들 사이에 나눌 만큼 충분한 상태가 있을 때, 또는 서로 무관한 여러 책임이 있어 별도의 액터가 개별적인 장애를 다른 것들에 최소한의 영향을 주며 처리하도록 할 수 있을 때입니다.

### 디바이스 매니저 계층 구조

디바이스 매니저는 3단계 액터 트리(three-level actor tree)로 모델링됩니다.

- **최상위 감독자(Top level supervisor)**: 디바이스를 위한 시스템 구성 요소를 나타내며, 디바이스 그룹과 디바이스 액터를 조회하고 생성하는 진입점 역할을 합니다.
- **그룹 액터(Group actors)**: 각각 하나의 그룹 ID(예: 하나의 가정)에 대한 디바이스 액터들을 감독합니다. 사용 가능한 디바이스들로부터 온도 측정값을 조회하는 것과 같은 서비스를 제공합니다.
- **디바이스 액터(Device actors)**: 실제 디바이스 센서와의 상호작용을 관리하며, 온도 측정값을 저장합니다.

이 아키텍처는 다음을 제공합니다: 그룹 내 장애 격리(isolation of failures), 그룹에 속한 디바이스 조회 간소화, 각 그룹이 전용 액터에서 동시에 실행되므로 병렬성(parallelism) 향상, 개별 디바이스 장애 격리, 그리고 네트워크 연결이 개별 디바이스 액터와 직접 통신함으로써 온도 측정값 수집 시 병렬성 향상입니다.

### 등록 프로토콜

등록 프로토콜의 기능은 필요한 동작을 다음과 같이 정의합니다.

1. `DeviceManager`가 그룹 ID와 디바이스 ID를 가진 요청을 받으면, 그것을 기존 그룹 액터로 전달하거나, 새로운 디바이스 그룹 액터를 생성하고 요청을 전달합니다.
2. `DeviceGroup` 액터는 기존 디바이스 액터의 `ActorRef`로 응답하거나, 디바이스 액터를 하나 생성하고 그 새 액터 참조로 응답합니다.
3. 센서는 직접 메시징을 위해 디바이스 액터의 `ActorRef`를 받습니다.

등록 요청과 확인 응답에 사용되는 메시지는 다음과 같이 정의됩니다.

**Scala:**

```scala
final case class RequestTrackDevice(groupId: String, deviceId: String, replyTo: ActorRef[DeviceRegistered])
    extends DeviceManager.Command
    with DeviceGroup.Command

final case class DeviceRegistered(device: ActorRef[Device.Command])
```

### 디바이스 그룹 액터에 등록 지원 추가하기

#### 등록 요청 처리

디바이스 그룹 액터는 기존 자식의 `ActorRef`로 응답하거나 하나를 생성해야 합니다. 디바이스 ID로 자식 액터를 조회하기 위해 `Map`이 사용됩니다.

**Scala:**

```scala
object DeviceGroup {
  def apply(groupId: String): Behavior[Command] =
    Behaviors.setup(context => new DeviceGroup(context, groupId))

  trait Command
  private final case class DeviceTerminated(device: ActorRef[Device.Command], groupId: String, deviceId: String)
      extends Command
}

class DeviceGroup(context: ActorContext[DeviceGroup.Command], groupId: String)
    extends AbstractBehavior[DeviceGroup.Command](context) {
  import DeviceGroup._
  import DeviceManager.{ DeviceRegistered, ReplyDeviceList, RequestDeviceList, RequestTrackDevice }

  private var deviceIdToActor = Map.empty[String, ActorRef[Device.Command]]

  context.log.info("DeviceGroup {} started", groupId)

  override def onMessage(msg: Command): Behavior[Command] =
    msg match {
      case trackMsg @ RequestTrackDevice(`groupId`, deviceId, replyTo) =>
        deviceIdToActor.get(deviceId) match {
          case Some(deviceActor) =>
            replyTo ! DeviceRegistered(deviceActor)
          case None =>
            context.log.info("Creating device actor for {}", trackMsg.deviceId)
            val deviceActor = context.spawn(Device(groupId, deviceId), s"device-$deviceId")
            deviceIdToActor += deviceId -> deviceActor
            replyTo ! DeviceRegistered(deviceActor)
        }
        this

      case RequestTrackDevice(gId, _, _) =>
        context.log.warn("Ignoring TrackDevice request for {}. This actor is responsible for {}.", gId, groupId)
        this
    }

  override def onSignal: PartialFunction[Signal, Behavior[Command]] = {
    case PostStop =>
      context.log.info("DeviceGroup {} stopped", groupId)
      this
  }
}
```

### 그룹 내 디바이스 액터 추적하기

디바이스는 생겼다가 사라지므로, 맵(map)에서 디바이스 액터를 제거해야 합니다. 디바이스가 제거되면 그에 대응하는 액터도 정지합니다. Akka는 한 액터가 다른 액터를 감시(watch)하고 그것이 정지하면 알림을 받을 수 있게 해 주는 **데스 워치(Death Watch)** 기능을 제공합니다. 감시되던 액터가 정지하면, 감시자(watcher)는 감시되던 액터 참조를 담은 `Terminated(actorRef)` 시그널을 받습니다.

하나의 디바이스가 정지한 후에도 그룹은 계속 동작해야 하므로, `Terminated(actorRef)` 시그널을 처리하는 것이 필요합니다. 그룹의 디바이스 액터는 새 디바이스 액터가 생성될 때 그것을 감시하기 시작하고, 정지를 나타내는 알림이 오면 맵에서 디바이스 액터를 제거해야 합니다.

`Terminated`에만 의존하는 대신 커스텀 메시지를 사용하면 그 메시지에 디바이스 ID를 담을 수 있습니다.

**Scala:**

```scala
class DeviceGroup(context: ActorContext[DeviceGroup.Command], groupId: String)
    extends AbstractBehavior[DeviceGroup.Command](context) {
  import DeviceGroup._
  import DeviceManager.{ DeviceRegistered, ReplyDeviceList, RequestDeviceList, RequestTrackDevice }

  private var deviceIdToActor = Map.empty[String, ActorRef[Device.Command]]

  context.log.info("DeviceGroup {} started", groupId)

  override def onMessage(msg: Command): Behavior[Command] =
    msg match {
      case trackMsg @ RequestTrackDevice(`groupId`, deviceId, replyTo) =>
        deviceIdToActor.get(deviceId) match {
          case Some(deviceActor) =>
            replyTo ! DeviceRegistered(deviceActor)
          case None =>
            context.log.info("Creating device actor for {}", trackMsg.deviceId)
            val deviceActor = context.spawn(Device(groupId, deviceId), s"device-$deviceId")
            context.watchWith(deviceActor, DeviceTerminated(deviceActor, groupId, deviceId))
            deviceIdToActor += deviceId -> deviceActor
            replyTo ! DeviceRegistered(deviceActor)
        }
        this

      case RequestTrackDevice(gId, _, _) =>
        context.log.warn("Ignoring TrackDevice request for {}. This actor is responsible for {}.", gId, groupId)
        this

      case DeviceTerminated(_, _, deviceId) =>
        context.log.info("Device actor for {} has been terminated", deviceId)
        deviceIdToActor -= deviceId
        this
    }

  override def onSignal: PartialFunction[Signal, Behavior[Command]] = {
    case PostStop =>
      context.log.info("DeviceGroup {} stopped", groupId)
      this
  }
}
```

디바이스 목록 조회 기능은 새로운 조회 메시지로 추가됩니다.

**Scala:**

```scala
final case class RequestDeviceList(requestId: Long, groupId: String, replyTo: ActorRef[ReplyDeviceList])
    extends DeviceManager.Command
    with DeviceGroup.Command

final case class ReplyDeviceList(requestId: Long, ids: Set[String])
```

디바이스 그룹 액터 구현은 디바이스 목록 조회를 처리하도록 갱신됩니다.

**Scala:**

```scala
class DeviceGroup(context: ActorContext[DeviceGroup.Command], groupId: String)
    extends AbstractBehavior[DeviceGroup.Command](context) {
  import DeviceGroup._
  import DeviceManager.{ DeviceRegistered, ReplyDeviceList, RequestDeviceList, RequestTrackDevice }

  private var deviceIdToActor = Map.empty[String, ActorRef[Device.Command]]

  context.log.info("DeviceGroup {} started", groupId)

  override def onMessage(msg: Command): Behavior[Command] =
    msg match {
      case trackMsg @ RequestTrackDevice(`groupId`, deviceId, replyTo) =>
        deviceIdToActor.get(deviceId) match {
          case Some(deviceActor) =>
            replyTo ! DeviceRegistered(deviceActor)
          case None =>
            context.log.info("Creating device actor for {}", trackMsg.deviceId)
            val deviceActor = context.spawn(Device(groupId, deviceId), s"device-$deviceId")
            context.watchWith(deviceActor, DeviceTerminated(deviceActor, groupId, deviceId))
            deviceIdToActor += deviceId -> deviceActor
            replyTo ! DeviceRegistered(deviceActor)
        }
        this

      case RequestTrackDevice(gId, _, _) =>
        context.log.warn("Ignoring TrackDevice request for {}. This actor is responsible for {}.", gId, groupId)
        this

      case RequestDeviceList(requestId, gId, replyTo) =>
        if (gId == groupId) {
          replyTo ! ReplyDeviceList(requestId, deviceIdToActor.keySet)
          this
        } else
          Behaviors.unhandled

      case DeviceTerminated(_, _, deviceId) =>
        context.log.info("Device actor for {} has been terminated", deviceId)
        deviceIdToActor -= deviceId
        this
    }

  override def onSignal: PartialFunction[Signal, Behavior[Command]] = {
    case PostStop =>
      context.log.info("DeviceGroup {} stopped", groupId)
      this
  }
}
```

디바이스 제거를 테스트하기 위해, 외부에서 정지를 허용하는 `Passivate` 메시지를 Device 액터에 추가합니다.

**Scala:**

```scala
case object Passivate extends Command
```

`Passivate`를 처리하는 Device 액터 구현은 다음과 같습니다.

**Scala:**

```scala
import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.PostStop
import akka.actor.typed.Signal
import akka.actor.typed.scaladsl.AbstractBehavior
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.Behaviors

object Device {
  def apply(groupId: String, deviceId: String): Behavior[Command] =
    Behaviors.setup(context => new Device(context, groupId, deviceId))

  sealed trait Command
  final case class ReadTemperature(requestId: Long, replyTo: ActorRef[RespondTemperature]) extends Command
  final case class RespondTemperature(requestId: Long, value: Option[Double])

  final case class RecordTemperature(requestId: Long, value: Double, replyTo: ActorRef[TemperatureRecorded])
      extends Command
  final case class TemperatureRecorded(requestId: Long)

  case object Passivate extends Command
}

class Device(context: ActorContext[Device.Command], groupId: String, deviceId: String)
    extends AbstractBehavior[Device.Command](context) {
  import Device._

  var lastTemperatureReading: Option[Double] = None

  context.log.info("Device actor {}-{} started", groupId, deviceId)

  override def onMessage(msg: Command): Behavior[Command] = {
    msg match {
      case RecordTemperature(id, value, replyTo) =>
        context.log.info("Recorded temperature reading {} with {}", value, id)
        lastTemperatureReading = Some(value)
        replyTo ! TemperatureRecorded(id)
        this

      case ReadTemperature(id, replyTo) =>
        replyTo ! RespondTemperature(id, lastTemperatureReading)
        this

      case Passivate =>
        Behaviors.stopped
    }
  }

  override def onSignal: PartialFunction[Signal, Behavior[Command]] = {
    case PostStop =>
      context.log.info("Device actor {}-{} stopped", groupId, deviceId)
      this
  }
}
```

### 디바이스 매니저 액터 만들기

디바이스 매니저 구성 요소의 진입점은, 그룹 액터가 디바이스 액터를 생성하는 방식과 유사하게, 필요에 따라(on-demand) 디바이스 그룹 액터를 생성합니다.

**Scala:**

```scala
object DeviceManager {
  def apply(): Behavior[Command] =
    Behaviors.setup(context => new DeviceManager(context))

  sealed trait Command

  final case class RequestTrackDevice(groupId: String, deviceId: String, replyTo: ActorRef[DeviceRegistered])
      extends DeviceManager.Command
      with DeviceGroup.Command

  final case class DeviceRegistered(device: ActorRef[Device.Command])

  final case class RequestDeviceList(requestId: Long, groupId: String, replyTo: ActorRef[ReplyDeviceList])
      extends DeviceManager.Command
      with DeviceGroup.Command

  final case class ReplyDeviceList(requestId: Long, ids: Set[String])

  private final case class DeviceGroupTerminated(groupId: String) extends DeviceManager.Command
}

class DeviceManager(context: ActorContext[DeviceManager.Command])
    extends AbstractBehavior[DeviceManager.Command](context) {
  import DeviceManager._

  var groupIdToActor = Map.empty[String, ActorRef[DeviceGroup.Command]]

  context.log.info("DeviceManager started")

  override def onMessage(msg: Command): Behavior[Command] =
    msg match {
      case trackMsg @ RequestTrackDevice(groupId, _, replyTo) =>
        groupIdToActor.get(groupId) match {
          case Some(ref) =>
            ref ! trackMsg
          case None =>
            context.log.info("Creating device group actor for {}", groupId)
            val groupActor = context.spawn(DeviceGroup(groupId), "group-" + groupId)
            context.watchWith(groupActor, DeviceGroupTerminated(groupId))
            groupActor ! trackMsg
            groupIdToActor += groupId -> groupActor
        }
        this

      case req @ RequestDeviceList(requestId, groupId, replyTo) =>
        groupIdToActor.get(groupId) match {
          case Some(ref) =>
            ref ! req
          case None =>
            replyTo ! ReplyDeviceList(requestId, Set.empty)
        }
        this

      case DeviceGroupTerminated(groupId) =>
        context.log.info("Device group actor for {} has been terminated", groupId)
        groupIdToActor -= groupId
        this
    }

  override def onSignal: PartialFunction[Signal, Behavior[Command]] = {
    case PostStop =>
      context.log.info("DeviceManager stopped")
      this
  }
}
```

### 테스트 케이스

다음 테스트 케이스들은 디바이스 그룹 기능을 검증합니다.

**Scala:**

```scala
"be able to register a device actor" in {
  val probe = createTestProbe[DeviceRegistered]()
  val groupActor = spawn(DeviceGroup("group"))

  groupActor ! RequestTrackDevice("group", "device1", probe.ref)
  val registered1 = probe.receiveMessage()
  val deviceActor1 = registered1.device

  // another deviceId
  groupActor ! RequestTrackDevice("group", "device2", probe.ref)
  val registered2 = probe.receiveMessage()
  val deviceActor2 = registered2.device
  deviceActor1 should !==(deviceActor2)

  // Check that the device actors are working
  val recordProbe = createTestProbe[TemperatureRecorded]()
  deviceActor1 ! RecordTemperature(requestId = 0, 1.0, recordProbe.ref)
  recordProbe.expectMessage(TemperatureRecorded(requestId = 0))
  deviceActor2 ! Device.RecordTemperature(requestId = 1, 2.0, recordProbe.ref)
  recordProbe.expectMessage(Device.TemperatureRecorded(requestId = 1))
}

"ignore requests for wrong groupId" in {
  val probe = createTestProbe[DeviceRegistered]()
  val groupActor = spawn(DeviceGroup("group"))

  groupActor ! RequestTrackDevice("wrongGroup", "device1", probe.ref)
  probe.expectNoMessage(500.milliseconds)
}

"return same actor for same deviceId" in {
  val probe = createTestProbe[DeviceRegistered]()
  val groupActor = spawn(DeviceGroup("group"))

  groupActor ! RequestTrackDevice("group", "device1", probe.ref)
  val registered1 = probe.receiveMessage()

  // registering same again should be idempotent
  groupActor ! RequestTrackDevice("group", "device1", probe.ref)
  val registered2 = probe.receiveMessage()

  registered1.device should ===(registered2.device)
}

"be able to list active devices" in {
  val registeredProbe = createTestProbe[DeviceRegistered]()
  val groupActor = spawn(DeviceGroup("group"))

  groupActor ! RequestTrackDevice("group", "device1", registeredProbe.ref)
  registeredProbe.receiveMessage()

  groupActor ! RequestTrackDevice("group", "device2", registeredProbe.ref)
  registeredProbe.receiveMessage()

  val deviceListProbe = createTestProbe[ReplyDeviceList]()
  groupActor ! RequestDeviceList(requestId = 0, groupId = "group", deviceListProbe.ref)
  deviceListProbe.expectMessage(ReplyDeviceList(requestId = 0, Set("device1", "device2")))
}

"be able to list active devices after one shuts down" in {
  val registeredProbe = createTestProbe[DeviceRegistered]()
  val groupActor = spawn(DeviceGroup("group"))

  groupActor ! RequestTrackDevice("group", "device1", registeredProbe.ref)
  val registered1 = registeredProbe.receiveMessage()
  val toShutDown = registered1.device

  groupActor ! RequestTrackDevice("group", "device2", registeredProbe.ref)
  registeredProbe.receiveMessage()

  val deviceListProbe = createTestProbe[ReplyDeviceList]()
  groupActor ! RequestDeviceList(requestId = 0, groupId = "group", deviceListProbe.ref)
  deviceListProbe.expectMessage(ReplyDeviceList(requestId = 0, Set("device1", "device2")))

  toShutDown ! Passivate
  registeredProbe.expectTerminated(toShutDown, registeredProbe.remainingOrDefault)

  // using awaitAssert to retry because it might take longer for the groupActor
  // to see the Terminated, that order is undefined
  registeredProbe.awaitAssert {
    groupActor ! RequestDeviceList(requestId = 1, groupId = "group", deviceListProbe.ref)
    deviceListProbe.expectMessage(ReplyDeviceList(requestId = 1, Set("device2")))
  }
}
```

### 다음 단계

디바이스를 등록하고 추적하며 측정값을 기록하는 계층적 구성 요소가 이제 완성되었습니다. 이 구현은 여러 대화 패턴(conversation pattern)을 보여줍니다. 온도 기록을 위한 요청-응답(request-respond), 디바이스 등록을 위한 필요시 생성(create-on-demand), 그리고 정리(cleanup)를 동반한 부모-자식 관계 수립을 위한 생성-감시-종료(create-watch-terminate)입니다.

다음 장에서는 **스캐터-개더(scatter-gather)** 대화 패턴을 수립하는 그룹 조회(group query) 기능을 소개하며, 사용자가 그룹에 속한 모든 디바이스의 상태를 조회할 수 있게 하는 기능을 구현합니다.

---

## 6. Part 5: 디바이스 그룹 조회하기 (Querying Device Groups)

이 파트에서는 그룹 내 모든 디바이스 액터를 조회하는 **스캐터-개더 조회 패턴(scatter-gather query pattern)**을 구현합니다. 핵심 과제는 **그룹의 멤버십이 동적(dynamic)이라는 점입니다. 각 센서 디바이스는 언제든지 정지할 수 있는 액터로 표현됩니다.**

### 동적 액터 멤버십 처리

이 구현은 다음과 같은 특정 동작들로 정리됩니다.

- 조회(query)가 도착하면, 그룹 액터는 기존 디바이스 액터들의 **스냅샷(snapshot)**을 취하고, 그 액터들에게만 온도를 요청합니다.
- 조회가 도착한 **이후에** 시작되는 액터들은 무시됩니다.
- 스냅샷에 포함된 액터가 응답하지 않고 조회 도중 정지하면, 우리는 그것이 정지했다는 사실을 조회 메시지의 송신자에게 보고합니다.

타임아웃 처리의 경우, 다음 두 경우 중 하나에서 조회를 완료된 것으로 간주합니다. 스냅샷에 포함된 모든 액터가 응답했거나 정지되었음을 확인한 경우, 또는 미리 정의된 마감 시한(deadline)에 도달한 경우입니다.

### 메시지 프로토콜

이 프로토콜은 조회 도중 디바이스의 네 가지 상태를 정의합니다.

**Scala:**

```scala
final case class RequestAllTemperatures(requestId: Long, groupId: String, replyTo: ActorRef[RespondAllTemperatures])
    extends DeviceGroupQuery.Command
    with DeviceGroup.Command
    with DeviceManager.Command

final case class RespondAllTemperatures(requestId: Long, temperatures: Map[String, TemperatureReading])

sealed trait TemperatureReading
final case class Temperature(value: Double) extends TemperatureReading
case object TemperatureNotAvailable extends TemperatureReading
case object DeviceNotAvailable extends TemperatureReading
case object DeviceTimedOut extends TemperatureReading
```

### 아키텍처 근거 (Architecture Rationale)

조회 로직을 그룹 액터에 내장하는 대신, 이 설계는 **단일 조회(single query)를 나타내며 그룹 액터를 대신하여 조회를 완료하는 액터**를 별도로 생성합니다. 이 접근 방식은 그룹 디바이스 액터를 단순하게 유지하고, 조회 기능을 독립적으로 테스트할 수 있게 해 줍니다.

### DeviceGroupQuery 액터 구현

#### 조회 액터 정의

조회 액터에는 다음이 필요합니다. 조회할 활성 디바이스 액터들의 스냅샷과 ID들, 조회를 시작한 요청의 ID(응답에 포함할 수 있도록), 조회를 보낸 액터의 참조(응답을 이 액터에게 직접 보냄), 그리고 조회가 응답을 얼마나 기다려야 하는지를 나타내는 마감 시한(deadline)입니다.

#### 타임아웃 스케줄링

이 구현은 마감 시한 관리를 강제하기 위해 `Behaviors.withTimers`와 `startSingleTimer`를 사용합니다.

**Scala:**

```scala
object DeviceGroupQuery {

  def apply(
      deviceIdToActor: Map[String, ActorRef[Device.Command]],
      requestId: Long,
      requester: ActorRef[DeviceManager.RespondAllTemperatures],
      timeout: FiniteDuration): Behavior[Command] = {
    Behaviors.setup { context =>
      Behaviors.withTimers { timers =>
        new DeviceGroupQuery(deviceIdToActor, requestId, requester, timeout, context, timers)
      }
    }
  }

  trait Command

  private case object CollectionTimeout extends Command

  final case class WrappedRespondTemperature(response: Device.RespondTemperature) extends Command

  private final case class DeviceTerminated(deviceId: String) extends Command
}

class DeviceGroupQuery(
    deviceIdToActor: Map[String, ActorRef[Device.Command]],
    requestId: Long,
    requester: ActorRef[DeviceManager.RespondAllTemperatures],
    timeout: FiniteDuration,
    context: ActorContext[DeviceGroupQuery.Command],
    timers: TimerScheduler[DeviceGroupQuery.Command])
    extends AbstractBehavior[DeviceGroupQuery.Command](context) {

  import DeviceGroupQuery._
  import DeviceManager.DeviceNotAvailable
  import DeviceManager.DeviceTimedOut
  import DeviceManager.RespondAllTemperatures
  import DeviceManager.Temperature
  import DeviceManager.TemperatureNotAvailable
  import DeviceManager.TemperatureReading

  timers.startSingleTimer(CollectionTimeout, CollectionTimeout, timeout)

  private val respondTemperatureAdapter = context.messageAdapter(WrappedRespondTemperature.apply)

  private var repliesSoFar = Map.empty[String, TemperatureReading]
  private var stillWaiting = deviceIdToActor.keySet

  deviceIdToActor.foreach {
    case (deviceId, device) =>
      context.watchWith(device, DeviceTerminated(deviceId))
      device ! Device.ReadTemperature(0, respondTemperatureAdapter)
  }

  override def onMessage(msg: Command): Behavior[Command] =
    msg match {
      case WrappedRespondTemperature(response) => onRespondTemperature(response)
      case DeviceTerminated(deviceId)          => onDeviceTerminated(deviceId)
      case CollectionTimeout                   => onCollectionTimout()
    }

  private def onRespondTemperature(response: Device.RespondTemperature): Behavior[Command] = {
    val reading = response.value match {
      case Some(value) => Temperature(value)
      case None        => TemperatureNotAvailable
    }

    val deviceId = response.deviceId
    repliesSoFar += (deviceId -> reading)
    stillWaiting -= deviceId

    respondWhenAllCollected()
  }

  private def onDeviceTerminated(deviceId: String): Behavior[Command] = {
    if (stillWaiting(deviceId)) {
      repliesSoFar += (deviceId -> DeviceNotAvailable)
      stillWaiting -= deviceId
    }
    respondWhenAllCollected()
  }

  private def onCollectionTimout(): Behavior[Command] = {
    repliesSoFar ++= stillWaiting.map(deviceId => deviceId -> DeviceTimedOut)
    stillWaiting = Set.empty
    respondWhenAllCollected()
  }

  private def respondWhenAllCollected(): Behavior[Command] = {
    if (stillWaiting.isEmpty) {
      requester ! RespondAllTemperatures(requestId, repliesSoFar)
      Behaviors.stopped
    } else {
      this
    }
  }
}
```

### 디바이스 액터 갱신

Device 액터의 `RespondTemperature`는 디바이스 ID를 포함해야 합니다.

**Scala:**

```scala
final case class RespondTemperature(requestId: Long, deviceId: String, value: Option[Double])
```

핸들러는 다음과 같습니다.

```scala
case ReadTemperature(id, replyTo) =>
  replyTo ! RespondTemperature(id, deviceId, lastTemperatureReading)
  this
```

### 쿼리를 DeviceGroup에 통합하기

그룹 액터는 필요에 따라 조회 액터를 생성합니다.

**Scala:**

```scala
class DeviceGroup(context: ActorContext[DeviceGroup.Command], groupId: String)
    extends AbstractBehavior[DeviceGroup.Command](context) {
  import DeviceGroup._
  import DeviceManager.{
    DeviceRegistered,
    ReplyDeviceList,
    RequestAllTemperatures,
    RequestDeviceList,
    RequestTrackDevice
  }

  private var deviceIdToActor = Map.empty[String, ActorRef[Device.Command]]

  context.log.info("DeviceGroup {} started", groupId)

  override def onMessage(msg: Command): Behavior[Command] =
    msg match {
      // ... other cases omitted

      case RequestAllTemperatures(requestId, gId, replyTo) =>
        if (gId == groupId) {
          context.spawnAnonymous(
            DeviceGroupQuery(deviceIdToActor, requestId = requestId, requester = replyTo, 3.seconds))
          this
        } else
          Behaviors.unhandled
    }

  override def onSignal: PartialFunction[Signal, Behavior[Command]] = {
    case PostStop =>
      context.log.info("DeviceGroup {} stopped", groupId)
      this
  }
}
```

### 테스트

#### 테스트 1: 정상 경로 - 온도 값

**Scala:**

```scala
"return temperature value for working devices" in {
  val requester = createTestProbe[RespondAllTemperatures]()

  val device1 = createTestProbe[Command]()
  val device2 = createTestProbe[Command]()

  val deviceIdToActor = Map("device1" -> device1.ref, "device2" -> device2.ref)

  val queryActor =
    spawn(DeviceGroupQuery(deviceIdToActor, requestId = 1, requester = requester.ref, timeout = 3.seconds))

  device1.expectMessageType[Device.ReadTemperature]
  device2.expectMessageType[Device.ReadTemperature]

  queryActor ! WrappedRespondTemperature(Device.RespondTemperature(requestId = 0, "device1", Some(1.0)))
  queryActor ! WrappedRespondTemperature(Device.RespondTemperature(requestId = 0, "device2", Some(2.0)))

  requester.expectMessage(
    RespondAllTemperatures(
      requestId = 1,
      temperatures = Map("device1" -> Temperature(1.0), "device2" -> Temperature(2.0))))
}
```

#### 테스트 2: 온도를 사용할 수 없는 경우

**Scala:**

```scala
"return TemperatureNotAvailable for devices with no readings" in {
  val requester = createTestProbe[RespondAllTemperatures]()

  val device1 = createTestProbe[Command]()
  val device2 = createTestProbe[Command]()

  val deviceIdToActor = Map("device1" -> device1.ref, "device2" -> device2.ref)

  val queryActor =
    spawn(DeviceGroupQuery(deviceIdToActor, requestId = 1, requester = requester.ref, timeout = 3.seconds))

  device1.expectMessageType[Device.ReadTemperature]
  device2.expectMessageType[Device.ReadTemperature]

  queryActor ! WrappedRespondTemperature(Device.RespondTemperature(requestId = 0, "device1", None))
  queryActor ! WrappedRespondTemperature(Device.RespondTemperature(requestId = 0, "device2", Some(2.0)))

  requester.expectMessage(
    RespondAllTemperatures(
      requestId = 1,
      temperatures = Map("device1" -> TemperatureNotAvailable, "device2" -> Temperature(2.0))))
}
```

#### 테스트 3: 디바이스가 응답하기 전에 정지하는 경우

**Scala:**

```scala
"return DeviceNotAvailable if device stops before answering" in {
  val requester = createTestProbe[RespondAllTemperatures]()

  val device1 = createTestProbe[Command]()
  val device2 = createTestProbe[Command]()

  val deviceIdToActor = Map("device1" -> device1.ref, "device2" -> device2.ref)

  val queryActor =
    spawn(DeviceGroupQuery(deviceIdToActor, requestId = 1, requester = requester.ref, timeout = 3.seconds))

  device1.expectMessageType[Device.ReadTemperature]
  device2.expectMessageType[Device.ReadTemperature]

  queryActor ! WrappedRespondTemperature(Device.RespondTemperature(requestId = 0, "device1", Some(2.0)))

  device2.stop()

  requester.expectMessage(
    RespondAllTemperatures(
      requestId = 1,
      temperatures = Map("device1" -> Temperature(2.0), "device2" -> DeviceNotAvailable)))
}
```

#### 테스트 4: 디바이스가 응답한 후에 정지하는 경우

**Scala:**

```scala
"return temperature reading even if device stops after answering" in {
  val requester = createTestProbe[RespondAllTemperatures]()

  val device1 = createTestProbe[Command]()
  val device2 = createTestProbe[Command]()

  val deviceIdToActor = Map("device1" -> device1.ref, "device2" -> device2.ref)

  val queryActor =
    spawn(DeviceGroupQuery(deviceIdToActor, requestId = 1, requester = requester.ref, timeout = 3.seconds))

  device1.expectMessageType[Device.ReadTemperature]
  device2.expectMessageType[Device.ReadTemperature]

  queryActor ! WrappedRespondTemperature(Device.RespondTemperature(requestId = 0, "device1", Some(1.0)))
  queryActor ! WrappedRespondTemperature(Device.RespondTemperature(requestId = 0, "device2", Some(2.0)))

  device2.stop()

  requester.expectMessage(
    RespondAllTemperatures(
      requestId = 1,
      temperatures = Map("device1" -> Temperature(1.0), "device2" -> Temperature(2.0))))
}
```

#### 테스트 5: 타임아웃 처리

**Scala:**

```scala
"return DeviceTimedOut if device does not answer in time" in {
  val requester = createTestProbe[RespondAllTemperatures]()

  val device1 = createTestProbe[Command]()
  val device2 = createTestProbe[Command]()

  val deviceIdToActor = Map("device1" -> device1.ref, "device2" -> device2.ref)

  val queryActor =
    spawn(DeviceGroupQuery(deviceIdToActor, requestId = 1, requester = requester.ref, timeout = 200.millis))

  device1.expectMessageType[Device.ReadTemperature]
  device2.expectMessageType[Device.ReadTemperature]

  queryActor ! WrappedRespondTemperature(Device.RespondTemperature(requestId = 0, "device1", Some(1.0)))

  // no reply from device2

  requester.expectMessage(
    RespondAllTemperatures(
      requestId = 1,
      temperatures = Map("device1" -> Temperature(1.0), "device2" -> DeviceTimedOut)))
}
```

#### 테스트 6: DeviceGroup과의 통합

**Scala:**

```scala
"be able to collect temperatures from all active devices" in {
  val registeredProbe = createTestProbe[DeviceRegistered]()
  val groupActor = spawn(DeviceGroup("group"))

  groupActor ! RequestTrackDevice("group", "device1", registeredProbe.ref)
  val deviceActor1 = registeredProbe.receiveMessage().device

  groupActor ! RequestTrackDevice("group", "device2", registeredProbe.ref)
  val deviceActor2 = registeredProbe.receiveMessage().device

  groupActor ! RequestTrackDevice("group", "device3", registeredProbe.ref)
  registeredProbe.receiveMessage()

  // Check that the device actors are working
  val recordProbe = createTestProbe[TemperatureRecorded]()
  deviceActor1 ! RecordTemperature(requestId = 0, 1.0, recordProbe.ref)
  recordProbe.expectMessage(TemperatureRecorded(requestId = 0))
  deviceActor2 ! RecordTemperature(requestId = 1, 2.0, recordProbe.ref)
  recordProbe.expectMessage(TemperatureRecorded(requestId = 1))
  // No temperature for device3

  val allTempProbe = createTestProbe[RespondAllTemperatures]()
  groupActor ! RequestAllTemperatures(requestId = 0, groupId = "group", allTempProbe.ref)
  allTempProbe.expectMessage(
    RespondAllTemperatures(
      requestId = 0,
      temperatures =
        Map("device1" -> Temperature(1.0), "device2" -> Temperature(2.0), "device3" -> TemperatureNotAvailable)))
}
```

### 요약

스캐터-개더 패턴(scatter-gather pattern) 구현은 몇 가지 핵심 액터 개념을 보여줍니다. 감시(watching)를 통한 동적 액터 생명주기 관리, 타이머(timer)를 이용한 타임아웃 조정, 별도의 액터 인스턴스를 통한 여러 동시 조회 처리, 그리고 어댑터(adapter)를 사용한 서로 다른 메시지 프로토콜 간의 변환입니다. 이 패턴은 디바이스가 나타났다 사라질 수 있고 서로 다른 속도로 응답하는 분산 시스템을 조회하는 과제를 성공적으로 해결합니다.

---

## 참고 자료

- [Akka 공식 문서](https://doc.akka.io/libraries/akka-core/current/)
- [Part 1: Actor Architecture](https://doc.akka.io/libraries/akka-core/current/typed/guide/tutorial_1.html)
- [Part 2: Creating the First Actor](https://doc.akka.io/libraries/akka-core/current/typed/guide/tutorial_2.html)
- [Part 3: Working with Device Actors](https://doc.akka.io/libraries/akka-core/current/typed/guide/tutorial_3.html)
- [Part 4: Working with Device Groups](https://doc.akka.io/libraries/akka-core/current/typed/guide/tutorial_4.html)
- [Part 5: Querying Device Groups](https://doc.akka.io/libraries/akka-core/current/typed/guide/tutorial_5.html)
