# gRPC Scala 클라이언트 (ScalaPB + ZIO gRPC)

> Scala에서 gRPC 클라이언트를 사용하는 방법을 `RouteGuide` 예제로 정리합니다.
> 원본 참고: https://scalapb.github.io/docs/grpc , https://scalapb.github.io/zio-grpc/

---

## 목차

1. [라이브러리 개요](#라이브러리-개요)
2. [코드 생성 설정](#코드-생성-설정)
3. [방식 1: ScalaPB 기본 스텁 (Future 기반)](#방식-1-scalapb-기본-스텁-future-기반)
4. [방식 2: ZIO gRPC (이펙트 기반)](#방식-2-zio-grpc-이펙트-기반)
5. [4가지 RPC 타입 호출 (ZIO gRPC)](#4가지-rpc-타입-호출-zio-grpc)
6. [메타데이터와 에러 처리](#메타데이터와-에러-처리)

---

## 라이브러리 개요

Scala에서 gRPC를 쓰는 방법은 크게 두 가지입니다.

| 라이브러리 | 스텁 반환 타입 | 특징 |
|---|---|---|
| **ScalaPB (`scalapb-runtime-grpc`)** | `scala.concurrent.Future[T]` | grpc-java 위에 얇게 얹은 표준 스텁. 블로킹/Future/스트림 옵저버 스타일 |
| **ZIO gRPC (`zio-grpc`)** | `zio.IO[StatusException, T]` / `zio.stream.Stream` | ZIO 이펙트로 감싸 자원·취소·에러 채널을 타입으로 표현 |

둘 다 내부적으로는 grpc-java(`io.grpc.ManagedChannel`)와 ScalaPB가 생성한 메시지 케이스 클래스를 사용합니다. 차이는 "호출 결과를 무엇으로 돌려주느냐"입니다.

아래 공통 `.proto`를 기준으로 설명합니다.

```proto
syntax = "proto3";

package routeguide;

service RouteGuide {
  rpc GetFeature(Point) returns (Feature) {}                    // 단방향
  rpc ListFeatures(Rectangle) returns (stream Feature) {}       // 서버 스트리밍
  rpc RecordRoute(stream Point) returns (RouteSummary) {}       // 클라이언트 스트리밍
  rpc RouteChat(stream RouteNote) returns (stream RouteNote) {} // 양방향 스트리밍
}

message Point {
  int32 latitude = 1;
  int32 longitude = 2;
}
```

---

## 코드 생성 설정

ScalaPB는 sbt 플러그인으로 `protoc`를 구동합니다. `project/plugins.sbt`에 플러그인을 추가하고, `build.sbt`에서 코드 생성기를 지정합니다.

```scala
// project/plugins.sbt
addSbtPlugin("com.thesamet" % "sbt-protoc" % "1.0.7")
libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.11.17"
// ZIO gRPC를 쓸 때만 추가
libraryDependencies += "com.thesamet.scalapb.zio-grpc" %% "zio-grpc-codegen" % "0.6.2"
```

```scala
// build.sbt
Compile / PB.targets := Seq(
  // 메시지 케이스 클래스 + grpc=true 로 Future 기반 스텁 생성
  scalapb.gen(grpc = true) -> (Compile / sourceManaged).value / "scalapb",
  // ZIO gRPC 스텁(ZioXxx)을 추가로 생성
  scalapb.zio_grpc.ZioCodeGenerator -> (Compile / sourceManaged).value / "scalapb",
)

libraryDependencies ++= Seq(
  "com.thesamet.scalapb"          %% "scalapb-runtime-grpc" % "0.11.17",
  "com.thesamet.scalapb.zio-grpc" %% "zio-grpc-core"        % "0.6.2",
  "io.grpc"                        % "grpc-netty"           % "1.67.1",
)
```

생성되는 산출물:

- `Point`, `Feature` … : 메시지 케이스 클래스
- `RouteGuideGrpc` : ScalaPB 기본 스텁(`RouteGuideStub` = Future 기반, `RouteGuideBlockingStub` = 블로킹)
- `ZioRouteGuide` : ZIO gRPC 스텁(`RouteGuideClient`)

---

## 방식 1: ScalaPB 기본 스텁 (Future 기반)

grpc-java의 `ManagedChannel`을 직접 만들고, 생성된 `RouteGuideGrpc.stub(channel)`로 스텁을 얻습니다.

```scala
import io.grpc.ManagedChannelBuilder
import routeguide.route_guide.{Point, RouteGuideGrpc}
import scala.concurrent.Future

// 채널은 비용이 크므로 애플리케이션당 한 번 만들어 재사용
val channel = ManagedChannelBuilder
  .forAddress("localhost", 8980)
  .usePlaintext()            // 암호화 없는 연결. 운영에서는 TLS 사용
  .build()

val stub = RouteGuideGrpc.stub(channel)        // Future 기반 비동기 스텁
// val blocking = RouteGuideGrpc.blockingStub(channel)  // 블로킹 스텁

// 단방향 호출: Future[Feature] 반환
val response: Future[Feature] =
  stub.getFeature(Point(latitude = 409146138, longitude = -746188906))

response.foreach(feature => println(feature.name))
```

> 블로킹 스텁(`blockingStub`)은 단방향/서버 스트리밍에서 결과를 동기로 받습니다(`Iterator[Feature]`). 간단한 스크립트나 테스트에 유용하지만, 운영 서버에서는 스레드를 점유하므로 Future/ZIO 스텁이 권장됩니다.

---

## 방식 2: ZIO gRPC (이펙트 기반)

ZIO gRPC는 호출 결과를 `IO[StatusException, T]`(에러 채널이 `io.grpc.StatusException`인 이펙트)로 돌려줍니다. 클라이언트는 보통 `ZLayer`로 한 번 만들어 의존성 주입처럼 사용합니다.

### 클라이언트 레이어 생성

```scala
import io.grpc.ManagedChannelBuilder
import scalapb.zio_grpc.ZManagedChannel
import routeguide.route_guide.ZioRouteGuide.RouteGuideClient
import zio._

// 채널 설정을 ZManagedChannel으로 감싸면 ZIO가 연결 수명을 관리
val clientLayer: Layer[Throwable, RouteGuideClient] =
  RouteGuideClient.live(
    ZManagedChannel(
      ManagedChannelBuilder
        .forAddress("localhost", 8980)
        .usePlaintext()
    )
  )
```

`RouteGuideClient`는 `RouteGuideClient.getFeature(...)`처럼 동반 객체(accessor)로도, 주입받은 인스턴스로도 호출할 수 있습니다.

---

## 4가지 RPC 타입 호출 (ZIO gRPC)

```scala
import routeguide.route_guide._
import routeguide.route_guide.ZioRouteGuide.RouteGuideClient
import zio._
import zio.stream._
```

### 단방향 (Unary)

`IO[Status, Feature]` 하나를 반환합니다.

```scala
val getOne: ZIO[RouteGuideClient, StatusException, Feature] =
  RouteGuideClient.getFeature(Point(409146138, -746188906))

// 사용 예
getOne.flatMap(f => Console.printLine(f.name).orDie)
```

### 서버 스트리밍 (Server streaming)

응답이 `Stream[Status, Feature]`(ZIO 스트림)로 옵니다. `foreach`/`runCollect` 등 스트림 연산으로 소비합니다.

```scala
val rect = Rectangle(
  lo = Some(Point(400000000, -750000000)),
  hi = Some(Point(420000000, -730000000)),
)

val listFeatures: ZIO[RouteGuideClient, StatusException, Unit] =
  RouteGuideClient
    .listFeatures(rect)            // Stream[StatusException, Feature]
    .foreach(f => Console.printLine(f.name).orDie)
```

### 클라이언트 스트리밍 (Client streaming)

요청을 `Stream`으로 흘려보내고, 응답은 단일 `IO[Status, RouteSummary]`로 받습니다.

```scala
val points: Stream[Nothing, Point] = ZStream(
  Point(407838351, -746143763),
  Point(408122808, -743999179),
  Point(413628156, -749015468),
)

val record: ZIO[RouteGuideClient, StatusException, RouteSummary] =
  RouteGuideClient.recordRoute(points)
```

### 양방향 스트리밍 (Bidirectional streaming)

요청 스트림을 넣으면 응답 스트림이 나옵니다. 입력과 출력이 독립적으로 흐릅니다.

```scala
val notes: Stream[Nothing, RouteNote] = ZStream(
  RouteNote(message = "First",  location = Some(Point(0, 1))),
  RouteNote(message = "Second", location = Some(Point(0, 2))),
)

val chat: ZIO[RouteGuideClient, StatusException, Unit] =
  RouteGuideClient
    .routeChat(notes)             // Stream[StatusException, RouteNote]
    .foreach(n => Console.printLine(n.message).orDie)
```

---

## 메타데이터와 에러 처리

### 에러 처리

ZIO gRPC는 에러를 `io.grpc.StatusException`으로 이펙트의 에러 채널에 담습니다. `mapError`로 상태 코드를 도메인 에러로 변환하는 것이 일반적입니다. 상태 코드는 `e.getStatus.getCode`로 꺼냅니다.

```scala
import io.grpc.Status

val safe: ZIO[RouteGuideClient, MyError, Feature] =
  RouteGuideClient
    .getFeature(Point(0, 0))
    .mapError { e =>                          // e: io.grpc.StatusException
      e.getStatus.getCode match {
        case Status.Code.NOT_FOUND          => MyError.NotFound
        case Status.Code.DEADLINE_EXCEEDED  => MyError.Timeout
        case _                              => MyError.Unknown(e.getStatus.getDescription)
      }
    }

sealed trait MyError
object MyError {
  case object NotFound extends MyError
  case object Timeout  extends MyError
  case class  Unknown(msg: String) extends MyError
}
```

> 기본 ScalaPB(Future) 스텁에서는 실패가 `StatusRuntimeException`으로 던져지므로, `Future#recover`나 `.transform`으로 `ex.getStatus.getCode`를 검사합니다.

### 메타데이터 / 데드라인

호출 단위로 헤더(메타데이터)나 타임아웃을 붙일 때는 클라이언트를 변형해 사용합니다. 메타데이터는 `mapMetadataZIO`(`SafeMetadata => UIO[SafeMetadata]` 함수를 받음)로 추가합니다.

```scala
import io.grpc.{Metadata, StatusException}

val AuthKey: Metadata.Key[String] =
  Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER)

// 메타데이터를 실어 호출
val withAuth: ZIO[RouteGuideClient, StatusException, Feature] =
  RouteGuideClient
    .mapMetadataZIO(md => md.put(AuthKey, "Bearer token").as(md))
    .getFeature(Point(0, 0))
```

타임아웃·데드라인은 클라이언트의 `withTimeoutMillis(...)` / `withTimeout(...)` / `withDeadline(...)`로 지정합니다.

---

## 요약

- ScalaPB는 메시지 케이스 클래스 + 표준 스텁(Future/블로킹)을 생성한다.
- ZIO gRPC는 그 위에서 호출 결과를 `IO[StatusException, T]` / `Stream[StatusException, T]`로 감싸, 에러·취소·자원 관리를 ZIO로 일원화한다.
- 채널(`ManagedChannel`)은 비싸므로 앱당 한 번 만들어 재사용하고, 스트리밍 4종은 입력/출력이 단일 값이냐 스트림이냐의 조합으로 구분된다.
