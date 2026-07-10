# 테스트 작성하기

# 자동화된 테스트 작성하기

> **원문:** https://doc.rust-lang.org/book/ch11-00-testing.html

## 개요

Rust는 프로그램의 정확성(Correctness)에 대해 높은 수준의 관심을 가지고 설계되었지만, 타입 시스템만으로는 모든 것을 잡아낼 수 없습니다. 이를 보완하기 위해 Rust는 자동화된 소프트웨어 테스트 작성을 위한 기능을 기본적으로 제공합니다.

**핵심 포인트:**
- 테스트는 코드가 의도한 대로 동작하는지 검증하는 핵심 도구
- Rust의 타입 시스템은 많은 버그를 방지하지만, 로직의 정확성까지 보장하지는 못함
- 자동화된 테스트를 통해 코드 변경 시 기존의 올바른 동작이 유지되는지 확인 가능

---

## 테스트의 필요성

Edsger W. Dijkstra는 1972년 에세이 "겸손한 프로그래머(The Humble Programmer)"에서 "프로그램 테스트는 버그의 존재를 보여주는 데에는 매우 효과적일 수 있지만, 버그의 부재를 보여주기에는 절망적으로 부적절하다"라고 말했습니다. 그렇다고 해서 가능한 한 많이 테스트하려는 노력을 포기해야 한다는 의미는 아닙니다!

프로그램의 정확성(Correctness)이란 코드가 우리가 의도한 바를 수행하는 정도를 의미합니다. Rust는 프로그램의 정확성에 대해 높은 수준의 관심을 가지고 설계되었지만, 정확성은 복잡하며 증명하기가 쉽지 않습니다. Rust의 타입 시스템(Type System)이 이 부담의 상당 부분을 감당하지만, 타입 시스템이 모든 것을 잡아낼 수는 없습니다. 따라서 Rust는 자동화된 소프트웨어 테스트 작성을 위한 지원을 포함하고 있습니다.

---

## 예시: `add_two` 함수

숫자를 전달받아 2를 더하는 `add_two` 함수를 작성한다고 가정해 봅시다. 이 함수의 시그니처는 정수를 매개변수로 받아 정수를 결과로 반환합니다. 이 함수를 구현하고 컴파일할 때, Rust는 지금까지 배운 모든 타입 검사(Type Checking)와 대여 검사(Borrow Checking)를 수행하여, 예를 들어 이 함수에 `String` 값이나 유효하지 않은 참조를 전달하지 않도록 보장합니다. 하지만 Rust는 이 함수가 정확히 우리가 의도한 대로 동작하는지, 즉 매개변수에 10을 더하거나 50을 빼는 것이 아니라 2를 더하는지는 확인할 수 **없습니다**! 바로 이 지점에서 테스트가 필요합니다.

**핵심 포인트:**
- 타입 시스템은 타입 오류와 대여 규칙 위반은 잡아내지만, 비즈니스 로직의 정확성은 보장하지 못함
- 테스트를 통해 함수가 올바른 값을 반환하는지 검증할 수 있음
- 예: `add_two` 함수에 `3`을 전달했을 때 반환값이 `5`인지 단언(assert)하는 테스트 작성 가능

우리는 예를 들어 `add_two` 함수에 `3`을 전달했을 때 반환값이 `5`인지를 단언(assert)하는 테스트를 작성할 수 있습니다. 코드를 변경할 때마다 이러한 테스트를 실행하여 기존의 올바른 동작이 변경되지 않았는지 확인할 수 있습니다.

---

## 이 장에서 다루는 내용

테스트는 복잡한 기술입니다. 한 장에서 좋은 테스트를 작성하는 방법에 대한 모든 세부 사항을 다룰 수는 없지만, 이 장에서는 Rust 테스트 기능의 메커니즘을 논의합니다. 구체적으로 다음 내용을 다룹니다:

**핵심 포인트:**
- 테스트 작성 시 사용할 수 있는 어노테이션(Annotation)과 매크로(Macro)
- 테스트 실행을 위해 제공되는 기본 동작과 옵션
- 단위 테스트(Unit Test)와 통합 테스트(Integration Test)로 테스트를 구성하는 방법

---

# 테스트 작성하기

> **원문:** https://doc.rust-lang.org/book/ch11-01-writing-tests.html

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

---

# 테스트 실행 방법 제어하기

> **원문:** https://doc.rust-lang.org/book/ch11-02-running-tests.html

