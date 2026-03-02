# 모듈 트리의 항목을 참조하기 위한 경로

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

기본적으로 모든 항목(함수, 메서드, 구조체, 열거형, 모듈, 상수)은 부모 모듈에 대해 비공개입니다. Listing 7-3을 컴파일하려고 하면 다음을 얻습니다:

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
