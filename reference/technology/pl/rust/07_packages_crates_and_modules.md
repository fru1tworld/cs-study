# 패키지, 크레이트, 모듈

# 패키지, 크레이트, 모듈

> **원문:** https://doc.rust-lang.org/book/ch07-00-managing-growing-projects-with-packages-crates-and-modules.html

## 개요

큰 프로그램을 작성할 때 코드를 체계화하는 것이 점점 더 중요해집니다. 관련 기능을 그룹화하고 별개의 기능을 가진 코드를 분리함으로써, 특정 기능을 구현하는 코드를 찾을 위치와 기능의 작동 방식을 변경할 위치를 명확히 할 수 있습니다.

## 코드 체계화

지금까지 작성한 프로그램은 하나의 파일에 있는 하나의 모듈에 있었습니다. 프로젝트가 성장함에 따라 다음과 같은 방법으로 코드를 체계화해야 합니다:
- 여러 모듈로 분할
- 여러 파일로 분할
- 패키지는 여러 바이너리 크레이트와 선택적으로 하나의 라이브러리 크레이트를 포함할 수 있음
- 부분을 별도의 크레이트로 추출하여 외부 의존성이 되게 함

함께 발전하는 상호 관련된 패키지 세트로 구성된 매우 큰 프로젝트의 경우, Cargo는 **워크스페이스**를 제공합니다 (14장에서 다룸).

## 캡슐화와 프라이버시

작업을 구현하면 다른 코드가 구현이 어떻게 작동하는지 알 필요 없이 공개 인터페이스를 통해 코드를 호출할 수 있습니다. 코드를 작성하는 방식이 다음을 정의합니다:
- **공개(Public)**: 다른 코드가 사용할 수 있음
- **비공개(Private)**: 변경할 권리를 보유한 구현 세부 사항

이는 머릿속에 유지해야 하는 세부 사항의 양을 제한합니다.

## 스코프

코드가 작성된 중첩된 컨텍스트에는 "스코프 내"로 정의된 이름 세트가 있습니다. 코드를 읽고, 쓰고, 컴파일할 때 프로그래머와 컴파일러는 다음을 알아야 합니다:
- 특정 이름이 변수, 함수, 구조체, 열거형, 모듈, 상수 또는 기타 항목을 참조하는지
- 해당 항목이 무엇을 의미하는지

스코프를 만들고 스코프 내외에 있는 이름을 변경할 수 있습니다. 같은 스코프에 같은 이름을 가진 두 항목을 가질 수 없습니다. 이름 충돌을 해결할 수 있는 도구가 있습니다.

## 모듈 시스템

Rust에는 **모듈 시스템**이라고 통칭되는 코드 체계화를 관리할 수 있는 기능이 있으며, 다음을 포함합니다:

- **패키지**: 크레이트를 빌드, 테스트, 공유할 수 있게 해주는 Cargo 기능
- **크레이트**: 라이브러리나 실행 파일을 생성하는 모듈의 트리
- **모듈과 use**: 경로의 체계화, 스코프, 프라이버시를 제어할 수 있게 해줌
- **경로**: 구조체, 함수, 모듈 등의 항목에 이름을 지정하는 방법

이 챕터가 끝나면 모듈 시스템에 대한 확실한 이해를 가지고 스코프를 전문가처럼 다룰 수 있을 것입니다!

---

# 패키지와 크레이트

> **원문:** https://doc.rust-lang.org/book/ch07-01-packages-and-crates.html

## 개요

모듈 시스템의 첫 번째 부분은 패키지와 크레이트입니다.

## 크레이트

**크레이트**는 Rust 컴파일러가 한 번에 고려하는 가장 작은 코드 단위입니다. `cargo` 대신 `rustc`를 실행하고 단일 소스 코드 파일을 전달하더라도 컴파일러는 해당 파일을 크레이트로 간주합니다. 크레이트는 모듈을 포함할 수 있으며, 모듈은 크레이트와 함께 컴파일되는 다른 파일에 정의될 수 있습니다.

크레이트는 두 가지 형태 중 하나로 제공됩니다:

### 바이너리 크레이트
- 실행할 수 있는 실행 파일로 컴파일할 수 있는 프로그램 (명령줄 프로그램, 서버 등)
- 실행 파일이 실행될 때 발생하는 일을 정의하는 `main` 함수가 있어야 함
- 지금까지 만든 모든 크레이트는 바이너리 크레이트였음

