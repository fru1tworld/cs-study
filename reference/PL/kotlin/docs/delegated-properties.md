# 위임된 프로퍼티

## 개요

위임된 프로퍼티를 사용하면 일반적인 프로퍼티 패턴을 한 번 구현하고 재사용할 수 있습니다. 문법:
```kotlin
val/var <프로퍼티 이름>: <타입> by <표현식>
```

`by` 뒤의 표현식은 `getValue()`와 `setValue()` 연산을 처리하는 위임입니다.

## 기본 위임 구현

위임 클래스는 `getValue()`와 `setValue()` 메서드가 필요합니다:

```kotlin
import kotlin.reflect.KProperty

class Delegate {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): String {
        return "$thisRef, thank you for delegating '${property.name}' to me!"
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {
        println("$value has been assigned to '${property.name}' in $thisRef.")
    }
}

class Example {
    var p: String by Delegate()
}

val e = Example()
println(e.p)  // Example@33a17727, thank you for delegating 'p' to me!
e.p = "NEW"   // NEW has been assigned to 'p' in Example@33a17727.
```

## 표준 위임

### 지연 프로퍼티

`lazy()`는 첫 접근 시에만 값을 계산합니다:

```kotlin
val lazyValue: String by lazy {
    println("computed!")
    "Hello"
}

fun main() {
    println(lazyValue)  // "computed!" 출력 후 "Hello" 출력
    println(lazyValue)  // "Hello"만 출력
}
```

**스레드 안전 옵션:**
- 기본값: synchronized (스레드 안전)
- `LazyThreadSafetyMode.PUBLICATION`: 다중 스레드 허용
- `LazyThreadSafetyMode.NONE`: 스레드 안전 없음

### 관찰 가능한 프로퍼티

`Delegates.observable()`은 변경 시 알립니다:

```kotlin
import kotlin.properties.Delegates

class User {
    var name: String by Delegates.observable("<no name>") { prop, old, new ->
        println("$old -> $new")
    }
}

fun main() {
    val user = User()
    user.name = "first"   // <no name> -> first
    user.name = "second"  // first -> second
}
```

**Vetoable 변형:** 할당을 가로채서 거부하려면 `Delegates.vetoable()`을 사용합니다.

## 다른 프로퍼티에 위임

프로퍼티는 `::` 문법을 사용하여 다른 프로퍼티에 위임할 수 있습니다:

```kotlin
var topLevelInt: Int = 0

class ClassWithDelegate(val anotherClassInt: Int)

class MyClass(var memberInt: Int, val anotherClassInstance: ClassWithDelegate) {
    var delegatedToMember: Int by this::memberInt
    var delegatedToTopLevel: Int by ::topLevelInt
    val delegatedToAnotherClass: Int by anotherClassInstance::anotherClassInt
}

var MyClass.extDelegated: Int by ::topLevelInt
```

**사용 사례 - 하위 호환성:**
```kotlin
class MyClass {
    var newName: Int = 0

    @Deprecated("Use 'newName' instead", ReplaceWith("newName"))
    var oldName: Int by this::newName
}
```

## Map에 프로퍼티 저장

동적 프로퍼티를 위해 map을 위임으로 사용합니다:

```kotlin
class User(val map: Map<String, Any?>) {
    val name: String by map
    val age: Int by map
}

fun main() {
    val user = User(mapOf(
        "name" to "John Doe",
        "age" to 25
    ))
    println(user.name)  // John Doe
    println(user.age)   // 25
}
```

가변 프로퍼티의 경우 `MutableMap`을 사용합니다:
```kotlin
class MutableUser(val map: MutableMap<String, Any?>) {
    var name: String by map
    var age: Int by map
}
```

## 지역 위임된 프로퍼티

위임은 지역 스코프에서도 작동합니다:

```kotlin
fun example(computeFoo: () -> Foo) {
    val memoizedFoo by lazy(computeFoo)
    if (someCondition && memoizedFoo.isValid()) {
        memoizedFoo.doSomething()
    }
}
```

