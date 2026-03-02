# 타입 별칭

## 개요

타입 별칭은 기존 타입에 대한 대체 이름을 제공합니다. 새 타입을 도입하지 않고 긴 타입 이름, 특히 제네릭 타입을 줄이는 데 유용합니다.

## 기본 사용법

### 컬렉션 타입
```kotlin
typealias NodeSet = Set<Network.Node>
typealias FileTable<K> = MutableMap<K, MutableList<File>>
```

### 함수 타입
```kotlin
typealias MyHandler = (Int, String, Any) -> Unit
typealias Predicate<T> = (T) -> Boolean
```

### 내부 및 중첩 클래스
```kotlin
class A { inner class Inner }
class B { inner class Inner }
typealias AInner = A.Inner
typealias BInner = B.Inner
```

## 타입 별칭 작동 방식

타입 별칭은 새 타입을 생성하지 않습니다 - 기본 타입과 동등합니다. Kotlin 컴파일러는 컴파일 중에 이를 확장합니다:

```kotlin
typealias Predicate<T> = (T) -> Boolean

fun foo(p: Predicate<Int>) = p(42)

fun main() {
    val f: (Int) -> Boolean = { it > 0 }
    println(foo(f)) // "true" 출력

    val p: Predicate<Int> = { it > 0 }
    println(listOf(1, -2).filter(p)) // "[1]" 출력
}
```

## 중첩된 타입 별칭

다른 선언 내부에 타입 별칭을 정의할 수 있지만 중요한 제한이 있습니다:

### 올바른 사용법 (비캡처)
```kotlin
class Dijkstra {
    typealias VisitedNodes = Set<Node>
    private fun step(visited: VisitedNodes, ...) = ...
}
```

### 잘못된 사용법 (타입 매개변수 캡처)
```kotlin
class Graph<Node> {
    // Node를 캡처하므로 잘못됨
    typealias Path = List<Node>
}
```

### 수정된 버전
```kotlin
class Graph<Node> {
    // Node가 타입 별칭 매개변수이므로 올바름
    typealias Path<Node> = List<Node>
}
```

## 중첩된 타입 별칭 규칙

- 기존의 모든 타입 별칭 규칙을 따라야 함
- 참조된 타입이 허용하는 것보다 더 많은 가시성을 노출할 수 없음
- 중첩 클래스와 동일한 스코프를 가짐 (클래스 내부에 정의, 동일한 이름의 부모 별칭 숨김)
- 가시성 제어를 위해 `internal` 또는 `private`으로 표시 가능
- Kotlin 멀티플랫폼의 `expect/actual` 선언에서는 **지원되지 않음**
