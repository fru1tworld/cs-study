# 변수와 가변성

## 개요

기본적으로 Rust에서 변수는 **불변(immutable)**입니다. 값이 이름에 바인딩되면 그 값을 변경할 수 없습니다. 이것은 Rust가 제공하는 안전 기능 중 하나이지만, 필요할 때 변수를 가변으로 만들 수 있는 옵션이 있습니다.

## 불변 변수

`let`으로 변수를 선언하면 기본적으로 불변입니다:

```rust
fn main() {
    let x = 5;
    println!("The value of x is: {x}");
    x = 6;  // 컴파일 오류 발생
    println!("The value of x is: {x}");
}
```

이것은 컴파일 타임 오류를 발생시킵니다:

```text
error[E0384]: cannot assign twice to immutable variable `x`
```

컴파일러가 우발적인 변경을 방지하여 버그를 일찍 잡는 데 도움을 줍니다. 코드의 한 부분이 값이 변경되지 않을 것이라고 가정하고 다른 부분이 그것을 변경하면, 컴파일러가 런타임 버그 대신 컴파일 타임에 이 문제를 잡아냅니다.

## 변수를 가변으로 만들기

변수를 가변으로 만들려면 `mut` 키워드를 추가하세요:

```rust
fn main() {
    let mut x = 5;
    println!("The value of x is: {x}");
    x = 6;
    println!("The value of x is: {x}");
}
```

출력:

```text
The value of x is: 5
The value of x is: 6
```

`mut`를 사용하면 코드를 읽는 다른 개발자에게 의도를 전달하기도 합니다.

## 상수 선언하기

상수는 불변 변수와 유사하지만 중요한 차이점이 있습니다:

```rust
const THREE_HOURS_IN_SECONDS: u32 = 60 * 60 * 3;
```

### 주요 차이점:

- 상수에는 `mut`를 사용할 수 **없습니다**—항상 불변입니다
- `let` 대신 `const` 키워드로 선언
- **타입 어노테이션 필수**
- 전역 스코프를 포함한 모든 스코프에서 선언 가능
- 런타임 값이 아닌 상수 표현식으로만 설정 가능
- 해당 스코프 내에서 프로그램 전체 수명 동안 유효

**명명 규칙:** 상수는 ALL_UPPERCASE_WITH_UNDERSCORES를 사용

## 섀도잉

이전 변수와 같은 이름으로 새 변수를 선언할 수 있습니다. 새 변수가 이전 변수를 "섀도잉"합니다:

```rust
fn main() {
    let x = 5;

    let x = x + 1;  // x는 이제 6

    {
        let x = x * 2;  // x는 이제 12 (내부 스코프)
        println!("The value of x in the inner scope is: {x}");
    }

    println!("The value of x is: {x}");  // x는 다시 6
}
```

출력:

```text
The value of x in the inner scope is: 12
The value of x is: 6
```

### 섀도잉 vs 가변성

**핵심 차이점:** 섀도잉은 **새 변수**를 생성하고, `mut`는 같은 변수의 재할당을 허용합니다.

**섀도잉의 경우**, `let`을 잊으면 컴파일 오류가 발생합니다:

```rust
let x = 5;
x = 6;  // 오류: cannot assign twice to immutable variable
```

**섀도잉은 타입 변경을 허용**하지만, `mut`는 그렇지 않습니다:

```rust
// 작동함 - 섀도잉은 타입 변경 허용
let spaces = "   ";
let spaces = spaces.len();  // 문자열을 숫자로 변환

// 실패함 - mut는 타입 변경을 허용하지 않음
let mut spaces = "   ";
spaces = spaces.len();  // 오류: mismatched types
```

오류:

```text
error[E0308]: mismatched types
expected `&str`, found `usize`
```

섀도잉은 값을 변환하면서 변환 후에도 불변으로 유지하고 싶을 때 유용합니다.
