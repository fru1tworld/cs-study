# 스타일 가이드 & Do's and Don'ts

> 이 문서는 Protocol Buffers 공식 문서의 "Style Guide"와 "Proto Best Practices(Do's and Don'ts)"를 한국어로 정리한 것입니다.
> 원본: https://protobuf.dev/programming-guides/style/ , https://protobuf.dev/programming-guides/dos-donts/

---

## 목차

1. [파일 이름과 구조](#파일-이름과-구조)
2. [포맷팅 규칙](#포맷팅-규칙)
3. [네이밍 규칙](#네이밍-규칙)
4. [피해야 할 것 (스타일)](#피해야-할-것-스타일)
5. [Don'ts (하지 말 것)](#donts-하지-말-것)
6. [Dos (해야 할 것)](#dos-해야-할-것)
7. [요약 체크리스트](#요약-체크리스트)

---

## 파일 이름과 구조

- 파일 이름은 `lower_snake_case.proto`를 사용합니다.
- 파일 내용은 다음 순서로 작성합니다.
  1. 라이선스 헤더(있다면)
  2. 파일 개요(주석)
  3. `syntax` 또는 `edition` 선언
  4. `package` 선언
  5. `import`(정렬된 상태로)
  6. 파일 옵션(`option ...`)
  7. 그 외 모든 정의(메시지, enum, 서비스 등)

---

## 포맷팅 규칙

- 한 줄 길이는 **80자**로 유지합니다.
- 들여쓰기는 **공백 2칸**을 사용합니다.
- 문자열은 큰따옴표(`"`)를 사용합니다.

---

## 네이밍 규칙

**메시지** — PascalCase(TitleCase).

```proto
message SongRequest {}
```

**필드** — snake_case. repeated 필드는 복수형 이름.

```proto
string song_name = 1;
repeated Song songs = 2;
```

**oneof** — lower_snake_case.

```proto
oneof song_id {
  string song_human_readable_id = 1;
  int64 song_machine_id = 2;
}
```

**enum** — 타입 이름은 PascalCase, 값은 UPPER_SNAKE_CASE.

```proto
enum FooBar {
  FOO_BAR_UNSPECIFIED = 0;
  FOO_BAR_FIRST_VALUE = 1;
}
```

- 첫 값은 0이며 `_UNSPECIFIED` 또는 `_UNKNOWN` 접미사를 권장합니다.
- 모든 값에 enum 이름(UPPER_SNAKE_CASE 변환)을 접두사로 붙여 충돌을 방지합니다.

**package** — 점으로 구분된 lower_snake_case. 대문자나 Java 스타일 네이밍은 피합니다.

**service / rpc** — 둘 다 PascalCase.

```proto
service FooService {
  rpc GetSomething(GetSomethingRequest) returns (GetSomethingResponse);
}
```

---

## 피해야 할 것 (스타일)

- `required` 필드(스키마 진화에 해로움)
- group(deprecated, 대신 중첩 메시지 사용)
- 문제를 일으키는 접두사를 가진 필드/oneof 이름: `has_`, `get_`, `set_`, `clear_`
- `_value`로 끝나는 필드/oneof 이름
- `descriptor`라는 이름, 각 언어의 예약어

---

## Don'ts (하지 말 것)

- **필드 번호를 재사용하지 말 것**: 필드 번호는 바이너리 형식에서 필드를 식별합니다. 재사용하면 구버전 메시지를 새 코드로 역직렬화할 때 데이터가 손상됩니다.
- **삭제한 필드를 다시 태깅하지 말 것**: 필드를 제거하면 그 번호를 `reserved`로 예약해 향후 실수로 재사용되는 것을 막아야 합니다.
- **필드 타입을 바꾸지 말 것**: 버전 간 타입 변경은 역직렬화를 깨고 조용한 데이터 손실/손상을 유발합니다.
- **required 필드를 추가하지 말 것**: 하위 호환을 깨뜨립니다. 새 required 필드는 그것을 제공하지 못하는 기존 코드를 망가뜨립니다.
- **필드가 과도하게 많은 메시지를 만들지 말 것**: 가독성, 유지보수성, 성능을 해치는 나쁜 설계 신호입니다.
- **언어 예약어를 쓰지 말 것**: C/C++ 등의 예약어와 겹치는 필드 이름은 컴파일 오류와 네임스페이스 충돌을 일으킵니다.
- **불리언 이름을 모호하게 짓지 말 것**: 의도가 분명한 이름을 사용해 상태에 대한 혼란을 줄입니다.
- **기본값을 바꾸지 말 것**: 기존 기본값에 의존하던 시스템의 의미론을 예기치 않게 바꿉니다.
- **텍스트 형식 메시지를 데이터 교환에 쓰지 말 것**: 텍스트 형식은 사람이 읽기 위한 용도이며, 시스템 간 교환에는 바이너리 형식을 사용합니다.

---

## Dos (해야 할 것)

- **삭제한 필드 번호를 예약할 것**: 제거한 필드 번호를 명시적으로 `reserved` 처리해 재사용을 방지하고 호환성을 유지합니다.
- **enum 값 0을 포함할 것**: 0을 unspecified/unknown 상태로 예약해 미설정 값을 버전 간에 안전하게 처리합니다.
- **well-known/공통 타입을 사용할 것**: `Timestamp`, `Duration`, `Any` 같은 표준 타입을 활용해 일관성과 상호운용성을 높이고 커스텀 코드를 줄입니다.
- **네이밍 규칙을 따를 것**: 메시지/필드/서비스에 일관된 명명 패턴을 적용해 명확성과 언어 간 사용성을 확보합니다.

---

## 요약 체크리스트

- [ ] 파일명은 `lower_snake_case.proto`, 들여쓰기 2칸, 80자 이내
- [ ] 메시지는 PascalCase, 필드는 snake_case, repeated는 복수형
- [ ] enum 첫 값은 `0` + `_UNSPECIFIED`, 값에 enum 이름 접두사
- [ ] 필드 번호 재사용 금지, 삭제 시 `reserved`
- [ ] 필드 타입/기본값 변경 금지
- [ ] `required`와 group 사용 금지
- [ ] 시각/기간/동적 데이터는 well-known types 재사용
- [ ] 데이터 교환은 바이너리 형식 사용
