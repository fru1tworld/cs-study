# 오류 처리

# 오류 처리 (Error Handling)

> **원문:** https://doc.rust-lang.org/book/ch09-00-error-handling.html

## 개요

오류는 소프트웨어에서 피할 수 없는 현실입니다. Rust는 무언가 잘못될 수 있는 상황을 처리하기 위한 다양한 기능을 제공합니다. 많은 경우 Rust는 오류 가능성을 인지하고 코드가 컴파일되기 전에 조치를 취하도록 요구합니다. 이 요구사항은 프로덕션에 배포하기 전에 오류를 발견하고 적절하게 처리하도록 보장하여 프로그램을 더욱 견고하게 만듭니다.

## 오류의 분류

Rust는 오류를 두 가지 주요 범주로 분류합니다:

### 복구 가능한 오류 (Recoverable Errors)
- 예시: 파일을 찾을 수 없는 오류
- 조치: 사용자에게 문제를 보고하고 작업을 재시도
- 결과: 프로그램 실행을 계속할 수 있음

### 복구 불가능한 오류 (Unrecoverable Errors)
- 예시: 배열 끝을 넘어서 접근 시도
- 조치: 프로그램을 즉시 중단
- 항상 코드의 버그 증상임

## Rust의 오류 처리 방식

대부분의 언어는 이 두 종류의 오류를 구분하지 않고 예외(exception)와 같은 메커니즘으로 동일하게 처리합니다. **Rust는 예외가 없습니다.** 대신 다음을 제공합니다:

1. **`Result<T, E>`** - 복구 가능한 오류를 처리하기 위한 타입
2. **`panic!` 매크로** - 복구 불가능한 오류를 만났을 때 실행을 중단

---

# `panic!`을 이용한 복구 불가능한 오류

## 개요

때때로 코드에서 나쁜 일이 발생하고, 이에 대해 할 수 있는 것이 없습니다. 이런 경우 Rust는 `panic!` 매크로를 제공합니다. 실제로 패닉을 유발하는 두 가지 방법이 있습니다:

1. 코드가 패닉을 유발하는 동작을 수행 (예: 배열 끝을 넘어서 접근)
2. 명시적으로 `panic!` 매크로 호출

기본적으로 이러한 패닉은 실패 메시지를 출력하고, 스택을 되감고(unwind), 정리한 후 종료합니다. 환경 변수를 통해 패닉 발생 시 호출 스택을 표시하도록 설정할 수 있습니다.

## 스택 되감기 vs 중단 (Unwinding vs Aborting)

패닉 발생 시 기본 동작은 **되감기**(unwinding)입니다. Rust가 스택을 거슬러 올라가며 각 함수의 데이터를 정리합니다. 하지만 이 과정은 많은 작업을 필요로 합니다.

대안으로 **중단**(aborting)을 선택할 수 있습니다. 이는 정리 없이 프로그램을 즉시 종료합니다. 운영체제가 메모리를 정리해야 합니다.

바이너리 크기를 최소화해야 한다면 `Cargo.toml`에서 중단 모드로 전환할 수 있습니다:

```toml
[profile.release]
panic = 'abort'
```

## 예제 1: 명시적 `panic!` 호출

```rust
fn main() {
    panic!("crash and burn");
}
```

출력:
```
thread 'main' panicked at src/main.rs:2:5:
crash and burn
note: run with `RUST_BACKTRACE=1` environment variable to display a backtrace
```

**핵심 포인트:**
- `panic!` 호출이 오류 메시지를 생성
- 첫 번째 줄은 패닉 메시지와 소스 코드 위치를 표시
- `src/main.rs:2:5`는 두 번째 줄, 다섯 번째 문자를 의미

## 예제 2: 배열 범위를 벗어난 접근

```rust
fn main() {
    let v = vec![1, 2, 3];

    v[99];
}
```

벡터에 세 개의 요소만 있는데 100번째 요소(인덱스 99)에 접근하려고 합니다. Rust는 C와 같은 언어에서 발생하는 **버퍼 오버리드(buffer overread)** 취약점을 방지하기 위해 패닉합니다.

출력:
```
thread 'main' panicked at src/main.rs:4:6:
index out of bounds: the len is 3 but the index is 99
note: run with `RUST_BACKTRACE=1` environment variable to display a backtrace
```

## 백트레이스 사용하기

`RUST_BACKTRACE` 환경 변수를 설정하여 백트레이스를 얻을 수 있습니다:

```
$ RUST_BACKTRACE=1 cargo run
thread 'main' panicked at src/main.rs:4:6:
index out of bounds: the len is 3 but the index is 99
stack backtrace:
   0: rust_begin_unwind
   ...
   6: panic::main
             at ./src/main.rs:4:6
   ...
```

