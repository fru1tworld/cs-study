# gRPC Scala 클라이언트 (ScalaPB + ZIO gRPC)

Scala에서 gRPC 클라이언트를 사용하는 방법을 `RouteGuide` 예제로 살펴본다.

> 원본 참고: https://scalapb.github.io/docs/grpc , https://scalapb.github.io/zio-grpc/

<a id="라이브러리-개요"></a>
### 라이브러리 개요

Scala에서 gRPC를 호출할 때는 결과를 `Future`로 받을지, ZIO 이펙트로 다룰지에 따라 다음 두 방식을 선택할 수 있다.

- ScalaPB (`scalapb-runtime-grpc`)
  - 스텁 반환 타입: `scala.concurrent.Future[T]`
  - 특징: grpc-java 위에 얇게 얹은 표준 스텁, 블로킹/Future/스트림 옵저버 스타일
- ZIO gRPC (`zio-grpc`)
  - 스텁 반환 타입: `zio.IO[StatusException, T]` / `zio.stream.Stream`
  - 특징: ZIO 이펙트로 감싸 자원, 취소, 에러 채널을 타입으로 표현

두 방식 모두 내부적으로 grpc-java의 `io.grpc.ManagedChannel`과 ScalaPB가 생성한 메시지 케이스 클래스를 사용한다. 공통 기반 위에서 호출 결과를 다루는 방식이 어떻게 달라지는지, 아래 `RouteGuide`의 네 가지 RPC를 기준으로 살펴본다.

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

<a id="코드-생성-설정"></a>

### 코드 생성 설정

먼저 `.proto`에서 메시지와 스텁을 생성해야 한다. ScalaPB는 sbt 플러그인으로 `protoc`를 실행하므로, `project/plugins.sbt`에 플러그인을 추가한 뒤 `build.sbt`에서 사용할 코드 생성기를 지정한다.

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

이 설정으로 다음 메시지 클래스와 스텁이 생성된다.

- `Point`, `Feature` … : 메시지 케이스 클래스
- `RouteGuideGrpc` : ScalaPB 기본 스텁(`RouteGuideStub` = Future 기반, `RouteGuideBlockingStub` = 블로킹)
- `ZioRouteGuide` : ZIO gRPC 스텁(`RouteGuideClient`)

<a id="방식-1-scalapb-기본-스텁-future-기반"></a>

### 방식 1: ScalaPB 기본 스텁 (Future 기반)

grpc-java의 `ManagedChannel`을 만들고 `RouteGuideGrpc.stub(channel)`에 넘기면 Future 기반 스텁을 얻는다. 채널은 생성 비용이 크므로 애플리케이션에서 한 번 만들어 재사용한다.

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

블로킹 스텁(`blockingStub`)은 단방향과 서버 스트리밍 호출의 결과를 동기로 받는다. 서버 스트리밍 결과는 `Iterator[Feature]`로 받는다. 간단한 스크립트나 테스트에는 유용하지만, 호출 중 스레드를 점유하므로 운영 서버에서는 Future나 ZIO 스텁을 권장한다.

<a id="방식-2-zio-grpc-이펙트-기반"></a>

### 방식 2: ZIO gRPC (이펙트 기반)

ZIO gRPC는 호출 결과를 `IO[StatusException, T]`로 반환한다. 성공 값은 `T`로, 실패는 `io.grpc.StatusException`인 에러 채널로 다룬다. 클라이언트는 보통 `ZLayer`로 한 번 구성해 필요한 코드에 주입한다.

#### 클라이언트 레이어 생성

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

이렇게 구성한 `RouteGuideClient`는 `RouteGuideClient.getFeature(...)`처럼 동반 객체(accessor)로 호출하거나, 주입받은 인스턴스로 호출할 수 있다.

<a id="4가지-rpc-타입-호출-zio-grpc"></a>

### 4가지 RPC 타입 호출 (ZIO gRPC)

```scala
import routeguide.route_guide._
import routeguide.route_guide.ZioRouteGuide.RouteGuideClient
import zio._
import zio.stream._
```

#### 단방향 (Unary)

단방향 호출은 요청 하나에 `IO[Status, Feature]` 하나를 반환한다. 아래 예제에서는 `getFeature`의 결과를 이펙트로 받아 출력한다.

```scala
val getOne: ZIO[RouteGuideClient, StatusException, Feature] =
  RouteGuideClient.getFeature(Point(409146138, -746188906))

// 사용 예
getOne.flatMap(f => Console.printLine(f.name).orDie)
```

#### 서버 스트리밍 (Server streaming)

서버 스트리밍은 응답을 `Stream[Status, Feature]`인 ZIO 스트림으로 반환한다. 따라서 `foreach`나 `runCollect` 같은 스트림 연산으로 각 응답을 소비한다.

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

#### 클라이언트 스트리밍 (Client streaming)

클라이언트 스트리밍은 요청을 `Stream`으로 보내고 응답은 단일 `IO[Status, RouteSummary]`로 받는다. 아래에서는 여러 `Point`를 보내고 경로 전체의 `RouteSummary`를 받는다.

```scala
val points: Stream[Nothing, Point] = ZStream(
  Point(407838351, -746143763),
  Point(408122808, -743999179),
  Point(413628156, -749015468),
)

val record: ZIO[RouteGuideClient, StatusException, RouteSummary] =
  RouteGuideClient.recordRoute(points)
```

#### 양방향 스트리밍 (Bidirectional streaming)

양방향 스트리밍은 요청과 응답을 모두 스트림으로 다룬다. 입력과 출력이 독립적으로 흐르므로, 요청 스트림을 넘긴 뒤 응답 스트림을 소비한다.

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

<a id="메타데이터와-에러-처리"></a>

### 메타데이터와 에러 처리

#### 에러 처리

호출 실패는 이펙트의 에러 채널에 `io.grpc.StatusException`으로 들어온다. 이를 애플리케이션에서 사용하는 에러로 바꾸려면 `mapError` 안에서 `e.getStatus.getCode`를 확인한다. 아래 예시는 리소스 없음과 시간 초과를 각각 도메인 에러로 변환한다.

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

기본 ScalaPB(Future) 스텁에서는 실패가 `StatusRuntimeException`으로 전달된다. 이 경우 `Future#recover`나 `.transform`에서 `ex.getStatus.getCode`를 검사한다.

#### 메타데이터 / 데드라인

호출 단위로 헤더(메타데이터)나 타임아웃을 붙이려면 클라이언트를 변형해 사용한다. 메타데이터를 추가하는 `mapMetadataZIO`는 `SafeMetadata => UIO[SafeMetadata]` 함수를 받는다.

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

타임아웃과 데드라인은 클라이언트의 `withTimeoutMillis(...)`, `withTimeout(...)`, `withDeadline(...)`로 지정한다.
