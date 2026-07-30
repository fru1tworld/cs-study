# 러스트 패키지·크레이트·모듈과 컬렉션

## 패키지, 크레이트, 모듈

## 패키지, 크레이트, 모듈

> **원문:** https://doc.rust-lang.org/book/ch07-00-managing-growing-projects-with-packages-crates-and-modules.html

### 개요

큰 프로그램을 작성할 때 코드를 체계화하는 것이 점점 더 중요해집니다. 관련 기능을 그룹화하고 별개의 기능을 가진 코드를 분리함으로써, 특정 기능을 구현하는 코드를 찾을 위치와 기능의 작동 방식을 변경할 위치를 명확히 할 수 있습니다.

### 코드 체계화

지금까지 작성한 프로그램은 하나의 파일에 있는 하나의 모듈에 있었습니다. 프로젝트가 성장함에 따라 다음과 같은 방법으로 코드를 체계화해야 합니다:
- 여러 모듈로 분할
- 여러 파일로 분할
- 패키지는 여러 바이너리 크레이트와 선택적으로 하나의 라이브러리 크레이트를 포함할 수 있음
- 부분을 별도의 크레이트로 추출하여 외부 의존성이 되게 함

함께 발전하는 상호 관련된 패키지 세트로 구성된 매우 큰 프로젝트의 경우, Cargo는 **워크스페이스**를 제공합니다 (14장에서 다룸).

### 캡슐화와 프라이버시

작업을 구현하면 다른 코드가 구현이 어떻게 작동하는지 알 필요 없이 공개 인터페이스를 통해 코드를 호출할 수 있습니다. 코드를 작성하는 방식이 다음을 정의합니다:
- **공개(Public)**: 다른 코드가 사용할 수 있음
- **비공개(Private)**: 변경할 권리를 보유한 구현 세부 사항

이는 머릿속에 유지해야 하는 세부 사항의 양을 제한합니다.

### 스코프

코드가 작성된 중첩된 컨텍스트에는 "스코프 내"로 정의된 이름 세트가 있습니다. 코드를 읽고, 쓰고, 컴파일할 때 프로그래머와 컴파일러는 다음을 알아야 합니다:
- 특정 이름이 변수, 함수, 구조체, 열거형, 모듈, 상수 또는 기타 항목을 참조하는지
- 해당 항목이 무엇을 의미하는지

스코프를 만들고 스코프 내외에 있는 이름을 변경할 수 있습니다. 같은 스코프에 같은 이름을 가진 두 항목을 가질 수 없습니다. 이름 충돌을 해결할 수 있는 도구가 있습니다.

### 모듈 시스템

Rust에는 **모듈 시스템**이라고 통칭되는 코드 체계화를 관리할 수 있는 기능이 있으며, 다음을 포함합니다:

- **패키지**: 크레이트를 빌드, 테스트, 공유할 수 있게 해주는 Cargo 기능
- **크레이트**: 라이브러리나 실행 파일을 생성하는 모듈의 트리
- **모듈과 use**: 경로의 체계화, 스코프, 프라이버시를 제어할 수 있게 해줌
- **경로**: 구조체, 함수, 모듈 등의 항목에 이름을 지정하는 방법

이 챕터가 끝나면 모듈 시스템에 대한 확실한 이해를 가지고 스코프를 전문가처럼 다룰 수 있을 것입니다!

---

## 패키지와 크레이트

> **원문:** https://doc.rust-lang.org/book/ch07-01-packages-and-crates.html

### 개요

모듈 시스템의 첫 번째 부분은 패키지와 크레이트입니다.

### 크레이트

**크레이트**는 Rust 컴파일러가 한 번에 고려하는 가장 작은 코드 단위입니다. `cargo` 대신 `rustc`를 실행하고 단일 소스 코드 파일을 전달하더라도 컴파일러는 해당 파일을 크레이트로 간주합니다. 크레이트는 모듈을 포함할 수 있으며, 모듈은 크레이트와 함께 컴파일되는 다른 파일에 정의될 수 있습니다.

크레이트는 두 가지 형태 중 하나로 제공됩니다:

#### 바이너리 크레이트
- 실행할 수 있는 실행 파일로 컴파일할 수 있는 프로그램 (명령줄 프로그램, 서버 등)
- 실행 파일이 실행될 때 발생하는 일을 정의하는 `main` 함수가 있어야 함
- 지금까지 만든 모든 크레이트는 바이너리 크레이트였음

#### 라이브러리 크레이트
- `main` 함수가 없고 실행 파일로 컴파일되지 않음
- 여러 프로젝트와 공유할 기능을 정의
- 예: `rand` 크레이트는 난수를 생성하는 기능을 제공
- Rustacean들이 "크레이트"라고 말할 때, 일반적으로 라이브러리 크레이트를 의미하며 일반적인 프로그래밍 개념인 "라이브러리"와 교환하여 사용

### 크레이트 루트

**크레이트 루트**는 Rust 컴파일러가 시작하는 소스 파일이며 크레이트의 루트 모듈을 구성합니다.

### 패키지

