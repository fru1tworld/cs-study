# Kotlin 투어: 클래스

## 개요

Kotlin은 클래스와 객체를 사용한 객체 지향 프로그래밍을 지원합니다. 클래스를 사용하면 객체의 특성 집합을 선언하여 반복적인 선언을 피함으로써 시간과 노력을 절약할 수 있습니다.

**기본 클래스 선언:**

```kotlin
class Customer
```

---

## 프로퍼티

프로퍼티는 클래스 객체의 특성을 선언합니다. 다음과 같이 선언할 수 있습니다:

1. **클래스 이름 뒤 괄호 안에 (클래스 헤더):**

   ```kotlin
   class Contact(val id: Int, var email: String)
   ```

2. **클래스 본문 내에 (중괄호):**

   ```kotlin
   class Contact(val id: Int, var email: String) {
       val category: String = ""
   }
   ```

**모범 사례:**
- 인스턴스 생성 후 수정이 필요하지 않으면 읽기 전용 프로퍼티(`val`) 사용
- `val`/`var` 없이 괄호 안에 있는 프로퍼티는 인스턴스 생성 후 접근 불가
- 프로퍼티는 기본값을 가질 수 있음

**기본값이 있는 예제:**

```kotlin
class Contact(val id: Int, var email: String = "example@gmail.com") {
    val category: String = "work"
}
```

---

## 인스턴스 생성

객체는 생성자를 사용하여 생성됩니다. Kotlin은 클래스 헤더 매개변수로 기본 생성자를 자동 생성합니다.

```kotlin
class Contact(val id: Int, var email: String)

fun main() {
    val contact = Contact(1, "mary@gmail.com")
}
```

---

## 프로퍼티 접근

점 표기법(`.`)을 사용하여 프로퍼티에 접근:

```kotlin
class Contact(val id: Int, var email: String)

fun main() {
    val contact = Contact(1, "mary@gmail.com")

    // 프로퍼티 읽기
    println(contact.email) // mary@gmail.com

    // 프로퍼티 수정
    contact.email = "jane@gmail.com"
    println(contact.email) // jane@gmail.com
}
```

**문자열 템플릿:**

```kotlin
println("Their email address is: ${contact.email}")
```

---

## 멤버 함수

클래스 본문에 선언된 멤버 함수로 객체 동작 정의:

```kotlin
class Contact(val id: Int, var email: String) {
    fun printId() {
        println(id)
    }
}

fun main() {
    val contact = Contact(1, "mary@gmail.com")
    contact.printId() // 1
}
```

---

## 데이터 클래스

데이터 클래스는 데이터 저장에 최적화되어 있으며 자동 생성된 유틸리티 함수가 함께 제공됩니다.

**선언:**

```kotlin
data class User(val name: String, val id: Int)
```

### 미리 정의된 멤버 함수

| 함수 | 설명 |
|------|------|
| `toString()` | 인스턴스와 프로퍼티의 읽기 쉬운 문자열 출력 |
| `equals()` 또는 `==` | 인스턴스 비교 |
| `copy()` | 선택적으로 다른 프로퍼티로 복사본 생성 |

### 문자열로 출력

```kotlin
data class User(val name: String, val id: Int)

fun main() {
    val user = User("Alex", 1)
    println(user) // User(name=Alex, id=1)
}
```

### 인스턴스 비교

```kotlin
data class User(val name: String, val id: Int)

fun main() {
    val user = User("Alex", 1)
    val secondUser = User("Alex", 1)
    val thirdUser = User("Max", 2)

    println("user == secondUser: ${user == secondUser}") // true
    println("user == thirdUser: ${user == thirdUser}") // false
}
```

### 인스턴스 복사

```kotlin
data class User(val name: String, val id: Int)

fun main() {
    val user = User("Alex", 1)

    // 정확한 복사
    println(user.copy()) // User(name=Alex, id=1)

    // 이름 변경된 복사
    println(user.copy("Max")) // User(name=Max, id=1)

    // id 변경된 복사
    println(user.copy(id = 3)) // User(name=Alex, id=3)
}
```

---

## 연습 문제

### 연습 1: Employee 데이터 클래스

```kotlin
data class Employee(val name: String, var salary: Int)

fun main() {
    val emp = Employee("Mary", 20)
    println(emp) // Employee(name=Mary, salary=20)
    emp.salary += 10
    println(emp) // Employee(name=Mary, salary=30)
}
```

### 연습 2: 중첩 데이터 클래스

```kotlin
data class Person(val name: Name, val address: Address, val ownsAPet: Boolean = true)
data class Name(val first: String, val last: String)
data class Address(val street: String, val city: City)
data class City(val name: String, val countryCode: String)

fun main() {
    val person = Person(
        Name("John", "Smith"),
        Address("123 Fake Street", City("Springfield", "US")),
        ownsAPet = false
    )
}
```

### 연습 3: 랜덤 Employee 생성기

```kotlin
import kotlin.random.Random

data class Employee(val name: String, var salary: Int)

class RandomEmployeeGenerator(var minSalary: Int, var maxSalary: Int) {
    val names = listOf("John", "Mary", "Ann", "Paul", "Jack", "Elizabeth")

    fun generateEmployee() = Employee(
        names.random(),
        Random.nextInt(from = minSalary, until = maxSalary)
    )
}

fun main() {
    val empGen = RandomEmployeeGenerator(10, 30)
    println(empGen.generateEmployee())
    println(empGen.generateEmployee())
    println(empGen.generateEmployee())
    empGen.minSalary = 50
    empGen.maxSalary = 100
    println(empGen.generateEmployee())
}
```

---

## 다음 단계

- [null 안전성](kotlin-tour-null-safety.html) - Kotlin 투어의 마지막 챕터
