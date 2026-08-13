# proto3 언어 가이드: 기본 문법

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

proto3 파일은 첫 줄에 문법 버전을 선언 → 이어서 메시지 정의.

```proto
syntax = "proto3";

message SearchRequest {
  string query = 1;
  int32 page_number = 2;
  int32 results_per_page = 3;
}
```

- 첫 줄 `syntax = "proto3";`은 이 파일이 proto3 문법을 사용함을 명시
- `message`는 필드들의 집합을 정의. 각 필드는 `타입 이름 = 번호;` 형태

---

## 필드 번호

각 필드에 고유한 번호를 지정 → 이 번호로 바이너리 형식에서 필드를 식별.

- 유효 범위: 1 ~ 536,870,911
- 예약 범위: 19,000 ~ 19,999는 Protocol Buffers 내부용으로 예약되어 사용 불가
- 최적화: 자주 설정되는 필드에는 1~15 사용 권장. 1~15는 1바이트로 인코딩(태그 + 와이어 타입), 16~2047은 2바이트
- 재사용 금지: 필드 번호는 절대 재사용 금지. 재사용하면 와이어 포맷 디코딩이 모호해짐

삭제한 필드 번호는 `reserved`로 예약 → 실수로 재사용되는 것 방지.

```proto
message Foo {
  reserved 2, 15, 9 to 11;
  reserved "foo", "bar";
}
```

> `9 to 11`은 9, 10, 11을 포함하는 닫힌 구간 표기.

---

## 필드 카디널리티 (singular / optional / repeated)

Singular(단일) — 0개 또는 1개의 값을 가짐.

- 암시적(implicit) presence: proto3 기본 동작. 필드를 설정했는지 여부를 추적하지 않으며, 미설정 시 기본값 반환
- `optional`: 명시적(explicit) presence. 필드 설정 여부를 추적 가능 → 설정된 경우에만 직렬화

```proto
message Example {
  string name = 1;            // 암시적 presence
  optional int32 age = 2;     // 명시적 presence (설정 여부 추적 가능)
}
```

Repeated(반복) — 같은 필드를 0개 이상 반복 가능(리스트).

```proto
message SearchResponse {
  repeated string results = 1;
}
```

- 스칼라 숫자 타입의 repeated 필드는 기본적으로 packed(압축) 형식으로 인코딩

Map(맵) — 키-값 쌍을 표현(자세한 내용은 03 문서 참고).

---

## 스칼라 값 타입

- `double`: Go 타입 `float64` — IEEE 754 배정밀도
- `float`: Go 타입 `float32` — IEEE 754 단정밀도
- `int32`: Go 타입 `int32` — 가변 길이, 음수에 비효율적
- `int64`: Go 타입 `int64` — 가변 길이, 음수에 비효율적
- `uint32`: Go 타입 `uint32` — 가변 길이
- `uint64`: Go 타입 `uint64` — 가변 길이
- `sint32`: Go 타입 `int32` — ZigZag 인코딩, 음수에 효율적
- `sint64`: Go 타입 `int64` — ZigZag 인코딩, 음수에 효율적
- `fixed32`: Go 타입 `uint32` — 항상 4바이트, 큰 값에 효율적
- `fixed64`: Go 타입 `uint64` — 항상 8바이트, 큰 값에 효율적
- `sfixed32`: Go 타입 `int32` — 항상 4바이트
- `sfixed64`: Go 타입 `int64` — 항상 8바이트
- `bool`: Go 타입 `bool` — 불리언
- `string`: Go 타입 `string` — UTF-8 또는 7비트 ASCII 텍스트
- `bytes`: Go 타입 `[]byte` — 임의의 바이트 시퀀스

> 음수를 자주 다루면 `int32`/`int64` 대신 `sint32`/`sint64`가 인코딩 효율 우수.

---

## 기본값

암시적 presence 필드가 메시지에 없을 때 적용되는 기본값.

- string: 빈 문자열 `""`
- bytes: 빈 바이트
- bool: `false`
- 숫자 타입: `0`
- enum: 정의된 첫 번째 값(반드시 0이어야 함)
- message: 미설정 상태(언어에 따라 다름. Go에서는 `nil`)
- repeated / map: 빈 컬렉션

암시적 presence에서는 기본값과 "미설정"을 구분 불가 → 구분이 필요하면 `optional` 사용.

---

## package

이름 충돌을 막기 위해 패키지 선언 가능.

```proto
syntax = "proto3";
package foo.bar;

message Open {}
```

- C++에서는 네임스페이스, Java에서는 패키지로 매핑
- Python은 패키지 선언을 무시
- Go에서는 패키지 선언과 별개로 `option go_package` 반드시 지정 필요(06 문서 참고)

---

## import

다른 `.proto` 파일의 정의를 가져옴.

```proto
import "myproject/other_protos.proto";
```

컴파일러는 `-I` / `--proto_path` 플래그로 지정한 디렉터리에서 파일 검색.

전이적(transitive) import가 필요하면 `import public` 사용.

```proto
import public "new.proto";
```

`import public` 사용 시 → 이 파일을 import하는 쪽에서도 `new.proto`의 정의 사용 가능.

---

## 주석

C/C++ 스타일의 한 줄(`//`)과 블록(`/* ... */`) 주석 사용.

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

기존 메시지를 안전하게 발전(evolve)시키기 위한 규칙.

안전한 변경(와이어 호환)

- 새 필드 추가
- 필드 삭제(번호는 반드시 `reserved` 처리)
- enum 값 추가
- 단일 필드를 새로운 oneof로 이동

위험한 변경(호환 깨짐)

- 기존 필드의 번호 변경
- 기존 oneof로 필드를 이동

조건부 호환(타입 변경, 데이터 손실 가능)

- `int32 ↔ uint32 ↔ int64 ↔ uint64 ↔ bool` 상호 변환
- `sint32 ↔ sint64` (다른 정수 타입과는 호환 불가)
- `string ↔ bytes` (유효한 UTF-8인 경우)
- string/bytes/message의 `singular ↔ repeated`
- `enum ↔ int32/uint32/int64/uint64`

> 알 수 없는 필드(unknown fields)는 파싱 시 보존되어 재직렬화할 때도 그대로 유지됨. 데이터 교환에는 텍스트 형식이 아닌 바이너리 형식 사용 권장.
