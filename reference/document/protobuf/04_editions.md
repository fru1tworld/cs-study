# Protobuf Editions

> 이 문서는 Protocol Buffers 공식 문서의 "Protobuf Editions" 가이드를 한국어로 정리한 것입니다.
> 원본: https://protobuf.dev/programming-guides/editions/

---

## 목차

1. [Editions란](#editions란)
2. [edition 선언](#edition-선언)
3. [기능 기반(feature) 모델](#기능-기반feature-모델)
4. [주요 features](#주요-features)
5. [features 설정 방법 (파일 / 메시지 / 필드)](#features-설정-방법-파일--메시지--필드)
6. [edition별 기본값](#edition별-기본값)
7. [proto2 / proto3에서 마이그레이션](#proto2--proto3에서-마이그레이션)

---

## Editions란

Protobuf Editions는 그동안 분리되어 있던 **proto2와 proto3 문법을 하나로 통합**한 현대적 방식입니다. 두 문법 버전 중 하나를 고르는 대신, **edition**(현재 2023, 2024)을 선택하고 필요한 **features**를 설정합니다.

이 방식으로 Protocol Buffers는 새로운 병렬 문법을 만들지 않고도 언어를 유연하게 진화시킬 수 있습니다. feature 시스템은 레거시 동작과 최신 모범 사례를 하나의 일관된 틀 안에서 모두 표현할 수 있게 해 줍니다.

---

## edition 선언

editions를 사용하는 `.proto` 파일은 첫 번째 비어 있지 않은(주석 제외) 줄에 edition을 선언해야 합니다.

```proto
edition = "2023";
```

`edition`도 `syntax`도 지정하지 않으면 컴파일러는 proto2 의미론(semantics)을 기본으로 적용합니다.

---

## 기능 기반(feature) 모델

proto2와 proto3는 동작이 문법에 하드코딩되어 있었습니다. editions는 이를 **설정 가능한 features**로 분리합니다. features는 세 가지 수준에서 설정할 수 있습니다.

- **파일 수준**: 파일 내 모든 메시지에 적용
- **메시지 수준**: 특정 메시지와 그 필드에 적용
- **필드 수준**: 개별 필드에 적용

좁은 범위의 설정이 넓은 범위의 설정을 재정의(override)합니다.

---

## 주요 features

### field_presence
단일 필드가 "명시적으로 설정되었는지" 추적하는지 제어합니다.

- `EXPLICIT`: presence 추적함(proto2 `optional`과 유사)
- `IMPLICIT`: presence 추적 안 함, 미설정 시 기본값 반환(proto3 동작)
- `LEGACY_REQUIRED`: required 강제(proto2 `required`를 마이그레이션한 형태)

### enum_type
enum 동작을 결정합니다.

- `OPEN`: 알 수 없는 값을 허용(proto3 스타일)
- `CLOSED`: 알 수 없는 값을 거부(proto2 스타일)

### repeated_field_encoding
repeated 스칼라 필드의 직렬화 방식을 제어합니다.

- `PACKED`: 압축 와이어 포맷(editions 기본값)
- `EXPANDED`: 전통적 방식, 요소마다 태그-값 쌍

### utf8_validation
string 필드가 파싱 시 유효한 UTF-8인지 검증합니다.

### message_encoding
메시지 직렬화 형식을 지정합니다(주로 proto2/proto3 호환).

### json_format
메시지의 JSON 직렬화 동작을 제어합니다.

---

## features 설정 방법 (파일 / 메시지 / 필드)

**파일 수준**

```proto
edition = "2023";

option features.field_presence = IMPLICIT;
option features.enum_type = OPEN;
```

**메시지 수준**

```proto
message MyMessage {
  option features.field_presence = EXPLICIT;
}
```

**필드 수준**

```proto
message MyMessage {
  int32 age = 1 [features.field_presence = EXPLICIT];
  repeated int32 samples = 4 [features.repeated_field_encoding = EXPANDED];
}
```

---

## edition별 기본값

**Edition 2023**의 기본값:

- `field_presence = EXPLICIT` (명시적 presence 추적)
- `enum_type = OPEN` (알 수 없는 enum 값 허용)
- `repeated_field_encoding = PACKED` (압축 인코딩)

**Edition 2024**는 위 기본값을 다듬고 field presence 의미론 개선 및 추가 feature 제어를 도입합니다.

---

## proto2 / proto3에서 마이그레이션

proto2/proto3 메시지 타입은 editions 메시지에서 import해 사용할 수 있고, 그 반대도 가능합니다. 마이그레이션은 보통 다음과 같이 진행합니다.

1. `syntax = "proto2";` 또는 `syntax = "proto3";`를 `edition = "2023";`으로 변경
2. 기존 동작을 보존하도록 features 설정
   - proto2 `required` → `field_presence = LEGACY_REQUIRED`
   - proto2 `optional` → `field_presence = EXPLICIT`
   - proto3 암시적 필드 → `field_presence = IMPLICIT`
3. proto2 호환이 필요하면 enum에 `enum_type = CLOSED` 적용

> 공식 도구 `protoc`의 `--edition_defaults`나 `prototiller` 같은 마이그레이션 도구가 변환을 돕습니다. editions의 핵심 장점은 하위 호환을 유지하면서, 병렬 문법 없이 언어를 진화시킬 수 있다는 점입니다.
