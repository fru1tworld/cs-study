# gRPC 핵심 개념

> 원본: https://grpc.io/docs/what-is-grpc/core-concepts/

---

## 목차

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

## 서비스 정의

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

## API 사용: 동기와 비동기

클라이언트에는 서버와 동일한 메서드를 제공하는 로컬 객체인 스텁이 있습니다. 클라이언트는 스텁의 메서드를 호출하고, gRPC가 서버로 요청을 보내 응답을 받아옵니다.

gRPC 프로그래밍 API는 대부분의 언어에서 동기(synchronous)와 비동기(asynchronous) 두 가지 형태를 제공합니다.

- **동기 RPC**: 서버 응답이 올 때까지 블로킹(blocking)합니다. "원격 호출을 로컬 함수처럼" 추상화하는 모델에 가장 잘 들어맞습니다.
- **비동기 RPC**: 블로킹하지 않으며 동시성(concurrency)/확장성이 중요할 때 유용합니다.

Go에서는 일반적으로 동기 호출 형태로 메서드를 호출하며, 동시성은 고루틴(goroutine)으로 처리합니다.

---

## RPC 라이프사이클

### 단방향 RPC

1. 클라이언트가 스텁 메서드를 호출하면, 서버는 호출에 대한 클라이언트 메타데이터, 메서드 이름, 데드라인을 받습니다.
2. 서버는 즉시 자신의 초기 메타데이터를 보내거나, 클라이언트의 요청 메시지를 기다립니다.
3. 서버는 요청을 받아 응답을 만들고, 상태 코드(status code), 상태 메시지, 선택적 트레일링 메타데이터(trailing metadata)와 함께 반환합니다.
4. 상태가 OK이면 클라이언트는 응답을 받아 호출이 완료됩니다.

### 서버 스트리밍 RPC

단방향과 비슷하지만, 서버가 상태 정보를 보내기 전에 여러 개의 응답 메시지를 스트림으로 보냅니다. 모든 메시지를 보낸 뒤에 상태 정보(와 트레일링 메타데이터)를 전송하면 완료됩니다.

### 클라이언트 스트리밍 RPC

클라이언트가 여러 요청 메시지를 스트림으로 보냅니다. 서버는 모든(또는 일부) 메시지를 받은 뒤 단일 응답 메시지와 상태 정보를 보냅니다.

### 양방향 스트리밍 RPC

클라이언트가 메서드를 호출하면 양쪽이 각자의 메시지 스트림을 독립적으로 운용합니다. 두 스트림은 서로 독립적이므로, 클라이언트와 서버는 임의의 순서로 읽고 쓸 수 있습니다(예: 서버가 모든 요청을 받은 뒤 응답하거나, 핑퐁식으로 주고받을 수도 있음).

---

## 채널(Channel)

gRPC 채널(channel)은 지정된 호스트와 포트의 gRPC 서버로의 연결을 제공합니다. 클라이언트 스텁을 만들 때 채널을 사용합니다.

채널은 상태(state)를 가집니다(`connected`, `idle` 등). 하나의 채널은 내부적으로 여러 HTTP/2 연결을 관리할 수 있으며, 채널을 통해 채널 인자(channel arguments)로 동작을 설정할 수 있습니다(예: 압축 활성화/비활성화).

채널 생성은 비용이 있으므로, RPC마다 새로 만들지 말고 재사용하는 것이 권장됩니다.

---

## 스텁(Stub)

스텁(stub)은 클라이언트가 가지는 로컬 객체로, 서버의 서비스 메서드와 동일한 메서드를 노출합니다. 클라이언트는 채널 위에 스텁을 생성한 뒤, 스텁 메서드를 호출하기만 하면 됩니다. 직렬화, 전송, 응답 수신은 gRPC가 처리합니다.

---

## 데드라인과 타임아웃

gRPC는 클라이언트가 RPC 완료를 얼마나 기다릴지 지정할 수 있게 합니다. 시간이 지나면 RPC는 `DEADLINE_EXCEEDED` 에러로 종료됩니다.

- **데드라인(deadline)**: "이 시점 이후로는 응답을 기다리지 않겠다"는 절대 시각.
- **타임아웃(timeout)**: 호출이 완료될 때까지 허용하는 최대 기간. 호출 시작 시 현재 시각에 더해 데드라인으로 변환됩니다.

서버는 데드라인이 지났는지, 또는 남은 시간이 얼마인지 확인할 수 있습니다. 기본값은 언어마다 다르며 일부는 데드라인이 없습니다. 따라서 클라이언트는 항상 현실적인 데드라인을 명시하는 것이 좋습니다. (자세한 Go 예제는 `08_error_handling_deadlines.md` 참조)

---

## RPC 취소(Cancellation)

클라이언트나 서버는 언제든지 RPC를 취소할 수 있습니다. 취소하면 RPC가 즉시 종료되어 더 이상 작업이 진행되지 않습니다.

중요한 점은 **취소 이전에 이루어진 변경은 롤백되지 않는다**는 것입니다. 또한 gRPC 라이브러리는 일반적으로 애플리케이션이 제공한 서버 핸들러를 강제로 중단시키지 못합니다. 따라서 장시간 실행되는 핸들러는 주기적으로 RPC가 취소되었는지 확인하고, 취소되었으면 스스로 처리를 멈춰야 합니다. (Go에서는 `ctx.Done()` / `ctx.Err()`로 확인)

---

## RPC 종료(Termination)

gRPC에서 클라이언트와 서버는 호출의 성공 여부를 각자 독립적이고 로컬하게 판단하며, 그 결론이 서로 다를 수 있습니다. 예를 들어 서버는 모든 응답을 성공적으로 보냈다고 판단했지만(서버 입장에서 OK), 클라이언트는 데드라인 이후에 응답이 도착해 실패(`DEADLINE_EXCEEDED`)로 처리할 수 있습니다.

---

## 메타데이터(Metadata)

메타데이터(metadata)는 특정 RPC 호출에 관한 정보를 키-값(key-value) 쌍으로 담는 데이터입니다(예: 인증 토큰). RPC가 처리하는 실제 메시지와는 별개의 부가 채널(side channel)입니다.

- 키는 대소문자를 구분하지 않으며(case insensitive), ASCII 문자, 숫자, 그리고 특수문자 `-`, `_`, `.`로 구성됩니다.
- 키는 `grpc-` 접두사로 시작할 수 없습니다. 이 접두사는 gRPC 자체가 예약해 사용합니다.
- 값은 ASCII 문자열 또는 바이너리 데이터일 수 있습니다(바이너리 키는 `-bin` 접미사를 붙임).

메타데이터에 접근하는 방식은 언어마다 다릅니다. (Go 예제는 `06_metadata_interceptors.md` 참조)

---