**패키지**는 기능 세트를 제공하는 하나 이상의 크레이트 번들입니다. 패키지에는 해당 크레이트를 빌드하는 방법을 설명하는 **Cargo.toml** 파일이 포함되어 있습니다.

예: Cargo는 다음을 포함하는 패키지입니다:
- 명령줄 도구용 바이너리 크레이트
- 바이너리 크레이트가 의존하는 라이브러리 크레이트
- 다른 프로젝트가 Cargo 라이브러리 크레이트에 의존하여 동일한 로직을 사용할 수 있음

#### 패키지 규칙
- 원하는 만큼 많은 바이너리 크레이트를 포함할 수 있음
- 최대 하나의 라이브러리 크레이트만 포함할 수 있음
- 최소한 하나의 크레이트 (라이브러리 또는 바이너리)를 포함해야 함

### 패키지 생성 예제

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

### Cargo 규칙

Cargo는 크레이트 루트에 대한 규칙을 따릅니다:

- **src/main.rs**는 패키지와 같은 이름을 가진 바이너리 크레이트의 크레이트 루트입니다
- **src/lib.rs**는 패키지가 패키지와 같은 이름을 가진 라이브러리 크레이트를 포함함을 나타내며, **src/lib.rs**가 크레이트 루트입니다
- Cargo는 라이브러리나 바이너리를 빌드하기 위해 크레이트 루트 파일을 `rustc`에 전달합니다

### 패키지 구조 예제

#### 바이너리만
**src/main.rs**만 포함하는 패키지는 `my-project`라는 바이너리 크레이트만 포함합니다

#### 바이너리와 라이브러리
**src/main.rs**와 **src/lib.rs**를 모두 포함하는 패키지는 두 개의 크레이트를 가집니다:
- 바이너리 크레이트
- 라이브러리 크레이트
- 둘 다 패키지와 같은 이름을 가짐

#### 여러 바이너리
패키지는 **src/bin** 디렉토리에 파일을 배치하여 여러 바이너리 크레이트를 가질 수 있습니다. 각 파일은 별도의 바이너리 크레이트가 됩니다.

---

## 모듈로 스코프와 프라이버시 제어하기

> **원문:** https://doc.rust-lang.org/book/ch07-02-defining-modules-to-control-scope-and-privacy.html

### 개요

이 섹션에서는 경로, `use` 키워드, `pub` 키워드를 포함한 Rust의 모듈과 모듈 시스템을 다룹니다.

### 모듈 치트 시트

#### 핵심 규칙

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

### 예제: 바이너리 크레이트 구조

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

#### src/main.rs

```rust
use crate::garden::vegetables::Asparagus;

pub mod garden;

fn main() {
    let plant = Asparagus {};
    println!("I'm growing {plant:?}!");
}
```

#### src/garden.rs

```rust
pub mod vegetables;
```

#### src/garden/vegetables.rs

```rust
#[derive(Debug)]
pub struct Asparagus {}
```

### 모듈로 관련 코드 그룹화

모듈을 사용하면:
- 가독성과 재사용을 위해 코드를 체계화
- 프라이버시를 제어 (항목은 기본적으로 비공개)
- 무엇을 공개적으로 노출할지 선택

#### 예제: 레스토랑 라이브러리

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

#### 모듈 트리 구조

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

### 핵심 개념

- **모듈 트리**: 파일 시스템 디렉토리 트리를 반영합니다. 계층적 조직을 제공합니다
- **크레이트 루트**: `src/main.rs`와 `src/lib.rs`는 `crate`라는 이름의 루트 모듈을 형성합니다
- **중첩**: 모듈은 다른 모듈을 포함할 수 있습니다
- **형제**: 같은 부모 모듈에 정의된 모듈
- **부모/자식**: 포함하는 모듈과 포함된 모듈 간의 관계

---

## 모듈 트리의 항목을 참조하기 위한 경로

> **원문:** https://doc.rust-lang.org/book/ch07-03-paths-for-referring-to-an-item-in-the-module-tree.html

### 개요

모듈 트리에서 항목을 찾을 위치를 Rust에 보여주기 위해, 파일 시스템을 탐색할 때 경로를 사용하는 것과 같은 방식으로 경로를 사용합니다. 함수를 호출하려면 경로를 알아야 합니다.

### 두 가지 형태의 경로

경로는 두 가지 형태를 취할 수 있습니다:

- **절대 경로**: 크레이트 루트에서 시작하는 전체 경로. 외부 크레이트의 코드의 경우 절대 경로는 크레이트 이름으로 시작하고, 현재 크레이트의 코드의 경우 리터럴 `crate`로 시작합니다.
- **상대 경로**: 현재 모듈에서 시작하며 `self`, `super` 또는 현재 모듈의 식별자를 사용합니다.

절대 경로와 상대 경로 모두 이중 콜론(`::`)으로 구분된 하나 이상의 식별자가 따라옵니다.

### 예제: 함수 호출

#### Listing 7-3: 절대 경로와 상대 경로 사용

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

### 프라이버시와 `pub` 키워드

#### 프라이버시 문제

기본적으로 모든 항목(함수, 메서드, 구조체, 열거형, 모듈, 상수)은 부모 모듈에서 비공개입니다. Listing 7-3을 컴파일하려고 하면 다음을 얻습니다:

```
error[E0603]: module `hosting` is private
```

#### 프라이버시 규칙

- 부모 모듈의 항목은 자식 모듈 내의 비공개 항목을 사용할 수 없음
- 자식 모듈의 항목은 조상 모듈의 항목을 사용할 수 있음
- 자식 모듈은 구현 세부 사항을 감싸고 숨김
- 자식 모듈은 정의된 컨텍스트를 볼 수 있음

#### `pub`로 경로 노출

항목을 공개하려면 정의 앞에 `pub` 키워드를 추가합니다.

##### Listing 7-5: `hosting` 모듈을 공개로 만들기

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

##### Listing 7-7: 모듈과 함수 모두 공개로 만들기

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

### 절대 경로와 상대 경로 선택

- **절대 경로**는 코드 정의와 항목 호출을 독립적으로 이동할 가능성이 높기 때문에 일반적으로 선호됩니다
- 항목 정의 코드를 사용하는 코드와 함께 이동할 가능성이 높으면 상대 경로를 사용합니다

### `super`를 사용한 상대 경로

상대 경로의 시작 부분에 `super`를 사용하여 부모 모듈의 항목을 참조합니다. 파일 시스템 경로의 `..`과 유사합니다.

#### Listing 7-8: `super` 사용

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

### 구조체와 열거형을 공개로 만들기

#### 구조체

구조체에 `pub`를 사용하면 구조체는 공개되지만 필드는 기본적으로 비공개로 유지됩니다. 필드는 개별적으로 `pub`로 표시해야 합니다.

##### Listing 7-9: 필드 프라이버시가 혼합된 공개 구조체

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

#### 열거형

열거형에 `pub`를 사용하면 모든 변형이 자동으로 공개됩니다.

##### Listing 7-10: 공개 변형이 있는 공개 열거형

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

### 바이너리 및 라이브러리 크레이트가 있는 패키지의 모범 사례

- `src/lib.rs`에서 모듈 트리를 정의
- 바이너리 크레이트에서 공개 항목에 접근하기 위해 패키지 이름을 사용
- 바이너리 크레이트는 라이브러리 크레이트의 사용자가 됨
- 이는 바이너리 크레이트가 공개 API만 사용하도록 보장하여 더 나은 API 설계에 도움이 됨

---

## `use` 키워드로 경로를 스코프로 가져오기

> **원문:** https://doc.rust-lang.org/book/ch07-04-bringing-paths-into-scope-with-the-use-keyword.html

### 개요

`use` 키워드를 사용하면 경로에 대한 바로가기를 만들어, 함수를 호출하거나 항목에 접근하기 위해 전체 경로를 반복적으로 작성할 필요가 없습니다. 이는 코드를 단순화하고 반복을 줄입니다.

### 기본 사용법

#### 간단한 `use` 문

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

#### 스코프 제한

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

### 관용적인 `use` 경로 만들기

#### 함수 vs 다른 항목

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

### 이름 충돌 처리

#### 부모 모듈 사용

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

#### `as` 키워드 사용

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

### `pub use`로 다시 내보내기

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

### 외부 패키지 사용

#### 의존성 추가

`Cargo.toml`에 패키지를 추가합니다:
```toml
rand = "0.8.5"
```

#### 패키지 항목 사용

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

### 중첩 경로

중첩 경로 문법을 사용하여 같은 경로에서 여러 가져오기를 결합합니다:

```rust
// 대신:
use std::cmp::Ordering;
use std::io;

// 작성:
use std::{cmp::Ordering, io};
```

#### 중첩 경로에서 `self` 사용

한 경로가 다른 경로의 하위 경로인 경우:

```rust
// 대신:
use std::io;
use std::io::Write;

// 작성:
use std::io::{self, Write};
```

### Glob 연산자

`*`를 사용하여 경로의 모든 공개 항목을 가져옵니다:

```rust
use std::collections::*;
```

#### 주의사항:
- 스코프에 어떤 이름이 있는지 파악하기 어려워짐
- 의존성이 변경되면 가져온 항목이 예기치 않게 변경될 수 있음
- 의존성이 코드와 충돌하는 이름을 추가하면 충돌이 발생할 수 있음

#### 사용 사례:
- 테스트 (모든 테스트 항목을 스코프로 가져오기)
- 프렐루드 패턴

### 요약

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

## 모듈을 다른 파일로 분리하기

> **원문:** https://doc.rust-lang.org/book/ch07-05-separating-modules-into-different-files.html

### 개요

모듈이 커지면 코드 구성과 탐색을 개선하기 위해 정의를 별도의 파일로 이동하는 것이 좋습니다. 이 섹션에서는 단일 파일에서 모듈을 자체 파일로 추출하는 방법을 설명합니다.

### 기본 모듈 추출

#### 시작점

하나의 파일(크레이트 루트 파일 `src/lib.rs` 또는 `src/main.rs`와 같은)에 여러 모듈이 정의되어 있으면 별도의 파일로 추출할 수 있습니다.

#### 단계 1: `front_of_house` 모듈 추출

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

