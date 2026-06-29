# gRPC Kotlin 클라이언트 (grpc-kotlin, 코루틴)

> Kotlin에서 gRPC 클라이언트를 사용하는 방법을 `RouteGuide` 예제로 정리합니다.
> 원본 참고: https://grpc.io/docs/languages/kotlin/basics/

---

## 목차

1. [라이브러리 개요](#라이브러리-개요)
2. [코드 생성 설정](#코드-생성-설정)
3. [채널과 스텁 생성](#채널과-스텁-생성)
4. [4가지 RPC 타입 호출](#4가지-rpc-타입-호출)
5. [메타데이터·인터셉터](#메타데이터인터셉터)
6. [데드라인·에러 처리](#데드라인에러-처리)

---

## 라이브러리 개요

Kotlin gRPC는 grpc-java 위에 코루틴 친화적인 스텁을 얹은 것입니다.

| 라이브러리 | 역할 |
|---|---|
| **`grpc-kotlin-stub`** | `suspend` 함수 / `Flow` 기반 코루틴 스텁(`XxxCoroutineStub`) 생성·런타임 |
| **`grpc-protobuf`** | protobuf 메시지 직렬화 |
| **`grpc-netty` (또는 `grpc-netty-shaded`)** | 전송 계층 |
| **`grpc-stub` / `grpc-core`** | grpc-java 기반 |

핵심 차이는 결과 표현 방식입니다.

- 단방향 / 클라이언트 스트리밍 → **`suspend` 함수** (단일 값 반환)
- 서버 스트리밍 / 양방향 스트리밍 → **`Flow<T>`** 반환

즉 콜백·옵저버 없이 코루틴/Flow로 자연스럽게 호출합니다.

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

## 코드 생성 설정

`protobuf-gradle-plugin`으로 `protoc`를 구동하고, 자바 메시지 + Kotlin 코루틴 스텁을 함께 생성합니다.

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
- `RouteGuideGrpcKt` : **코루틴 스텁** `RouteGuideGrpcKt.RouteGuideCoroutineStub`

---

## 채널과 스텁 생성

채널(`ManagedChannel`)은 비용이 크므로 앱당 한 번 만들어 재사용하고, 종료 시 `shutdown`합니다.

```kotlin
import io.grpc.ManagedChannelBuilder
import routeguide.RouteGuideGrpcKt

val channel = ManagedChannelBuilder
    .forAddress("localhost", 8980)
    .usePlaintext()                 // 암호화 없는 연결. 운영에서는 useTransportSecurity()
    .build()

val stub = RouteGuideGrpcKt.RouteGuideCoroutineStub(channel)
```

> Spring 같은 프레임워크에서는 채널·스텁을 빈으로 등록해 주입받는 것이 일반적입니다. `forTarget(url)` + 조건부 `useTransportSecurity()/usePlaintext()` 조합을 자주 씁니다.

종료:

```kotlin
channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
```

---

## 4가지 RPC 타입 호출

스텁 호출은 코루틴 컨텍스트(`suspend` 함수 안 또는 `runBlocking`/`coroutineScope`) 에서 합니다.

```kotlin
import kotlinx.coroutines.flow.*
import routeguide.*
```

### 단방향 (Unary) — suspend

```kotlin
suspend fun getOne(stub: RouteGuideGrpcKt.RouteGuideCoroutineStub) {
    val feature: Feature = stub.getFeature(
        point { latitude = 409146138; longitude = -746188906 }
    )
    println(feature.name)
}
```

### 서버 스트리밍 (Server streaming) — Flow 반환

응답이 `Flow<Feature>`로 옵니다. `collect`로 소비합니다.

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

### 클라이언트 스트리밍 (Client streaming) — Flow 인자, suspend 반환

요청을 `Flow<Point>`로 넘기고, 응답은 단일 값으로 받습니다.

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

### 양방향 스트리밍 (Bidirectional streaming) — Flow 인자, Flow 반환

입력 `Flow`를 주면 출력 `Flow`가 나옵니다.

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

## 메타데이터·인터셉터

### 호출 단위 메타데이터

`io.grpc.Metadata`를 만들어 스텁 호출에 함께 넘깁니다.

```kotlin
import io.grpc.Metadata

val key = Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER)
val md = Metadata().apply { put(key, "Bearer token") }

val feature = stub.getFeature(request, md)   // 코루틴 스텁은 두 번째 인자로 Metadata 수용
```

### 인터셉터 (공통 헤더 주입)

모든 호출에 공통 헤더를 붙일 때는 `ClientInterceptor`를 채널에 끼웁니다. 인증 토큰·추적 ID 전파에 자주 씁니다.

```kotlin
val channel = ManagedChannelBuilder
    .forAddress("localhost", 8980)
    .usePlaintext()
    .intercept(headerClientInterceptor)   // 공통 헤더를 주입하는 ClientInterceptor
    .build()
```

---

## 데드라인·에러 처리

### 데드라인(타임아웃)

스텁에 `withDeadlineAfter`를 적용한 새 스텁으로 호출합니다(스텁은 불변, 변형하면 새 인스턴스 반환).

```kotlin
import java.util.concurrent.TimeUnit

val feature = stub
    .withDeadlineAfter(5, TimeUnit.SECONDS)
    .getFeature(request)
```

### 에러 처리

실패는 `StatusException`(코루틴 스텁) 으로 던져집니다. `status.code`를 검사합니다.

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

> 스트리밍(`Flow`)에서는 수집 도중 예외가 던져지므로 `catch` 연산자나 `try/catch`로 `collect`를 감쌉니다.

---

## 요약

- Kotlin gRPC는 grpc-java 위의 코루틴 스텁(`XxxCoroutineStub`)을 쓴다.
- 단방향·클라이언트 스트리밍은 `suspend` 함수, 서버·양방향 스트리밍은 `Flow`로 표현된다 — 콜백·옵저버 없이 코루틴/Flow로 호출.
- 채널은 앱당 한 번 만들어 재사용하고, 인증 등 공통 헤더는 `ClientInterceptor`로, 타임아웃은 `withDeadlineAfter`로 건다.
- 실패는 `StatusException`으로 던져지며 `status.code`로 분기한다.