**핵심 포인트:**
- **백트레이스**(backtrace)는 이 지점까지 호출된 모든 함수의 목록
- 백트레이스를 읽는 핵심: 위에서부터 시작하여 작성한 파일이 보일 때까지 읽기
- 디버그 심볼은 `--release` 플래그 없이 빌드할 때 기본적으로 활성화됨

---

# `Result`를 이용한 복구 가능한 오류

## 개요

대부분의 오류는 프로그램을 완전히 중단해야 할 만큼 심각하지 않습니다. 예를 들어, 파일을 열려고 했는데 파일이 존재하지 않아 실패하면, 프로세스를 종료하는 대신 파일을 생성하고 싶을 수 있습니다.

## Result 열거형

`Result` 열거형은 두 개의 변형(variant)을 가집니다:

```rust
enum Result<T, E> {
    Ok(T),
    Err(E),
}
```

- `T`: 성공 시 `Ok` 변형 내에서 반환되는 값의 타입
- `E`: 실패 시 `Err` 변형 내에서 반환되는 오류의 타입

## 파일 열기

```rust
use std::fs::File;

fn main() {
    let greeting_file_result = File::open("hello.txt");
}
```

`File::open`의 반환 타입은 `Result<T, E>`입니다:
- `T`: `std::fs::File` (성공 시 파일 핸들)
- `E`: `std::io::Error` (실패 시 오류 타입)

## `match`로 Result 처리하기

```rust
use std::fs::File;

fn main() {
    let greeting_file_result = File::open("hello.txt");

    let greeting_file = match greeting_file_result {
        Ok(file) => file,
        Err(error) => panic!("Problem opening the file: {error:?}"),
    };
}
```

## 다양한 오류에 대한 매칭

중첩된 `match` 표현식을 사용하여 서로 다른 실패 이유를 다르게 처리할 수 있습니다:

```rust
use std::fs::File;
use std::io::ErrorKind;

fn main() {
    let greeting_file_result = File::open("hello.txt");

    let greeting_file = match greeting_file_result {
        Ok(file) => file,
        Err(error) => match error.kind() {
            ErrorKind::NotFound => match File::create("hello.txt") {
                Ok(fc) => fc,
                Err(e) => panic!("Problem creating the file: {e:?}"),
            },
            _ => {
                panic!("Problem opening the file: {error:?}");
            }
        },
    };
}
```

### 클로저를 사용한 대안

`unwrap_or_else`와 클로저를 사용:

```rust
use std::fs::File;
use std::io::ErrorKind;

fn main() {
    let greeting_file = File::open("hello.txt").unwrap_or_else(|error| {
        if error.kind() == ErrorKind::NotFound {
            File::create("hello.txt").unwrap_or_else(|error| {
                panic!("Problem creating the file: {error:?}");
            })
        } else {
            panic!("Problem opening the file: {error:?}");
        }
    });
}
```

## 오류 시 패닉을 위한 단축 메서드

### `unwrap` 메서드

`Ok`인 경우 내부 값을 반환하고, `Err`인 경우 `panic!`을 호출합니다:

```rust
use std::fs::File;

fn main() {
    let greeting_file = File::open("hello.txt").unwrap();
}
```

### `expect` 메서드

`unwrap`과 유사하지만 사용자 정의 오류 메시지를 지정할 수 있습니다:

```rust
use std::fs::File;

fn main() {
    let greeting_file = File::open("hello.txt")
        .expect("hello.txt should be included in this project");
}
```

**핵심 포인트:**
- 프로덕션 코드에서는 `unwrap`보다 `expect`를 사용하여 더 나은 디버깅 컨텍스트를 제공
- `expect` 메시지는 오류의 의도와 원인을 명확하게 설명해야 함

## 오류 전파하기 (Propagating Errors)

함수 내에서 오류를 처리하는 대신, 호출 코드에 오류를 반환할 수 있습니다:

```rust
use std::fs::File;
use std::io::{self, Read};

fn read_username_from_file() -> Result<String, io::Error> {
    let username_file_result = File::open("hello.txt");

    let mut username_file = match username_file_result {
        Ok(file) => file,
        Err(e) => return Err(e),
    };

    let mut username = String::new();

    match username_file.read_to_string(&mut username) {
        Ok(_) => Ok(username),
        Err(e) => Err(e),
    }
}
```

### `?` 연산자를 이용한 단축 문법

`?` 연산자는 오류 전파를 단순화합니다:

```rust
use std::fs::File;
use std::io::{self, Read};

fn read_username_from_file() -> Result<String, io::Error> {
    let mut username_file = File::open("hello.txt")?;
    let mut username = String::new();
    username_file.read_to_string(&mut username)?;
    Ok(username)
}
```

**`?` 연산자 동작 방식:**
- `Result`가 `Ok`이면 내부 값을 추출하고 프로그램 계속 진행
- `Result`가 `Err`이면 함수에서 즉시 오류를 반환
- 오류 값은 필요시 `From` 트레이트를 통해 오류 타입 간 변환

