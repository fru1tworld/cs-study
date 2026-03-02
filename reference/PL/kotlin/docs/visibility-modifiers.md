# 가시성 수정자

클래스, 객체, 인터페이스, 생성자, 함수, 프로퍼티 및 그 세터는 가시성 수정자를 가질 수 있습니다. 게터는 항상 해당 프로퍼티와 동일한 가시성을 갖습니다.

## Kotlin의 네 가지 가시성 수정자

- `private`
- `protected`
- `internal`
- `public` (기본값)

## 패키지

함수, 프로퍼티, 클래스, 객체, 인터페이스는 패키지 내부에 최상위 수준에서 직접 선언될 수 있습니다.

```kotlin
// 파일명: example.kt
package foo

fun baz() { ... }
class Bar { ... }
```

### 패키지 수준 선언의 가시성 규칙

| 수정자 | 가시성 |
|--------|--------|
| **public** (기본값) | 어디서나 볼 수 있음 |
| **private** | 선언을 포함하는 파일 내부에서만 볼 수 있음 |
| **internal** | 같은 모듈 내 어디서나 볼 수 있음 |
| **protected** | 최상위 선언에는 사용 불가 |

### 예제

```kotlin
// 파일명: example.kt
package foo

private fun foo() { ... }          // example.kt 내부에서만 볼 수 있음
public var bar: Int = 5            // 어디서나 볼 수 있음
    private set                    // 세터는 example.kt에서만 볼 수 있음
internal val baz = 6               // 같은 모듈 내에서 볼 수 있음
```

## 클래스 멤버

클래스 내부에 선언된 멤버의 경우:

| 수정자 | 가시성 |
|--------|--------|
| **private** | 이 클래스 내부에서만 볼 수 있음 (모든 멤버 포함) |
| **protected** | private과 동일, 추가로 서브클래스에서 볼 수 있음 |
| **internal** | 선언하는 클래스를 볼 수 있는 같은 모듈 내 모든 클라이언트에서 볼 수 있음 |
| **public** | 선언하는 클래스를 볼 수 있는 모든 클라이언트에서 볼 수 있음 |

**중요:** 외부 클래스는 내부 클래스의 private 멤버를 볼 수 없습니다.

**오버라이드 동작:** 가시성을 명시적으로 지정하지 않고 `protected` 또는 `internal` 멤버를 오버라이드하면 원본과 동일한 가시성을 유지합니다.

### 예제

```kotlin
open class Outer {
    private val a = 1
    protected open val b = 2
    internal open val c = 3
    val d = 4  // 기본적으로 public

    protected class Nested {
        public val e: Int = 5
    }
}

class Subclass : Outer() {
    // a는 볼 수 없음
    // b, c, d는 볼 수 있음
    // Nested와 e는 볼 수 있음
    override val b = 5        // 'b'는 protected
    override val c = 7        // 'c'는 internal
}

class Unrelated(o: Outer) {
    // o.a, o.b는 볼 수 없음
    // o.c와 o.d는 볼 수 있음 (같은 모듈)
    // Outer.Nested는 볼 수 없고, Nested::e도 볼 수 없음
}
```

## 생성자

### 주 생성자 가시성 문법

```kotlin
class C private constructor(a: Int) { ... }
```

- **기본값:** 모든 생성자는 기본적으로 `public`
- **봉인 클래스:** 생성자는 기본적으로 `protected`
- **명시적 키워드:** 가시성을 지정하려면 `constructor` 키워드 사용

## 지역 선언

지역 변수, 함수, 클래스는 **가시성 수정자를 가질 수 없습니다**.

## 모듈

`internal` 가시성 수정자는 멤버가 같은 모듈 내에서 볼 수 있음을 의미합니다.

### 모듈을 구성하는 것

- IntelliJ IDEA 모듈
- Maven 프로젝트
- Gradle 소스 세트 (참고: `test` 소스 세트는 `main`의 internal 선언에 접근 가능)
