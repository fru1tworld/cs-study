# 러스트 패키지·크레이트·모듈과 컬렉션

## 패키지, 크레이트, 모듈

## 패키지, 크레이트, 모듈

> 원문: https://doc.rust-lang.org/book/ch07-00-managing-growing-projects-with-packages-crates-and-modules.html

### 개요

큰 프로그램 작성 시 코드 체계화의 중요성 증가.
- 관련 기능 그룹화 → 특정 기능 구현 코드의 위치·작동 방식 변경 위치 명확화

### 코드 체계화

지금까지 작성한 프로그램은 하나의 파일에 있는 하나의 모듈. 프로젝트 성장에 따른 코드 체계화 방법:
- 여러 모듈로 분할
- 여러 파일로 분할
- 패키지는 여러 바이너리 크레이트와 선택적으로 하나의 라이브러리 크레이트 포함 가능
- 부분을 별도의 크레이트로 추출 → 외부 의존성화

함께 발전하는 상호 관련 패키지 세트로 구성된 매우 큰 프로젝트 → Cargo가 워크스페이스 제공(14장에서 다룸).

### 캡슐화와 프라이버시

작업 구현 → 다른 코드가 구현 작동 방식을 알 필요 없이 공개 인터페이스를 통해 호출 가능. 코드 작성 방식이 정의하는 것:
- 공개(Public): 다른 코드가 사용 가능
- 비공개(Private): 변경할 권리를 보유한 구현 세부 사항

효과: 머릿속에 유지해야 하는 세부 사항의 양 제한.

### 스코프

코드가 작성된 중첩된 컨텍스트 → "스코프 내"로 정의된 이름 세트 존재. 코드를 읽고 쓰고 컴파일할 때 프로그래머와 컴파일러가 알아야 하는 것:
- 특정 이름이 변수·함수·구조체·열거형·모듈·상수 또는 기타 항목을 참조하는지
- 해당 항목이 무엇을 의미하는지

스코프를 만들고 스코프 내외 이름 변경 가능. 같은 스코프에 같은 이름을 가진 두 항목은 불가 → 이름 충돌 해결 도구 존재.

### 모듈 시스템

Rust의 코드 체계화 관리 기능 → 모듈 시스템으로 통칭. 구성 요소:
- 패키지: 크레이트를 빌드·테스트·공유할 수 있게 해주는 Cargo 기능
- 크레이트: 라이브러리나 실행 파일을 생성하는 모듈의 트리
- 모듈과 use: 경로의 체계화·스코프·프라이버시 제어
- 경로: 구조체·함수·모듈 등의 항목에 이름을 지정하는 방법

---

## 패키지와 크레이트

> 원문: https://doc.rust-lang.org/book/ch07-01-packages-and-crates.html

### 개요

모듈 시스템의 첫 번째 부분: 패키지와 크레이트.

### 크레이트

크레이트: Rust 컴파일러가 한 번에 고려하는 가장 작은 코드 단위.
- `cargo` 대신 `rustc`를 실행하고 단일 소스 코드 파일을 전달해도 컴파일러는 해당 파일을 크레이트로 간주
- 크레이트는 모듈을 포함 가능 → 모듈은 크레이트와 함께 컴파일되는 다른 파일에 정의 가능

크레이트의 두 가지 형태:

#### 바이너리 크레이트
- 실행할 수 있는 실행 파일로 컴파일 가능한 프로그램(명령줄 프로그램·서버 등)
- 실행 파일 실행 시 발생하는 일을 정의하는 `main` 함수 필수
- 지금까지 만든 모든 크레이트는 바이너리 크레이트

#### 라이브러리 크레이트
- `main` 함수 없음, 실행 파일로 컴파일되지 않음
- 여러 프로젝트와 공유할 기능 정의
- 예: `rand` 크레이트 → 난수 생성 기능 제공
- Rustacean들이 "크레이트"라고 말할 때 → 일반적으로 라이브러리 크레이트를 의미, 일반적인 프로그래밍 개념인 "라이브러리"와 교환하여 사용

### 크레이트 루트

크레이트 루트: Rust 컴파일러가 시작하는 소스 파일 → 크레이트의 루트 모듈 구성.

### 패키지

패키지: 기능 세트를 제공하는 하나 이상의 크레이트 번들.
- 패키지에 Cargo.toml 파일 포함 → 해당 크레이트를 빌드하는 방법 기술