## 개요

`cargo test`는 코드를 테스트 모드로 컴파일하고 결과 테스트 바이너리를 실행합니다. 기본적으로 테스트는 **병렬**로 실행되고 **출력을 캡처**하여 결과를 읽기 쉽게 만듭니다. 명령줄 옵션으로 이 동작을 수정할 수 있습니다.

### 명령줄 인수

인수는 두 가지 범주로 구분됩니다:
- **`cargo test`에 대한 인수**: `--` 앞에 나열
- **테스트 바이너리에 대한 인수**: `--` 뒤에 나열

```bash
$ cargo test -- --option
$ cargo test --help              # cargo test 옵션
$ cargo test -- --help           # 테스트 바이너리 옵션
```

**핵심 포인트:**
- `cargo test --help`는 `cargo test`와 함께 사용할 수 있는 옵션을 표시
- `cargo test -- --help`는 구분자 뒤에 사용할 수 있는 옵션을 표시
- `--` 구분자로 두 종류의 인수를 분리

---

## 테스트를 병렬 또는 순차적으로 실행하기

기본적으로 테스트는 **스레드를 사용하여 병렬**로 실행되어 더 빠른 피드백을 제공합니다. 그러나 이를 위해서는 테스트가 독립적이고 상태를 공유하지 않아야 합니다.

### 문제 예시

여러 테스트가 같은 파일을 읽고/쓰면 서로 간섭할 수 있습니다:

```rust
// 각 테스트가 test-output.txt에 쓰기
// 한 테스트가 병렬 실행 중 다른 테스트의 데이터를 덮어쓸 수 있음
```

**핵심 포인트:**
- 테스트가 현재 작업 디렉토리나 환경 변수 같은 공유 환경에 의존하면 문제 발생
- 테스트가 같은 파일에 동시에 쓰면 데이터 충돌 발생
- 테스트끼리 서로 독립적이어야 안전하게 병렬 실행 가능

### 해결책: 단일 스레드 실행

```bash
$ cargo test -- --test-threads=1
```

이렇게 하면 테스트가 순차적으로 실행되어 시간이 더 오래 걸리지만 간섭을 방지합니다.

**핵심 포인트:**
- `--test-threads` 플래그로 사용할 스레드 수 지정
- 값을 `1`로 설정하면 병렬 처리를 사용하지 않음
- 공유 상태가 있는 테스트에서 유용

---

## 함수 출력 표시하기

기본적으로 Rust의 테스트 라이브러리는 통과하는 테스트의 **출력을 캡처**합니다. 실패한 테스트만 출력을 표시합니다.

### 예제 코드 (Listing 11-10)

```rust
fn prints_and_returns_10(a: i32) -> i32 {
    println!("I got the value {a}");
    10
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn this_test_will_pass() {
        let value = prints_and_returns_10(4);
        assert_eq!(value, 10);
    }

    #[test]
    fn this_test_will_fail() {
        let value = prints_and_returns_10(8);
        assert_eq!(value, 5);
    }
}
```

**기본 동작**: 통과하는 테스트의 `println!` 출력은 숨겨집니다.

**핵심 포인트:**
- 테스트가 통과하면 `println!` 출력이 표시되지 않음
- 테스트가 실패하면 표준 출력에 출력된 모든 내용이 표시됨
- 실패 메시지와 함께 실패 원인을 파악하는 데 도움

### 모든 출력 보기

```bash
$ cargo test -- --show-output
```

이 명령은 통과하는 테스트와 실패하는 테스트 모두의 출력을 표시합니다.

**핵심 포인트:**
- `--show-output` 플래그로 성공한 테스트의 출력도 확인 가능
- 디버깅 시 유용함
- 출력은 각 테스트별로 구분되어 표시됨

---

## 이름으로 테스트의 하위 집합 실행하기

### 단일 테스트 실행

```bash
$ cargo test one_hundred
```

`one_hundred`라는 이름의 테스트 함수만 실행합니다.

### 예제 코드 (Listing 11-11)

```rust
pub fn add_two(a: u64) -> u64 {
    a + 2
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn add_two_and_two() {
        let result = add_two(2);
        assert_eq!(result, 4);
    }

    #[test]
    fn add_three_and_two() {
        let result = add_two(3);
        assert_eq!(result, 5);
    }

    #[test]
    fn one_hundred() {
        let result = add_two(100);
        assert_eq!(result, 102);
    }
}
```

