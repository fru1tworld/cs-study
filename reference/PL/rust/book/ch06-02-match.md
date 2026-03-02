# `match` 제어 흐름 구조

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