예: Cargo는 다음을 포함하는 패키지
- 명령줄 도구용 바이너리 크레이트
- 바이너리 크레이트가 의존하는 라이브러리 크레이트
- 다른 프로젝트가 Cargo 라이브러리 크레이트에 의존 → 동일한 로직 사용 가능

#### 패키지 규칙
- 원하는 만큼 많은 바이너리 크레이트 포함 가능
- 최대 하나의 라이브러리 크레이트만 포함 가능
- 최소한 하나의 크레이트(라이브러리 또는 바이너리) 포함 필수

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
- Cargo.toml 파일 생성(패키지를 제공)
- main.rs가 포함된 src 디렉토리 생성

### Cargo 규칙

Cargo는 크레이트 루트에 대한 규칙을 따름:

- src/main.rs는 패키지와 같은 이름을 가진 바이너리 크레이트의 크레이트 루트
- src/lib.rs는 패키지가 패키지와 같은 이름을 가진 라이브러리 크레이트를 포함함을 나타냄 → src/lib.rs가 크레이트 루트
- Cargo는 라이브러리나 바이너리를 빌드하기 위해 크레이트 루트 파일을 `rustc`에 전달

### 패키지 구조 예제

#### 바이너리만
src/main.rs만 포함하는 패키지 → `my-project`라는 바이너리 크레이트만 포함

#### 바이너리와 라이브러리
src/main.rs와 src/lib.rs를 모두 포함하는 패키지 → 두 개의 크레이트 보유:
- 바이너리 크레이트
- 라이브러리 크레이트
- 둘 다 패키지와 같은 이름

#### 여러 바이너리
패키지는 src/bin 디렉토리에 파일을 배치 → 여러 바이너리 크레이트 보유 가능. 각 파일은 별도의 바이너리 크레이트가 됨.

---

## 모듈로 스코프와 프라이버시 제어하기

> 원문: https://doc.rust-lang.org/book/ch07-02-defining-modules-to-control-scope-and-privacy.html

### 개요

이 섹션에서 다루는 내용: 경로·`use` 키워드·`pub` 키워드를 포함한 Rust의 모듈과 모듈 시스템.

### 모듈 치트 시트

#### 핵심 규칙

- 크레이트 루트에서 시작: 컴파일러는 먼저 `src/lib.rs`(라이브러리 크레이트) 또는 `src/main.rs`(바이너리 크레이트)를 찾음

- 모듈 선언: 크레이트 루트에서 `mod garden;`으로 모듈 선언. 컴파일러가 코드를 찾는 위치:
  - 중괄호 내의 인라인
  - `src/garden.rs` 파일
  - `src/garden/mod.rs` 파일

- 서브모듈 선언: 크레이트 루트가 아닌 다른 파일에서 서브모듈 선언. 예를 들어 `src/garden.rs`의 `mod vegetables;`가 찾는 위치:
  - 중괄호 내의 인라인
  - `src/garden/vegetables.rs` 파일
  - `src/garden/vegetables/mod.rs` 파일

- 코드 경로: 프라이버시 규칙이 허용한다고 가정 → `crate::garden::vegetables::Asparagus` 같은 경로를 사용하여 모듈의 코드 참조

- 비공개 vs 공개: 모듈 내의 코드는 기본적으로 비공개. 모듈을 공개하려면 `pub mod` 사용, 항목을 공개하려면 앞에 `pub` 붙임

- `use` 키워드: 반복을 줄이기 위해 바로가기 생성. 예:
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

모듈 사용의 효과:
- 가독성과 재사용을 위해 코드 체계화
- 프라이버시 제어(항목은 기본적으로 비공개)
- 무엇을 공개적으로 노출할지 선택

#### 예제: 레스토랑 라이브러리

`cargo new restaurant --lib`로 라이브러리 생성.

파일명: src/lib.rs

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

- 모듈 트리: 파일 시스템 디렉토리 트리를 반영 → 계층적 조직 제공
- 크레이트 루트: `src/main.rs`와 `src/lib.rs`는 `crate`라는 이름의 루트 모듈을 형성
- 중첩: 모듈은 다른 모듈을 포함 가능
- 형제: 같은 부모 모듈에 정의된 모듈
- 부모/자식: 포함하는 모듈과 포함된 모듈 간의 관계

---

## 모듈 트리의 항목을 참조하기 위한 경로

> 원문: https://doc.rust-lang.org/book/ch07-03-paths-for-referring-to-an-item-in-the-module-tree.html

### 개요

