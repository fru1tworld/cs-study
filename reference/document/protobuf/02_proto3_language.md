# proto3 언어 가이드: 기본 문법

> 이 문서는 Protocol Buffers 공식 문서의 "Language Guide (proto3)" 중 기본 문법을 한국어로 정리한 것입니다.
> 원본: https://protobuf.dev/programming-guides/proto3/

---

## 목차

1. [메시지 정의](#메시지-정의)
2. [필드 번호](#필드-번호)
3. [필드 카디널리티 (singular / optional / repeated)](#필드-카디널리티-singular--optional--repeated)
4. [스칼라 값 타입](#스칼라-값-타입)
5. [기본값](#기본값)
6. [package](#package)
7. [import](#import)
8. [주석](#주석)
9. [메시지 타입 갱신과 호환성](#메시지-타입-갱신과-호환성)

---

## 메시지 정의

proto3 파일은 첫 줄에 문법 버전을 선언하고, 이어서 메시지를 정의합니다.

```proto
syntax = "proto3";

message SearchRequest {
  string query = 1;
  int32 page_number = 2;
  int32 results_per_page = 3;
}
```

- 첫 줄 `syntax = "proto3";`은 이 파일이 proto3 문법을 사용함을 명시합니다.
- `message`는 필드들의 집합을 정의합니다. 각 필드는 `타입 이름 = 번호;` 형태입니다.

---

## 필드 번호

각 필드에는 고유한 번호를 지정하며, 이 번호는 바이너리 형식에서 필드를 식별합니다.

- **유효 범위**: 1 ~ 536,870,911
- **예약 범위**: 19,000 ~ 19,999는 Protocol Buffers 내부용으로 예약되어 사용할 수 없습니다.
- **최적화**: 자주 설정되는 필드에는 1~15를 사용하세요. 1~15는 1바이트로 인코딩됩니다(태그 + 와이어 타입). 16~2047은 2바이트입니다.
- **재사용 금지**: 필드 번호는 절대 재사용하면 안 됩니다. 재사용하면 와이어 포맷 디코딩이 모호해집니다.

삭제한 필드 번호는 `reserved`로 예약하여 실수로 재사용되는 것을 막습니다.

```proto
message Foo {
  reserved 2, 15, 9 to 11;
  reserved "foo", "bar";
}
```

> `9 to 11`은 9, 10, 11을 포함하는 닫힌 구간 표기입니다.

---

## 필드 카디널리티 (singular / optional / repeated)

**Singular(단일)** — 0개 또는 1개의 값을 가집니다.

- 암시적(implicit) presence: proto3 기본 동작. 필드를 설정했는지 여부를 추적하지 않으며, 미설정 시 기본값을 반환합니다.
- `optional`: 명시적(explicit) presence. 필드가 설정되었는지 여부를 추적할 수 있으며, 설정된 경우에만 직렬화됩니다.

```proto
message Example {
  string name = 1;            // 암시적 presence
  optional int32 age = 2;     // 명시적 presence (설정 여부 추적 가능)
}
```

**Repeated(반복)** — 같은 필드를 0개 이상 반복할 수 있습니다(리스트).

```proto
message SearchResponse {
  repeated string results = 1;
}
```

- 스칼라 숫자 타입의 repeated 필드는 기본적으로 **packed**(압축) 형식으로 인코딩됩니다.

**Map(맵)** — 키-값 쌍을 표현합니다(자세한 내용은 03 문서 참조).

---

## 스칼라 값 타입

| .proto 타입 | Go 타입 | 비고 |
|------------|---------|------|
| double | float64 | IEEE 754 배정밀도 |
| float | float32 | IEEE 754 단정밀도 |
| int32 | int32 | 가변 길이. 음수에 비효율적 |
| int64 | int64 | 가변 길이. 음수에 비효율적 |
| uint32 | uint32 | 가변 길이 |
| uint64 | uint64 | 가변 길이 |
| sint32 | int32 | ZigZag 인코딩. 음수에 효율적 |
| sint64 | int64 | ZigZag 인코딩. 음수에 효율적 |
| fixed32 | uint32 | 항상 4바이트. 큰 값에 효율적 |
| fixed64 | uint64 | 항상 8바이트. 큰 값에 효율적 |
| sfixed32 | int32 | 항상 4바이트 |
| sfixed64 | int64 | 항상 8바이트 |
| bool | bool | 불리언 |
| string | string | UTF-8 또는 7비트 ASCII 텍스트 |
| bytes | []byte | 임의의 바이트 시퀀스 |

> 음수를 자주 다루면 `int32`/`int64` 대신 `sint32`/`sint64`가 인코딩 효율이 좋습니다.

---

## 기본값

암시적 presence 필드가 메시지에 없을 때 적용되는 기본값입니다.

- **string**: 빈 문자열 `""`
- **bytes**: 빈 바이트
- **bool**: `false`
- **숫자 타입**: `0`
- **enum**: 정의된 첫 번째 값(반드시 0이어야 함)
- **message**: 미설정 상태(언어에 따라 다름. Go에서는 `nil`)
- **repeated / map**: 빈 컬렉션

암시적 presence에서는 기본값과 "미설정"을 구분할 수 없습니다. 구분이 필요하면 `optional`을 사용하세요.

---

## package

이름 충돌을 막기 위해 패키지를 선언할 수 있습니다.

```proto
syntax = "proto3";
package foo.bar;

message Open {}
```

- C++에서는 네임스페이스, Java에서는 패키지로 매핑됩니다.
- Python은 패키지 선언을 무시합니다.
- **Go에서는** 패키지 선언과 별개로 `option go_package`를 반드시 지정해야 합니다(06 문서 참조).

---

## import

다른 `.proto` 파일의 정의를 가져옵니다.

```proto
import "myproject/other_protos.proto";
```

컴파일러는 `-I` / `--proto_path` 플래그로 지정한 디렉터리에서 파일을 찾습니다.

전이적(transitive) import가 필요하면 `import public`을 사용합니다.

```proto
import public "new.proto";
```

`import public`을 쓰면, 이 파일을 import한 쪽에서도 `new.proto`의 정의를 사용할 수 있습니다.

---

## 주석

C/C++ 스타일의 한 줄(`//`)과 블록(`/* ... */`) 주석을 사용합니다.

```proto
/**
 * SearchRequest는 페이지네이션 옵션을 포함한
 * 검색 질의를 표현합니다.
 */
message SearchRequest {
  string query = 1;  // 검색 질의 문자열
}
```

---

## 메시지 타입 갱신과 호환성

기존 메시지를 안전하게 진화(evolve)시키기 위한 규칙입니다.

**안전한 변경(와이어 호환)**

- 새 필드 추가
- 필드 삭제(번호는 반드시 `reserved` 처리)
- enum 값 추가
- 단일 필드를 새로운 oneof로 이동

**위험한 변경(호환 깨짐)**

- 기존 필드의 번호 변경
- 기존 oneof에 필드를 추가/이동

**조건부 호환(타입 변경, 데이터 손실 가능)**

- `int32 ↔ uint32 ↔ int64 ↔ uint64 ↔ bool` 상호 변환
- `sint32 ↔ sint64` (다른 정수 타입과는 호환되지 않음)
- `string ↔ bytes` (유효한 UTF-8인 경우)
- string/bytes/message의 `singular ↔ repeated`
- `enum ↔ int32/uint32/int64/uint64`

> 알 수 없는 필드(unknown fields)는 파싱 시 보존되었다가 재직렬화 시 그대로 유지됩니다. 데이터 교환에는 텍스트 형식이 아닌 바이너리 형식을 사용하세요.
