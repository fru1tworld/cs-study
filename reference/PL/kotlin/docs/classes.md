# 클래스

Kotlin에서 클래스는 데이터(프로퍼티)와 동작(함수)을 캡슐화하는 객체의 청사진 또는 템플릿입니다. 클래스는 재사용 가능하고 구조화된 코드를 선언하기 위한 간결한 문법을 제공합니다.

## 클래스 선언

클래스는 `class` 키워드를 사용하여 선언합니다:

```kotlin
class Person { /*...*/ }
```

클래스 선언은 다음으로 구성됩니다:

### 클래스 헤더
- `class` 키워드
- 클래스 이름
- 타입 매개변수 (선택사항)
- 주 생성자 (선택사항)

### 클래스 본문 (선택사항)
- 부 생성자
- 초기화 블록
- 함수
- 프로퍼티
- 중첩 및 내부 클래스
- 객체 선언

**최소 문법:**
```kotlin
// 본문이 없는 클래스
class Person(val name: String, var age: Int)
```

## 인스턴스 생성

인스턴스는 클래스 이름 뒤에 괄호 `()`를 사용하여 함수 호출과 유사하게 생성됩니다:

```kotlin
val anonymousUser = Person()
val namedUser = Person("Joe")
```

**핵심 포인트:**
- `new` 키워드가 필요 없음
- 기본값을 사용하거나 특정 인수를 전달할 수 있음
- 가변(`var`) 또는 읽기 전용(`val`) 변수에 할당

**예제:**
```kotlin
class Person(val name: String = "Sebastian")

fun main() {
    val anonymousUser = Person()           // 기본값 사용
    val namedUser = Person("Joe")          // 특정 값 사용
    println(anonymousUser.name)            // Sebastian
    println(namedUser.name)                // Joe
}
```

## 생성자와 초기화 블록

### 주 생성자

주 생성자는 클래스 헤더에서 선언되며 초기 상태를 설정합니다:

```kotlin
class Person(name: String) { /*...*/ }

// 어노테이션이나 수정자가 없으면 'constructor' 키워드는 선택사항:
class Person(name: String) { /*...*/ }
```

**생성자 매개변수 프로퍼티:**

읽기 전용에는 `val`을, 가변에는 `var`를 사용합니다:

```kotlin
class Person(val name: String, var age: Int) { /*...*/ }
```

**프로퍼티 선언 없는 매개변수:**

```kotlin
class PersonWithAssignment(name: String) {
    val displayName: String = name
}
```

**기본값:**

```kotlin
class Person(val name: String = "John", var age: Int = 30) { /*...*/ }

fun main() {
    val person = Person()
    println("Name: ${person.name}, Age: ${person.age}")  // Name: John, Age: 30
}
```

**클래스 본문에서 생성자 매개변수 사용:**

```kotlin
class Person(
    val name: String = "John",
    var age: Int = 30
) {
    val description: String = "Name: $name, Age: $age"
}

fun main() {
    val person = Person()
    println(person.description)  // Name: John, Age: 30
}
```

### 초기화 블록

초기화 블록(`init {}`)은 인스턴스가 생성될 때 실행되며 복잡한 초기화 로직을 포함할 수 있습니다:

```kotlin
class Person(val name: String, var age: Int) {
    init {
        println("Person created: $name, age $age.")
    }
}

fun main() {
    Person("John", 30)  // Person created: John, age 30.
}
```

**다중 초기화 블록:**

```kotlin
class Person(val name: String, var age: Int) {
    init {
        println("Person created: $name, age $age.")
    }

    init {
        if (age < 18) {
            println("$name is a minor.")
        } else {
            println("$name is an adult.")
        }
    }
}

fun main() {
    Person("John", 30)
    // Person created: John, age 30.
    // John is an adult.
}
```

**`init`을 사용한 데이터 유효성 검사:**