모듈 트리에서 항목을 찾을 위치를 Rust에 알리기 위해 → 파일 시스템 탐색 시 경로를 사용하는 것과 같은 방식으로 경로 사용. 함수 호출을 위해 경로 파악 필요.

### 두 가지 형태의 경로

경로의 두 가지 형태:

- 절대 경로: 크레이트 루트에서 시작하는 전체 경로. 외부 크레이트 코드의 경우 절대 경로는 크레이트 이름으로 시작, 현재 크레이트 코드의 경우 리터럴 `crate`로 시작
- 상대 경로: 현재 모듈에서 시작, `self`·`super` 또는 현재 모듈의 식별자 사용

절대 경로와 상대 경로 모두 이중 콜론(`::`)으로 구분된 하나 이상의 식별자가 따라옴.

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

절대 경로 설명:
- 크레이트 루트에서 시작하기 위해 `crate` 키워드 사용
- 파일 시스템 루트에서 시작하기 위해 `/`를 사용하는 것과 동일

상대 경로 설명:
- 같은 수준에서 정의된 모듈인 `front_of_house`로 시작
- 파일 시스템에서 상대 경로를 사용하는 것과 동일

### 프라이버시와 `pub` 키워드

#### 프라이버시 문제

기본적으로 모든 항목(함수·메서드·구조체·열거형·모듈·상수)은 부모 모듈에서 비공개. Listing 7-3을 컴파일하려고 하면 발생하는 오류:

```
error[E0603]: module `hosting` is private
```

#### 프라이버시 규칙

- 부모 모듈의 항목은 자식 모듈 내의 비공개 항목을 사용할 수 없음
- 자식 모듈의 항목은 조상 모듈의 항목을 사용할 수 있음
- 자식 모듈은 구현 세부 사항을 감싸고 숨김
- 자식 모듈은 정의된 컨텍스트를 볼 수 있음

#### `pub`로 경로 노출

항목을 공개하려면 정의 앞에 `pub` 키워드 추가.

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

참고: 모듈을 공개로 만드는 것 → 모듈 자체에 대한 접근만 허용, 내용에 대한 접근은 허용하지 않음. 모듈 내의 항목도 `pub`로 표시 필요.

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

이 코드는 성공적으로 컴파일됨.

### 절대 경로와 상대 경로 선택

- 절대 경로는 코드 정의와 항목 호출을 독립적으로 이동할 가능성이 높음 → 일반적으로 선호됨
- 항목 정의 코드를 사용하는 코드와 함께 이동할 가능성이 높으면 상대 경로 사용

### `super`를 사용한 상대 경로

상대 경로의 시작 부분에 `super`를 사용 → 부모 모듈의 항목 참조. 파일 시스템 경로의 `..`과 유사.

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

구조체에 `pub` 사용 → 구조체는 공개되지만 필드는 기본적으로 비공개 유지. 필드는 개별적으로 `pub`로 표시 필요.

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

핵심 포인트:
- 공개 필드는 점 표기법을 사용하여 직접 접근 가능
- 비공개 필드는 접근 불가
- 비공개 필드가 있는 구조체는 인스턴스를 구성하기 위한 공개 연관 함수 필요

#### 열거형

열거형에 `pub` 사용 → 모든 변형이 자동으로 공개.

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

- `src/lib.rs`에서 모듈 트리 정의
- 바이너리 크레이트에서 공개 항목에 접근하기 위해 패키지 이름 사용
- 바이너리 크레이트는 라이브러리 크레이트의 사용자가 됨
- 효과: 바이너리 크레이트가 공개 API만 사용하도록 보장 → 더 나은 API 설계에 도움

---

## `use` 키워드로 경로를 스코프로 가져오기

> 원문: https://doc.rust-lang.org/book/ch07-04-bringing-paths-into-scope-with-the-use-keyword.html

### 개요

`use` 키워드 사용 → 경로에 대한 바로가기 생성, 함수 호출이나 항목 접근을 위해 전체 경로를 반복적으로 작성할 필요 없음. 효과: 코드 단순화 및 반복 감소.

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

`use` 문 작성 → 파일 시스템에서 심볼릭 링크를 만드는 것과 유사. 바로가기 `hosting`은 이제 해당 스코프 전체에서 사용 가능.

#### 스코프 제한

중요: `use` 문은 선언된 스코프에만 적용.

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

수정 방법: `use` 문을 `customer` 모듈로 이동 또는 `super::hosting` 참조.

