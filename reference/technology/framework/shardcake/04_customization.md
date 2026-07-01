# Shardcake 커스터마이징

> 원본: https://devsisters.github.io/shardcake/docs/customization.html

---

## 목차

1. [Storage](#1-storage)
2. [메시징 프로토콜(Messaging Protocol)](#2-메시징-프로토콜messaging-protocol)
3. [직렬화(Serialization)](#3-직렬화serialization)
4. [헬스(Health)](#4-헬스health)
5. [참고 자료](#5-참고-자료)

---

[시작하기](01_getting_started.md#4-핵심-구성-요소key-components)에서 보았듯이, 시스템에는 자유롭게 커스터마이징할 수 있는 부분이 여러 곳 있습니다. Shardcake는 이런 부분마다 최소한 테스트용 가짜(fake) 구현과 흔히 쓰이는 기술로 만든 프로덕션용 구현을 함께 제공합니다. 물론 직접 구현해 써도 좋습니다.

![architecture diagram](https://devsisters.github.io/shardcake/arch.png)

---

## 1. Storage

`Storage` 트레이트는 파드와 샤드 할당 정보를 저장하고 읽어 오는 방법을 정의합니다. 메서드는 다섯 개입니다. 할당 정보를 다루는 `getAssignments`/`saveAssignments`, 파드를 다루는 `getPods`/`savePods`, 그리고 할당 갱신 사항을 전달받는 `assignmentsStream`입니다.

```scala
trait Storage {
  def getAssignments: Task[Map[ShardId, Option[PodAddress]]]
  def saveAssignments(assignments: Map[ShardId, Option[PodAddress]]): Task[Unit]
  
  def assignmentsStream: ZStream[Any, Throwable, Map[Int, Option[PodAddress]]]
  
  def getPods: Task[Map[PodAddress, Pod]]
  def savePods(pods: Map[PodAddress, Pod]): Task[Unit]
}
```

테스트할 때는 데이터를 메모리에 담아 두는 `Storage.memory` 레이어를 쓰면 됩니다.

Shardcake는 Redis4cats 라이브러리로 Redis 위에 구현한 `Storage`를 제공합니다(Redisson 기반 대안도 있습니다). 사용하려면 다음 의존성을 추가하세요.

```scala
libraryDependencies += "com.devsisters" %% "shardcake-storage-redis" % "2.7.1"
```

그리고 `StorageRedis.live` 레이어를 그대로 가져다 쓰면 됩니다.

이 레이어에는 다음 옵션을 담은 `RedisConfig`가 필요합니다.

- `assignmentsKey`: 샤드 할당 정보를 Redis에 저장할 때 쓰는 키
- `podsKey`: 등록된 파드를 Redis에 저장할 때 쓰는 키

여기에 `Redis` 객체도 필요합니다. 내부적으로 쓰는 [redis4cats](https://redis4cats.profunktor.dev/) 라이브러리의 `RedisCommands[Task, String, String] with PubSubCommands[fs2Stream, String, String]`를 가리키는 별칭입니다. 만드는 예시는 다음과 같습니다.

```scala
import com.devsisters.shardcake.StorageRedis.Redis
import dev.profunktor.redis4cats.Redis
import dev.profunktor.redis4cats.connection.RedisClient
import dev.profunktor.redis4cats.data.RedisCodec
import dev.profunktor.redis4cats.effect.Log
import dev.profunktor.redis4cats.pubsub.PubSub
import zio.interop.catz._
import zio.{ Task, ZEnvironment, ZIO, ZLayer }

val redis: ZLayer[Any, Throwable, Redis] =
  ZLayer.scopedEnvironment {
    implicit val runtime: zio.Runtime[Any] = zio.Runtime.default
    implicit val logger: Log[Task]         = new Log[Task] {
      override def debug(msg: => String): Task[Unit] = ZIO.unit
      override def error(msg: => String): Task[Unit] = ZIO.logError(msg)
      override def info(msg: => String): Task[Unit]  = ZIO.logDebug(msg)
    }

    (for {
      client   <- RedisClient[Task].from("redis://foobared@localhost")
      commands <- Redis[Task].fromClient(client, RedisCodec.Utf8)
      pubSub   <- PubSub.mkPubSubConnection[Task, String, String](client, RedisCodec.Utf8)
    } yield ZEnvironment(commands, pubSub)).toScopedZIO
  }
```

---

## 2. 메시징 프로토콜(Messaging Protocol)

`Pods` 트레이트는 원격 파드와 통신하는 방법을 정의합니다. Shard Manager가 샤드를 할당하거나 해제할 때, 그리고 파드끼리 내부 통신(서로 메시지를 주고받는 일)을 할 때 모두 쓰입니다.

```scala
trait Pods {
  def assignShards(pod: PodAddress, shards: Set[ShardId]): Task[Unit]
  def unassignShards(pod: PodAddress, shards: Set[ShardId]): Task[Unit]
  def ping(pod: PodAddress): Task[Unit]
  
  def sendMessage(pod: PodAddress, message: BinaryMessage): Task[Option[Array[Byte]]]
  def sendMessageStreaming(pod: PodAddress, message: BinaryMessage): ZStream[Any, Throwable, Array[Byte]]
}
```

테스트할 때는 아무 일도 하지 않는 `Pods.noop` 레이어를 쓰면 됩니다.

Shardcake는 gRPC 프로토콜로 만든 `Pods` 구현을 제공합니다. 사용하려면 다음 의존성을 추가하세요.

```scala
libraryDependencies += "com.devsisters" %% "shardcake-protocol-grpc" % "2.7.1"
```

그리고 `GrpcPods.live` 레이어를 그대로 가져다 쓰면 됩니다.

파드 쪽에서는 gRPC API도 노출해야 합니다. 환경에 `GrpcShardingService.live` 레이어를 추가하면 됩니다. 다만 이 레이어는 Shard Manager에는 필요하지 않습니다.

---

## 3. 직렬화(Serialization)

`Serialization` 트레이트는 파드 사이에 오가는 사용자 메시지를 직렬화하는 방법을 정의합니다. 특정 타입을 바이트로, 바이트를 다시 타입으로 바꾸는 `encode`와 `decode` 두 메서드로 이루어집니다.

```scala
trait Serialization {
  def encode(message: Any): Task[Array[Byte]]
  def decode[A](bytes: Array[Byte]): Task[A]
}
```

테스트할 때는 Java 직렬화(Java Serialization)를 쓰는 `Serialization.javaSerialization` 레이어를 활용할 수 있습니다(다만 프로덕션에서는 권장하지 않습니다).

Shardcake는 [Kryo](https://github.com/EsotericSoftware/kryo) 바이너리 직렬화 라이브러리로 만든 `Serialization` 구현을 제공합니다. 사용하려면 다음 의존성을 추가하세요.

```scala
libraryDependencies += "com.devsisters" %% "shardcake-serialization-kryo" % "2.7.1"
```

그리고 `KryoSerialization.live` 레이어를 그대로 가져다 쓰면 됩니다.

> **💡 서버 업데이트와 메시지 버전 관리**
>
> - 메시지는 영속화되지 않습니다. 따라서 시스템 전체를 멈췄다가 다시 켠다면 메시지 형식은 얼마든지 바꿔도 됩니다.
> - 반면 롤링 업데이트(다운타임 없이 서버를 조금씩 교체하는 방식)를 한다면 메시지 형식을 바꿀 때 조심해야 합니다.
> - 어디까지 바꿀 수 있는지는 직렬화 방식에 크게 달려 있습니다. 변경을 넉넉히 허용하는 방식이 있는가 하면 매우 빡빡한 방식도 있습니다. [Kryo](https://github.com/EsotericSoftware/kryo)는 기본값이 꽤 엄격해 대부분의 변경을 받아 주지 않지만, 성능이나 메시지 크기를 어느 정도 내주고 변경 폭을 넓히는 설정도 있습니다.
> - 기존 메시지를 손댈 수 없다면, 롤링 업데이트가 끝날 때까지는 쓰이지 않을 새 메시지를 따로 만드는 것도 한 방법입니다. 이렇게 하면 오래된 노드가 새 메시지를 받는 일이 생기지 않습니다.

---

## 4. 헬스(Health)

`PodsHealth` 트레이트는 파드가 아직 살아 있는지, 아니면 죽었는지(죽었다면 그 파드의 샤드를 모두 재할당해야 합니다)를 판단하는 방법을 정의합니다.

```scala
trait PodsHealth {
  def isAlive(podAddress: PodAddress): UIO[Boolean]
}
```

테스트할 때는 항상 true를 반환하는 `PodsHealth.noop` 레이어를 쓰거나, [메시징 프로토콜](#2-메시징-프로토콜messaging-protocol)의 `ping`으로 파드 생존 여부를 확인하는 `PodsHealth.local` 레이어를 쓰면 됩니다.

Shardcake는 [Kubernetes](https://kubernetes.io) API로 만든 `PodsHealth` 구현을 제공합니다. 사용하려면 다음 의존성을 추가하세요.

```scala
libraryDependencies += "com.devsisters" %% "shardcake-health-k8s" % "2.7.1"
```

그리고 `K8sPodsHealth.live` 레이어를 그대로 가져다 쓰면 됩니다. 이 레이어에는 [zio-k8s](https://coralogix.github.io/zio-k8s/docs/overview/overview_gettingstarted)가 제공하는 `Pods` 레이어가 필요합니다.

> **💡 예제**
>
> Redis, gRPC, Kryo 직렬화를 한꺼번에 사용하는 전체 예제는 [examples](https://github.com/devsisters/shardcake/tree/series/2.x/examples/src/main/scala/example/complex) 폴더에서 확인할 수 있습니다.

---

## 5. 참고 자료

- [Shardcake 공식 문서 — Customization](https://devsisters.github.io/shardcake/docs/customization.html)
- [설정(Configuration)](03_configuration.md)
- [redis4cats 문서](https://redis4cats.profunktor.dev/)
- [Kryo 저장소](https://github.com/EsotericSoftware/kryo)
- [zio-k8s 문서](https://coralogix.github.io/zio-k8s/docs/overview/overview_gettingstarted)
