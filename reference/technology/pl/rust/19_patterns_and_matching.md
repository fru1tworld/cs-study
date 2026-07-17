# 패턴과 매칭

# 패턴과 매칭

> **원문:** https://doc.rust-lang.org/book/ch19-00-patterns.html

## 개요

패턴(Pattern)은 Rust에서 복잡하거나 단순한 타입의 구조와 매칭하기 위한 특별한 문법입니다. `match` 표현식 및 다른 구조와 함께 패턴을 사용하면 프로그램의 제어 흐름을 더 세밀하게 제어할 수 있습니다.

## 패턴의 구성 요소

패턴은 다음 요소들의 조합으로 구성됩니다:

- **리터럴(Literals)**: 구체적인 값
- **구조 분해된 배열, 열거형, 구조체, 튜플**: 복합 타입의 분해
- **변수(Variables)**: 값을 바인딩할 이름
- **와일드카드(Wildcards)**: 모든 값과 매칭
- **플레이스홀더(Placeholders)**: 특정 위치의 값을 무시

## 패턴 예시

다음은 패턴의 예시입니다:

- `x` - 단순 변수 패턴
- `(a, 3)` - 튜플 패턴 (변수와 리터럴의 조합)
- `Some(Color::Red)` - 열거형 패턴

**핵심 포인트:**
- 패턴이 유효한 컨텍스트에서 이러한 구성 요소들은 데이터의 형태를 설명함
- 프로그램은 값을 패턴과 비교하여 특정 코드를 실행하기에 적합한 형태인지 판단함

## 패턴의 작동 방식

패턴을 사용하려면 어떤 값과 비교합니다. 패턴이 값과 매칭되면 코드에서 값의 각 부분을 사용합니다.

6장의 `match` 표현식(동전 분류기 예제)을 떠올려 보세요:
- 값이 패턴의 형태에 맞으면 이름이 지정된 부분을 사용할 수 있음
- 값이 패턴과 맞지 않으면 해당 패턴과 연관된 코드는 실행되지 않음

```rust
// match 표현식의 기본 구조 예시
match value {
    pattern1 => code1,
    pattern2 => code2,
    _ => default_code,
}
```

## 이 장에서 다루는 내용

이 장은 패턴에 관한 모든 것을 다루는 참조 자료입니다:

1. **패턴을 사용할 수 있는 유효한 위치**
   - `match` 표현식
   - `if let` 조건부 표현식
   - `while let` 조건부 루프
   - `for` 루프
   - `let` 문
   - 함수 매개변수

2. **반박 가능(Refutable)과 반박 불가능(Irrefutable) 패턴의 차이**
   - 반박 불가능 패턴: 전달된 모든 값과 매칭되는 패턴
   - 반박 가능 패턴: 일부 값에서 매칭에 실패할 수 있는 패턴

3. **다양한 패턴 문법**
   - 리터럴 매칭
   - 명명된 변수
   - 다중 패턴
   - 범위 매칭
   - 구조 분해
   - 값 무시
   - 매치 가드
   - `@` 바인딩

**핵심 포인트:**
- 패턴은 Rust에서 값의 구조를 표현하는 강력한 방법
- `match` 표현식은 패턴의 가장 일반적인 사용처
- 패턴을 마스터하면 Rust 코드를 더 명확하고 표현력 있게 작성할 수 있음

---

# 패턴을 사용할 수 있는 모든 곳

> **원문:** https://doc.rust-lang.org/book/ch19-01-all-the-places-for-patterns.html

## 개요

Rust에서 패턴은 언어 전반에 걸쳐 많은 곳에서 사용됩니다. 이 장에서는 패턴을 적용할 수 있는 모든 유효한 위치를 다룹니다.

**핵심 포인트:**
- 패턴은 Rust의 여러 구문에서 사용되는 강력한 기능
- `match`, `let`, `if let`, `while let`, `for`, 함수 매개변수에서 사용 가능
- 각 위치마다 패턴의 동작과 요구 사항이 다름

## `match` 갈래 (Arms)

`match` 표현식의 갈래에서 패턴을 사용합니다:

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
- 마지막 갈래에서 포괄 패턴(catch-all pattern)인 `_`를 사용하여 나머지 경우를 처리
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

**오류 처리:**

패턴의 요소 수는 값의 요소 수와 일치해야 합니다:

