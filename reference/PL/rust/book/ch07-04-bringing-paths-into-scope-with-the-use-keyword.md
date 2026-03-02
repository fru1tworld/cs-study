# `use` 키워드로 경로를 스코프로 가져오기

## 개요

`use` 키워드를 사용하면 경로에 대한 바로가기를 만들어, 함수를 호출하거나 항목에 접근하기 위해 전체 경로를 반복적으로 작성할 필요가 없습니다. 이는 코드를 단순화하고 반복을 줄입니다.

## 기본 사용법

### 간단한 `use` 문

```rust
mod front_of_house {
    pub mod hosting {
        pub fn add_to_waitlist() {}
    }
}

use crate::front_of_house::hosting;

pub fn eat_at_restaurant() {
    hosting::add_to_waitlist();
}
```

`use` 문을 만드는 것은 파일 시스템에서 심볼릭 링크를 만드는 것과 유사합니다. 바로가기 `hosting`은 이제 해당 스코프 전체에서 사용할 수 있습니다.

### 스코프 제한

**중요:** `use` 문은 선언된 스코프에만 적용됩니다.

```rust
mod front_of_house {
    pub mod hosting {
        pub fn add_to_waitlist() {}
    }
}

use crate::front_of_house::hosting;

mod customer {
    pub fn eat_at_restaurant() {
        hosting::add_to_waitlist();  // 오류: hosting이 스코프에 없음
    }
}
```

이를 수정하려면 `use` 문을 `customer` 모듈로 이동하거나 `super::hosting`을 참조합니다.

## 관용적인 `use` 경로 만들기

### 함수 vs 다른 항목

**함수의 경우:** 부모 모듈을 스코프로 가져옵니다:
```rust
use crate::front_of_house::hosting;

pub fn eat_at_restaurant() {
    hosting::add_to_waitlist();
}
```

**구조체, 열거형 및 기타 항목의 경우:** 전체 경로를 지정합니다:
```rust
use std::collections::HashMap;

fn main() {
    let mut map = HashMap::new();
    map.insert(1, 2);
}
```

이 규칙은 경로 반복을 최소화하면서 항목이 어디에서 왔는지 명확하게 합니다.

## 이름 충돌 처리

### 부모 모듈 사용

서로 다른 모듈에서 같은 이름의 항목을 가져올 때:
```rust
use std::fmt;
use std::io;

fn function1() -> fmt::Result {
    Ok(())
}

fn function2() -> io::Result<()> {
    Ok(())
}
```

### `as` 키워드 사용

충돌하는 이름에 대한 별칭을 만듭니다:
```rust
use std::fmt::Result;
use std::io::Result as IoResult;

fn function1() -> Result {
    Ok(())
}

fn function2() -> IoResult<()> {
    Ok(())
}
```

## `pub use`로 다시 내보내기

`pub`과 `use`를 결합하여 가져온 이름을 다른 코드에서 사용할 수 있게 합니다:

```rust
mod front_of_house {
    pub mod hosting {
        pub fn add_to_waitlist() {}
    }
}

pub use crate::front_of_house::hosting;

pub fn eat_at_restaurant() {
    hosting::add_to_waitlist();
}
```

외부 코드는 이제 전체 경로 `restaurant::front_of_house::hosting::add_to_waitlist()` 대신 `restaurant::hosting::add_to_waitlist()`를 사용할 수 있습니다.

**사용 사례:** 내부 코드 구조가 사용자가 API에 대해 생각하는 방식과 다를 때.

## 외부 패키지 사용

### 의존성 추가

`Cargo.toml`에 패키지를 추가합니다:
```toml
rand = "0.8.5"
```

### 패키지 항목 사용

```rust
use std::io;
use rand::Rng;

fn main() {
    println!("Guess the number!");

    let secret_number = rand::thread_rng().gen_range(1..=100);

    println!("The secret number is: {secret_number}");

    println!("Please input your guess.");

    let mut guess = String::new();

    io::stdin()
        .read_line(&mut guess)
        .expect("Failed to read line");

    println!("You guessed: {guess}");
}
```

참고: 표준 라이브러리(`std`)도 외부 크레이트이며 `use` 문이 필요하지만 `Cargo.toml`에 추가할 필요는 없습니다.

## 중첩 경로

중첩 경로 문법을 사용하여 같은 경로에서 여러 가져오기를 결합합니다:

```rust
// 대신:
use std::cmp::Ordering;
use std::io;

// 작성:
use std::{cmp::Ordering, io};
```

### 중첩 경로에서 `self` 사용

한 경로가 다른 경로의 하위 경로인 경우:

```rust
// 대신:
use std::io;
use std::io::Write;

// 작성:
use std::io::{self, Write};
```

## Glob 연산자

`*`를 사용하여 경로의 모든 공개 항목을 가져옵니다:

```rust
use std::collections::*;
```

### 주의사항:
- 스코프에 어떤 이름이 있는지 파악하기 어려워짐
- 의존성이 변경되면 가져온 항목이 예기치 않게 변경될 수 있음
- 의존성이 코드와 충돌하는 이름을 추가하면 충돌이 발생할 수 있음

### 사용 사례:
- 테스트 (모든 테스트 항목을 스코프로 가져오기)
- 프렐루드 패턴

## 요약

| 패턴 | 예제 | 사용 사례 |
|------|------|----------|
| 기본 모듈 가져오기 | `use crate::module::item;` | 표준 가져오기 |
| 함수 부모 | `use module;` | 함수 (관용적) |
| 전체 항목 경로 | `use std::collections::HashMap;` | 구조체, 열거형, 트레이트 |
| 별칭 | `use std::io::Result as IoResult;` | 이름 충돌 |
| 다시 내보내기 | `pub use crate::module::item;` | API 단순화 |
| 중첩 경로 | `use std::{io, fmt};` | 다중 가져오기 |
| Glob 연산자 | `use std::*;` | 테스트, 프렐루드 |
