# gRPC 개요와 핵심 개념

## gRPC 개요

> 원본: https://grpc.io/docs/what-is-grpc/introduction/

---

### 목차

1. [gRPC란](#grpc란)
2. [RPC 동작 방식](#rpc-동작-방식)
3. [Protocol Buffers](#protocol-buffers)
4. [서비스 정의](#서비스-정의)
5. [4가지 RPC 타입](#4가지-rpc-타입)
6. [HTTP/2 기반](#http2-기반)
7. [장점](#장점)
8. [지원 언어](#지원-언어)

---

### gRPC란

gRPC는 구글이 만든 현대적이고 고성능인 오픈 소스 RPC(Remote Procedure Call, 원격 프로시저 호출) 프레임워크입니다. 핵심 아이디어는 클라이언트 애플리케이션이 다른 머신의 서버 애플리케이션에 있는 메서드를, 마치 로컬 객체의 메서드인 것처럼 직접 호출할 수 있게 하는 것입니다. 이를 통해 분산 애플리케이션과 서비스를 더 쉽게 만들 수 있습니다.

gRPC는 서비스(service) 정의를 중심으로 동작합니다. 즉, 호출할 수 있는 메서드와 그 파라미터, 반환 타입을 미리 정의합니다.

- **서버 측**: 정의된 인터페이스를 구현하고 gRPC 서버를 실행해 클라이언트 호출을 처리합니다.
- **클라이언트 측**: 서버와 동일한 메서드를 제공하는 스텁(stub, 일부 언어에서는 client라고 부름)을 가집니다.

gRPC 클라이언트와 서버는 다양한 환경에서 실행될 수 있으며(구글 서버부터 개인 데스크톱까지), gRPC가 지원하는 어떤 언어로도 작성할 수 있습니다. 예를 들어 Java로 작성한 gRPC 서버를 Go, Python, Ruby 클라이언트가 호출할 수 있습니다.

---

### RPC 동작 방식

RPC는 네트워크 통신의 세부 사항을 추상화해, 원격 호출을 로컬 함수 호출처럼 보이게 합니다. gRPC에서의 흐름은 다음과 같습니다.

1. 클라이언트가 로컬 스텁의 메서드를 호출합니다.
2. 스텁이 요청 메시지를 직렬화(serialize)해 네트워크로 전송합니다.
3. 서버가 메시지를 역직렬화(deserialize)하고 실제 구현 메서드를 실행합니다.
4. 서버가 응답을 직렬화해 클라이언트로 되돌려 보냅니다.
5. 클라이언트 스텁이 응답을 역직렬화해 호출자에게 반환합니다.

개발자는 직렬화/역직렬화, 네트워크 전송, 연결 관리 같은 저수준 작업을 신경 쓰지 않고 비즈니스 로직에만 집중할 수 있습니다.

---

### Protocol Buffers

gRPC는 기본 인터페이스 정의 언어(IDL, Interface Definition Language)이자 기본 메시지 직렬화 포맷으로 Protocol Buffers(프로토콜 버퍼, protobuf)를 사용합니다.

`.proto` 파일에서 직렬화하려는 데이터의 구조를 메시지(message)로 정의합니다. 각 메시지는 이름과 번호(field number)를 가진 필드들의 집합입니다.

```proto
message Person {
  string name = 1;
  int32 id = 2;
  bool has_ponycopter = 3;
}
```

`.proto` 파일을 `protoc` 컴파일러로 컴파일하면, 선택한 언어에 맞는 데이터 접근 클래스(data access class)와 직렬화/역직렬화 코드가 자동 생성됩니다. protobuf는 바이너리 포맷이라 JSON 같은 텍스트 포맷보다 빠르고 작습니다.

gRPC와 함께 사용할 때는 proto3 버전을 사용하는 것이 권장됩니다. 모든 gRPC 지원 언어를 사용할 수 있고 호환성 문제를 피할 수 있기 때문입니다.

---

### 서비스 정의

gRPC 서비스는 `.proto` 파일에서 `service` 키워드로 정의합니다. 각 RPC 메서드는 파라미터(요청 메시지)와 반환 타입(응답 메시지)을 명시합니다.

```proto
service Greeter {
  rpc SayHello (HelloRequest) returns (HelloReply) {}
}

message HelloRequest {
  string name = 1;
}

message HelloReply {
  string message = 1;
}
```

이 정의로부터 서버 인터페이스와 클라이언트 스텁이 생성됩니다.

---

### 4가지 RPC 타입

gRPC는 요청/응답이 단일인지 스트림(stream)인지에 따라 네 가지 RPC 타입을 지원합니다.

#### 단방향(Unary) RPC

클라이언트가 단일 요청을 보내고 단일 응답을 받습니다. 일반 함수 호출과 같습니다.

```proto
rpc SayHello(HelloRequest) returns (HelloReply);
```

#### 서버 스트리밍(Server Streaming) RPC

클라이언트가 단일 요청을 보내면 서버가 메시지 스트림을 반환합니다. 클라이언트는 메시지가 더 이상 없을 때까지 스트림을 읽습니다.

```proto
rpc LotsOfReplies(HelloRequest) returns (stream HelloReply);
```

#### 클라이언트 스트리밍(Client Streaming) RPC

클라이언트가 메시지 시퀀스를 스트림으로 전송하고, 서버는 모든 메시지를 수신한 뒤 단일 응답을 반환합니다.

```proto
rpc LotsOfGreetings(stream HelloRequest) returns (HelloReply);
```

#### 양방향 스트리밍(Bidirectional Streaming) RPC

클라이언트와 서버가 각각 읽기/쓰기 스트림을 독립적으로 갖습니다. 두 스트림은 독립적으로 동작하므로 읽기/쓰기 순서는 애플리케이션이 자유롭게 정합니다.

```proto
rpc BidiHello(stream HelloRequest) returns (stream HelloReply);
```

---

### HTTP/2 기반

gRPC는 전송 계층(transport)으로 HTTP/2를 사용합니다. HTTP/2의 기능 덕분에 다음과 같은 이점을 얻습니다.

- **멀티플렉싱(multiplexing)**: 하나의 TCP 연결에서 여러 RPC를 동시에 처리합니다.
- **스트리밍(streaming)**: HTTP/2 스트림으로 4가지 RPC 타입을 구현합니다.
- **헤더 압축(header compression, HPACK)**: 메타데이터를 효율적으로 전송합니다.
- **바이너리 프레이밍(binary framing)**: 텍스트 기반 HTTP/1.1보다 효율적입니다.

메타데이터는 HTTP/2 헤더로, 메시지는 HTTP/2 데이터 프레임으로 전송됩니다.

---

### 장점

- **고성능**: 바이너리 직렬화(protobuf) + HTTP/2로 낮은 지연 시간과 높은 처리량을 제공합니다.
- **언어 중립성**: 단일 `.proto` 정의로 여러 언어의 클라이언트/서버를 생성합니다.
- **스트리밍 지원**: 단방향 외에 서버/클라이언트/양방향 스트리밍을 기본 지원합니다.
- **강타입 계약(strongly-typed contract)**: `.proto`가 명확한 API 계약 역할을 합니다.
- **코드 생성**: 보일러플레이트 코드를 자동 생성해 휴먼 에러를 줄입니다.
- **부가 기능**: 인증, 타임아웃/데드라인, 재시도, 인터셉터, 헬스 체크 등 분산 시스템에 필요한 기능을 내장합니다.

마이크로서비스 간 통신, 모바일/백엔드 연동, 폴리글랏(polyglot) 환경에 특히 적합합니다.

---

### 지원 언어

proto3는 Java, C++, Dart, Python, Objective-C, C#, Android Java, Ruby, JavaScript, Go를 지원하며, 추가 언어도 개발 중입니다. 이 레퍼런스의 코드 예제는 Go(`google.golang.org/grpc`)를 기준으로 작성됩니다.

---

---

## gRPC 핵심 개념

> 원본: https://grpc.io/docs/what-is-grpc/core-concepts/

---

### 목차

1. [서비스 정의](#서비스-정의)
2. [API 사용: 동기와 비동기](#api-사용-동기와-비동기)
3. [RPC 라이프사이클](#rpc-라이프사이클)
4. [채널(Channel)](#채널channel)
5. [스텁(Stub)](#스텁stub)
6. [데드라인과 타임아웃](#데드라인과-타임아웃)
7. [RPC 취소(Cancellation)](#rpc-취소cancellation)
8. [RPC 종료(Termination)](#rpc-종료termination)
9. [메타데이터(Metadata)](#메타데이터metadata)

---

### 서비스 정의

gRPC는 기본 IDL로 Protocol Buffers를 사용합니다. 서비스 안에 RPC 메서드를 정의하고 각 메서드의 요청/응답 타입을 명시합니다.

```proto
service HelloService {
  rpc SayHello (HelloRequest) returns (HelloResponse);
}

message HelloRequest {
  string greeting = 1;
}

message HelloResponse {
  string reply = 1;
}
```

RPC 메서드는 요청/응답이 단일인지 스트림인지에 따라 4가지로 나뉩니다.

- **단방향(Unary)**: `rpc SayHello(HelloRequest) returns (HelloResponse);`
- **서버 스트리밍**: `rpc LotsOfReplies(HelloRequest) returns (stream HelloResponse);`
- **클라이언트 스트리밍**: `rpc LotsOfGreetings(stream HelloRequest) returns (HelloResponse);`
- **양방향 스트리밍**: `rpc BidiHello(stream HelloRequest) returns (stream HelloResponse);`

`.proto`로부터 클라이언트 스텁과 서버 인터페이스가 생성됩니다.

---

### API 사용: 동기와 비동기

클라이언트에는 서버와 동일한 메서드를 제공하는 로컬 객체인 스텁이 있습니다. 클라이언트는 스텁의 메서드를 호출하고, gRPC가 서버로 요청을 보내 응답을 받아옵니다.

gRPC 프로그래밍 API는 대부분의 언어에서 동기(synchronous)와 비동기(asynchronous) 두 가지 형태를 제공합니다.

- **동기 RPC**: 서버 응답이 올 때까지 블로킹(blocking)합니다. "원격 호출을 로컬 함수처럼" 추상화하는 모델에 가장 잘 들어맞습니다.
- **비동기 RPC**: 블로킹하지 않으며 동시성(concurrency)/확장성이 중요할 때 유용합니다.

Go에서는 일반적으로 동기 호출 형태로 메서드를 호출하며, 동시성은 고루틴(goroutine)으로 처리합니다.

---

### RPC 라이프사이클

#### 단방향 RPC

1. 클라이언트가 스텁 메서드를 호출하면, 서버는 호출에 대한 클라이언트 메타데이터, 메서드 이름, 데드라인을 받습니다.
2. 서버는 즉시 자신의 초기 메타데이터를 보내거나, 클라이언트의 요청 메시지를 기다립니다.
3. 서버는 요청을 받아 응답을 만들고, 상태 코드(status code), 상태 메시지, 선택적 트레일링 메타데이터(trailing metadata)와 함께 반환합니다.
4. 상태가 OK이면 클라이언트는 응답을 받아 호출이 완료됩니다.

#### 서버 스트리밍 RPC

단방향과 비슷하지만, 서버가 상태 정보를 보내기 전에 여러 개의 응답 메시지를 스트림으로 보냅니다. 모든 메시지를 보낸 뒤에 상태 정보(와 트레일링 메타데이터)를 전송하면 완료됩니다.

#### 클라이언트 스트리밍 RPC

클라이언트가 여러 요청 메시지를 스트림으로 보냅니다. 서버는 모든(또는 일부) 메시지를 받은 뒤 단일 응답 메시지와 상태 정보를 보냅니다.

#### 양방향 스트리밍 RPC

클라이언트가 메서드를 호출하면 양쪽이 각자의 메시지 스트림을 독립적으로 운용합니다. 두 스트림은 서로 독립적이므로, 클라이언트와 서버는 임의의 순서로 읽고 쓸 수 있습니다(예: 서버가 모든 요청을 받은 뒤 응답하거나, 핑퐁식으로 주고받을 수도 있음).

---

### 채널(Channel)

gRPC 채널(channel)은 지정된 호스트와 포트의 gRPC 서버로의 연결을 제공합니다. 클라이언트 스텁을 만들 때 채널을 사용합니다.

채널은 상태(state)를 가집니다(`connected`, `idle` 등). 하나의 채널은 내부적으로 여러 HTTP/2 연결을 관리할 수 있으며, 채널을 통해 채널 인자(channel arguments)로 동작을 설정할 수 있습니다(예: 압축 활성화/비활성화).

채널 생성은 비용이 있으므로, RPC마다 새로 만들지 말고 재사용하는 것이 권장됩니다.

---

### 스텁(Stub)

스텁(stub)은 클라이언트가 가지는 로컬 객체로, 서버의 서비스 메서드와 동일한 메서드를 노출합니다. 클라이언트는 채널 위에 스텁을 생성한 뒤, 스텁 메서드를 호출하기만 하면 됩니다. 직렬화, 전송, 응답 수신은 gRPC가 처리합니다.

---

### 데드라인과 타임아웃

gRPC는 클라이언트가 RPC 완료를 얼마나 기다릴지 지정할 수 있게 합니다. 시간이 지나면 RPC는 `DEADLINE_EXCEEDED` 에러로 종료됩니다.

- **데드라인(deadline)**: "이 시점 이후로는 응답을 기다리지 않겠다"는 절대 시각.
- **타임아웃(timeout)**: 호출이 완료될 때까지 허용하는 최대 기간. 호출 시작 시 현재 시각에 더해 데드라인으로 변환됩니다.

서버는 데드라인이 지났는지, 또는 남은 시간이 얼마인지 확인할 수 있습니다. 기본값은 언어마다 다르며 일부는 데드라인이 없습니다. 따라서 클라이언트는 항상 현실적인 데드라인을 명시하는 것이 좋습니다. (자세한 Go 예제는 `08_error_handling_deadlines.md` 참조)

---

### RPC 취소(Cancellation)

클라이언트나 서버는 언제든지 RPC를 취소할 수 있습니다. 취소하면 RPC가 즉시 종료되어 더 이상 작업이 진행되지 않습니다.

중요한 점은 **취소 이전에 이루어진 변경은 롤백되지 않는다**는 것입니다. 또한 gRPC 라이브러리는 일반적으로 애플리케이션이 제공한 서버 핸들러를 강제로 중단시키지 못합니다. 따라서 장시간 실행되는 핸들러는 주기적으로 RPC가 취소되었는지 확인하고, 취소되었으면 스스로 처리를 멈춰야 합니다. (Go에서는 `ctx.Done()` / `ctx.Err()`로 확인)

---

### RPC 종료(Termination)

gRPC에서 클라이언트와 서버는 호출의 성공 여부를 각자 독립적이고 로컬하게 판단하며, 그 결론이 서로 다를 수 있습니다. 예를 들어 서버는 모든 응답을 성공적으로 보냈다고 판단했지만(서버 입장에서 OK), 클라이언트는 데드라인 이후에 응답이 도착해 실패(`DEADLINE_EXCEEDED`)로 처리할 수 있습니다.

---

### 메타데이터(Metadata)

메타데이터(metadata)는 특정 RPC 호출에 관한 정보를 키-값(key-value) 쌍으로 담는 데이터입니다(예: 인증 토큰). RPC가 처리하는 실제 메시지와는 별개의 부가 채널(side channel)입니다.

- 키는 대소문자를 구분하지 않으며(case insensitive), ASCII 문자, 숫자, 그리고 특수문자 `-`, `_`, `.`로 구성됩니다.
- 키는 `grpc-` 접두사로 시작할 수 없습니다. 이 접두사는 gRPC 자체가 예약해 사용합니다.
- 값은 ASCII 문자열 또는 바이너리 데이터일 수 있습니다(바이너리 키는 `-bin` 접미사를 붙임).

메타데이터에 접근하는 방식은 언어마다 다릅니다. (Go 예제는 `06_metadata_interceptors.md` 참조)

---
