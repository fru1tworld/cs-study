# 열거형과 패턴 매칭

# 열거형과 패턴 매칭

> **원문:** https://doc.rust-lang.org/book/ch06-00-enums.html

## 챕터 개요

이 챕터에서는 Rust의 열거형(enums)과 패턴 매칭을 다룹니다. 주요 주제는 다음과 같습니다:

### 학습 내용

1. **열거형 정의 및 사용**
   - 열거형을 사용하면 가능한 변형을 열거하여 타입을 정의할 수 있습니다
   - 열거형은 데이터와 함께 의미를 인코딩할 수 있습니다

2. **`Option` 열거형**
   - 특히 유용한 내장 열거형
   - 값이 무언가일 수도 있고 아무것도 아닐 수도 있음을 표현합니다

3. **`match`를 사용한 패턴 매칭**
   - `match` 표현식은 서로 다른 열거형 값에 대해 서로 다른 코드를 쉽게 실행할 수 있게 합니다
   - 다양한 경우를 처리하는 강력한 방법입니다

4. **`if let` 구문**
   - 열거형을 처리하기 위한 편리하고 간결한 관용구
   - 더 간단한 경우에 `match`의 대안이 됩니다

---

# 열거형 정의하기

> **원문:** https://doc.rust-lang.org/book/ch06-01-defining-an-enum.html

## 개요

열거형을 사용하면 가능한 변형을 열거하여 타입을 정의할 수 있습니다. 구조체가 관련 필드를 함께 그룹화하는 반면, 열거형은 값이 가능한 값들의 집합 중 하나임을 표현할 수 있게 합니다.

## 기본 열거형 정의

열거형은 값이 여러 개의 개별적인 가능성 중 하나일 수 있을 때 유용합니다. 예를 들어, IP 주소의 경우:

```rust
enum IpAddrKind {
    V4,
    V6,
}

fn main() {
    let four = IpAddrKind::V4;
    let six = IpAddrKind::V6;

    route(IpAddrKind::V4);
    route(IpAddrKind::V6);
}

fn route(ip_kind: IpAddrKind) {}
```

열거형 변형은 열거형의 식별자 아래에 네임스페이스가 지정되며 `::`를 사용하여 접근합니다. `IpAddrKind::V4`와 `IpAddrKind::V6`은 모두 같은 타입인 `IpAddrKind`입니다.

## 연관 데이터가 있는 열거형 값

열거형 변형과 연관 데이터를 보관하기 위해 구조체를 사용하는 대신:

```rust
fn main() {
    enum IpAddrKind {
        V4,
        V6,
    }

    struct IpAddr {
        kind: IpAddrKind,
        address: String,
    }

    let home = IpAddr {
        kind: IpAddrKind::V4,
        address: String::from("127.0.0.1"),
    };

    let loopback = IpAddr {
        kind: IpAddrKind::V6,
        address: String::from("::1"),
    };
}
```

데이터를 열거형 변형에 직접 첨부할 수 있습니다:

```rust
fn main() {
    enum IpAddr {
        V4(String),
        V6(String),
    }

    let home = IpAddr::V4(String::from("127.0.0.1"));
    let loopback = IpAddr::V6(String::from("::1"));
}
```

열거형 변형 이름은 자동으로 생성자 함수가 됩니다.

## 변형에서 다른 타입

각 열거형 변형은 서로 다른 타입과 양의 연관 데이터를 가질 수 있습니다:

```rust
fn main() {
    enum IpAddr {
        V4(u8, u8, u8, u8),
        V6(String),
    }

    let home = IpAddr::V4(127, 0, 0, 1);
    let loopback = IpAddr::V6(String::from("::1"));
}
```

## 복잡한 열거형 예제

열거형은 구조체를 포함한 다양한 데이터 타입을 보관할 수 있습니다:

```rust
enum Message {
    Quit,
    Move { x: i32, y: i32 },
    Write(String),
    ChangeColor(i32, i32, i32),
}

fn main() {}
```

이 열거형은 네 가지 변형을 가집니다:
- `Quit`: 연관 데이터 없음
- `Move`: 구조체처럼 명명된 필드
- `Write`: 단일 `String`
- `ChangeColor`: 세 개의 `i32` 값

## 열거형의 메서드

구조체처럼 `impl`을 사용하여 열거형에 메서드를 정의할 수 있습니다:

```rust
fn main() {
    enum Message {
        Quit,
        Move { x: i32, y: i32 },
        Write(String),
        ChangeColor(i32, i32, i32),
    }

    impl Message {
        fn call(&self) {
            // 메서드 본문은 여기에 정의됩니다
        }
    }

    let m = Message::Write(String::from("hello"));
    m.call();
}
```