**핵심 포인트:**
- 테스트 이름을 `cargo test`에 인수로 전달하여 특정 테스트만 실행
- 이 방법으로는 한 번에 하나의 테스트만 지정 가능
- 나머지 테스트는 "filtered out"으로 표시됨

### 여러 테스트 필터링

```bash
$ cargo test add
```

이름에 "add"가 포함된 모든 테스트를 실행합니다 (예: `add_two_and_two`, `add_three_and_two`). 모듈 이름도 필터와 일치합니다.

**핵심 포인트:**
- 테스트 이름의 일부를 지정하여 해당 값과 일치하는 모든 테스트 실행
- 모듈 이름도 필터에 포함되므로 모듈의 모든 테스트를 실행할 수 있음
- 관련된 테스트 그룹을 함께 실행할 때 유용

---

## 특별히 요청하지 않으면 일부 테스트 무시하기

시간이 많이 걸리는 테스트의 경우 `#[ignore]` 속성을 사용합니다:

```rust
#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn it_works() {
        let result = add(2, 2);
        assert_eq!(result, 4);
    }

    #[test]
    #[ignore]
    fn expensive_test() {
        // 실행하는 데 한 시간이 걸리는 코드
    }
}
```

**핵심 포인트:**
- `#[ignore]` 속성을 추가하면 기본 `cargo test`에서 제외됨
- 시간이 오래 걸리는 테스트에 유용
- 무시된 테스트는 별도 명령으로 실행 가능

### 무시된 테스트만 실행

```bash
$ cargo test -- --ignored
```

### 모든 테스트 실행 (무시된 테스트 포함)

```bash
$ cargo test -- --include-ignored
```

**핵심 포인트:**
- `--ignored` 플래그는 무시된 테스트만 실행
- `--include-ignored` 플래그는 무시 여부와 관계없이 모든 테스트 실행
- CI/CD 환경에서 전체 테스트 실행 시 유용

---

## 주요 명령어 요약

| 명령어 | 용도 |
|--------|------|
| `cargo test` | 모든 테스트를 병렬로 실행 (출력 캡처) |
| `cargo test -- --test-threads=1` | 테스트를 순차적으로 실행 |
| `cargo test -- --show-output` | 통과한 테스트의 출력도 표시 |
| `cargo test <name>` | 특정 이름의 테스트 실행 |
| `cargo test <partial_name>` | 필터와 일치하는 모든 테스트 실행 |
| `cargo test -- --ignored` | 무시된 테스트만 실행 |
| `cargo test -- --include-ignored` | 모든 테스트 실행 (무시된 테스트 포함) |

---

# 테스트 조직화

> **원문:** https://doc.rust-lang.org/book/ch11-03-test-organization.html

## 개요

Rust에서 테스트는 두 가지 주요 카테고리로 구성됩니다:

- **단위 테스트(Unit Tests)**: 작고 집중된 테스트로, 하나의 모듈을 격리하여 테스트합니다. 비공개 인터페이스도 테스트할 수 있습니다.
- **통합 테스트(Integration Tests)**: 라이브러리의 공개 API만 사용하는 외부 테스트로, 여러 모듈을 함께 테스트할 수 있습니다.

## 단위 테스트

### `tests` 모듈과 `#[cfg(test)]`

단위 테스트는 테스트하려는 코드와 함께 `src` 디렉토리에 배치됩니다. 관례적으로 `#[cfg(test)]` 속성이 붙은 `tests` 모듈을 생성합니다.

`#[cfg(test)]` 어노테이션은 Rust에게 `cargo test` 실행 시에만 테스트 코드를 컴파일하고 실행하도록 지시합니다. `cargo build` 시에는 컴파일되지 않습니다.

**핵심 포인트:**
- 컴파일 시간 절약
- 결과물(artifact) 크기 감소
- 프로덕션 코드와 테스트 코드의 명확한 분리

**예제:**

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

### 비공개 함수 테스트

Rust의 프라이버시 규칙은 비공개 함수를 직접 테스트하는 것을 허용합니다. 자식 모듈은 부모 모듈의 항목에 접근할 수 있습니다.

```rust
pub fn add_two(a: u64) -> u64 {
    internal_adder(a, 2)
}

fn internal_adder(left: u64, right: u64) -> u64 {
    left + right
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn internal() {
        let result = internal_adder(2, 2);
        assert_eq!(result, 4);
    }
}
```

