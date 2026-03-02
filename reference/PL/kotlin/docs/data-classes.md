# 데이터 클래스

Kotlin의 데이터 클래스는 주로 데이터를 보관하는 데 사용됩니다. 컴파일러는 인스턴스를 출력, 비교, 복사하는 등의 추가 멤버 함수를 자동으로 생성합니다. `data` 키워드로 표시됩니다.

```kotlin
data class User(val name: String, val age: Int)
```

## 자동 생성 멤버

컴파일러는 주 생성자에 선언된 모든 프로퍼티로부터 다음 멤버들을 파생합니다:

- **`equals()`/`hashCode()` 쌍** - 인스턴스 비교용
- **`toString()`** - `"User(name=John, age=42)"` 형식의 출력 반환
- **`componentN()` 함수** - 선언 순서대로 프로퍼티에 대응 (구조 분해용)
- **`copy()` 함수** - 수정된 프로퍼티로 복사본 생성

## 데이터 클래스 요구사항

- 주 생성자는 **최소 하나의 매개변수**를 가져야 함
- 모든 주 생성자 매개변수는 `val` 또는 `var`로 표시해야 함
- 데이터 클래스는 abstract, open, sealed, inner가 **될 수 없음**

## 클래스 본문에 선언된 프로퍼티

클래스 본문 내부에 선언된 프로퍼티(주 생성자가 아닌)는 생성된 함수에서 **제외**됩니다:

```kotlin
data class Person(val name: String) {
    var age: Int = 0
}

fun main() {
    val person1 = Person("John")
    val person2 = Person("John")
    person1.age = 10
    person2.age = 20
    println("person1 == person2: ${person1 == person2}") // true
    // age는 equals()에서 사용되지 않음, name만 사용
}
```

## 복사

`copy()` 함수는 얕은 복사본을 생성하며 프로퍼티 수정이 가능합니다:

```kotlin
val jack = User(name = "Jack", age = 1)
val olderJack = jack.copy(age = 2)
```

**중요:** `copy()`는 얕은 복사를 수행합니다 - 가변 참조는 공유됩니다:

```kotlin
data class Employee(val name: String, val roles: MutableList<String>)

val original = Employee("Jamie", mutableListOf("developer"))
val duplicate = original.copy()
duplicate.roles.add("team lead")
println(original)  // Employee(name=Jamie, roles=[developer, team lead])
// 둘 다 같은 리스트를 참조!
```

## 구조 분해 선언

데이터 클래스 컴포넌트 함수는 구조 분해를 가능하게 합니다:

```kotlin
val jane = User("Jane", 35)
val (name, age) = jane
println("$name, $age years of age") // Jane, 35 years of age
```

## JVM 매개변수 없는 생성자

JVM에서 매개변수 없는 생성자를 생성하려면 기본값을 제공합니다:

```kotlin
data class User(val name: String = "", val age: Int = 0)
```

## 표준 데이터 클래스

표준 라이브러리는 `Pair`와 `Triple` 클래스를 제공하지만, 코드 가독성을 위해 일반적으로 명명된 데이터 클래스가 선호됩니다.