### 라이브러리 크레이트
- `main` 함수가 없고 실행 파일로 컴파일되지 않음
- 여러 프로젝트와 공유할 기능을 정의
- 예: `rand` 크레이트는 난수를 생성하는 기능을 제공
- Rustacean들이 "크레이트"라고 말할 때, 일반적으로 라이브러리 크레이트를 의미하며 일반적인 프로그래밍 개념인 "라이브러리"와 교환하여 사용

## 크레이트 루트

**크레이트 루트**는 Rust 컴파일러가 시작하는 소스 파일이며 크레이트의 루트 모듈을 구성합니다.

## 패키지

**패키지**는 기능 세트를 제공하는 하나 이상의 크레이트 번들입니다. 패키지에는 해당 크레이트를 빌드하는 방법을 설명하는 **Cargo.toml** 파일이 포함되어 있습니다.

예: Cargo는 다음을 포함하는 패키지입니다:
- 명령줄 도구용 바이너리 크레이트
- 바이너리 크레이트가 의존하는 라이브러리 크레이트
- 다른 프로젝트가 Cargo 라이브러리 크레이트에 의존하여 동일한 로직을 사용할 수 있음

### 패키지 규칙
- 원하는 만큼 많은 바이너리 크레이트를 포함할 수 있음
- 최대 하나의 라이브러리 크레이트만 포함할 수 있음
- 최소한 하나의 크레이트 (라이브러리 또는 바이너리)를 포함해야 함

## 패키지 생성 예제

```bash
$ cargo new my-project
     Created binary (application) `my-project` package
$ ls my-project
Cargo.toml
src
$ ls my-project/src
main.rs
```

`cargo new my-project` 실행 후:
- **Cargo.toml** 파일이 생성됨 (패키지를 제공)
- **main.rs**가 포함된 **src** 디렉토리가 생성됨

## Cargo 규칙

Cargo는 크레이트 루트에 대한 규칙을 따릅니다:

- **src/main.rs**는 패키지와 같은 이름을 가진 바이너리 크레이트의 크레이트 루트입니다
- **src/lib.rs**는 패키지가 패키지와 같은 이름을 가진 라이브러리 크레이트를 포함함을 나타내며, **src/lib.rs**가 크레이트 루트입니다
- Cargo는 라이브러리나 바이너리를 빌드하기 위해 크레이트 루트 파일을 `rustc`에 전달합니다

## 패키지 구조 예제

### 바이너리만
**src/main.rs**만 포함하는 패키지는 `my-project`라는 바이너리 크레이트만 포함합니다

### 바이너리와 라이브러리
**src/main.rs**와 **src/lib.rs**를 모두 포함하는 패키지는 두 개의 크레이트를 가집니다:
- 바이너리 크레이트
- 라이브러리 크레이트
- 둘 다 패키지와 같은 이름을 가짐

### 여러 바이너리
패키지는 **src/bin** 디렉토리에 파일을 배치하여 여러 바이너리 크레이트를 가질 수 있습니다. 각 파일은 별도의 바이너리 크레이트가 됩니다.

---

# 모듈로 스코프와 프라이버시 제어하기

> **원문:** https://doc.rust-lang.org/book/ch07-02-defining-modules-to-control-scope-and-privacy.html

## 개요

이 섹션에서는 경로, `use` 키워드, `pub` 키워드를 포함한 Rust의 모듈과 모듈 시스템을 다룹니다.

## 모듈 치트 시트

### 핵심 규칙

- **크레이트 루트에서 시작**: 컴파일러는 먼저 `src/lib.rs`(라이브러리 크레이트) 또는 `src/main.rs`(바이너리 크레이트)를 찾습니다.

- **모듈 선언**: 크레이트 루트에서 `mod garden;`으로 모듈을 선언합니다. 컴파일러는 다음에서 코드를 찾습니다:
  - 중괄호 내의 인라인
  - `src/garden.rs` 파일
  - `src/garden/mod.rs` 파일

- **서브모듈 선언**: 크레이트 루트가 아닌 다른 파일에서 서브모듈을 선언합니다. 예를 들어, `src/garden.rs`의 `mod vegetables;`는 다음에서 찾습니다:
  - 중괄호 내의 인라인
  - `src/garden/vegetables.rs` 파일
  - `src/garden/vegetables/mod.rs` 파일

- **코드 경로**: 프라이버시 규칙이 허용한다고 가정하면 `crate::garden::vegetables::Asparagus` 같은 경로를 사용하여 모듈의 코드를 참조합니다.

- **비공개 vs 공개**: 모듈 내의 코드는 기본적으로 비공개입니다. 모듈을 공개하려면 `pub mod`를 사용하고, 항목을 공개하려면 앞에 `pub`를 붙입니다.

- **`use` 키워드**: 반복을 줄이기 위해 바로가기를 만듭니다. 예:
  ```rust
  use crate::garden::vegetables::Asparagus;
  ```

