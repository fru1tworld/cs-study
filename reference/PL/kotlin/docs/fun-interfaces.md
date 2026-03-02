# 함수형 (SAM) 인터페이스

## 개요

**함수형 인터페이스**(**단일 추상 메서드(SAM) 인터페이스**라고도 함)는 단 하나의 추상 멤버 함수만 가진 인터페이스입니다. 여러 개의 비추상 멤버 함수를 가질 수 있지만 정확히 하나의 추상 멤버만 있어야 합니다.

## 선언

함수형 인터페이스를 선언하려면 `fun` 수정자를 사용합니다:

```kotlin
fun interface KRunnable {
    fun invoke()
}
```

## SAM 변환

SAM 변환을 사용하면 명시적인 클래스 구현을 생성하는 대신 람다 표현식을 사용할 수 있어 코드가 더 간결하고 읽기 쉬워집니다.

### 예제

**SAM 변환 없이** (장황함):
```kotlin
fun interface IntPredicate {
    fun accept(i: Int): Boolean
}

val isEven = object : IntPredicate {
    override fun accept(i: Int): Boolean {
        return i % 2 == 0
    }
}
```

**SAM 변환 사용** (간결함):
```kotlin
fun interface IntPredicate {
    fun accept(i: Int): Boolean
}

val isEven = IntPredicate { it % 2 == 0 }

fun main() {
    println("Is 7 even? - ${isEven.accept(7)}")
}
```

## 생성자 함수에서 마이그레이션

Kotlin 1.6.20부터 함수형 인터페이스 생성자에 대한 호출 가능 참조를 사용할 수 있습니다:

**이전** (생성자 함수 사용):
```kotlin
interface Printer {
    fun print()
}

fun Printer(block: () -> Unit): Printer = object : Printer {
    override fun print() = block()
}
```

**이후** (함수형 인터페이스):
```kotlin
fun interface Printer {
    fun print()
}

// 호출 가능 참조 사용
documentsStorage.addPrinter(::Printer)
```

바이너리 호환성을 유지하려면 이전 함수를 deprecated로 표시합니다:
```kotlin
@Deprecated(message = "Your message", level = DeprecationLevel.HIDDEN)
fun Printer(...) {...}
```

## 함수형 인터페이스 vs 타입 별칭

### 타입 별칭 예제
```kotlin
typealias IntPredicate = (i: Int) -> Boolean
val isEven: IntPredicate = { it % 2 == 0 }

fun main() {
    println("Is 7 even? - ${isEven(7)}")
}
```

### 주요 차이점

| 특징 | 타입 별칭 | 함수형 인터페이스 |
|------|----------|-----------------|
| 타입 생성 | 단지 이름일 뿐 (새 타입 없음) | 새 타입 생성 |
| 멤버 | 하나만 | 여러 비추상 + 하나의 추상 |
| 확장 | 인터페이스 특정 확장 제공 불가 | 특정 확장 제공 가능 |
| 상속 | 인터페이스 구현/확장 불가 | 다른 인터페이스 구현/확장 가능 |
| 런타임 비용 | 더 낮음 | 잠재적으로 더 높음 (변환 필요) |

### 각각 언제 사용할까

- **타입 별칭**: API가 특정 매개변수와 반환 타입을 가진 어떤 함수든 받아들일 때 사용
- **함수형 인터페이스**: 함수 타입 시그니처로 표현할 수 없는 복잡한 계약과 연산을 가진 엔티티에 사용