### 중요한 개념

> 모듈 트리에서 `mod` 선언을 사용하여 파일을 **한 번만** 로드하면 됩니다. 컴파일러가 파일이 프로젝트의 일부임을 알게 되면, 다른 파일은 선언된 위치에 대한 경로를 사용하여 로드된 파일의 코드를 참조해야 합니다. `mod` 키워드는 다른 프로그래밍 언어의 "include" 연산이 **아닙니다**.

### 자식 모듈 추출

`hosting`(`front_of_house`의 자식)과 같은 자식 모듈의 경우 프로세스가 다릅니다:

#### 단계 1: 부모 모듈 파일 업데이트

**파일: src/front_of_house.rs**
```rust
pub mod hosting;
```

#### 단계 2: 디렉토리 및 자식 모듈 파일 생성

부모 모듈의 이름을 딴 디렉토리를 만들고 그 안에 자식 모듈 파일을 배치합니다:

**파일: src/front_of_house/hosting.rs**
```rust
pub fn add_to_waitlist() {}
```

디렉토리 구조가 모듈 트리 계층 구조와 일치합니다. `hosting.rs`가 `src/`에 직접 배치되면 컴파일러는 이를 `front_of_house`의 자식 대신 루트 모듈로 취급합니다.

### 대체 파일 경로

Rust는 두 가지 파일 경로 스타일을 지원합니다:

#### 루트 모듈 `front_of_house`의 경우:
- **현대적 (관용적)**: `src/front_of_house.rs`
- **이전 스타일 (여전히 지원)**: `src/front_of_house/mod.rs`

#### 서브모듈 `hosting`의 경우:
- **현대적 (관용적)**: `src/front_of_house/hosting.rs`
- **이전 스타일 (여전히 지원)**: `src/front_of_house/hosting/mod.rs`

#### 대체 경로에 대한 참고 사항:
- 같은 모듈에 두 스타일을 모두 사용하면 컴파일러 오류가 발생합니다
- 서로 다른 모듈에서 스타일을 혼합하는 것은 허용되지만 잠재적으로 혼란스러울 수 있습니다
- `mod.rs` 스타일은 같은 이름의 여러 파일을 생성할 수 있어 편집기에서 혼란스러울 수 있습니다

### 핵심 포인트

1. **모듈 트리는 변경되지 않음**: 모듈을 별도의 파일로 추출해도 모듈 계층 구조나 접근 방식은 변경되지 않습니다
2. **함수 호출은 변경 없이 작동**: `eat_at_restaurant()`와 같은 코드는 수정 없이 계속 작동합니다
3. **`pub use` 문은 변경 없음**: `src/lib.rs`의 다시 내보내기 문은 동일하게 유지됩니다
4. **`use`는 컴파일에 영향을 주지 않음**: `use` 키워드는 어떤 파일이 컴파일되는지에 영향을 미치지 않고 가시성에만 영향을 미칩니다
5. **`mod`는 포함이 아닌 선언**: `mod` 키워드는 모듈을 선언합니다. Rust는 모듈 이름의 파일을 자동으로 찾습니다

### 요약

Rust를 사용하면:
- 패키지를 여러 크레이트로 분할
- 크레이트를 모듈로 나누기
- 절대 또는 상대 경로를 사용하여 모듈 간에 항목 참조
- `use` 문으로 경로를 스코프로 가져오기
- `pub` 키워드로 정의를 공개
- 모듈이 커지면 별도의 파일로 이동하여 코드 구성

이 기술은 동일한 모듈 구조와 접근 패턴을 유지하면서 깔끔한 코드 구성을 가능하게 합니다.

---

## 일반적인 컬렉션

## 일반적인 컬렉션

> **원문:** https://doc.rust-lang.org/book/ch08-00-common-collections.html

### 개요

Rust의 표준 라이브러리에는 **컬렉션**이라고 불리는 매우 유용한 데이터 구조가 여러 개 포함되어 있습니다. 대부분의 다른 데이터 타입은 하나의 특정 값을 나타내지만, 컬렉션은 여러 값을 포함할 수 있습니다.

#### 주요 특징

- 내장 배열 및 튜플 타입과 달리, 이러한 컬렉션이 가리키는 데이터는 **힙**에 저장됩니다
- 데이터의 양을 컴파일 시점에 알 필요가 없습니다
- 프로그램이 실행되는 동안 컬렉션이 커지거나 줄어들 수 있습니다
- 각 종류의 컬렉션은 서로 다른 기능과 비용을 가집니다
- 현재 상황에 적합한 것을 선택하는 것은 시간이 지나면서 개발하게 될 기술입니다

### 세 가지 주요 컬렉션

이 챕터에서는 Rust 프로그램에서 매우 자주 사용되는 세 가지 컬렉션을 다룹니다:

1. **벡터** - 여러 값을 나란히 저장할 수 있게 해줍니다

2. **문자열** - 문자들의 컬렉션입니다. `String` 타입은 이전에 언급되었지만, 이 챕터에서 자세히 다룹니다

3. **해시 맵** - 값을 특정 키와 연관시킬 수 있게 해줍니다. **맵**이라고 불리는 더 일반적인 데이터 구조의 특정 구현입니다