### 관용적인 `use` 경로 만들기

#### 함수 vs 다른 항목

함수의 경우 → 부모 모듈을 스코프로 가져옴:
```rust
use crate::front_of_house::hosting;

pub fn eat_at_restaurant() {
    hosting::add_to_waitlist();
}
```

구조체·열거형 및 기타 항목의 경우 → 전체 경로 지정:
```rust
use std::collections::HashMap;

fn main() {
    let mut map = HashMap::new();
    map.insert(1, 2);
}
```

이 규칙의 효과: 경로 반복을 최소화하면서 항목이 어디에서 왔는지 명확화.

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

충돌하는 이름에 대한 별칭 생성:
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

`pub`과 `use`를 결합 → 가져온 이름을 다른 코드에서 사용 가능하게 함:

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

외부 코드는 이제 전체 경로 `restaurant::front_of_house::hosting::add_to_waitlist()` 대신 `restaurant::hosting::add_to_waitlist()` 사용 가능.

사용 사례: 내부 코드 구조가 사용자가 API에 대해 생각하는 방식과 다를 때.

### 외부 패키지 사용

#### 의존성 추가

`Cargo.toml`에 패키지 추가:
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

참고: 표준 라이브러리(`std`)도 외부 크레이트이며 `use` 문 필요, 다만 `Cargo.toml`에 추가할 필요는 없음.

### 중첩 경로

중첩 경로 문법 사용 → 같은 경로에서 여러 가져오기 결합:

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

`*` 사용 → 경로의 모든 공개 항목 가져옴:

```rust
use std::collections::*;
```

주의사항:
- 스코프에 어떤 이름이 있는지 파악하기 어려워짐
- 의존성이 변경되면 가져온 항목이 예기치 않게 변경될 수 있음
- 의존성이 코드와 충돌하는 이름을 추가하면 충돌 발생 가능

사용 사례:
- 테스트(모든 테스트 항목을 스코프로 가져오기)
- 프렐루드 패턴

### 요약

- 패턴별 예제·사용 사례
  - 기본 모듈 가져오기
    - 예제: `use crate::module::item;`
    - 사용 사례: 표준 가져오기
  - 함수 부모
    - 예제: `use module;`
    - 사용 사례: 함수(관용적)
  - 전체 항목 경로
    - 예제: `use std::collections::HashMap;`
    - 사용 사례: 구조체·열거형·트레이트
  - 별칭
    - 예제: `use std::io::Result as IoResult;`
    - 사용 사례: 이름 충돌
  - 다시 내보내기
    - 예제: `pub use crate::module::item;`
    - 사용 사례: API 단순화
  - 중첩 경로
    - 예제: `use std::{io, fmt};`
    - 사용 사례: 다중 가져오기
  - Glob 연산자
    - 예제: `use std::*;`
    - 사용 사례: 테스트·프렐루드

---

## 모듈을 다른 파일로 분리하기

> 원문: https://doc.rust-lang.org/book/ch07-05-separating-modules-into-different-files.html

### 개요

모듈이 커지면 코드 구성과 탐색 개선을 위해 정의를 별도의 파일로 이동. 이 섹션에서 다루는 내용: 단일 파일에서 모듈을 자체 파일로 추출하는 방법.

### 기본 모듈 추출

#### 시작점

하나의 파일(크레이트 루트 파일 `src/lib.rs` 또는 `src/main.rs`와 같은)에 여러 모듈이 정의되어 있으면 별도의 파일로 추출 가능.

#### 단계 1: `front_of_house` 모듈 추출

파일: src/lib.rs
```rust
mod front_of_house;

pub use crate::front_of_house::hosting;

pub fn eat_at_restaurant() {
    hosting::add_to_waitlist();
}
```

모듈 본문을 `mod` 선언만으로 교체 → 컴파일러는 `src/front_of_house.rs`라는 파일에서 모듈 코드를 찾음.

파일: src/front_of_house.rs
```rust
pub mod hosting {
    pub fn add_to_waitlist() {}
}
```

### 중요한 개념

모듈 트리에서 `mod` 선언을 사용하여 파일을 한 번만 로드하면 됨.
- 컴파일러가 파일이 프로젝트의 일부임을 알게 되면 → 다른 파일은 선언된 위치에 대한 경로를 사용하여 로드된 파일의 코드를 참조해야 함
- `mod` 키워드는 다른 프로그래밍 언어의 "include" 연산이 아님

### 자식 모듈 추출