## 프로퍼티 위임 요구사항

### 읽기 전용 프로퍼티용 (`val`)
```kotlin
class ResourceDelegate {
    operator fun getValue(thisRef: Owner, property: KProperty<*>): Resource {
        return Resource()
    }
}

class Owner {
    val valResource: Resource by ResourceDelegate()
}
```

### 가변 프로퍼티용 (`var`)
```kotlin
class ResourceDelegate(private var resource: Resource = Resource()) {
    operator fun getValue(thisRef: Owner, property: KProperty<*>): Resource {
        return resource
    }

    operator fun setValue(thisRef: Owner, property: KProperty<*>, value: Any?) {
        if (value is Resource) {
            resource = value
        }
    }
}

class Owner {
    var varResource: Resource by ResourceDelegate()
}
```

### 표준 라이브러리 인터페이스 사용

```kotlin
fun resourceDelegate(resource: Resource = Resource()): ReadWriteProperty<Any?, Resource> =
    object : ReadWriteProperty<Any?, Resource> {
        var curValue = resource

        override fun getValue(thisRef: Any?, property: KProperty<*>): Resource = curValue
        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Resource) {
            curValue = value
        }
    }

val readOnlyResource: Resource by resourceDelegate()
var readWriteResource: Resource by resourceDelegate()
```

## 변환 규칙

### 기본 변환

컴파일러가 보조 프로퍼티를 생성합니다:

```kotlin
class C {
    var prop: Type by MyDelegate()
}

// 컴파일러가 생성:
class C {
    private val prop$delegate = MyDelegate()
    var prop: Type
        get() = prop$delegate.getValue(this, this::prop)
        set(value: Type) = prop$delegate.setValue(this, this::prop, value)
}
```

### 최적화된 경우 (`$delegate` 필드 없음)

- 참조된 프로퍼티: `var prop: Type by ::impl`
- 명명된 객체: `val s: String by NamedObject`
- 지원 필드가 있는 final `val` 프로퍼티
- 상수 표현식, 열거형 항목, `this`, `null`

### 다른 프로퍼티에 위임

직접 접근 (`$delegate` 필드 없음):

```kotlin
class C<Type> {
    private var impl: Type = ...
    var prop: Type by ::impl
}

// 컴파일러가 생성:
class C<Type> {
    private var impl: Type = ...
    var prop: Type
        get() = impl
        set(value) { impl = value }
}
```

## 위임 제공

위임 생성을 커스터마이즈하려면 `provideDelegate`를 사용합니다:

```kotlin
class ResourceDelegate<T> : ReadOnlyProperty<MyUI, T> {
    override fun getValue(thisRef: MyUI, property: KProperty<*>): T { ... }
}

class ResourceLoader<T>(id: ResourceID<T>) {
    operator fun provideDelegate(
        thisRef: MyUI,
        prop: KProperty<*>
    ): ReadOnlyProperty<MyUI, T> {
        checkProperty(thisRef, prop.name)
        return ResourceDelegate()
    }

    private fun checkProperty(thisRef: MyUI, name: String) { ... }
}

class MyUI {
    fun <T> bindResource(id: ResourceID<T>): ResourceLoader<T> { ... }
    val image by bindResource(ResourceID.image_id)
    val text by bindResource(ResourceID.text_id)
}
```

### PropertyDelegateProvider 사용
```kotlin
val provider = PropertyDelegateProvider { thisRef: Any?, property ->
    ReadOnlyProperty<Any?, Int> { _, property -> 42 }
}
val delegate: Int by provider
```

## 요약

위임된 프로퍼티는 다음을 위한 강력한 메커니즘을 제공합니다:
- **지연 초기화** - 첫 접근 시 계산
- **관찰 가능한 프로퍼티** - 변경에 반응
- **Map 저장** - 동적 프로퍼티 바인딩
- **코드 재사용** - 패턴을 한 번 구현
- **하위 호환성** - 이전 이름을 우아하게 deprecated