### 메서드 체이닝과 `?`

```rust
use std::fs::File;
use std::io::{self, Read};

fn read_username_from_file() -> Result<String, io::Error> {
    let mut username = String::new();

    File::open("hello.txt")?.read_to_string(&mut username)?;

    Ok(username)
}
```

### `fs::read_to_string` 사용

이 일반적인 작업에 대한 가장 간결한 접근 방식:

```rust
use std::fs;
use std::io;

fn read_username_from_file() -> Result<String, io::Error> {
    fs::read_to_string("hello.txt")
}
```

## `?` 연산자 사용 위치

`?` 연산자는 반환 타입이 `?`와 함께 사용되는 값과 호환되는 함수에서만 사용할 수 있습니다.

### `main`에서 `Result` 반환

```rust
use std::error::Error;
use std::fs::File;

fn main() -> Result<(), Box<dyn Error>> {
    let greeting_file = File::open("hello.txt")?;

    Ok(())
}
```

**핵심 포인트:**
- `main`이 `Ok(())`를 반환하면 실행 파일은 `0`으로 종료
- `main`이 `Err`를 반환하면 0이 아닌 값으로 종료
- `Box<dyn Error>`는 모든 종류의 오류를 반환 가능

## `Option`에서 `?` 사용

`?` 연산자는 `Option<T>`에서도 유사하게 동작합니다:

```rust
fn last_char_of_first_line(text: &str) -> Option<char> {
    text.lines().next()?.chars().last()
}

fn main() {
    assert_eq!(
        last_char_of_first_line("Hello, world\nHow are you today?"),
        Some('d')
    );

    assert_eq!(last_char_of_first_line(""), None);
    assert_eq!(last_char_of_first_line("\nhi"), None);
}
```

**참고:** `Result`와 `Option`을 `?`와 혼합할 수 없습니다. `Result`의 `ok()` 메서드나 `Option`의 `ok_or()` 메서드를 사용하여 명시적으로 변환해야 합니다.

---

# `panic!`할 것인가, 말 것인가

## 개요

코드가 패닉하면 복구할 방법이 없습니다. 핵심 결정 포인트: `panic!`을 호출해야 할까요, 아니면 `Result`를 반환해야 할까요?

**핵심 원칙:** 실패할 수 있는 함수를 정의할 때 `Result`를 반환하는 것이 좋은 기본 선택입니다. 호출 코드에 오류를 적절히 처리할 옵션을 제공하기 때문입니다.

## 예제, 프로토타입 코드, 테스트에서 사용

다음 상황에서 `panic!` (또는 `unwrap`, `expect`)을 사용합니다:

- **예제:** 강력한 오류 처리 코드를 포함하면 예제가 덜 명확해짐. `unwrap`은 실제 오류 처리를 위한 플레이스홀더 역할
- **프로토타입 코드:** 오류 처리 방법을 아직 결정하지 않았을 때 `unwrap`과 `expect`가 유용. 프로그램을 더 견고하게 만들 때를 위한 명확한 마커
- **테스트:** 테스트에서 메서드 호출이 실패하면 전체 테스트가 실패해야 함. `unwrap`이나 `expect` 호출이 적절한 동작

## 컴파일러보다 더 많은 정보를 가진 경우

`Result`가 `Ok` 값을 가질 것이라는 로직이 있지만 컴파일러가 이해할 수 없을 때 `expect`를 호출하는 것이 적절합니다:

```rust
fn main() {
    use std::net::IpAddr;

    let home: IpAddr = "127.0.0.1"
        .parse()
        .expect("Hardcoded IP address should be valid");
}
```

`127.0.0.1`은 하드코딩되어 있으므로 유효하다는 것을 알고 있습니다. `expect` 메시지에 이유를 문서화하세요.

## 오류 처리 가이드라인

### 패닉해야 할 때

코드가 **나쁜 상태**(bad state)에 빠질 수 있고 (가정, 보장, 계약, 불변성이 깨질 때) 다음 조건에 해당하면 패닉하는 것이 좋습니다:

- 나쁜 상태가 **예상치 못한 것**일 때 (잘못된 사용자 입력처럼 가끔 발생하는 것이 아님)
- 이 지점 이후의 코드가 매 단계마다 검사하는 대신 **이 나쁜 상태가 아님을 가정**할 때
- 이 정보를 **타입으로 인코딩할 좋은 방법이 없을 때**

**패닉이 적절한 경우:**
- 계속 진행하면 안전하지 않거나 해로울 때
- 호출 코드가 유효하지 않은 값을 전달할 때 - 개발 중 프로그래머에게 버그 수정을 알림
- 외부 코드 의존성이 수정할 수 없는 유효하지 않은 상태를 반환할 때
- 계약 위반이 발생할 때 (계약 위반은 호출자 측 버그를 나타냄)
- 범위를 벗어난 메모리 접근이 시도될 때 (보안 문제)

