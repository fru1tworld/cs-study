# 패턴 문법

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

명명된 변수는 모든 값과 일치하는 반증 불가능(irrefutable) 패턴입니다. 그러나 패턴에서 선언된 변수는 새로운 스코프를 생성하고 외부 변수를 가립니다(shadow):

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