## Option 열거형

표준 라이브러리의 `Option<T>` 열거형은 무언가이거나 아무것도 아닌 값을 나타냅니다:

```rust
#![allow(unused)]
fn main() {
enum Option<T> {
    None,
    Some(T),
}
}
```

`Option<T>`는 매우 유용해서 프렐루드에 포함되어 있어 `Option::` 접두사 없이 `Some`과 `None`을 직접 사용할 수 있습니다.

### Option 사용하기

```rust
fn main() {
    let some_number = Some(5);
    let some_char = Some('e');
    let absent_number: Option<i32> = None;
}
```

- `some_number`의 타입은 `Option<i32>`입니다
- `some_char`의 타입은 `Option<char>`입니다
- `absent_number`의 경우, 컴파일러가 `None`만으로는 타입을 추론할 수 없으므로 명시적인 타입 주석이 필요합니다

### 안전성 이점

다른 언어의 null과 달리, Rust는 `Option<T>`와 `T`를 다른 타입으로 취급합니다. 이는 null 관련 버그를 방지합니다:

```rust
fn main() {
    let x: i8 = 5;
    let y: Option<i8> = Some(5);

    let sum = x + y;  // 컴파일 오류!
}
```

이 코드는 컴파일에 실패합니다:
```
error[E0277]: cannot add `Option<i8>` to `i8`
```

**핵심 이점**: 작업을 수행하기 전에 `Option<T>`를 `T`로 명시적으로 변환해야 합니다. 이는 값이 `None`일 수 있을 때 유효하다고 가정하는 위험을 제거합니다. 컴파일러가 `None` 케이스를 명시적으로 처리하도록 강제합니다.

### 이것이 중요한 이유

- 변수가 `Option<T>`가 아니면 유효한 값이 있다고 안전하게 가정할 수 있습니다
- `Option<T>` 타입만 `None`일 수 있습니다
- 값을 사용하기 전에 `Some(T)`와 `None` 케이스를 모두 명시적으로 처리해야 합니다

`Option<T>`의 값을 추출하고 작업하려면 `match` 표현식을 사용합니다. 값이 어떤 열거형 변형인지에 따라 다른 코드를 실행합니다.

---

# `match` 제어 흐름 구조

> **원문:** https://doc.rust-lang.org/book/ch06-02-match.html

## 개요

Rust에는 값을 일련의 패턴과 비교하고 일치하는 패턴에 따라 코드를 실행할 수 있는 `match`라는 강력한 제어 흐름 구조가 있습니다. 패턴은 리터럴 값, 변수 이름, 와일드카드 및 기타 많은 것들로 구성될 수 있습니다.

`match` 표현식을 동전 분류기처럼 생각하세요: 동전이 다양한 크기의 구멍이 있는 트랙을 따라 미끄러지고, 각 동전은 맞는 첫 번째 구멍에 떨어집니다. 마찬가지로, 값은 `match`의 각 패턴을 통과하고, 첫 번째로 일치하는 패턴에서 값은 관련 코드 블록을 실행합니다.

## 열거형을 사용한 기본 예제

```rust
enum Coin {
    Penny,
    Nickel,
    Dime,
    Quarter,
}

fn value_in_cents(coin: Coin) -> u8 {
    match coin {
        Coin::Penny => 1,
        Coin::Nickel => 5,
        Coin::Dime => 10,
        Coin::Quarter => 25,
    }
}

fn main() {}
```

### match 갈래의 구조

`match` 표현식은 다음과 같은 구조를 가집니다:
- `match` 키워드 다음에 표현식(매칭할 값)
- **Match 갈래**: 각 갈래는 두 부분으로 구성됩니다:
  - **패턴** (예: `Coin::Penny`)
  - 패턴과 코드를 구분하는 `=>` 연산자
  - 패턴이 일치하면 실행할 코드

갈래는 순서대로 평가되고, 첫 번째로 일치하는 패턴이 관련 코드를 실행합니다.

### match 갈래에서 여러 줄

match 갈래에서 여러 줄의 코드를 실행해야 하는 경우 중괄호를 사용합니다:

```rust
enum Coin {
    Penny,
    Nickel,
    Dime,
    Quarter,
}

fn value_in_cents(coin: Coin) -> u8 {
    match coin {
        Coin::Penny => {
            println!("Lucky penny!");
            1
        }
        Coin::Nickel => 5,
        Coin::Dime => 10,
        Coin::Quarter => 25,
    }
}

fn main() {}
```

## 값에 바인딩하는 패턴

match 갈래는 패턴과 일치하는 값의 일부에 바인딩하여 열거형 변형에서 데이터를 추출할 수 있습니다.

