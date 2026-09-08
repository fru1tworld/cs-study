# Shardcake 시작하기

> 원본: https://devsisters.github.io/shardcake/docs/

<a id="1-shardcake란"></a>
## 1. Shardcake란

Shardcake는 여러 서버에 엔티티(entity)를 분산하고, 각 엔티티가 어디에 있는지 몰라도 ID만으로 상호작용하게 해 주는 Scala 오픈소스 라이브러리다. 호출하는 쪽에서 실제 위치를 알 필요가 없는 이 특성을 위치 투명성(location transparency)이라고 한다.

이 API는 순수 함수형이며 [ZIO](https://zio.dev)에 크게 의존한다. 뒤의 예제를 읽으려면 먼저 ZIO에 익숙해지는 편이 좋다.

<a id="2-간단한-사용-사례"></a>

## 2. 간단한 사용 사례

사용자(user)가 길드(guild)에 가입하는 멀티플레이어 게임을 만든다고 가정하자. 게임이 성공하면 부하도 늘어날 테니 여러 서버로 확장(scale out)할 수 있어야 한다. 그런데 정원이 30명인 길드에 이미 29명이 있고, 두 사용자가 동시에 가입을 시도하면 어떻게 될까?

- ![naive diagram](https://devsisters.github.io/shardcake/usecase1.png)

두 서버가 가입 요청을 각각 처리하면 둘 다 현재 인원을 29명으로 읽을 수 있다. 두 서버 모두 정원에 여유가 있다고 판단해 가입을 허용하면, 길드원이 31명으로 늘어 정원을 넘긴다.

두 요청이 같은 상태를 보고 가입을 허용하지 않도록 만드는 일반적인 방법은 두 가지다.

- 전역 락(Global lock) 방식: 길드원을 확인할 때 여러 게임 서버가 공유하는 락(lock)을 획득 → 새 길드원 저장 후 해제
  - 두 게임 서버가 동시에 작업을 시도해도 두 번째 서버는 첫 번째 서버가 끝날 때까지 대기
- 단일 기록자(Single writer) 방식: 두 요청을 두 게임 서버에서 동시에 처리하는 대신 하나의 엔티티로 넘겨 순차적으로 처리

엔티티 샤딩(Entity Sharding)은 두 번째 방식을 구현한다. 길드 엔티티를 게임 서버 전체에 나눠 배치하되, 각 엔티티는 한 시점에 한 곳에만 존재하게 한다.

- ![single writer diagram](https://devsisters.github.io/shardcake/usecase2.png)

요청을 하나의 엔티티로 모으려면 이번에는 그 엔티티가 어느 서버에 있는지 찾아야 한다. 엔티티 샤딩의 위치 투명성은 이 탐색을 샤딩 시스템에 맡긴다. 호출하는 쪽은 엔티티 ID만 넘기면 된다.

- Shardcake는 다음을 담당하는 구성 요소를 제공

- 엔티티를 게임 서버에 할당하는 작업을 자동으로 관리
- 엔티티 ID를 사용해 엔티티에 메시지 전송

- 샤딩 설정 후에는 위치와 무관하게 길드에 메시지를 보내는 다음과 같은 코드 작성 가능

```scala
def joinGuild(guildId: GuildId): ZIO[Context, Throwable, GuildState] =
  for {
    userId     <- getCurrentUserFromContext
    guildState <- guild.send(guildId)(JoinGuild(userId, _))
  } yield guildState
```

<a id="3-용어terminology"></a>

## 3. 용어(Terminology)

- 더 나아가기 전에 몇 가지 용어를 정의

- 엔티티(entity): ID로 지정할 수 있는 작은 메시지 핸들러
  - 예를 들어 `User A`나 `Guild B`가 엔티티이며, 이때 `User`와 `Guild`는 엔티티 타입(entity type)이라 함
- 파드(pod): 엔티티를 호스팅할 수 있는 애플리케이션 서버
  - 엔티티는 보통 여러 파드에서 실행되지만 하나의 엔티티는 한 시점에 하나의 파드에서만 실행 → 같은 엔티티가 서로 다른 두 파드에서 동시에 실행되는 일은 없음
- 샤드(shard): 항상 같은 파드에 위치하는 엔티티의 논리적 그룹
  - 엔티티는 수백만 개가 될 수 있음 → 엔티티-파드 매핑을 수백만 개 유지하는 대신 엔티티를 샤드로 묶어 적당한 크기의 샤드-파드 매핑만 유지

- ![terminology diagram](https://devsisters.github.io/shardcake/terminology.png)

<a id="4-핵심-구성-요소key-components"></a>

## 4. 핵심 구성 요소(Key Components)

- Shardcake는 두 가지 주요 구성 요소로 구성

- Shard Manager: 인스턴스 하나만 실행되면 되는 독립 구성 요소 → 샤드를 파드에 할당하는 일을 담당
- 엔티티(Entities): 애플리케이션 서버에서 실행되며 자신에게 전달된 메시지를 처리
  - 엔티티 동작(엔티티 영속화 포함)은 전적으로 사용자가 정의
  - Shardcake는 올바른 파드에서 엔티티를 시작하고 엔티티끼리 통신하게 하는 일만 담당

- 원하는 기술로 직접 구현할 수 있는 교체 가능한(pluggable) 부분 4가지

- `Storage` 트레이트: 샤드 할당(assignment)을 어디에 저장할지 정의
  - Shardcake는 Redis를 사용하는 구현을 제공
- `Pods` 트레이트: 원격 파드와 통신하는 방법을 정의
  - Shardcake는 프로토콜로 gRPC를 사용하는 구현을 제공
- `Serialization`: 메시지를 인코딩하고 디코딩하는 방법을 정의
  - Shardcake는 Kryo를 사용하는 구현을 제공
- `PodsHealth` 트레이트: 파드가 정상인지 판단하는 방법을 정의
  - Shardcake는 k8s API를 사용하는 구현을 제공

- ![architecture diagram](https://devsisters.github.io/shardcake/arch.png)

<a id="5-예제"></a>

## 5. 예제

<a id="shard-manager"></a>

### Shard Manager

먼저 Shard Manager를 시작한다. `shardcake-manager` 의존성을 추가하면 사용할 수 있으며, 실행에는 `Storage`, `Pods`, `PodsHealth` 구현이 필요하다.

이 예제는 외부 시스템 없이 실행할 수 있도록 인메모리 `Storage`와 핑(ping)으로 파드의 생존 여부를 확인하는 `PodsHealth`를 사용한다. 파드 사이의 메시지 전송에는 `shardcake-protocol-grpc`를 사용한다.

```
libraryDependencies += "com.devsisters" %% "shardcake-manager"       % "2.7.1"
libraryDependencies += "com.devsisters" %% "shardcake-protocol-grpc" % "2.7.1"
```

- Shard Manager는 작은 GraphQL API를 노출 → 간단한 웹 서버 시작 필요
  - `Server.run`을 호출하고 필요한 의존성을 모두 제공하면 됨

```scala
import com.devsisters.shardcake._
import com.devsisters.shardcake.interfaces._
import zio._

object ShardManagerApp extends ZIOAppDefault {
  def run: Task[Nothing] =
    Server.run.provide(
      ZLayer.succeed(ManagerConfig.default),
      ZLayer.succeed(GrpcConfig.default),
      PodsHealth.local, // just ping a pod to see if it's alive
      GrpcPods.live,    // use gRPC protocol
      Storage.memory,   // store data in memory
      ShardManager.live // shard manager logic
    )
}
```

이 앱을 실행하면 Shard Manager와 API가 시작되어 파드의 등록(registration)을 받을 수 있다.

<a id="엔티티-동작entity-behavior"></a>

### 엔티티 동작(Entity Behavior)

Shard Manager가 준비됐으니 이제 엔티티가 받을 메시지와 그 메시지를 처리할 동작을 정의한다. 앞의 길드(Guild)를 모델로 삼아 가입과 탈퇴를 구현한다.

- 먼저 다음 의존성 필요

```
libraryDependencies += "com.devsisters" %% "shardcake-entities"      % "2.7.1"
libraryDependencies += "com.devsisters" %% "shardcake-protocol-grpc" % "2.7.1"
```

엔티티가 받을 메시지는 가입과 탈퇴 두 가지로, `sealed trait` 아래에 정의한다. 가입처럼 응답이 필요한 메시지에는 `Replier[A]`를 포함한다. 여기서 `A`는 응답 타입이다.

```scala
sealed trait GuildMessage

object GuildMessage {
  case class Join(userId: String, replier: Replier[Try[Set[String]]]) extends GuildMessage
  case class Leave(userId: String)                                    extends GuildMessage
}
```

- 엔티티 타입(Entity Type)도 정의 필요
  - `EntityType`을 상속하면서 메시지 타입과 이 타입을 식별하는 고유한 `String` 식별자를 지정하면 됨

```scala
object Guild extends EntityType[GuildMessage]("guild")
```

- 동작(behavior) 자체는 다음 시그니처를 갖는 함수

```scala
def behavior(entityId: String, messages: Queue[GuildMessage]): RIO[Sharding, Nothing]
```

이 함수는 `entityId`와 `Queue[GuildMessage]`를 받아 `messages`를 계속 소비하는 `ZIO`를 반환한다. 실행이 끝나지 않으므로 성공 값의 타입은 `Nothing`이다.

계속 메시지를 읽는 루프를 만들기 전에, 하나의 `GuildMessage`를 처리하는 함수를 정의한다. 길드원 목록은 `Ref[Set[String]]`에 담는다. `Join`을 받으면 정원이 찼는지 확인하고, 자리가 있으면 상태를 바꾼 뒤 `replier.reply`로 응답한다. 여기서는 정원을 5명으로 하드코딩하고, 성공한 경우의 전체 길드원 목록과 오류를 모두 표현할 수 있도록 응답을 `Try`로 감싼다.

```scala
def handleMessage(state: Ref[Set[String]], message: GuildMessage): RIO[Sharding, Unit] =
  message match {
    case GuildMessage.Join(userId, replier) =>
      state.get.flatMap(members =>
        if (members.size >= 5)
          replier.reply(Failure(new Exception("Guild is already full!")))
        else
          state.updateAndGet(_ + userId).flatMap { newMembers =>
            replier.reply(Success(newMembers))
          }
      )
    case GuildMessage.Leave(userId)         =>
      state.update(_ - userId)
  }
```

- 이제 엔티티가 생성될 때 빈 상태에서 출발하는 동작을 만들 준비 완료

```scala
def behavior(entityId: String, messages: Queue[GuildMessage]): RIO[Sharding, Nothing] =
  Ref
    .make(Set.empty[String])
    .flatMap(state => messages.take.flatMap(handleMessage(state, _)).forever)
```

<a id="애플리케이션-실행"></a>

### 애플리케이션 실행

- 엔티티를 실행하려면 그 동작을 샤딩 시스템에 등록 필요
  - `Sharding.registerEntity`에 엔티티 타입과 동작을 넘겨 호출하면 됨

- 그다음 새 파드가 엔티티를 실행할 준비가 되어 샤드를 할당받을 수 있음을 Shard Manager에 통지 필요
  - `Sharding.registerScoped`를 호출하면 되는데, 이는 프로그램 시작 시 `Sharding.register`를 호출하고 종료 시 `Sharding.unregister`를 호출하는 것과 동일

- 엔티티와 통신하려면 `Messenger[GuildMessage]` 필요 → `Sharding.messenger`를 호출해 획득
  - 엔티티를 전혀 호스팅하지 않는 파드에서도 `messenger` 사용 가능

```scala
  val program =
    for {
      _     <- Sharding.registerEntity(Guild, behavior)
      _     <- Sharding.registerScoped
      guild <- Sharding.messenger(Guild)
      _     <- guild.send("guild1")(Join("user1", _)).debug
      _     <- guild.send("guild1")(Join("user2", _)).debug
      _     <- guild.send("guild1")(Join("user3", _)).debug
      _     <- guild.send("guild1")(Join("user4", _)).debug
      _     <- guild.send("guild1")(Join("user5", _)).debug
      _     <- guild.send("guild1")(Join("user6", _)).debug
    } yield ()
```

- 마지막으로 Shard Manager 때와 마찬가지로 필요한 의존성을 모두 제공

```scala
def run: Task[Unit] =
  ZIO.scoped(program).provide(
    ZLayer.succeed(Config.default),
    ZLayer.succeed(GrpcConfig.default),
    Serialization.javaSerialization, // use java serialization for messages
    Storage.memory,                  // store data in memory
    ShardManagerClient.liveWithSttp, // client to communicate with the Shard Manager
    GrpcPods.live,                   // use gRPC protocol
    GrpcShardingService.live,        // expose gRPC service
    Sharding.live                    // sharding logic
  )
```

- 이제 프로그램 실행 가능
  - 길드에 `Join` 메시지 6개를 차례로 전송하고 결과를 출력

```
Success(Set(user1))
Success(Set(user1, user2))
Success(Set(user1, user2, user3))
Success(Set(user1, user2, user3, user4))
Success(HashSet(user1, user5, user4, user2, user3))
Failure(java.lang.Exception: Guild is already full!)
```

#### 스트리밍(Streaming)

- `send` 외에도 `sendStream`을 사용하면 단일 응답 대신 응답 스트림(stream) 수신 가능
  - 이 함수는 `Replier[A]` 대신 `StreamReplier[A]`를 제공 → 엔티티가 스트림으로 응답

#### 한 가지 더

- `Sharding`은 일부 사용 사례에서 유용한 메서드 두 가지도 제공

- `registerSingleton`: 어느 시점에나 오직 한 파드에서만 실행되는 백그라운드 프로세스를 등록
  - 싱글턴(singleton)은 메시지를 받을 수 없으며, 비활성(inactivity) 상태여도 중지되지 않음
- `registerTopic`: 등록된 모든 파드로 메시지를 브로드캐스트(broadcast)(`Sharding.messenger` 대신 `Sharding.broadcaster` 사용).

<a id="6-여기서-더-나아가려면"></a>

## 6. 여기서 더 나아가려면

지금까지의 예제는 파드가 하나라 샤딩의 이점을 보여 주지 못한다. 같은 코드를 여러 파드에서 실행하면 샤딩 시스템이 길드별로 실행할 파드를 정한다. 각 길드는 한 파드에서만 실행되므로 `guild1`로 보내는 메시지도 모두 같은 파드에서 처리된다.

여러 파드에서 실행하려면 저장 방식을 두 군데 바꿔야 한다.

- 샤딩에 실제 `Storage` 구현(예: Redis)을 사용
- 길드 상태를 인메모리 대신 어딘가에 저장하여, 길드 엔티티가 한 파드에서 다른 파드로 이동해도 상태를 잃지 않도록 함

- 이 예제의 실행 가능한 코드는 [여기서 확인 가능하며](https://github.com/devsisters/shardcake/tree/series/2.x/examples/src/main/scala/example/simple), Redis로 데이터를 영속화하고 여러 파드를 실행할 수 있는 [더 복잡한 예제](https://github.com/devsisters/shardcake/tree/series/2.x/examples/src/main/scala/example/complex)도 존재

- 샤딩 내부 동작 원리는 [아키텍처(Architecture)](02_architecture.md) 문서 참고
  - [설정(Configuration)](03_configuration.md) 문서는 샤딩 시스템 설정 방법을 설명
  - [커스터마이징(Customization)](04_customization.md) 문서는 직접 만든 스토리지, 직렬화, 메시징 프로토콜을 사용하는 방법과 Shardcake가 제공하는 옵션을 설명

> **Akka Cluster Sharding과의 차이점**

>

> [Akka Cluster Sharding](https://doc.akka.io/docs/akka/current/typed/cluster-sharding.html)은 Scala에서 샤딩을 구현하는 주요 대안. Shardcake는 다음과 같은 점에서 Akka와 차이.

>

> - Shardcake는 `ZIO` 기반의 **순수 함수형 라이브러리**인 반면, Akka는 "더 나은 Java" 스타일의 Scala에 가까운 라이브러리이며 `Future` 기반.

> - Akka에서는 모든 애플리케이션 서버가 **클러스터의 일부여야** 함 → 설정이 까다롭고 온갖 종류의 장애에 민감. Shardcake에서는 Shard Manager가 외부 구성 요소이고 파드 상태 감시를 Kubernetes 같은 시스템에 위임 → 이런 문제 없음.

> - Akka는 단순한 샤딩 라이브러리를 훨씬 넘어서는 사실상 기능이 아주 많은 프레임워크 전체. 반면 Shardcake는 훨씬 **단순하고 모듈화**되어 있어 파드 간 메시징 프로토콜을 비롯한 여러 요소를 직접 커스터마이징 가능.

> - Akka는 **상용 지원**(commercial support)을 제공하는 [Lightbend](https://www.lightbend.com)가 뒷받침하는 반면, Shardcake는 [Devsisters](https://www.devsisters.com)와 **ZIO 커뮤니티**가 뒷받침.

<a id="7-참고-자료"></a>

## 7. 참고 자료

- [Shardcake 공식 문서: Getting Started](https://devsisters.github.io/shardcake/docs/)
- [Shardcake GitHub 저장소](https://github.com/devsisters/shardcake)
- [간단한 예제 코드](https://github.com/devsisters/shardcake/tree/series/2.x/examples/src/main/scala/example/simple)
- [복잡한 예제 코드(Redis, gRPC, Kryo)](https://github.com/devsisters/shardcake/tree/series/2.x/examples/src/main/scala/example/complex)
- [ZIO 공식 문서](https://zio.dev)
