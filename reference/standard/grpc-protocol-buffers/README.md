# gRPC / Protocol Buffers

## 목차

1. [개요](#1-개요)
2. [역사](#2-역사)
3. [Protocol Buffers 상세](#3-protocol-buffers-상세)
4. [gRPC 상세](#4-grpc-상세)
5. [코드 생성 (protoc 컴파일러)](#5-코드-생성-protoc-컴파일러)
6. [언어별 지원 현황](#6-언어별-지원-현황)
7. [gRPC vs REST vs GraphQL 비교](#7-grpc-vs-rest-vs-graphql-비교)
8. [장점과 단점](#8-장점과-단점)
9. [실제 예시](#9-실제-예시)
10. [활용 사례](#10-활용-사례)

---

## 1. 개요

### 1.1 gRPC란

gRPC(gRPC Remote Procedure Call)는 Google이 개발한 고성능, 오픈소스 원격 프로시저 호출(RPC) 프레임워크다. 클라이언트 애플리케이션이 마치 로컬 메서드를 호출하듯이 다른 머신에 있는 서버 애플리케이션의 메서드를 직접 호출할 수 있게 해준다. HTTP/2를 전송 프로토콜로 사용하며, Protocol Buffers를 기본 직렬화 포맷으로 채택하고 있다.

gRPC의 "g"는 버전마다 의미가 달라지는 것으로 알려져 있으며(good, green, groovy 등), 공식적으로 고정된 의미는 없다.

### 1.2 Protocol Buffers란

Protocol Buffers(줄여서 Protobuf)는 Google이 개발한 언어 중립적이고 플랫폼 중립적인 구조화된 데이터 직렬화 메커니즘이다. XML이나 JSON과 유사한 역할을 하지만, 바이너리 형식으로 인코딩되어 훨씬 작고, 빠르며, 단순하다.

`.proto` 파일에 데이터 구조를 정의하면, `protoc` 컴파일러가 다양한 프로그래밍 언어로 직렬화/역직렬화 코드를 자동 생성해준다.

### 1.3 gRPC와 Protocol Buffers의 관계

```
+------------------+       +---------------------+
|                  |       |                     |
|  gRPC Framework  | ----> | Protocol Buffers    |
|  (전송 + RPC)     |       | (직렬화 + IDL)       |
|                  |       |                     |
+------------------+       +---------------------+
        |                           |
        v                           v
   HTTP/2 기반 통신            .proto 파일로
   4가지 스트리밍 패턴          서비스 및 메시지 정의
   인터셉터, 메타데이터         바이너리 인코딩
```

- Protocol Buffers는 IDL(Interface Definition Language) 겸 직렬화 포맷의 역할을 한다.
- gRPC는 Protocol Buffers로 정의된 서비스 인터페이스를 기반으로 RPC 통신 프레임워크를 제공한다.
- gRPC는 Protocol Buffers 없이도 사용할 수 있다(예: JSON, FlatBuffers 등). 그러나 기본이자 권장 조합은 gRPC + Protocol Buffers다.
- Protocol Buffers는 gRPC 없이도 단독으로 데이터 직렬화 용도로 널리 사용된다.

---

## 2. 역사

### 2.1 Google 내부의 Stubby에서 gRPC로

| 시기 | 사건 |
|------|------|
| 2001년경 | Google 내부에서 Stubby라는 단일 범용 RPC 인프라를 개발. Google의 모든 마이크로서비스 간 통신에 사용됨. 초당 수십억 건의 요청을 처리. |
| 2004-2014 | Stubby가 Google 내부에서 10년 이상 핵심 인프라로 운영됨. 그러나 Stubby는 Google의 내부 인프라에 강하게 결합되어 있어 외부 공개가 어려웠음. |
| 2015년 2월 | Google이 Stubby의 차세대 버전을 오픈소스로 공개하겠다고 발표. 이것이 gRPC. |
| 2015년 3월 | gRPC가 공식적으로 GitHub에 오픈소스로 공개됨. |
| 2016년 8월 | gRPC 1.0 정식 릴리스. |
| 2017년 3월 | gRPC가 CNCF(Cloud Native Computing Foundation) 인큐베이팅 프로젝트로 합류. |
| 2019년 4월 | CNCF 졸업(Graduated) 프로젝트로 승격. Kubernetes, Prometheus, Envoy 등과 동급. |

### 2.2 Protocol Buffers의 탄생

| 시기 | 사건 |
|------|------|
| 2001년경 | Google 내부에서 인덱스 서버의 요청/응답 프로토콜을 위해 Protocol Buffers 초기 버전 개발. 기존에는 수동으로 요청을 마샬링/언마샬링하고 있었는데, 이 방식이 버전 관리에 심각한 문제를 일으켰음. |
| 2001-2008 | `proto1` 형식이 Google 내부에서 발전. 수많은 내부 서비스에서 데이터 교환 포맷으로 채택됨. |
| 2008년 7월 | Google이 Protocol Buffers를 proto2 형식으로 오픈소스 공개. |
| 2014년 | 문법을 단순화하고 새로운 언어 지원을 강화한 proto3 발표. |
| 2023년 | Protocol Buffers Editions 도입. proto2/proto3의 구분 대신 에디션 기반 기능 선택 모델로 전환 시작. |

---

## 3. Protocol Buffers 상세

### 3.1 .proto 파일 문법

`.proto` 파일은 Protocol Buffers의 스키마 정의 파일이다. 이 파일에 메시지 구조와 서비스 인터페이스를 선언한다.

#### 3.1.1 syntax 선언

모든 `.proto` 파일은 최상단에 사용할 프로토콜 버전을 선언해야 한다.

```protobuf
syntax = "proto3";
```

#### 3.1.2 package

네임스페이스 충돌을 방지하기 위한 패키지 선언이다.

```protobuf
package mycompany.myproject.v1;
```

#### 3.1.3 import

다른 `.proto` 파일의 정의를 가져올 수 있다.

```protobuf
import "google/protobuf/timestamp.proto";
import "google/protobuf/any.proto";
import public "other_protos.proto";  // 전이적(transitive) import
```

#### 3.1.4 message

Protocol Buffers의 핵심 구조체. 필드들의 집합으로 구성된다.

```protobuf
message Person {
  string name = 1;          // 필드 번호 1
  int32 id = 2;             // 필드 번호 2
  string email = 3;         // 필드 번호 3
}
```

각 필드는 타입, 이름, 고유 필드 번호로 구성된다. 필드 번호는 바이너리 인코딩에서 해당 필드를 식별하는 데 사용되며, 한번 사용되면 변경하면 안 된다.

- 필드 번호 1~15: 1바이트로 인코딩됨 (자주 사용하는 필드에 할당 권장)
- 필드 번호 16~2047: 2바이트로 인코딩됨
- 필드 번호 범위: 1 ~ 2^29 - 1 (536,870,911), 단 19000~19999는 예약됨

#### 3.1.5 field 규칙 (proto3)

```protobuf
message Example {
  string singular_field = 1;           // 단수 필드 (0 또는 1개)
  optional string opt_field = 2;       // 명시적 optional (존재 여부 추적)
  repeated string list_field = 3;      // 반복 필드 (0개 이상, 배열)
}
```

- singular (기본): 0 또는 1개의 값을 가질 수 있다. proto3에서는 기본값이면 직렬화에서 생략된다.
- optional: singular와 동일하지만, `has_` 메서드를 통해 필드가 명시적으로 설정되었는지 확인 가능하다.
- repeated: 순서가 유지되는 동적 크기의 배열이다. 0개 이상의 값을 가진다.

#### 3.1.6 enum

```protobuf
enum Status {
  STATUS_UNSPECIFIED = 0;   // proto3에서 첫 번째 값은 반드시 0이어야 함
  STATUS_ACTIVE = 1;
  STATUS_INACTIVE = 2;
  STATUS_SUSPENDED = 3;
}

// 별칭(alias) 허용
enum PhoneType {
  option allow_alias = true;
  PHONE_TYPE_UNSPECIFIED = 0;
  PHONE_TYPE_MOBILE = 1;
  PHONE_TYPE_CELL = 1;      // MOBILE과 같은 값
  PHONE_TYPE_LANDLINE = 2;
}
```

proto3에서 enum의 첫 번째 값은 반드시 0이어야 하며, 이는 기본값으로 사용된다. 이름 충돌을 방지하기 위해 enum 값에 접두사를 붙이는 것이 관례다.

#### 3.1.7 oneof

여러 필드 중 최대 하나만 설정될 수 있는 필드 그룹이다.

```protobuf
message Profile {
  string name = 1;

  oneof avatar {
    string image_url = 2;
    bytes image_data = 3;
  }

  oneof contact {
    string email = 4;
    string phone = 5;
  }
}
```

- `oneof` 내의 필드 중 하나를 설정하면 나머지는 자동으로 클리어된다.
- `oneof` 안에는 `repeated` 필드를 넣을 수 없다.
- `oneof` 안에는 `map` 필드를 넣을 수 없다.

#### 3.1.8 map

키-값 쌍의 연관 맵이다.

```protobuf
message Project {
  string name = 1;
  map<string, string> labels = 2;         // string -> string
  map<int32, Feature> features = 3;       // int32 -> Feature 메시지
}
```

- 키 타입: 정수형 또는 string만 가능 (float, double, bytes, enum, message는 불가)
- 값 타입: map을 제외한 모든 타입 가능
- map 필드는 `repeated`일 수 없다.
- 순서가 보장되지 않는다.

#### 3.1.9 중첩 타입

```protobuf
message Outer {
  message Inner {
    string value = 1;
  }

  Inner inner = 1;

  enum Color {
    COLOR_UNSPECIFIED = 0;
    COLOR_RED = 1;
    COLOR_BLUE = 2;
  }

  Color color = 2;
}

// 외부에서 참조할 때
message Other {
  Outer.Inner inner = 1;
  Outer.Color color = 2;
}
```

#### 3.1.10 reserved

이전에 사용했지만 더 이상 사용하지 않는 필드 번호나 이름을 예약하여 재사용을 방지한다.

```protobuf
message Foo {
  reserved 2, 15, 9 to 11;
  reserved "foo", "bar";
  // 필드 번호와 이름을 같은 reserved 문에서 혼용할 수 없다.
}
```

#### 3.1.11 service (gRPC 서비스 정의)

```protobuf
service Greeter {
  rpc SayHello (HelloRequest) returns (HelloReply);
  rpc SayHelloStream (HelloRequest) returns (stream HelloReply);
}
```

### 3.2 스칼라 타입

Protocol Buffers는 다양한 스칼라 타입을 지원하며, 각 타입은 프로그래밍 언어별로 매핑된다.

| Proto 타입 | 설명 | C++ | Java | Python | Go | 기본값 |
|-----------|------|-----|------|--------|----|-------|
| `double` | 64비트 부동소수점 | double | double | float | float64 | 0 |
| `float` | 32비트 부동소수점 | float | float | float | float32 | 0 |
| `int32` | 가변 길이 인코딩. 음수에 비효율적 | int32 | int | int | int32 | 0 |
| `int64` | 가변 길이 인코딩. 음수에 비효율적 | int64 | long | int/long | int64 | 0 |
| `uint32` | 가변 길이 인코딩 | uint32 | int | int/long | uint32 | 0 |
| `uint64` | 가변 길이 인코딩 | uint64 | long | int/long | uint64 | 0 |
| `sint32` | 가변 길이 인코딩. 음수에 효율적 (ZigZag 인코딩) | int32 | int | int | int32 | 0 |
| `sint64` | 가변 길이 인코딩. 음수에 효율적 (ZigZag 인코딩) | int64 | long | int/long | int64 | 0 |
| `fixed32` | 항상 4바이트. 값이 자주 2^28 이상이면 uint32보다 효율적 | uint32 | int | int/long | uint32 | 0 |
| `fixed64` | 항상 8바이트. 값이 자주 2^56 이상이면 uint64보다 효율적 | uint64 | long | int/long | uint64 | 0 |
| `sfixed32` | 항상 4바이트 | int32 | int | int | int32 | 0 |
| `sfixed64` | 항상 8바이트 | int64 | long | int/long | int64 | 0 |
| `bool` | 불리언 | bool | boolean | bool | bool | false |
| `string` | UTF-8 또는 7비트 ASCII 텍스트 | string | String | str | string | "" |
| `bytes` | 임의의 바이트 시퀀스 | string | ByteString | bytes | []byte | 빈 바이트 |

정수 타입 선택 가이드:

- 양수만: `uint32` / `uint64`
- 음수 가능 (음수가 드문 경우): `int32` / `int64`
- 음수 가능 (음수가 빈번한 경우): `sint32` / `sint64` (ZigZag 인코딩으로 작은 음수도 작게 인코딩)
- 값의 범위가 크고 고정적인 경우: `fixed32` / `fixed64`

### 3.3 proto2 vs proto3 차이

| 항목 | proto2 | proto3 |
|------|--------|--------|
| 문법 선언 | `syntax = "proto2";` | `syntax = "proto3";` |
| 필드 규칙 | `required`, `optional`, `repeated` | `singular`(기본), `optional`, `repeated` |
| required 필드 | 지원 (반드시 값이 있어야 함) | 제거됨 (하위 호환성 문제의 주범으로 판단) |
| 기본값 | 사용자가 직접 지정 가능 (`default = 42`) | 사용자 지정 불가. 타입별 고정 기본값 사용 (0, "", false 등) |
| 필드 존재 추적 | 모든 `optional` 필드에 `has_` 메서드 제공 | 기본적으로 제공하지 않음. `optional` 키워드를 명시하면 제공 |
| enum 기본값 | 첫 번째 선언된 값 | 반드시 0번 값이어야 함 |
| Unknown fields | 보존 | 초기에는 폐기했으나, 3.5부터 보존으로 변경 |
| 확장(extensions) | 지원 | 제거됨. `Any` 타입으로 대체 |
| 그룹(groups) | 지원 (사용 비권장) | 제거됨 |
| JSON 매핑 | 제한적 | 공식 JSON 매핑 제공 |
| map 타입 | 미지원 (초기) -> 이후 추가 | 기본 지원 |

proto3를 사용해야 하는 경우:
- 새로운 프로젝트를 시작할 때
- gRPC 서비스를 개발할 때 (gRPC는 proto3가 기본)
- JSON 호환성이 중요할 때

proto2를 유지해야 하는 경우:
- 기존 proto2 기반 시스템과의 호환성이 필요할 때
- `required` 필드가 반드시 필요할 때
- 사용자 정의 기본값이 필요할 때

### 3.4 직렬화/역직렬화 원리 (바이너리 인코딩)

Protocol Buffers는 바이너리 인코딩을 사용하여 데이터를 직렬화한다. 이 인코딩은 자기 서술적(self-describing)이지 않으므로, 디코딩하려면 `.proto` 스키마가 필요하다.

#### 3.4.1 기본 구조: Tag-Length-Value (TLV)

각 필드는 다음과 같은 형태로 인코딩된다.

```
[Tag] [Length (타입에 따라)] [Value]
```

Tag = (field_number << 3) | wire_type

#### 3.4.2 Wire Types

| Wire Type | 의미 | 사용 타입 |
|-----------|------|----------|
| 0 | Varint | int32, int64, uint32, uint64, sint32, sint64, bool, enum |
| 1 | 64-bit | fixed64, sfixed64, double |
| 2 | Length-delimited | string, bytes, embedded messages, packed repeated fields |
| 5 | 32-bit | fixed32, sfixed32, float |

(Wire type 3, 4는 deprecated된 "group" 관련 타입이었다.)

#### 3.4.3 Varint 인코딩

가변 길이 정수 인코딩이다. 작은 숫자는 적은 바이트로, 큰 숫자는 많은 바이트로 인코딩한다.

```
값 1   -> 0x01              (1바이트)
값 127 -> 0x7F              (1바이트)
값 128 -> 0x80 0x01         (2바이트)
값 300 -> 0xAC 0x02         (2바이트)
```

인코딩 방식:
1. 값을 7비트씩 쪼갠다 (LSB 우선).
2. 마지막 바이트를 제외하고, 각 바이트의 MSB(최상위 비트)를 1로 설정한다.
3. 마지막 바이트의 MSB는 0으로 설정한다.

예시: 300 (0b100101100) 인코딩

```
원본 비트:      1 0010 1100

7비트씩 분리:   0000010  0101100
                (상위)   (하위)

LSB 우선 배치:  0101100  0000010

MSB 추가:       10101100  00000010
                  0xAC      0x02
```

#### 3.4.4 ZigZag 인코딩 (sint32, sint64)

음수를 효율적으로 인코딩하기 위한 방식이다. 일반 `int32`에서 음수는 항상 10바이트를 차지하지만, ZigZag을 사용하면 절대값이 작은 음수도 적은 바이트로 인코딩된다.

```
원본 값    ZigZag 인코딩 값
  0    ->    0
 -1    ->    1
  1    ->    2
 -2    ->    3
  2    ->    4
...

공식: (n << 1) ^ (n >> 31)   // sint32의 경우
```

#### 3.4.5 인코딩 예시

다음 메시지를 인코딩하는 과정:

```protobuf
message Test {
  int32 a = 1;
  string b = 2;
}

// a = 150, b = "testing"
```

```
필드 a (field_number=1, wire_type=0 Varint):
  Tag:   0x08  ->  (1 << 3) | 0 = 0b00001000
  Value: 0x96 0x01  ->  150의 varint 인코딩

필드 b (field_number=2, wire_type=2 Length-delimited):
  Tag:    0x12  ->  (2 << 3) | 2 = 0b00010010
  Length: 0x07  ->  7바이트
  Value:  0x74 0x65 0x73 0x74 0x69 0x6E 0x67  ->  "testing"의 UTF-8

최종 바이트 시퀀스:
  08 96 01 12 07 74 65 73 74 69 6E 67
  (총 12바이트)
```

같은 데이터를 JSON으로 표현하면 `{"a":150,"b":"testing"}` = 24바이트이다. Protocol Buffers는 절반의 크기로 인코딩된다.

### 3.5 하위 호환성 규칙

Protocol Buffers의 핵심 설계 목표 중 하나는 하위 호환성(backward compatibility) 이다. 스키마를 진화시키면서도 기존 코드가 깨지지 않도록 하기 위한 규칙들이 있다.

#### 안전한 변경 (DO)

| 변경 | 설명 |
|------|------|
| 새 필드 추가 | 이전 코드는 새 필드를 무시한다 (unknown field). |
| 필드 제거 | 이전 코드는 해당 필드를 기본값으로 읽는다. 제거한 필드 번호를 `reserved`로 등록해야 한다. |
| int32, uint32, int64, uint64, bool 간 변환 | Wire type이 같으므로 호환된다 (값 잘림 주의). |
| sint32 <-> sint64 변환 | ZigZag 인코딩 호환. |
| bytes <-> string 변환 | string이 유효한 UTF-8이면 호환. |
| fixed32 <-> sfixed32, fixed64 <-> sfixed64 | Wire type이 같으므로 호환. |
| 단수 필드를 oneof 멤버로 이동 | 바이너리 호환. |
| optional <-> singular 변환 | 바이너리 호환 (API 차이만 존재). |

#### 위험한 변경 (DON'T)

| 변경 | 위험성 |
|------|--------|
| 필드 번호 변경 | 기존 데이터를 완전히 읽을 수 없게 됨. |
| 필드 타입을 비호환 타입으로 변경 | Wire type이 다르면 파싱 실패. |
| 필드 번호 재사용 | 이전 데이터를 잘못된 타입/의미로 해석. |
| repeated <-> scalar 변경 | 데이터 손실 가능. |
| int32 <-> sint32 변경 | 다른 인코딩 방식이라 값이 깨짐. |
| enum의 기존 값 번호 변경 | 기존 데이터의 의미가 달라짐. |

#### 권장 사항

```protobuf
// 필드를 제거할 때는 반드시 reserved 사용
message MyMessage {
  reserved 2, 3;           // 다시는 이 번호를 쓰지 않음
  reserved "old_field";    // 다시는 이 이름을 쓰지 않음

  string name = 1;
  string new_field = 4;
}
```

---

## 4. gRPC 상세

### 4.1 4가지 통신 패턴

gRPC는 4가지 서비스 메서드 유형을 지원한다.

#### 4.1.1 Unary RPC (단항 RPC)

가장 기본적인 형태. 클라이언트가 하나의 요청을 보내고, 서버가 하나의 응답을 반환한다.

```protobuf
service UserService {
  rpc GetUser (GetUserRequest) returns (GetUserResponse);
}
```

```
Client                    Server
  |                         |
  |--- GetUserRequest ----->|
  |                         |
  |<-- GetUserResponse -----|
  |                         |
```

일반적인 HTTP 요청/응답과 유사하며, 대부분의 API 호출에 사용된다.

#### 4.1.2 Server Streaming RPC (서버 스트리밍)

클라이언트가 하나의 요청을 보내면, 서버가 여러 개의 응답을 스트림으로 반환한다.

```protobuf
service StockService {
  rpc WatchStock (StockRequest) returns (stream StockResponse);
}
```

```
Client                     Server
  |                          |
  |--- StockRequest -------->|
  |                          |
  |<-- StockResponse[1] -----|
  |<-- StockResponse[2] -----|
  |<-- StockResponse[3] -----|
  |         ...              |
  |<-- (stream end) ---------|
  |                          |
```

실시간 데이터 피드, 대량 데이터 다운로드 등에 적합하다.

#### 4.1.3 Client Streaming RPC (클라이언트 스트리밍)

클라이언트가 여러 개의 요청을 스트림으로 보내고, 서버가 하나의 응답을 반환한다.

```protobuf
service UploadService {
  rpc UploadFile (stream FileChunk) returns (UploadStatus);
}
```

```
Client                     Server
  |                          |
  |--- FileChunk[1] ------->|
  |--- FileChunk[2] ------->|
  |--- FileChunk[3] ------->|
  |         ...              |
  |--- (stream end) ------->|
  |                          |
  |<-- UploadStatus ---------|
  |                          |
```

파일 업로드, 센서 데이터 전송 등에 적합하다.

#### 4.1.4 Bidirectional Streaming RPC (양방향 스트리밍)

클라이언트와 서버가 모두 스트림으로 데이터를 주고받는다. 두 스트림은 독립적으로 동작한다.

```protobuf
service ChatService {
  rpc Chat (stream ChatMessage) returns (stream ChatMessage);
}
```

```
Client                     Server
  |                          |
  |--- ChatMessage[1] ----->|
  |<-- ChatMessage[A] ------|
  |--- ChatMessage[2] ----->|
  |--- ChatMessage[3] ----->|
  |<-- ChatMessage[B] ------|
  |<-- ChatMessage[C] ------|
  |         ...              |
```

채팅, 실시간 협업, 양방향 데이터 동기화 등에 적합하다. 두 스트림은 독립적이므로 클라이언트와 서버가 임의의 순서로 읽고 쓸 수 있다.

### 4.2 HTTP/2 기반 전송

gRPC는 HTTP/2를 전송 프로토콜로 사용한다. HTTP/2의 핵심 기능이 gRPC의 성능을 뒷받침한다.

#### 4.2.1 HTTP/2의 주요 특징

```
+------------------------------------------------------+
|                    HTTP/2 연결 (TCP)                    |
|                                                      |
|  +----------+  +----------+  +----------+            |
|  | Stream 1 |  | Stream 3 |  | Stream 5 |  ...       |
|  | (RPC #1) |  | (RPC #2) |  | (RPC #3) |            |
|  +----------+  +----------+  +----------+            |
|                                                      |
|  모든 스트림이 하나의 TCP 연결을 다중화(multiplex)          |
+------------------------------------------------------+
```

| HTTP/2 특징 | gRPC에서의 활용 |
|------------|---------------|
| 멀티플렉싱(Multiplexing) | 하나의 TCP 연결에서 여러 RPC 호출을 동시에 처리. 연결 수립 오버헤드 감소. |
| 바이너리 프레이밍 | 텍스트 기반 HTTP/1.1 대비 파싱 효율성 향상. Protocol Buffers의 바이너리 특성과 잘 맞음. |
| 헤더 압축 (HPACK) | 반복되는 헤더를 압축하여 네트워크 사용량 감소. |
| 서버 푸시 | 서버 스트리밍의 기반. |
| 흐름 제어 (Flow Control) | 스트림 단위, 연결 단위로 흐름을 제어하여 과부하 방지. |
| 스트림 우선순위 | 중요한 RPC에 더 많은 리소스를 할당 가능. |

#### 4.2.2 gRPC의 HTTP/2 매핑

```
요청:
  :method = POST
  :scheme = https
  :path = /{ServiceName}/{MethodName}
  :authority = {host}
  content-type = application/grpc
  te = trailers
  grpc-encoding = gzip           (선택적)
  grpc-timeout = 10S             (선택적)

  [Length-Prefixed Message]       (Protobuf 인코딩된 요청 메시지)

응답:
  :status = 200
  content-type = application/grpc

  [Length-Prefixed Message]       (Protobuf 인코딩된 응답 메시지)

  grpc-status = 0                (Trailer, 성공 시 0)
  grpc-message =                 (Trailer, 에러 메시지)
```

### 4.3 채널(Channel)과 스텁(Stub)

#### 4.3.1 Channel

Channel은 gRPC 서버와의 연결을 추상화한 객체다. 실제 TCP 연결의 생성, 관리, 풀링 등을 담당한다.

```
+------------------+         +------------------+
|   Application    |         |    gRPC Server   |
|                  |         |                  |
|  +------------+  |         |                  |
|  |   Stub     |  | Channel |                  |
|  |            |--+-------->|   Service Impl   |
|  +------------+  |  HTTP/2 |                  |
|                  |         |                  |
+------------------+         +------------------+
```

주요 특성:
- 연결 상태 관리: IDLE, CONNECTING, READY, TRANSIENT_FAILURE, SHUTDOWN
- 자동 재연결: 연결이 끊어지면 자동으로 재연결 시도
- 연결 풀링: 내부적으로 여러 서브채널을 관리
- 로드 밸런싱: 여러 백엔드 서버에 대한 부하 분산
- 스레드 안전: 여러 스레드에서 동시에 사용 가능

```go
// Go 예시
conn, err := grpc.Dial(
    "localhost:50051",
    grpc.WithTransportCredentials(insecure.NewCredentials()),
    grpc.WithDefaultServiceConfig(`{"loadBalancingPolicy":"round_robin"}`),
)
defer conn.Close()
```

#### 4.3.2 Stub

Stub은 `.proto` 파일에서 자동 생성되는 클라이언트 측 코드로, RPC 메서드를 로컬 메서드처럼 호출할 수 있게 해준다.

gRPC는 보통 두 종류의 stub을 생성한다:

| Stub 유형 | 설명 |
|----------|------|
| Blocking Stub (동기) | RPC 호출이 완료될 때까지 현재 스레드를 블로킹. Unary와 서버 스트리밍에만 사용 가능. |
| Async Stub (비동기) | 비블로킹 방식으로 RPC를 호출. 콜백 또는 Future로 결과를 받음. 모든 패턴에 사용 가능. |

```java
// Java 예시
// Blocking Stub
UserServiceGrpc.UserServiceBlockingStub blockingStub =
    UserServiceGrpc.newBlockingStub(channel);
GetUserResponse response = blockingStub.getUser(request);

// Async Stub
UserServiceGrpc.UserServiceStub asyncStub =
    UserServiceGrpc.newStub(channel);
asyncStub.getUser(request, new StreamObserver<GetUserResponse>() {
    @Override
    public void onNext(GetUserResponse response) { /* 응답 처리 */ }
    @Override
    public void onError(Throwable t) { /* 에러 처리 */ }
    @Override
    public void onCompleted() { /* 완료 처리 */ }
});
```

### 4.4 메타데이터(Metadata)

Metadata는 gRPC 호출에 부가 정보를 전달하기 위한 키-값 쌍이다. HTTP 헤더와 유사한 역할을 한다.

```
Client                          Server
  |                               |
  |-- [Metadata] Request -------->|  (Initial Metadata)
  |                               |
  |<- [Metadata] Response --------|  (Initial Metadata)
  |<- Response Data --------------|
  |<- [Metadata] Trailers --------|  (Trailing Metadata)
  |                               |
```

#### 메타데이터의 종류

| 종류 | 시점 | 설명 |
|------|------|------|
| Request Metadata | 클라이언트 -> 서버 (요청 시) | 인증 토큰, 트레이싱 ID, 커스텀 헤더 등 |
| Initial Metadata | 서버 -> 클라이언트 (응답 시작) | 서버 정보, 커스텀 헤더 등 |
| Trailing Metadata | 서버 -> 클라이언트 (응답 종료) | gRPC 상태 코드, 에러 상세 정보 등 |

#### 키 규칙

- 일반 키: ASCII 문자열. 값도 ASCII 문자열.
- 바이너리 키: `-bin` 접미사를 붙이면 값을 바이너리(Base64 인코딩)로 전달 가능.

```python
# Python 예시
metadata = [
    ('authorization', 'Bearer my-token'),
    ('x-request-id', 'abc-123'),
    ('x-binary-data-bin', b'\x00\x01\x02'),  # -bin 접미사 = 바이너리
]

response, call = stub.GetUser.with_call(request, metadata=metadata)

# 서버에서 보낸 메타데이터 읽기
initial_metadata = call.initial_metadata()
trailing_metadata = call.trailing_metadata()
```

### 4.5 인터셉터(Interceptor)

Interceptor는 RPC 호출의 전후에 공통 로직을 삽입할 수 있는 미들웨어 메커니즘이다. HTTP의 미들웨어, Java의 서블릿 필터와 유사하다.

```
Client                                              Server
  |                                                   |
  |  [Client Interceptor 1]                           |
  |    [Client Interceptor 2]                         |
  |      |--- RPC Request --------->                  |
  |      |                   [Server Interceptor 1]   |
  |      |                     [Server Interceptor 2] |
  |      |                       |--- Handler ------->|
  |      |                     [Server Interceptor 2] |
  |      |                   [Server Interceptor 1]   |
  |      |<-- RPC Response ---------                  |
  |    [Client Interceptor 2]                         |
  |  [Client Interceptor 1]                           |
  |                                                   |
```

#### 인터셉터 유형

| 유형 | 클라이언트 | 서버 |
|------|-----------|------|
| Unary | UnaryClientInterceptor | UnaryServerInterceptor |
| Stream | StreamClientInterceptor | StreamServerInterceptor |

#### 일반적인 활용 사례

- 로깅: 요청/응답 로깅, 소요 시간 측정
- 인증/인가: 토큰 검증, 권한 확인
- 메트릭 수집: Prometheus, OpenTelemetry 연동
- 에러 처리: 공통 에러 변환, 재시도 로직
- 트레이싱: 분산 추적 컨텍스트 전파 (Jaeger, Zipkin 등)
- 레이트 리미팅: 요청 속도 제한

```go
// Go 서버 인터셉터 예시
func loggingInterceptor(
    ctx context.Context,
    req interface{},
    info *grpc.UnaryServerInfo,
    handler grpc.UnaryHandler,
) (interface{}, error) {
    start := time.Now()
    log.Printf("RPC 시작: %s", info.FullMethod)

    // 실제 핸들러 호출
    resp, err := handler(ctx, req)

    log.Printf("RPC 완료: %s, 소요시간: %v, 에러: %v",
        info.FullMethod, time.Since(start), err)
    return resp, err
}

// 서버에 인터셉터 등록
server := grpc.NewServer(
    grpc.UnaryInterceptor(loggingInterceptor),
    // 여러 인터셉터를 체이닝:
    grpc.ChainUnaryInterceptor(
        loggingInterceptor,
        authInterceptor,
        metricsInterceptor,
    ),
)
```

### 4.6 에러 처리 (Status Codes)

gRPC는 자체적인 상태 코드 체계를 가지고 있다. HTTP 상태 코드와는 별도로 정의되며, `grpc-status` 트레일러로 전달된다.

#### gRPC Status Codes

| 코드 | 이름 | 설명 |
|------|------|------|
| 0 | OK | 성공 |
| 1 | CANCELLED | 클라이언트가 호출을 취소함 |
| 2 | UNKNOWN | 알 수 없는 에러 (예: 서버에서 처리되지 않은 예외) |
| 3 | INVALID_ARGUMENT | 클라이언트가 잘못된 인자를 전달함 (HTTP 400) |
| 4 | DEADLINE_EXCEEDED | 데드라인 초과 (HTTP 408) |
| 5 | NOT_FOUND | 리소스를 찾을 수 없음 (HTTP 404) |
| 6 | ALREADY_EXISTS | 리소스가 이미 존재함 (HTTP 409) |
| 7 | PERMISSION_DENIED | 권한 없음 (HTTP 403) |
| 8 | RESOURCE_EXHAUSTED | 리소스 한도 초과 (HTTP 429) |
| 9 | FAILED_PRECONDITION | 전제 조건 실패 (HTTP 400) |
| 10 | ABORTED | 동시성 충돌 등으로 중단 (HTTP 409) |
| 11 | OUT_OF_RANGE | 유효 범위 초과 (HTTP 400) |
| 12 | UNIMPLEMENTED | 메서드 미구현 (HTTP 501) |
| 13 | INTERNAL | 서버 내부 에러 (HTTP 500) |
| 14 | UNAVAILABLE | 서비스 일시 불가 (HTTP 503). 재시도 가능. |
| 15 | DATA_LOSS | 복구 불가능한 데이터 손실 (HTTP 500) |
| 16 | UNAUTHENTICATED | 인증 실패 (HTTP 401) |

#### 풍부한 에러 모델 (Rich Error Model)

기본 상태 코드 외에 구조화된 에러 상세 정보를 전달할 수 있다.

```protobuf
// google/rpc/error_details.proto에 정의된 표준 에러 상세 타입들
import "google/rpc/error_details.proto";

// 사용 예시:
// - RetryInfo: 재시도까지 대기 시간
// - DebugInfo: 디버그 스택 트레이스
// - BadRequest: 어떤 필드가 잘못되었는지
// - ResourceInfo: 어떤 리소스가 문제인지
// - ErrorInfo: 에러 이유, 도메인, 메타데이터
```

```go
// Go 에러 반환 예시
import "google.golang.org/grpc/status"

st := status.New(codes.InvalidArgument, "유효하지 않은 사용자 이름")
st, _ = st.WithDetails(&errdetails.BadRequest_FieldViolation{
    Field:       "username",
    Description: "사용자 이름은 3자 이상이어야 합니다",
})
return nil, st.Err()
```

### 4.7 데드라인/타임아웃

gRPC에서 데드라인(deadline) 은 RPC가 완료되어야 하는 절대 시점이고, 타임아웃(timeout) 은 상대적인 시간이다. 내부적으로 데드라인은 `grpc-timeout` 헤더로 전달된다.

```
Client (deadline: 5초 후)
  |
  |--- RPC 호출 (grpc-timeout: 5S) ----> Server A
  |                                        |
  |                         Server A가 Server B에 전파
  |                         (남은 시간: 3S)
  |                                        |--- RPC (grpc-timeout: 3S) ---> Server B
  |                                        |<-- 응답 -----------------------|
  |<-- 응답 --------------------------------|
```

핵심 특징:
- 전파(Propagation): 데드라인은 서비스 체인을 따라 자동으로 전파된다. 서버 A가 서버 B를 호출할 때, 남은 시간이 자동으로 전달된다.
- DEADLINE_EXCEEDED: 데드라인이 초과되면 양쪽 모두에서 `DEADLINE_EXCEEDED` 에러가 발생한다.
- 권장 사항: 모든 RPC 호출에 데드라인을 설정하는 것이 권장된다. 설정하지 않으면 실패한 요청이 서버 리소스를 무한히 점유할 수 있다.

```go
// Go 예시
ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
defer cancel()

response, err := client.GetUser(ctx, request)
if err != nil {
    st, ok := status.FromError(err)
    if ok && st.Code() == codes.DeadlineExceeded {
        log.Println("데드라인 초과!")
    }
}
```

```python
# Python 예시
try:
    response = stub.GetUser(request, timeout=5.0)  # 5초 타임아웃
except grpc.RpcError as e:
    if e.code() == grpc.StatusCode.DEADLINE_EXCEEDED:
        print("데드라인 초과!")
```

### 4.8 로드 밸런싱

gRPC는 다양한 로드 밸런싱 전략을 지원한다.

#### 4.8.1 프록시 기반 로드 밸런싱 (L7)

```
Clients -----> [L7 Load Balancer] -----> gRPC Servers
                (Envoy, NGINX,            (Server 1)
                 Linkerd 등)              (Server 2)
                                          (Server 3)
```

- gRPC는 HTTP/2 기반이므로, L4(TCP) 로드 밸런서는 부적합하다.
- HTTP/2 멀티플렉싱 때문에 하나의 TCP 연결에 모든 요청이 몰리는 문제 발생.
- L7 로드 밸런서(Envoy, NGINX 등)가 HTTP/2 프레임을 이해하고 요청 단위로 분산해야 한다.

#### 4.8.2 클라이언트 사이드 로드 밸런싱

```
                    +-> Server 1
Client (내장 LB) ---+-> Server 2
                    +-> Server 3
```

gRPC 클라이언트에 내장된 로드 밸런싱:

| 정책 | 설명 |
|------|------|
| pick_first (기본) | 리스트의 첫 번째 가용 서버에 연결 |
| round_robin | 모든 서버에 순차적으로 요청 분산 |
| grpclb | 외부 밸런서 서비스에 질의 (deprecated) |
| xDS | xDS 프로토콜 기반 (Envoy 호환). 가장 진보된 방식 |

```go
// 클라이언트 사이드 라운드 로빈
conn, err := grpc.Dial(
    "dns:///my-service.example.com:50051",
    grpc.WithDefaultServiceConfig(`{"loadBalancingPolicy":"round_robin"}`),
)
```

#### 4.8.3 Look-aside 로드 밸런싱 (xDS)

```
Client ---- xDS ----> Control Plane (istiod, etc.)
  |                        |
  | (서버 목록 + 정책 수신)   | (서버 상태 모니터링)
  |                        |
  +---- RPC 호출 ---------> gRPC Servers
```

xDS(eXtensible Discovery Service) 프로토콜을 사용하여 Istio, Traffic Director 등의 서비스 메시와 통합된다.

---

## 5. 코드 생성 (protoc 컴파일러)

### 5.1 protoc 개요

`protoc`은 Protocol Buffers 컴파일러로, `.proto` 파일을 읽어서 다양한 프로그래밍 언어의 소스 코드를 생성한다.

```
                        +-- protoc-gen-go ---------> user.pb.go
                        |
.proto 파일 --> protoc --+-- protoc-gen-go-grpc ----> user_grpc.pb.go
                        |
                        +-- protoc-gen-python -----> user_pb2.py
                        |
                        +-- protoc-gen-grpc-java --> UserServiceGrpc.java
```

### 5.2 설치

```bash
# macOS
brew install protobuf

# Ubuntu/Debian
apt install -y protobuf-compiler

# 버전 확인
protoc --version
```

### 5.3 플러그인 설치 (언어별)

```bash
# Go
go install google.golang.org/protobuf/cmd/protoc-gen-go@latest
go install google.golang.org/grpc/cmd/protoc-gen-go-grpc@latest

# Python
pip install grpcio-tools

# TypeScript / JavaScript
npm install @grpc/proto-loader grpc-tools
```

### 5.4 코드 생성 명령어

```bash
# Go (메시지 + gRPC 서비스)
protoc \
  --go_out=./gen \
  --go_opt=paths=source_relative \
  --go-grpc_out=./gen \
  --go-grpc_opt=paths=source_relative \
  proto/user.proto

# Python
python -m grpc_tools.protoc \
  -I./proto \
  --python_out=./gen \
  --grpc_python_out=./gen \
  proto/user.proto

# Java
protoc \
  --java_out=./gen \
  --grpc-java_out=./gen \
  --plugin=protoc-gen-grpc-java=/path/to/protoc-gen-grpc-java \
  proto/user.proto

# C++
protoc \
  --cpp_out=./gen \
  --grpc_out=./gen \
  --plugin=protoc-gen-grpc=`which grpc_cpp_plugin` \
  proto/user.proto
```

### 5.5 생성되는 코드

`.proto` 파일에서 다음이 생성된다:

| 구분 | 생성 내용 |
|------|----------|
| 메시지 | 각 `message`에 대한 구조체/클래스, getter/setter, 직렬화/역직렬화 메서드 |
| Enum | 각 `enum`에 대한 상수 정의 |
| 서비스 (서버) | 서비스 인터페이스. 개발자가 이를 구현(implement)함 |
| 서비스 (클라이언트) | Stub 클래스. RPC 메서드를 호출하는 클라이언트 코드 |
| 빌더 | (Java 등) 메시지 객체를 생성하기 위한 빌더 패턴 코드 |

### 5.6 Buf (차세대 도구)

`protoc`의 대안으로 Buf 도구가 등장했다. lint, breaking change 감지, 의존성 관리 등 더 현대적인 워크플로우를 제공한다.

```yaml
# buf.yaml
version: v2
modules:
  - path: proto
lint:
  use:
    - STANDARD
breaking:
  use:
    - FILE
```

```bash
# Buf로 코드 생성
buf generate

# lint 검사
buf lint

# 하위 호환성 검사
buf breaking --against '.git#branch=main'
```

---

## 6. 언어별 지원 현황

### 6.1 공식 지원 언어

| 언어 | 패키지/라이브러리 | 비고 |
|------|-----------------|------|
| C/C++ | `grpc`, `protobuf` | 핵심 구현체. 다른 언어 바인딩의 기반 |
| Java | `grpc-java`, `protobuf-java` | Android 지원 포함 (grpc-android) |
| Go | `google.golang.org/grpc`, `google.golang.org/protobuf` | Kubernetes 생태계에서 특히 활발 |
| Python | `grpcio`, `grpcio-tools` | asyncio 지원 |
| C#/.NET | `Grpc.Net.Client`, `Google.Protobuf` | ASP.NET Core 통합 |
| Node.js | `@grpc/grpc-js` | 순수 JavaScript 구현 |
| Ruby | `grpc` gem | |
| Objective-C | `gRPC-ProtoRPC` | iOS 개발용 |
| PHP | `grpc/grpc` | |
| Dart | `grpc` package | Flutter 지원 |
| Kotlin | `grpc-kotlin` | 코루틴 기반 API |
| Swift | `grpc-swift` | Apple 플랫폼용. SwiftNIO 기반 |

### 6.2 주요 생태계 도구

| 도구 | 역할 |
|------|------|
| grpc-gateway | gRPC 서비스를 RESTful JSON API로 노출하는 리버스 프록시 |
| grpcurl | gRPC 서버를 curl처럼 호출하는 CLI 도구 |
| grpc-web | 브라우저에서 gRPC를 사용하기 위한 JavaScript 클라이언트 |
| Evans | gRPC용 인터랙티브 CLI 클라이언트 |
| Buf | Protobuf 린팅, 호환성 검사, 코드 생성 도구 |
| BloomRPC / Postman | gRPC용 GUI 클라이언트 |
| protoc-gen-doc | `.proto` 파일에서 문서 자동 생성 |
| protoc-gen-validate | 메시지 필드 유효성 검사 코드 생성 |

---

## 7. gRPC vs REST vs GraphQL 비교

### 7.1 개괄 비교

| 항목 | gRPC | REST | GraphQL |
|------|------|------|---------|
| 프로토콜 | HTTP/2 | HTTP/1.1 (주로) | HTTP/1.1 (주로) |
| 데이터 포맷 | Protocol Buffers (바이너리) | JSON/XML (텍스트) | JSON (텍스트) |
| 인터페이스 정의 | `.proto` 파일 (필수) | OpenAPI/Swagger (선택) | Schema (필수) |
| 코드 생성 | 내장 (protoc) | 서드파티 도구 | 서드파티 도구 |
| 스트리밍 | 양방향 스트리밍 네이티브 지원 | SSE, WebSocket (별도) | Subscription (별도) |
| 브라우저 지원 | 제한적 (grpc-web 필요) | 네이티브 | 네이티브 |
| 타입 안전성 | 강타입 (컴파일 타임) | 약타입 (런타임) | 강타입 (런타임) |
| 성능 | 높음 | 중간 | 중간 |
| 페이로드 크기 | 작음 (바이너리) | 큼 (텍스트) | 중간 (필요한 필드만) |
| 학습 곡선 | 높음 | 낮음 | 중간 |
| 디버깅 | 어려움 (바이너리) | 쉬움 (텍스트) | 중간 |
| 캐싱 | 어려움 (POST 기반) | 쉬움 (HTTP 캐싱) | 어려움 (POST 기반) |

### 7.2 성능 비교

```
직렬화/역직렬화 속도 (상대적):
  gRPC (Protobuf)  |████████████████████| 빠름
  REST (JSON)      |██████████          | 중간
  GraphQL (JSON)   |██████████          | 중간

페이로드 크기 (같은 데이터 기준):
  gRPC (Protobuf)  |████                | 작음
  REST (JSON)      |████████████████    | 큼
  GraphQL (JSON)   |██████████          | 중간 (필요한 것만)

지연시간 (Latency):
  gRPC             |████                | 낮음 (HTTP/2 + 바이너리)
  REST             |████████████        | 중간
  GraphQL          |██████████          | 중간
```

### 7.3 적합한 사용 사례

| 기술 | 적합한 사례 |
|------|-----------|
| gRPC | 마이크로서비스 간 내부 통신, 실시간 스트리밍, 고성능 요구, 폴리글랏(다언어) 환경 |
| REST | 공개 API, 웹 애플리케이션, CRUD 기반 서비스, 단순한 인터페이스 |
| GraphQL | 복잡한 데이터 요구사항, 프론트엔드 주도 개발, 다양한 클라이언트 지원, 오버페칭/언더페칭 해결 |

### 7.4 혼합 사용 패턴

실무에서는 이 세 기술을 함께 사용하는 경우가 많다.

```
                  +-- REST API ---------> 외부 클라이언트
                  |
API Gateway ------+-- GraphQL API ------> 모바일/웹 앱
                  |
                  +-- gRPC-Web ---------> SPA

내부:
  Service A <--- gRPC ---> Service B
  Service B <--- gRPC ---> Service C
  Service C <--- gRPC ---> Database Service
```

---

## 8. 장점과 단점

### 8.1 장점

| 장점 | 설명 |
|------|------|
| 고성능 | 바이너리 직렬화(Protobuf)와 HTTP/2 멀티플렉싱으로 REST 대비 최대 10배 빠른 처리 가능 |
| 강력한 타입 시스템 | `.proto` 파일로 계약을 명확하게 정의. 컴파일 타임에 타입 오류 발견 |
| 코드 자동 생성 | `protoc`이 10개 이상의 언어에 대해 클라이언트/서버 코드를 자동 생성. 보일러플레이트 제거 |
| 양방향 스트리밍 | HTTP/2 기반 네이티브 양방향 스트리밍. 실시간 통신에 최적 |
| 하위 호환성 | Protobuf의 필드 번호 시스템으로 스키마 진화가 안전 |
| 언어 중립 | 다양한 언어 지원으로 폴리글랏 마이크로서비스에 적합 |
| 효율적인 네트워크 사용 | 작은 페이로드, 헤더 압축, 멀티플렉싱으로 대역폭 절약 |
| 데드라인 전파 | 서비스 체인을 따라 타임아웃이 자동 전파. 연쇄 장애 방지 |
| 인터셉터 | 인증, 로깅, 메트릭 등을 위한 미들웨어 패턴 내장 |
| 서비스 메시 통합 | Envoy, Istio 등과의 깊은 통합 |

### 8.2 단점

| 단점 | 설명 |
|------|------|
| 브라우저 지원 제한 | 브라우저에서 HTTP/2 트레일러를 직접 사용할 수 없어 grpc-web 프록시 필요 |
| 디버깅 어려움 | 바이너리 포맷이라 curl이나 브라우저로 직접 확인 불가. 별도 도구(grpcurl 등) 필요 |
| 학습 곡선 | Protobuf, HTTP/2, 스트리밍 개념 등 이해해야 할 것이 많음 |
| HTTP 캐싱 불가 | 모든 요청이 POST이므로 HTTP 캐시(CDN, 브라우저 캐시)를 활용할 수 없음 |
| 텍스트 비가독성 | JSON과 달리 사람이 직접 읽을 수 없는 바이너리 포맷 |
| Protobuf 제한 | null 값 표현 불가(wrapper type 필요), union type 불편, 상속 미지원 |
| 로드 밸런싱 복잡 | HTTP/2 멀티플렉싱 때문에 L4 로드 밸런서가 부적합. L7 필요 |
| 생태계 성숙도 | REST에 비해 도구, 라이브러리, 커뮤니티 자원이 적음 |
| 에러 처리 | HTTP 상태 코드에 익숙한 개발자에게 gRPC 상태 코드가 낯설 수 있음 |

---

## 9. 실제 예시

### 9.1 완전한 .proto 파일 예시

```protobuf
// user_service.proto
syntax = "proto3";

package mycompany.user.v1;

option go_package = "github.com/mycompany/api/user/v1;userv1";
option java_package = "com.mycompany.api.user.v1";
option java_multiple_files = true;

import "google/protobuf/timestamp.proto";
import "google/protobuf/field_mask.proto";
import "google/protobuf/wrappers.proto";

// ============================
// 메시지 정의
// ============================

// 사용자 정보
message User {
  // 고유 식별자 (UUID)
  string id = 1;

  // 사용자 이름
  string username = 2;

  // 이메일 주소
  string email = 3;

  // 프로필 정보
  UserProfile profile = 4;

  // 사용자 상태
  UserStatus status = 5;

  // 역할 목록
  repeated UserRole roles = 6;

  // 사용자 설정 (키-값)
  map<string, string> preferences = 7;

  // 연락처 (하나만 선택)
  oneof contact {
    string phone_number = 8;
    string slack_handle = 9;
  }

  // 연령 (null 가능 - wrapper type 사용)
  google.protobuf.Int32Value age = 10;

  // 생성 시각
  google.protobuf.Timestamp created_at = 11;

  // 수정 시각
  google.protobuf.Timestamp updated_at = 12;
}

// 사용자 프로필
message UserProfile {
  string display_name = 1;
  string bio = 2;
  string avatar_url = 3;
  Address address = 4;
}

// 주소
message Address {
  string street = 1;
  string city = 2;
  string state = 3;
  string zip_code = 4;
  string country = 5;
}

// 사용자 상태
enum UserStatus {
  USER_STATUS_UNSPECIFIED = 0;
  USER_STATUS_ACTIVE = 1;
  USER_STATUS_INACTIVE = 2;
  USER_STATUS_SUSPENDED = 3;
  USER_STATUS_DELETED = 4;
}

// 사용자 역할
enum UserRole {
  USER_ROLE_UNSPECIFIED = 0;
  USER_ROLE_ADMIN = 1;
  USER_ROLE_EDITOR = 2;
  USER_ROLE_VIEWER = 3;
}

// ============================
// 서비스 정의
// ============================

service UserService {
  // 사용자 생성
  rpc CreateUser (CreateUserRequest) returns (CreateUserResponse);

  // 사용자 조회
  rpc GetUser (GetUserRequest) returns (GetUserResponse);

  // 사용자 목록 조회 (서버 스트리밍)
  rpc ListUsers (ListUsersRequest) returns (stream User);

  // 사용자 정보 수정
  rpc UpdateUser (UpdateUserRequest) returns (UpdateUserResponse);

  // 사용자 삭제
  rpc DeleteUser (DeleteUserRequest) returns (DeleteUserResponse);

  // 대량 사용자 생성 (클라이언트 스트리밍)
  rpc BulkCreateUsers (stream CreateUserRequest) returns (BulkCreateUsersResponse);

  // 실시간 사용자 상태 구독 (양방향 스트리밍)
  rpc WatchUserStatus (stream WatchUserStatusRequest) returns (stream UserStatusEvent);
}

// ============================
// 요청/응답 메시지
// ============================

message CreateUserRequest {
  string username = 1;
  string email = 2;
  UserProfile profile = 3;
  repeated UserRole roles = 4;
}

message CreateUserResponse {
  User user = 1;
}

message GetUserRequest {
  string user_id = 1;
}

message GetUserResponse {
  User user = 1;
}

message ListUsersRequest {
  int32 page_size = 1;          // 페이지 크기
  string page_token = 2;        // 페이지 토큰
  UserStatus status_filter = 3; // 상태 필터
}

message UpdateUserRequest {
  User user = 1;
  // 수정할 필드만 지정 (Partial Update)
  google.protobuf.FieldMask update_mask = 2;
}

message UpdateUserResponse {
  User user = 1;
}

message DeleteUserRequest {
  string user_id = 1;
}

message DeleteUserResponse {}

message BulkCreateUsersResponse {
  int32 created_count = 1;
  repeated string failed_usernames = 2;
}

message WatchUserStatusRequest {
  repeated string user_ids = 1;
}

message UserStatusEvent {
  string user_id = 1;
  UserStatus old_status = 2;
  UserStatus new_status = 3;
  google.protobuf.Timestamp changed_at = 4;
}
```

### 9.2 Go 서버 구현 예시

```go
package main

import (
    "context"
    "log"
    "net"
    "time"

    "google.golang.org/grpc"
    "google.golang.org/grpc/codes"
    "google.golang.org/grpc/status"
    "google.golang.org/protobuf/types/known/timestamppb"

    pb "github.com/mycompany/api/user/v1"
)

type userServer struct {
    pb.UnimplementedUserServiceServer
    users map[string]*pb.User
}

func (s *userServer) CreateUser(
    ctx context.Context,
    req *pb.CreateUserRequest,
) (*pb.CreateUserResponse, error) {
    // 입력 검증
    if req.Username == "" {
        return nil, status.Error(codes.InvalidArgument, "username은 필수입니다")
    }
    if req.Email == "" {
        return nil, status.Error(codes.InvalidArgument, "email은 필수입니다")
    }

    // 사용자 생성
    user := &pb.User{
        Id:        generateUUID(),
        Username:  req.Username,
        Email:     req.Email,
        Profile:   req.Profile,
        Status:    pb.UserStatus_USER_STATUS_ACTIVE,
        Roles:     req.Roles,
        CreatedAt: timestamppb.Now(),
        UpdatedAt: timestamppb.Now(),
    }

    s.users[user.Id] = user

    return &pb.CreateUserResponse{User: user}, nil
}

func (s *userServer) GetUser(
    ctx context.Context,
    req *pb.GetUserRequest,
) (*pb.GetUserResponse, error) {
    user, ok := s.users[req.UserId]
    if !ok {
        return nil, status.Errorf(codes.NotFound,
            "사용자 %s를 찾을 수 없습니다", req.UserId)
    }
    return &pb.GetUserResponse{User: user}, nil
}

// 서버 스트리밍 예시
func (s *userServer) ListUsers(
    req *pb.ListUsersRequest,
    stream pb.UserService_ListUsersServer,
) error {
    for _, user := range s.users {
        // 필터 적용
        if req.StatusFilter != pb.UserStatus_USER_STATUS_UNSPECIFIED &&
            user.Status != req.StatusFilter {
            continue
        }

        if err := stream.Send(user); err != nil {
            return err
        }
    }
    return nil
}

func main() {
    lis, err := net.Listen("tcp", ":50051")
    if err != nil {
        log.Fatalf("listen 실패: %v", err)
    }

    // 인터셉터 추가
    server := grpc.NewServer(
        grpc.ChainUnaryInterceptor(
            loggingInterceptor,
        ),
    )

    pb.RegisterUserServiceServer(server, &userServer{
        users: make(map[string]*pb.User),
    })

    log.Println("gRPC 서버 시작: :50051")
    if err := server.Serve(lis); err != nil {
        log.Fatalf("서버 실행 실패: %v", err)
    }
}

func loggingInterceptor(
    ctx context.Context,
    req interface{},
    info *grpc.UnaryServerInfo,
    handler grpc.UnaryHandler,
) (interface{}, error) {
    start := time.Now()
    resp, err := handler(ctx, req)
    log.Printf("[%s] %v (err: %v)", info.FullMethod, time.Since(start), err)
    return resp, err
}

func generateUUID() string {
    // UUID 생성 로직 (생략)
    return "generated-uuid"
}
```

### 9.3 Go 클라이언트 구현 예시

```go
package main

import (
    "context"
    "io"
    "log"
    "time"

    "google.golang.org/grpc"
    "google.golang.org/grpc/credentials/insecure"
    "google.golang.org/grpc/metadata"

    pb "github.com/mycompany/api/user/v1"
)

func main() {
    // 채널 생성
    conn, err := grpc.Dial(
        "localhost:50051",
        grpc.WithTransportCredentials(insecure.NewCredentials()),
    )
    if err != nil {
        log.Fatalf("연결 실패: %v", err)
    }
    defer conn.Close()

    // Stub 생성
    client := pb.NewUserServiceClient(conn)

    // 데드라인 설정
    ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
    defer cancel()

    // 메타데이터 추가
    ctx = metadata.AppendToOutgoingContext(ctx,
        "authorization", "Bearer my-token",
        "x-request-id", "req-001",
    )

    // Unary RPC 호출
    createResp, err := client.CreateUser(ctx, &pb.CreateUserRequest{
        Username: "gopher",
        Email:    "gopher@example.com",
        Profile: &pb.UserProfile{
            DisplayName: "Go Gopher",
            Bio:         "I love Go!",
        },
        Roles: []pb.UserRole{pb.UserRole_USER_ROLE_EDITOR},
    })
    if err != nil {
        log.Fatalf("사용자 생성 실패: %v", err)
    }
    log.Printf("생성된 사용자: %v", createResp.User)

    // 서버 스트리밍 RPC 호출
    stream, err := client.ListUsers(ctx, &pb.ListUsersRequest{
        PageSize: 10,
    })
    if err != nil {
        log.Fatalf("사용자 목록 조회 실패: %v", err)
    }

    for {
        user, err := stream.Recv()
        if err == io.EOF {
            break
        }
        if err != nil {
            log.Fatalf("스트림 수신 실패: %v", err)
        }
        log.Printf("사용자: %v", user)
    }
}
```

### 9.4 Python 서버 구현 예시

```python
import grpc
from concurrent import futures
from google.protobuf.timestamp_pb2 import Timestamp

import user_service_pb2 as pb
import user_service_pb2_grpc as pb_grpc


class UserServiceServicer(pb_grpc.UserServiceServicer):

    def __init__(self):
        self.users = {}

    def CreateUser(self, request, context):
        if not request.username:
            context.abort(
                grpc.StatusCode.INVALID_ARGUMENT,
                "username은 필수입니다"
            )

        user = pb.User(
            id="generated-uuid",
            username=request.username,
            email=request.email,
            profile=request.profile,
            status=pb.USER_STATUS_ACTIVE,
        )
        user.created_at.GetCurrentTime()

        self.users[user.id] = user
        return pb.CreateUserResponse(user=user)

    def GetUser(self, request, context):
        user = self.users.get(request.user_id)
        if not user:
            context.abort(
                grpc.StatusCode.NOT_FOUND,
                f"사용자 {request.user_id}를 찾을 수 없습니다"
            )
        return pb.GetUserResponse(user=user)

    def ListUsers(self, request, context):
        """서버 스트리밍"""
        for user in self.users.values():
            yield user


def serve():
    server = grpc.server(futures.ThreadPoolExecutor(max_workers=10))
    pb_grpc.add_UserServiceServicer_to_server(
        UserServiceServicer(), server
    )
    server.add_insecure_port("[::]:50051")
    server.start()
    print("gRPC 서버 시작: :50051")
    server.wait_for_termination()


if __name__ == "__main__":
    serve()
```

---

## 10. 활용 사례

### 10.1 마이크로서비스 간 내부 통신

gRPC의 가장 대표적인 활용 사례다.

```
[API Gateway]
     |
     |-- gRPC --> [User Service]
     |                |
     |                |-- gRPC --> [Auth Service]
     |
     |-- gRPC --> [Order Service]
     |                |
     |                |-- gRPC --> [Inventory Service]
     |                |-- gRPC --> [Payment Service]
     |
     |-- gRPC --> [Notification Service]
```

- 높은 성능: JSON/REST 대비 바이너리 직렬화로 처리량 증가
- 강력한 계약: `.proto` 파일이 서비스 간 계약서 역할
- 다언어 지원: Go 서비스와 Java 서비스, Python 서비스가 원활히 통신
- 데드라인 전파: 서비스 체인 전체에 걸친 타임아웃 관리

실제 사용 기업: Google, Netflix, Square, Dropbox, Uber, Slack, CoreOS(etcd), Cockroach Labs(CockroachDB)

### 10.2 모바일 애플리케이션

```
[iOS/Android App]
     |
     |-- gRPC (Protobuf) --> [Backend Services]
     |
     vs.
     |
     |-- REST (JSON) -------> [Backend Services]
```

gRPC가 모바일에 적합한 이유:
- 작은 페이로드: 제한된 모바일 네트워크에서 데이터 사용량 절감
- 빠른 직렬화: CPU, 배터리 절약
- 양방향 스트리밍: 실시간 기능 구현에 용이
- 코드 생성: iOS(Swift/Obj-C), Android(Java/Kotlin) 클라이언트 자동 생성

### 10.3 실시간 데이터 스트리밍

```
[IoT Sensors] ---> [gRPC Server Streaming] ---> [Dashboard]
[Market Data] ---> [gRPC Bidi Streaming]   ---> [Trading System]
[Game Client] ---> [gRPC Bidi Streaming]   ---> [Game Server]
```

- 주식 시세 스트리밍
- IoT 센서 데이터 수집
- 실시간 게임 서버
- 채팅 시스템
- 라이브 알림

### 10.4 서비스 메시 및 클라우드 네이티브

```
[Kubernetes Cluster]
     |
     |-- Envoy Sidecar -- gRPC -- Envoy Sidecar -- [Service A]
     |-- Envoy Sidecar -- gRPC -- Envoy Sidecar -- [Service B]
     |-- Envoy Sidecar -- gRPC -- Envoy Sidecar -- [Service C]
     |
     |-- [Istio Control Plane] (xDS protocol = gRPC)
```

- Kubernetes: 내부 컴포넌트(kubelet, API server) 간 gRPC 사용
- Istio/Envoy: 제어 평면과 데이터 평면 간 xDS 프로토콜 (gRPC 기반)
- etcd: Raft 합의 프로토콜에 gRPC 사용
- containerd: 컨테이너 런타임 API가 gRPC

### 10.5 데이터베이스 및 스토리지

| 시스템 | gRPC 활용 |
|--------|----------|
| CockroachDB | 노드 간 통신 |
| TiDB/TiKV | 분산 트랜잭션 및 데이터 전송 |
| etcd | 클라이언트 API 및 피어 통신 |
| Google Spanner | 클라이언트 라이브러리 |
| Google Bigtable | 클라이언트 라이브러리 |

### 10.6 ML/AI 서빙

```
[Client App]
     |
     |-- gRPC --> [TensorFlow Serving]
     |-- gRPC --> [Triton Inference Server]
     |-- gRPC --> [Seldon Core]
```

ML 모델 서빙에서 gRPC가 선호되는 이유:
- 대용량 텐서 데이터를 효율적으로 전송 (바이너리 인코딩)
- 낮은 지연시간 (실시간 추론)
- 스트리밍으로 배치 추론 결과 전달

---

## 참고 자료

- [gRPC 공식 문서](https://grpc.io/docs/)
- [Protocol Buffers 공식 문서](https://protobuf.dev/)
- [gRPC GitHub 저장소](https://github.com/grpc/grpc)
- [Protocol Buffers Language Guide (proto3)](https://protobuf.dev/programming-guides/proto3/)
- [Protocol Buffers Encoding](https://protobuf.dev/programming-guides/encoding/)
- [gRPC Core Concepts](https://grpc.io/docs/what-is-grpc/core-concepts/)
- [Buf - Modern Protobuf Tooling](https://buf.build/)
