# Kotlin의 Dynamic 타입

## 개요

**dynamic 타입**은 정적 타입 검사를 비활성화하여 타입이 없거나 느슨하게 타입이 지정된 환경, 특히 JavaScript와의 상호운용성을 용이하게 하는 Kotlin 기능입니다. **참고:** dynamic 타입은 JVM을 타겟팅하는 코드에서는 지원되지 않습니다.

## 선언

```kotlin
val dyn: dynamic = ...
```

## 타입 검사 동작

`dynamic` 타입은 본질적으로 Kotlin의 타입 검사기를 끕니다:

- `dynamic` 값은 모든 변수에 할당하거나 어디서든 매개변수로 전달할 수 있습니다
- 모든 값을 `dynamic` 변수에 할당하거나 `dynamic`을 받는 함수에 전달할 수 있습니다
- `dynamic` 값에 대해 null 검사가 비활성화됩니다

## Dynamic 함수 및 프로퍼티 호출

`dynamic` 변수에서 모든 매개변수로 모든 프로퍼티나 함수를 호출할 수 있습니다:

```kotlin
dyn.whatever(1, "foo", dyn)           // 'whatever'는 어디에도 정의되어 있지 않음
dyn.whatever(*arrayOf(1, 2, 3))       // 스프레드 연산자와 함께 작동
```

이 코드는 그대로 JavaScript로 컴파일됩니다.

## 중요 고려사항

`dynamic` 값에서 Kotlin 함수를 호출할 때 Kotlin-to-JavaScript 컴파일러의 **이름 맹글링**에 주의하세요. 잘 정의된 이름을 할당하려면 `@JsName` 어노테이션을 사용합니다:

```kotlin
@JsName("myFunction")
fun myFunction() { ... }
```

## Dynamic 호출 체이닝

dynamic 호출은 `dynamic`을 반환하여 자유로운 체이닝을 허용합니다:

```kotlin
dyn.foo().bar.baz()
```

## 람다 매개변수

dynamic 호출의 람다 매개변수는 기본적으로 `dynamic` 타입입니다:

```kotlin
dyn.foo { x -> x.bar() }  // x는 dynamic
```

## 지원되는 연산자

- **이항:** `+`, `-`, `*`, `/`, `%`, `>`, `<`, `>=`, `<=`, `==`, `!=`, `===`, `!==`, `&&`, `||`
- **단항 전위:** `-`, `+`, `!`
- **전위/후위:** `++`, `--`
- **할당:** `+=`, `-=`, `*=`, `/=`, `%=`
- **인덱스 접근:** `d[a]` (읽기), `d[a1] = a2` (쓰기)

## 금지된 연산

`dynamic` 타입과 함께 `in`, `!in`, `..` 연산은 **허용되지 않습니다**.

## 사용 예시

### JavaScript 라이브러리와 상호작용

```kotlin
// JavaScript 라이브러리의 객체를 dynamic으로 처리
val jsObject: dynamic = js("{}")
jsObject.name = "Kotlin"
jsObject.version = 2.0
console.log(jsObject.name)  // "Kotlin" 출력
```

### JSON 데이터 처리

```kotlin
val jsonData: dynamic = JSON.parse("""{"key": "value", "count": 42}""")
println(jsonData.key)    // "value"
println(jsonData.count)  // 42
```

### DOM 조작

```kotlin
val element: dynamic = document.getElementById("myElement")
element.style.color = "red"
element.innerHTML = "Hello, World!"
element.addEventListener("click") {
    console.log("Element clicked!")
}
```

## 주의사항

- dynamic 타입은 컴파일 타임 타입 안전성을 포기합니다
- 런타임 오류가 발생할 수 있으므로 주의해서 사용해야 합니다
- 가능하면 타입이 지정된 Kotlin 래퍼나 외부 선언을 사용하는 것이 좋습니다

## 다음 단계

- [Kotlin/JS 개요](js-overview.html)
- [JavaScript 모듈과 상호작용](js-modules.html)
- [JavaScript에서 Kotlin 호출](js-to-kotlin-interop.html)