```rust
fn main() {
    let (x, y) = (1, 2, 3);  // 오류: 패턴은 2개 요소, 튜플은 3개 요소
}
```

**오류 메시지:**

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
| `match` 갈래 | 완전해야 함 (모든 경우 처리) |
| `let` 문 | 변수 바인딩 및 구조 분해 |
| `if let` 표현식 | 조건부 패턴 매칭 |
| `while let` 루프 | 패턴 기반 루프 조건 |
| `for` 루프 | 값 반복 및 구조 분해 |
| 함수 매개변수 | 인수 구조 분해 |

**핵심 포인트:**
- `match`는 완전성이 필수이지만 `if let`은 선택적
- 패턴은 구조 분해를 통해 복잡한 데이터 타입을 쉽게 분해
- 다음 섹션에서는 **반박 가능성(refutability)** - 패턴이 반드시 성공해야 하는지 실패할 수 있는지 - 를 탐구

---

# 반박 가능성: 패턴이 매칭에 실패할 수 있는지 여부

> **원문:** https://doc.rust-lang.org/book/ch19-02-refutability.html

## 개요

Rust의 패턴은 두 가지 형태로 나뉩니다:

1. **반박 불가능한 패턴(Irrefutable Patterns)**: **모든 가능한 값**에 대해 매칭되는 패턴입니다. 매칭에 실패할 수 없습니다.
   - 예: `let x = 5;`에서 `x`는 모든 것과 매칭됨

2. **반박 가능한 패턴(Refutable Patterns)**: 일부 가능한 값에 대해 **매칭에 실패할 수 있는** 패턴입니다.
   - 예: `if let Some(x) = a_value`에서 `Some(x)`는 값이 `None`이면 실패함

## 패턴 사용 규칙

**핵심 포인트:**
- **반박 불가능한 패턴만 허용**: `let` 문, 함수 매개변수, `for` 루프
- **반박 가능하거나 반박 불가능한 패턴 모두 허용**: `if let`, `while let`, `let...else` 표현식
- 컴파일러는 `if let`/`while let`/`let...else`에서 반박 불가능한 패턴을 사용하면 경고를 발생시킴 (이들은 실패를 처리하도록 설계되었기 때문)

## 예제: `let`에서 반박 가능한 패턴 사용 시 오류

```rust
fn main() {
    let some_option_value: Option<i32> = None;
    let Some(x) = some_option_value;  // ERROR
}
```

**컴파일러 오류:**
```
error[E0005]: refutable pattern in local binding
  |
3 |     let Some(x) = some_option_value;
  |         ^^^^^^^ pattern `None` not covered
  |
  = note: `let` bindings require an "irrefutable pattern"
```

**핵심 포인트:**
- `let` 바인딩은 반박 불가능한 패턴을 요구함
- `Some(x)` 패턴은 `None` 케이스를 처리하지 않으므로 반박 가능함
- 컴파일러는 커버되지 않은 패턴(`None`)을 알려줌

## 해결책: `let...else` 사용

```rust
fn main() {
    let some_option_value: Option<i32> = None;
    let Some(x) = some_option_value else {
        return;
    };
}
```

**핵심 포인트:**
- `let...else`는 반박 가능한 패턴을 허용함
- `else` 블록은 패턴이 매칭되지 않을 때 실행됨
- `else` 블록은 반드시 분기를 벗어나야 함 (`return`, `break`, `panic!` 등)

## 경고: `let...else`에서 반박 불가능한 패턴 사용

```rust
fn main() {
    let x = 5 else {
        return;
    };
}
```

**컴파일러 경고:**
```
warning: irrefutable `let...else` pattern
  |
2 |     let x = 5 else {
  |     ^^^^^^^^^
  |
  = note: this pattern will always match, so the `else` clause is useless
```

**핵심 포인트:**
- 반박 불가능한 패턴은 항상 매칭되므로 `else` 절이 무의미함
- 이런 경우 일반 `let` 문을 사용해야 함

## `match` 갈래 규칙

**핵심 포인트:**
- `match` 갈래는 **반박 가능한 패턴**을 사용해야 함
- 단, **마지막 갈래**는 남은 모든 값을 매칭하기 위해 반박 불가능한 패턴을 사용해야 함
- 이렇게 해야 모든 가능한 값이 처리됨을 보장함

