# 제네릭: in, out, where

## 개요

Kotlin의 클래스는 Java와 유사하게 타입 매개변수를 가질 수 있습니다. 타입 인수는 문맥에서 추론될 수 있어 더 깔끔한 문법이 가능합니다.

```kotlin
class Box<T>(t: T) { var value = t }

val box: Box<Int> = Box<Int>(1)
val box = Box(1) // 타입 추론: Box<Int>
```

## 변성

### Java에서의 변성과 와일드카드

Java의 제네릭 타입은 **불변**입니다. 즉 `List<String>`은 `List<Object>`의 서브타입이 아닙니다. 이는 런타임 오류를 방지하지만 유연성이 떨어집니다.

**문제 예제:**
```java
List<String> strs = new ArrayList<String>();
List<Object> objs = strs; // Java에서 컴파일 오류
objs.add(1); // 런타임에 ClassCastException 발생 가능
```

**해결책 - 와일드카드:**
- **`? extends E` (공변성 - 생산자)**: 읽기 전용, 서브타입 전달 안전
- **`? super E` (반공변성 - 소비자)**: 쓰기 전용, 상위 타입 받기 안전

**PECS 기억법**: Producer-Extends, Consumer-Super

### 선언 위치 변성

Kotlin은 **타입 매개변수 선언 위치**에서 변성 어노테이션으로 이 문제를 해결합니다:

#### `out` 수정자 (공변성)

타입 매개변수를 **생산 전용**(반환만, 소비 안 함)으로 만듭니다:

```kotlin
interface Source<out T> {
    fun nextT(): T
}

fun demo(strs: Source<String>) {
    val objects: Source<Any> = strs // OK! T가 공변
}
```

#### `in` 수정자 (반공변성)

타입 매개변수를 **소비 전용**(받기만, 반환 안 함)으로 만듭니다:

```kotlin
interface Comparable<in T> {
    operator fun compareTo(other: T): Int
}

fun demo(x: Comparable<Number>) {
    val y: Comparable<Double> = x // OK! 서브타입 할당 가능
}
```

**기억법**: "Consumer in, Producer out!"

## 타입 프로젝션

### 사용 위치 변성

생산과 소비를 모두 해야 하는 클래스(예: `Array`)의 경우 호출 위치에서 **타입 프로젝션**을 사용합니다:

```kotlin
class Array<T>(val size: Int) {
    operator fun get(index: Int): T { ... }
    operator fun set(index: Int, value: T) { ... }
}

// 문제: Array<Int>는 Array<Any>의 서브타입이 아님
fun copy(from: Array<Any>, to: Array<Any>) { ... }

val ints: Array<Int> = arrayOf(1, 2, 3)
val any = Array<Any>(3) { "" }
copy(ints, any) // 오류: 타입 불일치
```

**해결책 - `out`으로 타입 프로젝션:**
```kotlin
fun copy(from: Array<out Any>, to: Array<Any>) {
    for (i in from.indices) {
        to[i] = from[i] // 안전: from은 읽기 전용
    }
}
```

**`in`으로 타입 프로젝션:**
```kotlin
fun fill(dest: Array<in String>, value: String) { ... }
// 받을 수 있음: Array<String>, Array<CharSequence>, Array<Object>
```

### 스타 프로젝션

타입 인수를 모르지만 타입 안전 연산을 원할 때 `*`를 사용합니다:

- **`Foo<out T : TUpper>` -> `Foo<*>`**: `Foo<out TUpper>`와 동일 (읽기 안전)
- **`Foo<in T>` -> `Foo<*>`**: `Foo<in Nothing>`과 동일 (쓰기 불안전)
- **`Foo<T : TUpper>` (불변) -> `Foo<*>`**: `Foo<out TUpper>`로 읽기, `Foo<in Nothing>`으로 쓰기

```kotlin
interface Function<in T, out U>

Function<*, String>      // Function<in Nothing, String>
Function<Int, *>         // Function<Int, out Any?>
Function<*, *>           // Function<in Nothing, out Any?>
```

## 제네릭 함수

타입 매개변수는 함수 이름 앞에 배치합니다:

```kotlin
fun <T> singletonList(item: T): List<T> { ... }
fun <T> T.basicToString(): String { ... }

val l = singletonList<Int>(1)
val l = singletonList(1) // 타입 추론
```