### 예제: 주 쿼터

```rust
#[derive(Debug)]
enum UsState {
    Alabama,
    Alaska,
    // --snip--
}

enum Coin {
    Penny,
    Nickel,
    Dime,
    Quarter(UsState),
}

fn value_in_cents(coin: Coin) -> u8 {
    match coin {
        Coin::Penny => 1,
        Coin::Nickel => 5,
        Coin::Dime => 10,
        Coin::Quarter(state) => {
            println!("State quarter from {state:?}!");
            25
        }
    }
}

fn main() {
    value_in_cents(Coin::Quarter(UsState::Alaska));
}
```

`Coin::Quarter(state)`가 일치하면, `state` 변수는 해당 쿼터의 주 값에 바인딩되어 match 갈래 코드에서 사용할 수 있습니다.

## `Option<T>`와 매칭

`match`를 사용하여 `Option<T>` 타입을 처리할 수 있습니다:

```rust
fn main() {
    fn plus_one(x: Option<i32>) -> Option<i32> {
        match x {
            None => None,
            Some(i) => Some(i + 1),
        }
    }

    let five = Some(5);
    let six = plus_one(five);
    let none = plus_one(None);
}
```

패턴 매칭 과정:
1. `plus_one(Some(5))`가 호출되면, `x`는 `Some(5)`입니다
2. `None`과 일치하지 않으므로 다음 갈래로 계속합니다
3. `Some(5)`는 `Some(i)`와 일치하고, `i`는 `5`에 바인딩됩니다
4. 갈래 코드가 실행되어 `Some(6)`을 반환합니다

## match는 철저해야 함

가능한 모든 패턴이 match 갈래로 커버되어야 합니다. Rust는 모든 케이스를 처리하지 않는 코드를 컴파일하지 않습니다:

```rust
fn main() {
    fn plus_one(x: Option<i32>) -> Option<i32> {
        match x {
            Some(i) => Some(i + 1),
            // `None` 케이스 누락 - 컴파일 오류!
        }
    }
}
```

**컴파일러 오류:**
```
error[E0004]: non-exhaustive patterns: `None` not covered
 --> src/main.rs:3:15
  |
3 |         match x {
  |               ^ pattern `None` not covered
```

이는 `None`이 있을 수 있을 때 값이 있다고 실수로 가정하는 것을 방지합니다.

## 포괄 패턴과 `_` 플레이스홀더

### 포괄을 위한 변수 사용

```rust
fn main() {
    let dice_roll = 9;
    match dice_roll {
        3 => add_fancy_hat(),
        7 => remove_fancy_hat(),
        other => move_player(other),
    }

    fn add_fancy_hat() {}
    fn remove_fancy_hat() {}
    fn move_player(num_spaces: u8) {}
}
```

`other` 패턴은 구체적으로 나열되지 않은 모든 값과 일치하고 `other` 변수에 값을 캡처합니다.

### 값이 필요 없을 때 `_` 사용

```rust
fn main() {
    let dice_roll = 9;
    match dice_roll {
        3 => add_fancy_hat(),
        7 => remove_fancy_hat(),
        _ => reroll(),
    }

    fn add_fancy_hat() {}
    fn remove_fancy_hat() {}
    fn reroll() {}
}
```

`_` 패턴은 모든 값과 일치하지만 바인딩하지 않아 미사용 변수 경고를 방지합니다.

### 단위 값과 함께 `_` 사용

```rust
fn main() {
    let dice_roll = 9;
    match dice_roll {
        3 => add_fancy_hat(),
        7 => remove_fancy_hat(),
        _ => (),
    }

    fn add_fancy_hat() {}
    fn remove_fancy_hat() {}
}
```

단위 값 `()`와 함께 `_`를 사용하면 다른 값에 대해 코드가 실행되지 않아야 함을 명시적으로 나타냅니다.

## 핵심 포인트

- **강력한 패턴**: match 표현식은 리터럴 값, 변수, 와일드카드 등을 지원합니다
- **철저함**: Rust는 가능한 모든 케이스가 처리되도록 보장하여 버그를 방지합니다
- **패턴 바인딩**: match 갈래에서 열거형 변형의 값을 추출하고 사용합니다
- **포괄 패턴**: 나머지 케이스를 처리하기 위해 변수 이름이나 `_`를 사용합니다
- **순서가 중요함**: 패턴은 순서대로 평가됩니다. 포괄 패턴은 마지막에 와야 합니다

패턴에 대한 자세한 내용은 Rust 프로그래밍 언어 책의 18장을 참조하세요.

---

# `if let`과 `let...else`를 사용한 간결한 제어 흐름

