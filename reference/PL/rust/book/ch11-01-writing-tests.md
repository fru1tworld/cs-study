# 테스트 작성하기

## 개요

테스트는 비테스트 코드가 예상대로 작동하는지 검증하는 Rust 함수입니다. 테스트 함수는 일반적으로 세 가지 작업을 수행합니다:

- 필요한 데이터 또는 상태 설정
- 테스트하려는 코드 실행
- 결과가 예상과 일치하는지 단언(assert)

## 테스트 함수 구조

Rust에서 테스트는 `#[test]` 속성(attribute)으로 주석 처리된 함수입니다. `cargo test`를 실행하면 Rust는 주석이 달린 함수를 실행하고 통과/실패 결과를 보고하는 테스트 러너 바이너리를 빌드합니다.

### 기본 테스트 구조

```rust
pub fn add(left: u64, right: u64) -> u64 {
    left + right
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn it_works() {
        let result = add(2, 2);
        assert_eq!(result, 4);
    }
}
```

`cargo test`를 실행하면 출력은 다음과 같습니다:

```
running 1 test
test tests::it_works ... ok

test result: ok. 1 passed; 0 failed; 0 ignored; 0 measured
```

**핵심 포인트:**
- `#[test]` 속성으로 함수를 테스트로 표시
- `#[cfg(test)]`는 테스트 중에만 모듈을 컴파일
- `use super::*`로 상위 모듈의 모든 항목을 가져옴

### 테스트 실패

테스트 함수 내에서 패닉이 발생하면 테스트가 실패합니다. 각 테스트는 새 스레드에서 실행됩니다:

```rust
#[test]
fn another() {
    panic!("Make this test fail");
}
```

출력:

```
test tests::another ... FAILED

failures:
---- tests::another stdout ----
thread 'tests::another' panicked at src/lib.rs:17:9:
Make this test fail
```

## 단언 매크로 (Assertion Macros)

### `assert!` 매크로

조건이 `true`로 평가되는지 확인합니다:

```rust
#[derive(Debug)]
struct Rectangle {
    width: u32,
    height: u32,
}

impl Rectangle {
    fn can_hold(&self, other: &Rectangle) -> bool {
        self.width > other.width && self.height > other.height
    }
}

#[test]
fn larger_can_hold_smaller() {
    let larger = Rectangle { width: 8, height: 7 };
    let smaller = Rectangle { width: 5, height: 1 };
    assert!(larger.can_hold(&smaller));
}
```

**핵심 포인트:**
- `assert!`는 불리언 값을 받음
- `true`면 테스트 통과, `false`면 패닉 발생

### `assert_eq!`와 `assert_ne!` 매크로

동등성과 비동등성을 테스트하며, 실패 시 두 값을 출력합니다:

```rust
pub fn add_two(a: u64) -> u64 {
    a + 2
}

#[test]
fn it_adds_two() {
    let result = add_two(2);
    assert_eq!(result, 4);
}
```

단언이 실패하면:

```
assertion `left == right` failed
  left: 5
 right: 4
```

**핵심 포인트:**
- `assert_eq!`는 `==` 연산자 사용
- `assert_ne!`는 `!=` 연산자 사용
- 두 매크로 모두 `PartialEq`와 `Debug` 트레이트 구현 필요
- 구조체와 열거형은 이러한 트레이트를 derive할 수 있음:

```rust
#[derive(PartialEq, Debug)]
struct Guess {
    value: i32,
}
```

## 사용자 정의 실패 메시지

더 나은 디버깅을 위해 `assert!`, `assert_eq!`, `assert_ne!`에 사용자 정의 메시지를 추가할 수 있습니다:

```rust
pub fn greeting(name: &str) -> String {
    format!("Hello {name}!")
}

#[test]
fn greeting_contains_name() {
    let result = greeting("Carol");
    assert!(
        result.contains("Carol"),
        "Greeting did not contain name, value was `{result}`"
    );
}
```

실패 시 출력:

```
thread 'tests::greeting_contains_name' panicked at src/lib.rs:12:9:
Greeting did not contain name, value was `Hello!`
```

**핵심 포인트:**
- 첫 번째 인자 뒤에 추가 인자로 메시지 포맷 문자열 전달
- `format!` 매크로처럼 플레이스홀더 사용 가능

## `#[should_panic]`으로 패닉 확인

코드가 예상 조건에서 패닉을 발생시키는지 테스트합니다:

```rust
pub struct Guess {
    value: i32,
}

impl Guess {
    pub fn new(value: i32) -> Guess {
        if value < 1 || value > 100 {
            panic!("Guess value must be between 1 and 100, got {value}.");
        }
        Guess { value }
    }
}

#[test]
#[should_panic]
fn greater_than_100() {
    Guess::new(200);
}
```

**핵심 포인트:**
- 코드가 패닉하면 테스트 통과
- 코드가 패닉하지 않으면 테스트 실패
- `#[test]` 속성 다음에 `#[should_panic]` 배치

### `#[should_panic(expected = "...")]`

예상 패닉 메시지 하위 문자열을 지정하여 패닉 테스트를 더 정밀하게 만듭니다:

```rust
impl Guess {
    pub fn new(value: i32) -> Guess {
        if value < 1 {
            panic!("Guess value must be greater than or equal to 1, got {value}.");
        } else if value > 100 {
            panic!("Guess value must be less than or equal to 100, got {value}.");
        }
        Guess { value }
    }
}

#[test]
#[should_panic(expected = "less than or equal to 100")]
fn greater_than_100() {
    Guess::new(200);
}
```

불일치 시 출력:

```
panic did not contain expected string
  panic message: "Guess value must be greater than or equal to 1, got 200."
   expected substring: "less than or equal to 100"
```

## 테스트에서 `Result<T, E>` 사용

테스트는 패닉 대신 `Result<T, E>`를 반환할 수 있습니다:

```rust
#[test]
fn it_works() -> Result<(), String> {
    let result = add(2, 2);

    if result == 4 {
        Ok(())
    } else {
        Err(String::from("two plus two does not equal four"))
    }
}
```

**핵심 포인트:**
- `Result<T, E>`를 반환하는 테스트에서 `?` 연산자 사용 가능
- `Result<T, E>` 테스트에서는 `#[should_panic]` 사용 불가
- 에러 변형을 단언하려면 `assert!(value.is_err())` 사용

## 주요 기능 요약

| 기능 | 목적 |
|------|------|
| `#[test]` | 함수를 테스트로 표시 |
| `#[cfg(test)]` | 테스트 중에만 모듈 컴파일 |
| `assert!()` | 불리언 조건 확인 |
| `assert_eq!()` | 동등성 확인 (값 출력) |
| `assert_ne!()` | 비동등성 확인 (값 출력) |
| `#[should_panic]` | 테스트에서 패닉 예상 |
| `Result<T, E>` | 패닉 대신 에러 반환 |
