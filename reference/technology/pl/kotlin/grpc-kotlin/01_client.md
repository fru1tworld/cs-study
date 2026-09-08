# gRPC Kotlin 클라이언트 (grpc-kotlin, 코루틴)

> Kotlin에서 gRPC 클라이언트를 사용하는 방법을 `RouteGuide` 예제로 정리

> 원본 참고: https://grpc.io/docs/languages/kotlin/basics/

<a id="라이브러리-개요"></a>
### 라이브러리 개요

Kotlin gRPC는 grpc-java를 기반으로 코루틴에서 호출할 수 있는 스텁을 제공한다. 메시지 직렬화와 전송에는 grpc-java 구성 요소를 사용하고, 호출 결과는 `suspend` 함수와 `Flow`로 다룬다.

- `grpc-kotlin-stub`: `suspend` 함수 / `Flow` 기반 코루틴 스텁(`XxxCoroutineStub`) 생성, 런타임
- `grpc-protobuf`: protobuf 메시지 직렬화
- `grpc-netty` (또는 `grpc-netty-shaded`): 전송 계층
- `grpc-stub` / `grpc-core`: grpc-java 기반

응답이 하나인 단방향과 클라이언트 스트리밍은 `suspend` 함수가 단일 값을 반환한다. 응답이 여러 개인 서버 스트리밍과 양방향 스트리밍은 `Flow<T>`를 반환하므로, 콜백이나 옵저버 대신 Flow 연산으로 응답을 소비한다. 아래의 공통 `.proto`로 이 네 가지 호출을 살펴본다.

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

<a id="코드-생성-설정"></a>

### 코드 생성 설정

- `protobuf-gradle-plugin`으로 `protoc`를 구동하고, 자바 메시지 + Kotlin 코루틴 스텁을 함께 생성

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

- 생성물:

- `Point`, `Feature` … : 자바 메시지 클래스 (+ Kotlin DSL 빌더)
- `RouteGuideGrpc` : grpc-java 기본 스텁
- `RouteGuideGrpcKt` : 코루틴 스텁 `RouteGuideGrpcKt.RouteGuideCoroutineStub`

<a id="채널과-스텁-생성"></a>

### 채널과 스텁 생성

생성한 스텁을 사용하려면 먼저 서버와 통신할 `ManagedChannel`이 필요하다. 채널 생성 비용이 크므로 앱에서 한 번 만들어 재사용하고, 사용을 마치면 `shutdown`으로 종료한다.

```kotlin
import io.grpc.ManagedChannelBuilder
import routeguide.RouteGuideGrpcKt

val channel = ManagedChannelBuilder
    .forAddress("localhost", 8980)
    .usePlaintext()                 // 암호화 없는 연결. 운영에서는 useTransportSecurity()
    .build()

val stub = RouteGuideGrpcKt.RouteGuideCoroutineStub(channel)
```

> Spring 같은 프레임워크에서는 채널과 스텁을 빈으로 등록해 주입받는 것이 일반적. `forTarget(url)` + 조건부 `useTransportSecurity()/usePlaintext()` 조합을 자주 사용.

- 종료:

```kotlin
channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
```

<a id="4가지-rpc-타입-호출"></a>

### 4가지 RPC 타입 호출

- 스텁 호출은 코루틴 컨텍스트(`suspend` 함수 안 또는 `runBlocking`/`coroutineScope`)에서 수행

```kotlin
import kotlinx.coroutines.flow.*
import routeguide.*
```

#### 단방향 (Unary): suspend

```kotlin
suspend fun getOne(stub: RouteGuideGrpcKt.RouteGuideCoroutineStub) {
    val feature: Feature = stub.getFeature(
        point { latitude = 409146138; longitude = -746188906 }
    )
    println(feature.name)
}
```

#### 서버 스트리밍 (Server streaming): Flow 반환

- 응답이 `Flow<Feature>`로 옴 → `collect`로 소비

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

#### 클라이언트 스트리밍 (Client streaming): Flow 인자, suspend 반환

- 요청을 `Flow<Point>`로 넘기고, 응답은 단일 값으로 받음

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

#### 양방향 스트리밍 (Bidirectional streaming): Flow 인자, Flow 반환

- 입력 `Flow`를 주면 출력 `Flow`가 나옴

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

<a id="메타데이터인터셉터"></a>

### 메타데이터, 인터셉터

#### 호출 단위 메타데이터

- `io.grpc.Metadata`를 만들어 스텁 호출에 함께 넘김

```kotlin
import io.grpc.Metadata

val key = Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER)
val md = Metadata().apply { put(key, "Bearer token") }

val feature = stub.getFeature(request, md)   // 코루틴 스텁은 두 번째 인자로 Metadata 수용
```

#### 인터셉터 (공통 헤더 주입)

인증 토큰이나 추적 ID를 모든 호출에 붙여야 한다면 호출마다 메타데이터를 만들기보다 채널에 `ClientInterceptor`를 등록할 수 있다. 아래 채널은 인터셉터를 통해 공통 헤더를 주입한다.

```kotlin
val channel = ManagedChannelBuilder
    .forAddress("localhost", 8980)
    .usePlaintext()
    .intercept(headerClientInterceptor)   // 공통 헤더를 주입하는 ClientInterceptor
    .build()
```

<a id="데드라인에러-처리"></a>

### 데드라인, 에러 처리

#### 데드라인(타임아웃)

스텁은 불변이므로 `withDeadlineAfter`를 호출하면 데드라인이 설정된 새 스텁이 반환된다. 아래 예제는 이 스텁으로 요청해 호출 시간을 5초로 제한한다.

```kotlin
import java.util.concurrent.TimeUnit

val feature = stub
    .withDeadlineAfter(5, TimeUnit.SECONDS)
    .getFeature(request)
```

#### 에러 처리

코루틴 스텁의 호출이 실패하면 `StatusException`이 발생한다. `status.code`를 검사하면 실패 원인에 따라 처리할 수 있다.

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
