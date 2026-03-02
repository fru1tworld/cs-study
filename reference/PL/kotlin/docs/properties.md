# Kotlin 프로퍼티

## 개요

Kotlin의 프로퍼티를 사용하면 데이터에 접근하거나 변경하기 위한 함수를 작성하지 않고도 데이터를 저장하고 관리할 수 있습니다. 모든 프로퍼티는 이름, 타입, 그리고 자동 생성된 `get()` 함수 (getter)를 가집니다. 가변 프로퍼티는 `set()` 함수 (setter)도 가집니다. getter와 setter를 접근자라고 합니다.

---

## 프로퍼티 선언

프로퍼티는 **가변** (`var`) 또는 **읽기 전용** (`val`)일 수 있습니다.

### 최상위 프로퍼티
```kotlin
// File: Constants.kt
package my.app
val pi = 3.14159
var counter = 0
```

### 클래스, 인터페이스, 객체의 프로퍼티
```kotlin
class Address {
    var name: String = "Holmes, Sherlock"
    var street: String = "Baker"
    var city: String = "London"
}

interface ContactInfo {
    val email: String
}

object Company {
    var name: String = "Detective Inc."
    val country: String = "UK"
}

class PersonContact : ContactInfo {
    override val email: String = "sherlock@example.com"
}
```

### 프로퍼티 접근
```kotlin
fun copyAddress(address: Address): Address {
    val result = Address()
    result.name = address.name
    result.street = address.street
    result.city = address.city
    return result
}
```

---

## 커스텀 Getter와 Setter

### 커스텀 Getter
프로퍼티에 접근할 때마다 실행됩니다:
```kotlin
class Rectangle(val width: Int, val height: Int) {
    val area: Int get() = this.width * this.height
}
```

### 커스텀 Setter
값이 할당될 때마다 실행됩니다 (초기화 중 제외):
```kotlin
class Point(var x: Int, var y: Int) {
    var coordinates: String
        get() = "$x,$y"
        set(value) {
            val parts = value.split(",")
            x = parts[0].toInt()
            y = parts[1].toInt()
        }
}
```

### 가시성 변경 또는 어노테이션 추가

**접근자 가시성 변경:**
```kotlin
class BankAccount(initialBalance: Int) {
    var balance: Int = initialBalance
        private set

    fun deposit(amount: Int) {
        if (amount > 0) balance += amount
    }
}
```

**접근자에 어노테이션 추가:**
```kotlin
@Target(AnnotationTarget.PROPERTY_GETTER)
annotation class Inject

class Service {
    var dependency: String = "Default Service"
        @Inject get
}
```

---

## 백킹 필드

접근자는 메모리에 프로퍼티 값을 저장하기 위해 백킹 필드를 사용합니다. Kotlin은 필요할 때 자동으로 생성합니다. `field` 키워드를 사용하여 백킹 필드를 참조합니다.

```kotlin
class Scoreboard {
    var score: Int = 0
        set(value) {
            field = value
            println("Score updated to $field")
        }
}
```

**참고:** Kotlin은 기본 getter/setter를 사용하거나 커스텀 접근자에서 `field`를 사용할 때만 백킹 필드를 생성합니다.

---

## 백킹 프로퍼티

외부에서 읽기 전용으로 내부에서 수정하는 패턴:

```kotlin
class ShoppingCart {
    private val _items = mutableListOf<String>()
    val items: List<String> get() = _items

    fun addItem(item: String) {
        _items.add(item)
    }

    fun removeItem(item: String) {
        _items.remove(item)
    }
}
```

**상태를 공유하는 여러 프로퍼티:**
```kotlin
class Temperature {
    private var _celsius: Double = 0.0

    var celsius: Double
        get() = _celsius
        set(value) { _celsius = value }

    var fahrenheit: Double
        get() = _celsius * 9 / 5 + 32
        set(value) { _celsius = (value - 32) * 5 / 9 }
}
```

---

## 컴파일 타임 상수

`const`를 사용하여 읽기 전용 프로퍼티를 컴파일 타임 상수로 표시합니다:

```kotlin
const val MAX_LOGIN_ATTEMPTS = 3
```

**요구 사항:**
- 최상위 프로퍼티, 또는 `object`나 companion object의 멤버
- `String` 또는 기본 타입으로 초기화해야 함
- 커스텀 getter를 가질 수 없음

**어노테이션에서 사용:**
```kotlin
const val SUBSYSTEM_DEPRECATED: String = "This subsystem is deprecated"

@Deprecated(SUBSYSTEM_DEPRECATED)
fun processLegacyOrders() { ... }
```

---

## 늦은 초기화 프로퍼티

생성 후에 초기화되는 프로퍼티에 `lateinit` 수정자를 사용합니다:

```kotlin
public class OrderServiceTest {
    lateinit var orderService: OrderService

    @SetUp
    fun setup() {
        orderService = OrderService()
    }
}
```

**초기화 여부 확인:**
```kotlin
class WeatherStation {
    lateinit var latestReading: String

    fun printReading() {
        if (this::latestReading.isInitialized) {
            println("Latest reading: $latestReading")
        } else {
            println("No reading available")
        }
    }
}
```

**초기화 전 접근 시 예외:**
```
kotlin.UninitializedPropertyAccessException: lateinit property has not been initialized
```

---

## 위임된 프로퍼티

로직을 재사용하기 위해 프로퍼티 책임을 별도의 객체에 위임합니다:

**일반적인 사용 사례:**
- 값을 지연 계산
- 키로 맵에서 읽기
- 데이터베이스 접근
- 접근 시 리스너에 알림

구현 세부 사항은 [위임된 프로퍼티 문서](delegated-properties.md)를 참조하세요.