### 다루는 주제

이 챕터에서는 다음을 논의합니다:
- 벡터, 문자열, 해시 맵을 생성하고 업데이트하는 방법
- 각 컬렉션을 특별하게 만드는 것

### 추가 자료

표준 라이브러리가 제공하는 다른 종류의 컬렉션에 대한 정보는 [표준 라이브러리 문서](../std/collections/index.html)를 참조하세요.

---

## 벡터로 값 목록 저장하기

> **원문:** https://doc.rust-lang.org/book/ch08-01-vectors.html

### 개요

벡터라고도 불리는 `Vec<T>`는 모든 값이 메모리에 연속적으로 저장되는 단일 데이터 구조에 여러 값을 저장할 수 있는 컬렉션 타입입니다. 벡터는 같은 타입의 값만 저장할 수 있으며 파일의 텍스트 줄이나 장바구니의 가격과 같은 항목 목록에 유용합니다.

### 새 벡터 만들기

#### `Vec::new` 사용

새로운 빈 벡터를 만들려면 `Vec::new` 함수를 호출합니다:

```rust
fn main() {
    let v: Vec<i32> = Vec::new();
}
```

**참고:** Rust가 초기 값 없이는 요소 타입을 추론할 수 없으므로 빈 벡터를 만들 때 타입 주석이 필요합니다.

#### `vec!` 매크로 사용

더 일반적으로, Rust가 타입을 추론할 수 있게 해주는 `vec!` 매크로를 사용하여 초기 값이 있는 벡터를 만듭니다:

```rust
fn main() {
    let v = vec![1, 2, 3];
}
```

이 경우 Rust는 정수 타입(기본 정수 타입)을 기반으로 타입을 `Vec<i32>`로 추론합니다.

### 벡터 업데이트

벡터를 만들고 요소를 추가하려면 `push` 메서드를 사용합니다:

```rust
fn main() {
    let mut v = Vec::new();

    v.push(5);
    v.push(6);
    v.push(7);
    v.push(8);
}
```

**중요:** 벡터를 수정하려면 `mut` 키워드를 사용하여 가변으로 선언해야 합니다. Rust는 push된 값에서 `Vec<i32>` 타입을 추론합니다.

### 벡터 요소 읽기

벡터 요소를 참조하는 두 가지 방법이 있습니다:

#### 1. 인덱싱 문법

```rust
fn main() {
    let v = vec![1, 2, 3, 4, 5];

    let third: &i32 = &v[2];
    println!("The third element is {third}");
}
```

벡터는 0부터 인덱싱됩니다. `&`와 `[]`를 사용하면 요소에 대한 참조를 반환합니다.

#### 2. `get` 메서드 사용

```rust
fn main() {
    let v = vec![1, 2, 3, 4, 5];

    let third: Option<&i32> = v.get(2);
    match third {
        Some(third) => println!("The third element is {third}"),
        None => println!("There is no third element."),
    }
}
```

`get` 메서드는 매치할 수 있는 `Option<&T>`를 반환합니다.

#### 범위 밖 접근 시 동작 차이

벡터 범위 밖의 요소에 접근할 때:

```rust
fn main() {
    let v = vec![1, 2, 3, 4, 5];

    let does_not_exist = &v[100];  // 패닉!
    let does_not_exist = v.get(100);  // None 반환
}
```

- **인덱싱 `[]`**: 인덱스가 범위를 벗어나면 패닉합니다. 잘못된 접근 시 프로그램이 충돌하길 원할 때 사용합니다.
- **`get` 메서드**: 범위를 벗어나면 `None`을 반환합니다. 정상적인 작업 중에 범위 밖 접근이 발생할 수 있을 때 사용합니다.

#### 벡터 참조와 빌림 규칙

같은 스코프에서 불변 참조를 보유하고 벡터를 가변적으로 수정할 수 없습니다:

```rust
fn main() {
    let mut v = vec![1, 2, 3, 4, 5];

    let first = &v[0];

    v.push(6);  // 오류: 가변으로 빌릴 수 없음

    println!("The first element is: {first}");
}
```

**오류:**
```
error[E0502]: cannot borrow `v` as mutable because it is also borrowed as immutable
```

이 오류는 벡터가 요소를 메모리에 연속적으로 저장하기 때문에 발생합니다. 새 요소를 추가하면 메모리를 재할당하고 이전 요소를 복사해야 할 수 있으며, 이는 기존 참조를 무효화합니다.

### 벡터 값 반복

#### 불변 반복

```rust
fn main() {
    let v = vec![100, 32, 57];
    for i in &v {
        println!("{i}");
    }
}
```

#### 가변 반복

```rust
fn main() {
    let mut v = vec![100, 32, 57];
    for i in &mut v {
        *i += 50;
    }
}
```

값에 접근하고 수정하려면 역참조 연산자 `*`를 사용합니다. 빌림 검사기는 반복 루프 내에서 벡터의 동시 수정을 방지합니다.

### 열거형을 사용하여 여러 타입 저장

벡터는 하나의 타입만 저장할 수 있지만, 열거형을 사용하여 감싸면 여러 타입을 저장할 수 있습니다:

```rust
fn main() {
    enum SpreadsheetCell {
        Int(i32),
        Float(f64),
        Text(String),
    }

    let row = vec![
        SpreadsheetCell::Int(3),
        SpreadsheetCell::Text(String::from("blue")),
        SpreadsheetCell::Float(10.12),
    ];
}
```

Rust는 컴파일 시점에 모든 가능한 타입을 알아야 합니다. 런타임에 가능한 타입의 전체 집합을 모른다면 대신 트레이트 객체(18장)를 사용하세요.

### 벡터 드롭

다른 구조체처럼 벡터는 스코프를 벗어나면 해제되고 포함된 모든 요소도 드롭됩니다:

```rust
fn main() {
    {
        let v = vec![1, 2, 3, 4];

        // v로 작업 수행
    } // <- v는 여기서 스코프를 벗어나고 해제됨
}
```

빌림 검사기는 벡터 내용에 대한 참조가 벡터가 유효한 동안에만 사용되도록 보장합니다.

### 추가 메서드

`push` 외에도 표준 라이브러리는 `Vec<T>`에 많은 유용한 메서드를 제공합니다. 예를 들어:
- `pop` - 마지막 요소를 제거하고 반환

사용 가능한 모든 메서드는 [API 문서](../std/vec/struct.Vec.html)를 참조하세요.

---

## UTF-8로 인코딩된 텍스트를 문자열로 저장하기

> **원문:** https://doc.rust-lang.org/book/ch08-02-strings.html

### 개요

Rust는 핵심 언어에서 문자열 슬라이스 `str`이라는 문자열 타입 하나만 있으며, 일반적으로 빌린 형태인 `&str`로 볼 수 있습니다. `String` 타입은 Rust의 표준 라이브러리에서 증가 가능하고, 가변이며, 소유되고, UTF-8로 인코딩된 문자열 타입으로 제공됩니다.

### 문자열 정의

- **`&str`**: 문자열 슬라이스 - 다른 곳에 저장된 UTF-8로 인코딩된 문자열 데이터에 대한 참조 (예: 프로그램 바이너리의 문자열 리터럴)
- **`String`**: 추가 보장, 제한 및 기능과 함께 바이트 벡터(`Vec<u8>`)를 감싸는 래퍼로 구현됨

두 타입 모두 UTF-8로 인코딩되며 Rust의 표준 라이브러리에서 많이 사용됩니다.

### 새 문자열 만들기

#### `String::new()` 사용
```rust
fn main() {
    let mut s = String::new();
}
```

#### `to_string()` 사용
```rust
fn main() {
    let data = "initial contents";
    let s = data.to_string();

    // 리터럴에 직접 작동
    let s = "initial contents".to_string();
}
```

#### `String::from()` 사용
```rust
fn main() {
    let s = String::from("initial contents");
}
```

#### UTF-8 지원
```rust
fn main() {
    let hello = String::from("السلام عليكم");
    let hello = String::from("Dobrý den");
    let hello = String::from("Hello");
    let hello = String::from("שלום");
    let hello = String::from("नमस्ते");
    let hello = String::from("こんにちは");
    let hello = String::from("안녕하세요");
    let hello = String::from("你好");
    let hello = String::from("Olá");
    let hello = String::from("Здравствуйте");
    let hello = String::from("Hola");
}
```

### 문자열 업데이트

#### `push_str()`로 추가
```rust
fn main() {
    let mut s = String::from("foo");
    s.push_str("bar");
}
// 결과: "foobar"
```

`push_str()`은 문자열 슬라이스를 받고 소유권을 가져가지 않아 매개변수를 계속 사용할 수 있습니다:

```rust
fn main() {
    let mut s1 = String::from("foo");
    let s2 = "bar";
    s1.push_str(s2);
    println!("s2 is {s2}"); // s2는 여전히 유효
}
```

#### `push()`로 추가
```rust
fn main() {
    let mut s = String::from("lo");
    s.push('l');
}
// 결과: "lol"
```

#### `+`로 연결
```rust
fn main() {
    let s1 = String::from("Hello, ");
    let s2 = String::from("world!");
    let s3 = s1 + &s2; // s1은 이동됨, 더 이상 사용할 수 없음
}
// 결과: "Hello, world!"
```

**중요**: `+` 연산자는 다음 시그니처의 `add` 메서드를 사용합니다:
```rust
fn add(self, s: &str) -> String
```

- `self`의 소유권을 가져감 (s1이 이동됨)
- 두 번째 문자열의 참조를 받음 (`&s2`)
- 컴파일러가 `&String`을 `&str`로 강제 변환

#### `format!`으로 여러 문자열 연결
```rust
fn main() {
    let s1 = String::from("tic");
    let s2 = String::from("tac");
    let s3 = String::from("toe");

    let s = format!("{s1}-{s2}-{s3}");
}
// 결과: "tic-tac-toe"
```

`format!` 매크로:
- 출력하는 대신 `String`을 반환
- 참조를 사용하므로 매개변수의 소유권을 가져가지 않음
- 여러 `+` 작업보다 더 읽기 쉬움

