# 패턴을 사용할 수 있는 모든 곳

## 개요

Rust에서 패턴은 언어 전반에 걸쳐 많은 곳에서 사용됩니다. 이 장에서는 패턴을 적용할 수 있는 모든 유효한 위치를 다룹니다.

**핵심 포인트:**
- 패턴은 Rust의 여러 구문에서 사용되는 강력한 기능
- `match`, `let`, `if let`, `while let`, `for`, 함수 매개변수에서 사용 가능
- 각 위치마다 패턴의 동작과 요구 사항이 다름

## `match` 분기 (Arms)

`match` 표현식의 분기에서 패턴을 사용합니다:

```rust
match VALUE {
    PATTERN => EXPRESSION,
    PATTERN => EXPRESSION,
    PATTERN => EXPRESSION,
}
```

**예제:**

```rust
match x {
    None => None,
    Some(i) => Some(i + 1),
}
```

**핵심 포인트:**
- `match` 표현식은 **완전해야(exhaustive)** 함 - 모든 가능성을 다루어야 함
- 마지막 분기에서 포괄 패턴(catch-all pattern)인 `_`를 사용하여 나머지 경우를 처리
- `_` 패턴은 모든 것과 매칭되지만 변수에 바인딩하지 않음

## `let` 문

모든 `let` 문은 명시적으로 보이지 않더라도 패턴을 사용합니다.

**구문:**

```rust
let PATTERN = EXPRESSION;
```

**간단한 예제:**

```rust
let x = 5;
```

**튜플 구조 분해 예제:**

```rust
fn main() {
    let (x, y, z) = (1, 2, 3);
}
```

패턴 `(x, y, z)`는 튜플을 구조 분해하여 각 값을 해당 변수에 바인딩합니다.

**에러 처리:**

패턴의 요소 수는 값의 요소 수와 일치해야 합니다:

```rust
fn main() {
    let (x, y) = (1, 2, 3);  // 에러: 패턴은 2개 요소, 튜플은 3개 요소
}
```

**에러 메시지:**

```
error[E0308]: mismatched types
expected a tuple with 3 elements, found one with 2 elements
```

## 조건부 `if let` 표현식

`if let`은 단일 경우만 매칭하는 더 짧은 구문을 제공하며, `else if`와 `else if let`과 결합할 수 있습니다.

**예제:**

```rust
fn main() {
    let favorite_color: Option<&str> = None;
    let is_tuesday = false;
    let age: Result<u8, _> = "34".parse();

    if let Some(color) = favorite_color {
        println!("Using your favorite color, {color}, as the background");
    } else if is_tuesday {
        println!("Tuesday is green day!");
    } else if let Ok(age) = age {
        if age > 30 {
            println!("Using purple as the background color");
        } else {
            println!("Using orange as the background color");
        }
    } else {
        println!("Using blue as the background color");
    }
}
```

**핵심 포인트:**
- `if let`은 기존 변수를 섀도잉(shadowing)하는 새 변수를 도입할 수 있음
- 변수 스코프는 해당 블록 내로 제한됨
- `match`와 달리 컴파일러는 `if let`의 완전성(exhaustiveness)을 **검사하지 않음**

## `while let` 조건부 루프

`while let`은 패턴이 계속 매칭되는 동안 루프를 실행합니다.

**예제:**

```rust
fn main() {
    let (tx, rx) = std::sync::mpsc::channel();
    std::thread::spawn(move || {
        for val in [1, 2, 3] {
            tx.send(val).unwrap();
        }
    });

    while let Ok(value) = rx.recv() {
        println!("{value}");
    }
}
```

**출력:**

```
1
2
3
```

`recv()`가 `Ok`를 반환하는 동안 루프가 계속되고, 송신자가 연결을 끊으면(`Err` 발생) 종료됩니다.

## `for` 루프

`for` 루프에서 `for` 키워드 뒤에 오는 값은 패턴입니다.

**구문:**

```rust
for PATTERN in ITERABLE {
    // 코드
}
```

**튜플 구조 분해 예제:**

```rust
fn main() {
    let v = vec!['a', 'b', 'c'];

    for (index, value) in v.iter().enumerate() {
        println!("{value} is at index {index}");
    }
}
```

**출력:**

```
a is at index 0
b is at index 1
c is at index 2
```

`enumerate()` 메서드는 `(index, value)` 튜플을 생성하며, 이를 패턴으로 구조 분해합니다.

## 함수 매개변수

함수 매개변수는 값을 구조 분해할 수 있는 패턴입니다.

**간단한 예제:**

```rust
fn foo(x: i32) {
    // 코드
}
```

**튜플 구조 분해 예제:**

```rust
fn print_coordinates(&(x, y): &(i32, i32)) {
    println!("Current location: ({x}, {y})");
}

fn main() {
    let point = (3, 5);
    print_coordinates(&point);
}
```

**출력:**

```
Current location: (3, 5)
```

매개변수 패턴 `&(x, y)`는 튜플 참조를 구조 분해합니다.

**핵심 포인트:**
- 함수 매개변수에서 패턴을 사용하여 직접 구조 분해 가능
- 클로저 매개변수 목록에서도 함수 매개변수와 동일하게 패턴이 작동함

## 요약

패턴은 6개의 주요 위치에서 나타납니다:

| 위치 | 특징 |
|------|------|
| `match` 분기 | 완전해야 함 (모든 경우 처리) |
| `let` 문 | 변수 바인딩 및 구조 분해 |
| `if let` 표현식 | 조건부 패턴 매칭 |
| `while let` 루프 | 패턴 기반 루프 조건 |
| `for` 루프 | 값 반복 및 구조 분해 |
| 함수 매개변수 | 인수 구조 분해 |

**핵심 포인트:**
- `match`는 완전성이 필수이지만 `if let`은 선택적
- 패턴은 구조 분해를 통해 복잡한 데이터 타입을 쉽게 분해
- 다음 섹션에서는 **논박 가능성(refutability)** - 패턴이 반드시 성공해야 하는지 실패할 수 있는지 - 를 탐구
