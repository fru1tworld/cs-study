# Protobuf Editions

> 원본: https://protobuf.dev/programming-guides/editions/

<a id="editions란"></a>
## Editions란

Protobuf Editions는 proto2와 proto3로 나뉘어 있던 문법을 하나로 통합한다. 두 문법 중 하나를 고르는 대신 edition(현재 2023, 2024)을 선택하고 필요한 features를 설정한다.

이 feature 시스템은 레거시 동작과 최신 모범 사례를 같은 틀 안에서 표현한다. 따라서 새 동작을 도입할 때마다 별도의 병렬 문법을 추가하지 않고도 언어를 발전시킬 수 있다.

<a id="edition-선언"></a>

## edition 선언

- editions를 사용하는 `.proto` 파일은 첫 번째 비주석 비공백 줄에 edition을 선언해야 함

```proto
edition = "2023";
```

- `edition`도 `syntax`도 지정하지 않으면 컴파일러는 proto2 의미론(semantics)을 기본으로 적용함

<a id="기능-기반feature-모델"></a>

## 기능 기반(feature) 모델

proto2와 proto3에서는 문법을 선택하면 그에 따른 동작도 함께 정해졌다. Editions는 이 동작을 설정 가능한 features로 분리해, 다음 세 가지 수준에서 선택할 수 있게 한다.

- 파일 수준: 파일 내 모든 메시지에 적용
- 메시지 수준: 특정 메시지와 그 필드에 적용
- 필드 수준: 개별 필드에 적용

좁은 범위의 설정은 넓은 범위의 설정을 재정의(override)한다. 파일 전체의 기본 동작을 정한 뒤, 필요한 메시지나 필드만 다르게 설정할 수 있다.

<a id="주요-features"></a>

## 주요 features

### field_presence

- 단일 필드의 "명시적 설정 여부" 추적을 제어함

- `EXPLICIT`: presence 추적함(proto2 `optional`과 유사)
- `IMPLICIT`: presence 추적 안 함 → 미설정 시 기본값 반환(proto3 동작)
- `LEGACY_REQUIRED`: required 강제(proto2 `required`를 마이그레이션한 형태)

### enum_type

- enum 동작을 결정함

- `OPEN`: 알 수 없는 값을 허용(proto3 스타일)
- `CLOSED`: 알 수 없는 값을 거부(proto2 스타일)

### repeated_field_encoding

- repeated 스칼라 필드의 직렬화 방식을 제어함

- `PACKED`: 압축 와이어 포맷(editions 기본값)
- `EXPANDED`: 전통적 방식, 요소마다 태그-값 쌍

### utf8_validation

- string 필드가 파싱 시 유효한 UTF-8인지 검증함

### message_encoding

- 메시지 직렬화 형식을 지정함(주로 proto2/proto3 호환 용도).

### json_format

- 메시지의 JSON 직렬화 동작을 제어함

<a id="features-설정-방법-파일--메시지--필드"></a>

## features 설정 방법 (파일 / 메시지 / 필드)

- 파일 수준

```proto
edition = "2023";

option features.field_presence = IMPLICIT;
option features.enum_type = OPEN;
```

- 메시지 수준

```proto
message MyMessage {
  option features.field_presence = EXPLICIT;
}
```

- 필드 수준

```proto
message MyMessage {
  int32 age = 1 [features.field_presence = EXPLICIT];
  repeated int32 samples = 4 [features.repeated_field_encoding = EXPANDED];
}
```

<a id="edition별-기본값"></a>

## edition별 기본값

- Edition 2023의 기본값:

- `field_presence = EXPLICIT` (명시적 presence 추적)
- `enum_type = OPEN` (알 수 없는 enum 값 허용)
- `repeated_field_encoding = PACKED` (압축 인코딩)

- Edition 2024는 위 기본값을 다듬고 field presence 의미론 개선 및 추가 feature 제어를 도입함

<a id="proto2--proto3에서-마이그레이션"></a>

## proto2 / proto3에서 마이그레이션

proto2/proto3 메시지 타입은 editions 메시지에서 import해 사용할 수 있으며, 반대 방향도 가능하다. 기존 파일을 editions로 옮길 때는 선언만 바꾸는 데 그치지 않고, 기존 동작에 맞는 features를 함께 설정한다.

- 1\. `syntax = "proto2";` 또는 `syntax = "proto3";`를 `edition = "2023";`으로 변경
- 2\. 기존 동작을 보존하도록 features 설정
  - proto2 `required` → `field_presence = LEGACY_REQUIRED`
  - proto2 `optional` → `field_presence = EXPLICIT`
  - proto3 암시적 필드 → `field_presence = IMPLICIT`
- 3\. proto2 호환이 필요하면 enum에 `enum_type = CLOSED` 적용

> 공식 도구 `protoc`의 `--edition_defaults`나 `prototiller` 같은 마이그레이션 도구가 변환을 도움. editions의 핵심 장점은 하위 호환을 유지하면서, 병렬 문법 없이 언어를 진화시킬 수 있다는 점.