## 예제: 바이너리 크레이트 구조

`backyard` 크레이트의 디렉토리 구조:

```
backyard
├── Cargo.lock
├── Cargo.toml
└── src
    ├── garden
    │   └── vegetables.rs
    ├── garden.rs
    └── main.rs
```

### src/main.rs

```rust
use crate::garden::vegetables::Asparagus;

pub mod garden;

fn main() {
    let plant = Asparagus {};
    println!("I'm growing {plant:?}!");
}
```

### src/garden.rs

```rust
pub mod vegetables;
```

### src/garden/vegetables.rs

```rust
#[derive(Debug)]
pub struct Asparagus {}
```

## 모듈로 관련 코드 그룹화

모듈을 사용하면:
- 가독성과 재사용을 위해 코드를 체계화
- 프라이버시를 제어 (항목은 기본적으로 비공개)
- 무엇을 공개적으로 노출할지 선택

### 예제: 레스토랑 라이브러리

`cargo new restaurant --lib`로 라이브러리를 생성합니다.

**파일명: src/lib.rs**

```rust
mod front_of_house {
    mod hosting {
        fn add_to_waitlist() {}

        fn seat_at_table() {}
    }

    mod serving {
        fn take_order() {}

        fn serve_order() {}

        fn take_payment() {}
    }
}
```

### 모듈 트리 구조

```
crate
 └── front_of_house
     ├── hosting
     │   ├── add_to_waitlist
     │   └── seat_at_table
     └── serving
         ├── take_order
         ├── serve_order
         └── take_payment
```

## 핵심 개념

- **모듈 트리**: 파일 시스템 디렉토리 트리를 반영합니다. 계층적 조직을 제공합니다
- **크레이트 루트**: `src/main.rs`와 `src/lib.rs`는 `crate`라는 이름의 루트 모듈을 형성합니다
- **중첩**: 모듈은 다른 모듈을 포함할 수 있습니다
- **형제**: 같은 부모 모듈에 정의된 모듈
- **부모/자식**: 포함하는 모듈과 포함된 모듈 간의 관계

---

# 모듈 트리의 항목을 참조하기 위한 경로

> **원문:** https://doc.rust-lang.org/book/ch07-03-paths-for-referring-to-an-item-in-the-module-tree.html

## 개요

모듈 트리에서 항목을 찾을 위치를 Rust에 보여주기 위해, 파일 시스템을 탐색할 때 경로를 사용하는 것과 같은 방식으로 경로를 사용합니다. 함수를 호출하려면 경로를 알아야 합니다.

## 두 가지 형태의 경로

경로는 두 가지 형태를 취할 수 있습니다:

- **절대 경로**: 크레이트 루트에서 시작하는 전체 경로. 외부 크레이트의 코드의 경우 절대 경로는 크레이트 이름으로 시작하고, 현재 크레이트의 코드의 경우 리터럴 `crate`로 시작합니다.
- **상대 경로**: 현재 모듈에서 시작하며 `self`, `super` 또는 현재 모듈의 식별자를 사용합니다.

절대 경로와 상대 경로 모두 이중 콜론(`::`)으로 구분된 하나 이상의 식별자가 따라옵니다.

## 예제: 함수 호출

### Listing 7-3: 절대 경로와 상대 경로 사용

```rust
mod front_of_house {
    mod hosting {
        fn add_to_waitlist() {}
    }
}

pub fn eat_at_restaurant() {
    // 절대 경로
    crate::front_of_house::hosting::add_to_waitlist();

    // 상대 경로
    front_of_house::hosting::add_to_waitlist();
}
```

**절대 경로 설명:**
- 크레이트 루트에서 시작하기 위해 `crate` 키워드를 사용
- 파일 시스템 루트에서 시작하기 위해 `/`를 사용하는 것과 같음

**상대 경로 설명:**
- 같은 수준에서 정의된 모듈인 `front_of_house`로 시작
- 파일 시스템에서 상대 경로를 사용하는 것과 같음

## 프라이버시와 `pub` 키워드

### 프라이버시 문제

기본적으로 모든 항목(함수, 메서드, 구조체, 열거형, 모듈, 상수)은 부모 모듈에서 비공개입니다. Listing 7-3을 컴파일하려고 하면 다음을 얻습니다:

```
error[E0603]: module `hosting` is private
```

### 프라이버시 규칙

- 부모 모듈의 항목은 자식 모듈 내의 비공개 항목을 사용할 수 없음
- 자식 모듈의 항목은 조상 모듈의 항목을 사용할 수 있음
- 자식 모듈은 구현 세부 사항을 감싸고 숨김
- 자식 모듈은 정의된 컨텍스트를 볼 수 있음

