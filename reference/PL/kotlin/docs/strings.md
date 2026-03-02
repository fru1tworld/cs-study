# Kotlin 문자열

## 개요

Kotlin에서 문자열은 `String` 타입으로 표현됩니다. JVM에서 문자열은 문자당 약 2바이트의 UTF-16 인코딩을 사용합니다. 문자열은 큰따옴표로 묶인 불변의 문자 시퀀스입니다.

```kotlin
val str = "abcd 123"
```

## 기본 문자열 연산

### 문자 접근

인덱싱을 통해 요소에 접근하고 `for` 루프로 순회할 수 있습니다:

```kotlin
val str = "abcd"
for (c in str) {
    println(c)
}
```

### 문자열 불변성

일단 초기화되면 문자열은 변경할 수 없습니다. 모든 변환은 새로운 `String` 객체를 반환합니다:

```kotlin
val str = "abcd"
println(str.uppercase()) // ABCD
println(str)              // abcd (변경되지 않음)
```

### 문자열 연결

`+` 연산자를 사용합니다:

```kotlin
val s = "abc" + 1
println(s + "def") // abc1def
```

---

## 문자열 리터럴

### 이스케이프된 문자열

백슬래시(`\`)를 사용한 전통적인 이스케이프 시퀀스를 지원합니다:

```kotlin
val s = "Hello, world!\n"
```

### 여러 줄 문자열

삼중 따옴표(`"""`)로 구분되며, 이스케이프가 필요 없습니다:

```kotlin
val text = """
    for (c in "foo") print(c)
"""
```

`trimMargin()`을 사용하여 선행 공백 제거:

```kotlin
val text = """
    |Tell me and I forget.
    |Teach me and I remember.
    |Involve me and I learn.
    |(Benjamin Franklin)
""".trimMargin()
```

사용자 정의 마진 접두사: `trimMargin(">")`

---

## 문자열 템플릿

템플릿 표현식은 `$`를 사용하여 코드를 평가하고 결과를 문자열에 삽입합니다:

### 단순 변수 보간

```kotlin
val i = 10
println("i = $i") // i = 10

val letters = listOf("a","b","c","d","e")
println("Letters: $letters") // Letters: [a, b, c, d, e]
```

### 표현식 보간

```kotlin
val s = "abc"
println("$s.length is ${s.length}") // abc.length is 3
```

### 여러 줄 문자열에서 리터럴 달러 기호 삽입

```kotlin
val price = """${'$'}9.99"""
```

---

## 다중 달러 문자열 보간

보간을 트리거하기 위해 여러 개의 연속 달러 기호를 지정하여 단일 `$`를 리터럴로 취급합니다:

### 이중 달러 예제 (`$$`)

```kotlin
val KClass<*>.jsonSchema : String get() = $$"""
{
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "$id": "https://example.com/product.schema.json",
    "$dynamicAnchor": "meta",
    "title": "$${simpleName ?: qualifiedName ?: "unknown"}",
    "type": "object"
}
"""
```

### 삼중 달러 예제 (`$$$`)

```kotlin
val productName = "carrot"
val requestedData = $$$"""{
    "currency": "$",
    "enteredAmount": "42.45 $$",
    "$$serviceField": "none",
    "product": "$$$productName"
}"""
```

출력:

```
{
    "currency": "$",
    "enteredAmount": "42.45 $$",
    "$$serviceField": "none",
    "product": "carrot"
}
```

---

## 문자열 포맷팅

Kotlin/JVM에서만 사용 가능합니다. 형식 지정자와 함께 `String.format()`을 사용합니다:

**일반적인 지정자:**
- `%d` - 정수
- `%f` - 부동 소수점 숫자
- `%s` - 문자열

```kotlin
// 선행 0이 있는 정수
val integerNumber = String.format("%07d", 31416)
println(integerNumber) // 0031416

// + 기호와 소수점 4자리를 가진 부동 소수점
val floatNumber = String.format("%+.4f", 3.141592)
println(floatNumber) // +3.1416

// 대문자 문자열
val helloString = String.format("%S %S", "hello", "world")
println(helloString) // HELLO WORLD

// argument_index$로 인수 반복
val negativeNumberInParentheses = String.format("%(d means %1$d", -31416)
println(negativeNumberInParentheses) // (31416) means -31416
```

**장점:** 템플릿보다 더 다재다능합니다; 포맷 문자열은 변수에서 할당할 수 있습니다 (현지화에 유용).
