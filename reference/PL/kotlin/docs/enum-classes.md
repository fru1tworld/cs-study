# 열거형 클래스

## 개요

Kotlin의 열거형 클래스는 상수 집합을 정의하는 타입 안전한 방법을 제공합니다. 각 열거형 상수는 열거형 클래스의 객체 인스턴스입니다.

## 기본 열거형 선언

```kotlin
enum class Direction { NORTH, SOUTH, WEST, EAST }
```

열거형 상수는 쉼표로 구분됩니다.

## 프로퍼티가 있는 열거형

열거형은 프로퍼티를 가지고 초기화될 수 있습니다:

```kotlin
enum class Color(val rgb: Int) {
    RED(0xFF0000),
    GREEN(0x00FF00),
    BLUE(0x0000FF)
}
```

## 익명 클래스

열거형 상수는 해당 메서드를 가진 자체 익명 클래스를 선언하고 기본 메서드를 오버라이드할 수 있습니다:

```kotlin
enum class ProtocolState {
    WAITING { override fun signal() = TALKING },
    TALKING { override fun signal() = WAITING };
    abstract fun signal(): ProtocolState
}
```

**참고:** 상수 정의와 멤버 정의를 구분하려면 세미콜론을 사용합니다.

## 인터페이스 구현

열거형 클래스는 인터페이스를 구현할 수 있습니다(클래스에서 파생은 불가):

```kotlin
import java.util.function.BinaryOperator
import java.util.function.IntBinaryOperator

enum class IntArithmetics : BinaryOperator<Int>, IntBinaryOperator {
    PLUS {
        override fun apply(t: Int, u: Int): Int = t + u
    },
    TIMES {
        override fun apply(t: Int, u: Int): Int = t * u
    };

    override fun applyAsInt(t: Int, u: Int) = apply(t, u)
}
```

모든 열거형 클래스는 기본적으로 `Comparable`을 구현하며 상수들은 자연적인 순서로 정렬됩니다.

## 열거형 상수 작업

### 프로퍼티와 메서드

```kotlin
EnumClass.valueOf(value: String): EnumClass
EnumClass.entries: EnumEntries<EnumClass>  // 특수화된 List<EnumClass>
```

### 사용 예제

```kotlin
enum class RGB { RED, GREEN, BLUE }

fun main() {
    // entries를 통해 순회
    for (color in RGB.entries) {
        println(color.toString())  // RED, GREEN, BLUE
    }

    // 이름으로 가져오기
    println(RGB.valueOf("RED"))  // "The first color is: RED" 출력

    // name과 ordinal 접근
    println(RGB.RED.name)      // RED
    println(RGB.RED.ordinal)   // 0
}
```

### 제네릭 접근

제네릭 열거형 접근을 위해 인라인 구체화 타입 매개변수를 사용합니다:

```kotlin
enum class RGB { RED, GREEN, BLUE }

inline fun <reified T : Enum<T>> printAllValues() {
    println(enumEntries<T>().joinToString { it.name })
}

printAllValues<RGB>()  // RED, GREEN, BLUE
```

## 주요 함수

- **`valueOf(name: String)`** - 이름으로 열거형 상수 가져오기 (찾지 못하면 `IllegalArgumentException` 발생)
- **`entries`** - `EnumEntries<T>` 반환 (모든 상수의 특수화된 리스트) [Kotlin 1.9.0 이상 권장]
- **`enumEntries<T>()`** - 모든 열거형 항목을 반환하는 제네릭 함수 (deprecated된 `enumValues<T>()`보다 효율적)
- **`name`** 프로퍼티 - 상수의 이름 가져오기
- **`ordinal`** 프로퍼티 - 상수의 위치 가져오기 (0부터 시작)

## 성능 참고

deprecated된 `enumValues<T>()` 대신 `enumEntries<T>()`를 사용하세요 - 매번 동일한 리스트 인스턴스를 반환하여 불필요한 배열 생성을 피합니다.
