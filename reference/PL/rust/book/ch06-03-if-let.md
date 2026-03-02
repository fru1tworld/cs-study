# `if let`과 `let...else`를 사용한 간결한 제어 흐름

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
