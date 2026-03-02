# 부호 없는 정수 타입

## 개요

Kotlin은 표준 정수 타입 외에 네 가지 부호 없는 정수 타입을 제공합니다:

| 타입 | 크기 (비트) | 최소값 | 최대값 |
|------|-----------|-------|-------|
| `UByte` | 8 | 0 | 255 |
| `UShort` | 16 | 0 | 65,535 |
| `UInt` | 32 | 0 | 4,294,967,295 (2^32 - 1) |
| `ULong` | 64 | 0 | 18,446,744,073,709,551,615 (2^64 - 1) |

부호 없는 타입은 해당 부호 있는 타입의 대부분의 연산을 지원하며, 해당 부호 있는 타입을 포함하는 단일 저장 프로퍼티를 가진 인라인 클래스로 구현됩니다.

---

## 부호 없는 배열과 범위

**참고:** 부호 없는 배열은 베타 버전이며 `@ExperimentalUnsignedTypes` 어노테이션을 통한 옵트인이 필요합니다.

### 배열 타입
- `UByteArray` - 부호 없는 바이트 배열
- `UShortArray` - 부호 없는 short 배열
- `UIntArray` - 부호 없는 int 배열
- `ULongArray` - 부호 없는 long 배열

### 범위 지원

`UInt`와 `ULong`에 대해 범위와 진행이 지원됩니다:
- `UIntRange`
- `UIntProgression`
- `ULongRange`
- `ULongProgression`

이것들은 안정적인 기능입니다.

---

## 부호 없는 정수 리터럴

### 접미사 표기법

접미사를 사용하여 부호 없는 리터럴을 지정합니다:

```kotlin
// 'u' 또는 'U' - 컨텍스트에 따라 타입 추론
val b: UByte = 1u          // UByte (예상 타입 제공)
val s: UShort = 1u         // UShort (예상 타입 제공)
val l: ULong = 1u          // ULong (예상 타입 제공)
val a1 = 42u               // UInt (예상 타입 없음, UInt에 맞음)
val a2 = 0xFFFF_FFFF_FFFFu // ULong (UInt에 맞지 않음)

// 'uL' 또는 'UL' - 명시적으로 부호 없는 long
val a = 1UL // ULong
```

---

## 사용 사례

### 16진수 색상 표현

```kotlin
data class Color(val representation: UInt)
val yellow = Color(0xFFCC00CCu)
```

### 바이트 배열 초기화

```kotlin
val byteOrderMarkUtf8 = ubyteArrayOf(0xEFu, 0xBBu, 0xBFu)
```

### 네이티브 API 상호 운용성

Kotlin 함수 시그니처의 부호 없는 타입은 대체 없이 네이티브 선언에 직접 매핑되어 의미를 보존합니다.

---

## 비목표

부호 없는 정수는 다음 용도로 **의도되지 않았습니다:**
- 컬렉션 크기나 인덱스 (부호 있는 정수 사용)
- 음이 아닌 도메인 값 표현

**이유:**
- 부호 있는 정수는 우발적인 오버플로를 감지하고 오류 조건을 신호하는 데 도움이 됩니다 (예: 빈 리스트의 경우 `List.lastIndex == -1`)
- 부호 없는 정수는 부호 있는 정수의 범위 제한 버전으로 취급할 수 없습니다; 어느 타입도 다른 타입의 하위 타입이 아닙니다
