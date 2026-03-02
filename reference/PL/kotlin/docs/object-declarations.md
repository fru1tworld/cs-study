# 객체 선언과 표현식

## 개요

Kotlin에서 객체는 클래스를 정의하고 인스턴스를 한 단계로 생성할 수 있게 합니다. 이는 싱글톤과 일회성 객체에 유용합니다. Kotlin은 두 가지 접근 방식을 제공합니다:
- **객체 선언** - 싱글톤 생성용
- **객체 표현식** - 익명 일회성 객체 생성용

## 사용 사례

- 공유 리소스(예: 데이터베이스 연결 풀)를 위한 싱글톤 사용
- 동반 객체를 통한 팩토리 메서드 생성
- 기존 클래스 동작을 일시적으로 수정
- 인터페이스나 추상 클래스의 타입 안전 구현

## 객체 선언

`object` 키워드를 사용하여 단일 인스턴스를 생성합니다:

```kotlin
object DataProviderManager {
    private val providers = mutableListOf<DataProvider>()

    fun registerDataProvider(provider: DataProvider) {
        providers.add(provider)
    }

    val allDataProviders: Collection<DataProvider>
        get() = providers
}

// 사용 - 객체 이름으로 직접 참조
DataProviderManager.registerDataProvider(exampleProvider)
```

**핵심 포인트:**
- 첫 접근 시 스레드 안전 초기화
- 상위 타입을 가질 수 있음 (인터페이스 구현 또는 클래스 상속)
- 지역적일 수 없음 (함수 내부에 중첩 불가)
- 할당의 오른쪽에 사용할 수 없음

### 데이터 객체

`data` 수정자로 표시하여 컴파일러 생성 함수를 얻습니다:

```kotlin
data object MyDataObject {
    val number: Int = 3
}

fun main() {
    println(MyDataObject) // MyDataObject (MyDataObject@hashcode가 아님)
}
```

**생성되는 함수:**
- `toString()` - 객체 이름 반환
- `equals()`/`hashCode()` - 동등성 검사 가능
- `copy()` 함수 없음 (싱글톤 개념 위반)
- `componentN()` 함수 없음 (데이터 프로퍼티 없음)

**중요:** 데이터 객체는 참조(`===`)가 아닌 구조적(`==`)으로 비교

### 봉인 계층의 데이터 객체

```kotlin
sealed interface ReadResult
data class Number(val number: Int) : ReadResult
data class Text(val text: String) : ReadResult
data object EndOfFile : ReadResult

fun main() {
    println(Number(7))  // Number(number=7)
    println(EndOfFile)  // EndOfFile
}
```

## 동반 객체

클래스 수준 함수와 프로퍼티를 정의합니다:

```kotlin
class User(val name: String) {
    companion object Factory {
        fun create(name: String): User = User(name)
    }
}

// 클래스 이름으로 호출
val user = User.create("John Doe")
```

**특징:**
- 이름은 선택사항 (기본값은 `Companion`)
- 클래스의 `private` 멤버에 접근 가능
- 인터페이스 구현 가능
- 클래스 이름이 동반 객체에 대한 참조 역할

```kotlin
// 인터페이스를 구현하는 동반 객체
interface Factory<T> {
    fun create(name: String): T
}

class User(val name: String) {
    companion object : Factory<User> {
        override fun create(name: String): User = User(name)
    }
}

val userFactory: Factory<User> = User
val newUser = userFactory.create("Example User")
```

## 객체 표현식

이름을 지정하지 않고 익명 클래스를 선언하고 인스턴스를 생성합니다:

### 처음부터 익명 객체 생성

```kotlin
val helloWorld = object {
    val hello = "Hello"
    val world = "World"
    override fun toString() = "$hello $world"
}
print(helloWorld) // Hello World
```

### 상위 타입으로부터 상속

```kotlin
window.addMouseListener(object : MouseAdapter() {
    override fun mouseClicked(e: MouseEvent) { /*...*/ }
    override fun mouseEntered(e: MouseEvent) { /*...*/ }
})
```

다중 상위 타입:

```kotlin
open class BankAccount(initialBalance: Int) {
    open val balance: Int = initialBalance
}

interface Transaction {
    fun execute()
}

val temporaryAccount = object : BankAccount(1000), Transaction {
    override val balance = 1500
    override fun execute() {
        println("Executing special transaction. New balance is $balance.")
    }
}
```

### 익명 객체의 반환 타입

`public`/`protected`/`internal` 함수에서 반환될 때:
- **상위 타입 없음** -> 반환 타입은 `Any`
- **하나의 상위 타입** -> 반환 타입은 그 상위 타입
- **다중 상위 타입** -> 반환 타입을 명시적으로 선언해야 함

실제 타입에 선언된 멤버만 접근 가능합니다.

### 둘러싸는 스코프 변수 접근

```kotlin
fun countClicks(window: JComponent) {
    var clickCount = 0
    var enterCount = 0

    window.addMouseListener(object : MouseAdapter() {
        override fun mouseClicked(e: MouseEvent) {
            clickCount++
        }
        override fun mouseEntered(e: MouseEvent) {
            enterCount++
        }
    })
}
```

## 초기화 동작 차이

| 타입 | 초기화 |
|------|--------|
| **객체 표현식** | 사용되는 위치에서 즉시 실행 |
| **객체 선언** | 첫 접근 시 지연 초기화 |
| **동반 객체** | 클래스가 로드될 때 초기화 (Java 정적 초기화자와 유사) |
