# 추리 게임 프로그래밍

## 개요

이 챕터에서는 완전한 추리 게임 프로그램을 만들면서 기본적인 Rust 개념을 소개합니다. 게임은 1과 100 사이의 난수를 생성하고, 플레이어에게 추측을 입력받아, 각 추측이 너무 작은지, 너무 큰지, 정확한지 피드백을 제공합니다.

## 새 프로젝트 설정

```console
$ cargo new guessing_game
$ cd guessing_game
```

이것은 `Cargo.toml`과 `src/main.rs`를 포함하는 초기 프로젝트 구조를 생성합니다:

**Cargo.toml:**

```toml
[package]
name = "guessing_game"
version = "0.1.0"
edition = "2024"

[dependencies]
```

## 추측 처리하기

첫 번째 단계는 키보드에서 사용자 입력을 받는 것입니다:

**src/main.rs (초기 버전):**

```rust
use std::io;

fn main() {
    println!("Guess the number!");

    println!("Please input your guess.");

    let mut guess = String::new();

    io::stdin()
        .read_line(&mut guess)
        .expect("Failed to read line");

    println!("You guessed: {guess}");
}
```

### 핵심 개념

- **`use std::io;`** - 입출력 라이브러리를 스코프로 가져옴
- **`let mut guess = String::new();`** - 사용자 입력을 저장할 가변 변수 생성
  - Rust에서 변수는 기본적으로 불변
  - `mut` 키워드로 가변으로 만듦
- **`io::stdin().read_line(&mut guess)`** - 표준 입력에서 한 줄을 읽음
  - `&mut guess`는 문자열에 대한 가변 참조를 전달
  - `expect()`는 `Result` 타입을 처리—읽기 실패 시 크래시
- **`{guess}`** - 변수 출력을 위한 `println!` 매크로의 플레이스홀더 문법

## 비밀 번호 생성하기

난수를 생성하기 위해 `rand` 크레이트를 추가합니다.

**Cargo.toml 업데이트:**

```toml
[dependencies]
rand = "0.8.5"
```

**src/main.rs 업데이트:**

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

### Cargo 의존성 관리

- `cargo build` - 의존성 다운로드 및 컴파일
- `cargo run` - 프로그램 컴파일 및 실행
- **Cargo.lock** - 의존성 버전을 잠금으로써 재현 가능한 빌드 보장
- `cargo update` - 의존성을 최신 호환 버전으로 업데이트
- `cargo doc --open` - 의존성에 대한 로컬 문서 열기

## 추측과 비밀 번호 비교하기

문자열 입력을 숫자로 변환하고 비교:

```rust
use std::cmp::Ordering;
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

    let guess: u32 = guess.trim().parse().expect("Please type a number!");

    println!("You guessed: {guess}");

    match guess.cmp(&secret_number) {
        Ordering::Less => println!("Too small!"),
        Ordering::Greater => println!("Too big!"),
        Ordering::Equal => println!("You win!"),
    }
}
```

### 타입 변환

- **섀도잉(Shadowing)** - 타입을 변환할 때 변수 이름 재사용
- **`guess.trim()`** - 공백과 줄바꿈 문자 제거
- **`.parse()`** - 문자열을 다른 타입으로 변환; `Result` 반환
- **타입 어노테이션** - `let guess: u32`는 타입을 명시적으로 지정

### match 표현식

`match` 문은 비교의 다른 결과를 처리합니다:

- **갈래(Arms)** - 각 패턴과 관련 코드
- **패턴** - 매칭할 값들 (`Ordering::Less` 등)
- 첫 번째로 성공한 패턴을 매칭하고 그 코드를 실행

## 반복으로 여러 추측 허용하기

반복적인 추측을 위해 `loop`를 추가:

```rust
use std::cmp::Ordering;
use std::io;
use rand::Rng;

fn main() {
    println!("Guess the number!");

    let secret_number = rand::thread_rng().gen_range(1..=100);

    loop {
        println!("Please input your guess.");

        let mut guess = String::new();

        io::stdin()
            .read_line(&mut guess)
            .expect("Failed to read line");

        let guess: u32 = match guess.trim().parse() {
            Ok(num) => num,
            Err(_) => continue,
        };

        println!("You guessed: {guess}");

        match guess.cmp(&secret_number) {
            Ordering::Less => println!("Too small!"),
            Ordering::Greater => println!("Too big!"),
            Ordering::Equal => {
                println!("You win!");
                break;
            }
        }
    }
}
```

### 반복 제어

- **`loop`** - 무한 반복 생성
- **`break`** - 정확한 숫자를 맞추면 반복 종료
- **`continue`** - 다음 반복으로 건너뜀

### 잘못된 입력 처리

`expect()` 대신 `parse()` 결과와 함께 `match` 사용:

- `Ok(num)` - 성공적으로 파싱됨; 숫자 사용
- `Err(_)` - 파싱 실패; `_`는 오류 세부 사항 무시, `continue`는 다시 반복

## 최종 코드 (완전한 추리 게임)

```rust
use std::cmp::Ordering;
use std::io;
use rand::Rng;

fn main() {
    println!("Guess the number!");

    let secret_number = rand::thread_rng().gen_range(1..=100);

    loop {
        println!("Please input your guess.");

        let mut guess = String::new();

        io::stdin()
            .read_line(&mut guess)
            .expect("Failed to read line");

        let guess: u32 = match guess.trim().parse() {
            Ok(num) => num,
            Err(_) => continue,
        };

        println!("You guessed: {guess}");

        match guess.cmp(&secret_number) {
            Ordering::Less => println!("Too small!"),
            Ordering::Greater => println!("Too big!"),
            Ordering::Equal => {
                println!("You win!");
                break;
            }
        }
    }
}
```

## 요약

이 프로젝트에서 소개한 내용:

- **`let` 문** - 변수 선언
- **`match` 표현식** - 패턴 매칭과 제어 흐름
- **메서드** - 타입에 대한 연산 (`.trim()`, `.parse()`, `.cmp()`)
- **연관 함수** - `String::new()` 및 `rand::thread_rng()`와 같은 타입 수준 함수
- **외부 크레이트** - Cargo를 통해 `rand` 크레이트 사용
- **오류 처리** - `Result`와 `expect()` 또는 `match` 사용
- **타입 변환** - 문자열과 숫자 간 변환
- **반복** - `loop`, `break`, `continue` 사용

이러한 개념들은 Rust 책의 이후 챕터에서 더 자세히 탐구됩니다.
