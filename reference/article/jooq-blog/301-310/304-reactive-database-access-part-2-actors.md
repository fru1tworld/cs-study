# 리액티브 데이터베이스 접근 - Part 2 - 액터

> 원문: https://blog.jooq.org/reactive-database-access-part-2-actors/

이 글은 Manuel Bernhardt의 리액티브 데이터베이스 접근에 관한 3부작 게스트 포스트 시리즈 중 2부입니다. 전체 시리즈는 다음과 같습니다:

- Part 1: 왜 리액티브인가, 왜 "비동기"인가 & Future 소개
- Part 2: 액터 소개 (이 글)
- Part 3: jOOQ와 Scala, Future, 액터 함께 사용하기

저자 소개:
Manuel Bernhardt는 독립 소프트웨어 컨설턴트이자 "Reactive Web Applications" (Manning)의 저자입니다. 그는 2010년부터 Scala, Akka, Play Framework를 사용해 왔습니다.

## 액터 기초

### 액터 소개

액터는 비동기 메시지 전달을 통해 통신하는 경량 객체입니다. 주요 특징은 다음과 같습니다:

"각 액터는 메일박스를 가지고 있으며, 들어오는 메시지는 처리되기 전에 이 메일박스에 대기열로 저장됩니다."

액터는 세 가지 작업을 수행할 수 있습니다:
- 메시지 송수신
- 메시지에 대한 응답으로 동작/상태 변경
- 새로운 자식 액터 생성

액터는 다양한 상태(시작됨, 재개됨, 중지됨, 재시작됨)를 가지며, 액터 간 통신을 위한 액터 참조를 보유합니다.

### 코드 예제: 기본 액터 생성

```scala
import akka.actor._

class Luke extends Actor {
  def receive = {
    case _ => // 아무것도 하지 않음
  }
}
```

### 메시지 송수신

메시지는 불변이어야 하며 자체적으로 완결성을 가져야 합니다. case object를 사용한 예제:

```scala
import akka.actor._

case object RevelationOfFathership

class Luke extends Actor {
  def receive = {
    case RevelationOfFathership =>
      System.err.println("Noooooooooo")
  }
}
```

### Vader 액터 예제

```scala
import akka.actor._

class Vader extends Actor {

  override def preStart(): Unit =
    context.actorSelection("akka://application/user/luke") ! RevelationOfFathership

  def receive = {
    case _ => // ...
  }
}
```

### 액터 시스템 초기화

```scala
import akka.actor._

val system = ActorSystem("application")
val luke = system.actorOf(Props[Luke], name = "luke")
val vader = system.actorOf(Props[Vader], name = "vader")
```

Props는 액터 인스턴스화를 위한 불변 설명입니다.

## 액터 감독

### 계층 구조와 감독

액터는 부모-자식 관계가 있는 계층 구조 내에 존재합니다. User Guardian은 모든 사용자 공간 액터를 감독합니다. 감독자는 자식 액터의 실패를 어떻게 처리할지 결정합니다.

### 스톰트루퍼를 사용한 라우터 예제

```scala
import akka.actor._
import akka.routing._

class Vader extends Actor {

  val troopers: ActorRef = context.actorOf(
    RoundRobinPool(8).props(Props[StromTrooper])
  )
}
```

RoundRobinPool은 여러 액터에 메시지를 순차적으로 분배합니다.

### 감독 전략 예제

```scala
import akka.actor._

class Vader extends Actor {

  val troopers: ActorRef = context.actorOf(
    RoundRobinPool(8).props(Props[StromTrooper])
  )

  override def supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 3) {
      case t: Throwable =>
        log.error("StormTrooper down!", t)
        SupervisorStrategy.Restart
    }
}
```

이 전략은 실패한 스톰트루퍼를 최대 3회까지 재시작합니다.

## Future와 액터 결합하기

### 파이프 패턴

핵심 원칙: "블로킹 네트워크 호출과 같은 블로킹 작업을 수행해서는 안 됩니다."

기본 파이프 패턴 예제:

```scala
import akka.actor._
import akka.pattern.pipe

class Luke extends Actor {
  def receive = {
    case RevelationOfFathership =>
      sendTweet("Nooooooo") pipeTo self
    case tsr: TweetSendingResult =>
      // ...
  }

  def sendTweet(msg: String): Future[TweetSendingResult] = ...
}
```

### 파이프에서의 실패 복구

```scala
import akka.actor._
import akka.pattern.pipe
import scala.control.NonFatal

class Luke extends Actor {
  def receive = {
    case RevelationOfFathership =>
      val message = "Nooooooo"
      sendTweet(message) recover {
        case NonFatal(t) => TweetSendFailure(message, t)
      } pipeTo self
    case tsr: TweetSendingResult =>
      // ...
    case tsf: TweetSendingFailure =>
      // ...
  }

  def sendTweet(msg: String): Future[TweetSendingResult] = ...
}
```

이 개선된 버전은 실패를 더 우아하게 처리하여 맥락에 맞는 오류 처리를 가능하게 합니다.

## 결론

Part 3에서는 jOOQ를 사용한 리액티브 데이터베이스 접근을 위해 Future와 액터를 결합하는 방법을 시연할 것입니다.

---

참고: 원문 기사에는 "이 기사의 예제는 Typesafe의 Activator가 현재 다르게 작동하므로 구식일 수 있습니다"라는 면책 조항이 포함되어 있습니다.
