# Kotlin 널 안전성

## 개요

널 안전성은 널 참조 (10억 달러의 실수)의 위험을 크게 줄이도록 설계된 Kotlin 기능입니다. Java와 달리 Kotlin은 타입 시스템에서 널 가능성을 명시적으로 지원하여 런타임이 아닌 컴파일 타임에 잠재적인 널 관련 문제를 잡습니다.

## 핵심 개념

### 널 가능 vs 널 불가능 타입

**널 불가능 타입**은 `null`을 가질 수 없습니다:
```kotlin
var a: String = "abc"
a = null  // 컴파일러 오류
```

**널 가능 타입**은 `?`로 선언되며 `null`을 가질 수 있습니다:
```kotlin
var b: String? = "abc"
b = null  // 유효
```

## 널 값 처리

### 1. if 조건문으로 널 확인
```kotlin
val b: String? = null
val l = if (b != null) b.length else -1
print(l)  // -1
```

### 2. 안전 호출 연산자 `?.`
예외를 던지는 대신 `null`을 반환합니다:
```kotlin
val a: String? = "Kotlin"
val b: String? = null
println(a?.length)  // 6
println(b?.length)  // null
```

**안전 호출 체이닝:**
```kotlin
bob?.department?.head?.name
```

### 3. 엘비스 연산자 `?:`
왼쪽이 `null`이면 대안 값을 제공합니다:
```kotlin
val b: String? = null
val l = b?.length ?: 0
println(l)  // 0
```

`return`이나 `throw`도 사용할 수 있습니다:
```kotlin
val parent = node.getParent() ?: return null
val name = node.getName() ?: throw IllegalArgumentException("name expected")
```

### 4. 널 아님 단언 연산자 `!!`
널 가능 타입을 널 불가능으로 강제 처리합니다. 값이 `null`이면 NPE를 던집니다:
```kotlin
val b: String? = "Kotlin"
val l = b!!.length
println(l)  // 6

val b: String? = null
val l = b!!.length  // 예외: NullPointerException
```

### 5. 널 가능 수신자
확장 함수는 널 가능 수신자와 함께 작동할 수 있습니다:
```kotlin
val person: Person? = null
println(person.toString())  // null
```

### 6. let 함수
널이 아닌 값에서만 코드를 실행하기 위해 안전 호출과 `let`을 결합합니다:
```kotlin
val listWithNulls: List<String?> = listOf("Kotlin", null)
for (item in listWithNulls) {
    item?.let { println(it) }  // 널이 아닌 값만 출력
}
```

### 7. 안전 캐스트 `as?`
캐스트가 실패하면 `null`을 반환합니다:
```kotlin
val a: Any = "Hello, Kotlin!"
val aInt: Int? = a as? Int      // null
val aString: String? = a as? String  // "Hello, Kotlin!"
```

### 8. 널 가능 타입의 컬렉션
널 값을 제거하려면 `filterNotNull()`을 사용합니다:
```kotlin
val nullableList: List<Int?> = listOf(1, 2, null, 4)
val intList: List<Int> = nullableList.filterNotNull()
println(intList)  // [1, 2, 4]
```

## Kotlin에서 NPE의 가능한 원인

1. 명시적 `throw NullPointerException()`
2. 널 아님 단언 연산자 `!!` 사용
3. 초기화 중 데이터 불일치 (this 누출, 초기화되지 않은 슈퍼클래스)
4. 플랫폼 타입과 제네릭을 사용한 Java 상호운용 문제

## 관련 예외

**`UninitializedPropertyAccessException`** - 초기화되지 않은 `lateinit` 속성에 접근할 때 던져집니다.

## 다음 단계
- Java와 Kotlin 상호운용에서 널 가능성 처리
- 확실히 널이 아닌 제네릭 타입에 대해 알아보기