### 문자열 인덱싱

**Rust는 문자열에 대한 직접 인덱싱을 지원하지 않습니다:**

```rust
fn main() {
    let s1 = String::from("hi");
    let h = s1[0]; // 오류!
}
```

오류:
```
error[E0277]: the type `str` cannot be indexed by `{integer}`
```

#### 인덱싱이 없는 이유

`String`은 `Vec<u8>`을 감싸는 래퍼입니다. 다른 UTF-8 문자는 다른 바이트 수를 차지합니다:

- `"Hola"`: 4바이트 (문자당 1바이트)
- `"Здравствуйте"`: 24바이트 (키릴 문자당 2바이트)

바이트에 대한 인덱스가 항상 유효한 유니코드 스칼라 값과 일치하지 않습니다:

```rust
let hello = "Здравствуйте";
let answer = &hello[0]; // 'З'가 아니라 바이트 208을 반환할 것
```

인덱스 0의 바이트는 `208`이지만, 이는 2바이트 문자의 첫 번째 바이트입니다. `208`만 반환하면 유효한 문자가 아니며 버그를 일으킬 것입니다.

또한, 인덱싱은 상수 시간 O(1)이 예상되지만, Rust는 유효한 문자 경계를 결정하기 위해 문자열의 처음부터 순회해야 합니다.

### 내부 표현: 바이트, 스칼라 값, 자소 클러스터

예: 데바나가리 문자로 된 힌디어 단어 "नमस्ते"

**바이트로** (18바이트):
```
[224, 164, 168, 224, 164, 174, 224, 164, 184, 224, 165, 141, 224, 164, 164, 224, 165, 135]
```

**유니코드 스칼라 값으로** (6개의 `char` 값):
```
['न', 'म', 'स', '्', 'त', 'े']
```
(4번째와 6번째는 분음 부호이며 독립적인 글자가 아님)

**자소 클러스터로** (4개의 글자):
```
["न", "म", "स्", "ते"]
```

Rust는 다른 프로그램이 다른 해석을 필요로 하므로 문자열 데이터를 해석하는 다양한 방법을 제공합니다.

### 문자열 슬라이싱

범위 문법을 사용하여 문자열 슬라이스를 만듭니다:

```rust
fn main() {
    let hello = "Здравствуйте";
    let s = &hello[0..4];
}
// s = "Зд" (처음 4바이트 = 2문자)
```

**주의**: 멀티바이트 문자 중간에서 슬라이싱하면 패닉이 발생합니다:

```rust
let s = &hello[0..1]; // 패닉! 바이트 인덱스 1에서 패닉
```

오류:
```
thread 'main' panicked at src/main.rs:4:19:
byte index 1 is not a char boundary; it is inside 'З' (bytes 0..2) of `Здравствуйте`
```

### 문자열 반복

#### 유니코드 스칼라 값에 `chars()` 사용

```rust
fn main() {
    for c in "Зд".chars() {
        println!("{c}");
    }
}
```

출력:
```
З
д
```

#### 원시 바이트에 `bytes()` 사용

```rust
fn main() {
    for b in "Зд".bytes() {
        println!("{b}");
    }
}
```

출력:
```
208
151
208
180
```

#### 자소 클러스터 가져오기