**핵심 포인트:**
- `use super::*`를 통해 부모 모듈의 모든 항목(비공개 포함)에 접근
- 비공개 함수 테스트 여부는 프로그래밍 철학에 따라 다름
- Rust는 비공개 함수 테스트를 기술적으로 허용

## 통합 테스트

### `tests` 디렉토리

통합 테스트는 프로젝트 루트 레벨의 `tests` 디렉토리에 배치됩니다(`src` 옆). `tests` 디렉토리의 각 파일은 별도의 크레이트로 컴파일됩니다.

**프로젝트 구조:**

```
adder
├── Cargo.lock
├── Cargo.toml
├── src
│   └── lib.rs
└── tests
    └── integration_test.rs
```

### 통합 테스트 예제

```rust
use adder::add_two;

#[test]
fn it_adds_two() {
    let result = add_two(2);
    assert_eq!(result, 4);
}
```

**단위 테스트와의 주요 차이점:**
- `#[cfg(test)]` 어노테이션이 필요 없음 (Cargo가 `tests` 디렉토리를 특별히 처리)
- 라이브러리의 공개 API를 가져오기 위해 `use` 문 필수
- 비공개 함수에 접근 불가

### 통합 테스트 실행

모든 테스트 실행:
```bash
$ cargo test
```

특정 통합 테스트 파일 실행:
```bash
$ cargo test --test integration_test
```

## 통합 테스트의 서브모듈

### 헬퍼 모듈 문제

`tests` 디렉토리의 각 파일은 별도의 크레이트로 컴파일됩니다. `tests/common.rs` 파일을 만들면 테스트 출력에 `running 0 tests`로 표시되는데, 이는 헬퍼 함수에는 바람직하지 않습니다.

### 해결책: `tests/common/mod.rs` 사용

헬퍼 모듈이 테스트 출력에 나타나지 않도록 하려면 이전 명명 규칙을 사용합니다:

```
tests
├── common
│   └── mod.rs
└── integration_test.rs
```

`tests`의 하위 디렉토리는 별도의 크레이트로 컴파일되지 않습니다.

**`tests/common/mod.rs` 예제:**

```rust
pub fn setup() {
    // 라이브러리 테스트에 필요한 설정 코드
}
```

**`tests/integration_test.rs`에서 사용:**

```rust
use adder::add_two;

mod common;

#[test]
fn it_adds_two() {
    common::setup();

    let result = add_two(2);
    assert_eq!(result, 4);
}
```

**핵심 포인트:**
- `tests/common.rs` 대신 `tests/common/mod.rs` 사용
- 하위 디렉토리는 별도 크레이트로 취급되지 않음
- 여러 통합 테스트 파일에서 공유 설정 코드 재사용 가능

## 바이너리 크레이트의 통합 테스트

바이너리 크레이트(`src/lib.rs` 없이 `src/main.rs`만 포함)는 통합 테스트를 가질 수 없습니다. 통합 테스트는 가져올 라이브러리 크레이트가 필요하기 때문입니다.

**모범 사례:**
- `src/lib.rs`: 테스트 가능한 재사용 로직 포함
- `src/main.rs`: 라이브러리의 로직 호출 (최소한의 코드, 테스트 불필요)

이 구조를 통해 통합 테스트가 중요한 기능을 검증하면서 `main.rs`는 단순하게 유지할 수 있습니다.

## 테스트 출력

`cargo test` 실행 시 세 가지 섹션이 출력됩니다:

1. **단위 테스트** - `src/lib.rs`에서
2. **통합 테스트** - `tests/` 디렉토리에서
3. **문서 테스트** - 문서화 예제에서

어떤 섹션이 실패하면 이후 섹션은 실행되지 않습니다.

## 요약

Rust는 포괄적인 테스트 기능을 제공합니다:

- **단위 테스트**: 개별 컴포넌트와 비공개 세부사항 검증
- **통합 테스트**: 공개 API 동작과 컴포넌트 상호작용 검증
- Rust의 타입 시스템 및 소유권 규칙과 결합하여 로직 버그를 잡고 코드 변경 시 예상 동작 보장

**핵심 포인트:**
- `#[cfg(test)]`로 테스트 코드를 프로덕션에서 제외
- `tests/` 디렉토리로 통합 테스트 분리
- 헬퍼 함수는 `tests/common/mod.rs` 패턴 사용
- 바이너리 크레이트는 로직을 라이브러리로 분리하여 테스트 가능하게 구성