## 요약

| 구문 | 허용되는 패턴 유형 |
|------|-------------------|
| `let` 문 | 반박 불가능한 패턴만 |
| 함수 매개변수 | 반박 불가능한 패턴만 |
| `for` 루프 | 반박 불가능한 패턴만 |
| `if let` | 반박 가능한 패턴 (권장) |
| `while let` | 반박 가능한 패턴 (권장) |
| `let...else` | 반박 가능한 패턴 (권장) |
| `match` 갈래 | 반박 가능한 패턴 (마지막 갈래 제외) |

---

# 패턴 문법

> **원문:** https://doc.rust-lang.org/book/ch19-03-pattern-syntax.html

## 개요

이 섹션에서는 Rust에서 유효한 모든 패턴 문법을 다루고, 각 패턴을 언제 왜 사용하는지 설명합니다.

## 리터럴 매칭

리터럴 값에 대해 직접 패턴을 매칭할 수 있습니다:

```rust
fn main() {
    let x = 1;

    match x {
        1 => println!("one"),
        2 => println!("two"),
        3 => println!("three"),
        _ => println!("anything"),
    }
}
```

**핵심 포인트:**
- 코드가 특정 구체적인 값에 따라 동작해야 할 때 유용함
- 정수, 문자열 등 다양한 리터럴 타입과 매칭 가능

## 명명된 변수 매칭

명명된 변수는 모든 값과 일치하는 반박 불가능(irrefutable) 패턴입니다. 그러나 패턴에서 선언된 변수는 새로운 스코프를 생성하고 외부 변수를 가립니다(shadow):

```rust
fn main() {
    let x = Some(5);
    let y = 10;

    match x {
        Some(50) => println!("Got 50"),
        Some(y) => println!("Matched, y = {y}"),  // y가 외부 y를 가림
        _ => println!("Default case, x = {x:?}"),
    }

    println!("at the end: x = {x:?}, y = {y}");  // y는 여전히 10
}
```

**핵심 포인트:**
- `match` 내부의 `Some(y)`는 새로운 변수 `y`를 생성
- 이 `y`는 외부 스코프의 `y`와 다른 변수
- `match` 블록이 끝나면 내부 `y`는 스코프를 벗어남

## 다중 패턴 매칭

`|` 연산자(or 연산자)를 사용하여 여러 패턴을 매칭할 수 있습니다:

```rust
fn main() {
    let x = 1;

    match x {
        1 | 2 => println!("one or two"),
        3 => println!("three"),
        _ => println!("anything"),
    }
}
```

**핵심 포인트:**
- `|`는 "또는"을 의미
- 여러 값에 대해 동일한 동작을 수행할 때 유용

## `..=`로 범위 매칭

포괄적인(inclusive) 값의 범위를 매칭합니다:

```rust
fn main() {
    let x = 5;

    match x {
        1..=5 => println!("one through five"),
        _ => println!("something else"),
    }
}
```

범위는 숫자 값과 `char`에서 동작합니다:

```rust
fn main() {
    let x = 'c';

    match x {
        'a'..='j' => println!("early ASCII letter"),
        'k'..='z' => println!("late ASCII letter"),
        _ => println!("something else"),
    }
}
```

**핵심 포인트:**
- `..=`는 양 끝 값을 포함하는 범위
- 숫자와 문자 타입에서만 사용 가능
- 컴파일러가 컴파일 시점에 범위가 비어있지 않음을 확인

## 값 구조 분해

### 구조체 구조 분해

패턴을 사용하여 구조체 필드를 분해합니다:

```rust
struct Point {
    x: i32,
    y: i32,
}

fn main() {
    let p = Point { x: 0, y: 7 };

    let Point { x, y } = p;  // 축약 문법
    assert_eq!(0, x);
    assert_eq!(7, y);
}
```

구조 분해와 리터럴 값 테스트를 혼합할 수 있습니다:

```rust
struct Point {
    x: i32,
    y: i32,
}

fn main() {
    let p = Point { x: 0, y: 7 };

    match p {
        Point { x, y: 0 } => println!("On the x axis at {x}"),
        Point { x: 0, y } => println!("On the y axis at {y}"),
        Point { x, y } => println!("On neither axis: ({x}, {y})"),
    }
}
```