### `pub`로 경로 노출

항목을 공개하려면 정의 앞에 `pub` 키워드를 추가합니다.

#### Listing 7-5: `hosting` 모듈을 공개로 만들기

```rust
mod front_of_house {
    pub mod hosting {
        fn add_to_waitlist() {}
    }
}

pub fn eat_at_restaurant() {
    crate::front_of_house::hosting::add_to_waitlist();
    front_of_house::hosting::add_to_waitlist();
}
```

**참고:** 모듈을 공개로 만드는 것은 모듈 자체에 대한 접근만 허용하고 내용에 대한 접근은 허용하지 않습니다. 모듈 내의 항목도 `pub`로 표시해야 합니다.

#### Listing 7-7: 모듈과 함수 모두 공개로 만들기

```rust
mod front_of_house {
    pub mod hosting {
        pub fn add_to_waitlist() {}
    }
}

pub fn eat_at_restaurant() {
    // 절대 경로
    crate::front_of_house::hosting::add_to_waitlist();

    // 상대 경로
    front_of_house::hosting::add_to_waitlist();
}
```

이 코드는 성공적으로 컴파일됩니다!

## 절대 경로와 상대 경로 선택

- **절대 경로**는 코드 정의와 항목 호출을 독립적으로 이동할 가능성이 높기 때문에 일반적으로 선호됩니다
- 항목 정의 코드를 사용하는 코드와 함께 이동할 가능성이 높으면 상대 경로를 사용합니다

## `super`를 사용한 상대 경로

상대 경로의 시작 부분에 `super`를 사용하여 부모 모듈의 항목을 참조합니다. 파일 시스템 경로의 `..`과 유사합니다.

### Listing 7-8: `super` 사용

```rust
fn deliver_order() {}

mod back_of_house {
    fn fix_incorrect_order() {
        cook_order();
        super::deliver_order();
    }

    fn cook_order() {}
}
```

## 구조체와 열거형을 공개로 만들기

### 구조체

구조체에 `pub`를 사용하면 구조체는 공개되지만 필드는 기본적으로 비공개로 유지됩니다. 필드는 개별적으로 `pub`로 표시해야 합니다.

#### Listing 7-9: 필드 프라이버시가 혼합된 공개 구조체

```rust
mod back_of_house {
    pub struct Breakfast {
        pub toast: String,
        seasonal_fruit: String,
    }

    impl Breakfast {
        pub fn summer(toast: &str) -> Breakfast {
            Breakfast {
                toast: String::from(toast),
                seasonal_fruit: String::from("peaches"),
            }
        }
    }
}

pub fn eat_at_restaurant() {
    let mut meal = back_of_house::Breakfast::summer("Rye");
    meal.toast = String::from("Wheat");
    println!("I'd like {} toast please", meal.toast);

    // 컴파일되지 않음 - seasonal_fruit은 비공개
    // meal.seasonal_fruit = String::from("blueberries");
}
```

**핵심 포인트:**
- 공개 필드는 점 표기법을 사용하여 직접 접근할 수 있음
- 비공개 필드는 접근할 수 없음
- 비공개 필드가 있는 구조체는 인스턴스를 구성하기 위한 공개 연관 함수가 필요함

### 열거형

열거형에 `pub`를 사용하면 모든 변형이 자동으로 공개됩니다.

#### Listing 7-10: 공개 변형이 있는 공개 열거형

```rust
mod back_of_house {
    pub enum Appetizer {
        Soup,
        Salad,
    }
}

pub fn eat_at_restaurant() {
    let order1 = back_of_house::Appetizer::Soup;
    let order2 = back_of_house::Appetizer::Salad;
}
```

## 바이너리 및 라이브러리 크레이트가 있는 패키지의 모범 사례

- `src/lib.rs`에서 모듈 트리를 정의
- 바이너리 크레이트에서 공개 항목에 접근하기 위해 패키지 이름을 사용
- 바이너리 크레이트는 라이브러리 크레이트의 사용자가 됨
- 이는 바이너리 크레이트가 공개 API만 사용하도록 보장하여 더 나은 API 설계에 도움이 됨

---

# `use` 키워드로 경로를 스코프로 가져오기

> **원문:** https://doc.rust-lang.org/book/ch07-04-bringing-paths-into-scope-with-the-use-keyword.html

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

---

# 모듈을 다른 파일로 분리하기

> **원문:** https://doc.rust-lang.org/book/ch07-05-separating-modules-into-different-files.html

## 개요

