# 반환과 점프

## 개요

Kotlin에는 세 가지 구조적 점프 표현식이 있습니다:

- **`return`** - 가장 가까운 둘러싸는 함수나 익명 함수에서 반환
- **`break`** - 가장 가까운 둘러싸는 루프를 종료
- **`continue`** - 가장 가까운 둘러싸는 루프의 다음 단계로 진행

이 모든 표현식은 더 큰 표현식의 일부로 사용할 수 있습니다:

```kotlin
val s = person.name ?: return
```

이 표현식들의 타입은 `Nothing` 타입입니다.

---

## break와 continue 레이블

Kotlin의 모든 표현식은 레이블로 표시될 수 있습니다. 레이블은 식별자 뒤에 `@` 기호가 오는 형태입니다 (예: `abc@` 또는 `fooBar@`).

### 예제:

```kotlin
loop@ for (i in 1..100) { // ... }
```

`break`나 `continue`를 레이블로 한정할 수 있습니다:

```kotlin
loop@ for (i in 1..100) {
    for (j in 1..100) {
        if (...) break@loop
    }
}
```

- 레이블로 한정된 `break`는 해당 레이블이 표시된 루프 바로 다음 실행 지점으로 점프합니다
- `continue`는 해당 루프의 다음 반복으로 진행합니다

**참고:** 비지역 사용은 둘러싸는 인라인 함수에서 사용되는 람다 표현식에서 유효합니다.

---

## 레이블로 반환

함수는 함수 리터럴, 지역 함수, 객체 표현식을 사용하여 중첩될 수 있습니다. 한정된 `return`을 사용하면 외부 함수에서 반환할 수 있습니다.

### 명시적 레이블로 람다에서 반환:

```kotlin
fun foo() {
    listOf(1, 2, 3, 4, 5).forEach lit@{
        if (it == 3) return@lit // 람다 호출자로의 지역 반환
        print(it)
    }
    print(" done with explicit label")
}
```

### 암시적 레이블로 람다에서 반환:

```kotlin
fun foo() {
    listOf(1, 2, 3, 4, 5).forEach {
        if (it == 3) return@forEach // 람다 호출자로의 지역 반환
        print(it)
    }
    print(" done with implicit label")
}
```

### 익명 함수 사용:

```kotlin
fun foo() {
    listOf(1, 2, 3, 4, 5).forEach(fun(value: Int) {
        if (value == 3) return // 익명 함수에서의 지역 반환
        print(value)
    })
    print(" done with anonymous function")
}
```

### run 람다로 break 시뮬레이션:

```kotlin
fun foo() {
    run loop@{
        listOf(1, 2, 3, 4, 5).forEach {
            if (it == 3) return@loop // run에 전달된 람다에서의 비지역 반환
            print(it)
        }
    }
    print(" done with nested loop")
}
```

---

## 핵심 사항

- 람다에서의 지역 반환은 일반 루프의 `continue`와 유사합니다
- `break`에 대한 직접적인 등가물은 없지만 외부 `run` 람다를 사용하여 시뮬레이션할 수 있습니다
- 값을 반환할 때 파서는 한정된 반환에 우선순위를 부여합니다:
  ```kotlin
  return@a 1  // 레이블 @a에서 1을 반환 (레이블이 지정된 표현식이 아님)
  ```
- 람다에서의 비지역 반환은 람다가 인라인 함수 역할을 할 때 가능합니다