`hosting`(`front_of_house`의 자식)과 같은 자식 모듈의 경우 프로세스가 다름:

#### 단계 1: 부모 모듈 파일 업데이트

파일: src/front_of_house.rs
```rust
pub mod hosting;
```

#### 단계 2: 디렉토리 및 자식 모듈 파일 생성

부모 모듈의 이름을 딴 디렉토리를 만들고 그 안에 자식 모듈 파일 배치:

파일: src/front_of_house/hosting.rs
```rust
pub fn add_to_waitlist() {}
```

디렉토리 구조가 모듈 트리 계층 구조와 일치. `hosting.rs`가 `src/`에 직접 배치되면 컴파일러는 이를 `front_of_house`의 자식 대신 루트 모듈로 취급.

### 대체 파일 경로

Rust가 지원하는 두 가지 파일 경로 스타일:

#### 루트 모듈 `front_of_house`의 경우
- 현대적(관용적): `src/front_of_house.rs`
- 이전 스타일(여전히 지원): `src/front_of_house/mod.rs`

#### 서브모듈 `hosting`의 경우
- 현대적(관용적): `src/front_of_house/hosting.rs`
- 이전 스타일(여전히 지원): `src/front_of_house/hosting/mod.rs`

#### 대체 경로에 대한 참고 사항
- 같은 모듈에 두 스타일을 모두 사용 → 컴파일러 오류 발생
- 서로 다른 모듈에서 스타일을 혼합하는 것은 허용되지만 잠재적으로 혼란스러움
- `mod.rs` 스타일은 같은 이름의 여러 파일을 생성 → 편집기에서 혼란스러울 수 있음

### 핵심 포인트

- 모듈 트리는 변경되지 않음: 모듈을 별도의 파일로 추출해도 모듈 계층 구조나 접근 방식은 변경되지 않음
- 함수 호출은 변경 없이 작동: `eat_at_restaurant()`와 같은 코드는 수정 없이 계속 작동
- `pub use` 문은 변경 없음: `src/lib.rs`의 다시 내보내기 문은 동일하게 유지
- `use`는 컴파일에 영향을 주지 않음: `use` 키워드는 어떤 파일이 컴파일되는지에 영향을 미치지 않고 가시성에만 영향
- `mod`는 포함이 아닌 선언: `mod` 키워드는 모듈을 선언, Rust는 모듈 이름의 파일을 자동으로 찾음

### 요약

Rust로 가능한 것:
- 패키지를 여러 크레이트로 분할
- 크레이트를 모듈로 나누기
- 절대 또는 상대 경로를 사용하여 모듈 간에 항목 참조
- `use` 문으로 경로를 스코프로 가져오기
- `pub` 키워드로 정의를 공개
- 모듈이 커지면 별도의 파일로 이동하여 코드 구성

효과: 동일한 모듈 구조와 접근 패턴을 유지하면서 깔끔한 코드 구성 가능.

---

## 일반적인 컬렉션

## 일반적인 컬렉션

> 원문: https://doc.rust-lang.org/book/ch08-00-common-collections.html

### 개요

Rust의 표준 라이브러리에 포함된 매우 유용한 데이터 구조: 컬렉션. 대부분의 다른 데이터 타입은 하나의 특정 값을 나타내지만, 컬렉션은 여러 값을 포함 가능.

#### 주요 특징

- 내장 배열 및 튜플 타입과 달리 이러한 컬렉션이 가리키는 데이터는 힙에 저장
- 데이터의 양을 컴파일 시점에 알 필요 없음
- 프로그램이 실행되는 동안 컬렉션이 커지거나 줄어들 수 있음
- 각 종류의 컬렉션은 서로 다른 기능과 비용을 가짐
- 현재 상황에 적합한 것을 선택하는 것은 시간이 지나면서 개발하게 될 기술

### 세 가지 주요 컬렉션

이 챕터에서 다루는 세 가지 컬렉션(Rust 프로그램에서 매우 자주 사용됨):

- 벡터 - 여러 값을 나란히 저장 가능
- 문자열 - 문자들의 컬렉션. `String` 타입은 이전에 언급되었지만 이 챕터에서 자세히 다룸
- 해시 맵 - 값을 특정 키와 연관 가능. 맵이라고 불리는 더 일반적인 데이터 구조의 특정 구현

### 다루는 주제

- 벡터·문자열·해시 맵을 생성하고 업데이트하는 방법
- 각 컬렉션을 특별하게 만드는 것

### 추가 자료