모듈이 커지면 코드 구성과 탐색을 개선하기 위해 정의를 별도의 파일로 이동하는 것이 좋습니다. 이 섹션에서는 단일 파일에서 모듈을 자체 파일로 추출하는 방법을 설명합니다.

## 기본 모듈 추출

### 시작점

하나의 파일(크레이트 루트 파일 `src/lib.rs` 또는 `src/main.rs`와 같은)에 여러 모듈이 정의되어 있으면 별도의 파일로 추출할 수 있습니다.

### 단계 1: `front_of_house` 모듈 추출

**파일: src/lib.rs**
```rust
mod front_of_house;

pub use crate::front_of_house::hosting;

pub fn eat_at_restaurant() {
    hosting::add_to_waitlist();
}
```

모듈 본문을 `mod` 선언만으로 교체합니다. 컴파일러는 `src/front_of_house.rs`라는 파일에서 모듈 코드를 찾습니다.

**파일: src/front_of_house.rs**
```rust
pub mod hosting {
    pub fn add_to_waitlist() {}
}
```

## 중요한 개념

> 모듈 트리에서 `mod` 선언을 사용하여 파일을 **한 번만** 로드하면 됩니다. 컴파일러가 파일이 프로젝트의 일부임을 알게 되면, 다른 파일은 선언된 위치에 대한 경로를 사용하여 로드된 파일의 코드를 참조해야 합니다. `mod` 키워드는 다른 프로그래밍 언어의 "include" 연산이 **아닙니다**.

## 자식 모듈 추출

`hosting`(`front_of_house`의 자식)과 같은 자식 모듈의 경우 프로세스가 다릅니다:

### 단계 1: 부모 모듈 파일 업데이트

**파일: src/front_of_house.rs**
```rust
pub mod hosting;
```

### 단계 2: 디렉토리 및 자식 모듈 파일 생성

부모 모듈의 이름을 딴 디렉토리를 만들고 그 안에 자식 모듈 파일을 배치합니다:

**파일: src/front_of_house/hosting.rs**
```rust
pub fn add_to_waitlist() {}
```

디렉토리 구조가 모듈 트리 계층 구조와 일치합니다. `hosting.rs`가 `src/`에 직접 배치되면 컴파일러는 이를 `front_of_house`의 자식 대신 루트 모듈로 취급합니다.

## 대체 파일 경로

Rust는 두 가지 파일 경로 스타일을 지원합니다:

### 루트 모듈 `front_of_house`의 경우:
- **현대적 (관용적)**: `src/front_of_house.rs`
- **이전 스타일 (여전히 지원)**: `src/front_of_house/mod.rs`

### 서브모듈 `hosting`의 경우:
- **현대적 (관용적)**: `src/front_of_house/hosting.rs`
- **이전 스타일 (여전히 지원)**: `src/front_of_house/hosting/mod.rs`

### 대체 경로에 대한 참고 사항:
- 같은 모듈에 두 스타일을 모두 사용하면 컴파일러 오류가 발생합니다
- 서로 다른 모듈에서 스타일을 혼합하는 것은 허용되지만 잠재적으로 혼란스러울 수 있습니다
- `mod.rs` 스타일은 같은 이름의 여러 파일을 생성할 수 있어 편집기에서 혼란스러울 수 있습니다

## 핵심 포인트

1. **모듈 트리는 변경되지 않음**: 모듈을 별도의 파일로 추출해도 모듈 계층 구조나 접근 방식은 변경되지 않습니다
2. **함수 호출은 변경 없이 작동**: `eat_at_restaurant()`와 같은 코드는 수정 없이 계속 작동합니다
3. **`pub use` 문은 변경 없음**: `src/lib.rs`의 다시 내보내기 문은 동일하게 유지됩니다
4. **`use`는 컴파일에 영향을 주지 않음**: `use` 키워드는 어떤 파일이 컴파일되는지에 영향을 미치지 않고 가시성에만 영향을 미칩니다
5. **`mod`는 포함이 아닌 선언**: `mod` 키워드는 모듈을 선언합니다. Rust는 모듈 이름의 파일을 자동으로 찾습니다

## 요약

Rust를 사용하면:
- 패키지를 여러 크레이트로 분할
- 크레이트를 모듈로 나누기
- 절대 또는 상대 경로를 사용하여 모듈 간에 항목 참조
- `use` 문으로 경로를 스코프로 가져오기
- `pub` 키워드로 정의를 공개
- 모듈이 커지면 별도의 파일로 이동하여 코드 구성

이 기술은 동일한 모듈 구조와 접근 패턴을 유지하면서 깔끔한 코드 구성을 가능하게 합니다.
