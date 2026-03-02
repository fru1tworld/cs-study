# 인라인 함수

## 개요

인라인 함수는 호출 위치에 람다 표현식을 인라인하여 고차 함수의 런타임 패널티를 제거하고, 함수 객체 생성과 클로저 할당 오버헤드를 피합니다.

## 주요 섹션

### 1. 기본 인라인 함수

`inline` 수정자는 컴파일러에게 함수와 그 람다 매개변수 모두를 호출 위치에 인라인하도록 지시합니다.

**예제:**
```kotlin
inline fun <T> lock(lock: Lock, body: () -> T): T { ... }
```

다음 대신:
```kotlin
lock(l) { foo() }
```

컴파일러가 생성:
```kotlin
l.lock()
try {
    foo()
} finally {
    l.unlock()
}
```

### 2. noinline 수정자

다른 것은 인라인하면서 특정 람다 매개변수의 인라인을 방지합니다:

```kotlin
inline fun foo(inlined: () -> Unit, noinline notInlined: () -> Unit) { ... }
```

`noinline` 람다는 필드에 저장하고 자유롭게 전달할 수 있지만, 인라인 가능한 람다는 그렇지 않습니다.

### 3. 비지역 점프 표현식

#### 반환

인라인 함수는 람다에서 `return` 문을 사용하여 둘러싸는 함수를 빠져나갈 수 있습니다:

```kotlin
inline fun inlined(block: () -> Unit) {
    println("hi!")
}

fun foo() {
    inlined {
        return  // OK: foo()를 빠져나감
    }
}
```

#### Break와 Continue

인라인 함수는 람다에서 `break`와 `continue`를 지원합니다:

```kotlin
fun processList(elements: List<Int>): Boolean {
    for (element in elements) {
        val variable = element.nullableMethod() ?: run {
            log.warning("Element is null...")
            continue  // 인라인 함수에서 OK
        }
        if (variable == 0) return true
    }
    return false
}
```

#### crossinline 수정자

람다가 다른 컨텍스트에서 실행될 때 비지역 반환을 금지합니다:

```kotlin
inline fun f(crossinline body: () -> Unit) {
    val f = object: Runnable {
        override fun run() = body()  // 여기서 return 사용 불가
    }
}
```

### 4. 구체화된 타입 매개변수

인라인 함수는 `reified` 타입 매개변수를 가질 수 있어, 리플렉션 없이 런타임에 타입 정보에 접근할 수 있습니다:

```kotlin
inline fun <reified T> TreeNode.findParentOfType(): T? {
    var p = parent
    while (p != null && p !is T) {
        p = p.parent
    }
    return p as T?
}

// Class 매개변수 없이 호출
treeNode.findParentOfType<MyTreeNode>()
```

리플렉션이 필요한 비인라인 버전과 비교:
```kotlin
fun <T> TreeNode.findParentOfType(clazz: Class<T>): T? {
    var p = parent
    while (p != null && !clazz.isInstance(p)) {
        p = p.parent
    }
    return p as T?
}
// 호출에 Class 필요
treeNode.findParentOfType(MyTreeNode::class.java)
```

### 5. 인라인 프로퍼티

`inline` 수정자는 프로퍼티 접근자에 어노테이션할 수 있습니다:

```kotlin
val foo: Foo
    inline get() = Foo()

var bar: Bar
    get() = ...
    inline set(v) { ... }

// 또는 전체 프로퍼티
inline var bar: Bar
    get() = ...
    set(v) { ... }
```

### 6. 공개 API 인라인 함수 제한

- public/protected 인라인 함수는 본문에서 private/internal 선언을 사용할 수 없음
- 바이너리 비호환성 문제 방지
- `@PublishedApi` 어노테이션은 public 인라인 함수에서 internal 선언을 허용합니다:

```kotlin
@PublishedApi
internal class MyClass { ... }

public inline fun useMyClass() {
    // 여기서 MyClass에 접근 가능
}
```

## 성능 고려사항

- 인라인은 런타임 오버헤드를 제거하지만 생성된 코드 크기를 증가시킴
- 적당한 크기의 함수에 신중하게 사용
- 루프 내부의 다형성 호출 위치에서 가장 유익
- 인라인 함수에 인라인 가능한 매개변수가 없으면 컴파일러가 경고 (`@Suppress("NOTHING_TO_INLINE")`로 억제)