표준 라이브러리가 제공하는 다른 종류의 컬렉션에 대한 정보 → [표준 라이브러리 문서](../std/collections/index.html) 참조.

---

## 벡터로 값 목록 저장하기

> 원문: https://doc.rust-lang.org/book/ch08-01-vectors.html

### 개요

벡터라고도 불리는 `Vec<T>`: 모든 값이 메모리에 연속적으로 저장되는 단일 데이터 구조에 여러 값을 저장 가능한 컬렉션 타입.
- 벡터는 같은 타입의 값만 저장 가능
- 파일의 텍스트 줄이나 장바구니의 가격과 같은 항목 목록에 유용

### 새 벡터 만들기

#### `Vec::new` 사용

새로운 빈 벡터를 만들려면 `Vec::new` 함수 호출:

```rust
fn main() {
    let v: Vec<i32> = Vec::new();
}
```

참고: Rust가 초기 값 없이는 요소 타입을 추론할 수 없음 → 빈 벡터를 만들 때 타입 주석 필요.

#### `vec!` 매크로 사용

더 일반적으로, Rust가 타입을 추론할 수 있게 해주는 `vec!` 매크로를 사용하여 초기 값이 있는 벡터 생성:

```rust
fn main() {
    let v = vec![1, 2, 3];
}
```

이 경우 Rust는 정수 타입(기본 정수 타입)을 기반으로 타입을 `Vec<i32>`로 추론.

### 벡터 업데이트

벡터를 만들고 요소를 추가하려면 `push` 메서드 사용:

```rust
fn main() {
    let mut v = Vec::new();

    v.push(5);
    v.push(6);
    v.push(7);
    v.push(8);
}
```

중요: 벡터를 수정하려면 `mut` 키워드를 사용하여 가변으로 선언 필요. Rust는 push된 값에서 `Vec<i32>` 타입 추론.

### 벡터 요소 읽기

벡터 요소를 참조하는 두 가지 방법:

#### 1. 인덱싱 문법

```rust
fn main() {
    let v = vec![1, 2, 3, 4, 5];

    let third: &i32 = &v[2];
    println!("The third element is {third}");
}
```

벡터는 0부터 인덱싱. `&`와 `[]`를 사용하면 요소에 대한 참조 반환.

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

`get` 메서드는 매치할 수 있는 `Option<&T>` 반환.

#### 범위 밖 접근 시 동작 차이

벡터 범위 밖의 요소에 접근할 때:

```rust
fn main() {
    let v = vec![1, 2, 3, 4, 5];

    let does_not_exist = &v[100];  // 패닉!
    let does_not_exist = v.get(100);  // None 반환
}
```

- 인덱싱 `[]`: 인덱스가 범위를 벗어나면 패닉. 잘못된 접근 시 프로그램이 충돌하길 원할 때 사용
- `get` 메서드: 범위를 벗어나면 `None` 반환. 정상적인 작업 중에 범위 밖 접근이 발생할 수 있을 때 사용

#### 벡터 참조와 빌림 규칙

같은 스코프에서 불변 참조를 보유하고 벡터를 가변적으로 수정 불가:

```rust
fn main() {
    let mut v = vec![1, 2, 3, 4, 5];

    let first = &v[0];

    v.push(6);  // 오류: 가변으로 빌릴 수 없음

    println!("The first element is: {first}");
}
```

오류:
```
error[E0502]: cannot borrow `v` as mutable because it is also borrowed as immutable
```

발생 원인: 벡터가 요소를 메모리에 연속적으로 저장 → 새 요소를 추가하면 메모리를 재할당하고 이전 요소를 복사해야 할 수 있음 → 기존 참조가 무효화됨.

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

값에 접근하고 수정하려면 역참조 연산자 `*` 사용. 빌림 검사기는 반복 루프 내에서 벡터의 동시 수정을 방지.

### 열거형을 사용하여 여러 타입 저장

벡터는 하나의 타입만 저장 가능하지만, 열거형을 사용하여 감싸면 여러 타입 저장 가능:

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

Rust는 컴파일 시점에 모든 가능한 타입을 알아야 함. 런타임에 가능한 타입의 전체 집합을 모른다면 대신 트레이트 객체(18장) 사용.

### 벡터 드롭

다른 구조체처럼 벡터는 스코프를 벗어나면 해제되고 포함된 모든 요소도 드롭됨:

```rust
fn main() {
    {
        let v = vec![1, 2, 3, 4];

        // v로 작업 수행
    } // <- v는 여기서 스코프를 벗어나고 해제됨
}
```

빌림 검사기는 벡터 내용에 대한 참조가 벡터가 유효한 동안에만 사용되도록 보장.

### 추가 메서드

`push` 외에도 표준 라이브러리는 `Vec<T>`에 많은 유용한 메서드 제공. 예:
- `pop` - 마지막 요소를 제거하고 반환

사용 가능한 모든 메서드는 [API 문서](../std/vec/struct.Vec.html) 참조.

---

## UTF-8로 인코딩된 텍스트를 문자열로 저장하기

> 원문: https://doc.rust-lang.org/book/ch08-02-strings.html

### 개요

Rust는 핵심 언어에서 문자열 슬라이스 `str`이라는 문자열 타입 하나만 있음, 일반적으로 빌린 형태인 `&str`로 사용. `String` 타입은 Rust의 표준 라이브러리에서 증가 가능하고 가변이며 소유되고 UTF-8로 인코딩된 문자열 타입으로 제공.

### 문자열 정의

- `&str`: 문자열 슬라이스 - 다른 곳에 저장된 UTF-8로 인코딩된 문자열 데이터에 대한 참조(예: 프로그램 바이너리의 문자열 리터럴)
- `String`: 추가 보장·제한·기능과 함께 바이트 벡터(`Vec<u8>`)를 감싸는 래퍼로 구현됨

두 타입 모두 UTF-8로 인코딩되며 Rust의 표준 라이브러리에서 많이 사용됨.

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

`push_str()`은 문자열 슬라이스를 받고 소유권을 가져가지 않음 → 매개변수를 계속 사용 가능:

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

중요: `+` 연산자는 다음 시그니처의 `add` 메서드 사용:
```rust
fn add(self, s: &str) -> String
```

- `self`의 소유권을 가져감(s1이 이동됨)
- 두 번째 문자열의 참조를 받음(`&s2`)
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

`format!` 매크로의 특징:
- 출력하는 대신 `String` 반환
- 참조를 사용 → 매개변수의 소유권을 가져가지 않음
- 여러 `+` 작업보다 더 읽기 쉬움

### 문자열 인덱싱

Rust는 문자열에 대한 직접 인덱싱을 지원하지 않음:

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

`String`은 `Vec<u8>`을 감싸는 래퍼. 다른 UTF-8 문자는 다른 바이트 수를 차지:

- `"Hola"`: 4바이트(문자당 1바이트)
- `"Здравствуйте"`: 24바이트(키릴 문자당 2바이트)

바이트에 대한 인덱스가 항상 유효한 유니코드 스칼라 값과 일치하지 않음:

```rust
let hello = "Здравствуйте";
let answer = &hello[0]; // 'З'가 아니라 바이트 208을 반환할 것
```

인덱스 0의 바이트는 `208`이지만 이는 2바이트 문자의 첫 번째 바이트 → `208`만 반환하면 유효한 문자가 아니며 버그를 일으킴.

또한 인덱싱은 상수 시간 O(1)이 예상되지만, Rust는 유효한 문자 경계를 결정하기 위해 문자열의 처음부터 순회해야 함.

### 내부 표현: 바이트, 스칼라 값, 자소 클러스터

예: 데바나가리 문자로 된 힌디어 단어 "नमस्ते"

바이트로(18바이트):
```
[224, 164, 168, 224, 164, 174, 224, 164, 184, 224, 165, 141, 224, 164, 164, 224, 165, 135]
```

유니코드 스칼라 값으로(6개의 `char` 값):
```
['न', 'म', 'स', '्', 'त', 'े']
```
(4번째와 6번째는 분음 부호이며 독립적인 글자가 아님)

자소 클러스터로(4개의 글자):
```
["न", "म", "स्", "ते"]
```

Rust는 다른 프로그램이 다른 해석을 필요로 함 → 문자열 데이터를 해석하는 다양한 방법을 제공.

### 문자열 슬라이싱

범위 문법을 사용하여 문자열 슬라이스 생성:

```rust
fn main() {
    let hello = "Здравствуйте";
    let s = &hello[0..4];
}
// s = "Зд" (처음 4바이트 = 2문자)
```

주의: 멀티바이트 문자 중간에서 슬라이싱하면 패닉 발생:

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

