# gRPC 개요

> 원본: https://grpc.io/docs/what-is-grpc/introduction/

---

## 목차

1. [gRPC란](#grpc란)
2. [RPC 동작 방식](#rpc-동작-방식)
3. [Protocol Buffers](#protocol-buffers)
4. [서비스 정의](#서비스-정의)
5. [4가지 RPC 타입](#4가지-rpc-타입)
6. [HTTP/2 기반](#http2-기반)
7. [장점](#장점)
8. [지원 언어](#지원-언어)

---

## gRPC란

gRPC는 구글이 만든 현대적이고 고성능인 오픈 소스 RPC(Remote Procedure Call, 원격 프로시저 호출) 프레임워크입니다. 핵심 아이디어는 클라이언트 애플리케이션이 다른 머신의 서버 애플리케이션에 있는 메서드를, 마치 로컬 객체의 메서드인 것처럼 직접 호출할 수 있게 하는 것입니다. 이를 통해 분산 애플리케이션과 서비스를 더 쉽게 만들 수 있습니다.

gRPC는 서비스(service) 정의를 중심으로 동작합니다. 즉, 호출할 수 있는 메서드와 그 파라미터, 반환 타입을 미리 정의합니다.

- **서버 측**: 정의된 인터페이스를 구현하고 gRPC 서버를 실행해 클라이언트 호출을 처리합니다.
- **클라이언트 측**: 서버와 동일한 메서드를 제공하는 스텁(stub, 일부 언어에서는 client라고 부름)을 가집니다.

gRPC 클라이언트와 서버는 다양한 환경에서 실행될 수 있으며(구글 서버부터 개인 데스크톱까지), gRPC가 지원하는 어떤 언어로도 작성할 수 있습니다. 예를 들어 Java로 작성한 gRPC 서버를 Go, Python, Ruby 클라이언트가 호출할 수 있습니다.

---

## RPC 동작 방식

RPC는 네트워크 통신의 세부 사항을 추상화해, 원격 호출을 로컬 함수 호출처럼 보이게 합니다. gRPC에서의 흐름은 다음과 같습니다.

1. 클라이언트가 로컬 스텁의 메서드를 호출합니다.
2. 스텁이 요청 메시지를 직렬화(serialize)해 네트워크로 전송합니다.
3. 서버가 메시지를 역직렬화(deserialize)하고 실제 구현 메서드를 실행합니다.
4. 서버가 응답을 직렬화해 클라이언트로 되돌려 보냅니다.
5. 클라이언트 스텁이 응답을 역직렬화해 호출자에게 반환합니다.

개발자는 직렬화/역직렬화, 네트워크 전송, 연결 관리 같은 저수준 작업을 신경 쓰지 않고 비즈니스 로직에만 집중할 수 있습니다.

---

## Protocol Buffers

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

## 서비스 정의

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

## 4가지 RPC 타입

gRPC는 요청/응답이 단일인지 스트림(stream)인지에 따라 네 가지 RPC 타입을 지원합니다.

### 단방향(Unary) RPC

클라이언트가 단일 요청을 보내고 단일 응답을 받습니다. 일반 함수 호출과 같습니다.

```proto
rpc SayHello(HelloRequest) returns (HelloReply);
```

### 서버 스트리밍(Server Streaming) RPC

클라이언트가 단일 요청을 보내면 서버가 메시지 스트림을 반환합니다. 클라이언트는 메시지가 더 이상 없을 때까지 스트림을 읽습니다.

```proto
rpc LotsOfReplies(HelloRequest) returns (stream HelloReply);
```

### 클라이언트 스트리밍(Client Streaming) RPC

클라이언트가 메시지 시퀀스를 스트림으로 전송하고, 서버는 모든 메시지를 수신한 뒤 단일 응답을 반환합니다.

```proto
rpc LotsOfGreetings(stream HelloRequest) returns (HelloReply);
```

### 양방향 스트리밍(Bidirectional Streaming) RPC

클라이언트와 서버가 각각 읽기/쓰기 스트림을 독립적으로 갖습니다. 두 스트림은 독립적으로 동작하므로 읽기/쓰기 순서는 애플리케이션이 자유롭게 정합니다.

```proto
rpc BidiHello(stream HelloRequest) returns (stream HelloReply);
```

---

## HTTP/2 기반

gRPC는 전송 계층(transport)으로 HTTP/2를 사용합니다. HTTP/2의 기능 덕분에 다음과 같은 이점을 얻습니다.

- **멀티플렉싱(multiplexing)**: 하나의 TCP 연결에서 여러 RPC를 동시에 처리합니다.
- **스트리밍(streaming)**: HTTP/2 스트림으로 4가지 RPC 타입을 구현합니다.
- **헤더 압축(header compression, HPACK)**: 메타데이터를 효율적으로 전송합니다.
- **바이너리 프레이밍(binary framing)**: 텍스트 기반 HTTP/1.1보다 효율적입니다.

메타데이터는 HTTP/2 헤더로, 메시지는 HTTP/2 데이터 프레임으로 전송됩니다.

---

## 장점

- **고성능**: 바이너리 직렬화(protobuf) + HTTP/2로 낮은 지연 시간과 높은 처리량을 제공합니다.
- **언어 중립성**: 단일 `.proto` 정의로 여러 언어의 클라이언트/서버를 생성합니다.
- **스트리밍 지원**: 단방향 외에 서버/클라이언트/양방향 스트리밍을 기본 지원합니다.
- **강타입 계약(strongly-typed contract)**: `.proto`가 명확한 API 계약 역할을 합니다.
- **코드 생성**: 보일러플레이트 코드를 자동 생성해 휴먼 에러를 줄입니다.
- **부가 기능**: 인증, 타임아웃/데드라인, 재시도, 인터셉터, 헬스 체크 등 분산 시스템에 필요한 기능을 내장합니다.

마이크로서비스 간 통신, 모바일/백엔드 연동, 폴리글랏(polyglot) 환경에 특히 적합합니다.

---

## 지원 언어

proto3는 Java, C++, Dart, Python, Objective-C, C#, Android Java, Ruby, JavaScript, Go를 지원하며, 추가 언어도 개발 중입니다. 이 레퍼런스의 코드 예제는 Go(`google.golang.org/grpc`)를 기준으로 작성됩니다.

---
