# 모듈로 스코프와 프라이버시 제어하기

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