자소 클러스터 기능은 표준 라이브러리에서 제공되지 않습니다. 필요하다면 [crates.io](https://crates.io/)의 크레이트를 사용하세요.

### 문자열의 복잡성 처리

Rust는 `String` 데이터의 올바른 처리를 기본 동작으로 만들어, 프로그래머가 처음부터 UTF-8 데이터에 대해 생각해야 합니다. 이는 다른 언어보다 더 많은 복잡성을 노출하지만, 개발 후반에 비ASCII 문자와 관련된 버그를 방지합니다.

#### 유용한 표준 라이브러리 메서드

- `contains()` - 문자열에서 검색
- `replace()` - 문자열의 일부를 대체

표준 라이브러리는 이러한 복잡성을 올바르게 처리하기 위해 `String`과 `&str` 타입을 기반으로 구축된 상당한 기능을 제공합니다.

---

## 해시 맵에 연관된 값과 키 저장하기

> **원문:** https://doc.rust-lang.org/book/ch08-03-hash-maps.html

### 개요

`HashMap<K, V>` 타입은 해싱 함수를 사용하여 타입 `K`의 키를 타입 `V`의 값에 매핑하여 저장합니다. 해시 맵은 벡터처럼 인덱스가 아닌 모든 타입의 키로 데이터를 조회하는 데 유용합니다.

많은 프로그래밍 언어가 이 데이터 구조를 다른 이름으로 지원합니다: 해시, 맵, 객체, 해시 테이블, 딕셔너리, 연관 배열 등.

### 새 해시 맵 만들기

`new`를 사용하여 빈 해시 맵을 만들고 `insert`로 요소를 추가할 수 있습니다:

```rust
fn main() {
    use std::collections::HashMap;

    let mut scores = HashMap::new();

    scores.insert(String::from("Blue"), 10);
    scores.insert(String::from("Yellow"), 50);
}
```

**핵심 포인트:**
- `HashMap`은 `std::collections`에서 명시적으로 가져와야 함
- 벡터와 문자열과 달리 프렐루드에 자동으로 포함되지 않음
- 해시 맵은 데이터를 힙에 저장
- 벡터처럼 동종성을 가짐: 모든 키는 같은 타입이어야 하고, 모든 값도 같은 타입이어야 함

### 해시 맵의 값 접근

`get` 메서드를 사용하여 값을 검색합니다:

```rust
fn main() {
    use std::collections::HashMap;

    let mut scores = HashMap::new();

    scores.insert(String::from("Blue"), 10);
    scores.insert(String::from("Yellow"), 50);

    let team_name = String::from("Blue");
    let score = scores.get(&team_name).copied().unwrap_or(0);
}
```

`get` 메서드는 `Option<&V>`를 반환합니다:
- 키가 존재하면 `Some(&value)` 반환
- 키가 존재하지 않으면 `None` 반환

`copied()`를 사용하여 `Option<&i32>`를 `Option<i32>`로 변환하고, `unwrap_or()`를 사용하여 기본값을 제공합니다.

#### 키-값 쌍 반복

```rust
fn main() {
    use std::collections::HashMap;

    let mut scores = HashMap::new();

    scores.insert(String::from("Blue"), 10);
    scores.insert(String::from("Yellow"), 50);

    for (key, value) in &scores {
        println!("{key}: {value}");
    }
}
```

출력 (임의 순서):
```
Yellow: 50
Blue: 10
```

### 해시 맵에서 소유권 관리

#### `Copy` 타입의 경우
`Copy`를 구현하는 값(`i32` 같은)은 해시 맵에 복사됩니다:

```rust
scores.insert(String::from("Blue"), 10);  // 10이 복사됨
```

#### 소유된 타입의 경우
`String` 같은 소유된 값은 해시 맵으로 이동되고, 해시 맵이 소유자가 됩니다:

```rust
fn main() {
    use std::collections::HashMap;

    let field_name = String::from("Favorite color");
    let field_value = String::from("Blue");

    let mut map = HashMap::new();
    map.insert(field_name, field_value);
    // field_name과 field_value는 이제 유효하지 않음
}
```

#### 참조
참조를 삽입하면 값이 이동되지 않습니다. 참조된 값은 해시 맵이 존재하는 동안 유효해야 합니다.

### 해시 맵 업데이트

#### 값 덮어쓰기

```rust
fn main() {
    use std::collections::HashMap;

    let mut scores = HashMap::new();

    scores.insert(String::from("Blue"), 10);
    scores.insert(String::from("Blue"), 25);

    println!("{scores:?}");
}
```

출력: `{"Blue": 25}`

#### 키가 없을 때만 추가

`entry` API와 `or_insert`를 사용합니다:

```rust
fn main() {
    use std::collections::HashMap;

    let mut scores = HashMap::new();
    scores.insert(String::from("Blue"), 10);

    scores.entry(String::from("Yellow")).or_insert(50);
    scores.entry(String::from("Blue")).or_insert(50);

    println!("{scores:?}");
}
```

출력: `{"Yellow": 50, "Blue": 10}`

`or_insert` 메서드:
- 키가 존재하면 값에 대한 가변 참조를 반환
- 키가 존재하지 않으면 매개변수를 삽입하고 가변 참조를 반환

#### 이전 값을 기반으로 업데이트

```rust
fn main() {
    use std::collections::HashMap;

    let text = "hello world wonderful world";

    let mut map = HashMap::new();

    for word in text.split_whitespace() {
        let count = map.entry(word).or_insert(0);
        *count += 1;
    }

    println!("{map:?}");
}
```

출력: `{"world": 2, "hello": 1, "wonderful": 1}`

`or_insert` 메서드는 가변 참조(`&mut V`)를 반환하며, `*`로 역참조하여 값을 수정할 수 있습니다.

### 해싱 함수

기본적으로 `HashMap`은 서비스 거부(DoS) 공격에 대한 저항성을 제공하는 **SipHash**를 사용합니다. 가장 빠른 알고리즘은 아니지만 보안 트레이드오프는 가치가 있습니다.

다른 해싱 알고리즘을 사용하려면:
- `BuildHasher` 트레이트를 구현 (10장에서 논의)
- [crates.io](https://crates.io/)의 서드파티 해셔를 사용

### 연습 문제

1. **중앙값과 최빈값**: 정수 목록이 주어지면, 벡터를 사용하고 중앙값과 최빈값을 반환합니다 (해시 맵이 여기서 도움됨)

2. **피그 라틴**: 문자열을 피그 라틴으로 변환합니다:
   - 첫 번째 자음을 끝으로 이동하고 "ay" 추가
   - 예: "first" -> "irst-fay"
   - 모음으로 시작하는 단어는 "hay" 추가
   - 예: "apple" -> "apple-hay"

3. **직원 관리**: 해시 맵과 벡터를 사용하여 텍스트 인터페이스를 만듭니다:
   - 직원 이름을 부서에 추가
   - 부서별 목록 검색
   - 모든 사람을 알파벳순으로 정렬하여 표시