**핵심 포인트:**
- `Point { x, y }`는 `Point { x: x, y: y }`의 축약형
- 특정 필드 값을 리터럴과 매칭하면서 다른 필드는 변수로 캡처 가능

### 열거형 구조 분해

열거형 배리언트를 정의에 따라 구조 분해합니다:

```rust
enum Message {
    Quit,
    Move { x: i32, y: i32 },
    Write(String),
    ChangeColor(i32, i32, i32),
}

fn main() {
    let msg = Message::ChangeColor(0, 160, 255);

    match msg {
        Message::Quit => println!("Quit variant"),
        Message::Move { x, y } => println!("Move in x: {x}, y: {y}"),
        Message::Write(text) => println!("Text: {text}"),
        Message::ChangeColor(r, g, b) => println!("RGB({r}, {g}, {b})"),
    }
}
```

**핵심 포인트:**
- 각 배리언트의 구조에 맞는 패턴 사용
- 유닛 배리언트, 튜플 구조체 배리언트, 구조체 배리언트 각각 다르게 매칭

### 중첩된 구조체와 열거형

```rust
enum Color {
    Rgb(i32, i32, i32),
    Hsv(i32, i32, i32),
}

enum Message {
    ChangeColor(Color),
    // ... 다른 배리언트
}

fn main() {
    let msg = Message::ChangeColor(Color::Hsv(0, 160, 255));

    match msg {
        Message::ChangeColor(Color::Rgb(r, g, b)) => {
            println!("RGB: {r}, {g}, {b}");
        }
        Message::ChangeColor(Color::Hsv(h, s, v)) => {
            println!("HSV: {h}, {s}, {v}");
        }
        _ => (),
    }
}
```

### 구조체와 튜플

구조 분해 패턴을 혼합하고 중첩할 수 있습니다:

```rust
fn main() {
    struct Point {
        x: i32,
        y: i32,
    }

    let ((feet, inches), Point { x, y }) = ((3, 10), Point { x: 3, y: -10 });
}
```

**핵심 포인트:**
- 복잡한 타입도 한 번에 구조 분해 가능
- 패턴은 값의 형태를 반영해야 함

## 값 무시하기

### `_`로 전체 값 무시

바인딩 없이 전체 값을 무시합니다:

```rust
fn foo(_: i32, y: i32) {
    println!("This code only uses the y parameter: {y}");
}
```

**핵심 포인트:**
- 함수 시그니처에서 사용하지 않는 매개변수에 유용
- 트레이트 구현 시 특정 매개변수가 필요 없을 때 사용

### `_`로 값의 일부 무시

패턴의 특정 부분을 무시합니다:

```rust
fn main() {
    let mut setting_value = Some(5);
    let new_setting_value = Some(10);

    match (setting_value, new_setting_value) {
        (Some(_), Some(_)) => {
            println!("Can't overwrite an existing value");
        }
        _ => {
            setting_value = new_setting_value;
        }
    }
}
```

튜플에서 여러 값을 무시합니다:

```rust
fn main() {
    let numbers = (2, 4, 8, 16, 32);

    match numbers {
        (first, _, third, _, fifth) => {
            println!("Some numbers: {first}, {third}, {fifth}");
        }
    }
}
```

### `_` 접두사로 미사용 변수 표시

변수 이름 앞에 `_`를 붙여 미사용 변수 경고를 억제합니다:

```rust
fn main() {
    let _x = 5;  // 경고 없음
    let y = 10;  // 경고: 미사용 변수
}
```

**중요한 차이점:** `_x`는 여전히 값을 바인딩하지만, `_`는 전혀 바인딩하지 않습니다:

```rust
// 오류 발생 - String이 _s로 이동됨
fn main() {
    let s = Some(String::from("Hello!"));
    if let Some(_s) = s {
        println!("found a string");
    }
    println!("{s:?}");  // 오류: s가 이동됨
}

// 컴파일됨 - String이 이동되지 않음
fn main() {
    let s = Some(String::from("Hello!"));
    if let Some(_) = s {
        println!("found a string");
    }
    println!("{s:?}");  // OK: s는 이동되지 않음
}
```

### `..`로 나머지 부분 무시

`..` 문법으로 값의 나머지 부분을 무시합니다:

```rust
fn main() {
    struct Point {
        x: i32,
        y: i32,
        z: i32,
    }

    let origin = Point { x: 0, y: 0, z: 0 };

    match origin {
        Point { x, .. } => println!("x is {x}"),
    }
}
```

튜플에서 `..` 사용:

```rust
fn main() {
    let numbers = (2, 4, 8, 16, 32);

    match numbers {
        (first, .., last) => {
            println!("Some numbers: {first}, {last}");
        }
    }
}
```

**모호성 규칙:** `..`는 패턴당 한 번만 사용할 수 있습니다:

```rust
// 컴파일되지 않음
fn main() {
    let numbers = (2, 4, 8, 16, 32);

    match numbers {
        (.., second, ..) => {  // 오류: 모호함
            println!("Some numbers: {second}")
        },
    }
}
```

**핵심 포인트:**
- `..`는 하나 이상의 값을 무시
- 모호성을 피하기 위해 패턴당 한 번만 사용 가능

## 매치 가드 (Match Guards)

매치 가드는 암(arm)이 선택되기 위해 추가로 만족해야 하는 `if` 조건입니다. `match` 표현식에서만 사용 가능합니다:

```rust
fn main() {
    let num = Some(4);

    match num {
        Some(x) if x % 2 == 0 => println!("The number {x} is even"),
        Some(x) => println!("The number {x} is odd"),
        None => (),
    }
}
```

매치 가드를 사용하여 변수 가림(shadowing)을 피할 수 있습니다:

```rust
fn main() {
    let x = Some(5);
    let y = 10;

    match x {
        Some(50) => println!("Got 50"),
        Some(n) if n == y => println!("Matched, n = {n}"),  // 외부 y와 비교
        _ => println!("Default case, x = {x:?}"),
    }

    println!("at the end: x = {x:?}, y = {y}");
}
```

`|`로 여러 패턴을 매치 가드와 결합합니다:

```rust
fn main() {
    let x = 4;
    let y = false;

    match x {
        4 | 5 | 6 if y => println!("yes"),  // 가드가 모든 패턴에 적용
        _ => println!("no"),
    }
}
```

가드는 모든 패턴에 적용됩니다: `(4 | 5 | 6) if y => ...`

**핵심 포인트:**
- 패턴만으로 표현할 수 없는 복잡한 조건에 유용
- 가드는 `|`로 결합된 모든 패턴에 적용됨
- 외부 변수를 참조하여 섀도잉 문제 해결 가능

## @ 바인딩

`@` 연산자는 값을 패턴과 테스트하면서 동시에 변수에 바인딩합니다:

```rust
fn main() {
    enum Message {
        Hello { id: i32 },
    }

    let msg = Message::Hello { id: 5 };

    match msg {
        Message::Hello { id: id_variable @ 3..=7 } => {
            println!("Found an id in range: {id_variable}")
        }
        Message::Hello { id: 10..=12 } => {
            println!("Found an id in another range")
        }
        Message::Hello { id } => println!("Found some other id: {id}"),
    }
}
```

출력: `Found an id in range: 5`

**핵심 포인트:**
- `@`를 사용하면 값을 테스트하면서 동시에 캡처 가능
- 범위 패턴과 함께 사용할 때 특히 유용
- 첫 번째 암은 범위 테스트와 값 캡처를 동시에 수행
- 두 번째 암은 범위만 테스트하고 값을 사용할 수 없음

## 요약

Rust의 패턴은 다양한 종류의 데이터를 구별하는 강력한 방법을 제공합니다.

**주요 패턴 문법:**
- **리터럴 매칭**: 특정 값과 직접 비교
- **명명된 변수**: 모든 값을 캡처 (섀도잉 주의)
- **다중 패턴 (`|`)**: 여러 패턴 중 하나와 매칭
- **범위 (`..=`)**: 숫자나 문자의 범위 매칭
- **구조 분해**: 구조체, 열거형, 튜플의 내부 값 추출
- **값 무시 (`_`, `..`)**: 필요 없는 값 무시
- **매치 가드**: 추가 조건으로 암 선택 제한
- **@ 바인딩**: 테스트와 캡처 동시 수행

컴파일러는 `match` 표현식에서 패턴의 완전성(exhaustiveness)을 보장합니다. `let` 문과 함수 매개변수의 패턴은 값을 더 작은 부분으로 구조 분해할 수 있게 해줍니다.
