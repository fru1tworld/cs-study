# 타입 검사와 캐스트

## 개요

Kotlin에서는 런타임에 객체가 특정 타입인지 확인하고 다른 타입으로 변환할 수 있습니다. 타입 검사는 다루고 있는 객체의 종류를 확인하고, 타입 캐스트는 객체를 다른 타입으로 변환하려고 시도합니다.

---

## `is`와 `!is` 연산자로 검사

`is` 연산자 (또는 부정을 위한 `!is`)를 사용하여 런타임에 객체가 타입과 일치하는지 확인합니다:

```kotlin
fun main() {
    val input: Any = "Hello, Kotlin"

    if (input is String) {
        println("Message length: ${input.length}") // Message length: 13
    }

    if (input !is String) {
        println("Input is not a valid message")
    } else {
        println("Processing message: ${input.length} characters") // Processing message: 13 characters
    }
}
```

### 하위 타입 검사

`is`와 `!is`를 사용하여 객체가 하위 타입과 일치하는지도 확인할 수 있습니다:

```kotlin
interface Animal {
    val name: String
    fun speak()
}

class Dog(override val name: String) : Animal {
    override fun speak() = println("$name says: Woof!")
}

class Cat(override val name: String) : Animal {
    override fun speak() = println("$name says: Meow!")
}

fun handleAnimal(animal: Animal) {
    println("Handling animal: ${animal.name}")
    animal.speak()

    if (animal is Dog) {
        println("Special care instructions: This is a dog.")
    } else if (animal is Cat) {
        println("Special care instructions: This is a cat.")
    }
}

fun main() {
    val pets: List<Animal> = listOf(
        Dog("Buddy"), Cat("Whiskers"), Dog("Rex")
    )

    for (pet in pets) {
        handleAnimal(pet)
        println("---")
    }
}
```

---

## 스마트 캐스트

컴파일러는 불변 값에 대해 객체를 자동으로 캐스트하고 암시적(안전한) 캐스트를 자동으로 삽입합니다:

```kotlin
fun logMessage(data: Any) {
    // data는 자동으로 String으로 캐스트됨
    if (data is String) {
        println("Received text: ${data.length} characters")
    }
}

fun main() {
    logMessage("Server started") // Received text: 14 characters
    logMessage(404)
}
```

### 부정 검사를 사용한 스마트 캐스트

부정 검사가 반환으로 이어지면 컴파일러는 캐스트가 안전하다는 것을 알고 있습니다:

```kotlin
fun logMessage(data: Any) {
    // data는 자동으로 String으로 캐스트됨
    if (data !is String) return
    println("Received text: ${data.length} characters")
}

fun main() {
    logMessage("User signed in") // Received text: 14 characters
    logMessage(true)
}
```

### 제어 흐름 (when 표현식)

스마트 캐스트는 `when` 표현식과 함께 작동합니다:

```kotlin
fun processInput(data: Any) {
    when (data) {
        // data는 자동으로 Int로 캐스트됨
        is Int -> println("Log: Assigned new ID ${data + 1}")
        // data는 자동으로 String으로 캐스트됨
        is String -> println("Log: Received message \"$data\"")
        // data는 자동으로 IntArray로 캐스트됨
        is IntArray -> println("Log: Processed scores, total = ${data.sum()}")
    }
}

fun main() {
    processInput(1001) // Log: Assigned new ID 1002
    processInput("System rebooted") // Log: Received message "System rebooted"
    processInput(intArrayOf(10, 20, 30)) // Log: Processed scores, total = 60
}
```

### 제어 흐름 (while 루프)

```kotlin
sealed interface Status
data class Ok(val currentRoom: String) : Status
data object Error : Status

class RobotVacuum(val rooms: List<String>) {
    var index = 0

    fun status(): Status = if (index < rooms.size) Ok(rooms[index]) else Error

    fun clean(): Status {
        println("Finished cleaning ${rooms[index]}")
        index++
        return status()
    }
}

fun main() {
    val robo = RobotVacuum(listOf("Living Room", "Kitchen", "Hallway"))
    var status: Status = robo.status()

    while (status is Ok) {
        // 컴파일러가 status를 Ok 타입으로 스마트 캐스트
        println("Cleaning ${status.currentRoom}...")
        status = robo.clean()
    }
}
```

### Boolean 변수를 사용한 스마트 캐스트

```kotlin
class Cat {
    fun purr() {
        println("Purr purr")
    }
}

fun petAnimal(animal: Any) {
    val isCat = animal is Cat
    if (isCat) {
        // 컴파일러가 animal이 Cat으로 스마트 캐스트되었음을 알고 있음
        animal.purr()
    }
}

fun main() {
    val kitty = Cat()
    petAnimal(kitty) // Purr purr
}
```

### 논리 연산자

컴파일러는 `&&`와 `||` 연산자로 스마트 캐스트를 수행합니다:

```kotlin
// x는 ||의 오른쪽에서 자동으로 String으로 캐스트됨
if (x !is String || x.length == 0) return

// x는 &&의 오른쪽에서 자동으로 String으로 캐스트됨
if (x is String && x.length > 0) {
    print(x.length) // x는 자동으로 String으로 캐스트됨
}
```

다른 타입에 대한 `or` 연산자를 사용하면 공통 상위 타입이 사용됩니다:

