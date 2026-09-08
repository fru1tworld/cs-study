# proto3 언어 가이드: 고급 메시지 구성

> 원본: https://protobuf.dev/programming-guides/proto3/

<a id="열거형-enum"></a>
## 열거형 (enum)

- 미리 정의된 값들의 집합을 나타냄

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

- 규칙

- 첫 번째 값은 반드시 0이어야 함 → 기본값으로 사용됨
- 첫 값의 이름은 `ENUM_TYPE_NAME_UNSPECIFIED` 또는 `..._UNKNOWN` 권장
- enum 값은 32비트 정수 범위 내여야 함

- 별칭(alias): 같은 값에 여러 이름을 부여하려면 `allow_alias`를 켬

```proto
enum EnumAllowingAlias {
  option allow_alias = true;
  EAA_UNSPECIFIED = 0;
  EAA_STARTED = 1;
  EAA_RUNNING = 1;   // EAA_STARTED와 같은 값(별칭)
  EAA_FINISHED = 2;
}
```

- 예약 값: 삭제한 enum 항목의 재사용을 막음

```proto
enum Foo {
  reserved 2, 15, 9 to 11, 40 to max;
  reserved "FOO", "BAR";
}
```

<a id="중첩-타입-nested-types"></a>

## 중첩 타입 (Nested Types)

- 메시지 안에 다른 메시지를 정의 가능 → 점(`.`) 표기로 참조

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

<a id="oneof"></a>

## oneof

여러 필드 중 최대 하나만 설정할 수 있어야 한다면 `oneof`로 묶는다. 아래의 `test_oneof`에는 `name`과 `sub_message` 중 하나만 설정할 수 있으며, 다른 필드를 설정하면 기존 필드는 자동으로 해제된다.

```proto
message SampleMessage {
  oneof test_oneof {
    string name = 4;
    SubMessage sub_message = 9;
  }
}
```

- 규칙

- `map`이나 `repeated` 필드는 oneof에 넣기 금지
- 여러 값이 설정되면 마지막에 설정한 값이 이전 값을 덮어씀
- 어떤 필드가 설정됐는지는 (Go에서는) 타입 스위치로 확인
- 와이어에 oneof 멤버가 여러 개 나타나면 파서는 기존 멤버를 해제하고 마지막 값을 적용 → 원시 값은 덮어쓰고, 메시지 필드는 병합

<a id="map"></a>

## map

- 키-값 쌍 필드를 간단한 문법으로 정의

```proto
message Project { /* ... */ }

message Example {
  map<string, Project> projects = 3;
}
```

- 제약

- 키 타입: 정수형 또는 문자열 스칼라 타입(float, bytes, enum, message는 불가)
- 값 타입: 또 다른 map을 제외한 모든 타입
- map 필드는 `repeated` 불가
- 와이어상의 순서와 맵 순회 순서는 정의되지 않음(undefined)

- map은 와이어 포맷상 다음과 동등함

```proto
message MapFieldEntry {
  key_type key = 1;
  value_type value = 2;
}
repeated MapFieldEntry map_field = N;
```

<a id="any"></a>

## Any

- `.proto` 정의 없이도 임의의 메시지 타입을 담을 수 있는 타입

```proto
import "google/protobuf/any.proto";

message ErrorStatus {
  string message = 1;
  repeated google.protobuf.Any details = 2;
}
```

- 기본 타입 URL: `type.googleapis.com/패키지명.메시지명`
- 사용 시 언어별 pack/unpack 메서드로 실제 메시지를 직렬화, 역직렬화 → Go에서는 `anypb.New`, `(*anypb.Any).UnmarshalTo` 등을 사용

<a id="예약-reserved"></a>

## 예약 (reserved)

- 삭제한 필드 번호와 이름이 미래에 실수로 재사용되는 것을 막음

```proto
message Foo {
  reserved 2, 15, 9 to 11;
  reserved "foo", "bar";
}
```

예약한 번호나 이름을 다시 사용하면 컴파일러가 오류를 낸다. 번호와 이름은 같은 `reserved` 문에 섞어 쓸 수 없으므로, 위 예제처럼 별도 줄로 선언한다.

<a id="well-known-types"></a>

## Well-Known Types

시각이나 시간 간격처럼 여러 메시지에서 공통으로 쓰는 값에는 구글이 제공하는 표준 메시지 타입을 재사용한다. 아래 타입은 `import "google/protobuf/..."`로 가져올 수 있다.

- `Timestamp`
  - import 경로: `google/protobuf/timestamp.proto`
  - 용도: 시각(UTC 기준 초+나노초)
- `Duration`
  - import 경로: `google/protobuf/duration.proto`
  - 용도: 시간 간격
- `Any`
  - import 경로: `google/protobuf/any.proto`
  - 용도: 임의의 메시지 래핑
- `Struct`
  - import 경로: `google/protobuf/struct.proto`
  - 용도: JSON 유사 동적 구조
- `Value` / `ListValue`
  - import 경로: `google/protobuf/struct.proto`
  - 용도: 동적 값 / 값 목록
- `Empty`
  - import 경로: `google/protobuf/empty.proto`
  - 용도: 빈 메시지(RPC 반환 등)
- `FieldMask`
  - import 경로: `google/protobuf/field_mask.proto`
  - 용도: 부분 갱신 대상 필드 지정
- Wrappers (`Int32Value`, `StringValue`, `BoolValue` 등)
  - import 경로: `google/protobuf/wrappers.proto`
  - 용도: 스칼라에 presence 부여(null 구분)

```proto
import "google/protobuf/timestamp.proto";
import "google/protobuf/wrappers.proto";

message Event {
  string name = 1;
  google.protobuf.Timestamp created_at = 2;
  google.protobuf.Int32Value retry_count = 3;  // null과 0을 구분
}
```

> Wrapper 타입은 proto3에서 스칼라 값의 "설정됨 vs 미설정"을 구분하기 위해 쓰였음. proto3에서는 `optional`로도 같은 효과를 얻을 수 있음.

<a id="서비스-정의-rpc"></a>

## 서비스 정의 (rpc)

- RPC 엔드포인트를 `service`로 정의

```proto
service SearchService {
  rpc Search(SearchRequest) returns (SearchResponse);
}
```

protobuf와 함께 사용하는 RPC 시스템으로는 gRPC가 권장된다. 서비스 정의를 실행 가능한 코드로 만들려면 컴파일러 외에 별도의 코드 생성 플러그인이 필요하다. 예를 들어 Go에서는 메시지 코드를 만드는 `protoc-gen-go`와 별도로 `protoc-gen-go-grpc`를 사용한다.

- 스트리밍(streaming) RPC도 정의 가능

```proto
service ChatService {
  rpc ServerStream(Request) returns (stream Response);
  rpc ClientStream(stream Request) returns (Response);
  rpc BiDiStream(stream Request) returns (stream Response);
}
```