### Result를 반환해야 할 때

**실패가 예상되는 경우** `Result`를 반환합니다:
- 파서가 잘못된 형식의 데이터를 받을 때
- HTTP 요청이 속도 제한 상태를 반환할 때
- 사용자 입력이 유효하지 않을 때

`Result`를 반환하면 실패가 호출 코드가 반드시 처리해야 하는 예상된 가능성임을 나타냅니다.

## 유효성 검사를 위한 사용자 정의 타입

Rust의 타입 시스템을 사용하여 컴파일 타임에 유효한 값을 강제합니다. 모든 함수에서 런타임 검사를 하는 대신, 생성 시 유효성을 검사하는 사용자 정의 타입을 만듭니다.

**예제: Guess 타입**

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

    pub fn value(&self) -> i32 {
        self.value
    }
}
```

**주요 이점:**
- `value` 필드가 비공개이므로 `Guess::new()` 사용을 강제
- 유효성 검사는 생성 시 한 번만 수행
- 함수는 추가 검사 없이 시그니처에 `Guess`를 사용 가능
- 타입 시스템이 유효성을 보장

`Guess`를 받는 함수는 값이 1에서 100 사이임을 확신하고 진행할 수 있습니다 - 컴파일러가 이를 보장합니다.

## Rust의 타입 시스템 활용

타입을 사용하여 컴파일 타임에 유효하지 않은 상태를 방지합니다:
- 음수 값이 불가능해야 하면 `i32` 대신 `u32` 사용
- 무언가가 항상 존재해야 하면 `Option` 대신 전용 타입 사용
- 컴파일러가 런타임 검사를 요구하는 대신 유효하지 않은 코드가 컴파일되지 않도록 방지

---

## 요약

Rust의 오류 처리 기능은 견고한 코드를 작성하는 데 도움을 줍니다:

| 기능 | 용도 | 설명 |
|------|------|------|
| `panic!` | 복구 불가능한 상태 | 실행을 중단하고 프로그램 종료 |
| `Result` | 복구 가능한 오류 | 호출 코드가 오류 처리 방법 결정 |

**핵심 포인트:**
- `panic!`은 버그나 계속 진행할 수 없는 상태에 사용
- `Result`는 예상 가능한 실패에 사용하여 호출자에게 선택권 제공
- `?` 연산자로 오류 전파를 간결하게 처리
- 사용자 정의 타입으로 유효성을 타입 시스템에 인코딩
- `expect`를 사용하여 의도를 문서화하고 디버깅 용이하게

---

# `panic!`으로 복구 불가능한 오류 처리하기

> **원문:** https://doc.rust-lang.org/book/ch09-01-unrecoverable-errors-with-panic.html

## 개요

코드에서 처리할 수 없는 나쁜 일이 발생할 때가 있습니다. 이런 경우 Rust에는 `panic!` 매크로가 있습니다. 패닉을 발생시키는 방법은 두 가지가 있습니다:

1. 코드가 패닉을 발생시키는 동작 수행 (예: 배열 끝을 넘어 접근)
2. `panic!` 매크로를 명시적으로 호출

기본적으로 패닉이 발생하면:
- 실패 메시지 출력
- 스택을 되감고(unwind) 정리
- 프로그램 종료

`RUST_BACKTRACE` 환경 변수를 설정하여 패닉 발생 시 호출 스택을 표시할 수 있습니다.

## 스택 되감기(Unwinding) vs 중단(Abort)

### 기본 동작: 되감기(Unwinding)

패닉이 발생하면 프로그램은 **되감기**(unwinding)를 시작합니다. 이는 Rust가 스택을 거슬러 올라가며 만나는 각 함수의 데이터를 정리하는 것을 의미합니다.

### 대안적 동작: 중단(Abort)

스택을 거슬러 올라가며 정리하는 것은 작업이 많이 필요합니다. Rust는 정리 없이 즉시 **중단**(abort)하여 프로그램을 종료하는 것을 선택할 수 있게 합니다.

메모리는 대신 운영 체제가 정리합니다.

### 설정 방법

결과 바이너리를 최대한 작게 만들려면, *Cargo.toml*의 `[profile]` 섹션에 `panic = 'abort'`를 추가하여 되감기에서 중단으로 전환할 수 있습니다:

```toml
[profile.release]
panic = 'abort'
```

**핵심 포인트:**
- 되감기(Unwinding): 스택을 거슬러 올라가며 데이터 정리 (기본값)
- 중단(Abort): 정리 없이 즉시 종료, OS가 메모리 정리
- 바이너리 크기를 줄이려면 `panic = 'abort'` 사용

## 예제 1: 간단한 패닉

**파일명: src/main.rs**

```rust
fn main() {
    panic!("crash and burn");
}
```

**출력:**

```
$ cargo run
   Compiling panic v0.1.0 (file:///projects/panic)
    Finished `dev` profile [unoptimized + debuginfo] target(s) in 0.25s
     Running `target/debug/panic`

thread 'main' panicked at src/main.rs:2:5:
crash and burn
note: run with `RUST_BACKTRACE=1` environment variable to display a backtrace
```

**오류 메시지 구성:**
- 패닉 메시지: "crash and burn"
- 위치: *src/main.rs:2:5* (2번째 줄, 5번째 문자)

## 예제 2: 잘못된 벡터 접근으로 인한 패닉

**파일명: src/main.rs**

```rust
fn main() {
    let v = vec![1, 2, 3];

    v[99];
}
```

이 코드는 3개의 요소만 있는 벡터에서 100번째 요소(인덱스 99)에 접근하려 하여 패닉을 발생시킵니다.

**출력:**

```
$ cargo run
   Compiling panic v0.1.0 (file:///projects/panic)
    Finished `dev` profile [unoptimized + debuginfo] target(s) in 0.27s
     Running `target/debug/panic`

thread 'main' panicked at src/main.rs:4:6:
index out of bounds: the len is 3 but the index is 99
note: run with `RUST_BACKTRACE=1` environment variable to display a backtrace
```

### Rust가 패닉을 발생시키는 이유

C 언어에서는 데이터 구조의 끝을 넘어 읽는 것이 정의되지 않은 동작(undefined behavior)입니다. 해당 메모리 위치에 있는 값을 얻을 수 있으며, 이를 **버퍼 오버리드**(buffer overread)라고 합니다. 이는 보안 취약점으로 이어질 수 있습니다.

**Rust는 이를 보호합니다:** 잘못된 인덱스로 읽으려 하면 실행을 멈추고 계속 진행하지 않습니다.

**핵심 포인트:**
- C: 범위 밖 접근 시 정의되지 않은 동작 (보안 취약점 가능)
- Rust: 범위 밖 접근 시 즉시 패닉 (안전성 보장)
- 버퍼 오버리드 공격 방지

## 백트레이스(Backtrace) 사용하기

백트레이스를 얻으려면 `RUST_BACKTRACE` 환경 변수를 설정합니다:

```bash
$ RUST_BACKTRACE=1 cargo run
```

**백트레이스 출력 예시:**

```
thread 'main' panicked at src/main.rs:4:6:
index out of bounds: the len is 3 but the index is 99
stack backtrace:
   0: rust_begin_unwind
             at /rustc/4d91de4e48198da2e33413efdcd9cd2cc0c46688/library/std/src/panicking.rs:692:5
   1: core::panicking::panic_fmt
             at /rustc/4d91de4e48198da2e33413efdcd9cd2cc0c46688/library/core/src/panicking.rs:75:14
   2: core::panicking::panic_bounds_check
             at /rustc/4d91de4e48198da2e33413efdcd9cd2cc0c46688/library/core/src/panicking.rs:273:5
   3: <usize as core::slice::index::SliceIndex<[T]>>::index
             at file:///home/.rustup/toolchains/1.85/lib/rustlib/src/rust/library/core/src/slice/index.rs:274:10
   4: core::slice::index::<impl core::ops::index::Index<I> for [T]>::index
             at file:///home/.rustup/toolchains/1.85/lib/rustlib/src/rust/library/core/src/slice/index.rs:16:9
   5: <alloc::vec::Vec<T,A> as core::ops::index::Index<I>>::index
             at file:///home/.rustup/toolchains/1.85/lib/rustlib/src/rust/library/alloc/src/vec/mod.rs:3361:9
   6: panic::main
             at ./src/main.rs:4:6
   7: core::ops::function::FnOnce::call_once
             at file:///home/.rustup/toolchains/1.85/lib/rustlib/src/rust/library/core/src/ops/function.rs:250:5

note: Some details are omitted, run with `RUST_BACKTRACE=full` for a verbose backtrace.
```

### 백트레이스 읽는 방법

1. **위에서부터 시작**하여 작성한 파일이 나올 때까지 읽습니다
2. 그곳이 문제가 발생한 지점입니다
3. 위의 줄들 = 내 코드가 호출한 코드
4. 아래의 줄들 = 내 코드를 호출한 코드

위 예시에서 **6번 줄**이 사용자 코드의 실제 문제 지점을 가리킵니다: *src/main.rs:4:6*

**핵심 포인트:**
- 백트레이스는 문제 발생 시점까지의 함수 호출 경로를 보여줌
- 직접 작성한 코드 파일을 찾아서 문제 지점 파악
- `RUST_BACKTRACE=full`로 더 자세한 정보 확인 가능

## 디버깅 팁

- `cargo build` 또는 `cargo run`(--release 없이)을 사용하면 디버그 심볼이 기본으로 활성화됨
- 더 자세한 출력은 `RUST_BACKTRACE=full` 사용
- 코드가 패닉을 발생시키면, 어떤 동작과 값이 패닉을 유발했는지, 코드가 대신 무엇을 해야 하는지 파악해야 함

**핵심 포인트:**
- 디버그 빌드에서 디버그 심볼 자동 활성화
- 패닉 발생 시: 원인 파악 -> 올바른 동작 결정 -> 코드 수정
- 릴리스 빌드(`--release`)에서는 디버그 정보가 제거됨

## 요약

| 항목 | 설명 |
|------|------|
| `panic!` 매크로 | 복구 불가능한 오류 발생 시 프로그램 종료 |
| 되감기(Unwinding) | 스택을 거슬러 올라가며 정리 (기본값) |
| 중단(Abort) | 정리 없이 즉시 종료, 바이너리 크기 감소 |
| 백트레이스 | `RUST_BACKTRACE=1`로 호출 스택 확인 |
| 안전성 | 잘못된 메모리 접근 시 패닉으로 보안 보장 |

---

# Result로 복구 가능한 오류 처리하기

> **원문:** https://doc.rust-lang.org/book/ch09-02-recoverable-errors-with-result.html

## 개요

대부분의 오류는 프로그램을 완전히 중단시킬 만큼 심각하지 않습니다. `Result` 열거형(enum)은 함수가 성공 또는 실패를 반환하면서 호출 코드가 어떻게 응답할지 결정하게 합니다.

## Result 열거형 정의

```rust
enum Result<T, E> {
    Ok(T),
    Err(E),
}
```

**핵심 포인트:**
- `T`: 성공 시 반환되는 값의 타입 (`Ok` 변형)
- `E`: 실패 시 반환되는 오류의 타입 (`Err` 변형)

## match를 사용한 기본 오류 처리

### 예제: 파일 열기

```rust
use std::fs::File;

fn main() {
    let greeting_file_result = File::open("hello.txt");

    let greeting_file = match greeting_file_result {
        Ok(file) => file,
        Err(error) => panic!("Problem opening the file: {error:?}"),
    };
}
```

## 다른 오류에 따라 다르게 처리하기

오류 종류에 따라 다른 방식으로 처리할 수 있습니다:

```rust
use std::fs::File;
use std::io::ErrorKind;

fn main() {
    let greeting_file_result = File::open("hello.txt");

    let greeting_file = match greeting_file_result {
        Ok(file) => file,
        Err(error) => match error.kind() {
            ErrorKind::NotFound => match File::create("hello.txt") {
                Ok(fc) => fc,
                Err(e) => panic!("Problem creating the file: {e:?}"),
            },
            _ => panic!("Problem opening the file: {error:?}"),
        },
    };
}
```

## match의 대안

### 클로저와 함께 `unwrap_or_else` 사용

```rust
use std::fs::File;
use std::io::ErrorKind;

fn main() {
    let greeting_file = File::open("hello.txt").unwrap_or_else(|error| {
        if error.kind() == ErrorKind::NotFound {
            File::create("hello.txt").unwrap_or_else(|error| {
                panic!("Problem creating the file: {error:?}");
            })
        } else {
            panic!("Problem opening the file: {error:?}");
        }
    });
}
```

## 오류 발생 시 패닉을 위한 단축 메서드

### `unwrap()` 메서드

`Ok` 안의 값을 반환하거나 `Err`이면 `panic!`을 호출합니다:

```rust
use std::fs::File;

fn main() {
    let greeting_file = File::open("hello.txt").unwrap();
}
```

오류 출력:
```
thread 'main' panicked at src/main.rs:4:49:
called `Result::unwrap()` on an `Err` value: Os { code: 2, kind: NotFound, message: "No such file or directory" }
```

### `expect()` 메서드

`unwrap()`과 유사하지만 사용자 정의 오류 메시지를 지정할 수 있습니다:

```rust
use std::fs::File;

fn main() {
    let greeting_file = File::open("hello.txt")
        .expect("hello.txt should be included in this project");
}
```

오류 출력:
```
thread 'main' panicked at src/main.rs:5:10:
hello.txt should be included in this project: Os { code: 2, kind: NotFound, message: "No such file or directory" }
```

**핵심 포인트:**
- 프로덕션 코드에서는 더 나은 디버깅 컨텍스트를 위해 `unwrap()`보다 `expect()`를 선호해야 함
- `expect()`는 오류 발생 위치와 이유를 명확하게 전달

## 오류 전파하기

오류를 직접 처리하지 않고 호출 코드로 반환할 수 있습니다:

```rust
use std::fs::File;
use std::io::{self, Read};

fn read_username_from_file() -> Result<String, io::Error> {
    let username_file_result = File::open("hello.txt");

    let mut username_file = match username_file_result {
        Ok(file) => file,
        Err(e) => return Err(e),
    };

    let mut username = String::new();

    match username_file.read_to_string(&mut username) {
        Ok(_) => Ok(username),
        Err(e) => Err(e),
    }
}
```

**핵심 포인트:**
- 함수가 실패할 수 있는 작업을 호출할 때, 오류를 상위로 전파 가능
- 호출자가 오류 처리 방법을 결정하도록 위임

## `?` 연산자

오류 전파를 크게 단순화합니다:

```rust
use std::fs::File;
use std::io::{self, Read};

fn read_username_from_file() -> Result<String, io::Error> {
    let mut username_file = File::open("hello.txt")?;
    let mut username = String::new();
    username_file.read_to_string(&mut username)?;
    Ok(username)
}
```

### `?` 연산자의 동작 방식

**핵심 포인트:**
- `Result`가 `Ok`이면 내부 값을 반환하고 계속 진행
- `Result`가 `Err`이면 해당 오류를 함수 전체에서 반환
- 오류 타입은 `From` 트레이트를 통해 자동 변환됨

### `?`를 사용한 메서드 체이닝

```rust
use std::fs::File;
use std::io::{self, Read};

fn read_username_from_file() -> Result<String, io::Error> {
    let mut username = String::new();
    File::open("hello.txt")?.read_to_string(&mut username)?;
    Ok(username)
}
```

### 표준 라이브러리의 편의 함수 사용

```rust
use std::fs;
use std::io;

fn read_username_from_file() -> Result<String, io::Error> {
    fs::read_to_string("hello.txt")
}
```

## `?` 연산자를 사용할 수 있는 곳

`?` 연산자는 호환 가능한 반환 타입을 가진 함수에서만 사용할 수 있습니다: `Result`, `Option`, 또는 `FromResidual`을 구현하는 타입.

### 오류: `()`를 반환하는 `main()`에서 `?` 사용

```rust
use std::fs::File;

fn main() {
    let greeting_file = File::open("hello.txt")?;  // 컴파일 오류
}
```

### 해결책: `main` 반환 타입 변경

```rust
use std::error::Error;
use std::fs::File;

fn main() -> Result<(), Box<dyn Error>> {
    let greeting_file = File::open("hello.txt")?;
    Ok(())
}
```

## Option과 함께 `?` 사용하기

`?` 연산자는 `Option`을 반환하는 함수에서 `Option<T>`와 함께 작동합니다:

```rust
fn last_char_of_first_line(text: &str) -> Option<char> {
    text.lines().next()?.chars().last()
}

fn main() {
    assert_eq!(
        last_char_of_first_line("Hello, world\nHow are you today?"),
        Some('d')
    );

    assert_eq!(last_char_of_first_line(""), None);
    assert_eq!(last_char_of_first_line("\nhi"), None);
}
```

## 반환 타입 호환성

**핵심 포인트:**
- `Result`를 반환하는 함수에서만 `Result`에 `?` 사용 가능
- `Option`을 반환하는 함수에서만 `Option`에 `?` 사용 가능
- `?`로 `Result`와 `Option`을 혼합할 수 없음
- 필요한 경우 `ok()` 또는 `ok_or()` 같은 메서드로 명시적 변환

## main 함수의 반환 타입

Rust에서 `main`은 `Result<(), E>`를 반환할 수 있습니다:

```rust
use std::error::Error;
use std::fs::File;

fn main() -> Result<(), Box<dyn Error>> {
    let greeting_file = File::open("hello.txt")?;
    Ok(())
}
```

**핵심 포인트:**
- `Ok(())`: 실행 파일이 종료 코드 `0`으로 종료
- `Err`: 실행 파일이 0이 아닌 종료 코드로 종료
- 성공한 프로그램은 `0`을 반환하고 오류가 발생한 프로그램은 0이 아닌 값을 반환하는 C 규칙을 따름

## 요약

**Result와 오류 처리 핵심 개념:**
- `Result<T, E>`는 성공(`Ok`)과 실패(`Err`)를 표현하는 열거형
- `match`를 사용하여 각 경우를 명시적으로 처리
- `unwrap()`과 `expect()`는 빠른 프로토타이핑에 유용하지만, 프로덕션에서는 `expect()` 선호
- `?` 연산자는 오류 전파를 간결하게 만듦
- `?`는 `Result`와 `Option`에서 사용 가능하나 혼합 불가
- `main()`도 `Result`를 반환하여 `?` 연산자 사용 가능

---

# `panic!`을 사용할지 말지 결정하기

> **원문:** https://doc.rust-lang.org/book/ch09-03-to-panic-or-not-to-panic.html

## 개요

이 장에서는 Rust 오류 처리에서 `panic!`을 사용할지 `Result`를 반환할지 결정하는 방법을 설명합니다.

**핵심 원칙:** 코드가 패닉을 일으키면 복구할 방법이 없습니다. `Result`를 반환하면 호출 코드가 오류를 적절하게 처리할 수 있는 옵션을 제공합니다.

> 실패할 수 있는 함수를 정의할 때는 `Result`를 반환하는 것이 좋은 기본 선택입니다.

---

## `panic!` vs `Result` 사용 시점

### 예제, 프로토타입 코드, 테스트

다음 상황에서는 `panic!`을 사용합니다:

- **예제:** 강력한 오류 처리 코드를 포함하면 예제가 덜 명확해질 수 있습니다. `unwrap` 같은 메서드는 오류 처리가 플레이스홀더임을 나타냅니다.
- **프로토타이핑:** `unwrap`과 `expect`는 오류 처리 방법을 아직 결정하지 않았을 때 유용합니다.
- **테스트:** 테스트에서 메서드 호출이 실패하면 전체 테스트가 실패해야 합니다. `unwrap`이나 `expect`를 호출하는 것이 적절합니다. `panic!`이 테스트 실패를 표시하기 때문입니다.

### 컴파일러보다 더 많은 정보가 있을 때

`Result`가 `Ok`가 될 것이라는 로직이 있지만 컴파일러가 이해할 수 없는 경우 `expect`를 호출하는 것이 적절합니다.

```rust
fn main() {
    use std::net::IpAddr;

    let home: IpAddr = "127.0.0.1"
        .parse()
        .expect("Hardcoded IP address should be valid");
}
```

**핵심 포인트:**
- 컴파일러는 `"127.0.0.1"`이 항상 유효한지 판단할 수 없지만, 개발자는 수동으로 검증할 수 있음
- `expect` 메시지에 이유를 문서화하는 것이 좋음

---

## 오류 처리 가이드라인

### 패닉을 사용해야 하는 경우

코드가 **잘못된 상태(bad state)** 에 놓일 수 있을 때 패닉을 사용해야 합니다. 잘못된 상태란 가정, 보장, 계약 또는 불변성(invariant)이 위반된 상태를 말합니다:

1. 잘못된 상태가 **예상치 못한 것**일 때 (잘못된 사용자 입력처럼 가끔 발생하는 것이 아닌 경우)
2. 이 시점 이후의 코드가 **이 잘못된 상태가 아님에 의존**해야 할 때
3. 이 정보를 **타입 시스템에 인코딩할 좋은 방법이 없을** 때

**패닉 시나리오:**
- 코드에 잘못된 값이 전달된 경우
- 계속 진행하면 보안에 문제가 되거나 해로울 경우
- 외부 코드가 고칠 수 없는 잘못된 상태를 반환하는 경우
- 사용자에게 해를 끼칠 수 있는 작업(예: 메모리 접근) 전에 값을 검증할 때

> 계약(contract)이 위반되면 패닉을 일으키는 것이 합리적입니다. 계약 위반은 항상 호출자 측 버그를 나타내기 때문입니다.

### `Result`를 반환해야 하는 경우

실패가 **예상되는** 경우 `Result`를 반환합니다:

- 파서가 잘못된 형식의 데이터를 받는 경우
- HTTP 요청이 속도 제한 상태를 반환하는 경우
- `Result`를 반환하면 실패가 예상 가능한 상황임을 나타냄

---

## 유효성 검증을 위한 커스텀 타입

모든 함수에서 런타임 검사 대신, 생성 시 유효성을 검사하는 커스텀 타입을 만들어 Rust의 타입 시스템을 활용할 수 있습니다.

### 예제: Guess 타입

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

    pub fn value(&self) -> i32 {
        self.value
    }
}
```

**핵심 이점:**
- `value` 필드는 **private**이므로 `Guess::new`를 통해서만 생성 가능
- 유효성 검사가 한 곳에서 이루어지며 코드 전체에 분산되지 않음
- 함수가 시그니처에서 `Guess`를 받을 수 있어 런타임 검사가 불필요
- 타입 시스템이 유효한 값만 존재함을 보장

### 이 타입을 사용하는 함수

```rust
fn some_function(guess: Guess) {
    // guess.value()가 1과 100 사이임을 안전하게 사용할 수 있음
}
```

---

## 요약

- 정말로 복구할 수 없는 오류와 계약 위반에는 `panic!`을 사용
- 잠재적으로 복구 가능한 오류에는 기본적으로 `Result`를 반환
- Rust의 타입 시스템을 활용하여 컴파일 타임에 잘못된 상태를 방지
- 유효성 검사가 포함된 커스텀 타입을 만들어 반복적인 런타임 검사를 제거