```kotlin
interface Status {
    fun signal() {}
}

interface Ok : Status
interface Postponed : Status
interface Declined : Status

fun signalCheck(signalStatus: Any) {
    if (signalStatus is Postponed || signalStatus is Declined) {
        // signalStatus는 공통 상위 타입 Status로 스마트 캐스트됨
        signalStatus.signal()
    }
}
```

### 인라인 함수

```kotlin
interface Processor {
    fun process()
}

inline fun inlineAction(f: () -> Unit) = f()

fun nextProcessor(): Processor? = null

fun runProcessor(): Processor? {
    var processor: Processor? = null

    inlineAction {
        // 컴파일러가 processor가 지역 변수임을 알고 스마트 캐스트 가능
        if (processor != null) {
            processor.process()
        }
        processor = nextProcessor()
    }
    return processor
}
```

### 예외 처리

스마트 캐스트 정보는 `catch`와 `finally` 블록으로 전달됩니다:

```kotlin
fun testString() {
    var stringInput: String? = null
    stringInput = ""

    try {
        // 컴파일러가 stringInput이 null이 아님을 알고 있음
        println(stringInput.length) // 0
        stringInput = null
        if (2 > 1) throw Exception()
        stringInput = ""
    } catch (exception: Exception) {
        // 컴파일러가 stringInput이 null일 수 있음을 알고 있음
        println(stringInput?.length) // null
    }
}

fun main() {
    testString()
}
```

### 스마트 캐스트 전제 조건

스마트 캐스트는 컴파일러가 변수가 변경되지 않을 것임을 보장할 수 있을 때만 작동합니다:

| 변수 타입 | 조건 |
|----------|------|
| `val` 지역 변수 | 항상 (위임된 프로퍼티 제외) |
| `val` 프로퍼티 | `private`, `internal`이거나 검사가 같은 모듈에 있는 경우 |
| `var` 지역 변수 | 수정되지 않고, 수정하는 람다에서 캡처되지 않고, 위임되지 않은 경우 |
| `var` 프로퍼티 | 절대 불가 |

---

## `as`와 `as?` 캐스트 연산자

### 안전하지 않은 캐스트 연산자 (`as`)

`as` 연산자로 캐스트가 실패하면 `ClassCastException`이 발생합니다:

```kotlin
fun main() {
    val rawInput: Any = "user-1234"

    // String으로 성공적으로 캐스트
    val userId = rawInput as String
    println("Logging in user with ID: $userId") // Logging in user with ID: user-1234

    // ClassCastException 발생
    val wrongCast = rawInput as Int
    println("wrongCast contains: $wrongCast") // Exception in thread "main" java.lang.ClassCastException
}
```

### 안전 캐스트 연산자 (`as?`)

`as?` 연산자로 캐스트가 실패하면 `null`을 반환합니다:

```kotlin
fun main() {
    val rawInput: Any = "user-1234"

    // String으로 성공적으로 캐스트
    val userId = rawInput as? String
    println("Logging in user with ID: $userId") // Logging in user with ID: user-1234

    // wrongCast에 null 값 할당
    val wrongCast = rawInput as? Int
    println("wrongCast contains: $wrongCast") // wrongCast contains: null
}
```

### 캐스트 연산자와 Nullable 타입

```kotlin
fun main() {
    val config: Map<String, Any?> = mapOf(
        "username" to "kodee",
        "alias" to null,
        "loginAttempts" to 3
    )

    // nullable String으로 안전하지 않게 캐스트
    val username: String? = config["username"] as String?
    println("Username: $username") // Username: kodee

    // null 값을 nullable String으로 안전하지 않게 캐스트
    val alias: String? = config["alias"] as String?
    println("Alias: $alias") // Alias: null

    // as?로 안전하게 캐스트하여 예외 대신 null 반환
    val safeAttempts: String? = config["loginAttempts"] as? String
    println("Login attempts (safe): $safeAttempts") // Login attempts (safe): null
}
```

---

## 업캐스팅과 다운캐스팅

### 업캐스팅 (상위 타입으로)

업캐스팅은 특별한 구문이 필요 없습니다:

```kotlin
interface Animal {
    fun makeSound()
}

class Dog : Animal {
    override fun makeSound() {
        println("Dog says woof!")
    }
}

fun printAnimalInfo(animal: Animal) {
    animal.makeSound()
}

fun main() {
    val dog = Dog()
    // Dog 인스턴스를 Animal로 업캐스트
    printAnimalInfo(dog) // Dog says woof!
}
```

### 다운캐스팅 (하위 타입으로)

다운캐스팅은 명시적 캐스트 연산자가 필요합니다. 실패 시 안전하게 `null`을 반환하려면 `as?`를 사용하세요:

```kotlin
interface Animal {
    fun makeSound()
}

class Dog : Animal {
    override fun makeSound() {
        println("Dog says woof!")
    }

    fun bark() {
        println("BARK!")
    }
}

fun main() {
    // Animal 타입으로 Dog 인스턴스 생성
    val animal: Animal = Dog()

    // animal을 Dog 타입으로 안전하게 다운캐스트
    val dog: Dog? = animal as? Dog

    // dog가 null이 아니면 bark() 호출을 위해 안전 호출 사용
    dog?.bark() // "BARK!"
}
```

---

## 관련 주제
- [제네릭 타입 검사와 캐스트](generics.html#generics-type-checks-and-casts)
- [리플렉션](reflection.html)
- [제어 흐름](control-flow.html)
- [null 안전성](null-safety.html)
- [인라인 함수](inline-functions.html)
- [위임된 프로퍼티](delegated-properties.html)
- [가시성 수정자](visibility-modifiers.html)