자소 클러스터 기능은 표준 라이브러리에서 제공되지 않음. 필요하다면 [crates.io](https://crates.io/)의 크레이트 사용.

### 문자열의 복잡성 처리

Rust는 `String` 데이터의 올바른 처리를 기본 동작으로 만듦 → 프로그래머가 처음부터 UTF-8 데이터에 대해 생각해야 함.
- 다른 언어보다 더 많은 복잡성을 노출
- 개발 후반에 비ASCII 문자와 관련된 버그를 방지

#### 유용한 표준 라이브러리 메서드

- `contains()` - 문자열에서 검색
- `replace()` - 문자열의 일부를 대체

표준 라이브러리는 이러한 복잡성을 올바르게 처리하기 위해 `String`과 `&str` 타입을 기반으로 구축된 상당한 기능 제공.

---

## 해시 맵에 연관된 값과 키 저장하기

> 원문: https://doc.rust-lang.org/book/ch08-03-hash-maps.html

### 개요

`HashMap<K, V>` 타입: 해싱 함수를 사용하여 타입 `K`의 키를 타입 `V`의 값에 매핑하여 저장. 해시 맵은 벡터처럼 인덱스가 아닌 모든 타입의 키로 데이터를 조회하는 데 유용.

많은 프로그래밍 언어가 이 데이터 구조를 다른 이름으로 지원: 해시·맵·객체·해시 테이블·딕셔너리·연관 배열 등.

### 새 해시 맵 만들기

`new`를 사용하여 빈 해시 맵을 만들고 `insert`로 요소 추가 가능:

```rust
fn main() {
    use std::collections::HashMap;

    let mut scores = HashMap::new();

    scores.insert(String::from("Blue"), 10);
    scores.insert(String::from("Yellow"), 50);
}
```

핵심 포인트:
- `HashMap`은 `std::collections`에서 명시적으로 가져와야 함
- 벡터와 문자열과 달리 프렐루드에 자동으로 포함되지 않음
- 해시 맵은 데이터를 힙에 저장
- 벡터처럼 동종성을 가짐: 모든 키는 같은 타입이어야 하고 모든 값도 같은 타입이어야 함

### 해시 맵의 값 접근

`get` 메서드를 사용하여 값 검색:

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

`get` 메서드는 `Option<&V>` 반환:
- 키가 존재하면 `Some(&value)` 반환
- 키가 존재하지 않으면 `None` 반환

`copied()`를 사용하여 `Option<&i32>`를 `Option<i32>`로 변환, `unwrap_or()`를 사용하여 기본값 제공.

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

출력(임의 순서):
```
Yellow: 50
Blue: 10
```

### 해시 맵에서 소유권 관리

#### `Copy` 타입의 경우
`Copy`를 구현하는 값(`i32` 같은)은 해시 맵에 복사됨:

```rust
scores.insert(String::from("Blue"), 10);  // 10이 복사됨
```

#### 소유된 타입의 경우
`String` 같은 소유된 값은 해시 맵으로 이동되고, 해시 맵이 소유자가 됨:

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
참조를 삽입하면 값이 이동되지 않음. 참조된 값은 해시 맵이 존재하는 동안 유효해야 함.

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

`entry` API와 `or_insert` 사용:

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

`or_insert` 메서드의 동작:
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

`or_insert` 메서드는 가변 참조(`&mut V`)를 반환, `*`로 역참조하여 값 수정 가능.

### 해싱 함수

기본적으로 `HashMap`은 서비스 거부(DoS) 공격에 대한 저항성을 제공하는 SipHash 사용.
- 가장 빠른 알고리즘은 아니지만 보안 트레이드오프는 가치가 있음

다른 해싱 알고리즘을 사용하려면:
- `BuildHasher` 트레이트를 구현(10장에서 논의)
- [crates.io](https://crates.io/)의 서드파티 해셔 사용

### 연습 문제

- 중앙값과 최빈값: 정수 목록이 주어지면 벡터를 사용하고 중앙값과 최빈값을 반환(해시 맵이 여기서 도움됨)
- 피그 라틴: 문자열을 피그 라틴으로 변환
  - 첫 번째 자음을 끝으로 이동하고 "ay" 추가
  - 예: "first" -> "irst-fay"
  - 모음으로 시작하는 단어는 "hay" 추가
  - 예: "apple" -> "apple-hay"
- 직원 관리: 해시 맵과 벡터를 사용하여 텍스트 인터페이스 생성
  - 직원 이름을 부서에 추가
  - 부서별 목록 검색
  - 모든 사람을 알파벳순으로 정렬하여 표시
