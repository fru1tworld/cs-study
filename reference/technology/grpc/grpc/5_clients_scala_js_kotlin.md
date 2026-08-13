# gRPC 클라이언트: Scala, JavaScript/TypeScript, Kotlin

## gRPC Scala 클라이언트 (ScalaPB + ZIO gRPC)

> Scala에서 gRPC 클라이언트를 사용하는 방법을 `RouteGuide` 예제로 정리
> 원본 참고: https://scalapb.github.io/docs/grpc , https://scalapb.github.io/zio-grpc/

---

### 목차

1. [라이브러리 개요](#라이브러리-개요)
2. [코드 생성 설정](#코드-생성-설정)
3. [방식 1: ScalaPB 기본 스텁 (Future 기반)](#방식-1-scalapb-기본-스텁-future-기반)
4. [방식 2: ZIO gRPC (이펙트 기반)](#방식-2-zio-grpc-이펙트-기반)
5. [4가지 RPC 타입 호출 (ZIO gRPC)](#4가지-rpc-타입-호출-zio-grpc)
6. [메타데이터와 에러 처리](#메타데이터와-에러-처리)

---

### 라이브러리 개요

Scala에서 gRPC를 쓰는 방법은 크게 두 가지.

- ScalaPB (`scalapb-runtime-grpc`)
  - 스텁 반환 타입: `scala.concurrent.Future[T]`
  - 특징: grpc-java 위에 얇게 얹은 표준 스텁 · 블로킹/Future/스트림 옵저버 스타일
- ZIO gRPC (`zio-grpc`)
  - 스텁 반환 타입: `zio.IO[StatusException, T]` / `zio.stream.Stream`
  - 특징: ZIO 이펙트로 감싸 자원·취소·에러 채널을 타입으로 표현

둘 다 내부적으로는 grpc-java(`io.grpc.ManagedChannel`)와 ScalaPB가 생성한 메시지 케이스 클래스를 사용 → 차이는 "호출 결과를 무엇으로 돌려주느냐"임.

아래 공통 `.proto`를 기준으로 설명.

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

### 코드 생성 설정

ScalaPB는 sbt 플러그인으로 `protoc`를 구동 → `project/plugins.sbt`에 플러그인을 추가하고, `build.sbt`에서 코드 생성기를 지정.

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

### 방식 1: ScalaPB 기본 스텁 (Future 기반)

grpc-java의 `ManagedChannel`을 직접 만들고, 생성된 `RouteGuideGrpc.stub(channel)`로 스텁을 얻음.

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

> 블로킹 스텁(`blockingStub`)은 단방향/서버 스트리밍에서 결과를 동기로 받음(`Iterator[Feature]`). 간단한 스크립트나 테스트에 유용하지만, 운영 서버에서는 스레드를 점유 → Future/ZIO 스텁 권장.

---

### 방식 2: ZIO gRPC (이펙트 기반)

ZIO gRPC는 호출 결과를 `IO[StatusException, T]`(에러 채널이 `io.grpc.StatusException`인 이펙트)로 돌려줌. 클라이언트는 보통 `ZLayer`로 한 번 만들어 의존성 주입처럼 사용.

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

`RouteGuideClient`는 `RouteGuideClient.getFeature(...)`처럼 동반 객체(accessor)로도, 주입받은 인스턴스로도 호출 가능.

---

### 4가지 RPC 타입 호출 (ZIO gRPC)

```scala
import routeguide.route_guide._
import routeguide.route_guide.ZioRouteGuide.RouteGuideClient
import zio._
import zio.stream._
```

#### 단방향 (Unary)

`IO[Status, Feature]` 하나를 반환.

```scala
val getOne: ZIO[RouteGuideClient, StatusException, Feature] =
  RouteGuideClient.getFeature(Point(409146138, -746188906))

// 사용 예
getOne.flatMap(f => Console.printLine(f.name).orDie)
```

#### 서버 스트리밍 (Server streaming)

응답이 `Stream[Status, Feature]`(ZIO 스트림)로 옴 → `foreach`/`runCollect` 등 스트림 연산으로 소비.

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

요청을 `Stream`으로 흘려보내고, 응답은 단일 `IO[Status, RouteSummary]`로 받음.

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

요청 스트림을 넣으면 응답 스트림이 나옴 → 입력과 출력이 독립적으로 흐름.

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

### 메타데이터와 에러 처리

#### 에러 처리

ZIO gRPC는 에러를 `io.grpc.StatusException`으로 이펙트의 에러 채널에 담음. `mapError`로 상태 코드를 도메인 에러로 변환하는 것이 일반적 → 상태 코드는 `e.getStatus.getCode`로 꺼냄.

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

> 기본 ScalaPB(Future) 스텁에서는 실패가 `StatusRuntimeException`으로 던져지므로, `Future#recover`나 `.transform`으로 `ex.getStatus.getCode`를 검사.

#### 메타데이터 / 데드라인

호출 단위로 헤더(메타데이터)나 타임아웃을 붙일 때는 클라이언트를 변형해 사용. 메타데이터는 `mapMetadataZIO`(`SafeMetadata => UIO[SafeMetadata]` 함수를 받음)로 추가.

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

타임아웃·데드라인은 클라이언트의 `withTimeoutMillis(...)` / `withTimeout(...)` / `withDeadline(...)`로 지정.

---

### 요약

- ScalaPB는 메시지 케이스 클래스 + 표준 스텁(Future/블로킹)을 생성
- ZIO gRPC는 그 위에서 호출 결과를 `IO[StatusException, T]` / `Stream[StatusException, T]`로 감싸, 에러·취소·자원 관리를 ZIO로 일원화
- 채널(`ManagedChannel`)은 비싸므로 앱당 한 번 만들어 재사용 → 스트리밍 4종은 입력/출력이 단일 값이냐 스트림이냐의 조합으로 구분

---

## gRPC JavaScript / TypeScript 클라이언트 (@grpc/grpc-js)

> Node.js에서 gRPC 클라이언트를 사용하는 방법을 `RouteGuide` 예제로 정리
> 원본 참고: https://grpc.io/docs/languages/node/basics/

---

### 목차

1. [라이브러리 개요](#라이브러리-개요)
2. [코드 생성 방식](#코드-생성-방식)
3. [클라이언트(채널) 생성](#클라이언트채널-생성)
4. [4가지 RPC 타입 호출](#4가지-rpc-타입-호출)
5. [콜백을 Promise로 감싸기](#콜백을-promise로-감싸기)
6. [메타데이터·데드라인·에러](#메타데이터데드라인에러)

---

### 라이브러리 개요

- `@grpc/grpc-js`: 순수 JS로 구현된 공식 gRPC 런타임 · Node.js에서 사실상 표준(구 `grpc` 네이티브 모듈 대체)
- `@grpc/proto-loader`: 런타임에 `.proto`를 읽어 동적으로 클라이언트 생성
- `grpc-tools` / `ts-proto`: `.proto`를 미리 정적 코드(JS/TS 스텁)로 생성
- `nice-grpc`, `@pbkit/grpc-client`: `@grpc/grpc-js` 위에 Promise/async 친화적 API를 얹은 래퍼

기본(콜백 스타일) API는 `@grpc/grpc-js`에서 나옴. 래퍼 라이브러리들은 이 콜백을 Promise나 async iterator로 바꿔줄 뿐, 전송 계층은 동일 → 이 문서는 표준인 `@grpc/grpc-js`를 기준으로 설명.

공통 `.proto`:

```proto
syntax = "proto3";
package routeguide;

service RouteGuide {
  rpc GetFeature(Point) returns (Feature) {}
  rpc ListFeatures(Rectangle) returns (stream Feature) {}
  rpc RecordRoute(stream Point) returns (RouteSummary) {}
  rpc RouteChat(stream RouteNote) returns (stream RouteNote) {}
}

message Point { int32 latitude = 1; int32 longitude = 2; }
```

---

### 코드 생성 방식

#### 방식 A: 동적 로딩 (proto-loader)

빌드 단계 없이 런타임에 `.proto`를 파싱 → 빠르게 시작하기 좋음.

```js
const grpc = require('@grpc/grpc-js')
const protoLoader = require('@grpc/proto-loader')

const packageDef = protoLoader.loadSync('routeguide/route_guide.proto', {
  keepCase: true,
  longs: String,
  enums: String,
  defaults: true,
  oneofs: true,
})
const proto = grpc.loadPackageDefinition(packageDef).routeguide
// proto.RouteGuide 가 클라이언트 생성자
```

#### 방식 B: 정적 생성 (권장)

`protoc` + `grpc-tools` 플러그인으로 스텁을 미리 생성.

```bash
protoc \
  --plugin=protoc-gen-grpc=./node_modules/.bin/grpc_tools_node_protoc_plugin \
  --js_out=import_style=commonjs,binary:./generated \
  --grpc_out=grpc_js:./generated \
  routeguide/route_guide.proto
```

생성물의 `route_guide_grpc_pb.js`에 `RouteGuideClient` 생성자가 들어 있음.

> 위 명령은 JavaScript 스텁(`*_pb.js`, `*_grpc_pb.js`)만 만들고 `.d.ts` 타입 정의는 생성하지 않음. TypeScript 타입까지 얻으려면 `grpc_tools_node_protoc_ts`(`--ts_out`)를 함께 쓰거나, `.proto`를 곧장 TS로 컴파일하는 `ts-proto`를 사용.

---

### 클라이언트(채널) 생성

클라이언트 인스턴스가 곧 채널 → 생성 비용이 크므로 앱당 한 번 만들어 재사용.

```js
const grpc = require('@grpc/grpc-js')

const client = new proto.RouteGuide(
  'localhost:8980',
  grpc.credentials.createInsecure(), // 암호화 없는 연결. 운영에서는 createSsl()
)
```

TLS를 쓸 때:

```js
const credentials = grpc.credentials.createSsl(/* rootCerts, privateKey, certChain */)
const client = new proto.RouteGuide('example.com:443', credentials)
```

---

### 4가지 RPC 타입 호출

`@grpc/grpc-js`의 기본 API는 콜백 + 스트림(EventEmitter) 스타일.

#### 단방향 (Unary) — 콜백

```js
client.getFeature(
  { latitude: 409146138, longitude: -746188906 },
  (err, feature) => {
    if (err) return console.error(err)
    console.log(feature.name)
  },
)
```

#### 서버 스트리밍 (Server streaming)

호출이 읽기 가능한 스트림을 반환 → `data` / `end` / `error` 이벤트로 소비.

```js
const call = client.listFeatures({
  lo: { latitude: 400000000, longitude: -750000000 },
  hi: { latitude: 420000000, longitude: -730000000 },
})
call.on('data', (feature) => console.log(feature.name))
call.on('end', () => console.log('완료'))
call.on('error', (err) => console.error(err))
```

#### 클라이언트 스트리밍 (Client streaming)

호출이 쓰기 가능한 스트림을 반환 → `write`로 여러 번 보내고 `end()`로 닫으면 콜백으로 단일 응답이 옴.

```js
const call = client.recordRoute((err, summary) => {
  if (err) return console.error(err)
  console.log(`방문 지점 수: ${summary.point_count}`)
})
;[
  { latitude: 407838351, longitude: -746143763 },
  { latitude: 408122808, longitude: -743999179 },
].forEach((p) => call.write(p))
call.end()
```

#### 양방향 스트리밍 (Bidirectional streaming)

읽기·쓰기가 모두 가능한 스트림 → 입력과 출력이 독립적으로 흐름.

```js
const call = client.routeChat()
call.on('data', (note) => console.log(`받음: ${note.message}`))
call.on('end', () => console.log('서버가 스트림 종료'))

call.write({ message: 'First', location: { latitude: 0, longitude: 1 } })
call.write({ message: 'Second', location: { latitude: 0, longitude: 2 } })
call.end() // 더 보낼 게 없으면 닫기
```

---

### 콜백을 Promise로 감싸기

운영 코드에서는 콜백 대신 `async/await`를 쓰고 싶을 때가 많음. 단방향 호출은 `util.promisify`로 감싸거나 직접 래핑.

```js
const { promisify } = require('util')

// 먼저 client에 바인딩한 뒤 promisify (this 유실 방지)
const getFeature = promisify(client.getFeature.bind(client))

async function main() {
  const feature = await getFeature({ latitude: 409146138, longitude: -746188906 })
  console.log(feature.name)
}
```

서버 스트리밍을 `for await`로 소비하려면 async iterator로 감쌈.

```js
async function* toAsyncIterable(call) {
  const queue = []
  let done = false, resolve
  call.on('data', (d) => { queue.push(d); resolve && resolve() })
  call.on('end', () => { done = true; resolve && resolve() })
  while (!done || queue.length) {
    if (!queue.length) await new Promise((r) => (resolve = r))
    while (queue.length) yield queue.shift()
  }
}

for await (const feature of toAsyncIterable(client.listFeatures(rect))) {
  console.log(feature.name)
}
```

> `nice-grpc`, `@pbkit/grpc-client` 같은 래퍼를 쓰면 단방향은 Promise, 스트리밍은 async iterator로 처음부터 제공되므로 위 보일러플레이트가 불필요.

---

### 메타데이터·데드라인·에러

#### 메타데이터

요청 헤더는 `grpc.Metadata`로 만들어 호출 시 함께 전달.

```js
const metadata = new grpc.Metadata()
metadata.add('authorization', 'Bearer token')

client.getFeature(point, metadata, (err, feature) => { /* ... */ })
```

#### 데드라인(타임아웃)

세 번째 인자 `options`에 절대 시각으로 데드라인을 지정.

```js
const deadline = new Date()
deadline.setSeconds(deadline.getSeconds() + 5) // 5초 후

client.getFeature(point, metadata, { deadline }, (err, feature) => {
  if (err && err.code === grpc.status.DEADLINE_EXCEEDED) {
    console.error('타임아웃')
  }
})
```

#### 에러 코드

에러 객체의 `code`를 `grpc.status` 열거형과 비교.

```js
client.getFeature(point, (err, feature) => {
  if (err) {
    switch (err.code) {
      case grpc.status.NOT_FOUND:         return console.error('없음')
      case grpc.status.UNAVAILABLE:       return console.error('서버 다운')
      case grpc.status.DEADLINE_EXCEEDED: return console.error('타임아웃')
      default:                            return console.error(err.details)
    }
  }
  console.log(feature.name)
})
```

---

### 요약

- Node.js의 표준은 `@grpc/grpc-js`이며, 기본 API는 콜백 + 스트림(EventEmitter) 스타일
- 단방향=콜백, 서버 스트리밍=읽기 스트림, 클라이언트 스트리밍=쓰기 스트림, 양방향=읽기·쓰기 스트림
- 클라이언트 인스턴스(=채널)는 앱당 한 번 만들어 재사용
- 콜백은 `promisify`/async iterator로 감싸거나, `nice-grpc`·`@pbkit/grpc-client` 같은 래퍼로 Promise/async API를 사용

---

## gRPC Kotlin 클라이언트 (grpc-kotlin, 코루틴)

> Kotlin에서 gRPC 클라이언트를 사용하는 방법을 `RouteGuide` 예제로 정리
> 원본 참고: https://grpc.io/docs/languages/kotlin/basics/

---

### 목차

1. [라이브러리 개요](#라이브러리-개요)
2. [코드 생성 설정](#코드-생성-설정)
3. [채널과 스텁 생성](#채널과-스텁-생성)
4. [4가지 RPC 타입 호출](#4가지-rpc-타입-호출)
5. [메타데이터·인터셉터](#메타데이터인터셉터)
6. [데드라인·에러 처리](#데드라인에러-처리)

---

### 라이브러리 개요

Kotlin gRPC는 grpc-java 위에 코루틴 친화적인 스텁을 얹은 것.

- `grpc-kotlin-stub`: `suspend` 함수 / `Flow` 기반 코루틴 스텁(`XxxCoroutineStub`) 생성·런타임
- `grpc-protobuf`: protobuf 메시지 직렬화
- `grpc-netty` (또는 `grpc-netty-shaded`): 전송 계층
- `grpc-stub` / `grpc-core`: grpc-java 기반

핵심 차이는 결과 표현 방식.

- 단방향 / 클라이언트 스트리밍 → `suspend` 함수 (단일 값 반환)
- 서버 스트리밍 / 양방향 스트리밍 → `Flow<T>` 반환

즉 콜백·옵저버 없이 코루틴/Flow로 자연스럽게 호출.

공통 `.proto`:

```proto
syntax = "proto3";
package routeguide;

service RouteGuide {
  rpc GetFeature(Point) returns (Feature) {}
  rpc ListFeatures(Rectangle) returns (stream Feature) {}
  rpc RecordRoute(stream Point) returns (RouteSummary) {}
  rpc RouteChat(stream RouteNote) returns (stream RouteNote) {}
}

message Point { int32 latitude = 1; int32 longitude = 2; }
```

---

### 코드 생성 설정

`protobuf-gradle-plugin`으로 `protoc`를 구동하고, 자바 메시지 + Kotlin 코루틴 스텁을 함께 생성.

```kotlin
// build.gradle.kts
plugins {
    id("com.google.protobuf") version "0.9.4"
}

dependencies {
    implementation("io.grpc:grpc-protobuf:1.68.1")
    implementation("io.grpc:grpc-netty-shaded:1.68.1")
    implementation("io.grpc:grpc-kotlin-stub:1.4.1")
    implementation("com.google.protobuf:protobuf-kotlin:3.25.5")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
}

protobuf {
    protoc { artifact = "com.google.protobuf:protoc:3.25.5" }
    plugins {
        create("grpc")    { artifact = "io.grpc:protoc-gen-grpc-java:1.68.1" }
        create("grpckt")  { artifact = "io.grpc:protoc-gen-grpc-kotlin:1.4.1:jdk8@jar" }
    }
    generateProtoTasks {
        all().forEach {
            it.plugins {
                create("grpc")
                create("grpckt")
            }
            it.builtins { create("kotlin") }
        }
    }
}
```

생성물:

- `Point`, `Feature` … : 자바 메시지 클래스 (+ Kotlin DSL 빌더)
- `RouteGuideGrpc` : grpc-java 기본 스텁
- `RouteGuideGrpcKt` : 코루틴 스텁 `RouteGuideGrpcKt.RouteGuideCoroutineStub`

---

### 채널과 스텁 생성

채널(`ManagedChannel`)은 비용이 크므로 앱당 한 번 만들어 재사용하고, 종료 시 `shutdown`.

```kotlin
import io.grpc.ManagedChannelBuilder
import routeguide.RouteGuideGrpcKt

val channel = ManagedChannelBuilder
    .forAddress("localhost", 8980)
    .usePlaintext()                 // 암호화 없는 연결. 운영에서는 useTransportSecurity()
    .build()

val stub = RouteGuideGrpcKt.RouteGuideCoroutineStub(channel)
```

> Spring 같은 프레임워크에서는 채널·스텁을 빈으로 등록해 주입받는 것이 일반적. `forTarget(url)` + 조건부 `useTransportSecurity()/usePlaintext()` 조합을 자주 사용.

종료:

```kotlin
channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
```

---

### 4가지 RPC 타입 호출

스텁 호출은 코루틴 컨텍스트(`suspend` 함수 안 또는 `runBlocking`/`coroutineScope`)에서 수행.

```kotlin
import kotlinx.coroutines.flow.*
import routeguide.*
```

#### 단방향 (Unary) — suspend

```kotlin
suspend fun getOne(stub: RouteGuideGrpcKt.RouteGuideCoroutineStub) {
    val feature: Feature = stub.getFeature(
        point { latitude = 409146138; longitude = -746188906 }
    )
    println(feature.name)
}
```

#### 서버 스트리밍 (Server streaming) — Flow 반환

응답이 `Flow<Feature>`로 옴 → `collect`로 소비.

```kotlin
suspend fun listAll(stub: RouteGuideGrpcKt.RouteGuideCoroutineStub) {
    val request = rectangle {
        lo = point { latitude = 400000000; longitude = -750000000 }
        hi = point { latitude = 420000000; longitude = -730000000 }
    }
    stub.listFeatures(request).collect { feature ->
        println(feature.name)
    }
}
```

#### 클라이언트 스트리밍 (Client streaming) — Flow 인자, suspend 반환

요청을 `Flow<Point>`로 넘기고, 응답은 단일 값으로 받음.

```kotlin
suspend fun record(stub: RouteGuideGrpcKt.RouteGuideCoroutineStub) {
    val points: Flow<Point> = flowOf(
        point { latitude = 407838351; longitude = -746143763 },
        point { latitude = 408122808; longitude = -743999179 },
    )
    val summary: RouteSummary = stub.recordRoute(points)
    println("방문 지점 수: ${summary.pointCount}")
}
```

#### 양방향 스트리밍 (Bidirectional streaming) — Flow 인자, Flow 반환

입력 `Flow`를 주면 출력 `Flow`가 나옴.

```kotlin
suspend fun chat(stub: RouteGuideGrpcKt.RouteGuideCoroutineStub) {
    val outgoing: Flow<RouteNote> = flow {
        emit(routeNote { message = "First";  location = point { latitude = 0; longitude = 1 } })
        emit(routeNote { message = "Second"; location = point { latitude = 0; longitude = 2 } })
    }
    stub.routeChat(outgoing).collect { note ->
        println("받음: ${note.message}")
    }
}
```

---

### 메타데이터·인터셉터

#### 호출 단위 메타데이터

`io.grpc.Metadata`를 만들어 스텁 호출에 함께 넘김.

```kotlin
import io.grpc.Metadata

val key = Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER)
val md = Metadata().apply { put(key, "Bearer token") }

val feature = stub.getFeature(request, md)   // 코루틴 스텁은 두 번째 인자로 Metadata 수용
```

#### 인터셉터 (공통 헤더 주입)

모든 호출에 공통 헤더를 붙일 때는 `ClientInterceptor`를 채널에 끼움. 인증 토큰·추적 ID 전파에 자주 사용.

```kotlin
val channel = ManagedChannelBuilder
    .forAddress("localhost", 8980)
    .usePlaintext()
    .intercept(headerClientInterceptor)   // 공통 헤더를 주입하는 ClientInterceptor
    .build()
```

---

### 데드라인·에러 처리

#### 데드라인(타임아웃)

스텁에 `withDeadlineAfter`를 적용한 새 스텁으로 호출(스텁은 불변, 변형하면 새 인스턴스 반환).

```kotlin
import java.util.concurrent.TimeUnit

val feature = stub
    .withDeadlineAfter(5, TimeUnit.SECONDS)
    .getFeature(request)
```

#### 에러 처리

실패는 `StatusException`(코루틴 스텁)으로 던져짐 → `status.code`를 검사.

```kotlin
import io.grpc.Status
import io.grpc.StatusException

suspend fun safeGet(stub: RouteGuideGrpcKt.RouteGuideCoroutineStub): Feature? =
    try {
        stub.getFeature(request)
    } catch (e: StatusException) {
        when (e.status.code) {
            Status.Code.NOT_FOUND          -> { println("없음"); null }
            Status.Code.DEADLINE_EXCEEDED  -> { println("타임아웃"); null }
            Status.Code.UNAVAILABLE        -> { println("서버 다운"); null }
            else                           -> throw e
        }
    }
```

> 스트리밍(`Flow`)에서는 수집 도중 예외가 던져지므로 `catch` 연산자나 `try/catch`로 `collect`를 감쌈.

---

### 요약

- Kotlin gRPC는 grpc-java 위의 코루틴 스텁(`XxxCoroutineStub`)을 사용
- 단방향·클라이언트 스트리밍은 `suspend` 함수, 서버·양방향 스트리밍은 `Flow`로 표현 → 콜백·옵저버 없이 코루틴/Flow로 호출
- 채널은 앱당 한 번 만들어 재사용하고, 인증 등 공통 헤더는 `ClientInterceptor`로, 타임아웃은 `withDeadlineAfter`로 설정
- 실패는 `StatusException`으로 던져지며 `status.code`로 분기
