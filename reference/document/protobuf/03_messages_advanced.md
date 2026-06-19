# proto3 언어 가이드: 고급 메시지 구성

> 이 문서는 Protocol Buffers 공식 문서의 "Language Guide (proto3)" 중 고급 기능을 한국어로 정리한 것입니다.
> 원본: https://protobuf.dev/programming-guides/proto3/

---

## 목차

1. [열거형 (enum)](#열거형-enum)
2. [중첩 타입 (Nested Types)](#중첩-타입-nested-types)
3. [oneof](#oneof)
4. [map](#map)
5. [Any](#any)
6. [예약 (reserved)](#예약-reserved)
7. [Well-Known Types](#well-known-types)
8. [서비스 정의 (rpc)](#서비스-정의-rpc)

---

## 열거형 (enum)

미리 정의된 값들의 집합을 나타냅니다.

```proto
enum Corpus {
  CORPUS_UNSPECIFIED = 0;
  CORPUS_UNIVERSAL = 1;
  CORPUS_WEB = 2;
}

message SearchRequest {
  Corpus corpus = 4;
}
```

**규칙**

- 첫 번째 값은 **반드시 0**이어야 하며, 이는 기본값으로 사용됩니다.
- 첫 값의 이름은 `ENUM_TYPE_NAME_UNSPECIFIED` 또는 `..._UNKNOWN`을 권장합니다.
- enum 값은 32비트 정수 범위 내여야 합니다.

**별칭(alias)** — 같은 값에 여러 이름을 부여하려면 `allow_alias`를 켭니다.

```proto
enum EnumAllowingAlias {
  option allow_alias = true;
  EAA_UNSPECIFIED = 0;
  EAA_STARTED = 1;
  EAA_RUNNING = 1;   // EAA_STARTED와 같은 값(별칭)
  EAA_FINISHED = 2;
}
```

**예약 값** — 삭제한 enum 항목의 재사용을 막습니다.

```proto
enum Foo {
  reserved 2, 15, 9 to 11, 40 to max;
  reserved "FOO", "BAR";
}
```

---

## 중첩 타입 (Nested Types)

메시지 안에 다른 메시지를 정의할 수 있습니다. 점(`.`) 표기로 참조합니다.

```proto
message SearchResponse {
  message Result {
    string url = 1;
    string title = 2;
    repeated string snippets = 3;
  }
  repeated Result results = 1;
}

message SomeOtherMessage {
  SearchResponse.Result result = 1;  // 외부에서 참조
}
```

---

## oneof

여러 필드 중 **최대 하나만** 설정될 수 있는 그룹입니다. 하나를 설정하면 나머지는 자동으로 해제됩니다.

```proto
message SampleMessage {
  oneof test_oneof {
    string name = 4;
    SubMessage sub_message = 9;
  }
}
```

**규칙**

- `map`이나 `repeated` 필드는 oneof에 넣을 수 없습니다.
- 여러 값이 설정되면 마지막에 설정한 값이 이전 값을 덮어씁니다.
- 어떤 필드가 설정됐는지는 (Go에서는) 타입 스위치로 확인합니다.
- 와이어상에 oneof 멤버가 여러 개 나타나면, 파서는 다른 멤버가 설정돼 있으면 해제하고, 마지막 것을 적용합니다(원시 값은 덮어쓰고, 메시지는 병합).

---

## map

키-값 쌍 필드를 간단한 문법으로 정의합니다.

```proto
message Project { /* ... */ }

message Example {
  map<string, Project> projects = 3;
}
```

**제약**

- 키 타입: 정수형 또는 문자열 스칼라 타입(float, bytes, enum, message는 불가).
- 값 타입: 또 다른 map을 제외한 모든 타입.
- map 필드는 `repeated`일 수 없습니다.
- 와이어상의 순서와 맵 순회 순서는 정의되지 않습니다(undefined).

map은 와이어 포맷상 다음과 동등합니다.

```proto
message MapFieldEntry {
  key_type key = 1;
  value_type value = 2;
}
repeated MapFieldEntry map_field = N;
```

---

## Any

`.proto` 정의 없이도 임의의 메시지 타입을 담을 수 있는 타입입니다.

```proto
import "google/protobuf/any.proto";

message ErrorStatus {
  string message = 1;
  repeated google.protobuf.Any details = 2;
}
```

- 기본 타입 URL: `type.googleapis.com/패키지명.메시지명`
- 사용할 때는 언어별 pack/unpack 메서드로 실제 메시지를 넣고 꺼냅니다. Go에서는 `anypb.New` / `(*anypb.Any).UnmarshalTo` 등을 사용합니다.

---

## 예약 (reserved)

삭제한 필드 번호와 이름이 미래에 실수로 재사용되는 것을 막습니다.

```proto
message Foo {
  reserved 2, 15, 9 to 11;
  reserved "foo", "bar";
}
```

이후 누군가 예약된 번호나 이름을 다시 쓰려고 하면 컴파일러가 오류를 냅니다. 같은 `reserved` 문에 번호와 이름을 섞어 쓸 수는 없으며 별도 줄로 작성합니다.

---

## Well-Known Types

구글이 제공하는 표준 메시지 타입입니다. `import "google/protobuf/..."`로 가져와 사용합니다. 직접 비슷한 타입을 만들지 말고 이것을 재사용하세요.

| 타입 | import 경로 | 용도 |
|------|------------|------|
| `Timestamp` | `google/protobuf/timestamp.proto` | 시각(UTC 기준 초+나노초) |
| `Duration` | `google/protobuf/duration.proto` | 시간 간격 |
| `Any` | `google/protobuf/any.proto` | 임의의 메시지 래핑 |
| `Struct` | `google/protobuf/struct.proto` | JSON 유사 동적 구조 |
| `Value` / `ListValue` | `google/protobuf/struct.proto` | 동적 값 / 값 목록 |
| `Empty` | `google/protobuf/empty.proto` | 빈 메시지(RPC 반환 등) |
| `FieldMask` | `google/protobuf/field_mask.proto` | 부분 갱신 대상 필드 지정 |
| Wrappers (`Int32Value`, `StringValue`, `BoolValue` 등) | `google/protobuf/wrappers.proto` | 스칼라에 presence 부여(null 구분) |

```proto
import "google/protobuf/timestamp.proto";
import "google/protobuf/wrappers.proto";

message Event {
  string name = 1;
  google.protobuf.Timestamp created_at = 2;
  google.protobuf.Int32Value retry_count = 3;  // null과 0을 구분
}
```

> Wrapper 타입은 proto3에서 스칼라 값의 "설정됨 vs 미설정"을 구분하기 위해 쓰였습니다. proto3에서는 `optional`로도 같은 효과를 얻을 수 있습니다.

---

## 서비스 정의 (rpc)

RPC 엔드포인트를 `service`로 정의합니다.

```proto
service SearchService {
  rpc Search(SearchRequest) returns (SearchResponse);
}
```

- protobuf와 함께 권장되는 RPC 시스템은 **gRPC**입니다.
- 서비스 코드는 컴파일러만으로는 생성되지 않으며, 별도의 코드 생성 플러그인(예: `protoc-gen-go-grpc`)이 필요합니다. 기본 `protoc-gen-go`는 서비스에 대한 출력을 만들지 않습니다.

스트리밍(streaming) RPC도 정의할 수 있습니다.

```proto
service ChatService {
  rpc ServerStream(Request) returns (stream Response);
  rpc ClientStream(stream Request) returns (Response);
  rpc BiDiStream(stream Request) returns (stream Response);
}
```