```kotlin
class Person(val age: Int) {
    init {
        require(age > 0) { "age must be positive" }
    }
}
```

### 부 생성자

부 생성자는 클래스를 초기화하는 추가적인 방법을 제공합니다. 반드시 주 생성자에게 위임해야 합니다:

```kotlin
class Person(val name: String, var age: Int) {
    constructor(name: String, age: String) : this(name, age.toIntOrNull() ?: 0) {
        println("$name created with converted age: ${this.age}")
    }
}

fun main() {
    Person("Bob", "8")  // Bob created with converted age: 8
}
```

**직접 및 간접 위임:**

```kotlin
class Person(val name: String, var age: Int) {
    // 주 생성자에 직접 위임
    constructor(name: String) : this(name, 0) {
        println("Person created with default age: $age and name: $name.")
    }

    // 간접 위임: this("Bob") -> constructor(name: String) -> 주 생성자
    constructor() : this("Bob") {
        println("New person created with default age: $age and name: $name.")
    }
}

fun main() {
    Person("Alice")  // Person created with default age: 0 and name: Alice.
    Person()         // Person created with default age: 0 and name: Bob.
                     // New person created with default age: 0 and name: Bob.
}
```

**초기화 블록 실행 순서:**

```kotlin
class Person {
    init {
        println("1. First initializer block runs")
    }

    constructor(i: Int) {
        println("2. Person $i is created")
    }
}

fun main() {
    Person(1)
    // 1. First initializer block runs
    // 2. Person 1 is created
}
```

### 생성자가 없는 클래스

명시적 생성자가 없는 클래스는 암시적인 public 매개변수 없는 주 생성자를 가집니다:

```kotlin
class Person { }

fun main() {
    val person = Person()  // 암시적 생성자 사용
}
```

**생성자 가시성 제한:**

```kotlin
class Person private constructor() { /*...*/ }
```

**기본값이 있는 암시적 매개변수 없는 생성자:**

```kotlin
class Person(val personName: String = "")
// Kotlin이 암시적으로 제공: Person()
```

## 상속

클래스 상속을 통해 기존 기본 클래스에서 파생 클래스를 만들고, 프로퍼티와 함수를 상속받으면서 동작을 추가하거나 수정할 수 있습니다.

*자세한 내용은 상속 섹션을 참조하세요.*

## 추상 클래스

추상 클래스는 직접 인스턴스화할 수 없습니다. 상속되도록 설계되었으며 서브클래스가 구현해야 하는 추상 멤버를 제공합니다:

```kotlin
abstract class Person(val name: String, val age: Int) {
    // 추상 멤버 (구현 없음)
    abstract fun introduce()

    // 비추상 멤버 (구현 있음)
    fun greet() {
        println("Hello, my name is $name.")
    }
}

class Student(
    name: String,
    age: Int,
    val school: String
) : Person(name, age) {
    override fun introduce() {
        println("I am $name, $age years old, and I study at $school.")
    }
}

fun main() {
    val student = Student("Alice", 20, "Engineering University")
    student.greet()      // Hello, my name is Alice.
    student.introduce()  // I am Alice, 20 years old, and I study at Engineering University.
}
```

**핵심 포인트:**
- `abstract` 키워드를 사용하여 추상 클래스 선언
- 추상 멤버는 `open` 키워드가 필요 없음 (암시적으로 상속 가능)
- 서브클래스는 `override`를 사용하여 추상 멤버 구현

## 동반 객체

동반 객체를 사용하면 인스턴스를 만들지 않고도 클래스 이름을 사용하여 멤버에 접근할 수 있습니다:

```kotlin
class Person(val name: String) {
    companion object {
        fun createAnonymous() = Person("Anonymous")
    }
}

fun main() {
    val anonymous = Person.createAnonymous()
    println(anonymous.name)  // Anonymous
}
```

이것은 팩토리 함수와 논리적으로 관련된 정적과 같은 기능에 유용합니다.