> **원문:** https://doc.rust-lang.org/book/ch06-03-if-let.html

## 개요

`if let` 문법은 하나의 패턴과 일치하는 값을 처리하고 나머지는 무시하는 덜 장황한 방법을 제공합니다. `if`와 `let`을 패턴 매칭을 위한 간결한 구조로 결합합니다.

## `if let` 문법

### 기본 예제

장황한 `match` 대신:

```rust
fn main() {
    let config_max = Some(3u8);
    match config_max {
        Some(max) => println!("The maximum is configured to be {max}"),
        _ => (),
    }
}
```

간결한 `if let`을 사용합니다:

```rust
fn main() {
    let config_max = Some(3u8);
    if let Some(max) = config_max {
        println!("The maximum is configured to be {max}");
    }
}
```

### 작동 방식

- 등호로 구분된 패턴과 표현식을 받습니다
- 하나의 갈래만 있는 `match`처럼 작동합니다
- 패턴은 블록에서 사용할 수 있는 값을 바인딩합니다
- 값이 패턴과 일치하는 경우에만 코드가 실행됩니다

### 트레이드오프

**장점:**
- 타이핑과 들여쓰기 감소
- 보일러플레이트 코드 감소

**단점:**
- `match`가 강제하는 철저한 검사를 잃음
- 처리되지 않은 케이스를 놓칠 수 있음

## `else`와 함께 `if let`

일치하지 않는 케이스를 처리하기 위해 `if let`과 `else`를 결합합니다:

```rust
#[derive(Debug)]
enum Coin {
    Penny,
    Nickel,
    Dime,
    Quarter(UsState),
}

fn main() {
    let coin = Coin::Penny;
    let mut count = 0;
    if let Coin::Quarter(state) = coin {
        println!("State quarter from {state:?}!");
    } else {
        count += 1;
    }
}
```

`else` 블록은 해당 `match` 표현식의 `_` 케이스와 동일합니다.

## `let...else` - "행복한 경로" 패턴

`let...else` 문법은 값을 추출하거나 조기에 반환하는 일반적인 패턴을 위해 설계되었습니다:

### 문제: 중첩된 `if let`

```rust
fn describe_state_quarter(coin: Coin) -> Option<String> {
    if let Coin::Quarter(state) = coin {
        if state.existed_in(1900) {
            Some(format!("{state:?} is pretty old, for America!"))
        } else {
            Some(format!("{state:?} is relatively new."))
        }
    } else {
        None
    }
}
```

### 해결책: `let...else`

```rust
fn describe_state_quarter(coin: Coin) -> Option<String> {
    let Coin::Quarter(state) = coin else {
        return None;
    };

    if state.existed_in(1900) {
        Some(format!("{state:?} is pretty old, for America!"))
    } else {
        Some(format!("{state:?} is relatively new."))
    }
}
```

### `let...else` 작동 방식

- 왼쪽에 패턴, 오른쪽에 표현식
- **패턴이 일치하면:** 외부 스코프에 값을 바인딩
- **패턴이 일치하지 않으면:** `else` 분기를 실행 (반환하거나 제어 흐름을 중단해야 함)
- 깊이 중첩된 분기 없이 주요 로직을 "행복한 경로"에 유지합니다

## 전체 예제

```rust
#[derive(Debug)]
enum UsState {
    Alabama,
    Alaska,
    // --snip--
}

impl UsState {
    fn existed_in(&self, year: u16) -> bool {
        match self {
            UsState::Alabama => year >= 1819,
            UsState::Alaska => year >= 1959,
            // -- snip --
        }
    }
}

enum Coin {
    Penny,
    Nickel,
    Dime,
    Quarter(UsState),
}

fn describe_state_quarter(coin: Coin) -> Option<String> {
    let Coin::Quarter(state) = coin else {
        return None;
    };

    if state.existed_in(1900) {
        Some(format!("{state:?} is pretty old, for America!"))
    } else {
        Some(format!("{state:?} is relatively new."))
    }
}

fn main() {
    if let Some(desc) = describe_state_quarter(Coin::Quarter(UsState::Alaska)) {
        println!("{desc}");
    }
}
```

## 요약

- **`if let`**: 하나의 패턴에만 관심이 있고 나머지는 무시하고 싶을 때 사용
- **`if let...else`**: 일치하는 케이스와 일치하지 않는 케이스를 모두 처리해야 할 때 사용
- **`let...else`**: 주요 로직을 읽기 쉽게 유지하기 위한 일반적인 "값 추출 또는 조기 반환" 패턴에 사용
- 특정 요구 사항과 철저한 검사를 잃는 것이 허용 가능한지에 따라 `match`와 `if let`/`let...else` 중에서 선택하세요