## 제네릭 제약

### 상한

`extends` 문법을 사용하여 타입 매개변수를 제한합니다:

```kotlin
fun <T : Comparable<T>> sort(list: List<T>) { ... }

sort(listOf(1, 2, 3)) // OK: Int는 Comparable<Int>
sort(listOf(HashMap<Int, String>())) // 오류
```

**기본 상한**: `Any?`

### where를 사용한 다중 상한

```kotlin
fun <T> copyWhenGreater(list: List<T>, threshold: T): List<String>
    where T : CharSequence, T : Comparable<T> {
    return list.filter { it > threshold }.map { it.toString() }
}
```

## 명확히 널이 아닌 타입

Java 상호운용성을 위해 `T & Any`를 사용하여 제네릭 타입을 명확히 널이 아닌 것으로 선언합니다:

```kotlin
interface ArcadeGame<T1> : Game<T1> {
    override fun load(x: T1 & Any): T1 & Any
}
```

## 타입 소거

타입 정보는 **런타임에 소거**됩니다. `Foo<Bar>`와 `Foo<Baz?>`의 인스턴스는 모두 `Foo<*>`가 됩니다.

### 타입 검사와 캐스트

**허용되지 않음:**
```kotlin
if (something is List<Int>) { } // 오류: 타입 인수 소거됨
```

**허용됨 - 스타 프로젝션 검사:**
```kotlin
if (something is List<*>) {
    something.forEach { println(it) } // 아이템은 Any? 타입
}
```

**비제네릭 부분 검사:**
```kotlin
fun handleStrings(list: MutableList<String>) {
    if (list is ArrayList) { // OK! 비제네릭 검사
        // list가 ArrayList<String>으로 스마트 캐스트
    }
}
```

### 구체화된 타입 매개변수

`inline` 함수만 런타임 검사를 위해 구체화된 타입을 지원합니다:

```kotlin
inline fun <reified A, reified B> Pair<*, *>.asPairOf(): Pair<A, B>? {
    if (first !is A || second !is B) return null
    return first as A to second as B
}

val somePair: Pair<Any?, Any?> = "items" to listOf(1, 2, 3)
val stringToList = somePair.asPairOf<String, List<*>>() // 작동!
```

### 검사되지 않은 캐스트

`foo as List<String>`과 같은 구체적인 제네릭 타입으로의 캐스트는 런타임에 검사되지 않습니다:

```kotlin
fun readDictionary(file: File): Map<String, *> = ...

val intsFile = File("ints.dictionary")
// 경고: 검사되지 않은 캐스트
val intsDictionary: Map<String, Int> = readDictionary(intsFile) as Map<String, Int>
```

**경고 억제:**
```kotlin
@Suppress("UNCHECKED_CAST")
inline fun <reified T> List<*>.asListOfType(): List<T>? =
    if (all { it is T }) this as List<T> else null
```

## 타입 인수를 위한 밑줄 연산자

다른 타입이 명시적으로 지정될 때 타입 인수를 자동으로 추론하려면 `_`를 사용합니다:

```kotlin
abstract class SomeClass<T> { abstract fun execute(): T }
class SomeImplementation : SomeClass<String>() { override fun execute(): String = "Test" }

object Runner {
    inline fun <reified S: SomeClass<T>, T> run(): T {
        return S::class.java.getDeclaredConstructor().newInstance().execute()
    }
}

fun main() {
    val s = Runner.run<SomeImplementation, _>() // T가 String으로 추론됨
    assert(s == "Test")
}
```

## 핵심 정리

| 개념 | 목적 | 예제 |
|------|------|------|
| `out T` | 공변성 (생산 전용) | `Source<out T>` |
| `in T` | 반공변성 (소비 전용) | `Comparable<in T>` |
| 타입 프로젝션 | 사용 위치 변성 | `Array<out Any>` |
| 스타 프로젝션 | 알 수 없는 타입 인수 | `List<*>` |
| 구체화 | 인라인 함수의 런타임 타입 정보 | `inline fun <reified T>` |
| 상한 | 타입 매개변수 제한 | `<T : Comparable<T>>` |
| `T & Any` | 명확히 널이 아님 | Java 상호운용 |
